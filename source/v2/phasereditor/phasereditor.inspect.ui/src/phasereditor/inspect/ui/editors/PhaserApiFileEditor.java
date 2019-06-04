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
package phasereditor.inspect.ui.editors;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.registry.TMEclipseRegistryPlugin;
import org.eclipse.tm4e.ui.TMUIPlugin;
import org.eclipse.tm4e.ui.text.TMPresentationReconciler;
import org.eclipse.tm4e.ui.themes.ITheme;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;

/**
 * @author arian
 *
 */
public class PhaserApiFileEditor extends AbstractDecoratedTextEditor {
	public static final String ID = "phasereditor.inspect.ui.editors.PhaserApiFileEditor";

	public PhaserApiFileEditor() {
	}
	
	@Override
	public boolean isEditable() {
		return false;
	}

	@Override
	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		var viewer = new TMViewer2(parent, ruler, styles);
		var grammar = TMEclipseRegistryPlugin.getGrammarRegistryManager().getGrammarForScope("source.js");
		var theme = TMUIPlugin.getThemeManager().getThemeForScope("source.js");

		viewer.setGrammar(grammar);
		viewer.setTheme(theme);

		return viewer;
	}
}

/**
 * Simple TextMate Viewer.
 *
 */
class TMViewer2 extends SourceViewer {

	private TMPresentationReconciler reconciler;

	public TMViewer2(Composite parent, IVerticalRuler ruler, int styles) {
		super(parent, ruler, styles);
		init();
	}

	public TMViewer2(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler,
			boolean showAnnotationsOverview, int styles) {
		super(parent, verticalRuler, overviewRuler, showAnnotationsOverview, styles);
		init();
	}

	private void init() {
		this.reconciler = new TMPresentationReconciler();
		SourceViewerConfiguration configuration = new TMSourceViewerConfiguration();
		this.configure(configuration);
	}

	@Override
	public void configure(SourceViewerConfiguration configuration) {
//		try {
//			super.configure(configuration);
//		} catch (Exception e) {
//			// this is a hack!!! because it sends an illegal status exception.
//		}
		
		fHyperlinkManager = null;
		super.configure(configuration);
	}

	private class TMSourceViewerConfiguration extends SourceViewerConfiguration {

		@Override
		public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
			return reconciler;
		}

	}

	public void setGrammar(IGrammar grammar) {
		reconciler.setGrammar(grammar);
		if (getDocument() == null) {
			super.setDocument(new Document());
		}
	}

	public void setTheme(ITheme theme) {
		reconciler.setThemeId(theme.getId());
		StyledText styledText = getTextWidget();
		styledText.setForeground(null);
		styledText.setBackground(null);
		theme.initializeViewerColors(styledText);
		getTextWidget().setFont(JFaceResources.getTextFont());
	}

	public void setText(String text) {
		if (getDocument() == null) {
			super.setDocument(new Document());
		}
		getDocument().set(text);
	}
}
