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

import org.eclipse.swt.graphics.RGB;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorInput;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.canvas.core.AnimationModel;
import phasereditor.canvas.core.AssetSpriteModel;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.CircleArcadeBodyModel;
import phasereditor.canvas.core.RectArcadeBodyModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.grid.PGridAnimationsProperty;
import phasereditor.canvas.ui.editors.grid.PGridColorProperty;
import phasereditor.canvas.ui.editors.grid.PGridModel;
import phasereditor.canvas.ui.editors.grid.PGridNumberProperty;
import phasereditor.canvas.ui.editors.grid.PGridSection;
import phasereditor.canvas.ui.editors.grid.PGridStringProperty;
import phasereditor.ui.ColorButtonSupport;

/**
 * @author arian
 *
 */
public abstract class BaseSpriteControl<T extends BaseSpriteModel> extends BaseObjectControl<T> {

	private PGridNumberProperty _anchor_x_property;
	private PGridNumberProperty _anchor_y_property;
	private PGridColorProperty _tint_property;

	private PGridSection _spriteSection;
	private PGridSection _bodyArcadeSection;

	public BaseSpriteControl(ObjectCanvas canvas, T model) {
		super(canvas, model);
	}

	@Override
	protected void updateTransforms(ObservableList<Transform> transforms) {
		super.updateTransforms(transforms);

		{
			// anchor
			double anchorX = getModel().getAnchorX();
			double anchorY = getModel().getAnchorY();
			double x = -getTextureWidth() * anchorX;
			double y = -getTextureHeight() * anchorY;
			Translate anchor = Transform.translate(x, y);
			transforms.add(anchor);
		}
	}

	@Override
	public void updateFromModel() {
		T model = getModel();

		// update node of frame based sprites
		if (model instanceof AssetSpriteModel<?>) {
			IAssetKey key = ((AssetSpriteModel<?>) model).getAssetKey();
			if (key != null && key instanceof IAssetFrameModel) {
				IAssetFrameModel frame = (IAssetFrameModel) key;
				if (getNode() instanceof FrameNode) {
					((FrameNode) getNode()).updateContent(frame);
				}
			}
		}

		super.updateFromModel();

		updateTint();
	}

	protected void updateTint() {
		Node node = getNode();

		String tint = getModel().getTint();

		if (tint != null) {
			Blend multiply = new Blend(BlendMode.MULTIPLY);

			double width = getTextureWidth();
			double height = getTextureHeight();

			ColorInput value = new ColorInput(0, 0, width, height, Color.valueOf(tint));
			multiply.setTopInput(value);

			Blend atop = new Blend(BlendMode.SRC_ATOP);
			atop.setTopInput(multiply);
			node.setEffect(atop);
		}
	}

	@Override
	protected void initPGridModel(PGridModel propModel) {
		super.initPGridModel(propModel);

		createSpriteSection();
		createBodySection();

		propModel.getSections().add(_spriteSection);
		propModel.getSections().add(_bodyArcadeSection);
	}

