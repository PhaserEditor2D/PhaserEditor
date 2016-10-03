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
import java.util.Iterator;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.compare.JavaTokenComparator.ITokenComparatorFactory;
import org.eclipse.wst.jsdt.internal.ui.text.PreferencesAdapter;
import org.eclipse.wst.jsdt.ui.text.IJavaScriptColorConstants;
import org.eclipse.wst.jsdt.ui.text.IJavaScriptPartitions;
import org.eclipse.wst.jsdt.ui.text.JavaScriptSourceViewerConfiguration;
import org.eclipse.wst.jsdt.ui.text.JavaScriptTextTools;


public class JavaMergeViewer extends TextMergeViewer {
	
	private IPropertyChangeListener fPreferenceChangeListener;
	private IPreferenceStore fPreferenceStore;
	private boolean fUseSystemColors;
	private JavaScriptSourceViewerConfiguration fSourceViewerConfiguration;
	private ArrayList fSourceViewer;
	
		
	public JavaMergeViewer(Composite parent, int styles, CompareConfiguration mp) {
		super(parent, styles | SWT.LEFT_TO_RIGHT, mp);
				
		getPreferenceStore();
		
		fUseSystemColors= fPreferenceStore.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT);
		if (! fUseSystemColors) {
			RGB bg= createColor(fPreferenceStore, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
			setBackgroundColor(bg);
			RGB fg= createColor(fPreferenceStore, IJavaScriptColorConstants.JAVA_DEFAULT);
			setForegroundColor(fg);
		}
	}
	
	private IPreferenceStore getPreferenceStore() {
		if (fPreferenceStore == null)
			setPreferenceStore(createChainedPreferenceStore(null));
		return fPreferenceStore;
	}
	
	protected void handleDispose(DisposeEvent event) {
		setPreferenceStore(null);
		fSourceViewer= null;
		super.handleDispose(event);
	}
	
	public IJavaScriptProject getJavaProject(ICompareInput input) {
		
		if (input == null)
			return null;
		
		IResourceProvider rp= null;
		ITypedElement te= input.getLeft();
		if (te instanceof IResourceProvider)
			rp= (IResourceProvider) te;
		if (rp == null) {
			te= input.getRight();
			if (te instanceof IResourceProvider)
				rp= (IResourceProvider) te;
		}
		if (rp == null) {
			te= input.getAncestor();
			if (te instanceof IResourceProvider)
				rp= (IResourceProvider) te;
		}
		if (rp != null) {
			IResource resource= rp.getResource();
			if (resource != null) {
				IJavaScriptElement element= JavaScriptCore.create(resource);
				if (element != null)
					return element.getJavaScriptProject();
			}
		}
		return null;
	}

    public void setInput(Object input) {
    	
    	if (input instanceof ICompareInput) {    		
    		IJavaScriptProject project= getJavaProject((ICompareInput)input);
			if (project != null) {
				setPreferenceStore(createChainedPreferenceStore(project));
				if (fSourceViewer != null) {
					Iterator iterator= fSourceViewer.iterator();
					while (iterator.hasNext()) {
						SourceViewer sourceViewer= (SourceViewer) iterator.next();
						sourceViewer.unconfigure();
						sourceViewer.configure(getSourceViewerConfiguration());
					}
				}
			}
    	}
    		
    	super.setInput(input);
    }
    
    private ChainedPreferenceStore createChainedPreferenceStore(IJavaScriptProject project) {
    	ArrayList stores= new ArrayList(4);
    	if (project != null)
    		stores.add(new EclipsePreferencesAdapter(new ProjectScope(project.getProject()), JavaScriptCore.PLUGIN_ID));
		stores.add(JavaScriptPlugin.getDefault().getPreferenceStore());
		stores.add(new PreferencesAdapter(JavaScriptCore.getPlugin().getPluginPreferences()));
		stores.add(EditorsUI.getPreferenceStore());
		return new ChainedPreferenceStore((IPreferenceStore[]) stores.toArray(new IPreferenceStore[stores.size()]));
    }

