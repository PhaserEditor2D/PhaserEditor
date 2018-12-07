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

import java.util.List;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.AssetsTreeCanvasViewer;
import phasereditor.ui.FilteredTreeCanvas;
import phasereditor.ui.TreeArrayContentProvider;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
public class ManyAnimationPreviewSection extends FormPropertySection<AnimationModel> {

	public ManyAnimationPreviewSection() {
		super("Animations Preview");
		setFillSpace(true);
	}

	@Override
	public boolean supportThisNumberOfModels(int number) {
		return number > 1;
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof AnimationModel;
	}

	@Override
	public Control createContent(Composite parent) {
		return createAnimationsViewer(this, parent, () -> getModels());
	}

	public static FilteredTreeCanvas createAnimationsViewer(FormPropertySection<?> section, Composite parent,
			Supplier<List<AnimationModel>> input) {
		// preview
		var tree = new FilteredTreeCanvas(parent, SWT.BORDER);
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		var viewer = new AssetsTreeCanvasViewer(tree.getTree(), new TreeArrayContentProvider(),
				AssetLabelProvider.GLOBAL_16);
		section.addUpdate(() -> {
			viewer.setInput(input.get());
		});
		return tree;
	}

}
