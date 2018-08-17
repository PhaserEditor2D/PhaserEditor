package phasereditor.atlas.ui.editor.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.atlas.ui.editor.TexturePackerEditor;

public class ShowTexturePackerEditorSettingsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		TexturePackerEditor editor = (TexturePackerEditor) HandlerUtil.getActiveEditor(event);
		ISelectionProvider provider = editor.getEditorSite().getSelectionProvider();
		if (editor.getOutliner() != null) {
			provider = editor.getOutliner();
		}

		provider.setSelection(new StructuredSelection(editor.getModel()));

		try {
			IWorkbenchPage page = editor.getEditorSite().getPage();
			IViewPart view = page.showView("org.eclipse.ui.views.PropertySheet", null,
					IWorkbenchPage.VIEW_CREATE);
			page.activate(view);
		} catch (PartInitException e) {
			e.printStackTrace();
		}

		return null;
	}

}
