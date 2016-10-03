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

public interface IOAAMetaDataConstants {

	public final static String METADATA_FILE="oam.xml";
	
	public final static String NAMESPACE_API="http://openajax.org/metadata";
	
    public final static String TAG_ABOUTME="aboutMe";
	public final static String TAG_ALIAS="alias";
	public final static String ATTRIBUTE_ALAIS_NAME="name";
	public final static String ATTRIBUTE_ALAIS_TYPE="datatype";
    public final static String TAG_ALIASES  ="aliases";
    public final static String TAG_ANCESTORS = "ancestors";
    public final static String TAG_ANCESTOR = "ancestor";
    public final static String ATTRIBUTE_ANCESTOR_DATATYPE = "datatype";
	
    public final static String TAG_API="api";
	public final static String ATTRIBUTE_API_VERSION ="version";
	public final static String ATTRIBUTE_API_LANGUAGE ="language";
	public final static String ATTRIBUTE_API_GETTERPATTERN="getterPattern";
	public final static String ATTRIBUTE_API_SETTERPATTERN="setterPattern";
	public final static String ATTRIBUTE_API_SPEC="spec";
	
    public final static String TAG_AUTHOR="author";
    public final static String ATTRIBUTE_AUTHOR_EMAIL ="email";
    public final static String ATTRIBUTE_AUTHOR_LOCATION ="location";
    public final static String ATTRIBUTE_AUTHOR_NAME ="name";
    public final static String ATTRIBUTE_AUTHOR_ORGANIZATION ="organization";
    public final static String ATTRIBUTE_AUTHOR_PHOTO ="photo";
    public final static String ATTRIBUTE_AUTHOR_TYPE ="type";
    public final static String ATTRIBUTE_AUTHOR_WEBSITE ="website";
    public final static String TAG_AUTHORS ="authors";
    public final static String TAG_AVAILABLE ="available";
    public final static String ATTRIBUTE_AVAILABLE_VERSION ="version";
    public final static String TAG_CLASS ="class";
    public final static String ATTRIBUTE_CLASS_NAME ="name";
    public final static String ATTRIBUTE_CLASS_SUPERCLASS ="superclass";
    public final static String ATTRIBUTE_CLASS_VISIBILITY ="visibility"; 
    public final static String ATTRIBUTE_CLASS_GETTERPATTERN="getterPattern";
    public final static String ATTRIBUTE_CLASS_SETTERPATTERN="setterPattern";
    public final static String TAG_CLASSES ="classes";
    public final static String TAG_CONSTRUCTOR ="constructor";
    public final static String ATTRIBUTE_CONSTRUCTOR_SCOPE ="scope";
    public final static String ATTRIBUTE_CONSTRUCTOR_VISIBILITY ="visibility"; 
    public final static String TAG_CONSTRUCTORS ="constructors";
    public final static String TAG_DEPRECIATED ="deprecated";
    public final static String ATTRIBUTE_DEPRECIATED_VERSION = "version";
    public final static String TAG_DESCRIPTION ="description";
    public final static String ATTRIBUTE_DESCRIPTION_TYPE ="type";
    public final static String TAG_ENUM ="enum";
    public final static String ATTRIBUTE_ENUM_NAME ="name";
    public final static String ATTRIBUTE_ENUM_DATATYPE ="datatype";
    public final static String TAG_ENUMS ="enums";
    public final static String TAG_EVENT ="event";
    public final static String TAG_EVENTS ="events";
    public final static String TAG_EXAMPLE ="example";
    public final static String TAG_EXAMPLES ="examples";
    public final static String TAG_EXCEPTION ="exception";
    public final static String ATTRIBUTE_EXCEPTION_TYPE ="type";
    public final static String TAG_EXCEPTIONS ="exceptions";
    public final static String TAG_FIELD ="field";
    public final static String ATTRIBUTE_FIELD_NAME = "name";
    public final static String ATTRIBUTE_FIELD_DATATYPE = "datatype";
    public final static String ATTRIBUTE_FIELD_VISIBILITY = "visibility"; 
    public final static String ATTRIBUTE_FIELD_SCOPE = "scope"; 
    public final static String TAG_FIELDS ="fields";
    public final static String TAG_FRAGMENT ="fragment";
    public final static String TAG_GLOBALS ="globals";
    public final static String TAG_INCLUDE ="include";
    public final static String ATTRIBUTE_INCLUDE_SRC ="src";
    public final static String TAG_INTERFACE ="interface";
    public final static String ATTRIBUTE_INTERFACE_NAME ="name";
    public final static String ATTRIBUTE_INTERFACE_TYPE ="type";
    public final static String ATTRIBUTE_INTERFACE_SUPERCLASS ="superclass";
    public final static String ATTRIBUTE_INTERFACE_VISIBILITY ="visibility"; 
    public final static String TAG_INTERFACES ="interfaces";
    public final static String TAG_LICENSE ="license";
    public final static String TAG_METHOD ="method";
    public final static String ATTRIBUTE_METHOD_NAME ="name";
    public final static String ATTRIBUTE_METHOD_SCOPE ="scope";
    public final static String TAG_METHODS ="methods";
    public final static String TAG_MIX ="mix";
    public final static String ATTRIBUTE_MIX_DATATYPE ="datatype";
    public final static String ATTRIBUTE_MIX_FROMSCOPE ="fromScope";
    public final static String ATTRIBUTE_MIX_TOSCOPE ="toScope"; 
    public final static String TAG_MIXES ="mixes";
    public final static String TAG_MIXIN ="mixin";
    public final static String ATTRIBUTE_MIXIN_NAME ="name";
    public final static String ATTRIBUTE_MIXIN_SCOPE ="scope";
    public final static String ATTRIBUTE_MIXIN_VISIBILITY ="visibility";
	public final static String TAG_MIXINS ="mixins";
	public final static String TAG_NAMESPACE ="namespace";
	public final static String ATTRIBUTE_NAMESPACE_NAME ="name";
	public final static String ATTRIBUTE_NAMESPACE_VISIBILITY ="visibility";
	public final static String TAG_NAMESPACES ="namespaces";
    public final static String TAG_OPTION ="option";
    public final static String ATTRIBUTE_OPTION_LABEL ="label";
    public final static String ATTRIBUTE_OPTION_VALUE ="value";
	public final static String TAG_OPTIONS ="options";
    public final static String TAG_PARAMETER ="parameter";
    public final static String ATTRIBUTE_PARAMETER_NAME ="name";
    public final static String ATTRIBUTE_PARAMETER_DATATYPE ="datatype";
    public final static String ATTRIBUTE_PARAMETER_USAGE ="usage"; 
    public final static String TAG_PARAMETERS ="parameters";
    public final static String TAG_PROPERTIES ="properties";
    public final static String TAG_PROPERTY ="property";
    public final static String TAG_QOUTE="quote";
    public final static String TAG_REMARKS ="remarks";
    public final static String TAG_RETURNS ="returns";
    public final static String ATTRIBUTE_RETURNS_DATATYPE ="datatype";
	public final static String TAG_SEEALSO ="seealso";
	public final static String TAG_TITLE ="title";
	public final static String TAG_TOPIC ="topic";
	public final static String TAG_TOPICS ="topics";
	public final static String TAG_USERAGENT ="useragent";
	public final static String TAG_USERAGENTS ="useragents";
	
	
	public final static String USAGE_STATIC ="static";
	public final static String USAGE_INSTANCE ="instance";
	
	public final static String VISIBILITY_PUBLIC ="public";
	public final static String VISIBILITY_PRIVATE ="private";
	public final static String VISIBILITY_PROTECTED ="protected";
	public final static String VISIBILITY_INTERNAL ="internal";
	public final static String VISIBILITY_PROTECTED_INTERNAL ="protected-internal";

	
	public final static String  MIME_TYPE_TEXT="text/plain";
	public final static String MIME_TYPE_HTML="text/html";

//	
//	LOCALIZATION
//	
	public final static String TAG_MESSAGEBUNDLE ="messagebundle";
	public final static String TAG_MSG ="msg";
    public final static String ATTRIBUTE_MSG_NAME ="name";

    public final static String VARIABLE_SUBSTITUTION_STRING ="%%";
    
    
	
}
