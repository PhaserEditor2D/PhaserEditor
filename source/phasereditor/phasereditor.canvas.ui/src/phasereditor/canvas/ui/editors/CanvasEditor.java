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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.grid.PGrid;

/**
 * @author arian
 *
 */
public class CanvasEditor extends EditorPart implements IResourceChangeListener {

	public final static String ID = "phasereditor.canvas.ui.editors.canvas";

	private ObjectCanvas _canvas;
	private WorldModel _model;

	private PGrid _grid;
	private SashForm _sashForm;
	private FilteredTree _outlineTree;

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

		SashForm sashForm = new SashForm(parent, SWT.NONE);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		_sashForm = new SashForm(sashForm, SWT.VERTICAL);

		_outlineTree = new FilteredTree(_sashForm, SWT.BORDER | SWT.MULTI, new PatternFilter(), true);
		Tree tree = _outlineTree.getViewer().getTree();
		_outlineTree.getViewer().setLabelProvider(new OutlineLabelProvider());
		_outlineTree.getViewer().setContentProvider(new OutlineContentProvider());

		_grid = new PGrid(_sashForm, SWT.NONE);
		_canvas = new ObjectCanvas(sashForm);
		_sashForm.setWeights(new int[] { 1, 1 });
		sashForm.setWeights(new int[] { 3, 4 });

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		// name

		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);

		// canvas

		_canvas.init(_model, _grid, _outlineTree.getViewer());

		// selection

		getEditorSite().setSelectionProvider(_canvas.getSelectionBehavior());

		// menu

		MenuManager manager = createContextMenu();
		getEditorSite().registerContextMenu(manager, _canvas.getSelectionBehavior(), false);

		_canvas.setMenu(manager.createContextMenu(_canvas));
		_outlineTree.getViewer().getControl().setMenu(manager.createContextMenu(_outlineTree));

		// outline

		_outlineTree.getViewer().setInput(_canvas);
		_outlineTree.getViewer().expandAll();
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
}
