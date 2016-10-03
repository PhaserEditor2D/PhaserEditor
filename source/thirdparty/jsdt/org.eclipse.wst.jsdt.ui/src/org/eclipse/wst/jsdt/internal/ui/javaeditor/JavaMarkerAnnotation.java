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


import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.eclipse.wst.jsdt.core.CorrectionEngine;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelMarker;
import org.eclipse.wst.jsdt.core.JavaScriptCore;



public class JavaMarkerAnnotation extends MarkerAnnotation implements IJavaAnnotation {

	public static final String JAVA_MARKER_TYPE_PREFIX= "org.eclipse.wst.jsdt"; //$NON-NLS-1$
	public static final String ERROR_ANNOTATION_TYPE= "org.eclipse.wst.jsdt.ui.error"; //$NON-NLS-1$
	public static final String WARNING_ANNOTATION_TYPE= "org.eclipse.wst.jsdt.ui.warning"; //$NON-NLS-1$
	public static final String INFO_ANNOTATION_TYPE= "org.eclipse.wst.jsdt.ui.info"; //$NON-NLS-1$
	public static final String TASK_ANNOTATION_TYPE= "org.eclipse.ui.workbench.texteditor.task"; //$NON-NLS-1$

	private IJavaAnnotation fOverlay;


	public JavaMarkerAnnotation(IMarker marker) {
		super(marker);
	}

	/*
	 * @see IJavaAnnotation#getArguments()
	 */
	public String[] getArguments() {
		IMarker marker= getMarker();
		if (marker != null && marker.exists() && isProblem())
			return CorrectionEngine.getProblemArguments(marker);
		return null;
	}

	/*
	 * @see IJavaAnnotation#getId()
	 */
	public int getId() {
		IMarker marker= getMarker();
		if (marker == null  || !marker.exists())
			return -1;

		if (isProblem())
			return marker.getAttribute(IJavaScriptModelMarker.ID, -1);

//		if (TASK_ANNOTATION_TYPE.equals(getAnnotationType())) {
//			try {
//				if (marker.isSubtypeOf(IJavaScriptModelMarker.TASK_MARKER)) {
//					return IProblem.Task;
//				}
//			} catch (CoreException e) {
//				JavaScriptPlugin.log(e); // should no happen, we test for marker.exists
//			}
//		}

		return -1;
	}

	/*
	 * @see IJavaAnnotation#isProblem()
	 */
	public boolean isProblem() {
		String type= getType();
		return WARNING_ANNOTATION_TYPE.equals(type) || ERROR_ANNOTATION_TYPE.equals(type);
	}

	/**
	 * Overlays this annotation with the given javaAnnotation.
	 *
	 * @param javaAnnotation annotation that is overlaid by this annotation
	 */
	public void setOverlay(IJavaAnnotation javaAnnotation) {
		if (fOverlay != null)
			fOverlay.removeOverlaid(this);

		fOverlay= javaAnnotation;
		if (!isMarkedDeleted())
			markDeleted(fOverlay != null);

		if (fOverlay != null)
			fOverlay.addOverlaid(this);
	}

	/*
	 * @see IJavaAnnotation#hasOverlay()
	 */
	public boolean hasOverlay() {
		return fOverlay != null;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.IJavaAnnotation#getOverlay()
	 */
	public IJavaAnnotation getOverlay() {
		return fOverlay;
	}

	/*
	 * @see IJavaAnnotation#addOverlaid(IJavaAnnotation)
	 */
	public void addOverlaid(IJavaAnnotation annotation) {
		// not supported
	}

	/*
	 * @see IJavaAnnotation#removeOverlaid(IJavaAnnotation)
	 */
	public void removeOverlaid(IJavaAnnotation annotation) {
		// not supported
	}

	/*
	 * @see IJavaAnnotation#getOverlaidIterator()
	 */
	public Iterator getOverlaidIterator() {
		// not supported
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.IJavaAnnotation#getCompilationUnit()
	 */
	public IJavaScriptUnit getCompilationUnit() {
		IJavaScriptElement element= JavaScriptCore.create(getMarker().getResource());
		if (element instanceof IJavaScriptUnit) {
			return (IJavaScriptUnit)element;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.IJavaAnnotation#getMarkerType()
	 */
	public String getMarkerType() {
		IMarker marker= getMarker();
		if (marker == null  || !marker.exists())
			return null;
		
		return  MarkerUtilities.getMarkerType(getMarker());
	}
}
