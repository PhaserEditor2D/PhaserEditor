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
package org.eclipse.wst.jsdt.internal.ui.refactoring.reorg;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CopyFilesAndFoldersOperation;
import org.eclipse.ui.actions.CopyProjectOperation;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.ToolFactory;
import org.eclipse.wst.jsdt.core.compiler.IScanner;
import org.eclipse.wst.jsdt.core.compiler.ITerminalSymbols;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Modifier;
import org.eclipse.wst.jsdt.core.dom.PackageDeclaration;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.internal.corext.dom.ASTNodes;
import org.eclipse.wst.jsdt.internal.corext.refactoring.Checks;
import org.eclipse.wst.jsdt.internal.corext.refactoring.TypedSource;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.wst.jsdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.IConfirmQuery;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.IReorgQueries;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.JavaElementTransfer;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.ParentChecker;
import org.eclipse.wst.jsdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.wst.jsdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.wst.jsdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.corext.util.JdtFlags;
import org.eclipse.wst.jsdt.internal.corext.util.Messages;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringExecutionHelper;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.wst.jsdt.internal.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.wst.jsdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;
import org.eclipse.wst.jsdt.internal.ui.util.SelectionUtil;
import org.eclipse.wst.jsdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.wst.jsdt.internal.ui.workingsets.OthersWorkingSetUpdater;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction;


public class PasteAction extends SelectionDispatchAction{

	private final Clipboard fClipboard;

