package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.BodyModel;
import phasereditor.canvas.core.CircleArcadeBodyModel;
import phasereditor.canvas.core.RectArcadeBodyModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.SelectionNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;

public class EditBodyHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		ObjectCanvas canvas = editor.getCanvas();
		SelectionNode node = (SelectionNode) canvas.getSelectionPane().getChildren().get(0);
		ISpriteNode sprite = (ISpriteNode) node.getObjectNode();
		BodyModel body = sprite.getModel().getBody();
		if (body instanceof RectArcadeBodyModel) {
			node.setEnableArcadeRectHandlers(true);
		} else if (body instanceof CircleArcadeBodyModel) {
			node.setEnableArcadeCircleHandlers(true);
		}
		return null;
	}

}
