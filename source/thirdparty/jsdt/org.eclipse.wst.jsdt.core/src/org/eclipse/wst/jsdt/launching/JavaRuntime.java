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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.compiler.libraries.SystemLibraryLocation;

import com.ibm.icu.text.MessageFormat;

/**
 * The central access point for launching support. This class manages
 * the registered VM types contributed through the 
 * <code>"org.eclipse.wst.jsdt.launching.vmType"</code> extension point.
 * As well, this class provides VM install change notification,
 * and computes includepaths and source lookup paths for launch
 * configurations.
 * <p>
 * This class provides static methods only; it is not intended to be
 * instantiated or subclassed by clients.
 * </p>
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public final class JavaRuntime {
	
	/**
	 * Classpath variable name used for the default JRE's library
	 * (value <code>"JRE_LIB"</code>).
	 */
	public static final String JRELIB_VARIABLE= "JRE_LIB"; //$NON-NLS-1$

	/**
	 * Classpath variable name used for the default JRE's library source
	 * (value <code>"JRE_SRC"</code>).
	 */
	public static final String JRESRC_VARIABLE= "JRE_SRC"; //$NON-NLS-1$
	
	/**
	 * Classpath variable name used for the default JRE's library source root
	 * (value <code>"JRE_SRCROOT"</code>).
	 */	
	public static final String JRESRCROOT_VARIABLE= "JRE_SRCROOT"; //$NON-NLS-1$
	
	/**
	 * Simple identifier constant (value <code>"runtimeClasspathEntryResolvers"</code>) for the
	 * runtime includepath entry resolvers extension point.
	 * 
	 *  
	 */
	public static final String EXTENSION_POINT_RUNTIME_CLASSPATH_ENTRY_RESOLVERS= "runtimeClasspathEntryResolvers";	 //$NON-NLS-1$	
	
	/**
	 * Simple identifier constant (value <code>"classpathProviders"</code>) for the
	 * runtime includepath providers extension point.
	 * 
	 *  
	 */
	public static final String EXTENSION_POINT_RUNTIME_CLASSPATH_PROVIDERS= "classpathProviders";	 //$NON-NLS-1$		
	
	/**
	 * Simple identifier constant (value <code>"executionEnvironments"</code>) for the
	 * execution environments extension point.
	 * 
	 *  
	 */
	public static final String EXTENSION_POINT_EXECUTION_ENVIRONMENTS= "executionEnvironments";	 //$NON-NLS-1$
	
	/**
	 * Simple identifier constant (value <code>"vmInstalls"</code>) for the
	 * VM installs extension point.
	 * 
	 *  
	 */
	public static final String EXTENSION_POINT_VM_INSTALLS = "vmInstalls";	 //$NON-NLS-1$		
		
	/**
	 * Classpath container used for a project's JRE
	 * (value <code>"org.eclipse.wst.jsdt.launching.JRE_CONTAINER"</code>). A
	 * container is resolved in the context of a specific JavaScript project, to one
	 * or more system libraries contained in a JRE. The container can have zero
	 * or two path segments following the container name. When no segments
	 * follow the container name, the workspace default JRE is used to build a
	 * project. Otherwise the segments identify a specific JRE used to build a
	 * project:
	 * <ol>
	 * <li>VM Install Type Identifier - identifies the type of JRE used to build the
	 * 	project. For example, the standard VM.</li>
	 * <li>VM Install Name - a user defined name that identifies that a specific VM
	 * 	of the above kind. For example, <code>IBM 1.3.1</code>. This information is
	 *  shared in a projects includepath file, so teams must agree on JRE naming
	 * 	conventions.</li>
	 * </ol>
	 * <p>
	 * Since 3.2, the path may also identify an execution environment as follows:
	 * <ol>
	 * <li>Execution environment extension point name
	 * (value <code>executionEnvironments</code>)</li>
	 * <li>Identifier of a contributed execution environment</li>
	 * </ol>
	 * </p>
	 *  
	 */
	public static final String JRE_CONTAINER ="org.eclipse.wst.jsdt.launching.JRE_CONTAINER"; //$NON-NLS-1$
	public static final String BASE_BROWSER_LIB="org.eclipse.wst.jsdt.launching.baseBrowserLibrary";
	
	/*
	 * Default supertype for javascript unit s
	 */
	public static final String DEFAULT_SUPER_TYPE = "Global"; //$NON-NLS-1$
	
	public static final String DEFAULT_SUPER_TYPE_LIBRARY = JRE_CONTAINER; //$NON-NLS-1$
	/**
	 * A status code indicating that a JRE could not be resolved for a project.
	 * When a JRE cannot be resolved for a project by this plug-in's container
	 * initializer, an exception is thrown with this status code. A status handler
	 * may be registered for this status code. The <code>source</code> object provided
	 * to the status handler is the JavaScript project for which the path could not be
	 * resolved. The status handler must return an <code>IVMInstall</code> or <code>null</code>.
	 * The container resolver will re-set the project's includepath if required.
	 * 
	 *  
	 */
	public static final int ERR_UNABLE_TO_RESOLVE_JRE = 160;
	
	/**
	 * Preference key for launch/connect timeout. VM Runners should honor this timeout
	 * value when attempting to launch and connect to a debuggable VM. The value is
	 * an int, indicating a number of milliseconds.
	 * 
	 *  
	 */
	public static final String PREF_CONNECT_TIMEOUT = JavaScriptCore.PLUGIN_ID + ".PREF_CONNECT_TIMEOUT"; //$NON-NLS-1$
	
	/**
	 * Preference key for the String of XML that defines all installed VMs.
	 * 
	 *  
	 */
	public static final String PREF_VM_XML = JavaScriptCore.PLUGIN_ID + ".PREF_VM_XML"; //$NON-NLS-1$
	
	/**
	 * Default launch/connect timeout (ms).
	 * 
	 *  
	 */
	public static final int DEF_CONNECT_TIMEOUT = 20000;
	
	/**
	 * Attribute key for a process property. The class
	 * <code>org.eclipse.debug.core.model.IProcess</code> allows attaching
	 * String properties to processes.
	 * The value of this attribute is the command line a process
	 * was launched with. Implementers of <code>IVMRunner</code> should use
	 * this attribute key to attach the command lines to the processes they create.
	 * 
	 * @deprecated - use <code>IProcess.ATTR_CMDLINE</code>
	 */
	public final static String ATTR_CMDLINE= JavaScriptCore.PLUGIN_ID + ".launcher.cmdLine"; //$NON-NLS-1$
	
	/**
	 * Attribute key for a includepath attribute referencing a
	 * list of shared libraries that should appear on the
	 * <code>-Djava.library.path</code> system property.
	 * <p>
	 * The factory methods <code>newLibraryPathsAttribute(String[])</code>
	 * and <code>getLibraryPaths(IIncludePathAttribute)</code> should be used to
	 * encode and decode the attribute value. 
	 * </p>
	 * <p>
	 * Each string is used to create an <code>IPath</code> using the constructor
	 * <code>Path(String)</code>, and may contain <code>IStringVariable</code>'s.
	 * Variable substitution is performed on the string prior to constructing
	 * a path from the string.
	 * If the resulting <code>IPath</code> is a relative path, it is interpreted
	 * as relative to the workspace location. If the path is absolute, it is 
	 * interpreted as an absolute path in the local file system.
	 * </p>
	 *  
	 * @see org.eclipse.wst.jsdt.core.IIncludePathAttribute
	 */
	public static final String CLASSPATH_ATTR_LIBRARY_PATH_ENTRY =  JavaScriptCore.PLUGIN_ID + ".CLASSPATH_ATTR_LIBRARY_PATH_ENTRY"; //$NON-NLS-1$

	// lock for vm initialization
	private static Object fgVMLock = new Object();
	private static boolean fgInitializingVMs = false;
	
	private static IVMInstallType[] fgVMTypes= null;
	private static String fgDefaultVMId= null;
//	private static String fgDefaultVMConnectorId = null;
	
	/**
	 * Resolvers keyed by variable name, container id,
	 * and runtime includepath entry id.
	 */
	private static Map fgVariableResolvers = null;
	private static Map fgContainerResolvers = null;
//	private static Map fgRuntimeClasspathEntryResolvers = null;
	
	/**
	 * Path providers keyed by id
	 */
//	private static Map fgPathProviders = null;
	
	/**
	 * Default includepath and source path providers.
	 */
//	private static IRuntimeClasspathProvider fgDefaultClasspathProvider = new StandardClasspathProvider();
//	private static IRuntimeClasspathProvider fgDefaultSourcePathProvider = new StandardSourcePathProvider();
	
	/**
	 * VM change listeners
	 */
	private static ListenerList fgVMListeners = new ListenerList(5);
	
	/**
	 * Cache of already resolved projects in container entries. Used to avoid
	 * cycles in project dependencies when resolving includepath container entries.
	 * Counters used to know when entering/exiting to clear cache
	 */
//	private static ThreadLocal fgProjects = new ThreadLocal(); // Lists
//	private static ThreadLocal fgEntryCount = new ThreadLocal(); // Integers
	
    /**
     *  Set of IDs of VMs contributed via vmInstalls extension point.
     */
//    private static Set fgContributedVMs = new HashSet();

	private static IVMInstall defaultVM;
    
	/**
	 * This class contains only static methods, and is not intended
	 * to be instantiated.
	 */
	private JavaRuntime() {
	}

//	/**
//	 * Initializes vm type extensions.
//	 */
//	private static void initializeVMTypeExtensions() {
//		IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(JavaPlugin.getPluginId(), "vmInstallTypes"); //$NON-NLS-1$
//		IConfigurationElement[] configs= extensionPoint.getConfigurationElements(); 
//		MultiStatus status= new MultiStatus(JavaScriptCore.PLUGIN_ID, IStatus.OK, LaunchingMessages.JavaRuntime_exceptionOccurred, null); 
//		fgVMTypes= new IVMInstallType[configs.length];
//
//		for (int i= 0; i < configs.length; i++) {
//			try {
//				IVMInstallType vmType= (IVMInstallType)configs[i].createExecutableExtension("class"); //$NON-NLS-1$
//				fgVMTypes[i]= vmType;
//			} catch (CoreException e) {
//				status.add(e.getStatus());
//			}
//		}
//		if (!status.isOK()) {
//			//only happens on a CoreException
//			JavaPlugin.log(status);
//			//cleanup null entries in fgVMTypes
//			List temp= new ArrayList(fgVMTypes.length);
//			for (int i = 0; i < fgVMTypes.length; i++) {
//				if(fgVMTypes[i] != null) {
//					temp.add(fgVMTypes[i]);
//				}
//				fgVMTypes= new IVMInstallType[temp.size()];
//				fgVMTypes= (IVMInstallType[])temp.toArray(fgVMTypes);
//			}
//		}
//	}

	/**
	 * Returns the VM assigned to build the given JavaScript project.
	 * The project must exist. The VM assigned to a project is
	 * determined from its build path.
	 * 
	 * @param project the project to retrieve the VM from
	 * @return the VM instance that is assigned to build the given JavaScript project
	 * 		   Returns <code>null</code> if no VM is referenced on the project's build path.
	 * @throws CoreException if unable to determine the project's VM install
	 */
	public static IVMInstall getVMInstall(IJavaScriptProject project) throws CoreException {
		// check the includepath
		IVMInstall vm = null;
		IIncludePathEntry[] classpath = project.getRawIncludepath();
		IRuntimeClasspathEntryResolver resolver = null;
		for (int i = 0; i < classpath.length; i++) {
			IIncludePathEntry entry = classpath[i];
			switch (entry.getEntryKind()) {
				case IIncludePathEntry.CPE_VARIABLE:
					resolver = getVariableResolver(entry.getPath().segment(0));
					if (resolver != null) {
						vm = resolver.resolveVMInstall(entry);
					}
					break;
				case IIncludePathEntry.CPE_CONTAINER:
					resolver = getContainerResolver(entry.getPath().segment(0));
					if (resolver != null) {
						vm = resolver.resolveVMInstall(entry);
					}
					break;
			}
			if (vm != null) {
				return vm;
			}
		}
		return null;
	}
	
	/**
	 * Returns the VM install type with the given unique id. 
	 * @param id the VM install type unique id
	 * @return	The VM install type for the given id, or <code>null</code> if no
	 * 			VM install type with the given id is registered.
	 */
	public static IVMInstallType getVMInstallType(String id) {
		IVMInstallType[] vmTypes= getVMInstallTypes();
		for (int i= 0; i < vmTypes.length; i++) {
			if (vmTypes[i].getId().equals(id)) {
				return vmTypes[i];
			}
		}
		return null;
	}
	
	/**
	 * Sets a VM as the system-wide default VM, and notifies registered VM install
	 * change listeners of the change.
	 * 
	 * @param vm	The vm to make the default. May be <code>null</code> to clear 
	 * 				the default.
	 * @param monitor progress monitor or <code>null</code>
	 */
	public static void setDefaultVMInstall(IVMInstall vm, IProgressMonitor monitor) throws CoreException {
		setDefaultVMInstall(vm, monitor, true);
	}	
	
	/**
	 * Sets a VM as the system-wide default VM, and notifies registered VM install
	 * change listeners of the change.
	 * 
	 * @param vm	The vm to make the default. May be <code>null</code> to clear 
	 * 				the default.
	 * @param monitor progress monitor or <code>null</code>
	 * @param savePreference If <code>true</code>, update workbench preferences to reflect
	 * 		   				  the new default VM.
	 *  
	 */
	public static void setDefaultVMInstall(IVMInstall vm, IProgressMonitor monitor, boolean savePreference) throws CoreException {
		IVMInstall previous = null;
		if (fgDefaultVMId != null) {
			previous = getVMFromCompositeId(fgDefaultVMId);
		}
		fgDefaultVMId= getCompositeIdFromVM(vm);
		if (savePreference) {
			saveVMConfiguration();
		}
		IVMInstall current = null;
		if (fgDefaultVMId != null) {
			current = getVMFromCompositeId(fgDefaultVMId);
		}
		if (previous != current) {
			notifyDefaultVMChanged(previous, current);
		}
	}
	
