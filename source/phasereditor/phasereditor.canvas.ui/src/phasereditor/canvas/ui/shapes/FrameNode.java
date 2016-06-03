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

import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import phasereditor.assetpack.core.FrameData;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.ui.ImageCache;

/**
 * @author arian
 *
 */
public class FrameNode extends Pane {

	private ImageView _image;

	public FrameNode(IAssetFrameModel frameModel) {
		_image = new ImageView(ImageCache.getFXImage(frameModel.getImageFile()));
		getChildren().add(_image);

		updateContent(frameModel);
	}
	
	public ImageView getImageView() {
		return _image;
	}

	protected void updateContent(IAssetFrameModel frameModel) {
		FrameData frame = frameModel.getFrameData();

		_image.relocate(frame.dst.x, frame.dst.y);
		_image.setViewport(new Rectangle2D(frame.src.x, frame.src.y, frame.src.width, frame.src.height));

		setMaxSize(frame.srcSize.x, frame.srcSize.y);
		setMinSize(frame.srcSize.x, frame.srcSize.y);
	}
}
