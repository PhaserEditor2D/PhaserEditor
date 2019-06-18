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
package phasereditor.scene.ui.editor;

import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import phasereditor.scene.ui.editor.messages.SelectObjectsMessage;
import phasereditor.ui.IBrowser;

/**
 * @author arian
 *
 */
public class SceneWebView extends Composite {

	private IBrowser _webView;
	private SceneEditor _editor;

	public SceneWebView(SceneEditor editor, Composite parent, int style) {
		super(parent, style);
		var layout = new FillLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);

		_editor = editor;

		_webView = IBrowser.create(this, SWT.NONE);

		var keyDownListener = new Listener() {

			private int _lastTime;

			@Override
			public void handleEvent(Event event) {

				if (event.time == _lastTime) {
					return;
				}

				if (!_webView.getControl().isDisposed() && _webView.getControl().isFocusControl()) {
					if (event.keyCode == SWT.SPACE) {
						_lastTime = event.time;
						if (!editor.hasInteractiveTools(Set.of("Hand"))) {
							editor.setInteractiveTools(Set.of("Hand"));
						}
					}
				}
			}
		};

		var keyUpListener = new Listener() {

			private int _lastTime;

			@Override
			public void handleEvent(Event event) {
				if (event.time == _lastTime) {
					return;
				}

				if (!_webView.getControl().isDisposed() && _webView.getControl().isFocusControl()) {

					var stateMask = event.stateMask;

					var controlPressed = (stateMask & SWT.MOD1) == SWT.MOD1;

					if (controlPressed) {
						switch (event.keyCode) {
						case 'c':
							_lastTime = event.time;
							_editor.copy();
							break;
						case 'x':
							_lastTime = event.time;
							_editor.cut();
							break;
						case 'v':
							_lastTime = event.time;
							_editor.paste();
							break;
						case 'a':
							_lastTime = event.time;
							_editor.selectAll();
							break;
						default:
							break;
						}
					} else {
						switch (event.keyCode) {
						case SWT.DEL:
							_lastTime = event.time;
							getEditor().delete();
							break;
						case SWT.ESC:
							_lastTime = event.time;
							editor.setSelection(List.of());
							editor.getBroker().sendAll(new SelectObjectsMessage(editor));
							break;
						case SWT.SPACE:
							_lastTime = event.time;
							if (editor.hasInteractiveTools(Set.of("Hand"))) {
								editor.setInteractiveTools(Set.of());
							}
							break;
						default:
							break;
						}
					}

				}
			}
		};
		getDisplay().addFilter(SWT.KeyDown, keyDownListener);
		getDisplay().addFilter(SWT.KeyUp, keyUpListener);

		_webView.getControl().addDisposeListener(e -> {
			getDisplay().removeFilter(SWT.KeyDown, keyDownListener);
			getDisplay().removeFilter(SWT.KeyUp, keyUpListener);
		});

	}

	public void setUrl(String url) {
		_webView.setUrl(url);
	}

	public SceneEditor getEditor() {
		return _editor;
	}
}
