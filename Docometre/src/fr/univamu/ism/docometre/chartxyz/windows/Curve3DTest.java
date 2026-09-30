package fr.univamu.ism.docometre.chartxyz.windows;

import java.io.IOException;
import java.nio.file.Paths;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.chartxyz.CurveData;

public class Curve3DTest {

    private static Curve3D curve3d;

	public static void main(String[] args) {
    	CurveData curveData;
    	int choice = 1;
    	if(choice == 1) curveData = CurveData.loadFromEquation();
    	else {
          String dataFile = (args.length > 0) ? args[0] : "data/sample_curve.txt";
          try {
              curveData = CurveData.loadFromFile(Paths.get(dataFile));
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
        
        curve3d = new Curve3D(shell, curveData);

        Button button = new Button(shell, SWT.NORMAL);
        button.setText("PUSH");
        button.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		if(!curve3d.isDisposed()) curve3d.close();
        		else {
        			curve3d = new Curve3D(shell, curveData);
        			curve3d.moveAbove(button);
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
