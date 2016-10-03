/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Erling Ellingsen -  patch for bug 125570
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.lookup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.wst.jsdt.core.LibrarySuperType;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.compiler.libraries.SystemLibraryLocation;
import org.eclipse.wst.jsdt.core.infer.InferEngine;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.core.infer.InferrenceManager;
import org.eclipse.wst.jsdt.core.infer.InferrenceProvider;
import org.eclipse.wst.jsdt.internal.codeassist.ISearchRequestor;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.Assignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.FunctionExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ImportReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.wst.jsdt.internal.compiler.util.CompoundNameVector;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfType;
import org.eclipse.wst.jsdt.internal.compiler.util.ObjectVector;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleNameVector;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleSetOfCharArray;
import org.eclipse.wst.jsdt.internal.core.SearchableEnvironment;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IIndexConstants;
import org.eclipse.wst.jsdt.internal.core.util.QualificationHelpers;
import org.eclipse.wst.jsdt.internal.core.util.Util;

public class CompilationUnitScope extends BlockScope {

	public LookupEnvironment environment;
	public CompilationUnitDeclaration referenceContext;
	public char[][] currentPackageName;
	public PackageBinding fPackage;
	public ImportBinding[] imports;
	public HashtableOfObject typeOrPackageCache; // used in Scope.getTypeOrPackage()

	public SourceTypeBinding[] topLevelTypes;
	public SourceTypeBinding[] existingTopLevelTypes;

	private CompoundNameVector qualifiedReferences;
	private SimpleNameVector simpleNameReferences;
	private ObjectVector referencedTypes;
	private ObjectVector referencedSuperTypes;

	HashtableOfType constantPoolNameUsage;
	public int analysisIndex;
	private int captureID = 1;

	/* Allows a compilation unit to inherit fields from a superType */
	public ReferenceBinding superBinding;
	
	/**
	 * <p>
	 * <code>true</code> if the {@link #superBinding} is currently being
	 * built, <code>false</code> otherwise.
	 * </p>
	 * 
	 * @see #buildSuperType()
	 * @see #fBuidingSuperBindingLock
	 */
	private volatile boolean fBuildingSuperBinding;
	
	/**
	 * <p>
	 * Lock that should be used when using the {@link #fBuildingSuperBinding}
	 * property.
	 * </p>
	 */
	private final Object fBuidingSuperBindingLock = new Object();
	
	/**
	 * boolean flag to determine if we need to build the
	 * Global Super Type for this scope.
	 */
	private boolean shouldBuildGlobalSuperType = false;
	
	private ClassScope classScope;

	public int temporaryAnalysisIndex;

	public HashSet externalCompilationUnits = new HashSet();

	public static final char FILENAME_DOT_SUBSTITUTION = '#';

	class DeclarationVisitor extends ASTVisitor {
		ArrayList methods = new ArrayList();

		public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
			if(localDeclaration.initialization instanceof FunctionExpression) {
				this.visit(((FunctionExpression) localDeclaration.initialization).getMethodDeclaration(), scope);
			} else {
				TypeBinding type = localDeclaration.resolveVarType(scope);
				LocalVariableBinding binding = new LocalVariableBinding(localDeclaration, type, 0, false);
				localDeclaration.binding = binding;
				addLocalVariable(binding);
			}
			return false;
		}
		
		/**
		 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.Assignment, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
		 */
		public boolean visit(Assignment assignment, BlockScope scope) {
			/* If assigning to single name reference and there is no existing local
			 * variable binding for that reference, create one.  This is for the case
			 * where their is no variable declaration in the compilation unit but the
			 * variable is assigned to.
			 */
			if(assignment.lhs instanceof SingleNameReference) {
				SingleNameReference ref = (SingleNameReference)assignment.lhs;
				
				//only create new binding if one does not already exist
				LocalVariableBinding existingBinding = scope.compilationUnitScope().findVariable(ref.getToken());
				if(existingBinding == null) {
					LocalDeclaration localDeclaration = new LocalDeclaration(ref.getToken(),ref.sourceStart,ref.sourceEnd);
					localDeclaration.inferredType = assignment.getInferredType();
					
					TypeBinding binding = null;
					if(localDeclaration.inferredType != null) {
						binding = localDeclaration.inferredType.resolveType(scope.compilationUnitScope(), assignment);
					}
					
					LocalVariableBinding localBinding = new LocalVariableBinding(localDeclaration, binding, 0, false);
				    scope.compilationUnitScope().addLocalVariable(localBinding);
				}
			}

			return super.visit(assignment, scope);
		}

