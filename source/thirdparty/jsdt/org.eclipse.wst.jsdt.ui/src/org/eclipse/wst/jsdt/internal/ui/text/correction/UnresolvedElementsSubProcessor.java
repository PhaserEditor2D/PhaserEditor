/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Renaud Waldura &lt;renaud+eclipse@waldura.com&gt; - New class/interface with wizard
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTMatcher;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ArrayType;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.ConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldAccess;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.IPackageBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SimpleType;
import org.eclipse.wst.jsdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.SuperConstructorInvocation;
import org.eclipse.wst.jsdt.core.dom.SuperFieldAccess;
import org.eclipse.wst.jsdt.core.dom.SuperMethodInvocation;
import org.eclipse.wst.jsdt.core.dom.SwitchCase;
import org.eclipse.wst.jsdt.core.dom.SwitchStatement;
import org.eclipse.wst.jsdt.core.dom.ThisExpression;
import org.eclipse.wst.jsdt.core.dom.ThrowStatement;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.TypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.QualifiedTypeNameHistory;
import org.eclipse.wst.jsdt.internal.corext.util.TypeFilter;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.text.correction.ChangeMethodSignatureProposal.ChangeDescription;
import org.eclipse.wst.jsdt.internal.ui.text.correction.ChangeMethodSignatureProposal.EditDescription;
import org.eclipse.wst.jsdt.internal.ui.text.correction.ChangeMethodSignatureProposal.InsertDescription;
import org.eclipse.wst.jsdt.internal.ui.text.correction.ChangeMethodSignatureProposal.RemoveDescription;
import org.eclipse.wst.jsdt.internal.ui.text.correction.ChangeMethodSignatureProposal.SwapDescription;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.BindingLabelProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptElementImageDescriptor;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.text.java.IInvocationContext;
import org.eclipse.wst.jsdt.ui.text.java.IProblemLocation;

public class UnresolvedElementsSubProcessor {
	
	private static final String ADD_IMPORT_ID= "org.eclipse.wst.jsdt.ui.correction.addImport"; //$NON-NLS-1$

	public static void getVariableProposals(IInvocationContext context, IProblemLocation problem, IVariableBinding resolvedField, Collection proposals) throws CoreException {

		IJavaScriptUnit cu= context.getCompilationUnit();

		JavaScriptUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveredNode(astRoot);
		if (selectedNode == null) {
			return;
		}

		// type that defines the variable
		ITypeBinding binding= null;
		
		/* Commented out BC rev 1. quickfixing case 'var k = new <undefinedType>' */
		ITypeBinding declaringTypeBinding= Bindings.getBindingOfParentTypeContext(selectedNode);
//		if (declaringTypeBinding == null) {
//			return;
//		}

		// possible type kind of the node
		boolean suggestVariableProposals= true;
		int typeKind= 0;

		while (selectedNode instanceof ParenthesizedExpression) {
			selectedNode= ((ParenthesizedExpression) selectedNode).getExpression();
		}


		Name node= null;

		switch (selectedNode.getNodeType()) {
			case ASTNode.SIMPLE_NAME:
				node= (SimpleName) selectedNode;
				ASTNode parent= node.getParent();
				StructuralPropertyDescriptor locationInParent= node.getLocationInParent();
				if (locationInParent == FunctionInvocation.EXPRESSION_PROPERTY) {
					typeKind= SimilarElementsRequestor.CLASSES;
				} else if (locationInParent == FieldAccess.NAME_PROPERTY) {
					Expression expression= ((FieldAccess) parent).getExpression();
					if (expression != null) {
						binding= expression.resolveTypeBinding();
						if (binding == null) {
							node= null;
						}
					}
				} else if (parent instanceof SimpleType || parent instanceof ClassInstanceCreation) {
					
				//	suggestVariableProposals= false;
				//	typeKind= SimilarElementsRequestor.REF_TYPES_AND_VAR;
				} else if (parent instanceof QualifiedName) {
					Name qualifier= ((QualifiedName) parent).getQualifier();
					if (qualifier != node) {
						binding= qualifier.resolveTypeBinding();
					} else {
						typeKind= SimilarElementsRequestor.REF_TYPES;
					}
					ASTNode outerParent= parent.getParent();
					while (outerParent instanceof QualifiedName) {
						outerParent= outerParent.getParent();
					}
					if (outerParent instanceof SimpleType) {
						typeKind= SimilarElementsRequestor.REF_TYPES;
						suggestVariableProposals= false;
					}
				} else if (locationInParent == SwitchCase.EXPRESSION_PROPERTY) {
					ITypeBinding switchExp= ((SwitchStatement) node.getParent().getParent()).getExpression().resolveTypeBinding();
				} else if (locationInParent == SuperFieldAccess.NAME_PROPERTY) {
					binding= declaringTypeBinding.getSuperclass();
				}
				break;
			case ASTNode.QUALIFIED_NAME:
				QualifiedName qualifierName= (QualifiedName) selectedNode;
				ITypeBinding qualifierBinding= qualifierName.getQualifier().resolveTypeBinding();
				if (qualifierBinding != null) {
					node= qualifierName.getName();
					binding= qualifierBinding;
				} else {
					node= qualifierName.getQualifier();
					typeKind= SimilarElementsRequestor.REF_TYPES;
					suggestVariableProposals= node.isSimpleName();
				}
				if (selectedNode.getParent() instanceof SimpleType) {
					typeKind= SimilarElementsRequestor.REF_TYPES;
					suggestVariableProposals= false;
				}
				break;
			case ASTNode.FIELD_ACCESS:
				FieldAccess access= (FieldAccess) selectedNode;
				Expression expression= access.getExpression();
				if (expression != null) {
					binding= expression.resolveTypeBinding();
					if (binding != null) {
						node= access.getName();
					}
				}
				break;
			case ASTNode.SUPER_FIELD_ACCESS:
				binding= declaringTypeBinding.getSuperclass();
				node= ((SuperFieldAccess) selectedNode).getName();
				break;
			default:
		}

		if (node == null) {
			return;
		}

		// add type proposals
		if (typeKind != 0) {
			if (!JavaModelUtil.is50OrHigher(cu.getJavaScriptProject())) {
				typeKind &= ~(SimilarElementsRequestor.ANNOTATIONS | SimilarElementsRequestor.ENUMS | SimilarElementsRequestor.VARIABLES);
			}

			int relevance= Character.isUpperCase(ASTNodes.getSimpleNameIdentifier(node).charAt(0)) ? 5 : -2;
			addSimilarTypeProposals(typeKind, cu, node, relevance + 1, proposals);
			
			typeKind &= ~SimilarElementsRequestor.ANNOTATIONS;
			addNewTypeProposals(cu, node, typeKind, relevance, proposals);
		}

		if (!suggestVariableProposals) {
			return;
		}

		SimpleName simpleName= node.isSimpleName() ? (SimpleName) node : ((QualifiedName) node).getName();
		boolean isWriteAccess= ASTResolving.isWriteAccess(node);

		// similar variables
		addSimilarVariableProposals(cu, astRoot, binding, simpleName, isWriteAccess, proposals);

		if (resolvedField == null || binding == null || resolvedField.getDeclaringClass() != binding.getTypeDeclaration() && Modifier.isPrivate(resolvedField.getModifiers())) {
			
			// new fields
			addNewFieldProposals(cu, astRoot, binding, declaringTypeBinding, simpleName, isWriteAccess, proposals);
			
			// new parameters and local variables
			if (binding == null) {
				addNewVariableProposals(cu, node, simpleName, proposals);
			}
		}
	}

