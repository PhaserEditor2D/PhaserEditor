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
package org.eclipse.wst.jsdt.internal.ui.javaeditor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.RTFTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.texteditor.IAbstractTextEditorHelpContextIds;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.IBinding;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.Name;
import org.eclipse.wst.jsdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.ImportReferencesCollector;
import org.eclipse.wst.jsdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.wst.jsdt.internal.corext.dom.Bindings;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;


/**
 * Action for cut/copy and paste with support for adding imports on paste.
 */
public final class ClipboardOperationAction extends TextEditorAction {

	public static class ClipboardData {
		private String fOriginHandle;
		private String[] fTypeImports;
		private String[] fStaticImports;

		public ClipboardData(IJavaScriptElement origin, String[] typeImports, String[] staticImports) {
			Assert.isNotNull(origin);
			Assert.isNotNull(typeImports);
			Assert.isNotNull(staticImports);

			fTypeImports= typeImports;
			fStaticImports= staticImports;
			fOriginHandle= origin.getHandleIdentifier();
		}

		public ClipboardData(byte[] bytes) throws IOException {
			DataInputStream dataIn = new DataInputStream(new ByteArrayInputStream(bytes));
			try {
				fOriginHandle= dataIn.readUTF();
				fTypeImports= readArray(dataIn);
				fStaticImports= readArray(dataIn);
			} finally {
				dataIn.close();
			}
		}

		private static String[] readArray(DataInputStream dataIn) throws IOException {
			int count= dataIn.readInt();

			String[] array= new String[count];
			for (int i = 0; i < count; i++) {
				array[i]= dataIn.readUTF();
			}
			return array;
		}

		private static void writeArray(DataOutputStream dataOut, String[] array) throws IOException {
			dataOut.writeInt(array.length);
			for (int i = 0; i < array.length; i++) {
				dataOut.writeUTF(array[i]);
			}
		}

		public String[] getTypeImports() {
			return fTypeImports;
		}

		public String[] getStaticImports() {
			return fStaticImports;
		}

		public boolean isFromSame(IJavaScriptElement elem) {
			return fOriginHandle.equals(elem.getHandleIdentifier());
		}

		public byte[] serialize() throws IOException {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DataOutputStream dataOut = new DataOutputStream(out);
			try {
				dataOut.writeUTF(fOriginHandle);
				writeArray(dataOut, fTypeImports);
				writeArray(dataOut, fStaticImports);
			} finally {
				dataOut.close();
				out.close();
			}

			return out.toByteArray();
		}
	}


	private static class ClipboardTransfer extends ByteArrayTransfer {

		private static final String TYPE_NAME = "source-with-imports-transfer-format" + System.currentTimeMillis(); //$NON-NLS-1$

		private static final int TYPEID = registerType(TYPE_NAME);

		/* (non-Javadoc)
		 * @see org.eclipse.swt.dnd.Transfer#getTypeIds()
		 */
		protected int[] getTypeIds() {
			return new int[] { TYPEID };
		}

		/* (non-Javadoc)
		 * @see org.eclipse.swt.dnd.Transfer#getTypeNames()
		 */
		protected String[] getTypeNames() {
			return new String[] { TYPE_NAME };
		}

		/* (non-Javadoc)
		 * @see org.eclipse.swt.dnd.Transfer#javaToNative(java.lang.Object, org.eclipse.swt.dnd.TransferData)
		 */
		protected void javaToNative(Object data, TransferData transferData) {
			if (data instanceof ClipboardData) {
				try {
					super.javaToNative(((ClipboardData) data).serialize(), transferData);
				} catch (IOException e) {
					//it's best to send nothing if there were problems
				}
			}
		}

		/* (non-Javadoc)
		 * Method declared on Transfer.
		 */
		protected Object nativeToJava(TransferData transferData) {
			byte[] bytes = (byte[]) super.nativeToJava(transferData);
			if (bytes != null) {
				try {
					return new ClipboardData(bytes);
				} catch (IOException e) {
				}
			}
			return null;
		}

	}

	private static final ClipboardTransfer fgTransferInstance = new ClipboardTransfer();

	/** The text operation code */
	private int fOperationCode= -1;
	/** The text operation target */
	private ITextOperationTarget fOperationTarget;


	/**
	 * Creates the action.
	 */
	public ClipboardOperationAction(ResourceBundle bundle, String prefix, ITextEditor editor, int operationCode) {
		super(bundle, prefix, editor);
		fOperationCode= operationCode;

		if (operationCode == ITextOperationTarget.CUT) {
			setHelpContextId(IAbstractTextEditorHelpContextIds.CUT_ACTION);
			setActionDefinitionId(IWorkbenchActionDefinitionIds.CUT);
		} else if (operationCode == ITextOperationTarget.COPY) {
			setHelpContextId(IAbstractTextEditorHelpContextIds.COPY_ACTION);
			setActionDefinitionId(IWorkbenchActionDefinitionIds.COPY);
		} else if (operationCode == ITextOperationTarget.PASTE) {
			setHelpContextId(IAbstractTextEditorHelpContextIds.PASTE_ACTION);
			setActionDefinitionId(IWorkbenchActionDefinitionIds.PASTE);
		} else {
			Assert.isTrue(false, "Invalid operation code"); //$NON-NLS-1$
		}
		update();
	}

