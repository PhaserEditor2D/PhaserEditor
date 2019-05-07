package phasereditor.scene.ui.editor;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableEditor;
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
import org.json.JSONObject;

import phasereditor.assetpack.core.AnimationsAssetModel;
import phasereditor.assetpack.core.AssetFinder;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.MultiAtlasAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.lic.LicCore;
import phasereditor.project.core.PhaserProjectBuilder;
import phasereditor.scene.core.AnimationsComponent;
import phasereditor.scene.core.BitmapTextComponent;
import phasereditor.scene.core.BitmapTextModel;
import phasereditor.scene.core.ImageModel;
import phasereditor.scene.core.NameComputer;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.SceneCore;
import phasereditor.scene.core.SceneModel;
import phasereditor.scene.core.SpriteModel;
import phasereditor.scene.core.TextualComponent;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.core.VariableComponent;
import phasereditor.scene.ui.editor.messages.DeleteObjectsMessage;
import phasereditor.scene.ui.editor.messages.GetPastePositionMessage;
import phasereditor.scene.ui.editor.messages.ReloadPageMessage;
import phasereditor.scene.ui.editor.messages.RevealObjectMessage;
import phasereditor.scene.ui.editor.messages.SelectObjectsMessage;
import phasereditor.scene.ui.editor.messages.SetInteractiveToolMessage;
import phasereditor.scene.ui.editor.outline.SceneOutlinePage;
import phasereditor.scene.ui.editor.properties.ScenePropertyPage;
import phasereditor.scene.ui.editor.undo.WorldSnapshotOperation;
import phasereditor.ui.IEditorBlock;
import phasereditor.ui.IEditorBlockProvider;
import phasereditor.ui.SelectionProviderImpl;
import phasereditor.ui.editors.EditorFileStampHelper;
import phasereditor.webrun.core.BatchMessage;

public class SceneEditor extends EditorPart implements IPersistableEditor {

	private static final String SCENE_COPY_STAMP = "--scene--copy--stamp--";
	public static final String ID = "phasereditor.scene.ui.editor.SceneEditor";
	private static String COMMAND_CONTEXT = "phasereditor.scene.ui.editor.command";
	private static String SEARCH_CONTEXT = "phasereditor.scene.ui.editor.search";

	private SceneModel _model;
	private SceneOutlinePage _outline;
	private boolean _dirty;
	ISelectionChangedListener _outlinerSelectionListener;
	private List<ScenePropertyPage> _propertyPages;
	private Set<String> _interactiveTools;
	private boolean _transformLocalCoords;

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
	private IContextActivation _commandContextActivation;
	private EditorFileStampHelper _fileStampHelper;
	private SceneEditorBroker _broker;
	private SceneWebView _webView;
	private SelectionEvents _selectionEvents;
	private IContextActivation _searchContextActivation;
	private SceneEditorBlockProvider _blocksProvider;

