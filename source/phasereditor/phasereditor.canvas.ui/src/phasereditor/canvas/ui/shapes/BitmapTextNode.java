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

import java.io.InputStream;

import org.eclipse.core.resources.IFile;

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
public class BitmapTextNode extends Pane implements ISpriteNode {

	private BitmapTextControl _control;
	private Scale _scaleTx;

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
	public BaseSpriteControl<?> getControl() {
		return _control;
	}

	public void updateFromModel() {
		BitmapTextModel model = _control.getModel();

		BitmapFontAssetModel asset = model.getAssetKey();
		IFile fontFile = asset.getFileFromUrl(asset.getAtlasURL());
		Image image = ImageCache.getFXImage(asset.getTextureFile());

		try (InputStream input = fontFile.getContents()) {

			BitmapFontModel fontModel = new BitmapFontModel(input);

			double fontSize = fontModel.getInfoSize();
			int textSize = getModel().getFontSize();
			double scale = textSize / fontSize;

			if (_scaleTx != null) {
				getTransforms().remove(_scaleTx);
			}

			_scaleTx = Transform.scale(scale, scale);
			getTransforms().add(_scaleTx);

			getChildren().clear();

			fontModel.render(model.createRenderArgs(), new BitmapFontRenderer() {

				@Override
				public void render(char c, int x, int y, int srcX, int srcY, int srcW, int srcH) {
					ImageView img = new ImageView(image);
					img.setViewport(new Rectangle2D(srcX, srcY, srcW, srcH));
					img.relocate(x, y);
					getChildren().add(img);
				}
			});

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
