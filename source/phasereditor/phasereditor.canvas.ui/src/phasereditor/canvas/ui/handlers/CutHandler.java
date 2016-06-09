package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.behaviors.SelectionBehavior;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class CutHandler extends CopyHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		super.execute(event);

		IStructuredSelection sel = ((IStructuredSelection) HandlerUtil.getCurrentSelection(event));
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		SelectionBehavior selection = editor.getCanvas().getSelectionBehavior();

		for (Object obj : sel.toArray()) {
			if (obj instanceof IObjectNode) {
				IObjectNode inode = (IObjectNode) obj;
				inode.getControl().removeme();
				selection.removeNodeFromSelection(inode.getNode());
			}
		}

		editor.getCanvas().getWorldModel().firePropertyChange(WorldModel.PROP_STRUCTURE);
		selection.updateSelectedNodes();

		return null;
	}

}
