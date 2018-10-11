package phasereditor.inspect.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.inspect.ui.views.PhaserHierarchyView;

public class ChangeTypeHierarchyHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		var view = (PhaserHierarchyView) HandlerUtil.getActivePart(event);

		var showSubtypes = event.getCommand().getId().equals("phasereditor.inspect.ui.showSubHierarchy");
		
		view.setShowSubTypes(showSubtypes);
		
		return null;
	}

}
