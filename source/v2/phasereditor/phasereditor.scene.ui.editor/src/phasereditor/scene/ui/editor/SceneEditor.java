package phasereditor.scene.ui.editor;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.operations.UndoRedoActionGroup;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;

import phasereditor.lic.LicCore;
import phasereditor.project.core.PhaserProjectBuilder;
import phasereditor.scene.core.GameObjectEditorComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.SceneCore;
import phasereditor.scene.core.SceneModel;
import phasereditor.scene.ui.editor.outline.SceneOutlinePage;
import phasereditor.scene.ui.editor.properties.ScenePropertyPage;
import phasereditor.ui.SelectionProviderImpl;
import phasereditor.ui.editors.EditorFileStampHelper;

public class SceneEditor extends EditorPart {

	public static final String ID = "phasereditor.scene.ui.editor.SceneEditor";
	private static String OBJECTS_CONTEXT = "phasereditor.scene.ui.editor.objects";
	private static String COMMAND_CONTEXT = "phasereditor.scene.ui.editor.command";

	private SceneModel _model;
	private SceneCanvas _scene;
	private SceneOutlinePage _outline;
	private boolean _dirty;
	ISelectionChangedListener _outlinerSelectionListener;
	private List<ScenePropertyPage> _propertyPages;

	public final IUndoContext undoContext = new IUndoContext() {

		@Override
		public boolean matches(IUndoContext context) {
			return context == this;
		}

		@Override
		public String getLabel() {
			return "SCENE_EDITOR_CONTEXT";
		}
	};

	private UndoRedoActionGroup _undoRedoGroup;
	protected SelectionProviderImpl _selectionProvider;
	private IContextActivation _objectsContextActivation;
	private IContextActivation _commandContextActivation;
	private EditorFileStampHelper _fileStampHelper;

