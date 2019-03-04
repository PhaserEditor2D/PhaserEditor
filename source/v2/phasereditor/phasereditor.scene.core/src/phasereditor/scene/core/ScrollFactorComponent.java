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

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public interface ScrollFactorComponent {
	// scrollFactor
	static String scrollFactorX_name = "scrollFactorX";
	static String scrollFactorY_name = "scrollFactorY";

	static float scrollFactorX_default = 1;
	static float scrollFactorY_default = 1;

	static float get_scrollFactorX(ObjectModel obj) {
		return (float) obj.get("scrollFactorX");
	}

	static void set_scrollFactorX(ObjectModel obj, float scrollFactorX) {
		obj.put("scrollFactorX", scrollFactorX);
	}

	static float get_scrollFactorY(ObjectModel obj) {
		return (float) obj.get("scrollFactorY");
	}

	static void set_scrollFactorY(ObjectModel obj, float scrollFactorY) {
		obj.put("scrollFactorY", scrollFactorY);
	}

	// utils

	static boolean is(ObjectModel model) {
		return model instanceof ScrollFactorComponent;
	}

	static void init(ObjectModel model) {
		set_scrollFactorX(model, scrollFactorX_default);
		set_scrollFactorY(model, scrollFactorY_default);
	}
}
