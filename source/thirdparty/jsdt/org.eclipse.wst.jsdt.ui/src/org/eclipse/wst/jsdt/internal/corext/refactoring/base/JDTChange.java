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
package org.eclipse.wst.jsdt.internal.corext.refactoring.base;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.Resources;

/**
 * JDT specific change object.
 */
public abstract class JDTChange extends Change {

	private long fModificationStamp;
	private boolean fReadOnly;
	
	private static class ValidationState {
		private IResource fResource;
		private int fKind;
		private boolean fDirty;
		private boolean fReadOnly;
		private long fModificationStamp;
		private ITextFileBuffer fTextFileBuffer;
		public static final int RESOURCE= 1;
		public static final int DOCUMENT= 2;
		public ValidationState(IResource resource) {
			fResource= resource;
			if (resource instanceof IFile) {
				initializeFile((IFile)resource);
			} else {
				initializeResource(resource);
			}
		}
		public void checkDirty(RefactoringStatus status, long stampToMatch, IProgressMonitor pm) throws CoreException {
			if (fDirty) {
				if (fKind == DOCUMENT && fTextFileBuffer != null && stampToMatch == fModificationStamp) {
					fTextFileBuffer.commit(pm, false);
				} else {
					status.addFatalError(Messages.format(
						RefactoringCoreMessages.Change_is_unsaved, fResource.getFullPath().toString())); 
				}
			}
		}
		public void checkDirty(RefactoringStatus status) {
			if (fDirty) {
				status.addFatalError(Messages.format(
					RefactoringCoreMessages.Change_is_unsaved, fResource.getFullPath().toString())); 
			}
		}
		public void checkReadOnly(RefactoringStatus status) {
			if (fReadOnly) {
				status.addFatalError(Messages.format(
					RefactoringCoreMessages.Change_is_read_only, fResource.getFullPath().toString())); 
			}
		}
		public void checkSameReadOnly(RefactoringStatus status, boolean valueToMatch) {
			if (fReadOnly != valueToMatch) {
				status.addFatalError(Messages.format(
					RefactoringCoreMessages.Change_same_read_only,
					fResource.getFullPath().toString()));
			}
		}
		public void checkModificationStamp(RefactoringStatus status, long stampToMatch) {
			if (fKind == DOCUMENT) {
				if (stampToMatch != IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP && fModificationStamp != stampToMatch) {
					status.addFatalError(Messages.format(
						RefactoringCoreMessages.Change_has_modifications, fResource.getFullPath().toString())); 
				}
			} else {
				if (stampToMatch != IResource.NULL_STAMP && fModificationStamp != stampToMatch) {
					status.addFatalError(Messages.format(
						RefactoringCoreMessages.Change_has_modifications, fResource.getFullPath().toString())); 
					
				}
			}
		}
		private void initializeFile(IFile file) {
			fTextFileBuffer= getBuffer(file);
			if (fTextFileBuffer == null) {
				initializeResource(file);
			} else {
				IDocument document= fTextFileBuffer.getDocument();
				fDirty= fTextFileBuffer.isDirty();
				fReadOnly= Resources.isReadOnly(file);
				if (document instanceof IDocumentExtension4) {
					fKind= DOCUMENT;
					fModificationStamp= ((IDocumentExtension4)document).getModificationStamp();
				} else {
					fKind= RESOURCE;
					fModificationStamp= file.getModificationStamp();
				}
			}
			
		}
		private void initializeResource(IResource resource) {
			fKind= RESOURCE;
			fDirty= false;
			fReadOnly= Resources.isReadOnly(resource);
			fModificationStamp= resource.getModificationStamp();
		}
	}

	protected static final int NONE= 0;
	protected static final int READ_ONLY= 1 << 0;
	protected static final int DIRTY= 1 << 1;
	private static final int SAVE= 1 << 2;
	protected static final int SAVE_IF_DIRTY= SAVE | DIRTY;
	
	protected JDTChange() {
		fModificationStamp= IResource.NULL_STAMP;
		fReadOnly= false;
	}
	
