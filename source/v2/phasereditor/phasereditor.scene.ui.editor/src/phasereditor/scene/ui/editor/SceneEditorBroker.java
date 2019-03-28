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

import org.json.JSONObject;

import phasereditor.scene.ui.editor.messages.RefreshAllMessage;
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

	public void send(ApiMessage... list) {

		var batch = new BatchMessage();

		for (var msg : list) {
			batch.add(msg);
		}

		sendBatch(batch);
	}

	public void sendBatch(BatchMessage batch) {
		ApiHub.sendMessageAsync(_channel, batch);
	}

	public void dispose() {
		ApiHub.removeListener(_channel, this::messageReceived);
	}

	public String getUrl() {
		return "http://localhost:" + WebRunCore.getServerPort()
				+ "/extension/phasereditor.scene.ui.editor/sceneEditor/index.html?channel=" + _channel;
	}

	private void messageReceived(JSONObject msg) {
		var method = msg.getString("method");
		
		switch (method) {
		case "GetRefreshAll":
			send(new RefreshAllMessage(_editor));
			break;

		default:
			break;
		}
	}
}
