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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wb.swt.SWTResourceManager;

/**
 * @author arian
 *
 */
public class ExplainFrameCanvas extends ImageCanvas {

	public ExplainFrameCanvas(Composite parent, int style) {
		super(parent, style);
	}

	@Override
	protected void drawImage(GC gc, int srcX, int srcY, int srcW, int srcH, int dstW, int dstH, int dstX, int dstY) {
		super.drawImage(gc, srcX, srcY, srcW, srcH, dstW, dstH, dstX, dstY);

		var fd = getFrameData();

		var x = getPanOffsetX();
		var y = getOffsetY();
		var scale = getScale();

		gc.setAlpha(150);

		gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
		gc.drawRectangle(x, y, (int) (fd.srcSize.x * scale), (int) (fd.srcSize.y * scale));

		// sourceW
		{
			var text = Integer.toString(fd.srcSize.x);
			var size = gc.textExtent(text);
			gc.drawText(text, (int) (x + fd.srcSize.x / 2 * scale - size.x / 2), (int) (y + fd.srcSize.y * scale),
					true);
		}

		// sourceH
		{
			var text = Integer.toString(fd.srcSize.y);
			var size = gc.textExtent(text);

			var tx = new Transform(gc.getDevice());

			tx.translate(x + fd.srcSize.x * scale, dstY + fd.srcSize.y / 2 * scale);
			tx.translate(size.y + 2, -	size.x);
			tx.rotate(90);

			gc.setTransform(tx);

			gc.drawText(text, 0, 0, true);

			gc.setTransform(null);
			tx.dispose();
		}

		gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
		gc.drawRectangle(dstX, dstY, dstW, dstH);

		// spriteW
		{
			var text = Integer.toString(fd.dst.width);
			var size = gc.textExtent(text);
			gc.drawText(text, (int) (dstX + fd.dst.width / 2 * scale) - size.x / 2, dstY - size.y, true);
		}

		// spriteH
		{
			var text = Integer.toString(fd.dst.height);
			var size = gc.textExtent(text);

			var tx = new Transform(gc.getDevice());
			tx.translate(dstX - size.y / 2, dstY + dstW / 2 + size.x / 2);
			tx.translate(size.y/2, -	size.x);
			tx.rotate(90);
			gc.setTransform(tx);

			gc.drawText(text, 0, 0, true);

			gc.setTransform(null);
			tx.dispose();
		}

		// spriteX/Y
		{
			gc.setLineStyle(SWT.LINE_DOT);

			gc.drawLine(dstX, dstY, dstX, 0);
			gc.drawLine(x, dstY, x, 0);

			gc.drawLine(0, dstY, dstX, dstY);
			gc.drawLine(0, y, dstX, y);

			{
				var txt = Integer.toString(fd.dst.x);
				var size = gc.textExtent(txt);
				gc.drawText(txt, (dstX + x) / 2 - size.x / 2, 0, true);
			}

			{
				var txt = Integer.toString(fd.dst.y);
				var size = gc.textExtent(txt);
				var tx = new Transform(gc.getDevice());
				tx.translate(size.x + 2, (dstY + y) / 2 - size.y / 2);
				tx.rotate(90);
				gc.setTransform(tx);
				gc.drawText(txt, 0, 0, true);
				gc.setTransform(null);
				tx.dispose();
			}

			gc.setLineStyle(SWT.LINE_SOLID);
		}

		gc.setAlpha(255);

	}

	@Override
	protected Rectangle getFitArea() {
		var b = getBounds();

		if (b.width > 100) {
			b.width -= 25;
		}

		if (b.height > 100) {
			b.height -= 25;
		}

		return b;
	}

}
