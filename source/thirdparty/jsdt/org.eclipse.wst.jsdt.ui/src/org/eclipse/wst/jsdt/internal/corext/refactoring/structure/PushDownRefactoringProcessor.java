/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTRequestor;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.JavaScriptRefactoringDescriptor;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.wst.jsdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TextEditBasedChangeManager;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.SearchUtils;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * Refactoring processor for the push down refactoring.
 * 
 * 
 */
public final class PushDownRefactoringProcessor extends HierarchyProcessor {

	public static class MemberActionInfo implements IMemberActionInfo {

		public static final int NO_ACTION= 2;

		public static final int PUSH_ABSTRACT_ACTION= 1;

		public static final int PUSH_DOWN_ACTION= 0;

		private static void assertValidAction(IMember member, int action) {
			if (member instanceof IFunction)
				Assert.isTrue(action == PUSH_ABSTRACT_ACTION || action == NO_ACTION || action == PUSH_DOWN_ACTION);
			else if (member instanceof IField)
				Assert.isTrue(action == NO_ACTION || action == PUSH_DOWN_ACTION);
		}

		public static MemberActionInfo create(IMember member, int action) {
			return new MemberActionInfo(member, action);
		}

		static IMember[] getMembers(MemberActionInfo[] infos) {
			IMember[] result= new IMember[infos.length];
			for (int i= 0; i < result.length; i++) {
				result[i]= infos[i].getMember();
			}
			return result;
		}

		private int fAction;

		private final IMember fMember;

		private MemberActionInfo(IMember member, int action) {
			assertValidAction(member, action);
			Assert.isTrue(member instanceof IField || member instanceof IFunction);
			fMember= member;
			fAction= action;
		}

		boolean copyJavadocToCopiesInSubclasses() {
			return isToBeDeletedFromDeclaringClass();
		}

		public int getAction() {
			return fAction;
		}

		public int[] getAvailableActions() {
			if (isFieldInfo())
				return new int[] { PUSH_DOWN_ACTION, NO_ACTION };

			return new int[] { PUSH_DOWN_ACTION, PUSH_ABSTRACT_ACTION, NO_ACTION };
		}

		public IMember getMember() {
			return fMember;
		}

		int getNewModifiersForCopyInSubclass(int oldModifiers) throws JavaScriptModelException {
			if (isFieldInfo())
				return oldModifiers;
			if (isToBeDeletedFromDeclaringClass())
				return oldModifiers;
			int modifiers= oldModifiers;
			if (isNewMethodToBeDeclaredAbstract()) {
				if (!JdtFlags.isPublic(fMember))
					modifiers= Modifier.PROTECTED | JdtFlags.clearAccessModifiers(modifiers);
			}
			return modifiers;
		}

		int getNewModifiersForOriginal(int oldModifiers) throws JavaScriptModelException {
			if (isFieldInfo())
				return oldModifiers;
			if (isToBeDeletedFromDeclaringClass())
				return oldModifiers;
			int modifiers= oldModifiers;
			if (isNewMethodToBeDeclaredAbstract()) {
				modifiers= JdtFlags.clearFlag(Modifier.FINAL | Modifier.NATIVE, oldModifiers);
				modifiers|= Modifier.ABSTRACT;

				if (!JdtFlags.isPublic(fMember))
					modifiers= Modifier.PROTECTED | JdtFlags.clearAccessModifiers(modifiers);
			}
			return modifiers;
		}

		public boolean isActive() {
			return getAction() != NO_ACTION;
		}

		public boolean isEditable() {
			if (isFieldInfo())
				return false;
			if (getAction() == MemberActionInfo.NO_ACTION)
				return false;
			return true;
		}

		boolean isFieldInfo() {
			return fMember instanceof IField;
		}

		boolean isNewMethodToBeDeclaredAbstract() throws JavaScriptModelException {
			return !isFieldInfo() && !JdtFlags.isAbstract(fMember) && fAction == PUSH_ABSTRACT_ACTION;
		}

		boolean isToBeCreatedInSubclassesOfDeclaringClass() {
			return fAction != NO_ACTION;
		}

		boolean isToBeDeletedFromDeclaringClass() {
			return isToBePushedDown();
		}

		public boolean isToBePushedDown() {
			return fAction == PUSH_DOWN_ACTION;
		}

		public void setAction(int action) {
			assertValidAction(fMember, action);
			if (isFieldInfo())
				Assert.isTrue(action != PUSH_ABSTRACT_ACTION);
			fAction= action;
		}

	}

	private static final String ATTRIBUTE_ABSTRACT= "abstract"; //$NON-NLS-1$

	private static final String ATTRIBUTE_PUSH= "push"; //$NON-NLS-1$

	/** The identifier of this processor */
	public static final String IDENTIFIER= "org.eclipse.wst.jsdt.ui.pushDownProcessor"; //$NON-NLS-1$

	/** The push down group category set */
	private static final GroupCategorySet SET_PUSH_DOWN= new GroupCategorySet(new GroupCategory("org.eclipse.wst.jsdt.internal.corext.pushDown", //$NON-NLS-1$
			RefactoringCoreMessages.PushDownRefactoring_category_name, RefactoringCoreMessages.PushDownRefactoring_category_description));

