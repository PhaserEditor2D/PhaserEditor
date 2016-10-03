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
package org.eclipse.wst.jsdt.internal.compiler.lookup;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction;
import org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.env.INameEnvironment;
import org.eclipse.wst.jsdt.internal.compiler.env.ISourceType;
import org.eclipse.wst.jsdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.jsdt.internal.compiler.impl.ITypeRequestor;
import org.eclipse.wst.jsdt.internal.compiler.impl.ITypeRequestor2;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfPackage;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleSetOfCharArray;
import org.eclipse.wst.jsdt.internal.oaametadata.ClassData;
import org.eclipse.wst.jsdt.internal.oaametadata.LibraryAPIs;

public class LookupEnvironment implements ProblemReasons, TypeConstants {

	final static int BUILD_FIELDS_AND_METHODS = 4;
	final static int BUILD_TYPE_HIERARCHY = 1;
	final static int CHECK_AND_SET_IMPORTS = 2;
	final static int CONNECT_TYPE_HIERARCHY = 3;
	static final ProblemReferenceBinding TheNotFoundType = new ProblemReferenceBinding(CharOperation.NO_CHAR, null, NotFound);

	/**
	 * Map from typeBinding -> accessRestriction rule
	 */
	private Map accessRestrictions;
	ImportBinding[] defaultImports;

	public PackageBinding defaultPackage;
	HashtableOfPackage knownPackages;
	private int lastCompletedUnitIndex = -1;
	private int lastUnitIndex = -1;

	public INameEnvironment nameEnvironment;
	public CompilerOptions globalOptions;
	public ProblemReporter problemReporter;


	// indicate in which step on the compilation we are.
	// step 1 : build the reference binding
	// step 2 : conect the hierarchy (connect bindings)
	// step 3 : build fields and method bindings.
	private int stepCompleted;
	public ITypeRequestor typeRequestor;
	private ArrayBinding[][] uniqueArrayBindings;

	public CompilationUnitDeclaration unitBeingCompleted = null; // only set
	// while completing units
	public Object missingClassFileLocation = null; // only set when resolving
	// certain references, to help locating problems

	private CompilationUnitDeclaration[] units = new CompilationUnitDeclaration[4];
	private MethodVerifier verifier;
	SimpleSetOfCharArray acceptedCompilationUnits = new SimpleSetOfCharArray();
	private boolean fAddingUnits;
	Stack fAskingForTypeBinding = new CharArrayStack();

	public boolean shouldBuildFieldsAndMethods = true;


	public LookupEnvironment(ITypeRequestor typeRequestor, CompilerOptions globalOptions, ProblemReporter problemReporter, INameEnvironment nameEnvironment) {
		this.typeRequestor = typeRequestor;
		this.globalOptions = globalOptions;
		this.problemReporter = problemReporter;
		this.defaultPackage = new PackageBinding(this); // assume the default
		// package always exists
		this.defaultImports = null;
		this.nameEnvironment = nameEnvironment;
		this.knownPackages = new HashtableOfPackage();
		this.uniqueArrayBindings = new ArrayBinding[5][];
		this.uniqueArrayBindings[0] = new ArrayBinding[50]; // start off the
		// most common 1 dimension array @ 50
		this.accessRestrictions = new HashMap(3);
	}

	/**
	 * Ask the name environment for a type which corresponds to the
	 * compoundName. Answer null if the name cannot be found.
	 */
	public ReferenceBinding askForType(char[][] compoundName) {
		NameEnvironmentAnswer answer = nameEnvironment.findType(compoundName, this.typeRequestor);
		if (answer == null) {
			return null;
		}

		if (answer.isBinaryType()) {
			// the type was found as a .class file
			typeRequestor.accept(answer.getBinaryType(), computePackageFrom(compoundName), answer.getAccessRestriction());
		} else if (answer.isCompilationUnit()) {
			ICompilationUnit compilationUnit = answer.getCompilationUnit();
			
			// the type was found as a .js file, try to build it then search the cache
			acceptedCompilationUnits.add(compilationUnit.getFileName());
			typeRequestor.accept(compilationUnit, answer.getAccessRestriction());
		} else if (answer.isSourceType()) {
			// the type was found as a source model
			typeRequestor.accept(answer.getSourceTypes(), computePackageFrom(compoundName), answer.getAccessRestriction());
		} else if (answer.isMetaData()) {
			LibraryAPIs metadata = answer.getLibraryMetadata();
			
			// the type was found as a .js file, try to build it then search the cache
			acceptedCompilationUnits.add(metadata.fileName);
			typeRequestor.accept(metadata);
		}

		return getCachedType(compoundName);
	}

	/**
	 * Ask the oracle for a type named name in the packageBinding. Answer null if
	 * the name cannot be found.
	 */
	ReferenceBinding askForType(PackageBinding packageBinding, char[] name) {
		return (ReferenceBinding) askForBinding(packageBinding, name, Binding.TYPE | Binding.PACKAGE);
	}

