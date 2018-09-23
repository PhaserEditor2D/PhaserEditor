package phasereditor.scene.ui.editor;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.scene.core.SceneModel;
import phasereditor.scene.ui.editor.outline.SceneOutlinePage;
import phasereditor.scene.ui.editor.properties.ScenePropertiesPage;
import phasereditor.ui.SelectionProviderImpl;

public class SceneEditor extends EditorPart {

	private SceneModel _model;
	private SceneCanvas _canvas;
	private SceneOutlinePage _outline;
	private boolean _dirty;
	ISelectionChangedListener _outlinerSelectionListener;

	public SceneEditor() {
		_outlinerSelectionListener = new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				getCanvas().setSelection_from_external(event.getStructuredSelection());
			}
		};
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
		_canvas = new SceneCanvas(parent, SWT.NONE);

		_canvas.init(this);

	}

	@Override
	public void setFocus() {
		_canvas.setFocus();
	}

	public SceneModel getSceneModel() {
		return _model;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getAdapter(Class adapter) {

		if (adapter == IPropertySheetPage.class) {
			return new ScenePropertiesPage(this);
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

	public SceneCanvas getCanvas() {
		return _canvas;
	}

	public void removeOutline() {
		_outline.removeSelectionChangedListener(_outlinerSelectionListener);
		_outline = null;
	}

	public SceneOutlinePage getOutline() {
		return _outline;
	}

	public void setDirty(boolean dirty) {
		_dirty = dirty;
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public boolean isDirty() {
		return _dirty;
	}

	public void refreshOtuline() {
		if (_outline != null) {
			_outline.refresh();
		}
	}

}
