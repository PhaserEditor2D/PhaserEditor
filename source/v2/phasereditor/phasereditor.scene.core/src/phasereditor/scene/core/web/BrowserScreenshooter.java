// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.scene.core.web;

import static java.lang.System.out;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.function.Consumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import phasereditor.webrun.core.WebRunCore;

/**
 * @author arian
 *
 */
public class BrowserScreenshooter {

	@SuppressWarnings("unused")
	public static void screenshot(IFile file, Consumer<Image> consumer) {
		var display = Display.getDefault();

		var shell = new Shell(display, SWT.NO_TRIM | SWT.SHEET);
		shell.setLayout(new FillLayout());
		shell.setSize(420, 380);

		var browser = new Browser(shell, 0);

		shell.setAlpha(0);

		shell.open();

		var filepath = file.getFullPath().toPortableString();
		var port = WebRunCore.getServerPort();
		var url = "http://localhost:" + port
				+ "/extension/phasereditor.scene.ui.editor.html/sceneEditor/screenshot.html?" + filepath;

		browser.setUrl(url);

		// new BrowserFunction(browser, "GetDataURL") {
		// @Override
		// public Object function(Object[] arguments) {
		// try {
		//
		// var data = (String) arguments[0];
		// var i = data.indexOf(",");
		// data = data.substring(i + 1);
		//
		// out.println(data);
		//
		// var bytes = Base64.getDecoder().decode(data.getBytes());
		//
		// var image = new Image(display, new ByteArrayInputStream(bytes));
		//
		// consumer.accept(image);
		//
		// } catch (Exception e) {
		// SceneCore.logError(e);
		// } finally {
		// shell.dispose();
		// }
		//
		// return null;
		// }
		// };

		swtRun(600, () -> shell.dispose());

		out.println("Screenshot-URL: " + url);

	}
}
