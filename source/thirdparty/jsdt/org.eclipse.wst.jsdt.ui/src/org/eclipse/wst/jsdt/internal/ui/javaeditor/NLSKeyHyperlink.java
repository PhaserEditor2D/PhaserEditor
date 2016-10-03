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

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.internal.corext.refactoring.nls.AccessorClassReference;
import org.eclipse.wst.jsdt.internal.corext.refactoring.nls.NLSHintHelper;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;


/**
 * NLS key hyperlink.
 *
 * 
 */
public class NLSKeyHyperlink implements IHyperlink {

	private IRegion fRegion;
	private AccessorClassReference fAccessorClassReference;
	private IEditorPart fEditor;
	private final String fKeyName;


	/**
	 * Creates a new NLS key hyperlink.
	 *
	 * @param region
	 * @param keyName
	 * @param ref
	 * @param editor the editor which contains the hyperlink
	 */
	public NLSKeyHyperlink(IRegion region, String keyName, AccessorClassReference ref, IEditorPart editor) {
		Assert.isNotNull(region);
		Assert.isNotNull(keyName);
		Assert.isNotNull(ref);
		Assert.isNotNull(editor);

		fRegion= region;
		fKeyName= keyName;
		fAccessorClassReference= ref;
		fEditor= editor;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.IHyperlink#getHyperlinkRegion()
	 */
	public IRegion getHyperlinkRegion() {
		return fRegion;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.IHyperlink#open()
	 */
	public void open() {
		IStorage propertiesFile= null;
		try {
			ITypeBinding typeBinding= fAccessorClassReference.getBinding();
			propertiesFile= NLSHintHelper.getResourceBundle(typeBinding.getJavaElement().getJavaScriptProject(), fAccessorClassReference);
		} catch (JavaScriptModelException e) {
			// Don't open the file
		}
		if (propertiesFile == null) {
			showErrorInStatusLine(fEditor, JavaEditorMessages.Editor_OpenPropertiesFile_error_fileNotFound_dialogMessage);
			return;
		}

		IEditorPart editor;
		try {
			editor= EditorUtility.openInEditor(propertiesFile, true);
		} catch (PartInitException e) {
			handleOpenPropertiesFileFailed(propertiesFile);
			return;
		} catch (JavaScriptModelException e) {
			handleOpenPropertiesFileFailed(propertiesFile);
			return;
		}

//		// Reveal the key in the properties file
//		if (editor instanceof ITextEditor) {
//			IRegion region= null;
//			boolean found= false;
//
//			// Find key in document
//			IEditorInput editorInput= editor.getEditorInput();
//			IDocument document= ((ITextEditor)editor).getDocumentProvider().getDocument(editorInput);
//			if (document != null) {
//				FindReplaceDocumentAdapter finder= new FindReplaceDocumentAdapter(document);
//				PropertyKeyHyperlinkDetector detector= new PropertyKeyHyperlinkDetector();
//				detector.setContext(editor);
//				String key= PropertyFileDocumentModel.unwindEscapeChars(fKeyName);
//				int offset= document.getLength() - 1;
//				try {
//					while (!found && offset >= 0) {
//						region= finder.find(offset, key, false, true, false, false);
//						if (region == null)
//							offset= -1;
//						else {
//							// test whether it's the key
//							IHyperlink[] hyperlinks= detector.detectHyperlinks(null, region, false);
//							if (hyperlinks != null) {
//								for (int i= 0; i < hyperlinks.length; i++) {
//									IRegion hyperlinkRegion= hyperlinks[i].getHyperlinkRegion();
//									found= key.equals(document.get(hyperlinkRegion.getOffset(), hyperlinkRegion.getLength()));
//								}
//							} else if (document instanceof IDocumentExtension3) {
//								// Fall back: test using properties file partitioning
//								ITypedRegion partition= null;
//								partition= ((IDocumentExtension3)document).getPartition(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, region.getOffset(), false);
//								found= IDocument.DEFAULT_CONTENT_TYPE.equals(partition.getType())
//										&& key.equals(document.get(partition.getOffset(), partition.getLength()).trim());
//							}
//							// Prevent endless loop (panic code, shouldn't be needed)
//							if (offset == region.getOffset())
//								offset= -1;
//							else
//								offset= region.getOffset();
//						}
//					}
//				} catch (BadLocationException ex) {
//					found= false;
//				} catch (BadPartitioningException e1) {
//					found= false;
//				}
//			}
//			if (found)
//				EditorUtility.revealInEditor(editor, region);
//			else {
//				EditorUtility.revealInEditor(editor, 0, 0);
//				showErrorInStatusLine(editor, Messages.format(JavaEditorMessages.Editor_OpenPropertiesFile_error_keyNotFound, fKeyName));
//			}
//		}
	}

	private void showErrorInStatusLine(IEditorPart editor, final String message) {
		final Display display= fEditor.getSite().getShell().getDisplay();
		display.beep();
		final IEditorStatusLine statusLine= (IEditorStatusLine)editor.getAdapter(IEditorStatusLine.class);
		if (statusLine != null) {
			display.asyncExec(new Runnable() {
				/*
				 * @see java.lang.Runnable#run()
				 */
				public void run() {
					statusLine.setMessage(true, message, null);
				}
			});
		}
	}

	private void handleOpenPropertiesFileFailed(IStorage propertiesFile) {
		showErrorInStatusLine(fEditor, Messages.format(JavaEditorMessages.Editor_OpenPropertiesFile_error_openEditor_dialogMessage, propertiesFile.getFullPath().toOSString()));
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.IHyperlink#getTypeLabel()
	 */
	public String getTypeLabel() {
		return null;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.IHyperlink#getHyperlinkText()
	 */
	public String getHyperlinkText() {
		return null;
	}
}
