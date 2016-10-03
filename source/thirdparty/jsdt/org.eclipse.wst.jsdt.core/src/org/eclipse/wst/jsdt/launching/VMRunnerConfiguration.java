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

package org.eclipse.wst.jsdt.launching;

 
import java.util.Map;
 
/**
 * Holder for various arguments passed to a VM runner.
 * Mandatory parameters are passed in the constructor; optional arguments, via setters.
 * <p>
 * Clients may instantiate this class; it is not intended to be subclassed.
 * </p>
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class VMRunnerConfiguration {
	private String fClassToLaunch;
	private String[] fVMArgs;
	private String[] fProgramArgs;
	private String[] fEnvironment;
	private String[] fClassPath;
	private String[] fBootClassPath;
	private String fWorkingDirectory;
	private Map fVMSpecificAttributesMap;
	private boolean fResume = true;
	
	private static final String[] fgEmpty= new String[0];
	
	/**
	 * Creates a new configuration for launching a VM to run the given main class
	 * using the given class path.
	 *
	 * @param classToLaunch The fully qualified name of the class to launch. May not be null.
	 * @param classPath 	The includepath. May not be null.
	 */
	public VMRunnerConfiguration(String classToLaunch, String[] classPath) {
		if (classToLaunch == null) {
			throw new IllegalArgumentException(LaunchingMessages.vmRunnerConfig_assert_classNotNull); 
		}
		if (classPath == null) {
			throw new IllegalArgumentException(LaunchingMessages.vmRunnerConfig_assert_classPathNotNull); 
		}
		fClassToLaunch= classToLaunch;
		fClassPath= classPath;
	}

	/**
	 * Sets the <code>Map</code> that contains String name/value pairs that represent
	 * VM-specific attributes.
	 * 
	 * @param map the <code>Map</code> of VM-specific attributes.
	 *  
	 */
	public void setVMSpecificAttributesMap(Map map) {
		fVMSpecificAttributesMap = map;
	}

	/**
	 * Sets the custom VM arguments. These arguments will be appended to the list of 
	 * VM arguments that a VM runner uses when launching a VM. Typically, these VM arguments
	 * are set by the user.
	 * These arguments will not be interpreted by a VM runner, the client is responsible for
	 * passing arguments compatible with a particular VM runner.
	 *
	 * @param args the list of VM arguments
	 */
	public void setVMArguments(String[] args) {
		if (args == null) {
			throw new IllegalArgumentException(LaunchingMessages.vmRunnerConfig_assert_vmArgsNotNull); 
		}
		fVMArgs= args;
	}
	
	/**
	 * Sets the custom program arguments. These arguments will be appended to the list of 
	 * program arguments that a VM runner uses when launching a VM (in general: none). 
	 * Typically, these VM arguments are set by the user.
	 * These arguments will not be interpreted by a VM runner, the client is responsible for
	 * passing arguments compatible with a particular VM runner.
	 *
	 * @param args the list of arguments	
	 */
	public void setProgramArguments(String[] args) {
		if (args == null) {
			throw new IllegalArgumentException(LaunchingMessages.vmRunnerConfig_assert_programArgsNotNull); 
		}
		fProgramArgs= args;
	}
	
	/**
	 * Sets the environment for the JavaScript program. The JavaScript VM will be
	 * launched in the given environment.
	 * 
	 * @param environment the environment for the JavaScript program specified as an array
	 *  of strings, each element specifying an environment variable setting in the
	 *  format <i>name</i>=<i>value</i>
	 *  
	 */
	public void setEnvironment(String[] environment) {
		fEnvironment= environment;
	}
		
	/**
	 * Sets the boot includepath. Note that the boot includepath will be passed to the 
	 * VM "as is". This means it has to be complete. Interpretation of the boot class path
	 * is up to the VM runner this object is passed to.
	 * <p>
	 * In release 3.0, support has been added for appending and prepending the
	 * boot includepath. Generally an <code>IVMRunner</code> should use the prepend,
	 * main, and append boot includepaths provided. However, in the case that an
	 * <code>IVMRunner</code> does not support these options, a complete bootpath
	 * should also be specified.
	 * </p>
	 * @param bootClassPath The boot includepath. An empty array indicates an empty
	 *  bootpath and <code>null</code> indicates a default bootpath.
	 */
	public void setBootClassPath(String[] bootClassPath) {
		fBootClassPath= bootClassPath;
	}
	
	/**
	 * Returns the <code>Map</code> that contains String name/value pairs that represent
	 * VM-specific attributes.
	 * 
	 * @return The <code>Map</code> of VM-specific attributes or <code>null</code>.
	 *  
	 */
	public Map getVMSpecificAttributesMap() {
		return fVMSpecificAttributesMap;
	}
	
	/**
	 * Returns the name of the class to launch.
	 *
	 * @return The fully qualified name of the class to launch. Will not be <code>null</code>.
	 */
	public String getClassToLaunch() {
		return fClassToLaunch;
	}
	
	/**
	 * Returns the includepath.
	 *
	 * @return the includepath
	 */
	public String[] getClassPath() {
		return fClassPath;
	}
	
	/**
	 * Returns the boot includepath. An empty array indicates an empty
	 * bootpath and <code>null</code> indicates a default bootpath.
	 * <p>
	 * In 3.0, support has been added for prepending and appending to the
	 * boot includepath. The new attributes are stored in the VM specific
	 * attributes map using the following keys defined in 
	 * <code>IJavaLaunchConfigurationConstants</code>:
	 * <ul>
	 * <li>ATTR_BOOTPATH_PREPEND</li>
	 * <li>ATTR_BOOTPATH_APPEND</li>
	 * <li>ATTR_BOOTPATH</li>
	 * </ul>
	 * </p>
	 * @return The boot includepath. An empty array indicates an empty
	 *  bootpath and <code>null</code> indicates a default bootpath.
	 * @see #setBootClassPath(String[])
	 * @see IJavaLaunchConfigurationConstants
	 */
	public String[] getBootClassPath() {
		return fBootClassPath;
	}

	/**
	 * Returns the arguments to the VM itself.
	 *
	 * @return The VM arguments. Default is an empty array. Will not be <code>null</code>.
	 * @see #setVMArguments(String[])
	 */
	public String[] getVMArguments() {
		if (fVMArgs == null) {
			return fgEmpty;
		}
		return fVMArgs;
	}
	
	/**
	 * Returns the arguments to the JavaScript program.
	 *
	 * @return The JavaScript program arguments. Default is an empty array. Will not be <code>null</code>.
	 * @see #setProgramArguments(String[])
	 */
	public String[] getProgramArguments() {
		if (fProgramArgs == null) {
			return fgEmpty;
		}
		return fProgramArgs;
	}
	
	/**
	 * Returns the environment for the JavaScript program or <code>null</code>
	 * 
	 * @return The JavaScript program environment. Default is <code>null</code>
	 *  
	 */
	public String[] getEnvironment() {
		return fEnvironment;
	}
	
	/**
	 * Sets the working directory for a launched VM.
	 * 
	 * @param path the absolute path to the working directory
	 *  to be used by a launched VM, or <code>null</code> if
	 *  the default working directory is to be inherited from the
	 *  current process
	 *  
	 */
	public void setWorkingDirectory(String path) {
		fWorkingDirectory = path;
	}
	
	/**
	 * Returns the working directory of a launched VM.
	 * 
	 * @return the absolute path to the working directory
	 *  of a launched VM, or <code>null</code> if the working
	 *  directory is inherited from the current process
	 *  
	 */
	public String getWorkingDirectory() {
		return fWorkingDirectory;
	}	
	
	/**
	 * Sets whether the VM is resumed on startup when launched in
	 * debug mode. Has no effect when not in debug mode.
	 *  
	 * @param resume whether to resume the VM on startup
	 *  
	 */
	public void setResumeOnStartup(boolean resume) {
		fResume = resume;
	}
	
	/**
	 * Returns whether the VM is resumed on startup when launched
	 * in debug mode. Has no effect when no in debug mode. Default
	 * value is <code>true</code> for backwards compatibility.
	 * 
	 * @return whether to resume the VM on startup
	 *  
	 */
	public boolean isResumeOnStartup() {
		return fResume;
	}
}
