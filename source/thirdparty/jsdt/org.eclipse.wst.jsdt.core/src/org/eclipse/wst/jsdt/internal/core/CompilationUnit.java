/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alex Smirnoff (alexsmr@sympatico.ca) - part of the changes to support Java-like extension
 *                                                            (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=71460)
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.wst.jsdt.core.CompletionRequestor;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.IBufferFactory;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IImportContainer;
import org.eclipse.wst.jsdt.core.IImportDeclaration;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IProblemRequestor;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeRoot;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.core.LibrarySuperType;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.internal.compiler.IProblemFactory;
import org.eclipse.wst.jsdt.internal.compiler.SourceElementParser;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.jsdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.wst.jsdt.internal.compiler.util.SuffixConstants;
import org.eclipse.wst.jsdt.internal.core.util.MementoTokenizer;
import org.eclipse.wst.jsdt.internal.core.util.Messages;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * @see IJavaScriptUnit
 */
public class CompilationUnit extends Openable implements IJavaScriptUnit, org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit, SuffixConstants, IVirtualParent {
	/**
	 * Internal synonynm for deprecated constant AST.JSL2
	 * to alleviate deprecation warnings.
	 * @deprecated
	 */
	/*package*/ static final int JLS2_INTERNAL = AST.JLS2;

	private static final IImportDeclaration[] NO_IMPORTS = new IImportDeclaration[0];
	protected String name;
	public WorkingCopyOwner owner;
	public String superTypeName;
	
	/**
	 * <p>
	 * <code>true</code> if currently making this unit consistent,
	 * <code>false</code> otherwise.
	 * </p>
	 * <p>
	 * Used to prevent
	 * {@link #makeConsistent(int, boolean, int, HashMap, IProgressMonitor)}
	 * from being called in an infinite loop.
	 * </p>
	 * 
	 * @see #makeConsistent(int, boolean, int, HashMap, IProgressMonitor)
	 */
	private volatile boolean fIsMakingConsistent;

	/**
	 * Constructs a handle to a compilation unit with the given name in the
	 * specified package for the specified owner
	 */
	public CompilationUnit(PackageFragment parent, String name,String superTypeName, WorkingCopyOwner owner) {
		super(parent);
		this.name = name;
		this.owner = owner;
		this.superTypeName = superTypeName;
		this.fIsMakingConsistent = false;
	}
	
	public CompilationUnit(PackageFragment parent, String name, WorkingCopyOwner owner) {
		this(parent,name,null,owner);
	}
	
