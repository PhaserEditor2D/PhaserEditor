// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.canvas.ui;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorPart;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.assetpack.core.FindAssetReferencesResult;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.IAssetReference;
import phasereditor.assetpack.core.IAssetReplacer;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.CanvasCore.AssetInCanvasReference;
import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.ui.PhaserEditorUI;

/**
 * @author arian
 *
 */
public class CanvasAssetReplacer implements IAssetReplacer {

	@Override
	public void replace_SWTThread(FindAssetReferencesResult result, IAssetKey key, IProgressMonitor monitor) {

		for (IFile file : result.getFiles()) {
			List<IEditorPart> editors = PhaserEditorUI.findOpenFileEditors(file);
			for (IEditorPart editor : editors) {
				if (editor instanceof CanvasEditor) {
					ObjectCanvas canvas = ((CanvasEditor) editor).getCanvas();
					GroupNode world = canvas.getWorldNode();

					CompositeOperation operations = new CompositeOperation();

					for (IAssetReference ref : result.getReferencesOf(file)) {
						if (ref instanceof AssetInCanvasReference) {
							String objectId = ((AssetInCanvasReference) ref).getObjectId();
							BaseObjectControl<?> control = world.getControl().findById(objectId);
							if (control != null) {
								CanvasUI.changeSpriteTexture(control.getIObjectNode(), key, operations);
							}
						}
					}

					if (!operations.isEmpty()) {
						canvas.getUpdateBehavior().executeOperations(operations);
					}
				}
			}
		}
	}

	@Override
	public void replace_ResourceThread(FindAssetReferencesResult result, IAssetKey key, IProgressMonitor monitor)
			throws Exception {
		for (IFile file : result.getFiles()) {
			CanvasModel canvasModel = new CanvasModel(file);

			try (InputStream contents = file.getContents()) {
				canvasModel.read(new JSONObject(new JSONTokener(contents)));
			}

			boolean changed = false;

			for (IAssetReference ref : result.getReferencesOf(file)) {
				if (ref instanceof AssetInCanvasReference) {
					String objectId = ((AssetInCanvasReference) ref).getObjectId();
					BaseObjectModel objModel = canvasModel.getWorld().findById(objectId);
					if (objModel != null) {
						CanvasUI.changeSpriteTexture(objModel, key);
						changed = true;
					}
				}
			}

			if (changed) {
				JSONObject data = new JSONObject();
				canvasModel.write(data, true);
				file.setContents(new ByteArrayInputStream(data.toString(2).getBytes()), false, false, monitor);
			}
		}
	}
}
