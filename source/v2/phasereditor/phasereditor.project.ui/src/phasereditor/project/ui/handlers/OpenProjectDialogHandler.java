package phasereditor.project.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.project.ui.wizards.OpenProjectDialog;

public class OpenProjectDialogHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		var dlg = new OpenProjectDialog(HandlerUtil.getActiveShell(event));
		dlg.open();

		return null;
	}

}
