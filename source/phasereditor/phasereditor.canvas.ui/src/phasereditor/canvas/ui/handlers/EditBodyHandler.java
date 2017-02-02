package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.BodyModel;
import phasereditor.canvas.core.CircleArcadeBodyModel;
import phasereditor.canvas.core.RectArcadeBodyModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.shapes.ISpriteNode;

public class EditBodyHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISpriteNode sprite = (ISpriteNode) ((IStructuredSelection) HandlerUtil.getCurrentSelection(event))
				.getFirstElement();

		if (!sprite.getModel().isOverriding(BaseSpriteModel.PROPSET_PHYSICS)) {
			MessageDialog.openWarning(HandlerUtil.getActiveShell(event), "Physics",
					"Cannot change the physics of this prefab instance.");
			return null;
		}

		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		ObjectCanvas canvas = editor.getCanvas();

		BodyModel body = sprite.getModel().getBody();
		if (body instanceof RectArcadeBodyModel) {
			canvas.getHandlerBehavior().editArcadeRectBody(sprite);
		} else if (body instanceof CircleArcadeBodyModel) {
			canvas.getHandlerBehavior().editArcadeCircleBody(sprite);
		}
		return null;
	}

}
