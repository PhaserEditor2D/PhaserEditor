package phasereditor.canvas.ui.handlers;

import static java.lang.System.out;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.CanvasEditor;

public class UndoHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IOperationHistory history = HandlerUtil.getActiveWorkbenchWindow(event).getWorkbench().getOperationSupport()
				.getOperationHistory();
		IStatus status = history.undo(CanvasEditor.UNDO_CONTEXT, null, HandlerUtil.getActiveEditor(event));
		out.println("Undo status: " + status);
		return null;
	}

}
