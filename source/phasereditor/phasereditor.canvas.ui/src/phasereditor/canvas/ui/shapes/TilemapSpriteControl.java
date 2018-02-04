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

import phasereditor.assetpack.core.TilemapAssetModel;
import phasereditor.canvas.core.TilemapSpriteModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.grid.PGridModel;
import phasereditor.canvas.ui.editors.grid.PGridNumberProperty;
import phasereditor.canvas.ui.editors.grid.PGridSection;

/**
 * @author arian
 */
public class TilemapSpriteControl extends BaseSpriteControl<TilemapSpriteModel> {

	public TilemapSpriteControl(ObjectCanvas canvas, TilemapSpriteModel model) {
		super(canvas, model);
	}

	@Override
	public double getTextureWidth() {
		TilemapAssetModel asset = getModel().getAssetKey();
		if (asset.isCSVFormat()) {
			int[][] map = asset.getCsvData();
			if (map.length > 0) {
				return map[0].length * getModel().getTileWidth();
			}
		}
		return 100;
	}

	@Override
	public double getTextureHeight() {
		TilemapAssetModel asset = getModel().getAssetKey();
		if (asset.isCSVFormat()) {
			int[][] map = asset.getCsvData();
			if (map.length > 0) {
				return map.length * getModel().getTileHeight();
			}
		}
		return 100;
	}

	@Override
	protected TilemapSpriteNode createNode() {
		return new TilemapSpriteNode(this);
	}

	@Override
	public TilemapSpriteNode getNode() {
		return (TilemapSpriteNode) super.getNode();
	}

	@Override
	public void updateFromModel() {
		super.updateFromModel();

		getNode().updateContent();
	}

	@Override
	protected void initPGridModel(PGridModel propModel) {
		super.initPGridModel(propModel);

		PGridNumberProperty tileWidth_prop = new PGridNumberProperty(getId(), "tileWidth",
				help("Phaser.Tilemap", "tileWidth")) {

			@Override
			public void setValue(Double value, boolean notify) {
				getModel().setTileWidth(value.intValue());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getTileWidth());
			}

			@Override
			public boolean isModified() {
				return getModel().getTileWidth() != 32;
			}
		};

		PGridNumberProperty tileHeight_prop = new PGridNumberProperty(getId(), "tileHeight",
				help("Phaser.Tilemap", "tileWidth")) {

			@Override
			public void setValue(Double value, boolean notify) {
				getModel().setTileHeight(value.intValue());
				if (notify) {
					updateFromPropertyChange();
				}
			}

			@Override
			public Double getValue() {
				return Double.valueOf(getModel().getTileHeight());
			}

			@Override
			public boolean isModified() {
				return getModel().getTileHeight() != 32;
			}
		};

		PGridSection section = new PGridSection("Tilemap");

		section.add(tileWidth_prop);
		section.add(tileHeight_prop);

		propModel.getSections().add(section);
	}

}
