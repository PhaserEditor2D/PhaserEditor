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
package org.eclipse.wst.jsdt.internal.corext.dom;

import java.util.List;

import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
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
public class ModifierRewrite {
	
	public static final int VISIBILITY_MODIFIERS= Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED;
	
	private ListRewrite fModifierRewrite;
//	private AST fAst;


	public static ModifierRewrite create(ASTRewrite rewrite, ASTNode declNode) {
		return new ModifierRewrite(rewrite, declNode);
	}

	private ModifierRewrite(ASTRewrite rewrite, ASTNode declNode) {
		fModifierRewrite= evaluateListRewrite(rewrite, declNode);
//		fAst= declNode.getAST();
	}

	private ListRewrite evaluateListRewrite(ASTRewrite rewrite, ASTNode declNode) {
		switch (declNode.getNodeType()) {
			case ASTNode.FUNCTION_DECLARATION:
				return rewrite.getListRewrite(declNode, FunctionDeclaration.MODIFIERS2_PROPERTY);
			case ASTNode.FIELD_DECLARATION:
				return rewrite.getListRewrite(declNode, FieldDeclaration.MODIFIERS2_PROPERTY);
			case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
				return rewrite.getListRewrite(declNode, VariableDeclarationExpression.MODIFIERS2_PROPERTY);
			case ASTNode.VARIABLE_DECLARATION_STATEMENT:
				return rewrite.getListRewrite(declNode, VariableDeclarationStatement.MODIFIERS2_PROPERTY);
			case ASTNode.SINGLE_VARIABLE_DECLARATION:
				return rewrite.getListRewrite(declNode, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
			case ASTNode.TYPE_DECLARATION:
				return rewrite.getListRewrite(declNode, TypeDeclaration.MODIFIERS2_PROPERTY);
			default:
				throw new IllegalArgumentException("node has no modifiers: " + declNode.getClass().getName()); //$NON-NLS-1$
		}
	}

	public ListRewrite getModifierRewrite() {
		return fModifierRewrite;
	}
	
	public void setModifiers(int modfiers, TextEditGroup editGroup) {
		internalSetModifiers(modfiers, -1, editGroup);
	}
	
	public void setModifiers(int included, int excluded, TextEditGroup editGroup) {
		internalSetModifiers(included, included | excluded, editGroup);
	}
	
	public void setVisibility(int visibilityFlags, TextEditGroup editGroup) {
		internalSetModifiers(visibilityFlags, VISIBILITY_MODIFIERS, editGroup);
	}

	public void copyAllModifiers(ASTNode otherDecl, TextEditGroup editGroup) {
		ListRewrite modifierList= evaluateListRewrite(fModifierRewrite.getASTRewrite(), otherDecl);
		List originalList= modifierList.getOriginalList();
		if (originalList.isEmpty()) {
			return;
		}
		
		ASTNode copy= modifierList.createCopyTarget((ASTNode) originalList.get(0), (ASTNode) originalList.get(originalList.size() - 1));
		if (copy != null) {
			fModifierRewrite.insertLast(copy, editGroup);
		}
	}
	
	private void internalSetModifiers(int modfiers, int consideredFlags, TextEditGroup editGroup) {
		// remove modifiers
//		int newModifiers= modfiers & consideredFlags;
		
//		List originalList= fModifierRewrite.getOriginalList();
//		for (int i= 0; i < originalList.size(); i++) {
//			ASTNode curr= (ASTNode) originalList.get(i);
//			if (curr instanceof Modifier) {
//				int flag= ((Modifier)curr).getKeyword().toFlagValue();
//				if ((consideredFlags & flag) != 0) {
//					if ((newModifiers & flag) == 0) {
//						fModifierRewrite.remove(curr, editGroup);
//					}
//					newModifiers &= ~flag;
//				}
//			}
//		}
//		
//		// find last annotation
//		IExtendedModifier lastAnnotation= null;
//		List extendedList= fModifierRewrite.getRewrittenList();
//		for (int i= 0; i < extendedList.size(); i++) {
//			IExtendedModifier curr= (IExtendedModifier) extendedList.get(i);
//			if (curr.isAnnotation())
//				lastAnnotation= curr;
//		}
//		
//		// add modifiers
//		List newNodes= ASTNodeFactory.newModifiers(fAst, newModifiers);
//		for (int i= 0; i < newNodes.size(); i++) {
//			Modifier curr= (Modifier) newNodes.get(i);
//			if ((curr.getKeyword().toFlagValue() & VISIBILITY_MODIFIERS) != 0) {
//				if (lastAnnotation != null)
//					fModifierRewrite.insertAfter(curr, (ASTNode) lastAnnotation, editGroup);
//				else
//					fModifierRewrite.insertFirst(curr, editGroup);
//			} else {
//				fModifierRewrite.insertLast(curr, editGroup);
//			}
//		}
	}
}