//	/**
//	 * Sets a VM connector as the system-wide default VM. This setting is persisted when
//	 * saveVMConfiguration is called. 
//	 * @param	connector The connector to make the default. May be <code>null</code> to clear 
//	 * 				the default.
//	 * @param monitor The progress monitor to use
//	 *  
//	 * @throws CoreException Thrown if saving the new default setting fails
//	 */
//	public static void setDefaultVMConnector(IVMConnector connector, IProgressMonitor monitor) throws CoreException {
//		fgDefaultVMConnectorId= connector.getIdentifier();
//		saveVMConfiguration();
//	}		
	
	/**
	 * Return the default VM set with <code>setDefaultVM()</code>.
	 * @return	Returns the default VM. May return <code>null</code> when no default
	 * 			VM was set or when the default VM has been disposed.
	 */
	public static IVMInstall getDefaultVMInstall() {
		IVMInstall install= getVMFromCompositeId(getDefaultVMId());
//TODO: uncommment getInstallLocation()
		if (install != null /* && install.getInstallLocation().exists()*/) {
			return install;
		}
		// if the default JRE goes missing, re-detect
		if (install != null) {
			install.getVMInstallType().disposeVMInstall(install.getId());
		}
		synchronized (fgVMLock) {
			fgDefaultVMId = null;
			fgVMTypes = null;
			initializeVMs();
		}
		return getVMFromCompositeId(getDefaultVMId());
	}
	
//	/**
//	 * Return the default VM connector.
//	 * @return	Returns the default VM connector.
//	 *  
//	 */
//	public static IVMConnector getDefaultVMConnector() {
//		String id = getDefaultVMConnectorId();
//		IVMConnector connector = null;
//		if (id != null) {
//			connector = getVMConnector(id);
//		}
//		if (connector == null) {
//			connector = new SocketAttachConnector();
//		}
//		return connector;
//	}	
	
	/**
	 * Returns the list of registered VM types. VM types are registered via
	 * <code>"org.eclipse.wst.jsdt.launching.vmTypes"</code> extension point.
	 * Returns an empty list if there are no registered VM types.
	 * 
	 * @return the list of registered VM types
	 */
	public static IVMInstallType[] getVMInstallTypes() {
		initializeVMs();
		return fgVMTypes; 
	}
	
	private static String getDefaultVMId() {
		initializeVMs();
		return fgDefaultVMId;
	}
	
//	private static String getDefaultVMConnectorId() {
//		initializeVMs();
//		return fgDefaultVMConnectorId;
//	}	
//	
	/** 
	 * Returns a String that uniquely identifies the specified VM across all VM types.
	 * 
	 * @param vm the instance of IVMInstallType to be identified
	 * 
	 *  
	 */
	public static String getCompositeIdFromVM(IVMInstall vm) {
		if (vm == null) {
			return null;
		}
		//TODO: implement
		throw new org.eclipse.wst.jsdt.core.UnimplementedException();
//		IVMInstallType vmType= vm.getVMInstallType();
//		String typeID= vmType.getId();
//		CompositeId id= new CompositeId(new String[] { typeID, vm.getId() });
//		return id.toString();
	}
	
	/**
	 * Return the VM corresponding to the specified composite Id.  The id uniquely
	 * identifies a VM across all vm types.  
	 * 
	 * @param idString the composite id that specifies an instance of IVMInstall
	 * 
	 *  
	 */
	public static IVMInstall getVMFromCompositeId(String idString) {
		if (idString == null || idString.length() == 0) {
			return null;
		}
		return defaultVM;
//		CompositeId id= CompositeId.fromString(idString);
//		if (id.getPartCount() == 2) {
//			IVMInstallType vmType= getVMInstallType(id.get(0));
//			if (vmType != null) {
//				return vmType.findVMInstall(id.get(1));
//			}
//		}
//		return null;
	}

//	/**
//	 * Returns a new runtime includepath entry for the given expression that
//	 * may contain string substitution variable references. The resulting expression
//	 * refers to an archive (jar or directory) containing class files.
//	 * 
//	 * @param expression an expression that resolves to the location of an archive
//	 * @return runtime includepath entry
//	 *  
//	 */
//	public static IRuntimeClasspathEntry newStringVariableClasspathEntry(String expression) {
//		return new VariableClasspathEntry(expression);
//	}
//	
//	/**
//	 * Returns a new runtime includepath entry containing the default includepath
//	 * for the specified JavaScript project. 
//	 * 
//	 * @param project JavaScript project
//	 * @return runtime includepath entry
//	 *  
//	 */
//	public static IRuntimeClasspathEntry newDefaultProjectClasspathEntry(IJavaScriptProject project) {
//		return new DefaultProjectClasspathEntry(project);
//	}	
	
	/**
	 * Returns a new runtime includepath entry for the given project.
	 * 
	 * @param project JavaScript project
	 * @return runtime includepath entry
	 *  
	 */
	public static IRuntimeClasspathEntry newProjectRuntimeClasspathEntry(IJavaScriptProject project) {
		IIncludePathEntry cpe = JavaScriptCore.newProjectEntry(project.getProject().getFullPath());
		return newRuntimeClasspathEntry(cpe);
	}
	
	
	/**
	 * Returns a new runtime includepath entry for the given archive.
	 * 
	 * @param resource archive resource
	 * @return runtime includepath entry
	 *  
	 */
	public static IRuntimeClasspathEntry newArchiveRuntimeClasspathEntry(IResource resource) {
		IIncludePathEntry cpe = JavaScriptCore.newLibraryEntry(resource.getFullPath(), null, null);
		return newRuntimeClasspathEntry(cpe);
	}
	
	/**
	 * Returns a new runtime includepath entry for the given archive (possibly
	 * external).
	 * 
	 * @param path absolute path to an archive
	 * @return runtime includepath entry
	 *  
	 */
	public static IRuntimeClasspathEntry newArchiveRuntimeClasspathEntry(IPath path) {
		IIncludePathEntry cpe = JavaScriptCore.newLibraryEntry(path, null, null);
		return newRuntimeClasspathEntry(cpe);
	}

	/**
	 * Returns a new runtime includepath entry for the includepath
	 * variable with the given path.
	 * 
	 * @param path variable path; first segment is the name of the variable; 
	 * 	trailing segments are appended to the resolved variable value
	 * @return runtime includepath entry
	 *  
	 */
	public static IRuntimeClasspathEntry newVariableRuntimeClasspathEntry(IPath path) {
		IIncludePathEntry cpe = JavaScriptCore.newVariableEntry(path, null, null);
		return newRuntimeClasspathEntry(cpe);
	}

	/**
	 * Returns a runtime includepath entry for the given container path with the given
	 * includepath property.
	 * 
	 * @param path container path
	 * @param includepathProperty the type of entry - one of <code>USER_CLASSES</code>,
	 * 	<code>BOOTSTRAP_CLASSES</code>, or <code>STANDARD_CLASSES</code>
	 * @return runtime includepath entry
	 * @exception CoreException if unable to construct a runtime includepath entry
	 *  
	 */
	public static IRuntimeClasspathEntry newRuntimeContainerClasspathEntry(IPath path, int classpathProperty) throws CoreException {
		return newRuntimeContainerClasspathEntry(path, classpathProperty, null);
	}
	
	/**
	 * Returns a runtime includepath entry for the given container path with the given
	 * includepath property to be resolved in the context of the given JavaScript project.
	 * 
	 * @param path container path
	 * @param includepathProperty the type of entry - one of <code>USER_CLASSES</code>,
	 * 	<code>BOOTSTRAP_CLASSES</code>, or <code>STANDARD_CLASSES</code>
	 * @param project JavaScript project context used for resolution, or <code>null</code>
	 *  if to be resolved in the context of the launch configuration this entry
	 *  is referenced in
	 * @return runtime includepath entry
	 * @exception CoreException if unable to construct a runtime includepath entry
	 *  
	 */
	public static IRuntimeClasspathEntry newRuntimeContainerClasspathEntry(IPath path, int classpathProperty, IJavaScriptProject project) throws CoreException {
//		IIncludePathEntry cpe = JavaScriptCore.newContainerEntry(path);
//		RuntimeClasspathEntry entry = new RuntimeClasspathEntry(cpe, classpathProperty);
//		entry.setJavaProject(project);
//		return entry;
		//TODO: implement
		throw new org.eclipse.wst.jsdt.core.UnimplementedException();
	}	
		
	/**
	 * Returns a runtime includepath entry constructed from the given memento.
	 * 
	 * @param memento a memento for a runtime includepath entry
	 * @return runtime includepath entry
	 * @exception CoreException if unable to construct a runtime includepath entry
	 *  
	 */
	public static IRuntimeClasspathEntry newRuntimeClasspathEntry(String memento) throws CoreException {
//		try {
//			Element root = null;
//			DocumentBuilder parser = LaunchingPlugin.getParser();
//			StringReader reader = new StringReader(memento);
//			InputSource source = new InputSource(reader);
//			root = parser.parse(source).getDocumentElement();
//												
//			String id = root.getAttribute("id"); //$NON-NLS-1$
//			if (id == null || id.length() == 0) {
//				// assume an old format
//				return new RuntimeClasspathEntry(root);
//			}
//			// get the extension & create a new one
//			IRuntimeClasspathEntry2 entry = LaunchingPlugin.getDefault().newRuntimeClasspathEntry(id);
//			NodeList list = root.getChildNodes();
//			for (int i = 0; i < list.getLength(); i++) {
//				Node node = list.item(i);
//				if (node.getNodeType() == Node.ELEMENT_NODE) {
//					Element element = (Element)node;
//					if ("memento".equals(element.getNodeName())) { //$NON-NLS-1$
//						entry.initializeFrom(element);
//					}
//				}
//			}
//			return entry;
//		} catch (SAXException e) {
//			abort(LaunchingMessages.JavaRuntime_31, e); 
//		} catch (IOException e) {
//			abort(LaunchingMessages.JavaRuntime_32, e); 
//		}
//		return null;
		//TODO: implement
		throw new org.eclipse.wst.jsdt.core.UnimplementedException();
	}
	
	/**
	 * Returns a runtime includepath entry that corresponds to the given
	 * includepath entry. The includepath entry may not be of type <code>CPE_SOURCE</code>
	 * or <code>CPE_CONTAINER</code>.
	 * 
	 * @param entry a includepath entry
	 * @return runtime includepath entry
	 *  
	 */
	private static IRuntimeClasspathEntry newRuntimeClasspathEntry(IIncludePathEntry entry) {
//		return new RuntimeClasspathEntry(entry);
		//TODO: implement
		throw new org.eclipse.wst.jsdt.core.UnimplementedException();
	}	
			
	/**
	 * Computes and returns the default unresolved runtime includepath for the
	 * given project.
	 * 
	 * @return runtime includepath entries
	 * @exception CoreException if unable to compute the runtime includepath
	 * @see IRuntimeClasspathEntry
	 *  
	 */
	public static IRuntimeClasspathEntry[] computeUnresolvedRuntimeClasspath(IJavaScriptProject project) throws CoreException {
//		IIncludePathEntry[] entries = project.getRawClasspath();
//		List includepathEntries = new ArrayList(3);
//		for (int i = 0; i < entries.length; i++) {
//			IIncludePathEntry entry = entries[i];
//			switch (entry.getEntryKind()) {
//				case IIncludePathEntry.CPE_CONTAINER:
//					IJsGlobalScopeContainer container = JavaScriptCore.getJsGlobalScopeContainer(entry.getPath(), project);
//					if (container != null) {
//						switch (container.getKind()) {
//							case IJsGlobalScopeContainer.K_APPLICATION:
//								// don't look at application entries
//								break;
//							case IJsGlobalScopeContainer.K_DEFAULT_SYSTEM:
//								includepathEntries.add(newRuntimeContainerClasspathEntry(container.getPath(), IRuntimeClasspathEntry.STANDARD_CLASSES, project));
//								break;	
//							case IJsGlobalScopeContainer.K_SYSTEM:
//								includepathEntries.add(newRuntimeContainerClasspathEntry(container.getPath(), IRuntimeClasspathEntry.BOOTSTRAP_CLASSES, project));
//								break;
//						}						
//					}
//					break;
//				case IIncludePathEntry.CPE_VARIABLE:
//					if (JRELIB_VARIABLE.equals(entry.getPath().segment(0))) {
//						IRuntimeClasspathEntry jre = newVariableRuntimeClasspathEntry(entry.getPath());
//						jre.setClasspathProperty(IRuntimeClasspathEntry.STANDARD_CLASSES);
//						includepathEntries.add(jre);
//					}
//					break;
//				default:
//					break;
//			}
//		}
//		includepathEntries.add(newDefaultProjectClasspathEntry(project));
//		return (IRuntimeClasspathEntry[]) includepathEntries.toArray(new IRuntimeClasspathEntry[includepathEntries.size()]);
//		
//TODO: implement
throw new org.eclipse.wst.jsdt.core.UnimplementedException();
	}
	
