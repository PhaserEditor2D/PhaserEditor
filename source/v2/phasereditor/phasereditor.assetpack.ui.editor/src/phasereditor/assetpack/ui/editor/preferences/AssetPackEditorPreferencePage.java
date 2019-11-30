package phasereditor.assetpack.ui.editor.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import phasereditor.assetpack.core.AssetPackCore;

public class AssetPackEditorPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public AssetPackEditorPreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(AssetPackCore.getPreferenceStore());
		setDescription("Asset Pack Editor preferences");
	}

	@Override
	protected void createFieldEditors() {
		
		var editor = new BooleanFieldEditor(AssetPackCore.PREF_KEY_USE_CONTAINER_FOLDER_AS_KEY_PREFIX, "Use container folder as prefix for new asset keys.",
				getFieldEditorParent());

		addField(editor);
	}

}
