/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.javaeditor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;
import org.eclipse.wst.jsdt.internal.ui.actions.CopyQualifiedNameAction;
import org.eclipse.wst.jsdt.internal.ui.actions.FoldingActionGroup;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.selectionactions.GoToNextPreviousMemberAction;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.selectionactions.StructureSelectionAction;
import org.eclipse.wst.jsdt.ui.actions.IJavaEditorActionDefinitionIds;

/**
 * Common base class for action contributors for Java editors.
 */
public class BasicJavaEditorActionContributor extends BasicTextEditorActionContributor {

	private List fPartListeners= new ArrayList();

	private TogglePresentationAction fTogglePresentation;
	private ToggleMarkOccurrencesAction fToggleMarkOccurrencesAction;

	private RetargetTextEditorAction fGotoMatchingBracket;
	private RetargetTextEditorAction fShowOutline;
	private RetargetTextEditorAction fOpenStructure;
	private RetargetTextEditorAction fOpenHierarchy;

	private RetargetTextEditorAction fRetargetShowInformationAction;

	private RetargetTextEditorAction fStructureSelectEnclosingAction;
	private RetargetTextEditorAction fStructureSelectNextAction;
	private RetargetTextEditorAction fStructureSelectPreviousAction;
	private RetargetTextEditorAction fStructureSelectHistoryAction;

	private RetargetTextEditorAction fGotoNextMemberAction;
	private RetargetTextEditorAction fGotoPreviousMemberAction;

	private RetargetTextEditorAction fRemoveOccurrenceAnnotationsAction;

	public BasicJavaEditorActionContributor() {
		super();

		ResourceBundle b= JavaEditorMessages.getBundleForConstructedKeys();

		fRetargetShowInformationAction= new RetargetTextEditorAction(b, "Editor.ShowInformation."); //$NON-NLS-1$
		fRetargetShowInformationAction.setActionDefinitionId(ITextEditorActionDefinitionIds.SHOW_INFORMATION);

		// actions that are "contributed" to editors, they are considered belonging to the active editor
		fTogglePresentation= new TogglePresentationAction();

		fToggleMarkOccurrencesAction= new ToggleMarkOccurrencesAction();

		fGotoMatchingBracket= new RetargetTextEditorAction(b, "GotoMatchingBracket."); //$NON-NLS-1$
		fGotoMatchingBracket.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_MATCHING_BRACKET);

