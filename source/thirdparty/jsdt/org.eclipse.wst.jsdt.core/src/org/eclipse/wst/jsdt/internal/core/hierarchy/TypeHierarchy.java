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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.wst.jsdt.core.ElementChangedEvent;
import org.eclipse.wst.jsdt.core.IElementChangedListener;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptElementDelta;
import org.eclipse.wst.jsdt.core.IJavaScriptModelStatusConstants;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.ITypeHierarchyChangedListener;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.internal.core.ClassFile;
import org.eclipse.wst.jsdt.internal.core.CompilationUnit;
import org.eclipse.wst.jsdt.internal.core.JavaElement;
import org.eclipse.wst.jsdt.internal.core.JavaModelStatus;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.core.Openable;
import org.eclipse.wst.jsdt.internal.core.PackageFragment;
import org.eclipse.wst.jsdt.internal.core.Region;
import org.eclipse.wst.jsdt.internal.core.TypeVector;
import org.eclipse.wst.jsdt.internal.core.util.Messages;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * @see ITypeHierarchy
 */
public class TypeHierarchy implements ITypeHierarchy, IElementChangedListener {

	public static boolean DEBUG = false;

	static final byte VERSION = 0x0000;
	// SEPARATOR
	static final byte SEPARATOR1 = '\n';
	static final byte SEPARATOR2 = ',';
	static final byte SEPARATOR3 = '>';
	static final byte SEPARATOR4 = '\r';
	// general info
	static final byte COMPUTE_SUBTYPES = 0x0001;

	// type info
	static final byte CLASS = 0x0000;
	static final byte COMPUTED_FOR = 0x0002;
	static final byte ROOT = 0x0004;

	// cst
	static final byte[] NO_FLAGS = new byte[]{};
	static final int SIZE = 10;

	/**
	 * The Java Project in which the hierarchy is being built - this provides
	 * the context for determining a classpath and namelookup rules. Possibly
	 * null.
	 */
	protected IJavaScriptProject project;
	/**
	 * The type the hierarchy was specifically computed for, possibly null.
	 */
	protected IType focusType;

	/*
	 * The working copies that take precedence over original compilation units
	 */
	protected IJavaScriptUnit[] workingCopies;

	protected Map classToSuperclass;
	
	/**
	 * <code><{@link String},{@link TypeVector}></code>
	 */
	protected Map typeToSubtypes;
	protected Map typeFlags;
	protected TypeVector rootClasses = new TypeVector();
	public ArrayList missingTypes = new ArrayList(4);

	protected static final IType[] NO_TYPE = new IType[0];

	/**
	 * The progress monitor to report work completed too.
	 */
	protected IProgressMonitor progressMonitor = null;

	/**
	 * Change listeners - null if no one is listening.
	 */
	protected ArrayList changeListeners = null;

	/**
	 * A map from Openables to ArrayLists of ITypes
	 */
	public Map files = null;

	/**
	 * A region describing the packages considered by this hierarchy. Null if
	 * not activated.
	 */
	protected Region packageRegion = null;

	/**
	 * A region describing the projects considered by this hierarchy. Null if
	 * not activated.
	 */
	protected Region projectRegion = null;

	/**
	 * Whether this hierarchy should contains subtypes.
	 */
	protected boolean computeSubtypes;

	/**
	 * The scope this hierarchy should restrain itsef in.
	 */
	IJavaScriptSearchScope scope;

	/**
	 * Whether this hierarchy needs refresh
	 */
	public boolean needsRefresh = true;

	/**
	 * Collects changes to types
	 */
	protected ChangeCollector changeCollector;

	/**
	 * Creates an empty TypeHierarchy
	 */
	public TypeHierarchy() {
		// Creates an empty TypeHierarchy
	}

	/**
	 * Creates a TypeHierarchy on the given type.
	 */
	public TypeHierarchy(IType type, IJavaScriptUnit[] workingCopies, IJavaScriptProject project, boolean computeSubtypes) {
		this(type, workingCopies, SearchEngine.createJavaSearchScope(new IJavaScriptElement[]{project}), computeSubtypes);
		this.project = project;
	}

	/**
	 * Creates a TypeHierarchy on the given type.
	 */
	public TypeHierarchy(IType type, IJavaScriptUnit[] workingCopies, IJavaScriptSearchScope scope, boolean computeSubtypes) {
		this.focusType = type == null ? null : (IType) ((JavaElement) type).unresolved(); // unsure
		this.workingCopies = workingCopies;
		this.computeSubtypes = computeSubtypes;
		this.scope = scope;
	}

	/**
	 * Initializes the file, package and project regions
	 */
	protected void initializeRegions() {

		IType[] allTypes = getAllClasses();
		for (int i = 0; i < allTypes.length; i++) {
			IType type = allTypes[i];
			Openable o = (Openable) ((JavaElement) type).getOpenableParent();
			if (o != null) {
				ArrayList types = (ArrayList) this.files.get(o);
				if (types == null) {
					types = new ArrayList();
					this.files.put(o, types);
				}
				types.add(type);
			}
			IPackageFragment pkg = type.getPackageFragment();
			this.packageRegion.add(pkg);
			IJavaScriptProject declaringProject = type.getJavaScriptProject();
			if (declaringProject != null) {
				this.projectRegion.add(declaringProject);
			}
			checkCanceled();
		}
	}

