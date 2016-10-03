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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.internal.corext.SourceRange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;


public class ReorgUtils {

	//workaround for bug 18311
	private static final ISourceRange fgUnknownRange= new SourceRange(-1, 0);

	private ReorgUtils() {
	}

	public static boolean isArchiveMember(IJavaScriptElement[] elements) {
		for (int i= 0; i < elements.length; i++) {
			IJavaScriptElement element= elements[i];
			IPackageFragmentRoot root= (IPackageFragmentRoot)element.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
			if (root != null && root.isArchive())
				return true;
		}
		return false;
	}
	
	public static boolean containsOnlyProjects(List elements){
		if (elements.isEmpty())
			return false;
		for(Iterator iter= elements.iterator(); iter.hasNext(); ) {
			if (! isProject(iter.next()))
				return false;
		}
		return true;
	}
	
	public static boolean isProject(Object element){
		return (element instanceof IJavaScriptProject) || (element instanceof IProject);
	}

	public static boolean isInsideCompilationUnit(IJavaScriptElement element) {
		return 	!(element instanceof IJavaScriptUnit) && 
				hasAncestorOfType(element, IJavaScriptElement.JAVASCRIPT_UNIT);
	}
	
	public static boolean isInsideClassFile(IJavaScriptElement element) {
		return 	!(element instanceof IClassFile) && 
				hasAncestorOfType(element, IJavaScriptElement.CLASS_FILE);
	}
	
	public static boolean hasAncestorOfType(IJavaScriptElement element, int type){
		return element.getAncestor(type) != null;
	}
	
	/**
	 * May be <code>null</code>.
	 */
	public static IJavaScriptUnit getCompilationUnit(IJavaScriptElement javaElement){
		if (javaElement instanceof IJavaScriptUnit)
			return (IJavaScriptUnit) javaElement;
		return (IJavaScriptUnit) javaElement.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
	}

	/**
	 * some of the returned elements may be <code>null</code>.
	 */
	public static IJavaScriptUnit[] getCompilationUnits(IJavaScriptElement[] javaElements){
		IJavaScriptUnit[] result= new IJavaScriptUnit[javaElements.length];
		for (int i= 0; i < javaElements.length; i++) {
			result[i]= getCompilationUnit(javaElements[i]);
		}
		return result;
	}
		
	public static IResource getResource(IJavaScriptElement element){
		if (element instanceof IJavaScriptUnit)
			return ((IJavaScriptUnit)element).getPrimary().getResource();
		else
			return element.getResource();
	}
	
	public static IResource[] getResources(IJavaScriptElement[] elements) {
		IResource[] result= new IResource[elements.length];
		for (int i= 0; i < elements.length; i++) {
			result[i]= ReorgUtils.getResource(elements[i]);
		}
		return result;
	}
	
	public static String getName(IResource resource) {
		String pattern= createNamePattern(resource);
		String[] args= createNameArguments(resource);
		return Messages.format(pattern, args);
	}
	
	private static String createNamePattern(IResource resource) {
		switch(resource.getType()){
			case IResource.FILE:
				return RefactoringCoreMessages.ReorgUtils_0; 
			case IResource.FOLDER:
				return RefactoringCoreMessages.ReorgUtils_1; 
			case IResource.PROJECT:
				return RefactoringCoreMessages.ReorgUtils_2; 
			default:
				Assert.isTrue(false);
				return null;
		}
	}

	private static String[] createNameArguments(IResource resource) {
		return new String[]{resource.getName()};
	}

	public static String getName(IJavaScriptElement element) throws JavaScriptModelException {
		String pattern= createNamePattern(element);
		String[] args= createNameArguments(element);
		return Messages.format(pattern, args);
	}

