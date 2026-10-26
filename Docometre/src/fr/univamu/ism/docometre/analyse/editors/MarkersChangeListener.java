package fr.univamu.ism.docometre.analyse.editors;

import fr.univamu.ism.docometre.analyse.datamodel.Channel;

/**
 * Editor that displays the markers of signals and must be told when they
 * change (marker added or removed, markers group removed), see
 * UpdateWorkbenchDelegate.updateMarkers(Channel). SWT thread.
 */
public interface MarkersChangeListener {

	/** The markers of the signal changed in the MathEngine. */
	void markersChanged(Channel signal);
}
