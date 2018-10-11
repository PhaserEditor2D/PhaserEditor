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
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import phasereditor.inspect.core.jsdoc.IPhaserMember;
import phasereditor.inspect.core.jsdoc.PhaserType;
import phasereditor.inspect.ui.PhaserElementContentProvider;
import phasereditor.inspect.ui.PhaserElementLabelProvider;
import phasereditor.inspect.ui.PhaserSubTypesContentProvider;
import phasereditor.inspect.ui.PhaserSuperTypesContentProvider;
import phasereditor.ui.ComplexSelectionProvider;

/**
 * @author arian
 *
 */
public class PhaserHierarchyView extends ViewPart {

	public static final String ID = "phasereditor.inspect.ui.views.PhaserHierarchyView"; //$NON-NLS-1$
	private TreeViewer _hierarchyViewer;
	private TreeViewer _membersViewer;

	public PhaserHierarchyView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		var sash = new SashForm(parent, SWT.VERTICAL);

		_hierarchyViewer = new TreeViewer(sash);
		
		var labelProvider = new PhaserElementLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((IPhaserMember) element).getName();
			}
		};
		
		_hierarchyViewer.setLabelProvider(labelProvider);

		_hierarchyViewer.setContentProvider(new PhaserSubTypesContentProvider());

		_membersViewer = new TreeViewer(sash);
		_membersViewer.setLabelProvider(labelProvider);
		_membersViewer.setContentProvider(new PhaserElementContentProvider(false));

		sash.setWeights(new int[] { 1, 1 });

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		getViewSite().setSelectionProvider(new ComplexSelectionProvider(_hierarchyViewer, _membersViewer));

		{
			int options = DND.DROP_MOVE | DND.DROP_DEFAULT;
			DropTarget target = new DropTarget(_hierarchyViewer.getControl(), options);
			Transfer[] types = { LocalSelectionTransfer.getTransfer() };
			target.setTransfer(types);
			target.addDropListener(new DropTargetAdapter() {
				@Override
				public void drop(DropTargetEvent event) {
					if (event.data instanceof Object[]) {
						displayType(((Object[]) event.data)[0]);
					}
					if (event.data instanceof IStructuredSelection) {
						displayType(((IStructuredSelection) event.data).getFirstElement());
					}
				}
			});
		}

		_hierarchyViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				showMembers(event.getStructuredSelection().getFirstElement());
			}
		});
	}

	protected void showMembers(Object element) {
		if (element == null) {
			_membersViewer.setInput(new Object[] {});
		} else {
			var type = (PhaserType) element;
			_membersViewer.setInput(type);
		}
	}

	@Override
	public void setFocus() {
		_hierarchyViewer.getTree().setFocus();
	}

	public void displayType(Object element) {
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

			_hierarchyViewer.setInput(new Object[] { type });
			_hierarchyViewer.expandToLevel(2);
		}
	}

	public void setShowSubTypes(boolean showSubtypes) {
		if (showSubtypes) {
			_hierarchyViewer.setContentProvider(new PhaserSubTypesContentProvider());
		} else {
			_hierarchyViewer.setContentProvider(new PhaserSuperTypesContentProvider());
		}
	}

}
