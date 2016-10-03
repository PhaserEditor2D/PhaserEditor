/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Renaud Waldura &lt;renaud+eclipse@waldura.com&gt; - Access to static proposal
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.ISharedImages;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.CatchClause;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.eclipse.wst.jsdt.core.dom.InfixExpression;
import org.eclipse.wst.jsdt.core.dom.Initializer;
import org.eclipse.wst.jsdt.core.dom.InstanceofExpression;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.SwitchCase;
import org.eclipse.wst.jsdt.core.dom.SwitchStatement;
import org.eclipse.wst.jsdt.core.dom.TryStatement;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.Selection;
import org.eclipse.wst.jsdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.wst.jsdt.internal.corext.fix.CodeStyleFix;
import org.eclipse.wst.jsdt.internal.corext.fix.IFix;
import org.eclipse.wst.jsdt.internal.corext.fix.StringFix;
import org.eclipse.wst.jsdt.internal.corext.fix.UnusedCodeFix;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.nls.NLSRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.surround.ExceptionAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.fix.CodeStyleCleanUp;
import org.eclipse.wst.jsdt.internal.ui.fix.StringCleanUp;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.wst.jsdt.internal.ui.refactoring.nls.ExternalizeWizard;
import org.eclipse.wst.jsdt.internal.ui.text.correction.ChangeMethodSignatureProposal.ChangeDescription;
import org.eclipse.wst.jsdt.internal.ui.text.correction.ChangeMethodSignatureProposal.InsertDescription;
import org.eclipse.wst.jsdt.internal.ui.text.correction.ChangeMethodSignatureProposal.RemoveDescription;
import org.eclipse.wst.jsdt.ui.text.java.IInvocationContext;
import org.eclipse.wst.jsdt.ui.text.java.IProblemLocation;

/**
  */
public class LocalCorrectionsSubProcessor {

	private static final String ADD_EXCEPTION_TO_THROWS_ID= "org.eclipse.wst.jsdt.ui.correction.addThrowsDecl"; //$NON-NLS-1$
	private static final String ADD_NON_NLS_ID= "org.eclipse.wst.jsdt.ui.correction.addNonNLS"; //$NON-NLS-1$
	private static final String ADD_STATIC_ACCESS_ID= "org.eclipse.wst.jsdt.ui.correction.changeToStatic"; //$NON-NLS-1$
	private static final String REMOVE_UNNECESSARY_NLS_TAG_ID= "org.eclipse.wst.jsdt.ui.correction.removeNlsTag"; //$NON-NLS-1$
	
	public static void addUncaughtExceptionProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		IJavaScriptUnit cu= context.getCompilationUnit();

		JavaScriptUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (selectedNode == null) {
			return;
		}
		while (selectedNode != null && !(selectedNode instanceof Statement)) {
			selectedNode= selectedNode.getParent();
		}
		if (selectedNode == null) {
			return;
		}

		int offset= selectedNode.getStartPosition();
		int length= selectedNode.getLength();
		int selectionEnd= context.getSelectionOffset() + context.getSelectionLength();
		if (selectionEnd > offset + length) {
			// extend the selection if more than one statement is selected (bug 72149)
			length= selectionEnd - offset;
		}

		SurroundWithTryCatchRefactoring refactoring= SurroundWithTryCatchRefactoring.create(cu, offset, length, null);
		if (refactoring == null)
			return;

		refactoring.setLeaveDirty(true);
		if (refactoring.checkActivationBasics(astRoot).isOK()) {
			String label= CorrectionMessages.LocalCorrectionsSubProcessor_surroundwith_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			CUCorrectionProposal proposal= new CUCorrectionProposal(label, cu, (CompilationUnitChange) refactoring.createChange(null), 6, image);
			proposals.add(proposal);
		}

		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (decl == null) {
			return;
		}

		ITypeBinding[] uncaughtExceptions= ExceptionAnalyzer.perform(decl, Selection.createFromStartLength(offset, length));
		if (uncaughtExceptions.length == 0) {
			return;
		}

		TryStatement surroundingTry= ASTResolving.findParentTryStatement(selectedNode);
		if (surroundingTry != null && ASTNodes.isParent(selectedNode, surroundingTry.getBody())) {
			ASTRewrite rewrite= ASTRewrite.create(surroundingTry.getAST());

			String label= CorrectionMessages.LocalCorrectionsSubProcessor_addadditionalcatch_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, 7, image);
			
