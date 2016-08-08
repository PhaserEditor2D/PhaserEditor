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

import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public interface IEditHandlerNode {

	void handleMouseMoved(MouseEvent e);

	void handleMousePressed(MouseEvent e);

	void handleMouseDragged(MouseEvent e);

	void handleMouseReleased(MouseEvent e);

	void handleMouseExited(MouseEvent e);
	
	IObjectNode getObject();

	void updateHandler();
	
	/**
	 * Handle a drag event. Values are transformed to the local dimensions.
	 * 
	 * @param dx
	 *            Local delta X.
	 * @param dy
	 *            Local delta Y.
	 */
	void handleDrag(double localDX, double localDY);
	
	void handleDone();
	
	/**
	 * Transform an object local position into an scene position.
	 */
	default Point2D objectToScene(IObjectNode object, double x, double y) {
		Point2D p = object.getNode().localToScene(new Point2D(x, y));
		return p;
	}
}