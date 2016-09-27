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
package phasereditor.inspect.core.resources;

import static java.lang.System.out;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class InspectCoreResources {

	private static final String PROP_PHASEREDITOR_RESOURCES = "phasereditor.resources";
	public static final String PLUGIN_ID = Activator.PLUGIN_ID;
	private static Path _resourcesPath;

	/**
	 * The resources path. Resources is a folder in the root directory of the
	 * editor "install". In this folder are stored the external tools (ffmpeg,
	 * optipng, etc...), the templates, the phaser files, etc...
	 */
	public static Path getResourcesPath() {
		if (_resourcesPath == null) {
			String path = System.getProperty(PROP_PHASEREDITOR_RESOURCES);
			if (path == null) {
				try {
					_resourcesPath = Paths.get(Platform.getInstallLocation().getURL().toURI()).resolve("resources");
				} catch (URISyntaxException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			} else {
				_resourcesPath = Paths.get(path);
			}

			out.println("Resources path: " + _resourcesPath);

			if (!Files.exists(_resourcesPath)) {
				Display.getDefault().asyncExec(() -> {
					out.println("Resources path does not exist.");
					Shell shell = Display.getDefault().getActiveShell();
					MessageDialog.openError(shell, "Error", "The resources path '" + _resourcesPath
							+ "' does not exist. Please check your configuration. We are closing...");
					PlatformUI.getWorkbench().close();
				});
			}
		}

		return _resourcesPath;
	}

	public static Path getResourcesPath_CurrentOS() {
		return getResourcesPath().resolve(isWindowsOS() ? "win" : "linux");
	}

	public static Path getResourcesPath_AnyOS() {
		return getResourcesPath().resolve(isWindowsOS() ? "all" : "all");
	}

	public static String getExecutableName(String name) {
		return isWindowsOS() ? name + ".exe" : name;
	}

	/**
	 * Create a process taken as root the <code>[install]/resources/[os]</code>
	 * folder.
	 */
	public static ProcessBuilder createProcessBuilder(String exePath, String... args) {
		List<String> list = new ArrayList<>();

		Path path = getResourcesPath_CurrentOS().resolve(getExecutableName(exePath));

		list.add(path.toAbsolutePath().toString());
		list.addAll(Arrays.asList(args));
		ProcessBuilder pb = new ProcessBuilder(list);
		return pb;
	}

	private static boolean isWindowsOS() {
		return Platform.getOS().toLowerCase().contains("win");
	}
}
