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

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public interface TransformComponent {

	// x
	static String x_name = "x";
	static float x_default = 0f;

	static float get_x(ObjectModel obj) {
		return (float) obj.get("x");
	}

	static void set_x(ObjectModel obj, float x) {
		obj.put("x", x);
	}

	// y

	static String y_name = "y";

	static float y_default = 0f;

	static float get_y(ObjectModel obj) {
		return (float) obj.get("y");
	}

	static void set_y(ObjectModel obj, float y) {
		obj.put("y", y);
	}

	// scale

	static String scaleX_name = "scaleX";
	static String scaleY_name = "scaleY";

	static float scaleX_default = 1f;
	static float scaleY_default = 1f;

	static float get_scaleX(ObjectModel obj) {
		return (float) obj.get("scaleX");
	}

	static void set_scaleX(ObjectModel obj, float scaleX) {
		obj.put("scaleX", scaleX);
	}

	static float get_scaleY(ObjectModel obj) {
		return (float) obj.get("scaleY");
	}

	static void set_scaleY(ObjectModel obj, float scaleY) {
		obj.put("scaleY", scaleY);
	}

	// angle

	static String angle_name = "angle";

	static float angle_default = 0f;

	static float get_angle(ObjectModel obj) {
		return (float) obj.get("angle");
	}

	static void set_angle(ObjectModel obj, float angle) {
		obj.put("angle", angle);
	}

	static boolean is(Object model) {
		return model instanceof TransformComponent;
	}
	
	static void init(ObjectModel obj) {
		set_angle(obj, angle_default);
		set_scaleX(obj, scaleX_default);
		set_scaleY(obj, scaleY_default);
		set_x(obj, x_default);
		set_y(obj, y_default);
	}
}
