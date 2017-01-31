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

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.FrameData;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.ButtonSpriteModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.grid.PGridFrameProperty;
import phasereditor.canvas.ui.editors.grid.PGridModel;
import phasereditor.canvas.ui.editors.grid.PGridSection;
import phasereditor.canvas.ui.editors.grid.PGridStringProperty;

/**
 * @author arian
 *
 */
public class ButtonSpriteControl extends BaseSpriteControl<ButtonSpriteModel> {

	private FrameData _frameData;

	public ButtonSpriteControl(ObjectCanvas canvas, ButtonSpriteModel model) {
		super(canvas, model);
	}

	abstract class ButtonFrameProperty extends PGridFrameProperty {
		public ButtonFrameProperty(String name, String tooltip) {
			this(name, true, tooltip);
		}

		public ButtonFrameProperty(String name, boolean allowNull, String tooltip) {
			super(getId(), name, tooltip);
			setAllowNull(allowNull);
		}

		@Override
		public List<?> getFrames() {
			return getModel().getAssetKey().getAsset().getSubElements();
		}

		@Override
		public boolean isModified() {
			return getValue() != null;
		}

	}

	@Override
	protected void initPGridModel(PGridModel propModel) {
		super.initPGridModel(propModel);

		PGridSection section = new PGridSection("Button");

		PGridStringProperty callback_property = new PGridStringProperty(getId(), "callback",
				help("Phaser.Button", "callback")) {

			@Override
			public void setValue(String value, boolean notify) {
				getModel().setCallback(value);
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getValue() != null && getValue().trim().length() > 0;
			}

			@Override
			public String getValue() {
				return getModel().getCallback();
			}

		};
		section.add(callback_property);

		PGridStringProperty callbackContext_property = new PGridStringProperty(getId(), "callbackContext",
				help("Phaser.Button", "callbackContext")) {

			@Override
			public void setValue(String value, boolean notify) {
				getModel().setCallbackContext(value);
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getValue() != null && !getValue().equals("this");
			}

			@Override
			public String getValue() {
				return getModel().getCallbackContext();
			}

		};
		section.add(callbackContext_property);

		boolean isImage = getModel().getAssetKey().getAsset() instanceof ImageAssetModel;

		if (!isImage) {

			PGridFrameProperty overFrame_property = new ButtonFrameProperty("overFrame",
					help("Phaser.Button", "overFrame")) {

				@Override
				public void setValue(IAssetFrameModel value, boolean notify) {
					getModel().setOverFrame(value);
					if (notify) {
						updateFromPropertyChange();
					}
				}

				@Override
				public IAssetFrameModel getValue() {
					return getModel().getOverFrame();
				}

				@Override
				public boolean isReadOnly() {
					return getModel().isPrefabReadOnly("texture");
				}
			};

			// the outFrame is connected to the main assetKey!
			PGridFrameProperty outFrame_property = new ButtonFrameProperty("outFrame",
					help("Phaser.Button", "outFrame")) {

				@Override
				public void setValue(IAssetFrameModel value, boolean notify) {
					getModel().setAssetKey(value);
					if (notify) {
						updateFromPropertyChange();
					}
				}

				@Override
				public IAssetFrameModel getValue() {
					return (IAssetFrameModel) getModel().getAssetKey();
				}

				@Override
				public boolean isReadOnly() {
					return getModel().isPrefabReadOnly("texture");
				}

			};

			PGridFrameProperty downFrame_property = new ButtonFrameProperty("downFrame",
					help("Phaser.Button", "downFrame")) {

				@Override
				public void setValue(IAssetFrameModel value, boolean notify) {
					getModel().setDownFrame(value);
					if (notify) {
						updateFromPropertyChange();
					}
				}

				@Override
				public IAssetFrameModel getValue() {
					return getModel().getDownFrame();
				}

				@Override
				public boolean isReadOnly() {
					return getModel().isPrefabReadOnly("texture");
				}

			};

			PGridFrameProperty upFrame_property = new ButtonFrameProperty("upFrame", help("Phaser.Button", "upFrame")) {

				@Override
				public void setValue(IAssetFrameModel value, boolean notify) {
					getModel().setUpFrame(value);
					if (notify) {
						updateFromPropertyChange();
					}
				}

				@Override
				public IAssetFrameModel getValue() {
					return getModel().getUpFrame();
				}

				@Override
				public boolean isReadOnly() {
					return getModel().isPrefabReadOnly("texture");
				}

			};

			section.add(overFrame_property);
			section.add(outFrame_property);
			section.add(downFrame_property);
			section.add(upFrame_property);
		}

		propModel.getSections().add(section);

	}

	@Override
	public void updateFromModel() {
		IAssetFrameModel frame = (IAssetFrameModel) getModel().getAssetKey();

		_frameData = frame.getFrameData();

		super.updateFromModel();

		getNode().updateContent(frame);
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

	@Override
	protected BaseSpriteModel createModelWithTexture(IAssetFrameModel textureKey) {
		AssetModel old = getModel().getAssetKey().getAsset().getSharedVersion();
		ButtonSpriteModel model = new ButtonSpriteModel(getGroup().getModel(), textureKey);
		
		if (old == textureKey.getAsset().getSharedVersion()) {
			if (model.getDownFrame() != null) {
				model.setDownFrame((IAssetFrameModel) model.getDownFrame().getSharedVersion());
			}

			if (model.getUpFrame() != null) {
				model.setUpFrame((IAssetFrameModel) model.getUpFrame().getSharedVersion());
			}

			if (model.getOverFrame() != null) {
				model.setOverFrame((IAssetFrameModel) model.getOverFrame().getSharedVersion());
			}

			// the out frame is the same of the main asset key
		} else {
			model.setUpFrame(null);
			model.setDownFrame(null);
			model.setOverFrame(null);
		}
		
		return model;
	}
}
