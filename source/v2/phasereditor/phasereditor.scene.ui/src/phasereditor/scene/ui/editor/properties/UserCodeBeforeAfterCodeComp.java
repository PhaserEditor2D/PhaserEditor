// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.scene.ui.editor.properties;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.registry.TMEclipseRegistryPlugin;
import org.eclipse.tm4e.ui.text.TMPresentationReconciler;

import phasereditor.ui.SwtRM;

/**
 * @author arian
 *
 */
public class UserCodeBeforeAfterCodeComp extends Composite {

	private SourceViewer _beforeTextViewer;
	private SourceViewer _afterTextViewer;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public UserCodeBeforeAfterCodeComp(Composite parent, int style, String methodName) {
		super(parent, style);

		setBackgroundMode(SWT.INHERIT_FORCE);

		setLayout(new GridLayout(1, true));

		Label lblMethodName = new Label(this, SWT.NONE);
		lblMethodName.setFont(SwtRM.getFont("Monospace", 11, SWT.BOLD));
		lblMethodName.setText(methodName + "() { ");

		_beforeTextViewer = createViewer(this);
		StyledText styledText = _beforeTextViewer.getTextWidget();
		styledText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		Label lblMiddle = new Label(this, SWT.NONE);
		lblMiddle.setForeground(SwtRM.getColor(SWT.COLOR_DARK_GREEN));
		lblMiddle.setText("// ... generated code ... //");
		lblMiddle.setFont(SwtRM.getFont("Monospace", 11, SWT.NORMAL));

		_afterTextViewer = createViewer(this);
		StyledText styledText_1 = _afterTextViewer.getTextWidget();
		styledText_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		Label lblMethodEnd = new Label(this, SWT.NONE);
		lblMethodEnd.setFont(SwtRM.getFont("Monospace", 11, SWT.BOLD));
		lblMethodEnd.setText("}");

	}

	private static SourceViewer createViewer(Composite parent) {
		IDocument document = new Document();

		IGrammar grammar = TMEclipseRegistryPlugin.getGrammarRegistryManager().getGrammarForScope("source.js");

		SourceViewer viewer = new SourceViewer(parent, null, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		StyledText styledText = (StyledText) viewer.getControl();
		styledText.setFont(JFaceResources.getTextFont());

		TMPresentationReconciler reconciler = new TMPresentationReconciler();
		reconciler.setGrammar(grammar);
		reconciler.install(viewer);
		viewer.getControl().addDisposeListener(e -> {
			reconciler.uninstall();
		});

		viewer.setEditable(true);
		viewer.setDocument(document);

		return viewer;
	}

	public void setBeforeText(String text) {
		_beforeTextViewer.getDocument().set(text);
	}

	public String getBeforeText() {
		return _beforeTextViewer.getDocument().get();
	}

	public void setAfterText(String text) {
		_afterTextViewer.getDocument().set(text);
	}

	public String getAfterText() {
		return _afterTextViewer.getDocument().get();
	}

	public SourceViewer getBeforeTextViewer() {
		return _beforeTextViewer;
	}

	public SourceViewer getAfterTextViewer() {
		return _afterTextViewer;
	}
}
