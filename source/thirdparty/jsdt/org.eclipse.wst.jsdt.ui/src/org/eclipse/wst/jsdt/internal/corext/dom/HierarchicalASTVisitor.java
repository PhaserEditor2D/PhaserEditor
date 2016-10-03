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
package org.eclipse.wst.jsdt.internal.corext.dom;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ArrayAccess;
import org.eclipse.wst.jsdt.core.dom.ArrayCreation;
import org.eclipse.wst.jsdt.core.dom.ArrayInitializer;
import org.eclipse.wst.jsdt.core.dom.ArrayType;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BlockComment;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.BooleanLiteral;
import org.eclipse.wst.jsdt.core.dom.BreakStatement;
import org.eclipse.wst.jsdt.core.dom.CatchClause;
import org.eclipse.wst.jsdt.core.dom.CharacterLiteral;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.Comment;
import org.eclipse.wst.jsdt.core.dom.ConditionalExpression;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.ContinueStatement;
import org.eclipse.wst.jsdt.core.dom.DoStatement;
import org.eclipse.wst.jsdt.core.dom.EmptyStatement;
import org.eclipse.wst.jsdt.core.dom.EnhancedForStatement;
import org.eclipse.wst.jsdt.core.dom.Expression;
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
import org.eclipse.wst.jsdt.core.dom.Name;
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
import org.eclipse.wst.jsdt.core.dom.Statement;
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
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.TypeDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.TypeLiteral;
import org.eclipse.wst.jsdt.core.dom.UndefinedLiteral;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.WhileStatement;
import org.eclipse.wst.jsdt.core.dom.WithStatement;

/**
 * <p>This class provides a convenient behaviour-only 
 * extension mechanism for the ASTNode hierarchy.
 * If you feel like you would like to add a method to
 * the ASTNode hierarchy (or a subtree of the hierarchy),
 * and you want to have different implementations
 * of it at different points in the hierarchy,
 * simply create a HierarchicalASTVisitor representing
 * the new method and all its implementations,
 * locating each implementation within the right
 * visit(XX) method.  If you wanted to add a method implementation to abstract
 * class Foo, an ASTNode descendant, put your implementation in visit(Foo). 
 * This class will provide appropriate dispatch, just as if the method
 * implementations had been added to the ASTNode hierarchy.
 * </p>
 * 
 * <p><b>Details:<b></p>
 * 
 * <p>This class has a visit(XX node) method for every for every 
 * class (concrete or abstract) XX in the ASTNode hierarchy. In this class'
 * default implementations of these methods, the method corresponding to a given
 * ASTNode descendant class will call (and return the return value of) the
 * visit(YY) method for it's superclass YY, with the exception of the
 * visit(ASTNode) method which simply returns true, since ASTNode doesn't have a
 * superclass that is within the ASTNode hierarchy.
 * </p>
 * 
 * <p>Because of this organization, when visit(XX) methods  are overridden in a
 * subclass, and the visitor is applied to a node, only the most specialized
 * overridden method implementation for the node's type will be called, unless
 * this most specialized method calls other visit methods (this is discouraged)
 * or, (preferably) calls super.visit(XX node), (the reference type of the
 * parameter must be XX) which will invoke this class' implementation of the
 * method, which will, in turn, invoke the visit(YY) method corresponding to the
 * superclass, YY.
 * </p>
 * 
 * <p>Thus, the dispatching behaviour achieved when 
 * HierarchicalASTVisitors' visit(XX) methods, corresponding to a particular
 * concrete or abstract ASTNode descendant class, are overridden is exactly
 * analogous to the dispatching behaviour obtained when method implementations
 * are added to the same ASTNode descendant classes.
 * </p>
 */
