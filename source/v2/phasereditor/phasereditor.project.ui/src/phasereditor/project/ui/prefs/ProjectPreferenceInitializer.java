package phasereditor.project.ui.prefs;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import phasereditor.project.core.codegen.SourceLang;
import phasereditor.project.ui.ProjectUI;

public class ProjectPreferenceInitializer extends AbstractPreferenceInitializer {

	public ProjectPreferenceInitializer() {
	}

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = ProjectUI.getPreferenceStore();
		store.setDefault(ProjectUI.PREF_PROP_PROJECT_WIZARD_GAME_WIDTH, 800);
		store.setDefault(ProjectUI.PREF_PROP_PROJECT_WIZARD_GAME_HEIGHT, 600);
		store.setDefault(ProjectUI.PREF_PROP_PROJECT_WIZARD_LANGUAJE, SourceLang.JAVA_SCRIPT.name());
	}
}
