package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.CanvasEditor;

public class ZoomHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		boolean zoomIn = event.getCommand().getId().endsWith("zoomIn");
		
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		editor.getCanvas().getZoomBehavior().zoomKey(zoomIn);
		
		return null;
	}

}
