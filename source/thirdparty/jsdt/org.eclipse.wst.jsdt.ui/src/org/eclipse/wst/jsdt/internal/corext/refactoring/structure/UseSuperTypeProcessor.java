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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.ASTRequestor;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.JavaScriptRefactoringDescriptor;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.UseSupertypeDescriptor;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.constraints.SuperTypeConstraintsModel;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.constraints.SuperTypeConstraintsSolver;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.constraints.SuperTypeRefactoringProcessor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints2.ISourceConstraintVariable;
import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints2.ITypeConstraintVariable;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TextEditBasedChangeManager;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * Refactoring processor to replace type occurrences by a super type.
 */
public final class UseSuperTypeProcessor extends SuperTypeRefactoringProcessor {

	private static final String IDENTIFIER= "org.eclipse.wst.jsdt.ui.useSuperTypeProcessor"; //$NON-NLS-1$

	/**
	 * Finds the type with the given fully qualified name (generic type
	 * parameters included) in the hierarchy.
	 * 
	 * @param type
	 *            The hierarchy type to find the super type in
	 * @param name
	 *            The fully qualified name of the super type
	 * @return The found super type, or <code>null</code>
	 */
	protected static ITypeBinding findTypeInHierarchy(final ITypeBinding type, final String name) {
		if (type.isArray() || type.isPrimitive())
			return null;
		if (name.equals(type.getTypeDeclaration().getQualifiedName()))
			return type;
		final ITypeBinding binding= type.getSuperclass();
		if (binding != null) {
			final ITypeBinding result= findTypeInHierarchy(binding, name);
			if (result != null)
				return result;
		}
		return null;
	}

	/** The text change manager */
	private TextEditBasedChangeManager fChangeManager= null;

	/** The number of files affected by the last change generation */
	private int fChanges= 0;

	/** The subtype to replace */
	private IType fSubType;

	/** The supertype as replacement */
	private IType fSuperType= null;

	/**
	 * Creates a new super type processor.
	 * 
	 * @param subType
	 *            the subtype to replace its occurrences, or <code>null</code>
	 *            if invoked by scripting
	 */
	public UseSuperTypeProcessor(final IType subType) {
		super(null);
		fReplace= true;
		fSubType= subType;
	}

