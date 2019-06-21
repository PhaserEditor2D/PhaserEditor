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
package phasereditor.ide.ui.wizards;

import static phasereditor.ui.IEditorSharedImages.IMG_ATLAS_ADD;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.INewWizard;

import phasereditor.atlas.core.AtlasCore;
import phasereditor.atlas.ui.editor.wizards.NewAtlasMakerWizard;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public class NewAtlasWizardLauncher extends NewWizardLancher {

	public NewAtlasWizardLauncher() {
		super("Texture Packer File", "Create a new Textures Packer file.", EditorSharedImages.getImage(IMG_ATLAS_ADD));
	}

	@Override
	protected INewWizard getWizard() {
		return new NewAtlasMakerWizard();
	}

	@Override
	protected IStructuredSelection getSelection(IProject project) {

		var models = AtlasCore.getAtlasFileCache().getProjectData(project);

		if (!models.isEmpty()) {

			var file = models.stream().map(a -> a.getFile()).sorted(this::compare_getNewerFile).findFirst().get();

			return new StructuredSelection(file.getParent());
		}

		return super.getSelection(project);
	}

}
