/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IInitializer;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IOpenable;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.IInferenceFile;
import org.eclipse.wst.jsdt.core.infer.InferrenceManager;
import org.eclipse.wst.jsdt.core.infer.InferrenceProvider;
import org.eclipse.wst.jsdt.core.infer.ResolutionConfiguration;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.wst.jsdt.internal.compiler.util.SuffixConstants;
import org.eclipse.wst.jsdt.internal.core.search.BasicSearchEngine;
import org.eclipse.wst.jsdt.internal.core.search.IRestrictedAccessBindingRequestor;
import org.eclipse.wst.jsdt.internal.core.util.HandleFactory;
import org.eclipse.wst.jsdt.internal.core.util.HashtableOfArrayToObject;
import org.eclipse.wst.jsdt.internal.core.util.Messages;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * A <code>NameLookup</code> provides name resolution within a Java project.
 * The name lookup facility uses the project's classpath to prioritize the
 * order in which package fragments are searched when resolving a name.
 *
 * <p>Name lookup only returns a handle when the named element actually
 * exists in the model; otherwise <code>null</code> is returned.
 *
 * <p>There are two logical sets of methods within this interface.  Methods
 * which start with <code>find*</code> are intended to be convenience methods for quickly
 * finding an element within another element; for instance, for finding a class within a
 * package.  The other set of methods all begin with <code>seek*</code>.  These methods
 * do comprehensive searches of the <code>IJavaScriptProject</code> returning hits
 * in real time through an <code>IJavaElementRequestor</code>.
 *
 */
public class NameLookup implements SuffixConstants {
	public static class Answer {
		public IType type;
		public Object element;
		AccessRestriction restriction;
		Answer(IType type, AccessRestriction restriction) {
			this.type = type;
			this.restriction = restriction;
		}
		Answer(Object element, AccessRestriction restriction) {
			this.element = element;
			this.restriction = restriction;
		}
		public boolean ignoreIfBetter() {
			return this.restriction != null && this.restriction.ignoreIfBetter();
		}
		/*
		 * Returns whether this answer is better than the other awswer.
		 * (accessible is better than discouraged, which is better than
		 * non-accessible)
		 */
		public boolean isBetter(Answer otherAnswer) {
			if (otherAnswer == null) return true;
			if (this.restriction == null) return true;
			return otherAnswer.restriction != null
				&& this.restriction.getProblemId() < otherAnswer.restriction.getProblemId();
		}
	}

	// TODO (jerome) suppress the accept flags (qualified name is sufficient to find a type)
	/**
	 * Accept flag for specifying classes.
	 */
	public static final int ACCEPT_CLASSES = ASTNode.Bit2;

	/**
	 * Accept flag for specifying interfaces.
	 */
	public static final int ACCEPT_INTERFACES = ASTNode.Bit3;

	/**
	 * Accept flag for specifying enums.
	 */
	public static final int ACCEPT_ENUMS = ASTNode.Bit4;

	/**
	 * Accept flag for specifying annotations.
	 */
	public static final int ACCEPT_ANNOTATIONS = ASTNode.Bit5;

	/*
	 * Accept flag for all kinds of types
	 */
	public static final int ACCEPT_ALL = ACCEPT_CLASSES | ACCEPT_INTERFACES | ACCEPT_ENUMS | ACCEPT_ANNOTATIONS;

	public static boolean VERBOSE = false;

	private static final IType[] NO_TYPES = {};
	private static final IJavaScriptElement[] NO_BINDINGS = {};

	private boolean searchFiles=true;
	
	/**
	 * The <code>IPackageFragmentRoot</code>'s associated
	 * with the classpath of this NameLookup facility's
	 * project.
	 */
	protected IPackageFragmentRoot[] packageFragmentRoots;

	/**
	 * Table that maps package names to lists of package fragment roots
	 * that contain such a package known by this name lookup facility.
	 * To allow > 1 package fragment with the same name, values are
	 * arrays of package fragment roots ordered as they appear on the
	 * classpath.
	 * Note if the list is of size 1, then the IPackageFragmentRoot object
	 * replaces the array.
	 */
	protected HashtableOfArrayToObject packageFragments;

	/**
	 * Reverse map from root path to corresponding resolved CP entry
	 * (so as to be able to figure inclusion/exclusion rules)
	 */
	protected Map rootToResolvedEntries;

	/**
	 * A map from package handles to a map from type name to an IType or an IType[].
	 * Allows working copies to take precedence over compilation units.
	 */
	protected HashMap typesInWorkingCopies;
	protected HashMap[] bindingsInWorkingCopies;

	public long timeSpentInSeekTypesInSourcePackage = 0;
	public long timeSpentInSeekTypesInBinaryPackage = 0;

	protected HashSet acceptedCUs=new HashSet();
	private IJavaScriptUnit[] workingCopies;

	IRestrictedAccessBindingRequestor restrictedRequestor;

