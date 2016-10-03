/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.corext.refactoring.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.rewrite.TargetSourceRangeComputer;

public class TightSourceRangeComputer extends TargetSourceRangeComputer {
	private HashSet/*<ASTNode>*/ fTightSourceRangeNodes= new HashSet();
	
	public void addTightSourceNode(ASTNode reference) {
		fTightSourceRangeNodes.add(reference);
		
	    List properties= reference.structuralPropertiesForType();
	    for (Iterator iterator= properties.iterator(); iterator.hasNext();) {
	        StructuralPropertyDescriptor descriptor= (StructuralPropertyDescriptor)iterator.next();
	        if (descriptor.isChildProperty()) {
	        	ASTNode child= (ASTNode)reference.getStructuralProperty(descriptor);
	        	if (isExtending(child, reference)) {
	        		addTightSourceNode(child);
	        	}
	        } else if (descriptor.isChildListProperty()) {
	        	List childs= (List)reference.getStructuralProperty(descriptor);
	        	for (Iterator iterator2= childs.iterator(); iterator2.hasNext();) {
	                ASTNode child= (ASTNode)iterator2.next();
	                if (isExtending(child, reference)) {
		        		addTightSourceNode(child);
		        	}	                
                }
	        }
        }
    }

	public SourceRange computeSourceRange(ASTNode node) {
		if (fTightSourceRangeNodes.contains(node)) {
			return new TargetSourceRangeComputer.SourceRange(node.getStartPosition(), node.getLength());
		} else {
			return super.computeSourceRange(node); // see bug 85850
		}
	}
	
	private boolean isExtending(ASTNode child, ASTNode parent) {
	    SourceRange extendedRange= super.computeSourceRange(child);
	    
	    int parentStart= parent.getStartPosition();
		int extendedStart= extendedRange.getStartPosition();
		if (parentStart > extendedStart)
			return true;
		
		int parentEnd= parentStart + parent.getLength();
		int extendedEnd= extendedStart + extendedRange.getLength();
		if (parentEnd < extendedEnd)
			return true;

	    return false;
    }
}