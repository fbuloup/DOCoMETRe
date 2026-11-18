package fr.univamu.ism.docometre.dacqsystems.adwin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.adwin.driver.ADwinCommunicationError;
import de.adwin.driver.ADwinDevice;
import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.ModuleBehaviour;
import fr.univamu.ism.docometre.dacqsystems.TrialTimings;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

/**
 * Single transfer FIFO of the ADwin processes.
 *
 * Without it, each auto-transferred channel has its own FIFO (DATA_<transfer number>)
 * and the PC needs, for each channel and each iteration of the real-time loop, a
 * Fifo_Full and a GetFifo_Float request (three network round trips with UDP). With
 * it, the generated ADbasic code writes, for each sample of each auto-transferred
 * channel, its transfer number then its value in a single FIFO (DATA_198), and the
 * PC reads all the channels with one Fifo_Full and one GetFifo_Float request, then
 * dispatches the samples to the channels.
 *
 * When the single FIFO is full, the new sample is dropped and the data loss counter
 * of its channel (PAR_<transfer number>) is incremented, as with the channel FIFOs.
 *
 * Channels written by the user code (not auto-transferred) and stimuli keep their
 * own FIFO. The mode is chosen at compilation time (preference
 * {@link GeneralPreferenceConstants#ADWIN_SINGLE_TRANSFER_FIFO}) and saved in a
 * marker file next to the binary, so that the PC always reads the data as the
 * loaded binary writes them, even if the preference has changed since.
 */
public final class ADWinTransferFifo {

	/** ADwin DATA array used as single transfer FIFO (DATA_199 and DATA_200 hold the parameters). */
	public static final int FIFO_NUMBER = 198;
	private static final String FIFO_NAME = "DOCO_TRANSFER_FIFO";
	/** Extension of the marker file written next to the binary compiled with the single transfer FIFO. */
	private static final String MARKER_FILE_EXTENSION = ".TRANSFER_FIFO";
	/** Buffer size used for the size of the single FIFO if the one of a channel is not valid. */
	private static final int DEFAULT_BUFFER_SIZE = 1000;

	/** Samples received for a channel during one loop iteration. */
	private static final class ChannelSamples {
		private final ADWinChannel channel;
		private final int transferNumber;
		private final String name;
		private final boolean canChannel;
		private float[] values = new float[64];
		private int size;

		private ChannelSamples(ADWinChannel channel, int transferNumber) {
			this.channel = channel;
			this.transferNumber = transferNumber;
			this.name = channel.getProperty(ChannelProperties.NAME);
			this.canChannel = channel.getModule() instanceof ADWinCANModule;
		}

		private void add(float value) {
			if(size == values.length) values = Arrays.copyOf(values, 2 * size);
			values[size++] = value;
		}
	}

	private final ADWinProcess process;
	private final ADwinDevice adwinDevice;
	private final int processNumber;
	private final Map<Integer, ChannelSamples> channelsSamples;
	private boolean unknownTransferNumberLogged;

	private ADWinTransferFifo(ADWinProcess process, List<ADWinChannel> channels) {
		this.process = process;
		this.adwinDevice = ((ADWinDACQConfiguration) process.getDACQConfiguration()).getADwinDevice();
		this.processNumber = Integer.parseInt(process.getProperty(ADWinProcessProperties.PROCESS_NUMBER));
		channelsSamples = new LinkedHashMap<>();
		for (ADWinChannel channel : channels) {
			int transferNumber = channel.getTransferNumber();
			channelsSamples.put(transferNumber, new ChannelSamples(channel, transferNumber));
		}
	}

	// ------------------------------------------------------------------------------------------
	// Channels and mode
	// ------------------------------------------------------------------------------------------

