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
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackCore.IPacksChangeListener;
import phasereditor.assetpack.core.AssetPackCore.PackDelta;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.ui.PhaserEditorUI;

public class AssetExplorer extends ViewPart {
	Text _searchText;
	TreeViewer _viewer;
	private IPacksChangeListener _changeListener;
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
		gl_parent.marginWidth = 0;
		gl_parent.marginHeight = 0;
		parent.setLayout(gl_parent);

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout gl_composite = new GridLayout(1, false);
		gl_composite.marginWidth = 0;
		gl_composite.marginHeight = 0;
		composite.setLayout(gl_composite);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		_searchText = new Text(composite, SWT.BORDER);
		_searchText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		_viewer = new TreeViewer(composite, SWT.NONE);
		_viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				showSelectionInEditor();
			}
		});
		Tree tree = _viewer.getTree();
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		_viewer.setLabelProvider(new AssetExplorerLabelProvider());
		_viewer.setContentProvider(new AssetExplorerContentProvider());

		afterCreateWidgets();

	}

	protected void showSelectionInEditor() {
		Object elem = ((IStructuredSelection) _viewer.getSelection()).getFirstElement();
		AssetPackUI.openElementInEditor(elem);
	}

	private void afterCreateWidgets() {
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
		// UIUtils.initSearchText(_searchText, _viewer, new AssetsFilter());
		PhaserEditorUI.initSearchText(_searchText, _viewer, new AssetExplorerLabelProvider());

		// drag and drop

		Transfer[] types = { LocalSelectionTransfer.getTransfer(), TextTransfer.getInstance() };
		_viewer.addDragSupport(DND.DROP_MOVE | DND.DROP_DEFAULT, types, new DragSourceAdapter() {

			@Override
			public void dragStart(DragSourceEvent event) {
				LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
				transfer.setSelection(_viewer.getSelection());
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				IStructuredSelection sel = _viewer.getStructuredSelection();
				Object[] elems = sel.toArray();
				if (elems.length == 1) {
					Object elem = elems[0];
					event.data = AssetPackCore.getAssetStringReference(elem);
				}
			}
		});

		// selection provider

		getViewSite().setSelectionProvider(_viewer);

		AssetPackUI.installAssetTooltips(_viewer);
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
}
