/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Matt Chapman, mpchapman@gmail.com - 89977 Make JDT .java agnostic
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.ui.text.correction;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.core.search.TypeNameMatch;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.wst.jsdt.internal.corext.fix.IFix;
import org.eclipse.wst.jsdt.internal.corext.fix.UnusedCodeFix;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.AddToClasspathChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.RenameCompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.TypeNameMatchCollector;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.wst.jsdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.preferences.BuildPathsPropertyPage;
import org.eclipse.wst.jsdt.internal.ui.util.CoreUtility;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.wst.jsdt.launching.IVMInstall;
import org.eclipse.wst.jsdt.launching.IVMInstall2;
import org.eclipse.wst.jsdt.launching.IVMInstallType;
import org.eclipse.wst.jsdt.launching.JavaRuntime;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.actions.OrganizeImportsAction;
import org.eclipse.wst.jsdt.ui.text.java.IInvocationContext;
import org.eclipse.wst.jsdt.ui.text.java.IProblemLocation;

public class ReorgCorrectionsSubProcessor {

	public static void getWrongTypeNameProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		IJavaScriptUnit cu= context.getCompilationUnit();
		boolean isLinked= cu.getResource().isLinked();

		IJavaScriptProject javaProject= cu.getJavaScriptProject();
		String sourceLevel= javaProject.getOption(JavaScriptCore.COMPILER_SOURCE, true);
		String compliance= javaProject.getOption(JavaScriptCore.COMPILER_COMPLIANCE, true);
		
		JavaScriptUnit root= context.getASTRoot();
		
		ASTNode coveredNode= problem.getCoveredNode(root);
		if (!(coveredNode instanceof SimpleName))
			return;		
		
		ASTNode parentType= coveredNode.getParent();
		if (!(parentType instanceof AbstractTypeDeclaration))
			return;
		
		String currTypeName= ((SimpleName) coveredNode).getIdentifier();
		String newTypeName= JavaScriptCore.removeJavaScriptLikeExtension(cu.getElementName());
				
		boolean hasOtherPublicTypeBefore= false;
		
		boolean found= false;
		List types= root.types();
		for (int i= 0; i < types.size(); i++) {
			 AbstractTypeDeclaration curr= (AbstractTypeDeclaration) types.get(i);
			 if (parentType != curr) {
				 if (newTypeName.equals(curr.getName().getIdentifier())) {
					 return;
				 }
				 if (!found && Modifier.isPublic(curr.getModifiers())) {
					 hasOtherPublicTypeBefore= true;
				 }
			 } else {
				 found= true;
			 }
		 }
		if (!JavaScriptConventions.validateJavaScriptTypeName(newTypeName, sourceLevel, compliance).matches(IStatus.ERROR)) {
			proposals.add(new CorrectMainTypeNameProposal(cu, context, currTypeName, newTypeName, 5));
		}
		
