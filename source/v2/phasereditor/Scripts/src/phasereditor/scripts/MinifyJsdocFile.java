// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;

import phasereditor.inspect.core.InspectCore;

/**
 * @author arian
 *
 */
public class MinifyJsdocFile {
	private static Path _wsPath;
	private static Path _metadataProjectPath;
	private static Path _phaserJsdocFile;

	public static void main(String[] args) throws IOException {
		_wsPath = Paths.get(".").toAbsolutePath().getParent().getParent();
		_metadataProjectPath = _wsPath.resolve(InspectCore.RESOURCES_METADATA_PLUGIN);
		_phaserJsdocFile = _metadataProjectPath.resolve("phaser-custom/phaser3-docs/json/phaser.json");

		long size1 = Files.size(_phaserJsdocFile);

		out.println("Initial size: " + size1 / 1024f / 1024f + "MB");

		JSONObject data;

		try (InputStream input = Files.newInputStream(_phaserJsdocFile)) {
			data = JSONObject.read(input);
			JSONArray array = data.getJSONArray("docs");
			JSONArray newArray = new JSONArray();
			for (int i = 0; i < array.length(); i++) {
				JSONObject entry = array.getJSONObject(i);

				String access = entry.optString("access", "");

				if (access.equals("private") || access.equals("protected")) {
					continue;
				}

				entry.remove("author");
				entry.remove("copyright");
				entry.remove("license");
				entry.remove("___id");
				entry.remove("tags");

				JSONObject meta = entry.optJSONObject("meta");
				if (meta != null) {
					meta.remove("code");
					meta.remove("vars");
					entry.put("meta", meta);
				}

				newArray.put(entry);
			}
			
			data.put("docs", newArray);
			
		}

		Files.write(_phaserJsdocFile, data.toString().getBytes());

		long size2 = Files.size(_phaserJsdocFile);

		out.println("Final size: " + size2 / 1024f / 1024f + "MB");
		out.println("Reduced " + (size1 - size2) / 1024f / 1024f + "MB");
	}
}
