package phasereditor.assetpack.ui.search;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultListener;
import org.eclipse.search.ui.ISearchResultPage;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.search.ui.SearchResultEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.progress.WorkbenchJob;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.FindAssetReferencesResult;
import phasereditor.assetpack.core.IAssetConsumer;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.IAssetReference;
import phasereditor.assetpack.core.IAssetReplacer;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.TextureDialog;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;

public class SearchAssetResultPage extends Page implements ISearchResultPage, ISearchResultListener {

	private String _id;
	private TreeViewer _viewer;
	private ISearchResultViewPart _searchView;
	private SearchAssetResult _result;
	private MenuManager _menu;
	private ReplaceAction _replaceAllAction;

	public SearchAssetResultPage() {
	}

	@Override
	public Object getUIState() {
		return null;
	}

	@Override
	public void setInput(ISearchResult search, Object uiState) {
		if (search == null) {
			return;
		}
		search.addListener(this);
	}

	@Override
	public void setViewPart(ISearchResultViewPart part) {
		_searchView = part;
	}

	@Override
	public void restoreState(IMemento memento) {
		//
	}

	@Override
	public void saveState(IMemento memento) {
		//

	}

	@Override
	public void setID(String id) {
		_id = id;
	}

	@Override
	public String getID() {
		return _id;
	}

	@Override
	public String getLabel() {
		if (_result == null) {
			return "Asset references";
		}

		return _result.getLabel();
	}

	@Override
	public void createControl(Composite parent) {
		createActions();
		createViewer(parent);
		createMenu();
	}

	private void createActions() {
		_replaceAllAction = new ReplaceAction(true);
	}

	private void createMenu() {
		_menu = new MenuManager("#SearchAssetPopUp"); //$NON-NLS-1$
		_menu.setRemoveAllWhenShown(true);
		_menu.setParent(getSite().getActionBars().getMenuManager());
		_menu.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager mgr) {
				// SearchView.createContextMenuGroups(mgr);
				fillContextMenu(mgr);
				// _searchView.fillContextMenu(mgr);
			}
		});

		Menu menu = _menu.createContextMenu(_viewer.getControl());
		_viewer.getControl().setMenu(menu);

		getSite().setSelectionProvider(_viewer);
		// getSite().registerContextMenu(_searchView.getViewSite().getId(),
		// _menu, _viewer);
	}

	public class ReplaceAction extends Action {
		private boolean _replaceAll;

		public ReplaceAction(boolean replaceAll) {
			super("Replace " + (replaceAll ? "All" : "Selected") + "...");
			_replaceAll = replaceAll;
		}

		@Override
		public void run() {
			TextureDialog dlg = new TextureDialog(getSite().getShell());
			dlg.setAllowNull(false);

			FindAssetReferencesResult findResult = _result.getReferences();

			if (!_replaceAll && !_viewer.getSelection().isEmpty()) {
				Object[] sel = _viewer.getStructuredSelection().toArray();

				FindAssetReferencesResult result2 = new FindAssetReferencesResult();
				for (Object elem : sel) {
					if (elem instanceof IFile) {
						result2.addAll(findResult.getReferencesOf((IFile) elem));
					} else {
						result2.add((IAssetReference) elem);
					}
				}

				findResult = result2;
			}

			dlg.setProject(findResult.getFirstReference().getFile().getProject());

			int r = dlg.open();
			if (r == Window.OK) {
				IAssetKey key = (IAssetKey) dlg.getResult();

				List<IAssetReplacer> replacers = new ArrayList<>();

				for (IAssetConsumer c : AssetPackCore.requestAssetConsumers()) {
					replacers.add(c.getAssetReplacer());
				}

				FindAssetReferencesResult finalResult = findResult;

				new WorkbenchJob("Replacing assets in editors.") {

					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						for (IAssetReplacer replacer : replacers) {
							replacer.replace_SWTThread(finalResult, key, monitor);
						}
						return Status.OK_STATUS;
					}
				}.schedule();

				new WorkspaceJob("Replacing asset in files.") {

					@Override
					public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {

						for (IAssetReplacer replacer : replacers) {
							try {
								replacer.replace_ResourceThread(finalResult, key, monitor);
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						}

						return Status.OK_STATUS;
					}
				}.schedule();

			}
		}
	}

	protected void fillContextMenu(IMenuManager manager) {
		manager.add(_replaceAllAction);
		manager.add(new ReplaceAction(false));
	}

	public ReplaceAction getReplaceAllAction() {
		return _replaceAllAction;
	}

	/**
	 * @param parent
	 */
	private void createViewer(Composite parent) {
		_viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);

		_viewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IAssetReference) {
					return ((IAssetReference) element).getLabel();
				}

				if (element instanceof IFile) {
					return ((IFile) element).getProjectRelativePath().toPortableString();
				}

				return super.getText(element);
			}

			@Override
			public Image getImage(Object element) {

				if (element instanceof IFile) {
					Image img = AssetLabelProvider.GLOBAL_16.getImage(element);

					if (img == null) {
						img = EditorSharedImages.getImage(IEditorSharedImages.IMG_CANVAS);
					}

					return img;
				}

				if (element instanceof IAssetReference) {
					return AssetLabelProvider.GLOBAL_16.getImage(((IAssetReference) element).getAssetKey());
				}

				return null;
			}

		});

		_viewer.setContentProvider(new ITreeContentProvider() {

			private FindAssetReferencesResult _references;

			@Override
			public boolean hasChildren(Object element) {
				return getChildren(element).length > 0;
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public Object[] getElements(Object inputElement) {
				return getChildren(inputElement);
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				_references = (FindAssetReferencesResult) newInput;
			}

			@Override
			public Object[] getChildren(Object parentElement) {

				if (parentElement instanceof FindAssetReferencesResult) {
					return ((FindAssetReferencesResult) parentElement).getFiles().toArray();
				}

				if (parentElement instanceof IFile) {
					return _references.getReferencesOf((IFile) parentElement).toArray();
				}

				return new Object[0];
			}
		});

		_viewer.addDoubleClickListener(e -> {
			Object elem = _viewer.getStructuredSelection().getFirstElement();
			try {
				if (elem instanceof IFile) {
					IDE.openEditor(_searchView.getViewSite().getPage(), (IFile) elem);
				} else if (elem instanceof IAssetReference) {
					IAssetReference ref = (IAssetReference) elem;
					ref.reveal(_searchView.getViewSite().getPage());
				}
			} catch (PartInitException e1) {
				throw new RuntimeException(e1);
			}
		});

		// tooltips

		List<IAssetConsumer> consumers = AssetPackCore.requestAssetConsumers();

		for (IAssetConsumer c : consumers) {
			c.installTooltips(_viewer);
		}
	}

	@Override
	public Control getControl() {
		return _viewer.getControl();
	}

	@Override
	public void setFocus() {
		_viewer.getControl().setFocus();
	}

	@Override
	public void searchResultChanged(SearchResultEvent e) {
		_result = (SearchAssetResult) e.getSearchResult();
		Display.getDefault().asyncExec(() -> {
			FindAssetReferencesResult refs = _result.getReferences();
			IAssetReference ref = refs.getFirstReference();

			_viewer.getControl().setRedraw(false);
			try {
				_viewer.setInput(refs);

				if (ref != null) {
					_viewer.expandToLevel(ref.getFile(), 1);
					_viewer.setSelection(new StructuredSelection(ref), true);
				}
			} finally {
				_viewer.getControl().setRedraw(true);
			}

			_searchView.updateLabel();
		});
	}

}
