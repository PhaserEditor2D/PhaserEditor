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
package org.eclipse.wst.jsdt.internal.ui.refactoring;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.ui.refactoring.ChangePreviewViewerInput;
import org.eclipse.ltk.ui.refactoring.IChangePreviewViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.wst.jsdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.util.ViewerPane;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.text.JavaScriptSourceViewerConfiguration;
import org.eclipse.wst.jsdt.ui.text.JavaScriptTextTools;

/**
 * Change preview viewer for <code>CreateTextFileChange</code> objects.
 */
public final class CreateTextFileChangePreviewViewer implements IChangePreviewViewer {

	private static class CreateTextFilePreviewer extends ViewerPane {

		private ImageDescriptor fDescriptor;

		private Image fImage;

		public CreateTextFilePreviewer(Composite parent, int style) {
			super(parent, style);
		}

		public void setImageDescriptor(ImageDescriptor imageDescriptor) {
			fDescriptor= imageDescriptor;
		}

		public void setText(String text) {
			super.setText(text);
			Image current= null;
			if (fDescriptor != null) {
				current= fImage;
				fImage= fDescriptor.createImage();
			} else {
				current= fImage;
				fImage= null;
			}
			setImage(fImage);
			if (current != null) {
				current.dispose();
			}
		}

	}

	private CreateTextFilePreviewer fPane;

	private SourceViewer fSourceViewer;

	/**
	 * {@inheritDoc}
	 */
	public void createControl(Composite parent) {
		fPane= new CreateTextFilePreviewer(parent, SWT.BORDER | SWT.FLAT);
		Dialog.applyDialogFont(fPane);

		fSourceViewer= new SourceViewer(fPane, null, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		fSourceViewer.setEditable(false);
		fSourceViewer.getControl().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
		fPane.setContent(fSourceViewer.getControl());
	}

	/**
	 * {@inheritDoc}
	 */
	public Control getControl() {
		return fPane;
	}

	public void refresh() {
		fSourceViewer.refresh();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setInput(ChangePreviewViewerInput input) {
		Change change= input.getChange();
		if (change != null) {
			Object element= change.getModifiedElement();
			if (element instanceof IAdaptable) {
				IAdaptable adaptable= (IAdaptable) element;
				IWorkbenchAdapter workbenchAdapter= (IWorkbenchAdapter) adaptable.getAdapter(IWorkbenchAdapter.class);
				if (workbenchAdapter != null) {
					fPane.setImageDescriptor(workbenchAdapter.getImageDescriptor(element));
				} else {
					fPane.setImageDescriptor(null);
				}
			} else {
				fPane.setImageDescriptor(null);
			}
		}
		if (!(change instanceof CreateTextFileChange)) {
			fSourceViewer.setInput(null);
			fPane.setText(""); //$NON-NLS-1$
			return;
		}
		CreateTextFileChange textFileChange= (CreateTextFileChange) change;
		fPane.setText(textFileChange.getName());
		IDocument document= new Document(textFileChange.getPreview());
		// This is a temporary work around until we get the
		// source viewer registry.
		fSourceViewer.unconfigure();
		String textType= textFileChange.getTextType();
		JavaScriptTextTools textTools= JavaScriptPlugin.getDefault().getJavaTextTools();
		IPreferenceStore store= JavaScriptPlugin.getDefault().getCombinedPreferenceStore();
		if ("java".equals(textType)) { //$NON-NLS-1$
			textTools.setupJavaDocumentPartitioner(document);
			fSourceViewer.configure(new JavaScriptSourceViewerConfiguration(textTools.getColorManager(), store, null, null));
		} 
//		else if ("properties".equals(textType)) { //$NON-NLS-1$
//			PropertiesFileDocumentSetupParticipant.setupDocument(document);
//			fSourceViewer.configure(new PropertiesFileSourceViewerConfiguration(textTools.getColorManager(), store, null, IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING));
//		}
		else {
			fSourceViewer.configure(new SourceViewerConfiguration());
		}
		fSourceViewer.setInput(document);
	}
}
