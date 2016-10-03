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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.ArrayInitializer;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.CatchClause;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.DoStatement;
import org.eclipse.wst.jsdt.core.dom.EnhancedForStatement;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.ForInStatement;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.eclipse.wst.jsdt.core.dom.Initializer;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.NullLiteral;
import org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression;
import org.eclipse.wst.jsdt.core.dom.PostfixExpression;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.SwitchCase;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.WhileStatement;
import org.eclipse.wst.jsdt.core.dom.WithStatement;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.internal.corext.Corext;
import org.eclipse.wst.jsdt.internal.corext.SourceRange;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
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
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStringStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.RefactoringDescriptorChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RefactoringAnalyzeUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.NoCommentSourceRangeComputer;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.BindingLabelProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * Extract Local Variable (from selected expression inside method or initializer).
 */
public class ExtractTempRefactoring extends ScriptableRefactoring {

	private static final String ATTRIBUTE_REPLACE= "replace"; //$NON-NLS-1$
	private static final String ATTRIBUTE_FINAL= "final"; //$NON-NLS-1$

	private static final class ForStatementChecker extends ASTVisitor {

		private final Collection fForInitializerVariables;

		private boolean fReferringToForVariable= false;

		public ForStatementChecker(Collection forInitializerVariables) {
			Assert.isNotNull(forInitializerVariables);
			fForInitializerVariables= forInitializerVariables;
		}

		public boolean isReferringToForVariable() {
			return fReferringToForVariable;
		}

		public boolean visit(SimpleName node) {
			IBinding binding= node.resolveBinding();
			if (binding != null && fForInitializerVariables.contains(binding)) {
				fReferringToForVariable= true;
			}
			return false;
		}
	}



	private static boolean allArraysEqual(Object[][] arrays, int position) {
		Object element= arrays[0][position];
		for (int i= 0; i < arrays.length; i++) {
			Object[] array= arrays[i];
			if (!element.equals(array[position]))
				return false;
		}
		return true;
	}

	private static boolean canReplace(IASTFragment fragment) {
		ASTNode node= fragment.getAssociatedNode();
		ASTNode parent= node.getParent();
		if (parent instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment vdf= (VariableDeclarationFragment) parent;
			if (node.equals(vdf.getName()))
				return false;
		}
		if (isMethodParameter(node))
			return false;
		if (isThrowableInCatchBlock(node))
			return false;
		if (parent instanceof ExpressionStatement)
			return false;
		if (isLeftValue(node))
			return false;
		if (isReferringToLocalVariableFromFor((Expression) node))
			return false;
		if (isUsedInForInitializerOrUpdater((Expression) node))
			return false;
		if (parent instanceof SwitchCase)
			return false;
		return true;
	}

	private static Object[] getArrayPrefix(Object[] array, int prefixLength) {
		Assert.isTrue(prefixLength <= array.length);
		Assert.isTrue(prefixLength >= 0);
		Object[] prefix= new Object[prefixLength];
		for (int i= 0; i < prefix.length; i++) {
			prefix[i]= array[i];
		}
		return prefix;
	}

