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
package org.eclipse.wst.jsdt.ui.text.java;

import java.util.Comparator;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.jsdt.internal.ui.text.java.AbstractJavaCompletionProposal;
import org.eclipse.wst.jsdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.wst.jsdt.internal.ui.text.java.JavaTypeCompletionProposal;
import org.eclipse.wst.jsdt.internal.ui.text.java.LazyJavaCompletionProposal;
import org.eclipse.wst.jsdt.internal.ui.text.template.contentassist.TemplateProposal;

/**
 * Comparator for JavaScript completion proposals. Completion proposals can be
 * sorted by relevance or alphabetically.
 * <p>
 * Note: this comparator imposes orderings that are inconsistent with equals.
 * </p>
 * 
 *
 * Provisional API: This class/interface is part of an interim API that is
 * still under development and expected to change significantly before
 * reaching stability. It is being made available at this early stage to
 * solicit feedback from pioneering adopters on the understanding that any
 * code that uses this API will almost certainly be broken (repeatedly) as the
 * API evolves.
 */
public final class CompletionProposalComparator implements Comparator {

	private boolean fOrderAlphabetically;

	/**
	 * Creates a comparator that sorts by relevance.
	 */
	public CompletionProposalComparator() {
		fOrderAlphabetically = false;
	}

	/**
	 * Sets the sort order. Default is <code>false</code>, i.e. order by
	 * relevance.
	 *
	 * @param orderAlphabetically
	 *            <code>true</code> to order alphabetically,
	 *            <code>false</code> to order by relevance
	 */
	public void setOrderAlphabetically(boolean orderAlphabetically) {
		fOrderAlphabetically = orderAlphabetically;
	}

	/*
	 * @see Comparator#compare(Object, Object)
	 */
	public int compare(Object o1, Object o2) {
		{

			int a = 0;
			int b = 0;

			if (o1 instanceof JavaCompletionProposal) {
				a = 1;
			}

			if (o1 instanceof JavaTypeCompletionProposal) {
				a = 2;
			}

			if (o1 instanceof LazyJavaCompletionProposal) {
				a = 3;
			}

			if (o1 instanceof TemplateProposal) {
				a = 4;
			}

			if (o2 instanceof JavaCompletionProposal) {
				b = 1;
			}

			if (o2 instanceof JavaTypeCompletionProposal) {
				b = 2;
			}

			if (o2 instanceof LazyJavaCompletionProposal) {
				b = 3;
			}

			if (o2 instanceof TemplateProposal) {
				b = 4;
			}

			if (a != b) {
				return a < b ? -1 : 1;
			}
			// --
		}


		ICompletionProposal p1 = (ICompletionProposal) o1;
		ICompletionProposal p2 = (ICompletionProposal) o2;

		if (!fOrderAlphabetically) {
			int r1 = getRelevance(p1);
			int r2 = getRelevance(p2);
			int relevanceDif = r2 - r1;
			if (relevanceDif != 0) {
				return relevanceDif;
			}
		}
		/*
		 * TODO the correct (but possibly much slower) sorting would use a
		 * collator.
		 */
		// fix for bug 67468
		return getSortKey(p1).compareToIgnoreCase(getSortKey(p2));
	}

	private String getSortKey(ICompletionProposal p) {
		if (p instanceof AbstractJavaCompletionProposal)
			return ((AbstractJavaCompletionProposal) p).getSortString();
		return p.getDisplayString();
	}

	private int getRelevance(ICompletionProposal obj) {
		if (obj instanceof IJavaCompletionProposal) {
			IJavaCompletionProposal jcp = (IJavaCompletionProposal) obj;
			return jcp.getRelevance();
		}
		else if (obj instanceof TemplateProposal) {
			TemplateProposal tp = (TemplateProposal) obj;
			return tp.getRelevance();
		}
		// catch all
		return 0;
	}

}
