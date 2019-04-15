package phasereditor.scene.ui.editor.handlers;

import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.scene.core.TileSpriteModel;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.SceneUIEditor;
import phasereditor.scene.ui.editor.interactive.InteractiveTool;
import phasereditor.scene.ui.editor.interactive.TileSizeTool;

public class ResizeTileSpriteHandler extends ShowInteractiveToolHander {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		var editor = (SceneEditor) HandlerUtil.getActiveEditor(event);

		var morphing = editor.getSelectionList().stream()

				.filter(model -> !(model instanceof TileSpriteModel))

				.count() > 0;

		SceneUIEditor.action_MorphObjectsToNewType(editor, editor.getSelectionList(), TileSpriteModel.TYPE);

		if (morphing) {

			editor.getScene().setInteractiveTools(createTools(editor));
			editor.setInteractiveTools(getTools(editor));

			return null;
		}

		return super.execute(event);
	}

	@Override
	protected InteractiveTool[] createTools(SceneEditor editor) {
		return new InteractiveTool[] {

				new TileSizeTool(editor, true, false),

				new TileSizeTool(editor, false, true),

				new TileSizeTool(editor, true, true)

		};
	}

	@Override
	protected Class<?> getToolClass() {
		return TileSizeTool.class;
	}

	@Override
	protected Set<String> getTools(SceneEditor editor) {
		return Set.of("TileSize");
	}

}
