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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;

import org.eclipse.wst.jsdt.core.ast.IAssignment;
import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.ast.IFieldReference;
import org.eclipse.wst.jsdt.core.ast.IFunctionDeclaration;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.InferredAttribute;
import org.eclipse.wst.jsdt.core.infer.InferredMethod;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Argument;
import org.eclipse.wst.jsdt.internal.compiler.ast.Assignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.wst.jsdt.internal.compiler.util.Util;

public class SourceTypeBinding extends ReferenceBinding {
	/**
	 * <p>
	 * Enable for enhanced debugging
	 * </p>
	 */
	private static final boolean DEBUG = false;
	
	/**
	 * <p>
	 * Super type binding for this binding.
	 * </p>
	 * 
	 * <p>
	 * <b>WARNING:</b> a linked binding may have a different supper binding.
	 * </p>
	 * 
	 * @see #getSuperBinding0()
	 * @see #getSuperBinding()
	 * @see #setSuperBinding(ReferenceBinding)
	 */
	private ReferenceBinding fSuperBinding;
	
	protected FieldBinding[] fields;
	protected MethodBinding[] methods;
	public ReferenceBinding[] memberTypes = Binding.NO_MEMBER_TYPES;

	public Scope scope;
	public ClassScope classScope;

	/**
	 * <p>
	 * The next type in the circular linked list of linked types.  If this type is not linked to any other
	 * types then the value of {@link #fNextType} will be this type forming a circle of 1.
	 * </p>
	 * 
	 * <p>
	 * Due to the circular nature of this linked list always use {@link #performActionOnLinkedBindings(LinkedBindingAction)}
	 * when needing to perform any action on all of the linked types.
	 * </p>
	 * 
	 * @see #performActionOnLinkedBindings(LinkedBindingAction)
	 * @see LinkedBindingAction
	 */
	private SourceTypeBinding fNextType;
	
	private static final CRC32 checksumCalculator = new CRC32();
	
	/**
	 * <code>true</code> if all fields and functions have already been built,
	 * <code>false</code> otherwise
	 */
	private boolean fHasBuiltFieldsAndMethods;
	
	/**
	 * <code>true</code> if currently building fields and functions,
	 * <code>false</code> otherwise
	 */
	private boolean fBuildingAllFieldsAndFunctions;
	
	/**
	 * <p>
	 * When building specific selectors using
	 * {@link #buildFieldsAndMethods(char[])} then this list contains all of
	 * the selectors currently being built.
	 * </p>
	 * 
	 * <p>
	 * If all fields and functions are being built at once then this list is
	 * not used. use {@link #fBuildingAllFieldsAndFunctions} to detect this situation.
	 * </p>
	 * 
	 * @see #buildFieldsAndMethods(char[])
	 * @see #fBuildingAllFieldsAndFunctions
	 */
	private char[][] fBuildingSelectors;
	
	/**
	 * <p>
	 * If have built specific selectors using
	 * {@link #buildFieldsAndMethods(char[])} then this list contains all of
	 * the selectors that have already been built.
	 * </p>
	 * 
	 * <p>
	 * If all fields and functions have been built then this list is not accurate.
	 * Use {@link #fHasBuiltFieldsAndMethods} to detect this situation.
	 * </p>
	 * 
	 * @see #buildFieldsAndMethods(char[])
	 * @see #fHasBuiltFieldsAndMethods
	 */
	private char[][] fBuiltSelectors;
	
	/**
	 * <p>
	 * Used to synchronize access to fields that keep track of field and function building.
	 * </p>
	 * 
	 * @see #fBuildingAllFieldsAndFunctions
	 * @see #fBuildingSelectors
	 * @see #fBuiltSelectors
	 */
	private final Object fBuildFieldsAndFunctionsLock = new Object();

	public SourceTypeBinding(char[][] compoundName, PackageBinding fPackage,
			Scope scope) {
		
		this();
		this.compoundName = compoundName;
		this.fPackage = fPackage;
		this.fileName = scope == null ? null : scope.referenceCompilationUnit().getFileName();
		if (scope instanceof ClassScope) {
			this.classScope = (ClassScope) scope;
			if (this.classScope.referenceContext != null) {
				this.modifiers = this.classScope.referenceContext.modifiers;
				this.sourceName = this.classScope.referenceContext.name;
			} else {
				this.sourceName = this.classScope.inferredType.getName();

				this.modifiers = ClassFileConstants.AccPublic;
			}
		}
		this.scope = scope;

		// expect the fields & methods to be initialized correctly later
		this.fields = Binding.NO_FIELDS;
		this.methods = Binding.NO_METHODS;

		computeId();
	}

	protected SourceTypeBinding() {
		//next type is a circular linked list
		this.fNextType = this;
		
		this.fHasBuiltFieldsAndMethods = false;
		this.fBuildingAllFieldsAndFunctions = false;
		this.fBuildingSelectors = null;
		this.fBuiltSelectors = null;
	}
	
	/**
	 * <p>
	 * Builds all fields and functions for this type.
	 * </p>
	 * 
	 * <p>
	 * No operation if all fields and functions are already built. Also if an
	 * individual field or function has already been built then it will not be
	 * built again.
	 * </p>
	 */
	void buildFieldsAndMethods() {
		this.buildFieldsAndMethods(null);
	}

	/**
	 * <p>
	 * If no restriction is given then the same as
	 * {@link #buildFieldsAndMethods()}, else if a restriction is given only
	 * fields and functions with the given selector are built.
	 * </p>
	 * 
	 * <p>
	 * No operation if all fields and functions are already built. Also if an
	 * individual field or function has already been built then it will not be
	 * built again.
	 * </p>
	 * 
	 * @param restrictToSelector
	 *            restrict building to only fields and functions with this
	 *            selector, or if <code>null</code> build all fields and
	 *            functions
	 */
	private void buildFieldsAndMethods(char[] restrictToSelector) {
		synchronized (this.fBuildFieldsAndFunctionsLock) {
			//if already building or have already built then do nothing
			if(this.fBuildingAllFieldsAndFunctions || this.fHasBuiltFieldsAndMethods ) {
				return;
			}
			
			//if already building restrict to selector then do nothing
			if(restrictToSelector != null && this.fBuildingSelectors != null && this.fBuildingSelectors.length > 0) {
				if(CharOperation.contains(restrictToSelector, this.fBuildingSelectors)) {
					return;
				}
			}
			
			//if already built selector then do nothing
			if(restrictToSelector != null && this.fBuiltSelectors != null && this.fBuiltSelectors.length > 0) {
				if(CharOperation.contains(restrictToSelector, this.fBuiltSelectors)) {
					return;
				}
			}
			
			/* if restrict building to specific selector add it to list of currently building specific selectors
			 * else set building all fields and functions */
			if(restrictToSelector != null) {
				if(this.fBuildingSelectors == null) {
					this.fBuildingSelectors = new char[1][];
					this.fBuildingSelectors[0] = restrictToSelector;
				} else {
					char[][] newBuildingSelectors = new char[this.fBuildingSelectors.length+1][];
					System.arraycopy(this.fBuildingSelectors, 0, newBuildingSelectors, 0, this.fBuildingSelectors.length);
					this.fBuildingSelectors = newBuildingSelectors;
					this.fBuildingSelectors[this.fBuildingSelectors.length-1] = restrictToSelector;
				}
			} else {
				this.fBuildingAllFieldsAndFunctions = true;
			}
		}
		
		try {			
			/* build functions first because building fields depends on built functions
			 * 
			 * This is because building fields can add functions if their assignment is a function
			 * but only if a function with the fields name has not already been created */
			buildFunctions(restrictToSelector);
			buildFields(restrictToSelector);
			
			if(restrictToSelector == null) {
				this.fHasBuiltFieldsAndMethods = true;
			}
		} finally {
			synchronized (this.fBuildFieldsAndFunctionsLock) {
				if(restrictToSelector != null) {
					//remove selector from list of building selectors
					if(this.fBuildingSelectors.length == 1) {
						this.fBuildingSelectors = null;
					} else {
						char[][] newBuildingSelectors = new char[this.fBuildingSelectors.length-1][];
						int j = 0;
						for(int i = 0; i < this.fBuildingSelectors.length; ++i) {
							if(!CharOperation.equals(restrictToSelector, this.fBuildingSelectors[i])) {
								newBuildingSelectors[j++] = this.fBuildingSelectors[i];
							}
						}
						
						this.fBuildingSelectors = newBuildingSelectors;
					}
					
					//add selector to list of built selectors
					if(this.fBuiltSelectors == null) {
						this.fBuiltSelectors = new char[1][];
						this.fBuiltSelectors[0] = restrictToSelector;
					} else {
						char[][] newBuiltSelectors = new char[this.fBuiltSelectors.length+1][];
						System.arraycopy(this.fBuiltSelectors, 0, newBuiltSelectors, 0, this.fBuiltSelectors.length);
						this.fBuiltSelectors = newBuiltSelectors;
						this.fBuiltSelectors[this.fBuiltSelectors.length-1] = restrictToSelector;
					}
				} else {
					this.fBuildingAllFieldsAndFunctions = false;
				}
			}
		}
	}

	/**
	 * <p><b>IMPORTANT:</b> Gets the {@link InferredType} for this binding only. 
	 * This means that if this binding has a {@link #nextType} then the {@link InferredType}
	 * returned here is only a partially {@link InferredType}.</p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding#getInferredType()
	 */
	public InferredType getInferredType() {
		ClassScope classScope = scope.classScope();
		return classScope.inferredType;
	}