		public boolean visit(MethodDeclaration methodDeclaration, Scope parentScope) {
			// do not visit functions that are defined in another type, they will be visisted in
			// that type
			if(methodDeclaration.inferredMethod == null || methodDeclaration.inferredMethod.inType == null || methodDeclaration.inferredMethod.isConstructor) {
				char[] selector = methodDeclaration.getName();
				boolean isConstructor = false;

				if(methodDeclaration.inferredMethod != null && methodDeclaration.inferredMethod.isConstructor) {
					isConstructor = true;
				}

				MethodScope scope = new MethodScope(parentScope, methodDeclaration, false);
				if(selector != null && !methodDeclaration.hasBinding()) {
					MethodBinding methodBinding =
							scope.createMethod(methodDeclaration, selector, referenceContext.compilationUnitBinding,
									isConstructor, false);
					
					// is null if binding could not be created
					if(methodBinding != null && methodBinding.selector != null) {
						methods.add(methodBinding);
					}
					if(methodBinding.selector != null) {
						environment.defaultPackage.addBinding(methodBinding, methodBinding.selector, Binding.METHOD);
						fPackage.addBinding(methodBinding, methodBinding.selector, Binding.METHOD);
					}
					methodDeclaration.setBinding(methodBinding);
				} else {
					methodDeclaration.setScope(scope);
				}
				if(fPackage != environment.defaultPackage) {
					fPackage.addBinding(referenceContext.compilationUnitBinding, referenceContext.getMainTypeName(),
							Binding.COMPILATION_UNIT);
				}
				
				methodDeclaration.bindArguments();
			}
			return false;
		}
	}

	public CompilationUnitScope(CompilationUnitDeclaration unit, LookupEnvironment environment) {
		super(COMPILATION_UNIT_SCOPE, null);

		this.environment = environment;
		this.referenceContext = unit;
		unit.scope = this;
		
		char[][] pkgName =
				unit.currentPackage == null ? (unit.compilationResult != null ? unit.compilationResult.getPackageName()
						: null) : unit.currentPackage.tokens;
		this.currentPackageName = pkgName == null ? CharOperation.NO_CHAR_CHAR : pkgName;

		this.referencedTypes = new ObjectVector();
		if(compilerOptions().produceReferenceInfo) {
			this.qualifiedReferences = new CompoundNameVector();
			this.simpleNameReferences = new SimpleNameVector();
			this.referencedSuperTypes = new ObjectVector();
		} else {
			this.qualifiedReferences = null; // used to test if dependencies should be recorded
			this.simpleNameReferences = null;
			this.referencedSuperTypes = null;
		}

		this.fBuildingSuperBinding = false;
	}

	protected CompilationUnitScope(LookupEnvironment environment) {
		super(COMPILATION_UNIT_SCOPE, null);
		this.environment = environment;

		this.referencedTypes = new ObjectVector();
		if(compilerOptions().produceReferenceInfo) {
			this.qualifiedReferences = new CompoundNameVector();
			this.simpleNameReferences = new SimpleNameVector();
			this.referencedSuperTypes = new ObjectVector();
		} else {
			this.qualifiedReferences = null; // used to test if dependencies should be recorded
			this.simpleNameReferences = null;
			this.referencedSuperTypes = null;
		}
		
		this.fBuildingSuperBinding = false;
	}

	public ClassScope classScope() {
		if(this.classScope != null)
			return this.classScope;
		return super.classScope();
	}

	void buildFieldsAndMethods() {
		for(int i = 0, length = topLevelTypes.length; i < length; i++) {
			if(topLevelTypes[i] != null) {
				topLevelTypes[i].buildFieldsAndMethods();
			}
		}
	}

	void buildTypeBindings(AccessRestriction accessRestriction) {
		buildTypeBindings(CharOperation.NO_CHAR_CHAR, accessRestriction);
	}

	void buildTypeBindings(char[][] restrictToNames, AccessRestriction accessRestriction) {
		existingTopLevelTypes = topLevelTypes;
		topLevelTypes = new SourceTypeBinding[0]; // want it initialized if the package cannot be
													// resolved
		if(referenceContext.compilationResult.compilationUnit != null) {
			char[][] expectedPackageName = referenceContext.compilationResult.compilationUnit.getPackageName();
			if(expectedPackageName != null && !CharOperation.equals(currentPackageName, expectedPackageName)) {
				currentPackageName = expectedPackageName.length == 0 ? CharOperation.NO_CHAR_CHAR : expectedPackageName;
			}
		}
		if(currentPackageName == CharOperation.NO_CHAR_CHAR) {
			fPackage = environment.defaultPackage;
		} else {
			fPackage = environment.createPackage(currentPackageName);
		}

		this.faultInImports();

		// Skip typeDeclarations which know of previously reported errors
		int typeLength = referenceContext.numberInferredTypes;
		
		List newlyBuiltTypes = new ArrayList();

		SimpleSetOfCharArray addTypes = new SimpleSetOfCharArray(10);
		boolean shouldTraverse = true;
		String fileName = new String(this.referenceContext.getFileName());
		// do an initial pass through the types to be built and add their super types to the list, so they get
		// built in the event they are anonymous.
		for(int i = 0; i < typeLength; i++) {
			InferredType typeDecl = referenceContext.inferredTypes[i];

			if(typeDecl.isDefinition()) {
				if (restrictToNames != null && restrictToNames.length > 0) {
					boolean continueBuilding = false;
					for(int j = 0; !continueBuilding && j < restrictToNames.length; j++) {
						if(CharOperation.equals(typeDecl.getName(), restrictToNames[j]))
							continueBuilding = true;
					}
					if(continueBuilding && typeDecl.getSuperType() != null && !typeDecl.getSuperType().isIndexed()) {
						System.arraycopy(restrictToNames, 0, restrictToNames = new char[restrictToNames.length + 1][], 0, restrictToNames.length - 1);
						restrictToNames[restrictToNames.length - 1] = typeDecl.getSuperClassName();
					}
				}
			}
		}
		nextType: for(int i = 0; i < typeLength; i++) {
			InferredType typeDecl = referenceContext.inferredTypes[i];

			if(typeDecl.isDefinition()) {
				if (restrictToNames != null && restrictToNames.length > 0) {
					boolean continueBuilding = false;
					for(int j = 0; !continueBuilding && j < restrictToNames.length; j++) {
						if(CharOperation.equals(typeDecl.getName(), restrictToNames[j]))
							continueBuilding = true;
					}
					if(!continueBuilding)
						continue nextType;

				}
				ReferenceBinding typeBinding = null;
				// check that the type is not already built
				if(existingTopLevelTypes != null && restrictToNames != null && restrictToNames.length > 0) {
					for(int j = 0; j < existingTopLevelTypes.length; j++) {
						/* use #sourceName here because it does not check through all the linked types
						 * for SourceTypeBindings, which in this case we do not want to do because it is
						 * not needed and is a performance drag */
						if(existingTopLevelTypes[j] != null && CharOperation.equals(typeDecl.getName(), existingTopLevelTypes[j].sourceName())) {
							typeBinding = environment.defaultPackage.getType0(typeDecl.getName());
							if(typeBinding == null) {
								environment.defaultPackage.addType(existingTopLevelTypes[j]);
								fPackage.addType(existingTopLevelTypes[j]);
							}
							shouldTraverse = false;
							continue nextType;
						}
					}
				}
				
				shouldTraverse = false;
				
				// build the type and its synonyms
				SourceTypeBinding originalSourceType = null;
				int numberOfTypesToBuild = 1 + (referenceContext.inferredTypes[i].getSynonyms() == null ? 0 : referenceContext.inferredTypes[i].getSynonyms().length);
				for (int j = 0; j < numberOfTypesToBuild; j++) {
					if (j > 0) {
						// main type has been built, now build associated synonyms
						typeDecl = referenceContext.inferredTypes[i].getSynonyms()[j - 1];
						if(typeDecl.binding != null)
							break;
					}
					
					typeBinding = environment.defaultPackage.getType0(typeDecl.getName());
					recordSimpleReference(typeDecl.getName()); // needed to detect collision cases
					SourceTypeBinding existingBinding = null;
					if(typeBinding != null && !(typeBinding instanceof UnresolvedReferenceBinding)) {
						/* if a type exists, it must be a valid type - cannot
						 * be a NotFound problem type unless it's an unresolved
						 * type which is now being defined
						 */
						if(typeBinding instanceof SourceTypeBinding) {
							existingBinding = (SourceTypeBinding) typeBinding;
						}
					}
					ClassScope child = new ClassScope(this, typeDecl);
					SourceTypeBinding type = child.buildInferredType(null, environment.defaultPackage, accessRestriction);
					if(type != null) {
						if (j == 0) {
							originalSourceType = type;
						}
						
						if (existingBinding != null && typeDecl.isIndexed()) {
							existingBinding.addLinkedBinding(type);
							environment.defaultPackage.addType(existingBinding);
							fPackage.addType(existingBinding);
						}
						else if(typeDecl.isIndexed()) {
							addTypes.add(typeDecl.getName());
						}
						
						// set the original type as a nextType on this synonym
						if (j > 0 && originalSourceType != null) {
							type.addLinkedBinding(originalSourceType);
						}
						newlyBuiltTypes.add(type);
					}
				}
			}
		}
		
		/* shrink topLevelTypes...
		 * happens if error reported or if only building restricted type names */
		int count = newlyBuiltTypes.size();
		topLevelTypes = (SourceTypeBinding[]) newlyBuiltTypes.toArray(new SourceTypeBinding[count]);
		
		if(existingTopLevelTypes != null && restrictToNames != null && restrictToNames.length > 0) {
			System.arraycopy(topLevelTypes, 0, topLevelTypes = new SourceTypeBinding[count + existingTopLevelTypes.length], 0, count);
			System.arraycopy(existingTopLevelTypes, 0, topLevelTypes, count, existingTopLevelTypes.length);
			existingTopLevelTypes = null;
		}
		
		/* set up context, needs to be done before super type can be built
		 * since building super type may refer back to this contexts binding */
		char[] path = CharOperation.concatWith(this.currentPackageName, '/');
		if(referenceContext.compilationUnitBinding == null) {
			referenceContext.compilationUnitBinding =
					new CompilationUnitBinding(this, environment.defaultPackage, path);
			
			if(fPackage != environment.defaultPackage)
				fPackage.addBinding(referenceContext.compilationUnitBinding, referenceContext.getMainTypeName(),
						Binding.COMPILATION_UNIT);
		}
		
		// build the super type
		if(shouldBuildGlobalSuperType())
			buildSuperType();
		
		//set the super type on the binding
		this.referenceContext.compilationUnitBinding.setSuperBinding(this.superBinding);

		//add new type bindings
		char[][] typeNames = new char[addTypes.elementSize][];
		addTypes.asArray(typeNames);
		environment.addUnitsContainingBindings(typeNames, Binding.TYPE, fileName);

		// connect synonymous types, now that their synonyms should have bindings
		for (int i = 0; i < typeLength; i++) {
			char[] inferredTypeName = referenceContext.inferredTypes[i].getName();
			
			//determine if the inferredTypeName is in the restrict to list
			boolean isResctrictToName = true;
			if (restrictToNames != null && restrictToNames.length > 0) {
				isResctrictToName = false;
				for(int j = 0; !isResctrictToName && j < restrictToNames.length; j++) {
					isResctrictToName = CharOperation.equals(inferredTypeName, restrictToNames[j]);
				}
			}
			
			//if inferred type is on restricted list and any linked synonyms
			if (isResctrictToName && referenceContext.inferredTypes[i].getSynonyms() != null) {
				ReferenceBinding binding = environment.defaultPackage.getType0(inferredTypeName);
				if (binding != null && binding instanceof SourceTypeBinding) {
					for (int j = 0; j < referenceContext.inferredTypes[i].getSynonyms().length; j++) {
						ReferenceBinding synonymBinding = environment.defaultPackage.getType0(referenceContext.inferredTypes[i].getSynonyms()[j].getName());
						if (synonymBinding != null && synonymBinding instanceof SourceTypeBinding) {
							((SourceTypeBinding) binding).addLinkedBinding((SourceTypeBinding) synonymBinding);
						}
					}
				}
			}
		}
		
		if((restrictToNames == null || restrictToNames.length == 0) || shouldTraverse) {
			if(referenceContext.compilationUnitBinding.methods == Binding.NO_METHODS) {
				DeclarationVisitor visitor = new DeclarationVisitor();
				this.referenceContext.traverse(visitor, this);
				MethodBinding[] methods =
						(MethodBinding[]) visitor.methods.toArray(new MethodBinding[visitor.methods.size()]);
				referenceContext.compilationUnitBinding.setMethods(methods);
			}
		}
	}
	
	/**
	 * <p>
	 * Builds the super type for this scope. This also includes adding the
	 * "global" type to the global type type hierarchy.
	 * </p>
	 * <p>
	 * If the super type has already been built then this is a no-op.
	 * </p>
	 */
	public void buildSuperType() {
		//be sure to only build the super once and not allow an infinite loop of building to occur
		synchronized (this.fBuidingSuperBindingLock) {
			if(this.fBuildingSuperBinding || this.superBinding != null) {
				return;
			} else {
				this.fBuildingSuperBinding = true;
			}
		}
		
		try {
			char[] superTypeName = null;
			LibrarySuperType libSuperType = null;
			if(this.referenceContext.compilationResult!=null && this.referenceContext.compilationResult.compilationUnit!=null) {
				libSuperType = this.referenceContext.compilationResult.compilationUnit.getCommonSuperType();
				if(libSuperType==null) {
					return;
				} else {
					superTypeName = libSuperType.getSuperTypeName().toCharArray();
				}
			}
			
			if (superTypeName==null) {
				return;
			}
	
			this.superBinding = findType(superTypeName, environment.defaultPackage, environment.defaultPackage);
	
			if(this.superBinding==null || !this.superBinding.isValidBinding()) {
				superTypeName = null;
				return ;
			}
	
			/* If super type is combined source type, search through SourceTypes for the specific instance */
			if( (this.superBinding instanceof SourceTypeBinding)) {
				this.classScope = ((SourceTypeBinding)this.superBinding).classScope;
			} else if(this.superBinding!=null) {
				InferredType te = this.superBinding.getInferredType();
				this.classScope = new ClassScope(this, te);
			}
			
			if(this.superBinding != null && this.classScope != null) {
				SourceTypeBinding sourceType = null;
		
				if(this.superBinding instanceof SourceTypeBinding) {
					sourceType = (SourceTypeBinding)this.superBinding;
				}
				this.classScope.buildInferredType(sourceType, this.environment.defaultPackage, null);
		
				//if there is a searchable environment then merge global fields with fields on super binding
				if(this.environment().nameEnvironment instanceof SearchableEnvironment) {
					//find all of the global fields from the index
					SearchableEnvironment env = (SearchableEnvironment)this.environment().nameEnvironment;
					final HashtableOfObject globalFields = new HashtableOfObject();
					env.findVariables(null, new char[][]{IIndexConstants.GLOBAL_SYMBOL}, false, new ISearchRequestor() {
						
						public void acceptVariable(char[] signature, char[] typeQualification, char[] typeSimpleName, char[] declaringQualification, char[] declaringSimpleName, int modifiers, String path) {
							//store global field and its type name
							globalFields.put(signature, QualificationHelpers.createFullyQualifiedName(typeQualification, typeSimpleName));
						}
						
						public void acceptType(char[] packageName, char[] fileName, char[] typeName, char[][] enclosingTypeNames, int modifiers, AccessRestriction accessRestriction) {
							//ignore
						}
						
						public void acceptPackage(char[] packageName) {
							//ignore
						}
						
						public void acceptFunction(char[] signature, char[][] parameterFullyQualifedTypeNames, char[][] parameterNames, char[] returnQualification, char[] returnSimpleName, char[] declaringQualification, char[] declaringSimpleName, int modifiers, String path) {
							//ignore
						}
						
						public void acceptConstructor(int modifiers, char[] typeName, char[][] parameterTypes, char[][] parameterNames, String path, AccessRestriction access) {
							//ignore
						}
						
						public void acceptBinding(char[] packageName, char[] fileName, char[] bindingName, int bindingType, int modifiers, AccessRestriction accessRestriction) {
							//ignore
						}
					});
					
					//merge the global fields with the super binding fields
					mergeWithSuperBinding(globalFields);
				}
				
				recordTypeReference(this.superBinding);
				recordSuperTypeReference(this.superBinding);
				environment().setAccessRestriction(this.superBinding, null);
			}
			
			//check to see if the special "global" type was created in this scope, if so make it the super binding
			if(this.topLevelTypes != null) {
				TypeBinding globalType = null;
				for(int i = 0; i < this.topLevelTypes.length && globalType == null; ++i) {
					if(this.topLevelTypes[i] != null && CharOperation.equals(this.topLevelTypes[i].sourceName,IIndexConstants.GLOBAL_SYMBOL)) {
						globalType = this.topLevelTypes[i];
					}
				}
				
				if(globalType instanceof ReferenceBinding) {
					if(this.superBinding == null) {
						this.superBinding = (ReferenceBinding)globalType;
					} else if (this.superBinding instanceof SourceTypeBinding) {
						ReferenceBinding currentSuper = this.superBinding;
						ReferenceBinding previousSuper = this.superBinding;
						/* need to find the type with no super type or with Object as its super type,
						 * so that the "fake" Global object can be injected in there */
						while(currentSuper != null && !CharOperation.equals(currentSuper.sourceName, IIndexConstants.OBJECT)) {
							previousSuper = currentSuper;
							if(currentSuper instanceof SourceTypeBinding) {
								currentSuper = ((SourceTypeBinding)currentSuper).getSuperBinding0();
							}
							else {
								break;
							}
						}
						
						// if we found the null class just add the global object
						if(currentSuper == null) {
							if(!CharOperation.equals(previousSuper.sourceName, globalType.sourceName())) {
								((SourceTypeBinding)previousSuper).setSuperBinding((ReferenceBinding)globalType);
							}
						}
						else if(CharOperation.equals(currentSuper.sourceName, IIndexConstants.OBJECT)){
							// if we found the object case, set object as the parent of global first
							((SourceTypeBinding)globalType).setSuperBinding(currentSuper);
							
							if(!CharOperation.equals(previousSuper.sourceName, globalType.sourceName())) {
								((SourceTypeBinding)previousSuper).setSuperBinding((ReferenceBinding)globalType);
							}
						}
					}
				}
			}
		} finally {
			//finished building super
			synchronized (this.fBuidingSuperBindingLock) {
				this.fBuildingSuperBinding = false;
			}
		}
	}
	
	/**
	 * <p>
	 * Merges the table of global field names to their types with the fields of the super binding.
	 * </p>
	 * 
	 * @param globalFields table of global field names to their types to merge with the fields of
	 * the super binding
	 */
	private void mergeWithSuperBinding(HashtableOfObject globalFields) {
		//list of the super fields to iterate through
		List superBindingFields = new ArrayList();
		superBindingFields.addAll(Arrays.asList(this.superBinding.fields()));
		
		//set of the super field names so that duplicates do not get added to the list
		SimpleSetOfCharArray superBindingFieldsNames = new SimpleSetOfCharArray(superBindingFields.size());
		for(int i = 0; i < superBindingFields.size(); ++i) {
			superBindingFieldsNames.add(((FieldBinding)superBindingFields.get(i)).name);
		}
		
		/* for each super binding field check if there is an existing global
		 * field with the same name to merge it with */
		for(int superBindingFieldIndex = 0;
					superBindingFieldIndex < superBindingFields.size();
					++superBindingFieldIndex) {
			
			FieldBinding superBindingField = (FieldBinding)superBindingFields.get(superBindingFieldIndex);
			
			//check if there is an existing global field with the same name as the super type field
			char[] globalFieldType = (char[])globalFields.get(superBindingField.name);
			
			/* if no global field then guess that their might be an anonymous
			 * global type for the super binding.
			 * 
			 * IE:
			 * navigator.foo = 42
			 * 
			 * This creates a global anonymous type for navigator but does not
			 * add it to the global type for performance reasons. */
			if(globalFieldType == null || globalFieldType.length == 0) {
				globalFieldType = InferEngine.createAnonymousGlobalTypeName(superBindingField.name);
			}
				
			/* if the type for the global field can be found and is valid then
			 * merge it with the super binding field */
			ReferenceBinding globalFieldBinding = this.findType(globalFieldType,
						this.getCurrentPackage(), this.getCurrentPackage());
			if(globalFieldBinding != null &&
					globalFieldBinding instanceof SourceTypeBinding && 
					globalFieldBinding.isValidBinding() &&
					!globalFieldBinding.isAnyType()) {
				
				/* if the type of the super binding field is the same as the super binding
				 * then the fields of the super binding field should also be considered
				 * super binding fields
				 * 
				 * IE: there is a field on the Window type named window that has a
				 * type of Window.  Thus any fields of the window field also need to be
				 * merged with the super binding (this example is assuming window is the super
				 * type) */
				if(superBindingField.type != null && superBindingField.type.isEquivalentTo(this.superBinding)) {
					FieldBinding[] fieldsOnGlobalField = globalFieldBinding.fields();
					for(int fieldsOnGlobalFieldIndex = 0;
								fieldsOnGlobalFieldIndex < fieldsOnGlobalField.length;
								++fieldsOnGlobalFieldIndex) {
						
						//only add if field with name that is not already added
						if(!superBindingFieldsNames.includes(fieldsOnGlobalField[fieldsOnGlobalFieldIndex].name)) {
							superBindingFields.add(fieldsOnGlobalField[fieldsOnGlobalFieldIndex]);
							superBindingFieldsNames.add(fieldsOnGlobalField[fieldsOnGlobalFieldIndex].name);
						}
					}
				}
				
				/* if the super field type is a SourceTypeBinding then link it with that of the global field
				 * else if the super field does not have a type or is the any type set it to be the type
				 * of the global field */
				if(superBindingField.type != null && superBindingField.type instanceof SourceTypeBinding) {
					((SourceTypeBinding)superBindingField.type).addLinkedBinding((SourceTypeBinding)globalFieldBinding);
				} else if(superBindingField.type == null || superBindingField.type.isAnyType() ){
					superBindingField.type = globalFieldBinding;
				}
			}
		}
	}

	SourceTypeBinding buildType(InferredType inferredType, SourceTypeBinding enclosingType,
			PackageBinding packageBinding, AccessRestriction accessRestriction) {
		
		// provide the typeDeclaration with needed scopes
		if(enclosingType == null) {
			char[][] className = CharOperation.arrayConcat(packageBinding.compoundName, inferredType.getName());
			inferredType.binding = new SourceTypeBinding(className, packageBinding, this);

			// @GINO: Anonymous set bits
			if(!inferredType.isNamed())
				inferredType.binding.tagBits |= TagBits.AnonymousTypeMask;

		}

		SourceTypeBinding sourceType = inferredType.binding;
		environment().setAccessRestriction(sourceType, accessRestriction);
		environment().defaultPackage.addType(sourceType);
		sourceType.fPackage.addType(sourceType);
		return sourceType;
	}

	public PackageBinding getDefaultPackage() {
		return environment.defaultPackage;
	}

	public void addLocalVariable(LocalVariableBinding binding) {
		super.addLocalVariable(binding);
		environment.defaultPackage.addBinding(binding, binding.name, Binding.VARIABLE);
		fPackage.addBinding(binding, binding.name, Binding.VARIABLE);
	}

	void checkAndSetImports() {
		if(referenceContext.imports == null) {
			imports = getDefaultImports();
			return;
		}

		// allocate the import array, add java.lang.* by default
		int numberOfStatements = referenceContext.imports.length;
		int numberOfImports = numberOfStatements + 1;
		for(int i = 0; i < numberOfStatements; i++) {
			ImportReference importReference = referenceContext.imports[i];
			if(((importReference.bits & ASTNode.OnDemand) != 0)
					&& CharOperation.equals(JAVA_LANG, importReference.tokens)) {
				numberOfImports--;
				break;
			}
		}
		ImportBinding[] resolvedImports = new ImportBinding[numberOfImports];
		resolvedImports[0] = getDefaultImports()[0];
		int index = 1;

		nextImport: for(int i = 0; i < numberOfStatements; i++) {
			ImportReference importReference = referenceContext.imports[i];
			char[][] compoundName = importReference.tokens;

			// skip duplicates or imports of the current package
			for(int j = 0; j < index; j++) {
				ImportBinding resolved = resolvedImports[j];
				if(resolved.onDemand == ((importReference.bits & ASTNode.OnDemand) != 0))
					if(CharOperation.equals(compoundName, resolvedImports[j].compoundName))
						continue nextImport;
			}

			if((importReference.bits & ASTNode.OnDemand) != 0) {
				if(CharOperation.equals(compoundName, currentPackageName))
					continue nextImport;

				Binding importBinding = findImport(compoundName, compoundName.length);
				if(!importBinding.isValidBinding())
					continue nextImport; // we report all problems in faultInImports()
				resolvedImports[index++] = new ImportBinding(compoundName, true, importBinding, importReference);
			} else {
				// resolve single imports only when the last name matches
				resolvedImports[index++] = new ImportBinding(compoundName, false, null, importReference);
			}
		}

		// shrink resolvedImports... only happens if an error was reported
		if(resolvedImports.length > index)
			System.arraycopy(resolvedImports, 0, resolvedImports = new ImportBinding[index], 0, index);
		imports = resolvedImports;
	}

	/* INTERNAL USE-ONLY
	 * Innerclasses get their name computed as they are generated, since some may not
	 * be actually outputed if sitting inside unreachable code. */
	public char[] computeConstantPoolName(LocalTypeBinding localType) {
		if(localType.constantPoolName() != null) {
			return localType.constantPoolName();
		}
		// delegates to the outermost enclosing classfile, since it is the only one with a global
		// vision of its innertypes.

		if(constantPoolNameUsage == null)
			constantPoolNameUsage = new HashtableOfType();

		ReferenceBinding outerMostEnclosingType = localType.scope.outerMostClassScope().enclosingSourceType();

		// ensure there is not already such a local type name defined by the user
		int index = 0;
		char[] candidateName;
		boolean isCompliant15 = compilerOptions().complianceLevel >= ClassFileConstants.JDK1_5;
		while(true) {
			if(localType.isMemberType()) {
				if(index == 0) {
					candidateName =
							CharOperation.concat(localType.enclosingType().constantPoolName(), localType.sourceName,
									'.');
				} else {
					// in case of collision, then member name gets extra $1 inserted
					// e.g. class X { { class L{} new X(){ class L{} } } }
					candidateName =
							CharOperation.concat(localType.enclosingType().constantPoolName(), '.', String.valueOf(
									index).toCharArray(), '.', localType.sourceName);
				}
			} else if(localType.isAnonymousType()) {
				if(isCompliant15) {
					// from 1.5 on, use immediately enclosing type name
					candidateName =
							CharOperation.concat(localType.enclosingType.constantPoolName(),
									String.valueOf(index + 1).toCharArray(), '.');
				} else {
					candidateName =
							CharOperation.concat(outerMostEnclosingType.constantPoolName(),
									String.valueOf(index + 1).toCharArray(), '.');
				}
			} else {
				// local type
				if(isCompliant15) {
					candidateName =
							CharOperation.concat(CharOperation.concat(localType.enclosingType().constantPoolName(),
									String.valueOf(index + 1).toCharArray(), '.'), localType.sourceName);
				} else {
					candidateName =
							CharOperation.concat(outerMostEnclosingType.constantPoolName(), '.', String.valueOf(
									index + 1).toCharArray(), '.', localType.sourceName);
				}
			}
			if(constantPoolNameUsage.get(candidateName) != null) {
				index++;
			} else {
				constantPoolNameUsage.put(candidateName, localType);
				break;
			}
		}
		return candidateName;
	}

	void connectTypeHierarchy(char[][] typeNames) {
		if(classScope != null) {
			classScope.connectTypeHierarchy();
		}
		nextType: for(int i = 0; i < referenceContext.numberInferredTypes; i++) {
			InferredType inferredType = referenceContext.inferredTypes[i];
			if(typeNames.length > 0) {
				boolean continueBuilding = false;
				for(int j = 0; !continueBuilding && j < typeNames.length; j++) {
					if(CharOperation.equals(inferredType.getName(), typeNames[j]))
						continueBuilding = true;
				}
				if(!continueBuilding)
					continue nextType;
			}
			if(inferredType.binding != null && inferredType.binding.classScope != null) {
				inferredType.binding.classScope.connectTypeHierarchy();
			}
		}
	}

	void connectTypeHierarchy() {
		connectTypeHierarchy(CharOperation.NO_CHAR_CHAR);
	}

	void faultInImports() {
		if(this.typeOrPackageCache != null)
			return; // can be called when a field constant is resolved before static imports
		if(referenceContext.imports == null) {
			this.typeOrPackageCache = new HashtableOfObject(1);
			return;
		}

		// collect the top level type names if a single type import exists
		int numberOfStatements = referenceContext.imports.length;
		HashtableOfType typesBySimpleNames = null;
		for(int i = 0; i < numberOfStatements; i++) {
			if((referenceContext.imports[i].bits & ASTNode.OnDemand) == 0) {
				typesBySimpleNames = new HashtableOfType(topLevelTypes.length + numberOfStatements);
				for(int j = 0, length = topLevelTypes.length; j < length; j++)
					typesBySimpleNames.put(topLevelTypes[j].sourceName, topLevelTypes[j]);
				break;
			}
		}

		// allocate the import array, add java.lang.* by default
		ImportBinding[] defaultImports = getDefaultImports();
		int numberOfImports = numberOfStatements + defaultImports.length;
		for(int i = 0; i < numberOfStatements; i++) {
			ImportReference importReference = referenceContext.imports[i];
			if(((importReference.bits & ASTNode.OnDemand) != 0)
					&& CharOperation.equals(JAVA_LANG, importReference.tokens)) {
				numberOfImports--;
				break;
			}
		}
		ImportBinding[] resolvedImports = new ImportBinding[numberOfImports];
		System.arraycopy(defaultImports, 0, resolvedImports, 0, defaultImports.length);
		int index = defaultImports.length;

		// keep static imports with normal imports until there is a reason to split them up
		// on demand imports continue to be packages & types. need to check on demand type imports
		// for fields/methods
		// single imports change from being just types to types or fields
		nextImport: for(int i = 0; i < numberOfStatements; i++) {
			ImportReference importReference = referenceContext.imports[i];
			char[][] compoundName = importReference.tokens;
			if (compoundName.length == 0) {
				continue nextImport;
			}

			// skip duplicates or imports of the current package
			for(int j = 0; j < index; j++) {
				ImportBinding resolved = resolvedImports[j];
				if(resolved.onDemand == ((importReference.bits & ASTNode.OnDemand) != 0)) {
					if(CharOperation.equals(compoundName, resolved.compoundName)) {
						continue nextImport;
					}
				}
			}
			if((importReference.bits & ASTNode.OnDemand) != 0) {
				if(CharOperation.equals(compoundName, currentPackageName)) {
					continue nextImport;
				}

				Binding importBinding = findImport(compoundName, compoundName.length);
				if(!importBinding.isValidBinding()) {
					continue nextImport;
				}
				resolvedImports[index++] = new ImportBinding(compoundName, true, importBinding, importReference);
			} else {
				Binding importBinding = findSingleImport(compoundName);
				if(!importBinding.isValidBinding()) {
					continue nextImport;
				}
				ReferenceBinding conflictingType = null;
				if(importBinding instanceof MethodBinding) {
					conflictingType = (ReferenceBinding) getType(compoundName, compoundName.length);
					if(!conflictingType.isValidBinding())
						conflictingType = null;
				}
				// collisions between an imported static field & a type should be checked according
				// to spec... but currently not by javac
				if(importBinding instanceof ReferenceBinding || conflictingType != null) {
					ReferenceBinding referenceBinding =
							conflictingType == null ? (ReferenceBinding) importBinding : conflictingType;
					if(importReference.isTypeUseDeprecated(referenceBinding, this))
						problemReporter().deprecatedType(referenceBinding, importReference);

					ReferenceBinding existingType = typesBySimpleNames.get(compoundName[compoundName.length - 1]);
					if(existingType != null) {
						continue nextImport;
					}
					typesBySimpleNames.put(compoundName[compoundName.length - 1], referenceBinding);
				}
				resolvedImports[index++] =
						conflictingType == null ? new ImportBinding(compoundName, false, importBinding, importReference)
								: new ImportConflictBinding(compoundName, importBinding, conflictingType,
										importReference);
			}
		}

		// shrink resolvedImports... only happens if an error was reported
		if(resolvedImports.length > index)
			System.arraycopy(resolvedImports, 0, resolvedImports = new ImportBinding[index], 0, index);
		imports = resolvedImports;

		int length = imports.length;
		this.typeOrPackageCache = new HashtableOfObject(length);
		for(int i = 0; i < length; i++) {
			ImportBinding binding = imports[i];
			if(!binding.onDemand && binding.resolvedImport instanceof ReferenceBinding
					|| binding instanceof ImportConflictBinding)
				this.typeOrPackageCache.put(binding.compoundName[binding.compoundName.length - 1], binding);
		}
	}

	public void faultInTypes() {
		faultInImports();

		this.referenceContext.compilationUnitBinding.faultInTypesForFieldsAndMethods();
		for(int i = 0, length = topLevelTypes.length; i < length; i++)
			topLevelTypes[i].faultInTypesForFieldsAndMethods();
	}

	/**
	 * this API is for code assist purpose
	 * 
	 * @param compoundName
	 * @param onDemand
	 * @return
	 */
	public Binding findImport(char[][] compoundName, boolean onDemand) {
		if(onDemand) {
			return findImport(compoundName, compoundName.length);
		} else {
			return findSingleImport(compoundName);
		}
	}

	private Binding findImport(char[][] compoundName, int length) {
		recordQualifiedReference(compoundName);

		Binding binding = environment.getTopLevelPackage(compoundName[0]);
		int i = 1;
		foundNothingOrType: if(binding != null) {
			PackageBinding packageBinding = (PackageBinding) binding;
			while(i < length) {
				int type = (i + 1 == length) ? Binding.COMPILATION_UNIT : Binding.PACKAGE;
				binding = packageBinding.getTypeOrPackage(compoundName[i++], type);
				if(binding == null || !binding.isValidBinding()) {
					binding = null;
					break foundNothingOrType;
				}
				if(i == length && (binding instanceof CompilationUnitBinding))
					return binding;
				if(!(binding instanceof PackageBinding))
					break foundNothingOrType;

				packageBinding = (PackageBinding) binding;
			}
			return packageBinding;
		}

		ReferenceBinding type;
		if(binding == null) {
			if(environment.defaultPackage == null || compilerOptions().complianceLevel >= ClassFileConstants.JDK1_4)
				return new ProblemReferenceBinding(CharOperation.subarray(compoundName, 0, i), null,
						ProblemReasons.NotFound);
			type = findType(compoundName[0], environment.defaultPackage, environment.defaultPackage);
			if(type == null || !type.isValidBinding())
				return new ProblemReferenceBinding(CharOperation.subarray(compoundName, 0, i), null,
						ProblemReasons.NotFound);
			i = 1; // reset to look for member types inside the default package type
		} else {
			type = (ReferenceBinding) binding;
		}

		while(i < length) {
			if(!type.canBeSeenBy(environment.defaultPackage))
				return new ProblemReferenceBinding(CharOperation.subarray(compoundName, 0, i), type,
						ProblemReasons.NotVisible);

			char[] name = compoundName[i++];
			// does not look for inherited member types on purpose, only immediate members
			type = type.getMemberType(name);
			if(type == null)
				return new ProblemReferenceBinding(CharOperation.subarray(compoundName, 0, i), null,
						ProblemReasons.NotFound);
		}
		if(!type.canBeSeenBy(environment.defaultPackage))
			return new ProblemReferenceBinding(compoundName, type, ProblemReasons.NotVisible);
		return type;
	}

	private Binding findSingleImport(char[][] compoundName) {
		if(compoundName.length == 1) {
			// findType records the reference
			// the name cannot be a package
			if(environment.defaultPackage == null || compilerOptions().complianceLevel >= ClassFileConstants.JDK1_4)
				return new ProblemReferenceBinding(compoundName, null, ProblemReasons.NotFound);
			ReferenceBinding typeBinding =
					findType(compoundName[0], environment.defaultPackage, environment.defaultPackage);
			if(typeBinding == null)
				return new ProblemReferenceBinding(compoundName, null, ProblemReasons.NotFound);
			return typeBinding;
		}

		return findImport(compoundName, compoundName.length);
	}

	MethodBinding findStaticMethod(ReferenceBinding currentType, char[] selector) {
		if(!currentType.canBeSeenBy(this))
			return null;

		do {
			MethodBinding[] methods = currentType.getMethods(selector);
			if(methods != Binding.NO_METHODS) {
				for(int i = methods.length; --i >= 0;) {
					MethodBinding method = methods[i];
					if(method.isStatic() && method.canBeSeenBy(environment.defaultPackage))
						return method;
				}
			}

			((SourceTypeBinding) currentType).classScope.connectTypeHierarchy();
		} while((currentType = currentType.getSuperBinding()) != null);
		return null;
	}

	ImportBinding[] getDefaultImports() {
		// initialize the default imports if necessary... share the default java.lang.* import
		Binding importBinding = environment.defaultPackage;

		// abort if java.lang cannot be found...
		if(importBinding == null || !importBinding.isValidBinding()) {
			// create a proxy for the missing BinaryType
			MissingBinaryTypeBinding missingObject =
					environment.cacheMissingBinaryType(JAVA_LANG_OBJECT, this.referenceContext);
			importBinding = missingObject.fPackage;
		}
		ImportBinding systemJSBinding = null;
		if(environment.defaultImports != null) {
			systemJSBinding = environment.defaultImports[0];
		} else {
			systemJSBinding =
					new ImportBinding(new char[][] { SystemLibraryLocation.SYSTEM_LIBARAY_NAME }, true, importBinding,
							(ImportReference) null);
			environment.defaultImports = new ImportBinding[] { systemJSBinding };
		}

		ImportBinding[] defaultImports = null;
		String[] contextIncludes = null;
		InferrenceProvider[] inferenceProviders =
				InferrenceManager.getInstance().getInferenceProviders(this.referenceContext);
		if(inferenceProviders != null && inferenceProviders.length > 0) {
			for(int i = 0; i < inferenceProviders.length; i++) {
				if(contextIncludes == null) {
					contextIncludes = inferenceProviders[i].getResolutionConfiguration().getContextIncludes();
				} else {
					String[] contextIncludesTemp =
							inferenceProviders[0].getResolutionConfiguration().getContextIncludes();
					if(contextIncludesTemp != null) {
						String[] contextIncludesOld = contextIncludes;
						contextIncludes = new String[contextIncludesTemp.length + contextIncludesOld.length];
						System.arraycopy(contextIncludesOld, 0, contextIncludes, 0, contextIncludesOld.length);
						System.arraycopy(contextIncludesTemp, 0, contextIncludes, contextIncludesOld.length - 1,
								contextIncludesTemp.length);
					}
				}
			}

		}
		if(contextIncludes != null && contextIncludes.length > 0) {
			ArrayList list = new ArrayList();
			list.add(systemJSBinding);
			for(int i = 0; i < contextIncludes.length; i++) {
				String include = contextIncludes[i];
				if(include != null) {
					int index = Util.indexOfJavaLikeExtension(include);
					if(index >= 0)
						include = include.substring(0, index);
					include = include.replace('.', FILENAME_DOT_SUBSTITUTION);
					char[][] qualifiedName = CharOperation.splitOn('/', include.toCharArray());
					Binding binding = findImport(qualifiedName, qualifiedName.length);
					if(binding.isValidBinding()) {
						list.add(new ImportBinding(qualifiedName, true, binding, null));
					}
				}
			}
			defaultImports = (ImportBinding[]) list.toArray(new ImportBinding[list.size()]);
		} else
			defaultImports = new ImportBinding[] { systemJSBinding };
		return defaultImports;
	}


	/**
	 * <p><b>NOTE:</b> NOT Public API</p>
	 * 
	 * @param compoundName
	 * @param onDemand
	 * @return
	 */
	public final Binding getImport(char[][] compoundName, boolean onDemand) {
		if(onDemand)
			return findImport(compoundName, compoundName.length);
		return findSingleImport(compoundName);
	}

	public int nextCaptureID() {
		return this.captureID++;
	}

	/**
	 * Answer the problem reporter to use for raising new problems.
	 * 
	 * Note that as a side-effect, this updates the current reference context
	 * (unit, type or method) in case the problem handler decides it is necessary
	 * to abort.
	 */
	public ProblemReporter problemReporter() {
		ProblemReporter problemReporter = referenceContext.problemReporter;
		problemReporter.referenceContext = referenceContext;
		return problemReporter;
	}

	/**
	 * What do we hold onto:
	 * 
	 * 1. when we resolve 'a.b.c', say we keep only 'a.b.c'
	 * & when we fail to resolve 'c' in 'a.b', lets keep 'a.b.c'
	 * THEN when we come across a new/changed/removed item named 'a.b.c',
	 * we would find all references to 'a.b.c'
	 * -> This approach fails because every type is resolved in every onDemand import to
	 * detect collision cases... so the references could be 10 times bigger than necessary.
	 * 
	 * 2. when we resolve 'a.b.c', lets keep 'a.b' & 'c'
	 * & when we fail to resolve 'c' in 'a.b', lets keep 'a.b' & 'c'
	 * THEN when we come across a new/changed/removed item named 'a.b.c',
	 * we would find all references to 'a.b' & 'c'
	 * -> This approach does not have a space problem but fails to handle collision cases.
	 * What happens if a type is added named 'a.b'? We would search for 'a' & 'b' but
	 * would not find a match.
	 * 
	 * 3. when we resolve 'a.b.c', lets keep 'a', 'a.b' & 'a', 'b', 'c'
	 * & when we fail to resolve 'c' in 'a.b', lets keep 'a', 'a.b' & 'a', 'b', 'c'
	 * THEN when we come across a new/changed/removed item named 'a.b.c',
	 * we would find all references to 'a.b' & 'c'
	 * OR 'a.b' -> 'a' & 'b'
	 * OR 'a' -> '' & 'a'
	 * -> As long as each single char[] is interned, we should not have a space problem
	 * and can handle collision cases.
	 * 
	 * 4. when we resolve 'a.b.c', lets keep 'a.b' & 'a', 'b', 'c'
	 * & when we fail to resolve 'c' in 'a.b', lets keep 'a.b' & 'a', 'b', 'c'
	 * THEN when we come across a new/changed/removed item named 'a.b.c',
	 * we would find all references to 'a.b' & 'c'
	 * OR 'a.b' -> 'a' & 'b' in the simple name collection
	 * OR 'a' -> 'a' in the simple name collection
	 * -> As long as each single char[] is interned, we should not have a space problem
	 * and can handle collision cases.
	 */
	void recordQualifiedReference(char[][] qualifiedName) {
		if(qualifiedReferences == null)
			return; // not recording dependencies

		int length = qualifiedName.length;
		if(length > 1) {
			while(!qualifiedReferences.contains(qualifiedName)) {
				qualifiedReferences.add(qualifiedName);
				if(length == 2) {
					recordSimpleReference(qualifiedName[0]);
					recordSimpleReference(qualifiedName[1]);
					return;
				}
				length--;
				recordSimpleReference(qualifiedName[length]);
				System.arraycopy(qualifiedName, 0, qualifiedName = new char[length][], 0, length);
			}
		} else if(length == 1) {
			recordSimpleReference(qualifiedName[0]);
		}
	}

	void recordReference(char[][] qualifiedEnclosingName, char[] simpleName) {
		recordQualifiedReference(qualifiedEnclosingName);
		recordSimpleReference(simpleName);
	}

	void recordReference(ReferenceBinding type, char[] simpleName) {
		ReferenceBinding actualType = typeToRecord(type);
		if(actualType != null)
			recordReference(actualType.compoundName, simpleName);
	}

	void recordSimpleReference(char[] simpleName) {
		if(simpleNameReferences == null)
			return; // not recording dependencies

		if(!simpleNameReferences.contains(simpleName))
			simpleNameReferences.add(simpleName);
	}

	void recordSuperTypeReference(TypeBinding type) {
		if(referencedSuperTypes == null)
			return; // not recording dependencies

		ReferenceBinding actualType = typeToRecord(type);
		if(actualType != null && !referencedSuperTypes.containsIdentical(actualType))
			referencedSuperTypes.add(actualType);
	}

	public void recordTypeConversion(TypeBinding superType, TypeBinding subType) {
		// must record the hierarchy of the subType that is converted to the superType
		recordSuperTypeReference(subType); 
	}

	void recordTypeReference(TypeBinding type) {
		if(referencedTypes == null)
			return; // not recording dependencies

		ReferenceBinding actualType = typeToRecord(type);
		if(actualType != null && !referencedTypes.containsIdentical(actualType))
			referencedTypes.add(actualType);
	}

	void recordTypeReferences(TypeBinding[] types) {
		if(referencedTypes == null)
			return; // not recording dependencies
		if(types == null || types.length == 0)
			return;

		for(int i = 0, max = types.length; i < max; i++) {
			// No need to record supertypes of method arguments & thrown exceptions, just the
			// compoundName
			// If a field/method is retrieved from such a type then a separate call does the job
			ReferenceBinding actualType = null;
			if(types[i] != null)
				actualType = typeToRecord(types[i]);
			if(actualType != null && !referencedTypes.containsIdentical(actualType))
				referencedTypes.add(actualType);
		}
	}

	Binding resolveSingleImport(ImportBinding importBinding) {
		if(importBinding.resolvedImport == null) {
			importBinding.resolvedImport = findSingleImport(importBinding.compoundName);
			if(!importBinding.resolvedImport.isValidBinding() || importBinding.resolvedImport instanceof PackageBinding) {
				if(this.imports != null) {
					ImportBinding[] newImports = new ImportBinding[imports.length - 1];
					for(int i = 0, n = 0, max = this.imports.length; i < max; i++)
						if(this.imports[i] != importBinding)
							newImports[n++] = this.imports[i];
					this.imports = newImports;
				}
				return null;
			}
		}
		return importBinding.resolvedImport;
	}

	public void storeDependencyInfo() {
		// add the type hierarchy of each referenced supertype
		// cannot do early since the hierarchy may not be fully resolved
		for(int i = 0; i < referencedSuperTypes.size; i++) { // grows as more types are added
			ReferenceBinding type = (ReferenceBinding) referencedSuperTypes.elementAt(i);
			if(!referencedTypes.containsIdentical(type))
				referencedTypes.add(type);

			if(!type.isLocalType()) {
				ReferenceBinding enclosing = type.enclosingType();
				if(enclosing != null)
					recordSuperTypeReference(enclosing);
			}
			ReferenceBinding superclass = type.getSuperBinding();
			if(superclass != null)
				recordSuperTypeReference(superclass);
		}

		for(int i = 0, l = referencedTypes.size; i < l; i++) {
			ReferenceBinding type = (ReferenceBinding) referencedTypes.elementAt(i);
			if(type instanceof MultipleTypeBinding) {
				ReferenceBinding[] types = ((MultipleTypeBinding) type).types;
				for(int j = 0; j < types.length; j++) {
					if(!types[j].isLocalType())
						recordQualifiedReference(types[j].isMemberType() ? CharOperation.splitOn('.',
								types[j].readableName()) : types[j].compoundName);

				}
			} else if(!type.isLocalType())
				recordQualifiedReference(type.isMemberType() ? CharOperation.splitOn('.', type.readableName())
						: type.compoundName);
		}

		int size = qualifiedReferences.size;
		char[][][] qualifiedRefs = new char[size][][];
		for(int i = 0; i < size; i++)
			qualifiedRefs[i] = qualifiedReferences.elementAt(i);
		referenceContext.compilationResult.qualifiedReferences = qualifiedRefs;

		size = simpleNameReferences.size;
		char[][] simpleRefs = new char[size][];
		for(int i = 0; i < size; i++)
			simpleRefs[i] = simpleNameReferences.elementAt(i);
		referenceContext.compilationResult.simpleNameReferences = simpleRefs;
	}

	public String toString() {
		return "--- JavaScriptUnit Scope : " + new String(referenceContext.getFileName()); //$NON-NLS-1$
	}

	private ReferenceBinding typeToRecord(TypeBinding type) {
		while(type.isArrayType())
			type = ((ArrayBinding) type).leafComponentType;

		switch(type.kind()) {
			case Binding.BASE_TYPE:
				return null;
		}
		if(type instanceof CompilationUnitBinding)
			return null;
		ReferenceBinding refType = (ReferenceBinding) type;
		if(refType.isLocalType())
			return null;
		return refType;
	}

	public void cleanup() {

		if(this.referencedTypes != null)
			for(int i = 0, l = referencedTypes.size; i < l; i++) {
				Object obj = referencedTypes.elementAt(i);
				if(obj instanceof SourceTypeBinding) {
					SourceTypeBinding type = (SourceTypeBinding) obj;
					type.cleanup();
				}
			}
	}

	public void addExternalVar(LocalVariableBinding binding) {
		externalCompilationUnits.add(binding.declaringScope.compilationUnitScope());
	}

	/**
	 * @return the shouldBuildGlobalSuperType
	 */
	public boolean shouldBuildGlobalSuperType() {
		return shouldBuildGlobalSuperType;
	}

	/**
	 * @param shouldBuildGlobalSuperType the shouldBuildGlobalSuperType to set
	 */
	public void setShouldBuildGlobalSuperType(boolean shouldBuild) {
		this.shouldBuildGlobalSuperType = shouldBuild;
	}
	
	
}
