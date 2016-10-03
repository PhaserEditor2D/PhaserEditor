/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.javaeditor.saveparticipant;

import org.eclipse.core.runtime.Assert;

/**
 * Describes a save participant contribution.
 *
 * 
 */
public class SaveParticipantDescriptor {

	/** The listener */
	private final IPostSaveListener fPostSaveListener;
	/** The preference configuration block, if any */
	private ISaveParticipantPreferenceConfiguration fPreferenceConfiguration;
	
	/**
	 * Creates a new descriptor which connects a {@link IPostSaveListener}
	 * with an {@link ISaveParticipantPreferenceConfiguration}.
	 * 
	 * @param listener the listener
	 */
	SaveParticipantDescriptor(IPostSaveListener listener) {
		Assert.isNotNull(listener);

		fPostSaveListener= listener;
	}

	/**
	 * Returns the post save listener of the described
	 * save participant
	 * 
	 * @return the listener
	 */
	public IPostSaveListener getPostSaveListener()  {
		return fPostSaveListener;
	}

	/**
	 * Creates a new preference configuration of the described
	 * save participant.
	 * 
	 * @return the preference configuration
	 */
	public ISaveParticipantPreferenceConfiguration createPreferenceConfiguration() {
		return new AbstractSaveParticipantPreferenceConfiguration() {

			protected String getPostSaveListenerId() {
	            return fPostSaveListener.getId();
            }

			protected String getPostSaveListenerName() {
	            return fPostSaveListener.getName();
            }			
		};
	}
	
	/**
	 * Returns the preference configuration of the described
	 * save participant.
	 * 
	 * @return the preference configuration
	 */
	public ISaveParticipantPreferenceConfiguration getPreferenceConfiguration() {
		if (fPreferenceConfiguration == null)
			fPreferenceConfiguration= createPreferenceConfiguration();
		
	    return fPreferenceConfiguration;
    }

	/**
	 * Returns the identifier of the described save participant.
	 *
	 * @return the non-empty id of this descriptor
	 */
	public String getId() {
		return fPostSaveListener.getId();
	}

}
