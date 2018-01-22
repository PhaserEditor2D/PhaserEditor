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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.json.JSONArray;
import org.json.JSONObject;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.core.AtlasAssetModel.Frame;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;

/**
 * @author arian
 *
 */
public class CanvasModelFactory {
	public static BaseObjectModel createModel(GroupModel parent, Object obj) {
		if (obj instanceof ImageAssetModel.Frame) {
			return new ImageSpriteModel(parent, (ImageAssetModel.Frame) obj);
		} else if (obj instanceof ImageAssetModel) {
			return new ImageSpriteModel(parent, ((ImageAssetModel) obj).getFrame());
		} else if (obj instanceof SpritesheetAssetModel) {
			return new SpritesheetSpriteModel(parent, ((SpritesheetAssetModel) obj).getFrames().get(0));
		} else if (obj instanceof SpritesheetAssetModel.FrameModel) {
			SpritesheetSpriteModel model = new SpritesheetSpriteModel(parent, (SpritesheetAssetModel.FrameModel) obj);
			return model;
		} else if (obj instanceof AtlasAssetModel.Frame) {
			return new AtlasSpriteModel(parent, (Frame) obj);
		} else if (obj instanceof String) {
			return new TextModel(parent);
		} else if (obj instanceof BitmapFontAssetModel) {
			return new BitmapTextModel(parent, (BitmapFontAssetModel) obj);
		}
		return null;
	}

	public static void changeTextureToObjectData(JSONObject data, IAssetKey textureKey) {
		String type = data.getString("type");

		//@formatter:off
		
		// A simple sprite has a strong dependence on the asset, so we need
		// to know for the new asset what is the new sprite type
		
		if (type.equals(AtlasSpriteModel.TYPE_NAME) 
		|| type.equals(SpritesheetSpriteModel.TYPE_NAME)
		|| type.equals(ImageSpriteModel.TYPE_NAME)
		|| type.equals(BitmapTextModel.TYPE_NAME)) {
		
		//@formatter:on

			String newType = type;

			AssetModel asset = textureKey.getAsset();

			if (asset instanceof AtlasAssetModel) {
				newType = AtlasSpriteModel.TYPE_NAME;
			} else if (asset instanceof ImageAssetModel) {
				newType = ImageSpriteModel.TYPE_NAME;
			} else if (asset instanceof SpritesheetAssetModel) {
				newType = SpritesheetSpriteModel.TYPE_NAME;
			} else if (asset instanceof BitmapFontAssetModel) {
				newType = BitmapTextModel.TYPE_NAME;
			}

			data.put("type", newType);
		}

		data.put("asset-ref", AssetPackCore.getAssetJSONReference(textureKey));
	}

	public static BaseObjectModel createModel(GroupModel parent, JSONObject data) {
		try {
			BaseObjectModel model = null;
			String type = data.getString("type");
			switch (type) {
			case ImageSpriteModel.TYPE_NAME:
				model = new ImageSpriteModel(parent, data);
				break;
			case SpritesheetSpriteModel.TYPE_NAME:
				model = new SpritesheetSpriteModel(parent, data);
				break;
			case AtlasSpriteModel.TYPE_NAME:
				model = new AtlasSpriteModel(parent, data);
				break;
			case TileSpriteModel.TYPE_NAME:
				model = new TileSpriteModel(parent, data);
				break;
			case ButtonSpriteModel.TYPE_NAME:
				model = new ButtonSpriteModel(parent, data);
				break;
			case TextModel.TYPE_NAME:
				model = new TextModel(parent, data);
				break;
			case BitmapTextModel.TYPE_NAME:
				model = new BitmapTextModel(parent, data);
				break;
			case GroupModel.TYPE_NAME:
				model = new GroupModel(parent, data);
				break;
			case Prefab.TYPE_NAME:
				Prefab prefab;

				if (data.has("prefab")) {
					String tableId = data.optString("prefab");
					prefab = parent.getWorld().getPrefabTable().lookup(tableId);

					if (!prefab.getFile().exists()) {
						// use a data with no prefab table reference for
						// missing-nodes
						JSONObject data2 = new JSONObject(data.toString());

						data2.remove("prefab");
						data2.put("prefabFile", prefab.getFile().getProjectRelativePath().toPortableString());

						throw new MissingPrefabException(data2);
					}
				} else {
					String filePath = data.getString("prefabFile");
					IProject project = parent.getWorld().getFile().getProject();
					IFile file = project.getFile(filePath);

					if (!file.exists()) {
						throw new MissingPrefabException(data);
					}

					prefab = new Prefab(file);
				}

				JSONObject jsonInfo = data.optJSONObject("info");
				JSONObject newData = prefab.newInstance(jsonInfo);
				// TODO: this is a temporal solution to keep the asset, but the
				// real solution is to keep the texture in the info and make it
				// easy to change.

				boolean overrideTexture = false;

			{
				JSONArray array = jsonInfo.optJSONArray("prefabOverride");
				for (int i = 0; array != null && i < array.length(); i++) {
					if (array.getString(i).equals("texture")) {
						overrideTexture = true;
						break;
					}
				}
			}

				if (overrideTexture && data.has("asset-ref")) {
					JSONObject currentAssetRef = data.getJSONObject("asset-ref");
					JSONObject newAssetRef = newData.getJSONObject("asset-ref");
					if (!newAssetRef.toString().equals(currentAssetRef.toString())) {
						Object asset = AssetPackCore.findAssetElement(parent.getWorld().getProject(), currentAssetRef);
						if (asset != null) {
							changeTextureToObjectData(newData, (IAssetKey) asset);
						}
					}
				}
				model = createModel(parent, newData);
				model.setId(data.getString("id"));
				model.setPrefab(prefab);
				break;
			default:
				break;
			}
			return model;
		} catch (MissingAssetException e) {
			return new MissingAssetSpriteModel(parent, data);
		}
	}

	public static BaseObjectModel createModel(GroupModel parent, Prefab prefab) {
		JSONObject data = prefab.newInstance();
		BaseObjectModel model = CanvasModelFactory.createModel(parent, data);
		model.setPrefab(prefab);
		return model;
	}
}
