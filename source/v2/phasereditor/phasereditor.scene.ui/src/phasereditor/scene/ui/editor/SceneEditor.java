package phasereditor.scene.ui.editor;

import static java.util.stream.Collectors.toList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.operations.UndoRedoActionGroup;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.SceneModel;
import phasereditor.scene.core.WorldModel;
import phasereditor.scene.core.codegen.JS6_UnitCodeGenerator;
import phasereditor.scene.core.codegen.SceneCodeBuilder;
import phasereditor.scene.ui.editor.outline.SceneOutlinePage;
import phasereditor.scene.ui.editor.properties.ScenePropertyPage;
import phasereditor.ui.SelectionProviderImpl;

public class SceneEditor extends EditorPart {

	private SceneModel _model;
	private SceneCanvas _sceneCanvas;
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

	public SceneEditor() {
		_outlinerSelectionListener = new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				getScene().setSelection_from_external(event.getStructuredSelection());
			}
		};
		_propertyPages = new ArrayList<>();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {

		JSONObject data = new JSONObject();

		_model.write(data);

		var file = getEditorInput().getFile();

		try (var input = new ByteArrayInputStream(data.toString(2).getBytes())) {

			file.setContents(input, true, false, monitor);

			setDirty(false);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		generateCode(monitor);

	}

	private void generateCode(IProgressMonitor monitor) {
		try {
			IFile sceneFile = getEditorInput().getFile();

			var codePath = sceneFile.getProjectRelativePath().removeFileExtension().addFileExtension("js");

			var codeFile = sceneFile.getProject().getFile(codePath);

			Charset charset;

			if (codeFile.exists()) {
				charset = Charset.forName(codeFile.getCharset());
			} else {
				charset = Charset.forName("UTF-8");
			}

			String replace = null;

			if (codeFile.exists()) {
				byte[] bytes = Files.readAllBytes(codeFile.getLocation().makeAbsolute().toFile().toPath());
				replace = new String(bytes, charset);
			}

			var builder = new SceneCodeBuilder(codeFile);
			var unitDom = builder.build((WorldModel) getSceneModel().getRootObject());

			var codeGenerator = new JS6_UnitCodeGenerator(unitDom);
			var code = codeGenerator.generate(replace);

			ByteArrayInputStream stream = new ByteArrayInputStream(code.getBytes(charset));
			if (codeFile.exists()) {
				codeFile.setContents(stream, IResource.NONE, monitor);
			} else {
				codeFile.create(stream, false, monitor);
				codeFile.setCharset(charset.name(), monitor);
			}
			codeFile.refreshLocal(1, null);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	@Override
	public void doSaveAs() {
		// not supported
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);

		site.setSelectionProvider(new SelectionProviderImpl(true));

		registerUndoActions();

		IFileEditorInput fileInput = (IFileEditorInput) input;

		setPartName(fileInput.getName());

		_model = new SceneModel();

		var file = fileInput.getFile();

		try (var contents = file.getContents()) {

			var data = new JSONObject(new JSONTokener(contents));

			_model.read(data, file.getProject());

		} catch (IOException | CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public IFileEditorInput getEditorInput() {
		return (IFileEditorInput) super.getEditorInput();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		_sceneCanvas = new SceneCanvas(parent, SWT.NONE);

		_sceneCanvas.init(this);

	}

	@Override
	public void setFocus() {
		_sceneCanvas.setFocus();
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

		return super.getAdapter(adapter);
	}

	public SceneCanvas getScene() {
		return _sceneCanvas;
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

	public void setSelection(StructuredSelection selection) {
		getScene().setSelection_from_external(selection);

		if (_outline != null) {
			_outline.setSelection_from_external(selection);
			refreshOutline_basedOnId();
		}

		for (var page : _propertyPages) {
			page.selectionChanged(this, selection);
		}

		_sceneCanvas.redraw();
	}

	public void updatePropertyPagesContentWithSelection() {
		for (var page : _propertyPages) {
			page.selectionChanged(this, getEditorSite().getSelectionProvider().getSelection());
		}
	}

	public void updatePropertyPagesContentWithSelection_basedOnId() {
		var selectedElems = ((IStructuredSelection) getSite().getSelectionProvider().getSelection()).toArray();

		var selectedIds = Arrays.stream(selectedElems).map(e -> ((ObjectModel) e).getId()).collect(toList());

		var root = getSceneModel().getRootObject();

		var newSelectedElems = selectedIds.stream().map(id -> root.findById(id)).filter(o -> o != null).toArray();

		var sel = new StructuredSelection(newSelectedElems);
		setSelection(sel);

		getSite().getSelectionProvider().setSelection(sel);
	}

}
