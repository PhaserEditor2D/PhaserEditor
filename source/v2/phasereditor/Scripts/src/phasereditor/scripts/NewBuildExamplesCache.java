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
package phasereditor.scripts;

import static java.lang.System.out;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * @author arian
 *
 */
public class NewBuildExamplesCache {
	public static void main(String[] args) {

		startServer("/home/arian/Documents/Phaser/phaser3-examples/public");

		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());

		Browser browser = new Browser(shell, 0);
		// browser.setUrl("http://127.0.0.1:8080/view.html?src=src/spine/basic%20spineboy.js");
		browser.setUrl("http://127.0.0.1:1994/view.html?src=src/spine/basic%20spineboy.js");

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}

	private static void startServer(String examplesPath) {
		Server _server = new Server(1994);
		_server.setAttribute("useFileMappedBuffer", "false");
		HandlerList handlerList = new HandlerList();

		ContextHandler context = new ContextHandler("/");

		ResourceHandler resourceHandler = new ResourceHandler() {
			@Override
			public Resource getResource(String path) {
				out.println("URL: " + path);
				return super.getResource(path);
			}
		};
		resourceHandler.setCacheControl("no-store,no-cache,must-revalidate");
		resourceHandler.setDirectoriesListed(true);
		resourceHandler.setWelcomeFiles(new String[] { "index.html" });
		resourceHandler.setResourceBase(examplesPath);
		context.setHandler(resourceHandler);
		handlerList.addHandler(context);

		_server.setHandler(handlerList);

		try {
			_server.start();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
}
