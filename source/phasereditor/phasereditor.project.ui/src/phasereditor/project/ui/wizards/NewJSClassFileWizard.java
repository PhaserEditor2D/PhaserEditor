// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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

/**
 * @author arian
 *
 */
public class NewJSClassFileWizard extends Wizard implements INewWizard {

	private IStructuredSelection _selection;
	private NewJSClassFileWizardPage _filePage;
	private IWorkbenchPage _windowPage;
	private NewJSClassFileSettingsWizardPage _settingsPage;

	public NewJSClassFileWizard() {
		setWindowTitle("New JavaScript Class");
	}

	@Override
	public void addPages() {
		_filePage = new NewJSClassFileWizardPage(_selection);
		_settingsPage = new NewJSClassFileSettingsWizardPage();
		
		addPage(_filePage);
		addPage(_settingsPage);
	}

	public NewJSClassFileWizardPage getFilePage() {
		return _filePage;
	}
	
	public NewJSClassFileSettingsWizardPage getSettingsPage() {
		return _settingsPage;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		_selection = selection;
		_windowPage = workbench.getActiveWorkbenchWindow().getActivePage();
	}

	@Override
	public boolean performFinish() {
		boolean performedOK = false;

		// no file extension specified so add default extension
		String fileName = _filePage.getFileName();
		if (fileName.lastIndexOf('.') == -1) {
			String newFileName = fileName + "." + _filePage.getFileExtension();
			_filePage.setFileName(newFileName);
		}

		// create a new empty file
		IFile file = _filePage.createNewFile();
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
