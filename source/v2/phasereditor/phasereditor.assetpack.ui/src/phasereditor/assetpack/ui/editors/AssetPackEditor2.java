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
package phasereditor.assetpack.ui.editors;

import java.beans.PropertyChangeEvent;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.properties.IPropertySheetPage;

import phasereditor.assetpack.core.AssetFactory;
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.assetpack.ui.AssetsContentProvider;
import phasereditor.assetpack.ui.editors.operations.AddAssetOperation2;
import phasereditor.lic.LicCore;
import phasereditor.ui.ComplexSelectionProvider;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.PatternFilter2;
import phasereditor.ui.properties.PGridPage;

/**
 * @author arian
 *
 */
public class AssetPackEditor2 extends EditorPart {

	public static final IUndoContext UNDO_CONTEXT = new IUndoContext() {

		@Override
		public boolean matches(IUndoContext context) {
			return context == this || WorkspaceUndoUtil.getWorkspaceUndoContext().matches(context);
		}

		@Override
		public String getLabel() {
			return "ASSET_PACK_EDITOR_CONTEXT";
		}
	};

	private AssetPackModel _model;
	private Composite _container;
	private SectionsComp _sectionsComp;
	private TypesComp _typesComp;
	private AssetsComp _assetsComp;

	private PGridPage _properties;

	@Override
	public void doSave(IProgressMonitor monitor) {
		_model.save(monitor);
		firePropertyChange(PROP_DIRTY);
		saveEditingPoint();
		refresh();
	}

	public void refresh() {
		_sectionsComp.getViewer().refresh();
		_typesComp.getViewer().refresh();
		_assetsComp.getViewer().refresh();
	}

	public void saveEditingPoint() {
		//
	}

