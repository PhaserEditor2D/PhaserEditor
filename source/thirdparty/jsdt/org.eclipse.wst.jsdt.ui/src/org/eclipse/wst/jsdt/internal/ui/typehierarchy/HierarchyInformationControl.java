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
package org.eclipse.wst.jsdt.internal.ui.typehierarchy;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.keys.KeySequence;
import org.eclipse.ui.keys.SWTKeySupport;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IImportDeclaration;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.MethodOverrideTester;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.text.AbstractInformationControl;
import org.eclipse.wst.jsdt.internal.ui.typehierarchy.SuperTypeHierarchyViewer.SuperTypeHierarchyContentProvider;
import org.eclipse.wst.jsdt.internal.ui.typehierarchy.TraditionalHierarchyViewer.TraditionalHierarchyContentProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ColoredViewersManager;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.ProblemsLabelDecorator;
import org.eclipse.wst.jsdt.ui.actions.IJavaEditorActionDefinitionIds;

/**
 * Show hierarchy in light-weight control.
 * 
 * 
 */
public class HierarchyInformationControl extends AbstractInformationControl {
	
	private TypeHierarchyLifeCycle fLifeCycle;
	private HierarchyLabelProvider fLabelProvider;
	private KeyAdapter fKeyAdapter;
	
	private Object[] fOtherExpandedElements;
	private TypeHierarchyContentProvider fOtherContentProvider;
	
	private IFunction fFocus; // method to filter for or null if type hierarchy
	private boolean fDoFilter;
	
	private MethodOverrideTester fMethodOverrideTester;

	public HierarchyInformationControl(Shell parent, int shellStyle, int treeStyle) {
		super(parent, shellStyle, treeStyle, IJavaEditorActionDefinitionIds.OPEN_HIERARCHY, true);
		fOtherExpandedElements= null;
		fDoFilter= true;
		fMethodOverrideTester= null;
	}
	
