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
package org.eclipse.wst.jsdt.internal.compiler.parser;

/**
 * Converter from source element type to parsed compilation unit.
 *
 * Limitation:
 * | The source element field does not carry any information for its constant part, thus
 * | the converted parse tree will not include any field initializations.
 * | Therefore, any binary produced by compiling against converted source elements will
 * | not take advantage of remote field constant inlining.
 * | Given the intended purpose of the conversion is to resolve references, this is not
 * | a problem.
 *
 */

import java.util.ArrayList;

import org.eclipse.wst.jsdt.core.IImportDeclaration;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Argument;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Block;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Initializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Statement;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.env.ISourceImport;
import org.eclipse.wst.jsdt.internal.compiler.env.ISourceType;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.wst.jsdt.internal.core.ImportDeclaration;
import org.eclipse.wst.jsdt.internal.core.InitializerElementInfo;
import org.eclipse.wst.jsdt.internal.core.PackageFragment;
import org.eclipse.wst.jsdt.internal.core.SourceField;
import org.eclipse.wst.jsdt.internal.core.SourceFieldElementInfo;
import org.eclipse.wst.jsdt.internal.core.SourceMethod;
import org.eclipse.wst.jsdt.internal.core.SourceMethodElementInfo;
import org.eclipse.wst.jsdt.internal.core.SourceType;
import org.eclipse.wst.jsdt.internal.core.SourceTypeElementInfo;
import org.eclipse.wst.jsdt.internal.core.util.Util;

public class SourceTypeConverter {

	/*
	 * Exception thrown while converting an anonymous type of a member type
	 * in this case, we must parse the source as the enclosing instance cannot be recreated
	 * from the model
	 */
	static class AnonymousMemberFound extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	public static final int FIELD = 0x01;
	public static final int CONSTRUCTOR = 0x02;
	public static final int METHOD = 0x04;
	public static final int MEMBER_TYPE = 0x08;
	public static final int FIELD_INITIALIZATION = 0x10;
	public static final int FIELD_AND_METHOD = FIELD | CONSTRUCTOR | METHOD;
	public static final int LOCAL_TYPE = 0x20;
	public static final int NONE = 0;

	private int flags;
	private CompilationUnitDeclaration unit;
	private Parser parser;
	private ProblemReporter problemReporter;
	private ICompilationUnit cu;
	private char[] source;
	private boolean has1_5Compliance;

	int namePos;

	private SourceTypeConverter(int flags, ProblemReporter problemReporter) {
		this.flags = flags;
		this.problemReporter = problemReporter;
		this.has1_5Compliance = problemReporter.options.complianceLevel >= ClassFileConstants.JDK1_5;
	}

	/*
	 * Convert a set of source element types into a parsed compilation unit declaration
	 * The argument types are then all grouped in the same unit. The argument types must
	 * at least contain one type.
	 * Can optionally ignore fields & methods or member types or field initialization
	 */
	public static CompilationUnitDeclaration buildCompilationUnit(
		ISourceType[] sourceTypes,
		int flags,
		ProblemReporter problemReporter,
		CompilationResult compilationResult) {

//		long start = System.currentTimeMillis();
		SourceTypeConverter converter = new SourceTypeConverter(flags, problemReporter);
		try {
			return converter.convert(sourceTypes, compilationResult);
		} catch (JavaScriptModelException e) {
			return null;
/*		} finally {
			System.out.println("Spent " + (System.currentTimeMillis() - start) + "ms to convert " + ((JavaElement) converter.cu).toStringWithAncestors());
*/		}
	}

