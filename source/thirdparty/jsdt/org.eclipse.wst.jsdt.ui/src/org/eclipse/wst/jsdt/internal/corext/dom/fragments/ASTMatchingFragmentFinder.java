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
package org.eclipse.wst.jsdt.internal.corext.dom.fragments;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.internal.corext.dom.GenericVisitor;

class ASTMatchingFragmentFinder extends GenericVisitor {

	public static IASTFragment[] findMatchingFragments(ASTNode scope, ASTFragment toMatch) {
		return new ASTMatchingFragmentFinder(toMatch).findMatches(scope);
	}

	private ASTFragment fFragmentToMatch;
	private Set fMatches= new HashSet();

	private ASTMatchingFragmentFinder(ASTFragment toMatch) {
		super(true);
		fFragmentToMatch= toMatch;	
	}
	private IASTFragment[] findMatches(ASTNode scope) {
		fMatches.clear();
		scope.accept(this);
		return getMatches();
	}
	private IASTFragment[] getMatches() {
		return (IASTFragment[]) fMatches.toArray(new IASTFragment[fMatches.size()]);
	}
	
	public boolean visit(JSdoc node) {
		return false;
	}
	
	protected boolean visitNode(ASTNode node) {
		IASTFragment[] localMatches= fFragmentToMatch.getMatchingFragmentsWithNode(node);
		for(int i= 0; i < localMatches.length; i++) {
			fMatches.add(localMatches[i]);	
		}
		return true;
	}

}
