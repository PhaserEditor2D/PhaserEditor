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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.INewWizard;

import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.ui.wizards.NewWizard_Group;
import phasereditor.canvas.ui.wizards.NewWizard_Sprite;
import phasereditor.canvas.ui.wizards.NewWizard_State;

/**
 * @author arian
 *
 */
public class NewCanvasWizardLauncher extends NewWizardLancher {

	private CanvasType _type;

	public NewCanvasWizardLauncher(CanvasType type) {
		super(getLabel(type));
		_type = type;
	}

	@Override
	protected INewWizard getWizard() {
		switch (_type) {
		case SPRITE:
			return new NewWizard_Sprite();
		case GROUP:
			return new NewWizard_Group();
		case STATE:
			return new NewWizard_State();
		default:
			return null;
		}
	}

	@Override
	protected IStructuredSelection getSelection(IProject project) {

		var models = CanvasCore.getCanvasFileCache().getProjectData(project);

		if (!models.isEmpty()) {
			// look for the newer Canvas file of the same type
			var optFile = models.stream().filter(m -> m.getType() == _type).map(a -> a.getFile())
					.sorted(this::compare_getNewerFile).findFirst();
			IFile file = null;

			if (optFile.isPresent()) {
				// ok, there is one, get it.
				file = optFile.get();
			} else {
				// there is not a canvas file of the same type, then get the newer of any type
				file = models.stream().map(a -> a.getFile()).sorted(this::compare_getNewerFile).findFirst().get();
			}

			return new StructuredSelection(file.getParent());
		}

		return super.getSelection(project);
	}

}
