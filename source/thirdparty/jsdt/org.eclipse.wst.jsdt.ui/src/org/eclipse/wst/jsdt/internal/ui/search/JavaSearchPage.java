/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.formatter.IndentManipulation;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchPattern;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.ui.search.ElementQuerySpecification;
import org.eclipse.wst.jsdt.ui.search.PatternQuerySpecification;
import org.eclipse.wst.jsdt.ui.search.QuerySpecification;

public class JavaSearchPage extends DialogPage implements ISearchPage {
	
	private static class SearchPatternData {
		private int searchFor;
		private int limitTo;
		private String pattern;
		private boolean isCaseSensitive;
		private IJavaScriptElement javaElement;
		private int includeMask;
		private int scope;
		private IWorkingSet[] workingSets;
		
		public SearchPatternData(int searchFor, int limitTo, boolean isCaseSensitive, String pattern, IJavaScriptElement element, int includeMask) {
			this(searchFor, limitTo, pattern, isCaseSensitive, element, ISearchPageContainer.WORKSPACE_SCOPE, null, includeMask);
		}
		
		public SearchPatternData(int searchFor, int limitTo, String pattern, boolean isCaseSensitive, IJavaScriptElement element, int scope, IWorkingSet[] workingSets, int includeMask) {
			this.searchFor= searchFor;
			this.limitTo= limitTo;
			this.pattern= pattern;
			this.isCaseSensitive= isCaseSensitive;
			this.scope= scope;
			this.workingSets= workingSets;
			this.includeMask= includeMask;
			
			setJavaElement(element);
		}
		
		public void setJavaElement(IJavaScriptElement javaElement) {
			this.javaElement= javaElement;
		}

		public boolean isCaseSensitive() {
			return isCaseSensitive;
		}

		public IJavaScriptElement getJavaElement() {
			return javaElement;
		}

		public int getLimitTo() {
			return limitTo;
		}

		public String getPattern() {
			return pattern;
		}

		public int getScope() {
			return scope;
		}

		public int getSearchFor() {
			return searchFor;
		}

		public IWorkingSet[] getWorkingSets() {
			return workingSets;
		}
		
		public int getIncludeMask() {
			return includeMask;
		}
		
		public void store(IDialogSettings settings) {
			settings.put("searchFor", searchFor); //$NON-NLS-1$
			settings.put("scope", scope); //$NON-NLS-1$
			settings.put("pattern", pattern); //$NON-NLS-1$
			settings.put("limitTo", limitTo); //$NON-NLS-1$
			settings.put("javaElement", javaElement != null ? javaElement.getHandleIdentifier() : ""); //$NON-NLS-1$ //$NON-NLS-2$
			settings.put("isCaseSensitive", isCaseSensitive); //$NON-NLS-1$
			if (workingSets != null) {
				String[] wsIds= new String[workingSets.length];
				for (int i= 0; i < workingSets.length; i++) {
					wsIds[i]= workingSets[i].getName();
				}
				settings.put("workingSets", wsIds); //$NON-NLS-1$
			} else {
				settings.put("workingSets", new String[0]); //$NON-NLS-1$
			}
			settings.put("includeMask", includeMask); //$NON-NLS-1$
		}
		