//	/**
//	 * Computes and returns the unresolved source lookup path for the given launch
//	 * configuration.
//	 * 
//	 * @param configuration launch configuration
//	 * @return runtime includepath entries
//	 * @exception CoreException if unable to compute the source lookup path
//	 *  
//	 */
//	public static IRuntimeClasspathEntry[] computeUnresolvedSourceLookupPath(ILaunchConfiguration configuration) throws CoreException {
//		return getSourceLookupPathProvider(configuration).computeUnresolvedClasspath(configuration);
//	}
//	
//	/**
//	 * Resolves the given source lookup path, returning the resolved source lookup path
//	 * in the context of the given launch configuration.
//	 * 
//	 * @param entries unresolved entries
//	 * @param configuration launch configuration
//	 * @return resolved entries
//	 * @exception CoreException if unable to resolve the source lookup path
//	 *  
//	 */
//	public static IRuntimeClasspathEntry[] resolveSourceLookupPath(IRuntimeClasspathEntry[] entries, ILaunchConfiguration configuration) throws CoreException {
//		return getSourceLookupPathProvider(configuration).resolveClasspath(entries, configuration);
//	}	
//	
//	/**
//	 * Returns the includepath provider for the given launch configuration.
//	 * 
//	 * @param configuration launch configuration
//	 * @return includepath provider
//	 * @exception CoreException if unable to resolve the path provider
//	 *  
//	 */
//	public static IRuntimeClasspathProvider getClasspathProvider(ILaunchConfiguration configuration) throws CoreException {
//		String providerId = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, (String)null);
//		IRuntimeClasspathProvider provider = null;
//		if (providerId == null) {
//			provider = fgDefaultClasspathProvider;
//		} else {
//			provider = (IRuntimeClasspathProvider)getClasspathProviders().get(providerId);
//			if (provider == null) {
//				abort(MessageFormat.format(LaunchingMessages.JavaRuntime_26, new String[]{providerId}), null); 
//			}
//		}
//		return provider;
//	}	
//		
//	/**
//	 * Returns the source lookup path provider for the given launch configuration.
//	 * 
//	 * @param configuration launch configuration
//	 * @return source lookup path provider
//	 * @exception CoreException if unable to resolve the path provider
//	 *  
//	 */
//	public static IRuntimeClasspathProvider getSourceLookupPathProvider(ILaunchConfiguration configuration) throws CoreException {
//		String providerId = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, (String)null);
//		IRuntimeClasspathProvider provider = null;
//		if (providerId == null) {
//			provider = fgDefaultSourcePathProvider;
//		} else {
//			provider = (IRuntimeClasspathProvider)getClasspathProviders().get(providerId);
//			if (provider == null) {
//				abort(MessageFormat.format(LaunchingMessages.JavaRuntime_27, new String[]{providerId}), null); 
//			}
//		}
//		return provider;
//	}	
		
	/**
	 * Returns resolved entries for the given entry in the context of the given
	 * launch configuration. If the entry is of kind
	 * <code>VARIABLE</code> or <code>CONTAINER</code>, variable and container
	 * resolvers are consulted. If the entry is of kind <code>PROJECT</code>,
	 * and the associated JavaScript project specifies non-default output locations,
	 * the corresponding output locations are returned. Otherwise, the given
	 * entry is returned.
	 * <p>
	 * If the given entry is a variable entry, and a resolver is not registered,
	 * the entry itself is returned. If the given entry is a container, and a
	 * resolver is not registered, resolved runtime includepath entries are calculated
	 * from the associated container includepath entries, in the context of the project
	 * associated with the given launch configuration.
	 * </p>
	 * @param entry runtime includepath entry
	 * @param configuration launch configuration
	 * @return resolved runtime includepath entry
	 * @exception CoreException if unable to resolve
	 * @see IRuntimeClasspathEntryResolver
	 *  
	 */
	public static IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry entry, ILaunchConfiguration configuration) throws CoreException {
		//TODO: implement
		throw new org.eclipse.wst.jsdt.core.UnimplementedException();
//		switch (entry.getType()) {
//			case IRuntimeClasspathEntry.PROJECT:
//				// if the project has multiple output locations, they must be returned
//				IResource resource = entry.getResource();
//				if (resource instanceof IProject) {
//					IProject p = (IProject)resource;
//					IJavaScriptProject project = JavaScriptCore.create(p);
//					if (project == null || !p.isOpen() || !project.exists()) { 
//						return new IRuntimeClasspathEntry[0];
//					}
//					IRuntimeClasspathEntry[] entries = resolveOutputLocations(project, entry.getClasspathProperty());
//					if (entries != null) {
//						return entries;
//					}
//				} else {
//					// could not resolve project
//					abort(MessageFormat.format(LaunchingMessages.JavaRuntime_Classpath_references_non_existant_project___0__3, new String[]{entry.getPath().lastSegment()}), null); 
//				}
//				break;
//			case IRuntimeClasspathEntry.VARIABLE:
//				IRuntimeClasspathEntryResolver resolver = getVariableResolver(entry.getVariableName());
//				if (resolver == null) {
//					IRuntimeClasspathEntry[] resolved = resolveVariableEntry(entry, null, configuration);
//					if (resolved != null) { 
//						return resolved;
//					}
//					break;
//				} 
//				return resolver.resolveRuntimeClasspathEntry(entry, configuration);				
//			case IRuntimeClasspathEntry.CONTAINER:
//				resolver = getContainerResolver(entry.getVariableName());
//				if (resolver == null) {
//					return computeDefaultContainerEntries(entry, configuration);
//				} 
//				return resolver.resolveRuntimeClasspathEntry(entry, configuration);
//			case IRuntimeClasspathEntry.ARCHIVE:
//				// verify the archive exists
//				String location = entry.getLocation();
//				if (location == null) {
//					abort(MessageFormat.format(LaunchingMessages.JavaRuntime_Classpath_references_non_existant_archive___0__4, new String[]{entry.getPath().toString()}), null); 
//				}
//				File file = new File(location);
//				if (!file.exists()) {
//					abort(MessageFormat.format(LaunchingMessages.JavaRuntime_Classpath_references_non_existant_archive___0__4, new String[]{entry.getPath().toString()}), null); 
//				}
//				break;
//			case IRuntimeClasspathEntry.OTHER:
//				resolver = getContributedResolver(((IRuntimeClasspathEntry2)entry).getTypeId());
//				return resolver.resolveRuntimeClasspathEntry(entry, configuration);
//			default:
//				break;
//		}
//		return new IRuntimeClasspathEntry[] {entry};
	}
	
//	/**
//	 * Default resolution for a includepath variable - resolve to an archive. Only
//	 * one of project/configuration can be non-null.
//	 * 
//	 * @param entry
//	 * @param project the project context or <code>null</code>
//	 * @param configuration configuration context or <code>null</code>
//	 * @return IRuntimeClasspathEntry[]
//	 * @throws CoreException
//	 */
//	private static IRuntimeClasspathEntry[] resolveVariableEntry(IRuntimeClasspathEntry entry, IJavaScriptProject project, ILaunchConfiguration configuration) throws CoreException {
//		// default resolution - an archive
//		IPath archPath = JavaScriptCore.getClasspathVariable(entry.getVariableName());
//		if (archPath != null) {
//			if (entry.getPath().segmentCount() > 1) {
//				archPath = archPath.append(entry.getPath().removeFirstSegments(1));
//			}
//			IPath srcPath = null;
//			IPath srcVar = entry.getSourceAttachmentPath();
//			IPath srcRootPath = null;
//			IPath srcRootVar = entry.getSourceAttachmentRootPath();
//			if (archPath != null && !archPath.isEmpty()) {
//				if (srcVar != null && !srcVar.isEmpty()) {
//					srcPath = JavaScriptCore.getClasspathVariable(srcVar.segment(0));
//					if (srcPath != null) {
//						if (srcVar.segmentCount() > 1) {
//							srcPath = srcPath.append(srcVar.removeFirstSegments(1));
//						}
//						if (srcRootVar != null && !srcRootVar.isEmpty()) {
//							srcRootPath = JavaScriptCore.getClasspathVariable(srcRootVar.segment(0));	
//							if (srcRootPath != null) {
//								if (srcRootVar.segmentCount() > 1) {
//									srcRootPath = srcRootPath.append(srcRootVar.removeFirstSegments(1));					
//								}
//							}
//						}
//					}
//				}
//				// now resolve the archive (recursively)
//				IIncludePathEntry archEntry = JavaScriptCore.newLibraryEntry(archPath, srcPath, srcRootPath, entry.getClasspathEntry().isExported());
//				IRuntimeClasspathEntry runtimeArchEntry = newRuntimeClasspathEntry(archEntry);
//				runtimeArchEntry.setClasspathProperty(entry.getClasspathProperty());
//				if (configuration == null) {
//					return resolveRuntimeClasspathEntry(runtimeArchEntry, project);
//				} 
//				return resolveRuntimeClasspathEntry(runtimeArchEntry, configuration);
//			}		
//		}
//		return null;
//	}
//	
//	/**
//	 * Returns runtime includepath entries corresponding to the output locations
//	 * of the given project, or null if the project only uses the default
//	 * output location.
//	 * 
//	 * @param project
//	 * @param includepathProperty the type of includepath entries to create
//	 * @return IRuntimeClasspathEntry[] or <code>null</code>
//	 * @throws CoreException
//	 */
//	private static IRuntimeClasspathEntry[] resolveOutputLocations(IJavaScriptProject project, int includepathProperty) throws CoreException {
//		List nonDefault = new ArrayList();
//		if (project.exists() && project.getProject().isOpen()) {
//			IIncludePathEntry entries[] = project.getRawClasspath();
//			for (int i = 0; i < entries.length; i++) {
//				IIncludePathEntry includepathEntry = entries[i];
//				if (includepathEntry.getEntryKind() == IIncludePathEntry.CPE_SOURCE) {
//					IPath path = includepathEntry.getOutputLocation();
//					if (path != null) {
//						nonDefault.add(path);
//					}
//				}
//			}
//		}
//		if (nonDefault.isEmpty()) {
//			return null; 
//		} 
//		// add the default location if not already included
//		IPath def = project.getOutputLocation();
//		if (!nonDefault.contains(def)) {
//			nonDefault.add(def);						
//		}
//		IRuntimeClasspathEntry[] locations = new IRuntimeClasspathEntry[nonDefault.size()];
//		for (int i = 0; i < locations.length; i++) {
//			IIncludePathEntry newEntry = JavaScriptCore.newLibraryEntry((IPath)nonDefault.get(i), null, null);
//			locations[i] = new RuntimeClasspathEntry(newEntry);
//			locations[i].setClasspathProperty(includepathProperty);
//		}
//		return locations;						
//	}
	
	/**
	 * Returns resolved entries for the given entry in the context of the given
	 * JavaScript project. If the entry is of kind
	 * <code>VARIABLE</code> or <code>CONTAINER</code>, variable and container
	 * resolvers are consulted. If the entry is of kind <code>PROJECT</code>,
	 * and the associated JavaScript project specifies non-default output locations,
	 * the corresponding output locations are returned. Otherwise, the given
	 * entry is returned.
	 * <p>
	 * If the given entry is a variable entry, and a resolver is not registered,
	 * the entry itself is returned. If the given entry is a container, and a
	 * resolver is not registered, resolved runtime includepath entries are calculated
	 * from the associated container includepath entries, in the context of the 
	 * given project.
	 * </p>
	 * @param entry runtime includepath entry
	 * @param project JavaScript project context
	 * @return resolved runtime includepath entry
	 * @exception CoreException if unable to resolve
	 * @see IRuntimeClasspathEntryResolver
	 *  
	 */
	public static IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry entry, IJavaScriptProject project) throws CoreException {
//		switch (entry.getType()) {
//			case IRuntimeClasspathEntry.PROJECT:
//				// if the project has multiple output locations, they must be returned
//				IResource resource = entry.getResource();
//				if (resource instanceof IProject) {
//					IProject p = (IProject)resource;
//					IJavaScriptProject jp = JavaScriptCore.create(p);
//					if (jp != null && p.isOpen() && jp.exists()) {
//						IRuntimeClasspathEntry[] entries = resolveOutputLocations(jp, entry.getClasspathProperty());
//						if (entries != null) {
//							return entries;
//						}
//					} else {
//						return new IRuntimeClasspathEntry[0];
//					}
//				}
//				break;			
//			case IRuntimeClasspathEntry.VARIABLE:
//				IRuntimeClasspathEntryResolver resolver = getVariableResolver(entry.getVariableName());
//				if (resolver == null) {
//					IRuntimeClasspathEntry[] resolved = resolveVariableEntry(entry, project, null);
//					if (resolved != null) { 
//						return resolved;
//					}
//					break;
//				} 
//				return resolver.resolveRuntimeClasspathEntry(entry, project);				
//			case IRuntimeClasspathEntry.CONTAINER:
//				resolver = getContainerResolver(entry.getVariableName());
//				if (resolver == null) {
//					return computeDefaultContainerEntries(entry, project);
//				} 
//				return resolver.resolveRuntimeClasspathEntry(entry, project);
//			case IRuntimeClasspathEntry.OTHER:
//				resolver = getContributedResolver(((IRuntimeClasspathEntry2)entry).getTypeId());
//				return resolver.resolveRuntimeClasspathEntry(entry, project);				
//			default:
//				break;
//		}
//		return new IRuntimeClasspathEntry[] {entry};
		//TODO: implement
		throw new org.eclipse.wst.jsdt.core.UnimplementedException();
	}	
		
	/**
	 * Performs default resolution for a container entry.
	 * Delegates to the JavaScript model.
	 */
