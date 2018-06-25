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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.part.ViewPart;

import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.examples.PhaserExampleCategoryModel;
import phasereditor.inspect.core.examples.PhaserExampleModel;
import phasereditor.inspect.ui.TemplateContentProvider;
import phasereditor.inspect.ui.TemplateLabelProvider;
import phasereditor.ui.PatternFilter2;

/**
 * @author arian
 *
 */
public class PhaserExamplesView extends ViewPart implements ISelectionListener {

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

	@Override
	public void dispose() {

		getViewSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);

		super.dispose();
	}

	private void afterCreateWidgets() {
		_viewer.setInput(InspectCore.getPhaserExamplesRepoModel());
		getViewSite().setSelectionProvider(_viewer);
		getViewSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
	}

	@Override
	public void setFocus() {
		_viewer.getControl().setFocus();
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (part == this) {
			return;
		}

		if (selection instanceof IStructuredSelection) {
			Object elem = ((IStructuredSelection) selection).getFirstElement();

			if (elem == null) {
				return;
			}

			elem = Adapters.adapt(elem, PhaserExampleModel.class);

			if (elem == null) {
				return;
			}

			Object[] path = buildPath((PhaserExampleModel) elem);
			_viewer.reveal(new TreePath(path));
			_viewer.setSelection(new StructuredSelection(elem));
		}
	}

	private Object[] buildPath(PhaserExampleModel elem) {
		List<Object> path = new ArrayList<>();

		buildPath(elem, path);

		return path.toArray();
	}

	private void buildPath(Object elem, List<Object> path) {
		Object parent = null;
		if (elem instanceof PhaserExampleModel) {
			parent = ((PhaserExampleModel) elem).getCategory();
		} else {
			parent = ((PhaserExampleCategoryModel) elem).getParentCategory();
		}

		if (parent != null) {

			buildPath(parent, path);

			path.add(parent);
		}

		path.add(elem);
	}

}
