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
package phasereditor.scene.core;

import org.eclipse.core.resources.IProject;
import org.json.JSONObject;

/**
 * @author arian
 *
 */
public abstract class EditorObjectModel extends ParentModel implements GameObjectEditorComponent {

	public EditorObjectModel(String type) {
		super(type);

		GameObjectEditorComponent.init(this);
	}

	@Override
	public void write(JSONObject data) {

		super.write(data);

		data.put(gameObjectEditorName_name, GameObjectEditorComponent.get_gameObjectEditorName(this));
		data.put(gameObjectEditorField_name, GameObjectEditorComponent.get_gameObjectEditorField(this));
		data.put(gameObjectEditorClosed_name, GameObjectEditorComponent.get_gameObjectEditorClosed(this), editorClosed_default);
		data.put(gameObjectEditorTransparency_name, GameObjectEditorComponent.get_gameObjectEditorTransparency(this), gameObjectEditorTransparency_default);
	}

	@Override
	public void read(JSONObject data, IProject project) {

		super.read(data, project);

		GameObjectEditorComponent.set_gameObjectEditorName(this, data.getString(gameObjectEditorName_name));
		GameObjectEditorComponent.set_editorField(this, data.optBoolean(gameObjectEditorField_name));
		GameObjectEditorComponent.set_gameObjectEditorClosed(this, data.optBoolean(gameObjectEditorClosed_name, editorClosed_default));
		GameObjectEditorComponent.set_gameObjectEditorTransparency(this,
				data.optFloat(gameObjectEditorTransparency_name, gameObjectEditorTransparency_default));
	}

}
