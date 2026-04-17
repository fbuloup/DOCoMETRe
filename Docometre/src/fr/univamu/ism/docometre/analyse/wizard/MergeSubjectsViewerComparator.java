package fr.univamu.ism.docometre.analyse.wizard;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

public class MergeSubjectsViewerComparator extends ViewerComparator {
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		String subject1 = (String) e1;
		String subject2 = (String) e2;
		if(subject1.matches("^.*\\d+$") && subject2.matches("^.*\\d+$")) {
			String prefix1 = subject1.replaceAll("\\d+$", "");
			String prefix2 = subject2.replaceAll("\\d+$", "");
			if(prefix1.equals(prefix2)) {
				long number1 = Long.parseLong(subject1.replaceAll(prefix1, ""));
				long number2 = Long.parseLong(subject2.replaceAll(prefix2, ""));
				return (int) (number1 - number2);
			}
		}
		return super.compare(viewer, e1, e2);
	}
}