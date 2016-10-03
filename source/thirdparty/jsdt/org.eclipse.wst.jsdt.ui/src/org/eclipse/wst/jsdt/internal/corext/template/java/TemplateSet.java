/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.template.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * <code>TemplateSet</code> manages a collection of templates and makes them
 * persistent.
 * 
 * @deprecated use TemplateStore instead
 * 
 */
public class TemplateSet {

	private static final String NAME_ATTRIBUTE= "name"; //$NON-NLS-1$
	private static final String DESCRIPTION_ATTRIBUTE= "description"; //$NON-NLS-1$
	private static final String CONTEXT_ATTRIBUTE= "context"; //$NON-NLS-1$

	private List fTemplates= new ArrayList();
	private String fTemplateTag;
	
	private static final int TEMPLATE_PARSE_EXCEPTION= 10002;
	private static final int TEMPLATE_IO_EXCEPTION= 10005;
	private ContextTypeRegistry fRegistry;
	
	public TemplateSet(String templateTag, ContextTypeRegistry registry) {
		fTemplateTag= templateTag;
		fRegistry= registry;
	}
	
	/**
	 * Convenience method for reading templates from a file.
	 * 
	 * @param file
	 * @param allowDuplicates
	 * @see #addFromStream(InputStream, boolean)
	 * @throws CoreException
	 */
	public void addFromFile(File file, boolean allowDuplicates) throws CoreException {
		InputStream stream= null;

		try {
			stream= new FileInputStream(file);
			addFromStream(stream, allowDuplicates);

		} catch (IOException e) {
			throwReadException(e);

		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {
				// just exit
			}
		}		
	}
	
	public String getTemplateTag() {
		return fTemplateTag;
	}
	

	/**
	 * Reads templates from a XML stream and adds them to the templates
	 * 
	 * @param stream
	 * @param allowDuplicates
	 * @throws CoreException
	 */	
	public void addFromStream(InputStream stream, boolean allowDuplicates) throws CoreException {
		try {
			DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
			DocumentBuilder parser= factory.newDocumentBuilder();		
			Document document= parser.parse(new InputSource(stream));
			
			NodeList elements= document.getElementsByTagName(getTemplateTag());
			
			int count= elements.getLength();
			for (int i= 0; i != count; i++) {
				Node node= elements.item(i);					
				NamedNodeMap attributes= node.getAttributes();

				if (attributes == null)
					continue;

				String name= getAttributeValue(attributes, NAME_ATTRIBUTE);
				String description= getAttributeValue(attributes, DESCRIPTION_ATTRIBUTE);
				if (name == null || description == null)
					continue;
				
				String context= getAttributeValue(attributes, CONTEXT_ATTRIBUTE);

				if (name == null || description == null || context == null)
					throw new SAXException(JavaTemplateMessages.TemplateSet_error_missing_attribute); 

				StringBuffer buffer= new StringBuffer();
				NodeList children= node.getChildNodes();
				for (int j= 0; j != children.getLength(); j++) {
					String value= children.item(j).getNodeValue();
					if (value != null)
						buffer.append(value);
				}
				String pattern= buffer.toString().trim();

				Template template= new Template(name, description, context, pattern);
				
				String message= validateTemplate(template);
				if (message == null) {
					if (!allowDuplicates) {
						Template[] templates= getTemplates(name);
						for (int k= 0; k < templates.length; k++) {
							remove(templates[k]);
						}
					}
					add(template);					
				} else {
					throwReadException(null);
				}
			}
		} catch (ParserConfigurationException e) {
			throwReadException(e);
		} catch (IOException e) {
			throwReadException(e);
		} catch (SAXException e) {
			throwReadException(e);
		}
	}
	
	protected String validateTemplate(Template template) {
		TemplateContextType type= fRegistry.getContextType(template.getContextTypeId());
		if (type == null) {
			return "Unknown context type: " + template.getContextTypeId(); //$NON-NLS-1$
		}
		try {
			type.validate(template.getPattern());
			return null;
		} catch (TemplateException e) {
			return e.getMessage();
		}
	}
	
	private String getAttributeValue(NamedNodeMap attributes, String name) {
		Node node= attributes.getNamedItem(name);

		return node == null
			? null
			: node.getNodeValue();
	}

	/**
	 * Convenience method for saving to a file.
	 * 
	 * @param file the file
	 * @throws CoreException in case the save operation fails
	 * @see #saveToStream(OutputStream)
	 */
	public void saveToFile(File file) throws CoreException {
		OutputStream stream= null;

		try {
			stream= new FileOutputStream(file);
			saveToStream(stream);

		} catch (IOException e) {
			throwWriteException(e);

		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {
				// just exit
			}
		}
	}
		
