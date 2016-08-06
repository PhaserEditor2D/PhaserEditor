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

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class ScaleHandlerNode extends PathHandlerNode {

	private Axis _axis;
	private double _initScaleX;
	private double _initScaleY;
	private double _initWidth;
	private double _initHeight;
	private double _scaledInitWidth;
	private double _scaledInitHeight;

	public ScaleHandlerNode(IObjectNode object, Axis axis) {
		super(object);
		_axis = axis;
		setFill(Color.AQUAMARINE);
	}

	@Override
	public void handleMousePressed(MouseEvent e) {
		super.handleMousePressed(e);

		Bounds bounds = _node.getBoundsInLocal();

		_initScaleX = _model.getScaleX();
		_initScaleY = _model.getScaleY();
		_initWidth = bounds.getWidth();
		_initHeight = bounds.getHeight();
		_scaledInitWidth = _initWidth * _model.getScaleX();
		_scaledInitHeight = _initHeight * _model.getScaleY();
	}

	@Override
	protected void handleDrag(double dx, double dy) {
		if (_axis.changeW()) {
			double x = (_scaledInitWidth + dx * _model.getScaleX()) / _initWidth;
			_model.setScaleX(x);
		}

		if (_axis.changeH()) {
			double y = (_scaledInitHeight + dy * _model.getScaleY()) / _initHeight;
			_model.setScaleY(y);
		}
	}

	@Override
	protected void handleDone() {
		double x = _model.getScaleX();
		double y = _model.getScaleY();

		_model.setScaleX(_initScaleX);
		_model.setScaleY(_initScaleY);

		String id = _model.getId();

		_canvas.getUpdateBehavior().executeOperations(

		new CompositeOperation(

		new ChangePropertyOperation<Number>(id, "scale.x", Double.valueOf(x)),

		new ChangePropertyOperation<Number>(id, "scale.y", Double.valueOf(y))

		));
	}

	@Override
	public void updateHandler() {
		double x = _axis.x * _control.getTextureWidth();
		double y = _axis.y * _control.getTextureHeight();

		Point2D p = objectToScene(_object, x, y);
		relocate(p.getX() - 5, p.getY() - 5);

		setCursor(_axis.getResizeCursor(_object));
	}

}
