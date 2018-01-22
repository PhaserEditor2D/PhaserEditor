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

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;

/**
 * @author arian
 *
 */
public class JsonBitmapFontContentType implements IContentDescriber {

	public static final String CONTENT_TYPE_ID = "phasereditor.bmpfont.core.jsonBitmapFontContentType";

	public JsonBitmapFontContentType() {
	}

	@Override
	public int describe(InputStream contents, IContentDescription description) throws IOException {
		boolean valid;
		try {
			valid = BitmapFontCore.isJsonBitmapFontContent(contents);
		} catch (Exception e) {
			e.printStackTrace();
			valid = false;
		}
		return valid ? VALID : INVALID;
	}

	@Override
	public QualifiedName[] getSupportedOptions() {
		return new QualifiedName[0];
	}

}
