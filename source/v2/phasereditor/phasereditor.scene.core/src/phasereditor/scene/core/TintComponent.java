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
public interface TintComponent {
	// isTinted

	static String isTinted_name = "isTinted";

	static boolean isTinted_default = false;

	static boolean get_isTinted(ObjectModel obj) {
		return (boolean) obj.get("isTinted");
	}

	static void set_isTinted(ObjectModel obj, boolean isTinted) {
		obj.put("isTinted", isTinted);
	}

	// tintBottomLeft

	static String tintBottomLeft_name = "tintBottomLeft";

	static int tintBottomLeft_default = 0xffffff;

	static int get_tintBottomLeft(ObjectModel obj) {
		return (int) obj.get("tintBottomLeft");
	}

	static void set_tintBottomLeft(ObjectModel obj, int tintBottomLeft) {
		obj.put("tintBottomLeft", tintBottomLeft);
	}

	// tintBottomRight

	static String tintBottomRight_name = "tintBottomRight";

	static int tintBottomRight_default = 0xffffff;

	static int get_tintBottomRight(ObjectModel obj) {
		return (int) obj.get("tintBottomRight");
	}

	static void set_tintBottomRight(ObjectModel obj, int tintBottomRight) {
		obj.put("tintBottomRight", tintBottomRight);
	}

	// tintTopLeft

	static String tintTopLeft_name = "tintTopLeft";

	static int tintTopLeft_default = 0xffffff;

	static int get_tintTopLeft(ObjectModel obj) {
		return (int) obj.get("tintTopLeft");
	}

	static void set_tintTopLeft(ObjectModel obj, int tintTopLeft) {
		obj.put("tintTopLeft", tintTopLeft);
	}

	// tintTopRight

	static String tintTopRight_name = "tintTopRight";

	static int tintTopRight_default = 0xffffff;

	static int get_tintTopRight(ObjectModel obj) {
		return (int) obj.get("tintTopRight");
	}

	static void set_tintTopRight(ObjectModel obj, int tintTopRight) {
		obj.put("tintTopRight", tintTopRight);
	}

	// tintFill

	static String tintFill_name = "tintFill";

	static boolean tintFill_default = false;

	static boolean get_tintFill(ObjectModel obj) {
		return (boolean) obj.get("tintFill");
	}

	static void set_tintFill(ObjectModel obj, boolean tintFill) {
		obj.put("tintFill", tintFill);
	}

	// utils

	static void init(ObjectModel obj) {
		set_isTinted(obj, isTinted_default);
		set_tintBottomLeft(obj, tintBottomLeft_default);
		set_tintBottomRight(obj, tintBottomRight_default);
		set_tintTopLeft(obj, tintTopLeft_default);
		set_tintTopRight(obj, tintTopRight_default);
		set_tintFill(obj, tintFill_default);
	}
}
