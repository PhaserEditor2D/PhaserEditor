package phasereditor.scene.ui.editor.handlers;

import static java.lang.System.out;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

public class DuplicateSelectedObjectsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		var sel = HandlerUtil.getCurrentStructuredSelection(event);
		
		out.println("Duplicate " + sel.toList().size());
		
		return null;
	}

}