	void addUnitsContainingBindings(char[][] types, int kind, String excludePath) {
		if (fAddingUnits) {
			return;
		}

		fAddingUnits = true;
		try {
			// set used for performance reasons, prevents building the same file multiple times for the
			// same set of input types.
			SimpleSetOfCharArray currentAcceptedCompilationUnits = new SimpleSetOfCharArray();
			for (int i = 0; i < types.length; i++) {
				//skip bindings that have already asked for to avoid infinite recursion
				if((kind & Binding.TYPE) > 1 && this.alreadyAskedForBinding(types[i])) {
					continue;
				}
				
				NameEnvironmentAnswer answer = nameEnvironment.findBinding(types[i], defaultPackage.compoundName, kind, this.typeRequestor, true, excludePath);
				if (answer == null)
					continue;

				if (answer.isBinaryType())
					// the type was found as a .class file
					typeRequestor.accept(answer.getBinaryType(), defaultPackage, answer.getAccessRestriction());
				else if (answer.isSourceType())
					// the type was found as a source model
					typeRequestor.accept(answer.getSourceTypes(), defaultPackage, answer.getAccessRestriction());
				else if (answer.isCompilationUnit()) {
					ICompilationUnit compilationUnit = answer.getCompilationUnit();

					/*
					 * the type was found as a .js file, try to build it then
					 * search the cache
					 */
					acceptedCompilationUnits.add(compilationUnit.getFileName());
					if (typeRequestor instanceof ITypeRequestor2) {
						((ITypeRequestor2) typeRequestor).accept(compilationUnit, types, answer.getAccessRestriction());
					} else {
						typeRequestor.accept(compilationUnit, answer.getAccessRestriction());
					}

				}
				else if (answer.isCompilationUnits()) {
					ICompilationUnit[] compilationUnits = answer.getCompilationUnits();
					for (int j = 0; j < compilationUnits.length; j++) {
						// the type was found as a .js file, try to build it then search the cache
						acceptedCompilationUnits.add(compilationUnits[j].getFileName());
						if (!currentAcceptedCompilationUnits.includes(compilationUnits[j].getFileName())) {
							currentAcceptedCompilationUnits.add(compilationUnits[j].getFileName());
							if (typeRequestor instanceof ITypeRequestor2) {
								((ITypeRequestor2) typeRequestor).accept(compilationUnits[j], types, answer.getAccessRestriction());
							} else {
								typeRequestor.accept(compilationUnits[j], answer.getAccessRestriction());
							}
						}
					}
				}
			}
		}
		finally {
			fAddingUnits = false;
		}
	}
	

	void addUnitsContainingBinding(PackageBinding packageBinding, char[] type, int mask, String excludePath) {
		if (packageBinding == null) {
			if (defaultPackage == null) {
				return;
			}
			packageBinding = defaultPackage;
		}
		NameEnvironmentAnswer answer = nameEnvironment.findBinding(type, packageBinding.compoundName, mask, this.typeRequestor, true, excludePath);
		if (answer == null)
			return;


		if (answer.isBinaryType()) {
			// the type was found as a .class file
			typeRequestor.accept(answer.getBinaryType(), packageBinding, answer.getAccessRestriction());
		} else if (answer.isCompilationUnit()) {
			ICompilationUnit compilationUnit = answer.getCompilationUnit();
			if (!acceptedCompilationUnits.includes(compilationUnit.getFileName())) {
				// the type was found as a .js file, try to build it then search the cache
				acceptedCompilationUnits.add(compilationUnit.getFileName());
				typeRequestor.accept(compilationUnit, answer.getAccessRestriction());
			}
		}
		else if (answer.isCompilationUnits()) {
			ICompilationUnit[] compilationUnits = answer.getCompilationUnits();
			for (int i = 0; i < compilationUnits.length; i++) {
				if (!acceptedCompilationUnits.includes(compilationUnits[i].getFileName())) {
					// the type was found as a .js file, try to build it then search the cache
					acceptedCompilationUnits.add(compilationUnits[i].getFileName());
					typeRequestor.accept(compilationUnits[i], answer.getAccessRestriction());
				}
			}
		}
		else if (answer.isSourceType()) {
			// the type was found as a source model
			typeRequestor.accept(answer.getSourceTypes(), packageBinding, answer.getAccessRestriction());
		}

	}

	Binding askForBinding(GlobalBinding globalBinding, char[] name, int mask) {
		return null;
	}

	Binding askForBinding(PackageBinding packageBinding, char[] name, int mask) {
		if (packageBinding == null) {
			if (defaultPackage == null) {
				return null;
			}
			packageBinding = defaultPackage;
		}
		if (mask == Binding.PACKAGE && (name == null || name.length == 0) && this.defaultPackage.compoundName.length == 0)
			return this.defaultPackage;
		NameEnvironmentAnswer answer = nameEnvironment.findBinding(name, packageBinding.compoundName, mask, this.typeRequestor, true, null);
		if (answer == null)
			return null;

		if ((mask & Binding.TYPE) > 1) {
			//if already asked for, return null to avoid infinite recursion
			if(this.alreadyAskedForBinding(name)) {
				return null;
			}

			fAskingForTypeBinding.push(name);
		}
		try {
			if (answer.isBinaryType())
				// the type was found as a .class file
				typeRequestor.accept(answer.getBinaryType(), packageBinding, answer.getAccessRestriction());
			else if (answer.isCompilationUnit()) {
				ICompilationUnit compilationUnit = answer.getCompilationUnit();
				// the type was found as a .js file, try to build it then search the cache
				acceptedCompilationUnits.add(compilationUnit.getFileName());
				if (!compilationUnit.equals(unitBeingCompleted))
					if (typeRequestor instanceof ITypeRequestor2) {
						((ITypeRequestor2) typeRequestor).accept(compilationUnit, new char[][]{name}, answer.getAccessRestriction());
					}
					else {
						typeRequestor.accept(compilationUnit, answer.getAccessRestriction());
					}
			}
			else if (answer.isCompilationUnits()) {
				ICompilationUnit[] compilationUnits = answer.getCompilationUnits();
				for (int i = 0; i < compilationUnits.length; i++) {
					// the type was found as a .js file, try to build it then search the cache
					acceptedCompilationUnits.add(compilationUnits[i].getFileName());
					if (!compilationUnits[i].equals(unitBeingCompleted)) {
						if (typeRequestor instanceof ITypeRequestor2) {
							((ITypeRequestor2) typeRequestor).accept(compilationUnits[i], new char[][]{name}, answer.getAccessRestriction());
						}
						else {
							typeRequestor.accept(compilationUnits[i], answer.getAccessRestriction());
						}
					}
				}
			}
			else if (answer.isSourceType()) {
				// the type was found as a source model
				typeRequestor.accept(answer.getSourceTypes(), packageBinding, answer.getAccessRestriction());
			} else if (answer.isMetaData()) {
				LibraryAPIs metadata = answer.getLibraryMetadata();

				// the type was found as a .js file, try to build it then search the cache
				acceptedCompilationUnits.add(metadata.fileName);
				typeRequestor.accept(metadata);
			}
		}
		finally {
			if (mask == Binding.TYPE) {
				fAskingForTypeBinding.pop();
			}
		}
		return packageBinding.getBinding0(name, mask);
	}

