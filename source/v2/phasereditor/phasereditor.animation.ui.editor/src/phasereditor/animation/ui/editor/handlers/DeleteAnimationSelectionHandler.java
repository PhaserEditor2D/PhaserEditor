package phasereditor.animation.ui.editor.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.animation.ui.editor.AnimationFrameModel_in_Editor;
import phasereditor.animation.ui.editor.AnimationModel_in_Editor;
import phasereditor.animation.ui.editor.AnimationsEditor;

public class DeleteAnimationSelectionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		var editor = (AnimationsEditor) HandlerUtil.getActiveEditor(event);

		var sel = HandlerUtil.getCurrentStructuredSelection(event);

		List<AnimationModel_in_Editor> animations = new ArrayList<>();
		List<AnimationFrameModel_in_Editor> frames = new ArrayList<>();

		for (var obj : sel.toArray()) {
			if (obj instanceof AnimationModel_in_Editor) {
				if (((AnimationModel_in_Editor) obj).getEditor() == editor) {
					animations.add((AnimationModel_in_Editor) obj);
				}
			} else if (obj instanceof AnimationFrameModel_in_Editor) {
				AnimationModel_in_Editor anim = ((AnimationFrameModel_in_Editor) obj).getAnimation();
				AnimationsEditor animEditor = anim.getEditor();
				if (animEditor == editor) {
					frames.add((AnimationFrameModel_in_Editor) obj);
				}
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
