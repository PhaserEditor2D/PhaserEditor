package phasereditor.inspect.ui;

import static phasereditor.ui.IEditorSharedImages.IMG_PHASER_LOGO;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import phasereditor.inspect.core.InspectCore;
import phasereditor.ui.EditorSharedImages;

public class VersionsControl extends WorkbenchWindowControlContribution {

	public VersionsControl() {
	}

	@Override
	protected Control createControl(Composite parent) {
		CLabel label = new CLabel(parent, SWT.None);
		label.setText("Phaser v" + InspectCore.PHASER_VERSION);
		label.setImage(EditorSharedImages.getImage(IMG_PHASER_LOGO));
		return label;
	}

}
