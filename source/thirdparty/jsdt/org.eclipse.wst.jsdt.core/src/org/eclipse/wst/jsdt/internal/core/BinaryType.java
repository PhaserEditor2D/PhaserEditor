/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.wst.jsdt.core.CompletionRequestor;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IInitializer;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.internal.codeassist.CompletionEngine;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.env.IBinaryType;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.util.SuffixConstants;
import org.eclipse.wst.jsdt.internal.core.JavaModelManager.PerProjectInfo;
import org.eclipse.wst.jsdt.internal.core.hierarchy.TypeHierarchy;
import org.eclipse.wst.jsdt.internal.core.util.MementoTokenizer;
import org.eclipse.wst.jsdt.internal.core.util.Messages;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * Parent is an IClassFile.
 *
 * @see IType
 */

public class BinaryType extends BinaryMember implements IType, SuffixConstants {

	private static final IField[] NO_FIELDS = new IField[0];
	private static final IFunction[] NO_METHODS = new IFunction[0];
	private static final IType[] NO_TYPES = new IType[0];
	private static final IInitializer[] NO_INITIALIZERS = new IInitializer[0];
	public static final String EMPTY_JAVADOC = org.eclipse.wst.jsdt.internal.compiler.util.Util.EMPTY_STRING;

protected BinaryType(JavaElement parent, String name) {
	super(parent, name);
}
/*
 * Remove my cached children from the Java Model
 */
protected void closing(Object info) throws JavaScriptModelException {
	ClassFileInfo cfi = getClassFileInfo();
	cfi.removeBinaryChildren();
}

/*
 * @see IType#codeComplete(char[], int, int, char[][], char[][], int[], boolean, ICompletionRequestor)
 */
public void codeComplete(char[] snippet,int insertion,int position,char[][] localVariableTypeNames,char[][] localVariableNames,int[] localVariableModifiers,boolean isStatic,CompletionRequestor requestor) throws JavaScriptModelException {
	codeComplete(snippet, insertion, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic, requestor, DefaultWorkingCopyOwner.PRIMARY);
}

/*
 * @see IType#codeComplete(char[], int, int, char[][], char[][], int[], boolean, ICompletionRequestor, WorkingCopyOwner)
 */
public void codeComplete(char[] snippet,int insertion,int position,char[][] localVariableTypeNames,char[][] localVariableNames,int[] localVariableModifiers,boolean isStatic,CompletionRequestor requestor, WorkingCopyOwner owner) throws JavaScriptModelException {
	if (requestor == null) {
		throw new IllegalArgumentException("Completion requestor cannot be null"); //$NON-NLS-1$
	}
	JavaProject project = (JavaProject) getJavaScriptProject();
	SearchableEnvironment environment = newSearchableNameEnvironment(owner);
	CompletionEngine engine = new CompletionEngine(environment, requestor, project.getOptions(true), project);

	String source = getClassFile().getSource();
	if (source != null && insertion > -1 && insertion < source.length()) {
		// code complete

		char[] prefix = CharOperation.concat(source.substring(0, insertion).toCharArray(), new char[]{'{'});
		char[] suffix =  CharOperation.concat(new char[]{'}'}, source.substring(insertion).toCharArray());
		char[] fakeSource = CharOperation.concat(prefix, snippet, suffix);

		BasicCompilationUnit cu =
			new BasicCompilationUnit(
				fakeSource,
				null,
				getElementName(),
				project); // use project to retrieve corresponding .js IFile

		engine.complete(cu, prefix.length + position, prefix.length);
	} else {
		engine.complete(this, snippet, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic);
	}
	if (NameLookup.VERBOSE) {
		System.out.println(Thread.currentThread() + " TIME SPENT in NameLoopkup#seekTypesInSourcePackage: " + environment.nameLookup.timeSpentInSeekTypesInSourcePackage + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
		System.out.println(Thread.currentThread() + " TIME SPENT in NameLoopkup#seekTypesInBinaryPackage: " + environment.nameLookup.timeSpentInSeekTypesInBinaryPackage + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
	}
}

/*
 * @see IType#createField(String, IJavaScriptElement, boolean, IProgressMonitor)
 */
public IField createField(String contents, IJavaScriptElement sibling, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
	throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.READ_ONLY, this));
}
/*
 * @see IType#createInitializer(String, IJavaScriptElement, IProgressMonitor)
 */
public IInitializer createInitializer(String contents, IJavaScriptElement sibling, IProgressMonitor monitor) throws JavaScriptModelException {
	throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.READ_ONLY, this));
}
/*
 * @see IType#createMethod(String, IJavaScriptElement, boolean, IProgressMonitor)
 */
