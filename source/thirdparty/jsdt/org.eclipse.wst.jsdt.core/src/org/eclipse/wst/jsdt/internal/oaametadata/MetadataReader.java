/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.oaametadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.internal.core.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class MetadataReader extends DefaultHandler implements IOAAMetaDataConstants
{

	LibraryAPIs apis=new LibraryAPIs();
	
	Stack stack=new Stack();

	HashMap states=new HashMap();

	boolean collectText=false;
	boolean collectHTML=false;
	boolean pendingEndElement=false;
	
	boolean doTranslation=true;

	String collectTextElement;

	int currentState=STATE_API;
	Object currentObject;

	StringBuffer text=new StringBuffer();
	HashMap collections;

	String filePath;

	HashMap messages=new HashMap();
	
	
	static class StackElement {
		HashMap collections;
		Object currentObject;
		int state;
		
		StackElement(int st, Object obj, HashMap map)
		{
			this.collections=map;
			this.currentObject=obj;
			this.state=st;
		}
	}
	
	static final int STATE_ABOUTME=1;
	static final int STATE_ALIAS=2;
	static final int STATE_ALAISES=3;
	static final int STATE_ANCESTOR=4;
	static final int STATE_ANCESTORS=5;
	static final int STATE_API=6;
	static final int STATE_AUTHOR=7;
	static final int STATE_AUTHORS=8;
	static final int STATE_AVAILABLE= 9;
	static final int STATE_CONSTRUCTOR=10;
	static final int STATE_CONSTRUCTORS=11;
	static final int STATE_CLASS=12;
	static final int STATE_CLASSES=13;

	static final int STATE_DEPRECIATED =	14;
	static final int STATE_DESCRIPTION =15;
    static final int STATE_ENUM =16;
    static final int STATE_ENUMS =17;
    static final int STATE_EVENT =18;
    static final int STATE_EVENTS =19;
    static final int STATE_EXAMPLE =20;
    static final int STATE_EXAMPLES =21;
    static final int STATE_EXCEPTION =22;
    static final int STATE_EXCEPTIONS =23;
    static final int STATE_FIELD =24;
    static final int STATE_FIELDS =25;
	static final int STATE_GLOBALS=26;
    static final int STATE_INTERFACE =27;
    static final int STATE_INTERFACES =28;
    static final int STATE_LICENSE =29;
	static final int STATE_METHOD=30;
    static final int STATE_METHODS =31;
    static final int STATE_MIX =32;
    static final int STATE_MIXES =33;
    static final int STATE_MIXIN =34;
	static final int STATE_MIXINS =35;
	static final int STATE_NAMESPACE =36;
	static final int STATE_NAMESPACES =37;
    static final int STATE_OPTION =38;
	static final int STATE_OPTIONS =39;
    static final int STATE_PARAMETER =40;
    static final int STATE_PARAMETERS =41;
	static final int STATE_PROPERTY=42;
	static final int STATE_PROPERTIES=43;
    static final int STATE_QOUTE =44;
    static final int STATE_REMARKS =45;
    static final int STATE_RETURNS =46;
	static final int STATE_SEEALSO =47;
	static final int STATE_TITLE=48;
	static final int STATE_TOPIC =49;
	static final int STATE_TOPICS =50;
	static final int STATE_USERAGENT =51;
	static final int STATE_USERAGENTS =52;

	static final int STATE_INCLUDE =53;

	static final ArrayList EMPTY_LIST=new ArrayList();

	{
		states.put(TAG_ABOUTME, Integer.valueOf(STATE_ABOUTME));
		states.put(TAG_API, Integer.valueOf(STATE_API));
		states.put(TAG_AUTHOR, Integer.valueOf(STATE_AUTHOR));
		states.put(TAG_AVAILABLE, Integer.valueOf(STATE_AVAILABLE));
		states.put(TAG_CLASS, Integer.valueOf(STATE_CLASS));
		states.put(TAG_CLASSES, Integer.valueOf(STATE_CLASSES));
		states.put(TAG_ENUM, Integer.valueOf(STATE_ENUM));
		states.put(TAG_ENUMS, Integer.valueOf(STATE_ENUMS));
		states.put(TAG_EXAMPLE, Integer.valueOf(STATE_EXAMPLE));
		states.put(TAG_EXAMPLES, Integer.valueOf(STATE_EXAMPLES));
		states.put(TAG_GLOBALS, Integer.valueOf(STATE_GLOBALS));
		states.put(TAG_METHOD, Integer.valueOf(STATE_METHOD));
		states.put(TAG_PROPERTY, Integer.valueOf(STATE_PROPERTY));
		states.put(TAG_PROPERTIES, Integer.valueOf(STATE_PROPERTIES));
		states.put(TAG_ALIAS, Integer.valueOf(STATE_ALIAS));
		states.put(TAG_ALIASES, Integer.valueOf(STATE_ALAISES));
		states.put(TAG_ANCESTORS, Integer.valueOf(STATE_ANCESTORS));
		states.put(TAG_ANCESTOR, Integer.valueOf(STATE_ANCESTOR));
		states.put(TAG_AUTHORS, Integer.valueOf(STATE_AUTHORS));
		states.put(TAG_AUTHOR, Integer.valueOf(STATE_AUTHOR));
		states.put(TAG_CONSTRUCTORS, Integer.valueOf(STATE_CONSTRUCTORS));
		states.put(TAG_CONSTRUCTOR, Integer.valueOf(STATE_CONSTRUCTOR));


		states.put(TAG_DEPRECIATED, Integer.valueOf(STATE_DEPRECIATED));
		states.put(TAG_DESCRIPTION, Integer.valueOf(STATE_DESCRIPTION ));
	    states.put(TAG_ENUM, Integer.valueOf(STATE_ENUM ));
	    states.put(TAG_EVENT, Integer.valueOf(STATE_EVENT ));
	    states.put(TAG_EVENTS, Integer.valueOf(STATE_EVENTS));
	    states.put(TAG_EXAMPLE, Integer.valueOf(STATE_EXAMPLE ));
	    states.put(TAG_EXAMPLES, Integer.valueOf(STATE_EXAMPLES ));
	    states.put(TAG_EXCEPTION, Integer.valueOf(STATE_EXCEPTION ));
	    states.put(TAG_EXCEPTIONS, Integer.valueOf(STATE_EXCEPTIONS));
	    states.put(TAG_FIELD, Integer.valueOf(STATE_FIELD ));
	    states.put(TAG_FIELDS, Integer.valueOf(STATE_FIELDS ));
	    states.put(TAG_INTERFACE, Integer.valueOf(STATE_INTERFACE ));
	    states.put(TAG_INTERFACES, Integer.valueOf(STATE_INTERFACES ));
	    states.put(TAG_LICENSE, Integer.valueOf(STATE_LICENSE));
	    states.put(TAG_METHODS, Integer.valueOf(STATE_METHODS ));
	    states.put(TAG_MIX, Integer.valueOf(STATE_MIX ));
	    states.put(TAG_MIXES, Integer.valueOf(STATE_MIXES ));
	    states.put(TAG_MIXIN, Integer.valueOf(STATE_MIXIN));
		states.put(TAG_MIXINS, Integer.valueOf(STATE_MIXINS ));
		states.put(TAG_NAMESPACE, Integer.valueOf(STATE_NAMESPACE ));
		states.put(TAG_NAMESPACES, Integer.valueOf(STATE_NAMESPACES ));
	    states.put(TAG_OPTION, Integer.valueOf(STATE_OPTION ));
		states.put(TAG_OPTIONS, Integer.valueOf(STATE_OPTIONS ));
	    states.put(TAG_PARAMETER, Integer.valueOf(STATE_PARAMETER ));
	    states.put(TAG_PARAMETERS, Integer.valueOf(STATE_PARAMETERS ));
		states.put(TAG_QOUTE, Integer.valueOf(STATE_QOUTE ));
	    states.put(TAG_REMARKS, Integer.valueOf(STATE_REMARKS ));
	    states.put(TAG_RETURNS, Integer.valueOf(STATE_RETURNS ));
		states.put(TAG_SEEALSO, Integer.valueOf(STATE_SEEALSO ));
		states.put(TAG_TITLE, Integer.valueOf(STATE_TITLE));
		states.put(TAG_TOPIC, Integer.valueOf(STATE_TOPIC));
		states.put(TAG_TOPICS, Integer.valueOf(STATE_TOPICS ));
		states.put(TAG_USERAGENT, Integer.valueOf(STATE_USERAGENT ));
		states.put(TAG_USERAGENTS, Integer.valueOf(STATE_USERAGENTS ));

		states.put(TAG_INCLUDE, Integer.valueOf(STATE_INCLUDE ));
	
	}
	
	
	public static LibraryAPIs readAPIsFromStream(InputSource inputSource, String path)   {
		
		 MetadataReader handler= new MetadataReader();
		handler.filePath=path;
		handler.loadMessageBundle();
		parseMetadata(inputSource, handler);
		return handler.apis;
	}

	private static void parseMetadata(InputSource inputSource,
			 MetadataReader handler) {
		try {
		    final SAXParserFactory factory= SAXParserFactory.newInstance();
			final SAXParser parser= factory.newSAXParser();
//			parser.setProperty("http://xml.org/sax/features/namespaces", new Boolean(true));
			XMLReader reader=parser.getXMLReader();
			reader.setFeature("http://xml.org/sax/features/namespaces", true);
			parser.parse(inputSource, handler);
		} catch (SAXException e) {
			Util.log(e, "error reading oaametadata");
		} catch (IOException e) {
			Util.log(e, "error reading oaametadata");
		} catch (ParserConfigurationException e) {
			Util.log(e, "error reading oaametadata");
		}
	}
	
	public static LibraryAPIs readAPIsFromString(String metadata, String path) {
		return readAPIsFromStream(new InputSource(new StringReader(metadata)),path);
	}
	
	public static LibraryAPIs readAPIsFromFile(String fileName) {
		try {
			FileInputStream file = new FileInputStream(fileName);
			LibraryAPIs apis= readAPIsFromStream(new InputSource(file),fileName);
			apis.fileName=fileName.toCharArray();
			return apis;
		} catch (FileNotFoundException e) {
			Util.log(e,  "error reading oaametadata");
		}
		return null;
	}
	
	
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (collectText)
		{
			if (collectHTML && pendingEndElement)
			{
				text.append("/>");
				pendingEndElement=false;
			}
			text.append(ch,start,length);
		}
	}

	public void endElement(String uri, String localName, String name)
			throws SAXException {
		
		if (collectText)
		{
			if (NAMESPACE_API.equals(uri)&& localName.equals(collectTextElement))
			{
				switch (this.currentState)
				{
				case STATE_DESCRIPTION:
				{
					if (this.currentObject instanceof DocumentedElement)
					{
						DocumentedElement documentedElement=((DocumentedElement)this.currentObject);
						documentedElement.description=localizedString(this.text.toString());
						documentedElement.isHTMLDescription=this.collectHTML;
					}
					break;
				}
				case STATE_DEPRECIATED:
				case STATE_AVAILABLE:
				{
					if (this.currentObject instanceof DepreciatedOrAvailable)
					{
						((DepreciatedOrAvailable)this.currentObject).text=localizedString(this.text.toString());
					}
					break;
				}
				
				case STATE_ABOUTME:
				case STATE_QOUTE:
				{
					if (this.currentObject instanceof Author)
					{
						String text=localizedString(this.text.toString());
						Author author=(Author)this.currentObject;
						if (this.currentState==STATE_ABOUTME)
							author.aboutMe=text;
						else
							author.quote=text;
					}
					break;
				}
				}
				popState();
				this.collectText=false;
				this.collectHTML=false;
				this.collectTextElement=null;
				this.text=new StringBuffer();
			}
			else
			{
			  if (pendingEndElement)
				  text.append("/>");
			  else
				  text.append("</").append(localName).append(">");
			}
			pendingEndElement=false;
		}
		else
		{
			
			switch (this.currentState)
			{
			case STATE_API:
			{
				ArrayList collection = getCollection(TAG_CLASS);
				this.apis.classes= (ClassData[])collection.toArray(new ClassData[collection.size()]);
				 collection = getCollection(TAG_METHOD);
				this.apis.globalMethods= (Method[])collection.toArray(new Method[collection.size()]);
				 collection = getCollection(TAG_PROPERTY);
				this.apis.globalVars= (Property[])collection.toArray(new Property[collection.size()]);
				 collection = getCollection(TAG_AUTHOR);
				this.apis.authors= (Author[])collection.toArray(new Author[collection.size()]);
				 collection = getCollection(TAG_ENUM);
				this.apis.enums= (Enum[])collection.toArray(new Enum[collection.size()]); 
				collection = getCollection(TAG_ALIAS);
				this.apis.aliases= (Alias[])collection.toArray(new Alias[collection.size()]);
				collection = getCollection(TAG_MIXIN);
				this.apis.mixins= (Mixin[])collection.toArray(new Mixin[collection.size()]);
				collection = getCollection(TAG_NAMESPACE);
				this.apis.namespaces= (Namespace[])collection.toArray(new Namespace[collection.size()]);
				break;
			}
			case STATE_CLASS:
			case STATE_INTERFACE:
			{
				ClassData clazz=(ClassData)this.currentObject;
				ArrayList collection = getCollection(TAG_ANCESTOR);
				clazz.ancestors= (Ancestor[])collection.toArray(new Ancestor[collection.size()]);
				 collection = getCollection(TAG_CONSTRUCTOR);
				 clazz.constructors= (Method[])collection.toArray(new Method[collection.size()]);
				 collection = getCollection(TAG_EVENT);
				 clazz.events= (Event[])collection.toArray(new Event[collection.size()]);
				 collection = getCollection(TAG_PROPERTY);
				clazz.properties= (Property[])collection.toArray(new Property[collection.size()]);
				collection = getCollection(TAG_FIELD);
				clazz.fields= (Property[])collection.toArray(new Property[collection.size()]);
				collection = getCollection(TAG_METHOD);
				clazz.methods= (Method[])collection.toArray(new Method[collection.size()]);
				 collection = getCollection(TAG_AUTHOR);
					clazz.mixins= (Mix[])collection.toArray(new Mix[collection.size()]);
				 collection = getCollection(TAG_ALIAS);
					clazz.aliases= (Alias[])collection.toArray(new Alias[collection.size()]);
				break;
			}
			
			case STATE_CONSTRUCTOR:
			case STATE_METHOD:
			{
				Method method=(Method)this.currentObject;
				ArrayList collection = getCollection(TAG_EXCEPTION);
				method.exceptions= (Exception[])collection.toArray(new Exception[collection.size()]);
				 collection = getCollection(TAG_PARAMETER);
				 method.parameters= (Parameter[])collection.toArray(new Parameter[collection.size()]);

				break;
			}

			case STATE_ENUM:
			{
				Enum enumData=(Enum)this.currentObject;
				ArrayList collection = getCollection(TAG_OPTION);
				enumData.options= (Option[])collection.toArray(new Option[collection.size()]);
				break;
			}
			
			case STATE_EVENT:
			{
				Event event=(Event)this.currentObject;
				ArrayList collection = getCollection(TAG_PARAMETER);
				event.parameters= (Parameter[])collection.toArray(new Parameter[collection.size()]);
				break;
			}
			
			case STATE_EXCEPTION:
			{
				Exception exception=(Exception)this.currentObject;
				ArrayList collection = getCollection(TAG_PARAMETER);
				exception.parameters= (Parameter[])collection.toArray(new Parameter[collection.size()]);
				break;
			}

			}
			popState();
		}
	}

	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {
		if (collectText)
		{
			if (collectHTML) {
				text.append("<").append(localName);
				int nAttributes = attributes.getLength();
				for (int i = 0; i < nAttributes; i++) {
					String qname = attributes.getQName(i);
					String value = attributes.getValue(i);
					text.append(" ").append(qname).append("=\"").append(value)
							.append("\"");
				}
			}
			pendingEndElement=true;
		}
		else
		{
			Integer stateObj=null;
			pushState();
			if (NAMESPACE_API.equals(uri))
			{
				stateObj=(Integer)states.get(localName);
				if (stateObj!=null)
				{
					int state=stateObj.intValue();
					switch (state)
					{
					
					case STATE_ABOUTME:
					{
						startCollectingText(localName,attributes);
						break;
					}

					case STATE_ALIAS:
					{
						Alias alias=new Alias();
						this.currentObject=alias;
						addCollectionElement(TAG_ALIAS, alias);
						alias.name = attributes.getValue(ATTRIBUTE_ALAIS_NAME);
						alias.datatype = attributes.getValue(ATTRIBUTE_ALAIS_TYPE);
						
						break;
					}

					case STATE_ANCESTOR:
					{
						Ancestor ancestor=new Ancestor();
						this.currentObject=ancestor;
						addCollectionElement(TAG_ANCESTOR, ancestor);
						ancestor.dataType = attributes.getValue(ATTRIBUTE_ANCESTOR_DATATYPE);
						
						break;
					}

					case STATE_API:
					{
						this.apis.libraryVersion = attributes.getValue(ATTRIBUTE_API_VERSION);
						this.apis.language = attributes.getValue(ATTRIBUTE_API_LANGUAGE);
						this.apis.getterPattern = attributes.getValue(ATTRIBUTE_API_GETTERPATTERN);
						this.apis.setterPattern = attributes.getValue(ATTRIBUTE_API_SETTERPATTERN);
						this.apis.setterPattern = attributes.getValue(ATTRIBUTE_API_SETTERPATTERN);
						this.apis.spec = attributes.getValue(ATTRIBUTE_API_SPEC);
						this.collections=new HashMap();
						break;
					}

					case STATE_AUTHOR:
					{
						Author author=new Author();
						this.currentObject=author;
						
						author.email = attributes.getValue(ATTRIBUTE_AUTHOR_EMAIL);
						author.location = attributes.getValue(ATTRIBUTE_AUTHOR_LOCATION);
						author.name = attributes.getValue(ATTRIBUTE_AUTHOR_NAME);
						author.organization = attributes.getValue(ATTRIBUTE_AUTHOR_ORGANIZATION);
						author.photo = attributes.getValue(ATTRIBUTE_AUTHOR_PHOTO);
						author.type = attributes.getValue(ATTRIBUTE_AUTHOR_TYPE);
					 	author.website = attributes.getValue(ATTRIBUTE_AUTHOR_WEBSITE);

					 	addCollectionElement(TAG_AUTHOR, author);
 					break;
					}

					case STATE_AVAILABLE:
					{
						DepreciatedOrAvailable available=new DepreciatedOrAvailable();
						if (this.currentObject instanceof VersionableElement)
							((VersionableElement)this.currentObject).available=available;
						available.version = attributes.getValue(ATTRIBUTE_AVAILABLE_VERSION);
						startCollectingText(localName,attributes);
						break;
					}

					
					
					case STATE_CLASS:
					case STATE_INTERFACE:
					{
						ClassData clazz=new ClassData();
						this.currentObject=clazz;
						if (STATE_INTERFACE==state)
						{
							clazz.isInterface=true;
							addCollectionElement(TAG_INTERFACE, clazz);
						}
						else
							addCollectionElement(TAG_CLASS, clazz);
						clazz.name = attributes.getValue(ATTRIBUTE_CLASS_NAME);
						clazz.superclass = attributes.getValue(ATTRIBUTE_CLASS_SUPERCLASS);
						clazz.visibility = attributes.getValue(ATTRIBUTE_CLASS_VISIBILITY);
						clazz.getterPattern = attributes.getValue(ATTRIBUTE_CLASS_GETTERPATTERN);
						clazz.setterPattern = attributes.getValue(ATTRIBUTE_CLASS_SETTERPATTERN);
						
						this.collections=new HashMap();
						break;
					}

					case STATE_CONSTRUCTOR:
					case STATE_METHOD:
					{
						Method method=new Method();
						if (STATE_CONSTRUCTOR==state)
						{
							method.isContructor=true;
							addCollectionElement(TAG_CONSTRUCTOR, method);
						}
						else
							addCollectionElement(TAG_METHOD, method);
						this.currentObject=method;
						method.scope = attributes.getValue(ATTRIBUTE_CONSTRUCTOR_SCOPE);
						method.visibility = attributes.getValue(ATTRIBUTE_CONSTRUCTOR_VISIBILITY);
						method.name = attributes.getValue(ATTRIBUTE_METHOD_NAME);
						
						this.collections=new HashMap();
						break;
					}


					case STATE_DEPRECIATED:
					{
						DepreciatedOrAvailable depreciated=new DepreciatedOrAvailable();
						if (this.currentObject instanceof VersionableElement)
							((VersionableElement)this.currentObject).depreciated=depreciated;
						depreciated.isDepreciated=true;
						depreciated.version = attributes.getValue(ATTRIBUTE_DEPRECIATED_VERSION);
						startCollectingText(localName,attributes);
						break;
					}

					case STATE_DESCRIPTION:
					{
						startCollectingText(localName,attributes);
						break;
					}

					case STATE_ENUM:
					{
						Enum enumData =new Enum();
						this.currentObject=enumData;
						addCollectionElement(TAG_ENUM, enumData);
						enumData.name = attributes.getValue(ATTRIBUTE_ENUM_NAME);
						enumData.datatype = attributes.getValue(ATTRIBUTE_ENUM_DATATYPE);

						break;
					}

					case STATE_EVENT:
					{
						Event event=new Event();
						this.currentObject=event;
						addCollectionElement(TAG_EVENT, event);
						
						this.collections=new HashMap();
						break;
					}
					
					case STATE_EXCEPTION:
					{
						Exception exception=new Exception();
						this.currentObject=exception;
						addCollectionElement(TAG_EXCEPTION, exception);
						
						this.collections=new HashMap();
						break;
					}
					
					
					case STATE_FIELD:
					case STATE_PROPERTY:
					{
						Property property=new Property();
						this.currentObject=property;
						if (STATE_FIELD==state)
						{
							property.isField=true;
							addCollectionElement(TAG_FIELD, property);
						}
						else
							addCollectionElement(TAG_PROPERTY, property);
						property.name = attributes.getValue(ATTRIBUTE_FIELD_NAME);
						property.dataType = attributes.getValue(ATTRIBUTE_FIELD_DATATYPE);
						property.scope = attributes.getValue(ATTRIBUTE_FIELD_SCOPE);
						property.visibility = attributes.getValue(ATTRIBUTE_FIELD_VISIBILITY);
						
						this.collections=new HashMap();
						break;
					}

					
					case STATE_INCLUDE:
					{
						String src= attributes.getValue(ATTRIBUTE_INCLUDE_SRC);
						handleInclude(src);
						break;
					}

					
					case STATE_MIX:
					{
						Mix mix=new Mix();
						addCollectionElement(TAG_MIX, mix);
						this.currentObject=mix; 
						mix.datatype = attributes.getValue(ATTRIBUTE_MIX_DATATYPE);
						mix.fromScope = attributes.getValue(ATTRIBUTE_MIX_FROMSCOPE);
						mix.toScope = attributes.getValue(ATTRIBUTE_MIX_TOSCOPE);

						break;
						
					}

					case STATE_MIXIN:
					{
						Mixin mixin=new Mixin();
						addCollectionElement(TAG_MIXIN, mixin);
						this.currentObject=mixin; 
						mixin.name = attributes.getValue(ATTRIBUTE_MIXIN_NAME);
						mixin.scope = attributes.getValue(ATTRIBUTE_MIXIN_SCOPE);
						mixin.visibility = attributes.getValue(ATTRIBUTE_MIXIN_VISIBILITY);

						break;
						
					}

					case STATE_NAMESPACE:
					{
						Namespace namespace=new Namespace();
						addCollectionElement(TAG_NAMESPACE, namespace);
						this.currentObject=namespace; 
						namespace.name = attributes.getValue(ATTRIBUTE_NAMESPACE_NAME);
						namespace.visibility = attributes.getValue(ATTRIBUTE_NAMESPACE_VISIBILITY);

						break;
						
					}

					case STATE_PARAMETER:
					{
						Parameter parameter=new Parameter();
						this.currentObject=parameter;
						addCollectionElement(TAG_PARAMETER, parameter);
						parameter.name = attributes.getValue(ATTRIBUTE_PARAMETER_NAME);
						parameter.dataType = attributes.getValue(ATTRIBUTE_PARAMETER_DATATYPE);
						parameter.usage = attributes.getValue(ATTRIBUTE_PARAMETER_USAGE);
						
						this.collections=new HashMap();
						break;
					}
					
					case STATE_QOUTE:
					{
						startCollectingText(localName,attributes);
						break;
					}

					
					case STATE_RETURNS:
					{
						ReturnsData returnData =new ReturnsData();
						if (this.currentObject instanceof Method)
							((Method)this.currentObject).returns=returnData;
						else if (this.currentObject instanceof Event)
							((Event)this.currentObject).returns=returnData;
						else if (this.currentObject instanceof Exception)
							((Exception)this.currentObject).returns=returnData;
						this.currentObject=returnData;
						returnData.dataType = attributes.getValue(ATTRIBUTE_RETURNS_DATATYPE);
						break;
					}


					}
					this.currentState=state;
				}
			}
		}
	}

	private void startCollectingText(String localName,Attributes attributes) {
		this.collectText=true;
		this.collectTextElement=localName;
		this.collectHTML=MIME_TYPE_HTML.equals(attributes.getValue(ATTRIBUTE_DESCRIPTION_TYPE));
		this.text=new StringBuffer();
	}

	private void addCollectionElement(String tagClass,Object element) {
		if (this.collections==null)
			this.collections=new HashMap();
		ArrayList list = (ArrayList)this.collections.get(tagClass);
		if (list==null)
		{
			this.collections.put(tagClass, list=new ArrayList());
		}
		list.add(element);
	}

	
	private ArrayList getCollection(String tagClass) {
		ArrayList list = null;
		if (this.collections != null) {
			list = (ArrayList)this.collections.get(tagClass);
		}
		if (list==null)
			list=EMPTY_LIST;
		return list;
	}

	private void popState() {
		StackElement stackElement=(StackElement)stack.pop();
		this.currentState=stackElement.state;
		this.collections=stackElement.collections;
		this.currentObject=stackElement.currentObject;
	}
	

	private void pushState() {
		StackElement newElement=new StackElement(this.currentState,this.currentObject, this.collections);
		stack.push(newElement);
	}


	private String localizedString(String string)
	{
		if (!this.doTranslation)
			return string;
		int subLength=VARIABLE_SUBSTITUTION_STRING.length();
	    int index=string.indexOf(VARIABLE_SUBSTITUTION_STRING);
	    if (index<0)
	    	return string;
	    StringBuffer text=new StringBuffer();
	    int start=0;
	    while (index>=0)
	    {
	    	text.append(string.substring(start, index));
	    	start=index+subLength;
		    index=string.indexOf(VARIABLE_SUBSTITUTION_STRING,start);
		    if (index<0)
		    {
		    	text.append(string.substring(start-subLength));
		    	break;
		    }
		    String key=string.substring(start,index);
		    String value=(String)this.messages.get(key);
		    String replace=(value!=null)? 
		    		value :
		    		VARIABLE_SUBSTITUTION_STRING+key+VARIABLE_SUBSTITUTION_STRING;
		    text.append(replace);
	    	start=index+subLength;
		    index=string.indexOf(VARIABLE_SUBSTITUTION_STRING,start);
	    }
	    return text.toString();
	    
	}
	
	private void handleInclude(String src)
	{
		IPath basePath = new Path(this.filePath);
		Path srcPath= new Path (src);
		basePath=basePath.removeLastSegments(1);
		basePath=basePath.append(srcPath);
		
		String fileName = basePath.toOSString();
		String savePath=this.filePath;
		try {
			FileInputStream file = new FileInputStream(fileName);
			this.filePath=fileName;
			parseMetadata(	new InputSource(file),this);
		} catch (FileNotFoundException e) {
			Util.log(e,  "error reading oaametadata");
		}
		this.filePath=savePath;
		
	}
	
	private void loadMessageBundle()
	{

		class MessageBundleHandler extends DefaultHandler 
		{
			String currentID;
			StringBuffer text;
			public void startElement(String uri, String localName, String name,
					Attributes attributes) throws SAXException {
				if (TAG_MSG.equals(localName))
				{
					currentID = attributes.getValue(ATTRIBUTE_MSG_NAME);
                    if (currentID!=null && currentID.length()>0)
                    	text=new StringBuffer();
                    else
                    	currentID=null;
				}
			}			
			public void endElement(String uri, String localName, String name)
			throws SAXException {
				if (currentID!=null)
				{
					messages.put(currentID, text.toString());
				}
				currentID=null;
				text=null;
			}
			public void characters(char[] ch, int start, int length)
			throws SAXException {
				if (currentID!=null && text!=null)
				{
					text.append(ch,start,length);
				}
			}
		}
		
		
		final String[] variants = buildVariants();
		for (int i = 0; i < variants.length; i++) {
			// loader==null if we're launched off the Java boot classpath
			File file = new File(variants[i]);
			if (!file.exists())
				continue;
			
			try {
				InputSource inputSource = new InputSource(new FileReader(file));
			} catch (FileNotFoundException e) {
				Util.log(e,  "error reading oaametadata");
			}
		}

		
	}

	private static final String EXTENSION = ".xml"; //$NON-NLS-1$
	private static final String ALL = "ALL"; //$NON-NLS-1$
	private static String[] nlSuffixes;

	private static String[] buildVariants() {
		if (nlSuffixes == null) {
			//build list of suffixes for loading resource bundles
			String nl = Locale.getDefault().toString();
			ArrayList result = new ArrayList(4);
			int lastSeparator;
			String defaultStr="";
			while (true) {
				result.add( nl +defaultStr+ EXTENSION);
				lastSeparator = nl.lastIndexOf('_');
				if (lastSeparator == -1)
					break;
				defaultStr="_"+ALL;
				nl = nl.substring(0, lastSeparator);
			}
			//add the empty suffix last (most general)
			result.add(ALL+"_"+ALL+EXTENSION);
			nlSuffixes = (String[]) result.toArray(new String[result.size()]);
		}
		String[] variants = new String[nlSuffixes.length];
		for (int i = 0; i < variants.length; i++)
			variants[i] = nlSuffixes[i];
		return variants;
	}
}
