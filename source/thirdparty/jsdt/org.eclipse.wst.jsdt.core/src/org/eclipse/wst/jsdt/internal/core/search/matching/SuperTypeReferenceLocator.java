/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.search.matching;

import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.core.util.QualificationHelpers;

public class SuperTypeReferenceLocator extends PatternLocator {

protected SuperTypeReferencePattern pattern;

public SuperTypeReferenceLocator(SuperTypeReferencePattern pattern) {
	super(pattern);

	this.pattern = pattern;
}
//public int match(ASTNode node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(ConstructorDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(Expression node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(FieldDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(FunctionDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(MessageSend node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(Reference node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(TypeDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
public int match(TypeReference node, MatchingNodeSet nodeSet) {
	if (this.pattern.superTypeName == null)
		return nodeSet.addMatch(node, ((InternalSearchPattern)this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);

	char[] typeRefSimpleName = null;
	if (node instanceof SingleTypeReference) {
		typeRefSimpleName = ((SingleTypeReference) node).token;
	} else { // QualifiedTypeReference
		char[][] tokens = ((QualifiedTypeReference) node).tokens;
		typeRefSimpleName = tokens[tokens.length-1];
	}
	if (matchesName(this.pattern.superTypeName, typeRefSimpleName))
		return nodeSet.addMatch(node, ((InternalSearchPattern)this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);

	return IMPOSSIBLE_MATCH;
}

protected int matchContainer() {
	return CLASS_CONTAINER;
}
protected int referenceType() {
	return IJavaScriptElement.TYPE;
}
public int resolveLevel(ASTNode node) {
	if (!(node instanceof TypeReference)) return IMPOSSIBLE_MATCH;

	TypeReference typeRef = (TypeReference) node;
	TypeBinding binding = typeRef.resolvedType;
	if (binding == null) return INACCURATE_MATCH;
	char[][] superTypeName = QualificationHelpers.seperateFullyQualifedName(this.pattern.superTypeName);
	return resolveLevelForType(superTypeName[QualificationHelpers.SIMPLE_NAMES_INDEX], superTypeName[QualificationHelpers.QULIFIERS_INDEX], binding);
}
public int resolveLevel(Binding binding) {
	if (binding == null) return INACCURATE_MATCH;
	if (!(binding instanceof ReferenceBinding)) return IMPOSSIBLE_MATCH;

	ReferenceBinding type = (ReferenceBinding) binding;
	int level = IMPOSSIBLE_MATCH;
	char[][] superTypeName = QualificationHelpers.seperateFullyQualifedName(this.pattern.superTypeName);
	level = resolveLevelForType(superTypeName[QualificationHelpers.SIMPLE_NAMES_INDEX], superTypeName[QualificationHelpers.QULIFIERS_INDEX], type.getSuperBinding());
	if (level == ACCURATE_MATCH) return ACCURATE_MATCH;
	
	return level;
}
public String toString() {
	return "Locator for " + this.pattern.toString(); //$NON-NLS-1$
}
}
