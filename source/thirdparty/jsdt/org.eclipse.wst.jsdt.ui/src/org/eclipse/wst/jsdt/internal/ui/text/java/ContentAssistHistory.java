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
package org.eclipse.wst.jsdt.internal.ui.text.java;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.wst.jsdt.core.Flags;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeHierarchy;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.JavaUIException;
import org.eclipse.wst.jsdt.internal.ui.JavaUIStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * An LRU cache for code assist.
 * 
 * 
 */
public final class ContentAssistHistory {
	/**
	 * Persistence implementation.
	 * 
	 * 
	 */
	private static final class ReaderWriter {

		private static final String NODE_ROOT= "history"; //$NON-NLS-1$
		private static final String NODE_LHS= "lhs"; //$NON-NLS-1$
		private static final String NODE_RHS= "rhs"; //$NON-NLS-1$
		private static final String ATTRIBUTE_NAME= "name"; //$NON-NLS-1$
		private static final String ATTRIBUTE_MAX_LHS= "maxLHS"; //$NON-NLS-1$
		private static final String ATTRIBUTE_MAX_RHS= "maxRHS"; //$NON-NLS-1$

		public void store(ContentAssistHistory history, StreamResult result) throws CoreException {
			try {
				DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
				DocumentBuilder builder= factory.newDocumentBuilder();		
				Document document= builder.newDocument();
				
				Element rootElement = document.createElement(NODE_ROOT);
				rootElement.setAttribute(ATTRIBUTE_MAX_LHS, Integer.toString(history.fMaxLHS));
				rootElement.setAttribute(ATTRIBUTE_MAX_RHS, Integer.toString(history.fMaxRHS));
				document.appendChild(rootElement);
		
				for (Iterator leftHandSides= history.fLHSCache.keySet().iterator(); leftHandSides.hasNext();) {
					String lhs= (String) leftHandSides.next();
					Element lhsElement= document.createElement(NODE_LHS);
					lhsElement.setAttribute(ATTRIBUTE_NAME, lhs);
					rootElement.appendChild(lhsElement);
					
					Set rightHandSides= (Set) history.fLHSCache.get(lhs);
					for (Iterator rhsIterator= rightHandSides.iterator(); rhsIterator.hasNext();) {
						String rhs= (String) rhsIterator.next();
						Element rhsElement= document.createElement(NODE_RHS);
						rhsElement.setAttribute(ATTRIBUTE_NAME, rhs);
						lhsElement.appendChild(rhsElement);
					}
				}
				
				Transformer transformer=TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
				transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
				transformer.setOutputProperty(OutputKeys.INDENT, "no"); //$NON-NLS-1$
				DOMSource source = new DOMSource(document);

				transformer.transform(source, result);
			} catch (TransformerException e) {
				throw createException(e, JavaTextMessages.ContentAssistHistory_serialize_error);
			} catch (ParserConfigurationException e) {
				throw createException(e, JavaTextMessages.ContentAssistHistory_serialize_error);
			}
		}
		
