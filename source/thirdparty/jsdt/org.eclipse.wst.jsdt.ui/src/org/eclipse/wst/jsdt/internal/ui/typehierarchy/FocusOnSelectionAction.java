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
package org.eclipse.wst.jsdt.internal.ui.typehierarchy;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.util.SelectionUtil;
import org.eclipse.wst.jsdt.ui.ITypeHierarchyViewPart;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;

/**
 * Refocuses the type hierarchy on the currently selection type.
 */
public class FocusOnSelectionAction extends Action {
		
	private ITypeHierarchyViewPart fViewPart;
	
	public FocusOnSelectionAction(ITypeHierarchyViewPart part) {
		super(TypeHierarchyMessages.FocusOnSelectionAction_label); 
		setDescription(TypeHierarchyMessages.FocusOnSelectionAction_description); 
		setToolTipText(TypeHierarchyMessages.FocusOnSelectionAction_tooltip); 
		fViewPart= part;
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.FOCUS_ON_SELECTION_ACTION);
	}
	
	private ISelection getSelection() {
		ISelectionProvider provider= fViewPart.getSite().getSelectionProvider();
		if (provider != null) {
			return provider.getSelection();
		}
		return null;
	}
	

	/*
	 * @see Action#run
	 */
	public void run() {
		Object element= SelectionUtil.getSingleElement(getSelection());
		if (element instanceof IType) {
			fViewPart.setInputElement((IType)element);
		}
	}	
	
	public boolean canActionBeAdded() {
		Object element= SelectionUtil.getSingleElement(getSelection());
		if (element instanceof IType) {
			IType type= (IType)element;
			setText(Messages.format(
					TypeHierarchyMessages.FocusOnSelectionAction_label, 
					JavaScriptElementLabels.getTextLabel(type, 0))); 
			return true;
		}
		return false;
	}
}
