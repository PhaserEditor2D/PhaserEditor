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
package phasereditor.canvas.ui.editors.edithandlers;

import javafx.scene.control.Label;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.ui.IEditorSharedImages;

/**
 * @author arian
 *
 */
public class ScaleShortcutsPane extends ShortcutPane {

	private Label _xLabel;
	private Label _yLabel;
	private CoordsButton _coordsBtn;

	@SuppressWarnings("boxing")
	public ScaleShortcutsPane(IObjectNode object) {
		super(object);

		add(createTitle("scale"), 0, 0, 3, 1);

		_xLabel = createNumberField(_model.getScaleX(), "scale.x",
				x -> setScaleInObject(x, isLocalCoords() ? _model.getScaleY() : _model.getGlobalScaleY()));
		
		_yLabel = createNumberField(_model.getScaleY(), "scale.y",
				y -> setScaleInObject(isLocalCoords() ? _model.getScaleX() : _model.getGlobalScaleX(), y));

		_coordsBtn = createCoordsButton();

		add(_xLabel, 0, 1, 3, 1);
		add(_yLabel, 0, 2, 3, 1);

		add(new Btn("x"), 0, 3);
		add(new Btn("y"), 1, 3);
		add(new ResetBtn(), 2, 3);
		add(_coordsBtn, 0, 4, 3, 1);

	}

	@Override
	public void updateHandler() {

		_xLabel.setText("x = " + (isLocalCoords() ? _model.getScaleX() : _model.getGlobalScaleX()));
		_yLabel.setText("y = " + (isLocalCoords() ? _model.getScaleY() : _model.getGlobalScaleY()));

		_coordsBtn.update();

		super.updateHandler();
	}

	void setScaleInObject(double scaleX, double scaleY) {
		CompositeOperation operations = new CompositeOperation();

		String id = _model.getId();

		double x = scaleX;
		double y = scaleY;

		if (isGlobalCoords()) {
			GroupModel parent = _model.getParent();

			double parentX = parent.getGlobalScaleX();
			double parentY = parent.getGlobalScaleY();

			x = parentX == 0 ? 0 : scaleX / parentX;
			y = parentY == 0 ? 0 : scaleY / parentY;
		}

		operations.add(new ChangePropertyOperation<Number>(id, "scale.x", Double.valueOf(x)));
		operations.add(new ChangePropertyOperation<Number>(id, "scale.y", Double.valueOf(y)));

		_canvas.getUpdateBehavior().executeOperations(operations);
	}

	class ResetBtn extends ShortcutButton {
		public ResetBtn() {
			setSize(50, -1);
			setText("1:1");
		}

		@Override
		protected void doAction() {
			setScaleInObject(1, 1);
		}

	}

	class Btn extends ShortcutButton {

		private String _axis;

		public Btn(String axis) {
			_axis = axis;
			setSize(50, -1);

			if (_axis.equals("x")) {
				setIcon(IEditorSharedImages.IMG_SHAPE_FLIP_HORIZONTAL);
			} else {
				setIcon(IEditorSharedImages.IMG_SHAPE_FLIP_VERTICAL);
			}
		}

		@Override
		protected void doAction() {
			CompositeOperation operations = new CompositeOperation();

			String id = _model.getId();

			double scale = -1 * (_axis.equals("x") ? _model.getScaleX() : _model.getScaleY());

			operations.add(new ChangePropertyOperation<Number>(id, "scale." + _axis, Double.valueOf(scale)));

			_canvas.getUpdateBehavior().executeOperations(operations);
		}

	}

}
