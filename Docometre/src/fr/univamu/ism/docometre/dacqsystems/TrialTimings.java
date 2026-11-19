package fr.univamu.ism.docometre.dacqsystems;

import java.util.Arrays;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;

/**
 * Timing trace of the steps between the end of a trial (process ended on the
 * acquisition system) and the start of the next one, and of the start itself
 * (first real-time loop iteration, first data received).
 * Each measure is logged as an INFO message in the Eclipse log
 * (workspace/.metadata/.log), prefixed by [TIMING].
 *
 * Nothing is measured nor logged when {@link #trace} is false.
 */
public final class TrialTimings {

	/** Enables or disables the timing trace. */
	public static volatile boolean trace = false;

	/**
	 * ADwin FPAR in which the generated ADbasic code stores the duration of its
	 * INIT section (in timer ticks). A FPAR because the PARs are used as data loss
	 * counters of the transfered channels (PAR_n for transfer number n).
	 */
	public static final int INIT_DURATION_FPAR = 79;

	private static final String PREFIX = "[TIMING] ";

	// System.nanoTime() of the end of the previous trial, -1 when not measuring
	private static long origin = -1;
	// System.nanoTime() of the previous step
	private static long last;
	// System.nanoTime() of Start_Process, -1 when not measuring the start
	private static long startOrigin = -1;
	// Start events already logged for the current trial
	private static boolean firstLoopLogged;
	private static boolean firstCANDataLogged;
	private static boolean firstOtherDataLogged;
	// Number of calls to stop() for the current trial
	private static int stopCalls;

	/**
	 * Parts of a real-time loop iteration whose durations are accumulated
	 * (see {@link #loopPart}) and summarized at the end of the trial
	 * (see {@link #loopPartsSummary}).
	 */
	public enum LoopPart {
		PROCESS_STATUS("Process_Status"),
		DATA_LOSS_COUNTERS("Get_Par_Block (data loss counters)"),
		FIFO_FULL("Fifo_Full"),
		GET_FIFO("GetFifo_Float"),
		ADD_SAMPLES("addSamples (copy + file queue)"),
		DIARY("diary (appendToEventDiary)"),
		GENERATION("generation"),
		CHARTS_TRANSFER("transfer to charts (notifyChannelObservers)"),
		STATUS_LINE("status line (Get_FPar + Workload)");

		private final String label;

		LoopPart(String label) {
			this.label = label;
		}
	}

	// Accumulated durations of the loop parts for the current trial (reset at Start_Process)
	private static final long[] partTotalNanos = new long[LoopPart.values().length];
	private static final long[] partMaxNanos = new long[LoopPart.values().length];
	private static final long[] partCalls = new long[LoopPart.values().length];
	private static long getFifoSamples;

	/**
	 * Tasks of the user interface thread measured during a trial (see {@link #uiPart}) : a task longer than
	 * {@link #UI_TASK_LOG_THRESHOLD_MS} is logged with the time of the trial, all are summarized at its end.
	 */
	public enum UIPart {
		CHARTS_POINTS("charts : points added to the series"),
		CHARTS_RENDERING("charts : OpenGL rendering (one frame)");

		private final String label;

		UIPart(String label) {
			this.label = label;
		}
	}

	/** A task of the user interface thread longer than this is logged */
	public static final int UI_TASK_LOG_THRESHOLD_MS = 30;
	/** Period of the watchdog of the user interface thread (see {@link #startUIWatchdog()}) */
	private static final int UI_WATCHDOG_PERIOD_MS = 20;
	/** A delay of the watchdog longer than this is logged : the user interface thread was blocked */
	private static final int UI_BLOCK_LOG_THRESHOLD_MS = 80;
	/** An interval without points added to the charts longer than this is logged */
	private static final int UI_POINTS_GAP_LOG_THRESHOLD_MS = 200;

	// Accumulated durations of the tasks of the user interface thread for the current trial (reset at Start_Process)
	private static final long[] uiTotalNanos = new long[UIPart.values().length];
	private static final long[] uiMaxNanos = new long[UIPart.values().length];
	private static final long[] uiCalls = new long[UIPart.values().length];
	// Watchdog of the user interface thread : generation of the running watchdog (an older one stops), time of its
	// previous tick, number of delays logged and longest delay
	private static int uiWatchdogGeneration;
	private static long uiWatchdogLastTick;
	private static long uiBlocks;
	private static long uiMaxDelayNanos;
	// Points added to the charts : time of the last addition, longest interval without addition
	private static long uiLastPointsNanos;
	private static long uiMaxPointsGapNanos;

