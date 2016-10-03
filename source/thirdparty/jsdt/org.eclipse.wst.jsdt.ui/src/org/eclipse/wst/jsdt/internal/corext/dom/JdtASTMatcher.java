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

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.dom.ASTMatcher;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class JdtASTMatcher extends ASTMatcher {

	public boolean match(SimpleName node, Object other) {
		boolean isomorphic= super.match(node, other);
		if (! isomorphic || !(other instanceof SimpleName))
			return false;
		SimpleName name= (SimpleName)other;
		IBinding nodeBinding= node.resolveBinding();
		IBinding otherBinding= name.resolveBinding();
		if (nodeBinding == null) {
			if (otherBinding != null) {
				return false;
			}
		} else {
			if (nodeBinding != otherBinding) {
				return false;
			}
		}
		if (node.resolveTypeBinding() != name.resolveTypeBinding())
			return false;
		return true;	
	}
	
	public static boolean doNodesMatch(ASTNode one, ASTNode other) {
		Assert.isNotNull(one);
		Assert.isNotNull(other);
		
		return one.subtreeMatch(new JdtASTMatcher(), other);
	}
}
