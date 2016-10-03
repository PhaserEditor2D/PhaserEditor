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
package org.eclipse.wst.jsdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.Initializer;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.NullLiteral;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SwitchCase;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.JavaScriptRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.Corext;
import org.eclipse.wst.jsdt.internal.corext.SourceRange;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.dom.fragments.ASTFragmentFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.fragments.IASTFragment;
import org.eclipse.wst.jsdt.internal.corext.dom.fragments.IExpressionFragment;
import org.eclipse.wst.jsdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.wst.jsdt.internal.corext.fix.LinkedProposalPositionGroup;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStringStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.RefactoringDescriptorChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RefactoringAnalyzeUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.wst.jsdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.wst.jsdt.internal.ui.text.correction.ModifierCorrectionSubProcessor;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.BindingLabelProvider;
import org.eclipse.wst.jsdt.ui.CodeGeneration;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

public class ExtractConstantRefactoring extends ScriptableRefactoring {

	private static final String ATTRIBUTE_REPLACE= "replace"; //$NON-NLS-1$
	private static final String ATTRIBUTE_QUALIFY= "qualify"; //$NON-NLS-1$
	private static final String ATTRIBUTE_VISIBILITY= "visibility"; //$NON-NLS-1$

	private static final String MODIFIER= "static final"; //$NON-NLS-1$
	
	private static final String KEY_NAME= "name"; //$NON-NLS-1$
	private static final String KEY_TYPE= "type"; //$NON-NLS-1$
	
	private CompilationUnitRewrite fCuRewrite;
	private int fSelectionStart;
	private int fSelectionLength;
	private IJavaScriptUnit fCu;

	private IExpressionFragment fSelectedExpression;
	private Type fConstantTypeCache;
	private boolean fReplaceAllOccurrences= true; //default value
	private boolean fQualifyReferencesWithDeclaringClassName= false;	//default value

	private String fVisibility= JdtFlags.VISIBILITY_STRING_PRIVATE; //default value
	private boolean fTargetIsInterface= false;
	private String fConstantName;
	private String[] fExcludedVariableNames;

	private boolean fSelectionAllStaticFinal;
	private boolean fAllStaticFinalCheckPerformed= false;
	
	private List fBodyDeclarations;
	
	//Constant Declaration Location
	private BodyDeclaration fToInsertAfter;
	private boolean fInsertFirst;
	
	private CompilationUnitChange fChange;
	private String[] fGuessedConstNames;
	
	private LinkedProposalModel fLinkedProposalModel;

	/**
	 * Creates a new extract constant refactoring
	 * @param unit the compilation unit, or <code>null</code> if invoked by scripting
	 * @param selectionStart
	 * @param selectionLength
	 */
	public ExtractConstantRefactoring(IJavaScriptUnit unit, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= unit;
		fCuRewrite= null;
		fLinkedProposalModel= null;
		fConstantName= ""; //$NON-NLS-1$
	}
	
	public ExtractConstantRefactoring(JavaScriptUnit astRoot, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(astRoot.getTypeRoot() instanceof IJavaScriptUnit);
		
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= (IJavaScriptUnit) astRoot.getTypeRoot();
		fCuRewrite= new CompilationUnitRewrite(fCu, astRoot);
		fLinkedProposalModel= null;
		fConstantName= ""; //$NON-NLS-1$
	}
		
	public void setLinkedProposalModel(LinkedProposalModel linkedProposalModel) {
		fLinkedProposalModel= linkedProposalModel;
	}
	
	public String getName() {
		return RefactoringCoreMessages.ExtractConstantRefactoring_name; 
	}

	public boolean replaceAllOccurrences() {
		return fReplaceAllOccurrences;
	}

	public void setReplaceAllOccurrences(boolean replaceAllOccurrences) {
		fReplaceAllOccurrences= replaceAllOccurrences;
	}
	
