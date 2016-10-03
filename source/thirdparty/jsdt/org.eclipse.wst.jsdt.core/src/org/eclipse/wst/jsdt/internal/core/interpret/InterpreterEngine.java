/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.interpret;

import org.eclipse.wst.jsdt.core.UnimplementedException;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Assignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Block;
import org.eclipse.wst.jsdt.internal.compiler.ast.BreakStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ContinueStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.EqualExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.FalseLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.FunctionExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.IfStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.IntLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.MessageSend;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.NullLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteralField;
import org.eclipse.wst.jsdt.internal.compiler.ast.OperatorIds;
import org.eclipse.wst.jsdt.internal.compiler.ast.PostfixExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.PrefixExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.ProgramElement;
import org.eclipse.wst.jsdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.StringLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.ThisReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.UndefinedLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.WhileStatement;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.core.interpret.builtin.BuiltInString;

public class InterpreterEngine extends ASTVisitor implements Contants{

	
	protected InterpreterContext context;
	InterpreterResult result = new InterpreterResult();

	static final int STOP_RETURN =1;
	static final int STOP_BREAK =2;
	static final int STOP_CONTINUE =3;
	static final int STOP_THROW =4;
	
	
	
	
	
	class ExprStackItem extends Value{
		ValueReference reference;
		char [] referenceName;
		int value;
		Object objValue;
		
		ExprStackItem()
		{super(0);}
		
		public int numberValue()
		{
			switch (type)
			{
			case Value.BOOLEAN:
			case Value.NUMBER:
				return value;
			case Value.STRING:
				return Integer.valueOf((String)objValue).intValue();
			default:
				throw new UnimplementedException();
			}
		}
		public String stringValue()
		{
			switch (type)
			{
			case Value.BOOLEAN:
				return value!=0 ?"true":"false"; //$NON-NLS-1$ //$NON-NLS-2$
			case Value.NUMBER:
				return String.valueOf(value);
			case Value.STRING:
				return (String)objValue;
			default:
				throw new UnimplementedException();
			}
		}

		public boolean booleanValue()
		{
			switch (type)
			{
			case Value.BOOLEAN:
			case Value.NUMBER:
				return value!=0 ;
			case Value.STRING:
				return ((String)objValue).length()!=0;
			default:
				throw new UnimplementedException();
			}
		}
		
		public ObjectValue getObjectValue()
		{
			
			
			switch (type)
			{
			case OBJECT:
			case FUNCTION:
				return (ObjectValue)objValue;
			case UNDEFINED:
				throw new InterpretException("null reference"); //$NON-NLS-1$
			case BOOLEAN:
			{
				ObjectValue obj= new ObjectValue();
				obj.setValue(VALUE_ARR,this);
				return obj;
			}
			case NUMBER:
			{
				ObjectValue obj= new ObjectValue();
				obj.setValue(VALUE_ARR,this);
				return obj;
			}
			case STRING:
			{
						ObjectValue obj= new ObjectValue(BuiltInString.prototype);
						obj.setValue(VALUE_ARR,new StringValue(this.stringValue()));
						return obj;
			}
			}	
			throw new UnimplementedException();
		
		
		}
		
		Value getValue()
		{
			
			
			switch (type)
			{
			case NULL:
			case UNDEFINED:
				return (Value)objValue;
			case BOOLEAN:
				return new BooleanValue(value!=0);
			case NUMBER:
				return new NumberValue(value);
			case STRING:
				return new StringValue((String)objValue);
			case OBJECT:
				return (ObjectValue)objValue;
			case FUNCTION:
				return (FunctionValue)objValue;
			}
				
			throw new UnimplementedException();
		
		
		}
	
		public Object valueObject() {
			return objValue;
		}


	}
	
	ExprStackItem []stack=new ExprStackItem[30];
	int stackPtr=-1;
	
	
	public InterpreterEngine(InterpreterContext context) {
		this.context=context;
		
		for (int i=0;i<stack.length;i++)
			stack[i]=new ExprStackItem();
	}
	
