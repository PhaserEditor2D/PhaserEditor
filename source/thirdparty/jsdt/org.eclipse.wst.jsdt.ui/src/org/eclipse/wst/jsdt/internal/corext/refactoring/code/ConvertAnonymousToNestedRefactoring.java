/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     N.Metchev@teamphone.com - contributed fixes for
 *     - convert anonymous to nested should sometimes declare class as static [refactoring] 
 *       (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=43360)
 *     - Convert anonymous to nested: should show error if field form outer anonymous type is references [refactoring]
 *       (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=48282)
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
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
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.Initializer;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.SuperFieldAccess;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.JavaScriptRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.wst.jsdt.internal.corext.fix.LinkedProposalPositionGroup;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.text.correction.ModifierCorrectionSubProcessor;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.BindingLabelProvider;
import org.eclipse.wst.jsdt.ui.CodeGeneration;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

public class ConvertAnonymousToNestedRefactoring extends ScriptableRefactoring {

	private static final String ATTRIBUTE_VISIBILITY= "visibility"; //$NON-NLS-1$
	private static final String ATTRIBUTE_FINAL= "final"; //$NON-NLS-1$
	private static final String ATTRIBUTE_STATIC= "static"; //$NON-NLS-1$
	
	private static final String KEY_TYPE_NAME= "type_name"; //$NON-NLS-1$
	private static final String KEY_PARAM_NAME_EXT= "param_name_ext"; //$NON-NLS-1$
	private static final String KEY_PARAM_NAME_CONST= "param_name_const"; //$NON-NLS-1$
	private static final String KEY_FIELD_NAME_EXT= "field_name_ext"; //$NON-NLS-1$
	
	public static class TypeVariableFinder extends ASTVisitor {

		private final List fFound= new ArrayList();
		
		public final boolean visit(final SimpleName node) {
			Assert.isNotNull(node);
			node.resolveTypeBinding();
			return true;
		}

		public final ITypeBinding[] getResult() {
			final ITypeBinding[] result= new ITypeBinding[fFound.size()];
			fFound.toArray(result);
			return result;
		}
	}

    private int fSelectionStart;
    private int fSelectionLength;
    private IJavaScriptUnit fCu;

    private int fVisibility; /* see Modifier */
    private boolean fDeclareFinal= true;
    private boolean fDeclareStatic;
    private String fClassName= ""; //$NON-NLS-1$

    private JavaScriptUnit fCompilationUnitNode;
    private AnonymousClassDeclaration fAnonymousInnerClassNode;
    private Set fClassNamesUsed;
	private boolean fSelfInitializing= false;
	
	private LinkedProposalModel fLinkedProposalModel;

	/**
	 * Creates a new convert anonymous to nested refactoring
	 * @param unit the compilation unit, or <code>null</code> if invoked by scripting
	 * @param selectionStart
	 * @param selectionLength
	 */
    public ConvertAnonymousToNestedRefactoring(IJavaScriptUnit unit, int selectionStart, int selectionLength) {
        Assert.isTrue(selectionStart >= 0);
        Assert.isTrue(selectionLength >= 0);
        Assert.isTrue(unit == null || unit.exists());
        fSelectionStart= selectionStart;
        fSelectionLength= selectionLength;
        fCu= unit;
        fAnonymousInnerClassNode= null;
        fCompilationUnitNode= null;
    }
    
    public ConvertAnonymousToNestedRefactoring(AnonymousClassDeclaration declaration) {
    	Assert.isTrue(declaration != null);
    	
    	ASTNode astRoot= declaration.getRoot();
    	Assert.isTrue(astRoot instanceof JavaScriptUnit);
    	fCompilationUnitNode= (JavaScriptUnit) astRoot;
    	
     	IJavaScriptElement javaElement= fCompilationUnitNode.getJavaElement();
        Assert.isTrue(javaElement instanceof IJavaScriptUnit);
        
        fCu= (IJavaScriptUnit) javaElement;
        fSelectionStart= declaration.getStartPosition();
        fSelectionLength= declaration.getLength();
    }
    
	public void setLinkedProposalModel(LinkedProposalModel linkedProposalModel) {
		fLinkedProposalModel= linkedProposalModel;
	}

    public int[] getAvailableVisibilities() {
        if (isLocalInnerType()) {
            return new int[] { Modifier.NONE };
        } else {
            return new int[] { Modifier.PUBLIC, Modifier.PROTECTED, Modifier.NONE, Modifier.PRIVATE };
        }
    }

    public boolean isLocalInnerType() {
        return ASTNodes.getParent(ASTNodes.getParent(fAnonymousInnerClassNode, AbstractTypeDeclaration.class), ASTNode.ANONYMOUS_CLASS_DECLARATION) != null;
    }

    public int getVisibility() {
        return fVisibility;
    }

    public void setVisibility(int visibility) {
        Assert.isTrue(visibility == Modifier.PRIVATE || visibility == Modifier.NONE || visibility == Modifier.PROTECTED || visibility == Modifier.PUBLIC);
        fVisibility= visibility;
    }

    public void setClassName(String className) {
        Assert.isNotNull(className);
        fClassName= className;
    }