	private void createSpriteSection() {
		_spriteSection = new PGridSection("Sprite");

		_anchor_x_property = new PGridNumberProperty(getId(), "anchor.x", help("Phaser.Sprite.anchor")) {
			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getAnchorX());
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getModel().setAnchorX(value.doubleValue());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getAnchorX() != 0;
			}
		};

		_anchor_y_property = new PGridNumberProperty(getId(), "anchor.y", help("Phaser.Sprite.anchor")) {
			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getAnchorY());
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getModel().setAnchorY(value.doubleValue());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getAnchorY() != 0;
			}
		};

		_tint_property = new PGridColorProperty(getId(), "tint", help("Phaser.Sprite.tint")) {

			@Override
			public void setValue(RGB value, boolean notify) {
				String tint;
				if (value == null) {
					tint = null;
				} else {
					tint = ColorButtonSupport.getHexString2(value);
				}

				getModel().setTint(tint);
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				String tint = getModel().getTint();
				return tint != null && !tint.equals("0xffffff");
			}

			@Override
			public RGB getValue() {
				String tint = getModel().getTint();

				if (tint == null) {
					return null;
				}

				Color c = Color.valueOf(tint);
				return new RGB((int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
			}
		};

		_tint_property.setDefaultRGB(new RGB(255, 255, 255));

		PGridAnimationsProperty animations_properties = new PGridAnimationsProperty(getId(), "animations",
				help("Phaser.Sprite.animations")) {

			@Override
			public void setValue(List<AnimationModel> value, boolean notify) {
				getModel().setAnimations(value);
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public List<AnimationModel> getValue() {
				return getModel().getAnimations();
			}

			@Override
			public boolean isModified() {
				return !getModel().getAnimations().isEmpty();
			}

			@Override
			public IAssetKey getAssetKey() {
				if (getModel() instanceof AssetSpriteModel) {
					return ((AssetSpriteModel<?>) getModel()).getAssetKey();
				}
				return null;
			}
		};

		PGridStringProperty data_property = new PGridStringProperty(getId(), "data", help("Phaser.Sprite.data"), true) {

			@Override
			public boolean isModified() {
				String data = getModel().getData();
				if (data == null) {
					return false;
				}
				return data.trim().length() > 0;
			}

			@Override
			public String getValue() {
				return getModel().getData();
			}

			@Override
			public void setValue(String value, boolean notify) {
				getModel().setData(value);
				if (notify) {
					updateFromPropertyChange();
				}
			}
		};

		_spriteSection.add(_anchor_x_property);
		_spriteSection.add(_anchor_y_property);
		_spriteSection.add(_tint_property);
		_spriteSection.add(animations_properties);
		_spriteSection.add(data_property);
	}

	private void createBodySection() {
		_bodyArcadeSection = new PGridSection("Arcade") {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isActive() {
				return getModel().getArcadeBody() != null;
			}
		};

		_bodyArcadeSection
				.add(new PGridNumberProperty(getId(), "body.offsetX", help("Phaser.Physics.Arcade.Body.offset")) {

					@Override
					public void setValue(Double value, boolean notify) {
						getModel().getArcadeBody().setOffsetX(value.doubleValue());
						if (notify) {
							updateFromPropertyChange();
						}
					}

					@Override
					public Double getValue() {
						return Double.valueOf(getModel().getArcadeBody().getOffsetX());
					}

					@Override
					public boolean isModified() {
						return getModel().getArcadeBody().getOffsetX() != 0;
					}
				});

		_bodyArcadeSection
				.add(new PGridNumberProperty(getId(), "body.offsetY", help("Phaser.Physics.Arcade.Body.offset")) {

					@Override
					public void setValue(Double value, boolean notify) {
						getModel().getArcadeBody().setOffsetY(value.doubleValue());
						if (notify) {
							updateFromPropertyChange();
						}
					}

					@Override
					public Double getValue() {
						return Double.valueOf(getModel().getArcadeBody().getOffsetY());
					}

					@Override
					public boolean isModified() {
						return getValue().doubleValue() != 0;
					}
				});

		// rect arcade

		String defaultValueMsg = "\n\nPhaser Editor: set -1 to indicate default value.";

		_bodyArcadeSection.add(new PGridNumberProperty(getId(), "body.width",
				help("Phaser.Physics.Arcade.Body.setSize", "width") + defaultValueMsg) {

			@Override
			public void setValue(Double value, boolean notify) {
				((RectArcadeBodyModel) getModel().getArcadeBody()).setWidth(value.doubleValue());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public Double getValue() {
				return Double.valueOf(((RectArcadeBodyModel) getModel().getArcadeBody()).getWidth());
			}

			@Override
			public boolean isModified() {
				return getValue().doubleValue() != -1;
			}

			@Override
			public boolean isActive() {
				return getModel().getBody() instanceof RectArcadeBodyModel;
			}

		});
		_bodyArcadeSection.add(new PGridNumberProperty(getId(), "body.height",
				help("Phaser.Physics.Arcade.Body.setSize", "height") + defaultValueMsg) {

			@Override
			public void setValue(Double value, boolean notify) {
				((RectArcadeBodyModel) getModel().getArcadeBody()).setHeight(value.doubleValue());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public Double getValue() {
				return Double.valueOf(((RectArcadeBodyModel) getModel().getArcadeBody()).getHeight());
			}

			@Override
			public boolean isModified() {
				return getValue().doubleValue() != -1;
			}

			@Override
			public boolean isActive() {
				return getModel().getBody() instanceof RectArcadeBodyModel;
			}

		});

		// circle arcade

		_bodyArcadeSection.add(new PGridNumberProperty(getId(), "body.radius",
				help("Phaser.Physics.Arcade.Body.setCircle", "radius")) {

			@Override
			public void setValue(Double value, boolean notify) {
				((CircleArcadeBodyModel) getModel().getArcadeBody()).setRadius(value.doubleValue());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public Double getValue() {
				return Double.valueOf(((CircleArcadeBodyModel) getModel().getArcadeBody()).getRadius());
			}

			@Override
			public boolean isModified() {
				return true;
			}

			@Override
			public boolean isActive() {
				return getModel().getBody() instanceof CircleArcadeBodyModel;
			}

		});
	}

	protected PGridSection getSpriteSection() {
		return _spriteSection;
	}

	public PGridSection getBodyArcadeSection() {
		return _bodyArcadeSection;
	}

	@Override
	public abstract double getTextureWidth();

	@Override
	public abstract double getTextureHeight();

	@Override
	public double getTextureLeft() {
		T model = getModel();
		return model.getX() - getTextureWidth() * model.getAnchorX();
	}

	@Override
	public double getTextureTop() {
		T model = getModel();
		return model.getY() - getTextureHeight() * model.getAnchorY();
	}

	@Override
	public double getTextureRight() {
		return getTextureLeft() + getTextureWidth();
	}

	@Override
	public double getTextureBottom() {
		return getTextureTop() + getTextureHeight();
	}

}
