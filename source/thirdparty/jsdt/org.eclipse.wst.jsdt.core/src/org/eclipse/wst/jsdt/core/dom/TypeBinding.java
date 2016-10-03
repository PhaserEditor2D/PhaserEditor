/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.core.dom;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.env.IDependent;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.wst.jsdt.internal.compiler.util.SuffixConstants;
import org.eclipse.wst.jsdt.internal.compiler.util.Util;
import org.eclipse.wst.jsdt.internal.core.ClassFile;
import org.eclipse.wst.jsdt.internal.core.JavaElement;

/**
 * Internal implementation of type bindings.
 */
class TypeBinding implements ITypeBinding {
	protected static final IFunctionBinding[] NO_METHOD_BINDINGS = new IFunctionBinding[0];

	private static final String NO_NAME = ""; //$NON-NLS-1$
	protected static final ITypeBinding[] NO_TYPE_BINDINGS = new ITypeBinding[0];
	protected static final IVariableBinding[] NO_VARIABLE_BINDINGS = new IVariableBinding[0];

	private static final int VALID_MODIFIERS = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE |
		Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL | Modifier.STRICTFP;

	org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding binding;
	private String key;
	private BindingResolver resolver;
	private IVariableBinding[] fields;
	private IFunctionBinding[] methods;
	private ITypeBinding[] members;

	public TypeBinding(BindingResolver resolver, org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding binding) {
		this.binding = binding;
		this.resolver = resolver;
	}

	public ITypeBinding createArrayType(int dimension) {
		int realDimensions = dimension;
		realDimensions += this.getDimensions();
		if (realDimensions < 1 || realDimensions > 255) {
			throw new IllegalArgumentException();
		}
		return this.resolver.resolveArrayType(this, dimension);
	}


	/*
	 * @see ITypeBinding#getBinaryName()
	 *  
	 */
	public String getBinaryName() {
 		char[] constantPoolName = this.binding.constantPoolName();
		if (constantPoolName == null) return null;
		char[] dotSeparated = CharOperation.replaceOnCopy(constantPoolName, '/', '.');
		return new String(dotSeparated);
	}

	/*
	 * Returns the class file for the given file name, or null if not found.
	 * @see org.eclipse.wst.jsdt.internal.compiler.env.IDependent#getFileName()
	 */
	private IClassFile getClassFile(char[] fileName) {
		int jarSeparator = CharOperation.indexOf(IDependent.JAR_FILE_ENTRY_SEPARATOR, fileName);
		int pkgEnd = CharOperation.lastIndexOf('/', fileName); // pkgEnd is exclusive
		if (pkgEnd == -1)
			pkgEnd = CharOperation.lastIndexOf(File.separatorChar, fileName);
		if (jarSeparator != -1 && pkgEnd < jarSeparator) // if in a jar and no slash, it is a default package -> pkgEnd should be equal to jarSeparator
			pkgEnd = jarSeparator;
		if (pkgEnd == -1)
			return null;
		IPackageFragment pkg = getPackageFragment(fileName, pkgEnd, jarSeparator);
		if (pkg == null) return null;
		int start;
		return pkg.getClassFile(new String(fileName, start = pkgEnd + 1, fileName.length - start));
	}

	/*
	 * Returns the javaScript unit for the given file name, or null if not found.
	 * @see org.eclipse.wst.jsdt.internal.compiler.env.IDependent#getFileName()
	 */
	private IJavaScriptUnit getCompilationUnit(char[] fileName) {
		char[] slashSeparatedFileName = CharOperation.replaceOnCopy(fileName, File.separatorChar, '/');
		int pkgEnd = CharOperation.lastIndexOf('/', slashSeparatedFileName); // pkgEnd is exclusive
		if (pkgEnd == -1)
			return null;
		IPackageFragment pkg = getPackageFragment(slashSeparatedFileName, pkgEnd, -1/*no jar separator for .js files*/);
		if (pkg == null) return null;
		int start;
		IJavaScriptUnit cu = pkg.getJavaScriptUnit(new String(slashSeparatedFileName, start =  pkgEnd+1, slashSeparatedFileName.length - start));
		if (this.resolver instanceof DefaultBindingResolver) {
			IJavaScriptUnit workingCopy = cu.findWorkingCopy(((DefaultBindingResolver) this.resolver).workingCopyOwner);
			if (workingCopy != null)
				return workingCopy;
		}
		return cu;
	}

