/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mickael Istria (Red Hat Inc.) - 426209 Java 6 + Warnings cleanup
 *******************************************************************************/
package org.eclipse.wst.jsdt.launching;


import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
/**
 * Abstract implementation of a VM install.
 * <p>
 * Clients implementing VM installs must subclass this class.
 * </p>
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public abstract class AbstractVMInstall implements IVMInstall, IVMInstall2, IVMInstall3 {

	private IVMInstallType fType;
	private String fId;
	private String fName;
	private File fInstallLocation;
	private LibraryLocation[] fSystemLibraryDescriptions;
	private URL fJavadocLocation;
	private String fVMArgs;
	// system properties are cached in user preferences prefixed with this key, followed
	// by vm type, vm id, and system property name
//	private static final String PREF_VM_INSTALL_SYSTEM_PROPERTY = "PREF_VM_INSTALL_SYSTEM_PROPERTY"; //$NON-NLS-1$
	// whether change events should be fired
	private boolean fNotify = true;
	
	/**
	 * Constructs a new VM install.
	 * 
	 * @param	type	The type of this VM install.
	 * 					Must not be <code>null</code>
	 * @param	id		The unique identifier of this VM instance
	 * 					Must not be <code>null</code>.
	 * @throws	IllegalArgumentException	if any of the required
	 * 					parameters are <code>null</code>.
	 */
	public AbstractVMInstall(IVMInstallType type, String id) {
		if (type == null)
			throw new IllegalArgumentException(LaunchingMessages.vmInstall_assert_typeNotNull); 
		if (id == null)
			throw new IllegalArgumentException(LaunchingMessages.vmInstall_assert_idNotNull); 
		fType= type;
		fId= id;
	}

	/* (non-Javadoc)
	 * Subclasses should not override this method.
	 * @see IVMInstall#getId()
	 */
	public String getId() {
		return fId;
	}

	/* (non-Javadoc)
	 * Subclasses should not override this method.
	 * @see IVMInstall#getName()
	 */
	public String getName() {
		return fName;
	}

	/* (non-Javadoc)
	 * Subclasses should not override this method.
	 * @see IVMInstall#setName(String)
	 */
	public void setName(String name) {
		if (!name.equals(fName)) {
			PropertyChangeEvent event = new PropertyChangeEvent(this, IVMInstallChangedListener.PROPERTY_NAME, fName, name);
			fName= name;
			if (fNotify) {
				JavaRuntime.fireVMChanged(event);
			}
		}
	}

	/* (non-Javadoc)
	 * Subclasses should not override this method.
	 * @see IVMInstall#getInstallLocation()
	 */
	public File getInstallLocation() {
		return fInstallLocation;
	}

	/* (non-Javadoc)
	 * Subclasses should not override this method.
	 * @see IVMInstall#setInstallLocation(File)
	 */
	public void setInstallLocation(File installLocation) {
		if (!installLocation.equals(fInstallLocation)) {
			PropertyChangeEvent event = new PropertyChangeEvent(this, IVMInstallChangedListener.PROPERTY_INSTALL_LOCATION, fInstallLocation, installLocation);
			fInstallLocation= installLocation;
			if (fNotify) {
				JavaRuntime.fireVMChanged(event);
			}
		}
	}

	/* (non-Javadoc)
	 * Subclasses should not override this method.
	 * @see IVMInstall#getVMInstallType()
	 */
	public IVMInstallType getVMInstallType() {
		return fType;
	}

	/* (non-Javadoc)
	 * @see IVMInstall#getVMRunner(String)
	 */
	public IVMRunner getVMRunner(String mode) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.launching.IVMInstall#getLibraryLocations()
	 */
	public LibraryLocation[] getLibraryLocations() {
		return fSystemLibraryDescriptions;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.launching.IVMInstall#setLibraryLocations(org.eclipse.wst.jsdt.launching.LibraryLocation[])
	 */
	public void setLibraryLocations(LibraryLocation[] locations) {
		if (locations == fSystemLibraryDescriptions) {
			return;
		}
		LibraryLocation[] newLocations = locations;
		if (newLocations == null) {
			newLocations = getVMInstallType().getDefaultLibraryLocations(getInstallLocation()); 
		}
		LibraryLocation[] prevLocations = fSystemLibraryDescriptions;
		if (prevLocations == null) {
			prevLocations = getVMInstallType().getDefaultLibraryLocations(getInstallLocation()); 
		}
		
		if (newLocations.length == prevLocations.length) {
			int i = 0;
			boolean equal = true;
			while (i < newLocations.length && equal) {
				equal = newLocations[i].equals(prevLocations[i]);
				i++;
			}
			if (equal) {
				// no change
				return;
			}
		}

		PropertyChangeEvent event = new PropertyChangeEvent(this, IVMInstallChangedListener.PROPERTY_LIBRARY_LOCATIONS, prevLocations, newLocations);
		fSystemLibraryDescriptions = locations;
		if (fNotify) {
			JavaRuntime.fireVMChanged(event);		
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.launching.IVMInstall#getJavadocLocation()
	 */
	public URL getJavadocLocation() {
		return fJavadocLocation;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.launching.IVMInstall#setJavadocLocation(java.net.URL)
	 */
	public void setJavadocLocation(URL url) {
		if (url == fJavadocLocation) {
			return;
		}
		if (url != null && fJavadocLocation != null) {
			if (url.toString().equals(fJavadocLocation.toString())) {
				// no change
				return;
			}
		}
		
		PropertyChangeEvent event = new PropertyChangeEvent(this, IVMInstallChangedListener.PROPERTY_JAVADOC_LOCATION, fJavadocLocation, url);		
		fJavadocLocation = url;
		if (fNotify) {
			JavaRuntime.fireVMChanged(event);
		}
	}

	/**
	 * Whether this VM should fire property change notifications.
	 * 
	 * @param notify
	 *  
	 */
	protected void setNotify(boolean notify) {
		fNotify = notify;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
     *  
	 */
	public boolean equals(Object object) {
		if (object instanceof IVMInstall) {
			IVMInstall vm = (IVMInstall)object;
			return getVMInstallType().equals(vm.getVMInstallType()) &&
				getId().equals(vm.getId());
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 *  
	 */
	public int hashCode() {
		return getVMInstallType().hashCode() + getId().hashCode();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.launching.IVMInstall#getDefaultVMArguments()
	 *  
	 */
	public String[] getVMArguments() {
		String args = getVMArgs();
		if (args == null) {
		    return null;
		}
		ExecutionArguments ex = new ExecutionArguments(args, ""); //$NON-NLS-1$
		return ex.getVMArgumentsArray();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.launching.IVMInstall#setDefaultVMArguments(java.lang.String[])
	 *  
	 */
	public void setVMArguments(String[] vmArgs) {
		if (vmArgs == null) {
			setVMArgs(null);
		} else {
		    StringBuffer buf = new StringBuffer();
		    for (int i = 0; i < vmArgs.length; i++) {
	            String string = vmArgs[i];
	            buf.append(string);
	            buf.append(" "); //$NON-NLS-1$
	        }
			setVMArgs(buf.toString().trim());
		}
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.wst.jsdt.launching.IVMInstall2#getVMArgs()
     */
    public String getVMArgs() {
        return fVMArgs;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.wst.jsdt.launching.IVMInstall2#setVMArgs(java.lang.String)
     */
    public void setVMArgs(String vmArgs) {
        if (fVMArgs == null) {
            if (vmArgs == null) {
                // No change
                return;
            }
        } else if (fVMArgs.equals(vmArgs)) {
    		// No change
    		return;
    	}
        PropertyChangeEvent event = new PropertyChangeEvent(this, IVMInstallChangedListener.PROPERTY_VM_ARGUMENTS, fVMArgs, vmArgs);
        fVMArgs = vmArgs;
		if (fNotify) {
			JavaRuntime.fireVMChanged(event);		
		}
    }	
    
    /* (non-Javadoc)
     * Subclasses should override.
     * @see org.eclipse.wst.jsdt.launching.IVMInstall2#getJavaVersion()
     */
    public String getJavaVersion() {
        return null;
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.launching.IVMInstall3#evaluateSystemProperties(java.lang.String[], org.eclipse.core.runtime.IProgressMonitor)
	 */
    
// JSDT never called    
//	public Map evaluateSystemProperties(String[] properties, IProgressMonitor monitor) throws CoreException {
//		//locate the launching support jar - it contains the main program to run
//		if (monitor == null) {
//			monitor = new NullProgressMonitor();
//		}
//		Map map = new HashMap();
//		
//		// first check cache (preference store) to avoid launching VM
//		Preferences preferences = JavaRuntime.getPreferences();
//		boolean cached = true; 
//		for (int i = 0; i < properties.length; i++) {
//			String property = properties[i];
//			String key = getSystemPropertyKey(property);
//			if (preferences.contains(key)) {
//				String value = preferences.getString(key);
//				map.put(property, value);
//			} else {
//				map.clear();
//				cached = false;
//				break;
//			}
//		}
//		if (!cached) {		
//			// launch VM to evaluate properties
//			File file = LaunchingPlugin.getFileInPlugin(new Path("lib/launchingsupport.jar")); //$NON-NLS-1$
//			if (file.exists()) {
//				String javaVersion = getJavaVersion();
//				boolean hasXMLSupport = false;
//				if (javaVersion != null) {
//					hasXMLSupport = true;
//					if (javaVersion.startsWith(JavaScriptCore.VERSION_1_1) ||
//							javaVersion.startsWith(JavaScriptCore.VERSION_1_2) ||
//							javaVersion.startsWith(JavaScriptCore.VERSION_1_3)) {
//						hasXMLSupport = false;
//					}
//				}
//				String mainType = null;
//				if (hasXMLSupport) {
//					mainType = "org.eclipse.wst.jsdt.internal.launching.support.SystemProperties"; //$NON-NLS-1$
//				} else {
//					mainType = "org.eclipse.wst.jsdt.internal.launching.support.LegacySystemProperties"; //$NON-NLS-1$
//				}
//				VMRunnerConfiguration config = new VMRunnerConfiguration(mainType, new String[]{file.getAbsolutePath()});
//				IVMRunner runner = getVMRunner(ILaunchManager.RUN_MODE);
//				if (runner == null) {
//					abort(LaunchingMessages.AbstractVMInstall_0, null, IJavaLaunchConfigurationConstants.ERR_INTERNAL_ERROR);
//				}
//				config.setProgramArguments(properties);
//				Launch launch = new Launch(null, ILaunchManager.RUN_MODE, null);
//				if (monitor.isCanceled()) {
//					return map;
//				}
//				monitor.beginTask(LaunchingMessages.AbstractVMInstall_1, 2);
//				runner.run(config, launch, monitor);
//				IProcess[] processes = launch.getProcesses();
//				if (processes.length != 1) {
//					abort(LaunchingMessages.AbstractVMInstall_0, null, IJavaLaunchConfigurationConstants.ERR_INTERNAL_ERROR);
//				}
//				IProcess process = processes[0];
//				try {
//					int total = 0;
//					int max = JavaRuntime.getPreferences().getInt(JavaRuntime.PREF_CONNECT_TIMEOUT);
//					while (!process.isTerminated()) {
//						try {
//							if (total > max) {
//								break;
//							}
//							Thread.sleep(50);
//							total+=50;
//						} catch (InterruptedException e) {
//						}
//					}
//				} finally {
//					if (!launch.isTerminated()) {
//						launch.terminate();
//					}
//				}
//				monitor.worked(1);
//				if (monitor.isCanceled()) {
//					return map;
//				}
//				
//				monitor.subTask(LaunchingMessages.AbstractVMInstall_3);
//				IStreamsProxy streamsProxy = process.getStreamsProxy();
//				String text = null;
//				if (streamsProxy != null) {
//					text = streamsProxy.getOutputStreamMonitor().getContents();
//				}
//				if (text != null && text.length() > 0) {
//					try {
//						DocumentBuilder parser = LaunchingPlugin.getParser();
//						Document document = parser.parse(new ByteArrayInputStream(text.getBytes()));
//						Element envs = document.getDocumentElement();
//						NodeList list = envs.getChildNodes();
//						int length = list.getLength();
//						for (int i = 0; i < length; ++i) {
//							Node node = list.item(i);
//							short type = node.getNodeType();
//							if (type == Node.ELEMENT_NODE) {
//								Element element = (Element) node;
//								if (element.getNodeName().equals("property")) { //$NON-NLS-1$
//									String name = element.getAttribute("name"); //$NON-NLS-1$
//									String value = element.getAttribute("value"); //$NON-NLS-1$
//									map.put(name, value);
//								}
//							}
//						}			
//					} catch (SAXException e) {
//						abort(LaunchingMessages.AbstractVMInstall_4, e, IJavaLaunchConfigurationConstants.ERR_INTERNAL_ERROR);
//					} catch (IOException e) {
//						abort(LaunchingMessages.AbstractVMInstall_4, e, IJavaLaunchConfigurationConstants.ERR_INTERNAL_ERROR);
//					}
//				} else {
//					abort(LaunchingMessages.AbstractVMInstall_0, null, IJavaLaunchConfigurationConstants.ERR_INTERNAL_ERROR);
//				}
//				monitor.worked(1);
//			} else {
//				abort(LaunchingMessages.AbstractVMInstall_0, null, IJavaLaunchConfigurationConstants.ERR_INTERNAL_ERROR);
//			}
//			// cache for future reference
//			Iterator keys = map.keySet().iterator();
//			while (keys.hasNext()) {
//				String property = (String)keys.next();
//				String value = (String) map.get(property);
//				String key = getSystemPropertyKey(property);
//				preferences.setValue(key, value);
//			}
//		}
//		monitor.done();
//		return map;
//	}

	/**
	 * Generates a key used to cache system property for this VM in this plug-ins
	 * preference store.
	 * 
	 * @param property system property name
	 * @return preference store key
	 */
//	private String getSystemPropertyKey(String property) {
//		StringBuffer buffer = new StringBuffer();
//		buffer.append(PREF_VM_INSTALL_SYSTEM_PROPERTY);
//		buffer.append("."); //$NON-NLS-1$
//		buffer.append(getVMInstallType().getId());
//		buffer.append("."); //$NON-NLS-1$
//		buffer.append(getId());
//		buffer.append("."); //$NON-NLS-1$
//		buffer.append(property);
//		return buffer.toString();
//	}
	
	/**
	 * Throws a core exception with an error status object built from the given
	 * message, lower level exception, and error code.
	 * 
	 * @param message the status message
	 * @param exception lower level exception associated with the error, or
	 *            <code>null</code> if none
	 * @param code error code
	 * @throws CoreException the "abort" core exception
	 *  
	 */
	protected void abort(String message, Throwable exception, int code) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, JavaScriptCore.PLUGIN_ID, code, message, exception));
	}	
    
}
