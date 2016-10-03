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

package org.eclipse.wst.jsdt.internal.ui.text.java.hover;

import java.util.Iterator;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.internal.ui.text.html.HTMLPrinter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaAnnotationIterator;


/**
 * Abstract super class for annotation hovers.
 *
 * 
 */
public abstract class AbstractAnnotationHover extends AbstractJavaEditorTextHover {

	private IPreferenceStore fStore= JavaScriptPlugin.getDefault().getCombinedPreferenceStore();
	private DefaultMarkerAnnotationAccess fAnnotationAccess= new DefaultMarkerAnnotationAccess();
	private boolean fAllAnnotations;


	public AbstractAnnotationHover(boolean allAnnotations) {
		fAllAnnotations= allAnnotations;
	}

	/*
	 * Formats a message as HTML text.
	 */
	private String formatMessage(String message) {
		StringBuffer buffer= new StringBuffer();
		HTMLPrinter.insertPageProlog(buffer, 0, getStyleSheet());
		buffer.append(HTMLPrinter.convertToHTMLContent(message));
		HTMLPrinter.addPageEpilog(buffer);
		return buffer.toString();
	}

	/*
	 * @see ITextHover#getHoverInfo(ITextViewer, IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		IPath path;
		IAnnotationModel model;
		if (textViewer instanceof ISourceViewer) {
			path= null;
			model= ((ISourceViewer)textViewer).getAnnotationModel();
		} else {
			// Get annotation model from file buffer manager
			path= getEditorInputPath();
			model= getAnnotationModel(path);
		}
		if (model == null)
			return null;

		try {
			Iterator e= new JavaAnnotationIterator(model, true, fAllAnnotations);
			int layer= -1;
			String message= null;
			while (e.hasNext()) {
				Annotation a= (Annotation) e.next();

				AnnotationPreference preference= getAnnotationPreference(a);
				if (preference == null || !(preference.getTextPreferenceKey() != null && fStore.getBoolean(preference.getTextPreferenceKey()) || (preference.getHighlightPreferenceKey() != null && fStore.getBoolean(preference.getHighlightPreferenceKey()))))
					continue;

				Position p= model.getPosition(a);

				int l= fAnnotationAccess.getLayer(a);

				if (l > layer && p != null && p.overlapsWith(hoverRegion.getOffset(), hoverRegion.getLength())) {
					String msg= a.getText();
					if (msg != null && msg.trim().length() > 0) {
						message= msg;
						layer= l;
					}
				}
			}
			if (layer > -1)
				return formatMessage(message);

		} finally {
			try {
				if (path != null) {
					ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
					manager.disconnect(path, LocationKind.NORMALIZE, null);
				}
			} catch (CoreException ex) {
				JavaScriptPlugin.log(ex.getStatus());
			}
		}

		return null;
	}

	private IPath getEditorInputPath() {
		if (getEditor() == null)
			return null;

		IEditorInput input= getEditor().getEditorInput();
		if (input instanceof IStorageEditorInput) {
			try {
				return ((IStorageEditorInput)input).getStorage().getFullPath();
			} catch (CoreException ex) {
				JavaScriptPlugin.log(ex.getStatus());
			}
		}
		return null;
	}

	private IAnnotationModel getAnnotationModel(IPath path) {
		if (path == null)
			return null;

		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(path, LocationKind.NORMALIZE, null);
		} catch (CoreException ex) {
			JavaScriptPlugin.log(ex.getStatus());
			return null;
		}

		IAnnotationModel model= null;
		try {
			model= manager.getTextFileBuffer(path, LocationKind.NORMALIZE).getAnnotationModel();
			return model;
		} finally {
			if (model == null) {
				try {
					manager.disconnect(path, LocationKind.NORMALIZE, null);
				} catch (CoreException ex) {
					JavaScriptPlugin.log(ex.getStatus());
				}
			}
		}
	}

	/**
	 * Returns the annotation preference for the given annotation.
	 *
	 * @param annotation the annotation
	 * @return the annotation preference or <code>null</code> if none
	 */
	private AnnotationPreference getAnnotationPreference(Annotation annotation) {

		if (annotation.isMarkedDeleted())
			return null;
		return EditorsUI.getAnnotationPreferenceLookup().getAnnotationPreference(annotation);
	}
}
