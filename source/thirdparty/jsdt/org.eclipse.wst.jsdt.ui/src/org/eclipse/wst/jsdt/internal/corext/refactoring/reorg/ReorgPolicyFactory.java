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
package org.eclipse.wst.jsdt.internal.corext.refactoring.reorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.CopyArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.participants.ValidateEditChecker;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IImportContainer;
import org.eclipse.wst.jsdt.core.IImportDeclaration;
import org.eclipse.wst.jsdt.core.IInitializer;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IOpenable;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.JavaScriptRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CopyCompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CopyPackageChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CopyPackageFragmentRootChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CopyResourceChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.MoveCompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.MovePackageChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.MovePackageFragmentRootChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.MoveResourceChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.IReorgPolicy.ICopyPolicy;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ImportRewriteUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.Changes;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.QualifiedNameFinder;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.QualifiedNameSearchResult;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.wst.jsdt.internal.corext.util.JavaElementResourceMapping;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

public final class ReorgPolicyFactory {

	private static final class ActualSelectionComputer {

		private final IJavaScriptElement[] fJavaElements;

		private final IResource[] fResources;

		public ActualSelectionComputer(IJavaScriptElement[] javaElements, IResource[] resources) {
			fJavaElements= javaElements;
			fResources= resources;
		}

		public IJavaScriptElement[] getActualJavaElementsToReorg() throws JavaScriptModelException {
			List result= new ArrayList();
			for (int i= 0; i < fJavaElements.length; i++) {
				IJavaScriptElement element= fJavaElements[i];
				if (element == null)
					continue;
//				if (element instanceof IType) {
//					IType type= (IType) element;
//					IJavaScriptUnit cu= type.getCompilationUnit();
//					if (cu != null && type.getDeclaringType() == null && cu.exists() && cu.getTypes().length == 1 && !result.contains(cu))
//						result.add(cu);
//					else if (!result.contains(type))
//						result.add(type);
//				} else 
					if (!result.contains(element)) {
					result.add(element);
				}
			}
			return (IJavaScriptElement[]) result.toArray(new IJavaScriptElement[result.size()]);
		}

		public IResource[] getActualResourcesToReorg() {
			Set javaElementSet= new HashSet(Arrays.asList(fJavaElements));
			List result= new ArrayList();
			for (int i= 0; i < fResources.length; i++) {
				if (fResources[i] == null)
					continue;
				IJavaScriptElement element= JavaScriptCore.create(fResources[i]);
				if (element == null || !element.exists() || !javaElementSet.contains(element))
					if (!result.contains(fResources[i]))
						result.add(fResources[i]);
			}
			return (IResource[]) result.toArray(new IResource[result.size()]);

		}
	}

	private static final class CopyFilesFoldersAndCusPolicy extends FilesFoldersAndCusReorgPolicy implements ICopyPolicy {

		private static final String POLICY_COPY_RESOURCE= "org.eclipse.wst.jsdt.ui.copyResources"; //$NON-NLS-1$

		private static Change copyCuToPackage(IJavaScriptUnit cu, IPackageFragment dest, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			// XXX workaround for bug 31998 we will have to disable renaming of
			// linked packages (and cus)
			IResource res= ReorgUtils.getResource(cu);
			if (res != null && res.isLinked()) {
				if (ResourceUtil.getResource(dest) instanceof IContainer)
					return copyFileToContainer(cu, (IContainer) ResourceUtil.getResource(dest), nameProposer, copyQueries);
			}

			String newName= nameProposer.createNewName(cu, dest);
			Change simpleCopy= new CopyCompilationUnitChange(cu, dest, copyQueries.createStaticQuery(newName));
			if (newName == null || newName.equals(cu.getElementName()))
				return simpleCopy;

			try {
				IPath newPath= cu.getResource().getParent().getFullPath().append(JavaModelUtil.getRenamedCUName(cu, newName));
				INewNameQuery nameQuery= copyQueries.createNewCompilationUnitNameQuery(cu, newName);
				return new CreateCopyOfCompilationUnitChange(newPath, cu.getSource(), cu, nameQuery);
			} catch (CoreException e) {
				// Using inferred change
				return simpleCopy;
			}
		}

		private static Change copyFileToContainer(IJavaScriptUnit cu, IContainer dest, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			IResource resource= ReorgUtils.getResource(cu);
			return createCopyResourceChange(resource, nameProposer, copyQueries, dest);
		}

		private static Change createCopyResourceChange(IResource resource, NewNameProposer nameProposer, INewNameQueries copyQueries, IContainer destination) {
			if (resource == null || destination == null)
				return new NullChange();
			INewNameQuery nameQuery;
			String name= nameProposer.createNewName(resource, destination);
			if (name == null)
				nameQuery= copyQueries.createNullQuery();
			else
				nameQuery= copyQueries.createNewResourceNameQuery(resource, name);
			return new CopyResourceChange(resource, destination, nameQuery);
		}

		private CopyModifications fModifications;

		private ReorgExecutionLog fReorgExecutionLog;

		CopyFilesFoldersAndCusPolicy(IFile[] files, IFolder[] folders, IJavaScriptUnit[] cus) {
			super(files, folders, cus);
		}

		private Change createChange(IJavaScriptUnit unit, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			IPackageFragment pack= getDestinationAsPackageFragment();
			if (pack != null)
				return copyCuToPackage(unit, pack, nameProposer, copyQueries);
			IContainer container= getDestinationAsContainer();
			return copyFileToContainer(unit, container, nameProposer, copyQueries);
		}

		public Change createChange(IProgressMonitor pm, INewNameQueries copyQueries) {
			IFile[] file= getFiles();
			IFolder[] folders= getFolders();
			IJavaScriptUnit[] cus= getCus();
			pm.beginTask("", cus.length + file.length + folders.length); //$NON-NLS-1$
			NewNameProposer nameProposer= new NewNameProposer();
			CompositeChange composite= new DynamicValidationStateChange(RefactoringCoreMessages.ReorgPolicy_copy);
			composite.markAsSynthetic();
			for (int i= 0; i < cus.length; i++) {
				composite.add(createChange(cus[i], nameProposer, copyQueries));
				pm.worked(1);
			}
			if (pm.isCanceled())
				throw new OperationCanceledException();
			for (int i= 0; i < file.length; i++) {
				composite.add(createChange(file[i], nameProposer, copyQueries));
				pm.worked(1);
			}
			if (pm.isCanceled())
				throw new OperationCanceledException();
			for (int i= 0; i < folders.length; i++) {
				composite.add(createChange(folders[i], nameProposer, copyQueries));
				pm.worked(1);
			}
			pm.done();
			return composite;
		}

		private Change createChange(IResource resource, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			IContainer dest= getDestinationAsContainer();
			return createCopyResourceChange(resource, nameProposer, copyQueries, dest);
		}

		protected JDTRefactoringDescriptor createRefactoringDescriptor(JDTRefactoringDescriptorComment comment, Map arguments, String description, String project, int flags) {
			return new JDTCopyRefactoringDescriptor(getReorgExecutionLog(), getProcessorId(), project, description, comment.asString(), arguments, flags);
		}

		protected String getDescriptionPlural() {
			final int kind= getContentKind();
			switch (kind) {
				case ONLY_FOLDERS:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_folders;
				case ONLY_FILES:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_files;
				case ONLY_CUS:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_compilation_units;
			}
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_description_plural;
		}

		protected String getDescriptionSingular() {
			final int kind= getContentKind();
			switch (kind) {
				case ONLY_FOLDERS:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_folder;
				case ONLY_FILES:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_file;
				case ONLY_CUS:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_compilation_unit;
			}
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_description_singular;
		}

		private Object getDestination() {
			Object result= getDestinationAsPackageFragment();
			if (result != null)
				return result;
			return getDestinationAsContainer();
		}

		protected String getHeaderPattern() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_header;
		}

		protected RefactoringModifications getModifications() throws CoreException {
			if (fModifications != null)
				return fModifications;
			fModifications= new CopyModifications();
			fReorgExecutionLog= new ReorgExecutionLog();
			CopyArguments jArgs= new CopyArguments(getDestination(), fReorgExecutionLog);
			CopyArguments rArgs= new CopyArguments(getDestinationAsContainer(), fReorgExecutionLog);
			IJavaScriptUnit[] cus= getCus();
			for (int i= 0; i < cus.length; i++) {
				fModifications.copy(cus[i], jArgs, rArgs);
			}
			IResource[] resources= ReorgUtils.union(getFiles(), getFolders());
			for (int i= 0; i < resources.length; i++) {
				fModifications.copy(resources[i], rArgs);
			}
			return fModifications;
		}

		public String getPolicyId() {
			return POLICY_COPY_RESOURCE;
		}

		protected String getProcessorId() {
			return IJavaScriptRefactorings.COPY;
		}

		protected String getRefactoringId() {
			return IJavaScriptRefactorings.COPY;
		}

