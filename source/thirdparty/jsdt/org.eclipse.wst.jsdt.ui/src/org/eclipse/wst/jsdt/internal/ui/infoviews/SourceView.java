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
package org.eclipse.wst.jsdt.internal.ui.infoviews;

import java.io.IOException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IAbstractTextEditorHelpContextIds;
import org.eclipse.wst.jsdt.core.ICodeAssist;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.util.Strings;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.wst.jsdt.internal.ui.text.JavaCodeReader;
import org.eclipse.wst.jsdt.internal.ui.text.SimpleJavaSourceViewerConfiguration;
import org.eclipse.wst.jsdt.ui.IContextMenuConstants;
import org.eclipse.wst.jsdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.wst.jsdt.ui.actions.JdtActionConstants;
import org.eclipse.wst.jsdt.ui.actions.OpenAction;
import org.eclipse.wst.jsdt.ui.text.IJavaScriptPartitions;
import org.eclipse.wst.jsdt.ui.text.JavaScriptSourceViewerConfiguration;

/**
 * View which shows source for a given Java element.
 *
 * 
 */
public class SourceView extends AbstractInfoView implements IMenuListener {

	/** Symbolic Java editor font name. */
	private static final String SYMBOLIC_FONT_NAME= "org.eclipse.wst.jsdt.ui.editors.textfont"; //$NON-NLS-1$

	/**
	 * Internal property change listener for handling changes in the editor's preferences.
	 *
	 * 
	 */
	class PropertyChangeListener implements IPropertyChangeListener {
		/*
		 * @see IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
		 */
		public void propertyChange(PropertyChangeEvent event) {
			if (fViewer == null)
				return;

			if (fViewerConfiguration.affectsTextPresentation(event)) {
				fViewerConfiguration.handlePropertyChangeEvent(event);
				fViewer.invalidateTextPresentation();
			}
		}
	}

	/**
	 * Internal property change listener for handling workbench font changes.
	 */
	class FontPropertyChangeListener implements IPropertyChangeListener {
		/*
		 * @see IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
		 */
		public void propertyChange(PropertyChangeEvent event) {
			if (fViewer == null)
				return;

			String property= event.getProperty();

			if (SYMBOLIC_FONT_NAME.equals(property))
				setViewerFont();
		}
	}

	/**
	 * The Javadoc view's select all action.
	 */
	private static class SelectAllAction extends Action {

		private TextViewer fTextViewer;

		/**
		 * Creates the action.
		 *
		 * @param textViewer the text viewer
		 */
		public SelectAllAction(TextViewer textViewer) {
			super("selectAll"); //$NON-NLS-1$

			Assert.isNotNull(textViewer);
			fTextViewer= textViewer;

			setText(InfoViewMessages.SelectAllAction_label);
			setToolTipText(InfoViewMessages.SelectAllAction_tooltip);
			setDescription(InfoViewMessages.SelectAllAction_description);

			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IAbstractTextEditorHelpContextIds.SELECT_ALL_ACTION);
		}

