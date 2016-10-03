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
package org.eclipse.wst.jsdt.internal.ui.browsing;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.actions.MultiActionGroup;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectAllAction;
import org.eclipse.wst.jsdt.internal.ui.filters.NonJavaElementFilter;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaUILabelProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.LibraryFilter;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ProblemTableViewer;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ProblemTreeViewer;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.StatusBarUpdater;
import org.eclipse.wst.jsdt.ui.JavaScriptElementComparator;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;


public class PackagesView extends JavaBrowsingPart{

	private static final String TAG_VIEW_STATE= ".viewState"; //$NON-NLS-1$
	private static final int LIST_VIEW_STATE= 0;
	private static final int TREE_VIEW_STATE= 1;


	private static class StatusBarUpdater4LogicalPackage extends StatusBarUpdater {

		private StatusBarUpdater4LogicalPackage(IStatusLineManager statusLineManager) {
			super(statusLineManager);
		}

		protected String formatMessage(ISelection sel) {
			if (sel instanceof IStructuredSelection) {
				IStructuredSelection selection= (IStructuredSelection)sel;
				int nElements= selection.size();
				Object elem= selection.getFirstElement();
				if (nElements == 1 && (elem instanceof LogicalPackage))
					return formatLogicalPackageMessage((LogicalPackage) elem);
			}
			return super.formatMessage(sel);
		}

		private String formatLogicalPackageMessage(LogicalPackage logicalPackage) {
			IPackageFragment[] fragments= logicalPackage.getFragments();
			StringBuffer buf= new StringBuffer(logicalPackage.getElementName());
			buf.append(JavaScriptElementLabels.CONCAT_STRING);
			String message= ""; //$NON-NLS-1$
			boolean firstTime= true;
			for (int i= 0; i < fragments.length; i++) {
				IPackageFragment fragment= fragments[i];
				IJavaScriptElement element= fragment.getParent();
				if (element instanceof IPackageFragmentRoot) {
					IPackageFragmentRoot root= (IPackageFragmentRoot) element;
					String label= JavaScriptElementLabels.getElementLabel(root, JavaScriptElementLabels.DEFAULT_QUALIFIED | JavaScriptElementLabels.ROOT_QUALIFIED);
					if (firstTime) {
						buf.append(label);
						firstTime= false;
					}
					else
						message= Messages.format(JavaBrowsingMessages.StatusBar_concat, new String[] {message, label});
				}
			}
			buf.append(message);
			return buf.toString();
		}
	}


	private SelectAllAction fSelectAllAction;

	private int fCurrViewState;

	private PackageViewerWrapper fWrappedViewer;

	private MultiActionGroup fSwitchActionGroup;
	private boolean fLastInputWasProject;

	/**
	 * Adds filters the viewer of this part.
	 */
	protected void addFilters() {
		super.addFilters();
		getViewer().addFilter(createNonJavaElementFilter());
		getViewer().addFilter(new LibraryFilter());
	}


