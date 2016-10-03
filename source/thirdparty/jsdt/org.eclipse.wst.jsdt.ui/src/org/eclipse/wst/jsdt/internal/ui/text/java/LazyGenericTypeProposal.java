/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.java;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.eclipse.wst.jsdt.core.CompletionProposal;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.EditorHighlightingSynchronizer;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.ui.text.java.JavaContentAssistInvocationContext;

/**
 * Proposal for generic types.
 * <p>
 * Only used when compliance is set to 5.0 or higher.
 * </p>
 */
public final class LazyGenericTypeProposal extends LazyJavaTypeCompletionProposal {
	/** Triggers for types. Do not modify. */
	private final static char[] GENERIC_TYPE_TRIGGERS= new char[] { '.', '\t', '[', '(', '<', ' ' };

	/**
	 * Short-lived context information object for generic types. Currently, these
	 * are only created after inserting a type proposal, as core doesn't give us
	 * the correct type proposal from within SomeType<|>.
	 */
	private static class ContextInformation implements IContextInformation, IContextInformationExtension {
		private final String fInformationDisplayString;
		private final String fContextDisplayString;
		private final Image fImage;
		private final int fPosition;
		
		ContextInformation(LazyGenericTypeProposal proposal) {
			// don't cache the proposal as content assistant
			// might hang on to the context info
			fContextDisplayString= proposal.getDisplayString();
			fInformationDisplayString= computeContextString(proposal);
			fImage= proposal.getImage();
			fPosition= proposal.getReplacementOffset() + proposal.getReplacementString().indexOf('<') + 1;
		}
		
		/*
		 * @see org.eclipse.jface.text.contentassist.IContextInformation#getContextDisplayString()
		 */
		public String getContextDisplayString() {
			return fContextDisplayString;
		}

		/*
		 * @see org.eclipse.jface.text.contentassist.IContextInformation#getImage()
		 */
		public Image getImage() {
			return fImage;
		}

		/*
		 * @see org.eclipse.jface.text.contentassist.IContextInformation#getInformationDisplayString()
		 */
		public String getInformationDisplayString() {
			return fInformationDisplayString;
		}

		private String computeContextString(LazyGenericTypeProposal proposal) {
			try {
				TypeArgumentProposal[] proposals= proposal.computeTypeArgumentProposals();
				if (proposals.length == 0)
					return null;
				
				StringBuffer buf= new StringBuffer();
				for (int i= 0; i < proposals.length; i++) {
					buf.append(proposals[i].getDisplayName());
					if (i < proposals.length - 1)
						buf.append(", "); //$NON-NLS-1$
				}
				return buf.toString();
				
			} catch (JavaScriptModelException e) {
				return null;
			}
		}

		/*
		 * @see org.eclipse.jface.text.contentassist.IContextInformationExtension#getContextInformationPosition()
		 */
		public int getContextInformationPosition() {
			return fPosition;
		}
		
