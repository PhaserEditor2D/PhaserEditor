// The MIT License (MIT)
//
// Copyright (c) 2016 Arian Fornaris
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
package phasereditor.canvas.core;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.json.JSONObject;

/**
 * @author arian
 *
 */
public class WorldModel extends GroupModel {
	public static final String PROP_STRUCTURE = "structure";
	public static final String PROP_DIRTY = "dirty";

	private boolean _dirty;
	private IFile file;

	public WorldModel() {
		super(null);
		// setEditorName("world");
		init();
	}

	public WorldModel(IFile file, JSONObject data) throws Exception {
		this(data);
		this.file = file;
	}

	private WorldModel(JSONObject data) {
		super(null, data);
		init();
	}
	
	@Override
	public boolean isWorldModel() {
		return true;
	}

	public IFile getFile() {
		return file;
	}

	public void setFile(IFile file) {
		this.file = file;
	}

	public String getClassName() {
		if (this.file == null) {
			return "CanvasWorld";
		}

		String name = file.getName();
		String ext = file.getFileExtension();
		int end = name.length() - ext.length() - 1;
		return name.substring(0, end);
	}

	@Override
	public String getLabel() {
		return "[wrd] " + getEditorName();
	}

	private void init() {
		_dirty = false;

		addPropertyChangeListener(e -> {
			if (e.getPropertyName() != PROP_DIRTY) {
				setDirty(true);
			}
		});
	}

	public boolean isDirty() {
		return _dirty;
	}

	public void setDirty(boolean dirty) {
		_dirty = dirty;
		firePropertyChange(PROP_DIRTY);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public enum ZOperation {
		RISE {
			@Override
			public boolean perform(List list, Object elem) {
				int i = list.indexOf(elem);
				if (i < list.size() - 1) {
					list.remove(elem);
					list.add(i + 1, elem);
					return true;
				}
				return false;
			}
		},
		LOWER {
			@Override
			public boolean perform(List list, Object elem) {
				int i = list.indexOf(elem);
				if (i > 0) {
					list.remove(elem);
					list.add(i - 1, elem);
					return true;
				}
				return false;
			}
		},
		RISE_TOP {
			@Override
			public boolean perform(List list, Object elem) {
				if (list.size() > 1) {
					list.remove(elem);
					list.add(elem);
					return true;
				}
				return false;
			}
		},
		LOWER_BOTTOM {
			@Override
			public boolean perform(List list, Object elem) {
				if (list.size() > 1) {
					list.remove(elem);
					list.add(0, elem);
					return true;
				}
				return false;
			}
		};
		public abstract boolean perform(List list, Object elem);
	}

	private transient final PropertyChangeSupport _support = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(PropertyChangeListener l) {
		_support.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		_support.removePropertyChangeListener(l);
	}

	public void addPropertyChangeListener(String property, PropertyChangeListener l) {
		_support.addPropertyChangeListener(property, l);
	}

	public void removePropertyChangeListener(String property, PropertyChangeListener l) {
		_support.removePropertyChangeListener(property, l);
	}

	public void firePropertyChange(String property) {
		_support.firePropertyChange(property, true, false);
	}

	/**
	 * @param editorName
	 * @return
	 */
	public String createName(String basename) {
		int i = 1;
		String name = basename;
		while (true) {
			if (findByName(name) == null) {
				break;
			}
			name = basename + i;
			i++;
		}
		return name;
	}
}
