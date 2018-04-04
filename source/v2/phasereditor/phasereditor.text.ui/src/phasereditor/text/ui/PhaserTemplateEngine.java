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
package phasereditor.text.ui;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.template.java.CompilationUnitContext;
import org.eclipse.wst.jsdt.internal.corext.template.java.CompilationUnitContextType;
import org.eclipse.wst.jsdt.internal.corext.template.java.JavaContextType;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.text.template.contentassist.TemplateProposal;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

// copied from org.eclipse.wst.jsdt.internal.ui.text.template.contentassist.TemplateEngine
@SuppressWarnings("restriction")
public class PhaserTemplateEngine {

	private static final String $_LINE_SELECTION = "${" + GlobalTemplateVariables.LineSelection.NAME + "}"; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String $_WORD_SELECTION = "${" + GlobalTemplateVariables.WordSelection.NAME + "}"; //$NON-NLS-1$ //$NON-NLS-2$
	private static Template[] PHASER_TEMPLATES;

	static {
		try {
			PHASER_TEMPLATES = new Template[0];

			List<Template> templates = new ArrayList<>();
			URL url = new URL("platform:/plugin/" + TextUI.PLUGIN_IDE
					+ "/templates/PhaserSandboxTemplates.json");
			try (InputStream input = url.openStream()) {
				JSONObject doc = new JSONObject(new JSONTokener(input));

				JSONArray jsonRoot = doc.getJSONArray("templates");
				for (int i = 0; i < jsonRoot.length(); i++) {
					JSONObject jsonSection = jsonRoot.getJSONObject(i);
					for (String sectionName : JSONObject.getNames(jsonSection)) {
						JSONObject jsonTemplates = jsonSection
								.getJSONObject(sectionName);
						for (String templName : JSONObject
								.getNames(jsonTemplates)) {
							String pattern = jsonTemplates.getString(templName);
							String name = templName.toLowerCase().replace(" ",
									"");
							String desc = templName.toLowerCase() + " - "
									+ sectionName;
							Template templ = new Template(name, desc,
									JavaContextType.NAME, pattern, true);
							templates.add(templ);
						}
					}
				}
			}
			PHASER_TEMPLATES = templates
					.toArray(new Template[templates.size()]);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/** The context type. */
	private TemplateContextType fContextType;
	/** The result proposals. */
	private ArrayList<TemplateProposal> fProposals = new ArrayList<>();
	/** Positions created on the key documents to remove in reset. */
	private final Map<IDocument, Position> fPositions = new HashMap<>();

	/**
	 * Creates the template engine for a particular context type. See
	 * <code>TemplateContext</code> for supported context types.
	 */
	public PhaserTemplateEngine(TemplateContextType contextType) {
		Assert.isNotNull(contextType);
		fContextType = contextType;
	}

	/**
	 * Empties the collector.
	 */
	public void reset() {
		fProposals.clear();
		for (Iterator<?> it = fPositions.entrySet().iterator(); it.hasNext();) {
			Entry<?, ?> entry = (Entry<?, ?>) it.next();
			IDocument doc = (IDocument) entry.getKey();
			Position position = (Position) entry.getValue();
			doc.removePosition(position);
		}
		fPositions.clear();
	}

	/**
	 * Returns the array of matching templates.
	 */
	public TemplateProposal[] getResults() {
		return fProposals.toArray(new TemplateProposal[fProposals.size()]);
	}

	/**
	 * Inspects the context of the compilation unit around
	 * <code>completionPosition</code> and feeds the collector with proposals.
	 * 
	 * @param viewer
	 *            the text viewer
	 * @param completionPosition
	 *            the context position in the document of the text viewer
	 * @param compilationUnit
	 *            the compilation unit (may be <code>null</code>)
	 */
	public void complete(ITextViewer viewer, int completionPosition,
			IJavaScriptUnit compilationUnit) {
		IDocument document = viewer.getDocument();

		if (!(fContextType instanceof CompilationUnitContextType))
			return;

		Point selection = viewer.getSelectedRange();
		Position position = new Position(completionPosition, selection.y);

		// remember selected text
		String selectedText = null;
		if (selection.y != 0) {
			try {
				selectedText = document.get(selection.x, selection.y);
				document.addPosition(position);
				fPositions.put(document, position);
			} catch (BadLocationException e) {
				// nothing
			}
		}

		CompilationUnitContext context = ((CompilationUnitContextType) fContextType)
				.createContext(document, position, compilationUnit);
		context.setVariable("selection", selectedText); //$NON-NLS-1$
		int start = context.getStart();
		int end = context.getEnd();

		IRegion region = new Region(start, end - start);

		Template[] templates = PHASER_TEMPLATES;

		if (selection.y == 0) {
			for (int i = 0; i != templates.length; i++) {
				Template template = templates[i];
				if (context.canEvaluate(template)) {
					TemplateProposal proposal = new TemplateProposal(template,
							context, region,
							JavaPluginImages
									.get(JavaPluginImages.IMG_OBJS_TEMPLATE));
					proposal.setDisplayString(template.getDescription());
					fProposals.add(proposal);
				}
			}

		} else {

			if (context.getKey().length() == 0)
				context.setForceEvaluation(true);

			boolean multipleLinesSelected = areMultipleLinesSelected(viewer);

			for (int i = 0; i != templates.length; i++) {
				Template template = templates[i];
				if (context.canEvaluate(template)
						&& template.getContextTypeId().equals(
								context.getContextType().getId())
						&& (!multipleLinesSelected
								&& template.getPattern().indexOf(
										$_WORD_SELECTION) != -1 || (multipleLinesSelected && template
								.getPattern().indexOf($_LINE_SELECTION) != -1))) {
					fProposals.add(new TemplateProposal(templates[i], context,
							region, JavaPluginImages
									.get(JavaPluginImages.IMG_OBJS_TEMPLATE)));
				}
			}
		}
	}

	/**
	 * Returns <code>true</code> if one line is completely selected or if
	 * multiple lines are selected. Being completely selected means that all
	 * characters except the new line characters are selected.
	 *
	 * @return <code>true</code> if one or multiple lines are selected
	 * 
	 */
	private static boolean areMultipleLinesSelected(ITextViewer viewer) {
		if (viewer == null)
			return false;

		Point s = viewer.getSelectedRange();
		if (s.y == 0)
			return false;

		try {

			IDocument document = viewer.getDocument();
			int startLine = document.getLineOfOffset(s.x);
			int endLine = document.getLineOfOffset(s.x + s.y);
			IRegion line = document.getLineInformation(startLine);
			return startLine != endLine
					|| (s.x == line.getOffset() && s.y == line.getLength());

		} catch (BadLocationException x) {
			return false;
		}
	}
}