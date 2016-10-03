/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.typehierarchy;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.wst.jsdt.core.ElementChangedEvent;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IElementChangedListener;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptElementDelta;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IRegion;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.ITypeHierarchyChangedListener;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

/**
 * Manages a type hierarchy, to keep it refreshed, and to allow it to be shared.
 */
public class TypeHierarchyLifeCycle implements ITypeHierarchyChangedListener, IElementChangedListener {
	
	private boolean fHierarchyRefreshNeeded;
	private ITypeHierarchy fHierarchy;
	private IJavaScriptElement fInputElement;
	private boolean fIsSuperTypesOnly;
	
	private List fChangeListeners;
	
	public TypeHierarchyLifeCycle() {
		this(false);
	}	
	
	public TypeHierarchyLifeCycle(boolean isSuperTypesOnly) {
		fHierarchy= null;
		fInputElement= null;
		fIsSuperTypesOnly= isSuperTypesOnly;
		fChangeListeners= new ArrayList(2);
	}
	
	public ITypeHierarchy getHierarchy() {
		return fHierarchy;
	}
	
	public IJavaScriptElement getInputElement() {
		return fInputElement;
	}
	
	
	public void freeHierarchy() {
		if (fHierarchy != null) {
			fHierarchy.removeTypeHierarchyChangedListener(this);
			JavaScriptCore.removeElementChangedListener(this);
			fHierarchy= null;
			fInputElement= null;
		}
	}
	
	public void removeChangedListener(ITypeHierarchyLifeCycleListener listener) {
		fChangeListeners.remove(listener);
	}
	
	public void addChangedListener(ITypeHierarchyLifeCycleListener listener) {
		if (!fChangeListeners.contains(listener)) {
			fChangeListeners.add(listener);
		}
	}
	
	private void fireChange(IType[] changedTypes) {
		for (int i= fChangeListeners.size()-1; i>=0; i--) {
			ITypeHierarchyLifeCycleListener curr= (ITypeHierarchyLifeCycleListener) fChangeListeners.get(i);
			curr.typeHierarchyChanged(this, changedTypes);
		}
	}
			
