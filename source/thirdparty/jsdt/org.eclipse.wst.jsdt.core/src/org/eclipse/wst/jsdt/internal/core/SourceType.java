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
package org.eclipse.wst.jsdt.internal.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.CompletionRequestor;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IInitializer;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.internal.codeassist.CompletionEngine;
import org.eclipse.wst.jsdt.internal.codeassist.ISelectionRequestor;
import org.eclipse.wst.jsdt.internal.codeassist.SelectionEngine;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.env.ISourceType;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.core.hierarchy.TypeHierarchy;
import org.eclipse.wst.jsdt.internal.core.util.MementoTokenizer;
import org.eclipse.wst.jsdt.internal.core.util.Messages;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * Handle for a source type. Info object is a SourceTypeElementInfo.
 * 
 * Note: Parent is either an IClassFile, an IJavaScriptUnit or an IType.
 * 
 * @see IType
 */

public class SourceType extends NamedMember implements IType {

	/**
	 * <p>
	 * <code>true</code> if this type is anonymous.
	 * </p>
	 * <p>
	 * <b>NOTE:</b> Even if this is <code>false</code> {@link #isAnonymous()} can still return true
	 * based don the name of the type. The purpose of this is for when a type should be considered
	 * anonymous not based on its name.
	 * </p>
	 */
	private final boolean fIsAnonymous;

	protected SourceType(JavaElement parent, String name) {
		super(parent, name);
		this.fIsAnonymous = false;
	}

	protected SourceType(JavaElement parent, String name, boolean isAnonymous) {
		super(parent, name);
		this.fIsAnonymous = isAnonymous;
	}

	/**
	 * @see IType
	 */
	public void codeComplete(char[] snippet, int insertion, int position, char[][] localVariableTypeNames,
			char[][] localVariableNames, int[] localVariableModifiers, boolean isStatic, CompletionRequestor requestor)
			throws JavaScriptModelException {
		codeComplete(snippet, insertion, position, localVariableTypeNames, localVariableNames, localVariableModifiers,
				isStatic, requestor, DefaultWorkingCopyOwner.PRIMARY);
	}

