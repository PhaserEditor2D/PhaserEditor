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
package phasereditor.project.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * @author arian
 *
 */
public interface IResourceDeltaVisitor2 extends IResourceDeltaVisitor {

	@Override
	default boolean visit(IResourceDelta delta) throws CoreException {
		IResource resource = delta.getResource();

		if (resource instanceof IFile) {
			IFile file = (IFile) resource;
			int kind = delta.getKind();

			switch (kind) {
			case IResourceDelta.ADDED:
				fileAdded(file);
				break;
			case IResourceDelta.MOVED_TO:
				fileMovedTo(file, delta.getMovedFromPath(), delta.getMovedToPath());
				break;
			case IResourceDelta.REMOVED:
				fileRemoved(file);
				break;
			case IResourceDelta.CHANGED:
				fileChanged(file);
				break;
			default:
				break;
			}
		}

		return true;
	}

	public void fileAdded(IFile file);

	public void fileRemoved(IFile file);

	public void fileMovedTo(IFile file, IPath movedFromPath, IPath movedToPath);

	public void fileChanged(IFile file);
}
