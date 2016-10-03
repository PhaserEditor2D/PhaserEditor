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
package org.eclipse.wst.jsdt.internal.corext.refactoring.nls;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.Signature;
import org.eclipse.wst.jsdt.core.compiler.InvalidInputException;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ITypeBinding;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.ASTProvider;

/**
 * calculates hints for the nls-refactoring out of a compilation unit.
 * - package fragments of the accessor class and the resource bundle
 * - accessor class name, resource bundle name
 */
public class NLSHint {
	
	private String fAccessorName;
	private IPackageFragment fAccessorPackage;
	private String fResourceBundleName;
	private IPackageFragment fResourceBundlePackage;
	private NLSSubstitution[] fSubstitutions;

	public NLSHint(IJavaScriptUnit cu, JavaScriptUnit astRoot) {
		Assert.isNotNull(cu);
		Assert.isNotNull(astRoot);
		
		IPackageFragment cuPackage= (IPackageFragment) cu.getAncestor(IJavaScriptElement.PACKAGE_FRAGMENT);

		fAccessorName= NLSRefactoring.DEFAULT_ACCESSOR_CLASSNAME;
		fAccessorPackage= cuPackage;
		fResourceBundleName= NLSRefactoring.DEFAULT_PROPERTY_FILENAME + NLSRefactoring.PROPERTY_FILE_EXT;
		fResourceBundlePackage= cuPackage;
		
		IJavaScriptProject project= cu.getJavaScriptProject();
		NLSLine[] lines= createRawLines(cu);
		
		AccessorClassReference accessClassRef= findFirstAccessorReference(lines, astRoot);
		
		if (accessClassRef == null) {
			// Look for Eclipse NLS approach
			List eclipseNLSLines= new ArrayList();
			accessClassRef= createEclipseNLSLines(getDocument(cu), astRoot, eclipseNLSLines);
			if (!eclipseNLSLines.isEmpty()) {
				NLSLine[] rawLines= lines;
				int rawLinesLength= rawLines.length;
				int eclipseLinesLength= eclipseNLSLines.size();
				lines= new NLSLine[rawLinesLength + eclipseLinesLength];
				for (int i= 0; i < rawLinesLength; i++)
					lines[i]= rawLines[i];
				for (int i= 0; i < eclipseLinesLength; i++)
					lines[i+rawLinesLength]= (NLSLine)eclipseNLSLines.get(i);
			}
		}
		
		Properties props= null;
		if (accessClassRef != null)
			props= NLSHintHelper.getProperties(project, accessClassRef);
		
		if (props == null)
			props= new Properties();
		
		fSubstitutions= createSubstitutions(lines, props, astRoot);
		
		if (accessClassRef != null) {
			fAccessorName= accessClassRef.getName();
			ITypeBinding accessorClassBinding= accessClassRef.getBinding();
			
			try {
				IPackageFragment accessorPack= NLSHintHelper.getPackageOfAccessorClass(project, accessorClassBinding);
				if (accessorPack != null) {
					fAccessorPackage= accessorPack;
				}
				
				String fullBundleName= accessClassRef.getResourceBundleName();
				if (fullBundleName != null) {
					fResourceBundleName= Signature.getSimpleName(fullBundleName) + NLSRefactoring.PROPERTY_FILE_EXT;
					String packName= Signature.getQualifier(fullBundleName);
					
					IPackageFragment pack= NLSHintHelper.getResourceBundlePackage(project, packName, fResourceBundleName);
					if (pack != null) {
						fResourceBundlePackage= pack;
					}
				}
			} catch (JavaScriptModelException e) {
			}
		}
	}
	
	private AccessorClassReference createEclipseNLSLines(final IDocument document, JavaScriptUnit astRoot, List nlsLines) {
		
		final AccessorClassReference[] firstAccessor= new AccessorClassReference[1];
		final SortedMap lineToNLSLine= new TreeMap();
		
		astRoot.accept(new ASTVisitor() {
			
			private IJavaScriptUnit fCache_CU;
			private JavaScriptUnit fCache_AST;

			public boolean visit(QualifiedName node) {
				ITypeBinding type= node.getQualifier().resolveTypeBinding();
				if (type != null) {
					ITypeBinding superType= type.getSuperclass();
					if (superType != null && NLS.class.getName().equals(superType.getQualifiedName())) {
						Integer line;
						try {
							line = Integer.valueOf(document.getLineOfOffset(node.getStartPosition()));
						} catch (BadLocationException e) {
							return true; // ignore and continue
						}
						NLSLine nlsLine= (NLSLine)lineToNLSLine.get(line);
						if (nlsLine == null) {
							nlsLine=  new NLSLine(line.intValue());
							lineToNLSLine.put(line, nlsLine);
						}
						SimpleName name= node.getName();
						NLSElement element= new NLSElement(node.getName().getIdentifier(), name.getStartPosition(), 
				                name.getLength(), nlsLine.size() - 1, true);
						nlsLine.add(element);
						String bundleName;
						try {
							IJavaScriptUnit bundleCU= (IJavaScriptUnit)type.getJavaElement().getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT); 
							if (fCache_CU == null || !fCache_CU.equals(bundleCU) || fCache_AST == null) {
								fCache_CU= bundleCU;
								if (fCache_CU != null)
									fCache_AST=	JavaScriptPlugin.getDefault().getASTProvider().getAST(fCache_CU, ASTProvider.WAIT_YES, null);
								else
									fCache_AST= null;
							}
							bundleName = NLSHintHelper.getResourceBundleName(fCache_AST);
						} catch (JavaScriptModelException e) {
							return true; // ignore this accessor and continue
						}
						element.setAccessorClassReference(new AccessorClassReference(type, bundleName, new Region(node.getStartPosition(), node.getLength())));
						
						if (firstAccessor[0] == null)
							firstAccessor[0]= element.getAccessorClassReference();
						
					}
				}
				return true;
			}
		});
		