	/**
	 * @see IType
	 */
	public void codeComplete(char[] snippet, int insertion, int position, char[][] localVariableTypeNames,
			char[][] localVariableNames, int[] localVariableModifiers, boolean isStatic, CompletionRequestor requestor,
			WorkingCopyOwner owner) throws JavaScriptModelException {
		if(requestor == null) {
			throw new IllegalArgumentException("Completion requestor cannot be null"); //$NON-NLS-1$
		}

		JavaProject project = (JavaProject) getJavaScriptProject();
		SearchableEnvironment environment = newSearchableNameEnvironment(owner);
		CompletionEngine engine = new CompletionEngine(environment, requestor, project.getOptions(true), project);

		String source = getJavaScriptUnit().getSource();
		if(source != null && insertion > -1 && insertion < source.length()) {

			char[] prefix = CharOperation.concat(source.substring(0, insertion).toCharArray(), new char[] { '{' });
			char[] suffix = CharOperation.concat(new char[] { '}' }, source.substring(insertion).toCharArray());
			char[] fakeSource = CharOperation.concat(prefix, snippet, suffix);

			BasicCompilationUnit cu = new BasicCompilationUnit(fakeSource, null, getElementName(), getParent());

			engine.complete(cu, prefix.length + position, prefix.length);
		} else {
			engine.complete(this, snippet, position, localVariableTypeNames, localVariableNames,
					localVariableModifiers, isStatic);
		}
		if(NameLookup.VERBOSE) {
			System.out.println(Thread.currentThread()
					+ " TIME SPENT in NameLoopkup#seekTypesInSourcePackage: " + environment.nameLookup.timeSpentInSeekTypesInSourcePackage + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(Thread.currentThread()
					+ " TIME SPENT in NameLoopkup#seekTypesInBinaryPackage: " + environment.nameLookup.timeSpentInSeekTypesInBinaryPackage + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * @see IType
	 */
	public IField createField(String contents, IJavaScriptElement sibling, boolean force, IProgressMonitor monitor)
			throws JavaScriptModelException {
		CreateFieldOperation op = new CreateFieldOperation(this, contents, force);
		if(sibling != null) {
			op.createBefore(sibling);
		}
		op.runOperation(monitor);
		return (IField) op.getResultElements()[0];
	}

	/**
	 * @see IType
	 */
	public IFunction createMethod(String contents, IJavaScriptElement sibling, boolean force, IProgressMonitor monitor)
			throws JavaScriptModelException {
		CreateMethodOperation op = new CreateMethodOperation(this, contents, force);
		if(sibling != null) {
			op.createBefore(sibling);
		}
		op.runOperation(monitor);
		return (IFunction) op.getResultElements()[0];
	}

	/**
	 * @see IType
	 */
	public IType createType(String contents, IJavaScriptElement sibling, boolean force, IProgressMonitor monitor)
			throws JavaScriptModelException {
		CreateTypeOperation op = new CreateTypeOperation(this, contents, force);
		if(sibling != null) {
			op.createBefore(sibling);
		}
		op.runOperation(monitor);
		return (IType) op.getResultElements()[0];
	}

	public boolean equals(Object o) {
		if(!(o instanceof SourceType))
			return false;
		return super.equals(o);
	}

	/* @see IType */
	public IFunction[] findMethods(IFunction method) {
		try {
			return findMethods(method, getFunctions());
		} catch(JavaScriptModelException e) {
			// if type doesn't exist, no matching method can exist
			return null;
		}
	}

	public IJavaScriptElement[] getChildrenForCategory(String category) throws JavaScriptModelException {
		IJavaScriptElement[] children = getChildren();
		int length = children.length;
		if(length == 0)
			return NO_ELEMENTS;
		SourceTypeElementInfo info = (SourceTypeElementInfo) getElementInfo();
		HashMap categories = info.getCategories();
		if(categories == null)
			return NO_ELEMENTS;
		IJavaScriptElement[] result = new IJavaScriptElement[length];
		int index = 0;
		for(int i = 0; i < length; i++) {
			IJavaScriptElement child = children[i];
			String[] elementCategories = (String[]) categories.get(child);
			if(elementCategories != null)
				for(int j = 0, length2 = elementCategories.length; j < length2; j++) {
					if(elementCategories[j].equals(category))
						result[index++] = child;
				}
		}
		if(index == 0)
			return NO_ELEMENTS;
		if(index < length)
			System.arraycopy(result, 0, result = new IJavaScriptElement[index], 0, index);
		return result;
	}

	/**
	 * @see IMember
	 */
	public IType getDeclaringType() {
		IJavaScriptElement parentElement = getParent();
		while(parentElement != null) {
			if(parentElement.getElementType() == IJavaScriptElement.TYPE) {
				return (IType) parentElement;
			} else if(parentElement instanceof IMember) {
				parentElement = parentElement.getParent();
			} else {
				return null;
			}
		}
		return null;
	}

	/**
	 * @see IJavaScriptElement
	 */
	public int getElementType() {
		return TYPE;
	}

	/**
	 * @see IType#getField
	 */
	public IField getField(String fieldName) {
		return new SourceField(this, fieldName);
	}

	/**
	 * @see IType
	 */
	public IField[] getFields() throws JavaScriptModelException {
		ArrayList list = getChildrenOfType(FIELD);
		IField[] array = new IField[list.size()];
		list.toArray(array);
		return array;
	}

	/**
	 * @see IType#getFullyQualifiedName()
	 */
	public String getFullyQualifiedName() {
		return this.getFullyQualifiedName('$');
	}

	/**
	 * @see IType#getFullyQualifiedName(char)
	 */
	public String getFullyQualifiedName(char enclosingTypeSeparator) {
		try {
			return getFullyQualifiedName(enclosingTypeSeparator, false/* don't show parameters */);
		} catch(JavaScriptModelException e) {
			// exception thrown only when showing parameters
			return null;
		}
	}

	/* @see IType#getFullyQualifiedParameterizedName() */
	public String getFullyQualifiedParameterizedName() throws JavaScriptModelException {
		return getFullyQualifiedName('.', true/* show parameters */);
	}

	/* @see JavaElement */
	public IJavaScriptElement getHandleFromMemento(String token, MementoTokenizer memento,
			WorkingCopyOwner workingCopyOwner) {
		switch(token.charAt(0)) {
			case JEM_COUNT:
				return getHandleUpdatingCountFromMemento(memento, workingCopyOwner);
			case JEM_FIELD:
				if(!memento.hasMoreTokens())
					return this;
				String fieldName = memento.nextToken();
				JavaElement field = (JavaElement) getField(fieldName);
				return field.getHandleFromMemento(memento, workingCopyOwner);
			case JEM_INITIALIZER:
				if(!memento.hasMoreTokens())
					return this;
				String count = memento.nextToken();
				JavaElement initializer = (JavaElement) getInitializer(Integer.parseInt(count));
				return initializer.getHandleFromMemento(memento, workingCopyOwner);
			case JEM_METHOD:
				if(!memento.hasMoreTokens())
					return this;
				String selector = memento.nextToken();
				ArrayList params = new ArrayList();
				nextParam: while(memento.hasMoreTokens()) {
					token = memento.nextToken();
					switch(token.charAt(0)) {
						case JEM_TYPE:
						case JEM_TYPE_PARAMETER:
							break nextParam;
						case JEM_METHOD:
							if(!memento.hasMoreTokens())
								return this;
							String param = memento.nextToken();
							StringBuffer buffer = new StringBuffer();
							while(param.length() == 1 && Signature.C_ARRAY == param.charAt(0)) { // backward
																									// compatible
																									// with
																									// 3.0
																									// mementos
								buffer.append(Signature.C_ARRAY);
								if(!memento.hasMoreTokens())
									return this;
								param = memento.nextToken();
							}
							params.add(buffer.toString() + param);
							break;
						default:
							break nextParam;
					}
				}
				String[] parameters = new String[params.size()];
				params.toArray(parameters);
				JavaElement method = (JavaElement) getFunction(selector, parameters);
				switch(token.charAt(0)) {
					case JEM_TYPE:
					case JEM_TYPE_PARAMETER:
					case JEM_LOCALVARIABLE:
						return method.getHandleFromMemento(token, memento, workingCopyOwner);
					default:
						return method;
				}
			case JEM_TYPE:
				String typeName;
				if(memento.hasMoreTokens()) {
					typeName = memento.nextToken();
					char firstChar = typeName.charAt(0);
					if(firstChar == JEM_FIELD || firstChar == JEM_INITIALIZER || firstChar == JEM_METHOD
							|| firstChar == JEM_TYPE || firstChar == JEM_COUNT) {
						token = typeName;
						typeName = ""; //$NON-NLS-1$
					} else {
						token = null;
					}
				} else {
					typeName = ""; //$NON-NLS-1$
					token = null;
				}
				JavaElement type = (JavaElement) getType(typeName);
				if(token == null) {
					return type.getHandleFromMemento(memento, workingCopyOwner);
				} else {
					return type.getHandleFromMemento(token, memento, workingCopyOwner);
				}

		}
		return null;
	}

	/**
	 * @see IType
	 */
	public IInitializer getInitializer(int count) {
		return new Initializer(this, count);
	}

	/**
	 * @see IType
	 */
	public IInitializer[] getInitializers() throws JavaScriptModelException {
		ArrayList list = getChildrenOfType(INITIALIZER);
		IInitializer[] array = new IInitializer[list.size()];
		list.toArray(array);
		return array;
	}

	/* (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.jsdt.core.IType#getKey() */
	public String getKey() {
		try {
			return getKey(this, false/* don't open */);
		} catch(JavaScriptModelException e) {
			// happen only if force open is true
			return null;
		}
	}

	/**
	 * @see IType#getMethod
	 */
	public IFunction getFunction(String selector, String[] parameterTypeSignatures) {
		return new SourceMethod(this, selector, parameterTypeSignatures);
	}

	/**
	 * @see IType
	 */
	public IFunction[] getFunctions() throws JavaScriptModelException {
		ArrayList list = getChildrenOfType(METHOD);
		IFunction[] array = new IFunction[list.size()];
		list.toArray(array);
		return array;
	}

	/**
	 * @see IType
	 */
	public IPackageFragment getPackageFragment() {
		IJavaScriptElement parentElement = this.parent;
		while(parentElement != null) {
			if(parentElement.getElementType() == IJavaScriptElement.PACKAGE_FRAGMENT) {
				return (IPackageFragment) parentElement;
			} else {
				parentElement = parentElement.getParent();
			}
		}
		Assert.isTrue(false); // should not happen
		return null;
	}

	/* @see JavaElement#getPrimaryElement(boolean) */
	public IJavaScriptElement getPrimaryElement(boolean checkOwner) {
		if(checkOwner) {
			CompilationUnit cu = (CompilationUnit) getAncestor(JAVASCRIPT_UNIT);
			if(cu.isPrimary())
				return this;
		}
		IJavaScriptElement primaryParent = this.parent.getPrimaryElement(false);
		switch(primaryParent.getElementType()) {
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				return ((IJavaScriptUnit) primaryParent).getType(this.name);
			case IJavaScriptElement.TYPE:
				return ((IType) primaryParent).getType(this.name);
			case IJavaScriptElement.FIELD:
			case IJavaScriptElement.INITIALIZER:
			case IJavaScriptElement.METHOD:
				return ((IMember) primaryParent).getType(this.name, this.occurrenceCount);
		}
		return this;
	}

	/**
	 * @see IType
	 */
	public String getSuperclassName() throws JavaScriptModelException {
		SourceTypeElementInfo info = (SourceTypeElementInfo) getElementInfo();
		char[] superclassName = info.getSuperclassName();
		if(superclassName == null) {
			return null;
		}
		return new String(superclassName);
	}

	/**
	 * @see IType#getSuperclassTypeSignature()
	 * @since 3.0
	 */
	public String getSuperclassTypeSignature() throws JavaScriptModelException {
		SourceTypeElementInfo info = (SourceTypeElementInfo) getElementInfo();
		char[] superclassName = info.getSuperclassName();
		if(superclassName == null) {
			return null;
		}
		return new String(Signature.createTypeSignature(superclassName, false));
	}

	/**
	 * @see IType
	 */
	public IType getType(String typeName) {
		return new SourceType(this, typeName);
	}

	/**
	 * @see IType#getTypeQualifiedName()
	 */
	public String getTypeQualifiedName() {
		return this.getTypeQualifiedName('$');
	}

	/**
	 * @see IType#getTypeQualifiedName(char)
	 */
	public String getTypeQualifiedName(char enclosingTypeSeparator) {
		try {
			return getTypeQualifiedName(enclosingTypeSeparator, false/* don't show parameters */);
		} catch(JavaScriptModelException e) {
			// exception thrown only when showing parameters
			return null;
		}
	}

	/**
	 * @see IType
	 */
	public IType[] getTypes() throws JavaScriptModelException {
		ArrayList list = getChildrenOfType(TYPE);
		IType[] array = new IType[list.size()];
		list.toArray(array);
		return array;
	}

	/**
	 * @see IType#isAnonymous()
	 */
	public boolean isAnonymous() {
		return this.fIsAnonymous || this.name.length() == 0 || this.name.startsWith(Util.ANONYMOUS_MARKER);
	}

	/**
	 * @see IType
	 */
	public boolean isClass() throws JavaScriptModelException {
		SourceTypeElementInfo info = (SourceTypeElementInfo) getElementInfo();
		return TypeDeclaration.kind(info.getModifiers()) == TypeDeclaration.CLASS_DECL;
	}

	/**
	 * @see IType#isLocal()
	 */
	public boolean isLocal() {
		switch(this.parent.getElementType()) {
			case IJavaScriptElement.METHOD:
			case IJavaScriptElement.INITIALIZER:
			case IJavaScriptElement.FIELD:
				return true;
			default:
				return false;
		}
	}

	/**
	 * @see IType#isMember()
	 */
	public boolean isMember() {
		return getDeclaringType() != null;
	}

	/* (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.jsdt.core.IType#isResolved() */
	public boolean isResolved() {
		return false;
	}

	/**
	 * @see IType
	 */
	public ITypeHierarchy loadTypeHierachy(InputStream input, IProgressMonitor monitor) throws JavaScriptModelException {
		return loadTypeHierachy(input, DefaultWorkingCopyOwner.PRIMARY, monitor);
	}

	/**
	 * NOTE: This method is not part of the API has it is not clear clients would easily use it:
	 * they would need to
	 * first make sure all working copies for the given owner exist before calling it. This is
	 * especially har at startup
	 * time.
	 * In case clients want this API, here is how it should be specified:
	 * <p>
	 * Loads a previously saved ITypeHierarchy from an input stream. A type hierarchy can be stored
	 * using ITypeHierachy#store(OutputStream). A compilation unit of a loaded type has the given
	 * owner if such a working copy exists, otherwise the type's compilation unit is a primary
	 * compilation unit.
	 * 
	 * Only hierarchies originally created by the following methods can be loaded:
	 * <ul>
	 * <li>IType#newSupertypeHierarchy(IProgressMonitor)</li>
	 * <li>IType#newSupertypeHierarchy(WorkingCopyOwner, IProgressMonitor)</li>
	 * <li>IType#newTypeHierarchy(IJavaScriptProject, IProgressMonitor)</li>
	 * <li>IType#newTypeHierarchy(IJavaScriptProject, WorkingCopyOwner, IProgressMonitor)</li>
	 * <li>IType#newTypeHierarchy(IProgressMonitor)</li>
	 * <li>IType#newTypeHierarchy(WorkingCopyOwner, IProgressMonitor)</li> </u>
	 * 
	 * @param input
	 *            stream where hierarchy will be read
	 * @param monitor
	 *            the given progress monitor
	 * @return the stored hierarchy
	 * @exception JavaScriptModelException
	 *                if the hierarchy could not be restored, reasons include: - type is not the
	 *                focus of the hierarchy or - unable to read the input stream (wrong format,
	 *                IOException during reading, ...)
	 * @see ITypeHierarchy#store(java.io.OutputStream, IProgressMonitor)
	 * @since 3.0
	 */
	public ITypeHierarchy loadTypeHierachy(InputStream input, WorkingCopyOwner owner, IProgressMonitor monitor)
			throws JavaScriptModelException {
		// TODO monitor should be passed to TypeHierarchy.load(...)
		return TypeHierarchy.load(this, input, owner);
	}

	/**
	 * @see IType
	 */
	public ITypeHierarchy newSupertypeHierarchy(IProgressMonitor monitor) throws JavaScriptModelException {
		return this.newSupertypeHierarchy(DefaultWorkingCopyOwner.PRIMARY, monitor);
	}

	/* @see IType#newSupertypeHierarchy(IJavaScriptUnit[], IProgressMonitor) */
	public ITypeHierarchy newSupertypeHierarchy(IJavaScriptUnit[] workingCopies, IProgressMonitor monitor)
			throws JavaScriptModelException {

		CreateTypeHierarchyOperation op =
				new CreateTypeHierarchyOperation(this, workingCopies, SearchEngine.createWorkspaceScope(), false);
		op.runOperation(monitor);
		return op.getResult();
	}

	/**
	 * @see IType#newSupertypeHierarchy(WorkingCopyOwner, IProgressMonitor)
	 */
	public ITypeHierarchy newSupertypeHierarchy(WorkingCopyOwner owner, IProgressMonitor monitor)
			throws JavaScriptModelException {

		IJavaScriptUnit[] workingCopies =
				JavaModelManager.getJavaModelManager().getWorkingCopies(owner, true/* add primary
																					 * working
																					 * copies */);
		CreateTypeHierarchyOperation op =
				new CreateTypeHierarchyOperation(this, workingCopies, SearchEngine.createWorkspaceScope(), false);
		op.runOperation(monitor);
		return op.getResult();
	}

	/**
	 * @see IType
	 */
	public ITypeHierarchy newTypeHierarchy(IJavaScriptProject project, IProgressMonitor monitor)
			throws JavaScriptModelException {
		return newTypeHierarchy(project, DefaultWorkingCopyOwner.PRIMARY, monitor);
	}

	/**
	 * @see IType#newTypeHierarchy(IJavaScriptProject, WorkingCopyOwner, IProgressMonitor)
	 */
	public ITypeHierarchy newTypeHierarchy(IJavaScriptProject project, WorkingCopyOwner owner, IProgressMonitor monitor)
			throws JavaScriptModelException {
		if(project == null) {
			throw new IllegalArgumentException(Messages.hierarchy_nullProject);
		}
		IJavaScriptUnit[] workingCopies =
				JavaModelManager.getJavaModelManager().getWorkingCopies(owner, true/* add primary
																					 * working
																					 * copies */);
		IJavaScriptUnit[] projectWCs = null;
		if(workingCopies != null) {
			int length = workingCopies.length;
			projectWCs = new IJavaScriptUnit[length];
			int index = 0;
			for(int i = 0; i < length; i++) {
				IJavaScriptUnit wc = workingCopies[i];
				if(project.equals(wc.getJavaScriptProject())) {
					projectWCs[index++] = wc;
				}
			}
			if(index != length) {
				System.arraycopy(projectWCs, 0, projectWCs = new IJavaScriptUnit[index], 0, index);
			}
		}
		CreateTypeHierarchyOperation op = new CreateTypeHierarchyOperation(this, projectWCs, project, true);
		op.runOperation(monitor);
		return op.getResult();
	}

	/**
	 * @see IType
	 */
	public ITypeHierarchy newTypeHierarchy(IProgressMonitor monitor) throws JavaScriptModelException {
		CreateTypeHierarchyOperation op =
				new CreateTypeHierarchyOperation(this, null, SearchEngine.createWorkspaceScope(), true);
		op.runOperation(monitor);
		return op.getResult();
	}

	/* @see IType#newTypeHierarchy(IJavaScriptUnit[], IProgressMonitor) */
	public ITypeHierarchy newTypeHierarchy(IJavaScriptUnit[] workingCopies, IProgressMonitor monitor)
			throws JavaScriptModelException {

		CreateTypeHierarchyOperation op =
				new CreateTypeHierarchyOperation(this, workingCopies, SearchEngine.createWorkspaceScope(), true);
		op.runOperation(monitor);
		return op.getResult();
	}

	/**
	 * @see IType#newTypeHierarchy(WorkingCopyOwner, IProgressMonitor)
	 */
	public ITypeHierarchy newTypeHierarchy(WorkingCopyOwner owner, IProgressMonitor monitor)
			throws JavaScriptModelException {

		IJavaScriptUnit[] workingCopies =
				JavaModelManager.getJavaModelManager().getWorkingCopies(owner, true/* add primary
																					 * working
																					 * copies */);
		CreateTypeHierarchyOperation op =
				new CreateTypeHierarchyOperation(this, workingCopies, SearchEngine.createWorkspaceScope(), true);
		op.runOperation(monitor);
		return op.getResult();
	}

	public JavaElement resolved(Binding binding) {
		String name = null;
		char[] readableName = binding.readableName();
		if(readableName != null) {
			name = new String(readableName);
		} else {
			name = this.name;
		}
		
		SourceRefElement resolvedHandle =
				new ResolvedSourceType(this.parent, name, new String(binding.computeUniqueKey()));
		resolvedHandle.occurrenceCount = this.occurrenceCount;
		return resolvedHandle;
	}

	/**
	 * @see IType#resolveType(String)
	 */
	public String[][] resolveType(String typeName) throws JavaScriptModelException {
		return resolveType(typeName, DefaultWorkingCopyOwner.PRIMARY);
	}

	/**
	 * @see IType#resolveType(String, WorkingCopyOwner)
	 */
	public String[][] resolveType(String typeName, WorkingCopyOwner owner) throws JavaScriptModelException {
		ISourceType info = (ISourceType) getElementInfo();
		JavaProject project = (JavaProject) getJavaScriptProject();
		SearchableEnvironment environment = newSearchableNameEnvironment(owner);

		class TypeResolveRequestor implements ISelectionRequestor {
			String[][] answers = null;

			public void acceptType(char[] packageName, char[] fileName, char[] tName, int modifiers,
					boolean isDeclaration, char[] uniqueKey, int start, int end) {
				String[] answer = new String[] { new String(packageName), new String(tName) };
				if(this.answers == null) {
					this.answers = new String[][] { answer };
				} else {
					// grow
					int length = this.answers.length;
					System.arraycopy(this.answers, 0, this.answers = new String[length + 1][], 0, length);
					this.answers[length] = answer;
				}
			}

			public void acceptError(CategorizedProblem error) {
				// ignore
			}

			public void acceptField(char[] declaringTypePackageName, char[] fileName, char[] declaringTypeName,
					char[] fieldName, boolean isDeclaration, char[] uniqueKey, int start, int end) {
				// ignore
			}

			public void acceptMethod(char[] declaringTypePackageName, char[] fileName, char[] declaringTypeName,
					String enclosingDeclaringTypeSignature, char[] selector, char[][] parameterPackageNames,
					char[][] parameterTypeNames, String[] parameterSignatures, char[][] typeParameterNames,
					char[][][] typeParameterBoundNames, boolean isConstructor, boolean isDeclaration, char[] uniqueKey,
					int start, int end) {
				// ignore
			}

			public void acceptPackage(char[] packageName) {
				// ignore
			}

			public void acceptTypeParameter(char[] declaringTypePackageName, char[] fileName, char[] declaringTypeName,
					char[] typeParameterName, boolean isDeclaration, int start, int end) {
				// ignore
			}

			public void acceptMethodTypeParameter(char[] declaringTypePackageName, char[] fileName,
					char[] declaringTypeName, char[] selector, int selectorStart, int selcetorEnd,
					char[] typeParameterName, boolean isDeclaration, int start, int end) {
				// ignore
			}

		}
		TypeResolveRequestor requestor = new TypeResolveRequestor();
		SelectionEngine engine = new SelectionEngine(environment, requestor, project.getOptions(true));

		IType[] topLevelTypes = getJavaScriptUnit().getTypes();
		int length = topLevelTypes.length;
		SourceTypeElementInfo[] topLevelInfos = new SourceTypeElementInfo[length];
		for(int i = 0; i < length; i++) {
			topLevelInfos[i] = (SourceTypeElementInfo) ((SourceType) topLevelTypes[i]).getElementInfo();
		}

		engine.selectType(info, typeName.toCharArray(), topLevelInfos, false);
		if(NameLookup.VERBOSE) {
			System.out.println(Thread.currentThread()
					+ " TIME SPENT in NameLoopkup#seekTypesInSourcePackage: " + environment.nameLookup.timeSpentInSeekTypesInSourcePackage + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(Thread.currentThread()
					+ " TIME SPENT in NameLoopkup#seekTypesInBinaryPackage: " + environment.nameLookup.timeSpentInSeekTypesInBinaryPackage + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return requestor.answers;
	}

	/* @GINO: Anonymous ??? maybe don't need */
	public String getDisplayName() {
		if(isAnonymous())
			return ""; //$NON-NLS-1$
		else
			return super.getDisplayName();
	}
	
	/**
	 * <p>
	 * This implementation will search for info for this types synonyms if
	 * info for this type can not be found.
	 * </p>
	 * 
	 * @see org.eclipse.wst.jsdt.internal.core.JavaElement#getElementInfo(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Object getElementInfo(IProgressMonitor monitor) throws JavaScriptModelException {
		Object info = null;
		
		//try to find the element info using this types name
		try {
			info = super.getElementInfo(monitor);
		} catch(JavaScriptModelException e) {
			//ignore, means it could not be found
		}
		
		// if could not find info using this type name try using synonym names
		if(info == null) {
			char[][] synonyms = SearchEngine.getAllSynonyms(this.name.toCharArray(),
						SearchEngine.createJavaSearchScope(new IJavaScriptElement[] { this.getJavaScriptProject() }),
						IJavaScriptSearchConstants.FORCE_IMMEDIATE_SEARCH, monitor);
			
			/* for each synonym see if its info can be found, return the first one that can be found
			 * skip the first one because the first one is the original name */
			for(int i = 1; i < synonyms.length && info == null; ++i) {
				try {
					SourceType synonymType = new SourceType(this.parent, new String(synonyms[i]));
					info = synonymType.getElementInfo(monitor, false);
				} catch(JavaScriptModelException e) {
					//ignore, means it could not be found
				}
			}
		}
		
		//really could not find it, expected thing to do in that case is throw an exception
		if(info == null) {
			throw newNotPresentException();
		}
		
		return info;
	}
	
	/**
	 * <p>
	 * Gets the element info for this type with the option to use the types
	 * synonyms or not.
	 * </p>
	 * 
	 * @param monitor
	 *            {@link IProgressMonitor} to track the progress of getting
	 *            the element info
	 * @param searchSynonyms
	 *            <code>true</code> to use the types synonyms if info can not
	 *            be found for this type <code>false</code> otherwise
	 * 
	 * @return element info for this type, or possibly one of its synonyms if
	 *         info for this type can not be found and
	 *         <code>searchSynonyms</code> is <code>true</code>
	 * 
	 * @throws JavaScriptModelException
	 *             if element info can not be found
	 * 
	 * @see #getElementInfo(IProgressMonitor)
	 */
	private Object getElementInfo(IProgressMonitor monitor, boolean searchSynonyms) throws JavaScriptModelException {
		return searchSynonyms ? this.getElementInfo(monitor) : super.getElementInfo(monitor);
	}

	/**
	 * @private Debugging purposes
	 */
	protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
		buffer.append(tabString(tab));
		if(info == null) {
			String elementName = getElementName();
			if(elementName.length() == 0) {
				buffer.append("<anonymous #"); //$NON-NLS-1$
				buffer.append(this.occurrenceCount);
				buffer.append(">"); //$NON-NLS-1$
			} else {
				toStringName(buffer);
			}
			buffer.append(" (not open)"); //$NON-NLS-1$
		} else if(info == NO_INFO) {
			String elementName = getElementName();
			if(elementName.length() == 0) {
				buffer.append("<anonymous #"); //$NON-NLS-1$
				buffer.append(this.occurrenceCount);
				buffer.append(">"); //$NON-NLS-1$
			} else {
				toStringName(buffer);
			}
		} else {
			buffer.append("class "); //$NON-NLS-1$

			String elementName = getElementName();
			if(elementName.length() == 0) {
				buffer.append("<anonymous #"); //$NON-NLS-1$
				buffer.append(this.occurrenceCount);
				buffer.append(">"); //$NON-NLS-1$
			} else {
				toStringName(buffer);
			}
		}
	}
}
