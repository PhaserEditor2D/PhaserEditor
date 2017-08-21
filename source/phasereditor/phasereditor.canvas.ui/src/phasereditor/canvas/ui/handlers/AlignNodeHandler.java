package phasereditor.canvas.ui.handlers;

import static java.lang.System.out;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.behaviors.SelectionBehavior;
import phasereditor.canvas.ui.editors.behaviors.UpdateBehavior;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.UpdateFromPropertyChange;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class AlignNodeHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);

		String[] split = event.getCommand().getId().split("\\.");
		String place = split[split.length - 1];

		List<IObjectNode> elems = SelectionBehavior.filterSelection(sel);

		IObjectNode first = elems.get(0);
		
		Rectangle2D pivotInWorld = null;
		double sum = 0;

		if (elems.size() == 1) {
			pivotInWorld = makePivotRect(first);
			BaseObjectControl<?> control = first.getControl();

			ObjectCanvas canvas = control.getCanvas();
			EditorSettings settings = canvas.getSettingsModel();

			double w = settings.getSceneWidth();
			double h = settings.getSceneHeight();

			pivotInWorld = new Rectangle2D(0, 0, w, h);

			switch (place) {
			case "center":
				sum = pivotInWorld.getMinX() + pivotInWorld.getWidth() / 2;
				break;
			case "middle":
				sum = pivotInWorld.getMinY() + pivotInWorld.getHeight() / 2;
				break;
			default:
				break;
			}
		} else {
			for (Object elem : elems) {
				IObjectNode inode = (IObjectNode) elem;
				BaseObjectControl<?> control = inode.getControl();

				switch (place) {
				case "center":
					sum += inode.getModel().getX() + control.getTextureWidth() / 2;
					break;
				case "middle":
					sum += inode.getModel().getY() + control.getTextureHeight() / 2;
					break;
				default:
					break;
				}
				
				if (pivotInWorld == null) {
					pivotInWorld = makePivotRect(inode);
				}
			}
		}

		double avg = sum / elems.size();
		// round position to integer
		avg = Math.round(avg);

		align(place, elems, pivotInWorld, avg);

		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		ObjectCanvas canvas = editor.getCanvas();
		canvas.getSelectionBehavior().updateSelectedNodes();

		return null;
	}

	private static Rectangle2D makePivotRect(double x, double y, IObjectNode node) {
		BaseObjectControl<?> control = node.getControl();
		return new Rectangle2D(x, y, control.getTextureWidth(), control.getTextureHeight());
	}

	private static Rectangle2D makePivotRect(IObjectNode node) {
		return makePivotRect(node.getModel().getX(), node.getModel().getY(), node);
	}

	private static void align(String place, List<IObjectNode> elems, Rectangle2D pivotInWorld, double avgInWorld) {

		CompositeOperation operations = new CompositeOperation();
		UpdateFromPropertyChange updateFromPropChanges = new UpdateFromPropertyChange();
		UpdateBehavior update = null;

		for (Object elem : elems) {
			IObjectNode inode = (IObjectNode) elem;
			Node node = inode.getNode();
			GroupNode world = inode.getControl().getCanvas().getWorldNode();

			out.println("scene to local:" + node.sceneToLocal(0, 0));
			out.println("world to local:" + world.sceneToLocal(0, 0));

			BaseObjectModel model = inode.getModel();
			BaseObjectControl<?> control = inode.getControl();

			double localX = model.getX();
			double localY = model.getY();

			switch (place) {
			case "left":
				localX = worldToLocal(pivotInWorld.getMinX(), 0, node).getX();
				break;
			case "top":
				localY = worldToLocal(0, pivotInWorld.getMinY(), node).getY();
				break;
			case "right":
				double textureWidth = control.getTextureWidth();
				localX = worldToLocal(pivotInWorld.getWidth(), 0, node).getX() - textureWidth;
				break;
			case "bottom":
				double textureHeight = control.getTextureHeight();
				localY = worldToLocal(0, pivotInWorld.getHeight(), node).getY() - textureHeight;
				break;
			case "center":
				localX = worldToLocal(avgInWorld, 0, node).getX() - control.getTextureWidth() / 2;
				break;
			case "middle":
				localY = worldToLocal(0, avgInWorld, node).getY() - control.getTextureHeight() / 2;
				break;
			default:
				break;
			}

			update = control.getCanvas().getUpdateBehavior();
			update.addUpdateLocationOperation(operations, inode, localX, localY, false);
			updateFromPropChanges.add(inode.getControl().getId());
		}

		if (update != null) {
			operations.add(updateFromPropChanges);
			update.executeOperations(operations);
		}
	}

	public static Point2D localToWorld(double x, double y, Node local) {
		ObjectCanvas canvas = ((IObjectNode) local).getControl().getCanvas();
		Point2D p = local.localToScene(x, y);
		Bounds bounds = canvas.getWorldNode().getBoundsInParent();
		p = p.add(-bounds.getMinX(), bounds.getMinY());
		return p;
	}

	public static Point2D worldToLocal(double worldX, double worldY, Node local) {
		ObjectCanvas canvas = ((IObjectNode) local).getControl().getCanvas();
		GroupNode world = canvas.getWorldNode();
		Point2D scenePos = world.localToScene(worldX, worldY);
		Point2D localPos = local.getParent().sceneToLocal(scenePos);
		return localPos;
	}

}
