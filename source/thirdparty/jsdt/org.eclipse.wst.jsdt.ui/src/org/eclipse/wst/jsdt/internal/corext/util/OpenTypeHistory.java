/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 package org.eclipse.wst.jsdt.internal.corext.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.jsdt.core.ElementChangedEvent;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IElementChangedListener;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptElementDelta;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.TypeNameMatch;
import org.eclipse.wst.jsdt.internal.corext.CorextMessages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.w3c.dom.Element;

/**
 * History for the open type dialog. Object and keys are both {@link TypeNameMatch}s.
 */
public class OpenTypeHistory extends History {
	
	private static class TypeHistoryDeltaListener implements IElementChangedListener {
		public void elementChanged(ElementChangedEvent event) {
			if (processDelta(event.getDelta())) {
				OpenTypeHistory.getInstance().markAsInconsistent();
			}
		}
		
		/**
		 * Computes whether the history needs a consistency check or not.
		 * 
		 * @param delta the Java element delta
		 * 
		 * @return <code>true</code> if consistency must be checked 
		 *  <code>false</code> otherwise.
		 */
		private boolean processDelta(IJavaScriptElementDelta delta) {
			IJavaScriptElement elem= delta.getElement();
			
			boolean isChanged= delta.getKind() == IJavaScriptElementDelta.CHANGED;
			boolean isRemoved= delta.getKind() == IJavaScriptElementDelta.REMOVED;
						
			switch (elem.getElementType()) {
				case IJavaScriptElement.JAVASCRIPT_PROJECT:
					if (isRemoved || (isChanged && 
							(delta.getFlags() & IJavaScriptElementDelta.F_CLOSED) != 0)) {
						return true;
					}
					return processChildrenDelta(delta);
				case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
					if (isRemoved || (isChanged && (
							(delta.getFlags() & IJavaScriptElementDelta.F_ARCHIVE_CONTENT_CHANGED) != 0 ||
							(delta.getFlags() & IJavaScriptElementDelta.F_REMOVED_FROM_CLASSPATH) != 0))) {
						return true;
					}
					return processChildrenDelta(delta);
				case IJavaScriptElement.TYPE:
					if (isChanged && (delta.getFlags() & IJavaScriptElementDelta.F_MODIFIERS) != 0) {
						return true;
					}
					// type children can be inner classes: fall through
					//$FALL-THROUGH$
				case IJavaScriptElement.JAVASCRIPT_MODEL:
				case IJavaScriptElement.PACKAGE_FRAGMENT:
				case IJavaScriptElement.CLASS_FILE:
					if (isRemoved) {
						return true;
					}				
					return processChildrenDelta(delta);
				case IJavaScriptElement.JAVASCRIPT_UNIT:
					// Not the primary compilation unit. Ignore it 
					if (!JavaModelUtil.isPrimary((IJavaScriptUnit) elem)) {
						return false;
					}

					if (isRemoved || (isChanged && isUnknownStructuralChange(delta.getFlags()))) {
						return true;
					}
					return processChildrenDelta(delta);
				default:
					// fields, methods, imports ect
					return false;
			}	
		}
		
		private boolean isUnknownStructuralChange(int flags) {
			if ((flags & IJavaScriptElementDelta.F_CONTENT) == 0)
				return false;
			return (flags & IJavaScriptElementDelta.F_FINE_GRAINED) == 0; 
		}

		/*
		private boolean isPossibleStructuralChange(int flags) {
			return (flags & (IJavaScriptElementDelta.F_CONTENT | IJavaScriptElementDelta.F_FINE_GRAINED)) == IJavaScriptElementDelta.F_CONTENT;
		}
		*/		
		
		private boolean processChildrenDelta(IJavaScriptElementDelta delta) {
			IJavaScriptElementDelta[] children= delta.getAffectedChildren();
			for (int i= 0; i < children.length; i++) {
				if (processDelta(children[i])) {
					return true;
				}
			}
			return false;
		}
	}
	
	private static class UpdateJob extends Job {
		public static final String FAMILY= UpdateJob.class.getName();
		public UpdateJob() {
			super(CorextMessages.TypeInfoHistory_consistency_check);
		}
		protected IStatus run(IProgressMonitor monitor) {
			OpenTypeHistory history= OpenTypeHistory.getInstance();
			history.internalCheckConsistency(monitor);
			return new Status(IStatus.OK, JavaScriptPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
		}
		public boolean belongsTo(Object family) {
			return FAMILY.equals(family);
		}
	}
	
	// Needs to be volatile since accesses aren't synchronized.
	private volatile boolean fNeedsConsistencyCheck;
	// Map of cached time stamps
	private Map fTimestampMapping;
	
