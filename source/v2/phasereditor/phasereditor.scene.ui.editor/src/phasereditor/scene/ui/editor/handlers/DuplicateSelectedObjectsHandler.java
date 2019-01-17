package phasereditor.scene.ui.editor.handlers;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.UUID;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.json.JSONObject;

import phasereditor.scene.core.NameComputer;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.SceneModel;
import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.core.VariableComponent;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.undo.WorldSnapshotOperation;

public class DuplicateSelectedObjectsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		var editor = (SceneEditor) HandlerUtil.getActiveEditor(event);
		var sel = editor.getSelectionList();

		var list = sel.stream().filter(o -> o instanceof TransformComponent).collect(toList());

		if (list.isEmpty()) {
			return null;
		}

		var project = editor.getProject();
		var displayList = editor.getSceneModel().getDisplayList();
		var nameComputer = new NameComputer(displayList);

		var copyList = new ArrayList<ObjectModel>();

		var before = WorldSnapshotOperation.takeSnapshot(editor);

		for (var obj : list) {
			var parent = ParentComponent.get_parent(obj);

			var copyData = new JSONObject();
			obj.write(copyData);
			var copy = SceneModel.createModel(copyData.getString("-type"));
			copy.read(copyData, project);

			copy.setId(UUID.randomUUID().toString());

			var oldname = VariableComponent.get_variableName(copy);
			var newname = nameComputer.newName(oldname);
			VariableComponent.set_variableName(copy, newname);

			ParentComponent.utils_addChild(parent, copy);

			copyList.add(copy);
		}

		var after = WorldSnapshotOperation.takeSnapshot(editor);

		editor.refreshOutline();

		editor.setSelection(copyList);

		editor.executeOperation(new WorldSnapshotOperation(before, after, "Duplicate objects"));

		return null;
	}

}