	public SceneEditor() {
		_outlinerSelectionListener = new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				_selectionProvider.setAutoFireSelectionChanged(false);
				_selectionProvider.setSelection(event.getSelection());
				_selectionProvider.setAutoFireSelectionChanged(true);

				getBroker().sendAll(new SelectObjectsMessage(SceneEditor.this));
			}
		};

		_propertyPages = new ArrayList<>();

		_fileStampHelper = new EditorFileStampHelper(this, this::reloadMethod, this::saveMethod);

		_interactiveTools = new HashSet<>();

		_selectionEvents = new SelectionEvents(this);

		_transformLocalCoords = true;
	}

	public SelectionEvents getSelectionEvents() {
		return _selectionEvents;
	}

	public boolean isTransformLocalCoords() {
		return _transformLocalCoords;
	}

	public void setTransformLocalCoords(boolean transformLocalCoords) {
		_transformLocalCoords = transformLocalCoords;
	}

	public Set<String> getInteractiveTools() {
		return _interactiveTools;
	}

	public void setInteractiveTools(Set<String> interactiveTools) {
		_interactiveTools = interactiveTools;

		updatePropertyPagesContentWithSelection();

		_broker.sendAll(new SetInteractiveToolMessage(this));
	}

	public void setInteractiveTools(String... interactiveTools) {
		setInteractiveTools(Set.of(interactiveTools));
	}

	public boolean hasInteractiveTools(Set<String> tools) {
		return _interactiveTools.containsAll(tools);
	}

	private IContextService getContextService() {
		IContextService service = getSite().getWorkbenchWindow().getWorkbench().getService(IContextService.class);
		return service;
	}

	public void activateCommandContext() {
		_commandContextActivation = getContextService().activateContext(COMMAND_CONTEXT);
	}

	public void deactivateCommandContext() {
		getContextService().deactivateContext(_commandContextActivation);
	}

	public void activateSearchContext() {
		_searchContextActivation = getContextService().activateContext(SEARCH_CONTEXT);
	}

	public void deactivateSearchContext() {
		getContextService().deactivateContext(_searchContextActivation);
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
		updatePropertyPagesContentWithSelection();

		refreshOutline();

		_broker.sendAll(new ReloadPageMessage());
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
		_broker = new SceneEditorBroker(this);
		_webView = new SceneWebView(this, parent, SWT.NONE);
		_webView.setUrl(_broker.getUrl());
	}

	public SceneEditorBroker getBroker() {
		return _broker;
	}

	@Override
	public void dispose() {

		_broker.dispose();

		super.dispose();
	}

	@Override
	public void setFocus() {
		_webView.setFocus();
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

				}
			};

			return _outline;

		}

		if (adapter == IEditorBlockProvider.class) {
			if (_blocksProvider == null) {
				_blocksProvider = new SceneEditorBlockProvider();
			}
			return _blocksProvider;
		}

		return super.getAdapter(adapter);
	}

	class SceneEditorBlockProvider implements IEditorBlockProvider {

		@Override
		public List<IEditorBlock> getBlocks() {

			var packs = AssetPackCore.getAssetPackModels(getProject());

			var list = packs.stream()

					.flatMap(pack -> pack.getAssets().stream())

					.filter(asset -> {
						return asset instanceof ImageAssetModel || asset instanceof AtlasAssetModel
								|| asset instanceof MultiAtlasAssetModel || asset instanceof IAssetFrameModel || asset instanceof AnimationsAssetModel;
					})

					.map(asset -> AssetPackUI.getAssetEditorBlock(asset))

					.collect(toList());

			return list;
		}

		@Override
		public void setRefreshHandler(Runnable refresh) {
			//
		}

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

	public List<ObjectModel> selectionDropped(float x, float y, Object[] data) {
		var finder = AssetPackCore.getAssetFinder(getProject());

		var sceneModel = getSceneModel();

		var nameComputer = new NameComputer(sceneModel.getDisplayList());

		var beforeSnapshot = WorldSnapshotOperation.takeSnapshot(this);

		var modelX = sceneModel.snapValueX(x);
		var modelY = sceneModel.snapValueY(y);

		var newModels = new ArrayList<ObjectModel>();

		for (var obj : data) {

			if (obj instanceof ImageAssetModel) {
				obj = ((ImageAssetModel) obj).getFrame();
			}

			if (obj instanceof AnimationModel) {
				var animFrames = ((AnimationModel) obj).getFrames();
				if (!animFrames.isEmpty()) {
					obj = animFrames.get(0);
				}
			}

			if (obj instanceof AnimationFrameModel) {
				var animFrame = (AnimationFrameModel) obj;
				var textureFrame = animFrame.getFrameName() == null ? null : animFrame.getFrameName() + "";
				var textureKey = animFrame.getTextureKey();

				var texture = finder.findTexture(textureKey, textureFrame);

				if (texture != null) {
					var sprite = new SpriteModel();

					var name = nameComputer.newName(computeBaseName(texture));

					VariableComponent.set_variableName(sprite, name);

					TransformComponent.set_x(sprite, modelX);
					TransformComponent.set_y(sprite, modelY);

					TextureComponent.set_textureKey(sprite, textureKey);
					TextureComponent.set_textureFrame(sprite, textureFrame);

					AnimationsComponent.set_autoPlayAnimKey(sprite, animFrame.getAnimation().getKey());

					newModels.add(sprite);

				}
			} else if (obj instanceof IAssetFrameModel) {

				var frame = (IAssetFrameModel) obj;

				var sprite = new ImageModel();

				var name = nameComputer.newName(computeBaseName(frame));

				VariableComponent.set_variableName(sprite, name);

				TransformComponent.set_x(sprite, modelX);
				TransformComponent.set_y(sprite, modelY);

				TextureComponent.utils_setTexture(sprite, (IAssetFrameModel) obj);

				newModels.add(sprite);

			} else if (obj instanceof BitmapFontAssetModel) {

				var asset = (BitmapFontAssetModel) obj;

				var textModel = new BitmapTextModel();

				var name = nameComputer.newName(asset.getKey());

				VariableComponent.set_variableName(textModel, name);

				TransformComponent.set_x(textModel, modelX);
				TransformComponent.set_y(textModel, modelY);

				BitmapTextComponent.utils_setFont(textModel, asset);
				TextualComponent.set_text(textModel, "BitmapText");

				textModel.updateSizeFromBitmapFont(finder);

				newModels.add(textModel);

			}
		}

		for (var model : newModels) {
			ParentComponent.utils_addChild(sceneModel.getDisplayList(), model);
		}

		var afterSnapshot = WorldSnapshotOperation.takeSnapshot(this);

		executeOperation(new WorldSnapshotOperation(beforeSnapshot, afterSnapshot, "Drop assets"));

		refreshOutline();

		setSelection(newModels);

		setDirty(true);

		getEditorSite().getPage().activate(this);

		return newModels;
	}

	private static String computeBaseName(IAssetFrameModel texture) {
		if (texture instanceof SpritesheetAssetModel.FrameModel) {
			return texture.getAsset().getKey();
		}

		return texture.getKey();
	}

	public AssetFinder getAssetFinder() {
		return AssetPackCore.getAssetFinder(getProject());
	}

	public void delete() {
		var beforeData = WorldSnapshotOperation.takeSnapshot(this);

		var selection = getSelectionList();

		for (var model : selection) {
			ParentComponent.utils_removeFromParent(model);
		}

		for (var group : getSceneModel().getGroupsModel().getGroups()) {
			group.getChildren().removeAll(selection);
		}

		refreshOutline();

		setDirty(true);

		setSelection(List.of());

		var afterData = WorldSnapshotOperation.takeSnapshot(this);

		executeOperation(new WorldSnapshotOperation(beforeData, afterData, "Delete objects"));

		_broker.sendAll(new BatchMessage(

				new DeleteObjectsMessage(selection),

				new SelectObjectsMessage(this)

		));

	}

	private JSONObject _cameraState = new JSONObject();

	public JSONObject getCameraState() {
		return _cameraState;
	}

	public void setCameraState(JSONObject cameraState) {
		_cameraState = cameraState;
	}

	@Override
	public void saveState(IMemento memento) {
		memento.putString("cameraState", _cameraState.toString());
	}

	@Override
	public void restoreState(IMemento memento) {
		var s = memento.getString("cameraState");
		if (s != null) {
			_cameraState = new JSONObject(s);
		}
	}

	public void selectAll() {
		var list = new ArrayList<ObjectModel>();

		var root = getSceneModel().getDisplayList();

		root.visitChildren(model -> list.add(model));

		setSelection(list);

		getBroker().sendAll(new SelectObjectsMessage(this));
	}

	public void copy() {
		var sel = new StructuredSelection(filterChidlren(getSelectionList())

				.stream().map(model -> {

					var data = new JSONObject();

					data.put(SCENE_COPY_STAMP, true);

					model.write(data);

					// convert the local position to a global position

					// TODO: this maybe be needed to support containers. This info could be
					// part of SetObjectDisplayProperties message.

					// if (model instanceof TransformComponent) {
					//
					// var parent = ParentComponent.get_parent(model);
					//
					// var globalPoint = new float[] { 0, 0 };
					//
					// if (parent != null) {
					// globalPoint = _renderer.localToScene(parent, TransformComponent.get_x(model),
					// TransformComponent.get_y(model));
					// }
					//
					// data.put(TransformComponent.x_name, globalPoint[0]);
					// data.put(TransformComponent.y_name, globalPoint[1]);
					// }

					return data;

				}).toArray());

		LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
		transfer.setSelection(sel);

		Clipboard cb = new Clipboard(getDisplay());
		cb.setContents(new Object[] { sel.toArray() }, new Transfer[] { transfer });
		cb.dispose();
	}

	public void cut() {
		copy();
		delete();
	}

	public void paste() {
		paste(Optional.empty(), true);
	}

	public void paste(Optional<ObjectModel> parent, boolean pasteAtCursorPosition) {
		getBroker().sendAll(new GetPastePositionMessage(parent, pasteAtCursorPosition));
	}

	public void paste(ObjectModel parent, float pasteX, float pasteY) {
		var beforeData = WorldSnapshotOperation.takeSnapshot(this);

		LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

		Clipboard cb = new Clipboard(getDisplay());
		Object content = cb.getContents(transfer);
		cb.dispose();

		if (content == null) {
			return;
		}

		var project = getEditorInput().getFile().getProject();

		var copyElements = ((IStructuredSelection) content).toArray();

		List<ObjectModel> pasteModels = new ArrayList<>();

		// create the copies

		for (var obj : copyElements) {
			if (obj instanceof JSONObject) {
				var data = (JSONObject) obj;
				if (data.has(SCENE_COPY_STAMP)) {

					String type = data.getString("-type");

					var newModel = SceneModel.createModel(type);

					newModel.read(data, project);

					pasteModels.add(newModel);

				}
			}

		}

		// remove the children

		pasteModels = filterChidlren(pasteModels);

		// set new id and editorName

		var sceneModel = getSceneModel();

		var nameComputer = new NameComputer(sceneModel.getDisplayList());

		float[] offsetPoint;

		{
			var minX = Float.MAX_VALUE;
			var minY = Float.MAX_VALUE;

			for (var model : pasteModels) {
				if (model instanceof TransformComponent) {
					var x = TransformComponent.get_x(model);
					var y = TransformComponent.get_y(model);

					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
				}
			}

			offsetPoint = new float[] { minX - pasteX, minY - pasteY };
		}

		for (var model : pasteModels) {
			model.visit(model2 -> {
				model2.setId(UUID.randomUUID().toString());

				var name = VariableComponent.get_variableName(model2);

				name = nameComputer.newName(name);

				VariableComponent.set_variableName(model2, name);
			});

			if (model instanceof TransformComponent) {
				var x = TransformComponent.get_x(model);
				var y = TransformComponent.get_y(model);

				TransformComponent.set_x(model, sceneModel.snapValueX(x - offsetPoint[0]));
				TransformComponent.set_y(model, sceneModel.snapValueY(y - offsetPoint[1]));
			}
		}

		// add to the root object

		for (var model : pasteModels) {
			ParentComponent.utils_addChild(parent, model);
		}

		refreshOutline();

		setSelection(pasteModels);

		setDirty(true);

		var afterData = WorldSnapshotOperation.takeSnapshot(this);

		executeOperation(new WorldSnapshotOperation(beforeData, afterData, "Paste objects."));

	}

	private Display getDisplay() {
		return getEditorSite().getShell().getDisplay();
	}

	public static List<ObjectModel> filterChidlren(List<ObjectModel> models) {
		var result = new ArrayList<>(models);

		for (var i = 0; i < models.size(); i++) {
			for (var j = 0; j < models.size(); j++) {
				if (i != j) {
					var a = models.get(i);
					var b = models.get(j);
					if (ParentComponent.utils_isDescendentOf(a, b)) {
						result.remove(a);
					}
				}
			}
		}

		return result;
	}

	public void reveal(ObjectModel obj) {
		getBroker().sendAll(new RevealObjectMessage(obj));
	}

}
