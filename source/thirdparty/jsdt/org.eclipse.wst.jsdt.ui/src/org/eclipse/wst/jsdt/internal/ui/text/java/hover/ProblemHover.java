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
package org.eclipse.wst.jsdt.internal.ui.text.java.hover;

/**
 * This annotation hover shows the description of the
 * selected java annotation.
 *
 * XXX: Currently this problem hover only works for
 *		Java problems.
 *		see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=62081
 *
 * 
 */
public class ProblemHover extends AbstractAnnotationHover {

	public ProblemHover() {
		super(false);
	}

}
