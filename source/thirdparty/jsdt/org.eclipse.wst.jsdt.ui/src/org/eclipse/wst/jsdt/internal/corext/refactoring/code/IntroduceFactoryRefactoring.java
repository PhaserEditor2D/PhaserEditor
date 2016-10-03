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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.JavaScriptRefactoringDescriptor;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchMatch;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.wst.jsdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.wst.jsdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.wst.jsdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.ASTCreator;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.SearchUtils;
import org.eclipse.wst.jsdt.internal.ui.JavaUIStatus;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.BindingLabelProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * Refactoring class that permits the substitution of a factory method
 * for direct calls to a given constructor.
 * @author rfuhrer
 */
public class IntroduceFactoryRefactoring extends ScriptableRefactoring {

	private static final String ATTRIBUTE_PROTECT= "protect"; //$NON-NLS-1$

	/**
	 * The handle for the compilation unit holding the selection that was
	 * passed into this refactoring.
	 */
	private IJavaScriptUnit fCUHandle;

	/**
	 * The AST for the compilation unit holding the selection that was
	 * passed into this refactoring.
	 */
	private JavaScriptUnit fCU;

	/**
	 * Handle for compilation unit in which the factory method/class/interface will be
	 * generated.
	 */
	private IJavaScriptUnit fFactoryUnitHandle;

	/**
	 * The start of the original textual selection in effect when this refactoring
	 * was initiated. If the refactoring was initiated from a structured selection
	 * (e.g. from the outline view), then this refers to the textual selection that
	 * corresponds to the structured selection item.
	 */
	private int fSelectionStart;

	/**
	 * The length of the original textual selection in effect when this refactoring
	 * was initiated. If the refactoring was initiated from a structured selection
	 * (e.g. from the outline view), then this refers to the textual selection that
	 * corresponds to the structured selection item.
	 */
	private int fSelectionLength;

	/**
	 * The AST node corresponding to the user's textual selection.
	 */
	private ASTNode fSelectedNode;

	/**
	 * The method binding for the selected constructor.
	 */
	private IFunctionBinding fCtorBinding;
	
	/**
	 * <code>TypeDeclaration</code> for class containing the constructor to be
	 * encapsulated.
	 */
	private AbstractTypeDeclaration fCtorOwningClass;

	/**
	 * The name to be given to the generated factory method.
	 */
	private String fNewMethodName= null;

	/**
	 * An array of <code>SearchResultGroup</code>'s of all call sites
	 * that refer to the constructor signature in question.
	 */
	private SearchResultGroup[] fAllCallsTo;

	/**
	 * The class that will own the factory method/class/interface.
	 */
	private AbstractTypeDeclaration fFactoryOwningClass;

	/**
	 * The newly-generated factory method.
	 */
	private FunctionDeclaration fFactoryMethod= null;

	/**
	 * An array containing the names of the constructor's formal arguments,
	 * if available, otherwise "arg1" ... "argN".
	 */
	private String[] fFormalArgNames= null;

	/**
	 * An array of <code>ITypeBinding</code>'s that describes the types of
	 * the constructor arguments, in order.
	 */
	private ITypeBinding[] fArgTypes;

	/**
	 * True iff the given constructor has a varargs signature.
	 */
	private boolean fCtorIsVarArgs;

	/**
	 * If true, change the visibility of the constructor to protected to better
	 * encapsulate it.
	 */
	private boolean fProtectConstructor= true;

	/**
	 * An <code>ImportRewrite</code> that manages imports needed to satisfy
	 * newly-introduced type references in the <code>IJavaScriptUnit</code>
	 * currently being rewritten during <code>createChange()</code>.
	 */
	private ImportRewrite fImportRewriter;

	/**
	 * True iff there are call sites for the constructor to be encapsulated
	 * located in binary classes.
	 */
	private boolean fCallSitesInBinaryUnits;

	/**
	 * <code>JavaScriptUnit</code> in which the factory is to be created.
	 */
	private JavaScriptUnit fFactoryCU;

	/**
	 * The fully qualified name of the factory class. This is only used
	 * if invoked from a refactoring script.
	 */
	private String fFactoryClassName;
	
	private int fConstructorVisibility= Modifier.PRIVATE;

