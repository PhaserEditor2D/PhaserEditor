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

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.AssetsContentProvider;
import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasFile;
import phasereditor.canvas.core.CanvasModelFactory;
import phasereditor.canvas.ui.CanvasUI;
import phasereditor.canvas.ui.editors.AddSpriteDialog;
import phasereditor.canvas.ui.editors.CanvasEditor;

/**
 * @author arian
 *
 */
public class AddPrefabHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);

		AddSpriteDialog dlg = new AddSpriteDialog(HandlerUtil.getActiveShell(event), "Add Prefab");
		dlg.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((CanvasFile) element).getFile().getName();
			}

			@Override
			public Image getImage(Object element) {
				return CanvasUI.getCanvasFileIcon((CanvasFile) element, AssetLabelProvider.GLOBAL_48);
			}
		});
		dlg.setContentProvider(new AssetsContentProvider() {
			@Override
			public Object[] getChildren(Object parentElement) {

				if (parentElement instanceof IProject) {
					List<CanvasFile> list = CanvasCore.getCanvasFileCache().getProjectData((IProject) parentElement);
					return list.stream().filter(cfile -> cfile.getType().isPrefab()).toArray();
				}

				return super.getChildren(parentElement);
			}
		});
		dlg.setProject(editor.getEditorInputFile().getProject());

		if (dlg.open() != Window.OK) {
			return null;
		}

		IStructuredSelection result = dlg.getSelection();

		editor.getCanvas().getCreateBehavior().dropObjects(result, CanvasModelFactory::createModel);

		return null;
	}

}
