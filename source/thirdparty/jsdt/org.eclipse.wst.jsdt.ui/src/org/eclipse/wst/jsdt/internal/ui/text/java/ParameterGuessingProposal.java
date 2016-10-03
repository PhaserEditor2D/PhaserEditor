/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *		Andrew McCullough - initial API and implementation
 *		IBM Corporation  - general improvement and bug fixes, partial reimplementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.text.java;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.InclusivePositionUpdater;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.eclipse.wst.jsdt.core.CompletionProposal;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.EditorHighlightingSynchronizer;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.wst.jsdt.ui.text.java.JavaContentAssistInvocationContext;

/**
 * This is a {@link org.eclipse.wst.jsdt.internal.ui.text.java.JavaCompletionProposal} which includes templates
 * that represent the best guess completion for each parameter of a method.
 */
public final class ParameterGuessingProposal extends JavaMethodCompletionProposal {

	/** Tells whether this class is in debug mode. */
	private static final boolean DEBUG= "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.wst.jsdt.ui/debug/ResultCollector"));  //$NON-NLS-1$//$NON-NLS-2$
	
	private ICompletionProposal[][] fChoices; // initialized by guessParameters()
	private Position[] fPositions; // initialized by guessParameters()
	
	private IRegion fSelectedRegion; // initialized by apply()
	private IPositionUpdater fUpdater;

 	public ParameterGuessingProposal(CompletionProposal proposal, JavaContentAssistInvocationContext context) {
 		super(proposal, context);
 	}

	/*
	 * @see ICompletionProposalExtension#apply(IDocument, char)
	 */
	public void apply(IDocument document, char trigger, int offset) {
		try {
			super.apply(document, trigger, offset);
			
			int baseOffset= getReplacementOffset();
			String replacement= getReplacementString();
			
			if (fPositions != null && getTextViewer() != null) {
				
				LinkedModeModel model= new LinkedModeModel();
				
				for (int i= 0; i < fPositions.length; i++) {
					LinkedPositionGroup group= new LinkedPositionGroup();
					int positionOffset= fPositions[i].getOffset();
					int positionLength= fPositions[i].getLength();
				
					if (fChoices[i].length < 2) {
						group.addPosition(new LinkedPosition(document, positionOffset, positionLength, LinkedPositionGroup.NO_STOP));
					} else {
						ensurePositionCategoryInstalled(document, model);
						document.addPosition(getCategory(), fPositions[i]);
						group.addPosition(new ProposalPosition(document, positionOffset, positionLength, LinkedPositionGroup.NO_STOP, fChoices[i]));
					}
					model.addGroup(group);
				}

				model.forceInstall();
				JavaEditor editor= getJavaEditor();
				if (editor != null) {
					model.addLinkingListener(new EditorHighlightingSynchronizer(editor));
				}

				LinkedModeUI ui= new EditorLinkedModeUI(model, getTextViewer());
				ui.setExitPosition(getTextViewer(), baseOffset + replacement.length(), 0, Integer.MAX_VALUE);
				ui.setExitPolicy(new ExitPolicy(')', document));
				ui.setCyclingMode(LinkedModeUI.CYCLE_WHEN_NO_PARENT);
				ui.setDoContextInfo(true);
				ui.enter();
				fSelectedRegion= ui.getSelectedRegion();

			} else {
				fSelectedRegion= new Region(baseOffset + replacement.length(), 0);
			}

		} catch (BadLocationException e) {
			ensurePositionCategoryRemoved(document);
			JavaScriptPlugin.log(e);
			openErrorDialog(e);
		} catch (BadPositionCategoryException e) {
			ensurePositionCategoryRemoved(document);
			JavaScriptPlugin.log(e);
			openErrorDialog(e);
		}
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.JavaMethodCompletionProposal#needsLinkedMode()
	 */
	protected boolean needsLinkedMode() {
		return false; // we handle it ourselves
	}
	
	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.java.JavaMethodCompletionProposal#computeReplacementString()
	 */
	protected String computeReplacementString() {
		
		if (!hasParameters() || !hasArgumentList())
			return super.computeReplacementString();
		
		long millis= DEBUG ? System.currentTimeMillis() : 0;
		String replacement;
		try {
			replacement= computeGuessingCompletion();
		} catch (JavaScriptModelException x) {
			fPositions= null;
			fChoices= null;
			JavaScriptPlugin.log(x);
			openErrorDialog(x);
			return super.computeReplacementString();
		}
		if (DEBUG) System.err.println("Parameter Guessing: " + (System.currentTimeMillis() - millis)); //$NON-NLS-1$ 
		
		return replacement;
	}

	/**
	 * Creates the completion string. Offsets and Lengths are set to the offsets and lengths
	 * of the parameters.
	 */
	private String computeGuessingCompletion() throws JavaScriptModelException {
		
		StringBuffer buffer= new StringBuffer(String.valueOf(fProposal.getName()));
		
		FormatterPrefs prefs= getFormatterPrefs();
		if (prefs.beforeOpeningParen)
			buffer.append(SPACE);
		buffer.append(LPAREN);
		
		setCursorPosition(buffer.length());
		
		if (prefs.afterOpeningParen)
			buffer.append(SPACE);
		
		fChoices= guessParameters();
		int count= fChoices.length;
		int replacementOffset= getReplacementOffset();
		
		for (int i= 0; i < count; i++) {
			if (i != 0) {
				if (prefs.beforeComma)
					buffer.append(SPACE);
				buffer.append(COMMA);
				if (prefs.afterComma)
					buffer.append(SPACE);
			}

			ICompletionProposal proposal= fChoices[i][0];
			String argument= proposal.getDisplayString();
			Position position= fPositions[i];
			position.setOffset(replacementOffset + buffer.length());
			position.setLength(argument.length());
			if (proposal instanceof JavaCompletionProposal) // handle the "unknown" case where we only insert a proposal.
				((JavaCompletionProposal) proposal).setReplacementOffset(replacementOffset + buffer.length());
			buffer.append(argument);
		}
		
		if (prefs.beforeClosingParen)
			buffer.append(SPACE);

		buffer.append(RPAREN);

		return buffer.toString();
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

	private ICompletionProposal[][] guessParameters() throws JavaScriptModelException {
		// find matches in reverse order.  Do this because people tend to declare the variable meant for the last
		// parameter last.  That is, local variables for the last parameter in the method completion are more
		// likely to be closer to the point of code completion. As an example consider a "delegation" completion:
		//
		// 		public void myMethod(int param1, int param2, int param3) {
		// 			someOtherObject.yourMethod(param1, param2, param3);
		//		}
		//
		// The other consideration is giving preference to variables that have not previously been used in this
		// code completion (which avoids "someOtherObject.yourMethod(param1, param1, param1)";
		
		char[][] parameterNames= fProposal.findParameterNames(null);
		int count= parameterNames.length;
		fPositions= new Position[count];
		fChoices= new ICompletionProposal[count][];

		IDocument document= fInvocationContext.getDocument();
		IJavaScriptUnit cu= fInvocationContext.getCompilationUnit();
		JavaModelUtil.reconcile(cu);
		String[][] parameterTypes= getParameterSignatures();
		ParameterGuesser guesser= new ParameterGuesser(fProposal.getCompletionLocation() + 1, cu);
		
		for (int i= count - 1; i >= 0; i--) {
			String paramName= new String(parameterNames[i]);
			Position position= new Position(0,0);

			ICompletionProposal[] argumentProposals= guesser.parameterProposals(
						parameterTypes.length > i ? parameterTypes[i][0] : null, 
						parameterTypes.length > i ? parameterTypes[i][1] : null, 
						paramName, position, document);
			if (argumentProposals.length == 0)
				argumentProposals= new ICompletionProposal[] {new JavaCompletionProposal(paramName, 0, paramName.length(), null, paramName, 0)};
			
			fPositions[i]= position;
			fChoices[i]= argumentProposals;
		}
		
		return fChoices;
	}

	private String[][] getParameterSignatures() {
		char[] signature= fProposal.getSignature();
		char[][] types= Signature.getParameterTypes(signature);
		String[][] ret= new String[types.length][2];

		for (int i= 0; i < types.length; i++) {
			char[] type= types[i];
			ret[i][0]= String.valueOf(Signature.getSignatureQualifier(type));
			ret[i][1]= String.valueOf(Signature.getSignatureSimpleName(type));
		}
		return ret;
	}

	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		if (fSelectedRegion == null)
			return new Point(getReplacementOffset(), 0);

		return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
	}

	private void openErrorDialog(Exception e) {
		Shell shell= getTextViewer().getTextWidget().getShell();
		MessageDialog.openError(shell, JavaTextMessages.ParameterGuessingProposal_error_msg, e.getMessage());
	}

	private void ensurePositionCategoryInstalled(final IDocument document, LinkedModeModel model) {
		if (!document.containsPositionCategory(getCategory())) {
			document.addPositionCategory(getCategory());
			fUpdater= new InclusivePositionUpdater(getCategory());
			document.addPositionUpdater(fUpdater);

			model.addLinkingListener(new ILinkedModeListener() {

				/*
				 * @see org.eclipse.jface.text.link.ILinkedModeListener#left(org.eclipse.jface.text.link.LinkedModeModel, int)
				 */
				public void left(LinkedModeModel environment, int flags) {
					ensurePositionCategoryRemoved(document);
				}

				public void suspend(LinkedModeModel environment) {}
				public void resume(LinkedModeModel environment, int flags) {}
			});
		}
	}

	private void ensurePositionCategoryRemoved(IDocument document) {
		if (document.containsPositionCategory(getCategory())) {
			try {
				document.removePositionCategory(getCategory());
			} catch (BadPositionCategoryException e) {
				// ignore
			}
			document.removePositionUpdater(fUpdater);
		}
	}

	private String getCategory() {
		return "ParameterGuessingProposal_" + toString(); //$NON-NLS-1$
	}

}