		public ContentAssistHistory load(InputSource source) throws CoreException {
			Element root;
			try {
				DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				root = parser.parse(source).getDocumentElement();
			} catch (SAXException e) {
				throw createException(e, JavaTextMessages.ContentAssistHistory_deserialize_error);  
			} catch (ParserConfigurationException e) {
				throw createException(e, JavaTextMessages.ContentAssistHistory_deserialize_error); 
			} catch (IOException e) {
				throw createException(e, JavaTextMessages.ContentAssistHistory_deserialize_error); 
			}
			
			if (root == null || !root.getNodeName().equalsIgnoreCase(NODE_ROOT))
				return null;
			
			int maxLHS= parseNaturalInt(root.getAttribute(ATTRIBUTE_MAX_LHS), DEFAULT_TRACKED_LHS);
			int maxRHS= parseNaturalInt(root.getAttribute(ATTRIBUTE_MAX_RHS), DEFAULT_TRACKED_RHS);
			
			ContentAssistHistory history= new ContentAssistHistory(maxLHS, maxRHS);
			
			NodeList list= root.getChildNodes();
			int length= list.getLength();
			for (int i= 0; i < length; ++i) {
				Node lhsNode= list.item(i);
				if (lhsNode.getNodeType() == Node.ELEMENT_NODE) {
					Element lhsElement= (Element) lhsNode;
					if (lhsElement.getNodeName().equalsIgnoreCase(NODE_LHS)) {
						String lhs= lhsElement.getAttribute(ATTRIBUTE_NAME);
						if (lhs != null) {
							Set cache= history.getCache(lhs);
							NodeList children= lhsElement.getChildNodes();
							int nRHS= children.getLength();
							for (int j= 0; j < nRHS; j++) {
								Node rhsNode= children.item(j);
								if (rhsNode.getNodeType() == Node.ELEMENT_NODE) {
									Element rhsElement= (Element) rhsNode;
									if (rhsElement.getNodeName().equalsIgnoreCase(NODE_RHS)) {
										String rhs= rhsElement.getAttribute(ATTRIBUTE_NAME);
										if (rhs != null) {
											cache.add(rhs);
										}
									}
								}
							}
						}
					}
				}
			}
			
			return history;
		}

		private int parseNaturalInt(String attribute, int defaultValue) {
			try {
				int integer= Integer.parseInt(attribute);
				if (integer > 0)
					return integer;
				return defaultValue;
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}

		private JavaUIException createException(Exception e, String message) {
			return new JavaUIException(JavaUIStatus.createError(IStatus.ERROR, message, e));
		}
	}

	/**
	 * Most recently used variant with capped size that only counts
	 * {@linkplain #put(Object, Object) put} as access. This is implemented by always removing an
	 * element before it gets put back.
	 * 
	 * 
	 */
	private static final class MRUMap extends LinkedHashMap {
		private static final long serialVersionUID= 1L;
		private final int fMaxSize;
		
		/**
		 * Creates a new <code>MRUMap</code> with the given size.
		 * 
		 * @param maxSize the maximum size of the cache, must be &gt; 0
		 */
		public MRUMap(int maxSize) {
			Assert.isLegal(maxSize > 0);
			fMaxSize= maxSize;
		}
		
		/*
		 * @see java.util.HashMap#put(java.lang.Object, java.lang.Object)
		 */
		public Object put(Object key, Object value) {
			Object object= remove(key);
			super.put(key, value);
			return object;
		}
		
		/*
		 * @see java.util.LinkedHashMap#removeEldestEntry(java.util.Map.Entry)
		 */
		protected boolean removeEldestEntry(Entry eldest) {
			return size() > fMaxSize;
		}
	}
	
	/**
	 * Most recently used variant with capped size that orders the elements by addition.
	 * This is implemented by always removing an element before it gets added back.
	 * 
	 * 
	 */
	private static final class MRUSet extends LinkedHashSet {
		private static final long serialVersionUID= 1L;
		private final int fMaxSize;
		
		/**
		 * Creates a new <code>MRUSet</code> with the given size.
		 * 
		 * @param maxSize the maximum size of the cache, must be &gt; 0
		 */
		public MRUSet(int maxSize) {
			Assert.isLegal(maxSize > 0);
			fMaxSize= maxSize;
		}
		
		/*
		 * @see java.util.HashSet#add(java.lang.Object)
		 */
		public boolean add(Object o) {
			if (remove(o)) {
				super.add(o);
				return false;
			}
				
			if (size() >= fMaxSize)
				remove(this.iterator().next());
			
			super.add(o);
			return true;
		}
	}
	
	/**
	 * A ranking of the most recently selected types.
	 * 
	 * 
	 */
	public static final class RHSHistory {
		private final LinkedHashMap fHistory;
		private List fList;
		