	public NameLookup(
			IPackageFragmentRoot[] packageFragmentRoots,
			HashtableOfArrayToObject packageFragments,
			IJavaScriptUnit[] workingCopies,
			Map rootToResolvedEntries) {
		long start = -1;
		if (VERBOSE) {
			Util.verbose(" BUILDING NameLoopkup");  //$NON-NLS-1$
			Util.verbose(" -> pkg roots size: " + (packageFragmentRoots == null ? 0 : packageFragmentRoots.length));  //$NON-NLS-1$
			Util.verbose(" -> pkgs size: " + (packageFragments == null ? 0 : packageFragments.size()));  //$NON-NLS-1$
			Util.verbose(" -> working copy size: " + (workingCopies == null ? 0 : workingCopies.length));  //$NON-NLS-1$
			start = System.currentTimeMillis();
		}
//		this.restrictedRequestor=restrictedRequestor;
		//this.restrictToLanguage=restrictToLanguage;
		this.packageFragmentRoots = packageFragmentRoots;
		if (workingCopies == null) {
			this.packageFragments = packageFragments;
		} else {
			// clone tables as we're adding packages from working copies
			try {
				this.packageFragments = (HashtableOfArrayToObject) packageFragments.clone();
			} catch (CloneNotSupportedException e1) {
				// ignore (implementation of HashtableOfArrayToObject supports cloning)
			}
			this.typesInWorkingCopies = new HashMap();
			this.bindingsInWorkingCopies = new HashMap[Binding.NUMBER_BASIC_BINDING];
			for (int j = 0; j <Binding.NUMBER_BASIC_BINDING; j++) {
				this.bindingsInWorkingCopies[j] = new HashMap();
			}
			this.workingCopies=workingCopies;
			for (int i = 0, length = workingCopies.length; i < length; i++) {
				IJavaScriptUnit workingCopy = workingCopies[i];
				
				try {
					IType[] types = workingCopy.getTypes();
					int typeLength = types.length;
					if (typeLength == 0) {
						String typeName = Util.getNameWithoutJavaLikeExtension(workingCopy.getElementName());
						this.typesInWorkingCopies.put(typeName, NO_TYPES);
					} else {
						for (int j = 0; j < typeLength; j++) {
							IType type = types[j];
							String typeName = type.getElementName();
							Object existing = this.typesInWorkingCopies.get(typeName);
							if (existing == null) {
								this.typesInWorkingCopies.put(typeName, type);
							} else if (existing instanceof IType) {
								this.typesInWorkingCopies.put(typeName, new IType[] {(IType) existing, type});
							} else {
								IType[] existingTypes = (IType[]) existing;
								int existingTypeLength = existingTypes.length;
								System.arraycopy(existingTypes, 0, existingTypes = new IType[existingTypeLength+1], 0, existingTypeLength);
								existingTypes[existingTypeLength] = type;
								this.typesInWorkingCopies.put(typeName, existingTypes);
							}
						}
					}

					addWorkingCopyBindings(types, this.bindingsInWorkingCopies[Binding.TYPE]);
					addWorkingCopyBindings(workingCopy.getFields(), this.bindingsInWorkingCopies[Binding.VARIABLE]);
					addWorkingCopyBindings(workingCopy.getFields(), this.bindingsInWorkingCopies[Binding.LOCAL]);
					addWorkingCopyBindings(workingCopy.getFunctions(), this.bindingsInWorkingCopies[Binding.METHOD]);

				} catch (JavaScriptModelException e) {
					// working copy doesn't exist -> ignore
				}

				// add root of package fragment to cache
				PackageFragment pkg = (PackageFragment) workingCopy.getParent();
				IPackageFragmentRoot root = (IPackageFragmentRoot) pkg.getParent();
				String[] pkgName = pkg.names;
				Object existing = this.packageFragments.get(pkgName);
				if (existing == null || existing == JavaProjectElementInfo.NO_ROOTS) {
					this.packageFragments.put(pkgName, root);
					// ensure super packages (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=119161)
					// are also in the map
					JavaProjectElementInfo.addSuperPackageNames(pkgName, this.packageFragments);
				} else {
					if (existing instanceof PackageFragmentRoot) {
						if (!existing.equals(root))
							this.packageFragments.put(pkgName, new IPackageFragmentRoot[] {(PackageFragmentRoot) existing, root});
					} else {
						IPackageFragmentRoot[] roots = (IPackageFragmentRoot[]) existing;
						int rootLength = roots.length;
						boolean containsRoot = false;
						for (int j = 0; j < rootLength; j++) {
							if (roots[j].equals(root)) {
								containsRoot = true;
								break;
							}
						}
						if (containsRoot) {
							System.arraycopy(roots, 0, roots = new IPackageFragmentRoot[rootLength+1], 0, rootLength);
							roots[rootLength] = root;
							this.packageFragments.put(pkgName, roots);
						}
					}
				}
			}
		}

		this.rootToResolvedEntries = rootToResolvedEntries;
        if (VERBOSE) {
            Util.verbose(" -> spent: " + (System.currentTimeMillis() - start) + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
        }
	}

	public void setRestrictedAccessRequestor(IRestrictedAccessBindingRequestor restrictedRequestor) {
		this.restrictedRequestor=restrictedRequestor;
	}

	protected  IRestrictedAccessBindingRequestor getRestrictedAccessRequestor() {
		if(this.restrictedRequestor==null) {
			this.restrictedRequestor=new IRestrictedAccessBindingRequestor() {
				ArrayList foundPaths=new ArrayList();
				String excludePath;

				public void setExcludePath(String excludePath) {
					this.excludePath = excludePath;
				}
				public boolean acceptBinding(int type,int modifiers, char[] packageName,
						char[] simpleTypeName,
						String path, AccessRestriction access) {
					if (path == null)
						return false; // Since path is used to create openables we cannot accept bindings without a path
					if (excludePath!=null && path.equals(excludePath))
						return false;
					if(!foundPaths.contains(path))
						foundPaths.add(path);
					return true;
				}
				/* (non-Javadoc)
				 * @see org.eclipse.wst.jsdt.internal.core.search.IRestrictedAccessBindingRequestor#getFoundPaths()
				 */
				public String getFoundPath() {
					return foundPaths.size()>0?(String)foundPaths.get(0):null;
				}


				public void reset() {
					foundPaths.clear();
				}
				public ArrayList getFoundPaths()
				{
					return foundPaths;
				}

			};
		}

		return this.restrictedRequestor;
	}

	private void addWorkingCopyBindings(IJavaScriptElement [] elements, HashMap bindingsMap)
	{
		for (int j = 0; j < elements.length; j++) {
			IJavaScriptElement element = elements[j];
			String elementName = element.getElementName();
			Object existing = bindingsMap.get(elementName);
			if (existing == null) {
				bindingsMap.put(elementName, element);
			} else if (existing instanceof IJavaScriptElement) {
				bindingsMap.put(elementName, new IJavaScriptElement[] {(IJavaScriptElement) existing, element});
			} else {
				IJavaScriptElement[] existingElements = (IJavaScriptElement[]) existing;
				int existingElementsLength = existingElements.length;
				System.arraycopy(existingElements, 0, existingElements = new IJavaScriptElement[existingElementsLength+1], 0, existingElementsLength);
				existingElements[existingElementsLength] = element;
				bindingsMap.put(elementName, existingElements);
			}
		}

	}

	/**
	 * Returns true if:<ul>
	 *  <li>the given type is an existing class and the flag's <code>ACCEPT_CLASSES</code>
	 *      bit is on
	 *  <li>the given type is an existing interface and the <code>ACCEPT_INTERFACES</code>
	 *      bit is on
	 *  <li>neither the <code>ACCEPT_CLASSES</code> or <code>ACCEPT_INTERFACES</code>
	 *      bit is on
	 *  </ul>
	 * Otherwise, false is returned.
	 */
	protected boolean acceptType(IType type, int acceptFlags, boolean isSourceType) {
		if (!type.exists())
			return false;
		if (acceptFlags == 0 || acceptFlags == ACCEPT_ALL)
			return true; // no flags or all flags, always accepted
		try {
			int kind =  TypeDeclaration.kind(((SourceTypeElementInfo) ((SourceType) type).getElementInfo()).getModifiers());

//			int kind = isSourceType
//					? TypeDeclaration.kind(((SourceTypeElementInfo) ((SourceType) type).getElementInfo()).getModifiers())
//					: TypeDeclaration.kind(((IBinaryType) ((BinaryType) type).getElementInfo()).getModifiers());
			switch (kind) {
				case TypeDeclaration.CLASS_DECL :
					return (acceptFlags & ACCEPT_CLASSES) != 0;
				default:
					//case IGenericType.ANNOTATION_TYPE :
					return (acceptFlags & ACCEPT_ANNOTATIONS) != 0;
			}
		} catch (JavaScriptModelException npe) {
			return false; // the class is not present, do not accept.
		}
	}

	protected boolean doAcceptBinding(IJavaScriptElement element, int bindingType, boolean isSourceType,IJavaElementRequestor requestor) {
		switch (bindingType)
		{
		  case Binding.FIELD | Binding.METHOD:
			  if (element instanceof IFunction)
			  {

				  requestor.acceptMethod((IFunction)element);
				  return true;
			  }
			  if (element instanceof IField)
			  {
				  requestor.acceptField( (IField)element);
				  return true;
			  }
			  return false;
		  case Binding.FIELD:
		  case Binding.VARIABLE:
			  if (element instanceof IField)
			  {
				  requestor.acceptField( (IField)element);
				  return true;
			  }
			  return false;
		  case Binding.METHOD:
			  if (element instanceof IFunction)
			  {
				  requestor.acceptMethod((IFunction)element);
				  return true;
			  }
		  case Binding.TYPE:
			  if (element instanceof IType)
			  {
				  requestor.acceptType((IType)element);
				  return true;
			  }

		}
		return false;
	}

	/**
	 * Finds every type in the project whose simple name matches
	 * the prefix, informing the requestor of each hit. The requestor
	 * is polled for cancellation at regular intervals.
	 *
	 * <p>The <code>partialMatch</code> argument indicates partial matches
	 * should be considered.
	 */
	private void findAllTypes(String prefix, boolean partialMatch, int acceptFlags, IJavaElementRequestor requestor) {
		int count= this.packageFragmentRoots.length;
		for (int i= 0; i < count; i++) {
			if (requestor.isCanceled())
				return;
			IPackageFragmentRoot root= this.packageFragmentRoots[i];
			IJavaScriptElement[] packages= null;
			try {
				packages= root.getChildren();
			} catch (JavaScriptModelException npe) {
				continue; // the root is not present, continue;
			}
			if (packages != null) {
				for (int j= 0, packageCount= packages.length; j < packageCount; j++) {
					if (requestor.isCanceled())
						return;
					seekTypes(prefix, (IPackageFragment) packages[j], partialMatch, acceptFlags, requestor);
				}
			}
		}
	}

	private void findAllBindings(String prefix,int bindingType, boolean partialMatch, int acceptFlags, IJavaElementRequestor requestor) {
		int count= this.packageFragmentRoots.length;
		for (int i= 0; i < count; i++) {
			if (requestor.isCanceled())
				return;
			IPackageFragmentRoot root= this.packageFragmentRoots[i];
			IJavaScriptElement[] packages= null;
			try {
				packages= root.getChildren();
			} catch (JavaScriptModelException npe) {
				continue; // the root is not present, continue;
			}
			if (packages != null) {
				for (int j= 0, packageCount= packages.length; j < packageCount; j++) {
					if (requestor.isCanceled())
						return;
					seekBindings(prefix,bindingType, (IPackageFragment) packages[j], partialMatch, acceptFlags, requestor);
				}
			}
		}
	}

	/**
	 * Returns the <code>IJavaScriptUnit</code> which defines the type
	 * named <code>qualifiedTypeName</code>, or <code>null</code> if
	 * none exists. The domain of the search is bounded by the classpath
	 * of the <code>IJavaScriptProject</code> this <code>NameLookup</code> was
	 * obtained from.
	 * <p>
	 * The name must be fully qualified (eg "java.lang.Object", "java.util.Hashtable$Entry")
	 */
	public ITypeRoot findCompilationUnit(String qualifiedTypeName) {
		String[] pkgName = CharOperation.NO_STRINGS;
		String cuName = qualifiedTypeName;

		int index= qualifiedTypeName.lastIndexOf('.');
		if (index != -1) {
			pkgName= Util.splitOn('.', qualifiedTypeName, 0, index);
			cuName= qualifiedTypeName.substring(index + 1);
		}
		cuName=cuName.replace(CompilationUnitScope.FILENAME_DOT_SUBSTITUTION, '.');
		Object value = this.packageFragments.get(pkgName);
		if (value != null) {
			if (value instanceof PackageFragmentRoot) {
				return findCompilationUnit(pkgName, cuName, (PackageFragmentRoot) value);
			} else {
				IPackageFragmentRoot[] roots = (IPackageFragmentRoot[]) value;
				for (int i= 0; i < roots.length; i++) {
					PackageFragmentRoot root= (PackageFragmentRoot) roots[i];
					ITypeRoot cu = findCompilationUnit(pkgName, cuName, root);
					if (cu != null)
						return cu;
				}
			}
		}
		return null;
	}

	private ITypeRoot findCompilationUnit(String[] pkgName, String cuName, PackageFragmentRoot root) {
		if (!root.isArchive()) {
			IPackageFragment pkg = root.getPackageFragment(pkgName);
			try {
				IJavaScriptUnit[] cus = pkg.getJavaScriptUnits();
				for (int j = 0, length = cus.length; j < length; j++) {
					IJavaScriptUnit cu = cus[j];
					if (Util.equalsIgnoreJavaLikeExtension(cu.getElementName(), cuName))
						return cu;
				}
				IClassFile[] classFiles = pkg.getClassFiles();
				for (int j = 0, length = classFiles.length; j < length; j++) {
					IClassFile cu = classFiles[j];
					if (Util.equalsIgnoreJavaLikeExtension(cu.getElementName(), cuName))
						return cu;
				}
			} catch (JavaScriptModelException e) {
				// pkg does not exist
				// -> try next package
			}
		}
		return null;
}

	/**
	 * Returns the package fragment whose path matches the given
	 * (absolute) path, or <code>null</code> if none exist. The domain of
	 * the search is bounded by the classpath of the <code>IJavaScriptProject</code>
	 * this <code>NameLookup</code> was obtained from.
	 * The path can be:
	 * 	- internal to the workbench: "/Project/src"
	 *  - external to the workbench: "c:/jdk/classes.zip/java/lang"
	 */
	public IPackageFragment findPackageFragment(IPath path) {
		if (!path.isAbsolute()) {
			throw new IllegalArgumentException(Messages.path_mustBeAbsolute);
		}
/*
 * TODO (jerome) this code should rather use the package fragment map to find the candidate package, then
 * check if the respective enclosing root maps to the one on this given IPath.
 */
		IResource possibleFragment = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
		if (possibleFragment == null) {
			//external jar
			for (int i = 0; i < this.packageFragmentRoots.length; i++) {
				IPackageFragmentRoot root = this.packageFragmentRoots[i];
				if (!root.isExternal()) {
					continue;
				}
				IPath rootPath = root.getPath();
				int matchingCount = rootPath.matchingFirstSegments(path);
				if (matchingCount != 0) {
					String name = path.toOSString();
					// + 1 is for the File.separatorChar
					name = name.substring(rootPath.toOSString().length() + 1, name.length());
					name = name.replace(File.separatorChar, '.');
					IJavaScriptElement[] list = null;
					try {
						list = root.getChildren();
					} catch (JavaScriptModelException npe) {
						continue; // the package fragment root is not present;
					}
					int elementCount = list.length;
					for (int j = 0; j < elementCount; j++) {
						IPackageFragment packageFragment = (IPackageFragment) list[j];
						if (nameMatches(name, packageFragment, false)) {
							return packageFragment;
						}
					}
				}
			}
		} else {
			IJavaScriptElement fromFactory = JavaScriptCore.create(possibleFragment);
			if (fromFactory == null) {
				return null;
			}
			switch (fromFactory.getElementType()) {
				case IJavaScriptElement.PACKAGE_FRAGMENT:
					return (IPackageFragment) fromFactory;
				case IJavaScriptElement.JAVASCRIPT_PROJECT:
					// default package in a default root
					JavaProject project = (JavaProject) fromFactory;
					try {
						IIncludePathEntry entry = project.getClasspathEntryFor(path);
						if (entry != null) {
							IPackageFragmentRoot root =
								project.getPackageFragmentRoot(project.getResource());
							Object defaultPkgRoot = this.packageFragments.get(CharOperation.NO_STRINGS);
							if (defaultPkgRoot == null) {
								return null;
							}
							if (defaultPkgRoot instanceof PackageFragmentRoot && defaultPkgRoot.equals(root))
								return  ((PackageFragmentRoot) root).getPackageFragment(CharOperation.NO_STRINGS);
							else {
								IPackageFragmentRoot[] roots = (IPackageFragmentRoot[]) defaultPkgRoot;
								for (int i = 0; i < roots.length; i++) {
									if (roots[i].equals(root)) {
										return  ((PackageFragmentRoot) root).getPackageFragment(CharOperation.NO_STRINGS);
									}
								}
							}
						}
					} catch (JavaScriptModelException e) {
						return null;
					}
					return null;
				case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
					return ((PackageFragmentRoot)fromFactory).getPackageFragment(CharOperation.NO_STRINGS);
			}
		}
		return null;
	}

	/**
	 * Returns the package fragments whose name matches the given
	 * (qualified) name, or <code>null</code> if none exist.
	 *
	 * The name can be:
	 * <ul>
	 *		<li>empty: ""</li>
	 *		<li>qualified: "pack.pack1.pack2"</li>
	 * </ul>
	 * @param partialMatch partial name matches qualify when <code>true</code>,
	 *	only exact name matches qualify when <code>false</code>
	 */
	public IPackageFragment[] findPackageFragments(String name, boolean partialMatch) {
		return findPackageFragments(name, partialMatch, false);
	}

	/**
	 * Returns the package fragments whose name matches the given
	 * (qualified) name or pattern, or <code>null</code> if none exist.
	 *
	 * The name can be:
	 * <ul>
	 *		<li>empty: ""</li>
	 *		<li>qualified: "pack.pack1.pack2"</li>
	 * 	<li>a pattern: "pack.*.util"</li>
	 * </ul>
	 * @param partialMatch partial name matches qualify when <code>true</code>,
	 * @param patternMatch <code>true</code> when the given name might be a pattern,
	 *		<code>false</code> otherwise.
	 */
	public IPackageFragment[] findPackageFragments(String name, boolean partialMatch, boolean patternMatch) {
		ArrayList fragRootChildren = new ArrayList();
		for (int i = 0; i < this.packageFragmentRoots.length; i++) {
			IJavaScriptElement[] children;
			try {
				children = packageFragmentRoots[i].getChildren();
				for (int j = 0; j < children.length; j++) {
					IPackageFragment packageFragment = (IPackageFragment)children[j];
					if(packageFragment!=null && packageFragment.getElementName().equals(name))
						fragRootChildren.add((packageFragment));
				}
			} catch (JavaScriptModelException e) {}

		}

		return (IPackageFragment[])fragRootChildren.toArray(new IPackageFragment[fragRootChildren.size()]);
		//		if (partialMatch) {
//			String[] splittedName = Util.splitOn('.', name, 0, name.length());
//			IPackageFragment[] oneFragment = null;
//			ArrayList pkgs = null;
//			Object[][] keys = this.packageFragments.keyTable;
//			for (int i = 0, length = keys.length; i < length; i++) {
//				String[] pkgName = (String[]) keys[i];
//				if (pkgName != null && Util.startsWithIgnoreCase(pkgName, splittedName)) {
//					Object value = this.packageFragments.valueTable[i];
//					if (value instanceof PackageFragmentRoot) {
//						IPackageFragment pkg = ((PackageFragmentRoot) value).getPackageFragment(pkgName);
//						if (oneFragment == null) {
//							oneFragment = new IPackageFragment[] {pkg};
//						} else {
//							if (pkgs == null) {
//								pkgs = new ArrayList();
//								pkgs.add(oneFragment[0]);
//							}
//							pkgs.add(pkg);
//						}
//					} else {
//						IPackageFragmentRoot[] roots = (IPackageFragmentRoot[]) value;
//						for (int j = 0, length2 = roots.length; j < length2; j++) {
//							PackageFragmentRoot root = (PackageFragmentRoot) roots[j];
//							IPackageFragment pkg = root.getPackageFragment(pkgName);
//							if (oneFragment == null) {
//								oneFragment = new IPackageFragment[] {pkg};
//							} else {
//								if (pkgs == null) {
//									pkgs = new ArrayList();
//									pkgs.add(oneFragment[0]);
//								}
//								pkgs.add(pkg);
//							}
//						}
//					}
//				}
//			}
//			if (pkgs == null) return oneFragment;
//			int resultLength = pkgs.size();
//			IPackageFragment[] result = new IPackageFragment[resultLength];
//			pkgs.toArray(result);
//			return result;
//		} else {
//			String[] splittedName = (name.length()>0)? new String[]{name}: new String[0];//Util.splitOn('.', name, 0, name.length());
//			Object value = this.packageFragments.get(splittedName);
//			if (value==null)
//				value=this.packageFragments.get(new String[]{name});
//			if (value == null)
//				return null;
//			if (value instanceof PackageFragmentRoot) {
//				return new IPackageFragment[] {((PackageFragmentRoot) value).getPackageFragment(splittedName)};
//			} else {
//				IPackageFragmentRoot[] roots = (IPackageFragmentRoot[]) value;
//				IPackageFragment[] result = new IPackageFragment[roots.length];
//				for (int i= 0; i < roots.length; i++) {
//					result[i] = ((PackageFragmentRoot) roots[i]).getPackageFragment(splittedName);
//				}
//				return result;
//			}
//		}
	}

	/*
	 * Find secondary type for a project.
	 */
	private IType findSecondaryType(String packageName, String typeName, IJavaScriptProject project, boolean waitForIndexes, IProgressMonitor monitor) {
		if (JavaModelManager.VERBOSE) {
			Util.verbose("NameLookup FIND SECONDARY TYPES:"); //$NON-NLS-1$
			Util.verbose(" -> pkg name: " + packageName);  //$NON-NLS-1$
			Util.verbose(" -> type name: " + typeName);  //$NON-NLS-1$
			Util.verbose(" -> project: "+project.getElementName()); //$NON-NLS-1$
		}
		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		try {
			IJavaScriptProject javaProject = project;
			Map secondaryTypePaths = manager.secondaryTypes(javaProject, waitForIndexes, monitor);
			if (secondaryTypePaths.size() > 0) {
				Map types = (Map) secondaryTypePaths.get(packageName==null?"":packageName); //$NON-NLS-1$
				if (types != null && types.size() > 0) {
					IType type = (IType) types.get(typeName);
					if (type != null) {
						if (JavaModelManager.VERBOSE) {
							Util.verbose(" -> type: " + type.getElementName());  //$NON-NLS-1$
						}
						return type;
					}
				}
			}
		}
		catch (JavaScriptModelException jme) {
			// give up
		}
		return null;
	}

	/**
	 * Find type considering secondary types but without waiting for indexes.
	 * It means that secondary types may be not found under certain circumstances...
	 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=118789"
	 */
	public Answer findType(String typeName, String packageName, boolean partialMatch, int acceptFlags, boolean checkRestrictions) {

		if (USE_BINDING_SEARCH && this.searchFiles)
		{
			Answer answer =findBindingSearch(typeName, packageName, Binding.TYPE, partialMatch, acceptFlags, true, true, checkRestrictions, null,false,null);
			if (answer!=null && answer.type==null && answer.element instanceof ITypeRoot)
			{
				ITypeRoot typeroot=(ITypeRoot)answer.element;
				answer.type=typeroot.getType(typeName);
			}
			return answer;
		}

		return findType(typeName,
			packageName,
			partialMatch,
			acceptFlags,
			true/* consider secondary types */,
			false/* do NOT wait for indexes */,
			checkRestrictions,
			null);
	}

	public Answer findBinding(String typeName, String packageName,int type, boolean partialMatch,
			int acceptFlags, boolean checkRestrictions, boolean returnMultiple , String excludePath){
		
		if ((type&Binding.COMPILATION_UNIT)!=0)
		{
			String fullName=typeName;
			if (packageName.length()>0)
				fullName=packageName+"."+typeName;
			ITypeRoot compilationUnit = findCompilationUnit(fullName);
			if (compilationUnit!=null )
			{
				return new Answer(compilationUnit, null);
			}
			type &= ~Binding.COMPILATION_UNIT;
			if (type==0)
				return null;
		}
		
		if ((type&Binding.PACKAGE)!=0)
		{
			type &= ~Binding.PACKAGE;
			if (type==0)
				return null;
		}
		
		if (USE_BINDING_SEARCH && searchFiles)
		return findBindingSearch(typeName,
			packageName,
			type,
			partialMatch,
			acceptFlags,
			true/* consider secondary types */,
			false/* do NOT wait for indexes */,
			checkRestrictions,
			null,  returnMultiple,  excludePath);
		return findBinding(typeName,
				packageName,
				type,
				partialMatch,
				acceptFlags,
				true/* consider secondary types */,
				false/* do NOT wait for indexes */,
				checkRestrictions,
				null);
	}

	/* return all CUs defining a type */
	/**
	 * Returns all compilationUnits containing a specific type
	 *
	 * NOT FULLY IMPLEMENTED YET.
	 *
	 * @param typeName
	 * @param waitForIndexes
	 * @return all found compilationUnits containg [typeName]
	 */
	public IJavaScriptUnit[] findTypeSources(String typeName, boolean waitForIndexes) {

		// Look for concerned package fragments
		JavaElementRequestor elementRequestor = new JavaElementRequestor();
		seekPackageFragments(IPackageFragment.DEFAULT_PACKAGE_NAME, false, elementRequestor);
		IPackageFragment[] packages= elementRequestor.getPackageFragments();

		IType type = null;
		int length= packages.length;
		HashSet projects = null;
		IJavaScriptProject javaProject = null;
//		Answer suggestedAnswer = null;
		ArrayList found = new ArrayList();

		for (int i= 0; i < length; i++) {
			type = findType(typeName, packages[i], false, NameLookup.ACCEPT_ALL);
			if (type != null && type.exists()) {
				found.add(type);
			}
			if (javaProject == null) {
				javaProject = packages[i].getJavaScriptProject();
			} else if (projects == null)  {
				if (!javaProject.equals(packages[i].getJavaScriptProject())) {
					projects = new HashSet(3);
					projects.add(javaProject);
					projects.add(packages[i].getJavaScriptProject());
				}
			} else {
				projects.add(packages[i].getJavaScriptProject());
			}

		}

		// If type was not found, try to find it as secondary in source folders
		if (javaProject != null) {
			if (projects == null) {
				type = findSecondaryType(IPackageFragment.DEFAULT_PACKAGE_NAME, typeName, javaProject, waitForIndexes, new NullProgressMonitor());
			} else {
				Iterator allProjects = projects.iterator();
				while (type == null && allProjects.hasNext()) {
					type = findSecondaryType(IPackageFragment.DEFAULT_PACKAGE_NAME, typeName, (IJavaScriptProject) allProjects.next(), waitForIndexes, new NullProgressMonitor());
				}
			}
		}
		return null;
	}

	/**
	 * Find type. Considering secondary types and waiting for indexes depends on given corresponding parameters.
	 */
	public Answer findType(
			String typeName,
			String packageName,
			boolean partialMatch,
			int acceptFlags,
			boolean considerSecondaryTypes,
			boolean waitForIndexes,
			boolean checkRestrictions,
			IProgressMonitor monitor) {
		if (packageName == null || packageName.length() == 0) {
			packageName= IPackageFragment.DEFAULT_PACKAGE_NAME;
		} else if (typeName.length() > 0 && ScannerHelper.isLowerCase(typeName.charAt(0))) {
			// see if this is a known package and not a type
			if (findPackageFragments(packageName + "." + typeName, false) != null) return null; //$NON-NLS-1$
		}

		// Look for concerned package fragments
		JavaElementRequestor elementRequestor = new JavaElementRequestor();
		seekPackageFragments(packageName, false, elementRequestor);
		IPackageFragment[] packages= elementRequestor.getPackageFragments();

		// Try to find type in package fragments list
		IType type = null;
		int length= packages.length;
		HashSet projects = null;
		IJavaScriptProject javaProject = null;
		Answer suggestedAnswer = null;
		for (int i= 0; i < length; i++) {
			type = findType(typeName, packages[i], partialMatch, acceptFlags);
			if (type != null) {
				AccessRestriction accessRestriction = null;
				if (checkRestrictions) {
					accessRestriction = getViolatedRestriction(typeName, packageName, type, accessRestriction);
				}
				Answer answer = new Answer(type, accessRestriction);
				if (!answer.ignoreIfBetter()) {
					if (answer.isBetter(suggestedAnswer))
						return answer;
				} else if (answer.isBetter(suggestedAnswer))
					// remember suggestion and keep looking
					suggestedAnswer = answer;
			}
			else if (suggestedAnswer == null && considerSecondaryTypes) {
				if (javaProject == null) {
					javaProject = packages[i].getJavaScriptProject();
				} else if (projects == null)  {
					if (!javaProject.equals(packages[i].getJavaScriptProject())) {
						projects = new HashSet(3);
						projects.add(javaProject);
						projects.add(packages[i].getJavaScriptProject());
					}
				} else {
					projects.add(packages[i].getJavaScriptProject());
				}
			}
		}
		if (suggestedAnswer != null)
			// no better answer was found
			return suggestedAnswer;

		// If type was not found, try to find it as secondary in source folders
		if (considerSecondaryTypes && javaProject != null) {
			if (projects == null) {
				type = findSecondaryType(packageName, typeName, javaProject, waitForIndexes, monitor);
			} else {
				Iterator allProjects = projects.iterator();
				while (type == null && allProjects.hasNext()) {
					type = findSecondaryType(packageName, typeName, (IJavaScriptProject) allProjects.next(), waitForIndexes, monitor);
				}
			}
		}
		return type == null ? null : new Answer(type, null);
	}

	public Answer findBinding(
			String bindingName,
			String packageName,
			int bindingType,
			boolean partialMatch,
			int acceptFlags,
			boolean considerSecondaryTypes,
			boolean waitForIndexes,
			boolean checkRestrictions,
			IProgressMonitor monitor) {
		if (packageName == null || packageName.length() == 0) {
			packageName= IPackageFragment.DEFAULT_PACKAGE_NAME;
		} else if (bindingName.length() > 0 && ScannerHelper.isLowerCase(bindingName.charAt(0))) {
			// see if this is a known package and not a type
			if (findPackageFragments(packageName + "." + bindingName, false) != null) return null; //$NON-NLS-1$
		}

		if (VERBOSE)
			System.out.println("find binding: "+bindingName); //$NON-NLS-1$
		// Look for concerned package fragments
		JavaElementRequestor elementRequestor = new JavaElementRequestor();
		seekPackageFragments(packageName, false, elementRequestor);
		IPackageFragment[] packages= elementRequestor.getPackageFragments();

		// Try to find type in package fragments list
//		IType type = null;
		Object element = null;
		int length= packages.length;
		HashSet projects = null;
		IJavaScriptProject javaProject = null;
		Answer suggestedAnswer = null;
		for (int i= 0; i < length; i++) {
			element = findBinding(bindingName, bindingType,packages[i], partialMatch, acceptFlags);
			if (element != null) {
				AccessRestriction accessRestriction = null;
				if (checkRestrictions) {
					accessRestriction = getViolatedRestriction(bindingName, packageName, element, accessRestriction);
				}
				char[] path = null;

				if(element instanceof SourceTypeBinding) {
					path = ((SourceTypeBinding)element).getFileName();
				}else if ( element instanceof ReferenceBinding) {
					path = ((ReferenceBinding)element).getFileName();
				}else if(element instanceof SourceType){
					path = ((SourceType)element).getPath().toString().toCharArray();
				}
				if(path!=null && !getRestrictedAccessRequestor().acceptBinding(bindingType, acceptFlags, packageName.toCharArray(), bindingName.toCharArray(), new String(path), accessRestriction)){
					element = null;
					continue;
				}
//
				Answer answer = new Answer(element, accessRestriction);
				if (!answer.ignoreIfBetter()) {
					if (answer.isBetter(suggestedAnswer))
						return answer;
				} else if (answer.isBetter(suggestedAnswer))
					// remember suggestion and keep looking
					suggestedAnswer = answer;
			}
			else if (suggestedAnswer == null && considerSecondaryTypes) {
				if (javaProject == null) {
					javaProject = packages[i].getJavaScriptProject();
				} else if (projects == null)  {
					if (!javaProject.equals(packages[i].getJavaScriptProject())) {
						projects = new HashSet(3);
						projects.add(javaProject);
						projects.add(packages[i].getJavaScriptProject());
					}
				} else {
					projects.add(packages[i].getJavaScriptProject());
				}
			}
		}
		if (suggestedAnswer != null)
			// no better answer was found
			return suggestedAnswer;

//		// If type was not found, try to find it as secondary in source folders
//		if (considerSecondaryTypes && javaProject != null) {
//			if (projects == null) {
//				type = findSecondaryType(packageName, bindingName, javaProject, waitForIndexes, monitor);
//			} else {
//				Iterator allProjects = projects.iterator();
//				while (type == null && allProjects.hasNext()) {
//					type = findSecondaryType(packageName, typeName, (IJavaScriptProject) allProjects.next(), waitForIndexes, monitor);
//				}
//			}
//		}
		return element == null ? null : new Answer(element, null);
	}

	private AccessRestriction getViolatedRestriction(String typeName, String packageName, IType type, AccessRestriction accessRestriction) {
		PackageFragmentRoot root = (PackageFragmentRoot) type.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
		ClasspathEntry entry = (ClasspathEntry) this.rootToResolvedEntries.get(root);
		if (entry != null) { // reverse map always contains resolved CP entry
			AccessRuleSet accessRuleSet = entry.getAccessRuleSet();
			if (accessRuleSet != null) {
				// TODO (philippe) improve char[] <-> String conversions to avoid performing them on the fly
				char[][] packageChars = CharOperation.splitOn('.', packageName.toCharArray());
				char[] typeChars = typeName.toCharArray();
				accessRestriction = accessRuleSet.getViolatedRestriction(CharOperation.concatWith(packageChars, typeChars, '/'));
			}
		}
		return accessRestriction;
	}

	private AccessRestriction getViolatedRestriction(String typeName, String packageName, Object element, AccessRestriction accessRestriction) {
//TODO: implement
//		System.out.println("implement NameLookup.getViolatedRestriction");
//		PackageFragmentRoot root = (PackageFragmentRoot) type.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
//		ClasspathEntry entry = (ClasspathEntry) this.rootToResolvedEntries.get(root);
//		if (entry != null) { // reverse map always contains resolved CP entry
//			AccessRuleSet accessRuleSet = entry.getAccessRuleSet();
//			if (accessRuleSet != null) {
//				// TODO (philippe) improve char[] <-> String conversions to avoid performing them on the fly
//				char[][] packageChars = CharOperation.splitOn('.', packageName.toCharArray());
//				char[] typeChars = typeName.toCharArray();
//				accessRestriction = accessRuleSet.getViolatedRestriction(CharOperation.concatWith(packageChars, typeChars, '/'));
//			}
//		}
		return accessRestriction;
	}

	/**
	 * Returns the first type in the given package whose name
	 * matches the given (unqualified) name, or <code>null</code> if none
	 * exist. Specifying a <code>null</code> package will result in no matches.
	 * The domain of the search is bounded by the Java project from which
	 * this name lookup was obtained.
	 *
	 * @param name the name of the type to find
	 * @param pkg the package to search
	 * @param partialMatch partial name matches qualify when <code>true</code>,
	 *	only exact name matches qualify when <code>false</code>
	 * @param acceptFlags a bit mask describing if classes, interfaces or both classes and interfaces
	 * 	are desired results. If no flags are specified, all types are returned.
	 * @param considerSecondaryTypes flag to know whether secondary types has to be considered
	 * 	during the search
	 *
	 * @see #ACCEPT_CLASSES
	 * @see #ACCEPT_INTERFACES
	 * @see #ACCEPT_ENUMS
	 * @see #ACCEPT_ANNOTATIONS
	 */
	public IType findType(String name, IPackageFragment pkg, boolean partialMatch, int acceptFlags, boolean considerSecondaryTypes) {
		IType type = findType(name, pkg, partialMatch, acceptFlags);
		if (type == null && considerSecondaryTypes) {
			type = findSecondaryType(pkg.getElementName(), name, pkg.getJavaScriptProject(), false, null);
		}
		return type;
	}

	/**
	 * Returns the first type in the given package whose name
	 * matches the given (unqualified) name, or <code>null</code> if none
	 * exist. Specifying a <code>null</code> package will result in no matches.
	 * The domain of the search is bounded by the Java project from which
	 * this name lookup was obtained.
	 * <br>
	 *	Note that this method does not find secondary types.
	 * <br>
	 * @param name the name of the type to find
	 * @param pkg the package to search
	 * @param partialMatch partial name matches qualify when <code>true</code>,
	 *	only exact name matches qualify when <code>false</code>
	 * @param acceptFlags a bit mask describing if classes, interfaces or both classes and interfaces
	 * 	are desired results. If no flags are specified, all types are returned.
	 *
	 * @see #ACCEPT_CLASSES
	 * @see #ACCEPT_INTERFACES
	 * @see #ACCEPT_ENUMS
	 * @see #ACCEPT_ANNOTATIONS
	 */
	public IType findType(String name, IPackageFragment pkg, boolean partialMatch, int acceptFlags) {
		if (pkg == null) return null;

		// Return first found (ignore duplicates).
		SingleTypeRequestor typeRequestor = new SingleTypeRequestor();
		seekTypes(name, pkg, partialMatch, acceptFlags, typeRequestor);
		return typeRequestor.getType();
	}

	public IJavaScriptElement findBinding(String name, int type, IPackageFragment pkg, boolean partialMatch, int acceptFlags) {
		if (pkg == null) return null;

		// Return first found (ignore duplicates).
		JavaElementRequestor requestor = new JavaElementRequestor();
		seekBindings(name,type, pkg, partialMatch, acceptFlags, requestor);
		  IField[] fields = requestor.getFields();
		  IFunction[] methods = requestor.getMethods();
		  IType[] types = requestor.getTypes();
		switch (type)
		{
		  case Binding.FIELD | Binding.METHOD:
			  if (methods.length>0)
				  return methods[0];
		  case Binding.FIELD:
			  return (fields.length>0)?fields[0]:null;

		  case Binding.METHOD:
			  return (methods.length>0)?methods[0]:null;
		  case Binding.TYPE:
		  case Binding.TYPE|Binding.PACKAGE:
			  return (types.length>0)?types[0]:null;

		  default:
		  {
				if ((Binding.TYPE & type)!=0)
				{
					  if (types.length>0)
						  return types[0];
				}
				if ((Binding.METHOD & type)!=0)
				{
					  if (methods.length>0)
						  return methods[0];
				}
				if ((Binding.VARIABLE & type)!=0)
				{
					  if (fields.length>0)
						  return fields[0];
				}
		  }

		}
		return null;
	}

	/**
	 * Returns the type specified by the qualified name, or <code>null</code>
	 * if none exist. The domain of
	 * the search is bounded by the Java project from which this name lookup was obtained.
	 *
	 * @param name the name of the type to find
	 * @param partialMatch partial name matches qualify when <code>true</code>,
	 *	only exact name matches qualify when <code>false</code>
	 * @param acceptFlags a bit mask describing if classes, interfaces or both classes and interfaces
	 * 	are desired results. If no flags are specified, all types are returned.
	 *
	 * @see #ACCEPT_CLASSES
	 * @see #ACCEPT_INTERFACES
	 * @see #ACCEPT_ENUMS
	 * @see #ACCEPT_ANNOTATIONS
	 */
	public IType findType(String name, boolean partialMatch, int acceptFlags) {
		NameLookup.Answer answer = findType(name, partialMatch, acceptFlags, false/*don't check restrictions*/);
		return answer == null ? null : answer.type;
	}

	public Answer findType(String name, boolean partialMatch, int acceptFlags, boolean checkRestrictions) {
		return findType(name, partialMatch, acceptFlags, true/*consider secondary types*/, true/*wait for indexes*/, checkRestrictions, null);
	}
	public Answer findType(String name, boolean partialMatch, int acceptFlags, boolean considerSecondaryTypes, boolean waitForIndexes, boolean checkRestrictions, IProgressMonitor monitor) {
//		int index= name.lastIndexOf('.');
		String className= null, packageName= null;
//		if (index == -1) {
			packageName= IPackageFragment.DEFAULT_PACKAGE_NAME;
			className= name;
//		} else {
//			packageName= name.substring(0, index);
//			className= name.substring(index + 1);
//		}
		if (USE_BINDING_SEARCH && searchFiles)
			return findBindingSearch(className, packageName, Binding.TYPE, partialMatch, acceptFlags, considerSecondaryTypes, waitForIndexes, checkRestrictions, monitor,true,null);
		return findType(className, packageName, partialMatch, acceptFlags, considerSecondaryTypes, waitForIndexes, checkRestrictions, monitor);
	}

	private IType getMemberType(IType type, String name, int dot) {
		//while (dot != -1) {
			//int start = dot+1;
		//	dot = name.indexOf('.', start);
		//	String typeName = name.substring(start, dot == -1 ? name.length() : dot);
		//	type = type.getType(name);
		//}
		return type;
	}

	public boolean isPackage(String[] pkgName) {
		return this.packageFragments.get(pkgName) != null;
	}

	/**
	 * Returns true if the given element's name matches the
	 * specified <code>searchName</code>, otherwise false.
	 *
	 * <p>The <code>partialMatch</code> argument indicates partial matches
	 * should be considered.
	 * NOTE: in partialMatch mode, the case will be ignored, and the searchName must already have
	 *          been lowercased.
	 */
	protected boolean nameMatches(String searchName, IJavaScriptElement element, boolean partialMatch) {
		if (partialMatch) {
			// partial matches are used in completion mode, thus case insensitive mode
			return element.getElementName().toLowerCase().startsWith(searchName);
		} else {
			return element.getElementName().equals(searchName);
		}
	}

	/**
	 * Returns true if the given cu's name matches the
	 * specified <code>searchName</code>, otherwise false.
	 *
	 * <p>The <code>partialMatch</code> argument indicates partial matches
	 * should be considered.
	 * NOTE: in partialMatch mode, the case will be ignored, and the searchName must already have
	 *          been lowercased.
	 */
	protected boolean nameMatches(String searchName, IJavaScriptUnit cu, boolean partialMatch) {
		if (partialMatch) {
			// partial matches are used in completion mode, thus case insensitive mode
			return cu.getElementName().toLowerCase().startsWith(searchName);
		} else {
			return Util.equalsIgnoreJavaLikeExtension(cu.getElementName(), searchName);
		}
	}

	/**
	 * Notifies the given requestor of all package fragments with the
	 * given name. Checks the requestor at regular intervals to see if the
	 * requestor has canceled. The domain of
	 * the search is bounded by the <code>IJavaScriptProject</code>
	 * this <code>NameLookup</code> was obtained from.
	 *
	 * @param partialMatch partial name matches qualify when <code>true</code>;
	 *	only exact name matches qualify when <code>false</code>
	 */
	public void seekPackageFragments(String name, boolean partialMatch, IJavaElementRequestor requestor) {
/*		if (VERBOSE) {
			Util.verbose(" SEEKING PACKAGE FRAGMENTS");  //$NON-NLS-1$
			Util.verbose(" -> name: " + name);  //$NON-NLS-1$
			Util.verbose(" -> partial match:" + partialMatch);  //$NON-NLS-1$
		}
		*/
		for (int i = 0; i < this.packageFragmentRoots.length; i++) {


			IJavaScriptElement[] children;
			try {
				if (this.searchFiles || packageFragmentRoots[i].isLibrary())
				{
					children = packageFragmentRoots[i].getChildren();
					for (int j = 0; j < children.length; j++) {
						requestor.acceptPackageFragment((IPackageFragment)children[j]);
					}
				}
			} catch (JavaScriptModelException e) {}

		}



//		if (partialMatch) {
//			String[] splittedName = splitPackageName(name);
//			Object[][] keys = this.packageFragments.keyTable;
//			for (int i = 0, length = keys.length; i < length; i++) {
//				if (requestor.isCanceled())
//					return;
//				String[] pkgName = (String[]) keys[i];
//				if (pkgName != null && Util.startsWithIgnoreCase(pkgName, splittedName)) {
//					Object value = this.packageFragments.valueTable[i];
//					if (value instanceof PackageFragmentRoot) {
//						PackageFragmentRoot root = (PackageFragmentRoot) value;
//						requestor.acceptPackageFragment(root.getPackageFragment(pkgName));
//					} else {
//						IPackageFragmentRoot[] roots = (IPackageFragmentRoot[]) value;
//						for (int j = 0, length2 = roots.length; j < length2; j++) {
//							if (requestor.isCanceled())
//								return;
//							PackageFragmentRoot root = (PackageFragmentRoot) roots[j];
//							requestor.acceptPackageFragment(root.getPackageFragment(pkgName));
//						}
//					}
//				}
//			}
//		} else {
//			String[] splittedName = splitPackageName(name);
//			Object value = this.packageFragments.get(splittedName);
//			if (value instanceof PackageFragmentRoot) {
//				requestor.acceptPackageFragment(((PackageFragmentRoot) value).getPackageFragment(splittedName));
//			} else {
//				IPackageFragmentRoot[] roots = (IPackageFragmentRoot[]) value;
//				if (roots != null) {
//					for (int i = 0, length = roots.length; i < length; i++) {
//						if (requestor.isCanceled())
//							return;
//						PackageFragmentRoot root = (PackageFragmentRoot) roots[i];
//						requestor.acceptPackageFragment(root.getPackageFragment(splittedName));
//					}
//				}
//			}
//		}
//		if (name==null || name.equals(IPackageFragment.DEFAULT_PACKAGE_NAME))
//		{
//			for (int i = 0; i < this.packageFragmentRoots.length; i++) {
//				if (packageFragmentRoots[i] instanceof LibraryFragmentRoot)
//				{
//					IJavaScriptElement[] children;
//					try {
//						children = packageFragmentRoots[i].getChildren();
//						for (int j = 0; j < children.length; j++) {
//							requestor.acceptPackageFragment((IPackageFragment)children[j]);
//						}
//					} catch (JavaScriptModelException e) {
//					}
//				}
//			}
//		}
	}

//	private String [] splitPackageName(String name)
//	{
//		String[] strings;
//		int index=Util.indexOfJavaLikeExtension(name);
//		if (index>=0)
//		{
//			String extension=name.substring(index+1);
//			name=name.substring(0,index);
//			strings= Util.splitOn('.', name, 0, name.length());
//			strings[strings.length-1]=strings[strings.length-1]+extension;
//		}
//		else
//		strings  = Util.splitOn('.', name, 0, name.length());
//
//		return strings;
//	}
	/**
	 * Notifies the given requestor of all types (classes and interfaces) in the
	 * given package fragment with the given (unqualified) name.
	 * Checks the requestor at regular intervals to see if the requestor
	 * has canceled. If the given package fragment is <code>null</code>, all types in the
	 * project whose simple name matches the given name are found.
	 *
	 * @param name The name to search
	 * @param pkg The corresponding package fragment
	 * @param partialMatch partial name matches qualify when <code>true</code>;
	 *	only exact name matches qualify when <code>false</code>
	 * @param acceptFlags a bit mask describing if classes, interfaces or both classes and interfaces
	 * 	are desired results. If no flags are specified, all types are returned.
	 * @param requestor The requestor that collects the result
	 *
	 * @see #ACCEPT_CLASSES
	 * @see #ACCEPT_INTERFACES
	 * @see #ACCEPT_ENUMS
	 * @see #ACCEPT_ANNOTATIONS
	 */
	public void seekTypes(String name, IPackageFragment pkg, boolean partialMatch, int acceptFlags, IJavaElementRequestor requestor) {
/*		if (VERBOSE) {
			Util.verbose(" SEEKING TYPES");  //$NON-NLS-1$
			Util.verbose(" -> name: " + name);  //$NON-NLS-1$
			Util.verbose(" -> pkg: " + ((JavaElement) pkg).toStringWithAncestors());  //$NON-NLS-1$
			Util.verbose(" -> partial match:" + partialMatch);  //$NON-NLS-1$
		}
*/
		String matchName= partialMatch ? name.toLowerCase() : name;
		if (pkg == null) {
			findAllTypes(matchName, partialMatch, acceptFlags, requestor);
			return;
		}
		IPackageFragmentRoot root= (IPackageFragmentRoot) pkg.getParent();
		try {

			// look in working copies first
			int firstDot = -1;
			String topLevelTypeName = matchName;
			int packageFlavor= root.getKind();
//			if (this.typesInWorkingCopies != null || packageFlavor == IPackageFragmentRoot.K_SOURCE) {
//				firstDot = matchName.indexOf('.');
//				if (!partialMatch)
//					topLevelTypeName = firstDot == -1 ? matchName : matchName.substring(0, firstDot);
//			}
			if (this.typesInWorkingCopies != null) {
				if (seekTypesInWorkingCopies(matchName, pkg, firstDot, partialMatch, topLevelTypeName, acceptFlags, requestor))
					return;
			}

			// look in model
			switch (packageFlavor) {
				case IPackageFragmentRoot.K_BINARY :
					seekBindingsInBinaryPackage(matchName,Binding.TYPE, pkg, partialMatch, acceptFlags, requestor);
					break;
				case IPackageFragmentRoot.K_SOURCE :
					seekTypesInSourcePackage(matchName, pkg, firstDot, partialMatch, topLevelTypeName, acceptFlags, requestor);
					break;
				default :
					return;
			}
		} catch (JavaScriptModelException e) {
			return;
		}
	}


	public void seekBindings(String name, int bindingType, IPackageFragment pkg, boolean partialMatch, int acceptFlags, IJavaElementRequestor requestor) {
		/*		if (VERBOSE) {
					Util.verbose(" SEEKING TYPES");  //$NON-NLS-1$
					Util.verbose(" -> name: " + name);  //$NON-NLS-1$
					Util.verbose(" -> pkg: " + ((JavaElement) pkg).toStringWithAncestors());  //$NON-NLS-1$
					Util.verbose(" -> partial match:" + partialMatch);  //$NON-NLS-1$
				}
		*/
				String matchName= partialMatch ? name.toLowerCase() : name;
				if (pkg == null) {
					findAllBindings(matchName, bindingType, partialMatch, acceptFlags, requestor);
					return;
				}
				IPackageFragmentRoot root= (IPackageFragmentRoot) pkg.getParent();
				try {

					// look in working copies first
					int firstDot = -1;
					String topLevelTypeName = null;
					int packageFlavor= root.getKind();
					if (this.typesInWorkingCopies != null || packageFlavor == IPackageFragmentRoot.K_SOURCE) {
						firstDot = matchName.indexOf('.');
						if (!partialMatch)
							topLevelTypeName = firstDot == -1 ? matchName : matchName.substring(0, firstDot);
					}
					if (this.bindingsInWorkingCopies != null) {
						if (seekBindingsInWorkingCopies(matchName,bindingType, firstDot, partialMatch, topLevelTypeName, acceptFlags, requestor))
							return;
					}

					// look in model
					switch (packageFlavor) {
						case IPackageFragmentRoot.K_BINARY :
							seekBindingsInBinaryPackage(matchName,  bindingType,pkg, partialMatch, acceptFlags, requestor);
							break;
						case IPackageFragmentRoot.K_SOURCE :
							seekBindingsInSourcePackage(matchName, bindingType,pkg, firstDot, partialMatch, topLevelTypeName, acceptFlags, requestor);
							break;
						default :
							return;
					}
				} catch (JavaScriptModelException e) {
					return;
				}
			}


	/**
	 * Performs type search in a binary package.
	 */
//	protected void seekTypesInBinaryPackage(String name, IPackageFragment pkg, boolean partialMatch, int acceptFlags, IJavaElementRequestor requestor) {
//		long start = -1;
//		if (VERBOSE)
//			start = System.currentTimeMillis();
//		try {
//			if (!partialMatch) {
//				// exact match
//				if (requestor.isCanceled()) return;
//				ClassFile classFile =  new ClassFile((PackageFragment) pkg, name);
//				if (classFile.existsUsingJarTypeCache()) {
//					IType type = classFile.getType();
//					if (acceptType(type, acceptFlags, false/*not a source type*/)) {
//						requestor.acceptType(type);
//					}
//				}
//			} else {
//				IJavaScriptElement[] classFiles= null;
//				try {
//					classFiles= pkg.getChildren();
//				} catch (JavaScriptModelException npe) {
//					return; // the package is not present
//				}
//				int length= classFiles.length;
//				String unqualifiedName = name;
//				int index = name.lastIndexOf('$');
//				if (index != -1) {
//					//the type name of the inner type
//					unqualifiedName = Util.localTypeName(name, index, name.length());
//					// unqualifiedName is empty if the name ends with a '$' sign.
//					// See http://dev.eclipse.org/bugs/show_bug.cgi?id=14642
//				}
//				int matchLength = name.length();
//				for (int i = 0; i < length; i++) {
//					if (requestor.isCanceled())
//						return;
//					IJavaScriptElement classFile= classFiles[i];
//					// MatchName will never have the extension ".class" and the elementName always will.
//					String elementName = classFile.getElementName();
//					if (elementName.regionMatches(true /*ignore case*/, 0, name, 0, matchLength)) {
//						IType type = ((ClassFile) classFile).getType();
//						String typeName = type.getElementName();
//						if (typeName.length() > 0 && !Character.isDigit(typeName.charAt(0))) { //not an anonymous type
//							if (nameMatches(unqualifiedName, type, true/*partial match*/) && acceptType(type, acceptFlags, false/*not a source type*/))
//								requestor.acceptType(type);
//						}
//					}
//				}
//			}
//		} finally {
//			if (VERBOSE)
//				this.timeSpentInSeekTypesInBinaryPackage += System.currentTimeMillis()-start;
//		}
//	}
//
	protected void seekBindingsInBinaryPackage(String name, int bindingType,IPackageFragment pkg, boolean partialMatch, int acceptFlags, IJavaElementRequestor requestor) {
		long start = -1;
		if (VERBOSE)
			start = System.currentTimeMillis();
		try {
			this.acceptedCUs.clear();
			IJavaScriptElement[] classFiles= null;
			try {
				classFiles= pkg.getChildren();
			} catch (JavaScriptModelException npe) {
				return; // the package is not present
			}
			int length= classFiles.length;
			if (!partialMatch) {
				for (int i = 0; i < length; i++) {
					ClassFile classFile=(ClassFile)classFiles[i];
					if (this.acceptedCUs.contains(classFile))
						continue;
					switch (bindingType) {
					case Binding.TYPE:
					case Binding.TYPE | Binding.PACKAGE:
						IType type = classFile.getType(name);
						if (acceptType(type, acceptFlags, false/*not a source type*/)) {
							acceptedCUs.add(classFile);
							requestor.acceptType(type);
						}

						break;
					case Binding.VARIABLE:
//						String cuName = cu.getElementName();
//						int lastDot = cuName.lastIndexOf('.');
//						if (lastDot != topLevelTypeName.length() || !topLevelTypeName.regionMatches(0, cuName, 0, lastDot))
//							continue;
						IField field = classFile.getField(name);
						if (field.exists()) {
							acceptedCUs.add(classFile);
							requestor.acceptField(field);
						}

						break;

					case Binding.METHOD:
//						String cuName = cu.getElementName();
//						int lastDot = cuName.lastIndexOf('.');
//						if (lastDot != topLevelTypeName.length() || !topLevelTypeName.regionMatches(0, cuName, 0, lastDot))
//							continue;
						IFunction method = classFile.getFunction(name, null);
						if (method.exists()) {
							acceptedCUs.add(classFile);
							requestor.acceptMethod(method);
						}
						break;
					case Binding.METHOD | Binding.VARIABLE:
						 method = classFile.getFunction(name, null);
						if (method!=null
							&& method.exists()) {
								acceptedCUs.add(classFile);
							requestor.acceptMethod(method);
						} else
						{
						   field = classFile.getField(name);
						   if (field!=null && field.exists())
						   {
								acceptedCUs.add(classFile);
								  requestor.acceptField(field);

						   }
						}
						break;
					  default:
					  {
							if ((Binding.TYPE & bindingType)!=0)
							{
								IType thisType = classFile.getType(name);
								if (acceptType(thisType, acceptFlags, false/*not a source type*/)) {
									acceptedCUs.add(classFile);
									requestor.acceptType(thisType);
								}
							}
							if ((Binding.METHOD & bindingType)!=0)
							{
								 method = classFile.getFunction(name, null);
									if (method!=null && method.exists()) {
										acceptedCUs.add(classFile);
										requestor.acceptMethod(method);
									}
							}
							if ((Binding.VARIABLE & bindingType)!=0)
							{
							   field = classFile.getField(name);
								if (field!=null && field.exists()) {
									acceptedCUs.add(classFile);
									requestor.acceptField(field);
								}
							}
					  }
					}
				}
			} else {
				String unqualifiedName = name;
				int matchLength = name.length();
				for (int i = 0; i < length; i++) {
					if (requestor.isCanceled())
						return;
					IJavaScriptElement classFile= classFiles[i];
					// MatchName will never have the extension ".class" and the elementName always will.
					String elementName = classFile.getElementName();
					if (elementName.regionMatches(true /*ignore case*/, 0, name, 0, matchLength)) {
						IType type = ((ClassFile) classFile).getType();
						String typeName = type.getElementName();
						if (typeName.length() > 0 && !Character.isDigit(typeName.charAt(0))) { //not an anonymous type
							if (nameMatches(unqualifiedName, type, true/*partial match*/) && acceptType(type, acceptFlags, false/*not a source type*/))
								requestor.acceptType(type);
						}
					}
				}
			}
		} finally {
			if (VERBOSE)
				this.timeSpentInSeekTypesInBinaryPackage += System.currentTimeMillis()-start;
		}
	}

	/**
	 * Performs type search in a source package.
	 */
	protected void seekTypesInSourcePackage(
			String name,
			IPackageFragment pkg,
			int firstDot,
			boolean partialMatch,
			String topLevelTypeName,
			int acceptFlags,
			IJavaElementRequestor requestor) {

		long start = -1;
		if (VERBOSE)
			start = System.currentTimeMillis();
		try {
			if (!partialMatch) {
				try {
					IJavaScriptElement[] compilationUnits = pkg.getChildren();
					for (int i = 0, length = compilationUnits.length; i < length; i++) {
						if (requestor.isCanceled())
							return;
						IJavaScriptElement cu = compilationUnits[i];
//						String cuName = cu.getElementName();
						//int lastDot = cuName.lastIndexOf('.');
						//if (lastDot != topLevelTypeName.length() || !topLevelTypeName.regionMatches(0, cuName, 0, lastDot)) \
						//if(topLevelTypeName!=null && !topLevelTypeName.equals(cuName))
						//	continue;
						IType type = ((IJavaScriptUnit) cu).getType(name);
						//type = getMemberType(type, name, firstDot);
						if (acceptType(type, acceptFlags, true/*a source type*/)) { // accept type checks for existence
							requestor.acceptType(type);
							break;  // since an exact match was requested, no other matching type can exist
						}
					}
				} catch (JavaScriptModelException e) {
					// package doesn't exist -> ignore
				}
			} else {
				try {
					String cuPrefix = firstDot == -1 ? name : name.substring(0, firstDot);
					IJavaScriptElement[] compilationUnits = pkg.getChildren();
					for (int i = 0, length = compilationUnits.length; i < length; i++) {
						if (requestor.isCanceled())
							return;
						IJavaScriptElement cu = compilationUnits[i];
						if (!cu.getElementName().toLowerCase().startsWith(cuPrefix))
							continue;
						try {
							IType[] types = ((IJavaScriptUnit) cu).getTypes();
							for (int j = 0, typeLength = types.length; j < typeLength; j++)
								seekTypesInTopLevelType(name, firstDot, types[j], requestor, acceptFlags);
						} catch (JavaScriptModelException e) {
							// cu doesn't exist -> ignore
						}
					}
				} catch (JavaScriptModelException e) {
					// package doesn't exist -> ignore
				}
			}
		} finally {
			if (VERBOSE)
				this.timeSpentInSeekTypesInSourcePackage += System.currentTimeMillis()-start;
		}
	}

	protected void seekBindingsInSourcePackage(
			String name,
			int bindingType,
			IPackageFragment pkg,
			int firstDot,
			boolean partialMatch,
			String topLevelTypeName,
			int acceptFlags,
			IJavaElementRequestor requestor) {

		long start = -1;
		if (VERBOSE)
			start = System.currentTimeMillis();
		try {
			if (!partialMatch) {
				try {
					IJavaScriptElement[] compilationUnits = pkg.getChildren();
					for (int i = 0, length = compilationUnits.length; i < length; i++) {
						if (requestor.isCanceled())
							return;
						IJavaScriptElement cu = compilationUnits[i];
						if (cu instanceof IJavaScriptUnit && ((IJavaScriptUnit)cu).isWorkingCopy())
							continue;
						if (this.acceptedCUs.contains(cu))
							continue;

						switch (bindingType) {
						case Binding.TYPE:
//							String cuName = cu.getElementName();
//							int lastDot = cuName.lastIndexOf('.');
//							if (lastDot != topLevelTypeName.length() || !topLevelTypeName.regionMatches(0, cuName, 0, lastDot))
//								continue;
							IType type = ((IJavaScriptUnit) cu).getType(topLevelTypeName);
							type = getMemberType(type, name, firstDot);
							if (acceptType(type, acceptFlags, true/*a source type*/)) { // accept type checks for existence
								acceptedCUs.add(cu);
								requestor.acceptType(type);
								break;  // since an exact match was requested, no other matching type can exist
							}

							break;
						case Binding.VARIABLE:
//							String cuName = cu.getElementName();
//							int lastDot = cuName.lastIndexOf('.');
//							if (lastDot != topLevelTypeName.length() || !topLevelTypeName.regionMatches(0, cuName, 0, lastDot))
//								continue;
							IField field = ((IJavaScriptUnit) cu).getField(name);
							if (field.exists()) {
								acceptedCUs.add(cu);
								requestor.acceptField(field);
							}

							break;

						case Binding.METHOD:
//							String cuName = cu.getElementName();
//							int lastDot = cuName.lastIndexOf('.');
//							if (lastDot != topLevelTypeName.length() || !topLevelTypeName.regionMatches(0, cuName, 0, lastDot))
//								continue;
							IFunction method = ((IJavaScriptUnit) cu).getFunction(name, null);
							if (method.exists()) {
								acceptedCUs.add(cu);
								requestor.acceptMethod(method);
							}
							break;
						case Binding.METHOD | Binding.VARIABLE:
							 method = ((IJavaScriptUnit) cu).getFunction(name, null);
							if (method!=null)
								if  (method.exists()) {
									acceptedCUs.add(cu);
									requestor.acceptMethod(method);
								} else
							{
							   field = ((IJavaScriptUnit) cu).getField(name);
								if  (field.exists()) {
									acceptedCUs.add(cu);
									requestor.acceptField(field);
								}
							}
							break;

						default:
							break;
						}

					}
				} catch (JavaScriptModelException e) {
					// package doesn't exist -> ignore
				}
			} else {
				try {
					String cuPrefix = firstDot == -1 ? name : name.substring(0, firstDot);
					IJavaScriptElement[] compilationUnits = pkg.getChildren();
					for (int i = 0, length = compilationUnits.length; i < length; i++) {
						if (requestor.isCanceled())
							return;
						IJavaScriptElement cu = compilationUnits[i];
						if (!cu.getElementName().toLowerCase().startsWith(cuPrefix))
							continue;
						try {
							IType[] types = ((IJavaScriptUnit) cu).getTypes();
							for (int j = 0, typeLength = types.length; j < typeLength; j++)
								seekTypesInTopLevelType(name, firstDot, types[j], requestor, acceptFlags);
						} catch (JavaScriptModelException e) {
							// cu doesn't exist -> ignore
						}
					}
				} catch (JavaScriptModelException e) {
					// package doesn't exist -> ignore
				}
			}
		} finally {
			if (VERBOSE)
				this.timeSpentInSeekTypesInSourcePackage += System.currentTimeMillis()-start;
		}
	}

	/**
	 * Notifies the given requestor of all types (classes and interfaces) in the
	 * given type with the given (possibly qualified) name. Checks
	 * the requestor at regular intervals to see if the requestor
	 * has canceled.
	 */
	protected boolean seekTypesInType(String prefix, int firstDot, IType type, IJavaElementRequestor requestor, int acceptFlags) {
		IType[] types= null;
		try {
			types= type.getTypes();
		} catch (JavaScriptModelException npe) {
			return false; // the enclosing type is not present
		}
		int length= types.length;
		if (length == 0) return false;

		String memberPrefix = prefix;
		boolean isMemberTypePrefix = false;
		if (firstDot != -1) {
			memberPrefix= prefix.substring(0, firstDot);
			isMemberTypePrefix = true;
		}
		for (int i= 0; i < length; i++) {
			if (requestor.isCanceled())
				return false;
			IType memberType= types[i];
			if (memberType.getElementName().toLowerCase().startsWith(memberPrefix))
				if (isMemberTypePrefix) {
					String subPrefix = prefix.substring(firstDot + 1, prefix.length());
					return seekTypesInType(subPrefix, subPrefix.indexOf('.'), memberType, requestor, acceptFlags);
				} else {
					if (acceptType(memberType, acceptFlags, true/*a source type*/)) {
						requestor.acceptMemberType(memberType);
						return true;
					}
				}
		}
		return false;
	}

	protected boolean seekTypesInTopLevelType(String prefix, int firstDot, IType topLevelType, IJavaElementRequestor requestor, int acceptFlags) {
		if (!topLevelType.getElementName().toLowerCase().startsWith(prefix))
			return false;
		if (firstDot == -1) {
			if (acceptType(topLevelType, acceptFlags, true/*a source type*/)) {
				requestor.acceptType(topLevelType);
				return true;
			}
		} else {
			return seekTypesInType(prefix, firstDot, topLevelType, requestor, acceptFlags);
		}
		return false;
	}

	/*
	 * Seeks the type with the given name in the map of types with precedence (coming from working copies)
	 * Return whether a type has been found.
	 */
	protected boolean seekTypesInWorkingCopies(
			String name,
			IPackageFragment pkg,
			int firstDot,
			boolean partialMatch,
			String topLevelTypeName,
			int acceptFlags,
			IJavaElementRequestor requestor) {

		if (!partialMatch) {
			HashMap typeMap = (HashMap) (this.typesInWorkingCopies == null ? null : this.typesInWorkingCopies.get(pkg));
			if (typeMap != null) {
				Object object = typeMap.get(topLevelTypeName);
				if (object instanceof IType) {
					IType type = getMemberType((IType) object, name, firstDot);
					if (acceptType(type, acceptFlags, true/*a source type*/)) {
						requestor.acceptType(type);
						return true; // don't continue with compilation unit
					}
				} else if (object instanceof IType[]) {
					if (object == NO_TYPES) return true; // all types where deleted -> type is hidden
					IType[] topLevelTypes = (IType[]) object;
					for (int i = 0, length = topLevelTypes.length; i < length; i++) {
						if (requestor.isCanceled())
							return false;
						IType type = getMemberType(topLevelTypes[i], name, firstDot);
						if (acceptType(type, acceptFlags, true/*a source type*/)) {
							requestor.acceptType(type);
							return true; // return the first one
						}
					}
				}
			}
		} else {
			HashMap typeMap = (HashMap) (this.typesInWorkingCopies == null ? null : this.typesInWorkingCopies.get(pkg));
			if (typeMap != null) {
				Iterator iterator = typeMap.values().iterator();
				while (iterator.hasNext()) {
					if (requestor.isCanceled())
						return false;
					Object object = iterator.next();
					if (object instanceof IType) {
						seekTypesInTopLevelType(name, firstDot, (IType) object, requestor, acceptFlags);
					} else if (object instanceof IType[]) {
						IType[] topLevelTypes = (IType[]) object;
						for (int i = 0, length = topLevelTypes.length; i < length; i++)
							seekTypesInTopLevelType(name, firstDot, topLevelTypes[i], requestor, acceptFlags);
					}
				}
			}
		}
		return false;
	}


	private boolean checkBindingAccept(String topLevelTypeName,HashMap []bindingsMap,int bindingType,IJavaElementRequestor requestor)
	{
		Object object = bindingsMap[bindingType].get(topLevelTypeName);
		if (object instanceof IJavaScriptElement) {
			if (doAcceptBinding((IJavaScriptElement)object, bindingType , true/*a source type*/,requestor)) {
				return true; // don't continue with compilation unit
			}
		} else if (object instanceof IJavaScriptElement[]) {
			if (object == NO_BINDINGS) return true; // all types where deleted -> type is hidden
			IJavaScriptElement[] topLevelElements = (IJavaScriptElement[]) object;
			boolean isAnyBindingAccepted = false;
			for (int i = 0, length = topLevelElements.length; i < length; i++) {
				if (requestor.isCanceled())
					return false;
				if (doAcceptBinding(topLevelElements[i], bindingType, true/*a source type*/,requestor)) {
					isAnyBindingAccepted = true;
				}
			}
			return isAnyBindingAccepted;
		}
		return false;

	}
	protected boolean seekBindingsInWorkingCopies(
			String name,
			int bindingType,
			int firstDot,
			boolean partialMatch,
			String topLevelTypeName,
			int acceptFlags,
			IJavaElementRequestor requestor) {

		bindingType=bindingType & Binding.BASIC_BINDINGS_MASK;
		if (!partialMatch) {
			HashMap []bindingsMap = (this.bindingsInWorkingCopies == null ? null : this.bindingsInWorkingCopies);
			if (bindingsMap != null) {
				if (checkBindingAccept(topLevelTypeName, bindingsMap, bindingType, requestor))
					return true;
				if ((bindingType&Binding.VARIABLE)>0 && bindingType!=Binding.VARIABLE)
					if (checkBindingAccept(topLevelTypeName, bindingsMap, Binding.VARIABLE, requestor))
						return true;
				if ((bindingType&Binding.LOCAL)>0 && bindingType!=Binding.LOCAL)
					if (checkBindingAccept(topLevelTypeName, bindingsMap, Binding.LOCAL, requestor))
						return true;
				if ((bindingType&Binding.METHOD)>0 && bindingType!=Binding.METHOD)
					if (checkBindingAccept(topLevelTypeName, bindingsMap, Binding.METHOD, requestor))
						return true;
				if ((bindingType&Binding.TYPE)>0 && bindingType!=Binding.TYPE)
					if (checkBindingAccept(topLevelTypeName, bindingsMap, Binding.TYPE, requestor))
						return true;
			}
		} else {
			HashMap[] bindingsMap = (this.bindingsInWorkingCopies == null ? null : this.bindingsInWorkingCopies);
			if (bindingsMap != null) {
				Iterator iterator = bindingsMap[bindingType].values().iterator();
				while (iterator.hasNext()) {
					if (requestor.isCanceled())
						return false;
					Object object = iterator.next();
					if (object instanceof IType) {
						seekTypesInTopLevelType(name, firstDot, (IType) object, requestor, acceptFlags);
					} else if (object instanceof IType[]) {
						IType[] topLevelTypes = (IType[]) object;
						for (int i = 0, length = topLevelTypes.length; i < length; i++)
							seekTypesInTopLevelType(name, firstDot, topLevelTypes[i], requestor, acceptFlags);
					}
				}
			}
		}
		return false;
	}

	   public static final boolean USE_BINDING_SEARCH=true;
	   private HandleFactory handleFactory;
		protected IJavaScriptSearchScope searchScope;
		public Answer findBindingSearch(
				String bindingName,
				String packageName,
				int bindingType,
				boolean partialMatch,
				int acceptFlags,
				boolean considerSecondaryTypes,
				boolean waitForIndexes,
				boolean checkRestrictions,
				IProgressMonitor progressMonitor, boolean returnMultiple, String exclude) {


			class MyRequestor implements IJavaElementRequestor
			{
				ArrayList element;
				
				public void acceptField(IField field) {
					if(element == null)
						element = new ArrayList();
					element.add(field);
				}

				public void acceptInitializer(IInitializer initializer) {
				}

				public void acceptMemberType(IType type) {
					if(element == null)
						element = new ArrayList();
					element.add(type);
				}

				public void acceptMethod(IFunction method) {
					if(element == null)
						element = new ArrayList();
					element.add(method);

				}

				public void acceptPackageFragment(
						IPackageFragment packageFragment) {
				}

				public void acceptType(IType type) {
					if(element == null)
						element = new ArrayList();
					element.add(type);
				}

				public boolean isCanceled() {
					return false;
				}

			}

			if (this.searchScope==null)
				this.searchScope = BasicSearchEngine.createJavaSearchScope(packageFragmentRoots);

			ArrayList foundAnswers=new ArrayList();
			Path excludePath= (exclude!=null)? new Path(exclude) : null;

			MyRequestor requestor=new MyRequestor();
			JavaElementRequestor elementRequestor = new JavaElementRequestor();
//			seekPackageFragments(packageName, false, elementRequestor);
//			IPackageFragment[] packages= elementRequestor.getPackageFragments();
			seekBindingsInWorkingCopies(bindingName, bindingType, -1, partialMatch,
					bindingName, acceptFlags, requestor);
			if (requestor.element != null) {
				for(int i = 0; i < requestor.element.size(); i++) {
					IOpenable openable = ((IJavaScriptElement)requestor.element.get(i)).getOpenable();
					if (excludePath!=null && ((IJavaScriptElement)requestor.element.get(i)).getPath().equals(excludePath))
						continue;
					if (!returnMultiple) {
						return new Answer(openable, null);
					} else
						foundAnswers.add(openable);
				}
				requestor.element=null;
			}
			/*
			 * if (true){ findTypes(new String(prefix), storage,
			 * NameLookup.ACCEPT_CLASSES | NameLookup.ACCEPT_INTERFACES); return; }
			 */
			try {



				IRestrictedAccessBindingRequestor bindingAcceptor = getRestrictedAccessRequestor();
				if (exclude!=null)
					exclude=exclude.replace('\\', '/');
				bindingAcceptor.setExcludePath(exclude);

				try {
					int matchRule = SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE;
					new BasicSearchEngine().searchAllBindingNames(
							CharOperation.NO_CHAR,
							bindingName.toCharArray(),
							bindingType,
							matchRule, // not case sensitive
							/*IJavaScriptSearchConstants.TYPE,*/ this.searchScope,
							bindingAcceptor, IJavaScriptSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
							false,
							progressMonitor);
					if (bindingAcceptor.getFoundPath()!=null)
					{
						
						Object[] foundPaths = bindingAcceptor.getFoundPaths().toArray();
						for (int i = 0; i < foundPaths.length; ++i) {
							String path = (String) foundPaths[i];

						IOpenable openable ; //= createOpenable(getRestrictedAccessRequestor().getFoundPath(), this.searchScope);
//						if (openable!=null)
//							return new Answer(openable, null);

						if (this.handleFactory == null)
							this.handleFactory = new HandleFactory();

						openable = this.handleFactory.createOpenable(path, this.searchScope);


						if (openable!=null)
						{
							if (!returnMultiple)
								return new Answer(openable, null);
							else
								foundAnswers.add(openable);
						}
						}
						if (foundAnswers.size()>0 && returnMultiple)
							return new Answer(foundAnswers.toArray(),null);
					}

				} catch (OperationCanceledException e) {
					return findBinding( bindingName,packageName,bindingType,partialMatch,acceptFlags,considerSecondaryTypes,waitForIndexes,
							checkRestrictions,progressMonitor);
				}
				finally
				{
					bindingAcceptor.reset();
				}
			} catch (JavaScriptModelException e) {
				return findBinding( bindingName,packageName,bindingType,partialMatch,acceptFlags,considerSecondaryTypes,waitForIndexes,
						checkRestrictions,progressMonitor);
			}
			
			if (foundAnswers.size()>0 && returnMultiple)
				return new Answer(foundAnswers.toArray(),null);
			return null;
		}
		/* creates an openable from PackageFragmentRoots in this lookup
		 * i thought i was going to use, but found another way.  This may still
		 * be faster then the current method so leaveing in case...
		 *
		 * */
		public IOpenable createOpenable(String resourcePath, IJavaScriptSearchScope scope) {
			if(packageFragmentRoots==null) return null;
			IPath resourceP = new Path(resourcePath);
			for(int i = 0;i<packageFragmentRoots.length;i++) {
				IPackageFragmentRoot root = packageFragmentRoots[i];
				IPath fragPath = root.getPath();

				String fileName = resourceP.lastSegment();
				IPath rootPostFix=null;

				if( root.isLanguageRuntime() /*&& restrictToLanguage*/) {
					int rootSegs = fragPath.segmentCount();
					int resourceSegs = resourceP.segmentCount();
					if(rootSegs>resourceSegs)
						rootPostFix =  (fragPath.removeFirstSegments(rootSegs - resourceSegs )).setDevice(null).makeAbsolute();
				}

					if(fragPath.isPrefixOf(resourceP) || (rootPostFix!=null && rootPostFix.equals(resourceP.makeAbsolute()))) {
						if(root instanceof LibraryFragmentRoot  /*&& (!restrictToLanguage  || root.isLanguageRuntime()      )*/ ) {
								IClassFile file = root.getPackageFragment(root.getPath().toString()).getClassFile(root.getPath().toString());
								return file;

						}else{
							if(resourceP.toFile().exists()){
								IClassFile file = root.getPackageFragment(resourcePath).getClassFile(resourcePath);
								return file;
							}else {
								String pkgName = resourcePath.substring(fragPath.toString().length());
								int indexName = pkgName.indexOf(fileName);
								if(indexName>-1) {
									pkgName = pkgName.substring(0,indexName-1);
								}
								IJavaScriptUnit file = root.getPackageFragment(pkgName).getJavaScriptUnit((fileName));
								return file;
							}
						}


					}

			}
			return null;


		}


		public void setScriptFile(IInferenceFile compUnit)
		{
			InferrenceProvider[] inferenceProviders = InferrenceManager.getInstance().getInferenceProviders(compUnit);
			if (inferenceProviders!=null && inferenceProviders.length>0)
			{
				for(int i = 0; i < inferenceProviders.length; i++) {
					ResolutionConfiguration resolutionConfiguration = inferenceProviders[i].getResolutionConfiguration();
					if (resolutionConfiguration!=null)
						searchFiles=resolutionConfiguration.searchAllFiles();
					if(!searchFiles)
						break;
				}
			}
		}
		
}
