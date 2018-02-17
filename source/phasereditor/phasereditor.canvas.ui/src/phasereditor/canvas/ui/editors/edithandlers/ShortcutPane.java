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

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
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
	private static Point2D _location;
	private static String _lastObjectId;

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

		// Color color1 = Color.ALICEBLUE;
		// Color color2 = color1.deriveColor(0, 0, 1, 0.6);
		// color2 = Color.BLACK;

		setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, new Insets(0))));

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
				double x = _initPos.getX() + (point.getX() - _startPoint.getX());
				double y = _initPos.getY() + (point.getY() - _startPoint.getY());
				_location = new Point2D(x, y);
				relocate(x, y);
			}
			e.consume();
		});

		setOnMouseMoved(e -> {
			e.consume();
		});

		FadeTransition t = new FadeTransition(new Duration(100), this);
		t.setFromValue(0);
		t.setToValue(1);
		t.play();

		ScaleTransition s = new ScaleTransition(new Duration(100), this);
		s.setFromX(0.5);
		s.setFromY(0.5);
		s.setToX(1);
		s.setToY(1);
		s.play();
	}

	protected static Label createValueLabel() {
		Label label = new Label();
		label.setMinWidth(150);
		label.setMaxWidth(150);
		label.setTextOverrun(OverrunStyle.ELLIPSIS);
		label.setTextFill(Color.WHITE);
		return label;
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

		String id = _object.getModel().getId();

		if (_location == null || !id.equals(_lastObjectId)) {

			Bounds bounds = _control.getNode().getBoundsInLocal();
			bounds = _control.getNode().localToScene(bounds);
			double x = Math.max(bounds.getMinX(), bounds.getMaxX() + bounds.getWidth());
			double y = Math.max(bounds.getMinY(), bounds.getMaxY() + bounds.getHeight());
			x = bounds.getMinX() + bounds.getWidth();
			y = bounds.getMinY();

			_location = new Point2D(x + 30, y);
			_lastObjectId = id;

		}

		relocate(_location.getX(), _location.getY());
	}

	@Override
	public boolean isValid() {
		return true;
	}
}
