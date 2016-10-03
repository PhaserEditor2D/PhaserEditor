/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     bug 242694 -  Michael Spector <spektom@gmail.com>     
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.search.indexing;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.search.SearchDocument;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchParticipant;
import org.eclipse.wst.jsdt.internal.compiler.SourceElementParser;
import org.eclipse.wst.jsdt.internal.compiler.util.SuffixConstants;
import org.eclipse.wst.jsdt.internal.compiler.util.Util;
import org.eclipse.wst.jsdt.internal.core.BasicCompilationUnit;
import org.eclipse.wst.jsdt.internal.core.JavaModelManager;
import org.eclipse.wst.jsdt.internal.core.Logger;
import org.eclipse.wst.jsdt.internal.core.index.Index;
import org.eclipse.wst.jsdt.internal.core.search.JavaSearchDocument;
import org.eclipse.wst.jsdt.internal.oaametadata.LibraryAPIs;
import org.eclipse.wst.jsdt.internal.oaametadata.MetadataReader;
import org.eclipse.wst.jsdt.internal.oaametadata.MetadataSourceElementNotifier;

/**
 * A SourceIndexer indexes java files using a java parser. The following items are indexed:
 * Declarations of:
 * - Classes<br>
 * - Interfaces; <br>
 * - Methods;<br>
 * - Fields;<br>
 * References to:
 * - Methods (with number of arguments); <br>
 * - Fields;<br>
 * - Types;<br>
 * - Constructors.
 */
public class SourceIndexer extends AbstractIndexer implements SuffixConstants {

	public SourceIndexer(SearchDocument document) {
		super(document);
	}
	public void indexDocument() {
		// Create a new Parser
		SourceIndexerRequestor requestor = new SourceIndexerRequestor(this);
		String documentPath = this.document.getPath();
		SourceElementParser parser = ((InternalSearchDocument) this.document).parser;
		if (parser == null) {
			IPath path = new Path(documentPath);
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0));
			parser = JavaModelManager.getJavaModelManager().indexManager.getSourceElementParser(JavaScriptCore.create(project), requestor);
		} else {
			parser.setRequestor(requestor);
		}

		// Launch the parser
		char[] source = null;
		char[] name = null;
		try {
			source = document.getCharContents();
			name = documentPath.toCharArray();
		} catch(Exception e){
			// ignore
		}
		if (source == null || name == null) return; // could not retrieve document info (e.g. resource was discarded)
		String pkgName=((JavaSearchDocument)document).getPackageName();
		char [][]packageName=null;
		if (pkgName!=null)
		{
			packageName=new char[1][];
			packageName[0]=pkgName.toCharArray();
		}
		BasicCompilationUnit compilationUnit = new BasicCompilationUnit(source, packageName, new String(name));
		try {
			parser.parseCompilationUnit(compilationUnit, true/*full parse*/);
		} catch (Exception e) {
			Logger.logException("Error while indexing document", e);
		}
	}
	public void indexMetadata() {
		// Create a new Parser
		SourceIndexerRequestor requestor = new SourceIndexerRequestor(this);
		String documentPath = this.document.getPath();

		
		// Launch the parser
		char[] source = null;
		char[] name = null;
		try {
			source = document.getCharContents();
			name = documentPath.toCharArray();
		} catch(Exception e){
			// ignore
		}
		if (source == null || name == null) return; // could not retrieve document info (e.g. resource was discarded)
		String pkgName=((JavaSearchDocument)document).getPackageName();
		char [][]packageName=null;
		if (pkgName!=null)
		{
			packageName=new char[1][];
			packageName[0]=pkgName.toCharArray();
		}
		
		LibraryAPIs apis = MetadataReader.readAPIsFromString(new String(source),documentPath);
		new MetadataSourceElementNotifier(apis,requestor).notifyRequestor();
		
	}
	public void indexArchive() {
		/*
		 * index the individual documents in the archive into the single index
		 * file for the archive's path
		 */
		IPath jarPath = new Path(this.document.getPath());

		File file = new File(jarPath.toOSString());

		if (file.isFile()) {
			IndexManager indexManager = JavaModelManager.getJavaModelManager().getIndexManager();
			Index index = indexManager.getIndexForUpdate(jarPath, false /*don't reuse index file*/, true /*create if none*/);
			SearchParticipant participant = SearchEngine.getDefaultSearchParticipant();
			ZipFile zip = null;
			try {
				zip = new ZipFile(file);
				for (Enumeration e = zip.entries(); e.hasMoreElements();) {
					// iterate each entry to index it
					ZipEntry ze = (ZipEntry) e.nextElement();
					if (Util.isClassFileName(ze.getName())) {
						final byte[] classFileBytes = org.eclipse.wst.jsdt.internal.compiler.util.Util.getZipEntryByteContent(ze, zip);
						JavaSearchDocument entryDocument = new JavaSearchDocument(ze, jarPath, ByteBuffer.wrap(classFileBytes).asCharBuffer().array(), participant);
						indexManager.indexDocument(entryDocument, participant, index, jarPath);
					}
				}
				indexManager.saveIndex(index);
			}
			catch (ZipException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			finally {
				if (zip != null) {
					try {
						zip.close();
					}
					catch (IOException e) {
					}
					if(index != null) {
					}
				}
			}
		}
	}
}
