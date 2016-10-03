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
package org.eclipse.wst.jsdt.internal.ui.javadocexport;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

public class RecentSettingsStore {
	
	private final String HREF= "href"; //$NON-NLS-1$
	private final String DESTINATION= "destdir"; //$NON-NLS-1$
	private final String ANTPATH= "antpath"; //$NON-NLS-1$
	
	private final String SECTION_PROJECTS= "projects"; //$NON-NLS-1$
	
	private final static char REF_SEPARATOR= ';';
	
	
	//list of hrefs in string format
	private Map fPerProjectSettings;
	
	/**
	 * 
	 */
	public RecentSettingsStore(IDialogSettings settings) {
		fPerProjectSettings= new HashMap();
		if (settings != null) {
			load(settings);
		}
	}
	
	/**
	* Method creates a list of data structes that contain
	* The destination, antfile location and the list of library/project references for every
	* project in the workspace.Defaults are created for new project.
	*/
	private void load(IDialogSettings settings) {

		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		
		IDialogSettings projectsSection= settings.getSection(SECTION_PROJECTS); 
		if (projectsSection != null) {
			IDialogSettings[] sections= projectsSection.getSections();
			for (int i= 0; i < sections.length; i++) {
				IDialogSettings curr= sections[i];
				String projectName= curr.getName();
				IProject project= root.getProject(projectName);
				//make sure project has not been removed
				if (project.isAccessible()) {
					IJavaScriptProject javaProject= JavaScriptCore.create(project);
					if (!fPerProjectSettings.containsKey(javaProject)) {
						String hrefs= curr.get(HREF);
						if (hrefs == null) {
							hrefs= ""; //$NON-NLS-1$
						}
						String destdir= curr.get(DESTINATION);
						if (destdir == null || destdir.length() == 0) {
							destdir= getDefaultDestination(javaProject);
						}
						String antpath= curr.get(ANTPATH);
						if (antpath == null || antpath.length() == 0) {
							antpath= getDefaultAntPath(javaProject);
						}
						ProjectData data= new ProjectData();
						data.setDestination(destdir);
						data.setAntpath(antpath);
						data.setHRefs(hrefs);
						if (!fPerProjectSettings.containsValue(javaProject))
							fPerProjectSettings.put(javaProject, data);
					}
				}
			}
		}
		//finds projects in the workspace that have been added since the
		//last time the wizard was run
		IProject[] projects= root.getProjects();
		for (int i= 0; i < projects.length; i++) {
			IProject project= projects[i];
			if (project.isAccessible()) {
				IJavaScriptProject curr= JavaScriptCore.create(project);
				if (!fPerProjectSettings.containsKey(curr)) {
					ProjectData data= new ProjectData();
					data.setDestination(getDefaultDestination(curr));
					data.setAntpath(getDefaultAntPath(curr));
					data.setHRefs(""); //$NON-NLS-1$
					fPerProjectSettings.put(curr, data);
				}
			}
		}
	}
	
	public void store(IDialogSettings settings) {
		
		IDialogSettings projectsSection= settings.addNewSection(SECTION_PROJECTS);

		//Write all project information to DialogSettings.
		Set keys= fPerProjectSettings.keySet();
		for (Iterator iter= keys.iterator(); iter.hasNext();) {

			IJavaScriptProject curr= (IJavaScriptProject) iter.next();

			IDialogSettings proj= projectsSection.addNewSection(curr.getElementName());
			if (!keys.contains(curr)) {
				proj.put(HREF, ""); //$NON-NLS-1$
				proj.put(DESTINATION, ""); //$NON-NLS-1$
				proj.put(ANTPATH, ""); //$NON-NLS-1$
			} else {
				ProjectData data= (ProjectData) fPerProjectSettings.get(curr);
				proj.put(HREF, data.getHRefs());
				proj.put(DESTINATION, data.getDestination());
				proj.put(ANTPATH, data.getAntPath());
			}
			projectsSection.addSection(proj);
		}
	}

	public void setProjectSettings(IJavaScriptProject project, String destination, String antpath, String[] hrefs) {
		ProjectData data= (ProjectData) fPerProjectSettings.get(project);
		if (data == null) {
			data= new ProjectData();
		}
		data.setDestination(destination);
		data.setAntpath(antpath);
		
		StringBuffer refs= new StringBuffer();
		for (int i= 0; i < hrefs.length; i++) {
			if (i > 0) {
				refs.append(REF_SEPARATOR);
			}
			refs.append(hrefs[i]);
			
		}
		data.setHRefs(refs.toString());
	}
	
	public static String[] getRefTokens(String refs) {
		StringTokenizer tok= new StringTokenizer(refs, String.valueOf(REF_SEPARATOR));
		String[] res= new String[tok.countTokens()];
		for (int i= 0; i < res.length; i++) {
			res[i]= tok.nextToken();
		}
		return res;
	}
	
	
	
	public String[] getHRefs(IJavaScriptProject project) {
		ProjectData data= (ProjectData) fPerProjectSettings.get(project);
		if (data != null) {
			String refs= data.getHRefs();
			return getRefTokens(refs);
		}
		return new String[0];
	}
	
	//for now if multiple projects are selected the destination
	//feild will be empty, 
	public String getDestination(IJavaScriptProject project) {

		ProjectData data= (ProjectData) fPerProjectSettings.get(project);
		if (data != null)
			return data.getDestination();
		else
			return getDefaultDestination(project);
	}
	
	public String getAntpath(IJavaScriptProject project) {
		ProjectData data= (ProjectData) fPerProjectSettings.get(project);
		if (data != null)
			return data.getAntPath();
		else
			return getDefaultAntPath(project);
	}
	
	/// internal
	
	
	private String getDefaultAntPath(IJavaScriptProject project) {
		if (project != null) {
			// The Javadoc.xml file can only be stored locally. So if
			// the project isn't local then we can't provide a good 
			// default location.
			IPath path= project.getProject().getLocation();
			if (path != null)
				return path.append("javadoc.xml").toOSString(); //$NON-NLS-1$
		}

		return ""; //$NON-NLS-1$
	}

	private String getDefaultDestination(IJavaScriptProject project) {
		if (project != null) {
			URL url= JavaScriptUI.getProjectJSdocLocation(project);
			//uses default if source is has http protocol
			if (url == null || !url.getProtocol().equals("file")) { //$NON-NLS-1$
				// Since Javadoc.exe is a local tool its output is local.
				// So if the project isn't local then the default location
				// can't be local to a project. So use #getLocation() to
				// test this is fine here.
				IPath path= project.getProject().getLocation();
				if (path != null)
					return path.append("doc").toOSString(); //$NON-NLS-1$
			} else {
				//must do this to remove leading "/"
				return (new File(url.getFile())).getPath();
			}
		}

		return ""; //$NON-NLS-1$

	}
	
	private static class ProjectData {

		private String fHrefs;
		private String fDestinationDir;
		private String fAntPath;

		public void setHRefs(String hrefs) {
			if (hrefs == null)
				fHrefs= ""; //$NON-NLS-1$
			else
				fHrefs= hrefs;
		}

		public void setDestination(String destination) {
			if (destination == null)
				fDestinationDir= ""; //$NON-NLS-1$
			else
				fDestinationDir= destination;
		}

		public void setAntpath(String antpath) {
			if (antpath == null)
				fAntPath= ""; //$NON-NLS-1$
			else
				fAntPath= antpath;
		}

		public String getHRefs() {
			return fHrefs;
		}

		public String getDestination() {
			return fDestinationDir;
		}

		public String getAntPath() {
			return fAntPath;
		}

	}

	
}
