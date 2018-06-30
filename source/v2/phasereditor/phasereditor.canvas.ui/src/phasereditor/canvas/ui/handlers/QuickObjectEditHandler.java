package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.CanvasPGridDialog;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.ui.properties.PGridModel;

public class QuickObjectEditHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
		Object e = sel.getFirstElement();
		if (e != null) {
			CanvasPGridDialog dlg = new CanvasPGridDialog(HandlerUtil.getActiveShell(event));
			BaseObjectControl<?> control = ((IObjectNode)e).getControl();
			PGridModel model = control.getPropertyModel();
			dlg.setModel(model);
			dlg.setCanvas(control.getCanvas());
			dlg.open();
		}
		
		return null;
	}

}