	/*
	 * Convert a set of source element types into a parsed compilation unit declaration
	 * The argument types are then all grouped in the same unit. The argument types must
	 * at least contain one type.
	 */
	private CompilationUnitDeclaration convert(ISourceType[] sourceTypes, CompilationResult compilationResult) throws JavaScriptModelException {
		this.unit = new CompilationUnitDeclaration(this.problemReporter, compilationResult, 0);
		// not filled at this point

		if (sourceTypes.length == 0) return this.unit;
		SourceTypeElementInfo topLevelTypeInfo = (SourceTypeElementInfo) sourceTypes[0];
		org.eclipse.wst.jsdt.core.IJavaScriptUnit cuHandle = topLevelTypeInfo.getHandle().getJavaScriptUnit();
		this.cu = (ICompilationUnit) cuHandle;

		if (true){//this.has1_5Compliance && this.annotationPositions != null && this.annotationPositions.size() > 10) { // experimental value
			// if more than 10 annotations, diet parse as this is faster
			return new Parser(this.problemReporter, true).dietParse(this.cu, compilationResult);
		}

		/* only positions available */
		int start = topLevelTypeInfo.getNameSourceStart();
		int end = topLevelTypeInfo.getNameSourceEnd();

		/* convert package and imports */
		String[] packageName = ((PackageFragment) cuHandle.getParent()).names;
		if (packageName.length > 0)
			// if its null then it is defined in the default package
			this.unit.currentPackage =
				createImportReference(packageName, start, end, false);
		IImportDeclaration[] importDeclarations = topLevelTypeInfo.getHandle().getJavaScriptUnit().getImports();
		int importCount = importDeclarations.length;
		this.unit.imports = new ImportReference[importCount];
		for (int i = 0; i < importCount; i++) {
			ImportDeclaration importDeclaration = (ImportDeclaration) importDeclarations[i];
			ISourceImport sourceImport = (ISourceImport) importDeclaration.getElementInfo();
			String nameWithoutStar = importDeclaration.getNameWithoutStar();
			this.unit.imports[i] = createImportReference(
				Util.splitOn('.', nameWithoutStar, 0, nameWithoutStar.length()),
				sourceImport.getDeclarationSourceStart(),
				sourceImport.getDeclarationSourceEnd(),
				importDeclaration.isOnDemand());
		}
		/* convert type(s) */
		try {
			int typeCount = sourceTypes.length;
			final TypeDeclaration[] types = new TypeDeclaration[typeCount];
			/*
			 * We used a temporary types collection to prevent this.unit.types from being null during a call to
			 * convert(...) when the source is syntactically incorrect and the parser is flushing the unit's types.
			 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=97466
			 */
			for (int i = 0; i < typeCount; i++) {
				SourceTypeElementInfo typeInfo = (SourceTypeElementInfo) sourceTypes[i];
				types[i] = convert((SourceType) typeInfo.getHandle(), compilationResult);
			}
			this.unit.types = types;
			return this.unit;
		} catch (AnonymousMemberFound e) {
			return new Parser(this.problemReporter, true).parse(this.cu, compilationResult);
		}
	}

	private void addIdentifiers(String typeSignature, int start, int endExclusive, int identCount, ArrayList fragments) {
		if (identCount == 1) {
			char[] identifier;
			typeSignature.getChars(start, endExclusive, identifier = new char[endExclusive-start], 0);
			fragments.add(identifier);
		} else
			fragments.add(extractIdentifiers(typeSignature, start, endExclusive-1, identCount));
	}

	/*
	 * Convert an initializerinfo into a parsed initializer declaration
	 */
	private Initializer convert(InitializerElementInfo initializerInfo, CompilationResult compilationResult) throws JavaScriptModelException {

		Block block = new Block(0);
		Initializer initializer = new Initializer(block, ClassFileConstants.AccDefault);

		int start = initializerInfo.getDeclarationSourceStart();
		int end = initializerInfo.getDeclarationSourceEnd();

		initializer.sourceStart = initializer.declarationSourceStart = start;
		initializer.sourceEnd = initializer.declarationSourceEnd = end;
		initializer.modifiers = initializerInfo.getModifiers();

		/* convert local and anonymous types */
		IJavaScriptElement[] children = initializerInfo.getChildren();
		int typesLength = children.length;
		if (typesLength > 0) {
			Statement[] statements = new Statement[typesLength];
			for (int i = 0; i < typesLength; i++) {
				SourceType type = (SourceType) children[i];
				TypeDeclaration localType = convert(type, compilationResult);
				if ((localType.bits & ASTNode.IsAnonymousType) != 0) {
					QualifiedAllocationExpression expression = new QualifiedAllocationExpression(localType);
					expression.type = localType.superclass;
					localType.superclass = null;
					localType.allocation = expression;
					statements[i] = expression;
				} else {
					statements[i] = localType;
				}
			}
			block.statements = statements;
		}

		return initializer;
	}

