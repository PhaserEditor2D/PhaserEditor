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
package org.eclipse.wst.jsdt.internal.core.search.matching;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.NameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.core.LocalVariable;

public class LocalVariableLocator extends VariableLocator {

public LocalVariableLocator(VariablePattern pattern) {
	super(pattern);
}
public int match(LocalDeclaration node, MatchingNodeSet nodeSet) {
	int referencesLevel = IMPOSSIBLE_MATCH;
	if (this.pattern.findReferences)
		// must be a write only access with an initializer
		if (this.pattern.writeAccess && !this.pattern.readAccess && node.initialization != null)
			if (matchesName(this.pattern.name, node.name))
				referencesLevel = ((InternalSearchPattern)this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH;

	int declarationsLevel = IMPOSSIBLE_MATCH;
	if (this.pattern.findDeclarations)
		if (matchesName(this.pattern.name, node.name))
			if (node.declarationSourceStart == this.pattern.getVariableStart())
				declarationsLevel = ((InternalSearchPattern)this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH;

	return nodeSet.addMatch(node, referencesLevel >= declarationsLevel ? referencesLevel : declarationsLevel); // use the stronger match
}

//private LocalVariable getLocalVariable() {
//	return ((LocalVariablePattern) this.pattern).localVariable;
//}


protected void matchReportReference(ASTNode reference, IJavaScriptElement element, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
	int offset = -1;
	int length = -1;
	if (reference instanceof SingleNameReference) {
		offset = reference.sourceStart;
		length = reference.sourceEnd-offset+1;
	} else if (reference instanceof QualifiedNameReference) {
		QualifiedNameReference qNameRef = (QualifiedNameReference) reference;
		long sourcePosition = qNameRef.sourcePositions[0];
		offset = (int) (sourcePosition >>> 32);
		length = ((int) sourcePosition) - offset +1;
	} else if (reference instanceof LocalDeclaration) {
		element = this.pattern.getJavaElement();
		if (element instanceof LocalVariable)
		{
	 		LocalVariable localVariable = (LocalVariable)element;
			offset = localVariable.nameStart;
			length = localVariable.nameEnd-offset+1;
		}
		else
		{
			offset=this.pattern.getVariableStart();
			length=this.pattern.getVariableLength();
		}
	}
	if (offset >= 0) {
		match = locator.newLocalVariableReferenceMatch(element, accuracy, offset, length, reference);
		locator.report(match);
	}
}
protected int matchContainer() {
	return METHOD_CONTAINER;
}
protected int matchLocalVariable(LocalVariableBinding variable, boolean matchName) {
	if (variable == null) return INACCURATE_MATCH;

	if (matchName && !matchesName(this.pattern.name, variable.readableName())) return IMPOSSIBLE_MATCH;

	return variable.declaration.declarationSourceStart ==this.pattern.getVariableStart()
		? ACCURATE_MATCH
		: IMPOSSIBLE_MATCH;
}
protected int referenceType() {
	return IJavaScriptElement.LOCAL_VARIABLE;
}
public int resolveLevel(ASTNode possiblelMatchingNode) {
	if (this.pattern.findReferences)
		if (possiblelMatchingNode instanceof NameReference)
			return resolveLevel((NameReference) possiblelMatchingNode);
	if (possiblelMatchingNode instanceof LocalDeclaration)
		return matchLocalVariable(((LocalDeclaration) possiblelMatchingNode).binding, true);
	return IMPOSSIBLE_MATCH;
}
public int resolveLevel(Binding binding) {
	if (binding == null) return INACCURATE_MATCH;
	if (!(binding instanceof LocalVariableBinding)) return IMPOSSIBLE_MATCH;

	return matchLocalVariable((LocalVariableBinding) binding, true);
}
protected int resolveLevel(NameReference nameRef) {
	return resolveLevel(nameRef.binding);
}
}
