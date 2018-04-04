package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.WorldModel.ZOperation;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.operations.ChangeZOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;

public abstract class ChangeNodeZHandler extends AbstractHandler {

	private ZOperation _zoperation;

	public ChangeNodeZHandler(ZOperation zoperation) {
		_zoperation = zoperation;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
		Object[] elems = selection.toArray();

		CompositeOperation operations = new CompositeOperation();

		for (int i = elems.length - 1; i >= 0; i--) {
			Object elem = elems[i];
			IObjectNode node = (IObjectNode) elem;
			operations.add(new ChangeZOperation(node.getModel().getId(), _zoperation));
		}

		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		editor.getCanvas().getUpdateBehavior().executeOperations(operations);

		return null;
	}

}
