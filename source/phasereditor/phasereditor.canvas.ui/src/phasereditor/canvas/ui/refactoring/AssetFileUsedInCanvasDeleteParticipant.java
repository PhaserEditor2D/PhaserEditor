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
package phasereditor.canvas.ui.refactoring;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.FindAssetReferencesResult;
import phasereditor.assetpack.core.IAssetConsumer;
import phasereditor.assetpack.ui.refactorings.AssetFileDeleteParticipant;

/**
 * @author arian
 *
 */
public class AssetFileUsedInCanvasDeleteParticipant extends AssetFileDeleteParticipant {

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {

		RefactoringStatus status = new RefactoringStatus();

		String relpath = _file.getProjectRelativePath().toPortableString();

		Set<IFile> files = new LinkedHashSet<>();
		for (AssetModel asset : _result) {
			for (IAssetConsumer consumer : AssetPackCore.requestAssetConsumers()) {
				FindAssetReferencesResult refs = consumer.getAssetReferences(asset, pm);
				for (IFile file : refs.getFiles()) {
					if (!files.contains(file)) {
						files.add(file);

						status.addWarning(
								"The file '" + file.getName() + "' indirectly uses the file '" + relpath + "'.",
								new CanvasFileRefactoringStatusContext(file));
					}
				}
			}
		}

		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {

		// TODO: in a future we could ask if you want to delete the objects
		// using the asset.

		return new NullChange();
	}

}
