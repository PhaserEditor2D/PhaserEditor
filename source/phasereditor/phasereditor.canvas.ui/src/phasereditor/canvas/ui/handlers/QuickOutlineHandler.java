package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.QuickOutlineDialog;
import phasereditor.canvas.ui.editors.behaviors.SelectionBehavior;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class QuickOutlineHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		QuickOutlineDialog dlg = new QuickOutlineDialog(HandlerUtil.getActiveShell(event));
		dlg.setCanvas(editor.getCanvas());
		dlg.setResultHandler(e -> {
			if (e != null) {
				SelectionBehavior selBehavior = editor.getCanvas().getSelectionBehavior();
				selBehavior.setSelectionAndRevealInScene((IObjectNode) e);
			}
		});
		dlg.open();
		return null;
	}

}
