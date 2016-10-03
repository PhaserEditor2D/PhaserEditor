/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.ui;

import java.io.Reader;

import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;

/**
 * 
 * Provisional API: This class/interface is part of an interim API that is
 * still under development and expected to change significantly before
 * reaching stability. It is being made available at this early stage to
 * solicit feedback from pioneering adopters on the understanding that any
 * code that uses this API will almost certainly be broken (repeatedly) as the
 * API evolves.
 * 
 * Implementors of this interface retrieve raw and formatted documentation for
 * specific JavaScript elements
 */
public interface IDocumentationReader {

	public boolean appliesTo(IMember member);
	public boolean appliesTo(ILocalVariable declaration);
	
	public  Reader getDocumentation2HTMLReader(Reader contentReader);
	
	public  Reader getContentReader(IMember member, boolean allowInherited) throws JavaScriptModelException;
	public  Reader getContentReader(ILocalVariable declaration, boolean allowInherited) throws JavaScriptModelException;
}
