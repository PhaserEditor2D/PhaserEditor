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

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.SpritesheetAssetModel.FrameModel;
import phasereditor.canvas.core.SpritesheetSpriteModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.grid.PGridFrameProperty;
import phasereditor.canvas.ui.editors.grid.PGridModel;

/**
 * @author arian
 *
 */
public class SpritesheetSpriteControl extends BaseSpriteControl<SpritesheetSpriteModel> {

	public SpritesheetSpriteControl(ObjectCanvas canvas, SpritesheetSpriteModel model) {
		super(canvas, model);
	}

	@Override
	protected SpritesheetSpriteNode createNode() {
		return new SpritesheetSpriteNode(this);
	}

	@Override
	public SpritesheetSpriteNode getNode() {
		return (SpritesheetSpriteNode) super.getNode();
	}

	@Override
	public double getTextureWidth() {
		return getModel().getAssetKey().getAsset().getFrameWidth();
	}

	@Override
	public double getTextureHeight() {
		return getModel().getAssetKey().getAsset().getFrameHeight();
	}

	@Override
	protected void initPGridModel(PGridModel propModel) {
		super.initPGridModel(propModel);

		int initFrameIndex = getModel().getAssetKey().getIndex();

		PGridFrameProperty frame_property = new PGridFrameProperty(getUniqueId(), "frame") {

			@Override
			public boolean isModified() {
				return initFrameIndex != getModel().getAssetKey().getIndex();
			}

			@Override
			public void setValue(IAssetFrameModel value) {
				getModel().setAssetKey((FrameModel) value);
				updateGridChange();
			}

			@Override
			public IAssetFrameModel getValue() {
				return getModel().getAssetKey();
			}

			@Override
			public List<?> getFrames() {
				return getModel().getFrames();
			}

			@Override
			public String getTooltip() {
				return help("Phaser.Sprite.frame");
			}
		};
		getSpriteSection().add(frame_property);
	}
}
