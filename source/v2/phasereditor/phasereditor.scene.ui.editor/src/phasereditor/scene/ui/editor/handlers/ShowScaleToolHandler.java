package phasereditor.scene.ui.editor.handlers;

import java.util.Set;

import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.interactive.InteractiveTool;
import phasereditor.scene.ui.editor.interactive.ScaleTool;

public class ShowScaleToolHandler extends ShowInteractiveToolHander {

	@Override
	protected InteractiveTool[] createTools(SceneEditor editor) {
		return new InteractiveTool[] {

				new ScaleTool(editor, true, false),

				new ScaleTool(editor, false, true),

				new ScaleTool(editor, true, true)

		};
	}

	@Override
	protected Class<?> getToolClass() {
		return ScaleTool.class;
	}

	@Override
	protected Set<String> getTools(SceneEditor editor) {
		return Set.of("Scale");
	}

}
