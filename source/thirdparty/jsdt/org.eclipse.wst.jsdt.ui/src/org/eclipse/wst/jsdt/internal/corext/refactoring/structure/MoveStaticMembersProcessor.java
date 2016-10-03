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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveProcessor;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IInitializer;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.wst.jsdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.JavaScriptRefactoringDescriptor;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.MoveStaticMembersDescriptor;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.wst.jsdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.delegates.DelegateFieldCreator;
import org.eclipse.wst.jsdt.internal.corext.refactoring.delegates.DelegateMethodCreator;
import org.eclipse.wst.jsdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.MemberVisibilityAdjustor.IncomingMemberVisibilityAdjustment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.ICommentProvider;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IDelegateUpdating;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.SearchUtils;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.refactoring.IRefactoringProcessorIds;

public final class MoveStaticMembersProcessor extends MoveProcessor implements IDelegateUpdating, IScriptableRefactoring, ICommentProvider {

	private static final String ATTRIBUTE_DELEGATE="delegate"; //$NON-NLS-1$
	private static final String ATTRIBUTE_DEPRECATE="deprecate"; //$NON-NLS-1$
	private static final String TRACKED_POSITION_PROPERTY= "MoveStaticMembersProcessor.trackedPosition"; //$NON-NLS-1$

	private IMember[] fMembersToMove;
	private IType fDestinationType;
	private String fDestinationTypeName;
	
	private CodeGenerationSettings fPreferences;
	private CompositeChange fChange;
	private CompilationUnitRewrite fSource;
	private ITypeBinding fSourceBinding; 
	private CompilationUnitRewrite fTarget;
	private IBinding[] fMemberBindings;
	private BodyDeclaration[] fMemberDeclarations;
	private boolean fDelegateUpdating;
	private boolean fDelegateDeprecation;
	private String fComment;

	private static class TypeReferenceFinder extends ASTVisitor {
		List fResult= new ArrayList();
		Set fDefined= new HashSet();
		public static List perform(ASTNode root) {
			TypeReferenceFinder visitor= new TypeReferenceFinder();
			root.accept(visitor);
			return visitor.fResult;
		}
		public boolean visit(TypeDeclaration node) {
			fDefined.add(node.resolveBinding());
			return true;
		}
		public boolean visit(SimpleName node) {
			IBinding binding= node.resolveBinding();
			if (!(binding instanceof ITypeBinding))
				return true;
			if (!fDefined.contains(binding))
				fResult.add(binding);
			return true;
		}

	}

	/**
	 * Creates a new move static members processor.
	 * @param members the members to move, or <code>null</code> if invoked by scripting
	 * @param settings the code generation settings, or <code>null</code> if invoked by scripting
	 */
	public MoveStaticMembersProcessor(IMember[] members, CodeGenerationSettings settings) {
		fMembersToMove= members;
		fPreferences= settings;
		fDelegateUpdating= false;
		fDelegateDeprecation= true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTester.isMoveStaticMembersAvailable(fMembersToMove);
	}

