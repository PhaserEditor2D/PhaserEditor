// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class ExternalEditorPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	public ExternalEditorPreferencePage() {
		super(GRID);
		setDescription("Set the path and arhuments to open the external editor.\n"
				+ "You can use the variables ${project}, ${file} and ${line} where needed.");
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(PhaserEditorUI.getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {
		var parent = getFieldEditorParent();
		addField(new BooleanFieldEditor(PhaserEditorUI.PREF_PROP_ALIEN_EDITOR_ENABLED, "Open source files in an external editor.", parent));
		addField(new StringFieldEditor(PhaserEditorUI.PREF_PROP_ALIEN_EDITOR_PROGRAM, "Program Path", parent));
		addField(new StringFieldEditor(PhaserEditorUI.PREF_PROP_ALIEN_EDITOR_COMMON_ARGS, "Common arguments", parent));
		addField(new StringFieldEditor(PhaserEditorUI.PREF_PROP_ALIEN_EDITOR_PROJECT_ARGS, "Arguments to open a project", parent));
		addField(new StringFieldEditor(PhaserEditorUI.PREF_PROP_ALIEN_EDITOR_FILE_ARGS, "Arguments to open a file", parent));
		addField(new StringFieldEditor(PhaserEditorUI.PREF_PROP_ALIEN_EDITOR_FILE_LINE_ARGS, "Arguments to open a file at a line", parent));
	}

}
