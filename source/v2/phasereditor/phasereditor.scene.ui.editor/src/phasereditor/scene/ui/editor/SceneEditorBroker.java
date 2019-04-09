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

import org.json.JSONObject;

import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.ui.editor.messages.CreateGameMessage;
import phasereditor.scene.ui.editor.messages.SelectObjectsMessage;
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

	public void sendAll(ApiMessage msg) {
		ApiHub.sendMessageAllClients(_channel, wrapMessage(msg));
	}

	@SuppressWarnings("static-method")
	public void send(Object client, ApiMessage msg) {
		ApiHub.sendMessage(client, wrapMessage(msg));
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
	}

	public String getUrl() {
		return "http://localhost:" + WebRunCore.getServerPort()
				+ "/extension/phasereditor.scene.ui.editor/sceneEditor/index.html?channel=" + _channel;
	}

	private void messageReceived(Object client, JSONObject msg) {
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
		default:
			break;
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
