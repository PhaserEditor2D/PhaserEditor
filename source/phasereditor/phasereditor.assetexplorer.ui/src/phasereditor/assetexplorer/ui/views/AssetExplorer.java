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
package phasereditor.assetexplorer.ui.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.json.JSONArray;
import org.json.JSONObject;

import phasereditor.assetpack.core.AssetGroupModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.canvas.core.Prefab;
import phasereditor.ui.FilteredTree2;
import phasereditor.ui.PatternFilter2;

public class AssetExplorer extends ViewPart {
	public static final String ID = "phasereditor.assetpack.views.assetExplorer";
	TreeViewer _viewer;
	private FilteredTree _filteredTree;
	// private AssetExplorerLabelProvider _treeLabelProvider;
	// private AssetExplorerContentProvider _treeContentProvider;
	// private AssetExplorerListLabelProvider _listLabelProvider;
	// private AssetExplorerListContentProvider _listContentProvider;
	static String ROOT = "root";
	static String PREFABS_ROOT = "Prefabs";

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

	@SuppressWarnings("unused")
	@Override
	public void createPartControl(Composite parent) {
		GridLayout gl_parent = new GridLayout(1, false);
		gl_parent.marginWidth = 0;
		gl_parent.marginHeight = 0;
		parent.setLayout(gl_parent);

		_filteredTree = new FilteredTree2(parent, SWT.MULTI, new PatternFilter2(), 4);
		GridLayout gridLayout = (GridLayout) _filteredTree.getLayout();
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

		if (elem instanceof Prefab) {
			try {
				IDE.openEditor(getSite().getPage(), ((Prefab) elem).getFile());
			} catch (PartInitException e) {
				throw new RuntimeException(e);
			}
		}

		AssetPackUI.openElementInEditor(elem);
	}

	private void afterCreateWidgets() {
		// _treeLabelProvider = new AssetExplorerLabelProvider();
		// _treeContentProvider = new AssetExplorerContentProvider();
		// _listLabelProvider = new AssetExplorerListLabelProvider();
		// _listContentProvider = new AssetExplorerListContentProvider();

		// _viewer.setLabelProvider(_treeLabelProvider);
		// _viewer.setContentProvider(_treeContentProvider);

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

	public void refreshContent() {
		if (_viewer.getControl().isDisposed()) {
			return;
		}

		if (PlatformUI.getWorkbench().isClosing()) {
			return;
		}

		Object[] expanded = _viewer.getVisibleExpandedElements();

		_viewer.getTree().setRedraw(false);
		_viewer.refresh();

		List<Object> toExpand = new ArrayList<>();

		for (Object obj : expanded) {
			if (obj instanceof IAssetKey) {
				IAssetKey key = ((IAssetKey) obj).getSharedVersion();
				toExpand.add(key);
			} else if (obj instanceof AssetGroupModel) {
				AssetPackModel oldPack = ((AssetGroupModel) obj).getSection().getPack();
				JSONObject ref = oldPack.getAssetJSONRefrence(obj);
				IFile file = oldPack.getFile();
				AssetPackModel newPack = AssetPackCore.getAssetPackModel(file, false);
				if (newPack != null) {
					Object obj2 = newPack.getElementFromJSONReference(ref);
					if (obj2 != null) {
						toExpand.add(obj2);
					}
				}
			} else if (obj instanceof AssetSectionModel) {
				AssetPackModel oldPack = ((AssetSectionModel) obj).getPack();
				JSONObject ref = oldPack.getAssetJSONRefrence(obj);
				IFile file = oldPack.getFile();
				AssetPackModel newPack = AssetPackCore.getAssetPackModel(file, false);
				if (newPack != null) {
					Object obj2 = newPack.getElementFromJSONReference(ref);
					toExpand.add(obj2);
				}
			} else if (obj instanceof AssetPackModel) {
				AssetPackModel newPack = AssetPackCore.getAssetPackModel(((AssetPackModel) obj).getFile(), false);
				toExpand.add(newPack);
			} else if (obj == PREFABS_ROOT) {
				toExpand.add(obj);
			}
		}
		toExpand.remove(null);
		_viewer.setExpandedElements(toExpand.toArray());
		_viewer.getTree().setRedraw(true);
	}

}
