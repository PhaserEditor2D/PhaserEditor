/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.FunctionRef;
import org.eclipse.wst.jsdt.core.dom.FunctionRefParameter;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.ImportDeclaration;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.MemberRef;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.TagElement;
import org.eclipse.wst.jsdt.core.dom.TextElement;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.JavaScriptRefactoringDescriptor;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.corext.Corext;
import org.eclipse.wst.jsdt.internal.corext.SourceRange;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.dom.Selection;
import org.eclipse.wst.jsdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.ExceptionInfo;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.wst.jsdt.internal.corext.refactoring.ReturnTypeInfo;
import org.eclipse.wst.jsdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.wst.jsdt.internal.corext.refactoring.StubTypeContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.TypeContextChecker;
import org.eclipse.wst.jsdt.internal.corext.refactoring.TypeContextChecker.IProblemVerifier;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStringStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.delegates.DelegateMethodCreator;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RefactoringAnalyzeUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RippleMethodFinder2;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.TempOccurrenceAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IDelegateUpdating;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.JavadocUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TightSourceRangeComputer;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

public class ChangeSignatureRefactoring extends ScriptableRefactoring implements IDelegateUpdating {
	
	private static final String ATTRIBUTE_RETURN= "return"; //$NON-NLS-1$
	private static final String ATTRIBUTE_VISIBILITY= "visibility"; //$NON-NLS-1$
	private static final String ATTRIBUTE_PARAMETER= "parameter"; //$NON-NLS-1$
	private static final String ATTRIBUTE_DEFAULT= "default"; //$NON-NLS-1$
	private static final String ATTRIBUTE_KIND= "kind"; //$NON-NLS-1$
	private static final String ATTRIBUTE_DELEGATE= "delegate"; //$NON-NLS-1$
	private static final String ATTRIBUTE_DEPRECATE= "deprecate"; //$NON-NLS-1$
	private static final String ATTRIBUTE_FUNCTION_HEAD="function"; //$NON-NLS-1$
	
	private static final boolean PREFIX_FUNCTION_HEAD = true;
	
	
	private List fParameterInfos;

	private CompilationUnitRewrite fBaseCuRewrite;
	private List fExceptionInfos;
	protected TextChangeManager fChangeManager;
	protected List/*<Change>*/ fOtherChanges;
	private IFunction fMethod;
	private IFunction fTopMethod;
	private IFunction[] fRippleMethods;
	private SearchResultGroup[] fOccurrences;
	private ReturnTypeInfo fReturnTypeInfo;
	private String fMethodName;
	private int fVisibility;
//	private static final String CONST_CLASS_DECL = "class A{";//$NON-NLS-1$
	private static final String CONST_ASSIGN = " i=";		//$NON-NLS-1$
	private static final String CONST_CLOSE = ";";			//$NON-NLS-1$

	private StubTypeContext fContextCuStartEnd;
	private int fOldVarargIndex; // initialized in checkVarargs()

	private BodyUpdater fBodyUpdater;
	private IDefaultValueAdvisor fDefaultValueAdvisor;

	private ITypeHierarchy fCachedTypeHierarchy= null;
	private boolean fDelegateUpdating;
	private boolean fDelegateDeprecation;

	/**
	 * Creates a new change signature refactoring.
	 * @param method the method, or <code>null</code> if invoked by scripting framework
	 * @throws JavaScriptModelException
	 */
	public ChangeSignatureRefactoring(IFunction method) throws JavaScriptModelException {
		fMethod= method;
		fOldVarargIndex= -1;
		fDelegateUpdating= false;
		fDelegateDeprecation= true;
		if (fMethod != null) {
			fParameterInfos= createParameterInfoList(method);
			// fExceptionInfos is created in checkInitialConditions
			fReturnTypeInfo= new ReturnTypeInfo(Signature.toString(Signature.getReturnType(fMethod.getSignature())));
			fMethodName= fMethod.getElementName();
			fVisibility= JdtFlags.getVisibilityCode(fMethod);
		}
	}

	private static List createParameterInfoList(IFunction method) {
		try {
			String[] typeNames= method.getParameterTypes();
			String[] oldNames= method.getParameterNames();
			List result= new ArrayList(typeNames.length);
			for (int i= 0; i < oldNames.length; i++){
				ParameterInfo parameterInfo;
				if (i == oldNames.length - 1 && Flags.isVarargs(method.getFlags())) {
					String varargSignature= typeNames[i];
					int arrayCount= Signature.getArrayCount(varargSignature);
					String baseSignature= Signature.getElementType(varargSignature);
					if (arrayCount > 1)
						baseSignature= Signature.createArraySignature(baseSignature, arrayCount - 1);
					parameterInfo= new ParameterInfo(Signature.toString(baseSignature) + ParameterInfo.ELLIPSIS, oldNames[i], i);
				} else {
					parameterInfo= new ParameterInfo(Signature.toString(typeNames[i]), oldNames[i], i);
				}
				result.add(parameterInfo);
			}
			return result;
		} catch(JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
			return new ArrayList(0);
		}		
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.ChangeSignatureRefactoring_modify_Parameters; 
	}
	
	public IFunction getMethod() {
		return fMethod;
	}
	
	public String getMethodName() {
		return fMethodName;
	}
	
	public String getReturnTypeString() {
		return fReturnTypeInfo.getNewTypeName();
	}	

	public void setNewMethodName(String newMethodName){
		Assert.isNotNull(newMethodName);
		fMethodName= newMethodName;
	}
	
	public void setNewReturnTypeName(String newReturnTypeName){
		Assert.isNotNull(newReturnTypeName);
		fReturnTypeInfo.setNewTypeName(newReturnTypeName);
	}
	
	public boolean canChangeNameAndReturnType(){
		try {
			return ! fMethod.isConstructor();
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
			return false;
		}
	}
	
	/**
	 * @return visibility
	 * @see org.eclipse.wst.jsdt.core.dom.Modifier
	 */
	public int getVisibility(){
		return fVisibility;
	}

	/**
	 * @param visibility new visibility
	 * @see org.eclipse.wst.jsdt.core.dom.Modifier
	 */	
	public void setVisibility(int visibility){
		Assert.isTrue(	visibility == Modifier.PUBLIC ||
		            	visibility == Modifier.PROTECTED ||
		            	visibility == Modifier.NONE ||
		            	visibility == Modifier.PRIVATE);  
		fVisibility= visibility;            	
	}
	
	/*
	 * @see JdtFlags
	 */	
	public int[] getAvailableVisibilities() throws JavaScriptModelException{
//		if (fTopMethod.getDeclaringType().isInterface())
//			return new int[]{Modifier.PUBLIC};
//		else if (fTopMethod.getDeclaringType().isEnum() && fTopMethod.isConstructor())
//			return new int[]{	Modifier.NONE,
//								Modifier.PRIVATE};
//		else
			return new int[]{	Modifier.PUBLIC,
								Modifier.PROTECTED,
								Modifier.NONE,
								Modifier.PRIVATE};
	}
	
	/**
	 * 
	 * @return List of <code>ParameterInfo</code> objects.
	 */
	public List getParameterInfos(){
		return fParameterInfos;
	}
	
	/**
	 * @return List of <code>ExceptionInfo</code> objects.
	 */
	public List getExceptionInfos(){
		return fExceptionInfos;
	}
	
	public void setBodyUpdater(BodyUpdater bodyUpdater) {
		fBodyUpdater= bodyUpdater;
	}
	
	public CompilationUnitRewrite getBaseCuRewrite() {
		return fBaseCuRewrite;
	}
	
	//------------------- IDelegateUpdating ----------------------
	
	public boolean canEnableDelegateUpdating() {
		return true;
	}

	public boolean getDelegateUpdating() {
		return fDelegateUpdating;
	}

	public void setDelegateUpdating(boolean updating) {
		fDelegateUpdating= updating;
	}

	public void setDeprecateDelegates(boolean deprecate) {
		fDelegateDeprecation= deprecate;
	}

	public boolean getDeprecateDelegates() {
		return fDelegateDeprecation;
	}

	//------------------- /IDelegateUpdating ---------------------
	
	public RefactoringStatus checkSignature() {
		return checkSignature(false, doGetProblemVerifier());
	}
	
	private RefactoringStatus checkSignature(boolean resolveBindings, IProblemVerifier problemVerifier) {
		RefactoringStatus result= new RefactoringStatus();
		checkMethodName(result);
		if (result.hasFatalError())
			return result;
		
		checkParameterNamesAndValues(result);
		if (result.hasFatalError())
			return result;
		
		checkForDuplicateParameterNames(result);
		if (result.hasFatalError())
			return result;
		
		try {
			RefactoringStatus[] typeStati;
			if (resolveBindings)
				typeStati= TypeContextChecker.checkAndResolveMethodTypes(fMethod, getStubTypeContext(), getNotDeletedInfos(), fReturnTypeInfo, problemVerifier);
			else
				typeStati= TypeContextChecker.checkMethodTypesSyntax(fMethod, getNotDeletedInfos(), fReturnTypeInfo);
			for (int i= 0; i < typeStati.length; i++)
				result.merge(typeStati[i]);
			
			result.merge(checkVarargs());
		} catch (CoreException e) {
			//cannot do anything here
			throw new RuntimeException(e);
		}
		
		//checkExceptions() unnecessary (IType always ok)
		return result;
	}
    
	public boolean isSignatureSameAsInitial() throws JavaScriptModelException {
		if (! isVisibilitySameAsInitial())
			return false;
		if (! isMethodNameSameAsInitial())
			return false;
		if (! isReturnTypeSameAsInitial())
			return false;
		if (! areExceptionsSameAsInitial())
			return false;
		
		if (fMethod.getNumberOfParameters() == 0 && fParameterInfos.isEmpty())
			return true;
		
		if (areNamesSameAsInitial() && isOrderSameAsInitial() && areParameterTypesSameAsInitial())
			return true;
		
		return false;
	}
	
	/**
	 * @return true if the new method cannot coexist with the old method since
	 *         the signatures are too much alike
	 * @throws JavaScriptModelException
	 */
	public boolean isSignatureClashWithInitial() throws JavaScriptModelException {

		if (!isMethodNameSameAsInitial())
			return false; // name has changed.

		if (fMethod.getNumberOfParameters() == 0 && fParameterInfos.isEmpty())
			return true; // name is equal and both parameter lists are empty

		// name is equal and there are some parameters.
		// check if there are more or less parameters than before

		int no= getNotDeletedInfos().size();

		if (fMethod.getNumberOfParameters() != no)
			return false;

		// name is equal and parameter count is equal.
		// check whether types remained the same
		
		if (isOrderSameAsInitial())
			return areParameterTypesSameAsInitial();
		else
			return false; // could be more specific here
	}

