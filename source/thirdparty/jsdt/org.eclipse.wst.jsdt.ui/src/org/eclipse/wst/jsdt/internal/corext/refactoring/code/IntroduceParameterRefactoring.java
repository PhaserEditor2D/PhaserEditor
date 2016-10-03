/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Felix Pahl (fpahl@web.de) - contributed fix for:
 *       o introduce parameter throws NPE if there are compiler errors
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=48325)
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.corext.refactoring.code;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ArrayInitializer;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.NullLiteral;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.internal.corext.Corext;
import org.eclipse.wst.jsdt.internal.corext.SourceRange;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.dom.fragments.ASTFragmentFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.fragments.IASTFragment;
import org.eclipse.wst.jsdt.internal.corext.dom.fragments.IExpressionFragment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.RefactoringDescriptorChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.BodyUpdater;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ChangeSignatureRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IDelegateUpdating;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

public class IntroduceParameterRefactoring extends ScriptableRefactoring implements IDelegateUpdating {

	private static final String ATTRIBUTE_ARGUMENT= "argument"; //$NON-NLS-1$

	private static final String[] KNOWN_METHOD_NAME_PREFIXES= {"get", "is"}; //$NON-NLS-2$ //$NON-NLS-1$

	private IJavaScriptUnit fSourceCU;
	private int fSelectionStart;
	private int fSelectionLength;
	
	private IFunction fMethod;
	private ChangeSignatureRefactoring fChangeSignatureRefactoring;
	private ParameterInfo fParameter;
	private String fParameterName;
	private RefactoringArguments fArguments;

	private Expression fSelectedExpression;
	private String[] fExcludedParameterNames;
	
	/**
	 * Creates a new introduce parameter refactoring.
	 * @param unit the compilation unit, or <code>null</code> if invoked by scripting
	 * @param selectionStart
	 * @param selectionLength
	 */
	public IntroduceParameterRefactoring(IJavaScriptUnit unit, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		fSourceCU= unit;
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
	}
	
	// ------------------- IDelegateUpdating ----------------------

	public boolean canEnableDelegateUpdating() {
		return true;
	}

	public boolean getDelegateUpdating() {
		return (fChangeSignatureRefactoring != null) ? fChangeSignatureRefactoring.getDelegateUpdating() : false;
	}

	public void setDelegateUpdating(boolean updating) {
		if (fChangeSignatureRefactoring != null)
			fChangeSignatureRefactoring.setDelegateUpdating(updating);
	}

	public void setDeprecateDelegates(boolean deprecate) {
		if (fChangeSignatureRefactoring != null)
			fChangeSignatureRefactoring.setDeprecateDelegates(deprecate);
	}

	public boolean getDeprecateDelegates() {
		return (fChangeSignatureRefactoring != null) ? fChangeSignatureRefactoring.getDeprecateDelegates() : false;
	}

	// ------------------- /IDelegateUpdating ---------------------

	public String getName() {
		return RefactoringCoreMessages.IntroduceParameterRefactoring_name; 
	}

