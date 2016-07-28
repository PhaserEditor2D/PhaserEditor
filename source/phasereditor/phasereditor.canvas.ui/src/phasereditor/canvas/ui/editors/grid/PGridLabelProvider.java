// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.ui.editors.grid;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.ui.AssetLabelProvider;

/**
 * @author arian
 *
 */
public class PGridLabelProvider extends ColumnLabelProvider {
	private static final double FACTOR = 0.7;

	private Viewer _viewer;

	public PGridLabelProvider(Viewer viewer) {
		_viewer = viewer;
	}

	public Viewer getViewer() {
		return _viewer;
	}
	
	@Override
	public String getToolTipText(Object element) {
		if (element instanceof PGridProperty) {
			PGridProperty<?> prop = (PGridProperty<?>) element;
			String tooltip = prop.getTooltip();
			
			
			// needed in windows, else the tooltip window is messed
			tooltip = tooltip.replace("\r", "\n");
			
			
			return tooltip;
		}
		return super.getToolTipText(element);
	}

	@Override
	public Image getToolTipImage(Object object) {
		if (object instanceof PGridFrameProperty) {
			IAssetFrameModel value = ((PGridFrameProperty) object).getValue();
			return AssetLabelProvider.GLOBAL_48.getImage(value);
		}
		return super.getToolTipImage(object);
	}

	@Override
	public Font getFont(Object element) {
		if (isModified(element)) {
			return getBoldFont();
		}
		return getNormalFont();
	}

	protected Font getNormalFont() {
		return getViewer().getControl().getFont();
	}

	protected Font getBoldFont() {
		return SWTResourceManager.getBoldFont(_viewer.getControl().getFont());
	}

	public static RGB darker(RGB rgb, double factor) {
		int r = rgb.red;
		int g = rgb.green;
		int b = rgb.blue;

		return new RGB(Math.max((int) (r * factor), 0), Math.max((int) (g * factor), 0),
				Math.max((int) (b * factor), 0));
	}

	public static RGB brighter(RGB rgb) {
		int r = rgb.red;
		int g = rgb.green;
		int b = rgb.blue;

		/*
		 * From 2D group: 1. black.brighter() should return grey 2. applying
		 * brighter to blue will always return blue, brighter 3. non pure color
		 * (non zero rgb) will eventually return white
		 */
		int i = (int) (1.0 / (1.0 - FACTOR));
		if (r == 0 && g == 0 && b == 0) {
			return new RGB(i, i, i);
		}
		if (r > 0 && r < i)
			r = i;
		if (g > 0 && g < i)
			g = i;
		if (b > 0 && b < i)
			b = i;

		return new RGB(Math.min((int) (r / FACTOR), 255), Math.min((int) (g / FACTOR), 255),
				Math.min((int) (b / FACTOR), 255));
	}

	public static boolean isModified(Object element) {
		if (element instanceof PGridProperty) {
			PGridProperty<?> prop = (PGridProperty<?>) element;
			if (prop.isModified()) {
				return true;
			}
		}
		return false;
	}
}