		/**
		 * Selects all in the viewer.
		 */
		public void run() {
			fTextViewer.doOperation(ITextOperationTarget.SELECT_ALL);
		}
	}

	/** This view's source viewer */
	private SourceViewer fViewer;
	/** The viewers configuration */
	private JavaScriptSourceViewerConfiguration fViewerConfiguration;
	/** The viewer's font properties change listener. */
	private IPropertyChangeListener fFontPropertyChangeListener= new FontPropertyChangeListener();
	/**
	 * The editor's property change listener.
	 * 
	 */
	private IPropertyChangeListener fPropertyChangeListener= new PropertyChangeListener();
	/** The open action */
	private OpenAction fOpen;
	/** The number of removed leading comment lines. */
	private int fCommentLineCount;
	/** The select all action. */
	private SelectAllAction fSelectAllAction;
	/** Element opened by the open action. */
	private IJavaScriptElement fLastOpenedElement;


	/*
	 * @see AbstractInfoView#internalCreatePartControl(Composite)
	 */
	protected void internalCreatePartControl(Composite parent) {
		IPreferenceStore store= JavaScriptPlugin.getDefault().getCombinedPreferenceStore();
		fViewer= new JavaSourceViewer(parent, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL, store);
		fViewerConfiguration= new SimpleJavaSourceViewerConfiguration(JavaScriptPlugin.getDefault().getJavaTextTools().getColorManager(), store, null, IJavaScriptPartitions.JAVA_PARTITIONING, false);
		fViewer.configure(fViewerConfiguration);
		fViewer.setEditable(false);

		setViewerFont();
		JFaceResources.getFontRegistry().addListener(fFontPropertyChangeListener);

		store.addPropertyChangeListener(fPropertyChangeListener);

		getViewSite().setSelectionProvider(fViewer);
	}

	/*
	 * @see AbstractInfoView#internalCreatePartControl(Composite)
	 */
	protected void createActions() {
		super.createActions();
		fSelectAllAction= new SelectAllAction(fViewer);

		// Setup OpenAction
		fOpen= new OpenAction(getViewSite()) {

			/*
			 * @see org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction#getSelection()
			 */
			public ISelection getSelection() {
				return convertToJavaElementSelection(fViewer.getSelection());
			}

			/*
			 * @see org.eclipse.wst.jsdt.ui.actions.OpenAction#run(IStructuredSelection)
			 */
			public void run(IStructuredSelection selection) {
				if (selection.isEmpty()) {
					getShell().getDisplay().beep();
					return;
				}
				super.run(selection);
			}

			/*
			 * @see org.eclipse.wst.jsdt.ui.actions.OpenAction#getElementToOpen(Object)
			 */
			public Object getElementToOpen(Object object) throws JavaScriptModelException {
				if (object instanceof IJavaScriptElement)
					fLastOpenedElement= (IJavaScriptElement)object;
				else
					fLastOpenedElement= null;
				return super.getElementToOpen(object);
			}

			/*
			 * @see org.eclipse.wst.jsdt.ui.actions.OpenAction#run(Object[])
			 */
			public void run(Object[] elements) {
				stopListeningForSelectionChanges();
				super.run(elements);
				startListeningForSelectionChanges();
			}
		};
	}


	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.infoviews.AbstractInfoView#getSelectAllAction()
	 * 
	 */
	protected IAction getSelectAllAction() {
		return fSelectAllAction;
	}

	/*
	 * @see AbstractInfoView#fillActionBars(IActionBars)
	 */
	protected void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		actionBars.setGlobalActionHandler(JdtActionConstants.OPEN, fOpen);
		fOpen.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EDITOR);
	}

	/*
	 * @see AbstractInfoView#getControl()
	 */
	protected Control getControl() {
		return fViewer.getControl();
	}

	/*
	 * @see AbstractInfoView#menuAboutToShow(IMenuManager)
	 */
	public void menuAboutToShow(IMenuManager menu) {
		super.menuAboutToShow(menu);
		menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fOpen);
	}

	/*
	 * @see AbstractInfoView#setForeground(Color)
	 */
	protected void setForeground(Color color) {
		fViewer.getTextWidget().setForeground(color);
	}

	/*
	 * @see AbstractInfoView#setBackground(Color)
	 */
	protected void setBackground(Color color) {
		fViewer.getTextWidget().setBackground(color);
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.infoviews.AbstractInfoView#getBackgroundColorKey()
	 * 
	 */
	protected String getBackgroundColorKey() {
		return "org.eclipse.wst.jsdt.ui.DeclarationView.backgroundColor";		 //$NON-NLS-1$
	}
	
	/**
	 * Converts the given selection to a structured selection
	 * containing Java elements.
	 *
	 * @param selection the selection
	 * @return a structured selection with Java elements
	 */
	private IStructuredSelection convertToJavaElementSelection(ISelection selection) {

		if (!(selection instanceof ITextSelection && fCurrentViewInput instanceof ISourceReference))
			return StructuredSelection.EMPTY;

		ITextSelection textSelection= (ITextSelection)selection;

		Object codeAssist= fCurrentViewInput.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
		if (codeAssist == null)
			codeAssist= fCurrentViewInput.getAncestor(IJavaScriptElement.CLASS_FILE);

		if (codeAssist instanceof ICodeAssist) {
			IJavaScriptElement[] elements= null;
			try {
				ISourceRange range= ((ISourceReference)fCurrentViewInput).getSourceRange();
				elements= ((ICodeAssist)codeAssist).codeSelect(range.getOffset() + getOffsetInUnclippedDocument(textSelection), textSelection.getLength());
			} catch (JavaScriptModelException e) {
				return StructuredSelection.EMPTY;
			}
			if (elements != null && elements.length > 0) {
				return new StructuredSelection(elements[0]);
			} else
				return StructuredSelection.EMPTY;
		}

		return StructuredSelection.EMPTY;
	}

	/**
	 * Computes and returns the offset in the unclipped document
	 * based on the given text selection from the clipped
	 * document.
	 *
	 * @param textSelection
	 * @return the offest in the unclipped document or <code>-1</code> if the offset cannot be computed
	 */
	private int getOffsetInUnclippedDocument(ITextSelection textSelection) {
		IDocument unclippedDocument= null;
		try {
			unclippedDocument= new Document(((ISourceReference)fCurrentViewInput).getSource());
		} catch (JavaScriptModelException e) {
			return -1;
		}
		IDocument clippedDoc= (IDocument)fViewer.getInput();
		try {
			IRegion unclippedLineInfo= unclippedDocument.getLineInformation(fCommentLineCount + textSelection.getStartLine());
			IRegion clippedLineInfo= clippedDoc.getLineInformation(textSelection.getStartLine());
			int removedIndentation= unclippedLineInfo.getLength() - clippedLineInfo.getLength();
			int relativeLineOffset= textSelection.getOffset() - clippedLineInfo.getOffset();
			return unclippedLineInfo.getOffset() + removedIndentation + relativeLineOffset ;
		} catch (BadLocationException ex) {
			return -1;
		}
	}

	/*
	 * @see AbstractInfoView#internalDispose()
	 */
	protected void internalDispose() {
		fViewer= null;
		fViewerConfiguration= null;
		JFaceResources.getFontRegistry().removeListener(fFontPropertyChangeListener);
		JavaScriptPlugin.getDefault().getCombinedPreferenceStore().removePropertyChangeListener(fPropertyChangeListener);
	}

	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() {
		fViewer.getTextWidget().setFocus();
	}

	/*
	 * @see AbstractInfoView#computeInput(Object)
	 */
	protected Object computeInput(Object input) {

		if (fViewer == null || !(input instanceof ISourceReference))
			return null;

		ISourceReference sourceRef= (ISourceReference)input;

		if (fLastOpenedElement != null && input instanceof IJavaScriptElement && ((IJavaScriptElement)input).getHandleIdentifier().equals(fLastOpenedElement.getHandleIdentifier())) {
			fLastOpenedElement= null;
			return null;
		} else {
			fLastOpenedElement= null;
		}

		String source;
		try {
			source= sourceRef.getSource();
		} catch (JavaScriptModelException ex) {
			return ""; //$NON-NLS-1$
		}

		if (source == null)
			return ""; //$NON-NLS-1$

		source= removeLeadingComments(source);
		String delim= StubUtility.getLineDelimiterUsed((IJavaScriptElement) input);

		String[] sourceLines= Strings.convertIntoLines(source);
		if (sourceLines == null || sourceLines.length == 0)
			return ""; //$NON-NLS-1$

		String firstLine= sourceLines[0];
		boolean firstCharNotWhitespace= firstLine != null && firstLine.length() > 0 && !Character.isWhitespace(firstLine.charAt(0));
		if (firstCharNotWhitespace)
			sourceLines[0]= ""; //$NON-NLS-1$
		IJavaScriptProject project;
		if (input instanceof IJavaScriptElement)
			project= ((IJavaScriptElement) input).getJavaScriptProject();
		else
			project= null;
		Strings.trimIndentation(sourceLines, project);

		if (firstCharNotWhitespace)
			sourceLines[0]= firstLine;

		return Strings.concatenate(sourceLines, delim);
	}

	/*
	 * @see AbstractInfoView#setInput(Object)
	 */
	protected void setInput(Object input) {
		if (input instanceof IDocument)
			fViewer.setInput(input);
		else if (input == null)
			fViewer.setInput(new Document("")); //$NON-NLS-1$
		else {
			IDocument document= new Document(input.toString());
			JavaScriptPlugin.getDefault().getJavaTextTools().setupJavaDocumentPartitioner(document, IJavaScriptPartitions.JAVA_PARTITIONING);			
			fViewer.setInput(document);
		}
	}

	/**
	 * Removes the leading comments from the given source.
	 *
	 * @param source the string with the source
	 * @return the source without leading comments
	 */
	private String removeLeadingComments(String source) {
		JavaCodeReader reader= new JavaCodeReader();
		IDocument document= new Document(source);
		int i;
		try {
			reader.configureForwardReader(document, 0, document.getLength(), true, false);
			int c= reader.read();
			while (c != -1 && (c == '\r' || c == '\n' || c == '\t')) {
				c= reader.read();
			}
			i= reader.getOffset();
			reader.close();
		} catch (IOException ex) {
			i= 0;
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (IOException ex) {
				JavaScriptPlugin.log(ex);
			}
		}

		try {
			fCommentLineCount= document.getLineOfOffset(i);
		} catch (BadLocationException e) {
			fCommentLineCount= 0;
		}

		if (i < 0)
			return source;

		return source.substring(i);
	}

	/**
	 * Sets the font for this viewer sustaining selection and scroll position.
	 */
	private void setViewerFont() {
		Font font= JFaceResources.getFont(SYMBOLIC_FONT_NAME);

		if (fViewer.getDocument() != null) {

			Point selection= fViewer.getSelectedRange();
			int topIndex= fViewer.getTopIndex();

			StyledText styledText= fViewer.getTextWidget();
			Control parent= fViewer.getControl();

			parent.setRedraw(false);

			styledText.setFont(font);

			fViewer.setSelectedRange(selection.x , selection.y);
			fViewer.setTopIndex(topIndex);

			if (parent instanceof Composite) {
				Composite composite= (Composite) parent;
				composite.layout(true);
			}

			parent.setRedraw(true);


		} else {
			StyledText styledText= fViewer.getTextWidget();
			styledText.setFont(font);
		}
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.infoviews.AbstractInfoView#getHelpContextId()
	 * 
	 */
	protected String getHelpContextId() {
		return IJavaHelpContextIds.SOURCE_VIEW;
	}
}
