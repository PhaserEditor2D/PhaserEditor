/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Tom Eicher (Avaloq Evolution AG) - block selection mode
 *******************************************************************************/
package org.eclipse.wst.jsdt.ui.text;

import java.util.Arrays;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.AbstractInformationControlManager;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.MultiPassContentFormatter;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.wst.jsdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ICompilationUnitDocumentProvider;
import org.eclipse.wst.jsdt.internal.ui.text.AbstractJavaScanner;
import org.eclipse.wst.jsdt.internal.ui.text.ContentAssistPreference;
import org.eclipse.wst.jsdt.internal.ui.text.HTMLAnnotationHover;
import org.eclipse.wst.jsdt.internal.ui.text.JavaCommentScanner;
import org.eclipse.wst.jsdt.internal.ui.text.JavaCompositeReconcilingStrategy;
import org.eclipse.wst.jsdt.internal.ui.text.JavaElementProvider;
import org.eclipse.wst.jsdt.internal.ui.text.JavaOutlineInformationControl;
import org.eclipse.wst.jsdt.internal.ui.text.JavaPresentationReconciler;
import org.eclipse.wst.jsdt.internal.ui.text.JavaReconciler;
import org.eclipse.wst.jsdt.internal.ui.text.PreferencesAdapter;
import org.eclipse.wst.jsdt.internal.ui.text.SingleTokenJavaScanner;
import org.eclipse.wst.jsdt.internal.ui.text.comment.CommentFormattingStrategy;
import org.eclipse.wst.jsdt.internal.ui.text.correction.JavaCorrectionAssistant;
import org.eclipse.wst.jsdt.internal.ui.text.html.HTMLTextPresenter;
import org.eclipse.wst.jsdt.internal.ui.text.java.ContentAssistProcessor;
import org.eclipse.wst.jsdt.internal.ui.text.java.JavaAutoIndentStrategy;
import org.eclipse.wst.jsdt.internal.ui.text.java.JavaCodeScanner;
import org.eclipse.wst.jsdt.internal.ui.text.java.JavaCompletionProcessor;
import org.eclipse.wst.jsdt.internal.ui.text.java.JavaDoubleClickSelector;
import org.eclipse.wst.jsdt.internal.ui.text.java.JavaFormattingStrategy;
import org.eclipse.wst.jsdt.internal.ui.text.java.JavaStringAutoIndentStrategy;
import org.eclipse.wst.jsdt.internal.ui.text.java.JavaStringDoubleClickSelector;
import org.eclipse.wst.jsdt.internal.ui.text.java.JavadocDoubleClickStrategy;
import org.eclipse.wst.jsdt.internal.ui.text.java.SmartSemicolonAutoEditStrategy;
import org.eclipse.wst.jsdt.internal.ui.text.java.hover.JavaEditorTextHoverDescriptor;
import org.eclipse.wst.jsdt.internal.ui.text.java.hover.JavaEditorTextHoverProxy;
import org.eclipse.wst.jsdt.internal.ui.text.java.hover.JavaInformationProvider;
import org.eclipse.wst.jsdt.internal.ui.text.javadoc.JavaDocAutoIndentStrategy;
import org.eclipse.wst.jsdt.internal.ui.text.javadoc.JavaDocScanner;
import org.eclipse.wst.jsdt.internal.ui.text.javadoc.JavadocCompletionProcessor;
import org.eclipse.wst.jsdt.internal.ui.typehierarchy.HierarchyInformationControl;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.actions.IJavaEditorActionDefinitionIds;


