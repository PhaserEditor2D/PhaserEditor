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
package org.eclipse.wst.jsdt.internal.ui.refactoring.nls;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;


public class MultiStateCellEditor extends CellEditor {
	
	private int fValue;
	private final int fStateCount;
	
	/**
	 * @param stateCount must be > 1
	 * @param initialValue initialValue
	 */
	public MultiStateCellEditor(Composite parent, int stateCount, int initialValue) {
		super(parent);
		Assert.isTrue(stateCount > 1, "incorrect state count"); //$NON-NLS-1$
		fStateCount= stateCount;
		
		Assert.isTrue(initialValue >= 0 && initialValue < stateCount, "incorrect initial value"); //$NON-NLS-1$
		fValue= initialValue;
		
		setValueValid(true);
	}

	/*
	 * @see org.eclipse.jface.viewers.CellEditor#activate()
	 */
	public void activate() {
		fValue= getNextValue(fStateCount, fValue);
		fireApplyEditorValue();
	}
	
	public static int getNextValue(int stateCount, int currentValue){
		Assert.isTrue(stateCount > 1, "incorrect state count"); //$NON-NLS-1$
		Assert.isTrue(currentValue >= 0 && currentValue < stateCount, "incorrect initial value"); //$NON-NLS-1$
		return (currentValue + 1) % stateCount;
	}

	/*
	 * @see org.eclipse.jface.viewers.CellEditor#createControl(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createControl(Composite parent) {
		return null;
	}

	/*
	 * @see org.eclipse.jface.viewers.CellEditor#doGetValue()
	 * @return the Integer value
	 */
	protected Object doGetValue() {
		return Integer.valueOf(fValue);
	}

	/*
	 * @see org.eclipse.jface.viewers.CellEditor#doSetFocus()
	 */
	protected void doSetFocus() {
		// Ignore
	}

	/*
	 * @see org.eclipse.jface.viewers.CellEditor#doSetValue(java.lang.Object)
	 * @param value an Integer value
	 * must be >=0 and < stateCount (value passed in the constructor)
	 */
	protected void doSetValue(Object value) {
		Assert.isTrue(value instanceof Integer, "value must be Integer"); //$NON-NLS-1$
		fValue = ((Integer) value).intValue();
		Assert.isTrue(fValue >= 0 && fValue < fStateCount, "invalid value"); //$NON-NLS-1$
	}
}