	private static String[] createNameArguments(IJavaScriptElement element) throws JavaScriptModelException {
		switch(element.getElementType()){
			case IJavaScriptElement.CLASS_FILE:
				return new String[]{element.getElementName()};
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				return new String[]{element.getElementName()};
			case IJavaScriptElement.FIELD:
				return new String[]{element.getElementName()};
			case IJavaScriptElement.IMPORT_CONTAINER:
				return new String[0];
			case IJavaScriptElement.IMPORT_DECLARATION:
				return new String[]{element.getElementName()};
			case IJavaScriptElement.INITIALIZER:
				return new String[0];
			case IJavaScriptElement.JAVASCRIPT_PROJECT:
				return new String[]{element.getElementName()};
			case IJavaScriptElement.METHOD:
				return new String[]{element.getElementName()};
			case IJavaScriptElement.PACKAGE_FRAGMENT:
				return new String[]{element.getElementName()};
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
				return new String[]{element.getElementName()};
			case IJavaScriptElement.TYPE:
				IType type= (IType)element;
				String name= type.getElementName();
				if (name.length() == 0 && type.isAnonymous()) {
					String superclassName= Signature.getSimpleName(type.getSuperclassName());
					return new String[]{Messages.format(RefactoringCoreMessages.ReorgUtils_19, superclassName)}; 
				}
				return new String[]{element.getElementName()};
			default:
				Assert.isTrue(false);
				return null;
		}
	}

	private static String createNamePattern(IJavaScriptElement element) throws JavaScriptModelException {
		switch(element.getElementType()){
			case IJavaScriptElement.CLASS_FILE:
				return RefactoringCoreMessages.ReorgUtils_3; 
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				return RefactoringCoreMessages.ReorgUtils_4; 
			case IJavaScriptElement.FIELD:
				return RefactoringCoreMessages.ReorgUtils_5; 
			case IJavaScriptElement.IMPORT_CONTAINER:
				return RefactoringCoreMessages.ReorgUtils_6; 
			case IJavaScriptElement.IMPORT_DECLARATION:
				return RefactoringCoreMessages.ReorgUtils_7; 
			case IJavaScriptElement.INITIALIZER:
				return RefactoringCoreMessages.ReorgUtils_8; 
			case IJavaScriptElement.JAVASCRIPT_PROJECT:
				return RefactoringCoreMessages.ReorgUtils_9; 
			case IJavaScriptElement.METHOD:
				if (((IFunction)element).isConstructor())
					return RefactoringCoreMessages.ReorgUtils_10; 
				else
					return RefactoringCoreMessages.ReorgUtils_11; 
			case IJavaScriptElement.PACKAGE_FRAGMENT:
				if (JavaElementUtil.isDefaultPackage(element))
					return RefactoringCoreMessages.ReorgUtils_13; 
				else
					return RefactoringCoreMessages.ReorgUtils_14; 
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
				if (isSourceFolder(element))
					return RefactoringCoreMessages.ReorgUtils_15; 
				if (isClassFolder(element))
					return RefactoringCoreMessages.ReorgUtils_16; 
				return RefactoringCoreMessages.ReorgUtils_17; 
			case IJavaScriptElement.TYPE:
				IType type= (IType)element;
				if (type.getElementName().length() == 0 && type.isAnonymous())
					return RefactoringCoreMessages.ReorgUtils_20; 
				return RefactoringCoreMessages.ReorgUtils_18; 
			default:
				Assert.isTrue(false);
				return null;
		}
	}

	public static IResource[] getResources(List elements) {
		List resources= new ArrayList(elements.size());
		for (Iterator iter= elements.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (element instanceof IResource)
				resources.add(element);
		}
		return (IResource[]) resources.toArray(new IResource[resources.size()]);
	}

	public static IJavaScriptElement[] getJavaElements(List elements) {
		List resources= new ArrayList(elements.size());
		for (Iterator iter= elements.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (element instanceof IJavaScriptElement)
				resources.add(element);
		}
		return (IJavaScriptElement[]) resources.toArray(new IJavaScriptElement[resources.size()]);
	}
	
	public static IWorkingSet[] getWorkingSets(List elements) {
		List result= new ArrayList(1);
		for (Iterator iter= elements.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (element instanceof IWorkingSet) {
				result.add(element);
			}
		}
		return (IWorkingSet[])result.toArray(new IWorkingSet[result.size()]);
	}

	public static boolean hasSourceAvailable(IMember member) throws JavaScriptModelException{
		return ! member.isBinary() || 
				(member.getSourceRange() != null && ! fgUnknownRange.equals(member.getSourceRange()));
	}
	
