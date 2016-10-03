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
package org.eclipse.wst.jsdt.internal.formatter;

import java.util.ArrayList;

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
import org.eclipse.wst.jsdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.wst.jsdt.internal.compiler.ast.CombinedBinaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.DoubleLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.EqualExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ExtendedStringLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.FalseLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.FunctionExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.InstanceOfExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.IntLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.ListExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.NullLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.OR_OR_Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds;
import org.eclipse.wst.jsdt.internal.compiler.ast.PostfixExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.PrefixExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedThisReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.RegExLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.StringLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.StringLiteralConcatenation;
import org.eclipse.wst.jsdt.internal.compiler.ast.SuperReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ThisReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.UndefinedLiteral;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.parser.TerminalTokens;

class BinaryExpressionFragmentBuilder
	extends ASTVisitor {

	ArrayList fragmentsList;
	ArrayList operatorsList;
	private int realFragmentsSize;

	BinaryExpressionFragmentBuilder() {
		this.fragmentsList = new ArrayList();
		this.operatorsList = new ArrayList();
		this.realFragmentsSize = 0;
	}

	private final void addRealFragment(ASTNode node) {
		this.fragmentsList.add(node);
		this.realFragmentsSize++;
	}

	private final void addSmallFragment(ASTNode node) {
		this.fragmentsList.add(node);
	}

	private boolean buildFragments(Expression expression) {
		if (((expression.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT) != 0) {
			addRealFragment(expression);
			return false;
		} else {
			return true;
		}
	}

	public ASTNode[] fragments() {
		ASTNode[] fragments = new ASTNode[this.fragmentsList.size()];
		this.fragmentsList.toArray(fragments);
		return fragments;
	}

	public int[] operators() {
		int length = operatorsList.size();
		int[] tab = new int[length];
		for (int i = 0; i < length; i++) {
			tab[i] = ((Integer)operatorsList.get(i)).intValue();
		}
		return tab;
	}

	public int realFragmentsSize() {
		return this.realFragmentsSize;
	}

	public boolean visit(
		AllocationExpression allocationExpression,
		BlockScope scope) {
			this.addRealFragment(allocationExpression);
			return false;
	}

	public boolean visit(
		AND_AND_Expression and_and_Expression,
		BlockScope scope) {

		if (((and_and_Expression.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT) != 0) {
			addRealFragment(and_and_Expression);
		} else {
			and_and_Expression.left.traverse(this, scope);
			this.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNameAND_AND));
			and_and_Expression.right.traverse(this, scope);
		}
		return false;
	}

	public boolean visit(
		ArrayAllocationExpression arrayAllocationExpression,
		BlockScope scope) {
			this.addRealFragment(arrayAllocationExpression);
			return false;
	}

	public boolean visit(ArrayInitializer arrayInitializer, BlockScope scope) {
		this.addRealFragment(arrayInitializer);
		return false;
	}

	public boolean visit(
		ArrayQualifiedTypeReference arrayQualifiedTypeReference,
		BlockScope scope) {
			this.addRealFragment(arrayQualifiedTypeReference);
			return false;
	}

	public boolean visit(
		ArrayQualifiedTypeReference arrayQualifiedTypeReference,
		ClassScope scope) {
			this.addRealFragment(arrayQualifiedTypeReference);
			return false;
	}

	public boolean visit(ArrayReference arrayReference, BlockScope scope) {
		this.addRealFragment(arrayReference);
		return false;
	}

	public boolean visit(
		ArrayTypeReference arrayTypeReference,
		BlockScope scope) {
			this.addRealFragment(arrayTypeReference);
			return false;
	}

	public boolean visit(
		ArrayTypeReference arrayTypeReference,
		ClassScope scope) {
			this.addRealFragment(arrayTypeReference);
			return false;
	}

	public boolean visit(Assignment assignment, BlockScope scope) {
		this.addRealFragment(assignment);
		return false;
	}

	public boolean visit(BinaryExpression binaryExpression, BlockScope scope) {
		if (binaryExpression instanceof CombinedBinaryExpression) {
			CombinedBinaryExpression expression = (CombinedBinaryExpression) binaryExpression;
			if (expression.referencesTable != null) {
				return this.visit(expression, scope);
			}
		}
		final int numberOfParens = (binaryExpression.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT;
		if (numberOfParens > 0) {
			this.addRealFragment(binaryExpression);
		} else {
			switch((binaryExpression.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT) {
				case OperatorIds.PLUS :
					if (buildFragments(binaryExpression)) {
						binaryExpression.left.traverse(this, scope);
						this.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNamePLUS));
						binaryExpression.right.traverse(this, scope);
					}
					return false;
				case OperatorIds.MINUS :
					if (buildFragments(binaryExpression)) {
						binaryExpression.left.traverse(this, scope);
						this.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNameMINUS));
						binaryExpression.right.traverse(this, scope);
					}
					return false;
				case OperatorIds.MULTIPLY :
					if (buildFragments(binaryExpression)) {
						binaryExpression.left.traverse(this, scope);
						this.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNameMULTIPLY));
						binaryExpression.right.traverse(this, scope);
					}
					return false;
				case OperatorIds.REMAINDER :
					if (buildFragments(binaryExpression)) {
						binaryExpression.left.traverse(this, scope);
						this.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNameREMAINDER));
						binaryExpression.right.traverse(this, scope);
					}
					return false;
				case OperatorIds.XOR :
					if (buildFragments(binaryExpression)) {
						binaryExpression.left.traverse(this, scope);
						this.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNameXOR));
						binaryExpression.right.traverse(this, scope);
					}
					return false;
				case OperatorIds.DIVIDE :
					if (buildFragments(binaryExpression)) {
						binaryExpression.left.traverse(this, scope);
						this.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNameDIVIDE));
						binaryExpression.right.traverse(this, scope);
					}
					return false;
				case OperatorIds.OR :
					if (buildFragments(binaryExpression)) {
						binaryExpression.left.traverse(this, scope);
						this.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNameOR));
						binaryExpression.right.traverse(this, scope);
					}
					return false;
				case OperatorIds.AND :
					if (buildFragments(binaryExpression)) {
						binaryExpression.left.traverse(this, scope);
						this.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNameAND));
						binaryExpression.right.traverse(this, scope);
					}
					return false;
				default:
					this.addRealFragment(binaryExpression);
			}
		}
		return false;
	}

	public boolean visit(CombinedBinaryExpression combinedBinaryExpression, BlockScope scope) {
		// keep implementation in sync with BinaryExpression#resolveType
		if (combinedBinaryExpression.referencesTable == null) {
			this.addRealFragment(combinedBinaryExpression.left);
			this.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNamePLUS));
			this.addRealFragment(combinedBinaryExpression.right);
			return false;
		}
		BinaryExpression cursor = combinedBinaryExpression.referencesTable[0];
		if (cursor.left instanceof CombinedBinaryExpression) {
			this.visit((CombinedBinaryExpression) cursor.left, scope);
		} else {
			this.addRealFragment(cursor.left);
		}
		for (int i = 0, end = combinedBinaryExpression.arity; i < end; i ++) {
			this.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNamePLUS));
			this.addRealFragment(combinedBinaryExpression.referencesTable[i].right);
		}
		this.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNamePLUS));
		this.addRealFragment(combinedBinaryExpression.right);
		return false;
	}

	public boolean visit(
		ClassLiteralAccess classLiteralAccess,
		BlockScope scope) {
			this.addRealFragment(classLiteralAccess);
			return false;
	}

	public boolean visit(
		CompoundAssignment compoundAssignment,
		BlockScope scope) {
			this.addRealFragment(compoundAssignment);
			return false;
	}

	public boolean visit(
		ConditionalExpression conditionalExpression,
		BlockScope scope) {
			this.addRealFragment(conditionalExpression);
			return false;
	}

	public boolean visit(DoubleLiteral doubleLiteral, BlockScope scope) {
		this.addSmallFragment(doubleLiteral);
		return false;
	}

	public boolean visit(EqualExpression equalExpression, BlockScope scope) {
		this.addRealFragment(equalExpression);
		return false;
	}

	public boolean visit(
		ExtendedStringLiteral extendedStringLiteral,
		BlockScope scope) {
			this.addRealFragment(extendedStringLiteral);
			return false;
	}

	public boolean visit(FalseLiteral falseLiteral, BlockScope scope) {
		this.addSmallFragment(falseLiteral);
		return false;
	}

	public boolean visit(FieldReference fieldReference, BlockScope scope) {
		this.addRealFragment(fieldReference);
		return false;
	}


	public boolean visit(ObjectLiteral literal, BlockScope scope) {
		this.addRealFragment(literal);
		return false;
	}

	public boolean visit(UndefinedLiteral undefined, BlockScope scope) {
		this.addSmallFragment(undefined);
		return false;
	}

	public boolean visit(FunctionExpression functionExpression, BlockScope scope) {
		this.addRealFragment(functionExpression);
		return false;
	}

	public boolean visit(
		InstanceOfExpression instanceOfExpression,
		BlockScope scope) {
			this.addRealFragment(instanceOfExpression);
			return false;
	}

	public boolean visit(IntLiteral intLiteral, BlockScope scope) {
		this.addSmallFragment(intLiteral);
		return false;
	}

	public boolean visit(
		ListExpression listExpression,
		BlockScope scope) {
			this.addRealFragment(listExpression);
			return false;
	}

	public boolean visit(MessageSend messageSend, BlockScope scope) {
		this.addRealFragment(messageSend);
		return false;
	}

	public boolean visit(StringLiteralConcatenation stringLiteral, BlockScope scope) {
		if (((stringLiteral.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT) != 0) {
			addRealFragment(stringLiteral);
			return false;
		} else {
			for (int i = 0, max = stringLiteral.counter; i < max; i++) {
				this.addRealFragment(stringLiteral.literals[i]);
				if (i < max - 1) {
					this.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNamePLUS));
				}
			}
			return false;
		}
	}

	public boolean visit(NullLiteral nullLiteral, BlockScope scope) {
		this.addRealFragment(nullLiteral);
		return false;
	}

	public boolean visit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		if (((or_or_Expression.bits & ASTNode.ParenthesizedMASK) >> ASTNode.ParenthesizedSHIFT) != 0) {
			addRealFragment(or_or_Expression);
		} else {
			or_or_Expression.left.traverse(this, scope);
			this.operatorsList.add(Integer.valueOf(TerminalTokens.TokenNameOR_OR));
			or_or_Expression.right.traverse(this, scope);
		}
		return false;
	}

	public boolean visit(
		PostfixExpression postfixExpression,
		BlockScope scope) {
			this.addRealFragment(postfixExpression);
			return false;
	}

	public boolean visit(PrefixExpression prefixExpression, BlockScope scope) {
		this.addRealFragment(prefixExpression);
		return false;
	}

	public boolean visit(RegExLiteral regexLiteral, BlockScope scope) {
		this.addSmallFragment(regexLiteral);
		return false;
	}


	public boolean visit(
		QualifiedAllocationExpression qualifiedAllocationExpression,
		BlockScope scope) {
			this.addRealFragment(qualifiedAllocationExpression);
			return false;
	}
	public boolean visit(
		QualifiedNameReference qualifiedNameReference,
		BlockScope scope) {
			this.addRealFragment(qualifiedNameReference);
			return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedThisReference, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(
			QualifiedThisReference qualifiedThisReference,
			BlockScope scope) {
		this.addRealFragment(qualifiedThisReference);
		return false;
	}

	public boolean visit(
		SingleNameReference singleNameReference,
		BlockScope scope) {
			this.addRealFragment(singleNameReference);
			return false;
	}

	public boolean visit(StringLiteral stringLiteral, BlockScope scope) {
		this.addRealFragment(stringLiteral);
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.SuperReference, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
	 */
	public boolean visit(SuperReference superReference, BlockScope scope) {
		this.addRealFragment(superReference);
		return false;
	}

	public boolean visit(ThisReference thisReference, BlockScope scope) {
		this.addRealFragment(thisReference);
		return false;
	}

	public boolean visit(TrueLiteral trueLiteral, BlockScope scope) {
		this.addSmallFragment(trueLiteral);
		return false;
	}

	public boolean visit(UnaryExpression unaryExpression, BlockScope scope) {
		this.addRealFragment(unaryExpression);
		return false;
	}

	public int size() {
		return this.fragmentsList.size();
	}
}