		if (!hasOtherPublicTypeBefore) {
			String newCUName= JavaModelUtil.getRenamedCUName(cu, currTypeName);
			IJavaScriptUnit newCU= ((IPackageFragment) (cu.getParent())).getJavaScriptUnit(newCUName);
			if (!newCU.exists() && !isLinked && !JavaScriptConventions.validateCompilationUnitName(newCUName, sourceLevel, compliance).matches(IStatus.ERROR)) {
				RenameCompilationUnitChange change= new RenameCompilationUnitChange(cu, newCUName);
	
				// rename CU
				String label= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_renamecu_description, newCUName);
				proposals.add(new ChangeCorrectionProposal(label, change, 6, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_RENAME)));
			}
		}
	}

	public static void removeImportStatementProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		IFix fix= UnusedCodeFix.createRemoveUnusedImportFix(context.getASTRoot(), problem);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_DELETE_IMPORT);
			Map options= new Hashtable();
			options.put(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS, CleanUpConstants.TRUE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new UnusedCodeCleanUp(options), 6, image, context);
			proposals.add(proposal);
		}
		
		final IJavaScriptUnit cu= context.getCompilationUnit();
		String name= CorrectionMessages.ReorgCorrectionsSubProcessor_organizeimports_description;
		ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(name, null, 5, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)) {
			public void apply(IDocument document) {
				IEditorInput input= new FileEditorInput((IFile) cu.getResource());
				IWorkbenchPage p= JavaScriptPlugin.getActivePage();
				if (p == null) {
					return;
				}
				IEditorPart part= p.findEditor(input);
				if (part instanceof JavaEditor) {
					OrganizeImportsAction action= new OrganizeImportsAction((JavaEditor) part);
					action.run(cu);
				}
			}
		};
		proposals.add(proposal);
	}

	public static void importNotFoundProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		IJavaScriptUnit cu= context.getCompilationUnit();
		IJavaScriptProject project= cu.getJavaScriptProject();

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode != null) {
			ImportDeclaration importDeclaration= (ImportDeclaration) ASTNodes.getParent(selectedNode, ASTNode.IMPORT_DECLARATION);
			if (importDeclaration == null) {
				return;
			}
			if (!importDeclaration.isOnDemand()) {
				int kind= JavaModelUtil.is50OrHigher(cu.getJavaScriptProject()) ? SimilarElementsRequestor.REF_TYPES : SimilarElementsRequestor.CLASSES | SimilarElementsRequestor.INTERFACES;
				UnresolvedElementsSubProcessor.addNewTypeProposals(cu, importDeclaration.getName(), kind, 5, proposals);
			}
			
			
			String name= ASTNodes.asString(importDeclaration.getName());
			char[] packageName;
			char[] typeName= null;
			if (importDeclaration.isOnDemand()) {
				packageName= name.toCharArray();
			} else {
				packageName= Signature.getQualifier(name).toCharArray();
				typeName= Signature.getSimpleName(name).toCharArray();
			}
			IJavaScriptSearchScope scope= SearchEngine.createWorkspaceScope();
			ArrayList res= new ArrayList();
			TypeNameMatchCollector requestor= new TypeNameMatchCollector(res);
			int matchMode= SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE;
			new SearchEngine().searchAllTypeNames(packageName, matchMode, typeName,
					matchMode, IJavaScriptSearchConstants.TYPE, scope, requestor,
					IJavaScriptSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, null);

			if (res.isEmpty()) {
				return;
			}
			HashSet addedClaspaths= new HashSet();
			for (int i= 0; i < res.size(); i++) {
				TypeNameMatch curr= (TypeNameMatch) res.get(i);
				IType type= curr.getType();
				if (type != null) {
					IPackageFragmentRoot root= (IPackageFragmentRoot) type.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
					IIncludePathEntry entry= root.getRawIncludepathEntry();
					if (entry == null) {
						continue;
					}
					IJavaScriptProject other= root.getJavaScriptProject();
					int entryKind= entry.getEntryKind();
					if ((entry.isExported() || entryKind == IIncludePathEntry.CPE_SOURCE) && addedClaspaths.add(other)) {
						String[] args= { other.getElementName(), project.getElementName() };
						String label= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_addcp_project_description, args);
						IIncludePathEntry newEntry= JavaScriptCore.newProjectEntry(other.getPath());
						AddToClasspathChange change= new AddToClasspathChange(project, newEntry);
						if (change.validateClasspath()) {
							ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(label, change, 8, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
							proposals.add(proposal);
						}
					}
					if ((entryKind == IIncludePathEntry.CPE_LIBRARY || entryKind == IIncludePathEntry.CPE_VARIABLE || entryKind == IIncludePathEntry.CPE_CONTAINER) && addedClaspaths.add(entry)) {
						String label= getAddClasspathLabel(entry, root, project);
						if (label != null) {
							AddToClasspathChange change= new AddToClasspathChange(project, entry);
							if (change.validateClasspath()) {
								ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(label, change, 7, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
								proposals.add(proposal);
							}
						}
					}
				}
			}
		}
	}

	private static String getAddClasspathLabel(IIncludePathEntry entry, IPackageFragmentRoot root, IJavaScriptProject project) {
		switch (entry.getEntryKind()) {
			case IIncludePathEntry.CPE_LIBRARY:
				if (root.isArchive()) {
					String[] args= { JavaScriptElementLabels.getElementLabel(root, JavaScriptElementLabels.REFERENCED_ROOT_POST_QUALIFIED), project.getElementName() };
					return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_addcp_archive_description, args);
				} else {
					String[] args= { JavaScriptElementLabels.getElementLabel(root, JavaScriptElementLabels.REFERENCED_ROOT_POST_QUALIFIED), project.getElementName() };
					return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_addcp_classfolder_description, args);
				}
			case IIncludePathEntry.CPE_VARIABLE: {
					String[] args= { JavaScriptElementLabels.getElementLabel(root, 0), project.getElementName() };
					return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_addcp_variable_description, args);
				}
			case IIncludePathEntry.CPE_CONTAINER:
				try {
					String[] args= { JavaScriptElementLabels.getContainerEntryLabel(entry.getPath(), root.getJavaScriptProject()), project.getElementName() };
					return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_addcp_library_description, args);
				} catch (JavaScriptModelException e) {
					// ignore
				}
				break;
			}
		return null;
	}

	private static final class OpenBuildPathCorrectionProposal extends ChangeCorrectionProposal {
		private final IProject fProject;
		private final IBinding fReferencedType;
		private OpenBuildPathCorrectionProposal(IProject project, String label, int relevance, IBinding referencedType) {
			super(label, null, relevance, null);
			fProject= project;
			fReferencedType= referencedType;
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ACCESSRULES_ATTRIB));
		}
		public void apply(IDocument document) {
			Map data= null;
			if (fReferencedType != null) {
				IJavaScriptElement elem= fReferencedType.getJavaElement();
				if (elem != null) {
					IPackageFragmentRoot root= (IPackageFragmentRoot) elem.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
					if (root != null) {
						try {
							IIncludePathEntry entry= root.getRawIncludepathEntry();
							if (entry != null) {
								data= new HashMap(1);
								data.put(BuildPathsPropertyPage.DATA_REVEAL_ENTRY, entry);
								if (entry.getEntryKind() != IIncludePathEntry.CPE_CONTAINER) {
									data.put(BuildPathsPropertyPage.DATA_REVEAL_ATTRIBUTE_KEY, CPListElement.ACCESSRULES);
								}
							}
						} catch (JavaScriptModelException e) {
							// ignore
						}
					}
				}
			}
			PreferencesUtil.createPropertyDialogOn(JavaScriptPlugin.getActiveWorkbenchShell(), fProject, BuildPathsPropertyPage.PROP_ID, null, data).open();
		}
		public String getAdditionalProposalInfo() {
			return Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_configure_buildpath_description, fProject.getName());
		}
	}

	private static final class ChangeTo50Compliance extends ChangeCorrectionProposal implements IWorkspaceRunnable {
		
		private final IJavaScriptProject fProject;
		private final boolean fChangeOnWorkspace;
		
		private Job fUpdateJob;
		private boolean f50JREFound;
		
		public ChangeTo50Compliance(String name, IJavaScriptProject project, boolean changeOnWorkspace, int relevance) {
			super(name, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
			fProject= project;
			fChangeOnWorkspace= changeOnWorkspace;
			fUpdateJob= null;
			f50JREFound= false;
		}
		
		private boolean is50orGreaterVMInstall(IVMInstall install) {
			if (install instanceof IVMInstall2) {
				String compliance= JavaModelUtil.getCompilerCompliance((IVMInstall2) install, JavaScriptCore.VERSION_1_3);
				return JavaModelUtil.is50OrHigher(compliance);
			}
			return false;
		}
		
		private IVMInstall find50OrGreaterVMInstall() {
			IVMInstallType[] installTypes= JavaRuntime.getVMInstallTypes();
			for (int i= 0; i < installTypes.length; i++) {
				IVMInstall[] installs= installTypes[i].getVMInstalls();
				for (int k= 0; k < installs.length; k++) {
					if (is50orGreaterVMInstall(installs[k])) {
						return installs[k];
					}
				}
			}
			return null;
		}
		
		public void run(IProgressMonitor monitor) throws CoreException {
			boolean needsBuild= updateJRE(monitor);
			if (needsBuild) {
				fUpdateJob= CoreUtility.getBuildJob(fChangeOnWorkspace ? null : fProject.getProject());
			}
		}
		
		private boolean updateJRE( IProgressMonitor monitor) throws CoreException, JavaScriptModelException {
			try {
				IVMInstall vm50Install= find50OrGreaterVMInstall();
				f50JREFound= vm50Install != null;
				if (vm50Install != null) {
					IVMInstall install= JavaRuntime.getVMInstall(fProject); // can be null
					if (fChangeOnWorkspace) {
						monitor.beginTask(CorrectionMessages.ReorgCorrectionsSubProcessor_50_compliance_operation, 4);
						IVMInstall defaultVM= JavaRuntime.getDefaultVMInstall(); // can be null
						if (defaultVM != null && !defaultVM.equals(install)) {
							IPath newPath= new Path(JavaRuntime.JRE_CONTAINER);
							updateClasspath(newPath, new SubProgressMonitor(monitor, 1));
						} else {
							monitor.worked(1);
						}
						if (defaultVM == null || !is50orGreaterVMInstall(defaultVM)) {
							JavaRuntime.setDefaultVMInstall(vm50Install, new SubProgressMonitor(monitor, 3), true);
							return false;
						}
						return true;
					} else {
						if (install == null || !is50orGreaterVMInstall(install)) {
							IPath newPath= new Path(JavaRuntime.JRE_CONTAINER).append(vm50Install.getVMInstallType().getId()).append(vm50Install.getName());
							updateClasspath(newPath, monitor);
							return false;
						}
					}
				}
			} finally {
				monitor.done();
			}
			return true;
		}

		private void updateClasspath(IPath newPath, IProgressMonitor monitor) throws JavaScriptModelException {
			IIncludePathEntry[] classpath= fProject.getRawIncludepath();
			IPath jreContainerPath= new Path(JavaRuntime.JRE_CONTAINER);
			for (int i= 0; i < classpath.length; i++) {
				IIncludePathEntry curr= classpath[i];
				if (curr.getEntryKind() == IIncludePathEntry.CPE_CONTAINER && curr.getPath().matchingFirstSegments(jreContainerPath) > 0) {
					classpath[i]= JavaScriptCore.newContainerEntry(newPath, curr.getAccessRules(), curr.getExtraAttributes(), curr.isExported());
				}
			}
			fProject.setRawIncludepath(classpath, monitor);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
		 */
		public String getAdditionalProposalInfo() {
			StringBuffer message= new StringBuffer();
			if (fChangeOnWorkspace) {
				message.append(CorrectionMessages.ReorgCorrectionsSubProcessor_50_compliance_changeworkspace_description);
			} else {
				message.append(CorrectionMessages.ReorgCorrectionsSubProcessor_50_compliance_changeproject_description);
			}
			
			IVMInstall vm50Install= find50OrGreaterVMInstall();
			if (vm50Install != null) {
				try {
					IVMInstall install= JavaRuntime.getVMInstall(fProject); // can be null
					if (fChangeOnWorkspace) {
						IVMInstall defaultVM= JavaRuntime.getDefaultVMInstall(); // can be null
						if (defaultVM != null && !defaultVM.equals(install)) {
							message.append(CorrectionMessages.ReorgCorrectionsSubProcessor_50_compliance_changeProjectJREToDefault_description);
						}
						if (defaultVM == null || !is50orGreaterVMInstall(defaultVM)) {
							message.append(Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_50_compliance_changeWorkspaceJRE_description, vm50Install.getName()));
						}
					} else {
						if (install == null || !is50orGreaterVMInstall(install)) {
							message.append(Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_50_compliance_changeProjectJRE_description, vm50Install.getName()));
						}
					}
				} catch (CoreException e) {
					// ignore
				}
			}
			return message.toString();
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#apply(IDocument)
		 */
		public void apply(IDocument document) {
			if (fChangeOnWorkspace) {
				Hashtable map= JavaScriptCore.getOptions();
				JavaModelUtil.set50CompilanceOptions(map);
				JavaScriptCore.setOptions(map);
			} else {
				Map map= fProject.getOptions(false);
				int optionsCount= map.size();
				JavaModelUtil.set50CompilanceOptions(map);
				if (map.size() > optionsCount) {
					// options have been added -> ensure that all compliance options from preference page set
					JavaModelUtil.setDefaultClassfileOptions(map, JavaScriptCore.VERSION_1_5);
				}
				fProject.setOptions(map);
			}
			try {
				IProgressService progressService= PlatformUI.getWorkbench().getProgressService();
				progressService.run(true, true, new WorkbenchRunnableAdapter(this));
			} catch (InvocationTargetException e) {
				JavaScriptPlugin.log(e);
			} catch (InterruptedException e) {
				return;
			}
			
			if (fUpdateJob != null) {
				fUpdateJob.schedule();
			}
			
			if (!f50JREFound) {
				MessageDialog.openInformation(JavaScriptPlugin.getActiveWorkbenchShell(), CorrectionMessages.ReorgCorrectionsSubProcessor_no_50jre_title, CorrectionMessages.ReorgCorrectionsSubProcessor_no_50jre_message);
			}
		}
	}

	public static void getNeed50ComplianceProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		IJavaScriptProject project= context.getCompilationUnit().getJavaScriptProject();
		
		String label1= CorrectionMessages.ReorgCorrectionsSubProcessor_50_project_compliance_description;
		proposals.add(new ChangeTo50Compliance(label1, project, false, 5));

		if (project.getOption(JavaScriptCore.COMPILER_COMPLIANCE, false) == null) {
			String label2= CorrectionMessages.ReorgCorrectionsSubProcessor_50_workspace_compliance_description;
			proposals.add(new ChangeTo50Compliance(label2, project, true, 6));
		}
	}

	public static void getIncorrectBuildPathProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		IProject project= context.getCompilationUnit().getJavaScriptProject().getProject();
		String label= CorrectionMessages.ReorgCorrectionsSubProcessor_configure_buildpath_label;
		OpenBuildPathCorrectionProposal proposal= new OpenBuildPathCorrectionProposal(project, label, 5, null);
		proposals.add(proposal);
	}

	public static void getAccessRulesProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		IBinding referencedElement= null;
		ASTNode node= problem.getCoveredNode(context.getASTRoot());
		if (node instanceof Type) {
			referencedElement= ((Type) node).resolveBinding();
		} else if (node instanceof Name) {
			referencedElement= ((Name) node).resolveBinding();
		}
		IProject project= context.getCompilationUnit().getJavaScriptProject().getProject();
		String label= CorrectionMessages.ReorgCorrectionsSubProcessor_accessrules_description;
		OpenBuildPathCorrectionProposal proposal= new OpenBuildPathCorrectionProposal(project, label, 5, referencedElement);
		proposals.add(proposal);
	}
}
