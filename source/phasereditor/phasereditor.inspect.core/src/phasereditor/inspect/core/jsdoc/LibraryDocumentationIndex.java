/**
 * 
 */
package phasereditor.inspect.core.jsdoc;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.inspect.core.resources.InspectCoreResources;

/**
 * @author arian
 *
 */
public class LibraryDocumentationIndex {
	private static LibraryDocumentationIndex _instance;
	private Map<String, String> _map;

	public static synchronized LibraryDocumentationIndex getInstance() {
		if (_instance == null) {
			long t = currentTimeMillis();

			LibraryDocumentationIndex index = new LibraryDocumentationIndex();

			Path bundleFolder = InspectCoreResources.getBundleFolder();
			try {
				Path indexFile = bundleFolder.resolve("thirdparty-libraries/browser.doc.json").toAbsolutePath()
						.normalize();
				index.addIndexFile(indexFile);

				indexFile = bundleFolder.resolve("thirdparty-libraries/ecma5.doc.json").toAbsolutePath().normalize();
				index.addIndexFile(indexFile);

				_instance = index;
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			out.println("Libraries JSDoc " + (currentTimeMillis() - t));
		}

		return _instance;
	}

	public LibraryDocumentationIndex() {
		_map = new HashMap<>();
	}

	public void addIndexFile(Path indexFile) throws IOException {
		try (InputStream input = Files.newInputStream(indexFile);) {
			JSONObject obj = new JSONObject(new JSONTokener(input));
			for (String k : obj.keySet()) {
				_map.put(k, obj.getString(k));
			}
		}
	}

	public String getDocumentation(String key) {
		if (key.startsWith("_Window.")) {
			String key2 = key.substring(8);
			return _map.get(key2);
		}
		return _map.get(key);
	}
}
