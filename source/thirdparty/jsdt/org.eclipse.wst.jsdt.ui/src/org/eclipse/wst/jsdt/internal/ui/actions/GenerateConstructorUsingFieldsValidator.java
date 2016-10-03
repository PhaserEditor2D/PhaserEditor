/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.wst.jsdt.core.dom.IFunctionBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.dialogs.StatusInfo;

public class GenerateConstructorUsingFieldsValidator implements ISelectionStatusValidator {

	private GenerateConstructorUsingFieldsSelectionDialog fDialog;

	private final int fEntries;

	private List fSignatures;

	private ITypeBinding fType= null;

	public GenerateConstructorUsingFieldsValidator(GenerateConstructorUsingFieldsSelectionDialog dialog, ITypeBinding type, int entries) {
		fEntries= entries;
		fDialog= dialog;
		fType= type;
		fSignatures= getExistingConstructorSignatures();
	}

	public GenerateConstructorUsingFieldsValidator(int entries) {
		fEntries= entries;
		fType= null;
	}

	private int countSelectedFields(Object[] selection) {
		int count= 0;
		for (int index= 0; index < selection.length; index++) {
			if (selection[index] instanceof IVariableBinding)
				count++;
		}
		return count;
	}

	private void createSignature(final IFunctionBinding constructor, StringBuffer buffer, Object[] selection) {
		ITypeBinding types[]= constructor.getParameterTypes();
		for (int index= 0; index < types.length; index++)
			buffer.append(types[index].getName());
		if (selection != null) {
			for (int index= 0; index < selection.length; index++)
				if (selection[index] instanceof IVariableBinding)
					buffer.append(((IVariableBinding) selection[index]).getType().getErasure().getName());
		}
	}

	private List getExistingConstructorSignatures() {
		List existing= new ArrayList();
		IFunctionBinding[] methods= fType.getDeclaredMethods();
		for (int index= 0; index < methods.length; index++) {
			if (methods[index].isConstructor()) {
				StringBuffer buffer= new StringBuffer();
				createSignature(methods[index], buffer, null);
				existing.add(buffer.toString());
			}
		}
		return existing;
	}

	public IStatus validate(Object[] selection) {
		StringBuffer buffer= new StringBuffer();
		final IFunctionBinding constructor= fDialog.getSuperConstructorChoice();
		createSignature(constructor, buffer, selection);
		if (fSignatures.contains(buffer.toString()))
			return new StatusInfo(IStatus.WARNING, ActionMessages.GenerateConstructorUsingFieldsAction_error_duplicate_constructor); 
		return new StatusInfo(IStatus.INFO, Messages.format(ActionMessages.GenerateConstructorUsingFieldsAction_fields_selected, new Object[] { String.valueOf(countSelectedFields(selection)), String.valueOf(fEntries)})); 
	}
}
