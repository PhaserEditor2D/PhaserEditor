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

import org.eclipse.core.resources.IFile;
import org.json.JSONObject;

import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.core.GroupModel;

/**
 * @author arian
 *
 */
public class NewWizard_Group extends NewWizard_Base {

	public NewWizard_Group() {
		super(CanvasType.GROUP);
	}

	private NewPage_GroupSettings _settingsPage;

	@Override
	protected boolean isCanvasFileDesired() {
		return _settingsPage.isGenerateCanvasFile();
	}

	@Override
	protected JSONObject createFinalModelJSON(IFile file) {
		GroupModel model = new GroupModel(getModel().getWorld());
		getModel().getWorld().addChild(model);
		return super.createFinalModelJSON(file);
	}

	@Override
	public void addPages() {
		super.addPages();
		_settingsPage = new NewPage_GroupSettings();
		_settingsPage.setSettings(getModel().getSettings());
		addPage(_settingsPage);
	}
}
