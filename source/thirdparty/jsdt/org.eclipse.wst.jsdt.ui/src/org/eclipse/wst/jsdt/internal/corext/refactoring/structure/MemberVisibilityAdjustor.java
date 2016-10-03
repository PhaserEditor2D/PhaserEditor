/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.structure;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IInitializer;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.IExtendedModifier;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.wst.jsdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.SearchUtils;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * Helper class to adjust the visibilities of members with respect to a reference element.
 * 
 * 
 */
public final class MemberVisibilityAdjustor {

	/**
	 * The visibility group category set.
	 * 
	 * 
	 */
	public static final GroupCategorySet SET_VISIBILITY_ADJUSTMENTS= new GroupCategorySet(new GroupCategory("org.eclipse.wst.jsdt.internal.corext.visibilityAdjustments", //$NON-NLS-1$
			RefactoringCoreMessages.MemberVisibilityAdjustor_adjustments_name, RefactoringCoreMessages.MemberVisibilityAdjustor_adjustments_description));

	/** Description of a member visibility adjustment */
	public static class IncomingMemberVisibilityAdjustment implements IVisibilityAdjustment {

		/** The keyword representing the adjusted visibility */
		protected final ModifierKeyword fKeyword;

		/** The member whose visibility has been adjusted */
		protected final IMember fMember;

		/** Does the visibility adjustment need rewriting? */
		protected boolean fNeedsRewriting= true;

		/** The associated refactoring status */
		protected final RefactoringStatus fRefactoringStatus;

		/**
		 * Creates a new incoming member visibility adjustment.
		 * 
		 * @param member the member which is adjusted
		 * @param keyword the keyword representing the adjusted visibility
		 * @param status the associated status, or <code>null</code>
		 */
		public IncomingMemberVisibilityAdjustment(final IMember member, final ModifierKeyword keyword, final RefactoringStatus status) {
			Assert.isNotNull(member);
			Assert.isTrue(!(member instanceof IInitializer));
			Assert.isTrue(isVisibilityKeyword(keyword));
			fMember= member;
			fKeyword= keyword;
			fRefactoringStatus= status;
		}

		/**
		 * Returns the visibility keyword.
		 * 
		 * @return the visibility keyword
		 */
		public final ModifierKeyword getKeyword() {
			return fKeyword;
		}

		/**
		 * Returns the adjusted member.
		 * 
		 * @return the adjusted member
		 */
		public final IMember getMember() {
			return fMember;
		}

		/**
		 * Returns the associated refactoring status.
		 * 
		 * @return the associated refactoring status
		 */
		public final RefactoringStatus getStatus() {
			return fRefactoringStatus;
		}

		/**
		 * Does the visibility adjustment need rewriting?
		 * 
		 * @return <code>true</code> if it needs rewriting, <code>false</code> otherwise
		 */
		public final boolean needsRewriting() {
			return fNeedsRewriting;
		}