	/**
	 * Create the initial type bindings for the compilation unit.
	 * 
	 * See completeTypeBindings() for a description of the remaining steps
	 * 
	 * NOTE: This method can be called multiple times as additional source files
	 * are needed
	 */
	public void buildTypeBindings(CompilationUnitDeclaration unit, AccessRestriction accessRestriction) {
		buildTypeBindings(unit, CharOperation.NO_CHAR_CHAR, accessRestriction, false);
	}
	
	public void buildTypeBindings(CompilationUnitDeclaration unit, AccessRestriction accessRestriction, boolean shouldBuildGlobalSuperType) {
		buildTypeBindings(unit, CharOperation.NO_CHAR_CHAR, accessRestriction, shouldBuildGlobalSuperType);
	}
	
	public void buildTypeBindings(CompilationUnitDeclaration unit, char[][] typeNames, AccessRestriction accessRestriction) {
		buildTypeBindings(unit, typeNames, accessRestriction, false);
	}

	public void buildTypeBindings(CompilationUnitDeclaration unit, char[][] typeNames, AccessRestriction accessRestriction, boolean shouldBuildGlobalSuperType) {
		CompilationUnitScope scope = unit.scope;
		boolean addUnit = false;
		if (scope == null) {
			scope = new CompilationUnitScope(unit, this);
			addUnit = true;
		}

		scope.setShouldBuildGlobalSuperType(shouldBuildGlobalSuperType);
		scope.buildTypeBindings(typeNames, accessRestriction);

		if (addUnit) {
			int unitsLength = units.length;
			if (++lastUnitIndex >= unitsLength) {
				System.arraycopy(units, 0, units = new CompilationUnitDeclaration[2 * unitsLength], 0, unitsLength);
			}
			units[lastUnitIndex] = unit;
		}
	}

	/**
	 * Cache the binary type since we know it is needed during this compile.
	 * 
	 * Answer the created BinaryTypeBinding or null if the type is already in the
	 * cache.
	 */
	public BinaryTypeBinding cacheBinaryType(ISourceType binaryType, AccessRestriction accessRestriction) {
		return cacheBinaryType(binaryType, true, accessRestriction);
	}

	/**
	 * Cache the binary type since we know it is needed during this compile.
	 * 
	 * Answer the created BinaryTypeBinding or null if the type is already in the
	 * cache.
	 */
	public BinaryTypeBinding cacheBinaryType(ISourceType binaryType, boolean needFieldsAndMethods, AccessRestriction accessRestriction) {
		char[][] compoundName = CharOperation.splitOn('/', binaryType.getName());
		ReferenceBinding existingType = getCachedType(compoundName);

		if (existingType == null || existingType instanceof UnresolvedReferenceBinding) {
			// only add the binary type if its not already in the cache
			return createBinaryTypeFrom(binaryType, computePackageFrom(compoundName), needFieldsAndMethods, accessRestriction);
		}
		
		// the type already exists & can be retrieved from the cache
		return null; 
	}

	public MissingBinaryTypeBinding cacheMissingBinaryType(char[][] compoundName, CompilationUnitDeclaration unit) {
		PackageBinding packageBinding = computePackageFrom(compoundName);
		if (unit == null) {
			return null;
		}
		
		// create a proxy for the missing BinaryType
		MissingBinaryTypeBinding type = new MissingBinaryTypeBinding(packageBinding, compoundName, this, unit.scope);
		if (type.id != TypeIds.T_JavaLangObject) {
			// make Object be its superclass - it could in turn be missing as well
			ReferenceBinding objectType = getType(TypeConstants.JAVA_LANG_OBJECT);
			if (objectType == null) {
				objectType = cacheMissingBinaryType(TypeConstants.JAVA_LANG_OBJECT, unit); // create
			}
			
			// a proxy for the missing Object type
			type.setMissingSuperclass(objectType);
		}
		packageBinding.addType(type);
		return type;
	}

