package phasereditor.animation.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.animation.ui.AnimationsEditor;

public class PlayPauseAnimationHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		var editor = (AnimationsEditor) HandlerUtil.getActivePart(event);
		editor.playOrPause();
		return null;
	}

}
