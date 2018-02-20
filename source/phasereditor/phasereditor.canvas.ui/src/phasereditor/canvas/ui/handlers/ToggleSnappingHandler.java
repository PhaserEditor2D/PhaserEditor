package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;

public class ToggleSnappingHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);

		ObjectCanvas canvas = editor.getCanvas();

		EditorSettings model = canvas.getSettingsModel();

		model.setEnableStepping(!model.isEnableStepping());

		canvas.getHandlerBehavior().update();
		
		canvas.getPaintBehavior().repaint();

		return null;
	}

}
