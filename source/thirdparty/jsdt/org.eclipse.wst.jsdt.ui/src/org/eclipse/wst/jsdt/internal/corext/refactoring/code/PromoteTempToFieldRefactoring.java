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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ArrayCreation;
import org.eclipse.wst.jsdt.core.dom.ArrayInitializer;
import org.eclipse.wst.jsdt.core.dom.ArrayType;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.CatchClause;
import org.eclipse.wst.jsdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ListRewrite;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.HierarchicalASTVisitor;
import org.eclipse.wst.jsdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.wst.jsdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.wst.jsdt.internal.corext.fix.LinkedProposalPositionGroup;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.TempDeclarationFinder;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.TempOccurrenceAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.BindingLabelProvider;
import org.eclipse.wst.jsdt.ui.CodeGeneration;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

public class PromoteTempToFieldRefactoring extends ScriptableRefactoring {

	private static final String ATTRIBUTE_STATIC= "static"; //$NON-NLS-1$
	private static final String ATTRIBUTE_FINAL= "final"; //$NON-NLS-1$
	private static final String ATTRIBUTE_VISIBILITY= "visibility"; //$NON-NLS-1$
	private static final String ATTRIBUTE_INITIALIZE= "initialize"; //$NON-NLS-1$

	private int fSelectionStart;
    private int fSelectionLength;
    private IJavaScriptUnit fCu;
	
	public static final int INITIALIZE_IN_FIELD= 0;
	public static final int INITIALIZE_IN_METHOD= 1;
	public static final int INITIALIZE_IN_CONSTRUCTOR= 2;
	
	private static final String LINKED_NAME= "name"; //$NON-NLS-1$
	
	//------ settings ---------//
	private String fFieldName;
	private int fVisibility; 	/*see Modifier*/
	private boolean fDeclareStatic;
	private boolean fDeclareFinal;
	private int fInitializeIn; /*see INITIALIZE_IN_* constraints */

	//------ fields used for computations ---------//
    private JavaScriptUnit fCompilationUnitNode;
    private VariableDeclaration fTempDeclarationNode;
	//------ analysis ---------//
	private boolean fInitializerUsesLocalTypes;
	private boolean fTempTypeUsesClassTypeVariables;
	//------ scripting --------//
	private boolean fSelfInitializing= false;
	private LinkedProposalModel fLinkedProposalModel;

	/**
	 * Creates a new promote temp to field refactoring.
	 * @param unit the compilation unit, or <code>null</code> if invoked by scripting
	 * @param selectionStart
	 * @param selectionLength
	 */
	public PromoteTempToFieldRefactoring(IJavaScriptUnit unit, int selectionStart, int selectionLength){
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= unit;
		
        fFieldName= ""; //$NON-NLS-1$
        fVisibility= Modifier.PRIVATE;
        fDeclareStatic= false;
        fDeclareFinal= false;
        fInitializeIn= INITIALIZE_IN_METHOD;
        fLinkedProposalModel= null;
	}
	
	/**
	 * Creates a new promote temp to field refactoring.
	 * @param declaration the variable declaration node to convert to a field
	 */
	public PromoteTempToFieldRefactoring(VariableDeclaration declaration) {
		Assert.isTrue(declaration != null);
		fTempDeclarationNode= declaration;
		IVariableBinding resolveBinding= declaration.resolveBinding();
		Assert.isTrue(resolveBinding != null && !resolveBinding.isParameter() && !resolveBinding.isField());

		ASTNode root= declaration.getRoot();
		Assert.isTrue(root instanceof JavaScriptUnit);
		fCompilationUnitNode= (JavaScriptUnit) root;
		
		IJavaScriptElement input= fCompilationUnitNode.getJavaElement();
		Assert.isTrue(input instanceof IJavaScriptUnit);
		fCu= (IJavaScriptUnit) input;

		fSelectionStart= declaration.getStartPosition();
		fSelectionLength= declaration.getLength();
		
        fFieldName= ""; //$NON-NLS-1$
        fVisibility= Modifier.PRIVATE;
        fDeclareStatic= false;
        fDeclareFinal= false;
        fInitializeIn= INITIALIZE_IN_METHOD;
        fLinkedProposalModel= null;
	}

    public String getName() {
        return RefactoringCoreMessages.PromoteTempToFieldRefactoring_name; 
    }
    
    public int[] getAvailableVisibilities(){
    	return new int[]{Modifier.PUBLIC, Modifier.PROTECTED, Modifier.NONE, Modifier.PRIVATE};
    }
    
    public int getVisibility() {
        return fVisibility;
    }

    public boolean getDeclareFinal() {
        return fDeclareFinal;
    }

    public boolean getDeclareStatic() {
        return fDeclareStatic;
    }

