package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class EditPivotHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		IStructuredSelection sel = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);

		IObjectNode elem = (IObjectNode) sel.getFirstElement();
		if (!elem.getModel().isOverriding(BaseObjectModel.PROPSET_PIVOT)) {
			MessageDialog.openWarning(HandlerUtil.getActiveShell(event), "Pivot",
					"Cannot change the pivot of this prefab instance.");
			return null;
		}

		editor.getCanvas().getHandlerBehavior().editPivot(elem);
		return null;
	}

}
