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

package org.eclipse.wst.jsdt.internal.ui.preferences;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * Mockup preference store, for registering listeners and firing events,
 * without being an actual store.
 * <p>
 * All methods except firing, adding and removing listeners throw
 * an {@link java.lang.UnsupportedOperationException}.
 * </p>
 * 
 * 
 */
public class MockupPreferenceStore implements IPreferenceStore {

	/** Listeners on this store */
	private ListenerList fListeners= new ListenerList(ListenerList.IDENTITY);

	/**
	 * {@inheritDoc}
	 */
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		fListeners.add(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		fListeners.remove(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean contains(String name) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void firePropertyChangeEvent(String name, Object oldValue, Object newValue) {
		firePropertyChangeEvent(this, name, oldValue, newValue);
	}

	/**
	 * Fires a property change event with the given source, property name, old and new value. Used
	 * when the event source should be different from this mockup preference store.
	 * @param source The event source 
	 * @param name The property name
	 * @param oldValue The property's old value
	 * @param newValue The property's new value
	 */
	public void firePropertyChangeEvent(Object source, String name, Object oldValue, Object newValue) {
		PropertyChangeEvent event= new PropertyChangeEvent(source, name, oldValue, newValue);
		Object[] listeners= fListeners.getListeners();
		for (int i= 0; i < listeners.length; i++)
			((IPropertyChangeListener) listeners[i]).propertyChange(event);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean getBoolean(String name) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean getDefaultBoolean(String name) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public double getDefaultDouble(String name) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public float getDefaultFloat(String name) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public int getDefaultInt(String name) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public long getDefaultLong(String name) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDefaultString(String name) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public double getDouble(String name) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public float getFloat(String name) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public int getInt(String name) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public long getLong(String name) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getString(String name) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isDefault(String name) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean needsSaving() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void putValue(String name, String value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDefault(String name, double value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDefault(String name, float value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDefault(String name, int value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDefault(String name, long value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDefault(String name, String defaultObject) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDefault(String name, boolean value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setToDefault(String name) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setValue(String name, double value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setValue(String name, float value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setValue(String name, int value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setValue(String name, long value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setValue(String name, String value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setValue(String name, boolean value) {
		throw new UnsupportedOperationException();
	}

}
