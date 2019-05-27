// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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

import static phasereditor.ui.IEditorSharedImages.IMG_TAG_GREEN;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
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
import phasereditor.inspect.ui.PhaserElementLabelProvider;
import phasereditor.inspect.ui.PhaserElementStyledLabelProvider;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.PatternFilter2;
import phasereditor.ui.ViewerDragSourceListener;

/**
 * @author arian
 *
 */
public class PhaserVersionsView extends ViewPart implements ISelectionListener, IPropertyChangeListener {

	public PhaserVersionsView() {
	}

	public static final String ID = "phasereditor.inspect.ui.views.PhaserVersionsView"; //$NON-NLS-1$
	private FilteredTree _filteredTree;
	private PhaserElementStyledLabelProvider _styleLabelProvider;

	/**
	 * Create contents of the view part.
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new FillLayout());

		var filter = new PatternFilter2();
		_filteredTree = new FilteredTree(container, SWT.NONE, filter, true);

		TreeViewer treeViewer = _filteredTree.getViewer();

		var labelProvider = new MyLabelProvider();
		var contentProvider = new MyContentProvider(labelProvider);

		_styleLabelProvider = new PhaserElementStyledLabelProvider(labelProvider, true) {
			@Override
			public void update(ViewerCell cell) {
				super.update(cell);

				if (cell.getElement() instanceof String) {
					var ver = (String) cell.getElement();
					cell.setImage(EditorSharedImages.getImage(IMG_TAG_GREEN));
					cell.setText(ver + " (" + contentProvider.getChildren(ver).length + ")");
					cell.setStyleRanges(new StyleRange[] { new StyleRange(ver.length(), cell.getText().length(),
							JFaceResources.getColorRegistry().get(JFacePreferences.COUNTER_COLOR), null) });
				}
			}
		};
		treeViewer.setLabelProvider(_styleLabelProvider);

		treeViewer.setContentProvider(contentProvider);

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

	private static class MyLabelProvider extends PhaserElementLabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof IPhaserMember) {
				var member = (IPhaserMember) element;
				if (member.getContainer() != null) {
					return member.getContainer().getName() + "." + member.getName();
				}
			}

			return super.getText(element);
		}
	}

	private static class MyContentProvider implements ITreeContentProvider {
		private static final Object[] EMPTY = new Object[0];
		private Object[] _sinceList;
		private MyLabelProvider _labelProvider;

		public MyContentProvider(MyLabelProvider labelProvider) {
			_labelProvider = labelProvider;
			_sinceList = InspectCore.getPhaserHelp().getMembersMap().values().stream()

					.map(m -> m.getSince())

					.filter(since -> since != null)

					.distinct()

					.sorted((a, b) -> -Integer.compare(sinceValue(a), sinceValue(b)))

					.toArray();
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return _sinceList;
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof String) {

				var version = (String) parentElement;

				return InspectCore.getPhaserHelp().getMembersMap().values().stream()

						.filter(m -> version.equals(m.getSince()))

						.distinct()

						.sorted(

								(a, b) -> {
									if (a.getContainer() == b.getContainer()) {
										return a.getClass().getName().compareTo(b.getClass().getName());
									}

									return _labelProvider.getText(a).compareTo(_labelProvider.getText(b));
								}

						)

						.toArray();

			}

			return EMPTY;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

	}

	@Override
	public void dispose() {

		getViewSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);

		JFaceResources.getColorRegistry().removeListener(this);

		super.dispose();
	}

	private static int sinceValue(String since) {
		var comps = since.split("\\.");
		var v0 = Integer.parseInt(comps[0]);
		var v1 = Integer.parseInt(comps[1]);
		var v2 = Integer.parseInt(comps[2]);
		return v0 * 1000 + v1 * 100 + v2;
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

			if (member == null || member.getSince() == null) {
				return;
			}

			TreeViewer viewer = _filteredTree.getViewer();
			viewer.reveal(new TreePath(new Object[] { member.getSince(), member }));
			viewer.setSelection(new StructuredSelection(member));
		}

	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		_styleLabelProvider.updateStyleValues();
		_filteredTree.getViewer().refresh();
	}

}
