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
package org.eclipse.wst.jsdt.internal.compiler.lookup;

import org.eclipse.wst.jsdt.core.UnimplementedException;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.env.ClassSignature;
import org.eclipse.wst.jsdt.internal.compiler.env.ISourceField;
import org.eclipse.wst.jsdt.internal.compiler.env.ISourceMethod;
import org.eclipse.wst.jsdt.internal.compiler.env.ISourceType;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.wst.jsdt.internal.core.SourceField;
import org.eclipse.wst.jsdt.internal.core.SourceMethod;

/*
Not all fields defined by this type are initialized when it is created.
Some are initialized only when needed.

Accessors have been provided for some public fields so all TypeBindings have the same API...
but access public fields directly whenever possible.
Non-public fields have accessors which should be used everywhere you expect the field to be initialized.

null is NOT a valid value for a non-public field... it just means the field is not initialized.
*/

public class BinaryTypeBinding extends ReferenceBinding {

	// all of these fields are ONLY guaranteed to be initialized if accessed using their public accessor method
	protected ReferenceBinding superclass;
	protected ReferenceBinding enclosingType;
	protected FieldBinding[] fields;
	protected MethodBinding[] methods;
	protected ReferenceBinding[] memberTypes;

	// For the link with the principle structure
	protected LookupEnvironment environment;

static Object convertMemberValue(Object binaryValue, LookupEnvironment env) {
	if (binaryValue == null) return null;
	if (binaryValue instanceof Constant)
		return binaryValue;
	if (binaryValue instanceof ClassSignature)
		return env.getTypeFromSignature(((ClassSignature) binaryValue).getTypeName(), 0, -1, false, null);
	if (binaryValue instanceof Object[]) {
		Object[] objects = (Object[]) binaryValue;
		int length = objects.length;
		if (length == 0) return objects;
		Object[] values = new Object[length];
		for (int i = 0; i < length; i++)
			values[i] = convertMemberValue(objects[i], env);
		return values;
	}

	// should never reach here.
	throw new IllegalStateException();
}
public static ReferenceBinding resolveType(ReferenceBinding type, LookupEnvironment environment, boolean convertGenericToRawType) {
	if (type instanceof UnresolvedReferenceBinding)
		return ((UnresolvedReferenceBinding) type).resolve(environment, convertGenericToRawType);

	return type;
}
public static TypeBinding resolveType(TypeBinding type, LookupEnvironment environment, int rank) {
	switch (type.kind()) {

		case Binding.ARRAY_TYPE :
			resolveType(((ArrayBinding) type).leafComponentType, environment, rank);
			break;

		default:
			if (type instanceof UnresolvedReferenceBinding)
				return ((UnresolvedReferenceBinding) type).resolve(environment, true);
	}
	return type;
}

/**
 * Default empty constructor for subclasses only.
 */
protected BinaryTypeBinding() {
	// only for subclasses
}

/**
 * Standard constructor for creating binary type bindings from binary models (classfiles)
 * @param packageBinding
 * @param binaryType
 * @param environment
 */
public BinaryTypeBinding(PackageBinding packageBinding, ISourceType binaryType, LookupEnvironment environment) {
	this.compoundName = CharOperation.splitOn('/', binaryType.getName());
	this.fileName = binaryType.getFileName();
	computeId();

	this.tagBits |= TagBits.IsBinaryBinding;
	this.environment = environment;
	this.fPackage = packageBinding;

//	char[] typeSignature = environment.globalOptions.sourceLevel >= ClassFileConstants.JDK1_5 ? binaryType.getGenericSignature() : null;
//	this.typeVariables = typeSignature != null && typeSignature.length > 0 && typeSignature[0] == '<'
//		? null // is initialized in cachePartsFrom (called from LookupEnvironment.createBinaryTypeFrom())... must set to null so isGenericType() answers true
//		: Binding.NO_TYPE_VARIABLES;

	this.sourceName = binaryType.getFileName();
	this.modifiers = binaryType.getModifiers();

//	if ((binaryType.getTagBits() & TagBits.HasInconsistentHierarchy) != 0)
//		this.tagBits |= TagBits.HierarchyHasProblems;
//	if (binaryType.isAnonymous()) {
//		this.tagBits |= TagBits.AnonymousTypeMask;
//	} else if (binaryType.isLocal()) {
//		this.tagBits |= TagBits.LocalTypeMask;
//	} else if (binaryType.isMember()) {
//		this.tagBits |= TagBits.MemberTypeMask;
//	}
//	// need enclosing type to access type variables
//	char[] enclosingTypeName = binaryType.getEnclosingTypeName();
//	if (enclosingTypeName != null) {
//		// attempt to find the enclosing type if it exists in the cache (otherwise - resolve it when requested)
//		this.enclosingType = environment.getTypeFromConstantPoolName(enclosingTypeName, 0, -1, true); // pretend parameterized to avoid raw
//		this.tagBits |= TagBits.MemberTypeMask;   // must be a member type not a top-level or local type
//		this.tagBits |= 	TagBits.HasUnresolvedEnclosingType;
//		if (this.enclosingType().isStrictfp())
//			this.modifiers |= ClassFileConstants.AccStrictfp;
//		if (this.enclosingType().isDeprecated())
//			this.modifiers |= ExtraCompilerModifiers.AccDeprecatedImplicitly;
//	}
}

public FieldBinding[] availableFields() {
	if ((this.tagBits & TagBits.AreFieldsComplete) != 0)
		return fields;

	// lazily sort fields
	if ((this.tagBits & TagBits.AreFieldsSorted) == 0) {
		int length = this.fields.length;
		if (length > 1)
			ReferenceBinding.sortFields(this.fields, 0, length);
		this.tagBits |= TagBits.AreFieldsSorted;
	}
	FieldBinding[] availableFields = new FieldBinding[fields.length];
	int count = 0;
	for (int i = 0; i < fields.length; i++) {
		try {
			availableFields[count] = resolveTypeFor(fields[i]);
			count++;
		} catch (AbortCompilation a){
			// silent abort
		}
	}
	if (count < availableFields.length)
		System.arraycopy(availableFields, 0, availableFields = new FieldBinding[count], 0, count);
	return availableFields;
}
public MethodBinding[] availableMethods() {
	if ((this.tagBits & TagBits.AreMethodsComplete) != 0)
		return methods;

	// lazily sort methods
	if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
		int length = this.methods.length;
		if (length > 1)
			ReferenceBinding.sortMethods(this.methods, 0, length);
		this.tagBits |= TagBits.AreMethodsSorted;
	}
	MethodBinding[] availableMethods = new MethodBinding[methods.length];
	int count = 0;
	for (int i = 0; i < methods.length; i++) {
		try {
			availableMethods[count] = resolveTypesFor(methods[i]);
			count++;
		} catch (AbortCompilation a){
			// silent abort
		}
	}
	if (count < availableMethods.length)
		System.arraycopy(availableMethods, 0, availableMethods = new MethodBinding[count], 0, count);
	return availableMethods;
}
void cachePartsFrom(ISourceType binaryType, boolean needFieldsAndMethods) {
	// must retrieve member types in case superclass/interfaces need them
	this.memberTypes = Binding.NO_MEMBER_TYPES;
	ISourceType[] memberTypeStructures = binaryType.getMemberTypes();
	if (memberTypeStructures != null) {
		int size = memberTypeStructures.length;
		if (size > 0) {
			this.memberTypes = new ReferenceBinding[size];
			for (int i = 0; i < size; i++)
				// attempt to find each member type if it exists in the cache (otherwise - resolve it when requested)
				this.memberTypes[i] = environment.getTypeFromConstantPoolName(memberTypeStructures[i].getName(), 0, -1, false);
			this.tagBits |= 	TagBits.HasUnresolvedMemberTypes;
		}
	}

	long sourceLevel = environment.globalOptions.sourceLevel;
	char[] typeSignature = null;
//	if (sourceLevel >= ClassFileConstants.JDK1_5) {
//		typeSignature = binaryType.getGenericSignature();
//		this.tagBits |= binaryType.getTagBits();
//	}
	if (typeSignature == null) {
		char[] superclassName = binaryType.getSuperclassName();
		if (superclassName != null) {
			// attempt to find the superclass if it exists in the cache (otherwise - resolve it when requested)
			this.superclass = environment.getTypeFromConstantPoolName(superclassName, 0, -1, false);
			this.tagBits |= TagBits.HasUnresolvedSuperclass;
		}
	} else {
		// ClassSignature = ParameterPart(optional) super_TypeSignature interface_signature
		SignatureWrapper wrapper = new SignatureWrapper(typeSignature);
		if (wrapper.signature[wrapper.start] == '<') {
			// ParameterPart = '<' ParameterSignature(s) '>'
			wrapper.start++; // skip '<'
			wrapper.start++; // skip '>'
			this.tagBits |=  TagBits.HasUnresolvedTypeVariables;
		}

		// attempt to find the superclass if it exists in the cache (otherwise - resolve it when requested)
		this.superclass = (ReferenceBinding) environment.getTypeFromTypeSignature(wrapper, this);
		this.tagBits |= TagBits.HasUnresolvedSuperclass;
	}

	if (needFieldsAndMethods) {
		createFields(binaryType.getFields(), sourceLevel);
		createMethods(binaryType.getMethods(), sourceLevel);
	} else { // protect against incorrect use of the needFieldsAndMethods flag, see 48459
		this.fields = Binding.NO_FIELDS;
		this.methods = Binding.NO_METHODS;
	}
//	if (this.environment.globalOptions.storeAnnotations)
//		setAnnotations(createAnnotations(binaryType.getAnnotations(), this.environment));
}
private void createFields(ISourceField[] iFields, long sourceLevel) {
	this.fields = Binding.NO_FIELDS;
	if (iFields != null) {
		int size = iFields.length;
		if (size > 0) {
			this.fields = new FieldBinding[size];
//			boolean use15specifics = sourceLevel >= ClassFileConstants.JDK1_5;
			boolean isViewedAsDeprecated = isViewedAsDeprecated();
			boolean hasRestrictedAccess = hasRestrictedAccess();
//			int firstAnnotatedFieldIndex = -1;
			for (int i = 0; i < size; i++) {
				ISourceField binaryField = iFields[i];
				char[] fieldSignature = null;//use15specifics ? binaryField.getGenericSignature() : null;
				TypeBinding type = fieldSignature == null
					? environment.getTypeFromSignature(binaryField.getTypeName(), 0, -1, false, this)
					: environment.getTypeFromTypeSignature(new SignatureWrapper(fieldSignature), this);
				FieldBinding field =
					new FieldBinding(
//						binaryField.getName(),
						((SourceField)binaryField).getElementName ().toCharArray(),
						type,
						binaryField.getModifiers() | ExtraCompilerModifiers.AccUnresolved,
						this);
//				if (firstAnnotatedFieldIndex < 0
//						&& this.environment.globalOptions.storeAnnotations
//						&& binaryField.getAnnotations() != null) {
//					firstAnnotatedFieldIndex = i;
//				}
				field.id = i; // ordinal
//				if (use15specifics)
//					field.tagBits |= binaryField.getTagBits();
				if (isViewedAsDeprecated && !field.isDeprecated())
					field.modifiers |= ExtraCompilerModifiers.AccDeprecatedImplicitly;
				if (hasRestrictedAccess)
					field.modifiers |= ExtraCompilerModifiers.AccRestrictedAccess;
				this.fields[i] = field;
			}
			// second pass for reifying annotations, since may refer to fields being constructed (147875)
//			if (firstAnnotatedFieldIndex >= 0) {
//				for (int i = firstAnnotatedFieldIndex; i <size; i++) {
//					this.fields[i].setAnnotations(createAnnotations(iFields[i].getAnnotations(), this.environment));
//				}
//			}
		}
	}
}
private MethodBinding createMethod(ISourceMethod method, long sourceLevel) {
	int methodModifiers = method.getModifiers() | ExtraCompilerModifiers.AccUnresolved;
	if (sourceLevel < ClassFileConstants.JDK1_5)
		methodModifiers &= ~ClassFileConstants.AccVarargs; // vararg methods are not recognized until 1.5
//	ReferenceBinding[] exceptions = Binding.NO_EXCEPTIONS;
//	TypeBinding[] parameters = Binding.NO_PARAMETERS;
//	TypeVariableBinding[] typeVars = Binding.NO_TYPE_VARIABLES;
//	AnnotationBinding[][] paramAnnotations = null;
//	TypeBinding returnType = null;

	throw new UnimplementedException("fix compile errors for this code"); //$NON-NLS-1$
//	final boolean use15specifics = sourceLevel >= ClassFileConstants.JDK1_5;
//	char[] methodSignature = use15specifics ? method.getGenericSignature() : null;
//	if (methodSignature == null) { // no generics
//		char[] methodDescriptor = method.getMethodDescriptor();   // of the form (I[Ljava/jang/String;)V
//		int numOfParams = 0;
//		char nextChar;
//		int index = 0;   // first character is always '(' so skip it
//		while ((nextChar = methodDescriptor[++index]) != ')') {
//			if (nextChar != '[') {
//				numOfParams++;
//				if (nextChar == 'L')
//					while ((nextChar = methodDescriptor[++index]) != ';'){/*empty*/}
//			}
//		}
//
//		// Ignore synthetic argument for member types.
//		int startIndex = (method.isConstructor() && isMemberType() && !isStatic()) ? 1 : 0;
//		int size = numOfParams - startIndex;
//		if (size > 0) {
//			parameters = new TypeBinding[size];
//			if (this.environment.globalOptions.storeAnnotations)
//				paramAnnotations = new AnnotationBinding[size][];
//			index = 1;
//			int end = 0;   // first character is always '(' so skip it
//			for (int i = 0; i < numOfParams; i++) {
//				while ((nextChar = methodDescriptor[++end]) == '['){/*empty*/}
//				if (nextChar == 'L')
//					while ((nextChar = methodDescriptor[++end]) != ';'){/*empty*/}
//
//				if (i >= startIndex) {   // skip the synthetic arg if necessary
//					parameters[i - startIndex] = environment.getTypeFromSignature(methodDescriptor, index, end, false, this);
//					// 'paramAnnotations' line up with 'parameters'
//					// int parameter to method.getParameterAnnotations() include the synthetic arg
//					if (paramAnnotations != null)
//						paramAnnotations[i - startIndex] = createAnnotations(method.getParameterAnnotations(i), this.environment);
//				}
//				index = end + 1;
//			}
//		}
//
//		char[][] exceptionTypes = method.getExceptionTypeNames();
//		if (exceptionTypes != null) {
//			size = exceptionTypes.length;
//			if (size > 0) {
//				exceptions = new ReferenceBinding[size];
//				for (int i = 0; i < size; i++)
//					exceptions[i] = environment.getTypeFromConstantPoolName(exceptionTypes[i], 0, -1, false);
//			}
//		}
//
//		if (!method.isConstructor())
//			returnType = environment.getTypeFromSignature(methodDescriptor, index + 1, -1, false, this);   // index is currently pointing at the ')'
//	} else {
//		methodModifiers |= ExtraCompilerModifiers.AccGenericSignature;
//		// MethodTypeSignature = ParameterPart(optional) '(' TypeSignatures ')' return_typeSignature ['^' TypeSignature (optional)]
//		SignatureWrapper wrapper = new SignatureWrapper(methodSignature);
//		if (wrapper.signature[wrapper.start] == '<') {
//			// <A::Ljava/lang/annotation/Annotation;>(Ljava/lang/Class<TA;>;)TA;
//			// ParameterPart = '<' ParameterSignature(s) '>'
//			wrapper.start++; // skip '<'
//			typeVars = createTypeVariables(wrapper, false);
//			wrapper.start++; // skip '>'
//		}
//
//		if (wrapper.signature[wrapper.start] == '(') {
//			wrapper.start++; // skip '('
//			if (wrapper.signature[wrapper.start] == ')') {
//				wrapper.start++; // skip ')'
//			} else {
//				java.util.ArrayList types = new java.util.ArrayList(2);
//				while (wrapper.signature[wrapper.start] != ')')
//					types.add(environment.getTypeFromTypeSignature(wrapper, typeVars, this));
//				wrapper.start++; // skip ')'
//				int numParam = types.size();
//				parameters = new TypeBinding[numParam];
//				types.toArray(parameters);
//				if (this.environment.globalOptions.storeAnnotations) {
//					paramAnnotations = new AnnotationBinding[numParam][];
//					for (int i = 0; i < numParam; i++)
//						paramAnnotations[i] = createAnnotations(method.getParameterAnnotations(i), this.environment);
//				}
//			}
//		}
//
//		if (!method.isConstructor())
//			returnType = environment.getTypeFromTypeSignature(wrapper, typeVars, this);
//
//		if (!wrapper.atEnd() && wrapper.signature[wrapper.start] == '^') {
//			// attempt to find each superinterface if it exists in the cache (otherwise - resolve it when requested)
//			java.util.ArrayList types = new java.util.ArrayList(2);
//			do {
//				wrapper.start++; // skip '^'
//				types.add(environment.getTypeFromTypeSignature(wrapper, typeVars, this));
//			} while (!wrapper.atEnd() && wrapper.signature[wrapper.start] == '^');
//			exceptions = new ReferenceBinding[types.size()];
//			types.toArray(exceptions);
//		} else { // get the exceptions the old way
//			char[][] exceptionTypes = method.getExceptionTypeNames();
//			if (exceptionTypes != null) {
//				int size = exceptionTypes.length;
//				if (size > 0) {
//					exceptions = new ReferenceBinding[size];
//					for (int i = 0; i < size; i++)
//						exceptions[i] = environment.getTypeFromConstantPoolName(exceptionTypes[i], 0, -1, false);
//				}
//			}
//		}
//	}
//
//	FunctionBinding result = method.isConstructor()
//		? new FunctionBinding(methodModifiers, parameters, exceptions, this)
//		: new FunctionBinding(methodModifiers, method.getSelector(), returnType, parameters, exceptions, this);
//	if (this.environment.globalOptions.storeAnnotations)
//		result.setAnnotations(
//			createAnnotations(method.getAnnotations(), this.environment),
//			paramAnnotations,
//			isAnnotationType() ? convertMemberValue(method.getDefaultValue(), this.environment) : null);
//
//	if (use15specifics)
//		result.tagBits |= method.getTagBits();
//	result.typeVariables = typeVars;
//	// fixup the declaring element of the type variable
//	for (int i = 0, length = typeVars.length; i < length; i++)
//		typeVars[i].declaringElement = result;
//	return result;
}
/**
 * Create method bindings for binary type, filtering out <clinit> and synthetics
 */
