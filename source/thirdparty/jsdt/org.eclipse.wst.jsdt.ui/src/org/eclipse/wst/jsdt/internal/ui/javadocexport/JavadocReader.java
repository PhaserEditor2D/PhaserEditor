/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.javadocexport;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.Assert;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * Reads data from an InputStream and returns a JarPackage
 */
public class JavadocReader extends Object {

	private InputStream fInputStream;

	/**
	 * Reads a Javadoc Ant Script from the underlying stream.
	 * It is the client's responsibility to close the stream.
	 */
	public JavadocReader(InputStream inputStream) {
		Assert.isNotNull(inputStream);
		fInputStream= new BufferedInputStream(inputStream);
	}

	/**
	 * Closes this stream.
	 * It is the clients responsibility to close the stream.
	 * 
	 * @exception IOException
	 */
	public void close() throws IOException {
		if (fInputStream != null)
			fInputStream.close();
	}

	public Element readXML() throws IOException, SAXException {

		DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		DocumentBuilder parser= null;
		try {
			parser= factory.newDocumentBuilder();
		} catch (ParserConfigurationException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			// Note: Above code is OK since clients are responsible to close the stream
		}

		//find the project associated with the ant script
		Element xmlJavadocDesc= parser.parse(new InputSource(fInputStream)).getDocumentElement();

		NodeList targets= xmlJavadocDesc.getChildNodes();

		for (int i= 0; i < targets.getLength(); i++) {
			Node target= targets.item(i);

			//look through the XML file for the javadoc task
			if (target.getNodeName().equals("target")) { //$NON-NLS-1$
				NodeList children= target.getChildNodes();
				for (int j= 0; j < children.getLength(); j++) {
					Node child= children.item(j);
					if (child.getNodeName().equals("javadoc") && (child.getNodeType() == Node.ELEMENT_NODE)) { //$NON-NLS-1$
						return (Element) child;
					}
				}
			}

		}
		return null;
	}

}
