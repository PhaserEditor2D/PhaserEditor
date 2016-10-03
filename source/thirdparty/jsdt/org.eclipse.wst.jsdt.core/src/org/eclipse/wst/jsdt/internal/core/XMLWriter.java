/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core;
import java.io.Writer;

import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.internal.compiler.util.GenericXMLWriter;
import org.eclipse.wst.jsdt.internal.core.util.Util;
/**
 * @since 3.0
 */
class XMLWriter extends GenericXMLWriter {

	public XMLWriter(Writer writer, IJavaScriptProject project, boolean printXmlVersion) {
		super(writer, Util.getLineSeparator((String) null, project), printXmlVersion);
	}
}
