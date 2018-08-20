// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.atlas.ui.editor;

import static java.lang.System.out;
import static phasereditor.ui.PhaserEditorUI.eclipseFileToJavaPath;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.wb.swt.SWTResourceManager;
import org.json.JSONObject;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData.Region;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.utils.Array;

import phasereditor.atlas.core.SettingsBean;
import phasereditor.atlas.ui.AtlasCanvas;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.FilteredTreeCanvasContentOutlinePage;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.IconTreeCanvasItemRenderer;
import phasereditor.ui.ImageTreeCanvasItemRenderer;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;
import phasereditor.ui.TreeCanvasViewer;
import phasereditor.ui.properties.PGridPage;

public class TexturePackerEditor extends EditorPart implements IEditorSharedImages, IResourceChangeListener {

	public static final String ID = "phasereditor.atlas.ui.editor.TexturePackerEditor"; //$NON-NLS-1$

	protected TexturePackerEditorModel _model;
	private Composite _container;
	private boolean _dirty;
	TabFolder _tabsFolder;
	private List<IFile> _guessLastOutputFiles;
	TexturePackerContentOutlinePage _outliner;

	private ISelectionProvider _selectionProvider;

	private PGridPage _properties;

	private MenuManager _menuManager;

	private AtlasCanvas _canvasClicked;

	ISelectionChangedListener _outlinerSelectionListener;

	public TexturePackerEditor() {
		_guessLastOutputFiles = new ArrayList<>();
	}

