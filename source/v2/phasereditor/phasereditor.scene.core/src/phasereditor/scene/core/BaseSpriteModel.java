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
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.IAssetKey;

/**
 * @author arian
 *
 */
public abstract class BaseSpriteModel extends EditorObjectModel implements FlipComponent,

		OriginComponent,

		TextureComponent,

		TransformComponent,

		VisibleComponent

{

	protected BaseSpriteModel(String type) {
		super(type);

		FlipComponent.init(this);

		OriginComponent.init(this);

		TextureComponent.init(this);

		TransformComponent.init(this);

		VisibleComponent.init(this);

	}

	@Override
	public void write(JSONObject data) {

		super.write(data);

		data.put(flipX_name, FlipComponent.get_flipX(this), flipX_default);
		data.put(flipY_name, FlipComponent.get_flipY(this), flipY_default);

		data.put(originX_name, OriginComponent.get_originX(this), OriginComponent.originX_default(this));
		data.put(originY_name, OriginComponent.get_originY(this), OriginComponent.originY_default(this));

		data.put(x_name, TransformComponent.get_x(this), x_default);
		data.put(y_name, TransformComponent.get_y(this), y_default);
		data.put(scaleX_name, TransformComponent.get_scaleX(this), scaleX_default);
		data.put(scaleY_name, TransformComponent.get_scaleY(this), scaleY_default);
		data.put(angle_name, TransformComponent.get_angle(this), angle_default);

		data.put(visible_name, VisibleComponent.get_visible(this), visible_default);

		{
			IAssetKey assetKey = TextureComponent.get_frame(this);
			if (assetKey == null) {
				data.put(frame_name, (Object) null);
			} else {
				var ref = AssetPackCore.getAssetJSONReference(assetKey);
				data.put(frame_name, ref);
			}
		}
	}

	@Override
	public void read(JSONObject data, IProject project) {

		super.read(data, project);

		FlipComponent.set_flipX(this, data.optBoolean(flipX_name, flipX_default));
		FlipComponent.set_flipY(this, data.optBoolean(flipY_name, flipY_default));

		OriginComponent.set_originX(this, (float) data.optDouble(originX_name, OriginComponent.originX_default(this)));
		OriginComponent.set_originY(this, (float) data.optDouble(originY_name, OriginComponent.originY_default(this)));

		TransformComponent.set_x(this, (float) data.optDouble(x_name, x_default));
		TransformComponent.set_y(this, (float) data.optDouble(y_name, y_default));
		TransformComponent.set_scaleX(this, (float) data.optDouble(scaleX_name, scaleX_default));
		TransformComponent.set_scaleY(this, (float) data.optDouble(scaleY_name, scaleY_default));
		TransformComponent.set_angle(this, (float) data.optDouble(angle_name, angle_default));

		VisibleComponent.set_visible(this, data.optBoolean(visible_name, visible_default));

		{
			IAssetFrameModel frame = null;
			var ref = data.optJSONObject(frame_name);
			if (ref != null) {
				var result = AssetPackCore.findAssetElement(project, ref);
				if (result != null && result instanceof IAssetFrameModel) {
					frame = (IAssetFrameModel) result;
				}
			}
			TextureComponent.set_frame(this, frame);
		}

	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public boolean allowMorphTo(String type) {

		if (getType().equals(type)) {
			return false;
		}

		switch (type) {
		case SpriteModel.TYPE:
		case TileSpriteModel.TYPE:
		case ImageModel.TYPE:
			return true;
		}

		return false;
	}
}
