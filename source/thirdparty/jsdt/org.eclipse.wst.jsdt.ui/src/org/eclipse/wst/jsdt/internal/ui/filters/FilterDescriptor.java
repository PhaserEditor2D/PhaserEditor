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
package org.eclipse.wst.jsdt.internal.ui.filters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.IPluginContribution;
import org.eclipse.ui.activities.WorkbenchActivityHelper;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

import com.ibm.icu.text.Collator;

/**
 * Represents a custom filter which is provided by the
 * "org.eclipse.wst.jsdt.ui.javaElementFilters" extension point.
 * 
 * since 2.0
 */
public class FilterDescriptor implements Comparable, IPluginContribution {

	private static String PATTERN_FILTER_ID_PREFIX= "_patternFilterId_"; //$NON-NLS-1$


	private static final String EXTENSION_POINT_NAME= "javaElementFilters"; //$NON-NLS-1$

	private static final String FILTER_TAG= "filter"; //$NON-NLS-1$

	private static final String PATTERN_ATTRIBUTE= "pattern"; //$NON-NLS-1$	
	private static final String ID_ATTRIBUTE= "id"; //$NON-NLS-1$
	/**
	 * @deprecated as of 3.0 use {@link FilterDescriptor#TARGET_ID_ATTRIBUTE}
	 */
	private static final String VIEW_ID_ATTRIBUTE= "viewId"; //$NON-NLS-1$
	private static final String TARGET_ID_ATTRIBUTE= "targetId"; //$NON-NLS-1$
	private static final String CLASS_ATTRIBUTE= "class"; //$NON-NLS-1$
	private static final String NAME_ATTRIBUTE= "name"; //$NON-NLS-1$
	private static final String ENABLED_ATTRIBUTE= "enabled"; //$NON-NLS-1$
	private static final String DESCRIPTION_ATTRIBUTE= "description"; //$NON-NLS-1$	
	/**
	 * @deprecated	use "enabled" instead
	 */
	private static final String SELECTED_ATTRIBUTE= "selected"; //$NON-NLS-1$

	private static FilterDescriptor[] fgFilterDescriptors;


	private IConfigurationElement fElement;

	/**
	 * Returns all contributed Java element filters.
	 */
	public static FilterDescriptor[] getFilterDescriptors() {
		if (fgFilterDescriptors == null) {
			IExtensionRegistry registry= Platform.getExtensionRegistry();
			IConfigurationElement[] elements= registry.getConfigurationElementsFor(JavaScriptUI.ID_PLUGIN, EXTENSION_POINT_NAME);
			fgFilterDescriptors= createFilterDescriptors(elements);
		}	
		return fgFilterDescriptors;
	} 
	/**
	 * Returns all Java element filters which
	 * are contributed to the given view.
	 */
	public static FilterDescriptor[] getFilterDescriptors(String targetId) {
		FilterDescriptor[] filterDescs= FilterDescriptor.getFilterDescriptors();
		List result= new ArrayList(filterDescs.length);
		for (int i= 0; i < filterDescs.length; i++) {
			String tid= filterDescs[i].getTargetId();
			if (WorkbenchActivityHelper.filterItem(filterDescs[i]))
				continue;
			if (tid == null || tid.equals(targetId))
				result.add(filterDescs[i]);
		}
		return (FilterDescriptor[])result.toArray(new FilterDescriptor[result.size()]);
	}
	
	/**
	 * Creates a new filter descriptor for the given configuration element.
	 */
	private FilterDescriptor(IConfigurationElement element) {
		fElement= element;
		// it is either a pattern filter or a custom filter
		Assert.isTrue(isPatternFilter() ^ isCustomFilter(), "An extension for extension-point org.eclipse.wst.jsdt.ui.javaElementFilters does not specify a correct filter"); //$NON-NLS-1$
		Assert.isNotNull(getId(), "An extension for extension-point org.eclipse.wst.jsdt.ui.javaElementFilters does not provide a valid ID"); //$NON-NLS-1$
		Assert.isNotNull(getName(), "An extension for extension-point org.eclipse.wst.jsdt.ui.javaElementFilters does not provide a valid name"); //$NON-NLS-1$
	}

	/**
	 * Creates a new <code>ViewerFilter</code>.
	 * This method is only valid for viewer filters.
	 */
	public ViewerFilter createViewerFilter() {
		if (!isCustomFilter())
			return null;
		
		final ViewerFilter[] result= new ViewerFilter[1];
		String message= Messages.format(FilterMessages.FilterDescriptor_filterCreationError_message, getId()); 
		ISafeRunnable code= new SafeRunnable(message) {
			/*
			 * @see org.eclipse.core.runtime.ISafeRunnable#run()
			 */
			public void run() throws Exception {
				result[0]= (ViewerFilter)fElement.createExecutableExtension(CLASS_ATTRIBUTE);
			}
			
		};
		SafeRunner.run(code);
		return result[0];
	}
	
