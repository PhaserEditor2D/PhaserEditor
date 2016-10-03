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
package org.eclipse.wst.jsdt.internal.corext.refactoring.reorg;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptCore;


public class ParentChecker {
	private IResource[] fResources;
	private IJavaScriptElement[] fJavaElements;

	public ParentChecker(IResource[] resources, IJavaScriptElement[] javaElements) {
		Assert.isNotNull(resources);
		Assert.isNotNull(javaElements);
		fResources= resources;
		fJavaElements= javaElements;
	}
	
	public boolean haveCommonParent() {
		return getCommonParent() != null;
	}
	
	public Object getCommonParent(){
		if (fJavaElements.length == 0 && fResources.length == 0)
			return null;
		if (! resourcesHaveCommonParent() || ! javaElementsHaveCommonParent())
			return null;
		if (fJavaElements.length == 0){
			IResource commonResourceParent= getCommonResourceParent();
			Assert.isNotNull(commonResourceParent);
			IJavaScriptElement convertedToJava= JavaScriptCore.create(commonResourceParent);
			if (convertedToJava != null && convertedToJava.exists())
				return convertedToJava;
			else
				return commonResourceParent;
		}
		if (fResources.length == 0)
			return getCommonJavaElementParent();
			
		IResource commonResourceParent= getCommonResourceParent();
		IJavaScriptElement commonJavaElementParent= getCommonJavaElementParent();
		Assert.isNotNull(commonJavaElementParent);
		Assert.isNotNull(commonResourceParent);
		IJavaScriptElement convertedToJava= JavaScriptCore.create(commonResourceParent);
		if (convertedToJava == null || 
			! convertedToJava.exists() || 
			! commonJavaElementParent.equals(convertedToJava))
			return null;
		return commonJavaElementParent;	
	}

	private IJavaScriptElement getCommonJavaElementParent() {
		Assert.isNotNull(fJavaElements);
		Assert.isTrue(fJavaElements.length > 0);//safe - checked before
		return fJavaElements[0].getParent();
	}

	private IResource getCommonResourceParent() {
		Assert.isNotNull(fResources);
		Assert.isTrue(fResources.length > 0);//safe - checked before
		return fResources[0].getParent();
	}

	private boolean javaElementsHaveCommonParent() {
		if (fJavaElements.length == 0)
			return true;
		IJavaScriptElement firstParent= fJavaElements[0].getParent();
		Assert.isNotNull(firstParent); //this should never happen			
		for (int i= 1; i < fJavaElements.length; i++) {
			if (! firstParent.equals(fJavaElements[i].getParent()))
				return false;
		}
		return true;
	}

	private boolean resourcesHaveCommonParent() {
		if (fResources.length == 0)
			return true;
		IResource firstParent= fResources[0].getParent();
		Assert.isNotNull(firstParent);
		for (int i= 1; i < fResources.length; i++) {
			if (! firstParent.equals(fResources[i].getParent()))
				return false;
		}
		return true;
	}
	
	public IResource[] getResources(){
		return fResources;
	}		
		
	public IJavaScriptElement[] getJavaElements(){
		return fJavaElements;
	}

	public void removeElementsWithAncestorsOnList(boolean removeOnlyJavaElements) {
		if (! removeOnlyJavaElements){
			removeResourcesDescendantsOfResources();
			removeResourcesDescendantsOfJavaElements();
		}
		removeJavaElementsDescendantsOfJavaElements();
//		removeJavaElementsChildrenOfResources(); //this case is covered by removeUnconfirmedArchives
	}
				
	private void removeResourcesDescendantsOfJavaElements() {
		List subResources= new ArrayList(3);
		for (int i= 0; i < fResources.length; i++) {
			IResource subResource= fResources[i];
			for (int j= 0; j < fJavaElements.length; j++) {
				IJavaScriptElement superElements= fJavaElements[j];
				if (isDescendantOf(subResource, superElements))
					subResources.add(subResource);
			}
		}
		removeFromSetToDelete((IResource[]) subResources.toArray(new IResource[subResources.size()]));
	}

	private void removeJavaElementsDescendantsOfJavaElements() {
		List subElements= new ArrayList(3);
		for (int i= 0; i < fJavaElements.length; i++) {
			IJavaScriptElement subElement= fJavaElements[i];
			for (int j= 0; j < fJavaElements.length; j++) {
				IJavaScriptElement superElement= fJavaElements[j];
				if (isDescendantOf(subElement, superElement))
					subElements.add(subElement);
			}
		}
		removeFromSetToDelete((IJavaScriptElement[]) subElements.toArray(new IJavaScriptElement[subElements.size()]));
	}

	private void removeResourcesDescendantsOfResources() {
		List subResources= new ArrayList(3);
		for (int i= 0; i < fResources.length; i++) {
			IResource subResource= fResources[i];
			for (int j= 0; j < fResources.length; j++) {
				IResource superResource= fResources[j];
				if (isDescendantOf(subResource, superResource))
					subResources.add(subResource);
			}
		}
		removeFromSetToDelete((IResource[]) subResources.toArray(new IResource[subResources.size()]));
	}

	public static boolean isDescendantOf(IResource subResource, IJavaScriptElement superElement) {
		IResource parent= subResource.getParent();
		while(parent != null){
			IJavaScriptElement el= JavaScriptCore.create(parent);
			if (el != null && el.exists() && el.equals(superElement))
				return true;
			parent= parent.getParent();
		}
		return false;
	}

	public static boolean isDescendantOf(IJavaScriptElement subElement, IJavaScriptElement superElement) {
		if (subElement.equals(superElement))
			return false;
		IJavaScriptElement parent= subElement.getParent();
		while(parent != null){
			if (parent.equals(superElement))
				return true;
			parent= parent.getParent();
		}
		return false;
	}

	public static boolean isDescendantOf(IResource subResource, IResource superResource) {
		return ! subResource.equals(superResource) && superResource.getFullPath().isPrefixOf(subResource.getFullPath());
	}

	private void removeFromSetToDelete(IResource[] resourcesToNotDelete) {
		fResources= ReorgUtils.setMinus(fResources, resourcesToNotDelete);
	}
	
	private void removeFromSetToDelete(IJavaScriptElement[] elementsToNotDelete) {
		fJavaElements= ReorgUtils.setMinus(fJavaElements, elementsToNotDelete);
	}
}