	private static MemberActionInfo[] createInfosForAllPushableFieldsAndMethods(IType type) throws JavaScriptModelException {
		List result= new ArrayList();
		IMember[] pushableMembers= RefactoringAvailabilityTester.getPushDownMembers(type);
		for (int i= 0; i < pushableMembers.length; i++) {
			result.add(MemberActionInfo.create(pushableMembers[i], MemberActionInfo.NO_ACTION));
		}
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	private static IMember[] getAbstractMembers(IMember[] members) throws JavaScriptModelException {
		List result= new ArrayList(members.length);
		for (int i= 0; i < members.length; i++) {
			IMember member= members[i];
			if (JdtFlags.isAbstract(member))
				result.add(member);
		}
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}

	private static CompilationUnitRewrite getCompilationUnitRewrite(final Map rewrites, final IJavaScriptUnit unit) {
		Assert.isNotNull(rewrites);
		Assert.isNotNull(unit);
		CompilationUnitRewrite rewrite= (CompilationUnitRewrite) rewrites.get(unit);
		if (rewrite == null) {
			rewrite= new CompilationUnitRewrite(unit);
			rewrites.put(unit, rewrite);
		}
		return rewrite;
	}

	private static IJavaScriptElement[] getReferencingElementsFromSameClass(IMember member, IProgressMonitor pm, RefactoringStatus status) throws JavaScriptModelException {
		Assert.isNotNull(member);
		final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(SearchPattern.createPattern(member, IJavaScriptSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE));
		engine.setFiltering(true, true);
		engine.setScope(SearchEngine.createJavaSearchScope(new IJavaScriptElement[] { member.getDeclaringType() }));
		engine.setStatus(status);
		engine.searchPattern(new SubProgressMonitor(pm, 1));
		SearchResultGroup[] groups= (SearchResultGroup[]) engine.getResults();
		Set result= new HashSet(3);
		for (int i= 0; i < groups.length; i++) {
			SearchResultGroup group= groups[i];
			SearchMatch[] results= group.getSearchResults();
			for (int j= 0; j < results.length; j++) {
				SearchMatch searchResult= results[i];
				result.add(SearchUtils.getEnclosingJavaElement(searchResult));
			}
		}
		return (IJavaScriptElement[]) result.toArray(new IJavaScriptElement[result.size()]);
	}

	private ITypeHierarchy fCachedClassHierarchy;

	private MemberActionInfo[] fMemberInfos;

	/**
	 * Creates a new push down refactoring processor.
	 * 
	 * @param members
	 *            the members to pull up, or <code>null</code> if invoked by
	 *            scripting
	 */
	public PushDownRefactoringProcessor(IMember[] members) {
		super(members, null, false);
		if (members != null) {
			final IType type= RefactoringAvailabilityTester.getTopLevelType(members);
			try {
				if (type != null && RefactoringAvailabilityTester.getPushDownMembers(type).length != 0) {
					fMembersToMove= new IMember[0];
					fCachedDeclaringType= type;
				}
			} catch (JavaScriptModelException exception) {
				JavaScriptPlugin.log(exception);
			}
		}
	}

	private void addAllRequiredPushableMembers(List queue, IMember member, IProgressMonitor monitor) throws JavaScriptModelException {
		monitor.beginTask(RefactoringCoreMessages.PushDownRefactoring_calculating_required, 2);
		IProgressMonitor sub= new SubProgressMonitor(monitor, 1);
		sub.beginTask(RefactoringCoreMessages.PushDownRefactoring_calculating_required, 2);
		IFunction[] requiredMethods= ReferenceFinderUtil.getMethodsReferencedIn(new IJavaScriptElement[] { member }, new SubProgressMonitor(sub, 1));
		sub= new SubProgressMonitor(sub, 1);
		sub.beginTask(RefactoringCoreMessages.PushDownRefactoring_calculating_required, requiredMethods.length);
		for (int index= 0; index < requiredMethods.length; index++) {
			IFunction method= requiredMethods[index];
			if (!MethodChecks.isVirtual(method) && (method.getDeclaringType().equals(getDeclaringType()) && !queue.contains(method) && RefactoringAvailabilityTester.isPushDownAvailable(method)))
				queue.add(method);
		}
		sub.done();
		IField[] requiredFields= ReferenceFinderUtil.getFieldsReferencedIn(new IJavaScriptElement[] { member }, new SubProgressMonitor(monitor, 1));
		for (int index= 0; index < requiredFields.length; index++) {
			IField field= requiredFields[index];
			if (field.getDeclaringType().equals(getDeclaringType()) && !queue.contains(field) && RefactoringAvailabilityTester.isPushDownAvailable(field))
				queue.add(field);
		}
		monitor.done();
	}

	private RefactoringStatus checkAbstractMembersInDestinationClasses(IMember[] membersToPushDown, IType[] destinationClassesForAbstract) throws JavaScriptModelException {
		RefactoringStatus result= new RefactoringStatus();
		IMember[] abstractMembersToPushDown= getAbstractMembers(membersToPushDown);
		for (int index= 0; index < destinationClassesForAbstract.length; index++) {
			result.merge(MemberCheckUtil.checkMembersInDestinationType(abstractMembersToPushDown, destinationClassesForAbstract[index]));
		}
		return result;
	}

	private RefactoringStatus checkAccessedFields(IType[] subclasses, IProgressMonitor pm) throws JavaScriptModelException {
		RefactoringStatus result= new RefactoringStatus();
		IMember[] membersToPushDown= MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());
		List pushedDownList= Arrays.asList(membersToPushDown);
		IField[] accessedFields= ReferenceFinderUtil.getFieldsReferencedIn(membersToPushDown, pm);
		for (int i= 0; i < subclasses.length; i++) {
			IType targetClass= subclasses[i];
			ITypeHierarchy targetSupertypes= targetClass.newSupertypeHierarchy(null);
			for (int j= 0; j < accessedFields.length; j++) {
				IField field= accessedFields[j];
				boolean isAccessible= pushedDownList.contains(field) || canBeAccessedFrom(field, targetClass, targetSupertypes);
				if (!isAccessible) {
					String message= Messages.format(RefactoringCoreMessages.PushDownRefactoring_field_not_accessible, new String[] { JavaScriptElementLabels.getTextLabel(field, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getTextLabel(targetClass, JavaScriptElementLabels.ALL_FULLY_QUALIFIED) });
					result.addError(message, JavaStatusContext.create(field));
				}
			}
		}
		pm.done();
		return result;
	}

	private RefactoringStatus checkAccessedMethods(IType[] subclasses, IProgressMonitor pm) throws JavaScriptModelException {
		RefactoringStatus result= new RefactoringStatus();
		IMember[] membersToPushDown= MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());
		List pushedDownList= Arrays.asList(membersToPushDown);
		IFunction[] accessedMethods= ReferenceFinderUtil.getMethodsReferencedIn(membersToPushDown, pm);
		for (int index= 0; index < subclasses.length; index++) {
			IType targetClass= subclasses[index];
			ITypeHierarchy targetSupertypes= targetClass.newSupertypeHierarchy(null);
			for (int offset= 0; offset < accessedMethods.length; offset++) {
				IFunction method= accessedMethods[offset];
				boolean isAccessible= pushedDownList.contains(method) || canBeAccessedFrom(method, targetClass, targetSupertypes);
				if (!isAccessible) {
					String message= Messages.format(RefactoringCoreMessages.PushDownRefactoring_method_not_accessible, new String[] { JavaScriptElementLabels.getTextLabel(method, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getTextLabel(targetClass, JavaScriptElementLabels.ALL_FULLY_QUALIFIED) });
					result.addError(message, JavaStatusContext.create(method));
				}
			}
		}
		pm.done();
		return result;
	}

	private RefactoringStatus checkAccessedTypes(IType[] subclasses, IProgressMonitor pm) throws JavaScriptModelException {
		RefactoringStatus result= new RefactoringStatus();
		IType[] accessedTypes= getTypesReferencedInMovedMembers(pm);
		for (int index= 0; index < subclasses.length; index++) {
			IType targetClass= subclasses[index];
			ITypeHierarchy targetSupertypes= targetClass.newSupertypeHierarchy(null);
			for (int offset= 0; offset < accessedTypes.length; offset++) {
				IType type= accessedTypes[offset];
				if (!canBeAccessedFrom(type, targetClass, targetSupertypes)) {
					String message= Messages.format(RefactoringCoreMessages.PushDownRefactoring_type_not_accessible, new String[] { JavaScriptElementLabels.getTextLabel(type, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getTextLabel(targetClass, JavaScriptElementLabels.ALL_FULLY_QUALIFIED) });
					result.addError(message, JavaStatusContext.create(type));
				}
			}
		}
		pm.done();
		return result;
	}

	private RefactoringStatus checkElementsAccessedByModifiedMembers(IProgressMonitor pm) throws JavaScriptModelException {
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask(RefactoringCoreMessages.PushDownRefactoring_check_references, 3);
		IType[] subclasses= getAbstractDestinations(new SubProgressMonitor(pm, 1));
		result.merge(checkAccessedTypes(subclasses, new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedFields(subclasses, new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedMethods(subclasses, new SubProgressMonitor(pm, 1)));
		pm.done();
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor monitor, CheckConditionsContext context) throws CoreException, OperationCanceledException {
		try {
			monitor.beginTask(RefactoringCoreMessages.PushDownRefactoring_checking, 5);
			clearCaches();
			IJavaScriptUnit unit= getDeclaringType().getJavaScriptUnit();
			if (fLayer)
				unit= unit.findWorkingCopy(fOwner);
			resetWorkingCopies(unit);
			final RefactoringStatus result= new RefactoringStatus();
			result.merge(checkMembersInDestinationClasses(new SubProgressMonitor(monitor, 1)));
			result.merge(checkElementsAccessedByModifiedMembers(new SubProgressMonitor(monitor, 1)));
			result.merge(checkReferencesToPushedDownMembers(new SubProgressMonitor(monitor, 1)));
			if (!JdtFlags.isAbstract(getDeclaringType()) && getAbstractDeclarationInfos().length != 0)
				result.merge(checkConstructorCalls(getDeclaringType(), new SubProgressMonitor(monitor, 1)));
			else
				monitor.worked(1);
			if (result.hasFatalError())
				return result;
			List members= new ArrayList(fMemberInfos.length);
			for (int index= 0; index < fMemberInfos.length; index++) {
				if (fMemberInfos[index].getAction() != MemberActionInfo.NO_ACTION)
					members.add(fMemberInfos[index].getMember());
			}
			fMembersToMove= (IMember[]) members.toArray(new IMember[members.size()]);
			fChangeManager= createChangeManager(new SubProgressMonitor(monitor, 1), result);
			if (result.hasFatalError())
				return result;
			result.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits()), getRefactoring().getValidationContext()));
			return result;
		} finally {
			monitor.done();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		try {
			monitor.beginTask(RefactoringCoreMessages.PushDownRefactoring_checking, 1);
			RefactoringStatus status= new RefactoringStatus();
			status.merge(checkPossibleSubclasses(new SubProgressMonitor(monitor, 1)));
			if (status.hasFatalError())
				return status;
			status.merge(checkDeclaringType(new SubProgressMonitor(monitor, 1)));
			if (status.hasFatalError())
				return status;
			status.merge(checkIfMembersExist());
			if (status.hasFatalError())
				return status;
			fMemberInfos= createInfosForAllPushableFieldsAndMethods(getDeclaringType());
			List list= Arrays.asList(fMembersToMove);
			for (int offset= 0; offset < fMemberInfos.length; offset++) {
				MemberActionInfo info= fMemberInfos[offset];
				if (list.contains(info.getMember()))
					info.setAction(MemberActionInfo.PUSH_DOWN_ACTION);
			}
			return status;
		} finally {
			monitor.done();
		}
	}

	private RefactoringStatus checkMembersInDestinationClasses(IProgressMonitor monitor) throws JavaScriptModelException {
		monitor.beginTask(RefactoringCoreMessages.PushDownRefactoring_checking, 2);
		RefactoringStatus result= new RefactoringStatus();
		IMember[] membersToPushDown= MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());

		IType[] destinationClassesForNonAbstract= getAbstractDestinations(new SubProgressMonitor(monitor, 1));
		result.merge(checkNonAbstractMembersInDestinationClasses(membersToPushDown, destinationClassesForNonAbstract));
		List list= Arrays.asList(getAbstractMembers(getAbstractDestinations(new SubProgressMonitor(monitor, 1))));

		IType[] destinationClassesForAbstract= (IType[]) list.toArray(new IType[list.size()]);
		result.merge(checkAbstractMembersInDestinationClasses(membersToPushDown, destinationClassesForAbstract));
		monitor.done();
		return result;
	}

	private RefactoringStatus checkNonAbstractMembersInDestinationClasses(IMember[] membersToPushDown, IType[] destinationClassesForNonAbstract) throws JavaScriptModelException {
		RefactoringStatus result= new RefactoringStatus();
		List list= new ArrayList(); // Arrays.asList does not support removing
		list.addAll(Arrays.asList(membersToPushDown));
		list.removeAll(Arrays.asList(getAbstractMembers(membersToPushDown)));
		IMember[] nonAbstractMembersToPushDown= (IMember[]) list.toArray(new IMember[list.size()]);
		for (int i= 0; i < destinationClassesForNonAbstract.length; i++) {
			result.merge(MemberCheckUtil.checkMembersInDestinationType(nonAbstractMembersToPushDown, destinationClassesForNonAbstract[i]));
		}
		return result;
	}

	private RefactoringStatus checkPossibleSubclasses(IProgressMonitor pm) throws JavaScriptModelException {
		IType[] modifiableSubclasses= getAbstractDestinations(pm);
		if (modifiableSubclasses.length == 0) {
			String msg= Messages.format(RefactoringCoreMessages.PushDownRefactoring_no_subclasses, new String[] { JavaScriptElementLabels.getTextLabel(getDeclaringType(), JavaScriptElementLabels.ALL_FULLY_QUALIFIED) });
			return RefactoringStatus.createFatalErrorStatus(msg);
		}
		return new RefactoringStatus();
	}

	private RefactoringStatus checkReferencesToPushedDownMembers(IProgressMonitor monitor) throws JavaScriptModelException {
		List fields= new ArrayList(fMemberInfos.length);
		for (int index= 0; index < fMemberInfos.length; index++) {
			MemberActionInfo info= fMemberInfos[index];
			if (info.isToBePushedDown())
				fields.add(info.getMember());
		}
		IMember[] membersToPush= (IMember[]) fields.toArray(new IMember[fields.size()]);
		RefactoringStatus result= new RefactoringStatus();
		List movedMembers= Arrays.asList(MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass()));
		monitor.beginTask(RefactoringCoreMessages.PushDownRefactoring_check_references, membersToPush.length);
		for (int index= 0; index < membersToPush.length; index++) {
			IMember member= membersToPush[index];
			String label= createLabel(member);
			IJavaScriptElement[] referencing= getReferencingElementsFromSameClass(member, new SubProgressMonitor(monitor, 1), result);
			for (int offset= 0; offset < referencing.length; offset++) {
				IJavaScriptElement element= referencing[offset];
				if (movedMembers.contains(element))
					continue;
				if (!(element instanceof IMember))
					continue;
				IMember referencingMember= (IMember) element;
				Object[] keys= { label, createLabel(referencingMember) };
				String msg= Messages.format(RefactoringCoreMessages.PushDownRefactoring_referenced, keys);
				result.addError(msg, JavaStatusContext.create(referencingMember));
			}
		}
		monitor.done();
		return result;
	}

