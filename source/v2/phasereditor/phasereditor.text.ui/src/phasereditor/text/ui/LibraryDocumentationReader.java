/**
 * 
 */
package phasereditor.text.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.ui.IDocumentationReader;

import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.jsdoc.LibraryDocumentationIndex;

/**
 * @author arian
 *
 */
public class LibraryDocumentationReader implements IDocumentationReader {

	@Override
	public boolean appliesTo(IMember member) {
		String fullname = InspectCore.getFullName(member);
		LibraryDocumentationIndex index = LibraryDocumentationIndex.getInstance();
		String doc = index.getDocumentation(fullname);
		return doc != null;
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
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new StringReader(sb.toString());
	}

	@Override
	public Reader getContentReader(IMember member, boolean allowInherited) throws JavaScriptModelException {
		String fullname = InspectCore.getFullName(member);
		LibraryDocumentationIndex index = LibraryDocumentationIndex.getInstance();
		String doc = index.getDocumentation(fullname);
		return new StringReader(doc);
	}

	@Override
	public Reader getContentReader(ILocalVariable declaration, boolean allowInherited) throws JavaScriptModelException {
		// use default
		return null;
	}

}
