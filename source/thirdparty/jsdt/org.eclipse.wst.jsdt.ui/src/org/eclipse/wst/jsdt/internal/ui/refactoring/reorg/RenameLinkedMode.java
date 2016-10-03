/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.refactoring.reorg;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IEditingSupport;
import org.eclipse.jface.text.IEditingSupportRegistry;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.JavaScriptConventions;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.refactoring.IJavaScriptRefactorings;
import org.eclipse.wst.jsdt.core.refactoring.descriptors.RenameJavaScriptElementDescriptor;
import org.eclipse.wst.jsdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.wst.jsdt.internal.corext.dom.NodeFinder;
import org.eclipse.wst.jsdt.internal.corext.refactoring.rename.RenamingNameSuggestor;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.EditorHighlightingSynchronizer;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.wst.jsdt.internal.ui.refactoring.DelegateUIHelper;
import org.eclipse.wst.jsdt.internal.ui.text.correction.LinkedNamesAssistProposal.DeleteBlockingExitPolicy;
import org.eclipse.wst.jsdt.ui.refactoring.RenameSupport;

public class RenameLinkedMode {

	private class FocusEditingSupport implements IEditingSupport {
		public boolean ownsFocusShell() {
			if (fInfoPopup == null)
				return false;
			if (fInfoPopup.ownsFocusShell()) {
				return true;
			}
			
			Shell editorShell= fEditor.getSite().getShell();
			Shell activeShell= editorShell.getDisplay().getActiveShell();
			if (editorShell == activeShell)
				return true;
			return false;
		}

		public boolean isOriginator(DocumentEvent event, IRegion subjectRegion) {
			return false; //leave on external modification outside positions
		}
	}
	
	private class EditorSynchronizer implements ILinkedModeListener {
		public void left(LinkedModeModel model, int flags) {
			linkedModeLeft();
			if ( (flags & ILinkedModeListener.UPDATE_CARET) != 0) {
				doRename(fShowPreview);
			}
		}

		public void resume(LinkedModeModel model, int flags) {
		}

		public void suspend(LinkedModeModel model) {
		}
	}
	
	private class ExitPolicy extends DeleteBlockingExitPolicy {
		public ExitPolicy(IDocument document) {
			super(document);
		}

		public ExitFlags doExit(LinkedModeModel model, VerifyEvent event, int offset, int length) {
			fShowPreview|= (event.stateMask & SWT.CTRL) != 0;
			return super.doExit(model, event, offset, length);
		}
	}
	
	
	private static RenameLinkedMode fgActiveLinkedMode;
	
	private final CompilationUnitEditor fEditor;
	private final IJavaScriptElement fJavaElement;

	private RenameInformationPopup fInfoPopup;
	
	private boolean fOriginalSaved;
	private Point fOriginalSelection;
	private String fOriginalName;

	private LinkedPosition fNamePosition;
	private LinkedModeModel fLinkedModeModel;
	private LinkedPositionGroup fLinkedPositionGroup;
	private final FocusEditingSupport fFocusEditingSupport;
	private boolean fShowPreview;


	public RenameLinkedMode(IJavaScriptElement element, CompilationUnitEditor editor) {
		Assert.isNotNull(element);
		Assert.isNotNull(editor);
		fEditor= editor;
		fJavaElement= element;
		fFocusEditingSupport= new FocusEditingSupport();
	}
	
	public static RenameLinkedMode getActiveLinkedMode() {
		if (fgActiveLinkedMode != null) {
			ISourceViewer viewer= fgActiveLinkedMode.fEditor.getViewer();
			if (viewer != null) {
				StyledText textWidget= viewer.getTextWidget();
				if (textWidget != null && ! textWidget.isDisposed()) {
					return fgActiveLinkedMode;
				}
			}
			// make sure we don't hold onto the active linked mode if anything went wrong with canceling:
			fgActiveLinkedMode= null;
		}
		return null;
	}
	
