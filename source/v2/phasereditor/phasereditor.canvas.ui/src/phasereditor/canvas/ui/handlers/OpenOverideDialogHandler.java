package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.grid.PGridOverrideProperty;
import phasereditor.canvas.ui.editors.grid.editors.CanvasPGridEditingSupport;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class OpenOverideDialogHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		IObjectNode obj = (IObjectNode) HandlerUtil.getCurrentStructuredSelection(event).getFirstElement();
		BaseObjectControl<?> control = obj.getControl();
		PGridOverrideProperty prop = control.getOverride_property();

		Object value = CanvasPGridEditingSupport.openOverridePropertiesDialog(prop, HandlerUtil.getActiveShell(event));

		if (value != prop.getValue()) {
			editor.getPropertyGrid().getEditSupport().executeChangePropertyValueOperation(value, prop);
		}

		return null;
	}

}
