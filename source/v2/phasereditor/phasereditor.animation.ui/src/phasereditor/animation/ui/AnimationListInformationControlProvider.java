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
package phasereditor.animation.ui;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import com.subshell.snippets.jface.tooltip.tooltipsupport.IInformationProvider;

import phasereditor.ui.FrameCanvasUtils;

/**
 * @author arian
 *
 */
public class AnimationListInformationControlProvider implements IInformationProvider {

	private AnimationListCanvas<?> _canvas;

	public AnimationListInformationControlProvider(AnimationListCanvas<?> canvas) {
		super();
		_canvas = canvas;
	}

	@Override
	public Object getInformation(Point location) {
		FrameCanvasUtils utils = _canvas.getUtils();
		var point = utils.scrollPositionToReal(location.x, location.y);
		int row = point.y / _canvas.getRowHeight();
		var anim = utils.getFrameObject(row);
		return anim;
	}

	@Override
	public Rectangle getArea(Point location) {
		FrameCanvasUtils utils = _canvas.getUtils();
		
		var realPoint = utils.scrollPositionToReal(location.x, location.y);
		
		int rowHeight = _canvas.getRowHeight();
		
		int row = realPoint.y / _canvas.getRowHeight();
		
		var scrollPoint = utils.realPositionToScroll(0, row * rowHeight);
		
		Rectangle b = _canvas.getBounds();
		
		return new Rectangle(location.x, scrollPoint.y, b.width, rowHeight);
	}
}
