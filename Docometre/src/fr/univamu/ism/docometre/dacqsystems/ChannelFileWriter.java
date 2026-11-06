/*******************************************************************************
 * Copyright or © or Copr. Institut des Sciences du Mouvement 
 * (CNRS & Aix Marseille Université)
 * 
 * The DOCoMETER Software must be used with a real time data acquisition 
 * system marketed by ADwin (ADwin Pro and Gold, I and II) or an Arduino 
 * Uno. This software, created within the Institute of Movement Sciences, 
 * has been developed to facilitate their use by a "neophyte" public in the 
 * fields of industrial computing and electronics.  Students, researchers or 
 * engineers can configure this acquisition system in the best possible 
 * conditions so that it best meets their experimental needs. 
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 * 
 * Contributors:
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [25/03/2020]
 ******************************************************************************/
package fr.univamu.ism.docometre.dacqsystems;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

/**
 * Writes the samples files of the channels in a dedicated thread.
 * <p>
 * The real-time loop gives the samples received at each iteration to
 * {@link #write(FileChannel, ByteBuffer)}, which only queues them and returns at
 * once : a slow disk (other processes writing, antivirus, system tasks...) can no
 * more block the acquisition loop. The writes are done in the order they were
 * queued, by a single thread : the samples of a channel are written in order.
 * <p>
 * {@link #close(FileChannel)} waits for all the writes queued before it, then
 * closes the file : when it returns, the file is complete, as before (the files
 * must be complete before the end of the trial processing).
 */
public final class ChannelFileWriter {

	// Writes and closes to do, in order. Unbounded : about 100 KB per second for 20
	// channels at 1 kHz, the queue stays small even if the disk is blocked for seconds.
	private static final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();

	static {
		Thread writerThread = new Thread(ChannelFileWriter::run, "DOCoMETRe channels files writer");
		writerThread.setDaemon(true);
		writerThread.start();
	}

	private ChannelFileWriter() {
	}

	private static void run() {
		while (true) {
			try {
				tasks.take().run();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			} catch (Throwable t) {
				// The writer must go on for the other channels
				Logger.getLogger(Process.class).error("Exception message", t);
			}
		}
	}

	/**
	 * Queues the writing of the buffer (from its position to its limit) in the file,
	 * and returns at once. The buffer must not be modified afterwards.
	 */
	public static void write(FileChannel fileChannel, ByteBuffer buffer) {
		Objects.requireNonNull(fileChannel, "fileChannel");
		tasks.add(() -> {
			try {
				while (buffer.hasRemaining()) fileChannel.write(buffer);
			} catch (IOException e) {
				Logger.getLogger(Process.class).error("Exception message", e);
			}
		});
	}

	/**
	 * Waits for the end of the writes queued before this call, then closes the file.
	 * Returns when the file is closed (complete).
	 */
	public static void close(FileChannel fileChannel) throws IOException {
		Objects.requireNonNull(fileChannel, "fileChannel");
		CompletableFuture<Void> closed = new CompletableFuture<>();
		tasks.add(() -> {
			try {
				fileChannel.close();
				closed.complete(null);
			} catch (IOException e) {
				closed.completeExceptionally(e);
			}
		});
		try {
			closed.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while closing a channel file", e);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof IOException) throw (IOException) e.getCause();
			throw new IOException(e.getCause());
		}
	}
}
