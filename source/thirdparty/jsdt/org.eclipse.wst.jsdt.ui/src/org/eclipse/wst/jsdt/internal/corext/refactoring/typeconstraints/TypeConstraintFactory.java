/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/


package org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.wst.jsdt.core.dom.ITypeBinding;

public class TypeConstraintFactory implements ITypeConstraintFactory {

	private Map/*<ConstraintVariable, Map<ConstraintVariable, Map<ConstraintOperator, SimpleTypeConstraint>>*/ fSimpleConstraints= new HashMap();
	private Map/*<ConstraintVariable, Map<String, CompositeOrTypeConstraint>>*/ fOrConstraints= new HashMap();
	
	protected static final boolean PRINT_STATS= false;
	protected int fNrCreated= 0;
	protected int fNrFiltered= 0;
	protected int fNrRetrieved= 0;	
	
	// Only to be called by the createXXXConstraint() methods
	private SimpleTypeConstraint createSimpleTypeConstraint(ConstraintVariable v1, ConstraintVariable v2, ConstraintOperator operator) {
		if (fSimpleConstraints.containsKey(v1)){
			Map m2= (Map)fSimpleConstraints.get(v1);
			if (m2.containsKey(v2)){
				Map m3= (Map)m2.get(v2);
				if (m3.containsKey(operator)){
					if (PRINT_STATS) fNrRetrieved++;
					if (PRINT_STATS) dumpStats();
					return (SimpleTypeConstraint)m3.get(operator);
				} else {
					return storeConstraint(v1, v2, operator, m3);
				}
			} else {
				Map m3= new HashMap();
				m2.put(v2, m3);
				return storeConstraint(v1, v2, operator, m3);
			}
		} else {
			Map m2= new HashMap();
			fSimpleConstraints.put(v1, m2);
			Map m3= new HashMap();
			m2.put(v2, m3);
			return storeConstraint(v1, v2, operator, m3);
		}
	}
	
	private SimpleTypeConstraint storeConstraint(ConstraintVariable v1, ConstraintVariable v2, ConstraintOperator operator, Map m3) {
		SimpleTypeConstraint constraint= new SimpleTypeConstraint(v1, v2, operator);
		m3.put(operator, constraint);
		if (PRINT_STATS) fNrCreated++;
		if (PRINT_STATS) dumpStats();
		return constraint;
	}
	
	public ITypeConstraint[] createConstraint(ConstraintVariable v1, ConstraintVariable v2, ConstraintOperator operator){
		if (filter(v1, v2, operator)){
			return new ITypeConstraint[0];
		} else {
			return new ITypeConstraint[]{ createSimpleTypeConstraint(v1, v2, operator) };
		}
	}
	
	public ITypeConstraint[] createSubtypeConstraint(ConstraintVariable v1, ConstraintVariable v2){
		return createConstraint(v1, v2, ConstraintOperator.createSubTypeOperator());
	}
	
	public ITypeConstraint[] createStrictSubtypeConstraint(ConstraintVariable v1, ConstraintVariable v2){
		return createConstraint(v1, v2, ConstraintOperator.createStrictSubtypeOperator());
	}
	
	public ITypeConstraint[] createEqualsConstraint(ConstraintVariable v1, ConstraintVariable v2){
		return createConstraint(v1, v2, ConstraintOperator.createEqualsOperator());
	}
	
	public ITypeConstraint[] createDefinesConstraint(ConstraintVariable v1, ConstraintVariable v2){
		return createConstraint(v1, v2, ConstraintOperator.createDefinesOperator());
	}

	/**
	 * {@inheritDoc}
	 * Avoid creating constraints involving primitive types and self-constraints.
	 */
	public boolean filter(ConstraintVariable v1, ConstraintVariable v2, ConstraintOperator operator) {
		if ((v1.getBinding() != null && v1.getBinding().isPrimitive() &&
				v2.getBinding() != null && v2.getBinding().isPrimitive()) ||
					v1 == v2) {
			if (PRINT_STATS) fNrFiltered++;
			if (PRINT_STATS) dumpStats();
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.ITypeConstraintFactory#createCompositeOrTypeConstraint(org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints.ITypeConstraint[])
	 */
	public CompositeOrTypeConstraint createCompositeOrTypeConstraint(ITypeConstraint[] constraints){
		ConstraintVariable left= ((SimpleTypeConstraint)constraints[0]).getLeft();
		StringBuilder boundsBuilder= new StringBuilder();
		for (int i= 0; i < constraints.length; i++){
			ConstraintVariable right= ((SimpleTypeConstraint)constraints[i]).getRight();
			ITypeBinding binding= right.getBinding();
			boundsBuilder.append(binding.getQualifiedName());
			boundsBuilder.append(',');
		}
		String bounds = boundsBuilder.toString();
		
		if (fOrConstraints.containsKey(left)){
			Map m2= (Map)fOrConstraints.get(left);
			if (m2.containsKey(bounds)){
				if (PRINT_STATS) fNrRetrieved++;
				if (PRINT_STATS) dumpStats();
				return (CompositeOrTypeConstraint)m2.get(bounds);
			} else {
				CompositeOrTypeConstraint constraint= new CompositeOrTypeConstraint(constraints);
				m2.put(bounds, constraint);
				if (PRINT_STATS) dumpStats();
				if (PRINT_STATS) fNrCreated++;
				return constraint;
			}
		} else {
			Map m2= new HashMap();
			fOrConstraints.put(left, m2);
			CompositeOrTypeConstraint constraint= new CompositeOrTypeConstraint(constraints);
			m2.put(bounds, constraint);
			if (PRINT_STATS) dumpStats();
			if (PRINT_STATS) fNrCreated++;
			return constraint;
		}
	}
	
	protected void dumpStats() {
		System.out.println("Constraints: " + fNrCreated + " created, " + fNrRetrieved + " retrieved, " + fNrFiltered + " filtered");	 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
	
}