	/*
	 * Convert a field source element into a parsed field declaration
	 */
	private FieldDeclaration convert(SourceField fieldHandle, TypeDeclaration type, CompilationResult compilationResult) throws JavaScriptModelException {

		SourceFieldElementInfo fieldInfo = (SourceFieldElementInfo) fieldHandle.getElementInfo();
		FieldDeclaration field = new FieldDeclaration();

		int start = fieldInfo.getNameSourceStart();
		int end = fieldInfo.getNameSourceEnd();

		field.name = fieldHandle.getElementName().toCharArray();
		field.sourceStart = start;
		field.sourceEnd = end;
		field.declarationSourceStart = fieldInfo.getDeclarationSourceStart();
		field.declarationSourceEnd = fieldInfo.getDeclarationSourceEnd();
		int modifiers = fieldInfo.getModifiers();

		field.modifiers = modifiers;
		field.type = createTypeReference(fieldInfo.getTypeName(), start, end);
		
		/* conversion of field constant */
		if ((this.flags & FIELD_INITIALIZATION) != 0) {
			char[] initializationSource = fieldInfo.getInitializationSource();
			if (initializationSource != null) {
				if (this.parser == null) {
					this.parser = new Parser(this.problemReporter, true);
				}
				this.parser.parse(field, type, this.unit, initializationSource);
			}
		}

		/* conversion of local and anonymous types */
		if ((this.flags & LOCAL_TYPE) != 0) {
			IJavaScriptElement[] children = fieldInfo.getChildren();
			int childrenLength = children.length;
			if (childrenLength == 1) {
				field.initialization = convert(children[0], null, compilationResult);
			} else if (childrenLength > 1) {
				ArrayInitializer initializer = new ArrayInitializer();
				field.initialization = initializer;
				Expression[] expressions = new Expression[childrenLength];
				initializer.expressions = expressions;
				for (int i = 0; i < childrenLength; i++) {
					expressions[i] = convert(children[i], null, compilationResult);
				}
			}
		}
		return field;
	}

	private QualifiedAllocationExpression convert(IJavaScriptElement localType, FieldDeclaration enumConstant, CompilationResult compilationResult) throws JavaScriptModelException {
		TypeDeclaration anonymousLocalTypeDeclaration = convert((SourceType) localType, compilationResult);
		QualifiedAllocationExpression expression = new QualifiedAllocationExpression(anonymousLocalTypeDeclaration);
		expression.type = anonymousLocalTypeDeclaration.superclass;
		anonymousLocalTypeDeclaration.superclass = null;
		anonymousLocalTypeDeclaration.allocation = expression;
		if (enumConstant != null) {
			expression.type = null;
		}
		return expression;
	}

