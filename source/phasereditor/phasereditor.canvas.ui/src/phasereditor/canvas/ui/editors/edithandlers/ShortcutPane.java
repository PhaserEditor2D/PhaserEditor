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

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public abstract class ShortcutPane extends GridPane implements IEditHandlerNode {

	protected final IObjectNode _object;
	protected final ObjectCanvas _canvas;
	protected final Node _node;
	protected final BaseObjectModel _model;
	protected final BaseObjectControl<?> _control;
	private boolean _updated;
	private Point2D _startPoint;
	private Point2D _initPos;

	public ShortcutPane(IObjectNode object) {
		_object = object;
		_control = _object.getControl();
		_node = _object.getControl().getNode();
		_model = _object.getModel();
		_canvas = _object.getControl().getCanvas();

		setPadding(new Insets(10));
		setHgap(5);
		setVgap(5);

		setEffect(new DropShadow());
		setBackground(new Background(
				new BackgroundFill(Color.ALICEBLUE.deriveColor(0, 0, 1, 0.5), CornerRadii.EMPTY, new Insets(0))));

		setOnDragDetected(e -> {
			_initPos = localToParent(0, 0);
			_startPoint = localToParent(e.getX(), e.getY());
			e.consume();
		});

		setOnMouseReleased(e -> {
			_initPos = null;
			_startPoint = null;
			e.consume();
		});

		setOnMouseDragged(e -> {
			if (_initPos != null) {
				Point2D point = localToParent(e.getX(), e.getY());
				relocate(_initPos.getX() + (point.getX() - _startPoint.getX()),
						_initPos.getY() + (point.getY() - _startPoint.getY()));
			}
			e.consume();
		});

		setOnMouseMoved(e -> {
			e.consume();
		});

		setCursor(Cursor.MOVE);
	}

	@Override
	public IObjectNode getObject() {
		return _object;
	}

	@Override
	public void updateHandler() {
		if (_updated) {
			return;
		}

		_updated = true;

		double width = _control.getTextureWidth();
		Point2D p = objectToScene(width, 0);
		relocate(p.getX() + 10, p.getY());
	}
}
