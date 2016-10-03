/*******************************************************************************
 * Copyright (c) 2000, 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.ast;

import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.ITypeDeclaration;
import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.flow.InitializationFlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.UnconditionalFlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MemberTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.NestedTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TagBits;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeIds;
import org.eclipse.wst.jsdt.internal.compiler.parser.Parser;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortMethod;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortType;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemSeverities;

public class TypeDeclaration extends Statement implements ProblemSeverities, ReferenceContext, ITypeDeclaration {
	// Type decl kinds
	public static final int CLASS_DECL = 1;

	public int modifiers = ClassFileConstants.AccDefault;
	public int modifiersSourceStart;
	public char[] name;
	public TypeReference superclass;
	public FieldDeclaration[] fields;
	public AbstractMethodDeclaration[] methods;
	public TypeDeclaration[] memberTypes;
	public SourceTypeBinding binding= new SourceTypeBinding(null,null,null);
	public ClassScope scope;
	public MethodScope initializerScope;
	public MethodScope staticInitializerScope;
	public boolean ignoreFurtherInvestigation = false;
	public int maxFieldCount;
	public int declarationSourceStart;
	public int declarationSourceEnd;
	public int bodyStart;
	public int bodyEnd; // doesn't include the trailing comment if any.
	public CompilationResult compilationResult;
	public MethodDeclaration[] missingAbstractMethods;
	public Javadoc javadoc;

	public QualifiedAllocationExpression allocation; // for anonymous only
	public TypeDeclaration enclosingType; // for member types only

public TypeDeclaration(CompilationResult compilationResult){
	this.compilationResult = compilationResult;
}

/*
 *	We cause the compilation task to abort to a given extent.
 */
public void abort(int abortLevel, CategorizedProblem problem) {
	switch (abortLevel) {
		case AbortCompilation :
			throw new AbortCompilation(this.compilationResult, problem);
		case AbortCompilationUnit :
			throw new AbortCompilationUnit(this.compilationResult, problem);
		case AbortMethod :
			throw new AbortMethod(this.compilationResult, problem);
		default :
			throw new AbortType(this.compilationResult, problem);
	}
}

/**
 * This method is responsible for adding a <clinit> method declaration to the type method collections.
 * Note that this implementation is inserting it in first place (as VAJ or javac), and that this
 * impacts the behavior of the method ConstantPool.resetForClinit(int. int), in so far as
 * the latter will have to reset the constant pool state accordingly (if it was added first, it does
 * not need to preserve some of the method specific cached entries since this will be the first method).
 * inserts the clinit method declaration in the first position.
 *
 * @see org.eclipse.wst.jsdt.internal.compiler.codegen.ConstantPool#resetForClinit(int, int)
 */
public final void addClinit() {
	//see comment on needClassInitMethod
	if (needClassInitMethod()) {
		int length;
		AbstractMethodDeclaration[] methodDeclarations;
		if ((methodDeclarations = this.methods) == null) {
			length = 0;
			methodDeclarations = new AbstractMethodDeclaration[1];
		} else {
			length = methodDeclarations.length;
			System.arraycopy(
				methodDeclarations,
				0,
				(methodDeclarations = new AbstractMethodDeclaration[length + 1]),
				1,
				length);
		}
		Clinit clinit = new Clinit(this.compilationResult);
		methodDeclarations[0] = clinit;
		// clinit is added in first location, so as to minimize the use of ldcw (big consumer of constant inits)
		clinit.declarationSourceStart = clinit.sourceStart = this.sourceStart;
		clinit.declarationSourceEnd = clinit.sourceEnd = this.sourceEnd;
		clinit.bodyEnd = this.sourceEnd;
		this.methods = methodDeclarations;
	}
}

/*
 * INTERNAL USE ONLY - Creates a fake method declaration for the corresponding binding.
 * It is used to report errors for missing abstract methods.
 */
