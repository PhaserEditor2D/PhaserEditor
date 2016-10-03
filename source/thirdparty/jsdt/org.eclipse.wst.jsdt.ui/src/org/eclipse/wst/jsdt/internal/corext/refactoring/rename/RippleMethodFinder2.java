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
package org.eclipse.wst.jsdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IRegion;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;

public class RippleMethodFinder2 {
	
	private final IFunction fMethod;
	private List/*<IFunction>*/ fDeclarations;
	private ITypeHierarchy fHierarchy;
	private Map/*IType, IFunction*/ fTypeToMethod;
	private Set/*IType*/ fRootTypes;
	private MultiMap/*IType, IType*/ fRootReps;
	private Map/*IType, ITypeHierarchy*/ fRootHierarchies;
	private UnionFind fUnionFind;
	private boolean fExcludeBinaries;

	private static class MultiMap {
		HashMap/*<IType, Collection>*/ fImplementation= new HashMap();

		public void put(IType key, IType value) {
			Collection collection= (Collection) fImplementation.get(key);
			if (collection == null) {
				collection= new HashSet();
				fImplementation.put(key, collection);
			}
			collection.add(value);
		}
		
		public Collection get(IType key) {
			return (Collection) fImplementation.get(key);
		}
	}
	private static class UnionFind {
		HashMap/*<IType, IType>*/ fElementToRepresentative= new HashMap();
		
		public void init(IType type) {
			fElementToRepresentative.put(type, type);
		}
		
		//path compression:
		public IType find(IType element) {
			IType root= element;
			IType rep= (IType) fElementToRepresentative.get(root);
			while (rep != null && ! rep.equals(root)) {
				root= rep;
				rep= (IType) fElementToRepresentative.get(root);
			}
			if (rep == null)
				return null;

			rep= (IType) fElementToRepresentative.get(element);
			while (! rep.equals(root)) {
				IType temp= element;
				element= rep;
				fElementToRepresentative.put(temp, root);
				rep= (IType) fElementToRepresentative.get(element);
			}
			return root;
		}
		
//		//straightforward:
//		public IType find(IType element) {
//			IType current= element;
//			IType rep= (IType) fElementToRepresentative.get(current);
//			while (rep != null && ! rep.equals(current)) {
//				current= rep;
//				rep= (IType) fElementToRepresentative.get(current);
//			}
//			if (rep == null)
//				return null;
//			else
//				return current;
//		}
		
		public void union(IType rep1, IType rep2) {
			fElementToRepresentative.put(rep1, rep2);
		}
	}
	
	
	private RippleMethodFinder2(IFunction method, boolean excludeBinaries){
		fMethod= method;
		fExcludeBinaries= excludeBinaries;
	}
	
	public static IFunction[] getRelatedMethods(IFunction method, boolean excludeBinaries, IProgressMonitor pm, WorkingCopyOwner owner) throws CoreException {
		try{
			if (! MethodChecks.isVirtual(method))
				return new IFunction[]{ method };
			
			return new RippleMethodFinder2(method, excludeBinaries).getAllRippleMethods(pm, owner);
		} finally{
			pm.done();
		}
	}
	public static IFunction[] getRelatedMethods(IFunction method, IProgressMonitor pm, WorkingCopyOwner owner) throws CoreException {
		return getRelatedMethods(method, true, pm, owner);	
	}
	
