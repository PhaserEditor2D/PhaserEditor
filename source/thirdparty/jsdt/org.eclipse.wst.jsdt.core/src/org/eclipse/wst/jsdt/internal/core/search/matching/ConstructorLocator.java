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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.InferredMethod;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Argument;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocMessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.util.Util;

public class ConstructorLocator extends PatternLocator {

	protected ConstructorPattern pattern;

	public ConstructorLocator(ConstructorPattern pattern) {
		super(pattern);

		this.pattern = pattern;
	}

	public int match(ASTNode node, MatchingNodeSet nodeSet) { // interested in
		// ExplicitConstructorCall
		if (!this.pattern.findReferences)
			return IMPOSSIBLE_MATCH;
		if (!(node instanceof ExplicitConstructorCall))
			return IMPOSSIBLE_MATCH;

		if (!matchParametersCount(node, ((ExplicitConstructorCall) node).arguments))
			return IMPOSSIBLE_MATCH;

		return nodeSet.addMatch(node, ((InternalSearchPattern) this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
	}

	public int match(ConstructorDeclaration node, MatchingNodeSet nodeSet) {
		int referencesLevel = this.pattern.findReferences ? matchLevelForReferences(node) : IMPOSSIBLE_MATCH;
		int declarationsLevel = this.pattern.findDeclarations ? matchLevelForDeclarations(node) : IMPOSSIBLE_MATCH;

		return nodeSet.addMatch(node, referencesLevel >= declarationsLevel ? referencesLevel : declarationsLevel); // use
		// the stronger match
	}

	public int match(Expression node, MatchingNodeSet nodeSet) { // interested
		// in AllocationExpression
		if (!this.pattern.findReferences)
			return IMPOSSIBLE_MATCH;
		if (!(node instanceof AllocationExpression))
			return IMPOSSIBLE_MATCH;

		// constructor name is simple type name
		AllocationExpression allocation = (AllocationExpression) node;
		char[] typeName = getTypeName(allocation);
		if (typeName == null)
			return IMPOSSIBLE_MATCH;

		if (matchesTypeName(typeName))
				return nodeSet.addMatch(node, ((InternalSearchPattern)this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
		
		return IMPOSSIBLE_MATCH;
	}

	private char[] getTypeName(AllocationExpression allocation) {
		char[] typeName = null;

		if (allocation.member instanceof SingleNameReference)
			typeName = ((SingleNameReference) allocation.member).token;
		else if (allocation.member instanceof FieldReference)
			typeName = Util.getTypeName(allocation.member);
		return typeName;
	}

	private boolean matchesTypeName(char[] typeName) {
		char[] patternName;
		if (this.pattern.getSearchPrefix() != null) { 
			patternName = this.pattern.getSearchPrefix();
		} else {
			patternName = this.pattern.declaringSimpleName;
		}
		if (matchesName(patternName, typeName))
			return true;
		char [] pkg = this.pattern.declaringQualification;
		if (this.pattern.getSearchPrefix() == null && pkg != null) {
			if (pkg.length>0 &&
					matchesName(CharOperation.concat(pkg, this.pattern.declaringSimpleName, '.'), typeName))
				return true;
		} else {
			int index=CharOperation.lastIndexOf('.', typeName);
			if (index>=0 &&
						matchesName(patternName, CharOperation.subarray(typeName, index+1,typeName.length)))
				return true;
		}
		return false;
	}

	public int match(FieldDeclaration field, MatchingNodeSet nodeSet) {
		if (!this.pattern.findReferences)
			return IMPOSSIBLE_MATCH;
		// look only for enum constant
		if (field.type != null || !(field.initialization instanceof AllocationExpression))
			return IMPOSSIBLE_MATCH;

		// AllocationExpression allocation = (AllocationExpression)
		// field.initialization;
		if (field.binding != null && field.binding.declaringClass != null) {
			if (this.pattern.declaringSimpleName != null && !matchesName(this.pattern.declaringSimpleName, field.binding.declaringClass.sourceName()))
				return IMPOSSIBLE_MATCH;
		}

		return nodeSet.addMatch(field, ((InternalSearchPattern) this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
	}

	/**
	 * Special case for message send in javadoc comment. They can be in fact
	 * bound to a contructor.
	 * 
	 * @see "http://bugs.eclipse.org/bugs/show_bug.cgi?id=83285"
	 */
	public int match(MessageSend msgSend, MatchingNodeSet nodeSet) {
		if ((msgSend.bits & ASTNode.InsideJavadoc) == 0)
			return IMPOSSIBLE_MATCH;
		if (this.pattern.declaringSimpleName == null || CharOperation.equals(msgSend.selector, this.pattern.declaringSimpleName)) {
			return nodeSet.addMatch(msgSend, ((InternalSearchPattern) this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
		}
		return IMPOSSIBLE_MATCH;
	}

	public int match(TypeDeclaration node, MatchingNodeSet nodeSet) {
		if (!this.pattern.findReferences)
			return IMPOSSIBLE_MATCH;

		// need to look for a generated default constructor
		return nodeSet.addMatch(node, ((InternalSearchPattern) this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.core.search.matching.PatternLocator#match(org.eclipse.wst.jsdt.core.infer.InferredMethod, org.eclipse.wst.jsdt.internal.core.search.matching.MatchingNodeSet)
	 */
	public int match(InferredMethod inferredMethod, MatchingNodeSet nodeSet) {
		if (!this.pattern.findDeclarations) return IMPOSSIBLE_MATCH;
		
		//must be a constructor
		if(!inferredMethod.isConstructor) return IMPOSSIBLE_MATCH;

		if (!matchesTypeName(inferredMethod.name))
			return IMPOSSIBLE_MATCH;

		// Verify parameters types
		boolean resolve = false;
		if (this.pattern.parameterSimpleNames != null) {
			int length = this.pattern.parameterSimpleNames.length;
			ASTNode[] args = ((AbstractMethodDeclaration)inferredMethod.getFunctionDeclaration()).arguments;
			int argsLength = args == null ? 0 : args.length;
			if (length != argsLength) return IMPOSSIBLE_MATCH;
			for (int i = 0; i < argsLength; i++) {
				if (args != null && !matchesTypeReference(this.pattern.parameterSimpleNames[i], ((Argument) args[i]).type)) {
					// Do not return as impossible when source level is at least 1.5
					if (this.mayBeGeneric) {
						if (!((InternalSearchPattern)this.pattern).mustResolve) {
							// Set resolution flag on node set in case of types was inferred in parameterized types from generic ones...
						 	// (see  bugs https://bugs.eclipse.org/bugs/show_bug.cgi?id=79990, 96761, 96763)
							nodeSet.mustResolve = true;
							resolve = true;
						}
					} else {
						return IMPOSSIBLE_MATCH;
					}
				}
			}
		}

		// Method declaration may match pattern
		return nodeSet.addMatch(inferredMethod , resolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
	}

	protected int matchConstructor(MethodBinding constructor) {
		if (!constructor.isConstructor())
			return IMPOSSIBLE_MATCH;

		// declaring type, simple name has already been matched by
		// matchIndexEntry()
		int level;
		if (this.pattern.getSearchPrefix() != null) { 
			level = resolveLevelUsingSearchPrefix(this.pattern.getSearchPrefix(), constructor.declaringClass);
		} else {
			level = resolveLevelForType(this.pattern.declaringSimpleName, this.pattern.declaringQualification, constructor.declaringClass);
		}
		if (level == IMPOSSIBLE_MATCH)
			return IMPOSSIBLE_MATCH;

		// parameter types
		int parameterCount = this.pattern.parameterNames != null ? this.pattern.parameterNames.length : 0;
		if (parameterCount > -1) {
			if (constructor.parameters == null)
				return INACCURATE_MATCH;
			if (parameterCount != constructor.parameters.length)
				return IMPOSSIBLE_MATCH;
			for (int i = 0; i < parameterCount; i++) {
				// TODO (frederic) use this call to refine accuracy on
				int newLevel = resolveLevelForType(this.pattern.parameterSimpleNames[i], this.pattern.parameterQualifications[i], constructor.parameters[i]);
				if (level > newLevel) {
					if (newLevel == IMPOSSIBLE_MATCH) {
						return IMPOSSIBLE_MATCH;
					}
					level = newLevel; // can only be downgraded
				}
			}
		}
		return level;
	}

	protected int matchContainer() {
		if (this.pattern.findReferences)
			return ALL_CONTAINER; // handles both declarations + references &
		// just references
		// COMPILATION_UNIT_CONTAINER - implicit constructor call: case of Y
		// extends X and Y doesn't define any constructor
		// CLASS_CONTAINER - implicit constructor call: case of constructor
		// declaration with no explicit super call
		// METHOD_CONTAINER - reference in another constructor
		// FIELD_CONTAINER - anonymous in a field initializer

		// declarations are only found in Class
		return CLASS_CONTAINER;
	}

	protected int matchLevelForReferences(ConstructorDeclaration constructor) {
		ExplicitConstructorCall constructorCall = constructor.constructorCall;
		if (constructorCall == null || constructorCall.accessMode != ExplicitConstructorCall.ImplicitSuper)
			return IMPOSSIBLE_MATCH;

		if (this.pattern.parameterSimpleNames != null) {
			int length = this.pattern.parameterSimpleNames.length;
			Expression[] args = constructorCall.arguments;
			int argsLength = args == null ? 0 : args.length;
			if (length != argsLength)
				return IMPOSSIBLE_MATCH;
		}
		return ((InternalSearchPattern) this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH;
	}

	protected int matchLevelForDeclarations(ConstructorDeclaration constructor) {
		// constructor name is stored in selector field
		if (this.pattern.declaringSimpleName != null && !matchesName(this.pattern.declaringSimpleName, constructor.getName()))
			return IMPOSSIBLE_MATCH;

		if (this.pattern.parameterSimpleNames != null) {
			int length = this.pattern.parameterSimpleNames.length;
			Argument[] args = constructor.arguments;
			int argsLength = args == null ? 0 : args.length;
			if (length != argsLength)
				return IMPOSSIBLE_MATCH;
		}

		return ((InternalSearchPattern) this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH;
	}

	boolean matchParametersCount(ASTNode node, Expression[] args) {
		if (this.pattern.parameterSimpleNames != null && ((node.bits & ASTNode.InsideJavadoc) != 0)) {
			int length = this.pattern.parameterNames != null ? this.pattern.parameterNames.length : 0;
			if (length < 0)
				length = this.pattern.parameterSimpleNames.length;
			int argsLength = args == null ? 0 : args.length;
			if (length != argsLength) {
				return false;
			}
		}
		return true;
	}

	protected void matchReportReference(ASTNode reference, IJavaScriptElement element, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
		if (reference instanceof TypeDeclaration || reference instanceof FieldDeclaration) {
			super.matchReportReference(reference, element, elementBinding, accuracy, locator);
			if (match != null)
				return;
		}

		// Create search match
		match = locator.newMethodReferenceMatch(element, elementBinding, accuracy, -1, -1, true, reference);

		// See whether it is necessary to report or not
		if (match.getRule() == 0)
			return; // impossible match
		boolean report = (this.isErasureMatch && match.isErasure()) || (this.isEquivalentMatch && match.isEquivalent()) || match.isExact();
		if (!report)
			return;

		// Report match
		int offset = reference.sourceStart;
		match.setOffset(offset);
		match.setLength(reference.sourceEnd - offset + 1);
		if (reference instanceof FieldDeclaration) { // enum declaration
			FieldDeclaration enumConstant = (FieldDeclaration) reference;
			if (enumConstant.initialization instanceof QualifiedAllocationExpression) {
				locator.reportAccurateEnumConstructorReference(match, enumConstant, (QualifiedAllocationExpression) enumConstant.initialization);
				return;
			}
		}
		locator.report(match);
	}

	public SearchMatch newDeclarationMatch(ASTNode reference, IJavaScriptElement element, Binding binding, int accuracy, int length, MatchLocator locator) {
		match = null;
		int offset = reference.sourceStart;
		if (this.pattern.findReferences) {
			if (reference instanceof TypeDeclaration) {
				TypeDeclaration type = (TypeDeclaration) reference;
				AbstractMethodDeclaration[] methods = type.methods;
				if (methods != null) {
					for (int i = 0, max = methods.length; i < max; i++) {
						AbstractMethodDeclaration method = methods[i];
						match = locator.newMethodReferenceMatch(element, binding, accuracy, offset, length, method.isConstructor(), method);
					}
				}
			}
			else if (reference instanceof ConstructorDeclaration) {
				ConstructorDeclaration constructor = (ConstructorDeclaration) reference;
				match = locator.newMethodReferenceMatch(element, binding, accuracy, offset, length, constructor.isConstructor(), constructor);
			}
		}
		if (match != null) {
			return match;
		}
		// super implementation...
		return locator.newDeclarationMatch(element, binding, accuracy, reference.sourceStart, length);
	}

	public int resolveLevel(ASTNode node) {
		if (this.pattern.findReferences) {
			if (node instanceof AllocationExpression)
				return resolveLevel((AllocationExpression) node);
			if (node instanceof ExplicitConstructorCall)
				return resolveLevel(((ExplicitConstructorCall) node).binding);
			if (node instanceof TypeDeclaration)
				return resolveLevel((TypeDeclaration) node);
			if (node instanceof FieldDeclaration)
				return resolveLevel((FieldDeclaration) node);
			if (node instanceof JavadocMessageSend) {
				return resolveLevel(((JavadocMessageSend) node).binding);
			}
		}
		if (node instanceof ConstructorDeclaration)
			return resolveLevel((ConstructorDeclaration) node, true);
		return IMPOSSIBLE_MATCH;
	}

	protected int referenceType() {
		return IJavaScriptElement.METHOD;
	}

	protected int resolveLevel(AllocationExpression allocation) {
		
		return resolveLevel(allocation.binding);
	}

	protected int resolveLevel(FieldDeclaration field) {
		// only accept enum constants
		if (field.type != null || field.binding == null)
			return IMPOSSIBLE_MATCH;
		if (this.pattern.declaringSimpleName != null && !matchesName(this.pattern.declaringSimpleName, field.binding.type.sourceName()))
			return IMPOSSIBLE_MATCH;
		if (!(field.initialization instanceof AllocationExpression) || field.initialization.resolvedType.isLocalType())
			return IMPOSSIBLE_MATCH;

		return resolveLevel(((AllocationExpression) field.initialization).binding);
	}

	public int resolveLevel(Binding binding) {
		if (binding == null)
			return INACCURATE_MATCH;
		if (!(binding instanceof MethodBinding))
			return IMPOSSIBLE_MATCH;

		MethodBinding constructor = (MethodBinding) binding;
		int level = matchConstructor(constructor);
		if (level == IMPOSSIBLE_MATCH) {
			if (constructor != constructor.original()) {
				level = matchConstructor(constructor.original());
			}
		}
		return level;
	}

	protected int resolveLevel(ConstructorDeclaration constructor, boolean checkDeclarations) {
		int referencesLevel = IMPOSSIBLE_MATCH;
		if (this.pattern.findReferences) {
			ExplicitConstructorCall constructorCall = constructor.constructorCall;
			if (constructorCall != null && constructorCall.accessMode == ExplicitConstructorCall.ImplicitSuper) {
				// eliminate explicit super call as it will be treated with
				int callCount = (constructorCall.arguments == null) ? 0 : constructorCall.arguments.length;
				int patternCount = (this.pattern.parameterSimpleNames == null) ? 0 : this.pattern.parameterSimpleNames.length;
				if (patternCount != callCount) {
					referencesLevel = IMPOSSIBLE_MATCH;
				}
				else {
					referencesLevel = resolveLevel(constructorCall.binding);
					if (referencesLevel == ACCURATE_MATCH)
						return ACCURATE_MATCH; // cannot get better
				}
			}
		}
		if (!checkDeclarations)
			return referencesLevel;

		int declarationsLevel = this.pattern.findDeclarations ? resolveLevel(constructor.getBinding()) : IMPOSSIBLE_MATCH;
		return referencesLevel >= declarationsLevel ? referencesLevel : declarationsLevel; // answer
		// the stronger match
	}

	protected int resolveLevel(TypeDeclaration type) {
		// find default constructor
		AbstractMethodDeclaration[] methods = type.methods;
		if (methods != null) {
			for (int i = 0, length = methods.length; i < length; i++) {
				AbstractMethodDeclaration method = methods[i];
				if (method.isDefaultConstructor() && method.sourceStart < type.bodyStart) // if
					// synthetic
					return resolveLevel((ConstructorDeclaration) method, false);
			}
		}
		return IMPOSSIBLE_MATCH;
	}

	public String toString() {
		return "Locator for " + this.pattern.toString(); //$NON-NLS-1$
	}
}
