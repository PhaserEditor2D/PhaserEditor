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
public interface DisplayComponent {
	// display
	static String displayWidth_name = "displayWidth";
	static String displayHeight_name = "displayHeight";

	static float displayWidth_default = 0;
	static float displayHeight_default = 0;

	static float get_displayWidth(ObjectModel obj) {
		return (float) obj.get("displayWidth");
	}

	static void set_displayWidth(ObjectModel obj, float displayWidth) {
		obj.put("displayWidth", displayWidth);
	}

	static float get_displayHeight(ObjectModel obj) {
		return (float) obj.get("displayHeight");
	}

	static void set_displayHeight(ObjectModel obj, float displayHeight) {
		obj.put("displayHeight", displayHeight);
	}
	
	
	// utils
	
	static void init(ObjectModel model) {
		set_displayWidth(model, displayWidth_default);
		set_displayHeight(model, displayHeight_default);
	}
}
