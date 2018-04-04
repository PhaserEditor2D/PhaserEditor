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
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.jsdt.core.CompletionContext;
import org.eclipse.wst.jsdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.wst.jsdt.ui.text.java.JavaContentAssistInvocationContext;

public abstract class BaseProposalComputer implements IJavaCompletionProposalComputer {

	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context,
			IProgressMonitor monitor) {
		if (context instanceof JavaContentAssistInvocationContext) {

			JavaContentAssistInvocationContext javaContext = (JavaContentAssistInvocationContext) context;

			CompletionContext coreContext = javaContext.getCoreContext();

			char[] token = coreContext.getToken();
			if (token != null) {
				IProject project = javaContext.getCompilationUnit().getJavaScriptProject().getProject();
				List<ProposalData> propDataList = computeProjectProposals(project);

				if (propDataList.size() > 0) {

					String prefix = new String(token).toLowerCase();

					List<ICompletionProposal> list = new ArrayList<>();

					for (ProposalData propData : propDataList) {
						String name = propData.getName().toLowerCase();

						int index = name.indexOf(prefix);
						if (index >= 0) {
							CompletionProposal proposal = createCompletionProposal(coreContext, propData);
							if (index == 0) {
								list.add(0, proposal);
							} else {
								list.add(proposal);
							}
						}
					}

					return list;
				}
			}
		}
		return Collections.emptyList();
	}

	protected abstract List<ProposalData> computeProjectProposals(IProject project);

	private static CompletionProposal createCompletionProposal(CompletionContext coreContext, ProposalData propData) {

		int len = propData.getName().length();

		int start = coreContext.getTokenStart();
		int end = coreContext.getTokenEnd();

		// -1/+1 because the string ("|') character.
		int replOffset = start + 1;
		int replLen = end - start - 1;
		int cursor = len + 1;

		CompletionProposal proposal = new CompletionProposal(propData, replOffset, replLen, cursor);

		Image img = propData.getImage();
		if (img != null) {
			proposal.setImage(img);
		}

		return proposal;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List computeContextInformation(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		List<String> list = new ArrayList<>();
		return list;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public void sessionStarted() {
		// nothing
	}

	@Override
	public void sessionEnded() {
		// nothing
	}
}
