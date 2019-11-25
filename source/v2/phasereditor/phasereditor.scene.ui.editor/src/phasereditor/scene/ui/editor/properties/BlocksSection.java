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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.ui.properties.CheckListener;
import phasereditor.ui.properties.FormPropertyPage;

/**
 * @author arian
 *
 */
public class BlocksSection extends BaseDesignSection {

	public BlocksSection(FormPropertyPage page) {
		super("Blocks", page);
	}

	@SuppressWarnings("unused")
	@Override
	public Control createContent(Composite parent) {

		var comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));

		{
			label(comp, "Scope Blocks to scene folder", "*Show only blocks in the same folder tree of the scene.");
			var btn = new Button(comp, SWT.CHECK);
			// btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			new CheckListener(btn) {

				@Override
				protected void accept(boolean value) {
					wrapOperation(() -> {
						getSceneModel().setScopeBlocksToFolder(value);
						getEditor().refreshBlocks();
					});
				}
			};
			addUpdate(() -> {
				btn.setSelection(getSceneModel().isScopeBlocksToFolder());
			});
		}

		return comp;
	}

}
