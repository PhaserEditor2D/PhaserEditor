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
package phasereditor.canvas.ui.handlers;

import java.util.function.Consumer;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.canvas.core.BitmapTextModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.behaviors.CreateBehavior;
import phasereditor.canvas.ui.editors.grid.editors.BitmapTextFontDialog;
import phasereditor.canvas.ui.editors.grid.editors.TextDialog;

/**
 * @author arian
 *
 */
public class AddBitmapTextHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		openTextDialog(userText -> {
			CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
			ObjectCanvas canvas = editor.getCanvas();

			BitmapTextFontDialog dlg = new BitmapTextFontDialog(HandlerUtil.getActiveShell(event));
			dlg.setProject(editor.getEditorInputFile().getProject());

			if (dlg.open() == Window.OK) {
				BitmapFontAssetModel font = dlg.getSelectedFont();
				if (font == null) {
					return;
				}

				CreateBehavior create = canvas.getCreateBehavior();

				create.dropObjects(new StructuredSelection(userText), (group, text) -> {
					BitmapTextModel model = new BitmapTextModel(group, font);
					model.setText((String) text);
					return model;
				});

			}
		});

		return null;
	}

	public static void openTextDialog(Consumer<String> textConsumer) {
		TextDialog dlg = new TextDialog(Display.getCurrent().getActiveShell());

		dlg.setTitle("Add Text");
		dlg.setMessage("Enter the text:");
		dlg.setInitialText("This is a text");
		dlg.setSelectAll(true);

		if (dlg.open() == Window.OK) {
			textConsumer.accept(dlg.getResult());
		}
	}

}
