/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core;

import org.eclipse.wst.jsdt.internal.core.ClasspathAttribute;

/**
 * A includepath attribute defines a name/value pair that can be persisted with a includepath entry. Such an attribute
 * can be created using the factory method {@link JavaScriptCore#newIncludepathAttribute(String, String) newClasspathAttribute(String name, String value)}.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @see JavaScriptCore#newContainerEntry(
 *			org.eclipse.core.runtime.IPath containerPath,
 *			IAccessRule[] accessRules,
 *			IIncludePathAttribute[] extraAttributes,
 *			boolean isExported)
 * @see JavaScriptCore#newLibraryEntry(
 *			org.eclipse.core.runtime.IPath path,
 *			org.eclipse.core.runtime.IPath sourceAttachmentPath,
 *			org.eclipse.core.runtime.IPath sourceAttachmentRootPath,
 *			IAccessRule[] accessRules,
 *			IIncludePathAttribute[] extraAttributes,
 *			boolean isExported)
 * @see JavaScriptCore#newProjectEntry(
 *			org.eclipse.core.runtime.IPath path,
 *			IAccessRule[] accessRules,
 *			boolean combineAccessRestrictions,
 *			IIncludePathAttribute[] extraAttributes,
 *			boolean isExported)
 * @see JavaScriptCore#newSourceEntry(
 * 			org.eclipse.core.runtime.IPath path,
 * 			org.eclipse.core.runtime.IPath[] inclusionPatterns,
 * 			org.eclipse.core.runtime.IPath[] exclusionPatterns,
 * 			org.eclipse.core.runtime.IPath specificOutputLocation,
 * 			IIncludePathAttribute[] extraAttributes)
 * @see JavaScriptCore#newVariableEntry(
 *			org.eclipse.core.runtime.IPath variablePath,
 *			org.eclipse.core.runtime.IPath variableSourceAttachmentPath,
 *			org.eclipse.core.runtime.IPath variableSourceAttachmentRootPath,
 *			IAccessRule[] accessRules,
 *			IIncludePathAttribute[] extraAttributes,
 *			boolean isExported)
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IIncludePathAttribute {

	/**
	 * Constant for the name of the jsdoc location attribute.
	 *
	 */
	String JSDOC_LOCATION_ATTRIBUTE_NAME = "javadoc_location"; //$NON-NLS-1$

	/**
	 * Constant for the name of the optional attribute. The possible values
	 * for this attribute are <code>"true"</code> or <code>"false"</code>.
	 * When not present, <code>"false"</code> is assumed.
	 * If the value of this attribute is <code>"true"</code>, the includepath entry
	 * is optional. If the underlying resource or jar file doesn't exist, no error
	 * is reported and the includepath entry is ignored.
	 */
	String OPTIONAL = "optional"; //$NON-NLS-1$

	/**
	 * Returns the name of this includepath attribute.
	 *
	 * @return the name of this includepath attribute.
	 */
	String getName();

	/**
	 * Returns the value of this includepath attribute.
	 *
	 * @return the value of this includepath attribute.
	 */
	String getValue();
	
	public static final ClasspathAttribute HIDE = new ClasspathAttribute("hide","true"); //$NON-NLS-1$ //$NON-NLS-2$

}
