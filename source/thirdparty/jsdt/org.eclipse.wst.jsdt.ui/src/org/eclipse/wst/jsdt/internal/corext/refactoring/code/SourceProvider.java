/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fix for
 *       o bug "inline method - doesn't handle implicit cast" (see
 *         https://bugs.eclipse.org/bugs/show_bug.cgi?id=24941).
 *       o inline call that is used in a field initializer 
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38137)
 *       o inline call a field initializer: could detect self reference 
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=44417)
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditProcessor;
import org.eclipse.text.edits.UndoEdit;
import org.eclipse.wst.jsdt.core.ITypeRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ConditionalExpression;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.ForInStatement;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.ThisExpression;
import org.eclipse.wst.jsdt.core.dom.WhileStatement;
import org.eclipse.wst.jsdt.core.dom.WithStatement;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.CodeScopeBuilder;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.SourceAnalyzer.NameData;
import org.eclipse.wst.jsdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

/**
 * A SourceProvider encapsulates a piece of code (source) and the logic
 * to inline it into given CallContexts.
 */
public class SourceProvider {

	private ITypeRoot fTypeRoot;
	private IDocument fDocument;
	private FunctionDeclaration fDeclaration;
	private SourceAnalyzer fAnalyzer;
	private boolean fMustEvalReturnedExpression;
	private boolean fReturnValueNeedsLocalVariable;
	private List fReturnExpressions;
	private IDocument fSource;
	
	private static final int EXPRESSION_MODE= 1;
	private static final int STATEMENT_MODE= 2;
	private static final int RETURN_STATEMENT_MODE= 3;
	private int fMarkerMode;
	
	
	private class ReturnAnalyzer extends ASTVisitor {
		public boolean visit(ReturnStatement node) {
			Expression expression= node.getExpression();
			if (!(ASTNodes.isLiteral(expression) || expression instanceof Name)) {
				fMustEvalReturnedExpression= true;
			}
			if (Invocations.isInvocation(expression) || expression instanceof ClassInstanceCreation) {
				fReturnValueNeedsLocalVariable= false;
			}
			fReturnExpressions.add(expression);
			return false;
		}
	}

	public SourceProvider(ITypeRoot typeRoot, FunctionDeclaration declaration) {
		super();
		fTypeRoot= typeRoot;
		fDeclaration= declaration;
		List parameters= fDeclaration.parameters();
		for (Iterator iter= parameters.iterator(); iter.hasNext();) {
			SingleVariableDeclaration element= (SingleVariableDeclaration)iter.next();
			ParameterData data= new ParameterData(element);
			element.setProperty(ParameterData.PROPERTY, data);
		}
		fAnalyzer= new SourceAnalyzer(fTypeRoot, fDeclaration);
		fReturnValueNeedsLocalVariable= true;
		fReturnExpressions= new ArrayList();
	}
	
	/**
	 * TODO: unit's source does not match contents of source document and declaration node.
	 */
	public SourceProvider(ITypeRoot typeRoot, IDocument source, FunctionDeclaration declaration) {
		this(typeRoot, declaration);
		fSource= source;
	}

	public RefactoringStatus checkActivation() throws JavaScriptModelException {
		return fAnalyzer.checkActivation();
	}
	
	public void initialize() throws JavaScriptModelException {
		fDocument= fSource == null ? new Document(fTypeRoot.getBuffer().getContents()) : fSource;
		fAnalyzer.initialize();
		if (hasReturnValue()) {
			ASTNode last= getLastStatement();
			if (last != null) {
				ReturnAnalyzer analyzer= new ReturnAnalyzer();
				last.accept(analyzer);
			}
		}
	}

	public boolean isExecutionFlowInterrupted() {
		return fAnalyzer.isExecutionFlowInterrupted();
	}

	static class VariableReferenceFinder extends ASTVisitor {
		private boolean fResult;
		private IVariableBinding fBinding;
		public VariableReferenceFinder(IVariableBinding binding) {
			fBinding= binding;
		}
		public boolean getResult() {
			return fResult;
		}
		public boolean visit(SimpleName node) {
			if(!fResult) {
				fResult= Bindings.equals(fBinding, node.resolveBinding()); 
			}
			return false;
		}
	}

	/**
	 * Checks whether variable is referenced by the method declaration or not.
	 * 
	 * @param binding binding of variable to check.
	 * @return <code>true</code> if variable is referenced by the method, otherwise <code>false</code>
	 */
	public boolean isVariableReferenced(IVariableBinding binding) {
		VariableReferenceFinder finder= new VariableReferenceFinder(binding);
		fDeclaration.accept(finder);
		return finder.getResult();
	}

