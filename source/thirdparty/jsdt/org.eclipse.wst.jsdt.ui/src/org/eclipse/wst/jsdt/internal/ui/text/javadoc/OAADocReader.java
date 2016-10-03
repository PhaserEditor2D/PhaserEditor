/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.ui.text.javadoc;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.internal.core.MetadataFile;
import org.eclipse.wst.jsdt.internal.oaametadata.DocumentedElement;
import org.eclipse.wst.jsdt.internal.oaametadata.Exception;
import org.eclipse.wst.jsdt.internal.oaametadata.Method;
import org.eclipse.wst.jsdt.internal.oaametadata.Parameter;
import org.eclipse.wst.jsdt.internal.oaametadata.VersionableElement;

public class OAADocReader extends Reader {

	StringReader sr;
	StringBuffer buffer = new StringBuffer();

	public OAADocReader(MetadataFile openable, IMember member) {

		getDoc(openable, member);
	}

	private void getDoc(MetadataFile openable, IMember member) {
		DocumentedElement documentation = openable.getDocumentation(member);
		VersionableElement versionableElement = (documentation instanceof VersionableElement) ? (VersionableElement) documentation
				: null;
		Method method = (documentation instanceof Method) ? (Method) documentation
				: null;
		if (documentation != null) {
			if (documentation.description != null) {
				buffer.append("<p>"); //$NON-NLS-1$
				buffer.append(documentation.description);
				buffer.append("</p>"); //$NON-NLS-1$
			}

			if (method!=null)
			  printMethod(method);

		}
		buffer.append("</dl>"); //$NON-NLS-1$

		sr = new StringReader(buffer.toString());
	}

	private void printMethod(Method method) {
			buffer.append("<dl>"); //$NON-NLS-1$
		if ( method.parameters != null
					&& method.parameters.length > 0) {
				printSectionHead(JavaDocMessages.JavaDoc2HTMLTextReader_parameters_section);
				for (int i = 0; i < method.parameters.length; i++) {
					Parameter parameter = method.parameters[i];
					buffer.append("<dd>"); //$NON-NLS-1$
					buffer.append("<b>").append(parameter.name).append("</b> "); //$NON-NLS-1$ //$NON-NLS-2$
					if (parameter.description!=null)
						buffer.append(parameter.description);
					buffer.append("</dd>"); //$NON-NLS-1$
				}
			}

			if (method.returns != null)
				printSection(
						JavaDocMessages.JavaDoc2HTMLTextReader_returns_section,
						method.returns.dataType, method.returns.description);

		if ( method.exceptions != null
					&& method.exceptions.length > 0) {
				printSectionHead(JavaDocMessages.JavaDoc2HTMLTextReader_throws_section);
				for (int i = 0; i < method.exceptions.length; i++) {
					Exception exception = method.exceptions[i];
					buffer.append("<dd>"); //$NON-NLS-1$
					buffer.append(exception.description); //$NON-NLS-1$
					buffer.append("</dd>"); //$NON-NLS-1$
				}
			}
	}

	public void close() throws IOException {
		sr.close();
	}

	public int read(char[] cbuf, int off, int len) throws IOException {
		return sr.read(cbuf, off, len);
	}

	private void printSectionHead(String tag) {
		buffer.append("<dt>"); //$NON-NLS-1$
		buffer.append(tag);
		buffer.append("</dt>"); //$NON-NLS-1$
	}

	private void printSection(String tag, String nameIn, String descriptionIn) {
		String name = (nameIn != null && nameIn.length() > 0) ? nameIn : null;
		String description = (descriptionIn != null && descriptionIn.length() > 0) ? descriptionIn
				: null;
		if (name != null || description != null) {
			buffer.append("<dt>"); //$NON-NLS-1$
			buffer.append(tag);
			buffer.append("<dd>"); //$NON-NLS-1$
			if (name != null)
				buffer.append("<b>").append(name).append("</b>"); //$NON-NLS-1$ //$NON-NLS-2$
			if (description != null)
				buffer.append(description);
			buffer.append("</dd>"); //$NON-NLS-1$
			buffer.append("</dt>"); //$NON-NLS-1$
		}
	}

}
