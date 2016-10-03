/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.lookup;

import org.eclipse.wst.jsdt.core.ast.IAbstractVariableDeclaration;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.InferredMethod;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Argument;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ProgramElement;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Statement;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.flow.UnconditionalFlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObject;

/**
 * Particular block scope used for methods, constructors or clinits, representing
 * its outermost blockscope. Note also that such a scope will be provided to enclose
 * field initializers subscopes as well.
 */
public class MethodScope extends BlockScope {

	public ReferenceContext referenceContext;
	public boolean isStatic; // method modifier or initializer one

	//fields used during name resolution
	public boolean isConstructorCall = false;
	public FieldBinding initializedField; // the field being initialized
	public int lastVisibleFieldID = -1; // the ID of the last field which got declared
	// note that #initializedField can be null AND lastVisibleFieldID >= 0, when processing instance field initializers.

	// flow analysis
	public int analysisIndex; // for setting flow-analysis id

	// for local variables table attributes
	public int lastIndex = 0;
	public long[] definiteInits = new long[4];
	public long[][] extraDefiniteInits = new long[4][];

	public static final char [] ARGUMENTS_NAME={'a','r','g','u','m','e','n','t','s'};

	public LocalVariableBinding argumentsBinding;

	/**
	 * <p>
	 * Map of variable names to statements that have not been resolved that
	 * define those variables.
	 * </p>
	 */
	private HashtableOfObject fUnresolvedLocalVars;

	/**
	 * <p>
	 * Map of function names to statements that have not been resolved that
	 * define those variables.
	 * </p>
	 */
	private HashtableOfObject fUnresolvedLocalFuncs;

	public MethodScope(Scope parent, ReferenceContext context, boolean isStatic) {

		super(METHOD_SCOPE, parent);
		locals = new LocalVariableBinding[5];
		this.referenceContext = context;
		this.isStatic = isStatic;
		this.startIndex = 0;
		argumentsBinding = new LocalVariableBinding(ARGUMENTS_NAME,TypeBinding.UNKNOWN,0,true);
		argumentsBinding.declaringScope=this;
	}

	/* Spec : 8.4.3 & 9.4
	 */
	private void checkAndSetModifiersForConstructor(MethodBinding methodBinding) {

		int modifiers = methodBinding.modifiers;
		final ReferenceBinding declaringClass = methodBinding.declaringClass;
	
//		if (((ConstructorDeclaration) referenceContext).isDefaultConstructor) {
		if ((methodBinding.modifiers&ExtraCompilerModifiers.AccIsDefaultConstructor)>0) {
			// certain flags are propagated from declaring class onto constructor
			final int DECLARING_FLAGS = ClassFileConstants.AccPublic|ClassFileConstants.AccProtected;
			final int VISIBILITY_FLAGS = ClassFileConstants.AccPrivate|ClassFileConstants.AccPublic|ClassFileConstants.AccProtected;
			int flags;
			if ((flags = declaringClass.modifiers & DECLARING_FLAGS) != 0) {
				modifiers &= ~VISIBILITY_FLAGS;
				modifiers |= flags; // propagate public/protected
			}
		}

		// after this point, tests on the 16 bits reserved.
		int realModifiers = modifiers & ExtraCompilerModifiers.AccJustFlag;

		// check for incompatible modifiers in the visibility bits, isolate the visibility bits
		int accessorBits = realModifiers & (ClassFileConstants.AccPublic | ClassFileConstants.AccProtected | ClassFileConstants.AccPrivate);
		if ((accessorBits & (accessorBits - 1)) != 0) {
		
			// need to keep the less restrictive so disable Protected/Private as necessary
			if ((accessorBits & ClassFileConstants.AccPublic) != 0) {
				if ((accessorBits & ClassFileConstants.AccProtected) != 0)
					modifiers &= ~ClassFileConstants.AccProtected;
				if ((accessorBits & ClassFileConstants.AccPrivate) != 0)
					modifiers &= ~ClassFileConstants.AccPrivate;
			} else if ((accessorBits & ClassFileConstants.AccProtected) != 0 && (accessorBits & ClassFileConstants.AccPrivate) != 0) {
				modifiers &= ~ClassFileConstants.AccPrivate;
			}
		}

//		// if the receiver's declaring class is a private nested type, then make sure the receiver is not private (causes problems for inner type emulation)
//		if (declaringClass.isPrivate() && (modifiers & ClassFileConstants.AccPrivate) != 0)
//			modifiers &= ~ClassFileConstants.AccPrivate;

		methodBinding.modifiers = modifiers;
	}