private void createMethods(ISourceMethod[] iMethods, long sourceLevel) {
	int total = 0, initialTotal = 0, iClinit = -1;
	int[] toSkip = null;
	if (iMethods != null) {
		total = initialTotal = iMethods.length;
		boolean keepBridgeMethods = sourceLevel < ClassFileConstants.JDK1_5
			&& this.environment.globalOptions.complianceLevel >= ClassFileConstants.JDK1_5;
		for (int i = total; --i >= 0;) {
			ISourceMethod method = iMethods[i];
			if (iClinit == -1) {
				char[] methodName =((SourceMethod)method).getElementName().toCharArray();
				if (methodName.length == 8 && methodName[0] == '<') {
					// discard <clinit>
					iClinit = i;
					total--;
				}
			}
		}
	}
	if (total == 0) {
		this.methods = Binding.NO_METHODS;
		return;
	}

	boolean isViewedAsDeprecated = isViewedAsDeprecated();
	boolean hasRestrictedAccess = hasRestrictedAccess();
	this.methods = new MethodBinding[total];
	if (total == initialTotal) {
		for (int i = 0; i < initialTotal; i++) {
			MethodBinding method = createMethod(iMethods[i], sourceLevel);
			if (isViewedAsDeprecated && !method.isDeprecated())
				method.modifiers |= ExtraCompilerModifiers.AccDeprecatedImplicitly;
			if (hasRestrictedAccess)
				method.modifiers |= ExtraCompilerModifiers.AccRestrictedAccess;
			this.methods[i] = method;
		}
	} else {
		for (int i = 0, index = 0; i < initialTotal; i++) {
			if (iClinit != i && (toSkip == null || toSkip[i] != -1)) {
				MethodBinding method = createMethod(iMethods[i], sourceLevel);
				if (isViewedAsDeprecated && !method.isDeprecated())
					method.modifiers |= ExtraCompilerModifiers.AccDeprecatedImplicitly;
				if (hasRestrictedAccess)
					method.modifiers |= ExtraCompilerModifiers.AccRestrictedAccess;
				this.methods[index++] = method;
			}
		}
	}
}
/* Answer the receiver's enclosing type... null if the receiver is a top level type.
*
* NOTE: enclosingType of a binary type is resolved when needed
*/
public ReferenceBinding enclosingType() {
	if ((this.tagBits & TagBits.HasUnresolvedEnclosingType) == 0)
		return this.enclosingType;

	// finish resolving the type
	this.enclosingType = resolveType(this.enclosingType, this.environment, false);
	this.tagBits &= ~TagBits.HasUnresolvedEnclosingType;
	return this.enclosingType;
}
// NOTE: the type of each field of a binary type is resolved when needed
public FieldBinding[] fields() {
	if ((this.tagBits & TagBits.AreFieldsComplete) != 0)
		return fields;

	// lazily sort fields
	if ((this.tagBits & TagBits.AreFieldsSorted) == 0) {
		int length = this.fields.length;
		if (length > 1)
			ReferenceBinding.sortFields(this.fields, 0, length);
		this.tagBits |= TagBits.AreFieldsSorted;
	}
	for (int i = fields.length; --i >= 0;)
		resolveTypeFor(fields[i]);
	this.tagBits |= TagBits.AreFieldsComplete;
	return fields;
}
//NOTE: the return type, arg & exception types of each method of a binary type are resolved when needed
public MethodBinding getExactConstructor(TypeBinding[] argumentTypes) {

	// lazily sort methods
	if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
		int length = this.methods.length;
		if (length > 1)
			ReferenceBinding.sortMethods(this.methods, 0, length);
		this.tagBits |= TagBits.AreMethodsSorted;
	}
	int argCount = argumentTypes.length;
	long range;
	if ((range = ReferenceBinding.binarySearch(TypeConstants.INIT, this.methods)) >= 0) {
		nextMethod: for (int imethod = (int)range, end = (int)(range >> 32); imethod <= end; imethod++) {
			MethodBinding method = methods[imethod];
			if (method.parameters.length == argCount) {
				resolveTypesFor(method);
				TypeBinding[] toMatch = method.parameters;
				for (int iarg = 0; iarg < argCount; iarg++)
					if (toMatch[iarg] != argumentTypes[iarg])
						continue nextMethod;
				return method;
			}
		}
	}
	return null;
}

