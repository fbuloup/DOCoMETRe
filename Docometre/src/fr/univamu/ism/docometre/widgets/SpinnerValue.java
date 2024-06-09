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
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class SpinnerValue extends Composite {

	private StyledText valueText;
	private Button increaseButton;
	private Button decreaseButton;
	private double value;
	private double maxValue;
	private double minValue;
	private DecimalFormat decimalFormat = new DecimalFormat("#0.0000");
	private int currentCaretPostion;
	private ArrayList<Timer> increaseValueTimerList = new ArrayList<>(0);
	private ArrayList<Timer> decreaseValueTimerList = new ArrayList<>(0);
	
	private class UpdateValueTask extends TimerTask {
		private boolean increase;
		public UpdateValueTask(boolean increase) {
			this.increase = increase;
		}

		@Override
		public void run() {
			getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					decreaseIncreaseHandler(increase);
				}
			});
		}
	}

	public SpinnerValue(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(3, false));
		GridLayout gl = (GridLayout) getLayout();
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0; 
		
		valueText = new StyledText(this, SWT.SINGLE);
		valueText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		valueText.setText("0.0000");
		
		valueText.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				currentCaretPostion = valueText.getCaretOffset();
			}
		});
		
		valueText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				currentCaretPostion = valueText.getCaretOffset();
				if(e.character == SWT.CR) {
					try {
						String textValue = valueText.getText().replaceAll(",", ".");
						double newValue = Double.valueOf(textValue);
						validateValueHandler(newValue);
					} catch (NumberFormatException e1) {
						validateValueHandler(value);
					}
				}
			}
		});
		valueText.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				int n = Math.abs(e.count);
				double dx = computeDX();
				for (int i = 0; i < n; i++) {
					if(e.count > 0) validateValueHandler(value + dx);
					else validateValueHandler(value - dx);
				}
			}
		});
		decreaseButton = new Button(this,  SWT.ARROW | SWT.LEFT);
		decreaseButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		decreaseButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				TimerTask updateValueTask = new UpdateValueTask(false);
				Timer decreaseValueTimer = new Timer();
				decreaseValueTimerList.add(decreaseValueTimer);
				decreaseValueTimer.schedule(updateValueTask, 0, 200);
			}
			@Override
			public void mouseUp(MouseEvent e) {
				stopTimers();
			}
		});
		increaseButton = new Button(this,  SWT.ARROW | SWT.RIGHT);
		increaseButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		increaseButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				TimerTask updateValueTask = new UpdateValueTask(true);
				Timer increaseValueTimer = new Timer();
				increaseValueTimerList.add(increaseValueTimer);
				increaseValueTimer.schedule(updateValueTask, 0, 200);
			}
			@Override
			public void mouseUp(MouseEvent e) {
				stopTimers();
			}
		});

		value = 0;
		validateValueHandler(value);
		maxValue = 10.0;
		minValue = -10.0;
		
	}
	
	protected void stopTimers() {
		Timer[] timers = decreaseValueTimerList.toArray(new Timer[decreaseValueTimerList.size()]);
		for (Timer timer : timers) {
			timer.cancel();
			decreaseValueTimerList.remove(timer);
		}
		timers = increaseValueTimerList.toArray(new Timer[increaseValueTimerList.size()]);
		for (Timer timer : timers) {
			timer.cancel();
			increaseValueTimerList.remove(timer);
		}
		
	}

	private double computeDX() {
		// Get decimal position
		String stringValue = valueText.getText();
		int decimalPosition = stringValue.indexOf(",");
		if(decimalPosition == -1) decimalPosition = stringValue.indexOf(".");
		int power = (decimalPosition - currentCaretPostion);
		power = power>0?power-1:power;
		int coeff = (decimalPosition == currentCaretPostion)?0:1;
		coeff = (value < 0 && currentCaretPostion == 0)?0:coeff;
		double dx = coeff * Math.pow(10, power);
		return dx;
	}

	private void validateValueHandler(double newValue) {
		if(newValue <= maxValue && newValue >= minValue) {
			if(newValue > 0 && value <= 0) currentCaretPostion = currentCaretPostion - 1;
			if(newValue < 0 && value >= 0) currentCaretPostion = currentCaretPostion + 1;
			currentCaretPostion = (currentCaretPostion < 0)? 0:currentCaretPostion;
			value = newValue;
		}
		valueText.setText(decimalFormat.format(value));
		valueText.setCaretOffset(currentCaretPostion);
		valueText.getParent().getParent().layout();
	}

	private void decreaseIncreaseHandler(boolean increase) {
		double dx = computeDX();
		if(!increase) validateValueHandler(value - dx);
		else validateValueHandler(value + dx);
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		validateValueHandler(value);
	}

	public double getDx() {
		return computeDX();
	}

	public double getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(double maxValue) {
		this.maxValue = maxValue;
	}

	public double getMinValue() {
		return minValue;
	}

	public void setMinValue(double minValue) {
		this.minValue = minValue;
	}

}