    public boolean canEnableSettingFinal() {
        return true;
    }

    public boolean getDeclareFinal() {
        return fDeclareFinal;
    }
    
    public boolean getDeclareStatic() {
        return fDeclareStatic;
    }
    
    public void setDeclareFinal(boolean declareFinal) {
        fDeclareFinal= declareFinal;
    }

    public void setDeclareStatic(boolean declareStatic) {
        fDeclareStatic= declareStatic;
    }

    public String getName() {
        return RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_name; 
    }
    
    private boolean useThisForFieldAccess() {
    	return StubUtility.useThisForFieldAccess(fCu.getJavaScriptProject());
    }
    
    private boolean doAddComments() {
    	return StubUtility.doAddComments(fCu.getJavaScriptProject());
    }
    
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
        RefactoringStatus result= Checks.validateModifiesFiles(
        	ResourceUtil.getFiles(new IJavaScriptUnit[]{fCu}),
			getValidationContext());
		if (result.hasFatalError())
		    return result;

		initAST(pm);

		if (fAnonymousInnerClassNode == null)
		    return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_place_caret); 
		if (!fSelfInitializing)
			initializeDefaults();
		if (getSuperConstructorBinding() == null)
		    return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_compile_errors); 
		if (getSuperTypeBinding().isLocal())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_extends_local_class); 
		return new RefactoringStatus();
    }

    private void initializeDefaults() {
        fVisibility= isLocalInnerType() ? Modifier.NONE : Modifier.PRIVATE;
        fDeclareStatic = mustInnerClassBeStatic();
    }

    private void initAST(IProgressMonitor pm) {
    	if (fCompilationUnitNode == null) {
    		fCompilationUnitNode= RefactoringASTParser.parseWithASTProvider(fCu, true, pm);
    	}
    	if (fAnonymousInnerClassNode == null) {
    		fAnonymousInnerClassNode= getAnonymousInnerClass(NodeFinder.perform(fCompilationUnitNode, fSelectionStart, fSelectionLength));
    	}
		if (fAnonymousInnerClassNode != null) {
			final AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) ASTNodes.getParent(fAnonymousInnerClassNode, AbstractTypeDeclaration.class);
			if (declaration instanceof TypeDeclaration) {
				final AbstractTypeDeclaration[] nested= ((TypeDeclaration) declaration).getTypes();
				fClassNamesUsed= new HashSet(nested.length);
				for (int index= 0; index < nested.length; index++)
					fClassNamesUsed.add(nested[index].getName().getIdentifier());
			} else
				fClassNamesUsed= Collections.EMPTY_SET;
		}
	}

    private static AnonymousClassDeclaration getAnonymousInnerClass(ASTNode node) {
        if (node == null)
            return null;
        if (node instanceof AnonymousClassDeclaration)
            return (AnonymousClassDeclaration)node;
        if (node instanceof ClassInstanceCreation) {
            AnonymousClassDeclaration anon= ((ClassInstanceCreation)node).getAnonymousClassDeclaration();
            if (anon != null)
                return anon;
        }
        node= ASTNodes.getNormalizedNode(node);
        if (node.getLocationInParent() == ClassInstanceCreation.TYPE_PROPERTY) {
            AnonymousClassDeclaration anon= ((ClassInstanceCreation)node.getParent()).getAnonymousClassDeclaration();
            if (anon != null)
                return anon;
        }
        return (AnonymousClassDeclaration)ASTNodes.getParent(node, AnonymousClassDeclaration.class);
    }

    public RefactoringStatus validateInput() {
        RefactoringStatus result= Checks.checkTypeName(fClassName);
        if (result.hasFatalError())
            return result;

        if (fClassNamesUsed.contains(fClassName))
            return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_type_exists); 
        IFunctionBinding superConstructorBinding = getSuperConstructorBinding();
        if (superConstructorBinding == null)
            return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_compile_errors); 
        if (fClassName.equals(superConstructorBinding.getDeclaringClass().getName()))
            return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_another_name); 
        if (classNameHidesEnclosingType())
            return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_name_hides); 
        return result;
    }

    private boolean accessesAnonymousFields() {
        List anonymousInnerFieldTypes = getAllEnclosingAnonymousTypesField();
        List accessedField = getAllAccessedFields();
        final Iterator it = anonymousInnerFieldTypes.iterator();
        while(it.hasNext()) {
            final IVariableBinding variableBinding = (IVariableBinding) it.next();
            final Iterator it2 = accessedField.iterator();
            while (it2.hasNext()) {
                IVariableBinding variableBinding2 = (IVariableBinding) it2.next();
                if(Bindings.equals(variableBinding, variableBinding2)) {
                    return true;
                }   
            }
        }
        return false;
    }

	private List getAllAccessedFields() {
		final List accessedFields= new ArrayList();

		ASTVisitor visitor= new ASTVisitor() {

			public boolean visit(FieldAccess node) {
				final IVariableBinding binding= node.resolveFieldBinding();
				if (binding != null)
					accessedFields.add(binding);
				return super.visit(node);
			}

			public boolean visit(QualifiedName node) {
				final IBinding binding= node.resolveBinding();
				if (binding != null && binding instanceof IVariableBinding) {
					IVariableBinding variable= (IVariableBinding) binding;
					if (variable.isField())
						accessedFields.add(binding);
				}
				return super.visit(node);
			}

			public boolean visit(SimpleName node) {
				final IBinding binding= node.resolveBinding();
				if (binding != null && binding instanceof IVariableBinding) {
					IVariableBinding variable= (IVariableBinding) binding;
					if (variable.isField())
						accessedFields.add(binding);
				}
				return super.visit(node);
			}

			public boolean visit(SuperFieldAccess node) {
				final IVariableBinding binding= node.resolveFieldBinding();
				if (binding != null)
					accessedFields.add(binding);
				return super.visit(node);
			}
		};
		fAnonymousInnerClassNode.accept(visitor);

		return accessedFields;
	}

    private List getAllEnclosingAnonymousTypesField() {
		final List ans= new ArrayList();
		final AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) ASTNodes.getParent(fAnonymousInnerClassNode, AbstractTypeDeclaration.class);
		AnonymousClassDeclaration anonymous= (AnonymousClassDeclaration) ASTNodes.getParent(fAnonymousInnerClassNode, ASTNode.ANONYMOUS_CLASS_DECLARATION);
		while (anonymous != null) {
			if (ASTNodes.isParent(anonymous, declaration)) {
				ITypeBinding binding= anonymous.resolveBinding();
				if (binding != null) {
					ans.addAll(Arrays.asList(binding.getDeclaredFields()));
				}
			} else {
				break;
			}
			anonymous= (AnonymousClassDeclaration) ASTNodes.getParent(anonymous, ASTNode.ANONYMOUS_CLASS_DECLARATION);
		}
		return ans;
	}

    private boolean classNameHidesEnclosingType() {
        ITypeBinding type= ((AbstractTypeDeclaration) ASTNodes.getParent(fAnonymousInnerClassNode, AbstractTypeDeclaration.class)).resolveBinding();
        while (type != null) {
            if (fClassName.equals(type.getName()))
                return true;
            type= type.getDeclaringClass();
        }
        return false;
    }

    /*
     * @see org.eclipse.wst.jsdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
     */
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
        try {
            RefactoringStatus status= validateInput();
            if (accessesAnonymousFields())
                status.merge(RefactoringStatus.createErrorStatus(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_anonymous_field_access)); 
            return status;
        } finally {
            pm.done();
        }
    }
    
    public CompilationUnitChange createCompilationUnitChange(IProgressMonitor pm) throws CoreException {
		final CompilationUnitRewrite rewrite= new CompilationUnitRewrite(fCu, fCompilationUnitNode);
		addNestedClass(rewrite, null);
		modifyConstructorCall(rewrite, null);
		return rewrite.createChange(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_name, false, pm); 
    }
    

    /*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		final CompilationUnitChange result= createCompilationUnitChange(pm);
		result.setDescriptor(createRefactoringDescriptor());
		return result;
	}

	private RefactoringChangeDescriptor createRefactoringDescriptor() {
		final ITypeBinding binding= fAnonymousInnerClassNode.resolveBinding();
		final String[] labels= new String[] { BindingLabelProvider.getBindingLabel(binding, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), BindingLabelProvider.getBindingLabel(binding.getDeclaringMethod(), JavaScriptElementLabels.ALL_FULLY_QUALIFIED)};
		final Map arguments= new HashMap();
		final String projectName= fCu.getJavaScriptProject().getElementName();
		final int flags= RefactoringDescriptor.STRUCTURAL_CHANGE | JavaScriptRefactoringDescriptor.JAR_REFACTORING | JavaScriptRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
		final String description= RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_descriptor_description_short;
		final String header= Messages.format(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_descriptor_description, labels);
		final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(projectName, this, header);
		comment.addSetting(Messages.format(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_original_pattern, BindingLabelProvider.getBindingLabel(binding, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)));
		comment.addSetting(Messages.format(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_class_name_pattern, fClassName));
		String visibility= JdtFlags.getVisibilityString(fVisibility);
		if (visibility.length() == 0)
			visibility= RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_default_visibility;
		comment.addSetting(Messages.format(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_visibility_pattern, visibility));
		if (fDeclareFinal && fDeclareStatic)
			comment.addSetting(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_declare_final_static);
		else if (fDeclareFinal)
			comment.addSetting(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_declare_final);			
		else if (fDeclareStatic)
			comment.addSetting(RefactoringCoreMessages.ConvertAnonymousToNestedRefactoring_declare_static);			
		final JDTRefactoringDescriptor descriptor= new JDTRefactoringDescriptor(IJavaScriptRefactorings.CONVERT_ANONYMOUS, projectName, description, comment.asString(), arguments, flags);
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fCu));
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_NAME, fClassName);
		arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_SELECTION, Integer.valueOf(fSelectionStart).toString() + ' ' + Integer.valueOf(fSelectionLength).toString());
		arguments.put(ATTRIBUTE_FINAL, Boolean.valueOf(fDeclareFinal).toString());
		arguments.put(ATTRIBUTE_STATIC, Boolean.valueOf(fDeclareStatic).toString());
		arguments.put(ATTRIBUTE_VISIBILITY, Integer.valueOf(fVisibility).toString());
		return new RefactoringChangeDescriptor(descriptor);
	}

    private void modifyConstructorCall(CompilationUnitRewrite rewrite, ITypeBinding[] parameters) {
        rewrite.getASTRewrite().replace(fAnonymousInnerClassNode.getParent(), createNewClassInstanceCreation(rewrite, parameters), null);
    }

    private ASTNode createNewClassInstanceCreation(CompilationUnitRewrite rewrite, ITypeBinding[] parameters) {
		AST ast= fAnonymousInnerClassNode.getAST();
		ClassInstanceCreation newClassCreation= ast.newClassInstanceCreation();
		newClassCreation.setAnonymousClassDeclaration(null);
		Type type= null;
		SimpleName newNameNode= ast.newSimpleName(fClassName);
		if (parameters.length > 0) {
		} else
			type= ast.newSimpleType(newNameNode);
		newClassCreation.setType(type);
		copyArguments(rewrite, newClassCreation);
		addArgumentsForLocalsUsedInInnerClass(rewrite, newClassCreation);
		
		addLinkedPosition(KEY_TYPE_NAME, newNameNode, rewrite.getASTRewrite(), true);

		return newClassCreation;
	}

    private void addArgumentsForLocalsUsedInInnerClass(CompilationUnitRewrite rewrite, ClassInstanceCreation newClassCreation) {
        IVariableBinding[] usedLocals= getUsedLocalVariables();
        for (int i= 0; i < usedLocals.length; i++) {
            final AST ast= fAnonymousInnerClassNode.getAST();
			final IVariableBinding binding= usedLocals[i];
			Name name= ast.newSimpleName(binding.getName());
			newClassCreation.arguments().add(name);
        }
    }

    private void copyArguments(CompilationUnitRewrite rewrite, ClassInstanceCreation newClassCreation) {
        for (Iterator iter= ((ClassInstanceCreation) fAnonymousInnerClassNode.getParent()).arguments().iterator(); iter.hasNext(); )
            newClassCreation.arguments().add(rewrite.getASTRewrite().createCopyTarget((Expression)iter.next()));
    }

    private void addNestedClass(CompilationUnitRewrite rewrite, ITypeBinding[] typeParameters) throws CoreException {
        final AbstractTypeDeclaration declarations= (AbstractTypeDeclaration) ASTNodes.getParent(fAnonymousInnerClassNode, AbstractTypeDeclaration.class);
        int index= findIndexOfFistNestedClass(declarations.bodyDeclarations());
        if (index == -1)
            index= 0;
        rewrite.getASTRewrite().getListRewrite(declarations, declarations.getBodyDeclarationsProperty()).insertAt(createNewNestedClass(rewrite), index, null);
    }

    private static int findIndexOfFistNestedClass(List bodyDeclarations) {
        for (int i= 0, n= bodyDeclarations.size(); i < n; i++) {
            BodyDeclaration each= (BodyDeclaration)bodyDeclarations.get(i);
            if (isNestedType(each))
                return i;
        }
        return -1;
    }

    private static boolean isNestedType(BodyDeclaration each) {
        if (!(each instanceof AbstractTypeDeclaration))
            return false;
        return (each.getParent() instanceof AbstractTypeDeclaration);
    }

    private AbstractTypeDeclaration createNewNestedClass(CompilationUnitRewrite rewrite) throws CoreException {
		final AST ast= fAnonymousInnerClassNode.getAST();
		
		final TypeDeclaration newDeclaration= ast.newTypeDeclaration();
		newDeclaration.setJavadoc(null);
		newDeclaration.modifiers().addAll(ASTNodeFactory.newModifiers(ast, createModifiersForNestedClass()));
		newDeclaration.setName(ast.newSimpleName(fClassName));
		
		setSuperType(newDeclaration);
		
		IJavaScriptProject project= fCu.getJavaScriptProject();
		
		IVariableBinding[] bindings= getUsedLocalVariables();
		ArrayList fieldNames= new ArrayList();
		for (int i= 0; i < bindings.length; i++) {
			String name= StubUtility.removePrefixAndSuffixForVariable(project, bindings[i]);
			String[] fieldNameProposals= StubUtility.getVariableNameSuggestions(StubUtility.INSTANCE_FIELD, project, name, 0, fieldNames, true);
			fieldNames.add(fieldNameProposals[0]);
			
			
			if (fLinkedProposalModel != null) {
				LinkedProposalPositionGroup positionGroup= fLinkedProposalModel.getPositionGroup(KEY_FIELD_NAME_EXT + i, true);
				for (int k= 0; k < fieldNameProposals.length; k++) {
					positionGroup.addProposal(fieldNameProposals[k], null, fieldNameProposals.length - k);
				}
			}
		}
		String[] allFieldNames= (String[]) fieldNames.toArray(new String[fieldNames.size()]);
		
		List newBodyDeclarations= newDeclaration.bodyDeclarations();
		
		createFieldsForAccessedLocals(rewrite, bindings, allFieldNames, newBodyDeclarations);
		
		FunctionDeclaration newConstructorDecl= createNewConstructor(rewrite, bindings, allFieldNames);
		if (newConstructorDecl != null) {
			newBodyDeclarations.add(newConstructorDecl);
		}
		
		updateAndMoveBodyDeclarations(rewrite, bindings, allFieldNames, newBodyDeclarations, newConstructorDecl);
		
		if (doAddComments()) {
			String string= CodeGeneration.getTypeComment(rewrite.getCu(), fClassName, StubUtility.getLineDelimiterUsed(fCu));
			if (string != null) {
				JSdoc javadoc= (JSdoc) rewrite.getASTRewrite().createStringPlaceholder(string, ASTNode.JSDOC);
				newDeclaration.setJavadoc(javadoc);
			}
		}
		if (fLinkedProposalModel != null) {
			addLinkedPosition(KEY_TYPE_NAME, newDeclaration.getName(), rewrite.getASTRewrite(), false);
			ModifierCorrectionSubProcessor.installLinkedVisibilityProposals(fLinkedProposalModel, rewrite.getASTRewrite(), newDeclaration.modifiers(), false);
		}
		
		return newDeclaration;
	}

	private void updateAndMoveBodyDeclarations(CompilationUnitRewrite rewriter, IVariableBinding[] bindings, String[] fieldNames, List newBodyDeclarations, FunctionDeclaration newConstructorDecl) throws JavaScriptModelException {
		final ASTRewrite astRewrite= rewriter.getASTRewrite();
		final AST ast= astRewrite.getAST();
		
		final boolean useThisAccess= useThisForFieldAccess();
		
		int fieldInsertIndex= newConstructorDecl != null ? newBodyDeclarations.lastIndexOf(newConstructorDecl) : newBodyDeclarations.size();
		
		for (Iterator iterator= fAnonymousInnerClassNode.bodyDeclarations().iterator(); iterator.hasNext();) {
			BodyDeclaration body= (BodyDeclaration) iterator.next();
			
			for (int i= 0; i < bindings.length; i++) {
				SimpleName[] names= LinkedNodeFinder.findByBinding(body, bindings[i]);
				String fieldName= fieldNames[i];
				for (int k= 0; k < names.length; k++) {
					SimpleName newNode= ast.newSimpleName(fieldName);
					if (useThisAccess) {
						FieldAccess access= ast.newFieldAccess();
						access.setExpression(ast.newThisExpression());
						access.setName(newNode);
						astRewrite.replace(names[k], access, null);
					} else {
						astRewrite.replace(names[k], newNode, null);
					}
					addLinkedPosition(KEY_FIELD_NAME_EXT + i, newNode, astRewrite, false);
				}
			}
			if (body instanceof Initializer || body instanceof FieldDeclaration) {
				newBodyDeclarations.add(fieldInsertIndex++, astRewrite.createMoveTarget(body));
			} else {
				newBodyDeclarations.add(astRewrite.createMoveTarget(body));
			}
		}
		
		if (newConstructorDecl != null) {
			// move initialization of existing fields to constructor if an outer is referenced 
			List bodyStatements= newConstructorDecl.getBody().statements();

			List fieldsToInitializeInConstructor= getFieldsToInitializeInConstructor();
			for (Iterator iter= fieldsToInitializeInConstructor.iterator(); iter.hasNext();) {
				VariableDeclarationFragment fragment= (VariableDeclarationFragment) iter.next();
				Expression initializer= fragment.getInitializer();
				Expression replacement= (Expression) astRewrite.get(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY);
				if (replacement == initializer) {
					replacement= (Expression) astRewrite.createMoveTarget(initializer);
				}
				astRewrite.remove(initializer, null);
				SimpleName fieldNameNode= ast.newSimpleName(fragment.getName().getIdentifier());
				bodyStatements.add(newFieldAssignment(ast, fieldNameNode, replacement, useThisAccess));
			}
		}
	}

    private void createFieldsForAccessedLocals(CompilationUnitRewrite rewrite, IVariableBinding[] varBindings, String[] fieldNames, List newBodyDeclarations) {
		final ImportRewrite importRewrite= rewrite.getImportRewrite();
		final ASTRewrite astRewrite= rewrite.getASTRewrite();
		final AST ast= astRewrite.getAST();
				
		for (int i= 0; i < varBindings.length; i++) {
			VariableDeclarationFragment fragment= ast.newVariableDeclarationFragment();
			fragment.setExtraDimensions(0);
			fragment.setInitializer(null);
			fragment.setName(ast.newSimpleName(fieldNames[i]));
			FieldDeclaration field= ast.newFieldDeclaration(fragment);
			ITypeBinding varType= varBindings[i].getType();
			field.setType(importRewrite.addImport(varType, ast));
			field.modifiers().addAll(ASTNodeFactory.newModifiers(ast, Modifier.PRIVATE | Modifier.FINAL));
			if (doAddComments()) {
				try {
					String string= CodeGeneration.getFieldComment(rewrite.getCu(), varType.getName(), fieldNames[i], StubUtility.getLineDelimiterUsed(fCu));
					if (string != null) {
						JSdoc javadoc= (JSdoc) astRewrite.createStringPlaceholder(string, ASTNode.JSDOC);
						field.setJavadoc(javadoc);
					}
				} catch (CoreException exception) {
					JavaScriptPlugin.log(exception);
				}
			}
			
			newBodyDeclarations.add(field);
			
			addLinkedPosition(KEY_FIELD_NAME_EXT + i, fragment.getName(), astRewrite, false);
		}
	}
    
    private void addLinkedPosition(String key, ASTNode nodeToTrack, ASTRewrite rewrite, boolean isFirst) {
    	if (fLinkedProposalModel != null) {
    		fLinkedProposalModel.getPositionGroup(key, true).addPosition(rewrite.track(nodeToTrack), isFirst);
    	}
    }
    

    private IVariableBinding[] getUsedLocalVariables() {
        final Set result= new HashSet(0);
        collectRefrencedVariables(fAnonymousInnerClassNode, result);
        ArrayList usedLocals= new ArrayList();
        for (Iterator iterator= result.iterator(); iterator.hasNext();) {
        	IVariableBinding next= (IVariableBinding) iterator.next();
			if (isBindingToTemp(next)) {
        		usedLocals.add(next);
        	}
		}
        return (IVariableBinding[])usedLocals.toArray(new IVariableBinding[usedLocals.size()]);
    }

    private void collectRefrencedVariables(ASTNode root, final Set result) {
    	root.accept(new ASTVisitor() {
            public boolean visit(SimpleName node) {
                IBinding binding= node.resolveBinding();
                if (binding instanceof IVariableBinding)
                	result.add(binding);
                return true;
            }
        });
    }

    private boolean isBindingToTemp(IVariableBinding variable) {
		if (variable.isField())
			return false;
		if (!Modifier.isFinal(variable.getModifiers()))
			return false;
		ASTNode declaringNode= fCompilationUnitNode.findDeclaringNode(variable);
		if (declaringNode == null)
			return false;
		if (ASTNodes.isParent(declaringNode, fAnonymousInnerClassNode))
			return false;
		return true;
	}

    private FunctionDeclaration createNewConstructor(CompilationUnitRewrite rewrite, IVariableBinding[] bindings, String[] fieldNames) throws JavaScriptModelException {
    	ClassInstanceCreation instanceCreation= (ClassInstanceCreation) fAnonymousInnerClassNode.getParent();
    	
    	if (instanceCreation.arguments().isEmpty() && bindings.length == 0)
			return null;

    	IJavaScriptProject project= fCu.getJavaScriptProject();
        AST ast= rewrite.getAST();
        ImportRewrite importRewrite= rewrite.getImportRewrite();
        ASTRewrite astRewrite= rewrite.getASTRewrite();
		
		FunctionDeclaration newConstructor= ast.newFunctionDeclaration();
		newConstructor.setConstructor(true);
		newConstructor.setExtraDimensions(0);
		newConstructor.setJavadoc(null);
		newConstructor.modifiers().addAll(ASTNodeFactory.newModifiers(ast, fVisibility));
		newConstructor.setName(ast.newSimpleName(fClassName));
		addLinkedPosition(KEY_TYPE_NAME, newConstructor.getName(), astRewrite, false);
		
		newConstructor.setBody(ast.newBlock());
		
		List newStatements= newConstructor.getBody().statements();
		
        List newParameters= newConstructor.parameters();
        List newParameterNames= new ArrayList();
		
        // add parameters for elements passed with the instance creation
        if (!instanceCreation.arguments().isEmpty()) {
        	IFunctionBinding constructorBinding= getSuperConstructorBinding();
        	if (constructorBinding != null) {
        		SuperConstructorInvocation superConstructorInvocation= ast.newSuperConstructorInvocation();
    			ITypeBinding[] parameterTypes= constructorBinding.getParameterTypes();
    	        String[][] parameterNames= StubUtility.suggestArgumentNamesWithProposals(project, constructorBinding);
    	        for (int i= 0; i < parameterNames.length; i++) {
    	        	String[] nameProposals= parameterNames[i];
    	        	String paramName= nameProposals[0];
    	        	
    	    		SingleVariableDeclaration param= newParameterDeclaration(ast, importRewrite, paramName, parameterTypes[i]);
    	    		newParameters.add(param);
    	    		newParameterNames.add(paramName);
    	    		
    	    		SimpleName newSIArgument= ast.newSimpleName(paramName);
					superConstructorInvocation.arguments().add(newSIArgument);
    	    		
					if (fLinkedProposalModel != null) {
						LinkedProposalPositionGroup positionGroup= fLinkedProposalModel.getPositionGroup(KEY_PARAM_NAME_CONST+ String.valueOf(i), true);
						positionGroup.addPosition(astRewrite.track(param.getName()), false);
						positionGroup.addPosition(astRewrite.track(newSIArgument), false);
	        	        for (int k= 0; k < nameProposals.length; k++) {
	        	        	positionGroup.addProposal(nameProposals[k], null, nameProposals.length - k);
						}
					}
    	        }
    	        newStatements.add(superConstructorInvocation);
        	}
        }
        // add parameters for all outer variables used
        boolean useThisAccess= useThisForFieldAccess();
        for (int i= 0; i < bindings.length; i++) {
        	String baseName= StubUtility.removePrefixAndSuffixForVariable(project, bindings[i]);
        	String[] paramNameProposals= StubUtility.getVariableNameSuggestions(StubUtility.PARAMETER, project, baseName, 0, newParameterNames, true);
        	String paramName= paramNameProposals[0];
        	
        	SingleVariableDeclaration param= newParameterDeclaration(ast, importRewrite, paramName, bindings[i].getType());
    		newParameters.add(param);
    		newParameterNames.add(paramName);
    		
    		String fieldName= fieldNames[i];
    		SimpleName fieldNameNode= ast.newSimpleName(fieldName);
    		SimpleName paramNameNode= ast.newSimpleName(paramName);
			newStatements.add(newFieldAssignment(ast, fieldNameNode, paramNameNode, useThisAccess || newParameterNames.contains(fieldName)));
			
			if (fLinkedProposalModel != null) {
				LinkedProposalPositionGroup positionGroup= fLinkedProposalModel.getPositionGroup(KEY_PARAM_NAME_EXT+ String.valueOf(i), true);
				positionGroup.addPosition(astRewrite.track(param.getName()), false);
				positionGroup.addPosition(astRewrite.track(paramNameNode), false);
    	        for (int k= 0; k < paramNameProposals.length; k++) {
    	        	positionGroup.addProposal(paramNameProposals[k], null, paramNameProposals.length - k);
				}
    	        
    	        fLinkedProposalModel.getPositionGroup(KEY_FIELD_NAME_EXT + i, true).addPosition(astRewrite.track(fieldNameNode), false);
			}
		}
		
		if (doAddComments()) {
			try {
				String[] allParamNames= (String[]) newParameterNames.toArray(new String[newParameterNames.size()]);
				String string= CodeGeneration.getMethodComment(fCu, fClassName, fClassName, allParamNames, new String[0], null, null, StubUtility.getLineDelimiterUsed(fCu));
				if (string != null) {
					JSdoc javadoc= (JSdoc) astRewrite.createStringPlaceholder(string, ASTNode.JSDOC);
					newConstructor.setJavadoc(javadoc);
				}
			} catch (CoreException exception) {
				JavaScriptPlugin.log(exception);
			}
		}
		return newConstructor;
	}
    
    private Statement newFieldAssignment(AST ast, SimpleName fieldNameNode, Expression initializer, boolean useThisAccess) {
		Assignment assignment= ast.newAssignment();
		if (useThisAccess) {
			FieldAccess access= ast.newFieldAccess();
			access.setExpression(ast.newThisExpression());
			access.setName(fieldNameNode);
			assignment.setLeftHandSide(access);
		} else {
			assignment.setLeftHandSide(fieldNameNode);
		}
		assignment.setOperator(Assignment.Operator.ASSIGN);
		assignment.setRightHandSide(initializer);
		
		return ast.newExpressionStatement(assignment);
     }
    

    // live List of VariableDeclarationFragments
    private List getFieldsToInitializeInConstructor() {
        List result= new ArrayList(0);
        for (Iterator iter= fAnonymousInnerClassNode.bodyDeclarations().iterator(); iter.hasNext(); ) {
            Object element= iter.next();
            if (element instanceof FieldDeclaration) {
            	List fragments= ((FieldDeclaration) element).fragments();
                for (Iterator fragmentIter= fragments.iterator(); fragmentIter.hasNext(); ) {
                    VariableDeclarationFragment fragment= (VariableDeclarationFragment) fragmentIter.next();
                    if (isToBeInitializerInConstructor(fragment, result))
                        result.add(fragment);
                }
            }
        }
        return result;
    }

    private boolean isToBeInitializerInConstructor(VariableDeclarationFragment fragment, List fieldsToInitialize) {
    	return fragment.getInitializer() != null && areLocalsUsedIn(fragment.getInitializer(), fieldsToInitialize);
    }

    private boolean areLocalsUsedIn(Expression fieldInitializer, List fieldsToInitialize) {
        Set localsUsed= new HashSet(0);
        collectRefrencedVariables(fieldInitializer, localsUsed);
        
        ITypeBinding anonType= fAnonymousInnerClassNode.resolveBinding();
        
        for (Iterator iterator= localsUsed.iterator(); iterator.hasNext();) {
			IVariableBinding curr= (IVariableBinding) iterator.next();
			if (isBindingToTemp(curr)) { // reference a local from outside
				return true;
			} else if (curr.isField() && (curr.getDeclaringClass() == anonType) && fieldsToInitialize.contains(fCompilationUnitNode.findDeclaringNode(curr))) {
				return true; // references a field that references a local from outside
			}
		}
        return false;
    }

    private IFunctionBinding getSuperConstructorBinding() {
        //workaround for missing java core functionality - finding a
        // super constructor for an anonymous class creation
        IFunctionBinding anonConstr= ((ClassInstanceCreation) fAnonymousInnerClassNode.getParent()).resolveConstructorBinding();
        if (anonConstr == null)
            return null;
        ITypeBinding superClass= anonConstr.getDeclaringClass().getSuperclass();
        IFunctionBinding[] superMethods= superClass.getDeclaredMethods();
        for (int i= 0; i < superMethods.length; i++) {
            IFunctionBinding superMethod= superMethods[i];
            if (superMethod.isConstructor() && parameterTypesMatch(superMethod, anonConstr))
                return superMethod;
        }
        Assert.isTrue(false);//there's no way - it must be there
        return null;
    }

    private static boolean parameterTypesMatch(IFunctionBinding m1, IFunctionBinding m2) {
        ITypeBinding[] m1Params= m1.getParameterTypes();
        ITypeBinding[] m2Params= m2.getParameterTypes();
        if (m1Params.length != m2Params.length)
            return false;
        for (int i= 0; i < m2Params.length; i++) {
            if (!m1Params[i].equals(m2Params[i]))
                return false;
        }
        return true;
    }

    private SingleVariableDeclaration newParameterDeclaration(AST ast, ImportRewrite importRewrite, String paramName, ITypeBinding paramType) {
    	SingleVariableDeclaration param= ast.newSingleVariableDeclaration();	
		param.setExtraDimensions(0);
		param.setInitializer(null);
		param.setType(importRewrite.addImport(paramType, ast));
		param.setName(ast.newSimpleName(paramName));
		return param;
    }

    private void setSuperType(TypeDeclaration declaration) throws JavaScriptModelException {
        ClassInstanceCreation classInstanceCreation= (ClassInstanceCreation) fAnonymousInnerClassNode.getParent();
		ITypeBinding binding= classInstanceCreation.resolveTypeBinding();
        if (binding == null)
            return;
		Type newType= (Type) ASTNode.copySubtree(fAnonymousInnerClassNode.getAST(), classInstanceCreation.getType());
		if (binding.getSuperclass().getQualifiedName().equals("java.lang.Object")) { //$NON-NLS-1$
                return; 
        } else {
            declaration.setSuperclassType(newType); 
        }
    }

    private ITypeBinding getSuperTypeBinding() {
    	ITypeBinding types= fAnonymousInnerClassNode.resolveBinding();
    	return types.getSuperclass();
    }

    private int createModifiersForNestedClass() {
        int flags= fVisibility;
        if (fDeclareFinal)
            flags|= Modifier.FINAL;
        if (mustInnerClassBeStatic() || fDeclareStatic)
            flags|= Modifier.STATIC;
        return flags;
    }

    public boolean mustInnerClassBeStatic() {
        ITypeBinding typeBinding = ((AbstractTypeDeclaration) ASTNodes.getParent(fAnonymousInnerClassNode, AbstractTypeDeclaration.class)).resolveBinding();
        ASTNode current = fAnonymousInnerClassNode.getParent();
        boolean ans = false;
        while(current != null) {
            switch(current.getNodeType()) {
                case ASTNode.ANONYMOUS_CLASS_DECLARATION:
                {
                    AnonymousClassDeclaration enclosingAnonymousClassDeclaration= (AnonymousClassDeclaration)current;
                    ITypeBinding binding= enclosingAnonymousClassDeclaration.resolveBinding();
                    if (binding != null && Bindings.isSuperType(typeBinding, binding.getSuperclass())) {
                        return false;
                    }
                    break;
                }
                case ASTNode.FIELD_DECLARATION:
                {
                    FieldDeclaration enclosingFieldDeclaration= (FieldDeclaration)current;
                    if (Modifier.isStatic(enclosingFieldDeclaration.getModifiers())) {
                        ans = true;
                    }
                    break;
                }
                case ASTNode.FUNCTION_DECLARATION:
                {
                    FunctionDeclaration enclosingMethodDeclaration = (FunctionDeclaration)current;
                    if (Modifier.isStatic(enclosingMethodDeclaration.getModifiers())) {
                        ans = true;
                    }
                    break;
                }
                case ASTNode.TYPE_DECLARATION:
                {
                    return ans;
                }
            }
            current = current.getParent();
        }
        return ans;
    }

    public RefactoringStatus initialize(final RefactoringArguments arguments) {
		fSelfInitializing= true;
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			final String handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists() || element.getElementType() != IJavaScriptElement.JAVASCRIPT_UNIT)
					return createInputFatalStatus(element, IJavaScriptRefactorings.CONVERT_ANONYMOUS);
				else {
					fCu= (IJavaScriptUnit) element;
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
			final String name= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_NAME);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				fClassName= name;
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_NAME));
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
			final String declareStatic= extended.getAttribute(ATTRIBUTE_STATIC);
			if (declareStatic != null) {
				fDeclareStatic= Boolean.valueOf(declareStatic).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_STATIC));
			final String declareFinal= extended.getAttribute(ATTRIBUTE_FINAL);
			if (declareFinal != null) {
				fDeclareFinal= Boolean.valueOf(declareStatic).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_FINAL));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}
}
