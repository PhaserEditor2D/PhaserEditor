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

import java.util.List;

import phasereditor.assetpack.core.FrameData;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.canvas.core.ButtonSpriteModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.grid.PGridFrameProperty;
import phasereditor.canvas.ui.editors.grid.PGridModel;
import phasereditor.canvas.ui.editors.grid.PGridSection;

/**
 * @author arian
 *
 */
public class ButtonSpriteControl extends BaseSpriteControl<ButtonSpriteModel> {

	private IAssetFrameModel _frame;
	private FrameData _frameData;

	public ButtonSpriteControl(ObjectCanvas canvas, ButtonSpriteModel model) {
		super(canvas, model);
	}

	@Override
	protected void initPGridModel(PGridModel propModel) {
		super.initPGridModel(propModel);

		PGridSection section = new PGridSection("Button");

		PGridFrameProperty overFrame_property = new PGridFrameProperty("overFrame") {

			@Override
			public List<?> getFrames() {
				return getModel().getAssetKey().getAsset().getSubElements();
			}
			
			@Override
			public void setValue(IAssetFrameModel value) {
				getModel().setOverFrame(value);
				updateGridChange();
			}

			@Override
			public boolean isModified() {
				return getModel().getOverFrame() != null;
			}

			@Override
			public IAssetFrameModel getValue() {
				if (getModel().getOverFrame() == null) {
					return null;
				}
				return getModel().getOverFrame();
			}

			@Override
			public String getLabel() {
				IAssetFrameModel frame = getModel().getOverFrame();
				return frame == null ? "" : frame.getKey();
			}
		};

		section.add(overFrame_property);

		propModel.getSections().add(section);

	}

	@Override
	public void updateFromModel() {
		_frame = getModel().getFrame();
		_frameData = _frame.getFrameData();
		super.updateFromModel();
	}

	@Override
	public double getTextureWidth() {
		return _frameData.srcSize.x;
	}

	@Override
	public double getTextureHeight() {
		return _frameData.srcSize.y;
	}

	@Override
	protected IObjectNode createNode() {
		return new ButtonSpriteNode(this);
	}

	@Override
	public ButtonSpriteNode getNode() {
		return (ButtonSpriteNode) super.getNode();
	}
}
