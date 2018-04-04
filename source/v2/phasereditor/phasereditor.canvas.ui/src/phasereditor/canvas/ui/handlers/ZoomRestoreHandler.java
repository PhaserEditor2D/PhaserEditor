package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import javafx.scene.Scene;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.behaviors.ZoomBehavior;

public class ZoomRestoreHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		ZoomBehavior zoom = editor.getCanvas().getZoomBehavior();
		Scene scene = editor.getCanvas().getScene();
		zoom.zoom(1, scene.getWidth() / 2, scene.getHeight() / 2);
		return null;
	}

}
