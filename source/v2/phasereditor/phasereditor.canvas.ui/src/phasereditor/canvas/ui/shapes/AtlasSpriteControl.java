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

import java.util.List;

import phasereditor.assetpack.core.AtlasAssetModel.Frame;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.canvas.core.AtlasSpriteModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.grid.PGridFrameProperty;
import phasereditor.ui.properties.PGridModel;

/**
 * @author arian
 *
 */
public class AtlasSpriteControl extends BaseSpriteControl<AtlasSpriteModel> {

	public AtlasSpriteControl(ObjectCanvas canvas, AtlasSpriteModel model) {
		super(canvas, model);
	}

	@Override
	public AtlasSpriteNode getNode() {
		return (AtlasSpriteNode) super.getNode();
	}

	@Override
	protected ISpriteNode createNode() {
		AtlasSpriteNode node = new AtlasSpriteNode(this);
		return node;
	}

	@Override
	public double getTextureWidth() {
		return getModel().getAssetKey().getSourceW();
	}

	@Override
	public double getTextureHeight() {
		return getModel().getAssetKey().getSourceH();
	}

	@Override
	protected void initPGridModel(PGridModel propModel) {
		super.initPGridModel(propModel);

		String initFrameName = getModel().getAssetKey().getKey();

		PGridFrameProperty frame_property = new PGridFrameProperty(getId(), "frameName",
				help("Phaser.Sprite.frameName")) {

			@Override
			public boolean isModified() {
				return initFrameName != getModel().getAssetKey().getKey();
			}

			@Override
			public void setValue(IAssetFrameModel value, boolean notify) {
				getModel().setAssetKey((Frame) value);
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public IAssetFrameModel getValue() {
				return getModel().getAssetKey();
			}

			@Override
			public List<?> getFrames() {
				return getModel().getAssetKey().getAsset().getSubElements();
			}

			@Override
			public boolean isReadOnly() {
				return getModel().isPrefabReadOnly("texture");
			}

		};

		getSpriteSection().add(frame_property);
	}
}
