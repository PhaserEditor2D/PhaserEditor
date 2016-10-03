/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.ui.text.spelling.engine;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;


/**
 * Persistent modifiable word-list based dictionary.
 *
 * 
 */
public class PersistentSpellDictionary extends AbstractSpellDictionary {

	/** The word list location */
	private final URL fLocation;

	/**
	 * Creates a new persistent spell dictionary.
	 *
	 * @param url
	 *                   The URL of the word list for this dictionary
	 */
	public PersistentSpellDictionary(final URL url) {
		fLocation= url;
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.text.spelling.engine.AbstractSpellDictionary#acceptsWords()
	 */
	public boolean acceptsWords() {
		return true;
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.spelling.engine.ISpellDictionary#addWord(java.lang.String)
	 */
	public void addWord(final String word) {
		if (isCorrect(word))
			return;

		Charset charset= Charset.forName(getEncoding());
		ByteBuffer byteBuffer= charset.encode(word + "\n"); //$NON-NLS-1$
		int size= byteBuffer.limit();
		final byte[] byteArray;
		if (byteBuffer.hasArray())
			byteArray= byteBuffer.array();
		else {
			byteArray= new byte[size];
			byteBuffer.get(byteArray);
		}
		FileOutputStream fileStream = null;
			
		try {
			fileStream= new FileOutputStream(fLocation.getPath(), true);
			
			// Encoding UTF-16 charset writes a BOM. In which case we need to cut it away if the file isn't empty
			int bomCutSize= 0;
			if (!isEmpty() && "UTF-16".equals(charset.name())) //$NON-NLS-1$
				bomCutSize= 2;
			
			fileStream.write(byteArray, bomCutSize, size - bomCutSize);
		} catch (IOException exception) {
			JavaScriptPlugin.log(exception);
			return;
		} finally {
			try {
				if (fileStream != null)
					fileStream.close();
			} catch (IOException e) {
				// Ignore
			}
		}

		hashWord(word);
	}

	/*
	 * @see org.eclipse.wst.jsdt.internal.ui.text.spelling.engine.AbstractSpellDictionary#getURL()
	 */
	protected final URL getURL() {
		return fLocation;
	}
}
