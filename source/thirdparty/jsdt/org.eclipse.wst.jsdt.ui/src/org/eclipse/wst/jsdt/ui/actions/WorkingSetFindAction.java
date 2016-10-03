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
package org.eclipse.wst.jsdt.ui.actions;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;

/**
 * Wraps a <code>JavaElementSearchActions</code> to find its results
 * in the specified working set.
 * <p>
 * The action is applicable to selections and Search view entries
 * representing a JavaScript element.
 * 
 * <p>
 * Note: This class is for internal use only. Clients should not use this class.
 * </p>
 * 
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public class WorkingSetFindAction extends FindAction {

	private FindAction fAction;

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public WorkingSetFindAction(IWorkbenchSite site, FindAction action, String workingSetName) {
		super(site);
		init(action, workingSetName);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public WorkingSetFindAction(JavaEditor editor, FindAction action, String workingSetName) {
		super(editor);
		init(action, workingSetName);
	}

	Class[] getValidTypes() {
		return null; // ignore, we override canOperateOn
	}
	
	void init() {
		// ignore: do our own init in 'init(FindAction, String)'
	}
	
	private void init(FindAction action, String workingSetName) {
		Assert.isNotNull(action);
		fAction= action;
		setText(workingSetName);
		setImageDescriptor(action.getImageDescriptor());
		setToolTipText(action.getToolTipText());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.WORKING_SET_FIND_ACTION);
	}
	
	public void run(IJavaScriptElement element) {
		fAction.run(element);
	}

	boolean canOperateOn(IJavaScriptElement element) {
		return fAction.canOperateOn(element);
	}

	int getLimitTo() {
		return -1;
	}

	String getOperationUnavailableMessage() {
		return fAction.getOperationUnavailableMessage();
	}

}
