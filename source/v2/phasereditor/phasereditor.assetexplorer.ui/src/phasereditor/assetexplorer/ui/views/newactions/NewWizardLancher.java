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
package phasereditor.assetexplorer.ui.views.newactions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * @author arian
 *
 */
public abstract class NewWizardLancher {
	private String _description;
	private String _name;
	private Image _image;

	protected abstract INewWizard getWizard();

	public NewWizardLancher(String name, String description, Image image) {
		_name = name;
		_description = description;
		_image = image;
	}

	public Image getImage() {
		return _image;
	}

	public String getName() {
		return _name;
	}

	public String getDescription() {
		return _description;
	}

	@SuppressWarnings("static-method")
	public int compare_getNewerFile(IResource a, IResource b) {
		return -Long.compare(a.getLocalTimeStamp(), b.getLocalTimeStamp());
	}

	@SuppressWarnings("static-method")
	protected IStructuredSelection getSelection(IProject project) {
		if (project == null) {
			return StructuredSelection.EMPTY;
		}

		return new StructuredSelection(project);
	}

	@Override
	public String toString() {
		return _description;
	}

	public void openWizard(IProject project) {

		IStructuredSelection sel = getSelection(project);

		if (sel == null) {
			sel = StructuredSelection.EMPTY;
		}

		INewWizard wizard = getWizard();

		IWorkbench wb = PlatformUI.getWorkbench();

		wizard.init(wb, sel);
		var shell = wb.getActiveWorkbenchWindow().getShell();
		var dlg = new WizardDialog(shell, wizard);

		dlg.open();
	}

}
