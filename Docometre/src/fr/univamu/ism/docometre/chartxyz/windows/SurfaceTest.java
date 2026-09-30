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

import fr.univamu.ism.docometre.chartxyz.SurfaceData;

public class SurfaceTest {

    private static Surface surface;

	public static void main(String[] args) {
        SurfaceData surfaceData;
        int choice = 1;
        if(choice == 1) {
        	surfaceData = SurfaceData.loadFromEquation();
        } else {
        	String dataFile = (args.length > 0) ? args[0] : "data/sample_surface.txt";
            try {
                surfaceData = SurfaceData.loadFromFile(Paths.get(dataFile));
            } catch (IOException e) {
                System.err.println("Impossible de charger le fichier de grille : " + dataFile);
                e.printStackTrace();
                return;
            }
        }

        if(surfaceData == null) return;
        
        Display display = new Display();
        Shell shell = new Shell(display);
        shell.setText("Visualiseur de surface 3D — SWT + JOGL");
        shell.setSize(1000, 700);
        shell.setLayout(new FillLayout());

        surface = new Surface(shell, surfaceData);

        Button button = new Button(shell, SWT.NORMAL);
        button.setText("PUSH");
        button.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		if(!surface.isDisposed()) surface.close();
        		else {
        			surface = new Surface(shell, surfaceData);
        			surface.moveAbove(button);
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