public MethodDeclaration addMissingAbstractMethodFor(MethodBinding methodBinding) {
	TypeBinding[] argumentTypes = methodBinding.parameters;
	int argumentsLength = argumentTypes.length;
	//the constructor
	MethodDeclaration methodDeclaration = new MethodDeclaration(this.compilationResult);
	methodDeclaration.setSelector(methodBinding.selector);
	methodDeclaration.sourceStart = this.sourceStart;
	methodDeclaration.sourceEnd = this.sourceEnd;
	methodDeclaration.modifiers = methodBinding.getAccessFlags() & ~ClassFileConstants.AccAbstract;

	if (argumentsLength > 0) {
		String baseName = "arg";//$NON-NLS-1$
		Argument[] arguments = (methodDeclaration.arguments = new Argument[argumentsLength]);
		for (int i = argumentsLength; --i >= 0;) {
			arguments[i] = new Argument((baseName + i).toCharArray(), 0L, null /*type ref*/, ClassFileConstants.AccDefault);
		}
	}

	//adding the constructor in the methods list
	if (this.missingAbstractMethods == null) {
		this.missingAbstractMethods = new MethodDeclaration[] { methodDeclaration };
	} else {
		MethodDeclaration[] newMethods;
		System.arraycopy(
			this.missingAbstractMethods,
			0,
			newMethods = new MethodDeclaration[this.missingAbstractMethods.length + 1],
			1,
			this.missingAbstractMethods.length);
		newMethods[0] = methodDeclaration;
		this.missingAbstractMethods = newMethods;
	}

	//============BINDING UPDATE==========================
	if(!methodDeclaration.hasBinding()) {
		methodDeclaration.setBinding(new MethodBinding(
				methodDeclaration.modifiers, //methodDeclaration
				methodBinding.selector,
				methodBinding.returnType,
				argumentsLength == 0 ? Binding.NO_PARAMETERS : argumentTypes, //arguments bindings
				this.binding)); //declaringClass
	}

	methodDeclaration.setScope(new MethodScope(this.scope, methodDeclaration, true));

	return methodDeclaration;
}

/**
 *	Flow analysis for a local innertype
 *
 */
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	if (this.ignoreFurtherInvestigation)
		return flowInfo;
	try {
		if ((flowInfo.tagBits & FlowInfo.UNREACHABLE) == 0) {
			this.bits |= ASTNode.IsReachable;
			LocalTypeBinding localType = (LocalTypeBinding) this.binding;
			localType.setConstantPoolName(currentScope.compilationUnitScope().computeConstantPoolName(localType));
		}
		updateMaxFieldCount(); // propagate down the max field count
		internalAnalyseCode(flowContext, flowInfo);
	} catch (AbortType e) {
		this.ignoreFurtherInvestigation = true;
	}
	return flowInfo;
}

/**
 *	Flow analysis for a member innertype
 *
 */
public void analyseCode(ClassScope enclosingClassScope) {
	if (this.ignoreFurtherInvestigation)
		return;
	try {
		// propagate down the max field count
		updateMaxFieldCount();
		internalAnalyseCode(null, FlowInfo.initial(this.maxFieldCount));
	} catch (AbortType e) {
		this.ignoreFurtherInvestigation = true;
	}
}

/**
 *	Flow analysis for a local member innertype
 *
 */
public void analyseCode(ClassScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	if (this.ignoreFurtherInvestigation)
		return;
	try {
		if ((flowInfo.tagBits & FlowInfo.UNREACHABLE) == 0) {
			this.bits |= ASTNode.IsReachable;
			LocalTypeBinding localType = (LocalTypeBinding) this.binding;
			localType.setConstantPoolName(currentScope.compilationUnitScope().computeConstantPoolName(localType));
		}
		updateMaxFieldCount(); // propagate down the max field count
		internalAnalyseCode(flowContext, flowInfo);
	} catch (AbortType e) {
		this.ignoreFurtherInvestigation = true;
	}
}

/**
 *	Flow analysis for a package member type
 *
 */
public void analyseCode(CompilationUnitScope unitScope) {
	if (this.ignoreFurtherInvestigation)
		return;
	try {
		internalAnalyseCode(null, FlowInfo.initial(this.maxFieldCount));
	} catch (AbortType e) {
		this.ignoreFurtherInvestigation = true;
	}
}

/**
 * Check for constructor vs. method with no return type.
 * Answers true if at least one constructor is defined
 */
