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
package org.eclipse.wst.jsdt.internal.corext.refactoring.code;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Selection;
import org.eclipse.wst.jsdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

public class CodeRefactoringUtil {

    public static RefactoringStatus checkMethodSyntaxErrors(int selectionStart, int selectionLength, JavaScriptUnit cuNode, String invalidSelectionMessage){
		SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(selectionStart, selectionLength), true);
		cuNode.accept(analyzer);
		ASTNode coveringNode= analyzer.getLastCoveringNode();
		if (! (coveringNode instanceof Block) || ! (coveringNode.getParent() instanceof FunctionDeclaration))
			return RefactoringStatus.createFatalErrorStatus(invalidSelectionMessage); 
		if (ASTNodes.getMessages(coveringNode, ASTNodes.NODE_ONLY).length == 0)
			return RefactoringStatus.createFatalErrorStatus(invalidSelectionMessage); 

		FunctionDeclaration methodDecl= (FunctionDeclaration)coveringNode.getParent();
		String[] keys= {methodDecl.getName().getIdentifier()};
		String message= Messages.format(RefactoringCoreMessages.CodeRefactoringUtil_error_message, keys); 
		return RefactoringStatus.createFatalErrorStatus(message);	
	}
	
	public static int getIndentationLevel(ASTNode node, IJavaScriptUnit unit) throws CoreException {
		IPath fullPath= unit.getCorrespondingResource().getFullPath();
		try{
			FileBuffers.getTextFileBufferManager().connect(fullPath, LocationKind.IFILE, new NullProgressMonitor());
			ITextFileBuffer buffer= FileBuffers.getTextFileBufferManager().getTextFileBuffer(fullPath, LocationKind.IFILE);
			try {
				IRegion region= buffer.getDocument().getLineInformationOfOffset(node.getStartPosition());
				return Strings.computeIndentUnits(buffer.getDocument().get(region.getOffset(), region.getLength()), unit.getJavaScriptProject());
			} catch (BadLocationException exception) {
				JavaScriptPlugin.log(exception);
			}
			return 0;
		} finally {
			FileBuffers.getTextFileBufferManager().disconnect(fullPath, LocationKind.IFILE, new NullProgressMonitor());
		}
	}	

    private CodeRefactoringUtil() {}
}
