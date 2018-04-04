package phasereditor.canvas.ui.prefs;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class CanvasPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {

	public CanvasPreferencesPage() {
		super("Canvas");
		
		noDefaultAndApplyButton();
	}

	@Override
	public void init(IWorkbench workbench) {
		//
	}

	@Override
	protected Control createContents(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText("Expand the tree to edit preferences of specific features.");
		return label;
	}

}