    public int getInitializeIn() {
        return fInitializeIn;
    }

    public void setVisibility(int accessModifier) {
    	Assert.isTrue(accessModifier == Modifier.PRIVATE ||
    					accessModifier == Modifier.NONE ||
    					accessModifier == Modifier.PROTECTED ||
    					accessModifier == Modifier.PUBLIC);
        fVisibility= accessModifier;
    }

    public void setDeclareFinal(boolean declareFinal) {
        fDeclareFinal= declareFinal;
    }

    public void setDeclareStatic(boolean declareStatic) {
        fDeclareStatic= declareStatic;
    }

    public void setFieldName(String fieldName) {
    	Assert.isNotNull(fieldName);
        fFieldName= fieldName;
    }

    public void setInitializeIn(int initializeIn) {
    	Assert.isTrue(	initializeIn == INITIALIZE_IN_CONSTRUCTOR ||
    					initializeIn == INITIALIZE_IN_FIELD ||
    					initializeIn == INITIALIZE_IN_METHOD);
        fInitializeIn= initializeIn;
    }
	
	public boolean canEnableSettingStatic(){
		return fInitializeIn != INITIALIZE_IN_CONSTRUCTOR &&
				! isTempDeclaredInStaticMethod() &&
				! fTempTypeUsesClassTypeVariables;
	}
	
	public boolean canEnableSettingFinal(){
		if (fInitializeIn == INITIALIZE_IN_CONSTRUCTOR)
			return  canEnableSettingDeclareInConstructors() && ! tempHasAssignmentsOtherThanInitialization();
		else if (fInitializeIn == INITIALIZE_IN_FIELD)	
			return  canEnableSettingDeclareInFieldDeclaration() && ! tempHasAssignmentsOtherThanInitialization();
		else	if (getMethodDeclaration().isConstructor())
			return  !tempHasAssignmentsOtherThanInitialization();
		else 
			return false;
	}
	
    private boolean tempHasAssignmentsOtherThanInitialization() {
    	TempAssignmentFinder assignmentFinder= new TempAssignmentFinder(fTempDeclarationNode);
    	fCompilationUnitNode.accept(assignmentFinder);
		return assignmentFinder.hasAssignments();
    }
	
	public boolean canEnableSettingDeclareInConstructors(){
		return ! fDeclareStatic &&
				! fInitializerUsesLocalTypes &&
				! getMethodDeclaration().isConstructor() &&
				! isDeclaredInAnonymousClass() && 
				! isTempDeclaredInStaticMethod() && 
				tempHasInitializer();
	}
	
	public boolean canEnableSettingDeclareInMethod(){
		return ! fDeclareFinal && 
				tempHasInitializer();
	}
    private boolean tempHasInitializer() {
        return getTempInitializer() != null;
    }

	public boolean canEnableSettingDeclareInFieldDeclaration(){
		return ! fInitializerUsesLocalTypes && tempHasInitializer();
	}

    private Expression getTempInitializer() {
    	return fTempDeclarationNode.getInitializer();
    }

    private boolean isTempDeclaredInStaticMethod() {
    	return Modifier.isStatic(getMethodDeclaration().getModifiers());
    }
    
    private FunctionDeclaration getMethodDeclaration(){
    	return (FunctionDeclaration)ASTNodes.getParent(fTempDeclarationNode, FunctionDeclaration.class);
    }

    private boolean isDeclaredInAnonymousClass() {
    	return null != ASTNodes.getParent(fTempDeclarationNode, AnonymousClassDeclaration.class);
    }
	
