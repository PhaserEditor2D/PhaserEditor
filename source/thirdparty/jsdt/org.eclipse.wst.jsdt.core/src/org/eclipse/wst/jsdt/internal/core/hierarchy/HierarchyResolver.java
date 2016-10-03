/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.hierarchy;

/**
 * This is the public entry point to resolve type hierarchies.
 *
 * When requesting additional types from the name environment, the resolver
 * accepts all forms (binary, source & compilation unit) for additional types.
 *
 * Side notes: Binary types already know their resolved supertypes so this
 * only makes sense for source types. Even though the compiler finds all binary
 * types to complete the hierarchy of a given source type, is there any reason
 * why the requestor should be informed that binary type X subclasses Y &
 * implements I & J?
 */

import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.wst.jsdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.wst.jsdt.internal.compiler.IProblemFactory;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction;
import org.eclipse.wst.jsdt.internal.compiler.env.IBinaryType;
import org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.env.IGenericType;
import org.eclipse.wst.jsdt.internal.compiler.env.INameEnvironment;
import org.eclipse.wst.jsdt.internal.compiler.env.ISourceType;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.jsdt.internal.compiler.impl.ITypeRequestor;
import org.eclipse.wst.jsdt.internal.compiler.impl.ITypeRequestor2;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TagBits;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeIds;
import org.eclipse.wst.jsdt.internal.compiler.parser.Parser;
import org.eclipse.wst.jsdt.internal.compiler.parser.SourceTypeConverter;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.wst.jsdt.internal.core.ClassFile;
import org.eclipse.wst.jsdt.internal.core.CompilationUnit;
import org.eclipse.wst.jsdt.internal.core.JavaElement;
import org.eclipse.wst.jsdt.internal.core.Logger;
import org.eclipse.wst.jsdt.internal.core.Member;
import org.eclipse.wst.jsdt.internal.core.Openable;
import org.eclipse.wst.jsdt.internal.core.SourceTypeElementInfo;
import org.eclipse.wst.jsdt.internal.core.util.ASTNodeFinder;
import org.eclipse.wst.jsdt.internal.oaametadata.LibraryAPIs;

public class HierarchyResolver implements ITypeRequestor, ITypeRequestor2 {

	private ReferenceBinding focusType;
	private boolean superTypesOnly;
	private boolean hasMissingSuperClass;
	LookupEnvironment lookupEnvironment;
	private CompilerOptions options;
	HierarchyBuilder builder;
	private ReferenceBinding[] typeBindings;

	HashtableOfObject parsedUnits;
	HashSet processedUnits = new HashSet();

	private int typeIndex;
	private IGenericType[] typeModels;

	public HierarchyResolver(INameEnvironment nameEnvironment, Map settings, HierarchyBuilder builder, IProblemFactory problemFactory) {
		// create a problem handler with the 'exit after all problems' handling policy
		this.options = new CompilerOptions(settings);
		IErrorHandlingPolicy policy = DefaultErrorHandlingPolicies.exitAfterAllProblems();
		ProblemReporter problemReporter = new ProblemReporter(policy, this.options, problemFactory);

		this.setEnvironment(new LookupEnvironment(this, this.options, problemReporter, nameEnvironment), builder);
	}

