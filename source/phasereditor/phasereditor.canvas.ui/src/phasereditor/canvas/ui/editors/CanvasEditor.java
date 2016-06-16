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

import java.beans.PropertyChangeEvent;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableEditor;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import javafx.geometry.Point2D;
import phasereditor.canvas.core.JSCodeGenerator;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.behaviors.ZoomBehavior;
import phasereditor.canvas.ui.editors.grid.PGrid;
import phasereditor.canvas.ui.editors.operations.ChangeSettingsOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.palette.PaletteComp;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;

/**
 * @author arian
 *
 */
public class CanvasEditor extends EditorPart
		implements IResourceChangeListener, IPersistableEditor, IEditorSharedImages {

	private static final String PALETTE_CONTEXT_ID = "phasereditor.canvas.ui.palettecontext";
	public final static String ID = "phasereditor.canvas.ui.editors.canvas";
	public final static String NODES_CONTEXT_ID = "phasereditor.canvas.ui.nodescontext";
	protected static final String SCENE_CONTEXT_ID = "phasereditor.canvas.ui.scenecontext";

	public static final IUndoContext UNDO_CONTEXT = new IUndoContext() {

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
	private CanvasEditorModel _model;
	private PGrid _grid;
	private SashForm _leftSashForm;
	private FilteredTree _outlineTree;

	private ToolBarManager _toolBarManager;

	private SashForm _mainSashForm;
	private PaletteComp _paletteComp;
	protected IContextActivation _paletteContext;
	private Action _showPaletteAction;
	private Action _showSidePaneAction;

	public CanvasEditor() {
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == CanvasEditor.class) {
			return this;
		}
		return super.getAdapter(adapter);
	}

	@SuppressWarnings("unused")
	public void modelDirtyChanged(PropertyChangeEvent event) {
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			IFileEditorInput input = (IFileEditorInput) getEditorInput();
			JSONObject data = new JSONObject();
			_model.write(data);
			input.getFile().setContents(new ByteArrayInputStream(data.toString(2).getBytes()), true, false, monitor);

			if (getCanvas().getSettingsModel().isGenerateOnSave()) {
				generateCode();
			}

			_model.getWorld().setDirty(false);
			firePropertyChange(PROP_DIRTY);
		} catch (JSONException | CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		IFileEditorInput fileInput = (IFileEditorInput) input;
		IFile file = fileInput.getFile();
		try (InputStream contents = file.getContents();) {
			JSONObject data = new JSONObject(new JSONTokener(contents));
			_model = new CanvasEditorModel();
			_model.read(data);
			_model.getWorld().setFile(file);
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

	@SuppressWarnings("unused")
	@Override
	public void createPartControl(Composite parent1) {
		GridLayout gl_parent1 = new GridLayout(1, false);
		gl_parent1.marginWidth = 0;
		gl_parent1.marginHeight = 0;
		parent1.setLayout(gl_parent1);
		Composite parent = new Composite(parent1, SWT.NONE);
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		_toolBar = new ToolBar(parent, SWT.FLAT | SWT.RIGHT);
		_toolBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		
		GridLayout gl_parent = new GridLayout(1, false);
		gl_parent.verticalSpacing = 0;
		gl_parent.horizontalSpacing = 0;
		gl_parent.marginWidth = 3;
		gl_parent.marginHeight = 3;
		parent.setLayout(gl_parent);

		_mainSashForm = new SashForm(parent, SWT.NONE);
		_mainSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		_leftSashForm = new SashForm(_mainSashForm, SWT.VERTICAL);

		_outlineTree = new FilteredTree(_leftSashForm, SWT.BORDER | SWT.MULTI, new PatternFilter(), true);
		Tree tree = _outlineTree.getViewer().getTree();
		_outlineTree.getViewer().setLabelProvider(new OutlineLabelProvider());
		_outlineTree.getViewer().setContentProvider(new OutlineContentProvider());

		_grid = new PGrid(_leftSashForm, SWT.NONE);
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
	}

	private void afterCreateWidgets() {
		// name

		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);

		initCanvas();

		initMenus();

		initOutline();

		restoreState();

		initContexts();
	}

	private void initContexts() {
		_canvas.addFocusListener(new FocusListener() {

			private IContextActivation _sceneContext;

			@Override
			public void focusLost(FocusEvent e) {
				getContextService().deactivateContext(_sceneContext);
			}

			@Override
			public void focusGained(FocusEvent e) {
				_sceneContext = getContextService().activateContext(SCENE_CONTEXT_ID);
			}
		});

		FocusListener nodesContextHandler = new FocusListener() {

			private IContextActivation _nodesContext;

			@Override
			public void focusLost(FocusEvent e) {
				getContextService().deactivateContext(_nodesContext);
			}

			@Override
			public void focusGained(FocusEvent e) {
				_nodesContext = getContextService().activateContext(NODES_CONTEXT_ID);
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
		createToolbarManager();
	}

	private void initCanvas() {
		_canvas.init(this, _model, _grid, _outlineTree.getViewer(), _paletteComp);
		getEditorSite().setSelectionProvider(_canvas.getSelectionBehavior());
	}

	private void restoreState() {
		if (_state != null) {
			_canvas.getZoomBehavior().setScale(_state.zoomScale);
			_canvas.getZoomBehavior().setTranslate(_state.translate);
			_mainSashForm.setWeights(_state.sashWights);

			if (_state.paletteData != null) {
				_paletteComp.updateFromJSON(new JSONObject(_state.paletteData));
				_showPaletteAction.setChecked(_paletteComp.isPaletteVisible());
			}

			if (!_state.showSidePane) {
				_showSidePaneAction.run();
			}
			_showSidePaneAction.setChecked(_state.showSidePane);
		}
	}

	private void initOutline() {
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

	private void createToolbarManager() {
		_toolBarManager = new ToolBarManager(_toolBar);

		// depth commands

		String[] defs = {

				"riseToTop", "shape_move_front.png",

				"rise", "shape_move_forwards.png",

				"lower", "shape_move_backwards.png",

				"lowerBottom", "shape_move_back.png",

				"-", "-",

				"align.left", "shape_align_left.png",

				"align.right", "shape_align_right.png",

				"align.top", "shape_align_top.png",

				"align.bottom", "shape_align_bottom.png",

				"align.center", "shape_align_center.png",

				"align.middle", "shape_align_middle.png", };

		defsCommands(defs);

		{
			_toolBarManager.add(new Separator());

			_showSidePaneAction = new Action("", SWT.TOGGLE) {
				{
					setImageDescriptor(EditorSharedImages.getImageDescriptor(IMG_APPLICATION_SIDE_TREE));
				}

				@SuppressWarnings("synthetic-access")
				@Override
				public void run() {
					if (_mainSashForm.getMaximizedControl() == _centerComposite) {
						_mainSashForm.setMaximizedControl(null);
					} else {
						_mainSashForm.setMaximizedControl(_centerComposite);
					}
				}
			};
			_showSidePaneAction.setChecked(true);
			_toolBarManager.add(_showSidePaneAction);

			_showPaletteAction = new Action("", SWT.TOGGLE) {
				{
					setChecked(false);
					setImageDescriptor(EditorSharedImages.getImageDescriptor(IMG_PALETTE));
				}

				@Override
				public void run() {
					boolean visible = getPalette().isPaletteVisible();
					getPalette().setPaletteVisble(!visible);
				}
			};
			getPalette().setPaletteVisble(_showPaletteAction.isChecked());
			_toolBarManager.add(_showPaletteAction);
		}

		{
			_toolBarManager.add(new Separator());
			_toolBarManager.add(new Action("Settings", EditorSharedImages.getImageDescriptor(IMG_SETTINGS)) {
				@Override
				public void run() {
					openDialogSettings();
				}
			});
		}
		{
			_toolBarManager.add(new Action("Generate Code", EditorSharedImages.getImageDescriptor(IMG_BUILD)) {
				@Override
				public void run() {
					generateCode();
				}
			});
		}

		_toolBarManager.update(true);
	}

	void generateCode() {
		JSCodeGenerator generator = new JSCodeGenerator();
		try {
			WorldModel model = getCanvas().getWorldModel();
			String fname = model.getClassName() + ".js";
			IFile file = getEditorInputFile().getParent().getFile(new Path(fname));
			String replace = null;

			if (file.exists()) {
				byte[] bytes = Files.readAllBytes(file.getLocation().makeAbsolute().toFile().toPath());
				replace = new String(bytes);
			}

			String content = generator.generate(model, replace);

			ByteArrayInputStream stream = new ByteArrayInputStream(content.getBytes());
			if (file.exists()) {
				file.setContents(stream, IResource.NONE, null);
			} else {
				file.create(stream, false, null);
			}
			file.refreshLocal(1, null);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void defsCommands(String[] defs) {
		for (int i = 0; i < defs.length; i += 2) {
			if (defs[i].equals("-")) {
				_toolBarManager.add(new Separator());
				continue;
			}

			_toolBarManager.add(simpleCommand("phasereditor.canvas.ui." + defs[i], defs[i + 1]));
		}
	}

	private CommandContributionItem simpleCommand(String cmd, String imgname) {
		CommandContributionItemParameter p = new CommandContributionItemParameter(getSite(), null, cmd, SWT.PUSH);
		p.icon = EditorSharedImages.getImageDescriptor("icons/" + imgname);
		return new CommandContributionItem(p);
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

	public TreeViewer getOutline() {
		return _outlineTree.getViewer();
	}

	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		_canvas.getUpdateBehavior().dispose();
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.
	 * eclipse.core.resources.IResourceChangeEvent)
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			if (event.getDelta() == null) {
				return;
			}
			event.getDelta().accept(new IResourceDeltaVisitor() {

				@SuppressWarnings("synthetic-access")
				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					IFile thisFile = getEditorInputFile();
					IResource deltaFile = delta.getResource();
					if (deltaFile.equals(thisFile)) {
						if (delta.getKind() == IResourceDelta.REMOVED) {
							IPath movedTo = delta.getMovedToPath();
							if (movedTo == null) {
								// delete
								Display display = Display.getDefault();
								display.asyncExec(new Runnable() {

									@Override
									public void run() {
										getSite().getPage().closeEditor(CanvasEditor.this, false);
									}
								});

							} else {
								// rename
								IFile file = root.getFile(movedTo);
								CanvasEditor.super.setInput(new FileEditorInput(file));
								getModel().getWorld().setFile(file);
								swtRun(CanvasEditor.this::updateTitle);
							}
						}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	public IFile getEditorInputFile() {
		return ((IFileEditorInput) getEditorInput()).getFile();
	}

	public CanvasEditorModel getModel() {
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
	private ToolBar _toolBar;
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

	void openDialogSettings() {
		SettingsDialog dlg = new SettingsDialog(getSite().getShell());
		JSONObject data = new JSONObject();
		_model.getSettings().write(data);
		SceneSettings settings = new SceneSettings(data);
		dlg.setModel(settings);
		if (dlg.open() == Window.OK) {
			settings.write(data);
			getCanvas().getUpdateBehavior()
					.executeOperations(new CompositeOperation(new ChangeSettingsOperation(data)));
		}
	}
}
