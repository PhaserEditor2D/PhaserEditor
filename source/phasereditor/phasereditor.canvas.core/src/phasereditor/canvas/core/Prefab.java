// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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

import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.UUID;

import org.eclipse.core.resources.IFile;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * @author arian
 *
 */
public class Prefab extends CanvasFile{

	public static final String TYPE_NAME = "prefab";

	public Prefab(IFile file, CanvasType type) {
		super(file, type);
	}

	public Prefab(IFile file) {
		super(file, null);
	}

	/**
	 * The same as {@link #newInstance(JSONObject)} but it takes no instance
	 * info.
	 * 
	 * @return
	 */
	public JSONObject newInstance() {
		return newInstance(null);
	}

	/**
	 * Make an instance of this prefab.
	 *
	 * @param initInfo
	 *            Some info we want to overwrite.
	 * @return
	 */
	public JSONObject newInstance(JSONObject initInfo) {
		IFile file = getFile();
		CanvasModel model = new CanvasModel(file);
		try (InputStream contents = file.getContents()) {
			JSONObject data = new JSONObject(new JSONTokener(contents));

			model.read(data);
			BaseObjectModel objModel;
			if (model.getType() == CanvasType.SPRITE) {
				objModel = model.getWorld().findFirstSprite();
				if (objModel == null) {
					throw new InvalidParameterException("The prefab has an invalid state.");
				}
			} else {
				// get the prefab root
				objModel = model.getWorld().findGroupPrefabRoot();
			}

			if (initInfo != null) {
				applyInfo(objModel, initInfo);
			}

			JSONObject newData = new JSONObject();
			objModel.setId(UUID.randomUUID().toString());
			objModel.setEditorName(model.getSettings().getClassName());
			objModel.write(newData, false);

			return newData;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private static void applyInfo(BaseObjectModel objModel, JSONObject info) {
		JSONObject prefabData = new JSONObject();
		objModel.write(prefabData, false);
		JSONObject prefabInfo = prefabData.getJSONObject("info");
		for (String k : info.keySet()) {
			prefabInfo.put(k, info.get(k));
		}
		objModel.readInfo(prefabInfo);
	}
}