	/*
	 * Convert a method source element into a parsed method/constructor declaration
	 */
	private AbstractMethodDeclaration convert(SourceMethod methodHandle, SourceMethodElementInfo methodInfo, CompilationResult compilationResult) throws JavaScriptModelException {
		AbstractMethodDeclaration method;

		/* only source positions available */
		int start = methodInfo.getNameSourceStart();
		int end = methodInfo.getNameSourceEnd();

		int modifiers = methodInfo.getModifiers();
		if (methodInfo.isConstructor()) {
			ConstructorDeclaration decl = new ConstructorDeclaration(compilationResult);
			decl.isDefaultConstructor = false;
			method = decl;
		} else {
			MethodDeclaration decl = new MethodDeclaration(compilationResult);
			
			// convert return type
			decl.returnType = createTypeReference(methodInfo.getReturnTypeName(), start, end);

			method = decl;
		}
		method.setSelector(methodHandle.getElementName().toCharArray());
		boolean isVarargs = (modifiers & ClassFileConstants.AccVarargs) != 0;
		method.modifiers = modifiers & ~ClassFileConstants.AccVarargs;
		method.sourceStart = start;
		method.sourceEnd = end;
		method.declarationSourceStart = methodInfo.getDeclarationSourceStart();
		method.declarationSourceEnd = methodInfo.getDeclarationSourceEnd();

		/* convert arguments */
		String[] argumentTypeSignatures = methodHandle.getParameterTypes();
		char[][] argumentNames = methodInfo.getArgumentNames();
		int argumentCount = argumentTypeSignatures == null ? 0 : argumentTypeSignatures.length;
		if (argumentCount > 0) {
			long position = ((long) start << 32) + end;
			method.arguments = new Argument[argumentCount];
			for (int i = 0; i < argumentCount; i++) {
				TypeReference typeReference = createTypeReference(argumentTypeSignatures[i], start, end);
				if (isVarargs && i == argumentCount-1) {
					typeReference.bits |= ASTNode.IsVarArgs;
				}
				method.arguments[i] =
					new Argument(
						argumentNames[i],
						position,
						typeReference,
						ClassFileConstants.AccDefault);
				// do not care whether was final or not
			}
		}

		/* convert local and anonymous types */
		if ((this.flags & LOCAL_TYPE) != 0) {
			IJavaScriptElement[] children = methodInfo.getChildren();
			int typesLength = children.length;
			if (typesLength != 0) {
				Statement[] statements = new Statement[typesLength];
				for (int i = 0; i < typesLength; i++) {
					SourceType type = (SourceType) children[i];
					TypeDeclaration localType = convert(type, compilationResult);
					if ((localType.bits & ASTNode.IsAnonymousType) != 0) {
						QualifiedAllocationExpression expression = new QualifiedAllocationExpression(localType);
						expression.type = localType.superclass;
						localType.superclass = null;
						localType.allocation = expression;
						statements[i] = expression;
					} else {
						statements[i] = localType;
					}
				}
				method.statements = statements;
			}
		}

		return method;
	}

