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
package phasereditor.project.core.prefs;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import phasereditor.project.core.ProjectCore;
import phasereditor.project.core.codegen.SourceLang;

/**
 * @author arian
 *
 */
public class PhaserProjectPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(ProjectCore.getPreferenceStore());
		setTitle("Configure default project settigs:");
	}

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();

		addField(new IntegerFieldEditor(ProjectCore.PREF_PROP_PROJECT_GAME_WIDTH, "Default game width", parent));
		addField(new IntegerFieldEditor(ProjectCore.PREF_PROP_PROJECT_GAME_HEIGHT, "Default game height", parent));
		addField(new ComboFieldEditor(ProjectCore.PREF_PROP_PROJECT_WIZARD_LANGUAJE, "Default language", new String[][] {

				{ SourceLang.JAVA_SCRIPT_6.getDisplayName(), SourceLang.JAVA_SCRIPT_6.name() },
				{ SourceLang.TYPE_SCRIPT.getDisplayName(), SourceLang.TYPE_SCRIPT.name() }

		}, parent));
	}

}
