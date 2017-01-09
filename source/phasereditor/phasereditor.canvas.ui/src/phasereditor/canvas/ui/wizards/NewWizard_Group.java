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
package phasereditor.canvas.ui.wizards;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.json.JSONException;
import org.json.JSONObject;

import phasereditor.canvas.core.CanvasMainSettings;
import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.codegen.ICodeGenerator;

/**
 * @author arian
 *
 */
public class NewWizard_Group extends Wizard implements INewWizard {

	private IStructuredSelection _selection;
	private CanvasModel _model;
	private NewPage_GroupFile _filePage;
	private NewPage_GroupSettings _settingsPage;
	private IWorkbenchPage _windowPage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		_selection = selection;
		_windowPage = workbench.getActiveWorkbenchWindow().getActivePage();
	}

	@Override
	public void addPages() {
		_model = new CanvasModel(null);

		_filePage = new NewPage_GroupFile(_selection);
		_settingsPage = new NewPage_GroupSettings();

		_filePage.setModel(_model);
		_settingsPage.setSettings(_model.getSettings());

		addPage(_filePage);
		addPage(_settingsPage);
	}

	@Override
	public boolean performFinish() {
		boolean performedOK = false;

		// no file extension specified so add default extension
		String fileName = _filePage.getFileName();
		if (fileName.lastIndexOf('.') == -1) {
			String newFileName = fileName + ".canvas";
			_filePage.setFileName(newFileName);
		}

		// create a new empty file
		IFile file = _filePage.createNewFile();
		// if there was problem with creating file, it will be null, so make
		// sure to check
		if (file != null) {

			{
				// set default content
				String name = _filePage.getFileName();
				name = name.substring(0, name.length() - _filePage.getFileExtension().length() - 1);
				_model.getWorld().setEditorName(name);
				JSONObject obj = new JSONObject();
				_model.write(obj);
				try {
					file.setContents(new ByteArrayInputStream(obj.toString(2).getBytes()), false, false,
							new NullProgressMonitor());
				} catch (JSONException | CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}

			{
				// generate source code
				createSourceFile(file);
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

	private void createSourceFile(IFile canvasFile) {
		try {

			CanvasMainSettings settings = _model.getSettings();

			String canvasFilenamePart;
			{
				canvasFilenamePart = canvasFile.getName();
				String ext = canvasFile.getFileExtension();
				int end = canvasFilenamePart.length() - ext.length() - 1;
				canvasFilenamePart = canvasFilenamePart.substring(0, end);
			}

			String fname = canvasFilenamePart + "." + settings.getLang().getExtension();
			IFile srcFile = canvasFile.getParent().getFile(new Path(fname));
			String replace = null;

			if (srcFile.exists()) {
				byte[] bytes = Files.readAllBytes(srcFile.getLocation().makeAbsolute().toFile().toPath());
				replace = new String(bytes);
			}

			ICodeGenerator generator = settings.getLang().getCodeGenerator();
			String content = generator.generate(_model.getWorld(), replace);

			ByteArrayInputStream stream = new ByteArrayInputStream(content.getBytes());
			if (srcFile.exists()) {
				srcFile.setContents(stream, IResource.NONE, null);
			} else {
				srcFile.create(stream, false, null);
			}
			srcFile.refreshLocal(1, null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
