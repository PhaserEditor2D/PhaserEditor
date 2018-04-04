package phasereditor.canvas.ui.handlers;

import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.operations.ChangePropertyOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.shapes.IObjectNode;

public class RenameObjectHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<?> sel = HandlerUtil.getCurrentStructuredSelection(event).toList();

		ObjectCanvas canvas = ((CanvasEditor) HandlerUtil.getActiveEditor(event)).getCanvas();

		IObjectNode first = (IObjectNode) sel.get(0);

		String name = first.getModel().getEditorName();

		Set<String> allnames = canvas.getWorldModel().collectAllNames();

		InputDialog dlg = new InputDialog(HandlerUtil.getActiveShell(event), "Rename", "Enter the new name:", name,
				new IInputValidator() {

					@Override
					public String isValid(String newText) {
						BaseObjectModel result = canvas.getWorldModel().findByName(newText);
						if (allnames.contains(newText) && !sel.contains(result)) {
							return "That name already exists";
						}

						return null;
					}
				});

		if (dlg.open() == Window.OK) {

			String newName = dlg.getValue();

			CompositeOperation operations = new CompositeOperation();

			if (sel.size() == 1) {
				operations.add(new ChangePropertyOperation<>(first.getModel().getId(), "varName", newName));
			} else {
				int i = 0;
				for (Object obj : sel) {

					String name2 = newName + i;

					while (allnames.contains(name2)) {
						name2 = newName + ++i;
					}
					IObjectNode node = (IObjectNode) obj;

					operations.add(new ChangePropertyOperation<>(node.getModel().getId(), "varName", name2));
					
					i++;
				}
			}
			canvas.getUpdateBehavior().executeOperations(operations);
		}

		return null;
	}

}