public boolean checkConstructors(Parser parser) {
	//if a constructor has not the name of the type,
	//convert it into a method with 'null' as its return type
	boolean hasConstructor = false;
	if (this.methods != null) {
		for (int i = this.methods.length; --i >= 0;) {
			AbstractMethodDeclaration am;
			if ((am = this.methods[i]).isConstructor()) {
				if (!CharOperation.equals(am.getName(), this.name)) {
					// the constructor was in fact a method with no return type
					// unless an explicit constructor call was supplied
					ConstructorDeclaration c = (ConstructorDeclaration) am;
					if (c.constructorCall == null || c.constructorCall.isImplicitSuper()) { //changed to a method
						MethodDeclaration m = parser.convertToMethodDeclaration(c, this.compilationResult);
						this.methods[i] = m;
					}
				} else {
					hasConstructor = true;
				}
			}
		}
	}
	return hasConstructor;
}

public CompilationResult compilationResult() {
	return this.compilationResult;
}

public ConstructorDeclaration createDefaultConstructor(	boolean needExplicitConstructorCall, boolean needToInsert) {
	//Add to method'set, the default constuctor that just recall the
	//super constructor with no arguments
	//The arguments' type will be positionned by the TC so just use
	//the default int instead of just null (consistency purpose)

	//the constructor
	ConstructorDeclaration constructor = new ConstructorDeclaration(this.compilationResult);
	constructor.bits |= ASTNode.IsDefaultConstructor;
	constructor.setSelector(this.name);
	constructor.modifiers = this.modifiers & ExtraCompilerModifiers.AccVisibilityMASK;

	//if you change this setting, please update the
	//SourceIndexer2.buildTypeDeclaration(TypeDeclaration,char[]) method
	constructor.declarationSourceStart = constructor.sourceStart = this.sourceStart;
	constructor.declarationSourceEnd =
		constructor.sourceEnd = constructor.bodyEnd = this.sourceEnd;

	//the super call inside the constructor
	if (needExplicitConstructorCall) {
		constructor.constructorCall = SuperReference.implicitSuperConstructorCall();
		constructor.constructorCall.sourceStart = this.sourceStart;
		constructor.constructorCall.sourceEnd = this.sourceEnd;
	}

	//adding the constructor in the methods list: rank is not critical since bindings will be sorted
	if (needToInsert) {
		if (this.methods == null) {
			this.methods = new AbstractMethodDeclaration[] { constructor };
		} else {
			AbstractMethodDeclaration[] newMethods;
			System.arraycopy(
				this.methods,
				0,
				newMethods = new AbstractMethodDeclaration[this.methods.length + 1],
				1,
				this.methods.length);
			newMethods[0] = constructor;
			this.methods = newMethods;
		}
	}
	return constructor;
}

