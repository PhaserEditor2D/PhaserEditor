/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.compare;

/**
 * Provides "Replace from local history" for Java elements.
 */
public class JavaReplaceWithPreviousEditionAction extends JavaHistoryAction {
					
	public JavaReplaceWithPreviousEditionAction() {
	}	
	
	protected JavaHistoryActionImpl createDelegate() {
		return new JavaReplaceWithEditionActionImpl(true);
	}
}

