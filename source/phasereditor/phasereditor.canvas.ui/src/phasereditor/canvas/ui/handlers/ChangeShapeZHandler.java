package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.WorldModel.ZOperation;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.IObjectNode;

public abstract class ChangeShapeZHandler extends AbstractHandler {

	private ZOperation _zoperation;

	public ChangeShapeZHandler(ZOperation zoperation) {
		_zoperation = zoperation;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
		Object[] elems = selection.toArray();
		for (int i = elems.length - 1; i >= 0; i--) {
			Object elem = elems[i];
			IObjectNode node = (IObjectNode) elem;
			BaseObjectControl<?> shape = node.getControl();
			shape.sendNodeTo(_zoperation);
		}
		return null;
	}

}
