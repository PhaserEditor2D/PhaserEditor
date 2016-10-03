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
package org.eclipse.wst.jsdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IEditingSupport;
import org.eclipse.jface.text.IEditingSupportRegistry;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.search.TypeNameMatch;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.OrganizeImportsOperation;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.OrganizeImportsOperation.IChooseImportQuery;
import org.eclipse.wst.jsdt.internal.corext.util.History;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.corext.util.QualifiedTypeNameHistory;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionUtil;
import org.eclipse.wst.jsdt.internal.ui.actions.MultiOrganizeImportAction;
import org.eclipse.wst.jsdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.wst.jsdt.internal.ui.dialogs.MultiElementListSelectionDialog;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.wst.jsdt.internal.ui.util.ElementValidator;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.util.TypeNameMatchLabelProvider;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;

import com.ibm.icu.text.Collator;

/**
 * Organizes the imports of a compilation unit.
 * <p>
 * The action is applicable to selections containing elements of
 * type <code>IJavaScriptUnit</code> or <code>IPackage
 * </code>.
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
public class OrganizeImportsAction extends SelectionDispatchAction {

	private static final OrganizeImportComparator ORGANIZE_IMPORT_COMPARATOR= new OrganizeImportComparator();
	
	private JavaEditor fEditor;
	/** <code>true</code> if the query dialog is showing. */
	private boolean fIsQueryShowing= false;
	private final MultiOrganizeImportAction fCleanUpDelegate;

	/* (non-Javadoc)
	 * Class implements IObjectActionDelegate
	 */
	public static class ObjectDelegate implements IObjectActionDelegate {
		private OrganizeImportsAction fAction;
		public void setActivePart(IAction action, IWorkbenchPart targetPart) {
			fAction= new OrganizeImportsAction(targetPart.getSite());
		}
		public void run(IAction action) {
			fAction.run();
		}
		public void selectionChanged(IAction action, ISelection selection) {
			if (fAction == null)
				action.setEnabled(false);
		}
	}
	
	private static final class OrganizeImportComparator implements Comparator {
		
		public int compare(Object o1, Object o2) {
			if (((String)o1).equals(o2))
				return 0;
			
			History history= QualifiedTypeNameHistory.getDefault();
			
			int pos1= history.getPosition(o1);
			int pos2= history.getPosition(o2);
			
			if (pos1 == pos2)
				return Collator.getInstance().compare(o1, o2);
			
			if (pos1 > pos2) {
				return -1;
			} else {
				return 1;
			}
		}
		
	}

	/**
	 * Creates a new <code>OrganizeImportsAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public OrganizeImportsAction(IWorkbenchSite site) {
		super(site);
		
		fCleanUpDelegate= new MultiOrganizeImportAction(site);
		
		setText(ActionMessages.OrganizeImportsAction_label); 
		setToolTipText(ActionMessages.OrganizeImportsAction_tooltip); 
		setDescription(ActionMessages.OrganizeImportsAction_description);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.ORGANIZE_IMPORTS_ACTION);					
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the JavaScript editor
	 */
	public OrganizeImportsAction(JavaEditor editor) {
		super(editor.getEditorSite());
		
		fEditor= editor;
		fCleanUpDelegate= new MultiOrganizeImportAction(editor);
		
		setText(ActionMessages.OrganizeImportsAction_label); 
		setToolTipText(ActionMessages.OrganizeImportsAction_tooltip); 
		setDescription(ActionMessages.OrganizeImportsAction_description);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.ORGANIZE_IMPORTS_ACTION);

		setEnabled(fCleanUpDelegate.isEnabled());
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(ITextSelection selection) {
		fCleanUpDelegate.selectionChanged(selection);
		setEnabled(fCleanUpDelegate.isEnabled());
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(IStructuredSelection selection) {
		fCleanUpDelegate.selectionChanged(selection);
		setEnabled(fCleanUpDelegate.isEnabled());
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(ITextSelection selection) {
		IJavaScriptUnit cu= getCompilationUnit(fEditor);
		if (cu != null) {
			run(cu);
		}
	}

	private static IJavaScriptUnit getCompilationUnit(JavaEditor editor) {
		IJavaScriptElement element= JavaScriptUI.getEditorInputJavaElement(editor.getEditorInput());
		if (!(element instanceof IJavaScriptUnit))
			return null;
		
		return (IJavaScriptUnit)element;
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(IStructuredSelection selection) {
		IJavaScriptUnit[] cus= fCleanUpDelegate.getCompilationUnits(selection);
		if (cus.length == 0) {
			MessageDialog.openInformation(getShell(), ActionMessages.OrganizeImportsAction_EmptySelection_title, ActionMessages.OrganizeImportsAction_EmptySelection_description);
		} else if (cus.length == 1) {
			run(cus[0]);
		} else {
			fCleanUpDelegate.run(selection);
		}
	}
	
	/**
	 * Perform organize import on multiple compilation units. No editors are opened.
	 * @param cus The compilation units to run on
	 */
	public void runOnMultiple(final IJavaScriptUnit[] cus) {
		if (cus.length == 0)
			return;
		
		fCleanUpDelegate.run(new StructuredSelection(cus));	
	}

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 * @param cu The compilation unit to process
	 */
	public void run(IJavaScriptUnit cu) {
		if (!ElementValidator.check(cu, getShell(), ActionMessages.OrganizeImportsAction_error_title, fEditor != null)) 
			return;
		if (!ActionUtil.isEditable(fEditor, getShell(), cu))
			return;
		
		IEditingSupport helper= createViewerHelper();
		try {
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(cu.getJavaScriptProject());
			
			if (fEditor == null && EditorUtility.isOpenInEditor(cu) == null) {
				IEditorPart editor= JavaScriptUI.openInEditor(cu);
				if (editor instanceof JavaEditor) {
					fEditor= (JavaEditor) editor;
				}			
			}
			
			JavaScriptUnit astRoot= JavaScriptPlugin.getDefault().getASTProvider().getAST(cu, ASTProvider.WAIT_ACTIVE_ONLY, null);
			
			OrganizeImportsOperation op= new OrganizeImportsOperation(cu, astRoot, settings.importIgnoreLowercase, !cu.isWorkingCopy(), true, createChooseImportQuery());
		
			IRewriteTarget target= null;
			if (fEditor != null) {
				target= (IRewriteTarget) fEditor.getAdapter(IRewriteTarget.class);
				if (target != null) {
					target.beginCompoundChange();
				}
			}
			
			IProgressService progressService= PlatformUI.getWorkbench().getProgressService();
			IRunnableContext context= getSite().getWorkbenchWindow();
			if (context == null) {
				context= progressService;
			}
			try {
				registerHelper(helper);
				progressService.runInUI(context, new WorkbenchRunnableAdapter(op, op.getScheduleRule()), op.getScheduleRule());
				IProblem parseError= op.getParseError();
				if (parseError != null) {
					String message= Messages.format(ActionMessages.OrganizeImportsAction_single_error_parse, parseError.getMessage()); 
					MessageDialog.openInformation(getShell(), ActionMessages.OrganizeImportsAction_error_title, message); 
					if (fEditor != null && parseError.getSourceStart() != -1) {
						fEditor.selectAndReveal(parseError.getSourceStart(), parseError.getSourceEnd() - parseError.getSourceStart() + 1);
					}
				} else {
					if (fEditor != null) {
						setStatusBarMessage(getOrganizeInfo(op));
					}
				}
			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, getShell(), ActionMessages.OrganizeImportsAction_error_title, ActionMessages.OrganizeImportsAction_error_message); 
			} catch (InterruptedException e) {
			} finally {
				deregisterHelper(helper);
				if (target != null) {
					target.endCompoundChange();
				}
			}
		} catch (CoreException e) {	
			ExceptionHandler.handle(e, getShell(), ActionMessages.OrganizeImportsAction_error_title, ActionMessages.OrganizeImportsAction_error_message); 
		}
	}
	
	private String getOrganizeInfo(OrganizeImportsOperation op) {
		int nImportsAdded= op.getNumberOfImportsAdded();
		if (nImportsAdded >= 0) {
			return Messages.format(ActionMessages.OrganizeImportsAction_summary_added, String.valueOf(nImportsAdded)); 
		} else {
			return Messages.format(ActionMessages.OrganizeImportsAction_summary_removed, String.valueOf(-nImportsAdded)); 
		}
	}
		
	private IChooseImportQuery createChooseImportQuery() {
		return new IChooseImportQuery() {
			public TypeNameMatch[] chooseImports(TypeNameMatch[][] openChoices, ISourceRange[] ranges) {
				return doChooseImports(openChoices, ranges);
			}
		};
	}
	
	private TypeNameMatch[] doChooseImports(TypeNameMatch[][] openChoices, final ISourceRange[] ranges) {
		// remember selection
		ISelection sel= fEditor != null ? fEditor.getSelectionProvider().getSelection() : null;
		TypeNameMatch[] result= null;
		ILabelProvider labelProvider= new TypeNameMatchLabelProvider(TypeNameMatchLabelProvider.SHOW_FULLYQUALIFIED);
		
		MultiElementListSelectionDialog dialog= new MultiElementListSelectionDialog(getShell(), labelProvider) {
			protected void handleSelectionChanged() {
				super.handleSelectionChanged();
				// show choices in editor
				doListSelectionChanged(getCurrentPage(), ranges);
			}
		};
		fIsQueryShowing= true;
		dialog.setTitle(ActionMessages.OrganizeImportsAction_selectiondialog_title); 
		dialog.setMessage(ActionMessages.OrganizeImportsAction_selectiondialog_message);
		dialog.setElements(openChoices);
		dialog.setComparator(ORGANIZE_IMPORT_COMPARATOR);
		if (dialog.open() == Window.OK) {
			Object[] res= dialog.getResult();			
			result= new TypeNameMatch[res.length];
			for (int i= 0; i < res.length; i++) {
				Object[] array= (Object[]) res[i];
				if (array.length > 0) {
					result[i]= (TypeNameMatch) array[0];
					QualifiedTypeNameHistory.remember(result[i].getFullyQualifiedName());
				}
			}
		}
		// restore selection
		if (sel instanceof ITextSelection) {
			ITextSelection textSelection= (ITextSelection) sel;
			fEditor.selectAndReveal(textSelection.getOffset(), textSelection.getLength());
		}
		fIsQueryShowing= false;
		return result;
	}
	
	private void doListSelectionChanged(int page, ISourceRange[] ranges) {
		if (fEditor != null && ranges != null && page >= 0 && page < ranges.length) {
			ISourceRange range= ranges[page];
			fEditor.selectAndReveal(range.getOffset(), range.getLength());
		}
	}
	
	private void setStatusBarMessage(String message) {
		IStatusLineManager manager= fEditor.getEditorSite().getActionBars().getStatusLineManager();
		manager.setMessage(message);
	}
	
	private IEditingSupport createViewerHelper() {
		return new IEditingSupport() {
			public boolean isOriginator(DocumentEvent event, IRegion subjectRegion) {
				return true; // assume true, since we only register while we are active
			}
			public boolean ownsFocusShell() {
				return fIsQueryShowing;
			}
			
		};
	}
	
	private void registerHelper(IEditingSupport helper) {
		if (fEditor == null)
			return;
		ISourceViewer viewer= fEditor.getViewer();
		if (viewer instanceof IEditingSupportRegistry) {
			IEditingSupportRegistry registry= (IEditingSupportRegistry) viewer;
			registry.register(helper);
		}
	}

	private void deregisterHelper(IEditingSupport helper) {
		if (fEditor == null)
			return;
		ISourceViewer viewer= fEditor.getViewer();
		if (viewer instanceof IEditingSupportRegistry) {
			IEditingSupportRegistry registry= (IEditingSupportRegistry) viewer;
			registry.unregister(helper);
		}
	}
}
