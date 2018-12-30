// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.ide.intro;

import static java.lang.System.out;

import java.nio.file.Paths;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.ui.internal.ide.application.IDEApplication;

import phasereditor.lic.LicCore;

/**
 * @author arian
 *
 */
@SuppressWarnings("restriction")
public class PhaserIDE2 extends IDEApplication {

	@Override
	public Object start(IApplicationContext appContext) throws Exception {

		out.println("Starting Phaser IDE application " + LicCore.PRODUCT_VERSION);

		out.println("Starting license monitor");
		LicCore.startMonitor();

		{
			var nodeJsLocation = System.getProperty("NodeJsLocation");
			
			if (nodeJsLocation == null) {
				var eclipseHomeUrl = Platform.getInstallLocation().getURL();
				var eclipseHomePath = eclipseHomeUrl.getPath();
				var nodeJsPathname = "node/bin/node";
				if (Platform.getOS().equals(Platform.OS_WIN32)) {
					if (eclipseHomePath.startsWith("/")) {
						eclipseHomePath = eclipseHomePath.substring(1);
					}
					nodeJsPathname = "node/node.exe";
				}
				var nodeJsPath = Paths.get(eclipseHomePath).resolve(nodeJsPathname);
				nodeJsLocation = nodeJsPath.toString();
				System.setProperty("NodeJsLocation", nodeJsPath.toString());
			}
			
			out.println("Phaser Editor: NodeJS location = " + nodeJsLocation);
		}

		out.println("Starting Eclipse application");
		Object res = super.start(appContext);

		return res;
	}
}