		RHSHistory(LinkedHashMap history) {
			fHistory= history;
		}
		
		/**
		 * Returns the rank of a type in the history in [0.0,&nbsp;1.0]. The rank of the most
		 * recently selected type is 1.0, the rank of any type that is not remembered is zero.
		 * 
		 * @param type the fully qualified type name to get the rank for
		 * @return the rank of <code>type</code>
		 */
		public float getRank(String type) {
			if (fHistory == null)
				return 0.0F;
			Integer integer= (Integer) fHistory.get(type);
			return integer == null ? 0.0F : integer.floatValue() / fHistory.size();
		}
		
		/**
		 * Returns the size of the history.
		 * 
		 * @return the size of the history
		 */
		public int size() {
			return fHistory == null ? 0 : fHistory.size();
		}
		
		/**
		 * Returns the list of remembered types ordered by recency. The first element is the
		 * <i>least</i>, the last element the <i>most</i> recently remembered type.
		 * 
		 * @return the list of remembered types as fully qualified type names (element type:
		 *         {@link String})
		 */
		public List getTypes() {
			if (fHistory == null)
				return Collections.EMPTY_LIST;
			if (fList == null) {
				fList= Collections.unmodifiableList(new ArrayList(fHistory.keySet()));
			}
			return fList;
		}
	}
	
	private static final RHSHistory EMPTY_HISTORY= new RHSHistory(null);
	private static final int DEFAULT_TRACKED_LHS= 100;
	private static final int DEFAULT_TRACKED_RHS= 10;
	
	private static final Set UNCACHEABLE;
	static {
		Set uncacheable= new HashSet();
		uncacheable.add("java.lang.Object"); //$NON-NLS-1$
		uncacheable.add("java.lang.Comparable"); //$NON-NLS-1$
		uncacheable.add("java.io.Serializable"); //$NON-NLS-1$
		uncacheable.add("java.io.Externalizable"); //$NON-NLS-1$
		UNCACHEABLE= Collections.unmodifiableSet(uncacheable);
	}
	
	private final LinkedHashMap/*<IType, MRUSet<IType>>*/ fLHSCache;
	private final int fMaxLHS;
	private final int fMaxRHS;
	
	/**
	 * Creates a new history.
	 * 
	 * @param maxLHS the maximum number of tracked left hand sides (&gt; 0)
	 * @param maxRHS the maximum number of tracked right hand sides per left hand side(&gt; 0)
	 */
	public ContentAssistHistory(int maxLHS, int maxRHS) {
		Assert.isLegal(maxLHS > 0);
		Assert.isLegal(maxRHS > 0);
		fMaxLHS= maxLHS;
		fMaxRHS= maxRHS;
		fLHSCache= new MRUMap(fMaxLHS);
	}
	
	/**
	 * Creates a new history, equivalent to
	 * <code>ContentAssistHistory(DEFAULT_TRACKED_LHS, DEFAULT_TRACKED_RHS})</code>.
	 */
	public ContentAssistHistory() {
		this(DEFAULT_TRACKED_LHS, DEFAULT_TRACKED_RHS);
	}
	
	/**
	 * Remembers the selection of a right hand side type (proposal type) for a certain left hand side (expected
	 * type) in content assist.
	 * 
	 * @param lhs the left hand side / expected type
	 * @param rhs the selected right hand side
	 */
	public void remember(IType lhs, IType rhs) {
		Assert.isLegal(lhs != null);
		Assert.isLegal(rhs != null);
		
		try {
			if (!isCacheableRHS(rhs))
				return;
			ITypeHierarchy hierarchy= rhs.newSupertypeHierarchy(getProgressMonitor());
			if (hierarchy.contains(lhs)) {
				// TODO remember for every member of the LHS hierarchy or not? Yes for now.
				IType[] allLHSides= hierarchy.getAllSuperclasses(lhs);
				for (int i= 0; i < allLHSides.length; i++)
					rememberInternal(allLHSides[i], rhs);
				rememberInternal(lhs, rhs);
			}
		} catch (JavaScriptModelException x) {
			JavaScriptPlugin.log(x);
		}
	}
	