	//--- checkActivation
		
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 7); //$NON-NLS-1$
			
			if (! fSourceCU.isStructureKnown())		
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceParameterRefactoring_syntax_error); 
			
			IJavaScriptElement enclosingElement= SelectionConverter.resolveEnclosingElement(fSourceCU, new TextSelection(fSelectionStart, fSelectionLength));
			if (! (enclosingElement instanceof IFunction))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceParameterRefactoring_expression_in_method); 
			
			fMethod= (IFunction) enclosingElement;
			pm.worked(1);

			RefactoringStatus result= new RefactoringStatus();
			if (fArguments != null) {
				// invoked by script
				fChangeSignatureRefactoring= new ChangeSignatureRefactoring(null);
				result= fChangeSignatureRefactoring.initialize(fArguments);
				if (!result.hasFatalError()) {
					fChangeSignatureRefactoring.setValidationContext(getValidationContext());
					result.merge(fChangeSignatureRefactoring.checkInitialConditions(new SubProgressMonitor(pm, 2)));
					if (result.hasFatalError())
						return result;
				} else {
					pm.worked(2);
					return result;
				}
			} else {
				// first try:
				fChangeSignatureRefactoring= RefactoringAvailabilityTester.isChangeSignatureAvailable(fMethod) ? new ChangeSignatureRefactoring(fMethod) : null;
				if (fChangeSignatureRefactoring == null)
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceParameterRefactoring_expression_in_method);
				fChangeSignatureRefactoring.setValidationContext(getValidationContext());
				result.merge(fChangeSignatureRefactoring.checkInitialConditions(new SubProgressMonitor(pm, 1)));
				if (result.hasFatalError()) {
					RefactoringStatusEntry entry= result.getEntryMatchingSeverity(RefactoringStatus.FATAL);
					if (entry.getCode() == RefactoringStatusCodes.OVERRIDES_ANOTHER_METHOD || entry.getCode() == RefactoringStatusCodes.METHOD_DECLARED_IN_INTERFACE) {
						// second try:
						IFunction method= (IFunction) entry.getData();
						fChangeSignatureRefactoring= RefactoringAvailabilityTester.isChangeSignatureAvailable(method) ? new ChangeSignatureRefactoring(method) : null;
						if (fChangeSignatureRefactoring == null) {
							String msg= Messages.format(RefactoringCoreMessages.IntroduceParameterRefactoring_cannot_introduce, entry.getMessage());
							return RefactoringStatus.createFatalErrorStatus(msg);
						}
						result= fChangeSignatureRefactoring.checkInitialConditions(new SubProgressMonitor(pm, 1));
						if (result.hasFatalError())
							return result;
					} else {
						return result;
					}
				} else {
					pm.worked(1);
				}
			}

			CompilationUnitRewrite cuRewrite= fChangeSignatureRefactoring.getBaseCuRewrite();
			if (! cuRewrite.getCu().equals(fSourceCU))
				cuRewrite= new CompilationUnitRewrite(fSourceCU); // TODO: should try to avoid throwing away this AST
			
			initializeSelectedExpression(cuRewrite);
			pm.worked(1);
		
			result.merge(checkSelection(cuRewrite, new SubProgressMonitor(pm, 3)));
			if (result.hasFatalError())
				return result;

			initializeExcludedParameterNames(cuRewrite);
			
			addParameterInfo(cuRewrite);
			
			fChangeSignatureRefactoring.setBodyUpdater(new BodyUpdater() {
				public void updateBody(FunctionDeclaration methodDeclaration, CompilationUnitRewrite rewrite, RefactoringStatus updaterResult) {
					replaceSelectedExpression(rewrite);
				}
			});
			
			return result;
		} finally {
			pm.done();
			if (fChangeSignatureRefactoring != null)
				fChangeSignatureRefactoring.setValidationContext(null);
		}	
	}

	private void addParameterInfo(CompilationUnitRewrite cuRewrite) throws JavaScriptModelException {
		ITypeBinding typeBinding= Bindings.normalizeForDeclarationUse(fSelectedExpression.resolveTypeBinding(), fSelectedExpression.getAST());
		String typeName= cuRewrite.getImportRewrite().addImport(typeBinding);
		String name= fParameterName != null ? fParameterName : guessedParameterName();
		String defaultValue= fSourceCU.getBuffer().getText(fSelectedExpression.getStartPosition(), fSelectedExpression.getLength());
		fParameter= ParameterInfo.createInfoForAddedParameter(typeBinding, typeName, name, defaultValue);
		if (fArguments == null) {
			List parameterInfos= fChangeSignatureRefactoring.getParameterInfos();
			int parametersCount= parameterInfos.size();
			if (parametersCount > 0 && ((ParameterInfo) parameterInfos.get(parametersCount - 1)).isOldVarargs())
				parameterInfos.add(parametersCount - 1, fParameter);
			else
				parameterInfos.add(fParameter);
		}
	}

	private void replaceSelectedExpression(CompilationUnitRewrite cuRewrite) {
		if (! fSourceCU.equals(cuRewrite.getCu()))
			return;
		// TODO: do for all methodDeclarations and replace matching fragments?
		
		// cannot use fSelectedExpression here, since it could be from another AST (if method was replaced by overridden):
		Expression expression= (Expression) NodeFinder.perform(cuRewrite.getRoot(), fSelectedExpression.getStartPosition(), fSelectedExpression.getLength());
		
		ASTNode newExpression= cuRewrite.getRoot().getAST().newSimpleName(fParameter.getNewName());
		String description= RefactoringCoreMessages.IntroduceParameterRefactoring_replace; 
		cuRewrite.getASTRewrite().replace(expression, newExpression, cuRewrite.createGroupDescription(description));
	}

	private void initializeSelectedExpression(CompilationUnitRewrite cuRewrite) throws JavaScriptModelException {
		IASTFragment fragment= ASTFragmentFactory.createFragmentForSourceRange(
				new SourceRange(fSelectionStart, fSelectionLength), cuRewrite.getRoot(), cuRewrite.getCu());
		
		if (! (fragment instanceof IExpressionFragment))
			return;
		
		//TODO: doesn't handle selection of partial Expressions
		Expression expression= ((IExpressionFragment) fragment).getAssociatedExpression();
		if (fragment.getStartPosition() != expression.getStartPosition()
				|| fragment.getLength() != expression.getLength())
			return;
		
		if (Checks.isInsideJavadoc(expression))
			return;
		
		fSelectedExpression= expression;
	}
	
	private RefactoringStatus checkSelection(CompilationUnitRewrite cuRewrite, IProgressMonitor pm) {
		if (fSelectedExpression == null){
			String message= RefactoringCoreMessages.IntroduceParameterRefactoring_select;
			return CodeRefactoringUtil.checkMethodSyntaxErrors(fSelectionStart, fSelectionLength, cuRewrite.getRoot(), message);
		}	
		
		FunctionDeclaration methodDeclaration= (FunctionDeclaration) ASTNodes.getParent(fSelectedExpression, FunctionDeclaration.class);
		if (methodDeclaration == null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceParameterRefactoring_expression_in_method); 
		if (methodDeclaration.resolveBinding() == null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceParameterRefactoring_no_binding); 
		//TODO: check for rippleMethods -> find matching fragments, consider callers of all rippleMethods
		
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkExpression());
		if (result.hasFatalError())
			return result;
		
		result.merge(checkExpressionBinding());
		if (result.hasFatalError())
			return result;				
		
