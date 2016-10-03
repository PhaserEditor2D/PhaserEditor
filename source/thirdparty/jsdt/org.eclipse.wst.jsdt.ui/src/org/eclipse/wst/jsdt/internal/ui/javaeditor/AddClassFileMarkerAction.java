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

package org.eclipse.wst.jsdt.internal.ui.javaeditor;


import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.AddMarkerAction;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.IResourceLocator;



class AddClassFileMarkerAction extends AddMarkerAction {


	/**
	 * Creates a marker action.
	 */
	public AddClassFileMarkerAction(String prefix, ITextEditor textEditor, String markerType, boolean askForLabel) {
		super(JavaEditorMessages.getBundleForConstructedKeys(), prefix, textEditor, markerType, askForLabel);
	}

	/**
	 * @see AddMarkerAction#getResource()
	 */
	protected IResource getResource() {

		IResource resource= null;

		IEditorInput input= getTextEditor().getEditorInput();
		if (input instanceof IClassFileEditorInput) {
			IClassFile c= ((IClassFileEditorInput) input).getClassFile();
			IResourceLocator locator= (IResourceLocator) c.getAdapter(IResourceLocator.class);
			if (locator != null) {
				try {
					resource= locator.getContainingResource(c);
				} catch (JavaScriptModelException x) {
					// ignore but should inform
				}
			}
		}

		return resource;
	}

	/**
	 * @see AddMarkerAction#getInitialAttributes()
	 */
	protected Map getInitialAttributes() {

		Map attributes= super.getInitialAttributes();

		IEditorInput input= getTextEditor().getEditorInput();
		if (input instanceof IClassFileEditorInput) {

			IClassFile classFile= ((IClassFileEditorInput) input).getClassFile();
			JavaScriptCore.addJavaScriptElementMarkerAttributes(attributes, classFile);
		}

		return attributes;
	}
}
