/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.launching;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.core.compiler.libraries.LibraryLocation;
import org.eclipse.wst.jsdt.core.compiler.libraries.SystemLibraryLocation;
import org.eclipse.wst.jsdt.core.infer.DefaultInferrenceProvider;

/** 
 * Resolves a container for a JRE includepath container entry.
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */ 
public class JREContainerInitializer extends JsGlobalScopeContainerInitializer {
	
	public static final String JsECMA_NAME = LaunchingMessages.JREContainerInitializer_JsECMA_NAME;

	/**
	 * @see JsGlobalScopeContainerInitializer#initialize(IPath, IJavaScriptProject)
	 */
	public void initialize(IPath containerPath, IJavaScriptProject project) throws CoreException {		
		int size = containerPath.segmentCount();
		if (size > 0) {
			if (containerPath.segment(0).equals(JavaRuntime.JRE_CONTAINER)) {
				IVMInstall vm = resolveVM(containerPath);
				JREContainer container = null;
				if (vm != null) {
					container = new JREContainer(vm, containerPath);
				}
				JavaScriptCore.setJsGlobalScopeContainer(containerPath, new IJavaScriptProject[] {project}, new IJsGlobalScopeContainer[] {container}, null);
			}
		}
	}
	
	
	
	public int getKind() {
		return K_DEFAULT_SYSTEM;
	}



	/**
	 * Returns the VM install associated with the container path, or <code>null</code>
	 * if it does not exist.
	 */
	public static IVMInstall resolveVM(IPath containerPath) {
		IVMInstall vm = null;
		if (containerPath.segmentCount() > 1) {
			// specific JRE
			String id = getExecutionEnvironmentId(containerPath);
			if (id != null) {
				//TODO: implement
				throw new org.eclipse.wst.jsdt.core.UnimplementedException();
//				IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
//				IExecutionEnvironment environment = manager.getEnvironment(id);
//				if (environment != null) {
//					vm = resolveVM(environment);
//				}
			} else {
				String vmTypeId = getVMTypeId(containerPath);
				String vmName = getVMName(containerPath);
				IVMInstallType vmType = JavaRuntime.getVMInstallType(vmTypeId);
				if (vmType != null) {
					vm = vmType.findVMInstallByName(vmName);
				}
			}
		} else {
			// workspace default JRE
			vm = JavaRuntime.getDefaultVMInstall();
		}		
		return vm;
	}
	
//	/**
//	 * Returns the VM install bound to the given execution environment
//	 * or <code>null</code>.
//	 * 
//	 * @param environment
//	 * @return vm install or <code>null</code>
//	 *  
//	 */
//	public static IVMInstall resolveVM(IExecutionEnvironment environment) {
//		IVMInstall vm = environment.getDefaultVM();
//		if (vm == null) {
//			IVMInstall[] installs = environment.getCompatibleVMs();
//			// take the first strictly compatible vm if there is no default
//			for (int i = 0; i < installs.length; i++) {
//				IVMInstall install = installs[i];
//				if (environment.isStrictlyCompatible(install)) {
//					vm = install;
//					break;
//				}
//			}
//			// use the first vm failing that
//			if (vm == null && installs.length > 0) {
//				vm = installs[0];
//			}
//		}
//		return vm;
//	}
	
	/**
	 * Returns the segment from the path containing the execution environment id
	 * or <code>null</code>
	 * 
	 * @param path container path
	 * @return ee id
	 */
	public static String getExecutionEnvironmentId(IPath path) {
		return null;
//		String name = getVMName(path);
//		if (name != null) {
////			name = decodeEnvironmentId(name);
////			IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
////			IExecutionEnvironment environment = manager.getEnvironment(name);
////			if (environment != null) {
////				return environment.getId();
////			}
//		}
//		return null;
	}
	
	/**
	 * Returns whether the given path identifies a vm by exeuction environment.
	 * 
	 * @param path
	 * @return whether the given path identifies a vm by exeuction environment
	 */
	public static boolean isExecutionEnvironment(IPath path) {
		return getExecutionEnvironmentId(path) != null;
	}
	
	/**
	 * Escapes foward slashes in environment id.
	 * 
	 * @param id
	 * @return esaped name
	 */
	public static String encodeEnvironmentId(String id) {
		return id.replace('/', '%');
	}
	
	public static String decodeEnvironmentId(String id) {
		return id.replace('%', '/');
	}
	
	/**
	 * Returns the VM type identifier from the given container ID path.
	 * 
	 * @return the VM type identifier from the given container ID path
	 */
	public static String getVMTypeId(IPath path) {
		return path.segment(1);
	}
	
	/**
	 * Returns the VM name from the given container ID path.
	 * 
	 * @return the VM name from the given container ID path
	 */
	public static String getVMName(IPath path) {
		return path.segment(2);
	}	
	
	/**
	 * The container can be updated if it refers to an existing VM.
	 * 
	 * @see org.eclipse.jdt.core.JsGlobalScopeContainerInitializer#canUpdateJsGlobalScopeContainer(org.eclipse.core.runtime.IPath, org.eclipse.IJavaScriptProject.core.IJavaProject)
	 */
	public boolean canUpdateJsGlobalScopeContainer(IPath containerPath, IJavaScriptProject project) {
//		if (containerPath != null && containerPath.segmentCount() > 0) {
//			if (JavaRuntime.JRE_CONTAINER.equals(containerPath.segment(0))) {
//				return resolveVM(containerPath) != null;
//			}
//		}
		return false;
	}

