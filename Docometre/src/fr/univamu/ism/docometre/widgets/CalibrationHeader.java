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
package fr.univamu.ism.docometre.widgets;

import java.text.DecimalFormat;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;

public final class CalibrationHeader implements DisposeListener  {
	
	private DecimalFormat decimalFormat = new DecimalFormat("#0.0000");
	
	private ArrayList<CalibrationListener> aMinListener = new ArrayList<>();
	private ArrayList<CalibrationListener> aMaxListener = new ArrayList<>();
	private ArrayList<CalibrationListener> uMinListener = new ArrayList<>();
	private ArrayList<CalibrationListener> uMaxListener = new ArrayList<>();
	
	private double aMinValue;
	private double aMaxValue;
	private double uMinValue;
	private double uMaxValue;
	
	private StyledText aMinText;
	private StyledText aMaxText;
	private StyledText uMinText;
	private StyledText uMaxText;

	private Button captureAMinValueButton;

	private Button captureAMaxValueButton;

	private Image captureValueImage;
	
	public CalibrationHeader(Composite parent, double[] initialValues) {
		
		aMinValue = initialValues[0];
		aMaxValue = initialValues[1];
		uMinValue = initialValues[2];
		uMaxValue = initialValues[3];
		
		captureValueImage = Activator.getImage(IImageKeys.CAPTURE_VALUE_ICON);
		
		
		Label aMinLabel = new Label(parent, SWT.NONE);
		aMinLabel.setText("Amin :");
		aMinLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		aMinLabel.setToolTipText(DocometreMessages.AmpMinTooltip);
		aMinText = new StyledText(parent, SWT.SINGLE | SWT.BORDER);
		aMinText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		aMinText.setText(decimalFormat.format(initialValues[0]));
		((GridData)aMinText.getLayoutData()).widthHint = aMinText.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		addKeyHandler(aMinText);
		captureAMinValueButton = new Button(parent, SWT.FLAT);
		captureAMinValueButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		((GridData)captureAMinValueButton.getLayoutData()).heightHint = aMinText.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		captureAMinValueButton.setImage(captureValueImage);
		captureAMinValueButton.setToolTipText(DocometreMessages.CaptureValueMin);
		addMouseHandler(captureAMinValueButton);
		
		Label aMaxLabel = new Label(parent, SWT.NONE);
		aMaxLabel.setText("Amax :");
		aMaxLabel.setToolTipText(DocometreMessages.AmpMaxTooltip);
		aMaxLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		aMaxText = new StyledText(parent, SWT.SINGLE | SWT.BORDER);
		aMaxText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		aMaxText.setText(decimalFormat.format(initialValues[1]));
		((GridData)aMaxText.getLayoutData()).widthHint = aMaxText.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		addKeyHandler(aMaxText);
		captureAMaxValueButton = new Button(parent, SWT.FLAT);
		captureAMaxValueButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		((GridData)captureAMaxValueButton.getLayoutData()).heightHint = aMaxText.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		captureAMaxValueButton.setImage(captureValueImage);
		captureAMaxValueButton.setToolTipText(DocometreMessages.CaptureValueMax);
		addMouseHandler(captureAMaxValueButton);
		
		Label labelSeparator = new Label(parent, SWT.SEPARATOR | SWT.VERTICAL);
		labelSeparator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		((GridData) labelSeparator.getLayoutData()).heightHint = captureAMaxValueButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		
		Label uMinLabel = new Label(parent, SWT.NONE);
		uMinLabel.setText("Umin :");
		uMinLabel.setToolTipText(DocometreMessages.UnitMinTooltip);
		uMinLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		uMinText = new StyledText(parent, SWT.SINGLE | SWT.BORDER);
		uMinText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		uMinText.setText(decimalFormat.format(initialValues[2]));
		((GridData)uMinText.getLayoutData()).widthHint = uMinText.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		addKeyHandler(uMinText);
		
		Label uMaxLabel = new Label(parent, SWT.NONE);
		uMaxLabel.setText("Umax :");
		uMaxLabel.setToolTipText(DocometreMessages.UnitMaxTooltip);
		uMaxLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		uMaxText = new StyledText(parent, SWT.SINGLE | SWT.BORDER);
		uMaxText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		uMaxText.setText(decimalFormat.format(initialValues[3]));
		((GridData)uMaxText.getLayoutData()).widthHint = uMaxText.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		addKeyHandler(uMaxText);
		
		Label labelSeparator2 = new Label(parent, SWT.SEPARATOR | SWT.VERTICAL);
		labelSeparator2.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		((GridData) labelSeparator2.getLayoutData()).heightHint = ((GridData) labelSeparator.getLayoutData()).heightHint;
	}
	
	private void addMouseHandler(final Button button) {
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				ArrayList<CalibrationListener> cls = aMinListener;
				if(button == captureAMaxValueButton) cls = aMaxListener;
				if(cls.size() > 0) {
					double value;
					try {
						value = cls.get(0).get();
						StyledText text = aMinText;
						if(button == captureAMaxValueButton) text = aMaxText;
						text.setText(decimalFormat.format(value));
						validateValue(text);
					} catch (Exception e) {
						e.printStackTrace();
						Activator.logErrorMessageWithCause(e);
					}
				}
			}
		});
	}
	
	private void validateValue(StyledText text) {
		double oldValue = aMinValue;
		if(text == aMaxText) oldValue = aMaxValue;
		if(text == uMinText) oldValue = uMinValue;
		if(text == uMaxText) oldValue = uMaxValue;
		ArrayList<CalibrationListener> cls = aMinListener;
		if(text == aMaxText) cls = aMaxListener;
		if(text == uMinText) cls = uMinListener;
		if(text == uMaxText) cls = uMaxListener;
		try {
			String textValue = text.getText().replaceAll(",", ".");
			double newValue = Double.valueOf(textValue);
			notifyObservers(cls, newValue);
			text.setText(decimalFormat.format(newValue));
		} catch (NumberFormatException e1) {
			text.setText(decimalFormat.format(oldValue));
		}
	}

	private void addKeyHandler(StyledText text) {
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.character == SWT.CR) {
					validateValue(text);
				}
			}
		});
	}
	
	public void addAMinListener(CalibrationListener cl) {
		aMinListener.add(cl);
	}
	
	public void addAMaxListener(CalibrationListener cl) {
		aMaxListener.add(cl);
	}
	
	public void addUMinListener(CalibrationListener cl) {
		uMinListener.add(cl);
	}
	
	public void addUMaxListener(CalibrationListener cl) {
		uMaxListener.add(cl);
	}
	
	public void removeAMinListener(CalibrationListener cl) {
		aMinListener.remove(cl);
	}
	
	public void removeAMaxListener(CalibrationListener cl) {
		aMaxListener.remove(cl);
	}
	
	public void removeUMinListener(CalibrationListener cl) {
		uMinListener.remove(cl);
	}
	
	public void removeUMaxListener(CalibrationListener cl) {
		uMaxListener.remove(cl);
	}

	/*
	 * This notifier updates every listener in order 
	 * to update new calibrated value
	 */
	private void notifyObservers(ArrayList<CalibrationListener> cls, double newValue) {
		if(cls == aMinListener) aMinValue = newValue;
		if(cls == aMaxListener) aMaxValue = newValue;
		if(cls == uMinListener) uMinValue = newValue;
		if(cls == uMaxListener) uMaxValue = newValue;
		for (CalibrationListener calibrationListener : cls) {
			calibrationListener.push(newValue);
		}
	}

	@Override
	public void widgetDisposed(DisposeEvent e) {
		captureValueImage.dispose();
	}
	
}
