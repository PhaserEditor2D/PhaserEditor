// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * @author arian
 *
 */
public class CanvasCore {
	public static final String PLUGIN_ID = Activator.PLUGIN_ID;
	public static final String CANVAS_FILE_PROBLEM_MARKER_ID = "phasereditor.canvas.core.problem";

	public static void logError(Exception e) {
		e.printStackTrace();
		StatusManager.getManager().handle(new Status(IStatus.ERROR, CanvasCore.PLUGIN_ID, e.getMessage(), e));
	}

	public static String getValidJavaScriptName(String name) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (char c : name.toCharArray()) {
			if (i == 0 && !Character.isJavaIdentifierStart(c)) {
				sb.append("_");
			} else if (Character.isJavaIdentifierPart(c)) {
				sb.append(c);
			} else {
				sb.append("_");
			}
			i++;
		}
		return sb.toString();
	}

	public static boolean isCanvasFile(IFile file) {
		if (!file.exists() || !file.isSynchronized(IResource.DEPTH_ONE)) {
			return false;
		}

		// TODO: missing to define content type
		return file.getFileExtension().equals("canvas");
	}
}
