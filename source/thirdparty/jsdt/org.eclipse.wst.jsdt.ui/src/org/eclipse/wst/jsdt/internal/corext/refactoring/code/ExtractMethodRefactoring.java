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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.JavaScriptRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTFlattener;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.BodyDeclarationRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.wst.jsdt.internal.corext.dom.Selection;
import org.eclipse.wst.jsdt.internal.corext.dom.StatementRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.SelectionAwareSourceRangeComputer;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.BindingLabelProvider;
import org.eclipse.wst.jsdt.ui.CodeGeneration;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * Extracts a method in a compilation unit based on a text selection range.
 */
public class ExtractMethodRefactoring extends ScriptableRefactoring {

	private static final String ATTRIBUTE_VISIBILITY= "visibility"; //$NON-NLS-1$
	private static final String ATTRIBUTE_DESTINATION= "destination"; //$NON-NLS-1$
	private static final String ATTRIBUTE_COMMENTS= "comments"; //$NON-NLS-1$
	private static final String ATTRIBUTE_REPLACE= "replace"; //$NON-NLS-1$
	private static final String ATTRIBUTE_EXCEPTIONS= "exceptions"; //$NON-NLS-1$

	private IJavaScriptUnit fCUnit;
	private JavaScriptUnit fRoot;
	private ImportRewrite fImportRewriter;
	private int fSelectionStart;
	private int fSelectionLength;
	private AST fAST;
	private ASTRewrite fRewriter;
	private IDocument fDocument;
	private ExtractMethodAnalyzer fAnalyzer;
	private int fVisibility;
	private String fMethodName;
	private boolean fThrowRuntimeExceptions;
	private List fParameterInfos;
	private Set fUsedNames;
	private boolean fGenerateJavadoc;
	private boolean fReplaceDuplicates;
	private SnippetFinder.Match[] fDuplicates;
	private int fDestinationIndex= 0;
	// either of type TypeDeclaration or AnonymousClassDeclaration
	private ASTNode fDestination;
	// either of type TypeDeclaration or AnonymousClassDeclaration
	private ASTNode[] fDestinations;

	private static final String EMPTY= ""; //$NON-NLS-1$
	
	private static class UsedNamesCollector extends ASTVisitor {
		private Set result= new HashSet();
		private Set fIgnore= new HashSet();
		public static Set perform(ASTNode[] nodes) {
			UsedNamesCollector collector= new UsedNamesCollector();
			for (int i= 0; i < nodes.length; i++) {
				nodes[i].accept(collector);
			}
			return collector.result;
		}
		public boolean visit(FieldAccess node) {
			Expression exp= node.getExpression();
			if (exp != null)
				fIgnore.add(node.getName());
			return true;
		}
		public void endVisit(FieldAccess node) {
			fIgnore.remove(node.getName());
		}
		public boolean visit(FunctionInvocation node) {
			Expression exp= node.getExpression();
			if (exp != null) {
				SimpleName name = node.getName();
				if (name!=null)
					fIgnore.add(name);
			}
			return true;
		}
		public void endVisit(FunctionInvocation node) {
			SimpleName name = node.getName();
			if (name!=null)
				fIgnore.remove(name);
		}
		public boolean visit(QualifiedName node) {
			fIgnore.add(node.getName());
			return true;
		}
		public void endVisit(QualifiedName node) {
			fIgnore.remove(node.getName());
		}
		public boolean visit(SimpleName node) {
			if (!fIgnore.contains(node))
				result.add(node.getIdentifier());
			return true;
		}
		public boolean visit(TypeDeclaration node) {
			return visitType(node);
		}
		private boolean visitType(AbstractTypeDeclaration node) {
			result.add(node.getName().getIdentifier());
			// don't dive into type declaration since they open a new
			// context.
			return false;
		}
	}

	/**
	 * Creates a new extract method refactoring
	 * @param unit the compilation unit, or <code>null</code> if invoked by scripting
	 * @param selectionStart
	 * @param selectionLength
	 */
	public ExtractMethodRefactoring(IJavaScriptUnit unit, int selectionStart, int selectionLength) throws CoreException {
		fCUnit= unit;
		fMethodName= "extracted"; //$NON-NLS-1$
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fVisibility= -1;
		if (unit != null)
			initialize(unit);
	}

	private void initialize(IJavaScriptUnit cu) throws CoreException {
		fImportRewriter= StubUtility.createImportRewrite(cu, true);
	}

