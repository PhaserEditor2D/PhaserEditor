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
package org.eclipse.wst.jsdt.internal.ui.javaeditor;



import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.actions.IJavaEditorActionDefinitionIds;


/**
 * A toolbar action which toggles the enabling state of the
 * editor's text hover.
 */
public class ToggleTextHoverAction extends TextEditorAction implements IPropertyChangeListener {


	private IPreferenceStore fStore;

	/**
	 * Constructs and updates the action.
	 */
	public ToggleTextHoverAction() {
		super(JavaEditorMessages.getBundleForConstructedKeys(), "ToggleTextHover.", null); //$NON-NLS-1$
		JavaPluginImages.setToolImageDescriptors(this, "jdoc_hover_edit.gif"); //$NON-NLS-1$
		setActionDefinitionId(IJavaEditorActionDefinitionIds.TOGGLE_TEXT_HOVER);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.TOGGLE_TEXTHOVER_ACTION);
		update();
	}

	/*
	 * @see IAction#actionPerformed
	 */
	public void run() {
		ITextEditor editor= getTextEditor();
		if (editor == null)
			return;

		boolean showHover= !fStore.getBoolean(PreferenceConstants.EDITOR_SHOW_HOVER);
		setChecked(showHover);

		fStore.removePropertyChangeListener(this);
		fStore.setValue(PreferenceConstants.EDITOR_SHOW_HOVER, showHover);
		fStore.addPropertyChangeListener(this);
	}

	/*
	 * @see TextEditorAction#update
	 */
	public void update() {
		boolean showHover= fStore != null &&  fStore.getBoolean(PreferenceConstants.EDITOR_SHOW_HOVER);
		setChecked(showHover);
		setEnabled(getTextEditor() != null);
	}

	/*
	 * @see TextEditorAction#setEditor(ITextEditor)
	 */
	public void setEditor(ITextEditor editor) {
		super.setEditor(editor);
		if (editor != null) {
			if (fStore == null) {
				fStore= JavaScriptPlugin.getDefault().getPreferenceStore();
				fStore.addPropertyChangeListener(this);
			}
		} else if (fStore != null) {
			fStore.removePropertyChangeListener(this);
			fStore= null;
		}
		update();
	}

	/*
	 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(PreferenceConstants.EDITOR_SHOW_HOVER))
			update();
	}
}
