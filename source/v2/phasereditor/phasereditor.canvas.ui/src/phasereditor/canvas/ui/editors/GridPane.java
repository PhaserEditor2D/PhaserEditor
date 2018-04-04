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

import org.eclipse.swt.graphics.RGB;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.ui.shapes.GroupNode;

/**
 * @author arian
 *
 */
public class GridPane extends Canvas {
	private final static double MIN_WIDTH = 10;
	private final static double MAX_WIDTH = 100;

	private ObjectCanvas _canvas;

	public GridPane(ObjectCanvas canvas) {
		super();
		_canvas = canvas;

		Scene scene = canvas.getScene();
		widthProperty().bind(scene.widthProperty());
		heightProperty().bind(scene.heightProperty());
	}

	public void repaint() {
		GroupNode world = _canvas.getWorldNode();
		EditorSettings settings = _canvas.getSettingsModel();

		GraphicsContext g2 = getGraphicsContext2D();

		// background

		{
			RGB rgb = settings.getBackgroundColor();
			g2.setFill(Color.rgb(rgb.red, rgb.green, rgb.blue));
			g2.fillRect(0, 0, getScene().getWidth(), getScene().getHeight());
		}

		// scene color

		// this will be used to paint the game.stage.backgroundColor

		// CanvasMainSettings settings = _canvas.getSettingsModel();
		// RGB rgb = settings.getSceneColor();
		// if (rgb != null && !settings.isTransparent()) {
		// Bounds b = world.localToScene(new BoundingBox(0, 0,
		// settings.getSceneWidth(), settings.getSceneHeight()));
		// g2.setFill(Color.rgb(rgb.red, rgb.green, rgb.blue));
		// g2.fillRect(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
		// }

		// grid
		if (settings.isShowGrid()) {
			RGB rgb = settings.getGridColor();
			g2.setStroke(Color.rgb(rgb.red, rgb.green, rgb.blue));
			g2.setLineWidth(1);

			Point2D origin = world.localToScene(0, 0);

			Bounds proj;

			double xStep = 10;
			double yStep = 10;

			{
				if (settings.isEnableStepping()) {
					xStep = settings.getStepWidth();
					yStep = settings.getStepHeight();
				}
			}

			proj = world.localToScene(new BoundingBox(0, 0, xStep, yStep));
			pass(g2, origin, proj);

			proj = world.localToScene(new BoundingBox(0, 0, xStep * 10, yStep * 10));
			pass(g2, origin, proj);

			proj = world.localToScene(new BoundingBox(0, 0, xStep * 100, yStep * 100));
			pass(g2, origin, proj);

			proj = world.localToScene(new BoundingBox(0, 0, xStep * 1000, yStep * 1000));
			pass(g2, origin, proj);
		}
	}

	private void pass(GraphicsContext g2, Point2D origin, Bounds proj) {
		if (proj.getWidth() < MIN_WIDTH) {
			return;
		}

		double width = Math.floor(getScene().getWidth());
		double height = Math.floor(getScene().getHeight());

		double xoffs = origin.getX() % proj.getWidth();
		double yoffs = origin.getY() % proj.getHeight();

		double alpha = 1;

		if (proj.getWidth() < MAX_WIDTH) {
			double min = MIN_WIDTH;
			double alpha1 = (proj.getWidth() - min) / (MAX_WIDTH - min) * 0.6;
			alpha = Math.max(0.02, Math.min(0.6, alpha1));
		}

		g2.setGlobalAlpha(alpha);

		for (double x = 0; x < width + proj.getWidth(); x += proj.getWidth()) {
			double x2 = Math.floor(x + xoffs);
			for (double y = 0; y < height + proj.getHeight(); y += proj.getHeight()) {
				double y2 = Math.floor(y + yoffs);
				g2.strokeLine(x2, 0, x2, height);
				g2.strokeLine(0, y2, width, y2);
			}
		}

		g2.setGlobalAlpha(1);
	}
}
