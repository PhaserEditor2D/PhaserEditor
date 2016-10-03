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
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.compiler.env.IBinaryField;
import org.eclipse.wst.jsdt.internal.compiler.env.IBinaryMethod;
import org.eclipse.wst.jsdt.internal.compiler.env.IBinaryType;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.wst.jsdt.internal.core.BinaryType;
import org.eclipse.wst.jsdt.internal.core.ClassFile;
import org.eclipse.wst.jsdt.internal.core.JavaElement;
import org.eclipse.wst.jsdt.internal.core.ResolvedBinaryField;
import org.eclipse.wst.jsdt.internal.core.ResolvedBinaryMethod;
import org.eclipse.wst.jsdt.internal.core.ResolvedBinaryType;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IIndexConstants;
import org.eclipse.wst.jsdt.internal.core.util.QualificationHelpers;

public class ClassFileMatchLocator implements IIndexConstants {

public static char[] convertClassFileFormat(char[] name) {
	return CharOperation.replaceOnCopy(name, '/', '.');
}

private boolean checkDeclaringType(IBinaryType enclosingBinaryType, char[] simpleName, char[] qualification, boolean isCaseSensitive, boolean isCamelCase) {
	if (simpleName == null && qualification == null) return true;
	if (enclosingBinaryType == null) return true;

	char[] declaringTypeName = convertClassFileFormat(enclosingBinaryType.getName());
	return checkTypeName(simpleName, qualification, declaringTypeName, isCaseSensitive, isCamelCase);
}
private boolean checkParameters(char[] methodDescriptor, char[][] parameterSimpleNames, char[][] parameterQualifications, boolean isCaseSensitive, boolean isCamelCase) {
	char[][] arguments = Signature.getParameterTypes(methodDescriptor);
	int parameterCount = parameterSimpleNames.length;
	if (parameterCount != arguments.length) return false;
	for (int i = 0; i < parameterCount; i++)
		if (!checkTypeName(parameterSimpleNames[i], parameterQualifications[i], Signature.toCharArray(arguments[i]), isCaseSensitive, isCamelCase))
			return false;
	return true;
}
private boolean checkTypeName(char[] simpleName, char[] qualification, char[] fullyQualifiedTypeName, boolean isCaseSensitive, boolean isCamelCase) {
	// NOTE: if case insensitive then simpleName & qualification are assumed to be lowercase
	char[] wildcardPattern = PatternLocator.qualifiedPattern(simpleName, qualification);
	if (wildcardPattern == null) return true;
	return CharOperation.match(wildcardPattern, fullyQualifiedTypeName, isCaseSensitive);
}

private boolean checkTypeName(char[] typeName1, char[] typeName2, boolean isCaseSensitive, boolean isCamelCase) {
	char[][] typeNameSeperated = QualificationHelpers.seperateFullyQualifedName(typeName1);
	
	char[] wildcardPattern = PatternLocator.qualifiedPattern(typeNameSeperated[QualificationHelpers.SIMPLE_NAMES_INDEX], typeNameSeperated[QualificationHelpers.QULIFIERS_INDEX]);
	if (wildcardPattern == null) return true;
	return CharOperation.match(wildcardPattern, typeName2, isCaseSensitive);
}
/**
 * Locate declaration in the current class file. This class file is always in a jar.
 */
public void locateMatches(MatchLocator locator, ClassFile classFile, IBinaryType info) throws CoreException {
	// check class definition
	SearchPattern pattern = locator.pattern;
	BinaryType binaryType = (BinaryType) classFile.getType();
	if (matchBinary(pattern, info, null)) {
		binaryType = new ResolvedBinaryType((JavaElement) binaryType.getParent(), binaryType.getElementName(), binaryType.getKey());
		locator.reportBinaryMemberDeclaration(null, binaryType, null, info, SearchMatch.A_ACCURATE);
	}

	int accuracy = SearchMatch.A_ACCURATE;
	if (((InternalSearchPattern)pattern).mustResolve) {
		try {
			BinaryTypeBinding binding = locator.cacheBinaryType(binaryType, info);
			if (binding != null) {
				// filter out element not in hierarchy scope
				if (!locator.typeInHierarchy(binding)) return;

				MethodBinding[] methods = binding.methods();
				for (int i = 0, l = methods.length; i < l; i++) {
					MethodBinding method = methods[i];
					if (locator.patternLocator.resolveLevel(method) == PatternLocator.ACCURATE_MATCH) {
						char[] methodSignature = method.signature();
						IFunction methodHandle = binaryType.getFunction(
							new String(method.isConstructor() ? binding.compoundName[binding.compoundName.length-1] : method.selector),
							CharOperation.toStrings(Signature.getParameterTypes(convertClassFileFormat(methodSignature))));
						locator.reportBinaryMemberDeclaration(null, methodHandle, method, info, SearchMatch.A_ACCURATE);
					}
				}

				FieldBinding[] fields = binding.fields();
				for (int i = 0, l = fields.length; i < l; i++) {
					FieldBinding field = fields[i];
					if (locator.patternLocator.resolveLevel(field) == PatternLocator.ACCURATE_MATCH) {
						IField fieldHandle = binaryType.getField(new String(field.name));
						locator.reportBinaryMemberDeclaration(null, fieldHandle, field, info, SearchMatch.A_ACCURATE);
					}
				}

				// no need to check binary info since resolve was successful
				return;
			}
		} catch (AbortCompilation e) { // if compilation was aborted it is a problem with the class path
		}
		// report as a potential match if binary info matches the pattern
		accuracy = SearchMatch.A_INACCURATE;
	}

	IBinaryMethod[] methods = info.getMethods();
	if (methods != null) {
		for (int i = 0, l = methods.length; i < l; i++) {
			IBinaryMethod method = methods[i];
			if (matchBinary(pattern, method, info)) {
				char[] name;
				if (method.isConstructor()) {
					name = info.getName();
					int lastSlash = CharOperation.lastIndexOf('/', name);
					if (lastSlash != -1) {
						name = CharOperation.subarray(name, lastSlash+1, name.length);
					}
				} else {
					name = method.getSelector();
				}
				String selector = new String(name);
				String[] parameterTypes = CharOperation.toStrings(Signature.getParameterTypes(convertClassFileFormat(method.getMethodDescriptor())));
				IFunction methodHandle = binaryType.getFunction(selector, parameterTypes);
				methodHandle = new ResolvedBinaryMethod(binaryType, selector, parameterTypes, methodHandle.getKey());
				locator.reportBinaryMemberDeclaration(null, methodHandle, null, info, accuracy);
			}
		}
	}

	IBinaryField[] fields = info.getFields();
	if (fields != null) {
		for (int i = 0, l = fields.length; i < l; i++) {
			IBinaryField field = fields[i];
			if (matchBinary(pattern, field, info)) {
				String fieldName = new String(field.getName());
				IField fieldHandle = binaryType.getField(fieldName);
				fieldHandle = new ResolvedBinaryField(binaryType, fieldName, fieldHandle.getKey());
				locator.reportBinaryMemberDeclaration(null, fieldHandle, null, info, accuracy);
			}
		}
	}
}
/**
 * Finds out whether the given binary info matches the search pattern.
 * Default is to return false.
 */
boolean matchBinary(SearchPattern pattern, Object binaryInfo, IBinaryType enclosingBinaryType) {
	switch (((InternalSearchPattern)pattern).kind) {
		case CONSTRUCTOR_PATTERN :
			return matchConstructor((ConstructorPattern) pattern, binaryInfo, enclosingBinaryType);
		case FIELD_PATTERN :
			return matchField((FieldPattern) pattern, binaryInfo, enclosingBinaryType);
		case METHOD_PATTERN :
			return matchMethod((MethodPattern) pattern, binaryInfo, enclosingBinaryType);
		case SUPER_REF_PATTERN :
			return matchSuperTypeReference((SuperTypeReferencePattern) pattern, binaryInfo, enclosingBinaryType);
		case TYPE_DECL_PATTERN :
			return matchTypeDeclaration((TypeDeclarationPattern) pattern, binaryInfo, enclosingBinaryType);
		case OR_PATTERN :
			SearchPattern[] patterns = ((OrPattern) pattern).patterns;
			for (int i = 0, length = patterns.length; i < length; i++)
				if (matchBinary(patterns[i], binaryInfo, enclosingBinaryType)) return true;
	}
	return false;
}
boolean matchConstructor(ConstructorPattern pattern, Object binaryInfo, IBinaryType enclosingBinaryType) {
	if (!pattern.findDeclarations) return false; // only relevant when finding declarations
	if (!(binaryInfo instanceof IBinaryMethod)) return false;

	IBinaryMethod method = (IBinaryMethod) binaryInfo;
	if (!method.isConstructor()) return false;
	if (!checkDeclaringType(enclosingBinaryType, pattern.declaringSimpleName, pattern.declaringQualification, pattern.isCaseSensitive(), pattern.isCamelCase()))
		return false;
	if (pattern.parameterSimpleNames != null) {
		char[] methodDescriptor = convertClassFileFormat(method.getMethodDescriptor());
		if (!checkParameters(methodDescriptor, pattern.parameterSimpleNames, pattern.parameterQualifications, pattern.isCaseSensitive(), pattern.isCamelCase()))
			return false;
	}
	return true;
}
boolean matchField(FieldPattern pattern, Object binaryInfo, IBinaryType enclosingBinaryType) {
	if (!pattern.findDeclarations) return false; // only relevant when finding declarations
	if (!(binaryInfo instanceof IBinaryField)) return false;

	IBinaryField field = (IBinaryField) binaryInfo;
	if (!pattern.matchesName(pattern.name, field.getName())) return false;
	if (!checkDeclaringType(enclosingBinaryType, pattern.getDeclaringSimpleName(), pattern.getDeclaringQualification(), pattern.isCaseSensitive(), pattern.isCamelCase()))
		return false;

	char[] fieldTypeSignature = Signature.toCharArray(convertClassFileFormat(field.getTypeName()));
	return checkTypeName(pattern.typeSimpleName, pattern.typeQualification, fieldTypeSignature, pattern.isCaseSensitive(), pattern.isCamelCase());
}
boolean matchMethod(MethodPattern pattern, Object binaryInfo, IBinaryType enclosingBinaryType) {
	if (!pattern.findDeclarations) return false; // only relevant when finding declarations
	if (!(binaryInfo instanceof IBinaryMethod)) return false;

	IBinaryMethod method = (IBinaryMethod) binaryInfo;
	if (!pattern.matchesName(pattern.selector, method.getSelector())) return false;
	if (!checkDeclaringType(enclosingBinaryType, pattern.getDeclaringSimpleName(), pattern.getDeclaringQualification(), pattern.isCaseSensitive(), pattern.isCamelCase()))
		return false;

	// look at return type only if declaring type is not specified
	boolean checkReturnType = pattern.getDeclaringSimpleName() == null && (pattern.returnSimpleName != null || pattern.returnQualification != null);
	boolean checkParameters = pattern.parameterSimpleNames != null;
	if (checkReturnType || checkParameters) {
		char[] methodDescriptor = convertClassFileFormat(method.getMethodDescriptor());
		if (checkReturnType) {
			char[] returnTypeSignature = Signature.toCharArray(Signature.getReturnType(methodDescriptor));
			if (!checkTypeName(pattern.returnSimpleName, pattern.returnQualification, returnTypeSignature, pattern.isCaseSensitive(), pattern.isCamelCase()))
				return false;
		}
		if (checkParameters &&  !checkParameters(methodDescriptor, pattern.parameterSimpleNames, pattern.parameterQualifications, pattern.isCaseSensitive(), pattern.isCamelCase()))
			return false;
	}
	return true;
}
boolean matchSuperTypeReference(SuperTypeReferencePattern pattern, Object binaryInfo, IBinaryType enclosingBinaryType) {
	if (!(binaryInfo instanceof IBinaryType)) return false;

	IBinaryType type = (IBinaryType) binaryInfo;
	
	char[] vmName = type.getSuperclassName();
	if (vmName != null) {
		char[] superclassName = convertClassFileFormat(vmName);
		if (checkTypeName(pattern.typeName, superclassName, pattern.isCaseSensitive(), pattern.isCamelCase()))
				return true;
	}
	return false;
}
boolean matchTypeDeclaration(TypeDeclarationPattern pattern, Object binaryInfo, IBinaryType enclosingBinaryType) {
	if (!(binaryInfo instanceof IBinaryType)) return false;

	IBinaryType type = (IBinaryType) binaryInfo;
	char[] fullyQualifiedTypeName = convertClassFileFormat(type.getName());
	if (pattern.enclosingTypeNames == null) {
		char[] simpleName = (pattern.getMatchMode() == SearchPattern.R_PREFIX_MATCH)
			? CharOperation.concat(pattern.simpleName, IIndexConstants.ONE_STAR)
			: pattern.simpleName;
		char[] pkg = pattern.qualification;
		if (!checkTypeName(simpleName, pkg, fullyQualifiedTypeName, pattern.isCaseSensitive(), pattern.isCamelCase())) return false;
	} else {
		char[] enclosingTypeName = CharOperation.concatWith(pattern.enclosingTypeNames, '.');
		char[] patternString = pattern.qualification == null
			? enclosingTypeName
			: CharOperation.concat(pattern.qualification, enclosingTypeName, '.');
		if (!checkTypeName(pattern.simpleName, patternString, fullyQualifiedTypeName, pattern.isCaseSensitive(), pattern.isCamelCase())) return false;
	}

	return true;
}
}