	/**
	 * Saves the template set as XML.
	 * 
	 * @param stream the stream
	 * @throws CoreException in case the save operation fails
	 */
	public void saveToStream(OutputStream stream) throws CoreException {
		try {
			DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
			DocumentBuilder builder= factory.newDocumentBuilder();		
			Document document= builder.newDocument();

			Node root= document.createElement("templates"); //$NON-NLS-1$
			document.appendChild(root);
			
			for (int i= 0; i != fTemplates.size(); i++) {
				Template template= (Template) fTemplates.get(i);
				
				Node node= document.createElement(getTemplateTag());
				root.appendChild(node);
				
				NamedNodeMap attributes= node.getAttributes();
				
				Attr name= document.createAttribute(NAME_ATTRIBUTE);
				name.setValue(template.getName());
				attributes.setNamedItem(name);
	
				Attr description= document.createAttribute(DESCRIPTION_ATTRIBUTE);
				description.setValue(template.getDescription());
				attributes.setNamedItem(description);
	
				Attr context= document.createAttribute(CONTEXT_ATTRIBUTE);
				context.setValue(template.getContextTypeId());
				attributes.setNamedItem(context);			

				Text pattern= document.createTextNode(template.getPattern());
				node.appendChild(pattern);			
			}		
			
			
			Transformer transformer=TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(stream);

			transformer.transform(source, result);

		} catch (ParserConfigurationException e) {
			throwWriteException(e);
		} catch (TransformerException e) {
			throwWriteException(e);
		}		
	}

	private static void throwReadException(Throwable t) throws CoreException {
		int code;
		if (t instanceof SAXException)
			code= TEMPLATE_PARSE_EXCEPTION;
		else
			code= TEMPLATE_IO_EXCEPTION;
//		IStatus status= JavaUIStatus.createError(code, TemplateMessages.getString("TemplateSet.error.read"), t); //$NON-NLS-1$
//		throw new JavaUIException(status);
		throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.jface.text", code, JavaTemplateMessages.TemplateSet_error_read, t));  //$NON-NLS-1$
	}
	
	private static void throwWriteException(Throwable t) throws CoreException {
//		IStatus status= JavaUIStatus.createError(IJavaStatusConstants.TEMPLATE_IO_EXCEPTION,
//			TemplateMessages.getString("TemplateSet.error.write"), t); //$NON-NLS-1$
//		throw new JavaUIException(status);
		throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.jface.text", TEMPLATE_IO_EXCEPTION, JavaTemplateMessages.TemplateSet_error_write, t));  //$NON-NLS-1$
	}

	/**
	 * Adds a template to the set.
	 * 
	 * @param template the template to add to the set
	 */
	public void add(Template template) {
		if (exists(template))
			return; // ignore duplicate
		
		fTemplates.add(template);
	}

	private boolean exists(Template template) {
		for (Iterator iterator = fTemplates.iterator(); iterator.hasNext();) {
			Template anotherTemplate = (Template) iterator.next();

			if (template.equals(anotherTemplate))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Removes a template to the set.
	 * 
	 * @param template the template to remove from the set
	 */	
	public void remove(Template template) {
		fTemplates.remove(template);
	}

	/**
	 * Empties the set.
	 */		
	public void clear() {
		fTemplates.clear();
	}
	
	/**
	 * Returns all templates.
	 * 
	 * @return all templates
	 */
	public Template[] getTemplates() {
		return (Template[]) fTemplates.toArray(new Template[fTemplates.size()]);
	}
	
	/**
	 * Returns all templates with a given name.
	 * 
	 * @param name the template name
	 * @return the templates with the given name
	 */
	public Template[] getTemplates(String name) {
		ArrayList res= new ArrayList();
		for (Iterator iterator= fTemplates.iterator(); iterator.hasNext();) {
			Template curr= (Template) iterator.next();
			if (curr.getName().equals(name)) {
				res.add(curr);
			}
		}
		return (Template[]) res.toArray(new Template[res.size()]);
	}
	
	/**
	 * Returns the first templates with the given name.
	 * 
	 * @param name the template name
	 * @return the first template with the given name
	 */
	public Template getFirstTemplate(String name) {
		for (Iterator iterator= fTemplates.iterator(); iterator.hasNext();) {
			Template curr= (Template) iterator.next();
			if (curr.getName().equals(name)) {
				return curr;
			}
		}
		return null;
	}	
	
}

