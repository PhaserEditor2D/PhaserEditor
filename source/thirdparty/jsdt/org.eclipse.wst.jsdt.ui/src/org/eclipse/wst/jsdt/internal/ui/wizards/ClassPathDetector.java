/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Matt Chapman, mpchapman@gmail.com - 89977 Make JDT .java agnostic
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.ToolFactory;
import org.eclipse.wst.jsdt.core.compiler.IScanner;
import org.eclipse.wst.jsdt.core.compiler.ITerminalSymbols;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.internal.core.ClasspathEntry;
import org.eclipse.wst.jsdt.internal.core.util.Util;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;

import com.ibm.icu.text.Collator;

public class ClassPathDetector implements IResourceProxyVisitor {
		
	private HashMap fSourceFolders;
	private IProject fProject;
	private IIncludePathEntry[] fResultClasspath;
	private IProgressMonitor fMonitor;
	private IPath[] fExclusionPatterns;
	
	private static class CPSorter implements Comparator {
		private Collator fCollator= Collator.getInstance();
		public int compare(Object o1, Object o2) {
			IIncludePathEntry e1= (IIncludePathEntry) o1;
			IIncludePathEntry e2= (IIncludePathEntry) o2;
			return fCollator.compare(e1.getPath().toString(), e2.getPath().toString());
		}
	}

	public ClassPathDetector(IProject project, IProgressMonitor monitor) throws CoreException {
		this(project, monitor, ClasspathEntry.EXCLUDE_NONE);
	}
	
	public ClassPathDetector(IProject project, IProgressMonitor monitor,
				IPath[] exclusionPatterns) throws CoreException {
		fSourceFolders= new HashMap();
		fProject= project;
		fResultClasspath= null;
		fExclusionPatterns = exclusionPatterns;
		
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
			
		detectClasspath(monitor);
	}
	
	private char[][] convertPatternsToChars(IPath path, IPath[] patterns) {
		int length = patterns.length;
		char[][] patternChars = new char[length][];
		IPath prefixPath = path.removeTrailingSeparator();
		for (int i = 0; i < length; i++) {
			patternChars[i] =
				prefixPath.append(patterns[i]).toString().toCharArray();
		}
		return patternChars;
	}
	