	/*
	 * Convert a source element type into a parsed type declaration
	 */
	private TypeDeclaration convert(SourceType typeHandle, CompilationResult compilationResult) throws JavaScriptModelException {
		SourceTypeElementInfo typeInfo = (SourceTypeElementInfo) typeHandle.getElementInfo();
		if (typeInfo.isAnonymousMember())
			throw new AnonymousMemberFound();
		/* create type declaration - can be member type */
		TypeDeclaration type = new TypeDeclaration(compilationResult);
		if (typeInfo.getEnclosingType() == null) {
			if (typeHandle.isAnonymous()) {
				type.name = CharOperation.NO_CHAR;
				type.bits |= (ASTNode.IsAnonymousType|ASTNode.IsLocalType);
			} else {
				if (typeHandle.isLocal()) {
					type.bits |= ASTNode.IsLocalType;
				}
			}
		}  else {
			type.bits |= ASTNode.IsMemberType;
		}
		if ((type.bits & ASTNode.IsAnonymousType) == 0) {
			type.name = typeInfo.getName();
		}
		type.name = typeInfo.getName();
		int start, end; // only positions available
		type.sourceStart = start = typeInfo.getNameSourceStart();
		type.sourceEnd = end = typeInfo.getNameSourceEnd();
		type.modifiers = typeInfo.getModifiers();
		type.declarationSourceStart = typeInfo.getDeclarationSourceStart();
		type.declarationSourceEnd = typeInfo.getDeclarationSourceEnd();
		type.bodyEnd = type.declarationSourceEnd;

		/* set superclass and superinterfaces */
		if (typeInfo.getSuperclassName() != null) {
			type.superclass = createTypeReference(typeInfo.getSuperclassName(), start, end);
			type.superclass.bits |= ASTNode.IsSuperType;
		}

		/* convert member types */
		if ((this.flags & MEMBER_TYPE) != 0) {
			SourceType[] sourceMemberTypes = typeInfo.getMemberTypeHandles();
			int sourceMemberTypeCount = sourceMemberTypes.length;
			type.memberTypes = new TypeDeclaration[sourceMemberTypeCount];
			for (int i = 0; i < sourceMemberTypeCount; i++) {
				type.memberTypes[i] = convert(sourceMemberTypes[i], compilationResult);
			}
		}

		/* convert intializers and fields*/
		InitializerElementInfo[] initializers = null;
		int initializerCount = 0;
		if ((this.flags & LOCAL_TYPE) != 0) {
			initializers = typeInfo.getInitializers();
			initializerCount = initializers.length;
		}
		SourceField[] sourceFields = null;
		int sourceFieldCount = 0;
		if ((this.flags & FIELD) != 0) {
			sourceFields = typeInfo.getFieldHandles();
			sourceFieldCount = sourceFields.length;
		}
		int length = initializerCount + sourceFieldCount;
		if (length > 0) {
			type.fields = new FieldDeclaration[length];
			for (int i = 0; i < initializerCount; i++) {
				type.fields[i] = convert(initializers[i], compilationResult);
			}
			int index = 0;
			for (int i = initializerCount; i < length; i++) {
				type.fields[i] = convert(sourceFields[index++], type, compilationResult);
			}
		}

		/* convert methods - need to add default constructor if necessary */
		boolean needConstructor = (this.flags & CONSTRUCTOR) != 0;
		boolean needMethod = (this.flags & METHOD) != 0;
		if (needConstructor || needMethod) {

			SourceMethod[] sourceMethods = typeInfo.getMethodHandles();
			int sourceMethodCount = sourceMethods.length;

			/* source type has a constructor ?           */
			/* by default, we assume that one is needed. */
			int extraConstructor = 0;
			int methodCount = 0;
			int kind = TypeDeclaration.kind(type.modifiers);

			extraConstructor = needConstructor ? 1 : 0;
			for (int i = 0; i < sourceMethodCount; i++) {
				if (sourceMethods[i].isConstructor()) {
					if (needConstructor) {
						extraConstructor = 0; // Does not need the extra constructor since one constructor already exists.
						methodCount++;
					}
				} else if (needMethod) {
					methodCount++;
				}
			}
			
			type.methods = new AbstractMethodDeclaration[methodCount + extraConstructor];
			if (extraConstructor != 0) { // add default constructor in first position
				type.methods[0] = type.createDefaultConstructor(false, false);
			}
			int index = 0;
			boolean hasAbstractMethods = false;
			for (int i = 0; i < sourceMethodCount; i++) {
				SourceMethod sourceMethod = sourceMethods[i];
				SourceMethodElementInfo methodInfo = (SourceMethodElementInfo)sourceMethod.getElementInfo();
				boolean isConstructor = methodInfo.isConstructor();
				if ((methodInfo.getModifiers() & ClassFileConstants.AccAbstract) != 0) {
					hasAbstractMethods = true;
				}
				if ((isConstructor && needConstructor) || (!isConstructor && needMethod)) {
					AbstractMethodDeclaration method = convert(sourceMethod, methodInfo, compilationResult);
					if (method.isAbstract()) { // fix-up flag
						method.modifiers |= ExtraCompilerModifiers.AccSemicolonBody;
					}
					type.methods[extraConstructor + index++] = method;
				}
			}
			if (hasAbstractMethods) type.bits |= ASTNode.HasAbstractMethods;
		}

		return type;
	}

	/*
	 * Build an import reference from an import name, e.g. java.lang.*
	 */
	private ImportReference createImportReference(
		String[] importName,
		int start,
		int end,
		boolean onDemand) {

		int length = importName.length;
		long[] positions = new long[length];
		long position = ((long) start << 32) + end;
		char[][] qImportName = new char[length][];
		for (int i = 0; i < length; i++) {
			qImportName[i] = importName[i].toCharArray();
			positions[i] = position; // dummy positions
		}
		return new ImportReference(
			qImportName,
			positions,
			onDemand);
	}

