package phasereditor.scene.ui.editor.handlers;

import java.util.Set;

import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.interactive.AngleLineTool;
import phasereditor.scene.ui.editor.interactive.AngleTool;
import phasereditor.scene.ui.editor.interactive.InteractiveTool;

public class ShowAngleToolHandler extends ShowInteractiveToolHander {

	@Override
	protected InteractiveTool[] createTools(SceneEditor editor) {
		return new InteractiveTool[] {

				new AngleTool(editor),

				new AngleLineTool(editor, true),

				new AngleLineTool(editor, false)

		};
	}
	
	@Override
	protected Set<String> getTools(SceneEditor editor) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Class<?> getToolClass() {
		return AngleTool.class;
	}

}
