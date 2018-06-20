package phasereditor.inspect.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.inspect.core.jsdoc.IPhaserMember;
import phasereditor.inspect.ui.InspectUI;

public class ShowPhaserJsdocHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
		run(selection);
		return null;
	}

	public static void run(IStructuredSelection selection) {
		Object elem = selection.getFirstElement();
		IPhaserMember member = Adapters.adapt(elem, IPhaserMember.class);
		InspectUI.showJavaDoc(member);
	}

}
