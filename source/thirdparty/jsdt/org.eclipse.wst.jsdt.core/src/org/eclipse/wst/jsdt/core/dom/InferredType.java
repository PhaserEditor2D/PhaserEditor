/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class InferredType extends Type {

	private static final List PROPERTY_DESCRIPTORS;

//	public static final ChildPropertyDescriptor TYPE_PROPERTY =
//		new ChildPropertyDescriptor(InferredType.class, "type", String.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	static {
		List propertyList = new ArrayList(0);
		createPropertyList(InferredType.class, propertyList);
// 		addProperty(TYPE_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(propertyList);
	}

     String type;


	InferredType(AST ast) {
		super(ast);
	}

	public static List propertyDescriptors(int apiLevel) {
		return PROPERTY_DESCRIPTORS;
	}


	void accept0(ASTVisitor visitor) {
		boolean visitChildren = visitor.visit(this);
		if (visitChildren) {
//			acceptChild(visitor, getName());
		}
		visitor.endVisit(this);

	}

	ASTNode clone0(AST target) {
		InferredType result = new InferredType(target);
		result.setSourceRange(-1,0);
		result.type = type;

		return result;
	}

	int getNodeType0() {
		return INFERRED_TYPE;
	}

	List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);

	}


	int memSize() {
		return 0;
	}

	boolean subtreeMatch0(ASTMatcher matcher, Object other) {
		return matcher.match(this, other);
	}

	int treeSize() {
		return 0;
	}
	public boolean isInferred()
	{
		return true;
	}

	public String getType() {
		return this.type;
	}


}