	 public String getName() {
	 	return RefactoringCoreMessages.ExtractMethodRefactoring_name; 
	 }

	/**
	 * Checks if the refactoring can be activated. Activation typically means, if a
	 * corresponding menu entry can be added to the UI.
	 *
	 * @param pm a progress monitor to report progress during activation checking.
	 * @return the refactoring status describing the result of the activation check.	 
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", 100); //$NON-NLS-1$
		
		if (fSelectionStart < 0 || fSelectionLength == 0)
			return mergeTextSelectionStatus(result);
		
		IFile[] changedFiles= ResourceUtil.getFiles(new IJavaScriptUnit[]{fCUnit});
		result.merge(Checks.validateModifiesFiles(changedFiles, getValidationContext()));
		if (result.hasFatalError())
			return result;
		result.merge(ResourceChangeChecker.checkFilesToBeChanged(changedFiles, new SubProgressMonitor(pm, 1)));
		
		fRoot= RefactoringASTParser.parseWithASTProvider(fCUnit, true, new SubProgressMonitor(pm, 99));
		fAST= fRoot.getAST();
		fRoot.accept(createVisitor());
		
		result.merge(fAnalyzer.checkInitialConditions(fImportRewriter));
		if (result.hasFatalError())
			return result;
		if (fVisibility == -1) {
//			setVisibility(Modifier.PRIVATE);
			fVisibility=0;
		}
		initializeParameterInfos();
		initializeUsedNames();
		initializeDuplicates();
		initializeDestinations();
		return result;
	}
	
	private ASTVisitor createVisitor() throws JavaScriptModelException {
		fAnalyzer= new ExtractMethodAnalyzer(fCUnit, Selection.createFromStartLength(fSelectionStart, fSelectionLength));
		return fAnalyzer;
	}
	
	/**
	 * Sets the method name to be used for the extracted method.
	 *
	 * @param name the new method name.
	 */	
	public void setMethodName(String name) {
		fMethodName= name;
	}
	
	/**
	 * Returns the method name to be used for the extracted method.
	 * @return the method name to be used for the extracted method.
	 */
	public String getMethodName() {
		return fMethodName;
	} 
	
	/**
	 * Sets the visibility of the new method.
	 * 
	 * @param visibility the visibility of the new method. Valid values are
	 *  "public", "protected", "", and "private"
	 */
	public void setVisibility(int visibility) {
		fVisibility= visibility;
	}
	
	/**
	 * Returns the visibility of the new method.
	 * 
	 * @return the visibility of the new method
	 */
	public int getVisibility() {
		return fVisibility;
	}
	
	/**
	 * Returns the parameter infos.
	 * @return a list of parameter infos.
	 */
	public List getParameterInfos() {
		return fParameterInfos;
	}
	
	/**
	 * Sets whether the new method signature throws runtime exceptions.
	 * 
	 * @param throwRuntimeExceptions flag indicating if the new method
	 * 	throws runtime exceptions
	 */
	public void setThrowRuntimeExceptions(boolean throwRuntimeExceptions) {
		fThrowRuntimeExceptions= throwRuntimeExceptions;
	}
	
	/**
	 * Checks if the new method name is a valid method name. This method doesn't
	 * check if a method with the same name already exists in the hierarchy. This
	 * check is done in <code>checkInput</code> since it is expensive.
	 */
	public RefactoringStatus checkMethodName() {
		return Checks.checkMethodName(fMethodName);
	}
	
	public ASTNode[] getDestinations() {
		return fDestinations;
	}
	
	public void setDestination(int index) {
		fDestination= fDestinations[index];
		fDestinationIndex= index;
	}
	
