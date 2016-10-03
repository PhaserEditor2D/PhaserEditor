/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.hierarchy;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.env.IGenericType;
import org.eclipse.wst.jsdt.internal.compiler.env.ISourceType;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.wst.jsdt.internal.core.ClassFile;
import org.eclipse.wst.jsdt.internal.core.JavaElement;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.core.NameLookup;
import org.eclipse.wst.jsdt.internal.core.Openable;
import org.eclipse.wst.jsdt.internal.core.ResolvedBinaryType;
import org.eclipse.wst.jsdt.internal.core.SearchableEnvironment;
import org.eclipse.wst.jsdt.internal.core.SourceTypeElementInfo;
import org.eclipse.wst.jsdt.internal.core.util.ResourceCompilationUnit;

public abstract class HierarchyBuilder {
	/**
	 * The hierarchy being built.
	 */
	protected TypeHierarchy hierarchy;
	/**
	 * @see NameLookup
	 */
	protected NameLookup nameLookup;
	/**
	 * The resolver used to resolve type hierarchies
	 * @see HierarchyResolver
	 */
	protected HierarchyResolver hierarchyResolver;
	/**
	 * A temporary cache of infos to handles to speed info
	 * to handle translation - it only contains the entries
	 * for the types in the region (in other words, it contains
	 * no supertypes outside the region).
	 */
	protected Map infoToHandle;
	/*
	 * The dot-separated fully qualified name of the focus type, or null of none.
	 */
	protected String focusQualifiedName;

	public HierarchyBuilder(TypeHierarchy hierarchy) throws JavaScriptModelException {

		this.hierarchy = hierarchy;
		JavaProject project = (JavaProject) hierarchy.javaProject();

		IType focusType = hierarchy.getType();
		org.eclipse.wst.jsdt.core.IJavaScriptUnit unitToLookInside = focusType == null ? null : focusType.getJavaScriptUnit();
		org.eclipse.wst.jsdt.core.IJavaScriptUnit[] workingCopies = this.hierarchy.workingCopies;
		org.eclipse.wst.jsdt.core.IJavaScriptUnit[] unitsToLookInside;
		ICompilationUnit mainFile=null;
		if (unitToLookInside != null) {
			int wcLength = workingCopies == null ? 0 : workingCopies.length;
			if (wcLength == 0) {
				unitsToLookInside = new org.eclipse.wst.jsdt.core.IJavaScriptUnit[] {unitToLookInside};
			} else {
				unitsToLookInside = new org.eclipse.wst.jsdt.core.IJavaScriptUnit[wcLength+1];
				unitsToLookInside[0] = unitToLookInside;
				System.arraycopy(workingCopies, 0, unitsToLookInside, 1, wcLength);
			}
			mainFile=(ICompilationUnit)unitToLookInside;
		} else {
			unitsToLookInside = workingCopies;
			if (workingCopies!=null && workingCopies.length>0)
				mainFile=(ICompilationUnit)workingCopies[0];
		}
		if (project != null) {
			SearchableEnvironment searchableEnvironment = project.newSearchableNameEnvironment(unitsToLookInside);
			if (mainFile!=null)
				searchableEnvironment.setCompilationUnit(mainFile);
			this.nameLookup = searchableEnvironment.nameLookup;
			this.hierarchyResolver =
				new HierarchyResolver(
					searchableEnvironment,
					project.getOptions(true),
					this,
					new DefaultProblemFactory());
		}
		this.infoToHandle = new HashMap(5);
		this.focusQualifiedName = focusType == null ? null : focusType.getTypeQualifiedName();
	}