	private static void addNewVariableProposals(IJavaScriptUnit cu, Name node, SimpleName simpleName, Collection proposals) {
		String name= simpleName.getIdentifier();
		ASTNode bodyDeclaration= ASTResolving.findParentBodyDeclaration(node, true);
		int type= bodyDeclaration.getNodeType(); 
		if (type == ASTNode.FUNCTION_DECLARATION) {
			int relevance= StubUtility.hasParameterName(cu.getJavaScriptProject(), name) ? 8 : 5;
			String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createparameter_description, simpleName.getIdentifier());
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
			proposals.add(new NewVariableCompletionProposal(label, cu, NewVariableCompletionProposal.PARAM, simpleName, null, relevance, image));
		}
		if (type == ASTNode.INITIALIZER || type == ASTNode.JAVASCRIPT_UNIT ||
				(type == ASTNode.FUNCTION_DECLARATION && !ASTResolving.isInsideConstructorInvocation((FunctionDeclaration) bodyDeclaration, node))) {
			int relevance= StubUtility.hasLocalVariableName(cu.getJavaScriptProject(), name) ? 10 : 7;
			String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createlocal_description, simpleName.getIdentifier());
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
			proposals.add(new NewVariableCompletionProposal(label, cu, NewVariableCompletionProposal.LOCAL, simpleName, null, relevance, image));
		}

		if (node.getParent().getNodeType() == ASTNode.ASSIGNMENT) {
			Assignment assignment= (Assignment) node.getParent();
			if (assignment.getLeftHandSide() == node && assignment.getParent().getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
				ASTNode statement= assignment.getParent();
				ASTRewrite rewrite= ASTRewrite.create(statement.getAST());
				if (ASTNodes.isControlStatementBody(assignment.getParent().getLocationInParent())) {
					rewrite.replace(statement, rewrite.getAST().newBlock(), null);
				} else {
					rewrite.remove(statement, null);
				}
				String label= CorrectionMessages.UnresolvedElementsSubProcessor_removestatement_description;
				Image image= JavaScriptPlugin.getDefault().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 4, image);
				proposals.add(proposal);
			}
		}
	}

	private static void addNewFieldProposals(IJavaScriptUnit cu, JavaScriptUnit astRoot, ITypeBinding binding, ITypeBinding declaringTypeBinding, SimpleName simpleName, boolean isWriteAccess, Collection proposals) throws JavaScriptModelException {
		// new variables
		IJavaScriptUnit targetCU;
		ITypeBinding senderDeclBinding;
		if (binding != null) {
			senderDeclBinding= binding.getTypeDeclaration();
			targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, senderDeclBinding);
		} else { // binding is null for accesses without qualifier
			senderDeclBinding= declaringTypeBinding;
			targetCU= cu;
		}

		if (senderDeclBinding==null || !senderDeclBinding.isFromSource() || targetCU == null) {
			return;
		}
		
		boolean mustBeConst= ASTResolving.isInsideModifiers(simpleName);

		addNewFieldForType(targetCU, binding, senderDeclBinding, simpleName, isWriteAccess, mustBeConst, proposals);

		if (binding == null && senderDeclBinding.isNested()) {
			ASTNode anonymDecl= astRoot.findDeclaringNode(senderDeclBinding);
			if (anonymDecl != null) {
				ITypeBinding bind= Bindings.getBindingOfParentType(anonymDecl.getParent());
				if (!bind.isAnonymous()) {
					addNewFieldForType(targetCU, bind, bind, simpleName, isWriteAccess, mustBeConst, proposals);
				}
			}
		}
	}

	private static void addNewFieldForType(IJavaScriptUnit targetCU, ITypeBinding binding, ITypeBinding senderDeclBinding, SimpleName simpleName, boolean isWriteAccess, boolean mustBeConst, Collection proposals) {
		String name= simpleName.getIdentifier();
		String label;
		Image image;
		
		if (!mustBeConst) {
			if (binding == null) {
				label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createfield_description, name);
				image= JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE);
			} else {
				label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createfield_other_description, new Object[] { name, ASTResolving.getTypeSignature(senderDeclBinding) } );
				image= JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PUBLIC);
			}
			int fieldRelevance= StubUtility.hasFieldName(targetCU.getJavaScriptProject(), name) ? 9 : 6;
			proposals.add(new NewVariableCompletionProposal(label, targetCU, NewVariableCompletionProposal.FIELD, simpleName, senderDeclBinding, fieldRelevance, image));
		}

		if (!isWriteAccess && !senderDeclBinding.isAnonymous()) {
			if (binding == null) {
				label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createconst_description, name);
				image= JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE);
			} else {
				label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createconst_other_description, new Object[] { name, ASTResolving.getTypeSignature(senderDeclBinding) } );
				image= JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PUBLIC);
			}
			int constRelevance= StubUtility.hasConstantName(name) ? 9 : 4;
			proposals.add(new NewVariableCompletionProposal(label, targetCU, NewVariableCompletionProposal.CONST_FIELD, simpleName, senderDeclBinding, constRelevance, image));
		}
	}

	private static void addSimilarVariableProposals(IJavaScriptUnit cu, JavaScriptUnit astRoot, ITypeBinding binding, SimpleName node, boolean isWriteAccess, Collection proposals) {
		int kind= ScopeAnalyzer.VARIABLES | ScopeAnalyzer.CHECK_VISIBILITY;
		if (!isWriteAccess) {
			kind |= ScopeAnalyzer.METHODS; // also try to find similar methods
		}

		IBinding[] varsAndMethodsInScope= (new ScopeAnalyzer(astRoot)).getDeclarationsInScope(node, kind);
		if (varsAndMethodsInScope.length > 0) {
			// avoid corrections like int i= i;
			String otherNameInAssign= null;

			// help with x.getString() -> y.getString()
			String methodSenderName= null;
			String fieldSenderName= null;

			ASTNode parent= node.getParent();
			switch (parent.getNodeType()) {
			case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
				// node must be initializer
				otherNameInAssign= ((VariableDeclarationFragment) parent).getName().getIdentifier();
				break;
			case ASTNode.ASSIGNMENT:
				Assignment assignment= (Assignment) parent;
				if (isWriteAccess && assignment.getRightHandSide() instanceof SimpleName) {
					otherNameInAssign= ((SimpleName) assignment.getRightHandSide()).getIdentifier();
				} else if (!isWriteAccess && assignment.getLeftHandSide() instanceof SimpleName) {
					otherNameInAssign= ((SimpleName) assignment.getLeftHandSide()).getIdentifier();
				}
				break;
			case ASTNode.FUNCTION_INVOCATION:
				FunctionInvocation inv= (FunctionInvocation) parent;
				if (inv.getExpression() == node) {
					methodSenderName= inv.getName().getIdentifier();
				}
				break;
			case ASTNode.QUALIFIED_NAME:
				QualifiedName qualName= (QualifiedName) parent;
				if (qualName.getQualifier() == node) {
					fieldSenderName= qualName.getName().getIdentifier();
				}
				break;
			}


			ITypeBinding guessedType= ASTResolving.guessBindingForReference(node);

			ITypeBinding objectBinding= astRoot.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
			String identifier= node.getIdentifier();
			boolean isInStaticContext= ASTResolving.isInStaticContext(node);

			loop: for (int i= 0; i < varsAndMethodsInScope.length; i++) {
				IBinding varOrMeth= varsAndMethodsInScope[i];
				if (varOrMeth instanceof IVariableBinding) {
					IVariableBinding curr= (IVariableBinding) varOrMeth;
					String currName= curr.getName();
					if (currName.equals(otherNameInAssign)) {
						continue loop;
					}
					boolean isFinal= Modifier.isFinal(curr.getModifiers());
					if (isFinal && curr.isField() && isWriteAccess) {
						continue loop;
					}
					if (isInStaticContext && !Modifier.isStatic(curr.getModifiers()) && curr.isField()) {
						continue loop;
					}

					int relevance= 0;
					if (NameMatcher.isSimilarName(currName, identifier)) {
						relevance += 3; // variable with a similar name than the unresolved variable
					}
					if (currName.equalsIgnoreCase(identifier)) {
						relevance+= 5;
					}
					ITypeBinding varType= curr.getType();
					if (varType != null) {
						if (guessedType != null && guessedType != objectBinding) { // too many result with object
							// variable type is compatible with the guessed type
							if (!isWriteAccess && canAssign(varType, guessedType)
									|| isWriteAccess && canAssign(guessedType, varType)) {
								relevance += 2; // unresolved variable can be assign to this variable
							}
						}else if (guessedType==null && relevance==0) {
							/* type any? */
							relevance+=1;
						}
						if (methodSenderName != null && hasMethodWithName(varType, methodSenderName)) {
							relevance += 2;
						}
						if (fieldSenderName != null && hasFieldWithName(varType, fieldSenderName)) {
							relevance += 2;
						}
					}

					if (relevance > 0) {
						String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changevariable_description, currName);
						if(node.getParent() instanceof ClassInstanceCreation) {
							proposals.add(new RenameNodeCompletionProposal(label, cu, node.getParent().getStartPosition(), node.getParent().getLength(), currName, relevance));
						}else {
							proposals.add(new RenameNodeCompletionProposal(label, cu, node.getStartPosition(), node.getLength(), currName, relevance));
						}
					}
				} else if (varOrMeth instanceof IFunctionBinding) {
					IFunctionBinding curr= (IFunctionBinding) varOrMeth;
					if (!curr.isConstructor() && guessedType != null && canAssign(curr.getReturnType(), guessedType)) {
						if (NameMatcher.isSimilarName(curr.getName(), identifier)) {
							AST ast= astRoot.getAST();
							ASTRewrite rewrite= ASTRewrite.create(ast);
							String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changetomethod_description, ASTResolving.getMethodSignature(curr, false));
							Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
							LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, 8, image);
							proposals.add(proposal);

							FunctionInvocation newInv= ast.newFunctionInvocation();
							newInv.setName(ast.newSimpleName(curr.getName()));
							ITypeBinding[] parameterTypes= curr.getParameterTypes();
							for (int k= 0; k < parameterTypes.length; k++) {
								ASTNode arg= ASTNodeFactory.newDefaultExpression(ast, parameterTypes[k]);
								newInv.arguments().add(arg);
								proposal.addLinkedPosition(rewrite.track(arg), false, null);
							}
							rewrite.replace(node, newInv, null);
						}
					}
				}
			}
		}
		if (binding != null && binding.isArray()) {
			String idLength= "length"; //$NON-NLS-1$
			String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changevariable_description, idLength);
			proposals.add(new RenameNodeCompletionProposal(label, cu, node.getStartPosition(), node.getLength(), idLength, 8)); 
		}
	}

	private static boolean canAssign(ITypeBinding returnType, ITypeBinding guessedType) {
		return returnType.isAssignmentCompatible(guessedType);
	}

	private static boolean hasMethodWithName(ITypeBinding typeBinding, String name) {
		IVariableBinding[] fields= typeBinding.getDeclaredFields();
		for (int i= 0; i < fields.length; i++) {
			if (fields[i].getName().equals(name)) {
				return true;
			}
		}
		ITypeBinding superclass= typeBinding.getSuperclass();
		if (superclass != null) {
			return hasMethodWithName(superclass, name);
		}
		return false;
	}

	private static boolean hasFieldWithName(ITypeBinding typeBinding, String name) {
		IFunctionBinding[] methods= typeBinding.getDeclaredMethods();
		for (int i= 0; i < methods.length; i++) {
			if (methods[i].getName().equals(name)) {
				return true;
			}
		}
		ITypeBinding superclass= typeBinding.getSuperclass();
		if (superclass != null) {
			return hasMethodWithName(superclass, name);
		}
		return false;
	}

	private static int evauateTypeKind(ASTNode node, IJavaScriptProject project) {
		int kind= ASTResolving.getPossibleTypeKinds(node, JavaModelUtil.is50OrHigher(project));
		return kind;
	}


	public static void getTypeProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		IJavaScriptUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}

		int kind= evauateTypeKind(selectedNode, cu.getJavaScriptProject());

		while (selectedNode.getLocationInParent() == QualifiedName.NAME_PROPERTY) {
			selectedNode= selectedNode.getParent();
		}

		Name node= null;
		if (selectedNode instanceof SimpleType) {
			node= ((SimpleType) selectedNode).getName();
		} else if (selectedNode instanceof ArrayType) {
			Type elementType= ((ArrayType) selectedNode).getElementType();
			if (elementType.isSimpleType()) {
				node= ((SimpleType) elementType).getName();
			} else {
				return;
			}
		} else if (selectedNode instanceof Name) {
			node= (Name) selectedNode;
		} else {
			return;
		}

		// change to similar type proposals
		addSimilarTypeProposals(kind, cu, node, 3, proposals);

		while (node.getParent() instanceof QualifiedName) {
			node= (Name) node.getParent();
		}
		
		if (selectedNode != node) {
			kind= evauateTypeKind(node, cu.getJavaScriptProject());
		}
		if ((kind & (SimilarElementsRequestor.CLASSES | SimilarElementsRequestor.INTERFACES)) != 0) {
			kind &= ~SimilarElementsRequestor.ANNOTATIONS; // only propose annotations when there are no other suggestions
		}		
		addNewTypeProposals(cu, node, kind, 0, proposals);
	}

	private static void addSimilarTypeProposals(int kind, IJavaScriptUnit cu, Name node, int relevance, Collection proposals) throws CoreException {
		SimilarElement[] elements= SimilarElementsRequestor.findSimilarElement(cu, node, kind);

		// try to resolve type in context -> highest severity
		String resolvedTypeName= null;
		ITypeBinding binding= ASTResolving.guessBindingForTypeReference(node);
		if (binding != null) {
			ITypeBinding simpleBinding= binding;
			if (simpleBinding.isArray()) {
				simpleBinding= simpleBinding.getElementType();
			}
			simpleBinding= simpleBinding.getTypeDeclaration();
			
			resolvedTypeName= simpleBinding.getQualifiedName();
			CUCorrectionProposal proposal= createTypeRefChangeProposal(cu, resolvedTypeName, node, relevance + 2, elements.length);
			proposals.add(proposal);
			if (proposal instanceof AddImportCorrectionProposal)
				proposal.setRelevance(relevance + elements.length + 2);
		} else {
			ASTNode normalizedNode= ASTNodes.getNormalizedNode(node);
			if (!(normalizedNode.getParent() instanceof Type) && node.getParent() != normalizedNode) {
				ITypeBinding normBinding= ASTResolving.guessBindingForTypeReference(normalizedNode);
				if (normBinding != null) { 
					proposals.add(createTypeRefChangeFullProposal(cu, normBinding, normalizedNode, relevance + 2));
				}
			}
		}

		// add all similar elements
		for (int i= 0; i < elements.length; i++) {
			SimilarElement elem= elements[i];
			if ((elem.getKind() & SimilarElementsRequestor.ALL_TYPES) != 0) {
				String fullName= elem.getName();
				if (!fullName.equals(resolvedTypeName)) {
					proposals.add(createTypeRefChangeProposal(cu, fullName, node, relevance, elements.length));
				}
			}
		}
	}
		
	private static CUCorrectionProposal createTypeRefChangeProposal(IJavaScriptUnit cu, String fullName, Name node, int relevance, int maxProposals) throws CoreException {
		ImportRewrite importRewrite= null;
		String simpleName= fullName;
		String packName= Signature.getQualifier(fullName);
		if (packName.length() > 0) { // no imports for primitive types, type variables
			importRewrite= StubUtility.createImportRewrite((JavaScriptUnit) node.getRoot(), true);
			simpleName= importRewrite.addImport(fullName);
		}
		
		if (!isLikelyTypeName(simpleName)) {
			relevance -= 2;
		}

		ASTRewriteCorrectionProposal proposal;
		if (importRewrite != null && node.isSimpleName() && simpleName.equals(((SimpleName) node).getIdentifier())) { // import only
			// import only
			String[] arg= { simpleName, packName };
			String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_importtype_description, arg);
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_IMPDECL);
			int boost= QualifiedTypeNameHistory.getBoost(fullName, 0, maxProposals);
			proposal= new AddImportCorrectionProposal(label, cu, relevance + 100 + boost, image, packName, simpleName, (SimpleName)node);
			proposal.setCommandId(ADD_IMPORT_ID);
		} else {
			String label;
			if (packName.length() == 0) {
				label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changetype_nopack_description, simpleName);
			} else {
				String[] arg= { simpleName, packName };
				label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changetype_description, arg);
			}
			ASTRewrite rewrite= ASTRewrite.create(node.getAST());
			rewrite.replace(node, rewrite.createStringPlaceholder(simpleName, ASTNode.SIMPLE_TYPE), null);
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, relevance, image);
		}
		if (importRewrite != null) {
			proposal.setImportRewrite(importRewrite);
		}
		return proposal;
	}
	
	private static CUCorrectionProposal createTypeRefChangeFullProposal(IJavaScriptUnit cu, ITypeBinding binding, ASTNode node, int relevance) throws CoreException {
		ASTRewrite rewrite= ASTRewrite.create(node.getAST());
		String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_change_full_type_description, BindingLabelProvider.getBindingLabel(binding, JavaScriptElementLabels.ALL_DEFAULT));
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, relevance + 3, image);
		
		ImportRewrite imports= proposal.createImportRewrite((JavaScriptUnit) node.getRoot());
		Type type= imports.addImport(binding, node.getAST());
		
		rewrite.replace(node, type, null);
		return proposal;
	}

	private static boolean isLikelyTypeName(String name) {
		return name.length() > 0 && Character.isUpperCase(name.charAt(0));
	}

	private static boolean isLikelyPackageName(String name) {
		if (name.length() != 0) {
			int i= 0;
			do {
				if (Character.isUpperCase(name.charAt(i))) {
					return false;
				}
				i= name.indexOf('.', i) + 1;
			} while (i != 0 && i < name.length());
		}
		return true;
	}

	private static boolean isLikelyTypeParameterName(String name) {
		return name.length() == 1 && Character.isUpperCase(name.charAt(0));
	}

	public static void addNewTypeProposals(IJavaScriptUnit cu, Name refNode, int kind, int relevance, Collection proposals) throws JavaScriptModelException {
		Name node= refNode;
		do {
			String typeName= ASTNodes.getSimpleNameIdentifier(node);
			Name qualifier= null;
			// only propose to create types for qualifiers when the name starts with upper case
			boolean isPossibleName= isLikelyTypeName(typeName) || (node == refNode);
			if (isPossibleName) {
				IPackageFragment enclosingPackage= null;
				IType enclosingType= null;
				if (node.isSimpleName()) {
					enclosingPackage= (IPackageFragment) cu.getParent();
					// don't suggest member type, user can select it in wizard
				} else {
					Name qualifierName= ((QualifiedName) node).getQualifier();
					 IBinding binding= qualifierName.resolveBinding();
					 if (binding != null && binding.isRecovered()) {
						 binding= null;
					 }
					 if (binding instanceof ITypeBinding) {
						enclosingType=(IType) binding.getJavaElement();
					 } else if (binding instanceof IPackageBinding) {
						qualifier= qualifierName;
					 	enclosingPackage= (IPackageFragment) binding.getJavaElement();
					 } else {
					 	IJavaScriptElement[] res= cu.codeSelect(qualifierName.getStartPosition(), qualifierName.getLength());
						if (res!= null && res.length > 0 && res[0] instanceof IType) {
							enclosingType= (IType) res[0];
						} else {
							qualifier= qualifierName;
							enclosingPackage= JavaModelUtil.getPackageFragmentRoot(cu).getPackageFragment(ASTResolving.getFullName(qualifierName));
						}
					 }
				}
				int rel= relevance;
				if (enclosingPackage != null && isLikelyPackageName(enclosingPackage.getElementName())) {
					rel += 3;
				}

				if ((enclosingPackage != null && !enclosingPackage.getJavaScriptUnit(typeName + JavaModelUtil.DEFAULT_CU_SUFFIX).exists()) // new top level type
						|| (enclosingType != null && !enclosingType.isReadOnly() && !enclosingType.getType(typeName).exists())) { // new member type
					IJavaScriptElement enclosing= enclosingPackage != null ? (IJavaScriptElement) enclosingPackage : enclosingType;

					if ((kind & SimilarElementsRequestor.CLASSES) != 0) {
			            proposals.add(new NewCUCompletionUsingWizardProposal(cu, node, NewCUCompletionUsingWizardProposal.K_CLASS, enclosing, rel+3));
					}
//					if ((kind & SimilarElementsRequestor.INTERFACES) != 0) {
//			            proposals.add(new NewCUCompletionUsingWizardProposal(cu, node, NewCUCompletionUsingWizardProposal.K_INTERFACE, enclosing, rel+2));
//					}
//					if ((kind & SimilarElementsRequestor.ENUMS) != 0) {
//			            proposals.add(new NewCUCompletionUsingWizardProposal(cu, node, NewCUCompletionUsingWizardProposal.K_ENUM, enclosing, rel));
//					}
//					if ((kind & SimilarElementsRequestor.ANNOTATIONS) != 0) {
//						proposals.add(new NewCUCompletionUsingWizardProposal(cu, node, NewCUCompletionUsingWizardProposal.K_ANNOTATION, enclosing, rel + 1));
//					}
				}
			}
			node= qualifier;
		} while (node != null);

		// type parameter proposals
		if (refNode.isSimpleName() && ((kind & SimilarElementsRequestor.VARIABLES)  != 0)) {
			JavaScriptUnit root= (JavaScriptUnit) refNode.getRoot();
			String name= ((SimpleName) refNode).getIdentifier();
			BodyDeclaration declaration= ASTResolving.findParentBodyDeclaration(refNode);
			int baseRel= relevance;
			if (isLikelyTypeParameterName(name)) {
				baseRel += 4;
			}
			while (declaration != null) {
				IBinding binding= null;
				int rel= baseRel;
				if (declaration instanceof FunctionDeclaration) {
					binding= ((FunctionDeclaration) declaration).resolveBinding();
				} else if (declaration instanceof TypeDeclaration) {
					binding= ((TypeDeclaration) declaration).resolveBinding();
					rel++;
				}
				if (!Modifier.isStatic(declaration.getModifiers())) {
					declaration= ASTResolving.findParentBodyDeclaration(declaration.getParent());
				} else {
					declaration= null;
				}
			}
		}
	}

	public static void getMethodProposals(IInvocationContext context, IProblemLocation problem, boolean isOnlyParameterMismatch, Collection proposals) throws CoreException {

		IJavaScriptUnit cu= context.getCompilationUnit();

		JavaScriptUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);

		if (!(selectedNode instanceof SimpleName)) {
			return;
		}
		SimpleName nameNode= (SimpleName) selectedNode;

		List arguments;
		Expression sender;
		boolean isSuperInvocation;

		ASTNode invocationNode= nameNode.getParent();
		if (invocationNode instanceof FunctionInvocation) {
			FunctionInvocation methodImpl= (FunctionInvocation) invocationNode;
			arguments= methodImpl.arguments();
			sender= methodImpl.getExpression();
			isSuperInvocation= false;
		} else if (invocationNode instanceof SuperMethodInvocation) {
			SuperMethodInvocation methodImpl= (SuperMethodInvocation) invocationNode;
			arguments= methodImpl.arguments();
			sender= methodImpl.getQualifier();
			isSuperInvocation= true;
		} else {
			return;
		}

		String methodName= nameNode.getIdentifier();
		int nArguments= arguments.size();

		// corrections
		IBinding[] bindings= (new ScopeAnalyzer(astRoot)).getDeclarationsInScope(nameNode, ScopeAnalyzer.METHODS);

		HashSet suggestedRenames= new HashSet();
		for (int i= 0; i < bindings.length; i++) {
			IFunctionBinding binding= (IFunctionBinding) bindings[i];
			String curr= binding.getName();
			if (!curr.equals(methodName) && binding.getParameterTypes().length == nArguments && NameMatcher.isSimilarName(methodName, curr) && suggestedRenames.add(curr)) {
				String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changemethod_description, curr);
				proposals.add(new RenameNodeCompletionProposal(label, context.getCompilationUnit(), problem.getOffset(), problem.getLength(), curr, 6));
			}
		}
		suggestedRenames= null;

		if (isOnlyParameterMismatch) {
			ArrayList parameterMismatchs= new ArrayList();
			for (int i= 0; i < bindings.length; i++) {
				IFunctionBinding binding= (IFunctionBinding) bindings[i];
				if (binding.getName().equals(methodName)) {
					parameterMismatchs.add(binding);
				}
			}
			addParameterMissmatchProposals(context, problem, parameterMismatchs, invocationNode, arguments, proposals);
		}

		// new method
		addNewMethodProposals(cu, astRoot, sender, arguments, isSuperInvocation, invocationNode, methodName, proposals);

		if (!isSuperInvocation && sender == null && invocationNode.getParent() instanceof ThrowStatement) {
			String str= "new ";   //$NON-NLS-1$ // do it the manual way, copting all the arguments is nasty
			String label= CorrectionMessages.UnresolvedElementsSubProcessor_addnewkeyword_description;
			int relevance= Character.isUpperCase(methodName.charAt(0)) ? 7 : 4;
			ReplaceCorrectionProposal proposal= new ReplaceCorrectionProposal(label, cu, invocationNode.getStartPosition(), 0, str, relevance);
			proposals.add(proposal);
		}

	}

	private static void addNewMethodProposals(IJavaScriptUnit cu, JavaScriptUnit astRoot, Expression sender, List arguments, boolean isSuperInvocation, ASTNode invocationNode, String methodName, Collection proposals) throws JavaScriptModelException {
		ITypeBinding nodeParentType= Bindings.getBindingOfParentType(invocationNode);
		ITypeBinding binding= null;
		if (sender != null) {
			binding= sender.resolveTypeBinding();
		} else {
			binding= nodeParentType;
			if (isSuperInvocation && binding != null) {
				binding= binding.getSuperclass();
			}
		}
		if (binding != null && binding.isFromSource()) {
			ITypeBinding senderDeclBinding= binding.getTypeDeclaration();

			IJavaScriptUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, senderDeclBinding);
			if (targetCU != null) {
				String label;
				Image image;
				ITypeBinding[] parameterTypes= getParameterTypes(arguments);
				if (parameterTypes != null) {
					String sig= ASTResolving.getMethodSignature(methodName, parameterTypes, false);
	
					if (ASTResolving.isUseableTypeInContext(parameterTypes, senderDeclBinding, false)) {
						if (nodeParentType == senderDeclBinding) {
							label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createmethod_description, sig);
							image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PRIVATE);
						} else {
							label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createmethod_other_description, new Object[] { sig, senderDeclBinding.getName() } );
							image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
						}
						proposals.add(new NewMethodCompletionProposal(label, targetCU, invocationNode, arguments, senderDeclBinding, 5, image));
					}
					if (senderDeclBinding.isNested() && cu.equals(targetCU) && sender == null && Bindings.findMethodInHierarchy(senderDeclBinding, methodName, (ITypeBinding[]) null) == null) { // no covering method
						ASTNode anonymDecl= astRoot.findDeclaringNode(senderDeclBinding);
						if (anonymDecl != null) {
							senderDeclBinding= Bindings.getBindingOfParentType(anonymDecl.getParent());
							if (!senderDeclBinding.isAnonymous() && ASTResolving.isUseableTypeInContext(parameterTypes, senderDeclBinding, false)) {
								String[] args= new String[] { sig, ASTResolving.getTypeSignature(senderDeclBinding) };
								label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createmethod_other_description, args);
								image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PROTECTED);
								proposals.add(new NewMethodCompletionProposal(label, targetCU, invocationNode, arguments, senderDeclBinding, 5, image));
							}
						}
					}
				}
			}
		}
	}

	private static void addParameterMissmatchProposals(IInvocationContext context, IProblemLocation problem, List similarElements, ASTNode invocationNode, List arguments, Collection proposals) throws CoreException {
		int nSimilarElements= similarElements.size();
		ITypeBinding[] argTypes= getArgumentTypes(arguments);
		if (argTypes == null || nSimilarElements == 0)  {
			return;
		}

		for (int i= 0; i < nSimilarElements; i++) {
			IFunctionBinding elem = (IFunctionBinding) similarElements.get(i);
			int diff= elem.getParameterTypes().length - argTypes.length;
			if (diff == 0) {
				int nProposals= proposals.size();
				doEqualNumberOfParameters(context, invocationNode, problem, arguments, argTypes, elem, proposals);
				if (nProposals != proposals.size()) {
					return; // only suggest for one method (avoid duplicated proposals)
				}
			} else if (diff > 0) {
				doMoreParameters(context, problem, invocationNode, arguments, argTypes, elem, proposals);
			} else {
				doMoreArguments(context, problem, invocationNode, arguments, argTypes, elem, proposals);
			}
		}
	}

	private static void doMoreParameters(IInvocationContext context, IProblemLocation problem, ASTNode invocationNode, List arguments, ITypeBinding[] argTypes, IFunctionBinding methodBinding, Collection proposals) throws CoreException {
		ITypeBinding[] paramTypes= methodBinding.getParameterTypes();
		int k= 0, nSkipped= 0;
		int diff= paramTypes.length - argTypes.length;
		int[] indexSkipped= new int[diff];
		for (int i= 0; i < paramTypes.length; i++) {
			if (k < argTypes.length && canAssign(argTypes[k], paramTypes[i])) {
				k++; // match
			} else {
				if (nSkipped >= diff) {
					return; // too different
				}
				indexSkipped[nSkipped++]= i;
			}
		}
		ITypeBinding declaringType= methodBinding.getDeclaringClass();
		IJavaScriptUnit cu= context.getCompilationUnit();
		JavaScriptUnit astRoot= context.getASTRoot();

		// add arguments
		{
			String[] arg= new String[] { ASTResolving.getMethodSignature(methodBinding, false) };
			String label;
			if (diff == 1) {
				label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_addargument_description, arg);
			} else {
				label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_addarguments_description, arg);
			}
			AddArgumentCorrectionProposal proposal= new AddArgumentCorrectionProposal(label, context.getCompilationUnit(), invocationNode, indexSkipped, paramTypes, 8);
			proposal.setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD));
			proposals.add(proposal);
		}

		// remove parameters
		if (!declaringType.isFromSource()) {
			return;
		}

		IJavaScriptUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, declaringType);
		if (targetCU != null) {
			IFunctionBinding methodDecl= methodBinding.getMethodDeclaration();
			ITypeBinding[] declParameterTypes= methodDecl.getParameterTypes();

			ChangeDescription[] changeDesc= new ChangeDescription[declParameterTypes.length];
			ITypeBinding[] changedTypes= new ITypeBinding[diff];
			for (int i= diff - 1; i >= 0; i--) {
				int idx= indexSkipped[i];
				changeDesc[idx]= new RemoveDescription();
				changedTypes[i]= declParameterTypes[idx];
			}
			String[] arg= new String[] { ASTResolving.getMethodSignature(methodDecl, !cu.equals(targetCU)), getTypeNames(changedTypes) };
			String label;
			if (methodDecl.isConstructor()) {
				if (diff == 1) {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_removeparam_constr_description, arg);
				} else {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_removeparams_constr_description, arg);
				}
			} else {
				if (diff == 1) {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_removeparam_description, arg);
				} else {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_removeparams_description, arg);
				}
			}

			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_REMOVE);
			ChangeMethodSignatureProposal proposal= new ChangeMethodSignatureProposal(label, targetCU, invocationNode, methodDecl, changeDesc, null, 5, image);
			proposals.add(proposal);
		}
	}

	private static String getTypeNames(ITypeBinding[] types) {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < types.length; i++) {
			if (i > 0) {
				buf.append(", "); //$NON-NLS-1$
			}
			buf.append(ASTResolving.getTypeSignature(types[i]));
		}
		return buf.toString();
	}

	private static String getArgumentName(IJavaScriptUnit cu, List arguments, int index) {
		String def= String.valueOf(index + 1);

		ASTNode expr= (ASTNode) arguments.get(index);
		if (expr.getLength() > 18) {
			return def;
		}
		ASTMatcher matcher= new ASTMatcher();
		for (int i= 0; i < arguments.size(); i++) {
			if (i != index && matcher.safeSubtreeMatch(expr, arguments.get(i))) {
				return def;
			}
		}
		return '\'' + ASTNodes.asString(expr) + '\'';
	}

	private static void doMoreArguments(IInvocationContext context, IProblemLocation problem, ASTNode invocationNode, List arguments, ITypeBinding[] argTypes, IFunctionBinding methodRef, Collection proposals) throws CoreException {
		ITypeBinding[] paramTypes= methodRef.getParameterTypes();
		int k= 0, nSkipped= 0;
		int diff= argTypes.length - paramTypes.length;
		int[] indexSkipped= new int[diff];
		for (int i= 0; i < argTypes.length; i++) {
			if (k < paramTypes.length && canAssign(argTypes[i], paramTypes[k])) {
				k++; // match
			} else {
				if (nSkipped >= diff) {
					return; // too different
				}
				indexSkipped[nSkipped++]= i;
			}
		}

		IJavaScriptUnit cu= context.getCompilationUnit();
		JavaScriptUnit astRoot= context.getASTRoot();

		// remove arguments
		{
			ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());

			for (int i= diff - 1; i >= 0; i--) {
				rewrite.remove((Expression) arguments.get(indexSkipped[i]), null);
			}
			String[] arg= new String[] { ASTResolving.getMethodSignature(methodRef, false) };
			String label;
			if (diff == 1) {
				label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_removeargument_description, arg);
			} else {
				label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_removearguments_description, arg);
			}
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_REMOVE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 8, image);
			proposals.add(proposal);
		}

		IFunctionBinding methodDecl= methodRef.getMethodDeclaration();
		ITypeBinding declaringType= methodDecl.getDeclaringClass();

		// add parameters
		if (!declaringType.isFromSource()) {
			return;
		}
		IJavaScriptUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, declaringType);
		if (targetCU != null) {
			boolean isDifferentCU= !cu.equals(targetCU);

			if (isImplicitConstructor(methodDecl, targetCU)) {
				return;
			}

			ChangeDescription[] changeDesc= new ChangeDescription[argTypes.length];
			ITypeBinding[] changeTypes= new ITypeBinding[diff];
			for (int i= diff - 1; i >= 0; i--) {
				int idx= indexSkipped[i];
				Expression arg= (Expression) arguments.get(idx);
				String name= arg instanceof SimpleName ? ((SimpleName) arg).getIdentifier() : null;
				ITypeBinding newType= Bindings.normalizeTypeBinding(argTypes[idx]);
				if (newType == null) {
					newType= astRoot.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
				}
				if (!ASTResolving.isUseableTypeInContext(newType, methodDecl, false)) {
					return;
				}
				changeDesc[idx]= new InsertDescription(newType, name);
				changeTypes[i]= newType;
			}
			String[] arg= new String[] { ASTResolving.getMethodSignature(methodDecl, isDifferentCU), getTypeNames(changeTypes) };
			String label;
			if (methodDecl.isConstructor()) {
				if (diff == 1) {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_addparam_constr_description, arg);
				} else {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_addparams_constr_description, arg);
				}
			} else {
				if (diff == 1) {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_addparam_description, arg);
				} else {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_addparams_description, arg);
				}
			}
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
			ChangeMethodSignatureProposal proposal= new ChangeMethodSignatureProposal(label, targetCU, invocationNode, methodDecl, changeDesc, null, 5, image);
			proposals.add(proposal);
		}
	}

	private static boolean isImplicitConstructor(IFunctionBinding meth, IJavaScriptUnit targetCU) {
		return meth.isDefaultConstructor();
	}



	private static ITypeBinding[] getParameterTypes(List args) {
		ITypeBinding[] params= new ITypeBinding[args.size()];
		for (int i= 0; i < args.size(); i++) {
			Expression expr= (Expression) args.get(i);
			ITypeBinding curr= Bindings.normalizeTypeBinding(expr.resolveTypeBinding());
			
			if (curr == null) {
				curr= expr.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
			}
			params[i]= curr;
		}
		return params;
	}



	private static void doEqualNumberOfParameters(IInvocationContext context, ASTNode invocationNode, IProblemLocation problem, List arguments, ITypeBinding[] argTypes, IFunctionBinding methodBinding, Collection proposals) throws CoreException {
		ITypeBinding[] paramTypes= methodBinding.getParameterTypes();
		int[] indexOfDiff= new int[paramTypes.length];
		int nDiffs= 0;
		for (int n= 0; n < argTypes.length; n++) {
			if (!canAssign(argTypes[n], paramTypes[n])) {
				indexOfDiff[nDiffs++]= n;
			}
		}
		ITypeBinding declaringTypeDecl= methodBinding.getDeclaringClass().getTypeDeclaration();

		IJavaScriptUnit cu= context.getCompilationUnit();
		JavaScriptUnit astRoot= context.getASTRoot();

		ASTNode nameNode= problem.getCoveringNode(astRoot);
		if (nameNode == null) {
			return;
		}

		if (nDiffs == 0) {
			if (nameNode.getParent() instanceof FunctionInvocation) {
				FunctionInvocation inv= (FunctionInvocation) nameNode.getParent();
				if (inv.getExpression() == null) {
					addQualifierToOuterProposal(context, inv, methodBinding, proposals);
				}
			}
			return;
		}

		if (nDiffs == 1) { // one argument mismatching: try to fix
			int idx= indexOfDiff[0];
			Expression nodeToCast= (Expression) arguments.get(idx);
			ITypeBinding castType= paramTypes[idx];
			castType= Bindings.normalizeTypeBinding(castType);
			
			if (castType != null) {
				TypeMismatchSubProcessor.addChangeSenderTypeProposals(context, nodeToCast, castType, false, 5, proposals);
			}
		}
		if (nDiffs == 2) { // try to swap
			int idx1= indexOfDiff[0];
			int idx2= indexOfDiff[1];
			boolean canSwap= canAssign(argTypes[idx1], paramTypes[idx2]) && canAssign(argTypes[idx2], paramTypes[idx1]);
			 if (canSwap) {
				Expression arg1= (Expression) arguments.get(idx1);
				Expression arg2= (Expression) arguments.get(idx2);

				ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
				rewrite.replace(arg1, rewrite.createCopyTarget(arg2), null);
				rewrite.replace(arg2, rewrite.createCopyTarget(arg1), null);
				{
					String[] arg= new String[] { getArgumentName(cu, arguments, idx1), getArgumentName(cu, arguments, idx2) };
					String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_swaparguments_description, arg);
					Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
					ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 8, image);
					proposals.add(proposal);
				}

				if (declaringTypeDecl.isFromSource()) {
					IJavaScriptUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, declaringTypeDecl);
					if (targetCU != null) {
						ChangeDescription[] changeDesc= new ChangeDescription[paramTypes.length];
						for (int i= 0; i < nDiffs; i++) {
							changeDesc[idx1]= new SwapDescription(idx2);
						}
						IFunctionBinding methodDecl= methodBinding.getMethodDeclaration();
						ITypeBinding[] declParamTypes= methodDecl.getParameterTypes();

						ITypeBinding[] swappedTypes= new ITypeBinding[] { declParamTypes[idx1], declParamTypes[idx2] };
						String[] args=  new String[] { ASTResolving.getMethodSignature(methodDecl, !targetCU.equals(cu)), getTypeNames(swappedTypes) };
						String label;
						if (methodDecl.isConstructor()) {
							label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_swapparams_constr_description, args);
						} else {
							label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_swapparams_description, args);
						}
						Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
						ChangeMethodSignatureProposal proposal= new ChangeMethodSignatureProposal(label, targetCU, invocationNode, methodDecl, changeDesc, null, 5, image);
						proposals.add(proposal);
					}
				}
				return;
			}
		}

		if (declaringTypeDecl.isFromSource()) {
			IJavaScriptUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, declaringTypeDecl);
			if (targetCU != null) {
				ChangeDescription[] changeDesc= createSignatureChangeDescription(indexOfDiff, nDiffs, paramTypes, arguments, argTypes);
				if (changeDesc != null) {
				
					IFunctionBinding methodDecl= methodBinding.getMethodDeclaration();
					ITypeBinding[] declParamTypes= methodDecl.getParameterTypes();
	
					ITypeBinding[] newParamTypes= new ITypeBinding[changeDesc.length];
					for (int i= 0; i < newParamTypes.length; i++) {
						newParamTypes[i]= changeDesc[i] == null ? declParamTypes[i] : ((EditDescription) changeDesc[i]).type;
					}
					boolean isVarArgs= methodDecl.isVarargs() && newParamTypes.length > 0 && newParamTypes[newParamTypes.length - 1].isArray();
					String[] args=  new String[] { ASTResolving.getMethodSignature(methodDecl, !targetCU.equals(cu)), ASTResolving.getMethodSignature(methodDecl.getName(), newParamTypes, isVarArgs) };
					String label;
					if (methodDecl.isConstructor()) {
						label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changeparamsignature_constr_description, args);
					} else {
						label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changeparamsignature_description, args);
					}
					Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
					ChangeMethodSignatureProposal proposal= new ChangeMethodSignatureProposal(label, targetCU, invocationNode, methodDecl, changeDesc, null, 7, image);
					proposals.add(proposal);
				}
			}
		}
	}

	private static ChangeDescription[] createSignatureChangeDescription(int[] indexOfDiff, int nDiffs, ITypeBinding[] paramTypes, List arguments, ITypeBinding[] argTypes) {
		ChangeDescription[] changeDesc= new ChangeDescription[paramTypes.length];
		for (int i= 0; i < nDiffs; i++) {
			int diffIndex= indexOfDiff[i];
			Expression arg= (Expression) arguments.get(diffIndex);
			String name= arg instanceof SimpleName ? ((SimpleName) arg).getIdentifier() : null;
			ITypeBinding argType= argTypes[diffIndex];
			
			changeDesc[diffIndex]= new EditDescription(argType, name);
		}
		return changeDesc;
	}

	private static ITypeBinding[] getArgumentTypes(List arguments) {
		ITypeBinding[] res= new ITypeBinding[arguments.size()];
		for (int i= 0; i < res.length; i++) {
			Expression expression= (Expression) arguments.get(i);
 			ITypeBinding curr= expression.resolveTypeBinding();
			if (curr == null) {
				return null;
			}
			if (!curr.isNullType()) {	// don't normalize null type
				curr= Bindings.normalizeTypeBinding(curr);
				if (curr == null) {
					curr= expression.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
				}
			}
			res[i]= curr;
		}
		return res;
	}

	private static void addQualifierToOuterProposal(IInvocationContext context, FunctionInvocation invocationNode, IFunctionBinding binding, Collection proposals) throws CoreException {
		ITypeBinding declaringType= binding.getDeclaringClass();
		ITypeBinding parentType= Bindings.getBindingOfParentType(invocationNode);
		ITypeBinding currType= parentType;

		boolean isInstanceMethod= !Modifier.isStatic(binding.getModifiers());

		while (currType != null && !Bindings.isSuperType(declaringType, currType)) {
			if (isInstanceMethod && Modifier.isStatic(currType.getModifiers())) {
				return;
			}
			currType= currType.getDeclaringClass();
		}
		if (currType == null || currType == parentType) {
			return;
		}

		ASTRewrite rewrite= ASTRewrite.create(invocationNode.getAST());
		
		String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changetoouter_description, ASTResolving.getTypeSignature(currType));
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 8, image);
		
		ImportRewrite imports= proposal.createImportRewrite(context.getASTRoot());
		AST ast= invocationNode.getAST();

		String qualifier= imports.addImport(currType);
		Name name= ASTNodeFactory.newName(ast, qualifier);

		Expression newExpression;
		if (isInstanceMethod) {
			ThisExpression expr= ast.newThisExpression();
			expr.setQualifier(name);
			newExpression= expr;
		} else {
			newExpression= name;
		}

		rewrite.set(invocationNode, FunctionInvocation.EXPRESSION_PROPERTY, newExpression, null);

		proposals.add(proposal);
	}


	public static void getConstructorProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		IJavaScriptUnit cu= context.getCompilationUnit();

		JavaScriptUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (selectedNode == null) {
			return;
		}

		ITypeBinding targetBinding= null;
		List arguments= null;
		IFunctionBinding recursiveConstructor= null;

		int type= selectedNode.getNodeType();
		if (type == ASTNode.CLASS_INSTANCE_CREATION) {
			ClassInstanceCreation creation= (ClassInstanceCreation) selectedNode;

			IBinding binding= creation.getType().resolveBinding();
			if (binding instanceof ITypeBinding) {
				targetBinding= (ITypeBinding) binding;
				arguments= creation.arguments();
			}
		} else if (type == ASTNode.SUPER_CONSTRUCTOR_INVOCATION) {
			ITypeBinding typeBinding= Bindings.getBindingOfParentType(selectedNode);
			if (typeBinding != null && !typeBinding.isAnonymous()) {
				targetBinding= typeBinding.getSuperclass();
				arguments= ((SuperConstructorInvocation) selectedNode).arguments();
			}
		} else if (type == ASTNode.CONSTRUCTOR_INVOCATION) {
			ITypeBinding typeBinding= Bindings.getBindingOfParentType(selectedNode);
			if (typeBinding != null && !typeBinding.isAnonymous()) {
				targetBinding= typeBinding;
				arguments= ((ConstructorInvocation) selectedNode).arguments();
				recursiveConstructor= ASTResolving.findParentMethodDeclaration(selectedNode).resolveBinding();
			}
		}
		if (targetBinding == null) {
			return;
		}
		IFunctionBinding[] methods= targetBinding.getDeclaredMethods();
		ArrayList similarElements= new ArrayList();
		for (int i= 0; i < methods.length; i++) {
			IFunctionBinding curr= methods[i];
			if (curr.isConstructor() && recursiveConstructor != curr) {
				similarElements.add(curr); // similar elements can contain a implicit default constructor
			}
		}

		addParameterMissmatchProposals(context, problem, similarElements, selectedNode, arguments, proposals);

		if (targetBinding.isFromSource()) {
			ITypeBinding targetDecl= targetBinding.getTypeDeclaration();

			IJavaScriptUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, targetDecl);
			if (targetCU != null) {
				String[] args= new String[] { ASTResolving.getMethodSignature( ASTResolving.getTypeSignature(targetDecl), getParameterTypes(arguments), false) };
				String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createconstructor_description, args);
				Image image= JavaElementImageProvider.getDecoratedImage(JavaPluginImages.DESC_MISC_PUBLIC, JavaScriptElementImageDescriptor.CONSTRUCTOR, JavaElementImageProvider.SMALL_SIZE);
				proposals.add(new NewMethodCompletionProposal(label, targetCU, selectedNode, arguments, targetDecl, 5, image));
			}
		}
	}

	public static void getAmbiguosTypeReferenceProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		final IJavaScriptUnit cu= context.getCompilationUnit();
		int offset= problem.getOffset();
		int len= problem.getLength();

		IJavaScriptElement[] elements= cu.codeSelect(offset, len);
		for (int i= 0; i < elements.length; i++) {
			IJavaScriptElement curr= elements[i];
			if (curr instanceof IType && !TypeFilter.isFiltered((IType) curr)) {
				String qualifiedTypeName= JavaModelUtil.getFullyQualifiedName((IType) curr);

				JavaScriptUnit root= context.getASTRoot();
				
				String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_importexplicit_description, qualifiedTypeName);
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_IMPDECL);
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, ASTRewrite.create(root.getAST()), 5, image);

				ImportRewrite imports= proposal.createImportRewrite(root);
				imports.addImport(qualifiedTypeName);
				
				proposals.add(proposal);
			}
		}
	}

	public static void getArrayAccessProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {

		JavaScriptUnit root= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (!(selectedNode instanceof FunctionInvocation)) {
			return;
		}

		FunctionInvocation decl= (FunctionInvocation) selectedNode;
		SimpleName nameNode= decl.getName();
		String methodName= nameNode.getIdentifier();

		IBinding[] bindings= (new ScopeAnalyzer(root)).getDeclarationsInScope(nameNode, ScopeAnalyzer.METHODS);
		for (int i= 0; i < bindings.length; i++) {
			String currName= bindings[i].getName();
			if (NameMatcher.isSimilarName(methodName, currName)) {
				String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_arraychangetomethod_description, currName);
				proposals.add(new RenameNodeCompletionProposal(label, context.getCompilationUnit(), nameNode.getStartPosition(), nameNode.getLength(), currName, 6));
			}
		}
		// always suggest 'length'
		String lengthId= "length"; //$NON-NLS-1$
		String label= CorrectionMessages.UnresolvedElementsSubProcessor_arraychangetolength_description;
		int offset= nameNode.getStartPosition();
		int length= decl.getStartPosition() + decl.getLength() - offset;
		proposals.add(new RenameNodeCompletionProposal(label, context.getCompilationUnit(), offset, length, lengthId, 7));
	}

