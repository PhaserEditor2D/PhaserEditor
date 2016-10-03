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

package org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.core.IAccessRule;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.wizards.NewWizardMessages;

public class AccessRulesLabelProvider extends LabelProvider implements ITableLabelProvider {
	
	public AccessRulesLabelProvider() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
	 */
	public Image getColumnImage(Object element, int columnIndex) {
		if (element instanceof IAccessRule) {
			IAccessRule rule= (IAccessRule) element;
			if (columnIndex == 0) {
				return getResolutionImage(rule.getKind());
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
	 */
	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof IAccessRule) {
			IAccessRule rule= (IAccessRule) element;
			if (columnIndex == 0) {
				return getResolutionLabel(rule.getKind());
			} else {
				return rule.getPattern().toString();
			}
		}
		return element.toString();
	}
	
	public static Image getResolutionImage(int kind) {
		switch (kind) {
			case IAccessRule.K_ACCESSIBLE:
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_NLS_TRANSLATE);
			case IAccessRule.K_DISCOURAGED:
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_WARNING);
			case IAccessRule.K_NON_ACCESSIBLE:
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_ERROR);
		}
		return null;
	}
	
	
	public static String getResolutionLabel(int kind) {
		switch (kind) {
			case IAccessRule.K_ACCESSIBLE:
				return NewWizardMessages.AccessRulesLabelProvider_kind_accessible; 
			case IAccessRule.K_DISCOURAGED:
				return NewWizardMessages.AccessRulesLabelProvider_kind_discouraged; 
			case IAccessRule.K_NON_ACCESSIBLE:
				return NewWizardMessages.AccessRulesLabelProvider_kind_non_accessible; 
		}
		return ""; //$NON-NLS-1$
	}
}
