/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.navigator.deferred;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.packageview.PackageFragmentRootContainer;
import org.eclipse.wst.jsdt.internal.ui.packageview.PackagesMessages;

public class LoadingModelNode {
	
	private static ImageDescriptor loadingOne;
	private static ImageDescriptor loadingTwo;
	private static ImageDescriptor loadingThree;
	private static ImageDescriptor loadingFour;
	
	private static final Set loadingFiles = new HashSet();
	private static final Map placeHolders = new HashMap();

	static {
		try {
			loadingOne = JavaPluginImages.DESC_TOOL_LOADING_1;
			loadingTwo = JavaPluginImages.DESC_TOOL_LOADING_2;
			loadingThree = JavaPluginImages.DESC_TOOL_LOADING_3;
			loadingFour = JavaPluginImages.DESC_TOOL_LOADING_4;
		} catch (RuntimeException e) {
			loadingOne = ImageDescriptor.getMissingImageDescriptor();
			loadingTwo = ImageDescriptor.getMissingImageDescriptor();
			loadingThree = ImageDescriptor.getMissingImageDescriptor();
			loadingFour = ImageDescriptor.getMissingImageDescriptor();
		}
	}
 
	private String text; 
	private String text1;
	private String text2;
	private String text3;
	private int count = 0; 
	private boolean disposed = false;
	private PackageFragmentRootContainer packageFragmentRootContainer;
	
	/**
	 * Return a place holder node to sit in the tree until data is available.
	 * This place holder node will be animated for the user's enjoyment. 
	 *  
	 * @param modelFile The modelFile to be loaded
	 * @return A new unique place holder for this file for a given load cycle
	 */
	public static LoadingModelNode createPlaceHolder(PackageFragmentRootContainer packageFragmentRootContainer) {
		LoadingModelNode node = null;
		synchronized (LoadingModelNode.class) {
			if(placeHolders.containsKey(packageFragmentRootContainer))
				node = (LoadingModelNode) placeHolders.get(packageFragmentRootContainer);
			else 			
				placeHolders.put(packageFragmentRootContainer, node = new LoadingModelNode(packageFragmentRootContainer));
		}
		return node;
	}
	
	public LoadingModelNode(PackageFragmentRootContainer packageFragmentRootContainer) {
		text = PackagesMessages.LoadingJavaScriptNode;
		text1 = text  + "."; //$NON-NLS-1$
		text2 = text  + ".."; //$NON-NLS-1$
		text3 = text  + "..."; //$NON-NLS-1$
		this.packageFragmentRootContainer = packageFragmentRootContainer;
	}

	public String getText() {

		switch ( count % 4) {
			case 0 :
				return text;
			case 1 :
				return text1;
			case 2 :
				return text2;
			case 3 :
			default :
				return text3;
		} 
	}

	public Image getImage() {
		switch ( count = (++count % 4)) {
			case 0 :
				return loadingOne.createImage();
			case 1 :
				return loadingTwo.createImage();
			case 2 :
				return loadingThree.createImage();
			case 3 :
			default :
				return loadingFour.createImage();
		}
	}  
	
	public boolean isDisposed() {
		return disposed;
	}
	
	public void dispose() {
		synchronized (LoadingModelNode.class) {
			disposed = true;
			placeHolders.remove(packageFragmentRootContainer);	
			loadingFiles.remove(packageFragmentRootContainer);
		}
	}

	/**
	 * Employ a Test and Set (TST) primitive to ensure 
	 * that only job is spawned to load the model file
	 *   
	 * 
	 * @return True only if no other jobs are trying to load this model.
	 */
	public static boolean canBeginLoading(PackageFragmentRootContainer packageFragmentRootContainer) {
		synchronized (LoadingModelNode.class) {  
			if(loadingFiles.contains(packageFragmentRootContainer))
				return false;
			loadingFiles.add(packageFragmentRootContainer);
			return true;
		}

	}

	/**
	 * Return true if a job has requested permission to load the model file   
	 * @param modelFile The model file that should be loaded
	 * @return True only if a job is trying to load the model.
	 */
	public static boolean isBeingLoaded(PackageFragmentRootContainer packageFragmentRootContainer) {
		synchronized (LoadingModelNode.class) {  
			return loadingFiles.contains(packageFragmentRootContainer);
		}
	}
	
	
	public int hashCode() {	
		return packageFragmentRootContainer.hashCode();
	}
	  
	public boolean equals(Object o) {
		if(o instanceof LoadingModelNode) {
			return packageFragmentRootContainer.equals( ((LoadingModelNode)o).packageFragmentRootContainer );
		}
		return false; 
	}

	public String toString() {
		return "LoadingModelNode for " + packageFragmentRootContainer.getLabel(); //$NON-NLS-1$
	}

}
