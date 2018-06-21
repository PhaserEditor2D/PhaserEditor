package phasereditor.inspect.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.inspect.core.examples.PhaserExampleModel;
import phasereditor.ui.ISourceLocation;
import phasereditor.webrun.ui.WebRunUI;

public class RunPhaserExampleHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
		run(selection);
		return null;
	}

	public static void run(IStructuredSelection selection) {
		Object elem = selection.getFirstElement();

		PhaserExampleModel example = Adapters.adapt(elem, PhaserExampleModel.class);

		ISourceLocation location = Adapters.adapt(elem, ISourceLocation.class);
		int line = location == null ? -1 : location.getLine();

		WebRunUI.openExampleInBrowser(example, line);
	}

}
