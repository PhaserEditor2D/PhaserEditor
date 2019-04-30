package phasereditor.scene.ui.editor.handlers;

import java.util.Set;

import phasereditor.scene.ui.editor.SceneEditor;

public class ShowPositionToolHandler extends ShowInteractiveToolHander {

	@Override
	protected Set<String> getTools(SceneEditor editor) {
		return Set.of("Position");
	}

}