	public void start() {
		if (getActiveLinkedMode() != null) {
			// for safety; should already be handled in RenameJavaElementAction
			fgActiveLinkedMode.startFullDialog();
			return;
		}
		
		fOriginalSaved= ! fEditor.isDirty();
		
		ISourceViewer viewer= fEditor.getViewer();
		IDocument document= viewer.getDocument();
		fOriginalSelection= viewer.getSelectedRange();
		int offset= fOriginalSelection.x;
		
		try {
			JavaScriptUnit root= JavaScriptPlugin.getDefault().getASTProvider().getAST(getCompilationUnit(), ASTProvider.WAIT_YES, null);
			
			fLinkedPositionGroup= new LinkedPositionGroup();
			ASTNode selectedNode= NodeFinder.perform(root, fOriginalSelection.x, fOriginalSelection.y);
			if (! (selectedNode instanceof SimpleName)) {
				return; // TODO: show dialog
			}
			SimpleName nameNode= (SimpleName) selectedNode;
			
			fOriginalName= nameNode.getIdentifier();
			final int pos= nameNode.getStartPosition();
			ASTNode[] sameNodes= LinkedNodeFinder.findByNode(root, nameNode);
			
			//TODO: copied from LinkedNamesAssistProposal#apply(..):
			// sort for iteration order, starting with the node @ offset
			Arrays.sort(sameNodes, new Comparator() {
				public int compare(Object o1, Object o2) {
					return rank((ASTNode) o1) - rank((ASTNode) o2);
				}
				/**
				 * Returns the absolute rank of an <code>ASTNode</code>. Nodes
				 * preceding <code>pos</code> are ranked last.
				 *
				 * @param node the node to compute the rank for
				 * @return the rank of the node with respect to the invocation offset
				 */
				private int rank(ASTNode node) {
					int relativeRank= node.getStartPosition() + node.getLength() - pos;
					if (relativeRank < 0)
						return Integer.MAX_VALUE + relativeRank;
					else
						return relativeRank;
				}
			});
			for (int i= 0; i < sameNodes.length; i++) {
				ASTNode elem= sameNodes[i];
				LinkedPosition linkedPosition= new LinkedPosition(document, elem.getStartPosition(), elem.getLength(), i);
				if (i == 0)
					fNamePosition= linkedPosition;
				fLinkedPositionGroup.addPosition(linkedPosition);
			}
				
			fLinkedModeModel= new LinkedModeModel();
			fLinkedModeModel.addGroup(fLinkedPositionGroup);
			fLinkedModeModel.forceInstall();
			fLinkedModeModel.addLinkingListener(new EditorHighlightingSynchronizer(fEditor));
			fLinkedModeModel.addLinkingListener(new EditorSynchronizer());
            
			LinkedModeUI ui= new EditorLinkedModeUI(fLinkedModeModel, viewer);
			ui.setExitPosition(viewer, offset, 0, Integer.MAX_VALUE);
			ui.setExitPolicy(new ExitPolicy(document));
			ui.enter();
			
			viewer.setSelectedRange(fOriginalSelection.x, fOriginalSelection.y); // by default, full word is selected; restore original selection
			
			if (viewer instanceof IEditingSupportRegistry) {
				IEditingSupportRegistry registry= (IEditingSupportRegistry) viewer;
				registry.register(fFocusEditingSupport);
			}
			
			openSecondaryPopup();
//			startAnimation();
			fgActiveLinkedMode= this;
			
		} catch (BadLocationException e) {
			JavaScriptPlugin.log(e);
		}
	}
	
//	private void startAnimation() {
//		//TODO:
//		// - switch off if animations disabled 
//		// - show rectangle around target for 500ms after animation
//		Shell shell= fEditor.getSite().getShell();
//		StyledText textWidget= fEditor.getViewer().getTextWidget();
//		
//		// from popup:
//		Rectangle startRect= fPopup.getBounds();
//		
//		// from editor:
////		Point startLoc= textWidget.getParent().toDisplay(textWidget.getLocation());
////		Point startSize= textWidget.getSize();
////		Rectangle startRect= new Rectangle(startLoc.x, startLoc.y, startSize.x, startSize.y);
//		
//		// from hell:
////		Rectangle startRect= shell.getClientArea();
//		
//		Point caretLocation= textWidget.getLocationAtOffset(textWidget.getCaretOffset());
//		Point displayLocation= textWidget.toDisplay(caretLocation);
//		Rectangle targetRect= new Rectangle(displayLocation.x, displayLocation.y, 0, 0);
//		
//		RectangleAnimation anim= new RectangleAnimation(shell, startRect, targetRect);
//		anim.schedule();
//	}

