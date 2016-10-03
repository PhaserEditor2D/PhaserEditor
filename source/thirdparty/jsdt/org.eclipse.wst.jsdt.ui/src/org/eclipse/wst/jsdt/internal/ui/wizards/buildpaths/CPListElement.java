/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.core.IAccessRule;
import org.eclipse.wst.jsdt.core.IIncludePathAttribute;
import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.launching.JavaRuntime;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

public class CPListElement {
	
	public static final String EXCLUSION= "exclusion"; //$NON-NLS-1$
	public static final String INCLUSION= "inclusion"; //$NON-NLS-1$
	
	public static final String ACCESSRULES= "accessrules"; //$NON-NLS-1$
	public static final String COMBINE_ACCESSRULES= "combineaccessrules"; //$NON-NLS-1$

	public static final String JAVADOC= IIncludePathAttribute.JSDOC_LOCATION_ATTRIBUTE_NAME;
	
	private IJavaScriptProject fProject;
	
	private int fEntryKind;
	private IPath fPath, fOrginalPath;
	private IResource fResource;
	private boolean fIsExported;
	private boolean fIsMissing;
	
	private Object fParentContainer;
		
	private IIncludePathEntry fCachedEntry;
	private ArrayList fChildren;
	private IPath fLinkTarget, fOrginalLinkTarget;
	
	private CPListElement() {}
	
	public CPListElement(IJavaScriptProject project, int entryKind, IPath path, IResource res) {
		this(null, project, entryKind, path, res);
	}
	
	public CPListElement(Object parent, IJavaScriptProject project, int entryKind, IPath path, IResource res) {
		this(parent, project, entryKind, path, res, null);
	}
	
	public CPListElement(IJavaScriptProject project, int entryKind) {
		this(null, project, entryKind, null, null);
	}
	
	public CPListElement(Object parent, IJavaScriptProject project, int entryKind, IPath path, IResource res, IPath linkTarget) {
		fProject= project;

		fEntryKind= entryKind;
		fPath= path;
		fOrginalPath= path;
		fLinkTarget= linkTarget;
		fOrginalLinkTarget= linkTarget;
		fChildren= new ArrayList();
		fResource= res;
		fIsExported= false;
		
		fIsMissing= false;
		fCachedEntry= null;
		fParentContainer= parent;
		
		JsGlobalScopeContainerInitializer init = getContainerInitializer();
		
		boolean allowJsDoc = true;
		
		if(init!=null) {
			allowJsDoc = init.allowAttachJsDoc();
		}
		
		switch (entryKind) {
			case IIncludePathEntry.CPE_SOURCE:
				createAttributeElement(INCLUSION, new Path[0], true);
				createAttributeElement(EXCLUSION, new Path[0], true);
				break;
			case IIncludePathEntry.CPE_LIBRARY:
			case IIncludePathEntry.CPE_VARIABLE:
				if(allowJsDoc) createAttributeElement(JAVADOC, null, false);
				createAttributeElement(ACCESSRULES, new IAccessRule[0], true);
				break;
			case IIncludePathEntry.CPE_PROJECT:
				createAttributeElement(ACCESSRULES, new IAccessRule[0], true);
				createAttributeElement(COMBINE_ACCESSRULES, Boolean.FALSE, true); // not rendered
				break;
			case IIncludePathEntry.CPE_CONTAINER:
				createAttributeElement(ACCESSRULES, new IAccessRule[0], true);
				try {
					IJsGlobalScopeContainer container= JavaScriptCore.getJsGlobalScopeContainer(fPath, fProject);
					if (container != null) {
						IIncludePathEntry[] entries= container.getIncludepathEntries();
						if (entries != null) { // invalid container implementation
							for (int i= 0; i < entries.length; i++) {
								IIncludePathEntry entry= entries[i];
								if (entry != null) {
									if(init!=null) {
										String displayText = init.getDescription(entry.getPath(), project);
										if(displayText==null) continue;
									}
									
									CPListElement curr= createFromExisting(this, entry, fProject);
									fChildren.add(curr);
								} else {
									JavaScriptPlugin.logErrorMessage("Null entry in container '" + fPath + "'");  //$NON-NLS-1$//$NON-NLS-2$
								}
							}
						} else {
							JavaScriptPlugin.logErrorMessage("container returns null as entries: '" + fPath + "'");  //$NON-NLS-1$//$NON-NLS-2$
						}
					}
				} catch (JavaScriptModelException e) {
				}			
				break;
			default:
		}
		
	}
	
