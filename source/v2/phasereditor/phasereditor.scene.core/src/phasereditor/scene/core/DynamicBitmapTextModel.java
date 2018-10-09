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
public class DynamicBitmapTextModel extends BitmapTextModel implements

		DynamicBitmapTextComponent

{

	@SuppressWarnings("hiding")
	public static final String TYPE = "DynamicBitmapText";

	public DynamicBitmapTextModel() {
		super(TYPE);

		DynamicBitmapTextComponent.init(this);
	}

	@Override
	public void write(JSONObject data) {
		super.write(data);

		data.put(displayCallback_name, DynamicBitmapTextComponent.get_displayCallback(this), displayCallback_default);

		data.put(cropWidth_name, DynamicBitmapTextComponent.get_cropWidth(this), cropWidth_default);
		data.put(cropHeight_name, DynamicBitmapTextComponent.get_cropHeight(this), cropHeight_default);

		data.put(scrollX_name, DynamicBitmapTextComponent.get_scrollX(this), scrollX_default);
		data.put(scrollY_name, DynamicBitmapTextComponent.get_scrollY(this), scrollY_default);
	}

	@Override
	public void read(JSONObject data, IProject project) {
		super.read(data, project);

		DynamicBitmapTextComponent.set_displayCallback(this,
				data.optString(displayCallback_name, displayCallback_default));

		DynamicBitmapTextComponent.set_cropWidth(this, data.optInt(cropWidth_name, cropWidth_default));
		DynamicBitmapTextComponent.set_cropHeight(this, data.optInt(cropHeight_name, cropHeight_default));

		DynamicBitmapTextComponent.set_scrollX(this, data.optFloat(scrollX_name, scrollX_default));
		DynamicBitmapTextComponent.set_scrollY(this, data.optFloat(scrollY_name, scrollY_default));
	}

	@Override
	public boolean allowMorphTo(String type) {
		return BitmapTextModel.TYPE.equals(type);
	}

}
