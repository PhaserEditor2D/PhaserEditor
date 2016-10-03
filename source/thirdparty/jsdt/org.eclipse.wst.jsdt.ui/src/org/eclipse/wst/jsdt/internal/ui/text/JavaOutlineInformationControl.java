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
package org.eclipse.wst.jsdt.internal.ui.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.KeySequence;
import org.eclipse.ui.keys.SWTKeySupport;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.ITypeRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.MethodOverrideTester;
import org.eclipse.wst.jsdt.internal.corext.util.SuperTypeHierarchyCache;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaUIMessages;
import org.eclipse.wst.jsdt.internal.ui.actions.CategoryFilterActionGroup;
import org.eclipse.wst.jsdt.internal.ui.typehierarchy.AbstractHierarchyViewerSorter;
import org.eclipse.wst.jsdt.internal.ui.util.StringMatcher;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ColoredViewersManager;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.MemberFilter;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.OverrideIndicatorLabelDecorator;
import org.eclipse.wst.jsdt.ui.ProblemsLabelDecorator;
import org.eclipse.wst.jsdt.ui.StandardJavaScriptElementContentProvider;

/**
 * Show outline in light-weight control.
 *
 * 
 */
public class JavaOutlineInformationControl extends AbstractInformationControl {

	private KeyAdapter fKeyAdapter;
	private OutlineContentProvider fOutlineContentProvider;
	private IJavaScriptElement fInput= null;

	private OutlineSorter fOutlineSorter;

	private OutlineLabelProvider fInnerLabelProvider;

	private boolean fShowOnlyMainType;
	private LexicalSortingAction fLexicalSortingAction;
	private SortByDefiningTypeAction fSortByDefiningTypeAction;
	private ShowOnlyMainTypeAction fShowOnlyMainTypeAction;
	private Map fTypeHierarchies= new HashMap();
	
	/**
	 * Category filter action group.
	 * 
	 */
	private CategoryFilterActionGroup fCategoryFilterActionGroup;
	private String fPattern;

	private class OutlineLabelProvider extends AppearanceAwareLabelProvider {

		private boolean fShowDefiningType;

		private OutlineLabelProvider() {
			super(AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS |  JavaScriptElementLabels.F_APP_TYPE_SIGNATURE | JavaScriptElementLabels.ALL_CATEGORY, AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS);
		}

		/*
		 * @see ILabelProvider#getText
		 */
		public String getText(Object element) {
			String text= super.getText(element);
			if (fShowDefiningType) {
				try {
					IType type= getDefiningType(element);
					if (type != null) {
						StringBuffer buf= new StringBuffer(super.getText(type));
						buf.append(JavaScriptElementLabels.CONCAT_STRING);
						buf.append(text);
						return buf.toString();
					}
				} catch (JavaScriptModelException e) {
				}
			}
			return text;
		}

		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaUILabelProvider#getForeground(java.lang.Object)
		 */
		public Color getForeground(Object element) {
			if (fOutlineContentProvider.isShowingInheritedMembers()) {
				if (element instanceof IJavaScriptElement) {
					IJavaScriptElement je= (IJavaScriptElement)element;
					if (fInput.getElementType() == IJavaScriptElement.CLASS_FILE)
						je= je.getAncestor(IJavaScriptElement.CLASS_FILE);
					else
						je= je.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
					if (fInput.equals(je)) {
						return null;
					}
				}
				return JFaceResources.getColorRegistry().get(ColoredViewersManager.INHERITED_COLOR_NAME);
			}
			return null;
		}

		public void setShowDefiningType(boolean showDefiningType) {
			fShowDefiningType= showDefiningType;
		}

		public boolean isShowDefiningType() {
			return fShowDefiningType;
		}
		
		private IType getDefiningType(Object element) throws JavaScriptModelException {
			int kind= ((IJavaScriptElement) element).getElementType();
		
			if (kind != IJavaScriptElement.METHOD && kind != IJavaScriptElement.FIELD && kind != IJavaScriptElement.INITIALIZER) {
				return null;
			}
			IType declaringType= ((IMember) element).getDeclaringType();
			if (kind != IJavaScriptElement.METHOD) {
				return declaringType;
			}
			if (declaringType == null) {
			    return null;
			}
			ITypeHierarchy hierarchy= getSuperTypeHierarchy(declaringType);
			if (hierarchy == null) {
				return declaringType;
			}
			IFunction method= (IFunction) element;
			MethodOverrideTester tester= new MethodOverrideTester(declaringType, hierarchy);
			IFunction res= tester.findDeclaringMethod(method, true);
			if (res == null || method.equals(res)) {
				return declaringType;
			}
			return res.getDeclaringType();
		}
	}