	/**
	 * <p>
	 * Build all or a specific field.
	 * </p>
	 * 
	 * @param restrictToSelector
	 *            build only fields with this selector, or if
	 *            <code>null</code> build all fields
	 */
	private void buildFields(char[] restrictToSelector) {
		if(this.classScope == null)
			return;
		InferredType inferredType = this.classScope.inferredType;
		int size = inferredType.numberAttributes;
		
		// iterate the field declarations to create the bindings, lose all duplicates
		FieldBinding[] fieldBindings = new FieldBinding[size + 1];
		HashtableOfObject knownFieldNames = new HashtableOfObject(size);
		boolean duplicate = false;
		int count = 0;
		for (int i = 0; i < size; i++) {
			InferredAttribute field = inferredType.attributes[i];
			
			/* only build field if either the restricted selector is not set or
			 * the field name matches the restricted selector.
			 * 
			 * Also skip any field that is already building or has already been built */
			if((restrictToSelector == null || CharOperation.equals(field.name, restrictToSelector)) &&
						(restrictToSelector != null || !CharOperation.contains(field.name, this.fBuildingSelectors)) &&
						!CharOperation.contains(field.name, this.fBuiltSelectors)) {
			
				int modifiers = 0;
				if (field.isStatic) {
					modifiers |= ClassFileConstants.AccStatic;
				}
				
				InferredType fieldType = field.type;
				TypeBinding fieldTypeBinding = null;
				
				Scope searchScope = null;
				
				/* if field type set then use that for binding
				 * else if field node is an assignment use that
				 * 
				 * TODO: this is not the correct logic because it does not deal
				 * with cases where the RHS is a function and the field type
				 * has already been resolved to function because then the function
				 * itself has not actually been resolved yet, so we can not create
				 * a function binding on this type for it.  Initial attempts to fix
				 * this flaw have caused performance issues, so it will have to be
				 * addressed latter. To fix this turn this into two ifs, but again
				 * that then tanks performance for SOME scenarios.*/
				if (fieldType != null) {
					fieldTypeBinding = fieldType.resolveType(scope, field.node);
				} else if(field.node instanceof IAssignment) {
					IExpression rhs = ((IAssignment)field.node).getExpression();
					
					if(rhs instanceof Expression) {
						fieldTypeBinding = ((Expression) rhs).resolvedType;
						
						/* if field binding for RHS not set or is any look for a better one,
						 * if function then local scope needs to be built so function can be resolved */
						if(fieldTypeBinding == null || fieldTypeBinding.isAnyType()) {	
							
							/* if node is an assignment and that assignment is contained
							 * in a function resolve the function first */
							IFunctionDeclaration containingFunction = null;
							if(field.node instanceof Assignment) {
								containingFunction = ((Assignment)field.node).getContainingFunction();
								if(containingFunction != null && containingFunction instanceof AbstractMethodDeclaration) {	
									((AbstractMethodDeclaration)containingFunction).buildLocals(this.scope.compilationUnitScope());
									searchScope = ((AbstractMethodDeclaration)containingFunction).getScope();
								}
							}
							
							//if no search scope found yet find first parent scope that is a BlockScope
							if(searchScope == null) {
								searchScope = this.scope;
								while(searchScope != null && !(searchScope instanceof BlockScope)) {
									searchScope = searchScope.parent;
								}
							}
							
							//use search scope to find binding
							TypeBinding resolvedBinding = ((Expression) rhs).resolveType((BlockScope)searchScope);
							if(resolvedBinding != null) {
								fieldTypeBinding = resolvedBinding;
							}
						}
					}
						
					//if RHS binding is a function so create a new function binding on this source binding
					if(fieldTypeBinding != null && isFunctionType(fieldTypeBinding)) {
						
						/* if RHS is a field reference search its receiver for the function binding for the assigned function
						 * else if single name reference just use its method bindingn */
						MethodBinding assignedFuncBinding = null;
						
						/* this is to deal with cases like:
						 * foo = (bar = function() {}); */
						while(rhs instanceof IAssignment) {
							rhs = ((IAssignment)rhs).getExpression();
						}
						
						if(rhs instanceof IFieldReference) {
							char[] selector = ((IFieldReference) rhs).getToken();
							IExpression receiver = ((IFieldReference) rhs).getReceiver();
							if(receiver instanceof Expression) {
								TypeBinding receiverType = ((Expression) receiver).resolvedType;
								if(receiverType instanceof SourceTypeBinding) {
									//if found bindings use first one
									MethodBinding[] funcBindings = ((SourceTypeBinding) receiverType).getMethods(selector);
									if(funcBindings != null && funcBindings.length > 0) {
										assignedFuncBinding = funcBindings[0];
									}
								}
							}
						} else if(rhs instanceof SingleNameReference) {
							Binding binding = ((SingleNameReference) rhs).binding;
							
							/* if binding is method binding just use that
							 * else if binding is local variable, find func binding in scope with same name as variable
							 * 		var foo;
							 * 		foo = function() {} */
							if(binding instanceof MethodBinding) {
								assignedFuncBinding = (MethodBinding)binding;
							} else if(binding instanceof LocalVariableBinding && searchScope != null) {
								if(searchScope instanceof BlockScope) {
									assignedFuncBinding = ((BlockScope)searchScope).findMethod(field.name, null, true);
								} else {
									assignedFuncBinding = searchScope.findMethod(null, field.name, null, null);
								}
							}
						}
						
						//if RHS was a function binding create a new function binding on this type binding
						if(assignedFuncBinding != null) {
							InferredMethod dupMeth = inferredType.findMethod(field.name, null);
							if(dupMeth == null) {
								MethodBinding[] funcBindings = this.getMethods(field.name);
								if(funcBindings == null || funcBindings.length == 0) {
									MethodBinding funcBinding = new MethodBinding(assignedFuncBinding, this);
									funcBinding.setSelector(field.name);
									this.addMethod(funcBinding);
								}
							}
						}
					}
				}
				
				if (fieldTypeBinding == null) {
					fieldTypeBinding = TypeBinding.UNKNOWN;
				}
	
				FieldBinding fieldBinding = new FieldBinding(field,
						fieldTypeBinding, modifiers
								| ExtraCompilerModifiers.AccUnresolved, this);
				fieldBinding.id = count;
	
				if (knownFieldNames.containsKey(field.name)) {
					duplicate = true;
					FieldBinding previousBinding = (FieldBinding) knownFieldNames
							.get(field.name);
					if (previousBinding != null) {
						for (int f = 0; f < i; f++) {
							InferredAttribute previousField = inferredType.attributes[f];
							if (previousField.binding == previousBinding) {
								scope.problemReporter().duplicateFieldInType(this,
										previousField);
								previousField.binding = null;
								break;
							}
						}
					}
					// ensure that the duplicate field is found & removed
					knownFieldNames.put(field.name, null); 
					scope.problemReporter().duplicateFieldInType(this, field);
					field.binding = null;
				} else {
					knownFieldNames.put(field.name, fieldBinding);
					// remember that we have seen a field with this name
					fieldBindings[count++] = fieldBinding;
				}
			}
		}
		
		//only add prototype if not building specific selector
		if(restrictToSelector == null) {
			FieldBinding prototype = new FieldBinding(TypeConstants.PROTOTYPE,
						TypeBinding.UNKNOWN, modifiers
								| ExtraCompilerModifiers.AccUnresolved, this);
			
			fieldBindings[count++] = prototype;
		}
		
		// remove duplicate fields
		if (duplicate) {
			FieldBinding[] newFieldBindings = new FieldBinding[fieldBindings.length];
			// we know we'll be removing at least 1 duplicate name
			size = count;
			count = 0;
			for (int i = 0; i < size; i++) {
				FieldBinding fieldBinding = fieldBindings[i];
				if (knownFieldNames.get(fieldBinding.name) != null) {
					fieldBinding.id = count;
					newFieldBindings[count++] = fieldBinding;
				}
			}
			fieldBindings = newFieldBindings;
		}
		
		//make sure array length is correct
		if (count != fieldBindings.length) {
			System.arraycopy(fieldBindings, 0,
					fieldBindings = new FieldBinding[count], 0, count);
		}
		
		this.addFields(fieldBindings);
	}

	/**
	 * <p>
	 * Build all or a specific function.
	 * </p>
	 * 
	 * @param restrictToSelector
	 *            build only functions with this selector, or if
	 *            <code>null</code> build all functions
	 */
	private void buildFunctions(char[] restrictToSelector) {
		if(this.classScope == null) {
			return;
		}
		InferredType inferredType = this.classScope.inferredType;
		int size = (inferredType.methods != null) ? inferredType.methods.size()
				: 0;

		if (size == 0) {
			return;
		}

		int count = 0;
		MethodBinding[] methodBindings = new MethodBinding[size];
		// create bindings for source methods
		for (int i = 0; i < size; i++) {
			InferredMethod inferredMethod = (InferredMethod) inferredType.methods.get(i);
			
			/* only build function if either the restricted selector is not set or
			 * the function name matches the restricted selector.
			 * 
			 * Also skip any function that is already building or has already been built */
			if((restrictToSelector == null || CharOperation.equals(inferredMethod.name, restrictToSelector)) &&
						(restrictToSelector != null || !CharOperation.contains(inferredMethod.name, this.fBuildingSelectors)) &&
						!CharOperation.contains(inferredMethod.name, this.fBuiltSelectors)) {
					
				//determine if the method already has a resolved scope or not
				boolean doesNotHaveResolvedScope = inferredMethod.getFunctionDeclaration() instanceof AbstractMethodDeclaration &&
						((AbstractMethodDeclaration)inferredMethod.getFunctionDeclaration()).getScope() == null;
				
				//build method scope
				MethodDeclaration methDec = (MethodDeclaration) inferredMethod.getFunctionDeclaration();
				MethodBinding methodBinding;
				
				/* if does not already have a binding or existing binding has a different name then
				 * 		current inferred function create a new method binding
				 * else use existing method binding */
				if(!methDec.hasBinding() || !CharOperation.equals(methDec.getBinding().selector, inferredMethod.name)) {
					MethodScope scope = new MethodScope(this.scope, methDec, false);
					
					/* if the inferred method specifies that it is in a type use that one.
					 * 
					 * This is for the case where a method has been mixed in from another type
					 * but we still want that method to be reported as defined on the other
					 * type and not this type */
					SourceTypeBinding declaringTypeBinding = null;
					if(inferredMethod.inType != null && inferredMethod.inType.binding != null && !inferredMethod.isConstructor) {
						declaringTypeBinding = inferredMethod.inType.binding;
					} else {
						declaringTypeBinding = this;
					}
					
					/* if not existing binding or is a constructor then use scope to create new binding
					 * else create new binding based on existing binding */
					if(!methDec.hasBinding() || inferredMethod.isConstructor) {
						methodBinding = scope.createMethod(inferredMethod, declaringTypeBinding);
					} else {
						methodBinding = new MethodBinding(methDec.getBinding(), declaringTypeBinding);
						methodBinding.setSelector(inferredMethod.name);
					}
				} else {
					methodBinding = methDec.getBinding();
				}
				
				//set bindings
				inferredMethod.methodBinding = methodBinding;
				methDec.setBinding(methodBinding);
				
				//is null if binding could not be created
				if (methodBinding != null) {
					methodBindings[count++] = methodBinding;
					
					// if method did not already have a resolved scope, then add it to the environment
					if(doesNotHaveResolvedScope) {
						this.scope.environment().defaultPackage.addBinding(
								methodBinding, methodBinding.selector,
								Binding.METHOD);
					}
				}
			}
		}
		if (count != methodBindings.length) {
			System.arraycopy(methodBindings, 0,
					methodBindings = new MethodBinding[count], 0, count);
		}
		
		// in case some static imports reached already into this type
		tagBits &= ~TagBits.AreMethodsSorted; 
		
		this.addMethods(methodBindings);
	}