//			if (isUsedInForInitializerOrUpdater(getSelectedExpression().getAssociatedExpression()))
//				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.for_initializer_updater")); //$NON-NLS-1$
//			pm.worked(1);				
//
//			if (isReferringToLocalVariableFromFor(getSelectedExpression().getAssociatedExpression()))
//				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractTempRefactoring.refers_to_for_variable")); //$NON-NLS-1$
//			pm.worked(1);
		
		return result;		
	}

	private RefactoringStatus checkExpression() {
		//TODO: adjust error messages (or generalize for all refactorings on expression-selections?)
		Expression selectedExpression= fSelectedExpression;
		
		if (selectedExpression instanceof Name && selectedExpression.getParent() instanceof ClassInstanceCreation)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_name_in_new); 
			//TODO: let's just take the CIC automatically (no ambiguity -> no problem -> no dialog ;-)
		
		if (selectedExpression instanceof NullLiteral) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_null_literals); 
		} else if (selectedExpression instanceof ArrayInitializer) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_array_initializer); 
		} else if (selectedExpression instanceof Assignment) {
			if (selectedExpression.getParent() instanceof Expression)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_assignment); 
			else
				return null;

		} else if (selectedExpression instanceof SimpleName){
			if ((((SimpleName)selectedExpression)).isDeclaration())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_names_in_declarations); 
			if (selectedExpression.getParent() instanceof QualifiedName && selectedExpression.getLocationInParent() == QualifiedName.NAME_PROPERTY
					|| selectedExpression.getParent() instanceof FieldAccess && selectedExpression.getLocationInParent() == FieldAccess.NAME_PROPERTY)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_select_expression);
		} 
		
		return null;
	}

	private RefactoringStatus checkExpressionBinding() {
		return checkExpressionFragmentIsRValue();
	}
	
	// !! +/- same as in ExtractConstantRefactoring & ExtractTempRefactoring
	private RefactoringStatus checkExpressionFragmentIsRValue() {
		switch(Checks.checkExpressionIsRValue(fSelectedExpression)) {
			case Checks.NOT_RVALUE_MISC:
				return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.IntroduceParameterRefactoring_select, null, Corext.getPluginId(), RefactoringStatusCodes.EXPRESSION_NOT_RVALUE, null); 
			case Checks.NOT_RVALUE_VOID:
				return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.IntroduceParameterRefactoring_no_void, null, Corext.getPluginId(), RefactoringStatusCodes.EXPRESSION_NOT_RVALUE_VOID, null); 
			case Checks.IS_RVALUE:
				return new RefactoringStatus();
			default:
				Assert.isTrue(false); return null;
		}		
	}	

	public List getParameterInfos() {
		return fChangeSignatureRefactoring.getParameterInfos();
	}
	
	public ParameterInfo getAddedParameterInfo() {
		return fParameter;
	}
	
	public String getMethodSignaturePreview() throws JavaScriptModelException {
		return fChangeSignatureRefactoring.getNewMethodSignature();
	}
	
