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
package org.eclipse.wst.jsdt.ui;


import org.eclipse.ui.IViewPart;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IType;

/**
 * The standard type hierarchy view presents a type hierarchy for a given input class
 * or interface. Visually, this view consists of a pair of viewers, one showing the type
 * hierarchy, the other showing the members of the type selected in the first.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * @see JavaScriptUI#ID_TYPE_HIERARCHY

 *

 */
public interface ITypeHierarchyViewPart extends IViewPart {
	
	/**
	 * Constant used for the vertical view layout.
	 * 
	 */
	public static final int VIEW_LAYOUT_VERTICAL= 0;
	
	/**
	 * Constant used for the horizontal view layout.
	 * 
	 */
	public static final int VIEW_LAYOUT_HORIZONTAL= 1;
	
	/**
	 * Constant used for the single view layout (no members view)
	 * 
	 */
	public static final int VIEW_LAYOUT_SINGLE= 2;
	
	/**
	 * Constant used for the automatic view layout.
	 * 
	 */
	public static final int VIEW_LAYOUT_AUTOMATIC= 3;
	
	/**
	 * Constant used for the 'classic' type hierarchy mode.
	 * 
	 */
	public static final int HIERARCHY_MODE_CLASSIC= 2;
	
	/**
	 * Constant used for the super types hierarchy mode.
	 * 
	 */
	public static final int HIERARCHY_MODE_SUPERTYPES= 0;
	
	/**
	 * Constant used for the sub types hierarchy mode.
	 * 
	 */
	public static final int HIERARCHY_MODE_SUBTYPES= 1;

	/**
	 * Sets the input element of this type hierarchy view to a type.
	 *
	 * @param type the input element of this type hierarchy view, or <code>null</code>
	 *  to clear any input element
	 * @deprecated use setInputElement instead
	 */
	public void setInput(IType type);
	
	/**
	 * Sets the input element of this type hierarchy view. The following input types are possible
	 * <code>IMember</code> (types, methods, fields..), <code>IPackageFragment</code>, <code>IPackageFragmentRoot</code>
	 * and <code>IJavaScriptProject</code>.
	 *
	 * @param element the input element of this type hierarchy view, or <code>null</code>
	 *  to clear any input
	 * 
	 * 
	 */
	public void setInputElement(IJavaScriptElement element);	

	/**
	 * Returns the input element of this type hierarchy view.
	 *
	 * @return the input element, or <code>null</code> if no input element is set
	 * @see #setInputElement(IJavaScriptElement)
	 * 
	 * 
	 */
	public IJavaScriptElement getInputElement();

	/**
	 * Locks the the members view and shows the selected members in the hierarchy.
	 * 
	 * @param enabled If set, the members view will be locked and the selected members are shown in the hierarchy.
	 * 
	 * 
	 */
	public void showMembersInHierarchy(boolean enabled);

	/**
	 * If set, the lock mode is enabled.
	 * 
	 * @return returns if the lock mode is enabled.
	 * 
	 * 
	 */
	public boolean isShowMembersInHierarchy();
	
	/**
	 * Specifies if type names are shown with the parent container's name.
	 * 
	 * @param enabled if enabled, the hierarchy will also show the type container names
	 * 
	 * 
	 */
	public void showQualifiedTypeNames(boolean enabled);
	
	/**
	 * If set, type names are shown with the parent container's name.
	 * 
	 * @return returns if type names are shown with the parent container's name.
	 * 
	 * 
	 */
	public boolean isQualifiedTypeNamesEnabled();
	
    /**
     * Returns whether this type hierarchy view's selection automatically tracks the active editor.
     * 
     * @return <code>true</code> if linking is enabled, <code>false</code> if not
     * 
     * 
     */
	public boolean isLinkingEnabled();	
	
    /**
     * Sets whether this type hierarchy view's selection automatically tracks the active editor.
     * 
     * @param enabled <code>true</code> to enable, <code>false</code> to disable
     * 
     * 
     */
	public void setLinkingEnabled(boolean enabled);	
    
	/**
	 * Sets the view layout. Valid inputs are {@link #VIEW_LAYOUT_VERTICAL}, {@link #VIEW_LAYOUT_HORIZONTAL}
	 * {@link #VIEW_LAYOUT_SINGLE} and {@link #VIEW_LAYOUT_AUTOMATIC}.
	 * 
	 * @param layout The layout to set
	 * 
	 * 
	 */
	public void setViewLayout(int layout);
	
	/**
	 * Returns the currently configured view layout. Possible layouts are {@link #VIEW_LAYOUT_VERTICAL}, {@link #VIEW_LAYOUT_HORIZONTAL}
	 * {@link #VIEW_LAYOUT_SINGLE} and {@link #VIEW_LAYOUT_AUTOMATIC} but clients should also be able to handle yet unknown
	 * layout.
	 * 
	 * @return The layout currently set
	 * 
	 * 
	 */
	public int getViewLayout();
	
	/**
	 * Sets the hierarchy mode. Valid modes are {@link #HIERARCHY_MODE_SUBTYPES}, {@link #HIERARCHY_MODE_SUPERTYPES}
	 * and {@link #HIERARCHY_MODE_CLASSIC}.
	 * 
	 * @param mode The hierarchy mode to set
	 * 
	 * 
	 */
	public void setHierarchyMode(int mode);
	
	/**
	 * Returns the currently configured hierarchy mode. Possible modes are {@link #HIERARCHY_MODE_SUBTYPES}, {@link #HIERARCHY_MODE_SUPERTYPES}
	 * and {@link #HIERARCHY_MODE_CLASSIC} but clients should also be able to handle yet unknown modes.
	 * 
	 * @return The hierarchy mode currently set
	 * 
	 * 
	 */
	public int getHierarchyMode();
    
}