/**
 * Configuration for a source viewer which shows JavaScript code.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public class JavaScriptSourceViewerConfiguration extends TextSourceViewerConfiguration {

	private JavaScriptTextTools fJavaTextTools;
	private ITextEditor fTextEditor;
	/**
	 * The document partitioning.
	 * 
	 */
	private String fDocumentPartitioning;
	/**
	 * The JavaScript source code scanner.
	 * 
	 */
	private AbstractJavaScanner fCodeScanner;
	/**
	 * The JavaScript multi-line comment scanner.
	 * 
	 */
	private AbstractJavaScanner fMultilineCommentScanner;
	/**
	 * The JavaScript single-line comment scanner.
	 * 
	 */
	private AbstractJavaScanner fSinglelineCommentScanner;
	/**
	 * The JavaScript string scanner.
	 * 
	 */
	private AbstractJavaScanner fStringScanner;
	/**
	 * The Javadoc scanner.
	 * 
	 */
	private AbstractJavaScanner fJavaDocScanner;
	/**
	 * The color manager.
	 * 
	 */
	private IColorManager fColorManager;
	/**
	 * The double click strategy.
	 * 
	 */
	private JavaDoubleClickSelector fJavaDoubleClickSelector;


	/**
	 * Creates a new JavaScript source viewer configuration for viewers in the given editor
	 * using the given preference store, the color manager and the specified document partitioning.
	 * <p>
	 * Creates a JavaScript source viewer configuration in the new setup without text tools. Clients are
	 * allowed to call {@link JavaScriptSourceViewerConfiguration#handlePropertyChangeEvent(PropertyChangeEvent)}
	 * on the resulting
	 * JavaScript source viewer configuration.
	 * </p>
	 *
	 * @param colorManager the color manager
	 * @param preferenceStore the preference store, can be read-only
	 * @param editor the editor in which the configured viewer(s) will reside, or <code>null</code> if none
	 * @param partitioning the document partitioning for this configuration, or <code>null</code> for the default partitioning
	 * 
	 */
	public JavaScriptSourceViewerConfiguration(IColorManager colorManager, IPreferenceStore preferenceStore, ITextEditor editor, String partitioning) {
		super(preferenceStore);
		fColorManager= colorManager;
		fTextEditor= editor;
		fDocumentPartitioning= partitioning;
		initializeScanners();
	}

	/**
	 * Returns the JavaScript source code scanner for this configuration.
	 *
	 * @return the JavaScript source code scanner
	 */
	protected RuleBasedScanner getCodeScanner() {
		return fCodeScanner;
	}

	/**
	 * Returns the JavaScript multi-line comment scanner for this configuration.
	 *
	 * @return the JavaScript multi-line comment scanner
	 * 
	 */
	protected RuleBasedScanner getMultilineCommentScanner() {
		return fMultilineCommentScanner;
	}

	/**
	 * Returns the JavaScript single-line comment scanner for this configuration.
	 *
	 * @return the JavaScript single-line comment scanner
	 * 
	 */
	protected RuleBasedScanner getSinglelineCommentScanner() {
		return fSinglelineCommentScanner;
	}

	/**
	 * Returns the JavaScript string scanner for this configuration.
	 *
	 * @return the JavaScript string scanner
	 * 
	 */
	protected RuleBasedScanner getStringScanner() {
		return fStringScanner;
	}

	/**
	 * Returns the JavaDoc scanner for this configuration.
	 *
	 * @return the JavaDoc scanner
	 */
	protected RuleBasedScanner getJavaDocScanner() {
		return fJavaDocScanner;
	}

	/**
	 * Returns the color manager for this configuration.
	 *
	 * @return the color manager
	 */
	protected IColorManager getColorManager() {
		return fColorManager;
	}

	/**
	 * Returns the editor in which the configured viewer(s) will reside.
	 *
	 * @return the enclosing editor
	 */
	protected ITextEditor getEditor() {
		return fTextEditor;
	}

	/**
	 * @return <code>true</code> iff the new setup without text tools is in use.
	 *
	 * 
	 */
	private boolean isNewSetup() {
		return fJavaTextTools == null;
	}

	/**
	 * Creates and returns a preference store which combines the preference
	 * stores from the text tools and which is read-only.
	 *
	 * @param javaTextTools the JavaScript text tools
	 * @return the combined read-only preference store
	 * 
	 */
	private static final IPreferenceStore createPreferenceStore(JavaScriptTextTools javaTextTools) {
		Assert.isNotNull(javaTextTools);
		IPreferenceStore generalTextStore= EditorsUI.getPreferenceStore();
		if (javaTextTools.getCorePreferenceStore() == null)
			return new ChainedPreferenceStore(new IPreferenceStore[] { javaTextTools.getPreferenceStore(), generalTextStore});

		return new ChainedPreferenceStore(new IPreferenceStore[] { javaTextTools.getPreferenceStore(), new PreferencesAdapter(javaTextTools.getCorePreferenceStore()), generalTextStore });
	}

	/**
	 * Initializes the scanners.
	 *
	 * 
	 */
	private void initializeScanners() {
		Assert.isTrue(isNewSetup());
		fCodeScanner= new JavaCodeScanner(getColorManager(), fPreferenceStore);
		fMultilineCommentScanner= new JavaCommentScanner(getColorManager(), fPreferenceStore, IJavaScriptColorConstants.JAVA_MULTI_LINE_COMMENT);
		fSinglelineCommentScanner= new JavaCommentScanner(getColorManager(), fPreferenceStore, IJavaScriptColorConstants.JAVA_SINGLE_LINE_COMMENT);
		fStringScanner= new SingleTokenJavaScanner(getColorManager(), fPreferenceStore, IJavaScriptColorConstants.JAVA_STRING);
		fJavaDocScanner= new JavaDocScanner(getColorManager(), fPreferenceStore);
	}

	/*
	 * @see SourceViewerConfiguration#getPresentationReconciler(ISourceViewer)
	 */
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {

		PresentationReconciler reconciler= new JavaPresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		DefaultDamagerRepairer dr= new DefaultDamagerRepairer(getCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr= new DefaultDamagerRepairer(getJavaDocScanner());
		reconciler.setDamager(dr, IJavaScriptPartitions.JAVA_DOC);
		reconciler.setRepairer(dr, IJavaScriptPartitions.JAVA_DOC);

		dr= new DefaultDamagerRepairer(getMultilineCommentScanner());
		reconciler.setDamager(dr, IJavaScriptPartitions.JAVA_MULTI_LINE_COMMENT);
		reconciler.setRepairer(dr, IJavaScriptPartitions.JAVA_MULTI_LINE_COMMENT);

		dr= new DefaultDamagerRepairer(getSinglelineCommentScanner());
		reconciler.setDamager(dr, IJavaScriptPartitions.JAVA_SINGLE_LINE_COMMENT);
		reconciler.setRepairer(dr, IJavaScriptPartitions.JAVA_SINGLE_LINE_COMMENT);

		dr= new DefaultDamagerRepairer(getStringScanner());
		reconciler.setDamager(dr, IJavaScriptPartitions.JAVA_STRING);
		reconciler.setRepairer(dr, IJavaScriptPartitions.JAVA_STRING);

		dr= new DefaultDamagerRepairer(getStringScanner());
		reconciler.setDamager(dr, IJavaScriptPartitions.JAVA_CHARACTER);
		reconciler.setRepairer(dr, IJavaScriptPartitions.JAVA_CHARACTER);


		return reconciler;
	}

	/*
	 * @see SourceViewerConfiguration#getContentAssistant(ISourceViewer)
	 */
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {

		if (getEditor() != null) {

			ContentAssistant assistant= new ContentAssistant();
			assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

			assistant.setRestoreCompletionProposalSize(getSettings("completion_proposal_size")); //$NON-NLS-1$

			IContentAssistProcessor javaProcessor= new JavaCompletionProcessor(getEditor(), assistant, IDocument.DEFAULT_CONTENT_TYPE);
			assistant.setContentAssistProcessor(javaProcessor, IDocument.DEFAULT_CONTENT_TYPE);

			ContentAssistProcessor singleLineProcessor= new JavaCompletionProcessor(getEditor(), assistant, IJavaScriptPartitions.JAVA_SINGLE_LINE_COMMENT);
			assistant.setContentAssistProcessor(singleLineProcessor, IJavaScriptPartitions.JAVA_SINGLE_LINE_COMMENT);

			ContentAssistProcessor stringProcessor= new JavaCompletionProcessor(getEditor(), assistant, IJavaScriptPartitions.JAVA_STRING);
			assistant.setContentAssistProcessor(stringProcessor, IJavaScriptPartitions.JAVA_STRING);
			
			assistant.setContentAssistProcessor(stringProcessor, IJavaScriptPartitions.JAVA_CHARACTER);
			
			ContentAssistProcessor multiLineProcessor= new JavaCompletionProcessor(getEditor(), assistant, IJavaScriptPartitions.JAVA_MULTI_LINE_COMMENT);
			assistant.setContentAssistProcessor(multiLineProcessor, IJavaScriptPartitions.JAVA_MULTI_LINE_COMMENT);

			ContentAssistProcessor javadocProcessor= new JavadocCompletionProcessor(getEditor(), assistant);
			assistant.setContentAssistProcessor(javadocProcessor, IJavaScriptPartitions.JAVA_DOC);

			ContentAssistPreference.configure(assistant, fPreferenceStore);

			assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
			assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));
			
			return assistant;
		}

		return null;
	}

	/*
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getQuickAssistAssistant(org.eclipse.jface.text.source.ISourceViewer)
	 * 
	 */
	public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
		if (getEditor() != null)
			return new JavaCorrectionAssistant(getEditor());
		return null;
	}

	/*
	 * @see SourceViewerConfiguration#getReconciler(ISourceViewer)
	 */
	public IReconciler getReconciler(ISourceViewer sourceViewer) {

		final ITextEditor editor= getEditor();
		if (editor != null && editor.isEditable()) {

			JavaCompositeReconcilingStrategy strategy= new JavaCompositeReconcilingStrategy(sourceViewer, editor, getConfiguredDocumentPartitioning(sourceViewer));
			JavaReconciler reconciler= new JavaReconciler(editor, strategy, false);
			reconciler.setIsIncrementalReconciler(false);
			reconciler.setIsAllowedToModifyDocument(false);
			reconciler.setProgressMonitor(new NullProgressMonitor());
			reconciler.setDelay(500);

			return reconciler;
		}
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getAutoEditStrategies(org.eclipse.jface.text.source.ISourceViewer, java.lang.String)
	 */
	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
		String partitioning= getConfiguredDocumentPartitioning(sourceViewer);
		if (IJavaScriptPartitions.JAVA_DOC.equals(contentType) || IJavaScriptPartitions.JAVA_MULTI_LINE_COMMENT.equals(contentType))
			return new IAutoEditStrategy[] { new JavaDocAutoIndentStrategy(partitioning) };
		else if (IJavaScriptPartitions.JAVA_STRING.equals(contentType))
			return new IAutoEditStrategy[] { new SmartSemicolonAutoEditStrategy(partitioning), new JavaStringAutoIndentStrategy(partitioning) };
		else if (IJavaScriptPartitions.JAVA_CHARACTER.equals(contentType) || IDocument.DEFAULT_CONTENT_TYPE.equals(contentType))
			return new IAutoEditStrategy[] { new SmartSemicolonAutoEditStrategy(partitioning), new JavaAutoIndentStrategy(partitioning, getProject(), sourceViewer) };
		else
			return new IAutoEditStrategy[] { new JavaAutoIndentStrategy(partitioning, getProject(), sourceViewer) };
	}

	/*
	 * @see SourceViewerConfiguration#getDoubleClickStrategy(ISourceViewer, String)
	 */
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		if (IJavaScriptPartitions.JAVA_DOC.equals(contentType))
			return new JavadocDoubleClickStrategy();
		if (IJavaScriptPartitions.JAVA_MULTI_LINE_COMMENT.equals(contentType) ||
				IJavaScriptPartitions.JAVA_SINGLE_LINE_COMMENT.equals(contentType))
			return new DefaultTextDoubleClickStrategy();
		else if (IJavaScriptPartitions.JAVA_STRING.equals(contentType) ||
				IJavaScriptPartitions.JAVA_CHARACTER.equals(contentType))
			return new JavaStringDoubleClickSelector(getConfiguredDocumentPartitioning(sourceViewer));
		if (fJavaDoubleClickSelector == null) {
			fJavaDoubleClickSelector= new JavaDoubleClickSelector();
			fJavaDoubleClickSelector.setSourceVersion(fPreferenceStore.getString(JavaScriptCore.COMPILER_SOURCE));
		}
		return fJavaDoubleClickSelector;
	}

	/*
	 * @see SourceViewerConfiguration#getDefaultPrefixes(ISourceViewer, String)
	 * 
	 */
	public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
		return new String[] { "//", "" }; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * @see SourceViewerConfiguration#getIndentPrefixes(ISourceViewer, String)
	 */
	public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
 		IJavaScriptProject project= getProject();
		final int tabWidth= CodeFormatterUtil.getTabWidth(project);
		final int indentWidth= CodeFormatterUtil.getIndentWidth(project);
		boolean allowTabs= tabWidth <= indentWidth;
		
		String indentMode;
		if (project == null)
			indentMode= JavaScriptCore.getOption(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR);
		else
			indentMode= project.getOption(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, true);

		boolean useSpaces= JavaScriptCore.SPACE.equals(indentMode) || DefaultCodeFormatterConstants.MIXED.equals(indentMode);
		
		Assert.isLegal(allowTabs || useSpaces);
		
		if (!allowTabs) {
			char[] spaces= new char[indentWidth];
			Arrays.fill(spaces, ' ');
			return new String[] { new String(spaces), "" }; //$NON-NLS-1$
		} else if  (!useSpaces)
			return getIndentPrefixesForTab(tabWidth);
		else
			return getIndentPrefixesForSpaces(tabWidth);
	}

	/**
	 * Computes and returns the indent prefixes for space indentation
	 * and the given <code>tabWidth</code>.
	 * 
	 * @param tabWidth the display tab width
	 * @return the indent prefixes
	 * @see #getIndentPrefixes(ISourceViewer, String)
	 * 
	 */
	private String[] getIndentPrefixesForSpaces(int tabWidth) {
		String[] indentPrefixes= new String[tabWidth + 2];
		indentPrefixes[0]= getStringWithSpaces(tabWidth);
		
		for (int i= 0; i < tabWidth; i++) {
			String spaces= getStringWithSpaces(i);
			if (i < tabWidth)
				indentPrefixes[i+1]= spaces + '\t';
			else
				indentPrefixes[i+1]= new String(spaces);
		}
		
		indentPrefixes[tabWidth + 1]= ""; //$NON-NLS-1$

		return indentPrefixes;
	}

	/**
	 * Creates and returns a String with <code>count</code> spaces.
	 * 
	 * @param count	the space count
	 * @return the string with the spaces
	 * 
	 */
	private String getStringWithSpaces(int count) {
		char[] spaceChars= new char[count];
		Arrays.fill(spaceChars, ' ');
		return new String(spaceChars);
	}

	private IJavaScriptProject getProject() {
		ITextEditor editor= getEditor();
		if (editor == null)
			return null;

		IJavaScriptElement element= null;
		IEditorInput input= editor.getEditorInput();
		IDocumentProvider provider= editor.getDocumentProvider();
		if (provider instanceof ICompilationUnitDocumentProvider) {
			ICompilationUnitDocumentProvider cudp= (ICompilationUnitDocumentProvider) provider;
			element= cudp.getWorkingCopy(input);
		} else if (input instanceof IClassFileEditorInput) {
			IClassFileEditorInput cfei= (IClassFileEditorInput) input;
			element= cfei.getClassFile();
		}

		if (element == null)
			return null;

		return element.getJavaScriptProject();
	}

	/*
	 * @see SourceViewerConfiguration#getTabWidth(ISourceViewer)
	 */
	public int getTabWidth(ISourceViewer sourceViewer) {
		return CodeFormatterUtil.getTabWidth(getProject());
	}

	/*
	 * @see SourceViewerConfiguration#getAnnotationHover(ISourceViewer)
	 */
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return new HTMLAnnotationHover() {
			protected boolean isIncluded(Annotation annotation) {
				return isShowInVerticalRuler(annotation);
			}
		};
	}

	/*
	 * @see SourceViewerConfiguration#getOverviewRulerAnnotationHover(ISourceViewer)
	 * 
	 */
	public IAnnotationHover getOverviewRulerAnnotationHover(ISourceViewer sourceViewer) {
		return new HTMLAnnotationHover() {
			protected boolean isIncluded(Annotation annotation) {
				return isShowInOverviewRuler(annotation);
			}
		};
	}

	/*
	 * @see SourceViewerConfiguration#getConfiguredTextHoverStateMasks(ISourceViewer, String)
	 * 
	 */
	public int[] getConfiguredTextHoverStateMasks(ISourceViewer sourceViewer, String contentType) {
		JavaEditorTextHoverDescriptor[] hoverDescs= JavaScriptPlugin.getDefault().getJavaEditorTextHoverDescriptors();
		int stateMasks[]= new int[hoverDescs.length];
		int stateMasksLength= 0;
		for (int i= 0; i < hoverDescs.length; i++) {
			if (hoverDescs[i].isEnabled()) {
				int j= 0;
				int stateMask= hoverDescs[i].getStateMask();
				while (j < stateMasksLength) {
					if (stateMasks[j] == stateMask)
						break;
					j++;
				}
				if (j == stateMasksLength)
					stateMasks[stateMasksLength++]= stateMask;
			}
		}
		if (stateMasksLength == hoverDescs.length)
			return stateMasks;

		int[] shortenedStateMasks= new int[stateMasksLength];
		System.arraycopy(stateMasks, 0, shortenedStateMasks, 0, stateMasksLength);
		return shortenedStateMasks;
	}

	/*
	 * @see SourceViewerConfiguration#getTextHover(ISourceViewer, String, int)
	 * 
	 */
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
		JavaEditorTextHoverDescriptor[] hoverDescs= JavaScriptPlugin.getDefault().getJavaEditorTextHoverDescriptors();
		int i= 0;
		while (i < hoverDescs.length) {
			if (hoverDescs[i].isEnabled() &&  hoverDescs[i].getStateMask() == stateMask)
				return new JavaEditorTextHoverProxy(hoverDescs[i], getEditor());
			i++;
		}

		return null;
	}

	/*
	 * @see SourceViewerConfiguration#getTextHover(ISourceViewer, String)
	 */
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		return getTextHover(sourceViewer, contentType, ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK);
	}

	/*
	 * @see SourceViewerConfiguration#getConfiguredContentTypes(ISourceViewer)
	 */
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {
			IDocument.DEFAULT_CONTENT_TYPE,
			IJavaScriptPartitions.JAVA_DOC,
			IJavaScriptPartitions.JAVA_MULTI_LINE_COMMENT,
			IJavaScriptPartitions.JAVA_SINGLE_LINE_COMMENT,
			IJavaScriptPartitions.JAVA_STRING,
			IJavaScriptPartitions.JAVA_CHARACTER
		};
	}

	/*
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getConfiguredDocumentPartitioning(org.eclipse.jface.text.source.ISourceViewer)
	 * 
	 */
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		if (fDocumentPartitioning != null)
			return fDocumentPartitioning;
		return super.getConfiguredDocumentPartitioning(sourceViewer);
	}

	/*
	 * @see SourceViewerConfiguration#getContentFormatter(ISourceViewer)
	 */
	public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
		final MultiPassContentFormatter formatter= new MultiPassContentFormatter(getConfiguredDocumentPartitioning(sourceViewer), IDocument.DEFAULT_CONTENT_TYPE);

		formatter.setMasterStrategy(new JavaFormattingStrategy());
		formatter.setSlaveStrategy(new CommentFormattingStrategy(), IJavaScriptPartitions.JAVA_DOC);
		formatter.setSlaveStrategy(new CommentFormattingStrategy(), IJavaScriptPartitions.JAVA_SINGLE_LINE_COMMENT);
		formatter.setSlaveStrategy(new CommentFormattingStrategy(), IJavaScriptPartitions.JAVA_MULTI_LINE_COMMENT);

		return formatter;
	}

	/*
	 * @see SourceViewerConfiguration#getInformationControlCreator(ISourceViewer)
	 * 
	 */
	public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new DefaultInformationControl(parent, EditorsUI.getTooltipAffordanceString());
			}
		};
	}

	/**
	 * Returns the information presenter control creator. The creator is a factory creating the
	 * presenter controls for the given source viewer. This implementation always returns a creator
	 * for <code>DefaultInformationControl</code> instances.
	 *
	 * @param sourceViewer the source viewer to be configured by this configuration
	 * @return an information control creator
	 * 
	 */
	private IInformationControlCreator getInformationPresenterControlCreator(ISourceViewer sourceViewer) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				int shellStyle= SWT.RESIZE | SWT.TOOL;
				int style= SWT.V_SCROLL | SWT.H_SCROLL;
				return new DefaultInformationControl(parent, shellStyle, style, new HTMLTextPresenter(false));
			}
		};
	}

	/**
	 * Returns the outline presenter control creator. The creator is a factory creating outline
	 * presenter controls for the given source viewer. This implementation always returns a creator
	 * for <code>JavaOutlineInformationControl</code> instances.
	 *
	 * @param sourceViewer the source viewer to be configured by this configuration
	 * @param commandId the ID of the command that opens this control
	 * @return an information control creator
	 * 
	 */
	private IInformationControlCreator getOutlinePresenterControlCreator(ISourceViewer sourceViewer, final String commandId) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				int shellStyle= SWT.RESIZE;
				int treeStyle= SWT.V_SCROLL | SWT.H_SCROLL;
				return new JavaOutlineInformationControl(parent, shellStyle, treeStyle, commandId);
			}
		};
	}

	private IInformationControlCreator getHierarchyPresenterControlCreator(ISourceViewer sourceViewer) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				int shellStyle= SWT.RESIZE;
				int treeStyle= SWT.V_SCROLL | SWT.H_SCROLL;
				return new HierarchyInformationControl(parent, shellStyle, treeStyle);
			}
		};
	}

	/*
	 * @see SourceViewerConfiguration#getInformationPresenter(ISourceViewer)
	 * 
	 */
	public IInformationPresenter getInformationPresenter(ISourceViewer sourceViewer) {
		InformationPresenter presenter= new InformationPresenter(getInformationPresenterControlCreator(sourceViewer));
		presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		
		// Register information provider
		IInformationProvider provider= new JavaInformationProvider(getEditor());
		String[] contentTypes= getConfiguredContentTypes(sourceViewer);
		for (int i= 0; i < contentTypes.length; i++)
			presenter.setInformationProvider(provider, contentTypes[i]);
		
		presenter.setSizeConstraints(60, 10, true, true);
		return presenter;
	}

	/**
	 * Returns the outline presenter which will determine and shown
	 * information requested for the current cursor position.
	 *
	 * @param sourceViewer the source viewer to be configured by this configuration
	 * @param doCodeResolve a boolean which specifies whether code resolve should be used to compute the JavaScript element
	 * @return an information presenter
	 * 
	 */
	public IInformationPresenter getOutlinePresenter(ISourceViewer sourceViewer, boolean doCodeResolve) {
		InformationPresenter presenter;
		if (doCodeResolve)
			presenter= new InformationPresenter(getOutlinePresenterControlCreator(sourceViewer, IJavaEditorActionDefinitionIds.OPEN_STRUCTURE));
		else
			presenter= new InformationPresenter(getOutlinePresenterControlCreator(sourceViewer, IJavaEditorActionDefinitionIds.SHOW_OUTLINE));
		presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		presenter.setAnchor(AbstractInformationControlManager.ANCHOR_GLOBAL);
		IInformationProvider provider= new JavaElementProvider(getEditor(), doCodeResolve);
		presenter.setInformationProvider(provider, IDocument.DEFAULT_CONTENT_TYPE);
		presenter.setInformationProvider(provider, IJavaScriptPartitions.JAVA_DOC);
		presenter.setInformationProvider(provider, IJavaScriptPartitions.JAVA_MULTI_LINE_COMMENT);
		presenter.setInformationProvider(provider, IJavaScriptPartitions.JAVA_SINGLE_LINE_COMMENT);
		presenter.setInformationProvider(provider, IJavaScriptPartitions.JAVA_STRING);
		presenter.setInformationProvider(provider, IJavaScriptPartitions.JAVA_CHARACTER);
		presenter.setSizeConstraints(50, 20, true, false);
		return presenter;
	}

	/**
	 * Returns the settings for the given section.
	 *
	 * @param sectionName the section name
	 * @return the settings
	 * 
	 */
	private IDialogSettings getSettings(String sectionName) {
		IDialogSettings settings= JavaScriptPlugin.getDefault().getDialogSettings().getSection(sectionName);
		if (settings == null)
			settings= JavaScriptPlugin.getDefault().getDialogSettings().addNewSection(sectionName);

		return settings;
	}

	/**
	 * Returns the hierarchy presenter which will determine and shown type hierarchy
	 * information requested for the current cursor position.
	 *
	 * @param sourceViewer the source viewer to be configured by this configuration
	 * @param doCodeResolve a boolean which specifies whether code resolve should be used to compute the JavaScript element
	 * @return an information presenter
	 * 
	 */
	public IInformationPresenter getHierarchyPresenter(ISourceViewer sourceViewer, boolean doCodeResolve) {
		
		// Do not create hierarchy presenter if there's no CU.
		if (getEditor() != null && getEditor().getEditorInput() != null && JavaScriptUI.getEditorInputJavaElement(getEditor().getEditorInput()) == null)
			return null;
		
		InformationPresenter presenter= new InformationPresenter(getHierarchyPresenterControlCreator(sourceViewer));
		presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		presenter.setAnchor(AbstractInformationControlManager.ANCHOR_GLOBAL);
		IInformationProvider provider= new JavaElementProvider(getEditor(), doCodeResolve);
		presenter.setInformationProvider(provider, IDocument.DEFAULT_CONTENT_TYPE);
		presenter.setInformationProvider(provider, IJavaScriptPartitions.JAVA_DOC);
		presenter.setInformationProvider(provider, IJavaScriptPartitions.JAVA_MULTI_LINE_COMMENT);
		presenter.setInformationProvider(provider, IJavaScriptPartitions.JAVA_SINGLE_LINE_COMMENT);
		presenter.setInformationProvider(provider, IJavaScriptPartitions.JAVA_STRING);
		presenter.setInformationProvider(provider, IJavaScriptPartitions.JAVA_CHARACTER);
		presenter.setSizeConstraints(50, 20, true, false);
		return presenter;
	}

	/**
	 * Determines whether the preference change encoded by the given event
	 * changes the behavior of one of its contained components.
	 *
	 * @param event the event to be investigated
	 * @return <code>true</code> if event causes a behavioral change
	 * 
	 */
	public boolean affectsTextPresentation(PropertyChangeEvent event) {
		return  fCodeScanner.affectsBehavior(event)
			|| fMultilineCommentScanner.affectsBehavior(event)
			|| fSinglelineCommentScanner.affectsBehavior(event)
			|| fStringScanner.affectsBehavior(event)
			|| fJavaDocScanner.affectsBehavior(event);
	}

	/**
	 * Adapts the behavior of the contained components to the change
	 * encoded in the given event.
	 * <p>
	 * Clients are not allowed to call this method if the old setup with
	 * text tools is in use.
	 * </p>
	 *
	 * @param event the event to which to adapt
	 * @see JavaScriptSourceViewerConfiguration#JavaSourceViewerConfiguration(IColorManager, IPreferenceStore, ITextEditor, String)
	 * 
	 */
	public void handlePropertyChangeEvent(PropertyChangeEvent event) {
		Assert.isTrue(isNewSetup());
		if (fCodeScanner.affectsBehavior(event))
			fCodeScanner.adaptToPreferenceChange(event);
		if (fMultilineCommentScanner.affectsBehavior(event))
			fMultilineCommentScanner.adaptToPreferenceChange(event);
		if (fSinglelineCommentScanner.affectsBehavior(event))
			fSinglelineCommentScanner.adaptToPreferenceChange(event);
		if (fStringScanner.affectsBehavior(event))
			fStringScanner.adaptToPreferenceChange(event);
		if (fJavaDocScanner.affectsBehavior(event))
			fJavaDocScanner.adaptToPreferenceChange(event);
		if (fJavaDoubleClickSelector != null && JavaScriptCore.COMPILER_SOURCE.equals(event.getProperty()))
			if (event.getNewValue() instanceof String)
				fJavaDoubleClickSelector.setSourceVersion((String) event.getNewValue());
	}

	/*
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getHyperlinkDetectorTargets(org.eclipse.jface.text.source.ISourceViewer)
	 * 
	 */
	protected Map getHyperlinkDetectorTargets(ISourceViewer sourceViewer) {
		Map targets= super.getHyperlinkDetectorTargets(sourceViewer);
		targets.put("org.eclipse.wst.jsdt.ui.javaCode", fTextEditor); //$NON-NLS-1$
		return targets;
	}

}
