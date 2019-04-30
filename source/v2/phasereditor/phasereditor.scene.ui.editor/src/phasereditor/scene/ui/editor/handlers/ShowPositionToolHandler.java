package phasereditor.scene.ui.editor.handlers;

import java.util.Set;

import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.interactive.InteractiveTool;
import phasereditor.scene.ui.editor.interactive.PositionTool;

public class ShowPositionToolHandler extends ShowInteractiveToolHander {

	@Override
	protected InteractiveTool[] createTools(SceneEditor editor) {
		return new InteractiveTool[] {

				new PositionTool(editor, true, false),

				new PositionTool(editor, false, true),

				new PositionTool(editor, true, true) 
				
		};
	}

	@Override
	protected Class<?> getToolClass() {
		return PositionTool.class;
	}

	@Override
	protected Set<String> getTools(SceneEditor editor) {
		return Set.of("Position");
	}

}
