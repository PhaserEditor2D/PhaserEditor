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
package org.eclipse.wst.jsdt.internal.ui.compare;

import java.util.Map;
import java.util.ResourceBundle;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareViewerPane;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.StructureDiffViewer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;

class JavaStructureDiffViewer extends StructureDiffViewer {
	
	/**
	 * Toggles a boolean property of an <code>CompareConfiguration</code>.
	 */
	static class ChangePropertyAction extends Action {

		private CompareConfiguration fCompareConfiguration;
		private String fPropertyKey;
		private ResourceBundle fBundle;
		private String fPrefix;


		public ChangePropertyAction(ResourceBundle bundle, CompareConfiguration cc, String rkey, String pkey) {
			fPropertyKey= pkey;
			fBundle= bundle;
			fPrefix= rkey;
			JavaCompareUtilities.initAction(this, fBundle, fPrefix);
			setCompareConfiguration(cc);
		}

		public void run() {
			boolean b= !JavaCompareUtilities.getBoolean(fCompareConfiguration, fPropertyKey, false);
			setChecked(b);
			if (fCompareConfiguration != null)
				fCompareConfiguration.setProperty(fPropertyKey, new Boolean(b));
		}

		public void setChecked(boolean state) {
			super.setChecked(state);
			JavaCompareUtilities.initToggleAction(this, fBundle, fPrefix, state);
		}
		
		public void setCompareConfiguration(CompareConfiguration cc) {
			fCompareConfiguration= cc;
			setChecked(JavaCompareUtilities.getBoolean(fCompareConfiguration, fPropertyKey, false));
		}
	}

	private static final String SMART= "SMART"; //$NON-NLS-1$

	private ActionContributionItem fSmartActionItem;
	private JavaStructureCreator fStructureCreator;
	private boolean fThreeWay;

	public JavaStructureDiffViewer(Composite parent, CompareConfiguration configuration) {
		super(parent, configuration);
		fStructureCreator= new JavaStructureCreator();
		setStructureCreator(fStructureCreator);
	}
	
	/**
	 * Overridden to find and expand the first class.
	 */
	protected void initialSelection() {
		Object firstClass= null;
		Object o= getRoot();
		if (o != null) {
			Object[] children= getSortedChildren(o);
			if (children != null && children.length > 0) {
				for (int i= 0; i < children.length; i++) {
					o= children[i];
					Object[] sortedChildren= getSortedChildren(o);
					if (sortedChildren != null && sortedChildren.length > 0) {
						for (int j= 0; j < sortedChildren.length; j++) {
							o= sortedChildren[j];
							if (o instanceof DiffNode) {
								DiffNode dn= (DiffNode) o;
								ITypedElement e= dn.getId();
								if (e instanceof JavaNode) {
									JavaNode jn= (JavaNode) e;
									int tc= jn.getTypeCode();
									if (tc == JavaNode.CLASS || tc == JavaNode.INTERFACE) {
										firstClass= dn;
									}
								}
							}
						}
					}
				}
			}
		}
		if (firstClass != null)
			expandToLevel(firstClass, 1);
		else
			expandToLevel(2);
	}

	protected void compareInputChanged(ICompareInput input) {
		
		fThreeWay= input != null ? input.getAncestor() != null
							     : false;
		setSmartButtonVisible(fThreeWay);
		
		if (input != null) {
			Map compilerOptions= getCompilerOptions(input.getAncestor());
			if (compilerOptions == null)
				compilerOptions= getCompilerOptions(input.getLeft());
			if (compilerOptions == null)
				compilerOptions= getCompilerOptions(input.getRight());
			if (compilerOptions != null)
				fStructureCreator.setDefaultCompilerOptions(compilerOptions);
		}
		
		super.compareInputChanged(input);
	}
	
	private Map getCompilerOptions(ITypedElement input) {
		if (input instanceof IResourceProvider) {
			IResource resource= ((IResourceProvider) input).getResource();
			if (resource != null) {
				IJavaScriptElement element= JavaScriptCore.create(resource);
				if (element != null) {
					IJavaScriptProject javaProject= element.getJavaScriptProject();
					if (javaProject != null)
						return javaProject.getOptions(true);
				}
			}
		}
		return null;
	}
	
	/**
	 * Overriden to create a "smart" button in the viewer's pane control bar.
	 * <p>
	 * Clients can override this method and are free to decide whether they want to call
	 * the inherited method.
	 *
	 * @param toolBarManager the toolbar manager for which to add the buttons
	 */
	protected void createToolItems(ToolBarManager toolBarManager) {
		
		super.createToolItems(toolBarManager);
		
		IAction a= new ChangePropertyAction(getBundle(), getCompareConfiguration(), "action.Smart.", SMART); //$NON-NLS-1$
		fSmartActionItem= new ActionContributionItem(a);
		fSmartActionItem.setVisible(fThreeWay);
		toolBarManager.appendToGroup("modes", fSmartActionItem); //$NON-NLS-1$
	}
	
	protected void postDiffHook(Differencer differencer, IDiffContainer root, IProgressMonitor monitor) {
		if (fStructureCreator.canRewriteTree()) {
			boolean smart= JavaCompareUtilities.getBoolean(getCompareConfiguration(), SMART, false);
			if (smart && root != null)
				fStructureCreator.rewriteTree(differencer, root);
		}
	}
	
	/**
	 * Tracks property changes of the configuration object.
	 * Clients may override to track their own property changes.
	 * In this case they must call the inherited method.
	 */
	protected void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(SMART))
			diff();
		else
			super.propertyChange(event);
	}
	
	private void setSmartButtonVisible(boolean visible) {
		if (fSmartActionItem == null)
			return;
		Control c= getControl();
		if (c == null || c.isDisposed())
			return;
			
		fSmartActionItem.setVisible(visible);
		ToolBarManager tbm= CompareViewerPane.getToolBarManager(c.getParent());
		if (tbm != null) {
			tbm.update(true);
			ToolBar tb= tbm.getControl();
			if (!tb.isDisposed())
				tb.getParent().layout(true);
		}
	}
}
