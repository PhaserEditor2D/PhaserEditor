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

public class Event extends VersionableElement {

	public Parameter [] parameters;
	public ReturnsData returns;
	/*
	event_element = element event {
		  event_content  &  event_attributes  &  foreign_nodes
		}
		event_content = (
		  parameters_element?  &  returns_element?  &  
		  descriptive_elements  &  compatibility_elements
		  # Research the above, make consistent with the spec  
		  # <parameters>? <returns>?
		)
		event_attributes = (
		  empty
		)
	*/
	
}
