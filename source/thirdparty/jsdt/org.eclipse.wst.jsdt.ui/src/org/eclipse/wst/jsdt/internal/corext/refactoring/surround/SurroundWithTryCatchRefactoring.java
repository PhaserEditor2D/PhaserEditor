/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.surround;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.CatchClause;
import org.eclipse.wst.jsdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.IExtendedModifier;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.TryStatement;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.CodeScopeBuilder;
import org.eclipse.wst.jsdt.internal.corext.dom.Selection;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.SelectionAwareSourceRangeComputer;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

/**
 * Surround a set of statements with a try/catch block.
 * 
 * Special case:
 * 
 * URL url= file.toURL();
 * 
 * In this case the variable declaration statement gets convert into a
 * declaration without initializer. So the body of the try/catch block 
 * only consists of new assignments. In this case we can't move the 
 * selected nodes (e.g. the declaration) into the try block.
 */
public class SurroundWithTryCatchRefactoring extends Refactoring {

	private Selection fSelection;
	private ISurroundWithTryCatchQuery fQuery;
	private SurroundWithTryCatchAnalyzer fAnalyzer;
	private boolean fLeaveDirty;

	private IJavaScriptUnit fCUnit;
	private JavaScriptUnit fRootNode;
	private ASTRewrite fRewriter;
	private ImportRewrite fImportRewrite;
	private CodeScopeBuilder.Scope fScope;
	private ASTNode[] fSelectedNodes;

	private SurroundWithTryCatchRefactoring(IJavaScriptUnit cu, Selection selection, ISurroundWithTryCatchQuery query) {
		fCUnit= cu;
		fSelection= selection;
		fQuery= query;
		fLeaveDirty= false;
	}

	public static SurroundWithTryCatchRefactoring create(IJavaScriptUnit cu, ITextSelection selection, ISurroundWithTryCatchQuery query) {
		return new SurroundWithTryCatchRefactoring(cu, Selection.createFromStartLength(selection.getOffset(), selection.getLength()), query);
	}
		
	public static SurroundWithTryCatchRefactoring create(IJavaScriptUnit cu, int offset, int length, ISurroundWithTryCatchQuery query) {
		return new SurroundWithTryCatchRefactoring(cu, Selection.createFromStartLength(offset, length), query);
	}

	public void setLeaveDirty(boolean leaveDirty) {
		fLeaveDirty= leaveDirty;
	}
	
	public boolean stopExecution() {
		if (fAnalyzer == null)
			return true;
		ITypeBinding[] exceptions= fAnalyzer.getExceptions();
		return exceptions == null || exceptions.length == 0;
	}
	
