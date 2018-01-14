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
package phasereditor.canvas.ui.handlers;

import java.util.function.BiFunction;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.ButtonSpriteModel;
import phasereditor.canvas.core.CanvasModelFactory;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.TileSpriteModel;
import phasereditor.canvas.ui.editors.SelectTextureDialog;
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

		ObjectCanvas canvas = editor.getCanvas();
		CreateBehavior create = canvas.getCreateBehavior();
		BiFunction<GroupModel, IAssetKey, BaseObjectModel> factory = null;

		SelectTextureDialog dlg = new SelectTextureDialog(HandlerUtil.getActiveShell(event), "Add Sprite");
		dlg.setProject(editor.getEditorInputFile().getProject());

		if (dlg.open() != Window.OK) {
			return null;
		}

		String id = event.getParameter("phasereditor.canvas.ui.spriteType");

		IStructuredSelection result = dlg.getSelection();

		switch (id) {
		case "sprite":
			factory = CanvasModelFactory::createModel;
			break;
		case "button":
			factory = (group, key) -> {
				return new ButtonSpriteModel(group, getAssetFrame(key));
			};
			break;
		case "tileSprite":
			factory = (group, key) -> {
				return new TileSpriteModel(group, getAssetFrame(key));
			};
			break;
		default:
			break;
		}

		if (factory != null) {
			BiFunction<GroupModel, IAssetKey, BaseObjectModel> factory2 = factory;
			create.dropObjects(result, (group, key) -> {
				return factory2.apply(group, (IAssetKey) key);
			});
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