public IFunction createMethod(String contents, IJavaScriptElement sibling, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
	throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.READ_ONLY, this));
}
/*
 * @see IType#createType(String, IJavaScriptElement, boolean, IProgressMonitor)
 */
public IType createType(String contents, IJavaScriptElement sibling, boolean force, IProgressMonitor monitor) throws JavaScriptModelException {
	throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.READ_ONLY, this));
}
public boolean equals(Object o) {
	if (!(o instanceof BinaryType)) return false;
	return super.equals(o);
}
/*
 * @see IType#findMethods(IFunction)
 */
public IFunction[] findMethods(IFunction method) {
	try {
		return findMethods(method, getFunctions());
	} catch (JavaScriptModelException e) {
		// if type doesn't exist, no matching method can exist
		return null;
	}
}
/*
 * @see IParent#getChildren()
 */
public IJavaScriptElement[] getChildren() throws JavaScriptModelException {
	ClassFileInfo cfi = getClassFileInfo();
	return cfi.binaryChildren;
}
public IJavaScriptElement[] getChildrenForCategory(String category) throws JavaScriptModelException {
	IJavaScriptElement[] children = getChildren();
	int length = children.length;
	if (length == 0) return children;
	SourceMapper mapper= getSourceMapper();
	if (mapper != null) {
		// ensure the class file's buffer is open so that categories are computed
		((ClassFile)getClassFile()).getBuffer();

		HashMap categories = mapper.categories;
		IJavaScriptElement[] result = new IJavaScriptElement[length];
		int index = 0;
		if (categories != null) {
			for (int i = 0; i < length; i++) {
				IJavaScriptElement child = children[i];
				String[] cats = (String[]) categories.get(child);
				if (cats != null) {
					for (int j = 0, length2 = cats.length; j < length2; j++) {
						if (cats[j].equals(category)) {
							result[index++] = child;
							break;
						}
					}
				}
			}
		}
		if (index < length)
			System.arraycopy(result, 0, result = new IJavaScriptElement[index], 0, index);
		return result;
	}
	return NO_ELEMENTS;
}
protected ClassFileInfo getClassFileInfo() throws JavaScriptModelException {
	ClassFile cf = (ClassFile)this.parent;
	return (ClassFileInfo) cf.getElementInfo();
}
/*
 * @see IMember#getDeclaringType()
 */
public IType getDeclaringType() {
	IClassFile classFile = this.getClassFile();
	if (classFile.isOpen()) {
		try {
			char[] enclosingTypeName = ((IBinaryType) getElementInfo()).getEnclosingTypeName();
			if (enclosingTypeName == null) {
				return null;
			}
		 	enclosingTypeName = ClassFile.unqualifiedName(enclosingTypeName);

			// workaround problem with class files compiled with javac 1.1.*
			// that return a non-null enclosing type name for local types defined in anonymous (e.g. A$1$B)
			if (classFile.getElementName().length() > enclosingTypeName.length+1
					&& Character.isDigit(classFile.getElementName().charAt(enclosingTypeName.length+1))) {
				return null;
			}

			return getPackageFragment().getClassFile(new String(enclosingTypeName) + SUFFIX_STRING_java).getType();
		} catch (JavaScriptModelException npe) {
			return null;
		}
	} else {
		// cannot access .class file without opening it
		// and getDeclaringType() is supposed to be a handle-only method,
		// so default to assuming $ is an enclosing type separator
		String classFileName = classFile.getElementName();
		int lastDollar = -1;
		for (int i = 0, length = classFileName.length(); i < length; i++) {
			char c = classFileName.charAt(i);
			if (Character.isDigit(c) && lastDollar == i-1) {
				// anonymous or local type
				return null;
			} else if (c == '$') {
				lastDollar = i;
			}
		}
		if (lastDollar == -1) {
			return null;
		} else {
			String enclosingName = classFileName.substring(0, lastDollar);
			String enclosingClassFileName = enclosingName + SUFFIX_STRING_java;
			return
				new BinaryType(
					(JavaElement)this.getPackageFragment().getClassFile(enclosingClassFileName),
					Util.localTypeName(enclosingName, enclosingName.lastIndexOf('$'), enclosingName.length()));
		}
	}
}
public Object getElementInfo(IProgressMonitor monitor) throws JavaScriptModelException {
	JavaModelManager manager = JavaModelManager.getJavaModelManager();
	Object info = manager.getInfo(this);
	if (info != null && info != JavaModelCache.NON_EXISTING_JAR_TYPE_INFO) return info;
	return openWhenClosed(createElementInfo(), monitor);
}
/*
 * @see IJavaScriptElement
 */
