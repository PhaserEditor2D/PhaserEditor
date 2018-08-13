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

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.ui.wizards.NewAssetPackWizard;

/**
 * @author arian
 *
 */
public class NewAssetPackWizardLauncher extends NewWizardLancher {

	public NewAssetPackWizardLauncher() {
		super("Create a new Asset Pack file.");
	}

	@Override
	protected INewWizard getWizard() {
		return new NewAssetPackWizard();
	}

	@Override
	protected IStructuredSelection getSelection(IProject project) {

		var packs = AssetPackCore.getAssetPackModels(project);
		if (!packs.isEmpty()) {
			var file = packs.stream().map(p -> p.getFile()).sorted(this::compare_getNewerFile).findFirst().get();
			return new StructuredSelection(file.getParent());
		}

		return super.getSelection(project);
	}

}
