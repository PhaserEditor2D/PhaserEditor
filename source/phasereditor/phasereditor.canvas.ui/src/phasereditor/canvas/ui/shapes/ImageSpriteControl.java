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

import phasereditor.canvas.core.ImageSpriteModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.ui.ImageCache;

/**
 * @author arian
 *
 */
public class ImageSpriteControl extends BaseSpriteControl<ImageSpriteModel> {
	private ImageSpriteNode _spriteNode;

	public ImageSpriteControl(ObjectCanvas canvas, ImageSpriteModel model) {
		super(canvas, model);
	}

	@Override
	public void updateFromModel() {
		super.updateFromModel();
		getNode().setImage(ImageCache.getFXImage(getModel().getAssetKey().getUrlFile()));
	}
	
	@Override
	protected ImageSpriteNode createNode() {
		_spriteNode = createImageNode(getModel().getAssetKey().getUrlFile());
		return _spriteNode;
	}

	@Override
	public ImageSpriteNode getNode() {
		return (ImageSpriteNode) super.getNode();
	}

	@Override
	public double getTextureWidth() {
		return _spriteNode.getImage().getWidth();
	}

	@Override
	public double getTextureHeight() {
		return _spriteNode.getImage().getHeight();
	}
}