/*

 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public abstract class HierarchicalASTVisitor extends ASTVisitor {
//TODO: check callers for handling of comments
	
//---- Begin ASTNode Hierarchy -------------------------------------
public boolean visit(ASTNode node) {
	return true;
}
public void endVisit(ASTNode node) {
	// do nothing
}

public boolean visit(AnonymousClassDeclaration node) {
	return visit((ASTNode) node);
}
public void endVisit(AnonymousClassDeclaration node) {
	endVisit((ASTNode) node);
}

//---- Begin BodyDeclaration Hierarchy ---------------------------
public boolean visit(BodyDeclaration node) {
	return visit((ASTNode) node);
}
public void endVisit(BodyDeclaration node) {
	endVisit((ASTNode) node);
}

	//---- Begin AbstractTypeDeclaration Hierarchy ---------------------------
	public boolean visit(AbstractTypeDeclaration node) {
		return visit((BodyDeclaration) node);
	}
	public void endVisit(AbstractTypeDeclaration node) {
		endVisit((BodyDeclaration) node);
	}
	
		
		public boolean visit(TypeDeclaration node) {
			return visit((AbstractTypeDeclaration) node);
		}
		public void endVisit(TypeDeclaration node) {
			endVisit((AbstractTypeDeclaration) node);
		}
	//---- End AbstractTypeDeclaration Hierarchy ---------------------------
	

	
	public boolean visit(FieldDeclaration node) {
		return visit((BodyDeclaration) node);
	}
	public void endVisit(FieldDeclaration node) {
		endVisit((BodyDeclaration) node);
	}
	
	public boolean visit(Initializer node) {
		return visit((BodyDeclaration) node);
	}
	public void endVisit(Initializer node) {
		endVisit((BodyDeclaration) node);
	}
	
	public boolean visit(FunctionDeclaration node) {
		return visit((BodyDeclaration) node);
	}
	public void endVisit(FunctionDeclaration node) {
		endVisit((BodyDeclaration) node);
	}
	
//---- End BodyDeclaration Hierarchy -----------------------------

public boolean visit(CatchClause node) {
	return visit((ASTNode) node);
}
public void endVisit(CatchClause node) {
	endVisit((ASTNode) node);
}

//---- Begin Comment Hierarchy ----------------------------------
public boolean visit(Comment node) {
	return visit((ASTNode) node);
}
public void endVisit(Comment node) {
	endVisit((ASTNode) node);
}

	public boolean visit(BlockComment node) {
		return visit((Comment) node);
	}
	public void endVisit(BlockComment node) {
		endVisit((Comment) node);
	}
	
	public boolean visit(JSdoc node) {
		return visit((Comment) node);
	}
	public void endVisit(JSdoc node) {
		endVisit((Comment) node);
	}
	
	public boolean visit(LineComment node) {
		return visit((Comment) node);
	}
	public void endVisit(LineComment node) {
		endVisit((Comment) node);
	}
	
	public boolean visit(ListExpression node) {
		return visit((Expression) node);
	}
//---- End Comment Hierarchy -----------------------------

public boolean visit(JavaScriptUnit node) {
	return visit((ASTNode) node);
}
public void endVisit(JavaScriptUnit node) {
	endVisit((ASTNode) node);
}

//---- Begin Expression Hierarchy ----------------------------------
public boolean visit(Expression node) {
	return visit((ASTNode) node);
}
public void endVisit(Expression node) {
	endVisit((ASTNode) node);
}


	
	public boolean visit(ArrayAccess node) {
		return visit((Expression) node);
	}
	public void endVisit(ArrayAccess node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(ArrayCreation node) {
		return visit((Expression) node);
	}
	public void endVisit(ArrayCreation node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(ArrayInitializer node) {
		return visit((Expression) node);
	}
	public void endVisit(ArrayInitializer node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(Assignment node) {
		return visit((Expression) node);
	}
	public void endVisit(Assignment node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(BooleanLiteral node) {
		return visit((Expression) node);
	}
	public void endVisit(BooleanLiteral node) {
		endVisit((Expression) node);
	}
	
 
	public boolean visit(FunctionExpression node) {
		return visit((Expression) node);
	}
	public void endVisit(FunctionExpression node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(ObjectLiteral node) {
		return visit((Expression) node);
	}
	public void endVisit(ObjectLiteral node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(ObjectLiteralField node) {
		return visit((Expression) node);
	}
	public void endVisit(ObjectLiteralField node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(CharacterLiteral node) {
		return visit((Expression) node);
	}
	
	public void endVisit(CharacterLiteral node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(RegularExpressionLiteral node) {
		return visit((Expression) node);
	}
	
	public void endVisit(RegularExpressionLiteral node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(ClassInstanceCreation node) {
		return visit((Expression) node);
	}
	public void endVisit(ClassInstanceCreation node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(ConditionalExpression node) {
		return visit((Expression) node);
	}
	public void endVisit(ConditionalExpression node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(FieldAccess node) {
		return visit((Expression) node);
	}
	public void endVisit(FieldAccess node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(InfixExpression node) {
		return visit((Expression) node);
	}
	public void endVisit(InfixExpression node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(InstanceofExpression node) {
		return visit((Expression) node);
	}
	public void endVisit(InstanceofExpression node) {
		endVisit((Expression) node);
	}
	public void endVisit(ListExpression node) {
		endVisit((Expression) node);
	}
		public boolean visit(FunctionInvocation node) {
		return visit((Expression) node);
	}
	public void endVisit(FunctionInvocation node) {
		endVisit((Expression) node);
	}

	//---- Begin Name Hierarchy ----------------------------------
	public boolean visit(Name node) {
		return visit((Expression) node);
	}
	public void endVisit(Name node) {
		endVisit((Expression) node);
	}

		public boolean visit(QualifiedName node) {
			return visit((Name) node);
		}
		public void endVisit(QualifiedName node) {
			endVisit((Name) node);
		}
		
		public boolean visit(SimpleName node) {
			return visit((Name) node);
		}
		public void endVisit(SimpleName node) {
			endVisit((Name) node);
		}
		
	//---- End Name Hierarchy ------------------------------------

		public boolean visit(NullLiteral node) {
			return visit((Expression) node);
		}
		public void endVisit(NullLiteral node) {
			endVisit((Expression) node);
		}
		public boolean visit(UndefinedLiteral node) {
			return visit((Expression) node);
		}
		public void endVisit(UndefinedLiteral node) {
			endVisit((Expression) node);
		}
		
	public boolean visit(NumberLiteral node) {
		return visit((Expression) node);
	}
	public void endVisit(NumberLiteral node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(ParenthesizedExpression node) {
		return visit((Expression) node);
	}
	public void endVisit(ParenthesizedExpression node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(PostfixExpression node) {
		return visit((Expression) node);
	}
	public void endVisit(PostfixExpression node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(PrefixExpression node) {
		return visit((Expression) node);
	}
	public void endVisit(PrefixExpression node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(StringLiteral node) {
		return visit((Expression) node);
	}
	public void endVisit(StringLiteral node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(SuperFieldAccess node) {
		return visit((Expression) node);
	}
	public void endVisit(SuperFieldAccess node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(SuperMethodInvocation node) {
		return visit((Expression) node);
	}
	public void endVisit(SuperMethodInvocation node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(ThisExpression node) {
		return visit((Expression) node);
	}
	public void endVisit(ThisExpression node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(TypeLiteral node) {
		return visit((Expression) node);
	}
	public void endVisit(TypeLiteral node) {
		endVisit((Expression) node);
	}
	
	public boolean visit(VariableDeclarationExpression node) {
		return visit((Expression) node);
	}
	public void endVisit(VariableDeclarationExpression node) {
		endVisit((Expression) node);
	}

	//---- End Expression Hierarchy ----------------------------------

public boolean visit(ImportDeclaration node) {
	return visit((ASTNode) node);
}
public void endVisit(ImportDeclaration node) {
	endVisit((ASTNode) node);
}

public boolean visit(MemberRef node) {
	return visit((ASTNode) node);
}
public void endVisit(MemberRef node) {
	endVisit((ASTNode) node);
}


public boolean visit(FunctionRef node) {
	return visit((ASTNode) node);
}
public void endVisit(FunctionRef node) {
	endVisit((ASTNode) node);
}

public boolean visit(FunctionRefParameter node) {
	return visit((ASTNode) node);
}
public void endVisit(FunctionRefParameter node) {
	endVisit((ASTNode) node);
}

public boolean visit(Modifier node) {
	return visit((ASTNode) node);
}
public void endVisit(Modifier node) {
	endVisit((ASTNode) node);
}

public boolean visit(PackageDeclaration node) {
	return visit((ASTNode) node);
}
public void endVisit(PackageDeclaration node) {
	endVisit((ASTNode) node);
}

//---- Begin Statement Hierarchy --------------------------------- 
public boolean visit(Statement node) {
	return visit((ASTNode) node);
}
public void endVisit(Statement node) {
	endVisit((ASTNode) node);
}
	
	public boolean visit(Block node) {
		return visit((Statement) node);
	}
	public void endVisit(Block node) {
		endVisit((Statement) node);
	}
	
	public boolean visit(BreakStatement node) {
		return visit((Statement) node);
	}
	public void endVisit(BreakStatement node) {
		endVisit((Statement) node);
	}
	
	public boolean visit(ConstructorInvocation node) {
		return visit((Statement) node);
	}
	public void endVisit(ConstructorInvocation node) {
		endVisit((Statement) node);
	}
	
	public boolean visit(ContinueStatement node) {
		return visit((Statement) node);
	}
	public void endVisit(ContinueStatement node) {
		endVisit((Statement) node);
	}
	
	public boolean visit(DoStatement node) {
		return visit((Statement) node);
	}
	public void endVisit(DoStatement node) {
		endVisit((Statement) node);
	}
	
	public boolean visit(EmptyStatement node) {
		return visit((Statement) node);
	}
	public void endVisit(EmptyStatement node) {
		endVisit((Statement) node);
	}
	
	public boolean visit(EnhancedForStatement node) {
		return visit((Statement) node);
	}
	public void endVisit(EnhancedForStatement node) {
		endVisit((Statement) node);
	}
	
	public boolean visit(ExpressionStatement node) {
		return visit((Statement) node);
	}
	public void endVisit(ExpressionStatement node) {
		endVisit((Statement) node);
	}
	
	public boolean visit(ForStatement node) {
		return visit((Statement) node);
	}
	public void endVisit(ForStatement node) {
		endVisit((Statement) node);
	}
	
	public boolean visit(ForInStatement node) {
		return visit((Statement) node);
	}
	public void endVisit(ForInStatement node) {
		endVisit((Statement) node);
	}
	
	public boolean visit(IfStatement node) {
		return visit((Statement) node);
	}
	public void endVisit(IfStatement node) {
		endVisit((Statement) node);
	}
	
	public boolean visit(LabeledStatement node) {
		return visit((Statement) node);
	}
	public void endVisit(LabeledStatement node) {
		endVisit((Statement) node);
	}
	
	public boolean visit(ReturnStatement node) {
		return visit((Statement) node);
	}
	public void endVisit(ReturnStatement node) {
		endVisit((Statement) node);
	}
	
	public boolean visit(SuperConstructorInvocation node) {
		return visit((Statement) node);
	}
	public void endVisit(SuperConstructorInvocation node) {
		endVisit((Statement) node);
	}
	
	public boolean visit(SwitchCase node) {
		return visit((Statement) node);
	}
	public void endVisit(SwitchCase node) {
		endVisit((Statement) node);
	}
	
	public boolean visit(SwitchStatement node) {
		return visit((Statement) node);
	}
	public void endVisit(SwitchStatement node) {
		endVisit((Statement) node);
	}

	
	public boolean visit(ThrowStatement node) {
		return visit((Statement) node);
	}
	public void endVisit(ThrowStatement node) {
		endVisit((Statement) node);
	}
	
	public boolean visit(TryStatement node) {
		return visit((Statement) node);
	}
	public void endVisit(TryStatement node) {
		endVisit((Statement) node);
	}
	
	public boolean visit(TypeDeclarationStatement node) {
		return visit((Statement) node);
	}
	public void endVisit(TypeDeclarationStatement node) {
		endVisit((Statement) node);
	}
	
	public boolean visit(VariableDeclarationStatement node) {
		return visit((Statement) node);
	}
	public void endVisit(VariableDeclarationStatement node) {
		endVisit((Statement) node);
	}
	
	public boolean visit(WhileStatement node) {
		return visit((Statement) node);
	}
	public void endVisit(WhileStatement node) {
		endVisit((Statement) node);
	}
	
	
	public boolean visit(WithStatement node) {
		return visit((Statement) node);
	}
	public void endVisit(WithStatement node) {
		endVisit((Statement) node);
	}

//---- End Statement Hierarchy ----------------------------------  

public boolean visit(TagElement node) {
	return visit((ASTNode) node);
}
public void endVisit(TagElement node) {
	endVisit((ASTNode) node);
}

public boolean visit(TextElement node) {
	return visit((ASTNode) node);
}
public void endVisit(TextElement node) {
	endVisit((ASTNode) node);
}


//---- Begin Type Hierarchy -------------------------------------- 
public boolean visit(Type node) {
	return visit((ASTNode) node);
}
public void endVisit(Type node) {
	endVisit((ASTNode) node);
}

	public boolean visit(ArrayType node) {
		return visit((Type) node);
	}
	public void endVisit(ArrayType node) {
		endVisit((Type) node);
	}
	
	public boolean visit(PrimitiveType node) {
		return visit((Type) node);
	}
	public void endVisit(PrimitiveType node) {
		endVisit((Type) node);
	}
	
	public boolean visit(QualifiedType node) {
		return visit((Type) node);
	}
	public void endVisit(QualifiedType node) {
		endVisit((Type) node);
	}
	
	public boolean visit(SimpleType node) {
		return visit((Type) node);
	}
	public void endVisit(SimpleType node) {
		endVisit((Type) node);
	}

//---- Begin VariableDeclaration Hierarchy ---------------------------  
public boolean visit(VariableDeclaration node) {
	return visit((ASTNode) node);
}
public void endVisit(VariableDeclaration node) {
	endVisit((ASTNode) node);
}

	public boolean visit(SingleVariableDeclaration node) {
		return visit((VariableDeclaration) node);
	}
	public void endVisit(SingleVariableDeclaration node) {
		endVisit((VariableDeclaration) node);
	}

	public boolean visit(VariableDeclarationFragment node) {
		return visit((VariableDeclaration) node);
	}
	public void endVisit(VariableDeclarationFragment node) {
		endVisit((VariableDeclaration) node);
	}

//---- End VariableDeclaration Hierarchy ----------------------------- 
//---- End ASTNode Hierarchy -----------------------------------------
}