	/* non Java-doc
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.SurroundWithTryCatchRefactoring_name; 
	}

	public RefactoringStatus checkActivationBasics(JavaScriptUnit rootNode) throws JavaScriptModelException {
		RefactoringStatus result= new RefactoringStatus();
		fRootNode= rootNode;
			
		fAnalyzer= new SurroundWithTryCatchAnalyzer(fCUnit, fSelection, fQuery);
		fRootNode.accept(fAnalyzer);
		result.merge(fAnalyzer.getStatus());
		return result;
	}


	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		JavaScriptUnit rootNode= new RefactoringASTParser(AST.JLS3).parse(fCUnit, true, pm);
		return checkActivationBasics(rootNode);
	}

	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		return Checks.validateModifiesFiles(
			ResourceUtil.getFiles(new IJavaScriptUnit[]{fCUnit}),
			getValidationContext());
	}

	/* non Java-doc
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		final String NN= ""; //$NON-NLS-1$
		if (pm == null) pm= new NullProgressMonitor();
		pm.beginTask(NN, 2);
		// This is cheap since the compilation unit is already open in a editor.
		IPath path= getFile().getFullPath();
		ITextFileBufferManager bufferManager= FileBuffers.getTextFileBufferManager();
		try {
			bufferManager.connect(path, LocationKind.IFILE, new SubProgressMonitor(pm, 1));
			IDocument document= bufferManager.getTextFileBuffer(path, LocationKind.IFILE).getDocument();
			final CompilationUnitChange result= new CompilationUnitChange(getName(), fCUnit);
			if (fLeaveDirty)
				result.setSaveMode(TextFileChange.LEAVE_DIRTY);
			MultiTextEdit root= new MultiTextEdit();
			result.setEdit(root);
			fRewriter= ASTRewrite.create(fAnalyzer.getEnclosingBodyDeclaration().getAST());
			fRewriter.setTargetSourceRangeComputer(new SelectionAwareSourceRangeComputer(
				fAnalyzer.getSelectedNodes(), document, fSelection.getOffset(), fSelection.getLength()));
			fImportRewrite= StubUtility.createImportRewrite(fRootNode, true);
			
			fScope= CodeScopeBuilder.perform(fAnalyzer.getEnclosingBodyDeclaration(), fSelection).
				findScope(fSelection.getOffset(), fSelection.getLength());
			fScope.setCursor(fSelection.getOffset());
			
			fSelectedNodes= fAnalyzer.getSelectedNodes();
			
			createTryCatchStatement(document);
			
			if (fImportRewrite.hasRecordedChanges()) {
				TextEdit edit= fImportRewrite.rewriteImports(null);
				root.addChild(edit);
				result.addTextEditGroup(new TextEditGroup(NN, new TextEdit[] {edit} ));
			}
			TextEdit change= fRewriter.rewriteAST(document, fCUnit.getJavaScriptProject().getOptions(true));
			root.addChild(change);
			result.addTextEditGroup(new TextEditGroup(NN, new TextEdit[] {change} ));
			return result;
		} catch (BadLocationException e) {
			throw new CoreException(new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), IStatus.ERROR,
				e.getMessage(), e));
		} finally {
			bufferManager.disconnect(path, LocationKind.IFILE, new SubProgressMonitor(pm, 1));
			pm.done();
		}
	}
	
	private AST getAST() {
		return fRootNode.getAST();
	}
	
	private void createTryCatchStatement(IDocument document) throws CoreException, BadLocationException {
		String lineDelimiter= document.getLineDelimiter(0);
		List result= new ArrayList(1);
		TryStatement tryStatement= getAST().newTryStatement();
		ITypeBinding[] exceptions= fAnalyzer.getExceptions();
		for (int i= 0; i < exceptions.length; i++) {
			ITypeBinding exception= exceptions[i];
			String type= fImportRewrite.addImport(exception);
			CatchClause catchClause= getAST().newCatchClause();
			tryStatement.catchClauses().add(catchClause);
			SingleVariableDeclaration decl= getAST().newSingleVariableDeclaration();
			String varName= StubUtility.getExceptionVariableName(fCUnit.getJavaScriptProject());
			
			String name= fScope.createName(varName, false);
			decl.setName(getAST().newSimpleName(name));
			decl.setType(ASTNodeFactory.newType(getAST(), type));
			catchClause.setException(decl);
			Statement st= getCatchBody(type, name, lineDelimiter);
			if (st != null) {
				catchClause.getBody().statements().add(st);
			}
		}
		List variableDeclarations= getSpecialVariableDeclarationStatements();
		ListRewrite statements= fRewriter.getListRewrite(tryStatement.getBody(), Block.STATEMENTS_PROPERTY);
		boolean selectedNodeRemoved= false;
		ASTNode expressionStatement= null;
		for (int i= 0; i < fSelectedNodes.length; i++) {
			ASTNode node= fSelectedNodes[i];
			if (node instanceof VariableDeclarationStatement && variableDeclarations.contains(node)) {
				AST ast= getAST();
				VariableDeclarationStatement statement= (VariableDeclarationStatement)node;
				// Create a copy and remove the initializer
				VariableDeclarationStatement copy= (VariableDeclarationStatement)ASTNode.copySubtree(ast, statement);
				List modifiers= copy.modifiers();
				for (Iterator iter= modifiers.iterator(); iter.hasNext();) {
					IExtendedModifier modifier= (IExtendedModifier) iter.next();
					if (modifier.isModifier() && Modifier.isFinal(((Modifier)modifier).getKeyword().toFlagValue())) {
						iter.remove();
					}
				}
				List fragments= copy.fragments();
				for (Iterator iter= fragments.iterator(), original= statement.fragments().iterator(); iter.hasNext();) {
					VariableDeclarationFragment fragment= (VariableDeclarationFragment)iter.next();
					IVariableBinding binding= ((VariableDeclarationFragment)original.next()).resolveBinding();
					// If we want to initialize the new local then we should do a flow analysis upfront
					// to decide if the first access is a read or write.
					if (true /* binding == null */) {
						fragment.setInitializer(null);
					} else {
						fragment.setInitializer(ASTNodeFactory.newDefaultExpression(ast, binding.getType()));
					}
				}
				JavaScriptUnit root= (JavaScriptUnit)statement.getRoot();
				int extendedStart= root.getExtendedStartPosition(statement);
				// we have a leading comment and the comment is covered by the selection
				if (extendedStart != statement.getStartPosition() && extendedStart >= fSelection.getOffset()) {
					String commentToken= document.get(extendedStart, statement.getStartPosition() - extendedStart);
					commentToken= Strings.trimTrailingTabsAndSpaces(commentToken);
					Type type= statement.getType();
					String typeName= document.get(type.getStartPosition(), type.getLength());
					copy.setType((Type)fRewriter.createStringPlaceholder(commentToken + typeName, type.getNodeType()));
				}
				result.add(copy);
				// convert the fragments into expression statements
				fragments= statement.fragments();
				if (!fragments.isEmpty()) {
					List newExpressionStatements= new ArrayList();
					for (Iterator iter= fragments.iterator(); iter.hasNext();) {
						VariableDeclarationFragment fragment= (VariableDeclarationFragment)iter.next();
						Expression initializer= fragment.getInitializer();
						if (initializer != null) {
							Assignment assignment= ast.newAssignment();
							assignment.setLeftHandSide((Expression)fRewriter.createCopyTarget(fragment.getName()));
							assignment.setRightHandSide((Expression)fRewriter.createCopyTarget(initializer));
							newExpressionStatements.add(ast.newExpressionStatement(assignment));
						}
					}
					if (!newExpressionStatements.isEmpty()) {
						if (fSelectedNodes.length == 1) {
							expressionStatement= fRewriter.createGroupNode((ASTNode[])newExpressionStatements.toArray(new ASTNode[newExpressionStatements.size()]));
						} else {
							fRewriter.replace(
								statement, 
								fRewriter.createGroupNode((ASTNode[])newExpressionStatements.toArray(new ASTNode[newExpressionStatements.size()])), 
								null);
						}
					} else {
						fRewriter.remove(statement, null);
						selectedNodeRemoved= true;
					}
				} else {
					fRewriter.remove(statement, null);
					selectedNodeRemoved= true;
				}
			}
		}
		result.add(tryStatement);
		ASTNode replacementNode;
		if (result.size() == 1) {
			replacementNode= (ASTNode)result.get(0);
		} else {
			replacementNode= fRewriter.createGroupNode((ASTNode[])result.toArray(new ASTNode[result.size()]));
		}
		if (fSelectedNodes.length == 1) {
			if (expressionStatement != null) {
				statements.insertLast(expressionStatement, null);
			} else {
				if (!selectedNodeRemoved)
					statements.insertLast(fRewriter.createMoveTarget(fSelectedNodes[0]), null);
			}
			fRewriter.replace(fSelectedNodes[0], replacementNode, null);
		} else {
			ListRewrite source= fRewriter.getListRewrite(
				fSelectedNodes[0].getParent(), 
				(ChildListPropertyDescriptor)fSelectedNodes[0].getLocationInParent());
			ASTNode toMove= source.createMoveTarget(
				fSelectedNodes[0], fSelectedNodes[fSelectedNodes.length - 1],
				replacementNode, null);
			statements.insertLast(toMove, null);
		}
	}
	
	private List getSpecialVariableDeclarationStatements() {
		List result= new ArrayList(3);
		VariableDeclaration[] locals= fAnalyzer.getAffectedLocals();
		for (int i= 0; i < locals.length; i++) {
			ASTNode parent= locals[i].getParent();
			if (parent instanceof VariableDeclarationStatement && !result.contains(parent))
				result.add(parent);
		}
		return result;
		
	}
	
	private Statement getCatchBody(String type, String name, String lineSeparator) throws CoreException {
		String s= StubUtility.getCatchBodyContent(fCUnit, type, name, fSelectedNodes[0], lineSeparator);
		if (s == null) {
			return null;
		} else {
			return (Statement)fRewriter.createStringPlaceholder(s, ASTNode.RETURN_STATEMENT);
		}
	}
	
	private IFile getFile() {
		return (IFile) fCUnit.getPrimary().getResource();
	}
}
