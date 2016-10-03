/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.rename;

import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.RenameLocalVariableDescriptor;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.RefactoringDescriptorChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.wst.jsdt.internal.corext.refactoring.code.ScriptableRefactoring;
import org.eclipse.wst.jsdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenameAnalyzeUtil.LocalAnalyzePackage;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

public class RenameLocalVariableProcessor extends JavaRenameProcessor implements IReferenceUpdating {

	private ILocalVariable fLocalVariable;
	private IJavaScriptUnit fCu;
	
	//the following fields are set or modified after the construction
	private boolean fUpdateReferences;
	private String fCurrentName;
	private String fNewName;
	private JavaScriptUnit fCompilationUnitNode;
	private VariableDeclaration fTempDeclarationNode;
	private TextChange fChange;
	
	private boolean fIsComposite;
	private GroupCategorySet fCategorySet;
	private TextChangeManager fChangeManager;
	private RenameAnalyzeUtil.LocalAnalyzePackage fLocalAnalyzePackage;

	public static final String IDENTIFIER= "org.eclipse.wst.jsdt.ui.renameLocalVariableProcessor"; //$NON-NLS-1$
	
	/**
	 * Creates a new rename local variable processor.
	 * @param localVariable the local variable, or <code>null</code> if invoked by scripting
	 */
	public RenameLocalVariableProcessor(ILocalVariable localVariable) {
		fLocalVariable= localVariable;
		fUpdateReferences= true;
		if (localVariable != null)
			fCu= (IJavaScriptUnit) localVariable.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
		fNewName= ""; //$NON-NLS-1$
		fIsComposite= false;
	}
	
