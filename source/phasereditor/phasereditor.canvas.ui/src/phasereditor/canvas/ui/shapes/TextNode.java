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

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import phasereditor.canvas.core.TextModel;

/**
 * @author arian
 *
 */
public class TextNode extends Label implements ISpriteNode, ITextSpriteNode {

	private TextControl _control;
	private Text _skinText;

	public TextNode(TextControl control) {
		_control = control;
		setPickOnBounds(true);
	}
	
	@Override
	protected Skin<?> createDefaultSkin() {
		Skin<?> skin = super.createDefaultSkin();
		_skinText = (Text) skin.getNode().lookup(".text");

		updateFromModel();

		return skin;
	}

	public Point2D getSize() {
		Node text = _skinText;
		if (text == null) {
			text = new Text(getModel().getText());
		}

		Bounds b = text.getBoundsInLocal();

		return new Point2D(b.getWidth(), b.getHeight());
	}

	public void updateFromModel() {
		TextModel model = getModel();
		// text
		setText(model.getText());

		// style.font
		FontPosture styleFontStyle = model.getStyleFontStyle();
		Font font = Font.font(model.getStyleFont(), model.getStyleFontWeight(), styleFontStyle,
				model.getStyleFontSize());
		setFont(font);

		// style.fill
		setTextFill(Color.valueOf(model.getStyleFill()));

		if (_skinText != null) {
			// style.stroke
			String stroke = model.getStyleStroke();
			_skinText.setStroke(stroke == null ? null : Color.valueOf(stroke));
			// style.strokeThickness
			_skinText.setStrokeWidth(model.getStyleStrokeThickness());
			//_skinText.setTextOrigin(VPos.TOP);
			_skinText.setBoundsType(TextBoundsType.VISUAL);
		}

		// style.backgroundColor
		String bg = model.getStyleBackgroundColor();
		if (bg == null) {
			setBackground(null);
		} else {
			setBackground(new Background(new BackgroundFill(Color.valueOf(bg), null, null)));
		}
		// style.align
		setTextAlignment(model.getStyleAlign());
	}

	@Override
	public TextModel getModel() {
		return _control.getModel();
	}

	@Override
	public TextControl getControl() {
		return _control;
	}

	@Override
	public Node getNode() {
		return this;
	}

}
