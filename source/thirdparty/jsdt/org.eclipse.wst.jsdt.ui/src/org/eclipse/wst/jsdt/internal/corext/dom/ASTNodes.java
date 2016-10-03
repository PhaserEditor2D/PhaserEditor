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
 *       bug "inline method - doesn't handle implicit cast" (see
 *       https://bugs.eclipse.org/bugs/show_bug.cgi?id=24941).
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fix for
 *       bug Encapsulate field can fail when two variables in one variable declaration (see
 *       https://bugs.eclipse.org/bugs/show_bug.cgi?id=51540).
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.wst.jsdt.core.dom.ArrayType;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.DoStatement;
import org.eclipse.wst.jsdt.core.dom.EnhancedForStatement;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FieldDeclaration;
import org.eclipse.wst.jsdt.core.dom.ForInStatement;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.eclipse.wst.jsdt.core.dom.InfixExpression;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Message;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.PrimitiveType;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.QualifiedType;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SimpleType;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.VariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.WhileStatement;
import org.eclipse.wst.jsdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.preferences.MembersOrderPreferenceCache;
/**
*
* Provisional API: This class/interface is part of an interim API that is still under development and expected to
* change significantly before reaching stability. It is being made available at this early stage to solicit feedback
* from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
* (repeatedly) as the API evolves.
*/
public class ASTNodes {

	public static final int NODE_ONLY=				0;
	public static final int INCLUDE_FIRST_PARENT= 	1;
	public static final int INCLUDE_ALL_PARENTS= 	2;
	
	public static final int WARNING=				1 << 0;
	public static final int ERROR=					1 << 1;
	public static final int PROBLEMS=				WARNING | ERROR;

	private static final Message[] EMPTY_MESSAGES= new Message[0];
	private static final IProblem[] EMPTY_PROBLEMS= new IProblem[0];
	
	private static final int CLEAR_VISIBILITY= ~(Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE);
	
	
	private ASTNodes() {
		// no instance;
	}

	public static String asString(ASTNode node) {
		ASTFlattener flattener= new ASTFlattener();
		node.accept(flattener);
		return flattener.getResult();
	}
		
	public static String asFormattedString(ASTNode node, int indent, String lineDelim, Map options) {
		String unformatted= asString(node);
		TextEdit edit= CodeFormatterUtil.format2(node, unformatted, indent, lineDelim, options);
		if (edit != null) {
			return CodeFormatterUtil.evaluateFormatterEdit(unformatted, edit, null);
		}
		return unformatted; // unknown node
	}	

    /**
     * Returns the list that contains the given ASTNode. If the node
     * isn't part of any list, <code>null</code> is returned.
     * 
     * @param node the node in question 
     * @return the list that contains the node or <code>null</code>
     */
    public static List getContainingList(ASTNode node) {
    	StructuralPropertyDescriptor locationInParent= node.getLocationInParent();
    	if (locationInParent != null && locationInParent.isChildListProperty()) {
    		return (List) node.getParent().getStructuralProperty(locationInParent);
    	}
    	return null;
    }
    
	/**
	 * Returns a list of the direct children of a node. The siblings are ordered by start offset.
	 * @param node the node to get the children for
	 * @return the children
	 */    
	public static List getChildren(ASTNode node) {
		ChildrenCollector visitor= new ChildrenCollector();
		node.accept(visitor);
		return visitor.result;		
	}
	
	private static class ChildrenCollector extends GenericVisitor {
		public List result;

		public ChildrenCollector() {
			super(true);
			result= null;
		}
		protected boolean visitNode(ASTNode node) {
			if (result == null) { // first visitNode: on the node's parent: do nothing, return true
				result= new ArrayList();
				return true;
			}
			result.add(node);
			return false;
		}
	}
	
	/**
	 * Returns true if this is an existing node, i.e. it was created as part of
	 * a parsing process of a source code file. Returns false if this is a newly
	 * created node which has not yet been given a source position.
	 * 
	 * @param node the node to be tested.
	 * @return true if this is an existing node, false if not.
	 */
	public static boolean isExistingNode(ASTNode node) {
		return node.getStartPosition() != -1;
	}
	
	/**
	 * Returns the element type. This is a convenience method that returns its 
	 * argument if it is a simple type and the element type if the parameter is an array type.
	 * @param type The type to get the element type from.
	 * @return The element type of the type or the type itself.
	 */
	public static Type getElementType(Type type) {
		if (! type.isArrayType()) 
			return type;
		return ((ArrayType)type).getElementType();
	}
        
	public static ASTNode findDeclaration(IBinding binding, ASTNode root) {
		root= root.getRoot();
		if (root instanceof JavaScriptUnit) {
			return ((JavaScriptUnit)root).findDeclaringNode(binding);
		}
		return null;
	}
	
	public static VariableDeclaration findVariableDeclaration(IVariableBinding binding, ASTNode root) {
		if (binding.isField())
			return null;
		ASTNode result= findDeclaration(binding, root);
		if (result instanceof VariableDeclaration)
				return (VariableDeclaration)result;
				
		return null;
	}
	
	/**
	 * Returns the type node for the given declaration. 
	 * @param declaration the declaration
	 * @return the type node
	 */
	public static Type getType(VariableDeclaration declaration) {
		if (declaration instanceof SingleVariableDeclaration) {
			return ((SingleVariableDeclaration)declaration).getType();
		} else if (declaration instanceof VariableDeclarationFragment) {
			ASTNode parent= ((VariableDeclarationFragment)declaration).getParent();
			if (parent instanceof VariableDeclarationExpression)
				return ((VariableDeclarationExpression)parent).getType();
			else if (parent instanceof VariableDeclarationStatement)
				return ((VariableDeclarationStatement)parent).getType();
			else if (parent instanceof FieldDeclaration)
				return ((FieldDeclaration)parent).getType();
		}
		Assert.isTrue(false, "Unknown VariableDeclaration"); //$NON-NLS-1$
		return null;
	}
		
	public static int getDimensions(VariableDeclaration declaration) {
		int dim= declaration.getExtraDimensions();
		Type type= getType(declaration);
		if (type instanceof ArrayType) {
			dim += ((ArrayType) type).getDimensions();
		}
		return dim;
	}
		
	public static List getModifiers(VariableDeclaration declaration) {
		Assert.isNotNull(declaration);
		if (declaration instanceof SingleVariableDeclaration) {
			return ((SingleVariableDeclaration)declaration).modifiers();
		} else if (declaration instanceof VariableDeclarationFragment) {
			ASTNode parent= declaration.getParent();
			if (parent instanceof VariableDeclarationExpression)
				return ((VariableDeclarationExpression)parent).modifiers();
			else if (parent instanceof VariableDeclarationStatement)
				return ((VariableDeclarationStatement)parent).modifiers();
		}
		return new ArrayList(0);		
	}
	
	public static boolean isSingleDeclaration(VariableDeclaration declaration) {
		Assert.isNotNull(declaration);
		if (declaration instanceof SingleVariableDeclaration) {
			return true;
		} else if (declaration instanceof VariableDeclarationFragment) {
			ASTNode parent= declaration.getParent();
			if (parent instanceof VariableDeclarationExpression)
				return ((VariableDeclarationExpression)parent).fragments().size() == 1;
			else if (parent instanceof VariableDeclarationStatement)
				return ((VariableDeclarationStatement)parent).fragments().size() == 1;
		}
		return false;
	}
	
	public static boolean isLiteral(Expression expression) {
		int type= expression.getNodeType();
		return type == ASTNode.BOOLEAN_LITERAL || type == ASTNode.CHARACTER_LITERAL || type == ASTNode.NULL_LITERAL || 
			type == ASTNode.NUMBER_LITERAL || type == ASTNode.STRING_LITERAL || type == ASTNode.TYPE_LITERAL||
			type == ASTNode.UNDEFINED_LITERAL || type == ASTNode.OBJECT_LITERAL || type == ASTNode.REGULAR_EXPRESSION_LITERAL;

	}
	
	public static boolean isLabel(SimpleName name) {
		int parentType= name.getParent().getNodeType();
		return parentType == ASTNode.LABELED_STATEMENT || 
			parentType == ASTNode.BREAK_STATEMENT || parentType != ASTNode.CONTINUE_STATEMENT;
	}
	
	public static boolean isStatic(BodyDeclaration declaration) {
		return Modifier.isStatic(declaration.getModifiers());
	}
	
	public static List getBodyDeclarations(ASTNode node) {
		if (node instanceof AbstractTypeDeclaration) {
			return ((AbstractTypeDeclaration)node).bodyDeclarations();
		} else if (node instanceof AnonymousClassDeclaration) {
			return ((AnonymousClassDeclaration)node).bodyDeclarations();
		}
		// should not happen.
		Assert.isTrue(false); 
		return null;
	}
	
	public static ChildListPropertyDescriptor getBodyDeclarationsProperty(ASTNode node) {
		if (node instanceof JavaScriptUnit) {
			return JavaScriptUnit.STATEMENTS_PROPERTY;
		} else if (node instanceof AbstractTypeDeclaration) {
				return ((AbstractTypeDeclaration)node).getBodyDeclarationsProperty();
		} else if (node instanceof AnonymousClassDeclaration) {
			return AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY;
		}
		
		// should not happen.
		Assert.isTrue(false); 
		return null;
	}
	
	public static String getTypeName(Type type) {
		final StringBuffer buffer= new StringBuffer();
		ASTVisitor visitor= new ASTVisitor() {
			public boolean visit(PrimitiveType node) {
				buffer.append(node.getPrimitiveTypeCode().toString());
				return false;
			}
			public boolean visit(SimpleName node) {
				buffer.append(node.getIdentifier());
				return false;
			}
			public boolean visit(QualifiedName node) {
				buffer.append(node.getName().getIdentifier());
				return false;
			}
			public void endVisit(ArrayType node) {
				buffer.append("[]"); //$NON-NLS-1$
			}
		};
		type.accept(visitor);
		return buffer.toString();
	}
	
	public static InfixExpression.Operator convertToInfixOperator(Assignment.Operator operator) {
		if (operator.equals(Assignment.Operator.PLUS_ASSIGN))
			return InfixExpression.Operator.PLUS;
			
		if (operator.equals(Assignment.Operator.MINUS_ASSIGN))
			return InfixExpression.Operator.MINUS;
			
		if (operator.equals(Assignment.Operator.TIMES_ASSIGN))
			return InfixExpression.Operator.TIMES;
			
		if (operator.equals(Assignment.Operator.DIVIDE_ASSIGN))
			return InfixExpression.Operator.DIVIDE;
			
		if (operator.equals(Assignment.Operator.BIT_AND_ASSIGN))
			return InfixExpression.Operator.AND;
			
		if (operator.equals(Assignment.Operator.BIT_OR_ASSIGN))
			return InfixExpression.Operator.OR;
			
		if (operator.equals(Assignment.Operator.BIT_XOR_ASSIGN))
			return InfixExpression.Operator.XOR;
			
		if (operator.equals(Assignment.Operator.REMAINDER_ASSIGN))
			return InfixExpression.Operator.REMAINDER;
			
		if (operator.equals(Assignment.Operator.LEFT_SHIFT_ASSIGN))
			return InfixExpression.Operator.LEFT_SHIFT;
			
		if (operator.equals(Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN))
			return InfixExpression.Operator.RIGHT_SHIFT_SIGNED;
			
		if (operator.equals(Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN))
			return InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED;

		Assert.isTrue(false, "Cannot convert assignment operator"); //$NON-NLS-1$
		return null;			
	}
	
	/**
	 * Returns true if a node at a given location is a body of a control statement. Such body nodes are
	 * interesting as when replacing them, it has to be evaluates if a Block is needed instead.
	 * E.g. <code> if (x) do(); -> if (x) { do1(); do2() } </code>
	 *
	 * @param locationInParent Location of the body node
	 * @return Returns true if the location is a body node location of a control statement.
	 */
	public static boolean isControlStatementBody(StructuralPropertyDescriptor locationInParent) {
		return locationInParent == IfStatement.THEN_STATEMENT_PROPERTY
			|| locationInParent == IfStatement.ELSE_STATEMENT_PROPERTY
			|| locationInParent == ForStatement.BODY_PROPERTY
			|| locationInParent == ForInStatement.BODY_PROPERTY
			|| locationInParent == EnhancedForStatement.BODY_PROPERTY
			|| locationInParent == WhileStatement.BODY_PROPERTY
			|| locationInParent == DoStatement.BODY_PROPERTY;
	}
	
	public static boolean needsParentheses(Expression expression) {
		int type= expression.getNodeType();
		return type == ASTNode.INFIX_EXPRESSION || type == ASTNode.CONDITIONAL_EXPRESSION ||
			type == ASTNode.PREFIX_EXPRESSION || type == ASTNode.POSTFIX_EXPRESSION ||
			type == ASTNode.INSTANCEOF_EXPRESSION;
	}
	
	
	public static boolean substituteMustBeParenthesized(Expression substitute, Expression location) {
    	if (!needsParentheses(substitute))
    		return false;
    		
    	ASTNode parent= location.getParent();
    	if (parent instanceof VariableDeclarationFragment){
    		VariableDeclarationFragment vdf= (VariableDeclarationFragment)parent;
    		if (vdf.getInitializer().equals(location))
    			return false;
    	} else if (parent instanceof FunctionInvocation){
    		FunctionInvocation mi= (FunctionInvocation)parent;
    		if (mi.arguments().contains(location))
    			return false;
    	} else if (parent instanceof ReturnStatement)
    		return false;
    		
        return true;		
	}
	
	public static ASTNode getParent(ASTNode node, Class parentClass) {
		do {
			node= node.getParent();
		} while (node != null && !parentClass.isInstance(node));
		return node;
	}
	
	public static ASTNode getParent(ASTNode node, int nodeType) {
		do {
			node= node.getParent();
		} while (node != null && node.getNodeType() != nodeType);
		return node;
	}
	
	public static ASTNode findParent(ASTNode node, StructuralPropertyDescriptor[][] pathes) {
		for (int p= 0; p < pathes.length; p++) {
			StructuralPropertyDescriptor[] path= pathes[p];
			ASTNode current= node;
			int d= path.length - 1;
			for (; d >= 0 && current != null; d--) {
				StructuralPropertyDescriptor descriptor= path[d];
				if (!descriptor.equals(current.getLocationInParent()))
					break;
				current= current.getParent();
			}
			if (d < 0)
				return current;
		}
		return null;
	}
	
	public static ASTNode getNormalizedNode(ASTNode node) {
		ASTNode current= node;
		// normalize name
		if (QualifiedName.NAME_PROPERTY.equals(current.getLocationInParent())) {
			current= current.getParent();
		}
		// normalize type
		if (QualifiedType.NAME_PROPERTY.equals(current.getLocationInParent()) || 
			SimpleType.NAME_PROPERTY.equals(current.getLocationInParent())) {
			current= current.getParent();
		}
		return current;
	}
	
	public static boolean isParent(ASTNode node, ASTNode parent) {
		Assert.isNotNull(parent);
		do {
			node= node.getParent();
			if (node == parent)
				return true;
		} while (node != null);
		return false;
	}
	
	public static int getExclusiveEnd(ASTNode node){
		return node.getStartPosition() + node.getLength();
	}
	
	public static int getInclusiveEnd(ASTNode node){
		return node.getStartPosition() + node.getLength() - 1;
	}
	
	public static IFunctionBinding getMethodBinding(Name node) {
		IBinding binding= node.resolveBinding();
		if (binding instanceof IFunctionBinding)
			return (IFunctionBinding)binding;
		return null;
	}
	
	public static IVariableBinding getVariableBinding(Name node) {
		IBinding binding= node.resolveBinding();
		if (binding instanceof IVariableBinding)
			return (IVariableBinding)binding;
		return null;
	}
	
	public static IVariableBinding getLocalVariableBinding(Name node) {
		IVariableBinding result= getVariableBinding(node);
		if (result == null || result.isField())
			return null;
		
		return result;
	}
	
	public static IVariableBinding getFieldBinding(Name node) {
		IVariableBinding result= getVariableBinding(node);
		if (result == null || !result.isField())
			return null;
		
		return result;
	}
	
	public static ITypeBinding getTypeBinding(Name node) {
		IBinding binding= node.resolveBinding();
		if (binding instanceof ITypeBinding)
			return (ITypeBinding)binding;
		return null;
	}

	/**
	 * Returns the receiver's type binding of the given method invocation. 
	 * 
	 * @param invocation method invocation to resolve type of
	 * @return the type binding of the receiver
	 */
	public static ITypeBinding getReceiverTypeBinding(FunctionInvocation invocation) {
		ITypeBinding result= null;
		Expression exp= invocation.getExpression();
		if(exp != null) {
			return exp.resolveTypeBinding();
		}
		else {
			AbstractTypeDeclaration type= (AbstractTypeDeclaration)getParent(invocation, AbstractTypeDeclaration.class);
			if (type != null)
				return type.resolveBinding();
		}
		return result;
	}

	public static ITypeBinding getEnclosingType(ASTNode node) {
		while(node != null) {
			if (node instanceof AbstractTypeDeclaration) {
				return ((AbstractTypeDeclaration)node).resolveBinding();
			} else if (node instanceof AnonymousClassDeclaration) {
				return ((AnonymousClassDeclaration)node).resolveBinding();
			}  else if (node instanceof JavaScriptUnit) {
				return ((JavaScriptUnit)node).resolveBinding();
			}
			node= node.getParent();
		}
		return null;
	}

	public static IProblem[] getProblems(ASTNode node, int scope, int severity) {
		ASTNode root= node.getRoot();
		if (!(root instanceof JavaScriptUnit))
			return EMPTY_PROBLEMS;
		IProblem[] problems= ((JavaScriptUnit)root).getProblems();
		if (root == node)
			return problems;
		final int iterations= computeIterations(scope);
		List result= new ArrayList(5);
		for (int i= 0; i < problems.length; i++) {
			IProblem problem= problems[i];
			boolean consider= false;
			if ((severity & PROBLEMS) == PROBLEMS)
				consider= true;
			else if ((severity & WARNING) != 0)
				consider= problem.isWarning();
			else if ((severity & ERROR) != 0)
				consider= problem.isError();
			if (consider) {
				ASTNode temp= node;
				int count= iterations;
				do {
					int nodeOffset= temp.getStartPosition();
					int problemOffset= problem.getSourceStart();
					if (nodeOffset <= problemOffset && problemOffset < nodeOffset + temp.getLength()) {
						result.add(problem);
						count= 0;
					} else {
						count--;
					}
				} while ((temp= temp.getParent()) != null && count > 0);
			}
		}
		return (IProblem[]) result.toArray(new IProblem[result.size()]);
	}
	
	public static Message[] getMessages(ASTNode node, int flags) {
		ASTNode root= node.getRoot();
		if (!(root instanceof JavaScriptUnit))
			return EMPTY_MESSAGES;
		Message[] messages= ((JavaScriptUnit)root).getMessages();
		if (root == node)
			return messages;
		final int iterations= computeIterations(flags);
		List result= new ArrayList(5);
		for (int i= 0; i < messages.length; i++) {
			Message message= messages[i];
			ASTNode temp= node;
			int count= iterations;
			do {
				int nodeOffset= temp.getStartPosition();
				int messageOffset= message.getStartPosition();
				if (nodeOffset <= messageOffset && messageOffset < nodeOffset + temp.getLength()) {
					result.add(message);
					count= 0;
				} else {
					count--;
				}
			} while ((temp= temp.getParent()) != null && count > 0);
		}
		return (Message[]) result.toArray(new Message[result.size()]);
	}
	
	private static int computeIterations(int flags) {
		switch (flags) {
			case NODE_ONLY:
				return 1;
			case INCLUDE_ALL_PARENTS:
				return Integer.MAX_VALUE;
			case INCLUDE_FIRST_PARENT:
				return 2;
			default:
				return 1;
		}
	}
	
	
	private static int getOrderPreference(BodyDeclaration member, MembersOrderPreferenceCache store) {
		int memberType= member.getNodeType();
		int modifiers= member.getModifiers();

		switch (memberType) {
			case ASTNode.TYPE_DECLARATION:
				return store.getCategoryIndex(MembersOrderPreferenceCache.TYPE_INDEX) * 2;
			case ASTNode.FIELD_DECLARATION:
				if (Modifier.isStatic(modifiers)) {
					int index= store.getCategoryIndex(MembersOrderPreferenceCache.STATIC_FIELDS_INDEX) * 2;
					if (Modifier.isFinal(modifiers)) {
						return index; // first final static, then static
					}
					return index + 1;
				}
				return store.getCategoryIndex(MembersOrderPreferenceCache.FIELDS_INDEX) * 2;
			case ASTNode.INITIALIZER:
				if (Modifier.isStatic(modifiers)) {
					return store.getCategoryIndex(MembersOrderPreferenceCache.STATIC_INIT_INDEX) * 2;
				}
				return store.getCategoryIndex(MembersOrderPreferenceCache.INIT_INDEX) * 2;
			case ASTNode.FUNCTION_DECLARATION:
				if (Modifier.isStatic(modifiers)) {
					return store.getCategoryIndex(MembersOrderPreferenceCache.STATIC_METHODS_INDEX) * 2;
				}
				if (((FunctionDeclaration) member).isConstructor()) {
					return store.getCategoryIndex(MembersOrderPreferenceCache.CONSTRUCTORS_INDEX) * 2;
				}
				return store.getCategoryIndex(MembersOrderPreferenceCache.METHOD_INDEX) * 2;
			default:
				return 100;
		}
	}
			
	/**
	 * Computes the insertion index to be used to add the given member to the
	 * the list <code>container</code>.
	 * @param member the member to add
	 * @param container a list containing objects of type <code>BodyDeclaration</code>
	 * @return the insertion index to be used
	 */
	public static int getInsertionIndex(BodyDeclaration member, List container) {
		int containerSize= container.size();
		
		MembersOrderPreferenceCache orderStore= JavaScriptPlugin.getDefault().getMemberOrderPreferenceCache();
		
		int orderIndex= getOrderPreference(member, orderStore);
		
		int insertPos= containerSize;
		int insertPosOrderIndex= -1;

		for (int i= containerSize - 1; i >= 0; i--) {
			int currOrderIndex= getOrderPreference((BodyDeclaration) container.get(i), orderStore);
			if (orderIndex == currOrderIndex) {
				if (insertPosOrderIndex != orderIndex) { // no perfect match yet
					insertPos= i + 1; // after a same kind
					insertPosOrderIndex= orderIndex; // perfect match
				}
			} else if (insertPosOrderIndex != orderIndex) { // not yet a perfect match
				if (currOrderIndex < orderIndex) { // we are bigger
					if (insertPosOrderIndex == -1) {
						insertPos= i + 1; // after
						insertPosOrderIndex= currOrderIndex;
					}
				} else {
					insertPos= i; // before
					insertPosOrderIndex= currOrderIndex;
				}
			}
		}
		return insertPos;
	}

	public static SimpleName getLeftMostSimpleName(Name name) {
		if (name instanceof SimpleName) {
			return (SimpleName)name;
		} else {
			final SimpleName[] result= new SimpleName[1];
			ASTVisitor visitor= new ASTVisitor() {
				public boolean visit(QualifiedName qualifiedName) {
					Name left= qualifiedName.getQualifier();
					if (left instanceof SimpleName)
						result[0]= (SimpleName)left;
					else
						left.accept(this);
					return false;
				}
			};
			name.accept(visitor);
			return result[0];
		}
	}
	
	public static SimpleType getLeftMostSimpleType(QualifiedType type) {
		final SimpleType[] result= new SimpleType[1];
		ASTVisitor visitor= new ASTVisitor() {
			public boolean visit(QualifiedType qualifiedType) {
				Type left= qualifiedType.getQualifier();
				if (left instanceof SimpleType)
					result[0]= (SimpleType)left;
				else
					left.accept(this);
				return false;
			}
		};
		type.accept(visitor);
		return result[0];
	}
	
	public static Name getTopMostName(Name name) {
		Name result= name;
		while(result.getParent() instanceof Name) {
			result= (Name)result.getParent();
		}
		return result;
	}
	
	public static Type getTopMostType(Type type) {
		Type result= type;
		while(result.getParent() instanceof Type) {
			result= (Type)result.getParent();
		}
		return result;
	}
	
	public static int changeVisibility(int modifiers, int visibility) {
		return (modifiers & CLEAR_VISIBILITY) | visibility;
	}
	
	/**
	 * Adds flags to the given node and all its descendants.
	 * @param root The root node
	 * @param flags The flags to set
	 */
	public static void setFlagsToAST(ASTNode root, final int flags) {
		root.accept(new GenericVisitor(true) {
			protected boolean visitNode(ASTNode node) {
				node.setFlags(node.getFlags() | flags);
				return true;
			}
		});
	}

	public static String getQualifier(Name name) {
		if (name.isQualifiedName()) {
			return ((QualifiedName) name).getQualifier().getFullyQualifiedName();
		}
		return ""; //$NON-NLS-1$
	}

	public static String getSimpleNameIdentifier(Name name) {
		if (name.isQualifiedName()) {
			return ((QualifiedName) name).getName().getIdentifier();
		} else {
			return ((SimpleName) name).getIdentifier();
		}
	}

	public static boolean isDeclaration(Name name) {
		if (name.isQualifiedName()) {
			return ((QualifiedName) name).getName().isDeclaration();
		} else {
			return ((SimpleName) name).isDeclaration();
		}
	}

	public static Modifier findModifierNode(int flag, List modifiers) {
		for (int i= 0; i < modifiers.size(); i++) {
			Object curr= modifiers.get(i);
			if (curr instanceof Modifier && ((Modifier) curr).getKeyword().toFlagValue() == flag) {
				return (Modifier) curr;
			}
		}
		return null;
	}

	public static ITypeBinding getTypeBinding(JavaScriptUnit root, IType type) throws JavaScriptModelException {
		if (type.isAnonymous()) {
				final ClassInstanceCreation creation= (ClassInstanceCreation) getParent(NodeFinder.perform(root, type.getNameRange()), ClassInstanceCreation.class);
				if (creation != null)
					return creation.resolveTypeBinding();
		} else {
			final AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) getParent(NodeFinder.perform(root, type.getNameRange()), AbstractTypeDeclaration.class);
			if (declaration != null)
				return declaration.resolveBinding();
		}
		return null;
	}
}