	/**
	 * Adds the type to the collection of root classes if the classes is not
	 * already present in the collection.
	 */
	protected void addRootClass(IType type) {
		if (this.rootClasses.contains(type))
			return;
		this.rootClasses.add(type);
	}

	/**
	 * Adds the given subtype to the type.
	 */
	protected void addSubtype(IType type, IType subtype) {
	TypeVector subtypes = (TypeVector)this.typeToSubtypes.get(type.getDisplayName());
		if (subtypes == null) {
			subtypes = new TypeVector();
		this.typeToSubtypes.put(type.getDisplayName(), subtypes);
		}
		if (!subtypes.contains(subtype)) {
			subtypes.add(subtype);
		}
	}

	/**
	 * @see ITypeHierarchy
	 */
	public synchronized void addTypeHierarchyChangedListener(ITypeHierarchyChangedListener listener) {
		ArrayList listeners = this.changeListeners;
		if (listeners == null) {
			this.changeListeners = listeners = new ArrayList();
		}

		// register with JavaScriptCore to get Java element delta on first listener added
		if (listeners.size() == 0) {
			JavaScriptCore.addElementChangedListener(this);
		}

		// add listener only if it is not already present
		if (listeners.indexOf(listener) == -1) {
			listeners.add(listener);
		}
	}

	private static Integer bytesToFlags(byte[] bytes) {
		if (bytes != null && bytes.length > 0) {
			return Integer.valueOf(new String(bytes));
		}
		else {
			return null;
		}
	}

	/**
	 * cacheFlags.
	 */
	public void cacheFlags(IType type, int flags) {
		this.typeFlags.put(type, Integer.valueOf(flags));
	}

	/**
	 * Caches the handle of the superclass for the specified type. As a side
	 * effect cache this type as a subtype of the superclass.
	 */
	protected void cacheSuperclass(IType type, IType superclass) {
		if (superclass != null) {
			this.classToSuperclass.put(type, superclass);
			addSubtype(superclass, type);
		}
	}

