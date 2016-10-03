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
package org.eclipse.wst.jsdt.internal.corext.javadoc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IIncludePathAttribute;
import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IImportDeclaration;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.internal.corext.CorextMessages;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaUIException;
import org.eclipse.wst.jsdt.internal.ui.JavaUIStatus;
import org.eclipse.wst.jsdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathSupport;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class JavaDocLocations {
	
	public static final String ARCHIVE_PREFIX= "jar:"; //$NON-NLS-1$
	private static final String PREF_JAVADOCLOCATIONS= "org.eclipse.wst.jsdt.ui.javadoclocations"; //$NON-NLS-1$
	public static final String PREF_JAVADOCLOCATIONS_MIGRATED= "org.eclipse.wst.jsdt.ui.javadoclocations.migrated"; //$NON-NLS-1$

	
	private static final String NODE_ROOT= "javadoclocation"; //$NON-NLS-1$
	private static final String NODE_ENTRY= "location_01"; //$NON-NLS-1$
	private static final String NODE_PATH= "path"; //$NON-NLS-1$
	private static final String NODE_URL= "url"; //$NON-NLS-1$
	
	private static final QualifiedName PROJECT_JAVADOC= new QualifiedName(JavaScriptUI.ID_PLUGIN, "project_javadoc_location"); //$NON-NLS-1$
	
	public static void migrateToClasspathAttributes() {
		final Map oldLocations= loadOldForCompatibility();
		if (oldLocations.isEmpty()) {
			IPreferenceStore preferenceStore= PreferenceConstants.getPreferenceStore();
			preferenceStore.setValue(PREF_JAVADOCLOCATIONS, ""); //$NON-NLS-1$
			preferenceStore.setValue(PREF_JAVADOCLOCATIONS_MIGRATED, true);
			return;
		}
		
		Job job= new Job(CorextMessages.JavaDocLocations_migratejob_name) { 
			protected IStatus run(IProgressMonitor monitor) {
				try {
					IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
						public void run(IProgressMonitor pm) throws CoreException {
							updateClasspathEntries(oldLocations, pm);
							IPreferenceStore preferenceStore= PreferenceConstants.getPreferenceStore();
							preferenceStore.setValue(PREF_JAVADOCLOCATIONS, ""); //$NON-NLS-1$
							preferenceStore.setValue(PREF_JAVADOCLOCATIONS_MIGRATED, true);
						}
					};
					new WorkbenchRunnableAdapter(runnable).run(monitor);
				} catch (InvocationTargetException e) {
					JavaScriptPlugin.log(e);
				} catch (InterruptedException e) {
					// should not happen, cannot cancel
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
	
	final static void updateClasspathEntries(Map oldLocationMap, IProgressMonitor monitor) throws JavaScriptModelException {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IJavaScriptProject[] javaProjects= JavaScriptCore.create(root).getJavaScriptProjects();
		try {
			monitor.beginTask(CorextMessages.JavaDocLocations_migrate_operation, javaProjects.length); 
			for (int i= 0; i < javaProjects.length; i++) {
				IJavaScriptProject project= javaProjects[i];
				String projectJavadoc= (String) oldLocationMap.get(project.getPath());
				if (projectJavadoc != null) {
					try {
						setProjectJavadocLocation(project, projectJavadoc);
					} catch (CoreException e) {
						// ignore
					}
				}
				
				IIncludePathEntry[] rawClasspath= project.getRawIncludepath();
				boolean hasChange= false;
				for (int k= 0; k < rawClasspath.length; k++) {
					IIncludePathEntry updated= getConvertedEntry(rawClasspath[k], project, oldLocationMap);
					if (updated != null) {
						rawClasspath[k]= updated;
						hasChange= true;
					}
				}
				if (hasChange) {
					project.setRawIncludepath(rawClasspath, new SubProgressMonitor(monitor, 1));
				} else {
					monitor.worked(1);
				}
			}
		} finally {
			monitor.done();
		}
	}

	private static IIncludePathEntry getConvertedEntry(IIncludePathEntry entry, IJavaScriptProject project, Map oldLocationMap) {
		IPath path= null;
		switch (entry.getEntryKind()) {
			case IIncludePathEntry.CPE_SOURCE:
			case IIncludePathEntry.CPE_PROJECT:
				return null;
			case IIncludePathEntry.CPE_CONTAINER:
				convertContainer(entry, project, oldLocationMap);
				return null;
			case IIncludePathEntry.CPE_LIBRARY:
				path= entry.getPath();
				break;
			case IIncludePathEntry.CPE_VARIABLE:
				path= JavaScriptCore.getResolvedVariablePath(entry.getPath());
				break;
			default:
				return null;
		}
		if (path == null) {
			return null;
		}
		IIncludePathAttribute[] extraAttributes= entry.getExtraAttributes();
		for (int i= 0; i < extraAttributes.length; i++) {
			if (IIncludePathAttribute.JSDOC_LOCATION_ATTRIBUTE_NAME.equals(extraAttributes[i].getName())) {
				return null;
			}
		}
		String libraryJavadocLocation= (String) oldLocationMap.get(path);
		if (libraryJavadocLocation != null) {
			CPListElement element= CPListElement.createFromExisting(entry, project);
			element.setAttribute(CPListElement.JAVADOC, libraryJavadocLocation);
			return element.getClasspathEntry();
		}
		return null;
	}

	private static void convertContainer(IIncludePathEntry entry, IJavaScriptProject project, Map oldLocationMap) {
		try {
			IJsGlobalScopeContainer container= JavaScriptCore.getJsGlobalScopeContainer(entry.getPath(), project);
			if (container == null) {
				return;
			}
			
			IIncludePathEntry[] entries= container.getIncludepathEntries();
			boolean hasChange= false;
			for (int i= 0; i < entries.length; i++) {
				IIncludePathEntry curr= entries[i];
				IIncludePathEntry updatedEntry= getConvertedEntry(curr, project, oldLocationMap);
				if (updatedEntry != null) {
					entries[i]= updatedEntry;
					hasChange= true;
				}
			}
			if (hasChange) {
				BuildPathSupport.requestContainerUpdate(project, container, entries);
			}
		} catch (CoreException e) {
			// ignore
		}
	}

	/**
	 * Sets the Javadoc location for an archive with the given path.
	 */
	public static void setProjectJavadocLocation(IJavaScriptProject project, URL url) {
		try {
			String location= url != null ? url.toExternalForm() : null;
			setProjectJavadocLocation(project, location);
		} catch (CoreException e) {
			JavaScriptPlugin.log(e);
		}
	}
	
	private static void setProjectJavadocLocation(IJavaScriptProject project, String url) throws CoreException {
		project.getProject().setPersistentProperty(PROJECT_JAVADOC, url);
	}
	
	public static URL getProjectJavadocLocation(IJavaScriptProject project) {
		try {
			String prop= project.getProject().getPersistentProperty(PROJECT_JAVADOC);
			if (prop == null) {
				return null;
			}
			return new URL(prop);
		} catch (CoreException e) {
			JavaScriptPlugin.log(e);
		} catch (MalformedURLException e) {
			JavaScriptPlugin.log(e);
		}
		return null;
	}
	
	
	public static URL getLibraryJavadocLocation(IIncludePathEntry entry) {
		if (entry == null) {
			throw new IllegalArgumentException("Entry must not be null"); //$NON-NLS-1$
		}
		
		int kind= entry.getEntryKind();
		if (kind != IIncludePathEntry.CPE_LIBRARY && kind != IIncludePathEntry.CPE_VARIABLE) {
			throw new IllegalArgumentException("Entry must be of kind CPE_LIBRARY or CPE_VARIABLE"); //$NON-NLS-1$
		}
		
		IIncludePathAttribute[] extraAttributes= entry.getExtraAttributes();
		for (int i= 0; i < extraAttributes.length; i++) {
			IIncludePathAttribute attrib= extraAttributes[i];
			if (IIncludePathAttribute.JSDOC_LOCATION_ATTRIBUTE_NAME.equals(attrib.getName())) {
				try {
					return new URL(attrib.getValue());
				} catch (MalformedURLException e) {
					return null;
				}
			}
		}
		return null;
	}

	public static URL getJavadocBaseLocation(IJavaScriptElement element) throws JavaScriptModelException {	
		if (element.getElementType() == IJavaScriptElement.JAVASCRIPT_PROJECT) {
			return getProjectJavadocLocation((IJavaScriptProject) element);
		}
		
		IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(element);
		if (root == null) {
			return null;
		}

		if (root.getKind() == IPackageFragmentRoot.K_BINARY) {
			IIncludePathEntry entry= root.getRawIncludepathEntry();
			if (entry == null) {
				return null;
			}
			if (entry.getEntryKind() == IIncludePathEntry.CPE_CONTAINER) {
				entry= getRealClasspathEntry(root.getJavaScriptProject(), entry.getPath(), root.getPath());
				if (entry == null) {
					return null;
				}
			}
			return getLibraryJavadocLocation(entry);
		} else {
			return getProjectJavadocLocation(root.getJavaScriptProject());
		}	
	}
	
	private static IIncludePathEntry getRealClasspathEntry(IJavaScriptProject jproject, IPath containerPath, IPath libPath) throws JavaScriptModelException {
		IJsGlobalScopeContainer container= JavaScriptCore.getJsGlobalScopeContainer(containerPath, jproject);
		if (container != null) {
			IIncludePathEntry[] entries= container.getIncludepathEntries();
			for (int i= 0; i < entries.length; i++) {
				IIncludePathEntry curr= entries[i];
				IIncludePathEntry resolved= JavaScriptCore.getResolvedIncludepathEntry(curr);
				if (resolved != null && libPath.equals(resolved.getPath())) {
					return curr; // return the real entry
				}
			}
		}
		return null; // not found
	}
	
	
	// loading for compatibility
	
	private static JavaUIException createException(Throwable t, String message) {
		return new JavaUIException(JavaUIStatus.createError(IStatus.ERROR, message, t));
	}	
	
	private static Map/*<Path, String>*/ loadOldForCompatibility() {
		HashMap resultingOldLocations= new HashMap();
		
		// in 3.0, the javadoc locations were stored as one big string in the preferences
		String string= PreferenceConstants.getPreferenceStore().getString(PREF_JAVADOCLOCATIONS);
		if (string != null && string.length() > 0) {
			byte[] bytes;
			try {
				bytes= string.getBytes("UTF-8"); //$NON-NLS-1$
			} catch (UnsupportedEncodingException e) {
				bytes= string.getBytes();
			}
			InputStream is= new ByteArrayInputStream(bytes);
			try {
				loadFromStream(new InputSource(is), resultingOldLocations);
				PreferenceConstants.getPreferenceStore().setValue(PREF_JAVADOCLOCATIONS, ""); //$NON-NLS-1$
				return resultingOldLocations;
			} catch (CoreException e) {
				JavaScriptPlugin.log(e); // log but ignore
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}

		// in 2.1, the Javadoc locations were stored in a file in the meta data
		// note that it is wrong to use a stream reader with XML declaring to be UTF-8
		try {
			final String STORE_FILE= "javadoclocations.xml"; //$NON-NLS-1$
			File file= JavaScriptPlugin.getDefault().getStateLocation().append(STORE_FILE).toFile();
			if (file.exists()) {
				Reader reader= null;
				try {
					reader= new FileReader(file);
					loadFromStream(new InputSource(reader), resultingOldLocations);
					file.delete(); // remove file after successful store
					return resultingOldLocations;
				} catch (IOException e) {
					JavaScriptPlugin.log(e); // log but ignore
				} finally {
					try {
						if (reader != null) {
							reader.close();
						}
					} catch (IOException e) {}
				}
			}
		} catch (CoreException e) {
			JavaScriptPlugin.log(e); // log but ignore
		}	
		
		// in 2.0, the Javadoc locations were stored as one big string in the persistent properties
		// note that it is wrong to use a stream reader with XML declaring to be UTF-8
		try {
			final QualifiedName QUALIFIED_NAME= new QualifiedName(JavaScriptUI.ID_PLUGIN, "jdoclocation"); //$NON-NLS-1$
			
			IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
			String xmlString= root.getPersistentProperty(QUALIFIED_NAME); 
			if (xmlString != null) { // only set when workspace is old
				Reader reader= new StringReader(xmlString);
				try {
					loadFromStream(new InputSource(reader), resultingOldLocations);
					root.setPersistentProperty(QUALIFIED_NAME, null); // clear property
					return resultingOldLocations;
				} finally {

					try {
						reader.close();
					} catch (IOException e) {
						// error closing reader: ignore
					}
				}
			}
		} catch (CoreException e) {
			JavaScriptPlugin.log(e); // log but ignore
		}
		return resultingOldLocations;
	}
	
	private static void loadFromStream(InputSource inputSource, Map/*<Path, String>*/ oldLocations) throws CoreException {
		Element cpElement;
		try {
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			cpElement = parser.parse(inputSource).getDocumentElement();
		} catch (SAXException e) {
			throw createException(e, CorextMessages.JavaDocLocations_error_readXML); 
		} catch (ParserConfigurationException e) {
			throw createException(e, CorextMessages.JavaDocLocations_error_readXML); 
		} catch (IOException e) {
			throw createException(e, CorextMessages.JavaDocLocations_error_readXML); 
		}
		
		if (cpElement == null) return;
		if (!cpElement.getNodeName().equalsIgnoreCase(NODE_ROOT)) {
			return;
		}
		NodeList list= cpElement.getChildNodes();
		int length= list.getLength();
		for (int i= 0; i < length; ++i) {
			Node node= list.item(i);
			short type= node.getNodeType();
			if (type == Node.ELEMENT_NODE) {
				Element element= (Element) node;
				if (element.getNodeName().equalsIgnoreCase(NODE_ENTRY)) {
					String varPath = element.getAttribute(NODE_PATH);
					String varURL = element.getAttribute(NODE_URL);
					
					oldLocations.put(Path.fromPortableString(varPath), varURL);
				}
			}
		}
	}
		
	public static URL getJavadocLocation(IJavaScriptElement element, boolean includeMemberReference) throws JavaScriptModelException {
		URL baseLocation= getJavadocBaseLocation(element);
		if (baseLocation == null) {
			return null;
		}

		String urlString= baseLocation.toExternalForm();

		StringBuffer pathBuffer= new StringBuffer(urlString);
		if (!urlString.endsWith("/")) { //$NON-NLS-1$
			pathBuffer.append('/');
		}

		switch (element.getElementType()) {
			case IJavaScriptElement.PACKAGE_FRAGMENT:
				appendPackageSummaryPath((IPackageFragment) element, pathBuffer);
				break;
			case IJavaScriptElement.JAVASCRIPT_PROJECT:
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT :
				appendIndexPath(pathBuffer);
				break;
			case IJavaScriptElement.IMPORT_CONTAINER :
				element= element.getParent();
				//$FALL-THROUGH$
			case IJavaScriptElement.JAVASCRIPT_UNIT :
				IType mainType= ((IJavaScriptUnit) element).findPrimaryType();
				if (mainType == null) {
					return null;
				}
				appendTypePath(mainType, pathBuffer);
				break;
			case IJavaScriptElement.CLASS_FILE :
				appendTypePath(((IClassFile) element).getType(), pathBuffer);
				break;
			case IJavaScriptElement.TYPE :
				appendTypePath((IType) element, pathBuffer);
				break;
			case IJavaScriptElement.FIELD :
				IField field= (IField) element;
				appendTypePath(field.getDeclaringType(), pathBuffer);
				if (includeMemberReference) {
					appendFieldReference(field, pathBuffer);
				}
				break;
			case IJavaScriptElement.METHOD :
				IFunction method= (IFunction) element;
				appendTypePath(method.getDeclaringType(), pathBuffer);
				if (includeMemberReference) {
					appendMethodReference(method, pathBuffer);
				}
				break;
			case IJavaScriptElement.INITIALIZER :
				appendTypePath(((IMember) element).getDeclaringType(), pathBuffer);
				break;
			case IJavaScriptElement.IMPORT_DECLARATION :
				IImportDeclaration decl= (IImportDeclaration) element;

				if (decl.isOnDemand()) {
					IJavaScriptElement cont= JavaModelUtil.findTypeContainer(element.getJavaScriptProject(), Signature.getQualifier(decl.getElementName()));
					if (cont instanceof IType) {
						appendTypePath((IType) cont, pathBuffer);
					} else if (cont instanceof IPackageFragment) {
						appendPackageSummaryPath((IPackageFragment) cont, pathBuffer);
					}
				} else {
					IType imp= element.getJavaScriptProject().findType(decl.getElementName());
					appendTypePath(imp, pathBuffer);
				}
				break;
			default :
				return null;
		}

		try {
			return new URL(pathBuffer.toString());
		} catch (MalformedURLException e) {
			JavaScriptPlugin.log(e);
		}
		return null;
	}	
		
	private static void appendPackageSummaryPath(IPackageFragment pack, StringBuffer buf) {
		String packPath= pack.getElementName().replace('.', '/');
		buf.append(packPath);
		buf.append("/package-summary.html"); //$NON-NLS-1$
	}
	
	private static void appendIndexPath(StringBuffer buf) {
		buf.append("index.html"); //$NON-NLS-1$
	}	
	
	private static void appendTypePath(IType type, StringBuffer buf) {
		IPackageFragment pack= type.getPackageFragment();
		String packPath= pack.getElementName().replace('.', '/');
		String typePath= JavaModelUtil.getTypeQualifiedName(type);
		buf.append(packPath);
		buf.append('/');
		buf.append(typePath);
		buf.append(".html"); //$NON-NLS-1$
	}		
		
	private static void appendFieldReference(IField field, StringBuffer buf) {
		buf.append('#');
		buf.append(field.getElementName());
	}
	
	private static void appendMethodReference(IFunction meth, StringBuffer buf) throws JavaScriptModelException {
		buf.append('#');
		buf.append(meth.getElementName());	
		
		buf.append('(');
		String[] params= meth.getParameterTypes();
		IType declaringType= meth.getDeclaringType();
		boolean isVararg= Flags.isVarargs(meth.getFlags());
		int lastParam= params.length - 1;
		for (int i= 0; i <= lastParam; i++) {
			if (i != 0) {
				buf.append(", "); //$NON-NLS-1$
			}
			String curr= params[i];
			String fullName= JavaModelUtil.getResolvedTypeName(curr, declaringType);
			if (fullName != null) {
				buf.append(fullName);
				int dim= Signature.getArrayCount(curr);
				if (i == lastParam && isVararg) {
					dim--;
				}
				while (dim > 0) {
					buf.append("[]"); //$NON-NLS-1$
					dim--;
				}
				if (i == lastParam && isVararg) {
					buf.append("..."); //$NON-NLS-1$
				}
			}
		}
		buf.append(')');
	}


}