	public HierarchyResolver(LookupEnvironment lookupEnvironment, HierarchyBuilder builder) {
		this.setEnvironment(lookupEnvironment, builder);
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.impl.ITypeRequestor#accept(org.eclipse.wst.jsdt.internal.compiler.env.IBinaryType, org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding, org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction)
	 */
	public void accept(IBinaryType binaryType, PackageBinding packageBinding, AccessRestriction accessRestriction) {
		//do nothing
	}
	
	/**
	 * Add an additional compilation unit.
	 * 
	 * @param sourceUnit
	 */
	public void accept(ICompilationUnit sourceUnit, AccessRestriction accessRestriction) {
		accept(sourceUnit, CharOperation.NO_CHAR_CHAR, accessRestriction);
	}

	public void accept(ICompilationUnit sourceUnit, char[][] typeNames, AccessRestriction accessRestriction) {
		if (typeNames.length == 0 && this.processedUnits.contains(sourceUnit)) {
			return;
		}

		CompilationResult result = new CompilationResult(sourceUnit, 1, 1, this.options.maxProblemsPerUnit);

		if (parsedUnits == null) {
			parsedUnits = new HashtableOfObject();
		}
		
		CompilationUnitDeclaration parsedUnit = (CompilationUnitDeclaration) parsedUnits.get(sourceUnit.getFileName());
		if (parsedUnit == null) {
			if (typeNames.length == 0) {
				this.processedUnits.add(sourceUnit);
			}
			Parser parser = new Parser(this.lookupEnvironment.problemReporter, true);
			parsedUnit = parser.dietParse(sourceUnit, result);
			parser.inferTypes(parsedUnit, this.options);
			parsedUnits.put(sourceUnit.getFileName(), parsedUnit);
		}
		if (parsedUnit != null) {
			try {
				this.lookupEnvironment.buildTypeBindings(parsedUnit, typeNames, accessRestriction);
				rememberAllTypes(parsedUnit, sourceUnit, false);

				this.lookupEnvironment.completeTypeBindings(parsedUnit, typeNames, true);
			}
			catch (AbortCompilation e) {
				// missing 'java.lang' package: ignore
			}
		}

	}

	public void accept(LibraryAPIs libraryMetaData) {
		lookupEnvironment.buildTypeBindings(libraryMetaData);
	}

	/**
	 * Add additional source types
	 * 
	 * @param sourceTypes
	 * @param packageBinding
	 */
	public void accept(ISourceType[] sourceTypes, PackageBinding packageBinding, AccessRestriction accessRestriction) {
		/* find most enclosing type first (needed when explicit askForType(...) is done
		 * with a member type (e.g. p.A$B)) */
		IProgressMonitor progressMonitor = this.builder.hierarchy.progressMonitor;
		if (progressMonitor != null && progressMonitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		ISourceType sourceType = sourceTypes[0];
		while (sourceType.getEnclosingType() != null) {
			sourceType = sourceType.getEnclosingType();
		}
		
		// build corresponding compilation unit
		CompilationResult result = new CompilationResult(sourceType.getFileName(), sourceType.getPackageName(), 1, 1, this.options.maxProblemsPerUnit);
		
		// ignore secondary types, to improve laziness
		CompilationUnitDeclaration unit = SourceTypeConverter.buildCompilationUnit(new ISourceType[]{sourceType}, 
					SourceTypeConverter.MEMBER_TYPE, // need member types no need for field initialization
					this.lookupEnvironment.problemReporter, result);

		// build bindings
		if (unit != null) {
			try {
				this.lookupEnvironment.buildTypeBindings(unit, accessRestriction);

				org.eclipse.wst.jsdt.core.IJavaScriptUnit cu = ((SourceTypeElementInfo) sourceType).getHandle().getJavaScriptUnit();
				rememberAllTypes(unit, cu, false);

				this.lookupEnvironment.completeTypeBindings(unit, true);
			}
			catch (AbortCompilation e) {
				// missing 'java.lang' package: ignore
			}
		}
	}

	/**
	 * <p>
	 * Creates the super class handle of the given type. Returns null if the type
	 * has no super class. Adds the simple name to the hierarchy missing types if
	 * the class is not found and returns null.
	 * </p>
	 */
	private IType findSuperClass(IGenericType type, ReferenceBinding typeBinding) {
		ReferenceBinding superBinding = typeBinding.getSuperBinding();

		if (superBinding != null) {
			if (typeBinding.isHierarchyInconsistent()) {
				if (superBinding.problemId() == ProblemReasons.NotFound) {
					this.hasMissingSuperClass = true;
					
					// note: this could be Map$Entry
					this.builder.hierarchy.missingTypes.add(new String(superBinding.sourceName)); 
					return null;
				}
				else if ((superBinding.id == TypeIds.T_JavaLangObject)) {
					char[] superclassName;
					char separator;
					if (type instanceof IBinaryType) {
						superclassName = ((IBinaryType) type).getSuperclassName();
						separator = '/';
					}
					else if (type instanceof ISourceType) {
						superclassName = ((ISourceType) type).getSuperclassName();
						separator = '.';
					}
					else if (type instanceof HierarchyType) {
						superclassName = ((HierarchyType) type).superclassName;
						separator = '.';
					}
					else {
						return null;
					}

					// check whether subclass of Object due to broken hierarchy (as opposed to explicitly extending it)
					if (superclassName != null) { 
						int lastSeparator = CharOperation.lastIndexOf(separator, superclassName);
						char[] simpleName = lastSeparator == -1 ?
									superclassName : CharOperation.subarray(superclassName, lastSeparator + 1, superclassName.length);
						
						if (!CharOperation.equals(simpleName, TypeConstants.OBJECT)) {
							this.hasMissingSuperClass = true;
							this.builder.hierarchy.missingTypes.add(new String(simpleName));
							return null;
						}
					}
				}
			}
			char[] readableName = superBinding.readableName();
			for (int t = this.typeIndex; t >= 0; t--) {
				/* For the purpose of the hierarchy, two types are equivalent if
				 * they have the same readable name
				 * 
				 * this is instead of using TypeBinding#isEquivlant on purpose
				 * because then a type with multiple names would
				 * only show up in the hierarchy once. */
				
				if (CharOperation.equals(
							readableName,
							this.typeBindings[t].readableName())) {
					
					return this.builder.getHandle(this.typeModels[t], superBinding);
				}
			}
		}

		return null;
	}

	private void fixSupertypeBindings() {
		for (int current = this.typeIndex; current >= 0; current--) {
			ReferenceBinding typeBinding = this.typeBindings[current];

			if (typeBinding instanceof SourceTypeBinding) {
				ClassScope scope = (ClassScope) ((SourceTypeBinding) typeBinding).scope;
				if (scope != null) {
					TypeDeclaration typeDeclaration = scope.referenceContext;
					TypeReference superclassRef = typeDeclaration == null ? null : typeDeclaration.superclass;
					TypeBinding superclass = superclassRef == null ? null : superclassRef.resolvedType;
					if (superclass instanceof ProblemReferenceBinding) {
						superclass = ((ProblemReferenceBinding) superclass).closestMatch();
					}
					if (superclass != null) {
						((SourceTypeBinding) typeBinding).setSuperBinding((ReferenceBinding) superclass);
					}
				}
			}
			else if (typeBinding instanceof BinaryTypeBinding) {
				try {
					typeBinding.getSuperBinding();
				}
				catch (AbortCompilation e) {
					/* allow subsequent call to superclass() to succeed so that
					 * we don't have to catch AbortCompilation everywhere */
					((BinaryTypeBinding) typeBinding).tagBits &= ~TagBits.HasUnresolvedSuperclass;
					this.builder.hierarchy.missingTypes.add(new String(typeBinding.getSuperBinding().sourceName()));
					this.hasMissingSuperClass = true;
				}
			}
		}
	}

	private void remember(IGenericType suppliedType, ReferenceBinding typeBinding) {
		if (typeBinding == null) {
			return;
		}
		
		//check if this type is a duplicate of one already being remembered
		boolean isDuplicate = false;
		int i;
		for(i = 0; i <= this.typeIndex && !isDuplicate; ++i) {
			/* For the purpose of the hierarchy, two types are equivalent if
			 * they have the same readable name and are defined in the same file */
			isDuplicate = CharOperation.equals(typeBinding.getFileName(), this.typeBindings[i].getFileName()) &&
					CharOperation.equals(
						typeBinding.readableName(),
						this.typeBindings[i].readableName());
		}
		
		if(!isDuplicate) {
			if (++this.typeIndex == this.typeModels.length) {
				System.arraycopy(this.typeModels, 0, this.typeModels = new IGenericType[this.typeIndex * 2], 0, this.typeIndex);
				System.arraycopy(this.typeBindings, 0, this.typeBindings = new ReferenceBinding[this.typeIndex * 2], 0, this.typeIndex);
			}
			
			this.typeModels[this.typeIndex] = suppliedType;
			this.typeBindings[this.typeIndex] = typeBinding;
		}
	}

	private void remember(IType type, ReferenceBinding typeBinding) {
		if (((CompilationUnit) type.getJavaScriptUnit()).isOpen()) {
			try {
				IGenericType genericType = (IGenericType) ((JavaElement) type).getElementInfo();
				remember(genericType, typeBinding);
			}
			catch (JavaScriptModelException e) {
				// cannot happen since element is open
				return;
			}
		}
		else {
			if (typeBinding == null) {
				return;
			}

			TypeDeclaration typeDeclaration = ((ClassScope) ((SourceTypeBinding) typeBinding).scope).referenceType();

			// simple super class name
			char[] superclassName = null;
			TypeReference superclass;
			if ((typeDeclaration.bits & ASTNode.IsAnonymousType) != 0) {
				superclass = typeDeclaration.allocation.type;
			}
			else {
				superclass = typeDeclaration.superclass;
			}
			if (superclass != null) {
				char[][] typeName = superclass.getTypeName();
				superclassName = typeName == null ? null : typeName[typeName.length - 1];
			}

			HierarchyType hierarchyType = new HierarchyType(type, typeDeclaration.name, typeDeclaration.binding.modifiers, superclassName);
			remember(hierarchyType, typeDeclaration.binding);
		}

	}

	private void rememberInferredType(InferredType inferredType, IType type, ReferenceBinding typeBinding) {
		if (type.getJavaScriptUnit() != null && ((CompilationUnit) type.getJavaScriptUnit()).isOpen()) {
			try {
				IGenericType genericType = (IGenericType) ((JavaElement) type).getElementInfo();
				remember(genericType, typeBinding);
			}
			catch (JavaScriptModelException e) {
				// cannot happen since element is open
				return;
			}
		}
		else {
			if (typeBinding == null) {
				return;
			}

			// simple super class name
			char[] superclassName = null;
			ReferenceBinding superBinding = typeBinding.getSuperBinding();
			if(superBinding != null) {
				superclassName = superBinding.qualifiedSourceName();
			} 

			HierarchyType hierarchyType = new HierarchyType(type, inferredType.getName(), 0, superclassName);
			remember(hierarchyType, inferredType.binding);
		}

	}

	/**
	 * <p>
	 * Remembers all type bindings defined in the given parsed unit, adding
	 * local/anonymous types if specified.
	 * </p>
	 *
	 * @param parsedUnit
	 * @param container
	 * @param includeLocalTypes
	 */
	private void rememberAllTypes(CompilationUnitDeclaration parsedUnit, Object container, boolean includeLocalTypes) {
		org.eclipse.wst.jsdt.core.IJavaScriptUnit cu = (container instanceof org.eclipse.wst.jsdt.core.IJavaScriptUnit) ? (org.eclipse.wst.jsdt.core.IJavaScriptUnit) container : null;
		org.eclipse.wst.jsdt.core.IClassFile classFile = (container instanceof org.eclipse.wst.jsdt.core.IClassFile) ? (org.eclipse.wst.jsdt.core.IClassFile) container : null;
		TypeDeclaration[] types = parsedUnit.types;
		if (types != null) {
			for (int i = 0, length = types.length; i < length; i++) {
				TypeDeclaration type = types[i];
				IType typeHandle = (cu != null) ? cu.getType(new String(type.name)) : classFile.getType(new String(type.name));
				rememberWithMemberTypes(type, typeHandle);
			}
		}
		for (int i = 0; i < parsedUnit.numberInferredTypes; i++) {
			InferredType inferredType = parsedUnit.inferredTypes[i];

			if (inferredType.isDefinition()) {
				IType typeHandle = (cu != null) ? cu.getType(new String(inferredType.getName())) : classFile.getType(new String(inferredType.getName()));
				rememberInferredType(inferredType, typeHandle, inferredType.binding);
			}
		}
	}

	private void rememberWithMemberTypes(TypeDeclaration typeDecl, IType typeHandle) {
		remember(typeHandle, typeDecl.binding);

		TypeDeclaration[] memberTypes = typeDecl.memberTypes;
		if (memberTypes != null) {
			for (int i = 0, length = memberTypes.length; i < length; i++) {
				TypeDeclaration memberType = memberTypes[i];
				rememberWithMemberTypes(memberType, typeHandle.getType(new String(memberType.name)));
			}
		}
	}

	/**
	 * <p>
	 * Reports the hierarchy from the remembered bindings. Note that
	 * 'binaryTypeBinding' is null if focus type is a source type.
	 * </p>
	 *
	 * @param focus
	 * @param parsedUnit
	 * @param binaryTypeBinding
	 */
	private void reportHierarchy(IType focus, CompilationUnitDeclaration parsedUnit, ReferenceBinding binaryTypeBinding) {
		// set focus type binding
		if (focus != null) {
			if (binaryTypeBinding != null) {
				// binary type
				this.focusType = binaryTypeBinding;
			}
			else {
				// source type
				Member declaringMember = ((Member) focus).getOuterMostLocalContext();
				if (declaringMember == null) {
					// top level or member type
					char[] fullyQualifiedName = focus.getElementName().toCharArray();
					setFocusType(CharOperation.splitOn('.', fullyQualifiedName));
				}
				else {
					// anonymous or local type
					if (parsedUnit != null) {
						ASTNodeFinder nodeFinder = new ASTNodeFinder(parsedUnit);
						InferredType inferredType = nodeFinder.findInferredType(focus);
						if (inferredType != null)
							this.focusType = inferredType.binding;
						else {
							TypeDeclaration typeDecl = nodeFinder.findType(focus);
							if (typeDecl != null) {
								this.focusType = typeDecl.binding;
							}
						}
					}
				}
			}
		}
		// be resilient and fix super type bindings
		fixSupertypeBindings();

		int objectIndex = -1;
		for (int current = this.typeIndex; current >= 0; current--) {
			ReferenceBinding typeBinding = this.typeBindings[current];

			// java.lang.Object treated at the end
			if (typeBinding.id == TypeIds.T_JavaLangObject) {
				objectIndex = current;
				continue;
			}

			IGenericType suppliedType = this.typeModels[current];

			if (!subOrSuperOfFocus(typeBinding)) {
				continue; // ignore types outside of hierarchy
			}

			IType superclass = findSuperClass(suppliedType, typeBinding);

			this.builder.connect(suppliedType, this.builder.getHandle(suppliedType, typeBinding), superclass);
		}
		// add java.lang.Object only if the super class is not missing
		if (!this.hasMissingSuperClass && objectIndex > -1) {
			IGenericType objectType = this.typeModels[objectIndex];
			this.builder.connect(objectType, this.builder.getHandle(objectType, this.typeBindings[objectIndex]), null);
		}
	}


	private void reset() {
		this.lookupEnvironment.reset();

		this.focusType = null;
		this.superTypesOnly = false;
		this.typeIndex = -1;
		this.typeModels = new IGenericType[5];
		this.typeBindings = new ReferenceBinding[5];
		if (parsedUnits != null) {
			this.parsedUnits.clear();
		}
	}

	/**
	 * Resolve the supertypes for the supplied source type. Inform the
	 * requestor of the resolved supertypes using: connect(ISourceType
	 * suppliedType, IGenericType superclass)
	 * 
	 * @param suppliedType
	 */
	public void resolve(IGenericType suppliedType) {
		try {
			if (suppliedType.isBinaryType()) {
				ReferenceBinding binaryTypeBinding =
							this.lookupEnvironment.cacheBinaryType((ISourceType) suppliedType, false, null );
				
				remember(suppliedType, binaryTypeBinding);
				
				// We still need to add superclasses binding (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=53095)
				int startIndex = this.typeIndex;
				for (int i = startIndex; i <= this.typeIndex; i++) {
					IGenericType igType = this.typeModels[i];
					if (igType != null && igType.isBinaryType()) {
						// fault in its hierarchy...
						try {
							ReferenceBinding typeBinding = this.typeBindings[i];
							typeBinding.getSuperBinding();
						}
						catch (AbortCompilation e) {
							// classpath problem for this type: ignore
						}
					}
				}
				this.superTypesOnly = true;
				reportHierarchy(this.builder.getType(), null, binaryTypeBinding);
			}
			else {
				org.eclipse.wst.jsdt.core.IJavaScriptUnit cu = ((SourceTypeElementInfo) suppliedType).getHandle().getJavaScriptUnit();
				HashSet localTypes = new HashSet();
				localTypes.add(cu.getPath().toString());
				this.superTypesOnly = true;
				resolve(new Openable[]{(Openable) cu}, localTypes, null);
			}
		}
		catch (AbortCompilation e) {
			// ignore this exception for now since it typically means we cannot find java.lang.Object
		}
		finally {
			reset();
		}
	}

	/**
	 * Resolve the supertypes for the types contained in the given openables
	 * (ICompilationUnits and/or IClassFiles). Inform the requestor of the
	 * resolved supertypes for each supplied source type using:
	 * connect(ISourceType suppliedType, IGenericType superclass)
	 * 
	 * Also inform the requestor of the supertypes of each additional
	 * requested super type which is also a source type instead of a binary
	 * type.
	 * 
	 * @param openables
	 * @param localTypes
	 * @param monitor
	 */
	public void resolve(Openable[] openables, HashSet localTypes, IProgressMonitor monitor) {
		try {
			int openablesLength = openables.length;
			CompilationUnitDeclaration[] parsedUnits = new CompilationUnitDeclaration[openablesLength];
			boolean[] hasLocalType = new boolean[openablesLength];
			org.eclipse.wst.jsdt.core.IJavaScriptUnit[] cus = new org.eclipse.wst.jsdt.core.IJavaScriptUnit[openablesLength];
			int unitsIndex = 0;

			CompilationUnitDeclaration focusUnit = null;
			ReferenceBinding focusBinaryBinding = null;
			IType focus = this.builder.getType();
			Openable focusOpenable = null;
			if (focus != null) {
				if (focus.isBinary()) {
					focusOpenable = (Openable) focus.getClassFile();
				}
				else {
					focusOpenable = (Openable) focus.getJavaScriptUnit();
				}
			}

			processedUnits = new HashSet();
			// build type bindings
			Parser parser = new Parser(this.lookupEnvironment.problemReporter, true);
			for (int i = 0; i < openablesLength; i++) {
				Openable openable = openables[i];
				if (openable instanceof org.eclipse.wst.jsdt.core.IJavaScriptUnit) {
					org.eclipse.wst.jsdt.core.IJavaScriptUnit cu = (org.eclipse.wst.jsdt.core.IJavaScriptUnit) openable;

					// contains a potential subtype as a local or anonymous type?
					boolean containsLocalType = false;
					
					// case of hierarchy on region
					if (localTypes == null) { 
						containsLocalType = true;
					}
					else {
						IPath path = cu.getPath();
						containsLocalType = localTypes.contains(path.toString());
					}

					// build parsed unit
					CompilationUnitDeclaration parsedUnit = null;
					if (cu.isOpen()) {
						// create parsed unit from source element infos
						CompilationResult result = new CompilationResult(((ICompilationUnit) cu).getFileName(), ((ICompilationUnit) cu).getPackageName(), i, openablesLength, this.options.maxProblemsPerUnit);
						SourceTypeElementInfo[] typeInfos = null;
						try {
							IType[] topLevelTypes = cu.getTypes();
							int topLevelLength = topLevelTypes.length;
							
							// empty cu: no need to parse (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=65677)
							if (topLevelLength == 0) {
								unitsIndex++;
								continue; 
							}
							typeInfos = new SourceTypeElementInfo[topLevelLength];
							for (int j = 0; j < topLevelLength; j++) {
								IType topLevelType = topLevelTypes[j];
								typeInfos[j] = (SourceTypeElementInfo) ((JavaElement) topLevelType).getElementInfo();
							}
						}
						catch (JavaScriptModelException e) {
							// types/cu exist since cu is opened
						}
						int flags = !containsLocalType ?SourceTypeConverter.MEMBER_TYPE : SourceTypeConverter.FIELD_AND_METHOD | SourceTypeConverter.MEMBER_TYPE | SourceTypeConverter.LOCAL_TYPE;
						parsedUnit = SourceTypeConverter.buildCompilationUnit(typeInfos, flags, this.lookupEnvironment.problemReporter, result);
						if (containsLocalType) {
							parsedUnit.bits |= ASTNode.HasAllMethodBodies;
						}
					}
					else {
						// create parsed unit from file
						IFile file = (IFile) cu.getResource();
						ICompilationUnit sourceUnit = this.builder.createCompilationUnitFromPath(openable, file);

						CompilationResult unitResult = new CompilationResult(sourceUnit, i, openablesLength, this.options.maxProblemsPerUnit);
						parsedUnit = parser.dietParse(sourceUnit, unitResult);
					}

					if (parsedUnit != null) {
						parser.inferTypes(parsedUnit, this.options);
						hasLocalType[unitsIndex] = containsLocalType;
						cus[unitsIndex] = cu;
						parsedUnits[unitsIndex++] = parsedUnit;
						try {
							if (!processedUnits.contains(openable)) {
								this.lookupEnvironment.buildTypeBindings(parsedUnit, null);
							}
							
							processedUnits.add(openable);
							if (openable.equals(focusOpenable)) {
								focusUnit = parsedUnit;
							}
						}
						catch (AbortCompilation e) {
							// classpath problem for this type: ignore
						}
					}
				}
				else {
					// cache binary type binding
					ClassFile classFile = (ClassFile) openable;
					org.eclipse.wst.jsdt.internal.compiler.batch.CompilationUnit sourceUnit = new org.eclipse.wst.jsdt.internal.compiler.batch.CompilationUnit(null, new String(classFile.getFileName()), this.options.defaultEncoding);

					CompilationResult unitResult = new CompilationResult(sourceUnit, i, openablesLength, this.options.maxProblemsPerUnit);
					CompilationUnitDeclaration parsedUnit = parser.dietParse(sourceUnit, unitResult);
					if (parsedUnit != null) {
						parser.inferTypes(parsedUnit, this.options);
						hasLocalType[unitsIndex] = true;
						cus[unitsIndex] = null;
						parsedUnits[unitsIndex++] = parsedUnit;
						try {
							this.lookupEnvironment.buildTypeBindings(parsedUnit, null);
							if (openable.equals(focusOpenable)) {
								focusUnit = parsedUnit;
							}
						}
						catch (AbortCompilation e) {
							// classpath problem for this type: ignore
						}
					}
				}
			}

			for (int i = 0; i <= this.typeIndex; i++) {
				IGenericType suppliedType = this.typeModels[i];
				if (suppliedType != null && suppliedType.isBinaryType()) {
					// fault in its hierarchy...
					try {
						ReferenceBinding typeBinding = this.typeBindings[i];
						typeBinding.getSuperBinding();
					}
					catch (AbortCompilation e) {
						// classpath problem for this type: ignore
					}
				}
			}

			// complete type bindings (ie. connect super types)
			for (int i = 0; i < unitsIndex; i++) {
				CompilationUnitDeclaration parsedUnit = parsedUnits[i];
				if (parsedUnit != null) {
					try {
						boolean containsLocalType = hasLocalType[i];
						
						// NB: no-op if method bodies have been already parsed
						if (containsLocalType) { 
							parser.getMethodBodies(parsedUnit);
						}
						/* complete type bindings and build fields and methods only for local types
						 * (in this case the constructor is needed when resolving local types)
						 * (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=145333) */
						this.lookupEnvironment.completeTypeBindings(parsedUnit, containsLocalType);
					}
					catch (AbortCompilation e) {
						/* classpath problem for this type: don't try to
						 * resolve (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=49809) */
						hasLocalType[i] = false;
					}
				}
				worked(monitor, 1);
			}

			// remember type bindings
			for (int i = 0; i < unitsIndex; i++) {
				CompilationUnitDeclaration parsedUnit = parsedUnits[i];
				if (parsedUnit != null) {
					boolean containsLocalType = hasLocalType[i];
					if (containsLocalType) {
						// parsedUnit.scope.faultInTypes();
						// parsedUnit.resolve();
					}

					rememberAllTypes(parsedUnit, openables[i], containsLocalType);
				}
			}

			/* if no potential subtype was a real subtype of the binary focus type, no need to go further
			 * (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=54043) */
			if (focus != null && focus.isBinary()) {
				char[] typeName = focus.getElementName().toCharArray();
				char[] pkgName = focus.getPackageFragment().getElementName().toCharArray();
				char[][] qualifiedName = new char[][]{pkgName, typeName};
				focusBinaryBinding = this.lookupEnvironment.getCachedType(qualifiedName);
				if (focusBinaryBinding == null) {
					qualifiedName = new char[][]{typeName};
					focusBinaryBinding = this.lookupEnvironment.getCachedType(qualifiedName);
				}
				
				if (focusBinaryBinding == null) {
					return;
				}
			}

			reportHierarchy(focus, focusUnit, focusBinaryBinding);
		}
		catch (ClassCastException e) {
			Logger.logException("Error while resolving hierarchy.", e); //$NON-NLS-1$
		}
		catch (AbortCompilation e) {
			// ignore this exception for now since it typically means we cannot find java.lang.Object
			if (TypeHierarchy.DEBUG) {
				Logger.logException("Error while resolving hierarchy.", e); //$NON-NLS-1$
			}
		}
		finally {
			reset();
		}
	}

	private void setEnvironment(LookupEnvironment lookupEnvironment, HierarchyBuilder builder) {
		this.lookupEnvironment = lookupEnvironment;
		this.builder = builder;

		this.typeIndex = -1;
		this.typeModels = new IGenericType[5];
		this.typeBindings = new ReferenceBinding[5];
	}

	/**
	 * <p>
	 * Set the focus type (ie. the type that this resolver is computing the
	 * hierarch for. Returns the binding of this focus type or null if it could
	 * not be found.
	 * </p>
	 *
	 * @param compoundName
	 * @return
	 */
	public ReferenceBinding setFocusType(char[][] compoundName) {
		if (compoundName == null || this.lookupEnvironment == null) {
			return null;
		}
		
		this.focusType = this.lookupEnvironment.getCachedType(compoundName);
		if (this.focusType == null) {
			this.focusType = this.lookupEnvironment.askForType(compoundName);
		}
		
		if (this.focusType == null) {
			char[][] singleName = {CharOperation.concatWith(compoundName, '.')};
			this.focusType = this.lookupEnvironment.getCachedType(singleName);
			if (this.focusType == null) {
				this.focusType = this.lookupEnvironment.askForType(singleName);
			}
		}
		return this.focusType;
	}

	public boolean subOrSuperOfFocus(ReferenceBinding typeBinding) {
		if (this.focusType == null) {
			// accept all types (case of hierarchy in a region)
			return true; 
		}
		
		try {
			//check if given is the same as focus
			if(this.focusType.isEquivalentTo(typeBinding)) {
				return true;
			}
			
			//check if given is a sub type of the focus
			if (!this.superTypesOnly && this.focusType.isSuperclassOf(typeBinding)) {
				return true;
			}
			
			//check if given is a super of the focus
			if (typeBinding.isSuperclassOf(this.focusType)) {
				return true;
			}
		}
		catch (AbortCompilation e) {
			// unresolved superclass -> ignore
		}
		
		return false;
	}

	protected void worked(IProgressMonitor monitor, int work) {
		if (monitor != null) {
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			else {
				monitor.worked(work);
			}
		}
	}

	public CompilationUnitDeclaration doParse(ICompilationUnit sourceUnit, AccessRestriction accessRestriction) {
		Parser parser = new Parser(this.lookupEnvironment.problemReporter, true);
		CompilationResult unitResult = new CompilationResult(sourceUnit, 1, 1, this.options.maxProblemsPerUnit);
		CompilationUnitDeclaration declaration = parser.dietParse(sourceUnit, unitResult);
		parser.inferTypes(declaration, this.options);
		return declaration;
	}
}