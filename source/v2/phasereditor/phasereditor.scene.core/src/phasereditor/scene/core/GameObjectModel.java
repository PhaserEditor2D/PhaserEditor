// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
public abstract class GameObjectModel extends EditorObjectModel implements

		GameObjectComponent,

		VisibleComponent

{

	public GameObjectModel(String type) {
		super(type);

		GameObjectComponent.init(this);

		VisibleComponent.init(this);
	}

	@Override
	public void read(JSONObject data, IProject project) {
		super.read(data, project);

		GameObjectComponent.set_active(this, data.optBoolean(active_name, active_default));
		GameObjectComponent.set_data(this, data.optJSONObject(data_name));
		
		VisibleComponent.set_visible(this, data.optBoolean(visible_name, visible_default));
		
	}

	@Override
	public void write(JSONObject data) {
		super.write(data);

		data.put(active_name, GameObjectComponent.get_active(this), active_default);
		data.put(data_name, GameObjectComponent.get_data(this));

		data.put(visible_name, VisibleComponent.get_visible(this), visible_default);
	}

}
