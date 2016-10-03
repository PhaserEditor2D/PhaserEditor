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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.DeleteProcessor;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.JavaScriptRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.wst.jsdt.internal.corext.refactoring.participants.ResourceProcessors;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.ICommentProvider;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.Resources;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

public final class JavaDeleteProcessor extends DeleteProcessor implements IScriptableRefactoring, ICommentProvider {

	private static final String ATTRIBUTE_RESOURCES= "resources"; //$NON-NLS-1$
	private static final String ATTRIBUTE_ELEMENTS= "elements"; //$NON-NLS-1$
	private static final String ATTRIBUTE_SUGGEST_ACCESSORS= "accessors"; //$NON-NLS-1$
	private static final String ATTRIBUTE_DELETE_SUBPACKAGES= "subPackages"; //$NON-NLS-1$

	private boolean fAccessorsDeleted;
	private boolean fWasCanceled;
	private boolean fSuggestGetterSetterDeletion;
	private Object[] fElements;
	private IResource[] fResources;
	private IJavaScriptElement[] fJavaElements;
	private IReorgQueries fDeleteQueries;
	private DeleteModifications fDeleteModifications;
	private String fComment;

	private Change fDeleteChange;
	private boolean fDeleteSubPackages;
	
	public static final String IDENTIFIER= "org.eclipse.wst.jsdt.ui.DeleteProcessor"; //$NON-NLS-1$

	public JavaDeleteProcessor(Object[] elements) {
		fElements= elements;
		if (fElements != null) {
			fResources= RefactoringAvailabilityTester.getResources(elements);
			fJavaElements= RefactoringAvailabilityTester.getJavaElements(elements);
		}
		fSuggestGetterSetterDeletion= true;
		fDeleteSubPackages= false;
		fWasCanceled= false;
	}

	public String getIdentifier() {
		return IDENTIFIER;
	}
	
	public boolean isApplicable() throws CoreException {
		if (fElements.length == 0)
			return false;
		if (fElements.length != fResources.length + fJavaElements.length)
			return false;
		for (int i= 0; i < fResources.length; i++) {
			if (!RefactoringAvailabilityTester.isDeleteAvailable(fResources[i]))
				return false;
		}
		for (int i= 0; i < fJavaElements.length; i++) {
			if (!RefactoringAvailabilityTester.isDeleteAvailable(fJavaElements[i]))
				return false;
		}
		return true;
	}
	
	public boolean needsProgressMonitor() {
		if (fResources != null && fResources.length > 0)
			return true;
		if (fJavaElements != null) {
			for (int i= 0; i < fJavaElements.length; i++) {
				int type= fJavaElements[i].getElementType();
				if (type <= IJavaScriptElement.CLASS_FILE)
					return true;
			}
		}
		return false;
		
	}

	public String getProcessorName() {
		return RefactoringCoreMessages.DeleteRefactoring_7; 
	}
	
