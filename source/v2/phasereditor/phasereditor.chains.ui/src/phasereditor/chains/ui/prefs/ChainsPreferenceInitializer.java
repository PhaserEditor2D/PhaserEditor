package phasereditor.chains.ui.prefs;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import phasereditor.chains.ui.ChainsUI;

public class ChainsPreferenceInitializer extends AbstractPreferenceInitializer {

	public ChainsPreferenceInitializer() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = ChainsUI.getPreferenceStore();

		RGB rgb;

		rgb = Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION).getRGB();
		store.setDefault(ChainsUI.PREF_PROP_HIGHLIGHT_BG_COLOR, StringConverter.asString(rgb));
		rgb = Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT).getRGB();
		store.setDefault(ChainsUI.PREF_PROP_HIGHLIGHT_FG_COLOR, StringConverter.asString(rgb));
		rgb = new RGB(154, 131, 80);
		store.setDefault(ChainsUI.PREF_PROP_TYPE_PART_FG_COLOR, StringConverter.asString(rgb));

	}

}
