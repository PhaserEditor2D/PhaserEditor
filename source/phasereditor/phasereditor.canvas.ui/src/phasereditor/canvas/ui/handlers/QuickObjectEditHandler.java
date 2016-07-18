package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.PGridDialog;
import phasereditor.canvas.ui.editors.grid.PGridModel;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class QuickObjectEditHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
		Object e = sel.getFirstElement();
		if (e != null) {
			PGridDialog dlg = new PGridDialog(HandlerUtil.getActiveShell(event));
			PGridModel model = ((IObjectNode)e).getControl().getPropertyModel();
			dlg.setModel(model);
			dlg.open();
		}
		
		return null;
	}

}