	// return List<IVariableBinding>
	private static List getForInitializedVariables(VariableDeclarationExpression variableDeclarations) {
		List forInitializerVariables= new ArrayList(1);
		for (Iterator iter= variableDeclarations.fragments().iterator(); iter.hasNext();) {
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) iter.next();
			IVariableBinding binding= fragment.resolveBinding();
			if (binding != null)
				forInitializerVariables.add(binding);
		}
		return forInitializerVariables;
	}

	private static Object[] getLongestArrayPrefix(Object[][] arrays) {
		int length= -1;
		if (arrays.length == 0)
			return new Object[0];
		int minArrayLength= arrays[0].length;
		for (int i= 1; i < arrays.length; i++)
			minArrayLength= Math.min(minArrayLength, arrays[i].length);

		for (int i= 0; i < minArrayLength; i++) {
			if (!allArraysEqual(arrays, i))
				break;
			length++;
		}
		if (length == -1)
			return new Object[0];
		return getArrayPrefix(arrays[0], length + 1);
	}

	private static ASTNode[] getParents(ASTNode node) {
		ASTNode current= node;
		List parents= new ArrayList();
		do {
			parents.add(current.getParent());
			current= current.getParent();
		} while (current.getParent() != null);
		Collections.reverse(parents);
		return (ASTNode[]) parents.toArray(new ASTNode[parents.size()]);
	}

	private static boolean isLeftValue(ASTNode node) {
		ASTNode parent= node.getParent();
		if (parent instanceof Assignment) {
			Assignment assignment= (Assignment) parent;
			if (assignment.getLeftHandSide() == node)
				return true;
		}
		if (parent instanceof PostfixExpression)
			return true;
		if (parent instanceof PrefixExpression) {
			PrefixExpression.Operator op= ((PrefixExpression) parent).getOperator();
			if (op.equals(PrefixExpression.Operator.DECREMENT))
				return true;
			if (op.equals(PrefixExpression.Operator.INCREMENT))
				return true;
			return false;
		}
		return false;
	}

	private static boolean isMethodParameter(ASTNode node) {
		return (node instanceof SimpleName) && (node.getParent() instanceof SingleVariableDeclaration) && (node.getParent().getParent() instanceof FunctionDeclaration);
	}

	private static boolean isReferringToLocalVariableFromFor(Expression expression) {
		ASTNode current= expression;
		ASTNode parent= current.getParent();
		while (parent != null && !(parent instanceof BodyDeclaration)) {
			if (parent instanceof ForStatement) {
				ForStatement forStmt= (ForStatement) parent;
				if (forStmt.initializers().contains(current) || forStmt.updaters().contains(current) || forStmt.getExpression() == current) {
					List initializers= forStmt.initializers();
					if (initializers.size() == 1 && initializers.get(0) instanceof VariableDeclarationExpression) {
						List forInitializerVariables= getForInitializedVariables((VariableDeclarationExpression) initializers.get(0));
						ForStatementChecker checker= new ForStatementChecker(forInitializerVariables);
						expression.accept(checker);
						if (checker.isReferringToForVariable())
							return true;
					}
				}
			}
			else if (parent instanceof ForInStatement) {
				ForInStatement forInStmt= (ForInStatement) parent;
				if (forInStmt.getIterationVariable().equals(current)  || forInStmt.getCollection() == current) {
					if (forInStmt.getIterationVariable() instanceof VariableDeclarationStatement) {
						List forInitializerVariables= new ArrayList(1);
						forInitializerVariables.add(  ((VariableDeclarationStatement) forInStmt.getIterationVariable()).resolveBinding());
						ForStatementChecker checker= new ForStatementChecker(forInitializerVariables);
						expression.accept(checker);
						if (checker.isReferringToForVariable())
							return true;
					}
				}
			}
			current= parent;
			parent= current.getParent();
		}
		return false;
	}

	private static boolean isThrowableInCatchBlock(ASTNode node) {
		return (node instanceof SimpleName) && (node.getParent() instanceof SingleVariableDeclaration) && (node.getParent().getParent() instanceof CatchClause);
	}

	private static boolean isUsedInForInitializerOrUpdater(Expression expression) {
		ASTNode parent= expression.getParent();
		if (parent instanceof ForStatement) {
			ForStatement forStmt= (ForStatement) parent;
			return forStmt.initializers().contains(expression) || forStmt.updaters().contains(expression);
		}
		return false;
	}

	private static IASTFragment[] retainOnlyReplacableMatches(IASTFragment[] allMatches) {
		List result= new ArrayList(allMatches.length);
		for (int i= 0; i < allMatches.length; i++) {
			if (canReplace(allMatches[i]))
				result.add(allMatches[i]);
		}
		return (IASTFragment[]) result.toArray(new IASTFragment[result.size()]);
	}

	private JavaScriptUnit fCompilationUnitNode;
	
	private CompilationUnitRewrite fCURewrite;

	private IJavaScriptUnit fCu;

	private boolean fDeclareFinal;

	private String[] fExcludedVariableNames;

	private boolean fReplaceAllOccurrences;

	// caches:
	private IExpressionFragment fSelectedExpression;

	private int fSelectionLength;

	private int fSelectionStart;

	private String fTempName;
	private String[] fGuessedTempNames;

	private TextChange fChange;
	
	private LinkedProposalModel fLinkedProposalModel;
	
	private static final String KEY_NAME= "name"; //$NON-NLS-1$
