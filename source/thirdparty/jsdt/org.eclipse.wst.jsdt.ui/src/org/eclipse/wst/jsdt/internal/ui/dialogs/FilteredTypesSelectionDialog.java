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
package org.eclipse.wst.jsdt.internal.ui.dialogs;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.SearchPattern;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.infer.IInferEngine;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope;
import org.eclipse.wst.jsdt.core.search.SearchEngine;
import org.eclipse.wst.jsdt.core.search.TypeNameMatch;
import org.eclipse.wst.jsdt.core.search.TypeNameMatchRequestor;
import org.eclipse.wst.jsdt.core.search.TypeNameRequestor;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.OpenTypeHistory;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.corext.util.TypeFilter;
import org.eclipse.wst.jsdt.internal.corext.util.TypeInfoRequestorAdapter;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaUIMessages;
import org.eclipse.wst.jsdt.internal.ui.preferences.TypeFilterPreferencePage;
import org.eclipse.wst.jsdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.util.TypeNameMatchLabelProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ColoredJavaElementLabels;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ColoredString;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ColoredViewersManager;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.OwnerDrawSupport;
import org.eclipse.wst.jsdt.internal.ui.workingsets.WorkingSetFilterActionGroup;
import org.eclipse.wst.jsdt.launching.IVMInstall;
import org.eclipse.wst.jsdt.launching.IVMInstallType;
import org.eclipse.wst.jsdt.launching.JavaRuntime;
import org.eclipse.wst.jsdt.launching.LibraryLocation;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.dialogs.ITypeInfoFilterExtension;
import org.eclipse.wst.jsdt.ui.dialogs.ITypeInfoImageProvider;
import org.eclipse.wst.jsdt.ui.dialogs.ITypeSelectionComponent;
import org.eclipse.wst.jsdt.ui.dialogs.TypeSelectionExtension;

/**
 * Shows a list of Java types to the user with a text entry field for a string
 * pattern used to filter the list of types.
 * 
 * 
 */
public class FilteredTypesSelectionDialog extends FilteredItemsSelectionDialog implements ITypeSelectionComponent {
	
	/**
	 * Disabled "Show Container for Duplicates because of
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=184693 .
	 */
	private static final boolean BUG_184693= true;

	private static final String DIALOG_SETTINGS= "org.eclipse.wst.jsdt.internal.ui.dialogs.FilteredTypesSelectionDialog"; //$NON-NLS-1$

	private static final String SHOW_CONTAINER_FOR_DUPLICATES= "ShowContainerForDuplicates"; //$NON-NLS-1$

	private static final String WORKINGS_SET_SETTINGS= "WorkingSet"; //$NON-NLS-1$

	private WorkingSetFilterActionGroup fFilterActionGroup;

	private final TypeItemLabelProvider fTypeInfoLabelProvider;

	private String fTitle;

	private ShowContainerForDuplicatesAction fShowContainerForDuplicatesAction;

	private IJavaScriptSearchScope fSearchScope;
	
	private boolean fAllowScopeSwitching;

	private final int fElementKinds;

	private final ITypeInfoFilterExtension fFilterExtension;

	private final TypeSelectionExtension fExtension;

	private ISelectionStatusValidator fValidator;

	private final TypeInfoUtil fTypeInfoUtil;
	
	private static boolean fgFirstTime= true;

	private final TypeItemsComparator fTypeItemsComparator; 
	
	private int fTypeFilterVersion= 0;

	/**
	 * Creates new FilteredTypesSelectionDialog instance
	 * 
	 * @param parent
	 *            shell to parent the dialog on
	 * @param multi
	 *            <code>true</code> if multiple selection is allowed
	 * @param context
	 *            context used to execute long-running operations associated
	 *            with this dialog
	 * @param scope
	 *            scope used when searching for types
	 * @param elementKinds
	 *            flags defining nature of searched elements; the only valid
	 *            values are: <code>IJavaScriptSearchConstants.TYPE</code>
	 * 	<code>IJavaScriptSearchConstants.ANNOTATION_TYPE</code>
	 * 	<code>IJavaScriptSearchConstants.INTERFACE</code>
	 * 	<code>IJavaScriptSearchConstants.ENUM</code>
	 * 	<code>IJavaScriptSearchConstants.CLASS_AND_INTERFACE</code>
	 * 	<code>IJavaScriptSearchConstants.CLASS_AND_ENUM</code>.
	 *            Please note that the bitwise OR combination of the elementary
	 *            constants is not supported.
	 */
	public FilteredTypesSelectionDialog(Shell parent, boolean multi, IRunnableContext context, IJavaScriptSearchScope scope, int elementKinds) {
		this(parent, multi, context, scope, elementKinds, null);
	}

	/**
	 * Creates new FilteredTypesSelectionDialog instance.
	 * 
	 * @param shell
	 *            shell to parent the dialog on
	 * @param multi
	 *            <code>true</code> if multiple selection is allowed
	 * @param context
	 *            context used to execute long-running operations associated
	 *            with this dialog
	 * @param scope
	 *            scope used when searching for types. If the scope is <code>null</code>,
	 *            then workspace is scope is used as default, and the user can
	 *            choose a working set as scope. 
	 * @param elementKinds
	 *            flags defining nature of searched elements; the only valid
	 *            values are: <code>IJavaScriptSearchConstants.TYPE</code>
	 * 	<code>IJavaScriptSearchConstants.ANNOTATION_TYPE</code>
	 * 	<code>IJavaScriptSearchConstants.INTERFACE</code>
	 * 	<code>IJavaScriptSearchConstants.ENUM</code>
	 * 	<code>IJavaScriptSearchConstants.CLASS_AND_INTERFACE</code>
	 * 	<code>IJavaScriptSearchConstants.CLASS_AND_ENUM</code>.
	 *            Please note that the bitwise OR combination of the elementary
	 *            constants is not supported.
	 * @param extension
	 *            an extension of the standard type selection dialog; See
	 *            {@link TypeSelectionExtension}
	 */
	public FilteredTypesSelectionDialog(Shell shell, boolean multi, IRunnableContext context, IJavaScriptSearchScope scope, int elementKinds, TypeSelectionExtension extension) {
		super(shell, multi);
		
		setSelectionHistory(new TypeSelectionHistory());

		if (scope == null) {
			fAllowScopeSwitching= true;
			scope= SearchEngine.createWorkspaceScope();
		}
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IJavaHelpContextIds.TYPE_SELECTION_DIALOG2);

