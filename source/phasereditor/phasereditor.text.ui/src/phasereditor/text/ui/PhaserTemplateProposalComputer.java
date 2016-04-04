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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.internal.corext.template.java.JavaContextType;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.text.template.contentassist.TemplateProposal;
import org.eclipse.wst.jsdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.wst.jsdt.ui.text.java.JavaContentAssistInvocationContext;

@SuppressWarnings("restriction")
// copied from TemplateCompletionProposalComputer
public class PhaserTemplateProposalComputer implements
		IJavaCompletionProposalComputer {

	private final PhaserTemplateEngine fJavaTemplateEngine;

	public PhaserTemplateProposalComputer() {
		TemplateContextType contextType = JavaScriptPlugin.getDefault()
				.getTemplateContextRegistry()
				.getContextType(JavaContextType.NAME);
		// if (contextType == null) {
		// contextType = new JavaContextType();
		// JavaScriptPlugin.getDefault().getTemplateContextRegistry()
		// .addContextType(contextType);
		// }
		contextType = new PhaserJavaContextType();
		fJavaTemplateEngine = new PhaserTemplateEngine(contextType);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#
	 * computeCompletionProposals
	 * (org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public List<TemplateProposal> computeCompletionProposals(
			ContentAssistInvocationContext context, IProgressMonitor monitor) {
		PhaserTemplateEngine engine;
		engine = fJavaTemplateEngine;

		if (engine != null) {
			if (!(context instanceof JavaContentAssistInvocationContext))
				return Collections.emptyList();

			JavaContentAssistInvocationContext javaContext = (JavaContentAssistInvocationContext) context;
			IJavaScriptUnit unit = javaContext.getCompilationUnit();
			if (unit == null)
				return Collections.emptyList();

			engine.reset();
			engine.complete(javaContext.getViewer(),
					javaContext.getInvocationOffset(), unit);

			TemplateProposal[] templateProposals = engine.getResults();
			List<TemplateProposal> result = new ArrayList<>(
					Arrays.asList(templateProposals));

			return result;
		}

		return Collections.emptyList();
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#
	 * computeContextInformation
	 * (org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public List<?> computeContextInformation(
			ContentAssistInvocationContext context, IProgressMonitor monitor) {
		return Collections.emptyList();
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#
	 * getErrorMessage()
	 */
	@Override
	public String getErrorMessage() {
		return null;
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposalComputer#
	 * sessionStarted()
	 */
	@Override
	public void sessionStarted() {
		// nothing
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposalComputer#
	 * sessionEnded()
	 */
	@Override
	public void sessionEnded() {
		fJavaTemplateEngine.reset();
	}

}