//	private static final String KEY_TYPE= "type"; //$NON-NLS-1$
	

	/**
	 * Creates a new extract temp refactoring
	 * @param unit the compilation unit, or <code>null</code> if invoked by scripting
	 * @param selectionStart
	 * @param selectionLength
	 */
	public ExtractTempRefactoring(IJavaScriptUnit unit, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= unit;
		fCompilationUnitNode= null;

		fReplaceAllOccurrences= true; // default
		fDeclareFinal= false; // default
		fTempName= ""; //$NON-NLS-1$
		
		fLinkedProposalModel= null;
	}
	
	public ExtractTempRefactoring(JavaScriptUnit astRoot, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(astRoot.getTypeRoot() instanceof IJavaScriptUnit);
		
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= (IJavaScriptUnit) astRoot.getTypeRoot();
		fCompilationUnitNode= astRoot;

		fReplaceAllOccurrences= true; // default
		fDeclareFinal= false; // default
		fTempName= ""; //$NON-NLS-1$
		
		fLinkedProposalModel= null;
	}
	
	public void setLinkedProposalModel(LinkedProposalModel linkedProposalModel) {
		fLinkedProposalModel= linkedProposalModel;
	}

	private void addReplaceExpressionWithTemp() throws JavaScriptModelException {
		IASTFragment[] fragmentsToReplace= retainOnlyReplacableMatches(getMatchingFragments());
		//TODO: should not have to prune duplicates here...
		ASTRewrite rewrite= fCURewrite.getASTRewrite();
		HashSet seen= new HashSet();
		for (int i= 0; i < fragmentsToReplace.length; i++) {
			IASTFragment fragment= fragmentsToReplace[i];
			if (! seen.add(fragment))
				continue;
			SimpleName tempName= fCURewrite.getAST().newSimpleName(fTempName);
			TextEditGroup description= fCURewrite.createGroupDescription(RefactoringCoreMessages.ExtractTempRefactoring_replace);

			fragment.replace(rewrite, tempName, description);
			if (fLinkedProposalModel != null)
				fLinkedProposalModel.getPositionGroup(KEY_NAME, true).addPosition(rewrite.track(tempName), false);
		}
	}

	private RefactoringStatus checkExpression() throws JavaScriptModelException {
		Expression selectedExpression= getSelectedExpression().getAssociatedExpression();
		if (selectedExpression != null) {
			final ASTNode parent= selectedExpression.getParent();
			if (selectedExpression instanceof NullLiteral) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_null_literals);
			} else if (selectedExpression instanceof ArrayInitializer) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_array_initializer);
			} else if (selectedExpression instanceof Assignment) {
				if (parent instanceof Expression && !(parent instanceof ParenthesizedExpression))
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_assignment);
				else
					return null;

			} else if (selectedExpression instanceof SimpleName) {
				if ((((SimpleName) selectedExpression)).isDeclaration())
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_names_in_declarations);
				if (parent instanceof QualifiedName && selectedExpression.getLocationInParent() == QualifiedName.NAME_PROPERTY || parent instanceof FieldAccess && selectedExpression.getLocationInParent() == FieldAccess.NAME_PROPERTY)
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_select_expression);
			}
		}

		return null;
	}

	// !! Same as in ExtractConstantRefactoring
	private RefactoringStatus checkExpressionFragmentIsRValue() throws JavaScriptModelException {
		switch (Checks.checkExpressionIsRValue(getSelectedExpression().getAssociatedExpression())) {
			case Checks.NOT_RVALUE_MISC:
				return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.ExtractTempRefactoring_select_expression, null, Corext.getPluginId(), RefactoringStatusCodes.EXPRESSION_NOT_RVALUE, null); 
			case Checks.NOT_RVALUE_VOID:
				return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.ExtractTempRefactoring_no_void, null, Corext.getPluginId(), RefactoringStatusCodes.EXPRESSION_NOT_RVALUE_VOID, null); 
			case Checks.IS_RVALUE:
				return new RefactoringStatus();
			default:
				Assert.isTrue(false);
				return null;
		}
	}
	
	public TextChange createTextChange(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.ExtractTempRefactoring_checking_preconditions, 3);
			
			fCURewrite= new CompilationUnitRewrite(fCu, fCompilationUnitNode);
			fCURewrite.getASTRewrite().setTargetSourceRangeComputer(new NoCommentSourceRangeComputer());
			
			doCreateChange(new SubProgressMonitor(pm, 2));
			
			return fCURewrite.createChange(RefactoringCoreMessages.ExtractTempRefactoring_change_name, true, new SubProgressMonitor(pm, 1));
		} finally {
			pm.done();
		}
	}
	

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		fChange= createTextChange(pm);

		RefactoringStatus result= new RefactoringStatus();
		if (Arrays.asList(getExcludedVariableNames()).contains(fTempName))
			result.addWarning(Messages.format(RefactoringCoreMessages.ExtractTempRefactoring_another_variable, fTempName)); 

		result.merge(checkMatchingFragments());
		
		fChange.setKeepPreviewEdits(true);
		checkNewSource(result);
		
		return result;
	}
	
	private final JDTRefactoringDescriptor createRefactoringDescriptor() {
		final Map arguments= new HashMap();
		String project= null;
		IJavaScriptProject javaProject= fCu.getJavaScriptProject();
		if (javaProject != null)
			project= javaProject.getElementName();
		final String description= Messages.format(RefactoringCoreMessages.ExtractTempRefactoring_descriptor_description_short, fTempName);
		final String expression= ASTNodes.asString(fSelectedExpression.getAssociatedExpression());
		final String header= Messages.format(RefactoringCoreMessages.ExtractTempRefactoring_descriptor_description, new String[] { fTempName, expression});
		final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
		comment.addSetting(Messages.format(RefactoringCoreMessages.ExtractTempRefactoring_name_pattern, fTempName));
		final BodyDeclaration decl= (BodyDeclaration) ASTNodes.getParent(fSelectedExpression.getAssociatedExpression(), BodyDeclaration.class);
		if (decl instanceof FunctionDeclaration) {
			final IFunctionBinding method= ((FunctionDeclaration) decl).resolveBinding();
			final String label= method != null ? BindingLabelProvider.getBindingLabel(method, JavaScriptElementLabels.ALL_FULLY_QUALIFIED) : '{' + JavaScriptElementLabels.ELLIPSIS_STRING + '}';
			comment.addSetting(Messages.format(RefactoringCoreMessages.ExtractTempRefactoring_destination_pattern, label));
		}
		comment.addSetting(Messages.format(RefactoringCoreMessages.ExtractTempRefactoring_expression_pattern, expression));
		if (fReplaceAllOccurrences)
			comment.addSetting(RefactoringCoreMessages.ExtractTempRefactoring_replace_occurrences);
		if (fDeclareFinal)
			comment.addSetting(RefactoringCoreMessages.ExtractTempRefactoring_declare_final);
		final JDTRefactoringDescriptor descriptor= new JDTRefactoringDescriptor(IJavaScriptRefactorings.EXTRACT_LOCAL_VARIABLE, project, description, comment.asString(), arguments, RefactoringDescriptor.NONE);
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fCu));
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_NAME, fTempName);
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_SELECTION, Integer.valueOf(fSelectionStart).toString() + " " + Integer.valueOf(fSelectionLength).toString()); //$NON-NLS-1$
		arguments.put(ATTRIBUTE_REPLACE, Boolean.valueOf(fReplaceAllOccurrences).toString());
		arguments.put(ATTRIBUTE_FINAL, Boolean.valueOf(fDeclareFinal).toString());
		return descriptor;
	}
	
	private void doCreateChange(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.ExtractTempRefactoring_checking_preconditions, 1);
			try {
				createTempDeclaration();
			} catch (CoreException exception) {
				JavaScriptPlugin.log(exception);
			}
			addReplaceExpressionWithTemp();
		} finally {
			pm.done();
		}
	}

	private void checkNewSource(RefactoringStatus result) throws CoreException {
		String newCuSource= fChange.getPreviewContent(new NullProgressMonitor());
		JavaScriptUnit newCUNode= new RefactoringASTParser(AST.JLS3).parse(newCuSource, fCu, true, true, null);
		IProblem[] newProblems= RefactoringAnalyzeUtil.getIntroducedCompileProblems(newCUNode, fCompilationUnitNode);
		for (int i= 0; i < newProblems.length; i++) {
			IProblem problem= newProblems[i];
			if (problem.isError())
				result.addEntry(new RefactoringStatusEntry((problem.isError() ? RefactoringStatus.ERROR : RefactoringStatus.WARNING), problem.getMessage(), new JavaStringStatusContext(newCuSource, new SourceRange(problem))));
		}
	}
		
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 6); //$NON-NLS-1$

			RefactoringStatus result= Checks.validateModifiesFiles(ResourceUtil.getFiles(new IJavaScriptUnit[] { fCu}), getValidationContext());
			if (result.hasFatalError())
				return result;

			if (fCompilationUnitNode == null) {
				fCompilationUnitNode= RefactoringASTParser.parseWithASTProvider(fCu, true, new SubProgressMonitor(pm, 3));
			} else {
				pm.worked(3);
			}
			
			result.merge(checkSelection(new SubProgressMonitor(pm, 3)));
			if (!result.hasFatalError() && isLiteralNodeSelected())
				fReplaceAllOccurrences= false;
			return result;

		} finally {
			pm.done();
		}
	}

	private RefactoringStatus checkMatchingFragments() throws JavaScriptModelException {
		RefactoringStatus result= new RefactoringStatus();
		IASTFragment[] matchingFragments= getMatchingFragments();
		for (int i= 0; i < matchingFragments.length; i++) {
			ASTNode node= matchingFragments[i].getAssociatedNode();
			if (isLeftValue(node) && !isReferringToLocalVariableFromFor((Expression) node)) {
				String msg= RefactoringCoreMessages.ExtractTempRefactoring_assigned_to; 
				result.addWarning(msg, JavaStatusContext.create(fCu, node));
			}
		}
		return result;
	}

	private RefactoringStatus checkSelection(IProgressMonitor pm) throws JavaScriptModelException {
		try {
			pm.beginTask("", 8); //$NON-NLS-1$

			IExpressionFragment selectedExpression= getSelectedExpression();

			if (selectedExpression == null) {
				String message= RefactoringCoreMessages.ExtractTempRefactoring_select_expression;
				return CodeRefactoringUtil.checkMethodSyntaxErrors(fSelectionStart, fSelectionLength, fCompilationUnitNode, message);
			}
			pm.worked(1);

			if (isUsedInExplicitConstructorCall())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_explicit_constructor); 
			pm.worked(1);

			if (getEnclosingBodyNode() == null)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_expr_in_method_or_initializer); 
			pm.worked(1);

			ASTNode associatedNode= selectedExpression.getAssociatedNode();
			if (associatedNode instanceof Name && associatedNode.getParent() instanceof ClassInstanceCreation && associatedNode.getLocationInParent() == ClassInstanceCreation.TYPE_PROPERTY)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_name_in_new); 
			pm.worked(1);

			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkExpression());
			if (result.hasFatalError())
				return result;
			pm.worked(1);

			result.merge(checkExpressionFragmentIsRValue());
			if (result.hasFatalError())
				return result;
			pm.worked(1);

			if (isUsedInForInitializerOrUpdater(getSelectedExpression().getAssociatedExpression()))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_for_initializer_updater); 
			pm.worked(1);

			if (isReferringToLocalVariableFromFor(getSelectedExpression().getAssociatedExpression()))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractTempRefactoring_refers_to_for_variable); 
			pm.worked(1);

			return result;
		} finally {
			pm.done();
		}
	}

	public RefactoringStatus checkTempName(String newName) {
		RefactoringStatus status= Checks.checkTempName(newName);
		if (Arrays.asList(getExcludedVariableNames()).contains(newName))
			status.addWarning(Messages.format(RefactoringCoreMessages.ExtractTempRefactoring_another_variable, newName)); 
		return status;
	}

	private void createAndInsertTempDeclaration() throws CoreException {
		Expression initializer= getSelectedExpression().createCopyTarget(fCURewrite.getASTRewrite());
		VariableDeclarationStatement vds= createTempDeclaration(initializer);
		
		if ((!fReplaceAllOccurrences) || (retainOnlyReplacableMatches(getMatchingFragments()).length <= 1)) {
			insertAt(getSelectedExpression().getAssociatedNode(), vds);
			return;
		}
		
		ASTNode[] firstReplaceNodeParents= getParents(getFirstReplacedExpression().getAssociatedNode());
		ASTNode[] commonPath= findDeepestCommonSuperNodePathForReplacedNodes();
		Assert.isTrue(commonPath.length <= firstReplaceNodeParents.length);
		
		ASTNode deepestCommonParent= firstReplaceNodeParents[commonPath.length - 1];
		if (deepestCommonParent instanceof Block)
			insertAt(firstReplaceNodeParents[commonPath.length], vds);
		else
			insertAt(deepestCommonParent, vds);
	}

	private VariableDeclarationStatement createTempDeclaration(Expression initializer) throws CoreException {
		AST ast= fCURewrite.getAST();
		
		VariableDeclarationFragment vdf= ast.newVariableDeclarationFragment();
		vdf.setName(ast.newSimpleName(fTempName));
		vdf.setInitializer(initializer);
		
		VariableDeclarationStatement vds= ast.newVariableDeclarationStatement(vdf);
//		if (fDeclareFinal) {
//			vds.modifiers().add(ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
//		}
//		vds.setType(createTempType());
		
		if (fLinkedProposalModel != null) {
			ASTRewrite rewrite= fCURewrite.getASTRewrite();
			LinkedProposalPositionGroup nameGroup= fLinkedProposalModel.getPositionGroup(KEY_NAME, true);
			nameGroup.addPosition(rewrite.track(vdf.getName()), true);
			
			String[] nameSuggestions= guessTempNames();
			if (nameSuggestions.length > 0 && !nameSuggestions[0].equals(fTempName)) {
				nameGroup.addProposal(fTempName, null, nameSuggestions.length + 1);
			}
			for (int i= 0; i < nameSuggestions.length; i++) {
				nameGroup.addProposal(nameSuggestions[i], null, nameSuggestions.length - i);
			}
		}
		return vds;
	}

	private void insertAt(ASTNode target, Statement declaration) throws JavaScriptModelException {
		ASTRewrite rewrite= fCURewrite.getASTRewrite();
		TextEditGroup groupDescription= fCURewrite.createGroupDescription(RefactoringCoreMessages.ExtractTempRefactoring_declare_local_variable);
		
		ASTNode parent= target.getParent();
		while (! (parent instanceof Block)) {
			StructuralPropertyDescriptor locationInParent= target.getLocationInParent();
			if (locationInParent == IfStatement.THEN_STATEMENT_PROPERTY
					|| locationInParent == IfStatement.ELSE_STATEMENT_PROPERTY
					|| locationInParent == ForStatement.BODY_PROPERTY
					|| locationInParent == ForInStatement.BODY_PROPERTY
					|| locationInParent == EnhancedForStatement.BODY_PROPERTY
					|| locationInParent == DoStatement.BODY_PROPERTY
					|| locationInParent == WhileStatement.BODY_PROPERTY
					|| locationInParent == WithStatement.BODY_PROPERTY
					) {
				Block replacement= rewrite.getAST().newBlock();
				ListRewrite replacementRewrite= rewrite.getListRewrite(replacement, Block.STATEMENTS_PROPERTY);
				replacementRewrite.insertFirst(declaration, null);
				replacementRewrite.insertLast(rewrite.createMoveTarget(target), null);
				rewrite.replace(target, replacement, groupDescription);
				return;
			}
			target= parent;
			parent= parent.getParent();
		}
		ListRewrite listRewrite= rewrite.getListRewrite(parent, Block.STATEMENTS_PROPERTY);
		listRewrite.insertBefore(declaration, target, groupDescription);
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.ExtractTempRefactoring_checking_preconditions, 1);
			JDTRefactoringDescriptor descriptor= createRefactoringDescriptor();
			return new RefactoringDescriptorChange(descriptor, RefactoringCoreMessages.ExtractTempRefactoring_extract_temp, new Change[] { fChange});
		} finally {
			pm.done();
		}
	}
	
	private void createTempDeclaration() throws CoreException {
		if (shouldReplaceSelectedExpressionWithTempDeclaration())
			replaceSelectedExpressionWithTempDeclaration();
		else
			createAndInsertTempDeclaration();
	}

	public boolean declareFinal() {
		return fDeclareFinal;
	}

	private ASTNode[] findDeepestCommonSuperNodePathForReplacedNodes() throws JavaScriptModelException {
		ASTNode[] matchNodes= getMatchNodes();

		ASTNode[][] matchingNodesParents= new ASTNode[matchNodes.length][];
		for (int i= 0; i < matchNodes.length; i++) {
			matchingNodesParents[i]= getParents(matchNodes[i]);
		}
		List l= Arrays.asList(getLongestArrayPrefix(matchingNodesParents));
		return (ASTNode[]) l.toArray(new ASTNode[l.size()]);
	}

	private Block getEnclosingBodyNode() throws JavaScriptModelException {
		ASTNode node= getSelectedExpression().getAssociatedNode();
		do {
			switch (node.getNodeType()) {
				case ASTNode.FUNCTION_DECLARATION:
					return ((FunctionDeclaration) node).getBody();
				case ASTNode.INITIALIZER:
					return ((Initializer) node).getBody();
			}
			node= node.getParent();
		} while (node != null);
		return null;
	}

	private String[] getExcludedVariableNames() {
		if (fExcludedVariableNames == null) {
			try {
				IBinding[] bindings= new ScopeAnalyzer(fCompilationUnitNode).getDeclarationsInScope(getSelectedExpression().getStartPosition(), ScopeAnalyzer.VARIABLES);
				fExcludedVariableNames= new String[bindings.length];
				for (int i= 0; i < bindings.length; i++) {
					fExcludedVariableNames[i]= bindings[i].getName();
				}
			} catch (JavaScriptModelException e) {
				fExcludedVariableNames= new String[0];
			}
		}
		return fExcludedVariableNames;
	}

	private IExpressionFragment getFirstReplacedExpression() throws JavaScriptModelException {
		if (!fReplaceAllOccurrences)
			return getSelectedExpression();
		IASTFragment[] nodesToReplace= retainOnlyReplacableMatches(getMatchingFragments());
		if (nodesToReplace.length == 0)
			return getSelectedExpression();
		Comparator comparator= new Comparator() {

			public int compare(Object o1, Object o2) {
				return ((IASTFragment) o1).getStartPosition() - ((IASTFragment) o2).getStartPosition();
			}
		};
		Arrays.sort(nodesToReplace, comparator);
		return (IExpressionFragment) nodesToReplace[0];
	}

	private IASTFragment[] getMatchingFragments() throws JavaScriptModelException {
		if (fReplaceAllOccurrences) {
			IASTFragment[] allMatches= ASTFragmentFactory.createFragmentForFullSubtree(getEnclosingBodyNode()).getSubFragmentsMatching(getSelectedExpression());
			return allMatches;
		} else
			return new IASTFragment[] { getSelectedExpression()};
	}

	private ASTNode[] getMatchNodes() throws JavaScriptModelException {
		IASTFragment[] matches= retainOnlyReplacableMatches(getMatchingFragments());
		ASTNode[] result= new ASTNode[matches.length];
		for (int i= 0; i < matches.length; i++)
			result[i]= matches[i].getAssociatedNode();
		return result;
	}

	public String getName() {
		return RefactoringCoreMessages.ExtractTempRefactoring_name; 
	}


	private IExpressionFragment getSelectedExpression() throws JavaScriptModelException {
		if (fSelectedExpression != null)
			return fSelectedExpression;
		IASTFragment selectedFragment= ASTFragmentFactory.createFragmentForSourceRange(new SourceRange(fSelectionStart, fSelectionLength), fCompilationUnitNode, fCu);

		if (selectedFragment instanceof IExpressionFragment && !Checks.isInsideJavadoc(selectedFragment.getAssociatedNode())) {
			fSelectedExpression= (IExpressionFragment) selectedFragment;
		} else if (selectedFragment != null) {
			if (selectedFragment.getAssociatedNode() instanceof ExpressionStatement) {
				ExpressionStatement exprStatement= (ExpressionStatement) selectedFragment.getAssociatedNode();
				Expression expression= exprStatement.getExpression();
				fSelectedExpression= (IExpressionFragment) ASTFragmentFactory.createFragmentForFullSubtree(expression);
			} else if (selectedFragment.getAssociatedNode() instanceof Assignment) {
				Assignment assignment= (Assignment) selectedFragment.getAssociatedNode();
				fSelectedExpression= (IExpressionFragment) ASTFragmentFactory.createFragmentForFullSubtree(assignment);
			}
		}
		
		return fSelectedExpression;
	}

