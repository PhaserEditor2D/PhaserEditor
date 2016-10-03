/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.refactoring.binary;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.jar.JarFile;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.ui.refactoring.history.RefactoringHistoryWizard;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.JavaScriptRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringContribution;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.binary.SourceCreationOperation;
import org.eclipse.wst.jsdt.internal.corext.refactoring.binary.StubCreationOperation;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.util.CoreUtility;

/**
 * Partial implementation of a refactoring history wizard which creates stubs
 * from a binary package fragment root while refactoring.
 * 
 * 
 */
public abstract class BinaryRefactoringHistoryWizard extends RefactoringHistoryWizard {

	/** The meta-inf fragment */
	private static final String META_INF_FRAGMENT= JarFile.MANIFEST_NAME.substring(0, JarFile.MANIFEST_NAME.indexOf('/'));

	/** The temporary linked source folder */
	private static final String SOURCE_FOLDER= ".src"; //$NON-NLS-1$

	/** The temporary stubs folder */
	private static final String STUB_FOLDER= ".stubs"; //$NON-NLS-1$

	/**
	 * Updates the new classpath with exclusion patterns for the specified path.
	 * 
	 * @param entries
	 *            the classpath entries
	 * @param path
	 *            the path
	 */
	private static void addExclusionPatterns(final List entries, final IPath path) {
		for (int index= 0; index < entries.size(); index++) {
			final IIncludePathEntry entry= (IIncludePathEntry) entries.get(index);
			if (entry.getEntryKind() == IIncludePathEntry.CPE_SOURCE && entry.getPath().isPrefixOf(path)) {
				final IPath[] patterns= entry.getExclusionPatterns();
				if (!JavaModelUtil.isExcludedPath(path, patterns)) {
					final IPath[] filters= new IPath[patterns.length + 1];
					System.arraycopy(patterns, 0, filters, 0, patterns.length);
					filters[patterns.length]= path.removeFirstSegments(entry.getPath().segmentCount()).addTrailingSeparator();
					entries.set(index, JavaScriptCore.newSourceEntry(entry.getPath(), filters, null));
				}
			}
		}
	}

