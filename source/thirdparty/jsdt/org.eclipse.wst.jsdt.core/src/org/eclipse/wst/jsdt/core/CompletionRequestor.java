/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core;

import org.eclipse.wst.jsdt.core.compiler.IProblem;

/**
 * Abstract base class for a completion requestor which is passed completion
 * proposals as they are generated in response to a code assist request.
 * <p>
 * This class is intended to be subclassed by clients.
 * </p>
 * <p>
 * The code assist engine normally invokes methods on completion
 * requestor in the following sequence:
 * <pre>
 * requestor.beginReporting();
 * requestor.acceptContext(context);
 * requestor.accept(proposal_1);
 * requestor.accept(proposal_2);
 * ...
 * requestor.endReporting();
 * </pre>
 * If, however, the engine is unable to offer completion proposals
 * for whatever reason, <code>completionFailure</code> is called
 * with a problem object describing why completions were unavailable.
 * In this case, the sequence of calls is:
 * <pre>
 * requestor.beginReporting();
 * requestor.acceptContext(context);
 * requestor.completionFailure(problem);
 * requestor.endReporting();
 * </pre>
 * In either case, the bracketing <code>beginReporting</code>
 * <code>endReporting</code> calls are always made as well as
 * <code>acceptContext</code> call.
 * </p>
 *
 * @see ICodeAssist
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public abstract class CompletionRequestor {

	/**
	 * The set of CompletionProposal kinds that this requestor
	 * ignores; <code>0</code> means the set is empty.
	 * 1 << completionProposalKind
	 */
	private int ignoreSet = 0;

	private String[] favoriteReferences;

	/**
	 * The set of CompletionProposal kinds that this requestor
	 * allows for required proposals; <code>0</code> means the set is empty.
	 * 1 << completionProposalKind
	 */
	private int requiredProposalAllowSet[] = null;

	/**
	 * Creates a new completion requestor.
	 * The requestor is interested in all kinds of completion
	 * proposals; none will be ignored.
	 */
	public CompletionRequestor() {
		// do nothing
	}

	/**
	 * Returns whether the given kind of completion proposal is ignored.
	 *
	 * @param completionProposalKind one of the kind constants declared
	 * on <code>CompletionProposal</code>
	 * @return <code>true</code> if the given kind of completion proposal
	 * is ignored by this requestor, and <code>false</code> if it is of
	 * interest
	 * @see #setIgnored(int, boolean)
	 * @see CompletionProposal#getKind()
	 */
	public boolean isIgnored(int completionProposalKind) {
		if (completionProposalKind < CompletionProposal.FIRST_KIND
			|| completionProposalKind > CompletionProposal.LAST_KIND) {
				throw new IllegalArgumentException("Unknown kind of completion proposal: "+completionProposalKind); //$NON-NLS-1$
		}
		return 0 != (this.ignoreSet & (1 << completionProposalKind));
	}

	/**
	 * Sets whether the given kind of completion proposal is ignored.
	 *
	 * @param completionProposalKind one of the kind constants declared
	 * on <code>CompletionProposal</code>
	 * @param ignore <code>true</code> if the given kind of completion proposal
	 * is ignored by this requestor, and <code>false</code> if it is of
	 * interest
	 * @see #isIgnored(int)
	 * @see CompletionProposal#getKind()
	 */
	public void setIgnored(int completionProposalKind, boolean ignore) {
		if (completionProposalKind < CompletionProposal.FIRST_KIND
			|| completionProposalKind > CompletionProposal.LAST_KIND) {
				throw new IllegalArgumentException("Unknown kind of completion proposal: "+completionProposalKind); //$NON-NLS-1$
		}
		if (ignore) {
			this.ignoreSet |= (1 << completionProposalKind);
		} else {
			this.ignoreSet &= ~(1 << completionProposalKind);
		}
	}

	/**
	 * Returns whether a proposal of a given kind with a required proposal
	 * of the given kind is allowed.
	 *
	 * @param proposalKind one of the kind constants declared
	 * @param requiredProposalKind one of the kind constants declared
	 * on <code>CompletionProposal</code>
	 * @return <code>true</code> if a proposal of a given kind with a required proposal
	 * of the given kind is allowed by this requestor, and <code>false</code>
	 * if it isn't of interest.
	 * <p>
	 * By default, all kinds of required proposals aren't allowed.
	 * </p>
	 * @see #setAllowsRequiredProposals(int, int, boolean)
	 * @see CompletionProposal#getKind()
	 * @see CompletionProposal#getRequiredProposals()
	 *
	 */
	public boolean isAllowingRequiredProposals(int proposalKind, int requiredProposalKind) {
		if (proposalKind < CompletionProposal.FIRST_KIND
			|| proposalKind > CompletionProposal.LAST_KIND) {
				throw new IllegalArgumentException("Unknown kind of completion proposal: "+requiredProposalKind); //$NON-NLS-1$
			}

		if (requiredProposalKind < CompletionProposal.FIRST_KIND
			|| requiredProposalKind > CompletionProposal.LAST_KIND) {
				throw new IllegalArgumentException("Unknown required kind of completion proposal: "+requiredProposalKind); //$NON-NLS-1$
		}
		if (this.requiredProposalAllowSet == null) return false;

		return 0 != (this.requiredProposalAllowSet[proposalKind] & (1 << requiredProposalKind));
	}

	/**
	 * Sets whether a proposal of a given kind with a required proposal
	 * of the given kind is allowed.
	 *
	 * Currently only a subset of kinds support required proposals. To see what combinations
	 * are supported you must look at {@link CompletionProposal#getRequiredProposals()}
	 * documentation.
	 *
	 * @param proposalKind one of the kind constants declared
	 * @param requiredProposalKind one of the kind constants declared
	 * on <code>CompletionProposal</code>
	 * @param allow <code>true</code> if a proposal of a given kind with a required proposal
	 * of the given kind is allowed by this requestor, and <code>false</code>
	 * if it isn't of interest
	 * @see #isAllowingRequiredProposals(int, int)
	 * @see CompletionProposal#getKind()
	 * @see CompletionProposal#getRequiredProposals()
	 *
	 */
	public void setAllowsRequiredProposals(int proposalKind, int requiredProposalKind, boolean allow) {
		if (proposalKind < CompletionProposal.FIRST_KIND
			|| proposalKind > CompletionProposal.LAST_KIND) {
				throw new IllegalArgumentException("Unknown kind of completion proposal: "+requiredProposalKind); //$NON-NLS-1$
		}
		if (requiredProposalKind < CompletionProposal.FIRST_KIND
			|| requiredProposalKind > CompletionProposal.LAST_KIND) {
				throw new IllegalArgumentException("Unknown required kind of completion proposal: "+requiredProposalKind); //$NON-NLS-1$
		}

		if (this.requiredProposalAllowSet == null) {
			this.requiredProposalAllowSet = new int[CompletionProposal.LAST_KIND + 1];
		}

		if (allow) {
			this.requiredProposalAllowSet[proposalKind] |= (1 << requiredProposalKind);
		} else {
			this.requiredProposalAllowSet[proposalKind] &= ~(1 << requiredProposalKind);
		}
	}

	/**
	 * Returns the favorite references which are used to compute some completion proposals.
	 * <p>
	 * Currently only on demand type references (<code>"java.util.Arrays.*"</code>),
	 * references to a static method or a static field are used to compute completion proposals.
	 * Other kind of reference could be used in the future.
	 * </p>
	 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
	 *
	 * @return favorite imports
	 *
	 */
	public String[] getFavoriteReferences() {
		return this.favoriteReferences;
	}

	/**
	 * Set the favorite references which will be used to compute some completion proposals.
	 * A favorite reference is a qualified reference as it can be seen in an import statement.<br>
	 *
	 * <p><b>Note: This Method only applies to ECMAScript 4 which is not yet supported</b></p>
	 *
	 * @param favoriteImports
	 *
	 * @see #getFavoriteReferences()
	 *
	 */
	public void setFavoriteReferences(String[] favoriteImports) {
		this.favoriteReferences = favoriteImports;
	}

	/**
	 * Pro forma notification sent before reporting a batch of
	 * completion proposals.
	 * <p>
	 * The default implementation of this method does nothing.
	 * Clients may override.
	 * </p>
	 */
	public void beginReporting() {
		// do nothing
	}

	/**
	 * Pro forma notification sent after reporting a batch of
	 * completion proposals.
	 * <p>
	 * The default implementation of this method does nothing.
	 * Clients may override.
	 * </p>
	 */
	public void endReporting() {
		// do nothing
	}

	/**
	 * Notification of failure to produce any completions.
	 * The problem object explains what prevented completing.
	 * <p>
	 * The default implementation of this method does nothing.
	 * Clients may override to receive this kind of notice.
	 * </p>
	 *
	 * @param problem the problem object
	 */
	public void completionFailure(IProblem problem) {
		// default behavior is to ignore
	}

	/**
	 * Proposes a completion. Has no effect if the kind of proposal
	 * is being ignored by this requestor. Callers should consider
	 * checking {@link #isIgnored(int)} before avoid creating proposal
	 * objects that would only be ignored.
	 * <p>
	 * Similarly, implementers should check
	 * {@link #isIgnored(int) isIgnored(proposal.getKind())}
	 * and ignore proposals that have been declared as uninteresting.
	 * The proposal object passed is only valid for the duration of
	 * completion operation.
	 *
	 * @param proposal the completion proposal
	 * @exception IllegalArgumentException if the proposal is null
	 */
	public abstract void accept(CompletionProposal proposal);

	/**
	 * Propose the context in which the completion occurs.
	 * <p>
	 * This method is called one and only one time before any call to
	 * {@link #accept(CompletionProposal)}.
	 * The default implementation of this method does nothing.
	 * Clients may override.
	 * </p>
	 * @param context the completion context
	 *
	 */
	public void acceptContext(CompletionContext context) {
		// do nothing
	}
}
