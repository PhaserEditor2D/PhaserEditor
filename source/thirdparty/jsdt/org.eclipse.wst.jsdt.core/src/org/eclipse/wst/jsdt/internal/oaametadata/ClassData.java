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

import java.util.ArrayList;

public class ClassData extends VersionableElement{

	
/*	class_element = element class {
		  class_content  &  class_attributes  &  foreign_nodes
		}
		class_content = (
		  aliases_element?  &  ancestors_element?  &  constructors_element?  &  
		  mixes_element?  &  methods_element?  &  properties_element?  &  
		  descriptive_elements  &  compatibility_elements
		)
		class_attributes = (
		  name  &  superclass?  &  visibility?  &
		  getterPattern?  &  setterPattern?  
		)
		
	
	interface_element = element interface {
		  interface_content  &  interface_attributes  &  foreign_nodes
		}
		interface_content = (
		  constructors_element?  &  exceptions_element?  &  mixes_element?  &  
		  methods_element?  &  properties_element?  &  
		  descriptive_elements  &  compatibility_elements
		  # Research the above, make consistent with the spec  
		  # FIXME: aliases? ancestors?
		)
		interface_attributes = (
		  name?  &  superclass?  &  visibility?  &
		  getterPattern?  &  setterPattern?  
		)
		
		
*/		

	public Ancestor [] ancestors;
	public Alias [] aliases;
	public Method [] constructors;
	public Event [] events;
	public Method [] methods;
	public Property [] fields;
	public Property [] properties;
	public Mix [] mixins;

	public String name;
	public String superclass;
	public String visibility; 
	public String getterPattern; 
	public String setterPattern; 
	public boolean isInterface;
	
	public Property [] getFields()
	{
		ArrayList list = new ArrayList();
		if (this.fields!=null)
			for (int i = 0; i < this.fields.length; i++) {
				if (this.fields[i].isField)
					list.add(this.fields[i]);
		}
		return (Property [] )list.toArray(new Property[list.size()]);
	}
	
	public Property [] getProperties()
	{
		ArrayList list = new ArrayList();
		if (this.fields!=null)
			for (int i = 0; i < this.fields.length; i++) {
				if (!this.fields[i].isField)
					list.add(this.fields[i]);
		}
		return (Property [] )list.toArray(new Property[list.size()]);
	}
	

	public Method getMethod(String name)
	{
		if (this.methods!=null)
			for (int i = 0; i < this.methods.length; i++) {
				if (this.methods[i].name.equals(name))
					return this.methods[i];
				
			}
		return null;
	}

	public Property getField(String elementName) {
        if (this.fields!=null)
        	for (int i = 0; i < this.fields.length; i++) {
				if (elementName.equals( this.fields[i].name))
					return this.fields[i];
			}
        if (this.properties!=null)
        	for (int i = 0; i < this.properties.length; i++) {
				if (elementName.equals( this.properties[i].name))
					return this.properties[i];
			}
        return null;

	}
	
	public String getSuperClass()
	{
		if (superclass!=null && superclass.length()>0)
			return superclass;
		if (this.ancestors!=null )
			for (int i = 0; i < this.ancestors.length; i++) {
//TODO:   should be other attributes on ancestor to check, not yet in spec				
				String dataType = this.ancestors[i].dataType;
				if (dataType!=null&& dataType.length()>0)
					return dataType;
			}
		return null;
	}
}