	/**
	 * {@inheritDoc}
	 */
	public Object[] getElements() {
		Object[] result= new Object[fMembersToMove.length];
		System.arraycopy(fMembersToMove, 0, result, 0, fMembersToMove.length);
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getIdentifier() {
		return IRefactoringProcessorIds.MOVE_STATIC_MEMBERS_PROCESSOR;
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants) throws CoreException {
		List result= new ArrayList();
		MoveArguments args= new MoveArguments(fDestinationType, true);
		String[] natures= JavaProcessors.computeAffectedNaturs(fMembersToMove);
		for (int i= 0; i < fMembersToMove.length; i++) {
			IMember member= fMembersToMove[i];
			result.addAll(Arrays.asList(ParticipantManager.loadMoveParticipants(
				status, this, member, args, natures, sharedParticipants)));
		}
		return (RefactoringParticipant[])result.toArray(new RefactoringParticipant[result.size()]);
	}
	
	//------------------- IDelegateUpdating ----------------------
	
	public boolean canEnableDelegateUpdating() {
		try {
			for (int i= 0; i < fMembersToMove.length; i++) {
				if (isDelegateCreationAvailable(fMembersToMove[i]))
					return true;
			}
		} catch (JavaScriptModelException e) {
			return false;
		}
		return false;
	}

	private boolean isDelegateCreationAvailable(IMember member) throws JavaScriptModelException {
		if (member instanceof IFunction)
			return true; 
		if (member instanceof IField && RefactoringAvailabilityTester.isDelegateCreationAvailable(((IField)member)))
			return true; 
		return false;
	}
	
	public boolean getDelegateUpdating() {
		return fDelegateUpdating;
	}

	public void setDelegateUpdating(boolean updating) {
		fDelegateUpdating= updating;
	}

	public boolean getDeprecateDelegates() {
		return fDelegateDeprecation;
	}

	public void setDeprecateDelegates(boolean deprecate) {
		fDelegateDeprecation= deprecate;
	}

	public String getProcessorName() {
		return RefactoringCoreMessages.MoveMembersRefactoring_Move_Members; 
	}

	public IType getDestinationType() {
		return fDestinationType;
	}

	public void setDestinationTypeFullyQualifiedName(String fullyQualifiedTypeName) throws JavaScriptModelException {
		Assert.isNotNull(fullyQualifiedTypeName);
		fDestinationType= resolveType(fullyQualifiedTypeName);
		//workaround for bug 36032: IJavaScriptProject#findType(..) doesn't find secondary type
		fDestinationTypeName= fullyQualifiedTypeName;
	}
	
	public IMember[] getMembersToMove() {
		return fMembersToMove;
	}
	
	public IType getDeclaringType() {
		//all methods declared in same type - checked in precondition
		return  fMembersToMove[0].getDeclaringType(); //index safe - checked in areAllMoveable()
	}
	
	private IType resolveType(String qualifiedTypeName) throws JavaScriptModelException{
		IType type= getDeclaringType().getJavaScriptProject().findType(qualifiedTypeName);
		if (type == null)
			type= getDeclaringType().getJavaScriptProject().findType(getDeclaringType().getPackageFragment().getElementName(), qualifiedTypeName);
		return type;
	}
	
	//---- Activation checking ------------------------------------
	
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_checking, 1); 
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkDeclaringType());
			pm.worked(1);
			if (result.hasFatalError())
				return result;			
			
			fSource= new CompilationUnitRewrite(fMembersToMove[0].getJavaScriptUnit());
			fSourceBinding= (ITypeBinding)((SimpleName)NodeFinder.perform(fSource.getRoot(), fMembersToMove[0].getDeclaringType().getNameRange())).resolveBinding();
			fMemberBindings= getMemberBindings();
			if (fSourceBinding == null || hasUnresolvedMemberBinding()) {
				result.addFatalError(Messages.format(
					RefactoringCoreMessages.MoveMembersRefactoring_compile_errors, 
					fSource.getCu().getElementName()));
			}
			fMemberDeclarations= getASTMembers(result);
			return result;
		} finally {
			pm.done();
		}	
	}
	
	private boolean hasUnresolvedMemberBinding() {
		for (int i= 0; i < fMemberBindings.length; i++) {
			if (fMemberBindings[i] == null)
				return true;
		}
		return false;
	}
	
	private RefactoringStatus checkDeclaringType(){
		IType declaringType= getDeclaringType();
				
		if (JavaModelUtil.getFullyQualifiedName(declaringType).equals("java.lang.Object")) //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MoveMembersRefactoring_Object);	 

		if (declaringType.isBinary())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MoveMembersRefactoring_binary);	 

		if (declaringType.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.MoveMembersRefactoring_read_only);	 
		
		return null;
	}
	
	//---- Input checking ------------------------------------

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		fTarget= null;
		try {
			pm.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_checking, 10); 
			
			RefactoringStatus result= new RefactoringStatus();	
			
			fSource.clearASTAndImportRewrites();
			
			result.merge(checkDestinationType());			
			if (result.hasFatalError())
				return result;
						
			result.merge(checkDestinationInsideTypeToMove());
			if (result.hasFatalError())
				return result;

			result.merge(MemberCheckUtil.checkMembersInDestinationType(fMembersToMove, fDestinationType));	
			if (result.hasFatalError())
				return result;
			
			if (result.hasFatalError())
				return result;
			
			List modifiedCus= new ArrayList();
			createChange(modifiedCus, result, new SubProgressMonitor(pm, 7));
			IFile[] changedFiles= getAllFilesToModify(modifiedCus);
			ResourceChangeChecker checker= (ResourceChangeChecker)context.getChecker(ResourceChangeChecker.class);
			for (int i= 0; i < changedFiles.length; i++) {
				checker.getDeltaFactory().change(changedFiles[i]);
			}
			
			return result;
		} finally {
			pm.done();
		}	
	}
	
	private IFile[] getAllFilesToModify(List modifiedCus) {
		Set result= new HashSet();
		IResource resource= fDestinationType.getJavaScriptUnit().getResource();
		result.add(resource);
		for (int i= 0; i < fMembersToMove.length; i++) {
			resource= fMembersToMove[i].getJavaScriptUnit().getResource();
			if (resource != null)
				result.add(resource);
		}
		for (Iterator iter= modifiedCus.iterator(); iter.hasNext();) {
			IJavaScriptUnit unit= (IJavaScriptUnit)iter.next();
			if (unit.getResource() != null)
				result.add(unit.getResource());
		}
		return (IFile[]) result.toArray(new IFile[result.size()]);
	}

	private RefactoringStatus checkDestinationType() throws JavaScriptModelException {			
		if (fDestinationType == null){
			String message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_not_found, fDestinationTypeName);
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		
		if (fDestinationType.equals(getDeclaringType())){
			String message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_same, 
				JavaElementUtil.createSignature(fDestinationType));
			return RefactoringStatus.createFatalErrorStatus(message);	
		}	
		
		if (! fDestinationType.exists()){
			String message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_not_exist, 
				JavaElementUtil.createSignature(fDestinationType));
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
			
		if (fDestinationType.isBinary()){
			String message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_dest_binary, 
				JavaElementUtil.createSignature(fDestinationType));
			return RefactoringStatus.createFatalErrorStatus(message);
		}	

		RefactoringStatus result= new RefactoringStatus();				

		if (result.hasFatalError())
			return result;

		// no checking required for moving interface fields to classes
				
		if (! ((JdtFlags.isStatic(fDestinationType)) || (fDestinationType.getDeclaringType() == null))){
			String message= RefactoringCoreMessages.MoveMembersRefactoring_static_declaration; 
			result.addError(message);
		}	
				
		return result;	
	}
	
	private RefactoringStatus checkDestinationInsideTypeToMove() throws JavaScriptModelException {
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fMembersToMove.length; i++) {
			if (! (fMembersToMove[i] instanceof IType))
				continue;
			IType type= (IType) fMembersToMove[i];
			if (fDestinationType.equals(type) || JavaElementUtil.isAncestorOf(type, fDestinationType)) {
				String message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_inside, 
						new String[] {JavaModelUtil.getFullyQualifiedName(type),
								JavaModelUtil.getFullyQualifiedName(fDestinationType)});
				RefactoringStatusContext context= JavaStatusContext.create(fDestinationType.getJavaScriptUnit(), fDestinationType.getNameRange());
				result.addFatalError(message, context);
				return result;
			}
		}
		return result;
	}

	private RefactoringStatus checkFieldsForInterface() throws JavaScriptModelException {
		//could be more clever and make field final if it is only written once...
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fMembersToMove.length; i++) {
			if (! canMoveToInterface(fMembersToMove[i])) {
				String message= RefactoringCoreMessages.MoveMembersRefactoring_only_public_static; 
				result.addError(message, JavaStatusContext.create(fMembersToMove[i]));
			}
		}
		return result;
	}

	private boolean canMoveToInterface(IMember member) throws JavaScriptModelException {
		int flags= member.getFlags();
		switch (member.getElementType()) {
			case IJavaScriptElement.FIELD:
				VariableDeclarationFragment declaration= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField) member, fSource.getRoot());
				if (declaration != null)
					return declaration.getInitializer() != null;

			case IJavaScriptElement.TYPE: {
				return (Flags.isPublic(flags) && Flags.isStatic(flags));
			}
			default:
				return false;
		}
	}

	private RefactoringStatus checkMovedMemberAvailability(IMember memberToMove, IProgressMonitor pm) throws JavaScriptModelException{
		RefactoringStatus result= new RefactoringStatus();
		if (memberToMove instanceof IType) { // recursively check accessibility of member type's members
			IJavaScriptElement[] typeMembers= ((IType) memberToMove).getChildren();
			pm.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_checking, typeMembers.length + 1); 
			for (int i= 0; i < typeMembers.length; i++) {
				if (typeMembers[i] instanceof IInitializer)
					pm.worked(1);
				else
					result.merge(checkMovedMemberAvailability((IMember) typeMembers[i], new SubProgressMonitor(pm, 1)));
			}
		} else {
			pm.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_checking, 1); 
		}

		IType[] blindAccessorTypes= getTypesNotSeeingMovedMember(memberToMove, new SubProgressMonitor(pm, 1), result);
		for (int k= 0; k < blindAccessorTypes.length; k++) {
			String message= createNonAccessibleMemberMessage(memberToMove, blindAccessorTypes[k],/*moved*/true);
			result.addError(message, JavaStatusContext.create(memberToMove));
		}
		pm.done();
		return result;
	}
	
	private IType[] getTypesNotSeeingMovedMember(IMember member, IProgressMonitor pm, RefactoringStatus status) throws JavaScriptModelException {
		if (JdtFlags.isPublic(member) && JdtFlags.isPublic(fDestinationType))
			return new IType[0];

		HashSet blindAccessorTypes= new HashSet(); // referencing, but access to destination type illegal
		SearchResultGroup[] references= getReferences(member, new SubProgressMonitor(pm, 1), status);
		for (int i = 0; i < references.length; i++) {
			SearchMatch[] searchResults= references[i].getSearchResults();
			for (int k= 0; k < searchResults.length; k++) {
				SearchMatch searchResult= searchResults[k];
				IJavaScriptElement element= SearchUtils.getEnclosingJavaElement(searchResult);
				IType type= (IType) element.getAncestor(IJavaScriptElement.TYPE);
				if (type != null //reference can e.g. be an import declaration
						&& ! blindAccessorTypes.contains(type)
						&& ! isWithinMemberToMove(searchResult)
						&& ! isVisibleFrom(member, getDestinationType(), type)) {
					blindAccessorTypes.add(type);
				}
			}
		}
		
		if (fDelegateUpdating && isDelegateCreationAvailable(member)) {
			// ensure moved member is visible from the delegate
			IType type= member.getDeclaringType();
			if (!blindAccessorTypes.contains(type) && !isVisibleFrom(member, getDestinationType(), type))
				blindAccessorTypes.add(type);
		}
		
		return (IType[]) blindAccessorTypes.toArray(new IType[blindAccessorTypes.size()]);
	}

	private String createNonAccessibleMemberMessage(IMember member, IType accessingType, boolean moved){
		//Non-visibility can have various reasons and always displaying all visibility
		//flags for all enclosing elements would be too heavy. Context reveals exact cause.
		IType declaringType= moved ? getDestinationType() : getDeclaringType();
		String message;
		switch (member.getElementType()){
			case IJavaScriptElement.FIELD: {
				if (moved)
					message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_moved_field, 
								new String[]{JavaElementUtil.createFieldSignature((IField)member), 
									JavaModelUtil.getFullyQualifiedName(accessingType),
									JavaModelUtil.getFullyQualifiedName(declaringType)});
				else
					message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_accessed_field, 
								new String[]{JavaElementUtil.createFieldSignature((IField)member), 
									JavaModelUtil.getFullyQualifiedName(accessingType)});
				return message;
			}			
			case IJavaScriptElement.METHOD: {
				if (moved)
					message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_moved_method, 
								new String[]{JavaElementUtil.createMethodSignature((IFunction)member),
									JavaModelUtil.getFullyQualifiedName(accessingType),
									JavaModelUtil.getFullyQualifiedName(declaringType)});
				else				 
					message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_accessed_method, 
								new String[]{JavaElementUtil.createMethodSignature((IFunction)member),
									JavaModelUtil.getFullyQualifiedName(accessingType)});
								 
				return message;		
			}			
			case IJavaScriptElement.TYPE:{
				if (moved)
					message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_moved_type, 
								new String[]{JavaModelUtil.getFullyQualifiedName(((IType)member)), 
									JavaModelUtil.getFullyQualifiedName(accessingType),
									JavaModelUtil.getFullyQualifiedName(declaringType)});
				else
					message= Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_accessed_type, 
								new String[]{JavaModelUtil.getFullyQualifiedName(((IType)member)), 
									JavaModelUtil.getFullyQualifiedName(accessingType)});
				return message;
			}			
			default:
				Assert.isTrue(false);
				return null;
		}
	}

	private static SearchResultGroup[] getReferences(IMember member, IProgressMonitor monitor, RefactoringStatus status) throws JavaScriptModelException {
		final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(SearchPattern.createPattern(member, IJavaScriptSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE));
		engine.setFiltering(true, true);
		engine.setScope(RefactoringScopeFactory.create(member));
		engine.setStatus(status);
		engine.searchPattern(new SubProgressMonitor(monitor, 1));
		return (SearchResultGroup[]) engine.getResults();
	}

	private static boolean isVisibleFrom(IMember member, IType newMemberDeclaringType, IType accessingType) throws JavaScriptModelException{
		int memberVisibility= JdtFlags.getVisibilityCode(newMemberDeclaringType);
			
		IType declaringType= newMemberDeclaringType.getDeclaringType();
		while (declaringType != null) { //get lowest visibility in all parent types of newMemberDeclaringType
			memberVisibility= JdtFlags.getLowerVisibility(
					memberVisibility, JdtFlags.getVisibilityCode(declaringType));
			declaringType= declaringType.getDeclaringType();
		}
		
		switch (memberVisibility) {
			case Modifier.PRIVATE :
				return isEqualOrEnclosedType(accessingType, newMemberDeclaringType);
				
			case Modifier.NONE :
				return JavaModelUtil.isSamePackage(accessingType.getPackageFragment(), newMemberDeclaringType.getPackageFragment());
					
			case Modifier.PROTECTED :
				return JavaModelUtil.isSamePackage(accessingType.getPackageFragment(), newMemberDeclaringType.getPackageFragment())
						|| accessingType.newSupertypeHierarchy(null).contains(newMemberDeclaringType);
			
			case Modifier.PUBLIC :
				return true;

			default:
				Assert.isTrue(false);
				return false;
		}
	}
	
	private static boolean isEqualOrEnclosedType(IType inner, IType outer) {
		while (inner != null) {
			if (inner.equals(outer))
				return true;
			else
				inner= inner.getDeclaringType();
		}
		return false;
	}

	private boolean isWithinMemberToMove(SearchMatch result) throws JavaScriptModelException {
		IJavaScriptUnit referenceCU= SearchUtils.getCompilationUnit(result);
		if (! referenceCU.equals(fSource.getCu()))
			return false;
		int referenceStart= result.getOffset();
		for (int i= 0; i < fMembersToMove.length; i++) {
			ISourceRange range= fMembersToMove[i].getSourceRange();
			if (range.getOffset() <= referenceStart && range.getOffset() + range.getLength() >= referenceStart)
				return true;
		}
		return false;
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.done();
		return fChange;
	}
	
	private void createChange(List modifiedCus, RefactoringStatus status, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_creating, 5);
		final IMember[] members= getMembersToMove();
		String project= null;
		final IJavaScriptProject javaProject= getDeclaringType().getJavaScriptProject();
		if (javaProject != null)
			project= javaProject.getElementName();
		String header= null;
		if (members.length == 1)
			header= Messages.format(RefactoringCoreMessages.MoveStaticMembersProcessor_descriptor_description_single, new String[] { JavaScriptElementLabels.getElementLabel(members[0], JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getElementLabel(fDestinationType, JavaScriptElementLabels.ALL_FULLY_QUALIFIED) });
		else
			header= Messages.format(RefactoringCoreMessages.MoveStaticMembersProcessor_descriptor_description_multi, new String[] { String.valueOf(members.length), JavaScriptElementLabels.getElementLabel(fDestinationType, JavaScriptElementLabels.ALL_FULLY_QUALIFIED) });
		int flags= JavaScriptRefactoringDescriptor.JAR_MIGRATION | JavaScriptRefactoringDescriptor.JAR_REFACTORING | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
		final IType declaring= members[0].getDeclaringType();
		try {
			if (declaring.isLocal() || declaring.isAnonymous())
				flags|= JavaScriptRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
		} catch (JavaScriptModelException exception) {
			JavaScriptPlugin.log(exception);
		}
		final String description= members.length == 1 ? Messages.format(RefactoringCoreMessages.MoveStaticMembersProcessor_description_descriptor_short_multi, members[0].getElementName()) : RefactoringCoreMessages.MoveMembersRefactoring_move_members;
		final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
		comment.addSetting(Messages.format(RefactoringCoreMessages.MoveStaticMembersProcessor_target_element_pattern, JavaScriptElementLabels.getElementLabel(fDestinationType, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)));
		final MoveStaticMembersDescriptor descriptor= new MoveStaticMembersDescriptor();
		descriptor.setProject(project);
		descriptor.setDescription(description);
		descriptor.setComment(comment.asString());
		descriptor.setFlags(flags);
		descriptor.setDestinationType(fDestinationType);
		descriptor.setKeepOriginal(fDelegateUpdating);
		descriptor.setDeprecateDelegate(fDelegateDeprecation);
		descriptor.setMembers(members);
		fChange= new DynamicValidationRefactoringChange(descriptor, RefactoringCoreMessages.MoveMembersRefactoring_move_members);
		fTarget= getCuRewrite(fDestinationType.getJavaScriptUnit());
		ITypeBinding targetBinding= getDestinationBinding();
		if (targetBinding == null) {
			status.addFatalError(Messages.format(RefactoringCoreMessages.MoveMembersRefactoring_compile_errors, fTarget.getCu().getElementName()));
			monitor.done();
			return;
		}

		try {
			Map adjustments= new HashMap();
			IMember member= null;
			SubProgressMonitor sub= new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
			sub.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_creating, fMembersToMove.length);
			Set rewritten= new HashSet();
			for (int index= 0; index < fMembersToMove.length; index++) {
				member= fMembersToMove[index];
				final MemberVisibilityAdjustor adjustor= new MemberVisibilityAdjustor(fDestinationType, member);
				adjustor.setAdjustments(adjustments);
				adjustor.setStatus(status);
				adjustor.setVisibilitySeverity(RefactoringStatus.WARNING);
				adjustor.setFailureSeverity(RefactoringStatus.WARNING);
				adjustor.setRewrite(fSource.getASTRewrite(), fSource.getRoot());
				adjustor.adjustVisibility(new NullProgressMonitor());

				if (fDelegateUpdating && isDelegateCreationAvailable(member)) {
					// Add a visibility adjustment so the moved member
					// will be visible from within the delegate
					ModifierKeyword threshold= adjustor.getVisibilityThreshold(member, fDestinationType, new NullProgressMonitor());
					IncomingMemberVisibilityAdjustment adjustment= (IncomingMemberVisibilityAdjustment) adjustments.get(member);
					ModifierKeyword kw= (adjustment != null) ? adjustment.getKeyword() : ModifierKeyword.fromFlagValue(JdtFlags.getVisibilityCode(member));
					if (MemberVisibilityAdjustor.hasLowerVisibility(kw, threshold)) {
						adjustments.put(member, new MemberVisibilityAdjustor.IncomingMemberVisibilityAdjustment(member, threshold, RefactoringStatus.createWarningStatus(Messages.format(MemberVisibilityAdjustor.getMessage(member), new String[] { MemberVisibilityAdjustor.getLabel(member), MemberVisibilityAdjustor.getLabel(threshold)}), JavaStatusContext.create(member))));
					}
				}

				// Check if destination type is visible from references ->
				// error message if not (for example, when moving into a private type)
				status.merge(checkMovedMemberAvailability(member, new SubProgressMonitor(sub, 1)));
				// Put rewrite info into code and into status
				for (final Iterator iterator= rewritten.iterator(); iterator.hasNext();) {
					adjustments.remove(iterator.next());
				}
				rewritten.addAll(adjustments.keySet());
				adjustor.rewriteVisibility(new NullProgressMonitor());
			}

			// First update references in moved members, in order to extract the
			// source.
			String[] memberSources= getUpdatedMemberSource(status, fMemberDeclarations, targetBinding);
			monitor.worked(1);
			if (status.hasFatalError())
				return;

			final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2();
			engine.setPattern(fMembersToMove, IJavaScriptSearchConstants.ALL_OCCURRENCES);
			engine.setGranularity(RefactoringSearchEngine2.GRANULARITY_COMPILATION_UNIT);
			engine.setFiltering(true, true);
			engine.setScope(RefactoringScopeFactory.create(fMembersToMove));
			engine.setStatus(status);
			engine.searchPattern(new NullProgressMonitor());
			IJavaScriptUnit[] units= engine.getAffectedCompilationUnits();
			modifiedCus.addAll(Arrays.asList(units));
			final MemberVisibilityAdjustor adjustor= new MemberVisibilityAdjustor(fDestinationType, fDestinationType);
			sub= new SubProgressMonitor(monitor, 1);
			sub.beginTask(RefactoringCoreMessages.MoveMembersRefactoring_creating, units.length);
			for (int index= 0; index < units.length; index++) {
				IJavaScriptUnit unit= units[index];
				CompilationUnitRewrite rewrite= getCuRewrite(unit);
				adjustor.setRewrites(Collections.singletonMap(unit, rewrite));
				adjustor.setAdjustments(adjustments);
				adjustor.rewriteVisibility(unit, new SubProgressMonitor(sub, 1));
				ReferenceAnalyzer analyzer= new ReferenceAnalyzer(rewrite, fMemberBindings, targetBinding, fSourceBinding);
				rewrite.getRoot().accept(analyzer);
				status.merge(analyzer.getStatus());
				if (status.hasFatalError()) {
					fChange= null;
					return;
				}
				if (!(fSource.getCu().equals(unit) || fTarget.getCu().equals(unit)))
					fChange.add(rewrite.createChange());
			}
			status.merge(moveMembers(fMemberDeclarations, memberSources));
			fChange.add(fSource.createChange());
			modifiedCus.add(fSource.getCu());
			if (!fSource.getCu().equals(fTarget.getCu())) {
				fChange.add(fTarget.createChange());
				modifiedCus.add(fTarget.getCu());
			}
			monitor.worked(1);
		} catch (BadLocationException exception) {
			JavaScriptPlugin.log(exception);
		}
	}
	
	private CompilationUnitRewrite getCuRewrite(IJavaScriptUnit unit) {
		if (fSource.getCu().equals(unit))
			return fSource;
		if (fTarget != null && fTarget.getCu().equals(unit))
			return fTarget;
		return new CompilationUnitRewrite(unit);
	}
	
	private ITypeBinding getDestinationBinding() throws JavaScriptModelException {
		ASTNode node= NodeFinder.perform(fTarget.getRoot(), fDestinationType.getNameRange());
		if (!(node instanceof SimpleName))
			return null;
		IBinding binding= ((SimpleName)node).resolveBinding();
		if (!(binding instanceof ITypeBinding))
			return null;
		return (ITypeBinding)binding;
	}
	
	private IBinding[] getMemberBindings() throws JavaScriptModelException {
		IBinding[] result= new IBinding[fMembersToMove.length];
		for (int i= 0; i < fMembersToMove.length; i++) {
			IMember member= fMembersToMove[i];
			SimpleName name= (SimpleName)NodeFinder.perform(fSource.getRoot(), member.getNameRange());
			result[i]= name.resolveBinding();
		}
		return result;
	}
	
	private String[] getUpdatedMemberSource(RefactoringStatus status, BodyDeclaration[] members, ITypeBinding target) throws CoreException, BadLocationException {
		List typeRefs= new ArrayList();
		boolean targetNeedsSourceImport= false;
		boolean isSourceNotTarget= fSource != fTarget;
		Set exclude= new HashSet();
		for (int i= 0; i < members.length; i++) {
			BodyDeclaration declaration= members[i];
			if (declaration instanceof AbstractTypeDeclaration) {
				AbstractTypeDeclaration type= (AbstractTypeDeclaration) declaration;
				ITypeBinding binding= type.resolveBinding();
				if (binding != null)
					exclude.add(binding);
			} else if (declaration instanceof FunctionDeclaration) {
				FunctionDeclaration method= (FunctionDeclaration) declaration;
				IFunctionBinding binding= method.resolveBinding();
				if (binding != null)
					exclude.add(binding);
			} else if (declaration instanceof FieldDeclaration) {
				FieldDeclaration field= (FieldDeclaration) declaration;
				for (final Iterator iterator= field.fragments().iterator(); iterator.hasNext();) {
					VariableDeclarationFragment fragment= (VariableDeclarationFragment) iterator.next();
					IVariableBinding binding= fragment.resolveBinding();
					if (binding != null)
						exclude.add(binding);
				}
			}
		}
		for (int i= 0; i < members.length; i++) {
			BodyDeclaration declaration= members[i];
			if (isSourceNotTarget)
				typeRefs.addAll(TypeReferenceFinder.perform(declaration));
			MovedMemberAnalyzer analyzer= new MovedMemberAnalyzer(fSource, fMemberBindings, fSourceBinding, target);
			declaration.accept(analyzer);
			ImportRewriteUtil.addImports(fTarget, declaration, new HashMap(), new HashMap(), exclude, false);
			
			ITrackedNodePosition trackedPosition= fSource.getASTRewrite().track(declaration);
			declaration.setProperty(TRACKED_POSITION_PROPERTY, trackedPosition);
			targetNeedsSourceImport|= analyzer.targetNeedsSourceImport();
			status.merge(analyzer.getStatus());
		}
		// Adjust imports
		if (targetNeedsSourceImport && isSourceNotTarget) {
			fTarget.getImportRewrite().addImport(fSourceBinding);
		}
		if (isSourceNotTarget) {
			for (Iterator iter= typeRefs.iterator(); iter.hasNext();) {
				ITypeBinding binding= (ITypeBinding) iter.next();
				fTarget.getImportRewrite().addImport(binding);
			}
		}
		// extract updated members
		String[] updatedMemberSources= new String[members.length];
		IDocument document= new Document(fSource.getCu().getBuffer().getContents());
		TextEdit edit= fSource.getASTRewrite().rewriteAST(document, fSource.getCu().getJavaScriptProject().getOptions(true));
		edit.apply(document, TextEdit.UPDATE_REGIONS);
		for (int i= 0; i < members.length; i++) {
			updatedMemberSources[i]= getUpdatedMember(document, members[i]);
		}
		fSource.clearASTRewrite();
		return updatedMemberSources;
	}

	private String getUpdatedMember(IDocument document, BodyDeclaration declaration) throws BadLocationException {
		ITrackedNodePosition trackedPosition= (ITrackedNodePosition) declaration.getProperty(TRACKED_POSITION_PROPERTY);
		return Strings.trimIndentation(document.get(trackedPosition.getStartPosition(), trackedPosition.getLength()), fPreferences.tabWidth, fPreferences.indentWidth, false);
	}

	private RefactoringStatus moveMembers(BodyDeclaration[] members, String[] sources) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		AbstractTypeDeclaration destination= (AbstractTypeDeclaration)
		ASTNodes.getParent(
			NodeFinder.perform(fTarget.getRoot(), fDestinationType.getNameRange()),
			AbstractTypeDeclaration.class);
		ListRewrite containerRewrite= fTarget.getASTRewrite().getListRewrite(destination, destination.getBodyDeclarationsProperty());
		
		TextEditGroup delete= fSource.createGroupDescription(RefactoringCoreMessages.MoveMembersRefactoring_deleteMembers); 
		TextEditGroup add= fTarget.createGroupDescription(RefactoringCoreMessages.MoveMembersRefactoring_addMembers); 
		for (int i= 0; i < members.length; i++) {
			BodyDeclaration declaration= members[i];
			ASTNode removeImportsOf= null;
			boolean addedDelegate= false;
			
			if (fDelegateUpdating) {
				if (declaration instanceof FunctionDeclaration) {

					DelegateMethodCreator creator= new DelegateMethodCreator();
					creator.setDeclaration(declaration);
					creator.setDeclareDeprecated(fDelegateDeprecation);
					creator.setSourceRewrite(fSource);
					creator.setCopy(false);
					creator.setNewLocation(getDestinationBinding());
					creator.prepareDelegate();
					creator.createEdit();

					removeImportsOf= ((FunctionDeclaration) declaration).getBody();
					addedDelegate= true;
				}
				if (declaration instanceof FieldDeclaration) {

					// Note: this FieldDeclaration only has one fragment (@see #getASTMembers(RefactoringStatus))
					final VariableDeclarationFragment frag= (VariableDeclarationFragment) ((FieldDeclaration) declaration).fragments().get(0);
					
					if (!Modifier.isFinal(declaration.getModifiers())) {
						// Don't create a delegate for non-final fields
						result.addInfo(Messages.format(RefactoringCoreMessages.DelegateCreator_cannot_create_field_delegate_not_final, frag.getName()), null);
					} else if (frag.getInitializer() == null) {
						// Don't create a delegate without an initializer.
						result.addInfo(Messages.format(RefactoringCoreMessages.DelegateCreator_cannot_create_field_delegate_no_initializer, frag.getName()), null);
					} else {
						DelegateFieldCreator creator= new DelegateFieldCreator();
						creator.setDeclaration(declaration);
						creator.setDeclareDeprecated(fDelegateDeprecation);
						creator.setSourceRewrite(fSource);
						creator.setCopy(false);
						creator.setNewLocation(getDestinationBinding());
						creator.prepareDelegate();
						creator.createEdit();

						removeImportsOf= frag.getInitializer();
						addedDelegate= true;
					}
				}
				if (declaration instanceof AbstractTypeDeclaration) {
					result.addInfo(Messages.format(RefactoringCoreMessages.DelegateCreator_cannot_create_delegate_for_type, ((AbstractTypeDeclaration) declaration).getName().getIdentifier()),
							null);
				}
			} 
			
			if (!addedDelegate) {
				fSource.getASTRewrite().remove(declaration, delete);
				removeImportsOf= declaration;
			}

			if (removeImportsOf != null && fSource != fTarget)
				fSource.getImportRemover().registerRemovedNode(removeImportsOf);
			
			ASTNode node= fTarget.getASTRewrite().createStringPlaceholder(sources[i], declaration.getNodeType());
			List container= containerRewrite.getRewrittenList();
			int insertionIndex= ASTNodes.getInsertionIndex((BodyDeclaration) node, container);
			containerRewrite.insertAt(node, insertionIndex, add);
		}
		return result;
	}
	
	private BodyDeclaration[] getASTMembers(RefactoringStatus status) throws JavaScriptModelException {
		BodyDeclaration[] result= new BodyDeclaration[fMembersToMove.length];
		for (int i= 0; i < fMembersToMove.length; i++) {
			IMember member= fMembersToMove[i];
			ASTNode node= NodeFinder.perform(fSource.getRoot(), member.getNameRange());
			result[i]= (BodyDeclaration)ASTNodes.getParent(node, BodyDeclaration.class);

			//Fix for bug 42383: exclude multiple VariableDeclarationFragments ("int a=1, b=2")
			//ReferenceAnalyzer#visit(FieldDeclaration node) depends on fragments().size() != 1 !
			if (result[i] instanceof FieldDeclaration 
					&& ((FieldDeclaration) result[i]).fragments().size() != 1) {
				status.addFatalError(RefactoringCoreMessages.MoveMembersRefactoring_multi_var_fields); 
				return result;
			}
			
		}
	
		//Sorting members is important for field declarations referring to previous fields.
		Arrays.sort(result, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((BodyDeclaration) o1).getStartPosition()
						- ((BodyDeclaration) o2).getStartPosition();
			}
		});
		return result;
	}

	public RefactoringStatus initialize(final RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			String handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists() || element.getElementType() != IJavaScriptElement.TYPE)
					return ScriptableRefactoring.createInputFatalStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.MOVE_STATIC_MEMBERS);
				else {
					fDestinationType= (IType) element;
					fDestinationTypeName= fDestinationType.getFullyQualifiedName();
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
			final String delegate= extended.getAttribute(ATTRIBUTE_DELEGATE);
			if (delegate != null) {
				fDelegateUpdating= Boolean.valueOf(delegate).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DELEGATE));
			final String deprecate= extended.getAttribute(ATTRIBUTE_DEPRECATE);
			if (deprecate != null) {
				fDelegateDeprecation= Boolean.valueOf(deprecate).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DEPRECATE));
			int count= 1;
			final List elements= new ArrayList();
			String attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + count;
			final RefactoringStatus status= new RefactoringStatus();
			while ((handle= extended.getAttribute(attribute)) != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists())
					status.merge(ScriptableRefactoring.createInputWarningStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.MOVE_STATIC_MEMBERS));
				else
					elements.add(element);
				count++;
				attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + count;
			}
			fMembersToMove= (IMember[]) elements.toArray(new IMember[elements.size()]);
			if (elements.isEmpty())
				return ScriptableRefactoring.createInputFatalStatus(null, getRefactoring().getName(), IJavaScriptRefactorings.MOVE_STATIC_MEMBERS);
			IJavaScriptProject project= null;
			if (fMembersToMove.length > 0)
				project= fMembersToMove[0].getJavaScriptProject();
			fPreferences= JavaPreferencesSettings.getCodeGenerationSettings(project);
			if (!status.isOK())
				return status;
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}

	public boolean canEnableComment() {
		return true;
	}

	public String getComment() {
		return fComment;
	}

	public void setComment(String comment) {
		fComment= comment;
	}

	public String getDelegateUpdatingTitle(boolean plural) {
		if (plural)
			return RefactoringCoreMessages.DelegateMethodCreator_keep_original_moved_plural;
		else
			return RefactoringCoreMessages.DelegateMethodCreator_keep_original_moved_singular;
	}
}
