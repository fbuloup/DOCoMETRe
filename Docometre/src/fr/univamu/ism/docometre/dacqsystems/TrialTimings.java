package fr.univamu.ism.docometre.dacqsystems;

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
		synchronized (TrialTimings.class) {
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
		}
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
		if (!trace) return;
		log(String.format("real-time loop : %d iterations, last = %.1f ms, max = %.1f ms (the end is detected up to one iteration late)",
				iterations, lastIterationNanos / 1e6, maxIterationNanos / 1e6));
	}
}
