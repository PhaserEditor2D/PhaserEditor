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
package org.eclipse.wst.jsdt.internal.ui.refactoring;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.jsdt.internal.corext.refactoring.tagging.IDelegateUpdating;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

/**
 * This is a helper class to keep a consistent design between refactorings
 * capable of creating delegates.
 * 
 * 
 * 
 */
public class DelegateUIHelper {

	public static Button generateDeprecateDelegateCheckbox(Composite parent, Refactoring refactoring) {
		final IDelegateUpdating updating= (IDelegateUpdating) refactoring.getAdapter(IDelegateUpdating.class);
		if (updating == null || !updating.canEnableDelegateUpdating())
			return null;
		final Button button= createCheckbox(parent, getDeprecateDelegateCheckBoxTitle(), loadDeprecateDelegateSetting(updating));
		updating.setDeprecateDelegates(button.getSelection());
		button.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				updating.setDeprecateDelegates(button.getSelection());
			}
		});
		return button;
	}

	public static Button generateLeaveDelegateCheckbox(Composite parent, Refactoring refactoring, boolean plural) {
		final IDelegateUpdating updating= (IDelegateUpdating) refactoring.getAdapter(IDelegateUpdating.class);
		if (updating == null || !updating.canEnableDelegateUpdating())
			return null;
		final Button button= createCheckbox(parent, updating.getDelegateUpdatingTitle(plural), loadLeaveDelegateSetting(updating));
		updating.setDelegateUpdating(button.getSelection());
		button.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				updating.setDelegateUpdating(button.getSelection());
			}
		});
		return button;
	}

	public static void saveLeaveDelegateSetting(Button button) {
		saveBooleanSetting(DELEGATE_UPDATING, button);
	}

	public static void saveDeprecateDelegateSetting(Button button) {
		saveBooleanSetting(DELEGATE_DEPRECATION, button);
	}

	public static boolean loadLeaveDelegateSetting(IDelegateUpdating refactoring) {
		return getBooleanSetting(DELEGATE_UPDATING, refactoring.getDelegateUpdating());
	}

	public static boolean loadDeprecateDelegateSetting(IDelegateUpdating refactoring) {
		return getBooleanSetting(DELEGATE_DEPRECATION, refactoring.getDeprecateDelegates());
	}

	public static String getDeprecateDelegateCheckBoxTitle() {
		return RefactoringMessages.DelegateCreator_deprecate_delegates;
	}

	// ************** Helper methods *******************

	/**
	 * Dialog settings key (value is of type boolean).
	 */
	public static final String DELEGATE_UPDATING= "delegateUpdating"; //$NON-NLS-1$

	/**
	 * Dialog settings key (value is of type boolean).
	 */
	public static final String DELEGATE_DEPRECATION= "delegateDeprecation"; //$NON-NLS-1$

	private DelegateUIHelper() {
		// no instances
	}

	private static Button createCheckbox(Composite parent, String title, boolean value) {
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(title);
		checkBox.setSelection(value);
		return checkBox;
	}

	private static boolean getBooleanSetting(String key, boolean defaultValue) {
		String update= JavaScriptPlugin.getDefault().getDialogSettings().get(key);
		if (update != null)
			return Boolean.valueOf(update).booleanValue();
		else
			return defaultValue;
	}

	private static void saveBooleanSetting(String key, Button button) {
		if (button != null && !button.isDisposed() && button.getEnabled())
			JavaScriptPlugin.getDefault().getDialogSettings().put(key, button.getSelection());
	}
}
