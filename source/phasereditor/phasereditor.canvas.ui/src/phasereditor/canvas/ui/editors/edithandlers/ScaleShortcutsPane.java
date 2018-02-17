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
import javafx.scene.image.ImageView;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class ScaleShortcutsPane extends ShortcutPane {

	private Label _xLabel;
	private Label _yLabel;

	@SuppressWarnings("boxing")
	public ScaleShortcutsPane(IObjectNode object) {
		super(object);

		_xLabel = createValueLabel();
		_yLabel = createValueLabel();

		add(_xLabel, 0, 0);
		add(_yLabel, 0, 1);

		setColumnSpan(_xLabel, 3);
		setColumnSpan(_yLabel, 3);

		add(new Btn("x"), 0, 2);
		add(new Btn("y"), 1, 2);
		add(new ResetBtn(), 2, 2);

	}

	@Override
	public void updateHandler() {

		_xLabel.setText("x=" + _model.getScaleX());
		_yLabel.setText("y=" + _model.getScaleY());

		super.updateHandler();
	}

	class ResetBtn extends ShortcutButton {
		public ResetBtn() {
			setSize(50, -1);
			setGraphic(createLabel("1:1"));
		}

		@Override
		protected void doAction() {
			CompositeOperation operations = new CompositeOperation();

			String id = _model.getId();

			operations.add(new ChangePropertyOperation<Number>(id, "scale.x", Double.valueOf(1)));
			operations.add(new ChangePropertyOperation<Number>(id, "scale.y", Double.valueOf(1)));

			_canvas.getUpdateBehavior().executeOperations(operations);
		}

	}

	class Btn extends ShortcutButton {

		private String _axis;

		public Btn(String axis) {
			_axis = axis;
			setSize(50, -1);

			if (_axis.equals("x")) {
				setGraphic(new ImageView(PhaserEditorUI.getUIIconURL(IEditorSharedImages.IMG_WHITE_FLIP_HORIZONTAL)));
			} else {
				setGraphic(new ImageView(PhaserEditorUI.getUIIconURL(IEditorSharedImages.IMG_WHITE_FLIP_VERTICAL)));
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
