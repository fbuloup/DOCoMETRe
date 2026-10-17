package fr.univamu.ism.docometre.chartxyz.tests;

import java.io.IOException;
import java.nio.file.Paths;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.chartxyz.Curve3DData;
import fr.univamu.ism.docometre.chartxyz.windows.Chart3D;

public class Chart3DTest {

    private static Chart3D chart3d;

	public static void main(String[] args) {
    	Curve3DData curveData;
    	int choice = 1;
    	if(choice == 1) curveData = Curve3DData.loadFromEquation();
    	else {
          String dataFile = (args.length > 0) ? args[0] : "data/sample_curve.txt";
          try {
              curveData = Curve3DData.loadFromFile(Paths.get(dataFile));
          } catch (IOException e) {
              System.err.println("Impossible de charger le fichier de points : " + dataFile);
              e.printStackTrace();
              return;
          } 		
    	}
    	
    	if(curveData == null) return;

        Display display = new Display();
        Shell shell = new Shell(display);
        shell.setText("Visualiseur de courbe 3D — SWT + JOGL");
        shell.setSize(1000, 700);
        shell.setLayout(new FillLayout());
        
        chart3d = new Chart3D(shell, curveData);

        Button button = new Button(shell, SWT.NORMAL);
        button.setText("PUSH");
        button.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		if(!chart3d.isDisposed()) chart3d.close();
        		else {
        			chart3d = new Chart3D(shell, curveData);
        			chart3d.moveAbove(button);
        		}
        		shell.layout();
        	}
		});
        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }
}