	/*
	 * Build a type reference from a readable name, e.g. java.lang.Object[][]
	 */
	private TypeReference createTypeReference(
		char[] typeName,
		int start,
		int end) {

		int length = typeName.length;
		this.namePos = 0;
		return decodeType(typeName, length, start, end);
	}

	/*
	 * Build a type reference from a type signature, e.g. Ljava.lang.Object;
	 */
	private TypeReference createTypeReference(
			String typeSignature,
			int start,
			int end) {

		int length = typeSignature.length();
		this.namePos = 0;
		return decodeType(typeSignature, length, start, end);
	}

	private TypeReference decodeType(String typeSignature, int length, int start, int end) {
		int identCount = 1;
		int dim = 0;
		int nameFragmentStart = this.namePos, nameFragmentEnd = -1;
		boolean nameStarted = false;
		ArrayList fragments = null;
		typeLoop: while (this.namePos < length) {
			char currentChar = typeSignature.charAt(this.namePos);
			switch (currentChar) {
				case Signature.C_VOID :
					if (!nameStarted) {
						this.namePos++;
						new SingleTypeReference(TypeBinding.VOID.simpleName, ((long) start << 32) + end);
					}
					break;
				case Signature.C_RESOLVED :
				case Signature.C_UNRESOLVED :
					if (!nameStarted) {
						nameFragmentStart = this.namePos+1;
						nameStarted = true;
					}
					break;
				case Signature.C_ARRAY :
					dim++;
					break;
				case Signature.C_SEMICOLON :
					nameFragmentEnd = this.namePos-1;
					this.namePos++;
					break typeLoop;
				case Signature.C_DOT :
				case Signature.C_DOLLAR:
					if (!nameStarted) {
						nameFragmentStart = this.namePos+1;
						nameStarted = true;
					} else if (this.namePos > nameFragmentStart) // handle name starting with a $ (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=91709)
						identCount ++;
					break;
			}
			this.namePos++;
		}
		if (fragments == null) { // non parameterized
			/* rebuild identifiers and dimensions */
			if (identCount == 1) { // simple type reference
				if (dim == 0) {
					char[] nameFragment = new char[nameFragmentEnd - nameFragmentStart + 1];
					typeSignature.getChars(nameFragmentStart, nameFragmentEnd +1, nameFragment, 0);
					return new SingleTypeReference(nameFragment, ((long) start << 32) + end);
				} else {
					char[] nameFragment = new char[nameFragmentEnd - nameFragmentStart + 1];
					typeSignature.getChars(nameFragmentStart, nameFragmentEnd +1, nameFragment, 0);
					return new ArrayTypeReference(nameFragment, dim, ((long) start << 32) + end);
				}
			} else { // qualified type reference
				long[] positions = new long[identCount];
				long pos = ((long) start << 32) + end;
				for (int i = 0; i < identCount; i++) {
					positions[i] = pos;
				}
				char[][] identifiers = extractIdentifiers(typeSignature, nameFragmentStart, nameFragmentEnd, identCount);
				if (dim == 0) {
					return new QualifiedTypeReference(identifiers, positions);
				} else {
					return new ArrayQualifiedTypeReference(identifiers, dim, positions);
				}
			}
		} else { // parameterized
			// rebuild type reference from available fragments: char[][], arguments, char[][], arguments...
			// check trailing qualified name
			if (nameStarted) {
				addIdentifiers(typeSignature, nameFragmentStart, nameFragmentEnd + 1, identCount, fragments);
			}
			int fragmentLength = fragments.size();
			if (fragmentLength == 2) {
				Object firstFragment = fragments.get(0);
				if (firstFragment instanceof char[]) {
					// parameterized single type
					return null;
				}
			}
			// parameterized qualified type
			identCount = 0;
			for (int i = 0; i < fragmentLength; i ++) {
				Object element = fragments.get(i);
				if (element instanceof char[][]) {
					identCount += ((char[][])element).length;
				} else if (element instanceof char[])
					identCount++;
			}
			char[][] tokens = new char[identCount][];
			TypeReference[][] arguments = new TypeReference[identCount][];
			int index = 0;
			for (int i = 0; i < fragmentLength; i ++) {
				Object element = fragments.get(i);
				if (element instanceof char[][]) {
					char[][] fragmentTokens = (char[][]) element;
					int fragmentTokenLength = fragmentTokens.length;
					System.arraycopy(fragmentTokens, 0, tokens, index, fragmentTokenLength);
					index += fragmentTokenLength;
				} else if (element instanceof char[]) {
					tokens[index++] = (char[]) element;
				} else {
					arguments[index-1] = (TypeReference[]) element;
				}
			}
			long[] positions = new long[identCount];
			long pos = ((long) start << 32) + end;
			for (int i = 0; i < identCount; i++) {
				positions[i] = pos;
			}
			return null;
		}
	}

