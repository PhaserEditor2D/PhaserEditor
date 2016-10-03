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
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.env.IDependent;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.core.ClassFile;
import org.eclipse.wst.jsdt.internal.core.JavaElement;

/**
 * Internal implementation of type bindings.
 */
class JavaScriptUnitBinding implements ITypeBinding {
	private static final IFunctionBinding[] NO_METHOD_BINDINGS = new IFunctionBinding[0];

	private static final String NO_NAME = ""; //$NON-NLS-1$
	private static final ITypeBinding[] NO_TYPE_BINDINGS = new ITypeBinding[0];
	private static final IVariableBinding[] NO_VARIABLE_BINDINGS = new IVariableBinding[0];

	private static final int VALID_MODIFIERS = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE |
		Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL | Modifier.STRICTFP;

	org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding binding;
	private String key;
	private BindingResolver resolver;

	public JavaScriptUnitBinding(BindingResolver resolver, org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding binding) {
		this.binding = binding;
		this.resolver = resolver;
	}

	public ITypeBinding createArrayType(int dimension) {
//		int realDimensions = dimension;
//		realDimensions += this.getDimensions();
//		if (realDimensions < 1 || realDimensions > 255) {
//			throw new IllegalArgumentException();
//		}
//		return this.resolver.resolveArrayType(this, dimension);
		return null;
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
//	private IClassFile getClassFile(char[] fileName) {
//		int jarSeparator = CharOperation.indexOf(IDependent.JAR_FILE_ENTRY_SEPARATOR, fileName);
//		int pkgEnd = CharOperation.lastIndexOf('/', fileName); // pkgEnd is exclusive
//		if (pkgEnd == -1)
//			pkgEnd = CharOperation.lastIndexOf(File.separatorChar, fileName);
//		if (jarSeparator != -1 && pkgEnd < jarSeparator) // if in a jar and no slash, it is a default package -> pkgEnd should be equal to jarSeparator
//			pkgEnd = jarSeparator;
//		if (pkgEnd == -1)
//			return null;
//		IPackageFragment pkg = getPackageFragment(fileName, pkgEnd, jarSeparator);
//		if (pkg == null) return null;
//		int start;
//		return pkg.getClassFile(new String(fileName, start = pkgEnd + 1, fileName.length - start));
//	}

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
		return null;
	}

	/*
	 * @see ITypeBinding#getDeclaredFields()
	 */
	public IVariableBinding[] getDeclaredFields() {
		try {
				ReferenceBinding referenceBinding = (ReferenceBinding) this.binding;
				FieldBinding[] fields = referenceBinding.fields();
				int length = fields.length;
				IVariableBinding[] newFields = new IVariableBinding[length];
				for (int i = 0; i < length; i++) {
					newFields[i] = this.resolver.getVariableBinding(fields[i]);
				}
				return newFields;
		} catch (RuntimeException e) {
			/* in case a method cannot be resolvable due to missing jars on the includepath
			 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=57871
			 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=63550
			 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=64299
			 */
		}
		return NO_VARIABLE_BINDINGS;
	}


