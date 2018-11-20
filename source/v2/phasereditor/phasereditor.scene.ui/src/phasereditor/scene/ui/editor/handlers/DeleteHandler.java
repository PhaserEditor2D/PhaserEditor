package phasereditor.scene.ui.editor.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.scene.core.GroupComponent;
import phasereditor.scene.core.GroupModel;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.undo.GroupListSnapshotOperation;

public class DeleteHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		SceneEditor editor = (SceneEditor) HandlerUtil.getActiveEditor(event);

		var justGroups = editor.getSelectionList().stream().allMatch(o -> GroupComponent.is(o));

		if (justGroups) {

			var before = GroupListSnapshotOperation.takeSnapshot(editor);

			for (var obj : editor.getSelectionList()) {
				var group = (GroupModel) obj;
				ParentComponent.removeFromParent(group);
			}

			editor.refreshOutline_basedOnId();
			
			editor.setSelection(List.of());

			var after = GroupListSnapshotOperation.takeSnapshot(editor);

			editor.executeOperation(new GroupListSnapshotOperation(before, after, "Delete groups."));

		} else {
			editor.getScene().delete();
		}

		return null;
	}

}
