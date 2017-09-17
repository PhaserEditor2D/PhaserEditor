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
package phasereditor.webrun.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;

/**
 * @author arian
 *
 */
public class GamePlayerEditorInput implements IEditorInput {

	private String _url;
	private IProject _project;
	private Object[] _device;
	private boolean _rotated;

	public GamePlayerEditorInput(IProject project) {
		_url = WebRunUI.getProjectBrowserURL(project);
		_project = project;
	}
	
	public void setRotated(boolean rotated) {
		_rotated = rotated;
	}
	
	public boolean isRotated() {
		return _rotated;
	}
	
	public Object[] getDevice() {
		return _device;
	}
	
	public void setDevice(Object[] device) {
		_device = device;
	}

	public String getProjectName() {
		return _project.getName();
	}

	public IProject getProject() {
		return _project;
	}

	public String getUrl() {
		return _url;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.equals(IProject.class)) {
			return adapter.cast(_project);
		}
		return null;
	}

	@Override
	public boolean exists() {
		return _project.exists();
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_WORLD_PAGE_WHITE);
	}

	@Override
	public String getName() {
		return _project.getName() + " (experimental)";
	}

	@Override
	public IPersistableElement getPersistable() {
		return new IPersistableElement() {
			
			@SuppressWarnings({ "synthetic-access", "boxing" })
			@Override
			public void saveState(IMemento memento) {
				memento.putString("phasereditor.webrun.ui.gameplayer.project", _project.getName());
				if (_device != null) {
					memento.putString("phasereditor.webrun.ui.gameplayer.device.name", (String)_device[0]);
					memento.putInteger("phasereditor.webrun.ui.gameplayer.device.width", (int)_device[1]);
					memento.putInteger("phasereditor.webrun.ui.gameplayer.device.height", (int)_device[2]);
					memento.putBoolean("phasereditor.webrun.ui.gameplayer.rotated", _rotated);
				}
			}
			
			@Override
			public String getFactoryId() {
				return "phasereditor.webrun.ui.gameplayerFactory";
			}
		};
	}

	@Override
	public String getToolTipText() {
		return "Running " + getName();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_project == null) ? 0 : _project.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GamePlayerEditorInput other = (GamePlayerEditorInput) obj;
		if (_project == null) {
			if (other._project != null)
				return false;
		} else if (!_project.equals(other._project))
			return false;
		return true;
	}

}
