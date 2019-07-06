// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.scene.ui.editor.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.scene.core.SceneModel;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.messages.ReloadPageMessage;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.properties.FormPropertyPage;
import phasereditor.webrun.ui.WebRunUI;

/**
 * @author arian
 *
 */
public class WebViewSection extends BaseDesignSection {

	public WebViewSection(FormPropertyPage page) {
		super("WebView", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof SceneModel;
	}

	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(2, false));

		{
			var btn = new Button(comp, SWT.PUSH);
			btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			btn.setText("System Browser - pepe");
			btn.setImage(EditorSharedImages.getImage(IMG_WORLD));
			btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				WebRunUI.openBrowser(getEditor().getBroker().getUrl());
			}));
		}

		{
			var btn = createRefreshButton(comp, getEditor());
			btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		}

		{
			label(comp, "Debug Paint Calls",
					"*Check to count how many paint calls are requested.\nPaint calls should be requested by demand, not in a loop.");
			var btn = new Button(comp, SWT.CHECK);
			addUpdate(() -> {
				btn.setSelection(getSceneModel().isDebugPaintCalls());
			});
			btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				wrapOperation(() -> getSceneModel().setDebugPaintCalls(btn.getSelection()));
			}));
		}

		return comp;
	}

	public static Button createRefreshButton(Composite comp, SceneEditor editor) {
		var btn = new Button(comp, SWT.PUSH);

		btn.setText("Refresh");
		btn.setImage(EditorSharedImages.getImage(IMG_PAGE_REFRESH));
		btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			editor.getBroker().sendAll(new ReloadPageMessage());
		}));
		return btn;
	}

}
