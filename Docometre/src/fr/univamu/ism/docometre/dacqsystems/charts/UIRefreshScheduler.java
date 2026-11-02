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
package fr.univamu.ism.docometre.dacqsystems.charts;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * Coalesced and asynchronous refresh of a real time chart.
 * <p>
 * The channels observers are notified by the real time loop thread. Before, they
 * updated their chart synchronously (Display.syncExec) : the real time loop was
 * blocked until the UI thread was available and the chart rendered, for each curve
 * and at each iteration.
 * <p>
 * Now the observers buffer the samples and call {@link #request()}, which never blocks :
 * the refresh runnable is executed later by the UI thread, at most once every
 * {@link #REFRESH_PERIOD_MS} ms, with all the samples received in the meantime.
 */
public final class UIRefreshScheduler {
	
	/** Minimum period between two refreshes (25 Hz) */
	public static final int REFRESH_PERIOD_MS = 40;
	
	private final AtomicBoolean scheduled = new AtomicBoolean(false);
	private final Runnable refreshRunnable;
	
	/**
	 * @param refreshRunnable runnable executed by the UI thread. It must take all the
	 * data buffered since its previous execution.
	 */
	public UIRefreshScheduler(Runnable refreshRunnable) {
		this.refreshRunnable = refreshRunnable;
	}
	
	/**
	 * Asks for a refresh. Returns immediately. Several requests made before the
	 * refresh is executed lead to a single refresh.
	 */
	public void request() {
		if(!scheduled.compareAndSet(false, true)) return;
		Display display = PlatformUI.isWorkbenchRunning() ? PlatformUI.getWorkbench().getDisplay() : Display.getDefault();
		if(display == null || display.isDisposed()) {
			scheduled.set(false);
			return;
		}
		display.asyncExec(() -> {
			if(display.isDisposed()) return;
			display.timerExec(REFRESH_PERIOD_MS, () -> {
				// Reset before refreshing : data received during the refresh will lead to a new refresh
				scheduled.set(false);
				refreshRunnable.run();
			});
		});
	}
	
}