	/*
	 * @see ITypeBinding#getComponentType()
	 */
	public ITypeBinding getComponentType() {
		if (!this.isArray()) {
			return null;
		}
		ArrayBinding arrayBinding = (ArrayBinding) binding;
		return resolver.getTypeBinding(arrayBinding.elementsType());
	}

	/*
	 * @see ITypeBinding#getDeclaredFields()
	 */
	public synchronized IVariableBinding[] getDeclaredFields() {
		if (this.fields != null) {
			return this.fields;
		}
		try {
			if (isClass()) {
				ReferenceBinding referenceBinding = (ReferenceBinding) this.binding;
				FieldBinding[] fieldBindings = referenceBinding.availableFields(); // resilience
				int length = fieldBindings.length;
				if (length != 0) {
					IVariableBinding[] newFields = new IVariableBinding[length];
					for (int i = 0; i < length; i++) {
						IVariableBinding variableBinding = this.resolver.getVariableBinding(fieldBindings[i]);
						if (variableBinding == null) {
							return this.fields = NO_VARIABLE_BINDINGS;
						}
						newFields[i] = variableBinding;
					}
					return this.fields = newFields;
				}
			}
		} catch (RuntimeException e) {
			/* in case a method cannot be resolvable due to missing jars on the includepath
			 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=57871
			 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=63550
			 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=64299
			 */
			org.eclipse.wst.jsdt.internal.core.util.Util.log(e, "Could not retrieve declared fields"); //$NON-NLS-1$
		}
		return this.fields = NO_VARIABLE_BINDINGS;
	}

	/*
	 * @see ITypeBinding#getDeclaredMethods()
	 */
	public synchronized IFunctionBinding[] getDeclaredMethods() {
		if (this.methods != null) {
			return this.methods;
		}
		try {
			if (isClass()) {
				ReferenceBinding referenceBinding = (ReferenceBinding) this.binding;
				org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding[] internalMethods = referenceBinding.availableMethods(); // be resilient
				int length = internalMethods.length;
				if (length != 0) {
					int removeSyntheticsCounter = 0;
					IFunctionBinding[] newMethods = new IFunctionBinding[length];
					for (int i = 0; i < length; i++) {
						org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding methodBinding = internalMethods[i];
						if (!shouldBeRemoved(methodBinding)) {
							IFunctionBinding methodBinding2 = this.resolver.getMethodBinding(methodBinding);
							if (methodBinding2 != null) {
								newMethods[removeSyntheticsCounter++] = methodBinding2;
							}
						}
					}
					if (removeSyntheticsCounter != length) {
						System.arraycopy(newMethods, 0, (newMethods = new IFunctionBinding[removeSyntheticsCounter]), 0, removeSyntheticsCounter);
					}
					return this.methods = newMethods;
				}
			}
		} catch (RuntimeException e) {
			/* in case a method cannot be resolvable due to missing jars on the includepath
			 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=57871
			 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=63550
			 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=64299
			 */
			org.eclipse.wst.jsdt.internal.core.util.Util.log(e, "Could not retrieve declared methods"); //$NON-NLS-1$
		}
		return this.methods = NO_METHOD_BINDINGS;
	}

	/*
	 * @see ITypeBinding#getDeclaredModifiers()
	 */
	public int getDeclaredModifiers() {
		return getModifiers();
	}