//NOTE: the return type, arg & exception types of each method of a binary type are resolved when needed
//searches up the hierarchy as long as no potential (but not exact) match was found.
public MethodBinding getExactMethod(char[] selector, TypeBinding[] argumentTypes, CompilationUnitScope refScope) {
	// sender from refScope calls recordTypeReference(this)

	// lazily sort methods
	if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
		int length = this.methods.length;
		if (length > 1)
			ReferenceBinding.sortMethods(this.methods, 0, length);
		this.tagBits |= TagBits.AreMethodsSorted;
	}

	int argCount = argumentTypes.length;
	boolean foundNothing = true;

	long range;
	if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
		nextMethod: for (int imethod = (int)range, end = (int)(range >> 32); imethod <= end; imethod++) {
			MethodBinding method = methods[imethod];
			foundNothing = false; // inner type lookups must know that a method with this name exists
			if (method.parameters.length == argCount) {
				resolveTypesFor(method);
				TypeBinding[] toMatch = method.parameters;
				for (int iarg = 0; iarg < argCount; iarg++)
					if (toMatch[iarg] != argumentTypes[iarg])
						continue nextMethod;
				return method;
			}
		}
	}
	if (foundNothing) {
		if (getSuperBinding() != null) { // ensure superclass is resolved before checking
			if (refScope != null)
				refScope.recordTypeReference(superclass);
			return superclass.getExactMethod(selector, argumentTypes, refScope);
		}
	}
	return null;
}
//NOTE: the type of a field of a binary type is resolved when needed
public FieldBinding getField(char[] fieldName, boolean needResolve) {
	// lazily sort fields
	if ((this.tagBits & TagBits.AreFieldsSorted) == 0) {
		int length = this.fields.length;
		if (length > 1)
			ReferenceBinding.sortFields(this.fields, 0, length);
		this.tagBits |= TagBits.AreFieldsSorted;
	}
	FieldBinding field = ReferenceBinding.binarySearch(fieldName, this.fields);
	return needResolve && field != null ? resolveTypeFor(field) : field;
}
/**
 *  Rewrite of default getMemberType to avoid resolving eagerly all member types when one is requested
 */