	public static IResource[] setMinus(IResource[] setToRemoveFrom, IResource[] elementsToRemove) {
		Set setMinus= new HashSet(setToRemoveFrom.length - setToRemoveFrom.length);
		setMinus.addAll(Arrays.asList(setToRemoveFrom));
		setMinus.removeAll(Arrays.asList(elementsToRemove));
		return (IResource[]) setMinus.toArray(new IResource[setMinus.size()]);		
	}

	public static IJavaScriptElement[] setMinus(IJavaScriptElement[] setToRemoveFrom, IJavaScriptElement[] elementsToRemove) {
		Set setMinus= new HashSet(setToRemoveFrom.length - setToRemoveFrom.length);
		setMinus.addAll(Arrays.asList(setToRemoveFrom));
		setMinus.removeAll(Arrays.asList(elementsToRemove));
		return (IJavaScriptElement[]) setMinus.toArray(new IJavaScriptElement[setMinus.size()]);		
	}
	
	public static IJavaScriptElement[] union(IJavaScriptElement[] set1, IJavaScriptElement[] set2) {
		List union= new ArrayList(set1.length + set2.length);//use lists to avoid sequence problems
		addAll(set1, union);
		addAll(set2, union);
		return (IJavaScriptElement[]) union.toArray(new IJavaScriptElement[union.size()]);
	}	

	public static IResource[] union(IResource[] set1, IResource[] set2) {
		List union= new ArrayList(set1.length + set2.length);//use lists to avoid sequence problems
		addAll(ReorgUtils.getNotNulls(set1), union);
		addAll(ReorgUtils.getNotNulls(set2), union);
		return (IResource[]) union.toArray(new IResource[union.size()]);
	}	

	private static void addAll(Object[] array, List list) {
		for (int i= 0; i < array.length; i++) {
			if (! list.contains(array[i]))
				list.add(array[i]);
		}
	}

	public static Set union(Set set1, Set set2){
		Set union= new HashSet(set1.size() + set2.size());
		union.addAll(set1);
		union.addAll(set2);
		return union;
	}

	public static IType[] getMainTypes(IJavaScriptElement[] javaElements) throws JavaScriptModelException {
		List result= new ArrayList();
		for (int i= 0; i < javaElements.length; i++) {
			IJavaScriptElement element= javaElements[i];
			if (element instanceof IType && JavaElementUtil.isMainType((IType)element))
				result.add(element);
		}
		return (IType[]) result.toArray(new IType[result.size()]);
	}
	
	public static IFolder[] getFolders(IResource[] resources) {
		Set result= getResourcesOfType(resources, IResource.FOLDER);
		return (IFolder[]) result.toArray(new IFolder[result.size()]);
	}

	public static IFile[] getFiles(IResource[] resources) {
		Set result= getResourcesOfType(resources, IResource.FILE);
		return (IFile[]) result.toArray(new IFile[result.size()]);
	}
		
	//the result can be cast down to the requested type array
	public static Set getResourcesOfType(IResource[] resources, int typeMask){
		Set result= new HashSet(resources.length);
		for (int i= 0; i < resources.length; i++) {
			if (isOfType(resources[i], typeMask))
				result.add(resources[i]);
		}
		return result;
	}
	
	//the result can be cast down to the requested type array
	//type is _not_ a mask	
	public static List getElementsOfType(IJavaScriptElement[] javaElements, int type){
		List result= new ArrayList(javaElements.length);
		for (int i= 0; i < javaElements.length; i++) {
			if (isOfType(javaElements[i], type))
				result.add(javaElements[i]);
		}
		return result;
	}

	public static boolean hasElementsNotOfType(IResource[] resources, int typeMask) {
		for (int i= 0; i < resources.length; i++) {
			IResource resource= resources[i];
			if (resource != null && ! isOfType(resource, typeMask))
				return true;
		}
		return false;
	}

	//type is _not_ a mask	
	public static boolean hasElementsNotOfType(IJavaScriptElement[] javaElements, int type) {
		for (int i= 0; i < javaElements.length; i++) {
			IJavaScriptElement element= javaElements[i];
			if (element != null && ! isOfType(element, type))
				return true;
		}
		return false;
	}
	