	/*
	 * @see ITypeBinding#getDeclaredTypes()
	 */
	public synchronized ITypeBinding[] getDeclaredTypes() {
		if (this.members != null) {
			return this.members;
		}
		try {
			if (isClass()) {
				ReferenceBinding referenceBinding = (ReferenceBinding) this.binding;
				ReferenceBinding[] internalMembers = referenceBinding.memberTypes();
				int length = internalMembers.length;
				if (length != 0) {
					ITypeBinding[] newMembers = new ITypeBinding[length];
					for (int i = 0; i < length; i++) {
						ITypeBinding typeBinding = this.resolver.getTypeBinding(internalMembers[i]);
						if (typeBinding == null) {
							return this.members = NO_TYPE_BINDINGS;
						}
						newMembers[i] = typeBinding;
					}
					return this.members = newMembers;
				}
			}
		} catch (RuntimeException e) {
			/* in case a method cannot be resolvable due to missing jars on the includepath
			 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=57871
			 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=63550
			 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=64299
			 */
			org.eclipse.wst.jsdt.internal.core.util.Util.log(e, "Could not retrieve declared methods"); //$NON-NLS-1$
		}
		return this.members = NO_TYPE_BINDINGS;
	}

	/*
	 * @see ITypeBinding#getDeclaringMethod()
	 */
	public synchronized IFunctionBinding getDeclaringMethod() {
		if (this.binding instanceof LocalTypeBinding) {
			LocalTypeBinding localTypeBinding = (LocalTypeBinding) this.binding;
			MethodBinding methodBinding = localTypeBinding.enclosingMethod;
			if (methodBinding != null) {
				try {
					return this.resolver.getMethodBinding(localTypeBinding.enclosingMethod);
				} catch (RuntimeException e) {
					/* in case a method cannot be resolvable due to missing jars on the includepath
					 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=57871
					 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=63550
					 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=64299
					 */
					org.eclipse.wst.jsdt.internal.core.util.Util.log(e, "Could not retrieve declaring method"); //$NON-NLS-1$
				}
			}
		}
		return null;
	}

	/*
	 * @see ITypeBinding#getDeclaringClass()
	 */
	public synchronized ITypeBinding getDeclaringClass() {
		if (isClass()) {
			ReferenceBinding referenceBinding = (ReferenceBinding) this.binding;
			if (referenceBinding.isNestedType()) {
				try {
					return this.resolver.getTypeBinding(referenceBinding.enclosingType());
				} catch (RuntimeException e) {
					/* in case a method cannot be resolvable due to missing jars on the includepath
					 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=57871
					 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=63550
					 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=64299
					 */
					org.eclipse.wst.jsdt.internal.core.util.Util.log(e, "Could not retrieve declaring class"); //$NON-NLS-1$
				}
			}
		}
		return null;
	}

	/*
	 * @see ITypeBinding#getDimensions()
	 */
	public int getDimensions() {
		if (!this.isArray()) {
			return 0;
		}
		ArrayBinding arrayBinding = (ArrayBinding) binding;
		return arrayBinding.dimensions;
	}