	/**
	 * 1. Connect the type hierarchy for the type bindings created for
	 * parsedUnits. 2. Create the field bindings 3. Create the method bindings
	 * 
	 * We know each known compilationUnit is free of errors at this point...
	 * 
	 * Each step will create additional bindings unless a problem is detected, in
	 * which case either the faulty import/superinterface/field/method will be
	 * skipped or a suitable replacement will be substituted (such as Object for a
	 * missing superclass)
	 */
	public void completeTypeBindings() {
		stepCompleted = BUILD_TYPE_HIERARCHY;

		for (int i = this.lastCompletedUnitIndex + 1; i <= this.lastUnitIndex; i++) {
			(this.unitBeingCompleted = this.units[i]).scope.checkAndSetImports();
		}
		stepCompleted = CHECK_AND_SET_IMPORTS;

		for (int i = this.lastCompletedUnitIndex + 1; i <= this.lastUnitIndex; i++) {
			(this.unitBeingCompleted = this.units[i]).scope.connectTypeHierarchy();
		}
		stepCompleted = CONNECT_TYPE_HIERARCHY;

		for (int i = this.lastCompletedUnitIndex + 1; i <= this.lastUnitIndex; i++) {
			CompilationUnitScope unitScope = (this.unitBeingCompleted = this.units[i]).scope;
			unitScope.buildFieldsAndMethods();
			this.units[i] = null; // release unnecessary reference to the parsed unit
		}
		stepCompleted = BUILD_FIELDS_AND_METHODS;
		this.lastCompletedUnitIndex = this.lastUnitIndex;
		this.unitBeingCompleted = null;
	}

	public void completeTypeBindings(char[][] types) {
		stepCompleted = BUILD_TYPE_HIERARCHY;

		for (int i = this.lastCompletedUnitIndex + 1; i <= this.lastUnitIndex; i++) {
			(this.unitBeingCompleted = this.units[i]).scope.checkAndSetImports();
		}
		stepCompleted = CHECK_AND_SET_IMPORTS;

		for (int i = this.lastCompletedUnitIndex + 1; i <= this.lastUnitIndex; i++) {
			(this.unitBeingCompleted = this.units[i]).scope.connectTypeHierarchy(types);
		}
		stepCompleted = CONNECT_TYPE_HIERARCHY;

		for (int i = this.lastCompletedUnitIndex + 1; i <= this.lastUnitIndex; i++) {
			CompilationUnitScope unitScope = (this.unitBeingCompleted = this.units[i]).scope;
			unitScope.buildFieldsAndMethods();
			this.units[i] = null; // release unnecessary reference to the parsed unit
		}
		stepCompleted = BUILD_FIELDS_AND_METHODS;
		this.lastCompletedUnitIndex = this.lastUnitIndex;
		this.unitBeingCompleted = null;
	}
	
	/**
	 * 1. Connect the type hierarchy for the type bindings created for
	 * parsedUnits. 2. Create the field bindings 3. Create the method bindings
	 * 
	 * Each step will create additional bindings unless a problem is detected, in
	 * which case either the faulty import/superinterface/field/method will be
	 * skipped or a suitable replacement will be substituted (such as Object for a
	 * missing superclass)
	 */
	public void completeTypeBindings(CompilationUnitDeclaration parsedUnit) {
		completeTypeBindings(parsedUnit, CharOperation.NO_CHAR_CHAR);
	}

	public void completeTypeBindings(CompilationUnitDeclaration parsedUnit, char[][] typeNames) {
		if (stepCompleted == BUILD_FIELDS_AND_METHODS) {
			/* This can only happen because the original set of units are completely built and
			 * are now being processed, so we want to treat all the additional units as a group
			 * until they too are completely processed. */
			completeTypeBindings();
		}
		else {
			if (parsedUnit.scope == null) {
				return; // parsing errors were too severe
			}

			if (stepCompleted >= CHECK_AND_SET_IMPORTS) {
				(this.unitBeingCompleted = parsedUnit).scope.checkAndSetImports();
			}

			if (stepCompleted >= CONNECT_TYPE_HIERARCHY) {
				(this.unitBeingCompleted = parsedUnit).scope.connectTypeHierarchy(typeNames);
			}

			this.unitBeingCompleted = null;
		}
	}

	/**
	 * Used by other compiler tools which do not start by calling
	 * completeTypeBindings().
	 * 
	 * 1. Connect the type hierarchy for the type bindings created for
	 * parsedUnits. 2. Create the field bindings 3. Create the method bindings
	 */
	public void completeTypeBindings(CompilationUnitDeclaration parsedUnit, char[][] typeNames, boolean buildFieldsAndMethods) {
		if (parsedUnit.scope == null) {
			return; // parsing errors were too severe
		}

		(this.unitBeingCompleted = parsedUnit).scope.checkAndSetImports();
		parsedUnit.scope.connectTypeHierarchy(typeNames);
		if (buildFieldsAndMethods) {
			shouldBuildFieldsAndMethods = false;
			try {
				parsedUnit.scope.buildFieldsAndMethods();
			}
			finally {
				shouldBuildFieldsAndMethods = true;
			}
		}
		this.unitBeingCompleted = null;
	}

	public void completeTypeBindings(CompilationUnitDeclaration parsedUnit, boolean buildFieldsAndMethods) {
		completeTypeBindings(parsedUnit, CharOperation.NO_CHAR_CHAR, buildFieldsAndMethods);
	}

