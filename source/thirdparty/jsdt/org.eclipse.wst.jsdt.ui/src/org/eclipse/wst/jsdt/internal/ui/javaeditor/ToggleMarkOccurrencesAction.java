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


import org.eclipse.jface.action.IAction;
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


/**
 * A toolbar action which toggles the {@linkplain org.eclipse.wst.jsdt.ui.PreferenceConstants#EDITOR_MARK_OCCURRENCES mark occurrences preference}.
 *
 * 
 */
public class ToggleMarkOccurrencesAction extends TextEditorAction implements IPropertyChangeListener {

	private IPreferenceStore fStore;

	/**
	 * Constructs and updates the action.
	 */
	public ToggleMarkOccurrencesAction() {
		super(JavaEditorMessages.getBundleForConstructedKeys(), "ToggleMarkOccurrencesAction.", null, IAction.AS_CHECK_BOX); //$NON-NLS-1$
		JavaPluginImages.setToolImageDescriptors(this, "mark_occurrences.gif"); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.TOGGLE_MARK_OCCURRENCES_ACTION);
		update();
	}

	/*
	 * @see IAction#actionPerformed
	 */
	public void run() {
		fStore.setValue(PreferenceConstants.EDITOR_MARK_OCCURRENCES, isChecked());
	}

	/*
	 * @see TextEditorAction#update
	 */
	public void update() {
		ITextEditor editor= getTextEditor();

		boolean checked= false;
		if (editor instanceof JavaEditor)
			checked= ((JavaEditor)editor).isMarkingOccurrences();

		setChecked(checked);
		setEnabled(editor != null);
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
		if (event.getProperty().equals(PreferenceConstants.EDITOR_MARK_OCCURRENCES))
			setChecked(Boolean.valueOf(event.getNewValue().toString()).booleanValue());
	}
}
