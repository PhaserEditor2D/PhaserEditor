package phasereditor.ui.prefs;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PhaserEditorPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public PhaserEditorPreferencePage() {
		super("Phaser Editor");
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
