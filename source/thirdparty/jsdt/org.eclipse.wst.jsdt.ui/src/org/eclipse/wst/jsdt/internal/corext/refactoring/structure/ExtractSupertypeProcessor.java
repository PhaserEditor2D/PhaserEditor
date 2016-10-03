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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditCopier;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.ASTRequestor;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.core.formatter.CodeFormatter;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.JavaScriptRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility2;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CreateCompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.MultiStateCompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TextEditBasedChangeManager;
import org.eclipse.wst.jsdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.wst.jsdt.ui.CodeGeneration;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * Refactoring processor for the extract supertype refactoring.
 * 
 * 
 */
public final class ExtractSupertypeProcessor extends PullUpRefactoringProcessor {

	/** The extract attribute */
	private static final String ATTRIBUTE_EXTRACT= "extract"; //$NON-NLS-1$

	/** The types attribute */
	private static final String ATTRIBUTE_TYPES= "types"; //$NON-NLS-1$

	/** The extract supertype group category set */
	private static final GroupCategorySet SET_EXTRACT_SUPERTYPE= new GroupCategorySet(new GroupCategory("org.eclipse.wst.jsdt.internal.corext.extractSupertype", //$NON-NLS-1$
			RefactoringCoreMessages.ExtractSupertypeProcessor_category_name, RefactoringCoreMessages.ExtractSupertypeProcessor_category_description));

	/**
	 * The changes of the working copy layer (element type:
	 * &lt;IJavaScriptUnit, TextEditBasedChange&gt;)
	 * <p>
	 * The compilation units are all primary working copies or normal
	 * compilation units.
	 * </p>
	 */
	private final Map fLayerChanges= new HashMap();

	/** The possible extract supertype candidates, or the empty array */
	private IType[] fPossibleCandidates= {};

	/** The source of the supertype */
	private String fSuperSource;

	/** The name of the extracted type */
	private String fTypeName= ""; //$NON-NLS-1$

	/** The types where to extract the supertype */
	private IType[] fTypesToExtract= {};