	/* Spec : 8.4.3 & 9.4
	 */
	private void checkAndSetModifiersForMethod(MethodBinding methodBinding) {

		int modifiers = methodBinding.modifiers;
		final ReferenceBinding declaringClass = methodBinding.declaringClass;
	
		// after this point, tests on the 16 bits reserved.
		int realModifiers = modifiers & ExtraCompilerModifiers.AccJustFlag;

		// set the requested modifiers for a method in an interface/annotation
//		if (declaringClass.isInterface()) {
//			if ((realModifiers & ~(ClassFileConstants.AccPublic | ClassFileConstants.AccAbstract)) != 0) {
//				if ((declaringClass.modifiers & ClassFileConstants.AccAnnotation) != 0)
//					problemReporter().illegalModifierForAnnotationMember((AbstractMethodDeclaration) referenceContext);
//				else
//					problemReporter().illegalModifierForInterfaceMethod((AbstractMethodDeclaration) referenceContext);
//			}
//			return;
//		}

		// check for incompatible modifiers in the visibility bits, isolate the visibility bits
		int accessorBits = realModifiers & (ClassFileConstants.AccPublic | ClassFileConstants.AccProtected | ClassFileConstants.AccPrivate);
		if ((accessorBits & (accessorBits - 1)) != 0) {
			
			// need to keep the less restrictive so disable Protected/Private as necessary
			if ((accessorBits & ClassFileConstants.AccPublic) != 0) {
				if ((accessorBits & ClassFileConstants.AccProtected) != 0)
					modifiers &= ~ClassFileConstants.AccProtected;
				if ((accessorBits & ClassFileConstants.AccPrivate) != 0)
					modifiers &= ~ClassFileConstants.AccPrivate;
			} else if ((accessorBits & ClassFileConstants.AccProtected) != 0 && (accessorBits & ClassFileConstants.AccPrivate) != 0) {
				modifiers &= ~ClassFileConstants.AccPrivate;
			}
		}

		/* DISABLED for backward compatibility with javac (if enabled should also mark private methods as final)
		// methods from a final class are final : 8.4.3.3
		if (methodBinding.declaringClass.isFinal())
			modifiers |= AccFinal;
		*/
//		// static members are only authorized in a static member or top level type
//		if (((realModifiers & ClassFileConstants.AccStatic) != 0) && declaringClass.isNestedType() && !declaringClass.isStatic())
//			problemReporter().unexpectedStaticModifierForMethod(declaringClass, (AbstractMethodDeclaration) referenceContext);

		methodBinding.modifiers = modifiers;
	}

	MethodBinding createMethod(InferredMethod inferredMethod,SourceTypeBinding declaringClass) {
        boolean isConstructor=inferredMethod.isConstructor;
        if (isConstructor && declaringClass!=inferredMethod.inType.binding)
        	isConstructor=false;
		 MethodBinding binding = createMethod((AbstractMethodDeclaration) inferredMethod.getFunctionDeclaration(),inferredMethod.name,declaringClass, isConstructor,false); 
		 if (inferredMethod.isConstructor || declaringClass!=inferredMethod.inType.binding)
			 binding.allocationType=inferredMethod.inType.binding;
		 return binding;
	}

