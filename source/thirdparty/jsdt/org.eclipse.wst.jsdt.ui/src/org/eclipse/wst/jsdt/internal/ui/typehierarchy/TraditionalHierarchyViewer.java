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
package org.eclipse.wst.jsdt.internal.ui.typehierarchy;

import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;

/**
 * A TypeHierarchyViewer that looks like the type hierarchy view of VA/Java:
 * Starting form Object down to the element in focus, then all subclasses from
 * this element.
 * Used by the TypeHierarchyViewPart which has to provide a TypeHierarchyLifeCycle
 * on construction (shared type hierarchy)
 */
public class TraditionalHierarchyViewer extends TypeHierarchyViewer {	
	
	public TraditionalHierarchyViewer(Composite parent, TypeHierarchyLifeCycle lifeCycle, IWorkbenchPart part) {
		super(parent, new TraditionalHierarchyContentProvider(lifeCycle), lifeCycle, part);
	}
	
	/*
	 * @see TypeHierarchyViewer#getTitle
	 */	
	public String getTitle() {
		if (isMethodFiltering()) {
			return TypeHierarchyMessages.TraditionalHierarchyViewer_filtered_title; 
		} else {
			return TypeHierarchyMessages.TraditionalHierarchyViewer_title; 
		}
	}

	/*
	 * @see TypeHierarchyViewer#updateContent
	 */		
	public void updateContent(boolean expand) {
		getTree().setRedraw(false);
		refresh();
		
		if (expand) {
			TraditionalHierarchyContentProvider contentProvider= (TraditionalHierarchyContentProvider) getContentProvider();
			int expandLevel= contentProvider.getExpandLevel();
			if (isMethodFiltering()) {
				expandLevel++;
			}
			expandToLevel(expandLevel);
		}
		getTree().setRedraw(true);
	}	

	/**
	 * Content provider for the 'traditional' type hierarchy.
	 */	
	public static class TraditionalHierarchyContentProvider extends TypeHierarchyContentProvider {
		
			
		public TraditionalHierarchyContentProvider(TypeHierarchyLifeCycle provider) {
			super(provider);
		}
		
		public int getExpandLevel() {
			ITypeHierarchy hierarchy= getHierarchy();
			if (hierarchy != null) {
				IType input= hierarchy.getType();
				if (input != null) {
					return getDepth(hierarchy, input) + 2;
				} else {
					return 5;
				}
			}
			return 2;
		}
		
		private int getDepth(ITypeHierarchy hierarchy, IType input) {
			int count= 0;
			IType superType= hierarchy.getSuperclass(input);
			while (superType != null) {
				count++;
				superType= hierarchy.getSuperclass(superType);
			}
			return count;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.wst.jsdt.internal.ui.typehierarchy.TypeHierarchyContentProvider#getRootTypes(java.util.List)
		 */
		protected final void getRootTypes(List res) {
			ITypeHierarchy hierarchy= getHierarchy();
			if (hierarchy != null) {
				IType input= hierarchy.getType();
				if (input == null) {
					IType[] classes= hierarchy.getRootClasses();
					for (int i= 0; i < classes.length; i++) {
						res.add(classes[i]);
					}
				} else {
					IType[] roots= hierarchy.getRootClasses();
					for (int i= 0; i < roots.length; i++) {
						if (isObject(roots[i])) {
							res.add(roots[i]);
							return;
						}
					}
					res.addAll(Arrays.asList(roots)); // something wrong with the hierarchy
				}
			}
		}
				
		/*
		 * @see TypeHierarchyContentProvider.getTypesInHierarchy
		 */	
		protected final void getTypesInHierarchy(IType type, List res) {
			ITypeHierarchy hierarchy= getHierarchy();
			if (hierarchy != null) {
				IType[] types= hierarchy.getSubclasses(type);
				if (isObject(type)) {
					for (int i= 0; i < types.length; i++) {
						IType curr= types[i];
						res.add(curr);
					}
				} else {
					for (int i= 0; i < types.length; i++) {
						res.add(types[i]);
					}
					
				}
			}
		}

		protected IType getParentType(IType type) {
			ITypeHierarchy hierarchy= getHierarchy();
			if (hierarchy != null) {
				return hierarchy.getSuperclass(type);
			}
			return null;
		}	
			
	}
}
