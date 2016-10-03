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
package org.eclipse.wst.jsdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.refactoring.nls.NLSHintHelper;
import org.eclipse.wst.jsdt.internal.corext.refactoring.nls.NLSRefactoring;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.refactoring.nls.search.SearchBrokenNLSKeysUtil;
import org.eclipse.wst.jsdt.ui.IWorkingCopyManager;
import org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction;

public class FindBrokenNLSKeysAction extends SelectionDispatchAction {
	
	private static class SearchPatternData {

		private final IType fAccessorType;
		private final IFile fPropertyFile;
		
		public SearchPatternData(IType accessorType, IFile propertyFile) {
			fAccessorType= accessorType;
			fPropertyFile= propertyFile;
		}

		public IFile getPropertyFile() {
			return fPropertyFile;
		}

		public IType getWrapperClass() {
			return fAccessorType;
		}

	}

	//TODO: Add to API: IJavaEditorActionDefinitionIds
	public static final String FIND_BROKEN_NLS_KEYS_ACTION_ID= "org.eclipse.wst.jsdt.ui.edit.text.java.find.broken.nls.keys"; //$NON-NLS-1$

	//TODO: Add to API: JdtActionConstants
	public static final String ACTION_HANDLER_ID= "org.eclipse.wst.jsdt.ui.actions.FindNLSProblems"; //$NON-NLS-1$
	
	private JavaEditor fEditor;
	
	public FindBrokenNLSKeysAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.FindNLSProblemsAction_Name); 
		setToolTipText(ActionMessages.FindNLSProblemsAction_ToolTip); 
		setDescription(ActionMessages.FindNLSProblemsAction_Description);
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the Java editor
	 */
	public FindBrokenNLSKeysAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(getCompilationUnit(editor) != null);
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(ITextSelection selection) {
		ISelectionProvider selectionProvider= fEditor.getSelectionProvider();
		if (selectionProvider == null)
			return;
		
		run(new StructuredSelection(selectionProvider.getSelection()));
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(IStructuredSelection selection) {
		if (selection.size() == 1) {
			Object firstElement= selection.getFirstElement();
			if (firstElement instanceof IJavaScriptElement) {
				IJavaScriptElement javaElement= (IJavaScriptElement) firstElement;
				if (!ActionUtil.isProcessable(getShell(), javaElement)) {
					return;
				}
			}
		}
		
		SearchPatternData[] data= getNLSFiles(selection);
		if (data == null || data.length == 0) {
			MessageDialog.openInformation(getShell(), ActionMessages.FindNLSProblemsAction_ErrorDialogTitle, ActionMessages.FindNLSProblemsAction_NoPropertieFilesFoundErrorDescription);
			return;
		}
			
		String scope= "workspace"; //$NON-NLS-1$
		if (selection.size() == 1) {
			Object firstElement= selection.getFirstElement();
			if (firstElement instanceof IJavaScriptElement) {
				scope= ((IJavaScriptElement)firstElement).getElementName();
			} else if (firstElement instanceof IFile) {
				scope= ((IFile)firstElement).getName();
			} else if (firstElement instanceof IFolder) {
				scope= ((IFolder)firstElement).getName();
			}
		}
		run(data, scope);
	}
	
	private void run(SearchPatternData[] data, String scope) {
		List wrappers= new ArrayList();
		List properties= new ArrayList();
		for (int i= 0; i < data.length; i++) {
			SearchPatternData current= data[i];
			if (current.getWrapperClass() != null || current.getPropertyFile() != null) {
				wrappers.add(current.getWrapperClass());
				properties.add(current.getPropertyFile());
			}
		}
		IType[] accessorClasses= (IType[])wrappers.toArray(new IType[wrappers.size()]);
		IFile[] propertieFiles= (IFile[])properties.toArray(new IFile[properties.size()]);
		SearchBrokenNLSKeysUtil.search(scope, accessorClasses, propertieFiles);
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(ITextSelection selection) {
		ISelectionProvider selectionProvider= fEditor.getSelectionProvider();
		if (selectionProvider == null) {
			setEnabled(false);
		} else {
			selectionChanged(new StructuredSelection(selectionProvider.getSelection()));
		}
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(IStructuredSelection selection) { 
		setEnabled(canEnable(selection));
	}
	
	private SearchPatternData[] getNLSFiles(IStructuredSelection selection) {
		Object[] selectedElements= selection.toArray();
		List result= new ArrayList();
		collectNLSFiles(selectedElements, result);
		return (SearchPatternData[])result.toArray(new SearchPatternData[result.size()]);
	}
	
	private boolean canEnable(IStructuredSelection selection) {
		Object[] selected= selection.toArray();
		for (int i= 0; i < selected.length; i++) {
			try {
				if (selected[i] instanceof IJavaScriptElement) {
					IJavaScriptElement elem= (IJavaScriptElement) selected[i];
					if (elem.exists()) {
						switch (elem.getElementType()) {
							case IJavaScriptElement.TYPE:
								if (elem.getParent().getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT) {
									return true;
								}
								return false;
							case IJavaScriptElement.JAVASCRIPT_UNIT:
								return true;
							case IJavaScriptElement.IMPORT_CONTAINER:
								return false;
							case IJavaScriptElement.PACKAGE_FRAGMENT:
							case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
								IPackageFragmentRoot root= (IPackageFragmentRoot) elem.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
								return (root.getKind() == IPackageFragmentRoot.K_SOURCE);
							case IJavaScriptElement.JAVASCRIPT_PROJECT:
								return true;
						}
					}
				} else if (selected[i] instanceof LogicalPackage) {
					return true;
				} else if (selected[i] instanceof IFile) {
					IFile file= (IFile)selected[i];
					if ("properties".equalsIgnoreCase(file.getFileExtension())) //$NON-NLS-1$
						return true;
				}
			} catch (JavaScriptModelException e) {
				if (!e.isDoesNotExist()) {
					JavaScriptPlugin.log(e);
				}
			}
		}
		return false;
	}

	private void collectNLSFiles(Object[] objects, List result) {
		try {
			for (int i= 0; i < objects.length; i++) {
				if (objects[i] instanceof IJavaScriptElement) {
					IJavaScriptElement elem= (IJavaScriptElement) objects[i];
					if (elem.exists()) {
						switch (elem.getElementType()) {
							case IJavaScriptElement.TYPE:
								if (elem.getParent().getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT) {
									SearchPatternData data= tryIfPropertyCuSelected((IJavaScriptUnit)elem.getParent());
									if (data != null) {
										result.add(data);
									}
								}
								break;
							case IJavaScriptElement.JAVASCRIPT_UNIT:
								SearchPatternData data= tryIfPropertyCuSelected((IJavaScriptUnit)elem);
								if (data != null) {
									result.add(data);
								}
								break;
							case IJavaScriptElement.IMPORT_CONTAINER:
								break;
							case IJavaScriptElement.PACKAGE_FRAGMENT:
								IPackageFragment fragment= (IPackageFragment)elem;
								if (fragment.getKind() == IPackageFragmentRoot.K_SOURCE)
									collectNLSFiles(new Object[] {fragment.getCorrespondingResource()}, result);
								break;
							case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT: 
							{
								IPackageFragmentRoot root= (IPackageFragmentRoot) elem;
								if (root.getKind() == IPackageFragmentRoot.K_SOURCE)
									collectNLSFiles(new Object[] {root.getCorrespondingResource()}, result);
								break;
							}
							case IJavaScriptElement.JAVASCRIPT_PROJECT: 
							{
								IJavaScriptProject javaProject= (IJavaScriptProject)elem;
								IPackageFragmentRoot[] allPackageFragmentRoots= javaProject.getAllPackageFragmentRoots();
								for (int j= 0; j < allPackageFragmentRoots.length; j++) {
									IPackageFragmentRoot root= allPackageFragmentRoots[j];
									if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
										if (javaProject.equals(root.getJavaScriptProject())) {
											collectNLSFiles(new Object[] {root.getCorrespondingResource()}, result);
										}
									}
								}
								break;
							}
						}
					}
				} else if (objects[i] instanceof LogicalPackage) {
					LogicalPackage logicalPackage= (LogicalPackage)objects[i];
					collectNLSFiles(new Object[] {logicalPackage.getJavaProject()}, result);
				} else if (objects[i] instanceof IFolder) {
					collectNLSFiles(((IFolder)objects[i]).members(), result);
				} else if (objects[i] instanceof IFile) {
					SearchPatternData data= tryIfPropertyFileSelected((IFile)objects[i]);
					if (data != null) {
						result.add(data);
					}
				}
			}
		} catch (JavaScriptModelException e) {
			if (!e.isDoesNotExist()) {
				JavaScriptPlugin.log(e);
			}
		} catch (CoreException e) {
			JavaScriptPlugin.log(e);
		}
	}
	
	private SearchPatternData tryIfPropertyCuSelected(IJavaScriptUnit compilationUnit) throws JavaScriptModelException {
		if (compilationUnit == null)
			return null;
		
		if (!ActionUtil.isOnBuildPath(compilationUnit))
			return null;
		
		IType[] types= compilationUnit.getTypes();
		if (types.length > 1)
			return null;
		
		IStorage bundle= NLSHintHelper.getResourceBundle(compilationUnit);
		if (!(bundle instanceof IFile))
			return null;

		return new SearchPatternData(types[0], (IFile)bundle);
	}
	
	private SearchPatternData tryIfPropertyFileSelected(IFile file) throws JavaScriptModelException {
		if (!"properties".equalsIgnoreCase(file.getFileExtension())) //$NON-NLS-1$
			return null;
		
		IPath propertyFullPath= file.getFullPath();
		// Try to find a corresponding CU
		String[] javaExtensions= JavaScriptCore.getJavaScriptLikeExtensions();
		for (int i= 0; i < javaExtensions.length; i++) { 
			String extension= javaExtensions[i];
			IPath cuPath= propertyFullPath.removeFileExtension().addFileExtension(extension);
			IFile cuFile= (IFile)JavaScriptPlugin.getWorkspace().getRoot().findMember(cuPath);
			
			if (cuFile == null) { //try with uppercase first char
				String filename= cuPath.removeFileExtension().lastSegment();
				if (filename != null && filename.length() > 0) {
					filename= Character.toUpperCase(filename.charAt(0)) + filename.substring(1);
					IPath dirPath= propertyFullPath.removeLastSegments(1).addTrailingSeparator();
					cuPath= dirPath.append(filename).addFileExtension(extension);
					cuFile= (IFile)JavaScriptPlugin.getWorkspace().getRoot().findMember(cuPath);
				}
			}

			if (cuFile != null && cuFile.exists()) {
				IJavaScriptElement  element= JavaScriptCore.create(cuFile);
				if (element != null && element.exists() && element.getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT && ActionUtil.isOnBuildPath(element)) {
					IJavaScriptUnit compilationUnit= (IJavaScriptUnit)element;
					IType type= (compilationUnit).findPrimaryType();
					if (type != null) {
						String resourceBundleName= NLSHintHelper.getResourceBundleName(compilationUnit);
						if (resourceBundleName != null) {
							String resourceName= resourceBundleName + NLSRefactoring.PROPERTY_FILE_EXT;
							String name= file.getName();
							if (resourceName.endsWith(name)) {
								return new SearchPatternData(type, file);
							}
						}
					}
				}
			}
		}

		return null;
	}
	
	private static IJavaScriptUnit getCompilationUnit(JavaEditor editor) {
		IWorkingCopyManager manager= JavaScriptPlugin.getDefault().getWorkingCopyManager();
		IJavaScriptUnit cu= manager.getWorkingCopy(editor.getEditorInput());
		return cu;
	}

}
