/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Thompson = bug 214171 -Class cast exception
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.lookup;

import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Argument;
import org.eclipse.wst.jsdt.internal.compiler.ast.CaseStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;

public class BlockScope extends Scope {

	// Local variable management
	public LocalVariableBinding[] locals;
	public MethodBinding[] methods;
	public int numberMethods; // for variable allocation throughout scopes
	public int localIndex; // position for next variable
	public int startIndex;	// start position in this scope - for ordering scopes vs. variables
	public int offset; // for variable allocation throughout scopes
	public int maxOffset; // for variable allocation throughout scopes

	// finally scopes must be shifted behind respective try&catch scope(s) so as to avoid
	// collisions of secret variables (return address, save value).
	public BlockScope[] shiftScopes;

	public Scope[] subscopes = new Scope[1]; // need access from code assist
	public int subscopeCount = 0; // need access from code assist
	// record the current case statement being processed (for entire switch case block).
	public CaseStatement enclosingCase; // from 1.4 on, local types should not be accessed across switch case blocks (52221)

public BlockScope(BlockScope parent) {
	this(parent, true);
}

public BlockScope(BlockScope parent, boolean addToParentScope) {
	this(Scope.BLOCK_SCOPE, parent);
	this.locals = new LocalVariableBinding[5];
	this.methods=new MethodBinding[5];
	if (addToParentScope) parent.addSubscope(this);
	this.startIndex = parent.localIndex;
}

public BlockScope(BlockScope parent, int variableCount) {
	this(Scope.BLOCK_SCOPE, parent);
	this.locals = new LocalVariableBinding[variableCount];
	this.methods=new MethodBinding[5];
	parent.addSubscope(this);
	this.startIndex = parent.localIndex;
}

protected BlockScope(int kind, Scope parent) {
	super(kind, parent);
	this.locals = new LocalVariableBinding[5];
	this.methods=new MethodBinding[5];
}

/* Create the class scope & binding for the anonymous type.
 */
public final void addAnonymousType(TypeDeclaration anonymousType, ReferenceBinding superBinding) {
	ClassScope anonymousClassScope = new ClassScope(this, anonymousType);
	anonymousClassScope.buildAnonymousTypeBinding(
		enclosingSourceType(),
		superBinding);
}

/* Create the class scope & binding for the local type.
 */
public final void addLocalType(TypeDeclaration localType) {
	ClassScope localTypeScope = new ClassScope(this, localType);
	addSubscope(localTypeScope);
	localTypeScope.buildLocalTypeBinding(enclosingSourceType());
}

/* Insert a local variable into a given scope, updating its position
 * and checking there are not too many locals or arguments allocated.
 */
public  void addLocalVariable(LocalVariableBinding binding) {
	// insert local in scope
	if (this.localIndex == this.locals.length)
		System.arraycopy(
			this.locals,
			0,
			(this.locals = new LocalVariableBinding[this.localIndex * 2]),
			0,
			this.localIndex);
	this.locals[this.localIndex++] = binding;

	// update local variable binding
	binding.declaringScope = this;

	// share the outermost method scope analysisIndex
	MethodScope outerMostMethodScope = this.outerMostMethodScope();
	binding.id = (outerMostMethodScope!=null)? outerMostMethodScope.analysisIndex++ : this.compilationUnitScope().analysisIndex++;
}

public void addLocalMethod(MethodBinding methodBinding) {
	/* prevent duplicate bindings
	 * NOTE: this has no noticeable affect on performance */
	boolean isDuplicate = false;
	for(int i = 0; i < this.numberMethods && !isDuplicate; ++i) {
		isDuplicate = methodBinding == this.methods[i];
	}
	
	if(!isDuplicate) {
		if (this.numberMethods == this.methods.length) {
			System.arraycopy(
				this.methods,
				0,
				(this.methods = new MethodBinding[this.numberMethods * 2]),
				0,
				this.numberMethods);
		}
		this.methods[this.numberMethods++] = methodBinding;
	}
}


public void addSubscope(Scope childScope) {
	if (this.subscopeCount == this.subscopes.length)
		System.arraycopy(
			this.subscopes,
			0,
			(this.subscopes = new Scope[this.subscopeCount * 2]),
			0,
			this.subscopeCount);
	this.subscopes[this.subscopeCount++] = childScope;
}

/* Answer true if the receiver is suitable for assigning final blank fields.
 *
 * in other words, it is inside an initializer, a constructor or a clinit
 */
public final boolean allowBlankFinalFieldAssignment(FieldBinding binding) {
	if (enclosingReceiverType() != binding.declaringClass)
		return false;

	MethodScope methodScope = methodScope();
	if (methodScope.isStatic != binding.isStatic())
		return false;
	return methodScope.isInsideInitializer() // inside initializer
			|| ((AbstractMethodDeclaration) methodScope.referenceContext).isInitializationMethod(); // inside constructor or clinit
}
String basicToString(int tab) {
	StringBuilder sb = new StringBuilder('\n');
	for (int i = tab; --i >= 0;) {
		sb.append('\t');
	}
	sb.append("--- Block Scope ---"); //$NON-NLS-1$
	sb.append('\t');
	sb.append("locals:"); //$NON-NLS-1$
	for (int i = 0; i < this.localIndex; i++) {
		sb.append('\t');
		sb.append(this.locals[i]);
	}
	sb.append("startIndex = "); //$NON-NLS-1$
	sb.append(this.startIndex);
	return sb.toString();
}

public void reportUnusedDeclarations()
{
	if (this.locals!=null)
	for (int i = 0; i < localIndex; i++) {
		LocalVariableBinding local = this.locals[i]; // if no local at all, will be locals[ilocal]==null


		// do not report fake used variable
		if (local.useFlag == LocalVariableBinding.UNUSED
			&& (local.declaration != null) // unused (and non secret) local
			&& ((local.declaration.bits & ASTNode.IsLocalDeclarationReachable) != 0)) { // declaration is reachable

			if (!(local.declaration instanceof Argument))  // do not report unused catch arguments
				this.problemReporter().unusedLocalVariable(local.declaration);
		}


	}
}


/*
 *	Record the suitable binding denoting a synthetic field or constructor argument,
 * mapping to the actual outer local variable in the scope context.
 * Note that this may not need any effect, in case the outer local variable does not
 * need to be emulated and can directly be used as is (using its back pointer to its
 * declaring scope).
 */
public void emulateOuterAccess(LocalVariableBinding outerLocalVariable) {
	BlockScope outerVariableScope = outerLocalVariable.declaringScope;
	if (outerVariableScope == null)
		return; // no need to further emulate as already inserted (val$this$0)
	MethodScope currentMethodScope = this.methodScope();
	if (outerVariableScope.methodScope() != currentMethodScope &&
			 this.enclosingSourceType() instanceof
			NestedTypeBinding) {
		NestedTypeBinding currentType = (NestedTypeBinding) this.enclosingSourceType();

		//do nothing for member types, pre emulation was performed already
		if (!currentType.isLocalType()) {
			return;
		}
	}
}

/* Note that it must never produce a direct access to the targetEnclosingType,
 * but instead a field sequence (this$2.this$1.this$0) so as to handle such a test case:
 *
 * class XX {
 *	void foo() {
 *		class A {
 *			class B {
 *				class C {
 *					boolean foo() {
 *						return (Object) A.this == (Object) B.this;
 *					}
 *				}
 *			}
 *		}
 *		new A().new B().new C();
 *	}
 * }
 * where we only want to deal with ONE enclosing instance for C (could not figure out an A for C)
 */
public final ReferenceBinding findLocalType(char[] name) {
	long compliance = compilerOptions().complianceLevel;
	for (int i = this.subscopeCount-1; i >= 0; i--) {
		if (this.subscopes[i] instanceof ClassScope) {
			LocalTypeBinding sourceType = (LocalTypeBinding)((ClassScope) this.subscopes[i]).getReferenceBinding();
			// from 1.4 on, local types should not be accessed across switch case blocks (52221)
			if (compliance >= ClassFileConstants.JDK1_4 && sourceType.enclosingCase != null) {
				if (!this.isInsideCase(sourceType.enclosingCase)) {
					continue;
				}
			}
			if (CharOperation.equals(sourceType.sourceName(), name))
				return sourceType;
		}
	}
	return null;
}
public MethodBinding findMethod(char[] methodName,TypeBinding[]argumentTypes, boolean checkVars) {
	int methodLength = methodName.length;
	for (int i = this.numberMethods-1; i >= 0; i--) {
		MethodBinding method;
		char[] name;
		if ((name = (method = this.methods[i]).selector) != null && name.length == methodLength && CharOperation.equals(name, methodName)) {
			return method;
		}
	}
	if (checkVars)
	{
		LocalVariableBinding variable = findVariable(methodName);
		if (variable!=null)
		{
			MethodBinding binding;
			if (!(variable.type.isAnyType() || variable.type.isFunctionType()))
			{
			  binding=new ProblemMethodBinding(methodName,null,ProblemReasons.NotAFunction);	
			}
			else	
			 binding = new MethodBinding(ClassFileConstants.AccPublic,
					methodName,TypeBinding.UNKNOWN,null,variable.declaringScope.enclosingTypeBinding());
			
			addLocalMethod(binding);
			return binding;
		}
	}
	return null;
}


/**
 * Returns all declarations of most specific locals containing a given position in their source range.
 * This code does not recurse in nested types.
 * Returned array may have null values at trailing indexes.
 */
public LocalDeclaration[] findLocalVariableDeclarations(int position) {
	// local variable init
	int ilocal = 0, maxLocals = this.localIndex;
	boolean hasMoreVariables = maxLocals > 0;
	LocalDeclaration[] localDeclarations = null;
	int declPtr = 0;

	// scope init
	int iscope = 0, maxScopes = this.subscopeCount;
	boolean hasMoreScopes = maxScopes > 0;

	// iterate scopes and variables in parallel
	while (hasMoreVariables || hasMoreScopes) {
		if (hasMoreScopes
			&& (!hasMoreVariables || (this.subscopes[iscope].startIndex() <= ilocal))) {
			// consider subscope first
			Scope subscope = this.subscopes[iscope];
			if (subscope.kind == Scope.BLOCK_SCOPE) { // do not dive in nested types
				localDeclarations = ((BlockScope)subscope).findLocalVariableDeclarations(position);
				if (localDeclarations != null) {
					return localDeclarations;
				}
			}
			hasMoreScopes = ++iscope < maxScopes;
		} else {
			// consider variable first
			LocalVariableBinding local = this.locals[ilocal]; // if no local at all, will be locals[ilocal]==null
			if (local != null) {
				LocalDeclaration localDecl = local.declaration;
				if (localDecl != null) {
					if (localDecl.declarationSourceStart <= position) {
						if (position <= localDecl.declarationSourceEnd) {
							if (localDeclarations == null) {
								localDeclarations = new LocalDeclaration[maxLocals];
							}
							localDeclarations[declPtr++] = localDecl;
						}
					} else {
						return localDeclarations;
					}
				}
			}
			hasMoreVariables = ++ilocal < maxLocals;
			if (!hasMoreVariables && localDeclarations != null) {
				return localDeclarations;
			}
		}
	}
	return null;
}

public LocalVariableBinding findVariable(char[] variableName) {
	int varLength = variableName.length;
	for (int i = this.localIndex-1; i >= 0; i--) { // lookup backward to reach latest additions first
		LocalVariableBinding local;
		char[] localName;
		if ((localName = (local = this.locals[i]).name).length == varLength && CharOperation.equals(localName, variableName))
			return local;
	}
	return null;
}

/* API
 * flag is a mask of the following values VARIABLE (= FIELD or LOCAL), TYPE.
 * Only bindings corresponding to the mask will be answered.
 *
 *	if the VARIABLE mask is set then
 *		If the first name provided is a field (or local) then the field (or local) is answered
 *		Otherwise, package names and type names are consumed until a field is found.
 *		In this case, the field is answered.
 *
 *	if the TYPE mask is set,
 *		package names and type names are consumed until the end of the input.
 *		Only if all of the input is consumed is the type answered
 *
 *	All other conditions are errors, and a problem binding is returned.
 *
 *	NOTE: If a problem binding is returned, senders should extract the compound name
 *	from the binding & not assume the problem applies to the entire compoundName.
 *
 *	The VARIABLE mask has precedence over the TYPE mask.
 *
 *	InvocationSite implements
 *		isSuperAccess(); this is used to determine if the discovered field is visible.
 *		setFieldIndex(int); this is used to record the number of names that were consumed.
 *
 *	For example, getBinding({"foo","y","q", VARIABLE, site) will answer
 *	the binding for the field or local named "foo" (or an error binding if none exists).
 *	In addition, setFieldIndex(1) will be sent to the invocation site.
 *	If a type named "foo" exists, it will not be detected (and an error binding will be answered)
 *
 *	IMPORTANT NOTE: This method is written under the assumption that compoundName is longer than length 1.
 */
public Binding getBinding(char[][] compoundName, int mask, InvocationSite invocationSite, boolean needResolve) {
	Binding binding = getBinding(compoundName[0], mask | Binding.TYPE | Binding.PACKAGE, invocationSite, needResolve);
	invocationSite.setFieldIndex(1);
	if (binding instanceof VariableBinding) return binding;
	CompilationUnitScope unitScope = compilationUnitScope();
	// in the problem case, we want to ensure we record the qualified dependency in case a type is added
	// and we do not know that its package was also added (can happen with validationParticipants)
	unitScope.recordQualifiedReference(compoundName);
	if (!binding.isValidBinding()) return binding;

	int length = compoundName.length;
	int currentIndex = 1;
	foundType : if (binding instanceof PackageBinding) {
		PackageBinding packageBinding = (PackageBinding) binding;
		while (currentIndex < length) {
			unitScope.recordReference(packageBinding.compoundName, compoundName[currentIndex]);
			binding = packageBinding.getTypeOrPackage(compoundName[currentIndex++], mask);
			invocationSite.setFieldIndex(currentIndex);
			if (binding == null) {
				if (currentIndex == length) {
					// must be a type if its the last name, otherwise we have no idea if its a package or type
					return new ProblemReferenceBinding(
						CharOperation.subarray(compoundName, 0, currentIndex),
						null,
						ProblemReasons.NotFound);
				}
				return new ProblemBinding(
					CharOperation.subarray(compoundName, 0, currentIndex),
					ProblemReasons.NotFound);
			}
			if (binding instanceof ReferenceBinding) {
				if (!binding.isValidBinding())
					return new ProblemReferenceBinding(
						CharOperation.subarray(compoundName, 0, currentIndex),
						((ReferenceBinding)binding).closestMatch(),
						binding.problemId());
				if (!((ReferenceBinding) binding).canBeSeenBy(this))
					return new ProblemReferenceBinding(
						CharOperation.subarray(compoundName, 0, currentIndex),
						(ReferenceBinding) binding,
						ProblemReasons.NotVisible);
				break foundType;
			}
			packageBinding = (PackageBinding) binding;
		}

		// It is illegal to request a PACKAGE from this method.
		return new ProblemReferenceBinding(
			CharOperation.subarray(compoundName, 0, currentIndex),
			null,
			ProblemReasons.NotFound);
	}

	// know binding is now a ReferenceBinding
	ReferenceBinding referenceBinding = (ReferenceBinding) binding;
	binding = referenceBinding;
	if (invocationSite instanceof ASTNode) {
		ASTNode invocationNode = (ASTNode) invocationSite;
		if (invocationNode.isTypeUseDeprecated(referenceBinding, this)) {
			problemReporter().deprecatedType(referenceBinding, invocationNode);
		}
	}
	while (currentIndex < length) {
		referenceBinding = (ReferenceBinding) binding;
		char[] nextName = compoundName[currentIndex++];
		invocationSite.setFieldIndex(currentIndex);
		invocationSite.setActualReceiverType(referenceBinding);
		if ((mask & Binding.FIELD) != 0 && (binding = findField(referenceBinding, nextName, invocationSite, true /*resolve*/)) != null) {
			if (!binding.isValidBinding()) {
				return new ProblemFieldBinding(
					((ProblemFieldBinding)binding).closestMatch,
					((ProblemFieldBinding)binding).declaringClass,
					CharOperation.concatWith(CharOperation.subarray(compoundName, 0, currentIndex), '.'),
					binding.problemId());
			}
			break; // binding is now a field
		}
		if ((binding = findMemberType(nextName, referenceBinding)) == null) {
			if ((mask & Binding.FIELD) != 0) {
				return new ProblemBinding(
					CharOperation.subarray(compoundName, 0, currentIndex),
					referenceBinding,
					ProblemReasons.NotFound);
			}
			return new ProblemReferenceBinding(
				CharOperation.subarray(compoundName, 0, currentIndex),
				referenceBinding,
				ProblemReasons.NotFound);
		}
		// binding is a ReferenceBinding
		if (!binding.isValidBinding())
			return new ProblemReferenceBinding(
				CharOperation.subarray(compoundName, 0, currentIndex),
				((ReferenceBinding)binding).closestMatch(),
				binding.problemId());
		if (invocationSite instanceof ASTNode) {
			referenceBinding = (ReferenceBinding) binding;
			ASTNode invocationNode = (ASTNode) invocationSite;
			if (invocationNode.isTypeUseDeprecated(referenceBinding, this)) {
				problemReporter().deprecatedType(referenceBinding, invocationNode);
			}
		}
	}
	if ((mask & Binding.FIELD) != 0 && (binding instanceof FieldBinding)) {
		// was looking for a field and found a field
		FieldBinding field = (FieldBinding) binding;
		if (!field.isStatic())
			return new ProblemFieldBinding(
				field,
				field.declaringClass,
				CharOperation.concatWith(CharOperation.subarray(compoundName, 0, currentIndex), '.'),
				ProblemReasons.NonStaticReferenceInStaticContext);
		return binding;
	}
	if ((mask & Binding.TYPE) != 0 && (binding instanceof ReferenceBinding)) {
		// was looking for a type and found a type
		return binding;
	}

	// handle the case when a field or type was asked for but we resolved the compoundName to a type or field
	return new ProblemBinding(
		CharOperation.subarray(compoundName, 0, currentIndex),
		ProblemReasons.NotFound);
}

// Added for code assist... NOT Public API
public final Binding getBinding(char[][] compoundName, InvocationSite invocationSite) {
	int currentIndex = 0;
	int length = compoundName.length;
	Binding binding =
		getBinding(
			compoundName[currentIndex++],
			Binding.VARIABLE | Binding.TYPE | Binding.PACKAGE,
			invocationSite,
			true /*resolve*/);
	if (!binding.isValidBinding())
		return binding;

	foundType : if (binding instanceof PackageBinding) {
		while (currentIndex < length) {
			PackageBinding packageBinding = (PackageBinding) binding;
			binding = packageBinding.getTypeOrPackage(compoundName[currentIndex++], Binding.VARIABLE | Binding.TYPE | Binding.PACKAGE);

			if (binding == null) {
				if (currentIndex == length) {
					// must be a type if its the last name, otherwise we have no idea if its a package or type
					return new ProblemReferenceBinding(
						CharOperation.subarray(compoundName, 0, currentIndex),
						null,
						ProblemReasons.NotFound);
				}
				return new ProblemBinding(
					CharOperation.subarray(compoundName, 0, currentIndex),
					ProblemReasons.NotFound);
			}
			if (binding instanceof ReferenceBinding) {
				if (!binding.isValidBinding())
					return new ProblemReferenceBinding(
						CharOperation.subarray(compoundName, 0, currentIndex),
						((ReferenceBinding)binding).closestMatch(),
						binding.problemId());
				if (!((ReferenceBinding) binding).canBeSeenBy(this))
					return new ProblemReferenceBinding(
						CharOperation.subarray(compoundName, 0, currentIndex),
						(ReferenceBinding) binding,
						ProblemReasons.NotVisible);
				break foundType;
			}
		}
		return binding;
	}

	foundField : if (binding instanceof ReferenceBinding) {
		while (currentIndex < length) {
			ReferenceBinding typeBinding = (ReferenceBinding) binding;
			char[] nextName = compoundName[currentIndex++];
			if ((binding = findField(typeBinding, nextName, invocationSite, true /*resolve*/)) != null) {
				if (!binding.isValidBinding()) {
					return new ProblemFieldBinding(
						(FieldBinding) binding,
						((FieldBinding) binding).declaringClass,
						CharOperation.concatWith(CharOperation.subarray(compoundName, 0, currentIndex), '.'),
						binding.problemId());
				}
				if (!((FieldBinding) binding).isStatic())
					return new ProblemFieldBinding(
						(FieldBinding) binding,
						((FieldBinding) binding).declaringClass,
						CharOperation.concatWith(CharOperation.subarray(compoundName, 0, currentIndex), '.'),
						ProblemReasons.NonStaticReferenceInStaticContext);
				break foundField; // binding is now a field
			}
			if ((binding = findMemberType(nextName, typeBinding)) == null) {
				return new ProblemBinding(
					CharOperation.subarray(compoundName, 0, currentIndex),
					typeBinding,
					ProblemReasons.NotFound);
			}
			if (!binding.isValidBinding()) {
				return new ProblemReferenceBinding(
					CharOperation.subarray(compoundName, 0, currentIndex),
					((ReferenceBinding)binding).closestMatch(),
					binding.problemId());
			}
		}
		return binding;
	}

	VariableBinding variableBinding = (VariableBinding) binding;
	while (currentIndex < length) {
		TypeBinding typeBinding = variableBinding.type;
		if (typeBinding == null) {
			return new ProblemFieldBinding(
				null,
				null,
				CharOperation.concatWith(CharOperation.subarray(compoundName, 0, currentIndex), '.'),
				ProblemReasons.NotFound);
		}
		variableBinding = findField(typeBinding, compoundName[currentIndex++], invocationSite, true /*resolve*/);
		if (variableBinding == null) {
			return new ProblemFieldBinding(
				null,
				null,
				CharOperation.concatWith(CharOperation.subarray(compoundName, 0, currentIndex), '.'),
				ProblemReasons.NotFound);
		}
		if (!variableBinding.isValidBinding())
			return variableBinding;
	}
	return variableBinding;
}

/*
 * This retrieves the argument that maps to an enclosing instance of the suitable type,
 * 	if not found then answers nil -- do not create one
 *
 *		#implicitThis		  	 			: the implicit this will be ok
 *		#((arg) this$n)						: available as a constructor arg
 * 		#((arg) this$n ... this$p) 			: available as as a constructor arg + a sequence of fields
 * 		#((fieldDescr) this$n ... this$p) 	: available as a sequence of fields
 * 		nil 		 											: not found
 *
 * 	Note that this algorithm should answer the shortest possible sequence when
 * 		shortcuts are available:
 * 				this$0 . this$0 . this$0
 * 		instead of
 * 				this$2 . this$1 . this$0 . this$1 . this$0
 * 		thus the code generation will be more compact and runtime faster
 */
public VariableBinding[] getEmulationPath(LocalVariableBinding outerLocalVariable) {
	MethodScope currentMethodScope = this.methodScope();
	SourceTypeBinding sourceType = currentMethodScope.enclosingSourceType();

	// identity check
	BlockScope variableScope = outerLocalVariable.declaringScope;
	if (variableScope == null /*val$this$0*/ || currentMethodScope == variableScope.methodScope()) {
		return new VariableBinding[] { outerLocalVariable };
		// implicit this is good enough
	}
	return null;
}

/* Answer true if the variable name already exists within the receiver's scope.
 */
public final boolean isDuplicateLocalVariable(char[] name) {
	BlockScope current = this;
	while (true) {
		for (int i = 0; i < this.localIndex; i++) {
			if (CharOperation.equals(name, current.locals[i].name))
				return true;
		}
		if (current.kind != Scope.BLOCK_SCOPE) return false;
		current = (BlockScope)current.parent;
	}
}

public int maxShiftedOffset() {
	int max = -1;
	if (this.shiftScopes != null){
		for (int i = 0, length = this.shiftScopes.length; i < length; i++){
			int subMaxOffset = this.shiftScopes[i].maxOffset;
			if (subMaxOffset > max) max = subMaxOffset;
		}
	}
	return max;
}

/* Answer the problem reporter to use for raising new problems.
 *
 * Note that as a side-effect, this updates the current reference context
 * (unit, type or method) in case the problem handler decides it is necessary
 * to abort.
 */
public ProblemReporter problemReporter() {
	Scope scope = outerMostMethodScope();
	if (scope==null)
		scope=compilationUnitScope();
	return scope.problemReporter();
}

/* Answer the reference type of this scope.
 *
 * It is the nearest enclosing type of this scope.
 */
public TypeDeclaration referenceType() {
	return methodScope().referenceType();
}

/*
 * Answer the index of this scope relatively to its parent.
 * For method scope, answers -1 (not a classScope relative position)
 */
public int scopeIndex() {
	if (this instanceof MethodScope ||this instanceof CompilationUnitScope) return -1;
	BlockScope parentScope = (BlockScope)this.parent;
	Scope[] parentSubscopes = parentScope.subscopes;
	for (int i = 0, max = parentScope.subscopeCount; i < max; i++) {
		if (parentSubscopes[i] == this) return i;
	}
	return -1;
}
public void setMethods(MethodBinding[] methods) {
	this.methods = methods;
}
// start position in this scope - for ordering scopes vs. variables
int startIndex() {
	return this.startIndex;
}

public String toString() {
	return toString(0);
}

public String toString(int tab) {
	StringBuilder sb = new StringBuilder(basicToString(tab));
	for (int i = 0; i < this.subscopeCount; i++){
		if (this.subscopes[i] instanceof BlockScope) {
			sb.append(((BlockScope) this.subscopes[i]).toString(tab + 1));
			sb.append("\n"); //$NON-NLS-1$
		}
	}
	return sb.toString();
}
}
