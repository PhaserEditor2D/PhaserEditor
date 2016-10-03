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

import org.eclipse.wst.jsdt.internal.ui.text.TypingRun.ChangeType;


/**
 * Listener for <code>TypingRun</code> events.
 *
 * 
 */
public interface ITypingRunListener {
	/**
	 * Called when a new <code>TypingRun</code> is started.
	 *
	 * @param run the newly started run
	 */
	void typingRunStarted(TypingRun run);

	/**
	 * Called whenever a <code>TypingRun</code> is ended.
	 *
	 * @param run the ended run
	 * @param reason the type of change that caused the end of the run
	 */
	void typingRunEnded(TypingRun run, ChangeType reason);
}
