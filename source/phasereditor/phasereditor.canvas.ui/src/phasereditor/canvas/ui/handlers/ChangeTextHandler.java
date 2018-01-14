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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.BitmapTextModel;
import phasereditor.canvas.core.TextModel;
import phasereditor.canvas.ui.editors.grid.PGridStringProperty;
import phasereditor.canvas.ui.editors.grid.editors.PGridEditingSupport;
import phasereditor.canvas.ui.shapes.BitmapTextControl;
import phasereditor.canvas.ui.shapes.BitmapTextNode;
import phasereditor.canvas.ui.shapes.TextControl;
import phasereditor.canvas.ui.shapes.TextNode;

/**
 * @author arian
 *
 */
public class ChangeTextHandler extends AbstractHandler{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShell(event);
		Object[] selection = HandlerUtil.getCurrentStructuredSelection(event).toArray();

		for (Object obj : selection) {
			if (obj instanceof TextNode) {
				changeText(shell, (TextNode) obj);
				return null;
			} else if (obj instanceof BitmapTextNode) {
				changeBitmapText(shell, (BitmapTextNode) obj);
				return null;
			}
		}

		
		return null;
	}
	
	private static void changeBitmapText(Shell shell, BitmapTextNode text) {
		if (text.getModel().isOverriding(BitmapTextModel.PROPSET_TEXT)) {
			BitmapTextControl control = (BitmapTextControl) text.getControl();
			PGridStringProperty prop = control.getTextProperty();
			String result = PGridEditingSupport.openLongStringDialog(prop, shell);
			if (result != null) {
				PGridEditingSupport.changeUndoablePropertyValue(result, prop);
			}
		} else {
			MessageDialog.openInformation(shell, "Change Text", "The 'text' property is read-only.");
		}
	}

	private static void changeText(Shell shell, TextNode text) {

		if (text.getModel().isOverriding(TextModel.PROPSET_TEXT)) {
			TextControl control = (TextControl) text.getControl();
			PGridStringProperty prop = control.getTextProperty();
			String result = PGridEditingSupport.openLongStringDialog(prop, shell);
			if (result != null) {
				PGridEditingSupport.changeUndoablePropertyValue(result, prop);
			}
		} else {
			MessageDialog.openInformation(shell, "Change Text", "The 'text' property is read-only.");
		}
	}

}