	private TypeReference decodeType(char[] typeName, int length, int start, int end) {
		int identCount = 1;
		int dim = 0;
		int nameFragmentStart = this.namePos, nameFragmentEnd = -1;
		ArrayList fragments = null;
		typeLoop: while (this.namePos < length) {
			char currentChar = typeName[this.namePos];
			switch (currentChar) {
				case '?' :
					this.namePos++; // skip '?'
					while (typeName[this.namePos] == ' ') this.namePos++;
					switch(typeName[this.namePos]) {
						case 's' :
							break;
						case 'e' :
							break;
					}
				case '[' :
					if (dim == 0) nameFragmentEnd = this.namePos-1;
					dim++;
					break;
				case ']' :
					break;
				case '>' :
				case ',' :
					break typeLoop;
				case '.' :
					if (nameFragmentStart < 0) nameFragmentStart = this.namePos+1; // member type name
					identCount ++;
					break;
				case '<' :
					// convert 1.5 specific constructs only if compliance is 1.5 or above
					if (!this.has1_5Compliance)
						break typeLoop;
					if (fragments == null) fragments = new ArrayList(2);
					nameFragmentEnd = this.namePos-1;
					char[][] identifiers = CharOperation.splitOn('.', typeName, nameFragmentStart, this.namePos);
					fragments.add(identifiers);
					this.namePos++; // skip '<'
					TypeReference[] arguments = decodeTypeArguments(typeName, length, start, end); // positionned on '>' at end
					fragments.add(arguments);
					identCount = 0;
					nameFragmentStart = -1;
					nameFragmentEnd = -1;
					// next increment will skip '>'
					break;
			}
			this.namePos++;
		}
		if (nameFragmentEnd < 0) nameFragmentEnd = this.namePos-1;
		if (fragments == null) { // non parameterized
			/* rebuild identifiers and dimensions */
			if (identCount == 1) { // simple type reference
				if (dim == 0) {
					char[] nameFragment;
					if (nameFragmentStart != 0 || nameFragmentEnd >= 0) {
						int nameFragmentLength = nameFragmentEnd - nameFragmentStart + 1;
						System.arraycopy(typeName, nameFragmentStart, nameFragment = new char[nameFragmentLength], 0, nameFragmentLength);
					} else {
						nameFragment = typeName;
					}
					return new SingleTypeReference(nameFragment, ((long) start << 32) + end);
				} else {
					int nameFragmentLength = nameFragmentEnd - nameFragmentStart + 1;
					char[] nameFragment = new char[nameFragmentLength];
					System.arraycopy(typeName, nameFragmentStart, nameFragment, 0, nameFragmentLength);
					return new ArrayTypeReference(nameFragment, dim, ((long) start << 32) + end);
				}
			} else { // qualified type reference
				long[] positions = new long[identCount];
				long pos = ((long) start << 32) + end;
				for (int i = 0; i < identCount; i++) {
					positions[i] = pos;
				}
				char[][] identifiers = CharOperation.splitOn('.', typeName, nameFragmentStart, nameFragmentEnd+1);
				if (dim == 0) {
					return new QualifiedTypeReference(identifiers, positions);
				} else {
					return new ArrayQualifiedTypeReference(identifiers, dim, positions);
				}
			}
		} else { // parameterized
			// rebuild type reference from available fragments: char[][], arguments, char[][], arguments...
			// check trailing qualified name
			if (nameFragmentStart > 0 && nameFragmentStart < length) {
				char[][] identifiers = CharOperation.splitOn('.', typeName, nameFragmentStart, nameFragmentEnd+1);
				fragments.add(identifiers);
			}
			int fragmentLength = fragments.size();
			if (fragmentLength == 2) {
				char[][] firstFragment = (char[][]) fragments.get(0);
				if (firstFragment.length == 1) {
					// parameterized single type
					return null;
				}
			}
			// parameterized qualified type
			identCount = 0;
			for (int i = 0; i < fragmentLength; i ++) {
				Object element = fragments.get(i);
				if (element instanceof char[][]) {
					identCount += ((char[][])element).length;
				}
			}
			char[][] tokens = new char[identCount][];
			TypeReference[][] arguments = new TypeReference[identCount][];
			int index = 0;
			for (int i = 0; i < fragmentLength; i ++) {
				Object element = fragments.get(i);
				if (element instanceof char[][]) {
					char[][] fragmentTokens = (char[][]) element;
					int fragmentTokenLength = fragmentTokens.length;
					System.arraycopy(fragmentTokens, 0, tokens, index, fragmentTokenLength);
					index += fragmentTokenLength;
				} else {
					arguments[index-1] = (TypeReference[]) element;
				}
			}
			long[] positions = new long[identCount];
			long pos = ((long) start << 32) + end;
			for (int i = 0; i < identCount; i++) {
				positions[i] = pos;
			}
			return null;
		}
	}

