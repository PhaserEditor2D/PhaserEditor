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
import phasereditor.canvas.ui.shapes.IObjectNode;

public enum Axis {
	TOP_LEF(0, 0),

	TOP(0.5, 0),

	TOP_RIG(1, 0),

	RIG(1, 0.5),

	BOT_RIG(1, 1),

	BOT(0.5, 1),

	BOT_LEF(0, 1),

	LEF(0, 0.5),

	CENTER(0.5, 0.5);

	public double x;
	public double y;

	private Axis(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double signW() {
		if (x == 0) return -1;
		if (x == 1) return 1;
		return 0;
	}
	
	public double signH() {
		if (y == 0) return -1;
		if (y == 1) return 1;
		return 0;
	}
 
	public boolean changeW() {
		return x != 0.5;
	}

	public boolean changeH() {
		return y != 0.5;
	}

	public Cursor getResizeCursor(IObjectNode object) {
		if (this == CENTER) {
			return Cursor.MOVE;
		}

		boolean sx = object.getModel().getScaleX() >= 0;
		boolean sy = object.getModel().getScaleY() >= 0;
		String name = "";

		if (y == 0) {
			name += sy ? "N" : "S";
		} else if (y == 1) {
			name += sy ? "S" : "N";
		}

		if (x == 0) {
			name += sx ? "W" : "E";
		} else if (x == 1) {
			name += sx ? "E" : "W";
		}

		name += "_RESIZE";

		return Cursor.cursor(name);
	}

	public Cursor getRotateCursor(IObjectNode object) {
		if (this == CENTER) {
			return Cursor.MOVE;
		}

		boolean sx = object.getModel().getScaleX() >= 0;
		boolean sy = object.getModel().getScaleY() >= 0;
		String name = "";

		if (y == 0) {
			name += sy ? "N" : "S";
		} else if (y == 1) {
			name += sy ? "S" : "N";
		}

		if (x == 0) {
			name += sx ? "E" : "W";
		} else if (x == 1) {
			name += sx ? "W" : "E";
		}

		name += "_RESIZE";

		return Cursor.cursor(name);
	}
}