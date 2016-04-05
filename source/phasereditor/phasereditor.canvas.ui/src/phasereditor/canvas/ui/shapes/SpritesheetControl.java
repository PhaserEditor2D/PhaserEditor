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
import phasereditor.canvas.ui.editors.ShapeCanvas;
import phasereditor.canvas.ui.editors.grid.PGridModel;
import phasereditor.canvas.ui.editors.grid.PGridNumberProperty;
import phasereditor.canvas.ui.editors.grid.PGridSection;

/**
 * @author arian
 *
 */
public class SpritesheetControl extends BaseSpriteShapeControl<SpritesheetShapeModel> {

	private List<FrameData> _frames;
	private PGridNumberProperty _frame_property;

	public SpritesheetControl(ShapeCanvas canvas, SpritesheetShapeModel model) {
		super(canvas, model);
	}

	@Override
	protected SpriteShapeNode createShapeNode() {
		SpritesheetShapeModel model = getModel();

		SpritesheetAssetModel asset = model.getAsset();
		SpriteShapeNode node = createImageNode(asset.getUrlFile());

		Image img = node.getImageView().getImage();
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
	public SpriteShapeNode getNode() {
		return (SpriteShapeNode) super.getNode();
	}

	@Override
	public void updateFromModel() {
		super.updateFromModel();

		updateViewport(getNode());
	}

	@Override
	public double getWidth() {
		return getModel().getAsset().getFrameWidth();
	}

	@Override
	public double getHeight() {
		return getModel().getAsset().getFrameWidth();
	}

	private void updateViewport(SpriteShapeNode node) {
		FrameData frame = getCurrentFrame();
		Rectangle src = frame.src;

		node.setViewport(new Rectangle2D(src.x, src.y, src.width, src.height));
	}

	private FrameData getCurrentFrame() {
		return _frames.get(getModel().getFrameIndex());
	}

	@Override
	protected void initPropertyModel(PGridModel propModel) {
		super.initPropertyModel(propModel);

		_frame_property = new PGridNumberProperty("frame") {

			@Override
			public boolean isModified() {
				return getModel().getFrameIndex() != 0;
			}

			@Override
			public void setValue(Double value) {
				getModel().setFrameIndex(value.intValue());
				updateGridChange();
			}

			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getFrameIndex());
			}
		};

		PGridSection section = new PGridSection("Sprite Sheet");
		section.add(_frame_property);

		propModel.getSections().add(section);
	}
}