	public MethodBinding createMethod(AbstractMethodDeclaration method,char[] name,SourceTypeBinding declaringClass, boolean isConstructor, boolean isLocal) {

		MethodBinding methodBinding=null;
		// is necessary to ensure error reporting
		this.referenceContext = method;
		method.setScope(this);
		int modifiers = method.modifiers | ExtraCompilerModifiers.AccUnresolved;
		if ((method.modifiers &(ClassFileConstants.AccPrivate | ClassFileConstants.AccProtected))==0)
			modifiers|=ClassFileConstants.AccPublic;
		if (method.inferredMethod!=null &&  method.inferredMethod.isStatic)
			modifiers|= ClassFileConstants.AccStatic;
		if (isConstructor) {
			if (method.isDefaultConstructor() || isConstructor) {
				modifiers |= ExtraCompilerModifiers.AccIsDefaultConstructor;
			}
			TypeBinding constructorType = null;
			if (method.inferredMethod!=null && method.inferredMethod.inType != null) {
				constructorType=method.inferredMethod.inType.resolveType(this,method);
			}
			//return type still null, return type is unknown
			if (constructorType==null) {
				constructorType=TypeBinding.UNKNOWN;
			}
			methodBinding = new MethodBinding(modifiers, name, constructorType, null, constructorType instanceof ReferenceBinding ? (ReferenceBinding) constructorType : declaringClass);
			methodBinding.tagBits|=TagBits.IsConstructor;
			checkAndSetModifiersForConstructor(methodBinding);
		} else {
			TypeBinding returnType =
				 (method.inferredType!=null)?method.inferredType.resolveType(this,method):TypeBinding.UNKNOWN;
			if (method.inferredType==null && method.inferredMethod!=null && method.inferredMethod.isConstructor
					&& method.inferredMethod.inType!=null) {
				returnType=method.inferredMethod.inType.resolveType(this,method);
			}
			
			//return type still null, return type is unknown
			if (returnType==null) {
				returnType=TypeBinding.UNKNOWN;
			}
			
			if (isLocal && method.getName()!=null) {
				methodBinding =
					new LocalFunctionBinding(modifiers, name,returnType, null, declaringClass);
			} else{// not local method
				methodBinding =
					new MethodBinding(modifiers, name,returnType, null, declaringClass);
			}
			
			if (method.inferredMethod!=null) {
				methodBinding.tagBits |= TagBits.IsInferredType;
				if ((method.bits&ASTNode.IsInferredJsDocType)!=0) {
					methodBinding.tagBits |= TagBits.IsInferredJsDocType;
			
				}
			}
			//methodBinding.createFunctionTypeBinding(this);
//			if (method.inferredMethod!=null && method.inferredMethod.isConstructor) {
//				methodBinding.tagBits|=TagBits.IsConstructor;
//			}
			checkAndSetModifiersForMethod(methodBinding);
		}
		methodBinding.createFunctionTypeBinding(this);
		this.isStatic =methodBinding.isStatic();

		//set arguments
		Argument[] argTypes = method.arguments;
		int argLength = argTypes == null ? 0 : argTypes.length;
		if (argLength > 0 && compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5) {
			if (argTypes[--argLength].isVarArgs())
				methodBinding.modifiers |= ClassFileConstants.AccVarargs;
		}
	
		return methodBinding;
	}

	public FieldBinding findField(
		TypeBinding receiverType,
		char[] fieldName,
		InvocationSite invocationSite,
		boolean needResolve) {

		FieldBinding field = super.findField(receiverType, fieldName, invocationSite, needResolve);
		if (field == null)
			return null;
		if (!field.isValidBinding())
			return field; // answer the error field
		if (field.isStatic())
			return field; // static fields are always accessible

		if (!isConstructorCall || receiverType != enclosingSourceType())
			return field;

		if (invocationSite instanceof SingleNameReference)
			return new ProblemFieldBinding(
				field, // closest match
				field.declaringClass,
				fieldName,
				ProblemReasons.NonStaticReferenceInConstructorInvocation);
		if (invocationSite instanceof QualifiedNameReference) {
			// look to see if the field is the first binding
			QualifiedNameReference name = (QualifiedNameReference) invocationSite;
			if (name.binding == null)
				// only true when the field is the fieldbinding at the beginning of name's tokens
				return new ProblemFieldBinding(
					field, // closest match
					field.declaringClass,
					fieldName,
					ProblemReasons.NonStaticReferenceInConstructorInvocation);
		}
		return field;
	}

	public boolean isInsideConstructor() {

		return (referenceContext instanceof ConstructorDeclaration);
	}

	public boolean isInsideInitializer() {

		return (referenceContext instanceof TypeDeclaration);
	}

	public boolean isInsideInitializerOrConstructor() {

		return (referenceContext instanceof TypeDeclaration)
			|| (referenceContext instanceof ConstructorDeclaration);
	}

	/* Answer the problem reporter to use for raising new problems.
	 *
	 * Note that as a side-effect, this updates the current reference context
	 * (unit, type or method) in case the problem handler decides it is necessary
	 * to abort.
	 */
	public ProblemReporter problemReporter() {

		MethodScope outerMethodScope;
		if ((outerMethodScope = outerMostMethodScope()) == this) {
			ProblemReporter problemReporter = referenceCompilationUnit().problemReporter;
			problemReporter.referenceContext = referenceContext;
			return problemReporter;
		}
		return outerMethodScope.problemReporter();
	}

