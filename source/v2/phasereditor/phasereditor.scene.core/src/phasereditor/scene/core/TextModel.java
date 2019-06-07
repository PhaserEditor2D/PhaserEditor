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
import static phasereditor.scene.core.TextComponent.*;

/**
 * @author arian
 *
 */
public class TextModel extends TintedModel implements

		TextComponent

{

	public static final String TYPE = "Text";

	public TextModel() {
		super(TYPE);
		TextComponent.init(this);
	}

	@Override
	public void write(JSONObject data) {
		super.write(data);

		data.put(align_name, get_align(this), align_default);
		data.put(backgroundColor_name, get_backgroundColor(this), backgroundColor_default);
		data.put(baselineX_name, get_baselineX(this), baselineX_default);
		data.put(baselineY_name, get_baselineY(this), baselineY_default);
		data.put(color_name, get_color(this), color_default);
		data.put(fill_name, get_fill(this), fill_default);
		data.put(fixedWidth_name, get_fixedWidth(this), fixedWidth_default);
		data.put(fixedHeight_name, get_fixedHeight(this), fixedHeight_default);
		data.put(fontFamily_name, get_fontFamily(this), fontFamily_default);
		data.put(fontSize_name, get_fontSize(this), fontSize_default);
		data.put(fontStyle_name, get_fontStyle(this), fontStyle_default);
		data.put(maxLines_name, get_maxLines(this), maxLines_default);
		data.put(shadowBlur_name, get_shadowBlur(this), shadowBlur_default);
		data.put(shadowColor_name, get_shadowColor(this), shadowColor_default);
		data.put(shadowFill_name, get_shadowFill(this), shadowFill_default);
		data.put(shadowOffsetX_name, get_shadowOffsetX(this), shadowOffsetX_default);
		data.put(shadowOffsetY_name, get_shadowOffsetY(this), shadowOffsetY_default);
		data.put(shadowStroke_name, get_shadowStroke(this), shadowStroke_default);
		data.put(stroke_name, get_stroke(this), stroke_default);
		data.put(strokeThickness_name, get_strokeThickness(this), strokeThickness_default);

	}

	@Override
	public void read(JSONObject data, IProject project) {
		super.read(data, project);

		set_align(this, Align.valueOf(data.optString(align_name, align_default.name())));
		set_backgroundColor(this, data.optString(backgroundColor_name, backgroundColor_default));
		set_baselineX(this, data.optFloat(baselineX_name, baselineX_default));
		set_baselineY(this, data.optFloat(baselineY_name, baselineY_default));
		set_color(this, data.optString(color_name, color_default));
		set_fill(this, data.optString(fill_name, fill_default));
		set_fixedHeight(this, data.optInt(fixedHeight_name, fixedHeight_default));
		set_fixedWidth(this, data.optInt(fixedWidth_name, fixedWidth_default));
		set_fontFamily(this, data.optString(fontFamily_name, fontFamily_default));
		set_fontSize(this, data.optString(fontSize_name, fontSize_default));
		set_fontStyle(this, data.optString(fontStyle_name, fontStyle_default));
		set_maxLines(this, data.optInt(maxLines_name, maxLines_default));
		set_shadowBlur(this, data.optInt(shadowBlur_name, shadowBlur_default));
		set_shadowColor(this, data.optString(shadowColor_name, shadowColor_default));
		set_shadowFill(this, data.optBoolean(shadowFill_name, shadowFill_default));
		set_shadowOffsetX(this, data.optInt(shadowOffsetX_name, shadowOffsetX_default));
		set_shadowOffsetY(this, data.optInt(shadowOffsetY_name, shadowOffsetY_default));
		set_shadowStroke(this, data.optBoolean(shadowStroke_name, shadowStroke_default));
		set_stroke(this, data.optString(stroke_name, stroke_default));
		set_strokeThickness(this, data.optFloat(strokeThickness_name, strokeThickness_default));
	}

}