// anonymous type constructor creation: rank is important since bindings already got sorted
public MethodBinding createDefaultConstructorWithBinding(MethodBinding inheritedConstructorBinding) {
	//Add to method'set, the default constuctor that just recall the
	//super constructor with the same arguments
	String baseName = "$anonymous"; //$NON-NLS-1$
	TypeBinding[] argumentTypes = inheritedConstructorBinding.parameters;
	int argumentsLength = argumentTypes.length;
	//the constructor
	ConstructorDeclaration constructor = new ConstructorDeclaration(this.compilationResult);
	constructor.setSelector(new char[] { 'x' }); //no maining
	constructor.sourceStart = this.sourceStart;
	constructor.sourceEnd = this.sourceEnd;
	int newModifiers = this.modifiers & ExtraCompilerModifiers.AccVisibilityMASK;
	if (inheritedConstructorBinding.isVarargs()) {
		newModifiers |= ClassFileConstants.AccVarargs;
	}
	constructor.modifiers = newModifiers;
	constructor.bits |= ASTNode.IsDefaultConstructor;

	if (argumentsLength > 0) {
		Argument[] arguments = (constructor.arguments = new Argument[argumentsLength]);
		for (int i = argumentsLength; --i >= 0;) {
			arguments[i] = new Argument((baseName + i).toCharArray(), 0L, null /*type ref*/, ClassFileConstants.AccDefault);
		}
	}
	//the super call inside the constructor
	constructor.constructorCall = SuperReference.implicitSuperConstructorCall();
	constructor.constructorCall.sourceStart = this.sourceStart;
	constructor.constructorCall.sourceEnd = this.sourceEnd;

	if (argumentsLength > 0) {
		Expression[] args;
		args = constructor.constructorCall.arguments = new Expression[argumentsLength];
		for (int i = argumentsLength; --i >= 0;) {
			args[i] = new SingleNameReference((baseName + i).toCharArray(), 0L);
		}
	}

	//adding the constructor in the methods list
	if (this.methods == null) {
		this.methods = new AbstractMethodDeclaration[] { constructor };
	} else {
		AbstractMethodDeclaration[] newMethods;
		System.arraycopy(this.methods, 0, newMethods = new AbstractMethodDeclaration[this.methods.length + 1], 1, this.methods.length);
		newMethods[0] = constructor;
		this.methods = newMethods;
	}

	//============BINDING UPDATE==========================
	SourceTypeBinding sourceType = this.binding;
	if(!constructor.hasBinding()) {
		constructor.setBinding(new MethodBinding(
				constructor.modifiers, //methodDeclaration
				argumentsLength == 0 ? Binding.NO_PARAMETERS : argumentTypes, //arguments bindings
						sourceType)); //declaringClass
	}

	constructor.getBinding().modifiers |= ExtraCompilerModifiers.AccIsDefaultConstructor;

	constructor.setScope(new MethodScope(this.scope, constructor, true));
	constructor.constructorCall.resolve(constructor.getScope());

	MethodBinding[] methodBindings = sourceType.methods(); // trigger sorting
	int length;
	System.arraycopy(methodBindings, 0, methodBindings = new MethodBinding[(length = methodBindings.length) + 1], 1, length);
	methodBindings[0] = constructor.getBinding();
	if (++length > 1)
		ReferenceBinding.sortMethods(methodBindings, 0, length);	// need to resort, since could be valid methods ahead (140643) - DOM needs eager sorting
	sourceType.setMethods(methodBindings);
	//===================================================

	return constructor.getBinding();
}

/**
 * Find the matching parse node, answers null if nothing found
 */
public FieldDeclaration declarationOf(FieldBinding fieldBinding) {
	if (fieldBinding != null && this.fields != null) {
		for (int i = 0, max = this.fields.length; i < max; i++) {
			FieldDeclaration fieldDecl;
			if ((fieldDecl = this.fields[i]).binding == fieldBinding)
				return fieldDecl;
		}
	}
	return null;
}

/**
 * Find the matching parse node, answers null if nothing found
 */
public TypeDeclaration declarationOf(MemberTypeBinding memberTypeBinding) {
	if (memberTypeBinding != null && this.memberTypes != null) {
		for (int i = 0, max = this.memberTypes.length; i < max; i++) {
			TypeDeclaration memberTypeDecl;
			if ((memberTypeDecl = this.memberTypes[i]).binding == memberTypeBinding)
				return memberTypeDecl;
		}
	}
	return null;
}

/**
 * Find the matching parse node, answers null if nothing found
 */
public AbstractMethodDeclaration declarationOf(MethodBinding methodBinding) {
	if (methodBinding != null && this.methods != null) {
		for (int i = 0, max = this.methods.length; i < max; i++) {
			AbstractMethodDeclaration methodDecl;

			if ((methodDecl = this.methods[i]).getBinding() == methodBinding)
				return methodDecl;
		}
	}
	return null;
}

/**
 * Finds the matching type amoung this type's member types.
 * Returns null if no type with this name is found.
 * The type name is a compound name relative to this type
 * eg. if this type is X and we're looking for Y.X.A.B
 *     then a type name would be {X, A, B}
 */
public TypeDeclaration declarationOfType(char[][] typeName) {
	int typeNameLength = typeName.length;
	if (typeNameLength < 1 || !CharOperation.equals(typeName[0], this.name)) {
		return null;
	}
	if (typeNameLength == 1) {
		return this;
	}
	char[][] subTypeName = new char[typeNameLength - 1][];
	System.arraycopy(typeName, 1, subTypeName, 0, typeNameLength - 1);
	for (int i = 0; i < this.memberTypes.length; i++) {
		TypeDeclaration typeDecl = this.memberTypes[i].declarationOfType(subTypeName);
		if (typeDecl != null) {
			return typeDecl;
		}
	}
	return null;
}

public boolean hasErrors() {
	return this.ignoreFurtherInvestigation;
}

