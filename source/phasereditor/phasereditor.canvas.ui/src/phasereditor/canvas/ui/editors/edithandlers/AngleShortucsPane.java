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

import javafx.scene.control.Labeled;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.ui.IEditorSharedImages;

/**
 * @author arian
 *
 */
public class AngleShortucsPane extends ShortcutPane {

	private Labeled _angleText;
	private Labeled _angleDeltaText;
	private CoordsButton _coordsBtn;
	private static double _lastDeltaValue = 45;

	@SuppressWarnings("boxing")
	public AngleShortucsPane(IObjectNode object) {
		super(object);

		_angleText = createNumberField(object.getModel().getAngle(), "angle", value -> setAngleInObject(value));
		_angleDeltaText = createNumberField(_lastDeltaValue, "delta", v -> _lastDeltaValue = v);
		_angleDeltaText.setPrefSize(50, -1);

		_coordsBtn = createCoordsButton();

		add(createTitle("angle"), 0, 0, 3, 1);

		add(_angleText, 0, 1, 3, 1);

		add(new IncrAngleBtn(-1), 0, 2);
		add(new IncrAngleBtn(1), 1, 2);

		add(_angleDeltaText, 2, 2);
		add(_coordsBtn, 0, 3, 2, 1);
	}

	@Override
	public void updateHandler() {

		_angleText.setText("= " + (isLocalCoords() ? _model.getAngle() : _model.getGlobalAngle()));
		_coordsBtn.update();

		super.updateHandler();
	}

	private void setAngleInObject(double angle) {

		double a;

		if (isLocalCoords()) {
			a = angle;
		} else {
			a = angle - _model.getParent().getGlobalAngle();
		}

		CompositeOperation operations = new CompositeOperation();

		String id = _model.getId();

		operations.add(new ChangePropertyOperation<Number>(id, "angle", Double.valueOf(a)));

		_canvas.getUpdateBehavior().executeOperations(operations);
	}

	class IncrAngleBtn extends ShortcutButton {

		private double _sign;

		public IncrAngleBtn(double sign) {
			super((sign < 0 ? "Decrement" : "Increment") + " the angle.");
			_sign = sign;

			if (sign < 0) {
				setIcon(IEditorSharedImages.IMG_ARROW_ROTATE_ANTICLOCKWISE);
			} else {
				setIcon(IEditorSharedImages.IMG_ARROW_ROTATE_CLOCKWISE);
			}

			setSize(50, -1);
		}

		@SuppressWarnings("synthetic-access")
		@Override
		protected void doAction() {
			double incr = Double.parseDouble(_angleDeltaText.getText());
			double a = _model.getAngle() + _sign * incr;
			setAngleInObject(a % 360);
		}

	}
}