    /*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= Checks.validateModifiesFiles(
			ResourceUtil.getFiles(new IJavaScriptUnit[]{fCu}),
			getValidationContext());
		if (result.hasFatalError())
			return result;

		initAST(pm);

		if (fTempDeclarationNode == null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.PromoteTempToFieldRefactoring_select_declaration); 
		
		if (! Checks.isDeclaredIn(fTempDeclarationNode, FunctionDeclaration.class))
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.PromoteTempToFieldRefactoring_only_declared_in_methods); 
		
		if (isMethodParameter())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.PromoteTempToFieldRefactoring_method_parameters); 

		if (isTempAnExceptionInCatchBlock())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.PromoteTempToFieldRefactoring_exceptions); 

		result.merge(checkTempTypeForLocalTypeUsage());
		if (result.hasFatalError())
		    return result;
		
		checkTempInitializerForLocalTypeUsage();
		
		if (!fSelfInitializing)
			initializeDefaults();
		return result;
	}
    
    private void initializeDefaults() {
        fVisibility= Modifier.PRIVATE;
        fDeclareStatic= Modifier.isStatic(getMethodDeclaration().getModifiers());
        fDeclareFinal= false;
        if (canEnableSettingDeclareInMethod())
	        fInitializeIn= INITIALIZE_IN_METHOD;
	    else if (canEnableSettingDeclareInFieldDeclaration())    
	        fInitializeIn= INITIALIZE_IN_FIELD;
	    else if (canEnableSettingDeclareInConstructors())    
	        fInitializeIn= INITIALIZE_IN_CONSTRUCTOR;
    }
    
	public String[] guessFieldNames() {
		String rawTempName= StubUtility.removePrefixAndSuffixForVariable(fCu.getJavaScriptProject(), fTempDeclarationNode.resolveBinding());
		String[] excludedNames= getNamesOfFieldsInDeclaringType();
		int dim= ASTNodes.getDimensions(fTempDeclarationNode);
		return StubUtility.getFieldNameSuggestions(fCu.getJavaScriptProject(), rawTempName, dim, getModifiers(), excludedNames);
	}

    private String getInitialFieldName() {
    	String[] suggestedNames= guessFieldNames();
		if (suggestedNames.length > 0) {
			if (fLinkedProposalModel != null) {
				LinkedProposalPositionGroup nameGroup= fLinkedProposalModel.getPositionGroup(LINKED_NAME, true);
				for (int i= 0; i < suggestedNames.length; i++) {
					nameGroup.addProposal(suggestedNames[i], null, suggestedNames.length - i);
				}
			}
			return suggestedNames[0];
		} else {
			return fTempDeclarationNode.getName().getIdentifier();
		}
	}

	private String[] getNamesOfFieldsInDeclaringType() {
		final AbstractTypeDeclaration type= getEnclosingType();
		if (type instanceof TypeDeclaration) {
			FieldDeclaration[] fields= ((TypeDeclaration) type).getFields();
			List result= new ArrayList(fields.length);
			for (int i= 0; i < fields.length; i++) {
				for (Iterator iter= fields[i].fragments().iterator(); iter.hasNext();) {
					VariableDeclarationFragment field= (VariableDeclarationFragment) iter.next();
					result.add(field.getName().getIdentifier());
				}
			}
			return (String[]) result.toArray(new String[result.size()]);
		}
		return new String[] {};
	}
        
    private void checkTempInitializerForLocalTypeUsage() {
    	Expression initializer= fTempDeclarationNode.getInitializer();
    	if (initializer == null)
	        return;
	    
	    LocalTypeAndVariableUsageAnalyzer localTypeAnalyer= new LocalTypeAndVariableUsageAnalyzer(null);
	    initializer.accept(localTypeAnalyer);
	    fInitializerUsesLocalTypes= ! localTypeAnalyer.getUsageOfEnclosingNodes().isEmpty();
    }
    
    private RefactoringStatus checkTempTypeForLocalTypeUsage(){
    	VariableDeclarationStatement vds= getTempDeclarationStatement();
    	if (vds == null)
    		return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.PromoteTempToFieldRefactoring_cannot_promote); 
    	Type type= 	vds.getType();
    	ITypeBinding binding= type.resolveBinding();
    	if (binding == null)
    		return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.PromoteTempToFieldRefactoring_cannot_promote); 
    	
		LocalTypeAndVariableUsageAnalyzer analyzer= new LocalTypeAndVariableUsageAnalyzer(null);
		type.accept(analyzer);
		boolean usesLocalTypes= ! analyzer.getUsageOfEnclosingNodes().isEmpty();
		fTempTypeUsesClassTypeVariables= analyzer.getClassTypeVariablesUsed();
		if (usesLocalTypes)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.PromoteTempToFieldRefactoring_uses_type_declared_locally); 
		return null;    	
    }
    
    private VariableDeclarationStatement getTempDeclarationStatement() {
        return (VariableDeclarationStatement) ASTNodes.getParent(fTempDeclarationNode, VariableDeclarationStatement.class);
    }
    
    private boolean isTempAnExceptionInCatchBlock() {
		return (fTempDeclarationNode.getParent() instanceof CatchClause);
    }
    
    private boolean isMethodParameter() {
    	return (fTempDeclarationNode.getParent() instanceof FunctionDeclaration);
    }
    
	private void initAST(IProgressMonitor pm){
		if (fCompilationUnitNode == null) {
			fCompilationUnitNode= RefactoringASTParser.parseWithASTProvider(fCu, true, pm);
			fTempDeclarationNode= TempDeclarationFinder.findTempDeclaration(fCompilationUnitNode, fSelectionStart, fSelectionLength);
		}
	}

	public RefactoringStatus validateInput(){
		return Checks.checkFieldName(fFieldName);
	}
	
    /*
     * @see org.eclipse.wst.jsdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
     */
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
    	try{
	        RefactoringStatus result= new RefactoringStatus();	        
	        result.merge(checkClashesWithExistingFields());  
	        if (fInitializeIn == INITIALIZE_IN_CONSTRUCTOR)
		        result.merge(checkClashesInConstructors());  
	        return result;
    	} finally {
    		pm.done();
    	}
    }

    private RefactoringStatus checkClashesInConstructors() {
		Assert.isTrue(fInitializeIn == INITIALIZE_IN_CONSTRUCTOR);
		Assert.isTrue(!isDeclaredInAnonymousClass());
		final AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) getMethodDeclaration().getParent();
		if (declaration instanceof TypeDeclaration) {
			FunctionDeclaration[] methods= ((TypeDeclaration) declaration).getMethods();
			for (int i= 0; i < methods.length; i++) {
				FunctionDeclaration method= methods[i];
				if (!method.isConstructor())
					continue;
				NameCollector nameCollector= new NameCollector(method) {
					protected boolean visitNode(ASTNode node) {
						return true;
					}
				};
				method.accept(nameCollector);
				List names= nameCollector.getNames();
				if (names.contains(fFieldName)) {
					String[] keys= { fFieldName, BindingLabelProvider.getBindingLabel(method.resolveBinding(), JavaScriptElementLabels.ALL_FULLY_QUALIFIED)};
					String msg= Messages.format(RefactoringCoreMessages.PromoteTempToFieldRefactoring_Name_conflict, keys); 
					return RefactoringStatus.createFatalErrorStatus(msg);
				}
			}
		}
		return null;
	}

    private RefactoringStatus checkClashesWithExistingFields(){
        FieldDeclaration[] existingFields= getFieldDeclarations(getBodyDeclarationListOfDeclaringType());
        for (int i= 0; i < existingFields.length; i++) {
            FieldDeclaration declaration= existingFields[i];
			VariableDeclarationFragment[] fragments= (VariableDeclarationFragment[]) declaration.fragments().toArray(new VariableDeclarationFragment[declaration.fragments().size()]);
			for (int j= 0; j < fragments.length; j++) {
                VariableDeclarationFragment fragment= fragments[j];
                if (fFieldName.equals(fragment.getName().getIdentifier())){
                	//cannot conflict with more than 1 name
                	RefactoringStatusContext context= JavaStatusContext.create(fCu, fragment);
                	return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.PromoteTempToFieldRefactoring_Name_conflict_with_field, context); 
                }
            }
        }
        return null;
    }
    
    private ChildListPropertyDescriptor getBodyDeclarationListOfDeclaringType(){
    	ASTNode methodParent= getMethodDeclaration().getParent();
    	if (methodParent instanceof AbstractTypeDeclaration)
    		return ((AbstractTypeDeclaration) methodParent).getBodyDeclarationsProperty();
    	if (methodParent instanceof AnonymousClassDeclaration)
    		return AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY;
    	Assert.isTrue(false);
    	return null;	
    }
    
    private FieldDeclaration[] getFieldDeclarations(ChildListPropertyDescriptor descriptor) {
    	final List bodyDeclarations= (List) getMethodDeclaration().getParent().getStructuralProperty(descriptor);
    	List fields= new ArrayList(1);
    	for (Iterator iter= bodyDeclarations.iterator(); iter.hasNext();) {
	        Object each= iter.next();
	        if (each instanceof FieldDeclaration)
	        	fields.add(each);
        }
        return (FieldDeclaration[]) fields.toArray(new FieldDeclaration[fields.size()]);
    }

    /*
     * @see org.eclipse.wst.jsdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
     */
    public Change createChange(IProgressMonitor pm) throws CoreException {
    	pm.beginTask("", 1); //$NON-NLS-1$
    	try {
    		if (fFieldName.length() == 0) {
    			fFieldName= getInitialFieldName();
    		}
    		
    		ASTRewrite rewrite= ASTRewrite.create(fCompilationUnitNode.getAST());
    		if (fInitializeIn == INITIALIZE_IN_METHOD && tempHasInitializer())
    			addLocalDeclarationSplit(rewrite);
			else
				addLocalDeclarationRemoval(rewrite);
    		if (fInitializeIn == INITIALIZE_IN_CONSTRUCTOR)
    			addInitializersToConstructors(rewrite);
   			addTempRenames(rewrite);
    		addFieldDeclaration(rewrite);
            return createChange(rewrite);
    	} finally {
    		pm.done();
    	}
    }
    
    private void addTempRenames(ASTRewrite rewrite) {
    	boolean noNameChange= fFieldName.equals(fTempDeclarationNode.getName().getIdentifier());
		if (fLinkedProposalModel == null && noNameChange) {
			return; // no changes needed
		}
    	TempOccurrenceAnalyzer analyzer= new TempOccurrenceAnalyzer(fTempDeclarationNode, false);
		analyzer.perform();
    	SimpleName[] tempRefs= analyzer.getReferenceNodes(); // no javadocs (refactoring not for parameters)
    	
    	
		for (int j= 0; j < tempRefs.length; j++) {
			SimpleName occurence= tempRefs[j];
			if (noNameChange) {
				addLinkedName(rewrite, occurence, false);
			} else {
				SimpleName newName= getAST().newSimpleName(fFieldName);
				addLinkedName(rewrite, newName, false);
				rewrite.replace(occurence, newName, null);
			}
		}
    }
    
    private void addInitializersToConstructors(ASTRewrite rewrite) throws CoreException {
    	Assert.isTrue(! isDeclaredInAnonymousClass());
    	final AbstractTypeDeclaration declaration= (AbstractTypeDeclaration)getMethodDeclaration().getParent();
    	final FunctionDeclaration[] constructors= getAllConstructors(declaration);
    	if (constructors.length == 0) {
    		AST ast= rewrite.getAST();
    		FunctionDeclaration newConstructor= ast.newFunctionDeclaration();
    		newConstructor.setConstructor(true);
    		newConstructor.modifiers().addAll(ast.newModifiers(declaration.getModifiers() & ModifierRewrite.VISIBILITY_MODIFIERS));
    		newConstructor.setName(ast.newSimpleName(declaration.getName().getIdentifier()));
    		newConstructor.setJavadoc(getNewConstructorComment(rewrite));
    		newConstructor.setBody(ast.newBlock());
    		
    		addFieldInitializationToConstructor(rewrite, newConstructor);
    		
    		int insertionIndex= computeInsertIndexForNewConstructor(declaration);
			rewrite.getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newConstructor, insertionIndex, null);
    	} else {
    		for (int index= 0; index < constructors.length; index++) {
                if (shouldInsertTempInitialization(constructors[index]))
                    addFieldInitializationToConstructor(rewrite, constructors[index]);
            }
    	}
    }

	private String getEnclosingTypeName() {
		return getEnclosingType().getName().getIdentifier();
	}
	
	private AbstractTypeDeclaration getEnclosingType() {
		return (AbstractTypeDeclaration)ASTNodes.getParent(getTempDeclarationStatement(), AbstractTypeDeclaration.class);
	}

	private JSdoc getNewConstructorComment(ASTRewrite rewrite) throws CoreException {
		if (StubUtility.doAddComments(fCu.getJavaScriptProject())){
			String comment= CodeGeneration.getMethodComment(fCu, getEnclosingTypeName(), getEnclosingTypeName(), new String[0], new String[0], null, null, StubUtility.getLineDelimiterUsed(fCu));
			if (comment != null && comment.length() > 0) {
				return (JSdoc) rewrite.createStringPlaceholder(comment, ASTNode.JSDOC);
			}
		}
		return null;
	}

	private int computeInsertIndexForNewConstructor(AbstractTypeDeclaration declaration) {
    	List declarations= declaration.bodyDeclarations();
    	if (declarations.isEmpty())
	        return 0;
		int index= findFirstMethodIndex(declaration);
		if (index == -1)
			return declarations.size();
		else	
			return index;
    }
    
    private int findFirstMethodIndex(AbstractTypeDeclaration typeDeclaration) {
    	for (int i= 0, n= typeDeclaration.bodyDeclarations().size(); i < n; i++) {
            if (typeDeclaration.bodyDeclarations().get(i) instanceof FunctionDeclaration)
            	return i;
        }
        return -1;
    }
    
    private void addFieldInitializationToConstructor(ASTRewrite rewrite, FunctionDeclaration constructor) throws JavaScriptModelException {
    	if (constructor.getBody() == null)
	    	constructor.setBody(getAST().newBlock());
        ExpressionStatement newStatement= createExpressionStatementThatInitializesField(rewrite);
		rewrite.getListRewrite(constructor.getBody(), Block.STATEMENTS_PROPERTY).insertLast(newStatement, null);
    }
    
    private static boolean shouldInsertTempInitialization(FunctionDeclaration constructor){
    	Assert.isTrue(constructor.isConstructor());
        if (constructor.getBody() == null)
        	return false;
        List statements= constructor.getBody().statements();
        if (statements == null) 
        	return false;
        if (statements.size() > 0 && statements.get(0) instanceof ConstructorInvocation)
        	return false;
		return true;        
    }

    private static FunctionDeclaration[] getAllConstructors(AbstractTypeDeclaration typeDeclaration) {
		if (typeDeclaration instanceof TypeDeclaration) {
			FunctionDeclaration[] allMethods= ((TypeDeclaration) typeDeclaration).getMethods();
			List result= new ArrayList(Math.min(allMethods.length, 1));
			for (int i= 0; i < allMethods.length; i++) {
				FunctionDeclaration declaration= allMethods[i];
				if (declaration.isConstructor())
					result.add(declaration);
			}
			return (FunctionDeclaration[]) result.toArray(new FunctionDeclaration[result.size()]);
		}
		return new FunctionDeclaration[] {};
	}

    private Change createChange(ASTRewrite rewrite) throws CoreException {
		final Map arguments= new HashMap();
		String project= null;
		IJavaScriptProject javaProject= fCu.getJavaScriptProject();
		if (javaProject != null)
			project= javaProject.getElementName();
		final IVariableBinding binding= fTempDeclarationNode.resolveBinding();
		final String description= Messages.format(RefactoringCoreMessages.PromoteTempToFieldRefactoring_descriptor_description_short, binding.getName());
		final String header= Messages.format(RefactoringCoreMessages.PromoteTempToFieldRefactoring_descriptor_description, new String[] { BindingLabelProvider.getBindingLabel(binding, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), BindingLabelProvider.getBindingLabel(binding.getDeclaringMethod(), JavaScriptElementLabels.ALL_FULLY_QUALIFIED)});
		final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
		comment.addSetting(Messages.format(RefactoringCoreMessages.PromoteTempToFieldRefactoring_original_pattern, BindingLabelProvider.getBindingLabel(binding, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)));
		comment.addSetting(Messages.format(RefactoringCoreMessages.PromoteTempToFieldRefactoring_field_pattern, fFieldName));
		switch (fInitializeIn) {
			case INITIALIZE_IN_CONSTRUCTOR:
				comment.addSetting(RefactoringCoreMessages.PromoteTempToFieldRefactoring_initialize_constructor);
				break;
			case INITIALIZE_IN_FIELD:
				comment.addSetting(RefactoringCoreMessages.PromoteTempToFieldRefactoring_initialize_declaration);
				break;
			case INITIALIZE_IN_METHOD:
				comment.addSetting(RefactoringCoreMessages.PromoteTempToFieldRefactoring_initialize_method);
				break;
		}
		String visibility= JdtFlags.getVisibilityString(fVisibility);
		if ("".equals(visibility)) //$NON-NLS-1$
			visibility= RefactoringCoreMessages.PromoteTempToFieldRefactoring_default_visibility;
		comment.addSetting(Messages.format(RefactoringCoreMessages.PromoteTempToFieldRefactoring_visibility_pattern, visibility));
		if (fDeclareFinal && fDeclareStatic)
			comment.addSetting(RefactoringCoreMessages.PromoteTempToFieldRefactoring_declare_final_static);
		else if (fDeclareFinal)
			comment.addSetting(RefactoringCoreMessages.PromoteTempToFieldRefactoring_declare_final);
		else if (fDeclareStatic)
			comment.addSetting(RefactoringCoreMessages.PromoteTempToFieldRefactoring_declare_static);
		final JDTRefactoringDescriptor descriptor= new JDTRefactoringDescriptor(IJavaScriptRefactorings.CONVERT_LOCAL_VARIABLE, project, description, comment.asString(), arguments, RefactoringDescriptor.STRUCTURAL_CHANGE);
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fCu));
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_NAME, fFieldName);
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_SELECTION, Integer.valueOf(fSelectionStart).toString() + " " + Integer.valueOf(fSelectionLength).toString()); //$NON-NLS-1$
		arguments.put(ATTRIBUTE_STATIC, Boolean.valueOf(fDeclareStatic).toString());
		arguments.put(ATTRIBUTE_FINAL, Boolean.valueOf(fDeclareFinal).toString());
		arguments.put(ATTRIBUTE_VISIBILITY, Integer.valueOf(fVisibility).toString());
		arguments.put(ATTRIBUTE_INITIALIZE, Integer.valueOf(fInitializeIn).toString());
		final CompilationUnitChange result= new CompilationUnitChange(RefactoringCoreMessages.PromoteTempToFieldRefactoring_name, fCu);
		result.setDescriptor(new RefactoringChangeDescriptor(descriptor));
		TextEdit resultingEdits= rewrite.rewriteAST();
		TextChangeCompatibility.addTextEdit(result, RefactoringCoreMessages.PromoteTempToFieldRefactoring_editName, resultingEdits);
		return result;
	}

    private void addLocalDeclarationSplit(ASTRewrite rewrite) throws JavaScriptModelException {
    	VariableDeclarationStatement tempDeclarationStatement= getTempDeclarationStatement();
    	Block block= (Block)tempDeclarationStatement.getParent();//XXX can it be anything else?
    	int statementIndex= block.statements().indexOf(tempDeclarationStatement);
   	   	Assert.isTrue(statementIndex != -1);
    	List fragments= tempDeclarationStatement.fragments();
        int fragmentIndex= fragments.indexOf(fTempDeclarationNode);
    	Assert.isTrue(fragmentIndex != -1);

        for (int i1= fragmentIndex, n = fragments.size(); i1 < n; i1++) {
        	VariableDeclarationFragment fragment= (VariableDeclarationFragment)fragments.get(i1);
        	rewrite.remove(fragment, null);
        }
        if (fragmentIndex == 0)
           	rewrite.remove(tempDeclarationStatement, null);
        
        Assert.isTrue(tempHasInitializer());
        rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY).insertAt(createExpressionStatementThatInitializesField(rewrite), statementIndex + 1, null);
        
        if (fragmentIndex + 1 < fragments.size()){
            VariableDeclarationFragment firstFragmentAfter= (VariableDeclarationFragment)fragments.get(fragmentIndex + 1);
            VariableDeclarationFragment copyfirstFragmentAfter= (VariableDeclarationFragment)rewrite.createCopyTarget(firstFragmentAfter);
        	VariableDeclarationStatement statement= getAST().newVariableDeclarationStatement(copyfirstFragmentAfter);
         	Type type= (Type)rewrite.createCopyTarget(tempDeclarationStatement.getType());
        	statement.setType(type);
        	List modifiers= tempDeclarationStatement.modifiers();
        	if (modifiers.size() > 0) {
        		ListRewrite modifiersRewrite= rewrite.getListRewrite(tempDeclarationStatement, VariableDeclarationStatement.MODIFIERS2_PROPERTY);
        		ASTNode firstModifier= (ASTNode) modifiers.get(0);
				ASTNode lastModifier= (ASTNode) modifiers.get(modifiers.size() - 1);
				ASTNode modifiersCopy= modifiersRewrite.createCopyTarget(firstModifier, lastModifier);
	        	statement.modifiers().add(modifiersCopy);
        	}
        	for (int i= fragmentIndex + 2; i < fragments.size(); i++) {
        		VariableDeclarationFragment fragment= (VariableDeclarationFragment)fragments.get(i);
                VariableDeclarationFragment fragmentCopy= (VariableDeclarationFragment)rewrite.createCopyTarget(fragment);
                statement.fragments().add(fragmentCopy);
            }
            rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY).insertAt(statement, statementIndex + 2, null);
        }
    }
    
    private ExpressionStatement createExpressionStatementThatInitializesField(ASTRewrite rewrite) throws JavaScriptModelException{
		AST ast= getAST();
		Assignment assignment= ast.newAssignment();
		SimpleName fieldName= ast.newSimpleName(fFieldName);
		addLinkedName(rewrite, fieldName, true);
		assignment.setLeftHandSide(fieldName);
		assignment.setRightHandSide(getTempInitializerCopy(rewrite));
		return ast.newExpressionStatement(assignment);
    }

	private void addLinkedName(ASTRewrite rewrite, SimpleName fieldName, boolean isFirst) {
		if (fLinkedProposalModel != null) {
			fLinkedProposalModel.getPositionGroup(LINKED_NAME, true).addPosition(rewrite.track(fieldName), isFirst);
		}
	}
	
	private Expression getTempInitializerCopy(ASTRewrite rewrite) throws JavaScriptModelException {
		final Expression initializer= (Expression) rewrite.createCopyTarget(getTempInitializer());
		if (initializer instanceof ArrayInitializer && ASTNodes.getDimensions(fTempDeclarationNode) > 0) {
			ArrayCreation arrayCreation= rewrite.getAST().newArrayCreation();
			arrayCreation.setType((ArrayType) ASTNodeFactory.newType(rewrite.getAST(), fTempDeclarationNode));
			arrayCreation.setInitializer((ArrayInitializer) initializer);
			return arrayCreation;
		}
		return initializer;
	}

    private void addLocalDeclarationRemoval(ASTRewrite rewrite) {
		VariableDeclarationStatement tempDeclarationStatement= getTempDeclarationStatement();
    	List fragments= tempDeclarationStatement.fragments();

    	int fragmentIndex= fragments.indexOf(fTempDeclarationNode);
    	Assert.isTrue(fragmentIndex != -1);
        VariableDeclarationFragment fragment= (VariableDeclarationFragment)fragments.get(fragmentIndex);
        rewrite.remove(fragment, null);
        if (fragments.size() == 1)
			rewrite.remove(tempDeclarationStatement, null);
    }

    private void addFieldDeclaration(ASTRewrite rewrite) {
    	final ChildListPropertyDescriptor descriptor= getBodyDeclarationListOfDeclaringType();
    	FieldDeclaration[] fields= getFieldDeclarations(getBodyDeclarationListOfDeclaringType());
    	final ASTNode parent= getMethodDeclaration().getParent();
    	int insertIndex;
    	if (fields.length == 0)
    		insertIndex= 0;
    	else
    		insertIndex= ((List) parent.getStructuralProperty(descriptor)).indexOf(fields[fields.length - 1]) + 1;
    	
    	final FieldDeclaration declaration= createNewFieldDeclaration(rewrite);
		rewrite.getListRewrite(parent, descriptor).insertAt(declaration, insertIndex, null);
    }
    
    private FieldDeclaration createNewFieldDeclaration(ASTRewrite rewrite) {
		AST ast= getAST();
		VariableDeclarationFragment fragment= ast.newVariableDeclarationFragment();
		SimpleName variableName= ast.newSimpleName(fFieldName);
		fragment.setName(variableName);
		addLinkedName(rewrite, variableName, false);
		fragment.setExtraDimensions(fTempDeclarationNode.getExtraDimensions());
		if (fInitializeIn == INITIALIZE_IN_FIELD && tempHasInitializer()){
		    Expression initializer= (Expression)rewrite.createCopyTarget(getTempInitializer());
		    fragment.setInitializer(initializer);
		}
		FieldDeclaration fieldDeclaration= ast.newFieldDeclaration(fragment);
		
		VariableDeclarationStatement vds= getTempDeclarationStatement();
		Type type= (Type)rewrite.createCopyTarget(vds.getType());
		fieldDeclaration.setType(type);
		fieldDeclaration.modifiers().addAll(ASTNodeFactory.newModifiers(ast, getModifiers()));	
		return fieldDeclaration;
    }
    
    private int getModifiers() {
    	int flags= fVisibility;
    	if (fDeclareFinal)
    		flags |= Modifier.FINAL;
    	if (fDeclareStatic)	
    		flags |= Modifier.STATIC;
        return flags;
    }
    
    private AST getAST(){
    	return fTempDeclarationNode.getAST();
    }

    private static class LocalTypeAndVariableUsageAnalyzer extends HierarchicalASTVisitor{
    	private final List fLocalDefinitions= new ArrayList(0); // List of IBinding (Variable and Type)
    	private final List fLocalReferencesToEnclosing= new ArrayList(0); // List of ASTNodes
		private final List fMethodTypeVariables;
		private boolean fClassTypeVariablesUsed= false;
    	public LocalTypeAndVariableUsageAnalyzer(ITypeBinding[] methodTypeVariables) {
			fMethodTypeVariables= Arrays.asList(methodTypeVariables);
		}
		public List getUsageOfEnclosingNodes(){
			return fLocalReferencesToEnclosing;
		}
		public boolean getClassTypeVariablesUsed() {
			return fClassTypeVariablesUsed;
		}
		public boolean visit(SimpleName node) {
			ITypeBinding typeBinding= node.resolveTypeBinding();
			if (typeBinding != null && typeBinding.isLocal()) {
				if (node.isDeclaration()) {
					fLocalDefinitions.add(typeBinding);
				} else if (! fLocalDefinitions.contains(typeBinding)) {
					fLocalReferencesToEnclosing.add(node);
				}
			}
			IBinding binding= node.resolveBinding();
			if (binding != null && binding.getKind() == IBinding.VARIABLE && ! ((IVariableBinding)binding).isField()) {
				if (node.isDeclaration()) {
					fLocalDefinitions.add(binding);
				} else if (! fLocalDefinitions.contains(binding)) {
					fLocalReferencesToEnclosing.add(node);
				}
			}
			return super.visit(node);
		}
    }

	public RefactoringStatus initialize(final RefactoringArguments arguments) {
		fSelfInitializing= true;
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
					return createInputFatalStatus(element, IJavaScriptRefactorings.CONVERT_LOCAL_VARIABLE);
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
				fVisibility= flag;
			}
			final String initialize= extended.getAttribute(ATTRIBUTE_INITIALIZE);
			if (initialize != null && !"".equals(initialize)) {//$NON-NLS-1$
				int value= 0;
				try {
					value= Integer.parseInt(initialize);
				} catch (NumberFormatException exception) {
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_INITIALIZE));
				}
				fInitializeIn= value;
			}
			final String name= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_NAME);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				fFieldName= name;
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_NAME));
			final String declareStatic= extended.getAttribute(ATTRIBUTE_STATIC);
			if (declareStatic != null) {
				fDeclareStatic= Boolean.valueOf(declareStatic).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_STATIC));
			final String declareFinal= extended.getAttribute(ATTRIBUTE_FINAL);
			if (declareFinal != null) {
				fDeclareFinal= Boolean.valueOf(declareFinal).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_FINAL));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}


	public void setLinkedProposalModel(LinkedProposalModel model) {
		fLinkedProposalModel= model;
	}    
}
