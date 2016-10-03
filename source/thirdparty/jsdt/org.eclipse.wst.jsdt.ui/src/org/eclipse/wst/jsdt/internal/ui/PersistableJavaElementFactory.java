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
package org.eclipse.wst.jsdt.internal.ui;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.JavaScriptCore;

/** 
 * The JavaElementFactory is used to save and recreate an IJavaScriptElement object.
 * As such, it implements the IPersistableElement interface for storage
 * and the IElementFactory interface for recreation.
 *
 * @see IMemento
 * @see IPersistableElement
 * @see IElementFactory
 */
public class PersistableJavaElementFactory implements IElementFactory, IPersistableElement {

	private static final String KEY= "elementID"; //$NON-NLS-1$
	private static final String FACTORY_ID= "org.eclipse.wst.jsdt.ui.PersistableJavaElementFactory"; //$NON-NLS-1$

	private IJavaScriptElement fElement;
	
	/**
	 * Create a JavaElementFactory.  
	 */
	public PersistableJavaElementFactory() {
	}

	/**
	 * Create a JavaElementFactory.  This constructor is typically used
	 * for our IPersistableElement side.
	 */
	public PersistableJavaElementFactory(IJavaScriptElement element) {
		fElement= element;
	}

	/*
	 * @see IElementFactory
	 */
	public IAdaptable createElement(IMemento memento) {
	
		String identifier= memento.getString(KEY);
		if (identifier != null) {
			return JavaScriptCore.create(identifier);
		}
		return null;
	}
	
	/*
	 * @see IPersistableElement.
	 */
	public String getFactoryId() {
		return FACTORY_ID;
	}
	/*
	 * @see IPersistableElement
	 */
	public void saveState(IMemento memento) {
		memento.putString(KEY, fElement.getHandleIdentifier());
	}
}
