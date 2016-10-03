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
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;


/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class ReplaceRewrite {
	
	protected ASTRewrite fRewrite;
	protected ASTNode[] fToReplace;
	protected StructuralPropertyDescriptor fDescriptor;
	
	public static ReplaceRewrite create(ASTRewrite rewrite, ASTNode[] nodes) {
		return new ReplaceRewrite(rewrite, nodes);
	}
	
	protected ReplaceRewrite(ASTRewrite rewrite, ASTNode[] nodes) {
		Assert.isNotNull(rewrite);
		Assert.isNotNull(nodes);
		Assert.isTrue(nodes.length > 0);
		fRewrite= rewrite;
		fToReplace= nodes;
		fDescriptor= fToReplace[0].getLocationInParent();
		if (nodes.length > 1) {
			Assert.isTrue(fDescriptor instanceof ChildListPropertyDescriptor);
		}
	}
	
	public void replace(ASTNode[] replacements, TextEditGroup description) {
		if (fToReplace.length == 1) {
			if (replacements.length == 1) {
				handleOneOne(replacements, description);
			} else {
				handleOneMany(replacements, description);
			}
		} else {
			handleManyMany(replacements, description);
		}
	}

	protected void handleOneOne(ASTNode[] replacements, TextEditGroup description) {
		fRewrite.replace(fToReplace[0], replacements[0], description);
	}

	protected void handleOneMany(ASTNode[] replacements, TextEditGroup description) {
		handleManyMany(replacements, description);
	}
	
	protected void handleManyMany(ASTNode[] replacements, TextEditGroup description) {
		ListRewrite container= fRewrite.getListRewrite(fToReplace[0].getParent(), (ChildListPropertyDescriptor)fDescriptor);
		if (fToReplace.length == replacements.length) {
			for (int i= 0; i < fToReplace.length; i++) {
				container.replace(fToReplace[i], replacements[i], description);
			}
		} else if (fToReplace.length < replacements.length) {
			for (int i= 0; i < fToReplace.length; i++) {
				container.replace(fToReplace[i], replacements[i], description);
			}
			for (int i= fToReplace.length; i < replacements.length; i++) {
				container.insertAfter(replacements[i], replacements[i - 1], description);
			}
		} else if (fToReplace.length > replacements.length) {
			int delta= fToReplace.length - replacements.length;
			for(int i= 0; i < delta; i++) {
				container.remove(fToReplace[i], description);
			}
			for (int i= delta, r= 0; i < fToReplace.length; i++, r++) {
				container.replace(fToReplace[i], replacements[r], description);
			}
		}
	}
}