//	private static IRuntimeClasspathEntry[] computeDefaultContainerEntries(IRuntimeClasspathEntry entry, ILaunchConfiguration config) throws CoreException {
//		IJavaScriptProject project = entry.getJavaProject();
//		if (project == null) {
//			project = getJavaProject(config);
//		}
//		return computeDefaultContainerEntries(entry, project);
//	}
	
	/**
	 * Performs default resolution for a container entry.
	 * Delegates to the JavaScript model.
	 */
//	private static IRuntimeClasspathEntry[] computeDefaultContainerEntries(IRuntimeClasspathEntry entry, IJavaScriptProject project) throws CoreException {
//		//TODO: implement
//		throw new org.eclipse.wst.jsdt.core.UnimplementedException();
////		if (project == null || entry == null) {
////			// cannot resolve without entry or project context
////			return new IRuntimeClasspathEntry[0];
////		} 
////		IJsGlobalScopeContainer container = JavaScriptCore.getJsGlobalScopeContainer(entry.getPath(), project);
////		if (container == null) {
////			abort(MessageFormat.format(LaunchingMessages.JavaRuntime_Could_not_resolve_includepath_container___0__1, new String[]{entry.getPath().toString()}), null); 
////			// execution will not reach here - exception will be thrown
////			return null;
////		} 
////		IIncludePathEntry[] cpes = container.getClasspathEntries();
////		int property = -1;
////		switch (container.getKind()) {
////			case IJsGlobalScopeContainer.K_APPLICATION:
////				property = IRuntimeClasspathEntry.USER_CLASSES;
////				break;
////			case IJsGlobalScopeContainer.K_DEFAULT_SYSTEM:
////				property = IRuntimeClasspathEntry.STANDARD_CLASSES;
////				break;	
////			case IJsGlobalScopeContainer.K_SYSTEM:
////				property = IRuntimeClasspathEntry.BOOTSTRAP_CLASSES;
////				break;
////		}			
////		List resolved = new ArrayList(cpes.length);
////		List projects = (List) fgProjects.get();
////		Integer count = (Integer) fgEntryCount.get();
////		if (projects == null) {
////			projects = new ArrayList();
////			fgProjects.set(projects);
////			count = Integer.valueOf(0);
////		}
////		int intCount = count.intValue();
////		intCount++;
////		fgEntryCount.set(Integer.valueOf(intCount));
////		try {
////			for (int i = 0; i < cpes.length; i++) {
////				IIncludePathEntry cpe = cpes[i];
////				if (cpe.getEntryKind() == IIncludePathEntry.CPE_PROJECT) {
////					IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(cpe.getPath().segment(0));
////					IJavaScriptProject jp = JavaScriptCore.create(p);
////					if (!projects.contains(jp)) {
////						projects.add(jp);
////						IRuntimeClasspathEntry includepath = newDefaultProjectClasspathEntry(jp);
////						IRuntimeClasspathEntry[] entries = resolveRuntimeClasspathEntry(includepath, jp);
////						for (int j = 0; j < entries.length; j++) {
////							IRuntimeClasspathEntry e = entries[j];
////							if (!resolved.contains(e)) {
////								resolved.add(entries[j]);
////							}
////						}
////					}
////				} else {
////					IRuntimeClasspathEntry e = newRuntimeClasspathEntry(cpe);
////					if (!resolved.contains(e)) {
////						resolved.add(e);
////					}
////				}
////			}
////		} finally {
////			intCount--;
////			if (intCount == 0) {
////				fgProjects.set(null);
////				fgEntryCount.set(null);
////			} else {
////				fgEntryCount.set(Integer.valueOf(intCount));
////			}
////		}
////		// set includepath property
////		IRuntimeClasspathEntry[] result = new IRuntimeClasspathEntry[resolved.size()];
////		for (int i = 0; i < result.length; i++) {
////			result[i] = (IRuntimeClasspathEntry) resolved.get(i);
////			result[i].setClasspathProperty(property);
////		}
////		return result;
//	}
			
	/**
	 * Computes and returns the unresolved class path for the given launch configuration.
	 * Variable and container entries are unresolved.
	 * 
	 * @param configuration launch configuration
	 * @return unresolved runtime includepath entries
	 * @exception CoreException if unable to compute the includepath
	 *  
	 */
	public static IRuntimeClasspathEntry[] computeUnresolvedRuntimeClasspath(ILaunchConfiguration configuration) throws CoreException {
//		return getClasspathProvider(configuration).computeUnresolvedClasspath(configuration);
		//TODO: implement
		throw new org.eclipse.wst.jsdt.core.UnimplementedException();

	}
	
	/**
	 * Resolves the given includepath, returning the resolved includepath
	 * in the context of the given launch configuration.
	 *
	 * @param entries unresolved includepath
	 * @param configuration launch configuration
	 * @return resolved runtime includepath entries
	 * @exception CoreException if unable to compute the includepath
	 *  
	 */
	public static IRuntimeClasspathEntry[] resolveRuntimeClasspath(IRuntimeClasspathEntry[] entries, ILaunchConfiguration configuration) throws CoreException {
//		TODO: implement
		throw new org.eclipse.wst.jsdt.core.UnimplementedException();
//		return getClasspathProvider(configuration).resolveClasspath(entries, configuration);
	}	
	
	/**
	 * Return the <code>IJavaScriptProject</code> referenced in the specified configuration or
	 * <code>null</code> if none.
	 *
	 * @exception CoreException if the referenced JavaScript project does not exist
	 *  
	 */
	public static IJavaScriptProject getJavaProject(ILaunchConfiguration configuration) throws CoreException {
////TODO: implement
//throw new org.eclipse.wst.jsdt.core.UnimplementedException();
				String projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String)null);
		if ((projectName == null) || (projectName.trim().length() < 1)) {
			return null;
		}			
		IJavaScriptProject javaProject = getJavaModel().getJavaScriptProject(projectName);
		if (javaProject != null && javaProject.getProject().exists() && !javaProject.getProject().isOpen()) {
			abort(MessageFormat.format(LaunchingMessages.JavaRuntime_28, new String[] {configuration.getName(), projectName}), IJavaLaunchConfigurationConstants.ERR_PROJECT_CLOSED, null); 
		}
		if ((javaProject == null) || !javaProject.exists()) {
			abort(MessageFormat.format(LaunchingMessages.JavaRuntime_Launch_configuration__0__references_non_existing_project__1___1, new String[] {configuration.getName(), projectName}), IJavaLaunchConfigurationConstants.ERR_NOT_A_JAVA_PROJECT, null); 
		}
		return javaProject;
	}
				
	/**
	 * Convenience method to get the javascript model.
	 */
	private static IJavaScriptModel getJavaModel() {
		return JavaScriptCore.create(ResourcesPlugin.getWorkspace().getRoot());
	}
	
	/**
	 * Returns the VM install for the given launch configuration.
	 * The VM install is determined in the following prioritized way:
	 * <ol>
	 * <li>The VM install is explicitly specified on the launch configuration
	 *  via the <code>ATTR_JRE_CONTAINER_PATH</code> attribute (since 3.2).</li>
	 * <li>The VM install is explicitly specified on the launch configuration
	 * 	via the <code>ATTR_VM_INSTALL_TYPE</code> and <code>ATTR_VM_INSTALL_ID</code>
	 *  attributes.</li>
	 * <li>If no explicit VM install is specified, the VM install associated with
	 * 	the launch configuration's project is returned.</li>
	 * <li>If no project is specified, or the project does not specify a custom
	 * 	VM install, the workspace default VM install is returned.</li>
	 * </ol>
	 * 
	 * @param configuration launch configuration
	 * @return vm install
	 * @exception CoreException if unable to compute a vm install
	 *  
	 */
	public static IVMInstall computeVMInstall(ILaunchConfiguration configuration) throws CoreException {
		String jreAttr = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, (String)null);
		if (jreAttr == null) {
			String type = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, (String)null);
			if (type == null) {
				IJavaScriptProject proj = getJavaProject(configuration);
				if (proj != null) {
					IVMInstall vm = getVMInstall(proj);
					if (vm != null) {
						return vm;
					}
				}
			} else {
				String name = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_NAME, (String)null);
				return resolveVM(type, name, configuration);
			}
		} else {
			IPath jrePath = Path.fromPortableString(jreAttr);
			IIncludePathEntry entry = JavaScriptCore.newContainerEntry(jrePath);
			IRuntimeClasspathEntryResolver2 resolver = getVariableResolver(jrePath.segment(0));
			if (resolver != null) {
				return resolver.resolveVMInstall(entry);
			} else {
				resolver = getContainerResolver(jrePath.segment(0));
				if (resolver != null) {
					return resolver.resolveVMInstall(entry);
				}
			}
		}
		
		return getDefaultVMInstall();
	}
	/**
	 * Returns the VM of the given type with the specified name.
	 *  
	 * @param type vm type identifier
	 * @param name vm name
	 * @return vm install
	 * @exception CoreException if unable to resolve
	 *  
	 */
	private static IVMInstall resolveVM(String type, String name, ILaunchConfiguration configuration) throws CoreException {
		IVMInstallType vt = getVMInstallType(type);
		if (vt == null) {
			// error type does not exist
			abort(MessageFormat.format(LaunchingMessages.JavaRuntime_Specified_VM_install_type_does_not_exist___0__2, new String[] {type}), null); 
		}
		IVMInstall vm = null;
		// look for a name
		if (name == null) {
			// error - type specified without a specific install (could be an old config that specified a VM ID)
			// log the error, but choose the default VM.
			IStatus status = new Status(IStatus.WARNING, JavaScriptCore.PLUGIN_ID, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_VM_INSTALL, MessageFormat.format(LaunchingMessages.JavaRuntime_VM_not_fully_specified_in_launch_configuration__0____missing_VM_name__Reverting_to_default_VM__1, new String[] {configuration.getName()}), null); 
			JavaScriptCore.getPlugin().getLog().log(status);
			return getDefaultVMInstall();
		} 
		vm = vt.findVMInstallByName(name);
		if (vm == null) {
			// error - install not found
			abort(MessageFormat.format(LaunchingMessages.JavaRuntime_Specified_VM_install_not_found__type__0___name__1__2, new String[] {vt.getName(), name}), null);					 
		} else {
			return vm;
		}
		// won't reach here
		return null;
	}
	
	/**
	 * Throws a core exception with an internal error status.
	 * 
	 * @param message the status message
	 * @param exception lower level exception associated with the
	 *  error, or <code>null</code> if none
	 */
	private static void abort(String message, Throwable exception) throws CoreException {
		abort(message, IJavaLaunchConfigurationConstants.ERR_INTERNAL_ERROR, exception);
	}	
		
		
	/**
	 * Throws a core exception with an internal error status.
	 * 
	 * @param message the status message
	 * @param code status code
	 * @param exception lower level exception associated with the
	 * 
	 *  error, or <code>null</code> if none
	 */
	private static void abort(String message, int code, Throwable exception) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, code, message, exception));
	}	
		
	/**
	 * Computes the default application includepath entries for the given 
	 * project.
	 * 
	 * @param	jproject The project to compute the includepath for
	 * @return	The computed includepath. May be empty, but not null.
	 * @throws	CoreException if unable to compute the default includepath
	 */
	public static String[] computeDefaultRuntimeClassPath(IJavaScriptProject jproject) throws CoreException {
		IRuntimeClasspathEntry[] unresolved = computeUnresolvedRuntimeClasspath(jproject);
		// 1. remove bootpath entries
		// 2. resolve & translate to local file system paths
		List resolved = new ArrayList(unresolved.length);
		for (int i = 0; i < unresolved.length; i++) {
			IRuntimeClasspathEntry entry = unresolved[i];
			if (entry.getClasspathProperty() == IRuntimeClasspathEntry.USER_CLASSES) {
				IRuntimeClasspathEntry[] entries = resolveRuntimeClasspathEntry(entry, jproject);
				for (int j = 0; j < entries.length; j++) {
					String location = entries[j].getLocation();
					if (location != null) {
						resolved.add(location); 
					}
				}
			}
		}
		return (String[])resolved.toArray(new String[resolved.size()]);
	}	
		
	/**
	 * Saves the VM configuration information to the preferences. This includes
	 * the following information:
	 * <ul>
	 * <li>The list of all defined IVMInstall instances.</li>
	 * <li>The default VM</li>
	 * <ul>
	 * This state will be read again upon first access to VM
	 * configuration information.
	 */
	public static void saveVMConfiguration() throws CoreException {
		if (fgVMTypes == null) {
			// if the VM types have not been instantiated, there can be no changes.
			return;
		}
		//TODO: implement
		throw new org.eclipse.wst.jsdt.core.UnimplementedException();
//		try {
//			String xml = getVMsAsXML();
//			getPreferences().setValue(PREF_VM_XML, xml);
//			savePreferences();
//		} catch (IOException e) {
//			throw new CoreException(new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, IStatus.ERROR, LaunchingMessages.JavaRuntime_exceptionsOccurred, e)); 
//		} catch (ParserConfigurationException e) {
//			throw new CoreException(new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, IStatus.ERROR, LaunchingMessages.JavaRuntime_exceptionsOccurred, e)); 
//		} catch (TransformerException e) {
//			throw new CoreException(new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, IStatus.ERROR, LaunchingMessages.JavaRuntime_exceptionsOccurred, e)); 
//		}
	}
	