	private void handlePropertyChange(PropertyChangeEvent event) {
		
		String key= event.getProperty();
		
		if (key.equals(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND)) {

			if (!fUseSystemColors) {
				RGB bg= createColor(fPreferenceStore, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
				setBackgroundColor(bg);
			}
						
		} else if (key.equals(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT)) {

			fUseSystemColors= fPreferenceStore.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT);
			if (fUseSystemColors) {
				setBackgroundColor(null);
				setForegroundColor(null);
			} else {
				RGB bg= createColor(fPreferenceStore, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
				setBackgroundColor(bg);
				RGB fg= createColor(fPreferenceStore, IJavaScriptColorConstants.JAVA_DEFAULT);
				setForegroundColor(fg);
			}
		} else if (key.equals(IJavaScriptColorConstants.JAVA_DEFAULT)) {

			if (!fUseSystemColors) {
				RGB fg= createColor(fPreferenceStore, IJavaScriptColorConstants.JAVA_DEFAULT);
				setForegroundColor(fg);
			}
		}
		
		if (fSourceViewerConfiguration != null && fSourceViewerConfiguration.affectsTextPresentation(event)) {
			fSourceViewerConfiguration.handlePropertyChangeEvent(event);
			invalidateTextPresentation();
		}
	}
	
	/**
	 * Creates a color from the information stored in the given preference store.
	 * Returns <code>null</code> if there is no such information available.
	 * @param store preference store
	 * @param key preference key
	 * @return the color or <code>null</code>
	 */
	private static RGB createColor(IPreferenceStore store, String key) {
		if (!store.contains(key))
			return null;
		if (store.isDefault(key))
			return PreferenceConverter.getDefaultColor(store, key);
		return PreferenceConverter.getColor(store, key);
	}
	
	public String getTitle() {
		return CompareMessages.JavaMergeViewer_title; 
	}

	public ITokenComparator createTokenComparator(String s) {
		return new JavaTokenComparator(s, new ITokenComparatorFactory() {
			public ITokenComparator createTokenComparator(String text) {
				return JavaMergeViewer.super.createTokenComparator(text);
			}
		});
	}
	
	protected IDocumentPartitioner getDocumentPartitioner() {
		return JavaCompareUtilities.createJavaPartitioner();
	}
	
	protected String getDocumentPartitioning() {
		return IJavaScriptPartitions.JAVA_PARTITIONING;
	}
		
	protected void configureTextViewer(TextViewer textViewer) {
		if (textViewer instanceof SourceViewer) {
			if (fSourceViewer == null)
				fSourceViewer= new ArrayList();
			fSourceViewer.add(textViewer);
			JavaScriptTextTools tools= JavaCompareUtilities.getJavaTextTools();
			if (tools != null)
				((SourceViewer)textViewer).configure(getSourceViewerConfiguration());
		}
	}
	
	private JavaScriptSourceViewerConfiguration getSourceViewerConfiguration() {
		if (fSourceViewerConfiguration == null)
			getPreferenceStore();
		return fSourceViewerConfiguration;
	}
	
	protected int findInsertionPosition(char type, ICompareInput input) {
		
		int pos= super.findInsertionPosition(type, input);
		if (pos != 0)
			return pos;
		
		if (input instanceof IDiffElement) {
			
			// find the other (not deleted) element
			JavaNode otherJavaElement= null;
			ITypedElement otherElement= null;
			switch (type) {
			case 'L':
				otherElement= input.getRight();
				break;
			case 'R':
				otherElement= input.getLeft();
				break;
			}
			if (otherElement instanceof JavaNode)
				otherJavaElement= (JavaNode) otherElement;
			
			// find the parent of the deleted elements
			JavaNode javaContainer= null;
			IDiffElement diffElement= (IDiffElement) input;
			IDiffContainer container= diffElement.getParent();
			if (container instanceof ICompareInput) {
				
				ICompareInput parent= (ICompareInput) container;
				ITypedElement element= null;
				
				switch (type) {
				case 'L':
					element= parent.getLeft();
					break;
				case 'R':
					element= parent.getRight();
					break;
				}
				
				if (element instanceof JavaNode)
					javaContainer= (JavaNode) element;
			}
			
			if (otherJavaElement != null && javaContainer != null) {
				
				Object[] children;
				Position p;
				
				switch (otherJavaElement.getTypeCode()) {
				
				case JavaNode.PACKAGE:
					return 0;

				case JavaNode.IMPORT_CONTAINER:
					// we have to find the place after the package declaration
					children= javaContainer.getChildren();
					if (children.length > 0) {
						JavaNode packageDecl= null;
						for (int i= 0; i < children.length; i++) {
							JavaNode child= (JavaNode) children[i];
							switch (child.getTypeCode()) {
							case JavaNode.PACKAGE:
								packageDecl= child;
								break;
							case JavaNode.CLASS:
								return child.getRange().getOffset();
							}
						}
						if (packageDecl != null) {
							p= packageDecl.getRange();
							return p.getOffset() + p.getLength();
						}
					}
					return javaContainer.getRange().getOffset();
				
				case JavaNode.IMPORT:
					// append after last import
					p= javaContainer.getRange();
					return p.getOffset() + p.getLength();
				
				case JavaNode.CLASS:
					// append after last class
					children= javaContainer.getChildren();
					if (children.length > 0) {
						for (int i= children.length-1; i >= 0; i--) {
							JavaNode child= (JavaNode) children[i];
							switch (child.getTypeCode()) {
							case JavaNode.CLASS:
							case JavaNode.IMPORT_CONTAINER:
							case JavaNode.PACKAGE:
							case JavaNode.FIELD:
								p= child.getRange();
								return p.getOffset() + p.getLength();
							}
						}					
					}
					return javaContainer.getAppendPosition().getOffset();
					
				case JavaNode.METHOD:
					// append in next line after last child
					children= javaContainer.getChildren();
					if (children.length > 0) {
						JavaNode child= (JavaNode) children[children.length-1];
						p= child.getRange();
						return findEndOfLine(javaContainer, p.getOffset() + p.getLength());
					}
					// otherwise use position from parser
					return javaContainer.getAppendPosition().getOffset();
					
				case JavaNode.FIELD:
					// append after last field
					children= javaContainer.getChildren();
					if (children.length > 0) {
						JavaNode method= null;
						for (int i= children.length-1; i >= 0; i--) {
							JavaNode child= (JavaNode) children[i];
							switch (child.getTypeCode()) {
							case JavaNode.METHOD:
								method= child;
								break;
							case JavaNode.FIELD:
								p= child.getRange();
								return p.getOffset() + p.getLength();
							}
						}
						if (method != null)
							return method.getRange().getOffset();
					}
					return javaContainer.getAppendPosition().getOffset();
				}
			}
			
			if (javaContainer != null) {
				// return end of container
				Position p= javaContainer.getRange();
				return p.getOffset() + p.getLength();
			}
		}

		// we give up
		return 0;
	}
	
	private int findEndOfLine(JavaNode container, int pos) {
		int line;
		IDocument doc= container.getDocument();
		try {
			line= doc.getLineOfOffset(pos);
			pos= doc.getLineOffset(line+1);
		} catch (BadLocationException ex) {
			// silently ignored
		}
		
		// ensure that position is within container range
		Position containerRange= container.getRange();
		int start= containerRange.getOffset();
		int end= containerRange.getOffset() + containerRange.getLength();
		if (pos < start)
			return start;
		if (pos >= end)
			return end-1;
		
		return pos;
	}

	private void setPreferenceStore(IPreferenceStore ps) {
		if (fPreferenceChangeListener != null) {
			if (fPreferenceStore != null)
				fPreferenceStore.removePropertyChangeListener(fPreferenceChangeListener);
			fPreferenceChangeListener= null;
		}
		fPreferenceStore= ps;
		if (fPreferenceStore != null) {
			JavaScriptTextTools tools= JavaCompareUtilities.getJavaTextTools();
			fSourceViewerConfiguration= new JavaScriptSourceViewerConfiguration(tools.getColorManager(), fPreferenceStore, null, getDocumentPartitioning());
			fPreferenceChangeListener= new IPropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
					handlePropertyChange(event);
				}
			};
			fPreferenceStore.addPropertyChangeListener(fPreferenceChangeListener);
		}
	}
}
