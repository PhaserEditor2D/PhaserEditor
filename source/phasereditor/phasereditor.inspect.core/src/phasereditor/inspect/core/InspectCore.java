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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IType;
import org.json.JSONObject;

import phasereditor.inspect.core.examples.ExamplesModel;
import phasereditor.inspect.core.jsdoc.PhaserJSDoc;
import phasereditor.inspect.core.resources.InspectCoreResources;
import phasereditor.inspect.core.templates.TemplatesModel;

public class InspectCore {

	public static final String RESOURCES_PLUGIN_ID = InspectCoreResources.PLUGIN_ID;

	public static final String BUILTIN_PHASER_VERSION = "2.4.7";

	public static final String PREF_BUILTIN_PHASER_VERSION = "phasereditor.inspect.core.builtInPhaserVersion";
	public static final String PREF_USER_PHASER_VERSION_PATH = "phasereditor.inspect.core.userPhaserVersion";

	protected static ExamplesModel _examplesModel;
	private static TemplatesModel _builtInTemplates;

	public static PhaserJSDoc getPhaserHelp() {
		return PhaserJSDoc.getInstance();
	}
	
	public static ExamplesModel getExamplesModel() {
		if (_examplesModel == null) {
			try {
				Path phaserVersionPath = InspectCore.getPhaserVersionFolder();
				_examplesModel = new ExamplesModel(phaserVersionPath);
				Path cache = phaserVersionPath.resolve("phaser-custom/examples/examples-cache.json");
				if (Files.exists(cache)) {
					_examplesModel.loadCache(cache);
				} else {
					out.println("Cannot find the example cache " + cache);
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
				Path bundlePath = InspectCoreResources.getBundleFolder();
				String rel = "templates";
				Path templatesPath = bundlePath.resolve(rel);
				_builtInTemplates = new TemplatesModel(templatesPath);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		return _builtInTemplates;
	}

	public static String getFullName(IMember member) {
		String name;

		if (member instanceof IType) {
			name = member.getDisplayName();
		} else {
			IType type = member.getDeclaringType();
			if (type == null) {
				return "<invalid-member>";
			}
			name = type.getDisplayName() + "." + member.getDisplayName();
		}
		return name;
	}

	public static String getCurrentPhaserVersion() {
		if (!isBuiltInPhaserVersion()) {
			Path folder = getPhaserVersionFolder();
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
		return BUILTIN_PHASER_VERSION;
	}

	public static Path getPhaserLibrariesFolder() {
		return InspectCoreResources.getBundleFolder().resolve("phaser-libraries");
	}

	public static Path getBuiltInPhaserVersionFolder() {
		return InspectCoreResources.getBundleFolder().resolve("phaser-version");
	}

	public static Path getPhaserVersionFolder() {
		if (isBuiltInPhaserVersion()) {
			return getBuiltInPhaserVersionFolder();
		}

		String path = getPreferenceStore().getString(PREF_USER_PHASER_VERSION_PATH);
		Path folder = Paths.get(path);

		return folder;
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

	static void updatePhaserLibrary() {
		if (isBuiltInPhaserVersion()) {
			return;
		}

		Path libFolder = InspectCore.getPhaserLibrariesFolder();
		Path replaceTo = libFolder.resolve("phaser-api.js");

		Path newVersionFolder = getPhaserVersionFolder();
		Path replaceFrom = newVersionFolder.resolve("phaser-custom/api/phaser-api.js");
		try {
			out.println("Copying " + replaceFrom + " to " + replaceTo);
			Files.copy(replaceFrom, replaceTo, StandardCopyOption.REPLACE_EXISTING);
			out.println("done");
		} catch (IOException e) {
			setBuiltInPhaserVersion(true);
			throw new RuntimeException(e);
		}
	}

}