	private boolean areParameterTypesSameAsInitial() {
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (! info.isAdded() && ! info.isDeleted() && info.isTypeNameChanged())
				return false;
		}
		return true;
	}
	
	private boolean isReturnTypeSameAsInitial() throws JavaScriptModelException {
		return ! fReturnTypeInfo.isTypeNameChanged();
	}
	
	private boolean isMethodNameSameAsInitial() {
		return fMethodName.equals(fMethod.getElementName());
	}
	
	private boolean areExceptionsSameAsInitial() {
		for (Iterator iter= fExceptionInfos.iterator(); iter.hasNext();) {
			ExceptionInfo info= (ExceptionInfo) iter.next();
			if (! info.isOld())
				return false;
		}
		return true;
	}
	
	private void checkParameterNamesAndValues(RefactoringStatus result) {
		int i= 1;
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext(); i++) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (info.isDeleted())
				continue;
			checkParameterName(result, info, i);
			if (result.hasFatalError())
				return;
			if (info.isAdded())	{
				checkParameterDefaultValue(result, info);
				if (result.hasFatalError())
					return;
			}
		}
	}
	
	private void checkParameterName(RefactoringStatus result, ParameterInfo info, int position) {
		if (info.getNewName().trim().length() == 0) {
			result.addFatalError(Messages.format(
					RefactoringCoreMessages.ChangeSignatureRefactoring_param_name_not_empty, Integer.toString(position))); 
		} else {
			result.merge(Checks.checkTempName(info.getNewName()));
		}
	}

	private void checkMethodName(RefactoringStatus result) {
		if (isMethodNameSameAsInitial() || ! canChangeNameAndReturnType())
			return;
		if ("".equals(fMethodName.trim())) { //$NON-NLS-1$
			String msg= RefactoringCoreMessages.ChangeSignatureRefactoring_method_name_not_empty; 
			result.addFatalError(msg);
			return;
		}
		if (fMethod.getDeclaringType() != null && fMethodName.equals(fMethod.getDeclaringType().getElementName())) {
			String msg= RefactoringCoreMessages.ChangeSignatureRefactoring_constructor_name; 
			result.addWarning(msg);
		}
		result.merge(Checks.checkMethodName(fMethodName));
	}

	private void checkParameterDefaultValue(RefactoringStatus result, ParameterInfo info) {
		if (fDefaultValueAdvisor != null)
			return;
		if (info.isNewVarargs()) {
			if (! isValidVarargsExpression(info.getDefaultValue())){
				String msg= Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_invalid_expression, new String[]{info.getDefaultValue()}); 
				result.addFatalError(msg);
			}	
			return;
		}
		
		if (info.getDefaultValue().trim().equals("")){ //$NON-NLS-1$
			String msg= Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_default_value, new String[]{info.getNewName()}); 
			result.addFatalError(msg);
			return;
		}	
		if (! isValidExpression(info.getDefaultValue())){
			String msg= Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_invalid_expression, new String[]{info.getDefaultValue()}); 
			result.addFatalError(msg);
		}	
	}
	
	private RefactoringStatus checkVarargs() throws JavaScriptModelException {
		RefactoringStatus result= checkOriginalVarargs();
		if (result != null)
			return result;
			
		if (fRippleMethods != null) {
			for (int iRipple= 0; iRipple < fRippleMethods.length; iRipple++) {
				IFunction rippleMethod= fRippleMethods[iRipple];
				if (! JdtFlags.isVarargs(rippleMethod))
					continue;
				
				// Vararg method can override method that takes an array as last argument
				fOldVarargIndex= rippleMethod.getNumberOfParameters() - 1;
				List notDeletedInfos= getNotDeletedInfos();
				for (int i= 0; i < notDeletedInfos.size(); i++) {
					ParameterInfo info= (ParameterInfo) notDeletedInfos.get(i);
					if (fOldVarargIndex != -1 && info.getOldIndex() == fOldVarargIndex && ! info.isNewVarargs()) {
						String rippleMethodType= JavaModelUtil.getFullyQualifiedName(rippleMethod.getDeclaringType());
						String message= Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_ripple_cannot_convert_vararg, new Object[] {info.getNewName(), rippleMethodType}); 
						return RefactoringStatus.createFatalErrorStatus(message, JavaStatusContext.create(rippleMethod));
					}
				}
			}
		}
		
		return null;
	}

	private RefactoringStatus checkOriginalVarargs() throws JavaScriptModelException {
		if (JdtFlags.isVarargs(fMethod))
			fOldVarargIndex= fMethod.getNumberOfParameters() - 1;
		List notDeletedInfos= getNotDeletedInfos();
		for (int i= 0; i < notDeletedInfos.size(); i++) {
			ParameterInfo info= (ParameterInfo) notDeletedInfos.get(i);
			if (info.isOldVarargs() && ! info.isNewVarargs())
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_cannot_convert_vararg, info.getNewName())); 
			if (i != notDeletedInfos.size() - 1) {
				// not the last parameter
				if (info.isNewVarargs())
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_vararg_must_be_last, info.getNewName())); 
			}
		}
		return null;
	}
	
	private RefactoringStatus checkTypeVariables() throws JavaScriptModelException {
		if (fRippleMethods.length == 1)
			return null;
		
		RefactoringStatus result= new RefactoringStatus();
		if (fReturnTypeInfo.isTypeNameChanged() && fReturnTypeInfo.getNewTypeBinding() != null) {
			HashSet typeVariablesCollector= new HashSet();
			collectTypeVariables(fReturnTypeInfo.getNewTypeBinding(), typeVariablesCollector);
			if (typeVariablesCollector.size() != 0) {
				ITypeBinding first= (ITypeBinding) typeVariablesCollector.iterator().next();
				String msg= Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_return_type_contains_type_variable, new String[] {fReturnTypeInfo.getNewTypeName(), first.getName()}); 
				result.addError(msg);
			}
		}
		
		for (Iterator iter= getNotDeletedInfos().iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (info.isTypeNameChanged() && info.getNewTypeBinding() != null) {
				HashSet typeVariablesCollector= new HashSet();
				collectTypeVariables(info.getNewTypeBinding(), typeVariablesCollector);
				if (typeVariablesCollector.size() != 0) {
					ITypeBinding first= (ITypeBinding) typeVariablesCollector.iterator().next();
					String msg= Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_parameter_type_contains_type_variable, new String[] {info.getNewTypeName(), info.getNewName(), first.getName()}); 
					result.addError(msg);
				}
			}
		}
		return result;
	}
	
	private void collectTypeVariables(ITypeBinding typeBinding, Set typeVariablesCollector) {
		if (typeBinding.isArray()) {
			collectTypeVariables(typeBinding.getElementType(), typeVariablesCollector);
			
		}
	}
	
	public static boolean isValidExpression(String string){
		String trimmed= string.trim();
		StringBuffer cuBuff= new StringBuffer();
		cuBuff.append(CONST_ASSIGN);
		int offset= cuBuff.length();
		cuBuff.append(trimmed)
			  .append(CONST_CLOSE);
		ASTParser p= ASTParser.newParser(AST.JLS3);
		p.setSource(cuBuff.toString().toCharArray());
		JavaScriptUnit cu= (JavaScriptUnit) p.createAST(null);
		Selection selection= Selection.createFromStartLength(offset, trimmed.length());
		SelectionAnalyzer analyzer= new SelectionAnalyzer(selection, false);
		cu.accept(analyzer);
		ASTNode selected= analyzer.getFirstSelectedNode();
		return (selected instanceof Expression) && 
				trimmed.equals(cuBuff.substring(cu.getExtendedStartPosition(selected), cu.getExtendedStartPosition(selected) + cu.getExtendedLength(selected)));
		/*
		if ("".equals(trimmed)) //speed up for a common case //$NON-NLS-1$
			return false;
		StringBuffer cuBuff= new StringBuffer();
		cuBuff.append(CONST_CLASS_DECL)
			  .append("Object") //$NON-NLS-1$
			  .append(CONST_ASSIGN);
		int offset= cuBuff.length();
		cuBuff.append(trimmed)
			  .append(CONST_CLOSE);
		ASTParser p= ASTParser.newParser(AST.JLS3);
		p.setSource(cuBuff.toString().toCharArray());
		JavaScriptUnit cu= (JavaScriptUnit) p.createAST(null);
		Selection selection= Selection.createFromStartLength(offset, trimmed.length());
		SelectionAnalyzer analyzer= new SelectionAnalyzer(selection, false);
		cu.accept(analyzer);
		ASTNode selected= analyzer.getFirstSelectedNode();
		return (selected instanceof Expression) && 
				trimmed.equals(cuBuff.substring(cu.getExtendedStartPosition(selected), cu.getExtendedStartPosition(selected) + cu.getExtendedLength(selected)));
		*/
	}
	
	public static boolean isValidVarargsExpression(String string) {
		String trimmed= string.trim();
		if ("".equals(trimmed)) //speed up for a common case //$NON-NLS-1$
			return true;
		StringBuffer cuBuff= new StringBuffer();
		cuBuff.append("class A{ {m("); //$NON-NLS-1$
		int offset= cuBuff.length();
		cuBuff.append(trimmed)
			  .append(");}}"); //$NON-NLS-1$
		ASTParser p= ASTParser.newParser(AST.JLS3);
		p.setSource(cuBuff.toString().toCharArray());
		JavaScriptUnit cu= (JavaScriptUnit) p.createAST(null);
		Selection selection= Selection.createFromStartLength(offset, trimmed.length());
		SelectionAnalyzer analyzer= new SelectionAnalyzer(selection, false);
		cu.accept(analyzer);
		ASTNode[] selectedNodes= analyzer.getSelectedNodes();
		if (selectedNodes.length == 0)
			return false;
		for (int i= 0; i < selectedNodes.length; i++) {
			if (! (selectedNodes[i] instanceof Expression))
				return false;
		}
		return true;
	}

	public StubTypeContext getStubTypeContext() {
		try {
			if (fContextCuStartEnd == null)
				fContextCuStartEnd= TypeContextChecker.createStubTypeContext(getCu(), fBaseCuRewrite.getRoot(), fMethod.getSourceRange().getOffset());
		} catch (CoreException e) {
			//cannot do anything here
			throw new RuntimeException(e);
		}
		return fContextCuStartEnd;
	}

	private ITypeHierarchy getCachedTypeHierarchy(IProgressMonitor monitor) throws JavaScriptModelException {
		if (fCachedTypeHierarchy == null)
			fCachedTypeHierarchy= fMethod.getDeclaringType().newTypeHierarchy(new SubProgressMonitor(monitor, 1));
		return fCachedTypeHierarchy;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("", 5); //$NON-NLS-1$
			RefactoringStatus result= Checks.checkIfCuBroken(fMethod);
			if (result.hasFatalError())
				return result;
			if (fMethod == null || !fMethod.exists()) {
				String message= Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_method_deleted, getCu().getElementName());
				return RefactoringStatus.createFatalErrorStatus(message);
			}
//			if (fMethod.getDeclaringType().isInterface()) {
//				fTopMethod= MethodChecks.overridesAnotherMethod(fMethod, fMethod.getDeclaringType().newSupertypeHierarchy(new SubProgressMonitor(monitor, 1)));
//				monitor.worked(1);
//			} else 
			if (MethodChecks.isVirtual(fMethod)) {
				ITypeHierarchy hierarchy= getCachedTypeHierarchy(new SubProgressMonitor(monitor, 1));
				fTopMethod= null;
				if (fTopMethod == null)
					fTopMethod= MethodChecks.overridesAnotherMethod(fMethod, hierarchy);
			}
			if (fTopMethod == null)
				fTopMethod= fMethod;
			if (! fTopMethod.equals(fMethod)) {
				RefactoringStatusContext context= JavaStatusContext.create(fTopMethod);
				String message= Messages.format(RefactoringCoreMessages.MethodChecks_overrides, 
						new String[]{JavaElementUtil.createMethodSignature(fTopMethod), JavaModelUtil.getFullyQualifiedName(fTopMethod.getDeclaringType())});
				return RefactoringStatus.createStatus(RefactoringStatus.FATAL, message, context, Corext.getPluginId(), RefactoringStatusCodes.OVERRIDES_ANOTHER_METHOD, fTopMethod);
			
			}

			if (monitor.isCanceled())
				throw new OperationCanceledException();

			if (fBaseCuRewrite == null || !fBaseCuRewrite.getCu().equals(getCu())) {
				fBaseCuRewrite= new CompilationUnitRewrite(getCu());
				fBaseCuRewrite.getASTRewrite().setTargetSourceRangeComputer(new TightSourceRangeComputer());
			}
			monitor.worked(1);
			result.merge(createExceptionInfoList());
			monitor.worked(1);
			return result;
		} finally {
			monitor.done();
		}
	}
	
	private RefactoringStatus createExceptionInfoList() {
		if (fExceptionInfos == null || fExceptionInfos.isEmpty()) {
			fExceptionInfos= new ArrayList(0);
			try {
				ASTNode nameNode= NodeFinder.perform(fBaseCuRewrite.getRoot(), fMethod.getNameRange());
				if (nameNode == null || !(nameNode instanceof Name) || !(nameNode.getParent() instanceof FunctionDeclaration))
					return null;
				FunctionDeclaration methodDeclaration= (FunctionDeclaration) nameNode.getParent();
				List exceptions= methodDeclaration.thrownExceptions();
				List result= new ArrayList(exceptions.size());
				for (int i= 0; i < exceptions.size(); i++) {
					Name name= (Name) exceptions.get(i);
					ITypeBinding typeBinding= name.resolveTypeBinding();
					if (typeBinding == null)
						return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ChangeSignatureRefactoring_no_exception_binding);
					IType type= (IType) typeBinding.getJavaElement();
					result.add(ExceptionInfo.createInfoForOldException(type, typeBinding));
				}
				fExceptionInfos= result;
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
			}
		}
		return null;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.ChangeSignatureRefactoring_checking_preconditions, 8); 
			RefactoringStatus result= new RefactoringStatus();
			clearManagers();
			fBaseCuRewrite.clearASTAndImportRewrites();
			fBaseCuRewrite.getASTRewrite().setTargetSourceRangeComputer(new TightSourceRangeComputer());

			if (isSignatureSameAsInitial())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ChangeSignatureRefactoring_unchanged); 
			result.merge(checkSignature(true, doGetProblemVerifier()));
			if (result.hasFatalError())
				return result;
			
			if (fDelegateUpdating && isSignatureClashWithInitial()) 
				result.merge(RefactoringStatus.createErrorStatus(RefactoringCoreMessages.ChangeSignatureRefactoring_old_and_new_signatures_not_sufficiently_different ));

			fRippleMethods= RippleMethodFinder2.getRelatedMethods(fMethod, new SubProgressMonitor(pm, 1), null);
			result.merge(checkVarargs());
			if (result.hasFatalError())
				return result;
			
			fOccurrences= findOccurrences(new SubProgressMonitor(pm, 1), result);
			
			result.merge(checkVisibilityChanges());
			result.merge(checkTypeVariables());
			
			//TODO:
			// We need a common way of dealing with possible compilation errors for all occurrences,
			// including visibility problems, shadowing and missing throws declarations.
			
			if (! isOrderSameAsInitial())
				result.merge(checkReorderings(new SubProgressMonitor(pm, 1)));
			else
				pm.worked(1);
			
			//TODO (bug 58616): check whether changed signature already exists somewhere in the ripple,
			// - error if exists
			// - warn if exists with different parameter types (may cause overloading)
			
			if (! areNamesSameAsInitial())
				result.merge(checkRenamings(new SubProgressMonitor(pm, 1)));
			else
				pm.worked(1);
			if (result.hasFatalError())
				return result;
			