	private TypeReference[] decodeTypeArguments(char[] typeName, int length, int start, int end) {
		ArrayList argumentList = new ArrayList(1);
		int count = 0;
		argumentsLoop: while (this.namePos < length) {
			TypeReference argument = decodeType(typeName, length, start, end);
			count++;
			argumentList.add(argument);
			if (this.namePos >= length) break argumentsLoop;
			if (typeName[this.namePos] == '>') {
				break argumentsLoop;
			}
			this.namePos++; // skip ','
		}
		TypeReference[] typeArguments = new TypeReference[count];
		argumentList.toArray(typeArguments);
		return typeArguments;
	}

	private TypeReference[] decodeTypeArguments(String typeSignature, int length, int start, int end) {
		ArrayList argumentList = new ArrayList(1);
		int count = 0;
		argumentsLoop: while (this.namePos < length) {
			TypeReference argument = decodeType(typeSignature, length, start, end);
			count++;
			argumentList.add(argument);
			if (this.namePos >= length) break argumentsLoop;
			if (typeSignature.charAt(this.namePos) == '>') {
				break argumentsLoop;
			}
		}
		TypeReference[] typeArguments = new TypeReference[count];
		argumentList.toArray(typeArguments);
		return typeArguments;
	}

	private char[][] extractIdentifiers(String typeSignature, int start, int endInclusive, int identCount) {
		char[][] result = new char[identCount][];
		int charIndex = start;
		int i = 0;
		while (charIndex < endInclusive) {
			if (typeSignature.charAt(charIndex) == '.') {
				typeSignature.getChars(start, charIndex, result[i++] = new char[charIndex - start], 0);
				start = ++charIndex;
			} else
				charIndex++;
		}
		typeSignature.getChars(start, charIndex + 1, result[i++] = new char[charIndex - start + 1], 0);
		return result;
	}

	private char[] getSource() {
		if (this.source == null)
			this.source = this.cu.getContents();
		return this.source;
	}

	private Expression parseMemberValue(char[] memberValue) {
		// memberValue must not be null
		if (this.parser == null) {
			this.parser = new Parser(this.problemReporter, true);
		}
		return this.parser.parseMemberValue(memberValue, 0, memberValue.length, this.unit);
	}
}
