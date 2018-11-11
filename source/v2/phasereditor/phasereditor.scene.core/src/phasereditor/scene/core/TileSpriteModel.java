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
public class TileSpriteModel extends BaseSpriteModel implements

		TileSpriteComponent

{

	public static final String TYPE = "TileSprite";

	public TileSpriteModel() {
		super(TYPE);

		TileSpriteComponent.init(this);
	}

	@Override
	public void read(JSONObject data, IProject project) {

		super.read(data, project);

		TileSpriteComponent.set_tilePositionX(this, data.optFloat(tilePositionX_name, tilePositionX_default));
		TileSpriteComponent.set_tilePositionY(this, data.optFloat(tilePositionY_name, tilePositionY_default));

		TileSpriteComponent.set_tileScaleX(this, data.optFloat(tileScaleX_name, tileScaleX_default));
		TileSpriteComponent.set_tileScaleY(this, data.optFloat(tileScaleY_name, tileScaleY_default));

		TileSpriteComponent.set_width(this, data.optFloat(width_name, width_default));
		TileSpriteComponent.set_height(this, data.optFloat(height_name, height_default));
	}

	@Override
	public void write(JSONObject data) {

		super.write(data);

		data.put(tilePositionX_name, TileSpriteComponent.get_tilePositionX(this), tilePositionX_default);
		data.put(tilePositionY_name, TileSpriteComponent.get_tilePositionY(this), tilePositionY_default);

		data.put(tileScaleX_name, TileSpriteComponent.get_tileScaleX(this), tileScaleX_default);
		data.put(tileScaleY_name, TileSpriteComponent.get_tileScaleY(this), tileScaleY_default);

		data.put(width_name, TileSpriteComponent.get_width(this), width_default);
		data.put(height_name, TileSpriteComponent.get_height(this), height_default);
	}

	public void setSizeToFrame() {
		var frame = TextureComponent.get_frame(this);

		if (frame == null) {

			TileSpriteComponent.set_width(this, -1);
			TileSpriteComponent.set_height(this, -1);

		} else {

			var size = frame.getFrameData().src;

			TileSpriteComponent.set_width(this, size.width);
			TileSpriteComponent.set_height(this, size.height);
		}
	}

	@Override
	public boolean allowMorphTo(String type) {

		if (SpriteModel.TYPE.equals(type)) {
			return true;
		}

		return false;
	}

}
