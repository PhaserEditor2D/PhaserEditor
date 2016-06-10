package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class PickHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
		boolean pick = event.getCommand().getId().endsWith(".pick");

		CompositeOperation operations = new CompositeOperation();

		for (Object elem : sel.toArray()) {
			IObjectNode inode = (IObjectNode) elem;
			BaseObjectControl<?> control = inode.getControl();
			operations.add(new ChangePropertyOperation<>(inode.getModel().getId(),
					control.getEditorPick_property().getName(), Boolean.valueOf(pick)));
		}

		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		editor.getCanvas().getUpdateBehavior().executeOperations(operations);

		return null;

	}

}
