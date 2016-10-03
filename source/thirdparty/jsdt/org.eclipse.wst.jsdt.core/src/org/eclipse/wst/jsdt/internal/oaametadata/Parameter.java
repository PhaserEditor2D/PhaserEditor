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
package org.eclipse.wst.jsdt.internal.oaametadata;

public class Parameter extends VersionableElement {
//	parameter_element = element parameter {
//		  parameter_content  &  parameter_attributes  &  foreign_nodes
//		}
//		parameter_content = (
//		  options_element?  &  
//		  descriptive_elements  &  compatibility_elements
//		  # Research the above, make consistent with the spec  
//		)
//		parameter_attributes = (
//		  datatype?  &  name?  &  usage?  &
//		  datatype_supplemental_attributes  # FIXME: Is this correct?
//		)
		
		
	public String name;
	public String dataType;
	public String usage; 

}
