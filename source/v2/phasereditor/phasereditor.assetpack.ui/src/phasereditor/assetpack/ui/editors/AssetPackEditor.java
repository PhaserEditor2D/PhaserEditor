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

import static java.lang.System.out;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
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
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
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
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.operations.UndoRedoActionGroup;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;

import phasereditor.assetpack.core.AssetFactory;
import phasereditor.assetpack.core.AssetGroupModel;
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.assetpack.ui.AssetsContentProvider;
import phasereditor.assetpack.ui.editors.operations.AddAssetOperation;
import phasereditor.assetpack.ui.editors.operations.AddSectionOperation;
import phasereditor.lic.LicCore;
import phasereditor.ui.ComplexSelectionProvider;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.FilteredContentOutlinePage;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.PatternFilter2;
import phasereditor.ui.properties.PGridPage;

/**
 * @author arian
 *
 */
public class AssetPackEditor extends EditorPart implements IGotoMarker, IShowInSource {

	public static final String ID = "phasereditor.assetpack.editor";

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

	private static final QualifiedName EDITING_NODE = new QualifiedName("phasereditor.assetpack", "editingNode_v2");

	private AssetPackModel _model;
	private Composite _container;
	private SectionsComp _sectionsComp;
	private TypesComp _typesComp;
	private AssetsComp _assetsComp;

	private AssetPackEditorOutlinePage _outliner;

	public AssetPackEditorOutlinePage getOutliner() {
		return _outliner;
	}

	public void setOutliner(AssetPackEditorOutlinePage outliner) {
		_outliner = outliner;
	}

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

