// The MIT License (MIT)
//
// Copyright (c) 2016 Arian Fornaris
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
package phasereditor.canvas.ui.shapes;

import phasereditor.canvas.core.AtlasSpriteShapeModel;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.ImageSpriteShapeModel;
import phasereditor.canvas.core.SpritesheetShapeModel;
import phasereditor.canvas.ui.editors.ShapeCanvas;

/**
 * @author arian
 *
 */
public class ShapeFactory {
	public static BaseObjectControl<?> createShapeControl(ShapeCanvas canvas, BaseObjectModel model) {
		if (model instanceof GroupModel) {
			return new GroupControl(canvas, (GroupModel) model);
		} else if (model instanceof ImageSpriteShapeModel) {
			return new ImageSpriteControl(canvas, (ImageSpriteShapeModel) model);
		} else if (model instanceof SpritesheetShapeModel) {
			return new SpritesheetControl(canvas, (SpritesheetShapeModel) model);
		} else if (model instanceof AtlasSpriteShapeModel) {
			return new AtlasSpriteControl(canvas, (AtlasSpriteShapeModel) model);
		}
		return null;
	}
}
