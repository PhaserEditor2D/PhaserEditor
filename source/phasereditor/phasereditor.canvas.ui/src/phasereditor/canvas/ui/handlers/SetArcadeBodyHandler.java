package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import phasereditor.canvas.core.ArcadeBodyModel;
import phasereditor.canvas.core.BodyModel;
import phasereditor.canvas.core.CircleArcadeBodyModel;
import phasereditor.canvas.core.RectArcadeBodyModel;
import phasereditor.canvas.ui.editors.ArcadeCircleBodyHandlersGroup;
import phasereditor.canvas.ui.editors.ArcadeRectBodyHandlersGroup;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.SelectionNode;
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
				circle.setRadius(Math.min(control.getTextureWidth() / 2, control.getTextureHeight() / 2));
				body = circle;
			} else {
				body = new RectArcadeBodyModel();
			}

			operations.add(new ChangeBodyOperation(id, body));

			select.add(id);
		}

		operations.add(select);

		canvas.getUpdateBehavior().executeOperations(operations);

		// if there is only one node selected, then edit the body

		{
			ObservableList<Node> list = canvas.getSelectionPane().getChildren();
			if (list.size() == 1) {
				SelectionNode node = (SelectionNode) list.get(0);
				BodyModel body = ((ISpriteNode) node.getObjectNode()).getModel().getBody();
				if (body instanceof RectArcadeBodyModel) {
					node.showHandlers(ArcadeRectBodyHandlersGroup.class);
				} else if (body instanceof CircleArcadeBodyModel) {
					node.showHandlers(ArcadeCircleBodyHandlersGroup.class);
				}
			}
		}

		return null;
	}

}
