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
import fr.univamu.ism.docometre.chartxyz.macos.NewtChart3D;

/**
 * NEWT MUST BE USED WITH OSX instead of 
 */
public class NewtChart3DTest {

    private static NewtChart3D newtChart3D;

	public static void main(String[] args) {

        String dataFile = (args.length > 0) ? args[0] : "data/sample_curve.txt";

        Curve3DData curveData;
        try {
            curveData = Curve3DData.loadFromFile(Paths.get(dataFile));
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

        newtChart3D = new NewtChart3D(shell, curveData);
        
        Button button = new Button(shell, SWT.NORMAL);
        button.setText("PUSH");
        button.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		if(!newtChart3D.isDisposed()) newtChart3D.close();
        		else {
        			newtChart3D = new NewtChart3D(shell, curveData);
        			newtChart3D.moveAbove(button);
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
