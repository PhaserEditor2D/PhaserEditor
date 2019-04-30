package phasereditor.scene.ui.editor.handlers;

import java.util.Set;

import phasereditor.scene.ui.editor.SceneEditor;

public class ShowOriginToolHandler extends ShowInteractiveToolHander {

	@Override
	protected Set<String> getTools(SceneEditor editor) {
		return Set.of("Origin");
	}

}
