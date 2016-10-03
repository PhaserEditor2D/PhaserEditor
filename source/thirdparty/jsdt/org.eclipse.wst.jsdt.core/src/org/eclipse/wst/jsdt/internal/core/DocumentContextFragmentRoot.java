/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     bug:244839 - eugene@genuitec.com
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.IAccessRule;
import org.eclipse.wst.jsdt.core.IIncludePathAttribute;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JSDScopeUtil;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction;
import org.eclipse.wst.jsdt.internal.core.search.IRestrictedAccessBindingRequestor;
import org.eclipse.wst.jsdt.internal.core.search.JavaSearchScope;


public class DocumentContextFragmentRoot extends PackageFragmentRoot{

	/*
	 * if user includes dojo.js check if dojo.js.uncompressed.js exists instead and replace with that.
	 */
	public static final boolean HACK_DOJO= true;
	private final String UNCOMPRESSED_DOJO="dojo.js.uncompressed.js"; //$NON-NLS-1$
	private final  String DOJO_COMPRESSED = "dojo.js"; //$NON-NLS-1$
	//private static final ClasspathAttribute HIDE = new ClasspathAttribute("hide","true"); //$NON-NLS-1$ //$NON-NLS-2$
	private String[] includedFiles;
	//private Long[] timeStamps;
	private IFile fRelativeFile;
	private IResource absolutePath;
	private IPath webContext;
	private IIncludePathEntry rawClassPathEntry;

	//public static final boolean RETURN_CU = true;
	private static final boolean DEBUG = false;
	//private boolean dirty;

	private static int instances=0;
	private IJavaScriptUnit[] workingCopies;
	private String[] fSystemFiles;
	private RestrictedDocumentBinding importPolice;

	private static final IPath EMPTY_PATH = new Path(""); //$NON-NLS-1$
	
	class RestrictedDocumentBinding implements IRestrictedAccessBindingRequestor {

		private ArrayList foundPaths=new ArrayList();
		private String exclude;
		private boolean shown;

		public void reset() {
			foundPaths.clear();
			shown=false;
		}