	/**
	 * Creates a new rename local variable processor.
	 * <p>
	 * This constructor is only used by <code>RenameTypeProcessor</code>.
	 * </p>
	 * 
	 * @param localVariable the local variable
	 * @param manager the change manager
	 * @param node the compilation unit node
	 * @param categorySet the group category set
	 */
	RenameLocalVariableProcessor(ILocalVariable localVariable, TextChangeManager manager, JavaScriptUnit node, GroupCategorySet categorySet) {
		this(localVariable);
		fChangeManager= manager;
		fCategorySet= categorySet;
		fCompilationUnitNode= node;
		fIsComposite= true;
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.rename.JavaRenameProcessor#getAffectedProjectNatures()
	 */
	protected final String[] getAffectedProjectNatures() throws CoreException {
		return JavaProcessors.computeAffectedNatures(fLocalVariable);
	}
	
	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getElements()
	 */
	public Object[] getElements() {
		return new Object[] { fLocalVariable };
	}
	
	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getIdentifier()
	 */
	public String getIdentifier() {
		return IDENTIFIER;
	}
	
	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getProcessorName()
	 */
	public String getProcessorName() {
		return RefactoringCoreMessages.RenameTempRefactoring_rename; 
	}
	
	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#isApplicable()
	 */
	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTester.isRenameAvailable(fLocalVariable);
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IReferenceUpdating#canEnableUpdateReferences()
	 */
	public boolean canEnableUpdateReferences() {
		return true;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.rename.JavaRenameProcessor#getUpdateReferences()
	 */
	public boolean getUpdateReferences() {
		return fUpdateReferences;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IReferenceUpdating#setUpdateReferences(boolean)
	 */
	public void setUpdateReferences(boolean updateReferences) {
		fUpdateReferences= updateReferences;
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.INameUpdating#getCurrentElementName()
	 */
	public String getCurrentElementName() {
		return fCurrentName;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.INameUpdating#getNewElementName()
	 */
	public String getNewElementName() {
		return fNewName;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.INameUpdating#setNewElementName(java.lang.String)
	 */
	public void setNewElementName(String newName) {
		Assert.isNotNull(newName);
		fNewName= newName;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.INameUpdating#getNewElement()
	 */
	public Object getNewElement() {
		return null; //cannot create an ILocalVariable
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		initAST();
		if (fTempDeclarationNode == null || fTempDeclarationNode.resolveBinding() == null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameTempRefactoring_must_select_local); 
		//if (! Checks.isDeclaredIn(fTempDeclarationNode, FunctionDeclaration.class) 
		 //&& ! Checks.isDeclaredIn(fTempDeclarationNode, Initializer.class))
			//return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameTempRefactoring_only_in_methods_and_initializers); 
				
		initNames();			
		return new RefactoringStatus();
	}

	private void initAST() throws JavaScriptModelException {
		if (!fIsComposite)
			fCompilationUnitNode= RefactoringASTParser.parseWithASTProvider(fCu, true, null);
		ISourceRange sourceRange= fLocalVariable.getNameRange();
		ASTNode name= NodeFinder.perform(fCompilationUnitNode, sourceRange);
		if (name == null)
			return;
		if (name.getParent() instanceof VariableDeclaration)
			fTempDeclarationNode= (VariableDeclaration) name.getParent();
	}
	
	private void initNames(){
		fCurrentName= fTempDeclarationNode.getName().getIdentifier();
	}
	
	protected RenameModifications computeRenameModifications() throws CoreException {
		RenameModifications result= new RenameModifications();
		result.rename(fLocalVariable, new RenameArguments(getNewElementName(), getUpdateReferences()));
		return result;
	}
	
	protected IFile[] getChangedFiles() throws CoreException {
		return new IFile[] {ResourceUtil.getFile(fCu)};
	}
	
	public int getSaveMode() {
		return RefactoringSaveHelper.SAVE_NOTHING;
	}
	
	protected RefactoringStatus doCheckFinalConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws CoreException, OperationCanceledException {
		try {
			pm.beginTask("", 1);	 //$NON-NLS-1$

			RefactoringStatus result= checkNewElementName(fNewName);
			if (result.hasFatalError())
				return result;
			createEdits();
			if (!fIsComposite) {
				LocalAnalyzePackage[] localAnalyzePackages= new RenameAnalyzeUtil.LocalAnalyzePackage[] { fLocalAnalyzePackage };
				result.merge(RenameAnalyzeUtil.analyzeLocalRenames(localAnalyzePackages, fChange, fCompilationUnitNode, true));
			}
			return result;
		} finally {
			pm.done();
			if (fIsComposite) {
				// end of life cycle for this processor
				fChange= null;
				fCompilationUnitNode= null;
				fTempDeclarationNode= null;
			}
		}	
	}
		
	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.INameUpdating#checkNewElementName(java.lang.String)
	 */
	public RefactoringStatus checkNewElementName(String newName) throws JavaScriptModelException {
		RefactoringStatus result= Checks.checkFieldName(newName);
		if (! Checks.startsWithLowerCase(newName))
			if (fIsComposite) {
				final String nameOfParent= (fLocalVariable.getParent() instanceof IFunction) ? fLocalVariable.getParent().getElementName() : RefactoringCoreMessages.JavaElementUtil_initializer;
				final String nameOfType= fLocalVariable.getAncestor(IJavaScriptElement.TYPE).getElementName();
				result.addWarning(Messages.format(RefactoringCoreMessages.RenameTempRefactoring_lowercase2, new String[] { newName, nameOfParent, nameOfType }));
			} else {
				result.addWarning(RefactoringCoreMessages.RenameTempRefactoring_lowercase);
			}
		return result;		
	}
		
	private void createEdits() {
		TextEdit declarationEdit= createRenameEdit(fTempDeclarationNode.getName().getStartPosition());
		TextEdit[] allRenameEdits= getAllRenameEdits(declarationEdit);
		
		TextEdit[] allUnparentedRenameEdits= new TextEdit[allRenameEdits.length];
		TextEdit unparentedDeclarationEdit= null;
		
		fChange= new CompilationUnitChange(RefactoringCoreMessages.RenameTempRefactoring_rename, fCu); 
		MultiTextEdit rootEdit= new MultiTextEdit();
		fChange.setEdit(rootEdit);
		fChange.setKeepPreviewEdits(true);

		for (int i= 0; i < allRenameEdits.length; i++) {
			if (fIsComposite) {
				// Add a copy of the text edit (text edit may only have one
				// parent) to keep problem reporting code clean
				TextChangeCompatibility.addTextEdit(fChangeManager.get(fCu), RefactoringCoreMessages.RenameTempRefactoring_changeName, allRenameEdits[i].copy(), fCategorySet);
				
				// Add a separate copy for problem reporting
				allUnparentedRenameEdits[i]= allRenameEdits[i].copy();
				if (allRenameEdits[i].equals(declarationEdit))
					unparentedDeclarationEdit= allUnparentedRenameEdits[i];
			}
			rootEdit.addChild(allRenameEdits[i]);
			fChange.addTextEditGroup(new TextEditGroup(RefactoringCoreMessages.RenameTempRefactoring_changeName, allRenameEdits[i]));
		}

		// store information for analysis
		if (fIsComposite) {
			fLocalAnalyzePackage= new RenameAnalyzeUtil.LocalAnalyzePackage(unparentedDeclarationEdit, allUnparentedRenameEdits);
		} else 
			fLocalAnalyzePackage= new RenameAnalyzeUtil.LocalAnalyzePackage(declarationEdit, allRenameEdits);
	}
	
	private TextEdit[] getAllRenameEdits(TextEdit declarationEdit) {
		if (! fUpdateReferences)
			return new TextEdit[] { declarationEdit };
		
		TempOccurrenceAnalyzer fTempAnalyzer= new TempOccurrenceAnalyzer(fTempDeclarationNode, true);
		fTempAnalyzer.perform();
		int[] referenceOffsets= fTempAnalyzer.getReferenceAndJavadocOffsets();

		TextEdit[] allRenameEdits= new TextEdit[referenceOffsets.length + 1];
		for (int i= 0; i < referenceOffsets.length; i++)
			allRenameEdits[i]= createRenameEdit(referenceOffsets[i]);
		allRenameEdits[referenceOffsets.length]= declarationEdit;
		return allRenameEdits;
	}

	private TextEdit createRenameEdit(int offset) {
		return new ReplaceEdit(offset, fCurrentName.length(), fNewName);
	}

	public Change createChange(IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(RefactoringCoreMessages.RenameTypeProcessor_creating_changes, 1);
			Change change= fChange;
			if (change != null) {
				final ISourceRange range= fLocalVariable.getNameRange();
				String project= null;
				IJavaScriptProject javaProject= fCu.getJavaScriptProject();
				if (javaProject != null)
					project= javaProject.getElementName();
				final String header= Messages.format(RefactoringCoreMessages.RenameLocalVariableProcessor_descriptor_description, new String[] { fCurrentName, JavaScriptElementLabels.getElementLabel(fLocalVariable.getParent(), JavaScriptElementLabels.ALL_FULLY_QUALIFIED), fNewName});
				final String description= Messages.format(RefactoringCoreMessages.RenameLocalVariableProcessor_descriptor_description_short, fCurrentName);
				final String comment= new JDTRefactoringDescriptorComment(project, this, header).asString();
				final RenameLocalVariableDescriptor descriptor= new RenameLocalVariableDescriptor();
				descriptor.setProject(project);
				descriptor.setDescription(description);
				descriptor.setComment(comment);
				descriptor.setFlags(RefactoringDescriptor.NONE);
				descriptor.setCompilationUnit(fCu);
				descriptor.setNewName(getNewElementName());
				descriptor.setSelection(range);
				descriptor.setUpdateReferences(fUpdateReferences);
				final RefactoringDescriptorChange result= new RefactoringDescriptorChange(descriptor, RefactoringCoreMessages.RenameTempRefactoring_rename, new Change[] { change});
				result.markAsSynthetic();
				change= result;
			}
			return change;
		} finally {
			monitor.done();
		}
	}

	public RefactoringStatus initialize(RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			final String handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element != null && element.exists()) {
					if (element.getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT) {
						fCu= (IJavaScriptUnit) element;
					} else if (element.getElementType() == IJavaScriptElement.LOCAL_VARIABLE) {
						fLocalVariable= (ILocalVariable) element;
						fCu= (IJavaScriptUnit) fLocalVariable.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
						if (fCu == null)
							return ScriptableRefactoring.createInputFatalStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.RENAME_LOCAL_VARIABLE);
					} else
						return ScriptableRefactoring.createInputFatalStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.RENAME_LOCAL_VARIABLE);
				} else
					return ScriptableRefactoring.createInputFatalStatus(element, getRefactoring().getName(), IJavaScriptRefactorings.RENAME_LOCAL_VARIABLE);
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
			final String name= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_NAME);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				setNewElementName(name);
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_NAME));
			if (fCu != null && fLocalVariable == null) {
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
						try {
							final IJavaScriptElement[] elements= fCu.codeSelect(offset, length);
							if (elements != null) {
								for (int index= 0; index < elements.length; index++) {
									final IJavaScriptElement element= elements[index];
									if (element instanceof ILocalVariable)
										fLocalVariable= (ILocalVariable) element;
								}
							}
							if (fLocalVariable == null)
								return ScriptableRefactoring.createInputFatalStatus(null, getRefactoring().getName(), IJavaScriptRefactorings.RENAME_LOCAL_VARIABLE);
						} catch (JavaScriptModelException exception) {
							JavaScriptPlugin.log(exception);
						}
					} else
						return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_illegal_argument, new Object[] { selection, JDTRefactoringDescriptor.ATTRIBUTE_SELECTION}));
				} else
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_SELECTION));
			}
			final String references= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_REFERENCES);
			if (references != null) {
				fUpdateReferences= Boolean.valueOf(references).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_REFERENCES));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}

	public RenameAnalyzeUtil.LocalAnalyzePackage getLocalAnalyzePackage() {
		return fLocalAnalyzePackage;
	}
}
