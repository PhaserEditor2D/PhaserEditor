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
package phasereditor.canvas.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.json.JSONObject;

import phasereditor.project.core.PhaserProjectBuilder;

/**
 * @author arian
 *
 */
public class CanvasModelValidation {
	private WorldModel _world;

	public CanvasModelValidation(WorldModel world) {
		_world = world;
	}

	public List<IStatus> validate() {
		List<IStatus> problems = new ArrayList<>();

		Set<String> used = new HashSet<>();

		_world.walk(model -> {
			if (model instanceof MissingAssetSpriteModel) {
				MissingAssetSpriteModel missing = (MissingAssetSpriteModel) model;

				// add the assets not found
				String msg = getMissingRefMessage(missing.getSrcData());
				if (!used.contains(msg)) {
					problems.add(0, new Status(IStatus.ERROR, CanvasCore.PLUGIN_ID, "Asset not found: " + msg));
					used.add(msg);
				}

				// add the sprite missing asset error
				Status error = new Status(IStatus.ERROR, CanvasCore.PLUGIN_ID,
						"The asset for the sprite '" + missing.getEditorName() + "' is not found.");
				PhaserProjectBuilder.createErrorMarker(error, _world.getFile());
			}
		});

		return problems;
	}

	private static String getMissingRefMessage(JSONObject srcData) {
		JSONObject ref = srcData.getJSONObject("asset-ref");
		String msg = "section=" + ref.optString("section") + ", key=" + ref.optString("asset");

		if (ref.has("frame")) {
			msg += ", frame=" + ref.optString("sprite", "");
		}
		return msg;
	}
}