		public ReorgExecutionLog getReorgExecutionLog() {
			return fReorgExecutionLog;
		}
	}

	private static final class CopyPackageFragmentRootsPolicy extends PackageFragmentRootsReorgPolicy implements ICopyPolicy {

		private static final String POLICY_COPY_ROOTS= "org.eclipse.wst.jsdt.ui.copyRoots"; //$NON-NLS-1$

		private CopyModifications fModifications;

		private ReorgExecutionLog fReorgExecutionLog;

		public CopyPackageFragmentRootsPolicy(IPackageFragmentRoot[] roots) {
			super(roots);
		}

		private Change createChange(IPackageFragmentRoot root, IJavaScriptProject destination, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			IResource res= root.getResource();
			IProject destinationProject= destination.getProject();
			String newName= nameProposer.createNewName(res, destinationProject);
			INewNameQuery nameQuery;
			if (newName == null)
				nameQuery= copyQueries.createNullQuery();
			else
				nameQuery= copyQueries.createNewPackageFragmentRootNameQuery(root, newName);
			// TODO fix the query problem
			return new CopyPackageFragmentRootChange(root, destinationProject, nameQuery, null);
		}

		public Change createChange(IProgressMonitor pm, INewNameQueries copyQueries) {
			NewNameProposer nameProposer= new NewNameProposer();
			IPackageFragmentRoot[] roots= getPackageFragmentRoots();
			pm.beginTask("", roots.length); //$NON-NLS-1$
			CompositeChange composite= new DynamicValidationStateChange(RefactoringCoreMessages.ReorgPolicy_copy_source_folder);
			composite.markAsSynthetic();
			IJavaScriptProject destination= getDestinationJavaProject();
			Assert.isNotNull(destination);
			for (int i= 0; i < roots.length; i++) {
				composite.add(createChange(roots[i], destination, nameProposer, copyQueries));
				pm.worked(1);
			}
			pm.done();
			return composite;
		}

		protected JDTRefactoringDescriptor createRefactoringDescriptor(JDTRefactoringDescriptorComment comment, Map arguments, String description, String project, int flags) {
			return new JDTCopyRefactoringDescriptor(getReorgExecutionLog(), getProcessorId(), project, description, comment.asString(), arguments, flags);
		}

		protected String getDescriptionPlural() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_roots_plural;
		}

		protected String getDescriptionSingular() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_roots_singular;
		}

		protected String getHeaderPattern() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_roots_header;
		}

		protected RefactoringModifications getModifications() throws CoreException {
			if (fModifications != null)
				return fModifications;

			fModifications= new CopyModifications();
			fReorgExecutionLog= new ReorgExecutionLog();
			CopyArguments javaArgs= new CopyArguments(getDestinationJavaProject(), fReorgExecutionLog);
			CopyArguments resourceArgs= new CopyArguments(getDestinationJavaProject().getProject(), fReorgExecutionLog);
			IPackageFragmentRoot[] roots= getRoots();
			for (int i= 0; i < roots.length; i++) {
				fModifications.copy(roots[i], javaArgs, resourceArgs);
			}
			return fModifications;
		}

		public String getPolicyId() {
			return POLICY_COPY_ROOTS;
		}

		protected String getProcessorId() {
			return IJavaScriptRefactorings.COPY;
		}

		protected String getRefactoringId() {
			return IJavaScriptRefactorings.COPY;
		}

		public ReorgExecutionLog getReorgExecutionLog() {
			return fReorgExecutionLog;
		}
	}

	private static final class CopyPackagesPolicy extends PackagesReorgPolicy implements ICopyPolicy {

		private static final String POLICY_COPY_PACKAGES= "org.eclipse.wst.jsdt.ui.copyPackages"; //$NON-NLS-1$

		private CopyModifications fModifications;

		private ReorgExecutionLog fReorgExecutionLog;

		public CopyPackagesPolicy(IPackageFragment[] packageFragments) {
			super(packageFragments);
		}

		private Change createChange(IPackageFragment pack, IPackageFragmentRoot destination, NewNameProposer nameProposer, INewNameQueries copyQueries) {
			String newName= nameProposer.createNewName(pack, destination);
			if (newName == null || JavaScriptConventions.validatePackageName(newName).getSeverity() < IStatus.ERROR) {
				INewNameQuery nameQuery;
				if (newName == null)
					nameQuery= copyQueries.createNullQuery();
				else
					nameQuery= copyQueries.createNewPackageNameQuery(pack, newName);
				return new CopyPackageChange(pack, destination, nameQuery);
			} else {
				if (destination.getResource() instanceof IContainer) {
					IContainer dest= (IContainer) destination.getResource();
					IResource res= pack.getResource();
					INewNameQuery nameQuery= copyQueries.createNewResourceNameQuery(res, newName);
					return new CopyResourceChange(res, dest, nameQuery);
				} else {
					return new NullChange();
				}
			}
		}

		public Change createChange(IProgressMonitor pm, INewNameQueries newNameQueries) throws JavaScriptModelException {
			NewNameProposer nameProposer= new NewNameProposer();
			IPackageFragment[] fragments= getPackages();
			pm.beginTask("", fragments.length); //$NON-NLS-1$
			CompositeChange composite= new DynamicValidationStateChange(RefactoringCoreMessages.ReorgPolicy_copy_package);
			composite.markAsSynthetic();
			IPackageFragmentRoot root= getDestinationAsPackageFragmentRoot();
			for (int i= 0; i < fragments.length; i++) {
				composite.add(createChange(fragments[i], root, nameProposer, newNameQueries));
				pm.worked(1);
			}
			pm.done();
			return composite;
		}

		protected JDTRefactoringDescriptor createRefactoringDescriptor(JDTRefactoringDescriptorComment comment, Map arguments, String description, String project, int flags) {
			return new JDTCopyRefactoringDescriptor(getReorgExecutionLog(), getProcessorId(), project, description, comment.asString(), arguments, flags);
		}

		protected String getDescriptionPlural() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_packages_plural;
		}

		protected String getDescriptionSingular() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_package_singular;
		}

		protected String getHeaderPattern() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_packages_header;
		}

		protected RefactoringModifications getModifications() throws CoreException {
			if (fModifications != null)
				return fModifications;

			fModifications= new CopyModifications();
			fReorgExecutionLog= new ReorgExecutionLog();
			IPackageFragmentRoot destination= getDestinationAsPackageFragmentRoot();
			CopyArguments javaArgs= new CopyArguments(destination, fReorgExecutionLog);
			CopyArguments resourceArgs= new CopyArguments(destination.getResource(), fReorgExecutionLog);
			IPackageFragment[] packages= getPackages();
			for (int i= 0; i < packages.length; i++) {
				fModifications.copy(packages[i], javaArgs, resourceArgs);
			}
			return fModifications;
		}

		public String getPolicyId() {
			return POLICY_COPY_PACKAGES;
		}

		protected String getProcessorId() {
			return IJavaScriptRefactorings.COPY;
		}

		protected String getRefactoringId() {
			return IJavaScriptRefactorings.COPY;
		}

		public ReorgExecutionLog getReorgExecutionLog() {
			return fReorgExecutionLog;
		}
	}

	private static final class CopySubCuElementsPolicy extends SubCuElementReorgPolicy implements ICopyPolicy {

		private static final String POLICY_COPY_MEMBERS= "org.eclipse.wst.jsdt.ui.copyMembers"; //$NON-NLS-1$

		private CopyModifications fModifications;

		private ReorgExecutionLog fReorgExecutionLog;

		CopySubCuElementsPolicy(IJavaScriptElement[] javaElements) {
			super(javaElements);
		}

		public boolean canEnable() throws JavaScriptModelException {
			return super.canEnable() && (getSourceCu() != null || getSourceClassFile() != null);
		}

		public Change createChange(IProgressMonitor pm, INewNameQueries copyQueries) throws JavaScriptModelException {
			try {
				JavaScriptUnit sourceCuNode= createSourceCuNode();
				IJavaScriptUnit targetCu= getDestinationCu();
				CompilationUnitRewrite targetRewriter= new CompilationUnitRewrite(targetCu);
				IJavaScriptElement[] javaElements= getJavaElements();
				for (int i= 0; i < javaElements.length; i++) {
					copyToDestination(javaElements[i], targetRewriter, sourceCuNode, targetRewriter.getRoot());
				}
				return createCompilationUnitChange(targetCu, targetRewriter);
			} catch (JavaScriptModelException e) {
				throw e;
			} catch (CoreException e) {
				throw new JavaScriptModelException(e);
			}
		}

		protected JDTRefactoringDescriptor createRefactoringDescriptor(JDTRefactoringDescriptorComment comment, Map arguments, String description, String project, int flags) {
			return new JDTCopyRefactoringDescriptor(getReorgExecutionLog(), getProcessorId(), project, description, comment.asString(), arguments, flags);
		}

		private JavaScriptUnit createSourceCuNode() {
			Assert.isTrue(getSourceCu() != null || getSourceClassFile() != null);
			Assert.isTrue(getSourceCu() == null || getSourceClassFile() == null);
			ASTParser parser= ASTParser.newParser(AST.JLS3);
			if (getSourceCu() != null)
				parser.setSource(getSourceCu());
			else
				parser.setSource(getSourceClassFile());
			return (JavaScriptUnit) parser.createAST(null);
		}

		public IFile[] getAllModifiedFiles() {
			return ReorgUtils.getFiles(new IResource[] { ReorgUtils.getResource(getDestinationCu())});
		}

		protected String getDescriptionPlural() {
			final int kind= getContentKind();
			switch (kind) {
				case ONLY_TYPES:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_types;
				case ONLY_FIELDS:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_fields;
				case ONLY_METHODS:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_methods;
				case ONLY_INITIALIZERS:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_initializers;
				case ONLY_PACKAGE_DECLARATIONS:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_package_declarations;
				case ONLY_IMPORT_CONTAINERS:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_import_containers;
				case ONLY_IMPORT_DECLARATIONS:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_imports;
			}
			return RefactoringCoreMessages.ReorgPolicyFactory_move_elements_plural;
		}

		protected String getDescriptionSingular() {
			final int kind= getContentKind();
			switch (kind) {
				case ONLY_TYPES:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_type;
				case ONLY_FIELDS:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_field;
				case ONLY_METHODS:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_method;
				case ONLY_INITIALIZERS:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_initializer;
				case ONLY_PACKAGE_DECLARATIONS:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_package;
				case ONLY_IMPORT_CONTAINERS:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_import_section;
				case ONLY_IMPORT_DECLARATIONS:
					return RefactoringCoreMessages.ReorgPolicyFactory_copy_import;
			}
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_elements_singular;
		}

		protected String getHeaderPattern() {
			return RefactoringCoreMessages.ReorgPolicyFactory_copy_elements_header;
		}

		protected RefactoringModifications getModifications() throws CoreException {
			if (fModifications != null)
				return fModifications;

			fModifications= new CopyModifications();
			fReorgExecutionLog= new ReorgExecutionLog();
			CopyArguments args= new CopyArguments(getJavaElementDestination(), fReorgExecutionLog);
			IJavaScriptElement[] javaElements= getJavaElements();
			for (int i= 0; i < javaElements.length; i++) {
				fModifications.copy(javaElements[i], args, null);
			}
			return fModifications;
		}

		public String getPolicyId() {
			return POLICY_COPY_MEMBERS;
		}

		protected String getProcessorId() {
			return IJavaScriptRefactorings.COPY;
		}

		protected String getRefactoringId() {
			return IJavaScriptRefactorings.COPY;
		}

		public ReorgExecutionLog getReorgExecutionLog() {
			return fReorgExecutionLog;
		}

		private IClassFile getSourceClassFile() {
			// all have a common parent, so all must be in the same classfile
			// we checked before that the array in not null and not empty
			return (IClassFile) getJavaElements()[0].getAncestor(IJavaScriptElement.CLASS_FILE);
		}
	}

	private static abstract class FilesFoldersAndCusReorgPolicy extends ReorgPolicy {

		protected static final int ONLY_CUS= 2;

		protected static final int ONLY_FILES= 1;

		protected static final int ONLY_FOLDERS= 0;

		private static IContainer getAsContainer(IResource resDest) {
			if (resDest instanceof IContainer)
				return (IContainer) resDest;
			if (resDest instanceof IFile)
				return ((IFile) resDest).getParent();
			return null;
		}

		private IJavaScriptUnit[] fCus;

		private IFile[] fFiles;

		private IFolder[] fFolders;

		public FilesFoldersAndCusReorgPolicy(IFile[] files, IFolder[] folders, IJavaScriptUnit[] cus) {
			fFiles= files;
			fFolders= folders;
			fCus= cus;
		}

		public boolean canChildrenBeDestinations(IJavaScriptElement javaElement) {
			switch (javaElement.getElementType()) {
				case IJavaScriptElement.JAVASCRIPT_MODEL:
				case IJavaScriptElement.JAVASCRIPT_PROJECT:
				case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
					return true;
				default:
					return false;
			}
		}

		public boolean canChildrenBeDestinations(IResource resource) {
			return resource instanceof IContainer;
		}

		public boolean canElementBeDestination(IJavaScriptElement javaElement) {
			switch (javaElement.getElementType()) {
				case IJavaScriptElement.PACKAGE_FRAGMENT:
					return true;
				default:
					return false;
			}
		}

		public boolean canElementBeDestination(IResource resource) {
			return resource instanceof IProject || resource instanceof IFolder;
		}

		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws CoreException {
			RefactoringStatus status= super.checkFinalConditions(pm, context, reorgQueries);
			confirmOverwriting(reorgQueries);
			return status;
		}

		private void confirmOverwriting(IReorgQueries reorgQueries) {
			OverwriteHelper helper= new OverwriteHelper();
			helper.setFiles(fFiles);
			helper.setFolders(fFolders);
			helper.setCus(fCus);
			IPackageFragment destPack= getDestinationAsPackageFragment();
			if (destPack != null) {
				helper.confirmOverwriting(reorgQueries, destPack);
			} else {
				IContainer destinationAsContainer= getDestinationAsContainer();
				if (destinationAsContainer != null)
					helper.confirmOverwriting(reorgQueries, destinationAsContainer);
			}
			fFiles= helper.getFilesWithoutUnconfirmedOnes();
			fFolders= helper.getFoldersWithoutUnconfirmedOnes();
			fCus= helper.getCusWithoutUnconfirmedOnes();
		}

		protected boolean containsLinkedResources() {
			return ReorgUtils.containsLinkedResources(fFiles) || ReorgUtils.containsLinkedResources(fFolders) || ReorgUtils.containsLinkedResources(fCus);
		}

		protected JDTRefactoringDescriptor createRefactoringDescriptor(final JDTRefactoringDescriptorComment comment, final Map arguments, final String description, final String project, int flags) {
			return new JDTRefactoringDescriptor(getProcessorId(), project, description, comment.asString(), arguments, flags);
		}

		protected final int getContentKind() {
			final int length= fCus.length + fFiles.length + fFolders.length;
			if (length == fCus.length)
				return ONLY_CUS;
			else if (length == fFiles.length)
				return ONLY_FILES;
			else if (length == fFolders.length)
				return ONLY_FOLDERS;
			return -1;
		}

		protected final IJavaScriptUnit[] getCus() {
			return fCus;
		}

		public final ChangeDescriptor getDescriptor() {
			final Map arguments= new HashMap();
			final int length= fFiles.length + fFolders.length + fCus.length;
			final String description= length == 1 ? getDescriptionSingular() : getDescriptionPlural();
			final IProject resource= getSingleProject();
			final String project= resource != null ? resource.getName() : null;
			final String header= Messages.format(getHeaderPattern(), new String[] { String.valueOf(length), getDestinationLabel()});
			int flags= JavaScriptRefactoringDescriptor.JAR_MIGRATION | JavaScriptRefactoringDescriptor.JAR_REFACTORING | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
			final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
			final JDTRefactoringDescriptor descriptor= createRefactoringDescriptor(comment, arguments, description, project, flags);
			arguments.put(ATTRIBUTE_POLICY, getPolicyId());
			arguments.put(ATTRIBUTE_FILES, Integer.valueOf(fFiles.length).toString());
			for (int offset= 0; offset < fFiles.length; offset++)
				arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + 1), descriptor.resourceToHandle(fFiles[offset]));
			arguments.put(ATTRIBUTE_FOLDERS, Integer.valueOf(fFolders.length).toString());
			for (int offset= 0; offset < fFolders.length; offset++)
				arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + fFiles.length + 1), descriptor.resourceToHandle(fFolders[offset]));
			arguments.put(ATTRIBUTE_UNITS, Integer.valueOf(fCus.length).toString());
			for (int offset= 0; offset < fCus.length; offset++)
				arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + fFolders.length + fFiles.length + 1), descriptor.elementToHandle(fCus[offset]));
			arguments.putAll(getRefactoringArguments(project));
			return new RefactoringChangeDescriptor(descriptor);
		}

		protected final IContainer getDestinationAsContainer() {
			IResource resDest= getResourceDestination();
			if (resDest != null)
				return getAsContainer(resDest);
			IJavaScriptElement jelDest= getJavaElementDestination();
			Assert.isNotNull(jelDest);
			return getAsContainer(ReorgUtils.getResource(jelDest));
		}

		protected final IPackageFragment getDestinationAsPackageFragment() {
			IPackageFragment javaAsPackage= getJavaDestinationAsPackageFragment(getJavaElementDestination());
			if (javaAsPackage != null)
				return javaAsPackage;
			return getResourceDestinationAsPackageFragment(getResourceDestination());
		}

		protected final IJavaScriptElement getDestinationContainerAsJavaElement() {
			if (getJavaElementDestination() != null)
				return getJavaElementDestination();
			IContainer destinationAsContainer= getDestinationAsContainer();
			if (destinationAsContainer == null)
				return null;
			IJavaScriptElement je= JavaScriptCore.create(destinationAsContainer);
			if (je != null && je.exists())
				return je;
			return null;
		}

		protected final IFile[] getFiles() {
			return fFiles;
		}

		protected final IFolder[] getFolders() {
			return fFolders;
		}

		private IPackageFragment getJavaDestinationAsPackageFragment(IJavaScriptElement javaDest) {
			if (javaDest == null || (fCheckDestination && !javaDest.exists()))
				return null;
			if (javaDest instanceof IPackageFragment)
				return (IPackageFragment) javaDest;
			if (javaDest instanceof IPackageFragmentRoot)
				return ((IPackageFragmentRoot) javaDest).getPackageFragment(""); //$NON-NLS-1$
			if (javaDest instanceof IJavaScriptProject) {
				try {
					IPackageFragmentRoot root= ReorgUtils.getCorrespondingPackageFragmentRoot((IJavaScriptProject) javaDest);
					if (root != null)
						return root.getPackageFragment(""); //$NON-NLS-1$
				} catch (JavaScriptModelException e) {
					// fall through
				}
			}
			return (IPackageFragment) javaDest.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT);
		}

		public final IJavaScriptElement[] getJavaElements() {
			return fCus;
		}

		private IPackageFragment getResourceDestinationAsPackageFragment(IResource resource) {
			if (resource instanceof IFile)
				return getJavaDestinationAsPackageFragment(JavaScriptCore.create(resource.getParent()));
			return null;
		}

		public final IResource[] getResources() {
			return ReorgUtils.union(fFiles, fFolders);
		}

		private IProject getSingleProject() {
			IProject result= null;
			for (int index= 0; index < fFiles.length; index++) {
				if (result == null)
					result= fFiles[index].getProject();
				else if (!result.equals(fFiles[index].getProject()))
					return null;
			}
			for (int index= 0; index < fFolders.length; index++) {
				if (result == null)
					result= fFolders[index].getProject();
				else if (!result.equals(fFolders[index].getProject()))
					return null;
			}
			for (int index= 0; index < fCus.length; index++) {
				if (result == null)
					result= fCus[index].getJavaScriptProject().getProject();
				else if (!result.equals(fCus[index].getJavaScriptProject().getProject()))
					return null;
			}
			return result;
		}

		public RefactoringStatus initialize(RefactoringArguments arguments) {
			final RefactoringStatus status= new RefactoringStatus();
			if (arguments instanceof JavaRefactoringArguments) {
				final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
				int fileCount= 0;
				int folderCount= 0;
				int unitCount= 0;
				String value= extended.getAttribute(ATTRIBUTE_FILES);
				if (value != null && !"".equals(value)) {//$NON-NLS-1$
					try {
						fileCount= Integer.parseInt(value);
					} catch (NumberFormatException exception) {
						return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_FILES));
					}
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_FILES));
				value= extended.getAttribute(ATTRIBUTE_FOLDERS);
				if (value != null && !"".equals(value)) {//$NON-NLS-1$
					try {
						folderCount= Integer.parseInt(value);
					} catch (NumberFormatException exception) {
						return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_FOLDERS));
					}
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_FOLDERS));
				value= extended.getAttribute(ATTRIBUTE_UNITS);
				if (value != null && !"".equals(value)) {//$NON-NLS-1$
					try {
						unitCount= Integer.parseInt(value);
					} catch (NumberFormatException exception) {
						return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_UNITS));
					}
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_UNITS));
				String handle= null;
				List elements= new ArrayList();
				for (int index= 0; index < fileCount; index++) {
					final String attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (index + 1);
					handle= extended.getAttribute(attribute);
					if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
						final IResource resource= JDTRefactoringDescriptor.handleToResource(extended.getProject(), handle);
						if (resource == null || !resource.exists())
							status.merge(ScriptableRefactoring.createInputWarningStatus(resource, getProcessorId(), getRefactoringId()));
						else
							elements.add(resource);
					} else
						return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
				}
				fFiles= (IFile[]) elements.toArray(new IFile[elements.size()]);
				elements= new ArrayList();
				for (int index= 0; index < folderCount; index++) {
					final String attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (fileCount + index + 1);
					handle= extended.getAttribute(attribute);
					if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
						final IResource resource= JDTRefactoringDescriptor.handleToResource(extended.getProject(), handle);
						if (resource == null || !resource.exists())
							status.merge(ScriptableRefactoring.createInputWarningStatus(resource, getProcessorId(), getRefactoringId()));
						else
							elements.add(resource);
					} else
						return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
				}
				fFolders= (IFolder[]) elements.toArray(new IFolder[elements.size()]);
				elements= new ArrayList();
				for (int index= 0; index < unitCount; index++) {
					final String attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (folderCount + fileCount + index + 1);
					handle= extended.getAttribute(attribute);
					if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
						final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
						if (element == null || !element.exists() || element.getElementType() != IJavaScriptElement.JAVASCRIPT_UNIT)
							status.merge(ScriptableRefactoring.createInputWarningStatus(element, getProcessorId(), getRefactoringId()));
						else
							elements.add(element);
					} else
						return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
				}
				fCus= (IJavaScriptUnit[]) elements.toArray(new IJavaScriptUnit[elements.size()]);
			} else
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
			status.merge(super.initialize(arguments));
			return status;
		}

		private boolean isChildOfOrEqualToAnyFolder(IResource resource) {
			for (int i= 0; i < fFolders.length; i++) {
				IFolder folder= fFolders[i];
				if (folder.equals(resource) || ParentChecker.isDescendantOf(resource, folder))
					return true;
			}
			return false;
		}

		protected RefactoringStatus verifyDestination(IJavaScriptElement javaElement) throws JavaScriptModelException {
			Assert.isNotNull(javaElement);
			if (!fCheckDestination)
				return new RefactoringStatus();
			if (!javaElement.exists())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_doesnotexist0);
			if (javaElement instanceof IJavaScriptModel)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_jmodel);

			if (javaElement.isReadOnly())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_readonly);

			if (!javaElement.isStructureKnown())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_structure);

			if (javaElement instanceof IOpenable) {
				IOpenable openable= (IOpenable) javaElement;
				if (!openable.isConsistent())
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_inconsistent);
			}

			if (javaElement instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot root= (IPackageFragmentRoot) javaElement;
				if (root.isArchive())
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_archive);
				if (root.isExternal())
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_external);
			}

			if (ReorgUtils.isInsideCompilationUnit(javaElement)) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_cannot);
			}

			IContainer destinationAsContainer= getDestinationAsContainer();
			if (destinationAsContainer == null || isChildOfOrEqualToAnyFolder(destinationAsContainer))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_not_this_resource);

			if (containsLinkedResources() && !ReorgUtils.canBeDestinationForLinkedResources(javaElement))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_linked);
			return new RefactoringStatus();
		}

		protected RefactoringStatus verifyDestination(IResource resource) throws JavaScriptModelException {
			Assert.isNotNull(resource);
			if (!resource.exists() || resource.isPhantom())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_phantom);
			if (!resource.isAccessible())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_inaccessible);
			
			if (resource.getType() == IResource.ROOT)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_not_this_resource);

			if (isChildOfOrEqualToAnyFolder(resource))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_not_this_resource);

			if (containsLinkedResources() && !ReorgUtils.canBeDestinationForLinkedResources(resource))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_linked);

			return new RefactoringStatus();
		}
	}

	private static final class MoveFilesFoldersAndCusPolicy extends FilesFoldersAndCusReorgPolicy implements IMovePolicy {

		private static final String POLICY_MOVE_RESOURCES= "org.eclipse.wst.jsdt.ui.moveResources"; //$NON-NLS-1$

		private static Change moveCuToPackage(IJavaScriptUnit cu, IPackageFragment dest) {
			// XXX workaround for bug 31998 we will have to disable renaming of
			// linked packages (and cus)
			IResource resource= cu.getResource();
			if (resource != null && resource.isLinked()) {
				if (ResourceUtil.getResource(dest) instanceof IContainer)
					return moveFileToContainer(cu, (IContainer) ResourceUtil.getResource(dest));
			}
			return new MoveCompilationUnitChange(cu, dest);
		}

		private static Change moveFileToContainer(IJavaScriptUnit cu, IContainer dest) {
			return new MoveResourceChange(cu.getResource(), dest);
		}

		private TextChangeManager fChangeManager;

		private CreateTargetExecutionLog fCreateTargetExecutionLog= new CreateTargetExecutionLog();

		private String fFilePatterns;

		private MoveModifications fModifications;

		private QualifiedNameSearchResult fQualifiedNameSearchResult;

		private boolean fUpdateQualifiedNames;

		private boolean fUpdateReferences;

		MoveFilesFoldersAndCusPolicy(IFile[] files, IFolder[] folders, IJavaScriptUnit[] cus) {
			super(files, folders, cus);
			fUpdateReferences= true;
			fUpdateQualifiedNames= false;
			fQualifiedNameSearchResult= new QualifiedNameSearchResult();
		}

		public boolean canEnableQualifiedNameUpdating() {
			return getCus().length > 0 && !JavaElementUtil.isDefaultPackage(getCommonParent());
		}

		public boolean canEnableUpdateReferences() {
			return getCus().length > 0;
		}

		public boolean canUpdateQualifiedNames() {
			IPackageFragment pack= getDestinationAsPackageFragment();
			return (canEnableQualifiedNameUpdating() && pack != null && !pack.isDefaultPackage());
		}

		public boolean canUpdateReferences() {
			if (getCus().length == 0)
				return false;
			IPackageFragment pack= getDestinationAsPackageFragment();
			if (pack != null && pack.isDefaultPackage())
				return false;
			Object commonParent= getCommonParent();
			if (JavaElementUtil.isDefaultPackage(commonParent))
				return false;
			return true;
		}

		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws CoreException {
			try {
				pm.beginTask("", fUpdateQualifiedNames ? 7 : 3); //$NON-NLS-1$
				RefactoringStatus result= new RefactoringStatus();
				confirmMovingReadOnly(reorgQueries);
				fChangeManager= createChangeManager(new SubProgressMonitor(pm, 2), result);
				if (fUpdateQualifiedNames)
					computeQualifiedNameMatches(new SubProgressMonitor(pm, 4));
				result.merge(super.checkFinalConditions(new SubProgressMonitor(pm, 1), context, reorgQueries));
				return result;
			} catch (JavaScriptModelException e) {
				throw e;
			} catch (CoreException e) {
				throw new JavaScriptModelException(e);
			} finally {
				pm.done();
			}
		}

		private void computeQualifiedNameMatches(IProgressMonitor pm) throws JavaScriptModelException {
			if (!fUpdateQualifiedNames)
				return;
			IPackageFragment destination= getDestinationAsPackageFragment();
			if (destination != null) {
				IJavaScriptUnit[] cus= getCus();
				pm.beginTask("", cus.length); //$NON-NLS-1$
				pm.subTask(RefactoringCoreMessages.MoveRefactoring_scanning_qualified_names);
//				for (int i= 0; i < cus.length; i++) {
//					IJavaScriptUnit cu= cus[i];
//					IType[] types= cu.getTypes();
//					IProgressMonitor typesMonitor= new SubProgressMonitor(pm, 1);
//					typesMonitor.beginTask("", types.length); //$NON-NLS-1$
//					for (int j= 0; j < types.length; j++) {
//						handleType(types[j], destination, new SubProgressMonitor(typesMonitor, 1));
//						if (typesMonitor.isCanceled())
//							throw new OperationCanceledException();
//					}
//					typesMonitor.done();
//				}
			}
			pm.done();
		}

		private void confirmMovingReadOnly(IReorgQueries reorgQueries) throws CoreException {
			if (!ReadOnlyResourceFinder.confirmMoveOfReadOnlyElements(getJavaElements(), getResources(), reorgQueries))
				throw new OperationCanceledException();
		}

		private Change createChange(IJavaScriptUnit cu) {
			IPackageFragment pack= getDestinationAsPackageFragment();
			if (pack != null)
				return moveCuToPackage(cu, pack);
			IContainer container= getDestinationAsContainer();
			if (container == null)
				return new NullChange();
			return moveFileToContainer(cu, container);
		}

		public Change createChange(IProgressMonitor pm) throws JavaScriptModelException {
			if (!fUpdateReferences) {
				return createSimpleMoveChange(pm);
			} else {
				return createReferenceUpdatingMoveChange(pm);
			}
		}

		private Change createChange(IResource res) {
			IContainer destinationAsContainer= getDestinationAsContainer();
			if (destinationAsContainer == null)
				return new NullChange();
			return new MoveResourceChange(res, destinationAsContainer);
		}

		private TextChangeManager createChangeManager(IProgressMonitor pm, RefactoringStatus status) throws JavaScriptModelException {
			pm.beginTask("", 1);//$NON-NLS-1$
			try {
				if (!fUpdateReferences)
					return new TextChangeManager();

				IPackageFragment packageDest= getDestinationAsPackageFragment();
				if (packageDest != null) {
					MoveCuUpdateCreator creator= new MoveCuUpdateCreator(getCus(), packageDest);
					return creator.createChangeManager(new SubProgressMonitor(pm, 1), status);
				} else
					return new TextChangeManager();
			} finally {
				pm.done();
			}
		}

		protected JDTRefactoringDescriptor createRefactoringDescriptor(JDTRefactoringDescriptorComment comment, Map arguments, String description, String project, int flags) {
			return new JDTMoveRefactoringDescriptor(getCreateTargetExecutionLog(), getProcessorId(), project, description, comment.asString(), arguments, flags);
		}

		private Change createReferenceUpdatingMoveChange(IProgressMonitor pm) throws JavaScriptModelException {
			pm.beginTask("", 2 + (fUpdateQualifiedNames ? 1 : 0)); //$NON-NLS-1$
			try {
				CompositeChange composite= new DynamicValidationStateChange(RefactoringCoreMessages.ReorgPolicy_move);
				composite.markAsSynthetic();
				// XX workaround for bug 13558
				// <workaround>
				if (fChangeManager == null) {
					fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1), new RefactoringStatus());
					// TODO: non-CU matches silently dropped
					RefactoringStatus status= Checks.validateModifiesFiles(getAllModifiedFiles(), null);
					if (status.hasFatalError())
						fChangeManager= new TextChangeManager();
				}
				// </workaround>

				composite.merge(new CompositeChange(RefactoringCoreMessages.MoveRefactoring_reorganize_elements, fChangeManager.getAllChanges()));

				Change fileMove= createSimpleMoveChange(new SubProgressMonitor(pm, 1));
				if (fileMove instanceof CompositeChange) {
					composite.merge(((CompositeChange) fileMove));
				} else {
					composite.add(fileMove);
				}
				return composite;
			} finally {
				pm.done();
			}
		}

		private Change createSimpleMoveChange(IProgressMonitor pm) {
			CompositeChange result= new DynamicValidationStateChange(RefactoringCoreMessages.ReorgPolicy_move);
			result.markAsSynthetic();
			IFile[] files= getFiles();
			IFolder[] folders= getFolders();
			IJavaScriptUnit[] cus= getCus();
			pm.beginTask("", files.length + folders.length + cus.length); //$NON-NLS-1$
			for (int i= 0; i < files.length; i++) {
				result.add(createChange(files[i]));
				pm.worked(1);
			}
			if (pm.isCanceled())
				throw new OperationCanceledException();
			for (int i= 0; i < folders.length; i++) {
				result.add(createChange(folders[i]));
				pm.worked(1);
			}
			if (pm.isCanceled())
				throw new OperationCanceledException();
			for (int i= 0; i < cus.length; i++) {
				result.add(createChange(cus[i]));
				pm.worked(1);
			}
			pm.done();
			return result;
		}

		public IFile[] getAllModifiedFiles() {
			Set result= new HashSet();
			result.addAll(Arrays.asList(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits())));
			result.addAll(Arrays.asList(fQualifiedNameSearchResult.getAllFiles()));
			if (getDestinationAsPackageFragment() != null && getUpdateReferences())
				result.addAll(Arrays.asList(ResourceUtil.getFiles(getCus())));
			return (IFile[]) result.toArray(new IFile[result.size()]);
		}

		private Object getCommonParent() {
			return new ParentChecker(getResources(), getJavaElements()).getCommonParent();
		}

		public CreateTargetExecutionLog getCreateTargetExecutionLog() {
			return fCreateTargetExecutionLog;
		}

		public ICreateTargetQuery getCreateTargetQuery(ICreateTargetQueries createQueries) {
			return createQueries.createNewPackageQuery();
		}

		protected String getDescriptionPlural() {
			final int kind= getContentKind();
			switch (kind) {
				case ONLY_FOLDERS:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_folders;
				case ONLY_FILES:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_files;
				case ONLY_CUS:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_compilation_units;
			}
			return RefactoringCoreMessages.ReorgPolicyFactory_move_description_plural;
		}

		protected String getDescriptionSingular() {
			final int kind= getContentKind();
			switch (kind) {
				case ONLY_FOLDERS:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_folder;
				case ONLY_FILES:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_file;
				case ONLY_CUS:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_compilation_unit;
			}
			return RefactoringCoreMessages.ReorgPolicyFactory_move_description_singular;
		}

		public String getFilePatterns() {
			return fFilePatterns;
		}

		protected String getHeaderPattern() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_header;
		}

		protected RefactoringModifications getModifications() throws CoreException {
			if (fModifications != null)
				return fModifications;

			fModifications= new MoveModifications();
			IPackageFragment pack= getDestinationAsPackageFragment();
			IContainer container= getDestinationAsContainer();
			Object unitDestination= null;
			if (pack != null)
				unitDestination= pack;
			else
				unitDestination= container;

			// don't use fUpdateReferences directly since it is only valid if
			// canUpdateReferences is true
			boolean updateReferenes= canUpdateReferences() && getUpdateReferences();
			if (unitDestination != null) {
				IJavaScriptUnit[] units= getCus();
				for (int i= 0; i < units.length; i++) {
					fModifications.move(units[i], new MoveArguments(unitDestination, updateReferenes));
				}
			}
			if (container != null) {
				IFile[] files= getFiles();
				for (int i= 0; i < files.length; i++) {
					fModifications.move(files[i], new MoveArguments(container, updateReferenes));
				}
				IFolder[] folders= getFolders();
				for (int i= 0; i < folders.length; i++) {
					fModifications.move(folders[i], new MoveArguments(container, updateReferenes));
				}
			}
			return fModifications;
		}

		public String getPolicyId() {
			return POLICY_MOVE_RESOURCES;
		}

		protected String getProcessorId() {
			return IJavaScriptRefactorings.MOVE;
		}

		protected Map getRefactoringArguments(String project) {
			final Map arguments= new HashMap();
			arguments.putAll(super.getRefactoringArguments(project));
			if (fFilePatterns != null && !"".equals(fFilePatterns)) //$NON-NLS-1$
				arguments.put(ATTRIBUTE_PATTERNS, fFilePatterns);
			arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_REFERENCES, Boolean.valueOf(fUpdateReferences).toString());
			arguments.put(ATTRIBUTE_QUALIFIED, Boolean.valueOf(fUpdateQualifiedNames).toString());
			return arguments;
		}

		protected String getRefactoringId() {
			return IJavaScriptRefactorings.MOVE;
		}

		public boolean getUpdateQualifiedNames() {
			return fUpdateQualifiedNames;
		}

		public boolean getUpdateReferences() {
			return fUpdateReferences;
		}

		private void handleType(IType type, IPackageFragment destination, IProgressMonitor pm) {
			QualifiedNameFinder.process(fQualifiedNameSearchResult, type.getFullyQualifiedName(), destination.getElementName() + "." + type.getTypeQualifiedName(), //$NON-NLS-1$
					fFilePatterns, type.getJavaScriptProject().getProject(), pm);
		}

		public boolean hasAllInputSet() {
			return super.hasAllInputSet() && !canUpdateReferences() && !canUpdateQualifiedNames();
		}

		public RefactoringStatus initialize(RefactoringArguments arguments) {
			if (arguments instanceof JavaRefactoringArguments) {
				final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
				final String patterns= extended.getAttribute(ATTRIBUTE_PATTERNS);
				if (patterns != null && !"".equals(patterns)) //$NON-NLS-1$
					fFilePatterns= patterns;
				else
					fFilePatterns= ""; //$NON-NLS-1$
				final String references= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_REFERENCES);
				if (references != null) {
					fUpdateReferences= Boolean.valueOf(references).booleanValue();
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_REFERENCES));
				final String qualified= extended.getAttribute(ATTRIBUTE_QUALIFIED);
				if (qualified != null) {
					fUpdateQualifiedNames= Boolean.valueOf(qualified).booleanValue();
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_QUALIFIED));
			} else
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
			return super.initialize(arguments);
		}

		public boolean isTextualMove() {
			return false;
		}

		public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
			if (fQualifiedNameSearchResult != null) {
				return fQualifiedNameSearchResult.getSingleChange(Changes.getModifiedFiles(participantChanges));
			} else {
				return null;
			}
		}

		public void setDestinationCheck(boolean check) {
			fCheckDestination= check;
		}

		public void setFilePatterns(String patterns) {
			Assert.isNotNull(patterns);
			fFilePatterns= patterns;
		}

		public void setUpdateQualifiedNames(boolean update) {
			fUpdateQualifiedNames= update;
		}

		public void setUpdateReferences(boolean update) {
			fUpdateReferences= update;
		}

		protected RefactoringStatus verifyDestination(IJavaScriptElement destination) throws JavaScriptModelException {
			RefactoringStatus superStatus= super.verifyDestination(destination);
			if (superStatus.hasFatalError())
				return superStatus;

			Object commonParent= new ParentChecker(getResources(), getJavaElements()).getCommonParent();
			if (destination.equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_parent);
			IContainer destinationAsContainer= getDestinationAsContainer();
			if (destinationAsContainer != null && destinationAsContainer.equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_parent);
			IPackageFragment destinationAsPackage= getDestinationAsPackageFragment();
			if (destinationAsPackage != null && destinationAsPackage.equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_parent);

			return superStatus;
		}

		protected RefactoringStatus verifyDestination(IResource destination) throws JavaScriptModelException {
			RefactoringStatus superStatus= super.verifyDestination(destination);
			if (superStatus.hasFatalError())
				return superStatus;

			Object commonParent= getCommonParent();
			if (destination.equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_parent);
			IContainer destinationAsContainer= getDestinationAsContainer();
			if (destinationAsContainer != null && destinationAsContainer.equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_parent);
			IJavaScriptElement destinationContainerAsPackage= getDestinationContainerAsJavaElement();
			if (destinationContainerAsPackage != null && destinationContainerAsPackage.equals(commonParent))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_parent);

			return superStatus;
		}
	}

	private static final class MovePackageFragmentRootsPolicy extends PackageFragmentRootsReorgPolicy implements IMovePolicy {

		private static final String POLICY_MOVE_ROOTS= "org.eclipse.wst.jsdt.ui.moveRoots"; //$NON-NLS-1$

		private static boolean isParentOfAny(IJavaScriptProject javaProject, IPackageFragmentRoot[] roots) {
			for (int i= 0; i < roots.length; i++) {
				if (ReorgUtils.isParentInWorkspaceOrOnDisk(roots[i], javaProject))
					return true;
			}
			return false;
		}

		private CreateTargetExecutionLog fCreateTargetExecutionLog= new CreateTargetExecutionLog();

		private MoveModifications fModifications;

		MovePackageFragmentRootsPolicy(IPackageFragmentRoot[] roots) {
			super(roots);
		}

		public boolean canEnable() throws JavaScriptModelException {
			if (!super.canEnable())
				return false;
			IPackageFragmentRoot[] roots= getPackageFragmentRoots();
			for (int i= 0; i < roots.length; i++) {
				if (roots[i].isReadOnly() && !(roots[i].isArchive())) {
					final ResourceAttributes attributes= roots[i].getResource().getResourceAttributes();
					if (attributes == null || attributes.isReadOnly())
						return false;
				}
			}
			return true;
		}

		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws CoreException {
			try {
				RefactoringStatus status= super.checkFinalConditions(pm, context, reorgQueries);
				confirmMovingReadOnly(reorgQueries);
				return status;
			} catch (JavaScriptModelException e) {
				throw e;
			} catch (CoreException e) {
				throw new JavaScriptModelException(e);
			}
		}

		private void confirmMovingReadOnly(IReorgQueries reorgQueries) throws CoreException {
			if (!ReadOnlyResourceFinder.confirmMoveOfReadOnlyElements(getJavaElements(), getResources(), reorgQueries))
				throw new OperationCanceledException();
		}

		private Change createChange(IPackageFragmentRoot root, IJavaScriptProject destination) {
			// /XXX fix the query
			return new MovePackageFragmentRootChange(root, destination.getProject(), null);
		}

		public Change createChange(IProgressMonitor pm) throws JavaScriptModelException {
			IPackageFragmentRoot[] roots= getPackageFragmentRoots();
			pm.beginTask("", roots.length); //$NON-NLS-1$
			CompositeChange composite= new DynamicValidationStateChange(RefactoringCoreMessages.ReorgPolicy_move_source_folder);
			composite.markAsSynthetic();
			IJavaScriptProject destination= getDestinationJavaProject();
			Assert.isNotNull(destination);
			for (int i= 0; i < roots.length; i++) {
				composite.add(createChange(roots[i], destination));
				pm.worked(1);
			}
			pm.done();
			return composite;
		}

		protected JDTRefactoringDescriptor createRefactoringDescriptor(JDTRefactoringDescriptorComment comment, Map arguments, String description, String project, int flags) {
			return new JDTMoveRefactoringDescriptor(getCreateTargetExecutionLog(), getProcessorId(), project, description, comment.asString(), arguments, flags);
		}

		public CreateTargetExecutionLog getCreateTargetExecutionLog() {
			return fCreateTargetExecutionLog;
		}

		public ICreateTargetQuery getCreateTargetQuery(ICreateTargetQueries createQueries) {
			return null;
		}

		protected String getDescriptionPlural() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_roots_plural;
		}

		protected String getDescriptionSingular() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_roots_singular;
		}

		protected String getHeaderPattern() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_roots_header;
		}

		protected RefactoringModifications getModifications() throws CoreException {
			if (fModifications != null)
				return fModifications;

			fModifications= new MoveModifications();
			IJavaScriptProject destination= getDestinationJavaProject();
			boolean updateReferences= canUpdateReferences() && getUpdateReferences();
			if (destination != null) {
				IPackageFragmentRoot[] roots= getPackageFragmentRoots();
				for (int i= 0; i < roots.length; i++) {
					fModifications.move(roots[i], new MoveArguments(destination, updateReferences));
				}
			}
			return fModifications;
		}

		public String getPolicyId() {
			return POLICY_MOVE_ROOTS;
		}

		protected String getProcessorId() {
			return IJavaScriptRefactorings.MOVE;
		}

		protected String getRefactoringId() {
			return IJavaScriptRefactorings.MOVE;
		}

		public boolean isTextualMove() {
			return false;
		}

		public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
			return null;
		}

		public void setDestinationCheck(boolean check) {
			fCheckDestination= check;
		}

		protected RefactoringStatus verifyDestination(IJavaScriptElement javaElement) throws JavaScriptModelException {
			RefactoringStatus superStatus= super.verifyDestination(javaElement);
			if (superStatus.hasFatalError())
				return superStatus;
			IJavaScriptProject javaProject= getDestinationJavaProject();
			if (isParentOfAny(javaProject, getPackageFragmentRoots()))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_element2parent);
			return superStatus;
		}
	}

	private static final class MovePackagesPolicy extends PackagesReorgPolicy implements IMovePolicy {

		private static final String POLICY_MOVE_PACKAGES= "org.eclipse.wst.jsdt.ui.movePackages"; //$NON-NLS-1$

		private static boolean isParentOfAny(IPackageFragmentRoot root, IPackageFragment[] fragments) {
			for (int i= 0; i < fragments.length; i++) {
				IPackageFragment fragment= fragments[i];
				if (ReorgUtils.isParentInWorkspaceOrOnDisk(fragment, root))
					return true;
			}
			return false;
		}

		private CreateTargetExecutionLog fCreateTargetExecutionLog= new CreateTargetExecutionLog();

		private MoveModifications fModifications;

		MovePackagesPolicy(IPackageFragment[] packageFragments) {
			super(packageFragments);
		}

		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws CoreException {
			try {
				RefactoringStatus status= super.checkFinalConditions(pm, context, reorgQueries);
				confirmMovingReadOnly(reorgQueries);
				return status;
			} catch (JavaScriptModelException e) {
				throw e;
			} catch (CoreException e) {
				throw new JavaScriptModelException(e);
			}
		}

		private void confirmMovingReadOnly(IReorgQueries reorgQueries) throws CoreException {
			if (!ReadOnlyResourceFinder.confirmMoveOfReadOnlyElements(getJavaElements(), getResources(), reorgQueries))
				throw new OperationCanceledException();
		}

		private Change createChange(IPackageFragment pack, IPackageFragmentRoot destination) {
			return new MovePackageChange(pack, destination);
		}

		public Change createChange(IProgressMonitor pm) throws JavaScriptModelException {
			IPackageFragment[] fragments= getPackages();
			pm.beginTask("", fragments.length); //$NON-NLS-1$
			CompositeChange result= new DynamicValidationStateChange(RefactoringCoreMessages.ReorgPolicy_move_package);
			result.markAsSynthetic();
			IPackageFragmentRoot root= getDestinationAsPackageFragmentRoot();
			for (int i= 0; i < fragments.length; i++) {
				result.add(createChange(fragments[i], root));
				pm.worked(1);
				if (pm.isCanceled())
					throw new OperationCanceledException();
			}
			pm.done();
			return result;
		}

		protected JDTRefactoringDescriptor createRefactoringDescriptor(JDTRefactoringDescriptorComment comment, Map arguments, String description, String project, int flags) {
			return new JDTMoveRefactoringDescriptor(getCreateTargetExecutionLog(), getProcessorId(), project, description, comment.asString(), arguments, flags);
		}

		public CreateTargetExecutionLog getCreateTargetExecutionLog() {
			return fCreateTargetExecutionLog;
		}

		public ICreateTargetQuery getCreateTargetQuery(ICreateTargetQueries createQueries) {
			return null;
		}

		protected String getDescriptionPlural() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_packages_plural;
		}

		protected String getDescriptionSingular() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_packages_singular;
		}

		protected String getHeaderPattern() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_packages_header;
		}

		protected RefactoringModifications getModifications() throws CoreException {
			if (fModifications != null)
				return fModifications;

			fModifications= new MoveModifications();
			boolean updateReferences= canUpdateReferences() && getUpdateReferences();
			IPackageFragment[] packages= getPackages();
			IPackageFragmentRoot javaDestination= getDestinationAsPackageFragmentRoot();
			for (int i= 0; i < packages.length; i++) {
				fModifications.move(packages[i], new MoveArguments(javaDestination, updateReferences));
			}
			return fModifications;
		}

		public String getPolicyId() {
			return POLICY_MOVE_PACKAGES;
		}

		protected String getProcessorId() {
			return IJavaScriptRefactorings.MOVE;
		}

		protected String getRefactoringId() {
			return IJavaScriptRefactorings.MOVE;
		}

		public boolean isTextualMove() {
			return false;
		}

		public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
			return null;
		}

		public void setDestinationCheck(boolean check) {
			fCheckDestination= check;
		}

		protected RefactoringStatus verifyDestination(IJavaScriptElement javaElement) throws JavaScriptModelException {
			RefactoringStatus superStatus= super.verifyDestination(javaElement);
			if (superStatus.hasFatalError())
				return superStatus;

			IPackageFragmentRoot root= getDestinationAsPackageFragmentRoot();
			if (isParentOfAny(root, getPackages()))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_package2parent);
			return superStatus;
		}
	}

	private static final class MoveSubCuElementsPolicy extends SubCuElementReorgPolicy implements IMovePolicy {

		private static final String POLICY_MOVE_MEMBERS= "org.eclipse.wst.jsdt.ui.moveMembers"; //$NON-NLS-1$

		private CreateTargetExecutionLog fCreateTargetExecutionLog= new CreateTargetExecutionLog();

		MoveSubCuElementsPolicy(IJavaScriptElement[] javaElements) {
			super(javaElements);
		}

		public boolean canEnable() throws JavaScriptModelException {
			return super.canEnable() && getSourceCu() != null;
		}

		public Change createChange(IProgressMonitor pm) throws JavaScriptModelException {
			pm.beginTask("", 3); //$NON-NLS-1$
			try {
				final IJavaScriptUnit sourceCu= getSourceCu();
				JavaScriptUnit sourceCuNode= RefactoringASTParser.parseWithASTProvider(sourceCu, false, new SubProgressMonitor(pm, 1));
				CompilationUnitRewrite sourceRewriter= new CompilationUnitRewrite(sourceCu, sourceCuNode);
				IJavaScriptUnit destinationCu= getDestinationCu();
				CompilationUnitRewrite targetRewriter;
				if (sourceCu.equals(destinationCu)) {
					targetRewriter= sourceRewriter;
					pm.worked(1);
				} else {
					JavaScriptUnit destinationCuNode= RefactoringASTParser.parseWithASTProvider(destinationCu, false, new SubProgressMonitor(pm, 1));
					targetRewriter= new CompilationUnitRewrite(destinationCu, destinationCuNode);
				}
				IJavaScriptElement[] javaElements= getJavaElements();
				for (int i= 0; i < javaElements.length; i++) {
					copyToDestination(javaElements[i], targetRewriter, sourceRewriter.getRoot(), targetRewriter.getRoot());
				}
				ASTNodeDeleteUtil.markAsDeleted(javaElements, sourceRewriter, null);
				Change targetCuChange= createCompilationUnitChange(destinationCu, targetRewriter);
				if (sourceCu.equals(destinationCu)) {
					return targetCuChange;
				} else {
					CompositeChange result= new DynamicValidationStateChange(RefactoringCoreMessages.ReorgPolicy_move_members);
					result.markAsSynthetic();
					result.add(targetCuChange);
//					if (Arrays.asList(getJavaElements()).containsAll(Arrays.asList(sourceCu.getTypes())))
//						result.add(DeleteChangeCreator.createDeleteChange(null, new IResource[0], new IJavaScriptUnit[] {sourceCu}, RefactoringCoreMessages.ReorgPolicy_move, null));
//					else
						result.add(createCompilationUnitChange(sourceCu, sourceRewriter));
					return result;
				}
			} catch (JavaScriptModelException e) {
				throw e;
			} catch (CoreException e) {
				throw new JavaScriptModelException(e);
			} finally {
				pm.done();
			}
		}

		protected JDTRefactoringDescriptor createRefactoringDescriptor(JDTRefactoringDescriptorComment comment, Map arguments, String description, String project, int flags) {
			return new JDTMoveRefactoringDescriptor(getCreateTargetExecutionLog(), getProcessorId(), project, description, comment.asString(), arguments, flags);
		}

		public IFile[] getAllModifiedFiles() {
			return ReorgUtils.getFiles(new IResource[] { ReorgUtils.getResource(getSourceCu()), ReorgUtils.getResource(getDestinationCu())});
		}

		public CreateTargetExecutionLog getCreateTargetExecutionLog() {
			return fCreateTargetExecutionLog;
		}

		public ICreateTargetQuery getCreateTargetQuery(ICreateTargetQueries createQueries) {
			return null;
		}

		protected String getDescriptionPlural() {
			final int kind= getContentKind();
			switch (kind) {
				case ONLY_TYPES:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_types;
				case ONLY_FIELDS:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_fields;
				case ONLY_METHODS:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_methods;
				case ONLY_INITIALIZERS:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_initializers;
				case ONLY_PACKAGE_DECLARATIONS:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_package_declarations;
				case ONLY_IMPORT_CONTAINERS:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_import_containers;
				case ONLY_IMPORT_DECLARATIONS:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_import_declarations;
			}
			return RefactoringCoreMessages.ReorgPolicyFactory_move_elements_plural;
		}

		protected String getDescriptionSingular() {
			final int kind= getContentKind();
			switch (kind) {
				case ONLY_TYPES:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_type;
				case ONLY_FIELDS:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_field;
				case ONLY_METHODS:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_method;
				case ONLY_INITIALIZERS:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_initializer;
				case ONLY_PACKAGE_DECLARATIONS:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_package_declaration;
				case ONLY_IMPORT_CONTAINERS:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_import_section;
				case ONLY_IMPORT_DECLARATIONS:
					return RefactoringCoreMessages.ReorgPolicyFactory_move_import_declaration;
			}
			return RefactoringCoreMessages.ReorgPolicyFactory_move_elements_singular;
		}

		protected String getHeaderPattern() {
			return RefactoringCoreMessages.ReorgPolicyFactory_move_elements_header;
		}

		public String getPolicyId() {
			return POLICY_MOVE_MEMBERS;
		}

		protected String getProcessorId() {
			return IJavaScriptRefactorings.MOVE;
		}

		protected String getRefactoringId() {
			return IJavaScriptRefactorings.MOVE;
		}

		public boolean isTextualMove() {
			return true;
		}

		public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
			return null;
		}

		public void setDestinationCheck(boolean check) {
			fCheckDestination= check;
		}

		protected RefactoringStatus verifyDestination(IJavaScriptElement destination) throws JavaScriptModelException {
			IJavaScriptElement[] elements= getJavaElements();
			for (int i= 0; i < elements.length; i++) {
				IJavaScriptElement parent= destination.getParent();
				while (parent != null) {
					if (parent.equals(elements[i]))
						return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_cannot);
					parent= parent.getParent();
				}
			}

			RefactoringStatus superStatus= super.verifyDestination(destination);
			if (superStatus.hasFatalError())
				return superStatus;

			Object commonParent= new ParentChecker(new IResource[0], getJavaElements()).getCommonParent();
			if (destination.equals(commonParent) || Arrays.asList(getJavaElements()).contains(destination))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_element2parent);
			return superStatus;
		}
	}

	private static final class NewNameProposer {

		private static boolean isNewNameOk(IContainer container, String newName) {
			return container.findMember(newName) == null;
		}

		private static boolean isNewNameOk(IPackageFragment dest, String newName) {
			return !dest.getJavaScriptUnit(newName).exists();
		}

		private static boolean isNewNameOk(IPackageFragmentRoot root, String newName) {
			return !root.getPackageFragment(newName).exists();
		}

		private final Set fAutoGeneratedNewNames= new HashSet(2);

		public String createNewName(IJavaScriptUnit cu, IPackageFragment destination) {
			if (isNewNameOk(destination, cu.getElementName()))
				return null;
			if (!ReorgUtils.isParentInWorkspaceOrOnDisk(cu, destination))
				return null;
			int i= 1;
			while (true) {
				String newName;
				if (i == 1)
					newName= Messages.format(RefactoringCoreMessages.CopyRefactoring_cu_copyOf1, cu.getElementName());
				else
					newName= Messages.format(RefactoringCoreMessages.CopyRefactoring_cu_copyOfMore, new String[] { String.valueOf(i), cu.getElementName()});
				if (isNewNameOk(destination, newName) && !fAutoGeneratedNewNames.contains(newName)) {
					fAutoGeneratedNewNames.add(newName);
					return JavaScriptCore.removeJavaScriptLikeExtension(newName);
				}
				i++;
			}
		}

		public String createNewName(IPackageFragment pack, IPackageFragmentRoot destination) {
			if (isNewNameOk(destination, pack.getElementName()))
				return null;
			if (!ReorgUtils.isParentInWorkspaceOrOnDisk(pack, destination))
				return null;
			int i= 1;
			while (true) {
				String newName;
				if (i == 1)
					newName= Messages.format(RefactoringCoreMessages.CopyRefactoring_package_copyOf1, pack.getElementName());
				else
					newName= Messages.format(RefactoringCoreMessages.CopyRefactoring_package_copyOfMore, new String[] { String.valueOf(i), pack.getElementName()});
				if (isNewNameOk(destination, newName) && !fAutoGeneratedNewNames.contains(newName)) {
					fAutoGeneratedNewNames.add(newName);
					return newName;
				}
				i++;
			}
		}

		public String createNewName(IResource res, IContainer destination) {
			if (isNewNameOk(destination, res.getName()))
				return null;
			if (!ReorgUtils.isParentInWorkspaceOrOnDisk(res, destination))
				return null;
			int i= 1;
			while (true) {
				String newName;
				if (i == 1)
					newName= Messages.format(RefactoringCoreMessages.CopyRefactoring_resource_copyOf1, res.getName());
				else
					newName= Messages.format(RefactoringCoreMessages.CopyRefactoring_resource_copyOfMore, new String[] { String.valueOf(i), res.getName()});
				if (isNewNameOk(destination, newName) && !fAutoGeneratedNewNames.contains(newName)) {
					fAutoGeneratedNewNames.add(newName);
					return newName;
				}
				i++;
			}
		}
	}

	private static final class NoCopyPolicy extends ReorgPolicy implements ICopyPolicy {

		public boolean canEnable() throws JavaScriptModelException {
			return false;
		}

		public Change createChange(IProgressMonitor pm, INewNameQueries copyQueries) {
			return new NullChange();
		}

		protected String getDescriptionPlural() {
			return UNUSED_STRING;
		}

		protected String getDescriptionSingular() {
			return UNUSED_STRING;
		}

		public ChangeDescriptor getDescriptor() {
			return null;
		}

		protected String getHeaderPattern() {
			return UNUSED_STRING;
		}

		public IJavaScriptElement[] getJavaElements() {
			return new IJavaScriptElement[0];
		}

		public String getPolicyId() {
			return NO_POLICY;
		}

		protected String getProcessorId() {
			return NO_ID;
		}

		protected String getRefactoringId() {
			return NO_ID;
		}

		public ReorgExecutionLog getReorgExecutionLog() {
			return null;
		}

		public IResource[] getResources() {
			return new IResource[0];
		}

		public RefactoringStatus initialize(RefactoringArguments arguments) {
			return new RefactoringStatus();
		}

		protected RefactoringStatus verifyDestination(IJavaScriptElement javaElement) throws JavaScriptModelException {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_noCopying);
		}

		protected RefactoringStatus verifyDestination(IResource resource) throws JavaScriptModelException {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_noCopying);
		}
	}

	private static final class NoMovePolicy extends ReorgPolicy implements IMovePolicy {

		public boolean canEnable() throws JavaScriptModelException {
			return false;
		}

		public Change createChange(IProgressMonitor pm) {
			return new NullChange();
		}

		public CreateTargetExecutionLog getCreateTargetExecutionLog() {
			return new CreateTargetExecutionLog();
		}

		public ICreateTargetQuery getCreateTargetQuery(ICreateTargetQueries createQueries) {
			return null;
		}

		protected String getDescriptionPlural() {
			return UNUSED_STRING;
		}

		protected String getDescriptionSingular() {
			return UNUSED_STRING;
		}

		public ChangeDescriptor getDescriptor() {
			return null;
		}

		protected String getHeaderPattern() {
			return UNUSED_STRING;
		}

		public IJavaScriptElement[] getJavaElements() {
			return new IJavaScriptElement[0];
		}

		public String getPolicyId() {
			return NO_POLICY;
		}

		protected String getProcessorId() {
			return NO_ID;
		}

		protected String getRefactoringId() {
			return NO_ID;
		}

		public IResource[] getResources() {
			return new IResource[0];
		}

		public RefactoringStatus initialize(RefactoringArguments arguments) {
			return new RefactoringStatus();
		}

		public boolean isTextualMove() {
			return true;
		}

		public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
			return null;
		}

		public void setDestinationCheck(boolean check) {
			fCheckDestination= check;
		}

		protected RefactoringStatus verifyDestination(IJavaScriptElement javaElement) throws JavaScriptModelException {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_noMoving);
		}

		protected RefactoringStatus verifyDestination(IResource resource) throws JavaScriptModelException {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_noMoving);
		}
	}

	private static abstract class PackageFragmentRootsReorgPolicy extends ReorgPolicy {

		private IPackageFragmentRoot[] fPackageFragmentRoots;

		public PackageFragmentRootsReorgPolicy(IPackageFragmentRoot[] roots) {
			fPackageFragmentRoots= roots;
		}

		public boolean canChildrenBeDestinations(IJavaScriptElement javaElement) {
			switch (javaElement.getElementType()) {
				case IJavaScriptElement.JAVASCRIPT_MODEL:
				case IJavaScriptElement.JAVASCRIPT_PROJECT:
					return true;
				default:
					return false;
			}
		}

		public boolean canChildrenBeDestinations(IResource resource) {
			return false;
		}

		public boolean canElementBeDestination(IJavaScriptElement javaElement) {
			return javaElement.getElementType() == IJavaScriptElement.JAVASCRIPT_PROJECT;
		}

		public boolean canElementBeDestination(IResource resource) {
			return false;
		}

		public boolean canEnable() throws JavaScriptModelException {
			if (!super.canEnable())
				return false;
			for (int i= 0; i < fPackageFragmentRoots.length; i++) {
				if (!(ReorgUtils.isSourceFolder(fPackageFragmentRoots[i]) || (fPackageFragmentRoots[i].isArchive() && !fPackageFragmentRoots[i].isExternal())))
					return false;
			}
			if (ReorgUtils.containsLinkedResources(fPackageFragmentRoots))
				return false;
			return true;
		}

		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws CoreException {
			RefactoringStatus status= super.checkFinalConditions(pm, context, reorgQueries);
			confirmOverwriting(reorgQueries);
			return status;
		}

		private void confirmOverwriting(IReorgQueries reorgQueries) {
			OverwriteHelper oh= new OverwriteHelper();
			oh.setPackageFragmentRoots(fPackageFragmentRoots);
			IJavaScriptProject javaProject= getDestinationJavaProject();
			oh.confirmOverwriting(reorgQueries, javaProject);
			fPackageFragmentRoots= oh.getPackageFragmentRootsWithoutUnconfirmedOnes();
		}

		protected JDTRefactoringDescriptor createRefactoringDescriptor(final JDTRefactoringDescriptorComment comment, final Map arguments, final String description, final String project, int flags) {
			return new JDTRefactoringDescriptor(getProcessorId(), project, description, comment.asString(), arguments, flags);
		}

		public final ChangeDescriptor getDescriptor() {
			final Map arguments= new HashMap();
			final int length= fPackageFragmentRoots.length;
			final String description= length == 1 ? getDescriptionSingular() : getDescriptionPlural();
			final IProject resource= getSingleProject();
			final String project= resource != null ? resource.getName() : null;
			final String header= Messages.format(getHeaderPattern(), new String[] { String.valueOf(length), getDestinationLabel()});
			int flags= RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
			final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
			final JDTRefactoringDescriptor descriptor= createRefactoringDescriptor(comment, arguments, description, project, flags);
			arguments.put(ATTRIBUTE_POLICY, getPolicyId());
			arguments.put(ATTRIBUTE_ROOTS, Integer.valueOf(fPackageFragmentRoots.length).toString());
			for (int offset= 0; offset < fPackageFragmentRoots.length; offset++)
				arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + 1), descriptor.elementToHandle(fPackageFragmentRoots[offset]));
			arguments.putAll(getRefactoringArguments(project));
			return new RefactoringChangeDescriptor(descriptor);
		}

		private IJavaScriptProject getDestinationAsJavaProject(IJavaScriptElement javaElementDestination) {
			if (javaElementDestination == null)
				return null;
			else
				return javaElementDestination.getJavaScriptProject();
		}

		protected IJavaScriptProject getDestinationJavaProject() {
			return getDestinationAsJavaProject(getJavaElementDestination());
		}

		public IJavaScriptElement[] getJavaElements() {
			return fPackageFragmentRoots;
		}

		protected IPackageFragmentRoot[] getPackageFragmentRoots() {
			return fPackageFragmentRoots;
		}

		public IResource[] getResources() {
			return new IResource[0];
		}

		public IPackageFragmentRoot[] getRoots() {
			return fPackageFragmentRoots;
		}

		private IProject getSingleProject() {
			IProject result= null;
			for (int index= 0; index < fPackageFragmentRoots.length; index++) {
				if (result == null)
					result= fPackageFragmentRoots[index].getJavaScriptProject().getProject();
				else if (!result.equals(fPackageFragmentRoots[index].getJavaScriptProject().getProject()))
					return null;
			}
			return result;
		}

		public RefactoringStatus initialize(RefactoringArguments arguments) {
			final RefactoringStatus status= new RefactoringStatus();
			if (arguments instanceof JavaRefactoringArguments) {
				final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
				int rootCount= 0;
				String value= extended.getAttribute(ATTRIBUTE_ROOTS);
				if (value != null && !"".equals(value)) {//$NON-NLS-1$
					try {
						rootCount= Integer.parseInt(value);
					} catch (NumberFormatException exception) {
						return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_ROOTS));
					}
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_ROOTS));
				String handle= null;
				List elements= new ArrayList();
				for (int index= 0; index < rootCount; index++) {
					final String attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (index + 1);
					handle= extended.getAttribute(attribute);
					if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
						final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
						if (element == null || !element.exists() || element.getElementType() != IJavaScriptElement.PACKAGE_FRAGMENT_ROOT)
							status.merge(ScriptableRefactoring.createInputWarningStatus(element, getProcessorId(), getRefactoringId()));
						else
							elements.add(element);
					} else
						return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
				}
				fPackageFragmentRoots= (IPackageFragmentRoot[]) elements.toArray(new IPackageFragmentRoot[elements.size()]);
			} else
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
			status.merge(super.initialize(arguments));
			return status;
		}

		protected RefactoringStatus verifyDestination(IJavaScriptElement javaElement) throws JavaScriptModelException {
			Assert.isNotNull(javaElement);
			if (!fCheckDestination)
				return new RefactoringStatus();
			if (!javaElement.exists())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_cannot1);
			if (javaElement instanceof IJavaScriptModel)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_jmodel);
			if (!(javaElement instanceof IJavaScriptProject || javaElement instanceof IPackageFragmentRoot))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_src2proj);
			if (javaElement.isReadOnly())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_src2writable);
			if (ReorgUtils.isPackageFragmentRoot(javaElement.getJavaScriptProject()))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_src2nosrc);
			return new RefactoringStatus();
		}

		protected RefactoringStatus verifyDestination(IResource resource) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_src2proj);
		}
	}

	private static abstract class PackagesReorgPolicy extends ReorgPolicy {

		private IPackageFragment[] fPackageFragments;

		public PackagesReorgPolicy(IPackageFragment[] packageFragments) {
			fPackageFragments= packageFragments;
		}

		public boolean canChildrenBeDestinations(IJavaScriptElement javaElement) {
			switch (javaElement.getElementType()) {
				case IJavaScriptElement.JAVASCRIPT_MODEL:
				case IJavaScriptElement.JAVASCRIPT_PROJECT:
				case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
					// can be nested
					// (with exclusion
					// filters)
					return true;
				default:
					return false;
			}
		}

		public boolean canChildrenBeDestinations(IResource resource) {
			return false;
		}

		public boolean canElementBeDestination(IJavaScriptElement javaElement) {
			switch (javaElement.getElementType()) {
				case IJavaScriptElement.JAVASCRIPT_PROJECT:
				case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
					return true;
				default:
					return false;
			}
		}

		public boolean canElementBeDestination(IResource resource) {
			return false;
		}

		public boolean canEnable() throws JavaScriptModelException {
			for (int i= 0; i < fPackageFragments.length; i++) {
				if (JavaElementUtil.isDefaultPackage(fPackageFragments[i]) || fPackageFragments[i].isReadOnly())
					return false;
			}
			if (ReorgUtils.containsLinkedResources(fPackageFragments))
				return false;
			return true;
		}

		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws CoreException {
			RefactoringStatus refactoringStatus= super.checkFinalConditions(pm, context, reorgQueries);
			confirmOverwriting(reorgQueries);
			return refactoringStatus;
		}

		private void confirmOverwriting(IReorgQueries reorgQueries) throws JavaScriptModelException {
			OverwriteHelper helper= new OverwriteHelper();
			helper.setPackages(fPackageFragments);
			IPackageFragmentRoot destRoot= getDestinationAsPackageFragmentRoot();
			helper.confirmOverwriting(reorgQueries, destRoot);
			fPackageFragments= helper.getPackagesWithoutUnconfirmedOnes();
		}

		protected JDTRefactoringDescriptor createRefactoringDescriptor(final JDTRefactoringDescriptorComment comment, final Map arguments, final String description, final String project, int flags) {
			return new JDTRefactoringDescriptor(getProcessorId(), project, description, comment.asString(), arguments, flags);
		}

		public final ChangeDescriptor getDescriptor() {
			final Map arguments= new HashMap();
			final int length= fPackageFragments.length;
			final String description= length == 1 ? getDescriptionSingular() : getDescriptionPlural();
			final IProject resource= getSingleProject();
			final String project= resource != null ? resource.getName() : null;
			final String header= Messages.format(getHeaderPattern(), new String[] { String.valueOf(length), getDestinationLabel()});
			int flags= JavaScriptRefactoringDescriptor.JAR_REFACTORING | JavaScriptRefactoringDescriptor.JAR_MIGRATION | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
			final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
			final JDTRefactoringDescriptor descriptor= createRefactoringDescriptor(comment, arguments, description, project, flags);
			arguments.put(ATTRIBUTE_POLICY, getPolicyId());
			arguments.put(ATTRIBUTE_FRAGMENTS, Integer.valueOf(fPackageFragments.length).toString());
			for (int offset= 0; offset < fPackageFragments.length; offset++)
				arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + 1), descriptor.elementToHandle(fPackageFragments[offset]));
			arguments.putAll(getRefactoringArguments(project));
			return new RefactoringChangeDescriptor(descriptor);
		}

		protected IPackageFragmentRoot getDestinationAsPackageFragmentRoot() throws JavaScriptModelException {
			return getDestinationAsPackageFragmentRoot(getJavaElementDestination());
		}

		private IPackageFragmentRoot getDestinationAsPackageFragmentRoot(IJavaScriptElement javaElement) throws JavaScriptModelException {
			if (javaElement == null)
				return null;

			if (javaElement instanceof IPackageFragmentRoot)
				return (IPackageFragmentRoot) javaElement;

			if (javaElement instanceof IPackageFragment) {
				IPackageFragment pack= (IPackageFragment) javaElement;
				if (pack.getParent() instanceof IPackageFragmentRoot)
					return (IPackageFragmentRoot) pack.getParent();
			}

			if (javaElement instanceof IJavaScriptProject)
				return ReorgUtils.getCorrespondingPackageFragmentRoot((IJavaScriptProject) javaElement);
			return null;
		}

		public IJavaScriptElement[] getJavaElements() {
			return fPackageFragments;
		}

		protected IPackageFragment[] getPackages() {
			return fPackageFragments;
		}

		public IResource[] getResources() {
			return new IResource[0];
		}

		private IProject getSingleProject() {
			IProject result= null;
			for (int index= 0; index < fPackageFragments.length; index++) {
				if (result == null)
					result= fPackageFragments[index].getJavaScriptProject().getProject();
				else if (!result.equals(fPackageFragments[index].getJavaScriptProject().getProject()))
					return null;
			}
			return result;
		}

		public RefactoringStatus initialize(RefactoringArguments arguments) {
			final RefactoringStatus status= new RefactoringStatus();
			if (arguments instanceof JavaRefactoringArguments) {
				final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
				int fragmentCount= 0;
				String value= extended.getAttribute(ATTRIBUTE_FRAGMENTS);
				if (value != null && !"".equals(value)) {//$NON-NLS-1$
					try {
						fragmentCount= Integer.parseInt(value);
					} catch (NumberFormatException exception) {
						return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_FRAGMENTS));
					}
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_FRAGMENTS));
				String handle= null;
				List elements= new ArrayList();
				for (int index= 0; index < fragmentCount; index++) {
					final String attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (index + 1);
					handle= extended.getAttribute(attribute);
					if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
						final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
						if (element == null || !element.exists() || element.getElementType() != IJavaScriptElement.PACKAGE_FRAGMENT)
							status.merge(ScriptableRefactoring.createInputWarningStatus(element, getProcessorId(), getRefactoringId()));
						else
							elements.add(element);
					} else
						return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
				}
				fPackageFragments= (IPackageFragment[]) elements.toArray(new IPackageFragment[elements.size()]);
			} else
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
			status.merge(super.initialize(arguments));
			return status;
		}

		protected RefactoringStatus verifyDestination(IJavaScriptElement javaElement) throws JavaScriptModelException {
			Assert.isNotNull(javaElement);
			if (!fCheckDestination)
				return new RefactoringStatus();
			if (!javaElement.exists())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_cannot1);
			if (javaElement instanceof IJavaScriptModel)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_jmodel);
			IPackageFragmentRoot destRoot= getDestinationAsPackageFragmentRoot(javaElement);
			if (!ReorgUtils.isSourceFolder(destRoot))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_packages);
			return new RefactoringStatus();
		}

		protected RefactoringStatus verifyDestination(IResource resource) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_packages);
		}
	}

	private static abstract class ReorgPolicy implements IReorgPolicy {

		private static final String ATTRIBUTE_DESTINATION= "destination"; //$NON-NLS-1$

		private static final String ATTRIBUTE_TARGET= "target"; //$NON-NLS-1$

		protected boolean fCheckDestination= true;

		private IJavaScriptElement fJavaElementDestination;

		private IResource fResourceDestination;

		public boolean canChildrenBeDestinations(IJavaScriptElement javaElement) {
			return true;
		}

		public boolean canChildrenBeDestinations(IResource resource) {
			return true;
		}

		public boolean canElementBeDestination(IJavaScriptElement javaElement) {
			return true;
		}

		public boolean canElementBeDestination(IResource resource) {
			return true;
		}

		public boolean canEnable() throws JavaScriptModelException {
			IResource[] resources= getResources();
			for (int i= 0; i < resources.length; i++) {
				IResource resource= resources[i];
				if (!resource.exists() || resource.isPhantom() || !resource.isAccessible())
					return false;
			}

			IJavaScriptElement[] javaElements= getJavaElements();
			for (int i= 0; i < javaElements.length; i++) {
				IJavaScriptElement element= javaElements[i];
				if (!element.exists())
					return false;
			}
			return true;
		}

		public boolean canEnableQualifiedNameUpdating() {
			return false;
		}

		public boolean canEnableUpdateReferences() {
			return false;
		}

		public boolean canUpdateQualifiedNames() {
			Assert.isTrue(false);
			// should not be called if
			// canEnableQualifiedNameUpdating is not
			// overridden and returns false
			return false;
		}

		public boolean canUpdateReferences() {
			return false;
		}

		public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws CoreException {
			Assert.isNotNull(reorgQueries);
			ResourceChangeChecker checker= (ResourceChangeChecker) context.getChecker(ResourceChangeChecker.class);
			IFile[] allModifiedFiles= getAllModifiedFiles();
			RefactoringModifications modifications= getModifications();
			IResourceChangeDescriptionFactory deltaFactory= checker.getDeltaFactory();
			for (int i= 0; i < allModifiedFiles.length; i++) {
				deltaFactory.change(allModifiedFiles[i]);
			}
			if (modifications != null) {
				modifications.buildDelta(deltaFactory);
				modifications.buildValidateEdits((ValidateEditChecker) context.getChecker(ValidateEditChecker.class));
			}
			return new RefactoringStatus();
		}

		public IFile[] getAllModifiedFiles() {
			return new IFile[0];
		}

		protected abstract String getDescriptionPlural();

		protected abstract String getDescriptionSingular();

		protected String getDestinationLabel() {
			Object destination= getJavaElementDestination();
			if (destination == null)
				destination= getResourceDestination();
			return JavaScriptElementLabels.getTextLabel(destination, JavaScriptElementLabels.ALL_FULLY_QUALIFIED);
		}

		public String getFilePatterns() {
			Assert.isTrue(false);
			// should not be called if
			// canEnableQualifiedNameUpdating is not
			// overridden and returns false
			return null;
		}

		protected abstract String getHeaderPattern();

		public final IJavaScriptElement getJavaElementDestination() {
			return fJavaElementDestination;
		}

		protected RefactoringModifications getModifications() throws CoreException {
			return null;
		}

		protected abstract String getProcessorId();

		protected Map getRefactoringArguments(String project) {
			final Map arguments= new HashMap();
			final IJavaScriptElement element= getJavaElementDestination();
			if (element != null)
				arguments.put(ATTRIBUTE_DESTINATION, JDTRefactoringDescriptor.elementToHandle(project, element));
			else {
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=157479
				final IResource resource= getResourceDestination();
				if (resource != null)
					arguments.put(ATTRIBUTE_TARGET, JDTRefactoringDescriptor.resourceToHandle(null, resource));
			}
			return arguments;
		}

		protected abstract String getRefactoringId();

		public final IResource getResourceDestination() {
			return fResourceDestination;
		}

		public boolean getUpdateQualifiedNames() {
			Assert.isTrue(false);
			// should not be called if
			// canEnableQualifiedNameUpdating is not
			// overridden and returns false
			return false;
		}

		public boolean getUpdateReferences() {
			Assert.isTrue(false);
			// should not be called if
			// canUpdateReferences is not overridden and
			// returns false
			return false;
		}

		public boolean hasAllInputSet() {
			return fJavaElementDestination != null || fResourceDestination != null;
		}

		public RefactoringStatus initialize(RefactoringArguments arguments) {
			if (arguments instanceof JavaRefactoringArguments) {
				final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
				String handle= extended.getAttribute(ATTRIBUTE_DESTINATION);
				if (handle != null) {
					final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
					if (element != null) {
						if (fCheckDestination && !element.exists())
							return ScriptableRefactoring.createInputFatalStatus(element, getProcessorId(), getRefactoringId());
						else {
							try {
								return setDestination(element);
							} catch (JavaScriptModelException exception) {
								JavaScriptPlugin.log(exception);
								return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, new String[] { handle, JDTRefactoringDescriptor.ATTRIBUTE_INPUT}));
							}
						}
					} else {
						// Leave for compatibility
						// https://bugs.eclipse.org/bugs/show_bug.cgi?id=157479
						final IResource resource= JDTRefactoringDescriptor.handleToResource(extended.getProject(), handle);
						if (resource == null || (fCheckDestination && !resource.exists()))
							return ScriptableRefactoring.createInputFatalStatus(resource, getProcessorId(), getRefactoringId());
						else {
							try {
								return setDestination(resource);
							} catch (JavaScriptModelException exception) {
								JavaScriptPlugin.log(exception);
								return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, new String[] { handle, JDTRefactoringDescriptor.ATTRIBUTE_INPUT}));
							}
						}
					}
				} else {
					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=157479
					handle= extended.getAttribute(ATTRIBUTE_TARGET);
					if (handle != null) {
						final IResource resource= JDTRefactoringDescriptor.handleToResource(null, handle);
						if (resource == null || (fCheckDestination && !resource.exists()))
							return ScriptableRefactoring.createInputFatalStatus(resource, getProcessorId(), getRefactoringId());
						else {
							try {
								return setDestination(resource);
							} catch (JavaScriptModelException exception) {
								JavaScriptPlugin.log(exception);
								return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, new String[] { handle, JDTRefactoringDescriptor.ATTRIBUTE_INPUT}));
							}
						}
					} else
						return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		}

		public final RefactoringParticipant[] loadParticipants(RefactoringStatus status, RefactoringProcessor processor, String[] natures, SharableParticipants shared) throws CoreException {
			RefactoringModifications modifications= getModifications();
			if (modifications != null) {
				return modifications.loadParticipants(status, processor, natures, shared);
			} else {
				return new RefactoringParticipant[0];
			}
		}

		public final RefactoringStatus setDestination(IJavaScriptElement destination) throws JavaScriptModelException {
			Assert.isNotNull(destination);
			fJavaElementDestination= null;
			fResourceDestination= null;
			fJavaElementDestination= destination;
			return verifyDestination(destination);
		}

		public final RefactoringStatus setDestination(IResource destination) throws JavaScriptModelException {
			Assert.isNotNull(destination);
			fJavaElementDestination= null;
			fResourceDestination= null;
			fResourceDestination= destination;
			return verifyDestination(destination);
		}

		public void setFilePatterns(String patterns) {
			Assert.isTrue(false);
			// should not be called if
			// canEnableQualifiedNameUpdating is not
			// overridden and returns false
		}

		public void setUpdateQualifiedNames(boolean update) {
			Assert.isTrue(false);
			// should not be called if
			// canEnableQualifiedNameUpdating is not
			// overridden and returns false
		}

		public void setUpdateReferences(boolean update) {
			Assert.isTrue(false);
			// should not be called if
			// canUpdateReferences is not overridden and
			// returns false
		}

		protected abstract RefactoringStatus verifyDestination(IJavaScriptElement destination) throws JavaScriptModelException;

		protected abstract RefactoringStatus verifyDestination(IResource destination) throws JavaScriptModelException;
	}

	private static abstract class SubCuElementReorgPolicy extends ReorgPolicy {

		protected static final int ONLY_FIELDS= 1;

		protected static final int ONLY_IMPORT_CONTAINERS= 5;

		protected static final int ONLY_IMPORT_DECLARATIONS= 6;

		protected static final int ONLY_INITIALIZERS= 3;

		protected static final int ONLY_METHODS= 2;

		protected static final int ONLY_PACKAGE_DECLARATIONS= 4;

		protected static final int ONLY_TYPES= 0;

		protected static CompilationUnitChange createCompilationUnitChange(IJavaScriptUnit cu, CompilationUnitRewrite rewrite) throws CoreException {
			CompilationUnitChange change= rewrite.createChange();
			if (change != null) {
				if (cu.isWorkingCopy())
					change.setSaveMode(TextFileChange.LEAVE_DIRTY);
			}
			return change;
		}

		protected static final IJavaScriptUnit getDestinationCu(IJavaScriptElement destination) {
			if (destination instanceof IJavaScriptUnit)
				return (IJavaScriptUnit) destination;
			return (IJavaScriptUnit) destination.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
		}

		private static IJavaScriptUnit getEnclosingCu(IJavaScriptElement destination) {
			if (destination instanceof IJavaScriptUnit)
				return (IJavaScriptUnit) destination;
			return (IJavaScriptUnit) destination.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
		}

		private static IType getEnclosingType(IJavaScriptElement destination) {
			if (destination instanceof IType)
				return (IType) destination;
			return (IType) destination.getAncestor(IJavaScriptElement.TYPE);
		}

		private static String getUnindentedSource(ISourceReference sourceReference) throws JavaScriptModelException {
			Assert.isTrue(sourceReference instanceof IJavaScriptElement);
			String[] lines= Strings.convertIntoLines(sourceReference.getSource());
			final IJavaScriptProject project= ((IJavaScriptElement) sourceReference).getJavaScriptProject();
			Strings.trimIndentation(lines, project, false);
			return Strings.concatenate(lines, StubUtility.getLineDelimiterUsed((IJavaScriptElement) sourceReference));
		}

		private IJavaScriptElement[] fJavaElements;

		SubCuElementReorgPolicy(IJavaScriptElement[] javaElements) {
			fJavaElements= javaElements;
		}

		public boolean canChildrenBeDestinations(IResource resource) {
			return false;
		}

		public boolean canElementBeDestination(IResource resource) {
			return false;
		}

		public boolean canEnable() throws JavaScriptModelException {
			if (!super.canEnable())
				return false;
			for (int i= 0; i < fJavaElements.length; i++) {
				if (fJavaElements[i] instanceof IMember) {
					IMember member= (IMember) fJavaElements[i];
					// we can copy some binary members, but not all
					if (member.isBinary() && member.getSourceRange() == null)
						return false;
				}
			}
			return true;
		}

		private void copyImportsToDestination(IImportContainer container, ASTRewrite rewrite, JavaScriptUnit sourceCuNode, JavaScriptUnit destinationCuNode) throws JavaScriptModelException {
			IJavaScriptElement[] importDeclarations= container.getChildren();
			for (int i= 0; i < importDeclarations.length; i++) {
				Assert.isTrue(importDeclarations[i] instanceof IImportDeclaration);
				IImportDeclaration importDeclaration= (IImportDeclaration) importDeclarations[i];
				copyImportToDestination(importDeclaration, rewrite, sourceCuNode, destinationCuNode);
			}
		}

		private void copyImportToDestination(IImportDeclaration declaration, ASTRewrite targetRewrite, JavaScriptUnit sourceCuNode, JavaScriptUnit destinationCuNode) throws JavaScriptModelException {
			ImportDeclaration sourceNode= ASTNodeSearchUtil.getImportDeclarationNode(declaration, sourceCuNode);
			ImportDeclaration copiedNode= (ImportDeclaration) ASTNode.copySubtree(targetRewrite.getAST(), sourceNode);
			targetRewrite.getListRewrite(destinationCuNode, JavaScriptUnit.IMPORTS_PROPERTY).insertLast(copiedNode, null);
		}

		private void copyInitializerToDestination(IInitializer initializer, CompilationUnitRewrite targetRewriter, JavaScriptUnit sourceCuNode, JavaScriptUnit targetCuNode) throws JavaScriptModelException {
			BodyDeclaration newInitializer= (BodyDeclaration) targetRewriter.getASTRewrite().createStringPlaceholder(getUnindentedSource(initializer), ASTNode.INITIALIZER);
			copyMemberToDestination(initializer, targetRewriter, sourceCuNode, targetCuNode, newInitializer);
		}

		private void copyMemberToDestination(IMember member, CompilationUnitRewrite targetRewriter, JavaScriptUnit sourceCuNode, JavaScriptUnit targetCuNode, BodyDeclaration newMember) throws JavaScriptModelException {
			IJavaScriptElement javaElementDestination= getJavaElementDestination();
			ASTNode nodeDestination;
			ASTNode destinationContainer;
			switch (javaElementDestination.getElementType()) {
				case IJavaScriptElement.INITIALIZER:
					nodeDestination= ASTNodeSearchUtil.getInitializerNode((IInitializer) javaElementDestination, targetCuNode);
					destinationContainer= nodeDestination.getParent();
					break;
				case IJavaScriptElement.FIELD:
					nodeDestination= ASTNodeSearchUtil.getFieldOrEnumConstantDeclaration((IField) javaElementDestination, targetCuNode);
					destinationContainer= nodeDestination.getParent();
					break;
				case IJavaScriptElement.METHOD:
					nodeDestination= ASTNodeSearchUtil.getMethodOrAnnotationTypeMemberDeclarationNode((IFunction) javaElementDestination, targetCuNode);
					destinationContainer= nodeDestination.getParent();
					break;
				case IJavaScriptElement.TYPE:
					nodeDestination= null;
					IType typeDestination= (IType) javaElementDestination;
					if (typeDestination.isAnonymous())
						destinationContainer= ASTNodeSearchUtil.getClassInstanceCreationNode(typeDestination, targetCuNode).getAnonymousClassDeclaration();
					else
						destinationContainer= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(typeDestination, targetCuNode);
					break;
				default:
					nodeDestination= null;
					destinationContainer= null;
			}
			if (!(member instanceof IInitializer)) {
				BodyDeclaration decl= ASTNodeSearchUtil.getBodyDeclarationNode(member, sourceCuNode);
				if (decl != null)
					ImportRewriteUtil.addImports(targetRewriter, decl, new HashMap(), new HashMap(), false);
			}
			if (destinationContainer != null) {
				ListRewrite listRewrite;
				if (destinationContainer instanceof AbstractTypeDeclaration) {
						listRewrite= targetRewriter.getASTRewrite().getListRewrite(destinationContainer, ((AbstractTypeDeclaration) destinationContainer).getBodyDeclarationsProperty());
				} else
					listRewrite= targetRewriter.getASTRewrite().getListRewrite(destinationContainer, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);

				if (nodeDestination != null) {
					final List list= listRewrite.getOriginalList();
					final int index= list.indexOf(nodeDestination);
					if (index > 0 && index < list.size() - 1) {
						listRewrite.insertBefore(newMember, (ASTNode) list.get(index), null);
					} else
						listRewrite.insertLast(newMember, null);
				} else
					listRewrite.insertAt(newMember, ASTNodes.getInsertionIndex(newMember, listRewrite.getRewrittenList()), null);
				return; // could insert into/after destination
			}
			// fall-back / default:
//			final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(getDestinationAsType(), targetCuNode.);
			targetRewriter.getASTRewrite().getListRewrite(targetCuNode, JavaScriptUnit.STATEMENTS_PROPERTY).insertLast(newMember, null);
		}

		private void copyMethodToDestination(IFunction method, CompilationUnitRewrite targetRewriter, JavaScriptUnit sourceCuNode, JavaScriptUnit targetCuNode) throws JavaScriptModelException {
			BodyDeclaration newMethod= (BodyDeclaration) targetRewriter.getASTRewrite().createStringPlaceholder(getUnindentedSource(method), ASTNode.FUNCTION_DECLARATION);
			copyMemberToDestination(method, targetRewriter, sourceCuNode, targetCuNode, newMethod);
		}

		protected void copyToDestination(IJavaScriptElement element, CompilationUnitRewrite targetRewriter, JavaScriptUnit sourceCuNode, JavaScriptUnit targetCuNode) throws CoreException {
			final ASTRewrite rewrite= targetRewriter.getASTRewrite();
			switch (element.getElementType()) {
				case IJavaScriptElement.FIELD:
					copyMemberToDestination((IMember) element, targetRewriter, sourceCuNode, targetCuNode, createNewFieldDeclarationNode(((IField) element), rewrite, sourceCuNode));
					break;
				case IJavaScriptElement.IMPORT_CONTAINER:
					copyImportsToDestination((IImportContainer) element, rewrite, sourceCuNode, targetCuNode);
					break;
				case IJavaScriptElement.IMPORT_DECLARATION:
					copyImportToDestination((IImportDeclaration) element, rewrite, sourceCuNode, targetCuNode);
					break;
				case IJavaScriptElement.INITIALIZER:
					copyInitializerToDestination((IInitializer) element, targetRewriter, sourceCuNode, targetCuNode);
					break;
				case IJavaScriptElement.METHOD:
					copyMethodToDestination((IFunction) element, targetRewriter, sourceCuNode, targetCuNode);
					break;
				case IJavaScriptElement.TYPE:
					copyTypeToDestination((IType) element, targetRewriter, sourceCuNode, targetCuNode);
					break;

				default:
					Assert.isTrue(false);
			}
		}

		private void copyTypeToDestination(IType type, CompilationUnitRewrite targetRewriter, JavaScriptUnit sourceCuNode, JavaScriptUnit targetCuNode) throws JavaScriptModelException {
			AbstractTypeDeclaration newType= (AbstractTypeDeclaration) targetRewriter.getASTRewrite().createStringPlaceholder(getUnindentedSource(type), ASTNode.TYPE_DECLARATION);
			IType enclosingType= getEnclosingType(getJavaElementDestination());
			if (enclosingType != null) {
				copyMemberToDestination(type, targetRewriter, sourceCuNode, targetCuNode, newType);
			} else {
				targetRewriter.getASTRewrite().getListRewrite(targetCuNode, JavaScriptUnit.TYPES_PROPERTY).insertLast(newType, null);
			}
		}

		private BodyDeclaration createNewFieldDeclarationNode(IField field, ASTRewrite rewrite, JavaScriptUnit sourceCuNode) throws CoreException {
			AST targetAst= rewrite.getAST();
			ITextFileBuffer buffer= null;
			BodyDeclaration newDeclaration= null;
			IJavaScriptUnit unit= field.getJavaScriptUnit();
			try {
				buffer= RefactoringFileBuffers.acquire(unit);
				IDocument document= buffer.getDocument();
				BodyDeclaration bodyDeclaration= ASTNodeSearchUtil.getFieldOrEnumConstantDeclaration(field, sourceCuNode);
				if (bodyDeclaration instanceof FieldDeclaration) {
					FieldDeclaration fieldDeclaration= (FieldDeclaration) bodyDeclaration;
					if (fieldDeclaration.fragments().size() == 1)
						return (FieldDeclaration) ASTNode.copySubtree(targetAst, fieldDeclaration);
					VariableDeclarationFragment originalFragment= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(field, sourceCuNode);
					VariableDeclarationFragment copiedFragment= (VariableDeclarationFragment) ASTNode.copySubtree(targetAst, originalFragment);
					newDeclaration= targetAst.newFieldDeclaration(copiedFragment);
					((FieldDeclaration) newDeclaration).setType((Type) ASTNode.copySubtree(targetAst, fieldDeclaration.getType()));
				} else
					Assert.isTrue(false);
				if (newDeclaration != null) {
					newDeclaration.modifiers().addAll(ASTNodeFactory.newModifiers(targetAst, bodyDeclaration.getModifiers()));
					JSdoc javadoc= bodyDeclaration.getJavadoc();
					if (javadoc != null)
						newDeclaration.setJavadoc((JSdoc) rewrite.createStringPlaceholder(document.get(javadoc.getStartPosition(), javadoc.getLength()), ASTNode.JSDOC));
				}
			} catch (BadLocationException exception) {
				JavaScriptPlugin.log(exception);
			} finally {
				if (buffer != null)
					RefactoringFileBuffers.release(unit);
			}
			return newDeclaration;
		}

		protected JDTRefactoringDescriptor createRefactoringDescriptor(final JDTRefactoringDescriptorComment comment, final Map arguments, final String description, final String project, int flags) {
			return new JDTRefactoringDescriptor(getProcessorId(), project, description, comment.asString(), arguments, flags);
		}

		protected final int getContentKind() {
			final int types= ReorgUtils.getElementsOfType(fJavaElements, IJavaScriptElement.TYPE).size();
			final int fields= ReorgUtils.getElementsOfType(fJavaElements, IJavaScriptElement.FIELD).size();
			final int methods= ReorgUtils.getElementsOfType(fJavaElements, IJavaScriptElement.METHOD).size();
			final int initializers= ReorgUtils.getElementsOfType(fJavaElements, IJavaScriptElement.INITIALIZER).size();
			final int container= ReorgUtils.getElementsOfType(fJavaElements, IJavaScriptElement.IMPORT_CONTAINER).size();
			final int imp= ReorgUtils.getElementsOfType(fJavaElements, IJavaScriptElement.IMPORT_DECLARATION).size();
			final int length= types + fields + methods + initializers + container + imp;
			if (length == types)
				return ONLY_TYPES;
			else if (length == fields)
				return ONLY_FIELDS;
			else if (length == methods)
				return ONLY_METHODS;
			else if (length == initializers)
				return ONLY_INITIALIZERS;
			else if (length == container)
				return ONLY_IMPORT_CONTAINERS;
			else if (length == imp)
				return ONLY_IMPORT_DECLARATIONS;
			return -1;
		}

		public final ChangeDescriptor getDescriptor() {
			final Map arguments= new HashMap();
			final int length= fJavaElements.length;
			final String description= length == 1 ? getDescriptionSingular() : getDescriptionPlural();
			final IProject resource= getSingleProject();
			final String project= resource != null ? resource.getName() : null;
			final String header= Messages.format(getHeaderPattern(), new String[] { String.valueOf(length), getDestinationLabel()});
			int flags= JavaScriptRefactoringDescriptor.JAR_REFACTORING | JavaScriptRefactoringDescriptor.JAR_MIGRATION | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
			final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
			final JDTRefactoringDescriptor descriptor= createRefactoringDescriptor(comment, arguments, description, project, flags);
			arguments.put(ATTRIBUTE_POLICY, getPolicyId());
			arguments.put(ATTRIBUTE_MEMBERS, Integer.valueOf(fJavaElements.length).toString());
			for (int offset= 0; offset < fJavaElements.length; offset++)
				arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + 1), descriptor.elementToHandle(fJavaElements[offset]));
			arguments.putAll(getRefactoringArguments(project));
			return new RefactoringChangeDescriptor(descriptor);
		}

		private IType getDestinationAsType() throws JavaScriptModelException {
			IJavaScriptElement destination= getJavaElementDestination();
			IType enclosingType= getEnclosingType(destination);
			if (enclosingType != null)
				return enclosingType;
			IJavaScriptUnit enclosingCu= getEnclosingCu(destination);
			Assert.isNotNull(enclosingCu);
			IType mainType= JavaElementUtil.getMainType(enclosingCu);
			Assert.isNotNull(mainType);
			return mainType;
		}

		protected final IJavaScriptUnit getDestinationCu() {
			return getDestinationCu(getJavaElementDestination());
		}

		public final IJavaScriptElement[] getJavaElements() {
			return fJavaElements;
		}

		public final IResource[] getResources() {
			return new IResource[0];
		}

		private IProject getSingleProject() {
			IProject result= null;
			for (int index= 0; index < fJavaElements.length; index++) {
				if (result == null)
					result= fJavaElements[index].getJavaScriptProject().getProject();
				else if (!result.equals(fJavaElements[index].getJavaScriptProject().getProject()))
					return null;
			}
			return result;
		}

		protected final IJavaScriptUnit getSourceCu() {
			// all have a common parent, so all must be in the same cu
			// we checked before that the array in not null and not empty
			return (IJavaScriptUnit) fJavaElements[0].getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
		}

		public RefactoringStatus initialize(RefactoringArguments arguments) {
			final RefactoringStatus status= new RefactoringStatus();
			if (arguments instanceof JavaRefactoringArguments) {
				final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
				int memberCount= 0;
				String value= extended.getAttribute(ATTRIBUTE_MEMBERS);
				if (value != null && !"".equals(value)) {//$NON-NLS-1$
					try {
						memberCount= Integer.parseInt(value);
					} catch (NumberFormatException exception) {
						return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_MEMBERS));
					}
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_MEMBERS));
				String handle= null;
				List elements= new ArrayList();
				for (int index= 0; index < memberCount; index++) {
					final String attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (index + 1);
					handle= extended.getAttribute(attribute);
					if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
						final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
						if (element == null || !element.exists())
							status.merge(ScriptableRefactoring.createInputWarningStatus(element, getProcessorId(), getRefactoringId()));
						else
							elements.add(element);
					} else
						return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
				}
				fJavaElements= (IJavaScriptElement[]) elements.toArray(new IJavaScriptElement[elements.size()]);
			} else
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
			status.merge(super.initialize(arguments));
			return status;
		}

		private RefactoringStatus recursiveVerifyDestination(IJavaScriptElement destination) throws JavaScriptModelException {
			Assert.isNotNull(destination);
			if (!fCheckDestination)
				return new RefactoringStatus();
			if (!destination.exists())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_doesnotexist1);
			if (destination instanceof IJavaScriptModel)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_jmodel);
			if (!(destination instanceof IJavaScriptUnit) && !ReorgUtils.isInsideCompilationUnit(destination))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_cannot);

			IJavaScriptUnit destinationCu= getDestinationCu(destination);
			Assert.isNotNull(destinationCu);
			if (destinationCu.isReadOnly())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_cannot_modify);

			switch (destination.getElementType()) {
				case IJavaScriptElement.JAVASCRIPT_UNIT:
					int[] types0= new int[] { IJavaScriptElement.FIELD, IJavaScriptElement.INITIALIZER, IJavaScriptElement.METHOD};
					if (ReorgUtils.hasElementsOfType(getJavaElements(), types0))
						return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_cannot);
					break;

				case IJavaScriptElement.IMPORT_CONTAINER:
					if (ReorgUtils.hasElementsNotOfType(getJavaElements(), IJavaScriptElement.IMPORT_DECLARATION))
						return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_cannot);
					break;

				case IJavaScriptElement.IMPORT_DECLARATION:
					if (ReorgUtils.hasElementsNotOfType(getJavaElements(), IJavaScriptElement.IMPORT_DECLARATION))
						return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_cannot);
					break;

				case IJavaScriptElement.FIELD:// fall thru
				case IJavaScriptElement.INITIALIZER:// fall thru
				case IJavaScriptElement.METHOD:// fall thru
					return recursiveVerifyDestination(destination.getParent());

				case IJavaScriptElement.TYPE:
					int[] types1= new int[] { IJavaScriptElement.IMPORT_DECLARATION, IJavaScriptElement.IMPORT_CONTAINER};
					if (ReorgUtils.hasElementsOfType(getJavaElements(), types1))
						return recursiveVerifyDestination(destination.getParent());
					break;
			}

			return new RefactoringStatus();
		}

		protected RefactoringStatus verifyDestination(IJavaScriptElement destination) throws JavaScriptModelException {
			return recursiveVerifyDestination(destination);
		}

		protected final RefactoringStatus verifyDestination(IResource destination) throws JavaScriptModelException {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ReorgPolicyFactory_no_resource);
		}
	}

	private static final String ATTRIBUTE_FILES= "files"; //$NON-NLS-1$

	private static final String ATTRIBUTE_FOLDERS= "folders"; //$NON-NLS-1$

	private static final String ATTRIBUTE_FRAGMENTS= "fragments"; //$NON-NLS-1$

	private static final String ATTRIBUTE_LOG= "log"; //$NON-NLS-1$

	private static final String ATTRIBUTE_MEMBERS= "members"; //$NON-NLS-1$

	private static final String ATTRIBUTE_PATTERNS= "patterns"; //$NON-NLS-1$

	private static final String ATTRIBUTE_POLICY= "policy"; //$NON-NLS-1$

	private static final String ATTRIBUTE_QUALIFIED= "qualified"; //$NON-NLS-1$

	private static final String ATTRIBUTE_ROOTS= "roots"; //$NON-NLS-1$

	private static final String ATTRIBUTE_UNITS= "units"; //$NON-NLS-1$

	private static final String DELIMITER_ELEMENT= "\t"; //$NON-NLS-1$

	private static final String DELIMITER_RECORD= "\n"; //$NON-NLS-1$

	private static final String NO_ID= "no_id"; //$NON-NLS-1$

	private static final String NO_POLICY= "no_policy"; //$NON-NLS-1$

	private static final String UNUSED_STRING= "unused"; //$NON-NLS-1$

	private static boolean containsNull(Object[] objects) {
		for (int i= 0; i < objects.length; i++) {
			if (objects[i] == null)
				return true;
		}
		return false;
	}

	public static ICopyPolicy createCopyPolicy(IResource[] resources, IJavaScriptElement[] javaElements) throws JavaScriptModelException {
		return (ICopyPolicy) createReorgPolicy(true, resources, javaElements);
	}

	public static ICopyPolicy createCopyPolicy(RefactoringStatus status, RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			final String policy= extended.getAttribute(ATTRIBUTE_POLICY);
			if (policy != null && !"".equals(policy)) { //$NON-NLS-1$
				if (CopyFilesFoldersAndCusPolicy.POLICY_COPY_RESOURCE.equals(policy)) {
					return new CopyFilesFoldersAndCusPolicy(null, null, null);
				} else if (CopyPackageFragmentRootsPolicy.POLICY_COPY_ROOTS.equals(policy)) {
					return new CopyPackageFragmentRootsPolicy(null);
				} else if (CopyPackagesPolicy.POLICY_COPY_PACKAGES.equals(policy)) {
					return new CopyPackagesPolicy(null);
				} else if (CopySubCuElementsPolicy.POLICY_COPY_MEMBERS.equals(policy)) {
					return new CopySubCuElementsPolicy(null);
				} else
					status.merge(RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, new String[] { policy, ATTRIBUTE_POLICY})));
			} else
				status.merge(RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_POLICY)));
		} else
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments));
		return null;
	}

	public static IMovePolicy createMovePolicy(IResource[] resources, IJavaScriptElement[] javaElements) throws JavaScriptModelException {
		return (IMovePolicy) createReorgPolicy(false, resources, javaElements);
	}

	public static IMovePolicy createMovePolicy(RefactoringStatus status, RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			final String policy= extended.getAttribute(ATTRIBUTE_POLICY);
			if (policy != null && !"".equals(policy)) { //$NON-NLS-1$
				if (MoveFilesFoldersAndCusPolicy.POLICY_MOVE_RESOURCES.equals(policy)) {
					return new MoveFilesFoldersAndCusPolicy(null, null, null);
				} else if (MovePackageFragmentRootsPolicy.POLICY_MOVE_ROOTS.equals(policy)) {
					return new MovePackageFragmentRootsPolicy(null);
				} else if (MovePackagesPolicy.POLICY_MOVE_PACKAGES.equals(policy)) {
					return new MovePackagesPolicy(null);
				} else if (MoveSubCuElementsPolicy.POLICY_MOVE_MEMBERS.equals(policy)) {
					return new MoveSubCuElementsPolicy(null);
				} else
					status.merge(RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, new String[] { policy, ATTRIBUTE_POLICY})));
			} else
				status.merge(RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_POLICY)));
		} else
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments));
		return null;
	}

	private static IReorgPolicy createReorgPolicy(boolean copy, IResource[] selectedResources, IJavaScriptElement[] selectedJavaElements) throws JavaScriptModelException {
		final IReorgPolicy NO;
		if (copy)
			NO= new NoCopyPolicy();
		else
			NO= new NoMovePolicy();

		ActualSelectionComputer selectionComputer= new ActualSelectionComputer(selectedJavaElements, selectedResources);
		IResource[] resources= selectionComputer.getActualResourcesToReorg();
		IJavaScriptElement[] javaElements= selectionComputer.getActualJavaElementsToReorg();

		if ((resources.length + javaElements.length == 0) || containsNull(resources) || containsNull(javaElements) || ReorgUtils.isArchiveMember(javaElements) || ReorgUtils.hasElementsOfType(javaElements, IJavaScriptElement.JAVASCRIPT_PROJECT) || ReorgUtils.hasElementsOfType(javaElements, IJavaScriptElement.JAVASCRIPT_MODEL) || ReorgUtils.hasElementsOfType(resources, IResource.PROJECT | IResource.ROOT) || !new ParentChecker(resources, javaElements).haveCommonParent())
			return NO;

		if (ReorgUtils.hasElementsOfType(javaElements, IJavaScriptElement.PACKAGE_FRAGMENT)) {
			if (resources.length != 0 || ReorgUtils.hasElementsNotOfType(javaElements, IJavaScriptElement.PACKAGE_FRAGMENT))
				return NO;
			if (copy)
				return new CopyPackagesPolicy(ArrayTypeConverter.toPackageArray(javaElements));
			else
				return new MovePackagesPolicy(ArrayTypeConverter.toPackageArray(javaElements));
		}

		if (ReorgUtils.hasElementsOfType(javaElements, IJavaScriptElement.PACKAGE_FRAGMENT_ROOT)) {
			if (resources.length != 0 || ReorgUtils.hasElementsNotOfType(javaElements, IJavaScriptElement.PACKAGE_FRAGMENT_ROOT))
				return NO;
			if (copy)
				return new CopyPackageFragmentRootsPolicy(ArrayTypeConverter.toPackageFragmentRootArray(javaElements));
			else
				return new MovePackageFragmentRootsPolicy(ArrayTypeConverter.toPackageFragmentRootArray(javaElements));
		}

		if (ReorgUtils.hasElementsOfType(resources, IResource.FILE | IResource.FOLDER) || ReorgUtils.hasElementsOfType(javaElements, IJavaScriptElement.JAVASCRIPT_UNIT)) {
			if (ReorgUtils.hasElementsNotOfType(javaElements, IJavaScriptElement.JAVASCRIPT_UNIT))
				return NO;
			if (ReorgUtils.hasElementsNotOfType(resources, IResource.FILE | IResource.FOLDER))
				return NO;
			if (copy)
				return new CopyFilesFoldersAndCusPolicy(ReorgUtils.getFiles(resources), ReorgUtils.getFolders(resources), ArrayTypeConverter.toCuArray(javaElements));
			else
				return new MoveFilesFoldersAndCusPolicy(ReorgUtils.getFiles(resources), ReorgUtils.getFolders(resources), ArrayTypeConverter.toCuArray(javaElements));
		}

		if (hasElementsSmallerThanCuOrClassFile(javaElements)) {
			// assertions guaranteed by common parent
			Assert.isTrue(resources.length == 0);
			Assert.isTrue(!ReorgUtils.hasElementsOfType(javaElements, IJavaScriptElement.JAVASCRIPT_UNIT));
			Assert.isTrue(!ReorgUtils.hasElementsOfType(javaElements, IJavaScriptElement.CLASS_FILE));
			Assert.isTrue(!hasElementsLargerThanCuOrClassFile(javaElements));
			if (copy)
				return new CopySubCuElementsPolicy(javaElements);
			else
				return new MoveSubCuElementsPolicy(javaElements);
		}
		return NO;
	}

	private static boolean hasElementsLargerThanCuOrClassFile(IJavaScriptElement[] javaElements) {
		for (int i= 0; i < javaElements.length; i++) {
			if (!ReorgUtils.isInsideCompilationUnit(javaElements[i]) && !ReorgUtils.isInsideClassFile(javaElements[i]))
				return true;
		}
		return false;
	}

	private static boolean hasElementsSmallerThanCuOrClassFile(IJavaScriptElement[] javaElements) {
		for (int i= 0; i < javaElements.length; i++) {
			if (ReorgUtils.isInsideCompilationUnit(javaElements[i]))
				return true;
			if (ReorgUtils.isInsideClassFile(javaElements[i]))
				return true;
		}
		return false;
	}

	public static CreateTargetExecutionLog loadCreateTargetExecutionLog(RefactoringStatus status, JavaRefactoringArguments arguments) {
		CreateTargetExecutionLog log= new CreateTargetExecutionLog();
		final String value= arguments.getAttribute(ATTRIBUTE_LOG);
		if (value != null) {
			final StringTokenizer tokenizer= new StringTokenizer(value, DELIMITER_RECORD, false);
			while (tokenizer.hasMoreTokens()) {
				final String token= tokenizer.nextToken();
				processCreateTargetExecutionRecord(log, arguments, token);
			}
		}
		return log;
	}

	public static ReorgExecutionLog loadReorgExecutionLog(RefactoringStatus status, JavaRefactoringArguments arguments) {
		ReorgExecutionLog log= new ReorgExecutionLog();
		final String value= arguments.getAttribute(ATTRIBUTE_LOG);
		if (value != null) {
			final StringTokenizer tokenizer= new StringTokenizer(value, DELIMITER_RECORD, false);
			while (tokenizer.hasMoreTokens()) {
				final String token= tokenizer.nextToken();
				processReorgExecutionRecord(log, arguments, token);
			}
		}
		return log;
	}

	private static void processCreateTargetExecutionRecord(CreateTargetExecutionLog log, JavaRefactoringArguments arguments, String token) {
		final StringTokenizer tokenizer= new StringTokenizer(token, DELIMITER_ELEMENT, false);
		String value= null;
		if (tokenizer.hasMoreTokens()) {
			value= tokenizer.nextToken();
			Object selection= JDTRefactoringDescriptor.handleToElement(arguments.getProject(), value, false);
			if (selection == null)
				selection= JDTRefactoringDescriptor.handleToResource(arguments.getProject(), value);
			if (selection != null && tokenizer.hasMoreTokens()) {
				value= tokenizer.nextToken();
				Object created= JDTRefactoringDescriptor.handleToElement(arguments.getProject(), value, false);
				if (created == null)
					created= JDTRefactoringDescriptor.handleToResource(arguments.getProject(), value);
				if (created != null)
					log.markAsCreated(selection, created);
			}
		}
	}

	private static void processReorgExecutionRecord(ReorgExecutionLog log, JavaRefactoringArguments arguments, String token) {
		final StringTokenizer tokenizer= new StringTokenizer(token, DELIMITER_ELEMENT, false);
		String value= null;
		if (tokenizer.hasMoreTokens()) {
			value= tokenizer.nextToken();
			Object element= JDTRefactoringDescriptor.handleToElement(arguments.getProject(), value);
			if (element == null)
				element= JDTRefactoringDescriptor.handleToResource(arguments.getProject(), value);
			if (tokenizer.hasMoreTokens()) {
				final boolean processed= Boolean.valueOf(tokenizer.nextToken()).booleanValue();
				if (processed) {
					log.markAsProcessed(element);
					if (element instanceof IJavaScriptElement)
						log.markAsProcessed(JavaElementResourceMapping.create((IJavaScriptElement) element));
				}
				if (tokenizer.hasMoreTokens()) {
					final boolean renamed= Boolean.valueOf(tokenizer.nextToken()).booleanValue();
					if (renamed && tokenizer.hasMoreTokens()) {
						final String name= tokenizer.nextToken();
						log.setNewName(element, name);
						if (element instanceof IJavaScriptElement)
							log.setNewName(JavaElementResourceMapping.create((IJavaScriptElement) element), name);
					}
				}
			}
		}
	}

	public static void storeCreateTargetExecutionLog(String project, Map arguments, CreateTargetExecutionLog log) {
		if (log != null) {
			final StringBuffer buffer= new StringBuffer(64);
			final Object[] selections= log.getSelectedElements();
			for (int index= 0; index < selections.length; index++) {
				final Object selection= selections[index];
				if (selection != null) {
					final Object created= log.getCreatedElement(selection);
					if (created != null) {
						storeLogElement(buffer, project, selection);
						buffer.append(DELIMITER_ELEMENT);
						storeLogElement(buffer, project, created);
						buffer.append(DELIMITER_RECORD);
					}

				}
			}
			final String value= new String(buffer.toString().trim());
			if (!"".equals(value)) //$NON-NLS-1$
				arguments.put(ATTRIBUTE_LOG, value);
		}
	}

	private static boolean storeLogElement(StringBuffer buffer, String project, Object object) {
		if (object instanceof IJavaScriptElement) {
			final IJavaScriptElement element= (IJavaScriptElement) object;
			buffer.append(JDTRefactoringDescriptor.elementToHandle(project, element));
			return true;
		} else if (object instanceof IResource) {
			final IResource resource= (IResource) object;
			buffer.append(JDTRefactoringDescriptor.resourceToHandle(project, resource));
			return true;
		}
		return false;
	}

	public static void storeReorgExecutionLog(String project, Map arguments, ReorgExecutionLog log) {
		if (log != null) {
			final Set set= new HashSet();
			set.addAll(Arrays.asList(log.getProcessedElements()));
			set.addAll(Arrays.asList(log.getRenamedElements()));
			final StringBuffer buffer= new StringBuffer(64);
			for (final Iterator iterator= set.iterator(); iterator.hasNext();) {
				final Object object= iterator.next();
				if (storeLogElement(buffer, project, object)) {
					buffer.append(DELIMITER_ELEMENT);
					buffer.append(log.isProcessed(object));
					buffer.append(DELIMITER_ELEMENT);
					final boolean renamed= log.isRenamed(object);
					buffer.append(renamed);
					if (renamed) {
						buffer.append(DELIMITER_ELEMENT);
						buffer.append(log.getNewName(object));
					}
					buffer.append(DELIMITER_RECORD);
				}
			}
			final String value= new String(buffer.toString().trim());
			if (!"".equals(value)) //$NON-NLS-1$
				arguments.put(ATTRIBUTE_LOG, value);
		}
	}

	private ReorgPolicyFactory() {
		// private
	}
}