public int getElementType() {
	return TYPE;
}
/*
 * @see IType#getField(String name)
 */
public IField getField(String fieldName) {
	return new BinaryField(this, fieldName);
}
/*
 * @see IType#getFields()
 */
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
/*
 * @see IMember#getFlags()
 */
public int getFlags() throws JavaScriptModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	return info.getModifiers();
}
/*
 * @see IType#getFullyQualifiedName()
 */
public String getFullyQualifiedName() {
	return this.getFullyQualifiedName('$');
}
/*
 * @see IType#getFullyQualifiedName(char enclosingTypeSeparator)
 */
public String getFullyQualifiedName(char enclosingTypeSeparator) {
	try {
		return getFullyQualifiedName(enclosingTypeSeparator, false/*don't show parameters*/);
	} catch (JavaScriptModelException e) {
		// exception thrown only when showing parameters
		return null;
	}
}

/*
 * @see IType#getFullyQualifiedParameterizedName()
 */
public String getFullyQualifiedParameterizedName() throws JavaScriptModelException {
	return getFullyQualifiedName('.', true/*show parameters*/);
}

/*
 * @see JavaElement
 */
public IJavaScriptElement getHandleFromMemento(String token, MementoTokenizer memento, WorkingCopyOwner workingCopyOwner) {
	switch (token.charAt(0)) {
		case JEM_COUNT:
			return getHandleUpdatingCountFromMemento(memento, workingCopyOwner);
		case JEM_FIELD:
			if (!memento.hasMoreTokens()) return this;
			String fieldName = memento.nextToken();
			JavaElement field = (JavaElement)getField(fieldName);
			return field.getHandleFromMemento(memento, workingCopyOwner);
		case JEM_INITIALIZER:
			if (!memento.hasMoreTokens()) return this;
			String count = memento.nextToken();
			JavaElement initializer = (JavaElement)getInitializer(Integer.parseInt(count));
			return initializer.getHandleFromMemento(memento, workingCopyOwner);
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
					case JEM_METHOD:
						if (!memento.hasMoreTokens()) return this;
						String param = memento.nextToken();
						StringBuffer buffer = new StringBuffer();
						while (param.length() == 1 && Signature.C_ARRAY == param.charAt(0)) { // backward compatible with 3.0 mementos
							buffer.append(Signature.C_ARRAY);
							if (!memento.hasMoreTokens()) return this;
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
			JavaElement method = (JavaElement)getFunction(selector, parameters);
			switch (token.charAt(0)) {
				case JEM_TYPE:
				case JEM_TYPE_PARAMETER:
				case JEM_LOCALVARIABLE:
					return method.getHandleFromMemento(token, memento, workingCopyOwner);
				default:
					return method;
			}
		case JEM_TYPE:
			String typeName;
			if (memento.hasMoreTokens()) {
				typeName = memento.nextToken();
				char firstChar = typeName.charAt(0);
				if (firstChar == JEM_FIELD || firstChar == JEM_INITIALIZER || firstChar == JEM_METHOD || firstChar == JEM_TYPE || firstChar == JEM_COUNT) {
					token = typeName;
					typeName = ""; //$NON-NLS-1$
				} else {
					token = null;
				}
			} else {
				typeName = ""; //$NON-NLS-1$
				token = null;
			}
			JavaElement type = (JavaElement)getType(typeName);
			if (token == null) {
				return type.getHandleFromMemento(memento, workingCopyOwner);
			} else {
				return type.getHandleFromMemento(token, memento, workingCopyOwner);
			}
		case JEM_TYPE_PARAMETER:
			if (!memento.hasMoreTokens()) return this;
//			String typeParameterName = memento.nextToken();
//			JavaElement typeParameter = null;
//			return typeParameter.getHandleFromMemento(memento, workingCopyOwner);
	}
	return null;
}
/*
 * @see IType#getInitializer(int occurrenceCount)
 */
public IInitializer getInitializer(int count) {
	return new Initializer(this, count);
}
/*
 * @see IType#getInitializers()
 */
public IInitializer[] getInitializers() {
	return NO_INITIALIZERS;
}
public String getKey(boolean forceOpen) throws JavaScriptModelException {
	return getKey(this, forceOpen);
}
/*
 * @see IType#getMethod(String name, String[] parameterTypeSignatures)
 */
public IFunction getFunction(String selector, String[] parameterTypeSignatures) {
	return new BinaryMethod(this, selector, parameterTypeSignatures);
}

/*
 * @see IType#getMethods()
 */
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
/*
 * @see IType#getPackageFragment()
 */
public IPackageFragment getPackageFragment() {
	IJavaScriptElement parentElement = this.parent;
	while (parentElement != null) {
		if (parentElement.getElementType() == IJavaScriptElement.PACKAGE_FRAGMENT) {
			return (IPackageFragment)parentElement;
		}
		else {
			parentElement = parentElement.getParent();
		}
	}
	Assert.isTrue(false);  // should not happen
	return null;
}

/**
 * @see IType#getSuperclassTypeSignature()
 * @since 3.0
 */
public String getSuperclassTypeSignature() throws JavaScriptModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	char[] genericSignature = info.getGenericSignature();
	if (genericSignature != null) {
		int signatureLength = genericSignature.length;
		// skip type parameters
		int index = 0;
		if (genericSignature[0] == '<') {
			int count = 1;
			while (count > 0 && ++index < signatureLength) {
				switch (genericSignature[index]) {
					case '<':
						count++;
						break;
					case '>':
						count--;
						break;
				}
			}
			index++;
		}
		int start = index;
		index = Util.scanClassTypeSignature(genericSignature, start) + 1;
		char[] superclassSig = CharOperation.subarray(genericSignature, start, index);
		return new String(ClassFile.translatedName(superclassSig));
	} else {
		char[] superclassName = info.getSuperclassName();
		if (superclassName == null) {
			return null;
		}
		return new String(Signature.createTypeSignature(ClassFile.translatedName(superclassName), true));
	}
}