	public final int recordInitializationStates(FlowInfo flowInfo) {

		if ((flowInfo.tagBits & FlowInfo.UNREACHABLE) != 0) return -1;

		UnconditionalFlowInfo unconditionalFlowInfo = flowInfo.unconditionalInitsWithoutSideEffect();
		long[] extraInits = unconditionalFlowInfo.extra == null ?
				null : unconditionalFlowInfo.extra[0];
		long inits = unconditionalFlowInfo.definiteInits;
		checkNextEntry : for (int i = lastIndex; --i >= 0;) {
			if (definiteInits[i] == inits) {
				long[] otherInits = extraDefiniteInits[i];
				if ((extraInits != null) && (otherInits != null)) {
					if (extraInits.length == otherInits.length) {
						int j, max;
						for (j = 0, max = extraInits.length; j < max; j++) {
							if (extraInits[j] != otherInits[j]) {
								continue checkNextEntry;
							}
						}
						return i;
					}
				} else {
					if ((extraInits == null) && (otherInits == null)) {
						return i;
					}
				}
			}
		}

		// add a new entry
		if (definiteInits.length == lastIndex) {
			// need a resize
			System.arraycopy(
				definiteInits,
				0,
				(definiteInits = new long[lastIndex + 20]),
				0,
				lastIndex);
			System.arraycopy(
				extraDefiniteInits,
				0,
				(extraDefiniteInits = new long[lastIndex + 20][]),
				0,
				lastIndex);
		}
		definiteInits[lastIndex] = inits;
		if (extraInits != null) {
			extraDefiniteInits[lastIndex] = new long[extraInits.length];
			System.arraycopy(
				extraInits,
				0,
				extraDefiniteInits[lastIndex],
				0,
				extraInits.length);
		}
		return lastIndex++;
	}

	/* Answer the reference method of this scope, or null if initialization scoope.
	*/
	public AbstractMethodDeclaration referenceMethod() {

		if (referenceContext instanceof AbstractMethodDeclaration) return (AbstractMethodDeclaration) referenceContext;
		return null;
	}

	/* Answer the reference type of this scope.
	*
	* It is the nearest enclosing type of this scope.
	*/
	public TypeDeclaration referenceType() {
		if (parent instanceof ClassScope)
		  return ((ClassScope) parent).referenceContext;
		return null;
	}

	String basicToString(int tab) {

		StringBuilder sb = new StringBuilder('\n');
		for (int i = tab; --i >= 0;) {
			sb.append('\t');
		}

		sb.append("--- Method Scope ---"); //$NON-NLS-1$
		sb.append('\t');
		sb.append("locals:"); //$NON-NLS-1$
		for (int i = 0; i < localIndex; i++) {
			sb.append('\t');
			sb.append(locals[i]);
		}
		sb.append("startIndex = "); //$NON-NLS-1$ 
		sb.append(startIndex);
		sb.append("isConstructorCall = "); //$NON-NLS-1$
		sb.append(isConstructorCall);
		sb.append("initializedField = "); //$NON-NLS-1$
		sb.append(initializedField);
		sb.append("lastVisibleFieldID = "); //$NON-NLS-1$
		sb.append(lastVisibleFieldID);
		sb.append("referenceContext = "); //$NON-NLS-1$
		sb.append(referenceContext);
		return sb.toString();
	}

	/**
	 * <p>
	 * This implementation first calls super, if that returns no binding or a
	 * problem binding then the list of unresolved local variables is checked,
	 * and if there is a statement that defines a variable with the correct
	 * name it is resolved, and then the super implementation is called again.
	 * </p>
	 * 
	 * <p>
	 * This allows {@link #addUnresolvedLocalVar(char[], Statement)} to be used in
	 * conjunction with this method to prevent having to resolve the entire
	 * {@link AbstractMethodDeclaration} associated with this scope.
	 * </p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope#findVariable(char[])
	 */
	public LocalVariableBinding findVariable(char[] variableName) {
		LocalVariableBinding binding = super.findVariable(variableName);
		if (binding == null && CharOperation.equals(variableName,ARGUMENTS_NAME)) {
			binding = this.argumentsBinding;
		}
		
		// if super could not find a good binding then check list of unresolved local variables
		if(binding == null && this.fUnresolvedLocalVars != null) {
			IAbstractVariableDeclaration statement = (IAbstractVariableDeclaration)this.fUnresolvedLocalVars.removeKey(variableName);
			if(statement != null && statement instanceof ProgramElement) {
				//resolve and then call super again
				if (statement instanceof LocalDeclaration) {
					((LocalDeclaration)statement).resolveLocal(this);
				} else {
					((ProgramElement)statement).resolve(this);
				}
				binding = super.findVariable(variableName);
			}
		}
		
		return binding;
	}
	
