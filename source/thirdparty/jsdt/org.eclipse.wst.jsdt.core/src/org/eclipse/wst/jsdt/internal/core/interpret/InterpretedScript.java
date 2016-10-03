/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.interpret;

import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ProgramElement;
import org.eclipse.wst.jsdt.internal.compiler.util.Util;

public class InterpretedScript {
	public CompilationUnitDeclaration compilationUnit;
	int [] lineEnds;
	int sourceSize;
	
	public InterpretedScript(CompilationUnitDeclaration compilationUnit, int[] lineEnds, int sourceSize) {
		super();
		this.compilationUnit = compilationUnit;
		this.lineEnds = lineEnds;
		this.sourceSize=sourceSize;
	}
	
	public int lineNumber(ProgramElement element)
	{
		return Util.getLineNumber(element.sourceStart, lineEnds, 0, this.sourceSize);
	}
}
