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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.InferredAttribute;
import org.eclipse.wst.jsdt.core.search.FieldDeclarationMatch;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.NameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Reference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.env.IBinaryType;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleSet;
import org.eclipse.wst.jsdt.internal.core.JavaElement;

public class FieldLocator extends VariableLocator {

protected boolean isDeclarationOfAccessedFieldsPattern;
FieldPattern fieldPattern;


public FieldLocator(FieldPattern pattern) {
	super(pattern);
    this.fieldPattern=pattern;
	this.isDeclarationOfAccessedFieldsPattern = this.pattern instanceof DeclarationOfAccessedFieldsPattern;
}
public int match(ASTNode node, MatchingNodeSet nodeSet) {
	int declarationsLevel = IMPOSSIBLE_MATCH;
	return nodeSet.addMatch(node, declarationsLevel);
}
//public int match(ConstructorDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
public int match(FieldDeclaration node, MatchingNodeSet nodeSet) {
	int referencesLevel = IMPOSSIBLE_MATCH;
	if (this.pattern.findReferences)
		// must be a write only access with an initializer
		if (this.pattern.writeAccess && !this.pattern.readAccess && node.initialization != null)
			if (matchesName(this.pattern.name, node.name))
				referencesLevel = ((InternalSearchPattern)this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH;

	int declarationsLevel = IMPOSSIBLE_MATCH;
	if (this.pattern.findDeclarations) {
		switch (node.getKind()) {
			case AbstractVariableDeclaration.FIELD :
				if (matchesName(this.pattern.name, node.name))
					if (matchesTypeReference(((FieldPattern)this.pattern).typeSimpleName, node.type))
						declarationsLevel = ((InternalSearchPattern)this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH;
				break;
		}
	}
	return nodeSet.addMatch(node, referencesLevel >= declarationsLevel ? referencesLevel : declarationsLevel); // use the stronger match
}

public int match(LocalDeclaration node, MatchingNodeSet nodeSet) {
	if (!this.fieldPattern.isVar)
		return IMPOSSIBLE_MATCH;
	int referencesLevel = IMPOSSIBLE_MATCH;
	if (this.pattern.findReferences)
		// must be a write only access with an initializer
		if (this.pattern.writeAccess && !this.pattern.readAccess && node.initialization != null)
			if (matchesName(this.pattern.name, node.name))
				referencesLevel = ((InternalSearchPattern)this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH;

	int declarationsLevel = IMPOSSIBLE_MATCH;
	if (this.pattern.findDeclarations)
		if (matchesName(this.pattern.name, node.name))
//			if (node.declarationSourceStart == this.pattern.getVariableStart())
				declarationsLevel = ((InternalSearchPattern)this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH;

	return nodeSet.addMatch(node, referencesLevel >= declarationsLevel ? referencesLevel : declarationsLevel); // use the stronger match
}

//public int match(ConstructorDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
public int match(InferredAttribute node, MatchingNodeSet nodeSet) {
	if (this.fieldPattern.isVar)
		return IMPOSSIBLE_MATCH;
	int referencesLevel = IMPOSSIBLE_MATCH;
	if (this.pattern.findReferences)
		// must be a write only access with an initializer
		if (this.pattern.writeAccess && !this.pattern.readAccess /*&& node.initialization != null*/)
			if (matchesName(this.pattern.name, node.name))
				referencesLevel = ((InternalSearchPattern)this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH;

	int declarationsLevel = IMPOSSIBLE_MATCH;
	if (this.pattern.findDeclarations) {
//		switch (node.getKind()) {
//			case AbstractVariableDeclaration.FIELD :
//			case AbstractVariableDeclaration.ENUM_CONSTANT :
				if (matchesName(this.pattern.name, node.name))
//					if (matchesTypeReference(((FieldPattern)this.pattern).typeSimpleName, node.type))
						declarationsLevel = ((InternalSearchPattern)this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH;
//				break;
//		}
	}
	return nodeSet.addMatch(node, referencesLevel >= declarationsLevel ? referencesLevel : declarationsLevel); // use the stronger match
}
//public int match(FunctionDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(MessageSend node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(TypeDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(TypeReference node, MatchingNodeSet nodeSet) - SKIP IT

protected int matchContainer() {
	if (this.pattern.findReferences) {
		// need to look everywhere to find in javadocs and static import
		return ALL_CONTAINER;
	}
	return (this.fieldPattern.isVar)? COMPILATION_UNIT_CONTAINER : CLASS_CONTAINER;
}

protected int matchField(FieldBinding field, boolean matchName) {
	if (field == null) return INACCURATE_MATCH;

	if (matchName && !matchesName(this.pattern.name, field.readableName())) return IMPOSSIBLE_MATCH;

	FieldPattern fieldPattern = (FieldPattern)this.pattern;
	ReferenceBinding receiverBinding = field.declaringClass;
	if (receiverBinding == null) {
		if (field == ArrayBinding.ArrayLength)
			// optimized case for length field of an array
			return fieldPattern.getDeclaringQualification() == null && fieldPattern.getDeclaringSimpleName() == null
				? ACCURATE_MATCH
				: IMPOSSIBLE_MATCH;
		return INACCURATE_MATCH;
	}

	// Note there is no dynamic lookup for field access
	int declaringLevel = resolveLevelForType(fieldPattern.getDeclaringSimpleName(), fieldPattern.getDeclaringQualification(), receiverBinding);
	if (declaringLevel == IMPOSSIBLE_MATCH) return IMPOSSIBLE_MATCH;

	// look at field type only if declaring type is not specified
	if (fieldPattern.getDeclaringSimpleName() == null) return declaringLevel;

	// get real field binding
	FieldBinding fieldBinding = field;

	int typeLevel = resolveLevelForType(fieldBinding.type);
	return declaringLevel > typeLevel ? typeLevel : declaringLevel; // return the weaker match
}

protected int matchLocalVariable(LocalVariableBinding variable, boolean matchName) {
	if (variable == null) return INACCURATE_MATCH;

	if (matchName && !matchesName(this.pattern.name, variable.readableName())) return IMPOSSIBLE_MATCH;

	return ACCURATE_MATCH;
}

/* (non-Javadoc)
 * @see org.eclipse.wst.jsdt.internal.core.search.matching.PatternLocator#matchLevelAndReportImportRef(org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference, org.eclipse.wst.jsdt.internal.compiler.lookup.Binding, org.eclipse.wst.jsdt.internal.core.search.matching.MatchLocator)
 * Accept to report match of static field on static import
 */
protected void matchLevelAndReportImportRef(ImportReference importRef, Binding binding, MatchLocator locator) throws CoreException {
}
protected int matchReference(Reference node, MatchingNodeSet nodeSet, boolean writeOnlyAccess) {
	if (node instanceof FieldReference) {
		if (!this.fieldPattern.isVar && matchesName(this.pattern.name, ((FieldReference) node).token))
				return nodeSet.addMatch(node, ((InternalSearchPattern) this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
		return IMPOSSIBLE_MATCH;
	}
	return super.matchReference(node, nodeSet, writeOnlyAccess);
}
protected void matchReportReference(ASTNode reference, IJavaScriptElement element, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
	if (this.isDeclarationOfAccessedFieldsPattern) {
		// need exact match to be able to open on type ref
		if (accuracy != SearchMatch.A_ACCURATE) return;

		// element that references the field must be included in the enclosing element
		DeclarationOfAccessedFieldsPattern declPattern = (DeclarationOfAccessedFieldsPattern) this.pattern;
		while (element != null && !declPattern.enclosingElement.equals(element))
			element = element.getParent();
		if (element != null) {
			if (reference instanceof FieldReference) {
				reportDeclaration(((FieldReference) reference).binding, locator, declPattern.knownFields);
			} else if (reference instanceof QualifiedNameReference) {
				QualifiedNameReference qNameRef = (QualifiedNameReference) reference;
				Binding nameBinding = qNameRef.binding;
				if (nameBinding instanceof FieldBinding)
					reportDeclaration((FieldBinding)nameBinding, locator, declPattern.knownFields);
				int otherMax = qNameRef.otherBindings == null ? 0 : qNameRef.otherBindings.length;
				for (int i = 0; i < otherMax; i++)
					reportDeclaration(qNameRef.otherBindings[i], locator, declPattern.knownFields);
			} else if (reference instanceof SingleNameReference) {
				reportDeclaration((FieldBinding)((SingleNameReference) reference).binding, locator, declPattern.knownFields);
			}
		}
	} else if (reference instanceof ImportReference) {
		ImportReference importRef = (ImportReference) reference;
		long[] positions = importRef.sourcePositions;
		int lastIndex = importRef.tokens.length - 1;
		int start = (int) ((positions[lastIndex]) >>> 32);
		int end = (int) positions[lastIndex];
		match = locator.newFieldReferenceMatch(element, elementBinding, accuracy, start, end-start+1, importRef);
		locator.report(match);
	} else if (reference instanceof FieldReference) {
		FieldReference fieldReference = (FieldReference) reference;
		long position = fieldReference.nameSourcePosition;
		int start = (int) (position >>> 32);
		int end = (int) position;
		match = locator.newFieldReferenceMatch(element, elementBinding, accuracy, start, end-start+1, fieldReference);
		locator.report(match);
	} else if (reference instanceof SingleNameReference) {
		int offset = reference.sourceStart;
		match = locator.newFieldReferenceMatch(element, elementBinding, accuracy, offset, reference.sourceEnd-offset+1, reference);
		locator.report(match);
	} else if (reference instanceof QualifiedNameReference) {
		QualifiedNameReference qNameRef = (QualifiedNameReference) reference;
		int length = qNameRef.tokens.length;
		SearchMatch[] matches = new SearchMatch[length];
		Binding nameBinding = qNameRef.binding;
		int indexOfFirstFieldBinding = qNameRef.indexOfFirstFieldBinding > 0 ? qNameRef.indexOfFirstFieldBinding-1 : 0;

		// first token
		if (matchesName(this.pattern.name, qNameRef.tokens[indexOfFirstFieldBinding]) && !(nameBinding instanceof LocalVariableBinding)) {
			FieldBinding fieldBinding = nameBinding instanceof FieldBinding ? (FieldBinding) nameBinding : null;
			if (fieldBinding == null) {
				matches[indexOfFirstFieldBinding] = locator.newFieldReferenceMatch(element, elementBinding, accuracy, -1, -1, reference);
			} else {
				switch (matchField(fieldBinding, false)) {
					case ACCURATE_MATCH:
						matches[indexOfFirstFieldBinding] = locator.newFieldReferenceMatch(element, elementBinding, SearchMatch.A_ACCURATE, -1, -1, reference);
						break;
					case INACCURATE_MATCH:
						match = locator.newFieldReferenceMatch(element, elementBinding, SearchMatch.A_INACCURATE, -1, -1, reference);
						matches[indexOfFirstFieldBinding] = match;
						break;
				}
			}
		}

		// other tokens
		for (int i = indexOfFirstFieldBinding+1; i < length; i++) {
			char[] token = qNameRef.tokens[i];
			if (matchesName(this.pattern.name, token)) {
				FieldBinding otherBinding = qNameRef.otherBindings == null ? null : qNameRef.otherBindings[i-(indexOfFirstFieldBinding+1)];
				if (otherBinding == null) {
					matches[i] = locator.newFieldReferenceMatch(element, elementBinding, accuracy, -1, -1, reference);
				} else {
					switch (matchField(otherBinding, false)) {
						case ACCURATE_MATCH:
							matches[i] = locator.newFieldReferenceMatch(element, elementBinding, SearchMatch.A_ACCURATE, -1, -1, reference);
							break;
						case INACCURATE_MATCH:
							match = locator.newFieldReferenceMatch(element, elementBinding, SearchMatch.A_INACCURATE, -1, -1, reference);
							matches[i] = match;
							break;
					}
				}
			}
		}
		locator.reportAccurateFieldReference(matches, qNameRef);
	}
}
/* (non-Javadoc)
 * Overridden to reject unexact matches.
 * @see org.eclipse.wst.jsdt.internal.core.search.matching.PatternLocator#updateMatch(org.eclipse.wst.jsdt.internal.compiler.lookup.ParameterizedTypeBinding, char[][][], org.eclipse.wst.jsdt.internal.core.search.matching.MatchLocator)
 *
 */
protected void updateMatch(char[][][] patternTypeArguments, MatchLocator locator) {
	// We can only refine if locator has an unit scope.
	if (locator.unitScope == null) return;
	if (!match.isExact()) {
		// cannot accept neither erasure nor compatible match
		match.setRule(0);
	}
}
protected void reportDeclaration(FieldBinding fieldBinding, MatchLocator locator, SimpleSet knownFields) throws CoreException {
	// ignore length field
	if (fieldBinding == ArrayBinding.ArrayLength) return;

	ReferenceBinding declaringClass = fieldBinding.declaringClass;
	IType type = locator.lookupType(declaringClass);
	if (type == null) return; // case of a secondary type

	char[] bindingName = fieldBinding.name;
	IField field = type.getField(new String(bindingName));
	if (knownFields.addIfNotIncluded(field)==null) return;

	IResource resource = type.getResource();
	boolean isBinary = type.isBinary();
	IBinaryType info = null;
	if (isBinary) {
		if (resource == null)
			resource = type.getJavaScriptProject().getProject();
		info = locator.getBinaryInfo((org.eclipse.wst.jsdt.internal.core.ClassFile) type.getClassFile(), resource);
		locator.reportBinaryMemberDeclaration(resource, field, fieldBinding, info, SearchMatch.A_ACCURATE);
	} else {
		Scope scp = ((SourceTypeBinding) declaringClass).scope;
		if (scp instanceof ClassScope) {
			ClassScope scope=(ClassScope)scp;
			TypeDeclaration typeDecl = scope.referenceContext;
			if (typeDecl!=null) {
				FieldDeclaration fieldDecl = null;
				FieldDeclaration[] fieldDecls = typeDecl.fields;
				for (int i = 0, length = fieldDecls.length; i < length; i++) {
					if (CharOperation.equals(bindingName, fieldDecls[i].name)) {
						fieldDecl = fieldDecls[i];
						break;
					}
				}
				if (fieldDecl != null) {
					int offset = fieldDecl.sourceStart;
					match = new FieldDeclarationMatch(((JavaElement) field)
							.resolved(fieldBinding), SearchMatch.A_ACCURATE,
							offset, fieldDecl.sourceEnd - offset + 1, locator
									.getParticipant(), resource);
					locator.report(match);
				}
			} else if (scope.inferredType!=null)
			{
				InferredAttribute attribute=null;
			  for (int attributeInx=0; attributeInx<scope.inferredType.numberAttributes; attributeInx++) {
					InferredAttribute element = scope.inferredType.attributes[attributeInx];
					if (CharOperation.equals(bindingName, element.name)) {
						attribute =element;
						break;
					}

				}
				if (attribute != null) {
					int offset = attribute.sourceStart;
					match = new FieldDeclarationMatch(((JavaElement) field)
							.resolved(fieldBinding), SearchMatch.A_ACCURATE,
							offset, attribute.sourceEnd - offset + 1, locator
									.getParticipant(), resource);
					locator.report(match);
				}

			}

		}
		else if (scp !=null)
			//TODO: could be compilation unit scope
			throw new org.eclipse.wst.jsdt.core.UnimplementedException();
	}
}
protected int referenceType() {
	return IJavaScriptElement.FIELD;
}
public int resolveLevel(ASTNode possiblelMatchingNode) {
	if (this.pattern.findReferences) {
		if (possiblelMatchingNode instanceof FieldReference)
			if (!this.fieldPattern.isVar)
				return matchField(((FieldReference) possiblelMatchingNode).binding, true);
			else return IMPOSSIBLE_MATCH;
		else if (possiblelMatchingNode instanceof NameReference)
			return resolveLevel((NameReference) possiblelMatchingNode);
	}
	if (possiblelMatchingNode instanceof FieldDeclaration)
		return matchField(((FieldDeclaration) possiblelMatchingNode).binding, true);
	else if (possiblelMatchingNode instanceof LocalDeclaration)
		return matchLocalVariable(((LocalDeclaration) possiblelMatchingNode).binding, true);
	else if (possiblelMatchingNode instanceof InferredAttribute)
		return matchField(((InferredAttribute) possiblelMatchingNode).binding, true);

	return IMPOSSIBLE_MATCH;
}
public int resolveLevel(Binding binding) {
	if (binding == null) return INACCURATE_MATCH;
	if (fieldPattern.isVar)
	{
		if (!(binding instanceof LocalVariableBinding)) return IMPOSSIBLE_MATCH;
		LocalVariableBinding localVariableBinding=(LocalVariableBinding) binding;
		if (localVariableBinding.declaringScope.kind!=Scope.COMPILATION_UNIT_SCOPE)
			return IMPOSSIBLE_MATCH;
		return matchLocalVariable((LocalVariableBinding) binding, true);
	}
	else
	{
		if (!(binding instanceof FieldBinding)) return IMPOSSIBLE_MATCH;

		return matchField((FieldBinding) binding, true);

	}
}
protected int resolveLevel(NameReference nameRef) {
	if (nameRef instanceof SingleNameReference)
		return resolveLevel(nameRef.binding);

	Binding binding = nameRef.binding;
	QualifiedNameReference qNameRef = (QualifiedNameReference) nameRef;
	FieldBinding fieldBinding = null;
	if (binding instanceof FieldBinding) {
		fieldBinding = (FieldBinding) binding;
		char[] bindingName = fieldBinding.name;
		int lastDot = CharOperation.lastIndexOf('.', bindingName);
		if (lastDot > -1)
			bindingName = CharOperation.subarray(bindingName, lastDot+1, bindingName.length);
		if (matchesName(this.pattern.name, bindingName)) {
			int level = matchField(fieldBinding, false);
			if (level != IMPOSSIBLE_MATCH) return level;
		}
	}
	int otherMax = qNameRef.otherBindings == null ? 0 : qNameRef.otherBindings.length;
	for (int i = 0; i < otherMax; i++) {
		char[] token = qNameRef.tokens[i + qNameRef.indexOfFirstFieldBinding];
		if (matchesName(this.pattern.name, token)) {
			FieldBinding otherBinding = qNameRef.otherBindings[i];
			int level = matchField(otherBinding, false);
			if (level != IMPOSSIBLE_MATCH) return level;
		}
	}
	return IMPOSSIBLE_MATCH;
}
/* (non-Javadoc)
 * Resolve level for type with a given binding.
 */
protected int resolveLevelForType(TypeBinding typeBinding) {
	FieldPattern fieldPattern = (FieldPattern) this.pattern;
	TypeBinding fieldTypeBinding = typeBinding;
	return resolveLevelForType(
			fieldPattern.typeSimpleName,
			fieldPattern.typeQualification,
			fieldPattern.getTypeArguments(),
			0,
			fieldTypeBinding);
}

public int matchLocalDeclaration(LocalDeclaration node, MatchingNodeSet nodeSet) {
	int referencesLevel = IMPOSSIBLE_MATCH;
	if (this.pattern.findReferences)
		// must be a write only access with an initializer
		if (this.pattern.writeAccess && !this.pattern.readAccess && node.initialization != null)
			if (matchesName(this.pattern.name, node.name))
				referencesLevel = ((InternalSearchPattern)this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH;

	int declarationsLevel = IMPOSSIBLE_MATCH;
	if (this.pattern.findDeclarations)
		if (matchesName(this.pattern.name, node.name))
				declarationsLevel = ((InternalSearchPattern)this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH;

	return nodeSet.addMatch(node, referencesLevel >= declarationsLevel ? referencesLevel : declarationsLevel); // use the stronger match
}
}
