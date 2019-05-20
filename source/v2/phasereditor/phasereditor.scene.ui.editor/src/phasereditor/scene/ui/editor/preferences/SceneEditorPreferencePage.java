package phasereditor.scene.ui.editor.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import phasereditor.scene.ui.editor.SceneUIEditor;

public class SceneEditorPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public SceneEditorPreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(SceneUIEditor.getPreferenceStore());
		setDescription("Scene Editor preferences");
	}

	@Override
	protected void createFieldEditors() {
		{
			var editor = new RadioGroupFieldEditor(SceneUIEditor.PREF_KEY_PHASER_CONTEXT_TYPE,
					"Force the HTML5 Canvas context in Scene Editor", 1, new String[][] {

							{ "Phaser.CANVAS",
									SceneUIEditor.PREF_VALUE_PHASER_CONTEXT_TYPE_CANVAS },
							{ "Phaser.WEBGL", SceneUIEditor.PREF_VALUE_PHASER_CONTEXT_TYPE_WEBGL },
							{ "Default for this platform", SceneUIEditor.PREF_VALUE_PHASER_CONTEXT_TYPE_DEFAULT }

					}, getFieldEditorParent(), true);
			addField(editor);
		}
	}

}
