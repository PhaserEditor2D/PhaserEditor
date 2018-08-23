package phasereditor.animation.ui.editor;

import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.EditorActionBarContributor;

public class AnimationsEditorActionBarContributor extends EditorActionBarContributor {

	private AnimationsEditor _editor;

	public AnimationsEditorActionBarContributor() {
	}

	@Override
	public void setActiveEditor(IEditorPart targetEditor) {
		_editor = (AnimationsEditor) targetEditor;

		var manager = getActionBars().getToolBarManager();

		manager.removeAll();

		manager.add(_editor.getPlayAction());
		manager.add(_editor.getPauseAction());
		manager.add(_editor.getStopAction());

		manager.add(new Separator());

		manager.add(_editor.getZoom_1_1_action());
		manager.add(_editor.getZoom_fitWindow_action());

		manager.add(new Separator());

		manager.add(_editor.getNewAction());
		manager.add(_editor.getOutlineAction());
		manager.add(_editor.getQuickEditAction());

		manager.add(new Separator());

		manager.add(_editor.getDeleteAction());

	}

	public AnimationsEditor getEditor() {
		return _editor;
	}
}
