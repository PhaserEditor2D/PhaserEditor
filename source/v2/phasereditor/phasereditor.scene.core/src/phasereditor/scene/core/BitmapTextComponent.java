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

import phasereditor.assetpack.core.BitmapFontAssetModel;

/**
 * @author arian
 *
 */
@SuppressWarnings("boxing")
public interface BitmapTextComponent {
	// font

	static String font_name = "font";

	static BitmapFontAssetModel font_default = null;

	static BitmapFontAssetModel get_font(ObjectModel obj) {
		return (BitmapFontAssetModel) obj.get("font");
	}

	static void set_font(ObjectModel obj, BitmapFontAssetModel font) {
		obj.put("font", font);
	}

	// fontSize

	static String fontSize_name = "fontSize";

	static int fontSize_default = 0;

	static int get_fontSize(ObjectModel obj) {
		return (int) obj.get("fontSize");
	}

	static void set_fontSize(ObjectModel obj, int fontSize) {
		obj.put("fontSize", fontSize);
	}

	// align

	static final int ALIGN_LEFT = 0;
	static final int ALIGN_MIDDLE = 1;
	static final int ALIGN_RIGHT = 2;

	static String align_name = "align";

	static int align_default = ALIGN_LEFT;

	static int get_align(ObjectModel obj) {
		return (int) obj.get("align");
	}

	static void set_align(ObjectModel obj, int align) {
		obj.put("align", align);
	}
	
	// letterSpacing

	static String letterSpacing_name = "letterSpacing";

	static float letterSpacing_default = 0;

	static float get_letterSpacing(ObjectModel obj) {
		return (float) obj.get("letterSpacing");
	}

	static void set_letterSpacing(ObjectModel obj, float letterSpacing) {
		obj.put("letterSpacing", letterSpacing);
	}
	
	static boolean is(Object model) {
		return model instanceof BitmapTextComponent;
	}

	static void init(BitmapTextModel obj) {
		set_font(obj, font_default);
		set_fontSize(obj, fontSize_default);
		set_align(obj, align_default);
		set_letterSpacing(obj, letterSpacing_default);
	}

}
