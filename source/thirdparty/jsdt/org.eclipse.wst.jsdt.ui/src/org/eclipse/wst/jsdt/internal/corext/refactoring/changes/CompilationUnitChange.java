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
package org.eclipse.wst.jsdt.internal.corext.refactoring.changes;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.ContentStamp;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.UndoEdit;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

public class CompilationUnitChange extends TextFileChange {

	private final IJavaScriptUnit fCUnit;
	
	/** The (optional) refactoring descriptor */
	private ChangeDescriptor fDescriptor;
	
	/**
	 * Creates a new <code>CompilationUnitChange</code>.
	 * 
	 * @param name the change's name mainly used to render the change in the UI
	 * @param cunit the compilation unit this text change works on
	 */
	public CompilationUnitChange(String name, IJavaScriptUnit cunit) {
		super(name, getFile(cunit));
		Assert.isNotNull(cunit);
		fCUnit= cunit;
		setTextType("js"); //$NON-NLS-1$
	}
	
	private static IFile getFile(IJavaScriptUnit cunit) {
		return (IFile) cunit.getResource();
	}
	
	/* non java-doc
	 * Method declared in IChange.
	 */
	public Object getModifiedElement(){
		return fCUnit;
	}
	
	/**
	 * Returns the compilation unit this change works on.
	 * 
	 * @return the compilation unit this change works on
	 */
	public IJavaScriptUnit getCompilationUnit() {
		return fCUnit;
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected IDocument acquireDocument(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 2); //$NON-NLS-1$
		fCUnit.becomeWorkingCopy(new SubProgressMonitor(pm, 1));
		return super.acquireDocument(new SubProgressMonitor(pm, 1));
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void releaseDocument(IDocument document, IProgressMonitor pm) throws CoreException {
		boolean isModified= isDocumentModified();
		super.releaseDocument(document, pm);
		try {
			fCUnit.discardWorkingCopy();
		} finally {
			if (isModified && !isDocumentAcquired()) {
				if (fCUnit.isWorkingCopy())
					JavaModelUtil.reconcile(fCUnit);
				else
					fCUnit.makeConsistent(pm);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected Change createUndoChange(UndoEdit edit, ContentStamp stampToRestore) {
		try {
			return new UndoCompilationUnitChange(getName(), fCUnit, edit, stampToRestore, getSaveMode());
		} catch (CoreException e) {
			JavaScriptPlugin.log(e);
			return null;
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Object getAdapter(Class adapter) {
		if (IJavaScriptUnit.class.equals(adapter))
			return fCUnit;
		return super.getAdapter(adapter);
	}
	
	/**
	 * Sets the refactoring descriptor for this change
	 * 
	 * @param descriptor the descriptor to set
	 */
	public void setDescriptor(ChangeDescriptor descriptor) {
		fDescriptor= descriptor;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ChangeDescriptor getDescriptor() {
		return fDescriptor;
	}
}

