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
package phasereditor.ui.properties;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Font;

/**
 * @author arian
 *
 */
public class PGridKeyLabelProvider extends PGridLabelProvider {

	public PGridKeyLabelProvider(Viewer viewer) {
		super(viewer);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof PGridSection) {
			return ((PGridSection) element).getName();
		}

		if (element instanceof PGridProperty<?>) {
			return ((PGridProperty<?>) element).getName();
		}

		return super.getText(element);
	}

	@Override
	public Font getFont(Object element) {
		if (element instanceof PGridSection) {
			return getNormalFont();
		}
		return super.getFont(element);
	}
}