	public IIncludePathEntry getClasspathEntry() {
		if (fCachedEntry == null) {
			fCachedEntry= newClasspathEntry();
		}
		return fCachedEntry;
	}
	
	
	private IIncludePathAttribute[] getClasspathAttributes() {
		ArrayList res= new ArrayList();
		for (int i= 0; i < fChildren.size(); i++) {
			Object curr= fChildren.get(i);
			if (curr instanceof CPListElementAttribute) {
				CPListElementAttribute elem= (CPListElementAttribute) curr;
				if (!elem.isBuiltIn() && elem.getValue() != null) {
					res.add(elem.getClasspathAttribute());
				}
			}
		}
		return (IIncludePathAttribute[]) res.toArray(new IIncludePathAttribute[res.size()]);
	}
	

	private IIncludePathEntry newClasspathEntry() {

		IIncludePathAttribute[] extraAttributes= getClasspathAttributes();
		switch (fEntryKind) {
			case IIncludePathEntry.CPE_SOURCE:
				IPath[] inclusionPattern= (IPath[]) getAttribute(INCLUSION);
				IPath[] exclusionPattern= (IPath[]) getAttribute(EXCLUSION);
				return JavaScriptCore.newSourceEntry(fPath, inclusionPattern, exclusionPattern, null, extraAttributes);
			case IIncludePathEntry.CPE_LIBRARY: {
				IAccessRule[] accesRules= (IAccessRule[]) getAttribute(ACCESSRULES);
				return JavaScriptCore.newLibraryEntry(fPath, null, null, accesRules, extraAttributes, isExported());
			}
			case IIncludePathEntry.CPE_PROJECT: {
				IAccessRule[] accesRules= (IAccessRule[]) getAttribute(ACCESSRULES);
				boolean combineAccessRules= ((Boolean) getAttribute(COMBINE_ACCESSRULES)).booleanValue();
				return JavaScriptCore.newProjectEntry(fPath, accesRules, combineAccessRules, extraAttributes, isExported());
			}
			case IIncludePathEntry.CPE_CONTAINER: {
				IAccessRule[] accesRules= (IAccessRule[]) getAttribute(ACCESSRULES);
				return JavaScriptCore.newContainerEntry(fPath, accesRules, extraAttributes, isExported());
			}
			case IIncludePathEntry.CPE_VARIABLE: {
				IAccessRule[] accesRules= (IAccessRule[]) getAttribute(ACCESSRULES);
				return JavaScriptCore.newVariableEntry(fPath, null, null, accesRules, extraAttributes, isExported());
			}
			default:
				return null;
		}
	}
	
	/**
	 * Gets the class path entry path.
	 * @return returns the path
	 * @see IIncludePathEntry#getPath()
	 */
	public IPath getPath() {
		return fPath;
	}

	/**
	 * Gets the class path entry kind.
	 * @return the entry kind
	 * @see IIncludePathEntry#getEntryKind()
	 */	
	public int getEntryKind() {
		return fEntryKind;
	}

	/**
	 * Entries without resource are either non existing or a variable entry
	 * External jars do not have a resource
	 * @return returns the resource
	 */
	public IResource getResource() {
		return fResource;
	}
	
