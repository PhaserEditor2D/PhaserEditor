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

import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import phasereditor.assetpack.core.TilemapAssetModel;
import phasereditor.canvas.core.TilemapSpriteModel;

/**
 * @author arian
 */
public class TilemapSpriteNode extends Pane implements ISpriteNode {

	private TilemapSpriteControl _control;

	public TilemapSpriteNode(TilemapSpriteControl control) {
		_control = control;

		setCache(true);
		setCacheHint(CacheHint.SCALE_AND_ROTATE);

		updateContent();
	}

	public void updateContent() {
		if (getModel().getAssetKey().isCSVFormat()) {
			createMap_CSV();
		} else {
			createMap_JSON();
		}
	}

	private void createMap_JSON() {
		Label label = new Label("Tilemap JSON");
		getChildren().setAll(label);
	}

	private void createMap_CSV() {

		int tileW = getModel().getTileWidth();
		int tileH = getModel().getTileHeight();

		TilemapAssetModel asset = getModel().getAssetKey();

		int[][] map = asset.getCsvData();

		if (map.length == 0) {
			getChildren().setAll(new Label("Tilemap CSV: empty map"));
		} else {

			int max = 0;
			for (int i = 0; i < map.length; i++) {
				for (int j = 0; j < map[i].length; j++) {
					max = Math.max(map[i][j], max);
				}
			}

			Color[] colors = generateColors(max);

			Rectangle[] list = new Rectangle[map.length * map[0].length];
			int q = 0;

			for (int i = 0; i < map.length; i++) {
				int[] row = map[i];

				for (int j = 0; j < row.length; j++) {
					int frame = map[i][j];
					Color c = colors[frame % colors.length];
					Rectangle r = new Rectangle(tileW, tileH);
					r.setFill(c);
					r.setLayoutX(j * tileW);
					r.setLayoutY(i * tileH);
					list[q++] = r;
				}
			}
			getChildren().setAll(Arrays.asList(list));
		}
	}

	private static Color[] generateColors(int n) {
		Color[] colors = new Color[n];
		for (int i = 0; i < n; i++) {
			java.awt.Color c = java.awt.Color.getHSBColor((float) i / (float) n, 0.85f, 1f);
			colors[i] = Color.rgb(c.getRed(), c.getGreen(), c.getBlue());
		}
		return colors;
	}

	@Override
	public Node getNode() {
		return this;
	}

	@Override
	public TilemapSpriteModel getModel() {
		return _control.getModel();
	}

	@Override
	public TilemapSpriteControl getControl() {
		return _control;
	}
}
