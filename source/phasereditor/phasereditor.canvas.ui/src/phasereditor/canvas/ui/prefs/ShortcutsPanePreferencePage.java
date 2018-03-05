package phasereditor.canvas.ui.prefs;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import phasereditor.canvas.ui.CanvasUI;

public class ShortcutsPanePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public ShortcutsPanePreferencePage() {
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(CanvasUI.getPreferenceStore());
		setDescription("Configure the shortcuts pane:");

	}

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();

		addField(new BooleanFieldEditor(CanvasUI.PREF_PROP_CANVAS_SHORTCUT_PANE_ENABLED, "Enable shortcuts pane",
				BooleanFieldEditor.SEPARATE_LABEL, parent));

		addField(new ComboFieldEditor(CanvasUI.PREF_PROP_CANVAS_SHORTCUT_PANE_POSITION, "Pane position",
				new String[][] {

						{ "Top-Left", CanvasUI.PREF_VALUE_CANVAS_SHORTCUT_PANE_POSITION_TOP_LEFT },
						{ "Top-Right", CanvasUI.PREF_VALUE_CANVAS_SHORTCUT_PANE_POSITION_TOP_RIGHT },
						{ "Bottom-Left", CanvasUI.PREF_VALUE_CANVAS_SHORTCUT_PANE_POSITION_BOTTOM_LEFT },
						{ "Bottom-Right", CanvasUI.PREF_VALUE_CANVAS_SHORTCUT_PANE_POSITION_BOTTOM_RIGHT },
						{ "Next to Object", CanvasUI.PREF_VALUE_CANVAS_SHORTCUT_PANE_POSITION_NEXT_TO_OBJECT },

				}, parent));

		addField(new ColorFieldEditor(CanvasUI.PREF_PROP_CANVAS_SHORTCUT_PANE_FG_COLOR, "Foreground color", parent));
		addField(new ColorFieldEditor(CanvasUI.PREF_PROP_CANVAS_SHORTCUT_PANE_BG_COLOR, "Background color", parent));
	}

}
