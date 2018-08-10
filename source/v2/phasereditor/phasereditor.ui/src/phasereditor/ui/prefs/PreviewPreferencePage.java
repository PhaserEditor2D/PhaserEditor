package phasereditor.ui.prefs;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import phasereditor.ui.PhaserEditorUI;

public class PreviewPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PreviewPreferencePage() {
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(PhaserEditorUI.getPreferenceStore());
		setDescription("Configure the Preview windows:");
	}

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();
		{
			ComboFieldEditor editor = new ComboFieldEditor(PhaserEditorUI.PREF_PROP_PREVIEW_IMG_PAINT_BG_TYPE,
					"Background image type", new String[][] {

							{ "Transparent", PhaserEditorUI.PREF_VALUE_PREVIEW_IMG_PAINT_BG_TYPE_TRANSPARENT },
							{ "Solid color", PhaserEditorUI.PREF_VALUE_PREVIEW_IMG_PAINT_BG_TYPE_ONE_COLOR },
							{ "Pattern", PhaserEditorUI.PREF_VALUE_PREVIEW_IMG_PAINT_BG_TYPE_TWO_COLORS }

					}, parent);
			addField(editor);
		}

		{
			Composite group = new Composite(parent, SWT.None);
			GridData gd = new GridData();
			gd.grabExcessHorizontalSpace = true;
			gd.horizontalAlignment = SWT.FILL;

			GridLayout layout = new GridLayout(2, false);
			group.setLayout(layout);

			Label label = new Label(group, SWT.NONE);
			gd = new GridData();
			gd.horizontalSpan = 2;
			label.setLayoutData(gd);
			label.setText("Background image colors:");

			addField(new ColorFieldEditor(PhaserEditorUI.PREF_PROP_PREVIEW_IMG_PAINT_BG_SOLID_COLOR, "Solid color",
					group));
			addField(new ColorFieldEditor(PhaserEditorUI.PREF_PROP_PREVIEW_IMG_PAINT_BG_COLOR_1, "Pattern color",
					group));
		}

	}

}