		if (_outliner != null) {
			_outliner.refresh();
		}
	}

	public void saveEditingPoint() {
		AssetPackModel pack = getModel();
		if (pack != null) {
			var sel = (IStructuredSelection) getEditorSite().getSelectionProvider().getSelection();
			Object elem = sel.getFirstElement();
			if (elem != null) {
				IFile file = pack.getFile();
				try {
					file.setPersistentProperty(EDITING_NODE, pack.getStringReference(elem));
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void recoverEditingPoint() {
		AssetPackModel pack = getModel();
		IFile file = pack.getFile();
		try {
			String str = file.getPersistentProperty(EDITING_NODE);
			if (str != null) {
				Object elem = pack.getElementFromStringReference(str);
				revealElement(elem);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void doSaveAs() {
		//
	}

	@Override
	public void dispose() {
		if (_model != null && _model.getFile().exists()) {
			saveEditingPoint();
		}

		super.dispose();
	}

	@Override
	public void gotoMarker(IMarker marker) {
		try {
			String ref = (String) marker.getAttribute(AssetPackCore.ASSET_EDITOR_GOTO_MARKER_ATTR);
			if (ref != null) {
				Object asset = getModel().getElementFromStringReference(ref);
				revealElement(asset);
			}
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);

		registerUndoRedoActions();
	}

	private void registerUndoRedoActions() {
		IEditorSite site = getEditorSite();
		UndoRedoActionGroup group = new UndoRedoActionGroup(site, UNDO_CONTEXT, true);
		group.fillActionBars(site.getActionBars());
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
				FilteredTree tree = new FilteredTree(this, SWT.MULTI, new PatternFilter2(), true);
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
			Object[] selection = ((StructuredSelection) getViewer().getSelection()).toArray();
			AssetPackUI.launchDeleteWizard(selection);
		}

		protected void onClickedRenameButton() {
			AssetPackUI.launchRenameWizard(getViewer().getStructuredSelection().getFirstElement());
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

			TreeViewer viewer = getViewer();
			viewer.setContentProvider(new ITreeContentProvider() {

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
			viewer.setLabelProvider(AssetLabelProvider.GLOBAL_16);

			initDragAndDrop(viewer);
		}

		private void initDragAndDrop(TreeViewer viewer) {
			Transfer[] types = { LocalSelectionTransfer.getTransfer() };
			viewer.addDragSupport(DND.DROP_COPY | DND.DROP_MOVE, types, new DragSourceAdapter() {

				@Override
				public void dragStart(DragSourceEvent event) {
					IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
					LocalSelectionTransfer.getTransfer().setSelection(selection);
				}
			});
			viewer.addDropSupport(DND.DROP_MOVE, types, new ViewerDropAdapter(viewer) {

				private int _location;
				private int _target;

				@Override
				public boolean validateDrop(Object target, int operation, TransferData transferType) {
					return true;
				}

				@Override
				public boolean performDrop(Object data) {
					if (_target == -1) {
						return false;
					}

					Object[] array = ((IStructuredSelection) data).toArray();

					AssetPackModel pack = getModel();
					var moving = List.of(array)

							.stream()

							.filter(e -> e instanceof AssetSectionModel && ((AssetSectionModel) e).getPack() == pack)

							.map(e -> (AssetSectionModel) e).collect(Collectors.toList());

					if (moving.size() != array.length) {
						return false;
					}

					var sections = pack.getSections();

					int index = _target;

					var pivot = sections.get(index);

					pack.removeAllSections(moving, false);

					index = pack.getSections().indexOf(pivot);

					if (index < 0) {
						index = 0;
					}

					if (_location == LOCATION_AFTER) {
						index++;
					}

					pack.addAllSections(index, moving, true);

					AssetPackEditor.this.refresh();

					return true;
				}

				@Override
				public void dragOver(DropTargetEvent event) {
					_location = determineLocation(event);
					var item = (TreeItem) event.item;
					if (item == null) {
						_target = -1;
					} else {
						_target = viewer.getTree().indexOf(item);
					}
					super.dragOver(event);
				}
			});
		}

		public AssetSectionModel getSelectedSection() {
			return (AssetSectionModel) getViewer().getStructuredSelection().getFirstElement();
		}

		@Override
		protected void onClickedAddButton() {
			AssetPackModel model = getModel();
			InputDialog dlg = new InputDialog(getSite().getShell(), "New Section", "Enter the section key:",
					model.createKey("section"), new IInputValidator() {

						@Override
						public String isValid(String newText) {
							return model.hasKey(newText) ? "That key already exists, use other." : null;
						}
					});
			if (dlg.open() == Window.OK) {
				String sectionName = dlg.getValue();
				executeOperation(new AddSectionOperation(sectionName));
			}
		}
	}

	public class TypesColumnLabelProvider extends StyledCellLabelProvider {
		@Override
		public void update(ViewerCell cell) {
			Color counterColor = JFaceResources.getColorRegistry().get(JFacePreferences.COUNTER_COLOR);
			Color decorationsColor = JFaceResources.getColorRegistry().get(JFacePreferences.DECORATIONS_COLOR);

			var item = (AssetGroupModel) cell.getElement();

			int countAssets = item.getAssets().size();

			String text;
			StyleRange[] styles;

			String name = item.getType().name();
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
			cell.setImage(AssetLabelProvider.GLOBAL_16.getImage(item.getType()));
		}
	}

	public class TypesComp extends ColumnComp {

		public TypesComp(Composite parent) {
			super(parent, "File Types");
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
						Object[] data = Arrays.stream(AssetType.values()).map(t -> section.getGroup(t))
								.sorted((a, b) -> -Integer.compare(a.getAssets().size(), b.getAssets().size()))
								.toArray();
						return data;
					}
					return new Object[] {};
				}
			});

			getViewer().setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					var item = (AssetGroupModel) element;
					return item.getType().name();
				}
			});
			var col = new TreeViewerColumn(getViewer(), SWT.NONE);
			col.getColumn().setWidth(1000);

			col.setLabelProvider(new TypesColumnLabelProvider());
		}

		public AssetType getSelectedType() {
			var sel = getSelectedGroup();
			return sel == null ? null : sel.getType();
		}

		public AssetGroupModel getSelectedGroup() {
			return (AssetGroupModel) getViewer().getStructuredSelection().getFirstElement();
		}
	}

	public class AssetsComp extends EditableColumnComp {

		public AssetsComp(Composite parent) {
			super(parent, "Files");
			TreeViewer viewer = getViewer();
			viewer.setLabelProvider(AssetLabelProvider.GLOBAL_16);
			viewer.setContentProvider(new AssetsContentProvider(true));
			AssetPackUI.installAssetTooltips(viewer);

			ISelectionChangedListener listener = e -> {
				_addBtn.setEnabled(
						getSectionsComp().getSelectedSection() != null && getTypesComp().getSelectedType() != null);
			};
			getSectionsComp().getViewer().addSelectionChangedListener(listener);
			getTypesComp().getViewer().addSelectionChangedListener(listener);

			listener.selectionChanged(null);

			initDragAndDrop(viewer);
		}

		private void initDragAndDrop(TreeViewer viewer) {
			Transfer[] types = { LocalSelectionTransfer.getTransfer() };
			viewer.addDragSupport(DND.DROP_COPY | DND.DROP_MOVE, types, new DragSourceAdapter() {

				@Override
				public void dragStart(DragSourceEvent event) {
					IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
					LocalSelectionTransfer.getTransfer().setSelection(selection);
				}
			});
			viewer.addDropSupport(DND.DROP_MOVE, types, new ViewerDropAdapter(viewer) {

				private int _location;
				private int _target;

				@Override
				public boolean validateDrop(Object target, int operation, TransferData transferType) {
					return true;
				}

				@Override
				public boolean performDrop(Object data) {
					if (_target == -1) {
						return false;
					}

					AssetGroupModel selectedGroup = getTypesComp().getSelectedGroup();

					Object[] array = ((IStructuredSelection) data).toArray();

					var moving = List.of(array)

							.stream()

							.filter(e -> e instanceof AssetModel && ((AssetModel) e).getGroup() == selectedGroup)

							.map(e -> (AssetModel) e).collect(Collectors.toList());

					if (moving.size() != array.length) {
						return false;
					}

					var assets = getTypesComp().getSelectedGroup().getAssets();

					int index = _target;

					AssetModel pivot = assets.get(index);

					AssetSectionModel section = getSectionsComp().getSelectedSection();

					section.removeAllAssets(moving, false);

					index = section.getAssets().indexOf(pivot);

					if (index < 0) {
						index = 0;
					}

					if (_location == LOCATION_AFTER) {
						index++;
					}

					section.addAllAssets(index, moving, true);

					AssetPackEditor.this.refresh();

					return true;
				}

				@Override
				public void dragOver(DropTargetEvent event) {
					_location = determineLocation(event);
					var item = (TreeItem) event.item;
					if (item == null) {
						_target = -1;
					} else {
						_target = viewer.getTree().indexOf(item);
					}
					super.dragOver(event);
				}
			});
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
					executeOperation(new AddAssetOperation(section, asset));
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
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

				if (getSectionsComp().getViewer().getTree().isFocusControl()) {
					if (getOutliner() != null) {
						getOutliner().revealAndSelect(event.getStructuredSelection());
					}
				}
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

				if (getTypesComp().getViewer().getTree().isFocusControl()) {
					if (getOutliner() != null) {
						getOutliner().revealAndSelect(event.getStructuredSelection());
					}
				}
			}
		});

		_assetsComp.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (getAssetsComp().getViewer().getTree().isFocusControl()) {
					if (getOutliner() != null) {
						getOutliner().revealAndSelect(event.getStructuredSelection());
					}
				}
			}
		});

		getEditorSite().setSelectionProvider(new ComplexSelectionProvider(_sectionsComp.getViewer(),
				_typesComp.getViewer(), _assetsComp.getViewer()));

		recoverEditingPoint();

		swtRun(this::refresh);

	}

	@Override
	public ShowInContext getShowInContext() {
		return new ShowInContext(getEditorInput(), getEditorSite().getSelectionProvider().getSelection());
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

		List<Object> list = new ArrayList<>();

		if (elem instanceof IAssetKey) {
			var assetKey = (IAssetKey) elem;
			list.add(assetKey.getAsset().getSection());
			list.add(assetKey.getAsset().getGroup());

			if (!(assetKey instanceof AssetModel)) {
				list.add(assetKey.getAsset());
			}

			list.add(assetKey);

			getAssetsComp().getViewer().expandToLevel(assetKey.getAsset(), 1);

		} else if (elem instanceof AssetGroupModel) {
			var group = (AssetGroupModel) elem;
			list.add(group.getSection());
			list.add(group);
		} else {
			list.add(elem);
		}

		var sel = new StructuredSelection(list);

		getSectionsComp().getViewer().setSelection(sel);
		getTypesComp().getViewer().setSelection(sel);
		getAssetsComp().getViewer().setSelection(sel);
	}

	class AssetPackEditorOutlinePage extends FilteredContentOutlinePage {
		private ISelectionChangedListener _listener;

		public AssetPackEditorOutlinePage() {
		}

		public void revealAndSelect(IStructuredSelection selection) {
			TreeViewer viewer = getViewer();

			var iter = selection.iterator();
			while (iter.hasNext()) {
				var elem = iter.next();
				if (elem instanceof AssetGroupModel) {
					viewer.expandToLevel(((AssetGroupModel) elem).getSection(), 1);
				} else if (elem instanceof IAssetKey) {
					AssetModel asset = ((IAssetKey) elem).getAsset();
					AssetSectionModel section = asset.getSection();
					viewer.expandToLevel(section, 1);
					viewer.expandToLevel(asset, 1);
				}
			}

			viewer.setSelection(selection);

		}

		@Override
		public void createControl(Composite parent) {
			super.createControl(parent);

			TreeViewer viewer = getTreeViewer();
			viewer.setLabelProvider(AssetLabelProvider.GLOBAL_16);
			viewer.setContentProvider(new AssetsContentProvider(true));
			viewer.setInput(getModel());
			viewer.addSelectionChangedListener(_listener = new ISelectionChangedListener() {

				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					if (!viewer.getTree().isFocusControl()) {
						return;
					}

					Set<Object> clousure = new HashSet<>();

					for (var elem : event.getStructuredSelection().toList()) {
						clousure.add(elem);
						if (elem instanceof IAssetKey) {
							AssetModel asset = ((IAssetKey) elem).getAsset();
							clousure.addAll(List.of(asset, asset.getGroup(), asset.getSection()));
						} else if (elem instanceof AssetGroupModel) {
							clousure.add(((AssetGroupModel) elem).getSection());
						}
					}

					ISelection sel = new StructuredSelection(clousure.toArray());
					getSectionsComp().getViewer().setSelection(sel);
					getTypesComp().getViewer().setSelection(sel);

					for (var elem : clousure) {
						if (elem instanceof IAssetKey) {
							getAssetsComp().getViewer().expandToLevel(((IAssetKey) elem).getAsset(), 1);
						}
					}

					getAssetsComp().getViewer().setSelection(event.getSelection());
				}
			});
			AssetPackUI.installAssetTooltips(viewer);
			// viewer.getControl().setMenu(getMenuManager().createContextMenu(viewer.getControl()));
		}

		@Override
		public void dispose() {

			getViewer().removeSelectionChangedListener(_listener);

			setOutliner(null);

			super.dispose();
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == IPropertySheetPage.class) {
			out.println("AssetPackEditor: create new propety page.");
			return new PGridPage(true);
		}

		if (adapter == IContentOutlinePage.class) {
			if (_outliner == null) {
				_outliner = new AssetPackEditorOutlinePage();
			}
			return _outliner;
		}

		if (adapter == IContextProvider.class) {
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
					IContext context = HelpSystem.getContext("phasereditor.help.assetpackeditor");
					return context;
				}
			};
		}

		return super.getAdapter(adapter);
	}

	public void updateAssetEditor() {
		// TODO: What to do here? Maybe update all the properties.
	}

	public void handleFileRename(IFile file) {
		_model.setFile(file);
		swtRun(() -> {
			super.setInput(new FileEditorInput(file));
			setPartName(_model.getName());
		});
	}

}
