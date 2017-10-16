package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.TextModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.behaviors.CreateBehavior;
import phasereditor.canvas.ui.editors.grid.editors.TextDialog;

public class AddTextHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		TextDialog dlg = new TextDialog(HandlerUtil.getActiveShell(event));

		dlg.setTitle("Add Text");
		dlg.setMessage("Enter the text:");
		dlg.setInitialText("This is a text");
		dlg.setSelectAll(true);

		if (dlg.open() == Window.OK) {
			CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);

			ObjectCanvas canvas = editor.getCanvas();
			CreateBehavior create = canvas.getCreateBehavior();

			create.dropObjects(new StructuredSelection(dlg.getResult()), (group, text) -> {
				TextModel model = new TextModel(group);
				model.setText((String) text);
				return model;
			});

		}

		return null;
	}

}
