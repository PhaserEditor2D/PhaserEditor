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
package phasereditor.canvas.ui.editors.grid.editors;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * @author arian
 *
 */
@SuppressWarnings("restriction")
public class UserCodeBeforeAfterCodeComp extends Composite {

	private SourceViewer _beforeTextViewer;
	private SourceViewer _afterTextViewer;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public UserCodeBeforeAfterCodeComp(Composite parent, int style) {
		super(parent, style);
		setLayout(new FillLayout(SWT.HORIZONTAL));

		SashForm sashForm = new SashForm(this, SWT.VERTICAL);

		Composite composite = new Composite(sashForm, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		Label lblBefore = new Label(composite, SWT.NONE);
		lblBefore.setText("Before");

		 _beforeTextViewer = createViewer(composite);
		StyledText styledText = _beforeTextViewer.getTextWidget();
		styledText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		Composite composite_1 = new Composite(sashForm, SWT.NONE);
		composite_1.setLayout(new GridLayout(1, false));

		Label lblAfter = new Label(composite_1, SWT.NONE);
		lblAfter.setText("After");

		_afterTextViewer = createViewer(composite_1);
		StyledText styledText_1 = _afterTextViewer.getTextWidget();
		styledText_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		sashForm.setWeights(new int[] { 1, 1 });

	}

	private static SourceViewer createViewer(Composite parent) {
		IDocument document = new Document();
		
		//TODO: #RemovingWST		
//		JavaScriptTextTools tools = JavaScriptPlugin.getDefault().getJavaTextTools();
//		tools.setupJavaDocumentPartitioner(document, IJavaScriptPartitions.JAVA_PARTITIONING);
//		IPreferenceStore store = JavaScriptPlugin.getDefault().getCombinedPreferenceStore();
//		SourceViewer viewer = new JavaSourceViewer(parent, null, null, false, SWT.BORDER |  SWT.V_SCROLL | SWT.H_SCROLL,
//				store);
//		viewer.getTextWidget().setAlwaysShowScrollBars(false);
//		SimpleJavaSourceViewerConfiguration configuration = new SimpleJavaSourceViewerConfiguration(tools.getColorManager(), store, null, IJavaScriptPartitions.JAVA_PARTITIONING, false);
		
//		viewer.configure(configuration);

//		Font font = JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT);
//		viewer.getTextWidget().setFont(font);

		
		SourceViewer viewer = //new JavaSourceViewer(parent, null, null, false, SWT.BORDER |  SWT.V_SCROLL | SWT.H_SCROLL,store);
				new SourceViewer(parent, null, SWT.BORDER |  SWT.V_SCROLL | SWT.H_SCROLL);
		
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
	
}
