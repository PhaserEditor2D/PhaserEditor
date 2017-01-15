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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.json.JSONObject;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.IAssetKey;

/**
 * Common features to groups and shapes.
 * 
 * @author arian
 *
 */
public abstract class BaseObjectModel {
	private static final boolean DEF_EDITOR_PICK = true;
	private static final double DEF_X = 0;
	private static final double DEF_Y = 0;
	private static final double DEF_ROTATION = 0;
	private static final double DEF_SCALE_X = 1;
	private static final double DEF_SCALE_Y = 1;
	private static final double DEF_PIVOT_X = 0;
	private static final double DEF_PIVOT_Y = 0;
	private static final boolean DEF_EDITOR_GENERATE = true;
	public static final boolean DEF_EDITOR_PUBLIC = false;
	private GroupModel _parent;
	private String _typeName;
	private String _editorName;
	private boolean _editorPick;
	private boolean _editorGenerate;
	private boolean _editorPublic;
	private boolean _editorShow;
	private Prefab _prefab;

	private String _id;
	private double _x;
	private double _y;
	private double _rotation;
	private double _scaleX;
	private double _scaleY;
	private double _pivotX;
	private double _pivotY;

	public BaseObjectModel(GroupModel parent, String typeName, JSONObject obj) {
		this(parent, typeName);
		read(obj);
	}

	public BaseObjectModel(GroupModel parent, String typeName) {
		_id = UUID.randomUUID().toString();

		_parent = parent;
		_typeName = typeName;

		_editorName = typeName;
		_editorPick = DEF_EDITOR_PICK;
		_editorGenerate = DEF_EDITOR_GENERATE;
		_editorPublic = DEF_EDITOR_PUBLIC;
		_editorShow = true;

		_scaleX = 1;
		_scaleY = 1;
		_rotation = 0;
		_pivotX = 0;
		_pivotY = 0;
	}

	public String getId() {
		return _id;
	}

	public void setId(String id) {
		_id = id;
	}

	public Prefab getPrefab() {
		return _prefab;
	}

	public void setPrefab(Prefab prefabReference) {
		_prefab = prefabReference;
	}

	public boolean isPrefabInstance() {
		return _prefab != null;
	}

	public boolean isPrefabInstanceComponent() {
		return isPrefabInstance() || getParent() != null && getParent().isPrefabInstance();
	}

	public IAssetKey findAsset(JSONObject jsonModel) {

		if (jsonModel.has("asset")) {
			// read the compact mode
			String id = jsonModel.getString("asset");
			IAssetKey key = getWorld().getAssetTable().lookup(id);
			return key;
		}

		// read in the expanded mode

		JSONObject assetRef = jsonModel.getJSONObject("asset-ref");
		IAssetKey key = (IAssetKey) AssetPackCore.findAssetElement(getWorld().getProject(), assetRef);

		return key;
	}

	protected void readMetadata(JSONObject obj) {
		_id = obj.getString("id");
	}

	/**
	 * This method is used by the Canvas Build Participant.
	 */
	public abstract void build();

	public String getLabel() {
		return _editorName + "[" + _typeName + "]";
	}

	public GroupModel getParent() {
		return _parent;
	}

	public int getIndex() {
		if (getParent() == null) {
			return 0;
		}

		return getParent().getChildren().indexOf(this);
	}

	public int getDepth() {
		return getParent() == null ? 0 : getParent().getDepth() + 1;
	}

	public String getEditorName() {
		return _editorName;
	}

	public void setEditorName(String editorName) {
		_editorName = editorName;
	}

	public boolean isEditorPublic() {
		return _editorPublic;
	}

	public void setEditorPublic(boolean editorPublic) {
		_editorPublic = editorPublic;
	}

	public boolean isEditorPick() {
		return _editorPick;
	}

	public void setEditorPick(boolean editorPick) {
		_editorPick = editorPick;
	}

	public boolean isEditorGenerate() {
		return _editorGenerate;
	}

	public void setEditorGenerate(boolean editorGenerate) {
		_editorGenerate = editorGenerate;
	}

	public boolean isEditorShow() {
		return _editorShow;
	}

	public void setEditorShow(boolean editorShow) {
		_editorShow = editorShow;
	}

	public final String getTypeName() {
		return _typeName;
	}

	public double getX() {
		return _x;
	}

	public void setX(double x) {
		_x = x;
	}

	public double getY() {
		return _y;
	}

	public void setY(double y) {
		_y = y;
	}

	public double getRotation() {
		return _rotation;
	}

	public void setRotation(double rotation) {
		_rotation = rotation;
	}

	public double getAngle() {
		return _rotation * 180 / Math.PI;
	}

	public void setAngle(double angle) {
		_rotation = angle * Math.PI / 180;
	}

	public double getScaleX() {
		return _scaleX;
	}

	public void setScaleX(double scaleX) {
		_scaleX = scaleX;
	}

	public double getScaleY() {
		return _scaleY;
	}

	public void setScaleY(double scaleY) {
		_scaleY = scaleY;
	}

	public double getPivotX() {
		return _pivotX;
	}

	public void setPivotX(double pivotX) {
		_pivotX = pivotX;
	}

	public double getPivotY() {
		return _pivotY;
	}

