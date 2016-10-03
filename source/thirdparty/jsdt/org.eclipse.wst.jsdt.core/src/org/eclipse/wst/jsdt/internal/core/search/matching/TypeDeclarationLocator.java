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
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;

public class TypeDeclarationLocator extends PatternLocator {

protected TypeDeclarationPattern pattern; // can be a QualifiedTypeDeclarationPattern

public TypeDeclarationLocator(TypeDeclarationPattern pattern) {
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
public int match(TypeDeclaration node, MatchingNodeSet nodeSet) {
	if (matchesName(this.pattern.simpleName, node.name))
		return nodeSet.addMatch(node, ((InternalSearchPattern)this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);

	return IMPOSSIBLE_MATCH;
}
//public int match(TypeReference node, MatchingNodeSet nodeSet) - SKIP IT
public int match(InferredType node, MatchingNodeSet nodeSet) {
	char[] typeName = node.getName();
	char[] patternName;
	if (this.pattern.getSearchPrefix() != null) { 
		patternName = this.pattern.getSearchPrefix();
	} else {
		patternName = this.pattern.simpleName;
	}
	if (matchesName(patternName, typeName))
		return nodeSet.addMatch(node, ((InternalSearchPattern)this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
	char [] pkg = this.pattern.qualification;
	if (this.pattern.getSearchPrefix() == null && pkg != null) {
		if (pkg.length>0 &&
				matchesName(CharOperation.concat(pkg, this.pattern.simpleName, '.'), typeName))
			return nodeSet.addMatch(node, ((InternalSearchPattern)this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
	}
	else // any package
	{
		int index=CharOperation.lastIndexOf('.', typeName);
		if (index>=0 &&
					matchesName(patternName, CharOperation.subarray(typeName, index+1,typeName.length)))
			return nodeSet.addMatch(node, ((InternalSearchPattern)this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
		
	}
	return IMPOSSIBLE_MATCH;
}
public int resolveLevel(ASTNode node) {
	Binding binding=null;
	if (node instanceof TypeDeclaration)
		binding=((TypeDeclaration) node).binding;
	else if (node instanceof InferredType)
	{
		InferredType type=(InferredType) node;
		if (!type.isDefinition() || !type.isIndexed())
			return IMPOSSIBLE_MATCH;
		binding=type.binding;
	}
	else
		return IMPOSSIBLE_MATCH;

	return resolveLevel(binding);
}
public int resolveLevel(Binding binding) {
	if (binding == null) return INACCURATE_MATCH;
	if (!(binding instanceof TypeBinding)) return IMPOSSIBLE_MATCH;

	TypeBinding type = (TypeBinding) binding;

	int resolveLevel;
	if (this.pattern.getSearchPrefix() != null) { 
		resolveLevel = resolveLevelUsingSearchPrefix(this.pattern.getSearchPrefix(), type);
	} else {
		resolveLevel = resolveLevelForType(this.pattern.simpleName, this.pattern.qualification, type);
	}
	return resolveLevel;
}
/**
 * Returns whether the given type binding matches the given simple name pattern
 * qualification pattern and enclosing type name pattern.
 */
protected int resolveLevelForType(char[] simpleNamePattern, char[] qualificationPattern, char[] enclosingNamePattern, TypeBinding type) {
	if (enclosingNamePattern == null)
		return resolveLevelForType(simpleNamePattern, qualificationPattern, type);
	if (qualificationPattern == null)
		return resolveLevelForType(simpleNamePattern, enclosingNamePattern, type);

	// case of an import reference while searching for ALL_OCCURENCES of a type (see bug 37166)
	if (type instanceof ProblemReferenceBinding) return IMPOSSIBLE_MATCH;

	// pattern was created from a Java element: qualification is the package name.
	char[] fullQualificationPattern = CharOperation.concat(qualificationPattern, enclosingNamePattern, '.');
	if (CharOperation.equals(this.pattern.qualification, CharOperation.concatWith(type.getPackage().compoundName, '.')))
		return resolveLevelForType(simpleNamePattern, fullQualificationPattern, type);
	return IMPOSSIBLE_MATCH;
}
public String toString() {
	return "Locator for " + this.pattern.toString(); //$NON-NLS-1$
}


public int matchMetadataElement(IJavaScriptElement element) {
	String elementName = element.getElementName();
	char[] typeName = elementName.toCharArray();
	char [] pkg = this.pattern.qualification;
	if (this.pattern.simpleName == null || matchesName(this.pattern.simpleName, typeName))
		return ACCURATE_MATCH;
	if (pkg!=null && pkg.length>0 &&
			matchesName(CharOperation.concat(pkg, this.pattern.simpleName, '.'), typeName))
		return ACCURATE_MATCH;
	return IMPOSSIBLE_MATCH;
}


}