	public void computeAdditionalRequiredMembersToPushDown(IProgressMonitor monitor) throws JavaScriptModelException {
		List list= Arrays.asList(getAdditionalRequiredMembers(monitor));
		for (int index= 0; index < fMemberInfos.length; index++) {
			MemberActionInfo info= fMemberInfos[index];
			if (list.contains(info.getMember()))
				info.setAction(MemberActionInfo.PUSH_DOWN_ACTION);
		}
	}

	private void copyBodyOfPushedDownMethod(ASTRewrite targetRewrite, IFunction method, FunctionDeclaration oldMethod, FunctionDeclaration newMethod, TypeVariableMaplet[] mapping) throws JavaScriptModelException {
		Block body= oldMethod.getBody();
		if (body == null) {
			newMethod.setBody(null);
			return;
		}
		try {
			final IDocument document= new Document(method.getJavaScriptUnit().getBuffer().getContents());
			final ASTRewrite rewriter= ASTRewrite.create(body.getAST());
			final ITrackedNodePosition position= rewriter.track(body);
			body.accept(new TypeVariableMapper(rewriter, mapping));
			rewriter.rewriteAST(document, getDeclaringType().getJavaScriptUnit().getJavaScriptProject().getOptions(true)).apply(document, TextEdit.NONE);
			String content= document.get(position.getStartPosition(), position.getLength());
			String[] lines= Strings.convertIntoLines(content);
			Strings.trimIndentation(lines, method.getJavaScriptProject(), false);
			content= Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(method));
			newMethod.setBody((Block) targetRewrite.createStringPlaceholder(content, ASTNode.BLOCK));
		} catch (MalformedTreeException exception) {
			JavaScriptPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaScriptPlugin.log(exception);
		}
	}

	private void copyMembers(Collection adjustors, Map adjustments, Map rewrites, RefactoringStatus status, MemberActionInfo[] infos, IType[] destinations, CompilationUnitRewrite sourceRewriter, CompilationUnitRewrite unitRewriter, IProgressMonitor monitor) throws JavaScriptModelException {
		try {
			monitor.beginTask(RefactoringCoreMessages.PushDownRefactoring_checking, 1);
			IType type= null;
			TypeVariableMaplet[] mapping= null;
			for (int index= 0; index < destinations.length; index++) {
				type= destinations[index];
				if (unitRewriter.getCu().equals(type.getJavaScriptUnit())) {
					IMember member= null;
					MemberVisibilityAdjustor adjustor= null;
					AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(type, unitRewriter.getRoot());
					for (int offset= infos.length - 1; offset >= 0; offset--) {
						member= infos[offset].getMember();
						adjustor= new MemberVisibilityAdjustor(type, member);
						if (infos[offset].isNewMethodToBeDeclaredAbstract())
							adjustor.setIncoming(false);
						adjustor.setRewrite(sourceRewriter.getASTRewrite(), sourceRewriter.getRoot());
						adjustor.setRewrites(rewrites);

						// TW: set to error if bug 78387 is fixed
						adjustor.setFailureSeverity(RefactoringStatus.WARNING);

						adjustor.setStatus(status);
						adjustor.setAdjustments(adjustments);
						adjustor.adjustVisibility(new SubProgressMonitor(monitor, 1));
						adjustments.remove(member);
						adjustors.add(adjustor);
						status.merge(checkProjectCompliance(getCompilationUnitRewrite(rewrites, getDeclaringType().getJavaScriptUnit()), type, new IMember[] {infos[offset].getMember()}));
						if (infos[offset].isFieldInfo()) {
							final VariableDeclarationFragment oldField= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField) infos[offset].getMember(), sourceRewriter.getRoot());
							if (oldField != null) {
								FieldDeclaration newField= createNewFieldDeclarationNode(infos[offset], sourceRewriter.getRoot(), mapping, unitRewriter.getASTRewrite(), oldField);
								unitRewriter.getASTRewrite().getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newField, ASTNodes.getInsertionIndex(newField, declaration.bodyDeclarations()), unitRewriter.createCategorizedGroupDescription(RefactoringCoreMessages.HierarchyRefactoring_add_member, SET_PUSH_DOWN));
								ImportRewriteUtil.addImports(unitRewriter, oldField.getParent(), new HashMap(), new HashMap(), false);
							}
						} else {
							final FunctionDeclaration oldMethod= ASTNodeSearchUtil.getMethodDeclarationNode((IFunction) infos[offset].getMember(), sourceRewriter.getRoot());
							if (oldMethod != null) {
								FunctionDeclaration newMethod= createNewMethodDeclarationNode(infos[offset], sourceRewriter.getRoot(), mapping, unitRewriter, oldMethod);
								unitRewriter.getASTRewrite().getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newMethod, ASTNodes.getInsertionIndex(newMethod, declaration.bodyDeclarations()), unitRewriter.createCategorizedGroupDescription(RefactoringCoreMessages.HierarchyRefactoring_add_member, SET_PUSH_DOWN));
								ImportRewriteUtil.addImports(unitRewriter, oldMethod, new HashMap(), new HashMap(), false);
							}
						}
					}
				}
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		try {
			final Map arguments= new HashMap();
			String project= null;
			final IType declaring= getDeclaringType();
			final IJavaScriptProject javaProject= declaring.getJavaScriptProject();
			if (javaProject != null)
				project= javaProject.getElementName();
			int flags= JavaScriptRefactoringDescriptor.JAR_MIGRATION | JavaScriptRefactoringDescriptor.JAR_REFACTORING | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
			try {
				if (declaring.isLocal() || declaring.isAnonymous())
					flags|= JavaScriptRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
			} catch (JavaScriptModelException exception) {
				JavaScriptPlugin.log(exception);
			}
			final String description= fMembersToMove.length == 1 ? Messages.format(RefactoringCoreMessages.PushDownRefactoring_descriptor_description_short_multi, fMembersToMove[0].getElementName()) : RefactoringCoreMessages.PushDownRefactoring_descriptor_description_short;
			final String header= fMembersToMove.length == 1 ? Messages.format(RefactoringCoreMessages.PushDownRefactoring_descriptor_description_full, new String[] { JavaScriptElementLabels.getElementLabel(fMembersToMove[0], JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getElementLabel(declaring, JavaScriptElementLabels.ALL_FULLY_QUALIFIED) }) : Messages.format(RefactoringCoreMessages.PushDownRefactoring_descriptor_description, new String[] { JavaScriptElementLabels.getElementLabel(declaring, JavaScriptElementLabels.ALL_FULLY_QUALIFIED) });
			final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
			final String[] settings= new String[fMembersToMove.length];
			for (int index= 0; index < settings.length; index++)
				settings[index]= JavaScriptElementLabels.getElementLabel(fMembersToMove[index], JavaScriptElementLabels.ALL_FULLY_QUALIFIED);
			comment.addSetting(JDTRefactoringDescriptorComment.createCompositeSetting(RefactoringCoreMessages.PushDownRefactoring_pushed_members_pattern, settings));
			addSuperTypeSettings(comment, true);
			final JDTRefactoringDescriptor descriptor= new JDTRefactoringDescriptor(IJavaScriptRefactorings.PUSH_DOWN, project, description, comment.asString(), arguments, flags);
			if (fCachedDeclaringType != null)
				arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fCachedDeclaringType));
			for (int index= 0; index < fMembersToMove.length; index++) {
				arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (index + 1), descriptor.elementToHandle(fMembersToMove[index]));
				for (int offset= 0; offset < fMemberInfos.length; offset++) {
					if (fMemberInfos[offset].getMember().equals(fMembersToMove[index])) {
						switch (fMemberInfos[offset].getAction()) {
							case MemberActionInfo.PUSH_ABSTRACT_ACTION:
								arguments.put(ATTRIBUTE_ABSTRACT + (index + 1), Boolean.valueOf(true).toString());
								break;
							case MemberActionInfo.PUSH_DOWN_ACTION:
								arguments.put(ATTRIBUTE_PUSH + (index + 1), Boolean.valueOf(true).toString());
								break;
						}
					}
				}
			}
			return new DynamicValidationRefactoringChange(descriptor, RefactoringCoreMessages.PushDownRefactoring_change_name, fChangeManager.getAllChanges());
		} finally {
			pm.done();
			clearCaches();
		}
	}

	private TextEditBasedChangeManager createChangeManager(final IProgressMonitor monitor, final RefactoringStatus status) throws CoreException {
		Assert.isNotNull(monitor);
		Assert.isNotNull(status);
		try {
			monitor.beginTask(RefactoringCoreMessages.PushDownRefactoring_checking, 7);
			final IJavaScriptUnit source= getDeclaringType().getJavaScriptUnit();
			final CompilationUnitRewrite sourceRewriter= new CompilationUnitRewrite(source);
			final Map rewrites= new HashMap(2);
			rewrites.put(source, sourceRewriter);
			IType[] types= getHierarchyOfDeclaringClass(new SubProgressMonitor(monitor, 1)).getSubclasses(getDeclaringType());
			final Set result= new HashSet(types.length + 1);
			for (int index= 0; index < types.length; index++)
				result.add(types[index].getJavaScriptUnit());
			result.add(source);
			final Map adjustments= new HashMap();
			final List adjustors= new ArrayList();
			final IJavaScriptUnit[] units= (IJavaScriptUnit[]) result.toArray(new IJavaScriptUnit[result.size()]);
			IJavaScriptUnit unit= null;
			CompilationUnitRewrite rewrite= null;
			final IProgressMonitor sub= new SubProgressMonitor(monitor, 4);
			try {
				sub.beginTask(RefactoringCoreMessages.PushDownRefactoring_checking, units.length * 4);
				for (int index= 0; index < units.length; index++) {
					unit= units[index];
					rewrite= getCompilationUnitRewrite(rewrites, unit);
					if (unit.equals(sourceRewriter.getCu())) {
						final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(getDeclaringType(), rewrite.getRoot());
						if (!JdtFlags.isAbstract(getDeclaringType()) && getAbstractDeclarationInfos().length != 0)
							ModifierRewrite.create(rewrite.getASTRewrite(), declaration).setModifiers((Modifier.ABSTRACT | declaration.getModifiers()), rewrite.createCategorizedGroupDescription(RefactoringCoreMessages.PushDownRefactoring_make_abstract, SET_PUSH_DOWN));
						deleteDeclarationNodes(sourceRewriter, false, rewrite, Arrays.asList(getDeletableMembers()), SET_PUSH_DOWN);
						MemberActionInfo[] methods= getAbstractDeclarationInfos();
						for (int offset= 0; offset < methods.length; offset++)
							declareMethodAbstract(methods[offset], sourceRewriter, rewrite);
					}
					final IMember[] members= getAbstractMembers(getAbstractDestinations(new SubProgressMonitor(monitor, 1)));
					final IType[] classes= new IType[members.length];
					for (int offset= 0; offset < members.length; offset++)
						classes[offset]= (IType) members[offset];
					copyMembers(adjustors, adjustments, rewrites, status, getAbstractMemberInfos(), classes, sourceRewriter, rewrite, sub);
					copyMembers(adjustors, adjustments, rewrites, status, getEffectedMemberInfos(), getAbstractDestinations(new SubProgressMonitor(monitor, 1)), sourceRewriter, rewrite, sub);
					if (monitor.isCanceled())
						throw new OperationCanceledException();
				}
			} finally {
				sub.done();
			}
			if (!adjustors.isEmpty() && !adjustments.isEmpty()) {
				final MemberVisibilityAdjustor adjustor= (MemberVisibilityAdjustor) adjustors.get(0);
				adjustor.rewriteVisibility(new SubProgressMonitor(monitor, 1));
			}
			final TextEditBasedChangeManager manager= new TextEditBasedChangeManager();
			for (final Iterator iterator= rewrites.keySet().iterator(); iterator.hasNext();) {
				unit= (IJavaScriptUnit) iterator.next();
				rewrite= (CompilationUnitRewrite) rewrites.get(unit);
				if (rewrite != null)
					manager.manage(unit, rewrite.createChange());
			}
			return manager;
		} finally {
			monitor.done();
		}
	}

	private FieldDeclaration createNewFieldDeclarationNode(MemberActionInfo info, JavaScriptUnit declaringCuNode, TypeVariableMaplet[] mapping, ASTRewrite rewrite, VariableDeclarationFragment oldFieldFragment) throws JavaScriptModelException {
		Assert.isTrue(info.isFieldInfo());
		IField field= (IField) info.getMember();
		AST ast= rewrite.getAST();
		VariableDeclarationFragment newFragment= ast.newVariableDeclarationFragment();
		newFragment.setExtraDimensions(oldFieldFragment.getExtraDimensions());
		Expression initializer= oldFieldFragment.getInitializer();
		if (initializer != null) {
			Expression newInitializer= null;
			if (mapping.length > 0)
				newInitializer= createPlaceholderForExpression(initializer, field.getJavaScriptUnit(), mapping, rewrite);
			else
				newInitializer= createPlaceholderForExpression(initializer, field.getJavaScriptUnit(), rewrite);
			newFragment.setInitializer(newInitializer);
		}
		newFragment.setName(ast.newSimpleName(oldFieldFragment.getName().getIdentifier()));
		FieldDeclaration newField= ast.newFieldDeclaration(newFragment);
		FieldDeclaration oldField= ASTNodeSearchUtil.getFieldDeclarationNode(field, declaringCuNode);
		if (info.copyJavadocToCopiesInSubclasses())
			copyJavadocNode(rewrite, field, oldField, newField);
		copyAnnotations(oldField, newField);
		newField.modifiers().addAll(ASTNodeFactory.newModifiers(ast, info.getNewModifiersForCopyInSubclass(oldField.getModifiers())));
		Type oldType= oldField.getType();
		IJavaScriptUnit cu= field.getJavaScriptUnit();
		Type newType= null;
		if (mapping.length > 0) {
			newType= createPlaceholderForType(oldType, cu, mapping, rewrite);
		} else
			newType= createPlaceholderForType(oldType, cu, rewrite);
		newField.setType(newType);
		return newField;
	}

	private FunctionDeclaration createNewMethodDeclarationNode(MemberActionInfo info, JavaScriptUnit declaringCuNode, TypeVariableMaplet[] mapping, CompilationUnitRewrite rewriter, FunctionDeclaration oldMethod) throws JavaScriptModelException {
		Assert.isTrue(!info.isFieldInfo());
		IFunction method= (IFunction) info.getMember();
		ASTRewrite rewrite= rewriter.getASTRewrite();
		AST ast= rewrite.getAST();
		FunctionDeclaration newMethod= ast.newFunctionDeclaration();
		copyBodyOfPushedDownMethod(rewrite, method, oldMethod, newMethod, mapping);
		newMethod.setConstructor(oldMethod.isConstructor());
		newMethod.setExtraDimensions(oldMethod.getExtraDimensions());
		if (info.copyJavadocToCopiesInSubclasses())
			copyJavadocNode(rewrite, method, oldMethod, newMethod);
		final IJavaScriptProject project= rewriter.getCu().getJavaScriptProject();
		copyAnnotations(oldMethod, newMethod);
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, info.getNewModifiersForCopyInSubclass(oldMethod.getModifiers())));
		newMethod.setName(ast.newSimpleName(oldMethod.getName().getIdentifier()));
		copyReturnType(rewrite, method.getJavaScriptUnit(), oldMethod, newMethod, mapping);
		copyParameters(rewrite, method.getJavaScriptUnit(), oldMethod, newMethod, mapping);
		copyThrownExceptions(oldMethod, newMethod);
		return newMethod;
	}

	private void declareMethodAbstract(MemberActionInfo info, CompilationUnitRewrite sourceRewrite, CompilationUnitRewrite unitRewrite) throws JavaScriptModelException {
		Assert.isTrue(!info.isFieldInfo());
		IFunction method= (IFunction) info.getMember();
		if (JdtFlags.isAbstract(method))
			return;
		final FunctionDeclaration declaration= ASTNodeSearchUtil.getMethodDeclarationNode(method, sourceRewrite.getRoot());
		unitRewrite.getASTRewrite().remove(declaration.getBody(), null);
		sourceRewrite.getImportRemover().registerRemovedNode(declaration.getBody());
		ModifierRewrite.create(unitRewrite.getASTRewrite(), declaration).setModifiers(info.getNewModifiersForOriginal(declaration.getModifiers()), null);
	}

	private MemberActionInfo[] getAbstractDeclarationInfos() throws JavaScriptModelException {
		List result= new ArrayList(fMemberInfos.length);
		for (int index= 0; index < fMemberInfos.length; index++) {
			MemberActionInfo info= fMemberInfos[index];
			if (info.isNewMethodToBeDeclaredAbstract())
				result.add(info);
		}
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	private IType[] getAbstractDestinations(IProgressMonitor monitor) throws JavaScriptModelException {
		IType[] allDirectSubclasses= getHierarchyOfDeclaringClass(monitor).getSubclasses(getDeclaringType());
		List result= new ArrayList(allDirectSubclasses.length);
		for (int index= 0; index < allDirectSubclasses.length; index++) {
			IType subclass= allDirectSubclasses[index];
			if (subclass.exists() && !subclass.isBinary() && !subclass.isReadOnly() && subclass.getJavaScriptUnit() != null && subclass.isStructureKnown())
				result.add(subclass);
		}
		return (IType[]) result.toArray(new IType[result.size()]);
	}

	private MemberActionInfo[] getAbstractMemberInfos() throws JavaScriptModelException {
		List result= new ArrayList(fMemberInfos.length);
		for (int index= 0; index < fMemberInfos.length; index++) {
			MemberActionInfo info= fMemberInfos[index];
			if (info.isToBeCreatedInSubclassesOfDeclaringClass() && JdtFlags.isAbstract(info.getMember()))
				result.add(info);
		}
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	public IMember[] getAdditionalRequiredMembers(IProgressMonitor monitor) throws JavaScriptModelException {
		IMember[] members= MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());
		monitor.beginTask(RefactoringCoreMessages.PushDownRefactoring_calculating_required, members.length);// not
		// true,
		// but
		// not
		// easy
		// to
		// give
		// anything
		// better
		List queue= new ArrayList(members.length);
		queue.addAll(Arrays.asList(members));
		if (queue.isEmpty())
			return new IMember[0];
		int i= 0;
		IMember current;
		do {
			current= (IMember) queue.get(i);
			addAllRequiredPushableMembers(queue, current, new SubProgressMonitor(monitor, 1));
			i++;
			if (queue.size() == i)
				current= null;
		} while (current != null);
		queue.removeAll(Arrays.asList(members));// report only additional
		return (IMember[]) queue.toArray(new IMember[queue.size()]);
	}

	private IMember[] getDeletableMembers() {
		List result= new ArrayList(fMemberInfos.length);
		for (int i= 0; i < fMemberInfos.length; i++) {
			MemberActionInfo info= fMemberInfos[i];
			if (info.isToBeDeletedFromDeclaringClass())
				result.add(info.getMember());
		}
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}

	private MemberActionInfo[] getEffectedMemberInfos() throws JavaScriptModelException {
		List result= new ArrayList(fMemberInfos.length);
		for (int i= 0; i < fMemberInfos.length; i++) {
			MemberActionInfo info= fMemberInfos[i];
			if (info.isToBeCreatedInSubclassesOfDeclaringClass() && !JdtFlags.isAbstract(info.getMember()))
				result.add(info);
		}
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	public Object[] getElements() {
		return fMembersToMove;
	}

	private ITypeHierarchy getHierarchyOfDeclaringClass(IProgressMonitor monitor) throws JavaScriptModelException {
		try {
			if (fCachedClassHierarchy != null)
				return fCachedClassHierarchy;
			fCachedClassHierarchy= getDeclaringType().newTypeHierarchy(monitor);
			return fCachedClassHierarchy;
		} finally {
			monitor.done();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String getIdentifier() {
		return IDENTIFIER;
	}

	private MemberActionInfo[] getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass() throws JavaScriptModelException {
		MemberActionInfo[] abs= getAbstractMemberInfos();
		MemberActionInfo[] nonabs= getEffectedMemberInfos();
		List result= new ArrayList(abs.length + nonabs.length);
		result.addAll(Arrays.asList(abs));
		result.addAll(Arrays.asList(nonabs));
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	public MemberActionInfo[] getMemberActionInfos() {
		return fMemberInfos;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getProcessorName() {
		return RefactoringCoreMessages.PushDownRefactoring_name;
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus initialize(RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			String handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists() || element.getElementType() != IJavaScriptElement.TYPE)
					return ScriptableRefactoring.createInputFatalStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.PUSH_DOWN);
				else
					fCachedDeclaringType= (IType) element;
			}
			int count= 1;
			final List elements= new ArrayList();
			final List infos= new ArrayList();
			String attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + count;
			final RefactoringStatus status= new RefactoringStatus();
			while ((handle= extended.getAttribute(attribute)) != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists())
					status.merge(ScriptableRefactoring.createInputWarningStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.PUSH_DOWN));
				else
					elements.add(element);
				if (extended.getAttribute(ATTRIBUTE_ABSTRACT + count) != null)
					infos.add(MemberActionInfo.create((IMember) element, MemberActionInfo.PUSH_ABSTRACT_ACTION));
				else if (extended.getAttribute(ATTRIBUTE_PUSH + count) != null)
					infos.add(MemberActionInfo.create((IMember) element, MemberActionInfo.PUSH_DOWN_ACTION));
				else
					infos.add(MemberActionInfo.create((IMember) element, MemberActionInfo.NO_ACTION));
				count++;
				attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + count;
			}
			fMembersToMove= (IMember[]) elements.toArray(new IMember[elements.size()]);
			fMemberInfos= (MemberActionInfo[]) infos.toArray(new MemberActionInfo[infos.size()]);
			if (!status.isOK())
				return status;
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTester.isPushDownAvailable(fMembersToMove);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void rewriteTypeOccurrences(final TextEditBasedChangeManager manager, final ASTRequestor requestor, final CompilationUnitRewrite rewrite, final IJavaScriptUnit unit, final JavaScriptUnit node, final Set replacements, final IProgressMonitor monitor) throws CoreException {
		// Not needed
	}
}
