package phasereditor.assetpack.ui.search;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
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
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.Page;

import phasereditor.assetpack.core.FindAssetReferencesResult;
import phasereditor.assetpack.core.IAssetReference;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;

public class SearchAssetResultPage extends Page implements ISearchResultPage, ISearchResultListener {

	private String _id;
	private TreeViewer _viewer;
	private ISearchResultViewPart _searchView;
	private SearchAssetResult _result;

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
			_viewer.setInput(_result.getReferences());
			_searchView.updateLabel();
		});
	}
}
