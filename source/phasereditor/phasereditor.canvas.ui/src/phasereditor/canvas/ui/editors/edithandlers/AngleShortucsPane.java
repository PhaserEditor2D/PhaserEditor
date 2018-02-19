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
	private static double _lastDeltaValue = 45;

	@SuppressWarnings("boxing")
	public AngleShortucsPane(IObjectNode object) {
		super(object);

		add(createTitle("angle"), 0, 0, 3, 1);
		
		add(_angleText = createTextField(object.getModel().getAngle(), "angle", value -> {
			setAngleInObject(value);
		}), 0, 1, 3, 1);

		add(new AngleBtn(-1), 0, 2);
		add(new AngleBtn(1), 1, 2);

		_angleDeltaText = createTextField(_lastDeltaValue, "delta", v -> _lastDeltaValue = v);
		_angleDeltaText.setPrefSize(50, -1);

		add(_angleDeltaText, 2, 2);
	}

	@Override
	public void updateHandler() {

		_angleText.setText("= " + _model.getAngle());

		super.updateHandler();
	}

	private void setAngleInObject(double angle) {
		CompositeOperation operations = new CompositeOperation();

		String id = _model.getId();

		operations.add(new ChangePropertyOperation<Number>(id, "angle", Double.valueOf(angle)));

		_canvas.getUpdateBehavior().executeOperations(operations);
	}

	class AngleBtn extends ShortcutButton {

		private double _sign;

		public AngleBtn(double sign) {
			_sign = sign;

			if (sign < 0) {
				setIcon(IEditorSharedImages.IMG_WHITE_ROTATE_ANTICLOCKWISE);
			} else {
				setIcon(IEditorSharedImages.IMG_WHITE_ROTATE_CLOCKWISE);
			}

			setSize(50, -1);
		}

		@SuppressWarnings("synthetic-access")
		@Override
		protected void doAction() {
			double incr = Double.parseDouble(_angleDeltaText.getText());
			setAngleInObject(_model.getAngle() + _sign * incr);
		}

	}
}