	/**
	 * Creates a new extract supertype refactoring processor.
	 * 
	 * @param members
	 *            the members to extract, or <code>null</code> if invoked by
	 *            scripting
	 * @param settings
	 *            the code generation settings, or <code>null</code> if
	 *            invoked by scripting
	 */
	public ExtractSupertypeProcessor(final IMember[] members, final CodeGenerationSettings settings) {
		super(members, settings, true);
		if (members != null) {
			final IType declaring= getDeclaringType();
			if (declaring != null)
				fTypesToExtract= new IType[] { declaring};
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected final RefactoringStatus checkDeclaringSuperTypes(final IProgressMonitor monitor) throws JavaScriptModelException {
		return new RefactoringStatus();
	}
	
	protected CompilationUnitRewrite getCompilationUnitRewrite(final Map rewrites, final IJavaScriptUnit unit) {
		Assert.isNotNull(rewrites);
		Assert.isNotNull(unit);
		CompilationUnitRewrite rewrite= (CompilationUnitRewrite) rewrites.get(unit);
		if (rewrite == null) {
			rewrite= new CompilationUnitRewrite(fOwner, unit);
			rewrite.rememberContent();
			rewrites.put(unit, rewrite);
		}
		return rewrite;
	}

	/**
	 * Checks whether the compilation unit to be extracted is valid.
	 * 
	 * @return a status describing the outcome of the
	 */
	public RefactoringStatus checkExtractedCompilationUnit() {
		final RefactoringStatus status= new RefactoringStatus();
		final IJavaScriptUnit cu= getDeclaringType().getJavaScriptUnit();
		if (fTypeName == null || "".equals(fTypeName)) //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.Checks_Choose_name);
		status.merge(Checks.checkTypeName(fTypeName));
		if (status.hasFatalError())
			return status;
		status.merge(Checks.checkCompilationUnitName(JavaModelUtil.getRenamedCUName(cu, fTypeName)));
		if (status.hasFatalError())
			return status;
		status.merge(Checks.checkCompilationUnitNewName(cu, fTypeName));
		return status;
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus checkFinalConditions(final IProgressMonitor monitor, final CheckConditionsContext context) throws CoreException, OperationCanceledException {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractSupertypeProcessor_checking);
			status.merge(checkExtractedCompilationUnit());
			if (status.hasFatalError())
				return status;
			return super.checkFinalConditions(new SubProgressMonitor(monitor, 1), context);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Computes the destination type based on the new name.
	 * 
	 * @param name the new name
	 * @return the destination type
	 */
	public IType computeExtractedType(final String name) {
		if (name != null && !name.equals("")) {//$NON-NLS-1$
			final IType declaring= getDeclaringType();
			try {
				final IJavaScriptUnit[] units= declaring.getPackageFragment().getJavaScriptUnits(fOwner);
				final String newName= JavaModelUtil.getRenamedCUName(declaring.getJavaScriptUnit(), name);
				IJavaScriptUnit result= null;
				for (int index= 0; index < units.length; index++) {
					if (units[index].getElementName().equals(newName))
						result= units[index];
				}
				if (result != null) {
					final IType type= result.getType(name);
					setDestinationType(type);
					return type;
				}
			} catch (JavaScriptModelException exception) {
				JavaScriptPlugin.log(exception);
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Change createChange(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
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
			final String description= Messages.format(RefactoringCoreMessages.ExtractSupertypeProcessor_descriptor_description_short, fTypeName);
			final String header= Messages.format(RefactoringCoreMessages.ExtractSupertypeProcessor_descriptor_description, new String[] { JavaScriptElementLabels.getElementLabel(fDestinationType, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getElementLabel(fCachedDeclaringType, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)});
			final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
			final IType[] types= getTypesToExtract();
			String[] settings= new String[types.length];
			for (int index= 0; index < settings.length; index++)
				settings[index]= JavaScriptElementLabels.getElementLabel(types[index], JavaScriptElementLabels.ALL_FULLY_QUALIFIED);
			comment.addSetting(JDTRefactoringDescriptorComment.createCompositeSetting(RefactoringCoreMessages.ExtractSupertypeProcessor_subtypes_pattern, settings));
			comment.addSetting(Messages.format(RefactoringCoreMessages.ExtractSupertypeProcessor_refactored_element_pattern, JavaScriptElementLabels.getElementLabel(fDestinationType, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)));
			settings= new String[fMembersToMove.length];
			for (int index= 0; index < settings.length; index++)
				settings[index]= JavaScriptElementLabels.getElementLabel(fMembersToMove[index], JavaScriptElementLabels.ALL_FULLY_QUALIFIED);
			comment.addSetting(JDTRefactoringDescriptorComment.createCompositeSetting(RefactoringCoreMessages.ExtractInterfaceProcessor_extracted_members_pattern, settings));
			addSuperTypeSettings(comment, true);
			final JDTRefactoringDescriptor descriptor= new JDTRefactoringDescriptor(IJavaScriptRefactorings.EXTRACT_SUPERCLASS, project, description, comment.asString(), arguments, flags);
			arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_NAME, fTypeName);
			arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(getDeclaringType()));
			arguments.put(ATTRIBUTE_REPLACE, Boolean.valueOf(fReplace).toString());
			arguments.put(ATTRIBUTE_INSTANCEOF, Boolean.valueOf(fInstanceOf).toString());
			arguments.put(ATTRIBUTE_STUBS, Boolean.valueOf(fCreateMethodStubs).toString());
			arguments.put(ATTRIBUTE_EXTRACT, Integer.valueOf(fMembersToMove.length).toString());
			for (int offset= 0; offset < fMembersToMove.length; offset++)
				arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + 1), descriptor.elementToHandle(fMembersToMove[offset]));
			arguments.put(ATTRIBUTE_DELETE, Integer.valueOf(fDeletedMethods.length).toString());
			for (int offset= 0; offset < fDeletedMethods.length; offset++)
				arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + fMembersToMove.length + 1), descriptor.elementToHandle(fDeletedMethods[offset]));
			arguments.put(ATTRIBUTE_ABSTRACT, Integer.valueOf(fAbstractMethods.length).toString());
			for (int offset= 0; offset < fAbstractMethods.length; offset++)
				arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + fMembersToMove.length + fDeletedMethods.length + 1), descriptor.elementToHandle(fAbstractMethods[offset]));
			arguments.put(ATTRIBUTE_TYPES, Integer.valueOf(fTypesToExtract.length).toString());
			for (int offset= 0; offset < fTypesToExtract.length; offset++)
				arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + fMembersToMove.length + fDeletedMethods.length + fAbstractMethods.length + 1), descriptor.elementToHandle(fTypesToExtract[offset]));
			final DynamicValidationRefactoringChange change= new DynamicValidationRefactoringChange(descriptor, RefactoringCoreMessages.ExtractSupertypeProcessor_extract_supertype, fChangeManager.getAllChanges());
			final IFile file= ResourceUtil.getFile(declaring.getJavaScriptUnit());
			if (fSuperSource != null && fSuperSource.length() > 0)
				change.add(new CreateCompilationUnitChange(declaring.getPackageFragment().getJavaScriptUnit(JavaModelUtil.getRenamedCUName(declaring.getJavaScriptUnit(), fTypeName)), fSuperSource, file.getCharset(false)));
			return change;
		} finally {
			monitor.done();
			clearCaches();
		}
	}

	/**
	 * Creates the new extracted supertype.
	 * 
	 * @param superType
	 *            the super type, or <code>null</code> if no super type (ie.
	 *            <code>java.lang.Object</code>) is available
	 * @param monitor
	 *            the progress monitor
	 * @return a status describing the outcome of the operation
	 * @throws CoreException
	 *             if an error occurs
	 */
	protected final RefactoringStatus createExtractedSuperType(final IType superType, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(monitor);
		fSuperSource= null;
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask(RefactoringCoreMessages.ExtractSupertypeProcessor_preparing, 20);
			final IType declaring= getDeclaringType();
			final CompilationUnitRewrite declaringRewrite= new CompilationUnitRewrite(fOwner, declaring.getJavaScriptUnit());
			final AbstractTypeDeclaration declaringDeclaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(declaring, declaringRewrite.getRoot());
			if (declaringDeclaration != null) {
				final String name= JavaModelUtil.getRenamedCUName(declaring.getJavaScriptUnit(), fTypeName);
				final IJavaScriptUnit original= declaring.getPackageFragment().getJavaScriptUnit(name);
				final IJavaScriptUnit copy= getSharedWorkingCopy(original.getPrimary(), new SubProgressMonitor(monitor, 10));
				fSuperSource= createSuperTypeSource(copy, superType, declaringDeclaration, status, new SubProgressMonitor(monitor, 10));
				if (fSuperSource != null) {
					copy.getBuffer().setContents(fSuperSource);
					JavaModelUtil.reconcile(copy);
				}
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * Creates a working copy for the modified subtype.
	 * 
	 * @param unit
	 *            the compilation unit
	 * @param root
	 *            the compilation unit node
	 * @param subDeclaration
	 *            the declaration of the subtype to modify
	 * @param extractedType
	 *            the extracted super type
	 * @param extractedBinding
	 *            the binding of the extracted super type
	 * @param status
	 *            the refactoring status
	 */
	protected final void createModifiedSubType(final IJavaScriptUnit unit, final JavaScriptUnit root, final IType extractedType, final ITypeBinding extractedBinding, final AbstractTypeDeclaration subDeclaration, final RefactoringStatus status) {
		Assert.isNotNull(unit);
		Assert.isNotNull(subDeclaration);
		Assert.isNotNull(extractedType);
		try {
			final CompilationUnitRewrite rewrite= new CompilationUnitRewrite(fOwner, unit, root);
			createTypeSignature(rewrite, subDeclaration, extractedType, extractedBinding, new NullProgressMonitor());
			final Document document= new Document(unit.getBuffer().getContents());
			final CompilationUnitChange change= rewrite.createChange();
			if (change != null) {
				fLayerChanges.put(unit.getPrimary(), change);
				final TextEdit edit= change.getEdit();
				if (edit != null) {
					final TextEditCopier copier= new TextEditCopier(edit);
					final TextEdit copy= copier.perform();
					copy.apply(document, TextEdit.NONE);
				}
			}
			final IJavaScriptUnit copy= getSharedWorkingCopy(unit, new NullProgressMonitor());
			copy.getBuffer().setContents(document.get());
			JavaModelUtil.reconcile(copy);
		} catch (CoreException exception) {
			JavaScriptPlugin.log(exception);
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractSupertypeProcessor_unexpected_exception_on_layer));
		} catch (MalformedTreeException exception) {
			JavaScriptPlugin.log(exception);
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractSupertypeProcessor_unexpected_exception_on_layer));
		} catch (BadLocationException exception) {
			JavaScriptPlugin.log(exception);
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractSupertypeProcessor_unexpected_exception_on_layer));
		}
	}

	/**
	 * Creates the necessary constructors for the extracted supertype.
	 * 
	 * @param targetRewrite
	 *            the target compilation unit rewrite
	 * @param superType
	 *            the super type, or <code>null</code> if no super type (ie.
	 *            <code>java.lang.Object</code>) is available
	 * @param targetDeclaration
	 *            the type declaration of the target type
	 * @param status
	 *            the refactoring status
	 */
	protected final void createNecessaryConstructors(final CompilationUnitRewrite targetRewrite, final IType superType, final AbstractTypeDeclaration targetDeclaration, final RefactoringStatus status) {
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(targetDeclaration);
		if (superType != null) {
			final ITypeBinding binding= targetDeclaration.resolveBinding();
			if (binding != null && binding.isClass()) {
				final IFunctionBinding[] bindings= StubUtility2.getVisibleConstructors(binding, true, true);
				int deprecationCount= 0;
				for (int i= 0; i < bindings.length; i++) {
					if (bindings[i].isDeprecated())
						deprecationCount++;
				}
				final ListRewrite rewrite= targetRewrite.getASTRewrite().getListRewrite(targetDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
				if (rewrite != null) {
					boolean createDeprecated= deprecationCount == bindings.length;
					for (int i= 0; i < bindings.length; i++) {
						IFunctionBinding curr= bindings[i];
						if (!curr.isDeprecated() || createDeprecated) {
							FunctionDeclaration stub;
							try {
								stub= StubUtility2.createConstructorStub(targetRewrite.getCu(), targetRewrite.getASTRewrite(), targetRewrite.getImportRewrite(), curr, binding.getName(), Modifier.PUBLIC, false, false, fSettings);
								if (stub != null)
									rewrite.insertLast(stub, null);
							} catch (CoreException exception) {
								JavaScriptPlugin.log(exception);
								status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractSupertypeProcessor_unexpected_exception_on_layer));
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Creates the source for the new compilation unit containing the supertype.
	 * 
	 * @param extractedWorkingCopy
	 *            the working copy of the new extracted supertype
	 * @param superType
	 *            the super type, or <code>null</code> if no super type (ie.
	 *            <code>java.lang.Object</code>) is available
	 * @param declaringDeclaration
	 *            the declaration of the declaring type
	 * @param status
	 *            the refactoring status
	 * @param monitor
	 *            the progress monitor to display progress
	 * @return the source of the new compilation unit, or <code>null</code>
	 * @throws CoreException
	 *             if an error occurs
	 */
	protected final String createSuperTypeSource(final IJavaScriptUnit extractedWorkingCopy, final IType superType, final AbstractTypeDeclaration declaringDeclaration, final RefactoringStatus status, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(extractedWorkingCopy);
		Assert.isNotNull(declaringDeclaration);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		String source= null;
		try {
			monitor.beginTask("", 2); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractSupertypeProcessor_preparing);
			final IType declaring= getDeclaringType();
			final String delimiter= StubUtility.getLineDelimiterUsed(extractedWorkingCopy.getJavaScriptProject());
			String typeComment= null;
			String fileComment= null;
			if (fSettings.createComments) {
				typeComment= CodeGeneration.getTypeComment(extractedWorkingCopy, fTypeName, delimiter);
				fileComment= CodeGeneration.getFileComment(extractedWorkingCopy, delimiter);
			}
			final StringBuffer buffer= new StringBuffer(64);
			final ITypeBinding binding= declaringDeclaration.resolveBinding();
			if (binding != null) {
				final ITypeBinding superBinding= binding.getSuperclass();
				if (superBinding != null)
					fTypeBindings.add(superBinding);
			}
			final String imports= createTypeImports(extractedWorkingCopy, monitor);
			if (imports != null && !"".equals(imports)) { //$NON-NLS-1$
				buffer.append(imports);
			}
			createTypeDeclaration(extractedWorkingCopy, superType, declaringDeclaration, typeComment, buffer, status, new SubProgressMonitor(monitor, 1));
			source= createTypeTemplate(extractedWorkingCopy, "", fileComment, "", buffer.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			if (source == null) {
				if (!declaring.getPackageFragment().isDefaultPackage()) {
					if (imports.length() > 0)
						buffer.insert(0, imports);
					buffer.insert(0, "package " + declaring.getPackageFragment().getElementName() + ";"); //$NON-NLS-1$//$NON-NLS-2$
				}
				source= buffer.toString();
			}
			final IDocument document= new Document(source);
			final TextEdit edit= CodeFormatterUtil.format2(CodeFormatter.K_JAVASCRIPT_UNIT, source, 0, delimiter, extractedWorkingCopy.getJavaScriptProject().getOptions(true));
			if (edit != null) {
				try {
					edit.apply(document, TextEdit.UPDATE_REGIONS);
				} catch (MalformedTreeException exception) {
					JavaScriptPlugin.log(exception);
					status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractSupertypeProcessor_unexpected_exception_on_layer));
				} catch (BadLocationException exception) {
					JavaScriptPlugin.log(exception);
					status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractSupertypeProcessor_unexpected_exception_on_layer));
				}
				source= document.get();
			}
		} finally {
			monitor.done();
		}
		return source;
	}

	/**
	 * Creates the declaration of the new supertype, excluding any comments or
	 * package declaration.
	 * 
	 * @param extractedWorkingCopy
	 *            the working copy of the new extracted supertype
	 * @param superType
	 *            the super type, or <code>null</code> if no super type (ie.
	 *            <code>java.lang.Object</code>) is available
	 * @param declaringDeclaration
	 *            the declaration of the declaring type
	 * @param comment
	 *            the comment of the new type declaration
	 * @param buffer
	 *            the string buffer containing the declaration
	 * @param status
	 *            the refactoring status
	 * @param monitor
	 *            the progress monitor to use
	 * @throws CoreException
	 *             if an error occurs
	 */
	protected final void createTypeDeclaration(final IJavaScriptUnit extractedWorkingCopy, final IType superType, final AbstractTypeDeclaration declaringDeclaration, final String comment, final StringBuffer buffer, final RefactoringStatus status, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(extractedWorkingCopy);
		Assert.isNotNull(declaringDeclaration);
		Assert.isNotNull(buffer);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractSupertypeProcessor_preparing);
			final IJavaScriptProject project= extractedWorkingCopy.getJavaScriptProject();
			final String delimiter= StubUtility.getLineDelimiterUsed(project);
			if (comment != null && !"".equals(comment)) { //$NON-NLS-1$
				buffer.append(comment);
				buffer.append(delimiter);
			}
			buffer.append(JdtFlags.VISIBILITY_STRING_PUBLIC);
			if (superType != null && Flags.isAbstract(superType.getFlags())) {
				buffer.append(' ');
				buffer.append("abstract "); //$NON-NLS-1$
			}
			buffer.append(' ');
			buffer.append("class "); //$NON-NLS-1$
			buffer.append(fTypeName);
			if (superType != null && !"java.lang.Object".equals(superType.getFullyQualifiedName())) { //$NON-NLS-1$
				buffer.append(' ');
				buffer.append("extends "); //$NON-NLS-1$
				buffer.append(superType.getElementName());
			}
			buffer.append(" {"); //$NON-NLS-1$
			buffer.append(delimiter);
			buffer.append(delimiter);
			buffer.append('}');
			final String string= buffer.toString();
			extractedWorkingCopy.getBuffer().setContents(string);
			final IDocument document= new Document(string);
			final CompilationUnitRewrite targetRewrite= new CompilationUnitRewrite(fOwner, extractedWorkingCopy);
			final AbstractTypeDeclaration targetDeclaration= (AbstractTypeDeclaration) targetRewrite.getRoot().types().get(0);
			createTypeSignature(targetRewrite, superType, declaringDeclaration, targetDeclaration);
			createNecessaryConstructors(targetRewrite, superType, targetDeclaration, status);
			final TextEdit edit= targetRewrite.createChange().getEdit();
			try {
				edit.apply(document, TextEdit.UPDATE_REGIONS);
			} catch (MalformedTreeException exception) {
				JavaScriptPlugin.log(exception);
				status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractSupertypeProcessor_unexpected_exception_on_layer));
			} catch (BadLocationException exception) {
				JavaScriptPlugin.log(exception);
				status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractSupertypeProcessor_unexpected_exception_on_layer));
			}
			buffer.setLength(0);
			buffer.append(document.get());
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates a new type signature of a subtype.
	 * 
	 * @param subRewrite
	 *            the compilation unit rewrite of a subtype
	 * @param declaration
	 *            the type declaration of a subtype
	 * @param extractedType
	 *            the extracted super type
	 * @param extractedBinding
	 *            the binding of the extracted super type
	 * @param monitor
	 *            the progress monitor to use
	 * @throws JavaScriptModelException
	 *             if the type parameters cannot be retrieved
	 */
	protected final void createTypeSignature(final CompilationUnitRewrite subRewrite, final AbstractTypeDeclaration declaration, final IType extractedType, final ITypeBinding extractedBinding, final IProgressMonitor monitor) throws JavaScriptModelException {
		Assert.isNotNull(subRewrite);
		Assert.isNotNull(declaration);
		Assert.isNotNull(extractedType);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask(RefactoringCoreMessages.ExtractSupertypeProcessor_preparing, 10);
			final AST ast= subRewrite.getAST();
			Type type= null;
			if (extractedBinding != null) {
				type= subRewrite.getImportRewrite().addImport(extractedBinding, ast);
			} else {
				subRewrite.getImportRewrite().addImport(extractedType.getFullyQualifiedName('.'));
				type= ast.newSimpleType(ast.newSimpleName(extractedType.getElementName()));
			}
			subRewrite.getImportRemover().registerAddedImport(extractedType.getFullyQualifiedName('.'));
			final ASTRewrite rewriter= subRewrite.getASTRewrite();
			if (type != null && declaration instanceof TypeDeclaration) {
				final TypeDeclaration extended= (TypeDeclaration) declaration;
				final Type superClass= extended.getSuperclassType();
				if (superClass != null) {
					rewriter.replace(superClass, type, subRewrite.createCategorizedGroupDescription(RefactoringCoreMessages.ExtractSupertypeProcessor_add_supertype, SET_EXTRACT_SUPERTYPE));
					subRewrite.getImportRemover().registerRemovedNode(superClass);
				} else
					rewriter.set(extended, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, type, subRewrite.createCategorizedGroupDescription(RefactoringCoreMessages.ExtractSupertypeProcessor_add_supertype, SET_EXTRACT_SUPERTYPE));
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates the type signature of the extracted supertype.
	 * 
	 * @param targetRewrite
	 *            the target compilation unit rewrite
	 * @param superType
	 *            the super type, or <code>null</code> if no super type (ie.
	 *            <code>java.lang.Object</code>) is available
	 * @param declaringDeclaration
	 *            the declaration of the declaring type
	 * @param targetDeclaration
	 *            the type declaration of the target type
	 */
	protected final void createTypeSignature(final CompilationUnitRewrite targetRewrite, final IType superType, final AbstractTypeDeclaration declaringDeclaration, final AbstractTypeDeclaration targetDeclaration) {
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(declaringDeclaration);
		Assert.isNotNull(targetDeclaration);
		if (declaringDeclaration instanceof TypeDeclaration) {
			final TypeDeclaration declaration= (TypeDeclaration) declaringDeclaration;
			final Type superclassType= declaration.getSuperclassType();
			if (superclassType != null) {
				Type type= null;
				final ITypeBinding binding= superclassType.resolveBinding();
				if (binding != null) {
					type= targetRewrite.getImportRewrite().addImport(binding, targetRewrite.getAST());
					targetRewrite.getImportRemover().registerAddedImports(type);
				}
				if (type != null && targetDeclaration instanceof TypeDeclaration) {
					final TypeDeclaration extended= (TypeDeclaration) targetDeclaration;
					final Type targetSuperType= extended.getSuperclassType();
					if (targetSuperType != null) {
						targetRewrite.getASTRewrite().replace(targetSuperType, type, null);
					} else {
						targetRewrite.getASTRewrite().set(extended, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, type, null);
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final RefactoringStatus createWorkingCopyLayer(final IProgressMonitor monitor) {
		Assert.isNotNull(monitor);
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask(RefactoringCoreMessages.ExtractSupertypeProcessor_preparing, 70);
			status.merge(super.createWorkingCopyLayer(new SubProgressMonitor(monitor, 10)));
			final IType declaring= getDeclaringType();
			status.merge(createExtractedSuperType(getDeclaringSuperTypeHierarchy(new SubProgressMonitor(monitor, 10)).getSuperclass(declaring), new SubProgressMonitor(monitor, 10)));
			if (status.hasFatalError())
				return status;
			final IType extractedType= computeExtractedType(fTypeName);
			setDestinationType(extractedType);
			final List subTypes= new ArrayList(Arrays.asList(fTypesToExtract));
			if (!subTypes.contains(declaring))
				subTypes.add(declaring);
			final Map unitToTypes= new HashMap(subTypes.size());
			final Set units= new HashSet(subTypes.size());
			for (int index= 0; index < subTypes.size(); index++) {
				final IType type= (IType) subTypes.get(index);
				final IJavaScriptUnit unit= type.getJavaScriptUnit();
				units.add(unit);
				Collection collection= (Collection) unitToTypes.get(unit);
				if (collection == null) {
					collection= new ArrayList(2);
					unitToTypes.put(unit, collection);
				}
				collection.add(type);
			}
			final Map projectToUnits= new HashMap();
			Collection collection= null;
			IJavaScriptProject project= null;
			IJavaScriptUnit current= null;
			for (final Iterator iterator= units.iterator(); iterator.hasNext();) {
				current= (IJavaScriptUnit) iterator.next();
				project= current.getJavaScriptProject();
				collection= (Collection) projectToUnits.get(project);
				if (collection == null) {
					collection= new ArrayList();
					projectToUnits.put(project, collection);
				}
				collection.add(current);
			}
			final ITypeBinding[] extractBindings= { null};
			final ASTParser extractParser= ASTParser.newParser(AST.JLS3);
			extractParser.setWorkingCopyOwner(fOwner);
			extractParser.setResolveBindings(true);
			extractParser.setProject(project);
			extractParser.setSource(extractedType.getJavaScriptUnit());
			final JavaScriptUnit extractUnit= (JavaScriptUnit) extractParser.createAST(new SubProgressMonitor(monitor, 10));
			if (extractUnit != null) {
				final AbstractTypeDeclaration extractDeclaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(extractedType, extractUnit);
				if (extractDeclaration != null)
					extractBindings[0]= extractDeclaration.resolveBinding();
			}
			final ASTParser parser= ASTParser.newParser(AST.JLS3);
			final IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 30);
			try {
				final Set keySet= projectToUnits.keySet();
				subMonitor.beginTask("", keySet.size()); //$NON-NLS-1$
				subMonitor.setTaskName(RefactoringCoreMessages.ExtractSupertypeProcessor_preparing);
				for (final Iterator iterator= keySet.iterator(); iterator.hasNext();) {
					project= (IJavaScriptProject) iterator.next();
					collection= (Collection) projectToUnits.get(project);
					parser.setWorkingCopyOwner(fOwner);
					parser.setResolveBindings(true);
					parser.setProject(project);
					parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
					final IProgressMonitor subsubMonitor= new SubProgressMonitor(subMonitor, 1);
					try {
						subsubMonitor.beginTask("", collection.size()); //$NON-NLS-1$
						subsubMonitor.setTaskName(RefactoringCoreMessages.ExtractSupertypeProcessor_preparing);
						parser.createASTs((IJavaScriptUnit[]) collection.toArray(new IJavaScriptUnit[collection.size()]), new String[0], new ASTRequestor() {

							public final void acceptAST(final IJavaScriptUnit unit, final JavaScriptUnit node) {
								try {
									final Collection types= (Collection) unitToTypes.get(unit);
									if (types != null) {
										for (final Iterator innerIterator= types.iterator(); innerIterator.hasNext();) {
											final IType currentType= (IType) innerIterator.next();
											final AbstractTypeDeclaration currentDeclaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(currentType, node);
											if (currentDeclaration != null)
												createModifiedSubType(unit, node, extractedType, extractBindings[0], currentDeclaration, status);
										}
									}
								} catch (CoreException exception) {
									JavaScriptPlugin.log(exception);
									status.merge(RefactoringStatus.createFatalErrorStatus(exception.getLocalizedMessage()));
								} finally {
									subsubMonitor.worked(1);
								}
							}

							public final void acceptBinding(final String key, final IBinding binding) {
								// Do nothing
							}
						}, subsubMonitor);
					} finally {
						subsubMonitor.done();
					}
				}
			} finally {
				subMonitor.done();
			}
		} catch (CoreException exception) {
			JavaScriptPlugin.log(exception);
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractSupertypeProcessor_unexpected_exception_on_layer));
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * {@inheritDoc}
	 */
	public IType[] getCandidateTypes(final RefactoringStatus status, final IProgressMonitor monitor) {
		Assert.isNotNull(monitor);
		if (fPossibleCandidates == null || fPossibleCandidates.length == 0) {
			final IType declaring= getDeclaringType();
			if (declaring != null) {
				try {
					monitor.beginTask(RefactoringCoreMessages.ExtractSupertypeProcessor_computing_possible_types, 10);
					final IType superType= getDeclaringSuperTypeHierarchy(new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)).getSuperclass(declaring);
					if (superType != null) {
						fPossibleCandidates= superType.newTypeHierarchy(fOwner, new SubProgressMonitor(monitor, 9, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)).getSubclasses(superType);
						final LinkedList list= new LinkedList(Arrays.asList(fPossibleCandidates));
						final Set names= new HashSet();
						for (final Iterator iterator= list.iterator(); iterator.hasNext();) {
							final IType type= (IType) iterator.next();
							if (type.isReadOnly() || type.isBinary() || type.isAnonymous() || !type.isClass() || names.contains(type.getFullyQualifiedName()))
								iterator.remove();
							else
								names.add(type.getFullyQualifiedName());
						}
						fPossibleCandidates= (IType[]) list.toArray(new IType[list.size()]);
					}
				} catch (JavaScriptModelException exception) {
					JavaScriptPlugin.log(exception);
				} finally {
					monitor.done();
				}
			}
		}
		return fPossibleCandidates;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object[] getElements() {
		return new Object[] { getDeclaringType()};
	}

	/**
	 * Returns the extracted type.
	 * 
	 * @return the extracted type, or <code>null</code>
	 */
	public IType getExtractedType() {
		return getDestinationType();
	}

	/**
	 * Returns the type name.
	 * 
	 * @return the type name
	 */
	public String getTypeName() {
		return fTypeName;
	}

	/**
	 * Returns the types to extract. The declaring type may or may not be
	 * contained in the result.
	 * 
	 * @return the types to extract
	 */
	public IType[] getTypesToExtract() {
		return fTypesToExtract;
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus initialize(final RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			final String name= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_NAME);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				fTypeName= name;
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_NAME));
			String handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || element.getElementType() != IJavaScriptElement.TYPE)
					return ScriptableRefactoring.createInputFatalStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.EXTRACT_SUPERCLASS);
				IType type= null;
				final IJavaScriptUnit unit= ((IType) element).getJavaScriptUnit();
				if (unit != null && unit.exists()) {
					try {
						final IJavaScriptUnit copy= getSharedWorkingCopy(unit, new NullProgressMonitor());
						final IJavaScriptElement[] elements= copy.findElements(element);
						if (elements != null && elements.length == 1 && elements[0] instanceof IType && elements[0].exists())
							type= (IType) elements[0];
					} catch (JavaScriptModelException exception) {
						// TODO: log exception
					}
				}
				if (type != null)
					fCachedDeclaringType= type;
				else
					return ScriptableRefactoring.createInputFatalStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.EXTRACT_SUPERCLASS);
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
			final String stubs= extended.getAttribute(ATTRIBUTE_STUBS);
			if (stubs != null) {
				fCreateMethodStubs= Boolean.valueOf(stubs).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_STUBS));
			final String instance= extended.getAttribute(ATTRIBUTE_INSTANCEOF);
			if (instance != null) {
				fInstanceOf= Boolean.valueOf(instance).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_INSTANCEOF));
			final String replace= extended.getAttribute(ATTRIBUTE_REPLACE);
			if (replace != null) {
				fReplace= Boolean.valueOf(replace).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_REPLACE));
			int extractCount= 0;
			int abstractCount= 0;
			int deleteCount= 0;
			int typeCount= 0;
			String value= extended.getAttribute(ATTRIBUTE_ABSTRACT);
			if (value != null && !"".equals(value)) {//$NON-NLS-1$
				try {
					abstractCount= Integer.parseInt(value);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_ABSTRACT));
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_ABSTRACT));
			value= extended.getAttribute(ATTRIBUTE_DELETE);
			if (value != null && !"".equals(value)) {//$NON-NLS-1$
				try {
					deleteCount= Integer.parseInt(value);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DELETE));
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DELETE));
			value= extended.getAttribute(ATTRIBUTE_EXTRACT);
			if (value != null && !"".equals(value)) {//$NON-NLS-1$
				try {
					extractCount= Integer.parseInt(value);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_EXTRACT));
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_EXTRACT));
			value= extended.getAttribute(ATTRIBUTE_TYPES);
			if (value != null && !"".equals(value)) {//$NON-NLS-1$
				try {
					typeCount= Integer.parseInt(value);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_TYPES));
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_TYPES));
			final RefactoringStatus status= new RefactoringStatus();
			List elements= new ArrayList();
			for (int index= 0; index < extractCount; index++) {
				final String attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (index + 1);
				handle= extended.getAttribute(attribute);
				if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
					final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(fOwner, extended.getProject(), handle, false);
					if (element == null || !element.exists())
						status.merge(ScriptableRefactoring.createInputWarningStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.EXTRACT_SUPERCLASS));
					else
						elements.add(element);
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
			}
			fMembersToMove= (IMember[]) elements.toArray(new IMember[elements.size()]);
			elements= new ArrayList();
			for (int index= 0; index < deleteCount; index++) {
				final String attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (extractCount + index + 1);
				handle= extended.getAttribute(attribute);
				if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
					final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(fOwner, extended.getProject(), handle, false);
					if (element == null || !element.exists())
						status.merge(ScriptableRefactoring.createInputWarningStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.EXTRACT_SUPERCLASS));
					else
						elements.add(element);
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
			}
			fDeletedMethods= (IFunction[]) elements.toArray(new IFunction[elements.size()]);
			elements= new ArrayList();
			for (int index= 0; index < abstractCount; index++) {
				final String attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (extractCount + abstractCount + index + 1);
				handle= extended.getAttribute(attribute);
				if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
					final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(fOwner, extended.getProject(), handle, false);
					if (element == null || !element.exists())
						status.merge(ScriptableRefactoring.createInputWarningStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.EXTRACT_SUPERCLASS));
					else
						elements.add(element);
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
			}
			fAbstractMethods= (IFunction[]) elements.toArray(new IFunction[elements.size()]);
			elements= new ArrayList();
			for (int index= 0; index < typeCount; index++) {
				final String attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + (extractCount + abstractCount + deleteCount + index + 1);
				handle= extended.getAttribute(attribute);
				if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
					final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(fOwner, extended.getProject(), handle, false);
					if (element == null || !element.exists())
						status.merge(ScriptableRefactoring.createInputFatalStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.EXTRACT_SUPERCLASS));
					else
						elements.add(element);
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, attribute));
			}
			fTypesToExtract= (IType[]) elements.toArray(new IType[elements.size()]);
			IJavaScriptProject project= null;
			if (fMembersToMove.length > 0)
				project= fMembersToMove[0].getJavaScriptProject();
			fSettings= JavaPreferencesSettings.getCodeGenerationSettings(project);
			if (!status.isOK())
				return status;
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}

	/**
	 * {@inheritDoc}
	 */
	protected void registerChanges(final TextEditBasedChangeManager manager) throws CoreException {
		try {
			final IJavaScriptUnit extractedUnit= getExtractedType().getJavaScriptUnit();
			IJavaScriptUnit unit= null;
			CompilationUnitRewrite rewrite= null;
			for (final Iterator iterator= fCompilationUnitRewrites.keySet().iterator(); iterator.hasNext();) {
				unit= (IJavaScriptUnit) iterator.next();
				if (unit.equals(extractedUnit)) {
					rewrite= (CompilationUnitRewrite) fCompilationUnitRewrites.get(unit);
					if (rewrite != null) {
						CompilationUnitChange change= rewrite.createChange();

						if (change != null) {
							final TextEdit edit= ((TextChange) change).getEdit();
							if (edit != null) {
								final IDocument document= new Document(fSuperSource);
								try {
									edit.apply(document, TextEdit.UPDATE_REGIONS);
								} catch (MalformedTreeException exception) {
									JavaScriptPlugin.log(exception);
								} catch (BadLocationException exception) {
									JavaScriptPlugin.log(exception);
								}
								fSuperSource= document.get();
								manager.remove(extractedUnit);
							}
						}
					}
				} else {
					rewrite= (CompilationUnitRewrite) fCompilationUnitRewrites.get(unit);
					if (rewrite != null) {
						final CompilationUnitChange layerChange= (CompilationUnitChange) fLayerChanges.get(unit.getPrimary());
						final CompilationUnitChange rewriteChange= rewrite.createChange();
						if (rewriteChange != null && layerChange != null) {
							final MultiStateCompilationUnitChange change= new MultiStateCompilationUnitChange(rewriteChange.getName(), unit);
							change.addChange(layerChange);
							change.addChange(rewriteChange);
							fLayerChanges.remove(unit.getPrimary());
							manager.manage(unit, change);
						} else if (layerChange != null) {
							manager.manage(unit, layerChange);
							fLayerChanges.remove(unit.getPrimary());
						} else if (rewriteChange != null) {
							manager.manage(unit, rewriteChange);
						}
					}
				}
			}
			for (Iterator iterator= fLayerChanges.entrySet().iterator(); iterator.hasNext();) {
				final Map.Entry entry= (Map.Entry) iterator.next();
				manager.manage((IJavaScriptUnit) entry.getKey(), (TextEditBasedChange) entry.getValue());
			}
			IJavaScriptUnit[] units= manager.getAllCompilationUnits();
			for (int index= 0; index < units.length; index++) {
				if (units[index].getPath().equals(extractedUnit.getPath()))
					manager.remove(units[index]);
			}
		} finally {
			fLayerChanges.clear();
		}
	}

	/**
	 * Resets the changes necessary for the working copy layer.
	 */
	public void resetChanges() {
		fLayerChanges.clear();
	}

	/**
	 * Sets the type name.
	 * 
	 * @param name
	 *            the type name
	 */
	public void setTypeName(final String name) {
		Assert.isNotNull(name);
		fTypeName= name;
	}

	/**
	 * Sets the types to extract. Must be a subset of
	 * <code>getPossibleCandidates()</code>. If the declaring type is not
	 * contained, it will automatically be added.
	 * 
	 * @param types
	 *            the types to extract
	 */
	public void setTypesToExtract(final IType[] types) {
		Assert.isNotNull(types);
		fTypesToExtract= types;
	}
}
