package phasereditor.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.ui.ISourceLocation;
import phasereditor.ui.PhaserEditorUI;

public class ShowSourceCodeHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = HandlerUtil.getCurrentStructuredSelection(event);
		Object obj = sel.getFirstElement();
		ISourceLocation location = Adapters.adapt(obj, ISourceLocation.class);
		PhaserEditorUI.openJSEditor(location.getLine(), -1, location.getFilePath());
		return null;
	}

}
