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
package phasereditor.assetpack.ui.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.ui.preview.ExplainAssetFrameCanvas;

/**
 * @author arian
 *
 */
public class SingleFrameSection extends BaseAssetPackEditorSection<IAssetKey> {

	public SingleFrameSection(AssetPackEditorPropertyPage page) {
		super(page, "Texture Preview");
		setFillSpace(true);
	}

	@Override
	public boolean canEdit(Object obj) {
		return canEdit2(obj);
	}

	public static boolean canEdit2(Object obj) {
		return obj instanceof IAssetFrameModel || obj instanceof ImageAssetModel;
	}

	@Override
	public Control createContent(Composite parent) {
		var preview = new ExplainAssetFrameCanvas(parent, SWT.BORDER);
		preview.setLayoutData(new GridData(GridData.FILL_BOTH));
		addUpdate(() -> {
			var model = getModels().get(0);
			if (model instanceof ImageAssetModel) {
				model = ((ImageAssetModel) model).getFrame();
			}
			preview.setModel((IAssetFrameModel) model);
		});
		return preview;
	}

}