	public CPListElementAttribute setAttribute(String key, Object value) {
		CPListElementAttribute attribute= findAttributeElement(key);
		if (attribute == null) {
			return null;
			//createAttributeElement(key, value, false);
		}
		if (key.equals(EXCLUSION) || key.equals(INCLUSION)) {
			Assert.isTrue(value != null || fEntryKind != IIncludePathEntry.CPE_SOURCE);
		}
		
		if (key.equals(ACCESSRULES)) {
			Assert.isTrue(value != null || fEntryKind == IIncludePathEntry.CPE_SOURCE);
		}
		if (key.equals(COMBINE_ACCESSRULES)) {
			Assert.isTrue(value instanceof Boolean);
		}
		
		attribute.setValue(value);
		return attribute;
	}
	
	public boolean addToExclusions(IPath path) {
		String key= CPListElement.EXCLUSION;
		return addFilter(path, key);
	}
	
	public boolean addToInclusion(IPath path) {
		String key= CPListElement.INCLUSION;
		return addFilter(path, key);
	}
	
	public boolean removeFromExclusions(IPath path) {
		String key= CPListElement.EXCLUSION;
		return removeFilter(path, key);
	}
	
	public boolean removeFromInclusion(IPath path) {
		String key= CPListElement.INCLUSION;
		return removeFilter(path, key);
	}
	
	private boolean addFilter(IPath path, String key) {
		IPath[] filters= (IPath[]) getAttribute(key);
		if (filters == null)
			return false;
		
		if (!JavaModelUtil.isExcludedPath(path, filters)) {
			IPath toAdd= path.removeFirstSegments(getPath().segmentCount()).addTrailingSeparator();
			IPath[] newFilters= new IPath[filters.length + 1];
			System.arraycopy(filters, 0, newFilters, 0, filters.length);
			newFilters[filters.length]= toAdd;
			setAttribute(key, newFilters);
			return true;
		}
		return false;
	}
	
	private boolean removeFilter(IPath path, String key) {
		IPath[] filters= (IPath[]) getAttribute(key);
		if (filters == null)
			return false;
		
		IPath toRemove= path.removeFirstSegments(getPath().segmentCount()).addTrailingSeparator();
		if (JavaModelUtil.isExcludedPath(toRemove, filters)) {
			List l= new ArrayList(Arrays.asList(filters));
			l.remove(toRemove);
			IPath[] newFilters= (IPath[])l.toArray(new IPath[l.size()]);
			setAttribute(key, newFilters);
			return true;
		}
		return false;
	}
	
	public CPListElementAttribute findAttributeElement(String key) {
		for (int i= 0; i < fChildren.size(); i++) {
			Object curr= fChildren.get(i);
			if (curr instanceof CPListElementAttribute) {
				CPListElementAttribute elem= (CPListElementAttribute) curr;
				if (key.equals(elem.getKey())) {
					return elem;
				}
			}
		}		
		return null;		
	}
	
	
	public Object getAttribute(String key) {
		CPListElementAttribute attrib= findAttributeElement(key);
		if (attrib != null) {
			return attrib.getValue();
		}
		return null;
	}
	
	public CPListElementAttribute[] getAllAttributes() {
		ArrayList res= new ArrayList();
		for (int i= 0; i < fChildren.size(); i++) {
			Object curr= fChildren.get(i);
			if (curr instanceof CPListElementAttribute) {
				res.add(curr);
			}
		}		
		return (CPListElementAttribute[]) res.toArray(new CPListElementAttribute[res.size()]);
	}
	
	
	private void createAttributeElement(String key, Object value, boolean builtIn) {
		fChildren.add(new CPListElementAttribute(this, key, value, builtIn));
	}	
	
	private static boolean isFiltered(Object entry, String[] filteredKeys) {
		if (entry instanceof CPListElementAttribute) {
			CPListElementAttribute curr= (CPListElementAttribute) entry;
			String key= curr.getKey();
			for (int i= 0; i < filteredKeys.length; i++) {
				if (key.equals(filteredKeys[i])) {
					return true;
				}
			}
			if (curr.isNotSupported()) {
				return true;
			}
			if (!curr.isBuiltIn() && !key.equals(CPListElement.JAVADOC)) {
				return !JavaScriptPlugin.getDefault().getClasspathAttributeConfigurationDescriptors().containsKey(key);
			}
		}
		return false;
	}
	
