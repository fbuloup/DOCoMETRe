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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [24/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.calibration;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyOperation;

/**
 * Undoable reset of the calibration of an analog input : default calibration of the hardware (see
 * {@link ChannelCalibration#applyDefault(Channel)}), the input is no longer calibrated. Undo restores the calibration
 * values (AMin, AMax, UMin, UMax and calibrated state) as they were.
 */
public class ResetCalibrationOperation extends ModifyPropertyOperation {
	
	private final ChannelCalibration calibration;
	private final Channel channel;
	private final String[] previousValues;

	/**
	 * @param uiElement control of the input (focus and selection on undo/redo, see ModifyPropertyOperation)
	 */
	public ResetCalibrationOperation(Object uiElement, String label, ChannelCalibration calibration, Channel channel, IUndoContext undoContext) {
		super(uiElement, label, calibration.getCalibratedProperty(), channel, ChannelCalibration.FALSE, undoContext);
		this.calibration = calibration;
		this.channel = channel;
		previousValues = calibration.getValues(channel);
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		// Nothing done when the default calibration of the hardware can not be determined
		return calibration.applyDefault(channel) ? Status.OK_STATUS : Status.CANCEL_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		setFocus();
		return execute(monitor, info);
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		setFocus();
		calibration.restore(channel, previousValues);
		return Status.OK_STATUS;
	}

}
