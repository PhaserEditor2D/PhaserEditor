package phasereditor.scene.ui.editor.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.scene.core.TileSpriteModel;
import phasereditor.scene.ui.editor.SceneCanvas;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.SceneUIEditor;
import phasereditor.scene.ui.editor.interactive.TileSizeTool;

public class ResizeTileSpriteHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		var editor = (SceneEditor) HandlerUtil.getActiveEditor(event);

		SceneCanvas scene = editor.getScene();
		SceneUIEditor.action_MorphObjectsToNewType(editor, editor.getSelectionList(), TileSpriteModel.TYPE);

		if (scene.hasInteractiveTool(TileSizeTool.class)) {
			scene.setInteractiveTools();
		} else {
			scene.setInteractiveTools(

					new TileSizeTool(editor, true, false),

					new TileSizeTool(editor, false, true),

					new TileSizeTool(editor, true, true)

			);
		}

		return null;
	}

}
