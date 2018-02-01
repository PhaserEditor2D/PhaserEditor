package phasereditor.ui.prefs;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.RGB;

import phasereditor.ui.PhaserEditorUI;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public PreferenceInitializer() {
	}

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = PhaserEditorUI.getPreferenceStore();
		store.setDefault(PhaserEditorUI.PREF_PROP_COLOR_DIALOG_TYPE, PhaserEditorUI.PREF_VALUE_COLOR_DIALOG_NATIVE);
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_IMG_PAINT_BG_TYPE,
				PhaserEditorUI.PREF_VALUE_PREVIEW_IMG_PAINT_BG_TYPE_TWO_COLORS);
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_IMG_PAINT_BG_SOLID_COLOR,
				StringConverter.asString(new RGB(180, 180, 180)));
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_IMG_PAINT_BG_COLOR_1,
				StringConverter.asString(new RGB(180, 180, 180)));
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_IMG_PAINT_BG_COLOR_2,
				StringConverter.asString(new RGB(250, 250, 250)));

		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_SPRITESHEET_PAINT_FRAMES, true);
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_SPRITESHEET_PAINT_LABELS, true);
	}

}
