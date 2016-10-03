/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.corext.refactoring.typeconstraints;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.Assert;


public class CompositeOrTypeConstraint implements ITypeConstraint{
	
	private final ITypeConstraint[] fConstraints;
	
	/* package */ CompositeOrTypeConstraint(ITypeConstraint[] constraints){
		Assert.isNotNull(constraints);
		fConstraints= sort(getCopy(constraints));
	}
	
	private static ITypeConstraint[] getCopy(ITypeConstraint[] constraints) {
		List l= Arrays.asList(constraints);
		return (ITypeConstraint[]) l.toArray(new ITypeConstraint[l.size()]);
	}

	private static ITypeConstraint[] sort(ITypeConstraint[] constraints) {
		//TODO bogus to sort by toString - will have to come up with something better
		Arrays.sort(constraints, new Comparator(){
			public int compare(Object o1, Object o2) {
				return o2.toString().compareTo(o1.toString());
			}
		});
		return constraints;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.experiments.ITypeConstraint#toResolvedString()
	 */
	public String toResolvedString() {
		StringBuffer buff= new StringBuffer();
		for (int i= 0; i < fConstraints.length; i++) {
			ITypeConstraint constraint= fConstraints[i];
			if (i > 0)
				buff.append(" or ");			 //$NON-NLS-1$
			buff.append(constraint.toResolvedString());
		}
		return buff.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.corext.refactoring.experiments.ITypeConstraint#isSimpleTypeConstraint()
	 */
	public boolean isSimpleTypeConstraint() {
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buff= new StringBuffer();
		for (int i= 0; i < fConstraints.length; i++) {
			ITypeConstraint constraint= fConstraints[i];
			if (i > 0)
				buff.append(" or ");			 //$NON-NLS-1$
			buff.append(constraint.toString());
		}
		return buff.toString();
	}

	public ITypeConstraint[] getConstraints() {
		return fConstraints;
	}
}