	/**
	 * @param offset
	 * @param length
	 */
	public void start(int offset, int length) {
		ISourceViewer viewer = fEditor.getViewer();
		IDocument document = viewer.getDocument();
		int cursorPosition = viewer.getSelectedRange().x;
		viewer.setSelectedRange(offset, length);

		try {
			JavaScriptUnit root = JavaScriptPlugin.getDefault().getASTProvider().getAST(getCompilationUnit(), ASTProvider.WAIT_YES, null);

			fLinkedPositionGroup = new LinkedPositionGroup();
			ASTNode selectedNode = NodeFinder.perform(root, offset, length);
			if (! (selectedNode instanceof SimpleName))
				return;
			SimpleName nameNode = (SimpleName) selectedNode;
			LinkedPosition linkedPosition = new LinkedPosition(document, nameNode.getStartPosition(), nameNode.getLength());
			fLinkedPositionGroup.addPosition(linkedPosition);

			fLinkedModeModel = new LinkedModeModel();
			fLinkedModeModel.addGroup(fLinkedPositionGroup);
			fLinkedModeModel.forceInstall();
			fLinkedModeModel.addLinkingListener(new EditorHighlightingSynchronizer(fEditor));

			LinkedModeUI ui = new EditorLinkedModeUI(fLinkedModeModel, viewer);
			ui.setExitPosition(viewer, cursorPosition, 0, Integer.MAX_VALUE);
			ui.setExitPolicy(new ExitPolicy(document));
			ui.enter();

		} catch (BadLocationException e) {
			JavaScriptPlugin.log(e);
		}
	}

	void doRename(boolean showPreview) {
		cancel();
		
		Image image= null;
		Label label= null;
		
		fShowPreview|= showPreview;
		try {
			ISourceViewer viewer= fEditor.getViewer();
			if (viewer instanceof SourceViewer) {
				SourceViewer sourceViewer= (SourceViewer) viewer;
				Control viewerControl= sourceViewer.getControl();
				if (viewerControl instanceof Composite) {
					Composite composite= (Composite) viewerControl;
					Display display= composite.getDisplay();
					
					// Flush pending redraw requests:
					while (! display.isDisposed() && display.readAndDispatch()) {
					}
					
					// Copy editor area:
					GC gc= new GC(composite);
					Point size;
					try {
						size= composite.getSize();
						image= new Image(gc.getDevice(), size.x, size.y);
						gc.copyArea(image, 0, 0);
					} finally {
						gc.dispose();
						gc= null;
					}
					
					// Persist editor area while executing refactoring:
					label= new Label(composite, SWT.NONE);
					label.setImage(image);
					label.setBounds(0, 0, size.x, size.y);
					label.moveAbove(null);
				}
			}
			
			String newName= fNamePosition.getContent();
			if (fOriginalName.equals(newName))
				return;
			RenameSupport renameSupport= undoAndCreateRenameSupport(newName);
			if (renameSupport == null)
				return;
			
			Shell shell= fEditor.getSite().getShell();
			boolean executed;
			if (fShowPreview) { // could have been updated by undoAndCreateRenameSupport(..)
				executed= renameSupport.openDialog(shell, true);
			} else {
				renameSupport.perform(shell, fEditor.getSite().getWorkbenchWindow());
				executed= true;
			}
			if (executed) {
				restoreFullSelection();
			}
			JavaModelUtil.reconcile(getCompilationUnit());
		} catch (CoreException ex) {
			JavaScriptPlugin.log(ex);
		} catch (InterruptedException ex) {
			// canceling is OK -> redo text changes in that case?
		} catch (InvocationTargetException ex) {
			JavaScriptPlugin.log(ex);
		} catch (BadLocationException e) {
			JavaScriptPlugin.log(e);
		} finally {
			if (label != null)
				label.dispose();
			if (image != null)
				image.dispose();
		}
	}

