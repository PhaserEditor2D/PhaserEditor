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
package phasereditor.canvas.ui.handlers;

import java.util.function.BiFunction;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.ButtonSpriteModel;
import phasereditor.canvas.core.CanvasModelFactory;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.TileSpriteModel;
import phasereditor.canvas.ui.editors.AddSpriteDialog;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.behaviors.CreateBehavior;

/**
 * @author arian
 *
 */
public class AddSpriteHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);

		AddSpriteDialog dlg = new AddSpriteDialog(HandlerUtil.getActiveShell(event));
		dlg.setProject(editor.getEditorInputFile().getProject());

		ObjectCanvas canvas = editor.getCanvas();
		CreateBehavior create = canvas.getCreateBehavior();
		BiFunction<GroupModel, IAssetKey, BaseSpriteModel> factory = null;

		int id = dlg.open();
		IStructuredSelection result = dlg.getSelection();

		switch (id) {
		case AddSpriteDialog.ADD_SPRITE:
			factory = CanvasModelFactory::createModel;
			break;
		case AddSpriteDialog.ADD_BUTTON:
			factory = (group, key) -> {
				return new ButtonSpriteModel(group, getAssetFrame(key));
			};
			break;
		case AddSpriteDialog.ADD_TILE:
			factory = (group, key) -> {
				return new TileSpriteModel(group, getAssetFrame(key));
			};
			break;
		default:
			break;
		}

		if (factory != null) {
			create.dropAssets(result, factory);
		}

		return null;
	}

	private static IAssetFrameModel getAssetFrame(IAssetKey key) {
		IAssetFrameModel frame;

		if (key instanceof IAssetFrameModel) {
			frame = (IAssetFrameModel) key;
		} else if (key instanceof ImageAssetModel) {
			frame = ((ImageAssetModel) key).getFrame();
		} else {
			throw new RuntimeException("Cannot get a frame with " + key.getKey());
		}
		return frame;
	}

}
