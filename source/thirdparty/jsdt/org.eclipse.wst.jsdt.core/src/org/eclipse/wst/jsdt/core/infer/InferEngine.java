/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.infer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.wst.jsdt.core.ast.ASTVisitor;
import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.IAbstractFunctionDeclaration;
import org.eclipse.wst.jsdt.core.ast.IAbstractVariableDeclaration;
import org.eclipse.wst.jsdt.core.ast.IAllocationExpression;
import org.eclipse.wst.jsdt.core.ast.IArgument;
import org.eclipse.wst.jsdt.core.ast.IAssignment;
import org.eclipse.wst.jsdt.core.ast.IBinaryExpression;
import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.ast.IFalseLiteral;
import org.eclipse.wst.jsdt.core.ast.IFieldReference;
import org.eclipse.wst.jsdt.core.ast.IFunctionCall;
import org.eclipse.wst.jsdt.core.ast.IFunctionDeclaration;
import org.eclipse.wst.jsdt.core.ast.IFunctionExpression;
import org.eclipse.wst.jsdt.core.ast.IJsDoc;
import org.eclipse.wst.jsdt.core.ast.ILocalDeclaration;
import org.eclipse.wst.jsdt.core.ast.INumberLiteral;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteral;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteralField;
import org.eclipse.wst.jsdt.core.ast.IProgramElement;
import org.eclipse.wst.jsdt.core.ast.IReference;
import org.eclipse.wst.jsdt.core.ast.IReturnStatement;
import org.eclipse.wst.jsdt.core.ast.IScriptFileDeclaration;
import org.eclipse.wst.jsdt.core.ast.ISingleNameReference;
import org.eclipse.wst.jsdt.core.ast.IStringLiteral;
import org.eclipse.wst.jsdt.core.ast.IThisReference;
import org.eclipse.wst.jsdt.core.ast.ITrueLiteral;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.Assignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.FunctionExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Javadoc;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocSingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds;
import org.eclipse.wst.jsdt.internal.compiler.ast.Reference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.StringLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.ThisReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.wst.jsdt.internal.compiler.util.Util;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IIndexConstants;

/**
 * The default inference engine.
 * 
 * <p>
 * Clients may subclass this class but should expect some breakage by future
 * releases.
 * </p>
 * 
 * Provisional API: This class/interface is part of an interim API that is
 * still under development and expected to change significantly before
 * reaching stability. It is being made available at this early stage to
 * solicit feedback from pioneering adopters on the understanding that any
 * code that uses this API will almost certainly be broken (repeatedly) as the
 * API evolves.
 */
public class InferEngine extends ASTVisitor implements IInferEngine {

	/**
	 * <p>
	 * String type that is initialized on first use and added to the
	 * compilation unit.
	 * </p>
	 */
	private InferredType fStringType;

	/**
	 * <p>
	 * Number type that is initialized on first use and added to the
	 * compilation unit.
	 * </p>
	 */
	private InferredType fNumberType;

	/**
	 * <p>
	 * Boolean type that is initialized on first use and added to the
	 * compilation unit.
	 * </p>
	 */
	private InferredType fBooleanType;

	/**
	 * <p>
	 * Function type that is initialized on first use and added to the
	 * compilation unit.
	 * </p>
	 */
	private InferredType fFunctionType;

	/**
	 * <p>
	 * Array type that is initialized on first use and added to the
	 * compilation unit.
	 * </p>
	 */
	private InferredType fArrayType;

	/**
	 * <p>
	 * Void type that is initialized on first use and added to the compilation
	 * unit.
	 * </p>
	 */
	private InferredType fVoidType;

	/**
	 * <p>
	 * Object type that is initialized on first use and added to the
	 * compilation unit.
	 * </p>
	 */
	private InferredType fObjectType;

	InferOptions inferOptions;
	CompilationUnitDeclaration compUnit;
	Context[] contexts = new Context[100];
	int contextPtr = -1;
	Context currentContext = new Context();
	protected int passNumber = 1;

	boolean isTopLevelAnonymousFunction;
	int anonymousCount = 0;

	public static boolean DEBUG = false;

	public InferrenceProvider inferenceProvider;

	/**
	 * @deprecated use {@link #getStringType()}
	 */
	public InferredType StringType = new InferredType(TypeConstants.JAVA_LANG_STRING[0]);

	/**
	 * @deprecated use {@link #getNumberType()}
	 */
	public InferredType NumberType = new InferredType(TypeConstants.NUMBER[0]);

	/**
	 * @deprecated use {@link #getBooleanType()}
	 */
	public InferredType BooleanType = new InferredType(TypeConstants.BOOLEAN_OBJECT[0]);

	/**
	 * @deprecated use {@link #getFunctionType()}
	 */
	public InferredType FunctionType = new InferredType(TypeConstants.FUNCTION[0]);

	/**
	 * @deprecated use {@link #getArrayType()}
	 */
	public InferredType ArrayType = new InferredType(TypeConstants.ARRAY[0]);

	/**
	 * @deprecated use {@link #getVoidType()}
	 */
	public InferredType VoidType = new InferredType(TypeConstants.VOID);

	/**
	 * @deprecated use {@link #getObjectType()}
	 */
	public InferredType ObjectType = new InferredType(TypeConstants.OBJECT);

	/**
	 * @deprecated - no longer used
	 */
	public InferredType GlobalType = new InferredType(InferredType.GLOBAL_NAME);

	public static HashtableOfObject WellKnownTypes = new HashtableOfObject();
	{
		WellKnownTypes.put(TypeConstants.OBJECT, null);
		WellKnownTypes.put(TypeConstants.ARRAY[0], null);
		WellKnownTypes.put(TypeConstants.JAVA_LANG_STRING[0], null);
		WellKnownTypes.put(TypeConstants.NUMBER[0], null);
		WellKnownTypes.put(TypeConstants.BOOLEAN_OBJECT[0], null);
		WellKnownTypes.put(TypeConstants.FUNCTION[0], null);
		WellKnownTypes.put(new char[]{'D', 'a', 't', 'e'}, null);
		WellKnownTypes.put(new char[]{'M', 'a', 't', 'h'}, null);
		WellKnownTypes.put(new char[]{'R', 'e', 'g', 'E', 'x', 'p'}, null);
		WellKnownTypes.put(new char[]{'E', 'r', 'r', 'o', 'r'}, null);
	}

	protected InferredType inferredGlobal = null;

	/**
	 * @deprecated Will be removed
	 */
	static final char[] CONSTRUCTOR_ID = {'c', 'o', 'n', 's', 't', 'r', 'u', 'c', 't', 'o', 'r'};

	/**
	 * <p>
	 * Use to keep track of the current context of the infer engine.
	 * </p>
	 */
	static class Context {
		InferredType currentType;
		IFunctionDeclaration currentMethod;

		/** The current assignment. */
		IAssignment currentAssignment;

		/** the current declaration */
		ILocalDeclaration currentLocalDeclaration;

		/** The current return */
		IReturnStatement currentReturn;

		boolean isJsDocClass;

		private HashtableOfObject definedMembers;

		/*
		 * Parent context to provide chaining when searching for members in
		 * scope.
		 */
		private Context parent = null;

		/* Root context */
		Context() {
		}

		/* Nested context */
		Context(Context parent) {
			this.parent = parent;

			currentType = parent.currentType;
			currentMethod = parent.currentMethod;
			this.currentAssignment = parent.currentAssignment;
			this.currentLocalDeclaration = parent.currentLocalDeclaration;
			this.currentReturn = parent.currentReturn;
			this.isJsDocClass = parent.isJsDocClass;
		}

		public Object getMember(char[] key) {
			Object value = null;
			if (definedMembers != null) {
				value = definedMembers.get(key);
			}

			// chain lookup
			if (value == null && parent != null) {
				value = parent.getMember(key);
			}

			return value;
		}

		public void addMember(char[] key, Object member) {
			if (key == null)
				return;

			if (definedMembers == null) {
				definedMembers = new HashtableOfObject();
			}

			definedMembers.put(key, member);
		}

		public void setCurrentType(InferredType type) {
			this.currentType = type;
			Context parentContext = this.parent;

			while (parentContext != null && parentContext.currentMethod == this.currentMethod) {
				parentContext.currentType = type;
				parentContext = parentContext.parent;
			}
		}
	}

	private static boolean REPORT_INFER_TIME = false;

	/**
	 * <p>
	 * Constructor that uses default {@link InferOptions}.
	 * </p>
	 */
	public InferEngine() {
		this(new InferOptions());
	}

	/**
	 * <p>
	 * Constructor using given {@link InferOptions}.
	 * </p>
	 * 
	 * @param inferOptions
	 *            to create this infer engine with
	 */
	public InferEngine(InferOptions inferOptions) {
		this.inferOptions = inferOptions;
	}

	public void initialize() {
		this.contextPtr = -1;
		this.currentContext = new Context();
		this.passNumber = 1;
		this.isTopLevelAnonymousFunction = false;
		this.anonymousCount = 0;
		this.inferredGlobal = null;
	}

	public void setCompilationUnit(CompilationUnitDeclaration scriptFileDeclaration) {
		this.compUnit = scriptFileDeclaration;
		buildDefinedMembers(scriptFileDeclaration.getStatements(), null);
	}

	public boolean visit(IFunctionCall functionCall) {
		boolean visitChildren = handleFunctionCall(functionCall);
		if (visitChildren) {
			if (functionCall.getReceiver() instanceof FunctionExpression) {
				if (this.contextPtr == -1) {
					this.isTopLevelAnonymousFunction = true;
				}
				if (functionCall instanceof MessageSend && ((MessageSend) functionCall).getArguments() != null) {
					MethodDeclaration methodDeclaration = ((FunctionExpression) functionCall.getReceiver()).getMethodDeclaration();
					if (methodDeclaration != null && methodDeclaration.getArguments() != null) {
						IArgument[] declaredArguments = methodDeclaration.getArguments();
						IExpression[] sentArguments = ((MessageSend) functionCall).getArguments();
						for (int i = 0; i < declaredArguments.length; i++) {
							if (i >= sentArguments.length) {
								continue;
							}
							handleFunctionDeclarationArgument(declaredArguments[i], sentArguments[i]);
						}
					}
				}
			}
		}
		return visitChildren;
	}

	protected void handleFunctionDeclarationArgument(IArgument declaredArgument, IExpression sentArgument) {
		// set the declared argument's type to be the matching parameter type
		if (!declaredArgument.isType() && declaredArgument.getInferredType() == null) {
			if (sentArgument instanceof SingleNameReference) {
				InferredType inferredType = getInferredType(sentArgument);
				if (inferredType == null) {
					char[] parameterName = getName(sentArgument);
					if (parameterName != null && isGlobal(parameterName)) {
						inferredType = createAnonymousGlobalType(parameterName);
					}
				}
				declaredArgument.setInferredType(inferredType);
			}
			else if (sentArgument instanceof ThisReference) {
				// check if "this" refers to the global object
				if (this.isTopLevelAnonymousFunction) {
					char[] parameterName = declaredArgument.getName();
					if (parameterName != null) {
						InferredType inferredType = createAnonymousGlobalType(parameterName);
						declaredArgument.setInferredType(inferredType);
					}
				}
			}
		}
	}

