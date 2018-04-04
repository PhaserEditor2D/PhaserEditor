// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.text.ui.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;

import phasereditor.text.ui.TextUI;
import phasereditor.ui.PhaserEditorUI;

@SuppressWarnings("restriction")
public class PreviewJavaScriptElementHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);

		if (activeEditor instanceof JavaEditor) {
			JavaEditor javaEditor = (JavaEditor) activeEditor;

			Object preview = null;

			// check for asset keys
			List<Object> assetKeys = TextUI
					.getReferencedAssetElements(javaEditor);

			if (assetKeys.isEmpty()) {
				// check for files
				String str = TextUI.getStringLiteralUnderCursor(javaEditor);
				if (str != null) {
					preview = TextUI.getReferencedFile(javaEditor, str);
				}
			} else {
				preview = assetKeys.get(0);
			}

			if (preview != null) {
				PhaserEditorUI.openPreview(preview);
			}
		}
		return null;
	}

}