public String getSourceFileName(IBinaryType info) {
	if (info == null) {
		try {
			info = (IBinaryType) getElementInfo();
		} catch (JavaScriptModelException e) {
			// default to using the outer most declaring type name
			IType type = this;
			IType enclosingType = getDeclaringType();
			while (enclosingType != null) {
				type = enclosingType;
				enclosingType = type.getDeclaringType();
			}
			return type.getElementName() + Util.defaultJavaExtension();
		}
	}
	return sourceFileName(info);
}

/*
 * @see IType#getSuperclassName()
 */
public String getSuperclassName() throws JavaScriptModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	char[] superclassName = info.getSuperclassName();
	if (superclassName == null) {
		return null;
	}
	return new String(ClassFile.translatedName(superclassName));
}

/**
 * @see IType#getTypeParameterSignatures()
 * @since 3.0
 */
public String[] getTypeParameterSignatures() throws JavaScriptModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	char[] genericSignature = info.getGenericSignature();
	if (genericSignature == null)
		return CharOperation.NO_STRINGS;

	return CharOperation.toStrings(CharOperation.NO_CHAR_CHAR);
}

/*
 * @see IType#getType(String)
 */
public IType getType(String typeName) {
	IClassFile classFile= getPackageFragment().getClassFile(getTypeQualifiedName() + "$" + typeName + SUFFIX_STRING_java); //$NON-NLS-1$
	return new BinaryType((JavaElement)classFile, typeName);
}

/*
 * @see IType#getTypeQualifiedName()
 */
public String getTypeQualifiedName() {
	return this.getTypeQualifiedName('$');
}
/*
 * @see IType#getTypeQualifiedName(char)
 */
public String getTypeQualifiedName(char enclosingTypeSeparator) {
	try {
		return getTypeQualifiedName(enclosingTypeSeparator, false/*don't show parameters*/);
	} catch (JavaScriptModelException e) {
		// exception thrown only when showing parameters
		return null;
	}
}
/*
 * @see IType#getTypes()
 */
