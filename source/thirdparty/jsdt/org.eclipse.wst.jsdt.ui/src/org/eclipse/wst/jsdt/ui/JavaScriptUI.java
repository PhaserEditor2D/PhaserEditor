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
package org.eclipse.wst.jsdt.ui;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.JavaElementTransfer;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaUIMessages;
import org.eclipse.wst.jsdt.internal.ui.SharedImages;
import org.eclipse.wst.jsdt.internal.ui.dialogs.FilteredTypesSelectionDialog;
import org.eclipse.wst.jsdt.internal.ui.dialogs.PackageSelectionDialog;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.wst.jsdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.wst.jsdt.ui.dialogs.TypeSelectionExtension;
import org.eclipse.wst.jsdt.ui.text.IColorManager;

/**
 * Central access point for the JavaScript UI plug-in (id <code>"org.eclipse.wst.jsdt.ui"</code>).
 * This class provides static methods for:
 * <ul>
 *  <li> creating various kinds of selection dialogs to present a collection
 *       of JavaScript elements to the user and let them make a selection.</li>
 *  <li> opening a JavaScript editor on a compilation unit.</li> 
 * </ul>
 * <p>
 * This class provides static methods and fields only; it is not intended to be
 * instantiated or subclassed by clients.
 * </p>
 *  * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public final class JavaScriptUI {
	
	private static ISharedImages fgSharedImages= null;
	
	private JavaScriptUI() {
		// prevent instantiation of JavaScriptUI.
	}
	
	/**
	 * The id of the JavaScript plug-in (value <code>"org.eclipse.wst.jsdt.ui"</code>).
	 */	
	public static final String ID_PLUGIN= "org.eclipse.wst.jsdt.ui"; //$NON-NLS-1$
	
	/**
	 * The id of the JavaScript perspective
	 * (value <code>"org.eclipse.wst.jsdt.ui.JavaPerspective"</code>).
	 */	
	public static final String ID_PERSPECTIVE= 		"org.eclipse.wst.jsdt.ui.JavaPerspective"; //$NON-NLS-1$

	/**
	 * The id of the JavaScript action set
	 * (value <code>"org.eclipse.wst.jsdt.ui.JavaActionSet"</code>).
	 */
	public static final String ID_ACTION_SET= "org.eclipse.wst.jsdt.ui.JavaActionSet"; //$NON-NLS-1$

	/**
	 * The id of the JavaScript Element Creation action set
	 * (value <code>"org.eclipse.wst.jsdt.ui.JavaElementCreationActionSet"</code>).
	 * 
	 * 
	 */
	public static final String ID_ELEMENT_CREATION_ACTION_SET= "org.eclipse.wst.jsdt.ui.JavaElementCreationActionSet"; //$NON-NLS-1$
	
	/**
	 * The id of the JavaScript Coding action set
	 * (value <code>"org.eclipse.wst.jsdt.ui.CodingActionSet"</code>).
	 * 
	 * 
	 */
	public static final String ID_CODING_ACTION_SET= "org.eclipse.wst.jsdt.ui.CodingActionSet"; //$NON-NLS-1$

	/**
	 * The id of the JavaScript action set for open actions
	 * (value <code>"org.eclipse.wst.jsdt.ui.A_OpenActionSet"</code>).
	 * 
	 * 
	 */
	public static final String ID_OPEN_ACTION_SET= "org.eclipse.wst.jsdt.ui.A_OpenActionSet"; //$NON-NLS-1$

	/**
	 * The id of the JavaScript Search action set
	 * (value <code>org.eclipse.wst.jsdt.ui.SearchActionSet"</code>).
	 * 
	 * 
	 */
	public static final String ID_SEARCH_ACTION_SET= "org.eclipse.wst.jsdt.ui.SearchActionSet"; //$NON-NLS-1$
	
	/**
	 * The editor part id of the editor that presents JavaScript compilation units
	 * (value <code>"org.eclipse.wst.jsdt.ui.CompilationUnitEditor"</code>).
	 */	
	public static final String ID_CU_EDITOR=			"org.eclipse.wst.jsdt.ui.CompilationUnitEditor"; //$NON-NLS-1$
	
	/**
	 * The editor part id of the editor that presents JavaScript binary class files
	 * (value <code>"org.eclipse.wst.jsdt.ui.ClassFileEditor"</code>).
	 */
	public static final String ID_CF_EDITOR=			"org.eclipse.wst.jsdt.ui.ClassFileEditor"; //$NON-NLS-1$
	
	/**
	 * The editor part id of the code snippet editor
	 * (value <code>"org.eclipse.wst.jsdt.ui.SnippetEditor"</code>).
	 */
	public static final String ID_SNIPPET_EDITOR= 		"org.eclipse.wst.jsdt.ui.SnippetEditor"; //$NON-NLS-1$

	/**
	 * The view part id of the Packages view
	 * (value <code>"org.eclipse.wst.jsdt.ui.PackageExplorer"</code>).
	 * <p>
	 * When this id is used to access
	 * a view part with <code>IWorkbenchPage.findView</code> or 
	 * <code>showView</code>, the returned <code>IViewPart</code>
	 * can be safely cast to an <code>IPackagesViewPart</code>.
	 * </p>
	 *
	 * @see IPackagesViewPart
	 * @see org.eclipse.ui.IWorkbenchPage#findView(java.lang.String)
	 * @see org.eclipse.ui.IWorkbenchPage#showView(java.lang.String)
	 */ 
	public static final String ID_PACKAGES= 			"org.eclipse.wst.jsdt.ui.PackageExplorer"; //$NON-NLS-1$
	
	/** 
	 * The view part id of the type hierarchy part
	 * (value <code>"org.eclipse.wst.jsdt.ui.TypeHierarchy"</code>).
	 * <p>
	 * When this id is used to access
	 * a view part with <code>IWorkbenchPage.findView</code> or 
	 * <code>showView</code>, the returned <code>IViewPart</code>
	 * can be safely cast to an <code>ITypeHierarchyViewPart</code>.
	 * </p>
	 *
	 * @see ITypeHierarchyViewPart
	 * @see org.eclipse.ui.IWorkbenchPage#findView(java.lang.String)
	 * @see org.eclipse.ui.IWorkbenchPage#showView(java.lang.String)
	 */ 
	public static final String ID_TYPE_HIERARCHY= 		"org.eclipse.wst.jsdt.ui.TypeHierarchy"; //$NON-NLS-1$

	/** 
	 * The view part id of the source (declaration) view
	 * (value <code>"org.eclipse.wst.jsdt.ui.SourceView"</code>).
	 *
	 * @see org.eclipse.ui.IWorkbenchPage#findView(java.lang.String)
	 * @see org.eclipse.ui.IWorkbenchPage#showView(java.lang.String)
	 * 
	 */ 
	public static final String ID_SOURCE_VIEW=	"org.eclipse.wst.jsdt.ui.SourceView"; //$NON-NLS-1$
	
	/** 
	 * The view part id of the Javadoc view
	 * (value <code>"org.eclipse.wst.jsdt.ui.JavadocView"</code>).
	 *
	 * @see org.eclipse.ui.IWorkbenchPage#findView(java.lang.String)
	 * @see org.eclipse.ui.IWorkbenchPage#showView(java.lang.String)
	 * 
	 */ 
	public static final String ID_JAVADOC_VIEW=	"org.eclipse.wst.jsdt.ui.JavadocView"; //$NON-NLS-1$
	
	/**
	 * The id of the JavaScript Browsing Perspective
	 * (value <code>"org.eclipse.wst.jsdt.ui.JavaBrowsingPerspective"</code>).
	 * 
	 * 
	 */
	public static String ID_BROWSING_PERSPECTIVE= "org.eclipse.wst.jsdt.ui.JavaBrowsingPerspective"; //$NON-NLS-1$

	/**
	 * The view part id of the JavaScript Browsing Projects view
	 * (value <code>"org.eclipse.wst.jsdt.ui.ProjectsView"</code>).
	 * 
	 * 
	 */
	public static String ID_PROJECTS_VIEW= "org.eclipse.wst.jsdt.ui.ProjectsView"; //$NON-NLS-1$

	/**
	 * The view part id of the JavaScript Browsing Packages view
	 * (value <code>"org.eclipse.wst.jsdt.ui.PackagesView"</code>).
	 * 
	 * 
	 */
	public static String ID_PACKAGES_VIEW= "org.eclipse.wst.jsdt.ui.PackagesView"; //$NON-NLS-1$

	/**
	 * The view part id of the JavaScript Browsing Types view
	 * (value <code>"org.eclipse.wst.jsdt.ui.TypesView"</code>).
	 * 
	 * 
	 */
	public static String ID_TYPES_VIEW= "org.eclipse.wst.jsdt.ui.TypesView"; //$NON-NLS-1$

	/**
	 * The view part id of the JavaScript Browsing Members view
	 * (value <code>"org.eclipse.wst.jsdt.ui.MembersView"</code>).
	 * 
	 * 
	 */
	public static String ID_MEMBERS_VIEW= "org.eclipse.wst.jsdt.ui.MembersView"; //$NON-NLS-1$

	/**
	 * Returns the shared images for the JavaScript UI.
	 *
	 * @return the shared images manager
	 */
	public static ISharedImages getSharedImages() {
		if (fgSharedImages == null)
			fgSharedImages= new SharedImages();
			
		return fgSharedImages;
	}
	 
	/**
	 * Creates a selection dialog that lists all packages of the given JavaScript project.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected package (of type
	 * <code>IPackageFragment</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param project the JavaScript project
	 * @param style flags defining the style of the dialog; the valid flags are:
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_BINARIES</code>, indicating that 
	 *   packages from binary package fragment roots should be included in addition
	 *   to those from source package fragment roots;
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_REQUIRED_PROJECTS</code>, indicating that
	 *   packages from required projects should be included as well.
	 * @param filter the initial pattern to filter the set of packages. For example "com" shows 
	 * all packages starting with "com". The meta character '?' representing any character and 
	 * '*' representing any string are supported. Clients can pass an empty string if no filtering 
	 * is required.
	 * @return a new selection dialog
	 * @exception JavaScriptModelException if the selection dialog could not be opened
	 * 
	 * 
	 */
	public static SelectionDialog createPackageDialog(Shell parent, IJavaScriptProject project, int style, String filter) throws JavaScriptModelException {
		Assert.isTrue((style | IJavaScriptElementSearchConstants.CONSIDER_BINARIES | IJavaScriptElementSearchConstants.CONSIDER_REQUIRED_PROJECTS) ==
			(IJavaScriptElementSearchConstants.CONSIDER_BINARIES | IJavaScriptElementSearchConstants.CONSIDER_REQUIRED_PROJECTS));

		IPackageFragmentRoot[] roots= null;
		if ((style & IJavaScriptElementSearchConstants.CONSIDER_REQUIRED_PROJECTS) != 0) {
		    roots= project.getAllPackageFragmentRoots();
		} else {	
			roots= project.getPackageFragmentRoots();	
		}
		
		List consideredRoots= null;
		if ((style & IJavaScriptElementSearchConstants.CONSIDER_BINARIES) != 0) {
			consideredRoots= Arrays.asList(roots);
		} else {
			consideredRoots= new ArrayList(roots.length);
			for (int i= 0; i < roots.length; i++) {
				IPackageFragmentRoot root= roots[i];
				if (root.getKind() != IPackageFragmentRoot.K_BINARY)
					consideredRoots.add(root);
					
			}
		}
		
		IJavaScriptSearchScope searchScope= SearchEngine.createJavaSearchScope((IJavaScriptElement[])consideredRoots.toArray(new IJavaScriptElement[consideredRoots.size()]));
		BusyIndicatorRunnableContext context= new BusyIndicatorRunnableContext();
		if (style == 0 || style == IJavaScriptElementSearchConstants.CONSIDER_REQUIRED_PROJECTS) {
			return createPackageDialog(parent, context, searchScope, false, true, filter);
		} else {
			return createPackageDialog(parent, context, searchScope, false, false, filter);
		}
	}
	
	/**
	 * Creates a selection dialog that lists all packages of the given JavaScript search scope.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected package (of type
	 * <code>IPackageFragment</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param context the runnable context to run the search in
	 * @param scope the scope defining the available packages.
	 * @param multipleSelection true if multiple selection is allowed
	 * @param removeDuplicates true if only one package is shown per package name
	 * @param filter the initial pattern to filter the set of packages. For example "com" shows 
	 * all packages starting with "com". The meta character '?' representing any character and 
	 * '*' representing any string are supported. Clients can pass an empty string if no filtering 
	 * is required.
	 * @return a new selection dialog
	 * 
	 * 
	 */
	public static SelectionDialog createPackageDialog(Shell parent, IRunnableContext context, IJavaScriptSearchScope scope, 
			boolean multipleSelection, boolean removeDuplicates, String filter) {
		
		int flag= removeDuplicates ? PackageSelectionDialog.F_REMOVE_DUPLICATES : 0;
		PackageSelectionDialog dialog= new PackageSelectionDialog(parent, context, flag, scope);
		dialog.setFilter(filter);
		dialog.setIgnoreCase(false);
		dialog.setMultipleSelection(multipleSelection);
		return dialog;
	}
	
	/**
	 * Creates a selection dialog that lists all packages of the given JavaScript project.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected package (of type
	 * <code>IPackageFragment</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param project the JavaScript project
	 * @param style flags defining the style of the dialog; the valid flags are:
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_BINARIES</code>, indicating that 
	 *   packages from binary package fragment roots should be included in addition
	 *   to those from source package fragment roots;
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_REQUIRED_PROJECTS</code>, indicating that
	 *   packages from required projects should be included as well.
	 * @return a new selection dialog
	 * @exception JavaScriptModelException if the selection dialog could not be opened
	 */
	public static SelectionDialog createPackageDialog(Shell parent, IJavaScriptProject project, int style) throws JavaScriptModelException {
		return createPackageDialog(parent, project, style, ""); //$NON-NLS-1$
	}
	
	/**
	 * Creates a selection dialog that lists all packages under the given package 
	 * fragment root.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected package (of type
	 * <code>IPackageFragment</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param root the package fragment root
	 * @param filter the initial pattern to filter the set of packages. For example "com" shows 
	 * all packages starting with "com". The meta character '?' representing any character and 
	 * '*' representing any string are supported. Clients can pass an empty string if no filtering 
	 * is required.
	 * @return a new selection dialog
	 * @exception JavaScriptModelException if the selection dialog could not be opened
	 * 
	 * 
	 */
	public static SelectionDialog createPackageDialog(Shell parent, IPackageFragmentRoot root, String filter) throws JavaScriptModelException {
		IJavaScriptSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaScriptElement[] {root});
		BusyIndicatorRunnableContext context= new BusyIndicatorRunnableContext();
		return createPackageDialog(parent, context, scope, false, true, filter);
	}

	/**
	 * Creates a selection dialog that lists all packages under the given package 
	 * fragment root.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected package (of type
	 * <code>IPackageFragment</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param root the package fragment root
	 * @return a new selection dialog
	 * @exception JavaScriptModelException if the selection dialog could not be opened
	 */
	public static SelectionDialog createPackageDialog(Shell parent, IPackageFragmentRoot root) throws JavaScriptModelException {
		return createPackageDialog(parent, root, ""); //$NON-NLS-1$
	}

	/**
	 * Creates a selection dialog that lists all types in the given project.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected type(s) (of type
	 * <code>IType</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param context the runnable context used to show progress when the dialog
	 *   is being populated
	 * @param project the JavaScript project
	 * @param style flags defining the style of the dialog; the only valid values are
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_CLASSES</code>,
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_INTERFACES</code>, 
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_ANNOTATION_TYPES</code>,
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_ENUMS</code>,
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_ALL_TYPES</code>,
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_CLASSES_AND_INTERFACES</code>
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_CLASSES_AND_ENUMS</code>. Please note that
	 *   the bitwise OR combination of the elementary constants is not supported.
	 * @param multipleSelection <code>true</code> if multiple selection is allowed
	 * 
	 * @return a new selection dialog
	 * 
	 * @exception JavaScriptModelException if the selection dialog could not be opened
	 */
	public static SelectionDialog createTypeDialog(Shell parent, IRunnableContext context, IProject project, int style, boolean multipleSelection) throws JavaScriptModelException {
		IJavaScriptSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaScriptProject[] { JavaScriptCore.create(project) });
		return createTypeDialog(parent, context, scope, style, multipleSelection);
	}
	
	/**
	 * Creates a selection dialog that lists all types in the given scope.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected type(s) (of type
	 * <code>IType</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param context the runnable context used to show progress when the dialog
	 *   is being populated
	 * @param scope the scope that limits which types are included
	 * @param style flags defining the style of the dialog; the only valid values are
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_CLASSES</code>,
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_INTERFACES</code>, 
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_ANNOTATION_TYPES</code>,
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_ENUMS</code>,
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_ALL_TYPES</code>,
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_CLASSES_AND_INTERFACES</code>
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_CLASSES_AND_ENUMS</code>. Please note that
	 *   the bitwise OR combination of the elementary constants is not supported.
	 * @param multipleSelection <code>true</code> if multiple selection is allowed
	 * 
	 * @return a new selection dialog
	 * 
	 * @exception JavaScriptModelException if the selection dialog could not be opened
	 */
	public static SelectionDialog createTypeDialog(Shell parent, IRunnableContext context, IJavaScriptSearchScope scope, int style, boolean multipleSelection) throws JavaScriptModelException {
		return createTypeDialog(parent, context, scope, style, multipleSelection, "");//$NON-NLS-1$
	}
		
	/**
	 * Creates a selection dialog that lists all types in the given scope.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected type(s) (of type
	 * <code>IType</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param context the runnable context used to show progress when the dialog
	 *   is being populated
	 * @param scope the scope that limits which types are included
	 * @param style flags defining the style of the dialog; the only valid values are
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_CLASSES</code>,
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_INTERFACES</code>, 
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_ANNOTATION_TYPES</code>,
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_ENUMS</code>,
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_ALL_TYPES</code>,
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_CLASSES_AND_INTERFACES</code>
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_CLASSES_AND_ENUMS</code>. Please note that
	 *   the bitwise OR combination of the elementary constants is not supported.
	 * @param multipleSelection <code>true</code> if multiple selection is allowed
	 * @param filter the initial pattern to filter the set of types. For example "Abstract" shows 
	 *  all types starting with "abstract". The meta character '?' representing any character and 
	 *  '*' representing any string are supported. Clients can pass an empty string if no filtering 
	 *  is required.
	 *  
	 * @return a new selection dialog
	 * 
	 * @exception JavaScriptModelException if the selection dialog could not be opened
	 * 
	 * 
	 */
	public static SelectionDialog createTypeDialog(Shell parent, IRunnableContext context, IJavaScriptSearchScope scope, int style, boolean multipleSelection, String filter) throws JavaScriptModelException {
		return createTypeDialog(parent, context, scope, style, multipleSelection, filter, null);
	}
	
	/**
	 * Creates a selection dialog that lists all types in the given scope.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected type(s) (of type
	 * <code>IType</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param parent the parent shell of the dialog to be created
	 * @param context the runnable context used to show progress when the dialog
	 *   is being populated
	 * @param scope the scope that limits which types are included
	 * @param style flags defining the style of the dialog; the only valid values are
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_CLASSES</code>,
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_INTERFACES</code>, 
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_ANNOTATION_TYPES</code>,
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_ENUMS</code>,
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_ALL_TYPES</code>,
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_CLASSES_AND_INTERFACES</code>
	 *   <code>IJavaScriptElementSearchConstants.CONSIDER_CLASSES_AND_ENUMS</code>. Please note that
	 *   the bitwise OR combination of the elementary constants is not supported.
	 * @param multipleSelection <code>true</code> if multiple selection is allowed
	 * @param filter the initial pattern to filter the set of types. For example "Abstract" shows 
	 *  all types starting with "abstract". The meta character '?' representing any character and 
	 *  '*' representing any string are supported. Clients can pass an empty string if no filtering
	 *  is required.
	 * @param extension a user interface extension to the type selection dialog or <code>null</code>
	 *  if no extension is desired
	 *  
	 * @return a new selection dialog
	 * 
	 * @exception JavaScriptModelException if the selection dialog could not be opened
	 * 
	 * 
	 */
	public static SelectionDialog createTypeDialog(Shell parent, IRunnableContext context, IJavaScriptSearchScope scope, int style, 
			boolean multipleSelection, String filter, TypeSelectionExtension extension) throws JavaScriptModelException {
		int elementKinds= 0;
		if (style == IJavaScriptElementSearchConstants.CONSIDER_ALL_TYPES) {
			elementKinds= IJavaScriptSearchConstants.TYPE;
		} else if (style == IJavaScriptElementSearchConstants.CONSIDER_CLASSES) {
			elementKinds= IJavaScriptSearchConstants.CLASS;
		} else {	
			throw new IllegalArgumentException("Invalid style constant."); //$NON-NLS-1$
		}
		FilteredTypesSelectionDialog dialog= new FilteredTypesSelectionDialog(parent, multipleSelection, 
			context, scope, elementKinds, extension);
		dialog.setMessage(JavaUIMessages.JavaUI_defaultDialogMessage); 
		dialog.setInitialPattern(filter);
		return dialog;
	}



	/**
	 * Opens an editor on the given JavaScript element in the active page. Valid elements are all JavaScript elements that are {@link ISourceReference}.
 	 * For elements inside a compilation unit or class file, the parent is opened in the editor is opened and the element revealed.
 	 * If there already is an open JavaScript editor for the given element, it is returned.
	 *
	 * @param element the input element; either a compilation unit 
	 *   (<code>IJavaScriptUnit</code>) or a class file (<code>IClassFile</code>) or source references inside.
	 * @return returns the editor part of the opened editor or <code>null</code> if the element is not a {@link ISourceReference} or the
	 * file was opened in an external editor.
	 * @exception PartInitException if the editor could not be initialized or no workbench page is active
	 * @exception JavaScriptModelException if this element does not exist or if an exception occurs while accessing its underlying resource
	 */
	public static IEditorPart openInEditor(IJavaScriptElement element) throws JavaScriptModelException, PartInitException {
		return openInEditor(element, true, true);
	}
	
	/**
	 * Opens an editor on the given JavaScript element in the active page. Valid elements are all JavaScript elements that are {@link ISourceReference}.
 	 * For elements inside a compilation unit or class file, the parent is opened in the editor is opened.
 	 * If there already is an open JavaScript editor for the given element, it is returned.
	 *
	 * @param element the input element; either a compilation unit 
	 *   (<code>IJavaScriptUnit</code>) or a class file (<code>IClassFile</code>) or source references inside.
	 * @param activate if set, the editor will be activated.
	 * @param reveal if set, the element will be revealed.
	 * @return returns the editor part of the opened editor or <code>null</code> if the element is not a {@link ISourceReference} or the
	 * file was opened in an external editor.
	 * @exception PartInitException if the editor could not be initialized or no workbench page is active
	 * @exception JavaScriptModelException if this element does not exist or if an exception occurs while accessing its underlying resource
	 * 
	 */
	public static IEditorPart openInEditor(IJavaScriptElement element, boolean activate, boolean reveal) throws JavaScriptModelException, PartInitException {
		if (!(element instanceof ISourceReference)) {
			return null;
		}
		IEditorPart part= EditorUtility.openInEditor(element, activate);
		if (reveal && part != null) {
			EditorUtility.revealInEditor(part, element);
		}
		return part;
	}	

	/** 
	 * Reveals the given JavaScript element  in the given editor. If the element is not an instance
	 * of <code>ISourceReference</code> this method result in a NOP. If it is a source
	 * reference no checking is done if the editor displays a compilation unit or class file that 
	 * contains the source reference element. The editor simply reveals the source range 
	 * denoted by the given element.
	 * 
	 * @param part the editor displaying a compilation unit or class file
	 * @param element the element to be revealed
	 * 
	 * 
	 */
	public static void revealInEditor(IEditorPart part, IJavaScriptElement element) {
		EditorUtility.revealInEditor(part, element);
	}
	 
	/**
	 * Returns the working copy manager for the JavaScript UI plug-in.
	 *
	 * @return the working copy manager for the JavaScript UI plug-in
	 */
	public static IWorkingCopyManager getWorkingCopyManager() {
		return JavaScriptPlugin.getDefault().getWorkingCopyManager();
	}

	/**
	 * Returns the JavaScript element wrapped by the given editor input.
	 *
	 * @param editorInput the editor input
	 * @return the JavaScript element wrapped by <code>editorInput</code> or <code>null</code> if none
	 * 
	 */
	public static IJavaScriptElement getEditorInputJavaElement(IEditorInput editorInput) {
		// Performance: check working copy manager first: this is faster
		IJavaScriptElement je= JavaScriptPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(editorInput);
		if (je != null)
			return je;
		
		return (IJavaScriptElement)editorInput.getAdapter(IJavaScriptElement.class);
	}

	/**
	 * Returns the DocumentProvider used for JavaScript compilation units.
	 *
	 * @return the DocumentProvider for JavaScript compilation units.
	 * 
	 * @see IDocumentProvider
	 * 
	 */
	public static IDocumentProvider getDocumentProvider() {
		return JavaScriptPlugin.getDefault().getCompilationUnitDocumentProvider();
	}
		
	/**
	 * Returns the Javadoc location for library's classpath entry or <code>null</code> if no
	 * location is available. Note that only classpath entries of kind {@link IIncludePathEntry#CPE_LIBRARY} and
	 * {@link IIncludePathEntry#CPE_VARIABLE} support Javadoc locations.
	 * 
	 * @param entry the classpath entry to get the Javadoc location for
	 * @return the Javadoc location or<code>null</code> if no Javadoc location is available
	 * @throws IllegalArgumentException Thrown when the entry is <code>null</code> or not of kind
	 * {@link IIncludePathEntry#CPE_LIBRARY} or {@link IIncludePathEntry#CPE_VARIABLE}.
	 * 
	 * 
	 */	
	public static URL getLibraryJSdocLocation(IIncludePathEntry entry) {
		return JavaDocLocations.getLibraryJavadocLocation(entry);
	}
	
	/**
	 * Sets the Javadoc location for a JavaScript project. This location is used for
	 * all types located in the project's source folders.
	 * 
	 * @param project the project
	 * @param url the Javadoc location to set. This location should contain index.html and
	 * a file 'package-list'. <code>null</code> clears the current documentation
	 * location.
	 * 
	 * 
	 */
	public static void setProjectJSdocLocation(IJavaScriptProject project, URL url) {
		JavaDocLocations.setProjectJavadocLocation(project, url);
	}

	/**
	 * Returns the Javadoc location for a JavaScript project or <code>null</code> if no
	 * location is available. This location is used for all types located in the project's
	 * source folders.
	 * 
	 * @param project the project
	 * @return the Javadoc location for a JavaScript project or <code>null</code>
	 * 
	 * 
	 */	
	public static URL getProjectJSdocLocation(IJavaScriptProject project) {
		return JavaDocLocations.getProjectJavadocLocation(project);
	}	

	/**
	 * Returns the Javadoc base URL for an element. The base location contains the
	 * index file. This location doesn't have to exist. Returns <code>null</code>
	 * if no javadoc location has been attached to the element's library or project.
	 * Example of a returned URL is <i>http://www.junit.org/junit/javadoc</i>.
	 * 
	 * @param element the element for which the documentation URL is requested.
	 * @return the base location
	 * @throws JavaScriptModelException thrown when the element can not be accessed
	 * 
	 * 
	 */		
	public static URL getJSdocBaseLocation(IJavaScriptElement element) throws JavaScriptModelException {	
		return JavaDocLocations.getJavadocBaseLocation(element);
	}
	
	/**
	 * Returns the Javadoc URL for an element. Example of a returned URL is
	 * <i>http://www.junit.org/junit/javadoc/junit/extensions/TestSetup.html</i>.
	 * This returned location doesn't have to exist. Returns <code>null</code>
	 * if no javadoc location has been attached to the element's library or
	 * project.
	 * 
	 * @param element the element for which the documentation URL is requested.
	 * @param includeAnchor If set, the URL contains an anchor for member references:
	 * <i>http://www.junit.org/junit/javadoc/junit/extensions/TestSetup.html#run(junit.framework.TestResult)</i>. Note
	 * that this involves type resolving and is a more expensive call than without anchor.
	 * @return the Javadoc URL for the element
	 * @throws JavaScriptModelException thrown when the element can not be accessed
	 * 
	 * 
	 */		
	public static URL getJSdocLocation(IJavaScriptElement element, boolean includeAnchor) throws JavaScriptModelException {
		return JavaDocLocations.getJavadocLocation(element, includeAnchor);
	}
	
	/**
	 * Returns the transfer instance used to copy/paste JavaScript elements to
	 * and from the clipboard. Objects managed by this transfer instance
	 * are of type <code>IJavaScriptElement[]</code>. So to access data from the
	 * clipboard clients should use the following code snippet:
	 * <pre>
	 *   IJavaScriptElement[] elements=
	 *     (IJavaScriptElement[])clipboard.getContents(JavaScriptUI.getJavaElementClipboardTransfer());
	 * </pre>  
	 * 
	 * To put elements into the clipboard use the following snippet:
	 * 
	 * <pre>
	 *    IJavaScriptElement[] javaElements= ...;
	 *    clipboard.setContents(
	 *     new Object[] { javaElements },
	 *     new Transfer[] { JavaScriptUI.getJavaElementClipboardTransfer() } );
	 * </pre>
	 * 
	 * @return returns the transfer object used to copy/paste JavaScript elements
	 *  to and from the clipboard
	 * 
	 * 
	 */
	public static Transfer getJavaElementClipboardTransfer() {
		return JavaElementTransfer.getInstance();
	}
	
	/**
	 * Returns the color manager the JavaScript UI plug-in which is used to manage
	 * any Java-specific colors needed for such things like syntax highlighting.
	 *
	 * @return the color manager to be used for JavaScript text viewers
	 * 
	 */
	public static IColorManager getColorManager() {
		return JavaScriptPlugin.getDefault().getJavaTextTools().getColorManager();
	}
}
