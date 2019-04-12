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
package phasereditor.scene.ui.editor;

import static java.util.stream.Collectors.toSet;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.json.JSONObject;

import phasereditor.scene.core.DisplayComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.PackReferencesCollector;
import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.ui.editor.messages.CreateGameMessage;
import phasereditor.scene.ui.editor.messages.DropObjectsMessage;
import phasereditor.scene.ui.editor.messages.SelectObjectsMessage;
import phasereditor.scene.ui.editor.undo.SingleObjectSnapshotOperation;
import phasereditor.webrun.core.ApiHub;
import phasereditor.webrun.core.ApiMessage;
import phasereditor.webrun.core.BatchMessage;
import phasereditor.webrun.core.WebRunCore;

/**
 * @author arian
 *
 */
public class SceneEditorBroker {
	private SceneEditor _editor;
	private String _channel;
	private static int _channelCount;

	public SceneEditorBroker(SceneEditor editor) {
		super();

		_editor = editor;

		_channel = "SceneEditor-" + _channelCount++;

		WebRunCore.startServerIfNotRunning();

		ApiHub.addListener(_channel, this::messageReceived);
	}

	public void sendAllBatch(ApiMessage... msg) {
		sendAll(new BatchMessage(msg));
	}

	public void sendAll(ApiMessage msg) {
		ApiHub.sendMessageAllClients(_channel, wrapMessage(msg));
	}

	@SuppressWarnings("static-method")
	public void send(Object client, ApiMessage msg) {
		ApiHub.sendMessage(client, wrapMessage(msg));
	}

	public void sendBatch(Object client, ApiMessage... msgs) {
		send(client, new BatchMessage(msgs));
	}

	private static BatchMessage wrapMessage(ApiMessage msg) {
		if (msg instanceof BatchMessage) {
			return (BatchMessage) msg;
		}

		var batch = new BatchMessage();

		batch.add(msg);
		return batch;
	}

	public void dispose() {
		ApiHub.removeListener(_channel, this::messageReceived);
		ApiHub.stopConnection(_channel);
	}

	public String getUrl() {
		return "http://localhost:" + WebRunCore.getServerPort()
				+ "/extension/phasereditor.scene.ui.editor/sceneEditor/index.html?channel=" + _channel;
	}

	private void messageReceived(Object client, JSONObject msg) {
		try {
			var method = msg.getString("method");

			switch (method) {
			case "GetCreateGame":
				send(client, new CreateGameMessage(_editor));
				break;
			case "GetSelectObjects":
				send(client, new SelectObjectsMessage(_editor));
				break;
			case "ClickObject":
				onClickObject(client, msg);
				break;
			case "DropEvent":
				onDropEvent(client, msg);
				break;
			case "SetObjectDisplayProperties":
				onSetObjectDisplayProperties(msg);
				break;
			case "SetObjectPosition":
				onSetObjectPosition(msg);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			SceneUIEditor.logError(e);
		}
	}

	private void onSetObjectPosition(JSONObject msg) {
		var list = msg.getJSONArray("list");

		var table = _editor.getSceneModel().getDisplayList().lookupTable();

		var models = new ArrayList<ObjectModel>();
		var positions = new ArrayList<JSONObject>();

		for (int i = 0; i < list.length(); i++) {
			var pos = list.getJSONObject(i);
			var id = pos.getString("id");

			var model = table.lookup(id);

			if (model != null) {
				models.add(model);
				positions.add(pos);
			}
		}

		var before = SingleObjectSnapshotOperation.takeSnapshot(models);

		int i = 0;
		for (var model : models) {
			var pos = positions.get(i);

			TransformComponent.set_x(model, pos.getFloat("x"));
			TransformComponent.set_y(model, pos.getFloat("y"));

			i++;
		}

		var after = SingleObjectSnapshotOperation.takeSnapshot(models);

		swtRun(() -> {

			_editor.executeOperation(new SingleObjectSnapshotOperation(before, after, "Align Objects"));

			_editor.setDirty(true);
			
			_editor.updatePropertyPagesContentWithSelection();

		});
	}

	private void onSetObjectDisplayProperties(JSONObject msg) {

		var displayList = _editor.getSceneModel().getDisplayList();

		var list = msg.getJSONArray("list");

		for (int i = 0; i < list.length(); i++) {
			var objData = list.getJSONObject(i);
			var id = objData.getString("id");
			var model = displayList.findById(id);
			if (model != null) {

				var w = objData.getFloat("displayWidth");
				var h = objData.getFloat("displayHeight");

				DisplayComponent.set_displayWidth(model, w);
				DisplayComponent.set_displayHeight(model, h);
			}
		}
	}

	private void onDropEvent(Object client, JSONObject msg) {
		var x = msg.getFloat("x");
		var y = msg.getFloat("y");

		var sel = LocalSelectionTransfer.getTransfer().getSelection();

		if (!sel.isEmpty() && sel instanceof IStructuredSelection) {
			var data = ((IStructuredSelection) sel).toArray();

			swtRun(() -> {

				var collector = new PackReferencesCollector(_editor.getSceneModel(), _editor.getAssetFinder());

				var beforeKeys = new HashSet<>(collector.collectAssetKeys());

				var models = _editor.selectionDropped(x, y, data);

				var currentKeys = new HashSet<>(collector.collectAssetKeys());
				currentKeys.removeAll(beforeKeys);

				var newAssets = currentKeys.stream().map(key -> key.getAsset()).collect(toSet());

				JSONObject pack = null;

				if (!newAssets.isEmpty()) {
					pack = collector.collectNewPack(newAssets);
				}

				if (!models.isEmpty()) {
					sendBatch(client,

							new DropObjectsMessage(models, Optional.ofNullable(pack)),

							new SelectObjectsMessage(_editor)

					);
				}

			});

		}
	}

	private void onClickObject(Object client, JSONObject msg) {

		var id = msg.optString("id");
		var ctrl = msg.getBoolean("ctrl");

		ObjectModel obj = id == null ? null : _editor.getSceneModel().getDisplayList().findById(id);

		swtRun(() -> {
			_editor.getScene().getSelectionEvents().updateSelection(obj, ctrl);
			send(client, new SelectObjectsMessage(_editor));
		});
	}
}