	public TypeBinding computeBoxingType(TypeBinding type) {
		TypeBinding boxedType;
		switch (type.id) {
			case TypeIds.T_JavaLangBoolean :
				return TypeBinding.BOOLEAN;
			case TypeIds.T_JavaLangCharacter :
				return TypeBinding.CHAR;
			case TypeIds.T_JavaLangShort :
				return TypeBinding.SHORT;
			case TypeIds.T_JavaLangDouble :
				return TypeBinding.DOUBLE;
			case TypeIds.T_JavaLangFloat :
				return TypeBinding.FLOAT;
			case TypeIds.T_JavaLangInteger :
				return TypeBinding.INT;
			case TypeIds.T_JavaLangLong :
				return TypeBinding.LONG;

			case TypeIds.T_int :
				boxedType = getType(JAVA_LANG_INTEGER);
				if (boxedType != null)
					return boxedType;
				return new ProblemReferenceBinding(JAVA_LANG_INTEGER, null, NotFound);
			case TypeIds.T_short :
				boxedType = getType(JAVA_LANG_SHORT);
				if (boxedType != null)
					return boxedType;
				return new ProblemReferenceBinding(JAVA_LANG_SHORT, null, NotFound);
			case TypeIds.T_char :
				boxedType = getType(JAVA_LANG_CHARACTER);
				if (boxedType != null)
					return boxedType;
				return new ProblemReferenceBinding(JAVA_LANG_CHARACTER, null, NotFound);
			case TypeIds.T_long :
				boxedType = getType(JAVA_LANG_LONG);
				if (boxedType != null)
					return boxedType;
				return new ProblemReferenceBinding(JAVA_LANG_LONG, null, NotFound);
			case TypeIds.T_float :
				boxedType = getType(JAVA_LANG_FLOAT);
				if (boxedType != null)
					return boxedType;
				return new ProblemReferenceBinding(JAVA_LANG_FLOAT, null, NotFound);
			case TypeIds.T_double :
				boxedType = getType(JAVA_LANG_DOUBLE);
				if (boxedType != null)
					return boxedType;
				return new ProblemReferenceBinding(JAVA_LANG_DOUBLE, null, NotFound);
			case TypeIds.T_boolean :
				boxedType = getType(JAVA_LANG_BOOLEAN);
				if (boxedType != null)
					return boxedType;
				return new ProblemReferenceBinding(JAVA_LANG_BOOLEAN, null, NotFound);
		}
		return type;
	}

	private PackageBinding computePackageFrom(char[][] constantPoolName) {
		return defaultPackage;
	}

	/**
	 * Used to guarantee array type identity.
	 */
	public ArrayBinding createArrayType(TypeBinding leafComponentType, int dimensionCount) {
		 // cache local type arrays with the local type itself
		if (leafComponentType instanceof LocalTypeBinding) {
			return ((LocalTypeBinding) leafComponentType).createArrayType(dimensionCount, this);
		}

		// find the array binding cache for this dimension
		int dimIndex = dimensionCount - 1;
		int length = uniqueArrayBindings.length;
		ArrayBinding[] arrayBindings;
		if (dimIndex < length) {
			if ((arrayBindings = uniqueArrayBindings[dimIndex]) == null) {
				uniqueArrayBindings[dimIndex] = arrayBindings = new ArrayBinding[10];
			}
		}
		else {
			System.arraycopy(uniqueArrayBindings, 0, uniqueArrayBindings = new ArrayBinding[dimensionCount][], 0, length);
			uniqueArrayBindings[dimIndex] = arrayBindings = new ArrayBinding[10];
		}

		// find the cached array binding for this leaf component type (if any)
		int index = -1;
		length = arrayBindings.length;
		while (++index < length) {
			ArrayBinding currentBinding = arrayBindings[index];
			if (currentBinding == null) // no matching array, but space left
				return arrayBindings[index] = new ArrayBinding(leafComponentType, dimensionCount, this);
			if (currentBinding.leafComponentType == leafComponentType)
				return currentBinding;
		}

		// no matching array, no space left
		System.arraycopy(arrayBindings, 0, (arrayBindings = new ArrayBinding[length * 2]), 0, length);
		uniqueArrayBindings[dimIndex] = arrayBindings;
		return arrayBindings[length] = new ArrayBinding(leafComponentType, dimensionCount, this);
	}

	public BinaryTypeBinding createBinaryTypeFrom(ISourceType binaryType, PackageBinding packageBinding, AccessRestriction accessRestriction) {
		return createBinaryTypeFrom(binaryType, packageBinding, true, accessRestriction);
	}

	public BinaryTypeBinding createBinaryTypeFrom(ISourceType binaryType, PackageBinding packageBinding, boolean needFieldsAndMethods, AccessRestriction accessRestriction) {
		BinaryTypeBinding binaryBinding = new BinaryTypeBinding(packageBinding, binaryType, this);

		// resolve any array bindings which reference the unresolvedType
		ReferenceBinding cachedType = packageBinding.getType0(binaryBinding.compoundName[binaryBinding.compoundName.length - 1]);
		if (cachedType != null) { // update reference to unresolved binding
			// after having read classfile (knows whether generic for raw conversion)
			if (cachedType instanceof UnresolvedReferenceBinding) {
				((UnresolvedReferenceBinding) cachedType).setResolvedType(binaryBinding, this);
			}
			else {
				// sanity check... at this point the cache should ONLY contain unresolved types
				if (cachedType.isBinaryBinding()) {
					return (BinaryTypeBinding) cachedType;
				}
				/* it is possible with a large number of source files
				 * (exceeding AbstractImageBuilder.MAX_AT_ONCE) that a member type can be in
				 * the cache as an UnresolvedType,
				 * but because its enclosingType is resolved while its created
				 * (call to BinaryTypeBinding constructor), its replaced with a source type */
				return null;
			}
		}

		packageBinding.addType(binaryBinding);
		setAccessRestriction(binaryBinding, accessRestriction);
		binaryBinding.cachePartsFrom(binaryType, needFieldsAndMethods);
		return binaryBinding;
	}

