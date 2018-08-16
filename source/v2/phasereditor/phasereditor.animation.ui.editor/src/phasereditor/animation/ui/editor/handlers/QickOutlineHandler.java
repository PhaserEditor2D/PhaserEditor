package phasereditor.animation.ui.editor.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.animation.ui.editor.AnimationModel_in_Editor;
import phasereditor.animation.ui.editor.AnimationsEditor;

public class QickOutlineHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		var editor = (AnimationsEditor) HandlerUtil.getActiveEditor(event);

		var model = editor.getModel();

		var dlg = new QuickOutlineDialog(HandlerUtil.getActiveShell(event));
		dlg.setModel(model);
		dlg.setSelected(editor.getAnimationCanvas().getModel());

		if (dlg.open() == Window.OK) {
			var selected = dlg.getSelected();
			if (selected != null) {
				editor.selectAnimation((AnimationModel_in_Editor) selected);
			}
		}

		return null;
	}

}