	public boolean hasReturnValue() {
		IFunctionBinding binding= fDeclaration.resolveBinding();
		return binding.getReturnType() != fDeclaration.getAST().resolveWellKnownType("void"); //$NON-NLS-1$
	}
	
	// returns true if the declaration has a vararg and
	// the body contains an array access to the vararg
	public boolean hasArrayAccess() {
		return fAnalyzer.hasArrayAccess();
	}
	
	public boolean hasSuperMethodInvocation() {
		return fAnalyzer.hasSuperMethodInvocation();
	}
	
	public boolean mustEvaluateReturnedExpression() {
		return fMustEvalReturnedExpression;
	}
	
	public boolean returnValueNeedsLocalVariable() {
		return fReturnValueNeedsLocalVariable;
	}
	
	public int getNumberOfStatements() {
		return fDeclaration.getBody().statements().size();
	}
	
	public boolean isSimpleFunction() {
		List statements= fDeclaration.getBody().statements();
		if (statements.size() != 1)
			return false;
		return statements.get(0) instanceof ReturnStatement;
	}
	
	public boolean isLastStatementReturn() {
		List statements= fDeclaration.getBody().statements();
		if (statements.size() == 0)
			return false;
		return statements.get(statements.size() - 1) instanceof ReturnStatement;
	}
	
	public boolean isDangligIf() {
		List statements= fDeclaration.getBody().statements();
		if (statements.size() != 1)
			return false;
		
		Object statement= statements.get(0);
		if (! (statement instanceof IfStatement))
			return false;
		
		return ((IfStatement) statement).getElseStatement() == null;
	}
	
	public FunctionDeclaration getDeclaration() {
		return fDeclaration;
	}
	
	public String getMethodName() {
		return fDeclaration.getName().getIdentifier();
	}
	
	public ITypeBinding getReturnType() {
		return fDeclaration.resolveBinding().getReturnType();
	}
	
	public List getReturnExpressions() {
		return fReturnExpressions;
	}
	
	public boolean returnTypeMatchesReturnExpressions() {
		ITypeBinding returnType= getReturnType();
		for (Iterator iter= fReturnExpressions.iterator(); iter.hasNext();) {
			Expression expression= (Expression)iter.next();
			if (!Bindings.equals(returnType, expression.resolveTypeBinding()))
				return false;
		}
		return true;
	}
	
	public ParameterData getParameterData(int index) {
		SingleVariableDeclaration decl= (SingleVariableDeclaration)fDeclaration.parameters().get(index);
		return (ParameterData)decl.getProperty(ParameterData.PROPERTY);
	}
	
	public ITypeRoot getTypeRoot() {
		return fTypeRoot;
	}
	
	public boolean needsReturnedExpressionParenthesis() {
		ASTNode last= getLastStatement();
		if (last instanceof ReturnStatement) {
			return ASTNodes.needsParentheses(((ReturnStatement)last).getExpression());
		}
		return false;
	}
	
	public boolean returnsConditionalExpression() {
		ASTNode last= getLastStatement();
		if (last instanceof ReturnStatement) {
			return ((ReturnStatement)last).getExpression() instanceof ConditionalExpression;
		}
		return false;
	}
	
	public int getReceiversToBeUpdated() {
		return fAnalyzer.getImplicitReceivers().size();
	}
	
	public boolean isVarargs() {
		return fDeclaration.isVarargs();
	}
	
	public int getVarargIndex() {
		return fDeclaration.parameters().size() - 1;
	}
	
	public TextEdit getDeleteEdit() {
		final ASTRewrite rewriter= ASTRewrite.create(fDeclaration.getAST());
		rewriter.remove(fDeclaration, null);
		return rewriter.rewriteAST(fDocument, fTypeRoot.getJavaScriptProject().getOptions(true));
	}
	