	/**
	 * Used to create packages from the package statement.
	 */
	public PackageBinding createPackage(char[][] compoundName) {
		PackageBinding packageBinding = getPackage0(compoundName[0]);
		if (packageBinding == null) {
			packageBinding = new PackageBinding(compoundName[0], this);
		}

		for (int i = 1, length = compoundName.length; i < length; i++) {
			/* check to see if it collides with a known type...
			 * this case can only happen if the package does not exist as a
			 * directory in the file system
			 * otherwise when the source type was defined, the correct error
			 * would have been reported
			 * unless its an unresolved type which is referenced from an
			 * inconsistent class file
			 * NOTE: empty packages are not packages according to changes in
			 * JLS v2, 7.4.3
			 * so not all types cause collision errors when they're created
			 * even though the package did exist */
			ReferenceBinding type = packageBinding.getType0(compoundName[i]);
			if (type != null && type != TheNotFoundType && !(type instanceof UnresolvedReferenceBinding)) {
				return null;
			}

			PackageBinding parent = packageBinding;
			if ((packageBinding = parent.getPackage0(compoundName[i])) == null) {
				/* if the package is unknown, check to see if a type exists
				 * which would collide with the new package catches the case
				 * of a package statement of: package java.lang.Object; since
				 * the package can be added after a set of source files have
				 * already been compiled, we need to check whenever a package
				 * is created
				 */
				packageBinding = new PackageBinding(CharOperation.subarray(compoundName, 0, i + 1), parent, this);
				parent.addPackage(packageBinding);
			}
		}
		return packageBinding;
	}

	/**
	 * Returns the access restriction associated to a given type, or null if
	 * none
	 */
	public AccessRestriction getAccessRestriction(TypeBinding type) {
		return (AccessRestriction) this.accessRestrictions.get(type);
	}

	/**
	 * Answer the type for the compoundName if it exists in the cache. Answer
	 * theNotFoundType if it could not be resolved the first time it was
	 * looked up, otherwise answer null.
	 * 
	 * NOTE: Do not use for nested types... the answer is NOT the same for
	 * a.b.C or a.b.C.D.E assuming C is a type in both cases. In the a.b.C.D.E
	 * case, null is the answer.
	 */
	public ReferenceBinding getCachedType(char[][] compoundName) {
		if (compoundName.length == 1) {
			if (defaultPackage == null) {
				return null;
			}
			return defaultPackage.getType0(compoundName[0]);
		}

		PackageBinding packageBinding = getPackage0(compoundName[0]);
		if (packageBinding == null) {
			return null;
		}

		for (int i = 1, packageLength = compoundName.length - 1; i < packageLength; i++) {
			if ((packageBinding = packageBinding.getPackage0(compoundName[i])) == null) {
				return null;
			}
		}
		return packageBinding.getType0(compoundName[compoundName.length - 1]);
	}

	/**	
	 * Answer the top level package named name if it exists in the cache. Answer
	 * theNotFoundPackage if it could not be resolved the first time it was looked
	 * up, otherwise answer null.
	 * 
	 * NOTE: Senders must convert theNotFoundPackage into a real problem package
	 * if its to returned.
	 */
	PackageBinding getPackage0(char[] name) {
		if (CharOperation.equals(name, defaultPackage.readableName())) {
			return defaultPackage;
		}

		return knownPackages.get(name);
	}

	/**	
	 * Answer the type corresponding to the compoundName. Ask the name environment
	 * for the type if its not in the cache. Fail with a classpath error if the
	 * type cannot be found.
	 */
	public ReferenceBinding getResolvedType(char[][] compoundName, Scope scope) {
		ReferenceBinding type = getType(compoundName);
		if (type != null)
			return type;

		// create a proxy for the missing BinaryType
		return cacheMissingBinaryType(compoundName, scope == null ? this.unitBeingCompleted : scope.referenceCompilationUnit());
	}

	/**
	 * Answer the top level package named name. Ask the oracle for the package if
	 * its not in the cache. Answer null if the package cannot be found.
	 */
	PackageBinding getTopLevelPackage(char[] name) {
		if (CharOperation.equals(name, defaultPackage.readableName())) {
			return defaultPackage;
		}

		PackageBinding packageBinding = getPackage0(name);
		if (packageBinding != null) {
			return packageBinding;
		}

		if (nameEnvironment.isPackage(null, name)) {
			knownPackages.put(name, packageBinding = new PackageBinding(name, this));
			return packageBinding;
		}

		return null;
	}

	/**
	 * Answer the type corresponding to the compoundName. Ask the name environment
	 * for the type if its not in the cache. Answer null if the type cannot be
	 * found.
	 */
	public ReferenceBinding getType(char[][] compoundName) {
		ReferenceBinding referenceBinding;

		if (compoundName.length == 1) {
			if (defaultPackage == null)
				return null;

			if ((referenceBinding = defaultPackage.getType0(compoundName[0])) == null) {
				PackageBinding packageBinding = getPackage0(compoundName[0]);
				 
				// collides with a known package... should not call this method in such a case
				if (packageBinding != null) {
					return null; 
				}
				referenceBinding = askForType(defaultPackage, compoundName[0]);
			}
		}
		else {
			PackageBinding packageBinding = getPackage0(compoundName[0]);

			if (packageBinding != null) {
				for (int i = 1, packageLength = compoundName.length - 1; i < packageLength; i++) {
					if ((packageBinding = packageBinding.getPackage0(compoundName[i])) == null) {
						break;
					}
				}
			}

			if (packageBinding == null) {
				referenceBinding = askForType(compoundName);
			} else if ((referenceBinding = packageBinding.getType0(compoundName[compoundName.length - 1])) == null) {
				referenceBinding = askForType(packageBinding, compoundName[compoundName.length - 1]);
			}
		}

		if (referenceBinding == null || referenceBinding == TheNotFoundType) {
			return null;
		}
		referenceBinding = BinaryTypeBinding.resolveType(referenceBinding, this, false); // no
		// raw conversion for now
		// compoundName refers to a nested type incorrectly (for example, package1.A$B)
		if (referenceBinding.isNestedType()) {
			return new ProblemReferenceBinding(compoundName, referenceBinding, InternalNameProvided);
		}
		return referenceBinding;
	}

