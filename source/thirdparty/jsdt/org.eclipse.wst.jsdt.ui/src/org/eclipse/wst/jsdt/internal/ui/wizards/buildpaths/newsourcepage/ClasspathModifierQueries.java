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
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.newsourcepage;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.NewFolderDialog;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.ExclusionInclusionDialog;
import org.eclipse.wst.jsdt.ui.wizards.BuildPathDialogAccess;

/**
 * Helper class for queries used by the <code>ClasspathModifier</code>. 
 * Clients can either decide to implement their own queries or just taking 
 * the predefined queries.
 */
public class ClasspathModifierQueries {
    
    /**
     * Query to get information about the inclusion and exclusion filters of
     * an element.
     */
    public static interface IInclusionExclusionQuery {
        /**
         * Query to get information about the
         * inclusion and exclusion filters of
         * an element.
         * 
         * While executing <code>doQuery</code>,
         * these filter might change.
         * 
         * On calling <code>getInclusionPattern()</code>
         * or <code>getExclusionPattern()</code> it
         * is expected to get the new and updated
         * filters back.
         * 
         * @param element the element to get the
         * information from
         * @param focusOnExcluded
         * @return <code>true</code> if changes
         * have been accepted and <code>getInclusionPatter</code>
         * or <code>getExclusionPattern</code> can
         * be called.
         */
        public boolean doQuery(CPListElement element, boolean focusOnExcluded);
        
        /**
         * Can only be called after <code>
         * doQuery</code> has been executed and
         * has returned <code>true</code>
         * 
         * @return the new inclusion filters
         */
        public IPath[] getInclusionPattern();
        
        /**
         * Can only be called after <code>
         * doQuery</code> has been executed and
         * has returned <code>true</code>
         *
         * @return the new exclusion filters
         */
        public IPath[] getExclusionPattern();
    }

    /**
	 * Query to determine whether a linked folder should be removed.
	 */
	public static interface IRemoveLinkedFolderQuery {

		/** Remove status indicating that the removal should be cancelled */
		public static final int REMOVE_CANCEL= 0;

		/** Remove status indicating that the folder should be removed from the build path only */
		public static final int REMOVE_BUILD_PATH= 1;

		/** Remove status indicating that the folder should be removed from the build path and deleted */
		public static final int REMOVE_BUILD_PATH_AND_FOLDER= 2;

		/**
		 * Query to determined whether the linked folder should be removed as well.
		 * 
		 * @param folder the linked folder to remove
		 * @return a status code corresponding to one of the IRemoveLinkedFolderQuery#REMOVE_XXX constants
		 */
		public int doQuery(IFolder folder);
	}

    /**
	 * Query to create a folder.
	 */
    public static interface ICreateFolderQuery {
        /**
         * Query to create a folder.
         * 
         * @return <code>true</code> if the operation
         * was successful (e.g. no cancelled), <code>
         * false</code> otherwise
         */
        public boolean doQuery();
        
        /**
         * Find out whether a source folder is about
         * to be created or a normal folder which
         * is not on the classpath (and therefore
         * might have to be excluded).
         * 
         * Should only be called after having executed
         * <code>doQuery</code>, because otherwise
         * it might not be sure if a result exists or
         * not.
         * 
         * @return <code>true</code> if a source
         * folder should be created, <code>false
         * </code> otherwise
         */
        public boolean isSourceFolder();
        
        /**
         * Get the newly created folder.
         * This method is only valid after having
         * called <code>doQuery</code>.
         * 
         * @return the created folder of type
         * <code>IFolder</code>
         */
        public IFolder getCreatedFolder();
    }

    /**
     * Query to add archives (.jar or .zip files) to the buildpath.
     */
    public static interface IAddArchivesQuery {
        /**
         * Get the paths to the new archive entries that should be added to the buildpath.
         * 
         * @return Returns the new classpath container entry paths or an empty array if the query has
         * been cancelled by the user.
         */
        public IPath[] doQuery();
    }
    
    /**
     * Query to add libraries to the buildpath.
     */
    public static interface IAddLibrariesQuery {
        /**
         * Get the new classpath entries for libraries to be added to the buildpath.
         * 
         * @param project the Java project
         * @param entries an array of classpath entries for the project
         * @return Returns the selected classpath container entries or an empty if the query has
         * been cancelled by the user.
         */
        public IIncludePathEntry[] doQuery(final IJavaScriptProject project, final IIncludePathEntry[] entries);
    }