		nlsLines.addAll(lineToNLSLine.values());
		return firstAccessor[0];
	}
	
	private IDocument getDocument(IJavaScriptUnit cu) {
		IPath path= cu.getPath();
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(path, LocationKind.NORMALIZE, null);
		} catch (CoreException e) {
			return null;
		}
		
		try {
			ITextFileBuffer buffer= manager.getTextFileBuffer(path, LocationKind.NORMALIZE);
			if (buffer != null)
				return buffer.getDocument();
		} finally {
			try {
				manager.disconnect(path, LocationKind.NORMALIZE, null);
			} catch (CoreException e) {
				return null;
			}
		}
		return null;
	}

	private NLSSubstitution[] createSubstitutions(NLSLine[] lines, Properties props, JavaScriptUnit astRoot) {
		List result= new ArrayList();
		
		for (int i= 0; i < lines.length; i++) {
			NLSElement[] elements= lines[i].getElements();
			for (int j= 0; j < elements.length; j++) {
				NLSElement nlsElement= elements[j];
				if (nlsElement.hasTag()) {
					AccessorClassReference accessorClassReference= NLSHintHelper.getAccessorClassReference(astRoot, nlsElement);
					if (accessorClassReference == null) {
						// no accessor class => not translated				        
						result.add(new NLSSubstitution(NLSSubstitution.IGNORED, stripQuotes(nlsElement.getValue()), nlsElement));
					} else {
						String key= stripQuotes(nlsElement.getValue());
						String value= props.getProperty(key);
						result.add(new NLSSubstitution(NLSSubstitution.EXTERNALIZED, key, value, nlsElement, accessorClassReference));
					}
				} else if (nlsElement.isEclipseNLS()) {
					String key= nlsElement.getValue();
					result.add(new NLSSubstitution(NLSSubstitution.EXTERNALIZED, key, props.getProperty(key), nlsElement, nlsElement.getAccessorClassReference()));
				} else {
					result.add(new NLSSubstitution(NLSSubstitution.INTERNALIZED, stripQuotes(nlsElement.getValue()), nlsElement));
				}
			}
		}
		return (NLSSubstitution[]) result.toArray(new NLSSubstitution[result.size()]);
	}
	
	private static AccessorClassReference findFirstAccessorReference(NLSLine[] lines, JavaScriptUnit astRoot) {
		for (int i= 0; i < lines.length; i++) {
			NLSElement[] elements= lines[i].getElements();
			for (int j= 0; j < elements.length; j++) {
				NLSElement nlsElement= elements[j];
				if (nlsElement.hasTag()) {
					AccessorClassReference accessorClassReference= NLSHintHelper.getAccessorClassReference(astRoot, nlsElement);
					if (accessorClassReference != null) {
						return accessorClassReference;
					}
				}
			}
		}
		
		// try to find a access with missing //non-nls tag (bug 75155)
		for (int i= 0; i < lines.length; i++) {
			NLSElement[] elements= lines[i].getElements();
			for (int j= 0; j < elements.length; j++) {
				NLSElement nlsElement= elements[j];
				if (!nlsElement.hasTag()) {
					AccessorClassReference accessorClassReference= NLSHintHelper.getAccessorClassReference(astRoot, nlsElement);
					if (accessorClassReference != null) {
						return accessorClassReference;
					}
				}
			}
		}
		return null;
	}

	private static String stripQuotes(String str) {
		return str.substring(1, str.length() - 1);
	}

	private static NLSLine[] createRawLines(IJavaScriptUnit cu) {
		try {
			return NLSScanner.scan(cu);
		} catch (JavaScriptModelException x) {
			return new NLSLine[0];
		} catch (InvalidInputException x) {
			return new NLSLine[0];
		}
	}
	

	public String getAccessorClassName() {
		return fAccessorName;
	}
	
//	public boolean isEclipseNLS() {
//		return fIsEclipseNLS;
//	}

	public IPackageFragment getAccessorClassPackage() {
		return fAccessorPackage;
	}

	public String getResourceBundleName() {
		return fResourceBundleName;
	}

	public IPackageFragment getResourceBundlePackage() {
		return fResourceBundlePackage;
	}

	public NLSSubstitution[] getSubstitutions() {
		return fSubstitutions;
	}


}