	private Object[] getFilteredChildren(String[] filteredKeys) {
		int nChildren= fChildren.size();
		ArrayList res= new ArrayList(nChildren);
		
		for (int i= 0; i < nChildren; i++) {
			Object curr= fChildren.get(i);
			if (!isFiltered(curr, filteredKeys)) {
				res.add(curr);
			}
		}
		return res.toArray();
	}
		
	public Object[] getChildren() {
		if (fEntryKind == IIncludePathEntry.CPE_PROJECT) {
			return getFilteredChildren(new String[] { COMBINE_ACCESSRULES });
		}
		return getFilteredChildren(new String[0]);
	}
		
	public Object getParentContainer() {
		return fParentContainer;
	}	
	
	protected void attributeChanged(String key) {
		fCachedEntry= null;
	}
	/* return JsGlobalScopeContainerInitializer (if it exists)
	 * 
	 */
	public JsGlobalScopeContainerInitializer getContainerInitializer() {
		if (fEntryKind == IIncludePathEntry.CPE_CONTAINER && fProject != null) {
			JsGlobalScopeContainerInitializer initializer= JavaScriptCore.getJsGlobalScopeContainerInitializer(fPath.segment(0));
			return initializer ;
		}else if(fParentContainer !=null && fParentContainer instanceof CPListElement) {
			return ((CPListElement)fParentContainer).getContainerInitializer();
		}
		return null;
	}
	
	
	private IStatus evaluateContainerChildStatus(CPListElementAttribute attrib) {
		if (fProject != null) {
			JsGlobalScopeContainerInitializer initializer= JavaScriptCore.getJsGlobalScopeContainerInitializer(fPath.segment(0));
			if (initializer != null && initializer.canUpdateJsGlobalScopeContainer(fPath, fProject)) {
				if (attrib.isBuiltIn()) {
					if (CPListElement.ACCESSRULES.equals(attrib.getKey())) {
						return initializer.getAccessRulesStatus(fPath, fProject);
					}
				} else {
					return initializer.getAttributeStatus(fPath, fProject, attrib.getKey());
				}
			}
			return new Status(IStatus.ERROR, JavaScriptUI.ID_PLUGIN, JsGlobalScopeContainerInitializer.ATTRIBUTE_READ_ONLY, "", null); //$NON-NLS-1$
		}
		return null;
	}
	
	private boolean canUpdateContainer() {
		if (fEntryKind == IIncludePathEntry.CPE_CONTAINER && fProject != null) {
			//JsGlobalScopeContainerInitializer initializer= JavaScriptCore.getJsGlobalScopeContainerInitializer(fPath.segment(0));
			JsGlobalScopeContainerInitializer initializer=getContainerInitializer();
			return (initializer != null && initializer.canUpdateJsGlobalScopeContainer(fPath, fProject));
		}
		return false;
	}
	
	public boolean isInNonModifiableContainer() {
			if (fParentContainer!=null && fParentContainer instanceof CPListElement) {
				return !((CPListElement) fParentContainer).canUpdateContainer();
			}
			return fParentContainer==null && !canUpdateContainer();
		}
	
	public IStatus getContainerChildStatus(CPListElementAttribute attrib) {
		if (fParentContainer instanceof CPListElement) {
			CPListElement parent= (CPListElement) fParentContainer;
			if (parent.getEntryKind() == IIncludePathEntry.CPE_CONTAINER) {
				return parent.evaluateContainerChildStatus(attrib);
			}
			return ((CPListElement) fParentContainer).getContainerChildStatus(attrib);
		}
		return null;
	}
	
	public boolean isJRE() {
		IPath containerPath = getPath();
		IPath JREPath = new Path(JavaRuntime.JRE_CONTAINER);
		
		return (containerPath!=null && containerPath.equals(JREPath));
	}
	
	public boolean isInContainer(String containerName) {
		if (fParentContainer instanceof CPListElement) {
			CPListElement elem= (CPListElement) fParentContainer;
			return new Path(containerName).isPrefixOf(elem.getPath());
		}
		return false;
	}
	