public ReferenceBinding getMemberType(char[] typeName) {
	for (int i = this.memberTypes.length; --i >= 0;) {
	    ReferenceBinding memberType = this.memberTypes[i];
	    if (memberType instanceof UnresolvedReferenceBinding) {
			char[] name = memberType.sourceName; // source name is qualified with enclosing type name
			int prefixLength = this.compoundName[this.compoundName.length - 1].length + 1; // enclosing$
			if (name.length == (prefixLength + typeName.length)) // enclosing $ typeName
				if (CharOperation.fragmentEquals(typeName, name, prefixLength, true)) // only check trailing portion
					return this.memberTypes[i] = resolveType(memberType, this.environment, false); // no raw conversion for now
	    } else if (CharOperation.equals(typeName, memberType.sourceName)) {
	        return memberType;
	    }
	}
	return null;
}
// NOTE: the return type, arg & exception types of each method of a binary type are resolved when needed
public MethodBinding[] getMethods(char[] selector) {
	if ((this.tagBits & TagBits.AreMethodsComplete) != 0) {
		long range;
		if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
			int start = (int) range, end = (int) (range >> 32);
			int length = end - start + 1;
			if ((this.tagBits & TagBits.AreMethodsComplete) != 0) {
				// simply clone method subset
				MethodBinding[] result;
				System.arraycopy(this.methods, start, result = new MethodBinding[length], 0, length);
				return result;
			}
		}
		return Binding.NO_METHODS;
	}
	// lazily sort methods
	if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
		int length = this.methods.length;
		if (length > 1)
			ReferenceBinding.sortMethods(this.methods, 0, length);
		this.tagBits |= TagBits.AreMethodsSorted;
	}
	long range;
	if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
		int start = (int) range, end = (int) (range >> 32);
		int length = end - start + 1;
		MethodBinding[] result = new MethodBinding[length];
		// iterate methods to resolve them
		for (int i = start, index = 0; i <= end; i++, index++)
			result[index] = resolveTypesFor(methods[i]);
		return result;
	}
	return Binding.NO_METHODS;
}
public boolean hasMemberTypes() {
    return this.memberTypes.length > 0;
}
/**
 * Returns true if a type is identical to another one,
 * or for generic types, true if compared to its raw type.
 */
