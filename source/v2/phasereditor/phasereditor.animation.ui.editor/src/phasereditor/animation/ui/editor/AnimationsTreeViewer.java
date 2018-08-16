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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;

import phasereditor.animation.ui.AnimationTreeCanvasItemRenderer;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.assetpack.core.animations.AnimationsModel;
import phasereditor.ui.TreeCanvasViewer;
import phasereditor.ui.TreeCanvas;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;

/**
 * @author arian
 *
 */
public class AnimationsTreeViewer extends TreeCanvasViewer {

	public AnimationsTreeViewer(TreeCanvas canvas) {
		super(canvas, new ITreeContentProvider() {

			@Override
			public boolean hasChildren(Object element) {
				return false;
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public Object[] getElements(Object inputElement) {
				return ((AnimationsModel) inputElement).getAnimations().toArray();
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				return new Object[0];
			}
		}, new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((AnimationModel) element).getKey();
			}
		});
	}
	
	@Override
	protected void setItemProperties(TreeCanvasItem item, Object elem) {
		super.setItemProperties(item, elem);
		item.setRenderer(new AnimationTreeCanvasItemRenderer(item));
	}

}
