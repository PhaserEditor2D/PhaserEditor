package phasereditor.scene.ui.editor.handlers;

import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.interactive.AngleLineTool;
import phasereditor.scene.ui.editor.interactive.AngleTool;
import phasereditor.scene.ui.editor.interactive.InteractiveTool;

public class ShowAngleToolHandler extends ShowInteractiveToolHander {

	@Override
	protected InteractiveTool[] createTools(SceneEditor editor) {
		return new InteractiveTool[] {

				new AngleTool(editor, 1),

				new AngleTool(editor, 2),

				new AngleTool(editor, 3),

				new AngleLineTool(editor, true),

				new AngleLineTool(editor, false)

		};
	}

	@Override
	protected Class<?> getToolClass() {
		return AngleTool.class;
	}

}
