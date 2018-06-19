// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.chains.ui;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class ChainsUI {

	public static final String PREF_PROP_HIGHLIGHT_BG_COLOR = "phasereditor.chains.highlightBgColor";
	public static final String PREF_PROP_HIGHLIGHT_FG_COLOR = "phasereditor.chains.highlightFgColor";
	public static final String PREF_PROP_SECONDARY_FG_COLOR = "phasereditor.chains.secondaryFgColor";
	public static Color _PREF_PROP_HIGHLIGHT_BG_COLOR;
	public static Color _PREF_PROP_HIGHLIGHT_FG_COLOR;
	public static Color _PREF_PROP_SECONDARY_FG_COLOR;

	public static void listenPreferences() {
		{
			RGB rgb = StringConverter.asRGB(getPreferenceStore().getString(PREF_PROP_HIGHLIGHT_BG_COLOR));
			_PREF_PROP_HIGHLIGHT_BG_COLOR = SWTResourceManager.getColor(rgb);
			rgb = StringConverter.asRGB(getPreferenceStore().getString(PREF_PROP_HIGHLIGHT_FG_COLOR));
			_PREF_PROP_HIGHLIGHT_FG_COLOR = SWTResourceManager.getColor(rgb);
			rgb = StringConverter.asRGB(getPreferenceStore().getString(PREF_PROP_SECONDARY_FG_COLOR));
			_PREF_PROP_SECONDARY_FG_COLOR = SWTResourceManager.getColor(rgb);
		}

		getPreferenceStore().addPropertyChangeListener(event -> {

			String prop = event.getProperty();

			switch (prop) {

			case PREF_PROP_HIGHLIGHT_BG_COLOR:
				_PREF_PROP_HIGHLIGHT_BG_COLOR = SWTResourceManager.getColor(PhaserEditorUI.getRGBFromPrefEvent(event));
				break;
			case PREF_PROP_HIGHLIGHT_FG_COLOR:
				_PREF_PROP_HIGHLIGHT_FG_COLOR = SWTResourceManager.getColor(PhaserEditorUI.getRGBFromPrefEvent(event));
				break;
			case PREF_PROP_SECONDARY_FG_COLOR:
				_PREF_PROP_SECONDARY_FG_COLOR = SWTResourceManager.getColor(PhaserEditorUI.getRGBFromPrefEvent(event));
				break;
			default:
				break;
			}
		});
	}

	public static Color get_pref_Chains_highlightBgColor() {
		return _PREF_PROP_HIGHLIGHT_BG_COLOR;
	}

	public static Color get_pref_Chains_highlightFgColor() {
		return _PREF_PROP_HIGHLIGHT_FG_COLOR;
	}
	
	public static Color get_pref_Chains_secondaryFgColor() {
		return _PREF_PROP_SECONDARY_FG_COLOR;
	}

	public static IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

}
