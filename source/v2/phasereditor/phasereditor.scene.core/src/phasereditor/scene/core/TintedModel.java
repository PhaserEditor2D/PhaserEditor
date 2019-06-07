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
import static phasereditor.scene.core.TintComponent.*;

/**
 * @author arian
 *
 */
public class TintedModel extends TransformModel implements TintComponent {

	public TintedModel(String type) {
		super(type);

		init(this);
	}

	@Override
	public void read(JSONObject data, IProject project) {
		super.read(data, project);

		set_isTinted(this, data.optBoolean(isTinted_name, isTinted_default));
		set_tintFill(this, data.optBoolean(tintFill_name, tintFill_default));
		set_tintTopLeft(this, data.optInt(tintTopLeft_name, tintTopLeft_default));
		set_tintTopRight(this, data.optInt(tintTopRight_name, tintTopRight_default));
		set_tintBottomLeft(this, data.optInt(tintBottomLeft_name, tintBottomLeft_default));
		set_tintBottomRight(this, data.optInt(tintBottomRight_name, tintBottomRight_default));
	}

	@Override
	public void write(JSONObject data) {
		super.write(data);

		data.put(isTinted_name, get_isTinted(this), isTinted_default);
		data.put(tintFill_name, get_tintFill(this), tintFill_default);
		data.put(tintTopLeft_name, get_tintTopLeft(this), tintTopLeft_default);
		data.put(tintTopRight_name, get_tintTopRight(this), tintTopRight_default);
		data.put(tintBottomLeft_name, get_tintBottomLeft(this), tintBottomLeft_default);
		data.put(tintBottomRight_name, get_tintBottomRight(this), tintBottomRight_default);
	}

}
