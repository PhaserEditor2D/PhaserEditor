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
public interface DynamicBitmapTextComponent {

	// displayCallback

	static String displayCallback_name = "displayCallback";

	static String displayCallback_default = null;

	static String get_displayCallback(ObjectModel obj) {
		return (String) obj.get("displayCallback");
	}

	static void set_displayCallback(ObjectModel obj, String displayCallback) {
		obj.put("displayCallback", displayCallback);
	}


	// crop
	static String cropWidth_name = "cropWidth";
	static String cropHeight_name = "cropHeight";

	static int cropWidth_default = 0;
	static int cropHeight_default = 0;

	static int get_cropWidth(ObjectModel obj) {
		return (int) obj.get("cropWidth");
	}

	static void set_cropWidth(ObjectModel obj, int cropWidth) {
		obj.put("cropWidth", cropWidth);
	}

	static int get_cropHeight(ObjectModel obj) {
		return (int) obj.get("cropHeight");
	}

	static void set_cropHeight(ObjectModel obj, int cropHeight) {
		obj.put("cropHeight", cropHeight);
	}
	
	// scroll
	
	static String scrollX_name = "scrollX";
	static String scrollY_name = "scrollY";

	static float scrollX_default = 0;
	static float scrollY_default = 0;

	static float get_scrollX(ObjectModel obj) {
		return (float) obj.get("scrollX");
	}

	static void set_scrollX(ObjectModel obj, float scrollX) {
		obj.put("scrollX", scrollX);
	}

	static float get_scrollY(ObjectModel obj) {
		return (float) obj.get("scrollY");
	}

	static void set_scrollY(ObjectModel obj, float scrollY) {
		obj.put("scrollY", scrollY);
	}

	// init

	static void init(ObjectModel model) {
		set_displayCallback(model, displayCallback_default);

		set_cropWidth(model, cropWidth_default);
		set_cropHeight(model, cropHeight_default);

		set_scrollX(model, scrollX_default);
		set_scrollY(model, scrollY_default);
	}

}
