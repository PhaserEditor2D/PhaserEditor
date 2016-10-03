/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.MethodOverrideTester;
import org.eclipse.wst.jsdt.internal.corext.util.SuperTypeHierarchyCache;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ImageImageDescriptor;

/**
 * LabelDecorator that decorates an method's image with override or implements overlays.
 * The viewer using this decorator is responsible for updating the images on element changes.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public class OverrideIndicatorLabelDecorator implements ILabelDecorator, ILightweightLabelDecorator {

	private ImageDescriptorRegistry fRegistry;
	private boolean fUseNewRegistry= false;

	/**
	 * Creates a decorator. The decorator creates an own image registry to cache
	 * images. 
	 */
	public OverrideIndicatorLabelDecorator() {
		this(null);
		fUseNewRegistry= true;
	}	

	/*
	 * Creates decorator with a shared image registry.
	 * 
	 * @param registry The registry to use or <code>null</code> to use the JavaScript plugin's
	 * image registry.
	 */	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param registry The registry to use.
	 */
	public OverrideIndicatorLabelDecorator(ImageDescriptorRegistry registry) {
		fRegistry= registry;
	}
	
	private ImageDescriptorRegistry getRegistry() {
		if (fRegistry == null) {
			fRegistry= fUseNewRegistry ? new ImageDescriptorRegistry() : JavaScriptPlugin.getImageDescriptorRegistry();
		}
		return fRegistry;
	}	
	
	
	/* (non-Javadoc)
	 * @see ILabelDecorator#decorateText(String, Object)
	 */
	public String decorateText(String text, Object element) {
		return text;
	}	

	/* (non-Javadoc)
	 * @see ILabelDecorator#decorateImage(Image, Object)
	 */
	public Image decorateImage(Image image, Object element) {
		int adornmentFlags= computeAdornmentFlags(element);
		if (adornmentFlags != 0) {
			ImageDescriptor baseImage= new ImageImageDescriptor(image);
			Rectangle bounds= image.getBounds();
			return getRegistry().get(new JavaScriptElementImageDescriptor(baseImage, adornmentFlags, new Point(bounds.width, bounds.height)));
		}
		return image;
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 * @param element The element to decorate
	 * @return Resulting decorations (combination of JavaScriptElementImageDescriptor.IMPLEMENTS
	 * and JavaScriptElementImageDescriptor.OVERRIDES)
	 */
	public int computeAdornmentFlags(Object element) {
		if (element instanceof IFunction) {
			try {
				IFunction method= (IFunction) element;
				if (!method.getJavaScriptProject().isOnIncludepath(method)) {
					return 0;
				}
				int flags= method.getFlags();
				if (!method.isConstructor() && !Flags.isPrivate(flags) && !Flags.isStatic(flags)) {
					int res= getOverrideIndicators(method);
					return res;
				}
			} catch (JavaScriptModelException e) {
				if (!e.isDoesNotExist()) {
					JavaScriptPlugin.log(e);
				}
			}
		}
		return 0;
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 * @param method The element to decorate
	 * @return Resulting decorations (combination of JavaScriptElementImageDescriptor.IMPLEMENTS
	 * and JavaScriptElementImageDescriptor.OVERRIDES)
	 * @throws JavaScriptModelException
	 */
	protected int getOverrideIndicators(IFunction method) throws JavaScriptModelException {
		JavaScriptUnit astRoot= JavaScriptPlugin.getDefault().getASTProvider().getAST((IJavaScriptElement) method.getOpenable(), ASTProvider.WAIT_ACTIVE_ONLY, null);
		if (astRoot != null) {
			int res= findInHierarchyWithAST(astRoot, method);
			if (res != -1) {
				return res;
			}
		}
		
		IType type= method.getDeclaringType();
		if (type==null)
			return 0;
		
		MethodOverrideTester methodOverrideTester= SuperTypeHierarchyCache.getMethodOverrideTester(type);
		IFunction defining= methodOverrideTester.findOverriddenMethod(method, true);
		if (defining != null) {
			if (JdtFlags.isAbstract(defining)) {
				return JavaScriptElementImageDescriptor.IMPLEMENTS;
			} else {
				return JavaScriptElementImageDescriptor.OVERRIDES;
			}
		}
		return 0;
	}
	
	private int findInHierarchyWithAST(JavaScriptUnit astRoot, IFunction method) throws JavaScriptModelException {
		ASTNode node= NodeFinder.perform(astRoot, method.getNameRange());
		if (node instanceof SimpleName && node.getParent() instanceof FunctionDeclaration) {
			IFunctionBinding binding= ((FunctionDeclaration) node.getParent()).resolveBinding();
			if (binding != null) {
				IFunctionBinding defining= Bindings.findOverriddenMethod(binding, true);
				if (defining != null) {
					if (JdtFlags.isAbstract(defining)) {
						return JavaScriptElementImageDescriptor.IMPLEMENTS;
					} else {
						return JavaScriptElementImageDescriptor.OVERRIDES;
					}
				}
				return 0;
			}
		}		
		return -1;
	}

	/* (non-Javadoc)
	 * @see IBaseLabelProvider#addListener(ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener) {
	}

	/* (non-Javadoc)
	 * @see IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		if (fRegistry != null && fUseNewRegistry) {
			fRegistry.dispose();
		}
	}

	/* (non-Javadoc)
	 * @see IBaseLabelProvider#isLabelProperty(Object, String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		return true;
	}

	/* (non-Javadoc)
	 * @see IBaseLabelProvider#removeListener(ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object, org.eclipse.jface.viewers.IDecoration)
	 */
	public void decorate(Object element, IDecoration decoration) { 
		if (!isShowingOverrideIndicators()) { 
			return;
		}

		int adornmentFlags= computeAdornmentFlags(element);
		if ((adornmentFlags & JavaScriptElementImageDescriptor.IMPLEMENTS) != 0) {
			if ((adornmentFlags & JavaScriptElementImageDescriptor.SYNCHRONIZED) != 0) {
				decoration.addOverlay(JavaPluginImages.DESC_OVR_SYNCH_AND_IMPLEMENTS);
			} else {
				decoration.addOverlay(JavaPluginImages.DESC_OVR_IMPLEMENTS);
			}
		} else if ((adornmentFlags & JavaScriptElementImageDescriptor.OVERRIDES) != 0) {
			if ((adornmentFlags & JavaScriptElementImageDescriptor.SYNCHRONIZED) != 0) {
				decoration.addOverlay(JavaPluginImages.DESC_OVR_SYNCH_AND_OVERRIDES);
			} else {
				decoration.addOverlay(JavaPluginImages.DESC_OVR_OVERRIDES);
			}
		}
	}

	private boolean isShowingOverrideIndicators() {
		 return PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_OVERRIDE_INDICATORS);
	}
}