	private final IElementChangedListener fDeltaListener;
	private final UpdateJob fUpdateJob;
	
	private static final String FILENAME= "OpenTypeHistory.xml"; //$NON-NLS-1$
	private static final String NODE_ROOT= "typeInfoHistroy"; //$NON-NLS-1$
	private static final String NODE_TYPE_INFO= "typeInfo"; //$NON-NLS-1$
	private static final String NODE_HANDLE= "handle"; //$NON-NLS-1$
	private static final String NODE_MODIFIERS= "modifiers";  //$NON-NLS-1$
	private static final String NODE_TIMESTAMP= "timestamp"; //$NON-NLS-1$
	
	private static OpenTypeHistory fgInstance;
	
	public static synchronized OpenTypeHistory getInstance() {
		if (fgInstance == null)
			fgInstance= new OpenTypeHistory();
		return fgInstance;
	}
	
	public static synchronized void shutdown() {
		if (fgInstance == null)
			return;
		fgInstance.doShutdown();
	}
	
	private OpenTypeHistory() {
		super(FILENAME, NODE_ROOT, NODE_TYPE_INFO);
		fTimestampMapping= new HashMap();
		fNeedsConsistencyCheck= true;
		load();
		fDeltaListener= new TypeHistoryDeltaListener();
		JavaScriptCore.addElementChangedListener(fDeltaListener);
		fUpdateJob= new UpdateJob();
		// It is not necessary anymore that the update job has a rule since
		// markAsInconsistent isn't synchronized anymore. See bugs
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=128399 and
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=135278
		// for details.
		fUpdateJob.setPriority(Job.SHORT);
	}
	
	public void markAsInconsistent() {
		fNeedsConsistencyCheck= true;
		// cancel the old job. If no job is running this is a NOOP.
		fUpdateJob.cancel();
		fUpdateJob.schedule();
	}
	
	public boolean needConsistencyCheck() {
		return fNeedsConsistencyCheck;
	}

	public void checkConsistency(IProgressMonitor monitor) throws OperationCanceledException {
		if (!fNeedsConsistencyCheck)
			return;
		if (fUpdateJob.getState() == Job.RUNNING) {
			try {
				Job.getJobManager().join(UpdateJob.FAMILY, monitor);
			} catch (OperationCanceledException e) {
				// Ignore and do the consistency check without
				// waiting for the update job.
			} catch (InterruptedException e) {
				// Ignore and do the consistency check without
				// waiting for the update job.
			}
		}
		if (!fNeedsConsistencyCheck)
			return;
		internalCheckConsistency(monitor);
	}
	
	public synchronized boolean contains(TypeNameMatch type) {
		return super.contains(type);
	}

	public synchronized void accessed(TypeNameMatch info) {
		// Fetching the timestamp might not be cheap (remote file system
		// external Jars. So check if we alreay have one.
		if (!fTimestampMapping.containsKey(info)) {
			fTimestampMapping.put(info, Long.valueOf(getContainerTimestamp(info)));
		}
		super.accessed(info);
	}

	public synchronized TypeNameMatch remove(TypeNameMatch info) {
		fTimestampMapping.remove(info);
		return (TypeNameMatch)super.remove(info);
	}
	
	public synchronized void replace(TypeNameMatch old, TypeNameMatch newMatch) {
		fTimestampMapping.remove(old);
		fTimestampMapping.put(newMatch, Long.valueOf(getContainerTimestamp(newMatch)));
		super.remove(old);
		super.accessed(newMatch);
	}

	public synchronized TypeNameMatch[] getTypeInfos() {
		Collection values= getValues();
		int size= values.size();
		TypeNameMatch[] result= new TypeNameMatch[size];
		int i= size - 1;
		for (Iterator iter= values.iterator(); iter.hasNext();) {
			result[i]= (TypeNameMatch)iter.next();
			i--;
		}
		return result;
	}

	public synchronized TypeNameMatch[] getFilteredTypeInfos(TypeInfoFilter filter) {
		Collection values= getValues();
		List result= new ArrayList();
		for (Iterator iter= values.iterator(); iter.hasNext();) {
			TypeNameMatch type= (TypeNameMatch)iter.next();
			if ((filter == null || filter.matchesHistoryElement(type)) && !TypeFilter.isFiltered(type.getFullyQualifiedName()))
				result.add(type);
		}
		Collections.reverse(result);
		return (TypeNameMatch[])result.toArray(new TypeNameMatch[result.size()]);
		
	}
	
	protected Object getKey(Object object) {
		return object;
	}

