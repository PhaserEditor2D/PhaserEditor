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


import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;

/**
 * Standard images provided by the JavaScript UI plug-in. This class offers access to the 
 * standard images in two forms:
 * <ul>
 *   <li>Use <code>ISharedImages.getImage(IMG_OBJS_<i>FOO</i>)</code> 
 *    to access the shared standard <code>Image</code> object (caller must not
 *    dispose of image).</li>
 *   <li>Use <code>ISharedImages.getImageDescriptor(IMG_OBJS_<i>FOO</i>)</code> 
 *    to access the standard <code>ImageDescriptor</code> object (caller is 
 *    responsible for disposing of any <code>Image</code> objects it creates using
 *    this descriptor).</li>
 * </ul>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p> 

 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public interface ISharedImages {
			
	/**
	 * Key to access the shared image or image descriptor for a JavaScript compilation unit.
	 */
	public static final String IMG_OBJS_CUNIT= JavaPluginImages.IMG_OBJS_CUNIT;
	
	/**
	 * Key to access the shared image or image descriptor for a JavaScript class file.
	 */
	public static final String IMG_OBJS_CFILE= JavaPluginImages.IMG_OBJS_CFILE; 
	
	/**
	 * Key to access the shared image or image descriptor for a JAR archive.
	 */
	public static final String IMG_OBJS_JAR= JavaPluginImages.IMG_OBJS_JAR;
	
	/**
	 * Key to access the shared image or image descriptor for a JAR with source.
	 * 
	 */
	public static final String IMG_OBJS_JAR_WITH_SOURCE= JavaPluginImages.IMG_OBJS_JAR_WSRC;
			
	/**
	 * Key to access the shared image or image descriptor for external archives. 
	 * 
	 */
	public static final String IMG_OBJS_EXTERNAL_ARCHIVE= JavaPluginImages.IMG_OBJS_EXTJAR;
	
	/** 
	 * Key to access the shared image or image descriptor for external archives with source.
	 * 
	 */
	public static final String IMG_OBJS_EXTERNAL_ARCHIVE_WITH_SOURCE= JavaPluginImages.IMG_OBJS_EXTJAR_WSRC;

	/**
	 * Key to access the shared image or image descriptor for a classpath variable entry.
	 * 
	 */
	public static final String IMG_OBJS_CLASSPATH_VAR_ENTRY= JavaPluginImages.IMG_OBJS_ENV_VAR;

	/**
	 * Key to access the shared image or image descriptor for a library (class path container).
	 * 
	 */
	public static final String IMG_OBJS_LIBRARY= JavaPluginImages.IMG_OBJS_LIBRARY;
	
	/**
	 * Key to access the shared image or image descriptor for a package fragment root.
	 * 
	 */
	public static final String IMG_OBJS_PACKFRAG_ROOT= JavaPluginImages.IMG_OBJS_PACKFRAG_ROOT;
	
	/**
	 * Key to access the shared image or image descriptor for a package.
	 */
	public static final String IMG_OBJS_PACKAGE= JavaPluginImages.IMG_OBJS_PACKAGE;
	
	/**
	 * Key to access the shared image or image descriptor for an empty package.
	 * 
	 */
	public static final String IMG_OBJS_EMPTY_PACKAGE= JavaPluginImages.IMG_OBJS_EMPTY_PACKAGE;

	/**
	 * Key to access the shared image or image descriptor for a logical package.
	 * 
	 */
	public static final String IMG_OBJS_LOGICAL_PACKAGE= JavaPluginImages.IMG_OBJS_LOGICAL_PACKAGE;
	
	/**
	 * Key to access the shared image or image descriptor for an empty logical package.
	 * 
	 */
	public static final String IMG_OBJS_EMPTY_LOGICAL_PACKAGE= JavaPluginImages.IMG_OBJS_EMPTY_LOGICAL_PACKAGE;
	
	/**
	 * Key to access the shared image or image descriptor for a class.
	 */	
	public static final String IMG_OBJS_CLASS= JavaPluginImages.IMG_OBJS_CLASS;
	
	/**
	 * Key to access the shared image or image descriptor for a class with default visibility.
	 * 
	 */
	public static final String IMG_OBJS_CLASS_DEFAULT= JavaPluginImages.IMG_OBJS_CLASS_DEFAULT;
	
	/**
	 * Key to access the shared image or image descriptor for a public inner class.
	 * 
	 */
	public static final String IMG_OBJS_INNER_CLASS_PUBLIC= JavaPluginImages.IMG_OBJS_INNER_CLASS_PUBLIC;
	
	/**
	 * Key to access the shared image or image descriptor for a inner class with default visibility.
	 * 
	 */
	public static final String IMG_OBJS_INNER_CLASS_DEFAULT= JavaPluginImages.IMG_OBJS_INNER_CLASS_DEFAULT;
	
	/**
	 * Key to access the shared image or image descriptor for a protected inner class.
	 * 
	 */
	public static final String IMG_OBJS_INNER_CLASS_PROTECTED= JavaPluginImages.IMG_OBJS_INNER_CLASS_PROTECTED;
	
	/**
	 * Key to access the shared image or image descriptor for a private inner class.
	 * 
	 */
	public static final String IMG_OBJS_INNER_CLASS_PRIVATE= JavaPluginImages.IMG_OBJS_INNER_CLASS_PRIVATE;
	
	/**
	 * Key to access the shared image or image descriptor for an interface.
	 */
	public static final String IMG_OBJS_INTERFACE= JavaPluginImages.IMG_OBJS_INTERFACE;
	
	/**
	 * Key to access the shared image or image descriptor for an interface with default visibility.
	 * 
	 */
	public static final String IMG_OBJS_INTERFACE_DEFAULT= JavaPluginImages.IMG_OBJS_INTERFACE_DEFAULT;
	
	/**
	 * Key to access the shared image or image descriptor for a public inner interface.
	 * 
	 */
	public static final String IMG_OBJS_INNER_INTERFACE_PUBLIC= JavaPluginImages.IMG_OBJS_INNER_INTERFACE_PUBLIC;
	
	/**
	 * Key to access the shared image or image descriptor for an inner interface with default visibility.
	 * 
	 */
	public static final String IMG_OBJS_INNER_INTERFACE_DEFAULT= JavaPluginImages.IMG_OBJS_INNER_INTERFACE_DEFAULT;
	
	/**
	 * Key to access the shared image or image descriptor for a protected inner interface.
	 * 
	 */
	public static final String IMG_OBJS_INNER_INTERFACE_PROTECTED= JavaPluginImages.IMG_OBJS_INNER_INTERFACE_PROTECTED;
	
	/**
	 * Key to access the shared image or image descriptor for a private inner interface.
	 * 
	 */
	public static final String IMG_OBJS_INNER_INTERFACE_PRIVATE= JavaPluginImages.IMG_OBJS_INNER_INTERFACE_PRIVATE;

	/** Key to access the shared image or image descriptor for a package declaration. */
	public static final String IMG_OBJS_PACKDECL= JavaPluginImages.IMG_OBJS_PACKDECL;
	
	/** Key to access the shared image or image descriptor for an import container. */
	public static final String IMG_OBJS_IMPCONT= JavaPluginImages.IMG_OBJS_IMPCONT;
	
	/** Key to access the shared image or image descriptor for an import statement. */
	public static final String IMG_OBJS_IMPDECL= JavaPluginImages.IMG_OBJS_IMPDECL;
	
	/** Key to access the shared image or image descriptor for a public member. */
	public static final String IMG_OBJS_PUBLIC= JavaPluginImages.IMG_MISC_PUBLIC;
	
	/** Key to access the shared image or image descriptor for a protected member. */
	public static final String IMG_OBJS_PROTECTED= JavaPluginImages.IMG_MISC_PROTECTED;
	
	/** Key to access the shared image or image descriptor for a private member. */
	public static final String IMG_OBJS_PRIVATE= JavaPluginImages.IMG_MISC_PRIVATE;
	
	/** Key to access the shared image or image descriptor for class members with default visibility. */
	public static final String IMG_OBJS_DEFAULT= JavaPluginImages.IMG_MISC_DEFAULT;
	
	/**
	 * Key to access the shared image or image descriptor for a public field.
	 * 
	 */
	public static final String IMG_FIELD_PUBLIC= JavaPluginImages.IMG_FIELD_PUBLIC;
	
	/**
	 * Key to access the shared image or image descriptor for a protected field.
	 * 
	 */
	public static final String IMG_FIELD_PROTECTED= JavaPluginImages.IMG_FIELD_PROTECTED;
	
	/**
	 * Key to access the shared image or image descriptor for a private field.
	 * 
	 */
	public static final String IMG_FIELD_PRIVATE= JavaPluginImages.IMG_FIELD_PRIVATE;
	
	/**
	 * Key to access the shared image or image descriptor for a field with default visibility.
	 * 
	 */
	public static final String IMG_FIELD_DEFAULT= JavaPluginImages.IMG_FIELD_DEFAULT;
		
	/**
	 * Key to access the shared image or image descriptor for a local variable.
	 * 
	 */
	public static final String IMG_OBJS_LOCAL_VARIABLE= JavaPluginImages.IMG_OBJS_LOCAL_VARIABLE;
	
	/**
	 * Key to access the shared image or image descriptor for a enum type.
	 * 
	 */
	public static final String IMG_OBJS_ENUM= JavaPluginImages.IMG_OBJS_ENUM;
	
	/**
	 * Key to access the shared image or image descriptor for a enum type
	 * with default visibility.
	 * 
	 */
	public static final String IMG_OBJS_ENUM_DEFAULT= JavaPluginImages.IMG_OBJS_ENUM_DEFAULT;
	
	/**
	 * Key to access the shared image or image descriptor for a enum type
	 * with protected visibility.
	 * 
	 */
	public static final String IMG_OBJS_ENUM_PROTECTED= JavaPluginImages.IMG_OBJS_ENUM_PROTECTED;
	
	/**
	 * Key to access the shared image or image descriptor for a enum type
	 * with private visibility.
	 * 
	 */
	public static final String IMG_OBJS_ENUM_PRIVATE= JavaPluginImages.IMG_OBJS_ENUM_PRIVATE;
	
	/**
	 * Key to access the shared image or image descriptor for a annotation type.
	 * 
	 */
	public static final String IMG_OBJS_ANNOTATION= JavaPluginImages.IMG_OBJS_ANNOTATION;
	
	/**
	 * Key to access the shared image or image descriptor for a annotation type
	 * with default visibility.
	 * 
	 */
	public static final String IMG_OBJS_ANNOTATION_DEFAULT= JavaPluginImages.IMG_OBJS_ANNOTATION_DEFAULT;
	
	/**
	 * Key to access the shared image or image descriptor for a annotation type
	 * with protected visibility.
	 * 
	 */
	public static final String IMG_OBJS_ANNOTATION_PROTECTED= JavaPluginImages.IMG_OBJS_ANNOTATION_PROTECTED;
	
	/**
	 * Key to access the shared image or image descriptor for a annotation type
	 * with private visibility.
	 * 
	 */
	public static final String IMG_OBJS_ANNOTATION_PRIVATE= JavaPluginImages.IMG_OBJS_ANNOTATION_PRIVATE;
	
	/**
	 * Key to access the shared image or image descriptor for javadoc tags.
	 * 
	 */
	public static final String IMG_OBJS_JAVADOCTAG= JavaPluginImages.IMG_OBJS_JAVADOCTAG;
	
	/**
	 * Returns the shared image managed under the given key.
	 * <p>
	 * Note that clients <b>must not</b> dispose the image returned by this method.
	 * </p>
	 *
	 * @param key the image key; one of the <code>IMG_OBJS_* </code> constants
	 * @return the shared image managed under the given key, or <code>null</code>
	 *   if none
	 */
	public Image getImage(String key);
	
	/**
	 * Returns the image descriptor managed under the given key.
	 *
	 * @param key the image key; one of the <code>IMG_OBJS_* </code> constants
	 * @return the image descriptor managed under the given key, or <code>null</code>
	 *  if none
	 */
	public ImageDescriptor getImageDescriptor(String key);
}