	public int kind() {
		return Binding.TYPE;
	}

	public char[] computeUniqueKey(boolean isLeaf) {
		char[] uniqueKey = super.computeUniqueKey(isLeaf);
		if (uniqueKey.length == 2)
			return uniqueKey; // problem type's unique key is "L;"
		if (Util.isClassFileName(this.fileName)
				|| org.eclipse.wst.jsdt.internal.core.util.Util
						.isMetadataFileName(new String(this.fileName)))
			return uniqueKey; // no need to insert compilation unit name for a
								// .class file

		// insert compilation unit name if the type name is not the main type
		// name
		int end = CharOperation.lastIndexOf('.', this.fileName);
		if (end != -1) {
			int start = CharOperation.lastIndexOf('/', this.fileName) + 1;
			char[] mainTypeName = CharOperation.subarray(this.fileName, start,
					end);
			start = CharOperation.lastIndexOf('/', uniqueKey) + 1;
			if (start == 0)
				start = 1; // start after L
			end = CharOperation.indexOf('<', uniqueKey, start);
			if (end == -1)
				end = CharOperation.indexOf(';', uniqueKey, start);
			char[] topLevelType = CharOperation.subarray(uniqueKey, start, end);
			if (!CharOperation.equals(topLevelType, mainTypeName)) {
				StringBuffer buffer = new StringBuffer();
				buffer.append(uniqueKey, 0, start);
				buffer.append(mainTypeName);
				buffer.append('~');
				buffer.append(topLevelType);
				buffer.append(uniqueKey, end, uniqueKey.length - end);
				int length = buffer.length();
				uniqueKey = new char[length];
				buffer.getChars(0, length, uniqueKey, 0);
				return uniqueKey;
			}
		}
		return uniqueKey;
	}

	void faultInTypesForFieldsAndMethods() {
		// check @Deprecated annotation
		// getAnnotationTagBits(); // marks as deprecated by side effect
		ReferenceBinding enclosingType = this.enclosingType();
		if (enclosingType != null && enclosingType.isViewedAsDeprecated()
				&& !this.isDeprecated())
			this.modifiers |= ExtraCompilerModifiers.AccDeprecatedImplicitly;
		fields();
		methods();
	}

	/**
	 * <p>
	 * NOTE: the type of each field of a source type is resolved when needed
	 * </p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding#fields()
	 */
	public FieldBinding[] fields() {
		final Map fieldCache = new HashMap();
		
		//get fields across all linked types
		this.performActionOnLinkedBindings(new LinkedBindingAction() {
			/**
			 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#performAction(org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding)
			 */
			public boolean performAction(SourceTypeBinding linkedBinding) {
				//be sure fields and methods are built
				linkedBinding.buildFieldsAndMethods();
				
				// complete fields if not yet complete
				if ((linkedBinding.tagBits & TagBits.AreFieldsComplete) == 0) {
					int failed = 0;
					FieldBinding[] resolvedFields = linkedBinding.fields;
					try {
						// lazily sort fields
						if ((linkedBinding.tagBits & TagBits.AreFieldsSorted) == 0) {
							int length = linkedBinding.fields.length;
							if (length > 1) {
								ReferenceBinding.sortFields(linkedBinding.fields, 0, length);
							}
							linkedBinding.tagBits |= TagBits.AreFieldsSorted;
						}
						for (int i = 0, length = linkedBinding.fields.length; i < length; i++) {
							if (linkedBinding.resolveTypeFor(linkedBinding.fields[i]) == null) {
								/* do not alter original field array until resolution is
								 * over, due to reentrance (143259) */
								if (resolvedFields == linkedBinding.fields) {
									System.arraycopy(linkedBinding.fields, 0,
											resolvedFields = new FieldBinding[length],
											0, length);
								}
								resolvedFields[i] = null;
								failed++;
							}
						}
					} finally {
						if (failed > 0) {
							// ensure fields are consistent regardless of the error
							int newSize = resolvedFields.length - failed;
							if (newSize == 0) {
								linkedBinding.setFields(Binding.NO_FIELDS);
							} else {
								FieldBinding[] newFields = new FieldBinding[newSize];
								for (int i = 0, j = 0, length = resolvedFields.length; i < length; i++) {
									if (resolvedFields[i] != null) {
										newFields[j++] = resolvedFields[i];
									}
								}
								linkedBinding.setFields(newFields);
							}
						}
					}
					
					//mark fields as complete
					linkedBinding.tagBits |= TagBits.AreFieldsComplete;
				}
				
				//add fields to combined cache
				for(int i = 0; i < linkedBinding.fields.length; i++) {
					if(linkedBinding.fields[i] != null) {
						fieldCache.put(linkedBinding.fields[i].name, linkedBinding.fields[i]);
					}
				}
				
				// always search every linked type
				return true;
			}
		});
		
		return (FieldBinding[]) fieldCache.values().toArray(new FieldBinding[0]);
	}

	/**
	 * <p>
	 * Finds an exact constructor match searching across all linked type bindings.
	 * </p>
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding#getExactConstructor(org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding[])
	 */
	public MethodBinding getExactConstructor(final TypeBinding[] argumentTypes) {
		MethodBinding exactConstructor = (MethodBinding)this.performActionOnLinkedBindings(new LinkedBindingAction() {
			/**
			 * <p>
			 * The exact constructor match found on any of the linked types.
			 * </p>
			 */
			private MethodBinding fExactConstructorMatch = null;
			
			/**
			 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#performAction(org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding)
			 */
			public boolean performAction(SourceTypeBinding linkedBinding) {
				//be sure fields and methods are built
				linkedBinding.buildFieldsAndMethods();
				
				this.fExactConstructorMatch = linkedBinding.getExactConstructor0(argumentTypes);
				
				//keep processing if have not yet found exact match
				return this.fExactConstructorMatch == null;
			}
			
			/**
			 * @return {@link MethodBinding}
			 * 
			 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#getFinalResult()
			 */
			public Object getFinalResult() {
				return this.fExactConstructorMatch;
			}
		});
		
		return exactConstructor;
	}

