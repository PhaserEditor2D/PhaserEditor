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

import org.eclipse.swt.graphics.RGB;

import javafx.geometry.Point2D;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import phasereditor.canvas.core.TextModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.grid.PGridColorProperty;
import phasereditor.canvas.ui.editors.grid.PGridEnumProperty;
import phasereditor.canvas.ui.editors.grid.PGridModel;
import phasereditor.canvas.ui.editors.grid.PGridNumberProperty;
import phasereditor.canvas.ui.editors.grid.PGridSection;
import phasereditor.canvas.ui.editors.grid.PGridStringProperty;
import phasereditor.ui.ColorButtonSupport;

/**
 * 
 * @author arian
 */
@SuppressWarnings("boxing")
public class TextControl extends BaseSpriteControl<TextModel> {

	private PGridStringProperty _text_property;

	public TextControl(ObjectCanvas canvas, TextModel model) {
		super(canvas, model);
	}

	@Override
	protected IObjectNode createNode() {
		return new TextNode(this);
	}

	@Override
	public double getTextureWidth() {
		return getSize().getX();
	}

	@Override
	public double getTextureHeight() {
		return getSize().getY();

	}

	@Override
	public void updateFromModel() {
		TextNode node = getNode();

		node.updateFromModel();

		super.updateFromModel();
	}

	private Point2D getSize() {
		return getNode().getSize();
	}

	@Override
	public TextNode getNode() {
		return (TextNode) super.getNode();
	}

	public PGridStringProperty getTextProperty() {
		return _text_property;
	}

	@Override
	protected void initPGridModel(PGridModel propModel) {
		super.initPGridModel(propModel);

		PGridSection section = new PGridSection("Text");

		_text_property = new PGridStringProperty(getId(), "text", help("Phaser.Text.text"), "Write the text.") {

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
		};
		section.add(_text_property);

		{
			List<String> names = Font.getFamilies();
			section.add(new PGridEnumProperty<String>(getId(), "style.font", "The name of the font",
					names.toArray(new String[names.size()])) {

				@Override
				public String getValue() {
					return getModel().getStyleFont();
				}

				@Override
				public void setValue(String value, boolean notify) {
					getModel().setStyleFont(value);
					if (notify) {
						updateFromPropertyChange();
						getCanvas().getSelectionBehavior().updateSelectedNodes_async();
					}
				}

				@Override
				public boolean isModified() {
					return !getModel().getStyleFont().equals(TextModel.DEF_STYLE_FONT);
				}
			});
		}

		section.add(new PGridNumberProperty(getId(), "style.fontSize", "The size of the font (eg. 20pt)") {

			@Override
			public Double getValue() {
				return (double) getModel().getStyleFontSize();
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getModel().setStyleFontSize(value.intValue());
				if (notify) {
					updateFromPropertyChange();
					getCanvas().getSelectionBehavior().updateSelectedNodes_async();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getStyleFontSize() != TextModel.DEF_STYLE_FONT_SIZE;
			}
		});

		section.add(new PGridEnumProperty<FontWeight>(getId(), "style.fontWeight",
				"The weight of the font (eg. 'bold').", new FontWeight[] { FontWeight.NORMAL,
						FontWeight.BOLD } /* FontWeight.values() */) {

			@Override
			public FontWeight getValue() {
				return getModel().getStyleFontWeight();
			}

			@Override
			public void setValue(FontWeight value, boolean notify) {
				getModel().setStyleFontWeight(value);
				if (notify) {
					updateFromPropertyChange();
					getCanvas().getSelectionBehavior().updateSelectedNodes_async();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getStyleFontWeight() != FontWeight.BOLD;
			}
		});

		section.add(new PGridEnumProperty<FontPosture>(getId(), "style.fontStyle",
				"The style of the font (eg. 'italic').", FontPosture.values()) {

			@Override
			public FontPosture getValue() {
				return getModel().getStyleFontStyle();
			}

			@Override
			public void setValue(FontPosture value, boolean notify) {
				getModel().setStyleFontStyle(value);
				if (notify) {
					updateFromPropertyChange();
					getCanvas().getSelectionBehavior().updateSelectedNodes_async();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getStyleFontStyle() != FontPosture.REGULAR;
			}
		});

		section.add(new PGridColorProperty(getId(), "style.fill",
				"A canvas fillstyle that will be used on the text eg 'red', '#00FF00'.") {
			@Override
			public Object getDefaultValue() {
				return new RGB(0, 0, 0);
			}

			@Override
			public void setValue(RGB value, boolean notify) {
				getModel().setStyleFill(ColorButtonSupport.getHexString(value));
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return !getModel().getStyleFill().equals(TextModel.DEF_STYLE_FILL);
			}

			@Override
			public RGB getValue() {
				return ColorButtonSupport.toRGB(getModel().getStyleFill());
			}
		});

		section.add(new PGridColorProperty(getId(), "style.stroke",
				"A canvas stroke style that will be used on the text stroke eg 'blue', '#FCFF00'.") {

			@Override
			public Object getDefaultValue() {
				return new RGB(0, 0, 0);
			}

			@Override
			public void setValue(RGB value, boolean notify) {
				getModel().setStyleStroke(ColorButtonSupport.getHexString(value));
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return !getModel().getStyleStroke().equals(TextModel.DEF_STYLE_STROKE);
			}

			@Override
			public RGB getValue() {
				return ColorButtonSupport.toRGB(getModel().getStyleStroke());
			}
		});

		section.add(new PGridNumberProperty(getId(), "style.strokeThickness",
				"A number that represents the thickness of the stroke. Default is 0 (no stroke).") {

			@Override
			public Double getValue() {
				return (double) getModel().getStyleStrokeThickness();
			}

			@Override
			public void setValue(Double value, boolean notify) {
				getModel().setStyleStrokeThickness(value.intValue());
				if (notify) {
					updateFromPropertyChange();
					getCanvas().getSelectionBehavior().updateSelectedNodes_async();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getStyleStrokeThickness() != 0;
			}
		});

		section.add(new PGridColorProperty(getId(), "style.backgroundColor",
				"A canvas fillstyle that will be used as the background for the whole Text object. Set to `null` to disable.") {

			@Override
			public Object getDefaultValue() {
				return null;
			}

			@Override
			public void setValue(RGB value, boolean notify) {
				getModel().setStyleBackgroundColor(value == null ? null : ColorButtonSupport.getHexString(value));
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getStyleBackgroundColor() != null;
			}

			@Override
			public RGB getValue() {
				String c = getModel().getStyleBackgroundColor();
				return c == null ? null : ColorButtonSupport.toRGB(c);
			}
		});

		section.add(new PGridEnumProperty<TextAlignment>(getId(), "style.align",
				"Horizontal alignment of each line in multiline text. Can be: 'left', 'center' or 'right'. Does not affect single lines of text (see `textBounds` and `boundsAlignH` for that)",
				new TextAlignment[] { TextAlignment.LEFT, TextAlignment.CENTER, TextAlignment.RIGHT }) {

			@Override
			public TextAlignment getValue() {
				return getModel().getStyleAlign();
			}

			@Override
			public void setValue(TextAlignment value, boolean notify) {
				getModel().setStyleAlign(value);
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public boolean isModified() {
				return getModel().getStyleAlign() != TextAlignment.LEFT;
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
