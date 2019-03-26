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
import java.nio.file.Path;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;

import phasereditor.inspect.core.InspectCore;

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

		int port = 0;

		int p = 1_982;
		while (port == 0 && p < 2_000) {
			try (ServerSocket server = new ServerSocket(p)) {
				port = p;
			} catch (Exception e) {
				p++;
			}
		}

		out.println("Serving at port " + port);

		_server = new Server(port);
		_server.setAttribute("useFileMappedBuffer", "false");

		HandlerList handlerList = new HandlerList();

		addApiWebSocketHandler(handlerList);
		addJSLibsHandler(handlerList);
		addProjectsHandler(handlerList);
		addExamplesHandler(handlerList);
		addPhaserCodeHandler(handlerList);
		addAssetsHandler(handlerList);
		addExampleServletsHandler(handlerList);

		// collection

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

	private static void addApiWebSocketHandler(HandlerList handlerList) {
		out.println("Serving API websocket at /wsapi");
		var context = new ServletContextHandler();
		context.setContextPath("/ws");
		context.addServlet(ApiWebSocketServlet.class, "/api");
		handlerList.addHandler(context);
	}

	private static void addJSLibsHandler(HandlerList handlerList) {
		Path file = InspectCore.getBundleFile("phasereditor.webrun.core", "jslibs");
		String path = file.toFile().getAbsolutePath();

		addStaticFilesHandler(handlerList, path, "/jslibs");
	}

	private static void addProjectsHandler(HandlerList handlerList) {
		String path = ResourcesPlugin.getWorkspace().getRoot().getLocation().toPortableString();

		out.println("Serving projects (/projects) from: " + path);

		ContextHandler context = new ContextHandler("/projects");

		ResourceHandler resourceHandler = new WorkspaceResourcesHandler();
		resourceHandler.setCacheControl("no-store,no-cache,must-revalidate");
		resourceHandler.setDirectoriesListed(true);
		resourceHandler.setWelcomeFiles(new String[] { "index.html" });
		resourceHandler.setResourceBase(path);
		context.setHandler(resourceHandler);
		handlerList.addHandler(context);
	}

	private static void addPluginResourcesHandler(HandlerList handlerList, String url, String plugin,
			String pluginFolder) {
		out.println("Serving plugin folder: " + plugin + "/" + pluginFolder);

		ContextHandler context = new ContextHandler(url);

		ResourceHandler resourceHandler = new PluginResourcesHandler(plugin, pluginFolder);
		resourceHandler.setCacheControl("no-store,no-cache,must-revalidate");
		context.setHandler(resourceHandler);
		handlerList.addHandler(context);

	}

	private static void addExampleServletsHandler(HandlerList handlerList) {
		ServletHandler handler = new ServletHandler();

		handler.addServletWithMapping(new ServletHolder(new PhaserExamplesServlet()), "/phaser-examples");
		handler.addServletWithMapping(new ServletHolder(new PhaserExampleCategoryServlet()),
				"/phaser-examples-category");
		handler.addServletWithMapping(new ServletHolder(new PhaserExampleServlet()), "/phaser-example");

		handlerList.addHandler(handler);
	}

	private static void addPhaserCodeHandler(HandlerList handlerList) {
		Path file = InspectCore.getBundleFile(InspectCore.RESOURCES_PHASER_CODE_PLUGIN, "phaser-master");
		String path = file.toFile().getAbsolutePath();

		addStaticFilesHandler(handlerList, path, "/phaser-code");
	}

	private static void addExamplesHandler(HandlerList handlerList) {
		Path file = InspectCore.getBundleFile(InspectCore.RESOURCES_EXAMPLES_PLUGIN, "phaser3-examples/public");
		String path = file.toFile().getAbsolutePath();

		addStaticFilesHandler(handlerList, path, "/examples-files");
	}

	private static void addAssetsHandler(HandlerList handlerList) {
		addPluginResourcesHandler(handlerList, "/assets", InspectCore.RESOURCES_EXAMPLES_PLUGIN,
				"phaser3-examples/public/assets");

		addPluginResourcesHandler(handlerList, "/plugins", InspectCore.RESOURCES_EXAMPLES_PLUGIN,
				"phaser3-examples/public/plugins");
	}

	private static void addStaticFilesHandler(HandlerList handlerList, String path, String url) {
		out.println("Serving (" + url + ") from: " + path);

		ContextHandler context = new ContextHandler(url);

		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setCacheControl("no-store,no-cache,must-revalidate");
		resourceHandler.setDirectoriesListed(true);
		resourceHandler.setWelcomeFiles(new String[] { "index.html" });
		resourceHandler.setResourceBase(path);
		context.setHandler(resourceHandler);
		handlerList.addHandler(context);
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
