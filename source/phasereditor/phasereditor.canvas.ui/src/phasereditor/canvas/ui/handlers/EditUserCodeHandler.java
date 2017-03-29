package phasereditor.canvas.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.canvas.core.EditorSettings;
import phasereditor.canvas.core.EditorSettings_UserCode;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.grid.editors.UserCodeDialog;

public class EditUserCodeHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		UserCodeDialog dlg = new UserCodeDialog(HandlerUtil.getActiveShell(event));

		EditorSettings settings = editor.getModel().getSettings();
		EditorSettings_UserCode userCode = settings.getUserCode();
		EditorSettings_UserCode copy = userCode.copy();
		dlg.setUserCode(copy);

		if (dlg.open() == Window.OK) {
			if (!copy.toJSON().toString().equals(userCode.toJSON().toString())) {
				settings.setUserCode(copy);
				editor.setDirty(true);
			}
		}

		return null;
	}

}
