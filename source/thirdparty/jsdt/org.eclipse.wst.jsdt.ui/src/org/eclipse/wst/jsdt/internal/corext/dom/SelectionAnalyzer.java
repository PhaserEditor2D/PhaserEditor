/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.Expression;


/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class SelectionAnalyzer extends GenericVisitor {
	
	private Selection fSelection;
	private boolean fTraverseSelectedNode;
	private ASTNode fLastCoveringNode;
	
	// Selected nodes
	private List fSelectedNodes;
	
	public SelectionAnalyzer(Selection selection, boolean traverseSelectedNode) {
		super(true);
		Assert.isNotNull(selection);
		fSelection= selection;
		fTraverseSelectedNode= traverseSelectedNode;
	}
	
	public boolean hasSelectedNodes() {
		return fSelectedNodes != null && !fSelectedNodes.isEmpty();
	}
	
	public ASTNode[] getSelectedNodes() {
		if (fSelectedNodes == null || fSelectedNodes.isEmpty())
			return new ASTNode[0];
		return (ASTNode[]) fSelectedNodes.toArray(new ASTNode[fSelectedNodes.size()]);
	}
	
	public ASTNode getFirstSelectedNode() {
		if (fSelectedNodes == null || fSelectedNodes.isEmpty())
			return null;
		return (ASTNode)fSelectedNodes.get(0);
	}
	
	public ASTNode getLastSelectedNode() {
		if (fSelectedNodes == null || fSelectedNodes.isEmpty())
			return null;
		return (ASTNode)fSelectedNodes.get(fSelectedNodes.size() - 1);
	}
	
	public boolean isExpressionSelected() {
		if (!hasSelectedNodes())
			return false;
		return fSelectedNodes.get(0) instanceof Expression;
	}
	
	public IRegion getSelectedNodeRange() {
		if (fSelectedNodes == null || fSelectedNodes.isEmpty())
			return null;
		ASTNode firstNode= (ASTNode)fSelectedNodes.get(0);
		ASTNode lastNode= (ASTNode)fSelectedNodes.get(fSelectedNodes.size() - 1);
		int start= firstNode.getStartPosition();
		return new Region(start, lastNode.getStartPosition() + lastNode.getLength() - start);
	}
	
	public ASTNode getLastCoveringNode() {
		return fLastCoveringNode;
	}
	
	protected Selection getSelection() {
		return fSelection;
	}
	
	//--- node management ---------------------------------------------------------
	
	protected boolean visitNode(ASTNode node) {
		// The selection lies behind the node.
		if (fSelection.liesOutside(node)) {
			return false;
		} else if (fSelection.covers(node)) {
			if (isFirstNode()) {
				handleFirstSelectedNode(node);
			} else {
				handleNextSelectedNode(node);
			}
			return fTraverseSelectedNode;
		} else if (fSelection.coveredBy(node)) {
			fLastCoveringNode= node;
			return true;
		} else if (fSelection.endsIn(node)) {
			return handleSelectionEndsIn(node);
		}
		// There is a possibility that the user has selected trailing semicolons that don't belong
		// to the statement. So dive into it to check if sub nodes are fully covered.
		return true;
	}
	
	protected void reset() {
		fSelectedNodes= null;
	}
	
	protected void handleFirstSelectedNode(ASTNode node) {
		fSelectedNodes= new ArrayList(5);
		fSelectedNodes.add(node);
	}
	
	protected void handleNextSelectedNode(ASTNode node) {
		if (getFirstSelectedNode().getParent() == node.getParent()) {
			fSelectedNodes.add(node);
		}
	}

	protected boolean handleSelectionEndsIn(ASTNode node) {
		return false;
	}
	
	protected List internalGetSelectedNodes() {
		return fSelectedNodes;
	}
	
	private boolean isFirstNode() {
		return fSelectedNodes == null;
	}	
}
