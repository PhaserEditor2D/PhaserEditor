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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.eclipse.core.resources.IProject;
import org.json.JSONObject;

/**
 * @author arian
 *
 */
public abstract class ObjectModel implements EditorComponent {

	private Map<String, Object> _map;
	private String _id;
	private String _type;

	public ObjectModel(String type) {
		_type = type;
		_id = UUID.randomUUID().toString();

		_map = new HashMap<>();

		EditorComponent.init(this);
	}

	public String getId() {
		return _id;
	}

	public void put(String key, Object value) {
		_map.put(key, value);
	}

	public Object get(String key) {
		return _map.get(key);
	}

	public void write(JSONObject data) {
		data.put("-id", _id);
		data.put("-type", _type);

		data.put(editorName_name, EditorComponent.get_editorName(this));
		data.put(editorClosed_name, EditorComponent.get_editorClosed(this), editorClosed_default);
	}

	@SuppressWarnings("unused")
	public void read(JSONObject data, IProject project) {
		_id = data.getString("-id");
		_type = data.getString("-type");

		EditorComponent.set_editorName(this, data.getString(editorName_name));
		EditorComponent.set_editorClosed(this, data.optBoolean(editorClosed_name, editorClosed_default));

	}

	public void visit(Consumer<ObjectModel> visitor) {
		visitor.accept(this);

		if (this instanceof ParentComponent) {

			var children = ParentComponent.get_children(this);

			for (var child : children) {
				child.visit(visitor);
			}
		}
	}

}
