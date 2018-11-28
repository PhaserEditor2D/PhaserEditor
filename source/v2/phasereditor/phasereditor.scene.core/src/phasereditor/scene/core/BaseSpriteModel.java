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

/**
 * @author arian
 *
 */
public abstract class BaseSpriteModel extends TransformModel implements FlipComponent,

		OriginComponent,

		TextureComponent,

		VisibleComponent

{

	protected BaseSpriteModel(String type) {
		super(type);

		FlipComponent.init(this);

		OriginComponent.init(this);

		TextureComponent.init(this);

		VisibleComponent.init(this);

	}

	@Override
	public void write(JSONObject data) {

		super.write(data);

		data.put(flipX_name, FlipComponent.get_flipX(this), flipX_default);
		data.put(flipY_name, FlipComponent.get_flipY(this), flipY_default);

		data.put(originX_name, OriginComponent.get_originX(this), OriginComponent.originX_default(this));
		data.put(originY_name, OriginComponent.get_originY(this), OriginComponent.originY_default(this));

		data.put(visible_name, VisibleComponent.get_visible(this), visible_default);

		data.put(textureKey_name, TextureComponent.get_textureKey(this), textureKey_default);
		data.put(textureFrame_name, TextureComponent.get_textureFrame(this), textureFrame_default);
	}

	@Override
	public void read(JSONObject data, IProject project) {

		super.read(data, project);

		FlipComponent.set_flipX(this, data.optBoolean(flipX_name, flipX_default));
		FlipComponent.set_flipY(this, data.optBoolean(flipY_name, flipY_default));

		OriginComponent.set_originX(this, (float) data.optDouble(originX_name, OriginComponent.originX_default(this)));
		OriginComponent.set_originY(this, (float) data.optDouble(originY_name, OriginComponent.originY_default(this)));

		VisibleComponent.set_visible(this, data.optBoolean(visible_name, visible_default));

		TextureComponent.set_textureKey(this, data.optString(textureKey_name, textureKey_default));
		TextureComponent.set_textureFrame(this, data.optString(textureFrame_name, textureFrame_default));

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