/**
 *	Common flow analysis for all types
 */
private void internalAnalyseCode(FlowContext flowContext, FlowInfo flowInfo) {
	if ((this.binding.isPrivate() || (this.binding.tagBits & (TagBits.IsAnonymousType|TagBits.IsLocalType)) == TagBits.IsLocalType) && !this.binding.isUsed()) {
		if (!this.scope.referenceCompilationUnit().compilationResult.hasSyntaxError) {
			this.scope.problemReporter().unusedPrivateType(this);
		}
	}
	InitializationFlowContext initializerContext = new InitializationFlowContext(null, this, this.initializerScope);
	InitializationFlowContext staticInitializerContext = new InitializationFlowContext(null, this, this.staticInitializerScope);
	FlowInfo nonStaticFieldInfo = flowInfo.unconditionalFieldLessCopy();
	FlowInfo staticFieldInfo = flowInfo.unconditionalFieldLessCopy();
	if (this.fields != null) {
		for (int i = 0, count = this.fields.length; i < count; i++) {
			FieldDeclaration field = this.fields[i];
			if (field.isStatic()) {
				if ((staticFieldInfo.tagBits & FlowInfo.UNREACHABLE) != 0)
					field.bits &= ~ASTNode.IsReachable;

				/*if (field.isField()){
					staticInitializerContext.handledExceptions = NoExceptions; // no exception is allowed jls8.3.2
				} else {*/
				staticInitializerContext.handledExceptions = Binding.ANY_EXCEPTION; // tolerate them all, and record them
				/*}*/
				staticFieldInfo =
					field.analyseCode(
						this.staticInitializerScope,
						staticInitializerContext,
						staticFieldInfo);
				// in case the initializer is not reachable, use a reinitialized flowInfo and enter a fake reachable
				// branch, since the previous initializer already got the blame.
				if (staticFieldInfo == FlowInfo.DEAD_END) {
					staticFieldInfo = FlowInfo.initial(this.maxFieldCount).setReachMode(FlowInfo.UNREACHABLE);
				}
			} else {
				if ((nonStaticFieldInfo.tagBits & FlowInfo.UNREACHABLE) != 0)
					field.bits &= ~ASTNode.IsReachable;

				/*if (field.isField()){
					initializerContext.handledExceptions = NoExceptions; // no exception is allowed jls8.3.2
				} else {*/
					initializerContext.handledExceptions = Binding.ANY_EXCEPTION; // tolerate them all, and record them
				/*}*/
				nonStaticFieldInfo =
					field.analyseCode(this.initializerScope, initializerContext, nonStaticFieldInfo);
				// in case the initializer is not reachable, use a reinitialized flowInfo and enter a fake reachable
				// branch, since the previous initializer already got the blame.
				if (nonStaticFieldInfo == FlowInfo.DEAD_END) {
					nonStaticFieldInfo = FlowInfo.initial(this.maxFieldCount).setReachMode(FlowInfo.UNREACHABLE);
				}
			}
		}
	}
	if (this.memberTypes != null) {
		for (int i = 0, count = this.memberTypes.length; i < count; i++) {
			if (flowContext != null){ // local type
				this.memberTypes[i].analyseCode(this.scope, flowContext, nonStaticFieldInfo.copy().setReachMode(flowInfo.reachMode())); // reset reach mode in case initializers did abrupt completely
			} else {
				this.memberTypes[i].analyseCode(this.scope);
			}
		}
	}
	if (this.methods != null) {
		UnconditionalFlowInfo outerInfo = flowInfo.unconditionalFieldLessCopy();
		FlowInfo constructorInfo = nonStaticFieldInfo.unconditionalInits().discardNonFieldInitializations().addInitializationsFrom(outerInfo);
		for (int i = 0, count = this.methods.length; i < count; i++) {
			AbstractMethodDeclaration method = this.methods[i];
			if (method.ignoreFurtherInvestigation)
				continue;
			if (method.isInitializationMethod()) {
				if (method.isStatic()) { // <clinit>
					method.analyseCode(
						this.scope,
						staticInitializerContext,
						staticFieldInfo.unconditionalInits().discardNonFieldInitializations().addInitializationsFrom(outerInfo));
				} else { // constructor
					((ConstructorDeclaration)method).analyseCode(this.scope, initializerContext, constructorInfo.copy(), flowInfo.reachMode());
				}
			} else { // regular method
				method.analyseCode(this.scope, null, flowInfo.copy());
			}
		}
	}
}