	/** True if the single transfer FIFO is used by the code generated from now on (preference). */
	public static boolean isEnabled() {
		return Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.ADWIN_SINGLE_TRANSFER_FIFO);
	}

	/**
	 * True if this channel goes through the single transfer FIFO when it is enabled:
	 * transferred to the PC (not a stimulus), auto-transferred, not a parameter, with a
	 * valid transfer number.
	 */
	public static boolean isTransferFifoChannel(Channel channel) {
		if(!(channel instanceof ADWinChannel)) return false;
		ADWinChannel adwinChannel = (ADWinChannel) channel;
		if(!adwinChannel.isRecoveryAllowed()) return false;
		if(!"true".equals(adwinChannel.getProperty(ChannelProperties.AUTO_TRANSFER))) return false;
		if(adwinChannel instanceof ADWinVariable && ((ADWinVariable) adwinChannel).isParameter()) return false;
		try {
			int transferNumber = adwinChannel.getTransferNumber();
			return transferNumber > 0 && transferNumber < FIFO_NUMBER;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/** Channels of the configuration that go through the single transfer FIFO when it is enabled. */
	public static List<ADWinChannel> getTransferFifoChannels(DACQConfiguration dacqConfiguration) {
		List<ADWinChannel> channels = new ArrayList<>();
		ADWinVariable[] variables = ((ADWinDACQConfiguration) dacqConfiguration).getVariables();
		if(variables != null) for (ADWinVariable variable : variables) if(isTransferFifoChannel(variable)) channels.add(variable);
		for (int i = 0; i < dacqConfiguration.getModulesNumber(); i++) {
			ModuleBehaviour module = dacqConfiguration.getModule(i);
			Channel[] moduleChannels = module instanceof Module ? ((Module) module).getChannels() : null;
			if(moduleChannels != null) for (Channel channel : moduleChannels) if(isTransferFifoChannel(channel)) channels.add((ADWinChannel) channel);
		}
		return channels;
	}

	/** True if the code generated from now on uses the single transfer FIFO for this channel. */
	private static boolean generatesTransferFifoCode(Channel channel) {
		return isEnabled() && isTransferFifoChannel(channel);
	}
	
	/**
	 * True if the code generated from now on declares and clears the own FIFO of this transferred
	 * channel (DATA_<transfer number>, <name>_TAB) : false for the channels written in the single
	 * transfer FIFO, which do not use it (stimuli and channels written by the user code keep it).
	 */
	public static boolean declaresOwnFifo(Channel channel) {
		return !generatesTransferFifoCode(channel);
	}

	// ------------------------------------------------------------------------------------------
	// ADbasic code generation
	// ------------------------------------------------------------------------------------------

	/** Declaration of the single transfer FIFO (empty if it is not used). */
	public static String getDeclarationCode(DACQConfiguration dacqConfiguration) {
		if(!isEnabled()) return "";
		List<ADWinChannel> channels = getTransferFifoChannels(dacqConfiguration);
		if(channels.isEmpty()) return "";
		// Two values (transfer number, value) per sample : room for the buffers of all the channels
		long size = 2;
		for (ADWinChannel channel : channels) {
			int bufferSize = DEFAULT_BUFFER_SIZE;
			try {
				bufferSize = Integer.parseInt(channel.getProperty(ChannelProperties.BUFFER_SIZE));
			} catch (NumberFormatException e) {
				// Default size
			}
			size += 2L * Math.max(1, bufferSize);
		}
		String code = "\nREM ******** FIFO de transfert unique : numero de transfert puis valeur de chaque echantillon des canaux auto-transferes\n";
		code = code + "#DEFINE " + FIFO_NAME + " DATA_" + FIFO_NUMBER + "\n";
		code = code + "DIM " + FIFO_NAME + "[" + size + "] AS FLOAT AS FIFO\n";
		return code;
	}

	/** Initialization of the single transfer FIFO (empty if it is not used). */
	public static String getInitializationCode(DACQConfiguration dacqConfiguration) {
		if(!isEnabled() || getTransferFifoChannels(dacqConfiguration).isEmpty()) return "";
		return "FIFO_CLEAR(" + FIFO_NUMBER + ") ' FIFO de transfert unique\n";
	}

	/**
	 * Automatic transfer of a channel to the PC, in its own FIFO or in the single transfer FIFO.
	 * @param channel the channel
	 * @param name name of the channel (ADbasic variable)
	 * @param transferNumber transfer number of the channel
	 * @param frequencyRatio global frequency / channel frequency
	 */
	public static String getAutoTransferCode(Channel channel, String name, String transferNumber, int frequencyRatio) {
		String code = "\nIF (TRANSFERT_" + name + " = " + frequencyRatio + ") THEN\n";
		code = code + "\tTRANSFERT_" + name + " = 0\n";
		if(generatesTransferFifoCode(channel)) {
			// Not enough room for the transfer number and the value : sample dropped
			code = code + "\tIF (FIFO_EMPTY(" + FIFO_NUMBER + ") < 2) THEN\n";
			code = code + "\t\tPAR_" + transferNumber + " = PAR_" + transferNumber + " + 1\n";
			code = code + "\tELSE\n";
			code = code + "\t\t" + FIFO_NAME + " = " + transferNumber + "\n";
			code = code + "\t\t" + FIFO_NAME + " = " + name + "\n";
			code = code + "\tENDIF\n";
		} else {
			code = code + "\tIF (FIFO_EMPTY(" + transferNumber + ") = 0) THEN\n";
			code = code + "\t\tPAR_" + transferNumber + " = PAR_" + transferNumber + " + 1\n";
			code = code + "\t\tFIFO_CLEAR(" + transferNumber + ")\n";
			code = code + "\tENDIF\n";
			code = code + "\t" + name + "_TAB = " + name + "\n";
		}
		code = code + "ENDIF\n";
		code = code + "INC(TRANSFERT_" + name + ")\n";
		return code;
	}

	// ------------------------------------------------------------------------------------------
	// Marker file : mode of the compiled binary
	// ------------------------------------------------------------------------------------------

	private static File getMarkerFile(String binaryFilePath) {
		return new File(binaryFilePath + MARKER_FILE_EXTENSION);
	}

	/**
	 * Saves the mode of the binary that has just been compiled successfully from the
	 * code generated for this configuration.
	 * @param binaryFilePath path of the binary, without extension
	 */
	public static void saveCompiledMode(String binaryFilePath, DACQConfiguration dacqConfiguration) {
		File markerFile = getMarkerFile(binaryFilePath);
		try {
			if(isEnabled() && !getTransferFifoChannels(dacqConfiguration).isEmpty()) {
				Files.write(markerFile.toPath(), ("DATA_" + FIFO_NUMBER + "\n").getBytes(StandardCharsets.US_ASCII));
			} else Files.deleteIfExists(markerFile.toPath());
		} catch (IOException e) {
			Activator.logErrorMessageWithCause(e);
		}
	}

	/**
	 * Reader of the single transfer FIFO for an execution of the process, or null if
	 * its binary has been compiled without it.
	 * @param binaryFilePath path of the loaded binary, without extension
	 */
	public static ADWinTransferFifo open(ADWinProcess process, String binaryFilePath) {
		if(!getMarkerFile(binaryFilePath).exists()) return null;
		List<ADWinChannel> channels = getTransferFifoChannels(process.getDACQConfiguration());
		if(channels.isEmpty()) return null;
		return new ADWinTransferFifo(process, channels);
	}

	// ------------------------------------------------------------------------------------------
	// Recovery
	// ------------------------------------------------------------------------------------------

	/** True if the samples of this channel are read from the single transfer FIFO. */
	public boolean contains(Channel channel) {
		if(!(channel instanceof ADWinChannel)) return false;
		try {
			ChannelSamples channelSamples = channelsSamples.get(((ADWinChannel) channel).getTransferNumber());
			return channelSamples != null && channelSamples.channel == channel;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Reads the single transfer FIFO (two ADwin requests whatever the number of
	 * channels), gives its samples to each channel and checks the data loss counters.
	 */
	public void recover() throws ADwinCommunicationError {
		long start = TrialTimings.partStart();
		int available = adwinDevice.Fifo_Full(FIFO_NUMBER);
		TrialTimings.loopPart(TrialTimings.LoopPart.FIFO_FULL, start);
		// Even number of values : a transfer number written without its value yet is read next time
		int count = available - available % 2;
		if(count > 0) {
			start = TrialTimings.partStart();
			float[] data = process.getData(FIFO_NUMBER, count);
			TrialTimings.getFifo(start, count);
			for (ChannelSamples channelSamples : channelsSamples.values()) channelSamples.size = 0;
			for (int i = 0; i < count; i += 2) {
				ChannelSamples channelSamples = channelsSamples.get(Math.round(data[i]));
				if(channelSamples != null) channelSamples.add(data[i + 1]);
				else if(!unknownTransferNumberLogged) {
					unknownTransferNumberLogged = true;
					Activator.logWarningMessage("Single transfer FIFO : unknown transfer number " + data[i] + " (process compiled with another configuration ?)");
				}
			}
			for (ChannelSamples channelSamples : channelsSamples.values()) {
				if(channelSamples.size == 0) continue;
				TrialTimings.dataReceived(channelSamples.name, channelSamples.canChannel);
				start = TrialTimings.partStart();
				channelSamples.channel.addSamples(Arrays.copyOf(channelSamples.values, channelSamples.size));
				TrialTimings.loopPart(TrialTimings.LoopPart.ADD_SAMPLES, start);
				process.diaryRecovered(channelSamples.name, channelSamples.size);
			}
		}
		// Counters read with a single request at the beginning of the iteration (see ADWinProcess.recovery())
		for (ChannelSamples channelSamples : channelsSamples.values()) {
			RecoveryDelegate.checkDataLoss(process, adwinDevice, processNumber, channelSamples.channel, channelSamples.transferNumber);
		}
	}
}
