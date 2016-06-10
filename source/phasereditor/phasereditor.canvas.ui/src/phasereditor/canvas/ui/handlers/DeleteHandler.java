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
import phasereditor.canvas.ui.shapes.IObjectNode;

public class DeleteHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		executeDelete(event);
		return null;
	}

	public static void executeDelete(ExecutionEvent event) {
		IStructuredSelection sel = ((IStructuredSelection) HandlerUtil.getCurrentSelection(event));
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);

		// filter those children of selected nodes

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

		CompositeOperation operations = new CompositeOperation();

		for (IObjectNode node : nodes) {
			operations.add(new DeleteNodeOperation(node.getControl().getUniqueId()));
		}

		editor.getCanvas().getUpdateBehavior().executeOperations(operations);
	}

}
