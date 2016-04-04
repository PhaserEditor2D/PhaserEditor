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

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

public abstract class NewPhaserSourceFileWizard extends Wizard implements
		INewWizard {

	private IStructuredSelection _selection;
	private PhaserSourceFileWizardPage _page;
	private IWorkbenchPage _windowPage;
	private String _pageTitle;
	private String _pageDescription;

	public NewPhaserSourceFileWizard(String pageTitle, String pageDescription) {
		setWindowTitle("New File");
		_pageTitle = pageTitle;
		_pageDescription = pageDescription;
	}

	@Override
	public void addPages() {
		_page = createPage(_selection, _pageTitle, _pageDescription);
		addPage(_page);
	}

	protected PhaserSourceFileWizardPage createPage(
			IStructuredSelection selection, String pageTitle,
			String pageDescription) {
		return new PhaserSourceFileWizardPage(selection, pageTitle,
				pageDescription) {

			@Override
			protected String getTemplatePath() {
				return NewPhaserSourceFileWizard.this.getTemplatePath();
			}
			
		};
	}

	protected abstract String getTemplatePath();

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		_selection = selection;
		_windowPage = workbench.getActiveWorkbenchWindow().getActivePage();
	}

	@Override
	public boolean performFinish() {
		boolean performedOK = false;

		// no file extension specified so add default extension
		String fileName = _page.getFileName();
		if (fileName.lastIndexOf('.') == -1) {
			String newFileName = fileName + ".js";
			_page.setFileName(newFileName);
		}

		// create a new empty file
		IFile file = _page.createNewFile();
		// if there was problem with creating file, it will be null, so make
		// sure to check
		if (file != null) {
			// open the file in editor
			try {
				IDE.openEditor(_windowPage, file);
			} catch (PartInitException e) {
				throw new RuntimeException(e);
			}

			// everything's fine
			performedOK = true;
		}

		return performedOK;
	}

}