//	private static String getVMsAsXML() throws IOException, ParserConfigurationException, TransformerException {
//		VMDefinitionsContainer container = new VMDefinitionsContainer();	
//		container.setDefaultVMInstallCompositeID(getDefaultVMId());
//		container.setDefaultVMInstallConnectorTypeID(getDefaultVMConnectorId());	
//		IVMInstallType[] vmTypes= getVMInstallTypes();
//		for (int i = 0; i < vmTypes.length; ++i) {
//			IVMInstall[] vms = vmTypes[i].getVMInstalls();
//			for (int j = 0; j < vms.length; j++) {
//				IVMInstall install = vms[j];
//				container.addVM(install);
//			}
//		}
//		return container.getAsXML();
//	}
//	
//	/**
//	 * This method loads installed JREs based an existing user preference
//	 * or old vm configurations file. The VMs found in the preference
//	 * or vm configurations file are added to the given VM definitions container.
//	 * 
//	 * Returns whether the user preferences should be set - i.e. if it was
//	 * not already set when initialized.
//	 */
//	private static boolean addPersistedVMs(VMDefinitionsContainer vmDefs) throws IOException {
//		// Try retrieving the VM preferences from the preference store
//		String vmXMLString = getPreferences().getString(PREF_VM_XML);
//		
//		// If the preference was found, load VMs from it into memory
//		if (vmXMLString.length() > 0) {
//			try {
//				ByteArrayInputStream inputStream = new ByteArrayInputStream(vmXMLString.getBytes());
//				VMDefinitionsContainer.parseXMLIntoContainer(inputStream, vmDefs);
//				return false;
//			} catch (IOException ioe) {
//				LaunchingPlugin.log(ioe);
//			}			
//		} else {			
//			// Otherwise, look for the old file that previously held the VM definitions
//			IPath stateLocation= LaunchingPlugin.getDefault().getStateLocation();
//			IPath stateFile= stateLocation.append("vmConfiguration.xml"); //$NON-NLS-1$
//			File file = new File(stateFile.toOSString());
//			
//			if (file.exists()) {        
//				// If file exists, load VM definitions from it into memory and write the definitions to
//				// the preference store WITHOUT triggering any processing of the new value
//				FileInputStream fileInputStream = new FileInputStream(file);
//				VMDefinitionsContainer.parseXMLIntoContainer(fileInputStream, vmDefs);			
//			}		
//		}
//		return true;
//	}
//	
//	/**
//	 * Loads contributed VM installs
//	 *  
//	 */
//	private static void addVMExtensions(VMDefinitionsContainer vmDefs) {
//		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(LaunchingPlugin.ID_PLUGIN, JavaRuntime.EXTENSION_POINT_VM_INSTALLS);
//		IConfigurationElement[] configs= extensionPoint.getConfigurationElements();
//		for (int i = 0; i < configs.length; i++) {
//			IConfigurationElement element = configs[i];
//			try {
//				if ("vmInstall".equals(element.getName())) { //$NON-NLS-1$
//					String vmType = element.getAttribute("vmInstallType"); //$NON-NLS-1$
//					if (vmType == null) {
//						abort(MessageFormat.format("Missing required vmInstallType attribute for vmInstall contributed by {0}", //$NON-NLS-1$
//								new String[]{element.getContributor().getName()}), null);
//					}
//					String id = element.getAttribute("id"); //$NON-NLS-1$
//					if (id == null) {
//						abort(MessageFormat.format("Missing required id attribute for vmInstall contributed by {0}", //$NON-NLS-1$
//								new String[]{element.getContributor().getName()}), null);
//					}
//					IVMInstallType installType = getVMInstallType(vmType);
//					if (installType == null) {
//						abort(MessageFormat.format("vmInstall {0} contributed by {1} references undefined VM install type {2}", //$NON-NLS-1$
//								new String[]{id, element.getContributor().getName(), vmType}), null);
//					}
//					IVMInstall install = installType.findVMInstall(id);
//					if (install == null) {
//						// only load/create if first time we've seen this VM install
//						String name = element.getAttribute("name"); //$NON-NLS-1$
//						if (name == null) {
//							abort(MessageFormat.format("vmInstall {0} contributed by {1} missing required attribute name", //$NON-NLS-1$
//									new String[]{id, element.getContributor().getName()}), null);
//						}
//						String home = element.getAttribute("home"); //$NON-NLS-1$
//						if (home == null) {
//							abort(MessageFormat.format("vmInstall {0} contributed by {1} missing required attribute home", //$NON-NLS-1$
//									new String[]{id, element.getContributor().getName()}), null);
//						}		
//						String jsdoc = element.getAttribute("javadocURL"); //$NON-NLS-1$
//						String vmArgs = element.getAttribute("vmArgs"); //$NON-NLS-1$
//						VMStandin standin = new VMStandin(installType, id);
//						standin.setName(name);
//						home = substitute(home);
//						File homeDir = new File(home);
//                        if (homeDir.exists()) {
//                            try {
//                            	// adjust for relative path names
//                                home = homeDir.getCanonicalPath();
//                                homeDir = new File(home);
//                            } catch (IOException e) {
//                            }
//                        }
//                        IStatus status = installType.validateInstallLocation(homeDir);
//                        if (!status.isOK()) {
//                        	abort(MessageFormat.format("Illegal install location {0} for vmInstall {1} contributed by {2}: {3}", //$NON-NLS-1$
//                        			new String[]{home, id, element.getContributor().getName(), status.getMessage()}), null);
//                        }
//                        standin.setInstallLocation(homeDir);
//						if (jsdoc != null) {
//							try {
//								standin.setJavadocLocation(new URL(javadoc));
//							} catch (MalformedURLException e) {
//								abort(MessageFormat.format("Illegal javadocURL attribute for vmInstall {0} contributed by {1}", //$NON-NLS-1$
//										new String[]{id, element.getContributor().getName()}), e);
//							}
//						}
//						if (vmArgs != null) {
//							standin.setVMArgs(vmArgs);
//						}
//                        IConfigurationElement[] libraries = element.getChildren("library"); //$NON-NLS-1$
//                        LibraryLocation[] locations = null;
//                        if (libraries.length > 0) {
//                            locations = new LibraryLocation[libraries.length];
//                            for (int j = 0; j < libraries.length; j++) {
//                                IConfigurationElement library = libraries[j];
//                                String libPathStr = library.getAttribute("path"); //$NON-NLS-1$
//                                if (libPathStr == null) {
//                                    abort(MessageFormat.format("library for vmInstall {0} contributed by {1} missing required attribute libPath", //$NON-NLS-1$
//                                            new String[]{id, element.getContributor().getName()}), null);
//                                }
//                                String sourcePathStr = library.getAttribute("sourcePath"); //$NON-NLS-1$
//                                String packageRootStr = library.getAttribute("packageRootPath"); //$NON-NLS-1$
//                                String javadocOverride = library.getAttribute("javadocURL"); //$NON-NLS-1$
//                                URL url = null;
//                                if (javadocOverride != null) {
//                                    try {
//                                        url = new URL(javadocOverride);
//                                    } catch (MalformedURLException e) {
//                                        abort(MessageFormat.format("Illegal javadocURL attribute specified for library {0} for vmInstall {1} contributed by {2}" //$NON-NLS-1$
//                                                ,new String[]{libPathStr, id, element.getContributor().getName()}), e);
//                                    }
//                                }
//                                IPath homePath = new Path(home);
//                                IPath libPath = homePath.append(substitute(libPathStr));
//                                IPath sourcePath = Path.EMPTY;
//                                if (sourcePathStr != null) {
//                                    sourcePath = homePath.append(substitute(sourcePathStr));
//                                }
//                                IPath packageRootPath = Path.EMPTY;
//                                if (packageRootStr != null) {
//                                    packageRootPath = new Path(substitute(packageRootStr));
//                                }
//                                locations[j] = new LibraryLocation(libPath, sourcePath, packageRootPath, url);
//                            }
//                        }
//                        standin.setLibraryLocations(locations);
//                        vmDefs.addVM(standin);
//					}
//                    fgContributedVMs.add(id);
//				} else {
//					abort(MessageFormat.format("Illegal element {0} in vmInstalls extension contributed by {1}", //$NON-NLS-1$
//							new String[]{element.getName(), element.getContributor().getName()}), null);
//				}
//			} catch (CoreException e) {
//				LaunchingPlugin.log(e);
//			}
//		}
//	}
//    
//    /**
//     * Performs string substitution on the given expression.
//     * 
//     * @param expression
//     * @return expression after string substitution 
//     * @throws CoreException
//     *  
//     */
//    private static String substitute(String expression) throws CoreException {
//        return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(expression);
//    }
//    
//    /**
//     * Returns whether the VM install with the specified id was contributed via
//     * the vmInstalls extension point.
//     * 
//     * @param id vm id
//     * @return whether the vm install was contributed via extension point
//     *  
//     */
//    public static boolean isContributedVMInstall(String id) {
//        getVMInstallTypes(); // ensure VMs are initialized
//        return fgContributedVMs.contains(id);
//    }
	
	/**
	 * Evaluates library locations for a IVMInstall. If no library locations are set on the install, a default
	 * location is evaluated and checked if it exists.
	 * @return library locations with paths that exist or are empty
	 *  
	 */
	public static LibraryLocation[] getLibraryLocations(IVMInstall vm)  {
		IPath[] libraryPaths;
		IPath[] sourcePaths;
		IPath[] sourceRootPaths;
		URL[] javadocLocations;
		LibraryLocation[] locations= vm.getLibraryLocations();
		if (locations == null) {
            URL defJavaDocLocation = vm.getJavadocLocation(); 
			LibraryLocation[] dflts= vm.getVMInstallType().getDefaultLibraryLocations(vm.getInstallLocation());
			libraryPaths = new IPath[dflts.length];
			sourcePaths = new IPath[dflts.length];
			sourceRootPaths = new IPath[dflts.length];
			javadocLocations= new URL[dflts.length];
			for (int i = 0; i < dflts.length; i++) {
				libraryPaths[i]= dflts[i].getSystemLibraryPath();
                if (defJavaDocLocation == null) {
                    javadocLocations[i]= dflts[i].getJavadocLocation();
                } else {
                    javadocLocations[i]= defJavaDocLocation;
                }
				if (!libraryPaths[i].toFile().isFile()) {
					libraryPaths[i]= Path.EMPTY;
				}
				
				sourcePaths[i]= dflts[i].getSystemLibrarySourcePath();
				if (sourcePaths[i]!=null && sourcePaths[i].toFile().isFile()) {
					sourceRootPaths[i]= dflts[i].getPackageRootPath();
				} else {
					sourcePaths[i]= Path.EMPTY;
					sourceRootPaths[i]= Path.EMPTY;
				}
			}
		} else {
			libraryPaths = new IPath[locations.length];
			sourcePaths = new IPath[locations.length];
			sourceRootPaths = new IPath[locations.length];
			javadocLocations= new URL[locations.length];
			for (int i = 0; i < locations.length; i++) {			
				libraryPaths[i]= locations[i].getSystemLibraryPath();
				sourcePaths[i]= locations[i].getSystemLibrarySourcePath();
				sourceRootPaths[i]= locations[i].getPackageRootPath();
				javadocLocations[i]= locations[i].getJavadocLocation();
			}
		}
		locations = new LibraryLocation[sourcePaths.length];
		for (int i = 0; i < sourcePaths.length; i++) {
			locations[i] = new LibraryLocation(libraryPaths[i], sourcePaths[i], sourceRootPaths[i], javadocLocations[i]);
		}
		return locations;
	}
	
