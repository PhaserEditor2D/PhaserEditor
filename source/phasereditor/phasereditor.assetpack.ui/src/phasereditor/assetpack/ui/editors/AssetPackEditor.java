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
package phasereditor.assetpack.ui.editors;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
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
import org.eclipse.wb.swt.SWTResourceManager;

import phasereditor.assetpack.core.AssetFactory;
import phasereditor.assetpack.core.AssetGroupModel;
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AudioAssetModel;
import phasereditor.assetpack.core.AudioSpriteAssetModel;
import phasereditor.assetpack.core.BinaryAssetModel;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.PhysicsAssetModel;
import phasereditor.assetpack.core.ScriptAssetModel;
import phasereditor.assetpack.core.ShaderAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.TextAssetModel;
import phasereditor.assetpack.core.TilemapAssetModel;
import phasereditor.assetpack.core.VideoAssetModel;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.assetpack.ui.AssetsContentProvider;
import phasereditor.assetpack.ui.editors.operations.AddAssetOperation;
import phasereditor.assetpack.ui.editors.operations.AddSectionOperation;
import phasereditor.assetpack.ui.editors.operations.CompositeOperation;
import phasereditor.assetpack.ui.widgets.AddAudioResourceDialog;
import phasereditor.assetpack.ui.widgets.ImageResourceDialog;
import phasereditor.lic.LicCore;
import phasereditor.project.core.ProjectCore;
import phasereditor.ui.PhaserEditorUI;

public class AssetPackEditor extends EditorPart implements IGotoMarker, IShowInSource {

	public static final String ID = AssetPackCore.ASSET_EDITOR_ID;

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

	private AssetPackModel _model;

	public AssetPackEditor() {
	}

	@Override
	public ShowInContext getShowInContext() {
		return new ShowInContext(getEditorInput(), getSelection());
	}

	public void handleFileRename(IFile file) {
		_model.setFile(file);
		swtRun(() -> {
			super.setInput(new FileEditorInput(file));
			setPartName(_model.getName());
		});
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		_model.save(monitor);
		firePropertyChange(PROP_DIRTY);
		saveEditingPoint();
		refresh();
	}

