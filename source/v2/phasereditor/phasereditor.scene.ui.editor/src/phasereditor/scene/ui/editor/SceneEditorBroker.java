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

import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.json.JSONObject;

import phasereditor.scene.core.DisplayComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.OriginComponent;
import phasereditor.scene.core.PackReferencesCollector;
import phasereditor.scene.core.TileSpriteComponent;
import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.ui.editor.messages.CreateGameMessage;
import phasereditor.scene.ui.editor.messages.DropObjectsMessage;
import phasereditor.scene.ui.editor.messages.LoadAssetsMessage;
import phasereditor.scene.ui.editor.messages.SelectObjectsMessage;
import phasereditor.scene.ui.editor.messages.SetCameraStateMessage;
import phasereditor.scene.ui.editor.messages.SetInteractiveToolMessage;
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
			case "SetObjectOrigin":
				onSetObjectOrigin(msg);
				break;
			case "RecordCameraState":
				onRecordCameraState(msg);
				break;
			case "GetCameraState":
				onGetCameraState(client, msg);
				break;
			case "GetInitialState":
				onGetInitialState(client);
				break;
			case "SetTileSpriteProperties":
				onSetTileSpriteProperties(msg);
				break;
			case "SetOriginProperties":
				onSetOriginProperties(msg);
				break;
			case "SetTransformProperties":
				onSetTransformProperties(msg);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			SceneUIEditor.logError(e);
		}
	}

	private void onSetTileSpriteProperties(JSONObject msg) {
		setObjectCustomProperties(msg, (model, data) -> {
			TileSpriteComponent.set_tilePositionX(model, data.getFloat("tilePositionX"));
			TileSpriteComponent.set_tilePositionY(model, data.getFloat("tilePositionY"));
			TileSpriteComponent.set_tileScaleX(model, data.getFloat("tileScaleX"));
			TileSpriteComponent.set_tileScaleY(model, data.getFloat("tileScaleY"));
			TileSpriteComponent.set_width(model, data.getFloat("width"));
			TileSpriteComponent.set_height(model, data.getFloat("height"));
		});

	}

	private void onSetOriginProperties(JSONObject msg) {
		setObjectCustomProperties(msg, (model, data) -> {
			OriginComponent.set_originX(model, data.getFloat(OriginComponent.originX_name));
			OriginComponent.set_originY(model, data.getFloat(OriginComponent.originY_name));
			TransformComponent.set_x(model, data.getFloat(TransformComponent.x_name));
			TransformComponent.set_y(model, data.getFloat(TransformComponent.y_name));
		});
	}
	
	private void onSetTransformProperties(JSONObject msg) {
		setObjectCustomProperties(msg, (model, data) -> {
			TransformComponent.set_x(model, data.getFloat(TransformComponent.x_name));
			TransformComponent.set_y(model, data.getFloat(TransformComponent.y_name));
			TransformComponent.set_scaleX(model, data.getFloat(TransformComponent.scaleX_name));
			TransformComponent.set_scaleY(model, data.getFloat(TransformComponent.scaleY_name));
			TransformComponent.set_angle(model, data.getFloat(TransformComponent.angle_name));
		});
	}

	private void onGetInitialState(Object client) {
		sendBatch(client,

				new SetCameraStateMessage(_editor),

				new SetInteractiveToolMessage(_editor),

				new SelectObjectsMessage(_editor)

		);

	}

	@SuppressWarnings("unused")
	private void onGetCameraState(Object client, JSONObject msg) {
		send(client, new SetCameraStateMessage(_editor));
	}

	private void onRecordCameraState(JSONObject msg) {
		_editor.setCameraState(msg.getJSONObject("cameraState"));
	}

	private void onSetObjectPosition(JSONObject msg) {
		setObjectCustomProperties(msg, (model, data) -> {
			TransformComponent.set_x(model, data.getFloat("x"));
			TransformComponent.set_y(model, data.getFloat("y"));
		});
	}

	private void onSetObjectOrigin(JSONObject msg) {
		setObjectCustomProperties(msg, (model, data) -> {
			OriginComponent.set_originX(model, data.getFloat("originX"));
			OriginComponent.set_originY(model, data.getFloat("originY"));
			TransformComponent.set_x(model, data.getFloat("x"));
			TransformComponent.set_y(model, data.getFloat("y"));
		});
	}

	private void setObjectCustomProperties(JSONObject msg, BiConsumer<ObjectModel, JSONObject> operation) {
		var list = msg.getJSONArray("list");

		var table = _editor.getSceneModel().getDisplayList().lookupTable();

		var models = new ArrayList<ObjectModel>();
		var dataList = new ArrayList<JSONObject>();

		for (int i = 0; i < list.length(); i++) {
			var data = list.getJSONObject(i);
			var id = data.getString("id");

			var model = table.lookup(id);

			if (model != null) {
				models.add(model);
				dataList.add(data);
			}
		}

		var before = SingleObjectSnapshotOperation.takeSnapshot(models);

		int i = 0;
		for (var model : models) {
			var data = dataList.get(i);

			operation.accept(model, data);

			i++;
		}

		var after = SingleObjectSnapshotOperation.takeSnapshot(models);

		swtRun(() -> {

			_editor.executeOperation(new SingleObjectSnapshotOperation(before, after, "Change Object Properties"));

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

				List<ObjectModel> models = new ArrayList<>();

				var collector = new PackReferencesCollector(_editor.getSceneModel(), _editor.getAssetFinder());

				var packData = collector.collectNewPack(() -> {
					models.addAll(_editor.selectionDropped(x, y, data));
				});

				if (!models.isEmpty()) {
					sendBatch(client,

							new LoadAssetsMessage(packData),

							new DropObjectsMessage(models),

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