//	/**
//	 * Detect the VM that Eclipse is running on.
//	 * 
//	 * @return a VM standin representing the VM that Eclipse is running on, or
//	 * <code>null</code> if unable to detect the runtime VM
//	 */
//	private static VMStandin detectEclipseRuntime() {
//		VMStandin detectedVMStandin = null;
//		// Try to detect a VM for each declared VM type
//		IVMInstallType[] vmTypes= getVMInstallTypes();
//		for (int i = 0; i < vmTypes.length; i++) {
//			
//			File detectedLocation= vmTypes[i].detectInstallLocation();
//			if (detectedLocation != null && detectedVMStandin == null) {
//				
//				// Make sure the VM id is unique
//				long unique = System.currentTimeMillis();	
//				IVMInstallType vmType = vmTypes[i];
//				while (vmType.findVMInstall(String.valueOf(unique)) != null) {
//					unique++;
//				}
//
//				// Create a standin for the detected VM and add it to the result collector
//				String vmID = String.valueOf(unique);
//				detectedVMStandin = new VMStandin(vmType, vmID);
//				detectedVMStandin.setInstallLocation(detectedLocation);
//				detectedVMStandin.setName(generateDetectedVMName(detectedVMStandin));
//				if (vmType instanceof AbstractVMInstallType) {
//				    AbstractVMInstallType abs = (AbstractVMInstallType)vmType;
//				    URL url = abs.getDefaultJavadocLocation(detectedLocation);
//				    detectedVMStandin.setJavadocLocation(url);						
//				}
//			}				
//		}
//		return detectedVMStandin;
//	}
//	
//	private static boolean equals(String optionName, Map defaultOptions, Map options) {
//		return defaultOptions.get(optionName).equals(options.get(optionName));
//	}
//	
//	/**
//	 * Make the name of a detected VM stand out.
//	 */
//	private static String generateDetectedVMName(IVMInstall vm) {
//		return vm.getInstallLocation().getName();
//	}
//	
//	/**
//	 * Creates and returns a includepath entry describing
//	 * the JRE_LIB includepath variable.
//	 * 
//	 * @return a new IIncludePathEntry that describes the JRE_LIB includepath variable
//	 */
//	public static IIncludePathEntry getJREVariableEntry() {
//		return JavaScriptCore.newVariableEntry(
//			new Path(JRELIB_VARIABLE),
//			new Path(JRESRC_VARIABLE),
//			new Path(JRESRCROOT_VARIABLE)
//		);
//	}
//	
	/**
	 * Creates and returns a includepath entry describing
	 * the default JRE container entry.
	 * 
	 * @return a new IIncludePathEntry that describes the default JRE container entry
	 *  
	 */
	public static IIncludePathEntry getDefaultJREContainerEntry() {
		return JavaScriptCore.newContainerEntry(newDefaultJREContainerPath());
	}	
	
	/**
	 * Returns a path for the JRE includepath container identifying the 
	 * default VM install.
	 * 
	 * @return includepath container path
	 *  
	 */	
	public static IPath newDefaultJREContainerPath() {
		return new Path(JRE_CONTAINER);
	}
	
	/**
	 * Returns a path for the JRE includepath container identifying the 
	 * specified VM install by type and name.
	 * 
	 * @param vm vm install
	 * @return includepath container path
	 *  
	 */
	public static IPath newJREContainerPath(IVMInstall vm) {
		return newJREContainerPath(vm.getVMInstallType().getId(), vm.getName());
	}
	
	/**
	 * Returns a path for the JRE includepath container identifying the 
	 * specified VM install by type and name.
	 * 
	 * @param typeId vm install type identifier
	 * @param name vm install name
	 * @return includepath container path
	 *  
	 */	
	public static IPath newJREContainerPath(String typeId, String name) {
		IPath path = newDefaultJREContainerPath();
		path = path.append(typeId);
		path = path.append(name);
		return path;		
	}
	
//	/**
//	 * Returns a path for the JRE includepath container identifying the 
//	 * specified execution environment.
//	 * 
//	 * @param environment execution environment
//	 * @return includepath container path
//	 *  
//	 */
//	public static IPath newJREContainerPath(IExecutionEnvironment environment) {
//		IPath path = newDefaultJREContainerPath(); 
//		path = path.append(StandardVMType.ID_STANDARD_VM_TYPE);
//		path = path.append(JREContainerInitializer.encodeEnvironmentId(environment.getId()));
//		return path;
//	}	
	
	/**
	 * Returns the JRE referenced by the specified JRE includepath container
	 * path or <code>null</code> if none.
	 *  
	 * @param jreContainerPath
	 * @return JRE referenced by the specified JRE includepath container
	 *  path or <code>null</code>
	 *  
	 */
	public static IVMInstall getVMInstall(IPath jreContainerPath) {
		return JREContainerInitializer.resolveVM(jreContainerPath);
	}
//	
//	/**
//	 * Returns the identifier of the VM install type referenced by the
//	 * given JRE includepath container path, or <code>null</code> if none.
//	 * 
//	 * @param jreContainerPath
//	 * @return vm install type identifier or <code>null</code>
//	 *  
//	 */
//	public static String getVMInstallTypeId(IPath jreContainerPath) {
//		if (JREContainerInitializer.isExecutionEnvironment(jreContainerPath)) {
//			return null;
//		}
//		return JREContainerInitializer.getVMTypeId(jreContainerPath);
//	}
//
//	/**
//	 * Returns the name of the VM install referenced by the
//	 * given JRE includepath container path, or <code>null</code> if none.
//	 * 
//	 * @param jreContainerPath
//	 * @return vm name or <code>null</code>
//	 *  
//	 */
//	public static String getVMInstallName(IPath jreContainerPath) {
//		if (JREContainerInitializer.isExecutionEnvironment(jreContainerPath)) {
//			return null;
//		}
//		return JREContainerInitializer.getVMName(jreContainerPath);
//	}
	
	/**
	 * Returns the execution environment identifier in the following JRE
	 * includepath container path, or <code>null</code> if none.
	 *  
	 * @param jreContainerPath includepath container path
	 * @return execution environment identifier or <code>null</code>
	 *  
	 */
	public static String getExecutionEnvironmentId(IPath jreContainerPath) {
		return JREContainerInitializer.getExecutionEnvironmentId(jreContainerPath);
	}
	
	/**
	 * Returns a runtime includepath entry identifying the JRE to use when launching the specified
	 * configuration or <code>null</code> if none is specified. The entry returned represents a
	 * either a includepath variable or includepath container that resolves to a JRE.
	 * <p>
	 * The entry is resolved as follows:
	 * <ol>
	 * <li>If the <code>ATTR_JRE_CONTAINER_PATH</code> is present, it is used to create
	 *  a includepath container referring to a JRE.</li>
	 * <li>Next, if the <code>ATTR_VM_INSTALL_TYPE</code> and <code>ATTR_VM_INSTALL_NAME</code>
	 * attributes are present, they are used to create a includepath container.</li>
	 * <li>When none of the above attributes are specified, a default entry is
	 * created which refers to the JRE referenced by the build path of the configuration's
	 * associated JavaScript project. This could be a includepath variable or includepath container.</li>
	 * <li>When there is no JavaScript project associated with a configuration, the workspace
	 * default JRE is used to create a container path.</li>
	 * </ol>
	 * </p>
	 * @param configuration
	 * @return includepath container path identifying a JRE or <code>null</code>
	 * @exception org.eclipse.core.runtime.CoreException if an exception occurs retrieving
	 *  attributes from the specified launch configuration
	 *  
	 */
	public static IRuntimeClasspathEntry computeJREEntry(ILaunchConfiguration configuration) throws CoreException {
		String jreAttr = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, (String)null);
		IPath containerPath = null;
		if (jreAttr == null) {
			String type = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, (String)null);
			if (type == null) {
				// default JRE for the launch configuration
				IJavaScriptProject proj = getJavaProject(configuration);
				if (proj == null) {
					containerPath = newDefaultJREContainerPath();
				} else {
					return computeJREEntry(proj);
				}
			} else {
				String name = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_NAME, (String)null);
				if (name != null) {
					containerPath = newDefaultJREContainerPath().append(type).append(name);
				}
			}
		} else {
			containerPath = Path.fromPortableString(jreAttr);
		}
		if (containerPath != null) {
			return newRuntimeContainerClasspathEntry(containerPath, IRuntimeClasspathEntry.STANDARD_CLASSES);
		}
		return null;
	}
	
	/**
	 * Returns a runtime includepath entry identifying the JRE referenced by the specified
	 * project, or <code>null</code> if none. The entry returned represents a either a
	 * includepath variable or includepath container that resolves to a JRE.
	 * 
	 * @param project JavaScript project
	 * @return JRE runtime includepath entry or <code>null</code>
	 * @exception org.eclipse.core.runtime.CoreException if an exception occurs
	 * 	accessing the project's includepath
	 *  
	 */
	public static IRuntimeClasspathEntry computeJREEntry(IJavaScriptProject project) throws CoreException {
		IIncludePathEntry[] rawClasspath = project.getRawIncludepath();
		IRuntimeClasspathEntryResolver2 resolver = null;
		for (int i = 0; i < rawClasspath.length; i++) {
			IIncludePathEntry entry = rawClasspath[i];
			switch (entry.getEntryKind()) {
				case IIncludePathEntry.CPE_VARIABLE:
					resolver = getVariableResolver(entry.getPath().segment(0));
					if (resolver != null) {
						if (resolver.isVMInstallReference(entry)) {
							return newRuntimeClasspathEntry(entry);
						}
					}					
					break;
				case IIncludePathEntry.CPE_CONTAINER:
					resolver = getContainerResolver(entry.getPath().segment(0));
					if (resolver != null) {
						if (resolver.isVMInstallReference(entry)) {
							IJsGlobalScopeContainer container = JavaScriptCore.getJsGlobalScopeContainer(entry.getPath(), project);
							if (container != null) {
								switch (container.getKind()) {
									case IJsGlobalScopeContainer.K_APPLICATION:
										break;
									case IJsGlobalScopeContainer.K_DEFAULT_SYSTEM:
										return newRuntimeContainerClasspathEntry(entry.getPath(), IRuntimeClasspathEntry.STANDARD_CLASSES);
									case IJsGlobalScopeContainer.K_SYSTEM:
										return newRuntimeContainerClasspathEntry(entry.getPath(), IRuntimeClasspathEntry.BOOTSTRAP_CLASSES);
								}
							}
						}
					}
					break;
			}
			
		}
		return null;
	}	
	
