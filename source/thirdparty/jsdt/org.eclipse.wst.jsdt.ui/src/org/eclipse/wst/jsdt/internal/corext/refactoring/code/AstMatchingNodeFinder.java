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
package org.eclipse.wst.jsdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.wst.jsdt.core.dom.ASTMatcher;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ArrayAccess;
import org.eclipse.wst.jsdt.core.dom.ArrayCreation;
import org.eclipse.wst.jsdt.core.dom.ArrayInitializer;
import org.eclipse.wst.jsdt.core.dom.ArrayType;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BlockComment;
import org.eclipse.wst.jsdt.core.dom.BooleanLiteral;
import org.eclipse.wst.jsdt.core.dom.BreakStatement;
import org.eclipse.wst.jsdt.core.dom.CatchClause;
import org.eclipse.wst.jsdt.core.dom.CharacterLiteral;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.ConditionalExpression;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.ContinueStatement;
import org.eclipse.wst.jsdt.core.dom.DoStatement;
import org.eclipse.wst.jsdt.core.dom.EmptyStatement;
import org.eclipse.wst.jsdt.core.dom.EnhancedForStatement;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.ForInStatement;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionExpression;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.FunctionRef;
import org.eclipse.wst.jsdt.core.dom.FunctionRefParameter;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.InfixExpression;
import org.eclipse.wst.jsdt.core.dom.Initializer;
import org.eclipse.wst.jsdt.core.dom.InstanceofExpression;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.LabeledStatement;
import org.eclipse.wst.jsdt.core.dom.LineComment;
import org.eclipse.wst.jsdt.core.dom.ListExpression;
import org.eclipse.wst.jsdt.core.dom.MemberRef;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.NullLiteral;
import org.eclipse.wst.jsdt.core.dom.NumberLiteral;
import org.eclipse.wst.jsdt.core.dom.ObjectLiteral;
import org.eclipse.wst.jsdt.core.dom.ObjectLiteralField;
import org.eclipse.wst.jsdt.core.dom.PackageDeclaration;
import org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression;
import org.eclipse.wst.jsdt.core.dom.PostfixExpression;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.QualifiedType;
import org.eclipse.wst.jsdt.core.dom.RegularExpressionLiteral;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SimpleType;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.StringLiteral;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.SuperFieldAccess;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.SwitchCase;
import org.eclipse.wst.jsdt.core.dom.SwitchStatement;
import org.eclipse.wst.jsdt.core.dom.TagElement;
import org.eclipse.wst.jsdt.core.dom.TextElement;
import org.eclipse.wst.jsdt.core.dom.ThisExpression;
import org.eclipse.wst.jsdt.core.dom.ThrowStatement;
import org.eclipse.wst.jsdt.core.dom.TryStatement;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.TypeDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.TypeLiteral;
import org.eclipse.wst.jsdt.core.dom.UndefinedLiteral;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.WhileStatement;
import org.eclipse.wst.jsdt.core.dom.WithStatement;
import org.eclipse.wst.jsdt.internal.corext.dom.JdtASTMatcher;

class AstMatchingNodeFinder {
	
	private AstMatchingNodeFinder(){
	}
	
	public static ASTNode[] findMatchingNodes(ASTNode scope, ASTNode node){
		Visitor visitor= new Visitor(node);
		scope.accept(visitor);
		return visitor.getMatchingNodes();
	}
	
	private static class Visitor extends ASTVisitor{
		
		Collection fFound;
		ASTMatcher fMatcher;
		ASTNode fNodeToMatch;
		
		Visitor(ASTNode nodeToMatch){
			fNodeToMatch= nodeToMatch;
			fFound= new ArrayList();
			fMatcher= new JdtASTMatcher();
		}
		
		ASTNode[] getMatchingNodes(){
			return (ASTNode[]) fFound.toArray(new ASTNode[fFound.size()]);
		}
		
		private boolean matches(ASTNode node){
			fFound.add(node);
			return false;
		}
		
		public boolean visit(AnonymousClassDeclaration node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(ArrayAccess node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(ArrayCreation node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(ArrayInitializer node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(ArrayType node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(Assignment node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(Block node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(BooleanLiteral node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(BreakStatement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(FunctionExpression node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(ObjectLiteral node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(ObjectLiteralField node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}
 
		public boolean visit(CatchClause node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(CharacterLiteral node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}
		public boolean visit(RegularExpressionLiteral node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(ClassInstanceCreation node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(JavaScriptUnit node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(ConditionalExpression node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(ConstructorInvocation node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(ContinueStatement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(DoStatement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(EmptyStatement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(ExpressionStatement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(FieldAccess node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(FieldDeclaration node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(ForStatement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(ForInStatement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(IfStatement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(ImportDeclaration node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(InfixExpression node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(Initializer node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

        public boolean visit(InstanceofExpression node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
        }

		public boolean visit(JSdoc node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(LabeledStatement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}


		public boolean visit(ListExpression node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}
		public boolean visit(FunctionDeclaration node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(FunctionInvocation node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}


		public boolean visit(NullLiteral node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(UndefinedLiteral node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(NumberLiteral node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(PackageDeclaration node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(ParenthesizedExpression node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(PostfixExpression node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(PrefixExpression node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(PrimitiveType node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(QualifiedName node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(ReturnStatement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(SimpleName node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(SimpleType node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(SingleVariableDeclaration node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(StringLiteral node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(SuperConstructorInvocation node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(SuperFieldAccess node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(SuperMethodInvocation node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(SwitchCase node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(SwitchStatement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}


		public boolean visit(ThisExpression node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(ThrowStatement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(TryStatement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(TypeDeclaration node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(TypeDeclarationStatement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(TypeLiteral node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(VariableDeclarationExpression node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(VariableDeclarationFragment node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(VariableDeclarationStatement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(WhileStatement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}


		public boolean visit(WithStatement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(BlockComment node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(EnhancedForStatement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(LineComment node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(MemberRef node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(FunctionRef node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(FunctionRefParameter node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(Modifier node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(QualifiedType node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(TagElement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}

		public boolean visit(TextElement node) {
			if (node.subtreeMatch(fMatcher, fNodeToMatch))
				return matches(node);
			return super.visit(node);
		}
	}
}