	public boolean visit(ILocalDeclaration localDeclaration) {
		// add as a member of the current context
		currentContext.addMember(localDeclaration.getName(), localDeclaration);

		// create a new context for the local declaration
		pushContext();
		this.currentContext.currentLocalDeclaration = localDeclaration;

		if (this.passNumber == 1 && localDeclaration instanceof LocalDeclaration && this.currentContext.currentMethod == null) {
			((LocalDeclaration) localDeclaration).setIsLocal(false);
		}

		if (localDeclaration.getJsDoc() != null) {
			Javadoc javadoc = (Javadoc) localDeclaration.getJsDoc();
			createTypeIfNecessary(javadoc);
			InferredAttribute attribute = null;
			if (javadoc.memberOf != null) {
				InferredType type = this.addType(javadoc.memberOf.getFullTypeName(), true);
				int nameStart = localDeclaration.sourceStart();
				attribute = type.addAttribute(localDeclaration.getName(), localDeclaration, nameStart);
				handleAttributeDeclaration(attribute, localDeclaration.getInitialization());
				if (localDeclaration.getInitialization() != null) {
					attribute.initializationStart = localDeclaration.getInitialization().sourceStart();
					attribute.type = getTypeOf(localDeclaration.getInitialization());
				}
				attribute.inType = type;
			}

			if (javadoc.returnType != null) {
				InferredType type = this.addType(changePrimitiveToObject(javadoc.returnType.getFullTypeName()));
				localDeclaration.setInferredType(type);
				if (attribute != null)
					attribute.type = type;
			}
		}

		// visit the function in case it defines a type
		if (localDeclaration.getInitialization() instanceof IFunctionExpression) {
			boolean keepVisiting = handleFunctionExpressionLocalDeclaration(localDeclaration);
			if (!keepVisiting) {
				return false;
			}
		}

		// if initialization set, attempt to set the inferred type
		if (localDeclaration.getInitialization() != null) {
			if (localDeclaration.getInitialization() instanceof MessageSend) {
				handleFunctionCall((IFunctionCall) localDeclaration.getInitialization(), (LocalDeclaration) localDeclaration);
				if (((MessageSend) localDeclaration.getInitialization()).receiver instanceof IFunctionExpression && this.passNumber == 2) {
					if (((FunctionExpression) ((MessageSend) localDeclaration.getInitialization()).receiver).methodDeclaration != null) {
						localDeclaration.setInferredType(((FunctionExpression) ((MessageSend) localDeclaration.getInitialization()).receiver).methodDeclaration.inferredType);
					}
				}
			}
			else {
				if (this.isExpressionAType(localDeclaration.getInitialization())) {
					localDeclaration.setIsType(true);
					handleLocalDeclarationExpressionType(localDeclaration);
				}
				InferredType type = this.getTypeForVariableInitialization(localDeclaration.getName(), localDeclaration.getInitialization());
				if (localDeclaration.getInferredType() == null || (type != null && type.isAnonymous))
					localDeclaration.setInferredType(type);
			}
		}

		return true;
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.ast.ASTVisitor#endVisit(org.eclipse.wst.jsdt.core.ast.ILocalDeclaration)
	 */
	public void endVisit(ILocalDeclaration localDeclaration) {
		popContext();
	}

	private void createTypeIfNecessary(Javadoc javadoc) {
		if (javadoc.memberOf != null) {
			char[][] namespace = {};
			char[][] typeName = javadoc.memberOf.getTypeName();
			if (javadoc.namespace != null) {
				namespace = javadoc.namespace.getTypeName();
			}
			char[] name = CharOperation.concat(CharOperation.concatWith(namespace, '.'), CharOperation.concatWith(typeName, '.'), '.');
			this.currentContext.currentType = addType(name);
			if (javadoc.extendsType != null) {
				char[] superName = CharOperation.concatWith(javadoc.extendsType.getTypeName(), '.');
				this.currentContext.currentType.setSuperType(addType(superName));
			}
			this.currentContext.isJsDocClass = true;

		}

	}

	public boolean visit(IAssignment assignment) {
		//if assigning to single name add assignment to context if there is not an existing var declaration
		IAbstractVariableDeclaration existingVarDecl = null;
		IAssignment existingAssignmentDecl = null;
		if(assignment.getLeftHandSide() instanceof ISingleNameReference) {
			existingVarDecl = this.getVariable(assignment.getLeftHandSide());
			if(existingVarDecl == null) {
				existingAssignmentDecl = this.getAssignment(assignment.getLeftHandSide());
				if(existingAssignmentDecl == null) {
					currentContext.addMember(this.getName(assignment.getLeftHandSide()), assignment);
				}
			}
		}
		
		pushContext();
		this.currentContext.currentAssignment = assignment;
		
		//set the function that contains this assignment
		if(this.passNumber == 1 && assignment instanceof Assignment && this.currentContext.currentMethod != null) {
			((Assignment)assignment).setContainingFunction(this.currentContext.currentMethod);
		}
		
		IExpression assignmentExpression = assignment.getExpression();
		if(handlePotentialType(assignment)) {

		} else if(assignmentExpression instanceof FunctionExpression) {
			boolean keepVisiting = handleFunctionExpressionAssignment(assignment);
			
			//set the type on the existing var declaration if it does not already have one set
			if(assignment.getInferredType() != null && existingVarDecl != null && existingVarDecl.getInferredType() == null) {
				existingVarDecl.setInferredType(assignment.getInferredType());
			}
			
			if(!keepVisiting) {
				return false;
			}
		} else if(assignmentExpression instanceof SingleNameReference && this.currentContext.currentType != null
				&& isThis(assignment.getLeftHandSide())) {
			
			ISingleNameReference snr = (ISingleNameReference) assignmentExpression;
			Object object = this.currentContext.getMember(snr.getToken());

			IFieldReference fieldReference = (IFieldReference) assignment.getLeftHandSide();
			char[] memberName = fieldReference.getToken();
			InferredMember member = null;

			int nameStart = fieldReference.sourceEnd() - memberName.length + 1;

			/* this.foo = bar //bar is a function */
			if(object instanceof MethodDeclaration) {

				MethodDeclaration method = (MethodDeclaration) object;
				member = this.currentContext.currentType.addMethod(memberName, method, nameStart);

			}
			/* this.foo = bar //assume that bar is not a function and create a new attribute in the
			 * current type */
			else {
				member = this.currentContext.currentType.addAttribute(memberName, assignment, nameStart);
				handleAttributeDeclaration((InferredAttribute) member, assignment.getExpression());
				if(((InferredAttribute) member).type == null)
					((InferredAttribute) member).type = getTypeOf(assignmentExpression);
			}

			// setting location
			if(member != null) {
				// this is a not static member because it is being set on the this
				member.isStatic = false;
			}
		}

		// foo = ??;
		else if(assignment.getLeftHandSide() instanceof ISingleNameReference) {
			char[] variableName = this.getName(assignment);
			
			/* if there is an existing variable declaration
			 * else there is not */
			InferredType existingType = null;
			
			if(existingVarDecl != null) {
				existingType = existingVarDecl.getInferredType();
			} else if(existingAssignmentDecl != null) {
				existingType = existingAssignmentDecl.getInferredType();
			}
			
			/* if existing variable declaration does not already have an inferred type
			 * 
			 * else it does and the new assignment type is different then the currently
			 * assigned type */
			InferredType type = null;
			if(existingType == null) {
				type = this.getTypeForVariableInitialization(variableName, assignmentExpression);
				
				if(this.isExpressionAType(assignmentExpression)) {
					assignment.setIsType(true);
				}
			} else {
				InferredType newAssignmentType = this.getTypeOf(assignmentExpression);
				if(newAssignmentType != null && existingType != newAssignmentType) {
					/* if the existing type is not anonymous, not global and
					 * existing var is not an argument then create a new
					 * global anonymous type to mix everything together in
					 * 
					 * else just use the existing type */
					InferredType newCombinedType = null;
					if(!existingType.isAnonymous && !existingType.isGlobal() && !(existingVarDecl instanceof IArgument)) {
						if(existingVarDecl instanceof LocalDeclaration && ((LocalDeclaration)existingVarDecl).isLocal()) {
							newCombinedType = this.createAnonymousType(existingVarDecl, null);
						} else {
							newCombinedType = this.createAnonymousGlobalType(variableName);
						}
						newCombinedType.setIsDefinition(true);
						
						/* add the original type of the variable declaration to the new combined type
						 * 
						 * if the existing type is indexed then add it later
						 * else it is not must add it now */
						if(existingType.isIndexed()) {
							newCombinedType.addMixin(existingType.getName());
						} else {
							newCombinedType.mixin(existingType);
						}
					} else {
						newCombinedType = existingType;
					}
	
					/* add the type of the new assignment to the combined type
					 * 
					 * if the existing type is indexed then add it later
					 * else it is not must add it now */
					if(newAssignmentType.isIndexed()) {
						newCombinedType.addMixin(newAssignmentType.getName());
					} else {
						newCombinedType.mixin(newAssignmentType);
					}
					
					type = newCombinedType;
				}
			}
			
			/* use the new type to set the inferred type on this assignment and
			 * any existing variable declaration or assignment */
			if(type != null) {
				assignment.setInferredType(type);
				
				if(existingVarDecl != null) {
					existingVarDecl.setInferredType(type);
				}
				
				if(existingAssignmentDecl != null) {
					existingAssignmentDecl.setInferredType(type);
				}
			}

			return true;
		}
		else if(assignmentExpression instanceof AllocationExpression
				&& ((AllocationExpression) assignmentExpression).member instanceof FunctionExpression) {

			handleFunctionExpressionAssignment(assignment);
		} else if(assignmentExpression instanceof Assignment
				&& ((Assignment) assignmentExpression).expression instanceof FunctionExpression) {

			handleFunctionExpressionAssignment(assignment);
		}
		else if(this.inferOptions.useAssignments
					
					// arian
					
					// just allow those assigments in the JSDT defined libraries files, like phaser-api.js.
					
					&& new String(getScriptFileDeclaration().getFileName()).contains("org.eclipse.wst.jsdt.core")
					
					//
					
					) {
			
 			IExpression lhs = assignment.getLeftHandSide();
			
			// if foo.bar = ? where ? is not {} and not a function
			if(lhs instanceof FieldReference || lhs instanceof ArrayReference) {

				Reference lhsRef = (Reference) lhs;
				Expression receiver = null;
				char[] attName = null;
				int nameStart = 0;
				if(lhsRef instanceof FieldReference) {
					receiver = ((FieldReference) lhsRef).receiver;
					attName = ((FieldReference) lhsRef).token;
					nameStart = (int) (((FieldReference) lhsRef).nameSourcePosition >>> 32);
				} else if(lhsRef instanceof ArrayReference) {
					if(((ArrayReference) lhsRef).position instanceof StringLiteral) {
						receiver = ((ArrayReference) lhsRef).receiver;
						attName = ((StringLiteral) ((ArrayReference) lhsRef).position).source();
						nameStart = ((StringLiteral) ((ArrayReference) lhsRef).position).sourceStart + 1;
					}
				}

				//attempt to get receiver type
				InferredType receiverType = this.getInferredType(receiver);
				
				//if not found type yet check if receiver is function
				if(receiverType == null) {
					IFunctionDeclaration function = getDefinedFunction(receiver);
					if(function != null) {
						char[] typeName = constructTypeName(receiver);
						if(typeName != null) {
							receiverType = addType(typeName);
						}
					}
				}
				
				//all else fails on pass two will possibly create a receiver type
				if(receiverType == null && this.passNumber == 2) {
					receiverType = this.getReceiverType(receiver, true);
				}

				if(receiver != null && receiverType != null && attName != null && attName.length > 0) {
					/* if the receiver type is not anonymous and is the expression is not a type then
					 * create a new anonymous sub type to do the assignment to */
					if(!receiverType.isAnonymous && !this.isExpressionAType(receiver)) {
//						receiverType = this.createTypeToAssignTo(receiver, receiverType);
//						this.setTypeOf(receiver, receiverType);
					}
					
					// in the case where the supertype of the reciever is a function, make sure we store the actual function
					// for later use
					if(receiverType != null && receiverType.getSuperType() != null && receiverType.getSuperType().isFunction()) {
						IAbstractVariableDeclaration varDecl = this.getVariable(receiver);
						if(varDecl != null) {
							IExpression expression = varDecl.getInitialization();
							if(expression != null && expression instanceof IFunctionExpression) {
								receiverType.setCorrespondingFunction(((IFunctionExpression)expression).getMethodDeclaration());
							}
						}
					}
					
					/* if receiver is instance of type so create new type to assign to
					 * 
					 * else if receiver is a type then will just assign directly to it,
					 * and if not anonymous then its a static assignment that type statically */
					boolean isStatic = false;
					if(!this.isExpressionAType(receiver)) {
						receiverType = this.createTypeToAssignTo(receiver, receiverType);
					} else if(!receiverType.isAnonymous && !receiver.isThis()) {
						isStatic = true;
					}

					// check if there is an attribute or function already created
					InferredMethod method = null;
					InferredAttribute attr = receiverType.findAttribute(attName);
					if(attr == null) {
						method = receiverType.findMethod(attName, null);
					} else if (this.passNumber == 2) {
						handleAttributeDeclaration(attr);
					}

					// ignore if the attribute exists and has a type
					if((method == null && attr == null) || (method == null && attr != null && attr.type == null)) {
						//if type already set on assignment, use that
						InferredType rhsType = assignment.getInferredType();
						
						// If the RHS is a type then create a type on the LHS
						boolean isType = false;
						if(rhsType != null && assignment.getInferredType() != null && assignment.isType() &&
									this.isExpressionAType(receiver) && !receiverType.isAnonymous) {
							
							isType = true;
							
							//create new type name
							char[] newTypeName = receiverType.getName();
							newTypeName = CharOperation.concat(newTypeName, attName, '.');
							
							/* if the RHS type is anonymous just rename it to use the LHS name
							 * else create new type on the LHS and add it as a synonym of the RHS type */
							if(rhsType.isAnonymous && !rhsType.isGlobal()) {
								this.convertAnonymousTypeToNamed(rhsType, newTypeName);
								rhsType.setIsDefinition(true);
								rhsType.setNameStart(assignment.sourceStart());
							}
							else if(!CharOperation.equals(newTypeName, rhsType.getName())){
								InferredType newType = this.addType(newTypeName, true);
								rhsType.addSynonym(newType);
							}
						}
						
						//if type not found type yet, check if RHS refers to a function definition
						IFunctionDeclaration definedFunction = null;
						if(rhsType == null) {
							definedFunction = this.getDefinedFunction(assignmentExpression);
						}
						
						/* if RHS is a function, add a function to the receiver type
						 * else add new attribute to receiver type */
						if(definedFunction != null) {
							method = receiverType.addMethod(attName, definedFunction, nameStart);
							receiverType.setIsDefinition(true);
							method.isStatic = isStatic;
						} else {
							//create the attribute
							int nameStart_ = nameStart;
							attr = receiverType.addAttribute(attName, assignment, nameStart_);
							receiverType.setIsDefinition(true);
							this.handleAttributeDeclaration(attr, assignmentExpression);
							
							//if still not RHS type then get one
							if(rhsType == null) {
								/* if LHS is global "this"
								 * else just an attribute on a type */
								if(receiver instanceof IThisReference &&
											this.currentContext.currentType == this.getInferredGlobal(false)) {
									
									rhsType = this.getTypeForVariableInitialization(attName, assignmentExpression);
								} else {
									rhsType = this.getTypeOf(assignmentExpression);
									if(receiverType != null && rhsType != null && receiverType.isGlobal() && rhsType.isAnonymous) {
										char[] globalAttName = createAnonymousGlobalTypeName(attName);
										convertAnonymousTypeToNamed(rhsType, globalAttName);
									}
									// if the attribute is also a function, add the function using the corresponding function stored earlier
									if(rhsType != null && rhsType.getSuperType() != null && rhsType.getSuperType().isFunction() && rhsType.getCorrespondingFunction() != null) {
										method = receiverType.addMethod(attName, rhsType.getCorrespondingFunction(), nameStart);
										method.isStatic = isStatic;
									}
								}
							}
							
							//determine if static
							char[] possibleTypeName = constructTypeName(receiver);
							attr.isStatic = isStatic || (possibleTypeName != null && compUnit.findInferredType(possibleTypeName) != null);
							
							//assign the type to the attribute
							if(attr.type == null || rhsType != null) {
								attr.type = rhsType;
								attr.setIsType(isType);
							}
							
							//if determined RHS type set it as the inferred type for the assignment 
							if(rhsType != null) {
								assignment.setInferredType(rhsType);
							}
						}
					} else if(method == null && attr != null && attr.type != null && attr.type.getSuperType() != null && attr.type.getSuperType().isFunction() && attr.type.getCorrespondingFunction() != null) {
						// if the attribute is also a function, add the function using the cooresponding function stored earlier
						method = receiverType.addMethod(attName, attr.type.getCorrespondingFunction(), nameStart);
						receiverType.setIsDefinition(true);
						method.isStatic = isStatic;
					}
				}
			}
			// if foo = ? where ? is not {} and not a function
			else {
				// no inferred type already set, use the type of the RHS expression
				if(assignment.getInferredType() == null) {
					InferredType rhsType = this.getTypeOf(assignment.getExpression());
					assignment.setInferredType(rhsType);
				}
			}
			
			//only create global type for LHS if LHS's root is global
			if(this.isRootGlobal(lhs)) {
				// construct the LHS and RHS type names
				char[] lhsName = constructTypeName(assignment.getLeftHandSide());
				char[] rhsName = constructTypeName(assignment.getExpression());
	
				//if RHS type exists then create LHS type and add it as synonym of the RHS type
				if(lhsName != null && lhsName.length > 0 && rhsName != null && rhsName.length > 0) {
					InferredType rhsType = this.findDefinedType(rhsName);
					
					if(rhsType != null) {
						InferredType lhsType = this.addType(lhsName, true);
						lhsType.setNameStart(lhs.sourceStart());
						
						lhsType.addSynonym(rhsType);
					}
				}
			}
		}
		return true; // do nothing by default, keep traversing
	}

	protected InferredType createAnonymousType(char[] possibleTypeName, InferredType currentType) {
		char[] name;
		if (this.isKnownType(possibleTypeName)) {
			name = possibleTypeName;
		}
		else {
			char[] cs = String.valueOf(this.anonymousCount++).toCharArray();
			name = CharOperation.concat(ANONYMOUS_PREFIX, possibleTypeName, cs);
		}
		InferredType type = addType(name, true);
		type.isAnonymous = true;
		type.setIsGlobal(false);
		if (currentType != null) {
			type.setSuperType(currentType);
		}

		return type;
	}

	/**
	 * <p>
	 * Creates an anonymous type for a given node with an optional parent
	 * type.
	 * </p>
	 * 
	 * @param forNode
	 *            the node to create the anonymous type for, the text range of
	 *            this node will be used to create the anonymous type name
	 * @param parrentType
	 *            optional parent type of the new anonymous type
	 * 
	 * @return a new anonymous type
	 */
	protected InferredType createAnonymousType(IASTNode forNode, InferredType parrentType) {
		char[] name = createAnonymousTypeName(forNode);
		InferredType type = addType(name, true);
		type.isAnonymous = true;
		type.setIsGlobal(false);
		if (parrentType != null) {
			type.setSuperType(parrentType);
		}
		return type;
	}

	/**
	 * @deprecated - here for compatibility
	 */
	private InferredType createAnonymousType(IAbstractVariableDeclaration var) {

		InferredType currentType = var.getInferredType();

		if (currentType == null || !currentType.isAnonymous) {
			InferredType type = createAnonymousType(var, currentType);
			var.setInferredType(type);
		}
		return var.getInferredType();
	}

	/*
	 * Creates an anonymous type based in the location in the document. This
	 * information is used to avoid creating duplicates because of the 2-pass
	 * nature of this engine.
	 */
	private InferredType createAnonymousType(IObjectLiteral objLit) {
		InferredType anonType = objLit.getInferredType();
		if (anonType == null) {

			char[] name = createAnonymousTypeName(objLit);
			anonType = addType(name, true);
			anonType.isAnonymous = true;
			anonType.isObjectLiteral = true;
			anonType.setSuperType(this.getObjectType());
			anonType.setIsGlobal(false);

			anonType.sourceStart = objLit.sourceStart();
			anonType.sourceEnd = objLit.sourceEnd();
		}

		populateType(anonType, objLit, false);

		return anonType;
	}

	/**
	 * <p>
	 * Creates a global anonymous type.
	 * </p>
	 * 
	 * @param varName
	 *            name of the global variable to create the global anonymous
	 *            type for
	 * 
	 * @return a global anonymous type created from for the given global
	 *         variable name
	 */
	protected InferredType createAnonymousGlobalType(char[] varName) {
		char[] name = createAnonymousGlobalTypeName(varName);
		InferredType globalType = this.compUnit.findInferredType(name);

		if (globalType == null) {
			globalType = this.addType(name, false);
			globalType.isAnonymous = true;
			globalType.isObjectLiteral = true;
			globalType.setSuperType(this.getObjectType());
			globalType.setIsGlobal(true);
		}

		return globalType;
	}

	/**
	 * <p>
	 * Creates an anonymous type name for the given {@link IASTNode}
	 * </p>
	 * 
	 * @param node
	 *            create the anonymous type name off the location of this node
	 * @return an anonymous type name based off the given nodes location
	 */
	protected static char[] createAnonymousTypeName(IASTNode node) {
		char[] loc = (String.valueOf(node.sourceStart()) + '_' + String.valueOf(node.sourceEnd())).toCharArray();
		return CharOperation.concat(ANONYMOUS_PREFIX, ANONYMOUS_CLASS_ID, loc);
	}

	/**
	 * <p>
	 * Creates an anonymous type name from the given variable name.
	 * </p>
	 * 
	 * @param varName
	 *            to use when creating the anonymous type name
	 * @return
	 */
	public static char[] createAnonymousGlobalTypeName(char[] varName) {
		return CharOperation.concat(CharOperation.concat(ANONYMOUS_PREFIX, ANONYMOUS_CLASS_ID), varName, '_');
	}

	/**
	 * handle the inferencing for an assignment whose right hand side is a
	 * function expression
	 * 
	 * @param the
	 *            assignment AST node
	 * @return true if handled
	 */
	protected boolean handleFunctionExpressionAssignment(IAssignment assignment) {
		IFunctionExpression functionExpression = null;
		if (assignment.getExpression() instanceof IFunctionExpression) {
			functionExpression = (IFunctionExpression) assignment.getExpression();
		}
		else if (assignment.getExpression() instanceof IAllocationExpression) {
			functionExpression = (IFunctionExpression) ((IAllocationExpression) assignment.getExpression()).getMember();
		}
		else if (assignment.getExpression() instanceof IAssignment) {
			functionExpression = (FunctionExpression) ((IAssignment) assignment.getExpression()).getExpression();
		}

		if (functionExpression == null) {
			return false;
		}

		MethodDeclaration methodDeclaration = functionExpression.getMethodDeclaration();

		char[] possibleTypeName = constructTypeName(assignment.getLeftHandSide());

		InferredType type = null;
		if (possibleTypeName != null) {
			type = compUnit.findInferredType(possibleTypeName);
			if (type == null && isPossibleClassName(possibleTypeName)) {
				type = addType(possibleTypeName, true);
			}
			if (type == null && methodDeclaration.getJsDoc() != null && ((Javadoc) methodDeclaration.getJsDoc()).isConstructor) {
				type = addType(possibleTypeName, true);
				handleJSDocConstructor(type, methodDeclaration, assignment.sourceStart());
			}
		}

		// isConstructor
		if (type != null) {
			if (this.inferOptions.useInitMethod) {
				this.currentContext.currentType = type;
				int nameStart = assignment.getLeftHandSide().sourceStart();
				int expressionEnd = assignment.getExpression().sourceEnd();
				handleConstructor(type, methodDeclaration, nameStart, expressionEnd);

				// want to be sure to set the type of the assignment and local
				// declaration if there is one
				assignment.setInferredType(type);
				if (this.currentContext.currentLocalDeclaration != null && CharOperation.equals(this.currentContext.currentLocalDeclaration.getName(), getName(assignment))) {
					this.currentContext.currentLocalDeclaration.setInferredType(type);
				}

				// constructor is actually an anonymous function assigned to a
				// single name
				methodDeclaration.setIsAnonymous(true);
			}
		}
		else {// could be method
			if (assignment.getLeftHandSide() instanceof FieldReference || assignment.getLeftHandSide() instanceof ArrayReference) {

				Reference ref = (Reference) assignment.getLeftHandSide();
				Expression receiver = null;
				char[] methodName = null;
				int nameStart = 0;
				if (ref instanceof FieldReference) {
					receiver = ((FieldReference) ref).receiver;
					methodName = ((FieldReference) ref).token;
					nameStart = (int) (((FieldReference) ref).nameSourcePosition >>> 32);
				}
				else if (ref instanceof ArrayReference) {
					if (((ArrayReference) ref).position instanceof StringLiteral) {
						receiver = ((ArrayReference) ref).receiver;
						methodName = ((StringLiteral) ((ArrayReference) ref).position).source();
						nameStart = ((StringLiteral) ((ArrayReference) ref).position).sourceStart + 1;
					}
				}

				// if no receiver then done
				if (receiver == null) {
					return false;
				}

				InferredType receiverType = getInferredType(receiver);
				if (receiverType == null && passNumber == 2) {
					receiverType = this.getReceiverType(receiver, true);
				}

				if (receiverType != null && methodName != null) {
					if (!receiver.isThis()) {
						receiverType = this.createTypeToAssignTo(receiver, receiverType);
					}

					// check if there is a member method already created
					InferredMethod method = receiverType.findMethod(methodName, methodDeclaration);

					if (method == null) {
						// create member method if it does not exist
						method = receiverType.addMethod(methodName, methodDeclaration, nameStart);
						receiverType.setIsDefinition(true);

						/*
						 * determine if static check if the receiver is a type
						 */
						char[] possibleInTypeName = constructTypeName(receiver);
						method.isStatic = (possibleInTypeName != null && compUnit.findInferredType(possibleInTypeName) != null);

						return true; // keep visiting to get return type
					}
					else if (this.passNumber == 2) {
						return false; // no need to visit again
					}

				}
				else if (this.passNumber == 2 && methodName != null) { // create
																		// anonymous
																		// class
					receiverType = this.getReceiverType(receiver, false);
					if (receiverType != null) {
						InferredMethod method = receiverType.addMethod(methodName, methodDeclaration, nameStart);
						method.isStatic = !receiverType.isObjectLiteral;
						receiverType.updatePositions(assignment.sourceStart(), assignment.sourceEnd());
					}
				}
			}
			else if (assignment.getLeftHandSide() instanceof SingleNameReference && this.passNumber == 2) {
				// set the inferred type
				assignment.setInferredType(getTypeOf(assignment.getExpression()));

				methodDeclaration.setIsAnonymous(true);
				methodDeclaration.setSelector(((SingleNameReference) assignment.getLeftHandSide()).token);
				methodDeclaration.sourceStart = (((SingleNameReference) assignment.getLeftHandSide()).sourceStart());
			}
		}
		return true;
	}

	/**
	 * <p>
	 * Handle a local declaration who's right hand side is a function.
	 * </p>
	 * <p>
	 * Use case:
	 * </p>
	 * 
	 * <pre>
	 * foo.bar.Test = function() { this.num = 42; }
	 * </pre>
	 * 
	 * @param localDeclaration
	 *            {@link ILocalDeclaration} to attempt to infer a type from
	 * @return <code>true</code> if keep visiting, <code>false</code>
	 *         otherwise.
	 */
	private boolean handleFunctionExpressionLocalDeclaration(ILocalDeclaration localDeclaration) {
		boolean keepVisiting = true;
		IFunctionExpression functionExpression = null;
		IExpression expression = localDeclaration.getInitialization();
		if (expression instanceof IFunctionExpression) {
			functionExpression = (IFunctionExpression) expression;
		}
		else if (expression instanceof IAllocationExpression) {
			functionExpression = (IFunctionExpression) ((IAllocationExpression) expression).getMember();
		}
		else if (expression instanceof IAssignment) {
			functionExpression = (FunctionExpression) ((IAssignment) expression).getExpression();
		}

		if (functionExpression == null) {
			return false;
		}

		MethodDeclaration methodDeclaration = functionExpression.getMethodDeclaration();
		char[] possibleTypeName = localDeclaration.getName();

		InferredType type = null;
		if (possibleTypeName != null) {
			type = compUnit.findInferredType(possibleTypeName);
			if (type == null && isPossibleClassName(possibleTypeName)) {
				type = addType(possibleTypeName, true);
			}
			if (type == null && methodDeclaration.getJsDoc() != null && ((Javadoc) methodDeclaration.getJsDoc()).isConstructor) {

				type = addType(possibleTypeName, true);
				handleJSDocConstructor(type, methodDeclaration, localDeclaration.sourceStart());
			}
		}

		if (type != null) { // isConstructor
			if (this.inferOptions.useInitMethod) {
				this.currentContext.currentType = type;
				type.setIsDefinition(true);
				int nameStart = localDeclaration.sourceStart();
				type.addConstructorMethod(type.name, methodDeclaration, nameStart);
				type.updatePositions(nameStart, localDeclaration.getInitialization().sourceEnd());

				// set the type for the local declaration to be the new type
				localDeclaration.setInferredType(type);
				localDeclaration.setIsType(true);
			}

			keepVisiting = false;
		}
		return keepVisiting;
	}

	/**
	 * @param assignment
	 * @return whether a type was not created for this assignment
	 * @since 1.3
	 */
	protected boolean handleLocalDeclarationExpressionType(ILocalDeclaration localDeclaration) {
		IExpression expr = localDeclaration.getInitialization();
		InferredType type = null;

		if (expr instanceof IAssignment) {
			type = ((IAssignment) expr).getInferredType();
		}
		else if (expr instanceof IAbstractVariableDeclaration) {
			type = ((IAbstractVariableDeclaration) expr).getInferredType();
		}
		else if (expr instanceof IFieldReference) {
			IExpression receiver = ((IFieldReference) expr).getReceiver();
			InferredType receiverType = this.getTypeOf(receiver);
			if (receiverType != null) {
				InferredAttribute attr = receiverType.findAttribute(((IFieldReference) expr).getToken());
				if (attr != null)
					type = attr.inType;
			}
		}
		else if (expr instanceof ISingleNameReference) {
			IAbstractVariableDeclaration varDecl = this.getVariable(expr);
			if (varDecl != null) {
				type = varDecl.getInferredType();
			}

			if (type == null) {
				IAssignment assign = this.getAssignment(expr);
				if (assign != null) {
					type = assign.getInferredType();
				}
			}

			if (type == null) {
				IAbstractFunctionDeclaration funcDecl = this.getFunction(expr);
				if (funcDecl != null && funcDecl.getName() != null) {
					type = this.findDefinedType(funcDecl.getName());
				}
			}

			if (type == null) {
				type = this.compUnit.findInferredType(((ISingleNameReference) expr).getToken());
			}
		}
		else if (expr instanceof IThisReference) {
			InferredType newType = null;

			IFunctionDeclaration parentMethod = this.currentContext.currentMethod;
			IAssignment parentAssignment = this.currentContext.parent.currentAssignment;
			ILocalDeclaration parentLocalDeclaration = this.currentContext.parent.currentLocalDeclaration;
			char[] newTypeName = null;
			int typeStart = 0;
			int typeEnd = 0;
			/*
			 * if there is a current assignment and LHS is a function and that
			 * function is the current method then use the RHS as the type
			 * name
			 * 
			 * else if there is a current local declaration and the LHS is a
			 * function and that function is the current method then use the
			 * RHS as the type name
			 * 
			 * else if the parent method is not in a type and has a name use
			 * that as the type name
			 */
			if (this.currentContext.parent != null && parentAssignment != null && parentAssignment.getExpression() instanceof IFunctionExpression && ((IFunctionExpression) parentAssignment.getExpression()).getMethodDeclaration() == parentMethod) {
				// see if we're adding a field through the prototype
				if (parentAssignment.getLeftHandSide().getASTType() == IASTNode.FIELD_REFERENCE && ((Expression) ((IFieldReference) parentAssignment.getLeftHandSide()).getReceiver()).isPrototype()) {
					newTypeName = Util.getTypeName(((IFieldReference) ((IFieldReference) parentAssignment.getLeftHandSide()).getReceiver()).getReceiver());
				}
				else {
					newTypeName = Util.getTypeName(parentAssignment.getLeftHandSide());
				}
				typeStart = parentAssignment.sourceStart();
				typeEnd = parentAssignment.sourceEnd();
			}
			else if (this.currentContext.parent != null && parentLocalDeclaration != null && parentLocalDeclaration.getInitialization() instanceof IFunctionExpression && ((IFunctionExpression) parentLocalDeclaration.getInitialization()).getMethodDeclaration() == parentMethod) {

				newTypeName = parentLocalDeclaration.getName();
				typeStart = parentLocalDeclaration.sourceStart();
				typeEnd = parentLocalDeclaration.sourceEnd();
			}
			else if (parentMethod != null && parentMethod.getName() != null && (parentMethod.getInferredMethod() == null || parentMethod.getInferredMethod().inType == null)) {

				newTypeName = parentMethod.getName();
				typeStart = parentMethod.sourceStart();
				typeEnd = parentMethod.sourceEnd();
			}

			// if calculated new type name, use it to create a new type
			if (newTypeName != null) {
				newType = compUnit.findInferredType(newTypeName);
				// create the new type if not found
				if (newType == null) {
					newType = addType(newTypeName);
				}
			}
			else {
				return false; // no type to create
			}

			newType.setIsDefinition(true);
			newType.updatePositions(typeStart, typeEnd);
			type = newType;
		}
		// prevent Object literal based anonymous types from being created
		// more than once
		if (passNumber == 1 && expr instanceof IObjectLiteral) {
			return false;
		}

		if (type != null) {
			this.setTypeOf(expr, type);
			if (localDeclaration instanceof AbstractVariableDeclaration) {
				((AbstractVariableDeclaration) localDeclaration).setInferredType(type);
			}
			return true;
		}

		return false;
	}

	/**
	 * @param assignment
	 * @return whether a type was not created for this assignment
	 */
	protected boolean handlePotentialType(IAssignment assignment) {

		IExpression lhs = assignment.getLeftHandSide();
		if (lhs instanceof FieldReference) {
			FieldReference fieldReference = (FieldReference) lhs;

			/* foo.prototype = ? */
			if (fieldReference.isPrototype()) {
				/*
				 * When encountering a prototype, we are going to assume that
				 * the receiver is a type.
				 * 
				 * If the type had not been inferred, it will be added at this
				 * point
				 */
				InferredType newType = null;
				char[] possibleTypeName = constructTypeName(fieldReference.getReceiver());
				if (possibleTypeName != null)
					newType = compUnit.findInferredType(possibleTypeName);
				else
					return true; // no type created

				// create the new type if not found
				if (newType == null) {
					// if we have the function, check if it has a return type,
					// if so, don't consider it a new type
					IAbstractFunctionDeclaration theFunction = this.getFunction(fieldReference.getReceiver());
					IAbstractVariableDeclaration theVariable = null;
					if (theFunction == null)
						theVariable = this.getVariable(fieldReference.getReceiver());
					if (theFunction != null) {
						if (theFunction.getInferredType() != null && !theFunction.getInferredType().isVoid()) {
							return false;
						}
					}
					else if (theVariable != null) {
						if (theVariable.getInitialization() != null && theVariable.getInitialization() instanceof IFunctionExpression) {
							if (((IFunctionExpression) theVariable.getInitialization()).getMethodDeclaration().getInferredType() != null && !((IFunctionExpression) theVariable.getInitialization()).getMethodDeclaration().getInferredType().isVoid()) {
								return false;
							}
						}
					}
					newType = addType(possibleTypeName, true);
				}
				newType.setIsDefinition(true);
				newType.updatePositions(assignment.sourceStart(), assignment.sourceEnd());

				/* foo.prototype = new ... */
				if (assignment.getExpression() instanceof IAllocationExpression) {
					// setting the super type
					IAllocationExpression allocationExpression = (IAllocationExpression) assignment.getExpression();

					InferredType superType = null;
					char[] possibleSuperTypeName = constructTypeName(allocationExpression.getMember());
					if (possibleSuperTypeName != null) {
						superType = compUnit.findInferredType(possibleSuperTypeName);

						if (superType == null)
							superType = addType(possibleSuperTypeName);

						// check if it is set already because it might be set
						// by jsdocs
						if (newType.getSuperType() == null)
							newType.setSuperType(superType);
					}

					return true;
				}
				/* foo.prototype = {...} */
				else if (assignment.getExpression() instanceof IObjectLiteral) {
					// rather than creating an anonymous type, is better just
					// to set the members
					// directly
					// on newType
					populateType(newType, (IObjectLiteral) assignment.getExpression(), false);

					// check if it is set already because it might be set by
					// jsdocs
					if (newType.getSuperType() == null)
						newType.setSuperType(this.getObjectType());

					return true;
				}
				/* foo.prototype = foo.someField; */
				else if (assignment.getExpression() instanceof FieldReference) {
					InferredType superType = getTypeOf(assignment.getExpression());
					if (newType.getSuperType() == null && superType != null) {
						newType.setSuperType(superType);
					}
					return true;
				}
				/* foo.prototype = somevar; */
				else if (assignment.getExpression() instanceof SingleNameReference) {
					InferredType superType = getTypeOf(assignment.getExpression());
					if (newType.getSuperType() == null && superType != null) {
						newType.setSuperType(superType);
					}
					return true;
				}
			}
			/* foo.prototype.bar = ? */
			else if (fieldReference.receiver.isPrototype()) {

				FieldReference prototype = (FieldReference) fieldReference.receiver;

				// if prototype receiver is a type, get its type
				InferredType assignedToType = null;
				if (this.isExpressionAType(prototype.receiver)) {
					assignedToType = this.getTypeOf(prototype.receiver);
				}

				// if not found assigned to type and can create possible name,
				// then create a type
				if (assignedToType == null) {
					char[] possibleTypeName = constructTypeName(prototype.receiver);

					if (possibleTypeName != null) {
						assignedToType = addType(possibleTypeName);
						assignedToType.updatePositions(assignment.sourceStart(), assignment.sourceEnd());
						assignedToType.setIsDefinition(true);
					}
					else {
						return true; // can not create type, keep visiting
					}
				}

				// prevent Object literal based anonymous types from being
				// created more than once
				if (passNumber == 1 && assignment.getExpression() instanceof IObjectLiteral) {
					return false;
				}

				char[] memberName = fieldReference.token;
				int nameStart = (int) (fieldReference.nameSourcePosition >>> 32);

				InferredType typeOf = (assignment.getJsDoc() != null && assignment.getJsDoc() instanceof Javadoc && ((Javadoc) assignment.getJsDoc()).returnType != null) ? this.addType(changePrimitiveToObject(((Javadoc) assignment.getJsDoc()).returnType.getFullTypeName())) : getTypeOf(assignment.getExpression());
				IFunctionDeclaration methodDecl = null;

				if (typeOf == null || typeOf == this.getFunctionType())
					methodDecl = getDefinedFunction(assignment.getExpression());

				if (methodDecl != null) {
					assignedToType.addMethod(memberName, methodDecl, nameStart);
				}
				else {
					InferredAttribute attribute = assignedToType.addAttribute(memberName, assignment, nameStart);
					handleAttributeDeclaration(attribute, assignment.getExpression());
					attribute.initializationStart = assignment.getExpression().sourceStart();
					if (attribute.type == null)
						attribute.type = typeOf;
				}
				return true;
			}
			/* this.foo = ? */
			else if (fieldReference.receiver instanceof IThisReference) {
				InferredType newType = null;

				IFunctionDeclaration parentMethod = this.currentContext.currentMethod;
				IAssignment parentAssignment = this.currentContext.parent.currentAssignment;
				ILocalDeclaration parentLocalDeclaration = this.currentContext.parent.currentLocalDeclaration;
				char[] newTypeName = null;
				int typeStart = 0;
				int typeEnd = 0;
				/*
				 * if there is a current assignment and LHS is a function and
				 * that function is the current method then use the RHS as the
				 * type name
				 * 
				 * else if there is a current local declaration and the LHS is
				 * a function and that function is the current method then use
				 * the RHS as the type name
				 * 
				 * else if the parent method is not in a type and has a name
				 * use that as the type name
				 */
				if (this.currentContext.parent != null && parentAssignment != null && parentAssignment.getExpression() instanceof IFunctionExpression && ((IFunctionExpression) parentAssignment.getExpression()).getMethodDeclaration() == parentMethod) {
					// see if we're adding a field through the prototype
					if (parentAssignment.getLeftHandSide().getASTType() == IASTNode.FIELD_REFERENCE && ((Expression) ((IFieldReference) parentAssignment.getLeftHandSide()).getReceiver()).isPrototype()) {
						newTypeName = Util.getTypeName(((IFieldReference) ((IFieldReference) parentAssignment.getLeftHandSide()).getReceiver()).getReceiver());
					}
					else {
						newTypeName = Util.getTypeName(parentAssignment.getLeftHandSide());
					}
					typeStart = parentAssignment.sourceStart();
					typeEnd = parentAssignment.sourceEnd();
				}
				else if (this.currentContext.parent != null && parentLocalDeclaration != null && parentLocalDeclaration.getInitialization() instanceof IFunctionExpression && ((IFunctionExpression) parentLocalDeclaration.getInitialization()).getMethodDeclaration() == parentMethod) {

					newTypeName = parentLocalDeclaration.getName();
					typeStart = parentLocalDeclaration.sourceStart();
					typeEnd = parentLocalDeclaration.sourceEnd();
				}
				else if (parentMethod != null && parentMethod.getName() != null && (parentMethod.getInferredMethod() == null || parentMethod.getInferredMethod().inType == null)) {

					newTypeName = parentMethod.getName();
					typeStart = parentMethod.sourceStart();
					typeEnd = parentMethod.sourceEnd();
				}

				// if calculated new type name, use it to create a new type
				if (newTypeName != null) {
					newType = compUnit.findInferredType(newTypeName);
					// create the new type if not found
					if (newType == null) {
						newType = addType(newTypeName);
					}
				}
				else {
					return false; // no type to create
				}

				newType.setIsDefinition(true);
				newType.updatePositions(typeStart, typeEnd);

				// if there is a parent assignment then set the inferred type
				// and that it is a type
				if (parentAssignment != null) {
					parentAssignment.setInferredType(newType);
					parentAssignment.setIsType(true);
				}

				// if there is a parent declaration then set the inferred type
				// and that it is a type
				if (parentLocalDeclaration != null) {
					parentLocalDeclaration.setInferredType(newType);
					parentLocalDeclaration.setIsType(true);
				}

				// prevent Object literal based anonymous types from being
				// created more than once
				if (passNumber == 1 && assignment.getExpression() instanceof IObjectLiteral) {
					return false;
				}

				char[] memberName = fieldReference.token;
				int nameStart = (int) (fieldReference.nameSourcePosition >>> 32);

				InferredType typeOf = getTypeOf(assignment.getExpression());
				IFunctionDeclaration methodDecl = null;

				if (typeOf == null || typeOf == this.getFunctionType()) {
					methodDecl = getDefinedFunction(assignment.getExpression());
				}

				if (methodDecl != null) {
					newType.addMethod(memberName, methodDecl, nameStart);
					if (methodDecl.getInferredType() == null && assignment.getJsDoc() != null && ((Javadoc) assignment.getJsDoc()).returnType != null) {
						if (((Javadoc) assignment.getJsDoc()).returnType.getFullTypeName() != null)
							methodDecl.setInferredType(addType(((Javadoc) assignment.getJsDoc()).returnType.getFullTypeName()));
					}
				}
				else {
					InferredAttribute attribute = newType.addAttribute(memberName, assignment, nameStart);
					if (attribute.type == null && assignment.getJsDoc() != null && ((Javadoc) assignment.getJsDoc()).returnType != null) {
						if (((Javadoc) assignment.getJsDoc()).returnType.getFullTypeName() != null)
							attribute.type = addType(((Javadoc) assignment.getJsDoc()).returnType.getFullTypeName());
					}
					handleAttributeDeclaration(attribute, assignment.getExpression());
					attribute.initializationStart = assignment.getExpression().sourceStart();
					if (attribute.type == null)
						attribute.type = typeOf;
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the function referenced by the expression
	 * 
	 * @param expression
	 *            AST node
	 * @return the function or null
	 */
	protected IFunctionDeclaration getDefinedFunction(IExpression expression) {
		if (expression instanceof SingleNameReference) {
			Object object = this.currentContext.getMember(((SingleNameReference) expression).token);
			if (object instanceof AbstractMethodDeclaration) {
				return (MethodDeclaration) object;
			}
		}
		else if (expression instanceof FunctionExpression) {
			return ((FunctionExpression) expression).methodDeclaration;
		}
		else if (expression instanceof FieldReference) {
			FieldReference fieldReference = (FieldReference) expression;
			InferredType receiverType = getInferredType(fieldReference.receiver);
			if (receiverType == null && passNumber == 2) {
				receiverType = this.getReceiverType(fieldReference.receiver, false);
			}
			if (receiverType != null) {
				InferredMethod method = receiverType.findMethod(fieldReference.token, null);
				if (method != null) {
					return method.getFunctionDeclaration();
				}
			}

		}

		return null;

	}

	/**
	 * <p>
	 * Sets the inferred type of the given expression to the given type. Any
	 * existing inferred type is overridden. If the given expression is not
	 * supported by this method then it is a no op.
	 * </p>
	 * 
	 * <p>
	 * Currently supports:
	 * <ul>
	 * <li>{@link ISingleNameReference}</li>
	 * <li>{@link FieldReference} - if it can resolve everything it needs
	 * to</li>
	 * <li>{@link IObjectLiteral}</li>
	 * <li>{@link IThisReference}</li>
	 * </ul>
	 * </p>
	 * 
	 * @param expression
	 *            to set the inferred type for
	 * @param type
	 *            inferred type to set on the given expression
	 */
	protected void setTypeOf(IExpression expression, InferredType type) {
		if (expression instanceof ISingleNameReference) {
			// set on variable declaration if there is one
			IAbstractVariableDeclaration varDecl = getVariable(expression);
			if (varDecl != null) {
				varDecl.setInferredType(type);
			}

			// set on global field if there is one
			if (this.inferredGlobal != null) {
				InferredAttribute attribute = this.inferredGlobal.findAttribute(((ISingleNameReference) expression).getToken());
				if (attribute != null) {
					attribute.type = type;
				}
			}

			// set on assignment if there is one
			IAssignment varAssignment = this.getAssignment(expression);
			if (varAssignment != null) {
				varAssignment.setInferredType(type);
			}
		}
		else if (expression instanceof FieldReference) {
			FieldReference fieldReference = (FieldReference) expression;
			InferredType parentType = this.getTypeOf(fieldReference.getReceiver());
			if (parentType != null) {
				InferredAttribute attr = parentType.findAttribute(fieldReference.token);
				if (attr != null) {
					attr.type = type;
				}
			}
		}
		else if (expression instanceof IObjectLiteral) {
			((IObjectLiteral) expression).setInferredType(type);
		}
		else if (expression instanceof IThisReference) {
			this.currentContext.currentType = type;
		}
	}

	protected InferredType getTypeOf(IExpression expression) {
		if (expression instanceof IStringLiteral) {
			return this.getStringType();
		}
		else if (expression instanceof INumberLiteral) {
			return this.getNumberType();
		}
		else if (expression instanceof IAllocationExpression) {
			IAllocationExpression allocationExpression = (IAllocationExpression) expression;
			IExpression member = allocationExpression.getMember();
			InferredType type = null;

			// check for type with the same name as the allocation member
			char[] possibleTypeName = constructTypeName(member);
			if (possibleTypeName != null) {
				type = compUnit.findInferredType(possibleTypeName);

			}

			/*
			 * if no existing type with the same name as the allocation member
			 * attempt to get the type of the member
			 */
			if (type == null) {
				type = this.getTypeOf(member);
			}

			/*
			 * if no type with same name as member, and member does not have a
			 * type, create a new type with the same name as the member
			 */
			if ((type == null || type == getFunctionType()) && possibleTypeName != null && possibleTypeName.length > 0) {
				type = this.addType(possibleTypeName, false);
			}

			return type;
		}
		else if (expression instanceof ISingleNameReference) {
			// check for variable declaration to get type from
			IAbstractVariableDeclaration varDecl = this.getVariable(expression);
			if (varDecl != null) {
				return varDecl.getInferredType();
			}

			// check global type to get type from
			if (this.inferredGlobal != null) {
				InferredAttribute attribute = this.inferredGlobal.findAttribute(((ISingleNameReference) expression).getToken());
				if (attribute != null) {
					return attribute.type;
				}
			}

			// check for an assignment to get the type from
			IAssignment assign = this.getAssignment(expression);
			if (assign != null) {
				InferredType type = assign.getInferredType();
				if (type != null) {
					return type;
				}
			}

			/*
			 * check if expression is a function and if there is a type with
			 * the same name, if so then the given expression is a reference
			 * to that type
			 */
			IAbstractFunctionDeclaration funcDecl = this.getFunction(expression);
			if (funcDecl != null) {
				InferredType type = this.findDefinedType(funcDecl.getName());
				if (type != null) {
					return type;
				}
			}

			// search for type with that name
			char[] possibleTypeName = this.constructTypeName(expression);
			if (possibleTypeName != null) {
				InferredType type = compUnit.findInferredType(possibleTypeName);
				if (type != null) {
					return type;
				}
			}

			// search for anonymous global type based of SNR
			char[] possibleGlobalTypeName = createAnonymousGlobalTypeName(((SingleNameReference) expression).token);
			if (possibleGlobalTypeName != null) {
				InferredType type = compUnit.findInferredType(possibleGlobalTypeName);
				if (type != null) {
					return type;
				}
			}
			if (funcDecl != null) {
				return getFunctionType();
			}
		}
		else if (expression instanceof FieldReference) {
			FieldReference fieldReference = (FieldReference) expression;
			/*
			 * if this reference and in current type else not
			 */
			if (fieldReference.receiver.isThis() && currentContext.currentType != null) {
				InferredAttribute attribute = currentContext.currentType.findAttribute(fieldReference.getToken());
				if (attribute != null) {
					return attribute.type;
				}
			}
			else {
				// get the receiver type then get the type of an existing
				// field or function
				InferredType recieverType = this.getReceiverType(((FieldReference) expression).getReceiver(), false);
				if (recieverType != null) {
					char[] fieldName = ((FieldReference) expression).getToken();
					InferredAttribute attr = recieverType.findAttribute(fieldName);
					if (attr != null) {
						return attr.type;
					}
					else if (recieverType.findMethod(fieldName, null) != null) {
						return this.getFunctionType();
					}
				}
			}

			char[] typeName = constructTypeName(expression);
			if (typeName != null && typeName.length > 0) {
				InferredType type = this.findDefinedType(typeName);
				if (type != null) {
					return type;
				}
			}
		}
		else if (expression instanceof IReturnStatement) {
			return ((IReturnStatement) expression).getInferredType();
		}
		else if (expression instanceof ArrayInitializer) {
			ArrayInitializer arrayInitializer = (ArrayInitializer) expression;
			boolean typeSet = false;
			InferredType memberType = null;
			if (arrayInitializer.expressions != null)
				for (int i = 0; i < arrayInitializer.expressions.length; i++) {
					InferredType thisType = getTypeOf(arrayInitializer.expressions[i]);
					if (thisType != null) {
						if (!thisType.equals(memberType))
							if (!typeSet) {
								memberType = thisType;
							}
							else {
								memberType = null;
							}
						typeSet = true;

					}
				}
			if (memberType != null) {
				InferredType type = new InferredType(InferredType.ARRAY_NAME);
				type.referenceClass = memberType;
				return type;
			}
			else {
				return this.getArrayType();
			}
		}
		else if (expression instanceof ITrueLiteral || expression instanceof IFalseLiteral) {
			return this.getBooleanType();
		}
		else if (expression instanceof IObjectLiteral) {

			// create an anonymous type based on the ObjectLiteral
			InferredType type = createAnonymousType((IObjectLiteral) expression);

			// set the start and end
			type.sourceStart = expression.sourceStart();
			type.sourceEnd = expression.sourceEnd();

			return type;
		}
		else if (expression instanceof IThisReference) {
			return this.currentContext.currentType != null ? this.currentContext.currentType : getInferredGlobal(true);
		}
		else if (expression instanceof Assignment)
			return getTypeOf(((Assignment) expression).getExpression());
		else if (expression instanceof FunctionExpression)
			return this.getFunctionType();
		else if (expression instanceof UnaryExpression) {
			return getTypeOf(((UnaryExpression) expression).expression);
		}
		else if (expression instanceof BinaryExpression) {
			BinaryExpression bExpression = (BinaryExpression) expression;
			int operator = (bExpression.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT;
			switch (operator) {
				case OperatorIds.MULTIPLY :
				case OperatorIds.DIVIDE :
				case OperatorIds.REMAINDER :
				case OperatorIds.MINUS :
				case OperatorIds.LEFT_SHIFT :
				case OperatorIds.RIGHT_SHIFT :
					return this.getNumberType();
				case OperatorIds.PLUS : {
					InferredType leftType = getTypeOf(bExpression.left);
					InferredType rightType = getTypeOf(bExpression.right);
					if (leftType != null && leftType.equals(this.getStringType())) {
						return this.getStringType();
					}
					if (rightType != null && rightType.equals(this.getStringType())) {
						return this.getStringType();
					}
					if (leftType == null || rightType == null) {
						return null;
					}
					if (leftType.equals(this.getStringType()) || rightType.equals(this.getStringType())) {
						return this.getStringType();
					}
					else if (leftType.equals(this.getNumberType()) && rightType.equals(this.getNumberType())) {
						return this.getNumberType();
					}
					return null;
				}
				case OperatorIds.AND_AND :
				case OperatorIds.OR_OR : {
					/*
					 * if the case: foo = foo || "" return foo || "" foo = foo
					 * && "" return foo || "" else a normal || binary case
					 */
					if ((this.currentContext.currentAssignment != null && expressionContains(this.currentContext.currentAssignment.getExpression(), bExpression)) || (this.currentContext.currentLocalDeclaration != null && expressionContains(this.currentContext.currentLocalDeclaration.getInitialization(), bExpression)) || (this.currentContext.currentReturn != null && expressionContains(this.currentContext.currentReturn.getExpression(), bExpression))) {

						InferredType leftType = this.getTypeOf(bExpression.left);
						InferredType rightType = this.getTypeOf(bExpression.right);

						/*
						 * if both the left and right type are the same then
						 * just return the left one else merge the two types
						 * into one type
						 */
						if (leftType == rightType) {
							return leftType;
						}
						else if (leftType != null && rightType != null) {
							InferredType newCombinedType = this.createAnonymousType(expression, null);

							/*
							 * add the left type to the combined type
							 * 
							 * if the left type is indexed then add it later
							 * else it is not must add it now
							 */
							if (leftType.isIndexed()) {
								newCombinedType.addMixin(leftType.getName());
							}
							else {
								newCombinedType.mixin(leftType);
							}

							/*
							 * add the right type to the combined type
							 * 
							 * if the right type is indexed then add it later
							 * else it is not must add it now
							 */
							if (rightType.isIndexed()) {
								newCombinedType.addMixin(rightType.getName());
							}
							else {
								newCombinedType.mixin(rightType);
							}

							return newCombinedType;
						}
						else if (leftType != null) {
							return leftType;
						}
						else if (rightType != null) {
							return rightType;
						}
					}

					return this.getBooleanType();
				}
				case OperatorIds.EQUAL_EQUAL :
				case OperatorIds.EQUAL_EQUAL_EQUAL :
				case OperatorIds.NOT_EQUAL :
				case OperatorIds.NOT_EQUAL_EQUAL :
				case OperatorIds.GREATER :
				case OperatorIds.GREATER_EQUAL :
				case OperatorIds.LESS :
				case OperatorIds.LESS_EQUAL :
				case OperatorIds.INSTANCEOF :
				case OperatorIds.IN :
					return this.getBooleanType();
				default :
					return null;
			}
		}
		else if (expression instanceof MessageSend) {
			if (((MessageSend) expression).receiver instanceof IFunctionExpression) {
				if (((FunctionExpression) ((MessageSend) expression).receiver).methodDeclaration != null) {
					return ((FunctionExpression) ((MessageSend) expression).receiver).methodDeclaration.inferredType;
				}
			}
			else if (((MessageSend) expression).selector != null) {
				// if the method call has no receiver, see if we can look it
				// up
				// in the current context. If found, the inferredType(return
				// type) of
				// that function is the value we are looking for.
				Object method = this.currentContext.getMember(((MessageSend) expression).selector);
				if (method instanceof MethodDeclaration) {
					return ((MethodDeclaration) method).inferredType;
				}
			}
		}

		return null;
	}

	protected void populateType(InferredType type, IObjectLiteral objLit, boolean isStatic) {
		if (objLit.getInferredType() == null) {
			objLit.setInferredType(type);
			if (objLit.getFields() != null) {
				for (int i = 0; i < objLit.getFields().length; i++) {
					IObjectLiteralField field = objLit.getFields()[i];

					char[] name = null;
					int nameStart = -1;

					if (field.getFieldName() instanceof SingleNameReference) {
						SingleNameReference singleNameReference = (SingleNameReference) field.getFieldName();
						name = singleNameReference.token;
						nameStart = singleNameReference.sourceStart;
					}
					else if (field.getFieldName() instanceof IStringLiteral) {
						IStringLiteral stringLiteral = (IStringLiteral) field.getFieldName();
						name = stringLiteral.source();
						nameStart = stringLiteral.sourceStart();
					}
					else
						continue; // not supporting this case right now

					Javadoc javaDoc = (Javadoc) field.getJsDoc();
					InferredType returnType = null;
					if (javaDoc != null) {
						if (javaDoc.memberOf != null) {
							char[] typeName = javaDoc.memberOf.getFullTypeName();
							convertAnonymousTypeToNamed(type, typeName);
							type.setIsDefinition(true);
						}
						else if (this.currentContext.isJsDocClass && javaDoc.property != null) {
							if (type.isAnonymous) {
								InferredType previousType = this.currentContext.currentType;
								if (previousType != null) {
									copyAnonymousTypeToNamed(type, previousType);
									objLit.setInferredType(type = this.currentContext.currentType = previousType);
								}

							}
						}
						if (javaDoc.returnType != null) {
							returnType = this.addType(changePrimitiveToObject(javaDoc.returnType.getFullTypeName()));
						}
					}

					// need to build the members of the anonymous inferred
					// type
					if (field.getInitializer() instanceof IFunctionExpression) {
						IFunctionExpression functionExpression = (IFunctionExpression) field.getInitializer();
						InferredMember method = type.addMethod(name, functionExpression.getMethodDeclaration(), nameStart);
						method.isStatic = isStatic;
						if (javaDoc != null) {
							functionExpression.getMethodDeclaration().modifiers = javaDoc.modifiers;
						}
						handleFunctionDeclarationArguments(functionExpression.getMethodDeclaration(), javaDoc);
						if (returnType != null && functionExpression.getMethodDeclaration().getInferredType() == null) {
							functionExpression.getMethodDeclaration().setInferredType(returnType);
						}

					}
					// attribute
					else if (field.getInitializer() != null) {
						InferredAttribute attribute = type.findAttribute(name);
						if (attribute == null) {
							attribute = type.addAttribute(name, field.getInitializer(), nameStart);
							handleAttributeDeclaration(attribute, field.getInitializer());
							attribute.isStatic = isStatic;
							// @GINO: recursion might not be the best idea
							if (returnType != null) {
								attribute.type = returnType;
								// apply (force) type onto OL initializer
								if (field.getInitializer() instanceof ObjectLiteral) {
									((ObjectLiteral) field.getInitializer()).setInferredType(returnType);
								}
							}
							else {
								attribute.type = getTypeOf(field.getInitializer());
							}


						}

						// if attribute is function then create a function as
						// well
						if (attribute.type != null && attribute.type.isFunction()) {
							if (type.findMethod(attribute.name, null) == null) {
								IAbstractFunctionDeclaration func = this.getFunction(field.getInitializer());
								if (func instanceof IFunctionDeclaration) {
									type.addMethod(name, (IFunctionDeclaration) func, nameStart);
								}
							}
						}
					}
				}
			}
		}
	}

	public void endVisit(IAssignment assignment) {
		popContext();
	}

	protected boolean handleAttributeDeclaration(InferredAttribute attribute) {
		return true;
	}

	protected boolean handleAttributeDeclaration(InferredAttribute attribute, IExpression initializer) {
		return true;
	}

	protected boolean handleFunctionCall(IFunctionCall messageSend) {
		return handleFunctionCall(messageSend, null);
	}

	protected boolean handleFunctionCall(IFunctionCall messageSend, LocalDeclaration assignmentExpression) {
		return true;
	}

	public boolean visit(IReturnStatement returnStatement) {
		this.pushContext();
		this.currentContext.currentReturn = returnStatement;

		if (currentContext.currentMethod != null) {
			if (returnStatement.getExpression() != null) {

				InferredType type = null;
				IExpression expression = returnStatement.getExpression();
				if (expression instanceof IObjectLiteral) {
					type = createAnonymousType((ObjectLiteral) expression);

					// set the start and end
					type.sourceStart = expression.sourceStart();
					type.sourceEnd = expression.sourceEnd();
				}
				else {
					type = getTypeOf(expression);
				}
				if (currentContext.currentMethod.getInferredType() == this.getVoidType() && type != null) {
					currentContext.currentMethod.setInferredType(type);
				}
				else if (currentContext.currentMethod.getInferredType() == this.getArrayType() && currentContext.currentMethod.getInferredType().referenceClass == null && type != null && CharOperation.equals(type.name, TypeConstants.ARRAY[0])) {
					currentContext.currentMethod.setInferredType(type);
				}
				else {
					/*
					 * If the return statement inferred type is null or the
					 * existing inferred return type and the statement return
					 * type are not equal and the return type is either not
					 * well known or is well known and the return type names
					 * are the same
					 * 
					 * This logic is to cover the scenario where the return
					 * type is a known type but is from a different instance
					 * of the InferEngine
					 */
					boolean shouldSetToAny = !((MethodDeclaration) currentContext.currentMethod).isInferredJsDocType();
					if (type != null && shouldSetToAny) {
						// get the name of the current methods inferred return
						// type
						String currentMethodInferredType = null;
						if (this.currentContext.currentMethod.getInferredType() != null && this.currentContext.currentMethod.getInferredType().name != null) {
							currentMethodInferredType = new String(this.currentContext.currentMethod.getInferredType().name);
						}

						boolean returnTypesEqual = type.equals(currentContext.currentMethod.getInferredType());
						boolean returnTypeNamesEqual = (new String(type.name)).equals(currentMethodInferredType);
						boolean returnTypeIsWellKnown = WellKnownTypes.containsKey(type.name);

						shouldSetToAny = !returnTypesEqual && (!returnTypeIsWellKnown || !(returnTypeIsWellKnown && returnTypeNamesEqual));
					}

					if (shouldSetToAny) {
						currentContext.currentMethod.setInferredType(null);
					}
				}
			}
		}
		return false;
	}

	/**
	 * @see org.eclipse.wst.jsdt.core.ast.ASTVisitor#endVisit(org.eclipse.wst.jsdt.core.ast.IReturnStatement)
	 */
	public void endVisit(IReturnStatement returnStatement) {
		this.popContext();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.wst.jsdt.core.ast.ASTVisitor#endVisit(org.eclipse.wst.jsdt.
	 * core.ast.IFunctionCall)
	 */
	public void endVisit(IFunctionCall functionCall) {
		super.endVisit(functionCall);

		if (functionCall.getReceiver() instanceof FunctionExpression) {
			if (this.contextPtr == -1) {
				this.isTopLevelAnonymousFunction = true;
			}
			if (functionCall instanceof MessageSend && ((MessageSend) functionCall).getArguments() != null) {
				MethodDeclaration methodDeclaration = ((FunctionExpression) functionCall.getReceiver()).getMethodDeclaration();
				if (methodDeclaration != null && methodDeclaration.getArguments() != null) {
					IArgument[] declaredArguments = methodDeclaration.getArguments();
					IExpression[] sentArguments = ((MessageSend) functionCall).getArguments();
					for (int i = 0; i < declaredArguments.length; i++) {
						if (i >= sentArguments.length) {
							continue;
						}
						// if a sent argument now is a subtype of the declared
						// argument, update the sent argument to be the type
						// of the declared argument
						char[] parameterName = null;
						if ((parameterName = getName(sentArguments[i])) != null && passNumber == 2 && declaredArguments[i].getInferredType() != null) {
							IASTNode node = (IASTNode) currentContext.getMember(parameterName);
							if (node instanceof IAbstractVariableDeclaration && ((IAbstractVariableDeclaration) node).getInferredType() != null) {
								if (((IAbstractVariableDeclaration) node).getInferredType().isNamed() && ((LocalDeclaration) node).getInferredType().equals(declaredArguments[i].getInferredType().getSuperType())) {
									((IAbstractVariableDeclaration) node).setInferredType(declaredArguments[i].getInferredType());
								}
							}
							else if (node instanceof IAssignment && ((IAssignment) node).getInferredType() != null) {
								if (((IAssignment) node).getInferredType().isNamed() && ((IAssignment) node).getInferredType().equals(declaredArguments[i].getInferredType().getSuperType())) {
									((IAssignment) node).setInferredType(declaredArguments[i].getInferredType());
								}
							}
						}
					}
				}
			}
		}
	}

	public void endVisit(IFunctionDeclaration methodDeclaration) {
		popContext();
	}

	public boolean visit(IFunctionDeclaration methodDeclaration) {
		pushContext();
		if (this.isTopLevelAnonymousFunction && this.currentContext.currentType == null) {
			this.currentContext.currentType = this.getInferredGlobal(true);
		}
		else if (!this.isTopLevelAnonymousFunction && this.currentContext.currentType != null && CharOperation.equals(this.currentContext.currentType.getName(), IIndexConstants.GLOBAL_SYMBOL)) {
			// if we are not a top level function and the current type is
			// global, reset it to null.
			// this prevents this.anything style attributes from leaking to
			// the global scope.
			this.currentContext.currentType = null;
		}

		this.isTopLevelAnonymousFunction = false;
		char[] methodName = methodDeclaration.getName();
		// if declaration didn't have name get name from inferred method if
		// there is one
		if (methodName == null && methodDeclaration.getInferredMethod() != null) {
			methodName = methodDeclaration.getInferredMethod().name;
		}

		// set the function that contains this assignment
		if (this.passNumber == 1 && methodDeclaration instanceof AbstractMethodDeclaration && this.currentContext.currentMethod != null) {
			((AbstractMethodDeclaration) methodDeclaration).setContainingFunction(this.currentContext.currentMethod);
		}

		// always need to build arguments
		buildDefinedMembers(methodDeclaration.getStatements(), methodDeclaration.getArguments());

		// only on the first pass should JS doc be looked at
		if (passNumber == 1) {
			if (methodDeclaration.getJsDoc() != null) {
				InferredMethod method = null;
				Javadoc javadoc = (Javadoc) methodDeclaration.getJsDoc();
				createTypeIfNecessary(javadoc);
				if (javadoc.isConstructor) {
					InferredType type;
					if (!this.currentContext.isJsDocClass && methodName != null) {
						type = this.addType(methodName);
					}
					else {
						type = this.currentContext.currentType;
					}

					if (type != null) {
						handleJSDocConstructor(type, methodDeclaration, methodDeclaration.sourceStart());
					}
				}
				else if (javadoc.memberOf != null) {
					InferredType type = this.addType(javadoc.memberOf.getFullTypeName(), true);
					char[] name = methodName;
					int nameStart = methodDeclaration.sourceStart();
					if (name != null) {
						method = type.addMethod(methodName, methodDeclaration, nameStart);
					}
				}
				else if (javadoc.methodDef != null && this.currentContext.isJsDocClass) {
					InferredType type = this.currentContext.currentType;
					char[][] methName = javadoc.methodDef.getTypeName();
					int nameStart = ((MethodDeclaration) methodDeclaration).sourceStart;
					if (methName.length == 1) {
						method = type.addMethod(methName[0], methodDeclaration, nameStart);
					}
					else {
						method = type.addMethod(methName[methName.length - 1], methodDeclaration, nameStart);
						method.isStatic = true;
					}

				}

				if (javadoc.returnType != null) {
					InferredType type = this.addType(changePrimitiveToObject(javadoc.returnType.getFullTypeName()));
					methodDeclaration.setInferredType(type);
					((MethodDeclaration) methodDeclaration).bits |= ASTNode.IsInferredJsDocType;
				}

			}
			handleFunctionDeclarationArguments(methodDeclaration, methodDeclaration.getJsDoc());
		}

		// check if this is a constructor
		if (passNumber == 2) {
			if (methodName != null) {
				InferredType type = compUnit.findInferredType(methodName);
				InferredMethod inferMeth = methodDeclaration.getInferredMethod();

				// only a constructor if the method is in the found type, or
				// is in a type with the same name
				if (type != null && (inferMeth == null || inferMeth.inType == null || inferMeth.inType == type || CharOperation.equals(inferMeth.inType.getName(), type.getName()))) {

					this.currentContext.currentType = type;
					type.setIsDefinition(true);

					if (inferMeth == null || !inferMeth.isConstructor) {
						int nameStart = methodDeclaration.sourceStart();
						type.addConstructorMethod(methodName, methodDeclaration, nameStart);
					}
				}
			}
		}
		this.currentContext.currentMethod = methodDeclaration;
		if (methodDeclaration.getInferredMethod() != null && methodDeclaration.getInferredMethod().inType != null) {
			this.currentContext.currentType = methodDeclaration.getInferredMethod().inType;
		}

		if (methodDeclaration.getInferredType() == null) {
			methodDeclaration.setInferredType(this.getVoidType());
		}

		return true;
	}

	protected void handleConstructor(InferredType type, IFunctionDeclaration methodDeclaration, int start, int end) {
		type.setIsDefinition(true);
		type.addConstructorMethod(type.name, methodDeclaration, start);
		type.updatePositions(start, end);
	}

	protected void handleJSDocConstructor(InferredType type, IFunctionDeclaration methodDeclaration, int nameStart) {
		Javadoc javadoc = (Javadoc) methodDeclaration.getJsDoc();
		type.setIsDefinition(true);
		type.addConstructorMethod(type.name, methodDeclaration, nameStart);

		if (javadoc.extendsType != null) {
			InferredType superType = this.addType(javadoc.extendsType.getFullTypeName());
			type.setSuperType(superType);
		}

	}

	protected void handleFunctionDeclarationArguments(IFunctionDeclaration methodDeclaration, IJsDoc jsdoc) {
		if (jsdoc == null || !(jsdoc instanceof Javadoc))
			return;
		Javadoc javadoc = (Javadoc) jsdoc;

		IArgument[] arguments = methodDeclaration.getArguments();
		if (arguments != null) {
			for (int i = 0; i < arguments.length; i++) {
				// skip if argument already has a named type
				InferredType currType = arguments[i].getInferredType();
				if (currType != null && !currType.isAnonymous) {
					// TODO: this could possibly be smarter
					continue;
				}

				InferredType paramType = null;
				JavadocSingleNameReference param = javadoc.findParam(arguments[i].getName());
				if (param != null) {
					if (param.types != null) {
						char[] name = {};
						for (int j = 0; j < param.types.length; j++) {
							// char
							// []typeName=param.types[j].getFullTypeName();
							// make sure we are using the type version of
							// Boolean, even if the user
							// entered boolean as the JSdoc type.
							char[] typeName = changePrimitiveToObject(param.types[j].getFullTypeName());
							if (j == 0) {
								name = typeName;
							}
							else {
								name = CharOperation.append(name, '|');
								name = CharOperation.concat(name, typeName);
							}
						}

						paramType = this.addType(name);
					}
				}
				/**
				 * http://code.google.com/p/jsdoc-toolkit/wiki/InlineDocs
				 **/
				else if (arguments[i].getJsDoc() != null) {
					if (((Javadoc) arguments[i].getJsDoc()).returnType != null) {
						paramType = this.addType(((Javadoc) arguments[i].getJsDoc()).returnType.getFullTypeName());
					}
				}
				else if (arguments[i].getComment() != null) {
					char[] comment = CharOperation.trim(arguments[i].getComment());
					boolean validForName = true;
					for (int j = 0; j < comment.length && validForName; j++) {
						validForName &= !CharOperation.isWhitespace(comment[j]) && (Character.isJavaIdentifierPart(comment[j]) || comment[j] == '.');
					}
					if (validForName) {
						paramType = this.addType(comment);
					}
				}

				if (paramType != null) {
					/*
					 * if the current type is not null (then it it is also
					 * anonymous) set the doced type as its parent
					 */
					if (currType != null) {
						InferredType currSuperType = currType.getSuperType();
						currType.setSuperType(paramType);
						paramType.setSuperType(currSuperType);

						paramType = currType;
					}

					arguments[i].setInferredType(paramType);
				}
			}
		}
	}

	private void copyAnonymousTypeToNamed(InferredType inClass, InferredType toType) {
		if (toType == null)
			return;

		compUnit.inferredTypesHash.removeKey(inClass.name);
		if (inClass.methods != null) {
			toType.methods.addAll(inClass.methods);
		}
		if (inClass.attributes != null) {
			for (int i = 0; i < inClass.numberAttributes; i++) {
				toType.addAttribute(inClass.attributes[i]);
			}
		}

	}

	/**
	 * <p>
	 * Renames the given type to the given name. If there is a constructor on
	 * the type that is also renamed.
	 * </p>
	 * 
	 * @param type
	 *            {@link InferredType} to rename
	 * @param newTypeName
	 *            new type name for the given {@link InferredType}
	 */
	protected void renameType(InferredType type, char[] newTypeName) {
		// rename constructor on type if there is one
		InferredMethod constructor = type.findMethod(TypeConstants.INIT, null);
		if (constructor != null) {
			constructor.name = newTypeName;
		}

		// rename the type
		compUnit.inferredTypesHash.removeKey(type.name);
		type.name = newTypeName;
		compUnit.inferredTypesHash.put(newTypeName, type);
	}

	/**
	 * <p>
	 * Converts the given anonymous type to a named global type. If the given
	 * type is not anonymous then this is a no-op.
	 * </p>
	 * 
	 * @param type
	 *            anonymous {@link InferredType} to name
	 * @param newTypeName
	 *            new type name for the given anonymous {@link InferredType}
	 * 
	 * @see InferredType#isAnonymous
	 */
	protected void convertAnonymousTypeToNamed(InferredType type, char[] newTypeName) {
		if (type.isAnonymous) {
			this.renameType(type, newTypeName);
			type.isAnonymous = false;
			type.setIsGlobal(true);
		}
	}

	protected boolean isMatch(IExpression expr, char[][] names, int index) {
		char[] matchName = names[index];
		if (expr instanceof SingleNameReference) {
			SingleNameReference snr = (SingleNameReference) expr;
			return CharOperation.equals(snr.token, matchName);
		}
		else if (expr instanceof FieldReference && names.length > 1 && index > 0) {
			FieldReference fieldReference = (FieldReference) expr;
			if (CharOperation.equals(fieldReference.token, matchName)) {
				return isMatch(fieldReference.receiver, names, index - 1);
			}

		}
		return false;
	}

	/**
	 * @deprecated not used
	 */
	protected boolean isFunction(IFunctionCall messageSend, String string) {
		String[] names = string.split("\\."); //$NON-NLS-1$
		char[] functionName = names[names.length - 1].toCharArray();
		if (!CharOperation.equals(functionName, messageSend.getSelector())) {
			return false;
		}

		char[][] namesChars = new char[names.length][];
		for (int i = 0; i < namesChars.length; i++) {
			namesChars[i] = names[i].toCharArray();
		}
		if (names.length > 1) {
			return isMatch(messageSend.getReceiver(), namesChars, namesChars.length - 2);
		}
		return true;
	}

	protected boolean isFunction(IFunctionCall messageSend, char[][] names) {
		if (messageSend == null) {
			return false;
		}

		char[] functionName = names[names.length - 1];
		if (!CharOperation.equals(functionName, messageSend.getSelector())) {
			return false;
		}

		if (names.length > 1) {
			return isMatch(messageSend.getReceiver(), names, names.length - 2);
		}

		return true;
	}

	public void doInfer() {
		try {
			long time0 = 0;
			if (REPORT_INFER_TIME) {
				time0 = System.currentTimeMillis();
			}
			clearBuiltInTypes();

			ASTVisitor visitor = getVisitor(compUnit);
			if (visitor != null)
				compUnit.traverse(visitor);

			passNumber = 2;
			visitor = getVisitor(compUnit);
			if (visitor != null)
				compUnit.traverse(visitor);

			for (int i = 0; i < compUnit.numberInferredTypes; i++) {
				if (compUnit.inferredTypes[i].sourceStart < 0)
					compUnit.inferredTypes[i].sourceStart = 0;
			}

			if (REPORT_INFER_TIME) {
				long time = System.currentTimeMillis() - time0;
				System.err.println(getClass().getName() + " inferred " + new String(compUnit.getFileName()) + " in " //$NON-NLS-1$ //$NON-NLS-2$
							+ time + "ms"); //$NON-NLS-1$
			}
			this.compUnit = null;

		}
		catch (RuntimeException e) {
			org.eclipse.wst.jsdt.internal.core.util.Util.log(e, "error during type inferencing"); //$NON-NLS-1$
		}
	}

	protected InferredType addType(char[] className) {
		return addType(className, false);
	}

	/**
	 * Create a new inferred type with the given name
	 * 
	 * @param className
	 *            the name of the inferred type
	 * @param isDefinition
	 *            true if this unit defines the type
	 * @return new Inferred type
	 */
	protected InferredType addType(char[] className, boolean isDefinition) {
		InferredType type = compUnit.addType(className, isDefinition, this.inferenceProvider.getID());

		return type;
	}

	protected final void pushContext() {
		Context newContext = new Context(currentContext);
		contexts[++contextPtr] = currentContext;
		currentContext = newContext;

	}

	protected final void popContext() {
		currentContext = contexts[contextPtr];
		contexts[contextPtr--] = null;
	}


	/**
	 * @deprecated not used internally, will be removed
	 */
	protected final boolean isInNamedMethod() {
		return this.currentContext.currentMethod != null && this.currentContext.currentMethod.getName() != null;
	}

	/**
	 * Finds a Var Declaration on the context from the name represented with
	 * the expression
	 */
	protected IAbstractVariableDeclaration getVariable(IExpression expression) {
		char[] name = null;

		if (expression instanceof ISingleNameReference) {
			name = ((ISingleNameReference) expression).getToken();
		}
		else if (expression instanceof IFieldReference) {
			IReference ref = getRoot((IFieldReference) expression);
			if (ref != null && ref instanceof SingleNameReference) {
				name = ((SingleNameReference) ref).getToken();
			}
		}

		if (name != null) {
			Object var = this.currentContext.getMember(name);
			if (var instanceof IAbstractVariableDeclaration) {
				return (IAbstractVariableDeclaration) var;
			}

		}
		return null;

	}

	/**
	 * Finds a assignment on the context from the name represented with the
	 * expression
	 */
	protected IAssignment getAssignment(IExpression expression) {
		char[] name = null;

		if (expression instanceof ISingleNameReference) {
			name = ((ISingleNameReference) expression).getToken();
		}
		else if (expression instanceof IFieldReference) {
			IReference ref = getRoot((IFieldReference) expression);
			if (ref != null && ref instanceof SingleNameReference) {
				name = ((SingleNameReference) ref).getToken();
			}
		}

		if (name != null) {
			Object assignment = this.currentContext.getMember(name);
			if (assignment instanceof IAssignment) {
				return (IAssignment) assignment;
			}
		}

		return null;
	}

	/**
	 * <p>
	 * Finds a Function Declaration on the context from the name represented
	 * with the expression
	 * </p>
	 * 
	 * <p>
	 * Supported:
	 * <ul>
	 * <li>{@link ISingleNameReference}</li>
	 * <li>{@link ILocalDeclaration}</li>
	 * </ul>
	 * </p>
	 */
	protected IAbstractFunctionDeclaration getFunction(IExpression expression) {
		IAbstractFunctionDeclaration function = null;

		char[] name = null;
		if (expression instanceof ISingleNameReference) {
			name = ((ISingleNameReference) expression).getToken();

			Object method = this.currentContext.getMember(name);
			if (method instanceof IAbstractFunctionDeclaration) {
				function = (IAbstractFunctionDeclaration) method;
			}
			else if (method instanceof ILocalDeclaration) {
				IExpression init = ((ILocalDeclaration) method).getInitialization();
				if (init instanceof IFunctionExpression) {
					function = ((IFunctionExpression) init).getMethodDeclaration();
				}
			}
		}
		else if (expression instanceof IFieldReference) {
			InferredType receiverType = this.getReceiverType(((IFieldReference) expression).getReceiver(), false);
			if (receiverType != null) {
				InferredMethod inferredMethod = receiverType.findMethod(((IFieldReference) expression).getToken(), null);
				if (inferredMethod != null) {
					function = inferredMethod.getFunctionDeclaration();
				}
			}
			name = ((IFieldReference) expression).getToken();
		}
		else if (expression instanceof MessageSend) {
			name = ((MessageSend) expression).selector;
			if (name != null) {
				// look up the function in the current context, we want to get
				// to its return statement eventually.
				Object method = this.currentContext.getMember(name);
				if (method instanceof IAbstractFunctionDeclaration) {
					IAbstractFunctionDeclaration calledFunction = (IAbstractFunctionDeclaration) method;
					if (calledFunction.getStatements() != null) {
						IProgramElement[] statements = calledFunction.getStatements();
						// this is a bit of a guess, but look for a return
						// statement
						// if found and it is returning a function, then use
						// that
						// if not we just return nothing, no harm done.
						for (int i = 0; i < statements.length; i++) {
							if (statements[i] instanceof IReturnStatement) {
								if (((IReturnStatement) statements[i]).getExpression() instanceof IFunctionExpression) {
									function = ((IFunctionExpression) ((IReturnStatement) statements[i]).getExpression()).getMethodDeclaration();
									break;
								}
							}
						}
					}
				}
			}
		}

		return function;
	}

	private void buildDefinedMembers(IProgramElement[] statements, IArgument[] arguments) {

		if (arguments != null) {
			for (int i = 0; i < arguments.length; i++) {
				this.currentContext.addMember(arguments[i].getName(), arguments[i]);
			}
		}
		if (statements != null) {
			for (int i = 0; i < statements.length; i++) {
				if (statements[i] instanceof ILocalDeclaration) {
					ILocalDeclaration local = (ILocalDeclaration) statements[i];
					this.currentContext.addMember(local.getName(), local);
				}
				else if (statements[i] instanceof IAbstractFunctionDeclaration) {
					IAbstractFunctionDeclaration method = (IAbstractFunctionDeclaration) statements[i];
					if (method.getName() != null)
						this.currentContext.addMember(method.getName(), method);
				}
			}
		}
	}

	private static boolean isThis(IExpression expression) {
		return expression instanceof ASTNode && ((ASTNode) expression).isThis();
	}

	/*
	 * This method is used to determined the inferred type of a LHS
	 * Expression.
	 * 
	 * It could return null.
	 * 
	 * a.b.c
	 */
	private InferredType getInferredType(IExpression expression) {

		InferredType type = null;

		/* this */
		if (expression instanceof IThisReference) {
			type = this.currentContext.currentType;
		}
		/* foo (could be a Type name or a reference to a variable) */
		else if (expression instanceof SingleNameReference) {
			// check for local variable first
			IAbstractVariableDeclaration varDecl = this.getVariable(expression);
			if (varDecl != null) {
				type = varDecl.getInferredType();
			}

			// if not found type yet check for assignment
			IAssignment assignment = null;
			if (type == null) {
				assignment = getAssignment(expression);
				if (assignment != null) {
					type = assignment.getInferredType();
				}
			}

			/*
			 * if not found type yet and there was no declaration or
			 * assignment, or the found type is Function then check if there
			 * is a known type with the same name as the SNR.
			 */
			if ((type == null && varDecl == null && assignment == null) || (type != null && type.isFunction())) {

				char[] possibleTypeName = constructTypeName(expression);
				if (possibleTypeName != null) {
					/*
					 * if pre-existing type, use that else check if known type
					 * name
					 */
					InferredType existingType = compUnit.findInferredType(possibleTypeName);
					if (existingType != null) {
						type = existingType;
					}
					else if (WellKnownTypes.containsKey(possibleTypeName) || this.isKnownType(possibleTypeName)) {

						type = addType(possibleTypeName, false);
					}
				}
			}
		}
		/* foo.bar.xxx... */
		else if (expression instanceof FieldReference) {
			char[] possibleTypeName = constructTypeName(expression);

			if (possibleTypeName != null) {
				// search the defined types in the context
				type = compUnit.findInferredType(possibleTypeName);
			}

			if (type == null && isPossibleClassName(possibleTypeName)) {
				type = addType(possibleTypeName, true);
			}

			/*
			 * Continue the search by trying to resolve further down the name
			 * because this token of the field reference could be a member of
			 * a type or instance of a type
			 */
			if (type == null) {
				FieldReference fRef = (FieldReference) expression;

				// this
				InferredType parentType = getInferredType(fRef.receiver);

				if (parentType != null) {
					// check the members and return type
					InferredAttribute typeAttribute = parentType.findAttribute(fRef.token);

					if (typeAttribute != null) {
						type = typeAttribute.type;
					}
				}
			}

		}

		return type;
	}

	/**
	 * @deprecated - here for compatibility
	 */
	protected InferredType getInferredType2(IExpression fieldReceiver) {
		InferredType receiverType = null;
		IAbstractVariableDeclaration var = getVariable(fieldReceiver);
		if (var != null) {
			receiverType = createAnonymousType(var);
		}
		else {
			if (this.inferredGlobal != null && fieldReceiver instanceof ISingleNameReference) {
				char[] name = ((ISingleNameReference) fieldReceiver).getToken();
				InferredAttribute attr = this.inferredGlobal.findAttribute(name);
				if (attr != null) {
					receiverType = attr.type;
				}
			}

		}
		return receiverType;
	}

	protected boolean isKnownType(char[] possibleTypeName) {
		return false;
	}

	/*
	 * For SNR it returns the name For FR it construct a Qualified name
	 * separated by '.'
	 * 
	 * If at any point it hits a portion of the Field reference that is not
	 * supported (such as a function call, a prototype, or this )
	 */
	protected final char[] constructTypeName(IExpression expression) {
		return Util.getTypeName(expression);
	}

	public boolean visit(IObjectLiteral literal) {
		if (this.passNumber == 1 && literal.getInferredType() == null) {
			createAnonymousType(literal);
		}
		pushContext();
		this.currentContext.currentType = literal.getInferredType();
		return true;
	}

	public void endVisit(IObjectLiteral literal) {
		popContext();
	}

	/**
	 * Overridden by client who wish to update the infer options
	 * 
	 * @param options
	 */
	public void initializeOptions(InferOptions options) {
	}

	protected boolean isPossibleClassName(char[] name) {
		return false;
	}

	/**
	 * Get the Script file this inference is being done on
	 * 
	 * @return
	 */
	public IScriptFileDeclaration getScriptFileDeclaration() {
		return this.compUnit;
	}

	public InferredType findDefinedType(char[] className) {
		return compUnit.findInferredType(className);
	}

	protected Object findDefinedMember(char[] memberName) {
		return currentContext.getMember(memberName);
	}

	protected char[] changePrimitiveToObject(char[] name) {
		/*
		 * Changes the first character of the name of the primitive types to
		 * uppercase. This will allow future reference to the object wrapper
		 * instead of the primitive type.
		 */
		if (CharOperation.equals(name, TypeConstants.BOOLEAN, false)) {
			return this.getBooleanType().getName();
		}

		return name;
	}

	/**
	 * @return {@link InferredType} for the String type
	 */
	protected InferredType getStringType() {
		if (fStringType == null) {
			fStringType = this.addType(TypeConstants.JAVA_LANG_STRING[0]);
		}

		return fStringType;
	}

	/**
	 * @return {@link InferredType} for the Number type
	 */
	protected InferredType getNumberType() {
		if (fNumberType == null) {
			fNumberType = this.addType(TypeConstants.NUMBER[0]);
		}

		return fNumberType;
	}

	/**
	 * @return {@link InferredType} for the Boolean type
	 */
	protected InferredType getBooleanType() {
		if (fBooleanType == null) {
			fBooleanType = this.addType(TypeConstants.BOOLEAN_OBJECT[0]);
		}

		return fBooleanType;
	}

	/**
	 * @return {@link InferredType} for the Function type
	 */
	protected InferredType getFunctionType() {
		if (fFunctionType == null) {
			fFunctionType = this.addType(TypeConstants.FUNCTION[0]);
		}

		return fFunctionType;
	}

	/**
	 * @return {@link InferredType} for the Array type
	 */
	protected InferredType getArrayType() {
		if (fArrayType == null) {
			fArrayType = this.addType(TypeConstants.ARRAY[0]);
		}

		return fArrayType;
	}

	/**
	 * @return {@link InferredType} for the Void type
	 */
	protected InferredType getVoidType() {
		if (fVoidType == null) {
			fVoidType = this.addType(TypeConstants.VOID);
		}

		return fVoidType;
	}

	/**
	 * @return {@link InferredType} for the Object type
	 */
	protected InferredType getObjectType() {
		if (fObjectType == null) {
			fObjectType = this.addType(TypeConstants.OBJECT);
		}

		return fObjectType;
	}

	/**
	 * <p>
	 * Gets the name of the given expression.
	 * </p>
	 * 
	 * <p>
	 * Supported:
	 * <ul>
	 * <li>{@link ISingleNameReference}</li>
	 * <li>{@link IFieldReference}</li>
	 * <li>{@link IAssignment}</li>
	 * </ul>
	 * </p>
	 * 
	 * @param expression
	 *            {@link IExpression} to get the name for
	 * 
	 * @return name of the given {@link IExpression} or <code>null</code> if
	 *         none can be determined
	 */
	protected char[] getName(IExpression expression) {
		char[] name = null;

		if (expression instanceof ISingleNameReference) {
			name = ((ISingleNameReference) expression).getToken();
		}
		else if (expression instanceof IFieldReference) {
			name = ((IFieldReference) expression).getToken();
		}
		else if (expression instanceof IAssignment) {
			name = this.getName(((IAssignment) expression).getLeftHandSide());
		}

		return name;
	}

	/**
	 * <p>
	 * Given a variable name and an initialization expression determines the
	 * type to initialize the variable with the given name to.
	 * </p>
	 * 
	 * @param variableName
	 *            name of the variable to be initialized
	 * @param initialization
	 *            the initialization expression used to initialize the
	 *            variable with the given name
	 * 
	 * @return {@link InferredType} to initialize the variable with the given
	 *         name with
	 */
	private InferredType getTypeForVariableInitialization(char[] variableName, IExpression initialization) {
		InferredType type = this.getTypeOf(initialization);

		/*
		 * if the initialization type is not indexed and the variable
		 * declaration is in the global scope rename the type to a globally
		 * anonymous type and mark it as a global type
		 */
		boolean isGlobal = this.isGlobal(variableName);
		if (type != null && !type.isIndexed() && isGlobal) {
			char[] globalTypeName = createAnonymousGlobalTypeName(variableName);
			this.renameType(type, globalTypeName);
			type.isAnonymous = true;
			type.setIsGlobal(true);
		}
		else if (type == null && isGlobal && this.passNumber == 2) {
			type = this.createAnonymousGlobalType(variableName);
		}

		return type;
	}

	/**
	 * <p>
	 * Will create a type to assign to given the receiver that is being
	 * assigned to, the current type that is or will be assigned to the
	 * receiver, and the assignment expression that will be used for the
	 * assignment.
	 * </p>
	 * 
	 * <p>
	 * <b>NOTE:</b> This does not actually deal with the assignment, it just
	 * creates the type to assign to and updates the type for the given
	 * receiver.
	 * </p>
	 * 
	 * @param receiver
	 *            the receiver to update the type for
	 * @param currentReceiverType
	 *            current type that is or will be assigned to the receiver
	 * 
	 * @return {@link InferredType} that was created to do the assignment to
	 *         or the given current receiver type if no new type was created
	 */
	private InferredType createTypeToAssignTo(IExpression receiver, InferredType currentReceiverType) {
		char[] varName = this.getName(receiver);
		InferredType newType = null;

		/*
		 * If the current receiver type is not anonymous and is not a this
		 * statement, and the receiver is not a type (rather then instance of
		 * a type, then create a new anonymous type with current type as the
		 * parent.
		 * 
		 * else use the given current receiver type
		 */
		if (!currentReceiverType.isAnonymous && !isThis(receiver) && !CharOperation.equals(currentReceiverType.name, varName) && !this.isExpressionAType(receiver)) {

			/*
			 * if the variable being assigned to is in the global scope then
			 * need to create a global type
			 * 
			 * else create a local anonymous type
			 */
			boolean isGlobal = this.isGlobal(varName);
			if (isGlobal) {
				/*
				 * if current type is a function create a new type with the
				 * var name else create new anonymous global type
				 */
				if (currentReceiverType.isFunction()) {
					newType = this.addType(varName, true);
				}
				else {
					newType = this.createAnonymousGlobalType(varName);
				}
			}
			else {
				newType = this.createAnonymousType(receiver, currentReceiverType);
			}
		}

		/*
		 * if created a new type set its super type to the original type and
		 * update the type for the receiver
		 * 
		 * else just return the original given current receiver type
		 */
		if (newType != null) {
			newType.setSuperType(currentReceiverType);
			this.setTypeOf(receiver, newType);
		}
		else {
			newType = currentReceiverType;
		}

		return newType;
	}

	/**
	 * <p>
	 * Clears out all of the built in types.
	 * </p>
	 */
	private void clearBuiltInTypes() {
		fStringType = null;
		fNumberType = null;
		fBooleanType = null;
		fFunctionType = null;
		fArrayType = null;
		fVoidType = null;
		fObjectType = null;
	}

	/**
	 * <p>
	 * This method is intended to take a chain of field references and
	 * determine the type that the last field should be or is defined on.
	 * </p>
	 * 
	 * <p>
	 * EX: <code>foo.bar.awesome.crazy = 42;</code><br>
	 * <br>
	 * If that is the entirety of the file and the receiver of the
	 * <code>foo.bar.awesome.crazy</code> statement is given to this function,
	 * so the <code>foo.bar.awesome</code> part, then this function will
	 * create a <code>foo</code> field on the global inferred type and and
	 * then give it a type that has a <code>bar</code> field, and then give
	 * the <code>bar</code> field a type with an <code>awesome</code> field
	 * and then finally return a new type assigned to the <code>awesome</code>
	 * field such that some other code can deal with assigning the
	 * <code>crazy</code> field with whatever type is on the right hand side
	 * of the assignment.
	 * </p>
	 * 
	 * @param receiver
	 *            the receiver side of a {@link FieldReference} to get the
	 *            type for
	 * 
	 * @param defineRoot
	 *            Has two purposes. If the root of the field reference has no
	 *            type and this is <code>true</code> a type will be created.
	 *            If the root of this field reference does not have a type and
	 *            this is <code>false</code> no root type will be created an
	 *            thus no type will be returned by this method. When there is
	 *            a root type if this argument is <code>true</code> then the
	 *            type on the root will be marked as a definition, else if
	 *            <code>false</code> the root will not be marked as a
	 *            definition.
	 * 
	 * @return {@link InferredType} associated with the given receiver side of
	 *         a {@link FieldReference}
	 */
	protected InferredType getReceiverType(IExpression receiver, boolean defineRoot) {
		InferredType receiverType = null;
		IExpression current = receiver;
		List recievers = new ArrayList();
		InferredType rootType = null;

		/*
		 * Find the SingleNameReference that the reference chain starts with
		 * or the root type if the chain starts with a THIS statement
		 */
		while (current != null && !(current instanceof SingleNameReference) && rootType == null) {
			if (current instanceof FieldReference) {
				recievers.add(current);
				current = ((FieldReference) current).getReceiver();
			}
			else if (current instanceof ThisReference) {
				rootType = this.currentContext.currentType;
				((ThisReference) current).setInferredType(rootType);
				current = null;
			}
			else {
				current = null;
			}
		}

		// if the root was a single name then determine or create its type
		IAbstractVariableDeclaration rootVarDecl = null;
		IAssignment rootAssignment = null;
		if (rootType == null && current instanceof SingleNameReference) {

			// first check if there is a type with the same name as the single
			// name reference
			char[] name = ((SingleNameReference) current).getToken();
			rootType = this.findDefinedType(name);

			// if the single name reference is a function then create a type
			// using the function name
			if (rootType == null) {
				IFunctionDeclaration func = this.getDefinedFunction(current);
				if (func != null) {
					rootType = this.addType(name, true);
					rootType.setNameStart(current.sourceStart());
					rootType.setSourceEnd(current.sourceEnd());
				}
			}

			// if single name reference is to a variable
			if (rootType == null) {
				rootVarDecl = this.getVariable(current);
				if (rootVarDecl != null) {
					rootType = rootVarDecl.getInferredType();

					/*
					 * if variable does not already have an inferred type
					 * create an anonymous one or if the receiver type is not
					 * anonymous and is the expression is not a type then
					 * create a new anonymous sub type to do the assignment to
					 */

					if (rootType == null || (!rootType.isAnonymous && !rootVarDecl.isType())) {
						/*
						 * if global then create anonymous global type and set
						 * super type as current root type else create
						 * anonymous type using current root type as super
						 * type
						 */
						boolean isGlobal = this.isGlobal(name);
						if (isGlobal) {
							InferredType globalType = this.createAnonymousGlobalType(name);
							globalType.setSuperType(rootType);
							rootType = globalType;
							rootType.setIsDefinition(true);
							rootVarDecl.setInferredType(rootType);
						}
						else if (defineRoot) {
							rootType = this.createAnonymousType(current, rootType);
							rootType.setIsDefinition(true);
							rootVarDecl.setInferredType(rootType);
						}
					}
				}
			}

			// if single name reference is to a field on the inferred 'global'
			// type
			if (rootType == null && this.inferredGlobal != null) {
				InferredAttribute attr = this.getInferredGlobal(false).findAttribute(name);
				if (attr != null) {
					if (attr.type == null) {
						attr.type = this.createAnonymousGlobalType(attr.name);
					}
					rootType = attr.type;
				}
				else {
					InferredMethod meth = this.getInferredGlobal(false).findMethod(name, null);
					if (meth != null) {
						rootType = this.addType(name, true);
						rootType.setNameStart(current.sourceStart());
						rootType.setSourceEnd(current.sourceEnd());
					}
				}
			}

			// if single name reference is to an existing assignment with no
			// declaration
			if (rootType == null) {
				rootAssignment = this.getAssignment(current);
				if (rootAssignment != null) {
					rootType = rootAssignment.getInferredType();
				}
			}

			/* else if define root create a new anonymous global type */
			if (rootType == null && defineRoot) {
				rootType = this.createAnonymousGlobalType(name);
			}
		}

		/*
		 * if determined a root type, and there is no case where we should
		 * not, make sure there is fields and types built up for the rest of
		 * the reference chain
		 */
		if (rootType != null) {
			/*
			 * only define root if requested and root not part of long chain,
			 * or the root was already defined somewhere else in this file
			 * 
			 * IE: foo.bar.blarg = 42, foo is not defined here foo.bar = 42,
			 * foo is defined here
			 */
			if (defineRoot && (recievers.isEmpty() || rootVarDecl != null || rootAssignment != null)) {
				rootType.setIsDefinition(true);
			}

			InferredType currentType = rootType;
			for (int i = recievers.size() - 1; i >= 0; --i) {
				FieldReference ref = (FieldReference) recievers.get(i);

				InferredAttribute attr = currentType.findAttribute(ref.getToken());
				if (attr != null) {
					if (attr.type != null) {
						currentType = attr.type;
					}
					else {
						attr.type = this.createAnonymousType(attr, null);
						currentType = attr.type;
					}
				}
				else {
					InferredMethod meth = currentType.findMethod(ref.getToken(), null);
					if (meth != null) {
						char[] typeName = CharOperation.concatWith(ref.asQualifiedName(), '.');
						currentType = this.addType(typeName, true);
					}
					else {
						attr = currentType.addAttribute(ref.getToken(), ref, ref.sourceStart);

						if (currentType == this.getInferredGlobal(false)) {
							attr.type = this.createAnonymousGlobalType(attr.name);
							attr.type.setIsDefinition(true);
						}
						else {
							attr.type = this.createAnonymousType(attr, null);
						}
						currentType = attr.type;
					}
				}
			}

			// set the last type in the chain as the receiver type
			receiverType = currentType;
		}

		return receiverType;
	}

	/**
	 * @param define
	 *            <code>true</code> to define the inferred global type if one
	 *            is not yet defined, <code>false</code> otherwise
	 * 
	 * @return inferred global type, or <code>null</code> if none is yet
	 *         defined and <code>define</code> was given as <code>false</code>
	 */
	protected InferredType getInferredGlobal(boolean define) {
		if (this.inferredGlobal == null && define) {
			this.inferredGlobal = addType(IIndexConstants.GLOBAL_SYMBOL, true);
			this.inferredGlobal.isAnonymous = true;
			this.inferredGlobal.setIsGlobal(true);
		}

		return this.inferredGlobal;
	}

	/**
	 * <p>
	 * Determine if the root of the given expression is global or not.
	 * </p>
	 * 
	 * @param expr
	 *            Determine if the root of this expression is global or not
	 * 
	 * @return <code>true</code> if the root of the given expression is
	 *         global, <code>false</code> otherwise
	 */
	private boolean isRootGlobal(IExpression expr) {
		boolean isGlobal = false;

		IReference root = null;
		if (expr instanceof SingleNameReference) {
			root = (SingleNameReference) expr;
		}
		else if (expr instanceof FieldReference) {
			root = getRoot((FieldReference) expr);
		}

		/*
		 * if root is a single name reference then determine if it is global
		 * else if no root then assume global
		 */
		if (root != null && root instanceof ISingleNameReference) {
			isGlobal = this.isGlobal(((ISingleNameReference) root).getToken());
		}
		else if (root != null && root instanceof IThisReference) {
			isGlobal = this.currentContext.currentType == this.getInferredGlobal(false);
		}
		else if (root == null) {
			isGlobal = true;
		}

		return isGlobal;
	}

	/**
	 * <p>
	 * Determines if the given variable name is global.
	 * </p>
	 * 
	 * @param name
	 *            determine if there is a global variable with this name
	 * 
	 * @return <code>true</code> if there is a global variable with this name,
	 *         <code>false</code> otherwise
	 */
	protected boolean isGlobal(char[] name) {
		boolean isGlobal = false;

		if (name == null)
			return isGlobal;

		// check the root context
		Object globalMember;
		Object currentContextMember = this.currentContext.getMember(name);
		if (this.contexts[0] != null) {
			globalMember = this.contexts[0].getMember(name);
		}
		else {
			globalMember = currentContextMember;
		}

		/*
		 * is global if global member with same name is not null and from the
		 * current context the first member with that name is also the global
		 * member, this is to cover the case with a shadowing local variable
		 */
		isGlobal = globalMember != null && currentContextMember == globalMember;

		/*
		 * if not determined to be global then check if the current context
		 * member is an assignment. If it is then that assignment is an
		 * assignment without a local declaration which means it is global
		 */
		if (!isGlobal) {
			isGlobal = currentContextMember instanceof Assignment;
		}

		// if not determined to be global yet then check inferred global type
		if (!isGlobal) {
			globalMember = null;
			InferredType inferredGlobal = this.getInferredGlobal(false);
			if (inferredGlobal != null) {
				globalMember = inferredGlobal.findAttribute(name);

				if (globalMember == null) {
					globalMember = inferredGlobal.findMethod(name, null);
				}
			}

			isGlobal = globalMember != null;
		}

		/*
		 * if not determined to be global yet and no global member or local
		 * member found assume to be global
		 */
		if (!isGlobal) {
			if (globalMember == null && currentContextMember == null) {
				isGlobal = true;
			}
		}

		return isGlobal;
	}

	/**
	 * <p>
	 * Given an {@link IFieldReference} finds the root
	 * {@link SingleNameReference}.
	 * </p>
	 * 
	 * @param ref
	 *            {@link IFieldReference} to find the root
	 *            {@link SingleNameReference} for
	 * 
	 * @return {@link SingleNameReference} that is the root of the given
	 *         {@link IFieldReference}, or <code>null</code> if it can not be
	 *         found.
	 */
	private static IReference getRoot(IFieldReference ref) {
		IReference root = null;

		IExpression current = ref;
		while (root == null && current != null) {
			if (current instanceof IFieldReference) {
				current = ((IFieldReference) current).getReceiver();
			}
			else if (current instanceof SingleNameReference) {
				root = (SingleNameReference) current;
			}
			else if (current instanceof IThisReference) {
				root = (IThisReference) current;
			}
			else {
				current = null;
			}
		}

		return root;
	}

	/**
	 * <p>
	 * Determines if the given parent expression contains or is the given
	 * needle expression.
	 * </p>
	 * 
	 * <p>
	 * Handles:
	 * <ul>
	 * <li>{@link IBinaryExpression}</li>
	 * </ul>
	 * </p>
	 * 
	 * @param parent
	 *            determine if the given needle is or is contained by this
	 *            parent expression
	 * @param needle
	 *            determine if the this needle is or is contained by the given
	 *            parent expression
	 * 
	 * @return <code>true</code> if the given needle is or is contained by the
	 *         given parent expression, <code>false</code> otherwise
	 */
	private static boolean expressionContains(IExpression parent, IExpression needle) {
		boolean contains = false;

		LinkedList expressions = new LinkedList();
		expressions.add(parent);

		while (expressions.size() > 0 && !contains) {
			IExpression current = (IExpression) expressions.removeFirst();

			contains = needle == current;

			if (!contains) {
				if (current instanceof IBinaryExpression) {
					expressions.add(((IBinaryExpression) current).getLeft());
					expressions.add(((IBinaryExpression) current).getRight());
				}
				else if (current instanceof Assignment) {
					expressions.add(((Assignment) current).getLeftHandSide());
					expressions.add(((Assignment) current).getExpression());
				}
			}
		}

		return contains;
	}

	/**
	 * <p>
	 * Determines if the given expression is a type rather then an instance of
	 * a type.
	 * </p>
	 * 
	 * @param expr
	 *            determine if this expression is a type rather then an
	 *            instance of a type
	 * 
	 * @return <code>true</code> if the given expression is a type,
	 *         <code>false</code> if the given expression is the instance of a
	 *         type or unknown
	 */
	private boolean isExpressionAType(IExpression expr) {
		boolean isType = false;

		if (expr instanceof IAssignment) {
			isType = ((IAssignment) expr).isType();
		}
		else if (expr instanceof IAbstractVariableDeclaration) {
			isType = ((IAbstractVariableDeclaration) expr).isType();
		}
		else if (expr instanceof IFieldReference) {
			IExpression receiver = ((IFieldReference) expr).getReceiver();
			InferredType receiverType = this.getTypeOf(receiver);
			if (receiverType != null) {
				InferredAttribute attr = receiverType.findAttribute(((IFieldReference) expr).getToken());
				isType = attr != null && attr.isType();
			}
		}
		else if (expr instanceof ISingleNameReference) {
			IAbstractVariableDeclaration varDecl = this.getVariable(expr);
			if (varDecl != null) {
				isType = varDecl.isType();
			}

			if (!isType) {
				IAssignment assign = this.getAssignment(expr);
				if (assign != null) {
					isType = assign.isType();
				}
			}

			if (!isType) {
				IAbstractFunctionDeclaration funcDecl = this.getFunction(expr);
				if (funcDecl != null && funcDecl.getName() != null) {
					InferredType typeDefinedByFunc = this.findDefinedType(funcDecl.getName());
					isType = typeDefinedByFunc != null;
				}
			}

			if (!isType) {
				InferredType existingType = this.compUnit.findInferredType(((ISingleNameReference) expr).getToken());
				isType = existingType != null;
			}
		}
		else if (expr instanceof IThisReference) {
			isType = true;
		}

		return isType;
	}

	protected InferredType createAnonymousTypeForMixin(IExpression mixInto, InferredType parentType) {
		InferredType mixIntoType;
		IAbstractVariableDeclaration localVar = getVariable(mixInto);
		char[] varName = getName(mixInto);
		if (mixInto instanceof ISingleNameReference && (localVar == null || isGlobal(varName))) {
			mixIntoType = createAnonymousGlobalType(varName);
			mixIntoType.setSuperType(parentType);
		}
		else {
			mixIntoType = createAnonymousType(mixInto, parentType);
		}
		mixIntoType.isObjectLiteral = false;
		return mixIntoType;
	}

	protected InferredType getAttributeType(char[] attName, IExpression receiver, boolean defineRoot) {
		InferredType attrType = null;
		InferredType receiverType = this.getReceiverType(receiver, defineRoot);
		if (receiverType != null) {
			InferredAttribute attr = receiverType.findAttribute(attName);
			if (attr != null && attr.type != null) {
				attrType = attr.type;
			}
		}
		return attrType;
	}

	/**
	 * Return a visitor to traverse the given compilation unit's AST.
	 * Subclasses may override to provide a more minimal implementations while
	 * retaining use of utility methods from the base InferEngine.
	 * 
	 * @return a visitor for use with the given compilation unit and current
	 *         engine states, or <code>null</code>
	 */
	protected ASTVisitor getVisitor(CompilationUnitDeclaration compilationUnit) {
		return this;
	}
}