//	/**
//	 * Returns whether the given runtime includepath entry refers to a vm install.
//	 * 
//	 * @param entry
//	 * @return whether the given runtime includepath entry refers to a vm install
//	 *  
//	 */
//	public static boolean isVMInstallReference(IRuntimeClasspathEntry entry) {
//		IIncludePathEntry includepathEntry = entry.getClasspathEntry();
//		if (includepathEntry != null) {
//			switch (includepathEntry.getEntryKind()) {
//				case IIncludePathEntry.CPE_VARIABLE:
//					IRuntimeClasspathEntryResolver2 resolver = getVariableResolver(includepathEntry.getPath().segment(0));
//					if (resolver != null) {
//						return resolver.isVMInstallReference(includepathEntry);
//					}
//					break;					
//				case IIncludePathEntry.CPE_CONTAINER:
//					resolver = getContainerResolver(includepathEntry.getPath().segment(0));
//					if (resolver != null) {
//						return resolver.isVMInstallReference(includepathEntry);
//					}
//					break;
//				}
//		}
//		return false;
//	}
//	
//	/**
//	 * Returns the VM connector defined with the specified identifier,
//	 * or <code>null</code> if none.
//	 * 
//	 * @param id VM connector identifier
//	 * @return VM connector or <code>null</code> if none
//	 *  
//	 */
//	public static IVMConnector getVMConnector(String id) {
//		return LaunchingPlugin.getDefault().getVMConnector(id);
//	}
//	
//	/**
//	 * Returns all VM connector extensions.
//	 *
//	 * @return VM connectors
//	 *  
//	 */
//	public static IVMConnector[] getVMConnectors() {
//		return LaunchingPlugin.getDefault().getVMConnectors();
//	}	
	
	/**
	 * Returns the preference store for the launching plug-in.
	 * 
	 * @return the preference store for the launching plug-in
	 *  
	 */
	public static Preferences getPreferences() {
		return JavaScriptCore.getPlugin().getPluginPreferences();
	}
	
	/**
	 * Saves the preferences for the launching plug-in.
	 * 
	 *  
	 */
	public static void savePreferences() {
		 JavaScriptCore.getPlugin().savePluginPreferences();
	}
	
//	/**
//	 * Registers the given resolver for the specified variable.
//	 * 
//	 * @param resolver runtime includepath entry resolver
//	 * @param variableName variable name to register for
//	 *  
//	 */
//	public static void addVariableResolver(IRuntimeClasspathEntryResolver resolver, String variableName) {
//		Map map = getVariableResolvers();
//		map.put(variableName, resolver);
//	}
	
	/**
	 * Registers the given resolver for the specified container.
	 * 
	 * @param resolver runtime includepath entry resolver
	 * @param containerIdentifier identifier of the includepath container to register for
	 *  
	 */
	public static void addContainerResolver(IRuntimeClasspathEntryResolver resolver, String containerIdentifier) {
		Map map = getContainerResolvers();
		map.put(containerIdentifier, resolver);
	}	
	
	/**
	 * Returns all registered variable resolvers.
	 */
	private static Map getVariableResolvers() {
		if (fgVariableResolvers == null) {
			initializeResolvers();
		}
		return fgVariableResolvers;
	}
	
	/**
	 * Returns all registered container resolvers.
	 */
	private static Map getContainerResolvers() {
		if (fgContainerResolvers == null) {
			initializeResolvers();
		}
		return fgContainerResolvers;
	}
	
//	/**
//	 * Returns all registered runtime includepath entry resolvers.
//	 */
//	private static Map getEntryResolvers() {
//		if (fgRuntimeClasspathEntryResolvers == null) {
//			initializeResolvers();
//		}
//		return fgRuntimeClasspathEntryResolvers;
//	}	
//
	private static void initializeResolvers() {
		fgContainerResolvers=new HashMap();
//TODO: implement
//throw new org.eclipse.wst.jsdt.core.UnimplementedException();
		//		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(LaunchingPlugin.ID_PLUGIN, EXTENSION_POINT_RUNTIME_CLASSPATH_ENTRY_RESOLVERS);
//		IConfigurationElement[] extensions = point.getConfigurationElements();
//		fgVariableResolvers = new HashMap(extensions.length);
//		fgContainerResolvers = new HashMap(extensions.length);
//		fgRuntimeClasspathEntryResolvers = new HashMap(extensions.length);
//		for (int i = 0; i < extensions.length; i++) {
//			RuntimeClasspathEntryResolver res = new RuntimeClasspathEntryResolver(extensions[i]);
//			String variable = res.getVariableName();
//			String container = res.getContainerId();
//			String entryId = res.getRuntimeClasspathEntryId();
//			if (variable != null) {
//				fgVariableResolvers.put(variable, res);
//			}
//			if (container != null) {
//				fgContainerResolvers.put(container, res);
//			}
//			if (entryId != null) {
//				fgRuntimeClasspathEntryResolvers.put(entryId, res);
//			}
//		}		
	}

//	/**
//	 * Returns all registered includepath providers.
//	 */
//	private static Map getClasspathProviders() {
//		if (fgPathProviders == null) {
//			initializeProviders();
//		}
//		return fgPathProviders;
//	}
//		
//	private static void initializeProviders() {
//		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(LaunchingPlugin.ID_PLUGIN, EXTENSION_POINT_RUNTIME_CLASSPATH_PROVIDERS);
//		IConfigurationElement[] extensions = point.getConfigurationElements();
//		fgPathProviders = new HashMap(extensions.length);
//		for (int i = 0; i < extensions.length; i++) {
//			RuntimeClasspathProvider res = new RuntimeClasspathProvider(extensions[i]);
//			fgPathProviders.put(res.getIdentifier(), res);
//		}		
//	}
		
	/**
	 * Returns the resolver registered for the given variable, or
	 * <code>null</code> if none.
	 * 
	 * @param variableName the variable to determine the resolver for
	 * @return the resolver registered for the given variable, or
	 * <code>null</code> if none
	 */
	private static IRuntimeClasspathEntryResolver2 getVariableResolver(String variableName) {
		return (IRuntimeClasspathEntryResolver2)getVariableResolvers().get(variableName);
	}
	
	/**
	 * Returns the resolver registered for the given container id, or
	 * <code>null</code> if none.
	 * 
	 * @param containerId the container to determine the resolver for
	 * @return the resolver registered for the given container id, or
	 * <code>null</code> if none
	 */	
	private static IRuntimeClasspathEntryResolver2 getContainerResolver(String containerId) {
		return (IRuntimeClasspathEntryResolver2)getContainerResolvers().get(containerId);
	}
	
//	/**
//	 * Returns the resolver registered for the given contributed includepath
//	 * entry type.
//	 * 
//	 * @param typeId the id of the contributed includepath entry
//	 * @return the resolver registered for the given includepath entry
//	 */	
//	private static IRuntimeClasspathEntryResolver getContributedResolver(String typeId) {
//		IRuntimeClasspathEntryResolver resolver = (IRuntimeClasspathEntryResolver)getEntryResolvers().get(typeId);
//		if (resolver == null) {
//			return new DefaultEntryResolver();
//		}
//		return resolver;
//	}	
//	
//	/**
//	 * Adds the given listener to the list of registered VM install changed
//	 * listeners. Has no effect if an identical listener is already registered.
//	 * 
//	 * @param listener the listener to add
//	 *  
//	 */
//	public static void addVMInstallChangedListener(IVMInstallChangedListener listener) {
//		fgVMListeners.add(listener);
//	}
//	
//	/**
//	 * Removes the given listener from the list of registered VM install changed
//	 * listeners. Has no effect if an identical listener is not already registered.
//	 * 
//	 * @param listener the listener to remove
//	 *  
//	 */
//	public static void removeVMInstallChangedListener(IVMInstallChangedListener listener) {
//		fgVMListeners.remove(listener);
//	}	
	
	private static void notifyDefaultVMChanged(IVMInstall previous, IVMInstall current) {
		Object[] listeners = fgVMListeners.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			IVMInstallChangedListener listener = (IVMInstallChangedListener)listeners[i];
			listener.defaultVMInstallChanged(previous, current);
		}
	}
	
	/**
	 * Notifies all VM install changed listeners of the given property change.
	 * 
	 * @param event event describing the change.
	 *  
	 */
	public static void fireVMChanged(PropertyChangeEvent event) {
		Object[] listeners = fgVMListeners.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			IVMInstallChangedListener listener = (IVMInstallChangedListener)listeners[i];
			listener.vmChanged(event);
		}		
	}
	
	/**
	 * Notifies all VM install changed listeners of the VM addition
	 * 
	 * @param vm the VM that has been added
	 *  
	 */
	public static void fireVMAdded(IVMInstall vm) {
		if (!fgInitializingVMs) {
			Object[] listeners = fgVMListeners.getListeners();
			for (int i = 0; i < listeners.length; i++) {
				IVMInstallChangedListener listener = (IVMInstallChangedListener)listeners[i];
				listener.vmAdded(vm);
			}
		}
	}	
	
	/**
	 * Notifies all VM install changed listeners of the VM removal
	 * 
	 * @param vm the VM that has been removed
	 *  
	 */
	public static void fireVMRemoved(IVMInstall vm) {
		Object[] listeners = fgVMListeners.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			IVMInstallChangedListener listener = (IVMInstallChangedListener)listeners[i];
			listener.vmRemoved(vm);
		}		
	}		
	
