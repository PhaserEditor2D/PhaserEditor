package phasereditor.lic.internal.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import phasereditor.lic.LicCore;

public class ActivateProductHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		LicCore.openActivationDialog();
		return null;
	}

}
