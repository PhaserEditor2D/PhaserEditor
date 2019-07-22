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
package phasereditor.animation.ui.editor;

import static java.lang.System.out;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.StructuredSelection;
import org.json.JSONObject;

import phasereditor.assetpack.core.animations.AnimationModel;

/**
 * @author arian
 *
 */
public class AnimationOperation extends AbstractOperation {

	private List<State> _before;
	private List<State> _after;
	private boolean _updateEditorOnExecute;

	public static class State {
		private int index;
		private JSONObject data;

		public State(int index, JSONObject data) {
			this.index = index;
			this.data = data;
		}

		@Override
		public String toString() {
			return data.toString();
		}
	}

	public AnimationOperation(String label, List<State> before, List<State> after, boolean updateEditorOnExecute) {
		super(label);
		_before = before;
		_after = after;
		_updateEditorOnExecute = updateEditorOnExecute;
	}

	public static List<State> readState(List<AnimationModel> animations) {
		return animations.stream()

				.map(a -> new State(a.getAnimations().getAnimations().indexOf(a), a.toJSON()))

				.collect(toList());
	}

	private static IStatus loadState(IAdaptable info, List<State> stateList) {
		var editor = info.getAdapter(AnimationsEditor.class);
		var anims = new ArrayList<>();
		for (var state : stateList) {
			var anim = editor.getModel().getAnimations().get(state.index);
			out.println(state.data);
			anim.read(state.data);
			anims.add(anim);
		}

		editor.getModel().build();
		editor.updatePropertyPagesContentWithSelection();
		var stopped = editor.isStopped();
		editor.setSelection(new StructuredSelection(anims));
		editor.getTimelineCanvas().redraw();
		editor.resetPlayback(stopped);
		editor.setDirty();

		return Status.OK_STATUS;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		if (_updateEditorOnExecute) {
			var editor = info.getAdapter(AnimationsEditor.class);
			editor.getModel().build();
			editor.updatePropertyPagesContentWithSelection();
			editor.getTimelineCanvas().redraw();
			editor.resetPlayback();
			editor.setDirty();
		}
		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return loadState(info, _after);
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return loadState(info, _before);
	}

}