public final static int kind(int flags) {
	return TypeDeclaration.CLASS_DECL;
}

/**
 * A <clinit> will be requested as soon as static fields or assertions are present. It will be eliminated during
 * classfile creation if no bytecode was actually produced based on some optimizations/compiler settings.
 */
public final boolean needClassInitMethod() {
	// always need a <clinit> when assertions are present
	if ((this.bits & ASTNode.ContainsAssertion) != 0)
		return true;

	if (this.fields != null) {
		for (int i = this.fields.length; --i >= 0;) {
			FieldDeclaration field = this.fields[i];
			//need to test the modifier directly while there is no binding yet
			if ((field.modifiers & ClassFileConstants.AccStatic) != 0)
				return true; // TODO (philippe) shouldn't it check whether field is initializer or has some initial value ?
		}
	}
	return false;
}

public void parseMethod(Parser parser, CompilationUnitDeclaration unit) {
	//connect method bodies
	if (unit.ignoreMethodBodies)
		return;

	//members
	if (this.memberTypes != null) {
		int length = this.memberTypes.length;
		for (int i = 0; i < length; i++)
			this.memberTypes[i].parseMethod(parser, unit);
	}

	//methods
	if (this.methods != null) {
		int length = this.methods.length;
		for (int i = 0; i < length; i++) {
			this.methods[i].parseStatements(parser, unit);
		}
	}

	//initializers
	if (this.fields != null) {
		int length = this.fields.length;
		for (int i = 0; i < length; i++) {
			final FieldDeclaration fieldDeclaration = this.fields[i];
			switch(fieldDeclaration.getKind()) {
				case AbstractVariableDeclaration.INITIALIZER:
					((Initializer) fieldDeclaration).parseStatements(parser, this, unit);
					break;
			}
		}
	}
}

public StringBuffer print(int indent, StringBuffer output) {
	if (this.javadoc != null) {
		this.javadoc.print(indent, output);
	}
	if ((this.bits & ASTNode.IsAnonymousType) == 0) {
		printIndent(indent, output);
		printHeader(0, output);
	}
	return printBody(indent, output);
}

public StringBuffer printBody(int indent, StringBuffer output) {
	output.append(" {"); //$NON-NLS-1$
	if (this.memberTypes != null) {
		for (int i = 0; i < this.memberTypes.length; i++) {
			if (this.memberTypes[i] != null) {
				output.append('\n');
				this.memberTypes[i].print(indent + 1, output);
			}
		}
	}
	if (this.fields != null) {
		for (int fieldI = 0; fieldI < this.fields.length; fieldI++) {
			if (this.fields[fieldI] != null) {
				output.append('\n');
				this.fields[fieldI].print(indent + 1, output);
			}
		}
	}
	if (this.methods != null) {
		for (int i = 0; i < this.methods.length; i++) {
			if (this.methods[i] != null) {
				output.append('\n');
				this.methods[i].print(indent + 1, output);
			}
		}
	}
	output.append('\n');
	return printIndent(indent, output).append('}');
}

public StringBuffer printHeader(int indent, StringBuffer output) {
	printModifiers(this.modifiers, output);

	switch (kind(this.modifiers)) {
		case TypeDeclaration.CLASS_DECL :
			output.append("class "); //$NON-NLS-1$
			break;
	}
	output.append(this.name);
	
	if (this.superclass != null) {
		output.append(" extends ");  //$NON-NLS-1$
		this.superclass.print(0, output);
	}
	return output;
}

public StringBuffer printStatement(int tab, StringBuffer output) {
	return print(tab, output);
}