	private TrialTimings() {
	}

	/** Logs a message (with the [TIMING] prefix). */
	public static void log(String message) {
		if (!trace) return;
		Activator.logInfoMessage(PREFIX + message, TrialTimings.class);
	}

	/** The process has ended on the acquisition system: starts the measure. */
	public static void processEnded() {
		if (!trace) return;
		uiSummary();
		synchronized (TrialTimings.class) {
			// Watchdog of the user interface thread stopped
			uiWatchdogGeneration++;
			origin = System.nanoTime();
			last = origin;
			startOrigin = -1;
		}
		log("process ended (t = 0)");
	}

	/** Logs the duration of the step that has just finished. No effect when not measuring. */
	public static void step(String name) {
		if (!trace) return;
		String message;
		synchronized (TrialTimings.class) {
			if (origin < 0) return;
			long now = System.nanoTime();
			message = String.format("%-45s step = %8.1f ms   total = %8.1f ms", name, (now - last) / 1e6, (now - origin) / 1e6);
			last = now;
		}
		log(message);
	}

	/** Logs the last step (process started), stops the measure and starts the measure of the start. */
	public static void end(String name) {
		if (!trace) return;
		step(name);
		synchronized (TrialTimings.class) {
			origin = -1;
			startOrigin = System.nanoTime();
			firstLoopLogged = false;
			firstCANDataLogged = false;
			firstOtherDataLogged = false;
			stopCalls = 0;
			Arrays.fill(partTotalNanos, 0);
			Arrays.fill(partMaxNanos, 0);
			Arrays.fill(partCalls, 0);
			getFifoSamples = 0;
			Arrays.fill(uiTotalNanos, 0);
			Arrays.fill(uiMaxNanos, 0);
			Arrays.fill(uiCalls, 0);
			uiBlocks = 0;
			uiMaxDelayNanos = 0;
			uiLastPointsNanos = 0;
			uiMaxPointsGapNanos = 0;
		}
		startUIWatchdog();
	}

	/*
	 * Time since the start of the current trial in seconds, negative when the trial is not measured (caller
	 * synchronized on TrialTimings.class)
	 */
	private static double trialTime(long nanos) {
		return startOrigin < 0 ? -1 : (nanos - startOrigin) / 1e9;
	}

	/**
	 * Adds the duration of a task of the user interface thread, started at startNanos (see {@link #partStart}) :
	 * logged if it is longer than {@link #UI_TASK_LOG_THRESHOLD_MS}. Measured only during a trial.
	 */
	public static void uiPart(UIPart part, long startNanos) {
		if (!trace || startNanos == 0) return;
		long now = System.nanoTime();
		long duration = now - startNanos;
		String message = null;
		synchronized (TrialTimings.class) {
			if (startOrigin < 0) return;
			int i = part.ordinal();
			uiTotalNanos[i] += duration;
			uiCalls[i]++;
			if (duration > uiMaxNanos[i]) uiMaxNanos[i] = duration;
			if (duration > UI_TASK_LOG_THRESHOLD_MS * 1_000_000L)
				message = String.format("user interface thread : %s lasted %.1f ms (t = %.3f s of the trial)", part.label, duration / 1e6, trialTime(startNanos));
			if (part == UIPart.CHARTS_POINTS) {
				long gap = uiLastPointsNanos == 0 ? 0 : startNanos - uiLastPointsNanos;
				if (gap > uiMaxPointsGapNanos) uiMaxPointsGapNanos = gap;
				if (gap > UI_POINTS_GAP_LOG_THRESHOLD_MS * 1_000_000L) {
					String gapMessage = String.format("charts : no points added for %.1f ms (until t = %.3f s of the trial)", gap / 1e6, trialTime(startNanos));
					message = message == null ? gapMessage : gapMessage + System.lineSeparator() + PREFIX + message;
				}
				uiLastPointsNanos = now;
			}
		}
		if (message != null) log(message);
	}

