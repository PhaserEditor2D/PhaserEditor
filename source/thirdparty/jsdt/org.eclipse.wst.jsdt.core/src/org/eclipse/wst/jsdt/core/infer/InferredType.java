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
import java.util.Iterator;

import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.IAbstractFunctionDeclaration;
import org.eclipse.wst.jsdt.core.ast.IFunctionDeclaration;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteral;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.MultipleTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TagBits;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.wst.jsdt.internal.core.Logger;


/**
 * The representation of an inferred type. 
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class InferredType extends ASTNode {

	char [] name;
	public ArrayList methods;
	public InferredAttribute[] attributes=new InferredAttribute[5];
	public int numberAttributes=0;
	public HashtableOfObject attributesHash = new HashtableOfObject();
	
	/**
	 * <p>The parent type of this type, or <code>null</code> if this type does not have a parent type</p>
	 * 
	 * <p><b>NOTE: </b>This field should not be accessed directly, use the appropriate getter and setter.</p>
	 * 
	 * @see {@link #setSuperType(InferredType)}
	 * @see {@link #getSuperType()}
	 */
	public InferredType superClass;

	public InferredType referenceClass;

	public SourceTypeBinding binding;
	
	/**
	 * @deprecated this will not remain public forever
	 * 
	 * @see #isDefinition()
	 * @see #setIsDefinition(boolean)
	 */
	public boolean isDefinition;
	private TypeBinding resolvedType;
	public ClassScope scope;
	ReferenceBinding resolvedSuperType;

	public boolean isArray=false;
	public boolean isAnonymous=false;
	public boolean isObjectLiteral=false;

	private int nameStart = -1;
	
	public String inferenceProviderID;
	public String inferenceStyle;
	
	public ArrayList mixins;
	/**
	 * @since 1.1
	 */
	public int modifiers;
	
	// in the case where a type extends functions, we sometimes need
	// to know the actual function in order to create inferred methods
	private IFunctionDeclaration correspondingFunction;

	/**
	 * <p>
	 * <code>true</code> if this type is a globally visible type,
	 * <code>false</code> otherwise.
	 * </p>
	 * 
	 * <p>
	 * EX: The anonymous type for a global variable would be globally visible,
	 * the anonymous type for a local would not.
	 * </p>
	 * 
	 * @see #isIndexed()
	 * @see #setIsGlobal(boolean)
	 * 
	 * @since 1.2
	 */
	private boolean fIsGlobal;
	/**
	 * Contains the InferredTypes that with which this type is synonymous
	 * 
	 * @see #getSynonyms()
	 * @see #addSynonym(InferredType)
	 * 
	 * @since 1.2
	 */
	private InferredType[] fSynonyms;
	
	public final static char[] OBJECT_NAME=new char[]{'O','b','j','e','c','t'};
	
	/**
	 * @deprecated this is not used internally, this will be removed
	 */
	public final static char[] OBJECT_LITERAL_NAME = new char[]{'{','}'};

	public final static char[] ARRAY_NAME=new char[]{'A','r','r','a','y'};
	public final static char[] FUNCTION_NAME =new char[]{'F','u','n','c','t','i','o','n'};
	public final static char[] VOID_NAME =new char[]{'v','o','i','d'};

	/**
	 * @deprecated - no longer used
	 */
	public final static char[] GLOBAL_NAME=new char[]{'G','l','o','b','a','l'};
	
	/**
	 * @deprecated this is not used internally, this will be removed
	 */
	public Object userData;
	
	/**
	 * @deprecated this is not used internally, this will be removed
	 */
	boolean allStatic=false;
	
	/**
	 * Create a new inferred type
	 * 
	 * @param className inferred type name
	 */
	public InferredType(char [] className) {
		this.name=className;
		this.sourceStart=-1;
		this.fIsGlobal = false;
		this.isDefinition = false;
	}
	
	/**
	 * @since 1.1
	 * @return
	 */
	public int getModifiers() {
		return modifiers;
	}

	/**
	 * Gets the name of the inferred type
	 * 
	 * @return the inferred type name
	 */
	public char [] getName() {
		return name;
	}

	/**
	 * Get the superclass name of the inferred type
	 * 
	 * @return superclass name
	 */
	public char [] getSuperClassName()
	{
		return superClass!=null ? superClass.getName() : OBJECT_NAME;
	}
	
	/**
	 * Add a new inferred attribute to the inferred type
	 * 
	 * @param name the attribute name
	 * @param definer the ASTNode which this attribute is inferred from
	 * @param nameStart character position (in the source) of the attribute name
	 * @return a new InferredAttribute
	 */
	public InferredAttribute addAttribute(char [] name, IASTNode definer, int nameStart)
	{
		InferredAttribute attribute = findAttribute(name);
		if (attribute==null)
		{
			attribute=new InferredAttribute(name, this, definer);
			attribute.node=(ASTNode)definer;
			
			if (this.numberAttributes == this.attributes.length)

				System.arraycopy(
						this.attributes,
						0,
						this.attributes = new InferredAttribute[this.numberAttributes  * 2],
						0,
						this.numberAttributes );
						this.attributes [this.numberAttributes  ++] = attribute;


			attributesHash.put(name, attribute);

			if (!isAnonymous) {
				this.updatePositions(definer.sourceStart(), definer.sourceEnd());
			}
		}
		attribute.nameStart = nameStart;
		return attribute;
	}
	
	/**
	 * Adds a new inferred attribute to the inferred type if it doesn't already exist
	 * otherwise it replaces the existing one with the new one.
	 * 
	 * @param name the attribute name
	 * @param definer the ASTNode which this attribute is inferred from
	 * @param nameStart character position (in the source) of the attribute name
	 * @return a inferredAttribute
	 */
	public InferredAttribute replaceAttribute(char [] name, IASTNode definer, int nameStart) {
		InferredAttribute attribute = findAttribute(name);
		if (attribute == null)
			return addAttribute(name, definer, nameStart);
		
		attributesHash.removeKey(name);
		
		InferredAttribute newAttribute = new InferredAttribute(name, this, definer);
		newAttribute.node=(ASTNode)definer;
		
		for (int i = 0; i < this.numberAttributes; i++) {
			if (this.attributes[i].equals(attribute)) {
				this.attributes [i] = newAttribute;
			}
		}
		attributesHash.put(name, newAttribute);

		if (!isAnonymous) {
			this.updatePositions(definer.sourceStart(), definer.sourceEnd());
		}
		newAttribute.nameStart = nameStart;
		return newAttribute;
	}

	/**
	 * Add an InferredAttribute to this inferred type.
	 * 
	 * @param newAttribute the attribute to add.
	 * @return 
	 */
	public InferredAttribute addAttribute(InferredAttribute newAttribute)
	{
		IASTNode definer=newAttribute.node;
		InferredAttribute attribute = findAttribute(newAttribute.name);
		if (attribute==null)
		{

			if (this.numberAttributes == this.attributes.length)

				System.arraycopy(
						this.attributes,
						0,
						this.attributes = new InferredAttribute[this.numberAttributes  * 2],
						0,
						this.numberAttributes );
						this.attributes [this.numberAttributes  ++] = newAttribute;


			attributesHash.put(newAttribute.name, newAttribute);

			if (!isAnonymous) {
				if (definer != null) {
					this.updatePositions(definer.sourceStart(), definer.sourceEnd());
				}
				else {
					this.updatePositions(newAttribute.sourceStart(), newAttribute.sourceEnd());
				}
			}
		}
		return newAttribute;
	}
	/**
	 * Find the inferred attribute with the given name
	 * 
	 * @param name name of the attribute to find
	 * @return the found InferredAttribute, or null if not found
	 */
	public InferredAttribute findAttribute(char [] name)
	{
		return (InferredAttribute)attributesHash.get(name);
//		if (attributes!=null)
//		for (Iterator attrIterator = attributes.iterator(); attrIterator.hasNext();) {
//			InferredAttribute attribute = (InferredAttribute) attrIterator.next();
//			if (CharOperation.equals(name,attribute.name))
//				return attribute;
//		}
//		return null;
	}


	/**
	 * Add a new constructor method to the inferred type
	 * 
	 * @param methodName name of the method to add
	 * @param functionDeclaration the AST Node containing the method bode
	 * @param nameStart character position (in the source) of the method name
	 * @return a new inferred method
	 */
	public InferredMethod addConstructorMethod(char [] methodName, IFunctionDeclaration functionDeclaration, int nameStart) {
		InferredMethod method = this.addMethod(methodName, functionDeclaration, nameStart, true);
		method.isConstructor = true;
		this.setNameStart(nameStart);
		//method.getFunctionDeclaration().setInferredType(this);
		return method;
	}
	
	/**
	 * Add a new method to the inferred type
	 * 
	 * @param methodName name of the method to add
	 * @param functionDeclaration the AST Node containing the method bode
	 * @param nameStart character position (in the source) of the method name
	 * @return a new inferred method
	 */
	public InferredMethod addMethod(char [] methodName, IFunctionDeclaration functionDeclaration, int nameStart) {
		return this.addMethod(methodName, functionDeclaration, nameStart, false);
	}
	
	/**
	 * Add a new method to the inferred type
	 * 
	 * @param methodName name of the method to add
	 * @param functionDeclaration the AST Node containing the method bode
	 * @param isConstructor true if it is a constructor
	 * @return a new inferred method
	 */
	private InferredMethod addMethod(char [] methodName, IFunctionDeclaration functionDeclaration, int nameStart, boolean isConstructor) {
		MethodDeclaration methodDeclaration = (MethodDeclaration)functionDeclaration;
		InferredMethod method = findMethod(methodName, methodDeclaration);
		if (method==null) {
			/* if the inferred method for the declaration specifies that it is in a
			 * type use that one.
			 * 
			 * This is for the case where a method has been mixed in from another type
			 * but we still want that method to be reported as defined on the other
			 * type and not this type
			 */
			InferredType inType = this;
			if(methodDeclaration.getInferredMethod() != null && methodDeclaration.getInferredMethod().inType != null && !isConstructor && 
						!methodDeclaration.getInferredMethod().isConstructor &&
						!methodDeclaration.getInferredMethod().inType.isAnonymous && this.isAnonymous) {
				inType = methodDeclaration.getInferredMethod().inType;
			}
			
			method=new InferredMethod(methodName,methodDeclaration,inType);
			if (methodDeclaration.inferredMethod==null) 
				methodDeclaration.inferredMethod = method;
			else
			{
				if (isConstructor)
				{
					methodDeclaration.inferredMethod.inType=this;
					method.isStatic=methodDeclaration.inferredMethod.isStatic;
					method.bits=methodDeclaration.inferredMethod.bits;
					//methodDeclaration.inferredMethod = method;
				} //else if (methodDeclaration.inferredMethod.isConstructor)
					//method.inType=methodDeclaration.inferredMethod.inType;
				
			}
			if (methods==null)
				methods=new ArrayList();
			methods.add(method);

			if(!isAnonymous && !isConstructor)
				this.updatePositions(methodDeclaration.sourceStart, methodDeclaration.sourceEnd);
			method.isConstructor=isConstructor;
			method.nameStart = nameStart;
		} else {
			if (methodDeclaration.inferredMethod==null) {
				methodDeclaration.inferredMethod=method;
			}
		}
		
		return method;
	}
	
	/**
	 * Adds a new inferred method to the inferred type if it doesn't already exist
	 * otherwise it replaces the existing one with the new one.
	 * 
	 * @param methodName name of the method to add
	 * @param functionDeclaration the AST Node containing the method bode
	 * @param isConstructor true if it is a constructor
	 * @return an inferred method
	 */
	private InferredMethod replaceMethod(char [] methodName, IFunctionDeclaration functionDeclaration, int nameStart) {
		MethodDeclaration methodDeclaration = (MethodDeclaration) functionDeclaration;
		InferredMethod method = findMethod(methodName, methodDeclaration);
		if (method == null)
			return addMethod(methodName, functionDeclaration, nameStart);
		
			/* if the inferred method for the declaration specifies that it is in a
			 * type use that one.
			 * 
			 * This is for the case where a method has been mixed in from another type
			 * but we still want that method to be reported as defined on the other
			 * type and not this type
			 */
			InferredType inType = this;
			if (methodDeclaration.getInferredMethod() != null && methodDeclaration.getInferredMethod().inType != null) {
				inType = methodDeclaration.getInferredMethod().inType;
			} else {
				inType = this;
			}
			methods.remove(method);
			
			InferredMethod newMethod = new InferredMethod(methodName, methodDeclaration, inType);
			if (methodDeclaration.inferredMethod == null) 
				methodDeclaration.inferredMethod = newMethod;
			else if (methodDeclaration.inferredMethod.isConstructor)
				newMethod.inType = methodDeclaration.inferredMethod.inType;
			
			methods.add(newMethod);

			if (!isAnonymous)
				this.updatePositions(methodDeclaration.sourceStart, methodDeclaration.sourceEnd);
			newMethod.isConstructor = false;
			newMethod.nameStart = nameStart;
		
		return newMethod;
	}

	/**
	 * Find an inferred method
	 * 
	 * @param methodName name of the method to find
	 * @param methodDeclaration not used
	 * @return the found method, or null
	 */
	public InferredMethod findMethod(char [] methodName, IFunctionDeclaration methodDeclaration) {
		boolean isConstructor= methodName==TypeConstants.INIT;
		if (methods!=null)
			for (Iterator methodIterator = methods.iterator(); methodIterator.hasNext();) {
				InferredMethod method = (InferredMethod) methodIterator.next();
				if (CharOperation.equals(methodName,method.name))
					return method;
				if (isConstructor && method.isConstructor)
					return method;
			}
			return null;

	}

	public TypeBinding resolveType(Scope scope, ASTNode node) {
		if (scope == null)
			return null;
		
		// handle the error here
		if (this.resolvedType != null) // is a shared type reference which was already resolved
			return this.resolvedType.isValidBinding() ? this.resolvedType : null; // already reported error


		if (isArray())
		{
			TypeBinding memberType = (referenceClass!=null)?referenceClass.resolveType(scope,node):null;
			if (memberType==null)
				memberType=TypeBinding.UNKNOWN;
			this.resolvedType=new ArrayBinding(memberType, 1, scope.compilationUnitScope().environment) ;

		}
		else {
			if (CharOperation.indexOf('|', name)>0)
			{
				char[][] names = CharOperation.splitAndTrimOn('|', name);
				this.resolvedType=new MultipleTypeBinding(scope,names);
			}
			else
			  this.resolvedType = scope.getType(name);
			/* the inferred type isn't valid, so don't assign it to the variable */
			if(!this.resolvedType.isValidBinding()) this.resolvedType = null;
		}


		if (this.resolvedType == null)
			return null; // detected cycle while resolving hierarchy
		if (node!=null && !this.resolvedType.isValidBinding()) {
			scope.problemReporter().invalidType(node, this.resolvedType);
			return null;
		}
		if (node!=null && node.isTypeUseDeprecated(this.resolvedType, scope))
			scope.problemReporter().deprecatedType(this.resolvedType, node);

		if( !isNamed() )
			this.resolvedType.tagBits |= TagBits.AnonymousTypeMask;

		return this.resolvedType ;
	}



	public void dumpReference(StringBuffer sb)
	{
		sb.append(name);
		if (referenceClass!=null)
		{
			sb.append('(');
			referenceClass.dumpReference(sb);
			sb.append(')');
		}
	}

	public boolean containsMethod(IAbstractFunctionDeclaration inMethod) {
		if (methods!=null)
			for (Iterator iter = methods.iterator(); iter.hasNext();) {
				InferredMethod method = (InferredMethod) iter.next();
				if (method.getFunctionDeclaration()==inMethod)
					return true;
			}
		return false;
	}



	public ReferenceBinding resolveSuperType(ClassScope classScope) {
		if (this.resolvedSuperType != null)
			return this.resolvedSuperType;

		if(superClass != null) {
			TypeBinding typeBinding = classScope.getType(superClass.getName());
			if ( typeBinding instanceof ReferenceBinding ) this.resolvedSuperType = (ReferenceBinding)typeBinding;
		}

		return this.resolvedSuperType;
	}

	public boolean isArray()
	{
		return CharOperation.equals(ARRAY_NAME, name);
	}
	
	public boolean isFunction()
	{
		return CharOperation.equals(FUNCTION_NAME, name);
	}
	
	public boolean isVoid()
	{
		return CharOperation.equals(VOID_NAME, name);
	}

	public StringBuffer print(int indent, StringBuffer output) {
		printIndent(indent, output);
		char[] superName= getSuperClassName();
		output.append("class ").append(name).append(" extends ").append(superName).append("{\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		for (int i=0;i<this.numberAttributes;i++) {
				this.attributes[i].print(indent+1,output);
				output.append(";\n"); //$NON-NLS-1$
			}
		if (methods!=null)
			for (Iterator methodIterator = methods.iterator(); methodIterator.hasNext();) {
				InferredMethod method = (InferredMethod) methodIterator.next();
				method.print(indent+1,output);
				output.append("\n"); //$NON-NLS-1$
			}
		output.append("}"); //$NON-NLS-1$
		return output;
	}

	public boolean isInferred()
	{
		return true;
	}

	public void updatePositions(int start, int end)
	{
		if (this.sourceStart==-1 ||(start>=0 && start<this.sourceStart))
			this.sourceStart=start;
		if (end>0&&end>this.sourceEnd)
			this.sourceEnd=end;
	}

	public IAbstractFunctionDeclaration declarationOf(MethodBinding methodBinding) {
		if (methodBinding != null && this.methods != null) {
			for (int i = 0, max = this.methods.size(); i < max; i++) {
				InferredMethod method=(InferredMethod) this.methods.get(i);

				if (method.methodBinding==methodBinding)
					return method.getFunctionDeclaration();
			}
		}
		return null;
	}
	
	/**
	 * @return <code>true</code> if this type is anonymous or the the type
	 *         name starts with the anonymous prefix, <code>false</code>
	 *         otherwise
	 */
	public boolean isNamed() {
		return !isAnonymous || !CharOperation.prefixEquals(IInferEngine.ANONYMOUS_PREFIX, this.name);
	}
	
	/**
	 * @param modifiers the modifiers to set
	 * @since 1.1
	 */
	public void setModifiers(int modifiers) {
		this.modifiers = modifiers;
	}
	
	/**
	 * Set the charactor position (in the source) of the type name
	 * 
	 * @param start type name position
	 */
	public void setNameStart(int start)
	{
		this.nameStart=start;
	}
	
	public int getNameStart()
	{
		return this.nameStart!= -1 ? this.nameStart : this.sourceStart;
	}

	/**
	 * @deprecated - no longer used
	 */
	public boolean isEmptyGlobal() {
		return (CharOperation.equals(GLOBAL_NAME, this.name) && this.numberAttributes == 0 && (this.methods == null || this.methods.isEmpty()));
	}

	/**
	 * <p>Adds the name of a type to mix into this type during the resolving step.</p>
	 * 
	 * <p>Use {@link #mixin(InferredType)} if the type to mixin is an anonymous type
	 * since anonymous types are only available at the inference step and will not be
	 * available at the resolve step when mixins added with this method are mixed in.</p>
	 * 
	 * <p><b>NOTE:</b> Do not confuse this with <code>dojo.mixin()</code>, this operation is actually
	 * more akin to <code>dojo.extend</code></p>
	 * 
	 * @param mixinTypeName the name of the type to mix into this type
	 */
	public void addMixin(char[] mixinTypeName) {
		if (mixins==null) {
			mixins=new ArrayList();
		}
		
		//prevent duplicates
		if(!mixins.contains(mixinTypeName)) {
			mixins.add(mixinTypeName);
		}
	}
	
	/**
	 * <p>Mixes an {@link InferredType} into this {@link InferredType} right now.  Thus if the type being
	 * mixed in changes at all, fields or methods added/removed, at a later point those changes will
	 * not get reflected in this inferred type.  Thus this method should only be used when it is certain the
	 * mixin type will not change again, such as during the resolving step or an {@link ObjectLiteral} during
	 * the inference step.</p>
	 *
	 * <p><b>NOTE:</b> Do not confuse this with <code>dojo.mixin()</code>, this operation is actually
	 * more akin to <code>dojo.extend</code></p>
	 *
	 * @param mixin
	 * @since 1.1
	 */
	public void mixin(InferredType mixin) {
		if(mixin !=null) {
			InferredAttribute[] attributes = mixin.attributes;
			ArrayList methods = mixin.methods;
			if(methods == null)
				methods = new ArrayList(1);
			
			// get the full list of methods and attributes from the mix class and its super class
			InferredType mixSuperType = mixin.getSuperType();
			while(mixSuperType != null && !CharOperation.equals(mixSuperType.getName(), TypeConstants.OBJECT)) {
				// attributes
				InferredAttribute[] tempAttributes = new InferredAttribute[attributes.length + mixSuperType.numberAttributes];
				System.arraycopy(attributes, 0, tempAttributes, 0, attributes.length);
				System.arraycopy(mixSuperType.attributes, 0, tempAttributes, attributes.length - 1, mixSuperType.numberAttributes);
				attributes = tempAttributes;
				
				// methods
				if (mixSuperType.methods != null)
					methods.addAll(mixSuperType.methods);
				mixSuperType = mixSuperType.getSuperType();
			}
			
			// add attributes to the type
			for(int a = 0; a < attributes.length; a++) {
				//do not mix in statics
				if(attributes[a] != null && !attributes[a].isStatic) {
					InferredAttribute attr = this.replaceAttribute( attributes[a].name, attributes[a].node, attributes[a].nameStart);
					attr.type=attributes[a].type;
					attr.isStatic = false;
					attr.nameStart = attributes[a].nameStart;
					attr.modifiers = attributes[a].modifiers;
					attr.initializationStart = attributes[a].initializationStart;
				}
			}
			
			// add functions to the type
			for(int m = 0; m < methods.size(); m++) {
				InferredMethod functToMixin = (InferredMethod)methods.get(m);
				
				//do not mix in constructors or statics
				if(!functToMixin.isConstructor && !functToMixin.isStatic) {
					this.replaceMethod(functToMixin.name, functToMixin.getFunctionDeclaration(), functToMixin.nameStart);
				}
			}
		}
	}
	
	/**
	 * <p>Mixes an {@link InferredType} into this {@link InferredType}.  By passing true for the second argument, 
	 * objects will be recursively mixed.  This means that if a property of the first object is itself an object, 
	 * a mix will be performed if a property with the same key exists in the second object.  Otherwise it would be completely
	 * overriden by the property of the second object.
	 *
	 * @param mixin
	 */
	public void mixin(InferredType mixin, boolean isDeepCopy) {
		if (!isDeepCopy)
			mixin(mixin);
		else if (mixin != null) {
			InferredAttribute[] attributes = mixin.attributes;
			ArrayList methods = mixin.methods;
			if (methods == null)
				methods = new ArrayList(1);
			
			// get the full list of methods and attributes from the mix class and its super class
			InferredType mixSuperType = mixin.getSuperType();
			while (mixSuperType != null && !CharOperation.equals(mixSuperType.getName(), TypeConstants.OBJECT)) {
				// attributes
				InferredAttribute[] tempAttributes = new InferredAttribute[attributes.length + mixSuperType.numberAttributes];
				System.arraycopy(attributes, 0, tempAttributes, 0, attributes.length);
				System.arraycopy(mixSuperType.attributes, 0, tempAttributes, attributes.length - 1, mixSuperType.numberAttributes);
				attributes = tempAttributes;
				
				// methods
				if (mixSuperType.methods != null)
					methods.addAll(mixSuperType.methods);
				mixSuperType = mixSuperType.getSuperType();
			}
			
			// add attributes to the type
			for (int a = 0; a < attributes.length; a++) {
				//do not mix in statics
				if (attributes[a] != null && !attributes[a].isStatic) {
					InferredAttribute existingAttr = findAttribute(attributes[a].name);
					if (existingAttr != null && existingAttr.type != null && existingAttr.type.isAnonymous) {
						existingAttr.type.mixin(attributes[a].type, true);
					}
					else {
						InferredAttribute attr = this.replaceAttribute( attributes[a].name, attributes[a].node, attributes[a].nameStart);
						attr.type=attributes[a].type;
						attr.isStatic = false;
						attr.nameStart = attributes[a].nameStart;
						attr.modifiers = attributes[a].modifiers;
						attr.initializationStart = attributes[a].initializationStart;
					}
				}
			}
			
			// add functions to the type
			for (int m = 0; m < methods.size(); m++) {
				InferredMethod functToMixin = (InferredMethod)methods.get(m);
				
				//do not mix in constructors or statics
				if (!functToMixin.isConstructor && !functToMixin.isStatic) {
					this.replaceMethod(functToMixin.name, functToMixin.getFunctionDeclaration(), functToMixin.nameStart);
				}
			}
		}
	}

	/**
	 * @return super {@link InferredType} of this {@link InferredType}, or
	 * <code>null</code> if none is set
	 * @since 1.1
	 */
	public InferredType getSuperType() {
		return this.superClass;
	}
	
	/**
	 * <p>Sets the super type of this type unless the given super type is
	 * itself then this is a no op</p>
	 * 
	 * @param superType {@link InferredType} to set as the super type of this type,
	 * can not be the same as this type
	 * @since 1.1
	 */
	public void setSuperType(InferredType superType) {
		// prevent cycles, and log if someone attempts to create one
		InferredType testType = superType;
		while (testType != null) {
			if (testType == this) {
				if (InferEngine.DEBUG)
					Logger.log(Logger.WARNING, "InferredType#setSuperType: a hierarchy loop would be caused between: " + new String(getName()) + " and " + new String(superType.getName())); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			}
			testType = testType.getSuperType();
		}
		
		this.superClass = superType;
	}
	
	/**
	 * <p>
	 * Determines if this type should be indexed or not. A type should be indexed if it is named or
	 * has specifically been set to be a global type.
	 * </p>
	 * 
	 * @return <code>true</code> if this type should be indexed, <code>false</code> otherwise
	 * 
	 * @see #isNamed()
	 * @see #isGlobal()
	 * 
	 * @since 1.1
	 */
	public boolean isIndexed() {
		return this.isGlobal() || this.isNamed();
	}
	
	/**
	 * <p>
	 * EX: The anonymous type for a global variable would be globally visible, the anonymous type
	 * for a local would not.
	 * </p>
	 * 
	 * @param isGlobal
	 *            <code>true</code> if this type is a globally visible type, <code>false</code>
	 *            otherwise.
	 * 
	 * @since 1.1
	 */
	public void setIsGlobal(boolean isGlobal) {
		this.fIsGlobal = isGlobal;
	}
	
	/**
	 * @return <code>true</code> if this type is a globally visible type, <code>false</code>
	 *         otherwise.
	 * 
	 * @since 1.1
	 */
	public boolean isGlobal() {
		return this.fIsGlobal;
	}
	
	/**
	 * @return <code>true</code> if this type is a definition,
	 *         <code>false</code> otherwise
	 */
	public boolean isDefinition() {
		return isDefinition;
	}

	/**
	 * @param isDefinition
	 *            <code>true</code> if this type is a definition,
	 *            <code>false</code> otherwise
	 */
	public void setIsDefinition(boolean isDefinition) {
		this.isDefinition = isDefinition;
	}

	/**
	 * @since 1.2
	 * @return the types for which this type is synonymous, or null if none have been set
	 */
	public InferredType[] getSynonyms() {
		return fSynonyms;
	}
	
	/**
	 * @since 1.2
	 * @param type - adds a type for which this type is synonymous
	 */
	public void addSynonym(InferredType type) {
		/* be sure given synonym does not have the same name as this type
		 * also be sure the new synonym is not the super type of this type
		 * or that this type is the super type of the given synonym
		 * 
		 * This situation can arise when a pattern like this is used:
		 * 
		 * define("foo.BarImpl", "foo.Bar", {}):
		 * foo.Bar = foo.BarImpl; */
		if((type != this && !CharOperation.equals(type.getName(), this.getName())) && 
				!((this.getSuperType() != null && CharOperation.equals(type.getName(), this.getSuperType().getName())) ||
				(type.getSuperType() != null && CharOperation.equals(this.getName(), type.getSuperType().getName())))) {
			
			if (fSynonyms == null) {
				fSynonyms = new InferredType[]{type};
			} else {
				boolean alreadyContains = false;
				for(int i = 0; i < this.fSynonyms.length && !alreadyContains; ++i) {
					alreadyContains = type == this.fSynonyms[i];
				}
				
				if(!alreadyContains) {
					InferredType[] synonyms = new InferredType[fSynonyms.length + 1];
					System.arraycopy(fSynonyms, 0, synonyms, 0, fSynonyms.length);
					synonyms[fSynonyms.length] = type;
					fSynonyms = synonyms;
				}
			}
		}
	}
	
	
	/**
	 * @return the correspondingFunction
	 */
	public IFunctionDeclaration getCorrespondingFunction() {
		return correspondingFunction;
	}

	/**
	 * @param coorespondingFunction the coorespondingFunction to set
	 */
	public void setCorrespondingFunction(IFunctionDeclaration coorespondingFunction) {
		this.correspondingFunction = coorespondingFunction;
	}
}