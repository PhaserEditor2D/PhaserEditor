/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.core.infer;

import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;

public interface IInferEngineExtension {
	/**
     * Set compilation unit for processing.
     */
	void setCompilationUnit(CompilationUnitDeclaration parsedUnit, char[] contents);
}
