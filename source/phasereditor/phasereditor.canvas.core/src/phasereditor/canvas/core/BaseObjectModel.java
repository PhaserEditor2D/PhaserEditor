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
 * Common features to groups and shapes.
 * 
 * @author arian
 *
 */
public abstract class BaseObjectModel {
	private GroupModel _parent;
	private String _typeName;
	private String _editorName;
	private String _editorFactory;
	private boolean _editorGenerate;
	private boolean _editorPick;

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
		_parent = parent;
		_typeName = typeName;

		_editorName = typeName;
		_editorPick = true;
		_editorFactory = null;
		_editorGenerate = true;

		_scaleX = 1;
		_scaleY = 1;
		_rotation = 0;
		_pivotX = 0;
		_pivotY = 0;
	}

	public static IAssetKey findAsset(JSONObject jsonModel) {
		JSONObject assetRef = jsonModel.getJSONObject("asset-ref");
		IAssetKey key = (IAssetKey) AssetPackCore.findAssetElement(assetRef);
		return key;
	}

	@SuppressWarnings("unused")
	protected void readMetadata(JSONObject obj) {
		// nothing
	}
	
	public abstract void rebuild();

	public String getLabel() {
		return _editorName + "[" + _typeName + "]";
	}

	public GroupModel getParent() {
		return _parent;
	}

	public String getEditorName() {
		return _editorName;
	}

	public void setEditorName(String editorName) {
		_editorName = editorName;
	}

	public String getEditorFactory() {
		return _editorFactory;
	}

	public void setEditorFactory(String editorFactory) {
		_editorFactory = editorFactory;
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

	public void setLocation(double x, double y) {
		setX(x);
		setY(y);
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

	protected void read(JSONObject obj) {
		readMetadata(obj);
		JSONObject jsonInfo = obj.getJSONObject("info");
		readInfo(jsonInfo);
	}

	protected void readInfo(JSONObject jsonInfo) {
		_editorName = jsonInfo.optString("editorName");
		_editorFactory = jsonInfo.optString("editorFactory");
		_editorGenerate = jsonInfo.optBoolean("editorGenerate", true);
		_editorPick = jsonInfo.optBoolean("editorPick", true);

		_x = jsonInfo.optDouble("x", 0);
		_y = jsonInfo.optDouble("y", 0);
		_rotation = jsonInfo.optDouble("rotation", 0);
		_scaleX = jsonInfo.optDouble("scale.x", 1);
		_scaleY = jsonInfo.optDouble("scale.y", 1);
		_pivotX = jsonInfo.optDouble("pivot.x", 0);
		_pivotY = jsonInfo.optDouble("pivot.y", 0);
	}

	public final void write(JSONObject obj) {
		writeMetadata(obj);

		JSONObject jsonInfo = new JSONObject();
		obj.put("info", jsonInfo);
		writeInfo(jsonInfo);
	}

	protected void writeMetadata(JSONObject obj) {
		obj.put("type", _typeName);
	}

	protected void writeInfo(JSONObject jsonInfo) {
		jsonInfo.put("editorName", _editorName);
		jsonInfo.put("editorFactory", _editorFactory);
		jsonInfo.put("editorGenerate", _editorGenerate);
		jsonInfo.put("editorPick", _editorPick);

		jsonInfo.put("x", _x);
		jsonInfo.put("y", _y);
		jsonInfo.put("rotation", _rotation);
		jsonInfo.put("scale.x", _scaleX);
		jsonInfo.put("scale.y", _scaleY);
		jsonInfo.put("pivot.x", _pivotX);
		jsonInfo.put("pivot.y", _pivotY);
	}

	public WorldModel getWorld() {
		if (this instanceof WorldModel) {
			return (WorldModel) this;
		}
		return _parent.getWorld();
	}

	public void updateWith(BaseObjectModel model) {
		JSONObject obj = new JSONObject();
		model.writeInfo(obj);
		readInfo(obj);
	}
}
