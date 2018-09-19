package phasereditor.scene.ui.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;

import phasereditor.scene.core.SceneModel;
import phasereditor.scene.ui.editor.outline.SceneOutline;
import phasereditor.scene.ui.editor.properties.ScenePropertiesPage;
import phasereditor.ui.SelectionProviderImpl;

public class SceneEditor extends EditorPart {

	private SceneModel _model;
	private SceneCanvas _canvas;
	private SceneOutline _outline;

	public SceneEditor() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);

		site.setSelectionProvider(new SelectionProviderImpl(true));

		setPartName(((IFileEditorInput) input).getName());

		_model = new SceneModel();
	}

	@Override
	public boolean isDirty() {
		return false;
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
			return _outline = new SceneOutline(this);
		}

		return super.getAdapter(adapter);
	}

	public SceneCanvas getCanvas() {
		return _canvas;
	}

	public void setOutline(SceneOutline outline) {
		_outline = outline;
	}

	public SceneOutline getOutline() {
		return _outline;
	}

}
