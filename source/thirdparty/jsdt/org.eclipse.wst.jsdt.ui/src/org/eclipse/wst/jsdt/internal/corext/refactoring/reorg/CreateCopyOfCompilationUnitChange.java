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
package org.eclipse.wst.jsdt.internal.corext.refactoring.reorg;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.corext.refactoring.IRefactoringSearchRequestor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.wst.jsdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.wst.jsdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.TypeOccurrenceCollector;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.wst.jsdt.internal.corext.util.JavaElementResourceMapping;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.SearchUtils;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

public final class CreateCopyOfCompilationUnitChange extends CreateTextFileChange {

	private static TextChangeManager createChangeManager(IProgressMonitor monitor, IJavaScriptUnit copy, String newName) throws CoreException {
		TextChangeManager manager= new TextChangeManager();
		SearchResultGroup refs= getReferences(copy, monitor);
		if (refs == null)
			return manager;
		if (refs.getCompilationUnit() == null)
			return manager;

		String name= RefactoringCoreMessages.CopyRefactoring_update_ref;
		SearchMatch[] results= refs.getSearchResults();
		for (int j= 0; j < results.length; j++) {
			SearchMatch searchResult= results[j];
			if (searchResult.getAccuracy() == SearchMatch.A_INACCURATE)
				continue;
			int offset= searchResult.getOffset();
			int length= searchResult.getLength();
			TextChangeCompatibility.addTextEdit(manager.get(copy), name, new ReplaceEdit(offset, length, newName));
		}
		return manager;
	}

	private static SearchPattern createSearchPattern(IType type) throws JavaScriptModelException {
		SearchPattern pattern= SearchPattern.createPattern(type, IJavaScriptSearchConstants.ALL_OCCURRENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
		IFunction[] constructors= JavaElementUtil.getAllConstructors(type);
		if (constructors.length == 0)
			return pattern;
		SearchPattern constructorDeclarationPattern= RefactoringSearchEngine.createOrPattern(constructors, IJavaScriptSearchConstants.DECLARATIONS);
		return SearchPattern.createOrPattern(pattern, constructorDeclarationPattern);
	}

	private static String getCopiedFileSource(IProgressMonitor monitor, IJavaScriptUnit unit, String newTypeName) throws CoreException {
		IJavaScriptUnit copy= unit.getPrimary().getWorkingCopy(null);
		try {
			TextChangeManager manager= createChangeManager(monitor, copy, newTypeName);
			String result= manager.get(copy).getPreviewContent(new NullProgressMonitor());
			return result;
		} finally {
			copy.discardWorkingCopy();
		}
	}

	private static SearchResultGroup getReferences(final IJavaScriptUnit copy, IProgressMonitor monitor) throws JavaScriptModelException {
		final IJavaScriptUnit[] copies= new IJavaScriptUnit[] { copy};
		IJavaScriptSearchScope scope= SearchEngine.createJavaSearchScope(copies);
		final IType type= copy.findPrimaryType();
		if (type == null)
			return null;
		SearchPattern pattern= createSearchPattern(type);
		final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(pattern);
		engine.setScope(scope);
		engine.setWorkingCopies(copies);
		engine.searchPattern(monitor);
		engine.setRequestor(new IRefactoringSearchRequestor() {
			TypeOccurrenceCollector fTypeOccurrenceCollector= new TypeOccurrenceCollector(type);
			public SearchMatch acceptSearchMatch(SearchMatch match) {
				try {
					return fTypeOccurrenceCollector.acceptSearchMatch2(copy, match);
				} catch (CoreException e) {
					JavaScriptPlugin.log(e);
					return null;
				}
			}
		});
		final Object[] results= engine.getResults();
		// Assert.isTrue(results.length <= 1);
		// just 1 file or none, but inaccurate matches can play bad here (see
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=106127)
		for (int index= 0; index < results.length; index++) {
			SearchResultGroup group= (SearchResultGroup) results[index];
			if (group.getCompilationUnit().equals(copy))
				return group;
		}
		return null;
	}

	private final INewNameQuery fNameQuery;

	private final IJavaScriptUnit fOldCu;

	public CreateCopyOfCompilationUnitChange(IPath path, String source, IJavaScriptUnit oldCu, INewNameQuery nameQuery) {
		super(path, source, null, "java"); //$NON-NLS-1$
		fOldCu= oldCu;
		fNameQuery= nameQuery;
		setEncoding(oldCu);
	}

	public String getName() {
		return Messages.format(RefactoringCoreMessages.CreateCopyOfCompilationUnitChange_create_copy, new String[] { fOldCu.getElementName(), getPathLabel(fOldCu.getResource())});
	}

	protected IFile getOldFile(IProgressMonitor monitor) throws OperationCanceledException {
		try {
			monitor.beginTask("", 12); //$NON-NLS-1$
			String oldSource= super.getSource();
			IPath oldPath= super.getPath();
			String newTypeName= fNameQuery.getNewName();
			try {
				String newSource= getCopiedFileSource(new SubProgressMonitor(monitor, 9), fOldCu, newTypeName);
				setSource(newSource);
				setPath(fOldCu.getResource().getParent().getFullPath().append(JavaModelUtil.getRenamedCUName(fOldCu, newTypeName)));
				return super.getOldFile(new SubProgressMonitor(monitor, 1));
			} catch (CoreException e) {
				setSource(oldSource);
				setPath(oldPath);
				return super.getOldFile(new SubProgressMonitor(monitor, 2));
			}
		} finally {
			monitor.done();
		}
	}

	private String getPathLabel(IResource resource) {
		final StringBuffer buffer= new StringBuffer(resource.getProject().getName());
		final String path= resource.getParent().getProjectRelativePath().toString();
		if (path.length() > 0) {
			buffer.append('/');
			buffer.append(path);
		}
		return buffer.toString();
	}

	private void markAsExecuted(IJavaScriptUnit unit, ResourceMapping mapping) {
		ReorgExecutionLog log= (ReorgExecutionLog) getAdapter(ReorgExecutionLog.class);
		if (log != null) {
			log.markAsProcessed(unit);
			log.markAsProcessed(mapping);
		}
	}

	public Change perform(IProgressMonitor monitor) throws CoreException {
		ResourceMapping mapping= JavaElementResourceMapping.create(fOldCu);
		final Change result= super.perform(monitor);
		markAsExecuted(fOldCu, mapping);
		return result;
	}

	private void setEncoding(IJavaScriptUnit unit) {
		IResource resource= unit.getResource();
		// no file so the encoding is taken from the target
		if (!(resource instanceof IFile))
			return;
		IFile file= (IFile) resource;
		try {
			String encoding= file.getCharset(false);
			if (encoding != null) {
				setEncoding(encoding, true);
			} else {
				encoding= file.getCharset(true);
				if (encoding != null) {
					setEncoding(encoding, false);
				}
			}
		} catch (CoreException e) {
			// Take encoding from target
		}
	}
}
