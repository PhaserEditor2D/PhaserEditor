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
package phasereditor.assetpack.ui;

import static phasereditor.ui.IEditorSharedImages.IMG_FILM_GO;

import org.eclipse.jface.action.Action;

import phasereditor.animation.ui.AnimationUI;
import phasereditor.assetpack.core.animations.AnimationsModel;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public abstract class ShowAnimationsInAnimationsEditorAction extends Action {
	public ShowAnimationsInAnimationsEditorAction() {
		super("Show these animations in the Animations editor.", EditorSharedImages.getImageDescriptor(IMG_FILM_GO));
	}

	@Override
	public void run() {
		AnimationUI.showAnimationsInEditor(getAnimations());
	}

	protected abstract AnimationsModel getAnimations();
}
