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
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.ui.IWorkingCopyManager;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;


/**
 * A tool bar action which toggles the presentation model of the
 * connected text editor. The editor shows either the highlight range
 * only or always the whole document.
 */
public class TogglePresentationAction extends TextEditorAction implements IPropertyChangeListener {

	private IPreferenceStore fStore;

	/**
	 * Constructs and updates the action.
	 */
	public TogglePresentationAction() {
		super(JavaEditorMessages.getBundleForConstructedKeys(), "TogglePresentation.", null, IAction.AS_CHECK_BOX); //$NON-NLS-1$
		JavaPluginImages.setToolImageDescriptors(this, "segment_edit.gif"); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.TOGGLE_PRESENTATION_ACTION);
		update();
	}

	/*
	 * @see IAction#actionPerformed
	 */
	public void run() {

		ITextEditor editor= getTextEditor();
		if (editor == null)
			return;

		IRegion remembered= editor.getHighlightRange();
		editor.resetHighlightRange();

		boolean showAll= !editor.showsHighlightRangeOnly();
		setChecked(showAll);

		editor.showHighlightRangeOnly(showAll);
		if (remembered != null)
			editor.setHighlightRange(remembered.getOffset(), remembered.getLength(), true);

		fStore.removePropertyChangeListener(this);
		fStore.setValue(PreferenceConstants.EDITOR_SHOW_SEGMENTS, showAll);
		fStore.addPropertyChangeListener(this);
	}

	/*
	 * @see TextEditorAction#update
	 */
	public void update() {
		ITextEditor editor= getTextEditor();
		boolean checked= (editor != null && editor.showsHighlightRangeOnly());
		setChecked(checked);
		if (editor instanceof CompilationUnitEditor) {
			IWorkingCopyManager manager= JavaScriptPlugin.getDefault().getWorkingCopyManager();
			setEnabled(manager.getWorkingCopy(editor.getEditorInput()) != null);
		} else if (editor instanceof ClassFileEditor) {
			IEditorInput input= editor.getEditorInput();
			IClassFile cf= null;
			if (input instanceof IClassFileEditorInput) {
				IClassFileEditorInput cfi= (IClassFileEditorInput)input;
				cf= cfi.getClassFile();
			}
			setEnabled(cf != null && cf.exists());
		} else
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
			synchronizeWithPreference(editor);

		} else if (fStore != null) {
			fStore.removePropertyChangeListener(this);
			fStore= null;
		}

		update();
	}

	/**
	 * Synchronizes the appearance of the editor with what the preference store tells him.
	 *
	 * @param editor the text editor
	 */
	private void synchronizeWithPreference(ITextEditor editor) {

		if (editor == null)
			return;

		boolean showSegments= fStore.getBoolean(PreferenceConstants.EDITOR_SHOW_SEGMENTS);
		setChecked(showSegments);

		if (editor.showsHighlightRangeOnly() != showSegments) {
			IRegion remembered= editor.getHighlightRange();
			editor.resetHighlightRange();
			editor.showHighlightRangeOnly(showSegments);
			if (remembered != null)
				editor.setHighlightRange(remembered.getOffset(), remembered.getLength(), true);
		}
	}

	/*
	 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(PreferenceConstants.EDITOR_SHOW_SEGMENTS))
			synchronizeWithPreference(getTextEditor());
	}
}
