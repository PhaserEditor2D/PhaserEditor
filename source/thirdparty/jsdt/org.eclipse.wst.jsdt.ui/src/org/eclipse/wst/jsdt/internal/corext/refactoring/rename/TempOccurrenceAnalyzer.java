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

package org.eclipse.wst.jsdt.internal.corext.refactoring.rename;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;


public class TempOccurrenceAnalyzer extends ASTVisitor {
	/** Set of SimpleName */
	private Set fReferenceNodes;
	/** Set of SimpleName */
	private Set fJavadocNodes;

	private VariableDeclaration fTempDeclaration;
	private IBinding fTempBinding;
	private boolean fAnalyzeJavadoc;
	
	private boolean fIsInJavadoc;
	
	public TempOccurrenceAnalyzer(VariableDeclaration tempDeclaration, boolean analyzeJavadoc){
		Assert.isNotNull(tempDeclaration);
		fReferenceNodes= new HashSet();
		fJavadocNodes= new HashSet();
		fAnalyzeJavadoc= analyzeJavadoc;
		fTempDeclaration= tempDeclaration;
		fTempBinding= tempDeclaration.resolveBinding();
		fIsInJavadoc= false;
	}
	
	public void perform() {
		ASTNode cuNode= ASTNodes.getParent(fTempDeclaration, JavaScriptUnit.class);
		cuNode.accept(this);
	}
	
	public int[] getReferenceOffsets(){
		int[] offsets= new int[fReferenceNodes.size()];
		addOffsets(offsets, 0, fReferenceNodes);
		return offsets;
	}
	
	public int[] getReferenceAndJavadocOffsets(){
		int[] offsets= new int[fReferenceNodes.size() + fJavadocNodes.size()];
		addOffsets(offsets, 0, fReferenceNodes);
		addOffsets(offsets, fReferenceNodes.size(), fJavadocNodes);
		return offsets;
	}
	
	private void addOffsets(int[] offsets, int start, Set nodeSet) {
		int i= start;
		for (Iterator iter= nodeSet.iterator(); iter.hasNext(); i++) {
			ASTNode node= (ASTNode) iter.next();
			offsets[i]= node.getStartPosition();
		}
	}
	
	public int getNumberOfReferences() {
		return fReferenceNodes.size();
	}

	public SimpleName[] getReferenceNodes() {
		return (SimpleName[]) fReferenceNodes.toArray(new SimpleName[fReferenceNodes.size()]);
	}
	
	public SimpleName[] getJavadocNodes() {
		return (SimpleName[]) fJavadocNodes.toArray(new SimpleName[fJavadocNodes.size()]);
	}
	
	public SimpleName[] getReferenceAndDeclarationNodes() {
		SimpleName[] nodes= (SimpleName[]) fReferenceNodes.toArray(new SimpleName[fReferenceNodes.size() + 1]);
		nodes[fReferenceNodes.size()]= fTempDeclaration.getName();
		return nodes;
	}
			
	//------- visit ------ (don't call)
	
	public boolean visit(JSdoc node) {
		if (fAnalyzeJavadoc)
			fIsInJavadoc= true;
		return fAnalyzeJavadoc;
	}
		
	public void endVisit(JSdoc node) {
		fIsInJavadoc= false;
	}
	
	public boolean visit(SimpleName node){
		if (node.getParent() instanceof VariableDeclaration){
			if (((VariableDeclaration)node.getParent()).getName() == node)
				return true; //don't include declaration
		}
		
		if (fTempBinding != null && fTempBinding == node.resolveBinding()) {
			if (fIsInJavadoc)
				fJavadocNodes.add(node);
			else
				fReferenceNodes.add(node);
		}
				
		return true;
	}
}
