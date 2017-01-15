package phasereditor.canvas.ui.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import javafx.geometry.Point2D;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.SelectGroupDialog;
import phasereditor.canvas.ui.editors.behaviors.SelectionBehavior;
import phasereditor.canvas.ui.editors.operations.AddNodeOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.DeleteNodeOperation;
import phasereditor.canvas.ui.editors.operations.ExpandOutlineOperation;
import phasereditor.canvas.ui.editors.operations.RemoveFromSelectionOperation;
import phasereditor.canvas.ui.editors.operations.SelectOperation;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class MoveToOtherGroupHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ObjectCanvas canvas = ((CanvasEditor) HandlerUtil.getActiveEditor(event)).getCanvas();
		Shell shell = HandlerUtil.getActiveShell(event);
		SelectGroupDialog dlg = new SelectGroupDialog(shell);
		dlg.setCanvas(canvas);

		if (dlg.open() == Window.OK) {
			GroupNode target = dlg.getResult();

			String targetId = target.getModel().getId();

			List<IObjectNode> sel = SelectionBehavior
					.filterSelection((IStructuredSelection) HandlerUtil.getCurrentSelection(event));

			List<IObjectNode> nodes = new ArrayList<>();

			// filter the nodes

			for (IObjectNode node : sel) {
				if (canMove(node, target)) {
					nodes.add(node);
				} else {
					MessageDialog.openError(shell, "Move", "Cannot move the object to that group.");
					return null;
				}
			}

			// sort by Z order

			nodes.sort(IObjectNode.DISPLAY_ORDER_COMPARATOR);

			// make the copies
			List<BaseObjectModel> copies = new ArrayList<>();
			for (IObjectNode node : nodes) {
				BaseObjectModel copy = node.getModel().copy(true);
				copies.add(copy);

				// relocate the copy to the scene location
				Point2D p = node.getGroup().localToScene(copy.getX(), copy.getY());
				copy.setX(p.getX());
				copy.setY(p.getY());

			}

			CompositeOperation operations = new CompositeOperation();
			SelectOperation select = new SelectOperation();

			// clear the selection (because the delete node operation is not
			// doing it)
			RemoveFromSelectionOperation clearSelection = new RemoveFromSelectionOperation();
			operations.add(clearSelection);

			// delete the nodes from the old parent
			for (BaseObjectModel copy : copies) {
				String id = copy.getId();
				operations.add(new DeleteNodeOperation(id, false));
			}

			// add the nodes to the new parent
			int i = target.getChildren().size();
			for (BaseObjectModel copy : copies) {
				String id = copy.getId();
				// add nodes to the selection operations
				select.add(id);
				clearSelection.add(id);

				// transform the copy location to the new parent location
				Point2D p = target.sceneToLocal(copy.getX(), copy.getY());

				// add the node to the new parent
				operations.add(new AddNodeOperation(copy.toJSON(false), i, p.getX(), p.getY(), targetId));
				i++;
			}

			// expand the new parent to 1
			operations.add(new ExpandOutlineOperation(Arrays.asList(targetId)));

			// select the moved nodes
			operations.add(select);

			// execute operations!
			canvas.getUpdateBehavior().executeOperations(operations);
		}

		return null;
	}

	private static boolean canMove(IObjectNode node, GroupNode target) {
		return !target.getAncestors().contains(node);
	}

}
