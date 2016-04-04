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

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;

/**
 * @author arian
 *
 */
public class SpriteShapeNode extends BaseObjectNode {
	private ImageView _imageView;

	SpriteShapeNode(BaseObjectControl<?> control, IFile imageFile) {
		this(control, new ImageView("file:" + imageFile.getLocation().makeAbsolute().toPortableString()));
	}

	SpriteShapeNode(BaseObjectControl<?> control, ImageView imageView) {
		super(control);

		_imageView = imageView;

		getChildren().add(imageView);
		// imageView.setMouseTransparent(true);

		{
			Image img = _imageView.getImage();
			_selectionRect.setWidth(img.getWidth());
			_selectionRect.setHeight(img.getHeight());
		}
	}

	public ImageView getImageView() {
		return _imageView;
	}

	public void setViewport(Rectangle2D rect) {
		_imageView.setViewport(rect);

		_selectionRect.setWidth(rect.getWidth());
		_selectionRect.setHeight(rect.getHeight());
	}

	@SuppressWarnings("unused")
	private boolean isTransparent(double x, double y) {
		PixelReader reader = _imageView.getImage().getPixelReader();
		return reader.getArgb((int) x, (int) y) == 0;
	}

}
