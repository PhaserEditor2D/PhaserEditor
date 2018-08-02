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
package phasereditor.animation.ui.properties;

import phasereditor.animation.ui.AnimationsEditor;
import phasereditor.assetpack.ui.animations.AnimationCanvas;
import phasereditor.ui.properties.PGridModel;

/**
 * @author arian
 *
 */
public class BaseAnimationPGridModel extends PGridModel {

	private AnimationsPGridPage _propertyPage;

	public void setPropertyPage(AnimationsPGridPage propertyPage) {
		_propertyPage = propertyPage;
	}

	public AnimationsPGridPage getPropertyPage() {
		return _propertyPage;
	}

	protected void refreshGrid() {
		_propertyPage.getGrid().refresh();
	}

	public AnimationsEditor getEditor() {
		return _propertyPage.getEditor();
	}

	protected void updateAndRestartAnimation() {
		AnimationsEditor editor = getEditor();

		AnimationCanvas animCanvas = editor.getAnimationCanvas();
		var running = !animCanvas.isStopped();

		animCanvas.stop();

		var anim = editor.getTimelineCanvas().getAnimation();

		anim.buildTiming();

		editor.getTimelineCanvas().redraw();

		if (running) {
			animCanvas.play();
		}
	}
}