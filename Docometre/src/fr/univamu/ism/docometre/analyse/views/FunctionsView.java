package fr.univamu.ism.docometre.analyse.views;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ApplicationActionBarAdvisor;

public class FunctionsView extends ViewPart {
	
	public static String ID = "Docometre.FunctionsView";
	private TreeViewer functionsTreeViewer;

	public FunctionsView() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createPartControl(Composite parent) {
		functionsTreeViewer = new TreeViewer(parent, SWT.BORDER);
		functionsTreeViewer.setContentProvider(new FunctionsEditorTreeContentProvider());
		ILabelDecorator decorator = PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
		functionsTreeViewer.setLabelProvider(new DecoratingLabelProvider(new FunctionsEditorLabelProvider(), decorator));
		functionsTreeViewer.setComparator(new ViewerComparator());
		functionsTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				try {
					PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
								@Override
								public void run() {
									Object element = ((IStructuredSelection) functionsTreeViewer.getSelection()).getFirstElement();
									functionsTreeViewer.setExpandedState(element, !functionsTreeViewer.getExpandedState(element));
									ApplicationActionBarAdvisor.openEditorAction.run();
								}
							});
						}
					});
				} catch (InvocationTargetException | InterruptedException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			}
		});
		functionsTreeViewer.setInput(FunctionsModel.createModel());
		getSite().setSelectionProvider(functionsTreeViewer);
	}

	@Override
	public void setFocus() {
		functionsTreeViewer.getTree().setFocus();
	}

}
