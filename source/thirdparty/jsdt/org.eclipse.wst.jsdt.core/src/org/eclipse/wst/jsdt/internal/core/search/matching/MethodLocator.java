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

import java.util.HashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.InferredMethod;
import org.eclipse.wst.jsdt.core.search.MethodDeclarationMatch;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Argument;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.env.IBinaryType;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleSet;
import org.eclipse.wst.jsdt.internal.core.JavaElement;
import org.eclipse.wst.jsdt.internal.core.search.BasicSearchEngine;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IIndexConstants;
import org.eclipse.wst.jsdt.internal.core.util.QualificationHelpers;

public class MethodLocator extends PatternLocator {

protected MethodPattern pattern;
protected boolean isDeclarationOfReferencedMethodsPattern;

//extra reference info
public char[][][] allSuperDeclaringTypeNames;

//method declarations which parameters verification fail
private HashMap methodDeclarationsWithInvalidParam = new HashMap();

public MethodLocator(MethodPattern pattern) {
	super(pattern);

	this.pattern = pattern;
	this.isDeclarationOfReferencedMethodsPattern = this.pattern instanceof DeclarationOfReferencedMethodsPattern;
}
/*
 * Clear caches
 */
protected void clear() {
	this.methodDeclarationsWithInvalidParam = new HashMap();
}
public void initializePolymorphicSearch(MatchLocator locator) {
	long start = 0;
	if (BasicSearchEngine.VERBOSE) {
		start = System.currentTimeMillis();
	}
	try {
		this.allSuperDeclaringTypeNames =
			new SuperTypeNamesCollector(
				this.pattern,
				this.pattern.getDeclaringSimpleName(),
				this.pattern.getDeclaringQualification(),
				locator,
				null,
				locator.progressMonitor).collect();
	} catch (JavaScriptModelException e) {
		// inaccurate matches will be found
	}
	if (BasicSearchEngine.VERBOSE) {
		System.out.println("Time to initialize polymorphic search: "+(System.currentTimeMillis()-start)); //$NON-NLS-1$
	}
}
/*
 * Return whether a type name is in pattern all super declaring types names.
 */
private boolean isTypeInSuperDeclaringTypeNames(char[][] typeName) {
	if (allSuperDeclaringTypeNames == null) return false;
	int length = allSuperDeclaringTypeNames.length;
	for (int i= 0; i<length; i++) {
		if (CharOperation.equals(allSuperDeclaringTypeNames[i], typeName)) {
			return true;
		}
	}
	return false;
}
/**
 * Returns whether the code gen will use an invoke virtual for
 * this message send or not.
 */
protected boolean isVirtualInvoke(MethodBinding method, MessageSend messageSend) {
	return !method.isStatic() && !method.isPrivate() && !messageSend.isSuperAccess();
}
public int match(ASTNode node, MatchingNodeSet nodeSet) {
	int declarationsLevel = IMPOSSIBLE_MATCH;
	return nodeSet.addMatch(node, declarationsLevel);
}
//public int match(ConstructorDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(Expression node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(FieldDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
public int match(MethodDeclaration node, MatchingNodeSet nodeSet) {
	if (!this.pattern.findDeclarations) return IMPOSSIBLE_MATCH;
	// Matches are already attempted on InferredMethods
	if (node.isInferred()) return IMPOSSIBLE_MATCH;
	// Verify method name
	if (!matchesName(this.pattern.selector, node.getName())) return IMPOSSIBLE_MATCH;

	// Verify parameters types
	boolean resolve = ((InternalSearchPattern)this.pattern).mustResolve;
	if (this.pattern.parameterSimpleNames != null) {
		int length = this.pattern.parameterSimpleNames.length;
		ASTNode[] args = node.arguments;
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
					this.methodDeclarationsWithInvalidParam.put(node, null);
				} else {
					return IMPOSSIBLE_MATCH;
				}
			}
		}
	}

	// Method declaration may match pattern
	return nodeSet.addMatch(node, resolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
}
public int match(MessageSend node, MatchingNodeSet nodeSet) {
	if (!this.pattern.findReferences) return IMPOSSIBLE_MATCH;

	if (!matchesName(this.pattern.selector, node.selector)) return IMPOSSIBLE_MATCH;
	if (this.pattern.parameterSimpleNames != null && ((node.bits & ASTNode.InsideJavadoc) != 0)) {
		int length = this.pattern.parameterSimpleNames.length;
		ASTNode[] args = node.arguments;
		int argsLength = args == null ? 0 : args.length;
		if (length != argsLength) return IMPOSSIBLE_MATCH;
	}

	return nodeSet.addMatch(node, ((InternalSearchPattern)this.pattern).mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
}
//public int match(TypeDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(TypeReference node, MatchingNodeSet nodeSet) - SKIP IT

protected int matchContainer() {
	if (this.pattern.findReferences) {
		// need to look almost everywhere to find in javadocs and static import
		return ALL_CONTAINER;
	}
	return pattern.isFunction ? COMPILATION_UNIT_CONTAINER : CLASS_CONTAINER;
}
/* (non-Javadoc)
 * @see org.eclipse.wst.jsdt.internal.core.search.matching.PatternLocator#matchLevelAndReportImportRef(org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference, org.eclipse.wst.jsdt.internal.compiler.lookup.Binding, org.eclipse.wst.jsdt.internal.core.search.matching.MatchLocator)
 * Accept to report match of static field on static import
 */
protected void matchLevelAndReportImportRef(ImportReference importRef, Binding binding, MatchLocator locator) throws CoreException {
}
protected int matchMethod(MethodBinding method, boolean skipImpossibleArg) {
	if (!matchesName(this.pattern.selector, method.selector)) return IMPOSSIBLE_MATCH;

	int level = ACCURATE_MATCH;
	
	// look at return type only if declaring type is not specified
	if (this.pattern.getDeclaringSimpleName() == null) {
		// TODO (frederic) use this call to refine accuracy on return type
		// int newLevel = resolveLevelForType(this.pattern.returnSimpleName, this.pattern.returnQualification, this.pattern.returnTypeArguments, 0, method.returnType);
		int newLevel = resolveLevelForType(this.pattern.returnSimpleName, this.pattern.returnQualification, method.returnType);
		if (level > newLevel) {
			if (newLevel == IMPOSSIBLE_MATCH) return IMPOSSIBLE_MATCH;
			level = newLevel; // can only be downgraded
		}
	} else if (!CharOperation.equals(this.pattern.getDeclaringSimpleName(), IIndexConstants.GLOBAL_SYMBOL, false)) {
		level = resolveLevelForType(this.pattern.getDeclaringSimpleName(), this.pattern.getDeclaringQualification(), method.declaringClass);
	}

	return level;
}
private boolean matchOverriddenMethod(ReferenceBinding type, MethodBinding method, MethodBinding matchMethod) {
	if (type == null || this.pattern.selector == null) return false;

	// matches superclass
	if (!CharOperation.equals(type.compoundName, TypeConstants.JAVA_LANG_OBJECT)) {
		ReferenceBinding superClass = type.getSuperBinding();
		if (matchOverriddenMethod(superClass, method, matchMethod)) {
			return true;
		}
	}

	return false;
}
/**
 * @see org.eclipse.wst.jsdt.internal.core.search.matching.PatternLocator#matchReportReference(org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode, org.eclipse.wst.jsdt.core.IJavaScriptElement, Binding, int, org.eclipse.wst.jsdt.internal.core.search.matching.MatchLocator)
 */
protected void matchReportReference(ASTNode reference, IJavaScriptElement element, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
	MethodBinding methodBinding = (reference instanceof MessageSend) ? ((MessageSend)reference).binding: ((elementBinding instanceof MethodBinding) ? (MethodBinding) elementBinding : null);
	if (this.isDeclarationOfReferencedMethodsPattern) {
		if (methodBinding == null) return;
		// need exact match to be able to open on type ref
		if (accuracy != SearchMatch.A_ACCURATE) return;

		// element that references the method must be included in the enclosing element
		DeclarationOfReferencedMethodsPattern declPattern = (DeclarationOfReferencedMethodsPattern) this.pattern;
		while (element != null && !declPattern.enclosingElement.equals(element))
			element = element.getParent();
		if (element != null) {
			reportDeclaration(methodBinding, locator, declPattern.knownMethods);
		}
	} else {
		match = locator.newMethodReferenceMatch(element, elementBinding, accuracy, -1, -1, false /*not constructor*/, reference);
		if (this.pattern.findReferences && reference instanceof MessageSend) {
			IJavaScriptElement focus = ((InternalSearchPattern) this.pattern).focus;
			// verify closest match if pattern was bound
			// (see bug 70827)
			if (focus != null && focus.getElementType() == IJavaScriptElement.METHOD) {
				if (methodBinding != null) {
					boolean isPrivate = Flags.isPrivate(((IFunction) focus).getFlags());
					if (isPrivate && !CharOperation.equals(methodBinding.declaringClass.sourceName, focus.getParent().getElementName().toCharArray())) {
						return; // finally the match was not possible
					}
				}
			}
			matchReportReference((MessageSend)reference, locator, ((MessageSend)reference).binding);
		} else {
			int offset = reference.sourceStart;
			int length =  reference.sourceEnd - offset + 1;
			match.setOffset(offset);
			match.setLength(length);
			locator.report(match);
		}
	}
}
void matchReportReference(MessageSend messageSend, MatchLocator locator, MethodBinding methodBinding) throws CoreException {
	// See whether it is necessary to report or not
	if (match.getRule() == 0) return; // impossible match
	boolean report = (this.isErasureMatch && match.isErasure()) || (this.isEquivalentMatch && match.isEquivalent()) || match.isExact();
	if (!report) return;

	// Report match
	int offset = (int) (messageSend.nameSourcePosition >>> 32);
	match.setOffset(offset);
	match.setLength(messageSend.sourceEnd - offset + 1);
	locator.report(match);
}

public SearchMatch newDeclarationMatch(ASTNode reference, IJavaScriptElement element, Binding elementBinding, int accuracy, int length, MatchLocator locator) {
	if (elementBinding != null) {
		MethodBinding methodBinding = (MethodBinding) elementBinding;
		// If method parameters verification was not valid, then try to see if method arguments can match a method in hierarchy
		if (this.methodDeclarationsWithInvalidParam.containsKey(reference)) {
			// First see if this reference has already been resolved => report match if validated
			Boolean report = (Boolean) this.methodDeclarationsWithInvalidParam.get(reference);
			if (report != null) {
				if (report.booleanValue()) {
					return super.newDeclarationMatch(reference, element, elementBinding, accuracy, length, locator);
				}
				return null;
			}
			if (matchOverriddenMethod(methodBinding.declaringClass, methodBinding, null)) {
				this.methodDeclarationsWithInvalidParam.put(reference, Boolean.TRUE);
				return super.newDeclarationMatch(reference, element, elementBinding, accuracy, length, locator);
			}
			if (isTypeInSuperDeclaringTypeNames(methodBinding.declaringClass.compoundName)) {
				MethodBinding patternBinding = locator.getMethodBinding(this.pattern);
				if (patternBinding != null) {
					if (!matchOverriddenMethod(patternBinding.declaringClass, patternBinding, methodBinding)) {
						this.methodDeclarationsWithInvalidParam.put(reference, Boolean.FALSE);
						return null;
					}
				}
				this.methodDeclarationsWithInvalidParam.put(reference, Boolean.TRUE);
				return super.newDeclarationMatch(reference, element, elementBinding, accuracy, length, locator);
			}
			this.methodDeclarationsWithInvalidParam.put(reference, Boolean.FALSE);
			return null;
		}
	}
	return super.newDeclarationMatch(reference, element, elementBinding, accuracy, length, locator);
}
protected int referenceType() {
	return IJavaScriptElement.METHOD;
}
protected void reportDeclaration(MethodBinding methodBinding, MatchLocator locator, SimpleSet knownMethods) throws CoreException {
	ReferenceBinding declaringClass = methodBinding.declaringClass;
	IType type = locator.lookupType(declaringClass);
	if (type == null) return; // case of a secondary type

	char[] bindingSelector = methodBinding.selector;
	boolean isBinary = type.isBinary();
	IFunction method = null;
	TypeBinding[] parameters = methodBinding.original().parameters;
	int parameterLength = parameters.length;
//	if (isBinary) {
//		char[][] parameterTypes = new char[parameterLength][];
//		for (int i = 0; i<parameterLength; i++) {
//			char[] typeName = parameters[i].qualifiedSourceName();
//			for (int j=0, dim=parameters[i].dimensions(); j<dim; j++) {
//				typeName = CharOperation.concat(typeName, new char[] {'[', ']'});
//			}
//			parameterTypes[i] = typeName;
//		}
//		method = locator.createBinaryMethodHandle(type, methodBinding.selector, parameterTypes);
//	} else {
		String[] parameterTypes = new String[parameterLength];
		for (int i = 0; i  < parameterLength; i++) {
			char[] typeName = parameters[i].shortReadableName();
			if (parameters[i].isMemberType()) {
				typeName = CharOperation.subarray(typeName, CharOperation.indexOf('.', typeName)+1, typeName.length);
			}
			parameterTypes[i] = Signature.createTypeSignature(typeName, false);
		}
		method = type.getFunction(new String(bindingSelector), parameterTypes);
//	}
	if (method == null || knownMethods.addIfNotIncluded(method) == null) return;

	IResource resource = type.getResource();
	IBinaryType info = null;
	if (isBinary) {
		if (resource == null)
			resource = type.getJavaScriptProject().getProject();
		info = locator.getBinaryInfo((org.eclipse.wst.jsdt.internal.core.ClassFile)type.getClassFile(), resource);
		locator.reportBinaryMemberDeclaration(resource, method, methodBinding, info, SearchMatch.A_ACCURATE);
	} else {
		ClassScope scope = (ClassScope)((SourceTypeBinding) declaringClass).scope;
		if (scope != null) {
			TypeDeclaration typeDecl = scope.referenceContext;
			AbstractMethodDeclaration methodDecl = null;
			AbstractMethodDeclaration[] methodDecls = typeDecl.methods;
			for (int i = 0, length = methodDecls.length; i < length; i++) {
				if (CharOperation.equals(bindingSelector, methodDecls[i].getName())) {
					methodDecl = methodDecls[i];
					break;
				}
			}
			if (methodDecl != null) {
				int offset = methodDecl.sourceStart;
				Binding binding = methodDecl.getBinding();
				if (binding != null)
					method = (IFunction) ((JavaElement) method).resolved(binding);
				match = new MethodDeclarationMatch(method, SearchMatch.A_ACCURATE, offset, methodDecl.sourceEnd-offset+1, locator.getParticipant(), resource);
				locator.report(match);
			}
		}
	}
}
public int resolveLevel(ASTNode possibleMatchingNode) {
	if (this.pattern.findReferences) {
		if (possibleMatchingNode instanceof MessageSend) {
			return resolveLevel((MessageSend) possibleMatchingNode);
		}
	}
	if (this.pattern.findDeclarations) {
		if (possibleMatchingNode instanceof MethodDeclaration) {
			return resolveLevel(((MethodDeclaration) possibleMatchingNode).getBinding());
		}
		else if (possibleMatchingNode instanceof InferredMethod)
			return resolveLevel(((InferredMethod) possibleMatchingNode).methodBinding);
}
	return IMPOSSIBLE_MATCH;
}
public int resolveLevel(Binding binding) {
	if (binding == null) return INACCURATE_MATCH;
	if (!(binding instanceof MethodBinding)) return IMPOSSIBLE_MATCH;

	MethodBinding method = (MethodBinding) binding;
	boolean skipVerif = this.pattern.findDeclarations && this.mayBeGeneric;
	int methodLevel = matchMethod(method, skipVerif);
	if (methodLevel == IMPOSSIBLE_MATCH) {
		if (method != method.original()) methodLevel = matchMethod(method.original(), skipVerif);
		if (methodLevel == IMPOSSIBLE_MATCH) {
			return IMPOSSIBLE_MATCH;
		} else {
			method = method.original();
		}
	}

	// declaring type
	char[] qualifiedPattern = qualifiedPattern(this.pattern.getDeclaringSimpleName(), this.pattern.getDeclaringQualification());
	if (qualifiedPattern == null) return methodLevel; // since any declaring class will do

	boolean subType = !method.isStatic() && !method.isPrivate();
	if (subType && this.pattern.getDeclaringQualification() != null && method.declaringClass != null && method.declaringClass.fPackage != null) {
		subType = CharOperation.compareWith(this.pattern.getDeclaringQualification(), method.declaringClass.fPackage.shortReadableName()) == 0;
	}
	int declaringLevel = subType
		? resolveLevelAsSubtype(qualifiedPattern, method.declaringClass, null)
		: resolveLevelForType(qualifiedPattern, method.declaringClass);
	return methodLevel > declaringLevel ? declaringLevel : methodLevel; // return the weaker match
}
protected int resolveLevel(MessageSend messageSend) {
	MethodBinding method = messageSend.binding;
	if (method == null) {
		return INACCURATE_MATCH;
	}
	if (messageSend.resolvedType == null) {
		// Closest match may have different argument numbers when ProblemReason is NotFound
		// see MessageSend#resolveType(BlockScope)
		// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=97322
		int argLength = messageSend.arguments == null ? 0 : messageSend.arguments.length;
		if (pattern.parameterSimpleNames == null || argLength == pattern.parameterSimpleNames.length) {
			return INACCURATE_MATCH;
		}
		return IMPOSSIBLE_MATCH;
	}

	int methodLevel = matchMethod(method, false);
	if (methodLevel == IMPOSSIBLE_MATCH) {
		if (method != method.original()) methodLevel = matchMethod(method.original(), false);
		if (methodLevel == IMPOSSIBLE_MATCH) return IMPOSSIBLE_MATCH;
		method = method.original();
	}

	// receiver type
	char[] qualifiedPattern = qualifiedPattern(this.pattern.getDeclaringSimpleName(), this.pattern.getDeclaringQualification());
	if (qualifiedPattern == null) return methodLevel; // since any declaring class will do

	int declaringLevel;
	if (isVirtualInvoke(method, messageSend) && (messageSend.actualReceiverType instanceof ReferenceBinding)) {
		ReferenceBinding methodReceiverType = (ReferenceBinding) messageSend.actualReceiverType;
		declaringLevel = resolveLevelAsSubtype(qualifiedPattern, methodReceiverType, method.parameters);
		if (declaringLevel == IMPOSSIBLE_MATCH) {
			if (method.declaringClass == null || this.allSuperDeclaringTypeNames == null) {
				declaringLevel = INACCURATE_MATCH;
			} else {
				char[][] compoundName = methodReceiverType.compoundName;
				for (int i = 0, max = this.allSuperDeclaringTypeNames.length; i < max; i++) {
					if (CharOperation.equals(this.allSuperDeclaringTypeNames[i], compoundName)) {
						return methodLevel // since this is an ACCURATE_MATCH so return the possibly weaker match
							| SUPER_INVOCATION_FLAVOR; // this is an overridden method => add flavor to returned level
					}
				}
				/* Do not return interfaces potential matches
				 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=157814#c8"
				if (methodReceiverType.isInterface()) {
					// all methods interface with same name and parameters are potential matches
					// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=156491
					return INACCURATE_MATCH | POLYMORPHIC_FLAVOR;
				}
				*/
			}
		}
		if ((declaringLevel & FLAVORS_MASK) != 0) {
			// level got some flavors => return it
			return declaringLevel;
		}
	} else {
		declaringLevel = resolveLevelForType(qualifiedPattern, method.declaringClass);
	}
	return methodLevel > declaringLevel ? declaringLevel : methodLevel; // return the weaker match
}
/**
 * Returns whether the given reference type binding matches or is a subtype of a type
 * that matches the given qualified pattern.
 * Returns ACCURATE_MATCH if it does.
 * Returns INACCURATE_MATCH if resolve fails
 * Returns IMPOSSIBLE_MATCH if it doesn't.
 */
protected int resolveLevelAsSubtype(char[] qualifiedPattern, ReferenceBinding type, TypeBinding[] argumentTypes) {
	if (type == null) return INACCURATE_MATCH;

	int level = resolveLevelForType(qualifiedPattern, type);
	if (level != IMPOSSIBLE_MATCH) {
		level |= OVERRIDDEN_METHOD_FLAVOR;
		
		return level;
	}

	// matches superclass
	if (!CharOperation.equals(type.compoundName, TypeConstants.JAVA_LANG_OBJECT)) {
		level = resolveLevelAsSubtype(qualifiedPattern, type.getSuperBinding(), argumentTypes);
		if (level != IMPOSSIBLE_MATCH) {
			if (argumentTypes != null) {
				// need to verify if method may be overridden
				MethodBinding[] methods = type.getMethods(this.pattern.selector);
				for (int i=0, length=methods.length; i<length; i++) {
					MethodBinding method = methods[i];
					TypeBinding[] parameters = method.parameters;
					if (argumentTypes.length == parameters.length) {
						boolean found = true;
						for (int j=0,l=parameters.length; j<l; j++) {
							if (parameters[j] != argumentTypes[j]) {
								found = false;
								break;
							}
						}
						if (found) { // one method match in hierarchy
							if ((level & OVERRIDDEN_METHOD_FLAVOR) != 0) {
								// this method is already overridden on a super class, current match is impossible
								return IMPOSSIBLE_MATCH;
							}
							if (!method.isAbstract()) {
								// store the fact that the method is overridden
								level |= OVERRIDDEN_METHOD_FLAVOR;
							}
						}
					}
				}
			}
			return level | SUB_INVOCATION_FLAVOR; // add flavor to returned level
		}
	}

	
	return INACCURATE_MATCH;
}
public String toString() {
	return "Locator for " + this.pattern.toString(); //$NON-NLS-1$
}

public int match(InferredMethod inferredMethod, MatchingNodeSet nodeSet) {
	if (!this.pattern.findDeclarations) return IMPOSSIBLE_MATCH;

	// Verify method name
	if (!matchesName(this.pattern.selector, inferredMethod.name)) return IMPOSSIBLE_MATCH;
	
	boolean resolve = ((InternalSearchPattern)this.pattern).mustResolve;
	
	// Verify type name
	if(!resolve && inferredMethod.inType != null && !matchesName(QualificationHelpers.createFullyQualifiedName(this.pattern.getDeclaringQualification(), this.pattern.getDeclaringSimpleName()), inferredMethod.inType.getName())) return IMPOSSIBLE_MATCH;

	// Verify parameters types
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
					this.methodDeclarationsWithInvalidParam.put(inferredMethod.getFunctionDeclaration(), null);
				} else {
					return IMPOSSIBLE_MATCH;
				}
			}
		}
	}

	// Method declaration may match pattern
	return nodeSet.addMatch(inferredMethod , resolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
}
}
