// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package com.subshell.snippets.jface.internal.tooltip;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.subshell.snippets.jface.tooltip.tooltipsupport.ICustomInformationControlCreator;
import com.subshell.snippets.jface.tooltip.tooltipsupport.IInformationProvider;
import com.subshell.snippets.jface.tooltip.tooltipsupport.TableViewerInformationProvider;
import com.subshell.snippets.jface.tooltip.tooltipsupport.Tooltips;

public class Main {

	Main(Shell shell) throws Exception {
		// Create a table viewer
		TableViewer viewer = new TableViewer(shell);

		// Create the label provider
		viewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element == null) {
					return "";
				}
				return element.getClass().getSimpleName() + ": " + element.toString();
			}
		});

		// Create the content provider
		viewer.setContentProvider(new ArrayContentProvider());

		// Create the table input with different domain model objects
		List<Object> tableInput = new ArrayList<>();
		tableInput.add("Just a String");
		tableInput.add("Just another String");
		tableInput.add(new Person("Donald", "Duck"));
		tableInput.add(new Person("Daisy", "Duck"));
		tableInput.add(new URL("http://www.google.de"));
		tableInput.add(new URL("http://www.subshell.com"));
		tableInput.add(new URL("http://www.tagesschau.de"));

		viewer.setInput(tableInput);

		// Hook tooltips
		hookTooltips(viewer);
	}

	private static void hookTooltips(TableViewer viewer) {
		// Create an information provider for our table viewer
		IInformationProvider informationProvider = new TableViewerInformationProvider(viewer);

		// Our table viewer contains elements of type String, Person and URL.
		// Strings are handled by default. For Person and URL we need custom
		// control creators.
		List<ICustomInformationControlCreator> informationControlCreators = new ArrayList<>();
		informationControlCreators.add(new PersonInformationControlCreator());
		informationControlCreators.add(new WebBrowserInformationControlCreator());

		// Install tooltips
		Tooltips.install(viewer.getControl(), informationProvider, informationControlCreators, false);
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		new Main(shell);
		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

		display.dispose();
	}
}