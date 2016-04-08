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

import javafx.collections.ObservableList;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import phasereditor.canvas.core.BaseSpriteShapeModel;
import phasereditor.canvas.ui.editors.ShapeCanvas;
import phasereditor.canvas.ui.editors.grid.PGridModel;
import phasereditor.canvas.ui.editors.grid.PGridNumberProperty;
import phasereditor.canvas.ui.editors.grid.PGridSection;

/**
 * @author arian
 *
 */
public abstract class BaseSpriteShapeControl<T extends BaseSpriteShapeModel> extends BaseObjectControl<T> {

	private PGridNumberProperty _anchor_x_property;
	private PGridNumberProperty _anchor_y_property;

	public BaseSpriteShapeControl(ShapeCanvas canvas, T model) {
		super(canvas, model);
	}

	@Override
	protected void updateTransforms(ObservableList<Transform> transforms) {
		super.updateTransforms(transforms);

		{
			// anchor
			double anchorX = getModel().getAnchorX();
			double anchorY = getModel().getAnchorY();
			double x = -getWidth() * anchorX;
			double y = -getHeight() * anchorY;
			Translate anchor = Transform.translate(x, y);
			transforms.add(anchor);
		}
	}

	@Override
	protected void initPropertyModel(PGridModel propModel) {
		super.initPropertyModel(propModel);

		_anchor_x_property = new PGridNumberProperty("anchor.x") {
			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getAnchorX());
			}

			@Override
			public void setValue(Double value) {
				getModel().setAnchorX(value.doubleValue());
				updateGridChange();
			}

			@Override
			public boolean isModified() {
				return getModel().getAnchorX() != 0;
			}
		};

		_anchor_y_property = new PGridNumberProperty("anchor.y") {
			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getAnchorY());
			}

			@Override
			public void setValue(Double value) {
				getModel().setAnchorY(value.doubleValue());
				updateGridChange();
			}

			@Override
			public boolean isModified() {
				return getModel().getAnchorY() != 0;
			}
		};

		PGridSection spriteSection = new PGridSection("Sprite");

		spriteSection.add(_anchor_x_property);
		spriteSection.add(_anchor_y_property);

		propModel.getSections().add(spriteSection);
	}

	@Override
	public abstract double getWidth();

	@Override
	public abstract double getHeight();

}
