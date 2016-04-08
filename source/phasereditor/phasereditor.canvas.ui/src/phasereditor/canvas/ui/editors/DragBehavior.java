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

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class DragBehavior {
	private ShapeCanvas _canvas;
	private Scene _scene;
	private Node _dragNode;
	private Point2D _startScenePoint;
	private Point2D _startNodePoint;

	public DragBehavior(ShapeCanvas canvas) {
		super();
		_canvas = canvas;

		_scene = _canvas.getScene();

		_scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			if (event.getButton() != MouseButton.PRIMARY) {
				return;
			}

			PickResult pick = event.getPickResult();
			Node userPicked = pick.getIntersectedNode();
			Node picked = _canvas.getSelectionBehavior().findBestToPick(userPicked);

			if (picked == null) {
				return;
			}

			_dragNode = picked;
			_startNodePoint = new Point2D(picked.getLayoutX(), picked.getLayoutY());

			_startScenePoint = new Point2D(event.getSceneX(), event.getSceneY());
		});

		_scene.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
			if (_dragNode == null) {
				return;
			}
			double dx = event.getSceneX() - _startScenePoint.getX();
			double dy = event.getSceneY() - _startScenePoint.getY();
			_dragNode.setLayoutX(_startNodePoint.getX() + dx);
			_dragNode.setLayoutY(_startNodePoint.getY() + dy);
			_canvas.getSelectionBehavior().updateSelectedNodes();
		});

		_scene.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
			if (_dragNode != null) {
				BaseObjectControl<?> control = ((IObjectNode) _dragNode).getControl();
				BaseObjectModel model = control.getModel();
				model.setLocation(_dragNode.getLayoutX(), _dragNode.getLayoutY());

				UpdateChangeBehavior updateBehavior = _canvas.getUpdateBehavior();
				updateBehavior.update_Grid_from_PropertyChange(control.getX_property());
				updateBehavior.update_Grid_from_PropertyChange(control.getY_property());
			}
			_dragNode = null;
		});

		_scene.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
			if (_dragNode != null) {
				if (event.getCode() == KeyCode.ESCAPE) {
					_dragNode.relocate(_startScenePoint.getX(), _startScenePoint.getY());
					_dragNode = null;
				}
			}
		});
	}
}
