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
import javafx.scene.shape.Circle;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.RectArcadeBodyModel;
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
public class ArcadeBodyRectShortcutsPane extends ShortcutPane {

	private RectArcadeBodyModel _body;
	private Label _offsetXLabel;
	private Label _offsetYLabel;
	private ShortcutButton _setCircBodyBtn;
	private ShortcutButton _demoveBodyBtn;
	private Label _widthLabel;
	private Label _heightLabel;

	@SuppressWarnings("boxing")
	public ArcadeBodyRectShortcutsPane(IObjectNode object) {
		super(object);

		_body = (RectArcadeBodyModel) ((BaseSpriteModel) _model).getBody();

		_offsetXLabel = createNumberField(_body.getOffsetX(), "body.offset.x",
				x -> setBodyInObject(x, _body.getOffsetY(), _body.getWidth(), _body.getHeight()));

		_offsetYLabel = createNumberField(_body.getOffsetY(), "body.offset.y",
				y -> setBodyInObject(_body.getOffsetX(), y, _body.getWidth(), _body.getHeight()));

		_widthLabel = createNumberField(_body.getWidth(), "body.width",
				w -> setBodyInObject(_body.getOffsetX(), _body.getOffsetY(), w, _body.getHeight()));

		_heightLabel = createNumberField(_body.getWidth(), "body.height",
				h -> setBodyInObject(_body.getOffsetX(), _body.getOffsetY(), _body.getWidth(), h));

		_setCircBodyBtn = new ShortcutButton() {

			{
				Circle rect = new Circle(8);
				rect.setStroke(Color.WHITE);
				rect.setFill(Color.TRANSPARENT);
				setGraphic(rect);
				setText("make circ");
				setSize(-1, -1);
			}

			@Override
			protected void doAction() {
				EditBodyHandler.setNewBody((ISpriteNode) _object, EditBodyHandler.ARCADE_BODY_CIRCULAR);
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

		add(createTitle("arcade rect"), 0, 0);

		add(_offsetXLabel, 0, 1);
		add(_offsetYLabel, 0, 2);
		add(_widthLabel, 0, 3);
		add(_heightLabel, 0, 4);
		add(_setCircBodyBtn, 0, 5);
		add(_demoveBodyBtn, 0, 6);
	}

	@Override
	public void updateHandler() {

		_offsetXLabel.setText("offset.x = " + _body.getOffsetX());
		_offsetYLabel.setText("offset.y = " + _body.getOffsetY());
		_widthLabel.setText("width = " + _body.getWidth());
		_heightLabel.setText("height = " + _body.getHeight());

		super.updateHandler();
	}

	void setBodyInObject(double x, double y, double w, double h) {
		CompositeOperation operations = new CompositeOperation();

		String id = _model.getId();

		operations.add(new ChangePropertyOperation<Number>(id, "body.offset.x", Double.valueOf(x)));
		operations.add(new ChangePropertyOperation<Number>(id, "body.offset.y", Double.valueOf(y)));
		operations.add(new ChangePropertyOperation<Number>(id, "body.width", Double.valueOf(w)));
		operations.add(new ChangePropertyOperation<Number>(id, "body.height", Double.valueOf(h)));

		_canvas.getUpdateBehavior().executeOperations(operations);
	}

	@Override
	public boolean isValid() {
		return ((ISpriteNode) _object).getModel().getBody() instanceof RectArcadeBodyModel;
	}

}