	// NOTE: the return type, arg & exception types of each method of a source
	// type are resolved when needed
	private MethodBinding getExactConstructor0(TypeBinding[] argumentTypes) {
		// have resolved all arg types & return type of the methods
		if ((this.tagBits & TagBits.AreMethodsComplete) != 0) {
			long range;
			if ((range = ReferenceBinding.binarySearch(TypeConstants.INIT, this.methods)) >= 0) {
				if((int) range <= (int) (range >> 32)) {
					MethodBinding method = this.methods[(int) range];
					return method;
				}
			}
		} else {
			// lazily sort methods
			if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
				int length = this.methods.length;
				if (length > 1) {
					ReferenceBinding.sortMethods(this.methods, 0, length);
				}
				this.tagBits |= TagBits.AreMethodsSorted;
			}
			long range;
			if ((range = ReferenceBinding.binarySearch(TypeConstants.INIT, this.methods)) >= 0) {
				if((int) range <= (int) (range >> 32)) {
					MethodBinding method = this.methods[(int) range];
					if (resolveTypesFor(method) == null
							|| method.returnType == null) {
						methods();
						// try again since the problem methods have been removed
						return getExactConstructor(argumentTypes); 
					}
					return method;
				}
			}
		}
		return null;
	}

	/**
	 * <p>
	 * Finds an exact function match searching across all linked type bindings
	 * and their super types.
	 * </p>
	 * 
	 * <p>
	 * <b>NOTE:</b> this uses a breadth first search to find an exact function
	 * binding, first it searches all linked bindings, then their parents, so
	 * forth and so on.
	 * </p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding#getExactMethod(char[],
	 *      org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding[],
	 *      org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope)
	 */
	public MethodBinding getExactMethod(final char[] selector,
			final TypeBinding[] argumentTypes, final CompilationUnitScope refScope) {
		
		MethodBinding exactMethod = null;
		
		//search all linked types for exact method match
		if(selector != null) {
			final LinkedList typesToCheck = new LinkedList();
			
			/* this set will contain every type that has already been checked
			 * this includes all linked types, therefore a simple contains check
			 * can be done to see if a given type has already been checked
			 * rather then having to iterate and do an expensive #isEquivalentTo check */
			final Set checkedTypes = new HashSet();
			typesToCheck.add(this);
			
			while(!typesToCheck.isEmpty() && exactMethod == null) {
				ReferenceBinding typeToCheck = (ReferenceBinding)typesToCheck.removeFirst();
				
				
				/* if type to check is SourceTypeBinding then have to check all linked bindings
				 * else just check the ReferenceBinding directly */
				if(typeToCheck instanceof SourceTypeBinding) {
					exactMethod = (MethodBinding)((SourceTypeBinding)typeToCheck).performActionOnLinkedBindings(new LinkedBindingAction() {
						/**
						 * <p>
						 * The located exact function match.
						 * </p>
						 */
						private MethodBinding fExactFunctionMatch;
						
						/**
						 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#performAction(org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding)
						 */
						public boolean performAction(SourceTypeBinding typeToCheckLinkedBinding) {
							checkedTypes.add(typeToCheckLinkedBinding);
							
							/* only build fields and functions if inferred type has a function with correct name,
							 * and only bother checking inferred type if the fields and functions have not already been built
							 * 
							 * this saves time avoiding building fields and functions when it is not needed */
							if(!SourceTypeBinding.this.fHasBuiltFieldsAndMethods && typeToCheckLinkedBinding.inferredTypeHasFunction(selector)) {
								//be sure fields and methods are built
								typeToCheckLinkedBinding.buildFieldsAndMethods(selector);
							}
							
							//check self for exact function
							this.fExactFunctionMatch = typeToCheckLinkedBinding.getExactMethod0(selector, argumentTypes, refScope);
							
							/* add super type of current linked binding to types to check if
							 * not already there and not already checked */
							if(this.fExactFunctionMatch == null) {
								boolean alreadyGoingToCheck = false;
								ReferenceBinding superBinding = typeToCheckLinkedBinding.getSuperBinding0();
								if(superBinding != null) {
									Iterator typesToCheckIter = typesToCheck.iterator();
									while(typesToCheckIter.hasNext()) {
										alreadyGoingToCheck = ((ReferenceBinding)typesToCheckIter.next()).isEquivalentTo(superBinding);
									}
									
									boolean alreadyChecked = false;
									if(!alreadyGoingToCheck) {
										alreadyChecked = checkedTypes.contains(superBinding);
									}
									
									if(!alreadyGoingToCheck && !alreadyChecked) {
										typesToCheck.add(superBinding);
									}
								}
							}
							
							//keep processing if have not yet found exact match
							return this.fExactFunctionMatch == null;
						}
						
						/**
						 * @return {@link MethodBinding}
						 * 
						 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#getFinalResult()
						 */
						public Object getFinalResult() {
							return this.fExactFunctionMatch;
						}
					});
				} else {
					checkedTypes.add(typeToCheck);
					
					exactMethod = typeToCheck.getExactMethod(selector, argumentTypes, refScope);
					
					/* add super type of current binding to types to check if
					 * not already there and not already checked */
					if(exactMethod == null) {
						boolean alreadyGoingToCheck = false;
						ReferenceBinding superBinding = typeToCheck.getSuperBinding();
						if(superBinding != null) {
							Iterator typesToCheckIter = typesToCheck.iterator();
							while(typesToCheckIter.hasNext()) {
								alreadyGoingToCheck = ((ReferenceBinding)typesToCheckIter.next()).isEquivalentTo(superBinding);
							}
							
							boolean alreadyChecked = false;
							if(!alreadyGoingToCheck) {
								alreadyChecked = checkedTypes.contains(superBinding);
							}
							
							if(!alreadyGoingToCheck && !alreadyChecked) {
								typesToCheck.add(superBinding);
							}
						}
					}
				}
			}
		}
		
		return exactMethod;
	}

	/**
	 * <p>
	 * <b>NOTES:</b>
	 * <ul>
	 * <li>the return type, arg & exception types of each method of a source
	 * type are resolved when needed.</li>
	 * <li>this method only searches this specific binding, it does not search
	 * any linked bindings or any super bindings</li>
	 * </ul>
	 * </p>
	 * 
	 * @param selector
	 * @param argumentTypes
	 * @param refScope
	 * @return
	 */
	private MethodBinding getExactMethod0(char[] selector,
			TypeBinding[] argumentTypes, CompilationUnitScope refScope) {
		
		if(selector == null || this.methods == null || this.methods.length == 0)
			return null;

		// have resolved all arg types & return type of the methods
		if ((this.tagBits & TagBits.AreMethodsComplete) != 0) { 
			long range;
			if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
				if((int) range <= (int) (range >> 32)) {
					MethodBinding method = this.methods[(int) range];
					// inner type lookups must know that a  method with this name exists
					return method;
				}
			}
		} else {
			// lazily sort methods
			if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
				int length = this.methods.length;
				if (length > 1)
					ReferenceBinding.sortMethods(this.methods, 0, length);
				this.tagBits |= TagBits.AreMethodsSorted;
			}

			long range;
			if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
				// check unresolved method
				int start = (int) range, end = (int) (range >> 32);
				for (int imethod = start; imethod <= end; imethod++) {
					MethodBinding method = this.methods[imethod];
					if (resolveTypesFor(method) == null
							|| method.returnType == null) {
						methods();
						// try again since the problem methods have been removed
						return getExactMethod0(selector, argumentTypes, refScope); 
					}
				}
				// check dup collisions
				boolean isSource15 = this.scope != null
						&& this.scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5;
				for (int i = start; i <= end; i++) {
					MethodBinding method1 = this.methods[i];
					for (int j = end; j > i; j--) {
						MethodBinding method2 = this.methods[j];
						boolean paramsMatch = isSource15 ? method1
								.areParametersEqual(method2) : method1
								.areParametersEqual(method2);
						if (paramsMatch) {
							methods();
							// try again since the problem methods have been removed
							return getExactMethod0(selector, argumentTypes, refScope); 
						}
					}
				}
				return this.methods[start];
			}
		}

		return null;
	}

	/**
	 * <p>
	 * Searches all linked types for a field with the given name.
	 * </p>
	 * 
	 * <p>
	 * <b>NOTE:</p> this does not check super types.
	 * </p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding#getField(char[], boolean)
	 */
	public FieldBinding getField(final char[] fieldName, final boolean needResolve) {
		FieldBinding field = null;
		
		//search all linked types for exact method match
		if(fieldName != null) {
			field = (FieldBinding)this.performActionOnLinkedBindings(new LinkedBindingAction() {
				/**
				 * <p>
				 * The located exact function match.
				 * </p>
				 */
				private FieldBinding fFieldMatch;
				
				/**
				 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#performAction(org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding)
				 */
				public boolean performAction(SourceTypeBinding linkedBinding) {
					/* only build fields and functions if inferred type has field with correct name,
					 * and only bother checking inferred type if the fields and functions have not already been built
					 * 
					 * this saves time avoiding building fields and functions when it is not needed */
					if(!SourceTypeBinding.this.fHasBuiltFieldsAndMethods && linkedBinding.inferredTypeHasField(fieldName)) {
						//be sure fields and methods are built
						linkedBinding.buildFieldsAndMethods(fieldName);
					}
						
					//check self for exact field
					this.fFieldMatch = linkedBinding.getField0(fieldName, needResolve);
					
					//keep processing if have not yet found exact match
					return this.fFieldMatch == null;
				}
				
				/**
				 * @return {@link FieldBinding}
				 * 
				 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#getFinalResult()
				 */
				public Object getFinalResult() {
					return this.fFieldMatch;
				}
			});
		}
		
		return field;
	}

	public FieldBinding getFieldInHierarchy(char[] fieldName, boolean needResolve) {
		SourceTypeBinding currentType = this;
		while (currentType != null) {
			FieldBinding field = currentType.getField(fieldName, needResolve);
			if (field != null) {
				return field;
			}
			currentType = (SourceTypeBinding) currentType.getSuperBinding();
		}
		return null;
	}

	// NOTE: the type of a field of a source type is resolved when needed
	private FieldBinding getField0(char[] fieldName, boolean needResolve) {

		if ((this.tagBits & TagBits.AreFieldsComplete) != 0) {
			return ReferenceBinding.binarySearch(fieldName, this.fields);
		}

		// lazily sort fields
		if ((this.tagBits & TagBits.AreFieldsSorted) == 0) {
			int length = this.fields.length;
			if (length > 1)
				ReferenceBinding.sortFields(this.fields, 0, length);
			this.tagBits |= TagBits.AreFieldsSorted;
		}
		// always resolve anyway on source types
		FieldBinding field = ReferenceBinding.binarySearch(fieldName, this.fields);
		if (field != null) {
			FieldBinding result = null;
			try {
				result = resolveTypeFor(field);
				return result;
			} finally {
				if (result == null) {
					// ensure fields are consistent reqardless of the error
					int newSize = this.fields.length - 1;
					if (newSize == 0) {
						this.setFields(Binding.NO_FIELDS);
					} else {
						FieldBinding[] newFields = new FieldBinding[newSize];
						int index = 0;
						for (int i = 0, length = this.fields.length; i < length; i++) {
							FieldBinding f = this.fields[i];
							if (f == field) {
								continue;
							}
							newFields[index++] = f;
						}
						this.setFields(newFields);
					}
				}
			}
		}
		return null;
	}

	/**
	 * <p>
	 * Get all methods across all linked type bindings.
	 * </p>
	 * 
	 * <p>
	 * <b>NOTE:</p> this does not check super types.
	 * </p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding#getMethods(char[])
	 */
	public MethodBinding[] getMethods(final char[] selector) {
		MethodBinding[] allFunctionMatches = Binding.NO_METHODS;
		
		//search all linked types for functions matching the given selector
		if(selector != null) {
			allFunctionMatches = (MethodBinding[])this.performActionOnLinkedBindings(new LinkedBindingAction() {
				/**
				 * <p>
				 * All of the functions matching a given selector found across all of the linked types.
				 * </p>
				 */
				private MethodBinding[] fAllFunctionMatches = Binding.NO_METHODS;
				
				/**
				 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#performAction(org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding)
				 */
				public boolean performAction(SourceTypeBinding linkedBinding) {
					/* only build fields and functions if inferred type has field with correct name,
					 * and only bother checking inferred type if the fields and functions have not already been built
					 * 
					 * this saves time avoiding building fields and functions when it is not needed */
					if(!SourceTypeBinding.this.fHasBuiltFieldsAndMethods && linkedBinding.inferredTypeHasFunction(selector)) {
						linkedBinding.buildFieldsAndMethods(selector);
					}
					
					//get current types functions
					MethodBinding[] functionMatches = linkedBinding.getMethods0(selector);
					
					//combine all function matches into one array
					if(this.fAllFunctionMatches == null) {
						this.fAllFunctionMatches = functionMatches;
					} else {
						MethodBinding[] combinedFunctionMatches = new MethodBinding[this.fAllFunctionMatches.length + functionMatches.length];
			    		System.arraycopy(this.fAllFunctionMatches, 0, combinedFunctionMatches, 0, this.fAllFunctionMatches.length);
			    		System.arraycopy(functionMatches, 0, combinedFunctionMatches, this.fAllFunctionMatches.length, functionMatches.length);
			    		this.fAllFunctionMatches = combinedFunctionMatches;
					}
		    		
					// always search every linked type for methods
					return true;
				}
				
				/**
				 * @return {@link MethodBinding}[]
				 * 
				 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#getFinalResult()
				 */
				public Object getFinalResult() {
					return this.fAllFunctionMatches;
				}
			});
		}
		
		return allFunctionMatches;
	}

	/**
	 * <p>
	 * <b>NOTES:</b>
	 * <ul>
	 * <li>the return type, arg & exception types of each method of a source
	 * type are resolved when needed</li>
	 * <li>this does not check super types</li>
	 * </ul>
	 * </p>
	 * 
	 * @param selector
	 * @return
	 */
	private MethodBinding[] getMethods0(char[] selector) {
		// lazily sort methods
		if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
			int length = this.methods.length;
			if (length > 1) {
				ReferenceBinding.sortMethods(this.methods, 0, length);
			}
			this.tagBits |= TagBits.AreMethodsSorted;
		}
		
		if ((this.tagBits & TagBits.AreMethodsComplete) != 0) {
			long range;
			if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
				int start = (int) range, end = (int) (range >> 32);
				int length = end - start + 1;
				MethodBinding[] result;
				System.arraycopy(this.methods, start,
						result = new MethodBinding[length], 0, length);
				return result;
			} else {
				return Binding.NO_METHODS;
			}
		}
		
		MethodBinding[] result;
		long range;
		if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
			int start = (int) range, end = (int) (range >> 32);
			for (int i = start; i <= end; i++) {
				MethodBinding method = this.methods[i];
				if (resolveTypesFor(method) == null || method.returnType == null) {
					// try again since the problem methods have been removed
					methods();
					return getMethods(selector); 
				}
			}
			int length = end - start + 1;
			System.arraycopy(this.methods, start,
					result = new MethodBinding[length], 0, length);
		} else {
			return Binding.NO_METHODS;
		}
		boolean isSource15 = this.scope != null
				&& this.scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5;
		for (int i = 0, length = result.length - 1; i < length; i++) {
			MethodBinding method = result[i];
			for (int j = length; j > i; j--) {
				boolean paramsMatch = isSource15 ? method
						.areParametersEqual(result[j]) : method
						.areParametersEqual(result[j]);
				if (paramsMatch) {
					methods();
					// try again since the duplicate methods have been removed
					return getMethods(selector);
				}
			}
		}
		return result;
	}

	/**
	 * Returns true if a type is identical to another one.
	 */
	public boolean isEquivalentTo(final TypeBinding otherType) {
		//short cut for simple case
		boolean isEquivalent = this == otherType;
		
		if(!isEquivalent && otherType != null) {
			final boolean isOtherTypeSourceType = otherType instanceof SourceTypeBinding;
			
			isEquivalent = ((Boolean)this.performActionOnLinkedBindings(new LinkedBindingAction() {
				
				private boolean fIsEquivalent = false;
				
				public boolean performAction(final SourceTypeBinding selfLinkedBinding) {
					
				/* if other type is also a source type binding have to loop
				 * through all its types as well
				 * 
				 * else just compare current linked binding with other type */

					this.fIsEquivalent = selfLinkedBinding == otherType;
					
					return !this.fIsEquivalent;
				}
				
				public Object getFinalResult() {
					return new Boolean(this.fIsEquivalent);
				}
			})).booleanValue();
		}
		
		return isEquivalent;
	}

	/**
	 * <p>
	 * Get all member types across all of the linked type bindings.
	 * </p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding#memberTypes()
	 */
	public ReferenceBinding[] memberTypes() {
		//search all linked types for member types 
		ReferenceBinding[] allMemberTypes = (ReferenceBinding[])this.performActionOnLinkedBindings(new LinkedBindingAction() {
			/**
			 * <p>
			 * All of the member types found across all of the linked types.
			 * </p>
			 */
			private ReferenceBinding[] fAllMemberTypes;
			
			/**
			 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#performAction(org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding)
			 */
			public boolean performAction(SourceTypeBinding linkedBinding) {				
				//combine all methods into one array
				
				if(this.fAllMemberTypes == null) {
					this.fAllMemberTypes = linkedBinding.memberTypes;
				} else {
					ReferenceBinding[] combinedMemberTypes = new ReferenceBinding[this.fAllMemberTypes.length + linkedBinding.memberTypes.length];
		    		System.arraycopy(this.fAllMemberTypes, 0, combinedMemberTypes, 0, this.fAllMemberTypes.length);
		    		System.arraycopy(linkedBinding.memberTypes, 0, combinedMemberTypes, this.fAllMemberTypes.length, linkedBinding.memberTypes.length);
		    		this.fAllMemberTypes = combinedMemberTypes;
				}
	    		
				// always search every linked type for member types
				return true;
			}
			
			/**
			 * @return {@link ReferenceBinding}[]
			 * 
			 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#getFinalResult()
			 */
			public Object getFinalResult() {
				return this.fAllMemberTypes;
			}
		});
		
		return allMemberTypes;
	}

	public FieldBinding getUpdatedFieldBinding(FieldBinding targetField,
			ReferenceBinding newDeclaringClass) {
		Hashtable fieldMap = new Hashtable(5);
		FieldBinding updatedField = new FieldBinding(targetField,
				newDeclaringClass);
		fieldMap.put(newDeclaringClass, updatedField);
		return updatedField;
	}

	public MethodBinding getUpdatedMethodBinding(MethodBinding targetMethod,
			ReferenceBinding newDeclaringClass) {
		MethodBinding updatedMethod = new MethodBinding(targetMethod,
				newDeclaringClass);
		updatedMethod.createFunctionTypeBinding(scope);
		return updatedMethod;
	}

	/**
	 * <p>
	 * <code>true</code> if any of the linked types has have member types, <code>false</code> otherwise.
	 * </p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding#hasMemberTypes()
	 */
	public boolean hasMemberTypes() {
		//search all linked types for member types 
		Boolean hasMemberTypes = (Boolean)this.performActionOnLinkedBindings(new LinkedBindingAction() {
			/**
			 * <p>
			 * <code>true</code> if any linked type has member types, <code>false</code> otherwise
			 * </p>
			 */
			private boolean fHasMemberTypes = false;
			
			/**
			 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#performAction(org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding)
			 */
			public boolean performAction(SourceTypeBinding linkedBinding) {
				//check if this type has member types
				this.fHasMemberTypes = (linkedBinding.memberTypes != null
							&& linkedBinding.memberTypes.length > 0);
				
				//keep checking linked types if have not yet found member types
				return !this.fHasMemberTypes;
			}
			
			/**
			 * @return {@link Boolean}
			 * 
			 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#getFinalResult()
			 */
			public Object getFinalResult() {
				return new Boolean(this.fHasMemberTypes);
			}
		});
		
		return hasMemberTypes.booleanValue();
	}

	/**
	 * <p>
	 * NOTE: the return type, arg & exception types of each method of a source
	 * type are resolved when needed
	 * </p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding#methods()
	 */
	public MethodBinding[] methods() {
		//gather all the functions across all the linked types
		MethodBinding[] allFunctions = (MethodBinding[])this.performActionOnLinkedBindings(new LinkedBindingAction() {
			/**
			 * <p>
			 * All of the functions defined across all the linked types.
			 * </p>
			 */
			private MethodBinding[] fAllFunctions;
			
			/**
			 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#performAction(org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding)
			 */
			public boolean performAction(SourceTypeBinding linkedBinding) {
				//be sure fields and methods are built
				linkedBinding.buildFieldsAndMethods();

				if ((linkedBinding.tagBits & TagBits.AreMethodsComplete) == 0) {
					// lazily sort methods
					if ((linkedBinding.tagBits & TagBits.AreMethodsSorted) == 0) {
						int length = linkedBinding.methods.length;
						if (length > 1) {
							ReferenceBinding.sortMethods(linkedBinding.methods, 0, length);
						}
						linkedBinding.tagBits |= TagBits.AreMethodsSorted;
					}
					int failed = 0;
					MethodBinding[] resolvedMethods = linkedBinding.methods;
					try {
						for (int i = 0, length = linkedBinding.methods.length; i < length; i++) {
							if (resolveTypesFor(linkedBinding.methods[i]) == null) {
								/* do not alter original method array until resolution
								 * is over, due to reentrance (143259) */
								if (resolvedMethods == linkedBinding.methods) {
									System.arraycopy(linkedBinding.methods, 0,
											resolvedMethods = new MethodBinding[length], 0, length);
								}
								
								// unable to resolve parameters
								resolvedMethods[i] = null; 
								failed++;
							}
						}

						// find & report collision cases
						boolean complyTo15 = (linkedBinding.scope != null &&
									linkedBinding.scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5);
						for (int i = 0, length = linkedBinding.methods.length; i < length; i++) {
							MethodBinding method = resolvedMethods[i];
							if (method == null) {
								continue;
							}
							char[] selector = method.selector;
							AbstractMethodDeclaration methodDecl = null;
							nextSibling: for (int j = i + 1; j < length; j++) {
								MethodBinding method2 = resolvedMethods[j];
								if (method2 == null) {
									continue nextSibling;
								}
								
								//if its the same method skip ahead
								if(method == method2) {
									break nextSibling;
								}
								
								// methods with same selector are contiguous
								if (!CharOperation.equals(selector, method2.selector)) {
									break nextSibling;
								}

								if (complyTo15 && method.returnType != null
										&& method2.returnType != null) {
									/* 8.4.2, for collision to be detected between m1
									 * and m2:
									 * signature(m1) == signature(m2) i.e. same arity,
									 * same type parameter count, can be substituted
									 * signature(m1) == erasure(signature(m2)) or
									 * erasure(signature(m1)) == signature(m2) */
									TypeBinding[] params1 = method.parameters;
									TypeBinding[] params2 = method2.parameters;
									int pLength = params1.length;
									if (pLength != params2.length) {
										continue nextSibling;
									}

									MethodBinding subMethod = method2;
									boolean equalParams = method .areParametersEqual(subMethod);
									if (equalParams) {
										// duplicates regardless of return types
									} else if (method.returnType == subMethod.returnType
											&& (equalParams || method.areParametersEqual(method2))) {
										
										// name clash for sure if not duplicates, report as duplicates
									} else if (pLength > 0) {
										// check to see if the erasure of either method is equal to the other
										int index = pLength;
										for (; --index >= 0;) {
											if (params1[index] != params2[index]) {
												break;
											}
										}
										if (index >= 0 && index < pLength) {
											for (index = pLength; --index >= 0;) {
												if (params1[index] != params2[index]) {
													break;
												}
											}
										}
										if (index >= 0) {
											continue nextSibling;
										}
									}
								}
								// prior to 1.5, parameter identity meant a collision case
								else if (!method.areParametersEqual(method2)) {
									continue nextSibling;
								}
								
								// report duplicate
								if (methodDecl == null) {
									// cannot be retrieved after binding is lost & may still be null if method is special
									methodDecl = method.sourceMethod();
									
									//ensure its a valid user defined method
									if (methodDecl != null && methodDecl.hasBinding()) {
										linkedBinding.scope.problemReporter().duplicateMethodInType(linkedBinding, methodDecl);

										methodDecl.setBinding(null);
										/* do not alter original method array until
										 * resolution is over, due to reentrance (143259) */
										if (resolvedMethods == linkedBinding.methods) {
											System.arraycopy(linkedBinding.methods, 0,
														resolvedMethods = new MethodBinding[length], 0, length);
										}
										resolvedMethods[i] = null;
										failed++;
									}
								}
								AbstractMethodDeclaration method2Decl = method2.sourceMethod();
								
								//ensure its a valid user defined method
								if (method2Decl != null && method2Decl.hasBinding()) {
									linkedBinding.scope.problemReporter().duplicateMethodInType(linkedBinding, method2Decl);

									method2Decl.setBinding(null);
									/* do not alter original method array until
									 * resolution is over, due to reentrance (143259) */
									if (resolvedMethods == linkedBinding.methods) {
										System.arraycopy(linkedBinding.methods, 0,
													resolvedMethods = new MethodBinding[length], 0, length);
									}
									resolvedMethods[j] = null;
									failed++;
								}
							}
							
							//forget method with invalid return type... was kept to detect possible collisions
							if (method != null && method.returnType == null && methodDecl == null) {
								methodDecl = method.sourceMethod();
								if (methodDecl != null) {
									methodDecl.setBinding(null);
								}
								/* do not alter original method array until resolution
								 * is over, due to reentrance (143259) */
								if (resolvedMethods == linkedBinding.methods) {
									System.arraycopy(linkedBinding.methods, 0,
												resolvedMethods = new MethodBinding[length], 0, length);
								}
								resolvedMethods[i] = null;
								failed++;
							}
						}
					} finally {
						if (failed > 0) {
							int newSize = resolvedMethods.length - failed;
							if (newSize == 0) {
								linkedBinding.setMethods(Binding.NO_METHODS);
							} else {
								MethodBinding[] newMethods = new MethodBinding[newSize];
								for (int i = 0, j = 0, length = resolvedMethods.length; i < length; i++) {
									if (resolvedMethods[i] != null) {
										newMethods[j++] = resolvedMethods[i];
									}
								}
								linkedBinding.setMethods(newMethods);
							}
						}

						// mark functions as complete
						linkedBinding.tagBits |= TagBits.AreMethodsComplete;
					}
				}
				
				//combine newly found functions with already found ones
				if(this.fAllFunctions == null) {
					this.fAllFunctions = linkedBinding.methods;
				} else {
					MethodBinding[] combinedFunctions = new MethodBinding[this.fAllFunctions.length + linkedBinding.methods.length];
		    		System.arraycopy(this.fAllFunctions, 0, combinedFunctions, 0, this.fAllFunctions.length);
		    		System.arraycopy(linkedBinding.methods, 0, combinedFunctions, this.fAllFunctions.length, linkedBinding.methods.length);
		    		this.fAllFunctions = combinedFunctions;
				}
				
				// always search every linked type
				return true;
			}
			
			/**
			 * @return {@link MethodBinding}[]
			 * 
			 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#getFinalResult()
			 */
			public Object getFinalResult() {
				return this.fAllFunctions;
			}
		});
		
		return allFunctions;
	}

	private FieldBinding resolveTypeFor(FieldBinding field) {
		if ((field.modifiers & ExtraCompilerModifiers.AccUnresolved) == 0) {
			return field;
		}

		if (isViewedAsDeprecated() && !field.isDeprecated()) {
			field.modifiers |= ExtraCompilerModifiers.AccDeprecatedImplicitly;
		}
		
		if (hasRestrictedAccess()) {
			field.modifiers |= ExtraCompilerModifiers.AccRestrictedAccess;
		}
		
		return field;
	}

	public MethodBinding resolveTypesFor(MethodBinding method) {
		return resolveTypesFor(method, null);
	}

	public MethodBinding resolveTypesFor(MethodBinding method,
			AbstractMethodDeclaration methodDecl) {
		if ((method.modifiers & ExtraCompilerModifiers.AccUnresolved) == 0)
			return method;

		if (isViewedAsDeprecated() && !method.isDeprecated())
			method.modifiers |= ExtraCompilerModifiers.AccDeprecatedImplicitly;
		if (hasRestrictedAccess())
			method.modifiers |= ExtraCompilerModifiers.AccRestrictedAccess;

		if (methodDecl == null)
			methodDecl = method.sourceMethod();
		if (methodDecl == null)
			return null; // method could not be resolved in previous iteration
		
		boolean foundArgProblem = false;
		Argument[] arguments = methodDecl.arguments;
		if (arguments != null) {
			int size = arguments.length;
			method.setParameters(Binding.NO_PARAMETERS);
			TypeBinding[] newParameters = new TypeBinding[size];
			for (int i = 0; i < size; i++) {
				Argument arg = arguments[i];
				TypeBinding parameterType = TypeBinding.UNKNOWN;
				if (arg.type != null) {
					parameterType = arg.type
							.resolveType(methodDecl.getScope(), true /* check bounds */);
				} else if (arg.inferredType != null) {
					/* if argument has an anonymous inferred type then it has not been built
					 * at this point, so build it before attempt to resolve it. */
					if(arg.inferredType.isAnonymous && arg.inferredType.binding == null) {
						ReferenceBinding argTypeBinding = methodDecl.getScope().findType(
									arg.inferredType.getName(), this.getPackage(), this.getPackage());
						if(argTypeBinding instanceof SourceTypeBinding) {
							arg.inferredType.binding = (SourceTypeBinding) argTypeBinding;
						}
					}
					
					parameterType = arg.inferredType.resolveType(
							methodDecl.getScope(), arg);
				}

				if (parameterType == null) {
					parameterType = TypeBinding.ANY;
				}
				
				newParameters[i] = parameterType;
				if(arg.binding == null) {
					arg.binding = new LocalVariableBinding(arg, parameterType,
						arg.modifiers, true);
				}
			}
			// only assign parameters if no problems are found
			if (!foundArgProblem) {
				method.setParameters(newParameters);
			}
		}

		boolean foundReturnTypeProblem = false;
		if (!method.isConstructor()) {
			TypeReference returnType = methodDecl instanceof MethodDeclaration ? ((MethodDeclaration) methodDecl).returnType
					: null;
			if (returnType == null
					&& !(methodDecl instanceof MethodDeclaration)) {
				methodDecl.getScope().problemReporter()
						.missingReturnType(methodDecl);
				method.returnType = null;
				foundReturnTypeProblem = true;
			} else {
				TypeBinding methodType = (returnType != null) ? returnType
						.resolveType(methodDecl.getScope(), true /* check bounds */)
						: null;
				if (methodType == null)
					methodType = (methodDecl.inferredType != null) ? methodDecl.inferredType
							.resolveType(methodDecl.getScope(), methodDecl)
							: TypeBinding.UNKNOWN;
				if (methodType == null) {
					foundReturnTypeProblem = true;
				} else {
					method.returnType = methodType;
				}
			}
		}
		if (foundArgProblem) {
			methodDecl.setBinding(null);
			method.setParameters(Binding.NO_PARAMETERS); // see 107004
			// nullify type parameter bindings as well as they have a
			// backpointer to the method binding
			// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=81134)
			return null;
		}
		if (foundReturnTypeProblem)
			return method; // but its still unresolved with a null return type &
							// is still connected to its method declaration

		method.modifiers &= ~ExtraCompilerModifiers.AccUnresolved;
		return method;
	}

	public void setFields(FieldBinding[] fields) {
		this.tagBits &= ~TagBits.AreFieldsSorted;
		this.fields = fields;
	}

	public void setMethods(MethodBinding[] methods) {
		this.tagBits &= ~TagBits.AreMethodsSorted;
		this.methods = methods;
	}

	public int sourceEnd() {
		if (this.classScope.referenceContext != null) {
			return this.classScope.referenceContext.sourceEnd;
		} else {
			return this.classScope.inferredType.sourceEnd;
	}
	}

	public int sourceStart() {
		if (this.classScope.referenceContext != null) {
			return this.classScope.referenceContext.sourceStart;
		} else {
			return this.classScope.inferredType.sourceStart;
	}
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding#superclass()
	 */
	/**
	 * <p>
	 * Will return the super binding set on this binding or if this bindings
	 * super biding is null or <code>Object</code> will return the super
	 * binding set on the first linked binding who's super binding is not null
	 * and not <code>Object</code>
	 * </p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding#getSuperBinding()
	 * 
	 * @see #getSuperBinding0()
	 */
	public ReferenceBinding getSuperBinding() {
		//search all linked type bindings for the first one with a super type that is not Object
		ReferenceBinding superBinding = (ReferenceBinding)this.performActionOnLinkedBindings(new LinkedBindingAction() {
			/**
			 * <p>
			 * First super type found when searching all linked type bindings.
			 * </p>
			 */
			private ReferenceBinding fFoundSuperBinding = null;
			
			/**
			 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#performAction(org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding)
			 */
			public boolean performAction(SourceTypeBinding linkedBinding) {
				ReferenceBinding linkedSuperBinding = linkedBinding.getSuperBinding0();
				
				/* Be sure that the super type of a linked binding is not the same as this type
				 * This can legitimately happen when using a pattern like:
				 * 
				 * define("foo.BarImpl", "foo.Bar", {}):
				 * foo.Bar = foo.BarImpl;
				 * 
				 * A best effort is made to avoid this at the infer level by setting it only as the
				 * super type and not as a synonym, but still best to have this check here */
				if(linkedSuperBinding != null && linkedSuperBinding != SourceTypeBinding.this &&
							(this.fFoundSuperBinding == null || (linkedSuperBinding.id != TypeIds.T_JavaLangObject))) {
					
					this.fFoundSuperBinding = linkedSuperBinding;
				}
				
				//keep searching if super type is null or Object
				return this.fFoundSuperBinding == null || this.fFoundSuperBinding.id == TypeIds.T_JavaLangObject;
			}
			
			/**
			 * @return {@link ReferenceBinding}
			 * 
			 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#getFinalResult()
			 */
			public Object getFinalResult() {
				return this.fFoundSuperBinding;
			}
		});
		
		return superBinding;
	}
	
	/**
	 * @return {@link SourceTypeBinding} set as the super binding for this
	 *         binding only. Unlike {@link #getSuperBinding()} this function
	 *         will not search the linked bindings.
	 * 
	 * @see #getSuperBinding()
	 */
	public ReferenceBinding getSuperBinding0() {
		return this.fSuperBinding;
	}
	
	/**
	 * <p>
	 * Sets the super binding for this specific binding. This will overwrite
	 * any currently set super binding for this binding.
	 * </p>
	 * 
	 * <p>
	 * <b>WARNING:</b> A linked binding may have a different super binding.
	 * </p>
	 * 
	 * @param newSuperBinding
	 *            {@link SourceTypeBinding} to set as the super binding for
	 *            this binding, will overwrite any currently set super binding
	 */
	public void setSuperBinding(ReferenceBinding newSuperBinding) {
		this.fSuperBinding = newSuperBinding;
	}

	public String toString() {
		final StringBuffer buffer = new StringBuffer(30);
		buffer.append("(id="); //$NON-NLS-1$
		if (this.id == TypeIds.NoId)
			buffer.append("NoId"); //$NON-NLS-1$
		else
			buffer.append(this.id);
		buffer.append(")\n"); //$NON-NLS-1$
		if (isDeprecated())
			buffer.append("deprecated "); //$NON-NLS-1$
		if (isPublic())
			buffer.append("public "); //$NON-NLS-1$
		if (isPrivate())
			buffer.append("private "); //$NON-NLS-1$
		if (isStatic() && isNestedType())
			buffer.append("static "); //$NON-NLS-1$

		if (isClass())
			buffer.append("class "); //$NON-NLS-1$
		else
			buffer.append("interface "); //$NON-NLS-1$
		buffer.append((this.compoundName != null) ? CharOperation
				.toString(this.compoundName) : "UNNAMED TYPE"); //$NON-NLS-1$

		buffer.append("\n\textends "); //$NON-NLS-1$
		buffer.append((this.fSuperBinding != null) ? this.fSuperBinding.debugName()
				: "NULL TYPE"); //$NON-NLS-1$

		if (enclosingType() != null) {
			buffer.append("\n\tenclosing type : "); //$NON-NLS-1$
			buffer.append(enclosingType().debugName());
		}

		if (this.fields != null) {
			if (this.fields != Binding.NO_FIELDS) {
				buffer.append("\n/*   fields   */"); //$NON-NLS-1$
				for (int i = 0, length = this.fields.length; i < length; i++)
					buffer.append('\n').append(
							(this.fields[i] != null) ? this.fields[i]
									.toString() : "NULL FIELD"); //$NON-NLS-1$
			}
		} else {
			buffer.append("NULL FIELDS"); //$NON-NLS-1$
		}

		if (this.methods != null) {
			if (this.methods != Binding.NO_METHODS) {
				buffer.append("\n/*   methods   */"); //$NON-NLS-1$
				for (int i = 0, length = this.methods.length; i < length; i++)
					buffer.append('\n').append(
							(this.methods[i] != null) ? this.methods[i]
									.toString() : "NULL METHOD"); //$NON-NLS-1$
			}
		} else {
			buffer.append("NULL METHODS"); //$NON-NLS-1$
		}

		if (this.memberTypes != null) {
			if (this.memberTypes != Binding.NO_MEMBER_TYPES) {
				buffer.append("\n/*   members   */"); //$NON-NLS-1$
				for (int i = 0, length = this.memberTypes.length; i < length; i++)
					buffer.append('\n').append(
							(this.memberTypes[i] != null) ? this.memberTypes[i]
									.toString() : "NULL TYPE"); //$NON-NLS-1$
			}
		} else {
			buffer.append("NULL MEMBER TYPES"); //$NON-NLS-1$
		}
		
		//if debugging enabled then print out all the linked type names and their hashes
		if(DEBUG) {
			buffer.append("\n\nLINKED TYPE NAMES:\n"); //$NON-NLS-1$
			this.performActionOnLinkedBindings(new LinkedBindingAction() {
				public boolean performAction(SourceTypeBinding linkedBinding) {
					buffer.append(linkedBinding.qualifiedSourceName0());
					buffer.append(" -> "); //$NON-NLS-1$
					buffer.append(Integer.toHexString(System.identityHashCode(linkedBinding)));
					buffer.append("\n"); //$NON-NLS-1$
					return true;
				}
			});
		}

		buffer.append("\n\n"); //$NON-NLS-1$
		return buffer.toString();
	}

	public AbstractMethodDeclaration sourceMethod(MethodBinding binding) {
		if (this.classScope == null) {
			return null;
	}

		InferredType inferredType = this.classScope.inferredType;
		InferredMethod inferredMethod = inferredType.findMethod(
				binding.selector, null);
		if (inferredMethod != null) {
			return (AbstractMethodDeclaration) inferredMethod.getFunctionDeclaration();
		}
		return null;
	}

	public void addMethod(MethodBinding binding) {
		this.tagBits &= ~TagBits.AreMethodsSorted;
		
		int length = this.methods.length;
		System.arraycopy(this.methods, 0,
				this.methods = new MethodBinding[length + 1], 0, length);
		this.methods[length] = binding;
	}
	
	/**
	 * @param binding {@link FieldBinding} to add to this type binding
	 */
	public void addField(FieldBinding binding) {
		this.tagBits &= ~TagBits.AreFieldsSorted;
		
		int length = this.fields.length;
		System.arraycopy(this.fields, 0,
				this.fields = new FieldBinding[length + 1], 0, length);
		this.fields[length] = binding;
	}
	
	/**
	 * <p>
	 * Adds new function bindings to this type binding.
	 * </p>
	 * 
	 * @param newFunctionBindings
	 *            {@link MethodBinding}s to add to this type binding
	 */
	private void addMethods(MethodBinding[] newFunctionBindings) {
		this.tagBits &= ~TagBits.AreMethodsSorted;
		
		int length = this.methods.length;
		System.arraycopy(this.methods, 0,
					this.methods = new MethodBinding[length + newFunctionBindings.length], 0, length);
		System.arraycopy(newFunctionBindings, 0, this.methods, length, newFunctionBindings.length);
	}
	
	/**
	 * <p>
	 * Adds new field bindings to this type binding.
	 * </p>
	 * 
	 * @param newFieldBindings
	 *            {@link FieldBinding}s to add to this type binding
	 */
	private void addFields(FieldBinding[] newFieldBindings) {
		this.tagBits &= ~TagBits.AreFieldsSorted;
		
		int length = this.fields.length;
		System.arraycopy(this.fields, 0,
					this.fields = new FieldBinding[length + newFieldBindings.length], 0, length);
		System.arraycopy(newFieldBindings, 0, this.fields, length, newFieldBindings.length);
	}

	public void cleanup() {
		this.scope = null;
		this.classScope = null;
	}

	/**
	 * <p>
	 * Determines if a given binding is linked to this binding.
	 * </p>
	 * 
	 * @param searchBinding
	 *            check if this {@link ReferenceBinding} is linked to this binding
	 * 
	 * @return <code>true</code> if any linked types are the given binding,
	 *         <code>false</code> otherwise
	 */
	boolean isLinkedType(final ReferenceBinding searchBinding) {
		// searches all linked bindings to see if any of them are the given search binding
		Boolean isBindingLinked = (Boolean)this.performActionOnLinkedBindings(new LinkedBindingAction() {
			
			/**
			 * <p>
			 * <code>true</code> if any linked types are the given binding, <code>false</code> otherwise
			 * </p>
			 */
			private boolean fIsBindingLinked = false;
			
			/**
			 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#performAction(org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding)
			 */
			public boolean performAction(SourceTypeBinding linkedBinding) {
				this.fIsBindingLinked = searchBinding == linkedBinding;
				
				// keep searching if not found linked the given binding to be linked
				return !this.fIsBindingLinked;
			}
			
			/**
			 * @return {@link Boolean}
			 * 
			 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#getFinalResult()
			 */
			public Object getFinalResult() {
				return new Boolean(this.fIsBindingLinked);
			}
		});
		
		return isBindingLinked.booleanValue();
	}

	/**
	 * <p>
	 * Adds the given new linked type and all of its linked types to this
	 * types circle of linked types. If the given new linked type is already
	 * linked to this type then no operation is taken.
	 * </p>
	 * 
	 * <p>
	 * EX: <br>
	 * this type: A0 -> A1 -> A2 -> A3 -> A0<br>
	 * new linked type: B0 -> B1 -> B0<br>
	 * <br>
	 * combined after this operation:<br>
	 * A0 -> B0 -> B1 -> A1 -> A2 -> A3 -> A0
	 * </p>
	 * 
	 * @param newLinkedBinding
	 *            {@link SourceTypeBinding} to link to this one
	 */
	public void addLinkedBinding(final SourceTypeBinding newLinkedBinding) {
		// determine if the new linked type is a duplicate of a current linked type
		boolean isDuplicate = this.isLinkedType(newLinkedBinding);
		
		/* if linked type is not a duplicate then combine this types linked types
		 * circle with the new types circle into one giant circle
		 * 
		 * EX:
		 * 
		 * this type:  A0 -> A1 -> A2 -> A3 -> A0
		 * new linked type: B0 -> B1 -> B0
		 * 
		 * combined after this operation:
		 * A0 -> B0 -> B1 -> A1 -> A2 -> A3 -> A0
		 */
		if(!isDuplicate) {
			SourceTypeBinding currNextType = this.fNextType;
			this.fNextType = newLinkedBinding;
			
			//search for the end of the linked type circle, aka the type that links back to the new linked type
			SourceTypeBinding newTypesLastNextType = newLinkedBinding;
			while(newTypesLastNextType.fNextType != newLinkedBinding) {
				newTypesLastNextType = newTypesLastNextType.fNextType;
			}
			
			/* assign what was this types next type as the next type for
			 * the last next type in the new linked type's next type circle
			 * 
			 * clear as mud, see comment up a few lines for example of what is going on
			 */
			newTypesLastNextType.fNextType = currNextType;
		}
	}
	
	public static boolean checkIfDuplicateType(SourceTypeBinding binding1, SourceTypeBinding binding2) {
		InferredType type2 = binding2.classScope.inferredType;
		if(binding1.classScope == null) {
			if(binding1.fSuperBinding == null && type2.getSuperType() != null)
				return false;
			if(binding1.fSuperBinding != null && type2.getSuperType() == null)
				return false;
			if(binding1.fSuperBinding != null && type2.getSuperType() != null &&
					!CharOperation.equals(binding1.fSuperBinding.sourceName, type2.getSuperType().getName()))
				return false;
			if(binding1.fields.length != type2.attributes.length)
				return false;
			if(binding1.methods == null && type2.methods != null)
				return false;
			if(binding1.methods != null && type2.methods == null)
				return false;
			if(binding1.methods != null && type2.methods != null && binding1.methods.length != type2.methods.size())
				return false;
		} else {
			InferredType type1 = binding1.classScope.inferredType;

			if(type1.getSuperType() == null && type2.getSuperType() != null)
				return false;
			if(type1.getSuperType() != null && type2.getSuperType() == null)
				return false;
			if(type1.getSuperType() != null && type2.getSuperType() != null &&
					!CharOperation.equals(type1.getSuperType().getName(), type2.getSuperType().getName()))
				return false;
			if(type1.attributes.length != type2.attributes.length)
				return false;
			if(type1.methods == null && type2.methods != null)
				return false;
			if(type1.methods != null && type2.methods == null)
				return false;
			if(type1.methods != null && type2.methods != null && type1.methods.size() != type2.methods.size())
				return false;
			
			StringBuffer checkSumString1 = new StringBuffer();
			StringBuffer checkSumString2 = new StringBuffer();
			
			for(int i = 0; i < type1.attributes.length; i++) {
				checkSumString1.append((type1.attributes[i] == null ? "" : new String(type1.attributes[i].name))); //$NON-NLS-1$
				checkSumString2.append((type2.attributes[i] == null ? "" : new String(type2.attributes[i].name))); //$NON-NLS-1$
			}
			checksumCalculator.reset();
			checksumCalculator.update(checkSumString1.toString().getBytes());
			long checkSum1 = checksumCalculator.getValue();
			checksumCalculator.reset();
			checksumCalculator.update(checkSumString2.toString().getBytes());
			long checkSum2 = checksumCalculator.getValue();
			if(checkSum1 != checkSum2)
				return false;
			
			checkSumString1 = new StringBuffer();
			checkSumString2 = new StringBuffer();
			if(type1.methods != null && type2.methods != null) {
				for(int i = 0; i < type1.methods.size(); i++) {
					checkSumString1.append(new String(((InferredMethod)type1.methods.get(i)).name));
					checkSumString2.append(new String(((InferredMethod)type2.methods.get(i)).name));
				}
			}
			
			checksumCalculator.reset();
			checksumCalculator.update(checkSumString1.toString().getBytes());
			checkSum1 = checksumCalculator.getValue();
			checksumCalculator.reset();
			checksumCalculator.update(checkSumString2.toString().getBytes());
			checkSum2 = checksumCalculator.getValue();
			if(checkSum1 != checkSum2)
				return false;
		}
		return true;
	}

	public TypeBinding reconcileAnonymous(TypeBinding other) {
		if (!(other instanceof SourceTypeBinding)) {
			return null;
		}
		SourceTypeBinding otherBinding = (SourceTypeBinding) other;
		if (!otherBinding.isAnonymousType()) {
			return null;
		}
		
		if (otherBinding.methods != null) {
			for (int i = 0; i < otherBinding.methods.length; i++) {
				MethodBinding methodBinding = otherBinding.methods[i];
				MethodBinding exactMethod = this.getExactMethod(
						methodBinding.selector, methodBinding.parameters, null);
				if (exactMethod == null) {
					return null;
			}
		}
		}

		if (otherBinding.fields != null) {
			for (int i = 0; i < otherBinding.fields.length; i++) {
				FieldBinding fieldBinding = otherBinding.fields[i];
				FieldBinding myField = this.getFieldInHierarchy(
						fieldBinding.name, true);
				if (myField == null) {
					return null;
			}
		}
		}

		return this;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding#readableName()
	 */
	public char[] readableName() {
		return this.qualifiedSourceName();
	}
	
	/**
	 * <p>
	 * Will return the qualified source name from the first linked binding
	 * that is not anonymous, or if no linked bindings are not anonymous then
	 * returns the qualified source name for this binding.
	 * </p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding#qualifiedSourceName()
	 */
	public char[] qualifiedSourceName() {
		char[] qualifiedSourceName = (char[])performActionOnLinkedBindings(new LinkedBindingAction() {
			
			private char[] fQualifiedSourceName = null;
			
			public boolean performAction(SourceTypeBinding linkedBinding) {
				if(!linkedBinding.isAnonymousType()) {
					this.fQualifiedSourceName = linkedBinding.qualifiedSourceName0();
				}
				
				return this.fQualifiedSourceName == null;
			}
			
			/**
			 * @return char[]
			 * 
			 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#getFinalResult()
			 */
			public Object getFinalResult() {
				return this.fQualifiedSourceName;
			}
		});
		
		if(qualifiedSourceName == null) {
			qualifiedSourceName = this.qualifiedSourceName0();
		}
		
		return qualifiedSourceName;
	}
	
	/**
	 * @return qualified source name for this binding
	 */
	private char[] qualifiedSourceName0() {
		return super.qualifiedSourceName();
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding#isSuperclassOf(org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding)
	 */
	public boolean isSuperclassOf(ReferenceBinding otherType) {
		boolean isSuperTypeOf = false;
		
		if(otherType instanceof SourceTypeBinding) {
			//NOTE: this is a breadth first search of the super types
			
			/* compare this type against the super types of the types in this list
			 * 
			 * use the list for quickly iterating over and the set for preventing
			 * duplicates. */
			final LinkedList compareAgainstSupersOfList = new LinkedList();
			compareAgainstSupersOfList.add(otherType);
			final Set compareAgainstSupersOfSet = new HashSet();
			compareAgainstSupersOfSet.add(otherType);
			
			//prevent searching the super of the same types more then once
			final Set alreadyComparedAgainstSupersOf = new HashSet();
			
			//while there are types to compare this type against their super types with keep going
			while(!compareAgainstSupersOfList.isEmpty() && !isSuperTypeOf) {
				SourceTypeBinding checkSupersOf = (SourceTypeBinding)compareAgainstSupersOfList.removeFirst();
				compareAgainstSupersOfSet.remove(checkSupersOf);
				alreadyComparedAgainstSupersOf.add(checkSupersOf);
				
				isSuperTypeOf = ((Boolean)checkSupersOf.performActionOnLinkedBindings(new LinkedBindingAction() {

					private boolean fIsSuperTypeOf = false;
					
					/**
					 * 
					 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#performAction(org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding)
					 */
					public boolean performAction(SourceTypeBinding otherLinkedBinding) {
						ReferenceBinding otherLinkedSuperBinding = otherLinkedBinding.getSuperBinding0();
						
						if(otherLinkedSuperBinding != null &&
									!alreadyComparedAgainstSupersOf.contains(otherLinkedSuperBinding)) {
							
							fIsSuperTypeOf = otherLinkedSuperBinding.isEquivalentTo(SourceTypeBinding.this);
							
							//prevent searching the super of the same types more then once
							if(!compareAgainstSupersOfSet.contains(otherLinkedSuperBinding)) {
								compareAgainstSupersOfList.add(otherLinkedSuperBinding);
								compareAgainstSupersOfSet.add(otherLinkedSuperBinding);
							}
						}
						
						return !fIsSuperTypeOf;
					}
					
					/**
					 * @return {@link ReferenceBinding}
					 * 
					 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding.LinkedBindingAction#getFinalResult()
					 */
					public Object getFinalResult() {
						return new Boolean(this.fIsSuperTypeOf);
					}
				})).booleanValue();
			}
			
			
		} else {
			isSuperTypeOf = super.isSuperclassOf(otherType);
		}
		
		return isSuperTypeOf;
	}
	
	/**
	 * <p>
	 * Determine if this binding's inferred type has a function with the given
	 * name
	 * </p>
	 * 
	 * @param functionName
	 *            determine if this binding's inferred type has a function
	 *            with this name
	 * 
	 * @return <code>true</code>if this binding's inferred type has a function
	 *         with the given name, <code>false</code> otherwise
	 */
	private boolean inferredTypeHasFunction(char[] functionName) {
		InferredType currentType = this.classScope != null ? this.classScope.inferredType : null;
		if(currentType != null) {
			if(currentType.methods != null && currentType.methods.size() > 0) {
				for(int i = 0; i < currentType.methods.size(); i++) {
					InferredMethod method = (InferredMethod) currentType.methods.get(i);
					if(method != null && CharOperation.equals(method.name, functionName)) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * <p>
	 * Determine if this binding's inferred type has a field with the given
	 * name
	 * </p>
	 * 
	 * @param fieldName
	 *            determine if this binding's inferred type has a field with
	 *            this name
	 * 
	 * @return <code>true</code>if this binding's inferred type has a field
	 *         with the given name, <code>false</code> otherwise
	 */
	private boolean inferredTypeHasField(char[] fieldName) {
		InferredType currentType = this.classScope != null ? this.classScope.inferredType : null;
		if(currentType != null) {
			InferredAttribute[] attributes = currentType.attributes;
			if(attributes != null && currentType.numberAttributes > 0) {
				for(int i = 0; i < currentType.numberAttributes; i++) {
					if(attributes[i] != null && CharOperation.equals(attributes[i].name, fieldName)) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * @param binding
	 *            determine if this binding is a function binding
	 * 
	 * @return <code>true</code> if the given {@link TypeBinding} is a
	 *         function binding, <code>false</code> otherwise
	 */
	private static boolean isFunctionType(TypeBinding binding) {
		return binding.isFunctionType() ||
					binding instanceof FunctionTypeBinding ||
					CharOperation.equals(binding.sourceName(), InferredType.FUNCTION_NAME);
	}
	
	/**
	 * <p>
	 * Performs the given action on all of the {@link SourceTypeBinding}s
	 * linked to this one, including this one, unless the loop is cut short by
	 * a <code>false</code> result from
	 * {@link LinkedBindingAction#performAction(SourceTypeBinding)}.
	 * </p>
	 * 
	 * <p>
	 * Whenever an action needs to be performed on all of the linked bindings
	 * this is the method that should be used.
	 * </p>
	 * 
	 * @param action
	 *            {@link LinkedBindingAction} to perform on this binding and
	 *            all of its linked bindings or until
	 *            {@link LinkedBindingAction#performAction(SourceTypeBinding)}
	 *            returns <code>false</code>
	 * 
	 * @return the result of a call to
	 *         {@link LinkedBindingAction#getFinalResult()} on the given action
	 *         after running
	 *         {@link LinkedBindingAction#performAction(SourceTypeBinding)} on
	 *         each of the linked bindings or until
	 *         {@link LinkedBindingAction#performAction(SourceTypeBinding)}
	 *         returned <code>false</code>
	 * 
	 * @see LinkedBindingAction
	 */
	Object performActionOnLinkedBindings(LinkedBindingAction action) {
		SourceTypeBinding currBinding = this;
		
		/* perform the given action each linked type stopping either
		 * when looped back to the beginning or #performAction returns false */
		boolean keepProcessing = true;
		do {
			keepProcessing = action.performAction(currBinding);
			currBinding = currBinding.fNextType;
		} while(currBinding != this && keepProcessing);
		
		return action.getFinalResult();
	}
	
	/**
	 * <p>
	 * An action to perform on a set of linked {@link SourceTypeBinding}s
	 * </p>
	 * 
	 * @see #performAction(SourceTypeBinding)
	 */
	static abstract class LinkedBindingAction {
		/**
		 * <p>
		 * Performs an action on the given {@link SourceTypeBinding}.
		 * </p>
		 * 
		 * @param linkedBinding
		 *            {@link SourceTypeBinding} to perform the action on
		 * 
		 * @return <code>true</code> if the next linked binding should be
		 *         passed to this function, <code>false</code> if the loop
		 *         should stop prematurely
		 */
		public abstract boolean performAction(SourceTypeBinding linkedBinding);

		/**
		 * <p>
		 * Default implementation is to return <code>null</code> assuming no
		 * accumulative result was gathered.
		 * </p>
		 * 
		 * @return final result gathered after calling
		 *         {@link #performAction(SourceTypeBinding)} on the linked
		 *         bindings, default result is <code>null</code>
		 */
		public Object getFinalResult() {
			return null;
		}
	}
}