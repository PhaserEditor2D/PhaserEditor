/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.ast.IFieldReference;
import org.eclipse.wst.jsdt.core.ast.ILiteral;
import org.eclipse.wst.jsdt.core.ast.ISingleNameReference;
import org.eclipse.wst.jsdt.core.ast.IThisReference;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayReference;

public class Util implements SuffixConstants {

	public interface Displayable {
		String displayString(Object o);
	}

	private static final int DEFAULT_READING_SIZE = 8192;
	public final static String UTF_8 = "UTF-8";	//$NON-NLS-1$
	public static final String LINE_SEPARATOR = System.getProperty("line.separator"); //$NON-NLS-1$

	public static final String EMPTY_STRING = new String(CharOperation.NO_CHAR);
	public static final int[] EMPTY_INT_ARRAY= new int[0];

	/**
	 * Returns the given bytes as a char array using a given encoding (null means platform default).
	 */
	public static char[] bytesToChar(byte[] bytes, String encoding) throws IOException {

		return getInputStreamAsCharArray(new ByteArrayInputStream(bytes), bytes.length, encoding);

	}
	/**
	 * Returns the contents of the given file as a byte array.
	 * @throws IOException if a problem occured reading the file.
	 */
	public static byte[] getFileByteContent(File file) throws IOException {
		InputStream stream = null;
		try {
			stream = new FileInputStream(file);
			return getInputStreamAsByteArray(stream, (int) file.length());
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
	/**
	 * Returns the contents of the given file as a char array.
	 * When encoding is null, then the platform default one is used
	 * @throws IOException if a problem occured reading the file.
	 */
	public static char[] getFileCharContent(File file, String encoding) throws IOException {
		InputStream stream = null;
		try {
			stream = new FileInputStream(file);
			return getInputStreamAsCharArray(stream, (int) file.length(), encoding);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
	/*
	 * NIO support to get input stream as byte array.
	 * Not used as with JDK 1.4.2 this support is slower than standard IO one...
	 * Keep it as comment for future in case of next JDK versions improve performance
	 * in this area...
	 *
	public static byte[] getInputStreamAsByteArray(FileInputStream stream, int length)
		throws IOException {

		FileChannel channel = stream.getChannel();
		int size = (int)channel.size();
		if (length >= 0 && length < size) size = length;
		byte[] contents = new byte[size];
		ByteBuffer buffer = ByteBuffer.wrap(contents);
		channel.read(buffer);
		return contents;
	}
	*/
	/**
	 * Returns the given input stream's contents as a byte array.
	 * If a length is specified (ie. if length != -1), only length bytes
	 * are returned. Otherwise all bytes in the stream are returned.
	 * Note this doesn't close the stream.
	 * @throws IOException if a problem occured reading the stream.
	 */
	public static byte[] getInputStreamAsByteArray(InputStream stream, int length)
		throws IOException {
		byte[] contents;
		if (length == -1) {
			contents = new byte[0];
			int contentsLength = 0;
			int amountRead = -1;
			do {
				int amountRequested = Math.max(stream.available(), DEFAULT_READING_SIZE);  // read at least 8K

				// resize contents if needed
				if (contentsLength + amountRequested > contents.length) {
					System.arraycopy(
						contents,
						0,
						contents = new byte[contentsLength + amountRequested],
						0,
						contentsLength);
				}

				// read as many bytes as possible
				amountRead = stream.read(contents, contentsLength, amountRequested);

				if (amountRead > 0) {
					// remember length of contents
					contentsLength += amountRead;
				}
			} while (amountRead != -1);

			// resize contents if necessary
			if (contentsLength < contents.length) {
				System.arraycopy(
					contents,
					0,
					contents = new byte[contentsLength],
					0,
					contentsLength);
			}
		} else {
			contents = new byte[length];
			int len = 0;
			int readSize = 0;
			while ((readSize != -1) && (len != length)) {
				// See PR 1FMS89U
				// We record first the read size. In this case len is the actual read size.
				len += readSize;
				readSize = stream.read(contents, len, length - len);
			}
		}

		return contents;
	}
	/*
	 * NIO support to get input stream as char array.
	 * Not used as with JDK 1.4.2 this support is slower than standard IO one...
	 * Keep it as comment for future in case of next JDK versions improve performance
	 * in this area...
	public static char[] getInputStreamAsCharArray(FileInputStream stream, int length, String encoding)
		throws IOException {

		FileChannel channel = stream.getChannel();
		int size = (int)channel.size();
		if (length >= 0 && length < size) size = length;
		Charset charset = encoding==null?systemCharset:Charset.forName(encoding);
		if (charset != null) {
			MappedByteBuffer bbuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
		    CharsetDecoder decoder = charset.newDecoder();
		    CharBuffer buffer = decoder.decode(bbuffer);
		    char[] contents = new char[buffer.limit()];
		    buffer.get(contents);
		    return contents;
		}
		throw new UnsupportedCharsetException(SYSTEM_FILE_ENCODING);
	}
	*/
	/**
	 * Returns the given input stream's contents as a character array.
	 * If a length is specified (ie. if length != -1), this represents the number of bytes in the stream.
	 * Note this doesn't close the stream.
	 * @throws IOException if a problem occured reading the stream.
	 */
	public static char[] getInputStreamAsCharArray(InputStream stream, int length, String encoding)
		throws IOException {
		InputStreamReader reader = null;
		try {
			reader = encoding == null
						? new InputStreamReader(stream)
						: new InputStreamReader(stream, encoding);
		} catch (UnsupportedEncodingException e) {
			// encoding is not supported
			reader =  new InputStreamReader(stream);
		}
		char[] contents;
		int totalRead = 0;
		if (length == -1) {
			contents = CharOperation.NO_CHAR;
		} else {
			// length is a good guess when the encoding produces less or the same amount of characters than the file length
			contents = new char[length]; // best guess
		}

		while (true) {
			int amountRequested;
			if (totalRead < length) {
				// until known length is met, reuse same array sized eagerly
				amountRequested = length - totalRead;
			} else {
				// reading beyond known length
				int current = reader.read();
				if (current < 0) break;

				amountRequested = Math.max(stream.available(), DEFAULT_READING_SIZE);  // read at least 8K

				// resize contents if needed
				if (totalRead + 1 + amountRequested > contents.length)
					System.arraycopy(contents, 	0, 	contents = new char[totalRead + 1 + amountRequested], 0, totalRead);

				// add current character
				contents[totalRead++] = (char) current; // coming from totalRead==length
			}
			// read as many chars as possible
			int amountRead = reader.read(contents, totalRead, amountRequested);
			if (amountRead < 0) break;
			totalRead += amountRead;
		}

		// Do not keep first character for UTF-8 BOM encoding
		int start = 0;
		if (totalRead > 0 && UTF_8.equals(encoding)) {
			if (contents[0] == 0xFEFF) { // if BOM char then skip
				totalRead--;
				start = 1;
			}
		}

		// resize contents if necessary
		if (totalRead < contents.length)
			System.arraycopy(contents, start, contents = new char[totalRead], 	0, 	totalRead);

		return contents;
	}

	public static int getLineNumber(int position, int[] lineEnds, int g, int d) {
		if (lineEnds == null)
			return 1;
		if (d == -1)
			return 1;
		int m = g, start;
		while (g <= d) {
			m = g + (d - g) /2;
			if (position < (start = lineEnds[m])) {
				d = m-1;
			} else if (position > start) {
				g = m+1;
			} else {
				return m + 1;
			}
		}
		if (position < lineEnds[m]) {
			return m+1;
		}
		return m+2;
	}


	/**
	 * Returns the contents of the given zip entry as a byte array.
	 * @throws IOException if a problem occured reading the zip entry.
	 */
	public static byte[] getZipEntryByteContent(ZipEntry ze, ZipFile zip)
		throws IOException {

		InputStream stream = null;
		try {
			stream = zip.getInputStream(ze);
			if (stream == null) throw new IOException("Invalid zip entry name : " + ze.getName()); //$NON-NLS-1$
			return getInputStreamAsByteArray(stream, (int) ze.getSize());
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	/**
	 * Returns true iff str.toLowerCase().endsWith(".jar") || str.toLowerCase().endsWith(".zip")
	 * implementation is not creating extra strings.
	 */
	public final static boolean isArchiveFileName(String name) {
		int nameLength = name == null ? 0 : name.length();
		int suffixLength = SUFFIX_ZIP.length;
		if (nameLength == suffixLength) {
			for (int i = 0; i < suffixLength; i++) {
				char c = name.charAt(nameLength - i - 1);
				int suffixIndex = suffixLength - i - 1;
				if (c != SUFFIX_zip[suffixIndex] && c != SUFFIX_ZIP[suffixIndex])
					break;
			}
		}

		suffixLength = SUFFIX_JAR.length;
		if (nameLength < suffixLength) return false;

		for (int i = 0; i < suffixLength; i++) {
			char c = name.charAt(nameLength - i - 1);
			int suffixIndex = suffixLength - i - 1;
			if (c != SUFFIX_jar[suffixIndex] && c != SUFFIX_JAR[suffixIndex]) return false;
		}
		return true;
	}
	/**
	 * Returns true iff str.toLowerCase().endsWith(".class")
	 * implementation is not creating extra strings.
	 */
	public final static boolean isClassFileName(char[] name) {
		int nameLength = name == null ? 0 : name.length;
		int suffixLength = SUFFIX_JAVA.length;
		if (nameLength < suffixLength) return false;

		for (int i = 0, offset = nameLength - suffixLength; i < suffixLength; i++) {
			char c = name[offset + i];
			if (c != SUFFIX_java[i] && c != SUFFIX_JAVA[i]) return false;
		}
		return true;
	}
	/**
	 * Returns true iff str.toLowerCase().endsWith(".class")
	 * implementation is not creating extra strings.
	 */
	public final static boolean isClassFileName(String name) {
		int nameLength = name == null ? 0 : name.length();
		int suffixLength = SUFFIX_JAVA.length;
		if (nameLength < suffixLength) return false;

		for (int i = 0; i < suffixLength; i++) {
			char c = name.charAt(nameLength - i - 1);
			int suffixIndex = suffixLength - i - 1;
			if (c != SUFFIX_java[suffixIndex] && c != SUFFIX_JAVA[suffixIndex]) return false;
		}
		return true;
	}
	
	/**
	 * <p>If any inclusion patterns are provided then the given path is considered
	 * excluded if it does not match one of the inclusion patterns or if it matches
	 * an inclusion pattern and an exclusion pattern.  If no inclusion patterns are
	 * provided then to be considered excluded the given pattern must match one of
	 * the exclusion patterns.</p>
	 * 
	 * <p>NOTE: should not be asked directly using pkg root paths</p>
	 * 
	 * @param path determine if this path is excluded
	 * @param inclusionPatterns if not <code>null</code> consider the given <code>path</code>
	 * excluded if it does not match one of these paths, if <code>null</code> then a path is
	 * only considered excluded if it matches one of the given <code>exclusionPatterns</code>
	 * @param exclusionPatterns if the given <code>path</code> matches one
	 * of these patterns then it is considered to be excluded
	 * @param isFolderPath <code>true</code> if the given <code>path</code> is
	 * a folder path, <code>false</code> otherwise
	 * @return <code>true</code> if <code>inclusionPatterns</code> is not <code>null</code>
	 * and the given <code>path</code> is not included, or if the given <code>path</code>
	 * is included in the given <code>exclusionPatterns</code>
	 * 
	 * @see IIncludePathEntry#getInclusionPatterns
	 * @see IIncludePathEntry#getExclusionPatterns
	 */
	public final static boolean isExcluded(char[] path, char[][] inclusionPatterns, char[][] exclusionPatterns, boolean isFolderPath) {
		if (inclusionPatterns == null && exclusionPatterns == null) return false;

		inclusionCheck: if (inclusionPatterns != null) {
			for (int i = 0, length = inclusionPatterns.length; i < length; i++) {
				char[] pattern = inclusionPatterns[i];
				char[] folderPattern = pattern;
				if (isFolderPath) {
					int lastSlash = CharOperation.lastIndexOf('/', pattern);
					if (lastSlash != -1 && lastSlash != pattern.length-1){ // trailing slash -> adds '**' for free (see http://ant.apache.org/manual/dirtasks.html)
						int star = CharOperation.indexOf('*', pattern, lastSlash);
						if ((star == -1
								|| star >= pattern.length-1
								|| pattern[star+1] != '*')) {
							folderPattern = CharOperation.subarray(pattern, 0, lastSlash);
						}
					}
				}
				if (pathMatch(path, folderPattern)) {
					break inclusionCheck;
				}
			}
			return true; // never included
		}
		if (isFolderPath) {
			path = CharOperation.concat(path, new char[] {'*'}, '/');
		}
		if (exclusionPatterns != null) {
			for (int i = 0, length = exclusionPatterns.length; i < length; i++) {
				if (pathMatch(path, exclusionPatterns[i])) {
					return true;
				}
			}
		}
		return false;
	}
	/**
	 * Returns true iff str.toLowerCase().endsWith(".js")
	 * implementation is not creating extra strings.
	 */
	public final static boolean isJavaFileName(char[] name) {
		int nameLength = name == null ? 0 : name.length;
		int suffixLength = SUFFIX_JAVA.length;
		if (nameLength < suffixLength) return false;

		for (int i = 0, offset = nameLength - suffixLength; i < suffixLength; i++) {
			char c = name[offset + i];
			if (c != SUFFIX_java[i] && c != SUFFIX_JAVA[i]) return false;
		}
		return true;
	}
	/**
	 * Returns true if str.toLowerCase().endsWith(".js")
	 * implementation is not creating extra strings.
	 */
	public final static boolean isJavaFileName(String name) {
		int nameLength = name == null ? 0 : name.length();
		int suffixLength = SUFFIX_JAVA.length;
		if (nameLength < suffixLength) return false;

		for (int i = 0; i < suffixLength; i++) {
			char c = name.charAt(nameLength - i - 1);
			int suffixIndex = suffixLength - i - 1;
			if (c != SUFFIX_java[suffixIndex] && c != SUFFIX_JAVA[suffixIndex]) return false;
		}
		return true;
	}

	/**
	 * INTERNAL USE-ONLY
	 * Search the column number corresponding to a specific position
	 */
	public static final int searchColumnNumber(int[] startLineIndexes, int lineNumber, int position) {
		switch(lineNumber) {
			case 1 :
				return position + 1;
			case 2:
				return position - startLineIndexes[0];
			default:
				int line = lineNumber - 2;
	    		int length = startLineIndexes.length;
	    		if (line >= length) {
	    			return position - startLineIndexes[length - 1];
	    		}
	    		return position - startLineIndexes[line];
		}
	}

	/**
	 * Converts a boolean value into Boolean.
	 * @param bool The boolean to convert
	 * @return The corresponding Boolean object (TRUE or FALSE).
	 */
	public static Boolean toBoolean(boolean bool) {
		if (bool) {
			return Boolean.TRUE;
		} else {
			return Boolean.FALSE;
		}
	}
	/**
	 * Converts an array of Objects into String.
	 */
	public static String toString(Object[] objects) {
		return toString(objects,
			new Displayable(){
				public String displayString(Object o) {
					if (o == null) return "null"; //$NON-NLS-1$
					return o.toString();
				}
			});
	}

	/**
	 * Converts an array of Objects into String.
	 */
	public static String toString(Object[] objects, Displayable renderer) {
		if (objects == null) return ""; //$NON-NLS-1$
		StringBuffer buffer = new StringBuffer(10);
		for (int i = 0; i < objects.length; i++){
			if (i > 0) buffer.append(", "); //$NON-NLS-1$
			buffer.append(renderer.displayString(objects[i]));
		}
		return buffer.toString();
	}

	/**
	 * <p>Builds a type name from an expression by iterating over all parts of the expression.<p>
	 * 
	 * @param expression to iterate over and build a type name from
	 * @return type name built from iterating over the given <code>expression</code>, or
	 * <code>null</code> if a type name can not be built from the given expression
	 */
	public final static char[] getTypeName(IExpression expression) {

		IExpression currExpr = expression;
		
		char[] selector = null;
		while (currExpr != null) {
			if (currExpr instanceof IFieldReference) {
				if (selector == null) {
					selector = ((IFieldReference) currExpr).getToken();
				}
				else {
					selector = CharOperation.concatWith(new char[][]{((IFieldReference) currExpr).getToken(), selector}, '.');
				}
				currExpr = ((IFieldReference) currExpr).getReceiver();
			}
			else if (currExpr instanceof ISingleNameReference) {
				if (selector == null) {
					selector = ((ISingleNameReference) currExpr).getToken();
				}
				else {
					selector = CharOperation.concatWith(new char[][]{((ISingleNameReference) currExpr).getToken(), selector}, '.');
				}
				currExpr = null;
			}
			else if (currExpr instanceof ArrayReference) {
				ArrayReference arrayRef = (ArrayReference) currExpr;

				/*
				 * if the array reference position is a literately keep
				 * building selector else there is a dynamic selector of some
				 * sort so there is no way to build a type name from it
				 */
				if (arrayRef.position instanceof ILiteral) {
					if (selector == null) {
						selector = ((ILiteral) arrayRef.position).source();
					}
					else {
						selector = CharOperation.concatWith(new char[][]{((ILiteral) arrayRef.position).source(), selector}, '.');
					}
					currExpr = arrayRef.receiver;
				}
				else {
					currExpr = null;
					selector = null;
				}
			}
			else if (currExpr instanceof IThisReference) {
				// this can not be handled right now because the resolved type
				// for 'this' never seems to be resolved yet
				currExpr = null;
				selector = null;
			}
			else {
				// do not know how to handle the rest of the expression so
				// give up
				currExpr = null;
				selector = null;
			}
		}
		
		return selector;
	}
	
	/**
	 * <p>Determine if the given path is a match for the given match path.  If one path is
	 * file system absolute and another is relative or absolute to the workspace then the
	 * path that is not file system absolute will be converted to file system absolute.
	 * The matching pattern can contain *, **, or ? wild cards.</p>
	 * 
	 * @param pathChars  check to see if this path matches the <code>matchpathChars</code>
	 * @param matchPathChars check to see if the given <code>pathChars</code> match this pattern
	 * @return <code>true</code> if the given <code>pathChars</code> match the given given
	 * <code>matchPathChars<code>, <code>false</code> otherwise.
	 */
	public static boolean pathMatch(char[] pathChars, char[] matchPathChars) {
		IPath path = new Path(new String(pathChars));
		IPath matchPath = new Path(new String(matchPathChars));
	
		//determine if either path is file system absolute
		IPath fileSystemWorkspacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
		boolean isPathFileSystemAbsolute = fileSystemWorkspacePath.isPrefixOf(path);
		boolean isMatchPathFileSystemAbsolute = fileSystemWorkspacePath.isPrefixOf(matchPath);
		
		/* if the two paths are not both file system absolute or both workspace absolute
		 * then transform the none file system absolute path to file system absolute
		 */
		if((!isPathFileSystemAbsolute && isMatchPathFileSystemAbsolute) || (isPathFileSystemAbsolute && !isMatchPathFileSystemAbsolute)){
			if(!isPathFileSystemAbsolute) {
				boolean hadTrailingSeparator = path.hasTrailingSeparator();
				path = ResourcesPlugin.getWorkspace().getRoot().getFile(path).getLocation();
				if(hadTrailingSeparator) {
					path = path.addTrailingSeparator();
				}
			}
			
			if(!isMatchPathFileSystemAbsolute) {
				boolean hadTrailingSeparator = matchPath.hasTrailingSeparator();
				matchPath = ResourcesPlugin.getWorkspace().getRoot().getFile(matchPath).getLocation();
				if(hadTrailingSeparator) {
					matchPath = matchPath.addTrailingSeparator();
				}
			}
		}
		
		//be sure both are absolute now (fixes 'project1\file.js' to '\project1\file.js')
		path = path.makeAbsolute();
		matchPath = matchPath.makeAbsolute();
				
		return CharOperation.pathMatch(matchPath.toPortableString().toCharArray(), path.toPortableString().toCharArray(), true, IPath.SEPARATOR);
	}

}