	//type is _not_ a mask	
	public static boolean hasElementsOfType(IJavaScriptElement[] javaElements, int type) {
		for (int i= 0; i < javaElements.length; i++) {
			IJavaScriptElement element= javaElements[i];
			if (element != null && isOfType(element, type))
				return true;
		}
		return false;
	}

	public static boolean hasElementsOfType(IJavaScriptElement[] javaElements, int[] types) {
		for (int i= 0; i < types.length; i++) {
			if (hasElementsOfType(javaElements, types[i])) return true;
		}
		return false;
	}

	public static boolean hasElementsOfType(IResource[] resources, int typeMask) {
		for (int i= 0; i < resources.length; i++) {
			IResource resource= resources[i];
			if (resource != null && isOfType(resource, typeMask))
				return true;
		}
		return false;
	}

	private static boolean isOfType(IJavaScriptElement element, int type) {
		return element.getElementType() == type;//this is _not_ a mask
	}
		
	private static boolean isOfType(IResource resource, int type) {
		return resource != null && isFlagSet(resource.getType(), type);
	}
		
	private static boolean isFlagSet(int flags, int flag){
		return (flags & flag) != 0;
	}

	public static boolean isSourceFolder(IJavaScriptElement javaElement) throws JavaScriptModelException {
		return (javaElement instanceof IPackageFragmentRoot) &&
				((IPackageFragmentRoot)javaElement).getKind() == IPackageFragmentRoot.K_SOURCE;
	}
	
	public static boolean isClassFolder(IJavaScriptElement javaElement) throws JavaScriptModelException {
		return (javaElement instanceof IPackageFragmentRoot) &&
				((IPackageFragmentRoot)javaElement).getKind() == IPackageFragmentRoot.K_BINARY;
	}
	
	public static boolean isPackageFragmentRoot(IJavaScriptProject javaProject) throws JavaScriptModelException{
		return getCorrespondingPackageFragmentRoot(javaProject) != null;
	}
	
	private static boolean isPackageFragmentRootCorrespondingToProject(IPackageFragmentRoot root) {
		return root.getResource() instanceof IProject;
	}

