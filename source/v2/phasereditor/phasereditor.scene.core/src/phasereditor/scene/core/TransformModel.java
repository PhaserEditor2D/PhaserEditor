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
public abstract class TransformModel extends EditorObjectModel implements

		TransformComponent

{

	public TransformModel(String type) {
		super(type);

		TransformComponent.init(this);
	}

	@Override
	public void write(JSONObject data) {

		super.write(data);

		data.put(x_name, TransformComponent.get_x(this), x_default);
		data.put(y_name, TransformComponent.get_y(this), y_default);
		data.put(scaleX_name, TransformComponent.get_scaleX(this), scaleX_default);
		data.put(scaleY_name, TransformComponent.get_scaleY(this), scaleY_default);
		data.put(angle_name, TransformComponent.get_angle(this), angle_default);

	}

	@Override
	public void read(JSONObject data, IProject project) {

		super.read(data, project);

		TransformComponent.set_x(this, (float) data.optDouble(x_name, x_default));
		TransformComponent.set_y(this, (float) data.optDouble(y_name, y_default));
		TransformComponent.set_scaleX(this, (float) data.optDouble(scaleX_name, scaleX_default));
		TransformComponent.set_scaleY(this, (float) data.optDouble(scaleY_name, scaleY_default));
		TransformComponent.set_angle(this, (float) data.optDouble(angle_name, angle_default));

	}

}
