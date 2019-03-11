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
package phasereditor.assetpack.ui.editor;

import static phasereditor.ui.IEditorSharedImages.IMG_DELETE;
import static phasereditor.ui.IEditorSharedImages.IMG_RENAME;
import static phasereditor.ui.IEditorSharedImages.IMG_TYPE_VARIABLE_OBJ;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

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
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
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
import phasereditor.assetpack.core.AudioAssetModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.assetpack.ui.AssetsContentProvider;
import phasereditor.assetpack.ui.AssetsTreeCanvasViewer;
import phasereditor.assetpack.ui.IAssetPackEditor;
import phasereditor.assetpack.ui.ImageResourceDialog;
import phasereditor.assetpack.ui.SvgResourceDialog;
import phasereditor.audio.ui.AudioResourceDialog;
import phasereditor.lic.LicCore;
import phasereditor.project.core.ProjectCore;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.FilteredTreeCanvasContentOutlinePage;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;
import phasereditor.ui.TreeCanvas.TreeCanvasItemAction;
import phasereditor.ui.TreeCanvasViewer;
import phasereditor.ui.editors.EditorFileStampHelper;

/**
 * @author arian
 *
 */
public class AssetPackEditor extends EditorPart implements IGotoMarker, IShowInSource, IAssetPackEditor {

	public static final String ID = "phasereditor.assetpack.ui.editor.AssetPackEditor";

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

	private AssetPackEditorOutlinePage _outliner;

	private PackEditorCanvas _assetsCanvas;

	private EditorFileStampHelper _fileStampHelper;

	public AssetPackEditor() {
		_fileStampHelper = new EditorFileStampHelper(this, this::reloadMethod, this::saveMethod);
	}

	public AssetPackEditorOutlinePage getOutliner() {
		return _outliner;
	}

	public void setOutliner(AssetPackEditorOutlinePage outliner) {
		_outliner = outliner;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		_fileStampHelper.helpDoSave(monitor);
	}

	private void saveMethod(IProgressMonitor monitor) {
		_model.save(monitor);
		firePropertyChange(PROP_DIRTY);
		saveEditingPoint();
		refresh();
	}

	public void refresh() {
		if (_outliner != null) {
			_outliner.refresh();
		}

		_assetsCanvas.redraw();
	}