public boolean isEquivalentTo(TypeBinding otherType) {
	if (this == otherType) return true;
	if (otherType == null) return false;
	return false;
}
public int kind() {
	return Binding.TYPE;
}
// NOTE: member types of binary types are resolved when needed
public ReferenceBinding[] memberTypes() {
 	if ((this.tagBits & TagBits.HasUnresolvedMemberTypes) == 0)
		return this.memberTypes;

	for (int i = this.memberTypes.length; --i >= 0;)
		this.memberTypes[i] = resolveType(this.memberTypes[i], this.environment, false); // no raw conversion for now
	this.tagBits &= ~TagBits.HasUnresolvedMemberTypes;
	return this.memberTypes;
}
// NOTE: the return type, arg & exception types of each method of a binary type are resolved when needed
public MethodBinding[] methods() {
	if ((this.tagBits & TagBits.AreMethodsComplete) != 0)
		return methods;

	// lazily sort methods
	if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
		int length = this.methods.length;
		if (length > 1)
			ReferenceBinding.sortMethods(this.methods, 0, length);
		this.tagBits |= TagBits.AreMethodsSorted;
	}
	for (int i = methods.length; --i >= 0;)
		resolveTypesFor(methods[i]);
	this.tagBits |= TagBits.AreMethodsComplete;
	return methods;
}
private FieldBinding resolveTypeFor(FieldBinding field) {
	if ((field.modifiers & ExtraCompilerModifiers.AccUnresolved) == 0)
		return field;

	field.type = resolveType(field.type, this.environment, 0);
	field.modifiers &= ~ExtraCompilerModifiers.AccUnresolved;
	return field;
}
MethodBinding resolveTypesFor(MethodBinding method) {
	if ((method.modifiers & ExtraCompilerModifiers.AccUnresolved) == 0)
		return method;

	if (!method.isConstructor())
		method.returnType = resolveType(method.returnType, this.environment, 0);
	for (int i = method.parameters.length; --i >= 0;)
		method.parameters[i] = resolveType(method.parameters[i], this.environment, 0);
	method.modifiers &= ~ExtraCompilerModifiers.AccUnresolved;
	return method;
}

