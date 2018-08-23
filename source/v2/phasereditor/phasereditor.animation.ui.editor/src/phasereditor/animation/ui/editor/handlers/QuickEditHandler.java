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
package phasereditor.animation.ui.editor.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.animation.ui.editor.AnimationsEditor;
import phasereditor.animation.ui.editor.properties.AnimationsPGridPage;
import phasereditor.ui.properties.PGrid;
import phasereditor.ui.properties.QuickEditDialog;

/**
 * @author arian
 *
 */
public class QuickEditHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		var sel = HandlerUtil.getCurrentStructuredSelection(event);

		if (sel.isEmpty()) {
			return null;
		}

		var editor = (AnimationsEditor) HandlerUtil.getActiveEditor(event);
		var model = AnimationsPGridPage.createModelWithSelection_public(sel);

		var dlg = new QuickEditDialog(HandlerUtil.getActiveShell(event)) {
			@Override
			protected PGrid createPGrid(Composite container) {
				var grid = new PGrid(container, SWT.NONE, false, true);

				grid.setOnChanged(() -> gridChanged(editor));

				return grid;
			}
		};
		dlg.setModel(model);

		dlg.open();

		return null;
	}

	static void gridChanged(AnimationsEditor editor) {
		editor.gridPropertyChanged();

		QuickEditDialog.regreshAllPGridPropertyViews();
	}

}
