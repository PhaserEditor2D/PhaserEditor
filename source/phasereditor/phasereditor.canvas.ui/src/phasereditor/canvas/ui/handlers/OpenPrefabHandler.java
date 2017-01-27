package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;

import phasereditor.canvas.ui.shapes.IObjectNode;

public class OpenPrefabHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = HandlerUtil.getCurrentStructuredSelection(event);
		for (Object obj : sel.toArray()) {
			IObjectNode node = (IObjectNode) obj;
			IFile file = node.getModel().getPrefab().getFile();
			if (file.exists()) {
				try {
					IDE.openEditor(HandlerUtil.getActiveWorkbenchWindow(event).getActivePage(), file);
				} catch (PartInitException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		}
		return null;
	}

}