		fElementKinds= elementKinds;
		fExtension= extension;
		fFilterExtension= (extension == null) ? null : extension.getFilterExtension();
		fSearchScope= scope;

		if (extension != null) {
			fValidator= extension.getSelectionValidator();
		}

		fTypeInfoUtil= new TypeInfoUtil(extension != null ? extension.getImageProvider() : null);

		fTypeInfoLabelProvider= new TypeItemLabelProvider();

		setListLabelProvider(fTypeInfoLabelProvider);
		setListSelectionLabelDecorator(fTypeInfoLabelProvider);
		setDetailsLabelProvider(new TypeItemDetailsLabelProvider(fTypeInfoUtil));
		
		fTypeItemsComparator= new TypeItemsComparator();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.SelectionDialog#setTitle(java.lang.String)
	 */
	public void setTitle(String title) {
		super.setTitle(title);
		fTitle= title;
	}

	/**
	 * Adds or replaces subtitle of the dialog
	 * 
	 * @param text
	 *            the new subtitle for this dialog
	 */
	private void setSubtitle(String text) {
		if (text == null || text.length() == 0) {
			getShell().setText(fTitle);
		} else {
			getShell().setText(Messages.format(JavaUIMessages.FilteredTypeSelectionDialog_titleFormat, new String[] { fTitle, text }));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.AbstractSearchDialog#getDialogSettings()
	 */
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings= JavaScriptPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS);

		if (settings == null) {
			settings= JavaScriptPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);
		}