	public String[] getCodeBlocks(CallContext context) throws CoreException {
		final ASTRewrite rewriter= ASTRewrite.create(fDeclaration.getAST());
		replaceParameterWithExpression(rewriter, context.arguments);
		updateImplicitReceivers(rewriter, context);
		makeNamesUnique(rewriter, context.scope);
		updateTypeReferences(rewriter, context);
		updateStaticReferences(rewriter, context);
		updateMethodTypeVariable(rewriter, context);
		
		List ranges= null;
		if (hasReturnValue()) {
			if (context.callMode == ASTNode.RETURN_STATEMENT) {
				ranges= getStatementRanges();
			} else {
				ranges= getExpressionRanges();
			}
		} else {
			ASTNode last= getLastStatement();
			if (last != null && last.getNodeType() == ASTNode.RETURN_STATEMENT) {
				ranges= getReturnStatementRanges();
			} else {
				ranges= getStatementRanges();
			}
		}

		final TextEdit dummy= rewriter.rewriteAST(fDocument, fTypeRoot.getJavaScriptProject().getOptions(true));
		int size= ranges.size();
		RangeMarker[] markers= new RangeMarker[size];
		for (int i= 0; i < markers.length; i++) {
			IRegion range= (IRegion)ranges.get(i);
			markers[i]= new RangeMarker(range.getOffset(), range.getLength());
		}
		int split;
		if (size <= 1) {
			split= Integer.MAX_VALUE;
		} else {
			IRegion region= (IRegion)ranges.get(0);
			split= region.getOffset() + region.getLength();
		}
		TextEdit[] edits= dummy.removeChildren();
		for (int i= 0; i < edits.length; i++) {
			TextEdit edit= edits[i];
			int pos= edit.getOffset() >= split ? 1 : 0;
			markers[pos].addChild(edit);
		}
		MultiTextEdit root= new MultiTextEdit(0, fDocument.getLength());
		root.addChildren(markers);
		
		try {
			TextEditProcessor processor= new TextEditProcessor(fDocument, root, TextEdit.CREATE_UNDO | TextEdit.UPDATE_REGIONS);
			UndoEdit undo= processor.performEdits();
			String[] result= getBlocks(markers);
			// It is faster to undo the changes than coping the buffer over and over again.
			processor= new TextEditProcessor(fDocument, undo, TextEdit.UPDATE_REGIONS);
			processor.performEdits();
			return result;
		} catch (MalformedTreeException exception) {
			JavaScriptPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaScriptPlugin.log(exception);
		}
		return new String[] {};
	}

	private void replaceParameterWithExpression(ASTRewrite rewriter, String[] expressions) {
		for (int i= 0; i < expressions.length; i++) {
			String expression= expressions[i];
			ParameterData parameter= getParameterData(i);
			List references= parameter.references();
			for (Iterator iter= references.iterator(); iter.hasNext();) {
				ASTNode element= (ASTNode) iter.next();
				ASTNode newNode= rewriter.createStringPlaceholder(expression, element.getNodeType());
				rewriter.replace(element, newNode, null);
			}
		}
	}

	private void makeNamesUnique(ASTRewrite rewriter, CodeScopeBuilder.Scope scope) {
		Collection usedCalleeNames= fAnalyzer.getUsedNames();
		for (Iterator iter= usedCalleeNames.iterator(); iter.hasNext();) {
			SourceAnalyzer.NameData nd= (SourceAnalyzer.NameData) iter.next();
			if (scope.isInUse(nd.getName())) {
				String newName= scope.createName(nd.getName(), true);
				List references= nd.references();
				for (Iterator refs= references.iterator(); refs.hasNext();) {
					SimpleName element= (SimpleName) refs.next();
					ASTNode newNode= rewriter.createStringPlaceholder(newName, ASTNode.FUNCTION_INVOCATION);
					rewriter.replace(element, newNode, null);
				}
			}
		}
	}
	
