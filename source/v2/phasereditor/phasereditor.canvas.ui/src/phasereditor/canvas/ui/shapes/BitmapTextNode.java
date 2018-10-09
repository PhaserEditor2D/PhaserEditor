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
package phasereditor.canvas.ui.shapes;

import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.bmpfont.core.BitmapFontModel;
import phasereditor.bmpfont.core.BitmapFontRenderer;
import phasereditor.canvas.core.BitmapTextModel;
import phasereditor.ui.ImageCache;

/**
 * @author arian
 *
 */
public class BitmapTextNode extends Pane implements ISpriteNode, ITextSpriteNode {

	private BitmapTextControl _control;

	public BitmapTextNode(BitmapTextControl control) {
		_control = control;
	}

	@Override
	public Node getNode() {
		return this;
	}

	@Override
	public BitmapTextModel getModel() {
		return _control.getModel();
	}

	@Override
	public BitmapTextControl getControl() {
		return _control;
	}

	public void updateFromModel() {
		BitmapTextModel model = _control.getModel();

		BitmapFontAssetModel asset = model.getAssetKey();
		Image image = ImageCache.getFXImage(asset.getTextureFile());

		BitmapFontModel fontModel = model.createFontModel();

		double scale = (double) getModel().getFontSize() / (double) fontModel.getInfoSize();

		Scale scaleTx = Transform.scale(scale, scale);

		getChildren().clear();

		fontModel.render(model.createRenderArgs(), new BitmapFontRenderer() {

			@Override
			public void render(char c, int x, int y, int width, int height, int srcX, int srcY, int srcW, int srcH) {
				if (srcW * srcH == 0) {
					// space characters are renderer as a single transparent pixel or as a 0-size
					// rectangle, in this case we should ignore it.
					return;
				}

				ImageView img = new ImageView(image);
				img.setViewport(new Rectangle2D(srcX, srcY, srcW, srcH));
				img.relocate(x * scale, y * scale);
				img.getTransforms().add(scaleTx);
				getChildren().add(img);
			}
		});

		BitmapFontModel.MetricsRenderer metrics = new BitmapFontModel.MetricsRenderer();

		fontModel.render(model.createRenderArgs(), metrics);

		double width = metrics.getWidth() * scale;
		double height = metrics.getHeight() * scale;

		setMinSize(width, height);
		setMaxSize(width, height);
		setPrefSize(width, height);

	}
}
