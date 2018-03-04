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
import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.ui.IEditorSharedImages;

/**
 * @author arian
 *
 */
public class MoveShortcutsPane extends ShortcutPane {

	private Label _xLabel;
	private Label _yLabel;
	private ShortcutButton _stepBtn;
	private Label _stepXLabel;
	private Label _stepYLabel;
	private CoordsButton _coordsBtn;

	@SuppressWarnings("boxing")
	public MoveShortcutsPane(IObjectNode object) {
		super(object);

		EditorSettings settings = _canvas.getSettingsModel();

		_xLabel = createTextField(_model.getX(), "x", x -> setPositionInObject(x, _model.getY()));

		_yLabel = createTextField(_model.getY(), "y", y -> setPositionInObject(_model.getX(), y));

		_stepBtn = new ShortcutButton() {

			{
				setIcon(IEditorSharedImages.IMG_WHITE_ASTERISK);
			}

			@Override
			protected void doAction() {
				settings.setEnableStepping(!settings.isEnableStepping());
				updateHandler();
				_canvas.getPaintBehavior().repaint();
			}
		};

		_stepXLabel = createTextField(_model.getX(), "stepWidth", x -> {
			settings.setStepWidth(x.intValue());
			updateHandler();
		});

		_stepYLabel = createTextField(_model.getY(), "stepHeight", y -> {
			settings.setStepHeight(y.intValue());
			updateHandler();
		});

		_coordsBtn = createCoordsButton();

		add(createTitle("position"), 0, 0, 3, 1);
		add(_xLabel, 0, 1, 3, 1);
		add(_yLabel, 0, 2, 3, 1);
		add(_stepBtn, 0, 3, 1, 1);
		add(_stepXLabel, 1, 3, 1, 1);
		add(_stepYLabel, 2, 3, 1, 1);
		add(_coordsBtn, 0, 4, 3, 1);
	}

	@Override
	public void updateHandler() {

		_xLabel.setText("x = " + _model.getX());
		_yLabel.setText("y = " + _model.getY());

		EditorSettings settings = _canvas.getSettingsModel();
		_stepXLabel.setText("w=" + settings.getStepWidth());
		_stepYLabel.setText("h=" + settings.getStepHeight());

		_stepBtn.setSelected(settings.isEnableStepping() ? Boolean.TRUE : Boolean.FALSE);

		_coordsBtn.update();

		super.updateHandler();

	}

	void setPositionInObject(double x, double y) {
		CompositeOperation operations = new CompositeOperation();

		String id = _model.getId();

		operations.add(new ChangePropertyOperation<Number>(id, "x", Double.valueOf(x)));
		operations.add(new ChangePropertyOperation<Number>(id, "y", Double.valueOf(y)));

		_canvas.getUpdateBehavior().executeOperations(operations);
	}

}
