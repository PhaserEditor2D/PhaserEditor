// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.assetpack.ui;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.IAssetElementModel;

/**
 * @author arian
 *
 */
public class FlatAssetLabelProvider extends StyledCellLabelProvider implements ILabelProvider{
	private AssetLabelProvider _baseLabelProvider;

	public FlatAssetLabelProvider(AssetLabelProvider baseLabelProvider) {
		super();
		_baseLabelProvider = baseLabelProvider;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.StyledCellLabelProvider#update(org.eclipse.
	 * jface.viewers.ViewerCell)
	 */
	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();

		String text = _baseLabelProvider.getText(element);

		String tail = getTailLabelProvider(element);

		if (tail != null) {
			cell.setText(text + tail);
			int start = text.length();
			int len = tail.length();
			Color fg = cell.getControl().getDisplay().getSystemColor(SWT.COLOR_GRAY);
			Color bg = null;
			cell.setStyleRanges(new StyleRange[] { new StyleRange(start, len, fg, bg) });
		} else {
			cell.setText(text);
		}

		cell.setImage(_baseLabelProvider.getImage(element));
		
		super.update(cell);
	}

	private static String getTailLabelProvider(Object element) {
		String tail = null;

		if (element instanceof AssetModel) {
			AssetModel asset = (AssetModel) element;
			tail = " - " + asset.getSection().getKey() + "/" + asset.getPack().getName();
		}

		if (element instanceof IAssetElementModel) {
			IAssetElementModel assetElem = (IAssetElementModel) element;
			tail = " - " + assetElem.getAsset().getKey() + "/" + assetElem.getAsset().getSection().getKey() + "/"
					+ assetElem.getAsset().getPack().getName();
		}
		return tail;
	}

	@Override
	public Image getImage(Object element) {
		return null;
	}

	@Override
	public String getText(Object element) {
		String tail = getTailLabelProvider(element);
		return _baseLabelProvider.getText(element) + (tail == null? "" : tail);
	}

}
