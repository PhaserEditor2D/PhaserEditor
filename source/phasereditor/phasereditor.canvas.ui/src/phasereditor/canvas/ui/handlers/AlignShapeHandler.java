package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class AlignShapeHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);

		String[] split = event.getCommand().getId().split("\\.");
		String place = split[split.length - 1];

		Object[] elems = sel.toArray();

		IObjectNode pivot = (IObjectNode) elems[0];

		if (elems.length == 1) {
			pivot = pivot.getControl().getGroup();
		} else {
			for (Object elem : elems) {
				BaseObjectModel pivotModel = pivot.getModel();
				BaseObjectControl<?> pivotControl = pivot.getControl();

				IObjectNode inode = (IObjectNode) elem;
				BaseObjectModel model = inode.getModel();
				BaseObjectControl<?> control = inode.getControl();

				boolean update = false;
				switch (place) {
				case "left":
					update = model.getX() < pivotModel.getX();
					break;
				case "top":
					update = model.getY() < pivotModel.getY();
					break;
				case "right":
					update = model.getX() + control.getWidth() > pivotModel.getX() + pivotControl.getWidth();
					break;
				case "bottom":
					update = model.getY() + control.getHeight() > pivotModel.getY() + pivotControl.getHeight();
					break;
				default:
					break;
				}

				if (update) {
					pivot = inode;
				}
			}
		}

		align(place, elems, pivot);

		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		editor.getCanvas().getSelectionBehavior().updateSelectedNodes();

		return null;
	}

	private static void align(String place, Object[] elems, IObjectNode pivot) {
		BaseObjectControl<?> pivotControl = pivot.getControl();
		BaseObjectModel pivotModel = pivot.getModel();

		for (Object elem : elems) {
			IObjectNode inode = (IObjectNode) elem;
			BaseObjectModel model = inode.getModel();
			BaseObjectControl<?> control = inode.getControl();

			switch (place) {
			case "left":
				model.setX(pivotModel.getX());
				break;
			case "top":
				model.setY(pivotModel.getY());
				break;
			case "right":
				model.setX(pivotModel.getX() + pivotControl.getWidth() - control.getWidth());
				break;
			case "bottom":
				model.setY(pivotModel.getY() + pivotControl.getHeight() - control.getHeight());
				break;
			default:
				break;
			}

			control.updateFromModel();
		}
	}

}
