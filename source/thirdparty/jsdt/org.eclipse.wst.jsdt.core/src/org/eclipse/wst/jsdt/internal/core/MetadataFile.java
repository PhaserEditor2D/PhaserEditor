/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.CompletionRequestor;
import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.IBufferFactory;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IProblemRequestor;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.internal.core.util.MementoTokenizer;
import org.eclipse.wst.jsdt.internal.core.util.Util;
import org.eclipse.wst.jsdt.internal.oaametadata.ClassData;
import org.eclipse.wst.jsdt.internal.oaametadata.DocumentedElement;
import org.eclipse.wst.jsdt.internal.oaametadata.LibraryAPIs;
import org.eclipse.wst.jsdt.internal.oaametadata.MetadataReader;
import org.eclipse.wst.jsdt.internal.oaametadata.MetadataSourceElementNotifier;
import org.xml.sax.InputSource;

public class MetadataFile extends Openable implements 
	IClassFile,	org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit, IVirtualParent
	{
	
	protected String name;
	protected IPath filePath;
	private static final IField[] NO_FIELDS = new IField[0];
	private static final IFunction[] NO_METHODS = new IFunction[0];
	private LibraryAPIs apis=null;
	protected BinaryType binaryType = null;
	
	
	protected MetadataFile(PackageFragment parent, String path) {
		super(parent);
		this.filePath = Path.fromOSString(path);
		if (filePath.getFileExtension()!=null)
		{
			String lastSegment = filePath.lastSegment();
			this.name=lastSegment.substring(0,lastSegment.length()-(filePath.getFileExtension().length()+1));
		}
		else
			this.name=path;
	}

	protected boolean buildStructure(OpenableElementInfo info,
			IProgressMonitor pm, Map newElements, IResource underlyingResource)
			throws JavaScriptModelException {
		if (underlyingResource != null && !underlyingResource.isAccessible()) {
			throw newNotPresentException();
		}



		CompilationUnitElementInfo unitInfo = new CompilationUnitElementInfo();

		// get buffer contents

		// generate structure and compute syntax problems if needed
		CompilationUnitStructureRequestor requestor = new CompilationUnitStructureRequestor(this, unitInfo, newElements);

		boolean createAST;
//		HashMap problems;
		if (info instanceof ASTHolderCUInfo) {
			ASTHolderCUInfo astHolder = (ASTHolderCUInfo) info;
			createAST = astHolder.astLevel != IJavaScriptUnit.NO_AST;
//			problems = astHolder.problems;
		} else {
			createAST = false;
//			problems = null;
		}


			new MetadataSourceElementNotifier(getAPIs(),requestor).notifyRequestor();

 

		// update timestamp (might be IResource.NULL_STAMP if original does not exist)
		if (underlyingResource == null) {
			underlyingResource = getResource();
		}
		// underlying resource is null in the case of a working copy on a class file in a jar
		if (underlyingResource != null)
			unitInfo.timestamp = ((IFile)underlyingResource).getModificationStamp();

		// compute other problems if needed
//		CompilationUnitDeclaration compilationUnitDeclaration = null;
		info.setChildren(unitInfo.children);
		try {

			if (createAST) {
//				int astLevel = ((ASTHolderCUInfo) info).astLevel;
//				org.eclipse.wst.jsdt.core.dom.JavaScriptUnit cu = AST.convertCompilationUnit(astLevel, unit, contents, options, computeProblems, this, pm);
//				((ASTHolderCUInfo) info).ast = cu;
				throw new RuntimeException("Implement this"); //$NON-NLS-1$
			}
		} finally {

		}
		
		return true;

	}
	
	public LibraryAPIs getAPIs() 
	{
		if (apis==null)
		{
			IFile file = (IFile) getResource();
				try {
					apis = MetadataReader.readAPIsFromStream(new InputSource(file.getContents()),file.getLocation().toOSString());
					apis.fileName=file.getFullPath().toPortableString().toCharArray();
				} catch (Exception e) {
					Util.log(e, "error reading metadata");
					apis=new LibraryAPIs();
					apis.fileName=file.getFullPath().toPortableString().toCharArray();
				}
		}
		return apis;
	}

	public IJavaScriptElement getHandleFromMemento(String token,
			MementoTokenizer memento, WorkingCopyOwner owner) {
		switch (token.charAt(0)) {
		case JEM_TYPE:
			if (!memento.hasMoreTokens()) return this;
			String typeName = memento.nextToken();
			JavaElement type = new BinaryType(this, typeName);
			return type.getHandleFromMemento(memento, owner);
	}
	return null;
	}

	protected char getHandleMementoDelimiter() {
		return JavaElement.JEM_METADATA;

	}

	public IJavaScriptUnit becomeWorkingCopy(
			IProblemRequestor problemRequestor, WorkingCopyOwner owner,
			IProgressMonitor monitor) throws JavaScriptModelException {
		return null;
	}

	public byte[] getBytes() throws JavaScriptModelException {
		IFile file = (IFile) getResource();
		return Util.getResourceContentsAsByteArray(file);
	}

	public IType getType() {
		if (this.binaryType == null) {
			this.binaryType = new BinaryType(this, getTypeName());
		}
		return this.binaryType;
	}

	public IType[] getTypes() throws JavaScriptModelException {
		ArrayList list = getChildrenOfType(TYPE);
		IType[] array= new IType[list.size()];
		list.toArray(array);
		return array;
	}

	public IJavaScriptElement getWorkingCopy(IProgressMonitor monitor,
			IBufferFactory factory) throws JavaScriptModelException {
		return null;
	}

	public boolean isClass() throws JavaScriptModelException {
		return true;
	}

	public boolean isInterface() throws JavaScriptModelException {
		return false;
	}

	public IType findPrimaryType() {
		return null;
	}

	public IJavaScriptElement getElementAt(int position) throws JavaScriptModelException {
		return null;
	}

	public IJavaScriptUnit getWorkingCopy(WorkingCopyOwner owner,
			IProgressMonitor monitor) throws JavaScriptModelException {
		return null;
	}

	public int getElementType() {
		return CLASS_FILE;
	}

	public IPath getPath() {
		return this.filePath;
	}

	public IResource getResource() {
		return ((IContainer)this.getParent().getResource()).getFile(new Path(this.getElementName()));
	}

	public String getSource() throws JavaScriptModelException {
		IBuffer buffer = super.getBuffer();
		if (buffer == null) {
			return null;
		}
		return buffer.getContents();
	}

	public ISourceRange getSourceRange() throws JavaScriptModelException {
		return null;
	}

	public void codeComplete(int offset, CompletionRequestor requestor)
			throws JavaScriptModelException {

	}

	public void codeComplete(int offset, CompletionRequestor requestor,
			WorkingCopyOwner owner) throws JavaScriptModelException {

	}

	public IJavaScriptElement[] codeSelect(int offset, int length)
			throws JavaScriptModelException {
		return null;
	}

	public IJavaScriptElement[] codeSelect(int offset, int length,
			WorkingCopyOwner owner) throws JavaScriptModelException {
		return null;
	}

	public IField getField(String fieldName) {
		return new SourceField(this, fieldName);
	}

	public IField[] getFields() throws JavaScriptModelException {
		ArrayList list = getChildrenOfType(FIELD);
		int size;
		if ((size = list.size()) == 0) {
			return NO_FIELDS;
		} else {
			IField[] array= new IField[size];
			list.toArray(array);
			return array;
		}
	}

	public IFunction getFunction(String selector, String[] parameterTypeSignatures) {
		return new SourceMethod(this, selector, parameterTypeSignatures);
	}

	/**
	 * @deprecated Use {@link #getFunctions()} instead
	 */
	public IFunction[] getMethods() throws JavaScriptModelException {
		return getFunctions();
	}

	public IFunction[] getFunctions() throws JavaScriptModelException {
		ArrayList list = getChildrenOfType(METHOD);
		int size;
		if ((size = list.size()) == 0) {
			return NO_METHODS;
		} else {
			IFunction[] array= new IFunction[size];
			list.toArray(array);
			return array;
		}
	}

	public IType getType(String typeName) {
		return new SourceType(this, typeName);
	}

	public char[] getContents() {
		char [] chars=null;
		try {
			chars=org.eclipse.wst.jsdt.internal.compiler.util.Util.getFileCharContent(new File(filePath.toOSString()),null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return chars;
	}

	public String getInferenceID() {
		return null;
	}

	public char[] getMainTypeName() {
		return null;
	}

	public char[][] getPackageName() {
		return new char[][] {getParent().getElementName().toCharArray()};
	}

	public char[] getFileName() {
		return this.filePath!=null?this.filePath.toString().toCharArray():getElementName().toCharArray();
	}

	public JsGlobalScopeContainerInitializer getContainerInitializer() {
		JsGlobalScopeContainerInitializer init = ((IVirtualParent)parent).getContainerInitializer();
		return init;
	}

	public String getElementName() {
		return filePath.lastSegment();
	}

	
	protected boolean resourceExists() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		if (workspace == null) return false; // workaround for http://bugs.eclipse.org/bugs/show_bug.cgi?id=34069
		Object me = JavaModel.getTarget(workspace.getRoot(), this.getPath(), true) ;
		if(me!=null) return true;
		me = JavaModel.getTarget(workspace.getRoot(), this.getPath().makeRelative(), true);
		return (me!=null);

	}
	
	public DocumentedElement getDocumentation(IMember member)
	{
		IJavaScriptElement parent = member.getParent();
		String elementName = member.getElementName();
		LibraryAPIs apis = getAPIs();
		switch (member.getElementType()) {
		case IJavaScriptElement.TYPE:
			return apis.getClass(elementName);

		case IJavaScriptElement.METHOD:
			if (parent.equals(this))
				return apis.getGlobalMethod(elementName);
			ClassData clazz = apis.getClass(parent.getElementName());
			if (clazz!=null)
				return clazz.getMethod(elementName);
			return null;

		case IJavaScriptElement.FIELD:
			if (parent.equals(this))
				return apis.getGlobalVar(elementName);
			 clazz = apis.getClass(parent.getElementName());
			if (clazz!=null)
				return clazz.getField(elementName);
			return null;
 
		}
		return null;
	}
	
	public String getTypeName() {
		// Internal class file name doesn't contain ".class" file extension
		return this.name;
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof MetadataFile)) return false;
		MetadataFile other = (MetadataFile) o;
		return this.name.equals(other.name) && this.parent.equals(other.parent);
	}
 
	 
}
