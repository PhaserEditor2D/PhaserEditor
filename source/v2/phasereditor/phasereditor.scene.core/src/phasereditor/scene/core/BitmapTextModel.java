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

import org.eclipse.core.resources.IProject;
import org.json.JSONObject;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.bmpfont.core.BitmapFontModel;

/**
 * @author arian
 *
 */
public class BitmapTextModel extends ParentModel implements

		OriginComponent,

		TransformComponent,

		VisibleComponent,

		TextualComponent,

		BitmapTextComponent

{

	public static final String TYPE = "BitmapText";

	public BitmapTextModel() {
		this(TYPE);
	}

	protected BitmapTextModel(String type) {
		super(type);

		OriginComponent.init(this);

		// by default bitmap text has 0,0 origin
		OriginComponent.set_originX(this, 0);
		OriginComponent.set_originY(this, 0);

		TransformComponent.init(this);
		VisibleComponent.init(this);
		TextualComponent.init(this);
		BitmapTextComponent.init(this);
	}

	@Override
	public void write(JSONObject data) {
		super.write(data);

		data.put(originX_name, OriginComponent.get_originX(this), OriginComponent.originX_default(this));
		data.put(originY_name, OriginComponent.get_originY(this), OriginComponent.originY_default(this));

		data.put(x_name, TransformComponent.get_x(this), x_default);
		data.put(y_name, TransformComponent.get_y(this), y_default);
		data.put(scaleX_name, TransformComponent.get_scaleX(this), scaleX_default);
		data.put(scaleY_name, TransformComponent.get_scaleY(this), scaleY_default);
		data.put(angle_name, TransformComponent.get_angle(this), angle_default);

		data.put(visible_name, VisibleComponent.get_visible(this), visible_default);

		data.put(text_name, TextualComponent.get_text(this), text_default);

		{
			var fontModel = BitmapTextComponent.get_font(this);

			if (fontModel == null) {
				data.put(font_name, (Object) null);
			} else {
				var ref = AssetPackCore.getAssetJSONReference(fontModel);
				data.put(font_name, ref);
			}
		}

		data.put(fontSize_name, BitmapTextComponent.get_fontSize(this), fontSize_default);
		data.put(align_name, BitmapTextComponent.get_align(this), align_default);
		data.put(letterSpacing_name, BitmapTextComponent.get_letterSpacing(this), letterSpacing_default);
	}

	@Override
	public void read(JSONObject data, IProject project) {
		super.read(data, project);

		// origin default at 0,0
		OriginComponent.set_originX(this, (float) data.optDouble(originX_name, 0));
		OriginComponent.set_originY(this, (float) data.optDouble(originY_name, 0));

		TransformComponent.set_x(this, (float) data.optDouble(x_name, x_default));
		TransformComponent.set_y(this, (float) data.optDouble(y_name, y_default));
		TransformComponent.set_scaleX(this, (float) data.optDouble(scaleX_name, scaleX_default));
		TransformComponent.set_scaleY(this, (float) data.optDouble(scaleY_name, scaleY_default));
		TransformComponent.set_angle(this, (float) data.optDouble(angle_name, angle_default));

		VisibleComponent.set_visible(this, data.optBoolean(visible_name, visible_default));

		TextualComponent.set_text(this, data.optString(text_name, text_default));

		BitmapTextComponent.set_align(this, data.optInt(align_name, align_default));
		BitmapTextComponent.set_fontSize(this, data.optInt(fontSize_name, fontSize_default));
		BitmapTextComponent.set_letterSpacing(this, data.optFloat(letterSpacing_name, letterSpacing_default));

		{
			BitmapFontAssetModel fontAsset = null;
			var ref = data.optJSONObject(font_name);

			if (ref != null) {
				var result = AssetPackCore.findAssetElement(project, ref);
				if (result != null && result instanceof BitmapFontAssetModel) {
					fontAsset = (BitmapFontAssetModel) result;
				}
			}

			BitmapTextComponent.set_font(this, fontAsset);
		}

		updateSizeFromBitmapFont();

	}

	public void updateSizeFromBitmapFont() {
		var model = createFontModel();
		if (model != null) {
			if (BitmapTextComponent.get_fontSize(this) == fontSize_default) {
				BitmapTextComponent.set_fontSize(this, model.getInfoSize());
			}
		}
	}

	public BitmapFontModel createFontModel() {
		var fontAsset = BitmapTextComponent.get_font(this);

		if (fontAsset != null) {
			var model = fontAsset.createFontModel();
			return model;
		}

		return null;
	}

	@Override
	public boolean allowMorphTo(String type) {
		return DynamicBitmapTextModel.TYPE.equals(type);
	}

}
