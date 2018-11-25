package phasereditor.animation.ui.editor.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.animation.ui.editor.AnimationsEditor;
import phasereditor.assetpack.core.animations.AnimationFrameModel;
import phasereditor.assetpack.core.animations.AnimationModel;

public class DeleteAnimationSelectionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		var editor = (AnimationsEditor) HandlerUtil.getActiveEditor(event);

		var sel = HandlerUtil.getCurrentStructuredSelection(event);

		List<AnimationModel> animations = new ArrayList<>();
		List<AnimationFrameModel> frames = new ArrayList<>();

		for (var obj : sel.toArray()) {
			if (obj instanceof AnimationModel) {
				animations.add((AnimationModel) obj);
			} else if (obj instanceof AnimationFrameModel) {
				frames.add((AnimationFrameModel) obj);
			}
		}

		if (frames.isEmpty()) {
			var activePart = HandlerUtil.getActivePart(event);
			// if the active part is the editor it means we are trying to delete the current
			// animation, but it is not what we want to do.
			if (activePart != editor) {
				if (!animations.isEmpty()) {
					editor.deleteAnimations(animations);
				}
			}
		} else {
			editor.deleteFrames(frames);
		}

		return null;
	}

}
