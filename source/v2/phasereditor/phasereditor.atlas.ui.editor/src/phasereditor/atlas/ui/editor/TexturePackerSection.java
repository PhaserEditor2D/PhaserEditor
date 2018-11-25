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
package phasereditor.atlas.ui.editor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;

import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
public abstract class TexturePackerSection<T> extends FormPropertySection<T> {

	private TexturePackerEditor _editor;
	private Action _buildAction;
	private Action _settingsAction;

	public TexturePackerSection(String name, TexturePackerEditor editor) {
		super(name);

		_editor = editor;
	}

	public TexturePackerEditor getEditor() {
		return _editor;
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {
		manager.add(_settingsAction);
		manager.add(_buildAction);
	}

	protected void createActions() {
		_buildAction = new Action("Build Atlas", EditorSharedImages.getImageDescriptor(IMG_BUILD)) {
			@Override
			public void run() {
				getEditor().manuallyBuild();
			}
		};
		_settingsAction = new Action("Settings", EditorSharedImages.getImageDescriptor(IMG_SETTINGS)) {
			@Override
			public void run() {
				getEditor().selectSettings();
			}
		};
	}

	protected Action getBuildAction() {
		return _buildAction;
	}
	
	protected Action getSettingsAction() {
		return _settingsAction;
	}

}
