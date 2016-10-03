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
package org.eclipse.wst.jsdt.internal.corext.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;

public class IOCloser {
	public static void perform(Reader reader, InputStream stream) {
		try {
			rethrows(reader, stream);
		} catch (IOException e) {
			JavaScriptPlugin.log(e);
		}
	}
	
	public static void rethrows(Reader reader, InputStream stream) throws IOException {
		if (reader != null) {
			reader.close();
			return;
		}
		if (stream != null) {
			stream.close();
			return;
		}
	}	
}