	public void cancel() {
		if (fLinkedModeModel != null) {
			fLinkedModeModel.exit(ILinkedModeListener.NONE);
		}
		linkedModeLeft();
	}
	
	private void restoreFullSelection() {
		if (fOriginalSelection.y != 0) {
			int originalOffset= fOriginalSelection.x;
			LinkedPosition[] positions= fLinkedPositionGroup.getPositions();
			for (int i= 0; i < positions.length; i++) {
				LinkedPosition position= positions[i];
				if (! position.isDeleted() && position.includes(originalOffset)) {
					fEditor.getViewer().setSelectedRange(position.offset, position.length);
					return;
				}
			}
		}
	}
	
	private RenameSupport undoAndCreateRenameSupport(String newName) throws CoreException {
		// Assumption: the linked mode model should be shut down by now.
		
		ISourceViewer viewer= fEditor.getViewer();
		final IDocument document= viewer.getDocument();
		
		try {
			if (! fOriginalName.equals(newName)) {
				fEditor.getSite().getWorkbenchWindow().run(false, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						LinkedPosition[] positions= fLinkedPositionGroup.getPositions();
						Arrays.sort(positions, new Comparator() {
							public int compare(Object o1, Object o2) {
								return ((LinkedPosition) o1).offset - ((LinkedPosition) o2).offset;
							}
						});
						int correction= 0;
						int originalLength= fOriginalName.length();
						for (int i= 0; i < positions.length; i++) {
							LinkedPosition position= positions[i];
							try {
								int length= position.getLength();
								document.replace(position.getOffset() + correction, length, fOriginalName);
								correction= correction - length + originalLength;
							} catch (BadLocationException e) {
								throw new InvocationTargetException(e);
							}
						}
						if (fOriginalSaved) {
							fEditor.doSave(monitor); // started saved -> end saved
						}
					}
				});
			}
		} catch (InvocationTargetException e) {
			throw new CoreException(new Status(IStatus.ERROR, JavaScriptPlugin.getPluginId(), ReorgMessages.RenameLinkedMode_error_saving_editor, e));
		} catch (InterruptedException e) {
			// cancelling is OK
			return null;
		} finally {
			JavaModelUtil.reconcile(getCompilationUnit());
		}
		
		viewer.setSelectedRange(fOriginalSelection.x, fOriginalSelection.y);
		
		RenameJavaScriptElementDescriptor descriptor= createRenameDescriptor(fJavaElement, newName);
		RenameSupport renameSupport= RenameSupport.create(descriptor);
		return renameSupport;
	}

	private IJavaScriptUnit getCompilationUnit() {
		return (IJavaScriptUnit) EditorUtility.getEditorInputJavaElement(fEditor, false);
	}
	
	public void startFullDialog() {
		cancel();
		
		try {
			String newName= fNamePosition.getContent();
			RenameSupport renameSupport= undoAndCreateRenameSupport(newName);
			if (renameSupport != null)
				renameSupport.openDialog(fEditor.getSite().getShell());
		} catch (CoreException e) {
			JavaScriptPlugin.log(e);
		} catch (BadLocationException e) {
			JavaScriptPlugin.log(e);
		}
	}
	
	/**
	 * @param javaElement
	 * @param newName
	 * @return a rename descriptor with current settings as used in the refactoring dialogs 
	 * @throws JavaScriptModelException
	 */
	private RenameJavaScriptElementDescriptor createRenameDescriptor(IJavaScriptElement javaElement, String newName) throws JavaScriptModelException {
		String contributionId;
		// see RefactoringExecutionStarter#createRenameSupport(..):
		int elementType= javaElement.getElementType();
		switch (elementType) {
			case IJavaScriptElement.JAVASCRIPT_PROJECT:
				contributionId= IJavaScriptRefactorings.RENAME_JAVA_PROJECT;
				break;
			case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT:
				contributionId= IJavaScriptRefactorings.RENAME_SOURCE_FOLDER;
				break;
			case IJavaScriptElement.PACKAGE_FRAGMENT:
				contributionId= IJavaScriptRefactorings.RENAME_PACKAGE;
				break;
			case IJavaScriptElement.JAVASCRIPT_UNIT:
				contributionId= IJavaScriptRefactorings.RENAME_JAVASCRIPT_UNIT;
				break;
			case IJavaScriptElement.TYPE:
				contributionId= IJavaScriptRefactorings.RENAME_TYPE;
				break;
			case IJavaScriptElement.METHOD:
				final IFunction method= (IFunction) javaElement;
				if (method.isConstructor())
					return createRenameDescriptor(method.getDeclaringType(), newName);
				else
					contributionId= IJavaScriptRefactorings.RENAME_METHOD;
				break;
			case IJavaScriptElement.FIELD:
				contributionId= IJavaScriptRefactorings.RENAME_FIELD;
				break;
			case IJavaScriptElement.LOCAL_VARIABLE:
				contributionId= IJavaScriptRefactorings.RENAME_LOCAL_VARIABLE;
				break;
			default:
				return null;
		}
		
		RenameJavaScriptElementDescriptor descriptor= (RenameJavaScriptElementDescriptor) RefactoringCore.getRefactoringContribution(contributionId).createDescriptor();
		descriptor.setJavaElement(javaElement);
		descriptor.setNewName(newName);
		if (elementType != IJavaScriptElement.PACKAGE_FRAGMENT_ROOT)
			descriptor.setUpdateReferences(true);
		
		IDialogSettings javaSettings= JavaScriptPlugin.getDefault().getDialogSettings();
		IDialogSettings refactoringSettings= javaSettings.getSection(RefactoringWizardPage.REFACTORING_SETTINGS); //TODO: undocumented API
		if (refactoringSettings == null) {
			refactoringSettings= javaSettings.addNewSection(RefactoringWizardPage.REFACTORING_SETTINGS); 
		}
		
		switch (elementType) {
			case IJavaScriptElement.METHOD:
			case IJavaScriptElement.FIELD:
				descriptor.setDeprecateDelegate(refactoringSettings.getBoolean(DelegateUIHelper.DELEGATE_DEPRECATION));
				descriptor.setKeepOriginal(refactoringSettings.getBoolean(DelegateUIHelper.DELEGATE_UPDATING));
		}
		switch (elementType) {
			case IJavaScriptElement.TYPE:
//			case IJavaScriptElement.JAVASCRIPT_UNIT: // TODO
				descriptor.setUpdateSimilarDeclarations(refactoringSettings.getBoolean(RenameRefactoringWizard.TYPE_UPDATE_SIMILAR_ELEMENTS));
				int strategy;
				try {
					strategy= refactoringSettings.getInt(RenameRefactoringWizard.TYPE_SIMILAR_MATCH_STRATEGY);
				} catch (NumberFormatException e) {
					strategy= RenamingNameSuggestor.STRATEGY_EXACT;
				}
				descriptor.setMatchStrategy(strategy);
		}
		switch (elementType) {
			case IJavaScriptElement.PACKAGE_FRAGMENT:
				descriptor.setUpdateHierarchy(refactoringSettings.getBoolean(RenameRefactoringWizard.PACKAGE_RENAME_SUBPACKAGES));
		}
		switch (elementType) {
			case IJavaScriptElement.PACKAGE_FRAGMENT:
			case IJavaScriptElement.TYPE:
				String fileNamePatterns= refactoringSettings.get(RenameRefactoringWizard.QUALIFIED_NAMES_PATTERNS);
				if (fileNamePatterns != null && fileNamePatterns.length() != 0) {
					descriptor.setFileNamePatterns(fileNamePatterns);
					boolean updateQualifiedNames= refactoringSettings.getBoolean(RenameRefactoringWizard.UPDATE_QUALIFIED_NAMES);
					descriptor.setUpdateQualifiedNames(updateQualifiedNames);
					fShowPreview|= updateQualifiedNames;
				}
		}
		switch (elementType) {
			case IJavaScriptElement.PACKAGE_FRAGMENT:
			case IJavaScriptElement.TYPE:
			case IJavaScriptElement.FIELD:
				boolean updateTextualOccurrences= refactoringSettings.getBoolean(RenameRefactoringWizard.UPDATE_TEXTUAL_MATCHES);
				descriptor.setUpdateTextualOccurrences(updateTextualOccurrences);
				fShowPreview|= updateTextualOccurrences;
		}
		switch (elementType) {
			case IJavaScriptElement.FIELD:
				descriptor.setRenameGetters(refactoringSettings.getBoolean(RenameRefactoringWizard.FIELD_RENAME_GETTER));
				descriptor.setRenameSetters(refactoringSettings.getBoolean(RenameRefactoringWizard.FIELD_RENAME_SETTER));
		}
		return descriptor;
	}

	private void linkedModeLeft() {
		fgActiveLinkedMode= null;
		if (fInfoPopup != null) {
			fInfoPopup.close();
		}
		
		ISourceViewer viewer= fEditor.getViewer();
		if (viewer instanceof IEditingSupportRegistry) {
			IEditingSupportRegistry registry= (IEditingSupportRegistry) viewer;
			registry.unregister(fFocusEditingSupport);
		}
	}

	private void openSecondaryPopup() {
		fInfoPopup= new RenameInformationPopup(fEditor, this);
		fInfoPopup.open();
	}

	public boolean isCaretInLinkedPosition() {
		return getCurrentLinkedPosition() != null;
	}

	public LinkedPosition getCurrentLinkedPosition() {
		Point selection= fEditor.getViewer().getSelectedRange();
		int start= selection.x;
		int end= start + selection.y;
		LinkedPosition[] positions= fLinkedPositionGroup.getPositions();
		for (int i= 0; i < positions.length; i++) {
			LinkedPosition position= positions[i];
			if (position.includes(start) && position.includes(end))
				return position;
		}
		return null;
	}

	public boolean isEnabled() {
		try {
			String newName= fNamePosition.getContent();
			if (fOriginalName.equals(newName))
				return false;
			/* 
			 * TODO: use JavaRenameProcessor#checkNewElementName(String)
			 * but make sure implementations don't access outdated Java Model
			 * (cache all necessary information before starting linked mode).
			 */
			IJavaScriptProject project= fJavaElement.getJavaScriptProject();
			String sourceLevel= project.getOption(JavaScriptCore.COMPILER_SOURCE, true);
			String complianceLevel= project.getOption(JavaScriptCore.COMPILER_COMPLIANCE, true);
			return JavaScriptConventions.validateIdentifier(newName, sourceLevel, complianceLevel).isOK();
		} catch (BadLocationException e) {
			return false;
		}
		
	}

	public boolean isOriginalName() {
		try {
			String newName= fNamePosition.getContent();
			return fOriginalName.equals(newName);
		} catch (BadLocationException e) {
			return false;
		}
	}

}
