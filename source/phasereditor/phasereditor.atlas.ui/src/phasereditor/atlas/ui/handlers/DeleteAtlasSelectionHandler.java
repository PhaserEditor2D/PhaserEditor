package phasereditor.atlas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.atlas.ui.editors.AtlasGeneratorEditor;

public class DeleteAtlasSelectionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		AtlasGeneratorEditor editor = (AtlasGeneratorEditor) HandlerUtil.getActiveEditor(event);
		editor.deleteSelection();
		return null;
	}

}
