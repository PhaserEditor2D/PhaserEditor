/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Tom Eicher (Avaloq Evolution AG) - block selection mode
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.javaeditor;

import static java.lang.System.currentTimeMillis;

import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.operations.IOperationApprover;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISelectionValidator;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationRulerColumn;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.ISourceViewerExtension2;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.IVerticalRulerColumn;
import org.eclipse.jface.text.source.LineChangeHover;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.editors.text.DefaultEncodingSupport;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.IEncodingSupport;
import org.eclipse.ui.operations.NonLocalUndoUserApprover;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.TextNavigationAction;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IImportContainer;
import org.eclipse.wst.jsdt.core.IImportDeclaration;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.IVariableBinding;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.wst.jsdt.core.util.IModifierConstants;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.IProductConstants;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.ProductProperties;
import org.eclipse.wst.jsdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.wst.jsdt.internal.ui.actions.CopyQualifiedNameAction;
import org.eclipse.wst.jsdt.internal.ui.actions.FoldingActionGroup;
import org.eclipse.wst.jsdt.internal.ui.actions.SelectionConverter;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.selectionactions.GoToNextPreviousMemberAction;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.selectionactions.SelectionHistory;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.selectionactions.StructureSelectEnclosingAction;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.selectionactions.StructureSelectHistoryAction;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.selectionactions.StructureSelectNextAction;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.selectionactions.StructureSelectPreviousAction;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.selectionactions.StructureSelectionAction;
import org.eclipse.wst.jsdt.internal.ui.search.BreakContinueTargetFinder;
import org.eclipse.wst.jsdt.internal.ui.search.ExceptionOccurrencesFinder;
import org.eclipse.wst.jsdt.internal.ui.search.ImplementOccurrencesFinder;
import org.eclipse.wst.jsdt.internal.ui.search.MethodExitsFinder;
import org.eclipse.wst.jsdt.internal.ui.search.OccurrencesFinder;
import org.eclipse.wst.jsdt.internal.ui.text.DocumentCharacterIterator;
import org.eclipse.wst.jsdt.internal.ui.text.JavaChangeHover;
import org.eclipse.wst.jsdt.internal.ui.text.JavaPairMatcher;
import org.eclipse.wst.jsdt.internal.ui.text.JavaWordFinder;
import org.eclipse.wst.jsdt.internal.ui.text.JavaWordIterator;
import org.eclipse.wst.jsdt.internal.ui.text.PreferencesAdapter;
import org.eclipse.wst.jsdt.internal.ui.text.java.hover.JavaExpandHover;
import org.eclipse.wst.jsdt.internal.ui.text.java.hover.SourceViewerInformationControl;
import org.eclipse.wst.jsdt.internal.ui.util.JavaUIHelp;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ISelectionListenerWithAST;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.IViewPartInputProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.SelectionListenerWithASTManager;
import org.eclipse.wst.jsdt.ui.IContextMenuConstants;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.wst.jsdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.wst.jsdt.ui.actions.OpenEditorActionGroup;
import org.eclipse.wst.jsdt.ui.actions.OpenViewActionGroup;
import org.eclipse.wst.jsdt.ui.actions.ShowInPackageViewAction;
import org.eclipse.wst.jsdt.ui.text.IJavaScriptPartitions;
import org.eclipse.wst.jsdt.ui.text.JavaScriptSourceViewerConfiguration;
import org.eclipse.wst.jsdt.ui.text.JavaScriptTextTools;
import org.eclipse.wst.jsdt.ui.text.folding.IJavaFoldingStructureProvider;
import org.eclipse.wst.jsdt.ui.text.folding.IJavaFoldingStructureProviderExtension;
import org.osgi.service.prefs.BackingStoreException;

import com.ibm.icu.text.BreakIterator;

/**
 * Java specific text editor.
 */
public abstract class JavaEditor extends AbstractDecoratedTextEditor implements IViewPartInputProvider {

	/**
	 * Internal implementation class for a change listener.
	 * 
	 */
	protected abstract class AbstractSelectionChangedListener implements ISelectionChangedListener {

		/**
		 * Installs this selection changed listener with the given selection
		 * provider. If the selection provider is a post selection provider,
		 * post selection changed events are the preferred choice, otherwise
		 * normal selection changed events are requested.
		 *
		 * @param selectionProvider
		 */
		public void install(ISelectionProvider selectionProvider) {
			if (selectionProvider == null)
				return;

			if (selectionProvider instanceof IPostSelectionProvider) {
				IPostSelectionProvider provider = (IPostSelectionProvider) selectionProvider;
				provider.addPostSelectionChangedListener(this);
			}
			else {
				selectionProvider.addSelectionChangedListener(this);
			}
		}

		/**
		 * Removes this selection changed listener from the given selection
		 * provider.
		 *
		 * @param selectionProvider
		 *            the selection provider
		 */
		public void uninstall(ISelectionProvider selectionProvider) {
			if (selectionProvider == null)
				return;

			if (selectionProvider instanceof IPostSelectionProvider) {
				IPostSelectionProvider provider = (IPostSelectionProvider) selectionProvider;
				provider.removePostSelectionChangedListener(this);
			}
			else {
				selectionProvider.removeSelectionChangedListener(this);
			}
		}
	}

	/**
	 * Updates the Java outline page selection and this editor's range
	 * indicator.
	 *
	 * 
	 */
	private class EditorSelectionChangedListener extends AbstractSelectionChangedListener {

		/*
		 * @see org.eclipse.jface.viewers.ISelectionChangedListener#
		 * selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
		 */
		public void selectionChanged(SelectionChangedEvent event) {
			// XXX: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=56161
			JavaEditor.this.selectionChanged();
		}
	}

	/**
	 * Updates the selection in the editor's widget with the selection of the
	 * outline page.
	 */
	class OutlineSelectionChangedListener extends AbstractSelectionChangedListener {
		public void selectionChanged(SelectionChangedEvent event) {
			doSelectionChanged(event);
		}
	}


	/**
	 * Adapts an options {@link IEclipsePreferences} to
	 * {@link org.eclipse.jface.preference.IPreferenceStore}.
	 * <p>
	 * This preference store is read-only i.e. write access throws an
	 * {@link java.lang.UnsupportedOperationException}.
	 * </p>
	 *
	 * 
	 */
	private static class EclipsePreferencesAdapter implements IPreferenceStore {

		/**
		 * Preference change listener. Listens for events preferences fires a
		 * {@link org.eclipse.jface.util.PropertyChangeEvent} on this adapter
		 * with arguments from the received event.
		 */
		private class PreferenceChangeListener implements IEclipsePreferences.IPreferenceChangeListener {

