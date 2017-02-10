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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * @author arian
 *
 */
public abstract class FileDataCache<TData> {
	private Map<IProject, Map<IPath, TData>> _cache;

	public FileDataCache() {
		_cache = new HashMap<>();
	}

	public synchronized void buildProject(IProject project) throws CoreException {
		Map<IPath, TData> map = new HashMap<>();
		IContainer webContent = ProjectCore.getWebContentFolder(project);
		webContent.accept(new IResourceVisitor() {

			@Override
			public boolean visit(IResource resource) throws CoreException {
				if (resource instanceof IFile) {
					TData data = createData((IFile) resource);
					if (data != null) {
						map.put(resource.getFullPath(), data);
					}
				}
				return true;
			}
		});
		_cache.put(project, map);
	}

	public synchronized void buildDelta(IProject project, IResourceDelta delta) throws CoreException {
		Map<IPath, TData> map = getProjectMap(project);

		delta.accept(new IResourceDeltaVisitor2() {

			@Override
			public void fileAdded(IFile file) {
				map.put(file.getFullPath(), createData(file));
			}

			@Override
			public void fileRemoved(IFile file) {
				map.remove(file.getFullPath());
			}

			@Override
			public void fileMovedTo(IFile file, IPath movedFromPath, IPath movedToPath) {
				TData data = map.remove(movedFromPath);
				map.put(movedToPath, data);
			}

			@Override
			public void fileChanged(IFile file) {
				map.put(file.getFullPath(), createData(file));
			}

		});
	}

	public synchronized void clean(IProject project) {
		_cache.remove(project);
	}

	public synchronized List<TData> getProjectData(IProject project) {
		Map<IPath, TData> map = _cache.get(project);
		
		if (map == null) {
			return Collections.emptyList();
		}
		Collection<TData> values = map.values();
		return new ArrayList<>(values);
	}

	public synchronized TData getFileData(IFile file) {
		Map<IPath, TData> map = _cache.get(file.getProject());

		if (map == null) {
			return null;
		}

		TData data = map.get(file);

		return data;
	}

	private Map<IPath, TData> getProjectMap(IProject project) {
		return _cache.computeIfAbsent(project, p -> new HashMap<>());
	}

	/**
	 * Create the data associated to the file. Return null the file is not a
	 * candidate to be cached (a way to filter the files).
	 * 
	 * @param file
	 * @return The data of the file or null if the file has to be ignored.
	 */
	public abstract TData createData(IFile file);

}
