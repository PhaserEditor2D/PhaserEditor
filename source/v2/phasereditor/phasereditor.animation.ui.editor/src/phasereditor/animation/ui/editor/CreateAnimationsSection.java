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
package phasereditor.animation.ui.editor;

import static java.util.stream.Collectors.joining;
import static phasereditor.ui.PhaserEditorUI.plural;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.assetpack.core.IAssetKey;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
public class CreateAnimationsSection extends FormPropertySection<IAssetKey> {


	private AnimationBlocksPropertyPage _page;

	public CreateAnimationsSection(AnimationBlocksPropertyPage page) {
		super("Animations");
		
		_page = page;
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof IAssetKey;
	}

	@Override
	public boolean supportThisNumberOfModels(int number) {
		return number > 0;
	}

	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(1, false));

		{
			var btn = new Button(comp, SWT.PUSH);
			btn.setText("Create animations");
			btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				_page.getEditor().createAnimationsWithDrop(getModels().toArray());
			}));
			addUpdate(() -> {
				var groups = AnimationsEditor.splitFramesByPrefix(getModels().toArray());
				var size = groups.size();
				btn.setText("Create " + size + " " + plural("animation", size));
				btn.setToolTipText(groups

						.stream()

						.map(g -> g.getPrefix())

						.collect(joining("\n")));
			});
		}

		return comp;
	}

}
