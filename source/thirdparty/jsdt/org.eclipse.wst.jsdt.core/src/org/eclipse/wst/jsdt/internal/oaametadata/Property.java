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

public class Property extends VersionableElement{

	
//	field_element = element field {
//		  field_content  &  field_attributes  &  foreign_nodes
//		}
//		field_content = (
//		  descriptive_elements  &  compatibility_elements
//		  # Research the above, make consistent with the spec  
//		)
//		field_attributes = (
//		  name?  &   datatype?  &  visibility?  &  scope?  &
//		  datatype_supplemental_attributes  # FIXME: Is this correct?
//		  # <scope>?
//		)	
	
//	property_element = element property {
//		  property_content  &  property_attributes  &  foreign_nodes
//		}
//		property_content = (
//		  options_element?  &  
//		  descriptive_elements  &  compatibility_elements
//		)
//		property_attributes = (
//		  datatype?  &  default_attribute?  &  hidden?  &  name?  &  readonly?  &  
//		  required?  &  scope?  &  transient?  &  urlparam  &  visibility  &
//		  datatype_supplemental_attributes  &  pattern_attributes  &  pubsub_attributes
//		)
	
	
	public String name;
	public String dataType;
	public String visibility;
	public String scope;
	public boolean isField; 
	
	public boolean isStatic()
	{
		return IOAAMetaDataConstants.USAGE_STATIC.equals(scope);
	}

}
