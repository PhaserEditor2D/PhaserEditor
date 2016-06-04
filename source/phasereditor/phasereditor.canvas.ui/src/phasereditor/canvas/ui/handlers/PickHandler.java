package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class PickHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
		boolean pick = event.getCommand().getId().endsWith(".pick");

		for (Object elem : sel.toArray()) {
			IObjectNode inode = (IObjectNode) elem;
			BaseObjectControl<?> control = inode.getControl();
			control.getEditorPick_property().setValue(Boolean.valueOf(pick));
			control.getCanvas().getUpdateBehavior().update_Grid_from_PropertyChange(control.getEditorPick_property());
		}

		return null;

	}

}
