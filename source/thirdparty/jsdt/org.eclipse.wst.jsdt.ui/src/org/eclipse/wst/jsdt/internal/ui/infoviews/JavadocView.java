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
package org.eclipse.wst.jsdt.internal.ui.infoviews;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IAbstractTextEditorHelpContextIds;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IOpenable;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.internal.ui.text.html.HTMLPrinter;
import org.eclipse.wst.jsdt.internal.ui.text.html.HTMLTextPresenter;
import org.eclipse.wst.jsdt.ui.JSdocContentAccess;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.text.IJavaScriptPartitions;
import org.osgi.framework.Bundle;

/**
 * View which shows Javadoc for a given Java element.
 *
 * FIXME: As of 3.0 selectAll() and getSelection() is not working
 *			see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63022
 *
 * 
 */
public class JavadocView extends AbstractInfoView {

	/**
	 * Preference key for the preference whether to show a dialog
	 * when the SWT Browser widget is not available.
	 * 
	 */
	private static final String DO_NOT_WARN_PREFERENCE_KEY= "JavadocView.error.doNotWarn"; //$NON-NLS-1$
	
	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=73558
	private static final boolean WARNING_DIALOG_ENABLED= false;

	/** Flags used to render a label in the text widget. */
	private static final long LABEL_FLAGS=  JavaScriptElementLabels.ALL_FULLY_QUALIFIED
		| JavaScriptElementLabels.M_PRE_RETURNTYPE | JavaScriptElementLabels.M_PARAMETER_TYPES | JavaScriptElementLabels.M_PARAMETER_NAMES | JavaScriptElementLabels.M_EXCEPTIONS
		| JavaScriptElementLabels.F_PRE_TYPE_SIGNATURE | JavaScriptElementLabels.T_TYPE_PARAMETERS;


	/** The HTML widget. */
	private Browser fBrowser;
	/** The text widget. */
	private StyledText fText;
	/** The information presenter. */
	private DefaultInformationControl.IInformationPresenter fPresenter;
	/** The text presentation. */
	private TextPresentation fPresentation= new TextPresentation();
	/** The select all action */
	private SelectAllAction fSelectAllAction;
	/** The style sheet (css) */
	private static String fgStyleSheet;
	/**
	 * <code>true</code> once the style sheet has been loaded.
	 * 
	 */
	private static boolean fgStyleSheetLoaded= false;

	/** The Browser widget */
	private boolean fIsUsingBrowserWidget;

	private RGB fBackgroundColorRGB;
	/**
	 * The font listener.
	 * 
	 */
	private IPropertyChangeListener fFontListener;

	
	/**
	 * The Javadoc view's select all action.
	 */
	private class SelectAllAction extends Action {

		/** The control. */
		private Control fControl;
		/** The selection provider. */
		private SelectionProvider fSelectionProvider;

		/**
		 * Creates the action.
		 *
		 * @param control the widget
		 * @param selectionProvider the selection provider
		 */
		public SelectAllAction(Control control, SelectionProvider selectionProvider) {
			super("selectAll"); //$NON-NLS-1$

			Assert.isNotNull(control);
			Assert.isNotNull(selectionProvider);
			fControl= control;
			fSelectionProvider= selectionProvider;

			// FIXME: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63022
			setEnabled(!fIsUsingBrowserWidget);

			setText(InfoViewMessages.SelectAllAction_label);
			setToolTipText(InfoViewMessages.SelectAllAction_tooltip);
			setDescription(InfoViewMessages.SelectAllAction_description);

			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IAbstractTextEditorHelpContextIds.SELECT_ALL_ACTION);
		}

