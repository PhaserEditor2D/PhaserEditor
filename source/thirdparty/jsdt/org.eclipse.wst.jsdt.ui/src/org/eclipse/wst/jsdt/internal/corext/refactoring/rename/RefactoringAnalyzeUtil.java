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
package org.eclipse.wst.jsdt.internal.corext.refactoring.rename;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.text.IRegion;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.internal.corext.SourceRange;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStringStatusContext;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;

public class RefactoringAnalyzeUtil {
	
	private RefactoringAnalyzeUtil() {
		//no instances
	}
	
	public static IRegion[] getNewRanges(TextEdit[] edits, TextChange change){
		IRegion[] result= new IRegion[edits.length];
		for (int i= 0; i < edits.length; i++) {
			result[i]= RefactoringAnalyzeUtil.getNewTextRange(edits[i], change);
		}
		return result;
	}

	public static RefactoringStatus reportProblemNodes(String modifiedWorkingCopySource, SimpleName[] problemNodes) {
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < problemNodes.length; i++) {
			RefactoringStatusContext context= new JavaStringStatusContext(modifiedWorkingCopySource, new SourceRange(problemNodes[i]));
			result.addError(Messages.format(RefactoringCoreMessages.RefactoringAnalyzeUtil_name_collision, problemNodes[i].getIdentifier()), context); 
		}
		return result;
	}
	

	public static FunctionDeclaration getMethodDeclaration(TextEdit edit, TextChange change, JavaScriptUnit cuNode){
		ASTNode decl= RefactoringAnalyzeUtil.findSimpleNameNode(RefactoringAnalyzeUtil.getNewTextRange(edit, change), cuNode);
		return ((FunctionDeclaration)ASTNodes.getParent(decl, FunctionDeclaration.class));
	}

	public static Block getBlock(TextEdit edit, TextChange change, JavaScriptUnit cuNode){
		ASTNode decl= RefactoringAnalyzeUtil.findSimpleNameNode(RefactoringAnalyzeUtil.getNewTextRange(edit, change), cuNode);
		return ((Block)ASTNodes.getParent(decl, Block.class));
	}
	
	public static IProblem[] getIntroducedCompileProblems(JavaScriptUnit newCUNode, JavaScriptUnit oldCuNode) {
		Set subResult= new HashSet();				
		Set oldProblems= getOldProblems(oldCuNode);
		IProblem[] newProblems= ASTNodes.getProblems(newCUNode, ASTNodes.INCLUDE_ALL_PARENTS, ASTNodes.PROBLEMS);
		for (int i= 0; i < newProblems.length; i++) {
			IProblem correspondingOld= findCorrespondingProblem(oldProblems, newProblems[i]);
			if (correspondingOld == null)
				subResult.add(newProblems[i]);
		}
		return (IProblem[]) subResult.toArray(new IProblem[subResult.size()]);
	}
	
	public static IRegion getNewTextRange(TextEdit edit, TextChange change){
		return change.getPreviewEdit(edit).getRegion();
	}
	
	private static IProblem findCorrespondingProblem(Set oldProblems, IProblem iProblem) {
		for (Iterator iter= oldProblems.iterator(); iter.hasNext();) {
			IProblem oldProblem= (IProblem) iter.next();
			if (isCorresponding(oldProblem, iProblem))
				return oldProblem;
		}
		return null;
	}
	
	private static boolean isCorresponding(IProblem oldProblem, IProblem iProblem) {
		if (oldProblem.getID() != iProblem.getID())		
			return false;
		if (! oldProblem.getMessage().equals(iProblem.getMessage()))	
			return false;
		return true;
	}

	private static SimpleName getSimpleName(ASTNode node){
		if (node instanceof SimpleName)
			return (SimpleName)node;
		if (node instanceof VariableDeclaration)
			return ((VariableDeclaration)node).getName();
		return null;	
	}

	private static SimpleName findSimpleNameNode(IRegion range, JavaScriptUnit cuNode) {
		ASTNode node= NodeFinder.perform(cuNode, range.getOffset(), range.getLength());
		return getSimpleName(node);
	}

	private static Set getOldProblems(JavaScriptUnit oldCuNode) {
		return new HashSet(Arrays.asList(ASTNodes.getProblems(oldCuNode, ASTNodes.INCLUDE_ALL_PARENTS, ASTNodes.PROBLEMS)));
	}
}
