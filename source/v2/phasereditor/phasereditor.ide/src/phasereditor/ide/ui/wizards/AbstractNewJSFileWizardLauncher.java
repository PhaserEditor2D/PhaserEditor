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
package phasereditor.ide.ui.wizards;

import static phasereditor.ui.IEditorSharedImages.IMG_NEW_GENERIC_EDITOR;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import phasereditor.project.core.ProjectCore;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public abstract class AbstractNewJSFileWizardLauncher extends NewWizardLancher{

	public AbstractNewJSFileWizardLauncher(String name, String description) {
		super(name, description, EditorSharedImages.getImage(IMG_NEW_GENERIC_EDITOR));
	}
	
	@Override
	protected IStructuredSelection getSelection(IProject project) {
		var folder = ProjectCore.getWebContentFolder(project);
		var files = new ArrayList<IFile>();
		try {
			folder.accept(r -> {
				if (r instanceof IFile && r.getFullPath().getFileExtension().equals("js")) {
					files.add((IFile) r);
				}
				return true;
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}

		if (!files.isEmpty()) {
			files.sort(this::compare_getNewerFile);
			folder = files.get(0).getParent();
		}

		return new StructuredSelection(folder);
	}

}
