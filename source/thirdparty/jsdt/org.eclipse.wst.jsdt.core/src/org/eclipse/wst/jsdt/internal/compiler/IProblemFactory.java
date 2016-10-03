/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler;

import java.util.Locale;

import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;

/*
 * Factory used from inside the compiler to build the actual problems
 * which are handed back in the compilation result.
 *
 * This allows sharing the internal problem representation with the environment.
 *
 * Note: The factory is responsible for computing and storing a localized error message.
 */

public interface IProblemFactory {

	CategorizedProblem createProblem(
		char[] originatingFileName,
		int problemId,
		String[] problemArguments,
		String[] messageArguments, // shorter versions of the problemArguments
		int severity,
		int startPosition,
		int endPosition,
		int lineNumber,
		int columnNumber);

	Locale getLocale();

	String getLocalizedMessage(int problemId, String[] messageArguments);
}