		/**
		 * Selects all in the view.
		 */
		public void run() {
			if (fControl instanceof StyledText)
		        ((StyledText)fControl).selectAll();
			else {
				// FIXME: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63022
//				((Browser)fControl).selectAll();
				if (fSelectionProvider != null)
					fSelectionProvider.fireSelectionChanged();
			}
		}
	}

	/**
	 * The Javadoc view's selection provider.
	 */
	private static class SelectionProvider implements ISelectionProvider {

		/** The selection changed listeners. */
		private ListenerList fListeners= new ListenerList(ListenerList.IDENTITY);
		/** The widget. */
		private Control fControl;

		/**
		 * Creates a new selection provider.
		 *
		 * @param control	the widget
		 */
		public SelectionProvider(Control control) {
		    Assert.isNotNull(control);
			fControl= control;
			if (fControl instanceof StyledText) {
			    ((StyledText)fControl).addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
					    fireSelectionChanged();
					}
			    });
			} else {
				// FIXME: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63022
//				((Browser)fControl).addSelectionListener(new SelectionAdapter() {
//					public void widgetSelected(SelectionEvent e) {
//						fireSelectionChanged();
//					}
//				});
			}
		}

		/**
		 * Sends a selection changed event to all listeners.
		 */
		public void fireSelectionChanged() {
			ISelection selection= getSelection();
			SelectionChangedEvent event= new SelectionChangedEvent(this, selection);
			Object[] selectionChangedListeners= fListeners.getListeners();
			for (int i= 0; i < selectionChangedListeners.length; i++)
				((ISelectionChangedListener)selectionChangedListeners[i]).selectionChanged(event);
		}

		/*
		 * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
		 */
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
			fListeners.add(listener);
		}

		/*
		 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
		 */
		public ISelection getSelection() {
			if (fControl instanceof StyledText) {
				IDocument document= new Document(((StyledText)fControl).getSelectionText());
				return new TextSelection(document, 0, document.getLength());
			} else {
				// FIXME: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63022
				return StructuredSelection.EMPTY;
			}
		}

		/*
		 * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
		 */
		public void removeSelectionChangedListener(ISelectionChangedListener listener) {
			fListeners.remove(listener);
		}

		/*
		 * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
		 */
		public void setSelection(ISelection selection) {
			// not supported
		}
	}

	/*
	 * @see AbstractInfoView#internalCreatePartControl(Composite)
	 */
	protected void internalCreatePartControl(Composite parent) {
		try {
			fBrowser= new Browser(parent, SWT.NONE);
			fIsUsingBrowserWidget= true;
			
		} catch (SWTError er) {

			/* The Browser widget throws an SWTError if it fails to
			 * instantiate properly. Application code should catch
			 * this SWTError and disable any feature requiring the
			 * Browser widget.
			 * Platform requirements for the SWT Browser widget are available
			 * from the SWT FAQ web site.
			 */

			IPreferenceStore store= JavaScriptPlugin.getDefault().getPreferenceStore();
			boolean doNotWarn= store.getBoolean(DO_NOT_WARN_PREFERENCE_KEY);
			if (WARNING_DIALOG_ENABLED && !doNotWarn) {
				String title= InfoViewMessages.JavadocView_error_noBrowser_title;
				String message= InfoViewMessages.JavadocView_error_noBrowser_message;
				String toggleMessage= InfoViewMessages.JavadocView_error_noBrowser_doNotWarn;
				MessageDialogWithToggle dialog= MessageDialogWithToggle.openError(parent.getShell(), title, message, toggleMessage, false, null, null); 
				if (dialog.getReturnCode() == Window.OK)
					store.setValue(DO_NOT_WARN_PREFERENCE_KEY, dialog.getToggleState());
			}

			fIsUsingBrowserWidget= false;
		}

		if (!fIsUsingBrowserWidget) {
			fText= new StyledText(parent, SWT.V_SCROLL | SWT.H_SCROLL);
			fText.setEditable(false);
			fPresenter= new HTMLTextPresenter(false);

			fText.addControlListener(new ControlAdapter() {
				/*
				 * @see org.eclipse.swt.events.ControlAdapter#controlResized(org.eclipse.swt.events.ControlEvent)
				 */
				public void controlResized(ControlEvent e) {
					setInput(fText.getText());
				}
			});
		}

		initStyleSheet();
		listenForFontChanges();
		getViewSite().setSelectionProvider(new SelectionProvider(getControl()));
	}

	/**
	 * Registers a listener for the Java editor font.
	 * 
	 * 
	 */
	private void listenForFontChanges() {
		fFontListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (PreferenceConstants.APPEARANCE_JAVADOC_FONT.equals(event.getProperty())) {
					fgStyleSheetLoaded= false;
					// trigger reloading, but make sure other listeners have already run, so that
					// the style sheet gets reloaded only once.
					final Display display= getSite().getPage().getWorkbenchWindow().getWorkbench().getDisplay();
					if (!display.isDisposed()) {
						display.asyncExec(new Runnable() {
							public void run() {
								if (!display.isDisposed()) {
									initStyleSheet();
									refresh();
								}
							}
						});
					}
				}
			}
		};
		JFaceResources.getFontRegistry().addListener(fFontListener);
	}
	
	private static void initStyleSheet() {
		if (fgStyleSheetLoaded)
			return;
		fgStyleSheetLoaded= true;
		fgStyleSheet= loadStyleSheet();
	}
	
	private static String loadStyleSheet() {
		Bundle bundle= Platform.getBundle(JavaScriptPlugin.getPluginId());
		URL styleSheetURL= bundle.getEntry("/JavadocViewStyleSheet.css"); //$NON-NLS-1$
		if (styleSheetURL == null)
			return null;

		try {
			styleSheetURL= FileLocator.toFileURL(styleSheetURL);
			StringBuffer buffer= new StringBuffer(200);
			BufferedReader reader= new BufferedReader(new InputStreamReader(styleSheetURL.openStream()));
			try {
				String line= reader.readLine();
				while (line != null) {
					buffer.append(line);
					buffer.append('\n');
					line= reader.readLine();
				}
			} finally {
				reader.close();
			}
			FontData fontData= JFaceResources.getFontRegistry().getFontData(PreferenceConstants.APPEARANCE_JAVADOC_FONT)[0];
			return HTMLPrinter.convertTopLevelFont(buffer.toString(), fontData);
		} catch (IOException ex) {
			JavaScriptPlugin.log(ex);
			return null;
		}
	}

	/*
	 * @see AbstractInfoView#createActions()
	 */
	protected void createActions() {
		super.createActions();
		fSelectAllAction= new SelectAllAction(getControl(), (SelectionProvider)getSelectionProvider());
	}


	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.infoviews.AbstractInfoView#getSelectAllAction()
	 * 
	 */
	protected IAction getSelectAllAction() {
		// FIXME: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63022
		if (fIsUsingBrowserWidget)
			return null;

		return fSelectAllAction;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.infoviews.AbstractInfoView#getCopyToClipboardAction()
	 * 
	 */
	protected IAction getCopyToClipboardAction() {
		// FIXME: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63022
		if (fIsUsingBrowserWidget)
			return null;

		return super.getCopyToClipboardAction();
	}

	/*
 	 * @see AbstractInfoView#setForeground(Color)
 	 */
	protected void setForeground(Color color) {
		getControl().setForeground(color);
	}

	/*
	 * @see AbstractInfoView#setBackground(Color)
	 */
	protected void setBackground(Color color) {
		getControl().setBackground(color);
		fBackgroundColorRGB= color.getRGB();
		refresh();
	}

	/**
	 * Refreshes the view.
	 *
	 * 
	 */
	private void refresh() {
		IJavaScriptElement input= getInput();
		if (input == null) {
			StringBuffer buffer= new StringBuffer(""); //$NON-NLS-1$
			HTMLPrinter.insertPageProlog(buffer, 0, fBackgroundColorRGB, fgStyleSheet);
			setInput(buffer.toString());
		} else {
			setInput(computeInput(input));
		}
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.infoviews.AbstractInfoView#getBackgroundColorKey()
	 * 
	 */
	protected String getBackgroundColorKey() {
		return "org.eclipse.wst.jsdt.ui.JavadocView.backgroundColor";		 //$NON-NLS-1$
	}

	/*
	 * @see AbstractInfoView#internalDispose()
	 */
	protected void internalDispose() {
		fText= null;
		fBrowser= null;
		if (fFontListener != null) {
			JFaceResources.getFontRegistry().removeListener(fFontListener);
			fFontListener= null;
		}
	}

	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() {
		getControl().setFocus();
	}

	/*
	 * @see AbstractInfoView#computeInput(Object)
	 */
	protected Object computeInput(Object input) {
		if (getControl() == null || ! (input instanceof IJavaScriptElement))
			return null;

		IJavaScriptElement je= (IJavaScriptElement)input;
		String javadocHtml;

		switch (je.getElementType()) {
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				try {
					javadocHtml= getJavadocHtml(((IJavaScriptUnit)je).getTypes());
				} catch (JavaScriptModelException ex) {
					javadocHtml= null;
				}
				break;
			case IJavaScriptElement.CLASS_FILE:
				javadocHtml= getJavadocHtml(new IJavaScriptElement[] {((IClassFile)je).getType()});
				break;
			default:
				javadocHtml= getJavadocHtml(new IJavaScriptElement[] { je });
		}
		
		if (javadocHtml == null)
			return ""; //$NON-NLS-1$
		
		return javadocHtml;
	}

	/*
	 * @see AbstractInfoView#setInput(Object)
	 */
	protected void setInput(Object input) {
		String javadocHtml= (String)input;

		if (fIsUsingBrowserWidget) {
			if (javadocHtml != null && javadocHtml.length() > 0) {
				boolean RTL= (getSite().getShell().getStyle() & SWT.RIGHT_TO_LEFT) != 0;
				if (RTL) {
					StringBuffer buffer= new StringBuffer(javadocHtml);
					HTMLPrinter.insertStyles(buffer, new String[] { "direction:rtl" } ); //$NON-NLS-1$
					javadocHtml= buffer.toString();
				}
			}
			fBrowser.setText(javadocHtml);
		} else {
			fPresentation.clear();
			Rectangle size=  fText.getClientArea();

			try {
				javadocHtml= ((DefaultInformationControl.IInformationPresenterExtension)fPresenter).updatePresentation(getSite().getShell(), javadocHtml, fPresentation, size.width, size.height);
			} catch (IllegalArgumentException ex) {
				// the javadoc might no longer be valid
				return;
			}
			fText.setText(javadocHtml);
			TextPresentation.applyTextPresentation(fPresentation, fText);
		}
	}
	
	/**
	 * Returns the Javadoc in HTML format.
	 *
	 * @param result the Java elements for which to get the Javadoc
	 * @return a string with the Javadoc in HTML format.
	 */
	private String getJavadocHtml(IJavaScriptElement[] result) {
		StringBuffer buffer= new StringBuffer();
		int nResults= result.length;

		if (nResults == 0)
			return null;

		if (nResults > 1) {

			for (int i= 0; i < result.length; i++) {
				HTMLPrinter.startBulletList(buffer);
				IJavaScriptElement curr= result[i];
				if (curr instanceof IMember)
					HTMLPrinter.addBullet(buffer, getInfoText((IMember) curr));
				HTMLPrinter.endBulletList(buffer);
			}

		} else {

			IJavaScriptElement curr= result[0];
			if (curr instanceof IMember) {
				IMember member= (IMember) curr;
//				HTMLPrinter.addSmallHeader(buffer, getInfoText(member));
				Reader reader;
				try {
					reader= JSdocContentAccess.getHTMLContentReader(member, true, true);
					
					// Provide hint why there's no Javadoc
					if (reader == null && member.isBinary()) {
						boolean hasAttachedJavadoc= JavaDocLocations.getJavadocBaseLocation(member) != null;
						IPackageFragmentRoot root= (IPackageFragmentRoot)member.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT_ROOT);
						boolean hasAttachedSource= root != null && root.getSourceAttachmentPath() != null;
						IOpenable openable= member.getOpenable();
						boolean hasSource= openable.getBuffer() != null;
						
						if (!hasAttachedSource && !hasAttachedJavadoc)
							reader= new StringReader(InfoViewMessages.JavadocView_noAttachments);
						else if (!hasAttachedJavadoc && !hasSource)
							reader= new StringReader(InfoViewMessages.JavadocView_noAttachedJavadoc);
						else if (!hasAttachedSource)
							reader= new StringReader(InfoViewMessages.JavadocView_noAttachedSource);
						else if (!hasSource)
							reader= new StringReader(InfoViewMessages.JavadocView_noInformation);
					}
					
				} catch (JavaScriptModelException ex) {
					reader= new StringReader(InfoViewMessages.JavadocView_error_gettingJavadoc);
					JavaScriptPlugin.log(ex.getStatus());
				}
				if (reader != null) {
					HTMLPrinter.addParagraph(buffer, reader);
				}
			} else if (curr != null && curr.getElementType() == IJavaScriptElement.LOCAL_VARIABLE) {
				Reader reader = null;
				try {
					reader= JSdocContentAccess.getHTMLContentReader((ILocalVariable)curr, false, true);
				}
				catch (JavaScriptModelException e) {
					reader= new StringReader(InfoViewMessages.JavadocView_error_gettingJavadoc);
					JavaScriptPlugin.log(e.getStatus());
				}
				if (reader != null) {
					HTMLPrinter.addParagraph(buffer, reader);
				}
				else {
					HTMLPrinter.addSmallHeader(buffer, InfoViewMessages.JavadocView_noInformation);
				}
			}
		}

		boolean flushContent= true;
		if (buffer.length() > 0 || flushContent) {
			HTMLPrinter.insertPageProlog(buffer, 0, fBackgroundColorRGB, fgStyleSheet);
			HTMLPrinter.addPageEpilog(buffer);
			return buffer.toString();
		}

		return null;
	}

	/**
	 * Gets the label for the given member.
	 *
	 * @param member the Java member
	 * @return a string containing the member's label
	 */
	private String getInfoText(IMember member) {
		return JavaScriptElementLabels.getElementLabel(member, LABEL_FLAGS);
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.infoviews.AbstractInfoView#isIgnoringNewInput(org.eclipse.wst.jsdt.core.IJavaScriptElement, org.eclipse.jface.viewers.ISelection)
	 * 
	 */
	protected boolean isIgnoringNewInput(IJavaScriptElement je, IWorkbenchPart part, ISelection selection) {
		if (super.isIgnoringNewInput(je, part, selection)
				&& part instanceof ITextEditor
				&& selection instanceof ITextSelection) {
			
			ITextEditor editor= (ITextEditor)part;
			IDocumentProvider docProvider= editor.getDocumentProvider();
			if (docProvider == null)
				return false;
			
			IDocument document= docProvider.getDocument(editor.getEditorInput());
			if (!(document instanceof IDocumentExtension3))
				return false;
			
			try {
				int offset= ((ITextSelection)selection).getOffset();
				String partition= ((IDocumentExtension3)document).getContentType(IJavaScriptPartitions.JAVA_PARTITIONING, offset, false);
				return  partition != IJavaScriptPartitions.JAVA_DOC;
			} catch (BadPartitioningException ex) {
				return false;
			} catch (BadLocationException ex) {
				return false;
			}

		}
		return false;
	}

	/*
	 * @see AbstractInfoView#findSelectedJavaElement(IWorkbenchPart)
	 */
	protected IJavaScriptElement findSelectedJavaElement(IWorkbenchPart part, ISelection selection) {
		IJavaScriptElement element;
		try {
			element= super.findSelectedJavaElement(part, selection);

			if (element == null && part instanceof JavaEditor && selection instanceof ITextSelection) {

				JavaEditor editor= (JavaEditor)part;
				ITextSelection textSelection= (ITextSelection)selection;

				IDocumentProvider documentProvider= editor.getDocumentProvider();
				if (documentProvider == null)
					return null;

				IDocument document= documentProvider.getDocument(editor.getEditorInput());
				if (document == null)
					return null;

				ITypedRegion typedRegion= TextUtilities.getPartition(document, IJavaScriptPartitions.JAVA_PARTITIONING, textSelection.getOffset(), false);
				if (IJavaScriptPartitions.JAVA_DOC.equals(typedRegion.getType()))
					return TextSelectionConverter.getElementAtOffset((JavaEditor)part, textSelection);
				else
					return null;
			} else
				return element;
		} catch (JavaScriptModelException e) {
			return null;
		} catch (BadLocationException e) {
			return null;
		}
	}

	/*
	 * @see AbstractInfoView#getControl()
	 */
	protected Control getControl() {
		if (fIsUsingBrowserWidget)
			return fBrowser;
		else
			return fText;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.infoviews.AbstractInfoView#getHelpContextId()
	 * 
	 */
	protected String getHelpContextId() {
		return IJavaHelpContextIds.JAVADOC_VIEW;
	}
}
