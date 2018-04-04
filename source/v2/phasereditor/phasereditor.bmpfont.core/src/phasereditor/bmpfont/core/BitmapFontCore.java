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
package phasereditor.bmpfont.core;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Document;

/**
 * @author arian
 *
 */
public class BitmapFontCore {

	public static boolean isXmlBitmapFontContent(InputStream input) throws Exception {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(input);
		doc.getDocumentElement().normalize();

		if (doc.getElementsByTagName("info").getLength() == 0) {
			return false;
		}

		if (doc.getElementsByTagName("common").getLength() == 0) {
			return false;
		}

		if (doc.getElementsByTagName("char").getLength() == 0) {
			return false;
		}

		return true;
	}

	public static boolean isJsonBitmapFontContent(InputStream contents) {
		JSONObject doc = new JSONObject(new JSONTokener(contents));

		if (doc.has("font")) {
			JSONObject fontDoc = doc.getJSONObject("font");
			return fontDoc.has("chars");
		}

		return false;
	}

	public static boolean isBitmapFontJsonFile(IFile file) throws CoreException {
		if (!file.exists() || !file.isSynchronized(IResource.DEPTH_ONE)) {
			return false;
		}
		IContentDescription desc = file.getContentDescription();
		if (desc == null) {
			return false;
		}

		IContentType contentType = desc.getContentType();
		String id = contentType.getId();
		return id.equals(JsonBitmapFontContentType.CONTENT_TYPE_ID);
	}

	public static boolean isBitmapFontXmlFile(IFile file) throws CoreException {
		if (!file.exists() || !file.isSynchronized(IResource.DEPTH_ONE)) {
			return false;
		}
		IContentDescription desc = file.getContentDescription();
		if (desc == null) {
			return false;
		}

		IContentType contentType = desc.getContentType();
		String id = contentType.getId();
		return id.equals(XmlBitmapFontContentType.CONTENT_TYPE_ID);
	}
}