	public static IPackageFragmentRoot getCorrespondingPackageFragmentRoot(IJavaScriptProject p) throws JavaScriptModelException {
		IPackageFragmentRoot[] roots= p.getPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			if (isPackageFragmentRootCorrespondingToProject(roots[i]))
				return roots[i];
		}
		return null;
	}
		
	public static boolean containsLinkedResources(IResource[] resources){
		for (int i= 0; i < resources.length; i++) {
			if (resources[i] != null && resources[i].isLinked()) return true;
		}
		return false;
	}
	
	public static boolean containsLinkedResources(IJavaScriptElement[] javaElements){
		for (int i= 0; i < javaElements.length; i++) {
			IResource res= getResource(javaElements[i]);
			if (res != null && res.isLinked()) return true;
		}
		return false;
	}

	public static boolean canBeDestinationForLinkedResources(IResource resource) {
		return resource.isAccessible() && resource instanceof IProject;
	}

	public static boolean canBeDestinationForLinkedResources(IJavaScriptElement javaElement) {
		if (javaElement instanceof IPackageFragmentRoot){
			return isPackageFragmentRootCorrespondingToProject((IPackageFragmentRoot)javaElement);
		} else if (javaElement instanceof IJavaScriptProject){
			return true;//XXX ???
		} else return false;
	}
	
	public static boolean isParentInWorkspaceOrOnDisk(IPackageFragment pack, IPackageFragmentRoot root){
		if (pack == null)
			return false;		
		IJavaScriptElement packParent= pack.getParent();
		if (packParent == null)
			return false;		
		if (packParent.equals(root))	
			return true;
		IResource packageResource= ResourceUtil.getResource(pack);
		IResource packageRootResource= ResourceUtil.getResource(root);
		return isParentInWorkspaceOrOnDisk(packageResource, packageRootResource);
	}

	public static boolean isParentInWorkspaceOrOnDisk(IPackageFragmentRoot root, IJavaScriptProject javaProject){
		if (root == null)
			return false;		
		IJavaScriptElement rootParent= root.getParent();
		if (rootParent == null)
			return false;		
		if (rootParent.equals(root))	
			return true;
		IResource packageResource= ResourceUtil.getResource(root);
		IResource packageRootResource= ResourceUtil.getResource(javaProject);
		return isParentInWorkspaceOrOnDisk(packageResource, packageRootResource);
	}

	public static boolean isParentInWorkspaceOrOnDisk(IJavaScriptUnit cu, IPackageFragment dest){
		if (cu == null)
			return false;
		IJavaScriptElement cuParent= cu.getParent();
		if (cuParent == null)
			return false;
		if (cuParent.equals(dest))	
			return true;
		IResource cuResource= cu.getResource();
		IResource packageResource= ResourceUtil.getResource(dest);
		return isParentInWorkspaceOrOnDisk(cuResource, packageResource);
	}
	
	public static boolean isParentInWorkspaceOrOnDisk(IResource res, IResource maybeParent){
		if (res == null)
			return false;
		return areEqualInWorkspaceOrOnDisk(res.getParent(), maybeParent);
	}
	
	public static boolean areEqualInWorkspaceOrOnDisk(IResource r1, IResource r2){
		if (r1 == null || r2 == null)
			return false;
		if (r1.equals(r2))
			return true;
		URI r1Location= r1.getLocationURI();
		URI r2Location= r2.getLocationURI();
		if (r1Location == null || r2Location == null)
			return false;
		return r1Location.equals(r2Location);
	}
	
	public static IResource[] getNotNulls(IResource[] resources) {
		Collection result= new ArrayList(resources.length);
		for (int i= 0; i < resources.length; i++) {
			IResource resource= resources[i];
			if (resource != null && ! result.contains(resource))
				result.add(resource);
		}
		return (IResource[]) result.toArray(new IResource[result.size()]);
	}
	
	public static IResource[] getNotLinked(IResource[] resources) {
		Collection result= new ArrayList(resources.length);
		for (int i= 0; i < resources.length; i++) {
			IResource resource= resources[i];
			if (resource != null && ! result.contains(resource) && ! resource.isLinked())
				result.add(resource);
		}
		return (IResource[]) result.toArray(new IResource[result.size()]);
	}
	
	/* List<IJavaScriptElement> javaElements
	 * return IJavaScriptUnit -> List<IJavaScriptElement>
	 */
	public static Map groupByCompilationUnit(List javaElements){
		Map result= new HashMap();
		for (Iterator iter= javaElements.iterator(); iter.hasNext();) {
			IJavaScriptElement element= (IJavaScriptElement) iter.next();
			IJavaScriptUnit cu= ReorgUtils.getCompilationUnit(element);
			if (cu != null){
				if (! result.containsKey(cu))
					result.put(cu, new ArrayList(1));
				((List)result.get(cu)).add(element);
			}
		}
		return result;
	}
	
	public static void splitIntoJavaElementsAndResources(Object[] elements, List javaElementResult, List resourceResult) {
		for (int i= 0; i < elements.length; i++) {
			Object element= elements[i];
			if (element instanceof IJavaScriptElement) {
				javaElementResult.add(element);
			} else if (element instanceof IResource) {
				IResource resource= (IResource)element;
				IJavaScriptElement jElement= JavaScriptCore.create(resource);
				if (jElement != null && jElement.exists())
					javaElementResult.add(jElement);
				else
					resourceResult.add(resource);
			}
		}
	}

	public static boolean containsElementOrParent(Set elements, IJavaScriptElement element) {
		IJavaScriptElement curr= element;
		do {
			if (elements.contains(curr))
				return true;
			curr= curr.getParent();
		} while (curr != null);
		return false;
	}

	public static boolean containsElementOrParent(Set elements, IResource element) {
		IResource curr= element;
		do {
			if (elements.contains(curr))
				return true;
			IJavaScriptElement jElement= JavaScriptCore.create(curr);
			if (jElement != null && jElement.exists()) {
				return containsElementOrParent(elements, jElement);
			}
			curr= curr.getParent();
		} while (curr != null);
		return false;
	}
}
