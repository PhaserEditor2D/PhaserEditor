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
package phasereditor.project.ui.wizards;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import phasereditor.project.core.ProjectCore;
import phasereditor.project.core.codegen.SourceLang;

/**
 * @author arian
 *
 */
public abstract class SelectLangPage extends WizardPage {

	private ComboViewer _comboLang;

	public SelectLangPage() {
		super("selectLangPage");

		setMessage("Select the language");
		setTitle("Language");
		setDescription("Source code language");

	}

	@Override
	public void createControl(Composite parent) {
		var comp = new Composite(parent, SWT.None);
		comp.setLayout(new GridLayout(2, false));

		var label = new Label(comp, SWT.NONE);
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		label.setText("Language");

		_comboLang = new ComboViewer(comp, SWT.READ_ONLY);
		var combo = _comboLang.getCombo();
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		_comboLang.setContentProvider(new ArrayContentProvider());
		_comboLang.setLabelProvider(new SourceLangLabelProvider());

		_comboLang.setInput(new Object[] { SourceLang.JAVA_SCRIPT_6, SourceLang.TYPE_SCRIPT });
		_comboLang.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				SelectLangPage.this.langChanged((SourceLang) _comboLang.getStructuredSelection().toArray()[0]);
			}
		});
		_comboLang
				.setSelection(new StructuredSelection(ProjectCore.getProjectLanguage(ProjectCore.getActiveProject())));

		setControl(comp);
	}

	protected abstract void langChanged(SourceLang lang);

}
