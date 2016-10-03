/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
 
/**
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class StandardVM extends AbstractVMInstall {
	StandardVM(IVMInstallType type, String id) {
		super(type, id);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMInstall#getVMRunner(java.lang.String)
	 */
	public IVMRunner getVMRunner(String mode) {
//		if (ILaunchManager.RUN_MODE.equals(mode)) {
//			return new StandardVMRunner(this);
//		} else if (ILaunchManager.DEBUG_MODE.equals(mode)) {
//			return new StandardVMDebugger(this);
//		}
//		return null;
		//TODO: implement
		throw new org.eclipse.wst.jsdt.core.UnimplementedException();
	}

    /* (non-Javadoc)
     * @see org.eclipse.jdt.launching.IVMInstall#getJavaVersion()
     */
    public String getJavaVersion() {
        StandardVMType installType = (StandardVMType) getVMInstallType();
        File installLocation = getInstallLocation();
        if (installLocation != null) {
            File executable = StandardVMType.findJavaExecutable(installLocation);
            if (executable != null) {
                String vmVersion = installType.getVMVersion(installLocation, executable);
                // strip off extra info
                StringBuffer version = new StringBuffer();
                for (int i = 0; i < vmVersion.length(); i++) {
                    char ch = vmVersion.charAt(i);
                    if (Character.isDigit(ch) || ch == '.') {
                        version.append(ch);
                    } else {
                        break;
                    }
                }
                if (version.length() > 0) {
                    return version.toString();
                }
            }
        }
        return null;
    }
}
