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


import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;

import com.ibm.icu.text.MessageFormat;
 
/**
 * Abstract implementation of a VM install type.
 * Subclasses should implement
 * <ul>
 * <li><code>IVMInstall doCreateVMInstall(String id)</code></li>
 * <li><code>String getName()</code></li>
 * <li><code>IStatus validateInstallLocation(File installLocation)</code></li>
 * </ul>
 * <p>
 * Clients implementing VM install types should subclass this class.
 * </p>
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */

public abstract class AbstractVMInstallType implements IVMInstallType, IExecutableExtension {
	private List fVMs;
	private String fId;
	
	/**
	 * Constructs a new VM install type.
	 */
	protected AbstractVMInstallType() {
		fVMs= new ArrayList(10);
	}

	/* (non-Javadoc)
	 * Subclasses should not override this method.
	 * @see IVMType#getVMs()
	 */
	public IVMInstall[] getVMInstalls() {
		IVMInstall[] vms= new IVMInstall[fVMs.size()];
		return (IVMInstall[])fVMs.toArray(vms);
	}

	/* (non-Javadoc)
	 * Subclasses should not override this method.
	 * @see IVMType#disposeVM(String)
	 */
	public void disposeVMInstall(String id) {
		for (int i= 0; i < fVMs.size(); i++) {
			IVMInstall vm= (IVMInstall)fVMs.get(i);
			if (vm.getId().equals(id)) {
				fVMs.remove(i);
				JavaRuntime.fireVMRemoved(vm);
				return;
			}
		}
	}

	/* (non-Javadoc)
	 * Subclasses should not override this method.
	 * @see IVMType#getVM(String)
	 */
	public IVMInstall findVMInstall(String id) {
		for (int i= 0; i < fVMs.size(); i++) {
			IVMInstall vm= (IVMInstall)fVMs.get(i);
			if (vm.getId().equals(id)) {
				return vm;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * Subclasses should not override this method.
	 * @see IVMType#createVM(String)
	 */
	public IVMInstall createVMInstall(String id) throws IllegalArgumentException {
		if (findVMInstall(id) != null) {
			String format= LaunchingMessages.vmInstallType_duplicateVM; 
			throw new IllegalArgumentException(MessageFormat.format(format, new String[] { id }));
		}
		IVMInstall vm= doCreateVMInstall(id);
		fVMs.add(vm);
		return vm;
	}
	
	/**
	 * Subclasses should return a new instance of the appropriate
	 * <code>IVMInstall</code> subclass from this method.
	 * @param	id	The vm's id. The <code>IVMInstall</code> instance that is created must
	 * 				return <code>id</code> from its <code>getId()</code> method.
	 * 				Must not be <code>null</code>.
	 * @return	the newly created IVMInstall instance. Must not return <code>null</code>.
	 */
	protected abstract IVMInstall doCreateVMInstall(String id);

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	/**
	 * Initializes the id parameter from the "id" attribute
	 * in the configuration markup.
	 * Subclasses should not override this method.
	 * @param config the configuration element used to trigger this execution. 
	 *		It can be queried by the executable extension for specific
	 *		configuration properties
	 * @param propertyName the name of an attribute of the configuration element
	 *		used on the <code>createExecutableExtension(String)</code> call. This
	 *		argument can be used in the cases where a single configuration element
	 *		is used to define multiple executable extensions.
	 * @param data adapter data in the form of a <code>String</code>, 
	 *		a <code>Hashtable</code>, or <code>null</code>.
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
		fId= config.getAttribute("id"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * Subclasses should not override this method.
	 * @see IVMType#getId()
	 */
	public String getId() {
		return fId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMInstallType#findVMInstallByName(java.lang.String)
	 */
	public IVMInstall findVMInstallByName(String name) {
		for (int i= 0; i < fVMs.size(); i++) {
			IVMInstall vm= (IVMInstall)fVMs.get(i);
			if (vm.getName().equals(name)) {
				return vm;
			}
		}
		return null;
	}

	/**
	 * Returns a URL for the default jsdoc location of a VM installed at the
	 * given home location, or <code>null</code> if none. The default
	 * implementation returns <code>null</code>, subclasses must override as
	 * appropriate.
	 * <p>
	 * Note, this method would ideally be added to <code>IVMInstallType</code>,
	 * but it would have been a breaking API change between 2.0 and 2.1. Thus,
	 * it has been added to the abstract base class that VM install types should
	 * subclass.
	 * </p>
	 * 
	 * @param installLocation home location
	 * @return default jsdoc location or <code>null</code>
	 *  
	 */
	public URL getDefaultJavadocLocation(File installLocation) {
		return null;		
	}
}
