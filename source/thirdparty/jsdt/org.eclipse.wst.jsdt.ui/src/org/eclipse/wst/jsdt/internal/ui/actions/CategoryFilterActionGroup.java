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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptModel;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.ListDialogField;

import com.ibm.icu.text.Collator;

public class CategoryFilterActionGroup extends ActionGroup {

	private class CategoryFilter extends ViewerFilter {

		/**
		 * {@inheritDoc}
		 */
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof IMember) {
				IMember member= (IMember)element;
				try {
					String[] categories= member.getCategories();
					if (categories.length == 0)
						return !fFilterUncategorizedMembers;
					
					for (int i= 0; i < categories.length; i++) {
						if (!fFilteredCategories.contains(categories[i]))
							return true;
					}
					return false;
				} catch (JavaScriptModelException e) {
					JavaScriptPlugin.log(e);
				}
			}
			return true;
		}
		
	}
	
	private class CategoryFilterSelectionDialog extends SelectionStatusDialog implements IListAdapter {
		
		private static final int SELECT_ALL= 0;
		private static final int DESELECT_ALL= 1;

		private final CheckedListDialogField fCategoryList;

		public CategoryFilterSelectionDialog(Shell parent, List categories, List selectedCategories) {
			super(parent);
			
			setTitle(ActionMessages.CategoryFilterActionGroup_JavaCategoryFilter_title);
			
			String[] buttons= {
					ActionMessages.CategoryFilterActionGroup_SelectAllCategories, 
					ActionMessages.CategoryFilterActionGroup_DeselectAllCategories
					};
			
			fCategoryList= new CheckedListDialogField(this, buttons, new ILabelProvider() {
							public Image getImage(Object element) {return null;}
							public String getText(Object element) {return (String)element;}
							public void addListener(ILabelProviderListener listener) {}
							public void dispose() {}
							public boolean isLabelProperty(Object element, String property) {return false;}
							public void removeListener(ILabelProviderListener listener) {}
						});
			fCategoryList.addElements(categories);
			fCategoryList.setViewerComparator(new ViewerComparator());
			fCategoryList.setLabelText(ActionMessages.CategoryFilterActionGroup_SelectCategoriesDescription);
			fCategoryList.checkAll(true);
			for (Iterator iter= selectedCategories.iterator(); iter.hasNext();) {
				String selected= (String)iter.next();
				fCategoryList.setChecked(selected, false);
			}
			if (categories.size() == 0) {
				fCategoryList.setEnabled(false);
			}
		}
		
		/**
		 * {@inheritDoc}
		 */
		protected Control createDialogArea(Composite parent) {
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			composite.setLayout(new GridLayout(1, true));
			composite.setFont(parent.getFont());
			
			Composite list= new Composite(composite, SWT.NONE);
			list.setFont(composite.getFont());
			LayoutUtil.doDefaultLayout(list, new DialogField[] { fCategoryList }, true);
			LayoutUtil.setHorizontalGrabbing(fCategoryList.getListControl(null));
			Dialog.applyDialogFont(composite);
			
			setHelpAvailable(false);
			
			return composite;
		}

		/**
		 * {@inheritDoc}
		 */
		protected void computeResult() {
			setResult(fCategoryList.getCheckedElements());
		}

		/**
		 * {@inheritDoc}
		 */
		public void customButtonPressed(ListDialogField field, int index) {
			if (index == SELECT_ALL) {
				fCategoryList.checkAll(true);
				fCategoryList.refresh();
			} else if (index == DESELECT_ALL) {
				fCategoryList.checkAll(false);
				fCategoryList.refresh();
			}
		}

		public void doubleClicked(ListDialogField field) {
			List selectedElements= field.getSelectedElements();
			if (selectedElements.size() == 1) {
				Object selected= selectedElements.get(0);
				fCategoryList.setChecked(selected, !fCategoryList.isChecked(selected));
			}
		}
		public void selectionChanged(ListDialogField field) {}
	}
	
	private class CategoryFilterMenuAction extends Action {
		
		public CategoryFilterMenuAction() {
			setDescription(ActionMessages.CategoryFilterActionGroup_ShowCategoriesActionDescription); 
			setToolTipText(ActionMessages.CategoryFilterActionGroup_ShowCategoriesToolTip); 
			setText(ActionMessages.CategoryFilterActionGroup_ShowCategoriesLabel);
			JavaPluginImages.setLocalImageDescriptors(this, "category_menu.gif"); //$NON-NLS-1$
		}
		
		/**
		 * {@inheritDoc}
		 */
		public void run() {
			showCategorySelectionDialog(fInputElement);
		}

	}
		
	private class CategoryFilterAction extends Action {
		
		private final String fCategory;

		public CategoryFilterAction(String category, int count) {
			fCategory= category;
			StringBuffer buf = new StringBuffer();
			buf.append('&').append(count).append(' ').append(fCategory);
			setText(buf.toString());
			setChecked(!fFilteredCategories.contains(fCategory));
			setId(FILTER_CATEGORY_ACTION_ID);
		}

		/**
		 * {@inheritDoc}
		 */
		public void run() {
			super.run();
			if (fFilteredCategories.contains(fCategory)) {
				fFilteredCategories.remove(fCategory);
			} else {
				fFilteredCategories.add(fCategory);
			}
			fLRUList.put(fCategory, fCategory);
			storeSettings();
			fireSelectionChange();
		}

	}
	
	private class FilterUncategorizedMembersAction extends Action {

		public FilterUncategorizedMembersAction() {
			setText(ActionMessages.CategoryFilterActionGroup_ShowUncategorizedMembers);
			setChecked(!fFilterUncategorizedMembers);
			setId(FILTER_CATEGORY_ACTION_ID);
		}
		
		/**
		 * {@inheritDoc}
		 */
		public void run() {
			fFilterUncategorizedMembers= !fFilterUncategorizedMembers;
			storeSettings();
			fireSelectionChange();
		}
	}
	
	private interface IResultCollector {
		public boolean accept(String[] category);
	}
	
	private static int COUNTER= 0;//WORKAROUND for Bug 132669 https://bugs.eclipse.org/bugs/show_bug.cgi?id=132669
	
	private static final String FILTER_CATEGORY_ACTION_ID= "FilterCategoryActionId"; //$NON-NLS-1$
	private final String CATEGORY_MENU_GROUP_NAME= "CategoryMenuGroup" + (COUNTER++); //$NON-NLS-1$
	private static final int MAX_NUMBER_OF_CATEGORIES_IN_MENU= 5;

	private final StructuredViewer fViewer;
	private final String fViewerId;
	private final CategoryFilter fFilter;
	private final HashSet fFilteredCategories;
	private IJavaScriptElement[] fInputElement;
	private final CategoryFilterMenuAction fMenuAction;
	private IMenuManager fMenuManager;
	private IMenuListener fMenuListener;
	private final LinkedHashMap fLRUList;
	private boolean fFilterUncategorizedMembers;

	public CategoryFilterActionGroup(final StructuredViewer viewer, final String viewerId, IJavaScriptElement[] input) {
		Assert.isLegal(viewer != null);
		Assert.isLegal(viewerId != null);
		Assert.isLegal(input != null);
		
		fLRUList= new LinkedHashMap(MAX_NUMBER_OF_CATEGORIES_IN_MENU * 2, 0.75f, true) {
			private static final long serialVersionUID= 1L;
			protected boolean removeEldestEntry(Map.Entry eldest) {
				return size() > MAX_NUMBER_OF_CATEGORIES_IN_MENU;
			}
		};
		fViewer= viewer;
		fViewerId= viewerId;
		fInputElement= input;
		
		fFilter= new CategoryFilter();
		
		fFilteredCategories= new HashSet();
		loadSettings();

		fMenuAction= new CategoryFilterMenuAction();
		
		fViewer.addFilter(fFilter);
	}
	
	public void setInput(IJavaScriptElement[] input) {
		Assert.isLegal(input != null);
		fInputElement= input;
	}
	
	private void loadSettings() {
		fFilteredCategories.clear();
		IPreferenceStore store= JavaScriptPlugin.getDefault().getPreferenceStore();
		String string= store.getString(getPreferenceKey());
		if (string != null && string.length() > 0) {
			String[] categories= string.split(";"); //$NON-NLS-1$
			for (int i= 0; i < categories.length; i++) {
				fFilteredCategories.add(categories[i]);
			}
		}
		string= store.getString(getPreferenceKey()+".LRU"); //$NON-NLS-1$
		if (string != null && string.length() > 0) {
			String[] categories= string.split(";"); //$NON-NLS-1$
			for (int i= categories.length - 1; i >= 0; i--) {
				fLRUList.put(categories[i], categories[i]);
			}
		}
		fFilterUncategorizedMembers= store.getBoolean(getPreferenceKey()+".FilterUncategorized"); //$NON-NLS-1$
	}

	private void storeSettings() {
		IPreferenceStore store= JavaScriptPlugin.getDefault().getPreferenceStore();
		if (fFilteredCategories.size() == 0) {
			store.setValue(getPreferenceKey(), ""); //$NON-NLS-1$
		} else {
			StringBuffer buf= new StringBuffer();
			Iterator iter= fFilteredCategories.iterator();
			String element= (String)iter.next();
			buf.append(element);
			while (iter.hasNext()) {
				element= (String)iter.next();
				buf.append(';');
				buf.append(element);
			}
			store.setValue(getPreferenceKey(), buf.toString());
			buf= new StringBuffer();
			iter= fLRUList.values().iterator();
			element= (String)iter.next();
			buf.append(element);
			while (iter.hasNext()) {
				element= (String)iter.next();
				buf.append(';');
				buf.append(element);
			}
			store.setValue(getPreferenceKey()+".LRU", buf.toString()); //$NON-NLS-1$
			store.setValue(getPreferenceKey()+".FilterUncategorized", fFilterUncategorizedMembers); //$NON-NLS-1$
		}
	}
	
	public void contributeToViewMenu(IMenuManager menuManager) {
		menuManager.add(new Separator(CATEGORY_MENU_GROUP_NAME));
		menuManager.appendToGroup(CATEGORY_MENU_GROUP_NAME, fMenuAction);
		fMenuListener= new IMenuListener() {
					public void menuAboutToShow(IMenuManager manager) {
						if (!manager.isVisible())
							return;
						updateMenu(manager);
					}			
				};
		menuManager.addMenuListener(fMenuListener);
		fMenuManager= menuManager;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		super.dispose();
		if (fMenuManager != null) {
			fMenuManager.removeMenuListener(fMenuListener);
			fMenuManager= null;
			fMenuListener= null;
		}
	}

	private void updateMenu(IMenuManager manager) {
		IContributionItem[] items= manager.getItems();
		if (items != null) {
			for (int i= 0; i < items.length; i++) {
				IContributionItem item= items[i];
				if (item != null && item.getId() != null && item.getId().equals(FILTER_CATEGORY_ACTION_ID)) {
					IContributionItem removed= manager.remove(item);
					if (removed != null) {
						item.dispose();
					}
				}
			}
		}
		List menuEntries= new ArrayList();
		boolean hasUncategorizedMembers= getMenuCategories(menuEntries);
		Collections.sort(menuEntries, Collator.getInstance());
		
		if (menuEntries.size() > 0 && hasUncategorizedMembers)
			manager.appendToGroup(CATEGORY_MENU_GROUP_NAME, new FilterUncategorizedMembersAction());
		
		int count= 0;
		for (Iterator iter= menuEntries.iterator(); iter.hasNext();) {
			String category= (String)iter.next();
			manager.appendToGroup(CATEGORY_MENU_GROUP_NAME, new CategoryFilterAction(category, count + 1));
			count++;
		}
	}

	private boolean getMenuCategories(List result) {
		final HashSet/*<String>*/ categories= new HashSet();
		final HashSet/*<String>*/ foundLRUCategories= new HashSet();
		final boolean hasUncategorizedMember[]= new boolean[] {false};
		for (int i= 0; i < fInputElement.length && !(hasUncategorizedMember[0] && foundLRUCategories.size() >= MAX_NUMBER_OF_CATEGORIES_IN_MENU); i++) {
			collectCategories(fInputElement[i], new IResultCollector() {
				public boolean accept(String[] cats) {
					if (cats.length > 0) {
						for (int j= 0; j < cats.length; j++) {
							String category= cats[j];
							categories.add(category);
							if (fLRUList.containsKey(category)) {
								foundLRUCategories.add(category);
							}	
						}
					} else {
						hasUncategorizedMember[0]= true;
					}
					return hasUncategorizedMember[0] && foundLRUCategories.size() >= MAX_NUMBER_OF_CATEGORIES_IN_MENU;
				}
			});
		}
		int count= 0;
		for (Iterator iter= foundLRUCategories.iterator(); iter.hasNext();) {
			String element= (String)iter.next();
			result.add(element);
			count++;
		}
		if (count < MAX_NUMBER_OF_CATEGORIES_IN_MENU) {
			List sortedCategories= new ArrayList(categories);
			Collections.sort(sortedCategories, Collator.getInstance());
			for (Iterator iter= sortedCategories.iterator(); iter.hasNext() && count < MAX_NUMBER_OF_CATEGORIES_IN_MENU;) {
				String element= (String)iter.next();
				if (!foundLRUCategories.contains(element)) {
					result.add(element);
					count++;
				}
			}
		}
		return hasUncategorizedMember[0];
	}

	private boolean collectCategories(IJavaScriptElement element, IResultCollector collector) {//HashSet result, int max, LinkedHashMap lruList) {
		try {
			if (element instanceof IMember) {
				IMember member= (IMember)element;
				collector.accept(member.getCategories());
				return processChildren(member.getChildren(), collector);
			} else if (element instanceof IJavaScriptUnit) {
				return processChildren(((IJavaScriptUnit)element).getChildren(), collector);
			} else if (element instanceof IClassFile) {
				return processChildren(((IClassFile)element).getChildren(), collector);
			} else if (element instanceof IJavaScriptModel) {
				return processChildren(((IJavaScriptModel)element).getChildren(), collector);
			} else if (element instanceof IJavaScriptProject) {
				return processChildren(((IJavaScriptProject)element).getChildren(), collector);
			} else if (element instanceof IPackageFragment) {
				return processChildren(((IPackageFragment)element).getChildren(), collector);
			} else if (element instanceof IPackageFragmentRoot)	 {
				return processChildren(((IPackageFragmentRoot)element).getChildren(), collector);
			}
			return false;
		} catch (JavaScriptModelException e) {
			JavaScriptPlugin.log(e);
			return true;
		}
	}

	private boolean processChildren(IJavaScriptElement[] children, IResultCollector collector) {
		for (int i= 0; i < children.length; i++) {
			if (collectCategories(children[i], collector))
				return true;
		}
		return false;
	}

	private void fireSelectionChange() {
		fViewer.getControl().setRedraw(false);
		BusyIndicator.showWhile(fViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				fViewer.refresh();
			}
		});
		fViewer.getControl().setRedraw(true);
	}
	
	private String getPreferenceKey() {
		return "CategoryFilterActionGroup." + fViewerId; //$NON-NLS-1$
	}
	
	private void showCategorySelectionDialog(IJavaScriptElement[] input) {
		final HashSet/*<String>*/ categories= new HashSet();
		for (int i= 0; i < input.length; i++) {
			collectCategories(input[i], new IResultCollector() {
				public boolean accept(String[] cats) {
					for (int j= 0; j < cats.length; j++) {
						categories.add(cats[j]);
					}
					return false;
				}
			});
		}
		CategoryFilterSelectionDialog dialog= new CategoryFilterSelectionDialog(fViewer.getControl().getShell(), new ArrayList(categories), new ArrayList(fFilteredCategories));
		if (dialog.open() == Window.OK) {
			Object[] selected= dialog.getResult();
			for (Iterator iter= categories.iterator(); iter.hasNext();) {
				String category= (String)iter.next();
				if (contains(selected, category)) {
					if (fFilteredCategories.remove(category))
						fLRUList.put(category, category);
				} else {
					if (fFilteredCategories.add(category))
						fLRUList.put(category, category);
				}
			}
			storeSettings();
			fireSelectionChange();
		}
	}
	
	private boolean contains(Object[] selected, String category) {
		for (int i= 0; i < selected.length; i++) {
			if (selected[i].equals(category))
				return true;
		}
		return false;
	}

}
