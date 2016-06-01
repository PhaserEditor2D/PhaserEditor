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
package phasereditor.canvas.ui.shapes;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AtlasAssetModel.FrameItem;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.ui.AssetPackUI.FrameData;
import phasereditor.canvas.core.TileSpriteModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.grid.PGridModel;
import phasereditor.canvas.ui.editors.grid.PGridNumberProperty;
import phasereditor.canvas.ui.editors.grid.PGridProperty;
import phasereditor.canvas.ui.editors.grid.PGridSection;

/**
 * @author arian
 *
 */
public class TileSpriteControl extends BaseSpriteControl<TileSpriteModel> {

	private PGridProperty<Double> _width_property;
	private PGridProperty<Double> _height_property;
	private PGridNumberProperty _tilePositionX_property;
	private PGridNumberProperty _tilePositionY_property;
	private PGridNumberProperty _tileScaleX_property;
	private PGridNumberProperty _tileScaleY_property;

	public TileSpriteControl(ObjectCanvas canvas, TileSpriteModel model) {
		super(canvas, model);
	}

	@Override
	public void updateFromModel() {
		super.updateFromModel();
		getNode().updateFromModel();
	}

	@Override
	public double getTextureWidth() {
		return getModel().getWidth();
	}

	@Override
	public double getTextureHeight() {
		return getModel().getHeight();
	}

	@Override
	public TileSpriteNode getNode() {
		return (TileSpriteNode) super.getNode();
	}

	@Override
	protected IObjectNode createNode() {
		IAssetKey key = getModel().getAssetKey();

		{
			AssetModel asset = key.getAsset();
			if (asset instanceof ImageAssetModel) {
				IFile file = ((ImageAssetModel) asset).getUrlFile();
				return new TileSpriteNode(file, null, this);
			}
		}

		if (key instanceof AtlasAssetModel.FrameItem) {
			AtlasAssetModel.FrameItem frame = (FrameItem) key;
			AtlasAssetModel asset = frame.getAsset();
			IFile file = asset.getTextureFile();
			FrameData fd = new FrameData();
			fd.src = new Rectangle(frame.getFrameX(), frame.getFrameY(), frame.getFrameW(), frame.getFrameH());
			fd.dst = new Rectangle(frame.getSpriteX(), frame.getSpriteY(), frame.getSpriteW(), frame.getSpriteH());
			fd.srcSize = new Point(frame.getSourceW(), frame.getSourceH());
			return new TileSpriteNode(file, fd, this);
		}

		return null;
	}

	@SuppressWarnings("boxing")
	@Override
	protected void initPGridModel(PGridModel propModel) {
		super.initPGridModel(propModel);
		PGridSection section = new PGridSection("Sprite Tile");
		propModel.getSections().add(section);

		_tilePositionX_property = new PGridNumberProperty("tilePosition.x") {

			@Override
			public Double getValue() {
				return getModel().getTilePositionX();
			}

			@Override
			public void setValue(Double value) {
				getModel().setTilePositionX(value);
				updateGridChange();
			}

			@Override
			public boolean isModified() {
				return getModel().getTilePositionX() != 0;
			}
		};

		_tilePositionY_property = new PGridNumberProperty("tilePosition.y") {

			@Override
			public Double getValue() {
				return getModel().getTilePositionY();
			}

			@Override
			public void setValue(Double value) {
				getModel().setTilePositionY(value);
				updateGridChange();
			}

			@Override
			public boolean isModified() {
				return getModel().getTilePositionY() != 0;
			}
		};

		_width_property = new PGridNumberProperty("width") {

			@Override
			public Double getValue() {
				return getModel().getWidth();
			}

			@Override
			public void setValue(Double value) {
				getModel().setWidth(value);
				updateGridChange();
			}

			@Override
			public boolean isModified() {
				return true;
			}
		};

		_height_property = new PGridNumberProperty("height") {

			@Override
			public Double getValue() {
				return getModel().getHeight();
			}

			@Override
			public void setValue(Double value) {
				getModel().setHeight(value);
				updateGridChange();
			}

			@Override
			public boolean isModified() {
				return true;
			}
		};

		_tileScaleX_property = new PGridNumberProperty("tileScale.x") {

			@Override
			public Double getValue() {
				return getModel().getTileScaleX();
			}

			@Override
			public void setValue(Double value) {
				getModel().setTileScaleX(value);
				updateGridChange();
			}

			@Override
			public boolean isModified() {
				return getModel().getTileScaleX() != 1;
			}
		};

		_tileScaleY_property = new PGridNumberProperty("tileScale.y") {

			@Override
			public Double getValue() {
				return getModel().getTileScaleY();
			}

			@Override
			public void setValue(Double value) {
				getModel().setTileScaleY(value);
				updateGridChange();
			}

			@Override
			public boolean isModified() {
				return getModel().getTileScaleY() != 1;
			}
		};

		section.add(_tilePositionX_property);
		section.add(_tilePositionY_property);
		section.add(_width_property);
		section.add(_height_property);
		section.add(_tileScaleX_property);
		section.add(_tileScaleY_property);

	}

}