		fShowOutline= new RetargetTextEditorAction(JavaEditorMessages.getBundleForConstructedKeys(), "ShowOutline."); //$NON-NLS-1$
		fShowOutline.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_OUTLINE);

		fOpenHierarchy= new RetargetTextEditorAction(JavaEditorMessages.getBundleForConstructedKeys(), "OpenHierarchy."); //$NON-NLS-1$
		fOpenHierarchy.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_HIERARCHY);

		fOpenStructure= new RetargetTextEditorAction(JavaEditorMessages.getBundleForConstructedKeys(), "OpenStructure."); //$NON-NLS-1$
		fOpenStructure.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_STRUCTURE);

		fStructureSelectEnclosingAction= new RetargetTextEditorAction(b, "StructureSelectEnclosing."); //$NON-NLS-1$
		fStructureSelectEnclosingAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_ENCLOSING);
		fStructureSelectNextAction= new RetargetTextEditorAction(b, "StructureSelectNext."); //$NON-NLS-1$
		fStructureSelectNextAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_NEXT);
		fStructureSelectPreviousAction= new RetargetTextEditorAction(b, "StructureSelectPrevious."); //$NON-NLS-1$
		fStructureSelectPreviousAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_PREVIOUS);
		fStructureSelectHistoryAction= new RetargetTextEditorAction(b, "StructureSelectHistory."); //$NON-NLS-1$
		fStructureSelectHistoryAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_LAST);

		fGotoNextMemberAction= new RetargetTextEditorAction(b, "GotoNextMember."); //$NON-NLS-1$
		fGotoNextMemberAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_NEXT_MEMBER);
		fGotoPreviousMemberAction= new RetargetTextEditorAction(b, "GotoPreviousMember."); //$NON-NLS-1$
		fGotoPreviousMemberAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_PREVIOUS_MEMBER);

		fRemoveOccurrenceAnnotationsAction= new RetargetTextEditorAction(b, "RemoveOccurrenceAnnotations."); //$NON-NLS-1$
		fRemoveOccurrenceAnnotationsAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.REMOVE_OCCURRENCE_ANNOTATIONS);
	}

	protected final void markAsPartListener(RetargetAction action) {
		fPartListeners.add(action);
	}

	/*
	 * @see IEditorActionBarContributor#init(IActionBars, IWorkbenchPage)
	 */
	public void init(IActionBars bars, IWorkbenchPage page) {
		Iterator e= fPartListeners.iterator();
		while (e.hasNext())
			page.addPartListener((RetargetAction) e.next());

		super.init(bars, page);

		bars.setGlobalActionHandler(ITextEditorActionDefinitionIds.TOGGLE_SHOW_SELECTED_ELEMENT_ONLY, fTogglePresentation);
		bars.setGlobalActionHandler(IJavaEditorActionDefinitionIds.TOGGLE_MARK_OCCURRENCES, fToggleMarkOccurrencesAction);

	}

	/*
	 * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void contributeToMenu(IMenuManager menu) {

		super.contributeToMenu(menu);

		IMenuManager editMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {

			MenuManager structureSelection= new MenuManager(JavaEditorMessages.ExpandSelectionMenu_label, "expandSelection"); //$NON-NLS-1$
			editMenu.insertAfter(ITextEditorActionConstants.SELECT_ALL, structureSelection);
			structureSelection.add(fStructureSelectEnclosingAction);
			structureSelection.add(fStructureSelectNextAction);
			structureSelection.add(fStructureSelectPreviousAction);
			structureSelection.add(fStructureSelectHistoryAction);

			editMenu.appendToGroup(ITextEditorActionConstants.GROUP_INFORMATION, fRetargetShowInformationAction);
		}

		IMenuManager navigateMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_NAVIGATE);
		if (navigateMenu != null) {
			navigateMenu.appendToGroup(IWorkbenchActionConstants.SHOW_EXT, fShowOutline);
			navigateMenu.appendToGroup(IWorkbenchActionConstants.SHOW_EXT, fOpenHierarchy);
		}

		IMenuManager gotoMenu= menu.findMenuUsingPath("navigate/goTo"); //$NON-NLS-1$
		if (gotoMenu != null) {
			gotoMenu.add(new Separator("additions2"));  //$NON-NLS-1$
			gotoMenu.appendToGroup("additions2", fGotoPreviousMemberAction); //$NON-NLS-1$
			gotoMenu.appendToGroup("additions2", fGotoNextMemberAction); //$NON-NLS-1$
			gotoMenu.appendToGroup("additions2", fGotoMatchingBracket); //$NON-NLS-1$
		}
	}

	/*
	 * @see EditorActionBarContributor#setActiveEditor(IEditorPart)
	 */
	public void setActiveEditor(IEditorPart part) {

		super.setActiveEditor(part);

		ITextEditor textEditor= null;
		if (part instanceof ITextEditor)
			textEditor= (ITextEditor)part;

		fTogglePresentation.setEditor(textEditor);
		fToggleMarkOccurrencesAction.setEditor(textEditor);

		fGotoMatchingBracket.setAction(getAction(textEditor, GotoMatchingBracketAction.GOTO_MATCHING_BRACKET));
		fShowOutline.setAction(getAction(textEditor, IJavaEditorActionDefinitionIds.SHOW_OUTLINE));
		fOpenHierarchy.setAction(getAction(textEditor, IJavaEditorActionDefinitionIds.OPEN_HIERARCHY));
		fOpenStructure.setAction(getAction(textEditor, IJavaEditorActionDefinitionIds.OPEN_STRUCTURE));

		fStructureSelectEnclosingAction.setAction(getAction(textEditor, StructureSelectionAction.ENCLOSING));
		fStructureSelectNextAction.setAction(getAction(textEditor, StructureSelectionAction.NEXT));
		fStructureSelectPreviousAction.setAction(getAction(textEditor, StructureSelectionAction.PREVIOUS));
		fStructureSelectHistoryAction.setAction(getAction(textEditor, StructureSelectionAction.HISTORY));

		fGotoNextMemberAction.setAction(getAction(textEditor, GoToNextPreviousMemberAction.NEXT_MEMBER));
		fGotoPreviousMemberAction.setAction(getAction(textEditor, GoToNextPreviousMemberAction.PREVIOUS_MEMBER));

		fRemoveOccurrenceAnnotationsAction.setAction(getAction(textEditor, "RemoveOccurrenceAnnotations")); //$NON-NLS-1$
		fRetargetShowInformationAction.setAction(getAction(textEditor, ITextEditorActionConstants.SHOW_INFORMATION));		

		if (part instanceof JavaEditor) {
			JavaEditor javaEditor= (JavaEditor) part;
			javaEditor.getActionGroup().fillActionBars(getActionBars());
			FoldingActionGroup foldingActions= javaEditor.getFoldingActionGroup();
			if (foldingActions != null)
				foldingActions.updateActionBars();
		}

		IActionBars actionBars= getActionBars();
		IStatusLineManager manager= actionBars.getStatusLineManager();
		manager.setMessage(null);
		manager.setErrorMessage(null);
		
		/** The global actions to be connected with editor actions */
		IAction action= getAction(textEditor, ITextEditorActionConstants.NEXT);
		actionBars.setGlobalActionHandler(ITextEditorActionDefinitionIds.GOTO_NEXT_ANNOTATION, action);
		actionBars.setGlobalActionHandler(ITextEditorActionConstants.NEXT, action);
		action= getAction(textEditor, ITextEditorActionConstants.PREVIOUS);
		actionBars.setGlobalActionHandler(ITextEditorActionDefinitionIds.GOTO_PREVIOUS_ANNOTATION, action);
		actionBars.setGlobalActionHandler(ITextEditorActionConstants.PREVIOUS, action);
		action= getAction(textEditor, IJavaEditorActionConstants.COPY_QUALIFIED_NAME);
		actionBars.setGlobalActionHandler(CopyQualifiedNameAction.ACTION_HANDLER_ID, action);		
	}

	/*
	 * @see IEditorActionBarContributor#dispose()
	 */
	public void dispose() {

		Iterator e= fPartListeners.iterator();
		while (e.hasNext())
			getPage().removePartListener((RetargetAction) e.next());
		fPartListeners.clear();

		setActiveEditor(null);
		super.dispose();
	}
}
