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
public interface TextComponent extends TextualComponent {
	enum Align {
		left, center, right, justified
	}

	// align

	static String align_name = "align";

	static Align align_default = Align.left;

	static Align get_align(ObjectModel obj) {
		return (Align) obj.get("align");
	}

	static void set_align(ObjectModel obj, Align align) {
		obj.put("align", align);
	}

	// backgroundColor

	static String backgroundColor_name = "backgroundColor";

	static String backgroundColor_default = null;

	static String get_backgroundColor(ObjectModel obj) {
		return (String) obj.get("backgroundColor");
	}

	static void set_backgroundColor(ObjectModel obj, String backgroundColor) {
		obj.put("backgroundColor", backgroundColor);
	}

	// baseline
	static String baselineX_name = "baselineX";
	static String baselineY_name = "baselineY";

	static float baselineX_default = 1.2f;
	static float baselineY_default = 1.4f;

	static float get_baselineX(ObjectModel obj) {
		return (float) obj.get("baselineX");
	}

	static void set_baselineX(ObjectModel obj, float baselineX) {
		obj.put("baselineX", baselineX);
	}

	static float get_baselineY(ObjectModel obj) {
		return (float) obj.get("baselineY");
	}

	static void set_baselineY(ObjectModel obj, float baselineY) {
		obj.put("baselineY", baselineY);
	}

	// color

	static String color_name = "color";

	static String color_default = "#ffffff";

	static String get_color(ObjectModel obj) {
		return (String) obj.get("color");
	}

	static void set_color(ObjectModel obj, String color) {
		obj.put("color", color);
	}
	// fill

	static String fill_name = "fill";

	static String fill_default = null;

	static String get_fill(ObjectModel obj) {
		return (String) obj.get("fill");
	}

	static void set_fill(ObjectModel obj, String fill) {
		obj.put("fill", fill);
	}

	// fixed
	static String fixedWidth_name = "fixedWidth";
	static String fixedHeight_name = "fixedHeight";

	static int fixedWidth_default = 0;
	static int fixedHeight_default = 0;

	static int get_fixedWidth(ObjectModel obj) {
		return (int) obj.get("fixedWidth");
	}

	static void set_fixedWidth(ObjectModel obj, int fixedWidth) {
		obj.put("fixedWidth", fixedWidth);
	}

	static int get_fixedHeight(ObjectModel obj) {
		return (int) obj.get("fixedHeight");
	}

	static void set_fixedHeight(ObjectModel obj, int fixedHeight) {
		obj.put("fixedHeight", fixedHeight);
	}

	// fontFamily

	static String fontFamily_name = "fontFamily";

	static String fontFamily_default = "Courier";

	static String get_fontFamily(ObjectModel obj) {
		return (String) obj.get("fontFamily");
	}

	static void set_fontFamily(ObjectModel obj, String fontFamily) {
		obj.put("fontFamily", fontFamily);
	}

	// fontSize

	static String fontSize_name = "fontSize";

	static String fontSize_default = "16px";

	static String get_fontSize(ObjectModel obj) {
		return (String) obj.get("fontSize");
	}

	static void set_fontSize(ObjectModel obj, String fontSize) {
		obj.put("fontSize", fontSize);
	}

	// fontStyle

	static String fontStyle_name = "fontStyle";

	static String fontStyle_default = null;

	static String get_fontStyle(ObjectModel obj) {
		return (String) obj.get("fontStyle");
	}

	static void set_fontStyle(ObjectModel obj, String fontStyle) {
		obj.put("fontStyle", fontStyle);
	}

	// maxLines

	static String maxLines_name = "maxLines";

	static int maxLines_default = 0;

	static int get_maxLines(ObjectModel obj) {
		return (int) obj.get("maxLines");
	}

	static void set_maxLines(ObjectModel obj, int maxLines) {
		obj.put("maxLines", maxLines);
	}

	// shadowBlur

	static String shadowBlur_name = "shadowBlur";