//--- Input setting/validation

	public void setParameterName(String name) {
		Assert.isNotNull(name);
		fParameter.setNewName(name);
	}
	
	/** 
	 * must only be called <i>after</i> checkActivation() 
	 * @return guessed parameter name
	 */
	public String guessedParameterName() {
		String[] proposals= guessParameterNames();
		if (proposals.length == 0)
			return ""; //$NON-NLS-1$
		else
			return proposals[0];
	}
	
// --- TODO: copied from ExtractTempRefactoring - should extract ------------------------------
	
	/**
	 * Must only be called <i>after</i> checkActivation().
	 * The first proposal should be used as "best guess" (if it exists).
	 * @return proposed variable names (may be empty, but not null).
	 */
	public String[] guessParameterNames() {
		LinkedHashSet proposals= new LinkedHashSet(); //retain ordering, but prevent duplicates
		if (fSelectedExpression instanceof FunctionInvocation){
			proposals.addAll(guessTempNamesFromMethodInvocation((FunctionInvocation) fSelectedExpression, fExcludedParameterNames));
		}
		proposals.addAll(guessTempNamesFromExpression(fSelectedExpression, fExcludedParameterNames));
		return (String[]) proposals.toArray(new String[proposals.size()]);
	}
	
	private List/*<String>*/ guessTempNamesFromMethodInvocation(FunctionInvocation selectedMethodInvocation, String[] excludedVariableNames) {
		SimpleName name = selectedMethodInvocation.getName();
		
		String methodName;
		if (name!=null)
		{
			methodName= name.getIdentifier();
		for (int i= 0; i < KNOWN_METHOD_NAME_PREFIXES.length; i++) {
			String prefix= KNOWN_METHOD_NAME_PREFIXES[i];
			if (! methodName.startsWith(prefix))
				continue; //not this prefix
			if (methodName.length() == prefix.length())
				return Collections.EMPTY_LIST; // prefix alone -> don't take method name
			char firstAfterPrefix= methodName.charAt(prefix.length());
			if (! Character.isUpperCase(firstAfterPrefix))
				continue; //not uppercase after prefix
			//found matching prefix
			String proposal= Character.toLowerCase(firstAfterPrefix) + methodName.substring(prefix.length() + 1);
			methodName= proposal;
			break;
		}
		}
		else
			methodName="indirectFunctionCall"; //$NON-NLS-1$
		String[] proposals= StubUtility.getLocalNameSuggestions(fSourceCU.getJavaScriptProject(), methodName, 0, excludedVariableNames);
		return Arrays.asList(proposals);
	}
	
	private List/*<String>*/ guessTempNamesFromExpression(Expression selectedExpression, String[] excluded) {
		ITypeBinding expressionBinding= Bindings.normalizeForDeclarationUse(
			selectedExpression.resolveTypeBinding(),
			selectedExpression.getAST());
		String typeName= getQualifiedName(expressionBinding);
		if (typeName.length() == 0)
			typeName= expressionBinding.getName();
		if (typeName.length() == 0)			
			return Collections.EMPTY_LIST;
		int typeParamStart= typeName.indexOf("<"); //$NON-NLS-1$
		if (typeParamStart != -1)
			typeName= typeName.substring(0, typeParamStart);
		String[] proposals= StubUtility.getLocalNameSuggestions(fSourceCU.getJavaScriptProject(), typeName, expressionBinding.getDimensions(), excluded);
		return Arrays.asList(proposals);
	}
	
// ----------------------------------------------------------------------
	
	private static String getQualifiedName(ITypeBinding typeBinding) {
		if (typeBinding.isAnonymous())
			return getQualifiedName(typeBinding.getSuperclass());
		if (! typeBinding.isArray())
			return typeBinding.getQualifiedName();
		else
			return typeBinding.getElementType().getQualifiedName();
	}

	private void initializeExcludedParameterNames(CompilationUnitRewrite cuRewrite) {
		IBinding[] bindings= new ScopeAnalyzer(cuRewrite.getRoot()).getDeclarationsInScope(
				fSelectedExpression.getStartPosition(), ScopeAnalyzer.VARIABLES);
		fExcludedParameterNames= new String[bindings.length];
		for (int i= 0; i < fExcludedParameterNames.length; i++) {
			fExcludedParameterNames[i]= bindings[i].getName();
		}
	}
	
	public RefactoringStatus validateInput() {
		return fChangeSignatureRefactoring.checkSignature();
	}
	
