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

import org.eclipse.swt.graphics.Rectangle;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.assetpack.ui.AssetPackUI.FrameData;
import phasereditor.canvas.core.SpritesheetShapeModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.grid.PGridFrameProperty;
import phasereditor.canvas.ui.editors.grid.PGridModel;
import phasereditor.canvas.ui.editors.grid.PGridSection;

/**
 * @author arian
 *
 */
public class SpritesheetControl extends BaseSpriteShapeControl<SpritesheetShapeModel> {

	private List<FrameData> _frames;
	private PGridFrameProperty _frame_property;

	public SpritesheetControl(ObjectCanvas canvas, SpritesheetShapeModel model) {
		super(canvas, model);
	}

	@Override
	protected SpriteNode createShapeNode() {
		SpritesheetShapeModel model = getModel();

		SpritesheetAssetModel asset = model.getAsset();
		SpriteNode node = createImageNode(asset.getUrlFile());

		Image img = node.getImage();
		Rectangle src = new Rectangle(0, 0, (int) img.getWidth(), (int) img.getHeight());
		Rectangle dst = new Rectangle(0, 0, asset.getFrameWidth(), asset.getFrameHeight());

		_frames = AssetPackUI.generateSpriteSheetRects(asset, src, dst);

		{
			int last = _frames.size() - 1;
			if (model.getFrameIndex() > last) {
				model.setFrameIndex(0);
			}
		}

		updateViewport(node);

		return node;
	}

	@Override
	public SpriteNode getNode() {
		return (SpriteNode) super.getNode();
	}

	@Override
	public void updateFromModel() {
		super.updateFromModel();

		updateViewport(getNode());
	}

	@Override
	public double getTextureWidth() {
		return getModel().getAsset().getFrameWidth();
	}

	@Override
	public double getTextureHeight() {
		return getModel().getAsset().getFrameHeight();
	}

	private void updateViewport(SpriteNode node) {
		FrameData frame = getCurrentFrame();
		Rectangle src = frame.src;

		node.setViewport(new Rectangle2D(src.x, src.y, src.width, src.height));
	}

	private FrameData getCurrentFrame() {
		return _frames.get(getModel().getFrameIndex());
	}

	public List<FrameData> getFrames() {
		return _frames;
	}

	@Override
	protected void initPGridModel(PGridModel propModel) {
		super.initPGridModel(propModel);

		_frame_property = new PGridFrameProperty("frame", this) {

			@Override
			public boolean isModified() {
				return getModel().getFrameIndex() != 0;
			}

			@Override
			public void setValue(Integer value) {
				getModel().setFrameIndex(value.intValue());
				updateGridChange();
			}

			@Override
			public Integer getValue() {
				return Integer.valueOf(getModel().getFrameIndex());
			}
		};

		PGridSection section = new PGridSection("Sprite Sheet");
		section.add(_frame_property);

		propModel.getSections().add(section);
	}
}
