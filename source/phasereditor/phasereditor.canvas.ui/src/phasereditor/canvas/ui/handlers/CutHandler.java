package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class CutHandler extends CopyHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		super.execute(event);

		DeleteHandler.executeDelete(event);

		return null;
	}

}
