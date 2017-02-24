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

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorPart;

import phasereditor.assetpack.core.FindAssetReferencesResult;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.IAssetReference;
import phasereditor.assetpack.core.IAssetReplacer;
import phasereditor.canvas.ui.CanvasUI.AssetInCanvasEditorReference;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.BaseSpriteControl;
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
					CompositeOperation operations = new CompositeOperation();

					for (IAssetReference ref : result.getReferencesOf(file)) {
						if (ref instanceof AssetInCanvasEditorReference) {
							BaseSpriteControl<?> control = ((AssetInCanvasEditorReference) ref).getControl();
							CanvasUI.changeSpriteTexture(control.getIObjectNode(), key, operations);
						}
					}

					canvas.getUpdateBehavior().executeOperations(operations);
				}
			}
		}
	}

	@Override
	public void replace_ResourceThread(FindAssetReferencesResult result, IAssetKey key, IProgressMonitor monitor) {
		// TODO: missing
	}
}
