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
package org.eclipse.wst.jsdt.internal.corext.dom.fragments;

import org.eclipse.core.runtime.Assert;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.JdtASTMatcher;

class SimpleFragment extends ASTFragment {
	private final ASTNode fNode;

	SimpleFragment(ASTNode node) {
		Assert.isNotNull(node);
		fNode= node;
	}

	public IASTFragment[] getMatchingFragmentsWithNode(ASTNode node) {
		if (! JdtASTMatcher.doNodesMatch(getAssociatedNode(), node))
			return new IASTFragment[0];

		IASTFragment match= ASTFragmentFactory.createFragmentForFullSubtree(node);
		Assert.isTrue(match.matches(this) || this.matches(match));
		return new IASTFragment[] { match };
	}

	public boolean matches(IASTFragment other) {
		return other.getClass().equals(getClass()) && JdtASTMatcher.doNodesMatch(other.getAssociatedNode(), getAssociatedNode());
	}

	public IASTFragment[] getSubFragmentsMatching(IASTFragment toMatch) {
		return ASTMatchingFragmentFinder.findMatchingFragments(getAssociatedNode(), (ASTFragment) toMatch);
	}

	public int getStartPosition() {
		return fNode.getStartPosition();
	}

	public int getLength() {
		return fNode.getLength();
	}

	public ASTNode getAssociatedNode() {
		return fNode;
	}
	
	public void replace(ASTRewrite rewrite, ASTNode replacement, TextEditGroup textEditGroup) {
		rewrite.replace(fNode, replacement, textEditGroup);
	}

	public int hashCode() {
		return fNode.hashCode();
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimpleFragment other= (SimpleFragment) obj;
		return fNode.equals(other.fNode);
	}
	
}
