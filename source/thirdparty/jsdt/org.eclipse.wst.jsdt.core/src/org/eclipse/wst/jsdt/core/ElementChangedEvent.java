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
package org.eclipse.wst.jsdt.core;

import java.util.EventObject;

/**
 * An element changed event describes a change to the structure or contents
 * of a tree of JavaScript elements. The changes to the elements are described by
 * the associated delta object carried by this event.
 * <p>
 * This class is not intended to be instantiated or subclassed by clients.
 * Instances of this class are automatically created by the JavaScript model.
 * </p>
 *
 * @see IElementChangedListener
 * @see IJavaScriptElementDelta
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class ElementChangedEvent extends EventObject {

	/**
	 * Event type constant (bit mask) indicating an after-the-fact
	 * report of creations, deletions, and modifications
	 * to one or more JavaScript element(s) expressed as a hierarchical
	 * java element delta as returned by <code>getDelta()</code>.
	 *
	 * Note: this notification occurs during the corresponding POST_CHANGE
	 * resource change notification, and contains a full delta accounting for
	 * any JavaScriptModel operation  and/or resource change.
	 *
	 * @see IJavaScriptElementDelta
	 * @see org.eclipse.core.resources.IResourceChangeEvent
	 * @see #getDelta()
	 */
	public static final int POST_CHANGE = 1;

	/**
	 * Event type constant (bit mask) indicating an after-the-fact
	 * report of creations, deletions, and modifications
	 * to one or more JavaScript element(s) expressed as a hierarchical
	 * java element delta as returned by <code>getDelta</code>.
	 *
	 * Note: this notification occurs as a result of a working copy reconcile
	 * operation.
	 *
	 * @see IJavaScriptElementDelta
	 * @see org.eclipse.core.resources.IResourceChangeEvent
	 * @see #getDelta()
	 */
	public static final int 	POST_RECONCILE = 4;

	private static final long serialVersionUID = -8947240431612844420L; // backward compatible

	/*
	 * Event type indicating the nature of this event.
	 * It can be a combination either:
	 *  - POST_CHANGE
	 *  - PRE_AUTO_BUILD
	 *  - POST_RECONCILE
	 */
	private int type;

	/**
	 * Creates an new element changed event (based on a <code>IJavaScriptElementDelta</code>).
	 *
	 * @param delta the JavaScript element delta.
	 * @param type the type of delta (ADDED, REMOVED, CHANGED) this event contains
	 */
	public ElementChangedEvent(IJavaScriptElementDelta delta, int type) {
		super(delta);
		this.type = type;
	}
	/**
	 * Returns the delta describing the change.
	 *
	 * @return the delta describing the change
	 */
	public IJavaScriptElementDelta getDelta() {
		return (IJavaScriptElementDelta) this.source;
	}

	/**
	 * Returns the type of event being reported.
	 *
	 * @return one of the event type constants
	 * @see #POST_CHANGE
	 * @see #POST_RECONCILE
	 */
	public int getType() {
		return this.type;
	}
}