	public abstract void build(boolean computeSubtypes)
		throws JavaScriptModelException, CoreException;
	/**
	 * Configure this type hierarchy by computing the supertypes only.
	 */
	protected void buildSupertypes() {
		IType focusType = this.getType();
		if (focusType == null)
			return;
		// get generic type from focus type
		IGenericType type;
		try {
			type = (IGenericType) ((JavaElement) focusType).getElementInfo();
		} catch (JavaScriptModelException e) {
			// if the focus type is not present, or if cannot get workbench path
			// we cannot create the hierarchy
			return;
		}
		//NB: no need to set focus type on hierarchy resolver since no other type is injected
		//    in the hierarchy resolver, thus there is no need to check that a type is
		//    a sub or super type of the focus type.
		this.hierarchyResolver.resolve(type);

		// Add focus if not already in (case of a type with no explicit super type)
		if (!this.hierarchy.contains(focusType)) {
			this.hierarchy.addRootClass(focusType);
		}
	}
	/**
	 * Connect the supplied type to its superclass.
	 * The superclass are the identical binary or source types as
	 * supplied by the name environment.
	 */
	public void connect(
		IGenericType type,
		IType typeHandle,
		IType superclassHandle) {

		/*
		 * Temporary workaround for 1G2O5WK: ITPJCORE:WINNT - NullPointerException when selecting "Show in Type Hierarchy" for a inner class
		 */
		if (typeHandle == null)
			return;
		if (TypeHierarchy.DEBUG) {
			System.out.println(
				"Connecting: " + ((JavaElement) typeHandle).toStringWithAncestors()); //$NON-NLS-1$
			System.out.println(
				"  to superclass: " //$NON-NLS-1$
					+ (superclassHandle == null
						? "<None>" //$NON-NLS-1$
						: ((JavaElement) superclassHandle).toStringWithAncestors()));
		}
		// now do the caching
		switch (TypeDeclaration.kind(type.getModifiers())) {
			case TypeDeclaration.CLASS_DECL :
				if (superclassHandle == null) {
					this.hierarchy.addRootClass(typeHandle);
				} else {
					this.hierarchy.cacheSuperclass(typeHandle, superclassHandle);
				}
				break;
		}

		// record flags
		this.hierarchy.cacheFlags(typeHandle, type.getModifiers());
	}
	/**
	 * Returns a handle for the given generic type or null if not found.
	 */
	protected IType getHandle(IGenericType genericType, ReferenceBinding binding) {
		if (genericType == null)
			return null;
		if (genericType instanceof HierarchyType) {
			IType handle = (IType)this.infoToHandle.get(genericType);
			if (handle == null) {
				handle = ((HierarchyType)genericType).typeHandle;
				handle = (IType) ((JavaElement) handle).resolved(binding);
				this.infoToHandle.put(genericType, handle);
			}
			return handle;
		} else if (genericType instanceof SourceTypeElementInfo) {
			IType handle = ((SourceTypeElementInfo) genericType).getHandle();
			return (IType) ((JavaElement) handle).resolved(binding);
		} else if (genericType.isBinaryType()) {
			ClassFile classFile = (ClassFile) this.infoToHandle.get(genericType);
			// if it's null, it's from outside the region, so do lookup
			if (classFile == null) {
				IType handle = lookupBinaryHandle((ISourceType) genericType);
				if (handle == null)
					return null;
				// case of an anonymous type (see 1G2O5WK: ITPJCORE:WINNT - NullPointerException when selecting "Show in Type Hierarchy" for a inner class)
				// optimization: remember the handle for next call (case of java.io.Serializable that a lot of classes implement)
				classFile = (ClassFile) handle.getParent();
				this.infoToHandle.put(genericType, classFile);
			}
			return new ResolvedBinaryType(classFile, new String(binding.readableName()), new String(binding.computeUniqueKey()));
		} else {
			return null;
		}
	}
	protected IType getType() {
		return this.hierarchy.getType();
	}
	/**
	 * Looks up and returns a handle for the given binary info.
	 */
	protected IType lookupBinaryHandle(ISourceType typeInfo) {
		int flag;
		String qualifiedName;
		switch (TypeDeclaration.kind(typeInfo.getModifiers())) {
			case TypeDeclaration.CLASS_DECL :
				flag = NameLookup.ACCEPT_CLASSES;
				break;
			default:
				//case IGenericType.ANNOTATION :
				flag = NameLookup.ACCEPT_ANNOTATIONS;
				break;
		}
		char[] bName = typeInfo.getName();
		qualifiedName = new String(ClassFile.translatedName(bName));
		if (qualifiedName.equals(this.focusQualifiedName)) return getType();
		NameLookup.Answer answer = this.nameLookup.findType(qualifiedName,
			false,
			flag,
			true/* consider secondary types */,
			false/* do NOT wait for indexes */,
			false/*don't check restrictions*/,
			null);
		return answer == null || answer.type == null || !answer.type.isBinary() ? null : answer.type;

	}
	protected void worked(IProgressMonitor monitor, int work) {
		if (monitor != null) {
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			} else {
				monitor.worked(work);
			}
		}
	}
/**
 * Create an IJavaScriptUnit info from the given compilation unit on disk.
 */
protected ICompilationUnit createCompilationUnitFromPath(Openable handle, IFile file) {
	final char[] elementName = handle.getElementName().toCharArray();
	return new ResourceCompilationUnit(file, file.getLocationURI()) {
		public char[] getFileName() {
			return super.file.getFullPath().toString().toCharArray();
		}
	};
}


}