	//---- XML Attribute accessors ---------------------------------------------
	
	/**
	 * Returns the filter's id.
	 * <p>
	 * This attribute is mandatory for custom filters.
	 * The ID for pattern filters is
	 * PATTERN_FILTER_ID_PREFIX plus the pattern itself.
	 * </p>
	 */
	public String getId() {
		if (isPatternFilter()) {
			String targetId= getTargetId();
			if (targetId == null)
				return PATTERN_FILTER_ID_PREFIX + getPattern();
			else
				return targetId + PATTERN_FILTER_ID_PREFIX + getPattern();
		} else
			return fElement.getAttribute(ID_ATTRIBUTE);
	}
	
	/**
	 * Returns the filter's name.
	 * <p>
	 * If the name of a pattern filter is missing
	 * then the pattern is used as its name.
	 * </p>
	 */
	public String getName() {
		String name= fElement.getAttribute(NAME_ATTRIBUTE);
		if (name == null && isPatternFilter())
			name= getPattern();
		return name;
	}

	/**
	 * Returns the filter's pattern.
	 * 
	 * @return the pattern string or <code>null</code> if it's not a pattern filter
	 */
	public String getPattern() {
		return fElement.getAttribute(PATTERN_ATTRIBUTE);
	}

	/**
	 * Returns the filter's viewId.
	 * 
	 * @return the view ID or <code>null</code> if the filter is for all views
	 * 
	 */
	public String getTargetId() {
		String tid= fElement.getAttribute(TARGET_ID_ATTRIBUTE);
		
		if (tid != null)
			return tid;
		
		// Backwards compatibility code
		return fElement.getAttribute(VIEW_ID_ATTRIBUTE);
		
	}

	/**
	 * Returns the filter's description.
	 * 
	 * @return the description or <code>null</code> if no description is provided
	 */
	public String getDescription() {
		String description= fElement.getAttribute(DESCRIPTION_ATTRIBUTE);
		if (description == null)
			description= ""; //$NON-NLS-1$
		return description;
	}

	/**
	 * @return <code>true</code> if this filter is a custom filter.
	 */
	public boolean isPatternFilter() {
		return getPattern() != null;
	}

	/**
	 * @return <code>true</code> if this filter is a pattern filter.
	 */
	public boolean isCustomFilter() {
		return fElement.getAttribute(CLASS_ATTRIBUTE) != null;
	}

	/**
	 * Returns <code>true</code> if the filter
	 * is initially enabled.
	 * 
	 * This attribute is optional and defaults to <code>true</code>.
	 */
	public boolean isEnabled() {
		String strVal= fElement.getAttribute(ENABLED_ATTRIBUTE);
		if (strVal == null)
			// backward compatibility
			strVal= fElement.getAttribute(SELECTED_ATTRIBUTE);
		return strVal == null || Boolean.valueOf(strVal).booleanValue();
	}

	/* 
	 * Implements a method from IComparable 
	 */ 
	public int compareTo(Object o) {
		if (o instanceof FilterDescriptor)
			return Collator.getInstance().compare(getName(), ((FilterDescriptor)o).getName());
		else
			return Integer.MIN_VALUE;
	}

	//---- initialization ---------------------------------------------------
	
	/**
	 * Creates the filter descriptors.
	 */
	private static FilterDescriptor[] createFilterDescriptors(IConfigurationElement[] elements) {
		List result= new ArrayList(5);
		Set descIds= new HashSet(5);
		for (int i= 0; i < elements.length; i++) {
			final IConfigurationElement element= elements[i];
			if (FILTER_TAG.equals(element.getName())) {

				final FilterDescriptor[] desc= new FilterDescriptor[1];
				SafeRunner.run(new SafeRunnable(FilterMessages.FilterDescriptor_filterDescriptionCreationError_message) { 
					public void run() throws Exception {
						desc[0]= new FilterDescriptor(element);
					}
				});

				if (desc[0] != null && !descIds.contains(desc[0].getId())) {
					result.add(desc[0]);
					descIds.add(desc[0].getId());
				}
			}
		}
		return (FilterDescriptor[])result.toArray(new FilterDescriptor[result.size()]);
	}
	
	public String getLocalId() {
		return fElement.getAttribute(ID_ATTRIBUTE);
	}

    public String getPluginId() {
        return fElement.getContributor().getName();
    }
}
