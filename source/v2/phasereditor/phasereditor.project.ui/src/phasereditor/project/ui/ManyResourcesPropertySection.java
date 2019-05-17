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
package phasereditor.project.ui;

import static java.util.stream.Collectors.summingLong;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
public class ManyResourcesPropertySection extends FormPropertySection<IResource> {

	public ManyResourcesPropertySection() {
		super("Resource");
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof IResource;
	}

	@Override
	public boolean supportThisNumberOfModels(int number) {
		return number > 1;
	}

	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(2, false));

		{
			label(comp, "Size", "");
			var text = new Text(comp, SWT.READ_ONLY | SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			addUpdate(() -> {
				try {
					var size = getModels().stream().filter(e -> e instanceof IFile)
							.collect(summingLong(r -> r.getLocation().toFile().length()));
					text.setText(PhaserEditorUI.getFileHumanSize(size.longValue()));
				} catch (Exception e) {
					ProjectUI.logError(e);
					text.setText("");
				}
			});
		}

		{
			label(comp, "Count", "");
			var text = new Text(comp, SWT.READ_ONLY | SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			addUpdate(() -> {
				try {
					text.setText(Integer.toString(getModels().size()));
				} catch (Exception e) {
					ProjectUI.logError(e);
					text.setText("");
				}
			});
		}

		return comp;
	}

}
