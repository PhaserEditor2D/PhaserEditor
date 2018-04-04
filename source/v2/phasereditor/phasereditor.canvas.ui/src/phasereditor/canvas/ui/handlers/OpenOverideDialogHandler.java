package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.grid.PGridOverrideProperty;
import phasereditor.canvas.ui.editors.grid.editors.PGridEditingSupport;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class OpenOverideDialogHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IObjectNode obj = (IObjectNode) HandlerUtil.getCurrentStructuredSelection(event).getFirstElement();
		BaseObjectControl<?> control = obj.getControl();
		PGridOverrideProperty prop = control.getOverride_property();

		Object value = PGridEditingSupport.openOverridePropertiesDialog(prop, HandlerUtil.getActiveShell(event));

		if (value != prop.getValue()) {
			PGridEditingSupport.executeChangePropertyValueOperation(value, prop);
		}

		return null;
	}

}
