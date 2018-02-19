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
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public abstract class ShortcutButton extends Button {

	public ShortcutButton() {
		this(Color.ALICEBLUE);
	}

	public ShortcutButton(Color bgColor) {

		Color baseColor = bgColor == null ? Color.ALICEBLUE : bgColor;

		int size = 28;

		setSize(size, size);

		setBorder(null);
		// setEffect(new BoxBlur(2, 2, 1));

		setTextFill(Color.WHITE);

		Background bg = new Background(
				new BackgroundFill(baseColor.deriveColor(0, 0, 1, 0.2), CornerRadii.EMPTY, new Insets(0)));

		Background bgMove = new Background(
				new BackgroundFill(baseColor.deriveColor(0, 0, 1, 0.4), CornerRadii.EMPTY, new Insets(0)));

		setBackground(bg);

		setCursor(Cursor.DEFAULT);

		setOnMouseMoved(e -> {
			setBackground(bgMove);
		});

		setOnMouseExited(e -> {
			setBackground(bg);
		});

		addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {

			doAction();

			e.consume();
		});
	}

	public void setIcon(String icon) {
		setGraphic(new ImageView(PhaserEditorUI.getUIIconURL(icon)));
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
