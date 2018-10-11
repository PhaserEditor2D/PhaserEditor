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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import phasereditor.inspect.core.jsdoc.IPhaserMember;
import phasereditor.inspect.core.jsdoc.PhaserType;
import phasereditor.inspect.ui.PhaserElementLabelProvider;
import phasereditor.inspect.ui.PhaserSubTypesContentProvider;
import phasereditor.inspect.ui.PhaserSuperTypesContentProvider;

/**
 * @author arian
 *
 */
public class PhaserHierarchyView extends ViewPart {

	public static final String ID = "phasereditor.inspect.ui.views.PhaserHierarchyView"; //$NON-NLS-1$
	private TreeViewer _viewer;

	public PhaserHierarchyView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		_viewer = new TreeViewer(parent);
		_viewer.setLabelProvider(new PhaserElementLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((IPhaserMember) element).getName();
			}
		});

		_viewer.setContentProvider(new PhaserSubTypesContentProvider());

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		getViewSite().setSelectionProvider(_viewer);

		{
			int options = DND.DROP_MOVE | DND.DROP_DEFAULT;
			DropTarget target = new DropTarget(_viewer.getControl(), options);
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
	}

	@Override
	public void setFocus() {
		_viewer.getTree().setFocus();
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

			_viewer.setInput(new Object[] { type });
			_viewer.expandToLevel(2);
		}
	}

	public void setShowSubTypes(boolean showSubtypes) {
		if (showSubtypes) {
			_viewer.setContentProvider(new PhaserSubTypesContentProvider());
		} else {
			_viewer.setContentProvider(new PhaserSuperTypesContentProvider());
		}
	}

}