	/**
	 * Answer the type corresponding to the compound name. Does not ask the oracle
	 * for the type if its not found in the cache... instead an unresolved type is
	 * returned which must be resolved before used.
	 * 
	 * NOTE: Does NOT answer base types nor array types!
	 */
	ReferenceBinding getTypeFromCompoundName(char[][] compoundName, boolean isParameterized) {
		ReferenceBinding binding = getCachedType(compoundName);
		if (binding == null) {
			PackageBinding packageBinding = computePackageFrom(compoundName);
			binding = new UnresolvedReferenceBinding(compoundName, packageBinding);
			packageBinding.addType(binding);
		}
		else if (binding == TheNotFoundType) {
			// create a proxy for the missing BinaryType
			binding = cacheMissingBinaryType(compoundName, this.unitBeingCompleted);
		}
		return binding;
	}

	/**
	 * Answer the type corresponding to the name from the binary file. Does not
	 * ask the oracle for the type if its not found in the cache... instead an
	 * unresolved type is returned which must be resolved before used.
	 * 
	 * NOTE: Does NOT answer base types nor array types!
	 */
	ReferenceBinding getTypeFromConstantPoolName(char[] signature, int start, int end, boolean isParameterized) {
		if (end == -1) {
			end = signature.length;
		}

		char[][] compoundName = CharOperation.splitOn('/', signature, start, end);
		return getTypeFromCompoundName(compoundName, isParameterized);
	}

	/**
	 * Answer the type corresponding to the signature from the binary file. Does
	 * not ask the oracle for the type if its not found in the cache... instead an
	 * unresolved type is returned which must be resolved before used.
	 * 
	 * NOTE: Does answer base types & array types.
	 */
	TypeBinding getTypeFromSignature(char[] signature, int start, int end, boolean isParameterized, TypeBinding enclosingType) {
		int dimension = 0;
		while (signature[start] == '[') {
			start++;
			dimension++;
		}
		if (end == -1)
			end = signature.length - 1;

		// Just switch on signature[start] - the L case is the else
		TypeBinding binding = null;
		if (start == end) {
			switch (signature[start]) {
				case 'I' :
					binding = TypeBinding.INT;
					break;
				case 'Z' :
					binding = TypeBinding.BOOLEAN;
					break;
				case 'V' :
					binding = TypeBinding.VOID;
					break;
				case 'C' :
					binding = TypeBinding.CHAR;
					break;
				case 'D' :
					binding = TypeBinding.DOUBLE;
					break;
				case 'F' :
					binding = TypeBinding.FLOAT;
					break;
				case 'J' :
					binding = TypeBinding.LONG;
					break;
				case 'S' :
					binding = TypeBinding.SHORT;
					break;
				default :
					problemReporter.corruptedSignature(enclosingType, signature, start);
					// will never reach here, since error will cause abort
			}
		}
		else {
			// skip leading 'L' or 'T'
			binding = getTypeFromConstantPoolName(signature, start + 1, end, isParameterized); 
		}

		if (dimension == 0)
			return binding;
		return createArrayType(binding, dimension);
	}

	TypeBinding getTypeFromTypeSignature(SignatureWrapper wrapper, ReferenceBinding enclosingType) {
		// TypeVariableSignature = 'T' Identifier ';'
		// ArrayTypeSignature = '[' TypeSignature
		// ClassTypeSignature = 'L' Identifier TypeArgs(optional) ';'
		// or ClassTypeSignature '.' 'L' Identifier TypeArgs(optional) ';'
		// TypeArgs = '<' VariantTypeSignature VariantTypeSignatures '>'
		int dimension = 0;
		while (wrapper.signature[wrapper.start] == '[') {
			wrapper.start++;
			dimension++;
		}

		// cannot reach this, since previous problem will abort compilation
		if (wrapper.signature[wrapper.start] == 'T') {
			return null; 
		}
		TypeBinding type = getTypeFromSignature(wrapper.signature, wrapper.start, wrapper.computeEnd(), false, enclosingType);
		return dimension == 0 ? type : createArrayType(type, dimension);
	}

	TypeBinding getTypeFromVariantTypeSignature(SignatureWrapper wrapper, ReferenceBinding enclosingType, ReferenceBinding genericType, int rank) {
		// VariantTypeSignature = '-' TypeSignature
		// or '+' TypeSignature
		// or TypeSignature
		// or '*'
		switch (wrapper.signature[wrapper.start]) {
			case '-' :
				// ? super aType
				wrapper.start++;
				TypeBinding bound = getTypeFromTypeSignature(wrapper, enclosingType);
			case '+' :
				// ? extends aType
				wrapper.start++;
				bound = getTypeFromTypeSignature(wrapper, enclosingType);
			case '*' :
				// ?
				wrapper.start++;
			default :
				return getTypeFromTypeSignature(wrapper, enclosingType);
		}
	}

