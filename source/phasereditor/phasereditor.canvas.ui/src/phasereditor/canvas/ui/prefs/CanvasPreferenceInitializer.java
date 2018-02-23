package phasereditor.canvas.ui.prefs;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.RGB;

import phasereditor.canvas.ui.CanvasUI;

public class CanvasPreferenceInitializer extends AbstractPreferenceInitializer {

	public CanvasPreferenceInitializer() {
	}

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = CanvasUI.getPreferenceStore();

		store.setDefault(CanvasUI.PREF_PROP_CANVAS_SHORTCUT_PANE_POSITION,
				CanvasUI.PREF_VALUE_CANVAS_SHORTCUT_PANE_POSITION_TOP_RIGHT);

		store.setDefault(CanvasUI.PREF_PROP_CANVAS_SHORTCUT_PANE_BG_COLOR, StringConverter.asString(new RGB(0, 0, 0)));
	}

}
