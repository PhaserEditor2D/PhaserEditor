import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.TextModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.behaviors.CreateBehavior;

public class AddTextHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);

		ObjectCanvas canvas = editor.getCanvas();
		CreateBehavior create = canvas.getCreateBehavior();

		create.dropObjects(new StructuredSelection("This is a text"), (group, text) -> {
			TextModel model = new TextModel(group);
			model.setText((String) text);
			return model;
		});

		return null;
	}

}