	@Override
	public void doSaveAs() {
		//
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public boolean isDirty() {
		return _model.isDirty();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	class ColumnComp extends Composite {

		private TreeViewer _viewer;
		private ToolBar _toolbar;

		public ColumnComp(Composite parent, String title) {
			super(parent, SWT.BORDER);

			{
				GridLayout layout = new GridLayout(1, false);
				layout.marginWidth = 0;
				layout.marginHeight = 0;
				layout.verticalSpacing = 2;
				setLayout(layout);
			}

			{
				Composite top = new Composite(this, SWT.NONE);

				GridLayout layout = new GridLayout(2, false);
				layout.marginWidth = 0;
				layout.marginHeight = 0;

				top.setLayout(layout);
				top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

				Label label = new Label(top, SWT.NONE);
				label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				label.setText(title);

				_toolbar = new ToolBar(top, SWT.NONE);
				var gd = new GridData(SWT.RIGHT, SWT.FILL, false, false);
				_toolbar.setLayoutData(gd);
				ToolItem item = new ToolItem(_toolbar, SWT.NONE);
				gd.heightHint = item.getBounds().height + 4;
				item.dispose();
				fillToolbar(_toolbar);
			}

			{
				FilteredTree tree = new FilteredTree(this, SWT.NONE, new PatternFilter2(), true);
				tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				_viewer = tree.getViewer();
			}

		}

		@SuppressWarnings("unused")
		protected void fillToolbar(ToolBar toolbar) {
			// empty
		}

		public TreeViewer getViewer() {
			return _viewer;
		}

	}

	class EditableColumnComp extends ColumnComp implements ISelectionChangedListener {

		protected ToolItem _addBtn;
		protected ToolItem _deleteBtn;
		protected ToolItem _renameBtn;

		public EditableColumnComp(Composite parent, String title) {
			super(parent, title);

			getViewer().addSelectionChangedListener(this);

			validateButtons();
		}

		@Override
		protected void fillToolbar(ToolBar toolbar) {
			{
				ToolItem item = new ToolItem(toolbar, SWT.NONE);
				item.setImage(EditorSharedImages.getImage(IEditorSharedImages.IMG_ADD));
				item.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						onClickedAddButton();
					}
				});
				_addBtn = item;
			}
			{
				ToolItem item = new ToolItem(toolbar, SWT.NONE);
				item.setImage(EditorSharedImages.getImage(IEditorSharedImages.IMG_DELETE));
				item.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						onClickedDeleteButton();
					}
				});
				_deleteBtn = item;
			}
			{
				ToolItem item = new ToolItem(toolbar, SWT.NONE);
				item.setImage(EditorSharedImages.getImage(IEditorSharedImages.IMG_RENAME));
				item.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						onClickedRenameButton();
					}
				});
				_renameBtn = item;
			}
		}

		protected void onClickedAddButton() {
			//
		}

		protected void onClickedDeleteButton() {
			//
		}

		protected void onClickedRenameButton() {
			//
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			validateButtons();
		}

		private void validateButtons() {
			ITreeSelection sel = getViewer().getStructuredSelection();
			_deleteBtn.setEnabled(!sel.isEmpty());
			_renameBtn.setEnabled(!sel.isEmpty());
		}
	}

	public class SectionsComp extends EditableColumnComp {

		public SectionsComp(Composite parent) {
			super(parent, "Sections");

			getViewer().setContentProvider(new ITreeContentProvider() {

				@Override
				public boolean hasChildren(Object element) {
					return false;
				}

				@Override
				public Object getParent(Object element) {
					return null;
				}

				@Override
				public Object[] getElements(Object inputElement) {
					return ((AssetPackModel) inputElement).getSections().toArray();
				}

				@Override
				public Object[] getChildren(Object parentElement) {
					return new Object[] {};
				}
			});
			getViewer().setLabelProvider(AssetLabelProvider.GLOBAL_16);
		}

		public AssetSectionModel getSelectedSection() {
			return (AssetSectionModel) getViewer().getStructuredSelection().getFirstElement();
		}
	}

	class SectionTypePairModel implements Comparable<SectionTypePairModel>, IAdaptable {
		public AssetSectionModel section;
		public AssetType type;

		public SectionTypePairModel(AssetSectionModel section, AssetType type) {
			super();
			this.section = section;
			this.type = type;
		}

		public List<AssetModel> getAssets() {
			return section.getGroup(type).getAssets();
		}

		@Override
		public int compareTo(SectionTypePairModel o) {
			return -Integer.compare(getAssets().size(), o.getAssets().size());
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public Object getAdapter(Class adapter) {
			return Adapters.adapt(type, adapter);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((section == null) ? 0 : section.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SectionTypePairModel other = (SectionTypePairModel) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (section == null) {
				if (other.section != null)
					return false;
			} else if (!section.equals(other.section))
				return false;
			if (type != other.type)
				return false;
			return true;
		}

		private AssetPackEditor2 getOuterType() {
			return AssetPackEditor2.this;
		}
	}

	public class TypesColumnLabelProvider extends StyledCellLabelProvider {
		@Override
		public void update(ViewerCell cell) {
			Color counterColor = JFaceResources.getColorRegistry().get(JFacePreferences.COUNTER_COLOR);
			Color decorationsColor = JFaceResources.getColorRegistry().get(JFacePreferences.DECORATIONS_COLOR);

			var item = (SectionTypePairModel) cell.getElement();

			int countAssets = item.section.getGroup(item.type).getAssets().size();

			String text;
			StyleRange[] styles;

			String name = item.type.name();
			if (countAssets > 0) {
				text = name + " (" + countAssets + ")";
				int start = name.length();
				int len = text.length() - start;

				var style1 = new StyleRange(0, start, null, null);
				var style2 = new StyleRange(start, len, counterColor, null);
				styles = new StyleRange[] { style1, style2 };
			} else {
				text = name;

				var style = new StyleRange(0, text.length(), decorationsColor, null);
				style.fontStyle = SWT.ITALIC;

				styles = new StyleRange[] { style };
			}

			cell.setStyleRanges(styles);
			cell.setText(text);
			cell.setImage(AssetLabelProvider.GLOBAL_16.getImage(item.type));
		}
	}

	public class TypesComp extends ColumnComp {

		public TypesComp(Composite parent) {
			super(parent, "Types");
			getViewer().setContentProvider(new ITreeContentProvider() {

				@Override
				public boolean hasChildren(Object element) {
					return false;
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
				public Object[] getChildren(Object parentElement) {
					if (parentElement instanceof AssetSectionModel) {
						var section = (AssetSectionModel) parentElement;
						Object[] data = Arrays.stream(AssetType.values()).map(t -> new SectionTypePairModel(section, t))
								.sorted().toArray();
						return data;
					}
					return new Object[] {};
				}
			});

			getViewer().setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					SectionTypePairModel item = (SectionTypePairModel) element;
					return item.type.name();
				}
			});
			var col = new TreeViewerColumn(getViewer(), SWT.NONE);
			col.getColumn().setWidth(1000);

			col.setLabelProvider(new TypesColumnLabelProvider());
		}

		public AssetType getSelectedType() {
			SectionTypePairModel sel = (SectionTypePairModel) getViewer().getStructuredSelection().getFirstElement();
			return sel == null ? null : sel.type;
		}
	}

	public class AssetsComp extends EditableColumnComp {

		public AssetsComp(Composite parent) {
			super(parent, "Assets");
			TreeViewer viewer = getViewer();
			viewer.setLabelProvider(AssetLabelProvider.GLOBAL_16);
			viewer.setContentProvider(new AssetsContentProvider(true) {

				@Override
				public Object[] getChildren(Object parentElement) {

					if (parentElement instanceof SectionTypePairModel) {
						return ((SectionTypePairModel) parentElement).getAssets().toArray();
					}

					return super.getChildren(parentElement);
				}
			});

			ISelectionChangedListener listener = e -> {
				_addBtn.setEnabled(
						getSectionsComp().getSelectedSection() != null && getTypesComp().getSelectedType() != null);
			};
			getSectionsComp().getViewer().addSelectionChangedListener(listener);
			getTypesComp().getViewer().addSelectionChangedListener(listener);

			listener.selectionChanged(null);
		}

		@Override
		protected void onClickedAddButton() {
			if (LicCore.isEvaluationProduct()) {

				IProject project = getEditorInput().getFile().getProject();

				String rule = AssetPackCore.isFreeVersionAllowed(project);
				if (rule != null) {
					LicCore.launchGoPremiumDialogs(rule);
					return;
				}
			}

			try {

				AssetSectionModel section = getSectionsComp().getSelectedSection();
				AssetType initialType = getTypesComp().getSelectedType();

				AssetFactory factory = AssetFactory.getFactory(initialType);

				if (factory != null) {
					AssetType type = factory.getType();
					AssetPackModel pack = getModel();
					String key = pack.createKey(type.name());
					AssetModel asset = factory.createAsset(key, section);
					executeOperation(new AddAssetOperation2(section, asset));
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		@Override
		protected void onClickedDeleteButton() {
			Object[] selection = ((StructuredSelection) getViewer().getSelection()).toArray();
			AssetPackUI.launchDeleteWizard(selection);
		}
	}

	class ArrayAsTreeContentProvider implements ITreeContentProvider {
		private ArrayContentProvider _provider = new ArrayContentProvider();

		@Override
		public Object[] getElements(Object inputElement) {
			return _provider.getElements(inputElement);
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return _provider.getElements(parentElement);
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}

	}

	@Override
	public void createPartControl(Composite parent) {
		_container = new Composite(parent, SWT.NONE);
		_container.setLayout(new FillLayout());

		SashForm mainSash = new SashForm(_container, SWT.NONE);

		_sectionsComp = new SectionsComp(mainSash);

		_typesComp = new TypesComp(mainSash);

		_assetsComp = new AssetsComp(mainSash);

		mainSash.setWeights(new int[] { 1, 1, 1 });

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		_sectionsComp.getViewer().setInput(_model);
		_sectionsComp.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				var section = (AssetSectionModel) event.getStructuredSelection().getFirstElement();
				getTypesComp().getViewer().setInput(section);
			}
		});

		_typesComp.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = event.getStructuredSelection();
				Object input;
				if (selection.isEmpty()) {
					input = new Object[] {};
				} else {
					input = selection.getFirstElement();
				}

				getAssetsComp().getViewer().setInput(input);
			}
		});

		getEditorSite().setSelectionProvider(new ComplexSelectionProvider(_sectionsComp.getViewer(),
				_typesComp.getViewer(), _assetsComp.getViewer()));

	}

	public SectionsComp getSectionsComp() {
		return _sectionsComp;
	}

	public TypesComp getTypesComp() {
		return _typesComp;
	}

	public AssetsComp getAssetsComp() {
		return _assetsComp;
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);

		FileEditorInput fileInput = getEditorInput();
		IFile file = fileInput.getFile();

		try {
			// we create a model copy detached from the AssetCore registry.
			_model = new AssetPackModel(file);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		_model.addPropertyChangeListener(this::propertyChange);

		setPartName(_model.getName());

	}

	private void propertyChange(@SuppressWarnings("unused") PropertyChangeEvent evt) {
		getEditorSite().getShell().getDisplay().asyncExec(() -> {
			firePropertyChange(PROP_DIRTY);
		});
	}

	public AssetPackModel getModel() {
		return _model;
	}

	public IResource getAssetsFolder() {
		return getEditorInput().getFile().getParent();
	}

	@Override
	public FileEditorInput getEditorInput() {
		return (FileEditorInput) super.getEditorInput();
	}

	@Override
	public void setFocus() {
		_container.setFocus();
	}

	void executeOperation(IUndoableOperation op) {
		IOperationHistory history = getEditorSite().getWorkbenchWindow().getWorkbench().getOperationSupport()
				.getOperationHistory();
		try {
			history.execute(op, null, this);
		} catch (ExecutionException e) {
			AssetPackUI.showError(e);
		}
	}

	public void revealElement(Object elem) {
		if (elem == null) {
			return;
		}
		Object reveal = elem instanceof IAssetElementModel ? ((IAssetElementModel) elem).getAsset() : elem;
		TreeViewer viewer;
		if (elem instanceof AssetModel) {
			viewer = getAssetsComp().getViewer();
		} else {
			viewer = getSectionsComp().getViewer();
		}
		viewer.getTree().setFocus();
		viewer.setSelection(new StructuredSelection(reveal), true);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == IPropertySheetPage.class) {
			if (_properties == null) {
				_properties = new PGridPage(true);
			}
			return _properties;
		}
		return super.getAdapter(adapter);
	}
}
