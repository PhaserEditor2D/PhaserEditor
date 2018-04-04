package phasereditor.canvas.ui.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class SelectParentHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		ObjectCanvas canvas = editor.getCanvas();

		List<IObjectNode> nodes = canvas.getSelectionBehavior().getSelectedNodes();

		Object[] newsel = nodes.stream().map(n -> n.getGroup()).filter(g -> !g.getModel().isWorldModel()).toArray();

		canvas.getSelectionBehavior().setSelection(new StructuredSelection(newsel));

		return null;
	}

}
