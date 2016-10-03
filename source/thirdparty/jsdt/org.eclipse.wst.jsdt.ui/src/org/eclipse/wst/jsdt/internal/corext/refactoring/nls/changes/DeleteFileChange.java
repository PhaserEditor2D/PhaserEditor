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
package org.eclipse.wst.jsdt.internal.corext.refactoring.nls.changes;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JDTChange;
import org.eclipse.wst.jsdt.internal.corext.util.IOCloser;

public class DeleteFileChange extends JDTChange {

	private IPath fPath;
	private String fSource;
	
	public DeleteFileChange(IFile file){
		Assert.isNotNull(file, "file"); //$NON-NLS-1$
		fPath= file.getFullPath().removeFirstSegments(ResourcesPlugin.getWorkspace().getRoot().getFullPath().segmentCount());
	}
	
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		return isValid(pm, READ_ONLY | DIRTY);
	}
	
	public Change perform(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(NLSChangesMessages.deleteFile_deleting_resource, 1); 
			IFile file= ResourcesPlugin.getWorkspace().getRoot().getFile(fPath);
			Assert.isNotNull(file);
			Assert.isTrue(file.exists());
			Assert.isTrue(!file.isReadOnly());
			fSource= getSource(file);
			CreateFileChange undo= createUndoChange(file, fPath, file.getModificationStamp(), fSource);
			file.delete(true, true, pm);
			return undo;
		} finally {
			pm.done();
		}
	}
	
	private String getSource(IFile file) throws CoreException {
		String encoding= null;
		try {
			encoding= file.getCharset();
		} catch (CoreException ex) {
			// fall through. Take default encoding.
		}
		
		StringBuffer sb= new StringBuffer();
		BufferedReader br= null;
		InputStream in= null;
		try {
			in= file.getContents();
		    if (encoding != null)
		        br= new BufferedReader(new InputStreamReader(in, encoding));	
		    else
		        br= new BufferedReader(new InputStreamReader(in));	
			int read= 0;
			while ((read= br.read()) != -1)
				sb.append((char) read);
		} catch (IOException e){
			throw new JavaScriptModelException(e, IJavaScriptModelStatusConstants.IO_EXCEPTION);
		} finally {
			try{
				IOCloser.rethrows(br, in);
			} catch (IOException e){
				throw new JavaScriptModelException(e, IJavaScriptModelStatusConstants.IO_EXCEPTION);
			}	
		}
		return sb.toString();
	}
	
	private static CreateFileChange createUndoChange(IFile file, IPath path, long stampToRestore, String source) {
		String encoding;
		try {
			encoding= file.getCharset(false);
		} catch (CoreException e) {
			encoding= null;
		}
		return new CreateFileChange(path, source, encoding, stampToRestore);
	}

	public String getName() {
		return NLSChangesMessages.deleteFile_Delete_File; 
	}

	public Object getModifiedElement() {
		return ResourcesPlugin.getWorkspace().getRoot().getFile(fPath);
	}
}

