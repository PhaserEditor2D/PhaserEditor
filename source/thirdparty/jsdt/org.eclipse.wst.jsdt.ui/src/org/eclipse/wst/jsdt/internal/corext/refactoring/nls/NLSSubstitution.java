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

import java.util.Properties;

import org.eclipse.core.runtime.Assert;


public class NLSSubstitution {
	public static final int EXTERNALIZED= 0;
	public static final int IGNORED= 1;
	public static final int INTERNALIZED= 2;

	public static final int DEFAULT= EXTERNALIZED;
	public static final int STATE_COUNT= 3;

	private int fState;
	private String fKey;
	/**
	 * 
	 */
	private String fCachedPrefixPlusKey;
	private String fValue;

	private int fInitialState;
	private String fInitialKey;
	private String fInitialValue;
	
	private NLSElement fNLSElement;
	private AccessorClassReference fAccessorClassReference;
	
	private String fNewAccessorClassName;

	private String fPrefix= ""; //$NON-NLS-1$


	public NLSSubstitution(int state, String value, NLSElement element) {
		fNLSElement= element;
		fValue= value;
		fState= state;
		fInitialState= state;
		fInitialValue= value;
		fCachedPrefixPlusKey= null;
		Assert.isTrue(state == EXTERNALIZED || state == IGNORED || state == INTERNALIZED);
	}

	/**
	 * value == null indicates a corrupt substitution. 
	 */
	public NLSSubstitution(int state, String key, String value, NLSElement element, AccessorClassReference accessorClassReference) {
		this(state,value,element);
		if (state != EXTERNALIZED) {
			throw new IllegalArgumentException("Set to INTERNALIZE/IGNORED State with different Constructor"); //$NON-NLS-1$
		}
		fKey= key;
		fInitialKey= key;
		fAccessorClassReference= accessorClassReference;
		fNewAccessorClassName= null;
	}

	//util
	public static int countItems(NLSSubstitution[] elems, int task) {
		Assert.isTrue(task == NLSSubstitution.EXTERNALIZED || task == NLSSubstitution.IGNORED || task == NLSSubstitution.INTERNALIZED);
		int result= 0;
		for (int i= 0; i < elems.length; i++) {
			if (elems[i].fState == task) {
				result++;
			}
		}
		return result;
	}

	public NLSElement getNLSElement() {
		return fNLSElement;
	}
	
	public String getKeyWithoutPrefix() {
		return fKey;
	}

	/**
	 * Returns key dependent on state. 
	 * @return prefix + key when 
	 */
	public String getKey() {
		if ((fState == EXTERNALIZED) && hasStateChanged()) {
			if (fCachedPrefixPlusKey == null) {
				int length= 0;
				if (fPrefix != null)
					length= length + fPrefix.length(); 
				if (fKey != null)
					length= length + fKey.length(); 
				StringBuffer sb= new StringBuffer(length);
				sb.append(fPrefix);
				sb.append(fKey);
				fCachedPrefixPlusKey= sb.toString();
			}
			return fCachedPrefixPlusKey;
		}
		return fKey;
	}

	public void setKey(String key) {
		if (fState != EXTERNALIZED) {
			throw new IllegalStateException("Must be in Externalized State !"); //$NON-NLS-1$
		}
		fCachedPrefixPlusKey= null;
		fKey= key;
	}

	public void setValue(String value) {
		fValue= value;
	}
	
	public void setInitialValue(String value) {
		fInitialValue= value;
	}

	/**
	 * Value can be null.
	 */
	public String getValue() {
		return fValue;
	}
	
	public String getValueNonEmpty() {
		if (fValue == null) {
			return ""; //$NON-NLS-1$
		}
		return fValue;
	}

	public int getState() {
		return fState;
	}

	public void setState(int state) {
		fCachedPrefixPlusKey= null;
		fState= state;
	}
	
	public void setUpdatedAccessor(String accessorClassName) {
		fNewAccessorClassName= accessorClassName;
	}
	
	public String getUpdatedAccessor() {
		return fNewAccessorClassName;
	}

	public boolean hasStateChanged() {
		return fState != fInitialState;
	}
	
	public boolean isKeyRename() {
		return 	(fInitialKey != null && !fInitialKey.equals(fKey));
	}
	
	public boolean isValueRename() {
		return 	(fInitialValue != null && !fInitialValue.equals(fValue));
	}
	
