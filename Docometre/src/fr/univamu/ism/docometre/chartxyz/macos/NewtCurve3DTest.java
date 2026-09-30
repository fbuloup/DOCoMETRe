package fr.univamu.ism.docometre.chartxyz.macos;

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

/**
 * NEWT MUST BE USED WITH OSX instead of 
 */
public class NewtCurve3DTest {

    private static NewtCurve3D newtCurve3D;

	public static void main(String[] args) {

        String dataFile = (args.length > 0) ? args[0] : "data/sample_curve.txt";

        CurveData curveData;
        try {
            curveData = CurveData.loadFromFile(Paths.get(dataFile));
        } catch (IOException e) {
            System.err.println("Impossible de charger le fichier de points : " + dataFile);
            e.printStackTrace();
            return;
        }

        Display display = new Display();
        Shell shell = new Shell(display);
        shell.setText("Visualiseur de courbe 3D — SWT + JOGL (NEWT)");
        shell.setSize(1000, 700);
        shell.setLayout(new FillLayout());

        newtCurve3D = new NewtCurve3D(shell, curveData);
        
        Button button = new Button(shell, SWT.NORMAL);
        button.setText("PUSH");
        button.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		if(!newtCurve3D.isDisposed()) newtCurve3D.close();
        		else {
        			newtCurve3D = new NewtCurve3D(shell, curveData);
        			newtCurve3D.moveAbove(button);
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
