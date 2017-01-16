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
package phasereditor.canvas.core.codegen;

import phasereditor.canvas.core.AnimationModel;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.CanvasModel;

/**
 * @author arian
 *
 */
public abstract class JSLikeBaseSpriteCodeGenerator extends JSLikeCodeGenerator{

	public JSLikeBaseSpriteCodeGenerator(CanvasModel model) {
		super(model);
	}

	
	@Override
	protected void generatePublicFields() {
		BaseSpriteModel sprite = (BaseSpriteModel) _model.getWorld().findFirstSprite();
		if (sprite == null) {
			return;
		}
		generatePublicField(sprite);
	}

	@Override
	protected void generateObjectCreation() {
		BaseSpriteModel sprite = (BaseSpriteModel) _model.getWorld().findFirstSprite();
		if (sprite == null) {
			return;
		}

		generateProperties(sprite);
	}

	@Override
	protected String getVarName(BaseObjectModel model) {
		return "this";
	}

	@Override
	protected String getAnimationVarName(BaseObjectModel obj, AnimationModel anim) {
		return "anim_" + anim.getName();
	}
}
