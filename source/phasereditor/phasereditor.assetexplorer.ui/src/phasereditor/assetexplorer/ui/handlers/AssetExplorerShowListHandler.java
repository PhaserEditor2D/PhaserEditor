package phasereditor.assetexplorer.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.assetexplorer.ui.views.AssetExplorer;

public class AssetExplorerShowListHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		AssetExplorer view = (AssetExplorer) HandlerUtil.getActivePart(event);
		view.showList();
		return null;
	}

}
