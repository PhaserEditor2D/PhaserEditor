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
package phasereditor.atlas.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

public class AtlasCore {
	public static final String TEXTURE_ATLAS_MULTI = "TEXTURE_ATLAS_MULTI";
	public static final String TEXTURE_ATLAS_JSON_ARRAY = "TEXTURE_ATLAS_JSON_ARRAY";
	public static final String TEXTURE_ATLAS_JSON_HASH = "TEXTURE_ATLAS_JSON_HASH";
	public static final String TEXTURE_ATLAS_XML_STARLING = "TEXTURE_ATLAS_XML_STARLING";
	public static final String TEXTURE_ATLAS_UNITY = "TEXTURE_ATLAS_UNITY";

	private static final Set<String> IMG_EXTS = new HashSet<>(Arrays.asList("png", "jpg", "gif", "bmp"));

	private static AtlasFileDataCache _atlasFileCache;

	public static AtlasFileDataCache getAtlasFileCache() {
		if (_atlasFileCache == null) {
			_atlasFileCache = new AtlasFileDataCache();
		}
		return _atlasFileCache;
	}

	public static boolean isImageFile(IFile file) {
		return IMG_EXTS.contains(file.getFileExtension());
	}

	/**
	 * Get the atlas JSON format of the given content. Possible values are
	 * {@link AtlasAssetModel#TEXTURE_ATLAS_JSON_ARRAY} or
	 * {@link AtlasAssetModel#TEXTURE_ATLAS_JSON_HASH}, or null if it does not have
	 * any of those formats.
	 * 
	 * @param contents
	 *            The content to test.
	 * @return If the content has an atlas JSON format, or null if it does not.
	 * @see #isAtlasXMLFormat(InputStream)
	 * @see #isAtlasUnityFormat(InputStream)
	 */
	public static String getAtlasJSONFormat(String contents) {
		try {
			// try json format
			JSONObject obj = new JSONObject(contents);
			{
				Object jsonTextures = obj.opt("textures");
				if (jsonTextures != null && jsonTextures instanceof JSONArray) {
					return TEXTURE_ATLAS_MULTI;
				}
			}

			Object frames = obj.get("frames");
			if (frames instanceof JSONArray) {
				return TEXTURE_ATLAS_JSON_ARRAY;
			}

			return TEXTURE_ATLAS_JSON_HASH;
		} catch (JSONException e) {
			// e.printStackTrace();
		}
		return null;
	}

	/**
	 * Check if the given content has an XML atlas format
	 * {@link AtlasAssetModel#TEXTURE_ATLAS_XML_STARLING}.
	 * 
	 * @param contents
	 *            The content to test.
	 * @return If the content has an atlas XML format.
	 * @see #getAtlasJSONFormat(InputStream)
	 */
	public static boolean isAtlasXMLFormat(InputStream contents) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(contents));
			String line;
			while ((line = reader.readLine()) != null) {
				StringBuilder sb = new StringBuilder();
				for (char c : line.toCharArray()) {
					if (Character.isLetter(c)) {
						sb.append(c);
					}
				}
				if (sb.toString().startsWith("TextureAtlas")) {
					return true;
				}
			}
		} catch (Exception e) {
			// nothing
		}
		return false;
	}

	/**
	 * Check if the given content has a Unity atlas format
	 * {@link AtlasAssetModel#TEXTURE_ATLAS_UNITY}.
	 * 
	 * @param contents
	 *            The content to test.
	 * @return If the content has an atlas XML format.
	 * @see #getAtlasJSONFormat(InputStream)
	 */
	@SuppressWarnings("rawtypes")
	public static boolean isAtlasUnityFormat(InputStream contents) {
		try {
			Yaml yaml = new Yaml();
			var map = (Map) yaml.load(contents);

			map = (Map) map.get("TextureImporter");

			if (map == null) {
				return false;
			}

			map = (Map) map.get("spriteSheet");

			return map != null;
		} catch (Exception e) {
			// nothing
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Return the atlas format of the file, or null if it is not an atlas.
	 * 
	 * @param file
	 *            The file to test.
	 * @return The atlas format, or null if it is not an atlas.
	 * @throws CoreException
	 *             If error.
	 * @throws IOException
	 * @see #TEXTURE_ATLAS_JSON_ARRAY
	 * @see #TEXTURE_ATLAS_JSON_HASH
	 * @see #TEXTURE_ATLAS_XML_STARLING
	 */
	public static String getAtlasFormat(IFile file) throws Exception {
		String format = null;
		String ext = file.getFileExtension().toLowerCase();
		if (ext.equals("json")) {
			var str = new String(Files.readAllBytes(file.getLocation().toFile().toPath()));
			format = getAtlasJSONFormat(str);
		} else if (ext.equals("xml")) {
			try (InputStream input = file.getContents()) {
				if (isAtlasXMLFormat(input)) {
					return TEXTURE_ATLAS_XML_STARLING;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (ext.equals("meta")) {
			try (InputStream input = file.getContents()) {
				if (isAtlasUnityFormat(input)) {
					return TEXTURE_ATLAS_UNITY;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return format;
	}
}
