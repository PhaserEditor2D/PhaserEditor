package phasereditor.webrun.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.webrun.ui.WebRunUI;

public class RunInternalHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		WebRunUI.openInternalBrowser(HandlerUtil.getCurrentSelection(event));
		return null;
	}

}
