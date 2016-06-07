package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.CanvasEditor;

public class DeleteHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		boolean paletteFocused = editor.getPalette().getViewer().getTable().isFocusControl();
		if (paletteFocused) {
			editor.getPalette().deleteSelected();
		} else {
			editor.getCanvas().deleteSelected();
		}
		return null;
	}

}