	/**
	 * Checks with the progress monitor to see whether the creation of the
	 * type hierarchy should be canceled. Should be regularly called so that
	 * the user can cancel.
	 * 
	 * @exception OperationCanceledException
	 *                if cancelling the operation has been requested
	 * @see IProgressMonitor#isCanceled
	 */
	protected void checkCanceled() {
		if (this.progressMonitor != null && this.progressMonitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}

	/**
	 * Compute this type hierarchy.
	 */
	protected void compute() throws JavaScriptModelException, CoreException {
		if (this.focusType != null) {
			HierarchyBuilder builder = new IndexBasedHierarchyBuilder(this, this.scope);
			builder.build(this.computeSubtypes);
		} // else a RegionBasedTypeHierarchy should be used
	}

	/**
	 * @see ITypeHierarchy
	 */
	public boolean contains(IType type) {
		// classes
		if (this.classToSuperclass.get(type) != null) {
			return true;
		}

		// root classes
		if (this.rootClasses.contains(type))
			return true;

		return false;
	}

	/**
	 * Determines if the change effects this hierarchy, and fires change
	 * notification if required.
	 */
	public void elementChanged(ElementChangedEvent event) {
		// type hierarchy change has already been fired
		if (this.needsRefresh)
			return;

		if (isAffected(event.getDelta())) {
			this.needsRefresh = true;
			fireChange();
		}
	}

	/**
	 * @see ITypeHierarchy
	 */
	public boolean exists() {
		if (!this.needsRefresh)
			return true;

		return (this.focusType == null || this.focusType.exists()) && this.javaProject().exists();
	}

	/**
	 * Notifies listeners that this hierarchy has changed and needs
	 * refreshing. Note that listeners can be removed as we iterate through
	 * the list.
	 */
	public void fireChange() {
		ArrayList listeners = this.changeListeners;
		if (listeners == null) {
			return;
		}
		if (DEBUG) {
			System.out.println("FIRING hierarchy change [" + Thread.currentThread() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.focusType != null) {
				System.out.println("    for hierarchy focused on " + ((JavaElement) this.focusType).toStringWithAncestors()); //$NON-NLS-1$
			}
		}
		// clone so that a listener cannot have a side-effect on this list  when being notified
		listeners = (ArrayList) listeners.clone();
		for (int i = 0; i < listeners.size(); i++) {
			final ITypeHierarchyChangedListener listener = (ITypeHierarchyChangedListener) listeners.get(i);
			SafeRunner.run(new ISafeRunnable() {
				public void handleException(Throwable exception) {
					Util.log(exception, "Exception occurred in listener of Type hierarchy change notification"); //$NON-NLS-1$
				}

				public void run() throws Exception {
					listener.typeHierarchyChanged(TypeHierarchy.this);
				}
			});
		}
	}

	private static byte[] flagsToBytes(Integer flags) {
		if (flags != null) {
			return flags.toString().getBytes();
		}
		else {
			return NO_FLAGS;
		}
	}

	/**
	 * @see ITypeHierarchy
	 */
	public IType[] getAllClasses() {

		TypeVector classes = this.rootClasses.copy();
		for (Iterator iter = this.classToSuperclass.keySet().iterator(); iter.hasNext();) {
			classes.add((IType) iter.next());
		}
		return classes.elements();
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.ITypeHierarchy#getAllSubtypes(org.eclipse.wst.jsdt.core.IType)
	 */
	public IType[] getAllSubtypes(IType type) {
		Set subTypes = new HashSet();
		LinkedList typesToGetSubtypesOf = new LinkedList();
		typesToGetSubtypesOf.add(type);
		
		//for each type to get the sub types of
		while(!typesToGetSubtypesOf.isEmpty()) {
			IType typeToGetSubtypeOf = (IType)typesToGetSubtypesOf.removeFirst();
			IType[] currSubTypes = this.getSubtypesForType(typeToGetSubtypeOf);
			
			/* for each sub type check if it is already a known sub type
			 * if it is not add it to the list of known subtypes and then
			 * add it to the list of types to get the subtypes of */
			for(int i = 0; i < currSubTypes.length; ++i) {
				if(!subTypes.contains(currSubTypes[i])) {
					subTypes.add(currSubTypes[i]);
					typesToGetSubtypesOf.add(currSubTypes[i]);
				}
			}
		}
		
		IType[] subClasses = new IType[subTypes.size()];
		subTypes.toArray(subClasses);
		
		return subClasses;
	}

	/**
	 * Returns an array of subtypes for the given type - will never return
	 * null.
	 */
	private IType[] getSubtypesForType(IType type) {
	TypeVector vector = (TypeVector)this.typeToSubtypes.get(type.getDisplayName());
		if (vector == null)
			return NO_TYPE;
		else
			return vector.elements();
	}

	/**
	 * @see ITypeHierarchy
	 */
	public IType[] getAllSuperclasses(IType type) {
		IType superclass = getSuperclass(type);
		TypeVector supers = new TypeVector();
		while (superclass != null) {
			supers.add(superclass);
			superclass = getSuperclass(superclass);
		}
		return supers.elements();
	}

	/**
	 * @see ITypeHierarchy#getCachedFlags(IType)
	 */
	public int getCachedFlags(IType type) {
		Integer flagObject = (Integer) this.typeFlags.get(type);
		if (flagObject != null) {
			return flagObject.intValue();
		}
		return -1;
	}

	/**
	 * @see ITypeHierarchy
	 */
	public IType[] getRootClasses() {
		return this.rootClasses.elements();
	}

	/**
	 * @see ITypeHierarchy
	 */
	public IType[] getSubclasses(IType type) {
	TypeVector vector = (TypeVector)this.typeToSubtypes.get(type.getDisplayName());
		if (vector == null)
			return NO_TYPE;
		else
			return vector.elements();
	}

	/**
	 * @see ITypeHierarchy
	 */
	public IType getSuperclass(IType type) {
		return (IType) this.classToSuperclass.get(type);
	}

	/**
	 * @see ITypeHierarchy
	 */
	public IType getType() {
		return this.focusType;
	}

	/**
	 * Adds the new elements to a new array that contains all of the elements
	 * of the old array. Returns the new array.
	 */
	protected IType[] growAndAddToArray(IType[] array, IType[] additions) {
		if (array == null || array.length == 0) {
			return additions;
		}
		IType[] old = array;
		array = new IType[old.length + additions.length];
		System.arraycopy(old, 0, array, 0, old.length);
		System.arraycopy(additions, 0, array, old.length, additions.length);
		return array;
	}

	/**
	 * Adds the new element to a new array that contains all of the elements
	 * of the old array. Returns the new array.
	 */
	protected IType[] growAndAddToArray(IType[] array, IType addition) {
		if (array == null || array.length == 0) {
			return new IType[]{addition};
		}
		IType[] old = array;
		array = new IType[old.length + 1];
		System.arraycopy(old, 0, array, 0, old.length);
		array[old.length] = addition;
		return array;
	}

	/**
	 * Whether fine-grained deltas where collected and affects this hierarchy.
	 */
	public boolean hasFineGrainChanges() {
		ChangeCollector collector = this.changeCollector;
		return collector != null && collector.needsRefresh();
	}

	/**
	 * Returns whether one of the subtypes in this hierarchy has the given
	 * simple name or this type has the given simple name.
	 */
	private boolean hasSubtypeNamed(String simpleName) {
		if (this.focusType != null && this.focusType.getElementName().equals(simpleName)) {
			return true;
		}
		IType[] types = this.focusType == null ? getAllClasses() : getAllSubtypes(this.focusType);
		for (int i = 0, length = types.length; i < length; i++) {
			if (types[i].getElementName().equals(simpleName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns whether one of the types in this hierarchy has the given simple
	 * name.
	 */
	private boolean hasTypeNamed(String simpleName) {
		IType[] types = this.getAllClasses();
		for (int i = 0, length = types.length; i < length; i++) {
			if (types[i].getElementName().equals(simpleName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns whether the simple name of the given type or one of its
	 * supertypes is the simple name of one of the types in this hierarchy.
	 */
	boolean includesTypeOrSupertype(IType type) {
		try {
			// check type
			if (hasTypeNamed(type.getElementName()))
				return true;

			// check superclass
			String superclassName = type.getSuperclassName();
			if (superclassName != null) {
				int lastSeparator = superclassName.lastIndexOf('.');
				String simpleName = superclassName.substring(lastSeparator + 1);
				if (hasTypeNamed(simpleName))
					return true;
			}
		}
		catch (JavaScriptModelException e) {
			// ignore
		}
		return false;
	}

	/**
	 * Initializes this hierarchy's internal tables with the given size.
	 */
	protected void initialize(int size) {
		if (size < 10) {
			size = 10;
		}
		int smallSize = (size / 2);
		this.classToSuperclass = new HashMap(size);
		this.missingTypes = new ArrayList(smallSize);
		this.rootClasses = new TypeVector();
		this.typeToSubtypes = new HashMap(smallSize);
		this.typeFlags = new HashMap(smallSize);

		this.projectRegion = new Region();
		this.packageRegion = new Region();
		this.files = new HashMap(5);
	}

	/**
	 * Returns true if the given delta could change this type hierarchy
	 */
	public synchronized boolean isAffected(IJavaScriptElementDelta delta) {
		IJavaScriptElement element = delta.getElement();
		switch (element.getElementType()) {
			case IJavaScriptElement.JAVASCRIPT_MODEL :
				return isAffectedByJavaModel(delta, element);
			case IJavaScriptElement.JAVASCRIPT_PROJECT :
				return isAffectedByJavaProject(delta, element);
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT :
				return isAffectedByPackageFragmentRoot(delta, element);
			case IJavaScriptElement.PACKAGE_FRAGMENT :
				return isAffectedByPackageFragment(delta, (PackageFragment) element);
			case IJavaScriptElement.CLASS_FILE :
			case IJavaScriptElement.JAVASCRIPT_UNIT :
				return isAffectedByOpenable(delta, element);
		}
		return false;
	}

	/**
	 * Returns true if any of the children of a project, package fragment
	 * root, or package fragment have changed in a way that effects this type
	 * hierarchy.
	 */
	private boolean isAffectedByChildren(IJavaScriptElementDelta delta) {
		if ((delta.getFlags() & IJavaScriptElementDelta.F_CHILDREN) > 0) {
			IJavaScriptElementDelta[] children = delta.getAffectedChildren();
			for (int i = 0; i < children.length; i++) {
				if (isAffected(children[i])) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns true if the given java model delta could affect this type
	 * hierarchy
	 */
	private boolean isAffectedByJavaModel(IJavaScriptElementDelta delta, IJavaScriptElement element) {
		switch (delta.getKind()) {
			case IJavaScriptElementDelta.ADDED :
			case IJavaScriptElementDelta.REMOVED :
				return element.equals(this.javaProject().getJavaScriptModel());
			case IJavaScriptElementDelta.CHANGED :
				return isAffectedByChildren(delta);
		}
		return false;
	}

	/**
	 * Returns true if the given java project delta could affect this type
	 * hierarchy
	 */
	private boolean isAffectedByJavaProject(IJavaScriptElementDelta delta, IJavaScriptElement element) {
		int kind = delta.getKind();
		int flags = delta.getFlags();
		if ((flags & IJavaScriptElementDelta.F_OPENED) != 0) {
			kind = IJavaScriptElementDelta.ADDED; // affected in the same way
		}
		if ((flags & IJavaScriptElementDelta.F_CLOSED) != 0) {
			kind = IJavaScriptElementDelta.REMOVED; // affected in the same way
		}
		switch (kind) {
			case IJavaScriptElementDelta.ADDED :
				try {
					// if the added project is on the classpath, then the hierarchy has changed
					IIncludePathEntry[] classpath = ((JavaProject) this.javaProject()).getExpandedClasspath();
					for (int i = 0; i < classpath.length; i++) {
						if (classpath[i].getEntryKind() == IIncludePathEntry.CPE_PROJECT && classpath[i].getPath().equals(element.getPath())) {
							return true;
						}
					}
					if (this.focusType != null) {
						/* if the hierarchy's project is on the added project
						 * classpath, then the hierarchy has changed */
						classpath = ((JavaProject) element).getExpandedClasspath();
						IPath hierarchyProject = javaProject().getPath();
						for (int i = 0; i < classpath.length; i++) {
							if (classpath[i].getEntryKind() == IIncludePathEntry.CPE_PROJECT && classpath[i].getPath().equals(hierarchyProject)) {
								return true;
							}
						}
					}
					return false;
				}
				catch (JavaScriptModelException e) {
					return false;
				}
			case IJavaScriptElementDelta.REMOVED :
				/* removed project - if it contains packages we are interested in
				 * then the type hierarchy has changed */
				IJavaScriptElement[] pkgs = this.packageRegion.getElements();
				for (int i = 0; i < pkgs.length; i++) {
					IJavaScriptProject javaProject = pkgs[i].getJavaScriptProject();
					if (javaProject != null && javaProject.equals(element)) {
						return true;
					}
				}
				return false;
			case IJavaScriptElementDelta.CHANGED :
				return isAffectedByChildren(delta);
		}
		return false;
	}

	/**
	 * Returns true if the given package fragment delta could affect this type
	 * hierarchy
	 */
	private boolean isAffectedByPackageFragment(IJavaScriptElementDelta delta, PackageFragment element) {
		switch (delta.getKind()) {
			case IJavaScriptElementDelta.ADDED :
				// if the package fragment is in the projects being considered, this could
				// introduce new types, changing the hierarchy
				return this.projectRegion.contains(element);
			case IJavaScriptElementDelta.REMOVED :
				// is a change if the package fragment contains types in this hierarchy
				return packageRegionContainsSamePackageFragment(element);
			case IJavaScriptElementDelta.CHANGED :
				// look at the files in the package fragment
				return isAffectedByChildren(delta);
		}
		return false;
	}

	/**
	 * Returns true if the given package fragment root delta could affect this
	 * type hierarchy
	 */
	private boolean isAffectedByPackageFragmentRoot(IJavaScriptElementDelta delta, IJavaScriptElement element) {
		switch (delta.getKind()) {
			case IJavaScriptElementDelta.ADDED :
				return this.projectRegion.contains(element);
			case IJavaScriptElementDelta.REMOVED :
			case IJavaScriptElementDelta.CHANGED :
				int flags = delta.getFlags();
				if ((flags & IJavaScriptElementDelta.F_ADDED_TO_CLASSPATH) > 0) {
					// check if the root is in the classpath of one of the projects of this hierarchy
					if (this.projectRegion != null) {
						IPackageFragmentRoot root = (IPackageFragmentRoot) element;
						IPath rootPath = root.getPath();
						IJavaScriptElement[] elements = this.projectRegion.getElements();
						for (int i = 0; i < elements.length; i++) {
							JavaProject javaProject = (JavaProject) elements[i];
							try {
								IIncludePathEntry entry = javaProject.getClasspathEntryFor(rootPath);
								if (entry != null) {
									return true;
								}
							}
							catch (JavaScriptModelException e) {
								// igmore this project
							}
						}
					}
				}
				if ((flags & IJavaScriptElementDelta.F_REMOVED_FROM_CLASSPATH) > 0 || (flags & IJavaScriptElementDelta.F_CONTENT) > 0) {
					/* 1. removed from classpath - if it contains packages we
					 * are interested in the the type hierarchy has changed
					 * 
					 * 2. content of a jar changed - if it contains packages  we are interested in
					 *  the the type hierarchy has changed */
					IJavaScriptElement[] pkgs = this.packageRegion.getElements();
					for (int i = 0; i < pkgs.length; i++) {
						if (pkgs[i].getParent().equals(element)) {
							return true;
						}
					}
					return false;
				}
		}
		return isAffectedByChildren(delta);
	}

	/**
	 * Returns true if the given type delta (a compilation unit delta or a
	 * class file delta) could affect this type hierarchy.
	 */
	protected boolean isAffectedByOpenable(IJavaScriptElementDelta delta, IJavaScriptElement element) {
		if (element instanceof CompilationUnit) {
			CompilationUnit cu = (CompilationUnit) element;
			ChangeCollector collector = this.changeCollector;
			if (collector == null) {
				collector = new ChangeCollector(this);
			}
			try {
				collector.addChange(cu, delta);
			}
			catch (JavaScriptModelException e) {
				if (DEBUG)
					e.printStackTrace();
			}
			if (cu.isWorkingCopy()) {
				// changes to working copies are batched
				this.changeCollector = collector;
				return false;
			}
			else {
				return collector.needsRefresh();
			}
		}
		else if (element instanceof ClassFile) {
			switch (delta.getKind()) {
				case IJavaScriptElementDelta.REMOVED :
					return this.files.get(element) != null;
				case IJavaScriptElementDelta.ADDED :
					IType type = ((ClassFile) element).getType();
					String typeName = type.getElementName();
					if (hasSupertype(typeName) || subtypesIncludeSupertypeOf(type) || this.missingTypes.contains(typeName)) {

						return true;
					}
					break;
				case IJavaScriptElementDelta.CHANGED :
					IJavaScriptElementDelta[] children = delta.getAffectedChildren();
					for (int i = 0, length = children.length; i < length; i++) {
						IJavaScriptElementDelta child = children[i];
						IJavaScriptElement childElement = child.getElement();
						if (childElement instanceof IType) {
							type = (IType) childElement;
							boolean hasVisibilityChange = (delta.getFlags() & IJavaScriptElementDelta.F_MODIFIERS) > 0;
							boolean hasSupertypeChange = (delta.getFlags() & IJavaScriptElementDelta.F_SUPER_TYPES) > 0;
							if ((hasVisibilityChange && hasSupertype(type.getElementName())) || (hasSupertypeChange && includesTypeOrSupertype(type))) {
								return true;
							}
						}
					}
					break;
			}
		}
		return false;
	}

	/**
	 * Returns the java project this hierarchy was created in.
	 */
	public IJavaScriptProject javaProject() {
		return this.focusType.getJavaScriptProject();
	}

	protected static byte[] readUntil(InputStream input, byte separator) throws JavaScriptModelException, IOException {
		return readUntil(input, separator, 0);
	}

	protected static byte[] readUntil(InputStream input, byte separator, int offset) throws IOException, JavaScriptModelException {
		int length = 0;
		byte[] bytes = new byte[SIZE];
		byte b;
		while ((b = (byte) input.read()) != separator && b != -1) {
			if (bytes.length == length) {
				System.arraycopy(bytes, 0, bytes = new byte[length * 2], 0, length);
			}
			bytes[length++] = b;
		}
		if (b == -1) {
			throw new JavaScriptModelException(new JavaModelStatus(IStatus.ERROR));
		}
		System.arraycopy(bytes, 0, bytes = new byte[length + offset], offset, length);
		return bytes;
	}

	public static ITypeHierarchy load(IType type, InputStream input, WorkingCopyOwner owner) throws JavaScriptModelException {
		try {
			TypeHierarchy typeHierarchy = new TypeHierarchy();
			typeHierarchy.initialize(1);

			IType[] types = new IType[SIZE];
			int typeCount = 0;

			byte version = (byte) input.read();

			if (version != VERSION) {
				throw new JavaScriptModelException(new JavaModelStatus(IStatus.ERROR));
			}
			byte generalInfo = (byte) input.read();
			if ((generalInfo & COMPUTE_SUBTYPES) != 0) {
				typeHierarchy.computeSubtypes = true;
			}

			byte b;
			byte[] bytes;

			// read project
			bytes = readUntil(input, SEPARATOR1);
			if (bytes.length > 0) {
				typeHierarchy.project = (IJavaScriptProject) JavaScriptCore.create(new String(bytes));
				typeHierarchy.scope = SearchEngine.createJavaSearchScope(new IJavaScriptElement[]{typeHierarchy.project});
			}
			else {
				typeHierarchy.project = null;
				typeHierarchy.scope = SearchEngine.createWorkspaceScope();
			}

			// read missing type
			{
				bytes = readUntil(input, SEPARATOR1);
				byte[] missing;
				int j = 0;
				int length = bytes.length;
				for (int i = 0; i < length; i++) {
					b = bytes[i];
					if (b == SEPARATOR2) {
						missing = new byte[i - j];
						System.arraycopy(bytes, j, missing, 0, i - j);
						typeHierarchy.missingTypes.add(new String(missing));
						j = i + 1;
					}
				}
				System.arraycopy(bytes, j, missing = new byte[length - j], 0, length - j);
				typeHierarchy.missingTypes.add(new String(missing));
			}

			// read types
			while ((b = (byte) input.read()) != SEPARATOR1 && b != -1) {
				bytes = readUntil(input, SEPARATOR4, 1);
				bytes[0] = b;
				IType element = (IType) JavaScriptCore.create(new String(bytes), owner);

				if (types.length == typeCount) {
					System.arraycopy(types, 0, types = new IType[typeCount * 2], 0, typeCount);
				}
				types[typeCount++] = element;

				// read flags
				bytes = readUntil(input, SEPARATOR4);
				Integer flags = bytesToFlags(bytes);
				if (flags != null) {
					typeHierarchy.cacheFlags(element, flags.intValue());
				}

				// read info
				byte info = (byte) input.read();

				if ((info & COMPUTED_FOR) != 0) {
					if (!element.equals(type)) {
						throw new JavaScriptModelException(new JavaModelStatus(IStatus.ERROR));
					}
					typeHierarchy.focusType = element;
				}
				if ((info & ROOT) != 0) {
					typeHierarchy.addRootClass(element);
				}
			}

			// read super class
			while ((b = (byte) input.read()) != SEPARATOR1 && b != -1) {
				bytes = readUntil(input, SEPARATOR3, 1);
				bytes[0] = b;
				int subClass = Integer.valueOf(new String(bytes)).intValue();

				// read super type
				bytes = readUntil(input, SEPARATOR1);
				int superClass = Integer.valueOf(new String(bytes)).intValue();

				typeHierarchy.cacheSuperclass(types[subClass], types[superClass]);
			}

			if (b == -1) {
				throw new JavaScriptModelException(new JavaModelStatus(IStatus.ERROR));
			}
			return typeHierarchy;
		}
		catch (IOException e) {
			throw new JavaScriptModelException(e, IJavaScriptModelStatusConstants.IO_EXCEPTION);
		}
	}

	/**
	 * Returns <code>true</code> if an equivalent package fragment is included
	 * in the package region. Package fragments are equivalent if they both
	 * have the same name.
	 */
	protected boolean packageRegionContainsSamePackageFragment(PackageFragment element) {
		IJavaScriptElement[] pkgs = this.packageRegion.getElements();
		for (int i = 0; i < pkgs.length; i++) {
			PackageFragment pkg = (PackageFragment) pkgs[i];
			if (Util.equalArraysOrNull(pkg.names, element.names))
				return true;
		}
		return false;
	}

	/**
	 * @see ITypeHierarchy TODO (jerome) should use a PerThreadObject to build
	 *      the hierarchy instead of synchronizing (see also
	 *      isAffected(IJavaScriptElementDelta))
	 */
	public synchronized void refresh(IProgressMonitor monitor) throws JavaScriptModelException {
		try {
			this.progressMonitor = monitor;
			if (monitor != null) {
				monitor.beginTask(this.focusType != null ? Messages.bind(Messages.hierarchy_creatingOnType, this.focusType.getFullyQualifiedName()) : Messages.hierarchy_creating, 100);
			}
			long start = -1;
			if (DEBUG) {
				start = System.currentTimeMillis();
				if (this.computeSubtypes) {
					System.out.println("CREATING TYPE HIERARCHY [" + Thread.currentThread() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				else {
					System.out.println("CREATING SUPER TYPE HIERARCHY [" + Thread.currentThread() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if (this.focusType != null) {
					System.out.println("  on type " + ((JavaElement) this.focusType).toStringWithAncestors()); //$NON-NLS-1$
				}
			}

			compute();
			initializeRegions();
			this.needsRefresh = false;
			this.changeCollector = null;

			if (DEBUG) {
				if (this.computeSubtypes) {
					System.out.println("CREATED TYPE HIERARCHY in " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				else {
					System.out.println("CREATED SUPER TYPE HIERARCHY in " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				System.out.println(this.toString());
			}
		}
		catch (JavaScriptModelException e) {
			throw e;
		}
		catch (CoreException e) {
			throw new JavaScriptModelException(e);
		}
		finally {
			if (monitor != null) {
				monitor.done();
			}
			this.progressMonitor = null;
		}
	}

	/**
	 * @see ITypeHierarchy
	 */
	public synchronized void removeTypeHierarchyChangedListener(ITypeHierarchyChangedListener listener) {
		ArrayList listeners = this.changeListeners;
		if (listeners == null) {
			return;
		}
		listeners.remove(listener);

		// deregister from JavaScriptCore on last listener removed
		if (listeners.isEmpty()) {
			JavaScriptCore.removeElementChangedListener(this);
		}
	}

	/**
	 * @see ITypeHierarchy
	 */
	public void store(OutputStream output, IProgressMonitor monitor) throws JavaScriptModelException {
		try {
			// compute types in hierarchy
			Hashtable hashtable = new Hashtable();
			Hashtable hashtable2 = new Hashtable();
			int count = 0;

			if (this.focusType != null) {
				Integer index = Integer.valueOf(count++);
				hashtable.put(this.focusType, index);
				hashtable2.put(index, this.focusType);
			}
			Object[] types = this.classToSuperclass.entrySet().toArray();
			for (int i = 0; i < types.length; i++) {
				Map.Entry entry = (Map.Entry) types[i];
				Object t = entry.getKey();
				if (hashtable.get(t) == null) {
					Integer index = Integer.valueOf(count++);
					hashtable.put(t, index);
					hashtable2.put(index, t);
				}
				Object superClass = entry.getValue();
				if (superClass != null && hashtable.get(superClass) == null) {
					Integer index = Integer.valueOf(count++);
					hashtable.put(superClass, index);
					hashtable2.put(index, superClass);
				}
			}
			// save version of the hierarchy format
			output.write(VERSION);

			// save general info
			byte generalInfo = 0;
			if (this.computeSubtypes) {
				generalInfo |= COMPUTE_SUBTYPES;
			}
			output.write(generalInfo);

			// save project
			if (this.project != null) {
				output.write(this.project.getHandleIdentifier().getBytes());
			}
			output.write(SEPARATOR1);

			// save missing types
			for (int i = 0; i < this.missingTypes.size(); i++) {
				if (i != 0) {
					output.write(SEPARATOR2);
				}
				output.write(((String) this.missingTypes.get(i)).getBytes());

			}
			output.write(SEPARATOR1);

			// save types
			for (int i = 0; i < count; i++) {
				IType t = (IType) hashtable2.get(Integer.valueOf(i));

				// n bytes
				output.write(t.getHandleIdentifier().getBytes());
				output.write(SEPARATOR4);
				output.write(flagsToBytes((Integer) this.typeFlags.get(t)));
				output.write(SEPARATOR4);
				byte info = CLASS;
				if (this.focusType != null && this.focusType.equals(t)) {
					info |= COMPUTED_FOR;
				}
				if (this.rootClasses.contains(t)) {
					info |= ROOT;
				}
				output.write(info);
			}
			output.write(SEPARATOR1);

			// save superclasses
			types = this.classToSuperclass.entrySet().toArray();
			for (int i = 0; i < types.length; i++) {
				Map.Entry entry = (Map.Entry) types[i];
				IJavaScriptElement key = (IJavaScriptElement) entry.getKey();
				IJavaScriptElement value = (IJavaScriptElement) entry.getValue();

				output.write(((Integer) hashtable.get(key)).toString().getBytes());
				output.write('>');
				output.write(((Integer) hashtable.get(value)).toString().getBytes());
				output.write(SEPARATOR1);
			}
			output.write(SEPARATOR1);

			output.write(SEPARATOR1);
		}
		catch (IOException e) {
			throw new JavaScriptModelException(e, IJavaScriptModelStatusConstants.IO_EXCEPTION);
		}
	}

	/**
	 * Returns whether the simple name of a supertype of the given type is the
	 * simple name of one of the subtypes in this hierarchy or the simple name
	 * of this type.
	 */
	boolean subtypesIncludeSupertypeOf(IType type) {
		// look for superclass
		String superclassName = null;
		try {
			superclassName = type.getSuperclassName();
		}
		catch (JavaScriptModelException e) {
			if (DEBUG) {
				e.printStackTrace();
			}
			return false;
		}
		if (superclassName == null) {
			superclassName = "Object"; //$NON-NLS-1$
		}
		int dot = -1;
		String simpleSuper = (dot = superclassName.lastIndexOf('.')) > -1 ? superclassName.substring(dot + 1) : superclassName;
		if (hasSubtypeNamed(simpleSuper)) {
			return true;
		}

		return false;
	}

	/**
	 * @see ITypeHierarchy
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Focus: "); //$NON-NLS-1$
		buffer.append(this.focusType == null ? "<NONE>" : ((JavaElement) this.focusType).toStringWithAncestors(false/*don't show key*/)); //$NON-NLS-1$
		buffer.append("\n"); //$NON-NLS-1$
		if (exists()) {
			if (this.focusType != null) {
				buffer.append("Super types:\n"); //$NON-NLS-1$
				toString(buffer, this.focusType, 1, true);
				buffer.append("Sub types:\n"); //$NON-NLS-1$
				toString(buffer, this.focusType, 1, false);
			}
			else {
				buffer.append("Sub types of root classes:\n"); //$NON-NLS-1$
				IJavaScriptElement[] roots = Util.sortCopy(getRootClasses());
				for (int i = 0; i < roots.length; i++) {
					toString(buffer, (IType) roots[i], 1, false);
				}
			}
			if (this.rootClasses.size > 1) {
				buffer.append("Root classes:\n"); //$NON-NLS-1$
				IJavaScriptElement[] roots = Util.sortCopy(getRootClasses());
				for (int i = 0, length = roots.length; i < length; i++) {
					toString(buffer, (IType) roots[i], 1, false);
				}
			}
			else if (this.rootClasses.size == 0) {
				// see http://bugs.eclipse.org/bugs/show_bug.cgi?id=24691
				buffer.append("No root classes"); //$NON-NLS-1$
			}
		}
		else {
			buffer.append("(Hierarchy became stale)"); //$NON-NLS-1$
		}
		return buffer.toString();
	}

	/**
	 * Append a String to the given buffer representing the hierarchy for the
	 * type, beginning with the specified indentation level. If ascendant,
	 * shows the super types, otherwise show the sub types.
	 */
	private void toString(StringBuffer buffer, IType type, int indent, boolean ascendant) {
		IType[] types = ascendant ? new IType[]{getSuperclass(type)} : getSubclasses(type);
		IJavaScriptElement[] sortedTypes = Util.sortCopy(types);
		for (int i = 0; i < sortedTypes.length; i++) {
			for (int j = 0; j < indent; j++) {
				buffer.append("  "); //$NON-NLS-1$
			}
			JavaElement element = (JavaElement) sortedTypes[i];
			buffer.append(element.toStringWithAncestors(false/* don't show key */));
			buffer.append('\n');
			toString(buffer, types[i], indent + 1, ascendant);
		}
	}

	/**
	 * Returns whether one of the types in this hierarchy has a supertype
	 * whose simple name is the given simple name.
	 */
	boolean hasSupertype(String simpleName) {
		for (Iterator iter = this.classToSuperclass.values().iterator(); iter.hasNext();) {
			IType superType = (IType) iter.next();
			if (superType.getElementName().equals(simpleName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @see IProgressMonitor
	 */
	protected void worked(int work) {
		if (this.progressMonitor != null) {
			this.progressMonitor.worked(work);
			checkCanceled();
		}
	}
}