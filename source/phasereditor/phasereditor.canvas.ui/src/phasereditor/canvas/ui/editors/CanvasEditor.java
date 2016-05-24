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
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

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
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
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
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.grid.PGrid;
import phasereditor.ui.EditorSharedImages;

/**
 * @author arian
 *
 */
public class CanvasEditor extends EditorPart implements IResourceChangeListener, IPersistableEditor {

	public final static String ID = "phasereditor.canvas.ui.editors.canvas";

	private ObjectCanvas _canvas;
	private WorldModel _model;

	private PGrid _grid;
	private SashForm _leftSashForm;
	private FilteredTree _outlineTree;

	private ToolBarManager _toolBarManager;

	private SashForm _mainSashForm;

	public CanvasEditor() {
	}

	@SuppressWarnings("unused")
	private void modelDirtyChanged(PropertyChangeEvent event) {
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		IFileEditorInput input = (IFileEditorInput) getEditorInput();

		JSONObject json = new JSONObject();
		_model.write(json);

		try {
			input.getFile().setContents(new ByteArrayInputStream(json.toString(2).getBytes()), true, false, monitor);
			_model.setDirty(false);
		} catch (JSONException | CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		IFileEditorInput fileInput = (IFileEditorInput) input;
		try (InputStream contents = fileInput.getFile().getContents()) {
			JSONObject data = new JSONObject(new JSONTokener(contents));

			_model = new WorldModel(data);
			_model.addPropertyChangeListener(WorldModel.PROP_DIRTY, this::modelDirtyChanged);

			swtRun(this::updateTitle);

		} catch (IOException | CoreException e) {
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
		return _model.isDirty();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@SuppressWarnings("unused")
	@Override
	public void createPartControl(Composite parent) {
		GridLayout gl_parent = new GridLayout(1, false);
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

		_composite = new Composite(_mainSashForm, SWT.NONE);
		GridLayout gl_composite = new GridLayout(1, false);
		gl_composite.verticalSpacing = 2;
		gl_composite.marginHeight = 0;
		gl_composite.marginWidth = 0;
		_composite.setLayout(gl_composite);
		_toolBar = new ToolBar(_composite, SWT.FLAT | SWT.RIGHT);
		_toolBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		_canvas = new ObjectCanvas(_composite, SWT.BORDER);
		_canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		_mainSashForm.setWeights(new int[] { 1, 4 });

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		// name

		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);

		// canvas

		_canvas.init(_model, _grid, _outlineTree.getViewer());

		// selection

		getEditorSite().setSelectionProvider(_canvas.getSelectionBehavior());

		createMenuManager();
		createToolbarManager();

		// outline

		_outlineTree.getViewer().setInput(_canvas);
		_outlineTree.getViewer().expandAll();

		// restore state
		if (_state != null) {
			_canvas.getZoomBehavior().setScale(_state.zoomScale);
			_canvas.getZoomBehavior().setTranslate(_state.translate);
			_mainSashForm.setWeights(_state.sashWights);
		}
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

		// XY commands

		_toolBarManager.update(true);
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

	public ObjectCanvas getCanvas() {
		return _canvas;
	}

	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
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
								setInput(new FileEditorInput(root.getFile(movedTo)));
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
		int[] weights = _mainSashForm.getWeights();
		memento.putString("canvas.sash.weights", Arrays.toString(weights));
	}

	static class State {
		double zoomScale = 0;
		Point2D translate = new Point2D(0, 0);
		int[] sashWights = new int[] { 1, 5 };
	}

	private State _state;
	private ToolBar _toolBar;
	private Composite _composite;

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
				_state.translate = new Point2D(x == null ? 0 : x.doubleValue(), y == null ? 0 : y.doubleValue());
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