	static int shadowBlur_default = 0;

	static int get_shadowBlur(ObjectModel obj) {
		return (int) obj.get("shadowBlur");
	}

	static void set_shadowBlur(ObjectModel obj, int shadowBlur) {
		obj.put("shadowBlur", shadowBlur);
	}

	// shadowColor

	static String shadowColor_name = "shadowColor";

	static String shadowColor_default = "#000000";

	static String get_shadowColor(ObjectModel obj) {
		return (String) obj.get("shadowColor");
	}

	static void set_shadowColor(ObjectModel obj, String shadowColor) {
		obj.put("shadowColor", shadowColor);
	}

	// shadowFill

	static String shadowFill_name = "shadowFill";

	static boolean shadowFill_default = false;

	static boolean get_shadowFill(ObjectModel obj) {
		return (boolean) obj.get("shadowFill");
	}

	static void set_shadowFill(ObjectModel obj, boolean shadowFill) {
		obj.put("shadowFill", shadowFill);
	}

	// shadowOffset
	static String shadowOffsetX_name = "shadowOffsetX";
	static String shadowOffsetY_name = "shadowOffsetY";

	static int shadowOffsetX_default = 0;
	static int shadowOffsetY_default = 0;

	static int get_shadowOffsetX(ObjectModel obj) {
		return (int) obj.get("shadowOffsetX");
	}

	static void set_shadowOffsetX(ObjectModel obj, int shadowOffsetX) {
		obj.put("shadowOffsetX", shadowOffsetX);
	}

	static int get_shadowOffsetY(ObjectModel obj) {
		return (int) obj.get("shadowOffsetY");
	}

	static void set_shadowOffsetY(ObjectModel obj, int shadowOffsetY) {
		obj.put("shadowOffsetY", shadowOffsetY);
	}

	// shadowStroke

	static String shadowStroke_name = "shadowStroke";

	static boolean shadowStroke_default = false;

	static boolean get_shadowStroke(ObjectModel obj) {
		return (boolean) obj.get("shadowStroke");
	}

	static void set_shadowStroke(ObjectModel obj, boolean shadowStroke) {
		obj.put("shadowStroke", shadowStroke);
	}

	// stroke

	static String stroke_name = "stroke";

	static String stroke_default = "#ffffff";

	static String get_stroke(ObjectModel obj) {
		return (String) obj.get("stroke");
	}

	static void set_stroke(ObjectModel obj, String stroke) {
		obj.put("stroke", stroke);
	}

	// strokeThickness

	static String strokeThickness_name = "strokeThickness";

	static float strokeThickness_default = 0;

	static float get_strokeThickness(ObjectModel obj) {
		return (float) obj.get("strokeThickness");
	}

	static void set_strokeThickness(ObjectModel obj, float strokeThickness) {
		obj.put("strokeThickness", strokeThickness);
	}

	// utils

	static void init(ObjectModel model) {
		set_align(model, align_default);
		set_backgroundColor(model, backgroundColor_default);
		set_baselineX(model, baselineX_default);
		set_baselineY(model, baselineY_default);
		set_color(model, backgroundColor_default);
		set_fill(model, fill_default);
		set_fixedHeight(model, fixedHeight_default);
		set_fixedWidth(model, fixedWidth_default);
		set_fontFamily(model, fontFamily_default);
		set_fontSize(model, fontSize_default);
		set_fontStyle(model, fontStyle_default);
		set_maxLines(model, maxLines_default);
		set_shadowBlur(model, shadowBlur_default);
		set_shadowColor(model, shadowColor_default);
		set_shadowFill(model, shadowFill_default);
		set_shadowOffsetX(model, shadowOffsetX_default);
		set_shadowOffsetY(model, shadowOffsetY_default);
		set_shadowStroke(model, shadowStroke_default);
		set_stroke(model, stroke_default);
		set_strokeThickness(model, strokeThickness_default);
		// 20
	}
}
