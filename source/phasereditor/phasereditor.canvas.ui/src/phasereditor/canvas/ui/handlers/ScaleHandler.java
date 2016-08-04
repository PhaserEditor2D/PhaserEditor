package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ScaleHandlersGroup;
import phasereditor.canvas.ui.editors.SelectionNode;

public class ScaleHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		SelectionNode node = (SelectionNode) editor.getCanvas().getSelectionPane().getChildren().get(0);
		node.showHandlers(ScaleHandlersGroup.class);
		return null;
	}

}