public IType[] getTypes() throws JavaScriptModelException {
	ArrayList list = getChildrenOfType(TYPE);
	int size;
	if ((size = list.size()) == 0) {
		return NO_TYPES;
	} else {
		IType[] array= new IType[size];
		list.toArray(array);
		return array;
	}
}

/*
 * @see IType#isAnonymous()
 */
public boolean isAnonymous() throws JavaScriptModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	return info.isAnonymous();
}
/*
 * @see IType#isClass()
 */
public boolean isClass() throws JavaScriptModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	return TypeDeclaration.kind(info.getModifiers()) == TypeDeclaration.CLASS_DECL;

}

/*
 * @see IType#isLocal()
 */
public boolean isLocal() throws JavaScriptModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	return info.isLocal();
}
/*
 * @see IType#isMember()
 */
public boolean isMember() throws JavaScriptModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	return info.isMember();
}
/* (non-Javadoc)
 * @see org.eclipse.wst.jsdt.core.IType#isResolved()
 */
public boolean isResolved() {
	return false;
}
/*
 * @see IType
 */
public ITypeHierarchy loadTypeHierachy(InputStream input, IProgressMonitor monitor) throws JavaScriptModelException {
	return loadTypeHierachy(input, DefaultWorkingCopyOwner.PRIMARY, monitor);
}
/*
 * @see IType
 */
