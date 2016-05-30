// The MIT License (MIT)
//
// Copyright (c) 2016 Arian Fornaris
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
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AtlasAssetModel.FrameItem;
import phasereditor.canvas.core.AtlasSpriteShapeModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;

/**
 * @author arian
 *
 */
public class AtlasSpriteControl extends BaseSpriteShapeControl<AtlasSpriteShapeModel> {

	private FrameItem _frame;

	public AtlasSpriteControl(ObjectCanvas canvas, AtlasSpriteShapeModel model) {
		super(canvas, model);
	}

	@Override
	protected SpriteNode createShapeNode() {
		_frame = getModel().getFrame();

		AtlasAssetModel asset = (AtlasAssetModel) _frame.getAsset();

		SpriteNode node = createImageNode(asset.getTextureFile());

		Rectangle2D viewport = new Rectangle2D(_frame.getFrameX(), _frame.getFrameY(), _frame.getSpriteW(),
				_frame.getSpriteH());

		node.setViewport(viewport);

		return node;
	}

	@Override
	public double getTextureWidth() {
		return _frame.getSpriteW();
	}

	@Override
	public double getTextureHeight() {
		return _frame.getSpriteH();
	}
}
