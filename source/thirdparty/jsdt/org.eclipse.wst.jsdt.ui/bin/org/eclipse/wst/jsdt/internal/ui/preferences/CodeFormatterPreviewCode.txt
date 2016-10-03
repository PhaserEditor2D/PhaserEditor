/**
 *    This header comment may be left unformatted.
 */

package org.eclipse.formatter.example;

/**
 * Example class displaying the effects of various code formatting preferences.
 * <p> See also {@link org.eclipse.editor.syntax}. </p>
 * 
 * The blank line above may be cleared.
 * @version 3.0
 */
public class Example extends Object {
	/* This comment may be wrapped to multiple lines depending on the maximal line length. */
	private int integer= 0;
	// This single-line comment may be wrapped too...
	private String string= "zero";

	/**
	 * This comment shows the formatting of code snippets.
	 * <pre>
	 * while ((size = foo(size, max)) > 0) { System.out.println("bar"); }
	 * </pre>
	 * After this comment a blank line may be inserted.
	 * @param size The size
	 * @param max The maximum
	 */
	public int foo(int size, int max) {

		if (size < max) {
		try {
		size=(long)stream.available();
		} catch (IOException e) {

		}
		} else if (size == max) {
		++size;
		} else {
		--size;
		}
		return size;
	}
}
