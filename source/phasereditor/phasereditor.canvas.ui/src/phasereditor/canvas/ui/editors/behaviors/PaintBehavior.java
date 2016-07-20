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
package phasereditor.canvas.ui.editors.behaviors;

import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import phasereditor.canvas.ui.editors.ObjectCanvas;

/**
 * @author arian
 *
 */
public class PaintBehavior {
	private ObjectCanvas _canvas;

	public PaintBehavior(ObjectCanvas canvas) {
		super();
		_canvas = canvas;

		_canvas.addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent e) {
				repaint();
			}

			@Override
			public void controlMoved(ControlEvent e) {
				repaint();
			}
		});

		ChangeListener<Number> sizeListener = new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				repaint();
			}
		};
		_canvas.getScene().widthProperty().addListener(sizeListener);
		_canvas.getScene().heightProperty().addListener(sizeListener);
	}

	public void repaint() {
		_canvas.getBackGridPane().repaint();
		_canvas.getFrontGridPane().repaint();
	}

}
