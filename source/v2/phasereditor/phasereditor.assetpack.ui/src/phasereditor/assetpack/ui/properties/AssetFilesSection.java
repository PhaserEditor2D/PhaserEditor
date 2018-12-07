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
package phasereditor.assetpack.ui.properties;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
public class AssetFilesSection extends FormPropertySection<AssetModel> {

	public AssetFilesSection() {
		super("Files");
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof AssetModel;
	}

	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(1, false));

		var viewer = new TableViewer(comp);

		var gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.heightHint = 50;
		var table = viewer.getTable();
		table.setHeaderVisible(false);
		table.setLayoutData(gd);

		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());

		addUpdate(() -> {
			var files = getModels().stream().flatMap(model -> Arrays.stream(model.computeUsedFiles()))
					.collect(toList());
			viewer.setInput(files);
		});

		return comp;
	}

}