	/**
	 * Creates a new super type processor.
	 * 
	 * @param subType
	 *            the subtype to replace its occurrences, or <code>null</code>
	 *            if invoked by scripting
	 * @param superType
	 *            the supertype as replacement, or <code>null</code> if
	 *            invoked by scripting
	 */
	public UseSuperTypeProcessor(final IType subType, final IType superType) {
		super(null);
		fReplace= true;
		fSubType= subType;
		fSuperType= superType;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor,org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext)
	 */
	public final RefactoringStatus checkFinalConditions(final IProgressMonitor monitor, final CheckConditionsContext context) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		Assert.isNotNull(context);
		final RefactoringStatus status= new RefactoringStatus();
		fChangeManager= new TextEditBasedChangeManager();
		try {
			monitor.beginTask("", 200); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.UseSuperTypeProcessor_checking);
			fChangeManager= createChangeManager(new SubProgressMonitor(monitor, 200), status);
			if (!status.hasFatalError()) {
				final RefactoringStatus validation= Checks.validateModifiesFiles(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits()), getRefactoring().getValidationContext());
				if (!validation.isOK())
					return validation;
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final RefactoringStatus checkInitialConditions(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.UseSuperTypeProcessor_checking);
			// No checks
			monitor.worked(1);
		} finally {
			monitor.done();
		}
		return status;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final Change createChange(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		try {
			fChanges= 0;
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractInterfaceProcessor_creating);
			final TextEditBasedChange[] changes= fChangeManager.getAllChanges();
			if (changes != null && changes.length != 0) {
				fChanges= changes.length;
				IJavaScriptProject project= null;
				if (!fSubType.isBinary())
					project= fSubType.getJavaScriptProject();
				int flags= JavaScriptRefactoringDescriptor.JAR_MIGRATION | JavaScriptRefactoringDescriptor.JAR_REFACTORING | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
				try {
					if (fSubType.isLocal() || fSubType.isAnonymous())
						flags|= JavaScriptRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
				} catch (JavaScriptModelException exception) {
					JavaScriptPlugin.log(exception);
				}
				final String name= project != null ? project.getElementName() : null;
				final String description= Messages.format(RefactoringCoreMessages.UseSuperTypeProcessor_descriptor_description_short, fSuperType.getElementName());
				final String header= Messages.format(RefactoringCoreMessages.UseSuperTypeProcessor_descriptor_description, new String[] { JavaScriptElementLabels.getElementLabel(fSuperType, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getElementLabel(fSubType, JavaScriptElementLabels.ALL_FULLY_QUALIFIED) });
				final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(name, this, header);
				comment.addSetting(Messages.format(RefactoringCoreMessages.UseSuperTypeProcessor_refactored_element_pattern, JavaScriptElementLabels.getElementLabel(fSuperType, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)));
				addSuperTypeSettings(comment, false);
				final UseSupertypeDescriptor descriptor= new UseSupertypeDescriptor();
				descriptor.setProject(name);
				descriptor.setDescription(description);
				descriptor.setComment(comment.asString());
				descriptor.setFlags(flags);
				descriptor.setSubtype(getSubType());
				descriptor.setSupertype(getSuperType());
				descriptor.setReplaceInstanceof(fInstanceOf);
				return new DynamicValidationRefactoringChange(descriptor, RefactoringCoreMessages.UseSupertypeWherePossibleRefactoring_name, fChangeManager.getAllChanges());
			}
			monitor.worked(1);
		} finally {
			monitor.done();
		}
		return null;
	}

	/**
	 * Creates the text change manager for this processor.
	 * 
	 * @param monitor
	 *            the progress monitor to display progress
	 * @param status
	 *            the refactoring status
	 * @return the created text change manager
	 * @throws JavaScriptModelException
	 *             if the method declaration could not be found
	 * @throws CoreException
	 *             if the changes could not be generated
	 */
	protected final TextEditBasedChangeManager createChangeManager(final IProgressMonitor monitor, final RefactoringStatus status) throws JavaScriptModelException, CoreException {
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 300); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.UseSuperTypeProcessor_creating);
			final TextEditBasedChangeManager manager= new TextEditBasedChangeManager();
			final IJavaScriptProject project= fSubType.getJavaScriptProject();
			final ASTParser parser= ASTParser.newParser(AST.JLS3);
			parser.setWorkingCopyOwner(fOwner);
			parser.setResolveBindings(true);
			parser.setProject(project);
			parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
			if (fSubType.isBinary() || fSubType.isReadOnly()) {
				final IBinding[] bindings= parser.createBindings(new IJavaScriptElement[] { fSubType, fSuperType }, new SubProgressMonitor(monitor, 50));
				if (bindings != null && bindings.length == 2 && bindings[0] instanceof ITypeBinding && bindings[1] instanceof ITypeBinding) {
					solveSuperTypeConstraints(null, null, fSubType, (ITypeBinding) bindings[0], (ITypeBinding) bindings[1], new SubProgressMonitor(monitor, 100), status);
					if (!status.hasFatalError())
						rewriteTypeOccurrences(manager, null, null, null, null, new HashSet(), status, new SubProgressMonitor(monitor, 150));
				}
			} else {
				parser.createASTs(new IJavaScriptUnit[] { fSubType.getJavaScriptUnit() }, new String[0], new ASTRequestor() {

					public final void acceptAST(final IJavaScriptUnit unit, final JavaScriptUnit node) {
						try {
							final CompilationUnitRewrite subRewrite= new CompilationUnitRewrite(fOwner, unit, node);
							final AbstractTypeDeclaration subDeclaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(fSubType, subRewrite.getRoot());
							if (subDeclaration != null) {
								final ITypeBinding subBinding= subDeclaration.resolveBinding();
								if (subBinding != null) {
									final ITypeBinding superBinding= findTypeInHierarchy(subBinding, fSuperType.getFullyQualifiedName('.'));
									if (superBinding != null) {
										solveSuperTypeConstraints(subRewrite.getCu(), subRewrite.getRoot(), fSubType, subBinding, superBinding, new SubProgressMonitor(monitor, 100), status);
										if (!status.hasFatalError()) {
											rewriteTypeOccurrences(manager, this, subRewrite, subRewrite.getCu(), subRewrite.getRoot(), new HashSet(), status, new SubProgressMonitor(monitor, 200));
											final TextChange change= subRewrite.createChange();
											if (change != null)
												manager.manage(subRewrite.getCu(), change);
										}
									}
								}
							}
						} catch (CoreException exception) {
							JavaScriptPlugin.log(exception);
							status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.UseSuperTypeProcessor_internal_error));
						}
					}

					public final void acceptBinding(final String key, final IBinding binding) {
						// Do nothing
					}
				}, new NullProgressMonitor());
			}
			return manager;
		} finally {
			monitor.done();
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.structure.constraints.SuperTypeRefactoringProcessor#createContraintSolver(org.eclipse.wst.jsdt.internal.corext.refactoring.structure.constraints.SuperTypeConstraintsModel)
	 */
	protected final SuperTypeConstraintsSolver createContraintSolver(final SuperTypeConstraintsModel model) {
		return new SuperTypeConstraintsSolver(model);
	}

	/**
	 * Returns the number of files that are affected from the last change
	 * generation.
	 * 
	 * @return The number of files which are affected
	 */
	public final int getChanges() {
		return fChanges;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getElements()
	 */
	public final Object[] getElements() {
		return new Object[] { fSubType };
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getIdentifier()
	 */
	public final String getIdentifier() {
		return IDENTIFIER;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getProcessorName()
	 */
	public final String getProcessorName() {
		return RefactoringCoreMessages.UseSuperTypeProcessor_name;
	}

	/**
	 * Returns the subtype to be replaced.
	 * 
	 * @return The subtype to be replaced
	 */
	public final IType getSubType() {
		return fSubType;
	}

	/**
	 * Returns the supertype as replacement.
	 * 
	 * @return The supertype as replacement
	 */
	public final IType getSuperType() {
		return fSuperType;
	}

	/**
	 * {@inheritDoc}
	 */
	public final RefactoringStatus initialize(final RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			String handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists() || element.getElementType() != IJavaScriptElement.TYPE)
					return ScriptableRefactoring.createInputFatalStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.USE_SUPER_TYPE);
				else
					fSubType= (IType) element;
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
			handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + 1);
			if (handle != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists() || element.getElementType() != IJavaScriptElement.TYPE)
					return ScriptableRefactoring.createInputFatalStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.USE_SUPER_TYPE);
				else
					fSuperType= (IType) element;
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + 1));
			final String instance= extended.getAttribute(ATTRIBUTE_INSTANCEOF);
			if (instance != null) {
				fInstanceOf= Boolean.valueOf(instance).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_INSTANCEOF));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#isApplicable()
	 */
	public final boolean isApplicable() throws CoreException {
		return Checks.isAvailable(fSubType) && Checks.isAvailable(fSuperType) && !fSubType.isAnonymous() && !fSuperType.isAnonymous();
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#loadParticipants(org.eclipse.ltk.core.refactoring.RefactoringStatus,org.eclipse.ltk.core.refactoring.participants.SharableParticipants)
	 */
	public final RefactoringParticipant[] loadParticipants(final RefactoringStatus status, final SharableParticipants sharedParticipants) throws CoreException {
		return new RefactoringParticipant[0];
	}

	/**
	 * {@inheritDoc}
	 */
	protected final void rewriteTypeOccurrences(final TextEditBasedChangeManager manager, final ASTRequestor requestor, final CompilationUnitRewrite rewrite, final IJavaScriptUnit unit, final JavaScriptUnit node, final Set replacements, final IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("", 100); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractInterfaceProcessor_creating);
			final Collection collection= (Collection) fTypeOccurrences.get(unit);
			if (collection != null && !collection.isEmpty()) {
				final IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 100);
				try {
					subMonitor.beginTask("", collection.size() * 10); //$NON-NLS-1$
					subMonitor.setTaskName(RefactoringCoreMessages.ExtractInterfaceProcessor_creating);
					TType estimate= null;
					ISourceConstraintVariable variable= null;
					CompilationUnitRewrite currentRewrite= null;
					final IJavaScriptUnit sourceUnit= rewrite.getCu();
					if (sourceUnit.equals(unit))
						currentRewrite= rewrite;
					else
						currentRewrite= new CompilationUnitRewrite(fOwner, unit, node);
					for (final Iterator iterator= collection.iterator(); iterator.hasNext();) {
						variable= (ISourceConstraintVariable) iterator.next();
						estimate= (TType) variable.getData(SuperTypeConstraintsSolver.DATA_TYPE_ESTIMATE);
						if (estimate != null && variable instanceof ITypeConstraintVariable) {
							final ASTNode result= NodeFinder.perform(node, ((ITypeConstraintVariable) variable).getRange().getSourceRange());
							if (result != null)
								rewriteTypeOccurrence(estimate, currentRewrite, result, currentRewrite.createCategorizedGroupDescription(RefactoringCoreMessages.SuperTypeRefactoringProcessor_update_type_occurrence, SET_SUPER_TYPE));
						}
						subMonitor.worked(10);
					}
					if (!sourceUnit.equals(unit)) {
						final TextChange change= currentRewrite.createChange();
						if (change != null)
							manager.manage(unit, change);
					}
				} finally {
					subMonitor.done();
				}
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Sets the supertype as replacement.
	 * 
	 * @param type
	 *            The supertype to set
	 */
	public final void setSuperType(final IType type) {
		Assert.isNotNull(type);

		fSuperType= type;
	}
}
