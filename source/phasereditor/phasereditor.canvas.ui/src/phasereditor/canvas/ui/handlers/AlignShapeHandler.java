package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import javafx.geometry.Rectangle2D;
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

		IObjectNode first = (IObjectNode) elems[0];
		Rectangle2D pivot = makeRect(first);

		double sum = 0;

		if (elems.length == 1) {
			// the rect of the parent starts in 0,0
			pivot = makeRect(0, 0, first.getGroup());
			switch (place) {
			case "center":
				sum = pivot.getMinX() + pivot.getWidth() / 2;
				break;
			case "middle":
				sum = pivot.getMinY() + pivot.getHeight() / 2;
				break;
			default:
				break;
			}
		} else {
			for (Object elem : elems) {
				IObjectNode inode = (IObjectNode) elem;
				BaseObjectModel model = inode.getModel();
				BaseObjectControl<?> control = inode.getControl();

				boolean update = false;
				switch (place) {
				case "left":
					update = model.getX() < pivot.getMinX();
					break;
				case "top":
					update = model.getY() < pivot.getMinY();
					break;
				case "right":
					update = model.getX() + control.getWidth() > pivot.getMinX() + pivot.getWidth();
					break;
				case "bottom":
					update = model.getY() + control.getHeight() > pivot.getMinY() + pivot.getHeight();
					break;
				case "center":
					sum += model.getX() + control.getWidth() / 2;
					break;
				case "middle":
					sum += model.getY() + control.getHeight() / 2;
					break;
				default:
					break;
				}

				if (update) {
					pivot = makeRect(inode);
				}
			}
		}

		double avg = sum / elems.length;
		align(place, elems, pivot, avg);

		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		editor.getCanvas().getSelectionBehavior().updateSelectedNodes();

		return null;
	}

	private static Rectangle2D makeRect(double x, double y, IObjectNode node) {
		return new Rectangle2D(x, y, node.getControl().getWidth(), node.getControl().getHeight());
	}

	private static Rectangle2D makeRect(IObjectNode node) {
		return makeRect(node.getModel().getX(), node.getModel().getY(), node);
	}

	private static void align(String place, Object[] elems, Rectangle2D pivot, double avg) {
		for (Object elem : elems) {
			IObjectNode inode = (IObjectNode) elem;
			BaseObjectModel model = inode.getModel();
			BaseObjectControl<?> control = inode.getControl();

			switch (place) {
			case "left":
				model.setX(pivot.getMinX());
				break;
			case "top":
				model.setY(pivot.getMinY());
				break;
			case "right":
				model.setX(pivot.getMinX() + pivot.getWidth() - control.getWidth());
				break;
			case "bottom":
				model.setY(pivot.getMinY() + pivot.getHeight() - control.getHeight());
				break;
			case "center":
				model.setX(avg - control.getWidth() / 2);
				break;
			case "middle":
				model.setY(avg - control.getHeight() / 2);
				break;
			default:
				break;
			}

			control.updateFromModel();
		}
	}

}
