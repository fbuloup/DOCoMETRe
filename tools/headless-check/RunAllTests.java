import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

import java.io.PrintWriter;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/**
 * Runs the JUnit 5 tests of a package (fr.univamu.ism by default) outside Eclipse and prints a summary.
 * Exit code is 0 only if tests have been found and none has failed. Used by check.sh.
 */
public class RunAllTests {

	public static void main(String[] args) {
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request().selectors(selectPackage(args.length > 0 ? args[0] : "fr.univamu.ism")).build();
		Launcher launcher = LauncherFactory.create();
		SummaryGeneratingListener listener = new SummaryGeneratingListener();
		launcher.execute(request, listener);
		TestExecutionSummary summary = listener.getSummary();
		summary.printTo(new PrintWriter(System.out));
		summary.printFailuresTo(new PrintWriter(System.out), 15);
		System.exit(summary.getTotalFailureCount() == 0 && summary.getTestsFoundCount() > 0 ? 0 : 1);
	}

}