	@Override
	public void createPartControl(Composite parent) {
		_container = new Composite(parent, SWT.NONE);
		_container.setLayout(new FillLayout());

		_tabsFolder = new TabFolder(_container, SWT.BOTTOM);
		_tabsFolder.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));

		_tabsFolder.setSelection(0);

		afterCreateWidgets();
	}

	protected void handleTableKey(KeyEvent e) {
		if (e.character == SWT.DEL) {
			deleteSelection();
		}
	}

	public void deleteSelection() {
		if (_outliner != null) {
			Object[] sel = ((IStructuredSelection) _outliner.getSelection()).toArray();
			if (sel.length > 0) {
				List<IFile> toRemove = new ArrayList<>();
				for (Object item : sel) {
					TexturePackerEditorFrame frame = (TexturePackerEditorFrame) item;
					IFile file = findFile(frame);
					toRemove.add(file);
				}

				_model.getImageFiles().removeAll(toRemove);

				build(true);
			}
		}
	}

	IFile findFile(TexturePackerEditorFrame frame) {
		String regionName = frame.getRegionFilename();

		for (IFile file : _model.getImageFiles()) {
			String location = file.getLocation().toPortableString();
			if (location.startsWith(regionName + ".")) {
				return file;
			}
		}

		return null;
	}

	public AtlasCanvas getAtlasCanvas(int i) {
		return (AtlasCanvas) _tabsFolder.getItem(i).getControl();
	}

	public TexturePackerContentOutlinePage getOutliner() {
		return _outliner;
	}

	public MenuManager getMenuManager() {
		return _menuManager;
	}

	private void afterCreateWidgets() {
		{
			int options = DND.DROP_MOVE | DND.DROP_DEFAULT;
			DropTarget target = new DropTarget(_container, options);
			Transfer[] types = { LocalSelectionTransfer.getTransfer() };
			target.setTransfer(types);
			target.addDropListener(new DropTargetAdapter() {
				@Override
				public void drop(DropTargetEvent event) {
					if (event.data instanceof ISelection) {
						selectionDropped((ISelection) event.data);
					}
				}
			});
		}

		// menu

		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);

		_tabsFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = _tabsFolder.getSelectionIndex();
				repaintTab(index);
			}

		});

		_selectionProvider = new ISelectionProvider() {

			private ListenerList<ISelectionChangedListener> _list = new ListenerList<>();
			private ISelection _selection;

			@Override
			public void setSelection(ISelection selection) {
				_selection = selection;
				SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
				for (ISelectionChangedListener l : _list) {
					l.selectionChanged(event);
				}
			}

			@Override
			public void removeSelectionChangedListener(ISelectionChangedListener listener) {
				_list.remove(listener);
			}

			@Override
			public ISelection getSelection() {
				return _selection;
			}

			@Override
			public void addSelectionChangedListener(ISelectionChangedListener listener) {
				_list.add(listener);
			}
		};
		getEditorSite().setSelectionProvider(_selectionProvider);

		createMenu();

		// maybe we should build if we set a preference for that
		// build(false);
		if (_model != null) {
			updateUIFromModel();
		}

	}

	private void createMenu() {
		_selectionProvider.setSelection(StructuredSelection.EMPTY);

		_menuManager = new MenuManager();

		getEditorSite().registerContextMenu(_menuManager, _selectionProvider, false);
	}

	class AtlasEditorSelectionProvider implements ISelectionProvider {

		private ListenerList<ISelectionChangedListener> _listeners = new ListenerList<>();
		private ISelection _selection;

		@Override
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
			_listeners.add(listener);
		}

		@Override
		public ISelection getSelection() {
			return _selection;
		}

		@Override
		public void removeSelectionChangedListener(ISelectionChangedListener listener) {
			_listeners.remove(listener);
		}

		@Override
		public void setSelection(ISelection selection) {
			_selection = selection;
			SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
			for (ISelectionChangedListener l : _listeners) {
				l.selectionChanged(event);
			}
		}
	}

	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);

		if (_outliner != null) {
			_outliner.removeSelectionChangedListener(_outlinerSelectionListener);
		}

		try {
			if (_model != null) {
				for (EditorPage page : _model.getPages()) {
					page.dispose();
				}
			}
		} catch (SWTException e) {
			// nothing
		}

		super.dispose();
	}

	protected void selectionDropped(ISelection data) {
		if (data instanceof IStructuredSelection) {
			Object[] elems = ((IStructuredSelection) data).toArray();
			List<IFile> files = new ArrayList<>();

			for (Object e : elems) {
				if (e instanceof IFile) {
					files.add((IFile) e);
				} else if (e instanceof IContainer) {
					addTree((IContainer) e, files);
				}
			}

			if (!files.isEmpty()) {
				_model.addImageFiles(files);

				build(true);
			}
		}
	}

	private static void addTree(IContainer e, List<IFile> files) {
		try {
			e.accept(new IResourceVisitor() {

				@Override
				public boolean visit(IResource resource) throws CoreException {
					if (resource instanceof IFile) {
						files.add((IFile) resource);
					}
					return true;
				}
			});
		} catch (CoreException e1) {
			throw new RuntimeException(e1);
		}
	}

	public void dirtify() {
		_dirty = true;
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);

		IFile file = ((IFileEditorInput) input).getFile();
		try {
			boolean hasUI = _tabsFolder != null;
			if (_model == null) {
				_model = new TexturePackerEditorModel(this, file);

				if (hasUI) {
					updateUIFromModel();
				}
			} else {
				_model.setFile(file);
			}
			setPartName(file.getName());

		} catch (IOException | CoreException e) {
			throw new RuntimeException(e);
		}

	}

	private void build(boolean dirty) {
		build(dirty, null);
	}

	void build(boolean dirty, Runnable whenDone) {

		if (_model == null) {
			return;
		}

		_guessLastOutputFiles = _model.guessOutputFiles();

		if (dirty) {
			dirtify();
		}

		Job job = new Job("Build atlas '" + _model.getFile().getName() + "'") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Building atlas '" + _model.getFile().getName() + "'", 2);

				// build new atlas
				SettingsBean settings = _model.getSettings();

				TexturePacker packer;
				try {
					packer = new TexturePacker(settings);
				} catch (Exception e) {
					swtRun(() -> {
						MessageDialog.openError(getEditorSite().getShell(), "Packer Error", e.getMessage());
					});

					e.printStackTrace();

					return Status.OK_STATUS;
				}

				List<IFile> missingFiles = new ArrayList<>();
				List<IFile> oversizedFiles = new ArrayList<>();

				for (IFile wsFile : _model.getImageFiles()) {
					File file = eclipseFileToJavaPath(wsFile).toFile();
					if (file.exists() && file.isFile()) {

						try {
							int[] size = PhaserEditorUI.getImageSize(file);
							int maxWidth = getModel().getSettings().maxWidth;
							int maxHeight = getModel().getSettings().maxHeight;

							if (size[0] > maxWidth || size[1] > maxHeight) {
								oversizedFiles.add(wsFile);
							} else {
								packer.addImage(file);
							}

						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						missingFiles.add(wsFile);
					}
				}

				if (missingFiles.size() + oversizedFiles.size() > 0) {

					StringBuilder sb = new StringBuilder();

					sb.append(
							"There are some problems with the source files.\nDo you want to skip them and continue?\n");

					if (!missingFiles.isEmpty()) {
						sb.append("\nMissing files:\n");
						for (var file : missingFiles) {
							sb.append(file.getName() + " ");
						}
					}

					if (!oversizedFiles.isEmpty()) {
						sb.append("\nFiles above maximum size:\n");
						for (var file : oversizedFiles) {
							sb.append(file.getName() + " ");
						}
					}

					Shell shell = getSite().getShell();
					AtomicBoolean confirm = new AtomicBoolean(false);

					shell.getDisplay().syncExec(new Runnable() {

						@Override
						public void run() {
							if (MessageDialog.openConfirm(shell, "Atlas Build", sb.toString())) {
								confirm.set(true);
							}
						}
					});
					if (!confirm.get()) {
						return Status.CANCEL_STATUS;
					}

					_model.getImageFiles().removeAll(missingFiles);
				}

				monitor.worked(1);
				File tempDir = null;
				try {
					// create atlas files in temporal folder
					tempDir = Files.createTempDirectory("PhaserEditor_Atlas_").toFile();
					String atlasName = _model.getAtlasName();
					packer.pack(tempDir, atlasName);

					// read generated atlas file

					File libgdxAtlasFile = new File(tempDir, atlasName + ".atlas");

					out.println("Temporal atlas file " + libgdxAtlasFile);

					// out.println(new String(Files.readAllBytes(libgdxAtlasFile.toPath())));

					TextureAtlasData data = new TextureAtlasData(new FileHandle(libgdxAtlasFile),
							new FileHandle(tempDir), false);

					// create result model

					List<EditorPage> oldEditorPages = _model.getPages();

					List<EditorPage> newEditorPages = new ArrayList<>();

					ImageLoader loader = new ImageLoader();

					Array<Region> regions = data.getRegions();

					for (TextureAtlasData.Page packerPage : data.getPages()) {
						File textureFile = packerPage.textureFile.file();

						ImageData[] imgData = loader.load(textureFile.getAbsolutePath());
						Image img = new Image(Display.getDefault(), imgData[0]);

						int index = newEditorPages.size();
						EditorPage newEditorPage = new EditorPage(_model, index);

						newEditorPage.setImage(img);

						for (Region region : regions) {
							if (region.page == packerPage) {

								String regionFilename = region.name;
								if (region.index != -1) {
									regionFilename += "_" + region.index;
								}

								TexturePackerEditorFrame frame = new TexturePackerEditorFrame(regionFilename,
										region.index);

								frame.setName(PhaserEditorUI.getNameFromFilename(regionFilename));
								frame.setFrameX(region.left);
								frame.setFrameY(region.top);
								frame.setFrameW(region.width);
								frame.setFrameH(region.height);

								// todo: only if trimmed!
								frame.setSpriteX((int) (region.offsetX));

								// this happens when white spaces are stripped!
								if (region.offsetY != 0) {
									// LibGDX uses the OpenGL Y order (from
									// bottom to top)
									frame.setSpriteY((int) (region.originalHeight - region.offsetY - region.height));
								}

								frame.setSpriteW(region.width);
								frame.setSpriteH(region.height);

								frame.setSourceW(region.originalWidth);
								frame.setSourceH(region.originalHeight);

								newEditorPage.add(frame);
							}

						}

						if (settings.useIndexes) {
							newEditorPage.sortByIndexes();
						}

						newEditorPages.add(newEditorPage);
					}

					_model.setPages(newEditorPages);

					for (EditorPage page : _model.getPages()) {
						String atlasImageName = _model.getAtlasImageName(page.getIndex());
						IFile file = _model.getFile().getParent().getFile(new Path(atlasImageName));
						page.setImageFile(file);
					}

					monitor.worked(1);

					// update editor

					swtRun(new Runnable() {

						@Override
						public void run() {
							updateUIFromModel();

							try {
								if (oldEditorPages != null) {
									for (EditorPage page : oldEditorPages) {
										page.dispose();
									}
								}
							} catch (Exception e) {
								e.printStackTrace();
							}

							if (whenDone != null) {
								whenDone.run();
							}
						}
					});
				} catch (

				IOException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				} catch (Exception e) {
					e.printStackTrace();
					swtRun(new Runnable() {

						@Override
						public void run() {
							MessageDialog.openError(getEditorSite().getShell(), "Build Atlas", e.getMessage());
						}
					});
				} finally {
					try {
						// delete temporal files
						if (tempDir != null) {
							for (File f : tempDir.listFiles()) {
								f.delete();
							}
							tempDir.delete();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				return Status.OK_STATUS;

			}

		};

		job.setUser(true);
		job.schedule();
	}

	@Override
	public void setFocus() {
		_tabsFolder.setFocus();

		repaintTab(_tabsFolder.getSelectionIndex());
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			refreshFolder(monitor);

			List<IFile> toDelete = new ArrayList<>(_guessLastOutputFiles);

			{
				// save image
				int i = 0;
				for (EditorPage page : _model.getPages()) {
					String atlasImageName = _model.getAtlasImageName(i);
					IFile file = _model.getFile().getParent().getFile(new Path(atlasImageName));
					page.setImageFile(file);
					ImageLoader loader = new ImageLoader();
					loader.data = new ImageData[] { page.getImage().getImageData() };
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					loader.save(buffer, SWT.IMAGE_PNG);
					ByteArrayInputStream source = new ByteArrayInputStream(buffer.toByteArray());
					if (file.exists()) {
						file.setContents(source, true, false, monitor);
					} else {
						file.create(source, true, monitor);
					}
					toDelete.remove(file);
					i++;
				}
			}

			{
				// save editor model
				JSONObject json = _model.toJSON();
				ByteArrayInputStream source = new ByteArrayInputStream(json.toString(2).getBytes());
				IFile file = _model.getFile();
				file.setContents(source, true, false, monitor);
			}

			{
				// save atlas model

				if (_model.getSettings().multiatlas) {
					out.println("Generating with the multiatlas format");
					JSONObject json = _model.toPhaser3MultiatlasJSON();
					ByteArrayInputStream source = new ByteArrayInputStream(json.toString(2).getBytes());
					String atlasJSONName = _model.getAtlasName() + ".json";
					IFile file = _model.getFile().getParent().getFile(new Path(atlasJSONName));
					if (file.exists()) {
						file.setContents(source, true, false, monitor);
					} else {
						file.create(source, true, monitor);
					}
					toDelete.remove(file);
				} else {
					int i = 0;
					JSONObject[] list = _model.toPhaserHashJSON();
					for (JSONObject json : list) {
						ByteArrayInputStream source = new ByteArrayInputStream(json.toString(2).getBytes());
						String atlasJSONName = _model.getAtlasJSONName(i);
						IFile file = _model.getFile().getParent().getFile(new Path(atlasJSONName));
						if (file.exists()) {
							file.setContents(source, true, false, monitor);
						} else {
							file.create(source, true, monitor);
						}
						toDelete.remove(file);
						i++;
					}
				}
			}

			{
				// delete previous generates files
				for (IFile file : toDelete) {
					out.println("delete " + file);
					if (file.exists()) {
						try {
							file.delete(true, monitor);
						} catch (CoreException e) {
							throw new RuntimeException(e);
						}
					}
				}
			}

			refreshFolder(monitor);

			_dirty = false;
			swtRun(() -> firePropertyChange(PROP_DIRTY));
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	private void refreshFolder(IProgressMonitor monitor) throws CoreException {
		_model.getFile().getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
	}

	@Override
	public void doSaveAs() {
		// Do the Save As operation
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public boolean isDirty() {
		return _dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	void updateUIFromModel() {
		int sel = _tabsFolder.getSelectionIndex();
		if (sel < 0) {
			sel = 0;
		}

		while (_tabsFolder.getItemCount() > 0) {
			try {
				_tabsFolder.getItem(0).dispose();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		for (EditorPage page : _model.getPages()) {
			TabItem item = createTabItem();
			item.setText(page.getName());

			AtlasCanvas canvas = createAtlasCanvas(_tabsFolder);
			canvas.setImageFile(page.getImageFile(), page.getImage());
			canvas.setFrames(new ArrayList<>(page));

			item.setControl(canvas);
		}
		int tabsCount = _tabsFolder.getItemCount();
		if (tabsCount == 0) {
			addMainTab();
		} else {
			selectTab(Math.min(sel, tabsCount - 1));
		}

		if (_outliner != null) {
			_outliner.refresh();
		}
	}

	private AtlasCanvas createAtlasCanvas(Composite parent) {
		AtlasCanvas canvas = new AtlasCanvas(parent, SWT.NONE, false);
		// we handle the cache
		canvas.setDisableCanche(true);

		ISelectionChangedListener listener = new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateSelection_from_Canvas_to_Outliner(canvas);
			}
		};
		canvas.addSelectionChangedListener(listener);
		canvas.addDisposeListener(e -> canvas.removeSelectionChangedListener(listener));

		canvas.setMenu(_menuManager.createContextMenu(canvas));

		return canvas;
	}

	protected void updateSelection_from_Canvas_to_Outliner(AtlasCanvas canvas) {
		_canvasClicked = canvas;

		var selection = canvas.getSelection();

		if (_outliner != null) {
			_outliner.getTreeViewer().expandAll();
			_outliner.removeSelectionChangedListener(_outlinerSelectionListener);
			_outliner.setSelection(selection);
			_outliner.addSelectionChangedListener(_outlinerSelectionListener);
		}

		_selectionProvider.setSelection(selection);
	}

	private void addMainTab() {
		TabItem item = createTabItem();
		item.setText(_model.getAtlasImageName(0));
		AtlasCanvas canvas = createAtlasCanvas(_tabsFolder);
		canvas.setNoImageMessage("(drop image files here)");
		item.setControl(canvas);
		selectTab(0);
		canvas.setFocus();
	}

	/**
	 * @return
	 */
	private TabItem createTabItem() {
		return new TabItem(_tabsFolder, SWT.NONE);
	}

	public IFile getEditorInputFile() {
		return ((IFileEditorInput) getEditorInput()).getFile();
	}

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
										getSite().getPage().closeEditor(TexturePackerEditor.this, false);
									}
								});

							} else {
								// rename
								setInput(new FileEditorInput(root.getFile(movedTo)));
								swtRun(TexturePackerEditor.this::updateTitle);
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

	protected void updateTitle() {
		setPartName(getEditorInputFile().getName());
		firePropertyChange(PROP_TITLE);
	}

	public void manuallyBuild() {
		build(true, new Runnable() {

			@SuppressWarnings("synthetic-access")
			@Override
			public void run() {
				if (MessageDialog.openConfirm(getSite().getShell(), "Build Atlas",
						"Do you want to save the changes?")) {
					forceSave();
				}
			}
		});
	}

	private void forceSave() {
		WorkspaceJob job = new WorkspaceJob("Saving " + getEditorInputFile().getName() + ".") {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				doSave(monitor);
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	class TexturePackerEditorOutlineContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		@Override
		public Object[] getChildren(Object parent) {

			if (parent instanceof TexturePackerEditor) {
				return new Object[] { ((TexturePackerEditor) parent).getModel() };
			}

			if (parent instanceof TexturePackerEditorModel) {
				return ((TexturePackerEditorModel) parent).getPages().toArray();
			}

			if (parent instanceof EditorPage) {
				return ((EditorPage) parent).toArray();
			}

			return new Object[] {};
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

	}

	class TexturePackerEditorOutlineLabelProvider extends LabelProvider {

		@Override
		public String getText(Object element) {
			if (element instanceof TexturePackerEditorModel) {
				return ((TexturePackerEditorModel) element).getFile().getName();
			}

			if (element instanceof EditorPage) {
				return ((EditorPage) element).getName();
			}

			if (element instanceof TexturePackerEditorFrame) {
				return ((TexturePackerEditorFrame) element).getName();
			}

			return super.getText(element);
		}
	}

	class TexturePackerContentOutlinePage extends FilteredTreeCanvasContentOutlinePage {
		public TexturePackerContentOutlinePage() {
		}

		@Override
		public void createControl(Composite parent) {
			super.createControl(parent);

			var viewer = getTreeViewer();
			viewer.setLabelProvider(new TexturePackerEditorOutlineLabelProvider());
			viewer.setContentProvider(new TexturePackerEditorOutlineContentProvider());
			viewer.setInput(TexturePackerEditor.this);

			viewer.getControl().setMenu(getMenuManager().createContextMenu(viewer.getControl()));
		}

		@Override
		public void setSelection(ISelection selection) {
			for (Object obj : ((IStructuredSelection) selection).toArray()) {
				getTreeViewer().reveal(obj);
			}

			getTreeViewer().setSelection(selection);
		}

		@Override
		public void dispose() {
			getTreeViewer().removeSelectionChangedListener(_outlinerSelectionListener);
			TexturePackerEditor.this._outliner = null;
			super.dispose();
		}

		@Override
		protected TreeCanvasViewer createViewer() {
			return new TreeCanvasViewer(getCanvas()) {
				@Override
				protected void setItemProperties(TreeCanvasItem item) {
					super.setItemProperties(item);

					var element = item.getData();

					if (element instanceof EditorPage) {
						EditorPage page = (EditorPage) element;
						item.setRenderer(new ImageTreeCanvasItemRenderer(item, page.getImage()));
					}

					if (element instanceof TexturePackerEditorFrame) {
						TexturePackerEditorFrame frame = (TexturePackerEditorFrame) element;
						IFile file = findFile(frame);
						var img = getCanvas().loadImage(file);
						item.setRenderer(new ImageTreeCanvasItemRenderer(item, img));
					}

					if (element instanceof TexturePackerEditorFrame) {
						TexturePackerEditorFrame frame = (TexturePackerEditorFrame) element;
						IFile file = findFile(frame);
						var img = getCanvas().loadImage(file);
						item.setRenderer(new ImageTreeCanvasItemRenderer(item, img));
					}

					if (element instanceof TexturePackerEditorModel) {
						item.setRenderer(new IconTreeCanvasItemRenderer(item, EditorSharedImages.getImage(IEditorSharedImages.IMG_IMAGES)));
					}
				}
			};
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter.equals(IContentOutlinePage.class)) {
			return createOutliner();
		}

		if (adapter == IPropertySheetPage.class) {
			return createProperties();
		}

		if (adapter.equals(IContextProvider.class)) {
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
					IContext context = HelpSystem.getContext("phasereditor.help.textureatlaseditor");
					return context;
				}
			};
		}
		return super.getAdapter(adapter);
	}

	/**
	 * @return
	 */
	private Object createProperties() {
		if (_properties == null) {
			_properties = new TexturePackerPGridPage(this);
		}

		return _properties;
	}

	/**
	 * @return
	 */
	private Object createOutliner() {
		if (_outliner == null) {
			_outlinerSelectionListener = new ISelectionChangedListener() {

				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					updateSelection_from_Outline_to_Canvas();
				}
			};
			_outliner = new TexturePackerContentOutlinePage();
			_outliner.addSelectionChangedListener(_outlinerSelectionListener);
		}
		return _outliner;
	}

	public void updateSelection_from_Outline_to_Canvas() {
		IStructuredSelection selection = (IStructuredSelection) _outliner.getSelection();

		Object elem = selection.getFirstElement();

		int tabIndex = -1;

		if (elem instanceof EditorPage) {
			tabIndex = ((EditorPage) elem).getIndex();
		}

		for (int i = 0; i < _tabsFolder.getItemCount(); i++) {
			AtlasCanvas canvas = getAtlasCanvas(i);

			canvas.setSelection(selection, false);

			canvas.redraw();
			if (_canvasClicked == null) {
				if (!canvas.getSelection().isEmpty()) {
					tabIndex = i;
				}
			} else if (canvas == _canvasClicked) {
				tabIndex = i;
			}
		}

		if (tabIndex != -1) {
			selectTab(tabIndex);
		}

		_canvasClicked = null;
	}

	private void selectTab(int i) {
		_tabsFolder.setSelection(i);
		repaintTab(i);
	}

	void repaintTab(int i) {
		AtlasCanvas canvas = getAtlasCanvas(i);
		if (canvas.getScale() == 0) {
			canvas.resetZoom();
		}
	}

	public TexturePackerEditorModel getModel() {
		return _model;
	}
}