    /**
     * A default query for inclusion and exclusion filters.
     * The query is used to get information about the
     * inclusion and exclusion filters of an element.
     * 
     * @param shell shell if there is any or <code>null</code>
     * @return an <code>IInclusionExclusionQuery</code> that can be executed
     * 
     * @see ClasspathModifierQueries.IInclusionExclusionQuery
     */
	public static IInclusionExclusionQuery getDefaultInclusionExclusionQuery(final Shell shell) {
		return new IInclusionExclusionQuery() {
			
			protected IPath[] fInclusionPattern;
			protected IPath[] fExclusionPattern;
			
			public boolean doQuery(final CPListElement element, final boolean focusOnExcluded) {
				final boolean[] result= { false };
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						Shell sh= shell != null ? shell : JavaScriptPlugin.getActiveWorkbenchShell();
						ExclusionInclusionDialog dialog= new ExclusionInclusionDialog(sh, element, focusOnExcluded);
						result[0]= dialog.open() == Window.OK;
						fInclusionPattern= dialog.getInclusionPattern();
						fExclusionPattern= dialog.getExclusionPattern();
					}
				});
				return result[0];
			}
			
			public IPath[] getInclusionPattern() {
				return fInclusionPattern;
			}
			
			public IPath[] getExclusionPattern() {
				return fExclusionPattern;
			}
		};
	}
    
    /**
     * Shows the UI to select new external JAR or ZIP archive entries. If the query 
     * was aborted, the result is an empty array.
     * 
     * @param shell The parent shell for the dialog, can be <code>null</code>
     * @return an <code>IAddArchivesQuery</code> showing a dialog to selected archive files 
     * to be added to the buildpath
     * 
     * @see IAddArchivesQuery
     */
    public static IAddArchivesQuery getDefaultArchivesQuery(final Shell shell) {
        return new IAddArchivesQuery() {

            public IPath[] doQuery() {
                final IPath[][] selected= {null};
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        Shell sh= shell != null ? shell : JavaScriptPlugin.getActiveWorkbenchShell();
                        selected[0]= BuildPathDialogAccess.chooseExternalJAREntries(sh);
                    }
                });
                if(selected[0] == null)
                    return new IPath[0];
                return selected[0];
            }  
        };
    }

    /**
	 * Shows the UI to prompt whether a linked folder which has been removed from the build path should be deleted as well.
	 * 
	 * @param shell The parent shell for the dialog, can be <code>null</code>
	 * @return an <code>IRemoveLinkedFolderQuery</code> showing a dialog to prompt whether the linked folder should be deleted as well
	 * 
	 * @see IRemoveLinkedFolderQuery
	 */
	public static IRemoveLinkedFolderQuery getDefaultRemoveLinkedFolderQuery(final Shell shell) {
		return new IRemoveLinkedFolderQuery() {

			public final int doQuery(final IFolder folder) {
				final int[] result= { IRemoveLinkedFolderQuery.REMOVE_BUILD_PATH};
				Display.getDefault().syncExec(new Runnable() {

					public final void run() {
						final RemoveLinkedFolderDialog dialog= new RemoveLinkedFolderDialog((shell != null ? shell : JavaScriptPlugin.getActiveWorkbenchShell()), folder);
						final int status= dialog.open();
						if (status == 0)
							result[0]= dialog.getRemoveStatus();
						else
							result[0]= IRemoveLinkedFolderQuery.REMOVE_CANCEL;
					}
				});
				return result[0];
			}
		};
	}

    /**
     * Shows the UI to choose new classpath container classpath entries. See {@link IIncludePathEntry#CPE_CONTAINER} for
     * details about container classpath entries.
     * The query returns the selected classpath entries or an empty array if the query has
     * been cancelled.
     * 
     * @param shell The parent shell for the dialog, can be <code>null</code>
     * @return Returns the selected classpath container entries or an empty array if the query has
     * been cancelled by the user.
     */
    public static IAddLibrariesQuery getDefaultLibrariesQuery(final Shell shell) {
        return new IAddLibrariesQuery() {

            public IIncludePathEntry[] doQuery(final IJavaScriptProject project, final IIncludePathEntry[] entries) {
                final IIncludePathEntry[][] selected= {null};
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        Shell sh= shell != null ? shell : JavaScriptPlugin.getActiveWorkbenchShell();
                        selected[0]= BuildPathDialogAccess.chooseContainerEntries(sh, project, entries);
                    }
                });
                if(selected[0] == null)
                    return new IIncludePathEntry[0];
                return selected[0];
            }  
        };
    }

    /**
     * Shows the UI to create a new source folder. 
     * 
     * @param shell The parent shell for the dialog, can be <code>null</code>
     * @param project the Java project to create the source folder for
     * @return returns the query
     */
	public static ICreateFolderQuery getDefaultCreateFolderQuery(final Shell shell, final IJavaScriptProject project) {
		return new ICreateFolderQuery() {

			private IFolder fNewFolder;

			public boolean doQuery() {
				final boolean[] isOK= {false};
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        Shell sh= shell != null ? shell : JavaScriptPlugin.getActiveWorkbenchShell();
                        
                        NewFolderDialog dialog= new NewFolderDialog(sh, project.getProject());
                        isOK[0]= dialog.open() == Window.OK;
                        if (isOK[0]) {
                        	IResource sourceContainer= (IResource) dialog.getResult()[0];
                        	if (sourceContainer instanceof IFolder) {
                        		fNewFolder= (IFolder)sourceContainer;
                        	} else {
                        		fNewFolder= null;
                        	}
                        }
                    }
                });
                return isOK[0];
			}


			public boolean isSourceFolder() {
				return true;
			}

			public IFolder getCreatedFolder() {
				return fNewFolder;
			}
			
		};
	}
}
