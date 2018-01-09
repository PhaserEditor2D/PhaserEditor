// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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

import java.util.Arrays;
import java.util.List;

import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.canvas.core.BitmapTextModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.grid.PGridBitmapTextFontProperty;
import phasereditor.canvas.ui.editors.grid.PGridModel;
import phasereditor.canvas.ui.editors.grid.PGridNumberProperty;
import phasereditor.canvas.ui.editors.grid.PGridSection;
import phasereditor.canvas.ui.editors.grid.PGridStringProperty;

/**
 * @author arian
 *
 */
public class BitmapTextControl extends BaseSpriteControl<BitmapTextModel> {

	private PGridStringProperty _text_property;
	private PGridBitmapTextFontProperty _font_property;

	public BitmapTextControl(ObjectCanvas canvas, BitmapTextModel model) {
		super(canvas, model);
	}

	@Override
	public double getTextureWidth() {
		return getNode().getBoundsInLocal().getWidth();
	}

	@Override
	public double getTextureHeight() {
		return getNode().getBoundsInLocal().getHeight();
	}

	@Override
	protected IObjectNode createNode() {
		return new BitmapTextNode(this);
	}

	@Override
	public void updateFromModel() {
		super.updateFromModel();

		getNode().updateFromModel();
	}

	@Override
	public BitmapTextNode getNode() {
		return (BitmapTextNode) super.getNode();
	}

	@Override
	protected void initPrefabPGridModel(List<String> validProperties) {
		super.initPrefabPGridModel(validProperties);

		validProperties.addAll(Arrays.asList(

				BitmapTextModel.PROPSET_TEXT

		));
	}

	@Override
	protected void initPGridModel(PGridModel propModel) {
		super.initPGridModel(propModel);

		PGridSection section = new PGridSection("BitmapText");

		_font_property = new PGridBitmapTextFontProperty(getId(), "font", help("Phaser.BitmapText.font")) {

			@Override
			public void setValue(BitmapFontAssetModel value, boolean notify) {
				getModel().setAssetKey(value);
				if (notify) {
					updateFromPropertyChange();
					getCanvas().getSelectionBehavior().updateSelectedNodes_async();
				}
			}

			@Override
			public BitmapFontAssetModel getValue() {
				return getModel().getAssetKey();
			}

			@Override
			public boolean isModified() {
				return true;
			}

			@Override
			public BitmapTextModel getModel() {
				return BitmapTextControl.this.getModel();
			}

		};

		_text_property = new PGridStringProperty(getId(), "text", help("Phaser.BitmapText.text"), "Write the text.") {

			@Override
			public boolean isModified() {
				return getModel().getText().length() > 0;
			}

			@Override
			public void setValue(String value, boolean notify) {
				getModel().setText(value);
				if (notify) {
					updateFromPropertyChange();
					getCanvas().getSelectionBehavior().updateSelectedNodes_async();
				}
			}

			@Override
			public String getValue() {
				return getModel().getText();
			}

			@Override
			public boolean isReadOnly() {
				return getModel().isPrefabReadOnly(BitmapTextModel.PROPSET_TEXT);
			}
		};

		PGridNumberProperty _size_property = new PGridNumberProperty(getId(), "size", help("Phaser.BitmapText.size")) {

			@Override
			public boolean isModified() {
				return getModel().getSize() != BitmapTextModel.DEF_SIZE;
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getModel().setSize(value.intValue());
				if (notify) {
					updateFromPropertyChange();
					getCanvas().getSelectionBehavior().updateSelectedNodes_async();
				}
			}

			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getSize());
			}

			@Override
			public boolean isReadOnly() {
				return getModel().isPrefabReadOnly(BitmapTextModel.PROPSET_SIZE);
			}
		};

		section.add(_font_property);
		section.add(_text_property);
		section.add(_size_property);
		propModel.getSections().add(section);

		// will never be supported by BitmapText, this should be moved to asset
		// sprites.
		getAnimationsProperty().getSection().remove(getAnimationsProperty());
	}

	public PGridStringProperty getTextProperty() {
		return _text_property;
	}
}