	private class OutlineTreeViewer extends TreeViewer {

		private boolean fIsFiltering= false;

		private OutlineTreeViewer(Tree tree) {
			super(tree);
			
		}

		/**
		 * {@inheritDoc}
		 */
		protected Object[] getFilteredChildren(Object parent) {
			Object[] result = getRawChildren(parent);
			int unfilteredChildren= result.length;
			ViewerFilter[] filters = getFilters();
			if (filters != null) {
				for (int i= 0; i < filters.length; i++)
					result = filters[i].filter(this, parent, result);
			}
			fIsFiltering= unfilteredChildren != result.length;
			return result;
		}

		/**
		 * {@inheritDoc}
		 */
		protected void internalExpandToLevel(Widget node, int level) {
			if (!fIsFiltering && node instanceof TreeItem && getMatcher() == null) {
				TreeItem treeItem= (TreeItem)node;
				if (treeItem.getParentItem() != null && treeItem.getData() instanceof IJavaScriptElement) {
					IJavaScriptElement je= (IJavaScriptElement) treeItem.getData();
					if (je.getElementType() == IJavaScriptElement.IMPORT_CONTAINER || isInnerType(je)) {
						setExpanded(treeItem, false);
						return;
					}
				}
			}
			super.internalExpandToLevel(node, level);
		}

		private boolean isInnerType(IJavaScriptElement element) {
			if (element != null && element.getElementType() == IJavaScriptElement.TYPE) {
				IType type= (IType)element;
				try {
					return type.isMember();
				} catch (JavaScriptModelException e) {
					IJavaScriptElement parent= type.getParent();
					if (parent != null) {
						int parentElementType= parent.getElementType();
						return (parentElementType != IJavaScriptElement.JAVASCRIPT_UNIT && parentElementType != IJavaScriptElement.CLASS_FILE);
					}
				}
			}
			return false;
		}
	}


	private class OutlineContentProvider extends StandardJavaScriptElementContentProvider {

		private boolean fShowInheritedMembers;

		/**
		 * Creates a new Outline content provider.
		 *
		 * @param showInheritedMembers <code>true</code> iff inherited members are shown
		 */
		private OutlineContentProvider(boolean showInheritedMembers) {
			super(true);
			fShowInheritedMembers= showInheritedMembers;
		}

		public boolean isShowingInheritedMembers() {
			return fShowInheritedMembers;
		}

		public void toggleShowInheritedMembers() {
			Tree tree= getTreeViewer().getTree();

			tree.setRedraw(false);
			fShowInheritedMembers= !fShowInheritedMembers;
			getTreeViewer().refresh();
			getTreeViewer().expandToLevel(2);

			// reveal selection
			Object selectedElement= getSelectedElement();
			if (selectedElement != null)
				getTreeViewer().reveal(selectedElement);

			tree.setRedraw(true);
		}

		/**
		 * {@inheritDoc}
		 */
		public Object[] getChildren(Object element) {
			if (fShowOnlyMainType) {
				if (element instanceof ITypeRoot) {
					element= ((ITypeRoot)element).findPrimaryType();
				}

				if (element == null)
					return NO_CHILDREN;
			}

			if (fShowInheritedMembers && element instanceof IType) {
				IType type= (IType)element;
				if (type.getDeclaringType() == null) {
					ITypeHierarchy th= getSuperTypeHierarchy(type);
					if (th != null) {
						List children= new ArrayList();
						IType[] superClasses= th.getAllSuperclasses(type);
						children.addAll(Arrays.asList(super.getChildren(type)));
						for (int i= 0, scLength= superClasses.length; i < scLength; i++)
							children.addAll(Arrays.asList(super.getChildren(superClasses[i])));
						return children.toArray();
					}
				}
			}
			return super.getChildren(element);
		}

		/**
		 * {@inheritDoc}
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			super.inputChanged(viewer, oldInput, newInput);
			fTypeHierarchies.clear();
		}

		/**
		 * {@inheritDoc}
		 */
		public void dispose() {
			super.dispose();
			if (fCategoryFilterActionGroup != null) {
				fCategoryFilterActionGroup.dispose();
				fCategoryFilterActionGroup= null;
			}
			fTypeHierarchies.clear();
		}
	}


	private class ShowOnlyMainTypeAction extends Action {

		private static final String STORE_GO_INTO_TOP_LEVEL_TYPE_CHECKED= "GoIntoTopLevelTypeAction.isChecked"; //$NON-NLS-1$

		private TreeViewer fOutlineViewer;

