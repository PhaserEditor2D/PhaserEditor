/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids, sdavids@gmx.de, - bug 38692
 *     Matt Chapman, mpchapman@gmail.com - 89977 Make JDT .java agnostic
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.ui.javadocexport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaUIStatus;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.wst.jsdt.launching.ExecutionArguments;
import org.eclipse.wst.jsdt.launching.IVMInstall;
import org.eclipse.wst.jsdt.launching.IVMInstallType;
import org.eclipse.wst.jsdt.launching.JavaRuntime;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class JavadocOptionsManager {

	private IFile fXmlfile;

	private StatusInfo fWizardStatus;

	private String[] fJavadocCommandHistory;
	
	
	private IJavaScriptElement[] fSelectedElements;
	private IJavaScriptElement[] fInitialElements;

	private String fAccess;
	private String fDocletpath;
	private String fDocletname;
	private boolean fFromStandard;
	private String fStylesheet;
	private String fAdditionalParams;
	private String fVMParams;
	private String fOverview;
	private String fTitle;
	
	private String[] fHRefs;

	private IPath[] fSourcepath;
	private IPath[] fClasspath;

	private boolean fNotree;
	private boolean fNoindex;
	private boolean fSplitindex;
	private boolean fNonavbar;
	private boolean fNodeprecated;
	private boolean fNoDeprecatedlist;
	private boolean fAuthor;
	private boolean fVersion;
	private boolean fUse;
	
	private String fSource;

	private boolean fOpenInBrowser;
	
	private RecentSettingsStore fRecentSettings;

	//add-on for multi-project version
	private String fDestination;
	private String fAntpath;

	public final String PRIVATE= "private"; //$NON-NLS-1$
	public final String PROTECTED= "protected"; //$NON-NLS-1$
	public final String PACKAGE= "package"; //$NON-NLS-1$
	public final String PUBLIC= "public"; //$NON-NLS-1$

	public final String USE= "use"; //$NON-NLS-1$
	public final String NOTREE= "notree"; //$NON-NLS-1$
	public final String NOINDEX= "noindex"; //$NON-NLS-1$
	public final String NONAVBAR= "nonavbar"; //$NON-NLS-1$
	public final String NODEPRECATED= "nodeprecated"; //$NON-NLS-1$
	public final String NODEPRECATEDLIST= "nodeprecatedlist"; //$NON-NLS-1$
	public final String VERSION= "version"; //$NON-NLS-1$
	public final String AUTHOR= "author"; //$NON-NLS-1$
	public final String SPLITINDEX= "splitindex"; //$NON-NLS-1$
	public final String STYLESHEETFILE= "stylesheetfile"; //$NON-NLS-1$
	public final String OVERVIEW= "overview"; //$NON-NLS-1$
	public final String DOCLETNAME= "docletname"; //$NON-NLS-1$
	public final String DOCLETPATH= "docletpath"; //$NON-NLS-1$
	public final String SOURCEPATH= "sourcepath"; //$NON-NLS-1$
	public final String CLASSPATH= "classpath"; //$NON-NLS-1$
	public final String DESTINATION= "destdir"; //$NON-NLS-1$
	public final String OPENINBROWSER= "openinbrowser"; //$NON-NLS-1$	

	public final String VISIBILITY= "access"; //$NON-NLS-1$
	public final String PACKAGENAMES= "packagenames"; //$NON-NLS-1$
	public final String SOURCEFILES= "sourcefiles"; //$NON-NLS-1$
	public final String EXTRAOPTIONS= "additionalparam"; //$NON-NLS-1$
	public final String VMOPTIONS= "vmparam"; //$NON-NLS-1$
	//public final String JAVADOCCOMMAND= "javadoccommand"; //$NON-NLS-1$
	public final String TITLE= "doctitle"; //$NON-NLS-1$
	public final String HREF= "href"; //$NON-NLS-1$

	public final String NAME= "name"; //$NON-NLS-1$
	public final String PATH= "path"; //$NON-NLS-1$
	public final String FROMSTANDARD= "fromStandard"; //$NON-NLS-1$
	public final String ANTPATH= "antpath"; //$NON-NLS-1$
	public final String SOURCE= "source"; //$NON-NLS-1$
	
	private final String SECTION_JAVADOC= "javadoc"; //$NON-NLS-1$

	private static final String JAVADOC_COMMAND_HISTORY= "javadoc_command_history"; //$NON-NLS-1$
	
	public JavadocOptionsManager(IFile xmlJavadocFile, IDialogSettings dialogSettings, List currSelection) {
		fXmlfile= xmlJavadocFile;
		fWizardStatus= new StatusInfo();

		IDialogSettings javadocSection= dialogSettings.getSection(SECTION_JAVADOC); 
		
		String commandHistory= null; 
		if (javadocSection != null) {
			commandHistory= javadocSection.get(JAVADOC_COMMAND_HISTORY);
		}
		if (commandHistory == null || commandHistory.length() == 0) {
			commandHistory= initJavadocCommandDefault(); 
		}
		fJavadocCommandHistory= arrayFromFlatString(commandHistory);
		
		fRecentSettings= new RecentSettingsStore(javadocSection);
		
		if (xmlJavadocFile != null) {
			try {
				JavadocReader reader= new JavadocReader(xmlJavadocFile.getContents());
				Element element= reader.readXML();
				if (element != null) {
					loadFromXML(element);
					return;
				}
				fWizardStatus.setWarning(JavadocExportMessages.JavadocOptionsManager_antfileincorrectCE_warning); 
			} catch (CoreException e) {
				JavaScriptPlugin.log(e);
				fWizardStatus.setWarning(JavadocExportMessages.JavadocOptionsManager_antfileincorrectCE_warning); 
			} catch (IOException e) {
				JavaScriptPlugin.log(e);
				fWizardStatus.setWarning(JavadocExportMessages.JavadocOptionsManager_antfileincorrectIOE_warning); 
			} catch (SAXException e) {
				fWizardStatus.setWarning(JavadocExportMessages.JavadocOptionsManager_antfileincorrectSAXE_warning); 
			}
		}
		if (javadocSection != null) {
			loadFromDialogStore(javadocSection, currSelection);
		} else {
			loadDefaults(currSelection);
		}
	}


	/*
	 * Returns the Java project that is parent top all selected elements or null if
	 * the elements are from several projects.
	 */
	private IJavaScriptProject getSingleProjectFromInitialSelection() {
		IJavaScriptProject res= null;
		for (int i= 0; i < fInitialElements.length; i++) {
			IJavaScriptProject curr= fInitialElements[i].getJavaScriptProject();
			if (res == null) {
				res= curr;
			} else if (!res.equals(curr)) {
				return null;
			}
		}
		if (res != null && res.isOpen()) {
			return res;
		}
		return null;
	}
	
	
	private void loadFromDialogStore(IDialogSettings settings, List sel) {
		fInitialElements= getInitialElementsFromSelection(sel);
		
		IJavaScriptProject project= getSingleProjectFromInitialSelection();

		fAccess= settings.get(VISIBILITY);
		if (fAccess == null)
			fAccess= PROTECTED;

		//this is defaulted to false.
		fFromStandard= settings.getBoolean(FROMSTANDARD);

		//doclet is loaded even if the standard doclet is being used
		fDocletpath= settings.get(DOCLETPATH);
		fDocletname= settings.get(DOCLETNAME);
		if (fDocletpath == null || fDocletname == null) {
			fFromStandard= true;
			fDocletpath= ""; //$NON-NLS-1$
			fDocletname= ""; //$NON-NLS-1$
		}

		
		if (project != null) {
			fAntpath= getRecentSettings().getAntpath(project);
		} else {
			fAntpath= settings.get(ANTPATH);
			if (fAntpath == null) {
				fAntpath= ""; //$NON-NLS-1$
			}
		}

		
		if (project != null) {
			fDestination= getRecentSettings().getDestination(project);
		} else {
			fDestination= settings.get(DESTINATION);
			if (fDestination == null) {
				fDestination= ""; //$NON-NLS-1$
			}
		}

		fTitle= settings.get(TITLE);
		if (fTitle == null)
			fTitle= ""; //$NON-NLS-1$

		fStylesheet= settings.get(STYLESHEETFILE);
		if (fStylesheet == null)
			fStylesheet= ""; //$NON-NLS-1$

		fVMParams= settings.get(VMOPTIONS);
		if (fVMParams == null)
			fVMParams= ""; //$NON-NLS-1$
		
		fAdditionalParams= settings.get(EXTRAOPTIONS);
		if (fAdditionalParams == null)
			fAdditionalParams= ""; //$NON-NLS-1$

		fOverview= settings.get(OVERVIEW);
		if (fOverview == null)
			fOverview= ""; //$NON-NLS-1$

		fUse= loadBoolean(settings.get(USE));
		fAuthor= loadBoolean(settings.get(AUTHOR));
		fVersion= loadBoolean(settings.get(VERSION));
		fNodeprecated= loadBoolean(settings.get(NODEPRECATED));
		fNoDeprecatedlist= loadBoolean(settings.get(NODEPRECATEDLIST));
		fNonavbar= loadBoolean(settings.get(NONAVBAR));
		fNoindex= loadBoolean(settings.get(NOINDEX));
		fNotree= loadBoolean(settings.get(NOTREE));
		fSplitindex= loadBoolean(settings.get(SPLITINDEX));
		fOpenInBrowser= loadBoolean(settings.get(OPENINBROWSER));
		
		fSource= settings.get(SOURCE);
		if (project != null) {
			fSource= project.getOption(JavaScriptCore.COMPILER_SOURCE, true);
		}
		
		if (project != null) {
			fHRefs= getRecentSettings().getHRefs(project);
		} else {
			fHRefs= new String[0];
		}
	}


	//loads defaults for wizard (nothing is stored)
	private void loadDefaults(List sel) {
		fInitialElements= getInitialElementsFromSelection(sel);
		
		IJavaScriptProject project= getSingleProjectFromInitialSelection();

		if (project != null) {
			fAntpath= getRecentSettings().getAntpath(project);
			fDestination= getRecentSettings().getDestination(project);
			fHRefs= getRecentSettings().getHRefs(project);
		} else {
			fAntpath= ""; //$NON-NLS-1$
			fDestination= ""; //$NON-NLS-1$
			fHRefs= new String[0];
		}
		
		fAccess= PUBLIC;

		fDocletname= ""; //$NON-NLS-1$
		fDocletpath= ""; //$NON-NLS-1$
		fTitle= ""; //$NON-NLS-1$
		fStylesheet= ""; //$NON-NLS-1$
		fVMParams= ""; //$NON-NLS-1$
		fAdditionalParams= ""; //$NON-NLS-1$
		fOverview= ""; //$NON-NLS-1$

		fUse= true;
		fAuthor= true;
		fVersion= true;
		fNodeprecated= false;
		fNoDeprecatedlist= false;
		fNonavbar= false;
		fNoindex= false;
		fNotree= false;
		fSplitindex= true;
		fOpenInBrowser= false;
		fSource= "1.3"; //$NON-NLS-1$
		if (project != null) {
			fSource= project.getOption(JavaScriptCore.COMPILER_SOURCE, true);
		}

		//by default it is empty all project map to the empty string
		fFromStandard= true;
	}

	private void loadFromXML(Element element) {

		fAccess= element.getAttribute(VISIBILITY);
		if (fAccess.length() == 0)
			fAccess= PROTECTED;

		//Since the selected packages are stored we must locate the project
		String destination= element.getAttribute(DESTINATION);
		fDestination= makeAbsolutePathFromRelative(new Path(destination)).toOSString();
		fFromStandard= true;
		fDocletname= ""; //$NON-NLS-1$
		fDocletpath= ""; //$NON-NLS-1$

		if (destination.length() == 0) { 
			NodeList list= element.getChildNodes();
			for (int i= 0; i < list.getLength(); i++) {
				Node child= list.item(i);
				if (child.getNodeName().equals("doclet")) { //$NON-NLS-1$
					fDocletpath= ((Element) child).getAttribute(PATH);
					fDocletname= ((Element) child).getAttribute(NAME);
					if (fDocletpath.length() != 0 || fDocletname.length() != 0) {
						fFromStandard= false;
					} else {
						fDocletname= ""; //$NON-NLS-1$
						fDocletpath= ""; //$NON-NLS-1$
					}
					break;
				}
			}
		}
		
		fInitialElements= getSelectedElementsFromAnt(element);
		

		//find all the links stored in the ant script
		NodeList children= element.getChildNodes();
		fHRefs= new String[children.getLength()];
		for (int i= 0; i < fHRefs.length; i++) {
			Node child= children.item(i);
			if (child.getNodeName().equals("link")) { //$NON-NLS-1$
				fHRefs[i]= ((Element) child).getAttribute(HREF);
			}
		}

		// The ant file for generating the Javadoc is always local.
		IPath p= fXmlfile.getLocation();
		if (p != null)
			fAntpath= p.toOSString();
		else
			fAntpath= ""; //$NON-NLS-1$

		fStylesheet= element.getAttribute(STYLESHEETFILE);
		fTitle= element.getAttribute(TITLE);
		

		StringBuffer additionals= new StringBuffer();
		StringBuffer vmargs= new StringBuffer();
		String extraOptions= element.getAttribute(EXTRAOPTIONS);
		if (extraOptions.length() > 0) {
			ExecutionArguments tokens= new ExecutionArguments("", extraOptions); //$NON-NLS-1$
			String[] args= tokens.getProgramArgumentsArray();

			boolean vmarg= false;
			for (int i= 0; i < args.length; i++) {
				String curr= args[i];
				if (curr.length() > 0 && curr.charAt(0) == '-') {
					// an command
					vmarg=(curr.length() > 1 && curr.charAt(1) == 'J');
				}
				if (vmarg) {
					vmargs.append(curr).append(' ');
				} else {
					additionals.append(curr).append(' ');
				}
			}
		}
		
		fAdditionalParams= additionals.toString();
		fVMParams= vmargs.toString();
		fOverview= element.getAttribute(OVERVIEW);

		fUse= loadBoolean(element.getAttribute(USE));
		fAuthor= loadBoolean(element.getAttribute(AUTHOR));
		fVersion= loadBoolean(element.getAttribute(VERSION));
		fNodeprecated= loadBoolean(element.getAttribute(NODEPRECATED));
		fNoDeprecatedlist= loadBoolean(element.getAttribute(NODEPRECATEDLIST));
		fNonavbar= loadBoolean(element.getAttribute(NONAVBAR));
		fNoindex= loadBoolean(element.getAttribute(NOINDEX));
		fNotree= loadBoolean(element.getAttribute(NOTREE));
		fSplitindex= loadBoolean(element.getAttribute(SPLITINDEX));
		
		fSource= element.getAttribute(SOURCE); 
	}

	/*
	 * Method creates an absolute path to the project. If the path is already
	 * absolute it returns the path. If it encounters any difficulties in
	 * creating the absolute path, the method returns null.
	 * 
	 * @param path
	 * @return IPath
	 */
	private IPath makeAbsolutePathFromRelative(IPath path) {
		if (!path.isAbsolute()) {
			if (fXmlfile == null) {
				return null;
			}
			// The XML file is always local. So ok to use getLocation here
			IPath basePath= fXmlfile.getParent().getLocation(); // relative to the ant file location
			if (basePath == null) {
				return null;
			}
			return basePath.append(path);
		}
		return path;
	}
	
	private IContainer[] getSourceContainers(Element element) {
		String sourcePaths= element.getAttribute(SOURCEPATH);
		
		if (sourcePaths.endsWith(File.pathSeparator)) {
			sourcePaths += '.';
		}
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		
		ArrayList res= new ArrayList();
		
		String[] strings= sourcePaths.split(File.pathSeparator);
		for (int i= 0; i < strings.length; i++) {
			IPath path= makeAbsolutePathFromRelative(new Path(strings[i].trim()));
			if (path != null) {
				IContainer[] containers= root.findContainersForLocation(path);
				for (int k= 0; k < containers.length; k++) {
					res.add(containers[k]);
				}
			}
			
		}
		return (IContainer[]) res.toArray(new IContainer[res.size()]);
	}	

	private IJavaScriptElement[] getSelectedElementsFromAnt(Element element) {
		List res= new ArrayList();

		// get all the packages listed in the ANT file
		String packagenames= element.getAttribute(PACKAGENAMES);
		if (packagenames != null) {
			IContainer[] containers= getSourceContainers(element);
			
			StringTokenizer tokenizer= new StringTokenizer(packagenames, ","); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				IPath relPackagePath= new Path(tokenizer.nextToken().trim().replace('.', '/'));
				for (int i= 0; i < containers.length; i++) {
					IContainer curr= containers[i];
					IResource resource= curr.findMember(relPackagePath);
					if (resource != null) {
						IJavaScriptElement javaElem= JavaScriptCore.create(resource);
						if (javaElem instanceof IPackageFragment) {
							res.add(javaElem);
						}
					}
				}
			}
		}

		//get all CompilationUnites listed in the ANT file
		String sourcefiles= element.getAttribute(SOURCEFILES);
		if (sourcefiles != null) {
			IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
			
			StringTokenizer tokenizer= new StringTokenizer(sourcefiles, ","); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				String name= tokenizer.nextToken().trim();
				if (JavaScriptCore.isJavaScriptLikeFileName(name)) {
					IPath path= makeAbsolutePathFromRelative(new Path(name));
					//if unable to create an absolute path to the resource skip it
					if (path != null) {
						IFile[] files= root.findFilesForLocation(path);
						for (int i= 0; i < files.length; i++) {
							IJavaScriptElement el= JavaScriptCore.createCompilationUnitFrom(files[i]);
							if (el != null) {
								res.add(el);
							}
						}
					}
				}
			}
		}
		return (IJavaScriptElement[]) res.toArray(new IJavaScriptElement[res.size()]);
	}

	/**
	 * @return Returns the javadocCommandHistory.
	 */
	public String[] getJavadocCommandHistory() {
		return fJavadocCommandHistory;
	}

	
	//it is possible that the package list is empty
	public StatusInfo getWizardStatus() {
		return fWizardStatus;
	}

	public IJavaScriptElement[] getInitialElements() {
		return fInitialElements;
	}

	public IJavaScriptElement[] getSourceElements() {
		return fSelectedElements;
	}

	public String getAccess() {
		return fAccess;
	}

	public String getAntpath() {
		return fAntpath;
	}

	public boolean isFromStandard() {
		return fFromStandard;
	}

	public String getDestination() {
		return fDestination;
	}

	public String getDocletPath() {
		return fDocletpath;
	}

	public String getDocletName() {
		return fDocletname;
	}

	public String getStyleSheet() {
		return fStylesheet;
	}

	public String getOverview() {
		return fOverview;
	}

	public String getAdditionalParams() {
		return fAdditionalParams;
	}
	
	public String getVMParams() {
		return fVMParams;
	}

	public IPath[] getClasspath() {
		return fClasspath;
	}

	public IPath[] getSourcepath() {
		return fSourcepath;
	}

	public String getTitle() {
		return fTitle;
	}

	public boolean doOpenInBrowser() {
		return fOpenInBrowser;
	}
	
	public String[] getHRefs() {
		return fHRefs;
	}

	public boolean getBoolean(String flag) {

		if (flag.equals(AUTHOR))
			return fAuthor;
		else if (flag.equals(VERSION))
			return fVersion;
		else if (flag.equals(USE))
			return fUse;
		else if (flag.equals(NODEPRECATED))
			return fNodeprecated;
		else if (flag.equals(NODEPRECATEDLIST))
			return fNoDeprecatedlist;
		else if (flag.equals(NOINDEX))
			return fNoindex;
		else if (flag.equals(NOTREE))
			return fNotree;
		else if (flag.equals(SPLITINDEX))
			return fSplitindex;
		else if (flag.equals(NONAVBAR))
			return fNonavbar;
		else
			return false;
	}

	private boolean loadBoolean(String value) {

		if (value == null || value.length() == 0) 
			return false;
		else {
			if (value.equals("true")) //$NON-NLS-1$
				return true;
			else
				return false;
		}
	}
	
	private String flatPathList(IPath[] paths) {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < paths.length; i++) {
			if (i > 0) {
				buf.append(File.pathSeparatorChar);
			}
			buf.append(paths[i].toOSString());
		}
		return buf.toString();
	}
	
	private String flatStringList(String[] paths) {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < paths.length; i++) {
			if (i > 0) {
				buf.append(File.pathSeparatorChar);
			}
			buf.append(paths[i]);
		}
		return buf.toString();
	}
	
	private String[] arrayFromFlatString(String str) {
		StringTokenizer tok= new StringTokenizer(str, File.pathSeparator);
		String[] res= new String[tok.countTokens()];
		for (int i= 0; i < res.length; i++) {
			res[i]= tok.nextToken();
		}
		return res;
	}
	

	public IStatus getArgumentArray(List vmArgs, List toolArgs) {
		MultiStatus status= new MultiStatus(JavaScriptUI.ID_PLUGIN, IStatus.OK, JavadocExportMessages.JavadocOptionsManager_status_title, null);
		
		//bug 38692
		vmArgs.add(getJavadocCommandHistory()[0]);
		
		if (fFromStandard) {
			toolArgs.add("-d"); //$NON-NLS-1$
			toolArgs.add(fDestination);
		} else {
			toolArgs.add("-doclet"); //$NON-NLS-1$
			toolArgs.add(fDocletname);
			toolArgs.add("-docletpath"); //$NON-NLS-1$
			toolArgs.add(fDocletpath);
		}
		
		if (fSourcepath.length > 0) {
			toolArgs.add("-sourcepath"); //$NON-NLS-1$
			toolArgs.add(flatPathList(fSourcepath));
		}
		
		if (fClasspath.length > 0) {
			toolArgs.add("-classpath"); //$NON-NLS-1$
			toolArgs.add(flatPathList(fClasspath));
		}
		toolArgs.add("-" + fAccess); //$NON-NLS-1$

		if (fFromStandard) {
			if (fSource.length() > 0 && !fSource.equals("-")) { //$NON-NLS-1$
				toolArgs.add("-source"); //$NON-NLS-1$
				toolArgs.add(fSource); 
			}
			
			if (fUse)
				toolArgs.add("-use"); //$NON-NLS-1$
			if (fVersion)
				toolArgs.add("-version"); //$NON-NLS-1$
			if (fAuthor)
				toolArgs.add("-author"); //$NON-NLS-1$
			if (fNonavbar)
				toolArgs.add("-nonavbar"); //$NON-NLS-1$
			if (fNoindex)
				toolArgs.add("-noindex"); //$NON-NLS-1$
			if (fNotree)
				toolArgs.add("-notree"); //$NON-NLS-1$
			if (fNodeprecated)
				toolArgs.add("-nodeprecated"); //$NON-NLS-1$
			if (fNoDeprecatedlist)
				toolArgs.add("-nodeprecatedlist"); //$NON-NLS-1$
			if (fSplitindex)
				toolArgs.add("-splitindex"); //$NON-NLS-1$

			if (fTitle.length() != 0) { 
				toolArgs.add("-doctitle"); //$NON-NLS-1$
				toolArgs.add(fTitle);
			}


			if (fStylesheet.length() != 0) { 
				toolArgs.add("-stylesheetfile"); //$NON-NLS-1$
				toolArgs.add(fStylesheet);
			}
			
			for (int i= 0; i < fHRefs.length; i++) {
				toolArgs.add("-link"); //$NON-NLS-1$
				toolArgs.add(fHRefs[i]);
			}
			
		} //end standard options

		if (fAdditionalParams.length() + fVMParams.length() != 0) {
			ExecutionArguments tokens= new ExecutionArguments(fVMParams, fAdditionalParams); 
			String[] vmArgsArray= tokens.getVMArgumentsArray();
			for (int i= 0; i < vmArgsArray.length; i++) {
				vmArgs.add(vmArgsArray[i]);
			}
			String[] argsArray= tokens.getProgramArgumentsArray();
			for (int i= 0; i < argsArray.length; i++) {
				toolArgs.add(argsArray[i]);
			}
		}
		addProxyOptions(vmArgs);
		
		if (fOverview.length() != 0) { 
			toolArgs.add("-overview"); //$NON-NLS-1$
			toolArgs.add(fOverview);
		}

		for (int i= 0; i < fSelectedElements.length; i++) {
			IJavaScriptElement curr= fSelectedElements[i];
			if (curr instanceof IPackageFragment) {
				toolArgs.add(curr.getElementName());
			} else if (curr instanceof IJavaScriptUnit) {
				// Since the Javadoc tool is running locally we can only  create
				// Javadoc for local resources. So using the location is fine here.
				IPath p= curr.getResource().getLocation();
				if (p != null) {
					toolArgs.add(p.toOSString());
				} else {
					String message= JavadocExportMessages.JavadocOptionsManager_status_non_local;
					status.add(new StatusInfo(IStatus.WARNING, Messages.format(message, curr.getPath().makeRelative().toString())));
				}
			}
		}
		return status;
	}
	
	private void addProxyOptions(List vmOptions) {
		// bug 74132
		String hostPrefix= "-J-Dhttp.proxyHost="; //$NON-NLS-1$
		String portPrefix= "-J-Dhttp.proxyPort="; //$NON-NLS-1$
		for (int i= 0; i < vmOptions.size(); i++) {
			String curr= (String) vmOptions.get(i);
			if (curr.startsWith(hostPrefix) || curr.startsWith(portPrefix)) {
				return;
			}
		}
		String proxyHost= System.getProperty("http.proxyHost"); //$NON-NLS-1$
		if (proxyHost != null) {
			vmOptions.add(hostPrefix + proxyHost); 
		}
		
		String proxyPort= System.getProperty("http.proxyPort"); //$NON-NLS-1$
		if (proxyPort != null) {
			vmOptions.add(portPrefix + proxyPort); 
		}
	}
	

	public File createXML(IJavaScriptProject[] projects) throws CoreException {
		FileOutputStream objectStreamOutput= null;
		//@change
		//for now only writing ant files for single project selection
		try {
			if (fAntpath.length() > 0) {
				File file= new File(fAntpath);

				String encoding= "UTF-8"; //$NON-NLS-1$
				IContentType type= Platform.getContentTypeManager().getContentType("org.eclipse.ant.core.antBuildFile"); //$NON-NLS-1$
				if (type != null)
					encoding= type.getDefaultCharset();
				IPath filePath= Path.fromOSString(fAntpath);
				IPath directoryPath= filePath.removeLastSegments(1);
				
				IPath basePath= null;
				IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
				if (root.findFilesForLocation(filePath).length > 0) {
					basePath= directoryPath; // only do relative path if ant file is stored in the workspace
				}
				
				directoryPath.toFile().mkdirs();
			
				objectStreamOutput= new FileOutputStream(file);
				JavadocWriter writer= new JavadocWriter(objectStreamOutput, encoding, basePath, projects);
				writer.writeXML(this);
				return file;
			}
		} catch (IOException e) {
			String message= JavadocExportMessages.JavadocOptionsManager_createXM_error; 
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, message, e));
		} catch (ParserConfigurationException e) {
			String message= JavadocExportMessages.JavadocOptionsManager_createXM_error; 
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, message, e));
		} catch (TransformerException e) {
			String message= JavadocExportMessages.JavadocOptionsManager_createXM_error; 
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, message, e));
		} finally {
			if (objectStreamOutput != null) {
				try {
					objectStreamOutput.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}

	public void updateDialogSettings(IDialogSettings dialogSettings, IJavaScriptProject[] checkedProjects) {
		IDialogSettings settings= dialogSettings.addNewSection(SECTION_JAVADOC);

		settings.put(JAVADOC_COMMAND_HISTORY, flatStringList(fJavadocCommandHistory));
		if (fJavadocCommandHistory.length > 0) {
			IPreferenceStore store= PreferenceConstants.getPreferenceStore();
			store.setValue(PreferenceConstants.JAVADOC_COMMAND, fJavadocCommandHistory[0]);
		}
		
		
		settings.put(FROMSTANDARD, fFromStandard);

		settings.put(DOCLETNAME, fDocletname);
		settings.put(DOCLETPATH, fDocletpath);

		settings.put(VISIBILITY, fAccess);

		settings.put(USE, fUse);
		settings.put(AUTHOR, fAuthor);
		settings.put(VERSION, fVersion);
		settings.put(NODEPRECATED, fNodeprecated);
		settings.put(NODEPRECATEDLIST, fNoDeprecatedlist);
		settings.put(SPLITINDEX, fSplitindex);
		settings.put(NOINDEX, fNoindex);
		settings.put(NOTREE, fNotree);
		settings.put(NONAVBAR, fNonavbar);
		settings.put(OPENINBROWSER, fOpenInBrowser);
		settings.put(SOURCE, fSource);

		if (fAntpath.length() != 0) 
			settings.put(ANTPATH, fAntpath);
		if (fDestination.length() != 0) 
			settings.put(DESTINATION, fDestination);
		if (fAdditionalParams.length() != 0) 
			settings.put(EXTRAOPTIONS, fAdditionalParams);
		if (fVMParams.length() != 0) 
			settings.put(VMOPTIONS, fVMParams);
		if (fOverview.length() != 0) 
			settings.put(OVERVIEW, fOverview);
		if (fStylesheet.length() != 0) 
			settings.put(STYLESHEETFILE, fStylesheet);
		if (fTitle.length() != 0) 
			settings.put(TITLE, fTitle);

		if (checkedProjects.length == 1) {
			updateRecentSettings(checkedProjects[0]);
		}
		getRecentSettings().store(settings);
	}
		
	public void setJavadocCommandHistory(String[] javadocCommandHistory) {
		fJavadocCommandHistory= javadocCommandHistory;
	}

	public void setAccess(String access) {
		fAccess= access;
	}
	public void setDestination(String destination) {
		fDestination= destination;
	}

	public void setDocletPath(String docletpath) {
		fDocletpath= docletpath;
	}

	public void setDocletName(String docletname) {
		fDocletname= docletname;
	}

	public void setStyleSheet(String stylesheet) {
		fStylesheet= stylesheet;
	}

	public void setOverview(String overview) {
		fOverview= overview;
	}

	public void setAdditionalParams(String params) {
		fAdditionalParams= params;
	}
	
	public void setVMParams(String params) {
		fVMParams= params;
	}

	public void setGeneralAntpath(String antpath) {
		fAntpath= antpath;
	}
	public void setClasspath(IPath[] classpath) {
		fClasspath= classpath;
	}

	public void setSourcepath(IPath[] sourcepath) {
		fSourcepath= sourcepath;
	}

	public void setSelectedElements(IJavaScriptElement[] elements) {
		fSelectedElements= elements;
	}

	public void setFromStandard(boolean fromStandard) {
		fFromStandard= fromStandard;
	}

	public void setTitle(String title) {
		fTitle= title;
	}

	public void setOpenInBrowser(boolean openInBrowser) {
		fOpenInBrowser= openInBrowser;
	}
	
	public void setHRefs(String[] hrefs) {
		fHRefs= hrefs;
	}

	public void setBoolean(String flag, boolean value) {

		if (flag.equals(AUTHOR))
			fAuthor= value;
		else if (flag.equals(USE))
			fUse= value;
		else if (flag.equals(VERSION))
			fVersion= value;
		else if (flag.equals(NODEPRECATED))
			fNodeprecated= value;
		else if (flag.equals(NODEPRECATEDLIST))
			fNoDeprecatedlist= value;
		else if (flag.equals(NOINDEX))
			fNoindex= value;
		else if (flag.equals(NOTREE))
			fNotree= value;
		else if (flag.equals(SPLITINDEX))
			fSplitindex= value;
		else if (flag.equals(NONAVBAR))
			fNonavbar= value;
	}
	
	public void setSource(String source) {
		fSource= source;
	}
	
	public String getSource() {
		return fSource;
	}

	private IJavaScriptElement[] getInitialElementsFromSelection(List candidates) {
		ArrayList res= new ArrayList();
		for (int i= 0; i < candidates.size(); i++) {
			try {
				IJavaScriptElement elem= getSelectableJavaElement(candidates.get(i));
				if (elem != null) {
					res.add(elem);
				}
			} catch (JavaScriptModelException ignore) {
				// ignore this
			}
		}
		return (IJavaScriptElement[]) res.toArray(new IJavaScriptElement[res.size()]);
	}

	private IJavaScriptElement getSelectableJavaElement(Object obj) throws JavaScriptModelException {
		IJavaScriptElement je= null;
		if (obj instanceof IAdaptable) {
			je= (IJavaScriptElement) ((IAdaptable) obj).getAdapter(IJavaScriptElement.class);
		}

		if (je != null) {
			switch (je.getElementType()) {
				case IJavaScriptElement.JAVASCRIPT_MODEL :
				case IJavaScriptElement.JAVASCRIPT_PROJECT :
				case IJavaScriptElement.CLASS_FILE :
					break;
				case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT :
					if (containsCompilationUnits((IPackageFragmentRoot) je)) {
						return je;
					}
					break;
				case IJavaScriptElement.PACKAGE_FRAGMENT :
					if (containsCompilationUnits((IPackageFragment) je)) {
						return je;
					}
					break;
				default :
					IJavaScriptUnit cu= (IJavaScriptUnit) je.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
					if (cu != null) {
						return cu;
					}
			}
			IJavaScriptProject project= je.getJavaScriptProject();
			if (isValidProject(project))
				return project;
		}

		return null;
	}

	private boolean isValidProject(IJavaScriptProject project) throws JavaScriptModelException {
		if (project != null && project.exists() && project.isOpen()) {
			return true;
		}
		return false;
	}

	private boolean containsCompilationUnits(IPackageFragmentRoot root) throws JavaScriptModelException {
		if (root.getKind() != IPackageFragmentRoot.K_SOURCE) {
			return false;
		}

		IJavaScriptElement[] elements= root.getChildren();
		for (int i= 0; i < elements.length; i++) {
			if (elements[i] instanceof IPackageFragment) {
				IPackageFragment fragment= (IPackageFragment) elements[i];
				if (containsCompilationUnits(fragment)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean containsCompilationUnits(IPackageFragment pack) throws JavaScriptModelException {
		return pack.getJavaScriptUnits().length > 0;
	}

	public RecentSettingsStore getRecentSettings() {
		return fRecentSettings;
	}

	/**
	 * @param project
	 */
	public void updateRecentSettings(IJavaScriptProject project) {
		fRecentSettings.setProjectSettings(project, fDestination, fAntpath, fHRefs);
	}

	
	private static String initJavadocCommandDefault() {
		IPreferenceStore store= JavaScriptPlugin.getDefault().getPreferenceStore();
		String cmd= store.getString(PreferenceConstants.JAVADOC_COMMAND);	// old location
		if (cmd != null && cmd.length() > 0) {
			store.setToDefault(PreferenceConstants.JAVADOC_COMMAND);
			return cmd;
		}
		
		File file= findJavaDocCommand();
		if (file != null) {
			return file.getPath();
		}
		return ""; //$NON-NLS-1$
	}
	

	private static File findJavaDocCommand() {
		IVMInstall install= JavaRuntime.getDefaultVMInstall();
		if (install != null) {
			File res= getCommand(install);
			if (res != null) {
				return res;
			}
		}
		
		IVMInstallType[] jreTypes= JavaRuntime.getVMInstallTypes();
		for (int i= 0; i < jreTypes.length; i++) {
			IVMInstallType jreType= jreTypes[i];
			IVMInstall[] installs= jreType.getVMInstalls();
			for (int k= 0; k < installs.length; k++) {
				File res= getCommand(installs[k]);
				if (res != null) {
					return res;
				}
			}
		}
		return null;
	}

	private static File getCommand(IVMInstall install) {
		File installLocation= install.getInstallLocation();
		if (installLocation != null) {
			File javaDocCommand= new File(installLocation, "bin/javadoc"); //$NON-NLS-1$
			if (javaDocCommand.isFile()) {
				return javaDocCommand;
			}
			javaDocCommand= new File(installLocation, "bin/javadoc.exe"); //$NON-NLS-1$
			if (javaDocCommand.isFile()) {
				return javaDocCommand;
			}
		}
		return null;
	}




}
