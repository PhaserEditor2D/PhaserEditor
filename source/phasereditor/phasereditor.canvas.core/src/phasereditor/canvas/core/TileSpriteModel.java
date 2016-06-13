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
package phasereditor.canvas.core;

import org.json.JSONObject;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.IAssetKey;

/**
 * @author arian
 *
 */
public class TileSpriteModel extends AssetSpriteModel<IAssetKey> {

	public static final String TYPE_NAME = "tileSprite";

	private static final double DEF_TILE_POSITION = 0;

	private static final double DEF_TILE_SCALE = 1;

	private static final double DEF_WIDTH = 0;

	private static final double DEF_HEIGHT = 0;

	private double _width;
	private double _height;
	private double _tilePositionX;
	private double _tilePositionY;
	private double _tileScaleX;
	private double _tileScaleY;

	public TileSpriteModel(GroupModel parent, IAssetKey assetKey) {
		super(parent, assetKey, TYPE_NAME);

		_tilePositionX = DEF_TILE_POSITION;
		_tilePositionY = DEF_TILE_POSITION;
		_tileScaleX = DEF_TILE_SCALE;
		_tileScaleY = DEF_TILE_SCALE;
		_width = DEF_WIDTH;
		_height = DEF_HEIGHT;
	}

	public TileSpriteModel(GroupModel parent, JSONObject obj) {
		super(parent, TYPE_NAME, obj);
	}

	@Override
	protected void readInfo(JSONObject jsonInfo) {
		super.readInfo(jsonInfo);

		_width = jsonInfo.optDouble("width", DEF_WIDTH);
		_height = jsonInfo.optDouble("height", DEF_HEIGHT);

		_tilePositionX = jsonInfo.optDouble("tilePosition.x", DEF_TILE_POSITION);
		_tilePositionY = jsonInfo.optDouble("tilePosition.y", DEF_TILE_POSITION);
		_tileScaleX = jsonInfo.optDouble("tileScale.x", DEF_TILE_SCALE);
		_tileScaleY = jsonInfo.optDouble("tileScale.y", DEF_TILE_SCALE);
	}

	@Override
	protected void writeInfo(JSONObject jsonInfo) {
		super.writeInfo(jsonInfo);

		jsonInfo.put("width", _width, DEF_WIDTH);
		jsonInfo.put("height", _height, DEF_HEIGHT);

		jsonInfo.put("tilePosition.x", _tilePositionX, DEF_TILE_POSITION);
		jsonInfo.put("tilePosition.y", _tilePositionY, DEF_TILE_POSITION);
		jsonInfo.put("tileScale.x", _tileScaleX, DEF_TILE_SCALE);
		jsonInfo.put("tileScale.y", _tileScaleY, DEF_TILE_SCALE);
		jsonInfo.put("asset-ref", AssetPackCore.getAssetJSONReference(getAssetKey()));
	}

	public double getWidth() {
		return _width;
	}

	public void setWidth(double width) {
		_width = width;
	}

	public double getHeight() {
		return _height;
	}

	public void setHeight(double height) {
		_height = height;
	}

	public double getTilePositionX() {
		return _tilePositionX;
	}

	public void setTilePositionX(double tilePositionX) {
		_tilePositionX = tilePositionX;
	}

	public double getTilePositionY() {
		return _tilePositionY;
	}

	public void setTilePositionY(double tilePositionY) {
		_tilePositionY = tilePositionY;
	}

	public double getTileScaleX() {
		return _tileScaleX;
	}

	public void setTileScaleX(double tileScaleX) {
		_tileScaleX = tileScaleX;
	}

	public double getTileScaleY() {
		return _tileScaleY;
	}

	public void setTileScaleY(double tileScaleY) {
		_tileScaleY = tileScaleY;
	}
}
