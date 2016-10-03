/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Matt Chapman, mpchapman@gmail.com - 89977 Make JDT .java agnostic
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.RewriteSessionEditProcessor;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJarEntryResource;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.core.util.Util;
import org.eclipse.wst.jsdt.internal.corext.CorextMessages;
import org.eclipse.wst.jsdt.internal.corext.ValidateEditException;
import org.eclipse.wst.jsdt.internal.corext.template.java.SignatureUtil;
import org.eclipse.wst.jsdt.internal.ui.JavaUIStatus;
import org.eclipse.wst.jsdt.launching.IVMInstall;
import org.eclipse.wst.jsdt.launching.IVMInstall2;
import org.eclipse.wst.jsdt.launching.JavaRuntime;

/**
 * Utility methods for the Java Model.
 */
public final class JavaModelUtil {
	
	/**
	 * Only use this suffix for creating new .java files.
	 * In general, use one of the three *JavaLike*(..) methods in JavaScriptCore or create
	 * a name from an existing compilation unit with {@link #getRenamedCUName(IJavaScriptUnit, String)}
	 * <p> 
	 * Note: Unlike {@link JavaScriptCore#getJavaScriptLikeExtensions()}, this suffix includes a leading ".".
	 * </p>
	 * 
	 * @see JavaScriptCore#getJavaScriptLikeExtensions() 
	 * @see JavaScriptCore#isJavaScriptLikeFileName(String)
	 * @see JavaScriptCore#removeJavaScriptLikeExtension(String)
	 * @see #getRenamedCUName(IJavaScriptUnit, String)
	 */
	public static final String DEFAULT_CU_SUFFIX= ".js"; //$NON-NLS-1$
	
