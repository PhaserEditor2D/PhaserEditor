// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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

import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import phasereditor.canvas.core.TextModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.grid.PGridEnumProperty;
import phasereditor.canvas.ui.editors.grid.PGridModel;
import phasereditor.canvas.ui.editors.grid.PGridNumberProperty;
import phasereditor.canvas.ui.editors.grid.PGridSection;
import phasereditor.canvas.ui.editors.grid.PGridStringProperty;

/**
 * 
 * @author arian
 */
public class TextControl extends BaseSpriteControl<TextModel> {

	public TextControl(ObjectCanvas canvas, TextModel model) {
		super(canvas, model);
	}

	@Override
	protected IObjectNode createNode() {
		return new TextNode(this);
	}

	@Override
	public double getTextureWidth() {
		return new Text(getModel().getText()).getBoundsInLocal().getWidth();
	}

	@Override
	public double getTextureHeight() {
		return new Text(getModel().getText()).getBoundsInLocal().getHeight();
	}

	@Override
	public void updateFromModel() {
		TextModel model = getModel();
		TextNode node = getNode();

		node.setText(model.getText());

		Font font = Font.font(model.getFont(), model.getFontWeight(), model.getFontStyle(), model.getFontSize());
		node.setFont(font);

		super.updateFromModel();
	}

	@Override
	public TextNode getNode() {
		return (TextNode) super.getNode();
	}

	@Override
	protected void initPGridModel(PGridModel propModel) {
		super.initPGridModel(propModel);

		PGridSection section = new PGridSection("Text");

		section.add(new PGridStringProperty(getId(), "text", help("Phaser.Text.text"), "Write the text.") {

			@Override
			public boolean isModified() {
				return getModel().getText().length() > 0;
			}

			@Override
			public void setValue(String value, boolean notify) {
				getModel().setText(value);
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public String getValue() {
				return getModel().getText();
			}
		});

		{
			List<String> names = Font.getFamilies();
			section.add(new PGridEnumProperty<String>(getId(), "font", "The name of the font",
					names.toArray(new String[names.size()])) {

				@Override
				public String getValue() {
					return getModel().getFont();
				}

				@Override
				public void setValue(String value, boolean notify) {
					getModel().setFont(value);
					if (notify) {
						updateFromPropertyChange();
					}
				}

				@Override
				public boolean isModified() {
					return !getModel().getFont().equals(TextModel.DEF_FONT);
				}
			});
		}

		section.add(new PGridNumberProperty(getId(), "fontSize", "The size of the font (eg. 20px)") {

			@SuppressWarnings("boxing")
			@Override
			public Double getValue() {
				return (double) getModel().getFontSize();
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getModel().setFontSize(value.intValue());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getFontSize() != TextModel.DEF_FONT_SIZE;
			}
		});

		section.add(new PGridEnumProperty<FontWeight>(getId(), "fontWeight", "The weight of the font (eg. 'bold').",
				 new FontWeight[] {FontWeight.NORMAL, FontWeight.BOLD} /*FontWeight.values()*/) {

			@Override
			public FontWeight getValue() {
				return getModel().getFontWeight();
			}

			@Override
			public void setValue(FontWeight value, boolean notify) {
				getModel().setFontWeight(value);
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getFontWeight() != FontWeight.BOLD;
			}
		});
		
		section.add(new PGridEnumProperty<FontPosture>(getId(), "fontStyle", "The style of the font (eg. 'italic').",
				 FontPosture.values()) {

			@Override
			public FontPosture getValue() {
				return getModel().getFontStyle();
			}

			@Override
			public void setValue(FontPosture value, boolean notify) {
				getModel().setFontStyle(value);
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getFontStyle() != FontPosture.REGULAR;
			}
		});

		propModel.getSections().add(section);

		// will never be supported by text, this should be moved to asset
		// sprites.
		getAnimationsProperty().getSection().remove(getAnimationsProperty());
		// not supported on Phaser v2
		getTintProperty().getSection().remove(getTintProperty());
	}

}