	private void updateImplicitReceivers(ASTRewrite rewriter, CallContext context) {
		if (context.receiver == null)
			return;
		List implicitReceivers= fAnalyzer.getImplicitReceivers();
		for (Iterator iter= implicitReceivers.iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode)iter.next();
			if (node instanceof FunctionInvocation) {
				final FunctionInvocation inv= (FunctionInvocation)node;
				rewriter.set(inv, FunctionInvocation.EXPRESSION_PROPERTY, createReceiver(rewriter, context, (IFunctionBinding)inv.getName().resolveBinding()), null);
			} else if (node instanceof ClassInstanceCreation) {
				final ClassInstanceCreation inst= (ClassInstanceCreation)node;
				rewriter.set(inst, ClassInstanceCreation.EXPRESSION_PROPERTY, createReceiver(rewriter, context, inst.resolveConstructorBinding()), null);
			} else if (node instanceof ThisExpression) {
				rewriter.replace(node, rewriter.createStringPlaceholder(context.receiver, ASTNode.FUNCTION_INVOCATION), null);
			} else if (node instanceof FieldAccess) { 
				final FieldAccess access= (FieldAccess)node;
				rewriter.set(access, FieldAccess.EXPRESSION_PROPERTY, createReceiver(rewriter, context, access.resolveFieldBinding()), null);
			} else if (node instanceof SimpleName && ((SimpleName)node).resolveBinding() instanceof IVariableBinding) {
				IVariableBinding vb= (IVariableBinding)((SimpleName)node).resolveBinding();
				if (vb.isField()) { 
					Expression receiver= createReceiver(rewriter, context, vb);
					if (receiver != null) {
						FieldAccess access= node.getAST().newFieldAccess();
						ASTNode target= rewriter.createMoveTarget(node);
						access.setName((SimpleName)target);
						access.setExpression(receiver);
						rewriter.replace(node, access, null);
					}
				}
			}
		}
	}
	
	private void updateTypeReferences(ASTRewrite rewriter, CallContext context) {
		ImportRewrite importer= context.importer;
		for (Iterator iter= fAnalyzer.getTypesToImport().iterator(); iter.hasNext();) {
			Name element= (Name)iter.next();
			ITypeBinding binding= ASTNodes.getTypeBinding(element);
			if (binding != null && !binding.isLocal()) {
				// We have collected names not types. So we have to import
				// the declaration type if we reference a parameterized type
				// since we have an entry for every name node (e.g. one for
				// Vector and one for Integer in Vector<Integer>.
				String s= importer.addImport(binding);
				if (!ASTNodes.asString(element).equals(s)) {
					rewriter.replace(element, rewriter.createStringPlaceholder(s, ASTNode.SIMPLE_NAME), null);
				}
			}
		}
	}
	
	private void updateStaticReferences(ASTRewrite rewriter, CallContext context) {
		ImportRewrite importer= context.importer;
		for (Iterator iter= fAnalyzer.getStaticsToImport().iterator(); iter.hasNext();) {
			Name element= (Name)iter.next();
			IBinding binding= element.resolveBinding();
			if (binding != null) {
				String s= importer.addStaticImport(binding);
				if (!ASTNodes.asString(element).equals(s)) {
					rewriter.replace(element, rewriter.createStringPlaceholder(s, ASTNode.SIMPLE_NAME), null);
				}
			}
		}
		
	}

	private Expression createReceiver(ASTRewrite rewriter, CallContext context, IFunctionBinding method) {
		String receiver= getReceiver(context, method.getModifiers());
		if (receiver == null)
			return null;
		return (Expression)rewriter.createStringPlaceholder(receiver, ASTNode.FUNCTION_INVOCATION);
	}
	
	private Expression createReceiver(ASTRewrite rewriter, CallContext context, IVariableBinding field) {
		String receiver= getReceiver(context, field.getModifiers());
		if (receiver == null)
			return null;
		return (Expression)rewriter.createStringPlaceholder(receiver, ASTNode.SIMPLE_NAME);
	}
	
	private String getReceiver(CallContext context, int modifiers) {
		String receiver= context.receiver;
		ITypeBinding invocationType= ASTNodes.getEnclosingType(context.invocation);
		ITypeBinding sourceType= fDeclaration.resolveBinding().getDeclaringClass();
		if (!context.receiverIsStatic && Modifier.isStatic(modifiers)) {
			if ("this".equals(receiver) && invocationType != null && Bindings.equals(invocationType, sourceType)) { //$NON-NLS-1$
				receiver= null;
			} else {
				receiver= context.importer.addImport(sourceType);
			}
		}
		return receiver;
	}
	
	private void updateMethodTypeVariable(ASTRewrite rewriter, CallContext context) {
		IFunctionBinding method= Invocations.resolveBinding(context.invocation);
		if (method == null)
			return;
		rewriteReferences(rewriter, null, fAnalyzer.getMethodTypeParameterReferences());
	}

	private void rewriteReferences(ASTRewrite rewriter, ITypeBinding[] typeArguments, List typeParameterReferences) {
		if (typeArguments.length == 0)
			return;
		Assert.isTrue(typeArguments.length == typeParameterReferences.size());
		for (int i= 0; i < typeArguments.length; i++) {
			SourceAnalyzer.NameData refData= (NameData)typeParameterReferences.get(i);
			List references= refData.references();
			String newName= typeArguments[i].getName();
			for (Iterator iter= references.iterator(); iter.hasNext();) {
				SimpleName name= (SimpleName)iter.next();
				rewriter.replace(name, rewriter.createStringPlaceholder(newName, ASTNode.SIMPLE_NAME), null);
			}
		}
	}
	
	private ASTNode getLastStatement() {
		List statements= fDeclaration.getBody().statements();
		if (statements.isEmpty())
			return null;
		return (ASTNode)statements.get(statements.size() - 1);
	}

	private List getReturnStatementRanges() {
		fMarkerMode= RETURN_STATEMENT_MODE;
		List result= new ArrayList(1);
		List statements= fDeclaration.getBody().statements();
		int size= statements.size();
		if (size <= 1)
			return result;
		result.add(createRange(statements, size - 2));
		return result;
	}

	private List getStatementRanges() {
		fMarkerMode= STATEMENT_MODE;
		List result= new ArrayList(1);
		List statements= fDeclaration.getBody().statements();
		int size= statements.size();
		if (size == 0)
			return result;
		result.add(createRange(statements, size - 1));
		return result;
	}

	private List getExpressionRanges() {
		fMarkerMode= EXPRESSION_MODE;
		List result= new ArrayList(2);
		List statements= fDeclaration.getBody().statements();
		ReturnStatement rs= null;
		int size= statements.size();
		ASTNode node;
		switch (size) {
			case 0:
				return result;
			case 1:
				node= (ASTNode)statements.get(0);
				if (node.getNodeType() == ASTNode.RETURN_STATEMENT) {
					rs= (ReturnStatement)node;
				} else {
					result.add(new Region(node.getStartPosition(), node.getLength()));
				}
				break;
			default: {
				node= (ASTNode)statements.get(size - 1);
				if (node.getNodeType() == ASTNode.RETURN_STATEMENT) {
					result.add(createRange(statements, size - 2));
					rs= (ReturnStatement)node;
				} else {
					result.add(createRange(statements, size - 1));
				}
				break;
			}
		}
		if (rs != null) {
			Expression exp= rs.getExpression();
			result.add(new Region(exp.getStartPosition(), exp.getLength()));
		}
		return result;
	}
	
	private IRegion createRange(List statements, int end) {
		ASTNode first= (ASTNode)statements.get(0);
		ASTNode root= first.getRoot();
		if (root instanceof JavaScriptUnit) {
			JavaScriptUnit unit= (JavaScriptUnit)root;
			int start= unit.getExtendedStartPosition(first);
			ASTNode last= (ASTNode)statements.get(end);
			int length = unit.getExtendedStartPosition(last) - start + unit.getExtendedLength(last);
			IRegion range= new Region(start, length);
			return range;
		} else {
			int start= first.getStartPosition();
			ASTNode last= (ASTNode)statements.get(end);
			int length = last.getStartPosition() - start + last.getLength();
			IRegion range= new Region(start, length);
			return range;
		}
	}
	
	private String[] getBlocks(RangeMarker[] markers) throws BadLocationException {
		String[] result= new String[markers.length];
		for (int i= 0; i < markers.length; i++) {
			RangeMarker marker= markers[i];
			String content= fDocument.get(marker.getOffset(), marker.getLength());
			String lines[]= Strings.convertIntoLines(content);
			Strings.trimIndentation(lines, fTypeRoot.getJavaScriptProject(), false);
			if (fMarkerMode == STATEMENT_MODE && lines.length == 2 && isSingleControlStatementWithoutBlock()) {
				lines[1]= CodeFormatterUtil.createIndentString(1, fTypeRoot.getJavaScriptProject()) + lines[1];
			}
			result[i]= Strings.concatenate(lines, TextUtilities.getDefaultLineDelimiter(fDocument));
		}
		return result;
	}

	private boolean isSingleControlStatementWithoutBlock() {
		List statements= fDeclaration.getBody().statements();
		int size= statements.size();
		if (size != 1)
			return false;
		Statement statement= (Statement) statements.get(size - 1);
		int nodeType= statement.getNodeType();
		if (nodeType == ASTNode.IF_STATEMENT) {
			IfStatement ifStatement= (IfStatement) statement;
			return !(ifStatement.getThenStatement() instanceof Block) 
				&& !(ifStatement.getElseStatement() instanceof Block);
		} else if (nodeType == ASTNode.FOR_STATEMENT) {
			return !(((ForStatement)statement).getBody() instanceof Block);
		} else if (nodeType == ASTNode.WHILE_STATEMENT) {
			return !(((WhileStatement)statement).getBody() instanceof Block);
		} else if (nodeType == ASTNode.WITH_STATEMENT) {
			return !(((WithStatement)statement).getBody() instanceof Block);
		} else if (nodeType == ASTNode.FOR_IN_STATEMENT) {
			return !(((ForInStatement)statement).getBody() instanceof Block);
		}
		return false;
	}
}
