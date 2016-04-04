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
package phasereditor.text.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.ui.IDocumentationReader;

import phasereditor.inspect.core.jsdoc.PhaserJSDoc;

public class PhaserDocumentationReader implements IDocumentationReader {

	private static final String END_JSDOC = "##end#jsdoc#";

	public PhaserDocumentationReader() {
	}

	@Override
	public boolean appliesTo(IMember member) {
		boolean applies = getJSDoc().isPhaserMember(member);
		return applies;
	}

	private static PhaserJSDoc getJSDoc() {
		return PhaserJSDoc.getInstance();
	}

	@Override
	public boolean appliesTo(ILocalVariable declaration) {
		return true;
	}

	@Override
	public Reader getDocumentation2HTMLReader(Reader contentReader) {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = new BufferedReader(contentReader);
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				if (line.equals(END_JSDOC)) {
					break;
				}
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new StringReader(sb.toString());
	}

	@Override
	public Reader getContentReader(IMember member, boolean allowInherited)
			throws JavaScriptModelException {
		String jsDoc = getJSDoc().getJSDoc(member);
		StringReader reader = new StringReader(jsDoc + "\n" + END_JSDOC + "\n");
		return reader;
	}

	@Override
	public Reader getContentReader(ILocalVariable declaration,
			boolean allowInherited) throws JavaScriptModelException {
		// use default
		return null;
	}

}
