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
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.CircleArcadeBodyModel;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.handlers.DisablePhysicsHandler;
import phasereditor.canvas.ui.handlers.EditBodyHandler;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;

/**
 * @author arian
 *
 */
public class ArcadeBodyCircularShortcutsPane extends ShortcutPane {

	private CircleArcadeBodyModel _body;
	private Label _radiusLabel;
	private Label _offsetXLabel;
	private Label _offsetYLabel;
	private ShortcutButton _setRectBodyBtn;
	private ShortcutButton _demoveBodyBtn;

	@SuppressWarnings("boxing")
	public ArcadeBodyCircularShortcutsPane(IObjectNode object) {
		super(object);

		_body = (CircleArcadeBodyModel) ((BaseSpriteModel) _model).getBody();

		_offsetXLabel = createNumberField(_body.getOffsetX(), "body.offset.x",
				x -> setBodyInObject(x, _body.getOffsetY(), _body.getRadius()));

		_offsetYLabel = createNumberField(_body.getOffsetY(), "body.offset.y",
				y -> setBodyInObject(_body.getOffsetX(), y, _body.getRadius()));

		_radiusLabel = createNumberField(_body.getRadius(), "radius",
				r -> setBodyInObject(_body.getOffsetX(), _body.getOffsetY(), r));

		_setRectBodyBtn = new ShortcutButton() {

			{
				Rectangle rect = new Rectangle(16, 16);
				rect.setStroke(Color.WHITE);
				rect.setFill(Color.TRANSPARENT);
				setGraphic(rect);
				setText("make rect");
				setSize(-1, -1);
			}

			@Override
			protected void doAction() {
				EditBodyHandler.setNewBody((ISpriteNode) _object, EditBodyHandler.ARCADE_BODY_RECTANGULAR);
			}
		};

		_demoveBodyBtn = new ShortcutButton() {

			{
				setText("remove body");
				setSize(-1, -1);
			}

			@Override
			protected void doAction() {
				DisablePhysicsHandler.removeBody(_canvas, new Object[] { _object });
			}
		};

		add(createTitle("arcade circ"), 0, 0);

		add(_offsetXLabel, 0, 1);
		add(_offsetYLabel, 0, 2);
		add(_radiusLabel, 0, 3);
		add(_setRectBodyBtn, 0, 4);
		add(_demoveBodyBtn, 0, 5);
	}

	@Override
	public void updateHandler() {

		_offsetXLabel.setText("offset.x = " + _body.getOffsetX());
		_offsetYLabel.setText("offset.y = " + _body.getOffsetY());
		_radiusLabel.setText("radius = " + _body.getRadius());

		super.updateHandler();
	}

	void setBodyInObject(double x, double y, double radius) {
		CompositeOperation operations = new CompositeOperation();

		String id = _model.getId();

		operations.add(new ChangePropertyOperation<Number>(id, "body.offset.x", Double.valueOf(x)));
		operations.add(new ChangePropertyOperation<Number>(id, "body.offset.y", Double.valueOf(y)));
		operations.add(new ChangePropertyOperation<Number>(id, "body.radius", Double.valueOf(radius)));

		_canvas.getUpdateBehavior().executeOperations(operations);
	}

	@Override
	public boolean isValid() {
		return ((ISpriteNode) _object).getModel().getBody() instanceof CircleArcadeBodyModel;
	}

}