	public void setVisibility(String am) {
		Assert.isTrue(
			am == JdtFlags.VISIBILITY_STRING_PRIVATE || am == JdtFlags.VISIBILITY_STRING_PROTECTED || am == JdtFlags.VISIBILITY_STRING_PACKAGE || am == JdtFlags.VISIBILITY_STRING_PUBLIC
		);
		fVisibility= am;
	}
	
	public String getVisibility() {
		return fVisibility;	
	}
	
	public boolean getTargetIsInterface() {
		return fTargetIsInterface;
	}

	public boolean qualifyReferencesWithDeclaringClassName() {
		return fQualifyReferencesWithDeclaringClassName;
	}
	
	public void setQualifyReferencesWithDeclaringClassName(boolean qualify) {
		fQualifyReferencesWithDeclaringClassName= qualify;
	}
	
	public String guessConstantName() throws JavaScriptModelException {
		String[] proposals= guessConstantNames();
		if (proposals.length > 0)
			return proposals[0];
		else
			return fConstantName;
	}
	
	/**
	 * @return proposed variable names (may be empty, but not null).
	 * The first proposal should be used as "best guess" (if it exists).
	 */
	public String[] guessConstantNames() {
		if (fGuessedConstNames == null) {
			try {
				Expression expression= getSelectedExpression().getAssociatedExpression();
				if (expression != null) {
					ITypeBinding binding= expression.resolveTypeBinding();
					fGuessedConstNames= StubUtility.getVariableNameSuggestions(StubUtility.CONSTANT_FIELD, fCu.getJavaScriptProject(), binding, expression, Arrays.asList(getExcludedVariableNames()));
				} 
			} catch (JavaScriptModelException e) {
			}
			if (fGuessedConstNames == null)
				fGuessedConstNames= new String[0];
		}
		return fGuessedConstNames;
	}
	
	
	private String[] getExcludedVariableNames() {
		if (fExcludedVariableNames == null) {
			try {
				IExpressionFragment expr= getSelectedExpression();
				Collection takenNames= new ScopeAnalyzer(fCuRewrite.getRoot()).getUsedVariableNames(expr.getStartPosition(), expr.getLength());
				fExcludedVariableNames= (String[]) takenNames.toArray(new String[takenNames.size()]);
			} catch (JavaScriptModelException e) {
				fExcludedVariableNames= new String[0];
			}
		}
		return fExcludedVariableNames;
	}
		
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 7); //$NON-NLS-1$
	
			RefactoringStatus result= Checks.validateEdit(fCu, getValidationContext());
			if (result.hasFatalError())
				return result;
			pm.worked(1);
			
			if (fCuRewrite == null) {
				JavaScriptUnit cuNode= RefactoringASTParser.parseWithASTProvider(fCu, true, new SubProgressMonitor(pm, 3));
				fCuRewrite= new CompilationUnitRewrite(fCu, cuNode);
			} else {
				pm.worked(3);
			}
			result.merge(checkSelection(new SubProgressMonitor(pm, 3)));
	
			if (result.hasFatalError())
				return result;
			
			if (isLiteralNodeSelected())
				fReplaceAllOccurrences= false;
			
			return result;
		} finally {
			pm.done();
		}
	}
	
	public boolean selectionAllStaticFinal() {
		Assert.isTrue(fAllStaticFinalCheckPerformed);
		return fSelectionAllStaticFinal;
	}

	private void checkAllStaticFinal() throws JavaScriptModelException {
		fSelectionAllStaticFinal= ConstantChecks.isStaticFinalConstant(getSelectedExpression());
		fAllStaticFinalCheckPerformed= true;
	}

	private RefactoringStatus checkSelection(IProgressMonitor pm) throws JavaScriptModelException {
		try {
			pm.beginTask("", 2); //$NON-NLS-1$
			
			IExpressionFragment selectedExpression= getSelectedExpression();
			
			if (selectedExpression == null) {
				String message= RefactoringCoreMessages.ExtractConstantRefactoring_select_expression; 
				return CodeRefactoringUtil.checkMethodSyntaxErrors(fSelectionStart, fSelectionLength, fCuRewrite.getRoot(), message);
			}
			pm.worked(1);
			
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkExpression());
			if (result.hasFatalError())
				return result;
			pm.worked(1);
			
			return result;
		} finally {
			pm.done();
		}
	}

	private RefactoringStatus checkExpressionBinding() throws JavaScriptModelException {
		return checkExpressionFragmentIsRValue();
	}
	
	private RefactoringStatus checkExpressionFragmentIsRValue() throws JavaScriptModelException {
		/* Moved this functionality to Checks, to allow sharing with
		   ExtractTempRefactoring, others */
		switch(Checks.checkExpressionIsRValue(getSelectedExpression().getAssociatedExpression())) {
			case Checks.NOT_RVALUE_MISC:
				return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.ExtractConstantRefactoring_select_expression, null, Corext.getPluginId(), RefactoringStatusCodes.EXPRESSION_NOT_RVALUE, null); 
			case Checks.NOT_RVALUE_VOID:
				return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.ExtractConstantRefactoring_no_void, null, Corext.getPluginId(), RefactoringStatusCodes.EXPRESSION_NOT_RVALUE_VOID, null); 
			case Checks.IS_RVALUE:
				return new RefactoringStatus();
			default:
				Assert.isTrue(false); return null;
		}		
	}

	//	 !!! -- same as in ExtractTempRefactoring
	private boolean isLiteralNodeSelected() throws JavaScriptModelException {
		IExpressionFragment fragment= getSelectedExpression();
		if (fragment == null)
			return false;
		Expression expression= fragment.getAssociatedExpression();
		if (expression == null)
			return false;
		switch (expression.getNodeType()) {
			case ASTNode.BOOLEAN_LITERAL :
			case ASTNode.CHARACTER_LITERAL :
			case ASTNode.NULL_LITERAL :
			case ASTNode.NUMBER_LITERAL :
			case ASTNode.UNDEFINED_LITERAL :
			case ASTNode.REGULAR_EXPRESSION_LITERAL :
				return true;
			
			default :
				return false;
		}
	}

	private RefactoringStatus checkExpression() throws JavaScriptModelException {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkExpressionBinding());
		if(result.hasFatalError())
			return result;
		checkAllStaticFinal();

		IExpressionFragment selectedExpression= getSelectedExpression();
		Expression associatedExpression= selectedExpression.getAssociatedExpression();
		if (associatedExpression instanceof NullLiteral)
			result.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractConstantRefactoring_null_literals)); 
		else if (!ConstantChecks.isLoadTimeConstant(selectedExpression))
			result.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractConstantRefactoring_not_load_time_constant)); 
		else if (associatedExpression instanceof SimpleName) {
			if (associatedExpression.getParent() instanceof QualifiedName && associatedExpression.getLocationInParent() == QualifiedName.NAME_PROPERTY
					|| associatedExpression.getParent() instanceof FieldAccess && associatedExpression.getLocationInParent() == FieldAccess.NAME_PROPERTY)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractConstantRefactoring_select_expression);
		}
		
		return result;
	}

	public void setConstantName(String newName) {
		Assert.isNotNull(newName);
		fConstantName= newName;
	}

	public String getConstantName() {
		return fConstantName;
	}

	/**
	 * This method performs checks on the constant name which are
	 * quick enough to be performed every time the ui input component
	 * contents are changed.
	 * 
	 * @return return the resulting status
	 * @throws JavaScriptModelException thrown when the operation could not be executed
	 */
	public RefactoringStatus checkConstantNameOnChange() throws JavaScriptModelException {
		if (Arrays.asList(getExcludedVariableNames()).contains(fConstantName))
			return RefactoringStatus.createErrorStatus(Messages.format(RefactoringCoreMessages.ExtractConstantRefactoring_another_variable, getConstantName())); 
		return Checks.checkConstantName(getConstantName());
	}
	
	// !! similar to ExtractTempRefactoring equivalent
	public String getConstantSignaturePreview() throws JavaScriptModelException {
		String space= " "; //$NON-NLS-1$
		return getVisibility() + space + MODIFIER + space + getConstantTypeName() + space + fConstantName;
	}
	
	public CompilationUnitChange createTextChange(IProgressMonitor pm) throws CoreException {
		createConstantDeclaration();
		replaceExpressionsWithConstant();
		return fCuRewrite.createChange(RefactoringCoreMessages.ExtractConstantRefactoring_change_name, true, pm);
	}
	

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		pm.beginTask(RefactoringCoreMessages.ExtractConstantRefactoring_checking_preconditions, 4); 
		
		/* Note: some checks are performed on change of input widget
		 * values. (e.g. see ExtractConstantRefactoring.checkConstantNameOnChange())
		 */ 
		
		//TODO: possibly add more checking for name conflicts that might
		//      lead to a change in behaviour
		
		try {
			RefactoringStatus result= new RefactoringStatus();
			fChange= createTextChange(new SubProgressMonitor(pm, 2));
			
			String newCuSource= fChange.getPreviewContent(new NullProgressMonitor());
			JavaScriptUnit newCUNode= new RefactoringASTParser(AST.JLS3).parse(newCuSource, fCu, true, true, null);
			
			IProblem[] newProblems= RefactoringAnalyzeUtil.getIntroducedCompileProblems(newCUNode, fCuRewrite.getRoot());
			for (int i= 0; i < newProblems.length; i++) {
				IProblem problem= newProblems[i];
				if (problem.isError())
					result.addEntry(new RefactoringStatusEntry((problem.isError() ? RefactoringStatus.ERROR : RefactoringStatus.WARNING), problem.getMessage(), new JavaStringStatusContext(newCuSource, new SourceRange(problem))));
			}
			
			fConstantTypeCache= null;
			fCuRewrite.clearASTAndImportRewrites();

			return result;
		} finally {
			pm.done();
		}
	}

	private void createConstantDeclaration() throws CoreException {
		Type type= getConstantType();
		
		IExpressionFragment fragment= getSelectedExpression();
		String initializerSource= fCu.getBuffer().getText(fragment.getStartPosition(), fragment.getLength());
		
		AST ast= fCuRewrite.getAST();
		VariableDeclarationFragment variableDeclarationFragment= ast.newVariableDeclarationFragment();
		variableDeclarationFragment.setName(ast.newSimpleName(fConstantName));
		variableDeclarationFragment.setInitializer((Expression) fCuRewrite.getASTRewrite().createStringPlaceholder(initializerSource, ASTNode.SIMPLE_NAME));
		
		FieldDeclaration fieldDeclaration= ast.newFieldDeclaration(variableDeclarationFragment);
		fieldDeclaration.setType(type);
		Modifier.ModifierKeyword accessModifier= Modifier.ModifierKeyword.toKeyword(fVisibility);
		if (accessModifier != null)
			fieldDeclaration.modifiers().add(ast.newModifier(accessModifier));
		fieldDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));
		fieldDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD));
		
		boolean createComments= JavaPreferencesSettings.getCodeGenerationSettings(fCu.getJavaScriptProject()).createComments;
		if (createComments) {
			String comment= CodeGeneration.getFieldComment(fCu, getConstantTypeName(), fConstantName, StubUtility.getLineDelimiterUsed(fCu));
			if (comment != null && comment.length() > 0) {
				JSdoc doc= (JSdoc) fCuRewrite.getASTRewrite().createStringPlaceholder(comment, ASTNode.JSDOC);
				fieldDeclaration.setJavadoc(doc);
			}
		}
		
		AbstractTypeDeclaration parent= getContainingTypeDeclarationNode();
		ListRewrite listRewrite= fCuRewrite.getASTRewrite().getListRewrite(parent, parent.getBodyDeclarationsProperty());
		TextEditGroup msg= fCuRewrite.createGroupDescription(RefactoringCoreMessages.ExtractConstantRefactoring_declare_constant); 
		if (insertFirst()) {
			listRewrite.insertFirst(fieldDeclaration, msg);
		} else {
			listRewrite.insertAfter(fieldDeclaration, getNodeToInsertConstantDeclarationAfter(), msg);
		}
		
		if (fLinkedProposalModel != null) {
			ASTRewrite rewrite= fCuRewrite.getASTRewrite();
			LinkedProposalPositionGroup nameGroup= fLinkedProposalModel.getPositionGroup(KEY_NAME, true);
			nameGroup.addPosition(rewrite.track(variableDeclarationFragment.getName()), true);
			
			String[] nameSuggestions= guessConstantNames();
			if (nameSuggestions.length > 0 && !nameSuggestions[0].equals(fConstantName)) {
				nameGroup.addProposal(fConstantName, null, nameSuggestions.length + 1);
			}
			for (int i= 0; i < nameSuggestions.length; i++) {
				nameGroup.addProposal(nameSuggestions[i], null, nameSuggestions.length - i);
			}
			
			LinkedProposalPositionGroup typeGroup= fLinkedProposalModel.getPositionGroup(KEY_TYPE, true);
			typeGroup.addPosition(rewrite.track(type), true);
			
			ITypeBinding typeBinding= fragment.getAssociatedExpression().resolveTypeBinding();
			if (typeBinding != null) {
				ITypeBinding[] relaxingTypes= ASTResolving.getNarrowingTypes(ast, typeBinding);
				for (int i= 0; i < relaxingTypes.length; i++) {
					typeGroup.addProposal(relaxingTypes[i], fCuRewrite.getCu(), relaxingTypes.length - i);
				}
			}
			ModifierCorrectionSubProcessor.installLinkedVisibilityProposals(fLinkedProposalModel, rewrite, fieldDeclaration.modifiers(), false);
		}
	}

	private Type getConstantType() throws JavaScriptModelException {
		if (fConstantTypeCache == null) {
			IExpressionFragment fragment= getSelectedExpression();
			ITypeBinding typeBinding= fragment.getAssociatedExpression().resolveTypeBinding();
			AST ast= fCuRewrite.getAST();
			typeBinding= Bindings.normalizeForDeclarationUse(typeBinding, ast);
			fConstantTypeCache= fCuRewrite.getImportRewrite().addImport(typeBinding, ast);
		}
		return fConstantTypeCache;
	}

	public Change createChange(IProgressMonitor monitor) throws CoreException {
		final Map arguments= new HashMap();
		String project= null;
		IJavaScriptProject javaProject= fCu.getJavaScriptProject();
		if (javaProject != null)
			project= javaProject.getElementName();
		int flags= JavaScriptRefactoringDescriptor.JAR_REFACTORING | JavaScriptRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
		if (JdtFlags.getVisibilityCode(fVisibility) != Modifier.PRIVATE)
			flags|= RefactoringDescriptor.STRUCTURAL_CHANGE;
		String pattern= ""; //$NON-NLS-1$
		try {
			pattern= BindingLabelProvider.getBindingLabel(getContainingTypeBinding(), JavaScriptElementLabels.ALL_FULLY_QUALIFIED) + "."; //$NON-NLS-1$
		} catch (JavaScriptModelException exception) {
			JavaScriptPlugin.log(exception);
		}
		final String expression= ASTNodes.asString(fSelectedExpression.getAssociatedExpression());
		final String description= Messages.format(RefactoringCoreMessages.ExtractConstantRefactoring_descriptor_description_short, fConstantName);
		final String header= Messages.format(RefactoringCoreMessages.ExtractConstantRefactoring_descriptor_description, new String[] { pattern + fConstantName, expression});
		final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
		comment.addSetting(Messages.format(RefactoringCoreMessages.ExtractConstantRefactoring_constant_name_pattern, fConstantName));
		comment.addSetting(Messages.format(RefactoringCoreMessages.ExtractConstantRefactoring_constant_expression_pattern, expression));
		String visibility= fVisibility;
		if ("".equals(visibility)) //$NON-NLS-1$
			visibility= RefactoringCoreMessages.ExtractConstantRefactoring_default_visibility;
		comment.addSetting(Messages.format(RefactoringCoreMessages.ExtractConstantRefactoring_visibility_pattern, visibility));
		if (fReplaceAllOccurrences)
			comment.addSetting(RefactoringCoreMessages.ExtractConstantRefactoring_replace_occurrences);
		if (fQualifyReferencesWithDeclaringClassName)
			comment.addSetting(RefactoringCoreMessages.ExtractConstantRefactoring_qualify_references);
		final JDTRefactoringDescriptor descriptor= new JDTRefactoringDescriptor(IJavaScriptRefactorings.EXTRACT_CONSTANT, project, description, comment.asString(), arguments, flags);
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fCu));
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_NAME, fConstantName);
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_SELECTION, Integer.valueOf(fSelectionStart).toString() + " " + Integer.valueOf(fSelectionLength).toString()); //$NON-NLS-1$
		arguments.put(ATTRIBUTE_REPLACE, Boolean.valueOf(fReplaceAllOccurrences).toString());
		arguments.put(ATTRIBUTE_QUALIFY, Boolean.valueOf(fQualifyReferencesWithDeclaringClassName).toString());
		arguments.put(ATTRIBUTE_VISIBILITY, Integer.valueOf(JdtFlags.getVisibilityCode(fVisibility)).toString());
		return new RefactoringDescriptorChange(descriptor, RefactoringCoreMessages.ExtractConstantRefactoring_name, new Change[] { fChange});
	}

	private void replaceExpressionsWithConstant() throws JavaScriptModelException {
		ASTRewrite astRewrite= fCuRewrite.getASTRewrite();
		AST ast= astRewrite.getAST();
		
		IASTFragment[] fragmentsToReplace= getFragmentsToReplace();
		for (int i= 0; i < fragmentsToReplace.length; i++) {
			IASTFragment fragment= fragmentsToReplace[i];
			
			SimpleName ref= ast.newSimpleName(fConstantName);
			Name replacement= ref;
			if (qualifyReferencesWithDeclaringClassName()) {
				replacement= ast.newQualifiedName(ast.newSimpleName(getContainingTypeBinding().getName()), ref);
			}
			TextEditGroup description= fCuRewrite.createGroupDescription(RefactoringCoreMessages.ExtractConstantRefactoring_replace);
			
			fragment.replace(astRewrite, replacement, description);
			if (fLinkedProposalModel != null)
				fLinkedProposalModel.getPositionGroup(KEY_NAME, true).addPosition(astRewrite.track(ref), false);
		}
	}
	
	private void computeConstantDeclarationLocation() throws JavaScriptModelException {
		if (isDeclarationLocationComputed())
			return;

		BodyDeclaration lastStaticDependency= null;
		Iterator decls= getBodyDeclarations();
		
		Assert.isTrue(decls.hasNext()); /* Admissible selected expressions must occur
		                                   within a body declaration.  Thus, the 
		                                   class/interface in which such an expression occurs
		                                   must have at least one body declaration */
		
		while (decls.hasNext()) {
			BodyDeclaration decl= (BodyDeclaration) decls.next();
			
			int modifiers;
			if (decl instanceof FieldDeclaration)
				modifiers= ((FieldDeclaration) decl).getModifiers();
			else if (decl instanceof Initializer)
				modifiers= ((Initializer) decl).getModifiers();
			else {
				continue; /* this declaration is not a field declaration
				              or initializer, so the placement of the constant
				              declaration relative to it does not matter */
			}
			
			if (Modifier.isStatic(modifiers) && depends(getSelectedExpression(), decl))
				lastStaticDependency= decl;
		}
		
		if(lastStaticDependency == null)
			fInsertFirst= true;
		else
			fToInsertAfter= lastStaticDependency;
	}
	
	/* bd is a static field declaration or static initializer */
	private static boolean depends(IExpressionFragment selected, BodyDeclaration bd) {
		/* We currently consider selected to depend on bd only if db includes a declaration
		 * of a static field on which selected depends.
		 * 
		 * A more accurate strategy might be to also check if bd contains (or is) a
		 * static initializer containing code which changes the value of a static field on 
		 * which selected depends.  However, if a static is written to multiple times within
		 * during class initialization, it is difficult to predict which value should be used.
		 * This would depend on which value is used by expressions instances for which the new 
		 * constant will be substituted, and there may be many of these; in each, the
		 * static field in question may have taken on a different value (if some of these uses
		 * occur within static initializers).
		 */
		
		if(bd instanceof FieldDeclaration) {
			FieldDeclaration fieldDecl = (FieldDeclaration) bd;
			for(Iterator fragments = fieldDecl.fragments().iterator(); fragments.hasNext();) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.next();
				SimpleName staticFieldName = fragment.getName();
				if(selected.getSubFragmentsMatching(ASTFragmentFactory.createFragmentForFullSubtree(staticFieldName)).length != 0)
					return true;
			}
		}
		return false;
	}

	private boolean isDeclarationLocationComputed() {
		return fInsertFirst == true || fToInsertAfter != null;	
	}
	
	private boolean insertFirst() throws JavaScriptModelException {
		if(!isDeclarationLocationComputed())
			computeConstantDeclarationLocation();
		return fInsertFirst;
	}
	
	private BodyDeclaration getNodeToInsertConstantDeclarationAfter() throws JavaScriptModelException {
		if(!isDeclarationLocationComputed())
			computeConstantDeclarationLocation();
		return fToInsertAfter;
	}
	
	private Iterator getBodyDeclarations() throws JavaScriptModelException {
		if(fBodyDeclarations == null)
			fBodyDeclarations= getContainingTypeDeclarationNode().bodyDeclarations();
		return fBodyDeclarations.iterator();
	}

	private String getConstantTypeName() throws JavaScriptModelException {
		return ASTNodes.asString(getConstantType());
	}

	private static boolean isStaticFieldOrStaticInitializer(BodyDeclaration node) {
		if(node instanceof FunctionDeclaration || node instanceof AbstractTypeDeclaration)
			return false;
		
		int modifiers;
		if(node instanceof FieldDeclaration) {
			modifiers = ((FieldDeclaration) node).getModifiers();
		} else if(node instanceof Initializer) {
			modifiers = ((Initializer) node).getModifiers();
		} else {
			Assert.isTrue(false);
			return false;
		}
		
		if(!Modifier.isStatic(modifiers))
			return false;
		
		return true;
	}
	
	/*
	 * Elements returned by next() are BodyDeclaration
	 * instances.
	 */
	private Iterator getReplacementScope() throws JavaScriptModelException {
		boolean declPredecessorReached= false;
		
		Collection scope= new ArrayList();
		for(Iterator bodyDeclarations = getBodyDeclarations(); bodyDeclarations.hasNext();) {
		    BodyDeclaration bodyDeclaration= (BodyDeclaration) bodyDeclarations.next();
		    
		    if(bodyDeclaration == getNodeToInsertConstantDeclarationAfter())
		    	declPredecessorReached= true;
		    
		    if(insertFirst() || declPredecessorReached || !isStaticFieldOrStaticInitializer(bodyDeclaration))
		    	scope.add(bodyDeclaration);
		}
		return scope.iterator();
	}

	private IASTFragment[] getFragmentsToReplace() throws JavaScriptModelException {
		List toReplace = new ArrayList();
		if (fReplaceAllOccurrences) {
			Iterator replacementScope = getReplacementScope();
			while(replacementScope.hasNext()) {
				BodyDeclaration bodyDecl = (BodyDeclaration) replacementScope.next();
				IASTFragment[] allMatches= ASTFragmentFactory.createFragmentForFullSubtree(bodyDecl).getSubFragmentsMatching(getSelectedExpression());
				IASTFragment[] replaceableMatches = retainOnlyReplacableMatches(allMatches);
				for(int i = 0; i < replaceableMatches.length; i++)
					toReplace.add(replaceableMatches[i]);
			}
		} else if (canReplace(getSelectedExpression()))
			toReplace.add(getSelectedExpression());
		return (IASTFragment[]) toReplace.toArray(new IASTFragment[toReplace.size()]);
	}

	// !! - like one in ExtractTempRefactoring
	private static IASTFragment[] retainOnlyReplacableMatches(IASTFragment[] allMatches) {
		List result= new ArrayList(allMatches.length);
		for (int i= 0; i < allMatches.length; i++) {
			if (canReplace(allMatches[i]))
				result.add(allMatches[i]);
		}
		return (IASTFragment[]) result.toArray(new IASTFragment[result.size()]);
	}

	// !! - like one in ExtractTempRefactoring
	private static boolean canReplace(IASTFragment fragment) {
		ASTNode node= fragment.getAssociatedNode();
		ASTNode parent= node.getParent();
		if (parent instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment vdf= (VariableDeclarationFragment) parent;
			if (node.equals(vdf.getName()))
				return false;
		}
		if (parent instanceof ExpressionStatement)
			return false;
		if (parent instanceof SwitchCase)
			return false;
		return true;
	}

	private IExpressionFragment getSelectedExpression() throws JavaScriptModelException {
		if(fSelectedExpression != null)
			return fSelectedExpression;
		
		IASTFragment selectedFragment= ASTFragmentFactory.createFragmentForSourceRange(new SourceRange(fSelectionStart, fSelectionLength), fCuRewrite.getRoot(), fCu);
		
		if (selectedFragment instanceof IExpressionFragment
				&& ! Checks.isInsideJavadoc(selectedFragment.getAssociatedNode())) {
			fSelectedExpression= (IExpressionFragment) selectedFragment;
		}
		
		return fSelectedExpression;
	}

	private AbstractTypeDeclaration getContainingTypeDeclarationNode() throws JavaScriptModelException {
		AbstractTypeDeclaration result= (AbstractTypeDeclaration) ASTNodes.getParent(getSelectedExpression().getAssociatedNode(), AbstractTypeDeclaration.class);  
		Assert.isNotNull(result);
		return result;
	}

	private ITypeBinding getContainingTypeBinding() throws JavaScriptModelException {
		ITypeBinding result= getContainingTypeDeclarationNode().resolveBinding();
		Assert.isNotNull(result);
		return result;
	}

	public RefactoringStatus initialize(final RefactoringArguments arguments) {
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
					return createInputFatalStatus(element, IJavaScriptRefactorings.EXTRACT_CONSTANT);
				else
					fCu= (IJavaScriptUnit) element;
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
			final String visibility= extended.getAttribute(ATTRIBUTE_VISIBILITY);
			if (visibility != null && !"".equals(visibility)) {//$NON-NLS-1$
				int flag= 0;
				try {
					flag= Integer.parseInt(visibility);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_VISIBILITY));
				}
				fVisibility= JdtFlags.getVisibilityString(flag);
			}
			final String name= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_NAME);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				fConstantName= name;
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_NAME));
			final String replace= extended.getAttribute(ATTRIBUTE_REPLACE);
			if (replace != null) {
				fReplaceAllOccurrences= Boolean.valueOf(replace).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_REPLACE));
			final String declareFinal= extended.getAttribute(ATTRIBUTE_QUALIFY);
			if (declareFinal != null) {
				fQualifyReferencesWithDeclaringClassName= Boolean.valueOf(declareFinal).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_QUALIFY));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}
}