	@Override
	public void doSaveAs() {
		// not supported
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
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
	public void setInput(IEditorInput input) {
		super.setInput(input);

		FileEditorInput fileInput = getEditorInput();
		IFile file = fileInput.getFile();

		try {
			// we create a model copy detached from the AssetCore registry.
			_model = new AssetPackModel(file);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		_model.addPropertyChangeListener(this::propertyChange);

		setPartName(_model.getName());
	}

	@Override
	public void dispose() {
		if (_model != null && _model.getFile().exists()) {
			saveEditingPoint();
		}
		super.dispose();
	}

	private void swtRun(Runnable run) {
		Display display = getSite().getShell().getDisplay();
		if (display.getThread() == Thread.currentThread()) {
			run.run();
		} else {
			display.asyncExec(run);
		}
	}

	private void propertyChange(@SuppressWarnings("unused") PropertyChangeEvent evt) {
		getEditorSite().getShell().getDisplay().asyncExec(() -> {
			firePropertyChange(PROP_DIRTY);
		});
	}

	@Override
	public boolean isDirty() {
		return _model.isDirty();
	}

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
					IContext context = HelpSystem.getContext("phasereditor.help.assetpackeditor");
					return context;
				}
			};
		}
		return super.getAdapter(adapter);
	}

	TreeViewer _allAssetsViewer;
	private Button _addSectionButton;
	private Button _removeButton;
	private Button _addAssetButton;
	private Button _moveButton;
	private ImageAssetEditorComp _imageAssetEditorComp;
	private Composite _editorsContainer;
	private EmptyAssetEditorComp _emptyAssetEditorComp;
	private SpritesheetAssetEditorComp _spritesheetAssetEditorComp;
	private AudioAssetEditorComp _audioAssetEditorComp;
	private AudioSpriteAssetEditorComp _audioSpriteEditorComp;
	protected PropertyChangeListener _assetKeyListener;
	private AssetSectionEditorComp _sectionEditorComp;
	private AssetGroupEditorComp _groupEditorComp;
	private TilemapAssetEditorComp _tilemapAssetEditorComp;
	private BitmapFontAssetEditorComp _bitmapFontAssetEditorComp;
	private PhysicsAssetEditorComp _physicsAssetEditorComp;
	private AtlasAssetEditorComp _atlasAssetEditorComp;
	private TextAssetEditorComp _textAssetEditorComp;
	private BinaryAssetEditorComp _binaryAssetEditorComp;
	private ScriptAssetEditorComp _scriptAssetEditorComp;
	private Object _editingElement;

	private VideoAssetEditorComp _videoAssetEditorComp;

	private ShaderAssetEditorComp _shaderAssetEditorComp;

	@SuppressWarnings("unused")
	@Override
	public void createPartControl(Composite parent) {
		FillLayout fl_parent = new FillLayout();
		fl_parent.marginWidth = 5;
		fl_parent.marginHeight = 5;
		parent.setLayout(fl_parent);

		_sashForm = new SashForm(parent, SWT.NONE);
		// _sashForm.setBackground(SWTResourceManager.getColor(SWT.COLOR_LIST_BACKGROUND));

		Composite composite = new Composite(_sashForm, SWT.NONE);
		GridLayout gl_composite = new GridLayout(2, false);
		composite.setLayout(gl_composite);

		_labelAssetsTitle = new Label(composite, SWT.NONE);
		_labelAssetsTitle.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		_labelAssetsTitle.setText("Assets");

		Label _label = new Label(composite, SWT.WRAP);
		_label.setText(
				"Declare the assets of your game here. You can drag them to the Preview window. The keys defined here are available (as auto-completion proposals) in the Java Script editor.");
		GridData gd__label = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd__label.widthHint = 20;
		_label.setLayoutData(gd__label);

		_allAssetsViewer = new TreeViewer(composite, SWT.BORDER | SWT.MULTI);
		_allAssetsViewer.setAutoExpandLevel(1);
		Tree tree = _allAssetsViewer.getTree();
		tree.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == SWT.DEL) {
					delPressed();
				}
			}
		});
		GridData gd_tree = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_tree.heightHint = 50;
		tree.setLayoutData(gd_tree);
		_allAssetsViewer.setLabelProvider(AssetLabelProvider.GLOBAL_16);
		_allAssetsViewer.setContentProvider(new AssetsContentProvider());

		Composite composite_2 = new Composite(composite, SWT.NONE);
		RowLayout rl_composite_2 = new RowLayout(SWT.VERTICAL);
		rl_composite_2.marginTop = 0;
		rl_composite_2.marginRight = 0;
		rl_composite_2.marginLeft = 0;
		rl_composite_2.marginBottom = 0;
		rl_composite_2.fill = true;
		composite_2.setLayout(rl_composite_2);
		composite_2.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1));

		_addAssetButton = new Button(composite_2, SWT.NONE);
		_addAssetButton.setText("Add Asset");
		_addAssetButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openNewAssetDialog();
			}
		});

		_addSectionButton = new Button(composite_2, SWT.NONE);
		_addSectionButton.setText("Add Section");
		_addSectionButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openNewSectionDialog();
			}
		});
		_addSectionButton.setLayoutData(new RowData(100, SWT.DEFAULT));

		_previewButton = new Button(composite_2, SWT.NONE);
		_previewButton.setText("Preview");
		_previewButton.setToolTipText("Open the selected asset in the Preview window.");
		_previewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				previewAsset();
			}
		});

		Composite composite_3 = new Composite(composite_2, SWT.NONE);

		_moveButton = new Button(composite_2, SWT.NONE);
		_moveButton.setText("Move");
		_moveButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				moveAssetElement();
			}
		});

		_removeButton = new Button(composite_2, SWT.NONE);
		_removeButton.setText("Remove");

		_editorsContainer = new Composite(_sashForm, SWT.NONE);
		_editorsContainer.setLayout(new StackLayout());

		_emptyAssetEditorComp = new EmptyAssetEditorComp(_editorsContainer, SWT.NONE);

		_imageAssetEditorComp = new ImageAssetEditorComp(_editorsContainer, SWT.NONE);

		_spritesheetAssetEditorComp = new SpritesheetAssetEditorComp(_editorsContainer, SWT.NONE);

		_audioAssetEditorComp = new AudioAssetEditorComp(_editorsContainer, SWT.NONE);

		_videoAssetEditorComp = new VideoAssetEditorComp(_editorsContainer, SWT.NONE);

		_audioSpriteEditorComp = new AudioSpriteAssetEditorComp(_editorsContainer, SWT.NONE);

		_sectionEditorComp = new AssetSectionEditorComp(_editorsContainer, SWT.NONE);

		_groupEditorComp = new AssetGroupEditorComp(_editorsContainer, SWT.NONE);

		_tilemapAssetEditorComp = new TilemapAssetEditorComp(_editorsContainer, SWT.NONE);

		_bitmapFontAssetEditorComp = new BitmapFontAssetEditorComp(_editorsContainer, SWT.NONE);

		_physicsAssetEditorComp = new PhysicsAssetEditorComp(_editorsContainer, SWT.NONE);

		_atlasAssetEditorComp = new AtlasAssetEditorComp(_editorsContainer, SWT.NONE);

		_textAssetEditorComp = new TextAssetEditorComp(_editorsContainer, SWT.NONE);

		_shaderAssetEditorComp = new ShaderAssetEditorComp(_editorsContainer, SWT.NONE);

		_binaryAssetEditorComp = new BinaryAssetEditorComp(_editorsContainer, SWT.NONE);

		_scriptAssetEditorComp = new ScriptAssetEditorComp(_editorsContainer, SWT.NONE);
		_removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onRemoveAssetPressed();
			}
		});

		afterCreatedWidgets();
	}

	protected void previewAsset() {
		Object elem = ((IStructuredSelection) _allAssetsViewer.getSelection()).getFirstElement();
		if (elem instanceof AssetModel) {
			PhaserEditorUI.openPreview(elem);
		}
	}

	protected void delPressed() {
		if (_removeButton.isEnabled()) {
			onRemoveAssetPressed();
		}
	}

	private void afterCreatedWidgets() {
		_labelAssetsTitle.setFont(SWTResourceManager.getBoldFont(_labelAssetsTitle.getFont()));

		AssetPackModel model = getModel();
		_allAssetsViewer.setInput(model);
		updateUIFromSelection();
		_allAssetsViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateUIFromSelection();
			}
		});
		// add an 'asset key' listener to the model. We used this to update the
		// key text in the assets tree. This is fired by the
		// AssetModel.setKey(key)
		_assetKeyListener = new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				_allAssetsViewer.refresh(evt.getSource());
			}
		};
		model.addPropertyChangeListener(AssetPackModel.PROP_ASSET_KEY, _assetKeyListener);
		_allAssetsViewer.getTree().addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				model.removePropertyChangeListener(AssetPackModel.PROP_ASSET_KEY, _assetKeyListener);
			}
		});
		Transfer[] types = { LocalSelectionTransfer.getTransfer() };
		_allAssetsViewer.addDragSupport(DND.DROP_COPY | DND.DROP_MOVE, types, new DragSourceAdapter() {

			@Override
			public void dragStart(DragSourceEvent event) {
				IStructuredSelection selection = (IStructuredSelection) _allAssetsViewer.getSelection();
				LocalSelectionTransfer.getTransfer().setSelection(selection);
			}
		});

		recoverEditingPoint();

		getEditorSite().setSelectionProvider(_allAssetsViewer);

		AssetPackUI.installAssetTooltips(_allAssetsViewer);

		// update the pages if them were created already
		swtRun(this::refresh);
	}

	protected void moveAssetElement() {
		AssetPackUI.launchMoveWizard(_allAssetsViewer.getStructuredSelection());
	}

	void onRemoveAssetPressed() {
		Object[] selection = ((StructuredSelection) _allAssetsViewer.getSelection()).toArray();
		AssetPackUI.launchDeleteWizard(selection);
	}

	protected void openNewSectionDialog() {
		AssetPackModel model = getModel();
		InputDialog dlg = new InputDialog(getSite().getShell(), "New Section", "Enter the section key:",
				model.createKey("section"), new IInputValidator() {

					@Override
					public String isValid(String newText) {
						return model.hasKey(newText) ? "That key already exists, use other." : null;
					}
				});
		if (dlg.open() == Window.OK) {
			// String result = dlg.getValue();
			// AssetSectionModel section = new AssetSectionModel(result, model);
			// model.addSection(section, true);
			// refresh();
			// revealElement(section);

			String sectionName = dlg.getValue();
			executeOperation(new AddSectionOperation(sectionName));
		}
	}

	private void executeOperation(IUndoableOperation op) {
		IOperationHistory history = getEditorSite().getWorkbenchWindow().getWorkbench().getOperationSupport()
				.getOperationHistory();
		try {
			history.execute(op, null, this);
		} catch (ExecutionException e) {
			AssetPackUI.showError(e);
		}
	}

	protected void openNewAssetDialog() {

		if (LicCore.isEvaluationProduct()) {

			IProject project = getEditorInput().getFile().getProject();

			String rule = AssetPackCore.isFreeVersionAllowed(project);
			if (rule != null) {
				LicCore.launchGoPremiumDialogs(rule);
				return;
			}
		}

		try {

			Object selection = ((StructuredSelection) _allAssetsViewer.getSelection()).getFirstElement();

			AssetSectionModel section;
			AssetType initialType = null;
			if (selection instanceof AssetModel) {
				AssetModel asset = (AssetModel) selection;
				initialType = asset.getType();
				section = asset.getSection();
			} else if (selection instanceof AssetGroupModel) {
				AssetGroupModel group = (AssetGroupModel) selection;
				initialType = group.getType();
				section = group.getSection();
			} else {
				section = (AssetSectionModel) selection;
			}

			AssetFactory factory = null;

			if (initialType != null) {
				factory = AssetFactory.getFactory(initialType);
			}

			if (factory == null) {
				AddAssetToPackDialog dlg = new AddAssetToPackDialog(getSite().getShell());

				dlg.setInitialType(initialType);

				if (dlg.open() == Window.OK) {
					factory = dlg.getSelection();
				}
			}

			if (factory != null) {
				List<AssetModel> assets = openNewAssetListDialog(section, factory);

				CompositeOperation op = new CompositeOperation();

				for (AssetModel asset : assets) {
					op.add(new AddAssetOperation(section, asset));
				}

				executeOperation(op);
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private List<AssetModel> openNewAssetListDialog(AssetSectionModel section, AssetFactory factory) throws Exception {
		AssetType type = factory.getType();

		switch (type) {

		case image:
			return openNewImageListDialog(section);
		case audio:
			return openNewAudioListDialog(section);
		default:
			return Collections.singletonList(factory.createAsset(section.getPack().createKey(type.name()), section));
		}
	}

	private List<AssetModel> openNewAudioListDialog(AssetSectionModel section) throws CoreException {
		AssetPackModel pack = getModel();

		List<IFile> audioFiles = pack.discoverAudioFiles();

		Set<IFile> usedFiles = pack.sortFilesByNotUsed(audioFiles);

		Shell shell = getEditorSite().getShell();
		AddAudioResourceDialog dlg = new AddAudioResourceDialog(shell);
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

			for (Entry<String, List<String>> entry : filesMap.entrySet()) {
				Path path = new Path(entry.getKey());
				AudioAssetModel asset = new AudioAssetModel(pack.createKey(path.lastSegment()), section);
				asset.setUrls(entry.getValue());
				list.add(asset);
			}
		}

		return list;
	}

	private List<AssetModel> openNewImageListDialog(AssetSectionModel section) throws CoreException {
		AssetPackModel pack = getModel();
		List<IFile> imageFiles = pack.discoverImageFiles();

		Set<IFile> usedFiles = pack.sortFilesByNotUsed(imageFiles);

		IFile initial = null;

		if (!imageFiles.isEmpty()) {
			initial = imageFiles.get(0);
		}

		Shell shell = getEditorSite().getShell();
		ImageResourceDialog dlg = new ImageResourceDialog(shell);
		dlg.setLabelProvider(AssetPackUI.createFilesLabelProvider(usedFiles, shell));
		dlg.setInput(imageFiles);
		dlg.setObjectName("image");
		dlg.setMulti(true);

		if (initial != null) {
			dlg.setInitial(initial);
		}

		List<AssetModel> list = new ArrayList<>();

		if (dlg.open() == Window.OK) {
			for (Object obj : dlg.getMultipleSelection()) {
				IFile file = (IFile) obj;
				ImageAssetModel asset = new ImageAssetModel(pack.createKey(file), section);
				asset.setUrl(asset.getUrlFromFile(file));
				list.add(asset);
			}
		}

		return list;
	}

	void updateUIFromSelection() {
		IStructuredSelection selection = (IStructuredSelection) _allAssetsViewer.getSelection();
		boolean hasSelection = !selection.isEmpty();
		boolean hasOneElement = hasSelection && selection.toArray().length == 1;
		Object firstElement = selection.getFirstElement();

		AssetType initialType = null;
		if (firstElement instanceof AssetModel) {
			AssetModel asset = (AssetModel) firstElement;
			initialType = asset.getType();
		} else if (firstElement instanceof AssetGroupModel) {
			AssetGroupModel group = (AssetGroupModel) firstElement;
			initialType = group.getType();
		}

		if (initialType == null) {
			_addAssetButton.setText("Add Asset");
		} else {
			_addAssetButton.setText("+ " + initialType);
		}

		// buttons

		_addAssetButton.setEnabled(hasSelection);
		_removeButton.setEnabled(hasSelection);
		_previewButton.setEnabled(hasOneElement && firstElement instanceof AssetModel);

		_moveButton.setEnabled(firstElement != null
				&& (firstElement instanceof AssetModel || firstElement instanceof AssetGroupModel));

		// editors
		updateEditor(hasOneElement ? firstElement : null);
	}

	private void updateEditor(Object element) {
		_editingElement = element;
		Composite editor = _emptyAssetEditorComp;
		if (element != null) {
			if (element instanceof AssetModel) {
				AssetModel asset = (AssetModel) element;
				AssetType assetType = asset.getType();
				switch (assetType) {
				case image:
					_imageAssetEditorComp.setModel((ImageAssetModel) asset);
					editor = _imageAssetEditorComp;
					break;
				case spritesheet:
					_spritesheetAssetEditorComp.setModel((SpritesheetAssetModel) asset);
					editor = _spritesheetAssetEditorComp;
					break;
				case audio:
					_audioAssetEditorComp.setModel((AudioAssetModel) asset);
					editor = _audioAssetEditorComp;
					break;
				case video:
					_videoAssetEditorComp.setModel((VideoAssetModel) asset);
					editor = _videoAssetEditorComp;
					break;
				case audiosprite:
					_audioSpriteEditorComp.setModel((AudioSpriteAssetModel) asset);
					editor = _audioSpriteEditorComp;
					break;
				case tilemap:
					_tilemapAssetEditorComp.setModel((TilemapAssetModel) asset);
					editor = _tilemapAssetEditorComp;
					break;
				case bitmapFont:
					_bitmapFontAssetEditorComp.setModel((BitmapFontAssetModel) asset);
					editor = _bitmapFontAssetEditorComp;
					break;
				case physics:
					_physicsAssetEditorComp.setModel((PhysicsAssetModel) asset);
					editor = _physicsAssetEditorComp;
					break;
				case atlas:
					_atlasAssetEditorComp.setModel((AtlasAssetModel) asset);
					editor = _atlasAssetEditorComp;
					break;
				case text:
				case json:
				case xml:
					_textAssetEditorComp.setModel((TextAssetModel) asset);
					editor = _textAssetEditorComp;
					break;
				case shader:
					_shaderAssetEditorComp.setModel((ShaderAssetModel) asset);
					editor = _shaderAssetEditorComp;
					break;
				case script:
					_scriptAssetEditorComp.setModel((ScriptAssetModel) asset);
					editor = _scriptAssetEditorComp;
					break;
				case binary:
					_binaryAssetEditorComp.setModel((BinaryAssetModel) asset);
					editor = _binaryAssetEditorComp;
					break;
				default:
					break;
				}
			} else if (element instanceof AssetSectionModel) {
				_sectionEditorComp.setModel((AssetSectionModel) element);
				editor = _sectionEditorComp;
			} else if (element instanceof AssetGroupModel) {
				_groupEditorComp.setModel((AssetGroupModel) element);
				editor = _groupEditorComp;
			}
		}

		StackLayout layout = (StackLayout) _editorsContainer.getLayout();
		layout.topControl = editor;
		_editorsContainer.layout();
	}

	public void refresh() {
		_allAssetsViewer.refresh();
	}

	public Object getEditingElement() {
		return _editingElement;
	}

	private static final QualifiedName EDITING_NODE = new QualifiedName("phasereditor.assetpack", "editingNode_v2");
	private Button _previewButton;
	private SashForm _sashForm;
	private Label _labelAssetsTitle;

	public void saveEditingPoint() {
		AssetPackModel pack = getModel();
		if (pack != null) {
			Object elem = _editingElement;
			if (elem != null) {
				IFile file = pack.getFile();
				try {
					file.setPersistentProperty(EDITING_NODE, pack.getStringReference(elem));
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void recoverEditingPoint() {
		AssetPackModel pack = getModel();
		IFile file = pack.getFile();
		try {
			String str = file.getPersistentProperty(EDITING_NODE);
			if (str != null) {
				Object elem = pack.getElementFromStringReference(str);
				if (elem != null) {
					if (elem instanceof AssetGroupModel) {
						revealElement(((AssetGroupModel) elem).getSection());
					} else if (elem instanceof AssetModel) {
						revealElement(((AssetModel) elem).getSection());
						revealElement(((AssetModel) elem).getGroup());
					}
					revealElement(elem);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void revealElement(Object elem) {
		if (elem == null) {
			return;
		}
		Object reveal = elem instanceof IAssetElementModel ? ((IAssetElementModel) elem).getAsset() : elem;
		_allAssetsViewer.getTree().setFocus();
		_allAssetsViewer.setSelection(new StructuredSelection(reveal), true);
	}

	public ISelection getSelection() {
		return _allAssetsViewer.getSelection();
	}

	@Override
	public void setFocus() {
		_allAssetsViewer.getControl().setFocus();
	}

	public TreeViewer getViewer() {
		return _allAssetsViewer;
	}

	public void updateAssetEditor() {
		updateEditor(_editingElement);
	}
}
