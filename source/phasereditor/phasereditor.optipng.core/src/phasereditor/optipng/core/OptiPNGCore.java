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
package phasereditor.optipng.core;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.IPreferenceStore;

import phasereditor.inspect.core.InspectCore;

public class OptiPNGCore {
	public static final String PLUGIN_ID = Activator.PLUGIN_ID;

	private static QualifiedName PERSIST_KEY = new QualifiedName("phasereditor.optipng", "hash");

	public static final String PREF_OPTI_PNG_LEVEL = PLUGIN_ID + "compressionLeve;";

	public static final String PREF_OPTI_PNG_EXTRA_PARAMS = PLUGIN_ID + "extraParams";

	public static void updateHashCache(IResource resource) throws Exception {
		String hash2 = computeHash(resource);
		resource.setPersistentProperty(PERSIST_KEY, hash2);
	}

	public static void optimize(IPath path) {
		String ospath = path.toOSString();
		IPreferenceStore prefs = getPreferenceStore();

		String level = prefs.getString(PREF_OPTI_PNG_LEVEL);
		String extra = prefs.getString(PREF_OPTI_PNG_EXTRA_PARAMS);

		try {
			List<String> params = new ArrayList<>(Arrays.asList(level, "-quiet", ospath));
			params.addAll(Arrays.asList(extra.split(" ")));

			ProcessBuilder procBuilder = InspectCore.createProcessBuilder("optipng/optipng",
					params.toArray(new String[params.size()]));

			Process proc = procBuilder.start();
			int code = proc.waitFor();
			if (code != 0) {
				throw new IOException("Something wrong happened, exit code for "
						+ Arrays.toString(procBuilder.command().toArray()) + " is " + code);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private static String computeHash(IResource resource) throws Exception {
		if (resource instanceof IFile) {
			return getMD5Checksum(getInputStreamFromFileSystem(resource));
		}
		return "";
	}

	private static InputStream getInputStreamFromFileSystem(IResource resource) throws FileNotFoundException {
		return new FileInputStream(resource.getLocation().toFile());
	}

	private static byte[] createChecksum(InputStream input) throws Exception {
		byte[] buffer = new byte[1024];
		MessageDigest complete = MessageDigest.getInstance("MD5");
		int numRead;

		do {
			numRead = input.read(buffer);
			if (numRead > 0) {
				complete.update(buffer, 0, numRead);
			}
		} while (numRead != -1);

		input.close();
		return complete.digest();
	}

	public static String getMD5Checksum(InputStream input) throws Exception {
		byte[] b = createChecksum(input);
		String result = "";

		for (int i = 0; i < b.length; i++) {
			result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}

	public static IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	public static boolean isPNG(IResource res) {
		return res != null && res instanceof IFile && res.getFileExtension().toLowerCase().equals("png");
	}
}
