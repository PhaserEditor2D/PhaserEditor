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

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ArrayCreation;
import org.eclipse.wst.jsdt.core.dom.ArrayInitializer;
import org.eclipse.wst.jsdt.core.dom.ArrayType;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.CatchClause;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.internal.corext.SourceRange;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.TempDeclarationFinder;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.TempOccurrenceAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.BindingLabelProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

public class InlineTempRefactoring extends ScriptableRefactoring {

	private int fSelectionStart;
	private int fSelectionLength;
	private IJavaScriptUnit fCu;
	
	//the following fields are set after the construction
	private VariableDeclaration fVariableDeclaration;
	private SimpleName[] fReferences;
	private JavaScriptUnit fASTRoot;

	/**
	 * Creates a new inline constant refactoring.
	 * @param unit the compilation unit, or <code>null</code> if invoked by scripting
	 * @param node compilation unit node, or <code>null</code>
	 * @param selectionStart
	 * @param selectionLength
	 */
	public InlineTempRefactoring(IJavaScriptUnit unit, JavaScriptUnit node, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= unit;
		
		fASTRoot= node;
		fVariableDeclaration= null;
	}
	
	/**
	 * Creates a new inline constant refactoring.
	 * @param unit the compilation unit, or <code>null</code> if invoked by scripting
	 * @param selectionStart
	 * @param selectionLength
	 */
	public InlineTempRefactoring(IJavaScriptUnit unit, int selectionStart, int selectionLength) {
		this(unit, null, selectionStart, selectionLength);
	}
	
	public InlineTempRefactoring(VariableDeclaration decl) {
		fVariableDeclaration= decl;
		ASTNode astRoot= decl.getRoot();
		Assert.isTrue(astRoot instanceof JavaScriptUnit);
		fASTRoot= (JavaScriptUnit) astRoot;
		Assert.isTrue(fASTRoot.getJavaElement() instanceof IJavaScriptUnit);
		
		fSelectionStart= decl.getStartPosition();
		fSelectionLength= decl.getLength();
		fCu= (IJavaScriptUnit) fASTRoot.getJavaElement();
	}
	
