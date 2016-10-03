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
package org.eclipse.wst.jsdt.internal.corext.refactoring.reorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;


public class SourceReferenceUtil {
	
	//no instances
	private SourceReferenceUtil(){}
	
	public static IFile getFile(ISourceReference ref) {
		IJavaScriptUnit unit= getCompilationUnit(ref);
		return (IFile) unit.getPrimary().getResource();
	}
	
	public static IJavaScriptUnit getCompilationUnit(ISourceReference o){
		Assert.isTrue(! (o instanceof IClassFile));
		
		if (o instanceof IJavaScriptUnit)
			return (IJavaScriptUnit)o;
		if (o instanceof IJavaScriptElement)
			return (IJavaScriptUnit) ((IJavaScriptElement)o).getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
		return null;
	}	
	
	private static boolean hasParentInSet(IJavaScriptElement elem, Set set){
		IJavaScriptElement parent= elem.getParent();
		while (parent != null) {
			if (set.contains(parent))	
				return true;
			parent= parent.getParent();	
		}
		return false;
	}
	
	public static ISourceReference[] removeAllWithParentsSelected(ISourceReference[] elems){
		Set set= new HashSet(Arrays.asList(elems));
		List result= new ArrayList(elems.length);
		for (int i= 0; i < elems.length; i++) {
			ISourceReference elem= elems[i];
			if (! (elem instanceof IJavaScriptElement))
				result.add(elem);
			else{	
				if (! hasParentInSet(((IJavaScriptElement)elem), set))
					result.add(elem);
			}	
		}
		return (ISourceReference[]) result.toArray(new ISourceReference[result.size()]);
	}
	
	/**
	 * @return IFile -> List of ISourceReference (elements from that file)	
	 */
	public static Map groupByFile(ISourceReference[] elems) {
		Map map= new HashMap();
		for (int i= 0; i < elems.length; i++) {
			ISourceReference elem= elems[i];
			IFile file= SourceReferenceUtil.getFile(elem);
			if (! map.containsKey(file))
				map.put(file, new ArrayList());
			((List)map.get(file)).add(elem);
		}
		return map;
	}	
	
	public static ISourceReference[] sortByOffset(ISourceReference[] methods){
		Arrays.sort(methods, new Comparator(){
			public int compare(Object o1, Object o2){
				try{
					return ((ISourceReference)o2).getSourceRange().getOffset() - ((ISourceReference)o1).getSourceRange().getOffset();
				} catch (JavaScriptModelException e){
					return o2.hashCode() - o1.hashCode();
				}	
			}
		});
		return methods;
	}
}

