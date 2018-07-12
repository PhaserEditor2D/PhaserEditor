// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.assetpack.core;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import phasereditor.ui.PhaserEditorUI;

public class TilemapAssetModel extends AssetModel {
	public static final String TILEMAP_CSV = "CSV";
	public static final String TILEMAP_TILED_JSON = "TILED_JSON";
	private String _url;
	private String _data;
	private String _format;
	private TilemapJSON _tilemapJSON;
	private int[][] _csvData;

	public TilemapAssetModel(String key, AssetSectionModel section) throws JSONException {
		super(key, AssetType.tilemap, section);
		buildTilemap();
	}

	public TilemapAssetModel(JSONObject jsonDoc, AssetSectionModel section) throws JSONException {
		super(jsonDoc, section);
		_url = jsonDoc.optString("url", null);
		// the data can be a string (with CSV format) or a JSON object.
		Object obj = jsonDoc.opt("data");
		if (obj == null) {
			_data = null;
		} else {
			if (obj instanceof JSONObject) {
				_data = ((JSONObject) obj).toString(4);
			} else {
				_data = (String) obj;
			}
		}
		_format = jsonDoc.optString("format", TILEMAP_CSV);
	}

	public class TilemapJSON {
		private List<Layer> _layers;
		private List<Tileset> _tilesets;

		public TilemapJSON() {
			_layers = new ArrayList<>();
			_tilesets = new ArrayList<>();
		}

		public List<Layer> getLayers() {
			return _layers;
		}

		public List<Tileset> getTilesets() {
			return _tilesets;
		}
	}

	public class Layer implements IAssetElementModel {
		private String _name;

		@Override
		public String getName() {
			return _name;
		}

		public void setName(String name) {
			_name = name;
		}

		@Override
		public AssetModel getAsset() {
			return TilemapAssetModel.this;
		}

		@Override
		public String toString() {
			return "Layer of tilemap '" + getKey() + "'.";
		}
	}

	public class Tileset implements IAssetElementModel {
		private String _name;
		private String _image;

		@Override
		public AssetModel getAsset() {
			return TilemapAssetModel.this;
		}

		@Override
		public String getName() {
			return _name;
		}

		public void setName(String name) {
			_name = name;
		}

		public String getImage() {
			return _image;
		}

		public void setImage(String image) {
			_image = image;
		}

		public IFile getImageFile() {
			if (_image != null) {
				IFile file = getFileFromUrl(getUrl());
				if (file != null) {
					// looks for the image file in the same folder of the
					// tilemap
					IFolder folder = (IFolder) file.getParent();
					IFile imgFile = folder.getFile(_image);
					if (imgFile.exists()) {
						return imgFile;
					}
				}
			}
			return null;
		}
	}

	public void buildTilemap() {
		if (_format == null) {
			return;
		}

		_csvData = new int[0][0];
		_tilemapJSON = new TilemapJSON();

		if (isJSONFormat()) {
			buildTilemapJSON();
		} else {
			buildTilemapCSV();
		}

	}