/* Answer the receiver's superclass... null if the receiver is Object or an interface.
*
* NOTE: superclass of a binary type is resolved when needed
*/
public ReferenceBinding getSuperBinding() {
	if ((this.tagBits & TagBits.HasUnresolvedSuperclass) == 0)
		return this.superclass;

	// finish resolving the type
	this.superclass = resolveType(this.superclass, this.environment, true);
	this.tagBits &= ~TagBits.HasUnresolvedSuperclass;
	if (this.superclass == null || this.superclass.problemId() == ProblemReasons.NotFound)
		this.tagBits |= TagBits.HierarchyHasProblems; // propagate type inconsistency
	return this.superclass;
}

public String toString() {
	StringBuffer buffer = new StringBuffer();

	if (isDeprecated()) buffer.append("deprecated "); //$NON-NLS-1$
	if (isPublic()) buffer.append("public "); //$NON-NLS-1$
	if (isPrivate()) buffer.append("private "); //$NON-NLS-1$
	if (isStatic() && isNestedType()) buffer.append("static "); //$NON-NLS-1$

	if (isClass()) buffer.append("class "); //$NON-NLS-1$
	else buffer.append("interface "); //$NON-NLS-1$
	buffer.append((compoundName != null) ? CharOperation.toString(compoundName) : "UNNAMED TYPE"); //$NON-NLS-1$

	buffer.append("\n\textends "); //$NON-NLS-1$
	buffer.append((superclass != null) ? superclass.debugName() : "NULL TYPE"); //$NON-NLS-1$

	if (enclosingType != null) {
		buffer.append("\n\tenclosing type : "); //$NON-NLS-1$
		buffer.append(enclosingType.debugName());
	}

	if (fields != null) {
		if (fields != Binding.NO_FIELDS) {
			buffer.append("\n/*   fields   */"); //$NON-NLS-1$
			for (int i = 0, length = fields.length; i < length; i++)
				buffer.append((fields[i] != null) ? "\n" + fields[i].toString() : "\nNULL FIELD"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	} else {
		buffer.append("NULL FIELDS"); //$NON-NLS-1$
	}

	if (methods != null) {
		if (methods != Binding.NO_METHODS) {
			buffer.append("\n/*   methods   */"); //$NON-NLS-1$
			for (int i = 0, length = methods.length; i < length; i++)
				buffer.append((methods[i] != null) ? "\n" + methods[i].toString() : "\nNULL METHOD"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	} else {
		buffer.append("NULL METHODS"); //$NON-NLS-1$
	}

	if (memberTypes != null) {
		if (memberTypes != Binding.NO_MEMBER_TYPES) {
			buffer.append("\n/*   members   */"); //$NON-NLS-1$
			for (int i = 0, length = memberTypes.length; i < length; i++)
				buffer.append((memberTypes[i] != null) ? "\n" + memberTypes[i].toString() : "\nNULL TYPE"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	} else {
		buffer.append("NULL MEMBER TYPES"); //$NON-NLS-1$
	}

	buffer.append("\n\n\n"); //$NON-NLS-1$
	return buffer.toString();
}
MethodBinding[] unResolvedMethods() { // for the MethodVerifier so it doesn't resolve types
	return methods;
}
}
