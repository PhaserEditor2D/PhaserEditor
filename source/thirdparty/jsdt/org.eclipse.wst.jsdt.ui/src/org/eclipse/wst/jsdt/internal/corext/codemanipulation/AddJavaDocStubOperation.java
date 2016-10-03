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
package org.eclipse.wst.jsdt.internal.corext.codemanipulation;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.internal.corext.dom.TokenScanner;
import org.eclipse.wst.jsdt.internal.corext.util.MethodOverrideTester;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.corext.util.SuperTypeHierarchyCache;
import org.eclipse.wst.jsdt.internal.ui.JavaUIStatus;
import org.eclipse.wst.jsdt.ui.CodeGeneration;

/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class AddJavaDocStubOperation implements IWorkspaceRunnable {
	
	private IMember[] fMembers;
	
	public AddJavaDocStubOperation(IMember[] members) {
		super();
		fMembers= members;
	}

	private String createTypeComment(IType type, String lineDelimiter) throws CoreException {
		return CodeGeneration.getTypeComment(type.getJavaScriptUnit(), type.getTypeQualifiedName('.'), lineDelimiter);
	}		
	
	private String createMethodComment(IFunction meth, String lineDelimiter) throws CoreException {
		IType declaringType= meth.getDeclaringType();
		
		IFunction overridden= null;
		if (!meth.isConstructor() && declaringType!=null) {
			ITypeHierarchy hierarchy= SuperTypeHierarchyCache.getTypeHierarchy(declaringType);
			MethodOverrideTester tester= new MethodOverrideTester(declaringType, hierarchy);
			overridden= tester.findOverriddenMethod(meth, true);
		}
		return CodeGeneration.getMethodComment(meth, overridden, lineDelimiter);
	}
	
	private String createFieldComment(IField field, String lineDelimiter) throws JavaScriptModelException, CoreException {
		String typeName= Signature.toString(field.getTypeSignature());
		String fieldName= field.getElementName();
		return CodeGeneration.getFieldComment(field.getJavaScriptUnit(), typeName, fieldName, lineDelimiter);
	}		
		
	/**
	 * @return Returns the scheduling rule for this operation
	 */
	public ISchedulingRule getScheduleRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/**
	 * Runs the operation.
	 * @throws OperationCanceledException Runtime error thrown when operation is cancelled.
	 */	
	public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		if (fMembers.length == 0) {
			return;
		}
		try {
			monitor.beginTask(CodeGenerationMessages.AddJavaDocStubOperation_description, fMembers.length + 2); 

			addJavadocComments(monitor);
		} finally {
			monitor.done();
		}
	}
	/* moved this so we this can be re-used in web component */
	
	protected IDocument getDocument(IJavaScriptUnit cu, IProgressMonitor monitor) throws CoreException  {
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		IPath path= cu.getPath();
		try {
		manager.connect(path, LocationKind.IFILE, new SubProgressMonitor(monitor, 1));
			return manager.getTextFileBuffer(path).getDocument();
		
			
		}finally {
			manager.disconnect(path, LocationKind.IFILE,new SubProgressMonitor(monitor, 1));
		}
	}
	
	private void addJavadocComments(IProgressMonitor monitor) throws CoreException {
		IJavaScriptUnit cu= fMembers[0].getJavaScriptUnit();
		
		
		try {
			IDocument document= getDocument(cu,monitor);
			
			String lineDelim= TextUtilities.getDefaultLineDelimiter(document);
			MultiTextEdit edit= new MultiTextEdit();
			
			for (int i= 0; i < fMembers.length; i++) {
				IMember curr= fMembers[i];
				int memberStartOffset= getMemberStartOffset(curr, document);
				
				String comment= null;
				switch (curr.getElementType()) {
					case IJavaScriptElement.TYPE:
						comment= createTypeComment((IType) curr, lineDelim);
						break;
					case IJavaScriptElement.FIELD:
						comment= createFieldComment((IField) curr, lineDelim);	
						break;
					case IJavaScriptElement.METHOD:
						comment= createMethodComment((IFunction) curr, lineDelim);
						break;
				}
				if (comment == null) {
					StringBuffer buf= new StringBuffer();
					buf.append("/**").append(lineDelim); //$NON-NLS-1$
					buf.append(" *").append(lineDelim); //$NON-NLS-1$
					buf.append(" */").append(lineDelim); //$NON-NLS-1$
					comment= buf.toString();						
				} else {
					if (!comment.endsWith(lineDelim)) {
						comment= comment + lineDelim;
					}
				}
				
				final IJavaScriptProject project= cu.getJavaScriptProject();
				IRegion region= document.getLineInformationOfOffset(memberStartOffset);
				
				String line= document.get(region.getOffset(), region.getLength());
				String indentString= Strings.getIndentString(line, project);
				
				String indentedComment= Strings.changeIndent(comment, 0, project, indentString, lineDelim);

				edit.addChild(new InsertEdit(memberStartOffset, indentedComment));

				monitor.worked(1);
			}
			edit.apply(document); // apply all edits
		} catch (BadLocationException e) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
		} finally {
			
		}
	}

	private int getMemberStartOffset(IMember curr, IDocument document) throws JavaScriptModelException {
		int offset= curr.getSourceRange().getOffset();
		TokenScanner scanner= new TokenScanner(document, curr.getJavaScriptProject());
		try {
			return scanner.getNextStartOffset(offset, true); // read to the first real non comment token
		} catch (CoreException e) {
			// ignore
		}
		return offset;
	}
		
}