//--- checkInput
	
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		fChangeSignatureRefactoring.setValidationContext(getValidationContext());
		RefactoringStatus result;
		try {
			result= fChangeSignatureRefactoring.checkFinalConditions(pm);
		} finally {
			fChangeSignatureRefactoring.setValidationContext(null);
		}
		return result;
	}
	
	public Change createChange(IProgressMonitor pm) throws CoreException {
		fChangeSignatureRefactoring.setValidationContext(getValidationContext());
		Change result;
		try {
			result= fChangeSignatureRefactoring.createChange(pm);
		} finally {
			fChangeSignatureRefactoring.setValidationContext(null);
		}
		if (result != null) {
			final ChangeDescriptor descriptor= result.getDescriptor();
			if (descriptor instanceof RefactoringChangeDescriptor) {
				final RefactoringDescriptor refactoringDescriptor= ((RefactoringChangeDescriptor) descriptor).getRefactoringDescriptor();
				if (refactoringDescriptor instanceof JDTRefactoringDescriptor) {
					final JDTRefactoringDescriptor extended= (JDTRefactoringDescriptor) refactoringDescriptor;
					final Map arguments= new HashMap();
					arguments.put(ATTRIBUTE_ARGUMENT, fParameter.getNewName());
					arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_SELECTION, Integer.valueOf(fSelectionStart).toString() + " " + Integer.valueOf(fSelectionLength).toString()); //$NON-NLS-1$
					arguments.putAll(extended.getArguments());
					String signature= fChangeSignatureRefactoring.getMethodName();
					try {
						signature= fChangeSignatureRefactoring.getOldMethodSignature();
					} catch (JavaScriptModelException exception) {
						JavaScriptPlugin.log(exception);
					}
					final String description= Messages.format(RefactoringCoreMessages.IntroduceParameterRefactoring_descriptor_description_short, fChangeSignatureRefactoring.getMethod().getElementName());
					final String header= Messages.format(RefactoringCoreMessages.IntroduceParameterRefactoring_descriptor_description, new String[] { fParameter.getNewName(), signature, ASTNodes.asString(fSelectedExpression)});
					final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(extended.getProject(), this, header);
					comment.addSetting(Messages.format(RefactoringCoreMessages.IntroduceParameterRefactoring_original_pattern, JavaScriptElementLabels.getTextLabel(fChangeSignatureRefactoring.getMethod(), JavaScriptElementLabels.ALL_FULLY_QUALIFIED)));
					comment.addSetting(Messages.format(RefactoringCoreMessages.IntroduceParameterRefactoring_expression_pattern, ASTNodes.asString(fSelectedExpression)));
					comment.addSetting(Messages.format(RefactoringCoreMessages.IntroduceParameterRefactoring_parameter_pattern, getAddedParameterInfo().getNewName()));
					result= new RefactoringDescriptorChange(new JDTRefactoringDescriptor(IJavaScriptRefactorings.INTRODUCE_PARAMETER, extended.getProject(), description, comment.asString(), arguments, extended.getFlags()), RefactoringCoreMessages.IntroduceParameterRefactoring_name, new Change[] { result});
				}
			}
		}
		return result;
	}

	public RefactoringStatus initialize(final RefactoringArguments arguments) {
		fArguments= arguments;
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			final String selection= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_SELECTION);
			if (selection != null) {
				int offset= -1;
				int length= -1;
				final StringTokenizer tokenizer= new StringTokenizer(selection);
				if (tokenizer.hasMoreTokens())
					offset= Integer.valueOf(tokenizer.nextToken()).intValue();
				if (tokenizer.hasMoreTokens())
					length= Integer.valueOf(tokenizer.nextToken()).intValue();
				if (offset >= 0 && length >= 0) {
					fSelectionStart= offset;
					fSelectionLength= length;
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, new Object[] { selection, JDTRefactoringDescriptor.ATTRIBUTE_SELECTION}));
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_SELECTION));
			final String handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists() || element.getElementType() != IJavaScriptElement.JAVASCRIPT_UNIT)
					return createInputFatalStatus(element, IJavaScriptRefactorings.INTRODUCE_PARAMETER);
				else
					fSourceCU= ((IFunction) element).getJavaScriptUnit();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
			final String name= extended.getAttribute(ATTRIBUTE_ARGUMENT);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				fParameterName= name;
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_ARGUMENT));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDelegateUpdatingTitle(boolean plural) {
		if (plural)
			return RefactoringCoreMessages.DelegateCreator_keep_original_changed_plural;
		else
			return RefactoringCoreMessages.DelegateCreator_keep_original_changed_singular;
	}
}