public void resolve() {
	SourceTypeBinding sourceType = this.binding;
	if (sourceType == null) {
		this.ignoreFurtherInvestigation = true;
		return;
	}
	try {
		if ((this.bits & ASTNode.UndocumentedEmptyBlock) != 0) {
			this.scope.problemReporter().undocumentedEmptyBlock(this.bodyStart-1, this.bodyEnd);
		}

		// generics (and non static generic members) cannot extend Throwable
		if (sourceType.findSuperTypeErasingTo(TypeIds.T_JavaLangThrowable, true) != null) {
			ReferenceBinding current = sourceType;
			checkEnclosedInGeneric : do {
				if (current.isStatic()) break checkEnclosedInGeneric;
				if (current.isLocalType()) {
					NestedTypeBinding nestedType = (NestedTypeBinding) current;
					if (nestedType.scope.methodScope().isStatic) break checkEnclosedInGeneric;
				}
			} while ((current = current.enclosingType()) != null);
		}
		this.maxFieldCount = 0;
		int lastVisibleFieldID = -1;

		if (this.memberTypes != null) {
			for (int i = 0, count = this.memberTypes.length; i < count; i++) {
				this.memberTypes[i].resolve(this.scope);
			}
		}
		if (this.fields != null) {
			for (int i = 0, count = this.fields.length; i < count; i++) {
				FieldDeclaration field = this.fields[i];
				switch(field.getKind()) {
					case AbstractVariableDeclaration.FIELD:
						FieldBinding fieldBinding = field.binding;
						if (fieldBinding == null) {
							// still discover secondary errors
							if (field.initialization != null) field.initialization.resolve(field.isStatic() ? this.staticInitializerScope : this.initializerScope);
							this.ignoreFurtherInvestigation = true;
							continue;
						}
						this.maxFieldCount++;
						lastVisibleFieldID = field.binding.id;
						break;

					case AbstractVariableDeclaration.INITIALIZER:
						 ((Initializer) field).lastVisibleFieldID = lastVisibleFieldID + 1;
						break;
				}
				field.resolve(field.isStatic() ? this.staticInitializerScope : this.initializerScope);
			}
		}

		if (this.methods != null) {
			for (int i = 0, count = this.methods.length; i < count; i++) {
				this.methods[i].resolve(this.scope);
			}
		}
		// Resolve javadoc
		if (this.javadoc != null) {
			if (this.scope != null && (this.name != TypeConstants.PACKAGE_INFO_NAME)) {
				// if the type is package-info, the javadoc was resolved as part of the compilation unit javadoc
				this.javadoc.resolve(this.scope);
			}
		} else if (this.scope != null && sourceType != null && !sourceType.isLocalType()) {
			this.scope.problemReporter().javadocMissing(this.sourceStart, this.sourceEnd, sourceType.modifiers);
		}

	} catch (AbortType e) {
		this.ignoreFurtherInvestigation = true;
		return;
	}
}

/**
 * Resolve a local type declaration
 */
public void resolve(BlockScope blockScope) {

	// need to build its scope first and proceed with binding's creation
	if ((this.bits & ASTNode.IsAnonymousType) == 0) {
		// check collision scenarii
		blockScope.addLocalType(this);
	}

	if (this.binding != null) {
		// remember local types binding for innerclass emulation propagation
		blockScope.referenceCompilationUnit().record((LocalTypeBinding)this.binding);

		// binding is not set if the receiver could not be created
		resolve();
		updateMaxFieldCount();
	}
}

/**
 * Resolve a member type declaration (can be a local member)
 */
public void resolve(ClassScope upperScope) {
	// member scopes are already created
	// request the construction of a binding if local member type

	if (this.binding != null && this.binding instanceof LocalTypeBinding) {
		// remember local types binding for innerclass emulation propagation
		upperScope.referenceCompilationUnit().record((LocalTypeBinding)this.binding);
	}
	resolve();
	updateMaxFieldCount();
}

/**
 * Resolve a top level type declaration
 */
public void resolve(CompilationUnitScope upperScope) {
	// top level : scope are already created
	resolve();
	updateMaxFieldCount();
}

public void tagAsHavingErrors() {
	this.ignoreFurtherInvestigation = true;
}

/**
 *	Iteration for a package member type
 *
 */