	private boolean isReadOnlyOperation() {
		return fOperationCode == ITextOperationTarget.COPY;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		if (fOperationCode == -1 || fOperationTarget == null)
			return;

		ITextEditor editor= getTextEditor();
		if (editor == null)
			return;

		if (!isReadOnlyOperation() && !validateEditorInputState())
			return;

		BusyIndicator.showWhile(getDisplay(), new Runnable() {
			public void run() {
				internalDoOperation();
			}
		});
	}

	private Shell getShell() {
		ITextEditor editor= getTextEditor();
		if (editor != null) {
			IWorkbenchPartSite site= editor.getSite();
			Shell shell= site.getShell();
			if (shell != null && !shell.isDisposed()) {
				return shell;
			}
		}
		return null;
	}

	private Display getDisplay() {
		Shell shell= getShell();
		if (shell != null) {
			return shell.getDisplay();
		}
		return null;
	}


	protected final void internalDoOperation() {
		if (PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_IMPORTS_ON_PASTE)) {
			if (fOperationCode == ITextOperationTarget.PASTE) {
				doPasteWithImportsOperation();
			} else {
				doCutCopyWithImportsOperation();
			}
		} else {
			fOperationTarget.doOperation(fOperationCode);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.IUpdate#update()
	 */
	public void update() {
		super.update();

		if (!isReadOnlyOperation() && !canModifyEditor()) {
			setEnabled(false);
			return;
		}

		ITextEditor editor= getTextEditor();
		if (fOperationTarget == null && editor!= null && fOperationCode != -1)
			fOperationTarget= (ITextOperationTarget) editor.getAdapter(ITextOperationTarget.class);

		boolean isEnabled= (fOperationTarget != null && fOperationTarget.canDoOperation(fOperationCode));
		setEnabled(isEnabled);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.TextEditorAction#setEditor(org.eclipse.ui.texteditor.ITextEditor)
	 */
	public void setEditor(ITextEditor editor) {
		super.setEditor(editor);
		fOperationTarget= null;
	}


	private void doCutCopyWithImportsOperation() {
		ITextEditor editor= getTextEditor();
		IJavaScriptElement inputElement= (IJavaScriptElement) editor.getEditorInput().getAdapter(IJavaScriptElement.class);
		ISelection selection= editor.getSelectionProvider().getSelection();

		Object clipboardData= null;
		if (inputElement != null && selection instanceof ITextSelection && !selection.isEmpty()) {
			ITextSelection textSelection= (ITextSelection) selection;
			if (isNonTrivialSelection(textSelection)) {
				clipboardData= getClipboardData(inputElement, textSelection.getOffset(), textSelection.getLength());
			}
		}

		fOperationTarget.doOperation(fOperationCode);

		if (clipboardData != null) {
			Clipboard clipboard= new Clipboard(getDisplay());
			try {
				/*
				 * We currently make assumptions about what the styled text widget sets,
				 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=61876
				 */
				Object textData= clipboard.getContents(TextTransfer.getInstance());
				Object rtfData= clipboard.getContents(RTFTransfer.getInstance());

				ArrayList datas= new ArrayList(3);
				ArrayList transfers= new ArrayList(3);
				if (textData != null) {
					datas.add(textData);
					transfers.add(TextTransfer.getInstance());
				}
				if (rtfData != null) {
					datas.add(rtfData);
					transfers.add(RTFTransfer.getInstance());
				}

				/*
				 * Don't add if we didn't get any data from the clipboard
				 * see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=70077
				 */
				if (datas.isEmpty())
					return;

				datas.add(clipboardData);
				transfers.add(fgTransferInstance);

				Transfer[] dataTypes= (Transfer[]) transfers.toArray(new Transfer[transfers.size()]);
				Object[] data= datas.toArray();
				setClipboardContents(clipboard, data, dataTypes);
			} finally {
				clipboard.dispose();
			}
		}
	}

	private void setClipboardContents(Clipboard clipboard, Object[] datas, Transfer[] transfers) {
		try {
			clipboard.setContents(datas, transfers);
		} catch (SWTError e) {
			if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD) {
				throw e;
			}
			// silently fail.  see e.g. https://bugs.eclipse.org/bugs/show_bug.cgi?id=65975
		}
	}

	private boolean isNonTrivialSelection(ITextSelection selection) {
		if (selection.getLength() < 30) {
			String text= selection.getText();
			if (text != null) {
				for (int i= 0; i < text.length(); i++) {
					if (!Character.isJavaIdentifierPart(text.charAt(i))) {
						return true;
					}
				}
			}
			return false;
		}
		return true;
	}


	private ClipboardData getClipboardData(IJavaScriptElement inputElement, int offset, int length) {
		JavaScriptUnit astRoot= JavaScriptPlugin.getDefault().getASTProvider().getAST(inputElement, ASTProvider.WAIT_ACTIVE_ONLY, null);
		if (astRoot == null) {
			return null;
		}

		// do process import if selection spans over import declaration or package
		List list= astRoot.imports();
		if (!list.isEmpty()) {
			if (offset < ((ASTNode) list.get(list.size() - 1)).getStartPosition()) {
				return null;
			}
		} else if (astRoot.getPackage() != null) {
			if (offset < ((ASTNode) astRoot.getPackage()).getStartPosition()) {
				return null;
			}
		}

		ArrayList typeImportsRefs= new ArrayList();
		ArrayList staticImportsRefs= new ArrayList();

		ImportReferencesCollector.collect(astRoot, inputElement.getJavaScriptProject(), new Region(offset, length), typeImportsRefs, staticImportsRefs);

		if (typeImportsRefs.isEmpty() && staticImportsRefs.isEmpty()) {
			return null;
		}

		HashSet namesToImport= new HashSet(typeImportsRefs.size());
		for (int i= 0; i < typeImportsRefs.size(); i++) {
			Name curr= (Name) typeImportsRefs.get(i);
			IBinding binding= curr.resolveBinding();
			if (binding != null && binding.getKind() == IBinding.TYPE) {
				ITypeBinding typeBinding= (ITypeBinding) binding;
				if (typeBinding.isArray()) {
					typeBinding= typeBinding.getElementType();
				}
				
				if (typeBinding.isMember() || typeBinding.isTopLevel()) {
					String name= Bindings.getRawQualifiedName(typeBinding);
					if (name.length() > 0) {
						namesToImport.add(name);
					}
				}
			}
		}

		HashSet staticsToImport= new HashSet(staticImportsRefs.size());
		for (int i= 0; i < staticImportsRefs.size(); i++) {
			Name curr= (Name) staticImportsRefs.get(i);
			IBinding binding= curr.resolveBinding();
			if (binding != null) {
				StringBuffer buf= new StringBuffer(Bindings.getImportName(binding));
				if (binding.getKind() == IBinding.METHOD) {
					buf.append("()"); //$NON-NLS-1$
				}
				staticsToImport.add(buf.toString());
			}
		}


		if (namesToImport.isEmpty() && staticsToImport.isEmpty()) {
			return null;
		}

		String[] typeImports= (String[]) namesToImport.toArray(new String[namesToImport.size()]);
		String[] staticImports= (String[]) staticsToImport.toArray(new String[staticsToImport.size()]);
		return new ClipboardData(inputElement, typeImports, staticImports);
	}


	private void doPasteWithImportsOperation() {
		ITextEditor editor= getTextEditor();
		IJavaScriptElement inputElement= (IJavaScriptElement) editor.getEditorInput().getAdapter(IJavaScriptElement.class);

		Clipboard clipboard= new Clipboard(getDisplay());
		ClipboardData importsData= (ClipboardData) clipboard.getContents(fgTransferInstance);
		if (importsData != null && inputElement instanceof IJavaScriptUnit && !importsData.isFromSame(inputElement)) {
			// combine operation and adding of imports
			IRewriteTarget target= editor != null ? (IRewriteTarget) editor.getAdapter(IRewriteTarget.class) : null;
			if (target != null) {
				target.beginCompoundChange();
			}
			try {
				fOperationTarget.doOperation(fOperationCode);
				addImports((IJavaScriptUnit) inputElement, importsData);
			} catch (CoreException e) {
				JavaScriptPlugin.log(e);
			} finally {
				if (target != null) {
					target.endCompoundChange();
				}
			}
		} else {
			fOperationTarget.doOperation(fOperationCode);
		}
	}


	private void addImports(IJavaScriptUnit unit, ClipboardData data) throws CoreException {
		ImportRewrite rewrite= StubUtility.createImportRewrite(unit, true);
		String[] imports= data.getTypeImports();
		for (int i= 0; i < imports.length; i++) {
			rewrite.addImport(imports[i]);
		}
		String[] staticImports= data.getStaticImports();
		for (int i= 0; i < staticImports.length; i++) {
			String name= Signature.getSimpleName(staticImports[i]);
			boolean isField= !name.endsWith("()"); //$NON-NLS-1$
			if (!isField) {
				name= name.substring(0, name.length() - 2);
			}
			String qualifier= Signature.getQualifier(staticImports[i]);
			rewrite.addStaticImport(qualifier, name, isField);
		}

		JavaModelUtil.applyEdit(unit, rewrite.rewriteImports(null), false, null);
	}


}
