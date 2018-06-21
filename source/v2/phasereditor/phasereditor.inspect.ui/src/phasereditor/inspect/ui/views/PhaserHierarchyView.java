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

import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import phasereditor.inspect.core.jsdoc.IPhaserMember;
import phasereditor.inspect.core.jsdoc.PhaserType;
import phasereditor.inspect.ui.PhaserElementLabelProvider;
import phasereditor.inspect.ui.PhaserHierarchyContentProvider;

/**
 * @author arian
 *
 */
public class PhaserHierarchyView extends ViewPart implements ISelectionListener {

	public static final String ID = "phasereditor.inspect.ui.views.PhaserHierarchyView"; //$NON-NLS-1$
	private TreeViewer _viewer;

	public PhaserHierarchyView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		_viewer = new TreeViewer(parent);
		_viewer.setLabelProvider(new PhaserElementLabelProvider());
		_viewer.setContentProvider(new PhaserHierarchyContentProvider());

		afterCreateWidgets();
	}

	@Override
	public void dispose() {

		getViewSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);

		super.dispose();
	}

	private void afterCreateWidgets() {
		_viewer.setInput(new Object());

		getViewSite().setSelectionProvider(_viewer);

		getViewSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
	}

	@Override
	public void setFocus() {
		_viewer.getTree().setFocus();
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (part == this) {
			return;
		}

		if (selection instanceof IStructuredSelection) {

			Object element = ((IStructuredSelection) selection).getFirstElement();

			IPhaserMember member = Adapters.adapt(element, IPhaserMember.class);

			if (member != null) {
				PhaserType type;
				if (member instanceof PhaserType) {
					type = (PhaserType) member;
				} else {
					if (member.getContainer() instanceof PhaserType) {
						type = (PhaserType) member.getContainer();
					} else {
						return;
					}
				}

				_viewer.setContentProvider(new PhaserHierarchyContentProvider(type));
				_viewer.expandAll();
				_viewer.setSelection(new StructuredSelection(type), true);
			}
		}
	}

}