		/*
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if (obj instanceof ContextInformation) {
				ContextInformation ci= (ContextInformation) obj;
				return getContextInformationPosition() == ci.getContextInformationPosition() && getInformationDisplayString().equals(ci.getInformationDisplayString());
			}
			return false;
		}
	}

	private static final class TypeArgumentProposal {
		private final boolean fIsAmbiguous;
		private final String fProposal;
		private final String fTypeDisplayName;

		TypeArgumentProposal(String proposal, boolean ambiguous, String typeDisplayName) {
			fIsAmbiguous= ambiguous;
			fProposal= proposal;
			fTypeDisplayName= typeDisplayName;
		}

		public String getDisplayName() {
			return fTypeDisplayName;
		}

		boolean isAmbiguous() {
			return fIsAmbiguous;
		}

		String getProposals() {
			return fProposal;
		}

		public String toString() {
			return fProposal;
		}
	}

	private IRegion fSelectedRegion; // initialized by apply()
	private TypeArgumentProposal[] fTypeArgumentProposals;

	public LazyGenericTypeProposal(CompletionProposal typeProposal, JavaContentAssistInvocationContext context) {
		super(typeProposal, context);
	}

	/*
	 * @see ICompletionProposalExtension#apply(IDocument, char)
	 */
	public void apply(IDocument document, char trigger, int offset) {

		if (shouldAppendArguments(document, offset, trigger)) {
			try {
				TypeArgumentProposal[] typeArgumentProposals= computeTypeArgumentProposals();
				if (typeArgumentProposals.length > 0) {

					int[] offsets= new int[typeArgumentProposals.length];
					int[] lengths= new int[typeArgumentProposals.length];
					StringBuffer buffer= createParameterList(typeArgumentProposals, offsets, lengths);

					// set the generic type as replacement string
					boolean insertClosingParenthesis= trigger == '(' && autocloseBrackets();
					if (insertClosingParenthesis)
						updateReplacementWithParentheses(buffer);
					super.setReplacementString(buffer.toString());

					// add import & remove package, update replacement offset
					super.apply(document, '\0', offset);

					if (getTextViewer() != null) {
						if (hasAmbiguousProposals(typeArgumentProposals)) {
							adaptOffsets(offsets, buffer);
							installLinkedMode(document, offsets, lengths, typeArgumentProposals, insertClosingParenthesis);
						} else {
							if (insertClosingParenthesis)
								setUpLinkedMode(document, ')');
							else
								fSelectedRegion= new Region(getReplacementOffset() + getReplacementString().length(), 0);
						}
					}

					return;
				}
			} catch (JavaScriptModelException e) {
				// log and continue
				JavaScriptPlugin.log(e);
			}
		}

		// default is to use the super implementation
		// reasons:
		// - not a parameterized type,
		// - already followed by <type arguments>
		// - proposal type does not inherit from expected type
		super.apply(document, trigger, offset);
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.LazyJavaTypeCompletionProposal#computeTriggerCharacters()
	 */
	protected char[] computeTriggerCharacters() {
		return GENERIC_TYPE_TRIGGERS;
	}

	/**
	 * Adapt the parameter offsets to any modification of the replacement
	 * string done by <code>apply</code>. For example, applying the proposal
	 * may add an import instead of inserting the fully qualified name.
	 * <p>
	 * This assumes that modifications happen only at the beginning of the
	 * replacement string and do not touch the type arguments list.
	 * </p>
	 *
	 * @param offsets the offsets to modify
	 * @param buffer the original replacement string
	 */
	private void adaptOffsets(int[] offsets, StringBuffer buffer) {
		String replacementString= getReplacementString();
		int delta= buffer.length() - replacementString.length(); // due to using an import instead of package
		for (int i= 0; i < offsets.length; i++) {
			offsets[i]-= delta;
		}
	}

	/**
	 * Computes the type argument proposals for this type proposals. If there is
	 * an expected type binding that is a super type of the proposed type, the
	 * wildcard type arguments of the proposed type that can be mapped through
	 * to type the arguments of the expected type binding are bound accordingly.
	 * <p>
	 * For type arguments that cannot be mapped to arguments in the expected
	 * type, or if there is no expected type, the upper bound of the type
	 * argument is proposed.
	 * </p>
	 * <p>
	 * The argument proposals have their <code>isAmbiguos</code> flag set to
	 * <code>false</code> if the argument can be mapped to a non-wildcard type
	 * argument in the expected type, otherwise the proposal is ambiguous.
	 * </p>
	 *
	 * @return the type argument proposals for the proposed type
	 * @throws JavaScriptModelException if accessing the java model fails
	 */
	private TypeArgumentProposal[] computeTypeArgumentProposals() throws JavaScriptModelException {
		if (fTypeArgumentProposals == null) {
			
			IType type= (IType) getJavaElement();
			if (type == null)
				return new TypeArgumentProposal[0];
			
			
			return new TypeArgumentProposal[0];
		}
		return fTypeArgumentProposals;
	}

	/**
	 * Returns <code>true</code> if type arguments should be appended when
	 * applying this proposal, <code>false</code> if not (for example if the
	 * document already contains a type argument list after the insertion point.
	 *
	 * @param document the document
	 * @param offset the insertion offset
	 * @param trigger the trigger character
	 * @return <code>true</code> if arguments should be appended
	 */
	private boolean shouldAppendArguments(IDocument document, int offset, char trigger) {
		/*
		 * No argument list if there were any special triggers (for example a period to qualify an
		 * inner type).
		 */
		if (trigger != '\0' && trigger != '<' && trigger != '(')
			return false;
		
		/* No argument list if the completion is empty (already within the argument list). */
		char[] completion= fProposal.getCompletion();
		if (completion.length == 0)
			return false;

		/* No argument list if there already is a generic signature behind the name. */
		try {
			IRegion region= document.getLineInformationOfOffset(offset);
			String line= document.get(region.getOffset(), region.getLength());

			int index= offset - region.getOffset();
			while (index != line.length() && Character.isUnicodeIdentifierPart(line.charAt(index)))
				++index;

			if (index == line.length())
				return true;

			char ch= line.charAt(index);
			return ch != '<';

		} catch (BadLocationException e) {
			return true;
		}
	}

	private StringBuffer createParameterList(TypeArgumentProposal[] typeArguments, int[] offsets, int[] lengths) {
		StringBuffer buffer= new StringBuffer();
		buffer.append(getReplacementString());

		FormatterPrefs prefs= getFormatterPrefs();
		final char LESS= '<';
		final char GREATER= '>';
		if (prefs.beforeOpeningBracket)
			buffer.append(SPACE);
		buffer.append(LESS);
		if (prefs.afterOpeningBracket)
			buffer.append(SPACE);
		StringBuffer separator= new StringBuffer(3);
		if (prefs.beforeTypeArgumentComma)
			separator.append(SPACE);
		separator.append(COMMA);
		if (prefs.afterTypeArgumentComma)
			separator.append(SPACE);

		for (int i= 0; i != typeArguments.length; i++) {
			if (i != 0)
				buffer.append(separator);

			offsets[i]= buffer.length();
			buffer.append(typeArguments[i]);
			lengths[i]= buffer.length() - offsets[i];
		}
		if (prefs.beforeClosingBracket)
			buffer.append(SPACE);
		buffer.append(GREATER);

		return buffer;
	}

	private void installLinkedMode(IDocument document, int[] offsets, int[] lengths, TypeArgumentProposal[] typeArgumentProposals, boolean withParentheses) {
		int replacementOffset= getReplacementOffset();
		String replacementString= getReplacementString();

		try {
			LinkedModeModel model= new LinkedModeModel();
			for (int i= 0; i != offsets.length; i++) {
				if (typeArgumentProposals[i].isAmbiguous()) {
					LinkedPositionGroup group= new LinkedPositionGroup();
					group.addPosition(new LinkedPosition(document, replacementOffset + offsets[i], lengths[i]));
					model.addGroup(group);
				}
			}
			if (withParentheses) {
				LinkedPositionGroup group= new LinkedPositionGroup();
				group.addPosition(new LinkedPosition(document, replacementOffset + getCursorPosition(), 0));
				model.addGroup(group);
			}

			model.forceInstall();
			JavaEditor editor= getJavaEditor();
			if (editor != null) {
				model.addLinkingListener(new EditorHighlightingSynchronizer(editor));
			}

			LinkedModeUI ui= new EditorLinkedModeUI(model, getTextViewer());
			ui.setExitPolicy(new ExitPolicy(withParentheses ? ')' : '>', document));
			ui.setExitPosition(getTextViewer(), replacementOffset + replacementString.length(), 0, Integer.MAX_VALUE);
			ui.setDoContextInfo(true);
			ui.enter();

			fSelectedRegion= ui.getSelectedRegion();

		} catch (BadLocationException e) {
			JavaScriptPlugin.log(e);
			openErrorDialog(e);
		}
	}

	private boolean hasAmbiguousProposals(TypeArgumentProposal[] typeArgumentProposals) {
		boolean hasAmbiguousProposals= false;
		for (int i= 0; i < typeArgumentProposals.length; i++) {
			if (typeArgumentProposals[i].isAmbiguous()) {
				hasAmbiguousProposals= true;
				break;
			}
		}
		return hasAmbiguousProposals;
	}

	/**
	 * Returns the currently active java editor, or <code>null</code> if it
	 * cannot be determined.
	 *
	 * @return  the currently active java editor, or <code>null</code>
	 */
	private JavaEditor getJavaEditor() {
		IEditorPart part= JavaScriptPlugin.getActivePage().getActiveEditor();
		if (part instanceof JavaEditor)
			return (JavaEditor) part;
		else
			return null;
	}

	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		if (fSelectedRegion == null)
			return super.getSelection(document);

		return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
	}

	private void openErrorDialog(BadLocationException e) {
		Shell shell= getTextViewer().getTextWidget().getShell();
		MessageDialog.openError(shell, JavaTextMessages.FilledArgumentNamesMethodProposal_error_msg, e.getMessage());
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.LazyJavaCompletionProposal#computeContextInformation()
	 */
	protected IContextInformation computeContextInformation() {
		try {
			if (hasParameters()) {
				TypeArgumentProposal[] proposals= computeTypeArgumentProposals();
				if (hasAmbiguousProposals(proposals))
					return new ContextInformation(this);
			}
		} catch (JavaScriptModelException e) {
		}
		return super.computeContextInformation();
	}
	
	protected int computeCursorPosition() {
		if (fSelectedRegion != null)
			return fSelectedRegion.getOffset() - getReplacementOffset();
		return super.computeCursorPosition();
	}
	
	private boolean hasParameters() {
		IType type= (IType) getJavaElement();
		if (type == null)
			return false;

		return false;
	}
}