	private IFunction[] getAllRippleMethods(IProgressMonitor pm, WorkingCopyOwner owner) throws CoreException {
		pm.beginTask("", 4); //$NON-NLS-1$
		
		findAllDeclarations(new SubProgressMonitor(pm, 1), owner);
		//TODO: report binary methods to an error status
		//TODO: report assertion as error status and fall back to only return fMethod
		//check for bug 81058: 
		Assert.isTrue(fDeclarations.contains(fMethod), "Search for method declaration did not find original element"); //$NON-NLS-1$
		
		createHierarchyOfDeclarations(new SubProgressMonitor(pm, 1), owner);
		createTypeToMethod();
		createUnionFind();
		if (pm.isCanceled())
			throw new OperationCanceledException();

		fHierarchy= null;
		fRootTypes= null;

		Map/*IType, List<IType>*/ partitioning= new HashMap();
		for (Iterator iter= fTypeToMethod.keySet().iterator(); iter.hasNext();) {
			IType type= (IType) iter.next();
			IType rep= fUnionFind.find(type);
			List/*<IType>*/ types= (List) partitioning.get(rep);
			if (types == null)
				types= new ArrayList();
			types.add(type);
			partitioning.put(rep, types);
		}
		Assert.isTrue(partitioning.size() > 0);
		if (partitioning.size() == 1)
			return (IFunction[]) fDeclarations.toArray(new IFunction[fDeclarations.size()]);
		
		//Multiple partitions; must look out for nasty marriage cases
		//(types inheriting method from two ancestors, but without redeclaring it).
		IType methodTypeRep= fUnionFind.find(fMethod.getDeclaringType());
		List/*<IType>*/ relatedTypes= (List) partitioning.get(methodTypeRep);
		boolean hasRelatedInterfaces= false;
		List/*<IFunction>*/ relatedMethods= new ArrayList();
		for (Iterator iter= relatedTypes.iterator(); iter.hasNext();) {
			IType relatedType= (IType) iter.next();
			relatedMethods.add(fTypeToMethod.get(relatedType));
		}
		
		//Definition: An alien type is a type that is not a related type. The set of
		// alien types diminishes as new types become related (a.k.a marry a relatedType).
		
		List/*<IFunction>*/ alienDeclarations= new ArrayList(fDeclarations);
		fDeclarations= null;
		alienDeclarations.removeAll(relatedMethods);
		List/*<IType>*/ alienTypes= new ArrayList();
		boolean hasAlienInterfaces= false;
		for (Iterator iter= alienDeclarations.iterator(); iter.hasNext();) {
			IFunction alienDeclaration= (IFunction) iter.next();
			IType alienType= alienDeclaration.getDeclaringType();
			alienTypes.add(alienType);
		}
		if (alienTypes.size() == 0) //no nasty marriage scenarios without types to marry with...
			return (IFunction[]) relatedMethods.toArray(new IFunction[relatedMethods.size()]);
		if (! hasRelatedInterfaces && ! hasAlienInterfaces) //no nasty marriage scenarios without interfaces...
			return (IFunction[]) relatedMethods.toArray(new IFunction[relatedMethods.size()]);
		
		//find all subtypes of related types:
		HashSet/*<IType>*/ relatedSubTypes= new HashSet();
		List/*<IType>*/ relatedTypesToProcess= new ArrayList(relatedTypes);
		while (relatedTypesToProcess.size() > 0) {
			//TODO: would only need subtype hierarchies of all top-of-ripple relatedTypesToProcess
			for (Iterator iter= relatedTypesToProcess.iterator(); iter.hasNext();) {
				if (pm.isCanceled())
					throw new OperationCanceledException();
				IType relatedType= (IType) iter.next();
				ITypeHierarchy hierarchy= getCachedHierarchy(relatedType, owner, new SubProgressMonitor(pm, 1));
				if (hierarchy == null)
					hierarchy= relatedType.newTypeHierarchy(owner, new SubProgressMonitor(pm, 1));
				IType[] allSubTypes= hierarchy.getAllSubtypes(relatedType);
				for (int i= 0; i < allSubTypes.length; i++)
					relatedSubTypes.add(allSubTypes[i]);
			}
			relatedTypesToProcess.clear(); //processed; make sure loop terminates
			
			HashSet/*<IType>*/ marriedAlienTypeReps= new HashSet();
			for (Iterator iter= alienTypes.iterator(); iter.hasNext();) {
				if (pm.isCanceled())
					throw new OperationCanceledException();
				IType alienType= (IType) iter.next();
				IFunction alienMethod= (IFunction) fTypeToMethod.get(alienType);
				ITypeHierarchy hierarchy= getCachedHierarchy(alienType, owner, new SubProgressMonitor(pm, 1));
				if (hierarchy == null)
					hierarchy= alienType.newTypeHierarchy(owner, new SubProgressMonitor(pm, 1));
				IType[] allSubtypes= hierarchy.getAllSubtypes(alienType);
				for (int i= 0; i < allSubtypes.length; i++) {
					IType subtype= allSubtypes[i];
					if (relatedSubTypes.contains(subtype)) {
						if (JavaModelUtil.isVisibleInHierarchy(alienMethod, subtype.getPackageFragment())) {
							marriedAlienTypeReps.add(fUnionFind.find(alienType));
						} else {
							// not overridden
						}
					}
				}
			}
			
			if (marriedAlienTypeReps.size() == 0)
				return (IFunction[]) relatedMethods.toArray(new IFunction[relatedMethods.size()]);
			
			for (Iterator iter= marriedAlienTypeReps.iterator(); iter.hasNext();) {
				IType marriedAlienTypeRep= (IType) iter.next();
				List/*<IType>*/ marriedAlienTypes= (List) partitioning.get(marriedAlienTypeRep);
				for (Iterator iterator= marriedAlienTypes.iterator(); iterator.hasNext();) {
					IType marriedAlienInterfaceType= (IType) iterator.next();
					relatedMethods.add(fTypeToMethod.get(marriedAlienInterfaceType));
				}
				alienTypes.removeAll(marriedAlienTypes); //not alien any more
				relatedTypesToProcess.addAll(marriedAlienTypes); //process freshly married types again
			}
		}

		fRootReps= null;
		fRootHierarchies= null;
		fTypeToMethod= null;
		fUnionFind= null;

		return (IFunction[]) relatedMethods.toArray(new IFunction[relatedMethods.size()]);
	}