	public PasteAction(IWorkbenchSite site, Clipboard clipboard) {
		super(site);
		Assert.isNotNull(clipboard);
		fClipboard= clipboard;
		
		setText(ReorgMessages.PasteAction_4); 
		setDescription(ReorgMessages.PasteAction_5); 

		ISharedImages workbenchImages= JavaScriptPlugin.getDefault().getWorkbench().getSharedImages();
		setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE_DISABLED));
		setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.PASTE_ACTION);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		// Moved condition checking to run (see http://bugs.eclipse.org/bugs/show_bug.cgi?id=78450)
	}

	private Paster[] createEnabledPasters(TransferData[] availableDataTypes) throws JavaScriptModelException {
		Paster paster;
		Shell shell = getShell();
		List result= new ArrayList(2);
		paster= new ProjectPaster(shell, fClipboard);
		if (paster.canEnable(availableDataTypes)) 
			result.add(paster);
		
		paster= new JavaElementAndResourcePaster(shell, fClipboard);
		if (paster.canEnable(availableDataTypes)) 
			result.add(paster);

		paster= new TypedSourcePaster(shell, fClipboard);
		if (paster.canEnable(availableDataTypes)) 
			result.add(paster);

		paster= new FilePaster(shell, fClipboard);
		if (paster.canEnable(availableDataTypes)) 
			result.add(paster);
		
		paster= new WorkingSetPaster(shell, fClipboard);
		if (paster.canEnable(availableDataTypes))
			result.add(paster);
		
		paster= new TextPaster(shell, fClipboard);
		if (paster.canEnable(availableDataTypes))
			result.add(paster);
		return (Paster[]) result.toArray(new Paster[result.size()]);
	}

	private static Object getContents(final Clipboard clipboard, final Transfer transfer, Shell shell) {
		//see bug 33028 for explanation why we need this
		final Object[] result= new Object[1];
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				result[0]= clipboard.getContents(transfer);
			}
		});
		return result[0];
	}
	
	private static boolean isAvailable(Transfer transfer, TransferData[] availableDataTypes) {
		for (int i= 0; i < availableDataTypes.length; i++) {
			if (transfer.isSupportedType(availableDataTypes[i])) return true;
		}
		return false;
	}

	public void run(IStructuredSelection selection) {
		try {
			TransferData[] availableTypes= fClipboard.getAvailableTypes();
			List elements= selection.toList();
			IResource[] resources= ReorgUtils.getResources(elements);
			IJavaScriptElement[] javaElements= ReorgUtils.getJavaElements(elements);
			IWorkingSet[] workingSets= ReorgUtils.getWorkingSets(elements);
			Paster[] pasters= createEnabledPasters(availableTypes);
			for (int i= 0; i < pasters.length; i++) {
				if (pasters[i].canPasteOn(javaElements, resources, workingSets)) {
					pasters[i].paste(javaElements, resources, workingSets, availableTypes);
					return;// one is enough
				}
			}
			String msg= resources.length + javaElements.length + workingSets.length == 0
					? ReorgMessages.PasteAction_cannot_no_selection
					: ReorgMessages.PasteAction_cannot_selection;
			MessageDialog.openError(JavaScriptPlugin.getActiveWorkbenchShell(), ReorgMessages.PasteAction_name, msg); 
		} catch (JavaScriptModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.OpenRefactoringWizardAction_exception); 
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.OpenRefactoringWizardAction_exception); 
		} catch (InterruptedException e) {
			// OK
		}
	}

	private abstract static class Paster{
		private final Shell fShell;
		private final Clipboard fClipboard2;
		protected Paster(Shell shell, Clipboard clipboard){
			fShell= shell;
			fClipboard2= clipboard;
		}
		protected final Shell getShell() {
			return fShell;
		}
		protected final Clipboard getClipboard() {
			return fClipboard2;
		}

		protected final IResource[] getClipboardResources(TransferData[] availableDataTypes) {
			Transfer transfer= ResourceTransfer.getInstance();
			if (isAvailable(transfer, availableDataTypes)) {
				return (IResource[])getContents(fClipboard2, transfer, getShell());
			}
			return null;
		}

		protected final IJavaScriptElement[] getClipboardJavaElements(TransferData[] availableDataTypes) {
			Transfer transfer= JavaElementTransfer.getInstance();
			if (isAvailable(transfer, availableDataTypes)) {
				return (IJavaScriptElement[])getContents(fClipboard2, transfer, getShell());
			}
			return null;
		}
	
		protected final TypedSource[] getClipboardTypedSources(TransferData[] availableDataTypes) {
			Transfer transfer= TypedSourceTransfer.getInstance();
			if (isAvailable(transfer, availableDataTypes)) {
				return (TypedSource[])getContents(fClipboard2, transfer, getShell());
			}
			return null;
		}
	
		protected final String getClipboardText(TransferData[] availableDataTypes) {
			Transfer transfer= TextTransfer.getInstance();
			if (isAvailable(transfer, availableDataTypes)) {
				return (String) getContents(fClipboard2, transfer, getShell());
			}
			return null;
		}

		/**
		 * Used to be called on selection change, but is only called on execution now
		 * (before {@link #canPasteOn(IJavaScriptElement[], IResource[], IWorkingSet[])}).
		 * @param availableTypes transfer types
		 * @return whether the paste action can be enabled
		 * @throws JavaScriptModelException 
		 */
		public abstract boolean canEnable(TransferData[] availableTypes)  throws JavaScriptModelException;
		
		/**
		 * Only called if {@link #canEnable(TransferData[])} returns <code>true</code>.
		 * @param selectedJavaElements 
		 * @param selectedResources 
		 * @param selectedWorkingSets 
		 * @return whether the paste action can be enabled
		 * @throws JavaScriptModelException 
		 */
		public abstract boolean canPasteOn(IJavaScriptElement[] selectedJavaElements, IResource[] selectedResources, IWorkingSet[] selectedWorkingSets)  throws JavaScriptModelException;
		
		/**
		 * only called if {@link #canPasteOn(IJavaScriptElement[], IResource[], IWorkingSet[])} returns <code>true</code>
		 * @param selectedJavaElements 
		 * @param selectedResources 
		 * @param selectedWorkingSets 
		 * @param availableTypes 
		 * @throws JavaScriptModelException 
		 * @throws InterruptedException 
		 * @throws InvocationTargetException 
		 */
		public abstract void paste(IJavaScriptElement[] selectedJavaElements, IResource[] selectedResources, IWorkingSet[] selectedWorkingSets, TransferData[] availableTypes) throws JavaScriptModelException, InterruptedException, InvocationTargetException;
	}
    
    private static class TextPaster extends Paster {

		private static class ParsedCu {
			private final String fText;
			private final String fTypeName;
			private final String fPackageName;

			public static ParsedCu[] parse(IJavaScriptProject javaProject, String text) {
				IScanner scanner= ToolFactory.createScanner(false, false, false, false);
				scanner.setSource(text.toCharArray());
				
				ArrayList cus= new ArrayList();
				int start= 0;
				boolean tokensScanned= false;
				int tok;
				while (true) {
					try {
						tok= scanner.getNextToken();
					} catch (InvalidInputException e) {
						// Handle gracefully to give the ASTParser a chance to recover,
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=168691
						tok= ITerminalSymbols.TokenNameEOF;
					}
					if (tok == ITerminalSymbols.TokenNamepackage && tokensScanned) {
						int packageStart= scanner.getCurrentTokenStartPosition();
						ParsedCu cu= parseCu(javaProject, text.substring(start, packageStart));
						if (cu != null) {
							cus.add(cu);
							start= packageStart;
						}
					} else if (tok == ITerminalSymbols.TokenNameEOF) {
						ParsedCu cu= parseCu(javaProject, text.substring(start, text.length()));
						if (cu != null) {
							cus.add(cu);
						}
						break;
					}
					tokensScanned= true;
				}

				return (ParsedCu[]) cus.toArray(new ParsedCu[cus.size()]);
			}
			
			private static ParsedCu parseCu(IJavaScriptProject javaProject, String text) {
				String packageName= IPackageFragment.DEFAULT_PACKAGE_NAME;
				ASTParser parser= ASTParser.newParser(AST.JLS3);
				parser.setProject(javaProject);
				parser.setSource(text.toCharArray());
				parser.setStatementsRecovery(true);
				JavaScriptUnit unit= (JavaScriptUnit) parser.createAST(null);
				
				if (unit == null)
					return null;
				
				int typesCount= unit.types().size();
				String typeName= null;
				if (typesCount > 0) {
					// get first most visible type:
					int maxVisibility= Modifier.PRIVATE;
					for (ListIterator iter= unit.types().listIterator(typesCount); iter.hasPrevious();) {
						AbstractTypeDeclaration type= (AbstractTypeDeclaration) iter.previous();
						int visibility= JdtFlags.getVisibilityCode(type);
						if (! JdtFlags.isHigherVisibility(maxVisibility, visibility)) {
							maxVisibility= visibility;
							typeName= type.getName().getIdentifier();
						}
					}
				}
				if (typeName == null)
					return null;
				
				PackageDeclaration pack= unit.getPackage();
				if (pack != null) {
					packageName= pack.getName().getFullyQualifiedName();
				}
				
				return new ParsedCu(text, typeName, packageName);
			}
			
			private ParsedCu(String text, String typeName, String packageName) {
				fText= text;
				fTypeName= typeName;
				fPackageName= packageName;
			}

			public String getTypeName() {
				return fTypeName;
			}

			public String getPackageName() {
				return fPackageName;
			}

			public String getText() {
				return fText;
			}
		}
		
		private IPackageFragmentRoot fDestination;
		/**
		 * destination pack iff pasted 1 CU to package fragment or compilation unit, <code>null</code> otherwise
		 */
		private IPackageFragment fDestinationPack;
		private ParsedCu[] fParsedCus;
		private TransferData[] fAvailableTypes;
		
		protected TextPaster(Shell shell, Clipboard clipboard) {
			super(shell, clipboard);
		}
		
		public boolean canEnable(TransferData[] availableTypes) {
			fAvailableTypes= availableTypes;
			return PasteAction.isAvailable(TextTransfer.getInstance(), availableTypes);
		}

		public boolean canPasteOn(IJavaScriptElement[] javaElements, IResource[] resources, IWorkingSet[] selectedWorkingSets) throws JavaScriptModelException {
			if (selectedWorkingSets.length != 0)
				return false;
			if (resources.length != 0)
				return false; //alternative: create text file?
			if (javaElements.length > 1)
				return false;
			
			String text= getClipboardText(fAvailableTypes);
			IJavaScriptProject javaProject= null;
			IJavaScriptElement destination= null;
			if (javaElements.length == 1) {
				destination= javaElements[0];
				javaProject= destination.getJavaScriptProject();
			}
			fParsedCus= ParsedCu.parse(javaProject, text);
			
			if (fParsedCus.length == 0)
				return false;
			
			if (destination == null)
				return true;
			
			/*
			 * 1 CU: paste into package, adapt package declaration
			 * 2+ CUs: always paste into source folder
			 */
			
			IPackageFragmentRoot packageFragmentRoot;
			IPackageFragment destinationPack;
			switch (destination.getElementType()) {
				case IJavaScriptElement.JAVASCRIPT_PROJECT :
					IPackageFragmentRoot[] packageFragmentRoots= ((IJavaScriptProject) destination).getPackageFragmentRoots();
					for (int i= 0; i < packageFragmentRoots.length; i++) {
						packageFragmentRoot= packageFragmentRoots[i];
						if (isWritable(packageFragmentRoot)) {
							fDestination= packageFragmentRoot;
							return true;
						}
					}
					return false;
					
				case IJavaScriptElement.PACKAGE_FRAGMENT_ROOT :
					packageFragmentRoot= (IPackageFragmentRoot) destination;
					if (isWritable(packageFragmentRoot)) {
						fDestination= packageFragmentRoot;
						return true;
					}
					return false;
					
				case IJavaScriptElement.PACKAGE_FRAGMENT :
					destinationPack= (IPackageFragment) destination;
					packageFragmentRoot= (IPackageFragmentRoot) destinationPack.getParent();
					if (isWritable(packageFragmentRoot)) {
						fDestination= packageFragmentRoot;
						if (fParsedCus.length == 1) {
							fDestinationPack= destinationPack;
						}
						return true;
					}
					return false;
					
				case IJavaScriptElement.JAVASCRIPT_UNIT :
					destinationPack= (IPackageFragment) destination.getParent();
					packageFragmentRoot= (IPackageFragmentRoot) destinationPack.getParent();
					if (isWritable(packageFragmentRoot)) {
						fDestination= packageFragmentRoot;
						if (fParsedCus.length == 1) {
							fDestinationPack= destinationPack;
						}
						return true;
					}
					return false;
					
				default:
					return false;
			}
		}
		
		private boolean isWritable(IPackageFragmentRoot packageFragmentRoot) {
			try {
				return packageFragmentRoot.exists() && ! packageFragmentRoot.isArchive() && ! packageFragmentRoot.isReadOnly()
						&& packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE;
			} catch (JavaScriptModelException e) {
				return false;
			}
		}

		public void paste(IJavaScriptElement[] javaElements, IResource[] resources, IWorkingSet[] selectedWorkingSets, TransferData[] availableTypes) throws JavaScriptModelException, InterruptedException, InvocationTargetException{
			final IEditorPart[] editorPart= new IEditorPart[1];
			
			IRunnableWithProgress op= new IRunnableWithProgress() {
				private IPath fVMPath;
				private String fCompilerCompliance;

				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					final ArrayList cus= new ArrayList();
					try {
						JavaScriptCore.run(new IWorkspaceRunnable() {
							public void run(IProgressMonitor pm) throws CoreException {
								pm.beginTask("", 1 + fParsedCus.length); //$NON-NLS-1$
					
								if (fDestination == null) {
									fDestination= createNewProject(new SubProgressMonitor(pm, 1));
								} else {
									pm.worked(1);
								}
								IConfirmQuery confirmQuery= new ReorgQueries(getShell()).createYesYesToAllNoNoToAllQuery(ReorgMessages.PasteAction_TextPaster_confirmOverwriting, true, IReorgQueries.CONFIRM_OVERWRITING);
								for (int i= 0; i < fParsedCus.length; i++) {
									if (pm.isCanceled())
										break;
									IJavaScriptUnit cu= pasteCU(fParsedCus[i], new SubProgressMonitor(pm, 1), confirmQuery);
									if (cu != null)
										cus.add(cu);
								}
						
							}
						}, monitor);
					} catch (OperationCanceledException e) {
						// cancelling is fine
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					} finally {
						monitor.done();
					}
					IResource[] cuResources= ResourceUtil.getFiles((IJavaScriptUnit[]) cus.toArray(new IJavaScriptUnit[cus.size()]));
					SelectionUtil.selectAndReveal(cuResources, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
				}

				private IJavaScriptUnit pasteCU(ParsedCu parsedCu, SubProgressMonitor pm, IConfirmQuery confirmQuery) throws CoreException, OperationCanceledException {
					pm.beginTask("", 4); //$NON-NLS-1$
					try {
						IPackageFragment destinationPack;
						if (fDestinationPack != null) {
							destinationPack= fDestinationPack;
							pm.worked(1);
						} else {
							String packageName= parsedCu.getPackageName();
							destinationPack= fDestination.getPackageFragment(packageName);
							if (! destinationPack.exists()) {
								JavaModelUtil.getPackageFragmentRoot(destinationPack).createPackageFragment(packageName, true, new SubProgressMonitor(pm, 1));
							} else {
								pm.worked(1);
							}
						}
						
						final String cuName= parsedCu.getTypeName() + JavaModelUtil.DEFAULT_CU_SUFFIX;
						IJavaScriptUnit cu= destinationPack.getJavaScriptUnit(cuName);
						boolean alreadyExists= cu.exists();
						if (alreadyExists) {
							String msg= Messages.format(ReorgMessages.PasteAction_TextPaster_exists, new Object[] {cuName});
							boolean overwrite= confirmQuery.confirm(msg);
							if (! overwrite)
								return null;
							
							editorPart[0]= openCu(cu); //Open editor before overwriting to allow undo to restore original package declaration
						}
						
						destinationPack.createCompilationUnit(cuName, parsedCu.getText(), true, new SubProgressMonitor(pm, 1));
						
						if (! alreadyExists) {
							editorPart[0]= openCu(cu);
						}
						if (fDestinationPack != null && ! fDestinationPack.getElementName().equals(parsedCu.getPackageName())) {
							if (! alreadyExists && editorPart[0] != null)
								editorPart[0].doSave(new SubProgressMonitor(pm, 1)); //avoid showing error marker due to missing/wrong package declaration
							else
								pm.worked(1);
						} else {
							pm.worked(1);
						}
						return cu;
					} finally {
						pm.done();
					}
				}

				private IPackageFragmentRoot createNewProject(SubProgressMonitor pm) throws CoreException {
					pm.beginTask("", 10); //$NON-NLS-1$
					IProject project;
					int i= 1;
					do {
						String name= Messages.format(ReorgMessages.PasteAction_projectName, i == 1 ? (Object) "" : Integer.valueOf(i)); //$NON-NLS-1$
						project= JavaScriptPlugin.getWorkspace().getRoot().getProject(name);
						i++;
					} while (project.exists());
					
					BuildPathsBlock.createProject(project, null, new SubProgressMonitor(pm, 3));
					BuildPathsBlock.addJavaNature(project, new SubProgressMonitor(pm, 1));
					IJavaScriptProject javaProject= JavaScriptCore.create(project);
					
					IResource srcFolder;
					IPreferenceStore store= PreferenceConstants.getPreferenceStore();
					String sourceFolderName= store.getString(PreferenceConstants.SRCBIN_SRCNAME);
					if (store.getBoolean(PreferenceConstants.SRCBIN_FOLDERS_IN_NEWPROJ) && sourceFolderName.length() > 0) {
						IFolder folder= project.getFolder(sourceFolderName);
						if (! folder.exists()) {
							folder.create(false, true, new SubProgressMonitor(pm, 1));
						}
						srcFolder= folder;
					} else {
						srcFolder= project;
					}
					
					if (fCompilerCompliance != null) {
						Map options= javaProject.getOptions(false);
						JavaModelUtil.setCompilanceOptions(options, fCompilerCompliance);
						JavaModelUtil.setDefaultClassfileOptions(options, fCompilerCompliance);
						javaProject.setOptions(options);
					}
					IIncludePathEntry srcEntry= JavaScriptCore.newSourceEntry(srcFolder.getFullPath());
					IIncludePathEntry jreEntry= JavaScriptCore.newContainerEntry(fVMPath);
					//IPath outputLocation= BuildPathsBlock.getDefaultOutputLocation(javaProject);
					IIncludePathEntry[] cpes= new IIncludePathEntry[] { srcEntry, jreEntry };
					javaProject.setRawIncludepath(cpes, new SubProgressMonitor(pm, 1));
					return javaProject.getPackageFragmentRoot(srcFolder);
				}

			};
			
			IRunnableContext context= JavaScriptPlugin.getActiveWorkbenchWindow();
			if (context == null) {
				context= new BusyIndicatorRunnableContext();
			}
			PlatformUI.getWorkbench().getProgressService().runInUI(context, op, JavaScriptPlugin.getWorkspace().getRoot());
			
			if (editorPart[0] != null)
				editorPart[0].getEditorSite().getPage().activate(editorPart[0]); //activate editor again, since runInUI restores previous active part
		}

		private IEditorPart openCu(IJavaScriptUnit cu) {
			try {
				return JavaScriptUI.openInEditor(cu, true, true);
			} catch (PartInitException e) {
				JavaScriptPlugin.log(e);
				return null;
			} catch (JavaScriptModelException e) {
				JavaScriptPlugin.log(e);
				return null;
			}
		}
    }
    
	private static class WorkingSetPaster extends Paster {
		protected WorkingSetPaster(Shell shell, Clipboard clipboard) {
			super(shell, clipboard);
		}
		public void paste(IJavaScriptElement[] selectedJavaElements, IResource[] selectedResources, IWorkingSet[] selectedWorkingSets, TransferData[] availableTypes) throws JavaScriptModelException, InterruptedException, InvocationTargetException {
			IWorkingSet workingSet= selectedWorkingSets[0];
			Set elements= new HashSet(Arrays.asList(workingSet.getElements()));
			IJavaScriptElement[] javaElements= getClipboardJavaElements(availableTypes);
			if (javaElements != null) {
				for (int i= 0; i < javaElements.length; i++) {
					if (!ReorgUtils.containsElementOrParent(elements, javaElements[i]))
						elements.add(javaElements[i]);
				}
			}
			IResource[] resources= getClipboardResources(availableTypes);
			if (resources != null) {
				List realJavaElements= new ArrayList();
				List realResource= new ArrayList();
				ReorgUtils.splitIntoJavaElementsAndResources(resources, realJavaElements, realResource);
				for (Iterator iter= realJavaElements.iterator(); iter.hasNext();) {
					IJavaScriptElement element= (IJavaScriptElement)iter.next();
					if (!ReorgUtils.containsElementOrParent(elements, element))
						elements.add(element);
				}
				for (Iterator iter= realResource.iterator(); iter.hasNext();) {
					IResource element= (IResource)iter.next();
					if (!ReorgUtils.containsElementOrParent(elements, element))
						elements.add(element);
				}
			}
			workingSet.setElements((IAdaptable[])elements.toArray(new IAdaptable[elements.size()]));
		}
		public boolean canEnable(TransferData[] availableTypes) throws JavaScriptModelException {
			return isAvailable(ResourceTransfer.getInstance(), availableTypes) ||
				isAvailable(JavaElementTransfer.getInstance(), availableTypes);
		}
		public boolean canPasteOn(IJavaScriptElement[] selectedJavaElements, IResource[] selectedResources, IWorkingSet[] selectedWorkingSets) throws JavaScriptModelException {
			if (selectedResources.length != 0 || selectedJavaElements.length != 0 || selectedWorkingSets.length != 1)
				return false;
			IWorkingSet ws= selectedWorkingSets[0];
			return !OthersWorkingSetUpdater.ID.equals(ws.getId());
		}
	}
	
    private static class ProjectPaster extends Paster{
    	
    	protected ProjectPaster(Shell shell, Clipboard clipboard) {
			super(shell, clipboard);
		}

		public boolean canEnable(TransferData[] availableDataTypes) {
			boolean resourceTransfer= isAvailable(ResourceTransfer.getInstance(), availableDataTypes);
			boolean javaElementTransfer= isAvailable(JavaElementTransfer.getInstance(), availableDataTypes);
			if (! javaElementTransfer)
				return canPasteSimpleProjects(availableDataTypes);
			if (! resourceTransfer)
				return canPasteJavaProjects(availableDataTypes);
			return canPasteJavaProjects(availableDataTypes) && canPasteSimpleProjects(availableDataTypes);
    	}
    	
		public void paste(IJavaScriptElement[] javaElements, IResource[] resources, IWorkingSet[] selectedWorkingSets, TransferData[] availableTypes) {
			pasteProjects(availableTypes);
		}

		private void pasteProjects(TransferData[] availableTypes) {
			pasteProjects(getProjectsToPaste(availableTypes));
		}
		
		private void pasteProjects(IProject[] projects){
			Shell shell= getShell();
			for (int i = 0; i < projects.length; i++) {
				new CopyProjectOperation(shell).copyProject(projects[i]);
			}
		}
		private IProject[] getProjectsToPaste(TransferData[] availableTypes) {
			IResource[] resources= getClipboardResources(availableTypes);
			IJavaScriptElement[] javaElements= getClipboardJavaElements(availableTypes);
			Set result= new HashSet();
			if (resources != null)
				result.addAll(Arrays.asList(resources));
			if (javaElements != null)
				result.addAll(Arrays.asList(ReorgUtils.getNotNulls(ReorgUtils.getResources(javaElements))));
			Assert.isTrue(result.size() > 0);
			return (IProject[]) result.toArray(new IProject[result.size()]);
		}

		public boolean canPasteOn(IJavaScriptElement[] javaElements, IResource[] resources, IWorkingSet[] selectedWorkingSets) {
			return selectedWorkingSets.length == 0; // Can't paste on working sets here
		}
		
		private boolean canPasteJavaProjects(TransferData[] availableDataTypes) {
			IJavaScriptElement[] javaElements= getClipboardJavaElements(availableDataTypes);
			return 	javaElements != null && 
					javaElements.length != 0 && 
					! ReorgUtils.hasElementsNotOfType(javaElements, IJavaScriptElement.JAVASCRIPT_PROJECT);
		}

		private boolean canPasteSimpleProjects(TransferData[] availableDataTypes) {
			IResource[] resources= getClipboardResources(availableDataTypes);
			if (resources == null || resources.length == 0) return false;
			for (int i= 0; i < resources.length; i++) {
				if (resources[i].getType() != IResource.PROJECT || ! ((IProject)resources[i]).isOpen())
					return false;
			}
			return true;
		}
    }
    
    private static class FilePaster extends Paster{
		protected FilePaster(Shell shell, Clipboard clipboard) {
			super(shell, clipboard);
		}

		public void paste(IJavaScriptElement[] javaElements, IResource[] resources, IWorkingSet[] selectedWorkingSets, TransferData[] availableTypes) throws JavaScriptModelException {
			String[] fileData= getClipboardFiles(availableTypes);
			if (fileData == null)
				return;
    		
			IContainer container= getAsContainer(getTarget(javaElements, resources));
			if (container == null)
				return;
				
			new CopyFilesAndFoldersOperation(getShell()).copyFiles(fileData, container);
		}
		
		private Object getTarget(IJavaScriptElement[] javaElements, IResource[] resources) {
			if (javaElements.length + resources.length == 1){
				if (javaElements.length == 1)
					return javaElements[0];
				else
					return resources[0];
			} else				
				return getCommonParent(javaElements, resources);
		}

		public boolean canPasteOn(IJavaScriptElement[] javaElements, IResource[] resources, IWorkingSet[] selectedWorkingSets) throws JavaScriptModelException {
			Object target= getTarget(javaElements, resources);
			return target != null && canPasteFilesOn(getAsContainer(target)) && selectedWorkingSets.length == 0;
		}

		public boolean canEnable(TransferData[] availableDataTypes) throws JavaScriptModelException {
			return isAvailable(FileTransfer.getInstance(), availableDataTypes);
		}
				
		private boolean canPasteFilesOn(Object target) {
			boolean isPackageFragment= target instanceof IPackageFragment;
			boolean isJavaProject= target instanceof IJavaScriptProject;
			boolean isPackageFragmentRoot= target instanceof IPackageFragmentRoot;
			boolean isContainer= target instanceof IContainer;
		
			if (!(isPackageFragment || isJavaProject || isPackageFragmentRoot || isContainer)) 
				return false;

			if (isContainer) {
				return true;
			} else {
				IJavaScriptElement element= (IJavaScriptElement)target;
				return !element.isReadOnly();
			}
		}
		
		private IContainer getAsContainer(Object target) throws JavaScriptModelException{
			if (target == null) 
				return null;
			if (target instanceof IContainer) 
				return (IContainer)target;
			if (target instanceof IFile)
				return ((IFile)target).getParent();
			return getAsContainer(((IJavaScriptElement)target).getCorrespondingResource());
		}
		
		private String[] getClipboardFiles(TransferData[] availableDataTypes) {
			Transfer transfer= FileTransfer.getInstance();
			if (isAvailable(transfer, availableDataTypes)) {
				return (String[])getContents(getClipboard(), transfer, getShell());
			}
			return null;
		}
		private Object getCommonParent(IJavaScriptElement[] javaElements, IResource[] resources) {
			return new ParentChecker(resources, javaElements).getCommonParent();		
		}
    }
    private static class JavaElementAndResourcePaster extends Paster {

		protected JavaElementAndResourcePaster(Shell shell, Clipboard clipboard) {
			super(shell, clipboard);
		}

		private TransferData[] fAvailableTypes;

		public void paste(IJavaScriptElement[] javaElements, IResource[] resources, IWorkingSet[] selectedWorkingSets, TransferData[] availableTypes) throws JavaScriptModelException, InterruptedException, InvocationTargetException{
			IResource[] clipboardResources= getClipboardResources(availableTypes);
			if (clipboardResources == null) 
				clipboardResources= new IResource[0];
			IJavaScriptElement[] clipboardJavaElements= getClipboardJavaElements(availableTypes);
			if (clipboardJavaElements == null) 
				clipboardJavaElements= new IJavaScriptElement[0];

			Object destination= getTarget(javaElements, resources);
			if (destination instanceof IJavaScriptElement)
				ReorgCopyStarter.create(clipboardJavaElements, clipboardResources, (IJavaScriptElement)destination).run(getShell());
			else if (destination instanceof IResource)
				ReorgCopyStarter.create(clipboardJavaElements, clipboardResources, (IResource)destination).run(getShell());
		}

		private Object getTarget(IJavaScriptElement[] javaElements, IResource[] resources) {
			if (javaElements.length + resources.length == 1){
				if (javaElements.length == 1)
					return javaElements[0];
				else
					return resources[0];
			} else				
				return getCommonParent(javaElements, resources);
		}
		
		private Object getCommonParent(IJavaScriptElement[] javaElements, IResource[] resources) {
			return new ParentChecker(resources, javaElements).getCommonParent();		
		}

		public boolean canPasteOn(IJavaScriptElement[] javaElements, IResource[] resources, IWorkingSet[] selectedWorkingSets) throws JavaScriptModelException {
			if (selectedWorkingSets.length != 0)
				return false;
			IResource[] clipboardResources= getClipboardResources(fAvailableTypes);
			if (clipboardResources == null) 
				clipboardResources= new IResource[0];
			IJavaScriptElement[] clipboardJavaElements= getClipboardJavaElements(fAvailableTypes);
			if (clipboardJavaElements == null) 
				clipboardJavaElements= new IJavaScriptElement[0];
			Object destination= getTarget(javaElements, resources);
			if (destination instanceof IJavaScriptElement)
				return ReorgCopyStarter.create(clipboardJavaElements, clipboardResources, (IJavaScriptElement)destination) != null;
			if (destination instanceof IResource)
				return ReorgCopyStarter.create(clipboardJavaElements, clipboardResources, (IResource)destination) != null;
			return false;
		}
		
		public boolean canEnable(TransferData[] availableTypes) {
			fAvailableTypes= availableTypes;
			return isAvailable(JavaElementTransfer.getInstance(), availableTypes) || isAvailable(ResourceTransfer.getInstance(), availableTypes);
		}
    }
    
    private static class TypedSourcePaster extends Paster{

		protected TypedSourcePaster(Shell shell, Clipboard clipboard) {
			super(shell, clipboard);
		}
		private TransferData[] fAvailableTypes;

		public boolean canEnable(TransferData[] availableTypes) throws JavaScriptModelException {
			fAvailableTypes= availableTypes;
			return isAvailable(TypedSourceTransfer.getInstance(), availableTypes);
		}

		public boolean canPasteOn(IJavaScriptElement[] selectedJavaElements, IResource[] selectedResources, IWorkingSet[] selectedWorkingSets) throws JavaScriptModelException {
			if (selectedResources.length != 0 || selectedWorkingSets.length != 0)
				return false;
			TypedSource[] typedSources= getClipboardTypedSources(fAvailableTypes);				
			Object destination= getTarget(selectedJavaElements, selectedResources);
			if (destination instanceof IJavaScriptElement)
				return ReorgTypedSourcePasteStarter.create(typedSources, (IJavaScriptElement)destination) != null;
			return false;
		}
		
		public void paste(IJavaScriptElement[] selectedJavaElements, IResource[] selectedResources, IWorkingSet[] selectedWorkingSets, TransferData[] availableTypes) throws JavaScriptModelException, InterruptedException, InvocationTargetException {
			TypedSource[] typedSources= getClipboardTypedSources(availableTypes);
			IJavaScriptElement destination= getTarget(selectedJavaElements, selectedResources);
			ReorgTypedSourcePasteStarter.create(typedSources, destination).run(getShell());		
		}
		
		private static IJavaScriptElement getTarget(IJavaScriptElement[] selectedJavaElements, IResource[] selectedResources) {
			Assert.isTrue(selectedResources.length == 0);
			if (selectedJavaElements.length == 1) 
				return getAsTypeOrCu(selectedJavaElements[0]);
			Object parent= new ParentChecker(selectedResources, selectedJavaElements).getCommonParent();
			if (parent instanceof IJavaScriptElement)
				return getAsTypeOrCu((IJavaScriptElement)parent);
			return null;
		}
		private static IJavaScriptElement getAsTypeOrCu(IJavaScriptElement element) {
			//try to get type first
			if (element.getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT || element.getElementType() == IJavaScriptElement.TYPE)
				return element;
			IJavaScriptElement ancestorType= element.getAncestor(IJavaScriptElement.TYPE);
			if (ancestorType != null)
				return ancestorType;
			return ReorgUtils.getCompilationUnit(element);
		}
		private static class ReorgTypedSourcePasteStarter {
	
			private final PasteTypedSourcesRefactoring fPasteRefactoring;

			private ReorgTypedSourcePasteStarter(PasteTypedSourcesRefactoring pasteRefactoring) {
				Assert.isNotNull(pasteRefactoring);
				fPasteRefactoring= pasteRefactoring;
			}
	
			public static ReorgTypedSourcePasteStarter create(TypedSource[] typedSources, IJavaScriptElement destination) {
				Assert.isNotNull(typedSources);
				Assert.isNotNull(destination);
				PasteTypedSourcesRefactoring pasteRefactoring= PasteTypedSourcesRefactoring.create(typedSources);
				if (pasteRefactoring == null)
					return null;
				if (! pasteRefactoring.setDestination(destination).isOK())
					return null;
				return new ReorgTypedSourcePasteStarter(pasteRefactoring);
			}

			public void run(Shell parent) throws InterruptedException, InvocationTargetException {
				IRunnableContext context= new ProgressMonitorDialog(parent);
				new RefactoringExecutionHelper(fPasteRefactoring, RefactoringCore.getConditionCheckingFailedSeverity(), RefactoringSaveHelper.SAVE_NOTHING, parent, context).perform(false, false);
			}
		}
		private static class PasteTypedSourcesRefactoring extends Refactoring {
			
			private final TypedSource[] fSources;
			private IJavaScriptElement fDestination;
			
			static PasteTypedSourcesRefactoring create(TypedSource[] sources){
				if (! isAvailable(sources))
					return null;
				return new PasteTypedSourcesRefactoring(sources);
			}
			public RefactoringStatus setDestination(IJavaScriptElement destination) {
				fDestination= destination;
				if (ReorgUtils.getCompilationUnit(destination) == null)
					return RefactoringStatus.createFatalErrorStatus(ReorgMessages.PasteAction_wrong_destination); 
				if (! destination.exists())
					return RefactoringStatus.createFatalErrorStatus(ReorgMessages.PasteAction_element_doesnot_exist); 
				if (! canPasteAll(destination))
					return RefactoringStatus.createFatalErrorStatus(ReorgMessages.PasteAction_invalid_destination); 
				return new RefactoringStatus();
			}
			private boolean canPasteAll(IJavaScriptElement destination) {
				for (int i= 0; i < fSources.length; i++) {
					if (! canPaste(fSources[i].getType(), destination))
						return false;
				}
				return true;
			}
			private static boolean canPaste(int elementType, IJavaScriptElement destination) {
				IType ancestorType= getAncestorType(destination);
				if (ancestorType != null)
					return canPasteToType(elementType);
				return canPasteToCu(elementType);
			}
			private static boolean canPasteToType(int elementType) {
				return 	elementType == IJavaScriptElement.TYPE || 
						elementType == IJavaScriptElement.FIELD || 
						elementType == IJavaScriptElement.INITIALIZER || 
						elementType == IJavaScriptElement.METHOD;
			}
			private static boolean canPasteToCu(int elementType) {
				return	elementType == IJavaScriptElement.TYPE ||
						elementType == IJavaScriptElement.IMPORT_DECLARATION;
			}
			PasteTypedSourcesRefactoring(TypedSource[] sources){
				Assert.isNotNull(sources);
				Assert.isTrue(sources.length != 0);
				fSources= sources;
			}

			private static boolean isAvailable(TypedSource[] sources) {
				return sources != null && sources.length > 0;
			}

			public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
				return new RefactoringStatus();
			}

			public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
				RefactoringStatus result= Checks.validateModifiesFiles(
					ResourceUtil.getFiles(new IJavaScriptUnit[]{getDestinationCu()}), getValidationContext());
				return result;
			}

			public Change createChange(IProgressMonitor pm) throws CoreException {
				ASTParser p= ASTParser.newParser(AST.JLS3);
				p.setSource(getDestinationCu());
				JavaScriptUnit cuNode= (JavaScriptUnit) p.createAST(pm);
				ASTRewrite rewrite= ASTRewrite.create(cuNode.getAST());
				TypedSource source= null;
				for (int i= fSources.length - 1; i >= 0; i--) {
					source= fSources[i];
					final ASTNode destination= getDestinationNodeForSourceElement(fDestination, source.getType(), cuNode);
					if (destination != null) {
						if (destination instanceof JavaScriptUnit)
							insertToCu(rewrite, createNewNodeToInsertToCu(source, rewrite), (JavaScriptUnit) destination);
						else if (destination instanceof AbstractTypeDeclaration)
							insertToType(rewrite, createNewNodeToInsertToType(source, rewrite), (AbstractTypeDeclaration) destination);
					}
				}
				final CompilationUnitChange result= new CompilationUnitChange(ReorgMessages.PasteAction_change_name, getDestinationCu()); 
				try {
					ITextFileBuffer buffer= RefactoringFileBuffers.acquire(getDestinationCu());
					TextEdit rootEdit= rewrite.rewriteAST(buffer.getDocument(), fDestination.getJavaScriptProject().getOptions(true));
					if (getDestinationCu().isWorkingCopy())
						result.setSaveMode(TextFileChange.LEAVE_DIRTY);
					TextChangeCompatibility.addTextEdit(result, ReorgMessages.PasteAction_edit_name, rootEdit); 
				} finally {
					RefactoringFileBuffers.release(getDestinationCu());
				}
				return result;
			}

			private static void insertToType(ASTRewrite rewrite, ASTNode node, AbstractTypeDeclaration typeDeclaration) {
				switch (node.getNodeType()) {
					case ASTNode.TYPE_DECLARATION:
					case ASTNode.FUNCTION_DECLARATION:
					case ASTNode.FIELD_DECLARATION:
					case ASTNode.INITIALIZER:
						rewrite.getListRewrite(typeDeclaration, typeDeclaration.getBodyDeclarationsProperty()).insertAt(node, ASTNodes.getInsertionIndex((BodyDeclaration) node, typeDeclaration.bodyDeclarations()), null);
						break;
					default:
						Assert.isTrue(false, String.valueOf(node.getNodeType()));
				}
			}

			private static void insertToCu(ASTRewrite rewrite, ASTNode node, JavaScriptUnit cuNode) {
				switch (node.getNodeType()) {
					case ASTNode.TYPE_DECLARATION:
						rewrite.getListRewrite(cuNode, JavaScriptUnit.TYPES_PROPERTY).insertAt(node, ASTNodes.getInsertionIndex((AbstractTypeDeclaration) node, cuNode.types()), null);
						break;
					case ASTNode.IMPORT_DECLARATION:
						rewrite.getListRewrite(cuNode, JavaScriptUnit.IMPORTS_PROPERTY).insertLast(node, null);
						break;
					case ASTNode.PACKAGE_DECLARATION:
						// only insert if none exists
						if (cuNode.getPackage() == null)
							rewrite.set(cuNode, JavaScriptUnit.PACKAGE_PROPERTY, node, null);
						break;
					default:
						Assert.isTrue(false, String.valueOf(node.getNodeType()));
				}
			}

			/**
			 * @return an AbstractTypeDeclaration, a JavaScriptUnit, or null
			 */ 
			private ASTNode getDestinationNodeForSourceElement(IJavaScriptElement destination, int kind, JavaScriptUnit unit) throws JavaScriptModelException {
				final IType ancestor= getAncestorType(destination);
				if (ancestor != null)
					return ASTNodeSearchUtil.getAbstractTypeDeclarationNode(ancestor, unit);
				if (kind == IJavaScriptElement.TYPE || kind == IJavaScriptElement.IMPORT_DECLARATION || kind == IJavaScriptElement.IMPORT_CONTAINER)
					return unit;
				return null;	
			}
			
			private static IType getAncestorType(IJavaScriptElement destinationElement) {
				return destinationElement.getElementType() == IJavaScriptElement.TYPE ? (IType)destinationElement: (IType)destinationElement.getAncestor(IJavaScriptElement.TYPE);
			}
			private ASTNode createNewNodeToInsertToCu(TypedSource source, ASTRewrite rewrite) {
				switch(source.getType()){
					case IJavaScriptElement.TYPE:
						return rewrite.createStringPlaceholder(source.getSource(), ASTNode.TYPE_DECLARATION);
					case IJavaScriptElement.IMPORT_DECLARATION:
						return rewrite.createStringPlaceholder(source.getSource(), ASTNode.IMPORT_DECLARATION);
					default: Assert.isTrue(false, String.valueOf(source.getType()));
						return null;
				}
			}
			
			private ASTNode createNewNodeToInsertToType(TypedSource source, ASTRewrite rewrite) {
				switch(source.getType()){
					case IJavaScriptElement.TYPE:
						return rewrite.createStringPlaceholder(source.getSource(), ASTNode.TYPE_DECLARATION);
					case IJavaScriptElement.METHOD:
						return rewrite.createStringPlaceholder(source.getSource(), ASTNode.FUNCTION_DECLARATION);
					case IJavaScriptElement.FIELD:
						return rewrite.createStringPlaceholder(source.getSource(), ASTNode.FIELD_DECLARATION);
					case IJavaScriptElement.INITIALIZER:
						return rewrite.createStringPlaceholder(source.getSource(), ASTNode.INITIALIZER);
					default: Assert.isTrue(false);
						return null;
				}
			}
			
			private IJavaScriptUnit getDestinationCu() {
				return ReorgUtils.getCompilationUnit(fDestination);
			}

			public String getName() {
				return ReorgMessages.PasteAction_name; 
			}
		}
    }
}