	public boolean isDeprecated() {
		if (fEntryKind != IIncludePathEntry.CPE_VARIABLE) {
			return false;
		}
		if (fPath.segmentCount() > 0) {
			return JavaScriptCore.getIncludepathVariableDeprecationMessage(fPath.segment(0)) != null;
		}
		return false;
	}
	
	public String getDeprecationMessage() {
		if (fEntryKind != IIncludePathEntry.CPE_VARIABLE) {
			return null;
		}
		if (fPath.segmentCount() > 0) {
			String varName= fPath.segment(0);
			return BuildPathSupport.getDeprecationMessage(varName);
		}
		return null;
	}
	
	/*
	 * @see Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		if (other != null && other.getClass().equals(getClass())) {
			CPListElement elem= (CPListElement) other;
			return getClasspathEntry().equals(elem.getClasspathEntry());
		}
		return false;
	}
    	
	/*
	 * @see Object#hashCode()
	 */
	public int hashCode() {
		return fPath.hashCode() + fEntryKind;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getClasspathEntry().toString();
	}

	/**
	 * Returns if a entry is missing.
	 * @return Returns a boolean
	 */
	public boolean isMissing() {
		return fIsMissing;
	}

	/**
	 * Sets the 'missing' state of the entry.
	 * @param isMissing the new state
	 */
	public void setIsMissing(boolean isMissing) {
		fIsMissing= isMissing;
	}

	/**
	 * Returns if a entry is exported (only applies to libraries)
	 * @return Returns a boolean
	 */
	public boolean isExported() {
		return fIsExported;
	}

	/**
	 * Sets the export state of the entry.
	 * @param isExported the new state
	 */
	public void setExported(boolean isExported) {
		if (isExported != fIsExported) {
			fIsExported = isExported;
			
			attributeChanged(null);
		}
	}

	/**
	 * Gets the project.
	 * @return Returns a IJavaScriptProject
	 */
	public IJavaScriptProject getJavaProject() {
		return fProject;
	}
	
	public static CPListElement createFromExisting(IIncludePathEntry curr, IJavaScriptProject project) {
		return createFromExisting(null, curr, project);
	}
		
	public static CPListElement createFromExisting(Object parent, IIncludePathEntry curr, IJavaScriptProject project) {
		IPath path= curr.getPath();
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();

		// get the resource
		IResource res= null;
		boolean isMissing= false;
		IPath linkTarget= null;
		
		switch (curr.getEntryKind()) {
			case IIncludePathEntry.CPE_CONTAINER:
				try {
					isMissing= project != null && (JavaScriptCore.getJsGlobalScopeContainer(path, project) == null);
				} catch (JavaScriptModelException e) {
					isMissing= true;
				}
				break;
			case IIncludePathEntry.CPE_VARIABLE:
				IPath resolvedPath= JavaScriptCore.getResolvedVariablePath(path);
				isMissing=  root.findMember(resolvedPath) == null && !resolvedPath.toFile().isFile();
				break;
			case IIncludePathEntry.CPE_LIBRARY:
				res= root.findMember(path);
				if (res == null) {
					if (!ArchiveFileFilter.isArchivePath(path)) {
						if (root.getWorkspace().validatePath(path.toString(), IResource.FOLDER).isOK()
								&& root.getProject(path.segment(0)).exists()) {
							res= root.getFolder(path);
						}
					}
					isMissing= !path.toFile().isFile(); // look for external JARs
				} else if (res.isLinked()) {
					linkTarget= res.getLocation();
				}
				break;
			case IIncludePathEntry.CPE_SOURCE:
				path= path.removeTrailingSeparator();
				res= root.findMember(path);
				if (res == null) {
					if (root.getWorkspace().validatePath(path.toString(), IResource.FOLDER).isOK()) {
						res= root.getFolder(path);
					}
					isMissing= true;
				} else if (res.isLinked()) {
					linkTarget= res.getLocation();
				}
				break;
			case IIncludePathEntry.CPE_PROJECT:
				res= root.findMember(path);
				isMissing= (res == null);
				break;
		}
		CPListElement elem= new CPListElement(parent, project, curr.getEntryKind(), path, res, linkTarget);
		elem.setExported(curr.isExported());
		elem.setAttribute(EXCLUSION, curr.getExclusionPatterns());
		elem.setAttribute(INCLUSION, curr.getInclusionPatterns());
		elem.setAttribute(ACCESSRULES, curr.getAccessRules());
		elem.setAttribute(COMBINE_ACCESSRULES, new Boolean(curr.combineAccessRules())); 
		
		IIncludePathAttribute[] extraAttributes= curr.getExtraAttributes();
		for (int i= 0; i < extraAttributes.length; i++) {
			IIncludePathAttribute attrib= extraAttributes[i];
			CPListElementAttribute attribElem= elem.findAttributeElement(attrib.getName());
			if (attribElem == null) {
				elem.createAttributeElement(attrib.getName(), attrib.getValue(), false);
			} else {
				attribElem.setValue(attrib.getValue());
			}
		}
		
		if (project != null && project.exists()) {
			elem.setIsMissing(isMissing);
		}
		return elem;
	}