public void traverse(ASTVisitor visitor, CompilationUnitScope unitScope) {

	if (this.ignoreFurtherInvestigation)
		return;
	try {
		if (visitor.visit(this, unitScope)) {
			if (this.javadoc != null) {
				this.javadoc.traverse(visitor, this.scope);
			}
			if (this.superclass != null)
				this.superclass.traverse(visitor, this.scope);
			if (this.memberTypes != null) {
				int length = this.memberTypes.length;
				for (int i = 0; i < length; i++)
					this.memberTypes[i].traverse(visitor, this.scope);
			}
			if (this.fields != null) {
				int length = this.fields.length;
				for (int i = 0; i < length; i++) {
					FieldDeclaration field;
					if ((field = this.fields[i]).isStatic()) {
						field.traverse(visitor, this.staticInitializerScope);
					} else {
						field.traverse(visitor, this.initializerScope);
					}
				}
			}
			if (this.methods != null) {
				int length = this.methods.length;
				for (int i = 0; i < length; i++)
					this.methods[i].traverse(visitor, this.scope);
			}
		}
		visitor.endVisit(this, unitScope);
	} catch (AbortType e) {
		// silent abort
	}
}

/**
 *	Iteration for a local innertype
 */
public void traverse(ASTVisitor visitor, BlockScope blockScope) {
	if (this.ignoreFurtherInvestigation)
		return;
	try {
		if (visitor.visit(this, blockScope)) {
			if (this.javadoc != null) {
				this.javadoc.traverse(visitor, this.scope);
			}
			if (this.superclass != null)
				this.superclass.traverse(visitor, this.scope);
			if (this.memberTypes != null) {
				int length = this.memberTypes.length;
				for (int i = 0; i < length; i++)
					this.memberTypes[i].traverse(visitor, this.scope);
			}
			if (this.fields != null) {
				int length = this.fields.length;
				for (int i = 0; i < length; i++) {
					FieldDeclaration field;
					if ((field = this.fields[i]).isStatic()) {
						// local type cannot have static fields
					} else {
						field.traverse(visitor, this.initializerScope);
					}
				}
			}
			if (this.methods != null) {
				int length = this.methods.length;
				for (int i = 0; i < length; i++)
					this.methods[i].traverse(visitor, this.scope);
			}
		}
		visitor.endVisit(this, blockScope);
	} catch (AbortType e) {
		// silent abort
	}
}

/**
 *	Iteration for a member innertype
 *
 */
public void traverse(ASTVisitor visitor, ClassScope classScope) {
	if (this.ignoreFurtherInvestigation)
		return;
	try {
		if (visitor.visit(this, classScope)) {
			if (this.javadoc != null) {
				this.javadoc.traverse(visitor, scope);
			}
			if (this.superclass != null)
				this.superclass.traverse(visitor, this.scope);
			if (this.memberTypes != null) {
				int length = this.memberTypes.length;
				for (int i = 0; i < length; i++)
					this.memberTypes[i].traverse(visitor, this.scope);
			}
			if (this.fields != null) {
				int length = this.fields.length;
				for (int i = 0; i < length; i++) {
					FieldDeclaration field;
					if ((field = this.fields[i]).isStatic()) {
						field.traverse(visitor, this.staticInitializerScope);
					} else {
						field.traverse(visitor, this.initializerScope);
					}
				}
			}
			if (this.methods != null) {
				int length = this.methods.length;
				for (int i = 0; i < length; i++)
					this.methods[i].traverse(visitor, this.scope);
			}
		}
		visitor.endVisit(this, classScope);
	} catch (AbortType e) {
		// silent abort
	}
}

/**
 * MaxFieldCount's computation is necessary so as to reserve space for
 * the flow info field portions. It corresponds to the maximum amount of
 * fields this class or one of its innertypes have.
 *
 * During name resolution, types are traversed, and the max field count is recorded
 * on the outermost type. It is then propagated down during the flow analysis.
 *
 * This method is doing either up/down propagation.
 */
void updateMaxFieldCount() {
	if (this.scope == null || this.binding == null)
		return; // error scenario
	TypeDeclaration outerMostType = this.scope.outerMostClassScope().referenceType();
	if (this.maxFieldCount > outerMostType.maxFieldCount) {
		outerMostType.maxFieldCount = this.maxFieldCount; // up
	} else {
		this.maxFieldCount = outerMostType.maxFieldCount; // down
	}
}

/**
 * Returns whether the type is a secondary one or not.
 */
public boolean isSecondary() {
	return (this.bits & ASTNode.IsSecondaryType) != 0;
}
public int getASTType() {
	return IASTNode.TYPE_DECLARATION;

}
}