	/**
	 * Checks whether the archive referenced by the package fragment root is not
	 * shared with multiple java projects in the workspace.
	 * 
	 * @param root
	 *            the package fragment root
	 * @param monitor
	 *            the progress monitor to use
	 * @return the status of the operation
	 */
	private static RefactoringStatus checkPackageFragmentRoots(final IPackageFragmentRoot root, final IProgressMonitor monitor) {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask(RefactoringMessages.JarImportWizard_prepare_import, 100);
			final IWorkspaceRoot workspace= ResourcesPlugin.getWorkspace().getRoot();
			if (workspace != null) {
				final IJavaScriptModel model= JavaScriptCore.create(workspace);
				if (model != null) {
					try {
						final URI uri= getLocationURI(root.getRawIncludepathEntry());
						if (uri != null) {
							final IJavaScriptProject[] projects= model.getJavaScriptProjects();
							final IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
							try {
								subMonitor.beginTask(RefactoringMessages.JarImportWizard_prepare_import, projects.length * 100);
								for (int index= 0; index < projects.length; index++) {
									final IPackageFragmentRoot[] roots= projects[index].getPackageFragmentRoots();
									final IProgressMonitor subsubMonitor= new SubProgressMonitor(subMonitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
									try {
										subsubMonitor.beginTask(RefactoringMessages.JarImportWizard_prepare_import, roots.length);
										for (int offset= 0; offset < roots.length; offset++) {
											final IPackageFragmentRoot current= roots[offset];
											if (!current.equals(root) && current.getKind() == IPackageFragmentRoot.K_BINARY) {
												final IIncludePathEntry entry= current.getRawIncludepathEntry();
												if (entry.getEntryKind() == IIncludePathEntry.CPE_LIBRARY) {
													final URI location= getLocationURI(entry);
													if (uri.equals(location))
														status.addFatalError(Messages.format(RefactoringMessages.JarImportWizard_error_shared_jar, new String[] { current.getJavaScriptProject().getElementName() }));
												}
											}
											subsubMonitor.worked(1);
										}
									} finally {
										subsubMonitor.done();
									}
								}
							} finally {
								subMonitor.done();
							}
						}
					} catch (CoreException exception) {
						status.addError(exception.getLocalizedMessage());
					}
				}
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * Configures the classpath of the project before refactoring.
	 * 
	 * @param project
	 *            the java project
	 * @param root
	 *            the package fragment root to refactor
	 * @param folder
	 *            the temporary source folder
	 * @param monitor
	 *            the progress monitor to use
	 * @throws IllegalStateException
	 *             if the plugin state location does not exist
	 * @throws CoreException
	 *             if an error occurs while configuring the class path
	 */
	private static void configureClasspath(final IJavaScriptProject project, final IPackageFragmentRoot root, final IFolder folder, final IProgressMonitor monitor) throws IllegalStateException, CoreException {
		try {
			monitor.beginTask(RefactoringMessages.JarImportWizard_prepare_import, 200);
			final IIncludePathEntry entry= root.getRawIncludepathEntry();
			final IIncludePathEntry[] entries= project.getRawIncludepath();
			final List list= new ArrayList();
			list.addAll(Arrays.asList(entries));
			final IFileStore store= EFS.getLocalFileSystem().getStore(JavaScriptPlugin.getDefault().getStateLocation().append(STUB_FOLDER).append(project.getElementName()));
			if (store.fetchInfo(EFS.NONE, new SubProgressMonitor(monitor, 25, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)).exists())
				store.delete(EFS.NONE, new SubProgressMonitor(monitor, 25, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			store.mkdir(EFS.NONE, new SubProgressMonitor(monitor, 25, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			folder.createLink(store.toURI(), IResource.NONE, new SubProgressMonitor(monitor, 25, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			addExclusionPatterns(list, folder.getFullPath());
			for (int index= 0; index < entries.length; index++) {
				if (entries[index].equals(entry))
					list.add(index, JavaScriptCore.newSourceEntry(folder.getFullPath()));
			}
			project.setRawIncludepath((IIncludePathEntry[]) list.toArray(new IIncludePathEntry[list.size()]), false, new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
		} finally {
			monitor.done();
		}
	}

	/**
	 * Returns the location URI of the classpath entry
	 * 
	 * @param entry
	 *            the classpath entry
	 * @return the location URI
	 */
	public static URI getLocationURI(final IIncludePathEntry entry) {
		IPath path= null;
		if (entry.getEntryKind() == IIncludePathEntry.CPE_VARIABLE)
			path= JavaScriptCore.getResolvedVariablePath(entry.getPath());
		else
			path= entry.getPath();
		final IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		URI location= null;
		if (root.exists(path)) {
			location= root.getFile(path).getRawLocationURI();
		} else
			location= URIUtil.toURI(path);
		return location;
	}

	/** Is auto build enabled? */
	protected boolean fAutoBuild= true;

	/** Has the wizard been cancelled? */
	protected boolean fCancelled= false;

	/** The current refactoring arguments, or <code>null</code> */
	protected RefactoringArguments fCurrentArguments= null;

	/** The current refactoring to be initialized, or <code>null</code> */
	protected IScriptableRefactoring fCurrentRefactoring= null;

	/** The java project or <code>null</code> */
	protected IJavaScriptProject fJavaProject= null;

	/**
	 * The packages which already have been processed (element type:
	 * &lt;IPackageFragment&gt;)
	 */
	protected final Collection fProcessedFragments= new HashSet();

	/** The temporary source folder, or <code>null</code> */
	protected IFolder fSourceFolder= null;

	/**
	 * Creates a new stub refactoring history wizard.
	 * 
	 * @param overview
	 *            <code>true</code> to show an overview of the refactorings,
	 *            <code>false</code> otherwise
	 * @param caption
	 *            the wizard caption
	 * @param title
	 *            the wizard title
	 * @param description
	 *            the wizard description
	 */
	protected BinaryRefactoringHistoryWizard(final boolean overview, final String caption, final String title, final String description) {
		super(overview, caption, title, description);
	}

	/**
	 * Creates a new stub refactoring history wizard.
	 * 
	 * @param caption
	 *            the wizard caption
	 * @param title
	 *            the wizard title
	 * @param description
	 *            the wizard description
	 */
	protected BinaryRefactoringHistoryWizard(final String caption, final String title, final String description) {
		super(caption, title, description);
	}

	/**
	 * {@inheritDoc}
	 */
	protected RefactoringStatus aboutToPerformHistory(final IProgressMonitor monitor) {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			fJavaProject= null;
			fSourceFolder= null;
			fProcessedFragments.clear();
			monitor.beginTask(RefactoringMessages.JarImportWizard_prepare_import, 520);
			status.merge(super.aboutToPerformHistory(new SubProgressMonitor(monitor, 10, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)));
			if (!status.hasFatalError()) {
				final IPackageFragmentRoot root= getPackageFragmentRoot();
				if (root != null) {
					status.merge(checkPackageFragmentRoots(root, new SubProgressMonitor(monitor, 90, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)));
					if (!status.hasFatalError()) {
						status.merge(checkSourceAttachmentRefactorings(new SubProgressMonitor(monitor, 20, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)));
						if (!status.hasFatalError()) {
							final IJavaScriptProject project= root.getJavaScriptProject();
							if (project != null) {
								final IFolder folder= project.getProject().getFolder(SOURCE_FOLDER + String.valueOf(System.currentTimeMillis()));
								try {
									fAutoBuild= CoreUtility.enableAutoBuild(false);
									final RefactoringHistory history= getRefactoringHistory();
									if (history != null && !history.isEmpty())
										configureClasspath(project, root, folder, new SubProgressMonitor(monitor, 300, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
								} catch (CoreException exception) {
									status.merge(RefactoringStatus.createFatalErrorStatus(exception.getLocalizedMessage()));
									try {
										project.setRawIncludepath(project.readRawIncludepath(), false, new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
									} catch (CoreException throwable) {
										JavaScriptPlugin.log(throwable);
									}
								} finally {
									if (!status.hasFatalError()) {
										fJavaProject= project;
										fSourceFolder= folder;
									}
								}
							}
						}
					}
				}
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * {@inheritDoc}
	 */
	protected RefactoringStatus aboutToPerformRefactoring(final Refactoring refactoring, final RefactoringDescriptor descriptor, final IProgressMonitor monitor) {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask(RefactoringMessages.JarImportWizard_prepare_import, 100);
			status.merge(createNecessarySourceCode(refactoring, new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)));
			if (!status.hasFatalError()) {
				if (fCurrentRefactoring != null && fCurrentArguments != null)
					status.merge(fCurrentRefactoring.initialize(fCurrentArguments));
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * Can this wizard use the source attachment of the package fragment root if
	 * necessary?
	 * 
	 * @return <code>true</code> to use the source attachment,
	 *         <code>false</code> otherwise
	 */
	protected boolean canUseSourceAttachment() {
		final IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root != null) {
			try {
				return root.getSourceAttachmentPath() != null;
			} catch (JavaScriptModelException exception) {
				JavaScriptPlugin.log(exception);
			}
		}
		return false;
	}

	/**
	 * Checks whether there are any refactorings to be executed which need a
	 * source attachment, but none exists.
	 * 
	 * @param monitor
	 *            the progress monitor
	 * @return a status describing the outcome of the check
	 */
	protected RefactoringStatus checkSourceAttachmentRefactorings(final IProgressMonitor monitor) {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			if (!canUseSourceAttachment()) {
				final RefactoringDescriptorProxy[] proxies= getRefactoringHistory().getDescriptors();
				monitor.beginTask(RefactoringMessages.JarImportWizard_prepare_import, proxies.length * 100);
				for (int index= 0; index < proxies.length; index++) {
					final RefactoringDescriptor descriptor= proxies[index].requestDescriptor(new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
					if (descriptor != null) {
						final int flags= descriptor.getFlags();
						if ((flags & JavaScriptRefactoringDescriptor.JAR_SOURCE_ATTACHMENT) != 0)
							status.merge(RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringMessages.BinaryRefactoringHistoryWizard_error_missing_source_attachment, descriptor.getDescription())));
					}
				}
			} else
				monitor.beginTask(RefactoringMessages.JarImportWizard_prepare_import, 1);
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * Creates the necessary source code for the refactoring.
	 * 
	 * @param refactoring
	 *            the refactoring to create the source code for
	 * @param monitor
	 *            the progress monitor to use
	 */
	private RefactoringStatus createNecessarySourceCode(final Refactoring refactoring, final IProgressMonitor monitor) {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask(RefactoringMessages.JarImportWizard_prepare_import, 240);
			final IPackageFragmentRoot root= getPackageFragmentRoot();
			if (root != null && fSourceFolder != null && fJavaProject != null) {
				try {
					final SubProgressMonitor subMonitor= new SubProgressMonitor(monitor, 40, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
					final IJavaScriptElement[] elements= root.getChildren();
					final List list= new ArrayList(elements.length);
					try {
						subMonitor.beginTask(RefactoringMessages.JarImportWizard_prepare_import, elements.length);
						for (int index= 0; index < elements.length; index++) {
							final IJavaScriptElement element= elements[index];
							if (!fProcessedFragments.contains(element) && !element.getElementName().equals(META_INF_FRAGMENT))
								list.add(element);
							subMonitor.worked(1);
						}
					} finally {
						subMonitor.done();
					}
					if (!list.isEmpty()) {
						fProcessedFragments.addAll(list);
						final URI uri= fSourceFolder.getRawLocationURI();
						if (uri != null) {
							final IPackageFragmentRoot sourceFolder= fJavaProject.getPackageFragmentRoot(fSourceFolder);
							IWorkspaceRunnable runnable= null;
							if (canUseSourceAttachment()) {
								runnable= new SourceCreationOperation(uri, list) {

									private IPackageFragment fFragment= null;

									protected final void createCompilationUnit(final IFileStore store, final String name, final String content, final IProgressMonitor pm) throws CoreException {
										fFragment.createCompilationUnit(name, content, true, pm);
									}

									protected final void createPackageFragment(final IFileStore store, final String name, final IProgressMonitor pm) throws CoreException {
										fFragment= sourceFolder.createPackageFragment(name, true, pm);
									}
								};
							} else {
								runnable= new StubCreationOperation(uri, list, true) {

									private IPackageFragment fFragment= null;

									protected final void createCompilationUnit(final IFileStore store, final String name, final String content, final IProgressMonitor pm) throws CoreException {
										fFragment.createCompilationUnit(name, content, true, pm);
									}

									protected final void createPackageFragment(final IFileStore store, final String name, final IProgressMonitor pm) throws CoreException {
										fFragment= sourceFolder.createPackageFragment(name, true, pm);
									}
								};
							}
							try {
								runnable.run(new SubProgressMonitor(monitor, 150, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
							} finally {
								fSourceFolder.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 50, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
							}
						}
					}
				} catch (CoreException exception) {
					status.addFatalError(exception.getLocalizedMessage());
				}
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * {@inheritDoc}
	 */
	protected Refactoring createRefactoring(final RefactoringDescriptor descriptor, final RefactoringStatus status) throws CoreException {
		Assert.isNotNull(descriptor);
		Refactoring refactoring= null;
		if (descriptor instanceof JDTRefactoringDescriptor) {
			final JDTRefactoringDescriptor javaDescriptor= (JDTRefactoringDescriptor) descriptor;
			final RefactoringContribution contribution= RefactoringCore.getRefactoringContribution(javaDescriptor.getID());
			if (contribution instanceof JDTRefactoringContribution) {
				final JDTRefactoringContribution extended= (JDTRefactoringContribution) contribution;
				refactoring= extended.createRefactoring(descriptor);
			}
			if (refactoring != null) {
				final RefactoringArguments arguments= javaDescriptor.createArguments();
				if (arguments instanceof JavaRefactoringArguments) {
					final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
					if (fJavaProject != null) {
						final String name= fJavaProject.getElementName();
						extended.setProject(name);
						String handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_INPUT);
						if (handle != null && !"".equals(handle)) //$NON-NLS-1$
							extended.setAttribute(JDTRefactoringDescriptor.ATTRIBUTE_INPUT, getTransformedHandle(name, handle));
						int count= 1;
						String attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + count;
						while ((handle= extended.getAttribute(attribute)) != null) {
							if (!"".equals(handle)) //$NON-NLS-1$
								extended.setAttribute(attribute, getTransformedHandle(name, handle));
							count++;
							attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + count;
						}
					}
				} else
					status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments));
				if (refactoring instanceof IScriptableRefactoring) {
					fCurrentRefactoring= (IScriptableRefactoring) refactoring;
					fCurrentArguments= arguments;
				} else
					status.merge(RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptor_initialization_error, javaDescriptor.getID())));
			}
			return refactoring;
		}
		return null;
	}

	/**
	 * Deconfigures the classpath after all refactoring have been performed.
	 * 
	 * @param entries
	 *            the classpath entries to reset the project to
	 * @param monitor
	 *            the progress monitor to use
	 * @return <code>true</code> if the classpath has been changed,
	 *         <code>false</code> otherwise
	 * @throws CoreException
	 *             if an error occurs while deconfiguring the classpath
	 */
	protected boolean deconfigureClasspath(IIncludePathEntry[] entries, IProgressMonitor monitor) throws CoreException {
		return false;
	}

	/**
	 * Deconfigures the classpath of the project after refactoring.
	 * 
	 * @param monitor
	 *            the progress monitor to use
	 * @throws CoreException
	 *             if an error occurs while deconfiguring the classpath
	 */
	private void deconfigureClasspath(final IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(RefactoringMessages.JarImportWizard_cleanup_import, 300);
			if (fJavaProject != null) {
				final IIncludePathEntry[] entries= fJavaProject.readRawIncludepath();
				final boolean changed= deconfigureClasspath(entries, new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				final RefactoringHistory history= getRefactoringHistory();
				final boolean valid= history != null && !history.isEmpty();
				if (valid)
					RefactoringCore.getUndoManager().flush();
				if (valid || changed)
					fJavaProject.setRawIncludepath(entries, changed, new SubProgressMonitor(monitor, 60, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			}
			if (fSourceFolder != null) {
				final IFileStore store= EFS.getStore(fSourceFolder.getRawLocationURI());
				if (store.fetchInfo(EFS.NONE, new SubProgressMonitor(monitor, 10, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)).exists())
					store.delete(EFS.NONE, new SubProgressMonitor(monitor, 10, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				fSourceFolder.delete(true, false, new SubProgressMonitor(monitor, 10, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				fSourceFolder.clearHistory(new SubProgressMonitor(monitor, 10, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				fSourceFolder= null;
			}
			if (fJavaProject != null) {
				try {
					fJavaProject.getResource().refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				} catch (CoreException exception) {
					JavaScriptPlugin.log(exception);
				}
			}
		} finally {
			fJavaProject= null;
			monitor.done();
		}
	}

	/**
	 * Returns the package fragment root to stub.
	 * 
	 * @return the package fragment root to stub, or <code>null</code>
	 */
	protected abstract IPackageFragmentRoot getPackageFragmentRoot();

	/**
	 * Returns the refactoring history to perform.
	 * 
	 * @return the refactoring history to perform, or the empty history
	 */
	protected abstract RefactoringHistory getRefactoringHistory();

	/**
	 * Returns the transformed handle corresponding to the specified input
	 * handle.
	 * 
	 * @param project
	 *            the project, or <code>null</code> for the workspace
	 * @param handle
	 *            the handle to transform
	 * @return the transformed handle, or the original one if nothing needed to
	 *         be transformed
	 */
	private String getTransformedHandle(final String project, final String handle) {
		if (fSourceFolder != null) {
			final IJavaScriptElement target= JavaScriptCore.create(fSourceFolder);
			if (target instanceof IPackageFragmentRoot) {
				final IPackageFragmentRoot extended= (IPackageFragmentRoot) target;
				String sourceIdentifier= null;
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(project, handle, false);
				if (element != null) {
					final IPackageFragmentRoot root= (IPackageFragmentRoot) element.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
					if (root != null)
						sourceIdentifier= root.getHandleIdentifier();
					else {
						final IJavaScriptProject javaProject= element.getJavaScriptProject();
						if (javaProject != null)
							sourceIdentifier= javaProject.getHandleIdentifier();
					}
					if (sourceIdentifier != null) {
						final IJavaScriptElement result= JavaScriptCore.create(extended.getHandleIdentifier() + element.getHandleIdentifier().substring(sourceIdentifier.length()));
						if (result != null)
							return JDTRefactoringDescriptor.elementToHandle(project, result);
					}
				}
			}
		}
		return handle;
	}

	/**
	 * {@inheritDoc}
	 */
	protected RefactoringStatus historyPerformed(final IProgressMonitor monitor) {
		try {
			monitor.beginTask(RefactoringMessages.JarImportWizard_cleanup_import, 100);
			final RefactoringStatus status= super.historyPerformed(new SubProgressMonitor(monitor, 10, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			if (!status.hasFatalError()) {
				try {
					deconfigureClasspath(new SubProgressMonitor(monitor, 90, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				} catch (CoreException exception) {
					status.addError(exception.getLocalizedMessage());
				} finally {
					try {
						CoreUtility.enableAutoBuild(fAutoBuild);
					} catch (CoreException exception) {
						JavaScriptPlugin.log(exception);
					}
				}
			}
			return status;
		} finally {
			monitor.done();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean performCancel() {
		fCancelled= true;
		return super.performCancel();
	}

	/**
	 * {@inheritDoc}
	 */
	protected RefactoringStatus refactoringPerformed(final Refactoring refactoring, final IProgressMonitor monitor) {
		try {
			monitor.beginTask("", 120); //$NON-NLS-1$
			final RefactoringStatus status= super.refactoringPerformed(refactoring, new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			if (!status.hasFatalError()) {
				if (fSourceFolder != null) {
					try {
						fSourceFolder.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
					} catch (CoreException exception) {
						JavaScriptPlugin.log(exception);
					}
				}
			}
			return status;
		} finally {
			monitor.done();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected boolean selectPreviewChange(final Change change) {
		if (fSourceFolder != null) {
			final IPath source= fSourceFolder.getFullPath();
			final Object element= change.getModifiedElement();
			if (element instanceof IAdaptable) {
				final IAdaptable adaptable= (IAdaptable) element;
				final IResource resource= (IResource) adaptable.getAdapter(IResource.class);
				if (resource != null && source.isPrefixOf(resource.getFullPath()))
					return false;
			}
		}
		return super.selectPreviewChange(change);
	}

	/**
	 * {@inheritDoc}
	 */
	protected boolean selectStatusEntry(final RefactoringStatusEntry entry) {
		if (fSourceFolder != null) {
			final IPath source= fSourceFolder.getFullPath();
			final RefactoringStatusContext context= entry.getContext();
			if (context instanceof JavaStatusContext) {
				final JavaStatusContext extended= (JavaStatusContext) context;
				final IJavaScriptUnit unit= extended.getCompilationUnit();
				if (unit != null) {
					final IResource resource= unit.getResource();
					if (resource != null && source.isPrefixOf(resource.getFullPath()))
						return false;
				}
			}
		}
		return super.selectStatusEntry(entry);
	}
}
