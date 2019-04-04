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
package phasereditor.scene.ui.editor.messages;

import org.json.JSONObject;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.scene.core.PackReferencesCollector;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.SceneUIEditor;
import phasereditor.webrun.core.ApiMessage;
import phasereditor.webrun.ui.WebRunUI;

/**
 * @author arian
 *
 */
public class RefreshAllMessage extends ApiMessage {

	public RefreshAllMessage(SceneEditor editor) {

		var model = editor.getSceneModel();
		var project = editor.getProject();
		var finder = AssetPackCore.getAssetFinder(project);

		{
			var displayListData = new JSONObject();
			model.getDisplayList().write(displayListData);

			_data.put("method", "RefreshAll");
			_data.put("displayList", displayListData);
		}

		{
			var projectUrl = WebRunUI.getProjectBrowserURL(project);

			_data.put("projectUrl", projectUrl);
			var collector = new PackReferencesCollector(model, finder);

			try {
				var newPack = collector.collectNewPack(asset -> {
					return

					asset instanceof ImageAssetModel

							|| asset instanceof SpritesheetAssetModel

							|| asset instanceof AtlasAssetModel

							|| asset instanceof MultiAtlasAssetModel

							|| asset instanceof BitmapFontAssetModel;

				});

				_data.put("pack", newPack);

			} catch (Exception e) {
				SceneUIEditor.logError(e);
			}
		}
	}
}
