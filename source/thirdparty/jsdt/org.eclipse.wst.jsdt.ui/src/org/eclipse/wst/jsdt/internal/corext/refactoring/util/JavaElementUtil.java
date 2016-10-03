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
package org.eclipse.wst.jsdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.internal.corext.SourceRange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;

public class JavaElementUtil {
	
	//no instances
	private JavaElementUtil(){
	}
	
	public static String createMethodSignature(IFunction method){
		try {
			return Signature.toString(method.getSignature(), method.getElementName(), method.getParameterNames(), false, ! method.isConstructor());
		} catch(JavaScriptModelException e) {
			return method.getElementName(); //fallback
		}
	}
	
	public static String createFieldSignature(IField field){
		return JavaModelUtil.getFullyQualifiedName(field.getDeclaringType()) + "." + field.getElementName(); //$NON-NLS-1$
	}
	
	public static String createSignature(IMember member){
		switch (member.getElementType()){
			case IJavaScriptElement.FIELD:
				return createFieldSignature((IField)member);
			case IJavaScriptElement.TYPE:
				return JavaModelUtil.getFullyQualifiedName(((IType)member));
			case IJavaScriptElement.INITIALIZER:
				return RefactoringCoreMessages.JavaElementUtil_initializer; 
			case IJavaScriptElement.METHOD:
				return createMethodSignature((IFunction)member);				
			default:
				Assert.isTrue(false);
				return null;	
		}
	}
	
	public static IJavaScriptElement[] getElementsOfType(IJavaScriptElement[] elements, int type){
		Set result= new HashSet(elements.length);
		for (int i= 0; i < elements.length; i++) {
			IJavaScriptElement element= elements[i];
			if (element.getElementType() == type)
				result.add(element);
		}
		return (IJavaScriptElement[]) result.toArray(new IJavaScriptElement[result.size()]);
	}

	public static IType getMainType(IJavaScriptUnit cu) throws JavaScriptModelException{
		IType[] types= cu.getTypes();
		for (int i = 0; i < types.length; i++) {
			if (isMainType(types[i]))
				return types[i];
		}
		return null;
	}
	
	public static boolean isMainType(IType type) throws JavaScriptModelException{
		if (! type.exists())	
			return false;

		if (type.isBinary())
			return false;
			
		if (type.getJavaScriptUnit() == null)
			return false;
		
		if (type.getDeclaringType() != null)
			return false;
		
		return isPrimaryType(type) || isCuOnlyType(type);
	}


	private static boolean isPrimaryType(IType type){
		return type.equals(type.getJavaScriptUnit().findPrimaryType());
	}


	private static boolean isCuOnlyType(IType type) throws JavaScriptModelException{
		return type.getJavaScriptUnit().getTypes().length == 1;
	}

	/** see org.eclipse.wst.jsdt.internal.core.JavaElement#isAncestorOf(org.eclipse.wst.jsdt.core.IJavaScriptElement) */
	public static boolean isAncestorOf(IJavaScriptElement ancestor, IJavaScriptElement child) {
		IJavaScriptElement parent= child.getParent();
		while (parent != null && !parent.equals(ancestor)) {
			parent= parent.getParent();
		}
		return parent != null;
	}
	
	public static IFunction[] getAllConstructors(IType type) throws JavaScriptModelException {
		List result= new ArrayList();
		IFunction[] methods= type.getFunctions();
		for (int i= 0; i < methods.length; i++) {
			IFunction iMethod= methods[i];
			if (iMethod.isConstructor())
				result.add(iMethod);
		}
		return (IFunction[]) result.toArray(new IFunction[result.size()]);
	}

	/**
	 * Returns an array of projects that have the specified root on their
	 * classpaths.
	 */
	public static IJavaScriptProject[] getReferencingProjects(IPackageFragmentRoot root) throws JavaScriptModelException {
		IIncludePathEntry cpe= root.getRawIncludepathEntry();
		IJavaScriptProject[] allJavaProjects= JavaScriptCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaScriptProjects();
		List result= new ArrayList(allJavaProjects.length);
		for (int i= 0; i < allJavaProjects.length; i++) {
			IJavaScriptProject project= allJavaProjects[i];
			IPackageFragmentRoot[] roots= project.findPackageFragmentRoots(cpe);
			if (roots.length > 0)
				result.add(project);
		}
		return (IJavaScriptProject[]) result.toArray(new IJavaScriptProject[result.size()]);
	}	
	
	public static IMember[] merge(IMember[] a1, IMember[] a2) {
		// Don't use hash sets since ordering is important for some refactorings.
		List result= new ArrayList(a1.length + a2.length);
		for (int i= 0; i < a1.length; i++) {
			IMember member= a1[i];
			if (!result.contains(member))
				result.add(member);
		}
		for (int i= 0; i < a2.length; i++) {
			IMember member= a2[i];
			if (!result.contains(member))
				result.add(member);
		}
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}

	public static boolean isDefaultPackage(Object element) {
		return (element instanceof IPackageFragment) && ((IPackageFragment)element).isDefaultPackage();
	}
	
	/**
	 * @param pack a package fragment
	 * @return an array containing the given package and all subpackages 
	 * @throws JavaScriptModelException 
	 */
	public static IPackageFragment[] getPackageAndSubpackages(IPackageFragment pack) throws JavaScriptModelException {
		if (pack.isDefaultPackage())
			return new IPackageFragment[] { pack };
		
		IPackageFragmentRoot root= (IPackageFragmentRoot) pack.getParent();
		IJavaScriptElement[] allPackages= root.getChildren();
		ArrayList subpackages= new ArrayList();
		subpackages.add(pack);
		String prefix= pack.getElementName() + '.';
		for (int i= 0; i < allPackages.length; i++) {
			IPackageFragment currentPackage= (IPackageFragment) allPackages[i];
			if (currentPackage.getElementName().startsWith(prefix))
				subpackages.add(currentPackage);
		}
		return (IPackageFragment[]) subpackages.toArray(new IPackageFragment[subpackages.size()]);
	}
	
	/**
	 * @param pack the package fragment; may not be null
	 * @return the parent package fragment, or null if the given package fragment is the default package or a top level package
	 */
	public static IPackageFragment getParentSubpackage(IPackageFragment pack) {
		if (pack.isDefaultPackage())
			return null;
		
		final int index= pack.getElementName().lastIndexOf('.');
		if (index == -1)
			return null;

		final IPackageFragmentRoot root= (IPackageFragmentRoot) pack.getParent();
		final String newPackageName= pack.getElementName().substring(0, index);
		final IPackageFragment parent= root.getPackageFragment(newPackageName);
		if (parent.exists())
			return parent;
		else
			return null;
	}
	
	public static IMember[] sortByOffset(IMember[] members){
		Comparator comparator= new Comparator(){
			public int compare(Object o1, Object o2){
				try{
					return ((IMember) o1).getNameRange().getOffset() - ((IMember) o2).getNameRange().getOffset();
				} catch (JavaScriptModelException e){
					return 0;
				}	
			}
		};
		Arrays.sort(members, comparator);
		return members;
	}
	
	public static boolean isSourceAvailable(ISourceReference sourceReference) {
		try {
			return SourceRange.isAvailable(sourceReference.getSourceRange());
		} catch (JavaScriptModelException e) {
			return false;
		}
	}
}
