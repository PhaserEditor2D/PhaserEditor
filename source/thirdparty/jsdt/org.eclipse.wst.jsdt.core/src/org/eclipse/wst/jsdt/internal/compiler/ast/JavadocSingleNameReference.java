/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
import org.eclipse.wst.jsdt.core.ast.IJsDocSingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TagBits;

public class JavadocSingleNameReference extends SingleNameReference implements IJsDocSingleNameReference {

	public int tagSourceStart, tagSourceEnd;
	public TypeReference []types;

	public JavadocSingleNameReference(char[] source, long pos, int tagStart, int tagEnd) {
		super(source, pos);
		this.tagSourceStart = tagStart;
		this.tagSourceEnd = tagEnd;
		this.bits |= InsideJavadoc;
	}

	public void resolve(BlockScope scope) {
		resolve(scope, true, scope.compilerOptions().reportUnusedParameterIncludeDocCommentReference);
	}

	/**
	 * Resolve without warnings
	 */
	public void resolve(BlockScope scope, boolean warn, boolean considerParamRefAsUsage) {

		LocalVariableBinding variableBinding = scope.findVariable(this.token);
		if (variableBinding != null && variableBinding.isValidBinding() && ((variableBinding.tagBits & TagBits.IsArgument) != 0)) {
			this.binding = variableBinding;
			if (considerParamRefAsUsage) {
				variableBinding.useFlag = LocalVariableBinding.USED;
			}
			return;
		}
		if (warn) {
			try {
				MethodScope methScope = (MethodScope) scope;
				scope.problemReporter().javadocUndeclaredParamTagName(this.token, this.sourceStart, this.sourceEnd, methScope.referenceMethod().modifiers);
			}
			catch (Exception e) {
				scope.problemReporter().javadocUndeclaredParamTagName(this.token, this.sourceStart, this.sourceEnd, -1);
			}
		}
	}

	/* (non-Javadoc)
	 * Redefine to capture javadoc specific signatures
	 * @see org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode#traverse(org.eclipse.wst.jsdt.internal.compiler.ASTVisitor, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}
	/* (non-Javadoc)
	 * Redefine to capture javadoc specific signatures
	 * @see org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode#traverse(org.eclipse.wst.jsdt.internal.compiler.ASTVisitor, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public void traverse(ASTVisitor visitor, ClassScope scope) {
		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}

	public StringBuffer printExpression(int indent, StringBuffer output){

		if (types!=null && types.length>0)
		{
			output.append("{"); //$NON-NLS-1$
			for (int i = 0; i < types.length; i++) {
				if (i>0)
					output.append('|');
				types[i].printExpression(indent, output);
			}
			output.append("} "); //$NON-NLS-1$
		}
		output=super.printExpression(indent, output);
		return output;
	}
	public int getASTType() {
		return IASTNode.JSDOC_SINGLE_NAME_REFERENCE;
	
	}
}
