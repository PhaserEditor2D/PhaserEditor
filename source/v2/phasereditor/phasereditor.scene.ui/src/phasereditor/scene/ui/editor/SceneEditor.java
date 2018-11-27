package phasereditor.scene.ui.editor;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
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
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.operations.UndoRedoActionGroup;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;

import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.SceneCompiler;
import phasereditor.scene.core.SceneModel;
import phasereditor.scene.ui.editor.outline.SceneOutlinePage;
import phasereditor.scene.ui.editor.properties.ScenePropertyPage;
import phasereditor.ui.SelectionProviderImpl;

public class SceneEditor extends EditorPart {

	public static final String ID = "phasereditor.scene.ui.SceneEditor";
	private static String OBJECTS_CONTEXT = "phasereditor.scene.ui.objects";

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
	private boolean _builtFitstTime;
	private IContextActivation _objectsContextActivation;

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

	@Override
	public void doSave(IProgressMonitor monitor) {
		var file = getEditorInput().getFile();

		try {

			_model.save(file, monitor);

			setDirty(false);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		generateCode(monitor);

	}

	void generateCode(IProgressMonitor monitor) {
		try {
			var compiler = new SceneCompiler(getEditorInput().getFile(), getSceneModel());

			compiler.compile(monitor);

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
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);

		_selectionProvider = new SelectionProviderImpl(true);

		site.setSelectionProvider(_selectionProvider);

		registerUndoActions();

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
					
					getViewer().getCanvas().addFocusListener(new FocusListener() {
						
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

	private void registerUndoActions() {
		IEditorSite site = getEditorSite();

		_undoRedoGroup = new UndoRedoActionGroup(site, undoContext, true);

		IActionBars actionBars = site.getActionBars();
		_undoRedoGroup.fillActionBars(actionBars);

		actionBars.updateActionBars();
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

	public void build() {
		_builtFitstTime = true;

		_scene.redraw();

		updatePropertyPagesContentWithSelection();

		refreshOutline();

	}

	boolean isBuiltFirstTime() {
		return _builtFitstTime;
	}

}
