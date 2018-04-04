// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.optipng.ui;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import phasereditor.optipng.core.OptiPNGCore;

public class OptiPNGPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public OptiPNGPreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(OptiPNGCore.getPreferenceStore());
		setDescription("Configure OptiPNG settings. For a better understanding visit the http://optipng.sourceforge.net.");
	}

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();

		{
			String[][] values = new String[8][];
			for (int i = 0; i < 8; i++) {
				String label = (i == 0 ? "0 or 1" : Integer.valueOf(i))
						+ " trials";
				values[i] = new String[] { label, "-o" + i };
			}

			ComboFieldEditor levelEditor = new ComboFieldEditor(
					OptiPNGCore.PREF_OPTI_PNG_LEVEL, "Optimization Level",
					values, parent);
			addField(levelEditor);
		}

		{
			StringFieldEditor paramsEditor = new StringFieldEditor(
					OptiPNGCore.PREF_OPTI_PNG_EXTRA_PARAMS,
					"Additional Parameters", parent);
			addField(paramsEditor);
		}
	}
}