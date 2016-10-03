/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.ui.actions;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IImportDeclaration;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.wst.jsdt.internal.ui.search.SearchMessages;
import org.eclipse.wst.jsdt.internal.ui.search.SearchUtil;
import org.eclipse.wst.jsdt.ui.search.ElementQuerySpecification;
import org.eclipse.wst.jsdt.ui.search.QuerySpecification;

/**
 * Finds references of the selected element in the workspace.
 * The action is applicable to selections representing a JavaScript element.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
  *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public class FindReferencesAction extends FindAction {

	/**
	 * Creates a new <code>FindReferencesAction</code>. The action 
	 * requires that the selection provided by the site's selection provider is of type 
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public FindReferencesAction(IWorkbenchSite site) {
		super(site);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the JavaScript editor
	 */
	public FindReferencesAction(JavaEditor editor) {
		super(editor);
	}
	
	Class[] getValidTypes() {
		return new Class[] { IJavaScriptUnit.class, IType.class, IFunction.class, IField.class, IImportDeclaration.class, IPackageFragment.class, ILocalVariable.class };
	}
	
	void init() {
		setText(SearchMessages.Search_FindReferencesAction_label); 
		setToolTipText(SearchMessages.Search_FindReferencesAction_tooltip); 
		setImageDescriptor(JavaPluginImages.DESC_OBJS_SEARCH_REF);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.FIND_REFERENCES_IN_WORKSPACE_ACTION);
	}

	int getLimitTo() {
		return IJavaScriptSearchConstants.REFERENCES;
	}	
	
	QuerySpecification createQuery(IJavaScriptElement element) throws JavaScriptModelException, InterruptedException {
		JavaSearchScopeFactory factory= JavaSearchScopeFactory.getInstance();
		boolean isInsideJRE= factory.isInsideJRE(element);
		
		IJavaScriptSearchScope scope= factory.createWorkspaceScope(isInsideJRE);
		String description= factory.getWorkspaceScopeDescription(isInsideJRE);
		return new ElementQuerySpecification(element, getLimitTo(), scope, description);
	}

	public void run(IJavaScriptElement element) {
		SearchUtil.warnIfBinaryConstant(element, getShell());
		super.run(element);
	}
}
