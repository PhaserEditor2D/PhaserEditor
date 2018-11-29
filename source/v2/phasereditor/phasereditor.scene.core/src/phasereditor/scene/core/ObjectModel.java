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
public abstract class ObjectModel {

	private Map<String, Object> _map;
	private String _id;
	private String _type;

	public ObjectModel(String type) {
		_type = type;
		_id = UUID.randomUUID().toString();

		_map = new HashMap<>();
	}

	public String getId() {
		return _id;
	}

	public void setId(String id) {
		_id = id;
	}

	public String getType() {
		return _type;
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
	}

	@SuppressWarnings("unused")
	public void read(JSONObject data, IProject project) {
		_id = data.getString("-id");
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

	@SuppressWarnings({ "static-method", "unused" })
	public boolean allowMorphTo(String type) {
		return false;
	}
}