	/**
	 * Method detectClasspath.
	 * @param monitor The progress monitor (not null)
	 * @throws CoreException 
	 */
	private void detectClasspath(IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(NewWizardMessages.ClassPathDetector_operation_description, 4); 
			
			fMonitor= monitor;
			fProject.accept(this, IResource.NONE);
			monitor.worked(1);
			
			ArrayList cpEntries= new ArrayList();

			detectSourceFolders(cpEntries);
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
		
			monitor.worked(1);

			if (cpEntries.isEmpty()) {
				return;
			}
			
			IIncludePathEntry[] jreEntries= PreferenceConstants.getDefaultJRELibrary();
			for (int i= 0; i < jreEntries.length; i++) {
				cpEntries.add(jreEntries[i]);
			}

			IIncludePathEntry[] entries= (IIncludePathEntry[]) cpEntries.toArray(new IIncludePathEntry[cpEntries.size()]);
			if (!JavaScriptConventions.validateClasspath(JavaScriptCore.create(fProject), entries).isOK()) {
				return;
			}

			fResultClasspath= entries;
		} finally {
			monitor.done();
		}
	}

	private void detectSourceFolders(ArrayList resEntries) {
		ArrayList res= new ArrayList();
		Set sourceFolderSet= fSourceFolders.keySet();
		for (Iterator iter= sourceFolderSet.iterator(); iter.hasNext();) {
			IPath path= (IPath) iter.next();
			char[][] excludedPatternChars = convertPatternsToChars(path, fExclusionPatterns);
			if (!Util.isExcluded(path, null, excludedPatternChars, true)) {
				Set<IPath> excluded= new HashSet<IPath>();
				for (Iterator inner= sourceFolderSet.iterator(); inner.hasNext();) {
					IPath other= (IPath) inner.next();
					if (!path.equals(other) && path.isPrefixOf(other)) {
						IPath pathToExclude= other.removeFirstSegments(path.segmentCount()).addTrailingSeparator();
						excluded.add(pathToExclude);
					}
				}
				excluded.addAll(Arrays.asList(fExclusionPatterns));
				IPath[] excludedPaths= (IPath[]) excluded.toArray(new IPath[excluded.size()]);
				IIncludePathEntry entry= JavaScriptCore.newSourceEntry(path, excludedPaths);
				res.add(entry);
			}
		}
		Collections.sort(res, new CPSorter());
		resEntries.addAll(res);
	}

	private void visitCompilationUnit(IFile file) {
		IJavaScriptUnit cu= JavaScriptCore.createCompilationUnitFrom(file);
		if (cu != null) {
			IJavaScriptUnit workingCopy= null;
			try {
				workingCopy= cu.getWorkingCopy(null);
				IPath relPath= getPackagePath(workingCopy.getSource());
				IPath packPath= file.getParent().getFullPath();
				String cuName= file.getName();
				if (relPath == null) {
					addToMap(fSourceFolders, packPath, new Path(cuName));
				} else {
					IPath folderPath= getFolderPath(packPath, relPath);
					if (folderPath != null) {
						addToMap(fSourceFolders, folderPath, relPath.append(cuName));
					}					
				}				
			} catch (JavaScriptModelException e) {
				// ignore
			} catch (InvalidInputException e) {
				// ignore
			} finally {
				if (workingCopy != null) {
					try {
						workingCopy.discardWorkingCopy();
					} catch (JavaScriptModelException ignore) {
					}
				}
			}
		}
	}
	
	private IPath getPackagePath(String source) throws InvalidInputException {
		IScanner scanner= ToolFactory.createScanner(false, false, false, false);
		scanner.setSource(source.toCharArray());
		scanner.resetTo(0, source.length() - 1);
		int tok= scanner.getNextToken();
		if (tok != ITerminalSymbols.TokenNamepackage) {
			return null;
		}
		IPath res= Path.EMPTY;
		do {
			tok= scanner.getNextToken();
			if (tok == ITerminalSymbols.TokenNameIdentifier) {
				res= res.append(new String(scanner.getCurrentTokenSource()));
			} else {
				return res;
			}
			tok= scanner.getNextToken();
		} while (tok == ITerminalSymbols.TokenNameDOT);
		
		return res;
	}
	
	
	private void addToMap(HashMap map, IPath folderPath, IPath relPath) {
		List list= (List) map.get(folderPath);
		if (list == null) {
			list= new ArrayList(50);
			map.put(folderPath, list);
		}		
		list.add(relPath);
	}

	private IPath getFolderPath(IPath packPath, IPath relpath) {
		int remainingSegments= packPath.segmentCount() - relpath.segmentCount();
		if (remainingSegments >= 0) {
			IPath common= packPath.removeFirstSegments(remainingSegments);
			if (common.equals(relpath)) {
				return packPath.uptoSegment(remainingSegments);
			}
		}
		return null;
	}
	
	private boolean isValidCUName(String name) {
		return !JavaScriptConventions.validateCompilationUnitName(name).matches(IStatus.ERROR);
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceProxyVisitor#visit(org.eclipse.core.resources.IResourceProxy)
	 */
	public boolean visit(IResourceProxy proxy) {
		if (fMonitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		
		if (proxy.getType() == IResource.FILE) {
			String name= proxy.getName();
			if (isValidCUName(name))
				visitCompilationUnit((IFile) proxy.requestResource());
			return false;
		}
		return true;
	}
		
	public IIncludePathEntry[] getClasspath() {
		if (fResultClasspath == null)
			return new IIncludePathEntry[0];
		return fResultClasspath;
	}
}