//	/**
//	 * Return the String representation of the default output directory of the
//	 * launch config's project or <code>null</code> if there is no config, no
//	 * project or some sort of problem.
//	 * 
//	 * @return the default output directory for the specified launch
//	 * configuration's project
//	 *  
//	 */
//	public static String getProjectOutputDirectory(ILaunchConfiguration config) {
//		try {
//			if (config != null) {
//				IJavaScriptProject javaProject = JavaRuntime.getJavaProject(config);
//				if (javaProject != null) {
//					IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
//					IPath outputLocation = javaProject.getOutputLocation();
//					IResource resource = root.findMember(outputLocation);
//					if (resource != null) {
//						IPath path = resource.getFullPath();
//						if (path != null)  {
//							return path.makeRelative().toString();
//						}
//					}
//				} 
//			}
//		} catch (CoreException ce) {
//		} 
//		return null;
//	}
//	
//	/**
//	 * Returns a collection of source containers corresponding to the given
//	 * resolved runtime includepath entries.
//	 * <p>
//	 * Note that the entries must be resolved to ARCHIVE and PROJECT entries,
//	 * as source containers cannot be determined for unresolved entries.
//	 * </p>
//	 * @param entries entries to translate
//	 * @return source containers corresponding to the given runtime includepath entries
//	 *  
//	 */
//	public static ISourceContainer[] getSourceContainers(IRuntimeClasspathEntry[] entries) {
//		return JavaSourceLookupUtil.translate(entries);
//	}
//	
//	/**
//	 * Returns a collection of paths that should be appended to the given project's
//	 * <code>java.library.path</code> system property when launched. Entries are
//	 * searched for on the project's build path as extra includepath attributes.
//	 * Each entry represents an absolute path in the local file system.
//	 *
//	 * @param project the project to compute the <code>java.library.path</code> for
//	 * @param requiredProjects whether to consider entries in required projects
//	 * @return a collection of paths representing entries that should be appended
//	 *  to the given project's <code>java.library.path</code>
//	 * @throws CoreException if unable to compute the JavaScript library path
//	 *  
//	 * @see org.eclipse.wst.jsdt.core.IIncludePathAttribute
//	 * @see JavaRuntime#CLASSPATH_ATTR_LIBRARY_PATH_ENTRY
//	 */
//	public static String[] computeJavaLibraryPath(IJavaScriptProject project, boolean requiredProjects) throws CoreException {
//		Set visited = new HashSet();
//		List entries = new ArrayList();
//		gatherJavaLibraryPathEntries(project, requiredProjects, visited, entries);
//		List resolved = new ArrayList(entries.size());
//		Iterator iterator = entries.iterator();
//		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
//		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
//		while (iterator.hasNext()) {
//			String entry = (String) iterator.next();
//			String resolvedEntry = manager.performStringSubstitution(entry);
//			IPath path = new Path(resolvedEntry);
//			if (path.isAbsolute()) {
//				File file = path.toFile();
//				resolved.add(file.getAbsolutePath());
//			} else {
//				IResource resource = root.findMember(path);
//				if (resource != null) {
//					IPath location = resource.getLocation();
//					if (location != null) {
//						resolved.add(location.toFile().getAbsolutePath());
//					}
//				}
//			}
//		}
//		return (String[])resolved.toArray(new String[resolved.size()]);
//	}
//
//	/**
//	 * Gathers all JavaScript library entries for the given project and optionally its required
//	 * projects.
//	 * 
//	 * @param project project to gather entries for
//	 * @param requiredProjects whether to consider required projects 
//	 * @param visited projects already considered
//	 * @param entries collection to add library entries to
//	 * @throws CoreException if unable to gather includepath entries
//	 *  
//	 */
//	private static void gatherJavaLibraryPathEntries(IJavaScriptProject project, boolean requiredProjects, Set visited, List entries) throws CoreException {
//		if (visited.contains(project)) {
//			return;
//		}
//		visited.add(project);
//		IIncludePathEntry[] rawClasspath = project.getRawClasspath();
//		IIncludePathEntry[] required = processJavaLibraryPathEntries(project, requiredProjects, rawClasspath, entries);
//		if (required != null) {
//			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
//			for (int i = 0; i < required.length; i++) {
//				IIncludePathEntry entry = required[i];
//				String projectName = entry.getPath().segment(0);
//				IProject p = root.getProject(projectName);
//				if (p.exists()) {
//					IJavaScriptProject requiredProject = JavaScriptCore.create(p);
//					if (requiredProject != null) {
//						gatherJavaLibraryPathEntries(requiredProject, requiredProjects, visited, entries);
//					}
//				}
//			}
//		}
//	}
//	
//	/**
//	 * Adds all javascript library path extra includepath entry values to the given entries collection
//	 * specified on the given project's includepath, and returns a collection of required
//	 * projects, or <code>null</code>.
//	 *  
//	 * @param project project being processed
//	 * @param collectRequired whether to collect required projects
//	 * @param includepathEntries the project's raw includepath
//	 * @param entries collection to add javascript library path entries to
//	 * @return required project includepath entries or <code>null</code>
//	 * @throws CoreException
//	 *  
//	 */
//	private static IIncludePathEntry[] processJavaLibraryPathEntries(IJavaScriptProject project, boolean collectRequired, IIncludePathEntry[] includepathEntries, List entries) throws CoreException {
//		List req = null;
//		for (int i = 0; i < includepathEntries.length; i++) {
//			IIncludePathEntry entry = includepathEntries[i];
//			IIncludePathAttribute[] extraAttributes = entry.getExtraAttributes();
//			for (int j = 0; j < extraAttributes.length; j++) {
//				String[] paths = getLibraryPaths(extraAttributes[j]);
//				if (paths != null) {
//					for (int k = 0; k < paths.length; k++) {
//						entries.add(paths[k]);
//					}
//				}
//			}
//			if (entry.getEntryKind() == IIncludePathEntry.CPE_CONTAINER) {
//				IJsGlobalScopeContainer container = JavaScriptCore.getJsGlobalScopeContainer(entry.getPath(), project);
//				if (container != null) {
//					IIncludePathEntry[] requiredProjects = processJavaLibraryPathEntries(project, collectRequired, container.getClasspathEntries(), entries);
//					if (requiredProjects != null) {
//						if (req == null) {
//							req = new ArrayList();
//						}
//						for (int j = 0; j < requiredProjects.length; j++) {
//							req.add(requiredProjects[j]);
//						}
//					}
//				}
//			} else if (collectRequired && entry.getEntryKind() == IIncludePathEntry.CPE_PROJECT) {
//				if (req == null) {
//					req = new ArrayList();
//				}
//				req.add(entry);
//			}
//		}
//		if (req != null) {
//			return (IIncludePathEntry[]) req.toArray(new IIncludePathEntry[req.size()]);
//		}
//		return null;
//	}
//	
//	/**
//	 * Creates a new includepath attribute referencing a list of shared libraries that should
//	 * appear on the <code>-Djava.library.path</code> system property at runtime
//	 * for an associated {@link IIncludePathEntry}.
//	 * <p>
//	 * The factory methods <code>newLibraryPathsAttribute(String[])</code>
//	 * and <code>getLibraryPaths(IIncludePathAttribute)</code> should be used to
//	 * encode and decode the attribute value.
//	 * </p>
//	 * @param paths an array of strings representing paths of shared libraries.
//	 * Each string is used to create an <code>IPath</code> using the constructor
//	 * <code>Path(String)</code>, and may contain <code>IStringVariable</code>'s.
//	 * Variable substitution is performed on each string before a path is constructed
//	 * from a string.
//	 * @return a includepath attribute with the name <code>CLASSPATH_ATTR_LIBRARY_PATH_ENTRY</code>
//	 * and an value encoded to the specified paths.
//	 *  
//	 */
//	public static IIncludePathAttribute newLibraryPathsAttribute(String[] paths) {
//		StringBuffer value = new StringBuffer();
//		for (int i = 0; i < paths.length; i++) {
//			value.append(paths[i]);
//			if (i < (paths.length - 1)) {
//				value.append("|"); //$NON-NLS-1$
//			}
//		}
//		return JavaScriptCore.newClasspathAttribute(CLASSPATH_ATTR_LIBRARY_PATH_ENTRY, value.toString());
//	}
//	
//	/**
//	 * Returns an array of strings referencing shared libraries that should
//	 * appear on the <code>-Djava.library.path</code> system property at runtime
//	 * for an associated {@link IIncludePathEntry}, or <code>null</code> if the
//	 * given attribute is not a <code>CLASSPATH_ATTR_LIBRARY_PATH_ENTRY</code>.
//	 * Each string is used to create an <code>IPath</code> using the constructor
//	 * <code>Path(String)</code>, and may contain <code>IStringVariable</code>'s. 
//	 * <p>
//	 * The factory methods <code>newLibraryPathsAttribute(String[])</code>
//	 * and <code>getLibraryPaths(IIncludePathAttribute)</code> should be used to
//	 * encode and decode the attribute value. 
//	 * </p>
//	 * @param attribute a <code>CLASSPATH_ATTR_LIBRARY_PATH_ENTRY</code> includepath attribute
//	 * @return an array of strings referencing shared libraries that should
//	 * appear on the <code>-Djava.library.path</code> system property at runtime
//	 * for an associated {@link IIncludePathEntry}, or <code>null</code> if the
//	 * given attribute is not a <code>CLASSPATH_ATTR_LIBRARY_PATH_ENTRY</code>.
//	 * Each string is used to create an <code>IPath</code> using the constructor
//	 * <code>Path(String)</code>, and may contain <code>IStringVariable</code>'s.
//	 *  
//	 */	
//	public static String[] getLibraryPaths(IIncludePathAttribute attribute) {
//		if (CLASSPATH_ATTR_LIBRARY_PATH_ENTRY.equals(attribute.getName())) {
//			String value = attribute.getValue();
//			return value.split("\\|"); //$NON-NLS-1$
//		}
//		return null;
//	}
//	
//	/**
//	 * Returns the execution environments manager.
//	 * 
//	 * @return execution environments manager
//	 *  
//	 */
//	public static IExecutionEnvironmentsManager getExecutionEnvironmentsManager() {
//		return EnvironmentsManager.getDefault();
//	}
	
	/**
	 * Perform VM type and VM install initialization. Does not hold locks
	 * while performing change notification.
	 * 
	 *  
	 */
	private static void initializeVMs() {
		
		
//		VMDefinitionsContainer vmDefs = null;
//		boolean setPref = false;
//		boolean updateCompliance = false;
		synchronized (fgVMLock) {
			if (fgVMTypes == null) {
				try {
					fgInitializingVMs = true;
					fgVMTypes=new IVMInstallType[]{new StandardVMType()};
					defaultVM = new StandardVM(fgVMTypes[0],"defaultVM"); //$NON-NLS-1$
					fgDefaultVMId=defaultVM.getId();
					File location = SystemLibraryLocation.getInstance().getWorkingLibPath().toFile();
					defaultVM.setInstallLocation(location);
					
//					// 1. load VM type extensions
//					initializeVMTypeExtensions();
//					try {
//						vmDefs = new VMDefinitionsContainer();
//						// 2. add persisted VMs
//						setPref = addPersistedVMs(vmDefs);
//						
//						// 3. if there are none, detect the eclipse runtime
//						if (vmDefs.getValidVMList().isEmpty()) {
//							// calling out to detectEclipseRuntime() could allow clients to change
//							// VM settings (i.e. call back into change VM settings).
//							VMListener listener = new VMListener();
//							addVMInstallChangedListener(listener);
//							setPref = true;
//							VMStandin runtime = detectEclipseRuntime();
//							removeVMInstallChangedListener(listener);
//							if (!listener.isChanged()) {
//								if (runtime != null) {
//									updateCompliance = true;
//									vmDefs.addVM(runtime);
//									vmDefs.setDefaultVMInstallCompositeID(getCompositeIdFromVM(runtime));
//								}
//							} else {
//								// VMs were changed - reflect current settings
//								addPersistedVMs(vmDefs);
//								vmDefs.setDefaultVMInstallCompositeID(fgDefaultVMId);
//								updateCompliance = fgDefaultVMId != null;
//							}
//						}
//						// 4. load contributed VM installs
//						addVMExtensions(vmDefs);
//						// 5. verify default VM is valid
//						String defId = vmDefs.getDefaultVMInstallCompositeID();
//						boolean validDef = false;
//						if (defId != null) {
//							Iterator iterator = vmDefs.getValidVMList().iterator();
//							while (iterator.hasNext()) {
//								IVMInstall vm = (IVMInstall) iterator.next();
//								if (getCompositeIdFromVM(vm).equals(defId)) {
//									validDef = true;
//									break;
//								}
//							}
//						}
//						if (!validDef) {
//							// use the first as the default
//							setPref = true;
//							List list = vmDefs.getValidVMList();
//							if (!list.isEmpty()) {
//								IVMInstall vm = (IVMInstall) list.get(0);
//								vmDefs.setDefaultVMInstallCompositeID(getCompositeIdFromVM(vm));
//							}
//						}
//						fgDefaultVMId = vmDefs.getDefaultVMInstallCompositeID();
//						fgDefaultVMConnectorId = vmDefs.getDefaultVMInstallConnectorTypeID();
//						
//						// Create the underlying VMs for each valid VM
//						List vmList = vmDefs.getValidVMList();
//						Iterator vmListIterator = vmList.iterator();
//						while (vmListIterator.hasNext()) {
//							VMStandin vmStandin = (VMStandin) vmListIterator.next();
//							vmStandin.convertToRealVM();
//						}						
//						
//
//					} catch (IOException e) {
//						JavaPlugin.log(e);
//					}
				} finally {
					fgInitializingVMs = false;
				}
			}
		}
//		if (vmDefs != null) {
//			// notify of initial VMs for backwards compatibility
//			IVMInstallType[] installTypes = getVMInstallTypes();
//			for (int i = 0; i < installTypes.length; i++) {
//				IVMInstallType type = installTypes[i];
//				IVMInstall[] installs = type.getVMInstalls();
//				for (int j = 0; j < installs.length; j++) {
//					fireVMAdded(installs[j]);
//				}
//			}
//			
//			// save settings if required
//			if (setPref) {
//				try {
//					String xml = vmDefs.getAsXML();
//					LaunchingPlugin.getDefault().getPluginPreferences().setValue(PREF_VM_XML, xml);
//				} catch (ParserConfigurationException e) {
//					LaunchingPlugin.log(e);
//				} catch (IOException e) {
//					LaunchingPlugin.log(e);
//				} catch (TransformerException e) {
//					LaunchingPlugin.log(e);
//				}
//				
//			}
//			
//			// update compliance if required
//			if (updateCompliance) {
//				updateCompliance(getDefaultVMInstall());
//			}
//		}
	}
	
//	/**
//	 * Update compiler compliance settings based on the given vm.
//	 * 
//	 * @param vm
//	 */
//	private static void updateCompliance(IVMInstall vm) {
//        if (vm instanceof IVMInstall2) {
//            String javaVersion = ((IVMInstall2)vm).getJavaVersion();
//            if (javaVersion != null && javaVersion.startsWith(JavaScriptCore.VERSION_1_5)) {
//                Hashtable defaultOptions = JavaScriptCore.getDefaultOptions();
//                Hashtable options = JavaScriptCore.getOptions();
//                boolean isDefault =
//                	equals(JavaScriptCore.COMPILER_COMPLIANCE, defaultOptions, options) &&
//                	equals(JavaScriptCore.COMPILER_SOURCE, defaultOptions, options) &&
//                	equals(JavaScriptCore.COMPILER_CODEGEN_TARGET_PLATFORM, defaultOptions, options) &&
//                	equals(JavaScriptCore.COMPILER_PB_ASSERT_IDENTIFIER, defaultOptions, options) &&
//                	equals(JavaScriptCore.COMPILER_PB_ENUM_IDENTIFIER, defaultOptions, options);
//                // only update the compliance settings if they are default settings, otherwise the
//                // settings have already been modified by a tool or user
//                if (isDefault) {
//                    options.put(JavaScriptCore.COMPILER_COMPLIANCE, JavaScriptCore.VERSION_1_5);
//                    options.put(JavaScriptCore.COMPILER_SOURCE, JavaScriptCore.VERSION_1_5);
//                    options.put(JavaScriptCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaScriptCore.VERSION_1_5);
//                    options.put(JavaScriptCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaScriptCore.ERROR);
//                    options.put(JavaScriptCore.COMPILER_PB_ENUM_IDENTIFIER, JavaScriptCore.ERROR);
//                    JavaScriptCore.setOptions(options);
//                }
//            }
//        }		
//	}
//
}
