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
package org.eclipse.wst.jsdt.internal.ui.browsing;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;

/**
 * Contains a list of package fragments with the same name
 * but residing in different source folders of a unique Java project.
 */
public class LogicalPackage extends PlatformObject {

	private Set fPackages;
	private String fName;
	private IJavaScriptProject fJavaProject;

	public LogicalPackage(IPackageFragment fragment){
		Assert.isNotNull(fragment);
		fPackages= new HashSet();
		fJavaProject= fragment.getJavaScriptProject();
		Assert.isNotNull(fJavaProject);
		add(fragment);
		fName= fragment.getElementName();
	}

	public IJavaScriptProject getJavaProject(){
		return fJavaProject;
	}

	public IPackageFragment[] getFragments(){
		return (IPackageFragment[]) fPackages.toArray(new IPackageFragment[fPackages.size()]);
	}

	public void add(IPackageFragment fragment){
		Assert.isTrue(fragment != null && fJavaProject.equals(fragment.getJavaScriptProject()));
		fPackages.add(fragment);
	}

	public void remove(IPackageFragment fragment){
		fPackages.remove(fragment);
	}

	public boolean contains(IPackageFragment fragment){
		return fPackages.contains(fragment);
	}

	public String getElementName(){
		return fName;
	}

	public int size(){
		return fPackages.size();
	}

	/**
	 * Returns true if the given fragment has the same name and
	 * resides inside the same project as the other fragments in
	 * the LogicalPackage.
	 *
	 * @param fragment
	 * @return boolean
	 */
	public boolean belongs(IPackageFragment fragment) {

		if(fragment==null)
			return false;

		if(fJavaProject.equals(fragment.getJavaScriptProject())){
			return fName.equals(fragment.getElementName());
		}

		return false;
	}
	
	public boolean hasSubpackages() throws JavaScriptModelException {
		for (Iterator iter= fPackages.iterator(); iter.hasNext();) {
			IPackageFragment pack= (IPackageFragment) iter.next();
			if (pack.hasSubpackages()) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isDefaultPackage() {
		return fName.length() == 0;
	}

	public boolean equals(Object o){
		if (!(o instanceof LogicalPackage))
			return false;

		LogicalPackage lp= (LogicalPackage)o;
		if (!fJavaProject.equals(lp.getJavaProject()))
			return false;

		IPackageFragment[] fragments= lp.getFragments();

		if (fragments.length != getFragments().length)
			return false;

		//this works because a LogicalPackage cannot contain the same IPackageFragment twice
		for (int i= 0; i < fragments.length; i++) {
			IPackageFragment fragment= fragments[i];
			if(!fPackages.contains(fragment))
				return false;
		}

		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		IPackageFragment[] fragments= getFragments();
		return fJavaProject.hashCode() + getHash(fragments, fragments.length-1);
	}

	private int getHash(IPackageFragment[] fragments, int index) {
		if (index <= 0)
			return fragments[0].hashCode() * 17;
		else return fragments[index].hashCode() * 17 + getHash(fragments, index-1);
	}
}
