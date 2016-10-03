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
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IEditingSupport;
import org.eclipse.jface.text.IEditingSupportRegistry;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.text.correction.AssistContext;
import org.eclipse.wst.jsdt.internal.ui.text.correction.QuickTemplateProcessor;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.wst.jsdt.ui.actions.SurroundWithTryCatchAction;
import org.eclipse.wst.jsdt.ui.text.java.IInvocationContext;
import org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposal;

public class SurroundWithTemplateMenuAction implements IWorkbenchWindowPulldownDelegate2 {
	
	//TODO make api
	public static final String SURROUND_WITH_QUICK_MENU_ACTION_ID= "org.eclipse.wst.jsdt.ui.edit.text.java.surround.with.quickMenu";  //$NON-NLS-1$
	
	private static final String JAVA_TEMPLATE_PREFERENCE_PAGE_ID= "org.eclipse.wst.jsdt.ui.preferences.JavaTemplatePreferencePage"; //$NON-NLS-1$
	
	private static final String TEMPLATE_GROUP= "templateGroup"; //$NON-NLS-1$
	
	private static final String CONFIG_GROUP= "configGroup"; //$NON-NLS-1$
	
	private static class ConfigureTemplatesAction extends Action {

		public ConfigureTemplatesAction() {
			super(ActionMessages.SurroundWithTemplateMenuAction_ConfigureTemplatesActionName);
		}
		
		/**
		 * {@inheritDoc}
		 */
		public void run() {
			PreferencesUtil.createPreferenceDialogOn(getShell(), JAVA_TEMPLATE_PREFERENCE_PAGE_ID, new String[] {JAVA_TEMPLATE_PREFERENCE_PAGE_ID}, null).open();
		}

		private Shell getShell() {
			return JavaScriptPlugin.getActiveWorkbenchWindow().getShell();
		}
	}
	
	private static Action NONE_APPLICABLE_ACTION= new Action(ActionMessages.SurroundWithTemplateMenuAction_NoneApplicable) {
		public void run() {
			//Do nothing
		}
		public boolean isEnabled() {
			return false;
		}
	};

	private Menu fMenu;
	private IPartService fPartService;
	private IPartListener fPartListener= new IPartListener() {

		public void partActivated(IWorkbenchPart part) {
		}

		public void partBroughtToTop(IWorkbenchPart part) {
		}

		public void partClosed(IWorkbenchPart part) {
		}

		public void partDeactivated(IWorkbenchPart part) {
			disposeMenuItems();
		}

		public void partOpened(IWorkbenchPart part) {
		}
		
	};

	protected void disposeMenuItems() {
		if (fMenu == null || fMenu.isDisposed()) {
			return;
		}
		MenuItem[] items = fMenu.getItems();
		for (int i=0; i < items.length; i++) {
			MenuItem menuItem= items[i];
			if (!menuItem.isDisposed()) {
				menuItem.dispose();
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Menu getMenu(Menu parent) {
		setMenu(new Menu(parent));
		fillMenu(fMenu);
		initMenu();
		return fMenu;
	}

	/**
	 * {@inheritDoc}
	 */
	public Menu getMenu(Control parent) {
		setMenu(new Menu(parent));
		fillMenu(fMenu);
		initMenu();
		return fMenu;
	}

	public static void fillMenu(IMenuManager menu, CompilationUnitEditor editor) {
		IAction[] actions= getTemplateActions(editor);
//		surroundWithTryCatchAction.update(editor.getSelectionProvider().getSelection());
		
		if (actions == null /* && !surroundWithTryCatchAction.isEnabled()*/) {
			menu.add(NONE_APPLICABLE_ACTION);
		} else {		
//    		menu.add(surroundWithTryCatchAction);
    		menu.add(new Separator(TEMPLATE_GROUP));
    		
    		if (actions == null) {
    			menu.add(NONE_APPLICABLE_ACTION);
    		} else {
    			for (int i= 0; i < actions.length; i++) {
    				menu.add(actions[i]);
    			}
    		}		
		}
		
		menu.add(new Separator(CONFIG_GROUP));
		menu.add(new ConfigureTemplatesAction());
	}

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		if (fPartService != null) {
			fPartService.removePartListener(fPartListener);
			fPartService= null;
		}
		setMenu(null);
	}

	/**
	 * {@inheritDoc}
	 */
	public void init(IWorkbenchWindow window) {
		if (fPartService != null) {
			fPartService.removePartListener(fPartListener);
			fPartService= null;
		}
		
		if (window != null) {
			IPartService partService= window.getPartService();
			if (partService != null) {
				fPartService= partService;
				partService.addPartListener(fPartListener);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void run(IAction action) {
		IWorkbenchPart activePart= JavaScriptPlugin.getActivePage().getActivePart();
		if (!(activePart instanceof CompilationUnitEditor))
			return;
		
		final CompilationUnitEditor editor= (CompilationUnitEditor)activePart;
		
		(new JDTQuickMenuAction(editor, SURROUND_WITH_QUICK_MENU_ACTION_ID) {
			protected void fillMenu(IMenuManager menu) {
//				SurroundWithTryCatchAction surroundWithTryCatch= createSurroundWithTryCatchAction(editor);
				SurroundWithTemplateMenuAction.fillMenu(menu, editor);
			}
		}).run();
	}

	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		// Default do nothing
	}
	
	/**
	 * The menu to show in the workbench menu
	 */
	protected void fillMenu(Menu menu) {
		
		IWorkbenchPart activePart= JavaScriptPlugin.getActivePage().getActivePart();
		if (!(activePart instanceof CompilationUnitEditor)) {
			ActionContributionItem item= new ActionContributionItem(NONE_APPLICABLE_ACTION);
			item.fill(menu, -1);
			return;
		}
		
		CompilationUnitEditor editor= (CompilationUnitEditor)activePart;
		
		IAction[] actions= getTemplateActions(editor);
		
		SurroundWithTryCatchAction surroundAction= createSurroundWithTryCatchAction(editor);
		ActionContributionItem surroundItem= new ActionContributionItem(surroundAction);
		surroundItem.fill(menu, -1);
		
		Separator templateGroup= new Separator(TEMPLATE_GROUP);
		templateGroup.fill(menu, -1);
		
		if (actions == null || actions.length == 0) {
			ActionContributionItem item= new ActionContributionItem(NONE_APPLICABLE_ACTION);
			item.fill(menu, -1);
		} else {
			for (int i= 0; i < actions.length; i++) {
				ActionContributionItem item= new ActionContributionItem(actions[i]);
				item.fill(menu, -1);
			}
		}
		
		Separator configGroup= new Separator(CONFIG_GROUP);
		configGroup.fill(menu, -1);
		
		ActionContributionItem configAction= new ActionContributionItem(new ConfigureTemplatesAction());
		configAction.fill(menu, -1);
		
	}

	private static SurroundWithTryCatchAction createSurroundWithTryCatchAction(CompilationUnitEditor editor) {
		SurroundWithTryCatchAction result= new SurroundWithTryCatchAction(editor);
		result.setText(ActionMessages.SurroundWithTemplateMenuAction_SurroundWithTryCatchActionName);
		result.setActionDefinitionId(IJavaEditorActionDefinitionIds.SURROUND_WITH_TRY_CATCH);
		editor.setAction("SurroundWithTryCatch", result); //$NON-NLS-1$		
		return result;
	}

	protected void initMenu() {
		fMenu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent e) {
				Menu m = (Menu)e.widget;
				MenuItem[] items = m.getItems();
				for (int i=0; i < items.length; i++) {
					items[i].dispose();
				}
				fillMenu(m);
			}
		});
	}
	
	private void setMenu(Menu menu) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = menu;
	}
	
