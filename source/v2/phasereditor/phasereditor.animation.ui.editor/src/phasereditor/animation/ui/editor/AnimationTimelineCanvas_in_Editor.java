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
package phasereditor.animation.ui.editor;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;

import phasereditor.animation.ui.AnimationTimelineCanvas;
import phasereditor.assetpack.core.animations.AnimationModel;

/**
 * @author arian
 *
 */
public class AnimationTimelineCanvas_in_Editor extends AnimationTimelineCanvas<AnimationModel> {

	private AnimationsEditor _editor;

	public AnimationTimelineCanvas_in_Editor(Composite parent, int style) {
		super(parent, style | SWT.H_SCROLL | SWT.NO_REDRAW_RESIZE);
	}

	@Override
	public void clearSelection() {
		super.clearSelection();
		
		if (getModel() != null) {
			getEditor().getEditorSite().getSelectionProvider().setSelection(new StructuredSelection(getModel()));
		}
	}

	@Override
	protected void selectionDropped(Object[] data) {

		var anim = getModel();
		
		if (anim == null) {
			_editor.createAnimationsWithDrop(data);
			return;
		}

		super.selectionDropped(data);

		if (_editor.getAnimationCanvas().getImage() == null) {
			if (!anim.getFrames().isEmpty()) {
				_editor.getAnimationCanvas().showFrame(0);
			}
		}

		if (_editor.getOutliner() != null) {
			_editor.getOutliner().refresh();
		}

		_editor.setDirty();

		redraw();
	}

	public void setEditor(AnimationsEditor editor) {
		_editor = editor;
		setAnimationCanvas(_editor.getAnimationCanvas());
	}

	public AnimationsEditor getEditor() {
		return _editor;
	}

	@Override
	public void mouseDown(MouseEvent e) {
		// nothing
	}

	@Override
	protected void updateSelectionProvider() {
		_editor.getEditorSite().getSelectionProvider().setSelection(new StructuredSelection(getSelectedFrames().toArray()));
	}
}