	/**
	 * Creates new NonJavaElementFilter and overrides method select to allow for
	 * LogicalPackages.
	 * @return NonJavaElementFilter
	 */
	protected NonJavaElementFilter createNonJavaElementFilter() {
		return new NonJavaElementFilter(){
			public boolean select(Viewer viewer, Object parent, Object element){
				return ((element instanceof IJavaScriptElement) || (element instanceof LogicalPackage) || (element instanceof IFolder));
			}
		};
	}

	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		//this must be created before all actions and filters
		fWrappedViewer= new PackageViewerWrapper();
		restoreLayoutState(memento);
	}

	private void restoreLayoutState(IMemento memento) {
		if (memento == null) {
			//read state from the preference store
			IPreferenceStore store= JavaScriptPlugin.getDefault().getPreferenceStore();
			fCurrViewState= store.getInt(this.getViewSite().getId() + TAG_VIEW_STATE);
		} else {
			//restore from memento
			Integer integer= memento.getInteger(this.getViewSite().getId() + TAG_VIEW_STATE);
			if ((integer == null) || !isValidState(integer.intValue())) {
				fCurrViewState= LIST_VIEW_STATE;
			} else fCurrViewState= integer.intValue();
		}
	}

	private boolean isValidState(int state) {
		return (state==LIST_VIEW_STATE) || (state==TREE_VIEW_STATE);
	}



	/*
	 * @see org.eclipse.ui.IViewPart#saveState(org.eclipse.ui.IMemento)
	 */
	public void saveState(IMemento memento) {
		super.saveState(memento);
		memento.putInteger(this.getViewSite().getId()+TAG_VIEW_STATE,fCurrViewState);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.browsing.JavaBrowsingPart#createViewer(org.eclipse.swt.widgets.Composite)
	 */
	protected StructuredViewer createViewer(Composite parent) {
		//Creates the viewer of this part dependent on the current layout.
		StructuredViewer viewer;
		if(isInListState())
			viewer= createTableViewer(parent);
		else
			viewer= createTreeViewer(parent);

		fWrappedViewer.setViewer(viewer);
		return fWrappedViewer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.browsing.JavaBrowsingPart#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class key) {
		if (key == IShowInTargetList.class) {
			return new IShowInTargetList() {
				public String[] getShowInTargetIds() {
					return new String[] { JavaScriptUI.ID_PACKAGES, IPageLayout.ID_RES_NAV  };
				}
			};
		}
		return super.getAdapter(key);
	}

	protected boolean isInListState() {
		return fCurrViewState== LIST_VIEW_STATE;
	}

	private ProblemTableViewer createTableViewer(Composite parent) {
		return new PackagesViewTableViewer(parent, SWT.MULTI);
	}

	private ProblemTreeViewer createTreeViewer(Composite parent) {
		return new PackagesViewTreeViewer(parent, SWT.MULTI);
	}

	/**
	 * Overrides the createContentProvider from JavaBrowsingPart
	 * Creates the content provider of this part.
	 */
	protected IContentProvider createContentProvider() {
		if(isInListState())
			return new PackagesViewFlatContentProvider(fWrappedViewer.getViewer());
		else return new PackagesViewHierarchicalContentProvider(fWrappedViewer.getViewer());
	}

	protected JavaUILabelProvider createLabelProvider() {
		if(isInListState())
			return createListLabelProvider();
		else return createTreeLabelProvider();
	}

	private JavaUILabelProvider createTreeLabelProvider() {
		return new PackagesViewLabelProvider(PackagesViewLabelProvider.HIERARCHICAL_VIEW_STATE);
	}

	private JavaUILabelProvider createListLabelProvider() {
		return new PackagesViewLabelProvider(PackagesViewLabelProvider.FLAT_VIEW_STATE);
	}

	/**
	 * Returns the context ID for the Help system
	 *
	 * @return	the string used as ID for the Help context
	 */
	protected String getHelpContextId() {
		return IJavaHelpContextIds.PACKAGES_BROWSING_VIEW;
	}

	protected String getLinkToEditorKey() {
		return PreferenceConstants.LINK_BROWSING_PACKAGES_TO_EDITOR;
	}

	/**
	 * Answers if the given <code>element</code> is a valid
	 * input for this part.
	 *
	 * @param 	element	the object to test
	 * @return	<true> if the given element is a valid input
	 */
	protected boolean isValidInput(Object element) {
		if (element instanceof IJavaScriptProject || (element instanceof IPackageFragmentRoot && ((IJavaScriptElement)element).getElementName() != IPackageFragmentRoot.DEFAULT_PACKAGEROOT_PATH))
			try {
				IJavaScriptProject jProject= ((IJavaScriptElement)element).getJavaScriptProject();
				if (jProject != null)
					return jProject.getProject().hasNature(JavaScriptCore.NATURE_ID);
			} catch (CoreException ex) {
				return false;
			}
		return false;
	}

	/**
	 * Answers if the given <code>element</code> is a valid
	 * element for this part.
	 *
	 * @param 	element	the object to test
	 * @return	<true> if the given element is a valid element
	 */
	protected boolean isValidElement(Object element) {
		if (element instanceof IPackageFragment) {
			IJavaScriptElement parent= ((IPackageFragment)element).getParent();
			if (parent != null)
				return super.isValidElement(parent) || super.isValidElement(parent.getJavaScriptProject());
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.ui.browsing.JavaBrowsingPart#findElementToSelect(org.eclipse.wst.jsdt.core.IJavaScriptElement)
	 */
	protected IJavaScriptElement findElementToSelect(IJavaScriptElement je) {
		if (je == null)
			return null;

		switch (je.getElementType()) {
			case IJavaScriptElement.PACKAGE_FRAGMENT:
				return je;
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				return ((IJavaScriptUnit)je).getParent();
			case IJavaScriptElement.CLASS_FILE:
				return ((IClassFile)je).getParent();
			case IJavaScriptElement.TYPE:
				return ((IType)je).getPackageFragment();
			default:
				return findElementToSelect(je.getParent());
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.browsing.JavaBrowsingPart#setInput(java.lang.Object)
	 */
	protected void setInput(Object input) {
		setViewerWrapperInput(input);
		super.updateTitle();
	}

	private void setViewerWrapperInput(Object input) {
		fWrappedViewer.setViewerInput(input);
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.ui.browsing.JavaBrowsingPart#fillActionBars(org.eclipse.ui.IActionBars)
	 */
	protected void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		fSwitchActionGroup.fillActionBars(actionBars);
	}



	private void setUpViewer(StructuredViewer viewer){
		Assert.isTrue(viewer != null);

		JavaUILabelProvider labelProvider= createLabelProvider();
		viewer.setLabelProvider(createDecoratingLabelProvider(labelProvider));

		viewer.setComparator(createJavaElementComparator());
		viewer.setUseHashlookup(true);

		createContextMenu();

		//disapears when control disposed
		addKeyListener();

		//this methods only adds listeners to the viewer,
		//these listenters disapear when the viewer is disposed
		hookViewerListeners();

		// Set content provider
		viewer.setContentProvider(createContentProvider());
		//Disposed when viewer's Control is disposed
		initDragAndDrop();

	}

	//alter sorter to include LogicalPackages
	protected JavaScriptElementComparator createJavaElementComparator() {
		return new JavaScriptElementComparator(){
			public int category(Object element) {
				if (element instanceof LogicalPackage) {
					LogicalPackage cp= (LogicalPackage) element;
					return super.category(cp.getFragments()[0]);
				} else return super.category(element);
			}
			public int compare(Viewer viewer, Object e1, Object e2){
				if (e1 instanceof LogicalPackage) {
					LogicalPackage cp= (LogicalPackage) e1;
					e1= cp.getFragments()[0];
				}
				if (e2 instanceof LogicalPackage) {
					LogicalPackage cp= (LogicalPackage) e2;
					e2= cp.getFragments()[0];
				}
				return super.compare(viewer, e1, e2);
			}
		};
	}

	protected StatusBarUpdater createStatusBarUpdater(IStatusLineManager slManager) {
		return new StatusBarUpdater4LogicalPackage(slManager);
	}

	protected void setSiteSelectionProvider(){
		getSite().setSelectionProvider(fWrappedViewer);
	}

	void adjustInputAndSetSelection(Object o) {
		if (!(o instanceof LogicalPackage)) {
			super.adjustInputAndSetSelection(o);
			return;
		}

		LogicalPackage lp= (LogicalPackage)o;
		if (!lp.getJavaProject().equals(getInput()))
			setInput(lp.getJavaProject());

		setSelection(new StructuredSelection(lp), true);
	}

	//do the same thing as the JavaBrowsingPart but with wrapper
	protected void createActions() {
		super.createActions();

		createSelectAllAction();

		//create the switch action group
		fSwitchActionGroup= createSwitchActionGroup();
	}

	private MultiActionGroup createSwitchActionGroup(){

		LayoutAction switchToFlatViewAction= new LayoutAction(JavaBrowsingMessages.PackagesView_flatLayoutAction_label,LIST_VIEW_STATE);
		LayoutAction switchToHierarchicalViewAction= new LayoutAction(JavaBrowsingMessages.PackagesView_HierarchicalLayoutAction_label, TREE_VIEW_STATE);
		JavaPluginImages.setLocalImageDescriptors(switchToFlatViewAction, "flatLayout.gif"); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(switchToHierarchicalViewAction, "hierarchicalLayout.gif"); //$NON-NLS-1$

		return new LayoutActionGroup(new IAction[]{switchToFlatViewAction,switchToHierarchicalViewAction}, fCurrViewState);
	}

	private static class LayoutActionGroup extends MultiActionGroup {

		LayoutActionGroup(IAction[] actions, int index) {
			super(actions, index);
		}

		public void fillActionBars(IActionBars actionBars) {
			//create new layout group
			IMenuManager manager= actionBars.getMenuManager();
			final IContributionItem groupMarker= new GroupMarker("layout"); //$NON-NLS-1$
			manager.add(groupMarker);
			IMenuManager newManager= new MenuManager(JavaBrowsingMessages.PackagesView_LayoutActionGroup_layout_label);
			manager.appendToGroup("layout", newManager); //$NON-NLS-1$
			super.addActions(newManager);
		}
	}


	/**
	 * Switches between flat and hierarchical state.
	 */
	private class LayoutAction extends Action {

		private int fState;

		public LayoutAction(String text, int state) {
			super(text, IAction.AS_RADIO_BUTTON);
			fState= state;
			if (state == PackagesView.LIST_VIEW_STATE)
				PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.LAYOUT_FLAT_ACTION);
			else
				PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.LAYOUT_HIERARCHICAL_ACTION);
		}

		public int getState() {
			return fState;
		}

		public void setRunnable(Runnable runnable) {
			Assert.isNotNull(runnable);
		}

		/*
		 * @see org.eclipse.jface.action.IAction#run()
		 */
		public void run() {
			switchViewer(fState);
		}
	}

	private void switchViewer(int state) {
		//Indicate which viewer is to be used
		if (fCurrViewState == state)
			return;
		else {
			fCurrViewState= state;
			IPreferenceStore store= JavaScriptPlugin.getDefault().getPreferenceStore();
			store.setValue(getViewSite().getId() + TAG_VIEW_STATE, state);
		}

		//get the information from the existing viewer
		StructuredViewer viewer= fWrappedViewer.getViewer();
		Object object= viewer.getInput();
		ISelection selection= viewer.getSelection();

		// create and set up the new viewer
		Control control= createViewer(fWrappedViewer.getControl().getParent()).getControl();

		setUpViewer(fWrappedViewer);

		createSelectAllAction();

		// add the selection information from old viewer
		fWrappedViewer.setViewerInput(object);
		fWrappedViewer.getControl().setFocus();
		fWrappedViewer.setSelection(selection, true);

		// dispose old viewer
		viewer.getContentProvider().dispose();
		viewer.getControl().dispose();

		// layout the new viewer
		if (control != null && !control.isDisposed()) {
			control.setVisible(true);
			control.getParent().layout(true);
		}
	}

	private void createSelectAllAction() {
		IActionBars actionBars= getViewSite().getActionBars();
		if (isInListState()) {
			fSelectAllAction= new SelectAllAction((TableViewer)fWrappedViewer.getViewer());
			actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), fSelectAllAction);
		} else {
			actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), null);
			fSelectAllAction= null;
		}
		actionBars.updateActionBars();
	}

	protected IJavaScriptElement findInputForJavaElement(IJavaScriptElement je) {
		// null check has to take place here as well (not only in
		// findInputForJavaElement(IJavaScriptElement, boolean) since we
		// are accessing the Java element
		if (je == null)
			return null;
		if(je.getElementType() == IJavaScriptElement.PACKAGE_FRAGMENT_ROOT || je.getElementType() == IJavaScriptElement.JAVASCRIPT_PROJECT)
			return findInputForJavaElement(je, true);
		else
			return findInputForJavaElement(je, false);

	}

	protected IJavaScriptElement findInputForJavaElement(IJavaScriptElement je, boolean canChangeInputType) {
		if (je == null || !je.exists())
			return null;

		if (isValidInput(je)) {

			//don't update if input must be project (i.e. project is used as source folder)
			if (canChangeInputType)
				fLastInputWasProject= je.getElementType() == IJavaScriptElement.JAVASCRIPT_PROJECT;
			return je;
		} else if (fLastInputWasProject) {
			IPackageFragmentRoot packageFragmentRoot= (IPackageFragmentRoot)je.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
			if (!packageFragmentRoot.isExternal())
				return je.getJavaScriptProject();
		}

		return findInputForJavaElement(je.getParent(), canChangeInputType);
	}

	/**
	 * Override the getText and getImage methods for the DecoratingLabelProvider
	 * to handel the decoration of logical packages.
	 *
	 * @see org.eclipse.wst.jsdt.internal.ui.browsing.JavaBrowsingPart#createDecoratingLabelProvider(JavaUILabelProvider)
	 */
	protected DecoratingJavaLabelProvider createDecoratingLabelProvider(JavaUILabelProvider provider) {
		return new DecoratingJavaLabelProvider(provider, false, isInListState()) {

			public String getText(Object element){
				if (element instanceof LogicalPackage) {
					LogicalPackage el= (LogicalPackage) element;
					return super.getText(el.getFragments()[0]);
				} else return super.getText(element);
			}

			public Image getImage(Object element) {
				if(element instanceof LogicalPackage){
					LogicalPackage el= (LogicalPackage) element;
					ILabelDecorator decorator= getLabelDecorator();
					IPackageFragment[] fragments= el.getFragments();

					Image image= super.getImage(el);
					for (int i= 0; i < fragments.length; i++) {
						IPackageFragment fragment= fragments[i];
						Image decoratedImage= decorator.decorateImage(image, fragment);
						if(decoratedImage != null)
							image= decoratedImage;
					}
					return image;
				} else return super.getImage(element);
			}

		};
	}

	/*
	 * Overridden from JavaBrowsingPart to handel LogicalPackages and tree
	 * structure.
	 * @see org.eclipse.wst.jsdt.internal.ui.browsing.JavaBrowsingPart#adjustInputAndSetSelection(org.eclipse.wst.jsdt.core.IJavaScriptElement)
	 */
	void adjustInputAndSetSelection(IJavaScriptElement je) {

		IJavaScriptElement jElementToSelect= findElementToSelect(je);
		LogicalPackagesProvider p= (LogicalPackagesProvider) fWrappedViewer.getContentProvider();

		Object elementToSelect= jElementToSelect;
		if (jElementToSelect != null && jElementToSelect.getElementType() == IJavaScriptElement.PACKAGE_FRAGMENT) {
			IPackageFragment pkgFragment= (IPackageFragment)jElementToSelect;
			elementToSelect= p.findLogicalPackage(pkgFragment);
			if (elementToSelect == null)
				elementToSelect= pkgFragment;
		}

		IJavaScriptElement newInput= findInputForJavaElement(je);
		if (elementToSelect == null && !isValidInput(newInput))
			setInput(null);
		else if (elementToSelect == null || getViewer().testFindItem(elementToSelect) == null) {

			//optimization, if you are in the same project but expansion hasn't happened
			Object input= getViewer().getInput();
			if (elementToSelect != null && newInput != null) {
				if (newInput.equals(input)) {
					getViewer().reveal(elementToSelect);
				// Adjust input to selection
				} else {
					setInput(newInput);
					getViewer().reveal(elementToSelect);
				}
			} else
				setInput(newInput);

			if (elementToSelect instanceof IPackageFragment) {
				IPackageFragment pkgFragment= (IPackageFragment)elementToSelect;
				elementToSelect= p.findLogicalPackage(pkgFragment);
				if (elementToSelect == null)
					elementToSelect= pkgFragment;
			}
		}

		ISelection selection;
		if (elementToSelect != null)
			selection= new StructuredSelection(elementToSelect);
		else
			selection= StructuredSelection.EMPTY;
		setSelection(selection, true);
	}

}