	public Object[] getElements() {
		return fElements;
	}
	
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants shared) throws CoreException {
		return fDeleteModifications.loadParticipants(status, this, getAffectedProjectNatures(), shared);
	}
	
	private String[] getAffectedProjectNatures() throws CoreException {
		String[] jNatures= JavaProcessors.computeAffectedNaturs(fJavaElements);
		String[] rNatures= ResourceProcessors.computeAffectedNatures(fResources);
		Set result= new HashSet();
		result.addAll(Arrays.asList(jNatures));
		result.addAll(Arrays.asList(rNatures));
		return (String[])result.toArray(new String[result.size()]);
	}

	/* 
	 * This has to be customizable because when drag and drop is performed on a field,
	 * you don't want to suggest deleting getter/setter if only the field was moved.
	 */
	public void setSuggestGetterSetterDeletion(boolean suggest){
		fSuggestGetterSetterDeletion= suggest;
	}
	
	public void setDeleteSubPackages(boolean selection) {
		fDeleteSubPackages= selection;
	}
	
	public boolean getDeleteSubPackages() {
		return fDeleteSubPackages;
	}
	
	public boolean hasSubPackagesToDelete() {
		try {
			for (int i= 0; i < fJavaElements.length; i++) {
				if (fJavaElements[i] instanceof IPackageFragment) {
					IPackageFragment packageFragment= (IPackageFragment) fJavaElements[i];
					if (packageFragment.isDefaultPackage())
						continue; // see bug 132576 (can remove this if(..) continue; statement when bug is fixed)
					if (packageFragment.hasSubpackages())
						return true;
				}
			}
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
		}
		return false;
	}

	public void setQueries(IReorgQueries queries){
		Assert.isNotNull(queries);
		fDeleteQueries= queries;
	}
	
	public IJavaScriptElement[] getJavaElementsToDelete(){
		return fJavaElements;
	}

	public boolean wasCanceled() {
		return fWasCanceled;
	}
	
	public IResource[] getResourcesToDelete(){
		return fResources;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		Assert.isNotNull(fDeleteQueries);//must be set before checking activation
		RefactoringStatus result= new RefactoringStatus();
		result.merge(RefactoringStatus.create(Resources.checkInSync(ReorgUtils.getNotLinked(fResources))));
		IResource[] javaResources= ReorgUtils.getResources(fJavaElements);
		result.merge(RefactoringStatus.create(Resources.checkInSync(ReorgUtils.getNotNulls(javaResources))));
		for (int i= 0; i < fJavaElements.length; i++) {
			IJavaScriptElement element= fJavaElements[i];
			if (element instanceof IType && ((IType)element).isAnonymous()) {
				// work around for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=44450
				// result.addFatalError("Currently, there isn't any support to delete an anonymous type.");
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		pm.beginTask(RefactoringCoreMessages.DeleteRefactoring_1, 1); 
		try{
			fWasCanceled= false;
			RefactoringStatus result= new RefactoringStatus();

			recalculateElementsToDelete();

			checkDirtyCompilationUnits(result);
			checkDirtyResources(result);
			fDeleteModifications= new DeleteModifications();
			fDeleteModifications.delete(fResources);
			fDeleteModifications.delete(fJavaElements);
			List/*<IResource>*/ packageDeletes= fDeleteModifications.postProcess();
			
			TextChangeManager manager= new TextChangeManager();
			fDeleteChange= DeleteChangeCreator.createDeleteChange(manager, fResources, fJavaElements, getProcessorName(), packageDeletes);
			
			ResourceChangeChecker checker= (ResourceChangeChecker) context.getChecker(ResourceChangeChecker.class);
			IResourceChangeDescriptionFactory deltaFactory= checker.getDeltaFactory();
			fDeleteModifications.buildDelta(deltaFactory);
			IFile[] files= ResourceUtil.getFiles(manager.getAllCompilationUnits());
			for (int i= 0; i < files.length; i++) {
				deltaFactory.change(files[i]);
			}
			return result;
		} catch (OperationCanceledException e) {
			fWasCanceled= true;
			throw e;
		} catch (JavaScriptModelException e){
			throw e;
		} catch (CoreException e) {
			throw new JavaScriptModelException(e);
		} finally{
			pm.done();
		}
	}
	
	private void checkDirtyCompilationUnits(RefactoringStatus result) throws CoreException {
		if (fJavaElements == null || fJavaElements.length == 0)
			return;
		for (int je= 0; je < fJavaElements.length; je++) {
			IJavaScriptElement element= fJavaElements[je];
			if (element instanceof IJavaScriptUnit) {
				checkDirtyCompilationUnit(result, (IJavaScriptUnit)element);	
			} else if (element instanceof IPackageFragment) {
				IJavaScriptUnit[] units= ((IPackageFragment)element).getJavaScriptUnits();
				for (int u = 0; u < units.length; u++) {
					checkDirtyCompilationUnit(result, units[u]);
				}
			}
		}
	}
	
	private void checkDirtyCompilationUnit(RefactoringStatus result, IJavaScriptUnit cunit) {
		IResource resource= cunit.getResource();
		if (resource == null || resource.getType() != IResource.FILE)
			return;
		checkDirtyFile(result, (IFile)resource);
	}

	private void checkDirtyResources(final RefactoringStatus result) throws CoreException {
		for (int i= 0; i < fResources.length; i++) {
			IResource resource= fResources[i];
			resource.accept(new IResourceVisitor() {
				public boolean visit(IResource visitedResource) throws CoreException {
					if (visitedResource instanceof IFile) {
						checkDirtyFile(result, (IFile)visitedResource);
					}
					return true;
				}
			}, IResource.DEPTH_INFINITE, false);
		}
	}
	
	private void checkDirtyFile(RefactoringStatus result, IFile file) {
		if (file == null || !file.exists())
			return;
		ITextFileBuffer buffer= FileBuffers.getTextFileBufferManager().getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
		if (buffer != null && buffer.isDirty()) {
			if (buffer.isStateValidated() && buffer.isSynchronized()) {
				result.addWarning(Messages.format(
					RefactoringCoreMessages.JavaDeleteProcessor_unsaved_changes, 
					file.getFullPath().toString()));
			} else {
				result.addFatalError(Messages.format(
					RefactoringCoreMessages.JavaDeleteProcessor_unsaved_changes, 
					file.getFullPath().toString()));
			}
		}
	}

	/*
	 * The set of elements that will eventually be deleted may be very different from the set
	 * originally selected - there may be fewer, more or different elements.
	 * This method is used to calculate the set of elements that will be deleted - if necessary, 
	 * it asks the user.
	 */
	private void recalculateElementsToDelete() throws CoreException {
		//the sequence is critical here
		fAccessorsDeleted= false;
		if (fDeleteSubPackages) /* add subpackages first, to allow removing elements with parents in selection etc. */
			addSubPackages();
		
		removeElementsWithParentsInSelection(); /*ask before adding empty cus - you don't want to ask if you, for example delete 
												 *the package, in which the cus live*/
		removeUnconfirmedFoldersThatContainSourceFolders(); /* a selected folder may be a parent of a source folder
															 * we must inform the user about it and ask if ok to delete the folder*/
		removeUnconfirmedReferencedArchives();
//		addEmptyCusToDelete();
		removeJavaElementsChildrenOfJavaElements();/*because adding cus may create elements (types in cus)
												    *whose parents are in selection*/
		confirmDeletingReadOnly();   /*after empty cus - you want to ask for all cus that are to be deleted*/
	
		if (fSuggestGetterSetterDeletion)
			addGettersSetters();/*at the end - this cannot invalidate anything*/
		
		addDeletableParentPackagesOnPackageDeletion(); /* do not change the sequence in fJavaElements after this method */
	}

	/**
	 * Adds all subpackages of the selected packages to the list of items to be
	 * deleted.
	 * 
	 * @throws JavaScriptModelException
	 */
	private void addSubPackages() throws JavaScriptModelException {

		final Set javaElements= new HashSet();
		for (int i= 0; i < fJavaElements.length; i++) {
			if (fJavaElements[i] instanceof IPackageFragment) {
				javaElements.addAll(Arrays.asList(JavaElementUtil.getPackageAndSubpackages((IPackageFragment) fJavaElements[i])));
			} else {
				javaElements.add(fJavaElements[i]);
			}
		}

		fJavaElements= (IJavaScriptElement[]) javaElements.toArray(new IJavaScriptElement[javaElements.size()]);
	}

	/**
	 * Add deletable parent packages to the list of items to delete.
	 * 
	 * @throws CoreException
	 */
	private void addDeletableParentPackagesOnPackageDeletion() throws CoreException {

		final List/* <IPackageFragment */initialPackagesToDelete= ReorgUtils.getElementsOfType(fJavaElements, IJavaScriptElement.PACKAGE_FRAGMENT);

		if (initialPackagesToDelete.size() == 0)
			return;

		// Move from inner to outer packages
		Collections.sort(initialPackagesToDelete, new Comparator() {
			public int compare(Object arg0, Object arg1) {
				IPackageFragment one= (IPackageFragment) arg0;
				IPackageFragment two= (IPackageFragment) arg1;
				return two.getElementName().compareTo(one.getElementName());
			}
		});

		// Get resources and java elements which will be deleted as well
		final Set/* <IResource> */deletedChildren= new HashSet();
		deletedChildren.addAll(Arrays.asList(fResources));
		for (int i= 0; i < fJavaElements.length; i++) {
			if (!ReorgUtils.isInsideCompilationUnit(fJavaElements[i]))
				deletedChildren.add(fJavaElements[i].getResource());
		}

		// new package list in the right sequence
		final List/* <IPackageFragment */allFragmentsToDelete= new ArrayList();

		for (Iterator outerIter= initialPackagesToDelete.iterator(); outerIter.hasNext();) {
			final IPackageFragment currentPackageFragment= (IPackageFragment) outerIter.next();
			
			// The package will at least be cleared
			allFragmentsToDelete.add(currentPackageFragment);

			if (canRemoveCompletely(currentPackageFragment, initialPackagesToDelete)) {
				
				final IPackageFragment parent= JavaElementUtil.getParentSubpackage(currentPackageFragment);
				if (parent != null && !initialPackagesToDelete.contains(parent)) {

					final List/* <IPackageFragment> */emptyParents= new ArrayList();
					addDeletableParentPackages(parent, initialPackagesToDelete, deletedChildren, emptyParents);

					// Add parents in the right sequence (inner to outer)
					allFragmentsToDelete.addAll(emptyParents);
				}
			}
		}

		// Remove resources in deleted packages; and the packages as well
		final List/* <IJavaScriptElement> */javaElements= new ArrayList();
		for (int i= 0; i < fJavaElements.length; i++) {
			if (!(fJavaElements[i] instanceof IPackageFragment)) {
				// remove children of deleted packages
				final IPackageFragment frag= (IPackageFragment) fJavaElements[i].getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT);
				if (!allFragmentsToDelete.contains(frag))
					javaElements.add(fJavaElements[i]);
			}
		}
		// Re-add deleted packages - note the (new) sequence
		javaElements.addAll(allFragmentsToDelete);

		// Remove resources in deleted folders
		final List/* <IResource> */resources= new ArrayList();
		for (int i= 0; i < fResources.length; i++) {
			IResource parent= fResources[i];
			if (parent.getType() == IResource.FILE)
				parent= parent.getParent();
			if (!deletedChildren.contains(parent))
				resources.add(fResources[i]);
		}

		fJavaElements= (IJavaScriptElement[]) javaElements.toArray(new IJavaScriptElement[javaElements.size()]);
		fResources= (IResource[]) resources.toArray(new IResource[resources.size()]);
	}

	/**
	 * Returns true if this initially selected package is really deletable
	 * (if it has non-selected subpackages, it may only be cleared).
	 * 
	 */
	private boolean canRemoveCompletely(IPackageFragment pack, List packagesToDelete) throws JavaScriptModelException {
		final IPackageFragment[] subPackages= JavaElementUtil.getPackageAndSubpackages(pack);
		for (int i= 0; i < subPackages.length; i++) {
			if (!(subPackages[i].equals(pack)) && !(packagesToDelete.contains(subPackages[i])))
				return false;
		}
		return true;
	}

	/**
	 * Adds deletable parent packages of the fragment "frag" to the list
	 * "deletableParentPackages"; also adds the resources of those packages to the
	 * set "resourcesToDelete".
	 * 
	 */
	private void addDeletableParentPackages(IPackageFragment frag, List initialPackagesToDelete, Set resourcesToDelete, List deletableParentPackages)
			throws CoreException {

		if (frag.getResource().isLinked()) {
			final IConfirmQuery query= fDeleteQueries.createYesNoQuery(RefactoringCoreMessages.JavaDeleteProcessor_confirm_linked_folder_delete, false, IReorgQueries.CONFIRM_DELETE_LINKED_PARENT);
			if (!query.confirm(Messages.format(RefactoringCoreMessages.JavaDeleteProcessor_delete_linked_folder_question, new String[] { frag.getResource().getName() })))
					return;
		}
		
		final IResource[] children= (((IContainer) frag.getResource())).members();
		for (int i= 0; i < children.length; i++) {
			// Child must be a package fragment already in the list,
			// or a resource which is deleted as well.
			if (!resourcesToDelete.contains(children[i]))
				return;
		}
		resourcesToDelete.add(frag.getResource());
		deletableParentPackages.add(frag);

		final IPackageFragment parent= JavaElementUtil.getParentSubpackage(frag);
		if (parent != null && !initialPackagesToDelete.contains(parent))
			addDeletableParentPackages(parent, initialPackagesToDelete, resourcesToDelete, deletableParentPackages);
	}

	// ask for confirmation of deletion of all package fragment roots that are
	// on classpaths of other projects
	private void removeUnconfirmedReferencedArchives() throws JavaScriptModelException {
		String queryTitle= RefactoringCoreMessages.DeleteRefactoring_2; 
		IConfirmQuery query= fDeleteQueries.createYesYesToAllNoNoToAllQuery(queryTitle, true, IReorgQueries.CONFIRM_DELETE_REFERENCED_ARCHIVES);
		removeUnconfirmedReferencedPackageFragmentRoots(query);
		removeUnconfirmedReferencedArchiveFiles(query);
	}

	private void removeUnconfirmedReferencedArchiveFiles(IConfirmQuery query) throws JavaScriptModelException, OperationCanceledException {
		List filesToSkip= new ArrayList(0);
		for (int i= 0; i < fResources.length; i++) {
			IResource resource= fResources[i];
			if (! (resource instanceof IFile))
				continue;
		
			IJavaScriptProject project= JavaScriptCore.create(resource.getProject());
			if (project == null || ! project.exists())
				continue;
			IPackageFragmentRoot root= project.findPackageFragmentRoot(resource.getFullPath());
			if (root == null)
				continue;
			List referencingProjects= Arrays.asList(JavaElementUtil.getReferencingProjects(root));
			if (skipDeletingReferencedRoot(query, root, referencingProjects))
				filesToSkip.add(resource);
		}
		removeFromSetToDelete((IFile[]) filesToSkip.toArray(new IFile[filesToSkip.size()]));
	}

	private void removeUnconfirmedReferencedPackageFragmentRoots(IConfirmQuery query) throws JavaScriptModelException, OperationCanceledException {
		List rootsToSkip= new ArrayList(0);
		for (int i= 0; i < fJavaElements.length; i++) {
			IJavaScriptElement element= fJavaElements[i];
			if (! (element instanceof IPackageFragmentRoot))
				continue;
			IPackageFragmentRoot root= (IPackageFragmentRoot)element;
			ArrayList referencingProjects= new ArrayList(Arrays.asList(JavaElementUtil.getReferencingProjects(root)));
			referencingProjects.remove(root.getJavaScriptProject());
			if (skipDeletingReferencedRoot(query, root, referencingProjects))
				rootsToSkip.add(root);
		}
		removeFromSetToDelete((IJavaScriptElement[]) rootsToSkip.toArray(new IJavaScriptElement[rootsToSkip.size()]));
	}

	private static boolean skipDeletingReferencedRoot(IConfirmQuery query, IPackageFragmentRoot root, List referencingProjects) throws OperationCanceledException {
		if (referencingProjects.isEmpty() || root == null || ! root.exists() ||! root.isArchive())
			return false;
		String question= Messages.format(RefactoringCoreMessages.DeleteRefactoring_3, root.getElementName()); 
		return ! query.confirm(question, referencingProjects.toArray());
	}

	private void removeUnconfirmedFoldersThatContainSourceFolders() throws CoreException {
		String queryTitle= RefactoringCoreMessages.DeleteRefactoring_4; 
		IConfirmQuery query= fDeleteQueries.createYesYesToAllNoNoToAllQuery(queryTitle, true, IReorgQueries.CONFIRM_DELETE_FOLDERS_CONTAINING_SOURCE_FOLDERS);
		List foldersToSkip= new ArrayList(0);
		for (int i= 0; i < fResources.length; i++) {
			IResource resource= fResources[i];
			if (resource instanceof IFolder){
				IFolder folder= (IFolder)resource;
				if (containsSourceFolder(folder)){
					String question= Messages.format(RefactoringCoreMessages.DeleteRefactoring_5, folder.getName()); 
					if (! query.confirm(question))
						foldersToSkip.add(folder);
				}
			}
		}
		removeFromSetToDelete((IResource[]) foldersToSkip.toArray(new IResource[foldersToSkip.size()]));
	}

	private static boolean containsSourceFolder(IFolder folder) throws CoreException {
		IResource[] subFolders= folder.members();
		for (int i = 0; i < subFolders.length; i++) {
			if (! (subFolders[i] instanceof IFolder))
				continue;
			IJavaScriptElement element= JavaScriptCore.create(folder);
			if (element instanceof IPackageFragmentRoot)	
				return true;
			if (element instanceof IPackageFragment)	
				continue;
			if (containsSourceFolder((IFolder)subFolders[i]))
				return true;
		}
		return false;
	}

	private void removeElementsWithParentsInSelection() {
		ParentChecker parentUtil= new ParentChecker(fResources, fJavaElements);
		parentUtil.removeElementsWithAncestorsOnList(false);
		fJavaElements= parentUtil.getJavaElements();
		fResources= parentUtil.getResources();
	}

	private void removeJavaElementsChildrenOfJavaElements(){
		ParentChecker parentUtil= new ParentChecker(fResources, fJavaElements);
		parentUtil.removeElementsWithAncestorsOnList(true);
		fJavaElements= parentUtil.getJavaElements();
	}
	
	public Change createChange(IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(RefactoringCoreMessages.JavaDeleteProcessor_creating_change, 1);
			final Map arguments= new HashMap();
			final String description= fElements.length == 1 ? RefactoringCoreMessages.JavaDeleteProcessor_description_singular : RefactoringCoreMessages.JavaDeleteProcessor_description_plural;
			final IProject resource= getSingleProject();
			final String project= resource != null ? resource.getName() : null;
			final String source= project != null ? Messages.format(RefactoringCoreMessages.JavaDeleteProcessor_project_pattern, project) : RefactoringCoreMessages.JavaDeleteProcessor_workspace;
			final String header= Messages.format(RefactoringCoreMessages.JavaDeleteProcessor_header, new String[] { String.valueOf(fElements.length), source});
			int flags= JavaScriptRefactoringDescriptor.JAR_MIGRATION | JavaScriptRefactoringDescriptor.JAR_REFACTORING | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
			final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
			if (fDeleteSubPackages)
				comment.addSetting(RefactoringCoreMessages.JavaDeleteProcessor_delete_subpackages);
		 	if (fAccessorsDeleted)
				comment.addSetting(RefactoringCoreMessages.JavaDeleteProcessor_delete_accessors);
			final JDTRefactoringDescriptor descriptor= new JDTRefactoringDescriptor(IJavaScriptRefactorings.DELETE, project, description, comment.asString(), arguments, flags);
			arguments.put(ATTRIBUTE_DELETE_SUBPACKAGES, Boolean.valueOf(fDeleteSubPackages).toString());
			arguments.put(ATTRIBUTE_SUGGEST_ACCESSORS, Boolean.valueOf(fSuggestGetterSetterDeletion).toString());
			arguments.put(ATTRIBUTE_RESOURCES, Integer.valueOf(fResources.length).toString());
			for (int offset= 0; offset < fResources.length; offset++)
				arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + 1), descriptor.resourceToHandle(fResources[offset]));
			arguments.put(ATTRIBUTE_ELEMENTS, Integer.valueOf(fJavaElements.length).toString());
			for (int offset= 0; offset < fJavaElements.length; offset++)
				arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + fResources.length + 1), descriptor.elementToHandle(fJavaElements[offset]));
			return new DynamicValidationRefactoringChange(descriptor, RefactoringCoreMessages.DeleteRefactoring_7, new Change[] { fDeleteChange});
		} finally {
			monitor.done();
		}
	}

	private IProject getSingleProject() {
		IProject first= null;
		for (int index= 0; index < fElements.length; index++) {
			IProject project= null;
			if (fElements[index] instanceof IJavaScriptElement)
				project= ((IJavaScriptElement) fElements[index]).getJavaScriptProject().getProject();
			else if (fElements[index] instanceof IResource)
				project= ((IResource) fElements[index]).getProject();
			if (project != null) {
				if (first == null)
					first= project;
				else if (!project.equals(first))
					return null;
			}
		}
		return first;
	}

	private void addToSetToDelete(IJavaScriptElement[] newElements){
		fJavaElements= ReorgUtils.union(fJavaElements, newElements);		
	}
	
	private void removeFromSetToDelete(IResource[] resourcesToNotDelete) {
		fResources= ReorgUtils.setMinus(fResources, resourcesToNotDelete);
	}
	
	private void removeFromSetToDelete(IJavaScriptElement[] elementsToNotDelete) {
		fJavaElements= ReorgUtils.setMinus(fJavaElements, elementsToNotDelete);
	}

	private void addGettersSetters() throws JavaScriptModelException {
		IField[] fields= getFields(fJavaElements);
		if (fields.length == 0)
			return;
		//IField -> IFunction[]
		Map getterSetterMapping= createGetterSetterMapping(fields);
		if (getterSetterMapping.isEmpty())
			return;
		removeAlreadySelectedMethods(getterSetterMapping);
		if (getterSetterMapping.isEmpty())
			return;
		fAccessorsDeleted= true;
		List gettersSettersToAdd= getGettersSettersToDelete(getterSetterMapping);
		addToSetToDelete((IFunction[]) gettersSettersToAdd.toArray(new IFunction[gettersSettersToAdd.size()]));
	}

	private List getGettersSettersToDelete(Map getterSetterMapping) {
		List gettersSettersToAdd= new ArrayList(getterSetterMapping.size());
		String queryTitle= RefactoringCoreMessages.DeleteRefactoring_8; 
		IConfirmQuery getterSetterQuery= fDeleteQueries.createYesYesToAllNoNoToAllQuery(queryTitle, true, IReorgQueries.CONFIRM_DELETE_GETTER_SETTER);
		for (Iterator iter= getterSetterMapping.keySet().iterator(); iter.hasNext();) {
			IField field= (IField) iter.next();
			Assert.isTrue(hasGetter(getterSetterMapping, field) || hasSetter(getterSetterMapping, field));
			String deleteGetterSetter= Messages.format(RefactoringCoreMessages.DeleteRefactoring_9, JavaElementUtil.createFieldSignature(field)); 
			if (getterSetterQuery.confirm(deleteGetterSetter)){
				if (hasGetter(getterSetterMapping, field))
					gettersSettersToAdd.add(getGetter(getterSetterMapping, field));
				if (hasSetter(getterSetterMapping, field))
					gettersSettersToAdd.add(getSetter(getterSetterMapping, field));
			}
		}
		return gettersSettersToAdd;
	}

	//note: modifies the mapping
	private void removeAlreadySelectedMethods(Map getterSetterMapping) {
		List elementsToDelete= Arrays.asList(fJavaElements);
		for (Iterator iter= getterSetterMapping.keySet().iterator(); iter.hasNext();) {
			IField field= (IField) iter.next();
			//remove getter
			IFunction getter= getGetter(getterSetterMapping, field);
			if (getter != null && elementsToDelete.contains(getter))
				removeGetterFromMapping(getterSetterMapping, field);

			//remove setter
			IFunction setter= getSetter(getterSetterMapping, field);
			if (setter != null && elementsToDelete.contains(setter))
				removeSetterFromMapping(getterSetterMapping, field);

			//both getter and setter already included
			if (! hasGetter(getterSetterMapping, field) && ! hasSetter(getterSetterMapping, field))
				iter.remove();
		}
	}

	/*
	 * IField -> IFunction[] (array of 2 - [getter, setter], one of which can be null)
	 */
	private static Map createGetterSetterMapping(IField[] fields) throws JavaScriptModelException {
		Map result= new HashMap();
		for (int i= 0; i < fields.length; i++) {
			IField field= fields[i];
			IFunction[] getterSetter= getGetterSetter(field);
			if (getterSetter != null)
				result.put(field, getterSetter);
		}		
		return result;
	}
	private static boolean hasSetter(Map getterSetterMapping, IField field){
		return getterSetterMapping.containsKey(field) && 
			   getSetter(getterSetterMapping, field) != null;
	}
	private static boolean hasGetter(Map getterSetterMapping, IField field){
		return getterSetterMapping.containsKey(field) && 
			   getGetter(getterSetterMapping, field) != null;
	}
	private static void removeGetterFromMapping(Map getterSetterMapping, IField field){
		((IFunction[])getterSetterMapping.get(field))[0]= null;
	}
	private static void removeSetterFromMapping(Map getterSetterMapping, IField field){
		((IFunction[])getterSetterMapping.get(field))[1]= null;
	}
	private static IFunction getGetter(Map getterSetterMapping, IField field){
		return ((IFunction[])getterSetterMapping.get(field))[0];
	}
	private static IFunction getSetter(Map getterSetterMapping, IField field){
		return ((IFunction[])getterSetterMapping.get(field))[1];
	}
	private static IField[] getFields(IJavaScriptElement[] elements){
		List fields= new ArrayList(3);
		for (int i= 0; i < elements.length; i++) {
			if (elements[i] instanceof IField)
				fields.add(elements[i]);
		}
		return (IField[]) fields.toArray(new IField[fields.size()]);
	}

	/*
	 * returns an array of 2 [getter, setter] or null if no getter or setter exists
	 */
	private static IFunction[] getGetterSetter(IField field) throws JavaScriptModelException {
		IFunction getter= GetterSetterUtil.getGetter(field);
		IFunction setter= GetterSetterUtil.getSetter(field);
		if ((getter != null && getter.exists()) || 	(setter != null && setter.exists()))
			return new IFunction[]{getter, setter};
		else
			return null;
	}

	//----------- read-only confirmation business ------
	private void confirmDeletingReadOnly() throws CoreException {
		if (! ReadOnlyResourceFinder.confirmDeleteOfReadOnlyElements(fJavaElements, fResources, fDeleteQueries))
			throw new OperationCanceledException(); //saying 'no' to this one is like cancelling the whole operation
	}

	//----------- empty CUs related method
	private void addEmptyCusToDelete() throws JavaScriptModelException {
		Set cusToEmpty= getCusToEmpty();
		addToSetToDelete((IJavaScriptUnit[]) cusToEmpty.toArray(new IJavaScriptUnit[cusToEmpty.size()]));
	}

	private Set getCusToEmpty() throws JavaScriptModelException {
		Set result= new HashSet();
		for (int i= 0; i < fJavaElements.length; i++) {
			IJavaScriptElement element= fJavaElements[i];	
			IJavaScriptUnit cu= ReorgUtils.getCompilationUnit(element);
			if (cu != null && ! result.contains(cu) && willHaveAllTopLevelTypesDeleted(cu))
				result.add(cu);
		}
		return result;
	}

	private boolean willHaveAllTopLevelTypesDeleted(IJavaScriptUnit cu) throws JavaScriptModelException {
		Set elementSet= new HashSet(Arrays.asList(fJavaElements));
		IType[] topLevelTypes= cu.getTypes();
		for (int i= 0; i < topLevelTypes.length; i++) {
			if (! elementSet.contains(topLevelTypes[i]))
				return false;
		}
		return true;
	}

	public boolean canEnableComment() {
		return true;
	}

	public String getComment() {
		return fComment;
	}

	public void setComment(String comment) {
		fComment= comment;
	}

	public RefactoringStatus initialize(RefactoringArguments arguments) {
		setQueries(new NullReorgQueries());
		final RefactoringStatus status= new RefactoringStatus();
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			final String subPackages= extended.getAttribute(ATTRIBUTE_DELETE_SUBPACKAGES);
			if (subPackages != null) {
				fDeleteSubPackages= Boolean.valueOf(subPackages).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DELETE_SUBPACKAGES));
			final String suggest= extended.getAttribute(ATTRIBUTE_SUGGEST_ACCESSORS);
			if (suggest != null) {
				fSuggestGetterSetterDeletion= Boolean.valueOf(suggest).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_SUGGEST_ACCESSORS));
			int resourceCount= 0;
			int elementCount= 0;
			String value= extended.getAttribute(ATTRIBUTE_RESOURCES);
			if (value != null && !"".equals(value)) {//$NON-NLS-1$
				try {
					resourceCount= Integer.parseInt(value);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_RESOURCES));
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_RESOURCES));
			value= extended.getAttribute(ATTRIBUTE_ELEMENTS);
			if (value != null && !"".equals(value)) {//$NON-NLS-1$
				try {
					elementCount= Integer.parseInt(value);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_ELEMENTS));
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_ELEMENTS));
			String handle= null;
			List elements= new ArrayList();
			for (int index= 0; index < resourceCount; index++) {
				final String attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (index + 1);
				handle= extended.getAttribute(attribute);
				if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
					final IResource resource= JDTRefactoringDescriptor.handleToResource(extended.getProject(), handle);
					if (resource == null || !resource.exists())
						status.merge(ScriptableRefactoring.createInputWarningStatus(resource, getRefactoring().getName(), IJavaScriptRefactorings.DELETE));
					else
						elements.add(resource);
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
			}
			fResources= (IResource[]) elements.toArray(new IResource[elements.size()]);
			elements= new ArrayList();
			for (int index= 0; index < elementCount; index++) {
				final String attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (resourceCount + index + 1);
				handle= extended.getAttribute(attribute);
				if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
					final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
					if (element == null || !element.exists())
						status.merge(ScriptableRefactoring.createInputWarningStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.DELETE));
					else
						elements.add(element);
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
			}
			fJavaElements= (IJavaScriptElement[]) elements.toArray(new IJavaScriptElement[elements.size()]);
			fElements= new Object[fResources.length + fJavaElements.length];
			System.arraycopy(fResources, 0, fElements, 0, fResources.length);
			System.arraycopy(fJavaElements, 0, fElements, fResources.length, fJavaElements.length);
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return status;
	}
}
