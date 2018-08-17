package phasereditor.atlas.ui.editor.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.atlas.ui.editor.TexturePackerEditor;

public class BuildAtlasHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		TexturePackerEditor editor = (TexturePackerEditor) HandlerUtil.getActiveEditor(event);
		editor.manuallyBuild();
		return null;
	}

}
