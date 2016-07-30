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

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class DragHandlerNode extends Rectangle implements IEditHandler {
	private Point2D _start;
	private SelectionNode _selnode;

	public DragHandlerNode(SelectionNode selnode) {
		super(SelectionNode.HANDLE_SIZE, SelectionNode.HANDLE_SIZE);
		_selnode = selnode;
		setVisible(false);
		setFill(Color.GREENYELLOW);
		setStroke(Color.BLACK);
		setStrokeWidth(1);
	}

	@SuppressWarnings("unused")
	protected void handleDrag(double dx, double dy) {
		// nothing
	}
	
	protected void handleDone() {
		// nothing 
	}
	
	@Override
	public void handleMouseMoved(MouseEvent e) {
		// nothing
	}

	@Override
	public void handleMousePressed(MouseEvent e) {
		_start = new Point2D(e.getSceneX(), e.getSceneY());
	}

	@Override
	public void handleMouseDragged(MouseEvent e) {
		if (_start == null) {
			return;
		}
		
		Point2D p = new Point2D(e.getSceneX(), e.getSceneY());
		Bounds sceneDelta = new BoundingBox(0, 0, p.getX() - _start.getX(), p.getY() - _start.getY());
		Node node = _selnode.getObjectNode().getNode();
		Bounds spriteDelta = node.sceneToLocal(new BoundingBox(0, 0, sceneDelta.getWidth(), sceneDelta.getHeight()));

		double w = spriteDelta.getWidth();
		double h = spriteDelta.getHeight();

		if (p.getX() < _start.getX()) {
			w = -w;
		}

		if (p.getY() < _start.getY()) {
			h = -h;
		}

		handleDrag(w, h);

		_selnode.updateBounds(_selnode.getCanvas().getSelectionBehavior().buildSelectionBounds(node));
	}

	@Override
	public void handleMouseReleased(MouseEvent e) {
		handleDone();
	}

	@Override
	public void handleMouseExited(MouseEvent e) {
		setCursor(Cursor.DEFAULT);
	}
	
	public SelectionNode getSelectionNode() {
		return _selnode;
	}
	
	public IObjectNode getObjectNode() {
		return _selnode.getObjectNode();
	}
	
	public ObjectCanvas getCanvas() {
		return _selnode.getCanvas();
	}
}