	public boolean isAccessorRename() {
		return (fAccessorClassReference != null) && (fNewAccessorClassName != null) && !fNewAccessorClassName.equals(fAccessorClassReference.getName());
	}
	
	
	public boolean hasPropertyFileChange() {
		if (fInitialState != EXTERNALIZED && fState != EXTERNALIZED) {
			return false;
		}
		if (fInitialState != fState) {
			return true;
		}
		if (fState == EXTERNALIZED) {
			if (fInitialValue == null) {
				return true; // recreate entry in property file
			} else if (!fInitialValue.equals(fValue)) {
				return true; // change of value
			}
			if (!fInitialKey.equals(fKey)) {
				return true; // change of key
			}
		}
		return false;
	}
	
	public boolean hasAccessorClassChange() {
		if (fInitialState != EXTERNALIZED && fState != EXTERNALIZED) {
			return false;
		}
		if (fInitialState != fState) {
			return true;
		}
		if (fState == EXTERNALIZED) {
			if (fInitialValue == null) {
				return true; // recreate entry in property file
			} else if (!fInitialValue.equals(fValue)) {
				return false; // change of value
			}
			if (!fInitialKey.equals(fKey)) {
				return true; // change of key
			}
		}
		return false;
	}
	
	public boolean hasSourceChange() {
		if (hasStateChanged()) {
			return true;
		}
		if (fState == EXTERNALIZED) {
			if (!fInitialKey.equals(fKey)) {
				return true; // change of key
			}
			if (isAccessorRename()) {
				return true;
			}
		} else {
			if (!fInitialValue.equals(fValue)) {
				return true; // change of value
			}
		}
		return false;
	}
	
	public int getInitialState() {
		return fInitialState;
	}

	public String getInitialKey() {
		return fInitialKey;
	}

	public String getInitialValue() {
		return fInitialValue;
	}

	public AccessorClassReference getAccessorClassReference() {
		return fAccessorClassReference;
	}

	/**
	 * Sets the prefix.
	 */
	public void setPrefix(String prefix) {
		fPrefix= prefix;
		fCachedPrefixPlusKey= null;
	}

	public boolean isConflicting(NLSSubstitution[] substitutions) {
		if (fState == EXTERNALIZED) {
			String currKey= getKey();
			String currValue= getValueNonEmpty();
			for (int i= 0; i < substitutions.length; i++) {
				NLSSubstitution substitution= substitutions[i];
				if (substitution != this && substitution.getState() == EXTERNALIZED) {
					// same key but different value
					if (currKey.equals(substitution.getKey()) && !currValue.equals(substitution.getValueNonEmpty())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private String internalGetKeyWithoutPrefix() {
		if (fState == EXTERNALIZED && fPrefix != null && fKey != null && fKey.indexOf(fPrefix) == 0)
			return fKey.substring(fPrefix.length());
		return fKey;
	}

	public void generateKey(NLSSubstitution[] substitutions) {
		if (fState != EXTERNALIZED || ((fState == EXTERNALIZED) && hasStateChanged())) {
			int min= Integer.MAX_VALUE;
			int max= Integer.MIN_VALUE;
			
			for (int i= 0; i < substitutions.length; i++) {
				NLSSubstitution substitution= substitutions[i];
				if (substitution == this || substitution.fState != EXTERNALIZED)
					continue;
				try {
					int value= Integer.parseInt(substitution.internalGetKeyWithoutPrefix());
					min= Math.min(min, value);
					max= Math.max(max, value);
				} catch (NumberFormatException ex) {
					
				}
			}
			
			fCachedPrefixPlusKey= null;
			if (min == Integer.MAX_VALUE)
				fKey= createKey(0);
			else if (min > 0)
				fKey= createKey(min-1);
			else
				fKey= createKey(max + 1);
		}
	}

	public static void updateSubtitutions(NLSSubstitution[] substitutions, Properties props, String accessorClassName) {
		for (int i= 0; i < substitutions.length; i++) {
			NLSSubstitution substitution= substitutions[i];
			if ((substitution.getState() == NLSSubstitution.EXTERNALIZED) && !substitution.hasStateChanged()) {
				substitution.setInitialValue(props.getProperty(substitution.getKey()));
				substitution.setUpdatedAccessor(accessorClassName);
			}
		}
	}

	public void revert() {
		fState= fInitialState;
		fKey= fInitialKey;
		fCachedPrefixPlusKey= null;
		fValue= fInitialValue;
	}

	private String createKey(int counter) {
		return String.valueOf(counter);
	}

}
