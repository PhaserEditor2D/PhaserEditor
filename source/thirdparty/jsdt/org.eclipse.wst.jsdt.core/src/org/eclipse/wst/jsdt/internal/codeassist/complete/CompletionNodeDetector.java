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
package org.eclipse.wst.jsdt.internal.codeassist.complete;

import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.ast.AND_AND_Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Assignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.EqualExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.InstanceOfExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ListExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.OR_OR_Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.PostfixExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.PrefixExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedThisReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.StringLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.SuperReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ThisReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;

/**
 * Detect the presence of a node in expression
 */
public class CompletionNodeDetector extends ASTVisitor {
	private ASTNode searchedNode;
	private ASTNode parent;
	private boolean result;

	public CompletionNodeDetector(ASTNode searchedNode, ASTNode visitedAst){
		this.searchedNode = searchedNode;
		this.result = false;

		if(searchedNode != null && visitedAst != null) {
			visitedAst.traverse(this, null);
		}
	}

	public boolean containsCompletionNode() {
		return this.result;
	}

	public ASTNode getCompletionNodeParent() {
		return this.parent;
	}
	public void endVisit(AllocationExpression allocationExpression, BlockScope scope) {
		endVisit(allocationExpression);
	}
	public void endVisit(AND_AND_Expression and_and_Expression, BlockScope scope) {
		endVisit(and_and_Expression);
	}
	public void endVisit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
		endVisit(arrayAllocationExpression);
	}
	public void endVisit(ArrayInitializer arrayInitializer, BlockScope scope) {
		endVisit(arrayInitializer);
	}
	public void endVisit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, BlockScope scope) {
		endVisit(arrayQualifiedTypeReference);
	}
	public void endVisit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, ClassScope scope) {
		endVisit(arrayQualifiedTypeReference);
	}
	public void endVisit(ArrayReference arrayReference, BlockScope scope) {
		endVisit(arrayReference);
	}
	public void endVisit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		endVisit(arrayTypeReference);
	}
	public void endVisit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
		endVisit(arrayTypeReference);
	}
	public void endVisit(Assignment assignment, BlockScope scope) {
		endVisit(assignment);
	}
	public void endVisit(BinaryExpression binaryExpression, BlockScope scope) {
		endVisit(binaryExpression);
	}
	public void endVisit(CompoundAssignment compoundAssignment, BlockScope scope) {
		endVisit(compoundAssignment);
	}
	public void endVisit(ConditionalExpression conditionalExpression, BlockScope scope) {
		endVisit(conditionalExpression);
	}
	public void endVisit(EqualExpression equalExpression, BlockScope scope) {
		endVisit(equalExpression);
	}
	public void endVisit(ExplicitConstructorCall explicitConstructor, BlockScope scope) {
		endVisit(explicitConstructor);
	}
	public void endVisit(FieldReference fieldReference, BlockScope scope) {
		endVisit(fieldReference);
	}
	public void endVisit(InstanceOfExpression instanceOfExpression, BlockScope scope) {
		endVisit(instanceOfExpression);
	}
	public void endVisit(MessageSend messageSend, BlockScope scope) {
		endVisit(messageSend);
	}
	public void endVisit(ListExpression listExpression, BlockScope scope) {
		endVisit(listExpression);
	}
	public void endVisit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		endVisit(or_or_Expression);
	}
	public void endVisit(PostfixExpression postfixExpression, BlockScope scope) {
		endVisit(postfixExpression);
	}
	public void endVisit(PrefixExpression prefixExpression, BlockScope scope) {
		endVisit(prefixExpression);
	}
	public void endVisit(QualifiedAllocationExpression qualifiedAllocationExpression, BlockScope scope) {
		endVisit(qualifiedAllocationExpression);
	}
	public void endVisit(QualifiedNameReference qualifiedNameReference, BlockScope scope) {
		endVisit(qualifiedNameReference);
	}
	public void endVisit(QualifiedThisReference qualifiedThisReference, BlockScope scope) {
		endVisit(qualifiedThisReference);
	}
	public void endVisit(QualifiedTypeReference qualifiedTypeReference, BlockScope scope) {
		endVisit(qualifiedTypeReference);
	}
	public void endVisit(QualifiedTypeReference qualifiedTypeReference, ClassScope scope) {
		endVisit(qualifiedTypeReference);
	}
	public void endVisit(SingleNameReference singleNameReference, BlockScope scope) {
		endVisit(singleNameReference);
	}
	public void endVisit(SingleTypeReference singleTypeReference, BlockScope scope) {
		endVisit(singleTypeReference);
	}
	public void endVisit(SingleTypeReference singleTypeReference, ClassScope scope) {
		endVisit(singleTypeReference);
	}
	public void endVisit(SuperReference superReference, BlockScope scope) {
		endVisit(superReference);
	}
	public void endVisit(ThisReference thisReference, BlockScope scope) {
		endVisit(thisReference);
	}
	public void endVisit(UnaryExpression unaryExpression, BlockScope scope) {
		endVisit(unaryExpression);
	}
	public boolean visit(AllocationExpression allocationExpression, BlockScope scope) {
		return this.visit(allocationExpression);
	}
	public boolean visit(AND_AND_Expression and_and_Expression, BlockScope scope) {
		return this.visit(and_and_Expression);
	}
	public boolean visit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
		return this.visit(arrayAllocationExpression);
	}
	public boolean visit(ArrayInitializer arrayInitializer, BlockScope scope) {
		return this.visit(arrayInitializer);
	}
	public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, BlockScope scope) {
		return this.visit(arrayQualifiedTypeReference);
	}
	public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, ClassScope scope) {
		return this.visit(arrayQualifiedTypeReference);
	}
	public boolean visit(ArrayReference arrayReference, BlockScope scope) {
		return this.visit(arrayReference);
	}
	public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		return this.visit(arrayTypeReference);
	}
	public boolean visit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
		return this.visit(arrayTypeReference);
	}
	public boolean visit(Assignment assignment, BlockScope scope) {
		return this.visit(assignment);
	}
	public boolean visit(BinaryExpression binaryExpression, BlockScope scope) {
		return this.visit(binaryExpression);
	}
	public boolean visit(CompoundAssignment compoundAssignment, BlockScope scope) {
		return this.visit(compoundAssignment);
	}
	public boolean visit(ConditionalExpression conditionalExpression, BlockScope scope) {
		return this.visit(conditionalExpression);
	}
	public boolean visit(EqualExpression equalExpression, BlockScope scope) {
		return this.visit(equalExpression);
	}
	public boolean visit(ExplicitConstructorCall explicitConstructor, BlockScope scope) {
		return this.visit(explicitConstructor);
	}
	public boolean visit(FieldReference fieldReference, BlockScope scope) {
		return this.visit(fieldReference);
	}
	public boolean visit(ListExpression listExpression, BlockScope scope) {
		return this.visit(listExpression);
	}
	public boolean visit(InstanceOfExpression instanceOfExpression, BlockScope scope) {
		return this.visit(instanceOfExpression);
	}
	public boolean visit(MessageSend messageSend, BlockScope scope) {
		return this.visit(messageSend);
	}
	public boolean visit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		return this.visit(or_or_Expression);
	}
	public boolean visit(PostfixExpression postfixExpression, BlockScope scope) {
		return this.visit(postfixExpression);
	}
	public boolean visit(PrefixExpression prefixExpression, BlockScope scope) {
		return this.visit(prefixExpression);
	}
	public boolean visit(QualifiedAllocationExpression qualifiedAllocationExpression, BlockScope scope) {
		return this.visit(qualifiedAllocationExpression);
	}
	public boolean visit(QualifiedNameReference qualifiedNameReference, BlockScope scope) {
		return this.visit(qualifiedNameReference);
	}
	public boolean visit(QualifiedThisReference qualifiedThisReference, BlockScope scope) {
		return this.visit(qualifiedThisReference);
	}
	public boolean visit(QualifiedTypeReference qualifiedTypeReference, BlockScope scope) {
		return this.visit(qualifiedTypeReference);
	}
	public boolean visit(QualifiedTypeReference qualifiedTypeReference, ClassScope scope) {
		return this.visit(qualifiedTypeReference);
	}
	public boolean visit(SingleNameReference singleNameReference, BlockScope scope) {
		return this.visit(singleNameReference);
	}
	public boolean visit(SingleTypeReference singleTypeReference, BlockScope scope) {
		return this.visit(singleTypeReference);
	}
	public boolean visit(SingleTypeReference singleTypeReference, ClassScope scope) {
		return this.visit(singleTypeReference);
	}
	public boolean visit(StringLiteral stringLiteral, BlockScope scope) {
		return this.visit(stringLiteral);
	}
	public boolean visit(SuperReference superReference, BlockScope scope) {
		return this.visit(superReference);
	}
	public boolean visit(ThisReference thisReference, BlockScope scope) {
		return this.visit(thisReference);
	}
	public boolean visit(UnaryExpression unaryExpression, BlockScope scope) {
		return this.visit(unaryExpression);
	}
	private void endVisit(ASTNode astNode) {
		if(this.result && this.parent == null && astNode != this.searchedNode) {
			if(!(astNode instanceof AllocationExpression && ((AllocationExpression) astNode).type == this.searchedNode)
				&& !(astNode instanceof ConditionalExpression && ((ConditionalExpression) astNode).valueIfTrue == this.searchedNode)
				&& !(astNode instanceof ConditionalExpression && ((ConditionalExpression) astNode).valueIfFalse == this.searchedNode)) {
				this.parent = astNode;
			}
		}
	}
	private boolean visit(ASTNode astNode) {
		if(astNode == this.searchedNode) {
			this.result = true;
		}
		return !this.result;
	}
}
