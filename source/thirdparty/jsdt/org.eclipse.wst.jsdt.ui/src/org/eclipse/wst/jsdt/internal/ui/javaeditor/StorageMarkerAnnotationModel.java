/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.javaeditor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;
 

/**
 * Source editor resource marker annotation model implementation
 */
public class StorageMarkerAnnotationModel extends ResourceMarkerAnnotationModel {
	public final static String SECONDARY_ID_KEY = "org.eclipse.wst.sse.ui.extensions.breakpoint.path"; //$NON-NLS-1$
	protected IResource fMarkerResource;
	protected String fSecondaryMarkerAttributeValue;

	/**
	 * Constructor
	 * 
	 * @param resource
	 */
	public StorageMarkerAnnotationModel(IResource resource) {
		super(resource);
		fMarkerResource = resource;
	}

	public StorageMarkerAnnotationModel(IResource resource, String secondaryID) {
		super(resource);
		fMarkerResource = resource;
		fSecondaryMarkerAttributeValue = secondaryID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel#createMarkerAnnotation(org.eclipse.core.resources.IMarker)
	 */
//	protected MarkerAnnotation createMarkerAnnotation(IMarker marker) {
//		/*
//		 * We need to do some special processing if marker is a validation
//		 * (aka problem) marker or if marker is a breakpoint marker so create
//		 * a special marker annotation for those markers. Otherwise, use
//		 * default.
//		 */
//		if (MarkerUtilities.isMarkerType(marker, IMarker.PROBLEM)) {
//			return new StructuredMarkerAnnotation(marker);
//		}
//		return super.createMarkerAnnotation(marker);
//	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel#getMarkerPosition(org.eclipse.core.resources.IMarker)
	 */
	public Position getMarkerPosition(IMarker marker) {
		Position pos = super.getMarkerPosition(marker);

		// if ((pos == null || pos.getLength() == 0) && marker.getType() ==
		// IInternalDebugUIConstants.ANN_INSTR_POINTER_CURRENT) {
		if (pos == null || pos.getLength() == 0) {
			// We probably should create position from marker if marker
			// attributes specify a valid position
			pos = createPositionFromMarker(marker);
		}

		return pos;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel#isAcceptable(org.eclipse.core.resources.IMarker)
	 */
	protected boolean isAcceptable(IMarker marker) {
//		try {
//			Object attr = marker.getAttribute(IBreakpointConstants.ATTR_HIDDEN);
//			if (attr != null && ((Boolean) attr).equals(Boolean.TRUE))
//				return false;
//		}
//		catch (CoreException e) {
//			// ignore
//		}

		if (fSecondaryMarkerAttributeValue == null)
			return super.isAcceptable(marker);
		String markerSecondaryMarkerAttributeValue = marker.getAttribute(SECONDARY_ID_KEY, ""); //$NON-NLS-1$
		boolean isSameFile = fSecondaryMarkerAttributeValue.equalsIgnoreCase(markerSecondaryMarkerAttributeValue);
		return super.isAcceptable(marker) && isSameFile;
	}
}