//	public static void getAnnotationMemberProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
//		JavaScriptUnit astRoot= context.getASTRoot();
//		IJavaScriptUnit cu= context.getCompilationUnit();
//		ASTNode selectedNode= problem.getCoveringNode(astRoot);
//
//		Annotation annotation;
//		String memberName;
//		if (selectedNode.getLocationInParent() == MemberValuePair.NAME_PROPERTY) {
//			if (selectedNode.getParent().getLocationInParent() != NormalAnnotation.VALUES_PROPERTY) {
//				return;
//			}
//			annotation= (Annotation) selectedNode.getParent().getParent();
//			memberName= ((SimpleName) selectedNode).getIdentifier();
//		} else if (selectedNode.getLocationInParent() == SingleMemberAnnotation.VALUE_PROPERTY) {
//			annotation= (Annotation) selectedNode.getParent();
//			memberName= "value"; //$NON-NLS-1$
//		} else {
//			return;
//		}
//		
//		ITypeBinding annotBinding= annotation.resolveTypeBinding();
//		if (annotBinding == null) {
//			return;
//		}
//
//		
//		if (annotation instanceof NormalAnnotation) {
//			// similar names
//			IFunctionBinding[] otherMembers= annotBinding.getDeclaredMethods();
//			for (int i= 0; i < otherMembers.length; i++) {
//				IFunctionBinding binding= otherMembers[i];
//				String curr= binding.getName();
//				int relevance= NameMatcher.isSimilarName(memberName, curr) ? 6 : 3;
//				String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_UnresolvedElementsSubProcessor_changetoattribute_description, curr);
//				proposals.add(new RenameNodeCompletionProposal(label, cu, problem.getOffset(), problem.getLength(), curr, relevance));
//			}
//		}
//		
//		if (annotBinding.isFromSource()) {
//			IJavaScriptUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, annotBinding);
//			if (targetCU != null) {
//				String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_UnresolvedElementsSubProcessor_createattribute_description, memberName);
//				Image image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
//				proposals.add(new NewAnnotationMemberProposal(label, targetCU, selectedNode, annotBinding, 5, image));
//			}
//		}
//	}


}
