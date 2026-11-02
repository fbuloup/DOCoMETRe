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

import java.util.ArrayList;
import java.util.List;

/**
 * Points (x, y) buffered by a channel observer (real time loop thread) until they
 * are added to the chart serie by the UI thread (see {@link UIRefreshScheduler}).
 * Thread safe.
 */
public final class PendingPoints {
	
	private final List<Double[]> xBlocks = new ArrayList<>();
	private final List<Double[]> yBlocks = new ArrayList<>();
	private int size;
	
	/** Buffers points (x and y arrays must have the same length) */
	public synchronized void add(Double[] xValues, Double[] yValues) {
		if(xValues.length == 0) return;
		xBlocks.add(xValues);
		yBlocks.add(yValues);
		size += xValues.length;
	}
	
	/** Removes all the buffered points */
	public synchronized void clear() {
		xBlocks.clear();
		yBlocks.clear();
		size = 0;
	}
	
	/**
	 * Removes and returns all the buffered points, from the oldest to the most recent :
	 * x values in [0], y values in [1]. Returns null if there is no point.
	 */
	public synchronized Double[][] drain() {
		if(size == 0) return null;
		Double[] xValues = new Double[size];
		Double[] yValues = new Double[size];
		int position = 0;
		for (int i = 0; i < xBlocks.size(); i++) {
			Double[] xBlock = xBlocks.get(i);
			System.arraycopy(xBlock, 0, xValues, position, xBlock.length);
			System.arraycopy(yBlocks.get(i), 0, yValues, position, xBlock.length);
			position += xBlock.length;
		}
		clear();
		return new Double[][] {xValues, yValues};
	}
	
}