	/**
	 * <p>
	 * This implementation first calls super, if that returns no binding or a
	 * problem binding then the list of unresolved local functions is checked,
	 * and if there is a statement that defines a function with the correct
	 * name it is resolved, and then its binding is returned.
	 * </p>
	 * 
	 * <p>
	 * This allows {@link #addUnresolvedLocalFunc(char[], AbstractMethodDeclaration)} to be used in
	 * conjunction with this method to prevent having to resolve the entire
	 * {@link AbstractMethodDeclaration} associated with this scope.
	 * </p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.Scope#findMethod(org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding, char[], org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding[], org.eclipse.wst.jsdt.internal.compiler.lookup.InvocationSite)
	 */
	public MethodBinding findMethod(ReferenceBinding receiverType, char[] selector, TypeBinding[] argumentTypes, InvocationSite invocationSite) {
		MethodBinding binding = super.findMethod(receiverType, selector, argumentTypes, invocationSite);

		// if super could not find a good binding then check list of unresolved local functions
		if((binding == null || !binding.isValidBinding()) && this.fUnresolvedLocalFuncs != null) {
			AbstractMethodDeclaration statement = (AbstractMethodDeclaration)this.fUnresolvedLocalFuncs.removeKey(selector);
			if(statement != null) {
				//resolve and then return the statements binding
				statement.resolve(this);
				binding = statement.getBinding();
			}
		}
		
		return binding;
	}
	
	/**
	 * <p>
	 * This implementation first calls super, if that returns no binding or a
	 * problem binding then the list of unresolved local functions is checked,
	 * and if there is a statement that defines a function with the correct
	 * name it is resolved, and then its binding is returned.
	 * </p>
	 * 
	 * <p>
	 * This allows {@link #addUnresolvedLocalFunc(char[], AbstractMethodDeclaration)} to be used in
	 * conjunction with this method to prevent having to resolve the entire
	 * {@link AbstractMethodDeclaration} associated with this scope.
	 * </p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.Scope#findMethod(org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding, char[], org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding[], org.eclipse.wst.jsdt.internal.compiler.lookup.InvocationSite)
	 */
	public MethodBinding findMethod(char[] methodName, TypeBinding[]argumentTypes, boolean checkVars) {
		MethodBinding binding = super.findMethod(methodName, argumentTypes, checkVars);

		// if super could not find a good binding then check list of unresolved local functions
		if((binding == null || !binding.isValidBinding()) && this.fUnresolvedLocalFuncs != null) {
			AbstractMethodDeclaration methDecl = (AbstractMethodDeclaration)this.fUnresolvedLocalFuncs.removeKey(methodName);
			if(methDecl != null) {
				//resolve and then return the statements binding
				binding = methDecl.getBinding();
				if(binding == null || !binding.isValidBinding()) {
					
					/* if it is anonymous, but has a name (has to to be here) then this is a
					 * function assignment to a variable not declared in this scope so
					 * build with compilation unit scope.
					 * 
					 * else build with this scope */
					Scope scope = null;
					if(methDecl.isAnonymous()) {
						scope = this.compilationUnitScope();
					} else {
						scope = this;
					}
					methDecl.resolve(scope);
				}
				binding = methDecl.getBinding();
			}
		}
		
		return binding;
	}
	
	/**
	 * <p>
	 * Adds an unresolved local variable defined in this scope to be resolved
	 * on an as needed basses.
	 * </p>
	 * 
	 * @param name
	 *            the name of the unresolved local variable
	 * @param expr
	 *            {@link IAbstractVariableDeclaration} that defines the unresolved local variable
	 *            that will be used to resolve it if needed
	 */
	public void addUnresolvedLocalVar(char[] name, IAbstractVariableDeclaration expr) {
		if(name != null && name.length > 0 && expr != null) {
			if(this.fUnresolvedLocalVars == null) {
				this.fUnresolvedLocalVars = new HashtableOfObject();
			}
			
			this.fUnresolvedLocalVars.put(name, expr);
		}
	}
	
	/**
	 * <p>
	 * Adds an unresolved local function defined in this scope to be resolved
	 * on an as needed basses.
	 * </p>
	 * 
	 * @param name
	 *            the name of the unresolved local function
	 * @param expr
	 *            {@link AbstractMethodDeclaration} that defines the unresolved local function
	 *            that will be used to resolve it if needed
	 */
	public void addUnresolvedLocalFunc(char[] name, AbstractMethodDeclaration func) {
		if(name != null && name.length > 0 && func != null) {
			if(this.fUnresolvedLocalFuncs == null) {
				this.fUnresolvedLocalFuncs = new HashtableOfObject();
			}
		
			this.fUnresolvedLocalFuncs.put(name, func);
		}
	}
}