	/*
	 * @see ITypeBinding#getDeclaredMethods()
	 */
	public IFunctionBinding[] getDeclaredMethods() {
		try {
				ReferenceBinding referenceBinding = (ReferenceBinding) this.binding;
				org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding[] methods = referenceBinding.methods();
				int length = methods.length;
				IFunctionBinding[] newMethods = new IFunctionBinding[length];
				for (int i = 0; i < length; i++) {
					org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding methodBinding = methods[i];
//					if (!shouldBeRemoved(methodBinding)) {
						newMethods[i] = this.resolver.getMethodBinding(methodBinding);
//					}
				}
				return newMethods;
		} catch (RuntimeException e) {
			/* in case a method cannot be resolvable due to missing jars on the includepath
			 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=57871
			 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=63550
			 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=64299
			 */
		}
		return NO_METHOD_BINDINGS;
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
	public ITypeBinding[] getDeclaredTypes() {

		return NO_TYPE_BINDINGS;
	}

	/*
	 * @see ITypeBinding#getDeclaringMethod()
	 */
	public IFunctionBinding getDeclaringMethod() {

		return null;
	}

	/*
	 * @see ITypeBinding#getDeclaringClass()
	 */
	public ITypeBinding getDeclaringClass() {

		return null;
	}

	/*
	 * @see ITypeBinding#getDimensions()
	 */
	public int getDimensions() {
		return 0;
	}

	/*
	 * @see ITypeBinding#getElementType()
	 */
	public ITypeBinding getElementType() {
		return null;
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

	public ITypeBinding[] getInterfaces() {
//		if (this.binding == null)
			return NO_TYPE_BINDINGS;
//		switch (this.binding.kind()) {
//			case Binding.ARRAY_TYPE :
//			case Binding.BASE_TYPE :
//				return NO_TYPE_BINDINGS;
//		}
//		ReferenceBinding referenceBinding = (ReferenceBinding) this.binding;
//		ReferenceBinding[] interfaces = null;
//		try {
//			interfaces = referenceBinding.superInterfaces();
//		} catch (RuntimeException e) {
//			/* in case a method cannot be resolvable due to missing jars on the includepath
//			 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=57871
//			 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=63550
//			 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=64299
//			 */
//		}
//		if (interfaces == null) {
//			return NO_TYPE_BINDINGS;
//		}
//		int length = interfaces.length;
//		if (length == 0) {
//			return NO_TYPE_BINDINGS;
//		} else {
//			ITypeBinding[] newInterfaces = new ITypeBinding[length];
//			for (int i = 0; i < length; i++) {
//				ITypeBinding typeBinding = this.resolver.getTypeBinding(interfaces[i]);
//				if (typeBinding == null) {
//					return NO_TYPE_BINDINGS;
//				}
//				newInterfaces[i] = typeBinding;
//			}
//			return newInterfaces;
//		}
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

		ReferenceBinding referenceBinding = (ReferenceBinding) typeBinding;
		char[] fileName = referenceBinding.getFileName();
			if (fileName == null) return null; // case of a WilCardBinding that doesn't have a corresponding JavaScript element
			// member or top level type
			ITypeBinding declaringTypeBinding = getDeclaringClass();
			if (declaringTypeBinding == null) {
				// top level type
				if (((ReferenceBinding)this.binding).isBinaryBinding()) {
					ClassFile classFile = (ClassFile) getClassFile(fileName);
					return classFile;
				}
				IJavaScriptUnit cu = getCompilationUnit(fileName);
				return (JavaElement)cu;
			} else {
				// member type
				IType declaringType = (IType) declaringTypeBinding.getJavaElement();
				if (declaringType == null) return null;
				return (JavaElement) declaringType.getType(new String(referenceBinding.sourceName()));
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
		} else if (isAnnotation()) {
			ReferenceBinding referenceBinding = (ReferenceBinding) this.binding;
			final int accessFlags = referenceBinding.getAccessFlags() & VALID_MODIFIERS;
			// clear the AccAbstract, AccAnnotation and the AccInterface bits
			return accessFlags & ~(ClassFileConstants.AccAbstract);
		} else if (isInterface()) {
			ReferenceBinding referenceBinding = (ReferenceBinding) this.binding;
			final int accessFlags = referenceBinding.getAccessFlags() & VALID_MODIFIERS;
			// clear the AccAbstract and the AccInterface bits
			return accessFlags & ~(ClassFileConstants.AccAbstract);
		} else {
			return 0;
		}
	}

	public String getName() {
				return new String(this.binding.sourceName());
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
//		switch (this.binding.kind()) {
//
//			case Binding.RAW_TYPE :
//				return getTypeDeclaration().getQualifiedName();
//
//			case Binding.ARRAY_TYPE :
//				ITypeBinding elementType = getElementType();
//				if (elementType.isLocal() || elementType.isAnonymous() || elementType.isCapture()) {
//					return NO_NAME;
//				}
//				final int dimensions = getDimensions();
//				char[] brackets = new char[dimensions * 2];
//				for (int i = dimensions * 2 - 1; i >= 0; i -= 2) {
//					brackets[i] = ']';
//					brackets[i - 1] = '[';
//				}
//				buffer = new StringBuffer(elementType.getQualifiedName());
//				buffer.append(brackets);
//				return String.valueOf(buffer);
//
//			case Binding.TYPE_PARAMETER :
//				if (isCapture()) {
//					return NO_NAME;
//				}
//				TypeVariableBinding typeVariableBinding = (TypeVariableBinding) this.binding;
//				return new String(typeVariableBinding.sourceName);
//
//			case Binding.PARAMETERIZED_TYPE :
//				buffer = new StringBuffer();
//				if (isMember()) {
//					buffer
//						.append(getDeclaringClass().getQualifiedName())
//						.append('.');
//					ParameterizedTypeBinding parameterizedTypeBinding = (ParameterizedTypeBinding) this.binding;
//					buffer.append(parameterizedTypeBinding.sourceName());
//					ITypeBinding[] typeArguments = getTypeArguments();
//					final int typeArgumentsLength = typeArguments.length;
//					if (typeArgumentsLength != 0) {
//						buffer.append('<');
//						for (int i = 0, max = typeArguments.length; i < max; i++) {
//							if (i > 0) {
//								buffer.append(',');
//							}
//							buffer.append(typeArguments[i].getQualifiedName());
//						}
//						buffer.append('>');
//					}
//					return String.valueOf(buffer);
//				}
//				buffer.append(getTypeDeclaration().getQualifiedName());
//				ITypeBinding[] typeArguments = getTypeArguments();
//				final int typeArgumentsLength = typeArguments.length;
//				if (typeArgumentsLength != 0) {
//					buffer.append('<');
//					for (int i = 0, max = typeArguments.length; i < max; i++) {
//						if (i > 0) {
//							buffer.append(',');
//						}
//						buffer.append(typeArguments[i].getQualifiedName());
//					}
//					buffer.append('>');
//				}
//				return String.valueOf(buffer);
//
//			default :
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
//		}
	}

	/*
	 * @see ITypeBinding#getSuperclass()
	 */
	public ITypeBinding getSuperclass() {
			return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getTypeArguments()
	 */
	public ITypeBinding[] getTypeArguments() {
		return NO_TYPE_BINDINGS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getTypeBounds()
	 */
	public ITypeBinding[] getTypeBounds() {
		return NO_TYPE_BINDINGS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#getTypeParameters()
	 */
	public ITypeBinding[] getTypeParameters() {
		return NO_TYPE_BINDINGS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isGenericType()
	 *  
	 */
	public boolean isGenericType() {
			return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isAnnotation()
	 */
	public boolean isAnnotation() {
		return false;
	}

	/*
	 * @see ITypeBinding#isAnonymous()
	 */
	public boolean isAnonymous() {
		return false;
	}

	/*
	 * @see ITypeBinding#isArray()
	 */
	public boolean isArray() {
		return false;
	}

	/* (non-Javadoc)
	 * @see ITypeBinding#isAssignmentCompatible(ITypeBinding)
	 */
	public boolean isAssignmentCompatible(ITypeBinding type) {
		return false;
	}

	/* (non-Javadoc)
	 * @see ITypeBinding#isCapture()
	 */
	public boolean isCapture() {
		return false;
	}

	/* (non-Javadoc)
	 * @see ITypeBinding#isCastCompatible(ITypeBinding)
	 */
	public boolean isCastCompatible(ITypeBinding type) {
		return false;
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
		if (isClass() || isInterface() || isEnum()) {
			ReferenceBinding referenceBinding = (ReferenceBinding) this.binding;
			return referenceBinding.isDeprecated();
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see ITypeBinding#isEnum()
	 */
	public boolean isEnum() {
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
		if (!(other instanceof JavaScriptUnitBinding)) {
			return false;
		}
		org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding otherBinding = ((JavaScriptUnitBinding) other).binding;
		// check return type
		return BindingComparator.isEqual(this.binding, otherBinding);
	}

	/*
	 * @see ITypeBinding#isFromSource()
	 */
	public boolean isFromSource() {
		return !((ReferenceBinding)this.binding).isBinaryBinding();
	}

	/*
	 * @see ITypeBinding#isInterface()
	 */
	public boolean isInterface() {
		return false;
	}

	/*
	 * @see ITypeBinding#isLocal()
	 */
	public boolean isLocal() {
		return true;
	}

	/*
	 * @see ITypeBinding#isMember()
	 */
	public boolean isMember() {
		return false;
	}

	/*
	 * @see ITypeBinding#isNested()
	 */
	public boolean isNested() {
		return false;
	}

	/**
	 * @see ITypeBinding#isNullType()
	 */
	public boolean isNullType() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isParameterizedType()
	 */
	public boolean isParameterizedType() {
		return false;
	}

	/*
	 * @see ITypeBinding#isPrimitive()
	 */
	public boolean isPrimitive() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isRawType()
	 */
	public boolean isRawType() {
		return false;
	}

	/* (non-Javadoc)
	 * @see ITypeBinding#isSubTypeCompatible(ITypeBinding)
	 */
	public boolean isSubTypeCompatible(ITypeBinding type) {
		return false;
	}

	/*
	 * @see ITypeBinding#isTopLevel()
	 */
	public boolean isTopLevel() {
		return true;
	}

	/*
	 * @see ITypeBinding#isTypeVariable()
	 */
	public boolean isTypeVariable() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.core.dom.ITypeBinding#isUpperbound()
	 */
	public boolean isUpperbound() {
		return false;
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
		return true;
	}

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

	public boolean isRecovered() {
		return false;
	}

}
