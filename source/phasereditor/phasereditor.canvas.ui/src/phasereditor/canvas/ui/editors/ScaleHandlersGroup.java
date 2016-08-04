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
package phasereditor.canvas.ui.editors;

import javafx.geometry.Bounds;
import javafx.scene.input.MouseEvent;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;

/**
 * @author arian
 *
 */
public class ScaleHandlersGroup extends HandlersGroup {

	private enum ScaleAxis {
		X(true, false), Y(false, true), XY(true, true);

		public boolean x;
		public boolean y;

		private ScaleAxis(boolean x, boolean y) {
			this.x = x;
			this.y = y;
		}
	}

	private static class ScaleHandlerNode extends DragHandlerNode {

		private ScaleAxis _corner;
		private double _initScaleX;
		private double _initScaleY;
		private double _initWidth;
		private double _initHeight;
		private double _scaledInitWidth;
		private double _scaledInitHeight;

		public ScaleHandlerNode(SelectionNode selnode, ScaleAxis corner) {
			super(selnode);
			_corner = corner;
		}

		@Override
		public void handleMousePressed(MouseEvent e) {
			super.handleMousePressed(e);

			BaseObjectModel model = getObjectNode().getModel();
			Bounds bounds = getObjectNode().getNode().getBoundsInLocal();

			_initScaleX = model.getScaleX();
			_initScaleY = model.getScaleY();
			_initWidth = bounds.getWidth();
			_initHeight = bounds.getHeight();
			_scaledInitWidth = _initWidth * model.getScaleX();
			_scaledInitHeight = _initHeight * model.getScaleY();
		}

		@Override
		protected void handleDrag(double dx, double dy) {
			super.handleDrag(dx, dy);

			BaseObjectModel model = getObjectNode().getModel();

			if (_corner.x) {
				double x = (_scaledInitWidth + dx * model.getScaleX()) / _initWidth;
				model.setScaleX(x);
			}

			if (_corner.y) {
				double y = (_scaledInitHeight + dy * model.getScaleY()) / _initHeight;
				model.setScaleY(y);
			}

			getObjectNode().getControl().updateFromModel();
		}

		@Override
		protected void handleDone() {
			BaseObjectModel model = getObjectNode().getModel();

			double x = model.getScaleX();
			double y = model.getScaleY();

			model.setScaleX(_initScaleX);
			model.setScaleY(_initScaleY);

			String id = model.getId();

			getCanvas().getUpdateBehavior().executeOperations(

			new CompositeOperation(

			new ChangePropertyOperation<Number>(id, "scale.x", Double.valueOf(x)),

			new ChangePropertyOperation<Number>(id, "scale.y", Double.valueOf(y))

			));
		}
	}

	private ScaleHandlerNode _handler_X;
	private ScaleHandlerNode _handler_XY;
	private ScaleHandlerNode _handler_Y;

	public ScaleHandlersGroup(SelectionNode selnode) {
		super(selnode);
		_handler_X = new ScaleHandlerNode(selnode, ScaleAxis.X);
		_handler_XY = new ScaleHandlerNode(selnode, ScaleAxis.XY);
		_handler_Y = new ScaleHandlerNode(selnode, ScaleAxis.Y);
		getChildren().addAll(_handler_X, _handler_XY, _handler_Y);
	}

	@Override
	public void updateHandlers() {
		double w = _selnode.getMinWidth();
		double h = _selnode.getMinHeight();

		int hs = SelectionNode.HANDLER_SIZE / 2;
		_handler_X.relocate(w - hs, -hs);
		_handler_XY.relocate(w - hs, h - hs);
		_handler_Y.relocate(-hs, h - hs);
	}

}