			/**
			 * {@inheritDoc}
			 */
			public void preferenceChange(final IEclipsePreferences.PreferenceChangeEvent event) {
				if (Display.getCurrent() == null) {
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							firePropertyChangeEvent(event.getKey(), event.getOldValue(), event.getNewValue());
						}
					});
				}
				else {
					firePropertyChangeEvent(event.getKey(), event.getOldValue(), event.getNewValue());
				}
			}
		}

		/** Listeners on on this adapter */
		private ListenerList fListeners = new ListenerList(ListenerList.IDENTITY);

		/** Listener on the node */
		private IEclipsePreferences.IPreferenceChangeListener fListener = new PreferenceChangeListener();

		/** wrapped node */
		private final IScopeContext fContext;
		private final String fQualifier;

		/**
		 * Initialize with the node to wrap
		 *
		 * @param context
		 *            the context to access
		 * @param qualifier
		 *            the qualifier
		 */
		public EclipsePreferencesAdapter(IScopeContext context, String qualifier) {
			fContext = context;
			fQualifier = qualifier;
		}

		private IEclipsePreferences getNode() {
			return fContext.getNode(fQualifier);
		}

		/**
		 * {@inheritDoc}
		 */
		public void addPropertyChangeListener(IPropertyChangeListener listener) {
			if (fListeners.size() == 0)
				getNode().addPreferenceChangeListener(fListener);
			fListeners.add(listener);
		}

		/**
		 * {@inheritDoc}
		 */
		public void removePropertyChangeListener(IPropertyChangeListener listener) {
			fListeners.remove(listener);
			if (fListeners.size() == 0) {
				getNode().removePreferenceChangeListener(fListener);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean contains(String name) {
			return getNode().get(name, null) != null;
		}

		/**
		 * {@inheritDoc}
		 */
		public void firePropertyChangeEvent(String name, Object oldValue, Object newValue) {
			PropertyChangeEvent event = new PropertyChangeEvent(this, name, oldValue, newValue);
			Object[] listeners = fListeners.getListeners();
			for (int i = 0; i < listeners.length; i++)
				((IPropertyChangeListener) listeners[i]).propertyChange(event);
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean getBoolean(String name) {
			return getNode().getBoolean(name, BOOLEAN_DEFAULT_DEFAULT);
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean getDefaultBoolean(String name) {
			return BOOLEAN_DEFAULT_DEFAULT;
		}

		/**
		 * {@inheritDoc}
		 */
		public double getDefaultDouble(String name) {
			return DOUBLE_DEFAULT_DEFAULT;
		}

		/**
		 * {@inheritDoc}
		 */
		public float getDefaultFloat(String name) {
			return FLOAT_DEFAULT_DEFAULT;
		}

		/**
		 * {@inheritDoc}
		 */
		public int getDefaultInt(String name) {
			return INT_DEFAULT_DEFAULT;
		}

		/**
		 * {@inheritDoc}
		 */
		public long getDefaultLong(String name) {
			return LONG_DEFAULT_DEFAULT;
		}

		/**
		 * {@inheritDoc}
		 */
		public String getDefaultString(String name) {
			return STRING_DEFAULT_DEFAULT;
		}

		/**
		 * {@inheritDoc}
		 */
		public double getDouble(String name) {
			return getNode().getDouble(name, DOUBLE_DEFAULT_DEFAULT);
		}

		/**
		 * {@inheritDoc}
		 */
		public float getFloat(String name) {
			return getNode().getFloat(name, FLOAT_DEFAULT_DEFAULT);
		}

		/**
		 * {@inheritDoc}
		 */
		public int getInt(String name) {
			return getNode().getInt(name, INT_DEFAULT_DEFAULT);
		}

		/**
		 * {@inheritDoc}
		 */
		public long getLong(String name) {
			return getNode().getLong(name, LONG_DEFAULT_DEFAULT);
		}

		/**
		 * {@inheritDoc}
		 */
		public String getString(String name) {
			return getNode().get(name, STRING_DEFAULT_DEFAULT);
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean isDefault(String name) {
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean needsSaving() {
			try {
				return getNode().keys().length > 0;
			}
			catch (BackingStoreException e) {
				// ignore
			}
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		public void putValue(String name, String value) {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public void setDefault(String name, double value) {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public void setDefault(String name, float value) {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public void setDefault(String name, int value) {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public void setDefault(String name, long value) {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public void setDefault(String name, String defaultObject) {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public void setDefault(String name, boolean value) {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public void setToDefault(String name) {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public void setValue(String name, double value) {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public void setValue(String name, float value) {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public void setValue(String name, int value) {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public void setValue(String name, long value) {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public void setValue(String name, String value) {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public void setValue(String name, boolean value) {
			throw new UnsupportedOperationException();
		}

	}


	/**
	 * Cancels the occurrences finder job upon document changes.
	 *
	 * 
	 */
	class OccurrencesFinderJobCanceler implements IDocumentListener, ITextInputListener {

		public void install() {
			ISourceViewer sourceViewer = getSourceViewer();
			if (sourceViewer == null)
				return;

			StyledText text = sourceViewer.getTextWidget();
			if (text == null || text.isDisposed())
				return;

			sourceViewer.addTextInputListener(this);

			IDocument document = sourceViewer.getDocument();
			if (document != null)
				document.addDocumentListener(this);
		}

		public void uninstall() {
			ISourceViewer sourceViewer = getSourceViewer();
			if (sourceViewer != null)
				sourceViewer.removeTextInputListener(this);

			IDocumentProvider documentProvider = getDocumentProvider();
			if (documentProvider != null) {
				IDocument document = documentProvider.getDocument(getEditorInput());
				if (document != null)
					document.removeDocumentListener(this);
			}
		}


		/*
		 * @see
		 * org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(
		 * org.eclipse.jface.text.DocumentEvent)
		 */
		public void documentAboutToBeChanged(DocumentEvent event) {
			if (fOccurrencesFinderJob != null)
				fOccurrencesFinderJob.doCancel();
		}

		/*
		 * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.
		 * eclipse.jface.text.DocumentEvent)
		 */
		public void documentChanged(DocumentEvent event) {
		}

		/*
		 * @see org.eclipse.jface.text.ITextInputListener#
		 * inputDocumentAboutToBeChanged(org.eclipse.jface.text.IDocument,
		 * org.eclipse.jface.text.IDocument)
		 */
		public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
			if (oldInput == null)
				return;

			oldInput.removeDocumentListener(this);
		}

		/*
		 * @see
		 * org.eclipse.jface.text.ITextInputListener#inputDocumentChanged(org.
		 * eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocument)
		 */
		public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
			if (newInput == null)
				return;
			newInput.addDocumentListener(this);
		}
	}

	/**
	 * This action implements smart home.
	 *
	 * Instead of going to the start of a line it does the following:
	 *
	 * - if smart home/end is enabled and the caret is after the line's first
	 * non-whitespace then the caret is moved directly before it, taking
	 * JavaDoc and multi-line comments into account. - if the caret is before
	 * the line's first non-whitespace the caret is moved to the beginning of
	 * the line - if the caret is at the beginning of the line see first case.
	 *
	 * 
	 */
	protected class SmartLineStartAction extends LineStartAction {

		/**
		 * Creates a new smart line start action
		 *
		 * @param textWidget
		 *            the styled text widget
		 * @param doSelect
		 *            a boolean flag which tells if the text up to the
		 *            beginning of the line should be selected
		 */
		public SmartLineStartAction(final StyledText textWidget, final boolean doSelect) {
			super(textWidget, doSelect);
		}

		/*
		 * @see org.eclipse.ui.texteditor.AbstractTextEditor.LineStartAction#
		 * getLineStartPosition(java.lang.String, int, java.lang.String)
		 */
		protected int getLineStartPosition(final IDocument document, final String line, final int length, final int offset) {

			String type = IDocument.DEFAULT_CONTENT_TYPE;
			try {
				type = TextUtilities.getContentType(document, IJavaScriptPartitions.JAVA_PARTITIONING, offset, true);
			}
			catch (BadLocationException exception) {
				// Should not happen
			}

			int index = super.getLineStartPosition(document, line, length, offset);
			if (type.equals(IJavaScriptPartitions.JAVA_DOC) || type.equals(IJavaScriptPartitions.JAVA_MULTI_LINE_COMMENT)) {
				if (index < length - 1 && line.charAt(index) == '*' && line.charAt(index + 1) != '/') {
					do {
						++index;
					}
					while (index < length && Character.isWhitespace(line.charAt(index)));
				}
			}
			else {
				if (index < length - 1 && line.charAt(index) == '/' && line.charAt(index + 1) == '/') {
					index++;
					do {
						++index;
					}
					while (index < length && Character.isWhitespace(line.charAt(index)));
				}
			}
			return index;
		}
	}

	/**
	 * Text navigation action to navigate to the next sub-word.
	 *
	 * 
	 */
	protected abstract class NextSubWordAction extends TextNavigationAction {

		protected JavaWordIterator fIterator = new JavaWordIterator();

		/**
		 * Creates a new next sub-word action.
		 *
		 * @param code
		 *            Action code for the default operation. Must be an action
		 *            code from @see org.eclipse.swt.custom.ST.
		 */
		protected NextSubWordAction(int code) {
			super(getSourceViewer().getTextWidget(), code);
		}

		/*
		 * @see org.eclipse.jface.action.IAction#run()
		 */
		public void run() {
			// Check whether we are in a java code partition and the
			// preference is enabled
			final IPreferenceStore store = getPreferenceStore();
			if (!store.getBoolean(PreferenceConstants.EDITOR_SUB_WORD_NAVIGATION)) {
				super.run();
				return;
			}

			final ISourceViewer viewer = getSourceViewer();
			final IDocument document = viewer.getDocument();
			fIterator.setText((CharacterIterator) new DocumentCharacterIterator(document));
			int position = widgetOffset2ModelOffset(viewer, viewer.getTextWidget().getCaretOffset());
			if (position == -1)
				return;

			int next = findNextPosition(position);
			try {
				if (isBlockSelectionModeEnabled() && document.getLineOfOffset(next) != document.getLineOfOffset(position)) {
					super.run(); // may navigate into virtual white space
				}
				else if (next != BreakIterator.DONE) {
					setCaretPosition(next);
					getTextWidget().showSelection();
					fireSelectionChanged();
				}
			}
			catch (BadLocationException x) {
				// ignore
			}
		}

		/**
		 * Finds the next position after the given position.
		 *
		 * @param position
		 *            the current position
		 * @return the next position
		 */
		protected int findNextPosition(int position) {
			ISourceViewer viewer = getSourceViewer();
			int widget = -1;
			int next = position;
			while (next != BreakIterator.DONE && widget == -1) { // TODO:
																	// optimize
				next = fIterator.following(next);
				if (next != BreakIterator.DONE)
					widget = modelOffset2WidgetOffset(viewer, next);
			}

			IDocument document = viewer.getDocument();
			LinkedModeModel model = LinkedModeModel.getModel(document, position);
			if (model != null) {
				LinkedPosition linkedPosition = model.findPosition(new LinkedPosition(document, position, 0));
				if (linkedPosition != null) {
					int linkedPositionEnd = linkedPosition.getOffset() + linkedPosition.getLength();
					if (position != linkedPositionEnd && linkedPositionEnd < next)
						next = linkedPositionEnd;
				}
				else {
					LinkedPosition nextLinkedPosition = model.findPosition(new LinkedPosition(document, next, 0));
					if (nextLinkedPosition != null) {
						int nextLinkedPositionOffset = nextLinkedPosition.getOffset();
						if (position != nextLinkedPositionOffset && nextLinkedPositionOffset < next)
							next = nextLinkedPositionOffset;
					}
				}
			}

			return next;
		}

		/**
		 * Sets the caret position to the sub-word boundary given with
		 * <code>position</code>.
		 *
		 * @param position
		 *            Position where the action should move the caret
		 */
		protected abstract void setCaretPosition(int position);
	}

	/**
	 * Text navigation action to navigate to the next sub-word.
	 *
	 * 
	 */
	protected class NavigateNextSubWordAction extends NextSubWordAction {

		/**
		 * Creates a new navigate next sub-word action.
		 */
		public NavigateNextSubWordAction() {
			super(ST.WORD_NEXT);
		}

		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor.
		 * NextSubWordAction#setCaretPosition(int)
		 */
		protected void setCaretPosition(final int position) {
			getTextWidget().setCaretOffset(modelOffset2WidgetOffset(getSourceViewer(), position));
		}
	}

	/**
	 * Text operation action to delete the next sub-word.
	 *
	 * 
	 */
	protected class DeleteNextSubWordAction extends NextSubWordAction implements IUpdate {

		/**
		 * Creates a new delete next sub-word action.
		 */
		public DeleteNextSubWordAction() {
			super(ST.DELETE_WORD_NEXT);
		}

		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor.
		 * NextSubWordAction#setCaretPosition(int)
		 */
		protected void setCaretPosition(final int position) {
			if (!validateEditorInputState())
				return;

			final ISourceViewer viewer = getSourceViewer();
			StyledText text = viewer.getTextWidget();
			Point widgetSelection = text.getSelection();
			if (isBlockSelectionModeEnabled() && widgetSelection.y != widgetSelection.x) {
				final int caret = text.getCaretOffset();
				final int offset = modelOffset2WidgetOffset(viewer, position);

				if (caret == widgetSelection.x)
					text.setSelectionRange(widgetSelection.y, offset - widgetSelection.y);
				else
					text.setSelectionRange(widgetSelection.x, offset - widgetSelection.x);
				text.invokeAction(ST.DELETE_NEXT);
			}
			else {
				Point selection = viewer.getSelectedRange();
				final int caret, length;
				if (selection.y != 0) {
					caret = selection.x;
					length = selection.y;
				}
				else {
					caret = widgetOffset2ModelOffset(viewer, text.getCaretOffset());
					length = position - caret;
				}

				try {
					viewer.getDocument().replace(caret, length, ""); //$NON-NLS-1$
				}
				catch (BadLocationException exception) {
					// Should not happen
				}
			}
		}

		/*
		 * @see org.eclipse.ui.texteditor.IUpdate#update()
		 */
		public void update() {
			setEnabled(isEditorInputModifiable());
		}
	}

	/**
	 * Text operation action to select the next sub-word.
	 *
	 * 
	 */
	protected class SelectNextSubWordAction extends NextSubWordAction {

		/**
		 * Creates a new select next sub-word action.
		 */
		public SelectNextSubWordAction() {
			super(ST.SELECT_WORD_NEXT);
		}

		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor.
		 * NextSubWordAction#setCaretPosition(int)
		 */
		protected void setCaretPosition(final int position) {
			final ISourceViewer viewer = getSourceViewer();

			final StyledText text = viewer.getTextWidget();
			if (text != null && !text.isDisposed()) {

				final Point selection = text.getSelection();
				final int caret = text.getCaretOffset();
				final int offset = modelOffset2WidgetOffset(viewer, position);

				if (caret == selection.x)
					text.setSelectionRange(selection.y, offset - selection.y);
				else
					text.setSelectionRange(selection.x, offset - selection.x);
			}
		}
	}

	/**
	 * Text navigation action to navigate to the previous sub-word.
	 *
	 * 
	 */
	protected abstract class PreviousSubWordAction extends TextNavigationAction {

		protected JavaWordIterator fIterator = new JavaWordIterator();

		/**
		 * Creates a new previous sub-word action.
		 *
		 * @param code
		 *            Action code for the default operation. Must be an action
		 *            code from @see org.eclipse.swt.custom.ST.
		 */
		protected PreviousSubWordAction(final int code) {
			super(getSourceViewer().getTextWidget(), code);
		}

		/*
		 * @see org.eclipse.jface.action.IAction#run()
		 */
		public void run() {
			// Check whether we are in a java code partition and the
			// preference is enabled
			final IPreferenceStore store = getPreferenceStore();
			if (!store.getBoolean(PreferenceConstants.EDITOR_SUB_WORD_NAVIGATION)) {
				super.run();
				return;
			}

			final ISourceViewer viewer = getSourceViewer();
			final IDocument document = viewer.getDocument();
			fIterator.setText((CharacterIterator) new DocumentCharacterIterator(document));
			int position = widgetOffset2ModelOffset(viewer, viewer.getTextWidget().getCaretOffset());
			if (position == -1)
				return;

			int previous = findPreviousPosition(position);
			try {
				if (isBlockSelectionModeEnabled() && document.getLineOfOffset(previous) != document.getLineOfOffset(position)) {
					super.run(); // may navigate into virtual white space
				}
				else if (previous != BreakIterator.DONE) {
					setCaretPosition(previous);
					getTextWidget().showSelection();
					fireSelectionChanged();
				}
			}
			catch (BadLocationException x) {
				// ignore - getLineOfOffset failed
			}

		}

		/**
		 * Finds the previous position before the given position.
		 *
		 * @param position
		 *            the current position
		 * @return the previous position
		 */
		protected int findPreviousPosition(int position) {
			ISourceViewer viewer = getSourceViewer();
			int widget = -1;
			int previous = position;
			while (previous != BreakIterator.DONE && widget == -1) { // TODO:
																		// optimize
				previous = fIterator.preceding(previous);
				if (previous != BreakIterator.DONE)
					widget = modelOffset2WidgetOffset(viewer, previous);
			}

			IDocument document = viewer.getDocument();
			LinkedModeModel model = LinkedModeModel.getModel(document, position);
			if (model != null) {
				LinkedPosition linkedPosition = model.findPosition(new LinkedPosition(document, position, 0));
				if (linkedPosition != null) {
					int linkedPositionOffset = linkedPosition.getOffset();
					if (position != linkedPositionOffset && previous < linkedPositionOffset)
						previous = linkedPositionOffset;
				}
				else {
					LinkedPosition previousLinkedPosition = model.findPosition(new LinkedPosition(document, previous, 0));
					if (previousLinkedPosition != null) {
						int previousLinkedPositionEnd = previousLinkedPosition.getOffset() + previousLinkedPosition.getLength();
						if (position != previousLinkedPositionEnd && previous < previousLinkedPositionEnd)
							previous = previousLinkedPositionEnd;
					}
				}
			}

			return previous;
		}

		/**
		 * Sets the caret position to the sub-word boundary given with
		 * <code>position</code>.
		 *
		 * @param position
		 *            Position where the action should move the caret
		 */
		protected abstract void setCaretPosition(int position);
	}

	/**
	 * Text navigation action to navigate to the previous sub-word.
	 *
	 * 
	 */
	protected class NavigatePreviousSubWordAction extends PreviousSubWordAction {

		/**
		 * Creates a new navigate previous sub-word action.
		 */
		public NavigatePreviousSubWordAction() {
			super(ST.WORD_PREVIOUS);
		}

		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor.
		 * PreviousSubWordAction#setCaretPosition(int)
		 */
		protected void setCaretPosition(final int position) {
			getTextWidget().setCaretOffset(modelOffset2WidgetOffset(getSourceViewer(), position));
		}
	}

	/**
	 * Text operation action to delete the previous sub-word.
	 *
	 * 
	 */
	protected class DeletePreviousSubWordAction extends PreviousSubWordAction implements IUpdate {

		/**
		 * Creates a new delete previous sub-word action.
		 */
		public DeletePreviousSubWordAction() {
			super(ST.DELETE_WORD_PREVIOUS);
		}

		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor.
		 * PreviousSubWordAction#setCaretPosition(int)
		 */
		protected void setCaretPosition(int position) {
			if (!validateEditorInputState())
				return;

			final int length;
			final ISourceViewer viewer = getSourceViewer();
			StyledText text = viewer.getTextWidget();
			Point widgetSelection = text.getSelection();
			if (isBlockSelectionModeEnabled() && widgetSelection.y != widgetSelection.x) {
				final int caret = text.getCaretOffset();
				final int offset = modelOffset2WidgetOffset(viewer, position);

				if (caret == widgetSelection.x)
					text.setSelectionRange(widgetSelection.y, offset - widgetSelection.y);
				else
					text.setSelectionRange(widgetSelection.x, offset - widgetSelection.x);
				text.invokeAction(ST.DELETE_PREVIOUS);
			}
			else {
				Point selection = viewer.getSelectedRange();
				if (selection.y != 0) {
					position = selection.x;
					length = selection.y;
				}
				else {
					length = widgetOffset2ModelOffset(viewer, text.getCaretOffset()) - position;
				}

				try {
					viewer.getDocument().replace(position, length, ""); //$NON-NLS-1$
				}
				catch (BadLocationException exception) {
					// Should not happen
				}
			}
		}

		/*
		 * @see org.eclipse.ui.texteditor.IUpdate#update()
		 */
		public void update() {
			setEnabled(isEditorInputModifiable());
		}
	}

	/**
	 * Text operation action to select the previous sub-word.
	 *
	 * 
	 */
	protected class SelectPreviousSubWordAction extends PreviousSubWordAction {

		/**
		 * Creates a new select previous sub-word action.
		 */
		public SelectPreviousSubWordAction() {
			super(ST.SELECT_WORD_PREVIOUS);
		}

		/*
		 * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor.
		 * PreviousSubWordAction#setCaretPosition(int)
		 */
		protected void setCaretPosition(final int position) {
			final ISourceViewer viewer = getSourceViewer();

			final StyledText text = viewer.getTextWidget();
			if (text != null && !text.isDisposed()) {

				final Point selection = text.getSelection();
				final int caret = text.getCaretOffset();
				final int offset = modelOffset2WidgetOffset(viewer, position);

				if (caret == selection.x)
					text.setSelectionRange(selection.y, offset - selection.y);
				else
					text.setSelectionRange(selection.x, offset - selection.x);
			}
		}
	}

	/**
	 * Format element action to format the enclosing java element.
	 * <p>
	 * The format element action works as follows:
	 * <ul>
	 * <li>If there is no selection and the caret is positioned on a Java
	 * element, only this element is formatted. If the element has some
	 * accompanying comment, then the comment is formatted as well.</li>
	 * <li>If the selection spans one or more partitions of the document, then
	 * all partitions covered by the selection are entirely formatted.</li>
	 * <p>
	 * Partitions at the end of the selection are not completed, except for
	 * comments.
	 *
	 * 
	 */
	protected class FormatElementAction extends Action implements IUpdate {

		/*
		 * 
		 */
		FormatElementAction() {
			setEnabled(isEditorInputModifiable());
		}

		/*
		 * @see org.eclipse.jface.action.IAction#run()
		 */
		public void run() {

			final JavaSourceViewer viewer = (JavaSourceViewer) getSourceViewer();
			if (viewer.isEditable()) {

				final Point selection = viewer.rememberSelection();
				try {
					viewer.setRedraw(false);

					final String type = TextUtilities.getContentType(viewer.getDocument(), IJavaScriptPartitions.JAVA_PARTITIONING, selection.x, true);
					if (type.equals(IDocument.DEFAULT_CONTENT_TYPE) && selection.y == 0) {

						try {
							final IJavaScriptElement element = getElementAt(selection.x, true);
							if (element != null && element.exists()) {

								final int kind = element.getElementType();
								if (kind == IJavaScriptElement.TYPE || kind == IJavaScriptElement.METHOD || kind == IJavaScriptElement.INITIALIZER) {

									final ISourceReference reference = (ISourceReference) element;
									final ISourceRange range = reference.getSourceRange();

									if (range != null) {
										viewer.setSelectedRange(range.getOffset(), range.getLength());
										viewer.doOperation(ISourceViewer.FORMAT);
									}
								}
							}
						}
						catch (JavaScriptModelException exception) {
							// Should not happen
						}
					}
					else {
						viewer.setSelectedRange(selection.x, 1);
						viewer.doOperation(ISourceViewer.FORMAT);
					}
				}
				catch (BadLocationException exception) {
					// Can not happen
				}
				finally {

					viewer.setRedraw(true);
					viewer.restoreSelection();
				}
			}
		}

		/*
		 * @see org.eclipse.ui.texteditor.IUpdate#update()
		 * 
		 */
		public void update() {
			setEnabled(isEditorInputModifiable());
		}
	}

	/**
	 * Internal activation listener.
	 * 
	 */
	private class ActivationListener implements IWindowListener {

		/*
		 * @see org.eclipse.ui.IWindowListener#windowActivated(org.eclipse.ui.
		 * IWorkbenchWindow)
		 * 
		 */
		public void windowActivated(IWorkbenchWindow window) {
			if (window == getEditorSite().getWorkbenchWindow() && fMarkOccurrenceAnnotations && isActivePart()) {
				fForcedMarkOccurrencesSelection = getSelectionProvider().getSelection();
				updateOccurrenceAnnotations((ITextSelection) fForcedMarkOccurrencesSelection, JavaScriptPlugin.getDefault().getASTProvider().getAST(getInputJavaElement(), ASTProvider.WAIT_NO, getProgressMonitor()));
			}
		}

		/*
		 * @see
		 * org.eclipse.ui.IWindowListener#windowDeactivated(org.eclipse.ui.
		 * IWorkbenchWindow)
		 * 
		 */
		public void windowDeactivated(IWorkbenchWindow window) {
			if (window == getEditorSite().getWorkbenchWindow() && fMarkOccurrenceAnnotations && isActivePart())
				removeOccurrenceAnnotations();
		}

		/*
		 * @see org.eclipse.ui.IWindowListener#windowClosed(org.eclipse.ui.
		 * IWorkbenchWindow)
		 * 
		 */
		public void windowClosed(IWorkbenchWindow window) {
		}

		/*
		 * @see org.eclipse.ui.IWindowListener#windowOpened(org.eclipse.ui.
		 * IWorkbenchWindow)
		 * 
		 */
		public void windowOpened(IWorkbenchWindow window) {
		}
	}

	/**
	 * Runner that will toggle folding either instantly (if the editor is
	 * visible) or the next time it becomes visible. If a runner is started
	 * when there is already one registered, the registered one is canceled as
	 * toggling folding twice is a no-op.
	 * <p>
	 * The access to the fFoldingRunner field is not thread-safe, it is
	 * assumed that <code>runWhenNextVisible</code> is only called from the UI
	 * thread.
	 * </p>
	 *
	 * 
	 */
	private final class ToggleFoldingRunner implements IPartListener2 {
		/**
		 * The workbench page we registered the part listener with, or
		 * <code>null</code>.
		 */
		private IWorkbenchPage fPage;

		/**
		 * Does the actual toggling of projection.
		 */
		private void toggleFolding() {
			ISourceViewer sourceViewer = getSourceViewer();
			if (sourceViewer instanceof ProjectionViewer) {
				ProjectionViewer pv = (ProjectionViewer) sourceViewer;
				if (pv.isProjectionMode() != isFoldingEnabled()) {
					if (pv.canDoOperation(ProjectionViewer.TOGGLE))
						pv.doOperation(ProjectionViewer.TOGGLE);
				}
			}
		}

		/**
		 * Makes sure that the editor's folding state is correct the next time
		 * it becomes visible. If it already is visible, it toggles the
		 * folding state. If not, it either registers a part listener to
		 * toggle folding when the editor becomes visible, or cancels an
		 * already registered runner.
		 */
		public void runWhenNextVisible() {
			// if there is one already: toggling twice is the identity
			if (fFoldingRunner != null) {
				fFoldingRunner.cancel();
				return;
			}
			IWorkbenchPartSite site = getSite();
			if (site != null) {
				IWorkbenchPage page = site.getPage();
				if (!page.isPartVisible(JavaEditor.this)) {
					// if we're not visible - defer until visible
					fPage = page;
					fFoldingRunner = this;
					page.addPartListener(this);
					return;
				}
			}
			// we're visible - run now
			toggleFolding();
		}

		/**
		 * Remove the listener and clear the field.
		 */
		private void cancel() {
			if (fPage != null) {
				fPage.removePartListener(this);
				fPage = null;
			}
			if (fFoldingRunner == this)
				fFoldingRunner = null;
		}

		/*
		 * @see org.eclipse.ui.IPartListener2#partVisible(org.eclipse.ui.
		 * IWorkbenchPartReference)
		 */
		public void partVisible(IWorkbenchPartReference partRef) {
			if (JavaEditor.this.equals(partRef.getPart(false))) {
				cancel();
				toggleFolding();
			}
		}

		/*
		 * @see org.eclipse.ui.IPartListener2#partClosed(org.eclipse.ui.
		 * IWorkbenchPartReference)
		 */
		public void partClosed(IWorkbenchPartReference partRef) {
			if (JavaEditor.this.equals(partRef.getPart(false))) {
				cancel();
			}
		}

		public void partActivated(IWorkbenchPartReference partRef) {
		}

		public void partBroughtToTop(IWorkbenchPartReference partRef) {
		}

		public void partDeactivated(IWorkbenchPartReference partRef) {
		}

		public void partOpened(IWorkbenchPartReference partRef) {
		}

		public void partHidden(IWorkbenchPartReference partRef) {
		}

		public void partInputChanged(IWorkbenchPartReference partRef) {
		}
	}

	/** Preference key for matching brackets */
	protected final static String MATCHING_BRACKETS = PreferenceConstants.EDITOR_MATCHING_BRACKETS;
	/** Preference key for matching brackets color */
	protected final static String MATCHING_BRACKETS_COLOR = PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR;

	protected final static char[] BRACKETS = {'{', '}', '(', ')', '[', ']', '<', '>'};

	/** The outline page */
	protected JavaOutlinePage fOutlinePage;
	/** Outliner context menu Id */
	protected String fOutlinerContextMenuId;
	/**
	 * The editor selection changed listener.
	 *
	 * 
	 */
	private EditorSelectionChangedListener fEditorSelectionChangedListener;
	/** The selection changed listener */
	protected AbstractSelectionChangedListener fOutlineSelectionChangedListener = new OutlineSelectionChangedListener();
	/** The editor's bracket matcher */
	protected JavaPairMatcher fBracketMatcher = new JavaPairMatcher(BRACKETS);
	/** This editor's encoding support */
	private DefaultEncodingSupport fEncodingSupport;
	/** History for structure select action */
	private SelectionHistory fSelectionHistory;
	protected CompositeActionGroup fActionGroups;

	/**
	 * The action group for folding.
	 *
	 * 
	 */
	private FoldingActionGroup fFoldingGroup;

	private CompositeActionGroup fContextMenuGroup;
	/**
	 * Holds the current occurrence annotations.
	 * 
	 */
	private Annotation[] fOccurrenceAnnotations = null;
	/**
	 * Tells whether all occurrences of the element at the current caret
	 * location are automatically marked in this editor.
	 * 
	 */
	private boolean fMarkOccurrenceAnnotations;
	/**
	 * Tells whether the occurrence annotations are sticky i.e. whether they
	 * stay even if there's no valid Java element at the current caret
	 * position. Only valid if {@link #fMarkOccurrenceAnnotations} is
	 * <code>true</code>.
	 * 
	 */
	private boolean fStickyOccurrenceAnnotations;
	/**
	 * Tells whether to mark type occurrences in this editor. Only valid if
	 * {@link #fMarkOccurrenceAnnotations} is <code>true</code>.
	 * 
	 */
	private boolean fMarkTypeOccurrences;
	/**
	 * Tells whether to mark method occurrences in this editor. Only valid if
	 * {@link #fMarkOccurrenceAnnotations} is <code>true</code>.
	 * 
	 */
	private boolean fMarkMethodOccurrences;
	/**
	 * Tells whether to mark constant occurrences in this editor. Only valid
	 * if {@link #fMarkOccurrenceAnnotations} is <code>true</code>.
	 * 
	 */
	private boolean fMarkConstantOccurrences;
	/**
	 * Tells whether to mark field occurrences in this editor. Only valid if
	 * {@link #fMarkOccurrenceAnnotations} is <code>true</code>.
	 * 
	 */
	private boolean fMarkFieldOccurrences;
	/**
	 * Tells whether to mark local variable occurrences in this editor. Only
	 * valid if {@link #fMarkOccurrenceAnnotations} is <code>true</code>.
	 * 
	 */
	private boolean fMarkLocalVariableypeOccurrences;
	/**
	 * Tells whether to mark exception occurrences in this editor. Only valid
	 * if {@link #fMarkOccurrenceAnnotations} is <code>true</code>.
	 * 
	 */
	private boolean fMarkExceptions;
	/**
	 * Tells whether to mark method exits in this editor. Only valid if
	 * {@link #fMarkOccurrenceAnnotations} is <code>true</code>.
	 * 
	 */
	private boolean fMarkMethodExitPoints;

	/**
	 * Tells whether to mark targets of <code>break</code> and
	 * <code>continue</code> statements in this editor. Only valid if
	 * {@link #fMarkOccurrenceAnnotations} is <code>true</code>.
	 * 
	 */
	private boolean fMarkBreakContinueTargets;

	/**
	 * Tells whether to mark implementors in this editor. Only valid if
	 * {@link #fMarkOccurrenceAnnotations} is <code>true</code>.
	 * 
	 */
	private boolean fMarkImplementors;
	/**
	 * The selection used when forcing occurrence marking through code.
	 * 
	 */
	private ISelection fForcedMarkOccurrencesSelection;
	/**
	 * The document modification stamp at the time when the last occurrence
	 * marking took place.
	 * 
	 */
	private long fMarkOccurrenceModificationStamp = IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP;
	/**
	 * The region of the word under the caret used to when computing the
	 * current occurrence markings.
	 * 
	 */
	private IRegion fMarkOccurrenceTargetRegion;

	/**
	 * The internal shell activation listener for updating occurrences.
	 * 
	 */
	private ActivationListener fActivationListener = new ActivationListener();
	private ISelectionListenerWithAST fPostSelectionListenerWithAST;
	private OccurrencesFinderJob fOccurrencesFinderJob;
	/** The occurrences finder job canceler */
	private OccurrencesFinderJobCanceler fOccurrencesFinderJobCanceler;
	/**
	 * This editor's projection support
	 * 
	 */
	private ProjectionSupport fProjectionSupport;
	/**
	 * This editor's projection model updater
	 * 
	 */
	private IJavaFoldingStructureProvider fProjectionModelUpdater;
	/**
	 * The override and implements indicator manager for this editor.
	 * 
	 */
	protected OverrideIndicatorManager fOverrideIndicatorManager;
	/**
	 * Semantic highlighting manager , protected as of 3.3
	 */
	protected SemanticHighlightingManager fSemanticManager;
	/**
	 * The folding runner.
	 * 
	 */
	private ToggleFoldingRunner fFoldingRunner;

	/**
	 * Tells whether the selection changed event is caused by a call to
	 * {@link #gotoAnnotation(boolean)}.
	 * 
	 * 
	 */
	private boolean fSelectionChangedViaGotoAnnotation;
	/**
	 * The cached selected range.
	 * 
	 * @see ITextViewer#getSelectedRange()
	 * 
	 */
	private Point fCachedSelectedRange;


	/**
	 * Returns the most narrow java element including the given offset.
	 *
	 * @param offset
	 *            the offset inside of the requested element
	 * @return the most narrow java element
	 */
	abstract protected IJavaScriptElement getElementAt(int offset);

	/**
	 * Returns the java element of this editor's input corresponding to the
	 * given IJavaScriptElement.
	 *
	 * @param element
	 *            the java element
	 * @return the corresponding Java element
	 */
	abstract protected IJavaScriptElement getCorrespondingElement(IJavaScriptElement element);

	/**
	 * Default constructor.
	 */
	public JavaEditor() {
		super();
	}

	/**
	 * Sets the input of the editor's outline page.
	 *
	 * @param page
	 *            the Java outline page
	 * @param input
	 *            the editor input
	 */
	protected void setOutlinePageInput(JavaOutlinePage page, IEditorInput input) {
		if (page == null)
			return;

		IJavaScriptElement je = getInputJavaElement();
		if (je != null && je.exists())
			page.setInput(je);
		else
			page.setInput(null);

	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#
	 * initializeKeyBindingScopes()
	 */
	protected void initializeKeyBindingScopes() {
		setKeyBindingScopes(new String[]{"org.eclipse.wst.jsdt.ui.javascriptViewScope"}); //$NON-NLS-1$
	}

	/*
	 * @see
	 * org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#initializeEditor(
	 * )
	 */
	protected void initializeEditor() {
		IPreferenceStore store = createCombinedPreferenceStore(null);
		setPreferenceStore(store);
		setSourceViewerConfiguration(createJavaSourceViewerConfiguration());
		fMarkOccurrenceAnnotations = store.getBoolean(PreferenceConstants.EDITOR_MARK_OCCURRENCES);
		fStickyOccurrenceAnnotations = store.getBoolean(PreferenceConstants.EDITOR_STICKY_OCCURRENCES);
		fMarkTypeOccurrences = store.getBoolean(PreferenceConstants.EDITOR_MARK_TYPE_OCCURRENCES);
		fMarkMethodOccurrences = store.getBoolean(PreferenceConstants.EDITOR_MARK_METHOD_OCCURRENCES);
		fMarkConstantOccurrences = store.getBoolean(PreferenceConstants.EDITOR_MARK_CONSTANT_OCCURRENCES);
		fMarkFieldOccurrences = store.getBoolean(PreferenceConstants.EDITOR_MARK_FIELD_OCCURRENCES);
		fMarkLocalVariableypeOccurrences = store.getBoolean(PreferenceConstants.EDITOR_MARK_LOCAL_VARIABLE_OCCURRENCES);
		fMarkExceptions = store.getBoolean(PreferenceConstants.EDITOR_MARK_EXCEPTION_OCCURRENCES);
		fMarkImplementors = store.getBoolean(PreferenceConstants.EDITOR_MARK_IMPLEMENTORS);
		fMarkMethodExitPoints = store.getBoolean(PreferenceConstants.EDITOR_MARK_METHOD_EXIT_POINTS);
		fMarkBreakContinueTargets = store.getBoolean(PreferenceConstants.EDITOR_MARK_BREAK_CONTINUE_TARGETS);
	}

	/**
	 * Returns a new Java source viewer configuration.
	 * 
	 * @return a new <code>JavaScriptSourceViewerConfiguration</code>
	 * 
	 */
	protected JavaScriptSourceViewerConfiguration createJavaSourceViewerConfiguration() {
		JavaScriptTextTools textTools = JavaScriptPlugin.getDefault().getJavaTextTools();
		return new JavaScriptSourceViewerConfiguration(textTools.getColorManager(), getPreferenceStore(), this, IJavaScriptPartitions.JAVA_PARTITIONING);
	}

	/*
	 * @see AbstractTextEditor#createSourceViewer(Composite, IVerticalRuler,
	 * int)
	 */
	protected final ISourceViewer createSourceViewer(Composite parent, IVerticalRuler verticalRuler, int styles) {

		IPreferenceStore store = getPreferenceStore();
		ISourceViewer viewer = createJavaSourceViewer(parent, verticalRuler, getOverviewRuler(), isOverviewRulerVisible(), styles, store);

		JavaUIHelp.setHelp(this, viewer.getTextWidget(), IJavaHelpContextIds.JAVA_EDITOR);

		JavaSourceViewer javaSourceViewer = null;
		if (viewer instanceof JavaSourceViewer)
			javaSourceViewer = (JavaSourceViewer) viewer;

		/*
		 * This is a performance optimization to reduce the computation of the
		 * text presentation triggered by {@link
		 * #setVisibleDocument(IDocument)}
		 */
		if (javaSourceViewer != null && isFoldingEnabled() && (store == null || !store.getBoolean(PreferenceConstants.EDITOR_SHOW_SEGMENTS)))
			javaSourceViewer.prepareDelayedProjection();

		ProjectionViewer projectionViewer = (ProjectionViewer) viewer;
		fProjectionSupport = new ProjectionSupport(projectionViewer, getAnnotationAccess(), getSharedColors());
		fProjectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.error"); //$NON-NLS-1$
		fProjectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.warning"); //$NON-NLS-1$
		fProjectionSupport.setHoverControlCreator(new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell shell) {
				return new SourceViewerInformationControl(shell, SWT.TOOL | SWT.NO_TRIM | getOrientation(), SWT.NONE, EditorsUI.getTooltipAffordanceString());
			}
		});
		fProjectionSupport.setInformationPresenterControlCreator(new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell shell) {
				int shellStyle = SWT.RESIZE | SWT.TOOL | getOrientation();
				int style = SWT.V_SCROLL | SWT.H_SCROLL;
				return new SourceViewerInformationControl(shell, shellStyle, style);
			}
		});
		fProjectionSupport.install();

		fProjectionModelUpdater = JavaScriptPlugin.getDefault().getFoldingStructureProviderRegistry().getCurrentFoldingProvider();
		if (fProjectionModelUpdater != null)
			fProjectionModelUpdater.install(this, projectionViewer);

		// ensure source viewer decoration support has been created and
		// configured
		getSourceViewerDecorationSupport(viewer);

		return viewer;
	}

	public final ISourceViewer getViewer() {
		return getSourceViewer();
	}

	/*
	 * @see AbstractTextEditor#createSourceViewer(Composite, IVerticalRuler,
	 * int)
	 */
	protected ISourceViewer createJavaSourceViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler, boolean isOverviewRulerVisible, int styles, IPreferenceStore store) {
		return new JavaSourceViewer(parent, verticalRuler, getOverviewRuler(), isOverviewRulerVisible(), styles, store);
	}

	/*
	 * @see AbstractTextEditor#affectsTextPresentation(PropertyChangeEvent)
	 */
	protected boolean affectsTextPresentation(PropertyChangeEvent event) {
		return ((JavaScriptSourceViewerConfiguration) getSourceViewerConfiguration()).affectsTextPresentation(event) || super.affectsTextPresentation(event);
	}

	/**
	 * Creates and returns the preference store for this Java editor with the
	 * given input.
	 *
	 * @param input
	 *            The editor input for which to create the preference store
	 * @return the preference store for this editor
	 *
	 * 
	 */
	private IPreferenceStore createCombinedPreferenceStore(IEditorInput input) {
		List stores = new ArrayList(3);

		IJavaScriptProject project = EditorUtility.getJavaProject(input);
		if (project != null) {
			stores.add(new EclipsePreferencesAdapter(new ProjectScope(project.getProject()), JavaScriptCore.PLUGIN_ID));
		}

		stores.add(JavaScriptPlugin.getDefault().getPreferenceStore());
		stores.add(new PreferencesAdapter(JavaScriptCore.getPlugin().getPluginPreferences()));
		stores.add(EditorsUI.getPreferenceStore());

		return new ChainedPreferenceStore((IPreferenceStore[]) stores.toArray(new IPreferenceStore[stores.size()]));
	}

	/**
	 * Sets the outliner's context menu ID.
	 *
	 * @param menuId
	 *            the menu ID
	 */
	protected void setOutlinerContextMenuId(String menuId) {
		fOutlinerContextMenuId = menuId;
	}

	/**
	 * Returns the standard action group of this editor.
	 *
	 * @return returns this editor's standard action group
	 */
	protected ActionGroup getActionGroup() {
		return fActionGroups;
	}

	/*
	 * @see AbstractTextEditor#editorContextMenuAboutToShow
	 */
	public void editorContextMenuAboutToShow(IMenuManager menu) {

		super.editorContextMenuAboutToShow(menu);
		menu.insertAfter(IContextMenuConstants.GROUP_OPEN, new GroupMarker(IContextMenuConstants.GROUP_SHOW));

		ActionContext context = new ActionContext(getSelectionProvider().getSelection());
		fContextMenuGroup.setContext(context);
		fContextMenuGroup.fillContextMenu(menu);
		fContextMenuGroup.setContext(null);

		// Quick views
		IAction action = getAction(IJavaEditorActionDefinitionIds.SHOW_OUTLINE);
		menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, action);
		action = getAction(IJavaEditorActionDefinitionIds.OPEN_HIERARCHY);
		menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, action);

	}

	/**
	 * Creates the outline page used with this editor.
	 *
	 * @return the created Java outline page
	 */
	protected JavaOutlinePage createOutlinePage() {
		JavaOutlinePage page = new JavaOutlinePage(fOutlinerContextMenuId, this);
		fOutlineSelectionChangedListener.install(page);
		setOutlinePageInput(page, getEditorInput());
		return page;
	}

	/**
	 * Informs the editor that its outliner has been closed.
	 */
	public void outlinePageClosed() {
		if (fOutlinePage != null) {
			fOutlineSelectionChangedListener.uninstall(fOutlinePage);
			fOutlinePage = null;
			resetHighlightRange();
		}
	}

	/**
	 * Synchronizes the outliner selection with the given element position in
	 * the editor.
	 *
	 * @param element
	 *            the java element to select
	 */
	protected void synchronizeOutlinePage(ISourceReference element) {
		synchronizeOutlinePage(element, true);
	}

	/**
	 * Synchronizes the outliner selection with the given element position in
	 * the editor.
	 *
	 * @param element
	 *            the java element to select
	 * @param checkIfOutlinePageActive
	 *            <code>true</code> if check for active outline page needs to
	 *            be done
	 */
	protected void synchronizeOutlinePage(ISourceReference element, boolean checkIfOutlinePageActive) {
		if (fOutlinePage != null && element != null && !(checkIfOutlinePageActive && isJavaOutlinePageActive())) {
			fOutlineSelectionChangedListener.uninstall(fOutlinePage);
			fOutlinePage.select(element);
			fOutlineSelectionChangedListener.install(fOutlinePage);
		}
	}

	/**
	 * Synchronizes the outliner selection with the actual cursor position in
	 * the editor.
	 */
	public void synchronizeOutlinePageSelection() {
		synchronizeOutlinePage(computeHighlightRangeSourceReference());
	}

	/*
	 * @see AbstractTextEditor#getAdapter(Class)
	 */
	public Object getAdapter(Class required) {

		if (IContentOutlinePage.class.equals(required)) {
			if (fOutlinePage == null)
				fOutlinePage = createOutlinePage();
			return fOutlinePage;
		}

		if (IEncodingSupport.class.equals(required))
			return fEncodingSupport;

		if (required == IShowInTargetList.class) {
			return new IShowInTargetList() {
				public String[] getShowInTargetIds() {
					String explorerViewID = ProductProperties.getProperty(IProductConstants.PERSPECTIVE_EXPLORER_VIEW);
					// make sure the specified view ID is known
					if (PlatformUI.getWorkbench().getViewRegistry().find(explorerViewID) == null)
						explorerViewID = ProductProperties.ID_PERSPECTIVE_EXPLORER_VIEW;
					return new String[]{explorerViewID, JavaScriptUI.ID_PACKAGES, IPageLayout.ID_OUTLINE, IPageLayout.ID_RES_NAV};
				}

			};
		}

		if (required == IShowInSource.class) {
			return new IShowInSource() {
				public ShowInContext getShowInContext() {
					return new ShowInContext(getEditorInput(), null) {
						/*
						 * @see
						 * org.eclipse.ui.part.ShowInContext#getSelection()
						 * 
						 */
						public ISelection getSelection() {
							IJavaScriptElement je = null;
							try {
								je = SelectionConverter.getElementAtOffset(JavaEditor.this);
								if (je == null)
									return null;
								return new StructuredSelection(je);
							}
							catch (JavaScriptModelException ex) {
								return null;
							}
						}
					};
				}
			};
		}

		if (required == IJavaFoldingStructureProvider.class)
			return fProjectionModelUpdater;

		if (fProjectionSupport != null) {
			Object adapter = fProjectionSupport.getAdapter(getSourceViewer(), required);
			if (adapter != null)
				return adapter;
		}

		if (required == IContextProvider.class)
			return JavaUIHelp.getHelpContextProvider(this, IJavaHelpContextIds.JAVA_EDITOR);

		return super.getAdapter(required);
	}

	/**
	 * React to changed selection.
	 *
	 * 
	 */
	protected void selectionChanged() {
		if (getSelectionProvider() == null)
			return;
		ISourceReference element = computeHighlightRangeSourceReference();
		if (getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE))
			synchronizeOutlinePage(element);
		setSelection(element, false);
		if (!fSelectionChangedViaGotoAnnotation)
			updateStatusLine();
		fSelectionChangedViaGotoAnnotation = false;
	}

	protected void setSelection(ISourceReference reference, boolean moveCursor) {
		if (getSelectionProvider() == null)
			return;

		ISelection selection = getSelectionProvider().getSelection();
		if (selection instanceof ITextSelection) {
			ITextSelection textSelection = (ITextSelection) selection;
			// PR 39995: [navigation] Forward history cleared after going back
			// in navigation history:
			// mark only in navigation history if the cursor is being moved
			// (which it isn't if
			// this is called from a PostSelectionEvent that should only
			// update the magnet)
			if (moveCursor && (textSelection.getOffset() != 0 || textSelection.getLength() != 0))
				markInNavigationHistory();
		}

		if (reference != null) {

			StyledText textWidget = null;

			ISourceViewer sourceViewer = getSourceViewer();
			if (sourceViewer != null)
				textWidget = sourceViewer.getTextWidget();

			if (textWidget == null)
				return;

			try {
				ISourceRange range = null;
				if (reference instanceof ILocalVariable) {
					IJavaScriptElement je = ((ILocalVariable) reference).getParent();
					if (je instanceof ISourceReference)
						range = ((ISourceReference) je).getSourceRange();
				}
				else
					range = reference.getSourceRange();

				if (range == null)
					return;

				int offset = range.getOffset();
				int length = range.getLength();

				if (offset < 0 || length < 0)
					return;

				setHighlightRange(offset, length, moveCursor);

				if (!moveCursor)
					return;

				offset = -1;
				length = -1;

				if (reference instanceof IMember) {
					range = ((IMember) reference).getNameRange();
					if (range != null) {
						offset = range.getOffset();
						length = range.getLength();
					}
				}
				else if (reference instanceof ILocalVariable) {
					range = ((ILocalVariable) reference).getNameRange();
					if (range != null) {
						offset = range.getOffset();
						length = range.getLength();
					}
				}
				else if (reference instanceof IImportDeclaration) {
					String content = reference.getSource();
					if (content != null) {
						int start = content.indexOf("import") + 6; //$NON-NLS-1$
						while (start < content.length() && content.charAt(start) == ' ')
							start++;

						int end = content.indexOf(';');
						do {
							end--;
						}
						while (end >= 0 && content.charAt(end) == ' ');

						offset = range.getOffset() + start;
						length = end - start + 1;
					}
					else {
						// fallback
						offset = range.getOffset();
						length = range.getLength();
					}
				}

				if (offset > -1 && length > 0) {

					try {
						textWidget.setRedraw(false);
						sourceViewer.revealRange(offset, length);
						sourceViewer.setSelectedRange(offset, length);
					}
					finally {
						textWidget.setRedraw(true);
					}

					markInNavigationHistory();
				}

			}
			catch (JavaScriptModelException x) {
			}
			catch (IllegalArgumentException x) {
			}

		}
		else if (moveCursor) {
			resetHighlightRange();
			markInNavigationHistory();
		}
	}

	public void setSelection(IJavaScriptElement element) {

		if (element == null || element instanceof IJavaScriptUnit || element instanceof IClassFile) {
			/*
			 * If the element is an IJavaScriptUnit this unit is either the
			 * input of this editor or not being displayed. In both cases,
			 * nothing should happened.
			 * (http://dev.eclipse.org/bugs/show_bug.cgi?id=5128)
			 */
			return;
		}

		IJavaScriptElement corresponding = getCorrespondingElement(element);
		if (corresponding instanceof ISourceReference) {
			ISourceReference reference = (ISourceReference) corresponding;
			// set highlight range
			setSelection(reference, true);
			// set outliner selection
			if (fOutlinePage != null) {
				fOutlineSelectionChangedListener.uninstall(fOutlinePage);
				fOutlinePage.select(reference);
				fOutlineSelectionChangedListener.install(fOutlinePage);
			}
		}
	}

	protected void doSelectionChanged(SelectionChangedEvent event) {

		ISourceReference reference = null;

		ISelection selection = event.getSelection();
		Iterator iter = ((IStructuredSelection) selection).iterator();
		while (iter.hasNext()) {
			Object o = iter.next();
			if (o instanceof ISourceReference) {
				reference = (ISourceReference) o;
				break;
			}
		}
		if (!isActivePart() && JavaScriptPlugin.getActivePage() != null)
			JavaScriptPlugin.getActivePage().bringToTop(this);

		setSelection(reference, !isActivePart());

		ISelectionProvider selectionProvider = getSelectionProvider();
		if (selectionProvider == null)
			return;

		ISelection textSelection = selectionProvider.getSelection();
		if (!(textSelection instanceof ITextSelection))
			return;

		JavaScriptUnit ast = JavaScriptPlugin.getDefault().getASTProvider().getAST(getInputJavaElement(), ASTProvider.WAIT_ACTIVE_ONLY, getProgressMonitor());
		if (ast != null) {
			fForcedMarkOccurrencesSelection = textSelection;
			updateOccurrenceAnnotations((ITextSelection) textSelection, ast);
		}

	}

	/*
	 * @see AbstractTextEditor#adjustHighlightRange(int, int)
	 */
	protected void adjustHighlightRange(int offset, int length) {

		try {

			IJavaScriptElement element = getElementAt(offset, false);
			while (element instanceof ISourceReference) {
				ISourceRange range = ((ISourceReference) element).getSourceRange();
				if (range != null && offset < range.getOffset() + range.getLength() && range.getOffset() < offset + length) {

					ISourceViewer viewer = getSourceViewer();
					if (viewer instanceof ITextViewerExtension5) {
						ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
						extension.exposeModelRange(new Region(range.getOffset(), range.getLength()));
					}

					setHighlightRange(range.getOffset(), range.getLength(), true);
					if (fOutlinePage != null) {
						fOutlineSelectionChangedListener.uninstall(fOutlinePage);
						fOutlinePage.select((ISourceReference) element);
						fOutlineSelectionChangedListener.install(fOutlinePage);
					}

					return;
				}
				element = element.getParent();
			}

		}
		catch (JavaScriptModelException x) {
			JavaScriptPlugin.log(x.getStatus());
		}

		ISourceViewer viewer = getSourceViewer();
		if (viewer instanceof ITextViewerExtension5) {
			ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
			extension.exposeModelRange(new Region(offset, length));
		}
		else {
			resetHighlightRange();
		}

	}

	protected boolean isActivePart() {
		IWorkbenchPart part = getActivePart();
		return part != null && part.equals(this);
	}

	private boolean isJavaOutlinePageActive() {
		IWorkbenchPart part = getActivePart();
		return part instanceof ContentOutline && ((ContentOutline) part).getCurrentPage() == fOutlinePage;
	}

	private IWorkbenchPart getActivePart() {
		IWorkbenchWindow window = getSite().getWorkbenchWindow();
		IPartService service = window.getPartService();
		IWorkbenchPart part = service.getActivePart();
		return part;
	}

	/*
	 * @see StatusTextEditor#getStatusHeader(IStatus)
	 */
	protected String getStatusHeader(IStatus status) {
		if (fEncodingSupport != null) {
			String message = fEncodingSupport.getStatusHeader(status);
			if (message != null)
				return message;
		}
		return super.getStatusHeader(status);
	}

	/*
	 * @see StatusTextEditor#getStatusBanner(IStatus)
	 */
	protected String getStatusBanner(IStatus status) {
		if (fEncodingSupport != null) {
			String message = fEncodingSupport.getStatusBanner(status);
			if (message != null)
				return message;
		}
		return super.getStatusBanner(status);
	}

	/*
	 * @see StatusTextEditor#getStatusMessage(IStatus)
	 */
	protected String getStatusMessage(IStatus status) {
		if (fEncodingSupport != null) {
			String message = fEncodingSupport.getStatusMessage(status);
			if (message != null)
				return message;
		}
		return super.getStatusMessage(status);
	}

	/*
	 * @see AbstractTextEditor#doSetInput
	 */
	protected void doSetInput(IEditorInput input) throws CoreException {
		ISourceViewer sourceViewer = getSourceViewer();
		if (!(sourceViewer instanceof ISourceViewerExtension2)) {
			setPreferenceStore(createCombinedPreferenceStore(input));
			internalDoSetInput(input);
			return;
		}

		// uninstall & unregister preference store listener
		getSourceViewerDecorationSupport(sourceViewer).uninstall();
		((ISourceViewerExtension2) sourceViewer).unconfigure();

		setPreferenceStore(createCombinedPreferenceStore(input));

		// install & register preference store listener
		sourceViewer.configure(getSourceViewerConfiguration());
		getSourceViewerDecorationSupport(sourceViewer).install(getPreferenceStore());

		internalDoSetInput(input);
	}

	private void internalDoSetInput(IEditorInput input) throws CoreException {
		ISourceViewer sourceViewer = getSourceViewer();
		JavaSourceViewer javaSourceViewer = null;
		if (sourceViewer instanceof JavaSourceViewer)
			javaSourceViewer = (JavaSourceViewer) sourceViewer;

		IPreferenceStore store = getPreferenceStore();
		if (javaSourceViewer != null && isFoldingEnabled() && (store == null || !store.getBoolean(PreferenceConstants.EDITOR_SHOW_SEGMENTS)))
			javaSourceViewer.prepareDelayedProjection();

		super.doSetInput(input);

		if (javaSourceViewer != null && javaSourceViewer.getReconciler() == null) {
			IReconciler reconciler = getSourceViewerConfiguration().getReconciler(javaSourceViewer);
			if (reconciler != null) {
				reconciler.install(javaSourceViewer);
				javaSourceViewer.setReconciler(reconciler);
			}
		}

		if (fEncodingSupport != null)
			fEncodingSupport.reset();

		setOutlinePageInput(fOutlinePage, input);

		if (isShowingOverrideIndicators())
			installOverrideIndicator(false);
	}

	/*
	 * @see
	 * org.eclipse.ui.texteditor.AbstractTextEditor#setPreferenceStore(org.
	 * eclipse.jface.preference.IPreferenceStore)
	 * 
	 */
	protected void setPreferenceStore(IPreferenceStore store) {
		super.setPreferenceStore(store);
		if (getSourceViewerConfiguration() instanceof JavaScriptSourceViewerConfiguration) {
			JavaScriptTextTools textTools = JavaScriptPlugin.getDefault().getJavaTextTools();
			setSourceViewerConfiguration(new JavaScriptSourceViewerConfiguration(textTools.getColorManager(), store, this, IJavaScriptPartitions.JAVA_PARTITIONING));
		}
		if (getSourceViewer() instanceof JavaSourceViewer)
			((JavaSourceViewer) getSourceViewer()).setPreferenceStore(store);
	}

	/*
	 * @see IWorkbenchPart#dispose()
	 */
	public void dispose() {

		if (fProjectionModelUpdater != null) {
			fProjectionModelUpdater.uninstall();
			fProjectionModelUpdater = null;
		}

		if (fProjectionSupport != null) {
			fProjectionSupport.dispose();
			fProjectionSupport = null;
		}

		// cancel possible running computation
		fMarkOccurrenceAnnotations = false;
		uninstallOccurrencesFinder();

		uninstallOverrideIndicator();

		uninstallSemanticHighlighting();

		if (fActivationListener != null) {
			PlatformUI.getWorkbench().removeWindowListener(fActivationListener);
			fActivationListener = null;
		}

		if (fEncodingSupport != null) {
			fEncodingSupport.dispose();
			fEncodingSupport = null;
		}

		if (fBracketMatcher != null) {
			fBracketMatcher.dispose();
			fBracketMatcher = null;
		}

		if (fSelectionHistory != null) {
			fSelectionHistory.dispose();
			fSelectionHistory = null;
		}

		if (fEditorSelectionChangedListener != null) {
			fEditorSelectionChangedListener.uninstall(getSelectionProvider());
			fEditorSelectionChangedListener = null;
		}

		if (fActionGroups != null) {
			fActionGroups.dispose();
			fActionGroups = null;
		}

		super.dispose();
	}

	protected void createActions() {
		installEncodingSupport();

		super.createActions();

		ActionGroup oeg, ovg, jsg;
		fActionGroups = new CompositeActionGroup(new ActionGroup[]{oeg = new OpenEditorActionGroup(this), ovg = new OpenViewActionGroup(this), jsg = new JavaSearchActionGroup(this)});
		fContextMenuGroup = new CompositeActionGroup(new ActionGroup[]{oeg, ovg, jsg});

		fFoldingGroup = new FoldingActionGroup(this, getViewer());

		Action action = new GotoMatchingBracketAction(this);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_MATCHING_BRACKET);
		setAction(GotoMatchingBracketAction.GOTO_MATCHING_BRACKET, action);

		action = new TextOperationAction(JavaEditorMessages.getBundleForConstructedKeys(), "ShowOutline.", this, JavaSourceViewer.SHOW_OUTLINE, true); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_OUTLINE);
		setAction(IJavaEditorActionDefinitionIds.SHOW_OUTLINE, action);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(action, IJavaHelpContextIds.SHOW_OUTLINE_ACTION);

		action = new TextOperationAction(JavaEditorMessages.getBundleForConstructedKeys(), "OpenStructure.", this, JavaSourceViewer.OPEN_STRUCTURE, true); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_STRUCTURE);
		setAction(IJavaEditorActionDefinitionIds.OPEN_STRUCTURE, action);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(action, IJavaHelpContextIds.OPEN_STRUCTURE_ACTION);

		action = new TextOperationAction(JavaEditorMessages.getBundleForConstructedKeys(), "OpenHierarchy.", this, JavaSourceViewer.SHOW_HIERARCHY, true); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_HIERARCHY);
		setAction(IJavaEditorActionDefinitionIds.OPEN_HIERARCHY, action);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(action, IJavaHelpContextIds.OPEN_HIERARCHY_ACTION);

		fSelectionHistory = new SelectionHistory(this);

		action = new StructureSelectEnclosingAction(this, fSelectionHistory);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_ENCLOSING);
		setAction(StructureSelectionAction.ENCLOSING, action);

		action = new StructureSelectNextAction(this, fSelectionHistory);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_NEXT);
		setAction(StructureSelectionAction.NEXT, action);

		action = new StructureSelectPreviousAction(this, fSelectionHistory);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_PREVIOUS);
		setAction(StructureSelectionAction.PREVIOUS, action);

		StructureSelectHistoryAction historyAction = new StructureSelectHistoryAction(this, fSelectionHistory);
		historyAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_LAST);
		setAction(StructureSelectionAction.HISTORY, historyAction);
		fSelectionHistory.setHistoryAction(historyAction);

		action = GoToNextPreviousMemberAction.newGoToNextMemberAction(this);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_NEXT_MEMBER);
		setAction(GoToNextPreviousMemberAction.NEXT_MEMBER, action);

		action = GoToNextPreviousMemberAction.newGoToPreviousMemberAction(this);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_PREVIOUS_MEMBER);
		setAction(GoToNextPreviousMemberAction.PREVIOUS_MEMBER, action);

		action = new FormatElementAction();
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.QUICK_FORMAT);
		setAction("QuickFormat", action); //$NON-NLS-1$
		markAsStateDependentAction("QuickFormat", true); //$NON-NLS-1$

		action = new RemoveOccurrenceAnnotations(this);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.REMOVE_OCCURRENCE_ANNOTATIONS);
		setAction("RemoveOccurrenceAnnotations", action); //$NON-NLS-1$

		// add annotation actions for roll-over expand hover
		action = new JavaSelectMarkerRulerAction2(JavaEditorMessages.getBundleForConstructedKeys(), "Editor.RulerAnnotationSelection.", this); //$NON-NLS-1$
		setAction("AnnotationAction", action); //$NON-NLS-1$

		action = new ShowInPackageViewAction(this);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_IN_PACKAGE_VIEW);
		setAction("ShowInPackageView", action); //$NON-NLS-1$


		// replace cut/copy paste actions with a version that implement 'add
		// imports on paste'

		action = new ClipboardOperationAction(JavaEditorMessages.getBundleForConstructedKeys(), "Editor.Cut.", this, ITextOperationTarget.CUT); //$NON-NLS-1$
		setAction(ITextEditorActionConstants.CUT, action);

		action = new ClipboardOperationAction(JavaEditorMessages.getBundleForConstructedKeys(), "Editor.Copy.", this, ITextOperationTarget.COPY); //$NON-NLS-1$
		setAction(ITextEditorActionConstants.COPY, action);

		action = new ClipboardOperationAction(JavaEditorMessages.getBundleForConstructedKeys(), "Editor.Paste.", this, ITextOperationTarget.PASTE); //$NON-NLS-1$
		setAction(ITextEditorActionConstants.PASTE, action);

		action = new CopyQualifiedNameAction(this);
		setAction(IJavaEditorActionConstants.COPY_QUALIFIED_NAME, action);
	}

	/**
	 * Installs the encoding support on the given text editor.
	 * <p>
	 * Subclasses may override to install their own encoding support or to
	 * disable the default encoding support.
	 * </p>
	 * 
	 */
	protected void installEncodingSupport() {
		fEncodingSupport = new DefaultEncodingSupport();
		fEncodingSupport.initialize(this);
	}


	public void updatedTitleImage(Image image) {
		setTitleImage(image);
	}

	/*
	 * @see
	 * AbstractTextEditor#handlePreferenceStoreChanged(PropertyChangeEvent)
	 */
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {

		String property = event.getProperty();

		if (AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH.equals(property)) {
			/*
			 * Ignore tab setting since we rely on the formatter preferences.
			 * We do this outside the try-finally block to avoid that
			 * EDITOR_TAB_WIDTH is handled by the sub-class
			 * (AbstractDecoratedTextEditor).
			 */
			return;
		}

		try {

			ISourceViewer sourceViewer = getSourceViewer();
			if (sourceViewer == null)
				return;

			if (isJavaEditorHoverProperty(property))
				updateHoverBehavior();

			boolean newBooleanValue = false;
			Object newValue = event.getNewValue();
			if (newValue != null)
				newBooleanValue = Boolean.valueOf(newValue.toString()).booleanValue();

			if (PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE.equals(property)) {
				if (newBooleanValue)
					selectionChanged();
				return;
			}

			if (PreferenceConstants.EDITOR_MARK_OCCURRENCES.equals(property)) {
				if (newBooleanValue != fMarkOccurrenceAnnotations) {
					fMarkOccurrenceAnnotations = newBooleanValue;
					if (!fMarkOccurrenceAnnotations)
						uninstallOccurrencesFinder();
					else
						installOccurrencesFinder(true);
				}
				return;
			}
			if (PreferenceConstants.EDITOR_MARK_TYPE_OCCURRENCES.equals(property)) {
				fMarkTypeOccurrences = newBooleanValue;
				return;
			}
			if (PreferenceConstants.EDITOR_MARK_METHOD_OCCURRENCES.equals(property)) {
				fMarkMethodOccurrences = newBooleanValue;
				return;
			}
			if (PreferenceConstants.EDITOR_MARK_CONSTANT_OCCURRENCES.equals(property)) {
				fMarkConstantOccurrences = newBooleanValue;
				return;
			}
			if (PreferenceConstants.EDITOR_MARK_FIELD_OCCURRENCES.equals(property)) {
				fMarkFieldOccurrences = newBooleanValue;
				return;
			}
			if (PreferenceConstants.EDITOR_MARK_LOCAL_VARIABLE_OCCURRENCES.equals(property)) {
				fMarkLocalVariableypeOccurrences = newBooleanValue;
				return;
			}
			if (PreferenceConstants.EDITOR_MARK_EXCEPTION_OCCURRENCES.equals(property)) {
				fMarkExceptions = newBooleanValue;
				return;
			}
			if (PreferenceConstants.EDITOR_MARK_METHOD_EXIT_POINTS.equals(property)) {
				fMarkMethodExitPoints = newBooleanValue;
				return;
			}
			if (PreferenceConstants.EDITOR_MARK_BREAK_CONTINUE_TARGETS.equals(property)) {
				fMarkBreakContinueTargets = newBooleanValue;
				return;
			}
			if (PreferenceConstants.EDITOR_MARK_IMPLEMENTORS.equals(property)) {
				fMarkImplementors = newBooleanValue;
				return;
			}
			if (PreferenceConstants.EDITOR_STICKY_OCCURRENCES.equals(property)) {
				fStickyOccurrenceAnnotations = newBooleanValue;
				return;
			}
			if (SemanticHighlightings.affectsEnablement(getPreferenceStore(), event)) {
				if (isSemanticHighlightingEnabled())
					installSemanticHighlighting();
				else
					uninstallSemanticHighlighting();
				return;
			}

			if (JavaScriptCore.COMPILER_SOURCE.equals(property)) {
				if (event.getNewValue() instanceof String)
					fBracketMatcher.setSourceVersion((String) event.getNewValue());
				// fall through as others are interested in source change as
				// well.
			}

			((JavaScriptSourceViewerConfiguration) getSourceViewerConfiguration()).handlePropertyChangeEvent(event);

			if (affectsOverrideIndicatorAnnotations(event)) {
				if (isShowingOverrideIndicators()) {
					if (fOverrideIndicatorManager == null)
						installOverrideIndicator(true);
				}
				else {
					if (fOverrideIndicatorManager != null)
						uninstallOverrideIndicator();
				}
				return;
			}

			if (PreferenceConstants.EDITOR_FOLDING_PROVIDER.equals(property)) {
				if (sourceViewer instanceof ProjectionViewer) {
					ProjectionViewer projectionViewer = (ProjectionViewer) sourceViewer;
					if (fProjectionModelUpdater != null)
						fProjectionModelUpdater.uninstall();
					// either freshly enabled or provider changed
					fProjectionModelUpdater = JavaScriptPlugin.getDefault().getFoldingStructureProviderRegistry().getCurrentFoldingProvider();
					if (fProjectionModelUpdater != null) {
						fProjectionModelUpdater.install(this, projectionViewer);
					}
				}
				return;
			}

			if (DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE.equals(property) || DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE.equals(property) || DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR.equals(property)) {
				StyledText textWidget = sourceViewer.getTextWidget();
				int tabWidth = getSourceViewerConfiguration().getTabWidth(sourceViewer);
				if (textWidget.getTabs() != tabWidth)
					textWidget.setTabs(tabWidth);
				return;
			}

			if (PreferenceConstants.EDITOR_FOLDING_ENABLED.equals(property)) {
				if (sourceViewer instanceof ProjectionViewer) {
					new ToggleFoldingRunner().runWhenNextVisible();
				}
				return;
			}

		}
		finally {
			super.handlePreferenceStoreChanged(event);
		}

		if (AbstractDecoratedTextEditorPreferenceConstants.SHOW_RANGE_INDICATOR.equals(property)) {
			// superclass already installed the range indicator
			Object newValue = event.getNewValue();
			ISourceViewer viewer = getSourceViewer();
			if (newValue != null && viewer != null) {
				if (Boolean.valueOf(newValue.toString()).booleanValue()) {
					// adjust the highlightrange in order to get the magnet
					// right after changing the selection
					Point selection = viewer.getSelectedRange();
					adjustHighlightRange(selection.x, selection.y);
				}
			}

		}
	}

	/**
	 * Initializes the given viewer's colors.
	 *
	 * @param viewer
	 *            the viewer to be initialized
	 * 
	 */
	protected void initializeViewerColors(ISourceViewer viewer) {
		// is handled by JavaSourceViewer
	}

	private boolean isJavaEditorHoverProperty(String property) {
		return PreferenceConstants.EDITOR_TEXT_HOVER_MODIFIERS.equals(property);
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#
	 * updatePropertyDependentActions()
	 */
	protected void updatePropertyDependentActions() {
		super.updatePropertyDependentActions();
		if (fEncodingSupport != null)
			fEncodingSupport.reset();
	}

	/*
	 * Update the hovering behavior depending on the preferences.
	 */
	private void updateHoverBehavior() {
		SourceViewerConfiguration configuration = getSourceViewerConfiguration();
		String[] types = configuration.getConfiguredContentTypes(getSourceViewer());

		for (int i = 0; i < types.length; i++) {

			String t = types[i];

			ISourceViewer sourceViewer = getSourceViewer();
			if (sourceViewer instanceof ITextViewerExtension2) {
				// Remove existing hovers
				((ITextViewerExtension2) sourceViewer).removeTextHovers(t);

				int[] stateMasks = configuration.getConfiguredTextHoverStateMasks(getSourceViewer(), t);

				if (stateMasks != null) {
					for (int j = 0; j < stateMasks.length; j++) {
						int stateMask = stateMasks[j];
						ITextHover textHover = configuration.getTextHover(sourceViewer, t, stateMask);
						((ITextViewerExtension2) sourceViewer).setTextHover(textHover, t, stateMask);
					}
				}
				else {
					ITextHover textHover = configuration.getTextHover(sourceViewer, t);
					((ITextViewerExtension2) sourceViewer).setTextHover(textHover, t, ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK);
				}
			}
			else
				sourceViewer.setTextHover(configuration.getTextHover(sourceViewer, t), t);
		}
	}

	/*
	 * @see
	 * org.eclipse.wst.jsdt.internal.ui.viewsupport.IViewPartInputProvider#
	 * getViewPartInput()
	 */
	public Object getViewPartInput() {
		return getEditorInput().getAdapter(IJavaScriptElement.class);
	}

	/*
	 * @see
	 * org.eclipse.ui.texteditor.AbstractTextEditor#doSetSelection(ISelection)
	 */
	protected void doSetSelection(ISelection selection) {
		super.doSetSelection(selection);
		synchronizeOutlinePageSelection();
	}

	boolean isFoldingEnabled() {
		return JavaScriptPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_FOLDING_ENABLED);
	}

	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#getOrientation()
	 * 
	 */
	public int getOrientation() {
		return SWT.LEFT_TO_RIGHT; // Java editors are always left to right by
									// default
	}

	/*
	 * @see
	 * org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	public void createPartControl(Composite parent) {
		long t = currentTimeMillis();

		super.createPartControl(parent);

		fEditorSelectionChangedListener = new EditorSelectionChangedListener();
		fEditorSelectionChangedListener.install(getSelectionProvider());

		if (isSemanticHighlightingEnabled())
			installSemanticHighlighting();

		PlatformUI.getWorkbench().addWindowListener(fActivationListener);
	}

	protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {

		fBracketMatcher.setSourceVersion(getPreferenceStore().getString(JavaScriptCore.COMPILER_SOURCE));
		support.setCharacterPairMatcher(fBracketMatcher);
		support.setMatchingCharacterPainterPreferenceKeys(MATCHING_BRACKETS, MATCHING_BRACKETS_COLOR);

		super.configureSourceViewerDecorationSupport(support);
	}

	/**
	 * Returns the lock object for the given annotation model.
	 *
	 * @param annotationModel
	 *            the annotation model
	 * @return the annotation model's lock object
	 * 
	 */
	private Object getLockObject(IAnnotationModel annotationModel) {
		if (annotationModel instanceof ISynchronizable) {
			Object lock = ((ISynchronizable) annotationModel).getLockObject();
			if (lock != null)
				return lock;
		}
		return annotationModel;
	}

	/*
	 * @see
	 * org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#updateMarkerViews
	 * (org.eclipse.jface.text.source.Annotation)
	 * 
	 */
	protected void updateMarkerViews(Annotation annotation) {
		if (annotation instanceof IJavaAnnotation) {
			Iterator e = ((IJavaAnnotation) annotation).getOverlaidIterator();
			if (e != null) {
				while (e.hasNext()) {
					Object o = e.next();
					if (o instanceof MarkerAnnotation) {
						super.updateMarkerViews((MarkerAnnotation) o);
						return;
					}
				}
			}
			return;
		}
		super.updateMarkerViews(annotation);
	}

	/**
	 * Finds and marks occurrence annotations.
	 *
	 * 
	 */
	class OccurrencesFinderJob extends Job {

		private IDocument fDocument;
		private ISelection fSelection;
		private ISelectionValidator fPostSelectionValidator;
		private boolean fCanceled = false;
		private IProgressMonitor fProgressMonitor;
		private Position[] fPositions;

		public OccurrencesFinderJob(IDocument document, Position[] positions, ISelection selection) {
			super(JavaEditorMessages.JavaEditor_markOccurrences_job_name);
			fDocument = document;
			fSelection = selection;
			fPositions = positions;

			if (getSelectionProvider() instanceof ISelectionValidator)
				fPostSelectionValidator = (ISelectionValidator) getSelectionProvider();
		}

		// cannot use cancel() because it is declared final
		void doCancel() {
			fCanceled = true;
			cancel();
		}

		private boolean isCanceled() {
			return fCanceled || fProgressMonitor.isCanceled() || fPostSelectionValidator != null && !(fPostSelectionValidator.isValid(fSelection) || fForcedMarkOccurrencesSelection == fSelection) || LinkedModeModel.hasInstalledModel(fDocument);
		}

		/*
		 * @see Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public IStatus run(IProgressMonitor progressMonitor) {

			fProgressMonitor = progressMonitor;

			if (isCanceled())
				return Status.CANCEL_STATUS;

			ITextViewer textViewer = getViewer();
			if (textViewer == null)
				return Status.CANCEL_STATUS;

			IDocument document = textViewer.getDocument();
			if (document == null)
				return Status.CANCEL_STATUS;

			IDocumentProvider documentProvider = getDocumentProvider();
			if (documentProvider == null)
				return Status.CANCEL_STATUS;

			IAnnotationModel annotationModel = documentProvider.getAnnotationModel(getEditorInput());
			if (annotationModel == null)
				return Status.CANCEL_STATUS;

			// Add occurrence annotations
			int length = fPositions.length;
			Map annotationMap = new HashMap(length);
			for (int i = 0; i < length; i++) {

				if (isCanceled())
					return Status.CANCEL_STATUS;

				String message;
				Position position = fPositions[i];

				// Create & add annotation
				try {
					message = document.get(position.offset, position.length);
				}
				catch (BadLocationException ex) {
					// Skip this match
					continue;
				}
				annotationMap.put(new Annotation("org.eclipse.wst.jsdt.ui.occurrences", false, message), //$NON-NLS-1$
							position);
			}

			if (isCanceled())
				return Status.CANCEL_STATUS;

			synchronized (getLockObject(annotationModel)) {
				if (annotationModel instanceof IAnnotationModelExtension) {
					((IAnnotationModelExtension) annotationModel).replaceAnnotations(fOccurrenceAnnotations, annotationMap);
				}
				else {
					removeOccurrenceAnnotations();
					Iterator iter = annotationMap.entrySet().iterator();
					while (iter.hasNext()) {
						Map.Entry mapEntry = (Map.Entry) iter.next();
						annotationModel.addAnnotation((Annotation) mapEntry.getKey(), (Position) mapEntry.getValue());
					}
				}
				fOccurrenceAnnotations = (Annotation[]) annotationMap.keySet().toArray(new Annotation[annotationMap.keySet().size()]);
			}

			return Status.OK_STATUS;
		}
	}

	/**
	 * Updates the occurrences annotations based on the current selection.
	 *
	 * @param selection
	 *            the text selection
	 * @param astRoot
	 *            the compilation unit AST
	 * 
	 */
	protected void updateOccurrenceAnnotations(ITextSelection selection, JavaScriptUnit astRoot) {

		if (fOccurrencesFinderJob != null)
			fOccurrencesFinderJob.cancel();

		if (!fMarkOccurrenceAnnotations)
			return;

		if (astRoot == null || selection == null)
			return;

		IDocument document = getSourceViewer().getDocument();
		if (document == null)
			return;

		if (document instanceof IDocumentExtension4) {
			int offset = selection.getOffset();
			long currentModificationStamp = ((IDocumentExtension4) document).getModificationStamp();
			IRegion markOccurrenceTargetRegion = fMarkOccurrenceTargetRegion;
			if (markOccurrenceTargetRegion != null && currentModificationStamp == fMarkOccurrenceModificationStamp) {
				if (markOccurrenceTargetRegion.getOffset() <= offset && offset <= markOccurrenceTargetRegion.getOffset() + markOccurrenceTargetRegion.getLength())
					return;
			}
			fMarkOccurrenceTargetRegion = JavaWordFinder.findWord(document, offset);
			fMarkOccurrenceModificationStamp = currentModificationStamp;
		}

		List matches = null;

		ASTNode selectedNode = NodeFinder.perform(astRoot, selection.getOffset(), selection.getLength());

		if (fMarkExceptions || fMarkTypeOccurrences) {
			ExceptionOccurrencesFinder exceptionFinder = new ExceptionOccurrencesFinder();
			String message = exceptionFinder.initialize(astRoot, selectedNode);
			if (message == null) {
				matches = exceptionFinder.perform();
				if (!fMarkExceptions && !matches.isEmpty())
					matches.clear();
			}
		}

		if ((matches == null || matches.isEmpty()) && (fMarkMethodExitPoints || fMarkTypeOccurrences)) {
			MethodExitsFinder finder = new MethodExitsFinder();
			String message = finder.initialize(astRoot, selectedNode);
			if (message == null) {
				matches = finder.perform();
				if (!fMarkMethodExitPoints && !matches.isEmpty())
					matches.clear();
			}
		}

		if ((matches == null || matches.isEmpty()) && (fMarkBreakContinueTargets || fMarkTypeOccurrences)) {
			BreakContinueTargetFinder finder = new BreakContinueTargetFinder();
			String message = finder.initialize(astRoot, selectedNode);
			if (message == null) {
				matches = finder.perform();
				if (!fMarkBreakContinueTargets && !matches.isEmpty())
					matches.clear();
			}
		}

		if ((matches == null || matches.isEmpty()) && (fMarkImplementors || fMarkTypeOccurrences)) {
			ImplementOccurrencesFinder finder = new ImplementOccurrencesFinder();
			String message = finder.initialize(astRoot, selectedNode);
			if (message == null) {
				matches = finder.perform();
				if (!fMarkImplementors && !matches.isEmpty())
					matches.clear();
			}
		}

		if (matches == null) {
			IBinding binding = null;
			if (selectedNode instanceof Name)
				binding = ((Name) selectedNode).resolveBinding();

			if (binding != null && markOccurrencesOfType(binding)) {
				// Find the matches && extract positions so we can forget the
				// AST
				OccurrencesFinder finder = new OccurrencesFinder(binding);
				String message = finder.initialize(astRoot, selectedNode);
				if (message == null)
					matches = finder.perform();
			}
		}

		if (matches == null || matches.size() == 0) {
			if (!fStickyOccurrenceAnnotations)
				removeOccurrenceAnnotations();
			return;
		}

		Position[] positions = new Position[matches.size()];
		int i = 0;
		for (Iterator each = matches.iterator(); each.hasNext();) {
			ASTNode currentNode = (ASTNode) each.next();
			positions[i++] = new Position(currentNode.getStartPosition(), currentNode.getLength());
		}

		fOccurrencesFinderJob = new OccurrencesFinderJob(document, positions, selection);
		// fOccurrencesFinderJob.setPriority(Job.DECORATE);
		// fOccurrencesFinderJob.setSystem(true);
		// fOccurrencesFinderJob.schedule();
		fOccurrencesFinderJob.run(new NullProgressMonitor());
	}

	protected void installOccurrencesFinder(boolean forceUpdate) {
		fMarkOccurrenceAnnotations = true;

		fPostSelectionListenerWithAST = new ISelectionListenerWithAST() {
			public void selectionChanged(IEditorPart part, ITextSelection selection, JavaScriptUnit astRoot) {
				updateOccurrenceAnnotations(selection, astRoot);
			}
		};
		SelectionListenerWithASTManager.getDefault().addListener(this, fPostSelectionListenerWithAST);
		if (forceUpdate && getSelectionProvider() != null) {
			fForcedMarkOccurrencesSelection = getSelectionProvider().getSelection();
			updateOccurrenceAnnotations((ITextSelection) fForcedMarkOccurrencesSelection, JavaScriptPlugin.getDefault().getASTProvider().getAST(getInputJavaElement(), ASTProvider.WAIT_NO, getProgressMonitor()));
		}

		if (fOccurrencesFinderJobCanceler == null) {
			fOccurrencesFinderJobCanceler = new OccurrencesFinderJobCanceler();
			fOccurrencesFinderJobCanceler.install();
		}
	}

	protected void uninstallOccurrencesFinder() {
		fMarkOccurrenceAnnotations = false;

		if (fOccurrencesFinderJob != null) {
			fOccurrencesFinderJob.cancel();
			fOccurrencesFinderJob = null;
		}

		if (fOccurrencesFinderJobCanceler != null) {
			fOccurrencesFinderJobCanceler.uninstall();
			fOccurrencesFinderJobCanceler = null;
		}

		if (fPostSelectionListenerWithAST != null) {
			SelectionListenerWithASTManager.getDefault().removeListener(this, fPostSelectionListenerWithAST);
			fPostSelectionListenerWithAST = null;
		}

		removeOccurrenceAnnotations();
	}

	protected boolean isMarkingOccurrences() {
		IPreferenceStore store = getPreferenceStore();
		return store != null && store.getBoolean(PreferenceConstants.EDITOR_MARK_OCCURRENCES);
	}

	boolean markOccurrencesOfType(IBinding binding) {

		if (binding == null)
			return false;

		int kind = binding.getKind();

		if (fMarkTypeOccurrences && kind == IBinding.TYPE)
			return true;

		if (fMarkMethodOccurrences && kind == IBinding.METHOD)
			return true;

		if (kind == IBinding.VARIABLE) {
			IVariableBinding variableBinding = (IVariableBinding) binding;
			if (variableBinding.isField()) {
				int constantModifier = IModifierConstants.ACC_STATIC | IModifierConstants.ACC_FINAL;
				boolean isConstant = (variableBinding.getModifiers() & constantModifier) == constantModifier;
				if (isConstant)
					return fMarkConstantOccurrences;
				else
					return fMarkFieldOccurrences;
			}

			return fMarkLocalVariableypeOccurrences;
		}

		return false;
	}

	void removeOccurrenceAnnotations() {
		fMarkOccurrenceModificationStamp = IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP;
		fMarkOccurrenceTargetRegion = null;

		IDocumentProvider documentProvider = getDocumentProvider();
		if (documentProvider == null)
			return;

		IAnnotationModel annotationModel = documentProvider.getAnnotationModel(getEditorInput());
		if (annotationModel == null || fOccurrenceAnnotations == null)
			return;

		synchronized (getLockObject(annotationModel)) {
			if (annotationModel instanceof IAnnotationModelExtension) {
				((IAnnotationModelExtension) annotationModel).replaceAnnotations(fOccurrenceAnnotations, null);
			}
			else {
				for (int i = 0, length = fOccurrenceAnnotations.length; i < length; i++)
					annotationModel.removeAnnotation(fOccurrenceAnnotations[i]);
			}
			fOccurrenceAnnotations = null;
		}
	}

	protected void uninstallOverrideIndicator() {
		if (fOverrideIndicatorManager != null) {
			fOverrideIndicatorManager.removeAnnotations();
			fOverrideIndicatorManager = null;
		}
	}

	protected void installOverrideIndicator(boolean provideAST) {
		uninstallOverrideIndicator();
		IAnnotationModel model = getDocumentProvider().getAnnotationModel(getEditorInput());
		final IJavaScriptElement inputElement = getInputJavaElement();

		if (model == null || inputElement == null)
			return;

		fOverrideIndicatorManager = new OverrideIndicatorManager(model, inputElement, null);

		if (provideAST) {
			JavaScriptUnit ast = JavaScriptPlugin.getDefault().getASTProvider().getAST(inputElement, ASTProvider.WAIT_ACTIVE_ONLY, getProgressMonitor());
			fOverrideIndicatorManager.reconciled(ast, true, getProgressMonitor());
		}
	}

	/**
	 * Tells whether override indicators are shown.
	 *
	 * @return <code>true</code> if the override indicators are shown
	 * 
	 */
	protected boolean isShowingOverrideIndicators() {
		AnnotationPreference preference = getAnnotationPreferenceLookup().getAnnotationPreference(OverrideIndicatorManager.ANNOTATION_TYPE);
		IPreferenceStore store = getPreferenceStore();

		return getBoolean(store, PreferenceConstants.EDITOR_OVERRIDE_INDICATORS) && (getBoolean(store, preference.getHighlightPreferenceKey()) || getBoolean(store, preference.getVerticalRulerPreferenceKey()) || getBoolean(store, preference.getOverviewRulerPreferenceKey()) || getBoolean(store, preference.getTextPreferenceKey()));
	}

	/**
	 * Returns the boolean preference for the given key.
	 *
	 * @param store
	 *            the preference store
	 * @param key
	 *            the preference key
	 * @return <code>true</code> if the key exists in the store and its value
	 *         is <code>true</code>
	 * 
	 */
	private boolean getBoolean(IPreferenceStore store, String key) {
		return key != null && store.getBoolean(key);
	}

	/**
	 * Determines whether the preference change encoded by the given event
	 * changes the override indication.
	 *
	 * @param event
	 *            the event to be investigated
	 * @return <code>true</code> if event causes a change
	 * 
	 */
	protected boolean affectsOverrideIndicatorAnnotations(PropertyChangeEvent event) {
		String key = event.getProperty();
		AnnotationPreference preference = getAnnotationPreferenceLookup().getAnnotationPreference(OverrideIndicatorManager.ANNOTATION_TYPE);
		if (key == null || preference == null)
			return false;

		return key.equals(PreferenceConstants.EDITOR_OVERRIDE_INDICATORS) || key.equals(preference.getHighlightPreferenceKey()) || key.equals(preference.getVerticalRulerPreferenceKey()) || key.equals(preference.getOverviewRulerPreferenceKey()) || key.equals(preference.getTextPreferenceKey());
	}

	/**
	 * @return <code>true</code> if Semantic Highlighting is enabled.
	 *
	 * 
	 */
	private boolean isSemanticHighlightingEnabled() {
		return SemanticHighlightings.isEnabled(getPreferenceStore());
	}

	/**
	 * Install Semantic Highlighting.
	 *
	 * 
	 */
	private void installSemanticHighlighting() {
		if (fSemanticManager == null) {
			fSemanticManager = new SemanticHighlightingManager();
			fSemanticManager.install(this, (JavaSourceViewer) getSourceViewer(), JavaScriptPlugin.getDefault().getJavaTextTools().getColorManager(), getPreferenceStore());
		}
	}

	/**
	 * Uninstall Semantic Highlighting.
	 *
	 * 
	 */
	private void uninstallSemanticHighlighting() {
		if (fSemanticManager != null) {
			fSemanticManager.uninstall();
			fSemanticManager = null;
		}
	}

	/**
	 * Returns the Java element wrapped by this editors input.
	 *
	 * @return the Java element wrapped by this editors input.
	 * 
	 */
	protected IJavaScriptElement getInputJavaElement() {
		return EditorUtility.getEditorInputJavaElement(this, false);
	}

	protected void updateStatusLine() {
		ITextSelection selection = (ITextSelection) getSelectionProvider().getSelection();
		Annotation annotation = getAnnotation(selection.getOffset(), selection.getLength());
		setStatusLineErrorMessage(null);
		setStatusLineMessage(null);
		if (annotation != null) {
			updateMarkerViews(annotation);
			if (annotation instanceof IJavaAnnotation && ((IJavaAnnotation) annotation).isProblem())
				setStatusLineMessage(annotation.getText());
		}
	}

	/**
	 * Jumps to the matching bracket.
	 */
	public void gotoMatchingBracket() {

		ISourceViewer sourceViewer = getSourceViewer();
		IDocument document = sourceViewer.getDocument();
		if (document == null)
			return;

		IRegion selection = getSignedSelection(sourceViewer);

		int selectionLength = Math.abs(selection.getLength());
		if (selectionLength > 1) {
			setStatusLineErrorMessage(JavaEditorMessages.GotoMatchingBracket_error_invalidSelection);
			sourceViewer.getTextWidget().getDisplay().beep();
			return;
		}

		// #26314
		int sourceCaretOffset = selection.getOffset() + selection.getLength();
		if (isSurroundedByBrackets(document, sourceCaretOffset))
			sourceCaretOffset -= selection.getLength();

		IRegion region = fBracketMatcher.match(document, sourceCaretOffset);
		if (region == null) {
			setStatusLineErrorMessage(JavaEditorMessages.GotoMatchingBracket_error_noMatchingBracket);
			sourceViewer.getTextWidget().getDisplay().beep();
			return;
		}

		int offset = region.getOffset();
		int length = region.getLength();

		if (length < 1)
			return;

		int anchor = fBracketMatcher.getAnchor();
		// http://dev.eclipse.org/bugs/show_bug.cgi?id=34195
		int targetOffset = (ICharacterPairMatcher.RIGHT == anchor) ? offset + 1 : offset + length;

		boolean visible = false;
		if (sourceViewer instanceof ITextViewerExtension5) {
			ITextViewerExtension5 extension = (ITextViewerExtension5) sourceViewer;
			visible = (extension.modelOffset2WidgetOffset(targetOffset) > -1);
		}
		else {
			IRegion visibleRegion = sourceViewer.getVisibleRegion();
			// http://dev.eclipse.org/bugs/show_bug.cgi?id=34195
			visible = (targetOffset >= visibleRegion.getOffset() && targetOffset <= visibleRegion.getOffset() + visibleRegion.getLength());
		}

		if (!visible) {
			setStatusLineErrorMessage(JavaEditorMessages.GotoMatchingBracket_error_bracketOutsideSelectedElement);
			sourceViewer.getTextWidget().getDisplay().beep();
			return;
		}

		if (selection.getLength() < 0)
			targetOffset -= selection.getLength();

		sourceViewer.setSelectedRange(targetOffset, selection.getLength());
		sourceViewer.revealRange(targetOffset, selection.getLength());
	}

	/**
	 * Returns the signed current selection. The length will be negative if
	 * the resulting selection is right-to-left (RtoL).
	 * <p>
	 * The selection offset is model based.
	 * </p>
	 *
	 * @param sourceViewer
	 *            the source viewer
	 * @return a region denoting the current signed selection, for a resulting
	 *         RtoL selections length is < 0
	 */
	protected IRegion getSignedSelection(ISourceViewer sourceViewer) {
		StyledText text = sourceViewer.getTextWidget();
		Point selection = text.getSelectionRange();

		if (text.getCaretOffset() == selection.x) {
			selection.x = selection.x + selection.y;
			selection.y = -selection.y;
		}

		selection.x = widgetOffset2ModelOffset(sourceViewer, selection.x);

		return new Region(selection.x, selection.y);
	}

	/**
	 * Returns the cached selected range, which allows to query it from a
	 * non-UI thread.
	 * <p>
	 * The result might be outdated if queried from a non-UI thread.</em>
	 * </p>
	 *
	 * @return the caret offset in the master document
	 * @see ITextViewer#getSelectedRange()
	 * 
	 */
	public Point getCachedSelectedRange() {
		return fCachedSelectedRange;
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#
	 * handleCursorPositionChanged()
	 * 
	 */
	protected void handleCursorPositionChanged() {
		super.handleCursorPositionChanged();
		fCachedSelectedRange = getViewer().getSelectedRange();
	}

	private static boolean isBracket(char character) {
		for (int i = 0; i != BRACKETS.length; ++i)
			if (character == BRACKETS[i])
				return true;
		return false;
	}

	private static boolean isSurroundedByBrackets(IDocument document, int offset) {
		if (offset == 0 || offset == document.getLength())
			return false;

		try {
			return isBracket(document.getChar(offset - 1)) && isBracket(document.getChar(offset));

		}
		catch (BadLocationException e) {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Overrides the default implementation to handle {@link IJavaAnnotation}.
	 * </p>
	 *
	 * @param offset
	 *            the region offset
	 * @param length
	 *            the region length
	 * @param forward
	 *            <code>true</code> for forwards, <code>false</code> for
	 *            backward
	 * @param annotationPosition
	 *            the position of the found annotation
	 * @return the found annotation
	 * 
	 */
	protected Annotation findAnnotation(final int offset, final int length, boolean forward, Position annotationPosition) {

		Annotation nextAnnotation = null;
		Position nextAnnotationPosition = null;
		Annotation containingAnnotation = null;
		Position containingAnnotationPosition = null;
		boolean currentAnnotation = false;

		IDocument document = getDocumentProvider().getDocument(getEditorInput());
		int endOfDocument = document.getLength();
		int distance = Integer.MAX_VALUE;

		IAnnotationModel model = getDocumentProvider().getAnnotationModel(getEditorInput());
		Iterator e = new JavaAnnotationIterator(model, true, true);
		while (e.hasNext()) {
			Annotation a = (Annotation) e.next();
			if ((a instanceof IJavaAnnotation) && ((IJavaAnnotation) a).hasOverlay() || !isNavigationTarget(a))
				continue;

			Position p = model.getPosition(a);
			if (p == null)
				continue;

			if (forward && p.offset == offset || !forward && p.offset + p.getLength() == offset + length) {// ||
																											// p.includes(offset))
																											// {
				if (containingAnnotation == null || (forward && p.length >= containingAnnotationPosition.length || !forward && p.length >= containingAnnotationPosition.length)) {
					containingAnnotation = a;
					containingAnnotationPosition = p;
					currentAnnotation = p.length == length;
				}
			}
			else {
				int currentDistance = 0;

				if (forward) {
					currentDistance = p.getOffset() - offset;
					if (currentDistance < 0)
						currentDistance = endOfDocument + currentDistance;

					if (currentDistance < distance || currentDistance == distance && p.length < nextAnnotationPosition.length) {
						distance = currentDistance;
						nextAnnotation = a;
						nextAnnotationPosition = p;
					}
				}
				else {
					currentDistance = offset + length - (p.getOffset() + p.length);
					if (currentDistance < 0)
						currentDistance = endOfDocument + currentDistance;

					if (currentDistance < distance || currentDistance == distance && p.length < nextAnnotationPosition.length) {
						distance = currentDistance;
						nextAnnotation = a;
						nextAnnotationPosition = p;
					}
				}
			}
		}
		if (containingAnnotationPosition != null && (!currentAnnotation || nextAnnotation == null)) {
			annotationPosition.setOffset(containingAnnotationPosition.getOffset());
			annotationPosition.setLength(containingAnnotationPosition.getLength());
			return containingAnnotation;
		}
		if (nextAnnotationPosition != null) {
			annotationPosition.setOffset(nextAnnotationPosition.getOffset());
			annotationPosition.setLength(nextAnnotationPosition.getLength());
		}

		return nextAnnotation;
	}

	/**
	 * Returns the annotation overlapping with the given range or
	 * <code>null</code>.
	 *
	 * @param offset
	 *            the region offset
	 * @param length
	 *            the region length
	 * @return the found annotation or <code>null</code>
	 * 
	 */
	private Annotation getAnnotation(int offset, int length) {
		IAnnotationModel model = getDocumentProvider().getAnnotationModel(getEditorInput());
		Iterator e = new JavaAnnotationIterator(model, true, false);
		while (e.hasNext()) {
			Annotation a = (Annotation) e.next();
			Position p = model.getPosition(a);
			if (p != null && p.overlapsWith(offset, length))
				return a;
		}
		return null;
	}

	/*
	 * @see
	 * org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#gotoAnnotation(
	 * boolean)
	 * 
	 */
	public Annotation gotoAnnotation(boolean forward) {
		fSelectionChangedViaGotoAnnotation = true;
		return super.gotoAnnotation(forward);
	}

	/**
	 * Computes and returns the source reference that includes the caret and
	 * serves as provider for the outline page selection and the editor range
	 * indication.
	 *
	 * @return the computed source reference
	 * 
	 */
	protected ISourceReference computeHighlightRangeSourceReference() {
		ISourceViewer sourceViewer = getSourceViewer();
		if (sourceViewer == null)
			return null;

		StyledText styledText = sourceViewer.getTextWidget();
		if (styledText == null)
			return null;

		int caret = 0;
		if (sourceViewer instanceof ITextViewerExtension5) {
			ITextViewerExtension5 extension = (ITextViewerExtension5) sourceViewer;
			caret = extension.widgetOffset2ModelOffset(styledText.getCaretOffset());
		}
		else {
			int offset = sourceViewer.getVisibleRegion().getOffset();
			caret = offset + styledText.getCaretOffset();
		}

		IJavaScriptElement element = getElementAt(caret, false);

		if (!(element instanceof ISourceReference))
			return null;

		if (element.getElementType() == IJavaScriptElement.IMPORT_DECLARATION) {

			IImportDeclaration declaration = (IImportDeclaration) element;
			IImportContainer container = (IImportContainer) declaration.getParent();
			ISourceRange srcRange = null;

			try {
				srcRange = container.getSourceRange();
			}
			catch (JavaScriptModelException e) {
			}

			if (srcRange != null && srcRange.getOffset() == caret)
				return container;
		}

		return (ISourceReference) element;
	}

	/**
	 * Returns the most narrow java element including the given offset.
	 *
	 * @param offset
	 *            the offset inside of the requested element
	 * @param reconcile
	 *            <code>true</code> if editor input should be reconciled in
	 *            advance
	 * @return the most narrow java element
	 * 
	 */
	protected IJavaScriptElement getElementAt(int offset, boolean reconcile) {
		return getElementAt(offset);
	}

	/*
	 * @see
	 * org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#createChangeHover
	 * ()
	 */
	protected LineChangeHover createChangeHover() {
		return new JavaChangeHover(IJavaScriptPartitions.JAVA_PARTITIONING, getOrientation());
	}

	/*
	 * @see
	 * org.eclipse.ui.texteditor.AbstractTextEditor#createNavigationActions()
	 */
	protected void createNavigationActions() {
		super.createNavigationActions();

		final StyledText textWidget = getSourceViewer().getTextWidget();

		IAction action = new SmartLineStartAction(textWidget, false);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.LINE_START);
		setAction(ITextEditorActionDefinitionIds.LINE_START, action);

		action = new SmartLineStartAction(textWidget, true);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SELECT_LINE_START);
		setAction(ITextEditorActionDefinitionIds.SELECT_LINE_START, action);

		action = new NavigatePreviousSubWordAction();
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.WORD_PREVIOUS);
		setAction(ITextEditorActionDefinitionIds.WORD_PREVIOUS, action);
		textWidget.setKeyBinding(SWT.CTRL | SWT.ARROW_LEFT, SWT.NULL);

		action = new NavigateNextSubWordAction();
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.WORD_NEXT);
		setAction(ITextEditorActionDefinitionIds.WORD_NEXT, action);
		textWidget.setKeyBinding(SWT.CTRL | SWT.ARROW_RIGHT, SWT.NULL);

		action = new SelectPreviousSubWordAction();
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS);
		setAction(ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS, action);
		textWidget.setKeyBinding(SWT.CTRL | SWT.SHIFT | SWT.ARROW_LEFT, SWT.NULL);

		action = new SelectNextSubWordAction();
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SELECT_WORD_NEXT);
		setAction(ITextEditorActionDefinitionIds.SELECT_WORD_NEXT, action);
		textWidget.setKeyBinding(SWT.CTRL | SWT.SHIFT | SWT.ARROW_RIGHT, SWT.NULL);
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#
	 * createAnnotationRulerColumn(org.eclipse.jface.text.source.
	 * CompositeRuler)
	 * 
	 */
	protected IVerticalRulerColumn createAnnotationRulerColumn(CompositeRuler ruler) {
		if (!getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_ANNOTATION_ROLL_OVER))
			return super.createAnnotationRulerColumn(ruler);

		AnnotationRulerColumn column = new AnnotationRulerColumn(VERTICAL_RULER_WIDTH, getAnnotationAccess());
		column.setHover(new JavaExpandHover(ruler, getAnnotationAccess(), new IDoubleClickListener() {

			public void doubleClick(DoubleClickEvent event) {
				// for now: just invoke ruler double click action
				triggerAction(ITextEditorActionConstants.RULER_DOUBLE_CLICK);
			}

			private void triggerAction(String actionID) {
				IAction action = getAction(actionID);
				if (action != null) {
					if (action instanceof IUpdate)
						((IUpdate) action).update();
					// hack to propagate line change
					if (action instanceof ISelectionListener) {
						((ISelectionListener) action).selectionChanged(null, null);
					}
					if (action.isEnabled())
						action.run();
				}
			}

		}));

		return column;
	}

	/**
	 * Returns the folding action group, or <code>null</code> if there is
	 * none.
	 *
	 * @return the folding action group, or <code>null</code> if there is none
	 * 
	 */
	protected FoldingActionGroup getFoldingActionGroup() {
		return fFoldingGroup;
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#performRevert()
	 */
	protected void performRevert() {
		ProjectionViewer projectionViewer = (ProjectionViewer) getSourceViewer();
		projectionViewer.setRedraw(false);
		try {

			boolean projectionMode = projectionViewer.isProjectionMode();
			if (projectionMode) {
				projectionViewer.disableProjection();
				if (fProjectionModelUpdater != null)
					fProjectionModelUpdater.uninstall();
			}

			super.performRevert();

			if (projectionMode) {
				if (fProjectionModelUpdater != null)
					fProjectionModelUpdater.install(this, projectionViewer);
				projectionViewer.enableProjection();
			}

		}
		finally {
			projectionViewer.setRedraw(true);
		}
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#
	 * rulerContextMenuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 */
	protected void rulerContextMenuAboutToShow(IMenuManager menu) {
		super.rulerContextMenuAboutToShow(menu);
		IMenuManager foldingMenu = new MenuManager(JavaEditorMessages.Editor_FoldingMenu_name, "projection"); //$NON-NLS-1$
		menu.appendToGroup(ITextEditorActionConstants.GROUP_RULERS, foldingMenu);

		IAction action = getAction("FoldingToggle"); //$NON-NLS-1$
		foldingMenu.add(action);
		action = getAction("FoldingExpandAll"); //$NON-NLS-1$
		foldingMenu.add(action);
		action = getAction("FoldingCollapseAll"); //$NON-NLS-1$
		foldingMenu.add(action);
		action = getAction("FoldingRestore"); //$NON-NLS-1$
		foldingMenu.add(action);
		action = getAction("FoldingCollapseMembers"); //$NON-NLS-1$
		foldingMenu.add(action);
		action = getAction("FoldingCollapseComments"); //$NON-NLS-1$
		foldingMenu.add(action);
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#
	 * collectContextMenuPreferencePages()
	 * 
	 */
	protected String[] collectContextMenuPreferencePages() {
		String[] inheritedPages = super.collectContextMenuPreferencePages();
		int length = 10;
		String[] result = new String[inheritedPages.length + length];
		result[0] = "org.eclipse.wst.jsdt.ui.preferences.JavaEditorPreferencePage"; //$NON-NLS-1$
		result[1] = "org.eclipse.wst.jsdt.ui.preferences.JavaTemplatePreferencePage"; //$NON-NLS-1$
		result[2] = "org.eclipse.wst.jsdt.ui.preferences.CodeAssistPreferencePage"; //$NON-NLS-1$
		result[3] = "org.eclipse.wst.jsdt.ui.preferences.CodeAssistPreferenceAdvanced"; //$NON-NLS-1$
		result[4] = "org.eclipse.wst.jsdt.ui.preferences.JavaEditorHoverPreferencePage"; //$NON-NLS-1$
		result[5] = "org.eclipse.wst.jsdt.ui.preferences.JavaEditorColoringPreferencePage"; //$NON-NLS-1$
		result[6] = "org.eclipse.wst.jsdt.ui.preferences.FoldingPreferencePage"; //$NON-NLS-1$
		result[7] = "org.eclipse.wst.jsdt.ui.preferences.MarkOccurrencesPreferencePage"; //$NON-NLS-1$
		result[8] = "org.eclipse.wst.jsdt.ui.preferences.SmartTypingPreferencePage"; //$NON-NLS-1$
		result[9] = "org.eclipse.wst.jsdt.ui.preferences.SaveParticipantPreferencePage"; //$NON-NLS-1$
		System.arraycopy(inheritedPages, 0, result, length, inheritedPages.length);
		return result;
	}

	/*
	 * @see AbstractTextEditor#getUndoRedoOperationApprover(IUndoContext)
	 * 
	 */
	protected IOperationApprover getUndoRedoOperationApprover(IUndoContext undoContext) {
		// since IResource is a more general way to compare java elements, we
		// use this as the preferred class for comparing objects.
		return new NonLocalUndoUserApprover(undoContext, this, new Object[]{getInputJavaElement()}, IResource.class);
	}

	/**
	 * Resets the foldings structure according to the folding preferences.
	 * 
	 * 
	 */
	public void resetProjection() {
		if (fProjectionModelUpdater != null) {
			fProjectionModelUpdater.initialize();
		}
	}

	/**
	 * Collapses all foldable members if supported by the folding structure
	 * provider.
	 * 
	 * 
	 */
	public void collapseMembers() {
		if (fProjectionModelUpdater instanceof IJavaFoldingStructureProviderExtension) {
			IJavaFoldingStructureProviderExtension extension = (IJavaFoldingStructureProviderExtension) fProjectionModelUpdater;
			extension.collapseMembers();
		}
	}

	/**
	 * Collapses all foldable comments if supported by the folding structure
	 * provider.
	 * 
	 * 
	 */
	public void collapseComments() {
		if (fProjectionModelUpdater instanceof IJavaFoldingStructureProviderExtension) {
			IJavaFoldingStructureProviderExtension extension = (IJavaFoldingStructureProviderExtension) fProjectionModelUpdater;
			extension.collapseComments();
		}
	}
}
