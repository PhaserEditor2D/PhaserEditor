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
package phasereditor.project.ui.wizards;

import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.ui.wizards.IJsGlobalScopeContainerPage;
import org.eclipse.wst.jsdt.ui.wizards.IJsGlobalScopeContainerPageExtension;
import org.eclipse.wst.jsdt.ui.wizards.NewElementWizardPage;

import phasereditor.project.core.ProjectCore;

public class ECMA5LibraryWizardPage extends NewElementWizardPage
		implements IJsGlobalScopeContainerPage, IJsGlobalScopeContainerPageExtension {

	private IIncludePathEntry _selection;

	public ECMA5LibraryWizardPage() {
		super("ecma5.library.wizard");
	}

	@Override
	public void createControl(Composite parent) {
		setTitle("ECMA 5 API Library");
		setDescription("ECMA 5 API Library");
		Label label = new Label(parent, SWT.NONE);
		label.setText("ECMA5 5 API support.");

		setControl(label);
	}

	@Override
	public boolean finish() {
		return true;
	}

	@Override
	public IIncludePathEntry getSelection() {
		return _selection;
	}

	@Override
	public void setSelection(IIncludePathEntry containerEntry) {
		// it is null when it is adding the library the first time.
		if (containerEntry != null) {
			_selection = containerEntry;
		}
	}

	@Override
	public void initialize(IJavaScriptProject project, IIncludePathEntry[] currentEntries) {
		_selection = JavaScriptCore.newContainerEntry(new Path(ProjectCore.ECMA5_SCOPE_INITIALIZER_ID));
	}

}
