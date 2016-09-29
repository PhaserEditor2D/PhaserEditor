// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.ui;

import static java.lang.System.out;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import javafx.scene.image.Image;

/**
 * @author arian
 *
 */
public class ImageCache {
	static class Container<T> {
		public T value;
		public long token;
	}

	private static Map<IFile, Container<Image>> _fxcache = new HashMap<>();
	private static IResourceChangeListener _workspaceListener;

	public static Image getFXImage(IFile file) {

		if (_workspaceListener == null) {
			_workspaceListener = createWorkspaceListener();
			ResourcesPlugin.getWorkspace().addResourceChangeListener(_workspaceListener,
					IResourceChangeEvent.POST_CHANGE);
		}

		synchronized (_fxcache) {
			long t = file.getModificationStamp();
			if (_fxcache.containsKey(file)) {
				Container<Image> c = _fxcache.get(file);
				if (t != c.token) {
					c.token = t;
					c.value = new Image("file:" + file.getLocation().makeAbsolute().toOSString(), true);
				}
				return c.value;
			}
			Container<Image> c = new Container<>();
			c.token = t;
			c.value = new Image("file:" + file.getLocation().makeAbsolute().toOSString(), true);
			_fxcache.put(file, c);
			return c.value;
		}
	}

	private static IResourceChangeListener createWorkspaceListener() {
		return new IResourceChangeListener() {

			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				try {
					event.getDelta().accept((delta) -> {
						IResource res = delta.getResource();
						if (delta.getKind() == IResourceDelta.REMOVED && res instanceof IFile) {
							unloadFile((IFile) res);
						}
						return true;
					});
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		};
	}

	static void unloadFile(IFile file) {
		out.println("ImageCache: unload " + file);
		synchronized (_fxcache) {
			_fxcache.remove(file);
		}
	}

}
