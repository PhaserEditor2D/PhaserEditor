package phasereditor.canvas.ui.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.behaviors.SelectionBehavior;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.DeleteNodeOperation;
import phasereditor.canvas.ui.editors.operations.RemoveFromSelectionOperation;
import phasereditor.canvas.ui.editors.operations.SelectOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class DeleteObjectHandler extends AbstractHandler {

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

		List<IObjectNode> nodes = SelectionBehavior.filterSelection(sel);

		{
			CanvasModel model = editor.getModel();
			if (model.getWorld().getChildren().size() <= 1 && model.getType() == CanvasType.SPRITE) {
				MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Delete",
						"Cannot delete the only one object of this prefab");
				return;
			}
		}

		CompositeOperation operations = new CompositeOperation();

		SelectOperation select = new SelectOperation();
		operations.add(select);

		RemoveFromSelectionOperation clearSelection = new RemoveFromSelectionOperation();
		operations.add(clearSelection);

		for (IObjectNode node : nodes) {
			String id = node.getControl().getId();
			select.add(id);
			clearSelection.add(id);
			operations.add(new DeleteNodeOperation(id, false));
		}

		editor.getCanvas().getUpdateBehavior().executeOperations(operations);
	}
}
