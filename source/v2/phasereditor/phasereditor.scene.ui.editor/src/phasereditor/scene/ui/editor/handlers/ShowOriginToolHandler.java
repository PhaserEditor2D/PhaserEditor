package phasereditor.scene.ui.editor.handlers;

import java.util.Set;

import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.interactive.InteractiveTool;
import phasereditor.scene.ui.editor.interactive.OriginTool;

public class ShowOriginToolHandler extends ShowInteractiveToolHander {

	@Override
	protected InteractiveTool[] createTools(SceneEditor editor) {
		return new InteractiveTool[] {

				new OriginTool(editor, true, false),

				new OriginTool(editor, false, true),

				new OriginTool(editor, true, true)

		};
	}

	@Override
	protected Class<?> getToolClass() {
		return OriginTool.class;
	}
	
	@Override
	protected Set<String> getTools(SceneEditor editor) {
		return Set.of("Origin");
	}

}