	private static IAction[] getTemplateActions(JavaEditor editor) {
		ISelectionProvider selectionProvider= editor.getSelectionProvider();
		if (selectionProvider == null)
			return null;
		
		ISelection selection= selectionProvider.getSelection();
		if (!(selection instanceof ITextSelection))
			return null;
		
		ITextSelection textSelection= (ITextSelection)selection;
		if (textSelection.getLength() == 0)
			return null;
		
		IJavaScriptUnit cu= JavaScriptUI.getWorkingCopyManager().getWorkingCopy(editor.getEditorInput());
		if (cu == null)
			return null;
		
		QuickTemplateProcessor quickTemplateProcessor= new QuickTemplateProcessor();
		IInvocationContext context= new AssistContext(cu, textSelection.getOffset(), textSelection.getLength());
		
		try {
			IJavaCompletionProposal[] proposals= quickTemplateProcessor.getAssists(context, null);
			if (proposals == null || proposals.length == 0)
				return null;
			
			return getActionsFromProposals(proposals, context.getSelectionOffset(), editor.getViewer());
		} catch (CoreException e) {
			JavaScriptPlugin.log(e);
		}
		return null;
	}

	private static IAction[] getActionsFromProposals(IJavaCompletionProposal[] proposals, final int offset, final ITextViewer viewer) {
		List result= new ArrayList();
		
		for (int i= 0, j= 1; i < proposals.length; i++) {
			if (proposals[i] instanceof ICompletionProposalExtension2) {
				final IJavaCompletionProposal proposal= proposals[i];
				
				StringBuffer actionName= new StringBuffer();
				if (j<10) {
					actionName.append('&').append(j).append(' ');
				}
				actionName.append(proposals[i].getDisplayString());
				
				Action action= new Action(actionName.toString()) {
					/**
					 * {@inheritDoc} 
					 */
					public void run() {
						applyProposal(proposal, viewer, (char)0, 0, offset);
					}					
				};
				
				result.add(action);
				j++;
			}
		}
		if (result.size() == 0)
			return null;
		
		return (IAction[])result.toArray(new IAction[result.size()]);
	}
	
	private static void applyProposal(ICompletionProposal proposal, ITextViewer viewer, char trigger, int stateMask, final int offset) {
		Assert.isTrue(proposal instanceof ICompletionProposalExtension2);
		
		IRewriteTarget target= null;
		IEditingSupportRegistry registry= null;
		IEditingSupport helper= new IEditingSupport() {

			public boolean isOriginator(DocumentEvent event, IRegion focus) {
				return focus.getOffset() <= offset && focus.getOffset() + focus.getLength() >= offset;
			}

			public boolean ownsFocusShell() {
				return false;
			}

		};
		
		try {
			IDocument document= viewer.getDocument();

			if (viewer instanceof ITextViewerExtension) {
				ITextViewerExtension extension= (ITextViewerExtension) viewer;
				target= extension.getRewriteTarget();
			}

			if (target != null)
				target.beginCompoundChange();

			if (viewer instanceof IEditingSupportRegistry) {
				registry= (IEditingSupportRegistry) viewer;
				registry.register(helper);
			}

			((ICompletionProposalExtension2)proposal).apply(viewer, trigger, stateMask, offset);
			
			Point selection= proposal.getSelection(document);
			if (selection != null) {
				viewer.setSelectedRange(selection.x, selection.y);
				viewer.revealRange(selection.x, selection.y);
			}
		} finally {
			if (target != null)
				target.endCompoundChange();

			if (registry != null)
				registry.unregister(helper);
		}
	}
}
