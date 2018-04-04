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
package phasereditor.ui.prefs;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class TilemapPreviewPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();

		// Composite group = createGroup(parent);

		addField(new ColorFieldEditor(PhaserEditorUI.PREF_PROP_PREVIEW_TILEMAP_OVER_TILE_BORDER_COLOR,
				"Border color or the tile under the cursor", parent));

		addField(new ColorFieldEditor(PhaserEditorUI.PREF_PROP_PREVIEW_TILEMAP_LABELS_COLOR, "Tile index label color",
				parent));

		addField(new ColorFieldEditor(PhaserEditorUI.PREF_PROP_PREVIEW_TILEMAP_SELECTION_BG_COLOR,
				"Selected tile background color", parent));

		addField(new IntegerFieldEditor(PhaserEditorUI.PREF_PROP_PREVIEW_TILEMAP_TILE_WIDTH, "Default tile width (CSV)",
				parent));
		addField(new IntegerFieldEditor(PhaserEditorUI.PREF_PROP_PREVIEW_TILEMAP_TILE_HEIGHT,
				"Default tile height (CSV)", parent));
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(PhaserEditorUI.getPreferenceStore());
		setDescription("Configure the tilemap preview:");
	}

}
