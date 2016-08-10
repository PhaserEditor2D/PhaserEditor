// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.ui.editors.edithandlers;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.ISpriteNode;

/**
 * @author arian
 *
 */
public class AnchorHandlerNode extends PathHandlerNode {

	private double _initAnchorX;
	private double _initAnchorY;
	private double _initX;
	private double _initY;
	private double _initAnchorOffsetDX;
	private double _initAnchorOffsetDY;

	public AnchorHandlerNode(ISpriteNode object) {
		super(object);
		setFill(Color.BLUE);
		setCursor(Cursor.MOVE);
	}

	@Override
	public void handleLocalStart(double localX, double localY) {
		ISpriteNode sprite = (ISpriteNode) _object;
		BaseSpriteModel model = sprite.getModel();
		_initX = model.getX();
		_initY = model.getY();
		_initAnchorX = model.getAnchorX();
		_initAnchorY = model.getAnchorY();
		_initAnchorOffsetDX = model.getAnchorX() * _control.getTextureWidth();
		_initAnchorOffsetDY = model.getAnchorY() * _control.getTextureHeight();
	}

	@Override
	public void handleLocalDrag(double dx, double dy) {
		ISpriteNode sprite = (ISpriteNode) _object;
		BaseSpriteModel model = sprite.getModel();

		model.setAnchorX(_initAnchorX + dx / _control.getTextureWidth());
		model.setAnchorY(_initAnchorY + dy / _control.getTextureHeight());

		double anchorOffsetDX = model.getAnchorX() * _control.getTextureWidth();
		double anchorOffsetDY = model.getAnchorY() * _control.getTextureHeight();

		double dx2 = anchorOffsetDX - _initAnchorOffsetDX;
		double dy2 = anchorOffsetDY - _initAnchorOffsetDY;

		Point2D p = new Point2D(dx2, dy2);
		for (Transform t : _node.getTransforms()) {
			p = t.deltaTransform(p);
		}

		model.setX(_initX + p.getX());
		model.setY(_initY + p.getY());

	}

	@Override
	public void handleDone() {
		CompositeOperation operations = new CompositeOperation();
		ISpriteNode sprite = (ISpriteNode) _object;
		BaseSpriteModel model = sprite.getModel();

		double x = model.getX();
		double y = model.getY();
		double anchorX = model.getAnchorX();
		double anchorY = model.getAnchorY();

		model.setAnchorX(_initX);
		model.setAnchorY(_initY);
		model.setAnchorX(_initAnchorX);
		model.setAnchorY(_initAnchorY);

		String id = model.getId();

		operations.add(new ChangePropertyOperation<Number>(id, "x", Double.valueOf(x)));
		operations.add(new ChangePropertyOperation<Number>(id, "y", Double.valueOf(y)));
		operations.add(new ChangePropertyOperation<Number>(id, "anchor.x", Double.valueOf(anchorX)));
		operations.add(new ChangePropertyOperation<Number>(id, "anchor.y", Double.valueOf(anchorY)));

		_canvas.getUpdateBehavior().executeOperations(operations);
	}

	@Override
	public void updateHandler() {
		BaseSpriteModel model = (BaseSpriteModel) _model;

		double x = model.getAnchorX() * _control.getTextureWidth();
		double y = model.getAnchorY() * _control.getTextureHeight();

		Point2D p = objectToScene(x, y);
		relocate(p.getX() - 5, p.getY() - 5);
	}

}