	/**
	 * Returns the {@link RHSHistory history} of the types that have been selected most recently as
	 * right hand sides for the given type.
	 * 
	 * @param lhs the fully qualified type name of an expected type for which right hand sides are
	 *        requested, or <code>null</code>
	 * @return the right hand side history for the given type
	 */
	public RHSHistory getHistory(String lhs) {
		MRUSet rhsCache= (MRUSet) fLHSCache.get(lhs);
		if (rhsCache != null) {
			int count= rhsCache.size();
			LinkedHashMap history= new LinkedHashMap((int) (count / 0.75));
			int rank= 1;
			for (Iterator it= rhsCache.iterator(); it.hasNext(); rank++) {
				String type= (String) it.next();
				history.put(type, Integer.valueOf(rank));
			}
			return new RHSHistory(history);
		}
		return EMPTY_HISTORY;
	}
	
	/**
	 * Returns a read-only map from {@link IType} to {@link RHSHistory}, where each value is the
	 * history for the key type (see {@link #getHistory(String)}.
	 * 
	 * @return the set of remembered right hand sides ordered by least recent selection
	 */
	public Map getEntireHistory() {
		HashMap map= new HashMap((int) (fLHSCache.size() / 0.75));
		for (Iterator it= fLHSCache.entrySet().iterator(); it.hasNext();) {
			Entry entry= (Entry) it.next();
			String lhs= (String) entry.getKey();
			map.put(lhs, getHistory(lhs));
		}
		return Collections.unmodifiableMap(map);
	}
	
	private void rememberInternal(IType lhs, IType rhs) throws JavaScriptModelException {
		if (isCacheableLHS(lhs))
			getCache(lhs.getFullyQualifiedName()).add(rhs.getFullyQualifiedName());
	}

	private boolean isCacheableLHS(IType type) throws JavaScriptModelException {
		return !UNCACHEABLE.contains(type.getFullyQualifiedName());
	}
	
	private boolean isCacheableRHS(IType type) throws JavaScriptModelException {
		return !Flags.isAbstract(type.getFlags());
	}

	private Set getCache(String lhs) {
		MRUSet rhsCache= (MRUSet) fLHSCache.get(lhs);
		if (rhsCache == null) {
			rhsCache= new MRUSet(fMaxRHS);
			fLHSCache.put(lhs, rhsCache);
		}
		
		return rhsCache;
	}

	private IProgressMonitor getProgressMonitor() {
		return new NullProgressMonitor();
	}
	
	/**
	 * Stores the history as XML document into the given preferences.
	 * 
	 * @param history the history to store
	 * @param preferences the preferences to store the history into
	 * @param key the key under which to store the history
	 * @throws CoreException if serialization fails
	 * @see #load(Preferences, String) on how to restore a history stored by this method
	 */
	public static void store(ContentAssistHistory history, Preferences preferences, String key) throws CoreException {
		StringWriter writer= new StringWriter();
		new ReaderWriter().store(history, new StreamResult(writer));
		preferences.setValue(key, writer.toString());
	}
	
	/**
	 * Loads a history from an XML encoded preference value.
	 * 
	 * @param preferences the preferences to retrieve the history from
	 * @param key the key under which the history is stored
	 * @return the deserialized history, or <code>null</code> if there is nothing stored under the
	 *         given key
	 * @throws CoreException if deserialization fails
	 * @see #store(ContentAssistHistory, Preferences, String) on how to store a history such that it
	 *      can be read by this method
	 */
	public static ContentAssistHistory load(Preferences preferences, String key) throws CoreException {
		String value= preferences.getString(key);
		if (value != null && value.length() > 0) {
			return new ReaderWriter().load(new InputSource(new StringReader(value)));
		}
		return null;
	}
}
