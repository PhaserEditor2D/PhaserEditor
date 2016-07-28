package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.ArcadeBodyModel;
import phasereditor.canvas.core.CircleArcadeBodyModel;
import phasereditor.canvas.core.RectArcadeBodyModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.operations.ChangeBodyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.SelectOperation;
import phasereditor.canvas.ui.shapes.BaseSpriteControl;
import phasereditor.canvas.ui.shapes.ISpriteNode;

public class SetArcadeBodyHandler extends AbstractHandler {

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
			String id = control.getId();

			ArcadeBodyModel body;

			if (event.getCommand().getId().contains("Circle")) {
				CircleArcadeBodyModel circle = new CircleArcadeBodyModel();
				circle.setRadius(control.getTextureWidth() / 2);
				body = circle;
			} else {
				RectArcadeBodyModel rect = new RectArcadeBodyModel();
				rect.setWidth(control.getTextureWidth());
				rect.setHeight(control.getTextureHeight());
				body = rect;
			}

			operations.add(new ChangeBodyOperation(id, body));

			select.add(id);
		}

		operations.add(select);

		canvas.getUpdateBehavior().executeOperations(operations);

		return null;
	}

}
