// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.inspect.ui.prefs;

import java.nio.file.Paths;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import phasereditor.inspect.core.InspectCore;

/**
 * @author arian
 *
 */
public class PhaserPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	BooleanFieldEditor _builtInField;
	PhaserVersionDirectoryField _dirField;
	private String _initState;

	public PhaserPreferencePage() {
		super(FieldEditorPreferencePage.GRID);
		setPreferenceStore(InspectCore.getPreferenceStore());
		setTitle("Phaser Settings");
		setDescription("Select the Phaser Bundle to be used in the editor.\n\n"
				+ "Updated bundles can be downloaded from the Phaser Editor site:\n\n"
				+ "http://phasereditor.boniatillo.com/blog/downloads \n\n"
				+ "A Phaser Bundle contains a particular Phaser release plus related Phaser Editor metadata.\n\n"
				+ "** The current bundle supports Phaser v" + InspectCore.getCurrentPhaserVersion() + " **\n\n");
	}

	class PhaserVersionDirectoryField extends DirectoryFieldEditor {

		public PhaserVersionDirectoryField(String name, String labelText, Composite parent) {
			super(name, labelText, parent);
			setErrorMessage("Invalid Phaser Bundle directory.");
		}

		@Override
		protected boolean doCheckState() {
			if (!this.getLabelControl().isEnabled()) {
				return true;
			}

			String fileName = getTextControl().getText();
			fileName = fileName.trim();
			if (fileName.length() == 0 && isEmptyStringAllowed()) {
				return true;
			}

			if (!InspectCore.isValidPhaserVersionFolder(Paths.get(fileName))) {
				return false;
			}

			return super.doCheckState();
		}

		@Override
		public void valueChanged() {
			super.valueChanged();
		}
	}

	@Override
	protected void createFieldEditors() {
		_builtInField = new BooleanFieldEditor(InspectCore.PREF_BUILTIN_PHASER_VERSION,
				"Use built-in Phaser Bundle (supports v" + InspectCore.BUILTIN_PHASER_VERSION + ")",
				getFieldEditorParent());

		_dirField = new PhaserVersionDirectoryField(InspectCore.PREF_USER_PHASER_VERSION_PATH,
				"External Phaser Bundle path", getFieldEditorParent());

		addField(_builtInField);
		addField(_dirField);

		_dirField.setEnabled(!InspectCore.isBuiltInPhaserVersion(), getFieldEditorParent());

		_initState = InspectCore.isBuiltInPhaserVersion() + "@" + InspectCore.getPhaserVersionFolder();
	}

	@Override
	public boolean performOk() {
		boolean ok = super.performOk();
		if (ok) {
			String state = InspectCore.isBuiltInPhaserVersion() + "@" + InspectCore.getPhaserVersionFolder();
			if (!_initState.equals(state)) {
				if (MessageDialog.openConfirm(getShell(), "Restart",
						"The system needs to restart to apply the changes.")) {

					getShell().getDisplay().asyncExec(new Runnable() {

						@Override
						public void run() {
							PlatformUI.getWorkbench().restart(true);
						}
					});
				} else {
					return false;
				}
			}
		}
		return ok;
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		updateValidState();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);

		Object source = event.getSource();
		if (source == _builtInField) {
			updateValidState();
		}
	}

	private void updateValidState() {
		_dirField.setEnabled(!_builtInField.getBooleanValue(), getFieldEditorParent());
		_dirField.valueChanged();
	}

	@Override
	public void init(IWorkbench workbench) {
		// nothing
	}

}
