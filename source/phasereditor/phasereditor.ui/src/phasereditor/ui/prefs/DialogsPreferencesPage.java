package phasereditor.ui.prefs;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import phasereditor.ui.PhaserEditorUI;

public class DialogsPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public DialogsPreferencesPage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(PhaserEditorUI.getPreferenceStore());
		// setDescription("Configure default dialogs.");
	}

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();

		{
			RadioGroupFieldEditor editor = new RadioGroupFieldEditor(PhaserEditorUI.PREF_PROP_COLOR_DIALOG_TYPE,
					"Color Dialog", 1, new String[][] {

							{ "Native color dialog", PhaserEditorUI.PREF_VALUE_COLOR_DIALOG_NATIVE },
							{ "Cross-platform color dialog", PhaserEditorUI.PREF_VALUE_COLOR_DIALOG_JAVA }

					}, parent, true);
			addField(editor);

			if (PhaserEditorUI.isMacPlatform()) {
				editor.setEnabled(false, parent);
			}

		}
	}

}
