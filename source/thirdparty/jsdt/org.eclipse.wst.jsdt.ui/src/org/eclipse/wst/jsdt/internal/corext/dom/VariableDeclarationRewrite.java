/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.dom;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;

/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class VariableDeclarationRewrite {

	public static void rewriteModifiers(final SingleVariableDeclaration declarationNode, final int includedModifiers, final int excludedModifiers, final ASTRewrite rewrite, final TextEditGroup group) {
		ModifierRewrite listRewrite= ModifierRewrite.create(rewrite, declarationNode);
		listRewrite.setModifiers(includedModifiers, excludedModifiers, group);
	}
	
	public static void rewriteModifiers(final VariableDeclarationExpression declarationNode, final int includedModifiers, final int excludedModifiers, final ASTRewrite rewrite, final TextEditGroup group) {
		ModifierRewrite listRewrite= ModifierRewrite.create(rewrite, declarationNode);
		listRewrite.setModifiers(includedModifiers, excludedModifiers, group);	
	}
	
	public static void rewriteModifiers(final FieldDeclaration declarationNode, final VariableDeclarationFragment[] toChange, final int includedModifiers, final int excludedModifiers, final ASTRewrite rewrite, final TextEditGroup group) {
		final List fragmentsToChange= Arrays.asList(toChange);
		AST ast= declarationNode.getAST();
		
		List fragments= declarationNode.fragments();
		Iterator iter= fragments.iterator();
		
		ListRewrite blockRewrite;
		if (declarationNode.getParent() instanceof AbstractTypeDeclaration) {
			blockRewrite= rewrite.getListRewrite(declarationNode.getParent(), ((AbstractTypeDeclaration)declarationNode.getParent()).getBodyDeclarationsProperty());
		} else {
			blockRewrite= rewrite.getListRewrite(declarationNode.getParent(), AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);
		}
		
		VariableDeclarationFragment lastFragment= (VariableDeclarationFragment)iter.next();
		ASTNode lastStatement= declarationNode;
		
		int orginalModifiers= declarationNode.getModifiers();
		if (fragmentsToChange.contains(lastFragment)) {
			ListRewrite modifierRewrite= rewrite.getListRewrite(declarationNode, FieldDeclaration.MODIFIERS2_PROPERTY);
			for (Iterator iterator= declarationNode.modifiers().iterator(); iterator.hasNext();) {
				ASTNode node= (ASTNode)iterator.next();
				modifierRewrite.remove(node, group);
			}
			List newModifiers= ast.newModifiers((orginalModifiers & ~excludedModifiers) | includedModifiers);
			for (Iterator iterator= newModifiers.iterator(); iterator.hasNext();) {
				modifierRewrite.insertLast((ASTNode)iterator.next(), group);
			}
		}
		
		ListRewrite fragmentsRewrite= null;
		while (iter.hasNext()) {
			VariableDeclarationFragment currentFragment= (VariableDeclarationFragment)iter.next();
			
			if (fragmentsToChange.contains(lastFragment) != fragmentsToChange.contains(currentFragment)) {
		
					FieldDeclaration newStatement= ast.newFieldDeclaration((VariableDeclarationFragment)rewrite.createMoveTarget(currentFragment));
					newStatement.setType((Type)rewrite.createCopyTarget(declarationNode.getType()));
					if (fragmentsToChange.contains(currentFragment)) {
						newStatement.modifiers().addAll(ast.newModifiers((orginalModifiers & ~excludedModifiers) | includedModifiers));
					} else {
						newStatement.modifiers().addAll(ast.newModifiers(orginalModifiers));
					}
					blockRewrite.insertAfter(newStatement, lastStatement, group);
					
					fragmentsRewrite= rewrite.getListRewrite(newStatement, FieldDeclaration.FRAGMENTS_PROPERTY);
					lastStatement= newStatement;								
			} else if (fragmentsRewrite != null) {
				ASTNode fragment0= rewrite.createMoveTarget(currentFragment);
				fragmentsRewrite.insertLast(fragment0, group);
			}
			lastFragment= currentFragment;
		}
	}
	
	public static void rewriteModifiers(final VariableDeclarationStatement declarationNode, final VariableDeclarationFragment[] toChange, final int includedModifiers, final int excludedModifiers, ASTRewrite rewrite, final TextEditGroup group) {
		final List fragmentsToChange= Arrays.asList(toChange);
		AST ast= declarationNode.getAST();
		
		List fragments= declarationNode.fragments();
		Iterator iter= fragments.iterator();
		
		ListRewrite blockRewrite= rewrite.getListRewrite(declarationNode.getParent(), Block.STATEMENTS_PROPERTY);
		
		VariableDeclarationFragment lastFragment= (VariableDeclarationFragment)iter.next();
		ASTNode lastStatement= declarationNode;
		
		int orginalModifiers= declarationNode.getModifiers();
		if (fragmentsToChange.contains(lastFragment)) {
			ListRewrite modifierRewrite= rewrite.getListRewrite(declarationNode, VariableDeclarationStatement.MODIFIERS2_PROPERTY);
			for (Iterator iterator= declarationNode.modifiers().iterator(); iterator.hasNext();) {
				ASTNode node= (ASTNode)iterator.next();
				modifierRewrite.remove(node, group);
			}
			List newModifiers= ast.newModifiers((orginalModifiers & ~excludedModifiers) | includedModifiers);
			for (Iterator iterator= newModifiers.iterator(); iterator.hasNext();) {
				modifierRewrite.insertLast((ASTNode)iterator.next(), group);
			}
		}
		
		ListRewrite fragmentsRewrite= null;
		while (iter.hasNext()) {
			VariableDeclarationFragment currentFragment= (VariableDeclarationFragment)iter.next();
			
			if (fragmentsToChange.contains(lastFragment) != fragmentsToChange.contains(currentFragment)) {
		
					VariableDeclarationStatement newStatement= ast.newVariableDeclarationStatement((VariableDeclarationFragment)rewrite.createMoveTarget(currentFragment));
					newStatement.setType((Type)rewrite.createCopyTarget(declarationNode.getType()));
					if (fragmentsToChange.contains(currentFragment)) {
						newStatement.modifiers().addAll(ast.newModifiers((orginalModifiers & ~excludedModifiers) | includedModifiers));
					} else {
						newStatement.modifiers().addAll(ast.newModifiers(orginalModifiers));
					}
					blockRewrite.insertAfter(newStatement, lastStatement, group);
					
					fragmentsRewrite= rewrite.getListRewrite(newStatement, VariableDeclarationStatement.FRAGMENTS_PROPERTY);
					lastStatement= newStatement;								
			} else if (fragmentsRewrite != null) {
				ASTNode fragment0= rewrite.createMoveTarget(currentFragment);
				fragmentsRewrite.insertLast(fragment0, group);
			}
			lastFragment= currentFragment;
		}
	}
}
