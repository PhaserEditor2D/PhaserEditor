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
package org.eclipse.wst.jsdt.internal.corext.refactoring.nls;

import org.eclipse.jface.text.Region;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;


public class AccessorClassReference {
    
    private ITypeBinding fBinding;
    private Region fRegion;
    private String fResourceBundleName;
    
    public AccessorClassReference(ITypeBinding typeBinding, String resourceBundleName, Region accessorRegion) {
        super();
        fBinding= typeBinding;
        fRegion= accessorRegion;
        fResourceBundleName= resourceBundleName;
    }
    
	public ITypeBinding getBinding() {
		return fBinding;
	}

	public String getName() {
		return fBinding.getName();
	}

	public Region getRegion() {
		return fRegion;
	}
	
	public String getResourceBundleName() {
		return fResourceBundleName;
	}
	
    public boolean equals(Object obj) {
        if (obj instanceof AccessorClassReference) {
            AccessorClassReference cmp = (AccessorClassReference) obj;
            return fBinding == cmp.fBinding;          
        }
        return false;        
    }
    
    public int hashCode() {
        return fBinding.hashCode();
    }
}