	public void saveEditingPoint() {
		AssetPackModel pack = getModel();
		if (pack == null) {
			return;
		}

		var sel = getSelection();

		if (sel.length == 0) {
			return;
		}

		Object elem = sel[0];
		if (elem != null) {
			IFile file = pack.getFile();
			try {
				file.setPersistentProperty(EDITING_NODE, pack.getStringReference(elem));
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	private void recoverEditingPoint() {
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

	MenuManager createAddAssetMenu(AssetSectionModel section) {
		var manager = new MenuManager();
		for (var type : AssetType.values()) {
			if (AssetType.isTypeSupported(type.name())) {
				manager.add(new Action(type.getCapitalName(),
						EditorSharedImages.getImageDescriptor(IMG_TYPE_VARIABLE_OBJ)) {
					@Override
					public void run() {
						openAddAssetDialog(section, type);
					}
				});
			}
		}
		return manager;
	}

	@SuppressWarnings({ "boxing" })
	private List<AssetModel> openNewAssetListDialog(AssetSectionModel section, AssetFactory factory) throws Exception {
		AssetType type = factory.getType();

		var pack = getModel();

		switch (type) {

		case image:
			return openNewImageListDialog(section);
		case audio:
			return openNewAudioListDialog(section);
		case atlas:
		case atlasXML:
		case unityAtlas:
		case multiatlas:
			return openNewAtlasListDialog(section);
		case audioSprite:
			return openNewAudioSpriteListDialog(section);
		case binary:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return getModel().discoverFiles(f -> true);
				} catch (CoreException e) {
					throw new RuntimeException(e);
				}
			});
		case json:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return getModel().discoverJsonFiles();
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		case physics:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return pack.discoverJsonFiles();
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		case glsl:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return pack.discoverTextFiles(AssetPackCore.SHADER_EXTS);
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		case text:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return pack.discoverFiles(f -> Boolean.TRUE);
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		case xml:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return pack.discoverTextFiles("xml");
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		case htmlTexture:
		case html:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return pack.discoverTextFiles("html");
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		case script:
		case plugin:
		case scenePlugin:
		case sceneFile:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return pack.discoverTextFiles("js");
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		case bitmapFont:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return pack.discoverBitmapFontFiles();
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		case spritesheet:
			return openNewSpritesheetListDialog(section);
		case tilemapCSV:
		case tilemapTiledJSON:
		case tilemapImpact:
			return openNewTilemapListDialog(section, type);
		case animation:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return pack.discoverAnimationsFiles();
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		case svg:
			return openNewSvgListDialog(section);
		case video:
			break;
		default:
			break;
		}

		return Collections.emptyList();
	}

	protected void openAddAssetDialog(AssetSectionModel section, AssetType initialType) {
		if (LicCore.isEvaluationProduct()) {

			IProject project = getEditorInput().getFile().getProject();

			String rule = AssetPackCore.isFreeVersionAllowed(project);
			if (rule != null) {
				LicCore.launchGoPremiumDialogs(rule);
				return;
			}
		}

		try {
			AssetFactory factory = AssetFactory.getFactory(initialType);

			if (factory != null) {
				var assets = openNewAssetListDialog(section, factory);

				if (!assets.isEmpty()) {

					for (var asset : assets) {
						section.addAsset(asset, false);
					}

					_model.build();

					_assetsCanvas.getUtils().setSelectionList(assets);
					if (!assets.isEmpty()) {
						_assetsCanvas.reveal(assets.get(0));
					} else {
						_assetsCanvas.redraw();
					}

					_model.setDirty(true);

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private List<AssetModel> openNewTilemapListDialog(AssetSectionModel section, AssetType type) throws Exception {
		AssetPackModel pack = getModel();
		var tilemapFiles = pack.discoverTilemapFiles(type);

		var shell = getEditorSite().getShell();

		List<AssetModel> list = new ArrayList<>();

		List<IFile> selectedFiles = AssetPackUI.browseManyAssetFile(pack, "tilemap", tilemapFiles, shell);

		create_Assets_from_Files_and_add_to_List(list, section, type, selectedFiles);

		return list;
	}

	private List<AssetModel> openNewSpritesheetListDialog(AssetSectionModel section) throws CoreException {
		AssetPackModel pack = getModel();
		List<IFile> imageFiles = pack.discoverImageFiles();

		Set<IFile> usedFiles = pack.sortFilesByNotUsed(imageFiles);

		var shell = getEditorSite().getShell();
		var dlg = new ImageResourceDialog(shell);
		dlg.setLabelProvider(AssetPackUI.createFilesLabelProvider(usedFiles, shell));
		dlg.setInput(imageFiles);
		dlg.setObjectName(AssetType.spritesheet.getCapitalName());

		List<AssetModel> list = new ArrayList<>();

		if (dlg.open() == Window.OK) {
			for (Object obj : dlg.getMultipleSelection()) {
				IFile file = (IFile) obj;
				create_Asset_from_File_and_add_to_List(list, section, AssetType.spritesheet, file);
			}
		}

		return list;
	}

	private List<AssetModel> openNewSimpleFileListDialog(AssetSectionModel section, AssetType type,
			Supplier<List<IFile>> discoverFiles) {
		var pack = getModel();

		var dialogFiles = discoverFiles.get();

		var shell = getEditorSite().getShell();

		var list = new ArrayList<AssetModel>();

		var selectedFiles = AssetPackUI.browseManyAssetFile(pack, type.getCapitalName(), dialogFiles, shell);

		create_Assets_from_Files_and_add_to_List(list, section, type, selectedFiles);

		return list;
	}

	private List<AssetModel> openNewAudioSpriteListDialog(AssetSectionModel section) throws CoreException {
		AssetPackModel pack = getModel();

		List<IFile> audiospriteFiles = pack.discoverAudioSpriteFiles();

		var shell = getEditorSite().getShell();

		List<AssetModel> list = new ArrayList<>();

		List<IFile> selectedFiles = AssetPackUI.browseManyAssetFile(pack, "audiosprite", audiospriteFiles, shell);

		create_Assets_from_Files_and_add_to_List(list, section, AssetType.audioSprite, selectedFiles);

		return list;
	}

	private List<AssetModel> openNewAtlasListDialog(AssetSectionModel section) throws Exception {
		AssetPackModel pack = getModel();

		var fileTypeMap = new HashMap<IFile, AssetType>();

		for (var type : new AssetType[] { AssetType.atlas, AssetType.multiatlas, AssetType.atlasXML,
				AssetType.unityAtlas }) {
			for (var file : pack.discoverAtlasFiles(type)) {
				fileTypeMap.put(file, type);
			}
		}

		var atlasFiles = new ArrayList<>(fileTypeMap.keySet());

		var shell = getEditorSite().getShell();

		var list = new ArrayList<AssetModel>();

		var selectedFiles = AssetPackUI.browseManyAssetFile(pack, "Atlas (Multi, JSON, XML, Unity)", atlasFiles, shell);

		for (IFile file : selectedFiles) {
			var type = fileTypeMap.get(file);
			create_Asset_from_File_and_add_to_List(list, section, type, file);
		}

		return list;
	}

	private List<AssetModel> openNewAudioListDialog(AssetSectionModel section) throws CoreException {
		AssetPackModel pack = getModel();

		List<IFile> audioFiles = pack.discoverAudioFiles();

		Set<IFile> usedFiles = pack.sortFilesByNotUsed(audioFiles);

		var shell = getEditorSite().getShell();
		var dlg = new AudioResourceDialog(shell, false);
		dlg.setLabelProvider(AssetPackUI.createFilesLabelProvider(usedFiles, shell));
		dlg.setInput(audioFiles);

		List<AssetModel> list = new ArrayList<>();

		if (dlg.open() == Window.OK) {

			Map<String, List<String>> filesMap = new HashMap<>();

			for (IFile file : dlg.getSelection()) {
				String prefix = file.getFullPath().removeFileExtension().toPortableString();

				if (!filesMap.containsKey(prefix)) {
					filesMap.put(prefix, new ArrayList<>());
				}

				filesMap.get(prefix).add(ProjectCore.getAssetUrl(file));
			}

			for (var entry : filesMap.entrySet()) {
				var path = new Path(entry.getKey());
				var asset = new AudioAssetModel(pack.createKey(path.lastSegment()), section);
				asset.setUrls(entry.getValue());
				list.add(asset);
			}
		}

		return list;
	}

	private static void create_Asset_from_File_and_add_to_List(List<AssetModel> list, AssetSectionModel section,
			AssetType type, IFile file) {
		try {
			list.add(AssetFactory.getFactory(type).createAsset(section, file));
		} catch (Exception e) {
			AssetPackUI.logError(e);
			throw new RuntimeException(e);
		}
	}

	private static void create_Assets_from_Files_and_add_to_List(List<AssetModel> list, AssetSectionModel section,
			AssetType type, List<IFile> selectedFiles) {
		for (var file : selectedFiles) {
			create_Asset_from_File_and_add_to_List(list, section, type, file);
		}
	}

	private List<AssetModel> openNewImageListDialog(AssetSectionModel section) throws CoreException {
		AssetPackModel pack = getModel();
		List<IFile> imageFiles = pack.discoverImageFiles();

		Set<IFile> usedFiles = pack.sortFilesByNotUsed(imageFiles);

		var shell = getEditorSite().getShell();
		ImageResourceDialog dlg = new ImageResourceDialog(shell);
		dlg.setLabelProvider(AssetPackUI.createFilesLabelProvider(usedFiles, shell));
		dlg.setInput(imageFiles);
		dlg.setObjectName("");

		List<AssetModel> list = new ArrayList<>();

		if (dlg.open() == Window.OK) {
			for (Object obj : dlg.getMultipleSelection()) {
				IFile file = (IFile) obj;
				create_Asset_from_File_and_add_to_List(list, section, AssetType.image, file);
			}
		}

		return list;
	}

	@SuppressWarnings("boxing")
	private List<AssetModel> openNewSvgListDialog(AssetSectionModel section) throws CoreException {
		AssetPackModel pack = getModel();
		List<IFile> imageFiles = pack.discoverFiles(f -> "svg".equals(f.getFileExtension()));

		Set<IFile> usedFiles = pack.sortFilesByNotUsed(imageFiles);

		var shell = getEditorSite().getShell();
		var dlg = new SvgResourceDialog(shell);
		dlg.setLabelProvider(AssetPackUI.createFilesLabelProvider(usedFiles, shell));
		dlg.setInput(imageFiles);
		dlg.setObjectName(AssetType.svg.getCapitalName());

		List<AssetModel> list = new ArrayList<>();

		if (dlg.open() == Window.OK) {
			for (Object obj : dlg.getMultipleSelection()) {
				IFile file = (IFile) obj;
				create_Asset_from_File_and_add_to_List(list, section, AssetType.svg, file);
			}
		}

		return list;
	}

	Map<AssetSectionModel, AssetGroupModel> _lastSelectedTypeMap = new HashMap<>();

	private Action _deleteSelectionAction;

	private Action _renameSelectionAction;

	@Override
	public void createPartControl(Composite parent) {

		createActions();

		_assetsCanvas = new PackEditorCanvas(this, parent, 0);

		_assetsCanvas.getUtils().addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (_assetsCanvas.isFocusControl()) {
					if (getOutliner() != null) {
						getOutliner().revealAndSelect(event.getStructuredSelection());
					}
				}
			}
		});

		getEditorSite().setSelectionProvider(_assetsCanvas.getUtils());

		recoverEditingPoint();

		_assetsCanvas.setModel(_model);

		swtRun(this::refresh);

	}

	private void createActions() {
		_deleteSelectionAction = new Action("Delete selection (Refactoring)",
				EditorSharedImages.getImageDescriptor(IMG_DELETE)) {
			@Override
			public void run() {
				var selection = getSelection();
				AssetPackUIEditor.launchDeleteWizard(selection, AssetPackEditor.this);
			}
		};

		_renameSelectionAction = new Action("Rename object (Refactoring)",
				EditorSharedImages.getImageDescriptor(IMG_RENAME)) {
			@Override
			public void run() {
				var selection = getSelection();
				AssetPackUIEditor.launchRenameWizard(selection[0], AssetPackEditor.this);
			}
		};
	}

	public Action getDeleteSelectionAction() {
		return _deleteSelectionAction;
	}

	public Action getRenameSelectionAction() {
		return _renameSelectionAction;
	}

	@Override
	public ShowInContext getShowInContext() {
		return new ShowInContext(getEditorInput(), new StructuredSelection(getSelection()));
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);

		FileEditorInput fileInput = getEditorInput();
		IFile file = fileInput.getFile();

		try {
			// we create a model copy detached from the AssetCore registry.
			_model = new AssetPackModel(file);
			_model.build();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		_model.addPropertyChangeListener(this::propertyChange);

		setPartName(_model.getName());

	}

	public void handleFileMoved(IFile file) {
		_model.setFile(file);

		super.setInput(new FileEditorInput(file));

		setPartName(_model.getName());
	}

	public void build() {
		getModel().build();
		refresh();
	}

	public void reloadFile() {
		_fileStampHelper.helpReloadFile();
	}

	private void reloadMethod() {
		try {
			_model = new AssetPackModel(getEditorInput().getFile());
			firePropertyChange(PROP_DIRTY);

			_model.addPropertyChangeListener(this::propertyChange);

			if (_outliner != null) {
				_outliner.getTreeViewer().setInput(_model);
			}

			_assetsCanvas.setModel(_model);

			build();

			getEditorSite().getSelectionProvider().setSelection(StructuredSelection.EMPTY);
			_assetsCanvas.getUtils().setSelection(StructuredSelection.EMPTY);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void propertyChange(@SuppressWarnings("unused") PropertyChangeEvent evt) {
		getEditorSite().getShell().getDisplay().asyncExec(() -> {
			firePropertyChange(PROP_DIRTY);
			refresh();
		});
	}

	@Override
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
		_assetsCanvas.setFocus();
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

	@Override
	public void revealElement(Object elem) {
		if (elem == null) {
			return;
		}

		if (elem instanceof IAssetKey) {
			var assetKey = (IAssetKey) elem;
			getAssetsCanvas().reveal(assetKey.getAsset());
			_assetsCanvas.getUtils().setSelectionList(List.of(assetKey.getAsset()));
		}

	}

	public class AssetPackEditorOutlinePage extends FilteredTreeCanvasContentOutlinePage {
		private ISelectionChangedListener _listener;

		public AssetPackEditorOutlinePage() {
		}

		public AssetPackEditor getEditor() {
			return AssetPackEditor.this;
		}

		@Override
		protected TreeCanvasViewer createViewer() {
			var viewer = new AssetsTreeCanvasViewer(getFilteredTreeCanvas().getTree(), new AssetsContentProvider(true),
					AssetLabelProvider.GLOBAL_16) {

				@Override
				protected void setItemProperties(TreeCanvasItem item) {
					super.setItemProperties(item);

					if (item.getData() instanceof AssetSectionModel) {
						var section = (AssetSectionModel) item.getData();
						item.getActions()
								.add(new TreeCanvasItemAction(EditorSharedImages.getImage(IMG_ADD), "Add assets") {

									@Override
									public void run(MouseEvent event) {
										var manager = createAddAssetMenu(section);
										var menu = manager.createContextMenu(getTreeViewer().getControl());
										menu.setVisible(true);
									}
								});
					}
				}
			};
			viewer.getTree().getUtils().setFilterInputWhenSetSelection(false);
			return viewer;
		}

		public void revealAndSelect(IStructuredSelection selection) {
			getTreeViewer().setSelection(selection, true);
		}

		@Override
		public void createControl(Composite parent) {
			super.createControl(parent);

			var viewer = getTreeViewer();

			viewer.addSelectionChangedListener(_listener = new ISelectionChangedListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					if (!viewer.getTree().isFocusControl()) {
						return;
					}

					var list = event.getStructuredSelection().toList().stream()

							.filter(o -> o instanceof IAssetKey)

							.map(o -> ((IAssetKey) o).getAsset())

							.toArray();

					getAssetsCanvas().getUtils().setSelectionList(Arrays.asList(list));

					if (list.length > 0) {
						getAssetsCanvas().reveal((AssetModel) list[0]);
					} else {
						getAssetsCanvas().redraw();
					}
				}
			});

			AssetPackUI.installAssetTooltips(viewer.getTree(), viewer.getTree().getUtils());

			viewer.setInput(getModel());

			// viewer.getControl().setMenu(getMenuManager().createContextMenu(viewer.getControl()));
		}

		@Override
		public void dispose() {

			getTreeViewer().removeSelectionChangedListener(_listener);

			setOutliner(null);

			super.dispose();
		}
	}

	private List<AssetPackEditorPropertyPage> _propertyPageList = new ArrayList<>();

	List<AssetPackEditorPropertyPage> getPropertyPageList() {
		return _propertyPageList;
	}

	public void updatePropertyPages() {
		for (var page : _propertyPageList) {
			page.selectionChanged(this, getSite().getSelectionProvider().getSelection());
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == IPropertySheetPage.class) {
			var page = new AssetPackEditorPropertyPage(this);
			_propertyPageList.add(page);
			return page;
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

	public PackEditorCanvas getAssetsCanvas() {
		return _assetsCanvas;
	}

	public void launchRenameWizard() {
		AssetPackUIEditor.launchRenameWizard(getSelection()[0], this);
	}

	public void launchAddSectionDialog() {
		var model = getModel();
		var dlg = new InputDialog(getSite().getShell(), "New Section", "Enter the section key:",
				model.createKey("section"), new IInputValidator() {

					@Override
					public String isValid(String newText) {
						return model.hasKey(newText) ? "That key already exists, use other." : null;
					}
				});
		if (dlg.open() == Window.OK) {
			var sectionName = dlg.getValue();

			var section = new AssetSectionModel(sectionName, model);
			model.addSection(section, true);

			refresh();

			revealElement(section);
		}
	}

	private Object[] getSelection() {
		var sel = (IStructuredSelection) getSite().getSelectionProvider().getSelection();

		if (sel.isEmpty()) {
			if (_outliner != null) {
				return ((IStructuredSelection) _outliner.getSelection()).toArray();
			}
		}

		return sel.toArray();
	}

	public void clearSelection() {
		if (_outliner != null) {
			_outliner.setSelection(StructuredSelection.EMPTY);
			_assetsCanvas.getUtils().setSelectionList(List.of());
		}
	}

}
