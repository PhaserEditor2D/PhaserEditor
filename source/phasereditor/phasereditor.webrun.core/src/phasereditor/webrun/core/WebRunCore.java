// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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

import java.net.ServerSocket;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;

public class WebRunCore {
	private static Server _server;

	public static synchronized void startServerIfNotRunning() {
		if (!isServerRunning()) {
			startServer();
		}
	}

	private static boolean isServerRunning() {
		return _server != null && _server.isRunning();
	}

	private static void startServer() {
		// TODO: discover port
		if (_server != null) {
			try {
				_server.stop();
			} catch (Exception e) {
				e.printStackTrace();
				MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "HTTP Server",
						e.getClass().getName() + ": " + e.getMessage());
			}
		}

		String path = ResourcesPlugin.getWorkspace().getRoot().getLocation().toPortableString();

		int port = 0;

		int p = 1_982;
		while (port == 0 && p < 2_000) {
			try (ServerSocket server = new ServerSocket(p)) {
				port = p;
			} catch (Exception e) {
				p++;
			}
		}

		out.println("Serving " + path + ":" + port);
		_server = new Server(port);
		_server.setAttribute("useFileMappedBuffer", "false");

		// resources
		ResourceHandler resourceHandler = new WorkspaceResourcesHandler();
		resourceHandler.setMinMemoryMappedContentLength(-1);
		resourceHandler.setDirectoriesListed(true);
		resourceHandler.setWelcomeFiles(new String[] { "index.html" });
		// set the folder to serve
		resourceHandler.setResourceBase(path);

		Handler[] handlers = { resourceHandler };

		// collection
		HandlerList handlerList = new HandlerList();
		handlerList.setHandlers(handlers);
		_server.setHandler(handlerList);

		// start server
		try {
			_server.start();
			// _server.join();
		} catch (Exception e) {
			e.printStackTrace();
			MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "HTTP Server",
					e.getClass().getName() + ": " + e.getMessage());
		}

	}

	@SuppressWarnings("resource")
	public synchronized static int getServerPort() {
		if (_server == null || _server.getConnectors().length == 0) {
			return 0;
		}
		ServerConnector connector = (ServerConnector) _server.getConnectors()[0];
		return connector.getLocalPort();
	}
}
