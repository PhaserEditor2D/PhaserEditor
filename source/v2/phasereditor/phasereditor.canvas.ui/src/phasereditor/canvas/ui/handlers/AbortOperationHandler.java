package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;

public class AbortOperationHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		ObjectCanvas canvas = editor.getCanvas();

		// cancel dragging and selection

		if (canvas.getDragBehavior().isDragging()) {
			canvas.getDragBehavior().abort();
		} else if (canvas.getHandlerBehavior().isEditing()) {
			canvas.getHandlerBehavior().clear();
		} else {
			canvas.getSelectionBehavior().abort();
		}
		return null;
	}

}
