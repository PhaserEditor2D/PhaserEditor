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
package phasereditor.atlas.ui.editors;

import static java.lang.System.out;
import static phasereditor.ui.PhaserEditorUI.eclipseFileToJavaPath;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.wb.swt.ResourceManager;
import org.eclipse.wb.swt.SWTResourceManager;
import org.json.JSONObject;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData.Region;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.utils.Array;

import phasereditor.atlas.core.AtlasFrame;
import phasereditor.atlas.core.Result;
import phasereditor.atlas.core.ResultPage;
import phasereditor.atlas.core.SettingsBean;
import phasereditor.atlas.ui.AtlasCanvas;
import phasereditor.atlas.ui.editors.AtlasGeneratorEditorModel.EditorPage;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.IconCache;
import phasereditor.ui.PhaserEditorUI;

public class AtlasGeneratorEditor extends EditorPart implements IEditorSharedImages, IResourceChangeListener {
	class FramesLabelProvider extends LabelProvider {

		private IconCache _cache = new IconCache();

		@Override
		public void dispose() {
			super.dispose();
			_cache.dispose();
		}

		@Override
		public String getText(Object element) {
			return ((AtlasFrame) element).getName();
		}

		@Override
		public Image getImage(Object element) {
			AtlasFrame frame = (AtlasFrame) element;
			IFile file = findFile(frame);
			if (file != null) {
				Image img = _cache.getIcon(file, 32, null);
				return img;
			}
			return super.getImage(element);
		}
	}

	public static final String ID = "phasereditor.atlas.ui.editors.AtlasGenEditor"; //$NON-NLS-1$
	private static final QualifiedName QNAME_SHOW_FILE_LIST = new QualifiedName("com.phasereditor.atlas.editor",
			"show-file-list");

	TableViewer _framesViewer;
	protected AtlasGeneratorEditorModel _model;
	HashMap<AtlasFrame, String> _frameRegionNameMap;
	private Composite _container;
	protected Result _result;
	private boolean _dirty;
	private Action _delAction;
	private Action _addAction;
	private Action _buildAction;
	private ToolBar _toolBar;
	private Action _settingsAction;
	private SashForm _sashForm;
	private Action _layoutAction;
	private CTabFolder _tabsFolder;
	private List<IFile> _guessLastOutputFiles;
	private boolean _showFileList;
	private MenuManager _popupManager;

	public AtlasGeneratorEditor() {
		_guessLastOutputFiles = new ArrayList<>();
		_frameRegionNameMap = new HashMap<>();
	}

