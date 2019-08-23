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
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.part.ViewPart;

import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.jsdoc.IPhaserMember;
import phasereditor.inspect.ui.InspectUI;
import phasereditor.inspect.ui.PhaserElementContentProvider;
import phasereditor.inspect.ui.PhaserElementLabelProvider;
import phasereditor.inspect.ui.PhaserElementStyledLabelProvider;
import phasereditor.ui.PatternFilter2;
import phasereditor.ui.ViewerDragSourceListener;

/**
 * @author arian
 *
 */
public class PhaserTypesView extends ViewPart implements ISelectionListener, IPropertyChangeListener {

	public static final String ID = "phasereditor.inspect.ui.views.PhaserTypesView"; //$NON-NLS-1$
	private FilteredTree _filteredTree;
	private PhaserElementStyledLabelProvider _styleLabelProvider;

	public PhaserTypesView() {
	}

	/**
	 * Create contents of the view part.
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new FillLayout());

		_filteredTree = new FilteredTree(container, SWT.NONE, new PatternFilter2(), true);

		TreeViewer treeViewer = _filteredTree.getViewer();
		treeViewer.setContentProvider(new PhaserElementContentProvider(false));
		_styleLabelProvider = new PhaserElementStyledLabelProvider(new PhaserElementLabelProvider());
		treeViewer.setLabelProvider(_styleLabelProvider);

		treeViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				showSourceCodeOfSelectedItem();
			}
		});

		afterCreateWidgets();

		createActions();
		initializeToolBar();
		initializeMenu();
	}

	@Override
	public void dispose() {

		getViewSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);

		JFaceResources.getColorRegistry().removeListener(this);

		super.dispose();
	}

	void showSourceCodeOfSelectedItem() {
		Object elem = _filteredTree.getViewer().getStructuredSelection().getFirstElement();
		if (elem == null) {
			return;
		}

		InspectUI.showSourceCode((IPhaserMember) elem);
	}

	private void afterCreateWidgets() {
		JFaceResources.getColorRegistry().addListener(this);

		getViewSite().setSelectionProvider(_filteredTree.getViewer());

		_filteredTree.getViewer().setInput(InspectCore.getPhaserHelp());

		// InspectUI.installJsdocTooltips(_filteredTree.getViewer());

		getViewSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);

		init_DND();
	}

	private void init_DND() {
		{
			DragSource dragSource = new DragSource(_filteredTree.getViewer().getControl(),
					DND.DROP_MOVE | DND.DROP_DEFAULT);
			dragSource.setTransfer(new Transfer[] { TextTransfer.getInstance(), LocalSelectionTransfer.getTransfer() });
			dragSource.addDragListener(new ViewerDragSourceListener(_filteredTree.getViewer()));
		}
	}

	/**
	 * Create the actions.
	 */
	private void createActions() {
		// Create the actions
	}

	/**
	 * Initialize the toolbar.
	 */
	@SuppressWarnings("unused")
	private void initializeToolBar() {
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
	}
	
	/**
	 * Initialize the menu.
	 */
	@SuppressWarnings("unused")
	private void initializeMenu() {
		IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
	}

	@Override
	public void setFocus() {
		_filteredTree.getFilterControl().setFocus();
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (part == this) {
			return;
		}

		if (selection instanceof IStructuredSelection) {
			Object elem = ((IStructuredSelection) selection).getFirstElement();
			IPhaserMember member = Adapters.adapt(elem, IPhaserMember.class);

			if (member == null) {
				return;
			}

			TreeViewer viewer = _filteredTree.getViewer();
			Object[] path = buildTreePath(member);
			viewer.reveal(new TreePath(path));
			viewer.setSelection(new StructuredSelection(member));
		}

	}

	private Object[] buildTreePath(IPhaserMember member) {
		List<Object> path = new ArrayList<>();

		buildTreePath(path, member);

		return path.toArray();
	}

	private void buildTreePath(List<Object> path, IPhaserMember member) {
		if (InspectCore.getPhaserHelp().getGlobalScope().isGlobal(member)) {
			path.add(InspectCore.getPhaserHelp().getGlobalScope());
		}

		path.add(member);

		if (member.getContainer() != null) {
			buildTreePath(path, member.getContainer());
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		_styleLabelProvider.updateStyleValues();
		_filteredTree.getViewer().refresh();
	}

}
