/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.packageview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.jsdt.core.ElementChangedEvent;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IElementChangedListener;
import org.eclipse.wst.jsdt.core.IIncludePathAttribute;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptElementDelta;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IParent;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.navigator.ContainerFolder;
import org.eclipse.wst.jsdt.internal.ui.navigator.deferred.ClearPlaceHolderJob;
import org.eclipse.wst.jsdt.internal.ui.navigator.deferred.LoadingModelNode;
import org.eclipse.wst.jsdt.internal.ui.navigator.deferred.LoadingModelUIAnimationJob;
import org.eclipse.wst.jsdt.internal.ui.workingsets.WorkingSetModel;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.ProjectLibraryRoot;
import org.eclipse.wst.jsdt.ui.StandardJavaScriptElementContentProvider;
 
/**
 * Content provider for the PackageExplorer.
 * 
 * <p>
 * Since 2.1 this content provider can provide the children for flat or hierarchical
 * layout.
 * </p>
 * 
 * @see org.eclipse.wst.jsdt.ui.StandardJavaScriptElementContentProvider
 */
public class PackageExplorerContentProvider extends StandardJavaScriptElementContentProvider implements ITreeContentProvider, IElementChangedListener, IPropertyChangeListener {
	
	protected static final int ORIGINAL= 0;
	protected static final int PARENT= 1 << 0;
	protected static final int GRANT_PARENT= 1 << 1;
	protected static final int PROJECT= 1 << 2;
	
	private TreeViewer fViewer;
	private Object fInput;
	private boolean fIsFlatLayout;
	private boolean fShowLibrariesNode;
	private boolean fFoldPackages;
	
	private Collection fPendingUpdates;
		
	private UIJob fUpdateJob;

	/**
	 * Creates a new content provider for Java elements.
	 * @param provideMembers if set, members of compilation units and class files are shown
	 */
	public PackageExplorerContentProvider(boolean provideMembers) {
		super(provideMembers);
		fShowLibrariesNode= false;
		fIsFlatLayout= false;
		fFoldPackages= arePackagesFoldedInHierarchicalLayout();
		fPendingUpdates= null;
		JavaScriptPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);

