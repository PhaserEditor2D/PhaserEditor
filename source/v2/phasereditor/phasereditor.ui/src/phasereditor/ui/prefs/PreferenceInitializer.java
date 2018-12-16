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
		RGB _RED = new RGB(255, 0, 0);
		RGB _BLUE = new RGB(0, 0, 255);
		RGB _YELLOW = new RGB(255, 255, 0);
//		RGB _GRAY = new RGB(192, 192, 192);

		IPreferenceStore store = PhaserEditorUI.getPreferenceStore();
		store.setDefault(PhaserEditorUI.PREF_PROP_COLOR_DIALOG_TYPE, PhaserEditorUI.PREF_VALUE_COLOR_DIALOG_NATIVE);

		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_ANTIALIAS, false);
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_IMG_PAINT_BG_TYPE,
				PhaserEditorUI.PREF_VALUE_PREVIEW_IMG_PAINT_BG_TYPE_TRANSPARENT);
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_IMG_PAINT_BG_SOLID_COLOR,
				StringConverter.asString(new RGB(180, 180, 180)));
		
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_IMG_PAINT_BG_COLOR_1, StringConverter.asString(PhaserEditorUI.getListTextColor().getRGB()));

		// spritesheet

		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_SPRITESHEET_PAINT_FRAMES, true);
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_SPRITESHEET_PAINT_LABELS, true);
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_SPRITESHEET_FRAMES_BORDER_COLOR,
				StringConverter.asString(_RED));
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_SPRITESHEET_LABELS_COLOR, StringConverter.asString(_YELLOW));

		// tilemap

		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_TILEMAP_OVER_TILE_BORDER_COLOR,
				StringConverter.asString(_RED));
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_TILEMAP_LABELS_COLOR, StringConverter.asString(_YELLOW));
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_TILEMAP_SELECTION_BG_COLOR, StringConverter.asString(_BLUE));
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_TILEMAP_TILE_WIDTH, 32);
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_TILEMAP_TILE_HEIGHT, 32);
		
		PhaserEditorUI.listenPreferences();
	}

}