	public InterpreterResult interpret(CompilationUnitDeclaration ast)
	{
		if (ast.ignoreFurtherInvestigation)
			throw new InterpretException("compile errors"); //$NON-NLS-1$
		
		execBlock(ast.statements);
		if (stackPtr>=0)
			result.result=stack[stackPtr--];
		else
			result.result=Value.UndefinedObjectValue;
		return result;
	}

	
	public void endVisit(BinaryExpression binaryExpression, BlockScope scope) {
		ExprStackItem value2= stack[stackPtr--];
		ExprStackItem value1= stack[stackPtr--];
		int resultInt=0;
		Object resultObj=null;
		int type=Value.BOOLEAN;	// most common
		int operator=(binaryExpression.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT;
		switch (operator)
		{
			case OperatorIds.PLUS:
				if (value1.type==Value.STRING || value2.type == Value.STRING)
				{
					resultObj = value1.stringValue()+value2.stringValue();
					type=Value.STRING;
				}
				else
				{
					resultInt = value1.numberValue()+value2.numberValue();
					type=Value.NUMBER;
				}
				break;
			case OperatorIds.MINUS:
				resultInt = value1.numberValue()-value2.numberValue();
				type=Value.NUMBER;
				break;
			case OperatorIds.DIVIDE:
				resultInt = value1.numberValue() / value2.numberValue();
				type=Value.NUMBER;
				break;
			case OperatorIds.MULTIPLY:
				resultInt = value1.numberValue() * value2.numberValue();
				type=Value.NUMBER;
				break;
			case OperatorIds.REMAINDER:
				resultInt = value1.numberValue() % value2.numberValue();
				type=Value.NUMBER;
				break;
			case OperatorIds.GREATER:
				resultInt = (value1.numberValue() > value2.numberValue()) ?1:0;
				break;
			case OperatorIds.GREATER_EQUAL:
				resultInt = (value1.numberValue() >= value2.numberValue())?1:0;
				break;
			case OperatorIds.LESS:
				resultInt =  (value1.numberValue() < value2.numberValue())?1:0;
				break;
			case OperatorIds.LESS_EQUAL:
				resultInt =  (value1.numberValue() <= value2.numberValue())?1:0;
				break;
			case OperatorIds.AND_AND:
				resultInt =  (value1.booleanValue() && value2.booleanValue())?1:0;
				break;
			case OperatorIds.AND:
				resultInt =  value1.numberValue() & value2.numberValue();
				break;
			case OperatorIds.OR:
				resultInt =  value1.numberValue() | value2.numberValue();
				break;
			case OperatorIds.OR_OR:
				resultInt =  (value1.booleanValue() || value2.booleanValue())?1:0;
				break;
			default:
				throw new UnimplementedException(""); //$NON-NLS-1$
				
		}
		
		pushValue(type,resultInt,resultObj);
		
	}

	public void endVisit(EqualExpression equalExpression, BlockScope scope) {
		ExprStackItem value2= stack[stackPtr--];
		ExprStackItem value1= stack[stackPtr--];
		int type=Value.BOOLEAN;	// most common
		boolean equal=false;
		int operator=(equalExpression.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT;
		switch (operator)
		{
		case OperatorIds.EQUAL_EQUAL:
		case OperatorIds.NOT_EQUAL:
			
			switch (value1.type)
			{
			case Value.NUMBER:
			case Value.BOOLEAN:
				equal=value1.numberValue()==value2.numberValue(); break;
			case Value.STRING:
				if (value2.type==Value.NUMBER)
					equal=value1.numberValue()==value2.numberValue();
				else
					equal=value1.stringValue().equals(value2.stringValue());
				break;
			case Value.UNDEFINED:
			case Value.NULL:
				equal=(value2.type==Value.UNDEFINED || value2.type==Value.NULL); break;
			default:
				equal=(value1.objValue==value2.objValue);
			
			}
			if (operator==OperatorIds.NOT_EQUAL)
				equal = !equal;
				break;
		case OperatorIds.EQUAL_EQUAL_EQUAL:
		case OperatorIds.NOT_EQUAL_EQUAL:
				if (value1.type==value2.type)
				{
					switch (value1.type)
					{
					case Value.NUMBER:
					case Value.BOOLEAN:
						equal=value1.value==value2.value; break;
					case Value.STRING:
							equal=value1.stringValue().equals(value2.stringValue());
						break;
					case Value.UNDEFINED:
					case Value.NULL:
						equal=true; break;
					default:
						equal=(value1.objValue==value2.objValue);
					}
				}
				if (operator==OperatorIds.NOT_EQUAL_EQUAL)
					equal = !equal;
					break;
			default:
				throw new UnimplementedException(""); //$NON-NLS-1$
				
		}
		
		pushValue(type,equal? 1:0,null);
		
	}


    public boolean visit(PostfixExpression postfixExpression, BlockScope scope) {
    	ExprStackItem value= execute(postfixExpression.lhs);
		if (value.reference==null)
			throw new InterpretException("invalid assigment left hand side"); //$NON-NLS-1$

		int number=value.numberValue();
		int orgNumber=number;
		
    	switch (postfixExpression.operator) {
		case OperatorIds.PLUS :
			number++; //$NON-NLS-1$
			break;
		case OperatorIds.MINUS :
			number++; //$NON-NLS-1$
			break;
	} 
    	Value newValue = new NumberValue(number);
		value.reference.setValue(value.referenceName, newValue);
		pushNumber(orgNumber);
    	return false;
	}

	public boolean visit(PrefixExpression prefixExpression, BlockScope scope) {
    	ExprStackItem value= execute(prefixExpression.lhs);
		if (value.reference==null)
			throw new InterpretException("invalid assigment left hand side"); //$NON-NLS-1$

		int number=value.numberValue();
		
    	switch (prefixExpression.operator) {
		case OperatorIds.PLUS :
			number++; //$NON-NLS-1$
			break;
		case OperatorIds.MINUS :
			number++; //$NON-NLS-1$
			break;
	} 
    	Value newValue = new NumberValue(number);
		value.reference.setValue(value.referenceName, newValue);
		pushNumber(number);
    	return false;
	}

	private void pushValue(int type,int value,Object objValue)
    {
    	if (++stackPtr >=stack.length)
    	{
    		ExprStackItem []newStack=new ExprStackItem[stack.length*2];
    		System.arraycopy(stack, 0, newStack, 0, stack.length);
    		for (int i=stack.length;i<newStack.length;i++)
    			newStack[i]=new ExprStackItem();
    		stack=newStack;

    	}
    	stack[stackPtr].type=type;
    	stack[stackPtr].value=value;
    	stack[stackPtr].objValue=objValue;
    }
	
	private void pushNumber(int number)
	{
		pushValue(Value.NUMBER,number,null);
	}
	
	private void pushString (String string)
	{
		pushValue(Value.STRING,0,string);
	}
	
	private void pushExprStackItem(ExprStackItem item)
	{
		pushValue(item.type,item.value,item.objValue);
	}
	private void pushReference(ValueReference reference,char[] name,Value value)
	{
		pushValue(value,false);
		stack[stackPtr].reference=reference;
		stack[stackPtr].referenceName=name;
	}

	private void pushValue(Value value,boolean allowUndefined) {
		int type=0;
		int intValue=0;
		Object objValue=null;
		if (value!=null && (value.type!=Value.UNDEFINED || allowUndefined))
		{
			type=value.type;
			switch (type)
			{
			case Value.BOOLEAN:
			case Value.NUMBER:
				  intValue=value.numberValue(); break;
			case Value.STRING:
				objValue=value.stringValue(); break;
			
			default:
			  objValue=value;
			}
		}
		pushValue(type,intValue,objValue);
	}
	
	
	public boolean visit(IntLiteral intLiteral, BlockScope scope) {
		int value=(intLiteral.source==null)? intLiteral.value : Integer.valueOf(new String(intLiteral.source)).intValue();
		pushNumber(value);
		return true;
	} 

	public boolean visit(SingleNameReference singleNameReference, BlockScope scope) {
		char [] name=singleNameReference.token;
		Value value=this.context.getValue(name);
		pushReference(this.context.lastReference, name, value);
		return true;
	}

	public boolean visit(SingleNameReference singleNameReference, ClassScope scope) {
		return visit(singleNameReference,(BlockScope) null);
	}

	public void endVisit(Assignment assignment, BlockScope scope) {
		ExprStackItem assignValue= stack[stackPtr--];
		ExprStackItem refValue= stack[stackPtr--];
		if (refValue.reference==null)
			throw new InterpretException("invalid assigment left hand side"); //$NON-NLS-1$
		refValue.reference.setValue(refValue.referenceName, assignValue.getValue());
		pushExprStackItem(assignValue);
	}

	public void endVisit(ObjectLiteralField field, BlockScope scope) {
		ExprStackItem fieldValue= stack[stackPtr--];
		--stackPtr;//name
		ObjectValue object=(ObjectValue)stack[stackPtr].objValue;
		char [] name=null;
		if (field.fieldName instanceof SingleNameReference)
			name=((SingleNameReference)field.fieldName).token;
		else if (field.fieldName instanceof StringLiteral)
			name=((StringLiteral)field.fieldName).source();
		else
			throw new InterpretException("invalid object literal field"); //$NON-NLS-1$
		object.setValue(name, fieldValue);
	}

	public boolean visit(ObjectLiteral literal, BlockScope scope) {
		pushValue(Value.OBJECT, 0, new ObjectValue());
		return true;
	}

	protected ExprStackItem execute(Expression expr)
	{
		expr.traverse(this,(BlockScope) null);
		ExprStackItem value= stack[stackPtr--];
		return value;
	}
	
	public boolean visit(FieldReference fieldReference, BlockScope scope) {
		Value receiver=execute(fieldReference.receiver);
		ObjectValue object=receiver.getObjectValue();
		pushReference(object,fieldReference.token,object.getValue(fieldReference.token));
		return false;
	}
	public boolean visit(ThisReference thisReference, BlockScope scope) {
		ObjectValue value=null;
		if (this.context.thisObject instanceof ObjectValue)
			value=(ObjectValue)this.context.thisObject;
		pushValue(value, false);
		return false; 
	}
	public boolean visit(MethodDeclaration methodDeclaration, Scope scope) {
		FunctionValue func=new FunctionValue(methodDeclaration);
		this.context.setValue(methodDeclaration.getName(), func);
		
		return false;
	}

	public boolean visit(FunctionExpression functionExpression, BlockScope scope) {
		FunctionValue func=new FunctionValue(functionExpression.methodDeclaration);
		pushValue(func, false);
		return false;
	}

	public boolean visit(MessageSend messageSend, BlockScope scope) {
		FunctionValue function=null;
		
		ValueReference receiverObj=this.context;
		Value receiver=null;
		if (messageSend.receiver!=null)
		{
			receiver=execute(messageSend.receiver);
			if (receiver.type==Value.FUNCTION && messageSend.selector==null)
				  function=(FunctionValue) receiver.valueObject();
			else
				receiverObj=receiver.getObjectValue();
			
		}
		
		if (function==null)
		{
			receiver=receiverObj.getValue(messageSend.selector);
				if (receiver.type==Value.FUNCTION)
				  function=(FunctionValue)receiver;
				else
					throw new InterpretException("not a function:"+new String((messageSend.selector))); //$NON-NLS-1$
		  	
		}
		
		int restoreStackPtr=this.stackPtr;
		Value [] arguments;
		if (messageSend.arguments!=null)
		{
			arguments= new Value[messageSend.arguments.length];
			for (int i = 0; i < arguments.length; i++) {
				arguments[i]=execute(messageSend.arguments[i]);
				this.stackPtr++;
			}
		}
		else 
			arguments=new Value[0];
		if (!(receiverObj instanceof ObjectValue))
			receiverObj=null;
		Value returnValue=function.execute(this, (ObjectValue)receiverObj, arguments);
		this.stackPtr=restoreStackPtr;
		pushValue(returnValue,true);
		return false;
	}

	protected int execBlock(ProgramElement[] statements) {
		this.context.returnCode=0;
		for (int i = 0; i < statements.length && this.context.returnCode==0; i++) {
			statements[i].traverse(this, null);
		}
		return this.context.returnCode;
	}

	protected int execStatement(ProgramElement statement) {
		this.context.returnCode=0;
		if (statement==null)
			return 0;
		statement.traverse(this, null);
		return this.context.returnCode;
	}

	
	
	public boolean visit(AllocationExpression allocationExpression, BlockScope scope) {
		Value value = execute(allocationExpression.member);
		if (value.type!=Value.FUNCTION)
				throw new InterpretException("not a function:"+allocationExpression.member.toString()); //$NON-NLS-1$
		FunctionValue  function= (FunctionValue) value.valueObject();
		
		ObjectValue receiver = new ObjectValue(function.prototype);
		
		int restoreStackPtr=this.stackPtr;
		Value [] arguments;
		if (!allocationExpression.isShort && allocationExpression.arguments!=null)
		{
			arguments= new Value[allocationExpression.arguments.length];
			for (int i = 0; i < arguments.length; i++) {
				arguments[i]=execute(allocationExpression.arguments[i]);
				this.stackPtr++;
			}
		}
		else
			arguments= new Value[0];
		
		function.execute(this,  receiver, arguments);
		this.stackPtr=restoreStackPtr;
		pushValue(receiver,false);

		return false;
	}

	public boolean visit(FalseLiteral falseLiteral, BlockScope scope) {
		pushValue(Value.BOOLEAN, 0, null);
		return false;
	}

	public boolean visit(NullLiteral nullLiteral, BlockScope scope) {
		pushValue(Value.NULL, 1, Value.NullObjectValue);
		return false;
	}

	public boolean visit(UndefinedLiteral undefined, BlockScope scope) {
		pushValue(Value.UNDEFINED, 1, Value.UndefinedObjectValue);
		return false;
	}


	public boolean visit(StringLiteral stringLiteral, BlockScope scope) {
		pushString(new String(stringLiteral.source()));
		return false;
	}

	public boolean visit(TrueLiteral trueLiteral, BlockScope scope) {
		pushValue(Value.BOOLEAN, 1, null);
		return false;
	}

	public boolean visit(ReturnStatement returnStatement, BlockScope scope) {
		Value returnValue=Value.UndefinedObjectValue;
		if (returnStatement.expression!=null)
			returnValue = execute(returnStatement.expression);
		this.context.returnValue=returnValue;
		this.context.returnCode=STOP_RETURN;
		return false;
	}

	public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
		Value value = Value.UndefinedObjectValue;
		if (localDeclaration.initialization!=null)
			value=execute(localDeclaration.initialization);
		this.context.setValue(localDeclaration.name, value.getValue());
		return false;
	}

	public boolean visit(Block block, BlockScope scope) {
		execBlock(block.statements);
		return false;
	}

	public boolean visit(IfStatement ifStatement, BlockScope scope) {
		Value condition=execute(ifStatement.condition);
		if (condition.booleanValue())
			execStatement(ifStatement.thenStatement);
		else
			execStatement(ifStatement.elseStatement);
		
		return false;
	}

	public boolean visit(WhileStatement whileStatement, BlockScope scope) {
		while (true)
		{
			Value condition=execute(whileStatement.condition);
			if (condition.booleanValue())
			{
				int returnCode=execStatement(whileStatement.action);
				if (returnCode!=0 && returnCode!=STOP_CONTINUE)
					return false;
			}
			else
				return false;
		}
	}

	public boolean visit(BreakStatement breakStatement, BlockScope scope) {
		this.context.returnCode=STOP_BREAK;
		return false;
	}

	public boolean visit(ContinueStatement continueStatement, BlockScope scope) {
		this.context.returnCode=STOP_CONTINUE;
		return false;
	}

	
	
	protected InterpreterContext newContext(InterpreterContext parent,ObjectValue thisObject, ProgramElement method)
	{
		return new InterpreterContext( parent, thisObject);
	}

	public void restorePreviousContext() {
		this.context=this.context.parent;
	}


	
    

	
}
