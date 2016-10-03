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
package org.eclipse.wst.jsdt.core.refactoring;

import org.eclipse.wst.jsdt.core.IJavaScriptElement;

/**
 * An <code>IJavaScriptElementMapper</code> provides methods to map an original
 * elements to its refactored counterparts.
 * <p>
 * An <code>IJavaScriptElementMapper</code> can be obtained via 
 * {@link org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getAdapter(Class)}. 
 * </p>
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IJavaScriptElementMapper {
	
	/**
	 * Returns the refactored Java element for the given element.
	 * The returned Java element might not yet exist when the method 
	 * is called.
	 * </p>
	 * Note that local variables <strong>cannot</strong> be mapped 
	 * using this method.
	 * <p>
	 * 
	 * @param element the element to be refactored
	 * 
	 * @return the refactored element for the given element
	 */
	IJavaScriptElement getRefactoredJavaScriptElement(IJavaScriptElement element);
}
