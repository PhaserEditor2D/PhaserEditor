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

import org.json.JSONObject;

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public interface FlipComponent {

	// flip

	static String flipX_name = "flipX";
	static String flipY_name = "flipY";

	static boolean flipX_default = false;
	static boolean flipY_default = false;

	static boolean get_flipX(ObjectModel obj) {
		return (boolean) obj.get("flipX");
	}

	static void set_flipX(ObjectModel obj, boolean flipX) {
		obj.put("flipX", flipX);
	}

	static boolean get_flipY(ObjectModel obj) {
		return (boolean) obj.get("flipY");
	}

	static void set_flipY(ObjectModel obj, boolean flipY) {
		obj.put("flipY", flipY);
	}

	static boolean is(Object model) {
		return model instanceof FlipComponent;
	}

	static void init(ObjectModel obj) {
		set_flipX(obj, flipX_default);
		set_flipY(obj, flipY_default);
	}

	static void utils_write(ObjectModel model, JSONObject data) {
		data.put(flipX_name, FlipComponent.get_flipX(model), flipX_default);
		data.put(flipY_name, FlipComponent.get_flipY(model), flipY_default);
	}

	static void utils_read(ObjectModel model, JSONObject data) {
		FlipComponent.set_flipX(model, data.optBoolean(flipX_name, flipX_default));
		FlipComponent.set_flipY(model, data.optBoolean(flipY_name, flipY_default));
	}
}