	/**
	 * @see org.eclipse.jdt.core.JsGlobalScopeContainerInitializer#requestJsGlobalScopeContainerUpdate(org.eclipse.core.runtime.IPath, org.eclipse.IJavaScriptProject.core.IJavaProject, org.eclipse.jdt.core.IJsGlobalScopeContainer)
	 */
	public void requestJsGlobalScopeContainerUpdate(IPath containerPath, IJavaScriptProject project, IJsGlobalScopeContainer containerSuggestion) throws CoreException {
//		IVMInstall vm = resolveVM(containerPath);
//		if (vm == null) { 
//			IStatus status = new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IJavaLaunchConfigurationConstants.ERR_VM_INSTALL_DOES_NOT_EXIST, MessageFormat.format(LaunchingMessages.JREContainerInitializer_JRE_referenced_by_includepath_container__0__does_not_exist__1, new String[]{containerPath.toString()}), null); 
//			throw new CoreException(status);
//		}
//		// update of the vm with new library locations
//		
//		IIncludePathEntry[] entries = containerSuggestion.getClasspathEntries();
//		LibraryLocation[] libs = new LibraryLocation[entries.length];
//		for (int i = 0; i < entries.length; i++) {
//			IIncludePathEntry entry = entries[i];
//			if (entry.getEntryKind() == IIncludePathEntry.CPE_LIBRARY) {
//				IPath path = entry.getPath();
//				File lib = path.toFile();
//				if (lib.exists() && lib.isFile()) {
//					IPath srcPath = entry.getSourceAttachmentPath();
//					if (srcPath == null) {
//						srcPath = Path.EMPTY;
//					}
//					IPath rootPath = entry.getSourceAttachmentRootPath();
//					if (rootPath == null) {
//						rootPath = Path.EMPTY;
//					}
//					URL javadocLocation = null;
//					IIncludePathAttribute[] extraAttributes = entry.getExtraAttributes();
//					for (int j = 0; j < extraAttributes.length; j++) {
//						IIncludePathAttribute attribute = extraAttributes[j];
//						if (attribute.getName().equals(IIncludePathAttribute.JSDOC_LOCATION_ATTRIBUTE_NAME)) {
//							String url = attribute.getValue();
//							if (url != null && url.trim().length() > 0) {
//								try {
//									javadocLocation = new URL(url);
//								} catch (MalformedURLException e) {
//									JavaPlugin.log(e);
//								}
//							}
//						}
//					}
//					libs[i] = new LibraryLocation(path, srcPath, rootPath, javadocLocation);
//				} else {
//					IStatus status = new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IJavaLaunchConfigurationConstants.ERR_INTERNAL_ERROR, MessageFormat.format(LaunchingMessages.JREContainerInitializer_Classpath_entry__0__does_not_refer_to_an_existing_library__2, new String[]{entry.getPath().toString()}), null); 
//					throw new CoreException(status);
//				}
//			} else {
//				IStatus status = new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IJavaLaunchConfigurationConstants.ERR_INTERNAL_ERROR, MessageFormat.format(LaunchingMessages.JREContainerInitializer_Classpath_entry__0__does_not_refer_to_a_library__3, new String[]{entry.getPath().toString()}), null); 
//				throw new CoreException(status);
//			}
//		}
//		VMStandin standin = new VMStandin(vm);
//		standin.setLibraryLocations(libs);
//		standin.convertToRealVM();
//		JavaRuntime.saveVMConfiguration();
	}

	/**
	 * @see org.eclipse.jdt.core.JsGlobalScopeContainerInitializer#getDescription(org.eclipse.core.runtime.IPath, org.eclipse.IJavaScriptProject.core.IJavaProject)
	 */
	public String getDescription(IPath containerPath, IJavaScriptProject project) {
		if (containerPath != null && containerPath.segment(0).equals(JavaRuntime.JRE_CONTAINER))
			return LaunchingMessages.JREContainerInitializer_JsECMA_NAME;

//		String tag = getExecutionEnvironmentId(containerPath);
//		if (tag == null && containerPath.segmentCount() > 2) {
//			tag = getVMName(containerPath);
//		}
//		if (tag != null) {
//			return MessageFormat.format(LaunchingMessages.JREContainer_JRE_System_Library_1, new String[]{tag});
//		} 

//		return LaunchingMessages.JREContainerInitializer_Default_System_Library_1; 
		return containerPath.lastSegment();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.IJsGlobalScopeContainerInitialzer#getLibraryLocation()
	 */
	public LibraryLocation getLibraryLocation() {
		return new SystemLibraryLocation();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer#allowAttachJsDoc()
	 */
	public boolean allowAttachJsDoc() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer#containerSuperTypes()
	 */
	public String[] containerSuperTypes() {
		return new String[] {LaunchingMessages.JREContainerInitializer_Global,LaunchingMessages.JREContainerInitializer_Object,LaunchingMessages.JREContainerInitializer_Array};
	}
	
	public String getInferenceID() {
		return DefaultInferrenceProvider.ID;
	}
	

}
