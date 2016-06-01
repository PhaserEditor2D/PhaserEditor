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
package phasereditor.canvas.ui.shapes;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import phasereditor.assetpack.ui.AssetPackUI.FrameData;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.TileSpriteModel;

/**
 * @author arian
 *
 */
public class TileSpriteNode extends Canvas implements ISpriteNode {

	private FrameData _frame;
	private TileSpriteControl _control;
	private Image _image;

	public TileSpriteNode(IFile imageFile, FrameData frame, TileSpriteControl control) {
		_control = control;
		_image = new Image("file:" + imageFile.getLocation().makeAbsolute().toPortableString());

		if (frame == null) {
			Rectangle rect = new Rectangle(0, 0, (int) _image.getWidth(), (int) _image.getHeight());
			_frame = new FrameData();
			_frame.src = rect;
			_frame.dst = rect;
			_frame.srcSize = new Point(rect.width, rect.height);
		} else {
			_frame = frame;
		}

		updateFromModel();
	}

	public void updateFromModel() {
		TileSpriteModel model = _control.getModel();

		double width = model.getWidth();
		double height = model.getHeight();

		setWidth(width);
		setHeight(height);

		GraphicsContext g2 = getGraphicsContext2D();
		g2.clearRect(0, 0, width, height);

		double xoffs = model.getTilePositionX() % width;
		double yoffs = model.getTilePositionY() % height;

		double x0 = _frame.src.x;
		double y0 = _frame.src.y;
		double w0 = _frame.src.width;
		double h0 = _frame.src.height;

		double x1 = xoffs - (_frame.srcSize.x + _frame.dst.x) * model.getTileScaleX();
		double y1 = yoffs - (_frame.srcSize.y + _frame.dst.y) * model.getTileScaleY();
		double w1 = _frame.dst.width * model.getTileScaleX();
		double h1 = _frame.dst.height * model.getTileScaleY();

		for (double x = 0; x < getWidth() * 3; x += w1) {
			for (double y = 0; y < getHeight() * 3; y += h1) {
				g2.drawImage(_image, x0, y0, w0, h0, x1 + x, y1 + y, w1, h1);
			}
		}
	}

	public Image getImage() {
		return _image;
	}

	@Override
	public BaseObjectModel getModel() {
		return _control.getModel();
	}

	@Override
	public BaseObjectControl<?> getControl() {
		return _control;
	}

	@Override
	public Node getNode() {
		return this;
	}
}