public ITypeHierarchy loadTypeHierachy(InputStream input, WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaScriptModelException {
	return TypeHierarchy.load(this, input, owner);
}
/*
 * @see IType#newSupertypeHierarchy(IProgressMonitor monitor)
 */
public ITypeHierarchy newSupertypeHierarchy(IProgressMonitor monitor) throws JavaScriptModelException {
	return this.newSupertypeHierarchy(DefaultWorkingCopyOwner.PRIMARY, monitor);
}
/*
 *@see IType#newSupertypeHierarchy(IJavaScriptUnit[], IProgressMonitor monitor)
 */
public ITypeHierarchy newSupertypeHierarchy(
	IJavaScriptUnit[] workingCopies,
	IProgressMonitor monitor)
	throws JavaScriptModelException {

	CreateTypeHierarchyOperation op= new CreateTypeHierarchyOperation(this, workingCopies, SearchEngine.createWorkspaceScope(), false);
	op.runOperation(monitor);
	return op.getResult();
}
/*
 * @see IType#newSupertypeHierarchy(WorkingCopyOwner, IProgressMonitor)
 */
public ITypeHierarchy newSupertypeHierarchy(
	WorkingCopyOwner owner,
	IProgressMonitor monitor)
	throws JavaScriptModelException {

	IJavaScriptUnit[] workingCopies = JavaModelManager.getJavaModelManager().getWorkingCopies(owner, true/*add primary working copies*/);
	CreateTypeHierarchyOperation op= new CreateTypeHierarchyOperation(this, workingCopies, SearchEngine.createWorkspaceScope(), false);
	op.runOperation(monitor);
	return op.getResult();
}
/*
 * @see IType#newTypeHierarchy(IJavaScriptProject, IProgressMonitor)
 */
public ITypeHierarchy newTypeHierarchy(IJavaScriptProject project, IProgressMonitor monitor) throws JavaScriptModelException {
	return newTypeHierarchy(project, DefaultWorkingCopyOwner.PRIMARY, monitor);
}
/*
 * @see IType#newTypeHierarchy(IJavaScriptProject, WorkingCopyOwner, IProgressMonitor)
 */
public ITypeHierarchy newTypeHierarchy(IJavaScriptProject project, WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaScriptModelException {
	if (project == null) {
		throw new IllegalArgumentException(Messages.hierarchy_nullProject);
	}
	IJavaScriptUnit[] workingCopies = JavaModelManager.getJavaModelManager().getWorkingCopies(owner, true/*add primary working copies*/);
	IJavaScriptUnit[] projectWCs = null;
	if (workingCopies != null) {
		int length = workingCopies.length;
		projectWCs = new IJavaScriptUnit[length];
		int index = 0;
		for (int i = 0; i < length; i++) {
			IJavaScriptUnit wc = workingCopies[i];
			if (project.equals(wc.getJavaScriptProject())) {
				projectWCs[index++] = wc;
			}
		}
		if (index != length) {
			System.arraycopy(projectWCs, 0, projectWCs = new IJavaScriptUnit[index], 0, index);
		}
	}
	CreateTypeHierarchyOperation op= new CreateTypeHierarchyOperation(
		this,
		projectWCs,
		project,
		true);
	op.runOperation(monitor);
	return op.getResult();
}
/**
 * @param monitor the given progress monitor
 * @exception JavaScriptModelException if this element does not exist or if an
 *		exception occurs while accessing its corresponding resource.
 * @return a type hierarchy for this type containing
 *
 * @see IType#newTypeHierarchy(IProgressMonitor monitor)
 * @deprecated
 */
public ITypeHierarchy newTypeHierarchy(IProgressMonitor monitor) throws JavaScriptModelException {
	return newTypeHierarchy((IJavaScriptUnit[])null, monitor);
}
/*
 * @see IType#newTypeHierarchy(IJavaScriptUnit[], IProgressMonitor)
 */
public ITypeHierarchy newTypeHierarchy(
	IJavaScriptUnit[] workingCopies,
	IProgressMonitor monitor)
	throws JavaScriptModelException {

	CreateTypeHierarchyOperation op= new CreateTypeHierarchyOperation(this, workingCopies, SearchEngine.createWorkspaceScope(), true);
	op.runOperation(monitor);
	return op.getResult();
}
/*
 * @see IType#newTypeHierarchy(WorkingCopyOwner, IProgressMonitor)
 */
public ITypeHierarchy newTypeHierarchy(
	WorkingCopyOwner owner,
	IProgressMonitor monitor)
	throws JavaScriptModelException {

	IJavaScriptUnit[] workingCopies = JavaModelManager.getJavaModelManager().getWorkingCopies(owner, true/*add primary working copies*/);
	CreateTypeHierarchyOperation op= new CreateTypeHierarchyOperation(this, workingCopies, SearchEngine.createWorkspaceScope(), true);
	op.runOperation(monitor);
	return op.getResult();
}
public JavaElement resolved(Binding binding) {
	SourceRefElement resolvedHandle = new ResolvedBinaryType(this.parent, this.name, new String(binding.computeUniqueKey()));
	resolvedHandle.occurrenceCount = this.occurrenceCount;
	return resolvedHandle;
}
/*
 * @see IType#resolveType(String)
 */
public String[][] resolveType(String typeName) {
	// not implemented for binary types
	return null;
}
/*
 * @see IType#resolveType(String, WorkingCopyOwner)
 */
public String[][] resolveType(String typeName, WorkingCopyOwner owner) {
	// not implemented for binary types
	return null;
}
/*
 * Returns the source file name as defined in the given info.
 * If not present in the info, infers it from this type.
 */
public String sourceFileName(IBinaryType info) {
	char[] sourceFileName = info.sourceFileName();
	if (sourceFileName == null) {
		/*
		 * We assume that this type has been compiled from a file with its name
		 * For example, A.class comes from A.js and p.A.class comes from a file A.js
		 * in the folder p.
		 */
		if (info.isMember()) {
			IType enclosingType = getDeclaringType();
			if (enclosingType == null) return null; // play it safe
			while (enclosingType.getDeclaringType() != null) {
				enclosingType = enclosingType.getDeclaringType();
			}
			return enclosingType.getElementName() + Util.defaultJavaExtension();
		} else if (info.isLocal() || info.isAnonymous()){
			String typeQualifiedName = getTypeQualifiedName();
			int dollar = typeQualifiedName.indexOf('$');
			if (dollar == -1) {
				// malformed inner type: name doesn't contain a dollar
				return getElementName() + Util.defaultJavaExtension();
			}
			return typeQualifiedName.substring(0, dollar) + Util.defaultJavaExtension();
		} else {
			return getElementName() + Util.defaultJavaExtension();
		}
	} else {
		return  new String(sourceFileName);
	}
}
/*
 * @private Debugging purposes
 */
protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
	buffer.append(this.tabString(tab));
	if (info == null) {
		toStringName(buffer);
		buffer.append(" (not open)"); //$NON-NLS-1$
	} else if (info == NO_INFO) {
		toStringName(buffer);
	} else {
		buffer.append("class "); //$NON-NLS-1$
		toStringName(buffer);
	}
}
protected void toStringName(StringBuffer buffer) {
	if (getElementName().length() > 0)
		super.toStringName(buffer);
	else
		buffer.append("<anonymous>"); //$NON-NLS-1$
}
public String getAttachedJavadoc(IProgressMonitor monitor) throws JavaScriptModelException {
	final String contents = getJavadocContents(monitor);
	if (contents == null) return null;
	final int indexOfStartOfClassData = contents.indexOf(JavadocConstants.START_OF_CLASS_DATA);
	if (indexOfStartOfClassData == -1) throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.UNKNOWN_JSDOC_FORMAT, this));
	int indexOfNextSummary = contents.indexOf(JavadocConstants.NESTED_CLASS_SUMMARY);
	
	if (indexOfNextSummary == -1) {
		// try to find field summary start
		indexOfNextSummary = contents.indexOf(JavadocConstants.FIELD_SUMMARY);
	}
	if (indexOfNextSummary == -1) {
		// try to find constructor summary start
		indexOfNextSummary = contents.indexOf(JavadocConstants.CONSTRUCTOR_SUMMARY);
	}
	if (indexOfNextSummary == -1) {
		// try to find method summary start
		indexOfNextSummary = contents.indexOf(JavadocConstants.METHOD_SUMMARY);
	}
	if (indexOfNextSummary == -1) {
		// we take the end of class data
		indexOfNextSummary = contents.indexOf(JavadocConstants.END_OF_CLASS_DATA);
	}
	if (indexOfNextSummary == -1) {
		throw new JavaScriptModelException(new JavaModelStatus(IJavaScriptModelStatusConstants.UNKNOWN_JSDOC_FORMAT, this));
	}
	/*
	 * Check out to cut off the hierarchy see 119844
	 * We remove what the contents between the start of class data and the first <P>
	 */
	int start = indexOfStartOfClassData + JavadocConstants.START_OF_CLASS_DATA_LENGTH;
	int indexOfFirstParagraph = contents.indexOf("<P>", start); //$NON-NLS-1$
	if (indexOfFirstParagraph == -1) {
		indexOfFirstParagraph = contents.indexOf("<p>", start); //$NON-NLS-1$
	}
	if (indexOfFirstParagraph != -1 && indexOfFirstParagraph < indexOfNextSummary) {
		start = indexOfFirstParagraph;
	}
	return contents.substring(start, indexOfNextSummary);
}
public String getJavadocContents(IProgressMonitor monitor) throws JavaScriptModelException {
	PerProjectInfo projectInfo = JavaModelManager.getJavaModelManager().getPerProjectInfoCheckExistence(this.getJavaScriptProject().getProject());
	String cachedJavadoc = null;
	synchronized (projectInfo.javadocCache) {
		cachedJavadoc = (String) projectInfo.javadocCache.get(this);
	}
	if (cachedJavadoc != null && cachedJavadoc != EMPTY_JAVADOC) {
		return cachedJavadoc;
	}
	URL baseLocation= getJavadocBaseLocation();
	if (baseLocation == null) {
		return null;
	}
	StringBuffer pathBuffer = new StringBuffer(baseLocation.toExternalForm());

	if (!(pathBuffer.charAt(pathBuffer.length() - 1) == '/')) {
		pathBuffer.append('/');
	}
	IPackageFragment pack= this.getPackageFragment();
	String typeQualifiedName = null;
	if (this.isMember()) {
		IType currentType = this;
		StringBuffer typeName = new StringBuffer();
		while (currentType != null) {
			typeName.insert(0, currentType.getElementName());
			currentType = currentType.getDeclaringType();
			if (currentType != null) {
				typeName.insert(0, '.');
			}
		}
		typeQualifiedName = new String(typeName.toString());
	} else {
		typeQualifiedName = this.getElementName();
	}

	pathBuffer.append(pack.getElementName().replace('.', '/')).append('/').append(typeQualifiedName).append(JavadocConstants.HTML_EXTENSION);

	if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
	final String contents = getURLContents(String.valueOf(pathBuffer));
	synchronized (projectInfo.javadocCache) {
		projectInfo.javadocCache.put(this, contents);
	}
	return contents;
}
}
