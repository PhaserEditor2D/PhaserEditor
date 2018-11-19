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
public abstract class EditorObjectModel extends ParentModel implements EditorComponent {

	public EditorObjectModel(String type) {
		super(type);

		EditorComponent.init(this);
	}

	@Override
	public void write(JSONObject data) {

		super.write(data);

		data.put(editorName_name, EditorComponent.get_editorName(this));
		data.put(editorField_name, EditorComponent.get_editorField(this));
		data.put(editorClosed_name, EditorComponent.get_editorClosed(this), editorClosed_default);
		data.put(editorTransparency_name, EditorComponent.get_editorTransparency(this), editorTransparency_default);
	}

	@Override
	public void read(JSONObject data, IProject project) {

		super.read(data, project);

		EditorComponent.set_editorName(this, data.getString(editorName_name));
		EditorComponent.set_editorField(this, data.optBoolean(editorField_name));
		EditorComponent.set_editorClosed(this, data.optBoolean(editorClosed_name, editorClosed_default));
		EditorComponent.set_editorTransparency(this,
				data.optFloat(editorTransparency_name, editorTransparency_default));
	}

}