	/**
	 * Creates a new <code>IntroduceFactoryRefactoring</code> with the given selection
	 * on the given compilation unit.
	 * @param cu the <code>IJavaScriptUnit</code> in which the user selection was made, or <code>null</code> if invoked from scripting
	 * @param selectionStart the start of the textual selection in <code>cu</code>
	 * @param selectionLength the length of the textual selection in <code>cu</code>
	 */
	public IntroduceFactoryRefactoring(IJavaScriptUnit cu, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart  >= 0);
		Assert.isTrue(selectionLength >= 0);
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCUHandle= cu;
		if (cu != null)
			initialize();
	}

	private void initialize() {
		fCU= ASTCreator.createAST(fCUHandle, null);
	}

	/**
	 * Finds and returns the <code>ASTNode</code> for the given source text
	 * selection, if it is an entire constructor call or the class name portion
	 * of a constructor call or constructor declaration, or null otherwise.
	 * @param unit The compilation unit in which the selection was made 
	 * @param offset The textual offset of the start of the selection
	 * @param length The length of the selection in characters
	 * @return ClassInstanceCreation or FunctionDeclaration
	 */
	private ASTNode getTargetNode(IJavaScriptUnit unit, int offset, int length) {
		ASTNode node= ASTNodes.getNormalizedNode(NodeFinder.perform(fCU, offset, length));
		if (node.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION)
			return node;
		if (node.getNodeType() == ASTNode.FUNCTION_DECLARATION && ((FunctionDeclaration)node).isConstructor())
			return node;
		// we have some sub node. Make sure its the right child of the parent
		StructuralPropertyDescriptor location= node.getLocationInParent();
		ASTNode parent= node.getParent();
		if (location == ClassInstanceCreation.TYPE_PROPERTY) {
			return parent;
		} else if (location == FunctionDeclaration.NAME_PROPERTY && ((FunctionDeclaration)parent).isConstructor()) {
			return parent;
		}
		return null;
	}

	/**
	 * Determines what kind of AST node was selected, and returns an error status
	 * if the kind of node is inappropriate for this refactoring.
	 * @param pm
	 * @return a RefactoringStatus indicating whether the selection is valid
	 * @throws JavaScriptModelException
	 */
	private RefactoringStatus checkSelection(IProgressMonitor pm) throws JavaScriptModelException {
		try {
			pm.beginTask(RefactoringCoreMessages.IntroduceFactory_examiningSelection, 2); 
	
			fSelectedNode= getTargetNode(fCUHandle, fSelectionStart, fSelectionLength);
	
			if (fSelectedNode == null)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceFactory_notAConstructorInvocation); 
	
			// getTargetNode() must return either a ClassInstanceCreation or a
			// constructor FunctionDeclaration; nothing else.
			if (fSelectedNode instanceof ClassInstanceCreation) {
				ClassInstanceCreation classInstanceCreation= (ClassInstanceCreation)fSelectedNode;
				fCtorBinding= classInstanceCreation.resolveConstructorBinding();
			} else if (fSelectedNode instanceof FunctionDeclaration) {
				FunctionDeclaration methodDeclaration= (FunctionDeclaration)fSelectedNode;
				fCtorBinding= methodDeclaration.resolveBinding();
			}
	
			if (fCtorBinding == null)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceFactory_unableToResolveConstructorBinding); 

			// If this constructor is of a generic type, get the generic version,
			// not some instantiation thereof.
			fCtorBinding= fCtorBinding.getMethodDeclaration();

			if (fNewMethodName == null)
				fNewMethodName= "create" + fCtorBinding.getName();//$NON-NLS-1$
	
			pm.worked(1);
	
			// We don't handle constructors of nested types at the moment
			if (fCtorBinding.getDeclaringClass().isNested())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceFactory_unsupportedNestedTypes); 
	
			ITypeBinding	ctorType= fCtorBinding.getDeclaringClass();
			IType			ctorOwningType= (IType) ctorType.getJavaElement();
	
			if (ctorOwningType.isBinary())
				// Can't modify binary CU; don't know what CU to put factory method
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceFactory_constructorInBinaryClass); 
	
			// Put the generated factory method inside the type that owns the constructor
			fFactoryUnitHandle= ctorOwningType.getJavaScriptUnit();
			fFactoryCU= getASTFor(fFactoryUnitHandle);
	
			Name	ctorOwnerName= (Name) NodeFinder.perform(fFactoryCU, ctorOwningType.getNameRange());
	
			fCtorOwningClass= (AbstractTypeDeclaration) ASTNodes.getParent(ctorOwnerName, AbstractTypeDeclaration.class);
			fFactoryOwningClass= fCtorOwningClass;
	
			pm.worked(1);
	
			return new RefactoringStatus();
		} finally {
			pm.done();
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.IntroduceFactory_checkingActivation, 1); 
	
			if (!fCUHandle.isStructureKnown())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceFactory_syntaxError); 
	
			return checkSelection(new SubProgressMonitor(pm, 1));
		} finally {
			pm.done();
		}
	}

	/**
	 * Returns the set of compilation units that will be affected by this
	 * particular invocation of this refactoring. This in general includes
	 * the class containing the constructor in question, as well as all
	 * call sites to the constructor.
	 * @return IJavaScriptUnit[]
	 */
	private IJavaScriptUnit[] collectAffectedUnits(SearchResultGroup[] searchHits) {
		Collection	result= new ArrayList();
		boolean hitInFactoryClass= false;

		for(int i=0; i < searchHits.length; i++) {
			SearchResultGroup	rg=  searchHits[i];
			IJavaScriptUnit	icu= rg.getCompilationUnit();

			result.add(icu);
			if (icu.equals(fFactoryUnitHandle))
				hitInFactoryClass= true;
		}
		if (!hitInFactoryClass)
			result.add(fFactoryUnitHandle);
		return (IJavaScriptUnit[]) result.toArray(new IJavaScriptUnit[result.size()]);
	}

	/**
	 * Returns a <code>SearchPattern</code> that finds all calls to the constructor
	 * identified by the argument <code>methodBinding</code>.
	 */
	private SearchPattern createSearchPattern(IFunction ctor, IFunctionBinding methodBinding) {
		Assert.isNotNull(methodBinding,
				RefactoringCoreMessages.IntroduceFactory_noBindingForSelectedConstructor); 
		
		if (ctor != null)
			return SearchPattern.createPattern(ctor, IJavaScriptSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
		else { // perhaps a synthetic method? (but apparently not always... hmmm...)
			// Can't find an IFunction for this method, so build a string pattern instead
			StringBuffer	buf= new StringBuffer();

			buf.append(methodBinding.getDeclaringClass().getQualifiedName())
			   .append("(");//$NON-NLS-1$
			for(int i=0; i < fArgTypes.length; i++) {
				if (i != 0)
					buf.append(","); //$NON-NLS-1$
				buf.append(fArgTypes[i].getQualifiedName());
			}
			buf.append(")"); //$NON-NLS-1$
			return SearchPattern.createPattern(buf.toString(), IJavaScriptSearchConstants.CONSTRUCTOR,
					IJavaScriptSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
		}
	}
	
	private IJavaScriptSearchScope createSearchScope(IFunction ctor, IFunctionBinding binding) throws JavaScriptModelException {
		if (ctor != null) {
			return RefactoringScopeFactory.create(ctor);
		} else {
			ITypeBinding type= Bindings.getTopLevelType(binding.getDeclaringClass());
			return RefactoringScopeFactory.create(type.getJavaElement());
		}
	}

	/**
	 * Returns an array of <code>SearchResultGroup</code>'s like the argument,
	 * but omitting those groups that have no corresponding compilation unit
	 * (i.e. are binary and therefore can't be modified).
	 */
	private SearchResultGroup[] excludeBinaryUnits(SearchResultGroup[] groups) {
		Collection/*<SearchResultGroup>*/	result= new ArrayList();

		for (int i = 0; i < groups.length; i++) {
			SearchResultGroup	rg=   groups[i];
			IJavaScriptUnit	unit= rg.getCompilationUnit();

			if (unit != null)	// Ignore hits within a binary unit
				result.add(rg);
			else
				fCallSitesInBinaryUnits= true;
		}
		return (SearchResultGroup[]) result.toArray(new SearchResultGroup[result.size()]);
	}

	/**
	 * Search for all calls to the given <code>IFunctionBinding</code> in the project
	 * that contains the compilation unit <code>fCUHandle</code>.
	 * @param methodBinding
	 * @param pm
	 * @param status
	 * @return an array of <code>SearchResultGroup</code>'s that identify the search matches
	 * @throws JavaScriptModelException
	 */
	private SearchResultGroup[] searchForCallsTo(IFunctionBinding methodBinding, IProgressMonitor pm, RefactoringStatus status) throws JavaScriptModelException {
		IFunction method= (IFunction) methodBinding.getJavaElement();
		final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(createSearchPattern(method, methodBinding));
		engine.setFiltering(true, true);
		engine.setScope(createSearchScope(method, methodBinding));
		engine.setStatus(status);
		engine.searchPattern(new SubProgressMonitor(pm, 1));
		return (SearchResultGroup[]) engine.getResults();
	}

	/**
	 * Returns an array of <code>SearchResultGroup</code>'s containing all method
	 * calls in the Java project that invoke the constructor identified by the given
	 * <code>IFunctionBinding</code>
	 * @param ctorBinding an <code>IFunctionBinding</code> identifying a particular
	 * constructor signature to search for
	 * @param pm an <code>IProgressMonitor</code> to use during this potentially
	 * lengthy operation
	 * @param status
	 * @return an array of <code>SearchResultGroup</code>'s identifying all
	 * calls to the given constructor signature
	 */
	private SearchResultGroup[] findAllCallsTo(IFunctionBinding ctorBinding, IProgressMonitor pm, RefactoringStatus status) throws JavaScriptModelException {
		SearchResultGroup[] groups= excludeBinaryUnits(searchForCallsTo(ctorBinding, pm, status));

		return groups;
	}

	private IType findNonPrimaryType(String fullyQualifiedName, IProgressMonitor pm, RefactoringStatus status) throws JavaScriptModelException {
		SearchPattern p= SearchPattern.createPattern(fullyQualifiedName, IJavaScriptSearchConstants.TYPE, IJavaScriptSearchConstants.DECLARATIONS, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
		final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(p);

		engine.setFiltering(true, true);
		engine.setScope(RefactoringScopeFactory.create(fCtorBinding.getJavaElement().getJavaScriptProject()));
		engine.setStatus(status);
		engine.searchPattern(new SubProgressMonitor(pm, 1));

		SearchResultGroup[] groups= (SearchResultGroup[]) engine.getResults();

		if (groups.length != 0) {
			for(int i= 0; i < groups.length; i++) {
				SearchMatch[] matches= groups[i].getSearchResults();
				for(int j= 0; j < matches.length; j++) {
					if (matches[j].getAccuracy() == SearchMatch.A_ACCURATE)
						return (IType) matches[j].getElement();
				}
			}
		}
		return null;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.IntroduceFactory_checking_preconditions, 1); 
			RefactoringStatus result= new RefactoringStatus();
			
			if (fFactoryClassName != null)
				result.merge(setFactoryClass(fFactoryClassName));
			if (result.hasFatalError())
				return result;
			fArgTypes= fCtorBinding.getParameterTypes();
			fCtorIsVarArgs= fCtorBinding.isVarargs();
			fAllCallsTo= findAllCallsTo(fCtorBinding, pm, result);
			fFormalArgNames= findCtorArgNames();
 
			IJavaScriptUnit[]	affectedFiles= collectAffectedUnits(fAllCallsTo);
			result.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(affectedFiles), getValidationContext()));

			if (fCallSitesInBinaryUnits)
				result.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.IntroduceFactory_callSitesInBinaryClass)); 

			return result;
		} finally {
			pm.done();
		}
	}

	/**
	 * Returns an array containing the argument names for the constructor
	 * identified by <code>fCtorBinding</code>, if available, or default
	 * names if unavailable (e.g. if the constructor resides in a binary unit).
	 */
	private String[] findCtorArgNames() {
		int			numArgs= fCtorBinding.getParameterTypes().length;
		String[]	names= new String[numArgs];

		JavaScriptUnit		ctorUnit= (JavaScriptUnit) ASTNodes.getParent(fCtorOwningClass, JavaScriptUnit.class);
		FunctionDeclaration	ctorDecl= (FunctionDeclaration) ctorUnit.findDeclaringNode(fCtorBinding.getKey());

		if (ctorDecl != null) {
			List	formalArgs= ctorDecl.parameters();
			int		i= 0;

			for(Iterator iter= formalArgs.iterator(); iter.hasNext(); i++) {
				SingleVariableDeclaration	svd= (SingleVariableDeclaration) iter.next();

				names[i]= svd.getName().getIdentifier();
			}
			return names;
		}

		// Have no way of getting the formal argument names; just fake it.
		for(int i=0; i < numArgs; i++)
			names[i]= "arg" + (i+1); //$NON-NLS-1$

		return names;
	}

	/**
	 * Creates and returns a new FunctionDeclaration that represents the factory
	 * method to be used in place of direct calls to the constructor in question.
	 * @param ast An AST used as a factory for various AST nodes
	 * @param ctorBinding binding for the constructor being wrapped
	 * @param unitRewriter the ASTRewrite to be used
	 */
	private FunctionDeclaration createFactoryMethod(AST ast, IFunctionBinding ctorBinding, ASTRewrite unitRewriter) {
		FunctionDeclaration		newMethod= ast.newFunctionDeclaration();
		SimpleName				newMethodName= ast.newSimpleName(fNewMethodName);
		ClassInstanceCreation	newCtorCall= ast.newClassInstanceCreation();
		ReturnStatement			ret= ast.newReturnStatement();
		Block		body= ast.newBlock();
		List		stmts= body.statements();
		String		retTypeName= ctorBinding.getName();

		createFactoryMethodSignature(ast, newMethod);

		newMethod.setName(newMethodName);
		newMethod.setBody(body);

        setMethodReturnType(newMethod, retTypeName, null, ast);

		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, Modifier.STATIC | Modifier.PUBLIC));

        setCtorTypeArguments(newCtorCall, retTypeName, null, ast);

        createFactoryMethodConstructorArgs(ast, newCtorCall);

		ret.setExpression(newCtorCall);
		stmts.add(ret);

		return newMethod;
	}

	/**
	 * Sets the type being instantiated in the given constructor call, including
     * specifying any necessary type arguments.
	 * @param newCtorCall the constructor call to modify
	 * @param ctorTypeName the simple name of the type being instantiated
	 * @param ctorOwnerTypeParameters the formal type parameters of the type being
	 * instantiated
	 * @param ast utility object used to create AST nodes
	 */
	private void setCtorTypeArguments(ClassInstanceCreation newCtorCall, String ctorTypeName, ITypeBinding[] ctorOwnerTypeParameters, AST ast) {
        if (ctorOwnerTypeParameters.length == 0) // easy, just a simple type
            newCtorCall.setType(ASTNodeFactory.newType(ast, ctorTypeName));
	}

	/**
	 * Sets the return type of the factory method, including any necessary type
	 * arguments. E.g., for constructor <code>Foo()</code> in <code>Foo&lt;T&gt;</code>,
	 * the factory method defines a method type parameter <code>&lt;T&gt;</code> and
	 * returns a <code>Foo&lt;T&gt;</code>.
	 * @param newMethod the method whose return type is to be set
	 * @param retTypeName the simple name of the return type (without type parameters)
	 * @param ctorOwnerTypeParameters the formal type parameters of the type that the
	 * factory method instantiates (whose constructor is being encapsulated)
	 * @param ast utility object used to create AST nodes
	 */
	private void setMethodReturnType(FunctionDeclaration newMethod, String retTypeName, ITypeBinding[] ctorOwnerTypeParameters, AST ast) {
        if (ctorOwnerTypeParameters.length == 0)
            newMethod.setReturnType2(ast.newSimpleType(ast.newSimpleName(retTypeName)));
	}

	/**
	 * Creates and adds the necessary argument declarations to the given factory method.<br>
	 * An argument is needed for each original constructor argument for which the
	 * evaluation of the actual arguments across all calls was not able to be
	 * pushed inside the factory method (e.g. arguments with side-effects, references
	 * to fields if the factory method is to be static or reside in a factory class,
	 * or arguments that varied across the set of constructor calls).<br>
	 * <code>fArgTypes</code> identifies such arguments by a <code>null</code> value.
	 * @param ast utility object used to create AST nodes
	 * @param newMethod the <code>FunctionDeclaration</code> for the factory method
	 */
	private void createFactoryMethodSignature(AST ast, FunctionDeclaration newMethod) {
		List argDecls= newMethod.parameters();

		for(int i=0; i < fArgTypes.length; i++) {
			SingleVariableDeclaration argDecl= ast.newSingleVariableDeclaration();
			Type argType;

			if (i == (fArgTypes.length - 1) && fCtorIsVarArgs) {
				// The trailing varargs arg has an extra array dimension, compared to
				// what we need to pass to setType()...
				argType= typeNodeForTypeBinding(fArgTypes[i].getElementType(),
						fArgTypes[i].getDimensions()-1, ast);
				argDecl.setVarargs(true);
			} else
				argType= typeNodeForTypeBinding(fArgTypes[i], 0, ast);

			argDecl.setName(ast.newSimpleName(fFormalArgNames[i]));
			argDecl.setType(argType);
			argDecls.add(argDecl);
		}
	}

	/**
	 * Returns a Type that describes the given ITypeBinding. If the binding
	 * refers to an object type, use the import rewriter to determine whether
	 * the reference requires a new import, or instead needs to be qualified.<br>
	 * Like ASTNodeFactory.newType(), but for the handling of imports.
	 * @param extraDims number of extra array dimensions to add to the resulting type
	 */
	private Type typeNodeForTypeBinding(ITypeBinding argType, int extraDims, AST ast) {
		if (extraDims > 0) {
			return ast.newArrayType(typeNodeForTypeBinding(argType, 0, ast), extraDims);
			
		} else if (argType.isArray()) {
			Type elementType= typeNodeForTypeBinding(argType.getElementType(), extraDims, ast);
			return ast.newArrayType(elementType, argType.getDimensions());
			
		} else {
			return fImportRewriter.addImport(argType, ast);
		}
	}

	/**
	 * Create the list of actual arguments to the constructor call that is
	 * encapsulated inside the factory method, and associate the arguments
	 * with the given constructor call object.
	 * @param ast utility object used to create AST nodes
	 * @param newCtorCall the newly-generated constructor call to be wrapped inside
	 * the factory method
	 */
	private void createFactoryMethodConstructorArgs(AST ast, ClassInstanceCreation newCtorCall) {
		List	argList= newCtorCall.arguments();

		for(int i=0; i < fArgTypes.length; i++) {
			ASTNode	ctorArg= ast.newSimpleName(fFormalArgNames[i]);

			argList.add(ctorArg);
		}
	}

	/**
	 * Creates and returns a new FunctionInvocation node to represent a call to
	 * the factory method that replaces a direct constructor call.<br>
	 * The original constructor call is marked as replaced by the new method
	 * call with the ASTRewrite instance fCtorCallRewriter.
	 * @param ast utility object used to create AST nodes
	 * @param ctorCall the ClassInstanceCreation to be marked as replaced
	 */
	private FunctionInvocation createFactoryMethodCall(AST ast, ClassInstanceCreation ctorCall,
													 ASTRewrite unitRewriter, TextEditGroup gd) {
		FunctionInvocation	factoryMethodCall= ast.newFunctionInvocation();

		List	actualFactoryArgs= factoryMethodCall.arguments();
		List	actualCtorArgs= ctorCall.arguments();

		// Need to use a qualified name for the factory method if we're not
		// in the context of the class holding the factory.
		AbstractTypeDeclaration	callOwner= (AbstractTypeDeclaration) ASTNodes.getParent(ctorCall, AbstractTypeDeclaration.class);
		ITypeBinding callOwnerBinding= callOwner.resolveBinding();

		if (callOwnerBinding == null ||
			!Bindings.equals(callOwner.resolveBinding(), fFactoryOwningClass.resolveBinding())) {
			String qualifier= fImportRewriter.addImport(fFactoryOwningClass.resolveBinding());
			factoryMethodCall.setExpression(ASTNodeFactory.newName(ast, qualifier));
		}
		
		factoryMethodCall.setName(ast.newSimpleName(fNewMethodName));

		for(int i=0; i < actualCtorArgs.size(); i++) {
			Expression	actualCtorArg= (Expression) actualCtorArgs.get(i);
			ASTNode		movedArg= unitRewriter.createMoveTarget(actualCtorArg);

			actualFactoryArgs.add(movedArg);
//			unitRewriter.createMove(actualCtorArg);
//			ASTNode		rewrittenArg= rewriteArgument(actualCtorArg);
//			actualFactoryArgs.add(rewrittenArg);
		}

		unitRewriter.replace(ctorCall, factoryMethodCall, gd);

		return factoryMethodCall;
	}

	/**
	 * Returns true iff the given <code>IJavaScriptUnit</code> is the unit
	 * containing the original constructor.
	 * @param unit
	 */
	private boolean isConstructorUnit(IJavaScriptUnit unit) {
		return unit.equals(ASTCreator.getCu(fCtorOwningClass));
	}

	/**
	 * Returns true iff we should actually change the original constructor's
	 * visibility to <code>protected</code>. This takes into account the user-
	 * requested mode and whether the constructor's compilation unit is in
	 * source form.
	 */
	private boolean shouldProtectConstructor() {
		return fProtectConstructor && fCtorOwningClass != null;
	}

	/**
	 * Creates and adds the necessary change to make the constructor method protected.
	 * Returns false iff the constructor didn't exist (i.e. was implicit)
	 */
	private boolean protectConstructor(JavaScriptUnit unitAST, ASTRewrite unitRewriter, TextEditGroup declGD) {
		FunctionDeclaration constructor= (FunctionDeclaration) unitAST.findDeclaringNode(fCtorBinding.getKey());

		// No need to rewrite the modifiers if the visibility is what we already want it to be.
		if (constructor == null || (JdtFlags.getVisibilityCode(constructor)) == fConstructorVisibility)
			return false;
		ModifierRewrite.create(unitRewriter, constructor).setVisibility(fConstructorVisibility, declGD);
		return true;
	}

	/**
	 * Add all changes necessary on the <code>IJavaScriptUnit</code> in the given
	 * <code>SearchResultGroup</code> to implement the refactoring transformation
	 * to the given <code>CompilationUnitChange</code>.
	 * @param rg the <code>SearchResultGroup</code> for which changes should be created
	 * @param unitChange the CompilationUnitChange object for the compilation unit in question
	 * @throws CoreException
	 */
	private boolean addAllChangesFor(SearchResultGroup rg, IJavaScriptUnit	unitHandle, CompilationUnitChange unitChange) throws CoreException {
//		IJavaScriptUnit	unitHandle= rg.getCompilationUnit();
		Assert.isTrue(rg == null || rg.getCompilationUnit() == unitHandle);
		JavaScriptUnit		unit= getASTFor(unitHandle);
		ASTRewrite			unitRewriter= ASTRewrite.create(unit.getAST());
		MultiTextEdit		root= new MultiTextEdit();
		boolean				someChange= false;

		unitChange.setEdit(root);
		fImportRewriter= StubUtility.createImportRewrite(unit, true);

		// First create the factory method
		if (unitHandle.equals(fFactoryUnitHandle)) {
			TextEditGroup	factoryGD= new TextEditGroup(RefactoringCoreMessages.IntroduceFactory_addFactoryMethod); 

			createFactoryChange(unitRewriter, unit, factoryGD);
			unitChange.addTextEditGroup(factoryGD);
			someChange= true;
		}

		// Now rewrite all the constructor calls to use the factory method
		if (rg != null)
			if (replaceConstructorCalls(rg, unit, unitRewriter, unitChange))
				someChange= true;

		// Finally, make the constructor private, if requested.
		if (shouldProtectConstructor() && isConstructorUnit(unitHandle)) {
			TextEditGroup	declGD= new TextEditGroup(RefactoringCoreMessages.IntroduceFactory_protectConstructor); 

			if (protectConstructor(unit, unitRewriter, declGD)) {
				unitChange.addTextEditGroup(declGD);
				someChange= true;
			}
		}

		if (someChange) {
			root.addChild(unitRewriter.rewriteAST());
			root.addChild(fImportRewriter.rewriteImports(null));
		}

		return someChange;
	}

	/**
	 * Returns an AST for the given compilation unit handle.<br>
	 * If this is the unit containing the selection or the unit in which the factory
	 * is to reside, checks the appropriate field (<code>fCU</code> or <code>fFactoryCU</code>,
	 * respectively) and initializes the field with a new AST only if not already done.
	 */
	private JavaScriptUnit getASTFor(IJavaScriptUnit unitHandle) {
		if (unitHandle.equals(fCUHandle)) { // is this the unit containing the selection?
			if (fCU == null) {
				fCU= ASTCreator.createAST(unitHandle, null);
				if (fCU.equals(fFactoryUnitHandle)) // if selection unit and factory unit are the same...
					fFactoryCU= fCU; // ...make sure the factory unit gets initialized
			}
			return fCU;
		} else if (unitHandle.equals(fFactoryUnitHandle)) { // is this the "factory unit"?
			if (fFactoryCU == null)
				fFactoryCU= ASTCreator.createAST(unitHandle, null);
			return fFactoryCU;
		} else
			return ASTCreator.createAST(unitHandle, null);
	}

	/**
	 * Use the given <code>ASTRewrite</code> to replace direct calls to the constructor
	 * with calls to the newly-created factory method.
	 * @param rg the <code>SearchResultGroup</code> indicating all of the constructor references
	 * @param unit the <code>JavaScriptUnit</code> to be rewritten
	 * @param unitRewriter the rewriter
	 * @param unitChange the compilation unit change
	 * @throws CoreException
	 * @return true iff at least one constructor call site was rewritten.
	 */
	private boolean replaceConstructorCalls(SearchResultGroup rg, JavaScriptUnit unit,
											ASTRewrite unitRewriter, CompilationUnitChange unitChange)
	throws CoreException {
		Assert.isTrue(ASTCreator.getCu(unit).equals(rg.getCompilationUnit()));
		SearchMatch[]	hits= rg.getSearchResults();
		AST	ctorCallAST= unit.getAST();
		boolean someCallPatched= false;

		for(int i=0; i < hits.length; i++) {
			ClassInstanceCreation	creation= getCtorCallAt(hits[i].getOffset(), hits[i].getLength(), unit);

			if (creation != null) {
				TextEditGroup gd= new TextEditGroup(RefactoringCoreMessages.IntroduceFactory_replaceCalls); 

				createFactoryMethodCall(ctorCallAST, creation, unitRewriter, gd);
				unitChange.addTextEditGroup(gd);
				someCallPatched= true;
			}
		}
		return someCallPatched;
	}

	/**
	 * Look "in the vicinity" of the given range to find the <code>ClassInstanceCreation</code>
	 * node that this search hit identified. Necessary because the <code>SearchEngine</code>
	 * doesn't always cough up text extents that <code>NodeFinder.perform()</code> agrees with.
	 * @param start
	 * @param length
	 * @param unitAST
	 * @return may return null if this is really a constructor->constructor call (e.g. "this(...)")
	 */
	private ClassInstanceCreation getCtorCallAt(int start, int length, JavaScriptUnit unitAST) throws CoreException {
		IJavaScriptUnit unitHandle= ASTCreator.getCu(unitAST);
		ASTNode node= NodeFinder.perform(unitAST, start, length);

		if (node == null)
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR,
					Messages.format(RefactoringCoreMessages.IntroduceFactory_noASTNodeForConstructorSearchHit, 
							new Object[] { Integer.toString(start), Integer.toString(start + length),
								unitHandle.getSource().substring(start, start + length),
								unitHandle.getElementName() }),
					null));

		if (node instanceof ClassInstanceCreation) {
			return (ClassInstanceCreation) node;
		} else if (node instanceof VariableDeclaration) {
			Expression	init= ((VariableDeclaration) node).getInitializer();

			if (init instanceof ClassInstanceCreation) {
				return (ClassInstanceCreation) init;
			} else if (init != null)
				throw new CoreException(JavaUIStatus.createError(IStatus.ERROR,
						Messages.format(RefactoringCoreMessages.IntroduceFactory_unexpectedInitializerNodeType, 
								new Object[] { init.toString(), unitHandle.getElementName() }),
						null));
			else
				throw new CoreException(JavaUIStatus.createError(IStatus.ERROR,
						Messages.format(RefactoringCoreMessages.IntroduceFactory_noConstructorCallNodeInsideFoundVarbleDecl, 
								new Object[] { node.toString() }),
						null));
		} else if (node instanceof ConstructorInvocation) {
			// This is a call we can bypass; it's from one constructor flavor
			// to another flavor on the same class.
			return null;
		} else if (node instanceof SuperConstructorInvocation) {
			// This is a call we can bypass; it's from one constructor flavor
			// to another flavor on the same class.
			fConstructorVisibility= Modifier.PROTECTED;
			return null;
		} else if (node instanceof ExpressionStatement) {
			Expression	expr= ((ExpressionStatement) node).getExpression();

			if (expr instanceof ClassInstanceCreation)
				return (ClassInstanceCreation) expr;
			else
				throw new CoreException(JavaUIStatus.createError(IStatus.ERROR,
						Messages.format(RefactoringCoreMessages.IntroduceFactory_unexpectedASTNodeTypeForConstructorSearchHit, 
								new Object[] { expr.toString(), unitHandle.getElementName() }),
						null));
		} else if (node instanceof SimpleName && (node.getParent() instanceof FunctionDeclaration || node.getParent() instanceof AbstractTypeDeclaration)) {
			// We seem to have been given a hit for an implicit call to the base-class constructor.
			// Do nothing with this (implicit) call, but have to make sure we make the derived class
			// doesn't lose access to the base-class constructor (so make it 'protected', not 'private').
			fConstructorVisibility= Modifier.PROTECTED;
			return null;
		} else
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR,
					Messages.format(RefactoringCoreMessages.IntroduceFactory_unexpectedASTNodeTypeForConstructorSearchHit, 
							new Object[] { node.getClass().getName() + "('" + node.toString() + "')", unitHandle.getElementName() }), //$NON-NLS-1$ //$NON-NLS-2$
					null));
	}

	/**
	 * Perform the AST rewriting necessary on the given <code>JavaScriptUnit</code>
	 * to create the factory method. The method will reside on the type identified by
	 * <code>fFactoryOwningClass</code>.
	 * @param unitRewriter
	 * @param unit
	 * @param gd the <code>GroupDescription</code> to associate with the changes made
	 */
	private void createFactoryChange(ASTRewrite unitRewriter, JavaScriptUnit unit, TextEditGroup gd) {
		// ================================================================================
		// First add the factory itself (method, class, and interface as needed/directed by user)
		AST				ast= unit.getAST();

		fFactoryMethod= createFactoryMethod(ast, fCtorBinding, unitRewriter);

		AbstractTypeDeclaration	factoryOwner= (AbstractTypeDeclaration) unit.findDeclaringNode(fFactoryOwningClass.resolveBinding().getKey());
		fImportRewriter.addImport(fCtorOwningClass.resolveBinding());

		int	idx= ASTNodes.getInsertionIndex(fFactoryMethod, factoryOwner.bodyDeclarations());

		if (idx < 0) idx= 0; // Guard against bug in getInsertionIndex()
		unitRewriter.getListRewrite(factoryOwner, factoryOwner.getBodyDeclarationsProperty()).insertAt(fFactoryMethod, idx, gd);
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.IntroduceFactory_createChanges, fAllCallsTo.length);
			final ITypeBinding binding= fFactoryOwningClass.resolveBinding();
			final Map arguments= new HashMap();
			String project= null;
			IJavaScriptProject javaProject= fCUHandle.getJavaScriptProject();
			if (javaProject != null)
				project= javaProject.getElementName();
			int flags= JavaScriptRefactoringDescriptor.JAR_MIGRATION | JavaScriptRefactoringDescriptor.JAR_REFACTORING | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
			if (binding.isNested() && !binding.isMember())
				flags|= JavaScriptRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
			final String description= Messages.format(RefactoringCoreMessages.IntroduceFactoryRefactoring_descriptor_description_short, fCtorOwningClass.getName());
			final String header= Messages.format(RefactoringCoreMessages.IntroduceFactory_descriptor_description, new String[] { fNewMethodName, BindingLabelProvider.getBindingLabel(binding, JavaScriptElementLabels.ALL_FULLY_QUALIFIED), BindingLabelProvider.getBindingLabel(fCtorBinding, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)});
			final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
			comment.addSetting(Messages.format(RefactoringCoreMessages.IntroduceFactoryRefactoring_original_pattern, BindingLabelProvider.getBindingLabel(fCtorBinding, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)));
			comment.addSetting(Messages.format(RefactoringCoreMessages.IntroduceFactoryRefactoring_factory_pattern, fNewMethodName));
			comment.addSetting(Messages.format(RefactoringCoreMessages.IntroduceFactoryRefactoring_owner_pattern, BindingLabelProvider.getBindingLabel(binding, JavaScriptElementLabels.ALL_FULLY_QUALIFIED)));
			if (fProtectConstructor)
				comment.addSetting(RefactoringCoreMessages.IntroduceFactoryRefactoring_declare_private);
			final JDTRefactoringDescriptor descriptor= new JDTRefactoringDescriptor(IJavaScriptRefactorings.INTRODUCE_FACTORY, project, description, comment.asString(), arguments, flags);
			arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fCUHandle));
			arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_NAME, fNewMethodName);
			arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + 1, descriptor.elementToHandle(binding.getJavaElement()));
			arguments.put(JDTRefactoringDescriptor.ATTRIBUTE_SELECTION, Integer.valueOf(fSelectionStart).toString() + " " + Integer.valueOf(fSelectionLength).toString()); //$NON-NLS-1$
			arguments.put(ATTRIBUTE_PROTECT, Boolean.valueOf(fProtectConstructor).toString());
			final DynamicValidationStateChange result= new DynamicValidationRefactoringChange(descriptor, RefactoringCoreMessages.IntroduceFactory_name);
			boolean hitInFactoryClass= false;
			boolean hitInCtorClass= false;
			for (int i= 0; i < fAllCallsTo.length; i++) {
				SearchResultGroup rg= fAllCallsTo[i];
				IJavaScriptUnit unitHandle= rg.getCompilationUnit();
				CompilationUnitChange cuChange= new CompilationUnitChange(getName(), unitHandle);

				if (addAllChangesFor(rg, unitHandle, cuChange))
					result.add(cuChange);

				if (unitHandle.equals(fFactoryUnitHandle))
					hitInFactoryClass= true;
				if (unitHandle.equals(ASTCreator.getCu(fCtorOwningClass)))
					hitInCtorClass= true;

				pm.worked(1);
				if (pm.isCanceled())
					throw new OperationCanceledException();
			}
			if (!hitInFactoryClass) { // Handle factory class if no search hits there
				CompilationUnitChange cuChange= new CompilationUnitChange(getName(), fFactoryUnitHandle);
				addAllChangesFor(null, fFactoryUnitHandle, cuChange);
				result.add(cuChange);
			}
			if (!hitInCtorClass && !fFactoryUnitHandle.equals(ASTCreator.getCu(fCtorOwningClass))) { // Handle constructor-owning class if no search hits there
				CompilationUnitChange cuChange= new CompilationUnitChange(getName(), ASTCreator.getCu(fCtorOwningClass));
				addAllChangesFor(null, ASTCreator.getCu(fCtorOwningClass), cuChange);
				result.add(cuChange);
			}
			return result;
		} finally {
			pm.done();
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.IntroduceFactory_name;
	}

	/**
	 * Returns the name to be used for the generated factory method.
	 */
	public String getNewMethodName() {
		return fNewMethodName;
	}

	/**
	 * Sets the name to be used for the generated factory method.<br>
	 * Returns a <code>RefactoringStatus</code> that indicates whether the
	 * given name is valid for the new factory method.
	 * @param newMethodName the name to be used for the generated factory method
	 */
	public RefactoringStatus setNewMethodName(String newMethodName) {
		Assert.isNotNull(newMethodName);
		fNewMethodName = newMethodName;

		RefactoringStatus	stat= Checks.checkMethodName(newMethodName);

		stat.merge(isUniqueMethodName(newMethodName));

		return stat;
	}

	/**
	 * Returns a <code>RefactoringStatus</code> that identifies whether the
	 * the name <code>newMethodName</code> is available to use as the name of
	 * the new factory method within the factory-owner class (either a to-be-
	 * created factory class or the constructor-owning class, depending on the
	 * user options).
	 * @param methodName
	 */
	private RefactoringStatus isUniqueMethodName(String methodName) {
		boolean	conflict= hasMethod(fFactoryOwningClass, methodName);

		return conflict ? RefactoringStatus.createErrorStatus(RefactoringCoreMessages.IntroduceFactory_duplicateMethodName + methodName) : new RefactoringStatus(); 
	}

	/**
	 * Returns true iff the given <code>AbstractTypeDeclaration</code> has a method with
	 * the given name.
	 * @param type
	 * @param name
	 */
	private boolean hasMethod(AbstractTypeDeclaration type, String name) {
		List	decls= type.bodyDeclarations();

		for (Iterator iter = decls.iterator(); iter.hasNext();) {
			BodyDeclaration decl = (BodyDeclaration) iter.next();
			if (decl instanceof FunctionDeclaration) {
				if (((FunctionDeclaration) decl).getName().getIdentifier().equals(name))
					return true;
			}
		}
		return false;
	}

	/**
	 * Returns true iff the selected constructor can be protected.
	 */
	public boolean canProtectConstructor() {
		return fFactoryCU.findDeclaringNode(fCtorBinding.getKey()) != null;
	}

	/**
	 * If the argument is true, change the visibility of the constructor to
	 * <code>protected</code>, thereby encapsulating it.
	 * @param protectConstructor
	 */
	public void setProtectConstructor(boolean protectConstructor) {
		fProtectConstructor = protectConstructor;
	}

	/**
	 * Returns the project on behalf of which this refactoring was invoked.
	 */
	public IJavaScriptProject getProject() {
		return fCUHandle.getJavaScriptProject();
	}

	/**
	 * Sets the class on which the generated factory method is to be placed.
	 * @param fullyQualifiedTypeName an <code>IType</code> referring to an existing class
	 */
	public RefactoringStatus setFactoryClass(String fullyQualifiedTypeName) {
		IType factoryType;

		try {
			factoryType= findFactoryClass(fullyQualifiedTypeName);
			if (factoryType == null)
				return RefactoringStatus.createErrorStatus(Messages.format(RefactoringCoreMessages.IntroduceFactory_noSuchClass, fullyQualifiedTypeName)); 

		} catch (JavaScriptModelException e) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.IntroduceFactory_cantCheckForInterface); 
		}

		IJavaScriptUnit	factoryUnitHandle= factoryType.getJavaScriptUnit();

		if (factoryType.isBinary())
			return RefactoringStatus.createErrorStatus(RefactoringCoreMessages.IntroduceFactory_cantPutFactoryInBinaryClass); 
		else {
			try {
				if (!fFactoryUnitHandle.equals(factoryUnitHandle)) {
					fFactoryCU= getASTFor(factoryUnitHandle);
					fFactoryUnitHandle= factoryUnitHandle;
				}
				fFactoryOwningClass= (AbstractTypeDeclaration) ASTNodes.getParent(NodeFinder.perform(fFactoryCU, factoryType.getNameRange()), AbstractTypeDeclaration.class);

				String factoryPkg= factoryType.getPackageFragment().getElementName();
				String ctorPkg= fCtorOwningClass.resolveBinding().getPackage().getName();

				if (!factoryPkg.equals(ctorPkg))
					fConstructorVisibility= Modifier.PUBLIC;
				else if (fFactoryOwningClass != fCtorOwningClass)
					fConstructorVisibility= 0; // No such thing as Modifier.PACKAGE...


				if (fFactoryOwningClass != fCtorOwningClass)
					fConstructorVisibility= 0; // No such thing as Modifier.PACKAGE...

			} catch (JavaScriptModelException e) {
				return RefactoringStatus.createFatalErrorStatus(e.getMessage());
			}
			return new RefactoringStatus();
		}
	}

	/**
	 * Finds the factory class associated with the fully qualified name.
	 * @param fullyQualifiedTypeName the fully qualified type name
	 * @return the factory class, or <code>null</code> if not found
	 * @throws JavaScriptModelException if an error occurs while finding the factory class
	 */
	private IType findFactoryClass(String fullyQualifiedTypeName) throws JavaScriptModelException {
		IType factoryType= getProject().findType(fullyQualifiedTypeName);
		if (factoryType == null) // presumably a non-primary type; try the search engine
			factoryType= findNonPrimaryType(fullyQualifiedTypeName, new NullProgressMonitor(), new RefactoringStatus());
		return factoryType;
	}

	/**
	 * Returns the name of the class on which the generated factory method is
	 * to be placed.
	 */
	public String getFactoryClassName() {
		return fFactoryOwningClass.resolveBinding().getQualifiedName();
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
			String handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists() || element.getElementType() != IJavaScriptElement.JAVASCRIPT_UNIT)
					return createInputFatalStatus(element, IJavaScriptRefactorings.INTRODUCE_FACTORY);
				else {
					fCUHandle= (IJavaScriptUnit) element;
		        	initialize();
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
			handle= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + 1);
			if (handle != null) {
				final IJavaScriptElement element= JDTRefactoringDescriptor.handleToElement(extended.getProject(), handle, false);
				if (element == null || !element.exists() || element.getElementType() != IJavaScriptElement.TYPE)
					return createInputFatalStatus(element, IJavaScriptRefactorings.INTRODUCE_FACTORY);
				else {
					final IType type= (IType) element;
					fFactoryClassName= type.getFullyQualifiedName();
				}
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_INPUT));
			final String name= extended.getAttribute(JDTRefactoringDescriptor.ATTRIBUTE_NAME);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				fNewMethodName= name;
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JDTRefactoringDescriptor.ATTRIBUTE_NAME));
			final String protect= extended.getAttribute(ATTRIBUTE_PROTECT);
			if (protect != null) {
				fProtectConstructor= Boolean.valueOf(protect).booleanValue();
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_PROTECT));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}
}
