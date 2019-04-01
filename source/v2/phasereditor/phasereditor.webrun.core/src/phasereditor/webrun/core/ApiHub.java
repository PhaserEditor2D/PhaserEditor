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
package phasereditor.webrun.core;

import static java.lang.System.out;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.json.JSONObject;

/**
 * @author arian
 *
 */
public class ApiHub extends WebSocketAdapter {

	private static Map<String, List<ApiHub>> _channelSocketListMap = new HashMap<>();
	private static Map<String, List<IMessageListener>> _channelListenerListMap = new HashMap<>();

	private String _channel;

	public ApiHub(String channel) {
		_channel = channel;

		synchronized (_channelSocketListMap) {

			var list = _channelSocketListMap.get(channel);

			if (list == null) {
				list = new ArrayList<>();
				_channelSocketListMap.put(channel, list);
			}

			list.add(this);

		}
	}

	@Override
	public void onWebSocketConnect(Session sess) {
		super.onWebSocketConnect(sess);

		out.println("ApiHub.onWebSocketConnect@" + _channel);
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {

		out.println("ApiHub.onWebSocketClose@" + _channel);

		synchronized (_channelSocketListMap) {

			var list = _channelSocketListMap.get(_channel);

			if (list != null) {
				list.remove(this);
			}
		}

		super.onWebSocketClose(statusCode, reason);
	}

	@Override
	public void onWebSocketText(String message) {
		out.println("ApiHub.onWebSocketText@" + _channel);
		out.println(message);
		out.println("-----------------------------");

		var data = new JSONObject(message);

		synchronized (_channelListenerListMap) {
			var list = _channelListenerListMap.get(_channel);

			if (list != null) {
				for (var listener : list) {
					listener.messageReceived(data);
				}
			}
		}
	}

	public static void addListener(String channel, IMessageListener listener) {

		synchronized (_channelListenerListMap) {

			var list = _channelListenerListMap.get(channel);

			if (list == null) {
				list = new ArrayList<>();
				_channelListenerListMap.put(channel, list);
			}

			list.add(listener);

		}
	}

	public static void removeListener(String channel, IMessageListener listener) {
		synchronized (_channelListenerListMap) {

			var list = _channelListenerListMap.get(channel);

			if (list != null) {
				list.remove(listener);
			}
		}
	}

	public static void sendMessageAsync(String channel, ApiMessage message) {
		sendMessageAsync(channel, message.getData());
	}

	public static void sendMessageAsync(String channel, JSONObject message) {
		synchronized (_channelSocketListMap) {

			var list = _channelSocketListMap.get(channel);

			if (list != null) {
				for (var socket : list) {
					try {
						out.println(socket.hashCode() + "@ ApiHub.sendMessageAsync: " + message.toString(2));
						socket.getRemote().sendString(message.toString());
					} catch (IOException e) {
						WebRunCore.logError(e);
					}
				}
			}
		}
	}

	public static interface IMessageListener {
		public void messageReceived(JSONObject message);
	}
}
