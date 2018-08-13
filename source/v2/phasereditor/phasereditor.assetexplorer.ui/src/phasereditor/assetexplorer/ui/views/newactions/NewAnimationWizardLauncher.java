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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.INewWizard;

import phasereditor.animation.ui.editor.wizards.NewAnimationsFileWizard;
import phasereditor.assetpack.core.AssetPackCore;

/**
 * @author arian
 *
 */
public class NewAnimationWizardLauncher extends NewWizardLancher {

	public NewAnimationWizardLauncher() {
		super("Create a new Animations file.");
	}

	@Override
	protected INewWizard getWizard() {
		return new NewAnimationsFileWizard();
	}

	@Override
	protected IStructuredSelection getSelection(IProject project) {

		var models = AssetPackCore.getAnimationsFileCache().getProjectData(project);

		if (!models.isEmpty()) {

			var file = models.stream().map(a -> a.getFile()).sorted(this::compare_getNewerFile).findFirst().get();

			return new StructuredSelection(file.getParent());
		}

		return super.getSelection(project);
	}

}