		public boolean acceptBinding(int type,int modifiers, char[] packageName,char[] simpleTypeName, String path, AccessRestriction access) {
			if(path!=null && exclude!=null && path.compareTo(exclude)==0) return false;

			if(DEBUG && !shown) {
				shown=false;
				IJavaScriptProject proj = getJavaScriptProject();
				try {
					IIncludePathEntry[] entries = proj.getResolvedIncludepath(true);
					System.out.println("DocumentContextFragmentRoot ====>" +"Project Classpath : \n"); //$NON-NLS-1$ //$NON-NLS-2$

					for(int i = 0;i<entries.length;i++) {
						System.out.println("\t" + entries[i].getPath()); //$NON-NLS-1$

					}
				} catch (JavaScriptModelException ex) {
					// TODO Auto-generated catch block
					ex.printStackTrace();
				}
			}
			for (int i = 0; workingCopies!=null && i < workingCopies.length; i++) {
				if (workingCopies[i].getPath().toString().equals(path)) {
					if(DEBUG) System.out.println("DocumentContextFragmentRoot ====>" +"REJECTING binding..\n\t" + new String(simpleTypeName) + " in " + path + "\n\tfor file " + fRelativeFile.toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					if(DEBUG) System.out.println("\tType is in WorkingCopies "); //$NON-NLS-1$
					return false;
				}
			}
			
			this.foundPaths.add(path);
			return true;

//			for(int i = 0;i<includedFiles.length;i++) {
//				if(Util.isSameResourceString(path, includedFiles[i])) {
//					if(DEBUG) System.out.println("DocumentContextFragmentRoot ====>" + "Accepting binding.. " + new String(simpleTypeName) + " in " + path + "\n\tfor file " + fRelativeFile.toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
//					this.foundPaths.add(path);
//					return true;
//				} else if(includedFiles[i].equals("*")) { //$NON-NLS-1$
//					this.foundPaths.add(path);
//					return true;
//				}
//				else if(HACK_DOJO) {
//					String includeString = includedFiles[i];
//					if(path.toLowerCase().indexOf(DOJO_COMPRESSED)>0 && (includeString.toLowerCase().indexOf(UNCOMPRESSED_DOJO)>0)) {
//						this.foundPaths.add(path);
//						return true;
//					}
//
//				}
//			}
//
//			String systemFiles[] = getProjectSystemFiles();
//
//			for(int i = 0;i<systemFiles.length;i++) {
//				if(Util.isSameResourceString(path, systemFiles[i]) || (new Path(systemFiles[i])).isPrefixOf(new Path(path))) {
//					if(DEBUG) System.out.println("DocumentContextFragmentRoot ====>" + "Accepting binding.. " + new String(simpleTypeName) + " in " + path + " \n\tfor file " + fRelativeFile.toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
//					this.foundPaths.add(path);
//					return true;
//				}
//			}
//			if(DEBUG) System.out.println("DocumentContextFragmentRoot ====>" +"REJECTING binding..\n\t" + new String(simpleTypeName) + " in " + path + " \n\tfor file " + fRelativeFile.toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
//			if(DEBUG) System.out.println("\t(relative) page includes = : " ); //$NON-NLS-1$
//			if(DEBUG) {
//				for(int i = 0;includedFiles!=null && i<includedFiles.length;i++) {
//					System.out.println("\t\t" + includedFiles[i]); //$NON-NLS-1$
//				}
//			}
//			//this.foundPath=null;
//			return false;
		}

		public String getFoundPath() {
			return foundPaths.size()>0?(String)foundPaths.get(0):null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.internal.core.search.IRestrictedAccessBindingRequestor#getFoundPaths()
		 */
		public ArrayList getFoundPaths() {
			return foundPaths;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.internal.core.search.IRestrictedAccessBindingRequestor#setExcludePath(java.lang.String)
		 */
		public void setExcludePath(String excludePath) {
			this.exclude=excludePath;

		}
	}

	public String[] getProjectSystemFiles() {

		if(fSystemFiles!=null) return fSystemFiles;

		IJavaScriptProject javaProject = getJavaScriptProject();
		int lastGood = 0;
		IPackageFragmentRoot[]  projectRoots = null;

		try {
			projectRoots = javaProject.getPackageFragmentRoots();
			for(int i =0;i<projectRoots.length;i++) {
				if(projectRoots[i].isLanguageRuntime()) {
					projectRoots[lastGood++]=projectRoots[i];
				}else if(projectRoots[i].getRawIncludepathEntry().getEntryKind()== IIncludePathEntry.CPE_SOURCE) {
					projectRoots[lastGood++]=projectRoots[i];
				}
			}
		} catch (JavaScriptModelException ex) {
			projectRoots = new IPackageFragmentRoot[0];
		}

		fSystemFiles = new String[lastGood ];
		for(int i = 0;i<fSystemFiles.length;i++) {
			fSystemFiles[i] = projectRoots[i].getPath().toString().intern();
		}
		return fSystemFiles;
	}


	public void classpathChange() {
		fSystemFiles=null;
	}



	public DocumentContextFragmentRoot(IJavaScriptProject project,
									   IFile resourceRelativeFile,
									   IPath resourceAbsolutePath,
									   IPath webContext,
									   IIncludePathEntry rawClassPath) {

		super(resourceRelativeFile, (JavaProject)project);

		fRelativeFile = resourceRelativeFile ;
	//	this.includedFiles = new IPath[0];
		//this.timeStamps = new Long[0];
		this.absolutePath = ((IContainer)project.getResource()).findMember(resourceAbsolutePath);
		this.webContext=webContext;
		this.rawClassPathEntry = rawClassPath;
		//dirty = true;
		if(DEBUG) System.out.println("DocumentContextFragmentRoot ====>" + "Creating instance for total of:>>" + ++instances + "<<.  \n\tRelative file:" + fRelativeFile.toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$


	}

	public void finalize() {

		if(DEBUG) System.out.println("DocumentContextFragmentRoot ====>" + "finalize() for a  total of:>>" + --instances + "<<.  \n\tRelative file:" + fRelativeFile!=null?null:fRelativeFile.toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}


	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.core.PackageFragmentRoot#getRawClasspathEntry()
	 */
	public IIncludePathEntry getRawIncludepathEntry() throws JavaScriptModelException {
		if(rawClassPathEntry!=null) return rawClassPathEntry;
		return super.getRawIncludepathEntry();
	}

	protected  RestrictedDocumentBinding getRestrictedAccessRequestor() {
		 if(importPolice==null) {
			 importPolice = new RestrictedDocumentBinding();
		 }
		importPolice.reset();
		return importPolice;
	}

	public DocumentContextFragmentRoot(IJavaScriptProject project,
			   IFile resourceRelativeFile,
			   IPath resourceAbsolutePath,
			   IPath webContext) {
		this(project,resourceRelativeFile,resourceAbsolutePath,webContext,null);
	}

	public DocumentContextFragmentRoot(IJavaScriptProject project,
			   						   IFile resourceRelativeFile) {

			this(project,resourceRelativeFile, new Path(""), new Path("")); //$NON-NLS-1$ //$NON-NLS-2$
	}


	public void setIncludedFiles2(String[] fileNames) {



		ArrayList newImports = new ArrayList();
		//int arrayLength = 0;

		for(int i = 0; i<fileNames.length;i++) {
			File importFile = isValidImport(fileNames[i]);
			if(importFile==null) continue;
			IPath importPath = resolveChildPath(fileNames[i]);
			newImports.add( importPath.toString() );
		}

		boolean equals = includedFiles!=null && newImports.size()==includedFiles.length;

		for(int i=0;equals && i<newImports.size();i++) {
			if(((String)newImports.get(i)).compareTo(includedFiles[i])!=0) equals=false;
		}

		if(DEBUG) System.out.println("DocumentContextFragmentRoot ====>" + "Imports " + (equals?"did NOT change": "CHANGED:") + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		if(DEBUG) {
			for(int i = 0;includedFiles!=null && i<includedFiles.length;i++) {
				System.out.println("\t\t" + includedFiles[i]); //$NON-NLS-1$
			}
		}
		if(equals) return;
/*  start  try and expand the include paths from the library entries if necisary */
		IIncludePathEntry[] current = new IIncludePathEntry[0];
		IJavaScriptProject javaProject = getJavaScriptProject();

		try {
			current = javaProject.getRawIncludepath();
		} catch (JavaScriptModelException ex) {
			// TODO Auto-generated catch block
		//	ex.printStackTrace();
		}
		for(int i = 0;i<current.length;i++) {
			JsGlobalScopeContainerInitializer init = JSDScopeUtil.getContainerInitializer(current[i].getPath());
			for(int k=0;k<fileNames.length;k++) {
				String[] newEntries = init.resolvedLibraryImport(fileNames[k]);
				if(newEntries!=null && newEntries.length>0        ) {
					newImports.removeAll(Arrays.asList(newEntries));
					newImports.addAll(Arrays.asList(newEntries));
				}
			}
		}

/* end class path expansion */
		this.includedFiles = (String[])newImports.toArray(new String[newImports.size()]);
	//	System.arraycopy(newImports, 0, this.includedFiles, 0, arrayLength);
		updateClasspathIfNeeded();
		dojoHack();
	}
	public void setIncludedFiles(String[] fileNames) {
		
		
		
		String[] newImports = new String[fileNames.length];
		//Long[] newTimestamps = new Long[fileNames.length];
		int arrayLength = 0;
		
		for(int i = 0; i<fileNames.length;i++) {
			File importFile = isValidImport(fileNames[i]);
			if(importFile==null && !fileNames[i].equals("*")) continue; //$NON-NLS-1$
			if(fileNames[i].equals("*")) {
				newImports[arrayLength++] = fileNames[i];
			} else {
				IPath importPath = resolveChildPath(fileNames[i]);	
				newImports[arrayLength++] = importPath.toString();
			}
			//newTimestamps[arrayLength] = new Long(importFile.lastModified());	

			//arrayLength++;
		}
		
		boolean equals = includedFiles!=null && arrayLength==includedFiles.length;
		
		for(int i=0;equals && i<arrayLength;i++) {
			if(newImports[i].compareTo(includedFiles[i])!=0) equals=false;
		}
		
		//this.includedFiles!=null && (newImports !=null) &&   this.includedFiles.length == arrayLength;
		
		//equals = equals || (this.includedFiles==null && newImports ==null);
		//if(!equals) removeStaleClasspath(this.includedFiles);
		
		//		
//
//		if(!equals) dirty = true;
//		
//		/* try some more cases */
//		
//		for(int i = 0;!dirty && i<this.includedFiles.length;i++) {
//			if(!(this.includedFiles[i].equals(newImports[i]))) {
//				dirty = true;
//				
//			}
//		}
//		
//		for(int i = 0;!dirty && i<newTimestamps.length;i++) {
//			if(!(this.timeStamps[i].equals(newTimestamps[i]))) {
//				dirty = true;
//			}
//		}
//		
//		if(!dirty) return;
		if(DEBUG) System.out.println("DocumentContextFragmentRoot ====>" + "Imports " + (equals?"did NOT change": "CHANGED:") + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		if(DEBUG) {
			for(int i = 0;includedFiles!=null && i<includedFiles.length;i++) {
				System.out.println("\t\t" + includedFiles[i]); //$NON-NLS-1$
			}
		}
		if(equals) return;
		this.includedFiles = new String[arrayLength];
	//	this.timeStamps = new Long[arrayLength];
		System.arraycopy(newImports, 0, this.includedFiles, 0, arrayLength);
	//	System.arraycopy(newTimestamps, 0, this.timeStamps, 0, arrayLength);
		dojoHack();
		updateClasspathIfNeeded();
	
		
	}
	private void dojoHack() {
		if(!HACK_DOJO) return;
		

		for(int i = 0;i<includedFiles.length;i++) {
			String includeString = includedFiles[i];

			int dojoIndex = includeString.toLowerCase().indexOf(DOJO_COMPRESSED);

			if(includeString!=null && dojoIndex>=0) {
				/* found dojo.js replace it with dojo.js.uncompressed.js if it exists */
				String newIncludeString = includeString.substring(0, dojoIndex) + UNCOMPRESSED_DOJO + includeString.substring(dojoIndex + DOJO_COMPRESSED.length(),includeString.length());
				File djUncom = isValidImport(newIncludeString);
				if(djUncom!=null && djUncom.exists()) {
					includedFiles[i] = newIncludeString;
				}
			}
		}
	}

	//private void removeStaleClasspath(String[] oldEntries) {
//
//	}

	private void updateClasspathIfNeeded() {



		ArrayList newEntriesList = new ArrayList();
		IJavaScriptProject javaProject = getJavaScriptProject();
		IResource myResource = getResource();
		IContainer folder = (IContainer)myResource;

		for(int i = 0;i<includedFiles.length;i++) {
			IResource theFile = folder.findMember(includedFiles[i]);
			if(theFile == null || javaProject.isOnIncludepath(theFile)) continue;
			IIncludePathEntry entry = JavaScriptCore.newLibraryEntry(theFile.getLocation().makeAbsolute(), null, null, new IAccessRule[0], new IIncludePathAttribute[] {IIncludePathAttribute.HIDE}, true);

			newEntriesList.add(entry);
		}
		IIncludePathEntry[] current = new IIncludePathEntry[0];
		try {
			current = javaProject.getRawIncludepath();
		} catch (JavaScriptModelException ex) {
			// TODO Auto-generated catch block
		//	ex.printStackTrace();
		}


		IIncludePathEntry[] newCpEntries = new IIncludePathEntry[newEntriesList.size() + current.length];
		System.arraycopy(current, 0, newCpEntries, 0, current.length);
		int newPtr = 0 ;
		for(int i =  current.length; i<newCpEntries.length;i++) {
			newCpEntries[i] = (IIncludePathEntry)newEntriesList.get(newPtr++);
		}
		try {
			javaProject.setRawIncludepath(newCpEntries, false, new NullProgressMonitor());
		} catch (JavaScriptModelException ex) {}

	}

	public IPath resolveChildPath(String childPathString) {
        // Genuitec Begin Fix 6149: Exception opening external HTML file
	    if (getResource() == null) {
	        return null;
	    }
	    // Genuitec End Fix 6149: Exception opening external HTML file
		/* relative paths:
		 * ./testfile.js  are relative to file scope
		 * absolute paths: /scripts/file.js are relative to absolutePath, and must be made relative to this resource
		 * if the file does not exist in context root, the path is the absolute path on the filesystem.
		 */
		if(childPathString==null) return null;
		if(childPathString.length()==0) return new Path(""); //$NON-NLS-1$
		IPath resolvedPath = null;
		IResource member;
		switch(childPathString.charAt(0)) {

			default:
				resolvedPath = new Path(childPathString);
			//if(resolvedPath.toFile()!=null && resolvedPath.toFile().exists()) break;

			member = ((IContainer)getResource()).findMember(resolvedPath);

			if(member!=null && member.exists()) break;
			case '/':
			case '\\':
				IPath childPath = new Path(childPathString);

				IPath newPath = childPath.removeFirstSegments(childPath.matchingFirstSegments(webContext));

				member = ((IContainer)getResource()).findMember(newPath);
				//if(member.exists()) return new Path(newPath);

				resolvedPath = newPath;
				if(member!=null && member.exists()) break;

			case '.':
				/* returns a new relative path thats relative to the resource */
				IPath relative=null;
				try {
					relative = fRelativeFile.getFullPath().removeLastSegments(1);
				} catch (Exception ex) {
					/* file usually outside of workspace in this instance */
					return null;
				}
				IPath relRes = getResource().getFullPath();
				if(relRes.isPrefixOf(relative)) {
					IPath amended = relative.removeFirstSegments(relRes.matchingFirstSegments(relative));
					resolvedPath = amended.append(childPathString);
				}
				break;


		}

		return resolvedPath;

	}

	public IPath getPath() {
		if(fRelativeFile!=null) return fRelativeFile.getFullPath().removeLastSegments(1);
		return super.getPath();
	}

	public boolean equals(Object o) {
//		if (this == o)
//			return true;
		if (!(o instanceof DocumentContextFragmentRoot)) return false;

		DocumentContextFragmentRoot other= (DocumentContextFragmentRoot) o;



		boolean equalRelativeFileAndIncludedFileLengths = (this.fRelativeFile!=null && this.fRelativeFile.equals(other.fRelativeFile)) &&
						  this.includedFiles!=null && (other.includedFiles !=null) &&
						  this.includedFiles.length == other.includedFiles.length;

		if(!equalRelativeFileAndIncludedFileLengths) return false;

		/* try some more cases */

		for(int i = 0;i<this.includedFiles.length;i++) {
			if(!(this.includedFiles[i].equals(other.includedFiles[i]))) return false;
		}

//		for(int i = 0;i<this.timeStamps.length;i++) {
//			if(!(this.timeStamps[i].equals( other.timeStamps[i]        )        )) return false;
//		}


		return true;
	}

	public String getElementName() {
		if(fRelativeFile!=null) return this.fRelativeFile.getName();
		return super.getElementName();
	}

	public int hashCode() {
		return fRelativeFile!=null?this.fRelativeFile.hashCode():super.hashCode();
	}

	public boolean isExternal() {
		return false;
	}
	/**
	 * Jars and jar entries are all read only
	 */
	public boolean isReadOnly() {
		return false;
	}

	/**
 * Returns whether the corresponding resource or associated file exists
 */
	protected boolean resourceExists() {
		return true;
	}


	public SearchableEnvironment newSearchableNameEnvironment(WorkingCopyOwner owner) throws JavaScriptModelException {
		/* previously restricted searchable environment to 'this'.  But that removes library entries from search results so going back to global project */
		SearchableEnvironment env =  super.newSearchableNameEnvironment(owner);//new SearchableEnvironment((JavaProject)getJavaProject(),this, owner);
		int includeMask = IJavaScriptSearchScope.SOURCES | IJavaScriptSearchScope.APPLICATION_LIBRARIES | IJavaScriptSearchScope.SYSTEM_LIBRARIES | IJavaScriptSearchScope.REFERENCED_PROJECTS;
		env.nameLookup.setRestrictedAccessRequestor(getRestrictedAccessRequestor());
		((JavaSearchScope)env.searchScope).add((JavaProject)getJavaScriptProject(), includeMask, new HashSet(2));
		return env;
	}

	/*
	 * Returns a new name lookup. This name lookup first looks in the given working copies.
	 */
	public NameLookup newNameLookup(IJavaScriptUnit[] workingCopies) throws JavaScriptModelException {
		this.workingCopies = workingCopies;
		NameLookup lookup = super.newNameLookup(this.workingCopies);
		lookup.setRestrictedAccessRequestor(getRestrictedAccessRequestor());
		return lookup;
		//return ((LookupScopeElementInfo)getElementInfo()).newNameLookup( workingCopies);
	}

	/*
	 * Returns a new name lookup. This name lookup first looks in the working copies of the given owner.
	 */
	public NameLookup newNameLookup(WorkingCopyOwner owner) throws JavaScriptModelException {

		NameLookup lookup =  super.newNameLookup(owner);
		lookup.setRestrictedAccessRequestor(getRestrictedAccessRequestor());
		return lookup;
//
//		JavaModelManager manager = JavaModelManager.getJavaModelManager();
//		IJavaScriptUnit[] workingCopies = owner == null ? null : manager.getWorkingCopies(owner, true/*add primary WCs*/);
//		return newNameLookup(workingCopies);
	}

	public File isValidImport(String importName) {
		IPath filePath = resolveChildPath(importName);
		if(filePath==null) return null;
		File file = filePath.toFile();
		if(file.isFile()) {
			return file;
		}else {
			IFile resolved = null;
			/* since eclipse throws an exception if it doesn't exists (contrary to its API) we have to catch it*/

			try {
				resolved = ((IContainer)getResource()).getFile(new Path(file.getPath()));
			}catch(Exception e) {}
			
			if (resolved == null || !resolved.exists()) {
				return null;
			}

			/* Special case for absolute paths specified with \ and / */
			if( importName.charAt(0)=='\\' || importName.charAt(0)=='/'){
				if (EMPTY_PATH.equals(webContext) || 
							resolved.getFullPath().matchingFirstSegments(webContext) == 0) {
					return null;
				}
			}

			IPath resolvedLocation = resolved.getLocation();
			return resolvedLocation == null ? null : new File(resolved.getLocation().toString());
		}
	}

	public int getKind() throws JavaScriptModelException {
			return IPackageFragmentRoot.K_SOURCE;
	}


	public String toString() {
		StringBuffer me = new StringBuffer("Relative to: " + fRelativeFile.getName() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		me.append("Absolute to: " + webContext + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		me.append("Included File\t\t\tLast Moddified\n"); //$NON-NLS-1$
		for(int i = 0;i<includedFiles.length;i++) {
			me.append(includedFiles[i] /*+ "\t\t\t\t" + timeStamps[i].longValue()*/ + "\n"); //$NON-NLS-1$
		}

		return me.toString();
	}

	public IResource getResource() {
		return absolutePath;
	}


}