			ImportRewrite imports= proposal.createImportRewrite(context.getASTRoot());

			AST ast= astRoot.getAST();
			ListRewrite clausesRewrite= rewrite.getListRewrite(surroundingTry, TryStatement.CATCH_CLAUSES_PROPERTY);
			for (int i= 0; i < uncaughtExceptions.length; i++) {
				ITypeBinding excBinding= uncaughtExceptions[i];
				String varName= StubUtility.getExceptionVariableName(cu.getJavaScriptProject());
				SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
				var.setName(ast.newSimpleName(varName));
				var.setType(imports.addImport(excBinding, ast));
				CatchClause newClause= ast.newCatchClause();
				newClause.setException(var);
				String catchBody = StubUtility.getCatchBodyContent(cu, excBinding.getName(), varName, selectedNode, String.valueOf('\n'));
				if (catchBody != null) {
					ASTNode node= rewrite.createStringPlaceholder(catchBody, ASTNode.RETURN_STATEMENT);
					newClause.getBody().statements().add(node);
				}
				clausesRewrite.insertLast(newClause, null);

				String typeKey= "type" + i; //$NON-NLS-1$
				String nameKey= "name" + i; //$NON-NLS-1$
				proposal.addLinkedPosition(rewrite.track(var.getType()), false, typeKey);
				proposal.addLinkedPosition(rewrite.track(var.getName()), false, nameKey);
				addExceptionTypeLinkProposals(proposal, excBinding, typeKey);
			}
			proposals.add(proposal);
		}

		if (decl instanceof FunctionDeclaration) {
			FunctionDeclaration methodDecl= (FunctionDeclaration) decl;
			IFunctionBinding binding= methodDecl.resolveBinding();
			if (binding != null) {
				ArrayList unhandledExceptions= new ArrayList(uncaughtExceptions.length);
				for (int i= 0; i < uncaughtExceptions.length; i++) {
					ITypeBinding curr= uncaughtExceptions[i];
					if (!canRemoveException(curr, null)) {
						unhandledExceptions.add(curr);
					}
				}
				uncaughtExceptions= (ITypeBinding[]) unhandledExceptions.toArray(new ITypeBinding[unhandledExceptions.size()]);

				List exceptions= methodDecl.thrownExceptions();
				int nExistingExceptions= exceptions.size();
				ChangeDescription[] desc= new ChangeDescription[nExistingExceptions + uncaughtExceptions.length];
				for (int i= 0; i < exceptions.size(); i++) {
					Name elem= (Name) exceptions.get(i);
					if (canRemoveException(elem.resolveTypeBinding(), uncaughtExceptions)) {
						desc[i]= new RemoveDescription();
					}
				}
				for (int i = 0; i < uncaughtExceptions.length; i++) {
					desc[i + nExistingExceptions]= new InsertDescription(uncaughtExceptions[i], ""); //$NON-NLS-1$
				}

				String label= CorrectionMessages.LocalCorrectionsSubProcessor_addthrows_description;
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);

				ChangeMethodSignatureProposal proposal= new ChangeMethodSignatureProposal(label, cu, astRoot, binding, null, desc, 8, image);
				for (int i= 0; i < uncaughtExceptions.length; i++) {
					addExceptionTypeLinkProposals(proposal, uncaughtExceptions[i], proposal.getExceptionTypeGroupId(i + nExistingExceptions));
				}
				proposal.setCommandId(ADD_EXCEPTION_TO_THROWS_ID);
				proposals.add(proposal);
			}
		}
	}

	private static void addExceptionTypeLinkProposals(LinkedCorrectionProposal proposal, ITypeBinding exc, String key) {
		// all super classes except Object
		while (exc != null && !"java.lang.Object".equals(exc.getQualifiedName())) { //$NON-NLS-1$
			proposal.addLinkedPositionProposal(key, exc);
			exc= exc.getSuperclass();
		}
	}


	private static boolean canRemoveException(ITypeBinding curr, ITypeBinding[] addedExceptions) {
		while (curr != null) {
			for (int i= 0; i < addedExceptions.length; i++) {
				if (curr == addedExceptions[i]) {
					return true;
				}
			}
			curr= curr.getSuperclass();
		}
		return false;
	}

	public static void addUnreachableCatchProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}

		QuickAssistProcessor.getCatchClauseToThrowsProposals(context, selectedNode, proposals);
	}

	public static void addNLSProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		final IJavaScriptUnit cu= context.getCompilationUnit();
		if (cu == null || !cu.exists()){
			return;
		}
		String name= CorrectionMessages.LocalCorrectionsSubProcessor_externalizestrings_description;

		ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(name, null, 2, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)) {
			public void apply(IDocument document) {
				try {
					NLSRefactoring refactoring= NLSRefactoring.create(cu);
					if (refactoring == null)
						return;
					ExternalizeWizard wizard= new ExternalizeWizard(refactoring);
					String dialogTitle= CorrectionMessages.LocalCorrectionsSubProcessor_externalizestrings_dialog_title;
					new RefactoringStarter().activate(refactoring, wizard, JavaScriptPlugin.getActiveWorkbenchShell(), dialogTitle, RefactoringSaveHelper.SAVE_NON_JAVA_UPDATES);
				} catch (JavaScriptModelException e) {
					JavaScriptPlugin.log(e);
				}
			}
			public String getAdditionalProposalInfo() {
				return CorrectionMessages.LocalCorrectionsSubProcessor_externalizestrings_additional_info;
			}
			
		};
		proposals.add(proposal);
		
		IFix fix= StringFix.createFix(context.getASTRoot(), problem, false, true);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_NLS_NEVER_TRANSLATE);
			Map options= new Hashtable();
			options.put(CleanUpConstants.ADD_MISSING_NLS_TAGS, CleanUpConstants.TRUE);
			FixCorrectionProposal addNLS= new FixCorrectionProposal(fix, new StringCleanUp(options), 3, image, context);
			addNLS.setCommandId(ADD_NON_NLS_ID);
			proposals.add(addNLS);
		}
	}
	
	public static void getUnnecessaryNLSTagProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		IFix fix= StringFix.createFix(context.getASTRoot(), problem, true, false);
		if (fix != null) {
			Image image= JavaScriptPlugin.getDefault().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
			Map options= new Hashtable();
			options.put(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS, CleanUpConstants.TRUE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new StringCleanUp(options), 6, image, context);
			proposal.setCommandId(REMOVE_UNNECESSARY_NLS_TAG_ID);
			proposals.add(proposal);
		}
	}
	

	/*
	 * Fix instance accesses and indirect (static) accesses to static fields/methods
	 */
	public static void addCorrectAccessToStaticProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		IFix fix= CodeStyleFix.createIndirectAccessToStaticFix(context.getASTRoot(), problem);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			Map options= new HashMap();
			options.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS, CleanUpConstants.TRUE);
			options.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS, CleanUpConstants.TRUE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new CodeStyleCleanUp(options), 6, image, context);
			proposal.setCommandId(ADD_STATIC_ACCESS_ID);
			proposals.add(proposal);
			return;
		}
		
		IFix[] fixes= CodeStyleFix.createNonStaticAccessFixes(context.getASTRoot(), problem);
		if (fixes != null) {
			IFix fix1= fixes[0];
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			Map options= new HashMap();
			options.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS, CleanUpConstants.TRUE);
			options.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS, CleanUpConstants.TRUE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix1, new CodeStyleCleanUp(options), 6, image, context);
			proposal.setCommandId(ADD_STATIC_ACCESS_ID);
			proposals.add(proposal);
			
			if (fixes.length > 1) {
				Map options1= new HashMap();
				options1.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS, CleanUpConstants.TRUE);
				options1.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS, CleanUpConstants.TRUE);
				options1.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS, CleanUpConstants.TRUE);
				IFix fix2= fixes[1];
				proposal= new FixCorrectionProposal(fix2, new CodeStyleCleanUp(options), 5, image, context);
				proposals.add(proposal);
			}
		}
		ModifierCorrectionSubProcessor.addNonAccessibleReferenceProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_NON_STATIC, 4);
	}

	public static void addUnimplementedMethodsProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		IJavaScriptUnit cu= context.getCompilationUnit();
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}
		ASTNode typeNode= null;
		ITypeBinding binding= null;
		if (selectedNode.getNodeType() == ASTNode.SIMPLE_NAME && selectedNode.getParent() instanceof AbstractTypeDeclaration) {
			AbstractTypeDeclaration typeDecl= (AbstractTypeDeclaration) selectedNode.getParent();
			binding= typeDecl.resolveBinding();
			typeNode= typeDecl;
		} else if (selectedNode.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
			ClassInstanceCreation creation= (ClassInstanceCreation) selectedNode;
			AnonymousClassDeclaration anonymDecl= creation.getAnonymousClassDeclaration();
			binding= anonymDecl.resolveBinding();
			typeNode= anonymDecl;
		}
		if (typeNode != null && binding != null) {
			UnimplementedMethodsCompletionProposal proposal= new UnimplementedMethodsCompletionProposal(cu, typeNode, 10);
			proposals.add(proposal);
		}
		if (typeNode instanceof TypeDeclaration) {
			TypeDeclaration typeDeclaration= (TypeDeclaration) typeNode;
			ASTRewriteCorrectionProposal proposal= ModifierCorrectionSubProcessor.getMakeTypeAbstractProposal(cu, typeDeclaration, 5);
			proposals.add(proposal);
		}
	}

	public static void addUninitializedLocalVariableProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		IJavaScriptUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (!(selectedNode instanceof Name)) {
			return;
		}
		Name name= (Name) selectedNode;
		IBinding binding= name.resolveBinding();
		if (!(binding instanceof IVariableBinding)) {
			return;
		}
		IVariableBinding varBinding= (IVariableBinding) binding;

		JavaScriptUnit astRoot= context.getASTRoot();
		ASTNode node= astRoot.findDeclaringNode(binding);
		if (node instanceof VariableDeclarationFragment) {
			ASTRewrite rewrite= ASTRewrite.create(node.getAST());

			VariableDeclarationFragment fragment= (VariableDeclarationFragment) node;
			if (fragment.getInitializer() != null) {
				return;
			}
			Expression expression= ASTNodeFactory.newDefaultExpression(astRoot.getAST(), varBinding.getType());
			if (expression == null) {
				return;
			}
			rewrite.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, expression, null);

			String label= CorrectionMessages.LocalCorrectionsSubProcessor_uninitializedvariable_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);

			LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, 6, image);
			proposal.addLinkedPosition(rewrite.track(expression), false, "initializer"); //$NON-NLS-1$
			proposals.add(proposal);
		}
	}

	public static void addConstructorFromSuperclassProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}
		
		TypeDeclaration typeDeclaration= null;
		if (selectedNode.getLocationInParent() == TypeDeclaration.NAME_PROPERTY) {
			typeDeclaration= (TypeDeclaration) selectedNode.getParent();
		} else {
			BodyDeclaration declaration= ASTResolving.findParentBodyDeclaration(selectedNode);
			if (declaration instanceof Initializer && problem.getProblemId() == IProblem.UnhandledExceptionInDefaultConstructor) {
				addUncaughtExceptionProposals(context, problem, proposals);
			}
			return;
		}
		
		ITypeBinding binding= typeDeclaration.resolveBinding();
		if (binding == null || binding.getSuperclass() == null) {
			return;
		}
		IJavaScriptUnit cu= context.getCompilationUnit();
		IFunctionBinding[] methods= binding.getSuperclass().getDeclaredMethods();
		for (int i= 0; i < methods.length; i++) {
			IFunctionBinding curr= methods[i];
			if (curr.isConstructor() && !Modifier.isPrivate(curr.getModifiers())) {
				proposals.add(new ConstructorFromSuperclassProposal(cu, typeDeclaration, curr, 5));
			}
		}
	}

	public static void addUnusedMemberProposal(IInvocationContext context, IProblemLocation problem,  Collection proposals) {
		int problemId = problem.getProblemId();
		UnusedCodeFix fix= UnusedCodeFix.createUnusedMemberFix(context.getASTRoot(), problem, false);
		if (fix != null) {
			addProposal(context, proposals, fix);
		}
		
		if (problemId==IProblem.LocalVariableIsNeverUsed){
			fix= UnusedCodeFix.createUnusedMemberFix(context.getASTRoot(), problem, true);
			addProposal(context, proposals, fix);
		}
		
	}

	private static void addProposal(IInvocationContext context, Collection proposals, final UnusedCodeFix fix) {
		if (fix != null) {
			Image image= JavaScriptPlugin.getDefault().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, fix.getCleanUp(), 10, image, context);
			proposals.add(proposal);
		}
	}

	public static void addSuperfluousSemicolonProposal(IInvocationContext context, IProblemLocation problem,  Collection proposals) {
		String label= CorrectionMessages.LocalCorrectionsSubProcessor_removesemicolon_description;
		ReplaceCorrectionProposal proposal= new ReplaceCorrectionProposal(label, context.getCompilationUnit(), problem.getOffset(), problem.getLength(), "", 6); //$NON-NLS-1$
		proposals.add(proposal);
	}

	public static void addUnnecessaryInstanceofProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());

		ASTNode curr= selectedNode;
		while (curr instanceof ParenthesizedExpression) {
			curr= ((ParenthesizedExpression) curr).getExpression();
		}

		if (curr instanceof InstanceofExpression) {
			AST ast= curr.getAST();

			ASTRewrite rewrite= ASTRewrite.create(ast);

			InstanceofExpression inst= (InstanceofExpression) curr;

			InfixExpression expression= ast.newInfixExpression();
			expression.setLeftOperand((Expression) rewrite.createCopyTarget(inst.getLeftOperand()));
			expression.setOperator(InfixExpression.Operator.NOT_EQUALS);
			expression.setRightOperand(ast.newNullLiteral());


			if (false/*ASTNodes.needsParentheses(expression)*/) {
				ParenthesizedExpression parents= ast.newParenthesizedExpression();
				parents.setExpression(expression);
				rewrite.replace(inst, parents, null);
			} else {
				rewrite.replace(inst, expression, null);
			}

			String label= CorrectionMessages.LocalCorrectionsSubProcessor_unnecessaryinstanceof_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image);
			proposals.add(proposal);
		}

	}

	public static void addUnnecessaryThrownExceptionProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null || !(selectedNode.getParent() instanceof FunctionDeclaration)) {
			return;
		}
		FunctionDeclaration decl= (FunctionDeclaration) selectedNode.getParent();
		IFunctionBinding binding= decl.resolveBinding();
		if (binding != null) {
			List thrownExceptions= decl.thrownExceptions();
			int index= thrownExceptions.indexOf(selectedNode);
			if (index == -1) {
				return;
			}
			ChangeDescription[] desc= new ChangeDescription[thrownExceptions.size()];
			desc[index]= new RemoveDescription();

			IJavaScriptUnit cu= context.getCompilationUnit();
			String label= CorrectionMessages.LocalCorrectionsSubProcessor_unnecessarythrow_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);

			proposals.add(new ChangeMethodSignatureProposal(label, cu, selectedNode, binding, null, desc, 5, image));
		}
	}

//	public static void addUnqualifiedFieldAccessProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
//		IFix fix= CodeStyleFix.createAddFieldQualifierFix(context.getASTRoot(), problem);
//		if (fix != null) {
//			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
//			Map options= new HashMap();
//			options.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS, CleanUpConstants.TRUE);
//			options.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS, CleanUpConstants.TRUE);
//			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new CodeStyleCleanUp(options), 5, image, context);
//			proposal.setCommandId(ADD_FIELD_QUALIFICATION_ID);
//			proposals.add(proposal);
//		}
//	}
//
	public static void addInvalidVariableNameProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		// hiding, redefined or future keyword

		JavaScriptUnit root= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (selectedNode instanceof FunctionDeclaration) {
			selectedNode= ((FunctionDeclaration) selectedNode).getName();
		}
		if (!(selectedNode instanceof SimpleName)) {
			return;
		}
		SimpleName nameNode= (SimpleName) selectedNode;
		String valueSuggestion= null;

		String name;
		switch (problem.getProblemId()) {
			case IProblem.LocalVariableHidingLocalVariable:
			case IProblem.LocalVariableHidingField:
				name= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_hiding_local_label, nameNode.getIdentifier());
				break;
			case IProblem.FieldHidingLocalVariable:
			case IProblem.FieldHidingField:
			case IProblem.DuplicateField:
				name= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_hiding_field_label, nameNode.getIdentifier());
				break;
			case IProblem.ArgumentHidingLocalVariable:
			case IProblem.ArgumentHidingField:
				name= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_hiding_argument_label, nameNode.getIdentifier());
				break;
			case IProblem.DuplicateMethod:
				name= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_renaming_duplicate_method, nameNode.getIdentifier());
				break;
				
			default:
				name= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_rename_var_label, nameNode.getIdentifier());
		}

		if (problem.getProblemId() == IProblem.UseEnumAsAnIdentifier) {
			valueSuggestion= "enumeration"; //$NON-NLS-1$
		} else {
			valueSuggestion= nameNode.getIdentifier() + '1';
		}

		LinkedNamesAssistProposal proposal= new LinkedNamesAssistProposal(name, context.getCompilationUnit(), nameNode, valueSuggestion);
		proposals.add(proposal);
	}

	public static void getInvalidOperatorProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		JavaScriptUnit root= context.getASTRoot();
		AST ast= root.getAST();

		ASTNode selectedNode= problem.getCoveringNode(root);

		while (selectedNode instanceof ParenthesizedExpression) {
			selectedNode= ((ParenthesizedExpression) selectedNode).getExpression();
		}

		if (selectedNode instanceof PrefixExpression) {
			// !x instanceof X -> !(x instanceof X)

			PrefixExpression expression= (PrefixExpression) selectedNode;
			if (expression.getOperator() == PrefixExpression.Operator.NOT) {
				ASTNode parent= expression.getParent();

				String label= null;
				switch (parent.getNodeType()) {
					case ASTNode.INSTANCEOF_EXPRESSION:
						label= CorrectionMessages.LocalCorrectionsSubProcessor_setparenteses_instanceof_description;
						break;
					case ASTNode.INFIX_EXPRESSION:
						label= CorrectionMessages.LocalCorrectionsSubProcessor_setparenteses_description;
						break;
				}

				if (label != null) {
					ASTRewrite rewrite= ASTRewrite.create(ast);
					rewrite.replace(selectedNode, rewrite.createMoveTarget(expression.getOperand()), null);

					ParenthesizedExpression newParentExpr= ast.newParenthesizedExpression();
					newParentExpr.setExpression((Expression) rewrite.createMoveTarget(parent));
					PrefixExpression newPrefixExpr= ast.newPrefixExpression();
					newPrefixExpr.setOperand(newParentExpr);
					newPrefixExpr.setOperator(PrefixExpression.Operator.NOT);

					rewrite.replace(parent, newPrefixExpr, null);

					Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
					ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image); 
					proposals.add(proposal);
				}
			}
		} else if (selectedNode instanceof InfixExpression && isBitOperation((((InfixExpression) selectedNode).getOperator()))) {
			// a & b == c -> (a & b) == c
			final CompareInBitWiseOpFinder opFinder= new CompareInBitWiseOpFinder(selectedNode);
			if (opFinder.getCompareExpression() != null) { // compare operation inside bit operations: set parents			
				String label= CorrectionMessages.LocalCorrectionsSubProcessor_setparenteses_bitop_description;
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CAST);
				CUCorrectionProposal proposal= new CUCorrectionProposal(label, context.getCompilationUnit(), 5, image) {
					protected void addEdits(IDocument document, TextEdit edit) throws CoreException {
						InfixExpression compareExpression= opFinder.getCompareExpression();
						InfixExpression expression= opFinder.getParentInfixExpression();
						ASTNode left= compareExpression.getLeftOperand();
						if (expression.getStartPosition() < left.getStartPosition()) {
							edit.addChild(new InsertEdit(expression.getStartPosition(), String.valueOf('(')));
							edit.addChild(new InsertEdit(ASTNodes.getExclusiveEnd(left), String.valueOf(')')));
						}
						ASTNode rigth= compareExpression.getRightOperand();
						int selEnd= ASTNodes.getExclusiveEnd(expression);
						if (selEnd > ASTNodes.getExclusiveEnd(rigth)) {
							edit.addChild(new InsertEdit(rigth.getStartPosition(), String.valueOf('(')));
							edit.addChild(new InsertEdit(selEnd, String.valueOf(')')));
						}
					}
				};
				proposals.add(proposal);
			}
		}
	}

	private static boolean isBitOperation(InfixExpression.Operator op) {
		return op == InfixExpression.Operator.AND || op == InfixExpression.Operator.OR || op == InfixExpression.Operator.XOR;
	}

	private static class CompareInBitWiseOpFinder extends ASTVisitor {
		
		private InfixExpression fCompareExpression= null;
		private final ASTNode fSelectedNode;

		public CompareInBitWiseOpFinder(ASTNode selectedNode) {
			fSelectedNode= selectedNode;
			selectedNode.accept(this);
		}
		
		public boolean visit(InfixExpression e) {
			InfixExpression.Operator op= e.getOperator();
			if (isBitOperation(op)) {
				return true;
			} else if (op == InfixExpression.Operator.EQUALS || op == InfixExpression.Operator.NOT_EQUALS) {
				fCompareExpression= e;
				return false;
			}
			return false;
		}
		
		public InfixExpression getCompareExpression() {
			return fCompareExpression;
		}
		
		public InfixExpression getParentInfixExpression() {
			ASTNode expr= fSelectedNode;
			ASTNode parent= expr.getParent(); // include all parents
			while (parent instanceof InfixExpression && isBitOperation(((InfixExpression) parent).getOperator())) {
				expr= parent;
				parent= expr.getParent();
			}
			return (InfixExpression) expr;
		}
	}

	public static void getUnnecessaryElseProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		JavaScriptUnit root= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (selectedNode == null) {
			return;
		}
		if (!(selectedNode.getParent() instanceof IfStatement)) {
			return;
		}
		IfStatement ifStatement= (IfStatement) selectedNode.getParent();
		ASTNode ifParent= ifStatement.getParent();
		if (!(ifParent instanceof Block)) {
			return;
		}

		ASTRewrite rewrite= ASTRewrite.create(root.getAST());
		ASTNode placeholder=QuickAssistProcessor.getCopyOfInner(rewrite, ifStatement.getElseStatement(), false);
		if (placeholder == null) {
			return;
		}
		rewrite.remove(ifStatement.getElseStatement(), null);

		ListRewrite listRewrite= rewrite.getListRewrite(ifParent, Block.STATEMENTS_PROPERTY);
		listRewrite.insertAfter(placeholder, ifStatement, null);

		String label= CorrectionMessages.LocalCorrectionsSubProcessor_removeelse_description;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image); 
		proposals.add(proposal);
	}

	public static void getUnreachableCodeProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		JavaScriptUnit root= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (selectedNode == null) {
			return;
		}
		if (selectedNode.getParent() instanceof ExpressionStatement) {
			selectedNode= selectedNode.getParent();
		}
		
		if (selectedNode instanceof Statement) {
			ASTRewrite rewrite= ASTRewrite.create(selectedNode.getAST());
			rewrite.remove(selectedNode, null);
			String label= CorrectionMessages.LocalCorrectionsSubProcessor_removeunreachablecode_description;
			Image image= JavaScriptPlugin.getDefault().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 3, image); 
			proposals.add(proposal);
		}
	}

	public static void getAssignmentHasNoEffectProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		JavaScriptUnit root= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (!(selectedNode instanceof Assignment)) {
			return;
		}
		ASTNode assignedNode= ((Assignment) selectedNode).getLeftHandSide();
		ASTNode assignExpression= ((Assignment) selectedNode).getRightHandSide();
		if (!(assignedNode instanceof SimpleName) && !(assignExpression instanceof SimpleName)) {
			return;
		}
		
		IBinding binding= (assignedNode instanceof SimpleName) ? ((SimpleName) assignedNode).resolveBinding() : ((SimpleName) assignExpression).resolveBinding();
		if (!(binding instanceof IVariableBinding)) {
			return;
		}
		ITypeBinding typeBinding= Bindings.getBindingOfParentType(selectedNode);
		if (typeBinding == null)  {
			return;
		}
		IVariableBinding fieldBinding= Bindings.findFieldInHierarchy(typeBinding, binding.getName());
		if (fieldBinding == null || fieldBinding.getDeclaringClass() != typeBinding && Modifier.isPrivate(fieldBinding.getModifiers())) {
			return;
		}
		
		if (binding != fieldBinding) {
			if (assignedNode instanceof SimpleName) {
				String label= CorrectionMessages.LocalCorrectionsSubProcessor_qualify_left_hand_side_description;
				proposals.add(createNoSideEffectProposal(context, (SimpleName) assignedNode, fieldBinding, label, 6));
			}
			if (assignExpression instanceof SimpleName) {
				String label= CorrectionMessages.LocalCorrectionsSubProcessor_LocalCorrectionsSubProcessor_qualify_right_hand_side_description;
				proposals.add(createNoSideEffectProposal(context, (SimpleName) assignExpression, fieldBinding, label, 5));
			}
		}	
		
		if (binding == fieldBinding && ASTResolving.findParentBodyDeclaration(selectedNode) instanceof FunctionDeclaration) {
			SimpleName simpleName= (SimpleName) ((assignedNode instanceof SimpleName) ? assignedNode : assignExpression);
			String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createparameter_description, simpleName.getIdentifier());
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
			proposals.add(new NewVariableCompletionProposal(label, context.getCompilationUnit(), NewVariableCompletionProposal.PARAM, simpleName, null, 5, image));
		}
		
	
	}

	private static ASTRewriteCorrectionProposal createNoSideEffectProposal(IInvocationContext context, SimpleName nodeToQualify, IVariableBinding fieldBinding, String label, int relevance) {
		AST ast= nodeToQualify.getAST();
		
		Expression qualifier;
		if (Modifier.isStatic(fieldBinding.getModifiers())) {
			ITypeBinding declaringClass= fieldBinding.getDeclaringClass();
			qualifier= ast.newSimpleName(declaringClass.getTypeDeclaration().getName());
		} else {
			qualifier= ast.newThisExpression();
		}

		ASTRewrite rewrite= ASTRewrite.create(ast);
		FieldAccess access= ast.newFieldAccess();
		access.setName((SimpleName) rewrite.createCopyTarget(nodeToQualify));
		access.setExpression(qualifier);
		rewrite.replace(nodeToQualify, access, null);
		

		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		return new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, relevance, image);
	}

