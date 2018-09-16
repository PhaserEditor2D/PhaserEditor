// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.ui.editors;

import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.io.InputStream;
import java.util.Arrays;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPersistableEditor;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.operations.UndoRedoActionGroup;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import javafx.geometry.Point2D;
import phasereditor.canvas.core.CanvasCore;
import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.behaviors.ZoomBehavior;
import phasereditor.canvas.ui.editors.config.CanvasSettingsComp;
import phasereditor.canvas.ui.editors.grid.CanvasPGrid;
import phasereditor.canvas.ui.editors.outline.CanvasOutline;
import phasereditor.canvas.ui.editors.palette.PaletteComp;
import phasereditor.canvas.ui.editors.properties.CanvasPropertiesPage;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.PatternFilter2;

/**
 * @author arian
 *
 */
public class CanvasEditor extends MultiPageEditorPart
		implements IPersistableEditor, IEditorSharedImages, IGotoMarker, IPageChangedListener {

	private static final String PALETTE_CONTEXT_ID = "phasereditor.canvas.ui.palettecontext";
	public final static String ID = "phasereditor.canvas.ui.editors.canvas";
	public final static String NODES_CONTEXT_ID = "phasereditor.canvas.ui.nodescontext";
	public static final String SCENE_CONTEXT_ID = "phasereditor.canvas.ui.scenecontext";
	public static final String EDITOR_CONTEXT_ID = "phasereditor.canvas.ui.any";

	public final IUndoContext undoContext = new IUndoContext() {

		@Override
		public boolean matches(IUndoContext context) {
			return context == this;
		}

		@Override
		public String getLabel() {
			return "CANVAS_CONTEXT";
		}
	};

	private ObjectCanvas _canvas;
	private CanvasModel _model;
	private CanvasPGrid _grid;
	private SashForm _leftSashForm;
	private FilteredTree _outlineTree;

	private SashForm _mainSashForm;
	private PaletteComp _paletteComp;
	protected IContextActivation _paletteContext;
	private CanvasSettingsComp _settingsPage;
	private Control _designPage;
	private ObjectCanvas2 _canvas2;

	private UndoRedoActionGroup _undoRedoGroup;
	private CanvasOutline _outline2;

	public CanvasEditor() {
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == CanvasEditor.class) {
			return this;
		}

		if (adapter == IPropertySheetPage.class) {
			return new CanvasPropertiesPage();
		}

		if (adapter == IContentOutlinePage.class) {
			if (_outline2 == null) {
				_outline2 = new CanvasOutline(this);
			}

			return _outline2;
		}

		return super.getAdapter(adapter);
	}

	public CanvasOutline getOutline2() {
		return _outline2;
	}

	public void setOutline2(CanvasOutline outline2) {
		_outline2 = outline2;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		saveCanvas(monitor);
	}

	public void setDirty(boolean dirty) {
		_model.getWorld().setDirty(dirty);
		firePropertyChange(PROP_DIRTY);
	}

	/**
	 * @param monitor
	 */
	private void saveCanvas(IProgressMonitor monitor) {
		boolean hasErrors = _model.getWorld().hasErrors();

		if (hasErrors) {
			if (!MessageDialog.openQuestion(getSite().getShell(), "Canvas",
					"The scene has errors, do you want to save?")) {
				return;
			}
		}

		// save canvas

		try {
			IFileEditorInput input = (IFileEditorInput) getEditorInput();
			IFile file = input.getFile();

			_model.save(file, monitor);

			if (getCanvas().getSettingsModel().isGenerateOnSave()) {
				generateCode();
			}

			setDirty(false);
		} catch (JSONException | CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public CanvasSettingsComp getSettingsPage() {
		return _settingsPage;
	}

	public Control getDesignPage() {
		return _designPage;
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		IFileEditorInput fileInput = (IFileEditorInput) input;
		IFile file = fileInput.getFile();
		try (InputStream contents = file.getContents();) {
			JSONObject data = new JSONObject(new JSONTokener(contents));
			_model = new CanvasModel(file);
			try {
				_model.read(data);
			} catch (Exception e) {
				e.printStackTrace();
				_model = new CanvasModel(file);
				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						Shell shell = Display.getDefault().getActiveShell();
						MessageDialog.openError(shell, "Error", "The scene data cannot ve loaded.\n" + e.getMessage());
					}
				});
			}
			_model.getWorld().addPropertyChangeListener(WorldModel.PROP_STRUCTURE, arg -> {
				firePropertyChange(PROP_DIRTY);
			});
			swtRun(this::updateTitle);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public void doSaveAs() {
		// nothing
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public boolean isDirty() {
		return _model.getWorld().isDirty();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	protected void createPages() {
		createDesignPage();
		createSettingsPage();
		createNewDesignPage();

		addEditorActivationListeners();
		addPageChangedListener(this);
	}

	private IPartListener _partListener;
	protected IContextActivation _sceneContext;
	protected IContextActivation _nodesContext;

	@SuppressWarnings("synthetic-access")
	private void addEditorActivationListeners() {
		_partListener = new IPartListener() {

			@Override
			public void partOpened(IWorkbenchPart part) {
				if (part == CanvasEditor.this) {
					registerCanvasGlobalActions();
				}
			}

			@Override
			public void partDeactivated(IWorkbenchPart part) {
				//
			}

			@Override
			public void partClosed(IWorkbenchPart part) {
				//
			}

			@Override
			public void partBroughtToTop(IWorkbenchPart part) {
				if (part == CanvasEditor.this) {
					registerCanvasGlobalActions();
				}
			}

			@Override
			public void partActivated(IWorkbenchPart part) {
				if (part == CanvasEditor.this) {
					registerCanvasGlobalActions();
				}
			}
		};
		getSite().getWorkbenchWindow().getActivePage().addPartListener(_partListener);
	}

	private void createSettingsPage() {
		_settingsPage = new CanvasSettingsComp(getContainer(), SWT.BORDER);
		_settingsPage.setModel(_model);
		_settingsPage.setOnChanged(() -> {
			setDirty(true);
		});
		_settingsPage.setCanvas(_canvas);
		int i = addPage(_settingsPage);
		setPageText(i, "Configuration");
		setPageImage(i, EditorSharedImages.getImage(IEditorSharedImages.IMG_SETTINGS));
	}

	private void createDesignPage() {
		_canvas2 = new ObjectCanvas2(getContainer(), SWT.NONE);
		int i = addPage(_canvas2);
		setPageText(i, "Design 2");
		setPageImage(i, getTitleImage());
	}

	private void createNewDesignPage() {
		_designPage = createCanvasPartControl(getContainer());
		int i = addPage(_designPage);
		setPageText(i, "Design");
		setPageImage(i, getTitleImage());
	}

	private void registerCanvasGlobalActions() {
		IEditorSite site = getEditorSite();

		if (_undoRedoGroup == null) {
			_undoRedoGroup = new UndoRedoActionGroup(site, undoContext, true);
		}

		IActionBars actionBars = site.getActionBars();
		_undoRedoGroup.fillActionBars(actionBars);

		actionBars.updateActionBars();
	}

	private void disposeCanvasGlobalActions() {
		if (_undoRedoGroup != null) {
			_undoRedoGroup.dispose();
			IActionBars actionBars = getEditorSite().getActionBars();
			actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), null);
			actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), null);
			actionBars.updateActionBars();
		}
	}

	/**
	 * @param parent1
	 * @return
	 */
	@SuppressWarnings("unused")
	private Control createCanvasPartControl(Composite parent1) {
		GridLayout gl_parent1 = new GridLayout(1, false);
		gl_parent1.marginWidth = 2;
		gl_parent1.marginHeight = 2;
		parent1.setLayout(gl_parent1);

		_mainSashForm = new SashForm(parent1, SWT.NONE);
		_mainSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		_leftSashForm = new SashForm(_mainSashForm, SWT.VERTICAL);

		_outlineTree = new FilteredTree(_leftSashForm, SWT.BORDER | SWT.MULTI, new PatternFilter2(), true);
		Tree tree = _outlineTree.getViewer().getTree();
		_outlineTree.getViewer().setLabelProvider(new OutlineLabelProvider());
		_outlineTree.getViewer().setContentProvider(new OutlineContentProvider());

		_grid = new CanvasPGrid(_leftSashForm, SWT.NONE);
		_leftSashForm.setWeights(new int[] { 1, 1 });

		_centerComposite = new Composite(_mainSashForm, SWT.NONE);
		GridLayout gl_composite = new GridLayout(2, false);
		gl_composite.marginHeight = 0;
		gl_composite.marginWidth = 0;
		gl_composite.horizontalSpacing = 0;
		gl_composite.verticalSpacing = 2;
		_centerComposite.setLayout(gl_composite);
		_canvas = new ObjectCanvas(_centerComposite, SWT.BORDER);
		_canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		_paletteComp = new PaletteComp(_centerComposite, SWT.NONE);
		GridData gd_paletteComp = new GridData(SWT.RIGHT, SWT.FILL, false, true, 1, 1);
		gd_paletteComp.widthHint = 80;
		_paletteComp.setLayoutData(gd_paletteComp);

		_mainSashForm.setWeights(new int[] { 1, 4 });

		afterCreateWidgets();

		return _mainSashForm;
	}

	private void afterCreateWidgets() {
		// name

		// ResourcesPlugin.getWorkspace().addResourceChangeListener(this);

		initPalette();

		initCanvas();

		initMenus();

		initOutline();

		restoreState();

		initContexts();

		initPGrid();

		// addEditorActivationListeners();
	}

	private void initPGrid() {
		_grid.setCanvas(_canvas);
	}

	private void initPalette() {
		_paletteComp.setProject(getEditorInputFile().getProject());
	}

	private void initContexts() {
		getContextService().activateContext(EDITOR_CONTEXT_ID);

		_canvas.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				getContextService().deactivateContext(_sceneContext);
			}

			@Override
			public void focusGained(FocusEvent e) {
				activateSceneContext();
			}
		});

		FocusListener nodesContextHandler = new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				deactivateNodesContext();
			}

			@Override
			public void focusGained(FocusEvent e) {
				activateNodesContext();
			}
		};
		_canvas.addFocusListener(nodesContextHandler);
		_outlineTree.getViewer().getControl().addFocusListener(nodesContextHandler);

		// palette

		_paletteComp.getViewer().getTable().addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				getContextService().deactivateContext(_paletteContext);
			}

			@Override
			public void focusGained(FocusEvent e) {
				_paletteContext = getContextService().activateContext(PALETTE_CONTEXT_ID);
			}
		});
	}

	private void initMenus() {
		createMenuManager();
	}

	private void initCanvas() {
		_canvas.init(this, _model, _grid, _outlineTree.getViewer(), _paletteComp);

		_canvas2.init(this, _model);

		getEditorSite().setSelectionProvider(_canvas.getSelectionBehavior());
	}

	private void restoreState() {
		if (_state != null) {
			_canvas.getZoomBehavior().setScale(_state.zoomScale);
			_canvas.getZoomBehavior().setTranslate(_state.translate);
			_mainSashForm.setWeights(_state.sashWights);

			if (_state.paletteData != null) {
				_paletteComp.updateFromJSON(new JSONObject(_state.paletteData), getEditorInputFile().getProject());
				// _showPaletteAction.setChecked(_paletteComp.isPaletteVisible());
			}

			if (!_state.showSidePane) {
				// _showSidePaneAction.run();
			}
			// _showSidePaneAction.setChecked(_state.showSidePane);
		}
	}

	private void initOutline() {
		if (getModel().getType() == CanvasType.SPRITE) {
			_outlineTree.setVisible(false);
		}

		TreeViewer viewer = _outlineTree.getViewer();

		viewer.setInput(_canvas);

		viewer.expandAll();

		int operations = DND.DROP_DEFAULT | DND.DROP_MOVE;
		Transfer[] transfers = new Transfer[] { LocalSelectionTransfer.getTransfer() };
		viewer.addDragSupport(operations, transfers, new DragSourceListener() {

			private ISelection _data;

			@Override
			public void dragStart(DragSourceEvent event) {
				_data = viewer.getSelection();
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				event.data = _data;
				LocalSelectionTransfer.getTransfer().setSelection(_data);
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
				// finished
			}
		});

		viewer.addDropSupport(operations, transfers, new OutlineDropAdapter(this));

	}

	public IContextService getContextService() {
		IContextService service = getSite().getService(IContextService.class);
		return service;
	}

	private void createMenuManager() {
		MenuManager manager = createContextMenu();
		getEditorSite().registerContextMenu(manager, _canvas.getSelectionBehavior(), false);

		_canvas.setMenu(manager.createContextMenu(_canvas));
		_outlineTree.getViewer().getControl().setMenu(manager.createContextMenu(_outlineTree));
	}

	public IFile getFileToGenerate() {
		IFile canvasFile = getEditorInputFile();

		String fname = canvasFile.getFullPath().removeFileExtension()
				.addFileExtension(getCanvas().getSettingsModel().getLang().getExtension()).lastSegment();

		return canvasFile.getParent().getFile(new Path(fname));
	}

	public void generateCode() {

		if (_model.getWorld().hasErrors()) {
			MessageDialog.openWarning(getSite().getShell(), "Canvas",
					"The scene has errors, the JavaScript code generation is aborted.");
			return;
		}

		CanvasCore.compile(_model, null);
	}

	private static MenuManager createContextMenu() {
		MenuManager menuManager = new MenuManager();
		menuManager.add(new GroupMarker("object"));
		return menuManager;
	}

	@Override
	public void setFocus() {
		_canvas.setFocus();
	}

	public PaletteComp getPalette() {
		return _paletteComp;
	}

	public ObjectCanvas getCanvas() {
		return _canvas;
	}

	public ObjectCanvas2 getCanvas2() {
		return _canvas2;
	}

	public TreeViewer getOutline() {
		return _outlineTree.getViewer();
	}

	public CanvasPGrid getPropertyGrid() {
		return _grid;
	}

	@Override
	public void dispose() {
		if (_canvas != null) {
			_canvas.getUpdateBehavior().dispose();
		}
		super.dispose();
	}

	public IFile getEditorInputFile() {
		return ((IFileEditorInput) getEditorInput()).getFile();
	}

	public CanvasModel getModel() {
		return _model;
	}

	protected void updateTitle() {
		setPartName(getEditorInputFile().getName());
		firePropertyChange(PROP_TITLE);
	}

	@Override
	public void saveState(IMemento memento) {
		ZoomBehavior zoom = _canvas.getZoomBehavior();
		memento.putFloat("canvas.zoom.scale", (float) zoom.getScale());
		memento.putFloat("canvas.translate.x", (float) zoom.getTranslate().getX());
		memento.putFloat("canvas.translate.y", (float) zoom.getTranslate().getY());
		memento.putString("canvas.palette.data", _paletteComp.toJSON().toString());
		int[] weights = _mainSashForm.getWeights();
		memento.putString("canvas.sash.weights", Arrays.toString(weights));
		memento.putBoolean("canvas.sidepane.show", _mainSashForm.getMaximizedControl() == null);
	}

	static class State {
		double zoomScale = 0;
		Point2D translate = new Point2D(0, 0);
		int[] sashWights = new int[] { 1, 5 };
		String paletteData;
		boolean showSidePane;
	}

	private State _state;
	private Composite _centerComposite;

	@Override
	public void restoreState(IMemento memento) {
		try {
			_state = new State();

			{
				Float scale = memento.getFloat("canvas.zoom.scale");
				_state.zoomScale = scale == null ? 0 : scale.doubleValue();
			}

			{
				Float x = memento.getFloat("canvas.translate.x");
				Float y = memento.getFloat("canvas.translate.y");
				_state.translate = new Point2D(x == null ? 50 : x.doubleValue(), y == null ? 50 : y.doubleValue());
			}

			{
				String value = memento.getString("canvas.sash.weights");
				if (value != null) {
					value = value.replace("[", "").replace("]", "");
					String[] array = value.split(",");
					_state.sashWights = new int[array.length];
					for (int i = 0; i < array.length; i++) {
						_state.sashWights[i] = Integer.parseInt(array[i].trim());
					}
				}
			}

			{
				_state.paletteData = memento.getString("canvas.palette.data");
			}

			{
				Boolean b = memento.getBoolean("canvas.sidepane.show");
				_state.showSidePane = b != null && b.booleanValue();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void gotoMarker(IMarker marker) {
		try {
			String id = (String) marker.getAttribute(CanvasCore.GOTO_MARKER_OBJECT_ID_ATTR);
			if (id != null) {
				BaseObjectControl<?> control = _canvas.getWorldNode().getControl().findById(id);
				if (control != null) {
					_canvas.getSelectionBehavior().setSelectionAndRevealInScene(control.getIObjectNode());
				}
			}
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	public void handleFileRename(IFile newFile) {
		super.setInput(new FileEditorInput(newFile));
		_model.setFile(newFile);
		updateTitle();
	}

	public void handleFileDelete() {
		getEditorSite().getWorkbenchWindow().getActivePage().closeEditor(this, false);
	}

	public void toggleSidePanel() {
		if (_mainSashForm.getMaximizedControl() == _centerComposite) {
			_mainSashForm.setMaximizedControl(null);
		} else {
			_mainSashForm.setMaximizedControl(_centerComposite);
		}
	}

	public void togglePalette() {
		boolean visible = getPalette().isPaletteVisible();
		getPalette().setPaletteVisble(!visible);
	}

	public boolean isDesignPageActive() {
		return getSelectedPage() == _designPage;
	}

	public boolean isSettingsPageActive() {
		return getSelectedPage() == _designPage;
	}

	@Override
	public void pageChanged(PageChangedEvent event) {
		if (getActivePage() == 0) {
			registerCanvasGlobalActions();
			_canvas.getUpdateBehavior().updateFromSettings();
		} else {
			disposeCanvasGlobalActions();
		}
	}

	public void activateSceneContext() {
		_sceneContext = getContextService().activateContext(SCENE_CONTEXT_ID);
	}

	public void deactivateSceneContext() {
		if (_sceneContext != null) {
			getContextService().deactivateContext(_sceneContext);
		}
	}

	public void deactivateNodesContext() {
		if (_nodesContext != null) {
			getContextService().deactivateContext(_nodesContext);
		}
	}

	public void activateNodesContext() {
		_nodesContext = getContextService().activateContext(NODES_CONTEXT_ID);
	}

	public boolean isContextActive(String contextId) {
		return getContextService().getActiveContextIds().contains(contextId);
	}
}