		private ShowOnlyMainTypeAction(TreeViewer outlineViewer) {
			super(TextMessages.JavaOutlineInformationControl_GoIntoTopLevelType_label, IAction.AS_CHECK_BOX);
			setToolTipText(TextMessages.JavaOutlineInformationControl_GoIntoTopLevelType_tooltip);
			setDescription(TextMessages.JavaOutlineInformationControl_GoIntoTopLevelType_description);

			JavaPluginImages.setLocalImageDescriptors(this, "gointo_toplevel_type.gif"); //$NON-NLS-1$

			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.GO_INTO_TOP_LEVEL_TYPE_ACTION);

			fOutlineViewer= outlineViewer;

			boolean showclass= getDialogSettings().getBoolean(STORE_GO_INTO_TOP_LEVEL_TYPE_CHECKED);
			setTopLevelTypeOnly(showclass);
		}

		/*
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			setTopLevelTypeOnly(!fShowOnlyMainType);
		}

		private void setTopLevelTypeOnly(boolean show) {
			fShowOnlyMainType= show;
			setChecked(show);

			Tree tree= fOutlineViewer.getTree();
			tree.setRedraw(false);

			fOutlineViewer.refresh(false);
			if (!fShowOnlyMainType)
				fOutlineViewer.expandToLevel(2);


			// reveal selection
			Object selectedElement= getSelectedElement();
			if (selectedElement != null)
				fOutlineViewer.reveal(selectedElement);

			tree.setRedraw(true);

			getDialogSettings().put(STORE_GO_INTO_TOP_LEVEL_TYPE_CHECKED, show);
		}
	}

	private class OutlineSorter extends AbstractHierarchyViewerSorter {

		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.typehierarchy.AbstractHierarchyViewerSorter#getHierarchy(org.eclipse.wst.jsdt.core.IType)
		 * 
		 */
		protected ITypeHierarchy getHierarchy(IType type) {
			return getSuperTypeHierarchy(type);
		}

		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.typehierarchy.AbstractHierarchyViewerSorter#isSortByDefiningType()
		 * 
		 */
		public boolean isSortByDefiningType() {
			return fSortByDefiningTypeAction.isChecked();
		}

		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.typehierarchy.AbstractHierarchyViewerSorter#isSortAlphabetically()
		 * 
		 */
		public boolean isSortAlphabetically() {
			return fLexicalSortingAction.isChecked();
		}
	}


	private class LexicalSortingAction extends Action {

		private static final String STORE_LEXICAL_SORTING_CHECKED= "LexicalSortingAction.isChecked"; //$NON-NLS-1$

		private TreeViewer fOutlineViewer;

		private LexicalSortingAction(TreeViewer outlineViewer) {
			super(TextMessages.JavaOutlineInformationControl_LexicalSortingAction_label, IAction.AS_CHECK_BOX);
			setToolTipText(TextMessages.JavaOutlineInformationControl_LexicalSortingAction_tooltip);
			setDescription(TextMessages.JavaOutlineInformationControl_LexicalSortingAction_description);

			JavaPluginImages.setLocalImageDescriptors(this, "alphab_sort_co.gif"); //$NON-NLS-1$

			fOutlineViewer= outlineViewer;

			boolean checked=getDialogSettings().getBoolean(STORE_LEXICAL_SORTING_CHECKED);
			setChecked(checked);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.LEXICAL_SORTING_BROWSING_ACTION);
		}

		public void run() {
			valueChanged(isChecked(), true);
		}

		private void valueChanged(final boolean on, boolean store) {
			setChecked(on);
			BusyIndicator.showWhile(fOutlineViewer.getControl().getDisplay(), new Runnable() {
				public void run() {
					fOutlineViewer.refresh(false);
				}
			});

			if (store)
				getDialogSettings().put(STORE_LEXICAL_SORTING_CHECKED, on);
		}
	}


	private class SortByDefiningTypeAction extends Action {

		private static final String STORE_SORT_BY_DEFINING_TYPE_CHECKED= "SortByDefiningType.isChecked"; //$NON-NLS-1$

		private TreeViewer fOutlineViewer;

		/**
		 * Creates the action.
		 *
		 * @param outlineViewer the outline viewer
		 */
		private SortByDefiningTypeAction(TreeViewer outlineViewer) {
			super(TextMessages.JavaOutlineInformationControl_SortByDefiningTypeAction_label);
			setDescription(TextMessages.JavaOutlineInformationControl_SortByDefiningTypeAction_description);
			setToolTipText(TextMessages.JavaOutlineInformationControl_SortByDefiningTypeAction_tooltip);

			JavaPluginImages.setLocalImageDescriptors(this, "definingtype_sort_co.gif"); //$NON-NLS-1$

			fOutlineViewer= outlineViewer;

			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.SORT_BY_DEFINING_TYPE_ACTION);

			boolean state= getDialogSettings().getBoolean(STORE_SORT_BY_DEFINING_TYPE_CHECKED);
			setChecked(state);
			fInnerLabelProvider.setShowDefiningType(state);
		}

		/*
		 * @see Action#actionPerformed
		 */
		public void run() {
			BusyIndicator.showWhile(fOutlineViewer.getControl().getDisplay(), new Runnable() {
				public void run() {
					fInnerLabelProvider.setShowDefiningType(isChecked());
					getDialogSettings().put(STORE_SORT_BY_DEFINING_TYPE_CHECKED, isChecked());

					setMatcherString(fPattern, false);
					fOutlineViewer.refresh(true);

					// reveal selection
					Object selectedElement= getSelectedElement();
					if (selectedElement != null)
						fOutlineViewer.reveal(selectedElement);
				}
			});
		}
	}
	
	/**
	 * String matcher that can match two patterns.
	 * 
	 * 
	 */
	private static class OrStringMatcher extends StringMatcher {
		
		private StringMatcher fMatcher1;
		private StringMatcher fMatcher2;
		
		private OrStringMatcher(String pattern1, String pattern2, boolean ignoreCase, boolean foo) {
			super("", false, false); //$NON-NLS-1$
			fMatcher1= new StringMatcher(pattern1, ignoreCase, false);
			fMatcher2= new StringMatcher(pattern2, ignoreCase, false);
		}
		
		public boolean match(String text) {
			return fMatcher2.match(text) || fMatcher1.match(text);
		}
		
	}


	/**
	 * Creates a new Java outline information control.
	 *
	 * @param parent
	 * @param shellStyle
	 * @param treeStyle
	 * @param commandId
	 */
	public JavaOutlineInformationControl(Shell parent, int shellStyle, int treeStyle, String commandId) {
		super(parent, shellStyle, treeStyle, commandId, true);
	}

	/**
	 * {@inheritDoc}
	 */
	protected Text createFilterText(Composite parent) {
		Text text= super.createFilterText(parent);
		text.addKeyListener(getKeyAdapter());
		return text;
	}

	/**
	 * {@inheritDoc}
	 */
	protected TreeViewer createTreeViewer(Composite parent, int style) {
		Tree tree= new Tree(parent, SWT.SINGLE | (style & ~SWT.MULTI));
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= tree.getItemHeight() * 12;
		tree.setLayoutData(gd);

		final TreeViewer treeViewer= new OutlineTreeViewer(tree);
		ColoredViewersManager.install(treeViewer);

		// Hard-coded filters
		treeViewer.addFilter(new NamePatternFilter());
		treeViewer.addFilter(new MemberFilter());

		fInnerLabelProvider= new OutlineLabelProvider();
		fInnerLabelProvider.addLabelDecorator(new ProblemsLabelDecorator(null));
		IDecoratorManager decoratorMgr= PlatformUI.getWorkbench().getDecoratorManager();
		if (decoratorMgr.getEnabled("org.eclipse.wst.jsdt.ui.override.decorator")) //$NON-NLS-1$
			fInnerLabelProvider.addLabelDecorator(new OverrideIndicatorLabelDecorator(null));

		treeViewer.setLabelProvider(fInnerLabelProvider);

		fLexicalSortingAction= new LexicalSortingAction(treeViewer);
		fSortByDefiningTypeAction= new SortByDefiningTypeAction(treeViewer);
		fShowOnlyMainTypeAction= new ShowOnlyMainTypeAction(treeViewer);
		fCategoryFilterActionGroup= new CategoryFilterActionGroup(treeViewer, getId(), getInputForCategories());

		fOutlineContentProvider= new OutlineContentProvider(false);
		treeViewer.setContentProvider(fOutlineContentProvider);
		fOutlineSorter= new OutlineSorter();
		treeViewer.setComparator(fOutlineSorter);
		treeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);


		treeViewer.getTree().addKeyListener(getKeyAdapter());

		return treeViewer;
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getStatusFieldText() {
		KeySequence[] sequences= getInvokingCommandKeySequences();
		if (sequences == null || sequences.length == 0)
			return ""; //$NON-NLS-1$

		String keySequence= sequences[0].format();

		if (fOutlineContentProvider.isShowingInheritedMembers())
			return Messages.format(JavaUIMessages.JavaOutlineControl_statusFieldText_hideInheritedMembers, keySequence);
		else
			return Messages.format(JavaUIMessages.JavaOutlineControl_statusFieldText_showInheritedMembers, keySequence);
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.AbstractInformationControl#getId()
	 * 
	 */
	protected String getId() {
		return "org.eclipse.wst.jsdt.internal.ui.text.QuickOutline"; //$NON-NLS-1$
	}

	/**
	 * {@inheritDoc}
	 */
	public void setInput(Object information) {
		if (information == null || information instanceof String) {
			inputChanged(null, null);
			return;
		}
		IJavaScriptElement je= (IJavaScriptElement)information;
		IJavaScriptUnit cu= (IJavaScriptUnit)je.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
		if (cu != null)
			fInput= cu;
		else
			fInput= je.getAncestor(IJavaScriptElement.CLASS_FILE);

		inputChanged(fInput, information);
		
		fCategoryFilterActionGroup.setInput(getInputForCategories());
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
							toggleShowInheritedMembers();
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
	protected void handleStatusFieldClicked() {
		toggleShowInheritedMembers();
	}

	protected void toggleShowInheritedMembers() {
		long flags= fInnerLabelProvider.getTextFlags();
		flags ^= JavaScriptElementLabels.ALL_POST_QUALIFIED;
		fInnerLabelProvider.setTextFlags(flags);
		fOutlineContentProvider.toggleShowInheritedMembers();
		updateStatusFieldText();
		fCategoryFilterActionGroup.setInput(getInputForCategories());
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.AbstractInformationControl#fillViewMenu(org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillViewMenu(IMenuManager viewMenu) {
		super.fillViewMenu(viewMenu);
		viewMenu.add(fShowOnlyMainTypeAction); 

		viewMenu.add(new Separator("Sorters")); //$NON-NLS-1$
		viewMenu.add(fLexicalSortingAction);

		viewMenu.add(fSortByDefiningTypeAction);
		
		fCategoryFilterActionGroup.setInput(getInputForCategories());
		fCategoryFilterActionGroup.contributeToViewMenu(viewMenu);	
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.AbstractInformationControl#setMatcherString(java.lang.String, boolean)
	 * 
	 */
	protected void setMatcherString(String pattern, boolean update) {
		fPattern= pattern;
		if (pattern.length() == 0 || !fSortByDefiningTypeAction.isChecked()) {
			super.setMatcherString(pattern, update);
			return;
		}
		
		boolean ignoreCase= pattern.toLowerCase().equals(pattern);
		String pattern2= "*" + JavaScriptElementLabels.CONCAT_STRING + pattern; //$NON-NLS-1$
		fStringMatcher= new OrStringMatcher(pattern, pattern2, ignoreCase, false);

		if (update)
			stringMatcherUpdated();
		
	}

	private IJavaScriptElement[] getInputForCategories() {
		if (fInput == null)
			return new IJavaScriptElement[0];
		
		if (fOutlineContentProvider.isShowingInheritedMembers()) {
			IJavaScriptElement p= fInput;
			if (p instanceof ITypeRoot) {
				p= ((ITypeRoot)p).findPrimaryType();
			}
			while (p != null && !(p instanceof IType)) {
				p= p.getParent();
			}
			if (!(p instanceof IType))
				return new IJavaScriptElement[] {fInput};
			
			ITypeHierarchy hierarchy= getSuperTypeHierarchy((IType)p);
			if (hierarchy == null)
				return new IJavaScriptElement[] {fInput};
			
			IType[] supertypes= hierarchy.getAllSuperclasses((IType)p);
			IJavaScriptElement[] result= new IJavaScriptElement[supertypes.length + 1];
			result[0]= fInput;
			System.arraycopy(supertypes, 0, result, 1, supertypes.length);
			return result;
		} else {
			return new IJavaScriptElement[] {fInput};
		}
	}
	
	private ITypeHierarchy getSuperTypeHierarchy(IType type) {
		ITypeHierarchy th= (ITypeHierarchy)fTypeHierarchies.get(type);
		if (th == null) {
			try {
				th= SuperTypeHierarchyCache.getTypeHierarchy(type, getProgressMonitor());
			} catch (JavaScriptModelException e) {
				return null;
			} catch (OperationCanceledException e) {
				return null;
			}
			fTypeHierarchies.put(type, th);
		}
		return th;
	}

	private IProgressMonitor getProgressMonitor() {
		IWorkbenchPage wbPage= JavaScriptPlugin.getActivePage();
		if (wbPage == null)
			return null;

		IEditorPart editor= wbPage.getActiveEditor();
		if (editor == null)
			return null;

		return editor.getEditorSite().getActionBars().getStatusLineManager().getProgressMonitor();
	}
	
}