	public void setPivotY(double pivotY) {
		_pivotY = pivotY;
	}

	public void read(JSONObject obj) {
		readMetadata(obj);
		JSONObject jsonInfo = obj.getJSONObject("info");
		readInfo(jsonInfo);
	}

	protected void readInfo(JSONObject jsonInfo) {
		_editorName = jsonInfo.optString("editorName");
		_editorPick = jsonInfo.optBoolean("editorPick", DEF_EDITOR_PICK);
		_editorGenerate = jsonInfo.optBoolean("editorGenerate", DEF_EDITOR_GENERATE);
		_editorPublic = jsonInfo.optBoolean("editorPublic", DEF_EDITOR_PUBLIC);
		_editorShow = jsonInfo.optBoolean("editorShow", true);

		_x = jsonInfo.optDouble("x", DEF_X);
		_y = jsonInfo.optDouble("y", DEF_Y);
		_rotation = jsonInfo.optDouble("rotation", DEF_ROTATION);
		_scaleX = jsonInfo.optDouble("scale.x", DEF_SCALE_X);
		_scaleY = jsonInfo.optDouble("scale.y", DEF_SCALE_Y);
		_pivotX = jsonInfo.optDouble("pivot.x", DEF_PIVOT_X);
		_pivotY = jsonInfo.optDouble("pivot.y", DEF_PIVOT_Y);
	}

	public BaseObjectModel copy(boolean keepId) {
		JSONObject obj = new JSONObject();

		write(obj, false);

		BaseObjectModel copy = CanvasModelFactory.createModel(_parent, obj);
		if (!keepId) {
			copy.resetId();
		}
		return copy;
	}

	public JSONObject toJSON(boolean saving) {
		JSONObject obj = new JSONObject();
		write(obj, saving);
		return obj;
	}

	public void resetId() {
		_id = UUID.randomUUID().toString();
	}

	public void write(JSONObject obj, boolean saving) {
		if (isPrefabInstance()) {
			writePrefabMetadata(obj);
		} else {
			writeMetadata(obj, saving);
		}

		JSONObject jsonInfo = new JSONObject();
		obj.put("info", jsonInfo);
		writeInfo(jsonInfo, saving);

		if (saving && isPrefabInstance()) {
			JSONObject prefabData = getPrefab().newInstance();
			JSONObject prefabJsonInfo = prefabData.getJSONObject("info");
			removeDefaultKeys(jsonInfo, prefabJsonInfo, "editorName", "x", "y");
		}
	}

	private static void removeDefaultKeys(JSONObject obj, JSONObject defaultObj, String... skipKeys) {
		List<String> removeKeys = new ArrayList<>();

		Set<String> testKeys = new HashSet<>(obj.keySet());
		testKeys.removeAll(Arrays.asList(skipKeys));

		for (String k : testKeys) {
			Object value1 = obj.get(k);
			if (value1 == null) {
				value1 = "<null>";
			} else {
				value1 = value1.toString();
			}

			Object value2 = defaultObj.opt(k);
			if (value2 == null) {
				value2 = "<null>";
			} else {
				value2 = value2.toString();
			}

			if (value1.equals(value2)) {
				removeKeys.add(k);
			}
		}
		for (String k : removeKeys) {
			obj.remove(k);
		}
	}

	@SuppressWarnings("unused")
	protected void writeMetadata(JSONObject obj, boolean saving) {
		obj.put("type", _typeName);
		obj.put("id", _id);
	}

	protected void writePrefabMetadata(JSONObject obj) {
		obj.put("type", "prefab");
		String filePath = _prefab.getFile().getProjectRelativePath().toPortableString();
		obj.put("prefabFile", filePath);
		obj.put("id", _id);
	}

	@SuppressWarnings("unused")
	protected void writeInfo(JSONObject jsonInfo, boolean saving) {
		jsonInfo.put("editorName", _editorName);
		jsonInfo.put("editorPick", _editorPick, DEF_EDITOR_PICK);
		jsonInfo.put("editorGenerate", _editorGenerate, DEF_EDITOR_GENERATE);
		jsonInfo.put("editorPublic", _editorPublic, DEF_EDITOR_PUBLIC);
		jsonInfo.put("editorShow", _editorShow, true);

		jsonInfo.put("x", _x, DEF_X);
		jsonInfo.put("y", _y, DEF_Y);
		jsonInfo.put("rotation", _rotation, DEF_ROTATION);
		jsonInfo.put("scale.x", _scaleX, DEF_SCALE_X);
		jsonInfo.put("scale.y", _scaleY, DEF_SCALE_Y);
		jsonInfo.put("pivot.x", _pivotX, DEF_PIVOT_X);
		jsonInfo.put("pivot.y", _pivotY, DEF_PIVOT_Y);
	}

	public WorldModel getWorld() {
		if (this instanceof WorldModel) {
			return (WorldModel) this;
		}
		return _parent.getWorld();
	}

	public void updateWith(BaseObjectModel model) {
		JSONObject obj = new JSONObject();
		model.writeInfo(obj, true);
		readInfo(obj);
	}

	@SuppressWarnings("static-method")
	public boolean hasErrors() {
		return false;
	}
}
