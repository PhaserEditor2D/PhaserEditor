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

import org.eclipse.wst.jsdt.core.dom.ASTNode;

/**
 * @see org.eclipse.wst.jsdt.internal.corext.dom.fragments.IASTFragment
 * @see org.eclipse.wst.jsdt.internal.corext.dom.fragments.ASTFragmentFactory
 */
abstract class ASTFragment implements IASTFragment {

	/**
	 * Tries to create or find as many fragments as possible
	 * such that each fragment f matches
	 * this fragment and f.getNode() is <code>node</code>
	 */
	abstract IASTFragment[] getMatchingFragmentsWithNode(ASTNode node);
}

