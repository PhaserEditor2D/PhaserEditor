// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.animation.ui;

import java.util.Optional;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.assetpack.core.animations.AnimationsModel;

/**
 * @author arian
 *
 */
public class AnimationUI {

	public static void showAnimationInEditor(AnimationModel anim) {
		showAnimationsInEditor(anim.getAnimations()).ifPresent(editor -> {
			editor.revealAnimation(anim.getKey());
		});
	}

	public static Optional<IAnimationsEditor> showAnimationsInEditor(AnimationsModel animations) {
		var file = animations.getFile();

		if (!file.exists()) {
			return Optional.empty();
		}

		try {
			var editor = (IAnimationsEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.openEditor(new FileEditorInput(file), "phasereditor.animation.ui.editor.AnimationsEditor");

			if (editor == null) {
				return Optional.empty();
			}

			return Optional.of(editor);

		} catch (PartInitException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}
}
