package phasereditor.webrun.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import phasereditor.webrun.core.WebRunCore;
import phasereditor.webrun.ui.WebRunUI;

public class BrowsePhaserExamplesHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		WebRunUI.openBrowser("http://localhost:" + WebRunCore.getServerPort() + "/phaser-examples");

		return null;
	}

}
