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
package org.eclipse.wst.jsdt.internal.codeassist.select;

/*
 * Selection node build by the parser in any case it was intending to
 * reduce a type reference containing the completion identifier as part
 * of a qualified name.
 * e.g.
 *
 *	class X extends java.lang.[start]Object[end]
 *
 *	---> class X extends <SelectOnType:java.lang.Object>
 *
 */

import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class SelectionOnQualifiedTypeReference extends QualifiedTypeReference {
public SelectionOnQualifiedTypeReference(char[][] previousIdentifiers, char[] selectionIdentifier, long[] positions) {
	super(
		CharOperation.arrayConcat(previousIdentifiers, selectionIdentifier),
		positions);
}
public void aboutToResolve(Scope scope) {
	getTypeBinding(scope.parent); // step up from the ClassScope
}
protected TypeBinding getTypeBinding(Scope scope) {
	// it can be a package, type or member type
	Binding binding = scope.getTypeOrPackage(tokens);
	if (!binding.isValidBinding()) {
			// tolerate some error cases
			if (binding.problemId() == ProblemReasons.NotVisible){
				throw new SelectionNodeFound(binding);
			}
		scope.problemReporter().invalidType(this, (TypeBinding) binding);
		throw new SelectionNodeFound();
	}

	throw new SelectionNodeFound(binding);
}
public StringBuffer printExpression(int indent, StringBuffer output) {

	output.append("<SelectOnType:"); //$NON-NLS-1$
	for (int i = 0, length = tokens.length; i < length; i++) {
		if (i > 0) output.append('.');
		output.append(tokens[i]);
	}
	return output.append('>');
}
}
