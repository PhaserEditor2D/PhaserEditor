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
package phasereditor.ui;

import java.io.File;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

/**
 * @author arian
 *
 */
public class VirtualImageCanvas extends ZoomCanvas {

	private File _file;
	private FrameData _fd;

	public VirtualImageCanvas(Composite parent, int style) {
		super(parent, style);
	}

	@Override
	protected void customPaintControl(PaintEvent e) {
		var virtualImage = VirtualImage.get(_file, _fd);
		if (virtualImage != null) {
			var fd = virtualImage.getFinalFrameData();
			var calc = calc();
			var area = calc.modelToView(0, 0, fd.dst.width, fd.dst.height);
			virtualImage.paintScaledInArea(e.gc, area);
		}
	}

	@Override
	protected Point getImageSize() {
		if (_fd == null) {
			var virtualImage = VirtualImage.get(_file, _fd);

			if (virtualImage != null) {
				return virtualImage.getFinalFrameData().srcSize;
			}
		}

		return new Point(1, 1);
	}

	public void setImageInfo(File file, FrameData fd) {
		_file = file;
		_fd = fd;
		
		resetZoom();
	}

	public File getFile() {
		return _file;
	}

	public FrameData getFrameData() {
		return _fd;
	}

}
