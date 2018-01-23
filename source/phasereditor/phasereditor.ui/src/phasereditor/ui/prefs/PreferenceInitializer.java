package phasereditor.ui.prefs;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import phasereditor.ui.PhaserEditorUI;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public PreferenceInitializer() {
	}

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = PhaserEditorUI.getPreferenceStore();
		store.setDefault(PhaserEditorUI.PREF_PROP_COLOR_DIALOG_TYPE, PhaserEditorUI.PREF_COLOR_DIALOG_NATIVE_VALUE);

		PhaserEditorUI.listenPreferences();
	}

}
