/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.core.util;

import java.io.IOException;
import java.io.Reader;

/**
 * Provides the contents of multiple Readers in sequence.
 */
public class SequenceReader extends Reader {
	private Reader[] fReaders;
	private Reader fCurrentReader;

	/**
	 * @param readers
	 *            the readers from which to read
	 */
	public SequenceReader(Reader[] readers) {
		fReaders = readers;
		if (fReaders.length > 0) {
			fCurrentReader = fReaders[0];
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.Reader#close()
	 */
	public void close() throws IOException {
		if (fCurrentReader != null) {
			for (int i = 0; i <= indexOf(fCurrentReader); i++) {
				fReaders[i].close();
			}
		}
	}

	private int indexOf(Reader r) {
		for (int i = 0; i < fReaders.length; i++) {
			if (fReaders[i] == r) {
				return i;
			}
		}
		return -1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.Reader#read(char[], int, int)
	 */
	public int read(char[] cbuf, int off, int len) throws IOException {
		if (fCurrentReader != null) {
			int bufIndex = off;
			int read = 0;
			for (bufIndex = off; bufIndex - off < len; bufIndex++) {
				int c = primRead();
				if (c != -1) {
					read++;
					cbuf[bufIndex] = (char) c;
				}
				else if (read == 0) {
					return -1;
				}
			}
			return read;
		}
		return -1;
	}

	/**
	 * @return
	 */
	private int primRead() {
		int c = -1;
		try {
			c = fCurrentReader.read();
		}
		catch (IOException e) {
		}
		if (c == -1) {
			int index = indexOf(fCurrentReader);
			if (index > -1 && index < fReaders.length - 1) {
				fCurrentReader = fReaders[index + 1];
				c = primRead();
			}
		}
		return c;
	}
}
