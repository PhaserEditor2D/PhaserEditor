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
package phasereditor.canvas.ui.editors;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import phasereditor.canvas.core.CanvasMainSettings;
import phasereditor.canvas.ui.shapes.GroupNode;

/**
 * @author arian
 *
 */
public class FrontGridPane extends Canvas {
	private static final double FONT_SIZE = 10;
	private ObjectCanvas _canvas;

	public FrontGridPane(ObjectCanvas canvas) {
		super();
		_canvas = canvas;

		Scene scene = canvas.getScene();
		widthProperty().bind(scene.widthProperty());
		heightProperty().bind(scene.heightProperty());
	}

	public void repaint() {
		paintWorldBounds();
		paintGridLabels();
	}

	private void paintGridLabels() {
		GraphicsContext g2 = getGraphicsContext2D();
		g2.setFont(Font.font("Monospaced", FontWeight.BOLD, FONT_SIZE));

		g2.setFill(Color.BLACK);

		GroupNode world = _canvas.getWorldNode();
		Bounds proj;
		double i = 10;
		do {
			proj = world.localToScene(new BoundingBox(0, 0, i, i));
			i *= 5;
		} while (proj.getWidth() < 30);

		Point2D origin = world.localToScene(0, 0);
		double xoffs = origin.getX() % proj.getWidth();
		double yoffs = origin.getY() % proj.getHeight();

		for (double x = 0; x < getScene().getWidth() + proj.getWidth(); x += proj.getWidth()) {
			double x2 = x + xoffs;
			double x3 = Math.round(x2);
			g2.setStroke(Color.BLACK);
			g2.strokeLine(x3, 0, x3, 5);

			g2.setStroke(Color.LIGHTGREY);
			Point2D p = world.sceneToLocal(x2, 0);
			String str = Integer.toString((int) Math.round(p.getX()));
			double x4 = Math.round(x2 - new Text(str).getBoundsInLocal().getWidth() / 2);
			g2.strokeText(str, x4, 15, proj.getWidth());
			g2.fillText(str, x4, 15, proj.getWidth());
		}

		for (double y = 0; y < getScene().getHeight() + proj.getHeight(); y += proj.getHeight()) {
			double y2 = y + yoffs;
			double y3 = Math.round(y2);
			g2.setStroke(Color.BLACK);
			g2.strokeLine(0, y3, 5, y3);

			g2.setStroke(Color.LIGHTGREY);
			Point2D p = world.sceneToLocal(0, y2);
			String str = Integer.toString((int) Math.round(p.getY()));
			double y4 = Math.round(y2 + 2);
			g2.strokeText(str, 6, y4);
			g2.fillText(str, 6, y4);
		}
	}

	private void paintWorldBounds() {
		GroupNode node = _canvas.getWorldNode();
		CanvasMainSettings settings = _canvas.getSettingsModel();

		Bounds b = node.localToScene(new BoundingBox(0, 0, settings.getSceneWidth(), settings.getSceneHeight()));

		GraphicsContext g2 = getGraphicsContext2D();
		g2.clearRect(0, 0, getWidth(), getHeight());

		g2.setStroke(Color.BLACK);
		g2.strokeRect(b.getMinX() - 1, b.getMinY() - 1, b.getWidth() + 2, b.getHeight() + 2);

		g2.setStroke(Color.WHITE);
		g2.strokeRect(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
	}

}