	/*
	 * Watchdog of the user interface thread during a trial : a task run every UI_WATCHDOG_PERIOD_MS by the user
	 * interface thread. When it runs late, the user interface thread was blocked (by the charts or by any other task) :
	 * a delay longer than UI_BLOCK_LOG_THRESHOLD_MS is logged with the time of the trial. Stopped by processEnded().
	 */
	private static void startUIWatchdog() {
		Display display = PlatformUI.isWorkbenchRunning() ? PlatformUI.getWorkbench().getDisplay() : null;
		if (display == null || display.isDisposed()) return;
		int generation;
		synchronized (TrialTimings.class) {
			generation = ++uiWatchdogGeneration;
		}
		display.asyncExec(() -> {
			synchronized (TrialTimings.class) {
				uiWatchdogLastTick = System.nanoTime();
			}
			if (!display.isDisposed()) display.timerExec(UI_WATCHDOG_PERIOD_MS, () -> uiWatchdogTick(display, generation));
		});
	}

	private static void uiWatchdogTick(Display display, int generation) {
		String message = null;
		synchronized (TrialTimings.class) {
			if (!trace || generation != uiWatchdogGeneration) return;
			long now = System.nanoTime();
			long delay = now - uiWatchdogLastTick - UI_WATCHDOG_PERIOD_MS * 1_000_000L;
			uiWatchdogLastTick = now;
			if (delay > uiMaxDelayNanos) uiMaxDelayNanos = delay;
			if (delay > UI_BLOCK_LOG_THRESHOLD_MS * 1_000_000L) {
				uiBlocks++;
				message = String.format("user interface thread blocked about %.1f ms (until t = %.3f s of the trial)", delay / 1e6, trialTime(now));
			}
		}
		if (message != null) log(message);
		if (!display.isDisposed()) display.timerExec(UI_WATCHDOG_PERIOD_MS, () -> uiWatchdogTick(display, generation));
	}

	/*
	 * Summary of the user interface thread during the trial (called at its end, by processEnded())
	 */
	private static void uiSummary() {
		StringBuilder message = new StringBuilder();
		synchronized (TrialTimings.class) {
			if (startOrigin < 0) return;
			message.append(String.format("user interface thread during the trial : blocked %d times more than %d ms, longest delay of the watchdog = %.1f ms, longest interval without points added to the charts = %.1f ms",
					uiBlocks, UI_BLOCK_LOG_THRESHOLD_MS, uiMaxDelayNanos / 1e6, uiMaxPointsGapNanos / 1e6));
			for (UIPart part : UIPart.values()) {
				int i = part.ordinal();
				if (uiCalls[i] == 0) continue;
				message.append(String.format("%n    %-38s calls = %6d   total = %9.1f ms   mean/call = %7.3f ms   max/call = %7.1f ms",
						part.label, uiCalls[i], uiTotalNanos[i] / 1e6, uiTotalNanos[i] / 1e6 / uiCalls[i], uiMaxNanos[i] / 1e6));
			}
		}
		log(message.toString());
	}

	/** Start of a measured part: System.nanoTime(), or 0 when not measuring (no cost). */
	public static long partStart() {
		return trace ? System.nanoTime() : 0;
	}

	/** Adds the duration of a loop part, started at startNanos (see {@link #partStart}). */
	public static void loopPart(LoopPart part, long startNanos) {
		if (!trace || startNanos == 0) return;
		long duration = System.nanoTime() - startNanos;
		synchronized (TrialTimings.class) {
			int i = part.ordinal();
			partTotalNanos[i] += duration;
			partCalls[i]++;
			if (duration > partMaxNanos[i]) partMaxNanos[i] = duration;
		}
	}

	/** Same as {@link #loopPart(LoopPart, long)} for GetFifo_Float, with the number of samples read. */
	public static void getFifo(long startNanos, int samples) {
		if (!trace || startNanos == 0) return;
		loopPart(LoopPart.GET_FIFO, startNanos);
		synchronized (TrialTimings.class) {
			getFifoSamples += samples;
		}
	}

