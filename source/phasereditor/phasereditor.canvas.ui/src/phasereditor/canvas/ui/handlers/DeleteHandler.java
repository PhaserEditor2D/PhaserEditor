package phasereditor.canvas.ui.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.DeleteNodeOperation;
import phasereditor.canvas.ui.editors.operations.RemoveFromSelectionOperation;
import phasereditor.canvas.ui.editors.operations.SelectOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class DeleteHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		if (editor.getPalette().getViewer().getTable().isFocusControl()) {
			editor.getPalette().deleteSelected();
		} else {
			executeDelete(event);
		}
		return null;
	}

	public static void executeDelete(ExecutionEvent event) {
		IStructuredSelection sel = ((IStructuredSelection) HandlerUtil.getCurrentSelection(event));
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);

		List<IObjectNode> nodes = filterSelection(sel);

		CompositeOperation operations = new CompositeOperation();

		SelectOperation select = new SelectOperation();
		operations.add(select);

		RemoveFromSelectionOperation clearSelection = new RemoveFromSelectionOperation();
		operations.add(clearSelection);

		for (IObjectNode node : nodes) {
			select.add(node.getModel().getId());
			String id = node.getControl().getId();
			operations.add(new DeleteNodeOperation(id, false));
			clearSelection.add(id);
		}

		editor.getCanvas().getUpdateBehavior().executeOperations(operations);
	}

	/**
	 * If a node is in the selection but its parent is in the selection too,
	 * then that node is filtered. This helps to do operations like delete,
	 * paste, etc.. It removes redundant nodes.
	 * 
	 */
	public static List<IObjectNode> filterSelection(IStructuredSelection sel) {
		if (sel == null) {
			return null;
		}

		Set<Object> set = new HashSet<>(Arrays.asList(sel.toArray()));
		List<IObjectNode> nodes = new ArrayList<>();
		for (Object obj : sel.toArray()) {
			if (obj instanceof IObjectNode) {
				IObjectNode inode = (IObjectNode) obj;
				boolean add = true;
				for (Object ancestor : inode.getAncestors()) {
					if (set.contains(ancestor)) {
						add = false;
						break;
					}
				}
				if (add) {
					nodes.add(inode);
				}
			}
		}
		return nodes;
	}

}
