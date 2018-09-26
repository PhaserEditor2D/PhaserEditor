package phasereditor.scene.ui.editor.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.scene.ui.editor.SceneEditor;

public class DeleteHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		SceneEditor editor = (SceneEditor) HandlerUtil.getActiveEditor(event);

		editor.getScene().delete();

		return null;
	}

}