	@SuppressWarnings("boxing")
	private void buildTilemapCSV() {
		List<List<Integer>> data = new ArrayList<>();

		try {
			IFile file = getFileFromUrl(_url);
			if (file != null && file.exists()) {
				try (InputStream input = file.getContents()) {

					StringBuilder token = new StringBuilder();

					BufferedReader reader = new BufferedReader(new InputStreamReader(input));
					String line;
					while ((line = reader.readLine()) != null) {

						List<Integer> dataRow = new ArrayList<>();

						for (char c : line.toCharArray()) {
							if (c == ',') {
								Integer tile;
								try {
									tile = Integer.valueOf(Integer.parseInt(token.toString()));
								} catch (Exception e) {
									tile = -1;
								}
								dataRow.add(tile);
								token = new StringBuilder();
							} else {
								token.append(c);
							}
						}

						dataRow.add(Integer.valueOf(Integer.parseInt(token.toString())));
						token = new StringBuilder();

						data.add(dataRow);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		int rows = data.size();

		if (rows == 0) {
			_csvData = new int[0][0];
		} else {
			// row x column
			int cols = data.get(0).size();

			int[][] csvData = new int[rows][cols];

			for (int i = 0; i < rows; i++) {
				List<Integer> row = data.get(i);
				for (int j = 0; j < cols; j++) {
					csvData[i][j] = row.get(j).intValue();
				}
			}
			_csvData = csvData;
		}

	}

	public int[][] getCsvData() {
		if (_csvData == null) {
			buildTilemap();
		}
		return _csvData;
	}

	private void buildTilemapJSON() {
		TilemapJSON tilemap = new TilemapJSON();
		try {
			String data = normalizeString(_data);
			if (data == null) {
				IFile file = getFileFromUrl(_url);
				if (file != null && file.exists()) {
					try (InputStream input = file.getContents()) {
						data = PhaserEditorUI.readString(input);
					}
				}
			}
			if (data != null) {
				JSONObject obj = new JSONObject(data);
				{
					List<Layer> layers = tilemap.getLayers();
					JSONArray array = obj.getJSONArray("layers");
					for (int i = 0; i < array.length(); i++) {
						JSONObject elem = array.getJSONObject(i);
						Layer layerInfo = new Layer();
						layerInfo.setName(elem.getString("name"));
						layers.add(layerInfo);
					}
				}
				{
					List<Tileset> tilesets = tilemap.getTilesets();
					JSONArray array = obj.getJSONArray("tilesets");
					for (int i = 0; i < array.length(); i++) {
						JSONObject elem = array.getJSONObject(i);
						Tileset tileset = new Tileset();
						tileset.setName(elem.getString("name"));
						tileset.setImage(elem.getString("image"));
						tilesets.add(tileset);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		_tilemapJSON = tilemap;
	}

	public TilemapJSON getTilemapJSON() {
		if (_tilemapJSON == null) {
			buildTilemap();
		}
		return _tilemapJSON;
	}

	@Override
	public List<? extends IAssetElementModel> getSubElements() {
		List<IAssetElementModel> list = new ArrayList<>();
		TilemapJSON tilemap = getTilemapJSON();
		list.addAll(tilemap.getLayers());
		list.addAll(tilemap.getTilesets());
		return list;
	}

	@Override
	protected void writeParameters(JSONObject obj) {
		super.writeParameters(obj);
		obj.put("url", _url);
		{
			Object data = null;
			String strData = normalizeString(_data);
			if (strData != null) {
				if (_format.equals(TILEMAP_TILED_JSON)) {
					try {
						data = new JSONObject(strData);
					} catch (JSONException e) {
						throw new RuntimeException("Tilemap '" + getKey() + "' data: " + e.getMessage());
					}
				} else {
					data = strData;
				}
			}
			obj.put("data", data);
		}
		obj.put("format", _format);
	}

	@Override
	public IFile[] computeUsedFiles() {
		return new IFile[] { getUrlFile() };
	}

	public String getUrl() {
		return _url;
	}

	public void setUrl(String url) {
		_url = url;
		firePropertyChange("url");
		buildTilemap();
	}

	public IFile getUrlFile() {
		return getFileFromUrl(_url);
	}

	public String getData() {
		return _data;
	}

	public void setData(String data) {
		_data = data;
		firePropertyChange("data");
		buildTilemap();
	}

	public void setFormat(String format) {
		_format = format;
		firePropertyChange("format");
		buildTilemap();
	}

	public String getFormat() {
		return _format;
	}

	public boolean isJSONFormat() {
		return _format != null && _format.equals(TILEMAP_TILED_JSON);
	}

	public boolean isCSVFormat() {
		return _format != null && _format.equals(TILEMAP_CSV);
	}

	@Override
	public void internalBuild(List<IStatus> problems) {
		validateUrlAndData(problems, "url", _url, "data", _data);

		buildTilemap();
	}

	@Override
	public void fileChanged(IFile file, IFile newFile) {
		String url = getUrlFromFile(file);
		if (url.equals(_url)) {
			_url = getUrlFromFile(newFile);
		}
	}
}
