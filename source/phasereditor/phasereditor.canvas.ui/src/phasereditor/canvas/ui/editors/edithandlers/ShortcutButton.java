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
package phasereditor.canvas.ui.editors.edithandlers;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public abstract class ShortcutButton extends Button {
	private Boolean _selected;
	private Background _bgSelected;
	private Background _bgMove;
	private Background _bgNormal;

	public ShortcutButton() {
		this(Color.ALICEBLUE);
	}

	public ShortcutButton(Color bgColor) {

		Color baseColor = bgColor == null ? Color.ALICEBLUE : bgColor;

		int size = 28;

		setSize(size, size);

		Color color = Color.WHITE;
		setBorderColor(color);

		setTextFill(color);

		_bgNormal = new Background(
				new BackgroundFill(baseColor.deriveColor(0, 0, 1, 0.2), CornerRadii.EMPTY, new Insets(0)));

		_bgMove = new Background(
				new BackgroundFill(baseColor.deriveColor(0, 0, 1, 0.4), CornerRadii.EMPTY, new Insets(0)));

		_bgSelected = new Background(
				new BackgroundFill(baseColor.deriveColor(0, 0, 1, 0.6), CornerRadii.EMPTY, new Insets(0)));

		setBackground(_bgNormal);

		setCursor(Cursor.DEFAULT);

		setOnMouseMoved(e -> {
			if (_selected != Boolean.TRUE) {
				setBackground(_bgMove);
			}
		});

		setOnMouseExited(e -> {
			if (_selected != Boolean.TRUE) {
				setBackground(_bgNormal);
			}
		});

		if (_selected == Boolean.TRUE) {
			setBackground(_bgSelected);
		}

		addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {

			doAction();

			e.consume();
		});
	}

	public void setBorderColor(Color color) {
		setBorder(new Border(new BorderStroke(color.deriveColor(0, 0, 1, 0.5), BorderStrokeStyle.SOLID,
				CornerRadii.EMPTY, BorderWidths.DEFAULT)));
	}

	public void setSelected(Boolean selected) {
		_selected = selected;

		if (_selected == Boolean.TRUE) {
			setBackground(_bgSelected);
		} else {
			setBackground(_bgNormal);
		}
	}

	public Boolean isSelected() {
		return _selected;
	}

	public void setIcon(String icon) {
		setGraphic(new ImageView(PhaserEditorUI.getUIIconURL(icon)));
	}

	public SVGPath setIconSVG(String content) {
		SVGPath path = new SVGPath();
		path.setStroke(Color.WHITE);
		path.setContent(content);
		setGraphic(path);
		return path;
	}

	@SuppressWarnings("static-method")
	protected Label createLabel(Object text) {
		Label label = new Label(text + "");
		label.setTextFill(Color.WHITE);
		return label;
	}

	protected void setSize(double w, double h) {
		setPrefSize(w, h);
		setMinSize(w, h);
		setMaxSize(w, h);
	}

	protected abstract void doAction();
}