	/**
	 * Create contents of the editor part.
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		_container = new Composite(parent, SWT.NONE);
		_container.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				handleDispose();
			}
		});
		_container.setLayout(new GridLayout(1, false));

		_toolBar = new ToolBar(_container, SWT.FLAT | SWT.RIGHT);
		_toolBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));

		_sashForm = new SashForm(_container, SWT.NONE);
		_sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		_framesViewer = new TableViewer(_sashForm, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		_framesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				frameSelectedInViewerSelected();
			}
		});
		Table table = _framesViewer.getTable();
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				handleTableKey(e);
			}
		});
		_framesViewer.setLabelProvider(new FramesLabelProvider());
		_framesViewer.setContentProvider(new ArrayContentProvider());
		_tabsFolder = new CTabFolder(_sashForm, SWT.NONE);
		_tabsFolder.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));

		_sashForm.setWeights(new int[] { 20, 100 });

		_tabsFolder.setSelection(0);

		createActions();

		afterCreateWidgets();
	}

	protected void handleTableKey(KeyEvent e) {
		if (e.character == SWT.DEL) {
			deleteSelection();
		}
	}

	private void createActions() {
		_delAction = new Action("Delete selected files") {
			@Override
			public void run() {
				deleteSelection();
			}
		};
		_delAction.setImageDescriptor(
				ResourceManager.getPluginImageDescriptor("phasereditor.ui", "icons/picture_delete.png"));

		_addAction = new Action("Add image files") {
			@Override
			public void run() {
				addFiles();
			}
		};
		_addAction.setImageDescriptor(
				ResourceManager.getPluginImageDescriptor("phasereditor.ui", "icons/picture_add.png"));

		_buildAction = new Action("Build Atlas") {
			@Override
			public void run() {
				manuallyBuild();
			}
		};
		_buildAction.setImageDescriptor(ResourceManager.getPluginImageDescriptor("phasereditor.ui", "icons/cog.png"));

		_settingsAction = new Action("Settings") {
			@Override
			public void run() {
				openSettingsDialog();
			}
		};
		_settingsAction
				.setImageDescriptor(ResourceManager.getPluginImageDescriptor("phasereditor.ui", "icons/wrench.png"));

		_layoutAction = new Action("Show/hide the legft side panel.", IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				changeLayout();
			}
		};
		_layoutAction.setImageDescriptor(
				ResourceManager.getPluginImageDescriptor("phasereditor.ui", "icons/application_side_expand.png"));
	}

	public void changeLayout() {
		String icon;
		if (_sashForm.getMaximizedControl() == null) {
			_sashForm.setMaximizedControl(_tabsFolder);
			icon = "icons/application_side_expand.png";
		} else {
			_sashForm.setMaximizedControl(null);
			icon = "icons/application_side_contract.png";
		}
		_layoutAction.setImageDescriptor(ResourceManager.getPluginImageDescriptor("phasereditor.ui", icon));
		persistShowFileListState();
	}

	public void openSettingsDialog() {
		AtlasSettingsDialog dlg = new AtlasSettingsDialog(getEditorSite().getShell());
		dlg.setSettings(_model.getSettings().clone());
		if (dlg.open() == Window.OK) {
			_model.setSettings(dlg.getSettings());
			build(true);
		}
	}

	public void addFiles() {
		MessageDialog.openInformation(getEditorSite().getShell(), "Add Files",
				"Drag the files from the Project Explorer and drop them here.");
	}

	public void deleteSelection() {
		Object[] sel = ((IStructuredSelection) _framesViewer.getSelection()).toArray();
		if (sel.length > 0) {
			List<IFile> toRemove = new ArrayList<>();
			for (Object item : sel) {
				AtlasFrame frame = (AtlasFrame) item;
				IFile file = findFile(frame);
				toRemove.add(file);
			}

			_model.getImageFiles().removeAll(toRemove);

			build(true);
		}
	}

	IFile findFile(AtlasFrame frame) {
		String name = _frameRegionNameMap.get(frame);

		for (IFile file : _model.getImageFiles()) {
			String location = file.getLocation().toPortableString();
			if (location.startsWith(name + ".")) {
				return file;
			}
		}

		return null;
	}

	protected void frameSelectedInViewerSelected() {
		AtlasFrame selected = getSelectedFrame();
		for (int i = 0; i < _tabsFolder.getItemCount(); i++) {
			AtlasCanvas canvas = getAtlasCanvas(i);
			canvas.setFrame(selected);
			canvas.redraw();
			canvas.setToolTipText(buildTooltip(selected));

			if (selected != null && canvas.getFrames().contains(selected)) {
				_tabsFolder.setSelection(i);
				canvas.setFocus();
				break;
			}
		}
	}

	private AtlasCanvas getAtlasCanvas(int i) {
		return (AtlasCanvas) _tabsFolder.getItem(i).getControl();
	}

	protected void handleDispose() {
		try {
			if (_result != null) {
				_result.dispose();
			}
		} catch (SWTException e) {
			// nothing
		}
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

		ToolBarManager manager = new ToolBarManager(_toolBar);
		manager.add(_addAction);
		manager.add(_delAction);
		manager.add(_buildAction);
		manager.add(_layoutAction);
		manager.add(_settingsAction);

		manager.update(true);

		getEditorSite().setSelectionProvider(_framesViewer);
		_popupManager = new MenuManager();
		Table table = _framesViewer.getTable();
		table.setMenu(_popupManager.createContextMenu(table));
		getEditorSite().registerContextMenu(_popupManager, _framesViewer, false);

		build(false);

		updateLayout(_showFileList);

		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);

	}

	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
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
			_model.addImageFiles(files);

			build(true);
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

	private void dirtify() {
		_dirty = true;
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);

		IFile file = ((IFileEditorInput) input).getFile();
		try {
			boolean hasUI = _framesViewer != null;
			if (_model == null) {
				_model = new AtlasGeneratorEditorModel(file);

				if (hasUI) {
					build(false);
				}
			} else {
				_model.setFile(file);
			}
			setPartName(file.getName());

			Map<QualifiedName, String> props = file.getPersistentProperties();
			_showFileList = props.getOrDefault(QNAME_SHOW_FILE_LIST, Boolean.TRUE.toString())
					.equals(Boolean.TRUE.toString());
			if (hasUI) {
				updateLayout(_showFileList);
			}

		} catch (IOException | CoreException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * @param showFileList
	 */
	private void updateLayout(boolean showFileList) {
		String icon;
		if (showFileList) {
			_sashForm.setMaximizedControl(null);
			icon = "icons/application_side_contract.png";
		} else {
			_sashForm.setMaximizedControl(_tabsFolder);
			icon = "icons/application_side_expand.png";
		}
		_layoutAction.setImageDescriptor(ResourceManager.getPluginImageDescriptor("phasereditor.ui", icon));
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

				TexturePacker packer = new TexturePacker(settings);

				List<IFile> missingFiles = new ArrayList<>();

				for (IFile wsFile : _model.getImageFiles()) {
					File file = eclipseFileToJavaPath(wsFile).toFile();
					if (file.exists() && file.isFile()) {
						packer.addImage(file);
					} else {
						missingFiles.add(wsFile);
					}
				}

				if (!missingFiles.isEmpty()) {
					Shell shell = getSite().getShell();
					AtomicBoolean confirm = new AtomicBoolean(false);
					shell.getDisplay().syncExec(new Runnable() {

						@Override
						public void run() {
							StringBuilder sb = new StringBuilder();
							for (IFile f : missingFiles) {
								sb.append("    " + f.getName() + "\n");
							}
							if (MessageDialog.openConfirm(shell, "Atlas Build", String.format(
									"The following source images do not exist. Do you want to ignore them?\n\n%s",
									sb))) {
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

					out.println(new String(Files.readAllBytes(libgdxAtlasFile.toPath())));

					TextureAtlasData data = new TextureAtlasData(new FileHandle(libgdxAtlasFile),
							new FileHandle(tempDir), false);

					// create result model

					Result oldResult = _result;

					_result = new Result();

					ImageLoader loader = new ImageLoader();

					Array<Region> regions = data.getRegions();

					_frameRegionNameMap = new HashMap<>();

					List<String> names = new ArrayList<>();

					for (Region region : regions) {
						names.add(region.name);
					}

					int nameStartIndex = 0;

					if (settings.isIncludeFolderName()) {
						if (!names.isEmpty()) {
							String test = names.stream().sorted((a, b) -> Integer.compare(a.length(), b.length()))
									.findFirst().get();

							for (int i = 0; i < test.length(); i++) {
								char c = test.charAt(i);

								for (String name : names) {
									char c2 = name.charAt(i);
									if (c2 != c) {
										nameStartIndex = i;
										break;
									}
								}

								if (nameStartIndex > 0) {
									break;
								}
							}
						}
					}

					for (TextureAtlasData.Page page : data.getPages()) {
						File textureFile = page.textureFile.file();

						ImageData[] imgData = loader.load(textureFile.getAbsolutePath());
						Image img = new Image(Display.getDefault(), imgData[0]);

						ResultPage resultPage = new ResultPage();
						resultPage.setImage(img);

						for (Region region : regions) {
							if (region.page == page) {
								AtlasFrame frame = new AtlasFrame();

								String regionName = region.name;
								if (region.index != -1) {
									regionName += "_" + region.index;
								}

								if (settings.isIncludeFolderName()) {
									String name = regionName.substring(nameStartIndex);
									frame.setName(name);
								} else {
									frame.setName(PhaserEditorUI.getNameFromFilename(regionName));
								}

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

								resultPage.addFrame(frame, regionName, region.index);
								_frameRegionNameMap.put(frame, regionName);
							}
						}
						if (settings.useIndexes) {
							resultPage.sortByIndexes();
						}
						_result.getPages().add(resultPage);
					}

					// create editor model

					List<EditorPage> editorPages = new ArrayList<>();
					for (ResultPage resultPage : _result.getPages()) {
						{
							out.println("page " + resultPage.getImage().getBounds());
						}
						EditorPage editorPage = new EditorPage();
						editorPage.addAll(resultPage.getFrames());
						editorPages.add(editorPage);
					}
					_model.setPages(editorPages);

					monitor.worked(1);

					// update editor

					swtRun(new Runnable() {

						@Override
						public void run() {
							postBuild(_result);

							try {
								if (oldResult != null) {
									oldResult.dispose();
								}
							} catch (Exception e) {
								e.printStackTrace();
							}

							if (whenDone != null) {
								whenDone.run();
							}
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				} catch (Exception e) {
					e.printStackTrace();
					swtRun(new Runnable() {

						@Override
						public void run() {
							MessageDialog.openError(getEditorSite().getShell(), "Build Atlas", e.getMessage());
							openSettingsDialog();
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
		_framesViewer.getControl().setFocus();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			refreshFolder(monitor);

			List<IFile> toDelete = new ArrayList<>(_guessLastOutputFiles);

			{
				// save editor model
				JSONObject json = _model.toJSON();
				ByteArrayInputStream source = new ByteArrayInputStream(json.toString(2).getBytes());
				IFile file = _model.getFile();
				file.setContents(source, true, false, monitor);
			}

			{
				// save image
				int i = 0;
				for (ResultPage page : _result.getPages()) {
					String atlasImageName = _model.getAtlasImageName(i);
					IFile file = _model.getFile().getParent().getFile(new Path(atlasImageName));
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
				// save atlas model
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
			firePropertyChange(PROP_DIRTY);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	private void persistShowFileListState() {
		// save UI state
		try {
			Object value = _sashForm.getMaximizedControl() == null ? Boolean.TRUE : Boolean.FALSE;
			_model.getFile().setPersistentProperty(QNAME_SHOW_FILE_LIST, value.toString());
		} catch (CoreException e) {
			e.printStackTrace();
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

	void postBuild(Result result) {
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

		int i = 1;
		for (ResultPage page : result.getPages()) {
			AtlasCanvas canvas;
			canvas = createAtlasCanvas();
			CTabItem item = createTabItem();
			item.setText("page " + i);
			item.setControl(canvas);
			canvas.setImage(page.getImage());
			canvas.setFrames(page.getFrames());
			i++;
		}
		int tabsCount = _tabsFolder.getItemCount();
		if (tabsCount == 0) {
			addMainTab();
		} else {
			_tabsFolder.setSelection(Math.min(sel, tabsCount - 1));
		}

		List<AtlasFrame> frames = new ArrayList<>();
		for (ResultPage page : result.getPages()) {
			frames.addAll(page.getFrames());
		}
		_framesViewer.setInput(frames);

		buildTooltip(getSelectedFrame());
	}

	private String buildTooltip(AtlasFrame frame) {
		StringBuilder sb = new StringBuilder();

		if (frame != null) {
			sb.append("name: " + frame.getName() + "\n");
			sb.append(String.format("frame: x=%s, y=%s, w=%s, h=%s\n", Integer.toString(frame.getFrameX()),
					Integer.toString(frame.getFrameY()), Integer.toString(frame.getFrameW()),
					Integer.toString(frame.getFrameH())));
			sb.append(String.format("sprite: x=%s, y=%s, w=%s, h=%s\n", Integer.toString(frame.getSpriteX()),
					Integer.toString(frame.getSpriteY()), Integer.toString(frame.getSpriteW()),
					Integer.toString(frame.getSpriteH())));
			sb.append(String.format("source: w=%s, h=%s\n", Integer.toString(frame.getSourceW()),
					Integer.toString(frame.getSourceH())));
		}

		if (_result != null) {
			int i = 1;
			for (ResultPage page : _result.getPages()) {
				Image img = page.getImage();
				Rectangle b = img.getBounds();
				sb.append(String.format("page %s: w=%s h=%s\n", Integer.toString(i), Integer.toString(b.width),
						Integer.toString(b.height)));
				i++;
			}
		}

		return sb.toString();
	}

	/**
	 * @return
	 */
	private AtlasFrame getSelectedFrame() {
		return (AtlasFrame) ((IStructuredSelection) _framesViewer.getSelection()).getFirstElement();
	}

	private AtlasCanvas createAtlasCanvas() {
		AtlasCanvas canvas = new AtlasCanvas(_tabsFolder, SWT.NONE);
		canvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				if (e.button == 1) {
					atlasCanvasClicked(canvas);
				}
			}
		});
		canvas.addMouseTrackListener(new MouseTrackListener() {

			@Override
			public void mouseHover(MouseEvent e) {
				atlasCanvasHover(canvas);
			}

			@Override
			public void mouseExit(MouseEvent e) {
				// nothing
			}

			@Override
			public void mouseEnter(MouseEvent e) {
				// nothing
			}
		});
		canvas.setMenu(_popupManager.createContextMenu(canvas));
		return canvas;
	}

	protected void atlasCanvasClicked(AtlasCanvas canvas) {
		AtlasFrame over = canvas.getOverFrame();
		if (over == null) {
			_framesViewer.setSelection(StructuredSelection.EMPTY);
		} else {
			_framesViewer.setSelection(new StructuredSelection(over), true);
			_framesViewer.getTable().forceFocus();
		}
	}

	protected void atlasCanvasHover(AtlasCanvas canvas) {
		AtlasFrame over = canvas.getOverFrame();
		if (over != null && !canvas.isFocusControl()) {
			canvas.setFocus();
		}
		canvas.setToolTipText(buildTooltip(over));
	}

	private void addMainTab() {
		CTabItem item = createTabItem();
		item.setText("page 1");
		AtlasCanvas canvas = createAtlasCanvas();
		canvas.setNoImageMessage("(drop image files here)");
		item.setControl(canvas);
		_tabsFolder.setSelection(0);
		canvas.setFocus();
	}

	/**
	 * @return
	 */
	private CTabItem createTabItem() {
		return new CTabItem(_tabsFolder, SWT.NONE);
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
										getSite().getPage().closeEditor(AtlasGeneratorEditor.this, false);
									}
								});

							} else {
								// rename
								setInput(new FileEditorInput(root.getFile(movedTo)));
								swtRun(AtlasGeneratorEditor.this::updateTitle);
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class adapter) {
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
}