	/** 
	 * Finds a type by its qualified type name (dot separated).
	 * @param jproject The java project to search in
	 * @param fullyQualifiedName The fully qualified name (type name with enclosing type names and package (all separated by dots))
	 * @return The type found, or null if not existing
	 */	
	public static IType findType(IJavaScriptProject jproject, String fullyQualifiedName) throws JavaScriptModelException {
		//workaround for bug 22883
		IType type= jproject.findType(fullyQualifiedName);
		if (type != null)
			return type;
		IPackageFragmentRoot[] roots= jproject.getPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			IPackageFragmentRoot root= roots[i];
			type= findType(root, fullyQualifiedName);
			if (type != null && type.exists())
				return type;
		}	
		return null;
	}
	
	/** 
	 * Finds a type by its qualified type name (dot separated).
	 * @param jproject The java project to search in
	 * @param fullyQualifiedName The fully qualified name (type name with enclosing type names and package (all separated by dots))
	 * @param owner the working copy owner
	 * @return The type found, or null if not existing
	 */	
	public static IType findType(IJavaScriptProject jproject, String fullyQualifiedName, WorkingCopyOwner owner) throws JavaScriptModelException {
		//workaround for bug 22883
		IType type= jproject.findType(fullyQualifiedName, owner);
		if (type != null)
			return type;
		IPackageFragmentRoot[] roots= jproject.getPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			IPackageFragmentRoot root= roots[i];
			type= findType(root, fullyQualifiedName);
			if (type != null && type.exists())
				return type;
		}	
		return null;
	}
	

	
	private static IType findType(IPackageFragmentRoot root, String fullyQualifiedName) throws JavaScriptModelException{
		IJavaScriptElement[] children= root.getChildren();
		for (int i= 0; i < children.length; i++) {
			IJavaScriptElement element= children[i];
			if (element.getElementType() == IJavaScriptElement.PACKAGE_FRAGMENT){
				IPackageFragment pack= (IPackageFragment)element;
				if (! fullyQualifiedName.startsWith(pack.getElementName()))
					continue;
				IType type= findType(pack, fullyQualifiedName);
				if (type != null && type.exists())
					return type;
			}
		}		
		return null;
	}
	
	private static IType findType(IPackageFragment pack, String fullyQualifiedName) throws JavaScriptModelException{
		IJavaScriptUnit[] cus= pack.getJavaScriptUnits();
		for (int i= 0; i < cus.length; i++) {
			IJavaScriptUnit unit= cus[i];
			IType type= findType(unit, fullyQualifiedName);
			if (type != null && type.exists())
				return type;
		}
		return null;
	}
	
	private static IType findType(IJavaScriptUnit cu, String fullyQualifiedName) throws JavaScriptModelException{
		IType[] types= cu.getAllTypes();
		for (int i= 0; i < types.length; i++) {
			IType type= types[i];
			if (getFullyQualifiedName(type).equals(fullyQualifiedName))
				return type;
		}
		return null;
	}
	
	/**
	 * Finds a type container by container name.
	 * The returned element will be of type <code>IType</code> or a <code>IPackageFragment</code>.
	 * <code>null</code> is returned if the type container could not be found.
	 * @param jproject The Java project defining the context to search
	 * @param typeContainerName A dot separated name of the type container
	 * @see #getTypeContainerName(IType)
	 */
	public static IJavaScriptElement findTypeContainer(IJavaScriptProject jproject, String typeContainerName) throws JavaScriptModelException {
		// try to find it as type
		IJavaScriptElement result= jproject.findType(typeContainerName);
		if (result == null) {
			// find it as package
			IPath path= new Path(typeContainerName.replace('.', '/'));
			result= jproject.findElement(path);
			if (!(result instanceof IPackageFragment)) {
				result= null;
			}
			
		}
		return result;
	}	
	
	/** 
	 * Finds a type in a compilation unit. Typical usage is to find the corresponding
	 * type in a working copy.
	 * @param cu the compilation unit to search in
	 * @param typeQualifiedName the type qualified name (type name with enclosing type names (separated by dots))
	 * @return the type found, or null if not existing
	 */		
	public static IType findTypeInCompilationUnit(IJavaScriptUnit cu, String typeQualifiedName) throws JavaScriptModelException {
		IType[] types= cu.getAllTypes();
		for (int i= 0; i < types.length; i++) {
			String currName= getTypeQualifiedName(types[i]);
			if (typeQualifiedName.equals(currName)) {
				return types[i];
			}
		}
		return null;
	}
	
	/** 
	 * Returns the element of the given compilation unit which is "equal" to the
	 * given element. Note that the given element usually has a parent different
	 * from the given compilation unit.
	 * 
	 * @param cu the cu to search in
	 * @param element the element to look for
	 * @return an element of the given cu "equal" to the given element
	 */		
	public static IJavaScriptElement findInCompilationUnit(IJavaScriptUnit cu, IJavaScriptElement element) {
		IJavaScriptElement[] elements= cu.findElements(element);
		if (elements != null && elements.length > 0) {
			return elements[0];
		}
		return null;
	}
	
	/**
	 * Returns the qualified type name of the given type using '.' as separators.
	 * This is a replace for IType.getTypeQualifiedName()
	 * which uses '$' as separators. As '$' is also a valid character in an id
	 * this is ambiguous. JavaScriptCore PR: 1GCFUNT
	 */
	public static String getTypeQualifiedName(IType type) {
		try {
			if (type.isBinary() && !type.isAnonymous()) {
				IType declaringType= type.getDeclaringType();
				if (declaringType != null) {
					return getTypeQualifiedName(declaringType) + '.' + type.getElementName();
				}
			}
		} catch (JavaScriptModelException e) {
			// ignore
		}	
		return type.getTypeQualifiedName('.');
	}
	
	/**
	 * Returns the fully qualified name of the given type using '.' as separators.
	 * This is a replace for IType.getFullyQualifiedTypeName
	 * which uses '$' as separators. As '$' is also a valid character in an id
	 * this is ambiguous. JavaScriptCore PR: 1GCFUNT
	 */
	public static String getFullyQualifiedName(IType type) {
		try {
			if (type.isBinary() && !type.isAnonymous()) {
				IType declaringType= type.getDeclaringType();
				if (declaringType != null) {
					return getFullyQualifiedName(declaringType) + '.' + type.getElementName();
				}
			}
		} catch (JavaScriptModelException e) {
			// ignore
		}		
		return type.getFullyQualifiedName('.');
	}
	
	/**
	 * Returns the fully qualified name of a type's container. (package name or enclosing type name)
	 */
	public static String getTypeContainerName(IType type) {
		IType outerType= type.getDeclaringType();
		if (outerType != null) {
			return getFullyQualifiedName(outerType);
		} else {
			return type.getPackageFragment().getElementName();
		}
	}
	
	
	/**
	 * Concatenates two names. Uses a dot for separation.
	 * Both strings can be empty or <code>null</code>.
	 */
	public static String concatenateName(String name1, String name2) {
		StringBuffer buf= new StringBuffer();
		if (name1 != null && name1.length() > 0) {
			buf.append(name1);
		}
		if (name2 != null && name2.length() > 0) {
			if (buf.length() > 0) {
				buf.append('.');
			}
			buf.append(name2);
		}		
		return buf.toString();
	}
	
	/**
	 * Concatenates two names. Uses a dot for separation.
	 * Both strings can be empty or <code>null</code>.
	 */
	public static String concatenateName(char[] name1, char[] name2) {
		StringBuffer buf= new StringBuffer();
		if (name1 != null && name1.length > 0) {
			buf.append(name1);
		}
		if (name2 != null && name2.length > 0) {
			if (buf.length() > 0) {
				buf.append('.');
			}
			buf.append(name2);
		}		
		return buf.toString();
	}	
	
	/**
	 * Evaluates if a member (possible from another package) is visible from
	 * elements in a package.
	 * @param member The member to test the visibility for
	 * @param pack The package in focus
	 */
	public static boolean isVisible(IMember member, IPackageFragment pack) throws JavaScriptModelException {
		
		int type= member.getElementType();
		if  (type == IJavaScriptElement.INITIALIZER ||  (type == IJavaScriptElement.METHOD && member.getElementName().startsWith("<"))) { //$NON-NLS-1$
			return false;
		}
		
		int otherflags= member.getFlags();
		if (Flags.isPublic(otherflags)) {
			return true;
		} else if (Flags.isPrivate(otherflags)) {
			return false;
		}		
		
		IPackageFragment otherpack= (IPackageFragment) member.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT);
		return (pack != null && otherpack != null && isSamePackage(pack, otherpack));
	}
	
	/**
	 * Evaluates if a member in the focus' element hierarchy is visible from
	 * elements in a package.
	 * @param member The member to test the visibility for
	 * @param pack The package of the focus element focus
	 */
	public static boolean isVisibleInHierarchy(IMember member, IPackageFragment pack) throws JavaScriptModelException {
		int type= member.getElementType();
		if  (type == IJavaScriptElement.INITIALIZER ||  (type == IJavaScriptElement.METHOD && member.getElementName().startsWith("<"))) { //$NON-NLS-1$
			return false;
		}
		return true;
		
//		int otherflags= member.getFlags();
//		
//		if (Flags.isPublic(otherflags)) {
//			return true;
//		} else if (Flags.isPrivate(otherflags)) {
//			return false;
//		}		
//		
//		IPackageFragment otherpack= (IPackageFragment) member.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT);
//		return (pack != null && pack.equals(otherpack));
	}
			
		
	/**
	 * Returns the package fragment root of <code>IJavaScriptElement</code>. If the given
	 * element is already a package fragment root, the element itself is returned.
	 */
	public static IPackageFragmentRoot getPackageFragmentRoot(IJavaScriptElement element) {
		return (IPackageFragmentRoot) element.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
	}
	
	/**
	 * Finds a method in a type.
	 * This searches for a method with the same name and signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type name is done.
	 * Constructors are only compared by parameters, not the name.
	 * @param name The name of the method to find
	 * @param paramTypes The type signatures of the parameters e.g. <code>{"QString;","I"}</code>
	 * @param isConstructor If the method is a constructor
	 * @return The first found method or <code>null</code>, if nothing found
	 */
	public static IFunction findMethod(String name, String[] paramTypes, boolean isConstructor, IType type) throws JavaScriptModelException {
		IFunction[] methods= type.getFunctions();
		for (int i= 0; i < methods.length; i++) {
			if (isSameMethodSignature(name, paramTypes, isConstructor, methods[i])) {
				return methods[i];
			}
		}
		return null;
	}
				
	/**
	 * Finds a method in a type and all its super types. The super class hierarchy is searched first, then the super interfaces.
	 * This searches for a method with the same name and signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type name is done.
	 * Constructors are only compared by parameters, not the name.
	 * NOTE: For finding overridden methods or for finding the declaring method, use {@link MethodOverrideTester}
	 * @param hierarchy The hierarchy containing the type
	 * 	@param type The type to start the search from
	 * @param name The name of the method to find
	 * @param paramTypes The type signatures of the parameters e.g. <code>{"QString;","I"}</code>
	 * @param isConstructor If the method is a constructor
	 * @return The first found method or <code>null</code>, if nothing found
	 */
	public static IFunction findMethodInHierarchy(ITypeHierarchy hierarchy, IType type, String name, String[] paramTypes, boolean isConstructor) throws JavaScriptModelException {
		IFunction method= findMethod(name, paramTypes, isConstructor, type);
		if (method != null) {
			return method;
		}
		IType superClass= hierarchy.getSuperclass(type);
		if (superClass != null) {
			IFunction res=  findMethodInHierarchy(hierarchy, superClass, name, paramTypes, isConstructor);
			if (res != null) {
				return res;
			}
		}
		return method;		
	}
		
	
	/**
	 * Tests if a method equals to the given signature.
	 * Parameter types are only compared by the simple name, no resolving for
	 * the fully qualified type name is done. Constructors are only compared by
	 * parameters, not the name.
	 * @param name Name of the method
	 * @param paramTypes The type signatures of the parameters e.g. <code>{"QString;","I"}</code>
	 * @param isConstructor Specifies if the method is a constructor
	 * @return Returns <code>true</code> if the method has the given name and parameter types and constructor state.
	 */
	public static boolean isSameMethodSignature(String name, String[] paramTypes, boolean isConstructor, IFunction curr) throws JavaScriptModelException {
		if (isConstructor || name.equals(curr.getElementName())) {
			if (isConstructor == curr.isConstructor()) {
				String[] currParamTypes= curr.getParameterTypes();
				if (paramTypes.length == currParamTypes.length) {
					for (int i= 0; i < paramTypes.length; i++) {
						String t1= Signature.getSimpleName(Signature.toString(paramTypes[i]));
						String t2= Signature.getSimpleName(Signature.toString(currParamTypes[i]));
						if (!t1.equals(t2)) {
							return false;
						}
					}
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Tests if two <code>IPackageFragment</code>s represent the same logical java package.
	 * @return <code>true</code> if the package fragments' names are equal.
	 */
	public static boolean isSamePackage(IPackageFragment pack1, IPackageFragment pack2) {
		return pack1.getElementName().equals(pack2.getElementName());
	}
	
	/**
	 * Checks if the field is boolean.
	 */
	public static boolean isBoolean(IField field) throws JavaScriptModelException{
		return field.getTypeSignature().equals(SignatureUtil.BOOLEAN_SIGNATURE);
	}
		
	/**
	 * Resolves a type name in the context of the declaring type.
	 * 
	 * @param refTypeSig the type name in signature notation (for example 'QVector') this can also be an array type, but dimensions will be ignored.
	 * @param declaringType the context for resolving (type where the reference was made in)
	 * @return returns the fully qualified type name or build-in-type name. if a unresolved type couldn't be resolved null is returned
	 */
	public static String getResolvedTypeName(String refTypeSig, IType declaringType) throws JavaScriptModelException {
		int arrayCount= Signature.getArrayCount(refTypeSig);
		char type= refTypeSig.charAt(arrayCount);
		if (type == Signature.C_UNRESOLVED) {
			String name= ""; //$NON-NLS-1$
			int semi= refTypeSig.indexOf(Signature.C_SEMICOLON, arrayCount + 1);
			if (semi == -1) {
				throw new IllegalArgumentException();
			}
			name= refTypeSig.substring(arrayCount + 1, semi);
			
			String[][] resolvedNames= declaringType.resolveType(name);
			if (resolvedNames != null && resolvedNames.length > 0) {
				return JavaModelUtil.concatenateName(resolvedNames[0][0], resolvedNames[0][1]);
			}
			return null;
		} else {
			return Signature.toString(refTypeSig.substring(arrayCount));
		}
	}
	
	/**
	 * Returns if a CU can be edited.
	 */
	public static boolean isEditable(IJavaScriptUnit cu)  {
		Assert.isNotNull(cu);
		IResource resource= cu.getPrimary().getResource();
		return (resource.exists() && !resource.getResourceAttributes().isReadOnly());
	}

	/**
	 * Returns true if a cu is a primary cu (original or shared working copy)
	 */
	public static boolean isPrimary(IJavaScriptUnit cu) {
		return cu.getOwner() == null;
	}

	/*
	 * http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
	 * 
	 * Reconciling happens in a separate thread. This can cause a situation where the
	 * Java element gets disposed after an exists test has been done. So we should not
	 * log not present exceptions when they happen in working copies.
	 */
	public static boolean isExceptionToBeLogged(CoreException exception) {
		if (!(exception instanceof JavaScriptModelException))
			return true;
		JavaScriptModelException je= (JavaScriptModelException)exception;
		if (!je.isDoesNotExist())
			return true;
		IJavaScriptElement[] elements= je.getJavaScriptModelStatus().getElements();
		for (int i= 0; i < elements.length; i++) {
			IJavaScriptElement element= elements[i];
			// if the element is already a compilation unit don't log
			// does not exist exceptions. See bug 
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=75894
			// for more details
			if (element.getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT)
				continue;
			IJavaScriptUnit unit= (IJavaScriptUnit)element.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
			if (unit == null)
				return true;
			if (!unit.isWorkingCopy())
				return true;
		}
		return false;		
	}

	public static IType[] getAllSuperTypes(IType type, IProgressMonitor pm) throws JavaScriptModelException {
		// workaround for 23656
		IType[] superTypes= SuperTypeHierarchyCache.getTypeHierarchy(type).getAllSuperclasses(type);
		return superTypes;
	}
	
	public static boolean isSuperType(ITypeHierarchy hierarchy, IType possibleSuperType, IType type) {
		// filed bug 112635 to add this method to ITypeHierarchy
		IType superClass= hierarchy.getSuperclass(type);
		if (superClass != null && (possibleSuperType.equals(superClass) || isSuperType(hierarchy, possibleSuperType, superClass))) {
			return true;
		}
		return false;
	}
	
	public static boolean isExcludedPath(IPath resourcePath, IPath[] exclusionPatterns) {
		char[] path = resourcePath.toString().toCharArray();
		for (int i = 0, length = exclusionPatterns.length; i < length; i++) {
			char[] pattern= exclusionPatterns[i].toString().toCharArray();
			if (CharOperation.pathMatch(pattern, path, true, '/')) {
				return true;
			}
		}
		return false;	
	}


	/*
	 * Returns whether the given resource path matches one of the exclusion
	 * patterns.
	 * 
	 * @see IIncludePathEntry#getExclusionPatterns
	 */
	public final static boolean isExcluded(IPath resourcePath, char[][] exclusionPatterns) {
		if (exclusionPatterns == null) return false;
		char[] path = resourcePath.toString().toCharArray();
		for (int i = 0, length = exclusionPatterns.length; i < length; i++)
			if (CharOperation.pathMatch(exclusionPatterns[i], path, true, '/'))
				return true;
		return false;
	}	
		

	/**
	 * Force a reconcile of a compilation unit.
	 * @param unit
	 */
	public static void reconcile(IJavaScriptUnit unit) throws JavaScriptModelException {
		unit.reconcile(
				IJavaScriptUnit.NO_AST, 
				false /* don't force problem detection */, 
				null /* use primary owner */, 
				null /* no progress monitor */);
	}
	
	/**
	 * Helper method that tests if an classpath entry can be found in a
	 * container. <code>null</code> is returned if the entry can not be found
	 * or if the container does not allows the configuration of source
	 * attachments
	 * @param jproject The container's parent project
	 * @param containerPath The path of the container
	 * @param libPath The path of the library to be found
	 * @return IIncludePathEntry A classpath entry from the container of
	 * <code>null</code> if the container can not be modified.
	 * @throws JavaScriptModelException thrown if accessing the container failed
	 */
	public static IIncludePathEntry getClasspathEntryToEdit(IJavaScriptProject jproject, IPath containerPath, IPath libPath) throws JavaScriptModelException {
		IJsGlobalScopeContainer container= JavaScriptCore.getJsGlobalScopeContainer(containerPath, jproject);
		JsGlobalScopeContainerInitializer initializer= JavaScriptCore.getJsGlobalScopeContainerInitializer(containerPath.segment(0));
		if (container != null && initializer != null && initializer.canUpdateJsGlobalScopeContainer(containerPath, jproject)) {
			return findEntryInContainer(container, libPath);
		}
		return null; // attachment not possible
	}
	
	/**
	 * Finds an entry in a container. <code>null</code> is returned if the entry can not be found
	 * @param container The container
	 * @param libPath The path of the library to be found
	 * @return IIncludePathEntry A classpath entry from the container of
	 * <code>null</code> if the container can not be modified.
	 */
	public static IIncludePathEntry findEntryInContainer(IJsGlobalScopeContainer container, IPath libPath) {
		IIncludePathEntry[] entries= container.getIncludepathEntries();
		for (int i= 0; i < entries.length; i++) {
			IIncludePathEntry curr= entries[i];
			IIncludePathEntry resolved= JavaScriptCore.getResolvedIncludepathEntry(curr);
			if (resolved != null && libPath.equals(resolved.getPath())) {
				return curr; // return the real entry
			}
		}
		return null; // attachment not possible
	}
	
	/**
	 * Get all compilation units of a selection.
	 * @param javaElements the selected java elements
	 * @return all compilation units containing and contained in elements from javaElements
	 * @throws JavaScriptModelException
	 */
	public static IJavaScriptUnit[] getAllCompilationUnits(IJavaScriptElement[] javaElements) throws JavaScriptModelException {
		HashSet result= new HashSet();
		for (int i= 0; i < javaElements.length; i++) {
			addAllCus(result, javaElements[i]);
		}
		return (IJavaScriptUnit[]) result.toArray(new IJavaScriptUnit[result.size()]);
	}

	private static void addAllCus(HashSet/*<IJavaScriptUnit>*/ collector, IJavaScriptElement javaElement) throws JavaScriptModelException {
		switch (javaElement.getElementType()) {
			case IJavaScriptElement.JAVASCRIPT_PROJECT:
				IJavaScriptProject javaProject= (IJavaScriptProject) javaElement;
				IPackageFragmentRoot[] packageFragmentRoots= javaProject.getPackageFragmentRoots();
				for (int i= 0; i < packageFragmentRoots.length; i++)
					addAllCus(collector, packageFragmentRoots[i]);
				return;
		
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
				IPackageFragmentRoot packageFragmentRoot= (IPackageFragmentRoot) javaElement;
				if (packageFragmentRoot.getKind() != IPackageFragmentRoot.K_SOURCE)
					return;
				IJavaScriptElement[] packageFragments= packageFragmentRoot.getChildren();
				for (int j= 0; j < packageFragments.length; j++)
					addAllCus(collector, packageFragments[j]);
				return;
		
			case IJavaScriptElement.PACKAGE_FRAGMENT:
				IPackageFragment packageFragment= (IPackageFragment) javaElement;
				collector.addAll(Arrays.asList(packageFragment.getJavaScriptUnits()));
				return;
			
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				collector.add(javaElement);
				return;
				
			default:
				IJavaScriptElement cu= javaElement.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
				if (cu != null)
					collector.add(cu);
		}
	}

	
	/**
	 * Sets all compliance settings in the given map to 5.0
	 */
	public static void set50CompilanceOptions(Map map) {
		setCompilanceOptions(map, JavaScriptCore.VERSION_1_5);
	}
	
	public static void setCompilanceOptions(Map map, String compliance) {
		if (JavaScriptCore.VERSION_1_6.equals(compliance)) {
			map.put(JavaScriptCore.COMPILER_COMPLIANCE, JavaScriptCore.VERSION_1_6);
			map.put(JavaScriptCore.COMPILER_SOURCE, JavaScriptCore.VERSION_1_6);
			map.put(JavaScriptCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaScriptCore.VERSION_1_6);
			map.put(JavaScriptCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaScriptCore.ERROR);
		} else if (JavaScriptCore.VERSION_1_5.equals(compliance)) {
			map.put(JavaScriptCore.COMPILER_COMPLIANCE, JavaScriptCore.VERSION_1_5);
			map.put(JavaScriptCore.COMPILER_SOURCE, JavaScriptCore.VERSION_1_5);
			map.put(JavaScriptCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaScriptCore.VERSION_1_5);
			map.put(JavaScriptCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaScriptCore.ERROR);
		} else if (JavaScriptCore.VERSION_1_4.equals(compliance)) {
			map.put(JavaScriptCore.COMPILER_COMPLIANCE, JavaScriptCore.VERSION_1_4);
			map.put(JavaScriptCore.COMPILER_SOURCE, JavaScriptCore.VERSION_1_3);
			map.put(JavaScriptCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaScriptCore.VERSION_1_2);
			map.put(JavaScriptCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaScriptCore.WARNING);
		} else if (JavaScriptCore.VERSION_1_3.equals(compliance)) {
			map.put(JavaScriptCore.COMPILER_COMPLIANCE, JavaScriptCore.VERSION_1_3);
			map.put(JavaScriptCore.COMPILER_SOURCE, JavaScriptCore.VERSION_1_3);
			map.put(JavaScriptCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaScriptCore.VERSION_1_1);
			map.put(JavaScriptCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaScriptCore.IGNORE);
		} else {
			throw new IllegalArgumentException("Unsupported compliance: " + compliance); //$NON-NLS-1$
		}
	}
	
	public static void setDefaultClassfileOptions(Map map, String compliance) {
		map.put(JavaScriptCore.COMPILER_CODEGEN_INLINE_JSR_BYTECODE, is50OrHigher(compliance) ? JavaScriptCore.ENABLED : JavaScriptCore.DISABLED);
		map.put(JavaScriptCore.COMPILER_LOCAL_VARIABLE_ATTR, JavaScriptCore.GENERATE);
		map.put(JavaScriptCore.COMPILER_LINE_NUMBER_ATTR, JavaScriptCore.GENERATE);
		map.put(JavaScriptCore.COMPILER_SOURCE_FILE_ATTR, JavaScriptCore.GENERATE);
		map.put(JavaScriptCore.COMPILER_CODEGEN_UNUSED_LOCAL, JavaScriptCore.PRESERVE);
	}
	
	/**
	 * @return returns if version 1 is less than version 2.
	 */
	public static boolean isVersionLessThan(String version1, String version2) {
		return version1.compareTo(version2) < 0;
	}
	
	public static boolean is50OrHigher(String compliance) {
		return !isVersionLessThan(compliance, JavaScriptCore.VERSION_1_5);
	}
	
	public static boolean is50OrHigher(IJavaScriptProject project) {
		return is50OrHigher(project.getOption(JavaScriptCore.COMPILER_COMPLIANCE, true));
	}
	
	public static boolean is50OrHigherJRE(IJavaScriptProject project) throws CoreException {
		IVMInstall vmInstall= JavaRuntime.getVMInstall(project);
		if (!(vmInstall instanceof IVMInstall2))
			return true; // assume 5.0.
		
		String compliance= getCompilerCompliance((IVMInstall2) vmInstall, null);
		if (compliance == null)
			return true; // assume 5.0
		return compliance.startsWith(JavaScriptCore.VERSION_1_5) || compliance.startsWith(JavaScriptCore.VERSION_1_6);
	}
	
	public static String getCompilerCompliance(IVMInstall2 vMInstall, String defaultCompliance) {
		String version= vMInstall.getJavaVersion();
		if (version == null) {
			return defaultCompliance;
		} else if (version.startsWith(JavaScriptCore.VERSION_1_6)) {
			return JavaScriptCore.VERSION_1_6;
		} else if (version.startsWith(JavaScriptCore.VERSION_1_5)) {
			return JavaScriptCore.VERSION_1_5;
		} else if (version.startsWith(JavaScriptCore.VERSION_1_4)) {
			return JavaScriptCore.VERSION_1_4;
		} else if (version.startsWith(JavaScriptCore.VERSION_1_3)) {
			return JavaScriptCore.VERSION_1_3;
		} else if (version.startsWith(JavaScriptCore.VERSION_1_2)) {
			return JavaScriptCore.VERSION_1_3;
		} else if (version.startsWith(JavaScriptCore.VERSION_1_1)) {
			return JavaScriptCore.VERSION_1_3;
		}
		return defaultCompliance;
	}
	
//	public static String getExecutionEnvironmentCompliance(IExecutionEnvironment executionEnvironment) {
//		String desc= executionEnvironment.getId();
//		if (desc.indexOf("1.6") != -1) { //$NON-NLS-1$
//			return JavaScriptCore.VERSION_1_6;
//		} else if (desc.indexOf("1.5") != -1) { //$NON-NLS-1$
//			return JavaScriptCore.VERSION_1_5;
//		} else if (desc.indexOf("1.4") != -1) { //$NON-NLS-1$
//			return JavaScriptCore.VERSION_1_4;
//		}
//		return JavaScriptCore.VERSION_1_3;
//	}

	/**
	 * Compute a new name for a compilation unit, given the name of the new main type.
	 * This query tries to maintain the existing extension (e.g. ".java").
	 * 
	 * @param cu a compilation unit
	 * @param newMainName the new name of the cu's main type (without extension)
	 * @return the new name for the compilation unit  
	 */
	public static String getRenamedCUName(IJavaScriptUnit cu, String newMainName) {
		String oldName = cu.getElementName();
		int i = oldName.lastIndexOf('.');
		if (i != -1) {
			return newMainName + oldName.substring(i);
		} else {
			return newMainName;
		}
	}	
	
	/**
	 * Applies an text edit to a compilation unit. Filed bug 117694 against jdt.core. 
	 * 	@param cu the compilation unit to apply the edit to
	 * 	@param edit the edit to apply
	 * @param save is set, save the CU after the edit has been applied
	 * @param monitor the progress monitor to use
	 * @throws CoreException Thrown when the access to the CU failed
	 * @throws ValidateEditException if validate edit fails
	 */	
	public static void applyEdit(IJavaScriptUnit cu, TextEdit edit, boolean save, IProgressMonitor monitor) throws CoreException, ValidateEditException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		monitor.beginTask(CorextMessages.JavaModelUtil_applyedit_operation, 3); 

		try {
			IDocument document= null;
			try {
				document= aquireDocument(cu, new SubProgressMonitor(monitor, 1));
				if (save) {
					commitDocument(cu, document, edit, new SubProgressMonitor(monitor, 1));
				} else {
					new RewriteSessionEditProcessor(document, edit, TextEdit.UPDATE_REGIONS).performEdits();
				}
			} catch (BadLocationException e) {
				throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
			} finally {
				releaseDocument(cu, document, new SubProgressMonitor(monitor, 1));
			}
		} finally {
			monitor.done();
		}		
	}

	private static IDocument aquireDocument(IJavaScriptUnit cu, IProgressMonitor monitor) throws CoreException {
		if (JavaModelUtil.isPrimary(cu)) {
			IFile file= (IFile) cu.getResource();
			if (file.exists()) {
				ITextFileBufferManager bufferManager= FileBuffers.getTextFileBufferManager();
				IPath path= cu.getPath();
				bufferManager.connect(path, LocationKind.IFILE, monitor);
				return bufferManager.getTextFileBuffer(path, LocationKind.IFILE).getDocument();
			}
		}
		monitor.done();
		return new Document(cu.getSource());
	}
	
	private static void commitDocument(IJavaScriptUnit cu, IDocument document, TextEdit edit, IProgressMonitor monitor) throws CoreException, MalformedTreeException, BadLocationException {
		if (JavaModelUtil.isPrimary(cu)) {
			IFile file= (IFile) cu.getResource();
			if (file.exists()) {
				IStatus status= Resources.makeCommittable(file, null);
				if (!status.isOK()) {
					throw new ValidateEditException(status);
				}
				new RewriteSessionEditProcessor(document, edit, TextEdit.UPDATE_REGIONS).performEdits(); // apply after file is commitable
				
				ITextFileBufferManager bufferManager= FileBuffers.getTextFileBufferManager();
				bufferManager.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE).commit(monitor, true);
				return;
			}
		}
		// no commit possible, make sure changes are in
		new RewriteSessionEditProcessor(document, edit, TextEdit.UPDATE_REGIONS).performEdits();
	}

	
	private static void releaseDocument(IJavaScriptUnit cu, IDocument document, IProgressMonitor monitor) throws CoreException {
		if (JavaModelUtil.isPrimary(cu)) {
			IFile file= (IFile) cu.getResource();
			if (file.exists()) {
				ITextFileBufferManager bufferManager= FileBuffers.getTextFileBufferManager();
				bufferManager.disconnect(file.getFullPath(), LocationKind.IFILE, monitor);
				return;
			}
		}
		cu.getBuffer().setContents(document.get());
		monitor.done();
	}
	
	public static boolean isImplicitImport(String qualifier, IJavaScriptUnit cu) {
		if ("java.lang".equals(qualifier)) {  //$NON-NLS-1$
			return true;
		}
		String packageName= cu.getParent().getElementName();
		if (qualifier.equals(packageName)) {
			return true;
		}
		String typeName= JavaScriptCore.removeJavaScriptLikeExtension(cu.getElementName());
		String mainTypeName= JavaModelUtil.concatenateName(packageName, typeName);
		return qualifier.equals(mainTypeName);
	}

	public static boolean isOpenableStorage(Object storage) {
		if (storage instanceof IJarEntryResource) {
			return ((IJarEntryResource) storage).isFile();
		} else {
			return storage instanceof IStorage;
		}
	}
	
	/**
	 * Returns true iff the given local variable is a parameter of its
	 * declaring method.
	 * 
	 * TODO replace this method with new API when available: 
	 * 		https://bugs.eclipse.org/bugs/show_bug.cgi?id=48420
	 * @param currentLocal the local variable to test
	 * 
	 * @return returns true if the variable is a parameter
	 * @throws JavaScriptModelException 
	 */
	public static boolean isParameter(ILocalVariable currentLocal) throws JavaScriptModelException {

		final IJavaScriptElement parent= currentLocal.getParent();
		if (parent instanceof IFunction) {
			final String[] params= ((IFunction) parent).getParameterNames();
			for (int i= 0; i < params.length; i++) {
				if (params[i].equals(currentLocal.getElementName()))
					return true;
			}
		}
		return false;
	}
	
	
	public static String getFilePackage(IJavaScriptElement javaElement)
	{
		IJavaScriptElement fileAncestor = javaElement.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
		if (fileAncestor==null)
			fileAncestor=javaElement.getAncestor(IJavaScriptElement.CLASS_FILE);
		IPath filePath= fileAncestor.getResource().getFullPath();
		IJavaScriptElement rootElement=fileAncestor.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
		IPath rootPath = rootElement.getResource().getFullPath();
		String relativePath = filePath.removeFirstSegments(rootPath.segmentCount()).toPortableString();
		int index=Util.indexOfJavaLikeExtension(relativePath);
		if (index>=0)
			relativePath=relativePath.substring(0,index);
		relativePath=relativePath.replace('/', '.');
		return relativePath;
		
	}
}