	private KeyAdapter getKeyAdapter() {
		if (fKeyAdapter == null) {
			fKeyAdapter= new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					int accelerator = SWTKeySupport.convertEventToUnmodifiedAccelerator(e);
					KeySequence keySequence = KeySequence.getInstance(SWTKeySupport.convertAcceleratorToKeyStroke(accelerator));
					KeySequence[] sequences= getInvokingCommandKeySequences();
					if (sequences == null)
						return;
					
					for (int i= 0; i < sequences.length; i++) {
						if (sequences[i].equals(keySequence)) {
							e.doit= false;
							toggleHierarchy();
							return;
						}
					}
				}
			};			
		}
		return fKeyAdapter;		
	}

	/**
	 * {@inheritDoc}
	 */
	protected boolean hasHeader() {
		return true;
	}

	protected Text createFilterText(Composite parent) {
		// text set later
		Text text= super.createFilterText(parent);
		text.addKeyListener(getKeyAdapter());
		return text;
	}	
		
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.text.JavaOutlineInformationControl#createTreeViewer(org.eclipse.swt.widgets.Composite, int)
	 */
	protected TreeViewer createTreeViewer(Composite parent, int style) {
		Tree tree= new Tree(parent, SWT.SINGLE | (style & ~SWT.MULTI));
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= tree.getItemHeight() * 12;
		tree.setLayoutData(gd);

		TreeViewer treeViewer= new TreeViewer(tree);
		ColoredViewersManager.install(treeViewer);
		treeViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return element instanceof IType;
			}
		});		
		
		fLifeCycle= new TypeHierarchyLifeCycle(false);

		treeViewer.setComparator(new HierarchyViewerSorter(fLifeCycle));
		treeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);

		fLabelProvider= new HierarchyLabelProvider(fLifeCycle);
		fLabelProvider.setFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return hasFocusMethod((IType) element);
			}
		});	

		fLabelProvider.setTextFlags(JavaScriptElementLabels.ALL_DEFAULT | JavaScriptElementLabels.T_POST_QUALIFIED);
		fLabelProvider.addLabelDecorator(new ProblemsLabelDecorator(null));
		treeViewer.setLabelProvider(fLabelProvider);
		
		treeViewer.getTree().addKeyListener(getKeyAdapter());	
		
		return treeViewer;
	}
	
	protected boolean hasFocusMethod(IType type) {
		if (fFocus == null) {
			return true;
		}
		if (type.equals(fFocus.getDeclaringType())) {
			return true;
		}
		
		try {
			IFunction method= findMethod(fFocus, type);
			if (method != null) {
				// check visibility
				IPackageFragment pack= (IPackageFragment) fFocus.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT);
				if (JavaModelUtil.isVisibleInHierarchy(method, pack)) {
					return true;
				}
			}
		} catch (JavaScriptModelException e) {
			// ignore
			JavaScriptPlugin.log(e);
		}
		return false;			
		
	}
	
	private IFunction findMethod(IFunction filterMethod, IType typeToFindIn) throws JavaScriptModelException {
		IType filterType= filterMethod.getDeclaringType();
		ITypeHierarchy hierarchy= fLifeCycle.getHierarchy();
		
		boolean filterOverrides= JavaModelUtil.isSuperType(hierarchy, typeToFindIn, filterType);
		IType focusType= filterOverrides ? filterType : typeToFindIn;
		
		if (fMethodOverrideTester == null || !fMethodOverrideTester.getFocusType().equals(focusType)) {
			fMethodOverrideTester= new MethodOverrideTester(focusType, hierarchy);
		}

		if (filterOverrides) {
			return fMethodOverrideTester.findOverriddenMethodInType(typeToFindIn, filterMethod);
		} else {
			return fMethodOverrideTester.findOverridingMethodInType(typeToFindIn, filterMethod);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setInput(Object information) {
		if (!(information instanceof IJavaScriptElement)) {
			inputChanged(null, null);
			return;
		}
		IJavaScriptElement input= null;
		IFunction locked= null;
		try {
			IJavaScriptElement elem= (IJavaScriptElement) information;
			if (elem.getElementType() == IJavaScriptElement.LOCAL_VARIABLE) {
				elem= elem.getParent();
			}
			
			switch (elem.getElementType()) {
				case IJavaScriptElement.JAVASCRIPT_PROJECT :
				case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT :
				case IJavaScriptElement.PACKAGE_FRAGMENT :
				case IJavaScriptElement.TYPE :
					input= elem;
					break;
				case IJavaScriptElement.JAVASCRIPT_UNIT :
					input= ((IJavaScriptUnit) elem).findPrimaryType();
					break;
				case IJavaScriptElement.CLASS_FILE :
					input= ((IClassFile) elem).getType();
					break;
				case IJavaScriptElement.METHOD :
					IFunction method= (IFunction) elem;
					if (!method.isConstructor()) {
						locked= method;				
					}
					input= method.getDeclaringType();
					break;
				case IJavaScriptElement.FIELD :
				case IJavaScriptElement.INITIALIZER :
					input= ((IMember) elem).getDeclaringType();
					break;
				case IJavaScriptElement.IMPORT_DECLARATION :
					IImportDeclaration decl= (IImportDeclaration) elem;
					if (decl.isOnDemand()) {
						input= JavaModelUtil.findTypeContainer(decl.getJavaScriptProject(), Signature.getQualifier(decl.getElementName()));
					} else {
						input= decl.getJavaScriptProject().findType(decl.getElementName());
					}
					break;
				default :
					JavaScriptPlugin.logErrorMessage("Element unsupported by the hierarchy: " + elem.getClass()); //$NON-NLS-1$
					input= null;
			}
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
		}
		
		super.setTitleText(getHeaderLabel(locked == null ? input : locked));
		try {
			fLifeCycle.ensureRefreshedTypeHierarchy(input, JavaScriptPlugin.getActiveWorkbenchWindow());
		} catch (InvocationTargetException e1) {
			input= null;
		} catch (InterruptedException e1) {
			dispose();
			return;
		}
		IMember[] memberFilter= locked != null ? new IMember[] { locked } : null;
		
		TraditionalHierarchyContentProvider contentProvider= new TraditionalHierarchyContentProvider(fLifeCycle);
		contentProvider.setMemberFilter(memberFilter);
		getTreeViewer().setContentProvider(contentProvider);		
		
		fOtherContentProvider= new SuperTypeHierarchyContentProvider(fLifeCycle);
		fOtherContentProvider.setMemberFilter(memberFilter);
		
		fFocus= locked;
		
		Object[] topLevelObjects= contentProvider.getElements(fLifeCycle);
		if (topLevelObjects.length > 0 && contentProvider.getChildren(topLevelObjects[0]).length > 40) {
			fDoFilter= false;
		} else {
			getTreeViewer().addFilter(new NamePatternFilter());
		}

		Object selection= null;
		if (input instanceof IMember) {
			selection=  input;
		} else if (topLevelObjects.length > 0) {
			selection=  topLevelObjects[0];
		}
		inputChanged(fLifeCycle, selection);
	}
	
	protected void stringMatcherUpdated() {
		if (fDoFilter) {
			super.stringMatcherUpdated(); // refresh the view
		} else {
			selectFirstMatch();
		}
	}
	
	protected void toggleHierarchy() {
		TreeViewer treeViewer= getTreeViewer();
		
		treeViewer.getTree().setRedraw(false);
		
		Object[] expandedElements= treeViewer.getExpandedElements();
		TypeHierarchyContentProvider contentProvider= (TypeHierarchyContentProvider) treeViewer.getContentProvider();
		treeViewer.setContentProvider(fOtherContentProvider);

		treeViewer.refresh();
		if (fOtherExpandedElements != null) {
			treeViewer.setExpandedElements(fOtherExpandedElements);
		} else {
			treeViewer.expandAll();
		}
		
		treeViewer.getTree().setRedraw(true);
		
		fOtherContentProvider= contentProvider;
		fOtherExpandedElements= expandedElements;
		
		updateStatusFieldText();
	}
	
	
	private String getHeaderLabel(IJavaScriptElement input) {
		if (input instanceof IFunction) {
			String[] args= { input.getParent().getElementName(), JavaScriptElementLabels.getElementLabel(input, JavaScriptElementLabels.ALL_DEFAULT) };
			return Messages.format(TypeHierarchyMessages.HierarchyInformationControl_methodhierarchy_label, args); 
		} else if (input != null) {
			String arg= JavaScriptElementLabels.getElementLabel(input, JavaScriptElementLabels.DEFAULT_QUALIFIED);
			return Messages.format(TypeHierarchyMessages.HierarchyInformationControl_hierarchy_label, arg);	 
		} else {
			return ""; //$NON-NLS-1$
		}
	}
	
	protected String getStatusFieldText() {
		KeySequence[] sequences= getInvokingCommandKeySequences();
		String keyName= ""; //$NON-NLS-1$
		if (sequences != null && sequences.length > 0)
			keyName= sequences[0].format();
		
		if (fOtherContentProvider instanceof TraditionalHierarchyContentProvider) {
			return Messages.format(TypeHierarchyMessages.HierarchyInformationControl_toggle_traditionalhierarchy_label, keyName); 
		} else {
			return Messages.format(TypeHierarchyMessages.HierarchyInformationControl_toggle_superhierarchy_label, keyName); 
		}
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.AbstractInformationControl#getId()
	 */
	protected String getId() {
		return "org.eclipse.wst.jsdt.internal.ui.typehierarchy.QuickHierarchy"; //$NON-NLS-1$
	}

	/**
	 * {@inheritDoc}
	 */
	protected Object getSelectedElement() {
		Object selectedElement= super.getSelectedElement();
		if (selectedElement instanceof IType && fFocus != null) {
			IType type= (IType) selectedElement;
			try {
				return findMethod(fFocus, type);
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
			}
		}
		return selectedElement;
	}
}