	/*
	 * @see IJavaScriptUnit#becomeWorkingCopy(IProblemRequestor, IProgressMonitor)
	 */
	public void becomeWorkingCopy(IProblemRequestor problemRequestor, IProgressMonitor monitor) throws JavaScriptModelException {
		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		JavaModelManager.PerWorkingCopyInfo perWorkingCopyInfo = manager.getPerWorkingCopyInfo(this, false/*don't create*/, true /*record usage*/, null/*no problem requestor needed*/);
		if (perWorkingCopyInfo == null) {
			// close cu and its children
			close();
	
			BecomeWorkingCopyOperation operation = new BecomeWorkingCopyOperation(this, problemRequestor);
			operation.runOperation(monitor);
		}
	}
	/*
	 * @see IJavaScriptUnit#becomeWorkingCopy(IProgressMonitor)
	 */
	public void becomeWorkingCopy(IProgressMonitor monitor) throws JavaScriptModelException {
		IProblemRequestor requestor = this.owner == null ? null : this.owner.getProblemRequestor(this);
		becomeWorkingCopy(requestor, monitor);
	}
	protected boolean buildStructure(OpenableElementInfo info, final IProgressMonitor pm, Map newElements, IResource underlyingResource) throws JavaScriptModelException {
	
		// check if this compilation unit can be opened
		if (!isWorkingCopy()) { // no check is done on root kind or exclusion pattern for working copies
			IStatus status = validateCompilationUnit(underlyingResource);
			if (!status.isOK()) throw newJavaModelException(status);
		}
	
		// prevents reopening of non-primary working copies (they are closed when they are discarded and should not be reopened)
		if (!isPrimary() && getPerWorkingCopyInfo() == null) {
			throw newNotPresentException();
		}
	
		CompilationUnitElementInfo unitInfo = (CompilationUnitElementInfo) info;
	
		// get buffer contents
		IBuffer buffer = getBufferManager().getBuffer(CompilationUnit.this);
		if (buffer == null) {
			buffer = openBuffer(pm, unitInfo); // open buffer independently from the info, since we are building the info
		}
		final char[] contents;
		if (buffer == null) {
			contents = CharOperation.NO_CHAR ;
		} else {
			char[] characters = buffer.getCharacters();
			contents = characters == null ? CharOperation.NO_CHAR : characters;
		}
	
		// generate structure and compute syntax problems if needed
		CompilationUnitStructureRequestor requestor = new CompilationUnitStructureRequestor(this, unitInfo, newElements);
		JavaModelManager.PerWorkingCopyInfo perWorkingCopyInfo = getPerWorkingCopyInfo();
		IJavaScriptProject project = getJavaScriptProject();
	
		boolean createAST;
		boolean resolveBindings;
		int reconcileFlags;
		HashMap problems;
		if (info instanceof ASTHolderCUInfo) {
			ASTHolderCUInfo astHolder = (ASTHolderCUInfo) info;
			createAST = astHolder.astLevel != NO_AST;
			resolveBindings = astHolder.resolveBindings;
			reconcileFlags = astHolder.reconcileFlags;
			problems = astHolder.problems;
		} else {
			createAST = false;
			resolveBindings = false;
			reconcileFlags = 0;
			problems = null;
		}
	
		boolean computeProblems = perWorkingCopyInfo != null && perWorkingCopyInfo.isActive() && project != null && JavaProject.hasJavaNature(project.getProject());
		IProblemFactory problemFactory = new DefaultProblemFactory();
		Map options = project == null ? JavaScriptCore.getOptions() : project.getOptions(true);
		if (!computeProblems) {
			// disable task tags checking to speed up parsing
			options.put(JavaScriptCore.COMPILER_TASK_TAGS, ""); //$NON-NLS-1$
		}
		SourceElementParser parser = new SourceElementParser(
			requestor,
			problemFactory,
			new CompilerOptions(options),
			true/*report local declarations*/,
			!createAST /*optimize string literals only if not creating a DOM AST*/);
		parser.reportOnlyOneSyntaxError = !computeProblems;
		parser.setMethodsFullRecovery(true);
		parser.setStatementsRecovery((reconcileFlags & IJavaScriptUnit.ENABLE_STATEMENTS_RECOVERY) != 0);
	
		requestor.parser = parser;
		CompilationUnitDeclaration unit = parser.parseCompilationUnit(
			new org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit() {
				public char[] getContents() {
					return contents;
				}
				public char[] getMainTypeName() {
					return CompilationUnit.this.getMainTypeName();
				}
				public char[][] getPackageName() {
					return CompilationUnit.this.getPackageName();
				}
				public char[] getFileName() {
					return CompilationUnit.this.getFileName();
				}
				public LibrarySuperType getCommonSuperType() {
					return CompilationUnit.this.getCommonSuperType();
				}
				public String getInferenceID() {
					return CompilationUnit.this.getInferenceID();
				}
	
	
			},
			true /*full parse to find local elements*/);
	
		// update timestamp (might be IResource.NULL_STAMP if original does not exist)
		if (underlyingResource == null) {
			underlyingResource = getResource();
		}
		// underlying resource is null in the case of a working copy on a class file in a jar
		if (underlyingResource != null)
			unitInfo.timestamp = ((IFile)underlyingResource).getModificationStamp();
	
		// compute other problems if needed
		CompilationUnitDeclaration compilationUnitDeclaration = null;
		try {
			if (computeProblems) {
				if (problems == null) {
					// report problems to the problem requestor
					problems = new HashMap();
					compilationUnitDeclaration = CompilationUnitProblemFinder.process(unit, this, contents, parser, this.owner, problems, createAST, reconcileFlags, pm);
					try {
						perWorkingCopyInfo.beginReporting();
						for (Iterator iteraror = problems.values().iterator(); iteraror.hasNext();) {
							CategorizedProblem[] categorizedProblems = (CategorizedProblem[]) iteraror.next();
							if (categorizedProblems == null) continue;
							for (int i = 0, length = categorizedProblems.length; i < length; i++) {
								perWorkingCopyInfo.acceptProblem(categorizedProblems[i]);
							}
						}
					} finally {
						perWorkingCopyInfo.endReporting();
					}
				} else {
					// collect problems
					compilationUnitDeclaration = CompilationUnitProblemFinder.process(unit, this, contents, parser, this.owner, problems, createAST, reconcileFlags, pm);
				}
			}
	
			if (createAST) {
				int astLevel = ((ASTHolderCUInfo) info).astLevel;
				org.eclipse.wst.jsdt.core.dom.JavaScriptUnit cu = AST.convertCompilationUnit(astLevel, unit, contents, options, computeProblems, this, reconcileFlags, pm);
				((ASTHolderCUInfo) info).ast = cu;
			}
		} finally {
		    if (compilationUnitDeclaration != null) {
		        compilationUnitDeclaration.cleanUp();
		        if (compilationUnitDeclaration.scope!=null)
		        	compilationUnitDeclaration.scope.cleanup();
		    }
		}
	
		return unitInfo.isStructureKnown();
	}
	/*
	 * @see Openable#canBeRemovedFromCache
	 */
	public boolean canBeRemovedFromCache() {
		if (getPerWorkingCopyInfo() != null) return false; // working copies should remain in the cache until they are destroyed
		return super.canBeRemovedFromCache();
	}
	/*
	 * @see Openable#canBufferBeRemovedFromCache
	 */
	public boolean canBufferBeRemovedFromCache(IBuffer buffer) {
		if (getPerWorkingCopyInfo() != null) return false; // working copy buffers should remain in the cache until working copy is destroyed
		return super.canBufferBeRemovedFromCache(buffer);
	}/*
	 * @see org.eclipse.wst.jsdt.core.IOpenable#close
	 */
	public void close() throws JavaScriptModelException {
		if (getPerWorkingCopyInfo() != null) return; // a working copy must remain opened until it is discarded
		super.close();
	}
	/*
	 * @see Openable#closing
	 */
	protected void closing(Object info) {
		if (getPerWorkingCopyInfo() == null) {
			super.closing(info);
		} // else the buffer of a working copy must remain open for the lifetime of the working copy
	}
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.ICodeAssist#codeComplete(int, org.eclipse.wst.jsdt.core.CompletionRequestor)
	 */
	public void codeComplete(int offset, CompletionRequestor requestor) throws JavaScriptModelException {
		codeComplete(offset, requestor, DefaultWorkingCopyOwner.PRIMARY);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.ICodeAssist#codeComplete(int, org.eclipse.wst.jsdt.core.CompletionRequestor, org.eclipse.wst.jsdt.core.WorkingCopyOwner)
	 */
	public void codeComplete(int offset, CompletionRequestor requestor, WorkingCopyOwner workingCopyOwner) throws JavaScriptModelException {
		codeComplete(this, isWorkingCopy() ? (org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit) getOriginalElement() : this, offset, requestor, workingCopyOwner);
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.ICodeAssist#codeSelect(int, int)
	 */
	public IJavaScriptElement[] codeSelect(int offset, int length) throws JavaScriptModelException {
		return codeSelect(offset, length, DefaultWorkingCopyOwner.PRIMARY);
	}
	/**
	 * @see org.eclipse.wst.jsdt.core.ICodeAssist#codeSelect(int, int, WorkingCopyOwner)
	 */
	public IJavaScriptElement[] codeSelect(int offset, int length, WorkingCopyOwner workingCopyOwner) throws JavaScriptModelException {
		return super.codeSelect(this, offset, length, workingCopyOwner);
	}
	/**
	 * @see org.eclipse.wst.jsdt.core.IWorkingCopy#commit(boolean, IProgressMonitor)
	 * @deprecated
	 */
	public void commit(boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
		commitWorkingCopy(force, monitor);
	}
	/**
	 * @see IJavaScriptUnit#commitWorkingCopy(boolean, IProgressMonitor)
	 */
	public void commitWorkingCopy(boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
		CommitWorkingCopyOperation op= new CommitWorkingCopyOperation(this, force);
		op.runOperation(monitor);
	}
	/**
	 * @see org.eclipse.wst.jsdt.core.ISourceManipulation#copy(IJavaScriptElement, IJavaScriptElement, String, boolean, IProgressMonitor)
	 */
	public void copy(IJavaScriptElement container, IJavaScriptElement sibling, String rename, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
		if (container == null) {
			throw new IllegalArgumentException(Messages.operation_nullContainer);
		}
		IJavaScriptElement[] elements = new IJavaScriptElement[] {this};
		IJavaScriptElement[] containers = new IJavaScriptElement[] {container};
		String[] renamings = null;
		if (rename != null) {
			renamings = new String[] {rename};
		}
		getJavaScriptModel().copy(elements, containers, null, renamings, force, monitor);
	}
	/**
	 * Returns a new element info for this element.
	 */
	protected Object createElementInfo() {
		return new CompilationUnitElementInfo();
	}
	/**
	 * @see IJavaScriptUnit#createImport(String, IJavaScriptElement, IProgressMonitor)
	 */
	public IImportDeclaration createImport(String importName, IJavaScriptElement sibling, IProgressMonitor monitor) throws JavaScriptModelException {
		return createImport(importName, sibling, Flags.AccDefault, monitor);
	}
	
	/**
	 * @see IJavaScriptUnit#createImport(String, IJavaScriptElement, int, IProgressMonitor)
	 * @since 3.0
	 */
	public IImportDeclaration createImport(String importName, IJavaScriptElement sibling, int flags, IProgressMonitor monitor) throws JavaScriptModelException {
		CreateImportOperation op = new CreateImportOperation(importName, this, flags);
		if (sibling != null) {
			op.createBefore(sibling);
		}
		op.runOperation(monitor);
		return getImport(importName);
	}
	
	/**
	 * @see IJavaScriptUnit#createType(String, IJavaScriptElement, boolean, IProgressMonitor)
	 */
	public IType createType(String content, IJavaScriptElement sibling, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
		if (!exists()) {
			//autogenerate this compilation unit
			IPackageFragment pkg = (IPackageFragment) getParent();
			String source = ""; //$NON-NLS-1$
			if (!pkg.isDefaultPackage()) {
				//not the default package...add the package declaration
				String lineSeparator = Util.getLineSeparator(null/*no existing source*/, getJavaScriptProject());
				source = "package " + pkg.getElementName() + ";"  + lineSeparator + lineSeparator; //$NON-NLS-1$ //$NON-NLS-2$
			}
			CreateCompilationUnitOperation op = new CreateCompilationUnitOperation(pkg, this.name, source, force);
			op.runOperation(monitor);
		}
		CreateTypeOperation op = new CreateTypeOperation(this, content, force);
		if (sibling != null) {
			op.createBefore(sibling);
		}
		op.runOperation(monitor);
		return (IType) op.getResultElements()[0];
	}
	/**
	 * @see org.eclipse.wst.jsdt.core.ISourceManipulation#delete(boolean, IProgressMonitor)
	 */
	public void delete(boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
		IJavaScriptElement[] elements= new IJavaScriptElement[] {this};
		getJavaScriptModel().delete(elements, force, monitor);
	}
	/**
	 * @see org.eclipse.wst.jsdt.core.IWorkingCopy#destroy()
	 * @deprecated
	 */
	public void destroy() {
		try {
			discardWorkingCopy();
		} catch (JavaScriptModelException e) {
			if (JavaModelManager.VERBOSE)
				e.printStackTrace();
		}
	}
	
	/*
	 * @see IJavaScriptUnit#discardWorkingCopy
	 */
	public void discardWorkingCopy() throws JavaScriptModelException {
		// discard working copy and its children
		DiscardWorkingCopyOperation op = new DiscardWorkingCopyOperation(this);
		op.runOperation(null);
	}
	
	/**
	 * Returns true if this handle represents the same Java element
	 * as the given handle.
	 *
	 * @see Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof CompilationUnit)) return false;
		CompilationUnit other = (CompilationUnit)obj;
		return this.owner.equals(other.owner) && super.equals(obj);
	}
	public boolean exists() {
		// working copy always exists in the model until it is gotten rid of (even if not on classpath)
		if (getPerWorkingCopyInfo() != null) return true;
	
		// if not a working copy, it exists only if it is a primary compilation unit
		return isPrimary() && validateCompilationUnit(getResource()).isOK();
	}
	
	/**
	 * @see IJavaScriptUnit#findElements(IJavaScriptElement)
	 */
	public IJavaScriptElement[] findElements(IJavaScriptElement element) {
		ArrayList children = new ArrayList();
		while (element != null && element.getElementType() != IJavaScriptElement.JAVASCRIPT_UNIT) {
			children.add(element);
			element = element.getParent();
		}
		if (element == null) return null;
		IJavaScriptElement currentElement = this;
		for (int i = children.size()-1; i >= 0; i--) {
			SourceRefElement child = (SourceRefElement)children.get(i);
			switch (child.getElementType()) {
				case IJavaScriptElement.IMPORT_CONTAINER:
					currentElement = ((IJavaScriptUnit)currentElement).getImportContainer();
					break;
				case IJavaScriptElement.IMPORT_DECLARATION:
					currentElement = ((IImportContainer)currentElement).getImport(child.getElementName());
					break;
				case IJavaScriptElement.TYPE:
					switch (currentElement.getElementType()) {
						case IJavaScriptElement.JAVASCRIPT_UNIT:
							currentElement = ((IJavaScriptUnit)currentElement).getType(child.getElementName());
							break;
						case IJavaScriptElement.TYPE:
							currentElement = ((IType)currentElement).getType(child.getElementName());
							break;
						case IJavaScriptElement.FIELD:
						case IJavaScriptElement.INITIALIZER:
						case IJavaScriptElement.METHOD:
							currentElement =  ((IMember)currentElement).getType(child.getElementName(), child.occurrenceCount);
							break;
					}
					break;
				case IJavaScriptElement.INITIALIZER:
					currentElement = ((IType)currentElement).getInitializer(child.occurrenceCount);
					break;
				case IJavaScriptElement.FIELD:
					if (currentElement instanceof CompilationUnit)
						currentElement = ((CompilationUnit)currentElement).getField(child.getElementName());
					else
						if (currentElement instanceof IType)
					currentElement = ((IType)currentElement).getField(child.getElementName());
					break;
				case IJavaScriptElement.METHOD:
					if (currentElement instanceof CompilationUnit)
						currentElement = ((CompilationUnit)currentElement).getFunction(child.getElementName(), ((IFunction)child).getParameterTypes());
					else if (currentElement instanceof SourceMethod)
							currentElement = ((SourceMethod)currentElement).getFunction(child.getElementName(), ((IFunction)child).getParameterTypes());
					else
						currentElement = ((IType)currentElement).getFunction(child.getElementName(), ((IFunction)child).getParameterTypes());
					break;
			}
	
		}
		if (currentElement != null && currentElement.exists()) {
			return new IJavaScriptElement[] {currentElement};
		} else {
			return null;
		}
	}
	
	/**
	 * @see IJavaScriptUnit#findPrimaryType()
	 */
	public IType findPrimaryType() {
		String typeName = Util.getNameWithoutJavaLikeExtension(getElementName());
		IType primaryType= getType(typeName);
		if (primaryType.exists()) {
			return primaryType;
		}
		return null;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.IWorkingCopy#findSharedWorkingCopy(IBufferFactory)
	 * @deprecated
	 */
	public IJavaScriptElement findSharedWorkingCopy(IBufferFactory factory) {
	
		// if factory is null, default factory must be used
		if (factory == null) factory = this.getBufferManager().getDefaultBufferFactory();
	
		return findWorkingCopy(BufferFactoryWrapper.create(factory));
	}
	
	/**
	 * @see IJavaScriptUnit#findWorkingCopy(WorkingCopyOwner)
	 */
	public IJavaScriptUnit findWorkingCopy(WorkingCopyOwner workingCopyOwner) {
		CompilationUnit cu = new CompilationUnit((PackageFragment)this.parent, getElementName(), workingCopyOwner);
		if (workingCopyOwner == DefaultWorkingCopyOwner.PRIMARY) {
			return cu;
		} else {
			// must be a working copy
			JavaModelManager.PerWorkingCopyInfo perWorkingCopyInfo = cu.getPerWorkingCopyInfo();
			if (perWorkingCopyInfo != null) {
				return perWorkingCopyInfo.getWorkingCopy();
			} else {
				return null;
			}
		}
	}
	
	/**
	 * @see IJavaScriptUnit#getAllTypes()
	 */
	public IType[] getAllTypes() throws JavaScriptModelException {
		IJavaScriptElement[] types = getTypes();
		int i;
		ArrayList allTypes = new ArrayList(types.length);
		ArrayList typesToTraverse = new ArrayList(types.length);
		for (i = 0; i < types.length; i++) {
			typesToTraverse.add(types[i]);
		}
		while (!typesToTraverse.isEmpty()) {
			IType type = (IType) typesToTraverse.get(0);
			typesToTraverse.remove(type);
			allTypes.add(type);
			types = type.getTypes();
			for (i = 0; i < types.length; i++) {
				typesToTraverse.add(types[i]);
			}
		}
		IType[] arrayOfAllTypes = new IType[allTypes.size()];
		allTypes.toArray(arrayOfAllTypes);
		return arrayOfAllTypes;
	}
	
	/**
	 * @see IMember#getCompilationUnit()
	 * @deprecated Use {@link #getJavaScriptUnit()} instead
	 */
	public IJavaScriptUnit getCompilationUnit() {
		return getJavaScriptUnit();
	}
	
	/**
	 * @see IMember#getJavaScriptUnit()
	 */
	public IJavaScriptUnit getJavaScriptUnit() {
		return this;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit#getContents()
	 */
	public char[] getContents() {
		IBuffer buffer = getBufferManager().getBuffer(this);
		if (buffer != null) {
			char[] contents = buffer.getCharacters();
			return contents == null ? CharOperation.NO_CHAR : contents;			
		}
		
		// no need to force opening of CU to get the content
		// also this cannot be a working copy, as its buffer is never closed while the working copy is alive
		try {
			IResource resource = getResource();
			if (resource instanceof IFile) {
				return Util.getResourceContentsAsCharArray((IFile) resource);
			}
		} catch (JavaScriptModelException e) {
			// Ignore
		}
		return CharOperation.NO_CHAR;
	}

	/**
	 * A compilation unit has a corresponding resource unless it is contained
	 * in a jar.
	 *
	 * @see IJavaScriptElement#getCorrespondingResource()
	 */
	public IResource getCorrespondingResource() throws JavaScriptModelException {
		PackageFragmentRoot root = getPackageFragmentRoot();
		if (root == null || root.isArchive()) {
			return null;
		} else {
			return getUnderlyingResource();
		}
	}
	
	/**
	 * @see IJavaScriptUnit#getElementAt(int)
	 */
	public IJavaScriptElement getElementAt(int position) throws JavaScriptModelException {
	
		IJavaScriptElement e= getSourceElementAt(position);
		if (e == this) {
			return null;
		} else {
			return e;
		}
	}
	
	public String getElementName() {
		return this.name;
	}
	
	/**
	 * @see IJavaScriptElement
	 */
	public int getElementType() {
		return JAVASCRIPT_UNIT;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.env.IDependent#getFileName()
	 */
	public char[] getFileName(){
		if (name.startsWith("http:")) //$NON-NLS-1$
			return name.toCharArray();
		return getPath().toString().toCharArray();
	}
	
	/*
	 * @see JavaElement
	 */
	public IJavaScriptElement getHandleFromMemento(String token, MementoTokenizer memento, WorkingCopyOwner workingCopyOwner) {
		switch (token.charAt(0)) {
			case JEM_IMPORTDECLARATION:
				JavaElement container = (JavaElement)getImportContainer();
				return container.getHandleFromMemento(token, memento, workingCopyOwner);
			case JEM_TYPE:
				if (!memento.hasMoreTokens()) return this;
				String typeName = memento.nextToken();
				JavaElement type = (JavaElement)getType(typeName);
				return type.getHandleFromMemento(memento, workingCopyOwner);
	
	
			case JEM_FIELD:
				if (!memento.hasMoreTokens()) return this;
				String fieldName = memento.nextToken();
				JavaElement field = (JavaElement)getField(fieldName);
				return field.getHandleFromMemento(memento, workingCopyOwner);
			
			case JEM_LOCALVARIABLE:
				if (!memento.hasMoreTokens()) return this;
				String varName = memento.nextToken();
				if (!memento.hasMoreTokens()) return this;
				memento.nextToken(); // JEM_COUNT
				if (!memento.hasMoreTokens()) return this;
				int declarationStart = Integer.parseInt(memento.nextToken());
				if (!memento.hasMoreTokens()) return this;
				memento.nextToken(); // JEM_COUNT
				if (!memento.hasMoreTokens()) return this;
				int declarationEnd = Integer.parseInt(memento.nextToken());
				if (!memento.hasMoreTokens()) return this;
				memento.nextToken(); // JEM_COUNT
				if (!memento.hasMoreTokens()) return this;
				int nameStart = Integer.parseInt(memento.nextToken());
				if (!memento.hasMoreTokens()) return this;
				memento.nextToken(); // JEM_COUNT
				if (!memento.hasMoreTokens()) return this;
				int nameEnd = Integer.parseInt(memento.nextToken());
				if (!memento.hasMoreTokens()) return this;
				memento.nextToken(); // JEM_COUNT
				if (!memento.hasMoreTokens()) return this;
				String typeSignature = memento.nextToken();
				return new LocalVariable(this, varName, declarationStart, declarationEnd, nameStart, nameEnd, typeSignature);
	
			case JEM_METHOD:
				if (!memento.hasMoreTokens()) return this;
				String selector = memento.nextToken();
				ArrayList params = new ArrayList();
				nextParam: while (memento.hasMoreTokens()) {
					token = memento.nextToken();
					switch (token.charAt(0)) {
						case JEM_TYPE:
						case JEM_TYPE_PARAMETER:
							break nextParam;
//						case JEM_METHOD:
//							if (!memento.hasMoreTokens()) return this;
//							String param = memento.nextToken();
//							StringBuffer buffer = new StringBuffer();
//							while (param.length() == 1 && Signature.C_ARRAY == param.charAt(0)) { // backward compatible with 3.0 mementos
//								buffer.append(Signature.C_ARRAY);
//								if (!memento.hasMoreTokens()) return this;
//								param = memento.nextToken();
//							}
//							params.add(buffer.toString() + param);
//							break;
						default:
							break nextParam;
					}
				}
				String[] parameters = new String[params.size()];
				params.toArray(parameters);
				JavaElement method = (JavaElement)getFunction(selector, parameters);
				if (token.charAt(0) == JEM_COUNT) {
					if (!memento.hasMoreTokens()) return method;
					memento.nextToken(); // JEM_COUNT
					if (!memento.hasMoreTokens()) return method;
					token = memento.nextToken();
				}
				switch (token.charAt(0)) {
					case JEM_TYPE:
					case JEM_TYPE_PARAMETER:
					case JEM_LOCALVARIABLE:
						return method.getHandleFromMemento(token, memento, workingCopyOwner);
					case JEM_METHOD:
						if (memento.hasMoreTokens())
							return method.getHandleFromMemento(token, memento, workingCopyOwner);
					default:
						return method;
				}
		}
		return null;
	}
	
	/**
	 * @see JavaElement#getHandleMementoDelimiter()
	 */
	protected char getHandleMementoDelimiter() {
		return JavaElement.JEM_COMPILATIONUNIT;
	}
	
	/**
	 * @see IJavaScriptUnit#getImport(String)
	 */
	public IImportDeclaration getImport(String importName) {
		return getImportContainer().getImport(importName);
	}
	
	/**
	 * @see IJavaScriptUnit#getImportContainer()
	 */
	public IImportContainer getImportContainer() {
		return new ImportContainer(this);
	}
	
	/**
	 * @see IJavaScriptUnit#getImports()
	 */
	public IImportDeclaration[] getImports() throws JavaScriptModelException {
		IImportContainer container= getImportContainer();
		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		Object info = manager.getInfo(container);
		if (info == null) {
			if (manager.getInfo(this) != null)
				// CU was opened, but no import container, then no imports
				return NO_IMPORTS;
			else {
				open(null); // force opening of CU
				info = manager.getInfo(container);
				if (info == null)
					// after opening, if no import container, then no imports
					return NO_IMPORTS;
			}
		}
		IJavaScriptElement[] elements = ((JavaElementInfo) info).children;
		int length = elements.length;
		IImportDeclaration[] imports = new IImportDeclaration[length];
		System.arraycopy(elements, 0, imports, 0, length);
		return imports;
	}
	
	/**
	 * @see IMember#getTypeRoot()
	 */
	public ITypeRoot getTypeRoot() {
		return this;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit#getMainTypeName()
	 */
	public char[] getMainTypeName(){
		return Util.getNameWithoutJavaLikeExtension(getElementName()).toCharArray();
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.IWorkingCopy#getOriginal(IJavaScriptElement)
	 * @deprecated
	 */
	public IJavaScriptElement getOriginal(IJavaScriptElement workingCopyElement) {
		// backward compatibility
		if (!isWorkingCopy()) return null;
		CompilationUnit cu = (CompilationUnit)workingCopyElement.getAncestor(JAVASCRIPT_UNIT);
		if (cu == null || !this.owner.equals(cu.owner)) {
			return null;
		}
	
		return workingCopyElement.getPrimaryElement();
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.IWorkingCopy#getOriginalElement()
	 * @deprecated
	 */
	public IJavaScriptElement getOriginalElement() {
		// backward compatibility
		if (!isWorkingCopy()) return null;
	
		return getPrimaryElement();
	}
	
	/*
	 * @see IJavaScriptUnit#getOwner()
	 */
	public WorkingCopyOwner getOwner() {
		return isPrimary() || !isWorkingCopy() ? null : this.owner;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit#getPackageName()
	 */
	public char[][] getPackageName() {
		PackageFragment packageFragment = (PackageFragment) getParent();
		if (packageFragment == null) return CharOperation.NO_CHAR_CHAR;
		return Util.toCharArrays(packageFragment.names);
	}
	
	/**
	 * @see IJavaScriptElement#getPath()
	 */
	public IPath getPath() {
		PackageFragmentRoot root = getPackageFragmentRoot();
		if (root == null) return new Path(getElementName()); // working copy not in workspace
		if (root.isArchive()) {
			return root.getPath();
		} else {
			return getParent().getPath().append(getElementName());
		}
	}
	/*
	 * Returns the per working copy info for the receiver, or null if none exist.
	 * Note: the use count of the per working copy info is NOT incremented.
	 */
	public JavaModelManager.PerWorkingCopyInfo getPerWorkingCopyInfo() {
		return JavaModelManager.getJavaModelManager().getPerWorkingCopyInfo(this, false/*don't create*/, false/*don't record usage*/, null/*no problem requestor needed*/);
	}
	
	/*
	 * @see IJavaScriptUnit#getPrimary()
	 */
	public IJavaScriptUnit getPrimary() {
		return (IJavaScriptUnit)getPrimaryElement(true);
	}
	
	/*
	 * @see JavaElement#getPrimaryElement(boolean)
	 */
	public IJavaScriptElement getPrimaryElement(boolean checkOwner) {
		if (checkOwner && isPrimary()) return this;
		return new CompilationUnit((PackageFragment)getParent(), getElementName(), DefaultWorkingCopyOwner.PRIMARY);
	}
	
	/**
	 * @see IJavaScriptElement#getResource()
	 */
	public IResource getResource() {
		PackageFragmentRoot root = getPackageFragmentRoot();
		if (root == null) return null; // working copy not in workspace
		if (root.isArchive()) {
			return root.getResource();
		} else {
			IContainer parentResource = (IContainer) getParent().getResource();
			if (parentResource!=null)
				return parentResource.getFile(new Path(getElementName()));
		}
		return null;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.ISourceReference#getSource()
	 */
	public String getSource() throws JavaScriptModelException {
		IBuffer buffer = getBuffer();
		if (buffer == null) return ""; //$NON-NLS-1$
		return buffer.getContents();
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.ISourceReference#getSourceRange()
	 */
	public ISourceRange getSourceRange() throws JavaScriptModelException {
		return ((CompilationUnitElementInfo) getElementInfo()).getSourceRange();
	}
	
	/**
	 * @see IJavaScriptUnit#getType(String)
	 */
	public IType getType(String typeName) {
		return new SourceType(this, typeName);
	}
	
	/**
	 * @see IJavaScriptUnit#getTypes()
	 */
	public IType[] getTypes() throws JavaScriptModelException {
		ArrayList list = getChildrenOfType(TYPE);
		IType[] array= new IType[list.size()];
		list.toArray(array);
		return array;
	}
	
	/**
	 * @see IJavaScriptElement
	 */
	public IResource getUnderlyingResource() throws JavaScriptModelException {
		if (isWorkingCopy() && !isPrimary()) return null;
		return super.getUnderlyingResource();
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.IWorkingCopy#getSharedWorkingCopy(IProgressMonitor, IBufferFactory, IProblemRequestor)
	 * @deprecated
	 */
	public IJavaScriptElement getSharedWorkingCopy(IProgressMonitor pm, IBufferFactory factory, IProblemRequestor problemRequestor) throws JavaScriptModelException {
	
		// if factory is null, default factory must be used
		if (factory == null) factory = this.getBufferManager().getDefaultBufferFactory();
	
		return getWorkingCopy(BufferFactoryWrapper.create(factory), problemRequestor, pm);
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.IWorkingCopy#getWorkingCopy()
	 * @deprecated
	 */
	public IJavaScriptElement getWorkingCopy() throws JavaScriptModelException {
		return getWorkingCopy(null);
	}
	
	/**
	 * @see IJavaScriptUnit#getWorkingCopy(IProgressMonitor)
	 */
	public IJavaScriptUnit getWorkingCopy(IProgressMonitor monitor) throws JavaScriptModelException {
		return getWorkingCopy(new WorkingCopyOwner() {/*non shared working copy*/}, null/*no problem requestor*/, monitor);
	}
	
	/**
	 * @see ITypeRoot#getWorkingCopy(WorkingCopyOwner, IProgressMonitor)
	 */
	public IJavaScriptUnit getWorkingCopy(WorkingCopyOwner workingCopyOwner, IProgressMonitor monitor) throws JavaScriptModelException {
		return getWorkingCopy(workingCopyOwner, null, monitor);
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.IWorkingCopy#getWorkingCopy(IProgressMonitor, IBufferFactory, IProblemRequestor)
	 * @deprecated
	 */
	public IJavaScriptElement getWorkingCopy(IProgressMonitor monitor, IBufferFactory factory, IProblemRequestor problemRequestor) throws JavaScriptModelException {
		return getWorkingCopy(BufferFactoryWrapper.create(factory), problemRequestor, monitor);
	}
	
	/**
	 * @see IJavaScriptUnit#getWorkingCopy(WorkingCopyOwner, IProblemRequestor, IProgressMonitor)
	 * @deprecated
	 */
	public IJavaScriptUnit getWorkingCopy(WorkingCopyOwner workingCopyOwner, IProblemRequestor problemRequestor, IProgressMonitor monitor) throws JavaScriptModelException {
		if (!isPrimary()) return this;
	
		JavaModelManager manager = JavaModelManager.getJavaModelManager();
	
		CompilationUnit workingCopy = new CompilationUnit((PackageFragment)getParent(), getElementName(),superTypeName, workingCopyOwner);
		JavaModelManager.PerWorkingCopyInfo perWorkingCopyInfo =
			manager.getPerWorkingCopyInfo(workingCopy, false/*don't create*/, true/*record usage*/, null/*not used since don't create*/);
		if (perWorkingCopyInfo != null) {
			return perWorkingCopyInfo.getWorkingCopy(); // return existing handle instead of the one created above
		}
		BecomeWorkingCopyOperation op = new BecomeWorkingCopyOperation(workingCopy, problemRequestor);
		op.runOperation(monitor);
		return workingCopy;
	}
	
	/**
	 * @see Openable#hasBuffer()
	 */
	protected boolean hasBuffer() {
		return true;
	}
	
	/*
	 * @see IJavaScriptUnit#hasResourceChanged()
	 */
	public boolean hasResourceChanged() {
		if (!isWorkingCopy()) return false;
	
		// if resource got deleted, then #getModificationStamp() will answer IResource.NULL_STAMP, which is always different from the cached
		// timestamp
		Object info = JavaModelManager.getJavaModelManager().getInfo(this);
		if (info == null) return false;
		IResource resource = getResource();
		if (resource == null) return false;
		return ((CompilationUnitElementInfo)info).timestamp != resource.getModificationStamp();
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.IWorkingCopy#isBasedOn(IResource)
	 * @deprecated
	 */
	public boolean isBasedOn(IResource resource) {
		if (!isWorkingCopy()) return false;
		if (!getResource().equals(resource)) return false;
		return !hasResourceChanged();
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.IOpenable#isConsistent()
	 */
	public boolean isConsistent() {
		return !JavaModelManager.getJavaModelManager().getElementsOutOfSynchWithBuffers().contains(this);
	}
	
	public boolean isPrimary() {
		return this.owner == DefaultWorkingCopyOwner.PRIMARY;
	}
	
	/**
	 * @see Openable#isSourceElement()
	 */
	protected boolean isSourceElement() {
		return true;
	}
	
	protected IStatus validateCompilationUnit(IResource resource) {
		IPackageFragmentRoot root = getPackageFragmentRoot();
		// root never null as validation is not done for working copies
		if (resource != null) {
			char[][] inclusionPatterns = ((PackageFragmentRoot)root).fullInclusionPatternChars();
			char[][] exclusionPatterns = ((PackageFragmentRoot)root).fullExclusionPatternChars();
			if (Util.isExcluded(resource, inclusionPatterns, exclusionPatterns))
				return new JavaModelStatus(IJavaScriptModelStatusConstants.ELEMENT_NOT_ON_CLASSPATH, this);
			if (!resource.isAccessible())
				return new JavaModelStatus(IJavaScriptModelStatusConstants.ELEMENT_DOES_NOT_EXIST, this);
		}
		IJavaScriptProject project = getJavaScriptProject();
		return JavaScriptConventions.validateCompilationUnitName(getElementName(),project.getOption(JavaScriptCore.COMPILER_SOURCE, true), project.getOption(JavaScriptCore.COMPILER_COMPLIANCE, true));
	}
	
	/*
	 * @see IJavaScriptUnit#isWorkingCopy()
	 */
	public boolean isWorkingCopy() {
		// For backward compatibility, non primary working copies are always returning true; in removal
		// delta, clients can still check that element was a working copy before being discarded.
		return !isPrimary() || getPerWorkingCopyInfo() != null;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.IOpenable#makeConsistent(IProgressMonitor)
	 */
	public void makeConsistent(IProgressMonitor monitor) throws JavaScriptModelException {
		makeConsistent(NO_AST, false/*don't resolve bindings*/, 0 /* don't perform statements recovery */, null/*don't collect problems but report them*/, monitor);
	}
	
	public org.eclipse.wst.jsdt.core.dom.JavaScriptUnit makeConsistent(int astLevel, boolean resolveBindings, int reconcileFlags, HashMap problems, IProgressMonitor monitor) throws JavaScriptModelException {
		if (isConsistent() || this.fIsMakingConsistent) return null;
		
		this.fIsMakingConsistent = true;
	
		try {
			// create a new info and make it the current info
			// (this will remove the info and its children just before storing the new infos)
			if (astLevel != NO_AST || problems != null) {
				ASTHolderCUInfo info = new ASTHolderCUInfo();
				info.astLevel = astLevel;
				info.resolveBindings = resolveBindings;
				info.reconcileFlags = reconcileFlags;
				info.problems = problems;
				openWhenClosed(info, monitor);
				org.eclipse.wst.jsdt.core.dom.JavaScriptUnit result = info.ast;
				info.ast = null;
				return result;
			} else {
				openWhenClosed(createElementInfo(), monitor);
				return null;
			}
		} finally {
			this.fIsMakingConsistent = false;
		}
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.ISourceManipulation#move(IJavaScriptElement, IJavaScriptElement, String, boolean, IProgressMonitor)
	 */
	public void move(IJavaScriptElement container, IJavaScriptElement sibling, String rename, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
		if (container == null) {
			throw new IllegalArgumentException(Messages.operation_nullContainer);
		}
		IJavaScriptElement[] elements= new IJavaScriptElement[] {this};
		IJavaScriptElement[] containers= new IJavaScriptElement[] {container};
	
		String[] renamings= null;
		if (rename != null) {
			renamings= new String[] {rename};
		}
		getJavaScriptModel().move(elements, containers, null, renamings, force, monitor);
	}
	
	/**
	 * @see Openable#openBuffer(IProgressMonitor, Object)
	 */
	protected IBuffer openBuffer(IProgressMonitor pm, Object info) throws JavaScriptModelException {
	
		// create buffer
		BufferManager bufManager = getBufferManager();
		boolean isWorkingCopy = isWorkingCopy();
		IBuffer buffer =
			isWorkingCopy
				? this.owner.createBuffer(this)
				: BufferManager.createBuffer(this);
		if (buffer == null) return null;
	
		// synchronize to ensure that 2 threads are not putting 2 different buffers at the same time
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=146331
		synchronized(bufManager) {
			IBuffer existingBuffer = bufManager.getBuffer(this);
			if (existingBuffer != null)
				return existingBuffer;
	
			// set the buffer source
			if (buffer.getCharacters() == null) {
				if (isWorkingCopy) {
					IJavaScriptUnit original;
					if (!isPrimary()
							&& (original = new CompilationUnit((PackageFragment)getParent(), getElementName(), DefaultWorkingCopyOwner.PRIMARY)).isOpen()) {
						buffer.setContents(original.getSource());
					} else {
						IFile file = (IFile)getResource();
						if (file == null || !file.exists()) {
							// initialize buffer with empty contents
							buffer.setContents(CharOperation.NO_CHAR);
						} else {
							buffer.setContents(Util.getResourceContentsAsCharArray(file));
						}
					}
				} else {
					IFile file = (IFile)this.getResource();
					if (file == null || !file.exists()) throw newNotPresentException();
					buffer.setContents(Util.getResourceContentsAsCharArray(file));
				}
			}
	
			// add buffer to buffer cache
			// note this may cause existing buffers to be removed from the buffer cache, but only primary compilation unit's buffer
			// can be closed, thus no call to a client's IBuffer#close() can be done in this synchronized block.
			bufManager.addBuffer(buffer);
	
			// listen to buffer changes
			buffer.addBufferChangedListener(this);
		}
		return buffer;
	}
	protected void openParent(Object childInfo, HashMap newElements, IProgressMonitor pm) throws JavaScriptModelException {
		if (!isWorkingCopy())
			super.openParent(childInfo, newElements, pm);
		// don't open parent for a working copy to speed up the first becomeWorkingCopy
		// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=89411)
	}
	
	/**
	 * @see IJavaScriptUnit#reconcile()
	 * @deprecated
	 */
	public IMarker[] reconcile() throws JavaScriptModelException {
		reconcile(NO_AST, false/*don't force problem detection*/, false, null/*use primary owner*/, null/*no progress monitor*/);
		return null;
	}
	
	/**
	 * @see IJavaScriptUnit#reconcile(int, boolean, WorkingCopyOwner, IProgressMonitor)
	 */
	public void reconcile(boolean forceProblemDetection, IProgressMonitor monitor) throws JavaScriptModelException {
		reconcile(NO_AST, forceProblemDetection? IJavaScriptUnit.FORCE_PROBLEM_DETECTION : 0, null/*use primary owner*/, monitor);
	}
	
	/**
	 * @see IJavaScriptUnit#reconcile(int, boolean, WorkingCopyOwner, IProgressMonitor)
	 * @since 3.0
	 */
	public org.eclipse.wst.jsdt.core.dom.JavaScriptUnit reconcile(
			int astLevel,
			boolean forceProblemDetection,
			WorkingCopyOwner workingCopyOwner,
			IProgressMonitor monitor) throws JavaScriptModelException {
		return reconcile(astLevel, forceProblemDetection, false, workingCopyOwner, monitor);
	}
	
	/**
	 * @see IJavaScriptUnit#reconcile(int, boolean, WorkingCopyOwner, IProgressMonitor)
	 * @since 3.0
	 */
	public org.eclipse.wst.jsdt.core.dom.JavaScriptUnit reconcile(
			int astLevel,
			boolean forceProblemDetection,
			boolean enableStatementsRecovery,
			WorkingCopyOwner workingCopyOwner,
			IProgressMonitor monitor) throws JavaScriptModelException {
		int flags = 0;
		if (forceProblemDetection) flags |= IJavaScriptUnit.FORCE_PROBLEM_DETECTION;
		if (enableStatementsRecovery) flags |= IJavaScriptUnit.ENABLE_STATEMENTS_RECOVERY;
		return reconcile(astLevel, flags, workingCopyOwner, monitor);
	}
	
	public org.eclipse.wst.jsdt.core.dom.JavaScriptUnit reconcile(
			int astLevel,
			int reconcileFlags,
			WorkingCopyOwner workingCopyOwner,
			IProgressMonitor monitor)
			throws JavaScriptModelException {
	
		if (!isWorkingCopy()) return null; // Reconciling is not supported on non working copies
		if (workingCopyOwner == null) workingCopyOwner = DefaultWorkingCopyOwner.PRIMARY;
	
	
		PerformanceStats stats = null;
		if(ReconcileWorkingCopyOperation.PERF) {
			stats = PerformanceStats.getStats(JavaModelManager.RECONCILE_PERF, this);
			stats.startRun(new String(this.getFileName()));
		}
		ReconcileWorkingCopyOperation op = new ReconcileWorkingCopyOperation(this, astLevel, reconcileFlags, workingCopyOwner);
		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		try {
			manager.cacheZipFiles(); // cache zip files for performance (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=134172)
			op.runOperation(monitor);
		} finally {
			manager.flushZipFiles();
		}
		if(ReconcileWorkingCopyOperation.PERF) {
			stats.endRun();
		}
		return op.ast;
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.ISourceManipulation#rename(String, boolean, IProgressMonitor)
	 */
	public void rename(String newName, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
		if (newName == null) {
			throw new IllegalArgumentException(Messages.operation_nullName);
		}
		IJavaScriptElement[] elements= new IJavaScriptElement[] {this};
		IJavaScriptElement[] dests= new IJavaScriptElement[] {this.getParent()};
		String[] renamings= new String[] {newName};
		getJavaScriptModel().rename(elements, dests, renamings, force, monitor);
	}
	
	/*
	 * @see IJavaScriptUnit
	 */
	public void restore() throws JavaScriptModelException {
	
		if (!isWorkingCopy()) return;
	
		CompilationUnit original = (CompilationUnit) getOriginalElement();
		IBuffer buffer = this.getBuffer();
		if (buffer == null) return;
		buffer.setContents(original.getContents());
		updateTimeStamp(original);
		makeConsistent(null);
	}
	
	/**
	 * @see org.eclipse.wst.jsdt.core.IOpenable
	 */
	public void save(IProgressMonitor pm, boolean force) throws JavaScriptModelException {
		if (isWorkingCopy()) {
			// no need to save the buffer for a working copy (this is a noop)
			reconcile();   // not simply makeConsistent, also computes fine-grain deltas
									// in case the working copy is being reconciled already (if not it would miss
									// one iteration of deltas).
		} else {
			super.save(pm, force);
		}
	}
	
	/**
	 * Debugging purposes
	 */
	protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
		if (!isPrimary()) {
			buffer.append(this.tabString(tab));
			buffer.append("[Working copy] "); //$NON-NLS-1$
			toStringName(buffer);
		} else {
			if (isWorkingCopy()) {
				buffer.append(this.tabString(tab));
				buffer.append("[Working copy] "); //$NON-NLS-1$
				toStringName(buffer);
				if (info == null) {
					buffer.append(" (not open)"); //$NON-NLS-1$
				}
			} else {
				super.toStringInfo(tab, buffer, info, showResolvedInfo);
			}
		}
	}
	
	/*
	 * Assume that this is a working copy
	 */
	protected void updateTimeStamp(CompilationUnit original) throws JavaScriptModelException {
		long timeStamp =
			((IFile) original.getResource()).getModificationStamp();
		if (timeStamp == IResource.NULL_STAMP) {
			throw new JavaScriptModelException(
				new JavaModelStatus(IJavaScriptModelStatusConstants.INVALID_RESOURCE));
		}
		((CompilationUnitElementInfo) getElementInfo()).timestamp = timeStamp;
	}
	
	public IField getField(String fieldName) {
		return new SourceField(this, fieldName);
	}
	
	public IField[] getFields() throws JavaScriptModelException {
		ArrayList list = getChildrenOfType(FIELD);
		IField[] array= new IField[list.size()];
		list.toArray(array);
		return array;
	
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
		IFunction[] array= new IFunction[list.size()];
		list.toArray(array);
		return array;
	}
	
	public IField createField(String contents, IJavaScriptElement sibling, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
		CreateFieldOperation op = new CreateFieldOperation(this, contents, force);
		if (sibling != null) {
			op.createBefore(sibling);
		}
		op.runOperation(monitor);
		return (IField) op.getResultElements()[0];
	}
	
	/**
	 * @see IType
	 */
	public IFunction createMethod(String contents, IJavaScriptElement sibling, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
		CreateMethodOperation op = new CreateMethodOperation(this, contents, force);
		if (sibling != null) {
			op.createBefore(sibling);
		}
		op.runOperation(monitor);
		return (IFunction) op.getResultElements()[0];
	}
	
	public String getDisplayName() {
		if(isVirtual()) {
	
			JsGlobalScopeContainerInitializer init = ((IVirtualParent)parent).getContainerInitializer();
			if(init==null) return super.getDisplayName();
			return init.getDescription(new Path(getElementName()), getJavaScriptProject());
		}
		return super.getDisplayName();
	}
	
	public URI getHostPath() {
		if(isVirtual()) {
			JsGlobalScopeContainerInitializer init = ((IVirtualParent)parent).getContainerInitializer();
			if(init!=null) return init.getHostPath(new Path(getElementName()), getJavaScriptProject());
		}
		return null;
	}
	
	public JsGlobalScopeContainerInitializer getContainerInitializer() {
		JsGlobalScopeContainerInitializer init = null;
		if (parent instanceof IVirtualParent)
			init=((IVirtualParent)parent).getContainerInitializer();
		return init;
	}
	
	public LibrarySuperType getCommonSuperType() {
		IJavaScriptProject javaProject = getJavaScriptProject();
		if(javaProject!=null && javaProject.exists()) return javaProject.getCommonSuperType();
		return null;
	}
	
	public IFunction[] findFunctions(IFunction method) {
		ArrayList list = new ArrayList();
		try {
			IFunction[]methods=getFunctions();
			String elementName = method.getElementName();
			String parentName = method.getParent().getElementName();
			for (int i = 0, length = methods.length; i < length; i++) {
				IFunction existingMethod = methods[i];
				if (elementName.equals(existingMethod.getElementName()) 
							&& parentName.equals(existingMethod.getParent().getElementName())) {
					list.add(existingMethod);
				}
				else {
					IFunction nestedMethod = findNestedFunction(existingMethod, elementName, parentName);
					if (nestedMethod != null) {
						list.add(nestedMethod);
					}
				}
			}
		} catch (JavaScriptModelException e) {
		}
		int size = list.size();
		if (size == 0) {
			return null;
		} else {
			IFunction[] result = new IFunction[size];
			list.toArray(result);
			return result;
		}
	}
	
	public IFunction findNestedFunction(IFunction method, String elementName, String parentName) {
		try {
			if (method.hasChildren()) {
				ArrayList methods = ((JavaElement) method).getChildrenOfType(METHOD);
				for (Iterator iterator = methods.iterator(); iterator.hasNext();) {
					IFunction childMethod = (IFunction) iterator.next();
					if (elementName.equals(childMethod.getElementName()) 
								&& parentName.equals(childMethod.getParent().getElementName())) {
						return childMethod;
					}
					IFunction nestedMethod = findNestedFunction(childMethod, elementName, parentName);
					if (nestedMethod != null)
						return nestedMethod;
				} 
			}
		}
		catch (JavaScriptModelException e) {
		}
		return null;
	}
	
	public String getInferenceID() {
		JsGlobalScopeContainerInitializer containerInitializer = getContainerInitializer();
		if (containerInitializer!=null)
			return containerInitializer.getInferenceID();
		return null;
	}
	
	public SearchableEnvironment newSearchableNameEnvironment(WorkingCopyOwner owner) throws JavaScriptModelException {
		SearchableEnvironment env=super.newSearchableNameEnvironment(owner);
		env.setCompilationUnit(this);
		return env;
	}
}