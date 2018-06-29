package phasereditor.atlas.ui.properties;

import static java.lang.System.out;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.properties.IPropertySheetPage;

public class DemoPropPage extends Page implements IPropertySheetPage {

	private Label _label;

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		out.println("Selection changed " + selection + " " + part);
	}

	@Override
	public void createControl(Composite parent) {
		_label = new Label(parent, SWT.NONE);
		_label.setText("Hello property!");
	}

	@Override
	public Control getControl() {
		return _label;
	}

	@Override
	public void setFocus() {
		getControl().setFocus();
	}

}