	/**
	 * Summary of the loop parts, logged when the process has ended: for each
	 * part, number of calls, total, share of the loop time, mean and max per
	 * call, mean per iteration.
	 *
	 * @param iterations number of loop iterations
	 * @param loopNanos  total duration of the iterations
	 */
	public static void loopPartsSummary(long iterations, long loopNanos) {
		if (!trace || iterations <= 0) return;
		StringBuilder message = new StringBuilder();
		message.append(String.format("real-time loop parts (%d iterations, mean iteration = %.2f ms) :", iterations, loopNanos / 1e6 / iterations));
		long measured = 0;
		synchronized (TrialTimings.class) {
			for (LoopPart part : LoopPart.values()) {
				int i = part.ordinal();
				if (partCalls[i] == 0) continue;
				measured += partTotalNanos[i];
				message.append(String.format("%n    %-38s calls = %6d   total = %9.1f ms (%5.1f %%)   mean/call = %7.3f ms   max/call = %7.1f ms   mean/iteration = %6.2f ms",
						part.label, partCalls[i], partTotalNanos[i] / 1e6, loopNanos > 0 ? 100.0 * partTotalNanos[i] / loopNanos : 0,
						partTotalNanos[i] / 1e6 / partCalls[i], partMaxNanos[i] / 1e6, partTotalNanos[i] / 1e6 / iterations));
				if (part == LoopPart.GET_FIFO) message.append(String.format("   samples/call = %.1f", (double) getFifoSamples / partCalls[i]));
			}
		}
		long other = loopNanos - measured;
		message.append(String.format("%n    %-38s                  total = %9.1f ms (%5.1f %%)                                                mean/iteration = %6.2f ms",
				"other (not measured)", other / 1e6, loopNanos > 0 ? 100.0 * other / loopNanos : 0, other / 1e6 / iterations));
		log(message.toString());
	}

	/** First iteration of the real-time loop (logged once per trial). */
	public static void firstLoopIteration() {
		if (!trace) return;
		String message;
		synchronized (TrialTimings.class) {
			if (startOrigin < 0 || firstLoopLogged) return;
			firstLoopLogged = true;
			message = String.format("%-45s %8.1f ms after Start_Process", "first real-time loop iteration", (System.nanoTime() - startOrigin) / 1e6);
		}
		log(message);
	}

	/**
	 * Data received for a channel: the first data of a CAN channel and the
	 * first data of another channel are logged (once per trial).
	 */
	public static void dataReceived(String channelName, boolean canChannel) {
		if (!trace) return;
		String message;
		synchronized (TrialTimings.class) {
			if (startOrigin < 0) return;
			if (canChannel) {
				if (firstCANDataLogged) return;
				firstCANDataLogged = true;
			} else {
				if (firstOtherDataLogged) return;
				firstOtherDataLogged = true;
			}
			String name = (canChannel ? "first CAN data (" : "first non CAN data (") + channelName + ")";
			message = String.format("%-45s %8.1f ms after Start_Process", name, (System.nanoTime() - startOrigin) / 1e6);
		}
		log(message);
	}

	/** Duration of a call to Process.stop() (the first call of the trial and each time out are logged). */
	public static void stopMeasured(long durationNanos, boolean timeOut) {
		if (!trace) return;
		int calls;
		synchronized (TrialTimings.class) {
			calls = ++stopCalls;
		}
		if (calls > 1 && !timeOut) return;
		String message = String.format("stop() call #%d : %8.1f ms", calls, durationNanos / 1e6);
		if (timeOut) message = message + "   -> TIME OUT (100 ms) : Clear_Process + boot() + process reload at next start !";
		log(message);
	}

	/** Summary of the real-time loop, logged when the process has ended. */
	public static void loopSummary(long iterations, long lastIterationNanos, long maxIterationNanos) {
		loopSummary("real-time loop", "(the end is detected up to one iteration late)", iterations, lastIterationNanos, maxIterationNanos);
	}

	/**
	 * Summary of the real-time loop, logged when the process has ended.
	 * @param label what is counted as an iteration
	 * @param comment comment added at the end of the message
	 */
	public static void loopSummary(String label, String comment, long iterations, long lastIterationNanos, long maxIterationNanos) {
		if (!trace) return;
		log(String.format("%s : %d iterations, last = %.1f ms, max = %.1f ms %s",
				label, iterations, lastIterationNanos / 1e6, maxIterationNanos / 1e6, comment));
	}
}