//	public static void addValueForAnnotationProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
//		IJavaScriptUnit cu= context.getCompilationUnit();
//		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
//		if (selectedNode instanceof Annotation) {
//			Annotation annotation= (Annotation) selectedNode;
//			if (annotation.resolveTypeBinding() == null) {
//				return;
//			}
//			MissingAnnotationAttributesProposal proposal= new MissingAnnotationAttributesProposal(cu, annotation, 10);
//			proposals.add(proposal);		
//		}
//	}
	
	public static void addFallThroughProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode instanceof SwitchCase && selectedNode.getParent() instanceof SwitchStatement) {
			AST ast= selectedNode.getAST();
			ASTRewrite rewrite= ASTRewrite.create(ast);
			ListRewrite listRewrite= rewrite.getListRewrite(selectedNode.getParent(), SwitchStatement.STATEMENTS_PROPERTY);
			listRewrite.insertBefore(ast.newBreakStatement(), selectedNode, null);
			
			String label= CorrectionMessages.LocalCorrectionsSubProcessor_insert_break_statement;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image);
			proposals.add(proposal);
		}
	}
	
	public static void addDeprecatedFieldsToMethodsProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode instanceof Name) {
			IBinding binding= ((Name) selectedNode).resolveBinding();
			if (binding instanceof IVariableBinding) {
				IVariableBinding variableBinding= (IVariableBinding) binding;
				if (variableBinding.isField()) {
					String qualifiedName= variableBinding.getDeclaringClass().getTypeDeclaration().getQualifiedName();
					String fieldName= variableBinding.getName();
					String[] methodName= getMethod(JavaModelUtil.concatenateName(qualifiedName, fieldName));
					if (methodName != null) {
						AST ast= selectedNode.getAST();
						ASTRewrite astRewrite= ASTRewrite.create(ast);
						ImportRewrite importRewrite= StubUtility.createImportRewrite(context.getASTRoot(), true);

						FunctionInvocation method= ast.newFunctionInvocation();
						String qfn= importRewrite.addImport(methodName[0]);
						method.setExpression(ast.newName(qfn));
						method.setName(ast.newSimpleName(methodName[1]));
						astRewrite.replace(selectedNode, method, null);

						String label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_replacefieldaccesswithmethod_description, method);
						Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
						ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), astRewrite, 10, image);
						proposal.setImportRewrite(importRewrite);
						proposals.add(proposal);
					}
				}
			}
		}
	}
	
	private static Map/*<String,String[]>*/ resolveMap; 
	private static String[] getMethod(String fieldName) {
		if (resolveMap==null){
			resolveMap=new HashMap();
			resolveMap.put("java.util.Collections.EMPTY_MAP", new String[]{"java.util.Collections","emptyMap"});   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			resolveMap.put("java.util.Collections.EMPTY_SET", new String[]{"java.util.Collections","emptySet"});  //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			resolveMap.put("java.util.Collections.EMPTY_LIST", new String[]{"java.util.Collections","emptyList"});//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		}
		return (String[]) resolveMap.get(fieldName);
	}
}
