package phasereditor.assetpack.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.assetpack.ui.editors.AssetPackEditor;

public class RedoHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IOperationHistory history = HandlerUtil.getActiveWorkbenchWindow(event).getWorkbench().getOperationSupport()
				.getOperationHistory();
		history.redo(AssetPackEditor.UNDO_CONTEXT, null, HandlerUtil.getActiveEditor(event));
		return null;
	}

}