		return settings;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.AbstractSearchDialog#storeDialog(org.eclipse.jface.dialogs.IDialogSettings)
	 */
	protected void storeDialog(IDialogSettings settings) {
		super.storeDialog(settings);

		if (! BUG_184693) {
			settings.put(SHOW_CONTAINER_FOR_DUPLICATES, fShowContainerForDuplicatesAction.isChecked());
		}

		if (fFilterActionGroup != null) {
			XMLMemento memento= XMLMemento.createWriteRoot("workingSet"); //$NON-NLS-1$
			fFilterActionGroup.saveState(memento);
			fFilterActionGroup.dispose();
			StringWriter writer= new StringWriter();
			try {
				memento.save(writer);
				settings.put(WORKINGS_SET_SETTINGS, writer.getBuffer().toString());
			} catch (IOException e) {
				// don't do anything. Simply don't store the settings
				JavaScriptPlugin.log(e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.AbstractSearchDialog#restoreDialog(org.eclipse.jface.dialogs.IDialogSettings)
	 */
	protected void restoreDialog(IDialogSettings settings) {
		super.restoreDialog(settings);

		if (! BUG_184693) {
			boolean showContainer= settings.getBoolean(SHOW_CONTAINER_FOR_DUPLICATES);
			fShowContainerForDuplicatesAction.setChecked(showContainer);
			fTypeInfoLabelProvider.setContainerInfo(showContainer);
		} else {
			fTypeInfoLabelProvider.setContainerInfo(true);
		}
		
		if (fAllowScopeSwitching) {
			String setting= settings.get(WORKINGS_SET_SETTINGS);
			if (setting != null) {
				try {
					IMemento memento= XMLMemento.createReadRoot(new StringReader(setting));
					fFilterActionGroup.restoreState(memento);
				} catch (WorkbenchException e) {
					// don't do anything. Simply don't restore the settings
					JavaScriptPlugin.log(e);
				}
			}
			IWorkingSet ws= fFilterActionGroup.getWorkingSet();
			if (ws == null || (ws.isAggregateWorkingSet() && ws.isEmpty())) {
				setSearchScope(SearchEngine.createWorkspaceScope());
				setSubtitle(null);
			} else {
				setSearchScope(JavaSearchScopeFactory.getInstance().createJavaSearchScope(ws, true));
				setSubtitle(ws.getLabel());
			}
		}

		// TypeNameMatch[] types = OpenTypeHistory.getInstance().getTypeInfos();
		//
		// for (int i = 0; i < types.length; i++) {
		// TypeNameMatch type = types[i];
		// accessedHistoryItem(type);
		// }
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.AbstractSearchDialog#fillViewMenu(org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillViewMenu(IMenuManager menuManager) {
		super.fillViewMenu(menuManager);

		if (! BUG_184693) {
			fShowContainerForDuplicatesAction= new ShowContainerForDuplicatesAction();
			menuManager.add(fShowContainerForDuplicatesAction);
		}
		if (fAllowScopeSwitching) {
			fFilterActionGroup= new WorkingSetFilterActionGroup(getShell(), JavaScriptPlugin.getActivePage(), new IPropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
					IWorkingSet ws= (IWorkingSet) event.getNewValue();
					if (ws == null || (ws.isAggregateWorkingSet() && ws.isEmpty())) {
						setSearchScope(SearchEngine.createWorkspaceScope());
						setSubtitle(null);
					} else {
						setSearchScope(JavaSearchScopeFactory.getInstance().createJavaSearchScope(ws, true));
						setSubtitle(ws.getLabel());
					}
	
					applyFilter();
				}
			});
			fFilterActionGroup.fillViewMenu(menuManager);
		}
		//no type filter preference pages currently exist for JSDT
		//menuManager.add(new Separator());
		//menuManager.add(new TypeFiltersPreferencesAction());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#createExtendedContentArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createExtendedContentArea(Composite parent) {
		Control addition= null;

		if (fExtension != null) {

			addition= fExtension.createContentArea(parent);
			if (addition != null) {
				GridData gd= new GridData(GridData.FILL_HORIZONTAL);
				gd.horizontalSpan= 2;
				addition.setLayoutData(gd);

			}
			
			fExtension.initialize(this);
		}

		return addition;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.SelectionDialog#setResult(java.util.List)
	 */
	protected void setResult(List newResult) {

		List resultToReturn= new ArrayList();

		for (int i= 0; i < newResult.size(); i++) {
			if (newResult.get(i) instanceof TypeNameMatch) {
				IType type= ((TypeNameMatch) newResult.get(i)).getType();
				if (type.exists()) {
					// items are added to history in the
					// org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#computeResult()
					// method
					resultToReturn.add(type);
				} else {
					TypeNameMatch typeInfo= (TypeNameMatch) newResult.get(i);
					IPackageFragmentRoot root= typeInfo.getPackageFragmentRoot();
					String containerName= JavaScriptElementLabels.getElementLabel(root, JavaScriptElementLabels.ROOT_QUALIFIED);
					String message= Messages.format(JavaUIMessages.FilteredTypesSelectionDialog_dialogMessage, new String[] { typeInfo.getFullyQualifiedName(), containerName });
					MessageDialog.openError(getShell(), fTitle, message);
					getSelectionHistory().remove(typeInfo);
				}
			}
		}

		super.setResult(resultToReturn);
	}

	/*
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#create()
	 */
	public void create() {
		super.create();
		Control patternControl= getPatternControl();
		if (patternControl instanceof Text) {
			TextFieldNavigationHandler.install((Text) patternControl);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.window.Window#open()
	 */
	public int open() {
		if (getInitialPattern() == null) {
			IWorkbenchWindow window= JavaScriptPlugin.getActiveWorkbenchWindow();
			if (window != null) {
				ISelection selection= window.getSelectionService().getSelection();
				if (selection instanceof ITextSelection) {
					String text= ((ITextSelection) selection).getText();
					if (text != null) {
						text= text.trim();
						if (text.length() > 0 && JavaScriptConventions.validateJavaScriptTypeName(text, JavaScriptCore.VERSION_1_3, JavaScriptCore.VERSION_1_3).isOK()) {
							setInitialPattern(text, FULL_SELECTION);
						}
					}
				}
			}
		}
		return super.open();
	}

	/**
	 * Sets a new validator.
	 * 
	 * @param validator
	 *            the new validator
	 */
	public void setValidator(ISelectionStatusValidator validator) {
		fValidator= validator;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#createFilter()
	 */
	protected ItemsFilter createFilter() {
		return new TypeItemsFilter(fSearchScope, fElementKinds, fFilterExtension);
	}
	
	protected Control createContents(Composite parent) {
		Control contents= super.createContents(parent);
		if (ColoredViewersManager.showColoredLabels()) {
			if (contents instanceof Composite) {
				Table listControl= findTableControl((Composite) contents);
				if (listControl != null) {
					installOwnerDraw(listControl);
				}
			}
		}
		return contents;
	}
	
	private void installOwnerDraw(Table tableControl) {
		new OwnerDrawSupport(tableControl) { // installs the owner draw listeners
			public ColoredString getColoredLabel(Item item) {
				String text= item.getText();
				ColoredString str= new ColoredString(text);
				int index= text.indexOf('-');
				if (index != -1) {
					str.colorize(index, str.length() - index, ColoredJavaElementLabels.QUALIFIER_STYLE);
				}
				return str;
			}

			public Color getColor(String foregroundColorName, Display display) {
				return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry().get(foregroundColorName);
			}
		};
	}

	private Table findTableControl(Composite composite) {
		Control[] children= composite.getChildren();
		for (int i= 0; i < children.length; i++) {
			Control curr= children[i];
			if (curr instanceof Table) {
				return (Table) curr;
			} else if (curr instanceof Composite) {
				Table res= findTableControl((Composite) curr);
				if (res != null) {
					return res;
				}
			}
		}
		return null;
	}
	
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#fillContentProvider(org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.AbstractContentProvider,
	 *      org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter,
	 *      org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void fillContentProvider(AbstractContentProvider provider, ItemsFilter itemsFilter, IProgressMonitor progressMonitor) throws CoreException {
		TypeItemsFilter typeSearchFilter= (TypeItemsFilter) itemsFilter;
		TypeSearchRequestor requestor= new TypeSearchRequestor(provider, typeSearchFilter);
		SearchEngine engine= new SearchEngine((WorkingCopyOwner) null);
		progressMonitor.setTaskName(JavaUIMessages.FilteredTypesSelectionDialog_searchJob_taskName);
		
		/*
		 * Setting the filter into match everything mode avoids filtering twice
		 * by the same pattern (the search engine only provides filtered
		 * matches). For the case when the pattern is a camel case pattern with
		 * a terminator, the filter is not set to match everything mode because
		 * jdt.core's SearchPattern does not support that case.
		 */ 
		String prefix= typeSearchFilter.getInitialPattern();
		int matchRule= typeSearchFilter.getMatchRule();
		if (matchRule == SearchPattern.RULE_CAMELCASE_MATCH) {
			 // If the pattern is empty, the RULE_BLANK_MATCH will be chosen, so we don't have to check the pattern length
			char lastChar= prefix.charAt(prefix.length() - 1);

			if (lastChar == '<' || lastChar == ' ') {
				prefix= prefix.substring(0, prefix.length() - 1);
			} else {
				typeSearchFilter.setMatchEverythingMode(true);
			}
			matchRule |= SearchPattern.RULE_PREFIX_MATCH;
		} else {
			typeSearchFilter.setMatchEverythingMode(true);
		}

		try {
			engine.searchAllTypeNames(prefix.toCharArray(),
					matchRule, //TODO: https://bugs.eclipse.org/bugs/show_bug.cgi?id=176017
					typeSearchFilter.getElementKind(),
					typeSearchFilter.getSearchScope(),
					requestor,
					IJavaScriptSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
					progressMonitor);
		} finally {
			typeSearchFilter.setMatchEverythingMode(false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#getItemsComparator()
	 */
	protected Comparator getItemsComparator() {
		return fTypeItemsComparator;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#getElementName(java.lang.Object)
	 */
	public String getElementName(Object item) {
		TypeNameMatch type= (TypeNameMatch) item;
		return fTypeInfoUtil.getText(type);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#validateItem(java.lang.Object)
	 */
	protected IStatus validateItem(Object item) {

		if (item == null)
			return new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), IStatus.ERROR, "", null); //$NON-NLS-1$

		if (fValidator != null) {
			IType type= ((TypeNameMatch) item).getType();
			if (!type.exists())
				return new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), IStatus.ERROR, Messages.format(JavaUIMessages.FilteredTypesSelectionDialog_error_type_doesnot_exist, ((TypeNameMatch) item).getFullyQualifiedName()), null);
			Object[] elements= { type };
			return fValidator.validate(elements);
		} else
			return new Status(IStatus.OK, JavaScriptPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
	}

	/**
	 * Sets search scope used when searching for types.
	 * 
	 * @param scope
	 *            the new scope
	 */
	private void setSearchScope(IJavaScriptSearchScope scope) {
		fSearchScope= scope;
	}
	
	/*
	 * We only have to ensure history consistency here since the search engine
	 * takes care of working copies.
	 */
	private static class ConsistencyRunnable implements IRunnableWithProgress {
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			if (fgFirstTime) {
				// Join the initialize after load job.
				IJobManager manager= Job.getJobManager();
				manager.join(JavaScriptUI.ID_PLUGIN, monitor);
			}
			OpenTypeHistory history= OpenTypeHistory.getInstance();
			if (fgFirstTime || history.isEmpty()) {
				if (history.needConsistencyCheck()) {
					monitor.beginTask(JavaUIMessages.TypeSelectionDialog_progress_consistency, 100);
					refreshSearchIndices(new SubProgressMonitor(monitor, 90));
					history.checkConsistency(new SubProgressMonitor(monitor, 10));
				} else {
					refreshSearchIndices(monitor);
				}
				monitor.done();
				fgFirstTime= false;
			} else {
				history.checkConsistency(monitor);
			}
		}
		public static boolean needsExecution() {
			OpenTypeHistory history= OpenTypeHistory.getInstance();
			return fgFirstTime || history.isEmpty() || history.needConsistencyCheck(); 
		}
		private void refreshSearchIndices(IProgressMonitor monitor) throws InvocationTargetException {
			try {
				new SearchEngine().searchAllTypeNames(
						null,
						0,
						// make sure we search a concrete name. This is faster according to Kent  
						"_______________".toCharArray(), //$NON-NLS-1$
						SearchPattern.RULE_EXACT_MATCH | SearchPattern.RULE_CASE_SENSITIVE, 
						IJavaScriptSearchConstants.ENUM,
						SearchEngine.createWorkspaceScope(), 
						new TypeNameRequestor() {}, 
						IJavaScriptSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
						monitor);
			} catch (JavaScriptModelException e) {
				throw new InvocationTargetException(e);
			}
		}
	}
	
	/*
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#reloadCache(boolean, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void reloadCache(boolean checkDuplicates, IProgressMonitor monitor) {
		IProgressMonitor remainingMonitor;
		if (ConsistencyRunnable.needsExecution()) {
			monitor.beginTask(JavaUIMessages.TypeSelectionDialog_progress_consistency, 10);
			try {
				ConsistencyRunnable runnable= new ConsistencyRunnable();
				runnable.run(new SubProgressMonitor(monitor, 1));
			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, JavaUIMessages.TypeSelectionDialog_error3Title, JavaUIMessages.TypeSelectionDialog_error3Message); 
				close();
				return;
			} catch (InterruptedException e) {
				// cancelled by user
				close();
				return;
			}
			remainingMonitor= new SubProgressMonitor(monitor, 9);
		} else {
			remainingMonitor= monitor;
		}
		super.reloadCache(checkDuplicates, remainingMonitor);
		monitor.done();
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.dialogs.ITypeSelectionComponent#triggerSearch()
	 */
	public void triggerSearch() {
		fTypeFilterVersion++;
		applyFilter();
	}

	/**
	 * The <code>ShowContainerForDuplicatesAction</code> provides means to
	 * show/hide container information for duplicate elements.
	 */
	private class ShowContainerForDuplicatesAction extends Action {

		/**
		 * Creates a new instance of the class
		 */
		public ShowContainerForDuplicatesAction() {
			super(JavaUIMessages.FilteredTypeSelectionDialog_showContainerForDuplicatesAction, IAction.AS_CHECK_BOX);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			fTypeInfoLabelProvider.setContainerInfo(isChecked());
		}
	}
	
	private class TypeFiltersPreferencesAction extends Action {
		
		public TypeFiltersPreferencesAction() {
			super(JavaUIMessages.FilteredTypesSelectionDialog_TypeFiltersPreferencesAction_label);
		}
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			String typeFilterID= TypeFilterPreferencePage.TYPE_FILTER_PREF_PAGE_ID;
			PreferencesUtil.createPreferenceDialogOn(getShell(), typeFilterID, new String[] { typeFilterID }, null).open();
			triggerSearch();
		}
	}

	/**
	 * A <code>LabelProvider</code> for (the table of) types.
	 */
	private class TypeItemLabelProvider extends LabelProvider implements ILabelDecorator {

		private boolean fContainerInfo;


		/**
		 * Construct a new <code>TypeItemLabelProvider</code>. F
		 */
		public TypeItemLabelProvider() {

		}

		public void setContainerInfo(boolean containerInfo) {
			fContainerInfo= containerInfo;
			fireLabelProviderChanged(new LabelProviderChangedEvent(this));
		}

		private boolean isInnerType(TypeNameMatch match) {
			return match.getTypeQualifiedName().indexOf('.') != -1;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			if (!(element instanceof TypeNameMatch)) {
				return super.getImage(element);
			}

			TypeNameMatch type= (TypeNameMatch) element;

			ImageDescriptor iD= JavaElementImageProvider.getTypeImageDescriptor(isInnerType(type), false, type.getModifiers(), false);
			
			return JavaScriptPlugin.getImageDescriptorRegistry().get(iD);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			if (!(element instanceof TypeNameMatch)) {
				return super.getText(element);
			}

			if (fContainerInfo && isDuplicateElement(element)) {
				return fTypeInfoUtil.getFullyQualifiedText((TypeNameMatch) element);
			}

			if (!fContainerInfo && isDuplicateElement(element)) {
				return fTypeInfoUtil.getQualifiedText((TypeNameMatch) element);
			}

			return fTypeInfoUtil.getText(element);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.ILabelDecorator#decorateImage(org.eclipse.swt.graphics.Image,
		 *      java.lang.Object)
		 */
		public Image decorateImage(Image image, Object element) {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.ILabelDecorator#decorateText(java.lang.String,
		 *      java.lang.Object)
		 */
		public String decorateText(String text, Object element) {
			if (!(element instanceof TypeNameMatch)) {
				return null;
			}

			if (fContainerInfo && isDuplicateElement(element)) {
				return fTypeInfoUtil.getFullyQualifiedText((TypeNameMatch) element);
			}

			return fTypeInfoUtil.getQualifiedText((TypeNameMatch) element);
		}

	}

	/**
	 * A <code>LabelProvider</code> for the label showing type details.
	 */
	private static class TypeItemDetailsLabelProvider extends LabelProvider {

		private final TypeNameMatchLabelProvider fLabelProvider= new TypeNameMatchLabelProvider(TypeNameMatchLabelProvider.SHOW_TYPE_CONTAINER_ONLY + TypeNameMatchLabelProvider.SHOW_ROOT_POSTFIX);

		private final TypeInfoUtil fTypeInfoUtil;

		public TypeItemDetailsLabelProvider(TypeInfoUtil typeInfoUtil) {
			fTypeInfoUtil= typeInfoUtil;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			if (element instanceof TypeNameMatch) {
				return fLabelProvider.getImage((element));
			}

			return super.getImage(element);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			if (element instanceof TypeNameMatch) {
				return fTypeInfoUtil.getQualificationText((TypeNameMatch) element);
			}

			return super.getText(element);
		}
	}

	private static class TypeInfoUtil {

		private final ITypeInfoImageProvider fProviderExtension;

		private final TypeInfoRequestorAdapter fAdapter= new TypeInfoRequestorAdapter();

		private final Map fLib2Name= new HashMap();

		private final String[] fInstallLocations;

		private final String[] fVMNames;

		private boolean fFullyQualifyDuplicates;

		public TypeInfoUtil(ITypeInfoImageProvider extension) {
			fProviderExtension= extension;
			List locations= new ArrayList();
			List labels= new ArrayList();
			IVMInstallType[] installs= JavaRuntime.getVMInstallTypes();
			for (int i= 0; i < installs.length; i++) {
				processVMInstallType(installs[i], locations, labels);
			}
			fInstallLocations= (String[]) locations.toArray(new String[locations.size()]);
			fVMNames= (String[]) labels.toArray(new String[labels.size()]);

		}

		public void setFullyQualifyDuplicates(boolean value) {
			fFullyQualifyDuplicates= value;
		}

		private void processVMInstallType(IVMInstallType installType, List locations, List labels) {
			if (installType != null) {
				IVMInstall[] installs= installType.getVMInstalls();
				boolean isMac= Platform.OS_MACOSX.equals(Platform.getOS());
				final String HOME_SUFFIX= "/Home"; //$NON-NLS-1$
				for (int i= 0; i < installs.length; i++) {
					String label= getFormattedLabel(installs[i].getName());
					LibraryLocation[] libLocations= installs[i].getLibraryLocations();
					if (libLocations != null) {
						processLibraryLocation(libLocations, label);
					} else {
						String filePath= installs[i].getInstallLocation().getAbsolutePath();
						// on MacOS X install locations end in an additional
						// "/Home" segment; remove it
						if (isMac && filePath.endsWith(HOME_SUFFIX))
							filePath= filePath.substring(0, filePath.length() - HOME_SUFFIX.length() + 1);
						locations.add(filePath);
						labels.add(label);
					}
				}
			}
		}

		private void processLibraryLocation(LibraryLocation[] libLocations, String label) {
			for (int l= 0; l < libLocations.length; l++) {
				LibraryLocation location= libLocations[l];
				fLib2Name.put(location.getSystemLibraryPath().toOSString(), label);
			}
		}

		private String getFormattedLabel(String name) {
			return Messages.format(JavaUIMessages.FilteredTypesSelectionDialog_library_name_format, name);
		}

		public String getText(Object element) {

			return ((TypeNameMatch) element).getQualifiedName();
		}

		public String getQualifiedText(TypeNameMatch type) {
			StringBuffer result= new StringBuffer();
			result.append(type.getQualifiedName());
			String containerName= type.getTypeContainerName();
			result.append(JavaScriptElementLabels.CONCAT_STRING);
			if (containerName.length() > 0) {
				result.append(containerName);
			} else {
				result.append(JavaUIMessages.FilteredTypesSelectionDialog_default_package);
			}
			return result.toString();
		}

		public String getFullyQualifiedText(TypeNameMatch type) {
			StringBuffer result= new StringBuffer();
			result.append(type.getSimpleTypeName());
			String containerName= type.getTypeContainerName();
			if (containerName.length() > 0) {
				result.append(JavaScriptElementLabels.CONCAT_STRING);
				result.append(containerName);
			}
			result.append(JavaScriptElementLabels.CONCAT_STRING);
			result.append(getContainerName(type));
			return result.toString();
		}

		public String getText(TypeNameMatch last, TypeNameMatch current, TypeNameMatch next) {
			StringBuffer result= new StringBuffer();
			int qualifications= 0;
			String currentTN= current.getSimpleTypeName();
			result.append(currentTN);
			String currentTCN= getTypeContainerName(current);
			if (last != null) {
				String lastTN= last.getSimpleTypeName();
				String lastTCN= getTypeContainerName(last);
				if (currentTCN.equals(lastTCN)) {
					if (currentTN.equals(lastTN)) {
						result.append(JavaScriptElementLabels.CONCAT_STRING);
						result.append(currentTCN);
						result.append(JavaScriptElementLabels.CONCAT_STRING);
						result.append(getContainerName(current));
						return result.toString();
					}
				} else if (currentTN.equals(lastTN)) {
					qualifications= 1;
				}
			}
			if (next != null) {
				String nextTN= next.getSimpleTypeName();
				String nextTCN= getTypeContainerName(next);
				if (currentTCN.equals(nextTCN)) {
					if (currentTN.equals(nextTN)) {
						result.append(JavaScriptElementLabels.CONCAT_STRING);
						result.append(currentTCN);
						result.append(JavaScriptElementLabels.CONCAT_STRING);
						result.append(getContainerName(current));
						return result.toString();
					}
				} else if (currentTN.equals(nextTN)) {
					qualifications= 1;
				}
			}
			if (qualifications > 0) {
				result.append(JavaScriptElementLabels.CONCAT_STRING);
				result.append(currentTCN);
				if (fFullyQualifyDuplicates) {
					result.append(JavaScriptElementLabels.CONCAT_STRING);
					result.append(getContainerName(current));
				}
			}
			return result.toString();
		}

		public String getQualificationText(TypeNameMatch type) {
			StringBuffer result= new StringBuffer();
			String containerName= type.getTypeContainerName();
			if (containerName.length() > 0) {
				result.append(containerName);
				result.append(JavaScriptElementLabels.CONCAT_STRING);
			}
			result.append(getContainerName(type));
			return result.toString();
		}

		private boolean isInnerType(TypeNameMatch match) {
			return match.getTypeQualifiedName().indexOf('.') != -1;
		}

		public ImageDescriptor getImageDescriptor(Object element) {
			TypeNameMatch type= (TypeNameMatch) element;
			if (fProviderExtension != null) {
				fAdapter.setMatch(type);
				ImageDescriptor descriptor= fProviderExtension.getImageDescriptor(fAdapter);
				if (descriptor != null)
					return descriptor;
			}
			return JavaElementImageProvider.getTypeImageDescriptor(isInnerType(type), false, type.getModifiers(), false);
		}

		private String getTypeContainerName(TypeNameMatch info) {
			String result= info.getTypeContainerName();
			if (result.length() > 0)
				return result;
			return JavaUIMessages.FilteredTypesSelectionDialog_default_package;
		}

		private String getContainerName(TypeNameMatch type) {
			IPackageFragmentRoot root= type.getPackageFragmentRoot();
			if (root.isExternal()) {
				String name= root.getPath().toOSString();
				for (int i= 0; i < fInstallLocations.length; i++) {
					if (name.startsWith(fInstallLocations[i])) {
						return fVMNames[i];
					}
				}
				String lib= (String) fLib2Name.get(name);
				if (lib != null)
					return lib;
			}
			StringBuffer buf= new StringBuffer();
			JavaScriptElementLabels.getPackageFragmentRootLabel(root, JavaScriptElementLabels.ROOT_QUALIFIED | JavaScriptElementLabels.ROOT_VARIABLE, buf);
			return buf.toString();
		}
	}

	/**
	 * Filters types using pattern, scope, element kind and filter extension.
	 */
	private class TypeItemsFilter extends ItemsFilter {

		private final IJavaScriptSearchScope fScope;

		private final boolean fIsWorkspaceScope;

		private final int fElemKind;

		private final ITypeInfoFilterExtension fFilterExt;

		private final TypeInfoRequestorAdapter fAdapter= new TypeInfoRequestorAdapter();

		private SearchPattern fPackageMatcher;
		
		private boolean fMatchEverything= false;
		
		private final int fMyTypeFilterVersion= fTypeFilterVersion;
		
		private TypeSearchPattern fInitialPattern;

		/**
		 * Creates instance of TypeItemsFilter
		 * 
		 * @param scope
		 * @param elementKind
		 * @param extension
		 */
		public TypeItemsFilter(IJavaScriptSearchScope scope, int elementKind, ITypeInfoFilterExtension extension) {
			super(new TypeSearchPattern());
			fScope= scope;
			fIsWorkspaceScope= scope == null ? false : scope.equals(SearchEngine.createWorkspaceScope());
			fElemKind= elementKind;
			fFilterExt= extension;
			String initialString = ((TypeSearchPattern) patternMatcher).getInitialString();
			String stringPackage= ((TypeSearchPattern) patternMatcher).getPackagePattern();
			fInitialPattern = new TypeSearchPattern();
			fInitialPattern.setInitialPattern(initialString);
			if (stringPackage != null) {
				fPackageMatcher= new SearchPattern();
				fPackageMatcher.setPattern(stringPackage);
			} else {
				fPackageMatcher= null;
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter#isSubFilter(org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter)
		 */
		public boolean isSubFilter(ItemsFilter filter) {
			if (!super.isSubFilter(filter))
				return false;
			TypeItemsFilter typeItemsFilter= (TypeItemsFilter) filter;
			if (fScope != typeItemsFilter.getSearchScope())
				return false;
			if (fMyTypeFilterVersion != typeItemsFilter.getMyTypeFilterVersion())
				return false;
			return getPattern().indexOf('.', filter.getPattern().length()) == -1;
		}

		public boolean equalsFilter(ItemsFilter iFilter) {
			if (!super.equalsFilter(iFilter))
				return false;
			if (!(iFilter instanceof TypeItemsFilter))
				return false;
			TypeItemsFilter typeItemsFilter= (TypeItemsFilter) iFilter;
			if (fScope != typeItemsFilter.getSearchScope())
				return false;
			if (fMyTypeFilterVersion != typeItemsFilter.getMyTypeFilterVersion())
				return false;
			return true;
		}

		public int getElementKind() {
			return fElemKind;
		}

		public ITypeInfoFilterExtension getFilterExtension() {
			return fFilterExt;
		}

		public IJavaScriptSearchScope getSearchScope() {
			return fScope;
		}

		public int getMyTypeFilterVersion() {
			return fMyTypeFilterVersion;
		}
		
		public String getPackagePattern() {
			if (fPackageMatcher == null)
				return null;
			return fPackageMatcher.getPattern();
		}

		public int getPackageFlags() {
			if (fPackageMatcher == null)
				return SearchPattern.RULE_PREFIX_MATCH;

			return fPackageMatcher.getMatchRule();
		}

		public String getInitialPattern() {
			if (fInitialPattern == null)
				return null;
			return fInitialPattern.getPattern();
		}

		public boolean matchesRawNamePattern(TypeNameMatch type) {
			return Strings.startsWithIgnoreCase(type.getSimpleTypeName(), getPattern());
		}

		public boolean matchesCachedResult(TypeNameMatch type) {
			if (!(matchesPackage(type) && matchesFilterExtension(type)))
				return false;
			return matchesName(type);
		}

		public boolean matchesHistoryElement(TypeNameMatch type) {
			if (!(matchesPackage(type) && matchesModifiers(type) && matchesScope(type) && matchesFilterExtension(type)))
				return false;
			return matchesName(type);
		}

		public boolean matchesFilterExtension(TypeNameMatch type) {
			if (fFilterExt == null)
				return true;
			fAdapter.setMatch(type);
			return fFilterExt.select(fAdapter);
		}

		private boolean matchesName(TypeNameMatch type) {
			return matches(type.getSimpleTypeName());
		}

		private boolean matchesPackage(TypeNameMatch type) {
			if (fPackageMatcher == null)
				return true;
			return fPackageMatcher.matches(type.getPackageName());
		}

		private boolean matchesScope(TypeNameMatch type) {
			if (fIsWorkspaceScope)
				return true;
			return fScope.encloses(type.getType());

		}

		private boolean matchesModifiers(TypeNameMatch type) {
			if (fElemKind == IJavaScriptSearchConstants.TYPE)
				return true;
			int modifiers= type.getModifiers();
			switch (fElemKind) {
			case IJavaScriptSearchConstants.CLASS:
				return modifiers == 0;
			}
			return false;
		}
		
		/**
		 * Set filter to "match everything" mode.
		 * 
		 * @param matchEverything if <code>true</code>, {@link #matchItem(Object)} always returns true.
		 * 					If <code>false</code>, the filter is enabled.
		 */
		public void setMatchEverythingMode(boolean matchEverything) {
			this.fMatchEverything= matchEverything;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter#isConsistentItem(java.lang.Object)
		 */
		public boolean isConsistentItem(Object item) {
			return true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter#matchItem(java.lang.Object)
		 */
		public boolean matchItem(Object item) {

			if (fMatchEverything) 
				return true;
			
			TypeNameMatch type= (TypeNameMatch) item;
			if (!(matchesPackage(type) && matchesModifiers(type) && matchesScope(type) && matchesFilterExtension(type)))
				return false;
			
			return  
				fInitialPattern.matches(type.getPackageName()) ||
			    matchesName(type);
		}
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter#matchesRawNamePattern(java.lang.Object)
		 */
		public boolean matchesRawNamePattern(Object item) {
			TypeNameMatch type= (TypeNameMatch) item;
			return matchesRawNamePattern(type); 
		}
		
		public int getMatchRule() {
			return fInitialPattern.getMatchRule();
		}
	
	}

	/**
	 * Extends functionality of SearchPatterns
	 */
	private static class TypeSearchPattern extends SearchPattern {

		private String packagePattern;
		private String initialString;

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.dialogs.SearchPattern#setPattern(java.lang.String)
		 */
		public void setPattern(String stringPattern) {
			initialString = stringPattern;
			String pattern= stringPattern;
			String packPattern= null;
			int index= stringPattern.lastIndexOf("."); //$NON-NLS-1$
			if (index != -1) {
				packPattern= evaluatePackagePattern(stringPattern.substring(0, index));
				pattern= stringPattern.substring(index + 1);
				if (pattern.length() == 0)
					pattern= "**"; //$NON-NLS-1$
			}
			super.setPattern(pattern);
			packagePattern= packPattern;
		}
		
		public void setInitialPattern(String stringPattern) {
			super.setPattern(stringPattern);
		}

		/*
		 * Transforms o.e.j to o*.e*.j*
		 */
		private String evaluatePackagePattern(String s) {
			StringBuffer buf= new StringBuffer();
			boolean hasWildCard= false;
			for (int i= 0; i < s.length(); i++) {
				char ch= s.charAt(i);
				if (ch == '.') {
					if (!hasWildCard) {
						buf.append('*');
					}
					hasWildCard= false;
				} else if (ch == '*' || ch == '?') {
					hasWildCard= true;
				}
				buf.append(ch);
			}
			if (!hasWildCard) {
				buf.append('*');
			}
			return buf.toString();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.dialogs.SearchPattern#isNameCharAllowed(char)
		 */
		protected boolean isNameCharAllowed(char nameChar) {
			return super.isNameCharAllowed(nameChar);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.dialogs.SearchPattern#isPatternCharAllowed(char)
		 */
		protected boolean isPatternCharAllowed(char patternChar) {
			return super.isPatternCharAllowed(patternChar);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.dialogs.SearchPattern#isValidCamelCaseChar(char)
		 */
		protected boolean isValidCamelCaseChar(char ch) {
			return ch != '.';
		}

		/**
		 * @return the packagePattern
		 */
		public String getPackagePattern() {
			return packagePattern;
		}
		
		/**
		 * @return the initialPattern
		 */
		public String getInitialString() {
			return initialString;
		}

	}

	/**
	 * A <code>TypeSearchRequestor</code> collects matches filtered using
	 * <code>TypeItemsFilter</code>. The attached content provider is filled
	 * on the basis of the collected entries (instances of
	 * <code>TypeNameMatch</code>).
	 */
	private static class TypeSearchRequestor extends TypeNameMatchRequestor {
		private volatile boolean fStop;

		private final AbstractContentProvider fContentProvider;

		private final TypeItemsFilter fTypeItemsFilter;

		public TypeSearchRequestor(AbstractContentProvider contentProvider, TypeItemsFilter typeItemsFilter) {
			super();
			fContentProvider= contentProvider;
			fTypeItemsFilter= typeItemsFilter;
		}

		public void cancel() {
			fStop= true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.wst.jsdt.core.search.TypeNameMatchRequestor#acceptTypeNameMatch(org.eclipse.wst.jsdt.core.search.TypeNameMatch)
		 */
		public void acceptTypeNameMatch(TypeNameMatch match) {
			if (fStop)
				return;
			if (TypeFilter.isFiltered(match))
				return;
			if (CharOperation.indexOf(IInferEngine.ANONYMOUS_PREFIX, match.getSimpleTypeName().toCharArray(), false) == 0)
				return;
			if (fTypeItemsFilter.matchesFilterExtension(match))
				fContentProvider.add(match, fTypeItemsFilter);
		}

	}

	/**
	 * Compares TypeItems is used during sorting
	 */
	private static class TypeItemsComparator implements Comparator {

		private final Map fLib2Name= new HashMap();

		private final String[] fInstallLocations;

		private final String[] fVMNames;

		/**
		 * Creates new instance of TypeItemsComparator
		 */
		public TypeItemsComparator() {
			List locations= new ArrayList();
			List labels= new ArrayList();
			IVMInstallType[] installs= JavaRuntime.getVMInstallTypes();
			for (int i= 0; i < installs.length; i++) {
				processVMInstallType(installs[i], locations, labels);
			}
			fInstallLocations= (String[]) locations.toArray(new String[locations.size()]);
			fVMNames= (String[]) labels.toArray(new String[labels.size()]);
		}

		private void processVMInstallType(IVMInstallType installType, List locations, List labels) {
			if (installType != null) {
				IVMInstall[] installs= installType.getVMInstalls();
				boolean isMac= Platform.OS_MACOSX.equals(Platform.getOS());
				final String HOME_SUFFIX= "/Home"; //$NON-NLS-1$
				for (int i= 0; i < installs.length; i++) {
					String label= getFormattedLabel(installs[i].getName());
					LibraryLocation[] libLocations= installs[i].getLibraryLocations();
					if (libLocations != null) {
						processLibraryLocation(libLocations, label);
					} else {
						String filePath= installs[i].getInstallLocation().getAbsolutePath();
						// on MacOS X install locations end in an additional
						// "/Home" segment; remove it
						if (isMac && filePath.endsWith(HOME_SUFFIX))
							filePath= filePath.substring(0, filePath.length() - HOME_SUFFIX.length() + 1);
						locations.add(filePath);
						labels.add(label);
					}
				}
			}
		}

		private void processLibraryLocation(LibraryLocation[] libLocations, String label) {
			for (int l= 0; l < libLocations.length; l++) {
				LibraryLocation location= libLocations[l];
				fLib2Name.put(location.getSystemLibraryPath().toString(), label);
			}
		}

		private String getFormattedLabel(String name) {
			return MessageFormat.format(JavaUIMessages.FilteredTypesSelectionDialog_library_name_format, new Object[] { name });
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Object left, Object right) {

			TypeNameMatch leftInfo= (TypeNameMatch) left;
			TypeNameMatch rightInfo= (TypeNameMatch) right;

			int result= compareName(leftInfo.getSimpleTypeName(), rightInfo.getSimpleTypeName());
			if (result != 0)
				return result;
			result= compareTypeContainerName(leftInfo.getTypeContainerName(), rightInfo.getTypeContainerName());
			if (result != 0)
				return result;

			int leftCategory= getElementTypeCategory(leftInfo);
			int rightCategory= getElementTypeCategory(rightInfo);
			if (leftCategory < rightCategory)
				return -1;
			if (leftCategory > rightCategory)
				return +1;
			return compareContainerName(leftInfo, rightInfo);
		}

		private int compareName(String leftString, String rightString) {
			int result= leftString.compareToIgnoreCase(rightString);
			if (result != 0 || rightString.length() == 0) {
				return result;
			} else if (Strings.isLowerCase(leftString.charAt(0)) && !Strings.isLowerCase(rightString.charAt(0))) {
				return +1;
			} else if (Strings.isLowerCase(rightString.charAt(0)) && !Strings.isLowerCase(leftString.charAt(0))) {
				return -1;
			} else {
				return leftString.compareTo(rightString);
			}
		}

		private int compareTypeContainerName(String leftString, String rightString) {
			int leftLength= leftString.length();
			int rightLength= rightString.length();
			if (leftLength == 0 && rightLength > 0)
				return -1;
			if (leftLength == 0 && rightLength == 0)
				return 0;
			if (leftLength > 0 && rightLength == 0)
				return +1;
			return compareName(leftString, rightString);
		}

		private int compareContainerName(TypeNameMatch leftType, TypeNameMatch rightType) {
			return getContainerName(leftType).compareTo(getContainerName(rightType));
		}

		private String getContainerName(TypeNameMatch type) {
			IPackageFragmentRoot root= type.getPackageFragmentRoot();
			if (root.isExternal()) {
				String name= root.getPath().toOSString();
				for (int i= 0; i < fInstallLocations.length; i++) {
					if (name.startsWith(fInstallLocations[i])) {
						return fVMNames[i];
					}
				}
				String lib= (String) fLib2Name.get(name);
				if (lib != null)
					return lib;
			}
			StringBuffer buf= new StringBuffer();
			JavaScriptElementLabels.getPackageFragmentRootLabel(root, JavaScriptElementLabels.ROOT_QUALIFIED | JavaScriptElementLabels.ROOT_VARIABLE, buf);
			return buf.toString();
		}

		private int getElementTypeCategory(TypeNameMatch type) {
			try {
				if (type.getPackageFragmentRoot().getKind() == IPackageFragmentRoot.K_SOURCE)
					return 0;
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
			}
			return 1;
		}
	}

	/**
	 * Extends the <code>SelectionHistory</code>, providing support for
	 * <code>OpenTypeHistory</code>.
	 */
	protected class TypeSelectionHistory extends SelectionHistory {

		/**
		 * Creates new instance of TypeSelectionHistory
		 */

		public TypeSelectionHistory() {
			super();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.SelectionHistory#accessed(java.lang.Object)
		 */
		public synchronized void accessed(Object object) {
			super.accessed(object);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.SelectionHistory#remove(java.lang.Object)
		 */
		public synchronized boolean remove(Object element) {
			OpenTypeHistory.getInstance().remove((TypeNameMatch) element);
			return super.remove(element);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.SelectionHistory#load(org.eclipse.ui.IMemento)
		 */
		public void load(IMemento memento) {
			TypeNameMatch[] types= OpenTypeHistory.getInstance().getTypeInfos();

			for (int i= 0; i < types.length; i++) {
				TypeNameMatch type= types[i];
				accessed(type);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.SelectionHistory#save(org.eclipse.ui.IMemento)
		 */
		public void save(IMemento memento) {
			persistHistory();
		}

		/**
		 * Stores contents of the local history into persistent history
		 * container.
		 */
		private synchronized void persistHistory() {
			if (getReturnCode() == OK) {
				Object[] items= getHistoryItems();
				for (int i= 0; i < items.length; i++) {
					OpenTypeHistory.getInstance().accessed((TypeNameMatch) items[i]);
				}
			}
		}

		protected Object restoreItemFromMemento(IMemento element) {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.SelectionHistory#storeItemToMemento(java.lang.Object,
		 *      org.eclipse.ui.IMemento)
		 */
		protected void storeItemToMemento(Object item, IMemento element) {

		}

	}

}
