/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.oaametadata;

public class LibraryAPIs {
	public ClassData[] classes;
	
	public Method [] globalMethods;
	public Property[] globalVars;
	
//	interface
//	license
	
	
	public String description;
	public Author[] authors;
	public char[] fileName;
	public Enum[] enums;
	public Mixin [] mixins;
	public Namespace [] namespaces;

		// Attributes
	public String libraryVersion;
	public String language;
	public String getterPattern;
	public String setterPattern;
	public String spec;
	
//	  alias_element*  &  aliases_element?  &  class_element*  &  classes_element?  &
//			  globals_element?  &  interface_element*  &  interfaces_element?  &  license_element?  &  
//			  mixin_element*  &  mixins_element?  &  namespace_element*  &  namespaces_element?  &
//			  descriptive_elements 
	Alias [] aliases;
	
	
	public Property getGlobalVar(String name) {
		if (this.globalVars!=null)
			for (int i = 0; i < this.globalVars.length; i++) {
				if (name.equals(this.globalVars[i].name))
					return this.globalVars[i];
			}
			return null;
	}
	public ClassData getClass(String name) {
		if (this.classes!=null)
			for (int i = 0; i < this.classes.length; i++) {
				if (name.equals(this.classes[i].name))
					return this.classes[i];
			}
			return null;
	}
	public Method getGlobalMethod(String name) {
		if (this.globalMethods!=null)
			for (int i = 0; i < this.globalMethods.length; i++) {
				if (name.equals(this.globalMethods[i].name))
					return this.globalMethods[i];
			}
			return null;
	}
	public Enum getEnum(String name) {
		if (this.enums!=null)
			for (int i = 0; i < this.enums.length; i++) {
				if (name.equals(this.enums[i].name))
					return this.enums[i];
			}
			return null;
	}
	
	
}
