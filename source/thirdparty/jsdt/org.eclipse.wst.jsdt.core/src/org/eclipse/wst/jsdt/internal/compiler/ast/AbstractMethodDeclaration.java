/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.IAbstractFunctionDeclaration;
import org.eclipse.wst.jsdt.core.ast.IArgument;
import org.eclipse.wst.jsdt.core.ast.IAssignment;
import org.eclipse.wst.jsdt.core.ast.IFunctionDeclaration;
import org.eclipse.wst.jsdt.core.ast.IFunctionExpression;
import org.eclipse.wst.jsdt.core.ast.IJsDoc;
import org.eclipse.wst.jsdt.core.ast.IProgramElement;
import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;
import org.eclipse.wst.jsdt.core.infer.InferredMethod;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalFunctionBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.parser.Parser;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortMethod;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortType;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemSeverities;


public abstract class AbstractMethodDeclaration extends Statement
	implements IAbstractFunctionDeclaration,  ProblemSeverities, ReferenceContext {

	/**
	 * <p>Current scope used by this declaration.</p>
	 */
	private MethodScope fScope;
	
	private MethodScope prevScope;
	
	/**
	 * <p>The function selector</p>
	 */
	public char[] selector;
	
	/**
	 * <p><code>true</code> if this function is defined as an anonymous function,
	 * <code>false</code> otherwise.</p>
	 * 
	 * <p><b>NOTE:</b> A function could be defend as anonymous but
	 * still have a selector if assigned to a variable.</p>
	 */
	private boolean fIsAnonymous;
	
	public int declarationSourceStart;
	public int declarationSourceEnd;
	public int modifiers;
	public Argument[] arguments;
	public Statement[] statements;
	public int explicitDeclarations;
	protected MethodBinding binding;
	public boolean ignoreFurtherInvestigation = false;
	public boolean needFreeReturn = false;
	public boolean resolveChildStatments = true;
	public boolean hasResolvedChildStatements = false;

	public Javadoc javadoc;

	public int bodyStart;
	public int bodyEnd = -1;
	public CompilationResult compilationResult;

	public InferredType inferredType;
	public InferredMethod inferredMethod;

	public boolean errorInSignature = false;
	public int exprStackPtr;
	
	/**
	 * <p>
	 * <code>true</code> if {@link #buildLocals(BlockScope)} has been called,
	 * <code>false</code> otherwise.
	 * </p>
	 */
	private boolean fhasBuiltLocals;
	
	/**
	 * <p>
	 * <code>true</code> if {@link #resolve(Scope)} has been called,
	 * <code>false</code> otherwise.
	 * </p>
	 */
	private boolean fHasResolved;
	
	/**
	 * <p>
	 * {@link IFunctionDeclaration} that this declaration is contained in,
	 * or <code>null</code> if declaration not contained in an {@link IFunctionDeclaration}
	 */
	private IFunctionDeclaration fContainingFunction;

	AbstractMethodDeclaration(CompilationResult compilationResult){
		this.compilationResult = compilationResult;
		this.prevScope = null;
		
		this.fhasBuiltLocals = false;
		this.fHasResolved = false;
		this.fContainingFunction = null;
	}

	public void setArguments( IArgument[] args) {
		if(args instanceof Argument[]) this.arguments = (Argument[])args;
	}
	
	public IArgument[] getArguments() {
		return this.arguments;
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
			case AbortType :
				throw new AbortType(this.compilationResult, problem);
			default :
				throw new AbortMethod(this.compilationResult, problem);
		}
	}

	public FlowInfo analyseCode(BlockScope classScope, FlowContext initializationContext, FlowInfo info)
	{
	 return this.analyseCode((Scope)classScope, initializationContext, info);
	}
	
	public abstract FlowInfo analyseCode(Scope classScope, FlowContext initializationContext, FlowInfo info);

	/**
	 * Bind and add argument's binding into the scope of the method
	 */
	public void bindArguments() {
		//only bind arguments if the current scope does not equal the scope last used to bind args
		if (this.arguments != null && (this.prevScope == null || this.prevScope != this.fScope)) {
			this.prevScope = this.fScope;
			
			if (this.binding == null) {
				for (int i = 0, length = this.arguments.length; i < length; i++) {
					this.arguments[i].resolve(this.fScope);
				}
				return;
			}
			if (this.arguments.length>0 && this.binding.parameters.length==0)  // types not set yet
			{
				ReferenceBinding declaringClass = this.binding.declaringClass;
				if (declaringClass instanceof SourceTypeBinding) {
					SourceTypeBinding binding = (SourceTypeBinding) declaringClass;
					binding.resolveTypesFor(this.binding,this);
				}
			}
			boolean used = this.binding.isAbstract();
			for (int i = 0, length = this.arguments.length; i < length && i < this.binding.parameters.length; i++) {
				IArgument argument = this.arguments[i];
				argument.bind(this.fScope, this.binding.parameters[i], used);
			}
		}
	}

	public CompilationResult compilationResult() {

		return this.compilationResult;
	}

	public boolean hasErrors() {
		return this.ignoreFurtherInvestigation;
	}

	public boolean isAbstract() {

		if (this.binding != null)
			return this.binding.isAbstract();
		return (this.modifiers & ClassFileConstants.AccAbstract) != 0;
	}

	public boolean isClinit() {

		return false;
	}


	/**
	 * @return If the {@link #inferredMethod} is set then use that to determine if
	 * this declaration is a constructor, else <code>false</code>
	 */
	public boolean isConstructor() {
		boolean isConstructor = false;
		if(this.inferredMethod != null) {
			isConstructor = this.inferredMethod.isConstructor;
		}
		return isConstructor;
	}

	public boolean isDefaultConstructor() {

		return false;
	}

	public boolean isInitializationMethod() {

		return false;
	}

	public boolean isMethod() {

		return false;
	}

	public boolean isStatic() {

		if (this.binding != null)
			return this.binding.isStatic();
		return (this.modifiers & ClassFileConstants.AccStatic) != 0;
	}
	
	public boolean isInferredJsDocType() {
		return (this.bits & ASTNode.IsInferredJsDocType) != 0;
	}

	/**
	 * Fill up the method body with statement
	 * @param parser
	 * @param unit
	 */
	public abstract void parseStatements(
		Parser parser,
		CompilationUnitDeclaration unit);

	public StringBuffer printStatement(int indent, StringBuffer output)
	{
		return print(indent,output);
	}

	public StringBuffer print(int tab, StringBuffer output) {

		if (this.javadoc != null) {
			this.javadoc.print(tab, output);
		}
		printIndent(tab, output);

		output.append("function "); //$NON-NLS-1$
		if (this.selector!=null)
			output.append(this.selector);
		output.append('(');
		if (this.arguments != null) {
			for (int i = 0; i < this.arguments.length; i++) {
				if (i > 0) output.append(", "); //$NON-NLS-1$
				this.arguments[i].print(0, output);
			}
		}
		output.append(')');
		printBody(tab + 1, output);
		return output;
	}

	public StringBuffer printBody(int indent, StringBuffer output) {

		if (isAbstract() || (this.modifiers & ExtraCompilerModifiers.AccSemicolonBody) != 0)
			return output.append(';');

		output.append(" {"); //$NON-NLS-1$
		if (this.statements != null) {
			for (int i = 0; i < this.statements.length; i++) {
				output.append('\n');
				this.statements[i].printStatement(indent, output);
			}
		}
		output.append('\n');
		printIndent(indent == 0 ? 0 : indent - 1, output).append('}');
		return output;
	}

	public StringBuffer printReturnType(int indent, StringBuffer output) {
		return output;
	}

	public void resolve(Scope upperScope) {
		/* resolve if the scope is not yet set or
		 * the locals were built causing the scope to be set without resolving */
		if (this.getScope() == null || this.fhasBuiltLocals) {
			this.fHasResolved = true;
			
			//set the scope if it has not yet been set
			if(this.getScope() == null) {
				this.setScope(new MethodScope(upperScope,this, false));
			}
			
			SourceTypeBinding compilationUnitBinding = upperScope.enclosingCompilationUnit();
			if (this.getName()!=null && !this.hasBinding()) {
				//is local if the upper scope is not a compilation unit scope
				boolean isLocal = upperScope.kind != Scope.COMPILATION_UNIT_SCOPE;
				
				/* if inferred method has declaring binding, use that
				 * else use compilation unit binding */
				SourceTypeBinding declaringBinding = null;
				if(this.getInferredMethod() != null &&
							this.getInferredMethod().inType != null &&
							this.getInferredMethod().inType.binding != null) {
					
					declaringBinding = this.getInferredMethod().inType.binding;
				} else {
					declaringBinding = compilationUnitBinding;
				}
				
				//create and set the method binding
				MethodBinding methodBinding = fScope.createMethod(this,
						this.getName(), declaringBinding, false, isLocal);
				this.setBinding(methodBinding);
			}
			
			if (this.binding != null) {
				MethodBinding methodBinding = compilationUnitBinding
						.resolveTypesFor(this.binding,this);
				if (methodBinding != null && methodBinding.selector != null) {
					MethodScope enclosingMethodScope = upperScope.enclosingMethodScope();
					if (enclosingMethodScope != null) {
						enclosingMethodScope.addLocalMethod(methodBinding);
					} else {
						compilationUnitBinding.addMethod(methodBinding);
						upperScope.environment().defaultPackage.addBinding(
								methodBinding, methodBinding.selector,
								Binding.METHOD);
					}
				}
			}
		}

		if (this.binding == null) {
			this.ignoreFurtherInvestigation = true;
		}

		try {
			// only need to resolve args, jsdoc, and statments once per function
			if(resolveChildStatments && !hasResolvedChildStatements) {
				hasResolvedChildStatements = true;
				bindArguments();
				resolveJavadoc();
				resolveStatements();
			}
		} catch (AbortMethod e) {	// ========= abort on fatal error =============
			this.ignoreFurtherInvestigation = true;
		}
	}

	public void resolveJavadoc() {

		if (this.binding == null) return;
		if (this.javadoc != null) {
			this.javadoc.resolve(this.fScope);
			return;
		}
		if (this.binding.declaringClass != null && !this.binding.declaringClass.isLocalType()) {
			this.fScope.problemReporter().javadocMissing(this.sourceStart, this.sourceEnd, this.binding.modifiers);
		}
	}

	// made some changes here to fix https://bugs.eclipse.org/bugs/show_bug.cgi?id=262728
	public void resolveStatements() {
		if (this.statements != null) {
			List nonFunctions = null;
			List functions = null;
			for (int i = 0, length = this.statements.length; i < length; i++) {
				Statement statement = this.statements[i];
				
				//look for an AbstractMethodDeclaration as part of the statement
				AbstractMethodDeclaration methodDecl = null;
				BlockScope scope = this.fScope;
				if (statement instanceof AbstractMethodDeclaration) {
					methodDecl = (AbstractMethodDeclaration)statement;
					
					//fully process the method declaration later
					if(functions == null) {
						functions = new ArrayList();
					}
					functions.add(methodDecl);
				}
				if(methodDecl != null) {
					methodDecl.resolveChildStatments = false;
					methodDecl.resolve(scope);
					methodDecl.resolveChildStatments = true;
				}
				
				//if the statement itself was not a method declaration save it to processes after processing all functions
				if(!(statement instanceof AbstractMethodDeclaration)) {
					if(nonFunctions == null) {
						nonFunctions = new ArrayList();
					}
					nonFunctions.add(statements[i]);
				}
			}
			
			/* resolve all none method declarations, this includes expressions that have a child method declaration,
			 * such as an assignment
			 */
			if(nonFunctions != null) {
				for(int j = 0; j < nonFunctions.size(); j++) {
					Statement statement = (Statement)nonFunctions.get(j);
					AbstractMethodDeclaration methodDecl = null;
					BlockScope scope = this.fScope;
					if(statement instanceof AbstractVariableDeclaration) {
						AbstractVariableDeclaration variableDecl = (AbstractVariableDeclaration)statement;
						if(variableDecl.initialization instanceof IFunctionExpression) {
							methodDecl = ((IFunctionExpression)variableDecl.initialization).getMethodDeclaration();
						}
					} else if(statement instanceof IAssignment) {
						IAssignment assignment = (IAssignment)statement;
						if(assignment.getExpression() instanceof IFunctionExpression) {
							methodDecl = ((IFunctionExpression)assignment.getExpression()).getMethodDeclaration();
						}
						
						//if the LHS is an undeclared single name resolve the function at the compilation unit level
						if(assignment.getLeftHandSide() instanceof SingleNameReference) {
							SingleNameReference nameRef = (SingleNameReference)assignment.getLeftHandSide();
							
							/* if the binding is a problem binding or not a local function
							 * binding then built with unit scope because it is not a local function */
							Binding binding = nameRef.findBinding(this.fScope);
							if(binding instanceof ProblemBinding || !(binding instanceof LocalFunctionBinding || binding instanceof LocalVariableBinding)) {
								scope = this.fScope.compilationUnitScope();
							}
						}
					}
					if(methodDecl != null) {
						methodDecl.resolveChildStatments = false;
						methodDecl.resolve(scope);
						methodDecl.resolveChildStatments = true;
					}
					statement.resolve(this.fScope);
				}
			}
			
			// now its time to resolve the children statements of the method declarations
			if(functions != null) {
				for(int f = 0; f < functions.size(); f++) {
					((Statement)functions.get(f)).resolve(this.fScope);
				}
			}
		} else if ((this.bits & UndocumentedEmptyBlock) != 0) {
			this.fScope.problemReporter().undocumentedEmptyBlock(this.bodyStart-1, this.bodyEnd+1);
		}
	}

	public void tagAsHavingErrors() {
		this.ignoreFurtherInvestigation = true;
	}

	public void traverse(
		ASTVisitor visitor,
		Scope classScope) {
		// default implementation: subclass will define it
	}

	public void resolve(BlockScope scope) {
		this.resolve((Scope)scope);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode#isInferred()
	 */
	public boolean isInferred() {
		return this.inferredMethod != null;
	}
	
	public int getASTType() {
		return IASTNode.ABSTRACT_FUNCTION_DECLARATION;
	
	}
	
	public IJsDoc getJsDoc() {
		return this.javadoc;
	}

	public IProgramElement[] getStatements() {
		return this.statements;
	}

	/**
	 * <p>Returns this functions selector or inferred selector in that priority
	 * order, or <code>null</code> if neither are defined.</p>
	 * 
	 * @see org.eclipse.wst.jsdt.core.ast.IAbstractFunctionDeclaration#getName()
	 */
	public char[] getName() {
		char[] name = null;
		
		if(this.selector != null) {
			name = this.selector;
		} else if(this.inferredMethod != null && this.inferredMethod.name != null) {
			name = this.inferredMethod.name;
		}
		
		return name;
	}

	public void setInferredType(InferredType type) {
		this.inferredType=type;
	}

	public InferredMethod getInferredMethod() {
		return this.inferredMethod;
	}

	public InferredType getInferredType() {
		return this.inferredType;
	}
	
	/**
	 * @return {@link MethodBinding} associated with this function declaration
	 */
	public MethodBinding getBinding() {
		return this.binding;
	}
	
	/**
	 * <p>Sets the {@link MethodBinding} associated with this function declaration.
	 * If one is already set then it will be overwritten.</p>
	 * 
	 * @param binding {@link MethodBinding} to associate with this function declaration
	 */
	public void setBinding(MethodBinding binding) {
		this.binding = binding;
	}
	
	/**
	 * @return <code>true</code> if a {@link MethodBinding} has already been associated
	 * with this function declaration, <code>false</code> otherwise.
	 */
	public boolean hasBinding() {
		return this.binding != null;
	}
	
	/**
	 * <p>Sets the selector.</p>
	 * 
	 * @param selector for this function declaration
	 */
	public void setSelector(char[] selector) {
		this.selector = selector;
	}
	
	/**
	 * <p>Set whether this function declared as anonymous or not.</p>
	 * 
	 * <p><b>NOTE:</b> A function could be defend as anonymous but
	 * still have a selector if assigned to a variable.</p>
	 * 
	 * @param isAnonymous <code>true</code> if this function is anonymous,
	 * <code>false</code> otherwise
	 */
	public void setIsAnonymous(boolean isAnonymous) {
		this.fIsAnonymous = isAnonymous;
	}
	
	/**
	 * <p><b>NOTE:</b> A function could be defend as anonymous but
	 * still have a selector if assigned to a variable.</p>
	 * 
	 * @return <code>true</code> if this function is anonymous,
	 * <code>false</code> otherwise. 
	 */
	public boolean isAnonymous() {
		return this.fIsAnonymous || this.getName() == null;
	}
	
	/**
	 * @param scope
	 *            {@link MethodScope} to use for this declaration
	 */
	public void setScope(MethodScope scope) {
		this.fScope = scope;
	}

	/**
	 * @return {@link MethodScope} used by this declaration, or
	 *         <code>null</code> if none is set
	 */
	public MethodScope getScope() {
		return this.fScope;
	}
	
	/**
	 * @param containingFunction {@link IFunctionDeclaration} that contains this declaration
	 */
	public void setContainingFunction(IFunctionDeclaration containingFunction) {
		//declaration can never and should never contain itself
		if(containingFunction != this) {
			this.fContainingFunction = containingFunction;
		}
	}
	
	/**
	 * @return {@link IFunctionDeclaration} that contains this declaration,
	 * or <code>null</code> if this declaration is not contained in an {@link IFunctionDeclaration}
	 */
	public IFunctionDeclaration getContainingFunction() {
		return this.fContainingFunction;
	}
	
	/**
	 * <p>
	 * Finds all of the variables and functions defined in this function
	 * declaration and adds them to this functions scope.
	 * </p>
	 * 
	 * <p>
	 * This is much cheaper then {@link #resolve(Scope)} and should be used
	 * whenever possible.
	 * </p>
	 * 
	 * <p>
	 * <b>NOTE:</b> This is a no-op if this function has already been invoked
	 * or {@link #resolve(Scope)} has already been invoked.
	 * </p>
	 * 
	 * @param givenUpperScope
	 *            {@link BlockScope} to use as the upper scope for this
	 *            functions scope
	 */
	public void buildLocals(Scope givenUpperScope) {
		//this is not resolving, but there is no point in doing it if already resolved
		if(!this.fhasBuiltLocals && !this.fHasResolved) {
			this.fhasBuiltLocals = true;
			
			//build the locals all for all of the containing functions
			Scope upperScope = givenUpperScope;
			IFunctionDeclaration containingFunc = this.getContainingFunction();
			if(containingFunc instanceof AbstractMethodDeclaration) {
				((AbstractMethodDeclaration) containingFunc).buildLocals(givenUpperScope);
				upperScope = ((AbstractMethodDeclaration) containingFunc).getScope();
			}
		
			//create scope if it has not yet been created
			if (this.getScope() == null ) {
				this.setScope(new MethodScope(upperScope, this, false));
			}
			
			//traverse this functions statements looking for variables and functions
			this.traverse(new ASTVisitor() {
				/**
				 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.Argument, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
				 */
				public boolean visit(Argument argument, BlockScope scope) {
					if(scope != null && scope instanceof MethodScope) {
						((MethodScope) scope).addUnresolvedLocalVar(argument.getName(), argument);
					}
					
					return true;
				}

				/**
				 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
				 */
				public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
					if(scope != null && scope instanceof MethodScope) {
						/* be sure to add all variable declarations
						 * 
						 * var b, c, d = "foo" 
						 * 
						 * do nothing in case of localDeclaration is already added in order to prevent 
						 * repeat on adding the same variables
						 * (which means that all the variable declarations were added before
						 *
						MethodScope methodScope = (MethodScope)scope;
						if (methodScope.getUnresolvedLocalVar(localDeclaration.getName()) == null) {
							AbstractVariableDeclaration currVarDecl = localDeclaration;
							while(currVarDecl != null) {
								methodScope.addUnresolvedLocalVar(currVarDecl.getName(), currVarDecl);
								currVarDecl = currVarDecl.nextLocal;
							}
						}
						*/
						/* 
						 * No need to add all localDeclaration.nextLocal-s here, 
						 * because it's done by LocalDeclaration.traverse() method 
						 * (the only place that calls this visit method.
						 * 
						 * See: https://bugs.eclipse.org/bugs/show_bug.cgi?id=431547
						 */  
						((MethodScope)scope).addUnresolvedLocalVar(localDeclaration.getName(), localDeclaration);
					}
					
					return true;
				}
				
				/**
				 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration, org.eclipse.wst.jsdt.internal.compiler.lookup.Scope)
				 */
				public boolean visit(MethodDeclaration methodDeclaration, Scope scope) {
					boolean isSelf = AbstractMethodDeclaration.this == methodDeclaration;
					if(scope != null && scope instanceof MethodScope) {
						if(!isSelf) {
							((MethodScope) scope).addUnresolvedLocalFunc(methodDeclaration.getName(), methodDeclaration);
						}
					}
					
					return isSelf;
				}
			}, this.getScope());
		}
	}
	
	/**
	 * <p>Given an {@link IProgramElement} returns the {@link AbstractMethodDeclaration}
	 * if there is one in the given element.  The element itself could be the method, or
	 * the method could be part of a declaration, assignment, and so on.</p>
	 * 
	 * @param element to search for an {@link AbstractMethodDeclaration}
	 * 
	 * @return {@link AbstractMethodDeclaration} if the given {@link IProgramElement} contains
	 * one, <code>null</code> otherwise
	 */
	public static AbstractMethodDeclaration findMethodDeclaration(IProgramElement element) {
		AbstractMethodDeclaration methodDecl = null;
		
		/* if the statement is a method declaration
		 * else if statement is a variable declaration that could have a function assigned to it
		 * else if the statement is an assignment that could be assigning a function to a variable
		 */
		if (element instanceof AbstractMethodDeclaration) {
			methodDecl = (AbstractMethodDeclaration)element;
		} else if(element instanceof AbstractVariableDeclaration) {
			AbstractVariableDeclaration variableDecl = (AbstractVariableDeclaration)element;
			if(variableDecl.initialization instanceof IFunctionExpression) {
				methodDecl = ((IFunctionExpression)variableDecl.initialization).getMethodDeclaration();
			}
		} else if(element instanceof IAssignment) {
			IAssignment assignment = (IAssignment)element;
			if(assignment.getExpression() instanceof IFunctionExpression) {
				methodDecl = ((IFunctionExpression)assignment.getExpression()).getMethodDeclaration();
			}
		} else if(element instanceof IFunctionExpression) {
			methodDecl = ((IFunctionExpression)element).getMethodDeclaration();
		}
		
		return methodDecl;
	}
}