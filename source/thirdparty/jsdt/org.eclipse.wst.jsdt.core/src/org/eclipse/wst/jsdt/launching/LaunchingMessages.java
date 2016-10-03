/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.launching;

import org.eclipse.osgi.util.NLS;

public class LaunchingMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.wst.jsdt.launching.LaunchingMessages";//$NON-NLS-1$

	public static String JavaRuntime_Specified_VM_install_type_does_not_exist___0__2;
	public static String JavaRuntime_VM_not_fully_specified_in_launch_configuration__0____missing_VM_name__Reverting_to_default_VM__1;
	public static String JavaRuntime_Specified_VM_install_not_found__type__0___name__1__2;

	public static String JREContainerInitializer_Array;

	public static String JREContainerInitializer_Global;

	public static String JREContainerInitializer_JsECMA_NAME;

	public static String JREContainerInitializer_Object;


	public static String libraryLocation_assert_libraryNotNull;


	public static String StandardVMType_Standard_VM_3;

	public static String vmInstall_assert_idNotNull;
	public static String vmInstall_assert_typeNotNull;

	public static String vmInstallType_duplicateVM;

	public static String vmRunnerConfig_assert_classNotNull;
	public static String vmRunnerConfig_assert_classPathNotNull;
	public static String vmRunnerConfig_assert_programArgsNotNull;
	public static String vmRunnerConfig_assert_vmArgsNotNull;


	public static String JREContainer_JRE_System_Library_1;

	public static String JREContainerInitializer_Default_System_Library_1;

	public static String JavaRuntime_28;
	public static String JavaRuntime_Launch_configuration__0__references_non_existing_project__1___1;


	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, LaunchingMessages.class);
	}


}
