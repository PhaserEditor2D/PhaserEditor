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
package phasereditor.assetpack.ui.properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.ui.ExplainFrameDataCanvas;
import phasereditor.ui.ImageProxy;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
public class ImageFilePreviewPropertySection extends FormPropertySection<IFile> {

	public ImageFilePreviewPropertySection() {
		super("Image Preview");
		setFillSpace(true);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof IFile && AssetPackCore.isImage((IResource) obj);
	}

	@Override
	public boolean supportThisNumberOfModels(int number) {
		return number == 1;
	}

	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(1, false));
		var preview = new ExplainFrameDataCanvas(comp, 0);
		addUpdate(() -> {
			var file = getModels().get(0);
			var proxy = ImageProxy.get(file, null);
			var fd = proxy.getFinalFrameData();
			preview.setImageInfo(file, fd);
		});
		preview.setLayoutData(new GridData(GridData.FILL_BOTH));
		return comp;
	}

}