		fUpdateJob= null;
	}
	
	private boolean arePackagesFoldedInHierarchicalLayout(){
		return PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.APPEARANCE_FOLD_PACKAGES_IN_PACKAGE_EXPLORER);
	}
			
	protected Object getViewerInput() {
		return fInput;
	}
	
	/* (non-Javadoc)
	 * Method declared on IElementChangedListener.
	 */
	public void elementChanged(final ElementChangedEvent event) {
		final ArrayList runnables= new ArrayList();
		try {
			// 58952 delete project does not update Package Explorer [package explorer] 
			// if the input to the viewer is deleted then refresh to avoid the display of stale elements
			if (inputDeleted(runnables))
				return;

			processDelta(event.getDelta(), runnables);
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
		} finally {	
			executeRunnables(runnables);
		}
	}

	protected final void executeRunnables(final Collection runnables) {

		// now post all collected runnables
		final Control ctrl= fViewer.getControl();
		if (ctrl != null && !ctrl.isDisposed()) {
			//Are we in the UIThread? If so spin it until we are done
			if ((ctrl.getDisplay().getThread() == Thread.currentThread()) && !fViewer.isBusy()) {
				runUpdates(runnables);
			} else {
				synchronized (this) {
					if (fPendingUpdates == null) {
						fPendingUpdates= runnables;
					} else {
						fPendingUpdates.addAll(runnables);
					}
				}
				postAsyncUpdate(ctrl.getDisplay());
			}
		}
	}
	
	private void postAsyncUpdate(final Display display) {
		if (fUpdateJob == null) {
			fUpdateJob= new UIJob(display, PackagesMessages.PackageExplorerContentProvider_update_job_description) {
				public IStatus runInUIThread(IProgressMonitor monitor) {
					TreeViewer viewer= fViewer;
					if (viewer != null && viewer.isBusy()) {
						schedule(100); // reschedule when viewer is busy: bug 184991
					} else {
						runPendingUpdates();
					}
					return Status.OK_STATUS;
				}
			};
			fUpdateJob.setSystem(true);
		}
		fUpdateJob.schedule();
	}         
	
	/**
	 * Run all of the runnables that are the widget updates. Must be called in the display thread.
	 */
	public void runPendingUpdates() {
		Collection pendingUpdates;
		synchronized (this) {
			pendingUpdates= fPendingUpdates;
			fPendingUpdates= null;
		}
		if (pendingUpdates != null && fViewer != null) {
			Control control = fViewer.getControl();
			if (control != null && !control.isDisposed()) {
				runUpdates(pendingUpdates);
			}
		}
	}
	
	private void runUpdates(Collection runnables) {
		Iterator runnableIterator = runnables.iterator();
		while (runnableIterator.hasNext()){
			((Runnable) runnableIterator.next()).run();
		}
	}
	

	private boolean inputDeleted(Collection runnables) {
		if (fInput == null)
			return false;
		if ((fInput instanceof IJavaScriptElement) && ((IJavaScriptElement) fInput).exists())
			return false;
		if ((fInput instanceof IResource) && ((IResource) fInput).exists())
			return false;
		if (fInput instanceof WorkingSetModel)
			return false;
		if (fInput instanceof IWorkingSet) // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=156239
			return false;
		postRefresh(fInput, ORIGINAL, fInput, runnables);
		return true;
	}

	/* (non-Javadoc)
	 * Method declared on IContentProvider.
	 */
	public void dispose() {
		super.dispose();
		JavaScriptCore.removeElementChangedListener(this);
		JavaScriptPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.StandardJavaScriptElementContentProvider#getPackageFragmentRootContent(org.eclipse.wst.jsdt.core.IPackageFragmentRoot)
	 */
	protected Object[] getPackageFragmentRootContent(IPackageFragmentRoot root) throws JavaScriptModelException {
		if (fIsFlatLayout) {
			return super.getPackageFragmentRootContent(root);
		}
		
		// hierarchical package mode
		ArrayList result= new ArrayList();
		getHierarchicalPackageChildren(root, null, result);
		if (!isProjectPackageFragmentRoot(root)) {
			Object[] nonJavaResources= root.getNonJavaScriptResources();
			for (int i= 0; i < nonJavaResources.length; i++) {
				result.add(nonJavaResources[i]);
			}
		}
		return result.toArray();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.StandardJavaScriptElementContentProvider#getPackageContent(org.eclipse.wst.jsdt.core.IPackageFragment)
	 */
	protected Object[] getPackageContent(IPackageFragment fragment) throws JavaScriptModelException {
		if (fIsFlatLayout) {
			return super.getPackageContent(fragment);
		}
		
		// hierarchical package mode
		ArrayList result= new ArrayList();
		
		getHierarchicalPackageChildren((IPackageFragmentRoot) fragment.getParent(), fragment, result);
		Object[] nonPackages= super.getPackageContent(fragment);
		if (result.isEmpty())
			return nonPackages;
		for (int i= 0; i < nonPackages.length; i++) {
			result.add(nonPackages[i]);
		}
		return result.toArray();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.StandardJavaScriptElementContentProvider#getFolderContent(org.eclipse.core.resources.IFolder)
	 */
	protected Object[] getFolderContent(IFolder folder) throws CoreException {
		if (fIsFlatLayout) {
			return super.getFolderContent(folder);
		}
		
		// hierarchical package mode
		ArrayList result= new ArrayList();
		
		getHierarchicalPackagesInFolder(folder, result);
		Object[] others= super.getFolderContent(folder);
		if (result.isEmpty())
			return others;
		for (int i= 0; i < others.length; i++) {
			result.add(others[i]);
		}
		return result.toArray();
	}
	
	
	public Object[] getChildren(Object parentElement) {
		try {
			if (parentElement instanceof IJavaScriptModel) 
				return concatenate(getJavaProjects((IJavaScriptModel)parentElement), getNonJavaProjects((IJavaScriptModel)parentElement));

			if(parentElement instanceof ContainerFolder) {
				return getContainerPackageFragmentRoots((PackageFragmentRootContainer)((ContainerFolder)parentElement).getParentObject());
			}
			if (parentElement instanceof PackageFragmentRootContainer) {
				if(LoadingModelNode.isBeingLoaded((PackageFragmentRootContainer)parentElement)) {
					return new Object[] { 
							LoadingModelNode.createPlaceHolder((PackageFragmentRootContainer)parentElement) }; 
				} else { 
					LoadingModelNode placeHolder = 
						LoadingModelNode.createPlaceHolder((PackageFragmentRootContainer)parentElement);
					/* we need to load the model, 
						possible long running operation */					
					if(LoadingModelNode.canBeginLoading((PackageFragmentRootContainer)parentElement))
						new LoadModelJob((AbstractTreeViewer)fViewer, 
								 placeHolder, 
								 (PackageFragmentRootContainer)parentElement)
							.schedule();
						return new Object[] { placeHolder }; 
				}
			}
			else if (parentElement instanceof NamespaceGroup && ((NamespaceGroup) parentElement).getPackageFragmentRootContainer() != null) {
				return getContainerPackageFragmentRoots(((NamespaceGroup) parentElement).getPackageFragmentRootContainer(), true, ((NamespaceGroup) parentElement));
			}
			
			if(parentElement instanceof ProjectLibraryRoot) {
//				return ((ProjectLibraryRoot)parentElement).getChildren();
				// Include source folders (and also scour their model contents)
				Object[] children1 = ((ProjectLibraryRoot) parentElement).getChildren();
				List sourceRoots = new ArrayList();
				try {
					IPackageFragmentRoot[] packageFragmentRoots = ((ProjectLibraryRoot) parentElement).getProject().getPackageFragmentRoots();
					for (int i = 0; i < packageFragmentRoots.length; i++) {
						IIncludePathEntry entry = packageFragmentRoots[i].getRawIncludepathEntry();
						if (IIncludePathEntry.CPE_SOURCE == entry.getEntryKind()) {
							boolean hidden = false;
							IIncludePathAttribute[] attribs = entry.getExtraAttributes();
							for (int k = 0; !hidden && attribs != null && k < attribs.length; k++) {
								hidden |= (attribs[k] == IIncludePathAttribute.HIDE);
							}
							if (!hidden) {
								sourceRoots.add(packageFragmentRoots[i]);
							}
						}
					}
				}
				catch (JavaScriptModelException e) {
					e.printStackTrace();
				}
				Object[] combined = new Object[children1.length + sourceRoots.size()];
				System.arraycopy(children1, 0, combined, 0, children1.length);
				if (!sourceRoots.isEmpty()) {
					System.arraycopy(sourceRoots.toArray(), 0, combined, children1.length, sourceRoots.size());
				}
				return combined;
			}
//			if (parentElement instanceof IPackageFragmentRoot) {
//				Object[] children = super.getChildren(parentElement);
//				for (int i = 0; i < children.length; i++) {
//					// replace the "default package" with its contents
//					if (children[i] instanceof IPackageFragment && ((IPackageFragment) children[i]).isDefaultPackage()) {
//						List combined = new ArrayList(children.length);
//						for (int j = 0; j < children.length; j++) {
//							if (j != i) {
//								combined.add(children[j]);
//							}
//							else {
//								Object[] defaultChildren = super.getChildren(children[j]);
//								for (int k = 0; k < defaultChildren.length; k++) {
//									combined.add(defaultChildren[k]);
//								}
//							}
//						}
//						return combined.toArray();
//					}
//				}
//				return children;
//			}
			if (parentElement instanceof IProject) {
				IProject project= (IProject) parentElement;
				if (project.isAccessible())
					return project.members();
				return NO_CHILDREN;
			}			
			if (parentElement instanceof IPackageFragmentRoot && ((IPackageFragmentRoot)parentElement).isVirtual()) {
				return getLibraryChildren((IPackageFragmentRoot)parentElement, fIsFlatLayout, null);
			}
			else if (parentElement instanceof NamespaceGroup && ((NamespaceGroup) parentElement).getPackageFragmentRoot() != null && ((NamespaceGroup) parentElement).getPackageFragmentRoot().isVirtual()) {
				return getLibraryChildren(((NamespaceGroup) parentElement).getPackageFragmentRoot(), true, ((NamespaceGroup) parentElement));
			}
			else if(parentElement instanceof IPackageFragmentRoot && IIncludePathEntry.CPE_SOURCE == ((IPackageFragmentRoot) parentElement).getRawIncludepathEntry().getEntryKind()) {
				return getSourceChildren(parentElement, fIsFlatLayout, null);
			}
			else if (parentElement instanceof NamespaceGroup && ((NamespaceGroup) parentElement).getPackageFragmentRoot() != null && IIncludePathEntry.CPE_SOURCE == ((NamespaceGroup) parentElement).getPackageFragmentRoot().getRawIncludepathEntry().getEntryKind()) {
				return getSourceChildren(((NamespaceGroup) parentElement).getPackageFragmentRoot(), true, ((NamespaceGroup) parentElement));
			}
			// if script unit
			else if (parentElement instanceof IJavaScriptUnit) {
				return getSourceChildren(parentElement, fIsFlatLayout, null);
			}
			// if group with script unit as parent
			else if(parentElement instanceof NamespaceGroup && ((NamespaceGroup) parentElement).getJavaScriptUnit() != null) {
				return getSourceChildren(((NamespaceGroup) parentElement).getJavaScriptUnit(), true, ((NamespaceGroup) parentElement));
			}
			return super.getChildren(parentElement);
		} catch (CoreException e) {
			e.printStackTrace();
			return NO_CHILDREN;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.StandardJavaScriptElementContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		if (element instanceof JsGlobalScopeContainer) {
			return true;//((JsGlobalScopeContainer) element).hasChildren();
		}
		if (element instanceof ProjectLibraryRoot) {
			return ((ProjectLibraryRoot) element).hasChildren();
		}
		return super.hasChildren(element);
	}

	private Object[] getSourceChildren(Object parentElement, boolean neverGroup, NamespaceGroup onlyGroup) throws JavaScriptModelException {
		/* if the parent is a fragment root use its children
		 * else if parent element is a script unit, use it as the child
		 */
		Object[] rawChildren = new Object[0];
		if(parentElement instanceof IPackageFragmentRoot) {
			rawChildren = ((IParent) parentElement).getChildren();
		} else if(parentElement instanceof IJavaScriptUnit) {
			rawChildren = new Object[] { parentElement };
		}
		
		if (rawChildren == null)
			return new Object[0];

		ArrayList allChildren = new ArrayList();
		ArrayList expanded = new ArrayList();
		expanded.addAll(Arrays.asList(rawChildren));

		if (expanded.isEmpty())
			return new Object[0];

		Object next = expanded.remove(0);
		Map groups = new HashMap();

		while (next != null) {
			if (next instanceof IPackageFragment) {
				expanded.addAll(Arrays.asList(((IPackageFragment) next).getChildren()));
			}
			else if (next instanceof IPackageFragmentRoot) {
				expanded.addAll(Arrays.asList(((IPackageFragmentRoot) next).getChildren()));
			}
			else if (next instanceof IClassFile || next instanceof IJavaScriptUnit) {
				IJavaScriptElement[] filtered = filter(((IParent) next).getChildren());
				List newChildren = Arrays.asList(filtered);
				allChildren.removeAll(newChildren);
				if (fIsFlatLayout || neverGroup) {
					if (onlyGroup == null) {
						allChildren.addAll(newChildren);
					}
					else {
						for (int j = 0; j < filtered.length; j++) {
							switch(filtered[j].getElementType()) {
								case IJavaScriptElement.TYPE :
								case IJavaScriptElement.FIELD :
								case IJavaScriptElement.METHOD :
								case IJavaScriptElement.INITIALIZER :
								case IJavaScriptElement.LOCAL_VARIABLE : {
									String displayName = filtered[j].getDisplayName();
									int groupNamesEnd = displayName.lastIndexOf('.');
									if (groupNamesEnd == onlyGroup.fNamePrefixLength && displayName.startsWith(onlyGroup.fNamePrefix)) {
										allChildren.add(filtered[j]);
									}
									break;
								}
								default : {
									allChildren.add(filtered[j]);
								}
							}
						}
					}
				}
				else {
					for (int j = 0; j < filtered.length; j++) {
						switch(filtered[j].getElementType()) {
							case IJavaScriptElement.TYPE :
							case IJavaScriptElement.FIELD :
							case IJavaScriptElement.METHOD :
							case IJavaScriptElement.INITIALIZER :
							case IJavaScriptElement.LOCAL_VARIABLE : {
								String displayName = filtered[j].getDisplayName();
								int groupEnd = displayName.lastIndexOf('.');
								if (groupEnd > 0) {
									String groupName = displayName.substring(0, groupEnd);
									if (!groups.containsKey(groupName)) {
										// create the group based on the parent type
										NamespaceGroup group = null;
										if(parentElement instanceof IPackageFragmentRoot) {
											group = new NamespaceGroup((IPackageFragmentRoot) parentElement, groupName);
										} else if(parentElement instanceof IJavaScriptUnit) {
											group = new NamespaceGroup((IJavaScriptUnit) parentElement, groupName);
										}
										
										if(group != null) {
											groups.put(groupName, group);
											allChildren.add(group);
										}
									}
								}
								else {
									allChildren.add(filtered[j]);
								}

								break;
							}
							default : {
								allChildren.add(filtered[j]);
							}
						}
					}
				}
			}

			if (expanded.size() > 0)
				next = expanded.remove(0);
			else
				next = null;

		}

		return allChildren.toArray();
	}
	
	
	private Object[] getLibraryChildren(IPackageFragmentRoot container, boolean neverGroup, NamespaceGroup onlyGroup) {		
		Object[] children=null;
		try {
			children = container.getChildren();
		} catch (JavaScriptModelException ex1) {
			ex1.printStackTrace();
		}
		if(children==null) return null;
		ArrayList allChildren = new ArrayList();
		
		Map groups = null;
		if (!fIsFlatLayout) {
			groups = new HashMap();
		}
	
		boolean unique = false;
		try {
			while(!unique && children!=null && children.length>0) {
				for(int i = 0;i<children.length;i++) {
					String display1 = ((IJavaScriptElement)children[0]).getDisplayName();
					String display2 = ((IJavaScriptElement)children[i]).getDisplayName();
					if(!(   (display1==display2) || (display1!=null && display1.compareTo(display2)==0))){
						allChildren.addAll(Arrays.asList(children));
						unique=true;
						break;
					}
				}
				List more = new ArrayList();
				for(int i = 0;!unique && i<children.length;i++) {
					if(children[i] instanceof IPackageFragment) {
						more.addAll(Arrays.asList(((IPackageFragment)children[i]).getChildren()));
					}else if(children[i] instanceof IPackageFragmentRoot) {
						more.addAll(Arrays.asList(((IPackageFragmentRoot)children[i]).getChildren()));
					}else if(children[i] instanceof IClassFile || children[i] instanceof IJavaScriptUnit) {
						IJavaScriptElement[] filtered = filter(((IParent) children[i]).getChildren());
						List newChildren = Arrays.asList(filtered);
						allChildren.removeAll(newChildren);
						if (fIsFlatLayout || neverGroup) {
							if (onlyGroup == null) {
								allChildren.addAll(newChildren);
							}
							else {
								for (int j = 0; j < filtered.length; j++) {
									switch(filtered[j].getElementType()) {
										case IJavaScriptElement.TYPE :
										case IJavaScriptElement.FIELD :
										case IJavaScriptElement.METHOD :
										case IJavaScriptElement.INITIALIZER :
										case IJavaScriptElement.LOCAL_VARIABLE : {
											String displayName = filtered[j].getDisplayName();
											int groupNamesEnd = displayName.lastIndexOf('.');
											if (groupNamesEnd == onlyGroup.fNamePrefixLength && displayName.startsWith(onlyGroup.fNamePrefix)) {
												allChildren.add(filtered[j]);
											}
											break;
										}
										default : {
											allChildren.add(filtered[j]);
										}
									}
								}
							}
						}
						else {
							for (int j = 0; j < filtered.length; j++) {
								switch(filtered[j].getElementType()) {
									case IJavaScriptElement.TYPE :
									case IJavaScriptElement.FIELD :
									case IJavaScriptElement.METHOD :
									case IJavaScriptElement.INITIALIZER :
									case IJavaScriptElement.LOCAL_VARIABLE : {
										String displayName = filtered[j].getDisplayName();
										int groupEnd = displayName.lastIndexOf('.');
										if (groupEnd > 0) {
											String groupName = displayName.substring(0, groupEnd);
											if (!groups.containsKey(groupName)) {
												NamespaceGroup group = new NamespaceGroup(container, groupName);
												groups.put(groupName, group);
												allChildren.add(group);
											}
										}
										else {
											allChildren.add(filtered[j]);
										}

										break;
									}
									default : {
										allChildren.add(filtered[j]);
									}
								}
							}
						}
					}
					else {
						/* bottomed out, now at javaScriptElement level */
						unique=true;
						break;
					}
					
				}
				if(!unique) children = more.toArray();
			}
		} catch (JavaScriptModelException ex) {
			ex.printStackTrace();
		}
		
		return allChildren.toArray();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.StandardJavaScriptElementContentProvider#getPackageFragmentRoots(org.eclipse.wst.jsdt.core.IJavaScriptProject)
	 */
	protected Object[] getPackageFragmentRoots(IJavaScriptProject project) throws JavaScriptModelException {
		if (!project.getProject().isOpen())
			return NO_CHILDREN;
			
		List result= new ArrayList();

		boolean addJARContainer= false;
		ArrayList projectPackageFragmentRoots  = new ArrayList();
		IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			IPackageFragmentRoot root= roots[i];
			IIncludePathEntry classpathEntry= root.getRawIncludepathEntry();
			
			IIncludePathAttribute[] attribs = classpathEntry.getExtraAttributes();
			boolean shouldHide = false;
			for(int p = 0;p<attribs.length;p++){
				if(attribs[p]==IIncludePathAttribute.HIDE) shouldHide = true;
			}
			
			int entryKind= classpathEntry.getEntryKind();
			if (entryKind == IIncludePathEntry.CPE_CONTAINER) {
				// all JsGlobalScopeContainers are added later 
			} else if (fShowLibrariesNode && (entryKind != IIncludePathEntry.CPE_SOURCE) && entryKind!=IIncludePathEntry.CPE_CONTAINER) {
				addJARContainer= true;
				projectPackageFragmentRoots.add(root);
			} else {
				if (isProjectPackageFragmentRoot(root)) {
					// filter out package fragments that correspond to projects and
					// replace them with the package fragments directly
//					Object[] fragments= getPackageFragmentRootContent(root);
//					for (int j= 0; j < fragments.length; j++) {
//						result.add(fragments[j]);
//					}
				} /*else if(!shouldHide){
					result.add(root);
				}*/
			}
		}
		
		if (addJARContainer) {
			projectPackageFragmentRoots.add(new LibraryContainer(project));
		}
		
		// separate loop to make sure all containers are on the classpath
		IIncludePathEntry[] rawClasspath= project.getRawIncludepath();
		for (int i= 0; i < rawClasspath.length; i++) {
			IIncludePathEntry classpathEntry= rawClasspath[i];
			if (classpathEntry.getEntryKind() == IIncludePathEntry.CPE_CONTAINER) {
				projectPackageFragmentRoots.add(new JsGlobalScopeContainer(project, classpathEntry));
			}	
		}	
//		Object[] resources= project.getNonJavaScriptResources();
//		for (int i= 0; i < resources.length; i++) {
//			result.add(resources[i]);
//		}
		ProjectLibraryRoot projectLibs = new ProjectLibraryRoot(project);
		result.add(0,projectLibs);
		return result.toArray();
	}
	
	public Object getParent(Object element) {
		if (element instanceof NamespaceGroup) {
			return ((NamespaceGroup)element).getParent();
		}
		if (element instanceof IPackageFragmentRoot) {
			IJavaScriptProject project = (IJavaScriptProject) ((IPackageFragmentRoot) element).getAncestor(IJavaScriptElement.JAVASCRIPT_PROJECT);
			if (project != null) {
				return new ProjectLibraryRoot(project);
			}
		}
		return super.getParent(element);
	}

//	private Object[] getContainerPackageFragmentRoots3(PackageFragmentRootContainer container) {
//		Object[] children = container.getChildren();
//		if(children==null) return null;
//		ArrayList allChildren = new ArrayList();
//		for(int i=0;i<children.length;i++) {
//			try {
//				allChildren.addAll(Arrays.asList(((IPackageFragmentRoot)children[i]).getChildren()));
//			} catch (JavaScriptModelException ex) {
//				
//			}
//		}
//		return allChildren.toArray();
//	}
//	
	private Object[] getContainerPackageFragmentRoots(PackageFragmentRootContainer container) {
		return getContainerPackageFragmentRoots(container, false, null);
	}
	private Object[] getContainerPackageFragmentRoots(PackageFragmentRootContainer container, boolean neverGroup, NamespaceGroup onlyGroup) {
		
			Object[] children = container.getChildren();
			if (children == null)
				return new Object[0];
			for (int i = 0; i < children.length; i++) {
				/* if one of the children is not a JS model element, or is an archive, return as-is for further navigation */
				if (!(children[i] instanceof IJavaScriptElement) || (((IJavaScriptElement) children[i]).getElementType() == IJavaScriptElement.PACKAGE_FRAGMENT_ROOT && ((IPackageFragmentRoot) children[i]).isArchive()))
					return children;
			}
			
			ArrayList allChildren = new ArrayList();
			ArrayList expanded = new ArrayList();
			expanded.addAll(Arrays.asList(children));
			
			
			if(expanded.isEmpty()) return new Object[0];
			
			Map groups = null;
			if (!fIsFlatLayout) {
				groups = new HashMap();
			}
		
			Object next = expanded.remove(0);

			while(next!=null) {
				try {
					if(next instanceof IPackageFragment) {
						expanded.addAll(Arrays.asList(((IPackageFragment)next).getChildren()));
					}else if(next instanceof IPackageFragmentRoot) {
						expanded.addAll(Arrays.asList(((IPackageFragmentRoot)next).getChildren()));
					}else if(next instanceof IClassFile || next instanceof IJavaScriptUnit) {
						IJavaScriptElement[] filtered = filter(((IParent) next).getChildren());
						List newChildren = Arrays.asList(filtered);
						allChildren.removeAll(newChildren);
						if (fIsFlatLayout || neverGroup) {
							if (onlyGroup == null) {
								allChildren.addAll(newChildren);
							}
							else {
								for (int j = 0; j < filtered.length; j++) {
									switch(filtered[j].getElementType()) {
										case IJavaScriptElement.TYPE :
										case IJavaScriptElement.FIELD :
										case IJavaScriptElement.METHOD :
										case IJavaScriptElement.INITIALIZER :
										case IJavaScriptElement.LOCAL_VARIABLE : {
											String displayName = filtered[j].getDisplayName();
											int groupNamesEnd = displayName.lastIndexOf('.');
											if (groupNamesEnd == onlyGroup.fNamePrefixLength && displayName.startsWith(onlyGroup.fNamePrefix)) {
												allChildren.add(filtered[j]);
											}
											break;
										}
										default : {
											allChildren.add(filtered[j]);
										}
									}
								}
							}
						}
						else {
							for (int j = 0; j < filtered.length; j++) {
								switch(filtered[j].getElementType()) {
									case IJavaScriptElement.TYPE :
									case IJavaScriptElement.FIELD :
									case IJavaScriptElement.METHOD :
									case IJavaScriptElement.INITIALIZER :
									case IJavaScriptElement.LOCAL_VARIABLE : {
										String displayName = filtered[j].getDisplayName();
										int groupEnd = displayName.lastIndexOf('.');
										if (groupEnd > 0) {
											String groupName = displayName.substring(0, groupEnd);
											if (!groups.containsKey(groupName)) {
												NamespaceGroup group = new NamespaceGroup(container, groupName);
												groups.put(groupName, group);
												allChildren.add(group);
											}
										}
										else {
											allChildren.add(filtered[j]);
										}

										break;
									}
									default : {
										allChildren.add(filtered[j]);
									}
								}
							}
						}
					}
					else {
						allChildren.add(next);
					}
				} catch (JavaScriptModelException ex) {
					ex.printStackTrace();
				}
				
				if(expanded.size()>0) 
					next = expanded.remove(0);
				else
					next = null;
				
			}
			
			return allChildren.toArray();
		
		
		
	}
	private Object[] getContainerPackageFragmentRootsDeprc(PackageFragmentRootContainer container, boolean createFolder) {
		
		
		if(container!=null) {	
			
			Object[] children = container.getChildren();
			if(children==null) return null;
			ArrayList allChildren = new ArrayList();
			
			boolean unique = false;
			
				while(!unique && children!=null && children.length>0) {
					String display1=null;
					for(int i = 0;i<children.length;i++) {
						display1 = ((IJavaScriptElement)children[0]).getDisplayName();
						String display2 = ((IJavaScriptElement)children[i]).getDisplayName();
						if(!(   (display1==display2) || (display1!=null && display1.compareTo(display2)==0))){
							allChildren.addAll(Arrays.asList(children));
							unique=true;
							break;
						}
					}
					if(!unique && createFolder) {
						ContainerFolder folder = new ContainerFolder(display1, container);
						return new Object[] {folder};
					}
					ArrayList more = new ArrayList();
					for(int i = 0;!unique && i<children.length;i++) {
					
						try {
							if(children[i] instanceof IPackageFragment) {
								more.addAll(Arrays.asList(((IPackageFragment)children[i]).getChildren()));
							}else if(children[i] instanceof IPackageFragmentRoot) {
								more.addAll(Arrays.asList(((IPackageFragmentRoot)children[i]).getChildren()));
							}else if(children[i] instanceof IClassFile) {
								more.addAll(Arrays.asList( filter(((IClassFile)children[i]).getChildren())) );
							}else if(children[i] instanceof IJavaScriptUnit) {
								more.addAll(Arrays.asList( filter(((IJavaScriptUnit)children[i]).getChildren())) );
							}else {
								/* bottomed out, now at javaElement level */
								unique=true;
								break;
							}
						} catch (JavaScriptModelException ex) {
							// TODO Auto-generated catch block
							ex.printStackTrace();
						}
						
					}
					if(!unique) children = more.toArray();
				}
		
			
			
			return allChildren.toArray();
		}else {
			return new Object[0];
		}
	}

	private Object[] getNonJavaProjects(IJavaScriptModel model) throws JavaScriptModelException {
		return model.getNonJavaScriptResources();
	}

	protected Object internalGetParent(Object element) {
		if (!fIsFlatLayout && element instanceof IPackageFragment) {
			return getHierarchicalPackageParent((IPackageFragment) element);
		} else if (element instanceof IPackageFragmentRoot) {
			// since we insert logical package containers we have to fix
			// up the parent for package fragment roots so that they refer
			// to the container and containers refer to the project
			IPackageFragmentRoot root= (IPackageFragmentRoot)element;
			
			try {
				IIncludePathEntry entry= root.getRawIncludepathEntry();
				int entryKind= entry.getEntryKind();
				if (entryKind == IIncludePathEntry.CPE_CONTAINER) {
					return new JsGlobalScopeContainer(root.getJavaScriptProject(), entry);
				} else if (fShowLibrariesNode && (entryKind == IIncludePathEntry.CPE_LIBRARY || entryKind == IIncludePathEntry.CPE_VARIABLE)) {
					return new LibraryContainer(root.getJavaScriptProject());
				}
			} catch (JavaScriptModelException e) {
				// fall through
			}
		} else if (element instanceof PackageFragmentRootContainer) {
			return ((PackageFragmentRootContainer)element).getJavaProject();
		}
		return super.internalGetParent(element);
	}

	/* (non-Javadoc)
	 * Method declared on IContentProvider.
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		super.inputChanged(viewer, oldInput, newInput);
		fViewer= (TreeViewer)viewer;
		if (oldInput == null && newInput != null) {
			JavaScriptCore.addElementChangedListener(this); 
		} else if (oldInput != null && newInput == null) {
			JavaScriptCore.removeElementChangedListener(this); 
		}
		fInput= newInput;
	}

	// hierarchical packages
	/**
	 * Returns the hierarchical packages inside a given fragment or root.
	 * @param parent The parent package fragment root
	 * @param fragment The package to get the children for or 'null' to get the children of the root.
	 * @param result Collection where the resulting elements are added
	 * @throws JavaScriptModelException
	 */
	private void getHierarchicalPackageChildren(IPackageFragmentRoot parent, IPackageFragment fragment, Collection result) throws JavaScriptModelException {
		IJavaScriptElement[] children= parent.getChildren();
		String prefix= fragment != null ? fragment.getElementName() + '/' : ""; //$NON-NLS-1$
		if (prefix.length()==1)
			prefix=""; //$NON-NLS-1$
		int prefixLen= prefix.length();
		for (int i= 0; i < children.length; i++) {
			IPackageFragment curr= (IPackageFragment) children[i];
			if (fragment==null)
			{
				if (curr.isDefaultPackage()) 
					result.add(curr);
			}
			else
			{
				String name= curr.getElementName();
				if (name.startsWith(prefix) && name.length() > prefixLen && name.indexOf('/', prefixLen) == -1) {
					if (fFoldPackages) {
						curr= getFolded(children, curr);
					}
					result.add(curr);
				}
			}

		}
	}
	
	/**
	 * Returns the hierarchical packages inside a given folder.
	 * @param folder The parent folder
	 * @param result Collection where the resulting elements are added
	 * @throws CoreException thrown when elements could not be accessed
	 */
	private void getHierarchicalPackagesInFolder(IFolder folder, Collection result) throws CoreException {
		IResource[] resources= folder.members();
		for (int i= 0; i < resources.length; i++) {
			IResource resource= resources[i];
			if (resource instanceof IFolder) {
				IFolder curr= (IFolder) resource;
				IJavaScriptElement element= JavaScriptCore.create(curr);
				if (element instanceof IPackageFragment) {
					if (element.exists()) {
					if (fFoldPackages) {
						IPackageFragment fragment= (IPackageFragment) element;
						IPackageFragmentRoot root= (IPackageFragmentRoot) fragment.getParent();
						element= getFolded(root.getChildren(), fragment);
					}
					result.add(element);	
				} 
			}	
		}
	}
	}

	public Object getHierarchicalPackageParent(IPackageFragment child) {
		String name= child.getElementName();
		IPackageFragmentRoot parent= (IPackageFragmentRoot) child.getParent();
		int index= name.lastIndexOf('/');
		if (index != -1) {
			String realParentName= name.substring(0, index);
			IPackageFragment element= parent.getPackageFragment(realParentName);
			if (element.exists()) {
				try {
					if (fFoldPackages && isEmpty(element) && findSinglePackageChild(element, parent.getChildren()) != null) {
						return getHierarchicalPackageParent(element);
					}
				} catch (JavaScriptModelException e) {
					// ignore
				}
				return element;
			} else { // bug 65240
				IResource resource= element.getResource();
				if (resource != null) {
					return resource;
				}
			}
		}
		if (parent.getResource() instanceof IProject) {
			return parent.getJavaScriptProject();
		}
		return parent;
	}
	
	private static IPackageFragment getFolded(IJavaScriptElement[] children, IPackageFragment pack) throws JavaScriptModelException {
		while (isEmpty(pack)) {
			IPackageFragment collapsed= findSinglePackageChild(pack, children);
			if (collapsed == null) {
				return pack;
			}
			pack= collapsed;
		}
		return pack;
	}
		
	private static boolean isEmpty(IPackageFragment fragment) throws JavaScriptModelException {
		return !fragment.containsJavaResources() && fragment.getNonJavaScriptResources().length == 0;
	}
	
	private static IPackageFragment findSinglePackageChild(IPackageFragment fragment, IJavaScriptElement[] children) {
		String prefix= fragment.getElementName() + '/';
		int prefixLen= prefix.length();
		IPackageFragment found= null;
		for (int i= 0; i < children.length; i++) {
			IJavaScriptElement element= children[i];
			String name= element.getElementName();
			if (name.startsWith(prefix) && name.length() > prefixLen && name.indexOf('/', prefixLen) == -1) {
				if (found == null) {
					found= (IPackageFragment) element;
				} else {
					return null;
				}
			}
		}
		return found;
	}
	
	// ------ delta processing ------

	/**
	 * Processes a delta recursively. When more than two children are affected the
	 * tree is fully refreshed starting at this node.
	 * 
	 * @param delta the delta to process
	 * @param runnables the resulting view changes as runnables (type {@link Runnable})
	 * @return true is returned if the conclusion is to refresh a parent of an element. In that case no siblings need
	 * to be processed
	 * @throws JavaScriptModelException thrown when the access to an element failed
	 */
	private boolean processDelta(IJavaScriptElementDelta delta, Collection runnables) throws JavaScriptModelException {
	
		int kind= delta.getKind();
		int flags= delta.getFlags();
		IJavaScriptElement element= delta.getElement();
		int elementType= element.getElementType();
		
		
		if (elementType != IJavaScriptElement.JAVASCRIPT_MODEL && elementType != IJavaScriptElement.JAVASCRIPT_PROJECT) {
			IJavaScriptProject proj= element.getJavaScriptProject();
			if (proj == null || !proj.getProject().isOpen()) // TODO: Not needed if parent already did the 'open' check!
				return false;	
		}
		
		if (!fIsFlatLayout && elementType == IJavaScriptElement.PACKAGE_FRAGMENT) {
			if (kind == IJavaScriptElementDelta.REMOVED) {
				final Object parent = getHierarchicalPackageParent((IPackageFragment) element);
				if (parent instanceof IPackageFragmentRoot) {
					postRemove(element,  runnables);
					return false;
				} else {
					postRefresh(internalGetParent(parent), GRANT_PARENT, element, runnables);
					return true;
				}
			} else if (kind == IJavaScriptElementDelta.ADDED) {
				final Object parent = getHierarchicalPackageParent((IPackageFragment) element);
				if (parent instanceof IPackageFragmentRoot) {
					postAdd(parent, element,  runnables);
					return false;
				} else {
					postRefresh(internalGetParent(parent), GRANT_PARENT, element, runnables);
					return true;
				}
			}
			handleAffectedChildren(delta, element, runnables);
			return false;
		}
		
		if (elementType == IJavaScriptElement.JAVASCRIPT_UNIT) {
			IJavaScriptUnit cu= (IJavaScriptUnit) element;
			if (!JavaModelUtil.isPrimary(cu)) {
				return false;
			}
						
			if (!getProvideMembers() && cu.isWorkingCopy() && kind == IJavaScriptElementDelta.CHANGED) {
				return false;
			}
			
			if ((kind == IJavaScriptElementDelta.CHANGED) && !isStructuralCUChange(flags)) {
				return false; // test moved ahead
			}
			
			if (!isOnClassPath(cu)) { // TODO: isOnClassPath expensive! Should be put after all cheap tests
				return false;
			}
			
		}
		
		if (elementType == IJavaScriptElement.JAVASCRIPT_PROJECT) {
			// handle open and closing of a project
			if ((flags & (IJavaScriptElementDelta.F_CLOSED | IJavaScriptElementDelta.F_OPENED)) != 0) {			
				postRefresh(element, ORIGINAL, element, runnables);
				return false;
			}
			// if the raw class path has changed we refresh the entire project
			if ((flags & IJavaScriptElementDelta.F_INCLUDEPATH_CHANGED) != 0) {
				postRefresh(element, ORIGINAL, element, runnables);
				if (element.getElementType() == IJavaScriptElement.JAVASCRIPT_PROJECT) {
					postRefresh(((IJavaScriptProject)element).getProject(), ORIGINAL, ((IJavaScriptProject)element).getProject(), runnables);
				}
				return false;				
			}
			// refresh just like on removal
			if (kind == IJavaScriptElementDelta.ADDED) {
				postRefresh(element, PARENT, element, runnables);
				return true;
			}
		}
	
		if (kind == IJavaScriptElementDelta.REMOVED) {
			Object parent= internalGetParent(element);			
			if (element instanceof IPackageFragment) {
				// refresh package fragment root to allow filtering empty (parent) packages: bug 72923
				if (fViewer.testFindItem(parent) != null)
					postRefresh(parent, PARENT, element, runnables);
				return true;
			}
			
			postRemove(element, runnables);
			if (parent instanceof IPackageFragment) 
				postUpdateIcon((IPackageFragment)parent, runnables);
			// we are filtering out empty subpackages, so we
			// a package becomes empty we remove it from the viewer. 
			if (isPackageFragmentEmpty(element.getParent())) {
				if (fViewer.testFindItem(parent) != null)
					postRefresh(internalGetParent(parent), GRANT_PARENT, element, runnables);
				return true;
			}  
			return false;
		}
	
		if (kind == IJavaScriptElementDelta.ADDED) { 
			Object parent= internalGetParent(element);
			// we are filtering out empty subpackages, so we
			// have to handle additions to them specially. 
			if (parent instanceof IPackageFragment) {
				Object grandparent= internalGetParent(parent);
				// 1GE8SI6: ITPJUI:WIN98 - Rename is not shown in Packages View
				// avoid posting a refresh to an invisible parent
				if (parent.equals(fInput)) {
					postRefresh(parent, PARENT, element, runnables);
				} else {
					// refresh from grandparent if parent isn't visible yet
					if (fViewer.testFindItem(parent) == null)
						postRefresh(grandparent, GRANT_PARENT, element, runnables);
					else {
						postRefresh(parent, PARENT, element, runnables);
					}	
				}
				return true;		
			} else {  
				postAdd(parent, element, runnables);
			}
		}
	
		if (elementType == IJavaScriptElement.JAVASCRIPT_UNIT) {
			if (kind == IJavaScriptElementDelta.CHANGED) {
				// isStructuralCUChange already performed above
				postRefresh(element, ORIGINAL, element, runnables);
				IResource underlyingResource = ((IJavaScriptUnit)element).getUnderlyingResource();
				if(underlyingResource != null) {
					postRefresh(underlyingResource, ORIGINAL, element, runnables);
				}
				updateSelection(delta, runnables);
			}
			return false;
		}
		// no changes possible in class files
		if (elementType == IJavaScriptElement.CLASS_FILE)
			return false;
		
		
		if (elementType == IJavaScriptElement.PACKAGE_FRAGMENT_ROOT) {
			// the contents of an external JAR has changed
			if ((flags & IJavaScriptElementDelta.F_ARCHIVE_CONTENT_CHANGED) != 0) {
				postRefresh(element, ORIGINAL, element, runnables);
				return false;
			}
			// the source attachment of a JAR has changed
			if ((flags & (IJavaScriptElementDelta.F_SOURCEATTACHED | IJavaScriptElementDelta.F_SOURCEDETACHED)) != 0)
				postUpdateIcon(element, runnables);
			
			if (isClassPathChange(delta)) {
				 // throw the towel and do a full refresh of the affected java project. 
				postRefresh(element.getJavaScriptProject(), PROJECT, element, runnables);
				return true;
			}
		}	
		handleAffectedChildren(delta, element, runnables);
		return false;
	}
	
	private static boolean isStructuralCUChange(int flags) {
		// No refresh on working copy creation (F_PRIMARY_WORKING_COPY)
		return ((flags & IJavaScriptElementDelta.F_CHILDREN) != 0) || ((flags & (IJavaScriptElementDelta.F_CONTENT | IJavaScriptElementDelta.F_FINE_GRAINED)) == IJavaScriptElementDelta.F_CONTENT);
	}
	
	/* package */ void handleAffectedChildren(IJavaScriptElementDelta delta, IJavaScriptElement element, Collection runnables) throws JavaScriptModelException {
		int count= 0;
		
		IResourceDelta[] resourceDeltas= delta.getResourceDeltas();
		if (resourceDeltas != null) {
			for (int i= 0; i < resourceDeltas.length; i++) {
				int kind= resourceDeltas[i].getKind();
				if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED) {
					count++;
				}
			}
		}
		IJavaScriptElementDelta[] affectedChildren= delta.getAffectedChildren();
		for (int i= 0; i < affectedChildren.length; i++) {
			int kind= affectedChildren[i].getKind();
			if (kind == IJavaScriptElementDelta.ADDED || kind == IJavaScriptElementDelta.REMOVED) {
				count++;
			}
		}

		if (count > 1) {
			// more than one child changed, refresh from here downwards
			if (element instanceof IPackageFragment) {
				// a package fragment might become non empty refresh from the parent
				IJavaScriptElement parent= (IJavaScriptElement) internalGetParent(element);
				// 1GE8SI6: ITPJUI:WIN98 - Rename is not shown in Packages View
				// avoid posting a refresh to an invisible parent
				if (element.equals(fInput)) {
					postRefresh(element, ORIGINAL, element, runnables);
				} else {
					postRefresh(parent, PARENT, element, runnables);
				}
			} else if (element instanceof IPackageFragmentRoot) {
				Object toRefresh= internalGetParent(element);
				postRefresh(toRefresh, ORIGINAL, toRefresh, runnables);
			} else {
				postRefresh(element, ORIGINAL, element, runnables);
			}
			return;
		}
		if (resourceDeltas != null) {
			for (int i= 0; i < resourceDeltas.length; i++) {
				if (processResourceDelta(resourceDeltas[i], element, runnables)) {
					return; // early return, element got refreshed
				}
			}
		}
		for (int i= 0; i < affectedChildren.length; i++) {
			if (processDelta(affectedChildren[i], runnables)) {
				return; // early return, element got refreshed
			}
		}
	}
	
	protected void processAffectedChildren(IJavaScriptElementDelta[] affectedChildren, Collection runnables) throws JavaScriptModelException {
		for (int i= 0; i < affectedChildren.length; i++) {
			processDelta(affectedChildren[i], runnables);
		}
	}

	private boolean isOnClassPath(IJavaScriptUnit element) {
		IJavaScriptProject project= element.getJavaScriptProject();
		if (project == null || !project.exists())
			return false;
		return project.isOnIncludepath(element);
	}

	/**
	 * Updates the selection. It finds newly added elements
	 * and selects them.
	 * @param delta the delta to process
	 * @param runnables the resulting view changes as runnables (type {@link Runnable})
	 */
	private void updateSelection(IJavaScriptElementDelta delta, Collection runnables) {
		final IJavaScriptElement addedElement= findAddedElement(delta);
		if (addedElement != null) {
			final StructuredSelection selection= new StructuredSelection(addedElement);
			runnables.add(new Runnable() {
				public void run() {
					// 19431
					// if the item is already visible then select it
					if (fViewer.testFindItem(addedElement) != null)
						fViewer.setSelection(selection);
				}
			});	
		}	
	}

	private IJavaScriptElement findAddedElement(IJavaScriptElementDelta delta) {
		if (delta.getKind() == IJavaScriptElementDelta.ADDED)  
			return delta.getElement();
		
		IJavaScriptElementDelta[] affectedChildren= delta.getAffectedChildren();
		for (int i= 0; i < affectedChildren.length; i++) 
			return findAddedElement(affectedChildren[i]);
			
		return null;
	}

	/**
	 * Updates the package icon
	 * @param element the element to update
	 * @param runnables the resulting view changes as runnables (type {@link Runnable})
	 */
	 private void postUpdateIcon(final IJavaScriptElement element, Collection runnables) {
		 runnables.add(new Runnable() {
			public void run() {
				// 1GF87WR: ITPUI:ALL - SWTEx + NPE closing a workbench window.
				fViewer.update(element, new String[]{IBasicPropertyConstants.P_IMAGE});
			}
		});
	 }

	/**
	 * Process a resource delta.
	 * 
	 * @param delta the delta to process
	 * @param parent the parent
	 * @param runnables the resulting view changes as runnables (type {@link Runnable})
	 * @return true if the parent got refreshed
	 */
	private boolean processResourceDelta(IResourceDelta delta, Object parent, Collection runnables) {
		int status= delta.getKind();
		int flags= delta.getFlags();
		
		IResource resource= delta.getResource();
		// filter out changes affecting the output folder
		if (resource == null)
			return false;	
			
		// this could be optimized by handling all the added children in the parent
		if ((status & IResourceDelta.REMOVED) != 0) {
			if (parent instanceof IPackageFragment) {
				// refresh one level above to deal with empty package filtering properly
				postRefresh(internalGetParent(parent), PARENT, parent, runnables);
				return true;
			} else {
				postRemove(resource, runnables);
				return false;
			}
		}
		if ((status & IResourceDelta.ADDED) != 0) {
			if (parent instanceof IPackageFragment) {
				// refresh one level above to deal with empty package filtering properly
				postRefresh(internalGetParent(parent), PARENT, parent, runnables);	
				return true;
			} else
			{
				postAdd(parent, resource, runnables);
				return false;
			}
		}
		if ((status & IResourceDelta.CHANGED) != 0) {
			if ((flags & IResourceDelta.TYPE) != 0) {
				postRefresh(parent, PARENT, resource, runnables);
				return true;
			}
		}
		// open/close state change of a project
		if ((flags & IResourceDelta.OPEN) != 0) {
			postProjectStateChanged(internalGetParent(parent), runnables);
			return true;		
		}
		IResourceDelta[] resourceDeltas= delta.getAffectedChildren();
		 
		int count= 0;
		for (int i= 0; i < resourceDeltas.length; i++) {
			int kind= resourceDeltas[i].getKind();
			if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED) {
				count++;
				if (count > 1) {
					postRefresh(parent, PARENT, resource, runnables);
					return true;
				}
			}
		}	
		for (int i= 0; i < resourceDeltas.length; i++) {
			if (processResourceDelta(resourceDeltas[i], resource, runnables)) {
				return false; // early return, element got refreshed
			}
		}
		return false;
	}
	
	public void setIsFlatLayout(boolean state) {
		fIsFlatLayout= state;
	}
	
	public void setShowLibrariesNode(boolean state) {
		fShowLibrariesNode= state;
	}
	
	private void postRefresh(Object root, int relation, Object affectedElement, Collection runnables) {
		// JFace doesn't refresh when object isn't part of the viewer
		// Therefore move the refresh start down to the viewer's input
		if (isParent(root, fInput)) 
			root= fInput;
		List toRefresh= new ArrayList(1);
		toRefresh.add(root);
		augmentElementToRefresh(toRefresh, relation, affectedElement);
		postRefresh(toRefresh, true, runnables);
	}
	
	/**
	 * Can be implemented by subclasses to add additional elements to refresh
	 * 
	 * @param toRefresh the elements to refresh
	 * @param relation the relation to the affected element ({@link #GRANT_PARENT}, {@link #PARENT}, {@link #ORIGINAL}, {@link #PROJECT})
	 * @param affectedElement the affected element
	 */
	protected void augmentElementToRefresh(List toRefresh, int relation, Object affectedElement) {
	}

	private boolean isParent(Object root, Object child) {
		Object parent= getParent(child);
		if (parent == null)
			return false;
		if (parent.equals(root))
			return true;
		return isParent(root, parent);
	}

	protected void postRefresh(final List toRefresh, final boolean updateLabels, Collection runnables) {
		runnables.add(new Runnable() {
			public void run() {
				for (Iterator iter= toRefresh.iterator(); iter.hasNext();) {
					fViewer.refresh(iter.next(), updateLabels);
				}
			}
		});
	}

	protected void postAdd(final Object parent, final Object element, Collection runnables) {
		runnables.add(new Runnable() {
			public void run() {
				Widget[] items= fViewer.testFindItems(element);
				for (int i= 0; i < items.length; i++) {
					Widget item= items[i];
					if (item instanceof TreeItem && !item.isDisposed()) {
						TreeItem parentItem= ((TreeItem) item).getParentItem();
						if (parentItem != null && !parentItem.isDisposed() && parent.equals(parentItem.getData())) {
							return; // no add, element already added (most likely by a refresh)
						}
					}
				}
				fViewer.add(parent, element);
			}
		});
	}

	protected void postRemove(final Object element, Collection runnables) {
		runnables.add(new Runnable() {
			public void run() {
				fViewer.remove(element);
			}
		});
	}

	protected void postProjectStateChanged(final Object root, Collection runnables) {
		runnables.add(new Runnable() {
			public void run() {
				fViewer.refresh(root, true);
				// trigger a synthetic selection change so that action refresh their
				// enable state.
				fViewer.setSelection(fViewer.getSelection());
			}
		});
	}
	
	
	/*
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (arePackagesFoldedInHierarchicalLayout() != fFoldPackages){
			fFoldPackages= arePackagesFoldedInHierarchicalLayout();
			if (fViewer != null && !fViewer.getControl().isDisposed()) {
				fViewer.getControl().setRedraw(false);
				Object[] expandedObjects= fViewer.getExpandedElements();
				fViewer.refresh();	
				fViewer.setExpandedElements(expandedObjects);
				fViewer.getControl().setRedraw(true);
			}
		}
	}
	
	// inner class for deferred model loading
	public class LoadModelJob extends Job {
		 
		private LoadingModelNode placeHolder;
		private AbstractTreeViewer viewer;
		private PackageFragmentRootContainer packageFragmentRootContainer;
	
		public LoadModelJob(AbstractTreeViewer viewer, LoadingModelNode placeHolder, PackageFragmentRootContainer packageFragmentRootContainer) {
			super(placeHolder.getText());
			this.viewer = viewer;
			this.placeHolder = placeHolder;
			this.packageFragmentRootContainer = packageFragmentRootContainer;
		}
	
		protected IStatus run(IProgressMonitor monitor) { 
	
			LoadingModelUIAnimationJob updateUIJob = new LoadingModelUIAnimationJob(viewer, placeHolder);
			updateUIJob.schedule();
	
			Object[] retVal = new Object[0];
			try {
				// Load the model in the background after starting the animation job
				
				retVal = getContainerPackageFragmentRoots(packageFragmentRootContainer, fIsFlatLayout, null);
				
			} finally { 
				/* dispose of the place holder, causes the termination of the animation job */
				placeHolder.dispose();
				new ClearPlaceHolderJob(viewer, placeHolder, packageFragmentRootContainer, retVal).schedule();
			}
			
			return Status.OK_STATUS;
		}
	
	}
}
