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
package org.eclipse.wst.jsdt.internal.ui.viewsupport;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;


/**
 *  Viewer sorter which sorts the Java elements like
 *  they appear in the source.
 * 
 * 
 */
public class SourcePositionComparator extends ViewerComparator {

	/*
	 * @see org.eclipse.jface.viewers.ViewerSorter#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (!(e1 instanceof ISourceReference))
			return 0;
		if (!(e2 instanceof ISourceReference))
			return 0;
		
		IJavaScriptElement parent1= ((IJavaScriptElement)e1).getParent();
		if (parent1 == null || !parent1.equals(((IJavaScriptElement)e2).getParent())) {
				IType t1= getOutermostDeclaringType(e1);
				if (t1 == null)
					return 0;
				
				IType t2= getOutermostDeclaringType(e2);
				try {
					if (!t1.equals(t2)) {
						if (t2 == null)
							return 0;

						if (Flags.isPublic(t1.getFlags()) && Flags.isPublic(t2.getFlags()))
							return 0;

						if (!t1.getPackageFragment().equals(t2.getPackageFragment()))
							return 0;

						IJavaScriptUnit cu1= (IJavaScriptUnit)((IJavaScriptElement)e1).getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
						if (cu1 != null) {
							if (!cu1.equals(((IJavaScriptElement)e2).getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT)))
								return 0;
						} else {
							IClassFile cf1= (IClassFile)((IJavaScriptElement)e1).getAncestor(IJavaScriptElement.CLASS_FILE);
							if (cf1 == null)
								return 0;
							IClassFile cf2= (IClassFile)((IJavaScriptElement)e2).getAncestor(IJavaScriptElement.CLASS_FILE);
							String source1= cf1.getSource();
							if (source1 != null && !source1.equals(cf2.getSource()))
								return 0;
						}
					}
				} catch (JavaScriptModelException e3) {
					return 0;
				}
		}
		
		try {
			ISourceRange sr1= ((ISourceReference)e1).getSourceRange();
			ISourceRange sr2= ((ISourceReference)e2).getSourceRange();
			if (sr1 == null || sr2 == null)
				return 0;
			
			return sr1.getOffset() - sr2.getOffset();
			
		} catch (JavaScriptModelException e) {
			return 0;
		}
	}

	private IType getOutermostDeclaringType(Object element) {
		if (!(element instanceof IMember))
			return null;
		
		IType declaringType;
		if (element instanceof IType)
			declaringType= (IType)element;
		else {
			declaringType= ((IMember)element).getDeclaringType();
			if (declaringType == null)
				return null;
		}
		
		IType declaringTypeDeclaringType= declaringType.getDeclaringType();
		while (declaringTypeDeclaringType != null) {
			declaringType= declaringTypeDeclaringType;
			declaringTypeDeclaringType= declaringType.getDeclaringType();
		}
		return declaringType;
	}
}
