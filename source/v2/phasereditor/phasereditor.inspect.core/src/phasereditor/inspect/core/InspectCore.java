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
package phasereditor.inspect.core;

import static java.lang.System.out;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.statushandlers.StatusManager;
import org.json.JSONObject;

import phasereditor.inspect.core.examples.ExamplesModel;
import phasereditor.inspect.core.jsdoc.PhaserJSDoc;
import phasereditor.inspect.core.templates.TemplatesModel;

public class InspectCore {

	public static final String RESOURCES_EXAMPLES_PLUGIN = "phasereditor.resources.phaser.examples";
	public static final String RESOURCES_METADATA_PLUGIN = "phasereditor.resources.phaser.metadata";
	public static final String RESOURCES_PHASER_CODE_PLUGIN = "phasereditor.resources.phaser.code";
	public static final String RESOURCES_EXECUTABLES_PLUGIN = "phasereditor.resources.executables";
	public static final String RESOURCES_TEMPLATES_PLUGIN = "phasereditor.resources.templates";
	public static final String RESOURCES_JSLIBS_PLUGIN = "phasereditor.resources.jslibs";

	public static final String PHASER_VERSION;

	public static final String PREF_BUILTIN_PHASER_VERSION = "phasereditor.inspect.core.builtInPhaserVersion";
	public static final String PREF_USER_PHASER_VERSION_PATH = "phasereditor.inspect.core.userPhaserVersion";
	public static final String PLUGIN_ID = Activator.PLUGIN_ID;

	protected static ExamplesModel _examplesModel;
	private static TemplatesModel _builtInTemplates;
	private static TemplatesModel _projectTemplates;

	static {
		PHASER_VERSION = readPhaserVersion(InspectCore.getBundleFile(RESOURCES_METADATA_PLUGIN, "phaser-custom"));
		out.println("Built-in Phaser version: " + PHASER_VERSION);
	}

	public static PhaserJSDoc getPhaserHelp() {
		return PhaserJSDoc.getInstance();
	}

	public static ExamplesModel getExamplesModel() {
		if (_examplesModel == null) {
			try {
				// Path phaserVersionPath =
				// InspectCore.getPhaserVersionFolder();
				Path examplesPath = getBundleFile(RESOURCES_EXAMPLES_PLUGIN, "");
				_examplesModel = new ExamplesModel(examplesPath);
				Path cachePath = getBundleFile(RESOURCES_METADATA_PLUGIN, "phaser-custom/examples/examples-cache.json");
				if (Files.exists(cachePath)) {
					_examplesModel.loadCache(cachePath);
				} else {
					StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							"Cannot find the example cache " + cachePath), StatusManager.SHOW);
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		return _examplesModel;
	}

	public static TemplatesModel getGeneralTemplates() {
		if (_builtInTemplates == null) {
			try {
				Path templatesPath = InspectCore.getBundleFile(InspectCore.RESOURCES_TEMPLATES_PLUGIN, "templates");
				_builtInTemplates = new TemplatesModel(templatesPath);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		return _builtInTemplates;
	}
	
	public static TemplatesModel getProjectTemplates() {
		if (_projectTemplates == null) {
			try {
				Path templatesPath = InspectCore.getBundleFile(InspectCore.RESOURCES_TEMPLATES_PLUGIN, "templates_newproject");
				_projectTemplates = new TemplatesModel(templatesPath);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		return _projectTemplates;
	}

	//TODO: removing wst
//	public static String getFullName(IMember member) {
//		String name;
//
//		if (member instanceof IType) {
//			name = member.getDisplayName();
//		} else {
//			IType type = member.getDeclaringType();
//			if (type == null) {
//				return "<invalid-member>";
//			}
//			name = type.getDisplayName() + "." + member.getDisplayName();
//		}
//		return name;
//	}

	/**
	 * @param folder
	 * @return
	 */
	private static String readPhaserVersion(Path folder) {
		try {
			byte[] bytes = Files.readAllBytes(folder.resolve("version-info.json"));
			JSONObject info = new JSONObject(new String(bytes));
			String version = info.getString("phaser-version");
			return version;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static boolean isBuiltInPhaserVersion() {
		return getPreferenceStore().getBoolean(PREF_BUILTIN_PHASER_VERSION);
	}

	public static void setBuiltInPhaserVersion(boolean b) {
		getPreferenceStore().setValue(PREF_BUILTIN_PHASER_VERSION, b);
	}

	public static boolean isValidPhaserVersionFolder(Path path) {
		if (path == null || !Files.exists(path) || !Files.isDirectory(path)) {
			return false;
		}

		if (!Files.exists(path.resolve("version-info.json"))) {
			return false;
		}

		if (!Files.exists(path.resolve("phaser-custom/api/phaser-api.js"))) {
			return false;
		}

		return true;
	}

	public static IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	public static Path getBundleFile(String bundleName, String filePath) {
		try {
			String spec = "platform:/plugin/" + bundleName + "/" + filePath;
			URL url = FileLocator.toFileURL(new URL(spec));
			Path path = Paths.get(new URI("file://" + url.getPath().replace(" ", "%20")));
			return path;
		} catch (IOException | URISyntaxException e) {
			logError(e);
			throw new RuntimeException(e);
		}
	}

	public static void logError(Exception e) {
		StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}

	/**
	 * Create a process taken as root the <code>[install]/resources/[os]</code>
	 * folder.
	 */
	public static ProcessBuilder createProcessBuilder(String exePath, String... args) {
		Path path = getBundleFile(RESOURCES_EXECUTABLES_PLUGIN, getExecutableName(exePath));
		List<String> list = new ArrayList<>();
		list.add(path.toAbsolutePath().toString());
		list.addAll(Arrays.asList(args));
		ProcessBuilder pb = new ProcessBuilder(list);
		return pb;
	}

	public static String getExecutableName(String name) {
		return isWindowsOS() ? name + ".exe" : name;
	}

	public static boolean isWindowsOS() {
		return Platform.getOS().toLowerCase().contains("win");
	}
}
