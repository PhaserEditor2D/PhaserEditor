package phasereditor.animation.ui.editor.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.animation.ui.editor.AnimationsEditor;

public class QickOutlineHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		var editor = (AnimationsEditor) HandlerUtil.getActiveEditor(event);

		editor.getOutlineAction().run();

		return null;
	}

}
