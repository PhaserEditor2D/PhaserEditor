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
package phasereditor.ui;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * @author arian
 *
 */
public class PatternFilter2 extends PatternFilter {
	public PatternFilter2() {
		setIncludeLeadingWildcard(true);
	}
	
	@Override
	public boolean isElementVisible(Viewer viewer, Object element) {
		if (super.isElementVisible(viewer, element)) {
			return true;
		}

		if (anyParentVisible(viewer, element)) {
			return true;
		}
		return false;
	}

	private boolean anyParentVisible(Viewer viewer, Object element) {
		Object parent = ((ITreeContentProvider) ((TreeViewer) viewer).getContentProvider()).getParent(element);
		if (parent == null) {
			return false;
		}

		if (isLeafMatch(viewer, parent)) {
			return true;
		}
		return anyParentVisible(viewer, parent);
	}
}
