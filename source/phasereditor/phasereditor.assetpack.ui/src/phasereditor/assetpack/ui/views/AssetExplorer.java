// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.assetpack.ui.views;

import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.part.ViewPart;
import org.json.JSONArray;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackCore.IPacksChangeListener;
import phasereditor.assetpack.core.AssetPackCore.PackDelta;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.ui.FilteredTree2;
import phasereditor.ui.PatternFilter2;

public class AssetExplorer extends ViewPart {
	public static final String ID = "phasereditor.assetpack.views.assetExplorer";
	TreeViewer _viewer;
	private IPacksChangeListener _changeListener;
	private FilteredTree _filteredTree;
	// private AssetExplorerLabelProvider _treeLabelProvider;
	// private AssetExplorerContentProvider _treeContentProvider;
	// private AssetExplorerListLabelProvider _listLabelProvider;
	// private AssetExplorerListContentProvider _listContentProvider;
	static String ROOT = "root";

	static class Container {
		public Object[] children;
		public String name;

		public Container(String name, Object[] children) {
			super();
			this.children = children;
			this.name = name;
		}
	}

	public AssetExplorer() {
		super();
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout gl_parent = new GridLayout(1, false);
		parent.setLayout(gl_parent);

		_filteredTree = new FilteredTree2(parent, SWT.MULTI, new PatternFilter2(), 4);
		_viewer = _filteredTree.getViewer();
		_viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				showSelectionInEditor();
			}
		});
		Tree tree = _viewer.getTree();
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		_viewer.setContentProvider(new AssetExplorerContentProvider());
		_viewer.setLabelProvider(new AssetExplorerLabelProvider());

		afterCreateWidgets();

	}

	protected void showSelectionInEditor() {
		Object elem = ((IStructuredSelection) _viewer.getSelection()).getFirstElement();
		AssetPackUI.openElementInEditor(elem);
	}

	private void afterCreateWidgets() {
		// _treeLabelProvider = new AssetExplorerLabelProvider();
		// _treeContentProvider = new AssetExplorerContentProvider();
		// _listLabelProvider = new AssetExplorerListLabelProvider();
		// _listContentProvider = new AssetExplorerListContentProvider();

		// _viewer.setLabelProvider(_treeLabelProvider);
		// _viewer.setContentProvider(_treeContentProvider);

		// change listener

		AssetPackCore.getAssetPackModels();

		_changeListener = new AssetPackCore.IPacksChangeListener() {

			@Override
			public void packsChanged(PackDelta packDelta) {
				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						if (_viewer.getControl().isDisposed()) {
							return;
						}

						if (PlatformUI.getWorkbench().isClosing()) {
							return;
						}

						_viewer.refresh();
					}
				});
			}
		};
		AssetPackCore.addPacksChangedListener(_changeListener);

		// content

		_viewer.setInput(ROOT);
		_viewer.expandToLevel(3);

		// drag and drop

		Transfer[] types = { LocalSelectionTransfer.getTransfer(), TextTransfer.getInstance() };
		_viewer.addDragSupport(DND.DROP_MOVE | DND.DROP_DEFAULT, types, new DragSourceAdapter() {

			private Object[] _data;

			@Override
			public void dragStart(DragSourceEvent event) {
				LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
				_data = ((IStructuredSelection) _viewer.getSelection()).toArray();
				transfer.setSelection(new StructuredSelection(_data));
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				JSONArray array = new JSONArray();
				for (Object elem : _data) {
					if (elem instanceof IAssetKey) {
						array.put(AssetPackCore.getAssetJSONReference((IAssetKey) elem));
					}
				}
				event.data = array.toString();
			}
		});

		// selection provider

		getViewSite().setSelectionProvider(_viewer);

		{

			// menu
			MenuManager manager = new MenuManager();
			Tree tree = _viewer.getTree();
			Menu menu = manager.createContextMenu(tree);
			tree.setMenu(menu);

			getViewSite().registerContextMenu(manager, _viewer);
		}

		// tooltips
		AssetPackUI.installAssetTooltips(_viewer);

		// labels
		((AssetLabelProvider) _viewer.getLabelProvider()).setControl(_viewer.getControl());

	}

	@Override
	public void dispose() {
		AssetPackCore.removePacksChangedListener(_changeListener);
		super.dispose();
	}

	@Override
	public void setFocus() {
		_viewer.getTree().setFocus();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter.equals(IContextProvider.class)) {
			return new IContextProvider() {

				@Override
				public String getSearchExpression(Object target) {
					return null;
				}

				@Override
				public int getContextChangeMask() {
					return NONE;
				}

				@Override
				public IContext getContext(Object target) {
					IContext context = HelpSystem.getContext("phasereditor.help.assetexplorer");
					return context;
				}
			};
		}
		return super.getAdapter(adapter);
	}

	public void showList() {
		// changeViewMode(_listLabelProvider, _listContentProvider);
	}

	public void showTree() {
		// changeViewMode(_treeLabelProvider, _treeContentProvider);
	}

	// private void changeViewMode(ILabelProvider labelProvider,
	// ITreeContentProvider contentProvider) {
	// Object provider = _viewer.getLabelProvider();
	// if (provider != labelProvider) {
	// _viewer.getTree().clearAll(true);
	// _viewer.setLabelProvider(labelProvider);
	// _viewer.setContentProvider(contentProvider);
	// _viewer.setInput(ROOT);
	// _viewer.getTree().setRedraw(true);
	// _viewer.expandToLevel(3);
	// }
	// }
}
