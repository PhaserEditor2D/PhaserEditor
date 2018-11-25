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
package phasereditor.scene.ui;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import phasereditor.scene.core.BitmapTextModel;
import phasereditor.scene.core.DynamicBitmapTextModel;
import phasereditor.scene.core.ImageModel;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.SpriteModel;
import phasereditor.scene.core.TileSpriteModel;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.undo.WorldSnapshotOperation;

/**
 * @author arian
 *
 */
public class SceneUI {

	public static void action_MorphObjectsToNewType(SceneEditor editor, List<?> models, String morphToType) {
		var before = WorldSnapshotOperation.takeSnapshot(editor);

		var newModels = new ArrayList<ObjectModel>();
		var project = editor.getEditorInput().getFile().getProject();

		for (var obj : models) {
			if (!(obj instanceof ObjectModel)) {
				continue;
			}

			var model = (ObjectModel) obj;

			if (model.getType().equals(morphToType)) {
				continue;
			}

			var data = new JSONObject();
			model.write(data);

			ObjectModel newModel = null;

			switch (morphToType) {
			case SpriteModel.TYPE:
				newModel = new SpriteModel();
				newModel.read(data, project);
				break;
			case ImageModel.TYPE:
				newModel = new ImageModel();
				newModel.read(data, project);
				break;
			case TileSpriteModel.TYPE:
				var tileModel = new TileSpriteModel();
				tileModel.read(data, project);
				tileModel.setSizeToFrame(editor.getScene().getAssetFinder());
				newModel = tileModel;
				break;
			case BitmapTextModel.TYPE:
				newModel = new BitmapTextModel();
				newModel.read(data, project);
				break;
			case DynamicBitmapTextModel.TYPE:
				newModel = new DynamicBitmapTextModel();
				newModel.read(data, project);
				break;

			default:
				break;
			}

			if (newModel != null) {

				var parent = ParentComponent.get_parent(model);
				var siblings = ParentComponent.get_children(parent);
				var index = siblings.indexOf(model);

				ParentComponent.removeFromParent(model);
				ParentComponent.addChild(parent, index, newModel);

				newModels.add(newModel);

			}

		}

		if (!newModels.isEmpty()) {

			editor.refreshOutline_basedOnId();

			editor.setSelection(newModels);

			// we do this because the Properties window is active (not the editor)
			editor.updatePropertyPagesContentWithSelection();

			editor.setDirty(true);

			var after = WorldSnapshotOperation.takeSnapshot(editor);
			editor.executeOperation(new WorldSnapshotOperation(before, after, "Morph to " + morphToType));
		}
	}
	
}