//	private Type createTempType() throws CoreException {
//		Expression expression= getSelectedExpression().getAssociatedExpression();
//		
//		Type resultingType= null;
//		ITypeBinding typeBinding= expression.resolveTypeBinding();
//		
//		ASTRewrite rewrite= fCURewrite.getASTRewrite();
//		AST ast= rewrite.getAST();
//		
//		if (expression instanceof ClassInstanceCreation) {
//			resultingType= (Type) rewrite.createCopyTarget(((ClassInstanceCreation) expression).getType());
//		} else if (expression instanceof CastExpression) {
//			resultingType= (Type) rewrite.createCopyTarget(((CastExpression) expression).getType());
//		} else {
//			if (typeBinding == null) {
//				typeBinding= ASTResolving.guessBindingForReference(expression);
//			}
//			if (typeBinding != null) {
//				typeBinding= Bindings.normalizeForDeclarationUse(typeBinding, ast);
//				resultingType= fCURewrite.getImportRewrite().addImport(typeBinding, ast);
//			} else {
//				resultingType= ast.newSimpleType(ast.newSimpleName("Object")); //$NON-NLS-1$
//			}
//		}
//		if (fLinkedProposalModel != null) {
//			LinkedProposalPositionGroup typeGroup= fLinkedProposalModel.getPositionGroup(KEY_TYPE, true);
//			typeGroup.addPosition(rewrite.track(resultingType), false);
//			if (typeBinding != null) {
//				ITypeBinding[] relaxingTypes= ASTResolving.getNarrowingTypes(ast, typeBinding);
//				for (int i= 0; i < relaxingTypes.length; i++) {
//					typeGroup.addProposal(relaxingTypes[i], fCURewrite.getCu(), relaxingTypes.length - i);
//				}
//			}
//		}
//		return resultingType;
//	}

	public String guessTempName() {
		String[] proposals= guessTempNames();
		if (proposals.length == 0)
			return fTempName;
		else
			return proposals[0];
	}

	/**
	 * @return proposed variable names (may be empty, but not null). The first proposal should be used as "best guess" (if it exists).
	 */
	public String[] guessTempNames() {
		if (fGuessedTempNames == null) {
			try {
				Expression expression= getSelectedExpression().getAssociatedExpression();
				if (expression != null) {
					ITypeBinding binding= expression.resolveTypeBinding();
					fGuessedTempNames= StubUtility.getVariableNameSuggestions(StubUtility.LOCAL, fCu.getJavaScriptProject(), binding, expression, Arrays.asList(getExcludedVariableNames()));
				} 
			} catch (JavaScriptModelException e) {
			}
			if (fGuessedTempNames == null)
				fGuessedTempNames= new String[0];
		}
		return fGuessedTempNames;
	}

	private boolean isLiteralNodeSelected() throws JavaScriptModelException {
		IExpressionFragment fragment= getSelectedExpression();
		if (fragment == null)
			return false;
		Expression expression= fragment.getAssociatedExpression();
		if (expression == null)
			return false;
		switch (expression.getNodeType()) {
			case ASTNode.BOOLEAN_LITERAL:
			case ASTNode.CHARACTER_LITERAL:
			case ASTNode.NULL_LITERAL:
			case ASTNode.NUMBER_LITERAL:
			case ASTNode.REGULAR_EXPRESSION_LITERAL:
			case ASTNode.UNDEFINED_LITERAL:
				return true;

			default:
				return false;
		}
	}

	private boolean isUsedInExplicitConstructorCall() throws JavaScriptModelException {
		Expression selectedExpression= getSelectedExpression().getAssociatedExpression();
		if (ASTNodes.getParent(selectedExpression, ConstructorInvocation.class) != null)
			return true;
		if (ASTNodes.getParent(selectedExpression, SuperConstructorInvocation.class) != null)
			return true;
		return false;
	}

	public boolean replaceAllOccurrences() {
		return fReplaceAllOccurrences;
	}

	private void replaceSelectedExpressionWithTempDeclaration() throws CoreException {
		ASTRewrite rewrite= fCURewrite.getASTRewrite();
		Expression selectedExpression= getSelectedExpression().getAssociatedExpression(); // whole expression selected
		
		Expression initializer= (Expression) rewrite.createMoveTarget(selectedExpression);
		ASTNode replacement= createTempDeclaration(initializer); // creates a VariableDeclarationStatement
		
		ExpressionStatement parent= (ExpressionStatement) selectedExpression.getParent();
		if (ASTNodes.isControlStatementBody(parent.getLocationInParent())) {
			Block block= rewrite.getAST().newBlock();
			block.statements().add(replacement);
			replacement= block;
			
		}
		rewrite.replace(parent, replacement, fCURewrite.createGroupDescription(RefactoringCoreMessages.ExtractTempRefactoring_declare_local_variable));
	}

	public void setDeclareFinal(boolean declareFinal) {
		fDeclareFinal= declareFinal;
	}

	public void setReplaceAllOccurrences(boolean replaceAllOccurrences) {
		fReplaceAllOccurrences= replaceAllOccurrences;
	}

	public void setTempName(String newName) {
		fTempName= newName;
	}

	private boolean shouldReplaceSelectedExpressionWithTempDeclaration() throws JavaScriptModelException {
		IExpressionFragment selectedFragment= getSelectedExpression();
		return selectedFragment.getAssociatedNode().getParent() instanceof ExpressionStatement
			&& selectedFragment.matches(ASTFragmentFactory.createFragmentForFullSubtree(selectedFragment.getAssociatedNode()));
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
					return createInputFatalStatus(element, IJavaScriptRefactorings.EXTRACT_LOCAL_VARIABLE);
				else
					fCu= (IJavaScriptUnit) element;
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
			final String name= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_NAME);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				fTempName= name;
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_NAME));
			final String replace= extended.getAttribute(ATTRIBUTE_REPLACE);
			if (replace != null) {
				fReplaceAllOccurrences= Boolean.valueOf(replace).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_REPLACE));
			final String declareFinal= extended.getAttribute(ATTRIBUTE_FINAL);
			if (declareFinal != null) {
				fDeclareFinal= Boolean.valueOf(declareFinal).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_FINAL));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}
}
