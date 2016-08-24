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
package phasereditor.canvas.ui.editors.edithandlers;

import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public abstract class PathHandlerNode extends Path implements IEditHandlerNode {

	protected final IObjectNode _object;
	protected final ObjectCanvas _canvas;
	protected final Node _node;
	protected final BaseObjectModel _model;
	protected final BaseObjectControl<?> _control;
	private HandlerNodeHelper _helper;

	public PathHandlerNode(IObjectNode object) {
		// super(10, 10);
		_object = object;
		_control = _object.getControl();
		_node = _object.getControl().getNode();
		_model = _object.getModel();
		_canvas = _object.getControl().getCanvas();

		setStroke(Color.BLACK);
		setStrokeWidth(1);

		getElements().setAll(

		new MoveTo(0, 0),

		new LineTo(10, 0),

		new LineTo(10, 10),

		new LineTo(0, 10),

		new LineTo(0, 0));

		_helper = new HandlerNodeHelper(this);
	}

	@Override
	public IObjectNode getObject() {
		return _object;
	}

	@Override
	public void handleMouseMoved(MouseEvent e) {
		// nothing
	}

	@Override
	public void handleMousePressed(MouseEvent e) {
		_helper.handleMousePressed(e);
	}

	@Override
	public void handleMouseDragged(MouseEvent e) {
		_helper.handleMouseDragged(e);
	}

	@Override
	public void handleMouseReleased(MouseEvent e) {
		_helper.handleMouseReleased(e);
	}

	@Override
	public void handleMouseExited(MouseEvent e) {
		setCursor(Cursor.DEFAULT);
	}
	
	@Override
	public boolean isValid() {
		return true;
	}
}