	/**
	 * Checks if the parameter names are valid.
	 */
	public RefactoringStatus checkParameterNames() {
		RefactoringStatus result= new RefactoringStatus();
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo parameter= (ParameterInfo)iter.next();
			result.merge(Checks.checkIdentifier(parameter.getNewName()));
			for (Iterator others= fParameterInfos.iterator(); others.hasNext();) {
				ParameterInfo other= (ParameterInfo) others.next();
				if (parameter != other && other.getNewName().equals(parameter.getNewName())) {
					result.addError(Messages.format(
						RefactoringCoreMessages.ExtractMethodRefactoring_error_sameParameter, 
						other.getNewName()));
					return result;
				}
			}
			if (parameter.isRenamed() && fUsedNames.contains(parameter.getNewName())) {
				result.addError(Messages.format(
					RefactoringCoreMessages.ExtractMethodRefactoring_error_nameInUse, 
					parameter.getNewName()));
				return result;
			}
		}
		return result;
	}
	
	/**
	 * Checks if varargs are ordered correctly.
	 */
	public RefactoringStatus checkVarargOrder() {
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo)iter.next();
			if (info.isOldVarargs() && iter.hasNext()) {
				return RefactoringStatus.createFatalErrorStatus(Messages.format(
					 RefactoringCoreMessages.ExtractMethodRefactoring_error_vararg_ordering,
					 info.getOldName()));
			}
		}
		return new RefactoringStatus();
	}
	
	/**
	 * Returns the names already in use in the selected statements/expressions.
	 * 
	 * @return names already in use.
	 */
	public Set getUsedNames() {
		return fUsedNames;
	}
	
	/* (non-Javadoc)
	 * Method declared in Refactoring
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		pm.beginTask(RefactoringCoreMessages.ExtractMethodRefactoring_checking_new_name, 2); 
		pm.subTask(EMPTY);
		
		RefactoringStatus result= checkMethodName();
		result.merge(checkParameterNames());
		result.merge(checkVarargOrder());
		pm.worked(1);
		if (pm.isCanceled())
			throw new OperationCanceledException();

		BodyDeclaration node= fAnalyzer.getEnclosingBodyDeclaration();
		if (node != null) {
			fAnalyzer.checkInput(result, fMethodName, fAST);
			pm.worked(1);
		}
		pm.done();
		return result;
	}
	
	/* (non-Javadoc)
	 * Method declared in IRefactoring
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		if (fMethodName == null)
			return null;
		pm.beginTask("", 2); //$NON-NLS-1$
		fAnalyzer.aboutToCreateChange();
		BodyDeclaration declaration= fAnalyzer.getEnclosingBodyDeclaration();
		fRewriter= ASTRewrite.create(declaration.getAST());
		final Map arguments= new HashMap();
		String project= null;
		IJavaScriptProject javaProject= fCUnit.getJavaScriptProject();
		if (javaProject != null)
			project= javaProject.getElementName();
		ITypeBinding type= null;
		if (fDestination instanceof AbstractTypeDeclaration) {
			final AbstractTypeDeclaration decl= (AbstractTypeDeclaration) fDestination;
			type= decl.resolveBinding();
		} else if (fDestination instanceof AnonymousClassDeclaration) {
			final AnonymousClassDeclaration decl= (AnonymousClassDeclaration) fDestination;
			type= decl.resolveBinding();
		}
		IFunctionBinding method= null;
		final BodyDeclaration enclosing= fAnalyzer.getEnclosingBodyDeclaration();
		if (enclosing instanceof FunctionDeclaration) {
			final FunctionDeclaration node= (FunctionDeclaration) enclosing;
			method= node.resolveBinding();
		}
		final int flags= RefactoringDescriptor.STRUCTURAL_CHANGE | JavaScriptRefactoringDescriptor.JAR_REFACTORING | JavaScriptRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
		final String description= Messages.format(RefactoringCoreMessages.ExtractMethodRefactoring_descriptor_description_short, fMethodName);
		final String label= method != null ? BindingLabelProvider.getBindingLabel(method, JavaScriptElementLabels.ALL_FULLY_QUALIFIED) : '{' + JavaScriptElementLabels.ELLIPSIS_STRING + '}';
		final String header= Messages.format(RefactoringCoreMessages.ExtractMethodRefactoring_descriptor_description, new String[] { getSignature(), label, BindingLabelProvider.getBindingLabel(type, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)});
		final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
		comment.addSetting(Messages.format(RefactoringCoreMessages.ExtractMethodRefactoring_name_pattern, fMethodName));
		comment.addSetting(Messages.format(RefactoringCoreMessages.ExtractMethodRefactoring_destination_pattern, BindingLabelProvider.getBindingLabel(type, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)));
//		String visibility= JdtFlags.getVisibilityString(fVisibility);
//		if ("".equals(visibility)) //$NON-NLS-1$
//			visibility= RefactoringCoreMessages.ExtractMethodRefactoring_default_visibility;
//		comment.addSetting(Messages.format(RefactoringCoreMessages.ExtractMethodRefactoring_visibility_pattern, visibility));
//		if (fThrowRuntimeExceptions)
//			comment.addSetting(RefactoringCoreMessages.ExtractMethodRefactoring_declare_thrown_exceptions);
		if (fReplaceDuplicates)
			comment.addSetting(RefactoringCoreMessages.ExtractMethodRefactoring_replace_occurrences);
		if (fGenerateJavadoc)
			comment.addSetting(RefactoringCoreMessages.ExtractMethodRefactoring_generate_comment);
		final JDTRefactoringDescriptor descriptor= new JDTRefactoringDescriptor(IJavaScriptRefactorings.EXTRACT_METHOD, project, description, comment.asString(), arguments, flags);
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fCUnit));
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_NAME, fMethodName);
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_SELECTION, Integer.valueOf(fSelectionStart).toString() + " " + Integer.valueOf(fSelectionLength).toString()); //$NON-NLS-1$
//		arguments.put(ATTRIBUTE_VISIBILITY, Integer.valueOf(fVisibility).toString());
		arguments.put(ATTRIBUTE_DESTINATION, Integer.valueOf(fDestinationIndex).toString());
//		arguments.put(ATTRIBUTE_EXCEPTIONS, Boolean.valueOf(fThrowRuntimeExceptions).toString());
		arguments.put(ATTRIBUTE_COMMENTS, Boolean.valueOf(fGenerateJavadoc).toString());
		arguments.put(ATTRIBUTE_REPLACE, Boolean.valueOf(fReplaceDuplicates).toString());
		final CompilationUnitChange result= new CompilationUnitChange(RefactoringCoreMessages.ExtractMethodRefactoring_change_name, fCUnit);
		result.setSaveMode(TextFileChange.KEEP_SAVE_STATE);
		result.setDescriptor(new RefactoringChangeDescriptor(descriptor));

		MultiTextEdit root= new MultiTextEdit();
		result.setEdit(root);
		// This is cheap since the compilation unit is already open in a editor.
		IPath path= ((IFile)fCUnit.getPrimary().getResource()).getFullPath();
		ITextFileBufferManager bufferManager= FileBuffers.getTextFileBufferManager();
		try {
			bufferManager.connect(path, LocationKind.IFILE, new SubProgressMonitor(pm, 1));
			fDocument= bufferManager.getTextFileBuffer(path, LocationKind.IFILE).getDocument();
			
			ASTNode[] selectedNodes= fAnalyzer.getSelectedNodes();
			fRewriter.setTargetSourceRangeComputer(new SelectionAwareSourceRangeComputer(selectedNodes,
				fDocument, fSelectionStart, fSelectionLength));
			
			TextEditGroup substituteDesc= new TextEditGroup(Messages.format(RefactoringCoreMessages.ExtractMethodRefactoring_substitute_with_call, fMethodName)); 
			result.addTextEditGroup(substituteDesc);
			
			FunctionDeclaration mm= createNewMethod(fMethodName, true, selectedNodes, fDocument.getLineDelimiter(0), substituteDesc);

			TextEditGroup insertDesc= new TextEditGroup(Messages.format(RefactoringCoreMessages.ExtractMethodRefactoring_add_method, fMethodName)); 
			result.addTextEditGroup(insertDesc);
			
			ASTNode afterDecl = declaration.getBodyChild();
			if (fDestination == fDestinations[0] || afterDecl==null) {
				ChildListPropertyDescriptor desc= (ChildListPropertyDescriptor)afterDecl.getLocationInParent();
				ListRewrite container= fRewriter.getListRewrite(afterDecl.getParent(), desc);
				container.insertAfter(mm, afterDecl, insertDesc);
			} else {
				BodyDeclarationRewrite container= BodyDeclarationRewrite.create(fRewriter, fDestination);
				container.insert(mm, insertDesc);
			}
			
			replaceDuplicates(result);
		
			if (fImportRewriter.hasRecordedChanges()) {
				TextEdit edit= fImportRewriter.rewriteImports(null);
				root.addChild(edit);
				result.addTextEditGroup(new TextEditGroup(
					RefactoringCoreMessages.ExtractMethodRefactoring_organize_imports, 
					new TextEdit[] {edit}
				));
			}
			root.addChild(fRewriter.rewriteAST(fDocument, fCUnit.getJavaScriptProject().getOptions(true)));
		} catch (BadLocationException e) {
			throw new CoreException(new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), IStatus.ERROR,
				e.getMessage(), e));
		} finally {
			bufferManager.disconnect(path, LocationKind.IFILE, new SubProgressMonitor(pm, 1));
			pm.done();
		}
		return result;
	}
	
	/**
	 * Returns the signature of the new method.
	 * 
	 * @return the signature of the extracted method
	 */
	public String getSignature() {
		return getSignature(fMethodName);
	}
	
	/**
	 * Returns the signature of the new method.
	 * 
	 * @param methodName the method name used for the new method
	 * @return the signature of the extracted method
	 */
	public String getSignature(String methodName) {
		FunctionDeclaration method= null;
		try {
			method= createNewMethod(methodName, false, null, StubUtility.getLineDelimiterUsed(fCUnit), null);
		} catch (CoreException cannotHappen) {
			// we don't generate a code block and java comments.
			Assert.isTrue(false);
		} catch (BadLocationException e) {
			// we don't generate a code block and java comments.
			Assert.isTrue(false);
		}
		method.setBody(fAST.newBlock());
		ASTFlattener flattener= new ASTFlattener() {
			public boolean visit(Block node) {
				return false;
			}
		};
		method.accept(flattener);
		return flattener.getResult();		
	}
	
	/**
	 * Returns the number of duplicate code snippets found.
	 * 
	 * @return the number of duplicate code fragments
	 */
	public int getNumberOfDuplicates() {
		if (fDuplicates == null)
			return 0;
		int result=0;
		for (int i= 0; i < fDuplicates.length; i++) {
			if (!fDuplicates[i].isMethodBody())
				result++;
		}
		return result;
	}
	
	public boolean getReplaceDuplicates() {
		return fReplaceDuplicates;
	}

	public void setReplaceDuplicates(boolean replace) {
		fReplaceDuplicates= replace;
	}
	
	public void setGenerateJavadoc(boolean generate) {
		fGenerateJavadoc= generate;
	}
	
	public boolean getGenerateJavadoc() {
		return fGenerateJavadoc;
	}
	
	//---- Helper methods ------------------------------------------------------------------------
	
	private void initializeParameterInfos() {
		IVariableBinding[] arguments= fAnalyzer.getArguments();
		fParameterInfos= new ArrayList(arguments.length);
		ASTNode root= fAnalyzer.getEnclosingBodyDeclaration();
		ParameterInfo vararg= null;
		for (int i= 0; i < arguments.length; i++) {
			IVariableBinding argument= arguments[i];
			if (argument == null)
				continue;
			VariableDeclaration declaration= ASTNodes.findVariableDeclaration(argument, root);
			boolean isVarargs= declaration instanceof SingleVariableDeclaration 
				? ((SingleVariableDeclaration)declaration).isVarargs()
				: false;
			ParameterInfo info= new ParameterInfo(argument, getType(declaration, isVarargs), argument.getName(), i);
			if (isVarargs) {
				vararg= info;
			} else {
				fParameterInfos.add(info);
			}
		}
		if (vararg != null) {
			fParameterInfos.add(vararg);
		}
	}
	
	private void initializeUsedNames() {
		fUsedNames= UsedNamesCollector.perform(fAnalyzer.getSelectedNodes());
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo parameter= (ParameterInfo)iter.next();
			fUsedNames.remove(parameter.getOldName());
		}
	}
	
	private void initializeDuplicates() {
		ASTNode start= fAnalyzer.getEnclosingBodyDeclaration();
		while(!(start instanceof JavaScriptUnit) &&
				!(start instanceof AbstractTypeDeclaration) &&
				!(start instanceof AnonymousClassDeclaration)) {
			start= start.getParent();
		}
		
		fDuplicates= SnippetFinder.perform(start, fAnalyzer.getSelectedNodes());
		fReplaceDuplicates= fDuplicates.length > 0 && ! fAnalyzer.isLiteralNodeSelected();
	}
	
	private void initializeDestinations() {
		List result= new ArrayList();
		BodyDeclaration decl= fAnalyzer.getEnclosingBodyDeclaration();
		ASTNode current= getNextParent(decl);
		result.add(current);
		if (decl instanceof FunctionDeclaration) {
			ITypeBinding binding= ASTNodes.getEnclosingType(current);
			ASTNode next= getNextParent(current);
			while (next != null && binding != null && binding.isNested() && !Modifier.isStatic(binding.getDeclaredModifiers())) {
				result.add(next);
				current= next;
				binding= ASTNodes.getEnclosingType(current);
				next= getNextParent(next);
			}
		}
		fDestinations= (ASTNode[])result.toArray(new ASTNode[result.size()]);
		fDestination= fDestinations[fDestinationIndex];
	}
	
	private ASTNode getNextParent(ASTNode node) {
		do {
			node= node.getParent();
		} while (node != null && !(
				(node instanceof JavaScriptUnit) ||(node instanceof AbstractTypeDeclaration) || (node instanceof AnonymousClassDeclaration)));
		return node;
	}
		
	private RefactoringStatus mergeTextSelectionStatus(RefactoringStatus status) {
		status.addFatalError(RefactoringCoreMessages.ExtractMethodRefactoring_no_set_of_statements); 
		return status;	
	}
	
	private String getType(VariableDeclaration declaration, boolean isVarargs) {
		String type= ASTNodes.asString(ASTNodeFactory.newType(declaration.getAST(), declaration));
		if (isVarargs)
			return type + ParameterInfo.ELLIPSIS;
		else
			return type;
	}
	
	//---- Code generation -----------------------------------------------------------------------
	
	private ASTNode[] createCallNodes(SnippetFinder.Match duplicate) {
		List result= new ArrayList(2);
		
		IVariableBinding[] locals= fAnalyzer.getCallerLocals();
		for (int i= 0; i < locals.length; i++) {
			result.add(createDeclaration(locals[i], null));
		}
		
		FunctionInvocation invocation= fAST.newFunctionInvocation();
		invocation.setName(fAST.newSimpleName(fMethodName));
		List arguments= invocation.arguments();
		for (int i= 0; i < fParameterInfos.size(); i++) {
			ParameterInfo parameter= ((ParameterInfo)fParameterInfos.get(i));
			arguments.add(ASTNodeFactory.newName(fAST, getMappedName(duplicate, parameter)));
		}		
		
		ASTNode call;		
		int returnKind= fAnalyzer.getReturnKind();
		switch (returnKind) {
			case ExtractMethodAnalyzer.ACCESS_TO_LOCAL:
				IVariableBinding binding= fAnalyzer.getReturnLocal();
				if (binding != null) {
					VariableDeclarationStatement decl= createDeclaration(getMappedBinding(duplicate, binding), invocation);
					call= decl;
				} else {
					Assignment assignment= fAST.newAssignment();
					assignment.setLeftHandSide(ASTNodeFactory.newName(fAST, 
							getMappedBinding(duplicate, fAnalyzer.getReturnValue()).getName()));
					assignment.setRightHandSide(invocation);
					call= assignment;
				}
				break;
			case ExtractMethodAnalyzer.RETURN_STATEMENT_VALUE:
				ReturnStatement rs= fAST.newReturnStatement();
				rs.setExpression(invocation);
				call= rs;
				break;
			default:
				call= invocation;
		}
		
		if (call instanceof Expression && !fAnalyzer.isExpressionSelected()) {
			call= fAST.newExpressionStatement((Expression)call);
		}
		result.add(call);
		
		// We have a void return statement. The code looks like
		// extracted();
		// return;	
		if (returnKind == ExtractMethodAnalyzer.RETURN_STATEMENT_VOID && !fAnalyzer.isLastStatementSelected()) {
			result.add(fAST.newReturnStatement());
		}
		return (ASTNode[])result.toArray(new ASTNode[result.size()]);		
	}
	
	private IVariableBinding getMappedBinding(SnippetFinder.Match duplicate, IVariableBinding org) {
		if (duplicate == null)
			return org;
		return duplicate.getMappedBinding(org);
	}
	
	private String getMappedName(SnippetFinder.Match duplicate, ParameterInfo paramter) {
		if (duplicate == null)
			return paramter.getOldName();
		return duplicate.getMappedName(paramter.getOldBinding()).getIdentifier();
	}
	
	private void replaceDuplicates(CompilationUnitChange result) {
		int numberOf= getNumberOfDuplicates();
		if (numberOf == 0 || !fReplaceDuplicates)
			return;
		String label= null;
		if (numberOf == 1)
			label= Messages.format(RefactoringCoreMessages.ExtractMethodRefactoring_duplicates_single, fMethodName); 
		else
			label= Messages.format(RefactoringCoreMessages.ExtractMethodRefactoring_duplicates_multi, fMethodName); 
		
		TextEditGroup description= new TextEditGroup(label);
		result.addTextEditGroup(description);
		
		for (int d= 0; d < fDuplicates.length; d++) {
			SnippetFinder.Match duplicate= fDuplicates[d];
			if (!duplicate.isMethodBody()) {
				ASTNode[] callNodes= createCallNodes(duplicate);
				new StatementRewrite(fRewriter, duplicate.getNodes()).replace(callNodes, description);
			}
		}		
	}
	
	private FunctionDeclaration createNewMethod(String name, boolean code, ASTNode[] selectedNodes, String lineDelimiter, TextEditGroup substitute) throws CoreException, BadLocationException {
		FunctionDeclaration result= fAST.newFunctionDeclaration();
		int modifiers= fVisibility;
		if (Modifier.isStatic(fAnalyzer.getEnclosingBodyDeclaration().getModifiers()) || fAnalyzer.getForceStatic()) {
			modifiers|= Modifier.STATIC;
		}
		
		result.modifiers().addAll(ASTNodeFactory.newModifiers(fAST, modifiers));
		result.setReturnType2((Type)ASTNode.copySubtree(fAST, fAnalyzer.getReturnType()));
		result.setName(fAST.newSimpleName(name));
		
		List parameters= result.parameters();
		for (int i= 0; i < fParameterInfos.size(); i++) {
			ParameterInfo info= (ParameterInfo)fParameterInfos.get(i);
			VariableDeclaration infoDecl= getVariableDeclaration(info);
			SingleVariableDeclaration parameter= fAST.newSingleVariableDeclaration();
			parameter.modifiers().addAll(ASTNodeFactory.newModifiers(fAST, ASTNodes.getModifiers(infoDecl)));
			parameter.setType(ASTNodeFactory.newType(fAST, infoDecl));
			parameter.setName(fAST.newSimpleName(info.getNewName()));
			parameter.setVarargs(info.isNewVarargs());
			parameters.add(parameter);
		}
		
		List exceptions= result.thrownExceptions();
		ITypeBinding[] exceptionTypes= fAnalyzer.getExceptions(fThrowRuntimeExceptions, fAST);
		for (int i= 0; i < exceptionTypes.length; i++) {
			ITypeBinding exceptionType= exceptionTypes[i];
			exceptions.add(ASTNodeFactory.newName(fAST, fImportRewriter.addImport(exceptionType)));
		}
		if (code) {
			result.setBody(createMethodBody(result, selectedNodes, substitute));
			if (fGenerateJavadoc) {
				AbstractTypeDeclaration enclosingType= 
					(AbstractTypeDeclaration)ASTNodes.getParent(fAnalyzer.getEnclosingBodyDeclaration(), AbstractTypeDeclaration.class);

				String typeName = (enclosingType!=null)?enclosingType.getName().getIdentifier():null;
				String string= CodeGeneration.getMethodComment(fCUnit, typeName, result, null, lineDelimiter);
				if (string != null) {
					JSdoc javadoc= (JSdoc)fRewriter.createStringPlaceholder(string, ASTNode.JSDOC);
					result.setJavadoc(javadoc);
				}
			}
		}
		
		return result;
	}
	
	private Block createMethodBody(FunctionDeclaration method, ASTNode[] selectedNodes, TextEditGroup substitute) throws BadLocationException, CoreException {
		Block result= fAST.newBlock();
		ListRewrite statements= fRewriter.getListRewrite(result, Block.STATEMENTS_PROPERTY);
		
		// Locals that are not passed as an arguments since the extracted method only
		// writes to them
		IVariableBinding[] methodLocals= fAnalyzer.getMethodLocals();
		for (int i= 0; i < methodLocals.length; i++) {
			if (methodLocals[i] != null) {
				result.statements().add(createDeclaration(methodLocals[i], null));
			}
		}

		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo parameter= (ParameterInfo)iter.next();
			if (parameter.isRenamed()) {
				for (int n= 0; n < selectedNodes.length; n++) {
					SimpleName[] oldNames= LinkedNodeFinder.findByBinding(selectedNodes[n], parameter.getOldBinding());
					for (int i= 0; i < oldNames.length; i++) {
						fRewriter.replace(oldNames[i], fAST.newSimpleName(parameter.getNewName()), null);
					}
				}
			}
		}
		
		boolean extractsExpression= fAnalyzer.isExpressionSelected();
		ASTNode[] callNodes= createCallNodes(null);
		ASTNode replacementNode;
		if (callNodes.length == 1) {
			replacementNode= callNodes[0];
		} else {
			replacementNode= fRewriter.createGroupNode(callNodes);
		}
		if (extractsExpression) {
			// if we have an expression then only one node is selected.
			ITypeBinding binding= fAnalyzer.getExpressionBinding();
			if (binding != null && (!binding.isPrimitive() || !"void".equals(binding.getName()))) { //$NON-NLS-1$
				ReturnStatement rs= fAST.newReturnStatement();
				rs.setExpression((Expression)fRewriter.createMoveTarget(selectedNodes[0]));
				statements.insertLast(rs, null);
			} else {
				ExpressionStatement st= fAST.newExpressionStatement((Expression)fRewriter.createMoveTarget(selectedNodes[0]));
				statements.insertLast(st, null);
			}
			fRewriter.replace(selectedNodes[0], replacementNode, substitute);
		} else {
			if (selectedNodes.length == 1) {
				statements.insertLast(fRewriter.createMoveTarget(selectedNodes[0]), substitute);
				fRewriter.replace(selectedNodes[0], replacementNode, substitute);
			} else {
				ListRewrite source= fRewriter.getListRewrite(
					selectedNodes[0].getParent(), 
					(ChildListPropertyDescriptor)selectedNodes[0].getLocationInParent());
				ASTNode toMove= source.createMoveTarget(
					selectedNodes[0], selectedNodes[selectedNodes.length - 1],
					replacementNode, substitute);
				statements.insertLast(toMove, substitute);
			}
			IVariableBinding returnValue= fAnalyzer.getReturnValue();
			if (returnValue != null) {
				ReturnStatement rs= fAST.newReturnStatement();
				rs.setExpression(fAST.newSimpleName(getName(returnValue)));
				statements.insertLast(rs, null);				
			}
		}
		return result;
	}
	
	private String getName(IVariableBinding binding) {
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo)iter.next();
			if (Bindings.equals(binding, info.getOldBinding())) {
				return info.getNewName();
			}
		}
		return binding.getName();
	}
	
	private VariableDeclaration getVariableDeclaration(ParameterInfo parameter) {
		return ASTNodes.findVariableDeclaration(parameter.getOldBinding(), fAnalyzer.getEnclosingBodyDeclaration());
	}
	
	private VariableDeclarationStatement createDeclaration(IVariableBinding binding, Expression intilizer) {
		VariableDeclaration original= ASTNodes.findVariableDeclaration(binding, fAnalyzer.getEnclosingBodyDeclaration());
		VariableDeclarationFragment fragment= fAST.newVariableDeclarationFragment();
		fragment.setName((SimpleName)ASTNode.copySubtree(fAST, original.getName()));
		fragment.setInitializer(intilizer);	
		VariableDeclarationStatement result= fAST.newVariableDeclarationStatement(fragment);
		result.modifiers().addAll(ASTNode.copySubtrees(fAST, ASTNodes.getModifiers(original)));
		result.setType(ASTNodeFactory.newType(fAST, original));
		return result;
	}	

	public IJavaScriptUnit getCompilationUnit() {
		return fCUnit;
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
					return createInputFatalStatus(element, IJavaScriptRefactorings.EXTRACT_METHOD);
				else {
					fCUnit= (IJavaScriptUnit) element;
		        	try {
						initialize(fCUnit);
					} catch (CoreException exception) {
						JavaScriptPlugin.log(exception);
					}
				}
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
				fVisibility= flag;
			}
			final String name= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_NAME);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				fMethodName= name;
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_NAME));
			final String destination= extended.getAttribute(ATTRIBUTE_DESTINATION);
			if (destination != null && !"".equals(destination)) {//$NON-NLS-1$
				int index= 0;
				try {
					index= Integer.parseInt(destination);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DESTINATION));
				}
				fDestinationIndex= index;
			}
			final String replace= extended.getAttribute(ATTRIBUTE_REPLACE);
			if (replace != null) {
				fReplaceDuplicates= Boolean.valueOf(replace).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_REPLACE));
			final String comments= extended.getAttribute(ATTRIBUTE_COMMENTS);
			if (comments != null)
				fGenerateJavadoc= Boolean.valueOf(comments).booleanValue();
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_COMMENTS));
			final String exceptions= extended.getAttribute(ATTRIBUTE_EXCEPTIONS);
			if (exceptions != null)
				fThrowRuntimeExceptions= Boolean.valueOf(exceptions).booleanValue();
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_EXCEPTIONS));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}	
}