		public static SearchPatternData create(IDialogSettings settings) {
			String pattern= settings.get("pattern"); //$NON-NLS-1$
			if (pattern.length() == 0) {
				return null;
			}
			IJavaScriptElement elem= null;
			String handleId= settings.get("javaElement"); //$NON-NLS-1$
			if (handleId != null && handleId.length() > 0) {
				IJavaScriptElement restored= JavaScriptCore.create(handleId); 
				if (restored != null && isSearchableType(restored) && restored.exists()) {
					elem= restored;
				}
			}
			String[] wsIds= settings.getArray("workingSets"); //$NON-NLS-1$
			IWorkingSet[] workingSets= null;
			if (wsIds != null && wsIds.length > 0) {
				IWorkingSetManager workingSetManager= PlatformUI.getWorkbench().getWorkingSetManager();
				workingSets= new IWorkingSet[wsIds.length];
				for (int i= 0; workingSets != null && i < wsIds.length; i++) {
					workingSets[i]= workingSetManager.getWorkingSet(wsIds[i]);
					if (workingSets[i] == null) {
						workingSets= null;
					}
				}
			}

			try {
				int searchFor= settings.getInt("searchFor"); //$NON-NLS-1$
				int scope= settings.getInt("scope"); //$NON-NLS-1$
				int limitTo= settings.getInt("limitTo"); //$NON-NLS-1$
				boolean isCaseSensitive= settings.getBoolean("isCaseSensitive"); //$NON-NLS-1$
				
				int includeMask;
				if (settings.get("includeMask") != null) { //$NON-NLS-1$
					includeMask= settings.getInt("includeMask"); //$NON-NLS-1$
				} else {
					includeMask= JavaSearchScopeFactory.NO_JRE;
					if (settings.get("includeJRE") == null ? forceIncludeAll(limitTo, elem) : settings.getBoolean("includeJRE")) {  //$NON-NLS-1$ //$NON-NLS-2$
						includeMask= JavaSearchScopeFactory.ALL;
					}
				}
				return new SearchPatternData(searchFor, limitTo, pattern, isCaseSensitive, elem, scope, workingSets, includeMask);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		
	}
	
	// search for
	private final static int TYPE= IJavaScriptSearchConstants.TYPE;
	private final static int METHOD= IJavaScriptSearchConstants.METHOD;
//	private final static int PACKAGE= IJavaScriptSearchConstants.PACKAGE;
	private final static int CONSTRUCTOR= IJavaScriptSearchConstants.CONSTRUCTOR;
	private final static int FIELD= IJavaScriptSearchConstants.FIELD;
	private final static int VAR= IJavaScriptSearchConstants.VAR;
	private final static int FUNCTION= IJavaScriptSearchConstants.FUNCTION;
	
	// limit to
	private final static int DECLARATIONS= IJavaScriptSearchConstants.DECLARATIONS;
//	private final static int IMPLEMENTORS= IJavaScriptSearchConstants.IMPLEMENTORS;
	private final static int REFERENCES= IJavaScriptSearchConstants.REFERENCES;
	private final static int ALL_OCCURRENCES= IJavaScriptSearchConstants.ALL_OCCURRENCES;
	private final static int READ_ACCESSES= IJavaScriptSearchConstants.READ_ACCESSES;
	private final static int WRITE_ACCESSES= IJavaScriptSearchConstants.WRITE_ACCESSES;
	
	public static final String PARTICIPANT_EXTENSION_POINT= "org.eclipse.wst.jsdt.ui.queryParticipants"; //$NON-NLS-1$

	public static final String EXTENSION_POINT_ID= "org.eclipse.wst.jsdt.ui.JavaSearchPage"; //$NON-NLS-1$
	
	private static final int HISTORY_SIZE= 12;
	
	// Dialog store id constants
	private final static String PAGE_NAME= "JavaSearchPage"; //$NON-NLS-1$
	private final static String STORE_CASE_SENSITIVE= "CASE_SENSITIVE"; //$NON-NLS-1$
	private final static String STORE_INCLUDE_MASK= "INCLUDE_MASK"; //$NON-NLS-1$
	private final static String STORE_HISTORY= "HISTORY"; //$NON-NLS-1$
	private final static String STORE_HISTORY_SIZE= "HISTORY_SIZE"; //$NON-NLS-1$
	
	private final List fPreviousSearchPatterns;
	
	private SearchPatternData fInitialData;
	private IJavaScriptElement fJavaElement;
	private boolean fFirstTime= true;
	private IDialogSettings fDialogSettings;
	private boolean fIsCaseSensitive;
	
	private Combo fPattern;
	private ISearchPageContainer fContainer;
	private Button fCaseSensitive;
	
	private Button[] fSearchFor;
	private Button[] fLimitTo;
	private Button[] fIncludeMasks;

	
	/**
	 * 
	 */
	public JavaSearchPage() {
		fPreviousSearchPatterns= new ArrayList();
	}
	
	
	//---- Action Handling ------------------------------------------------
	
	public boolean performAction() {
		return performNewSearch();
	}
	
	private boolean performNewSearch() {
		SearchPatternData data= getPatternData();

		// Setup search scope
		IJavaScriptSearchScope scope= null;
		String scopeDescription= ""; //$NON-NLS-1$
		
		int searchFor= data.getSearchFor();
		int limitTo= data.getLimitTo();
		
		int includeMask= data.getIncludeMask();
		JavaSearchScopeFactory factory= JavaSearchScopeFactory.getInstance();
		
		switch (getContainer().getSelectedScope()) {
			case ISearchPageContainer.WORKSPACE_SCOPE:
				scopeDescription= factory.getWorkspaceScopeDescription(includeMask);
				scope= factory.createWorkspaceScope(includeMask);
				break;
			case ISearchPageContainer.SELECTION_SCOPE:
				IJavaScriptElement[] javaElements= factory.getJavaElements(getContainer().getSelection());
				scope= factory.createJavaSearchScope(javaElements, includeMask);
				scopeDescription= factory.getSelectionScopeDescription(javaElements, includeMask);
				break;
			case ISearchPageContainer.SELECTED_PROJECTS_SCOPE: {
				String[] projectNames= getContainer().getSelectedProjectNames();
				scope= factory.createJavaProjectSearchScope(projectNames, includeMask);
				scopeDescription= factory.getProjectScopeDescription(projectNames, includeMask);
				break;
			}
			case ISearchPageContainer.WORKING_SET_SCOPE: {
				IWorkingSet[] workingSets= getContainer().getSelectedWorkingSets();
				// should not happen - just to be sure
				if (workingSets == null || workingSets.length < 1)
					return false;
				scopeDescription= factory.getWorkingSetScopeDescription(workingSets, includeMask); 
				scope= factory.createJavaSearchScope(workingSets, includeMask);
				SearchUtil.updateLRUWorkingSets(workingSets);
			}
		}
		
		QuerySpecification querySpec= null;
		if (data.getJavaElement() != null && getPattern().equals(fInitialData.getPattern())) {
			if (limitTo == REFERENCES)
				SearchUtil.warnIfBinaryConstant(data.getJavaElement(), getShell());
			querySpec= new ElementQuerySpecification(data.getJavaElement(), limitTo, scope, scopeDescription);
		} else {
			querySpec= new PatternQuerySpecification(data.getPattern(), searchFor, data.isCaseSensitive(), data.getLimitTo(), scope, scopeDescription);
			data.setJavaElement(null);
		} 
		
		JavaSearchQuery textSearchJob= new JavaSearchQuery(querySpec);
		NewSearchUI.runQueryInBackground(textSearchJob);
		return true;
	}
	
	private int getLimitTo() {
		for (int i= 0; i < fLimitTo.length; i++) {
			Button button= fLimitTo[i];
			if (button.getSelection()) {
				return getIntData(button);
			}
		}
		return -1;
	}

	private int setLimitTo(int searchFor, int limitTo) {
//		if (searchFor != TYPE && limitTo == IMPLEMENTORS) {
//			limitTo= REFERENCES;
//		}

		if ( searchFor != FIELD && searchFor != VAR && (limitTo == READ_ACCESSES || limitTo == WRITE_ACCESSES)) {
			limitTo= REFERENCES;
		}
		
		for (int i= 0; i < fLimitTo.length; i++) {
			Button button= fLimitTo[i];
			int val= getIntData(button);
			button.setSelection(limitTo == val);
			
			switch (val) {
				case DECLARATIONS:
				case REFERENCES:
				case ALL_OCCURRENCES:
					button.setEnabled(true);
					break;
//				case IMPLEMENTORS:
//					button.setEnabled(searchFor == TYPE);
//					break;
				case READ_ACCESSES:
				case WRITE_ACCESSES:
					button.setEnabled(searchFor == FIELD || searchFor==VAR);
					break;					
			}
		}
		return limitTo;
	}
	
	private int getIncludeMask() {
		int mask= 0;
		for (int i= 0; i < fIncludeMasks.length; i++) {
			Button button= fIncludeMasks[i];
			if (button.getSelection()) {
				mask |= getIntData(button);
			}
		}
		return mask;
	}
	
	private void setIncludeMask(int includeMask, int limitTo) {
		for (int i= 0; i < fIncludeMasks.length; i++) {
			Button button= fIncludeMasks[i];
			button.setSelection((includeMask & getIntData(button)) != 0);
		}
	}
	

	private String[] getPreviousSearchPatterns() {
		// Search results are not persistent
		int patternCount= fPreviousSearchPatterns.size();
		String [] patterns= new String[patternCount];
		for (int i= 0; i < patternCount; i++)
			patterns[i]= ((SearchPatternData) fPreviousSearchPatterns.get(i)).getPattern();
		return patterns;
	}
	
	private int getSearchFor() {
		for (int i= 0; i < fSearchFor.length; i++) {
			Button button= fSearchFor[i];
			if (button.getSelection()) {
				return getIntData(button);
			}
		}
		Assert.isTrue(false, "shouldNeverHappen"); //$NON-NLS-1$
		return -1;
	}
	
	private void setSearchFor(int searchFor) {
		for (int i= 0; i < fSearchFor.length; i++) {
			Button button= fSearchFor[i];
			button.setSelection(searchFor == getIntData(button));
		}
	}
	
	private int getIntData(Button button) {
		return ((Integer) button.getData()).intValue();
	}
	
	private String getPattern() {
		return fPattern.getText();
	}

	
	private SearchPatternData findInPrevious(String pattern) {
		for (Iterator iter= fPreviousSearchPatterns.iterator(); iter.hasNext();) {
			SearchPatternData element= (SearchPatternData) iter.next();
			if (pattern.equals(element.getPattern())) {
				return element;
			}
		}
		return null;
	}
	
	/**
	 * Return search pattern data and update previous searches.
	 * An existing entry will be updated.
	 * @return the pattern data
	 */
	private SearchPatternData getPatternData() {
		String pattern= getPattern();
		SearchPatternData match= findInPrevious(pattern);
		if (match != null) {
			fPreviousSearchPatterns.remove(match);
		}
		match= new SearchPatternData(
				getSearchFor(),
				getLimitTo(),
				pattern,
				fCaseSensitive.getSelection(),
				fJavaElement,
				getContainer().getSelectedScope(),
				getContainer().getSelectedWorkingSets(),
				getIncludeMask()
		);
			
		fPreviousSearchPatterns.add(0, match); // insert on top
		return match;
	}

	/*
	 * Implements method from IDialogPage
	 */
	public void setVisible(boolean visible) {
		if (visible && fPattern != null) {
			if (fFirstTime) {
				fFirstTime= false;
				// Set item and text here to prevent page from resizing
				fPattern.setItems(getPreviousSearchPatterns());
				initSelections();
			}
			fPattern.setFocus();
		}
		updateOKStatus();
		super.setVisible(visible);
	}
	
	public boolean isValid() {
		return true;
	}

	//---- Widget creation ------------------------------------------------

	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		readConfiguration();
		
		Composite result= new Composite(parent, SWT.NONE);
		
		GridLayout layout= new GridLayout(2, false);
		layout.horizontalSpacing= 10;
		result.setLayout(layout);
		
		Control expressionComposite= createExpression(result);
		expressionComposite.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1));
		
