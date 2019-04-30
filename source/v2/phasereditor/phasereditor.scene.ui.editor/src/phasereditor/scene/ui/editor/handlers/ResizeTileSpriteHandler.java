package phasereditor.scene.ui.editor.handlers;

import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.scene.core.TileSpriteModel;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.SceneUIEditor;

public class ResizeTileSpriteHandler extends ShowInteractiveToolHander {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		var editor = (SceneEditor) HandlerUtil.getActiveEditor(event);

		var morphing = editor.getSelectionList().stream()

				.filter(model -> model.allowMorphTo(TileSpriteModel.TYPE))

				.count() > 0;

		if (morphing) {

			SceneUIEditor.action_MorphObjectsToNewType(editor, editor.getSelectionList(), TileSpriteModel.TYPE);

			editor.setInteractiveTools(getTools(editor));

			return null;
		}

		return super.execute(event);
	}

	@Override
	protected Set<String> getTools(SceneEditor editor) {
		return Set.of("TileSize");
	}

}
