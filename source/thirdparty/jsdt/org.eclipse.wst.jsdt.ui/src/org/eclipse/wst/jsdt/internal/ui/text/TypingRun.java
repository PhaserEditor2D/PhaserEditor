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
package org.eclipse.wst.jsdt.internal.ui.text;



/**
 * Describes a run of similar typing changes.
 * <p>
 * XXX to be extended with further information, e.g. offset, length, and
 * content of the run.
 * </p>
 *
 * 
 */
public final class TypingRun {
	/**
	 * A change of type <code>DELETE</code> deletes one single character (through delete or
	 * backspace or empty paste).
	 */
	public static final ChangeType DELETE= new ChangeType(true, "DELETE"); //$NON-NLS-1$
	/**
	 * A change of type <code>INSERT</code> inserts one single character
	 * (normal typing).
	 */
	public static final ChangeType INSERT= new ChangeType(true, "INSERT"); //$NON-NLS-1$
	/**
	 * A change of type <code>NO_CHANGE</code> does not change anything.
	 */
	public static final ChangeType NO_CHANGE= new ChangeType(false, "NO_CHANGE"); //$NON-NLS-1$
	/**
	 * A change of type <code>OVERTYPE</code> replaces one single character
	 * (overwrite mode, pasting a single character).
	 */
	public static final ChangeType OVERTYPE= new ChangeType(true, "OVERTYPE"); //$NON-NLS-1$
	/**
	 * A change of type <code>SELECTION</code> does not change text, but
	 * changes the focus, or selection. Such a change ends all typing runs.
	 */
	public static final ChangeType SELECTION= new ChangeType(false, "SELECTION"); //$NON-NLS-1$
	/**
	 * A change of type <code>UNKNOWN</code> modifies text in an
	 * unspecified way. An example is pasting more than one character, or
	 * deleting an entire selection, or reverting a file. Such a change ends
	 * all typing runs and cannot form a typing run with any other change,
	 * including a change of type <code>UNKNOWN</code>.
	 */
	public static final ChangeType UNKNOWN= new ChangeType(true, "UNKNOWN"); //$NON-NLS-1$


	/**
	 * Enumeration of change types.
	 *
	 * 
	 */
	public static final class ChangeType {
		private final boolean fIsModification;
		private final String fName;

		/** Private ctor for type safe enumeration. */
		private ChangeType(boolean isRunPart, String name) {
			fIsModification= isRunPart;
			fName= name;
		}

		/**
		 * Returns <code>true</code> if changes of this type modify text.
		 *
		 * @return <code>true</code> if changes of this type modify text,
		 *         <code>false</code> otherwise
		 */
		boolean isModification() {
			return fIsModification;
		}

		/*
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return fName;
		}
	}

	/**
	 * Creates a new run.
	 *
	 * @param type the type of the run
	 */
	TypingRun(ChangeType type) {
		this.type= type;
	}

	/** The change type of this run. */
	public final ChangeType type;
}