	/**
	 * Ask the oracle if a package exists named name in the package named
	 * compoundName.
	 */
	boolean isPackage(char[][] compoundName, char[] name) {
		if (compoundName == null || compoundName.length == 0) {
			return nameEnvironment.isPackage(null, name);
		}
		return nameEnvironment.isPackage(compoundName, name);
	}

	/**
	 * The method verifier is lazily initialized to guarantee the receiver, the
	 *  compiler & the oracle are ready.
	 *
	 * @return
	 */
	public MethodVerifier methodVerifier() {
		if (verifier == null)
			verifier = new MethodVerifier(this);
		return verifier;
	}

	public void reset() {
		// assume the default package always exists
		this.defaultPackage = new PackageBinding(this); 
		this.defaultImports = null;
		this.accessRestrictions = new HashMap(3);

		this.verifier = null;
		for (int i = this.uniqueArrayBindings.length; --i >= 0;) {
			ArrayBinding[] arrayBindings = this.uniqueArrayBindings[i];
			if (arrayBindings != null) {
				for (int j = arrayBindings.length; --j >= 0;) {
					arrayBindings[j] = null;
				}
			}
		}

		for (int i = this.units.length; --i >= 0;) {
			this.units[i] = null;
		}
		this.lastUnitIndex = -1;
		this.lastCompletedUnitIndex = -1;
		this.unitBeingCompleted = null; // in case AbortException occurred

		// name environment has a longer life cycle, and must be reset in
		// the code which created it.
		this.acceptedCompilationUnits.clear();
		this.fAskingForTypeBinding.clear();
	}

	/**
	 * Associate a given type with some access restriction (did not store the
	 * restriction directly into binding, since sparse information)
	 */
	public void setAccessRestriction(ReferenceBinding type, AccessRestriction accessRestriction) {
		if (accessRestriction == null)
			return;
		type.modifiers |= ExtraCompilerModifiers.AccRestrictedAccess;
		this.accessRestrictions.put(type, accessRestriction);
	}

	public void buildTypeBindings(LibraryAPIs libraryMetaData) {

		ClassData[] classes = libraryMetaData.classes;
		PackageBinding packageBinding = this.defaultPackage;
		int typeLength = (classes != null ? classes.length : 0);
		int count = 0;

		LibraryAPIsScope scope = new LibraryAPIsScope(libraryMetaData, this);
		SourceTypeBinding[] topLevelTypes = new SourceTypeBinding[typeLength];

		for (int i = 0; i < typeLength; i++) {
			ClassData clazz = classes[i];
			char[][] className = CharOperation.arrayConcat(packageBinding.compoundName, clazz.name.toCharArray());

			SourceTypeBinding binding = new MetatdataTypeBinding(className, packageBinding, clazz, scope);
			this.defaultPackage.addType(binding);
			binding.fPackage.addType(binding);
			topLevelTypes[count++] = binding;

		}
		if (count != topLevelTypes.length) {
			System.arraycopy(topLevelTypes, 0, topLevelTypes = new SourceTypeBinding[count], 0, count);
		}

		char[] fullFileName = libraryMetaData.fileName;

		LibraryAPIsBinding libraryAPIsBinding = new LibraryAPIsBinding(null, defaultPackage, fullFileName);

		if (packageBinding != this.defaultPackage) {
			packageBinding.addBinding(libraryAPIsBinding, libraryAPIsBinding.shortReadableName(), Binding.COMPILATION_UNIT);
		}
	}

	public CompilationUnitDeclaration getExistingCompilationUnitDeclaration(char[] fileName) {
		for (int i = 0; i <= this.lastUnitIndex; i++) {
			if (CharOperation.equals(this.units[i].getFileName(), fileName)) {
				return this.units[i];
			}
		}
		return null;
	}
	
	/**
	 * <p>
	 * Determines if a binding has already been asked for for the given type
	 * name.
	 * </p>
	 * 
	 * @param typeName
	 *            determine if a binding has already been asked for for the
	 *            type with this name
	 * 
	 * @return <code>true</code> if a type with the given name has already had
	 *         a binding requested, </code>false</code> otherwise.
	 */
	private boolean alreadyAskedForBinding(char[] typeName) {
		return !this.fAskingForTypeBinding.isEmpty() && this.fAskingForTypeBinding.contains(typeName);
	}

	/**
	 * <p>
	 * This stack can handle doing {@link #contains(Object)} and
	 * {@link #search(Object)}s for <code>char[]</code>s which a normal stack
	 * could not handle because <code>equals</code> does not work on
	 * <code>char[]</code>s.
	 * </p>
	 */
	private static class CharArrayStack extends Stack {
		private static final long serialVersionUID = 1L;

		/**
		 * @see java.util.Vector#contains(java.lang.Object)
		 */
		public boolean contains(Object o) {
			boolean contains = false;

			if (o instanceof char[]) {
				contains = this.search(o) != -1;
			}
			else {
				contains = super.contains(o);
			}

			return contains;
		}

		/**
		 * @see java.util.Stack#search(java.lang.Object)
		 */
		public synchronized int search(Object o) {
			int index = -1;

			if (o instanceof char[]) {
				for (int i = 0; i < this.size() && index == -1; ++i) {
					Object curr = this.get(i);
					if (curr instanceof char[] && CharOperation.equals((char[]) o, (char[]) curr)) {
						index = i;
					}
				}
			}
			else {
				index = super.search(o);
			}

			return index;
		}
	}
}