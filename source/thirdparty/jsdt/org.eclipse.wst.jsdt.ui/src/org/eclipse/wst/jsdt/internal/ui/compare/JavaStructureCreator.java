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
package org.eclipse.wst.jsdt.internal.ui.compare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IEditableContentExtension;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.compare.structuremergeviewer.StructureCreator;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.services.IDisposable;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.ToolFactory;
import org.eclipse.wst.jsdt.core.compiler.IScanner;
import org.eclipse.wst.jsdt.core.compiler.ITerminalSymbols;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.ui.text.IJavaScriptPartitions;


public class JavaStructureCreator extends StructureCreator {
	
	private Map fDefaultCompilerOptions;
	
	/**
	 * A root node for the structure. It is similar to {@link org.eclipse.compare.structuremergeviewer.StructureRootNode} but needed
	 * to be a subclass of {@link JavaNode} because of the code used to build the structure.
	 */
	private final class RootJavaNode extends JavaNode implements IDisposable {
		
		private final Object fInput;
		private final boolean fEditable;
		private final ISharedDocumentAdapter fAdapter;

		private RootJavaNode(IDocument document, boolean editable, Object input, ISharedDocumentAdapter adapter) {
			super(document);
			this.fEditable = editable;
			fInput= input;
			fAdapter= adapter;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.compare.structuremergeviewer.DocumentRangeNode#isEditable()
		 */
		public boolean isEditable() {
			return fEditable;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.compare.structuremergeviewer.DocumentRangeNode#nodeChanged(org.eclipse.compare.structuremergeviewer.DocumentRangeNode)
		 */
		protected void nodeChanged(DocumentRangeNode node) {
			save(this, fInput);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.services.IDisposable#dispose()
		 */
		public void dispose() {
			if (fAdapter != null) {
				fAdapter.disconnect(fInput);
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.compare.structuremergeviewer.DocumentRangeNode#getAdapter(java.lang.Class)
		 */
		public Object getAdapter(Class adapter) {
			if (adapter == ISharedDocumentAdapter.class) {
				return fAdapter;
			}
			return super.getAdapter(adapter);
		}
		
		
		/* (non-Javadoc)
		 * @see org.eclipse.compare.structuremergeviewer.DocumentRangeNode#isReadOnly()
		 */
		public boolean isReadOnly() {
			if (fInput instanceof IEditableContentExtension) {
				IEditableContentExtension ext = (IEditableContentExtension) fInput;
				return ext.isReadOnly();
			}
			return super.isReadOnly();
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.compare.structuremergeviewer.DocumentRangeNode#validateEdit(org.eclipse.swt.widgets.Shell)
		 */
		public IStatus validateEdit(Shell shell) {
			if (fInput instanceof IEditableContentExtension) {
				IEditableContentExtension ext = (IEditableContentExtension) fInput;
				return ext.validateEdit(shell);
			}
			return super.validateEdit(shell);
		}
	}

	/**
	 * RewriteInfos are used temporarily when rewriting the diff tree
	 * in order to combine similar diff nodes ("smart folding").
	 */
	static class RewriteInfo {
		
		boolean fIsOut= false;
		
		JavaNode fAncestor= null;
		JavaNode fLeft= null;
		JavaNode fRight= null;
		
		ArrayList fChildren= new ArrayList();
		
		void add(IDiffElement diff) {
			fChildren.add(diff);
		}
		
		void setDiff(ICompareInput diff) {
			if (fIsOut)
				return;
			
			fIsOut= true;
			
			JavaNode a= (JavaNode) diff.getAncestor();
			JavaNode y= (JavaNode) diff.getLeft();
			JavaNode m= (JavaNode) diff.getRight();
			
			if (a != null) {
				if (fAncestor != null)
					return;
				fAncestor= a;
			}
			if (y != null) {
				if (fLeft != null)
					return;
				fLeft= y;
			}
			if (m != null) {
				if (fRight != null)
					return;
				fRight= m;
			}
			
			fIsOut= false;
		}
				
		/**
		 * @return true if some nodes could be successfully combined into one
		 */
		boolean matches() {
			return !fIsOut && fAncestor != null && fLeft != null && fRight != null;
		}
	}		
	
	public JavaStructureCreator() {
	}
	
	void setDefaultCompilerOptions(Map compilerSettings) {
		fDefaultCompilerOptions= compilerSettings;
	}
	
	/**
	 * @return the name that appears in the enclosing pane title bar
	 */
	public String getName() {
		return CompareMessages.JavaStructureViewer_title; 
	}
	
	/**
	 * @param input implement the IStreamContentAccessor interface
	 * @return a tree of JavaNodes for the given input.
	 * In case of error null is returned.
	 */
	public IStructureComparator getStructure(final Object input) {
		String contents= null;
		char[] buffer= null;
		IDocument doc= CompareUI.getDocument(input);
		if (doc == null) {
			if (input instanceof IStreamContentAccessor) {
				IStreamContentAccessor sca= (IStreamContentAccessor) input;			
				try {
					contents= JavaCompareUtilities.readString(sca);
				} catch (CoreException ex) {
					// return null indicates the error.
					return null;
				}			
			}
			
			if (contents != null) {
				int n= contents.length();
				buffer= new char[n];
				contents.getChars(0, n, buffer, 0);
				
				doc= new Document(contents);
				setupDocument(doc);				
			}
		}
		
		return createStructureComparator(input, buffer, doc, null, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.StructureCreator#createStructureComparator(java.lang.Object, org.eclipse.jface.text.IDocument, org.eclipse.compare.ISharedDocumentAdapter, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStructureComparator createStructureComparator(Object element,
			IDocument document, ISharedDocumentAdapter sharedDocumentAdapter,
			IProgressMonitor monitor) throws CoreException {
		return createStructureComparator(element, null, document, sharedDocumentAdapter, monitor);
	}
	
	private IStructureComparator createStructureComparator(final Object input, char[] buffer, IDocument doc, ISharedDocumentAdapter adapter, IProgressMonitor monitor) {
		String contents;
		Map compilerOptions= null;
		
		if (input instanceof IResourceProvider) {
			IResource resource= ((IResourceProvider) input).getResource();
			if (resource != null) {
				IJavaScriptElement element= JavaScriptCore.create(resource);
				if (element != null) {
					IJavaScriptProject javaProject= element.getJavaScriptProject();
					if (javaProject != null)
						compilerOptions= javaProject.getOptions(true);
				}
			}
		}
		if (compilerOptions == null)
			compilerOptions= fDefaultCompilerOptions;
		
		if (doc != null) {
			boolean isEditable= false;
			if (input instanceof IEditableContent)
				isEditable= ((IEditableContent) input).isEditable();
			
			// we hook into the root node to intercept all node changes
			JavaNode root= new RootJavaNode(doc, isEditable, input, adapter);
			
			if (buffer == null) {
				contents= doc.get();
				int n= contents.length();
				buffer= new char[n];
				contents.getChars(0, n, buffer, 0);
			}
						
			ASTParser parser= ASTParser.newParser(AST.JLS3);
			if (compilerOptions != null)
				parser.setCompilerOptions(compilerOptions);
			parser.setSource(buffer);
			parser.setFocalPosition(0);
			JavaScriptUnit cu= (JavaScriptUnit) parser.createAST(monitor);
			cu.accept(new JavaParseTreeBuilder(root, buffer, true));
			
			return root;
		}
		return null;
	}
	
	/**
	 * Returns the contents of the given node as a string.
	 * This string is used to test the content of a Java element
	 * for equality. Is is never shown in the UI, so any string representing
	 * the content will do.
	 * @param node must implement the IStreamContentAccessor interface
	 * @param ignoreWhiteSpace if true all Java white space (including comments) is removed from the contents.
	 * @return contents for equality test
	 */
	public String getContents(Object node, boolean ignoreWhiteSpace) {
		
		if (! (node instanceof IStreamContentAccessor))
			return null;
			
		IStreamContentAccessor sca= (IStreamContentAccessor) node;
		String content= null;
		try {
			content= JavaCompareUtilities.readString(sca);
		} catch (CoreException ex) {
			JavaScriptPlugin.log(ex);
			return null;
		}
				
		if (ignoreWhiteSpace) { 	// we return everything but Java whitespace
			
			// replace comments and whitespace by a single blank
			StringBuffer buf= new StringBuffer();
			char[] b= content.toCharArray();
			
			// to avoid the trouble when dealing with Unicode
			// we use the Java scanner to extract non-whitespace and non-comment tokens
			IScanner scanner= ToolFactory.createScanner(true, true, false, false);	// however we request Whitespace and Comments
			scanner.setSource(b);
			try {
				int token;
				while ((token= scanner.getNextToken()) != ITerminalSymbols.TokenNameEOF) {
					switch (token) {
					case ITerminalSymbols.TokenNameWHITESPACE:						
						int l= buf.length();
						if (l > 0 && buf.charAt(l-1) != ' ')
							buf.append(' ');
						break;
					default:
						buf.append(scanner.getCurrentTokenSource());
						buf.append(' ');
						break;
					}
				}
				content= buf.toString();	// success!
			} catch (InvalidInputException ex) {
				// NeedWork
			}
		}
		return content;
	}
	
	/**
	 * @return true since this IStructureCreator can rewrite the diff tree
	 * in order to fold certain combinations of additions and deletions.
	 */
	public boolean canRewriteTree() {
		return true;
	}
	
	/**
	 * Tries to detect certain combinations of additions and deletions
	 * as renames or signature changes and folders them into a single node.
	 * @param differencer 
	 * @param root 
	 */
	public void rewriteTree(Differencer differencer, IDiffContainer root) {
		
		HashMap map= new HashMap(10);
				
		Object[] children= root.getChildren();
		for (int i= 0; i < children.length; i++) {
			DiffNode diff= (DiffNode) children[i];
			JavaNode jn= (JavaNode) diff.getId();
			
			if (jn == null)
				continue;
			int type= jn.getTypeCode();
			
			// we can only combine methods or constructors
			if (type == JavaNode.METHOD || type == JavaNode.CONSTRUCTOR) {
				
				// find or create a RewriteInfo for all methods with the same name
				String name= jn.extractMethodName();
				RewriteInfo nameInfo= (RewriteInfo) map.get(name);
				if (nameInfo == null) {
					nameInfo= new RewriteInfo();
					map.put(name, nameInfo);
				}
				nameInfo.add(diff);
				
				// find or create a RewriteInfo for all methods with the same
				// (non-empty) argument list
				String argList= jn.extractArgumentList();
				RewriteInfo argInfo= null;
				if (argList != null && !argList.equals("()")) { //$NON-NLS-1$
					argInfo= (RewriteInfo) map.get(argList);
					if (argInfo == null) {
						argInfo= new RewriteInfo();
						map.put(argList, argInfo);
					}
					argInfo.add(diff);
				}
				
				switch (diff.getKind() & Differencer.CHANGE_TYPE_MASK) {
				case Differencer.ADDITION:
				case Differencer.DELETION:
					// we only consider addition and deletions
					// since a rename or argument list change looks
					// like a pair of addition and deletions
					if (type != JavaNode.CONSTRUCTOR)
						nameInfo.setDiff(diff);
					
					if (argInfo != null)
						argInfo.setDiff(diff);
					break;
				default:
					break;
				}
			}
			
			// recurse
			rewriteTree(differencer, diff);
		}
		
		// now we have to rebuild the diff tree according to the combined
		// changes
		Iterator it= map.keySet().iterator();
		while (it.hasNext()) {
			String name= (String) it.next();
			RewriteInfo i= (RewriteInfo) map.get(name);
			if (i.matches()) { // we found a RewriteInfo that could be successfully combined
				
				// we have to find the differences of the newly combined node
				// (because in the first pass we only got a deletion and an addition)
				DiffNode d= (DiffNode) differencer.findDifferences(true, null, root, i.fAncestor, i.fLeft, i.fRight);
				if (d != null) {// there better should be a difference
					d.setDontExpand(true);
					Iterator it2= i.fChildren.iterator();
					while (it2.hasNext()) {
						IDiffElement rd= (IDiffElement) it2.next();
						root.removeToRoot(rd);
						d.add(rd);
					}
				}
			}
		}
	}

	/**
	 * The JavaHistoryAction uses this function to determine whether
	 * a selected Java element can be replaced by some piece of
	 * code from the local history.
	 * @param je Java element
	 * @return true if the given IJavaScriptElement maps to a JavaNode
	 */
	static boolean hasEdition(IJavaScriptElement je) {
		return JavaElementHistoryPageSource.hasEdition(je);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.StructureCreator#getDocumentPartitioner()
	 */
	protected IDocumentPartitioner getDocumentPartitioner() {
		return JavaCompareUtilities.createJavaPartitioner();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.StructureCreator#getDocumentPartitioning()
	 */
	protected String getDocumentPartitioning() {
		return IJavaScriptPartitions.JAVA_PARTITIONING;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.StructureCreator#getPath(java.lang.Object, java.lang.Object)
	 */
	protected String[] getPath(Object element, Object input) {
		if (element instanceof IJavaScriptElement) {
			IJavaScriptElement je = (IJavaScriptElement) element;
			// build a path starting at the given Java element and walk
			// up the parent chain until we reach a IWorkingCopy or IJavaScriptUnit
			List args= new ArrayList();
			while (je != null) {
				// each path component has a name that uses the same
				// conventions as a JavaNode name
				String name= JavaCompareUtilities.getJavaElementID(je);
				if (name == null)
					return null;
				args.add(name);
				if (je instanceof IJavaScriptUnit)
					break;
				je= je.getParent();
			}
			
			// revert the path
			int n= args.size();
			String[] path= new String[n];
			for (int i= 0; i < n; i++)
				path[i]= (String) args.get(n-1-i);
				
			return path;
		}
		return null;
	}
}
