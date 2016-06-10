package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import javafx.geometry.Rectangle2D;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.behaviors.UpdateBehavior;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class AlignNodeHandler extends AbstractHandler {

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
			pivot = makePivotRect(0, 0, first.getGroup());
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
				BaseObjectControl<?> control = inode.getControl();

				boolean update = false;
				switch (place) {
				case "left":
					update = control.getTextureLeft() < pivot.getMinX();
					break;
				case "top":
					update = control.getTextureTop() < pivot.getMinY();
					break;
				case "right":
					update = control.getTextureLeft() + control.getTextureWidth() > pivot.getMinX() + pivot.getWidth();
					break;
				case "bottom":
					update = control.getTextureTop() + control.getTextureHeight() > pivot.getMinY() + pivot.getHeight();
					break;
				case "center":
					sum += control.getTextureLeft() + control.getTextureWidth() / 2;
					break;
				case "middle":
					sum += control.getTextureTop() + control.getTextureHeight() / 2;
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
		ObjectCanvas canvas = editor.getCanvas();
		canvas.getSelectionBehavior().updateSelectedNodes();

		return null;
	}

	private static Rectangle2D makePivotRect(double x, double y, IObjectNode node) {
		BaseObjectControl<?> control = node.getControl();
		BaseObjectModel model = control.getModel();

		double xoffs = model.getX() - control.getTextureLeft();
		double yoffs = model.getY() - control.getTextureTop();

		double width = control.getTextureWidth();
		double height = control.getTextureHeight();

		return new Rectangle2D(x + xoffs, y + yoffs, width, height);
	}

	private static Rectangle2D makeRect(IObjectNode node) {
		return makePivotRect(node.getControl().getTextureLeft(), node.getControl().getTextureTop(), node);
	}

	private static void align(String place, Object[] elems, Rectangle2D pivot, double avg) {

		CompositeOperation operations = new CompositeOperation();
		UpdateBehavior update = null;

		for (Object elem : elems) {
			IObjectNode inode = (IObjectNode) elem;
			BaseObjectModel model = inode.getModel();
			BaseObjectControl<?> control = inode.getControl();

			double ix = model.getX();
			double iy = model.getY();

			double xoffs = ix - control.getTextureLeft();
			double yoffs = iy - control.getTextureTop();

			double x = ix;
			double y = iy;

			switch (place) {
			case "left":
				x = pivot.getMinX() + xoffs;
				break;
			case "top":
				y = pivot.getMinY() + yoffs;
				break;
			case "right":
				double textureWidth = control.getTextureWidth();
				x = pivot.getMinX() + pivot.getWidth() - textureWidth + xoffs;
				break;
			case "bottom":
				double textureHeight = control.getTextureHeight();
				y = pivot.getMinY() + pivot.getHeight() - textureHeight + yoffs;
				break;
			case "center":
				x = avg - control.getTextureWidth() / 2 + xoffs;
				break;
			case "middle":
				y = avg - control.getTextureHeight() / 2 + yoffs;
				break;
			default:
				break;
			}

			if (x != ix || y != iy) {
				update = control.getCanvas().getUpdateBehavior();
				update.addUpdateLocationOperation(operations, inode, x, y);
			}
		}

		if (update != null) {
			update.executeOperations(operations);
		}
	}

}
