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

import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.InferredAttribute;
import org.eclipse.wst.jsdt.core.infer.InferredMethod;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Reference;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IIndexConstants;

public abstract class PatternLocator implements IIndexConstants {

// store pattern info
protected int matchMode;
protected boolean isCaseSensitive;
protected boolean isCamelCase;
protected boolean isEquivalentMatch;
protected boolean isErasureMatch;
protected boolean mustResolve;
protected boolean mayBeGeneric;

// match to report
SearchMatch match = null;

/* match levels */
public static final int IMPOSSIBLE_MATCH = 0;
public static final int INACCURATE_MATCH = 1;
public static final int POSSIBLE_MATCH = 2;
public static final int ACCURATE_MATCH = 3;
public static final int ERASURE_MATCH = 4;

// Possible rule match flavors
// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=79866
public static final int EXACT_FLAVOR = 0x0010;
public static final int PREFIX_FLAVOR = 0x0020;
public static final int PATTERN_FLAVOR = 0x0040;
public static final int REGEXP_FLAVOR = 0x0080;
public static final int CAMELCASE_FLAVOR = 0x0100;
public static final int SUPER_INVOCATION_FLAVOR = 0x0200;
public static final int SUB_INVOCATION_FLAVOR = 0x0400;
public static final int OVERRIDDEN_METHOD_FLAVOR = 0x0800;
public static final int MATCH_LEVEL_MASK = 0x0F;
public static final int FLAVORS_MASK = ~MATCH_LEVEL_MASK;

/* match container */
public static final int COMPILATION_UNIT_CONTAINER = 1;
public static final int CLASS_CONTAINER = 2;
public static final int METHOD_CONTAINER = 4;
public static final int FIELD_CONTAINER = 8;
public static final int ALL_CONTAINER =
	COMPILATION_UNIT_CONTAINER | CLASS_CONTAINER | METHOD_CONTAINER | FIELD_CONTAINER;

/* match rule */
public static final int RAW_MASK = SearchPattern.R_EQUIVALENT_MATCH | SearchPattern.R_ERASURE_MATCH;
public static final int RULE_MASK = RAW_MASK; // no other values for the while...

public static PatternLocator patternLocator(SearchPattern pattern) {
	switch (((InternalSearchPattern)pattern).kind) {
		case IIndexConstants.PKG_REF_PATTERN :
			return new PackageReferenceLocator((PackageReferencePattern) pattern);
		case IIndexConstants.PKG_DECL_PATTERN :
			return new PackageDeclarationLocator((PackageDeclarationPattern) pattern);
		case IIndexConstants.TYPE_REF_PATTERN :
			return new TypeReferenceLocator((TypeReferencePattern) pattern);
		case IIndexConstants.TYPE_DECL_PATTERN :
			return new TypeDeclarationLocator((TypeDeclarationPattern) pattern);
		case IIndexConstants.SUPER_REF_PATTERN :
			return new SuperTypeReferenceLocator((SuperTypeReferencePattern) pattern);
		case IIndexConstants.CONSTRUCTOR_PATTERN :
			return new ConstructorLocator((ConstructorPattern) pattern);
		case IIndexConstants.FIELD_PATTERN :
			IJavaScriptElement element = ((FieldPattern)pattern).getJavaElement();
			if (element instanceof IField) {
				IField field = (IField) element;
				if (field.getDeclaringType()==null)
					return new LocalVariableLocator((VariablePattern)pattern);
			}
			return new FieldLocator((FieldPattern) pattern);
		case IIndexConstants.METHOD_PATTERN :
			return new MethodLocator((MethodPattern) pattern);
		case IIndexConstants.OR_PATTERN :
			return new OrLocator((OrPattern) pattern);
		case IIndexConstants.LOCAL_VAR_PATTERN :
			return new LocalVariableLocator((LocalVariablePattern) pattern);
	}
	return null;
}
public static char[] qualifiedPattern(char[] simpleNamePattern, char[] qualificationPattern) {
	// NOTE: if case insensitive search then simpleNamePattern & qualificationPattern are assumed to be lowercase
	if (simpleNamePattern == null) {
		if (qualificationPattern == null) return null;
		return CharOperation.concat(qualificationPattern, ONE_STAR, '.');
	} else {
		return qualificationPattern == null
			? CharOperation.concat(ONE_STAR, simpleNamePattern)
			: CharOperation.concat(qualificationPattern, simpleNamePattern, '/');
	}
}
public static char[] qualifiedSourceName(TypeBinding binding) {
	if (binding instanceof ReferenceBinding) {
		ReferenceBinding type = (ReferenceBinding) binding;
		if (type.isLocalType())
			return type.isMemberType()
				? CharOperation.concat(qualifiedSourceName(type.enclosingType()), type.sourceName(), '.')
				: CharOperation.concat(qualifiedSourceName(type.enclosingType()), new char[] {'.', '1', '.'}, type.sourceName());
	}
	return binding != null ? binding.qualifiedSourceName() : null;
}

public PatternLocator(SearchPattern pattern) {
	int matchRule = pattern.getMatchRule();
	this.isCaseSensitive = (matchRule & SearchPattern.R_CASE_SENSITIVE) != 0;
	this.isCamelCase = (matchRule & SearchPattern.R_CAMELCASE_MATCH) != 0;
	this.isErasureMatch = (matchRule & SearchPattern.R_ERASURE_MATCH) != 0;
	this.isEquivalentMatch = (matchRule & SearchPattern.R_EQUIVALENT_MATCH) != 0;
	this.matchMode = matchRule & JavaSearchPattern.MATCH_MODE_MASK;
	this.mustResolve = ((InternalSearchPattern)pattern).mustResolve;
}
/*
 * Clear caches
 */
protected void clear() {
	// nothing to clear by default
}
/* (non-Javadoc)
 * Modify PatternLocator.qualifiedPattern behavior:
 * do not add star before simple name pattern when qualification pattern is null.
 * This avoid to match p.X when pattern is only X...
 */
protected char[] getQualifiedPattern(char[] simpleNamePattern, char[] qualificationPattern) {
	// NOTE: if case insensitive search then simpleNamePattern & qualificationPattern are assumed to be lowercase
	if (simpleNamePattern == null) {
		if (qualificationPattern == null) return null;
		return CharOperation.concat(qualificationPattern, ONE_STAR, '.');
	} else if (qualificationPattern == null) {
		return simpleNamePattern;
	} else {
		return CharOperation.concat(qualificationPattern, simpleNamePattern, '.');
	}
}
/* (non-Javadoc)
 * Modify PatternLocator.qualifiedSourceName behavior:
 * also concatene enclosing type name when type is a only a member type.
 */
protected char[] getQualifiedSourceName(TypeBinding binding) {
	TypeBinding type = binding instanceof ArrayBinding ? ((ArrayBinding)binding).leafComponentType : binding;
	if (type instanceof ReferenceBinding) {
		if (type.isLocalType()) {
			return CharOperation.concat(qualifiedSourceName(type.enclosingType()), new char[] {'.', '1', '.'}, binding.sourceName());
		} else if (type.isMemberType()) {
			return CharOperation.concat(qualifiedSourceName(type.enclosingType()), binding.sourceName(), '.');
		}
	}
	return binding != null ? binding.qualifiedSourceName() : null;
}
/*
 * Get binding of type argument from a class unit scope and its index position.
 * Cache is lazy initialized and if no binding is found, then store a problem binding
 * to avoid making research twice...
 */
protected TypeBinding getTypeNameBinding(int index) {
	return null;
}
/**
 * Initializes this search pattern so that polymorphic search can be performed.
 */
public void initializePolymorphicSearch(MatchLocator locator) {
	// default is to do nothing
}
/**
 * Check if the given ast node syntactically matches this pattern.
 * If it does, add it to the match set.
 * Returns the match level.
 */
public int match(ASTNode node, MatchingNodeSet nodeSet) { // needed for some generic nodes
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(ConstructorDeclaration node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(Expression node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(FieldDeclaration node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(LocalDeclaration node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(MethodDeclaration node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(MessageSend node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(Reference node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(TypeDeclaration node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(TypeReference node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(InferredType inferredType, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}

public int match(InferredMethod inferredMethod, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}

public int match(InferredAttribute inferredAttribute, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}

/**
 * Returns the type(s) of container for this pattern.
 * It is a bit combination of types, denoting compilation unit, class declarations, field declarations or method declarations.
 */
protected int matchContainer() {
	// override if the pattern can be more specific
	return ALL_CONTAINER;
}
/**
 * Returns whether the given name matches the given pattern.
 */
protected boolean matchesName(char[] pattern, char[] name) {
	if (pattern == null) return true; // null is as if it was "*"
	if (name == null) return false; // cannot match null name
	return matchNameValue(pattern, name) != IMPOSSIBLE_MATCH;
}
/**
 * Return how the given name matches the given pattern.
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=79866"
 *
 * @param pattern
 * @param name
 * @return Possible values are:
 * <ul>
 * 	<li> {@link #ACCURATE_MATCH}</li>
 * 	<li> {@link #IMPOSSIBLE_MATCH}</li>
 * 	<li> {@link #POSSIBLE_MATCH} which may be flavored with following values:
 * 		<ul>
 * 		<li>{@link #EXACT_FLAVOR}: Given name is equals to pattern</li>
 * 		<li>{@link #PREFIX_FLAVOR}: Given name prefix equals to pattern</li>
 * 		<li>{@link #CAMELCASE_FLAVOR}: Given name matches pattern as Camel Case</li>
 * 		<li>{@link #PATTERN_FLAVOR}: Given name matches pattern as Pattern (ie. using '*' and '?' characters)</li>
 * 		</ul>
 * 	</li>
 * </ul>
 */
protected int matchNameValue(char[] pattern, char[] name) {
	if (pattern == null) return ACCURATE_MATCH; // null is as if it was "*"
	if (name == null) return IMPOSSIBLE_MATCH; // cannot match null name
	if (name.length == 0) { // empty name
		if (pattern.length == 0) { // can only matches empty pattern
			return ACCURATE_MATCH;
		}
		return IMPOSSIBLE_MATCH;
	} else if (pattern.length == 0) {
		return IMPOSSIBLE_MATCH; // need to have both name and pattern length==0 to be accurate
	}
	boolean matchFirstChar = !this.isCaseSensitive || pattern[0] == name[0];
	boolean sameLength = pattern.length == name.length;
	boolean canBePrefix = name.length >= pattern.length;
	if (this.isCamelCase && matchFirstChar && CharOperation.camelCaseMatch(pattern, name)) {
		return POSSIBLE_MATCH;
	}
	switch (this.matchMode) {
		case SearchPattern.R_EXACT_MATCH:
			if (!this.isCamelCase) {
				if (sameLength && matchFirstChar && CharOperation.equals(pattern, name, this.isCaseSensitive)) {
					return POSSIBLE_MATCH | EXACT_FLAVOR;
				}
				break;
			}
			// fall through next case to match as prefix if camel case failed
		case SearchPattern.R_PREFIX_MATCH:
			if (canBePrefix && matchFirstChar && CharOperation.prefixEquals(pattern, name, this.isCaseSensitive)) {
				return POSSIBLE_MATCH;
			}
			break;
		case SearchPattern.R_PATTERN_MATCH:
			if (!this.isCaseSensitive) {
				pattern = CharOperation.toLowerCase(pattern);
			}
			if (CharOperation.match(pattern, name, this.isCaseSensitive)) {
				return POSSIBLE_MATCH;
			}
			break;
		case SearchPattern.R_REGEXP_MATCH :
			// TODO (frederic) implement regular expression match
			break;
	}
	return IMPOSSIBLE_MATCH;
}

protected boolean isAccurateNameMatch(char[] pattern, char[] sourceName) {
	if ((this.matchMode & SearchPattern.R_PREFIX_MATCH) != 0) {
		if (CharOperation.prefixEquals(pattern, sourceName, this.isCaseSensitive)) {
			return true;
		}
	}
	if (this.isCamelCase) {
		if (!this.isCaseSensitive || (pattern.length>0 && sourceName.length>0 && pattern[0] == sourceName[0])) {
			if (CharOperation.camelCaseMatch(pattern, sourceName)) {
				return true;
			}
		}
		if (this.matchMode == SearchPattern.R_EXACT_MATCH) {
			return CharOperation.prefixEquals(pattern, sourceName, this.isCaseSensitive);
		}
	}
	return CharOperation.match(pattern, sourceName, this.isCaseSensitive);
}
/**
 * Returns whether the given type reference matches the given pattern.
 */
protected boolean matchesTypeReference(char[] pattern, TypeReference type) {
	if (pattern == null) return true; // null is as if it was "*"
	if (type == null) return true; // treat as an inexact match

	char[][] compoundName = type.getTypeName();
	char[] simpleName = compoundName[compoundName.length - 1];
	int dimensions = type.dimensions() * 2;
	if (dimensions > 0) {
		int length = simpleName.length;
		char[] result = new char[length + dimensions];
		System.arraycopy(simpleName, 0, result, 0, length);
		for (int i = length, l = result.length; i < l;) {
			result[i++] = '[';
			result[i++] = ']';
		}
		simpleName = result;
	}

	return matchesName(pattern, simpleName);
}
/**
 * Returns the match level for the given importRef.
 */
protected int matchLevel(ImportReference importRef) {
	// override if interested in import references which are caught by the generic version of match(ASTNode, MatchingNodeSet)
	return IMPOSSIBLE_MATCH;
}
/**
 * Reports the match of the given import reference if the resolveLevel is high enough.
 */
protected void matchLevelAndReportImportRef(ImportReference importRef, Binding binding, MatchLocator locator) throws CoreException {
	int level = resolveLevel(binding);
	if (level >= INACCURATE_MATCH) {
		matchReportImportRef(
			importRef,
			binding,
			locator.createImportHandle(importRef),
			level == ACCURATE_MATCH
				? SearchMatch.A_ACCURATE
				: SearchMatch.A_INACCURATE,
			locator);
	}
}
/**
 * Reports the match of the given import reference.
 */
protected void matchReportImportRef(ImportReference importRef, Binding binding, IJavaScriptElement element, int accuracy, MatchLocator locator) throws CoreException {
	if (locator.encloses(element)) {
		// default is to report a match as a regular ref.
		this.matchReportReference(importRef, element, null/*no binding*/, accuracy, locator);
	}
}
/**
 * Reports the match of the given reference.
 */
protected void matchReportReference(ASTNode reference, IJavaScriptElement element, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
	match = null;
	int referenceType = referenceType();
	int offset = reference.sourceStart;
	switch (referenceType) {
		case IJavaScriptElement.PACKAGE_FRAGMENT:
			match = locator.newPackageReferenceMatch(element, accuracy, offset, reference.sourceEnd-offset+1, reference);
			break;
		case IJavaScriptElement.TYPE:
			match = locator.newTypeReferenceMatch(element, elementBinding, accuracy, offset, reference.sourceEnd-offset+1, reference);
			break;
		case IJavaScriptElement.FIELD:
			match = locator.newFieldReferenceMatch(element, elementBinding, accuracy, offset, reference.sourceEnd-offset+1, reference);
			break;
		case IJavaScriptElement.LOCAL_VARIABLE:
			match = locator.newLocalVariableReferenceMatch(element, accuracy, offset, reference.sourceEnd-offset+1, reference);
			break;
	}
	if (match != null) {
		locator.report(match);
	}
}
/**
 * Reports the match of the given reference. Also provide a local element to eventually report in match.
 */
protected void matchReportReference(ASTNode reference, IJavaScriptElement element, IJavaScriptElement localElement, IJavaScriptElement[] otherElements, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
	matchReportReference(reference, element, elementBinding, accuracy, locator);
}
/**
 * Reports the match of the given reference. Also provide a scope to look for potential other elements.
 */
protected void matchReportReference(ASTNode reference, IJavaScriptElement element, Binding elementBinding, Scope scope, int accuracy, MatchLocator locator) throws CoreException {
	matchReportReference(reference, element, elementBinding, accuracy, locator);
}
public SearchMatch newDeclarationMatch(ASTNode reference, IJavaScriptElement element, Binding elementBinding, int accuracy, int length, MatchLocator locator) {
	int offset=(reference!=null  )?reference.sourceStart : 0;
	if (reference instanceof AbstractMethodDeclaration) {
		AbstractMethodDeclaration method = (AbstractMethodDeclaration) reference;
		if (method.getName()==null && method.inferredMethod!=null)
		{
			offset=method.inferredMethod.nameStart;
			if (length>=0)
				length=method.inferredMethod.name.length;
		}
	}

		return locator.newDeclarationMatch(element, elementBinding, accuracy, offset, length);
}
protected int referenceType() {
	return 0; // defaults to unknown (a generic JavaSearchMatch will be created)
}
/**
 * Finds out whether the given ast node matches this search pattern.
 * Returns IMPOSSIBLE_MATCH if it doesn't.
 * Returns INACCURATE_MATCH if it potentially matches this search pattern (ie.
 * it has already been resolved but resolving failed.)
 * Returns ACCURATE_MATCH if it matches exactly this search pattern (ie.
 * it doesn't need to be resolved or it has already been resolved.)
 */
public int resolveLevel(ASTNode possibleMatchingNode) {
	// only called with nodes which were possible matches to the call to matchLevel
	// need to do instance of checks to find out exact type of ASTNode
	return IMPOSSIBLE_MATCH;
}
/*
 * Update pattern locator match comparing type arguments with pattern ones.
 * Try to resolve pattern and look for compatibility with type arguments
 * to set match rule.
 */
protected void updateMatch(TypeBinding[] argumentsBinding, MatchLocator locator, char[][] patternArguments, boolean hasTypeParameters) {
	// Only possible if locator has an unit scope.
	if (locator.unitScope == null) return;

	// First compare lengthes
	int patternTypeArgsLength = patternArguments==null ? 0 : patternArguments.length;
	int typeArgumentsLength = argumentsBinding == null ? 0 : argumentsBinding.length;

	// Initialize match rule
	int matchRule = match.getRule();
	if (match.isRaw()) {
		if (patternTypeArgsLength != 0) {
			matchRule &= ~SearchPattern.R_FULL_MATCH;
		}
	}
	if (hasTypeParameters) {
		matchRule = SearchPattern.R_ERASURE_MATCH;
	}

	// Compare arguments lengthes
	if (patternTypeArgsLength == typeArgumentsLength) {
		if (!match.isRaw() && hasTypeParameters) {
			// generic patterns are always not compatible match
			match.setRule(SearchPattern.R_ERASURE_MATCH);
			return;
		}
	} else {
		if (patternTypeArgsLength==0) {
			if (!match.isRaw() || hasTypeParameters) {
				match.setRule(matchRule & ~SearchPattern.R_FULL_MATCH);
			}
		} else  if (typeArgumentsLength==0) {
			// raw binding is always compatible
			match.setRule(matchRule & ~SearchPattern.R_FULL_MATCH);
		} else {
			match.setRule(0); // impossible match
		}
		return;
	}
	if (argumentsBinding == null || patternArguments == null) {
		match.setRule(matchRule);
		return;
	}

	// Compare binding for each type argument only if pattern is not erasure only and at first level
	if (!hasTypeParameters && !match.isRaw() && (match.isEquivalent() || match.isExact())) {
		for (int i=0; i<typeArgumentsLength; i++) {
			// Get parameterized type argument binding
			TypeBinding argumentBinding = argumentsBinding[i];
			// Get binding for pattern argument
			char[] patternTypeArgument = patternArguments[i];
			char patternWildcard = patternTypeArgument[0];
			char[] patternTypeName = patternTypeArgument;
			
			patternTypeName = Signature.toCharArray(patternTypeName);
			TypeBinding patternBinding = locator.getType(patternTypeArgument, patternTypeName);

			// If have no binding for pattern arg, then we won't be able to refine accuracy
			if (patternBinding == null) {
				continue;
			}

			// Verify tha pattern binding is compatible with match type argument binding
			switch (patternWildcard) {
				default:
					if (argumentBinding == patternBinding)
						// valid only when arg is equals to pattern
						continue;
					break;
			}

			// Argument does not match => erasure match will be the only possible one
			match.setRule(SearchPattern.R_ERASURE_MATCH);
			return;
		}
	}

	// Set match rule
	match.setRule(matchRule);
}
/**
 * Finds out whether the given binding matches this search pattern.
 * Returns ACCURATE_MATCH if it does.
 * Returns INACCURATE_MATCH if resolve failed but match is still possible.
 * Returns IMPOSSIBLE_MATCH otherwise.
 * Default is to return INACCURATE_MATCH.
 */
public int resolveLevel(Binding binding) {
	// override if the pattern can match the binding
	return INACCURATE_MATCH;
}
/**
 * Returns whether the given type binding matches the given simple name pattern
 * and qualification pattern.
 * Note that from since 3.1, this method resolve to accurate member or local types
 * even if they are not fully qualified (ie. X.Member instead of p.X.Member).
 * Returns ACCURATE_MATCH if it does.
 * Returns INACCURATE_MATCH if resolve failed.
 * Returns IMPOSSIBLE_MATCH if it doesn't.
 */
protected int resolveLevelForType(char[] simpleNamePattern, char[] qualificationPattern, TypeBinding binding) {
//	return resolveLevelForType(qualifiedPattern(simpleNamePattern, qualificationPattern), type);
	if (binding==TypeBinding.ANY || binding==TypeBinding.UNKNOWN)
		return ACCURATE_MATCH;
	if (Arrays.equals(Signature.ANY, simpleNamePattern))
		return ACCURATE_MATCH;
	char[] qualifiedPattern = getQualifiedPattern(simpleNamePattern, qualificationPattern);
	int level = resolveLevelForType(qualifiedPattern, binding);
	if (level == ACCURATE_MATCH || binding == null) return level;
	TypeBinding type = binding instanceof ArrayBinding ? ((ArrayBinding)binding).leafComponentType : binding;
	char[] sourceName = null;
	if (type.isMemberType() || type.isLocalType()) {
		if (qualificationPattern != null) {
			sourceName =  getQualifiedSourceName(binding);
		} else {
			sourceName =  binding.sourceName();
		}
	} else if (qualificationPattern == null) {
		sourceName =  getQualifiedSourceName(binding);
	}
	if (sourceName == null) return IMPOSSIBLE_MATCH;
	boolean matchPattern = isAccurateNameMatch(qualifiedPattern, sourceName);
	return matchPattern ? ACCURATE_MATCH : IMPOSSIBLE_MATCH;

}
/**
 * Returns whether the given type binding matches the given qualified pattern.
 * Returns ACCURATE_MATCH if it does.
 * Returns INACCURATE_MATCH if resolve failed.
 * Returns IMPOSSIBLE_MATCH if it doesn't.
 */
protected int resolveLevelForType(char[] qualifiedPattern, TypeBinding type) {
	if (qualifiedPattern == null) return ACCURATE_MATCH;
	if (type == null) return INACCURATE_MATCH;

	// NOTE: if case insensitive search then qualifiedPattern is assumed to be lowercase
    char [] filePath=new char[]{};
    char [] bindingPath=filePath;
	int index;
	if ( (index=CharOperation.lastIndexOf('/', qualifiedPattern))>-1)
	{
		filePath=CharOperation.subarray(qualifiedPattern, 0, index);
		qualifiedPattern=CharOperation.subarray(qualifiedPattern, index+1, qualifiedPattern.length);
		bindingPath=type.getFileName();
		index=CharOperation.lastIndexOf('/', bindingPath);
		if (index>-1)
			bindingPath=CharOperation.subarray(bindingPath, 0, index);
	}
	
	char[] qualifiedPackageName = type.qualifiedPackageName();
	char[] qualifiedSourceName = qualifiedSourceName(type);
	char[] fullyQualifiedTypeName = qualifiedPackageName.length == 0
		? qualifiedSourceName
		: CharOperation.concat(qualifiedPackageName, qualifiedSourceName, '.');
	if (CharOperation.match(qualifiedPattern, fullyQualifiedTypeName, this.isCaseSensitive))
	{
		if (filePath.length>0)
		{
		   return (CharOperation.endsWith(bindingPath, filePath)) ? ACCURATE_MATCH:IMPOSSIBLE_MATCH;	
		}
		return ACCURATE_MATCH;
	}
	return IMPOSSIBLE_MATCH;
}
/* (non-Javadoc)
 * Resolve level for type with a given binding with all pattern information.
 */
protected int resolveLevelForType (char[] simpleNamePattern,
									char[] qualificationPattern,
									char[][][] patternTypeArguments,
									int depth,
									TypeBinding type) {
	// standard search with no generic additional information must succeed
	int level = resolveLevelForType(simpleNamePattern, qualificationPattern, type);
	if (level == IMPOSSIBLE_MATCH) return IMPOSSIBLE_MATCH;
	if (type == null || patternTypeArguments == null || patternTypeArguments.length == 0 || depth >= patternTypeArguments.length) {
		return level;
	}
	
	// Standard types (ie. neither generic nor parameterized nor raw types)
	// cannot match pattern with type parameters or arguments
	return (patternTypeArguments[depth]==null || patternTypeArguments[depth].length==0) ? level : IMPOSSIBLE_MATCH;
	
}

protected int resolveLevelUsingSearchPrefix(char[] searchPrefix, TypeBinding binding) {
	if (binding == TypeBinding.ANY || binding == TypeBinding.UNKNOWN)
		return ACCURATE_MATCH;

	char[] sourceName = qualifiedSourceName(binding);
	if (sourceName == null) return IMPOSSIBLE_MATCH;
	
	if (isAccurateNameMatch(searchPrefix, sourceName))
		return ACCURATE_MATCH;
	
	int index = CharOperation.lastIndexOf('.', sourceName);
	if (index>=0 && isAccurateNameMatch(searchPrefix, CharOperation.subarray(sourceName, index+1,sourceName.length)))
		return ACCURATE_MATCH;
	
	return IMPOSSIBLE_MATCH;
}
public String toString(){
	return "SearchPattern"; //$NON-NLS-1$
}

public int matchMetadataElement(IJavaScriptElement element)
{
	return IMPOSSIBLE_MATCH;
}


}
