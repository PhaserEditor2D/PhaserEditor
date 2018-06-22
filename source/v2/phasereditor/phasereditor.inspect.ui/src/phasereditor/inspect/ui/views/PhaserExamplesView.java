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
package phasereditor.inspect.ui.views;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.part.ViewPart;

import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.ui.TemplateContentProvider;
import phasereditor.inspect.ui.TemplateLabelProvider;
import phasereditor.ui.PatternFilter2;

/**
 * @author arian
 *
 */
public class PhaserExamplesView extends ViewPart {

	public static final String ID = "phasereditor.inspect.ui.views.phaserExamples"; //$NON-NLS-1$
	private TreeViewer _viewer;
	private FilteredTree _filteredViewer;

	public PhaserExamplesView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		_filteredViewer = new FilteredTree(parent, SWT.NONE, new PatternFilter2(), true);
		_viewer = _filteredViewer.getViewer();
		_viewer.setLabelProvider(new TemplateLabelProvider());
		_viewer.setContentProvider(new TemplateContentProvider());

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		_viewer.setInput(InspectCore.getPhaserExamplesRepoModel());
		getViewSite().setSelectionProvider(_viewer);
	}

	@Override
	public void setFocus() {
		_viewer.getControl().setFocus();
	}

}