	private synchronized void internalCheckConsistency(IProgressMonitor monitor) throws OperationCanceledException {
		// Setting fNeedsConsistencyCheck is necessary here since 
		// markAsInconsistent isn't synchronized.
		fNeedsConsistencyCheck= true;
		List typesToCheck= new ArrayList(getKeys());
		monitor.beginTask(CorextMessages.TypeInfoHistory_consistency_check, typesToCheck.size());
		monitor.setTaskName(CorextMessages.TypeInfoHistory_consistency_check);
		for (Iterator iter= typesToCheck.iterator(); iter.hasNext();) {
			TypeNameMatch type= (TypeNameMatch)iter.next();
			long currentTimestamp= getContainerTimestamp(type);
			Long lastTested= (Long)fTimestampMapping.get(type);
			if (lastTested != null && currentTimestamp != IResource.NULL_STAMP && currentTimestamp == lastTested.longValue() && !isContainerDirty(type))
				continue;
			try {
				IType jType= type.getType();
				if (jType == null || !jType.exists()) {
					remove(type);
				} else {
					// copy over the modifiers since they may have changed
					int modifiers= jType.getFlags();
					if (modifiers != type.getModifiers()) {
						replace(type, SearchEngine.createTypeNameMatch(jType, modifiers));
					} else {
						fTimestampMapping.put(type, Long.valueOf(currentTimestamp));
					}
				}
			} catch (JavaScriptModelException e) {
				remove(type);
			}
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			monitor.worked(1);
		}
		monitor.done();
		fNeedsConsistencyCheck= false;
	}
	
	private long getContainerTimestamp(TypeNameMatch match) {
		try {
			IType type= match.getType();
			IResource resource= type.getResource();
			if (resource != null) {
				URI location= resource.getLocationURI();
				if (location != null) {
					IFileInfo info= EFS.getStore(location).fetchInfo();
					if (info.exists()) {
						// The element could be removed from the build path. So check
						// if the Java element still exists.
						IJavaScriptElement element= JavaScriptCore.create(resource);
						if (element != null && element.exists())
							return info.getLastModified();
					}
				}
			} else { // external JAR
				IPackageFragmentRoot root= match.getPackageFragmentRoot();
				if (root.exists()) {
					IFileInfo info= EFS.getLocalFileSystem().getStore(root.getPath()).fetchInfo();
					if (info.exists()) {
						return info.getLastModified();
					}
				}
			}
		} catch (CoreException e) {
			// Fall through
		}
		return IResource.NULL_STAMP;
	}
	
	
	public boolean isContainerDirty(TypeNameMatch match) {
		IJavaScriptUnit cu= match.getType().getJavaScriptUnit();
		if (cu == null) {
			return false;
		}
		IResource resource= cu.getResource(); 
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		ITextFileBuffer textFileBuffer= manager.getTextFileBuffer(resource.getFullPath(), LocationKind.IFILE);
		if (textFileBuffer != null) {
			return textFileBuffer.isDirty();
		}
		return false;
	}
	
	
	private void doShutdown() {
		JavaScriptCore.removeElementChangedListener(fDeltaListener);
		save();
	}
	
	protected Object createFromElement(Element type) {
		String handle= type.getAttribute(NODE_HANDLE);
		if (handle == null )
			return null;
		
		IJavaScriptElement element= JavaScriptCore.create(handle);
		if (!(element instanceof IType))
			return null;
		
		int modifiers= 0;
		try {
			modifiers= Integer.parseInt(type.getAttribute(NODE_MODIFIERS));
		} catch (NumberFormatException e) {
			// take zero
		}
		TypeNameMatch info= SearchEngine.createTypeNameMatch((IType) element, modifiers);
		long timestamp= IResource.NULL_STAMP;
		String timestampValue= type.getAttribute(NODE_TIMESTAMP);
		if (timestampValue != null && timestampValue.length() > 0) {
			try {
				timestamp= Long.parseLong(timestampValue);
			} catch (NumberFormatException e) {
				// take null stamp
			}
		}
		if (timestamp != IResource.NULL_STAMP) {
			fTimestampMapping.put(info, Long.valueOf(timestamp));
		}
		return info;
	}

	protected void setAttributes(Object object, Element typeElement) {
		TypeNameMatch type= (TypeNameMatch) object;
		String handleId= type.getType().getHandleIdentifier();
		typeElement.setAttribute(NODE_HANDLE, handleId);
		typeElement.setAttribute(NODE_MODIFIERS, Integer.toString(type.getModifiers()));
		Long timestamp= (Long) fTimestampMapping.get(type);
		if (timestamp == null) {
			typeElement.setAttribute(NODE_TIMESTAMP, Long.toString(IResource.NULL_STAMP));			
		} else {
			typeElement.setAttribute(NODE_TIMESTAMP, timestamp.toString()); 
		}
	}

}
