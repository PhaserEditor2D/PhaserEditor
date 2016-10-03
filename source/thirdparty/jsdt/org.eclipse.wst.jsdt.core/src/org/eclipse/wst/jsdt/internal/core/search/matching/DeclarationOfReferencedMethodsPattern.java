/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.search.matching;

//import java.util.HashSet;

import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleSet;

public class DeclarationOfReferencedMethodsPattern extends MethodPattern {

protected IJavaScriptElement enclosingElement;
protected SimpleSet knownMethods;

public DeclarationOfReferencedMethodsPattern(IJavaScriptElement enclosingElement) {
	super(false, true, false,null, R_PATTERN_MATCH);

	this.enclosingElement = enclosingElement;
	this.knownMethods = new SimpleSet();
	((InternalSearchPattern)this).mustResolve = true;
}
}
