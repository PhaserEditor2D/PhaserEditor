/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.flow.ExceptionHandlingFlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowContext;
import org.eclipse.wst.jsdt.internal.compiler.flow.FlowInfo;
import org.eclipse.wst.jsdt.internal.compiler.flow.InitializationFlowContext;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.wst.jsdt.internal.compiler.parser.Parser;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortMethod;

public class Clinit extends AbstractMethodDeclaration  {

	public Clinit(CompilationResult compilationResult) {
		super(compilationResult);
		modifiers = 0;
		selector = TypeConstants.CLINIT;
	}

	public FlowInfo analyseCode(
		Scope classScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {

		InitializationFlowContext staticInitializerFlowContext =(InitializationFlowContext)flowContext;
		if (ignoreFurtherInvestigation)
			return flowInfo;
		try {
			ExceptionHandlingFlowContext clinitContext =
				new ExceptionHandlingFlowContext(
					staticInitializerFlowContext.parent,
					this,
					Binding.NO_EXCEPTIONS,
					this.getScope(),
					FlowInfo.DEAD_END);

			// check for missing returning path
			this.needFreeReturn = (flowInfo.tagBits & FlowInfo.UNREACHABLE) == 0;


			// check missing blank final field initializations
			flowInfo = flowInfo.mergedWith(staticInitializerFlowContext.initsOnReturn);
			
			// check static initializers thrown exceptions
			staticInitializerFlowContext.checkInitializerExceptions(
				this.getScope(),
				clinitContext,
				flowInfo);
		} catch (AbortMethod e) {
			this.ignoreFurtherInvestigation = true;
		}
		return flowInfo;
	}

	public boolean isClinit() {

		return true;
	}

	public boolean isInitializationMethod() {

		return true;
	}

	public boolean isStatic() {

		return true;
	}

	public void parseStatements(Parser parser, CompilationUnitDeclaration unit) {
		//the clinit is filled by hand ....
	}

	public StringBuffer print(int tab, StringBuffer output) {

		printIndent(tab, output).append("<clinit>()"); //$NON-NLS-1$
		printBody(tab + 1, output);
		return output;
	}

	public void resolve(ClassScope classScope) {

		this.setScope(new MethodScope(classScope, classScope.referenceContext, true));
	}

	public void traverse(
		ASTVisitor visitor,
		ClassScope classScope) {

		visitor.visit(this, classScope);
		visitor.endVisit(this, classScope);
	}

	public void setAssertionSupport(FieldBinding assertionSyntheticFieldBinding, boolean needClassLiteralField) {

		// we need to add the field right now, because the field infos are generated before the methods
		if (needClassLiteralField) {
			SourceTypeBinding sourceType =
				this.getScope().outerMostClassScope().enclosingSourceType();
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=22334
		}
	}
	public int getASTType() {
		return IASTNode.CL_INIT;
	
	}
}
