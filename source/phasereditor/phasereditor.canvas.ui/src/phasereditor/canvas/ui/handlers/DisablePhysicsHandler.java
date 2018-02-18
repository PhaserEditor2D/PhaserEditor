package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.operations.ChangeBodyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.SelectOperation;
import phasereditor.canvas.ui.shapes.BaseSpriteControl;
import phasereditor.canvas.ui.shapes.ISpriteNode;

public class DisablePhysicsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);

		ObjectCanvas canvas = editor.getCanvas();

		Object[] sel = ((IStructuredSelection) HandlerUtil.getCurrentSelection(event)).toArray();

		CompositeOperation operations = new CompositeOperation();

		SelectOperation select = new SelectOperation();

		for (Object obj : sel) {
			ISpriteNode node = (ISpriteNode) obj;

			BaseSpriteControl<?> control = node.getControl();

			if (control.getModel().getBody() == null) {
				continue;
			}

			String id = control.getId();

			operations.add(new ChangeBodyOperation(id, null));

			select.add(id);
		}

		if (operations.isEmpty()) {
			return null;
		}

		operations.add(select);

		canvas.getHandlerBehavior().clear();

		canvas.getUpdateBehavior().executeOperations(operations);

		return null;
	}

}