	public RefactoringStatus checkIfTempSelected() {
		VariableDeclaration decl= getVariableDeclaration();
		if (decl == null) {
			return CodeRefactoringUtil.checkMethodSyntaxErrors(fSelectionStart, fSelectionLength, getASTRoot(), RefactoringCoreMessages.InlineTempRefactoring_select_temp);
		}
		if (decl.getParent() instanceof FieldDeclaration) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTemRefactoring_error_message_fieldsCannotBeInlined); 
		}
		return new RefactoringStatus();
	}	
	
	private JavaScriptUnit getASTRoot() {
		if (fASTRoot == null) {
			fASTRoot= RefactoringASTParser.parseWithASTProvider(fCu, true, null);
		}
		return fASTRoot;
	}
	
	public VariableDeclaration getVariableDeclaration() {
		if (fVariableDeclaration == null) {
			fVariableDeclaration= TempDeclarationFinder.findTempDeclaration(getASTRoot(), fSelectionStart, fSelectionLength);
		}
		return fVariableDeclaration;
	}
	
	/*
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.InlineTempRefactoring_name; 
	}
	
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 1); //$NON-NLS-1$
			
			RefactoringStatus result= Checks.validateModifiesFiles(ResourceUtil.getFiles(new IJavaScriptUnit[]{fCu}), getValidationContext());
			if (result.hasFatalError())
				return result;
					
			VariableDeclaration declaration= getVariableDeclaration();
			
			result.merge(checkSelection(declaration));
			if (result.hasFatalError())
				return result;
			
			result.merge(checkInitializer(declaration));	
			return result;
		} finally {
			pm.done();
		}	
	}

    private RefactoringStatus checkInitializer(VariableDeclaration decl) {
		if (decl.getInitializer().getNodeType() == ASTNode.NULL_LITERAL)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTemRefactoring_error_message_nulLiteralsCannotBeInlined);
		return null;
	}

	private RefactoringStatus checkSelection(VariableDeclaration decl) {
		ASTNode parent= decl.getParent();
		if (parent instanceof FunctionDeclaration) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTempRefactoring_method_parameter); 
		}
		
		if (parent instanceof CatchClause) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTempRefactoring_exceptions_declared); 
		}
		
		if (parent instanceof VariableDeclarationExpression && parent.getLocationInParent() == ForStatement.INITIALIZERS_PROPERTY) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTempRefactoring_for_initializers); 
		}
		
		if (decl.getInitializer() == null) {
			String message= Messages.format(RefactoringCoreMessages.InlineTempRefactoring_not_initialized, decl.getName().getIdentifier());
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
				
		return checkAssignments(decl);
	}
	
	private RefactoringStatus checkAssignments(VariableDeclaration decl) {
		TempAssignmentFinder assignmentFinder= new TempAssignmentFinder(decl);
		getASTRoot().accept(assignmentFinder);
		if (!assignmentFinder.hasAssignments())
			return new RefactoringStatus();
		ASTNode firstAssignment= assignmentFinder.getFirstAssignment();
		int start= firstAssignment.getStartPosition();
		int length= firstAssignment.getLength();
		ISourceRange range= new SourceRange(start, length);
		RefactoringStatusContext context= JavaStatusContext.create(fCu, range);	
		String message= Messages.format(RefactoringCoreMessages.InlineTempRefactoring_assigned_more_once, decl.getName().getIdentifier());
		return RefactoringStatus.createFatalErrorStatus(message, context);
	}
	
	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 1); //$NON-NLS-1$
			return new RefactoringStatus();
		} finally {
			pm.done();
		}	
	}
	
	//----- changes

	public Change createChange(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.InlineTempRefactoring_preview, 2);
			final Map arguments= new HashMap();
			String project= null;
			IJavaScriptProject javaProject= fCu.getJavaScriptProject();
			if (javaProject != null)
				project= javaProject.getElementName();
			
			final IVariableBinding binding= getVariableDeclaration().resolveBinding();
			String text= null;
			final IFunctionBinding method= binding.getDeclaringMethod();
			if (method != null)
				text= BindingLabelProvider.getBindingLabel(method, JavaScriptElementLabels.ALL_FULLY_QUALIFIED);
			else
				text= '{' + JavaScriptElementLabels.ELLIPSIS_STRING + '}';
			final String description= Messages.format(RefactoringCoreMessages.InlineTempRefactoring_descriptor_description_short, binding.getName());
			final String header= Messages.format(RefactoringCoreMessages.InlineTempRefactoring_descriptor_description, new String[] { BindingLabelProvider.getBindingLabel(binding, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), text});
			final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
			comment.addSetting(Messages.format(RefactoringCoreMessages.InlineTempRefactoring_original_pattern, BindingLabelProvider.getBindingLabel(binding, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)));
			final JDTRefactoringDescriptor descriptor= new JDTRefactoringDescriptor(IJavaScriptRefactorings.INLINE_LOCAL_VARIABLE, project, description, comment.asString(), arguments, RefactoringDescriptor.NONE);
			arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fCu));
			arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_SELECTION, String.valueOf(fSelectionStart) + ' ' + String.valueOf(fSelectionLength));
			
			CompilationUnitRewrite cuRewrite= new CompilationUnitRewrite(fCu, fASTRoot);
			
			inlineTemp(cuRewrite);
			removeTemp(cuRewrite);
			
			final CompilationUnitChange result= cuRewrite.createChange(RefactoringCoreMessages.InlineTempRefactoring_inline, false, new SubProgressMonitor(pm, 1));
			result.setDescriptor(new RefactoringChangeDescriptor(descriptor));
			return result;
		} finally {
			pm.done();
		}
	}

	private void inlineTemp(CompilationUnitRewrite cuRewrite) throws JavaScriptModelException {
		SimpleName[] references= getReferences();

		TextEditGroup groupDesc= cuRewrite.createGroupDescription(RefactoringCoreMessages.InlineTempRefactoring_inline_edit_name);
		ASTRewrite rewrite= cuRewrite.getASTRewrite();

		for (int i= 0; i < references.length; i++){
			SimpleName curr= references[i];
			ASTNode initializerCopy= getInitializerSource(cuRewrite, curr);
			rewrite.replace(curr, initializerCopy, groupDesc);
		}
	}
	
    private boolean needsBrackets(SimpleName name, VariableDeclaration variableDeclaration) {
		Expression initializer= variableDeclaration.getInitializer();
		if (initializer instanceof Assignment) //for esthetic reasons
			return true;
    		
    	return ASTNodes.substituteMustBeParenthesized(initializer, name);
    }
    

	private void removeTemp(CompilationUnitRewrite cuRewrite) throws JavaScriptModelException {
		VariableDeclaration variableDeclaration= getVariableDeclaration();
		TextEditGroup groupDesc= cuRewrite.createGroupDescription(RefactoringCoreMessages.InlineTempRefactoring_remove_edit_name);
		ASTNode parent= variableDeclaration.getParent();
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		if (parent instanceof VariableDeclarationStatement && ((VariableDeclarationStatement) parent).fragments().size() == 1) {
			rewrite.remove(parent, groupDesc);
		} else {
			rewrite.remove(variableDeclaration, groupDesc);
		}
	}
	
	private Expression getInitializerSource(CompilationUnitRewrite rewrite, SimpleName reference) throws JavaScriptModelException {
		Expression copy= getModifiedInitializerSource(rewrite, reference);
		boolean brackets= needsBrackets(reference, getVariableDeclaration());
		if (brackets) {
			ParenthesizedExpression parentExpr= rewrite.getAST().newParenthesizedExpression();
			parentExpr.setExpression(copy);
			return parentExpr;
		}
		return copy;
	}
	
	private Expression getModifiedInitializerSource(CompilationUnitRewrite rewrite, SimpleName reference) throws JavaScriptModelException {
		VariableDeclaration varDecl= getVariableDeclaration();
		Expression initializer= varDecl.getInitializer();
		
		ASTNode referenceContext= reference.getParent();
		if (isInvocation(initializer)) {
			if (Invocations.isResolvedTypeInferredFromExpectedType(initializer)) {
				if (! (referenceContext instanceof VariableDeclarationFragment
						|| referenceContext instanceof SingleVariableDeclaration
						|| referenceContext instanceof Assignment)) {
					Invocations.resolveBinding(initializer);
					String newSource= createParameterizedInvocation(initializer, new Type[0]);
					return (Expression) rewrite.getASTRewrite().createStringPlaceholder(newSource, initializer.getNodeType());
				}
			}
		}
		
		Expression copy= (Expression) rewrite.getASTRewrite().createCopyTarget(initializer);
		if (initializer instanceof ArrayInitializer && ASTNodes.getDimensions(varDecl) > 0) {
			ArrayType newType= (ArrayType) ASTNodeFactory.newType(rewrite.getAST(), varDecl);
			
			ArrayCreation newArrayCreation= rewrite.getAST().newArrayCreation();
			newArrayCreation.setType(newType);
			newArrayCreation.setInitializer((ArrayInitializer) copy);
			return newArrayCreation;
		}
		return copy;
	}

	private String createParameterizedInvocation(Expression invocation, Type[] typeArgumentNodes) throws JavaScriptModelException {
		ASTRewrite rewrite= ASTRewrite.create(invocation.getAST());
		ListRewrite typeArgsRewrite= rewrite.getListRewrite(invocation, Invocations.getTypeArgumentsProperty(invocation));
		for (int i= 0; i < typeArgumentNodes.length; i++) {
			typeArgsRewrite.insertLast(typeArgumentNodes[i], null);
		}
		
		IDocument document= new Document(fCu.getBuffer().getContents());
		final RangeMarker marker= new RangeMarker(invocation.getStartPosition(), invocation.getLength());
		IJavaScriptProject project= fCu.getJavaScriptProject();
		TextEdit[] rewriteEdits= rewrite.rewriteAST(document, project.getOptions(true)).removeChildren();
		marker.addChildren(rewriteEdits);
		try {
			marker.apply(document, TextEdit.UPDATE_REGIONS);
			String rewrittenInitializer= document.get(marker.getOffset(), marker.getLength());
			IRegion region= document.getLineInformation(document.getLineOfOffset(marker.getOffset()));
			int oldIndent= Strings.computeIndentUnits(document.get(region.getOffset(), region.getLength()), project);
			return Strings.changeIndent(rewrittenInitializer, oldIndent, project, "", TextUtilities.getDefaultLineDelimiter(document)); //$NON-NLS-1$
		} catch (MalformedTreeException e) {
			JavaScriptPlugin.log(e);
		} catch (BadLocationException e) {
			JavaScriptPlugin.log(e);
		}
		//fallback:
		return fCu.getBuffer().getText(invocation.getStartPosition(), invocation.getLength());
	}
	
	private static boolean isInvocation(Expression node) {
		return node instanceof FunctionInvocation || node instanceof SuperMethodInvocation;
	}

	public SimpleName[] getReferences() {
		if (fReferences != null)
			return fReferences;
		TempOccurrenceAnalyzer analyzer= new TempOccurrenceAnalyzer(getVariableDeclaration(), false);
		analyzer.perform();
		fReferences= analyzer.getReferenceNodes();
		return fReferences;
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
					return createInputFatalStatus(element, IJavaScriptRefactorings.INLINE_LOCAL_VARIABLE);
				else {
					fCu= (IJavaScriptUnit) element;
		        	if (checkIfTempSelected().hasFatalError())
						return createInputFatalStatus(element, IJavaScriptRefactorings.INLINE_LOCAL_VARIABLE);
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}
}
