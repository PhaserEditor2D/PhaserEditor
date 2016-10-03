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


import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.JavaScriptCore;


/**
 *
 */
public class ClassFileMarkerAnnotationModel extends AbstractMarkerAnnotationModel implements IResourceChangeListener {

	protected IClassFile fClassFile;
	protected IWorkspace fWorkspace;
	protected IResource fMarkerResource;
	protected boolean fChangesApplied;


	public ClassFileMarkerAnnotationModel(IResource markerResource) {
		super();
		fMarkerResource= markerResource;
		fWorkspace= fMarkerResource.getWorkspace();
	}

	public void setClassFile(IClassFile classFile) {
		fClassFile= classFile;
	}

	/**
	 * @see AbstractMarkerAnnotationModel#isAcceptable
	 */
	protected boolean isAcceptable(IMarker marker) {
		try {
			return JavaScriptCore.isReferencedBy(fClassFile, marker);
		} catch (CoreException x) {
			handleCoreException(x, JavaEditorMessages.ClassFileMarkerAnnotationModel_error_isAcceptable);
			return false;
		}
	}

	protected boolean isAffected(IMarkerDelta markerDelta) {
		try {
			return JavaScriptCore.isReferencedBy(fClassFile, markerDelta);
		} catch (CoreException x) {
			handleCoreException(x, JavaEditorMessages.ClassFileMarkerAnnotationModel_error_isAffected);
			return false;
		}
	}

	/**
	 * @see AbstractMarkerAnnotationModel#createMarkerAnnotation(IMarker)
	 */
	protected MarkerAnnotation createMarkerAnnotation(IMarker marker) {
		return new JavaMarkerAnnotation(marker);
	}

	/**
	 * @see AbstractMarkerAnnotationModel#listenToMarkerChanges(boolean)
	 */
	protected void listenToMarkerChanges(boolean listen) {
		if (listen)
			fWorkspace.addResourceChangeListener(this);
		else
			fWorkspace.removeResourceChangeListener(this);
	}

	/**
	 * @see AbstractMarkerAnnotationModel#deleteMarkers(IMarker[])
	 */
	protected void deleteMarkers(IMarker[] markers) throws CoreException {
		// empty as class files are read only
	}

	/**
	 * @see AbstractMarkerAnnotationModel#retrieveMarkers()
	 */
	protected IMarker[] retrieveMarkers() throws CoreException {
		if (fMarkerResource != null) {
			IMarker[] workspaceMarkers= fWorkspace.getRoot().findMarkers(IMarker.MARKER, true, IResource.DEPTH_ZERO);
			IMarker[] resourceMarkers= fMarkerResource.findMarkers(IMarker.MARKER, true, IResource.DEPTH_ZERO);
			int workspaceMarkersLength= workspaceMarkers.length;
			if (workspaceMarkersLength == 0)
				return resourceMarkers;
			
			int resourceMarkersLength= resourceMarkers.length;
			if (resourceMarkersLength == 0)
				return workspaceMarkers;
			
			IMarker[] result= new IMarker[resourceMarkersLength + workspaceMarkersLength];
			System.arraycopy(resourceMarkers, 0, result, 0, resourceMarkersLength);
			System.arraycopy(workspaceMarkers, 0, result, resourceMarkersLength, workspaceMarkersLength);
			return result;
		}
		return null;
	}

	private void checkDeltas(IMarkerDelta[] markerDeltas) throws CoreException {
		for (int i= 0; i < markerDeltas.length; i++) {
			if (isAffected(markerDeltas[i])) {
				IMarker marker= markerDeltas[i].getMarker();
				switch (markerDeltas[i].getKind()) {
					case IResourceDelta.ADDED :
						addMarkerAnnotation(marker);
						fChangesApplied= true;
						break;
					case IResourceDelta.REMOVED :
						removeMarkerAnnotation(marker);
						fChangesApplied= true;
						break;
					case IResourceDelta.CHANGED:
						modifyMarkerAnnotation(marker);
						fChangesApplied= true;
						break;
				}
			}
		}
	}

	/**
	 * @see IResourceChangeListener#resourceChanged
	 */
	public void resourceChanged(IResourceChangeEvent e) {
		try {
			IMarkerDelta[] deltas= e.findMarkerDeltas(null, true);
			if (deltas != null) {
				fChangesApplied= false;
				checkDeltas(deltas);
				if (fChangesApplied)
					fireModelChanged();
			}
		} catch (CoreException x) {
			handleCoreException(x, JavaEditorMessages.ClassFileMarkerAnnotationModel_error_resourceChanged);
		}
	}
}

