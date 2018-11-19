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
public interface OriginComponent {

	// origin
	static String originX_name = "originX";
	static String originY_name = "originY";

	static float originX_default(ObjectModel obj) {

		if (obj instanceof BitmapTextModel) {
			return 0;
		}

		return .5f;
	}

	static float originY_default(ObjectModel obj) {
		if (obj instanceof BitmapTextModel) {
			return 0;
		}

		return .5f;
	}

	static float get_originX(ObjectModel obj) {
		return (float) obj.get("originX");
	}

	static void set_originX(ObjectModel obj, float originX) {
		obj.put("originX", originX);
	}

	static float get_originY(ObjectModel obj) {
		return (float) obj.get("originY");
	}

	static void set_originY(ObjectModel obj, float originY) {
		obj.put("originY", originY);
	}

	static boolean is(Object model) {
		return model instanceof OriginComponent;
	}
	
	static void init(ObjectModel obj) {
		set_originX(obj, originX_default(obj));
		set_originY(obj, originY_default(obj));
	}

}
