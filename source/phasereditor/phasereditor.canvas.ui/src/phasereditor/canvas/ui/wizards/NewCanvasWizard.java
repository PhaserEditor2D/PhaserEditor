// The MIT License (MIT)
//
// Copyright (c) 2016 Arian Fornaris
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
package phasereditor.canvas.ui.wizards;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.json.JSONException;
import org.json.JSONObject;

import phasereditor.canvas.ui.editors.CanvasEditorModel;

/**
 * @author arian
 *
 */
public class NewCanvasWizard extends Wizard implements INewWizard {

	private IStructuredSelection _selection;
	private CanvasNewFilePage _newCanvasPage;
	private IWorkbenchPage _windowPage;

	public NewCanvasWizard() {
		setWindowTitle("New Canvas File");
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		_selection = selection;
		_windowPage = workbench.getActiveWorkbenchWindow().getActivePage();
	}

	@Override
	public void addPages() {
		_newCanvasPage = new CanvasNewFilePage(_selection);
		addPage(_newCanvasPage);
	}

	@Override
	public boolean performFinish() {
		boolean performedOK = false;

		// no file extension specified so add default extension
		String fileName = _newCanvasPage.getFileName();
		if (fileName.lastIndexOf('.') == -1) {
			String newFileName = fileName + ".canvas";
			_newCanvasPage.setFileName(newFileName);
		}

		// create a new empty file
		IFile file = _newCanvasPage.createNewFile();
		// if there was problem with creating file, it will be null, so make
		// sure to check
		if (file != null) {

			{
				// set default content
				CanvasEditorModel model = new CanvasEditorModel(null);
				String name = _newCanvasPage.getFileName();
				name = name.substring(0, name.length() - _newCanvasPage.getFileExtension().length() - 1);
				model.getWorld().setEditorName(name);
				JSONObject obj = new JSONObject();
				model.write(obj);
				try {
					file.setContents(new ByteArrayInputStream(obj.toString(2).getBytes()), false, false,
							new NullProgressMonitor());
				} catch (JSONException | CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}

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