	public void ensureRefreshedTypeHierarchy(final IJavaScriptElement element, IRunnableContext context) throws InvocationTargetException, InterruptedException {
		if (element == null || !element.exists()) {
			freeHierarchy();
			return;
		}
		boolean hierachyCreationNeeded= (fHierarchy == null || !element.equals(fInputElement));
		
		if (hierachyCreationNeeded || fHierarchyRefreshNeeded) {
			
			IRunnableWithProgress op= new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) throws InvocationTargetException, InterruptedException {
					try {
						doHierarchyRefresh(element, pm);
					} catch (JavaScriptModelException e) {
						throw new InvocationTargetException(e);
					} catch (OperationCanceledException e) {
						throw new InterruptedException();
					}
				}
			};
			fHierarchyRefreshNeeded= true;
			context.run(true, true, op);
			fHierarchyRefreshNeeded= false;
		}
	}
	
	private ITypeHierarchy createTypeHierarchy(IJavaScriptElement element, IProgressMonitor pm) throws JavaScriptModelException {
		if (element.getElementType() == IJavaScriptElement.TYPE) {
			IType type= (IType) element;
			if (fIsSuperTypesOnly) {
				return type.newSupertypeHierarchy(pm);
			} else {
				return type.newTypeHierarchy(pm);
			}
		} else {
			IRegion region= JavaScriptCore.newRegion();
			if (element.getElementType() == IJavaScriptElement.JAVASCRIPT_PROJECT) {
				// for projects only add the contained source folders
				IPackageFragmentRoot[] roots= ((IJavaScriptProject) element).getPackageFragmentRoots();
				for (int i= 0; i < roots.length; i++) {
					if (!roots[i].isExternal()) {
						region.add(roots[i]);
					}
				}
			} else if (element.getElementType() == IJavaScriptElement.PACKAGE_FRAGMENT) {
				IPackageFragmentRoot[] roots= element.getJavaScriptProject().getPackageFragmentRoots();
				String name= element.getElementName();
				for (int i= 0; i < roots.length; i++) {
					IPackageFragment pack= roots[i].getPackageFragment(name);
					if (pack.exists()) {
						region.add(pack);
					}
				}
			} else {
				region.add(element);
			}
			IJavaScriptProject jproject= element.getJavaScriptProject();
			return jproject.newTypeHierarchy(region, pm);
		}
	}
	
	
	public synchronized void doHierarchyRefresh(IJavaScriptElement element, IProgressMonitor pm) throws JavaScriptModelException {
		boolean hierachyCreationNeeded= (fHierarchy == null || !element.equals(fInputElement));
		// to ensure the order of the two listeners always remove / add listeners on operations
		// on type hierarchies
		if (fHierarchy != null) {
			fHierarchy.removeTypeHierarchyChangedListener(this);
			JavaScriptCore.removeElementChangedListener(this);
		}
		if (hierachyCreationNeeded) {
			fHierarchy= createTypeHierarchy(element, pm);
			if (pm != null && pm.isCanceled()) {
				throw new OperationCanceledException();
			}
			fInputElement= element;
		} else {
			fHierarchy.refresh(pm);
		}
		fHierarchy.addTypeHierarchyChangedListener(this);
		JavaScriptCore.addElementChangedListener(this);
		fHierarchyRefreshNeeded= false;
	}		
	
	/*
	 * @see ITypeHierarchyChangedListener#typeHierarchyChanged
	 */
	public void typeHierarchyChanged(ITypeHierarchy typeHierarchy) {
	 	fHierarchyRefreshNeeded= true;
 		fireChange(null);
	}		

	/*
	 * @see IElementChangedListener#elementChanged(ElementChangedEvent)
	 */
	public void elementChanged(ElementChangedEvent event) {
		if (fChangeListeners.isEmpty()) {
			return;
		}
		
		if (fHierarchyRefreshNeeded) {
			return;
		} else {
			ArrayList changedTypes= new ArrayList();
			processDelta(event.getDelta(), changedTypes);
			if (changedTypes.size() > 0) {
				fireChange((IType[]) changedTypes.toArray(new IType[changedTypes.size()]));
			}
		}
	}
	
	/*
	 * Assume that the hierarchy is intact (no refresh needed)
	 */					
	private void processDelta(IJavaScriptElementDelta delta, ArrayList changedTypes) {
		IJavaScriptElement element= delta.getElement();
		switch (element.getElementType()) {
			case IJavaScriptElement.TYPE:
				processTypeDelta((IType) element, changedTypes);
				processChildrenDelta(delta, changedTypes); // (inner types)
				break;
			case IJavaScriptElement.JAVASCRIPT_MODEL:
			case IJavaScriptElement.JAVASCRIPT_PROJECT:
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
			case IJavaScriptElement.PACKAGE_FRAGMENT:
				processChildrenDelta(delta, changedTypes);
				break;
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				IJavaScriptUnit cu= (IJavaScriptUnit)element;
				if (!JavaModelUtil.isPrimary(cu)) {
					return;
				}
				
				if (delta.getKind() == IJavaScriptElementDelta.CHANGED && isPossibleStructuralChange(delta.getFlags())) {
					try {
						if (cu.exists()) {
							IType[] types= cu.getAllTypes();
							for (int i= 0; i < types.length; i++) {
								processTypeDelta(types[i], changedTypes);
							}
						}
					} catch (JavaScriptModelException e) {
						JavaScriptPlugin.log(e);
					}
				} else {
					processChildrenDelta(delta, changedTypes);
				}
				break;
			case IJavaScriptElement.CLASS_FILE:	
				if (delta.getKind() == IJavaScriptElementDelta.CHANGED) {
					IType type= ((IClassFile) element).getType();
					processTypeDelta(type, changedTypes);
				} else {
					processChildrenDelta(delta, changedTypes);
				}
				break;				
		}
	}
	
	private boolean isPossibleStructuralChange(int flags) {
		return (flags & (IJavaScriptElementDelta.F_CONTENT | IJavaScriptElementDelta.F_FINE_GRAINED)) == IJavaScriptElementDelta.F_CONTENT;
	}
	
	private void processTypeDelta(IType type, ArrayList changedTypes) {
		if (getHierarchy().contains(type)) {
			changedTypes.add(type);
		}
	}
	
	private void processChildrenDelta(IJavaScriptElementDelta delta, ArrayList changedTypes) {
		IJavaScriptElementDelta[] children= delta.getAffectedChildren();
		for (int i= 0; i < children.length; i++) {
			processDelta(children[i], changedTypes); // recursive
		}
	}
	

}