	/*
	 * @see ITypeBinding#getElementType()
	 */
	public ITypeBinding getElementType() {
		if (!this.isArray()) {
			return null;
		}
		ArrayBinding arrayBinding = (ArrayBinding) binding;
		return resolver.getTypeBinding(arrayBinding.leafComponentType);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getTypeDeclaration()
	 */
	public ITypeBinding getTypeDeclaration() {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getErasure()
	 */
	public ITypeBinding getErasure() {
		return this.resolver.getTypeBinding(this.binding);
	}

	public IJavaScriptElement getJavaElement() {
		JavaElement element = getUnresolvedJavaElement();
		if (element == null)
			return null;
		return element.resolved(this.binding);
	}

	private JavaElement getUnresolvedJavaElement() {
		return getUnresolvedJavaElement(this.binding);
	}
	private JavaElement getUnresolvedJavaElement(org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding typeBinding ) {
		if (typeBinding == null)
			return null;
		switch (typeBinding.kind()) {
			case Binding.ARRAY_TYPE :
				typeBinding = ((ArrayBinding) typeBinding).leafComponentType();
				return getUnresolvedJavaElement(typeBinding);
			case Binding.BASE_TYPE :
				return null;
		}
		ReferenceBinding referenceBinding = (ReferenceBinding) typeBinding;
		char[] fileName = referenceBinding.getFileName();
		if (referenceBinding.isLocalType() || referenceBinding.isAnonymousType()) {
			// local or anonymous type
			if (Util.isClassFileName(fileName)) {
				int jarSeparator = CharOperation.indexOf(IDependent.JAR_FILE_ENTRY_SEPARATOR, fileName);
				int pkgEnd = CharOperation.lastIndexOf('/', fileName); // pkgEnd is exclusive
				if (pkgEnd == -1)
					pkgEnd = CharOperation.lastIndexOf(File.separatorChar, fileName);
				if (jarSeparator != -1 && pkgEnd < jarSeparator) // if in a jar and no slash, it is a default package -> pkgEnd should be equal to jarSeparator
					pkgEnd = jarSeparator;
				if (pkgEnd == -1)
					return null;
				IPackageFragment pkg = getPackageFragment(fileName, pkgEnd, jarSeparator);
				char[] constantPoolName = referenceBinding.constantPoolName();
				if (constantPoolName == null) {
					ClassFile classFile = (ClassFile) getClassFile(fileName);
					return classFile == null ? null : (JavaElement) classFile.getType();
				}
				pkgEnd = CharOperation.lastIndexOf('/', constantPoolName);
				char[] classFileName = CharOperation.subarray(constantPoolName, pkgEnd+1, constantPoolName.length);
				ClassFile classFile = (ClassFile) pkg.getClassFile(new String(classFileName) + SuffixConstants.SUFFIX_STRING_java);
				return (JavaElement) classFile.getType();
			}
			IJavaScriptUnit cu = getCompilationUnit(fileName);
			if (cu == null) return null;
			// must use getElementAt(...) as there is no back pointer to the defining method (scope is null after resolution has ended)
			try {
				int sourceStart = ((LocalTypeBinding) referenceBinding).sourceStart;
				return (JavaElement) cu.getElementAt(sourceStart);
			} catch (JavaScriptModelException e) {
				// does not exist
				return null;
			}
		} else {
			if (fileName == null) return null; // case of a WilCardBinding that doesn't have a corresponding JavaScript element
			// member or top level type
			ITypeBinding declaringTypeBinding = null;
			if (this.isArray()) {
				declaringTypeBinding = this.getElementType().getDeclaringClass();
			} else {
				declaringTypeBinding = this.getDeclaringClass();
			}
			if (declaringTypeBinding == null) {
				// top level type
				if (Util.isClassFileName(fileName)) {
					ClassFile classFile = (ClassFile) getClassFile(fileName);
					if (classFile == null) return null;
					return (JavaElement) classFile.getType();
				}
				IJavaScriptUnit cu = getCompilationUnit(fileName);
				if (cu == null) return null;
				return (JavaElement) cu.getType(new String(referenceBinding.sourceName()));
			} else {
				// member type
				IType declaringType = (IType) declaringTypeBinding.getJavaElement();
				if (declaringType == null) return null;
				return (JavaElement) declaringType.getType(new String(referenceBinding.sourceName()));
			}
		}
	}

	/*
	 * @see IBinding#getKey()
	 */
	public String getKey() {
		if (this.key == null) {
			this.key = new String(this.binding.computeUniqueKey());
		}
		return this.key;
	}

	/*
	 * @see IBinding#getKind()
	 */
	public int getKind() {
		return IBinding.TYPE;
	}

	/*
	 * @see IBinding#getModifiers()
	 */
	public int getModifiers() {
		if (isClass()) {
			ReferenceBinding referenceBinding = (ReferenceBinding) this.binding;
			final int accessFlags = referenceBinding.getAccessFlags() & VALID_MODIFIERS;
			if (referenceBinding.isAnonymousType()) {
				return accessFlags & ~Modifier.FINAL;
			}
			return accessFlags;
		} else {
			return Modifier.NONE;
		}
	}

	public String getName() {
		StringBuffer buffer;
		switch (this.binding.kind()) {

			case Binding.ARRAY_TYPE :
				ITypeBinding elementType = getElementType();
				if (elementType.isLocal() || elementType.isAnonymous()) {
					return NO_NAME;
				}
				int dimensions = getDimensions();
				char[] brackets = new char[dimensions * 2];
				for (int i = dimensions * 2 - 1; i >= 0; i -= 2) {
					brackets[i] = ']';
					brackets[i - 1] = '[';
				}
				buffer = new StringBuffer(elementType.getName());
				buffer.append(brackets);
				return String.valueOf(buffer);

			default :
				if (isPrimitive() || isNullType()) {
					BaseTypeBinding baseTypeBinding = (BaseTypeBinding) this.binding;
					return new String(baseTypeBinding.simpleName);
				}
				if (isAnonymous()) {
					return NO_NAME;
				}
				return new String(this.binding.sourceName());
		}
	}

	/*
	 * @see ITypeBinding#getPackage()
	 */
	public IPackageBinding getPackage() {
		switch (this.binding.kind()) {
			case Binding.BASE_TYPE :
			case Binding.ARRAY_TYPE :
				return null;
		}
		ReferenceBinding referenceBinding = (ReferenceBinding) this.binding;
		return this.resolver.getPackageBinding(referenceBinding.getPackage());
	}

	/*
	 * Returns the package that includes the given file name, or null if not found.
	 * pkgEnd == jarSeparator if default package in a jar
	 * pkgEnd > jarSeparator if non default package in a jar
	 * pkgEnd > 0 if package not in a jar
	 *
	 * @see org.eclipse.wst.jsdt.internal.compiler.env.IDependent#getFileName()
	 */
	private IPackageFragment getPackageFragment(char[] fileName, int pkgEnd, int jarSeparator) {
		if (jarSeparator != -1) {
			String jarMemento = new String(fileName, 0, jarSeparator);
			IPackageFragmentRoot root = (IPackageFragmentRoot) JavaScriptCore.create(jarMemento);
			if (pkgEnd == jarSeparator)
				return root.getPackageFragment(IPackageFragment.DEFAULT_PACKAGE_NAME);
			char[] pkgName = CharOperation.subarray(fileName, jarSeparator+1, pkgEnd);
			CharOperation.replace(pkgName, '/', '.');
			return root.getPackageFragment(new String(pkgName));
		} else {
			Path path = new Path(new String(fileName, 0, pkgEnd));
			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			IContainer folder = path.segmentCount() == 1 ? workspaceRoot.getProject(path.lastSegment()) : (IContainer) workspaceRoot.getFolder(path);
			IJavaScriptElement element = JavaScriptCore.create(folder);
			if (element == null) return null;
			switch (element.getElementType()) {
				case IJavaScriptElement.PACKAGE_FRAGMENT:
					return (IPackageFragment) element;
				case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
					return ((IPackageFragmentRoot) element).getPackageFragment(IPackageFragment.DEFAULT_PACKAGE_NAME);
				case IJavaScriptElement.JAVASCRIPT_PROJECT:
					IPackageFragmentRoot root = ((IJavaScriptProject) element).getPackageFragmentRoot(folder);
					if (root == null) return null;
					return root.getPackageFragment(IPackageFragment.DEFAULT_PACKAGE_NAME);
			}
			return null;
		}
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getQualifiedName()
	 */
	public String getQualifiedName() {
		StringBuffer buffer;
		switch (this.binding.kind()) {

			case Binding.ARRAY_TYPE :
				ITypeBinding elementType = getElementType();
				if (elementType.isLocal() || elementType.isAnonymous()) {
					return NO_NAME;
				}
				final int dimensions = getDimensions();
				char[] brackets = new char[dimensions * 2];
				for (int i = dimensions * 2 - 1; i >= 0; i -= 2) {
					brackets[i] = ']';
					brackets[i - 1] = '[';
				}
				buffer = new StringBuffer(elementType.getQualifiedName());
				buffer.append(brackets);
				return String.valueOf(buffer);

			default :
				if (isAnonymous() || isLocal()) {
					return NO_NAME;
				}
				if (isPrimitive() || isNullType()) {
					BaseTypeBinding baseTypeBinding = (BaseTypeBinding) this.binding;
					return new String(baseTypeBinding.simpleName);
				}
				if (isMember()) {
					buffer = new StringBuffer();
					buffer
						.append(getDeclaringClass().getQualifiedName())
						.append('.');
					buffer.append(getName());
					return String.valueOf(buffer);
				}
				PackageBinding packageBinding = this.binding.getPackage();
				buffer = new StringBuffer();
				if (packageBinding != null && packageBinding.compoundName != CharOperation.NO_CHAR_CHAR) {
					buffer.append(CharOperation.concatWith(packageBinding.compoundName, '.')).append('.');
				}
				buffer.append(getName());
				return String.valueOf(buffer);
		}
	}

	/*
	 * @see ITypeBinding#getSuperclass()
	 */
	public synchronized ITypeBinding getSuperclass() {
		if (this.binding == null)
			return null;
		switch (this.binding.kind()) {
			case Binding.ARRAY_TYPE :
			case Binding.BASE_TYPE :
				return null;
		}
		ReferenceBinding superclass = null;
		try {
			superclass = ((ReferenceBinding)this.binding).getSuperBinding();
		} catch (RuntimeException e) {
			/* in case a method cannot be resolvable due to missing jars on the includepath
			 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=57871
			 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=63550
			 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=64299
			 */
			org.eclipse.wst.jsdt.internal.core.util.Util.log(e, "Could not retrieve superclass"); //$NON-NLS-1$
			return this.resolver.resolveWellKnownType("Object"); //$NON-NLS-1$
		}
		if (superclass == null) {
			return null;
		}
		return this.resolver.getTypeBinding(superclass);
	}

	/*
	 * @see ITypeBinding#isAnonymous()
	 */
	public boolean isAnonymous() {
		if (isClass()) {
			ReferenceBinding referenceBinding = (ReferenceBinding) this.binding;
			return referenceBinding.isAnonymousType();
		}
		return false;
	}

	/*
	 * @see ITypeBinding#isArray()
	 */
	public boolean isArray() {
		return binding.isArrayType();
	}

	/* (non-Javadoc)
	 * @see ITypeBinding#isAssignmentCompatible(ITypeBinding)
	 */
	public boolean isAssignmentCompatible(ITypeBinding type) {
		try {
			if (this == type) return true;
			if (!(type instanceof TypeBinding)) return false;
			TypeBinding other = (TypeBinding) type;
			Scope scope = this.resolver.scope();
			if (scope == null) return false;
			return this.binding.isCompatibleWith(other.binding) || scope.isBoxingCompatibleWith(this.binding, other.binding);
		} catch (AbortCompilation e) {
			// don't surface internal exception to clients
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=143013
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see ITypeBinding#isCastCompatible(ITypeBinding)
	 */
	public boolean isCastCompatible(ITypeBinding type) {
		try {
			Expression expression = new Expression() {
				public StringBuffer printExpression(int indent,StringBuffer output) {
					return null;
				}
			};
			Scope scope = this.resolver.scope();
			if (scope == null) return false;
			if (!(type instanceof TypeBinding)) return false;
			org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding expressionType = ((TypeBinding) type).binding;
			// simulate capture in case checked binding did not properly get extracted from a reference
			return expression.checkCastTypesCompatibility(scope, this.binding, expressionType, null);
		} catch (AbortCompilation e) {
			// don't surface internal exception to clients
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=143013
			return false;
		}
	}

	/*
	 * @see ITypeBinding#isClass()
	 */
	public boolean isClass() {
		return this.binding.isClass();
	}

	/*
	 * @see IBinding#isDeprecated()
	 */
	public boolean isDeprecated() {
		if (isClass()) {
			ReferenceBinding referenceBinding = (ReferenceBinding) this.binding;
			return referenceBinding.isDeprecated();
		}
		return false;
	}

	/*
	 * @see IBinding#isEqualTo(Binding)
	 *  
	 */
	public boolean isEqualTo(IBinding other) {
		if (other == this) {
			// identical binding - equal (key or no key)
			return true;
		}
		if (other == null) {
			// other binding missing
			return false;
		}
		if (!(other instanceof TypeBinding)) {
			return false;
		}
		org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding otherBinding = ((TypeBinding) other).binding;
		// check return type
		return BindingComparator.isEqual(this.binding, otherBinding);
	}

	/*
	 * @see ITypeBinding#isFromSource()
	 */
	public boolean isFromSource() {
		if (isClass()) {
			ReferenceBinding referenceBinding = (ReferenceBinding) this.binding;
			return !referenceBinding.isBinaryBinding();
		}
		return false;
	}

	/*
	 * @see ITypeBinding#isLocal()
	 */
	public boolean isLocal() {
		if (isClass()) {
			ReferenceBinding referenceBinding = (ReferenceBinding) this.binding;
			return referenceBinding.isLocalType() && !referenceBinding.isMemberType();
		}
		return false;
	}

	/*
	 * @see ITypeBinding#isMember()
	 */
	public boolean isMember() {
		if (isClass()) {
			ReferenceBinding referenceBinding = (ReferenceBinding) this.binding;
			return referenceBinding.isMemberType();
		}
		return false;
	}

	/*
	 * @see ITypeBinding#isNested()
	 */
	public boolean isNested() {
		if (isClass()) {
			ReferenceBinding referenceBinding = (ReferenceBinding) this.binding;
			return referenceBinding.isNestedType();
		}
		return false;
	}

	/**
	 * @see ITypeBinding#isNullType()
	 */
	public boolean isNullType() {
		return this.binding == org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding.NULL;
	}

	/*
	 * @see ITypeBinding#isPrimitive()
	 */
	public boolean isPrimitive() {
		return !isNullType() && binding.isBaseType();
	}

	/* (non-Javadoc)
	 * @see IBinding#isRecovered()
	 */
	public boolean isRecovered() {
		return false;
	}

	/* (non-Javadoc)
	 * @see ITypeBinding#isSubTypeCompatible(ITypeBinding)
	 */
	public boolean isSubTypeCompatible(ITypeBinding type) {
		try {
			if (this == type) return true;
			if (this.binding.isBaseType()) return false;
			if (!(type instanceof TypeBinding)) return false;
			TypeBinding other = (TypeBinding) type;
			if (other.binding.isBaseType()) return false;
			return this.binding.isCompatibleWith(other.binding);
		} catch (AbortCompilation e) {
			// don't surface internal exception to clients
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=143013
			return false;
		}
	}

	/*
	 * @see ITypeBinding#isTopLevel()
	 */
	public boolean isTopLevel() {
		if (isClass()) {
			ReferenceBinding referenceBinding = (ReferenceBinding) this.binding;
			return !referenceBinding.isNestedType();
		}
		return false;
	}

	private boolean shouldBeRemoved(org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding methodBinding) {
		return methodBinding.isDefaultAbstract();
	}

	/*
	 * For debugging purpose only.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.binding.toString();
	}

	public boolean isCompilationUnit()
	{
		return false;
	}
}
