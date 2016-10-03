/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     jens.lukowski@gmx.de - contributed code to convert prefix and postfix 
 *       expressions into a combination of setter and getter calls.
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fix for
 *       bug Encapsulate field can fail when two variables in one variable declaration (see
 *       https://bugs.eclipse.org/bugs/show_bug.cgi?id=51540).
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.sef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.NamingConventions;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.Message;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.JavaScriptRefactoringDescriptor;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.BindingLabelProvider;
import org.eclipse.wst.jsdt.ui.CodeGeneration;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * Encapsulates a field into getter and setter calls.
 */
public class SelfEncapsulateFieldRefactoring extends ScriptableRefactoring {

	private static final String ATTRIBUTE_VISIBILITY= "visibility"; //$NON-NLS-1$
	private static final String ATTRIBUTE_GETTER= "getter"; //$NON-NLS-1$
	private static final String ATTRIBUTE_SETTER= "setter"; //$NON-NLS-1$
	private static final String ATTRIBUTE_INSERTION= "insertion"; //$NON-NLS-1$
	private static final String ATTRIBUTE_COMMENTS= "comments"; //$NON-NLS-1$
	private static final String ATTRIBUTE_DECLARING= "declaring"; //$NON-NLS-1$

	private IField fField;
	private TextChangeManager fChangeManager;
	
	private JavaScriptUnit fRoot;
	private VariableDeclarationFragment fFieldDeclaration;
	private ASTRewrite fRewriter;
	private ImportRewrite fImportRewrite;

	private int fVisibility= -1;
	private String fGetterName;
	private String fSetterName;
	private String fArgName;
	private boolean fSetterMustReturnValue;
	private int fInsertionIndex;	// -1 represents as first method.
	private boolean fEncapsulateDeclaringClass;
	private boolean fGenerateJavadoc;
	
	private List fUsedReadNames;
	private List fUsedModifyNames;
	private boolean fConsiderVisibility=true;
	
	private static final String NO_NAME= ""; //$NON-NLS-1$
	
	/**
	 * Creates a new self encapsulate field refactoring.
	 * @param field the field, or <code>null</code> if invoked by scripting
	 * @throws JavaScriptModelException
	 */
	public SelfEncapsulateFieldRefactoring(IField field) throws JavaScriptModelException {
		fEncapsulateDeclaringClass= true;
		fChangeManager= new TextChangeManager();
		fField= field;
		if (field != null)
			initialize(field);
	}

	private void initialize(IField field) throws JavaScriptModelException {
		fGetterName= GetterSetterUtil.getGetterName(field, null);
		fSetterName= GetterSetterUtil.getSetterName(field, null);
		fArgName= NamingConventions.removePrefixAndSuffixForFieldName(field.getJavaScriptProject(), field.getElementName(), field.getFlags());
		checkArgName();
	}
	
	public IField getField() {
		return fField;
	}

	public String getGetterName() {
		return fGetterName;
	}
		
	public void setGetterName(String name) {
		fGetterName= name;
		Assert.isNotNull(fGetterName);
	}

	public String getSetterName() {
		return fSetterName;
	}
	
	public void setSetterName(String name) {
		fSetterName= name;
		Assert.isNotNull(fSetterName);
	}
	
	public void setInsertionIndex(int index) {
		fInsertionIndex= index;
	}
	
	public int getVisibility() {
		return fVisibility;
	}
	
	public void setVisibility(int visibility) {
		fVisibility= visibility;
	}
	
	public void setEncapsulateDeclaringClass(boolean encapsulateDeclaringClass) {
		fEncapsulateDeclaringClass= encapsulateDeclaringClass;
	}

	public boolean getEncapsulateDeclaringClass() {
		return fEncapsulateDeclaringClass;
	}
	
	public boolean getGenerateJavadoc() {
		return fGenerateJavadoc;
	}
	
	public void setGenerateJavadoc(boolean value) {
		fGenerateJavadoc= value;
	}

	//----activation checking ----------------------------------------------------------

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		if (fVisibility < 0)
			fVisibility= (fField.getFlags() & (Flags.AccPublic | Flags.AccPrivate));
		RefactoringStatus result=  new RefactoringStatus();
		result.merge(Checks.checkAvailability(fField));
		if (result.hasFatalError())
			return result;
		fRoot= new RefactoringASTParser(AST.JLS3).parse(fField.getJavaScriptUnit(), true, pm);
		ISourceRange sourceRange= fField.getNameRange();
		ASTNode node= NodeFinder.perform(fRoot, sourceRange.getOffset(), sourceRange.getLength());
		if (node == null) {
			return mappingErrorFound(result, node);
		}
		fFieldDeclaration= (VariableDeclarationFragment)ASTNodes.getParent(node, VariableDeclarationFragment.class);
		if (fFieldDeclaration == null) {
			return mappingErrorFound(result, node);
		}
		if (fFieldDeclaration.resolveBinding() == null) {
			if (!processCompilerError(result, node))
				result.addFatalError(RefactoringCoreMessages.SelfEncapsulateField_type_not_resolveable); 
			return result;
		}
		computeUsedNames();
		fRewriter= ASTRewrite.create(fRoot.getAST());
		return result;
	}

	private RefactoringStatus mappingErrorFound(RefactoringStatus result, ASTNode node) {
		if (node != null && (node.getFlags() & ASTNode.MALFORMED) != 0 && processCompilerError(result, node))
			return result;
		result.addFatalError(getMappingErrorMessage());
		return result;
	}

	private boolean processCompilerError(RefactoringStatus result, ASTNode node) {
		Message[] messages= ASTNodes.getMessages(node, ASTNodes.INCLUDE_ALL_PARENTS);
		if (messages.length == 0)
			return false;
		result.addFatalError(Messages.format(
			RefactoringCoreMessages.SelfEncapsulateField_compiler_errors_field,  
			new String[] { fField.getElementName(), messages[0].getMessage()}));
		return true;
	}

	private String getMappingErrorMessage() {
		return Messages.format(
			RefactoringCoreMessages.SelfEncapsulateField_cannot_analyze_selected_field, 
			new String[] {fField.getElementName()});
	}

	//---- Input checking ----------------------------------------------------------

	public RefactoringStatus checkMethodNames() {
		return checkMethodNames(isUsingLocalGetter(),isUsingLocalSetter());
	}
	
	public RefactoringStatus checkMethodNames(boolean usingLocalGetter, boolean usingLocalSetter) {
		RefactoringStatus result= new RefactoringStatus();
		IType declaringType= fField.getDeclaringType();
		checkName(result, fGetterName, fUsedReadNames, declaringType, usingLocalGetter, fField);
		checkName(result, fSetterName, fUsedModifyNames, declaringType, usingLocalSetter, fField);
		return result;
	}
	
	private static void checkName(RefactoringStatus status, String name, List usedNames, IType type, boolean reUseExistingField, IField field) {
		if ("".equals(name)) { //$NON-NLS-1$
			status.addFatalError(RefactoringCoreMessages.Checks_Choose_name); 
			return;
	    }
		boolean isStatic=false;
		try {
			isStatic= Flags.isStatic(field.getFlags());
		} catch (JavaScriptModelException e) {
		}
		status.merge(Checks.checkMethodName(name));
		for (Iterator iter= usedNames.iterator(); iter.hasNext(); ) {
			IFunctionBinding method= (IFunctionBinding)iter.next();
			String selector= method.getName();
			if (selector.equals(name)) {
				if (!reUseExistingField) {
					status.addFatalError(Messages.format(RefactoringCoreMessages.SelfEncapsulateField_method_exists, new String[] { BindingLabelProvider.getBindingLabel(method, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), type.getElementName() }));
				} else {
					boolean methodIsStatic= Modifier.isStatic(method.getModifiers());
					if (methodIsStatic && !isStatic)
						status.addWarning(Messages.format(RefactoringCoreMessages.SelfEncapsulateFieldRefactoring_static_method_but_nonstatic_field, new String[] { method.getName(), field.getElementName() }));
					if (!methodIsStatic && isStatic)
						status.addFatalError(Messages.format(RefactoringCoreMessages.SelfEncapsulateFieldRefactoring_nonstatic_method_but_static_field, new String[] { method.getName(), field.getElementName() }));
					return;
				}

			}
		}
		if (reUseExistingField)
			status.addFatalError(Messages.format(
				RefactoringCoreMessages.SelfEncapsulateFieldRefactoring_methoddoesnotexist_status_fatalError, 
				new String[] {name, type.getElementName()}));
	}	

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		fChangeManager.clear();
		pm.beginTask(NO_NAME, 12);
		pm.setTaskName(RefactoringCoreMessages.SelfEncapsulateField_checking_preconditions);
		boolean usingLocalGetter=isUsingLocalGetter();
		boolean usingLocalSetter=isUsingLocalSetter();
		result.merge(checkMethodNames(usingLocalGetter,usingLocalSetter));
		pm.worked(1);
		if (result.hasFatalError())
			return result;
		pm.setTaskName(RefactoringCoreMessages.SelfEncapsulateField_searching_for_cunits); 
		final SubProgressMonitor subPm= new SubProgressMonitor(pm, 5);
		IJavaScriptUnit[] affectedCUs= RefactoringSearchEngine.findAffectedCompilationUnits(
			SearchPattern.createPattern(fField, IJavaScriptSearchConstants.REFERENCES),
			RefactoringScopeFactory.create(fField, fConsiderVisibility),
			subPm,
			result, true);
		
		checkInHierarchy(result, usingLocalGetter, usingLocalSetter);
		if (result.hasFatalError())
			return result;
			
		pm.setTaskName(RefactoringCoreMessages.SelfEncapsulateField_analyzing);	 
		IProgressMonitor sub= new SubProgressMonitor(pm, 5);
		sub.beginTask(NO_NAME, affectedCUs.length);
		IVariableBinding fieldIdentifier= fFieldDeclaration.resolveBinding();
		ITypeBinding declaringClass= 
			((AbstractTypeDeclaration)ASTNodes.getParent(fFieldDeclaration, AbstractTypeDeclaration.class)).resolveBinding();
		List ownerDescriptions= new ArrayList();
		IJavaScriptUnit owner= fField.getJavaScriptUnit();
		fImportRewrite= StubUtility.createImportRewrite(fRoot, true);
		
		for (int i= 0; i < affectedCUs.length; i++) {
			IJavaScriptUnit unit= affectedCUs[i];
			sub.subTask(unit.getElementName());
			JavaScriptUnit root= null;
			ASTRewrite rewriter= null;
			ImportRewrite importRewrite;
			List descriptions;
			if (owner.equals(unit)) {
				root= fRoot;
				rewriter= fRewriter;
				importRewrite= fImportRewrite;
				descriptions= ownerDescriptions;
			} else {
				root= new RefactoringASTParser(AST.JLS3).parse(unit, true);
				rewriter= ASTRewrite.create(root.getAST());
				descriptions= new ArrayList();
				importRewrite= StubUtility.createImportRewrite(root, true);
			}
			checkCompileErrors(result, root, unit);
			AccessAnalyzer analyzer= new AccessAnalyzer(this, unit, fieldIdentifier, declaringClass, rewriter, importRewrite);
			root.accept(analyzer);
			result.merge(analyzer.getStatus());
			if (!fSetterMustReturnValue) 
				fSetterMustReturnValue= analyzer.getSetterMustReturnValue();
			if (result.hasFatalError()) {
				fChangeManager.clear();
				return result;
			}
			descriptions.addAll(analyzer.getGroupDescriptions());
			if (!owner.equals(unit))
				createEdits(unit, rewriter, descriptions, importRewrite);
			sub.worked(1);
			if (pm.isCanceled())
				throw new OperationCanceledException();
		}
		ownerDescriptions.addAll(addGetterSetterChanges(fRoot, fRewriter, owner.findRecommendedLineSeparator(),usingLocalSetter, usingLocalGetter));
		createEdits(owner, fRewriter, ownerDescriptions, fImportRewrite);

		sub.done();
		IFile[] filesToBeModified= ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
		result.merge(Checks.validateModifiesFiles(filesToBeModified, getValidationContext()));
		if (result.hasFatalError())
			return result;
		ResourceChangeChecker.checkFilesToBeChanged(filesToBeModified, new SubProgressMonitor(pm, 1));
		return result;
	}

	private void createEdits(IJavaScriptUnit unit, ASTRewrite rewriter, List groups, ImportRewrite importRewrite) throws CoreException {
		TextChange change= fChangeManager.get(unit);
		MultiTextEdit root= new MultiTextEdit();
		change.setEdit(root);
		root.addChild(importRewrite.rewriteImports(null));
		root.addChild(rewriter.rewriteAST());
		for (Iterator iter= groups.iterator(); iter.hasNext();) {
			change.addTextEditGroup((TextEditGroup)iter.next());
		}
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		final Map arguments= new HashMap();
		String project= null;
		IJavaScriptProject javaProject= fField.getJavaScriptProject();
		if (javaProject != null)
			project= javaProject.getElementName();
		int flags= JavaScriptRefactoringDescriptor.JAR_MIGRATION | JavaScriptRefactoringDescriptor.JAR_REFACTORING | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
		final IType declaring= fField.getDeclaringType();
		try {
			if (declaring.isAnonymous() || declaring.isLocal())
				flags|= JavaScriptRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
		} catch (JavaScriptModelException exception) {
			JavaScriptPlugin.log(exception);
		}
		final String description= Messages.format(RefactoringCoreMessages.SelfEncapsulateField_descriptor_description_short, fField.getElementName());
		final String header= Messages.format(RefactoringCoreMessages.SelfEncapsulateFieldRefactoring_descriptor_description, new String[] { JavaScriptElementLabels.getElementLabel(fField, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), JavaScriptElementLabels.getElementLabel(declaring, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)});
		final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
		comment.addSetting(Messages.format(RefactoringCoreMessages.SelfEncapsulateField_original_pattern, JavaScriptElementLabels.getElementLabel(fField, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)));
		comment.addSetting(Messages.format(RefactoringCoreMessages.SelfEncapsulateField_getter_pattern, fGetterName));
		comment.addSetting(Messages.format(RefactoringCoreMessages.SelfEncapsulateField_setter_pattern, fSetterName));
		String visibility= JdtFlags.getVisibilityString(fVisibility);
		if ("".equals(visibility)) //$NON-NLS-1$
			visibility= RefactoringCoreMessages.SelfEncapsulateField_default_visibility;
		comment.addSetting(Messages.format(RefactoringCoreMessages.SelfEncapsulateField_visibility_pattern, visibility));
		if (fEncapsulateDeclaringClass)
			comment.addSetting(RefactoringCoreMessages.SelfEncapsulateField_use_accessors);
		else
			comment.addSetting(RefactoringCoreMessages.SelfEncapsulateField_do_not_use_accessors);			
		if (fGenerateJavadoc)
			comment.addSetting(RefactoringCoreMessages.SelfEncapsulateField_generate_comments);
		final JDTRefactoringDescriptor descriptor= new JDTRefactoringDescriptor(IJavaScriptRefactorings.ENCAPSULATE_FIELD, project, description, comment.asString(), arguments, flags);
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fField));
		arguments.put(ATTRIBUTE_VISIBILITY, Integer.valueOf(fVisibility).toString());
		arguments.put(ATTRIBUTE_INSERTION, Integer.valueOf(fInsertionIndex).toString());
		arguments.put(ATTRIBUTE_SETTER, fSetterName);
		arguments.put(ATTRIBUTE_GETTER, fGetterName);
		arguments.put(ATTRIBUTE_COMMENTS, Boolean.valueOf(fGenerateJavadoc).toString());
		arguments.put(ATTRIBUTE_DECLARING, Boolean.valueOf(fEncapsulateDeclaringClass).toString());
		final DynamicValidationRefactoringChange result= new DynamicValidationRefactoringChange(descriptor, getName());
		TextChange[] changes= fChangeManager.getAllChanges();
		pm.beginTask(NO_NAME, changes.length);
		pm.setTaskName(RefactoringCoreMessages.SelfEncapsulateField_create_changes);
		for (int i= 0; i < changes.length; i++) {
			result.add(changes[i]);
			pm.worked(1);
		}
		pm.done();
		return result;
	}

	public String getName() {
		return RefactoringCoreMessages.SelfEncapsulateField_name; 
	}
	
	//---- Helper methods -------------------------------------------------------------
	
	private void checkCompileErrors(RefactoringStatus result, JavaScriptUnit root, IJavaScriptUnit element) {
		IProblem[] messages= root.getProblems();
		for (int i= 0; i < messages.length; i++) {
			IProblem problem= messages[i];
			if (!isIgnorableProblem(problem)) {
				result.addError(Messages.format(
						RefactoringCoreMessages.SelfEncapsulateField_compiler_errors_update, 
						element.getElementName()), JavaStatusContext.create(element));
				return;
			}
		}
	}
	
	private void checkInHierarchy(RefactoringStatus status, boolean usingLocalGetter, boolean usingLocalSetter) {
		AbstractTypeDeclaration declaration= (AbstractTypeDeclaration)ASTNodes.getParent(fFieldDeclaration, AbstractTypeDeclaration.class);
		ITypeBinding type= declaration.resolveBinding();
		if (type != null) {
			ITypeBinding fieldType= fFieldDeclaration.resolveBinding().getType();
			checkMethodInHierarchy(type, fGetterName, fieldType, new ITypeBinding[0], status, usingLocalGetter);
			checkMethodInHierarchy(type, fSetterName, fFieldDeclaration.getAST().resolveWellKnownType("void"), //$NON-NLS-1$
				new ITypeBinding[] {fieldType}, status, usingLocalSetter);
		}
	}
	
	public static void checkMethodInHierarchy(ITypeBinding type, String methodName, ITypeBinding returnType, ITypeBinding[] parameters, RefactoringStatus result, boolean reUseMethod) {
		IFunctionBinding method= Bindings.findMethodInHierarchy(type, methodName, parameters);
		if (method != null) {
			boolean returnTypeClash= false;
			ITypeBinding methodReturnType= method.getReturnType();
			if (returnType != null && methodReturnType != null) {
				String returnTypeKey= returnType.getKey();
				String methodReturnTypeKey= methodReturnType.getKey();
				if (returnTypeKey == null && methodReturnTypeKey == null) {
					returnTypeClash= returnType != methodReturnType;	
				} else if (returnTypeKey != null && methodReturnTypeKey != null) {
					returnTypeClash= !returnTypeKey.equals(methodReturnTypeKey);
				}
			}
			ITypeBinding dc= method.getDeclaringClass();
			if (returnTypeClash) {
				result.addError(Messages.format(RefactoringCoreMessages.Checks_methodName_returnTypeClash, 
					new Object[] {methodName, dc.getName()}),
					JavaStatusContext.create(method));
			} else {
				if (!reUseMethod)
					result.addError(Messages.format(RefactoringCoreMessages.Checks_methodName_overrides, 
						new Object[] {methodName, dc.getName()}),
						JavaStatusContext.create(method));
			}
		} else {
			if (reUseMethod){
				result.addError(Messages.format(RefactoringCoreMessages.SelfEncapsulateFieldRefactoring_nosuchmethod_status_fatalError, 
						new Object[] {methodName}),
						JavaStatusContext.create(method));
			}
		}
	}
	
	private void computeUsedNames() {
		fUsedReadNames= new ArrayList(0);
		fUsedModifyNames= new ArrayList(0);
		IVariableBinding binding= fFieldDeclaration.resolveBinding();
		ITypeBinding type= binding.getType();
		IFunctionBinding[] methods= binding.getDeclaringClass().getDeclaredMethods();
		for (int i= 0; i < methods.length; i++) {
			IFunctionBinding method= methods[i];
			ITypeBinding[] parameters= methods[i].getParameterTypes();
			if (parameters == null || parameters.length == 0) {
				fUsedReadNames.add(method);
			} else if (parameters.length == 1 && parameters[0] == type) {
				fUsedModifyNames.add(method);
			} 
		}
	}

	private List addGetterSetterChanges(JavaScriptUnit root, ASTRewrite rewriter, String lineDelimiter, boolean usingLocalSetter, boolean usingLocalGetter) throws CoreException {
		List result= new ArrayList(2);
		AST ast= root.getAST();
		FieldDeclaration decl= (FieldDeclaration)ASTNodes.getParent(fFieldDeclaration, ASTNode.FIELD_DECLARATION);
		int position= 0;
		int numberOfMethods= 0;
		List members= ASTNodes.getBodyDeclarations(decl.getParent());
		for (Iterator iter= members.iterator(); iter.hasNext();) {
			BodyDeclaration element= (BodyDeclaration)iter.next();
			if (element.getNodeType() == ASTNode.FUNCTION_DECLARATION) {
				if (fInsertionIndex == -1) {
					break;
				} else if (fInsertionIndex == numberOfMethods) {
					position++;
					break;
				}
				numberOfMethods++;	
			}
			position++;
		}
		TextEditGroup description;
		ListRewrite rewrite= fRewriter.getListRewrite(decl.getParent(), getBodyDeclarationsProperty(decl.getParent()));
		if (!JdtFlags.isFinal(fField) && !usingLocalSetter) {
			description= new TextEditGroup(RefactoringCoreMessages.SelfEncapsulateField_add_setter); 
			result.add(description);
			rewrite.insertAt(createSetterMethod(ast, rewriter, lineDelimiter), position++, description);
		}
		if (!usingLocalGetter){
			description= new TextEditGroup(RefactoringCoreMessages.SelfEncapsulateField_add_getter); 
			result.add(description);
			rewrite.insertAt(createGetterMethod(ast, rewriter, lineDelimiter), position, description);
		}
		if (!JdtFlags.isPrivate(fField))
			result.add(makeDeclarationPrivate(rewriter, decl));
		return result;
	}

	private TextEditGroup makeDeclarationPrivate(ASTRewrite rewriter, FieldDeclaration decl) {
		AST ast= rewriter.getAST();
		TextEditGroup description= new TextEditGroup(RefactoringCoreMessages.SelfEncapsulateField_change_visibility); 
		if (decl.fragments().size() > 1) {
			//TODO: doesn't work for cases like this:  int field1, field2= field1, field3= field2; // keeping refs to field
			rewriter.remove(fFieldDeclaration, description);
			ChildListPropertyDescriptor descriptor= getBodyDeclarationsProperty(decl.getParent());
			VariableDeclarationFragment newField= (VariableDeclarationFragment) rewriter.createCopyTarget(fFieldDeclaration);
			FieldDeclaration fieldDecl= ast.newFieldDeclaration(newField);
			fieldDecl.setType((Type)rewriter.createCopyTarget(decl.getType()));
			fieldDecl.modifiers().addAll(ASTNodeFactory.newModifiers(ast, Modifier.PRIVATE));
			rewriter.getListRewrite(decl.getParent(), descriptor).insertAfter(fieldDecl, decl, description);
		} else {
			ModifierRewrite.create(rewriter, decl).setVisibility(Modifier.PRIVATE, description);
		}
		return description;
	}

	private ChildListPropertyDescriptor getBodyDeclarationsProperty(ASTNode declaration) {
		if (declaration instanceof AnonymousClassDeclaration)
			return AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY;
		else if (declaration instanceof AbstractTypeDeclaration)
			return ((AbstractTypeDeclaration) declaration).getBodyDeclarationsProperty();
		Assert.isTrue(false);
		return null;
	}

	private FunctionDeclaration createSetterMethod(AST ast, ASTRewrite rewriter, String lineDelimiter) throws CoreException {
		FieldDeclaration field= (FieldDeclaration)ASTNodes.getParent(fFieldDeclaration, FieldDeclaration.class);
		Type type= field.getType();
		FunctionDeclaration result= ast.newFunctionDeclaration();
		result.setName(ast.newSimpleName(fSetterName));
		result.modifiers().addAll(ASTNodeFactory.newModifiers(ast, createModifiers()));
		if (fSetterMustReturnValue) {
			result.setReturnType2((Type)rewriter.createCopyTarget(type));
		}
		SingleVariableDeclaration param= ast.newSingleVariableDeclaration();
		result.parameters().add(param);
		param.setName(ast.newSimpleName(fArgName));
		param.setType((Type)rewriter.createCopyTarget(type));
		
		Block block= ast.newBlock();
		result.setBody(block);
		Assignment ass= ast.newAssignment();
		ass.setLeftHandSide(createFieldAccess(ast));
		ass.setRightHandSide(ast.newSimpleName(fArgName));
		if (fSetterMustReturnValue) {
			ReturnStatement rs= ast.newReturnStatement();
			rs.setExpression(ass);
			block.statements().add(rs);
		} else {
			block.statements().add(ast.newExpressionStatement(ass));
		}
		
		if (fGenerateJavadoc) {
			String string= CodeGeneration.getSetterComment(
				fField.getJavaScriptUnit() , getTypeName(field.getParent()), fSetterName, 
				fField.getElementName(), ASTNodes.asString(type), fArgName, 
				NamingConventions.removePrefixAndSuffixForFieldName(fField.getJavaScriptProject(), fField.getElementName(), fField.getFlags()),
				lineDelimiter);
			if (string != null) {
				JSdoc javadoc= (JSdoc)fRewriter.createStringPlaceholder(string, ASTNode.JSDOC);
				result.setJavadoc(javadoc);
			}
		}
		return result;
	}
	
	private FunctionDeclaration createGetterMethod(AST ast, ASTRewrite rewriter, String lineDelimiter) throws CoreException {
		FieldDeclaration field= (FieldDeclaration)ASTNodes.getParent(fFieldDeclaration, FieldDeclaration.class);
		Type type= field.getType();
		FunctionDeclaration result= ast.newFunctionDeclaration();
		result.setName(ast.newSimpleName(fGetterName));
		result.modifiers().addAll(ASTNodeFactory.newModifiers(ast, createModifiers()));
		result.setReturnType2((Type)rewriter.createCopyTarget(type));
		
		Block block= ast.newBlock();
		result.setBody(block);
		ReturnStatement rs= ast.newReturnStatement();
		rs.setExpression(ast.newSimpleName(fField.getElementName()));
		block.statements().add(rs);
		if (fGenerateJavadoc) {
			String string= CodeGeneration.getGetterComment(
				fField.getJavaScriptUnit() , getTypeName(field.getParent()), fGetterName,
				fField.getElementName(), ASTNodes.asString(type), 
				NamingConventions.removePrefixAndSuffixForFieldName(fField.getJavaScriptProject(), fField.getElementName(), fField.getFlags()),
				lineDelimiter);
			if (string != null) {
				JSdoc javadoc= (JSdoc)fRewriter.createStringPlaceholder(string, ASTNode.JSDOC);
				result.setJavadoc(javadoc);
			}
		}
		return result;
	}

	private int createModifiers() throws JavaScriptModelException {
		int result= 0;
		if (Flags.isPublic(fVisibility)) 
			result |= Modifier.PUBLIC;
		else if (Flags.isPrivate(fVisibility))
			result |= Modifier.PRIVATE;
		if (JdtFlags.isStatic(fField)) 
			result |= Modifier.STATIC; 
		return result;
	}
	
	private Expression createFieldAccess(AST ast) throws JavaScriptModelException {
		String fieldName= fField.getElementName();
		if (fArgName.equals(fieldName)) {
			if (JdtFlags.isStatic(fField)) {
				return ast.newQualifiedName(
					ast.newSimpleName(fField.getDeclaringType().getElementName()), 
					ast.newSimpleName(fieldName));
			} else {
				FieldAccess result= ast.newFieldAccess();
				result.setExpression(ast.newThisExpression());
				result.setName(ast.newSimpleName(fieldName));
				return result;
			}
		} else {
			return ast.newSimpleName(fieldName);
		}
	}
	
	private void checkArgName() {
		String fieldName= fField.getElementName();
		boolean isStatic= true;
		try {
			isStatic= JdtFlags.isStatic(fField);
		} catch(JavaScriptModelException e) {
		}
		IJavaScriptProject project= fField.getJavaScriptProject();
		String sourceLevel= project.getOption(JavaScriptCore.COMPILER_SOURCE, true);
		String compliance= project.getOption(JavaScriptCore.COMPILER_COMPLIANCE, true);
		
		if ((isStatic && fArgName.equals(fieldName) && fieldName.equals(fField.getDeclaringType().getElementName()))
			|| JavaScriptConventions.validateIdentifier(fArgName, sourceLevel, compliance).getSeverity() == IStatus.ERROR)
			fArgName= "_" + fArgName; //$NON-NLS-1$
	}
	
	private String getTypeName(ASTNode type) {
		if (type instanceof AbstractTypeDeclaration) {
			return ((AbstractTypeDeclaration)type).getName().getIdentifier();
		} else if (type instanceof AnonymousClassDeclaration) {
			ClassInstanceCreation node= (ClassInstanceCreation)ASTNodes.getParent(type, ClassInstanceCreation.class);
			return ASTNodes.asString(node.getType());
		}
		Assert.isTrue(false, "Should not happen"); //$NON-NLS-1$
		return null;
	}

	public RefactoringStatus initialize(RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			final String handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists() || element.getElementType() != IJavaScriptElement.FIELD)
					return createInputFatalStatus(element, IJavaScriptRefactorings.ENCAPSULATE_FIELD);
				else {
					fField= (IField) element;
					try {
						initialize(fField);
					} catch (JavaScriptModelException exception) {
						return createInputFatalStatus(element, IJavaScriptRefactorings.ENCAPSULATE_FIELD);
					}
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
			String name= extended.getAttribute(ATTRIBUTE_GETTER);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				fGetterName= name;
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_GETTER));
			name= extended.getAttribute(ATTRIBUTE_SETTER);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				fSetterName= name;
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_SETTER));
			final String encapsulate= extended.getAttribute(ATTRIBUTE_DECLARING);
			if (encapsulate != null) {
				fEncapsulateDeclaringClass= Boolean.valueOf(encapsulate).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DECLARING));
			final String matches= extended.getAttribute(ATTRIBUTE_COMMENTS);
			if (matches != null) {
				fGenerateJavadoc= Boolean.valueOf(matches).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_COMMENTS));
			final String visibility= extended.getAttribute(ATTRIBUTE_VISIBILITY);
			if (visibility != null && !"".equals(visibility)) {//$NON-NLS-1$
				int flag= 0;
				try {
					flag= Integer.parseInt(visibility);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_VISIBILITY));
				}
				fVisibility= flag;
			}
			final String insertion= extended.getAttribute(ATTRIBUTE_INSERTION);
			if (insertion != null && !"".equals(insertion)) {//$NON-NLS-1$
				int index= 0;
				try {
					index= Integer.parseInt(insertion);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_INSERTION));
				}
				fInsertionIndex= index;
			}
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}

	public boolean isUsingLocalGetter() {
		IType declaringType= fField.getDeclaringType();
		return checkName(fGetterName, fUsedReadNames, declaringType);
	}

	public boolean isUsingLocalSetter() {
		IType declaringType= fField.getDeclaringType();
		return checkName(fSetterName, fUsedModifyNames, declaringType);
	}
	
	private static boolean checkName(String name, List usedNames, IType type) {
		for (Iterator iter= usedNames.iterator(); iter.hasNext(); ) {
			IFunctionBinding method= (IFunctionBinding)iter.next();
			String selector= method.getName();
			if (selector.equals(name)) {
				return true;
			}
		}
		return false;
	}	
	
	private boolean isIgnorableProblem(IProblem problem) {
		if (problem.getID() == IProblem.NotVisibleField)
			return true;
		return false;
	}

	public boolean isConsiderVisibility() {
		return fConsiderVisibility;
	}

	public void setConsiderVisibility(boolean considerVisibility) {
		fConsiderVisibility= considerVisibility;
	}


}