	public void initializeValidationData(IProgressMonitor pm) {
		IResource resource= getResource(getModifiedElement());
		if (resource != null) {
			fModificationStamp= getModificationStamp(resource);
			fReadOnly= Resources.isReadOnly(resource);
		}
	}

	// protected final RefactoringStatus isValid(IProgressMonitor pm, boolean checkReadOnly, boolean checkDirty) throws CoreException {
	protected final RefactoringStatus isValid(IProgressMonitor pm, int flags) throws CoreException {
		pm.beginTask("", 2); //$NON-NLS-1$
		try {
			RefactoringStatus result= new RefactoringStatus();
			Object modifiedElement= getModifiedElement();
			checkExistence(result, modifiedElement);
			if (result.hasFatalError())
				return result;
			if (flags == NONE)
				return result;
			IResource resource= getResource(modifiedElement);
			if (resource != null) {
				ValidationState state= new ValidationState(resource);
				state.checkModificationStamp(result, fModificationStamp);
				if (result.hasFatalError())
					return result;
				state.checkSameReadOnly(result, fReadOnly);
				if (result.hasFatalError())
					return result;
				if ((flags & READ_ONLY) != 0) {
					state.checkReadOnly(result);
					if (result.hasFatalError())
						return result;
				}
				if ((flags & DIRTY) != 0) {
					if ((flags & SAVE) != 0) {
						state.checkDirty(result, fModificationStamp, new SubProgressMonitor(pm, 1));
					} else {
						state.checkDirty(result);
					}
				}
			}
			return result;
		} finally {
			pm.done();
		}
	}

	protected static void checkIfModifiable(RefactoringStatus status, Object element, int flags) {
		checkIfModifiable(status, getResource(element), flags);
	}

	protected static void checkIfModifiable(RefactoringStatus result, IResource resource, int flags) {
		checkExistence(result, resource);
		if (result.hasFatalError())
			return;
		if (flags == NONE)
			return;
		ValidationState state= new ValidationState(resource);
		if ((flags & READ_ONLY) != 0) {
			state.checkReadOnly(result);
			if (result.hasFatalError())
				return;
		}
		if ((flags & DIRTY) != 0) {
			state.checkDirty(result);
		}
	}

	protected static void checkExistence(RefactoringStatus status, Object element) {
		if (element == null) {
			status.addFatalError(RefactoringCoreMessages.DynamicValidationStateChange_workspace_changed); 
			
		} else if (element instanceof IResource && !((IResource)element).exists()) {
			status.addFatalError(Messages.format(
				RefactoringCoreMessages.Change_does_not_exist, ((IResource)element).getFullPath().toString())); 
		} else if (element instanceof IJavaScriptElement && !((IJavaScriptElement)element).exists()) {
			status.addFatalError(Messages.format(
				RefactoringCoreMessages.Change_does_not_exist, ((IJavaScriptElement)element).getElementName())); 
		}
	}

	private static IResource getResource(Object element) {
		if (element instanceof IResource) {
			return (IResource)element;
		}
		if (element instanceof IJavaScriptUnit) {
			return ((IJavaScriptUnit)element).getPrimary().getResource();
		}
		if (element instanceof IJavaScriptElement) {
			return ((IJavaScriptElement)element).getResource();
		}
		if (element instanceof IAdaptable) {
			return (IResource)((IAdaptable)element).getAdapter(IResource.class);
		}
		return null;
	}

	public String toString() {
		return getName();
	}
	
	public long getModificationStamp(IResource resource) {
		if (!(resource instanceof IFile))
			return resource.getModificationStamp();
		IFile file= (IFile)resource;
		ITextFileBuffer buffer= getBuffer(file);
		if (buffer == null) {
			return file.getModificationStamp();
		} else {
			IDocument document= buffer.getDocument();
			if (document instanceof IDocumentExtension4) {
				return ((IDocumentExtension4)document).getModificationStamp();
			} else {
				return file.getModificationStamp();
			}
		}
	}
	
	private static ITextFileBuffer getBuffer(IFile file) {
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		return manager.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
	}
}