		/**
		 * Rewrites the visibility adjustment.
		 * 
		 * @param adjustor the java element visibility adjustor
		 * @param rewrite the AST rewrite to use
		 * @param root the root of the AST used in the rewrite
		 * @param group the text edit group description to use, or <code>null</code>
		 * @param status the refactoring status, or <code>null</code>
		 * @throws JavaScriptModelException if an error occurs
		 */
		protected final void rewriteVisibility(final MemberVisibilityAdjustor adjustor, final ASTRewrite rewrite, final JavaScriptUnit root, final CategorizedTextEditGroup group, final RefactoringStatus status) throws JavaScriptModelException {
			Assert.isNotNull(adjustor);
			Assert.isNotNull(rewrite);
			Assert.isNotNull(root);
			final int visibility= fKeyword != null ? fKeyword.toFlagValue() : Modifier.NONE;
			if (fMember instanceof IField) {
				final VariableDeclarationFragment fragment= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField) fMember, root);
				final FieldDeclaration declaration= (FieldDeclaration) fragment.getParent();
				if (declaration.fragments().size() == 1)
					ModifierRewrite.create(rewrite, declaration).setVisibility(visibility, group);
				else {
					final VariableDeclarationFragment newFragment= rewrite.getAST().newVariableDeclarationFragment();
					newFragment.setName((SimpleName) rewrite.createCopyTarget(fragment.getName()));
					final FieldDeclaration newDeclaration= rewrite.getAST().newFieldDeclaration(newFragment);
					newDeclaration.setType((Type) rewrite.createCopyTarget(declaration.getType()));
					IExtendedModifier extended= null;
					for (final Iterator iterator= declaration.modifiers().iterator(); iterator.hasNext();) {
						extended= (IExtendedModifier) iterator.next();
						if (extended.isModifier()) {
							final Modifier modifier= (Modifier) extended;
							final int flag= modifier.getKeyword().toFlagValue();
							if ((flag & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE)) != 0)
								continue;
						}
						newDeclaration.modifiers().add(rewrite.createCopyTarget((ASTNode) extended));
					}
					ModifierRewrite.create(rewrite, newDeclaration).setVisibility(visibility, group);
					final AbstractTypeDeclaration type= (AbstractTypeDeclaration) declaration.getParent();
					rewrite.getListRewrite(type, type.getBodyDeclarationsProperty()).insertAfter(newDeclaration, declaration, null);
					final ListRewrite list= rewrite.getListRewrite(declaration, FieldDeclaration.FRAGMENTS_PROPERTY);
					list.remove(fragment, group);
					if (list.getRewrittenList().isEmpty())
						rewrite.remove(declaration, null);
				}
				if (status != null)
					adjustor.fStatus.merge(status);
			} else if (fMember != null) {
				final BodyDeclaration declaration= ASTNodeSearchUtil.getBodyDeclarationNode(fMember, root);
				if (declaration != null) {
					ModifierRewrite.create(rewrite, declaration).setVisibility(visibility, group);
					if (status != null)
						adjustor.fStatus.merge(status);
				}
			}
		}

		/*
		 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.structure.MemberVisibilityAdjustor.IVisibilityAdjustment#rewriteVisibility(org.eclipse.wst.jsdt.internal.corext.refactoring.structure.MemberVisibilityAdjustor, org.eclipse.core.runtime.IProgressMonitor)
		 */
		public void rewriteVisibility(final MemberVisibilityAdjustor adjustor, final IProgressMonitor monitor) throws JavaScriptModelException {
			Assert.isNotNull(adjustor);
			Assert.isNotNull(monitor);
			try {
				monitor.beginTask("", 1); //$NON-NLS-1$
				monitor.setTaskName(RefactoringCoreMessages.MemberVisibilityAdjustor_adjusting);
				if (fNeedsRewriting) {
					if (adjustor.fRewrite != null && adjustor.fRoot != null)
						rewriteVisibility(adjustor, adjustor.fRewrite, adjustor.fRoot, null, fRefactoringStatus);
					else {
						final CompilationUnitRewrite rewrite= adjustor.getCompilationUnitRewrite(fMember.getJavaScriptUnit());
						rewriteVisibility(adjustor, rewrite.getASTRewrite(), rewrite.getRoot(), rewrite.createCategorizedGroupDescription(Messages.format(RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility, getLabel(getKeyword())), SET_VISIBILITY_ADJUSTMENTS), fRefactoringStatus);
					}
				} else if (fRefactoringStatus != null)
					adjustor.fStatus.merge(fRefactoringStatus);
				monitor.worked(1);
			} finally {
				monitor.done();
			}
		}

		/**
		 * Determines whether the visibility adjustment needs rewriting.
		 * 
		 * @param rewriting <code>true</code> if it needs rewriting, <code>false</code> otherwise
		 */
		public final void setNeedsRewriting(final boolean rewriting) {
			fNeedsRewriting= rewriting;
		}
	}

	/** Interface for visibility adjustments */
	public interface IVisibilityAdjustment {

		/**
		 * Rewrites the visibility adjustment.
		 * 
		 * @param adjustor the java element visibility adjustor
		 * @param monitor the progress monitor to use
		 * @throws JavaScriptModelException if an error occurs
		 */
		public void rewriteVisibility(MemberVisibilityAdjustor adjustor, IProgressMonitor monitor) throws JavaScriptModelException;
	}

	/** Description of an outgoing member visibility adjustment */
	public static class OutgoingMemberVisibilityAdjustment extends IncomingMemberVisibilityAdjustment {

		/**
		 * Creates a new outgoing member visibility adjustment.
		 * 
		 * @param member the member which is adjusted
		 * @param keyword the keyword representing the adjusted visibility
		 * @param status the associated status
		 */
		public OutgoingMemberVisibilityAdjustment(final IMember member, final ModifierKeyword keyword, final RefactoringStatus status) {
			super(member, keyword, status);
		}

		/*
		 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.structure.MemberVisibilityAdjustor.IVisibilityAdjustment#rewriteVisibility(org.eclipse.wst.jsdt.internal.corext.refactoring.structure.MemberVisibilityAdjustor, org.eclipse.core.runtime.IProgressMonitor)
		 */
		public void rewriteVisibility(final MemberVisibilityAdjustor adjustor, final IProgressMonitor monitor) throws JavaScriptModelException {
			Assert.isNotNull(adjustor);
			Assert.isNotNull(monitor);
			try {
				monitor.beginTask("", 1); //$NON-NLS-1$
				monitor.setTaskName(RefactoringCoreMessages.MemberVisibilityAdjustor_adjusting);
				if (fNeedsRewriting) {
					final CompilationUnitRewrite rewrite= adjustor.getCompilationUnitRewrite(fMember.getJavaScriptUnit());
					rewriteVisibility(adjustor, rewrite.getASTRewrite(), rewrite.getRoot(), rewrite.createCategorizedGroupDescription(Messages.format(RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility, getLabel(getKeyword())), SET_VISIBILITY_ADJUSTMENTS), fRefactoringStatus);
				}
				monitor.worked(1);
			} finally {
				monitor.done();
			}
		}
	}

	/**
	 * Returns the label for the specified java element.
	 * 
	 * @param element the element to get the label for
	 * @return the label for the element
	 */
	public static String getLabel(final IJavaScriptElement element) {
		Assert.isNotNull(element);
		return JavaScriptElementLabels.getElementLabel(element, JavaScriptElementLabels.ALL_FULLY_QUALIFIED | JavaScriptElementLabels.ALL_DEFAULT);
	}

	/**
	 * Returns the label for the specified visibility keyword.
	 * 
	 * @param keyword the keyword to get the label for, or <code>null</code> for default visibility
	 * @return the label for the keyword
	 */
	public static String getLabel(final ModifierKeyword keyword) {
		Assert.isTrue(isVisibilityKeyword(keyword));
		if (keyword == null)
			return RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility_default;
		else if (ModifierKeyword.PUBLIC_KEYWORD.equals(keyword))
			return RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility_public;
		else if (ModifierKeyword.PROTECTED_KEYWORD.equals(keyword))
			return RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility_protected;
		else
			return RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility_private;
	}

	/**
	 * Returns the message string for the specified member.
	 * 
	 * @param member the member to get the string for
	 * @return the string for the member
	 */
	public static String getMessage(final IMember member) {
		Assert.isTrue(member instanceof IType || member instanceof IFunction || member instanceof IField);
		if (member instanceof IType)
			return RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility_type_warning;
		else if (member instanceof IFunction)
			return RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility_method_warning;
		else
			return RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility_field_warning;
	}

	/**
	 * Do the specified modifiers represent a lower visibility than the required threshold?
	 * 
	 * @param modifiers the modifiers to test
	 * @param threshold the visibility threshold to compare with
	 * @return <code>true</code> if the visibility is lower than required, <code>false</code> otherwise
	 */
	public static boolean hasLowerVisibility(final int modifiers, final int threshold) {
		if (Modifier.isPrivate(threshold))
			return false;
		else if (Modifier.isPublic(threshold))
			return !Modifier.isPublic(modifiers);
		else if (Modifier.isProtected(threshold))
			return !Modifier.isProtected(modifiers) && !Modifier.isPublic(modifiers);
		else
			return Modifier.isPrivate(modifiers);
	}

	/**
	 * Does the specified modifier keyword represent a lower visibility than the required threshold?
	 * 
	 * @param keyword the visibility keyword to test, or <code>null</code> for default visibility
	 * @param threshold the visibility threshold keyword to compare with, or <code>null</code> to compare with default visibility
	 * @return <code>true</code> if the visibility is lower than required, <code>false</code> otherwise
	 */
	public static boolean hasLowerVisibility(final ModifierKeyword keyword, final ModifierKeyword threshold) {
		Assert.isTrue(isVisibilityKeyword(keyword));
		Assert.isTrue(isVisibilityKeyword(threshold));
		return hasLowerVisibility(keyword != null ? keyword.toFlagValue() : Modifier.NONE, threshold != null ? threshold.toFlagValue() : Modifier.NONE);
	}

	/**
	 * Is the specified severity a refactoring status severity?
	 * 
	 * @param severity the severity to test
	 * @return <code>true</code> if it is a refactoring status severity, <code>false</code> otherwise
	 */
	private static boolean isStatusSeverity(final int severity) {
		return severity == RefactoringStatus.ERROR || severity == RefactoringStatus.FATAL || severity == RefactoringStatus.INFO || severity == RefactoringStatus.OK || severity == RefactoringStatus.WARNING;
	}

	/**
	 * Is the specified modifier keyword a visibility keyword?
	 * 
	 * @param keyword the keyword to test, or <code>null</code>
	 * @return <code>true</code> if it is a visibility keyword, <code>false</code> otherwise
	 */
	private static boolean isVisibilityKeyword(final ModifierKeyword keyword) {
		return keyword == null || ModifierKeyword.PUBLIC_KEYWORD.equals(keyword) || ModifierKeyword.PROTECTED_KEYWORD.equals(keyword) || ModifierKeyword.PRIVATE_KEYWORD.equals(keyword);
	}

	/**
	 * Is the specified modifier a visibility modifier?
	 * 
	 * @param modifier the keyword to test
	 * @return <code>true</code> if it is a visibility modifier, <code>false</code> otherwise
	 */
	private static boolean isVisibilityModifier(final int modifier) {
		return modifier == Modifier.NONE || modifier == Modifier.PUBLIC || modifier == Modifier.PROTECTED || modifier == Modifier.PRIVATE;
	}

	/**
	 * Converts a given modifier keyword into a visibility flag.
	 * 
	 * @param keyword the keyword to convert
	 * @return the visibility flag
	 */
	private static int keywordToVisibility(final ModifierKeyword keyword) {
		int visibility= 0;
		if (keyword == ModifierKeyword.PUBLIC_KEYWORD)
			visibility= Flags.AccPublic;
		else if (keyword == ModifierKeyword.PRIVATE_KEYWORD)
			visibility= Flags.AccPrivate;
		return visibility;
	}

	/**
	 * Does the specified member need further visibility adjustment?
	 * 
	 * @param member the member to test
	 * @param threshold the visibility threshold to test for
	 * @param adjustments the map of members to visibility adjustments
	 * @return <code>true</code> if the member needs further adjustment, <code>false</code> otherwise
	 */
	public static boolean needsVisibilityAdjustments(final IMember member, final int threshold, final Map adjustments) {
		Assert.isNotNull(member);
		Assert.isTrue(isVisibilityModifier(threshold));
		Assert.isNotNull(adjustments);
		final IncomingMemberVisibilityAdjustment adjustment= (IncomingMemberVisibilityAdjustment) adjustments.get(member);
		if (adjustment != null) {
			final ModifierKeyword keyword= adjustment.getKeyword();
			return hasLowerVisibility(keyword == null ? Modifier.NONE : keyword.toFlagValue(), threshold);
		}
		return true;
	}

	/**
	 * Does the specified member need further visibility adjustment?
	 * 
	 * @param member the member to test
	 * @param threshold the visibility threshold to test for, or <code>null</code> for default visibility
	 * @param adjustments the map of members to visibility adjustments
	 * @return <code>true</code> if the member needs further adjustment, <code>false</code> otherwise
	 */
	public static boolean needsVisibilityAdjustments(final IMember member, final ModifierKeyword threshold, final Map adjustments) {
		Assert.isNotNull(member);
		Assert.isNotNull(adjustments);
		final IncomingMemberVisibilityAdjustment adjustment= (IncomingMemberVisibilityAdjustment) adjustments.get(member);
		if (adjustment != null)
			return hasLowerVisibility(adjustment.getKeyword(), threshold);
		return true;
	}

	/** The map of members to visibility adjustments */
	private Map fAdjustments= new HashMap();

	/** Should incoming references be adjusted? */
	private boolean fIncoming= true;

	/** Should outgoing references be adjusted? */
	private boolean fOutgoing= true;

	/** The referenced element causing the visibility adjustment */
	private final IMember fReferenced;

	/** The referencing java element */
	private final IJavaScriptElement fReferencing;

	/** The AST rewrite to use for reference visibility adjustments, or <code>null</code> to use a compilation unit rewrite */
	private ASTRewrite fRewrite= null;

	/** The map of compilation units to compilation unit rewrites */
	private Map fRewrites= new HashMap(3);

	/** The root node of the AST rewrite for reference visibility adjustments, or <code>null</code> to use a compilation unit rewrite */
	private JavaScriptUnit fRoot= null;

	/** The incoming search scope */
	private IJavaScriptSearchScope fScope;

	/** The status of the visibility adjustment */
	private RefactoringStatus fStatus= new RefactoringStatus();

	/** The type hierarchy cache */
	private final Map fTypeHierarchies= new HashMap();

	/** The visibility message severity */
	private int fVisibilitySeverity= RefactoringStatus.WARNING;

	/** The working copy owner, or <code>null</code> to use none */
	private WorkingCopyOwner fOwner= null;

	/**
	 * Creates a new java element visibility adjustor.
	 * 
	 * @param referencing the referencing element used to compute the visibility
	 * @param referenced the referenced member which causes the visibility changes
	 */
	public MemberVisibilityAdjustor(final IJavaScriptElement referencing, final IMember referenced) {
		Assert.isTrue(!(referenced instanceof IInitializer));
		Assert.isTrue(referencing instanceof IJavaScriptUnit || referencing instanceof IType || referencing instanceof IPackageFragment);
		fScope= RefactoringScopeFactory.createReferencedScope(new IJavaScriptElement[] { referenced}, IJavaScriptSearchScope.REFERENCED_PROJECTS | IJavaScriptSearchScope.SOURCES | IJavaScriptSearchScope.APPLICATION_LIBRARIES);
		fReferencing= referencing;
		fReferenced= referenced;
	}

	/**
	 * Adjusts the visibility of the specified member.
	 * 
	 * @param element the "source" point from which to calculate the visibility
	 * @param referencedMovedElement the moved element which may be adjusted in visibility
	 * @param monitor the progress monitor to use
	 * @throws JavaScriptModelException if the visibility adjustment could not be computed
	 */
	private void adjustIncomingVisibility(final IJavaScriptElement element, IMember referencedMovedElement, final IProgressMonitor monitor) throws JavaScriptModelException {
		final ModifierKeyword threshold= getVisibilityThreshold(element, referencedMovedElement, monitor);
		int flags= referencedMovedElement.getFlags();
		IType declaring= referencedMovedElement.getDeclaringType();
		if (hasLowerVisibility(flags, threshold == null ? Modifier.NONE : threshold.toFlagValue()) && needsVisibilityAdjustment(referencedMovedElement, threshold))
			fAdjustments.put(referencedMovedElement, new IncomingMemberVisibilityAdjustment(referencedMovedElement, threshold, RefactoringStatus.createStatus(fVisibilitySeverity, Messages.format(getMessage(referencedMovedElement), new String[] { getLabel(referencedMovedElement), getLabel(threshold)}), JavaStatusContext.create(referencedMovedElement), null, RefactoringStatusEntry.NO_CODE, null)));
	}

	/**
	 * Check whether anyone accesses the members of the moved type from the
	 * outside. Those may need to have their visibility adjusted.
	 * @param member 
	 * @param monitor 
	 * @throws JavaScriptModelException 
	 */
	private void adjustMemberVisibility(final IMember member, final IProgressMonitor monitor) throws JavaScriptModelException {

		if (member instanceof IType) {
			// recursively check accessibility of member type's members
			final IJavaScriptElement[] typeMembers= ((IType) member).getChildren();
			for (int i= 0; i < typeMembers.length; i++) {
				if (! (typeMembers[i] instanceof IInitializer))
					adjustMemberVisibility((IMember) typeMembers[i], monitor);
			}
		}

		if ((member.equals(fReferenced)) || (Modifier.isPublic(member.getFlags())))
			return;

		final SearchResultGroup[] references= findReferences(member, monitor);
		for (int i= 0; i < references.length; i++) {
			final SearchMatch[] searchResults= references[i].getSearchResults();
			for (int k= 0; k < searchResults.length; k++) {
				final IJavaScriptElement referenceToMember= (IJavaScriptElement) searchResults[k].getElement();
				if (fAdjustments.get(member) == null && referenceToMember instanceof IMember && !isInsideMovedMember(referenceToMember)) {
					// check whether the member is still visible from the
					// destination. As we are moving a type, the destination is
					// a package or another type.
					adjustIncomingVisibility(fReferencing, member, new SubProgressMonitor(monitor, 1));
				}
			}
		}
	}

	/**
	 * Is the specified member inside the moved member?
	 * @param element the element
	 * @return <code>true</code> if it is inside, <code>false</code> otherwise
	 */
	private boolean isInsideMovedMember(final IJavaScriptElement element) {
		IJavaScriptElement current= element;
		while ((current= current.getParent()) != null)
			if (current.equals(fReferenced))
				return true;
		return false;
	}

	/**
	 * Finds references to the specified member.
	 * @param member the member
	 * @param monitor the progress monitor to use
	 * @return the search result groups
	 * @throws JavaScriptModelException if an error occurs during search
	 */
	private SearchResultGroup[] findReferences(final IMember member, final IProgressMonitor monitor) throws JavaScriptModelException {
		final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(SearchPattern.createPattern(member, IJavaScriptSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE));
		engine.setOwner(fOwner);
		engine.setFiltering(true, true);
		engine.setScope(RefactoringScopeFactory.create(member));
		engine.searchPattern(new SubProgressMonitor(monitor, 1));
		return (SearchResultGroup[]) engine.getResults();
	}

	/**
	 * Adjusts the visibility of the member based on the incoming references
	 * represented by the specified search result groups.
	 * 
	 * If there is at least one reference to the moved element from outside the
	 * moved element, visibility must be increased such that the moved element
	 * (fReferenced) is still visible at the target from all references. This
	 * effectively means that the old element (fReferenced) must be visible from
	 * the new location (fReferencing).
	 * 
	 * @param groups the search result groups representing the references
	 * @param monitor the progress monitor to use
	 * @throws JavaScriptModelException if the java elements could not be accessed
	 */
	private void adjustIncomingVisibility(final SearchResultGroup[] groups, final IProgressMonitor monitor) throws JavaScriptModelException {
		try {
			monitor.beginTask("", groups.length); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.MemberVisibilityAdjustor_checking);
			SearchMatch[] matches= null;
			boolean adjusted= false;
			for (int index= 0; index < groups.length; index++) {
				matches= groups[index].getSearchResults();
				for (int offset= 0; offset < matches.length; offset++) {
					final Object element= matches[offset].getElement();
					if (element instanceof IMember && !isInsideMovedMember((IMember) element)) {
						// found one reference which is not inside the moved
						// element => adjust visibility of the moved element
						adjustIncomingVisibility(fReferencing, fReferenced, monitor);
						adjusted= true; // one adjustment is enough
						break;
					}
				}
				if (adjusted)
					break;
				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Adjusts the visibility of the referenced field found in a compilation unit.
	 * 
	 * @param field the referenced field to adjust
	 * @param threshold the visibility threshold, or <code>null</code> for default visibility
	 * @throws JavaScriptModelException if an error occurs
	 */
	private void adjustOutgoingVisibility(final IField field, final ModifierKeyword threshold) throws JavaScriptModelException {
		Assert.isTrue(!field.isBinary() && !field.isReadOnly());
		//bug 100555 (moving inner class to top level class; taking private fields with you)
		final IType declaring= field.getDeclaringType();
		if (declaring != null && declaring.equals(fReferenced)) return;
		if (hasLowerVisibility(field.getFlags(), keywordToVisibility(threshold)) && needsVisibilityAdjustment(field, threshold)) 
			adjustOutgoingVisibility(field, threshold, RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility_field_warning);
	}

	/**
	 * Adjusts the visibility of the referenced body declaration.
	 * 
	 * @param member the member where to adjust the visibility
	 * @param threshold the visibility keyword representing the required visibility, or <code>null</code> for default visibility
	 * @param template the message template to use
	 * @throws JavaScriptModelException if an error occurs
	 */
	private void adjustOutgoingVisibility(final IMember member, final ModifierKeyword threshold, final String template) throws JavaScriptModelException {
		Assert.isTrue(!member.isBinary() && !member.isReadOnly());
		boolean adjust= true;
		final IType declaring= member.getDeclaringType();
		if (declaring != null && (declaring.equals(fReferenced)))
			adjust= false;
		if (adjust && hasLowerVisibility(member.getFlags(), keywordToVisibility(threshold)) && needsVisibilityAdjustment(member, threshold))
			fAdjustments.put(member, new OutgoingMemberVisibilityAdjustment(member, threshold, RefactoringStatus.createStatus(fVisibilitySeverity, Messages.format(template, new String[] { JavaScriptElementLabels.getTextLabel(member, JavaScriptElementLabels.M_PARAMETER_TYPES | JavaScriptElementLabels.ALL_FULLY_QUALIFIED), getLabel(threshold)}), JavaStatusContext.create(member), null, RefactoringStatusEntry.NO_CODE, null)));
	}

	/**
	 * Adjusts the visibilities of the referenced element from the search match found in a compilation unit.
	 * 
	 * @param match the search match representing the element declaration
	 * @param monitor the progress monitor to use
	 * @throws JavaScriptModelException if the visibility could not be determined
	 */
	private void adjustOutgoingVisibility(final SearchMatch match, final IProgressMonitor monitor) throws JavaScriptModelException {
		final Object element= match.getElement();
		if (element instanceof IMember) {
			final IMember member= (IMember) element;
			if (!member.isBinary() && !member.isReadOnly() && !isInsideMovedMember(member)) {
				adjustOutgoingVisibilityChain(member, monitor);
			}
		}
	}

	private void adjustOutgoingVisibilityChain(final IMember member, final IProgressMonitor monitor) throws JavaScriptModelException {

		if (!Modifier.isPublic(member.getFlags())) {
			final ModifierKeyword threshold= computeOutgoingVisibilityThreshold(fReferencing, member, monitor);
			if (member instanceof IFunction) {
				adjustOutgoingVisibility(member, threshold, RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility_method_warning);
			} else if (member instanceof IField) {
				adjustOutgoingVisibility((IField) member, threshold);
			} else if (member instanceof IType) {
				adjustOutgoingVisibility(member, threshold, RefactoringCoreMessages.MemberVisibilityAdjustor_change_visibility_type_warning);
			}
		}

		if (member.getDeclaringType() != null)
			adjustOutgoingVisibilityChain(member.getDeclaringType(), monitor);
	}

	/**
	 * Adjusts the visibilities of the outgoing references from the member represented by the specified search result groups.
	 * 
	 * @param groups the search result groups representing the references
	 * @param monitor the progress monitor to us
	 * @throws JavaScriptModelException if the visibility could not be determined
	 */
	private void adjustOutgoingVisibility(final SearchResultGroup[] groups, final IProgressMonitor monitor) throws JavaScriptModelException {
		try {
			monitor.beginTask("", groups.length); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.MemberVisibilityAdjustor_checking);
			IJavaScriptElement element= null;
			SearchMatch[] matches= null;
			SearchResultGroup group= null;
			for (int index= 0; index < groups.length; index++) {
				group= groups[index];
				element= JavaScriptCore.create(group.getResource());
				if (element instanceof IJavaScriptUnit) {
					matches= group.getSearchResults();
					for (int offset= 0; offset < matches.length; offset++)
						adjustOutgoingVisibility(matches[offset], new SubProgressMonitor(monitor, 1));
				} // else if (element != null)
				// fStatus.merge(RefactoringStatus.createStatus(fFailureSeverity, RefactoringCoreMessages.getFormattedString("MemberVisibilityAdjustor.binary.outgoing.project", new String[] { element.getJavaProject().getElementName(), getLabel(fReferenced)}), null, null, RefactoringStatusEntry.NO_CODE, null)); //$NON-NLS-1$
				// else if (group.getResource() != null)
				// fStatus.merge(RefactoringStatus.createStatus(fFailureSeverity, RefactoringCoreMessages.getFormattedString("MemberVisibilityAdjustor.binary.outgoing.resource", new String[] { group.getResource().getName(), getLabel(fReferenced)}), null, null, RefactoringStatusEntry.NO_CODE, null)); //$NON-NLS-1$

				// TW: enable when bug 78387 is fixed

				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Adjusts the visibilities of the referenced and referencing elements.
	 * 
	 * @param monitor the progress monitor to use
	 * @throws JavaScriptModelException if an error occurs during search
	 */
	public final void adjustVisibility(final IProgressMonitor monitor) throws JavaScriptModelException {
		try {
			monitor.beginTask("", 7); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.MemberVisibilityAdjustor_checking);
			final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(SearchPattern.createPattern(fReferenced, IJavaScriptSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE));
			engine.setScope(fScope);
			engine.setStatus(fStatus);
			engine.setOwner(fOwner);
			if (fIncoming) { 
				// check calls to the referenced (moved) element, adjust element
				// visibility if necessary.
				engine.searchPattern(new SubProgressMonitor(monitor, 1));
				adjustIncomingVisibility((SearchResultGroup[]) engine.getResults(), new SubProgressMonitor(monitor, 1));
				engine.clearResults();
				// If the moved element is a type: Adjust visibility of members
				// of the type if they are accessed outside of the moved type
				if (fReferenced instanceof IType) {
					final IType type= (IType) fReferenced;
					adjustMemberVisibility(type, new SubProgressMonitor(monitor, 1));
				}
			}
			if (fOutgoing) {
				/*
				 * Search for the types, fields, and methods which
				 * are called/acted upon inside the referenced element (the one
				 * to be moved) and assure that they are also visible from
				 * within the referencing element (the destination type (or
				 * package if move to new type)).
				 */
				engine.searchReferencedTypes(fReferenced, new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				engine.searchReferencedFields(fReferenced, new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				engine.searchReferencedMethods(fReferenced, new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				adjustOutgoingVisibility((SearchResultGroup[]) engine.getResults(), new SubProgressMonitor(monitor, 1));
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Computes the visibility threshold for the referenced element.
	 * 
	 * @param referencing the referencing element
	 * @param referenced the referenced element
	 * @param monitor the progress monitor to use
	 * @return the visibility keyword corresponding to the threshold, or <code>null</code> for default visibility
	 * @throws JavaScriptModelException if the java elements could not be accessed
	 */
	public ModifierKeyword getVisibilityThreshold(final IJavaScriptElement referencing, final IMember referenced, final IProgressMonitor monitor) throws JavaScriptModelException {
		Assert.isTrue(!(referencing instanceof IInitializer));
		Assert.isTrue(!(referenced instanceof IInitializer));
		ModifierKeyword keyword= ModifierKeyword.PUBLIC_KEYWORD;
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.MemberVisibilityAdjustor_checking);
			final int referencingType= referencing.getElementType();
			final int referencedType= referenced.getElementType();
			switch (referencedType) {
				case IJavaScriptElement.TYPE: {
					final IType typeReferenced= (IType) referenced;
					final IJavaScriptUnit referencedUnit= typeReferenced.getJavaScriptUnit();
					switch (referencingType) {
						case IJavaScriptElement.TYPE: {
							keyword= thresholdTypeToType((IType) referencing, typeReferenced, monitor);
							break;
						}
						case IJavaScriptElement.FIELD:
						case IJavaScriptElement.METHOD: {
							final IMember member= (IMember) referencing;
							if (typeReferenced.equals(member.getDeclaringType()))
								keyword= ModifierKeyword.PRIVATE_KEYWORD;
							else if (referencedUnit != null && referencedUnit.equals(member.getJavaScriptUnit()))
								keyword= ModifierKeyword.PRIVATE_KEYWORD;
							else if (typeReferenced.getPackageFragment().equals(member.getDeclaringType().getPackageFragment()))
								keyword= null;
							break;
						}
						case IJavaScriptElement.PACKAGE_FRAGMENT: {
							final IPackageFragment fragment= (IPackageFragment) referencing;
							if (typeReferenced.getPackageFragment().equals(fragment))
								keyword= null;
							break;
						}
						default:
							Assert.isTrue(false);
					}
					break;
				}
				case IJavaScriptElement.FIELD: {
					final IField fieldReferenced= (IField) referenced;
					final IJavaScriptUnit referencedUnit= fieldReferenced.getJavaScriptUnit();
					switch (referencingType) {
						case IJavaScriptElement.TYPE: {
							keyword= thresholdTypeToField((IType) referencing, fieldReferenced, monitor);
							break;
						}
						case IJavaScriptElement.FIELD:
						case IJavaScriptElement.METHOD: {
							final IMember member= (IMember) referencing;
							if (fieldReferenced.getDeclaringType().equals(member.getDeclaringType()))
								keyword= ModifierKeyword.PRIVATE_KEYWORD;
							else if (referencedUnit != null && referencedUnit.equals(member.getJavaScriptUnit()))
								keyword= ModifierKeyword.PRIVATE_KEYWORD;
							else if (fieldReferenced.getDeclaringType().getPackageFragment().equals(member.getDeclaringType().getPackageFragment()))
								keyword= null;
							break;
						}
						case IJavaScriptElement.PACKAGE_FRAGMENT: {
							final IPackageFragment fragment= (IPackageFragment) referencing;
							if (fieldReferenced.getDeclaringType().getPackageFragment().equals(fragment))
								keyword= null;
							break;
						}
						default:
							Assert.isTrue(false);
					}
					break;
				}
				case IJavaScriptElement.METHOD: {
					final IFunction methodReferenced= (IFunction) referenced;
					final IJavaScriptUnit referencedUnit= methodReferenced.getJavaScriptUnit();
					switch (referencingType) {
						case IJavaScriptElement.TYPE: {
							keyword= thresholdTypeToMethod((IType) referencing, methodReferenced, monitor);
							break;
						}
						case IJavaScriptElement.FIELD: 
						case IJavaScriptElement.METHOD: {
							final IMember member= (IMember) referencing;
							if (methodReferenced.getDeclaringType().equals(member.getDeclaringType()))
								keyword= ModifierKeyword.PRIVATE_KEYWORD;
							else if (referencedUnit != null && referencedUnit.equals(member.getJavaScriptUnit()))
								keyword= ModifierKeyword.PRIVATE_KEYWORD;
							else if (methodReferenced.getDeclaringType().getPackageFragment().equals(member.getDeclaringType().getPackageFragment()))
								keyword= null;
							break;
						}
						case IJavaScriptElement.PACKAGE_FRAGMENT: {
							final IPackageFragment fragment= (IPackageFragment) referencing;
							if (methodReferenced.getDeclaringType().getPackageFragment().equals(fragment))
								keyword= null;
							break;
						}
						default:
							Assert.isTrue(false);
					}
					break;
				}
				default:
					Assert.isTrue(false);
			}
		} finally {
			monitor.done();
		}
		return keyword;
	}

	/**
	 * Computes the visibility threshold for the referenced element.
	 * 
	 * @param referencing the referencing element
	 * @param referenced the referenced element
	 * @param monitor the progress monitor to use
	 * @return the visibility keyword corresponding to the threshold, or <code>null</code> for default visibility
	 * @throws JavaScriptModelException if the java elements could not be accessed
	 */
	private ModifierKeyword computeOutgoingVisibilityThreshold(final IJavaScriptElement referencing, final IMember referenced, final IProgressMonitor monitor) throws JavaScriptModelException {
		Assert.isTrue(referencing instanceof IJavaScriptUnit || referencing instanceof IType || referencing instanceof IPackageFragment);
		Assert.isTrue(referenced instanceof IType || referenced instanceof IField || referenced instanceof IFunction);
		ModifierKeyword keyword= ModifierKeyword.PUBLIC_KEYWORD;
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.MemberVisibilityAdjustor_checking);
			final int referencingType= referencing.getElementType();
			final int referencedType= referenced.getElementType();
			switch (referencedType) {
				case IJavaScriptElement.TYPE: {
					final IType typeReferenced= (IType) referenced;
					switch (referencingType) {
						case IJavaScriptElement.JAVASCRIPT_UNIT: {
							final IJavaScriptUnit unit= (IJavaScriptUnit) referencing;
							final IJavaScriptUnit referencedUnit= typeReferenced.getJavaScriptUnit();
							if (referencedUnit != null && referencedUnit.equals(unit))
								keyword= ModifierKeyword.PRIVATE_KEYWORD;
							else if (referencedUnit != null && referencedUnit.getParent().equals(unit.getParent()))
								keyword= null;
							break;
						}
						case IJavaScriptElement.TYPE: {
							keyword= thresholdTypeToType((IType) referencing, typeReferenced, monitor);
							break;
						}
						case IJavaScriptElement.PACKAGE_FRAGMENT: {
							final IPackageFragment fragment= (IPackageFragment) referencing;
							if (typeReferenced.getPackageFragment().equals(fragment))
								keyword= null;
							break;
						}
						default:
							Assert.isTrue(false);
					}
					break;
				}
				case IJavaScriptElement.FIELD: {
					final IField fieldReferenced= (IField) referenced;
					final IJavaScriptUnit referencedUnit= fieldReferenced.getJavaScriptUnit();
					switch (referencingType) {
						case IJavaScriptElement.JAVASCRIPT_UNIT: {
							final IJavaScriptUnit unit= (IJavaScriptUnit) referencing;
							if (referencedUnit != null && referencedUnit.equals(unit))
								keyword= ModifierKeyword.PRIVATE_KEYWORD;
							else if (referencedUnit != null && referencedUnit.getParent().equals(unit.getParent()))
								keyword= null;
							break;
						}
						case IJavaScriptElement.TYPE: {
							keyword= thresholdTypeToField((IType) referencing, fieldReferenced, monitor);
							break;
						}
						case IJavaScriptElement.PACKAGE_FRAGMENT: {
							final IPackageFragment fragment= (IPackageFragment) referencing;
							if (fieldReferenced.getDeclaringType().getPackageFragment().equals(fragment))
								keyword= null;
							break;
						}
						default:
							Assert.isTrue(false);
					}
					break;
				}
				case IJavaScriptElement.METHOD: {
					final IFunction methodReferenced= (IFunction) referenced;
					final IJavaScriptUnit referencedUnit= methodReferenced.getJavaScriptUnit();
					switch (referencingType) {
						case IJavaScriptElement.JAVASCRIPT_UNIT: {
							final IJavaScriptUnit unit= (IJavaScriptUnit) referencing;
							if (referencedUnit != null && referencedUnit.equals(unit))
								keyword= ModifierKeyword.PRIVATE_KEYWORD;
							else if (referencedUnit != null && referencedUnit.getParent().equals(unit.getParent()))
								keyword= null;
							break;
						}
						case IJavaScriptElement.TYPE: {
							keyword= thresholdTypeToMethod((IType) referencing, methodReferenced, monitor);
							break;
						}
						case IJavaScriptElement.PACKAGE_FRAGMENT: {
							final IPackageFragment fragment= (IPackageFragment) referencing;
							if (methodReferenced.getDeclaringType().getPackageFragment().equals(fragment))
								keyword= null;
							break;
						}
						default:
							Assert.isTrue(false);
					}
					break;
				}
				default:
					Assert.isTrue(false);
			}
		} finally {
			monitor.done();
		}
		return keyword;
	}

	/**
	 * Returns the existing visibility adjustments (element type: Map <IMember, IVisibilityAdjustment>).
	 * 
	 * @return the visibility adjustments
	 */
	public final Map getAdjustments() {
		return fAdjustments;
	}

	/**
	 * Returns a compilation unit rewrite for the specified compilation unit.
	 * 
	 * @param unit the compilation unit to get the rewrite for
	 * @return the rewrite for the compilation unit
	 */
	private CompilationUnitRewrite getCompilationUnitRewrite(final IJavaScriptUnit unit) {
		CompilationUnitRewrite rewrite= (CompilationUnitRewrite) fRewrites.get(unit);
		if (rewrite == null) {
			if (fOwner == null)
				rewrite= new CompilationUnitRewrite(unit);
			else
				rewrite= new CompilationUnitRewrite(fOwner, unit);
		}
		return rewrite;
	}

	/**
	 * Returns a cached type hierarchy for the specified type.
	 * 
	 * @param type the type to get the hierarchy for
	 * @param monitor the progress monitor to use
	 * @return the type hierarchy
	 * @throws JavaScriptModelException if the type hierarchy could not be created
	 */
	private ITypeHierarchy getTypeHierarchy(final IType type, final IProgressMonitor monitor) throws JavaScriptModelException {
		ITypeHierarchy hierarchy= null;
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.MemberVisibilityAdjustor_checking);
			try {
				hierarchy= (ITypeHierarchy) fTypeHierarchies.get(type);
				if (hierarchy == null) {
					if (fOwner == null)
						hierarchy= type.newSupertypeHierarchy(new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
					else
						hierarchy= type.newSupertypeHierarchy(fOwner, new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				}
			} finally {
				monitor.done();
			}
		} finally {
			monitor.done();
		}
		return hierarchy;
	}

	/**
	 * Does the specified member need further visibility adjustment?
	 * 
	 * @param member the member to test
	 * @param threshold the visibility threshold to test for
	 * @return <code>true</code> if the member needs further adjustment, <code>false</code> otherwise
	 */
	private boolean needsVisibilityAdjustment(final IMember member, final ModifierKeyword threshold) {
		Assert.isNotNull(member);
		return needsVisibilityAdjustments(member, threshold, fAdjustments);
	}

	/**
	 * Rewrites the computed adjustments for the specified compilation unit.
	 * 
	 * @param unit the compilation unit to rewrite the adjustments
	 * @param monitor the progress monitor to use
	 * @throws JavaScriptModelException if an error occurs during search
	 */
	public final void rewriteVisibility(final IJavaScriptUnit unit, final IProgressMonitor monitor) throws JavaScriptModelException {
		try {
			monitor.beginTask("", fAdjustments.keySet().size()); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.MemberVisibilityAdjustor_adjusting);
			IMember member= null;
			IVisibilityAdjustment adjustment= null;
			for (final Iterator iterator= fAdjustments.keySet().iterator(); iterator.hasNext();) {
				member= (IMember) iterator.next();
				if (unit.equals(member.getJavaScriptUnit())) {
					adjustment= (IVisibilityAdjustment) fAdjustments.get(member);
					if (adjustment != null)
						adjustment.rewriteVisibility(this, new SubProgressMonitor(monitor, 1));
				}
			}
		} finally {
			fTypeHierarchies.clear();
			monitor.done();
		}
	}

	/**
	 * Rewrites the computed adjustments.
	 * 
	 * @param monitor the progress monitor to use
	 * @throws JavaScriptModelException if an error occurs during search
	 */
	public final void rewriteVisibility(final IProgressMonitor monitor) throws JavaScriptModelException {
		try {
			monitor.beginTask("", fAdjustments.keySet().size()); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.MemberVisibilityAdjustor_adjusting);
			IMember member= null;
			IVisibilityAdjustment adjustment= null;
			for (final Iterator iterator= fAdjustments.keySet().iterator(); iterator.hasNext();) {
				member= (IMember) iterator.next();
				adjustment= (IVisibilityAdjustment) fAdjustments.get(member);
				if (adjustment != null)
					adjustment.rewriteVisibility(this, new SubProgressMonitor(monitor, 1));
				if (monitor.isCanceled())
					throw new OperationCanceledException();
			}
		} finally {
			fTypeHierarchies.clear();
			monitor.done();
		}
	}

	/**
	 * Sets the existing visibility adjustments to be taken into account (element type: Map <IMember, IVisibilityAdjustment>).
	 * <p>
	 * This method must be called before calling {@link MemberVisibilityAdjustor#adjustVisibility(IProgressMonitor)}. The default is to take no existing adjustments into account.
	 * 
	 * @param adjustments the existing adjustments to set
	 */
	public final void setAdjustments(final Map adjustments) {
		Assert.isNotNull(adjustments);
		fAdjustments= adjustments;
	}

	/**
	 * Sets the severity of failure messages.
	 * <p>
	 * This method must be called before calling {@link MemberVisibilityAdjustor#adjustVisibility(IProgressMonitor)}. The default is a status with value {@link RefactoringStatus#ERROR}.
	 * 
	 * @param severity the severity of failure messages
	 */
	public final void setFailureSeverity(final int severity) {
		Assert.isTrue(isStatusSeverity(severity));
	}

	/**
	 * Determines whether incoming references should be adjusted.
	 * <p>
	 * This method must be called before calling {@link MemberVisibilityAdjustor#adjustVisibility(IProgressMonitor)}. The default is to adjust incoming references.
	 * 
	 * @param incoming <code>true</code> if incoming references should be adjusted, <code>false</code> otherwise
	 */
	public final void setIncoming(final boolean incoming) {
		fIncoming= incoming;
	}

	/**
	 * Determines whether outgoing references should be adjusted.
	 * <p>
	 * This method must be called before calling {@link MemberVisibilityAdjustor#adjustVisibility(IProgressMonitor)}. The default is to adjust outgoing references.
	 * 
	 * @param outgoing <code>true</code> if outgoing references should be adjusted, <code>false</code> otherwise
	 */
	public final void setOutgoing(final boolean outgoing) {
		fOutgoing= outgoing;
	}

	/**
	 * Sets the AST rewrite to use for member visibility adjustments.
	 * <p>
	 * This method must be called before calling {@link MemberVisibilityAdjustor#adjustVisibility(IProgressMonitor)}. The default is to use a compilation unit rewrite.
	 * 
	 * @param rewrite the AST rewrite to set
	 * @param root the root of the AST used in the rewrite
	 */
	public final void setRewrite(final ASTRewrite rewrite, final JavaScriptUnit root) {
		Assert.isTrue(rewrite == null || root != null);
		fRewrite= rewrite;
		fRoot= root;
	}

	/**
	 * Sets the compilation unit rewrites used by this adjustor (element type: Map <IJavaScriptUnit, CompilationUnitRewrite>).
	 * <p>
	 * This method must be called before calling {@link MemberVisibilityAdjustor#adjustVisibility(IProgressMonitor)}. The default is to use no existing rewrites.
	 * 
	 * @param rewrites the map of compilation units to compilation unit rewrites to set
	 */
	public final void setRewrites(final Map rewrites) {
		Assert.isNotNull(rewrites);
		fRewrites= rewrites;
	}

	/**
	 * Sets the incoming search scope used by this adjustor.
	 * <p>
	 * This method must be called before calling {@link MemberVisibilityAdjustor#adjustVisibility(IProgressMonitor)}. The default is the whole workspace as scope.
	 * 
	 * @param scope the search scope to set
	 */
	public final void setScope(final IJavaScriptSearchScope scope) {
		Assert.isNotNull(scope);
		fScope= scope;
	}

	/**
	 * Sets the working copy owner used by this adjustor.
	 * <p>
	 * This method must be called before calling {@link MemberVisibilityAdjustor#adjustVisibility(IProgressMonitor)}. The default is to use none.
	 * 
	 * @param owner the working copy owner, or <code>null</code> to use none
	 */
	public final void setOwner(final WorkingCopyOwner owner) {
		fOwner= owner;
	}

	/**
	 * Sets the refactoring status used by this adjustor.
	 * <p>
	 * This method must be called before calling {@link MemberVisibilityAdjustor#adjustVisibility(IProgressMonitor)}. The default is a fresh status with status {@link RefactoringStatus#OK}.
	 * 
	 * @param status the refactoring status to set
	 */
	public final void setStatus(final RefactoringStatus status) {
		Assert.isNotNull(status);
		fStatus= status;
	}

	/**
	 * Sets the severity of visibility messages.
	 * <p>
	 * This method must be called before calling {@link MemberVisibilityAdjustor#adjustVisibility(IProgressMonitor)}. The default is a status with value {@link RefactoringStatus#WARNING}.
	 * 
	 * @param severity the severity of visibility messages
	 */
	public final void setVisibilitySeverity(final int severity) {
		Assert.isTrue(isStatusSeverity(severity));
		fVisibilitySeverity= severity;
	}

	/**
	 * Returns the visibility threshold from a type to a field.
	 * 
	 * @param referencing the referencing type
	 * @param referenced the referenced field
	 * @param monitor the progress monitor to use
	 * @return the visibility keyword corresponding to the threshold, or <code>null</code> for default visibility
	 * @throws JavaScriptModelException if the java elements could not be accessed
	 */
	private ModifierKeyword thresholdTypeToField(final IType referencing, final IField referenced, final IProgressMonitor monitor) throws JavaScriptModelException {
		ModifierKeyword keyword= ModifierKeyword.PUBLIC_KEYWORD;
		final IJavaScriptUnit referencedUnit= referenced.getJavaScriptUnit();
		if (referenced.getDeclaringType().equals(referencing))
			keyword= ModifierKeyword.PRIVATE_KEYWORD;
		else {
			final ITypeHierarchy hierarchy= getTypeHierarchy(referencing, new SubProgressMonitor(monitor, 1));
			final IType superType= hierarchy.getSuperclass(referencing);
			
			if (superType.equals(referenced.getDeclaringType())) {
				keyword= ModifierKeyword.PROTECTED_KEYWORD;
				return keyword;
			}
		}
		final IJavaScriptUnit typeUnit= referencing.getJavaScriptUnit();
		if (referencedUnit != null && referencedUnit.equals(typeUnit))
			keyword= ModifierKeyword.PRIVATE_KEYWORD;
		else if (referencedUnit != null && typeUnit != null && referencedUnit.getParent().equals(typeUnit.getParent()))
			keyword= null;
		return keyword;
	}

	/**
	 * Returns the visibility threshold from a type to a method.
	 * 
	 * @param referencing the referencing type
	 * @param referenced the referenced method
	 * @param monitor the progress monitor to use
	 * @return the visibility keyword corresponding to the threshold, or <code>null</code> for default visibility
	 * @throws JavaScriptModelException if the java elements could not be accessed
	 */
	private ModifierKeyword thresholdTypeToMethod(final IType referencing, final IFunction referenced, final IProgressMonitor monitor) throws JavaScriptModelException {
		final IJavaScriptUnit referencedUnit= referenced.getJavaScriptUnit();
		ModifierKeyword keyword= ModifierKeyword.PUBLIC_KEYWORD;
		if (referenced.getDeclaringType().equals(referencing))
			keyword= ModifierKeyword.PRIVATE_KEYWORD;
		else {
			final ITypeHierarchy hierarchy= getTypeHierarchy(referencing, new SubProgressMonitor(monitor, 1));
			final IType superType= hierarchy.getSuperclass(referencing);

			if (superType.equals(referenced.getDeclaringType())) {
				keyword= ModifierKeyword.PROTECTED_KEYWORD;
				return keyword;
			}
			
		}
		final IJavaScriptUnit typeUnit= referencing.getJavaScriptUnit();
		if (referencedUnit != null && referencedUnit.equals(typeUnit)) {
			if (referenced.getDeclaringType().getDeclaringType() != null)
				keyword= null;
			else
				keyword= ModifierKeyword.PRIVATE_KEYWORD;
		} else if (referencedUnit != null && referencedUnit.getParent().equals(typeUnit.getParent()))
			keyword= null;
		return keyword;
	}

	/**
	 * Returns the visibility threshold from a type to another type.
	 * 
	 * @param referencing the referencing type
	 * @param referenced the referenced type
	 * @param monitor the progress monitor to use
	 * @return the visibility keyword corresponding to the threshold, or <code>null</code> for default visibility
	 * @throws JavaScriptModelException if the java elements could not be accessed
	 */
	private ModifierKeyword thresholdTypeToType(final IType referencing, final IType referenced, final IProgressMonitor monitor) throws JavaScriptModelException {
		ModifierKeyword keyword= ModifierKeyword.PUBLIC_KEYWORD;
		final IJavaScriptUnit referencedUnit= referenced.getJavaScriptUnit();
		if (referencing.equals(referenced.getDeclaringType()))
			keyword= ModifierKeyword.PRIVATE_KEYWORD;
		else {
			final ITypeHierarchy hierarchy= getTypeHierarchy(referencing, new SubProgressMonitor(monitor, 1));
			final IType superType = hierarchy.getSuperclass(referencing);
			
			if (superType.equals(referenced)) {
				keyword= null;
				return keyword;
			}
		}
		final IJavaScriptUnit typeUnit= referencing.getJavaScriptUnit();
		if (referencedUnit != null && referencedUnit.equals(typeUnit)) {
			if (referenced.getDeclaringType() != null)
				keyword= null;
			else
				keyword= ModifierKeyword.PRIVATE_KEYWORD;
		} else if (referencedUnit != null && typeUnit != null && referencedUnit.getParent().equals(typeUnit.getParent()))
			keyword= null;
		return keyword;
	}
}