	public static StringBuffer appendEncodePath(IPath path, StringBuffer buf) {
		if (path != null) {
			String str= path.toString();
			buf.append('[').append(str.length()).append(']').append(str);
		} else {
			buf.append('[').append(']');
		}
		return buf;
	}
	
	public static StringBuffer appendEncodedString(String str, StringBuffer buf) {
		if (str != null) {
			buf.append('[').append(str.length()).append(']').append(str);
		} else {
			buf.append('[').append(']');
		}
		return buf;
	}
	
	public static StringBuffer appendEncodedFilter(IPath[] filters, StringBuffer buf) {
		if (filters != null) {
			buf.append('[').append(filters.length).append(']');
			for (int i= 0; i < filters.length; i++) {
				appendEncodePath(filters[i], buf).append(';');
			}
		} else {
			buf.append('[').append(']');
		}
		return buf;
	}
	
	public static StringBuffer appendEncodedAccessRules(IAccessRule[] rules, StringBuffer buf) {
		if (rules != null) {
			buf.append('[').append(rules.length).append(']');
			for (int i= 0; i < rules.length; i++) {
				appendEncodePath(rules[i].getPattern(), buf).append(';');
				buf.append(rules[i].getKind()).append(';');
			}
		} else {
			buf.append('[').append(']');
		}
		return buf;
	}
	

	public StringBuffer appendEncodedSettings(StringBuffer buf) {
		buf.append(fEntryKind).append(';');
		if (getLinkTarget() == null) {
			appendEncodePath(fPath, buf).append(';');
		} else {
			appendEncodePath(fPath, buf).append('-').append('>');
			appendEncodePath(getLinkTarget(), buf).append(';');
		}
		buf.append(Boolean.valueOf(fIsExported)).append(';');
		for (int i= 0; i < fChildren.size(); i++) {
			Object curr= fChildren.get(i);
			if (curr instanceof CPListElementAttribute) {
				CPListElementAttribute elem= (CPListElementAttribute) curr;
				if (elem.isBuiltIn()) {
					String key= elem.getKey();
					if (EXCLUSION.equals(key) || INCLUSION.equals(key)) {
						appendEncodedFilter((IPath[]) elem.getValue(), buf).append(';');
					} else if (ACCESSRULES.equals(key)) {
						appendEncodedAccessRules((IAccessRule[]) elem.getValue(), buf).append(';');
					} else if (COMBINE_ACCESSRULES.equals(key)) {
						buf.append(((Boolean) elem.getValue()).booleanValue()).append(';');	
					}
				} else {
					appendEncodedString((String) elem.getValue(), buf);
				}
			}
		}
		return buf;
	}

	public IPath getLinkTarget() {
		return fLinkTarget;
	}

	public void setPath(IPath path) {
		fCachedEntry= null;
		fPath= path;
	}
	
	public void setLinkTarget(IPath linkTarget) {
		fCachedEntry= null;
		fLinkTarget= linkTarget;
	}