	private ITypeHierarchy getCachedHierarchy(IType type, WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaScriptModelException {
		IType rep= fUnionFind.find(type);
		if (rep != null) {
			Collection collection= fRootReps.get(rep);
			for (Iterator iter= collection.iterator(); iter.hasNext();) {
				IType root= (IType) iter.next();
				ITypeHierarchy hierarchy= (ITypeHierarchy) fRootHierarchies.get(root);
				if (hierarchy == null) {
					hierarchy= root.newTypeHierarchy(owner, new SubProgressMonitor(monitor, 1));
					fRootHierarchies.put(root, hierarchy);
				}
				if (hierarchy.contains(type))
					return hierarchy;
			}
		}
		return null;
	}

	private void findAllDeclarations(IProgressMonitor monitor, WorkingCopyOwner owner) throws CoreException {
		fDeclarations= new ArrayList();
		final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(SearchPattern.createPattern(fMethod, IJavaScriptSearchConstants.DECLARATIONS | IJavaScriptSearchConstants.IGNORE_DECLARING_TYPE | IJavaScriptSearchConstants.IGNORE_RETURN_TYPE, SearchPattern.R_ERASURE_MATCH | SearchPattern.R_CASE_SENSITIVE));
		if (owner != null)
			engine.setOwner(owner);
		engine.setScope(RefactoringScopeFactory.createRelatedProjectsScope(fMethod.getJavaScriptProject(), IJavaScriptSearchScope.SOURCES | IJavaScriptSearchScope.APPLICATION_LIBRARIES | IJavaScriptSearchScope.SYSTEM_LIBRARIES));
		engine.setFiltering(false, fExcludeBinaries);
		engine.setGrouping(false);
		engine.searchPattern(new SubProgressMonitor(monitor, 1));
		final SearchMatch[] matches= (SearchMatch[]) engine.getResults();
		IFunction method= null;
		for (int index= 0; index < matches.length; index++) {
			method= (IFunction) matches[index].getElement();
			if (method != null)
				fDeclarations.add(method);
		}
	}

	private void createHierarchyOfDeclarations(IProgressMonitor pm, WorkingCopyOwner owner) throws JavaScriptModelException {
		IRegion region= JavaScriptCore.newRegion();
		for (Iterator iter= fDeclarations.iterator(); iter.hasNext();) {
			IType declaringType= ((IFunction) iter.next()).getDeclaringType();
			region.add(declaringType);
		}
		fHierarchy= JavaScriptCore.newTypeHierarchy(region, owner, pm);
	}
	
	private void createTypeToMethod() {
		fTypeToMethod= new HashMap();
		for (Iterator iter= fDeclarations.iterator(); iter.hasNext();) {
			IFunction declaration= (IFunction) iter.next();
			fTypeToMethod.put(declaration.getDeclaringType(), declaration);
		}
	}

	private void createUnionFind() throws JavaScriptModelException {
		fRootTypes= new HashSet(fTypeToMethod.keySet());
		fUnionFind= new UnionFind();
		for (Iterator iter= fTypeToMethod.keySet().iterator(); iter.hasNext();) {
			IType type= (IType) iter.next();
			fUnionFind.init(type);
		}
		for (Iterator iter= fTypeToMethod.keySet().iterator(); iter.hasNext();) {
			IType type= (IType) iter.next();
			uniteWithSupertypes(type, type);
		}
		fRootReps= new MultiMap();
		for (Iterator iter= fRootTypes.iterator(); iter.hasNext();) {
			IType type= (IType) iter.next();
			IType rep= fUnionFind.find(type);
			if (rep != null)
				fRootReps.put(rep, type);
		}
		fRootHierarchies= new HashMap();
	}

	private void uniteWithSupertypes(IType anchor, IType type) throws JavaScriptModelException {
		IType supertype = fHierarchy.getSuperclass(type);
		
		IType superRep= fUnionFind.find(supertype);
		if (superRep == null) {
			//Type doesn't declare method, but maybe supertypes?
			uniteWithSupertypes(anchor, supertype);
		} else {
			//check whether method in supertype is really overridden:
			IMember superMethod= (IMember) fTypeToMethod.get(supertype);
			if (JavaModelUtil.isVisibleInHierarchy(superMethod, anchor.getPackageFragment())) {
				IType rep= fUnionFind.find(anchor);
				fUnionFind.union(rep, superRep);
				// current type is no root anymore
				fRootTypes.remove(anchor);
				uniteWithSupertypes(supertype, supertype);
			} else {
				//Not overridden -> overriding chain ends here.
			}
		}
		
	}	
}