		Label separator= new Label(result, SWT.NONE);
		separator.setVisible(false);
		GridData data= new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1);
		data.heightHint= convertHeightInCharsToPixels(1) / 3;
		separator.setLayoutData(data);
		
		Control searchFor= createSearchFor(result);
		searchFor.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1));

		Control limitTo= createLimitTo(result);
		limitTo.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1));

		Control includeMask= createIncludeMask(result);
		includeMask.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
				
		//createParticipants(result);
		
		SelectionAdapter javaElementInitializer= new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (getSearchFor() == fInitialData.getSearchFor())
					fJavaElement= fInitialData.getJavaElement();
				else
					fJavaElement= null;
				int limitToVal= setLimitTo(getSearchFor(), getLimitTo());
				setIncludeMask(getIncludeMask(), limitToVal);
				doPatternModified();
			}
		};

		for (int i= 0; i < fSearchFor.length; i++) {
			fSearchFor[i].addSelectionListener(javaElementInitializer);
		}

		setControl(result);

		Dialog.applyDialogFont(result);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(result, IJavaHelpContextIds.JAVA_SEARCH_PAGE);	
	}
	
	
	/*private Control createParticipants(Composite result) {
		if (!SearchParticipantsExtensionPoint.hasAnyParticipants())
			return new Composite(result, SWT.NULL);
		Button selectParticipants= new Button(result, SWT.PUSH);
		selectParticipants.setText(SearchMessages.getString("SearchPage.select_participants.label")); //$NON-NLS-1$
		GridData gd= new GridData();
		gd.verticalAlignment= GridData.VERTICAL_ALIGN_BEGINNING;
		gd.horizontalAlignment= GridData.HORIZONTAL_ALIGN_END;
		gd.grabExcessHorizontalSpace= false;
		gd.horizontalAlignment= GridData.END;
		gd.horizontalSpan= 2;
		selectParticipants.setLayoutData(gd);
		selectParticipants.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PreferencePageSupport.showPreferencePage(getShell(), "org.eclipse.wst.jsdt.ui.preferences.SearchParticipantsExtensionPoint", new SearchParticipantsExtensionPoint()); //$NON-NLS-1$
			}

		});
		return selectParticipants;
	}*/


	private Control createExpression(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout(2, false);
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		result.setLayout(layout);

		// Pattern text + info
		Label label= new Label(result, SWT.LEFT);
		label.setText(SearchMessages.SearchPage_expression_label); 
		label.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1));

		// Pattern combo
		fPattern= new Combo(result, SWT.SINGLE | SWT.BORDER);
		fPattern.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handlePatternSelected();
				updateOKStatus();
			}
		});
		fPattern.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doPatternModified();
				updateOKStatus();

			}
		});
		TextFieldNavigationHandler.install(fPattern);
		GridData data= new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1);
		data.widthHint= convertWidthInCharsToPixels(50);
		fPattern.setLayoutData(data);

		// Ignore case checkbox		
		fCaseSensitive= new Button(result, SWT.CHECK);
		fCaseSensitive.setText(SearchMessages.SearchPage_expression_caseSensitive); 
		fCaseSensitive.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fIsCaseSensitive= fCaseSensitive.getSelection();
			}
		});
		fCaseSensitive.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1));
		
		return result;
	}
	
	final void updateOKStatus() {
		boolean isValid= isValidSearchPattern();
		getContainer().setPerformActionEnabled(isValid);
	}
	
	private boolean isValidSearchPattern() {
		if (getPattern().length() == 0) {
			return false;
		}
		if (fJavaElement != null) {
			return true;
		}
		return SearchPattern.createPattern(getPattern(), getSearchFor(), getLimitTo(), SearchPattern.R_EXACT_MATCH) != null;		
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	public void dispose() {
		writeConfiguration();
		super.dispose();
	}

	private void doPatternModified() {
		if (fInitialData != null && getPattern().equals(fInitialData.getPattern()) && fInitialData.getJavaElement() != null && fInitialData.getSearchFor() == getSearchFor()) {
			fCaseSensitive.setEnabled(false);
			fCaseSensitive.setSelection(true);
			fJavaElement= fInitialData.getJavaElement();
		} else {
			fCaseSensitive.setEnabled(true);
			fCaseSensitive.setSelection(fIsCaseSensitive);
			fJavaElement= null;
		}
	}

	private void handlePatternSelected() {
		int selectionIndex= fPattern.getSelectionIndex();
		if (selectionIndex < 0 || selectionIndex >= fPreviousSearchPatterns.size())
			return;
		
		SearchPatternData initialData= (SearchPatternData) fPreviousSearchPatterns.get(selectionIndex);

		setSearchFor(initialData.getSearchFor());
		int limitToVal= setLimitTo(initialData.getSearchFor(), initialData.getLimitTo());
		setIncludeMask(initialData.getIncludeMask(), limitToVal);

		fPattern.setText(initialData.getPattern());
		fIsCaseSensitive= initialData.isCaseSensitive();
		fJavaElement= initialData.getJavaElement();
		fCaseSensitive.setEnabled(fJavaElement == null);
		fCaseSensitive.setSelection(initialData.isCaseSensitive());

		
		if (initialData.getWorkingSets() != null)
			getContainer().setSelectedWorkingSets(initialData.getWorkingSets());
		else
			getContainer().setSelectedScope(initialData.getScope());
		
		fInitialData= initialData;
	}
	

	private Control createSearchFor(Composite parent) {
		Group result= new Group(parent, SWT.NONE);
		result.setText(SearchMessages.SearchPage_searchFor_label); 
		result.setLayout(new GridLayout(2, true));

		fSearchFor= new Button[] {
			createButton(result, SWT.RADIO, SearchMessages.SearchPage_searchFor_function, FUNCTION, true),
			createButton(result, SWT.RADIO, SearchMessages.SearchPage_searchFor_var, VAR, false),
			createButton(result, SWT.RADIO, SearchMessages.SearchPage_searchFor_method, METHOD, false),
			createButton(result, SWT.RADIO, SearchMessages.SearchPage_searchFor_field, FIELD, false),
			createButton(result, SWT.RADIO, SearchMessages.SearchPage_searchFor_type, TYPE, false),
//			createButton(result, SWT.RADIO, SearchMessages.SearchPage_searchFor_package, PACKAGE, false),
			createButton(result, SWT.RADIO, SearchMessages.SearchPage_searchFor_constructor, CONSTRUCTOR, false)
		};
			
		// Fill with dummy radio buttons
		Label filler= new Label(result, SWT.NONE);
		filler.setVisible(false);
		filler.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

		return result;		
	}
	
	private Control createLimitTo(Composite parent) {
		Group result= new Group(parent, SWT.NONE);
		result.setText(SearchMessages.SearchPage_limitTo_label); 
		result.setLayout(new GridLayout(2, true));

		fLimitTo= new Button[] {
			createButton(result, SWT.RADIO, SearchMessages.SearchPage_limitTo_declarations, DECLARATIONS, false),
//			createButton(result, SWT.RADIO, SearchMessages.SearchPage_limitTo_implementors, IMPLEMENTORS, false),
			createButton(result, SWT.RADIO, SearchMessages.SearchPage_limitTo_references, REFERENCES, true),
			createButton(result, SWT.RADIO, SearchMessages.SearchPage_limitTo_allOccurrences, ALL_OCCURRENCES, false),
			createButton(result, SWT.RADIO, SearchMessages.SearchPage_limitTo_readReferences, READ_ACCESSES, false),
			createButton(result, SWT.RADIO, SearchMessages.SearchPage_limitTo_writeReferences, WRITE_ACCESSES, false)
		};
		
		SelectionAdapter listener= new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateUseJRE();
			}
		};
		for (int i= 0; i < fLimitTo.length; i++) {
			fLimitTo[i].addSelectionListener(listener);
		}
		return result;		
	}
	
	private Control createIncludeMask(Composite parent) {
		Group result= new Group(parent, SWT.NONE);
		result.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		result.setText(SearchMessages.SearchPage_searchIn_label); 
		result.setLayout(new GridLayout(4, false));
		fIncludeMasks= new Button[] {
			createButton(result, SWT.CHECK, SearchMessages.SearchPage_searchIn_sources, JavaSearchScopeFactory.SOURCES, true),
			createButton(result, SWT.CHECK, SearchMessages.SearchPage_searchIn_projects, JavaSearchScopeFactory.PROJECTS, true),
			createButton(result, SWT.CHECK, SearchMessages.SearchPage_searchIn_jre, JavaSearchScopeFactory.JRE, false),
			createButton(result, SWT.CHECK, SearchMessages.SearchPage_searchIn_libraries, JavaSearchScopeFactory.LIBS, true),
		};
		return result;
	}
	
	private Button createButton(Composite parent, int style, String text, int data, boolean isSelected) {
		Button button= new Button(parent, style);
		button.setText(text);
		button.setData(Integer.valueOf(data));
		button.setLayoutData(new GridData());
		button.setSelection(isSelected);
		return button;
	}
	
	private void initSelections() {
		ISelection sel= getContainer().getSelection();
		SearchPatternData initData= null;

		if (sel instanceof IStructuredSelection) {
			initData= tryStructuredSelection((IStructuredSelection) sel);
		} else if (sel instanceof ITextSelection) {
			IEditorPart activePart= getActiveEditor();
			if (activePart instanceof JavaEditor) {
				try {
					IJavaScriptElement[] elements= SelectionConverter.codeResolve((JavaEditor) activePart);
					if (elements != null && elements.length > 0) {
						// Elements array may contain null-values: https://bugs.eclipse.org/bugs/show_bug.cgi?id=473186
						// As such, we're to take the first non-null element if any
						// TODO: It's not OK to have null values here, so it should be investigated
						for (int i = 0; i < elements.length; i++) {
							if (elements[i] != null) {
								initData= determineInitValuesFrom(elements[1]);
								break;
							}
						}
					}
				} catch (JavaScriptModelException e) {
					// ignore
				}
			}
			if (initData == null) {
				initData= trySimpleTextSelection((ITextSelection) sel);
			}
		}
		if (initData == null) {
			initData= getDefaultInitValues();
		}
		
		fInitialData= initData;
		fJavaElement= initData.getJavaElement();
		fCaseSensitive.setSelection(initData.isCaseSensitive());
		fCaseSensitive.setEnabled(fJavaElement == null);
		
		setSearchFor(initData.getSearchFor());
		int limitToVal= setLimitTo(initData.getSearchFor(), initData.getLimitTo());
		setIncludeMask(initData.getIncludeMask(), limitToVal);

		fPattern.setText(initData.getPattern());
	}

	private void updateUseJRE() {
		setIncludeMask(getIncludeMask(), getLimitTo());
	}

	private static boolean forceIncludeAll(int limitTo, IJavaScriptElement elem) {
		return elem != null && (limitTo == DECLARATIONS /*|| limitTo == IMPLEMENTORS*/);
	}

	private SearchPatternData tryStructuredSelection(IStructuredSelection selection) {
		if (selection == null || selection.size() > 1)
			return null;

		Object o= selection.getFirstElement();
		SearchPatternData res= null;
		if (o instanceof IJavaScriptElement) {
			res= determineInitValuesFrom((IJavaScriptElement) o);
//		} else if (o instanceof LogicalPackage) {
//			LogicalPackage lp= (LogicalPackage)o;
//			return new SearchPatternData(PACKAGE, REFERENCES, fIsCaseSensitive, lp.getElementName(), null, getLastIncludeMask());
		} else if (o instanceof IAdaptable) {
			IJavaScriptElement element= (IJavaScriptElement) ((IAdaptable) o).getAdapter(IJavaScriptElement.class);
			if (element != null) {
				res= determineInitValuesFrom(element);
			}
		}
		if (res == null && o instanceof IAdaptable) {
			IWorkbenchAdapter adapter= (IWorkbenchAdapter)((IAdaptable)o).getAdapter(IWorkbenchAdapter.class);
			if (adapter != null) {
				return new SearchPatternData(VAR, REFERENCES, fIsCaseSensitive, adapter.getLabel(o), null, getLastIncludeMask());
			}
		}
		return res;
	}
	
	final static boolean isSearchableType(IJavaScriptElement element) {
		switch (element.getElementType()) {
			case IJavaScriptElement.PACKAGE_FRAGMENT:
			case IJavaScriptElement.IMPORT_DECLARATION:
			case IJavaScriptElement.TYPE:
			case IJavaScriptElement.FIELD:
			case IJavaScriptElement.METHOD:
				return true;
		}
		return false;
	}

	private SearchPatternData determineInitValuesFrom(IJavaScriptElement element) {
		try {
			//JavaSearchScopeFactory factory= JavaSearchScopeFactory.getInstance();
			//boolean isInsideJRE= factory.isInsideJRE(element);
			int includeMask= getLastIncludeMask();
			
			switch (element.getElementType()) {
//				case IJavaScriptElement.PACKAGE_FRAGMENT:
//				case IJavaScriptElement.PACKAGE_DECLARATION:
//					return new SearchPatternData(PACKAGE, REFERENCES, true, element.getElementName(), element, includeMask);
//				case IJavaScriptElement.IMPORT_DECLARATION: {
//					IImportDeclaration declaration= (IImportDeclaration) element;
//					if (declaration.isOnDemand()) {
//						String name= Signature.getQualifier(declaration.getElementName());
//						return new SearchPatternData(PACKAGE, DECLARATIONS, true, name, element, JavaSearchScopeFactory.ALL);
//					}
//					return new SearchPatternData(TYPE, DECLARATIONS, true, element.getElementName(), element, JavaSearchScopeFactory.ALL);
//				}
				case IJavaScriptElement.TYPE:
					return new SearchPatternData(TYPE, REFERENCES, true, PatternStrings.getTypeSignature((IType) element), element, includeMask);
				case IJavaScriptElement.JAVASCRIPT_UNIT: {
					IType mainType= ((IJavaScriptUnit) element).findPrimaryType();
					if (mainType != null) {
						return new SearchPatternData(TYPE, REFERENCES, true, PatternStrings.getTypeSignature(mainType), mainType, includeMask);
					}
					break;
				}
				case IJavaScriptElement.CLASS_FILE: {
					IType mainType= ((IClassFile) element).getType();
					if (mainType.exists()) {
						return new SearchPatternData(TYPE, REFERENCES, true, PatternStrings.getTypeSignature(mainType), mainType, includeMask);
					}
					break;
				}
				case IJavaScriptElement.FIELD:
					IField field = (IField) element;
					return new SearchPatternData(field.getParent().getElementType()==IJavaScriptElement.TYPE?FIELD:VAR, REFERENCES, true,
							PatternStrings.getFieldSignature(field), element, includeMask);
				case IJavaScriptElement.METHOD:
					IFunction method= (IFunction) element;
					int searchFor= method.isConstructor() ? CONSTRUCTOR : METHOD;
					if (method.getParent().getElementType()!=IJavaScriptElement.TYPE)
						searchFor=FUNCTION;
					return new SearchPatternData(searchFor, REFERENCES, true, PatternStrings.getMethodSignature(method), element, includeMask);
			}
			
		} catch (JavaScriptModelException e) {
			if (!e.isDoesNotExist()) {
				ExceptionHandler.handle(e, SearchMessages.Search_Error_javaElementAccess_title, SearchMessages.Search_Error_javaElementAccess_message); 
			}
			// element might not exist
		}
		return null;	
	}
	
	private SearchPatternData trySimpleTextSelection(ITextSelection selection) {
		String selectedText= selection.getText();
		if (selectedText != null && selectedText.length() > 0) {
			int i= 0;
			while (i < selectedText.length() && !IndentManipulation.isLineDelimiterChar(selectedText.charAt(i))) {
				i++;
			}
			if (i > 0) {
				return new SearchPatternData(TYPE, REFERENCES, fIsCaseSensitive, selectedText.substring(0, i), null, JavaSearchScopeFactory.ALL);
			}
		}
		return null;
	}
	
	private SearchPatternData getDefaultInitValues() {
		if (!fPreviousSearchPatterns.isEmpty()) {
			return (SearchPatternData) fPreviousSearchPatterns.get(0);
		}

		return new SearchPatternData(TYPE, REFERENCES, fIsCaseSensitive, "", null, getLastIncludeMask()); //$NON-NLS-1$
	}
	
	private int getLastIncludeMask() {
		try {
			return getDialogSettings().getInt(STORE_INCLUDE_MASK);
		} catch (NumberFormatException e) {
			return JavaSearchScopeFactory.NO_JRE;
		}
	}

	/*
	 * Implements method from ISearchPage
	 */
	public void setContainer(ISearchPageContainer container) {
		fContainer= container;
	}
	
	/**
	 * Returns the search page's container.
	 * @return the search page container
	 */
	private ISearchPageContainer getContainer() {
		return fContainer;
	}
		
	private IEditorPart getActiveEditor() {
		IWorkbenchPage activePage= JavaScriptPlugin.getActivePage();
		if (activePage != null) {
			return activePage.getActiveEditor();
		}
		return null;
	}
	
	//--------------- Configuration handling --------------
	
	/**
	 * Returns the page settings for this Java search page.
	 * 
	 * @return the page settings to be used
	 */
	private IDialogSettings getDialogSettings() {
		if (fDialogSettings == null) {
			fDialogSettings= JavaScriptPlugin.getDefault().getDialogSettingsSection(PAGE_NAME);
		}
		return fDialogSettings;
	}
	
	/**
	 * Initializes itself from the stored page settings.
	 */
	private void readConfiguration() {
		IDialogSettings s= getDialogSettings();
		fIsCaseSensitive= s.getBoolean(STORE_CASE_SENSITIVE);
		
		try {
			int historySize= s.getInt(STORE_HISTORY_SIZE);
			for (int i= 0; i < historySize; i++) {
				IDialogSettings histSettings= s.getSection(STORE_HISTORY + i);
				if (histSettings != null) {
					SearchPatternData data= SearchPatternData.create(histSettings);
					if (data != null) {
						fPreviousSearchPatterns.add(data);
					}
				}
			}
		} catch (NumberFormatException e) {
			// ignore
		}
	}
	
	/**
	 * Stores the current configuration in the dialog store.
	 */
	private void writeConfiguration() {
		IDialogSettings s= getDialogSettings();
		s.put(STORE_CASE_SENSITIVE, fIsCaseSensitive);
		s.put(STORE_INCLUDE_MASK, getIncludeMask());
		
		int historySize= Math.min(fPreviousSearchPatterns.size(), HISTORY_SIZE);
		s.put(STORE_HISTORY_SIZE, historySize);
		for (int i= 0; i < historySize; i++) {
			IDialogSettings histSettings= s.addNewSection(STORE_HISTORY + i);
			SearchPatternData data= ((SearchPatternData) fPreviousSearchPatterns.get(i));
			data.store(histSettings);
		}
	}
}
