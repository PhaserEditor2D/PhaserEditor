package phasereditor.scene.ui.editor.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.undo.SceneSnapshotOperation;

public class DeleteHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		SceneEditor editor = (SceneEditor) HandlerUtil.getActiveEditor(event);

		var beforeData = SceneSnapshotOperation.takeSnapshot(editor);

		var list = ((IStructuredSelection) editor.getEditorSite().getSelectionProvider().getSelection()).toArray();

		for (var obj : list) {
			var model = (ObjectModel) obj;

			ParentComponent.removeFromParent(model);
		}

		editor.getScene().redraw();

		editor.getEditorSite().getSelectionProvider().setSelection(StructuredSelection.EMPTY);

		{
			var outline = editor.getOutline();
			if (outline != null) {
				outline.refresh();
				outline.setSelection_from_external(StructuredSelection.EMPTY);
			}
		}

		var afterData = SceneSnapshotOperation.takeSnapshot(editor);

		editor.executeOperation(new SceneSnapshotOperation(beforeData, afterData, "Delete objects"));

		return null;
	}

}