	public static void insert(CPListElement element, List cpList) {
		int length= cpList.size();
		CPListElement[] elements= (CPListElement[])cpList.toArray(new CPListElement[length]);
		int i= 0;
		while (i < length && elements[i].getEntryKind() != element.getEntryKind()) {
			i++;
		}
		if (i < length) {
			i++;
			while (i < length && elements[i].getEntryKind() == element.getEntryKind()) {
				i++;
			}
			cpList.add(i, element);
			return;
		}
		
		switch (element.getEntryKind()) {
		case IIncludePathEntry.CPE_SOURCE:
			cpList.add(0, element);
			break;
		case IIncludePathEntry.CPE_CONTAINER:
		case IIncludePathEntry.CPE_LIBRARY:
		case IIncludePathEntry.CPE_PROJECT:
		case IIncludePathEntry.CPE_VARIABLE:
		default:
			cpList.add(element);
			break;
		}
	}

	public static IIncludePathEntry[] convertToClasspathEntries(List/*<CPListElement>*/ cpList) {
		IIncludePathEntry[] result= new IIncludePathEntry[cpList.size()];
		int i= 0;
		for (Iterator iter= cpList.iterator(); iter.hasNext();) {
			CPListElement cur= (CPListElement)iter.next();
			result[i]= cur.getClasspathEntry();
			i++;
		}
		return result;
	}
	
	public static CPListElement[] createFromExisting(IJavaScriptProject project) throws JavaScriptModelException {
		IIncludePathEntry[] rawClasspath= project.getRawIncludepath();
		CPListElement[] result= new CPListElement[rawClasspath.length];
		for (int i= 0; i < rawClasspath.length; i++) {
			result[i]= CPListElement.createFromExisting(rawClasspath[i], project);
		}
		return result;
	}
	
	public static boolean isProjectSourceFolder(CPListElement[] existing, IJavaScriptProject project) {
		IPath projPath= project.getProject().getFullPath();	
		for (int i= 0; i < existing.length; i++) {
			IIncludePathEntry curr= existing[i].getClasspathEntry();
			if (curr.getEntryKind() == IIncludePathEntry.CPE_SOURCE) {
				if (projPath.equals(curr.getPath())) {
					return true;
				}
			}
		}
		return false;
	}

	public IPath getOrginalPath() {
		return fOrginalPath;
	}

	public IPath getOrginalLinkTarget() {
		return fOrginalLinkTarget;
	}


    public CPListElement copy() {
    	CPListElement result= new CPListElement();
    	result.fProject= fProject;
    	result.fEntryKind= fEntryKind;
    	result.fPath= fPath;
    	result.fOrginalPath= fOrginalPath;
    	result.fResource= fResource;
    	result.fIsExported= fIsExported;
    	result.fIsMissing= fIsMissing;
    	result.fParentContainer= fParentContainer;
    	result.fCachedEntry= null;
    	result.fChildren= new ArrayList(fChildren.size());
    	for (Iterator iterator= fChildren.iterator(); iterator.hasNext();) {
    		Object child= iterator.next();
    		if (child instanceof CPListElement) {
    			result.fChildren.add(((CPListElement)child).copy());
    		} else {
	        	result.fChildren.add(((CPListElementAttribute)child).copy());
    		}
        }
    	result.fLinkTarget= fLinkTarget;
    	result.fOrginalLinkTarget= fOrginalLinkTarget;
	    return result;
    }
    
    public void setAttributesFromExisting(CPListElement existing) {
    	Assert.isTrue(existing.getEntryKind() == getEntryKind());
		CPListElementAttribute[] attributes= existing.getAllAttributes();
		for (int i= 0; i < attributes.length; i++) {
			CPListElementAttribute curr= attributes[i];
			CPListElementAttribute elem= findAttributeElement(curr.getKey());
			if (elem == null) {
				createAttributeElement(curr.getKey(), curr.getValue(), false);
			} else {
				elem.setValue(curr.getValue());
			}
		}
    }

}
