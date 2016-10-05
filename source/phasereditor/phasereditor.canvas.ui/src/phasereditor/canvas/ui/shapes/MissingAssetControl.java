// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.ui.shapes;

import org.json.JSONObject;

import javafx.scene.control.Label;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.CanvasModelFactory;
import phasereditor.canvas.core.MissingAssetSpriteModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;

/**
 * @author arian
 *
 */
public class MissingAssetControl extends BaseObjectControl<MissingAssetSpriteModel> {

	public MissingAssetControl(ObjectCanvas canvas, MissingAssetSpriteModel model) {
		super(canvas, model);
	}

	@Override
	protected IObjectNode createNode() {
		return new MissingAssetNode(this);
	}

	@Override
	public double getTextureWidth() {
		return ((Label) getNode()).getWidth();
	}

	@Override
	public double getTextureHeight() {
		return ((Label) getNode()).getHeight();
	}

	@Override
	public boolean rebuild() {
		JSONObject data = getModel().getSrcData();
		JSONObject ref = data.getJSONObject("asset-ref");
		Object asset = AssetPackCore.findAssetElement(getModel().getWorld().getProject(), ref);
		// TODO: missing to check if the key has the right type.
		// maybe the user is trying to recover an atlas frame with a spritesheet
		// frame.
		if (asset != null && asset instanceof IAssetKey) {
			int i = getModel().getIndex();

			JSONObject srcData = getModel().getSrcData();
			BaseObjectModel newModel = CanvasModelFactory.createModel(getModel().getParent(), srcData);
			BaseObjectControl<?> newControl = CanvasObjectFactory.createObjectControl(getCanvas(), newModel);
			GroupControl parentControl = getGroup().getControl();

			removeme();
			parentControl.addChild(i, newControl.getIObjectNode());

			return true;
		}

		return false;
	}
}