	public SceneEditor() {
		_outlinerSelectionListener = new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				_selectionProvider.setAutoFireSelectionChanged(false);
				_selectionProvider.setSelection(event.getSelection());
				_selectionProvider.setAutoFireSelectionChanged(true);

				getScene().redraw();
			}
		};

		_propertyPages = new ArrayList<>();

		_fileStampHelper = new EditorFileStampHelper(this, this::reloadMethod, this::saveMethod);
	}

	private IContextService getContextService() {
		IContextService service = getSite().getWorkbenchWindow().getWorkbench().getService(IContextService.class);
		return service;
	}

	public void activateObjectsContext() {
		_objectsContextActivation = getContextService().activateContext(OBJECTS_CONTEXT);
	}

	public void deactivateObjectsContext() {
		getContextService().deactivateContext(_objectsContextActivation);
	}

	public void activateCommandContext() {
		_commandContextActivation = getContextService().activateContext(COMMAND_CONTEXT);
	}

	public void deactivateCommandContext() {
		getContextService().deactivateContext(_commandContextActivation);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		_fileStampHelper.helpDoSave(monitor);
	}

	public void reloadFile() {
		_fileStampHelper.helpReloadFile();
	}

	private void reloadMethod() {

		var file = getEditorInput().getFile();

		if (!file.exists()) {
			// abort reload, we are in the case of a rename, move or delete.
			return;
		}

		_model = new SceneModel();

		try {

			_model.read(file);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		if (_outline != null) {
			_outline.getViewer().setInput(_model);
		}

		build();

		setSelection(new ArrayList<>());

		setDirty(false);

		try {
			var history = getSite().getWorkbenchWindow().getWorkbench().getOperationSupport().getOperationHistory();
			history.dispose(undoContext, true, true, true);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void build() {
		_scene.redraw();

		updatePropertyPagesContentWithSelection();

		refreshOutline();
	}

	private void saveMethod(IProgressMonitor monitor) {
		var file = getEditorInput().getFile();

		if (LicCore.isEvaluationProduct()) {
			var cause = SceneCore.isFreeVersionAllowed(file.getProject());
			if (cause != null) {
				LicCore.launchGoPremiumDialogs(cause);
				return;
			}
		}

		try {

			_model.save(file, monitor);

			setDirty(false);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		generateCode(monitor);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);

		_selectionProvider = new SelectionProviderImpl(true);

		site.setSelectionProvider(_selectionProvider);

		registerUndoRedoActions();

		IFileEditorInput fileInput = (IFileEditorInput) input;

		setPartName(fileInput.getName());

		_model = new SceneModel();

		var file = fileInput.getFile();

		try {

			_model.read(file);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void handleFileMoved(IFile file) {
		setInput(new FileEditorInput(file));
		setPartName(file.getName());
		firePropertyChange(PROP_TITLE);
	}

	void generateCode(IProgressMonitor monitor) {
		try {

			SceneCore.compileScene(getSceneModel(), getEditorInput().getFile(), monitor);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	public void compile() {
		var job = new WorkspaceJob("Scene compiler.") {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {

				generateCode(monitor);

				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	@Override
	public void doSaveAs() {
		// not supported
	}

	@Override
	public IFileEditorInput getEditorInput() {
		return (IFileEditorInput) super.getEditorInput();
	}

	public IProject getProject() {
		return getEditorInput().getFile().getProject();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		_scene = new SceneCanvas(parent, SWT.NONE);

		_scene.init(this);

		_scene.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				deactivateObjectsContext();
			}

			@Override
			public void focusGained(FocusEvent e) {
				activateObjectsContext();
			}
		});

	}

	@Override
	public void setFocus() {
		_scene.setFocus();
	}

	public SceneModel getSceneModel() {
		return _model;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getAdapter(Class adapter) {

		if (adapter == IPropertySheetPage.class) {
			var page = new ScenePropertyPage(this);
			_propertyPages.add(page);

			return page;
		}

		if (adapter == IContentOutlinePage.class) {
			_outline = new SceneOutlinePage(this) {

				@Override
				public void createControl(Composite parent) {
					super.createControl(parent);

					addSelectionChangedListener(_outlinerSelectionListener);

					getViewer().getTree().addFocusListener(new FocusListener() {

						@Override
						public void focusLost(FocusEvent e) {
							deactivateObjectsContext();
						}

						@Override
						public void focusGained(FocusEvent e) {
							activateObjectsContext();
						}
					});
				}
			};

			return _outline;

		}

		return super.getAdapter(adapter);
	}

	public SceneCanvas getScene() {
		return _scene;
	}

	public void removeOutline() {
		_outline.removeSelectionChangedListener(_outlinerSelectionListener);
		_outline = null;
	}

	public SceneOutlinePage getOutline() {
		return _outline;
	}

	public List<ScenePropertyPage> getPropertyPages() {
		return _propertyPages;
	}

	public void setDirty(boolean dirty) {
		if (dirty != _dirty) {
			_dirty = dirty;
			firePropertyChange(PROP_DIRTY);
		}
	}

	@Override
	public boolean isDirty() {
		return _dirty;
	}

	public void refreshOutline() {
		if (_outline != null) {
			_outline.refresh();
		}
	}

	public void refreshOutline_basedOnId() {
		if (_outline != null) {
			_outline.refresh_basedOnId();
		}
	}

	public void removePropertyPage(ScenePropertyPage page) {
		_propertyPages.remove(page);
	}

	private void registerUndoRedoActions() {
		var site = getEditorSite();

		_undoRedoGroup = new UndoRedoActionGroup(site, undoContext, true);

		var actionBars = site.getActionBars();

		_undoRedoGroup.fillActionBars(actionBars);

		actionBars.updateActionBars();
	}

	public UndoRedoActionGroup getUndoRedoGroup() {
		return _undoRedoGroup;
	}

	public void executeOperation(IUndoableOperation operation) {

		operation.addContext(undoContext);
		IWorkbench workbench = getSite().getWorkbenchWindow().getWorkbench();
		try {
			IOperationHistory history = workbench.getOperationSupport().getOperationHistory();
			history.execute(operation, null, this);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void setSelectionFromIdList(List<String> objectIdList) {
		var models = _model.getDisplayList().findByIds(objectIdList);
		setSelection(models);
	}

	public void setSelection(List<ObjectModel> models) {
		_setSelection(new StructuredSelection(models));
	}

	private void _setSelection(StructuredSelection selection) {
		_selectionProvider.setSelection(selection);

		if (_outline != null) {
			_outline.setSelection_from_external(selection);
		}

		_scene.redraw();
	}

	public void updatePropertyPagesContentWithSelection() {
		for (var page : _propertyPages) {
			page.selectionChanged(this, getEditorSite().getSelectionProvider().getSelection());
		}
	}

	public List<String> getSelectionIdList() {
		return getSelectionList().stream().map(o -> o.getId()).collect(toList());
	}

	public void refreshSelectionBaseOnId() {
		var ids = new ArrayList<String>();

		for (var obj : getSelectionList()) {
			ids.add(obj.getId());
		}

		var models = getSceneModel().getDisplayList().findByIds(ids);

		setSelection(models);
	}

	@SuppressWarnings({ "cast", "rawtypes", "unchecked" })
	public List<ObjectModel> getSelectionList() {
		return (List<ObjectModel>) (List) _selectionProvider.getSelectionList();
	}

	@SuppressWarnings("static-method")
	public boolean isWaitingForProjectBuilders() {
		boolean b = !PhaserProjectBuilder.isStartupFinished();
		return b;
	}

	public void rebuildImageCache() {
		_scene.getModel().getDisplayList().visit(m -> {
			GameObjectEditorComponent.set_gameObjectEditorDirty(m, true);
		});

		_scene.redraw();

		refreshOutline();
	}

	public void openSourceFile() {
		openSourceFile(-1);
	}

	public void openSourceFile(int offset) {
		var file = SceneCore.getSceneSourceCodeFile(getEditorInput().getFile());
		if (file.exists()) {
			try {
				var editor = (TextEditor) IDE.openEditor(getEditorSite().getWorkbenchWindow().getActivePage(), file);

				if (offset != -1) {

					StyledText textWidget = (StyledText) editor.getAdapter(Control.class);

					try {
						textWidget.setCaretOffset(offset);
						var index = textWidget.getLineAtOffset(offset);
						textWidget.setTopIndex(index);
					} catch (IllegalArgumentException e) {
						// protect from index out of bounds
						e.printStackTrace();
					}

				}

			} catch (PartInitException e1) {
				e1.printStackTrace();
				throw new RuntimeException(e1);
			}
		}
	}

}