//			resolveTypesWithoutBindings(new SubProgressMonitor(pm, 1)); // already done in checkSignature(true)

			createChangeManager(new SubProgressMonitor(pm, 1), result);
			fCachedTypeHierarchy= null;

			if (mustAnalyzeAstOfDeclaringCu()) 
				result.merge(checkCompilationofDeclaringCu()); //TODO: should also check in ripple methods (move into createChangeManager)
			if (result.hasFatalError())
				return result;

			result.merge(validateModifiesFiles());
			return result;
		} finally {
			pm.done();
		}
	}

	protected IProblemVerifier doGetProblemVerifier() {
		return null;
	}

	private void clearManagers() {
		fChangeManager= null;
		fOtherChanges= new ArrayList();
	}
	
	private RefactoringStatus checkVisibilityChanges() throws JavaScriptModelException {
		if (isVisibilitySameAsInitial())
			return null;
	    if (fRippleMethods.length == 1)
	    	return null;
	    Assert.isTrue(JdtFlags.getVisibilityCode(fMethod) != Modifier.PRIVATE);
	    if (fVisibility == Modifier.PRIVATE)
	    	return RefactoringStatus.createWarningStatus(RefactoringCoreMessages.ChangeSignatureRefactoring_non_virtual); 
		return null;
	}

	public String getOldMethodSignature() throws JavaScriptModelException{
		StringBuffer buff= new StringBuffer();
		
		int flags= getMethod().getFlags();
		if(JavaScriptCore.IS_ECMASCRIPT4) {
			buff.append(getVisibilityString(flags));
			if (Flags.isStatic(flags))
				buff.append("static "); //$NON-NLS-1$
		}
		
		if(PREFIX_FUNCTION_HEAD) {
			buff.append(ATTRIBUTE_FUNCTION_HEAD + " "); //$NON-NLS-1$
		}
		
		if (! getMethod().isConstructor() && JavaScriptCore.IS_ECMASCRIPT4)
			buff.append(fReturnTypeInfo.getOldTypeName())
				.append(' ');

		buff.append(JavaScriptElementLabels.getElementLabel(fMethod.getParent(), JavaScriptElementLabels.ALL_FULLY_QUALIFIED));
		buff.append('.');
		buff.append(fMethod.getElementName())
			.append(Signature.C_PARAM_START)
			.append(getOldMethodParameters())
			.append(Signature.C_PARAM_END);
		
		buff.append(getOldMethodThrows());
		
		return buff.toString();
	}

	public String getNewMethodSignature() throws JavaScriptModelException{
		StringBuffer buff= new StringBuffer();
		if(PREFIX_FUNCTION_HEAD) {
			buff.append(ATTRIBUTE_FUNCTION_HEAD + " "); //$NON-NLS-1$
		}
		if(JavaScriptCore.IS_ECMASCRIPT4) {
			buff.append(getVisibilityString(fVisibility));
			if (Flags.isStatic(getMethod().getFlags()))
				buff.append("static "); //$NON-NLS-1$
		}
		if (! getMethod().isConstructor() && JavaScriptCore.IS_ECMASCRIPT4)
			buff.append(getReturnTypeString()).append(' ');

		buff.append(getMethodName())
			.append(Signature.C_PARAM_START)
			.append(getMethodParameters())
			.append(Signature.C_PARAM_END);
		if(JavaScriptCore.IS_ECMASCRIPT4) 
			buff.append(getMethodThrows());
		
		return buff.toString();
	}

	private String getVisibilityString(int visibility) {
		String visibilityString= JdtFlags.getVisibilityString(visibility);
		if ("".equals(visibilityString)) //$NON-NLS-1$
			return visibilityString;
		return visibilityString + ' ';
	}

	private String getMethodThrows() {
		final String throwsString= " throws "; //$NON-NLS-1$
		StringBuffer buff= new StringBuffer(throwsString);
		for (Iterator iter= fExceptionInfos.iterator(); iter.hasNext(); ) {
			ExceptionInfo info= (ExceptionInfo) iter.next();
			if (! info.isDeleted()) {
				buff.append(info.getType().getElementName());
				buff.append(", "); //$NON-NLS-1$
			}
		}
		if (buff.length() == throwsString.length())
			return ""; //$NON-NLS-1$
		buff.delete(buff.length() - 2, buff.length());
		return buff.toString();
	}

	private String getOldMethodThrows() {
		final String throwsString= " throws "; //$NON-NLS-1$
		StringBuffer buff= new StringBuffer(throwsString);
		for (Iterator iter= fExceptionInfos.iterator(); iter.hasNext(); ) {
			ExceptionInfo info= (ExceptionInfo) iter.next();
			if (! info.isAdded()) {
				buff.append(info.getType().getElementName());
				buff.append(", "); //$NON-NLS-1$
			}
		}
		if (buff.length() == throwsString.length())
			return ""; //$NON-NLS-1$
		buff.delete(buff.length() - 2, buff.length());
		return buff.toString();
	}

	private void checkForDuplicateParameterNames(RefactoringStatus result){
		Set found= new HashSet();
		Set doubled= new HashSet();
		for (Iterator iter = getNotDeletedInfos().iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo)iter.next();
			String newName= info.getNewName();
			if (found.contains(newName) && !doubled.contains(newName)){
				result.addFatalError(Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_duplicate_name, newName));
				doubled.add(newName);
			} else {
				found.add(newName);
			}	
		}
	}
	
	private IJavaScriptUnit getCu() {
		return fMethod.getJavaScriptUnit();
	}
	
	private boolean mustAnalyzeAstOfDeclaringCu() throws JavaScriptModelException{
		if (JdtFlags.isAbstract(getMethod()))
			return false;
		else 
			return true;
	}
	
	private RefactoringStatus checkCompilationofDeclaringCu() throws CoreException {
		IJavaScriptUnit cu= getCu();
		TextChange change= fChangeManager.get(cu);
		String newCuSource= change.getPreviewContent(new NullProgressMonitor());
		JavaScriptUnit newCUNode= new RefactoringASTParser(AST.JLS3).parse(newCuSource, cu, true, false, null);
		IProblem[] problems= RefactoringAnalyzeUtil.getIntroducedCompileProblems(newCUNode, fBaseCuRewrite.getRoot());
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < problems.length; i++) {
			IProblem problem= problems[i];
			if (shouldReport(problem, newCUNode))
				result.addEntry(new RefactoringStatusEntry((problem.isError() ? RefactoringStatus.ERROR : RefactoringStatus.WARNING), problem.getMessage(), new JavaStringStatusContext(newCuSource, new SourceRange(problem))));
		}
		return result;
	}
		
	private boolean shouldReport(IProblem problem, JavaScriptUnit cu) {
		if (! problem.isError())
			return false;
		if (problem.getID() == IProblem.UndefinedType) //reported when trying to import
			return false;
		ASTNode node= ASTNodeSearchUtil.getAstNode(cu, problem.getSourceStart(), problem.getSourceEnd() - problem.getSourceStart());
		IProblemVerifier verifier= doGetProblemVerifier();
		if (verifier != null)
			return verifier.isError(problem, node);
		return true;	
	}

	private String getOldMethodParameters() {
		StringBuffer buff= new StringBuffer();
		int i= 0;
		for (Iterator iter= getNotAddedInfos().iterator(); iter.hasNext(); i++) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (i != 0 )
				buff.append(", ");  //$NON-NLS-1$
			buff.append(createDeclarationString(info));
		}
		return buff.toString();
	}
		
	private String getMethodParameters() {
		StringBuffer buff= new StringBuffer();
		int i= 0;
		for (Iterator iter= getNotDeletedInfos().iterator(); iter.hasNext(); i++) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (i != 0 )
				buff.append(", ");  //$NON-NLS-1$
			buff.append(createDeclarationString(info));
		}
		return buff.toString();
	}

	private List getAddedInfos(){
		List result= new ArrayList(1);
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (info.isAdded())
				result.add(info);
		}
		return result;
	}

	private List getDeletedInfos(){
		List result= new ArrayList(1);
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (info.isDeleted())
				result.add(info);
		}
		return result;
	}

	private List getNotAddedInfos(){
		List all= new ArrayList(fParameterInfos);
		all.removeAll(getAddedInfos());
		return all;
	}
	
	private List getNotDeletedInfos(){
		List all= new ArrayList(fParameterInfos);
		all.removeAll(getDeletedInfos());
		return all;
	}
	
	private boolean areNamesSameAsInitial() {
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (info.isRenamed())
				return false;
		}
		return true;
	}

	private boolean isOrderSameAsInitial(){
		int i= 0;
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext(); i++) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (info.getOldIndex() != i) // includes info.isAdded()
				return false;
			if (info.isDeleted())
				return false;
		}
		return true;
	}

	private RefactoringStatus checkReorderings(IProgressMonitor pm) throws JavaScriptModelException {
		try{
			pm.beginTask(RefactoringCoreMessages.ChangeSignatureRefactoring_checking_preconditions, 1); 
			return checkNativeMethods();
		} finally{
			pm.done();
		}	
	}
	
	private RefactoringStatus checkRenamings(IProgressMonitor pm) throws JavaScriptModelException {
		try{
			pm.beginTask(RefactoringCoreMessages.ChangeSignatureRefactoring_checking_preconditions, 1); 
			return checkParameterNamesInRippleMethods();
		} finally{
			pm.done();
		}	
	}
	
	private RefactoringStatus checkParameterNamesInRippleMethods() throws JavaScriptModelException {
		RefactoringStatus result= new RefactoringStatus();
		Set newParameterNames= getNewParameterNamesList();
		for (int i= 0; i < fRippleMethods.length; i++) {
			String[] paramNames= fRippleMethods[i].getParameterNames();
			for (int j= 0; j < paramNames.length; j++) {
				if (newParameterNames.contains(paramNames[j])){
					String[] args= new String[]{JavaElementUtil.createMethodSignature(fRippleMethods[i]), paramNames[j]};
					String msg= Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_already_has, args); 
					RefactoringStatusContext context= JavaStatusContext.create(fRippleMethods[i].getJavaScriptUnit(), fRippleMethods[i].getNameRange());
					result.addError(msg, context);
				}	
			}
		}
		return result;
	}
	
	private Set getNewParameterNamesList() {
		Set oldNames= getOriginalParameterNames();
		Set currentNames= getNamesOfNotDeletedParameters();
		currentNames.removeAll(oldNames);
		return currentNames;
	}
	
	private Set getNamesOfNotDeletedParameters() {
		Set result= new HashSet();
		for (Iterator iter= getNotDeletedInfos().iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			result.add(info.getNewName());
		}
		return result;
	}
	
	private Set getOriginalParameterNames() {
		Set result= new HashSet();
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (! info.isAdded())
				result.add(info.getOldName());
		}
		return result;
	}
	
	private RefactoringStatus checkNativeMethods() throws JavaScriptModelException{
		RefactoringStatus result= new RefactoringStatus();
		return result;
	}

	private IFile[] getAllFilesToModify(){
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles(){
		return Checks.validateModifiesFiles(getAllFilesToModify(), getValidationContext());
	}

	public Change createChange(IProgressMonitor pm) {
		pm.beginTask("", 1); //$NON-NLS-1$
		try {
			final TextChange[] changes= fChangeManager.getAllChanges();
			final List list= new ArrayList(changes.length);
			list.addAll(fOtherChanges);
			list.addAll(Arrays.asList(changes));
			final Map arguments= new HashMap();
			String project= null;
			IJavaScriptProject javaProject= fMethod.getJavaScriptProject();
			if (javaProject != null)
				project= javaProject.getElementName();
			int flags= JavaScriptRefactoringDescriptor.JAR_MIGRATION | JavaScriptRefactoringDescriptor.JAR_REFACTORING | RefactoringDescriptor.STRUCTURAL_CHANGE;
			try {
				if (!Flags.isPrivate(fMethod.getFlags()))
					flags|= RefactoringDescriptor.MULTI_CHANGE;
				final IType declaring= fMethod.getDeclaringType();
				if (declaring!=null && (declaring.isAnonymous() || declaring.isLocal()))
					flags|= JavaScriptRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
			} catch (JavaScriptModelException exception) {
				JavaScriptPlugin.log(exception);
			}
			JDTRefactoringDescriptor descriptor= null;
			try {
				final String description= Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_descriptor_description_short, fMethod.getElementName());
				final String header= Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_descriptor_description, new String[] { getOldMethodSignature(), getNewMethodSignature()});
				final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
				if (!fMethod.getElementName().equals(fMethodName))
					comment.addSetting(Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_new_name_pattern, fMethodName));
				if (!isVisibilitySameAsInitial()) {
					String visibility= JdtFlags.getVisibilityString(fVisibility);
					if ("".equals(visibility)) //$NON-NLS-1$
						visibility= RefactoringCoreMessages.ChangeSignatureRefactoring_default_visibility;
					comment.addSetting(Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_new_visibility_pattern, visibility));
				}
				if (fReturnTypeInfo.isTypeNameChanged())
					comment.addSetting(Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_new_return_type_pattern, fReturnTypeInfo.getNewTypeName()));
				List deleted= new ArrayList();
				List added= new ArrayList();
				List changed= new ArrayList();
				for (final Iterator iterator= fParameterInfos.iterator(); iterator.hasNext();) {
					final ParameterInfo info= (ParameterInfo) iterator.next();
					if (info.isDeleted())
						deleted.add(Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_deleted_parameter_pattern, new String[] { info.getOldTypeName(), info.getOldName()}));
					else if (info.isAdded())
						added.add(Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_added_parameter_pattern, new String[] { info.getNewTypeName(), info.getNewName()}));
					else if (info.isRenamed() || info.isTypeNameChanged() || info.isVarargChanged())
						changed.add(Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_changed_parameter_pattern, new String[] { info.getOldTypeName(), info.getOldName()}));
				}
				if (!added.isEmpty())
					comment.addSetting(JDTRefactoringDescriptorComment.createCompositeSetting(RefactoringCoreMessages.ChangeSignatureRefactoring_added_parameters, (String[]) added.toArray(new String[added.size()])));
				if (!deleted.isEmpty())
					comment.addSetting(JDTRefactoringDescriptorComment.createCompositeSetting(RefactoringCoreMessages.ChangeSignatureRefactoring_removed_parameters, (String[]) deleted.toArray(new String[deleted.size()])));
				if (!changed.isEmpty())
					comment.addSetting(JDTRefactoringDescriptorComment.createCompositeSetting(RefactoringCoreMessages.ChangeSignatureRefactoring_changed_parameters, (String[]) changed.toArray(new String[changed.size()])));
				added.clear();
				deleted.clear();
				changed.clear();
				for (final Iterator iterator= fExceptionInfos.iterator(); iterator.hasNext();) {
					final ExceptionInfo info= (ExceptionInfo) iterator.next();
					if (info.isAdded())
						added.add(info.getType().getElementName());
					else if (info.isDeleted())
						deleted.add(info.getType().getElementName());
				}
				if (!added.isEmpty())
					comment.addSetting(JDTRefactoringDescriptorComment.createCompositeSetting(RefactoringCoreMessages.ChangeSignatureRefactoring_added_exceptions, (String[]) added.toArray(new String[added.size()])));
				if (!deleted.isEmpty())
					comment.addSetting(JDTRefactoringDescriptorComment.createCompositeSetting(RefactoringCoreMessages.ChangeSignatureRefactoring_removed_exceptions, (String[]) deleted.toArray(new String[deleted.size()])));
				descriptor= new JDTRefactoringDescriptor(IJavaScriptRefactorings.CHANGE_METHOD_SIGNATURE, project, description, comment.asString(), arguments, flags);
				arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fMethod));
				arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_NAME, fMethodName);
				arguments.put(ATTRIBUTE_DELEGATE, Boolean.valueOf(fDelegateUpdating).toString());
				arguments.put(ATTRIBUTE_DEPRECATE, Boolean.valueOf(fDelegateDeprecation).toString());
				if (fReturnTypeInfo.isTypeNameChanged())
					arguments.put(ATTRIBUTE_RETURN, fReturnTypeInfo.getNewTypeName());
				try {
					if (!isVisibilitySameAsInitial())
						arguments.put(ATTRIBUTE_VISIBILITY, Integer.valueOf(fVisibility).toString());
				} catch (JavaScriptModelException exception) {
					JavaScriptPlugin.log(exception);
				}
				int count= 1;
				for (final Iterator iterator= fParameterInfos.iterator(); iterator.hasNext();) {
					final ParameterInfo info= (ParameterInfo) iterator.next();
					final StringBuffer buffer= new StringBuffer(64);
					buffer.append(info.getOldTypeName());
					buffer.append(" "); //$NON-NLS-1$
					buffer.append(info.getOldName());
					buffer.append(" "); //$NON-NLS-1$
					buffer.append(info.getOldIndex());
					buffer.append(" "); //$NON-NLS-1$
					buffer.append(info.getNewTypeName());
					buffer.append(" "); //$NON-NLS-1$
					buffer.append(info.getNewName());
					buffer.append(" "); //$NON-NLS-1$
					buffer.append(info.isDeleted());
					arguments.put(ATTRIBUTE_PARAMETER + count, buffer.toString());
					final String value= info.getDefaultValue();
					if (value != null && !"".equals(value)) //$NON-NLS-1$
						arguments.put(ATTRIBUTE_DEFAULT + count, value);
					count++;
				}
				count= 1;
				for (final Iterator iterator= fExceptionInfos.iterator(); iterator.hasNext();) {
					final ExceptionInfo info= (ExceptionInfo) iterator.next();
					arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + count, descriptor.elementToHandle(info.getType()));
					arguments.put(ATTRIBUTE_KIND + count, Integer.valueOf(info.getKind()).toString());
					count++;
				}
			} catch (JavaScriptModelException exception) {
				JavaScriptPlugin.log(exception);
				return null;
			}
			return new DynamicValidationRefactoringChange(descriptor, doGetRefactoringChangeName(), (Change[]) list.toArray(new Change[list.size()]));
		} finally {
			pm.done();
			clearManagers();
		}
	}

	protected String doGetRefactoringChangeName() {
		return RefactoringCoreMessages.ChangeSignatureRefactoring_restructure_parameters;
	}

	private TextChangeManager createChangeManager(IProgressMonitor pm, RefactoringStatus result) throws CoreException {
		pm.beginTask(RefactoringCoreMessages.ChangeSignatureRefactoring_preview, 2); 
		fChangeManager= new TextChangeManager();
		boolean isNoArgConstructor= isNoArgConstructor();
		Map namedSubclassMapping= null;
		if (isNoArgConstructor){
			//create only when needed;
			namedSubclassMapping= createNamedSubclassMapping(new SubProgressMonitor(pm, 1));
		}else{
			pm.worked(1);
		}
		for (int i= 0; i < fOccurrences.length; i++) {
			if (pm.isCanceled())
				throw new OperationCanceledException();
			SearchResultGroup group= fOccurrences[i];
			IJavaScriptUnit cu= group.getCompilationUnit();
			if (cu == null)
				continue;
			CompilationUnitRewrite cuRewrite;
			if (cu.equals(getCu())) {
				cuRewrite= fBaseCuRewrite;
			} else {
				cuRewrite= new CompilationUnitRewrite(cu);
				cuRewrite.getASTRewrite().setTargetSourceRangeComputer(new TightSourceRangeComputer());
			}
			ASTNode[] nodes= ASTNodeSearchUtil.findNodes(group.getSearchResults(), cuRewrite.getRoot());
			
			//IntroduceParameterObjectRefactoring needs to update declarations first:
			List/*<OccurrenceUpdate>*/ deferredUpdates= new ArrayList();
			for (int j= 0; j < nodes.length; j++) {
				OccurrenceUpdate update= createOccurrenceUpdate(nodes[j], cuRewrite, result);
				if (update instanceof DeclarationUpdate) {
					update.updateNode();
				} else {
					deferredUpdates.add(update);
				}
			}
			for (Iterator iter= deferredUpdates.iterator(); iter.hasNext();) {
				((OccurrenceUpdate) iter.next()).updateNode();
			}
			
			if (isNoArgConstructor && namedSubclassMapping.containsKey(cu)){
				//only non-anonymous subclasses may have noArgConstructors to modify - see bug 43444
				Set subtypes= (Set)namedSubclassMapping.get(cu);
				for (Iterator iter= subtypes.iterator(); iter.hasNext();) {
					IType subtype= (IType) iter.next();
					AbstractTypeDeclaration subtypeNode= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(subtype, cuRewrite.getRoot());
					if (subtypeNode != null)
						modifyImplicitCallsToNoArgConstructor(subtypeNode, cuRewrite);
				}
			}
			TextChange change= cuRewrite.createChange();
			if (change != null)
				fChangeManager.manage(cu, change);
		}
		
		pm.done();
		return fChangeManager;
	}
	
	//Map<IJavaScriptUnit, Set<IType>>
	private Map createNamedSubclassMapping(IProgressMonitor pm) throws JavaScriptModelException{
		IType[] subclasses= getCachedTypeHierarchy(new SubProgressMonitor(pm, 1)).getSubclasses(fMethod.getDeclaringType());
		Map result= new HashMap();
		for (int i= 0; i < subclasses.length; i++) {
			IType subclass= subclasses[i];
			if (subclass.isAnonymous())
				continue;
			IJavaScriptUnit cu= subclass.getJavaScriptUnit();
			if (! result.containsKey(cu))
				result.put(cu, new HashSet());
			((Set)result.get(cu)).add(subclass);
		}
		return result;
	}
	
	private void modifyImplicitCallsToNoArgConstructor(AbstractTypeDeclaration subclass, CompilationUnitRewrite cuRewrite) {
		FunctionDeclaration[] constructors= getAllConstructors(subclass);
		if (constructors.length == 0){
			addNewConstructorToSubclass(subclass, cuRewrite);
		} else {
			for (int i= 0; i < constructors.length; i++) {
				if (! containsImplicitCallToSuperConstructor(constructors[i]))
					continue;
				addExplicitSuperConstructorCall(constructors[i], cuRewrite);
			}
		}
	}
	
	private void addExplicitSuperConstructorCall(FunctionDeclaration constructor, CompilationUnitRewrite cuRewrite) {
		SuperConstructorInvocation superCall= constructor.getAST().newSuperConstructorInvocation();
		addArgumentsToNewSuperConstructorCall(superCall, cuRewrite);
		String msg= RefactoringCoreMessages.ChangeSignatureRefactoring_add_super_call; 
		TextEditGroup description= cuRewrite.createGroupDescription(msg);
		cuRewrite.getASTRewrite().getListRewrite(constructor.getBody(), Block.STATEMENTS_PROPERTY).insertFirst(superCall, description);
	}
	
	private void addArgumentsToNewSuperConstructorCall(SuperConstructorInvocation superCall, CompilationUnitRewrite cuRewrite) {
		int i= 0;
		for (Iterator iter= getNotDeletedInfos().iterator(); iter.hasNext(); i++) {
			ParameterInfo info= (ParameterInfo) iter.next();
			Expression newExpression= createNewExpression(info, getParameterInfos(), superCall.arguments(), cuRewrite, (FunctionDeclaration) ASTNodes.getParent(superCall, FunctionDeclaration.class));
			if (newExpression != null)
				superCall.arguments().add(newExpression);
		}
	}
	
	private static boolean containsImplicitCallToSuperConstructor(FunctionDeclaration constructor) {
		Assert.isTrue(constructor.isConstructor());
		Block body= constructor.getBody();
		if (body == null)
			return false;
		if (body.statements().size() == 0)
			return true;
		if (body.statements().get(0) instanceof ConstructorInvocation)
			return false;
		if (body.statements().get(0) instanceof SuperConstructorInvocation)
			return false;
		return true;
	}
	
	private void addNewConstructorToSubclass(AbstractTypeDeclaration subclass, CompilationUnitRewrite cuRewrite) {
		AST ast= subclass.getAST();
		FunctionDeclaration newConstructor= ast.newFunctionDeclaration();
		newConstructor.setName(ast.newSimpleName(subclass.getName().getIdentifier()));
		newConstructor.setConstructor(true);
		newConstructor.setExtraDimensions(0);
		newConstructor.setJavadoc(null);
		newConstructor.modifiers().addAll(ASTNodeFactory.newModifiers(ast, getAccessModifier(subclass)));
		newConstructor.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
		Block body= ast.newBlock();
		newConstructor.setBody(body);
		SuperConstructorInvocation superCall= ast.newSuperConstructorInvocation();
		addArgumentsToNewSuperConstructorCall(superCall, cuRewrite);
		body.statements().add(superCall);
		
		String msg= RefactoringCoreMessages.ChangeSignatureRefactoring_add_constructor; 
		TextEditGroup description= cuRewrite.createGroupDescription(msg);
		cuRewrite.getASTRewrite().getListRewrite(subclass, subclass.getBodyDeclarationsProperty()).insertFirst(newConstructor, description);
		
		// TODO use AbstractTypeDeclaration
	}
	
	private static int getAccessModifier(AbstractTypeDeclaration subclass) {
		int modifiers= subclass.getModifiers();
		if (Modifier.isPublic(modifiers))
			return Modifier.PUBLIC;
		else if (Modifier.isProtected(modifiers))
			return Modifier.PROTECTED;
		else if (Modifier.isPrivate(modifiers))
			return Modifier.PRIVATE;
		else
			return Modifier.NONE;
	}
	
	private FunctionDeclaration[] getAllConstructors(AbstractTypeDeclaration typeDeclaration) {
		BodyDeclaration decl;
		List result= new ArrayList(1);
		for (Iterator it = typeDeclaration.bodyDeclarations().listIterator(); it.hasNext(); ) {
			decl= (BodyDeclaration) it.next();
			if (decl instanceof FunctionDeclaration && ((FunctionDeclaration) decl).isConstructor())
				result.add(decl);
		}
		return (FunctionDeclaration[]) result.toArray(new FunctionDeclaration[result.size()]);
	}
	
	private boolean isNoArgConstructor() throws JavaScriptModelException {
		return fMethod.isConstructor() && fMethod.getNumberOfParameters() == 0;
	}
	
	private Expression createNewExpression(ParameterInfo info, List parameterInfos, List nodes, CompilationUnitRewrite cuRewrite, FunctionDeclaration method) {
		if (info.isNewVarargs() && info.getDefaultValue().trim().length() == 0)
			return null;
		else {
			if (fDefaultValueAdvisor == null)
				return (Expression) cuRewrite.getASTRewrite().createStringPlaceholder(info.getDefaultValue(), ASTNode.FUNCTION_INVOCATION);
			else
				return fDefaultValueAdvisor.createDefaultExpression(nodes, info, parameterInfos, method, false, cuRewrite);
		}
	}

	private boolean isVisibilitySameAsInitial() throws JavaScriptModelException {
		return fVisibility == JdtFlags.getVisibilityCode(fMethod);
	}
	
	private IJavaScriptSearchScope createRefactoringScope()  throws JavaScriptModelException{
		return RefactoringScopeFactory.create(fMethod);
	}
	
	private SearchResultGroup[] findOccurrences(IProgressMonitor pm, RefactoringStatus status) throws JavaScriptModelException{
		if (fMethod.isConstructor()){
			// workaround for bug 27236:
			return ConstructorReferenceFinder.getConstructorOccurrences(fMethod, pm, status);
		}else{	
			SearchPattern pattern= RefactoringSearchEngine.createOrPattern(fRippleMethods, IJavaScriptSearchConstants.ALL_OCCURRENCES);
			return RefactoringSearchEngine.search(pattern, createRefactoringScope(), pm, status);
		}
	}
	
	private static String createDeclarationString(ParameterInfo info) {
		if(JavaScriptCore.IS_ECMASCRIPT4) {
			String newTypeName= info.getNewTypeName();
			int index= newTypeName.indexOf('.');
			if (index != -1){
				newTypeName= newTypeName.substring(index+1);
			}
			return newTypeName + " " + info.getNewName(); //$NON-NLS-1$
		}else {
			return info.getNewName();
		}
	}
	
	private OccurrenceUpdate createOccurrenceUpdate(ASTNode node, CompilationUnitRewrite cuRewrite, RefactoringStatus result) {
		
		if (isReferenceNode(node))
			return new ReferenceUpdate(node, cuRewrite, result);
		
		else if (node instanceof SimpleName && node.getParent() instanceof FunctionDeclaration)
			return new DeclarationUpdate((FunctionDeclaration) node.getParent(), cuRewrite, result);
		
		else if (node instanceof MemberRef || node instanceof FunctionRef)
			return new DocReferenceUpdate(node, cuRewrite, result);
		
		else if (ASTNodes.getParent(node, ImportDeclaration.class) != null)
			return new StaticImportUpdate((ImportDeclaration) ASTNodes.getParent(node, ImportDeclaration.class), cuRewrite, result);
		
		else
			return new NullOccurrenceUpdate(node, cuRewrite, result);
	}

	private static boolean isReferenceNode(ASTNode node){
		switch (node.getNodeType()) {
			case ASTNode.FUNCTION_INVOCATION :
			case ASTNode.SUPER_METHOD_INVOCATION :
			case ASTNode.CLASS_INSTANCE_CREATION :
			case ASTNode.CONSTRUCTOR_INVOCATION :
			case ASTNode.SUPER_CONSTRUCTOR_INVOCATION :
				return true;

			default :
				return false;
		}
	}

	abstract class OccurrenceUpdate {
		protected final CompilationUnitRewrite fCuRewrite;
		protected final TextEditGroup fDescription;
		protected RefactoringStatus fResult;
		
		protected OccurrenceUpdate(CompilationUnitRewrite cuRewrite, TextEditGroup description, RefactoringStatus result) {
			fCuRewrite= cuRewrite;
			fDescription= description;
			fResult= result;
		}
		
		protected final ASTRewrite getASTRewrite() {
			return fCuRewrite.getASTRewrite();
		}

		protected final ImportRewrite getImportRewrite() {
			return fCuRewrite.getImportRewrite();
		}
		
		protected final ImportRemover getImportRemover() {
			return fCuRewrite.getImportRemover();
		}
		
		protected final CompilationUnitRewrite getCompilationUnitRewrite() {
			return fCuRewrite;
		}
		
		public abstract void updateNode() throws CoreException;
		
		protected void registerImportRemoveNode(ASTNode node) {
			getImportRemover().registerRemovedNode(node);
		}

		protected final void reshuffleElements() {
			if (isOrderSameAsInitial())
				return;
			
			//varargs; method(p1, p2, .., pn), call(a1, a2, .., ax) :
			// if (method_was_vararg) {
			//     assert fOldVarargIndex != -1
			//     if (vararg_retained) {
			//         assert vararg_is_last_non_deleted (pn)
			//         assert no_other_varargs
			//         => reshuffle [1..n-1] then append remaining nodes [n..x], possibly none
			//         
			//     } else (vararg_deleted) {
			//         assert all_are_non_vararg
			//         => reshuffle [1..n-1], drop all remaining nodes [n..x], possibly none
			//     }
			// 
			// } else if (method_became_vararg) {
			//     assert n == x
			//     assert fOldVarargIndex == -1
			//     => reshuffle [1..n]
			// 
			// } else (JLS2_case) {
			//     assert n == x
			//     assert fOldVarargIndex == -1
			//     => reshuffle [1..n]
			// }
			
			ListRewrite listRewrite= getParamgumentsRewrite();
			Map newOldMap= new LinkedHashMap();
			List nodes= listRewrite.getRewrittenList();
			Iterator rewriteIter= nodes.iterator();
			List original= listRewrite.getOriginalList();
			for (Iterator iter= original.iterator(); iter.hasNext();) {
				newOldMap.put(rewriteIter.next(),iter.next());
			}
			List newNodes= new ArrayList();
			// register removed nodes, and collect nodes in new sequence:
			for (int i= 0; i < fParameterInfos.size(); i++) {
				ParameterInfo info= (ParameterInfo) fParameterInfos.get(i);
				int oldIndex= info.getOldIndex();
				
				if (info.isDeleted()) {
					if (oldIndex != fOldVarargIndex) {
						registerImportRemoveNode((ASTNode) nodes.get(oldIndex));
					} else {
						//vararg deleted -> remove all remaining nodes:
						for (int n= oldIndex; n < nodes.size(); n++) {
							registerImportRemoveNode((ASTNode) nodes.get(n));
						}
					}
					
				} else if (info.isAdded()) {
					ASTNode newParamgument= createNewParamgument(info, fParameterInfos, nodes);
					if (newParamgument != null)
						newNodes.add(newParamgument);
					
				} else /* parameter stays */ {
					if (oldIndex != fOldVarargIndex) {
						ASTNode oldNode= (ASTNode) nodes.get(oldIndex);
						ASTNode movedNode= moveNode(oldNode, getASTRewrite());
						newNodes.add(movedNode);
					} else {
						//vararg stays and is last parameter -> copy all remaining nodes:
						for (int n= oldIndex; n < nodes.size(); n++) {
							ASTNode oldNode= (ASTNode) nodes.get(n);
							ASTNode movedNode= moveNode(oldNode, getASTRewrite());
							newNodes.add(movedNode);
						}
					}
				}
			}
			
			Iterator nodesIter= nodes.iterator();
			Iterator newIter= newNodes.iterator();
			//replace existing nodes with new ones:
			while (nodesIter.hasNext() && newIter.hasNext()) {
				ASTNode node= (ASTNode) nodesIter.next();
				ASTNode newNode= (ASTNode) newIter.next();
				if (!ASTNodes.isExistingNode(node)) //XXX:should better be addressed in ListRewriteEvent.replaceEntry(ASTNode, ASTNode)
					listRewrite.replace((ASTNode) newOldMap.get(node), newNode, fDescription);
				else
					listRewrite.replace(node, newNode, fDescription);
			}
			//remove remaining existing nodes:
			while (nodesIter.hasNext()) {
				ASTNode node= (ASTNode) nodesIter.next();
				if (!ASTNodes.isExistingNode(node))
					listRewrite.remove((ASTNode) newOldMap.get(node), fDescription);
				else
					listRewrite.remove(node, fDescription);
			}
			//add additional new nodes:
			while (newIter.hasNext()) {
				ASTNode node= (ASTNode) newIter.next();
				listRewrite.insertLast(node, fDescription);
			}
		}

		/**
		 * @return ListRewrite of parameters or arguments
		 */
		protected abstract ListRewrite getParamgumentsRewrite();
		
		protected final void changeParamguments() {
			for (Iterator iter= getParameterInfos().iterator(); iter.hasNext();) {
				ParameterInfo info= (ParameterInfo) iter.next();
				if (info.isAdded() || info.isDeleted())
					continue;
				
				if (info.isRenamed())
					changeParamgumentName(info);
		
				if (info.isTypeNameChanged())
					changeParamgumentType(info);
			}
		}

		protected void changeParamgumentName(ParameterInfo info) {
			// no-op
		}

		protected void changeParamgumentType(ParameterInfo info) {
			// no-op
		}

		protected final void replaceTypeNode(Type typeNode, String newTypeName, ITypeBinding newTypeBinding){
			Type newTypeNode= createNewTypeNode(newTypeName, newTypeBinding);
			getASTRewrite().replace(typeNode, newTypeNode, fDescription);
			registerImportRemoveNode(typeNode);
			getTightSourceRangeComputer().addTightSourceNode(typeNode);
		}
	
		/**
		 * @param info
		 * @param parameterInfos TODO
		 * @param nodes TODO
		 * @return a new method parameter or argument, or <code>null</code> for an empty vararg argument
		 */
		protected abstract ASTNode createNewParamgument(ParameterInfo info, List parameterInfos, List nodes);

		protected abstract SimpleName getMethodNameNode();

		protected final void changeMethodName() {
			if (! isMethodNameSameAsInitial()) {
				SimpleName nameNode= getMethodNameNode();
				SimpleName newNameNode= nameNode.getAST().newSimpleName(fMethodName);
				getASTRewrite().replace(nameNode, newNameNode, fDescription);
				registerImportRemoveNode(nameNode);
				getTightSourceRangeComputer().addTightSourceNode(nameNode);
			}
		}

		protected final Type createNewTypeNode(String newTypeName, ITypeBinding newTypeBinding) {
			Type newTypeNode;
			if (newTypeBinding == null) {
				if (fDefaultValueAdvisor != null)
					newTypeNode= fDefaultValueAdvisor.createType(newTypeName, getMethodNameNode().getStartPosition(), getCompilationUnitRewrite());
				else
					newTypeNode= (Type) getASTRewrite().createStringPlaceholder(newTypeName, ASTNode.SIMPLE_TYPE);
				//Don't import if not resolved.
			} else {
				newTypeNode= getImportRewrite().addImport(newTypeBinding, fCuRewrite.getAST());
				getImportRemover().registerAddedImports(newTypeNode);
			}
			return newTypeNode;
		}
		
		protected final TightSourceRangeComputer getTightSourceRangeComputer() {
			return (TightSourceRangeComputer) fCuRewrite.getASTRewrite().getExtendedSourceRangeComputer();
		}
	}
	
	class ReferenceUpdate extends OccurrenceUpdate {
		/** isReferenceNode(fNode) */
		private ASTNode fNode;

		protected ReferenceUpdate(ASTNode node, CompilationUnitRewrite cuRewrite, RefactoringStatus result) {
			super(cuRewrite, cuRewrite.createGroupDescription(RefactoringCoreMessages.ChangeSignatureRefactoring_update_reference), result); 
			fNode= node; //holds: Assert.isTrue(isReferenceNode(node));
		}

		public void updateNode() {
			reshuffleElements();
			changeMethodName();
		}
		
		/** @return {@inheritDoc} (element type: Expression) */
		protected ListRewrite getParamgumentsRewrite() {
			if (fNode instanceof FunctionInvocation)	
				return getASTRewrite().getListRewrite(fNode, FunctionInvocation.ARGUMENTS_PROPERTY);
				
			if (fNode instanceof SuperMethodInvocation)	
				return getASTRewrite().getListRewrite(fNode, SuperMethodInvocation.ARGUMENTS_PROPERTY);
				
			if (fNode instanceof ClassInstanceCreation)	
				return getASTRewrite().getListRewrite(fNode, ClassInstanceCreation.ARGUMENTS_PROPERTY);
				
			if (fNode instanceof ConstructorInvocation)	
				return getASTRewrite().getListRewrite(fNode, ConstructorInvocation.ARGUMENTS_PROPERTY);
				
			if (fNode instanceof SuperConstructorInvocation)	
				return getASTRewrite().getListRewrite(fNode, SuperConstructorInvocation.ARGUMENTS_PROPERTY);
			
			
			return null;
		}
		
		protected ASTNode createNewParamgument(ParameterInfo info, List parameterInfos, List nodes) {
			CompilationUnitRewrite cuRewrite= getCompilationUnitRewrite();
			FunctionDeclaration declaration= (FunctionDeclaration) ASTNodes.getParent(fNode, FunctionDeclaration.class);
			if (isRecursiveReference()) {
				return createNewExpressionRecursive(info, parameterInfos, nodes, cuRewrite, declaration);
			} else
				return createNewExpression(info, parameterInfos, nodes, cuRewrite, declaration);
		}

		private Expression createNewExpressionRecursive(ParameterInfo info, List parameterInfos, List nodes, CompilationUnitRewrite cuRewrite, FunctionDeclaration methodDeclaration) {
			if (fDefaultValueAdvisor != null && info.isAdded()) {
				return fDefaultValueAdvisor.createDefaultExpression(nodes, info, parameterInfos, methodDeclaration, true, cuRewrite);
			}
			return (Expression) getASTRewrite().createStringPlaceholder(info.getNewName(), ASTNode.FUNCTION_INVOCATION);
		}

		protected SimpleName getMethodNameNode() {
			if (fNode instanceof FunctionInvocation)	
				return ((FunctionInvocation)fNode).getName();
				
			if (fNode instanceof SuperMethodInvocation)	
				return ((SuperMethodInvocation)fNode).getName();
				
			return null;	
		}
		
		private boolean isRecursiveReference() {
			FunctionDeclaration enclosingMethodDeclaration= (FunctionDeclaration) ASTNodes.getParent(fNode, FunctionDeclaration.class);
			if (enclosingMethodDeclaration == null)
				return false;
			
			IFunctionBinding enclosingMethodBinding= enclosingMethodDeclaration.resolveBinding();
			if (enclosingMethodBinding == null)
				return false;
			
			if (fNode instanceof FunctionInvocation)	
				return enclosingMethodBinding == ((FunctionInvocation)fNode).resolveMethodBinding();
				
			if (fNode instanceof SuperMethodInvocation) {
				IFunctionBinding methodBinding= ((SuperMethodInvocation)fNode).resolveMethodBinding();
				return isSameMethod(methodBinding, enclosingMethodBinding);
			}
				
			if (fNode instanceof ClassInstanceCreation)	
				return enclosingMethodBinding == ((ClassInstanceCreation)fNode).resolveConstructorBinding();
				
			if (fNode instanceof ConstructorInvocation)	
				return enclosingMethodBinding == ((ConstructorInvocation)fNode).resolveConstructorBinding();
				
			if (fNode instanceof SuperConstructorInvocation) {
				return false; //Constructors don't override -> enclosing has not been changed -> no recursion
			}
			
			Assert.isTrue(false);
			return false;
		}
		
		/**
		 * @param m1 method 1
		 * @param m2 method 2
		 * @return true iff
		 * 		<ul><li>the methods are both constructors with same argument types, or</li>
		 *	 		<li>the methods have the same name and the same argument types</li></ul>
		 */
		private boolean isSameMethod(IFunctionBinding m1, IFunctionBinding m2) {
			if (m1.isConstructor()) {
				if (! m2.isConstructor())
					return false;
			} else {
				if (! m1.getName().equals(m2.getName()))
					return false;
			}
			
			ITypeBinding[] m1Parameters= m1.getParameterTypes();
			ITypeBinding[] m2Parameters= m2.getParameterTypes();
			if (m1Parameters.length != m2Parameters.length)
				return false;
			for (int i= 0; i < m1Parameters.length; i++) {
				if (m1Parameters[i].getErasure() != m2Parameters[i].getErasure())
					return false;
			}
			return true;
		}

	}

	class DeclarationUpdate extends OccurrenceUpdate {
		private FunctionDeclaration fMethDecl;

		protected DeclarationUpdate(FunctionDeclaration decl, CompilationUnitRewrite cuRewrite, RefactoringStatus result) {
			super(cuRewrite, cuRewrite.createGroupDescription(RefactoringCoreMessages.ChangeSignatureRefactoring_change_signature), result); 
			fMethDecl= decl;
		}
		
		// Prevent import removing if delegate is created.
		protected void registerImportRemoveNode(ASTNode node) {
			if (!fDelegateUpdating)
				super.registerImportRemoveNode(node);
		}

		public void updateNode() throws CoreException {
			changeParamguments();
			
			if (canChangeNameAndReturnType()) {
				changeMethodName();
				changeReturnType();
			}
					
			if (needsVisibilityUpdate())
				changeVisibility();
			reshuffleElements();
			changeExceptions();
			
			changeJavadocTags();
			
			if (fBodyUpdater == null || fBodyUpdater.needsParameterUsedCheck())
				checkIfDeletedParametersUsed();
			
			if (fBodyUpdater != null)
				fBodyUpdater.updateBody(fMethDecl, fCuRewrite, fResult);
			
			if (fDelegateUpdating)
				addDelegate();
		}

		private void addDelegate() throws JavaScriptModelException {

			DelegateMethodCreator creator= new DelegateMethodCreator();
			creator.setDeclaration(fMethDecl);
			creator.setDeclareDeprecated(fDelegateDeprecation);
			creator.setSourceRewrite(fCuRewrite);
			creator.prepareDelegate();
			
			/*
			 * The delegate now contains a call and a javadoc reference to the
			 * old method (i.e., to itself).
			 * 
			 * Use ReferenceUpdate() / DocReferenceUpdate() to update these
			 * references like any other reference.
			 */
			final ASTNode delegateInvocation= creator.getDelegateInvocation();
			if (delegateInvocation != null)
				// may be null if the delegate is an interface method or
				// abstract -> no body
				new ReferenceUpdate(delegateInvocation, creator.getDelegateRewrite(), fResult).updateNode();
			new DocReferenceUpdate(creator.getJavadocReference(), creator.getDelegateRewrite(), fResult).updateNode();
			
			creator.createEdit();
		}
		
		/** @return {@inheritDoc} (element type: SingleVariableDeclaration) */
		protected ListRewrite getParamgumentsRewrite() {
			return getASTRewrite().getListRewrite(fMethDecl, FunctionDeclaration.PARAMETERS_PROPERTY);
		}

		protected void changeParamgumentName(ParameterInfo info) {
			SingleVariableDeclaration param= (SingleVariableDeclaration) fMethDecl.parameters().get(info.getOldIndex());
			if (! info.getOldName().equals(param.getName().getIdentifier()))
				return; //don't change if original parameter name != name in rippleMethod
			
			String msg= RefactoringCoreMessages.ChangeSignatureRefactoring_update_parameter_references; 
			TextEditGroup description= fCuRewrite.createGroupDescription(msg);
			TempOccurrenceAnalyzer analyzer= new TempOccurrenceAnalyzer(param, false);
			analyzer.perform();
			SimpleName[] paramOccurrences= analyzer.getReferenceAndDeclarationNodes(); // @param tags are updated in changeJavaDocTags()
			for (int j= 0; j < paramOccurrences.length; j++) {
				SimpleName occurence= paramOccurrences[j];
				getASTRewrite().set(occurence, SimpleName.IDENTIFIER_PROPERTY, info.getNewName(), description);
			}
		}
		
		protected void changeParamgumentType(ParameterInfo info) {
			SingleVariableDeclaration oldParam= (SingleVariableDeclaration) fMethDecl.parameters().get(info.getOldIndex());
			getASTRewrite().set(oldParam, SingleVariableDeclaration.VARARGS_PROPERTY, Boolean.valueOf(info.isNewVarargs()), fDescription);
			replaceTypeNode(oldParam.getType(), ParameterInfo.stripEllipsis(info.getNewTypeName()), info.getNewTypeBinding());
			removeExtraDimensions(oldParam);
		}

		private void removeExtraDimensions(SingleVariableDeclaration oldParam) {
			if (oldParam.getExtraDimensions() != 0) {		
				getASTRewrite().set(oldParam, SingleVariableDeclaration.EXTRA_DIMENSIONS_PROPERTY, Integer.valueOf(0), fDescription);
			}
		}
	
		private void changeReturnType() throws JavaScriptModelException {
		    if (isReturnTypeSameAsInitial())
		    	return;
			replaceTypeNode(fMethDecl.getReturnType2(), fReturnTypeInfo.getNewTypeName(), fReturnTypeInfo.getNewTypeBinding());
	        removeExtraDimensions(fMethDecl);
	    	//Remove expression from return statement when changed to void? No, would lose information!
	    	//Could add return statement with default value and add todo comment, but compile error is better.
		}
	
		private void removeExtraDimensions(FunctionDeclaration methDecl) {
			if (methDecl.getExtraDimensions() != 0)
				getASTRewrite().set(methDecl, FunctionDeclaration.EXTRA_DIMENSIONS_PROPERTY, Integer.valueOf(0), fDescription);
		}

		private boolean needsVisibilityUpdate() throws JavaScriptModelException {
			if (isVisibilitySameAsInitial())
				return false;
			if (isIncreasingVisibility())
				return JdtFlags.isHigherVisibility(fVisibility, JdtFlags.getVisibilityCode(fMethDecl));
			else
				return JdtFlags.isHigherVisibility(JdtFlags.getVisibilityCode(fMethDecl), fVisibility);
		}
		
		private boolean isIncreasingVisibility() throws JavaScriptModelException{
			return JdtFlags.isHigherVisibility(fVisibility, JdtFlags.getVisibilityCode(fMethod));
		}
		
		private void changeVisibility() {
			ModifierRewrite.create(getASTRewrite(), fMethDecl).setVisibility(fVisibility, fDescription);
		}
	
		private void changeExceptions() {
			for (Iterator iter= fExceptionInfos.iterator(); iter.hasNext();) {
				ExceptionInfo info= (ExceptionInfo) iter.next();
				if (info.isOld())
					continue;
				if (info.isDeleted())
					removeExceptionFromNodeList(info, fMethDecl.thrownExceptions());
				else
					addExceptionToNodeList(info, getASTRewrite().getListRewrite(fMethDecl, FunctionDeclaration.THROWN_EXCEPTIONS_PROPERTY));
			}
		}
		
		private void removeExceptionFromNodeList(ExceptionInfo toRemove, List exceptionsNodeList) {
			ITypeBinding typeToRemove= toRemove.getTypeBinding();
			for (Iterator iter= exceptionsNodeList.iterator(); iter.hasNext(); ) {
				Name currentName= (Name) iter.next();
				ITypeBinding currentType= currentName.resolveTypeBinding();
				/* Maybe remove all subclasses of typeToRemove too.
				 * Problem:
				 * - B extends A;
				 * - A.m() throws IOException, Exception;
				 * - B.m() throws IOException, AWTException;
				 * Removing Exception should remove AWTException,
				 * but NOT remove IOException (or a subclass of JavaScriptModelException). */
				 // if (Bindings.isSuperType(typeToRemove, currentType))
				if (currentType == null)
					continue; // newly added or unresolvable type
				if (Bindings.equals(currentType, typeToRemove) || toRemove.getType().getElementName().equals(currentType.getName())) {
					getASTRewrite().remove(currentName, fDescription);
					registerImportRemoveNode(currentName);
				}
			}
		}
	
		private void addExceptionToNodeList(ExceptionInfo exceptionInfo, ListRewrite exceptionListRewrite) {
			String fullyQualified= JavaModelUtil.getFullyQualifiedName(exceptionInfo.getType());
			for (Iterator iter= exceptionListRewrite.getOriginalList().iterator(); iter.hasNext(); ) {
				Name exName= (Name) iter.next();
				//XXX: existing superclasses of the added exception are redundant and could be removed
				ITypeBinding typeBinding= exName.resolveTypeBinding();
				if (typeBinding == null)
					continue; // newly added or unresolvable type
				if (typeBinding.getQualifiedName().equals(fullyQualified))
					return; // don't add it again
			}
			String importedType= getImportRewrite().addImport(JavaModelUtil.getFullyQualifiedName(exceptionInfo.getType()));
			getImportRemover().registerAddedImport(importedType);
			ASTNode exNode= getASTRewrite().createStringPlaceholder(importedType, ASTNode.SIMPLE_NAME);
			exceptionListRewrite.insertLast(exNode, fDescription);
		}
		
		private void changeJavadocTags() throws JavaScriptModelException {
			//update tags in javadoc: @param, @return, @exception, @throws, ...
			JSdoc javadoc= fMethDecl.getJavadoc();
			if (javadoc == null)
				return;

			ITypeBinding typeBinding= Bindings.getBindingOfParentType(fMethDecl);
			if (typeBinding == null)
				return;
			IFunctionBinding methodBinding= fMethDecl.resolveBinding();
			if (methodBinding == null)
				return;
				
			boolean isTopOfRipple= (Bindings.findOverriddenMethod(methodBinding, false) == null);
			//add tags: only iff top of ripple; change and remove: always.
			//TODO: should have preference for adding tags in (overriding) methods (with template: todo, inheritDoc, ...)
			
			List tags= javadoc.tags(); // List of TagElement
			ListRewrite tagsRewrite= getASTRewrite().getListRewrite(javadoc, JSdoc.TAGS_PROPERTY);

			if (! isReturnTypeSameAsInitial()) {
				if (PrimitiveType.VOID.toString().equals(fReturnTypeInfo.getNewTypeName())) {
					for (int i = 0; i < tags.size(); i++) {
						TagElement tag= (TagElement) tags.get(i);
						if (TagElement.TAG_RETURN.equals(tag.getTagName())) {
							getASTRewrite().remove(tag, fDescription);
							registerImportRemoveNode(tag);
						}
					}
				} else if (isTopOfRipple && Signature.SIG_VOID.equals(fMethod.getReturnType())){
					TagElement returnNode= createReturnTag();
					TagElement previousTag= findTagElementToInsertAfter(tags, TagElement.TAG_RETURN);
					insertTag(returnNode, previousTag, tagsRewrite);
					tags= tagsRewrite.getRewrittenList();
				}
			}
			
			if (! (areNamesSameAsInitial() && isOrderSameAsInitial())) {
				ArrayList paramTags= new ArrayList(); // <TagElement>, only not deleted tags with simpleName
				// delete & rename:
				for (Iterator iter = tags.iterator(); iter.hasNext(); ) {
					TagElement tag = (TagElement) iter.next();
					String tagName= tag.getTagName();
					List fragments= tag.fragments();
					if (! (TagElement.TAG_PARAM.equals(tagName) && fragments.size() > 0 && fragments.get(0) instanceof SimpleName))
						continue;
					SimpleName simpleName= (SimpleName) fragments.get(0);
					String identifier= simpleName.getIdentifier();
					boolean removed= false;
					for (int i= 0; i < fParameterInfos.size(); i++) {
						ParameterInfo info= (ParameterInfo) fParameterInfos.get(i);
						if (identifier.equals(info.getOldName())) {
							if (info.isDeleted()) {
								getASTRewrite().remove(tag, fDescription);
								registerImportRemoveNode(tag);
								removed= true;
							} else if (info.isRenamed()) {
								SimpleName newName= simpleName.getAST().newSimpleName(info.getNewName());
								getASTRewrite().replace(simpleName, newName, fDescription);
								registerImportRemoveNode(tag);
							}
							break;
						}
					}
					if (! removed)
						paramTags.add(tag);
				}
				tags= tagsRewrite.getRewrittenList();

				if (! isOrderSameAsInitial()) {
					// reshuffle (sort in declaration sequence) & add (only add to top of ripple):
					TagElement previousTag= findTagElementToInsertAfter(tags, TagElement.TAG_PARAM);
					boolean first= true; // workaround for bug 92111: preserve first tag if possible
					// reshuffle:
					for (Iterator infoIter= fParameterInfos.iterator(); infoIter.hasNext();) {
						ParameterInfo info= (ParameterInfo) infoIter.next();
						String oldName= info.getOldName();
						String newName= info.getNewName();
						if (info.isAdded()) {
							first= false;
							if (! isTopOfRipple)
								continue;
							TagElement paramNode= JavadocUtil.createParamTag(newName, fCuRewrite.getRoot().getAST(), fCuRewrite.getCu().getJavaScriptProject());
							insertTag(paramNode, previousTag, tagsRewrite);
							previousTag= paramNode;
						} else {
							for (Iterator tagIter= paramTags.iterator(); tagIter.hasNext();) {
								TagElement tag= (TagElement) tagIter.next();
								SimpleName tagName= (SimpleName) tag.fragments().get(0);
								if (oldName.equals(tagName.getIdentifier())) {
									tagIter.remove();
									if (first) {
										previousTag= tag;
									} else {
										TagElement movedTag= (TagElement) getASTRewrite().createMoveTarget(tag);
										getASTRewrite().remove(tag, fDescription);
										insertTag(movedTag, previousTag, tagsRewrite);
										previousTag= movedTag;
									}
								}
								first= false;
							}
						}
					}
					// params with bad names:
					for (Iterator iter= paramTags.iterator(); iter.hasNext();) {
						TagElement tag= (TagElement) iter.next();
						TagElement movedTag= (TagElement) getASTRewrite().createMoveTarget(tag);
						getASTRewrite().remove(tag, fDescription);
						insertTag(movedTag, previousTag, tagsRewrite);
						previousTag= movedTag;
					}
				}
				tags= tagsRewrite.getRewrittenList();
			}
			
			if (! areExceptionsSameAsInitial()) {
				// collect exceptionTags and remove deleted:
				ArrayList exceptionTags= new ArrayList(); // <TagElement>, only not deleted tags with name
				for (int i= 0; i < tags.size(); i++) {
					TagElement tag= (TagElement) tags.get(i);
					if (! TagElement.TAG_THROWS.equals(tag.getTagName()) && ! TagElement.TAG_EXCEPTION.equals(tag.getTagName()))
						continue;
					if (! (tag.fragments().size() > 0 && tag.fragments().get(0) instanceof Name))
						continue;
					boolean tagDeleted= false;
					Name name= (Name) tag.fragments().get(0);
					for (int j= 0; j < fExceptionInfos.size(); j++) {
						ExceptionInfo info= (ExceptionInfo) fExceptionInfos.get(j);
						if (info.isDeleted()) {
							boolean remove= false;
							final ITypeBinding nameBinding= name.resolveTypeBinding();
							if (nameBinding != null) {
								final ITypeBinding infoBinding= info.getTypeBinding();
								if (infoBinding != null && Bindings.equals(infoBinding, nameBinding))
									remove= true;
								else if (info.getType().getElementName().equals(nameBinding.getName()))
									remove= true;
								if (remove) {
									getASTRewrite().remove(tag, fDescription);
									registerImportRemoveNode(tag);
									tagDeleted= true;
									break;
								}
							}
						}
					}
					if (! tagDeleted)
						exceptionTags.add(tag);
				}
				// reshuffle:
				tags= tagsRewrite.getRewrittenList();
				TagElement previousTag= findTagElementToInsertAfter(tags, TagElement.TAG_THROWS);
				for (Iterator infoIter= fExceptionInfos.iterator(); infoIter.hasNext();) {
					ExceptionInfo info= (ExceptionInfo) infoIter.next();
					if (info.isAdded()) {
						if (!isTopOfRipple)
							continue;
						TagElement excptNode= createExceptionTag(info.getType().getElementName());
						insertTag(excptNode, previousTag, tagsRewrite);
						previousTag= excptNode;
					} else {
						for (Iterator tagIter= exceptionTags.iterator(); tagIter.hasNext();) {
							TagElement tag= (TagElement) tagIter.next();
							Name tagName= (Name) tag.fragments().get(0);
							final ITypeBinding nameBinding= tagName.resolveTypeBinding();
							if (nameBinding != null) {
								boolean process= false;
								final ITypeBinding infoBinding= info.getTypeBinding();
								if (infoBinding != null && Bindings.equals(infoBinding, nameBinding))
									process= true;
								else if (info.getType().getElementName().equals(nameBinding.getName()))
									process= true;
								if (process) {
									tagIter.remove();
									TagElement movedTag= (TagElement) getASTRewrite().createMoveTarget(tag);
									getASTRewrite().remove(tag, fDescription);
									insertTag(movedTag, previousTag, tagsRewrite);
									previousTag= movedTag;
								}
							}
						}
					}
				}
				// exceptions with bad names:
				for (Iterator iter= exceptionTags.iterator(); iter.hasNext();) {
					TagElement tag= (TagElement) iter.next();
					TagElement movedTag= (TagElement) getASTRewrite().createMoveTarget(tag);
					getASTRewrite().remove(tag, fDescription);
					insertTag(movedTag, previousTag, tagsRewrite);
					previousTag= movedTag;
				}
			}
		}

		private TagElement createReturnTag() {
			TagElement returnNode= getASTRewrite().getAST().newTagElement();
			returnNode.setTagName(TagElement.TAG_RETURN);
			
			TextElement textElement= getASTRewrite().getAST().newTextElement();
			String text= StubUtility.getTodoTaskTag(fCuRewrite.getCu().getJavaScriptProject());
			if (text != null)
				textElement.setText(text); //TODO: use template with {@todo} ...
			returnNode.fragments().add(textElement);
			
			return returnNode;
		}

		private TagElement createExceptionTag(String simpleName) {
			TagElement excptNode= getASTRewrite().getAST().newTagElement();
			excptNode.setTagName(TagElement.TAG_THROWS);

			SimpleName nameNode= getASTRewrite().getAST().newSimpleName(simpleName);
			excptNode.fragments().add(nameNode);

			TextElement textElement= getASTRewrite().getAST().newTextElement();
			String text= StubUtility.getTodoTaskTag(fCuRewrite.getCu().getJavaScriptProject());
			if (text != null)
				textElement.setText(text); //TODO: use template with {@todo} ...
			excptNode.fragments().add(textElement);
			
			return excptNode;
		}

		private void insertTag(TagElement tag, TagElement previousTag, ListRewrite tagsRewrite) {
			if (previousTag == null)
				tagsRewrite.insertFirst(tag, fDescription);
			else
				tagsRewrite.insertAfter(tag, previousTag, fDescription);
		}

		/**
		 * @param tags existing tags
		 * @param tagName name of tag to add
		 * @return the <code>TagElement<code> just before a new <code>TagElement</code> with name <code>tagName</code>,
		 *   or <code>null</code>.
		 */
		private TagElement findTagElementToInsertAfter(List tags, String tagName) {
			List tagOrder= Arrays.asList(new String[] {
					TagElement.TAG_AUTHOR,
					TagElement.TAG_VERSION,
					TagElement.TAG_PARAM,
					TagElement.TAG_RETURN,
					TagElement.TAG_THROWS,
					TagElement.TAG_EXCEPTION,
					TagElement.TAG_SEE,
					TagElement.TAG_SINCE,
					TagElement.TAG_SERIAL,
					TagElement.TAG_SERIALFIELD,
					TagElement.TAG_SERIALDATA,
					TagElement.TAG_DEPRECATED,
					TagElement.TAG_VALUE
			});
			int goalOrdinal= tagOrder.indexOf(tagName);
			if (goalOrdinal == -1) // unknown tag -> to end
				return (tags.size() == 0) ? null : (TagElement) tags.get(tags.size());
			for (int i= 0; i < tags.size(); i++) {
				int tagOrdinal= tagOrder.indexOf(((TagElement) tags.get(i)).getTagName());
				if (tagOrdinal >= goalOrdinal)
					return (i == 0) ? null : (TagElement) tags.get(i-1);
			}
			return (tags.size() == 0) ? null : (TagElement) tags.get(tags.size()-1);
		}

		//TODO: already reported as compilation error -> don't report there?
		private void checkIfDeletedParametersUsed() {
			for (Iterator iter= getDeletedInfos().iterator(); iter.hasNext();) {
				ParameterInfo info= (ParameterInfo) iter.next();
				SingleVariableDeclaration paramDecl= (SingleVariableDeclaration) fMethDecl.parameters().get(info.getOldIndex());
				TempOccurrenceAnalyzer analyzer= new TempOccurrenceAnalyzer(paramDecl, false);
				analyzer.perform();
				SimpleName[] paramRefs= analyzer.getReferenceNodes();

				if (paramRefs.length > 0){
					RefactoringStatusContext context= JavaStatusContext.create(fCuRewrite.getCu(), paramRefs[0]);
					String typeName= getFullTypeName(fMethDecl);
					Object[] keys= new String[]{paramDecl.getName().getIdentifier(),
												fMethDecl.getName().getIdentifier(),
												typeName};
					String msg= Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_parameter_used, keys); 
					fResult.addError(msg, context);
				}
			}	
		}
		
		private String getFullTypeName(FunctionDeclaration decl) {
			ASTNode node= decl;
			while (true) {
				node= node.getParent();
				if (node instanceof AbstractTypeDeclaration) {
					return ((AbstractTypeDeclaration) node).getName().getIdentifier();
				} else if (node instanceof ClassInstanceCreation) {
					ClassInstanceCreation cic= (ClassInstanceCreation) node;
					return Messages.format(RefactoringCoreMessages.ChangeSignatureRefactoring_anonymous_subclass, new String[]{ASTNodes.asString(cic.getType())}); 
				}
			}
		}
		
		protected ASTNode createNewParamgument(ParameterInfo info, List parameterInfos, List nodes) {
			return createNewSingleVariableDeclaration(info);	
		}
	
		private SingleVariableDeclaration createNewSingleVariableDeclaration(ParameterInfo info) {
			SingleVariableDeclaration newP= getASTRewrite().getAST().newSingleVariableDeclaration();
			newP.setName(getASTRewrite().getAST().newSimpleName(info.getNewName()));
			newP.setType(createNewTypeNode(ParameterInfo.stripEllipsis(info.getNewTypeName()), info.getNewTypeBinding()));
			newP.setVarargs(info.isNewVarargs());
			return newP;
		}
		
		protected SimpleName getMethodNameNode() {
			return fMethDecl.getName();
		}
	
	}

	class DocReferenceUpdate extends OccurrenceUpdate {
		/** instanceof MemberRef || FunctionRef */
		private ASTNode fNode;

		protected DocReferenceUpdate(ASTNode node, CompilationUnitRewrite cuRewrite, RefactoringStatus result) {
			super(cuRewrite, cuRewrite.createGroupDescription(RefactoringCoreMessages.ChangeSignatureRefactoring_update_javadoc_reference), result); 
			fNode= node;
		}

		public void updateNode() {
			if (fNode instanceof FunctionRef) {
				changeParamguments();
				reshuffleElements();
			}
			if (canChangeNameAndReturnType())
				changeMethodName();
		}
		
		protected ASTNode createNewParamgument(ParameterInfo info, List parameterInfos, List nodes) {
			return createNewMethodRefParameter(info);
		}
		
		private FunctionRefParameter createNewMethodRefParameter(ParameterInfo info) {
			FunctionRefParameter newP= getASTRewrite().getAST().newFunctionRefParameter();
			
			// only add name iff first parameter already has a name:
			List parameters= getParamgumentsRewrite().getOriginalList();
			if (parameters.size() > 0)
				if (((FunctionRefParameter) parameters.get(0)).getName() != null)
					newP.setName(getASTRewrite().getAST().newSimpleName(info.getNewName()));
			
			newP.setType(createNewDocRefType(info));
			newP.setVarargs(info.isNewVarargs());
			return newP;
		}

		private Type createNewDocRefType(ParameterInfo info) {
			String newTypeName= ParameterInfo.stripEllipsis(info.getNewTypeName());
			ITypeBinding newTypeBinding= info.getNewTypeBinding();
			if (newTypeBinding != null)
				newTypeBinding= newTypeBinding.getErasure(); //see bug 83127: Javadoc references are raw (erasures)
			return createNewTypeNode(newTypeName, newTypeBinding);
		}

		protected SimpleName getMethodNameNode() {
			if (fNode instanceof MemberRef)
				return ((MemberRef) fNode).getName();
			
			if (fNode instanceof FunctionRef)
				return ((FunctionRef) fNode).getName();
			
			return null;	
		}
		
		/** @return {@inheritDoc} (element type: FunctionRefParameter) */
		protected ListRewrite getParamgumentsRewrite() {
			return getASTRewrite().getListRewrite(fNode, FunctionRef.PARAMETERS_PROPERTY);
		}

		protected void changeParamgumentName(ParameterInfo info) {
			if (! (fNode instanceof FunctionRef))
				return;

			FunctionRefParameter oldParam= (FunctionRefParameter) ((FunctionRef) fNode).parameters().get(info.getOldIndex());
			SimpleName oldParamName= oldParam.getName();
			if (oldParamName != null)
				getASTRewrite().set(oldParamName, SimpleName.IDENTIFIER_PROPERTY, info.getNewName(), fDescription);
		}
		
		protected void changeParamgumentType(ParameterInfo info) {
			if (! (fNode instanceof FunctionRef))
				return;
			
			FunctionRefParameter oldParam= (FunctionRefParameter) ((FunctionRef) fNode).parameters().get(info.getOldIndex());
			Type oldTypeNode= oldParam.getType();
			Type newTypeNode= createNewDocRefType(info);
			if (info.isNewVarargs()) {
				if (info.isOldVarargs() && ! oldParam.isVarargs()) {
					// leave as array reference of old reference was not vararg
					newTypeNode= getASTRewrite().getAST().newArrayType(newTypeNode);
				} else {
					getASTRewrite().set(oldParam, FunctionRefParameter.VARARGS_PROPERTY, Boolean.TRUE, fDescription);
				}
			} else {
				if (oldParam.isVarargs()) {
					getASTRewrite().set(oldParam, FunctionRefParameter.VARARGS_PROPERTY, Boolean.FALSE, fDescription);
				}
			}
			
			getASTRewrite().replace(oldTypeNode, newTypeNode, fDescription);
			registerImportRemoveNode(oldTypeNode);
		}
	}
	
	class StaticImportUpdate extends OccurrenceUpdate {

		private final ImportDeclaration fImportDecl;

		public StaticImportUpdate(ImportDeclaration importDecl, CompilationUnitRewrite cuRewrite, RefactoringStatus result) {
			super(cuRewrite, null, result);
			fImportDecl= importDecl;
		}

		public void updateNode() throws JavaScriptModelException {
			ImportRewrite importRewrite= fCuRewrite.getImportRewrite();
			QualifiedName name= (QualifiedName) fImportDecl.getName();
			//will be removed by importRemover if not used elsewhere ... importRewrite.removeStaticImport(name.getFullyQualifiedName());
			importRewrite.addStaticImport(name.getQualifier().getFullyQualifiedName(), fMethodName, false);
		}

		protected ListRewrite getParamgumentsRewrite() {
			return null;
		}

		protected ASTNode createNewParamgument(ParameterInfo info, List parameterInfos, List nodes) {
			return null;
		}

		protected SimpleName getMethodNameNode() {
			return null;
		}
	}
	
	class NullOccurrenceUpdate extends OccurrenceUpdate {
		private ASTNode fNode;
		protected NullOccurrenceUpdate(ASTNode node, CompilationUnitRewrite cuRewrite, RefactoringStatus result) {
			super(cuRewrite, null, result);
			fNode= node;
		}
		public void updateNode() throws JavaScriptModelException {
			int start= fNode.getStartPosition();
			int length= fNode.getLength();
			String msg= "Cannot update found node: nodeType=" + fNode.getNodeType() + "; "  //$NON-NLS-1$//$NON-NLS-2$
					+ fNode.toString() + "[" + start + ", " + length + "]";  //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			JavaScriptPlugin.log(new Exception(msg + ":\n" + fCuRewrite.getCu().getSource().substring(start, start + length))); //$NON-NLS-1$
			fResult.addError(msg, JavaStatusContext.create(fCuRewrite.getCu(), fNode));
		}
		protected ListRewrite getParamgumentsRewrite() {
			return null;
		}
		protected ASTNode createNewParamgument(ParameterInfo info, List parameterInfos, List nodes) {
			return null;
		}
		protected SimpleName getMethodNameNode() {
			return null;
		}
	}

	public RefactoringStatus initialize(final RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			final String handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists() || element.getElementType() != IJavaScriptElement.METHOD)
					return createInputFatalStatus(element, IJavaScriptRefactorings.CHANGE_METHOD_SIGNATURE);
				else {
					fMethod= (IFunction) element;
					fMethodName= fMethod.getElementName();
					try {
						fVisibility= JdtFlags.getVisibilityCode(fMethod);
						fReturnTypeInfo= new ReturnTypeInfo(Signature.toString(Signature.getReturnType(fMethod.getSignature())));
					} catch (JavaScriptModelException exception) {
						return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, ATTRIBUTE_VISIBILITY));
					}
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
			final String name= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_NAME);
			if (name != null) {
				fMethodName= name;
				final RefactoringStatus status= Checks.checkMethodName(fMethodName);
				if (status.hasError())
					return status;
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_NAME));
			final String type= extended.getAttribute(ATTRIBUTE_RETURN);
			if (type != null && !"".equals(type)) //$NON-NLS-1$
				fReturnTypeInfo= new ReturnTypeInfo(type);
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
			int count= 1;
			String attribute= ATTRIBUTE_PARAMETER + count;
			String value= null;
			fParameterInfos= new ArrayList(3);
			while ((value= extended.getAttribute(attribute)) != null) {
				StringTokenizer tokenizer= new StringTokenizer(value);
				if (tokenizer.countTokens() < 6)
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, ATTRIBUTE_PARAMETER));
				String oldTypeName= tokenizer.nextToken();
				String oldName= tokenizer.nextToken();
				String oldIndex= tokenizer.nextToken();
				String newTypeName= tokenizer.nextToken();
				String newName= tokenizer.nextToken();
				String deleted= tokenizer.nextToken();
				ParameterInfo info= null;
				try {
					info= new ParameterInfo(oldTypeName, oldName, Integer.valueOf(oldIndex).intValue());
					info.setNewTypeName(newTypeName);
					info.setNewName(newName);
					if (Boolean.valueOf(deleted).booleanValue())
						info.markAsDeleted();
					fParameterInfos.add(info);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, ATTRIBUTE_PARAMETER));
				}
				final String result= extended.getAttribute(ATTRIBUTE_DEFAULT + count);
				if (result != null && !"".equals(result)) //$NON-NLS-1$
					info.setDefaultValue(result);
				count++;
				attribute= ATTRIBUTE_PARAMETER + count;
			}
			count= 1;
			fExceptionInfos= new ArrayList(2);
			attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + count;
			while ((value= extended.getAttribute(attribute)) != null) {
				ExceptionInfo info= null;
				final String kind= extended.getAttribute(ATTRIBUTE_KIND + count);
				if (kind != null) {
					final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), value, false);
					if (element == null || !element.exists())
						return createInputFatalStatus(element, IJavaScriptRefactorings.CHANGE_METHOD_SIGNATURE);
					else {
						try {
							info= new ExceptionInfo((IType) element, Integer.valueOf(kind).intValue(), null);
						} catch (NumberFormatException exception) {
							return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, ATTRIBUTE_KIND));
						}
					}
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, ATTRIBUTE_KIND));
				fExceptionInfos.add(info);
				count++;
				attribute= JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + count;
			}
			final String deprecate= extended.getAttribute(ATTRIBUTE_DEPRECATE);
			if (deprecate != null) {
				fDelegateDeprecation= Boolean.valueOf(deprecate).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DEPRECATE));
			final String delegate= extended.getAttribute(ATTRIBUTE_DELEGATE);
			if (delegate != null) {
				fDelegateUpdating= Boolean.valueOf(delegate).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DELEGATE));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}

	/**
	 * If this occurrence update is called from within a declaration update
	 * (i.e., to update the call inside the newly created delegate), the old
	 * node does not yet exist and therefore cannot be a move target.
	 * 
	 * Normally, always use createMoveTarget as this has the advantage of
	 * being able to add changes inside changed nodes (for example, a method
	 * call within a method call, see test case #4) and preserving comments
	 * inside calls.
	 * @param oldNode original node
	 * @param rewrite an AST rewrite
	 * @return the node to insert at the target location
	 */
	protected ASTNode moveNode(ASTNode oldNode, ASTRewrite rewrite) {
		ASTNode movedNode;
		if (ASTNodes.isExistingNode(oldNode))
			movedNode= rewrite.createMoveTarget(oldNode); //node must be one of ast
		else
			movedNode= ASTNode.copySubtree(rewrite.getAST(), oldNode);
		return movedNode;
	}
	
	public String getDelegateUpdatingTitle(boolean plural) {
		if (plural)
			return RefactoringCoreMessages.DelegateCreator_keep_original_changed_plural;
		else
			return RefactoringCoreMessages.DelegateCreator_keep_original_changed_singular;
	}

	public IDefaultValueAdvisor getDefaultValueAdvisor() {
		return fDefaultValueAdvisor;
	}

	public void setDefaultValueAdvisor(IDefaultValueAdvisor defaultValueAdvisor) {
		fDefaultValueAdvisor= defaultValueAdvisor;
	}
}
