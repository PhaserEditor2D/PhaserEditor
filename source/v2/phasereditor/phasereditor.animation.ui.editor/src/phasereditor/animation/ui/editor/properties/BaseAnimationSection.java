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
package phasereditor.animation.ui.editor.properties;

import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import phasereditor.animation.ui.editor.AnimationsEditor;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
public abstract class BaseAnimationSection<T> extends FormPropertySection<T> {

	private AnimationsEditor _editor;

	public BaseAnimationSection(AnimationsEditor editor, String name) {
		super(name);

		_editor = editor;
	}

	public AnimationsEditor getEditor() {
		return _editor;
	}
	
	protected void restartPlayback() {
		var editor = getEditor();
		
		if (!editor.isStopped()) {
			editor.getStopAction().run();
			editor.getPlayAction().run();
		}
	}
	
	@Override
	public void fillToolbar(ToolBarManager manager) {
		var editor = getEditor();
		
		manager.add(editor.getPlayAction());
		manager.add(editor.getPauseAction());
		manager.add(editor.getStopAction());
		manager.add(new Separator());
		manager.add(editor.getDeleteAction());
	}
	
}
