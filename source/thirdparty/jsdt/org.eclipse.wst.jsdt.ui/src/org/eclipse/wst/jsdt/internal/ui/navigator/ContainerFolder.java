/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * 
 */
package org.eclipse.wst.jsdt.internal.ui.navigator;


/**
 * @author childsb
 *
 */
public class ContainerFolder {
	

	
	
		Object parent;
		String name;
		public ContainerFolder(String fullPath, Object parent){

			this.parent = parent;
			name = fullPath;
		}
		
		public Object getParentObject() {
			return parent;
		}
		
		public String getName() {
			return name;
			
		}
		public String toString() { return name;}
	
}
