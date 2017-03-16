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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.json.JSONObject;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.IAssetKey;

/**
 * @author arian
 *
 */
public class WorldModel extends GroupModel {
	public static final String PROP_STRUCTURE = "structure";
	public static final String PROP_DIRTY = "dirty";

	private boolean _dirty;
	private AssetTable _assetTable;
	private PrefabTable _prefabTable;
	private CanvasModel _canvasModel;

	public WorldModel(CanvasModel canvasModel) {
		super(null);
		_canvasModel = canvasModel;
		init();
	}

	public AssetTable getAssetTable() {
		return _assetTable;
	}

	public void setAssetTable(AssetTable assetTable) {
		_assetTable = assetTable;
	}

	public PrefabTable getPrefabTable() {
		return _prefabTable;
	}

	public void setPrefabTable(PrefabTable prefabTable) {
		_prefabTable = prefabTable;
	}

	public IProject getProject() {
		return getFile().getProject();
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
		return _canvasModel.getFile();
	}

	public CanvasModel getCanvasModel() {
		return _canvasModel;
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

		_assetTable = new AssetTable(this);
		_prefabTable = new PrefabTable(this);
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
		int count = 1;
		String basename2 = basename;
		
		// remove trailing numbers
		for(int i = basename.length() - 1; i > 0 ; i--) {
			if (!Character.isDigit(basename.charAt(i))) {
				basename2 = basename.substring(0, i + 1);
				break;
			}
		}
		
		String name = basename2;
		
		while (true) {
			if (findByName(name) == null) {
				break;
			}
			name = basename2 + count;
			count++;
		}
		return name;
	}

	public BaseObjectModel findFirstSprite() {
		List<BaseObjectModel> list = new ArrayList<>(getChildren());
		Collections.reverse(list);
		for (BaseObjectModel obj : list) {
			if (obj instanceof AssetSpriteModel<?>) {
				return obj;
			} else if (obj instanceof MissingAssetSpriteModel) {
				return obj;
			} else if (obj instanceof MissingPrefabModel) {
				return obj;
			}
		}
		return null;
	}

	public GroupModel findGroupPrefabRoot() {
		if (getChildren().isEmpty()) {
			return null;
		}

		BaseObjectModel obj = getChildren().get(0);

		if (obj instanceof GroupModel) {
			return (GroupModel) obj;
		}

		return null;
	}

	private BiFunction<IProject, JSONObject, Object> _findAssetFunction = (project,
			assetRef) -> (IAssetKey) AssetPackCore.findAssetElement(getProject(), assetRef);

	public BiFunction<IProject, JSONObject, Object> getFindAssetFunction() {
		return _findAssetFunction;
	}

	/**
	 * This function is used for open the asset registry. It means, it can look
	 * for assets in the shared model or any other model provided by the user.
	 * 
	 * @param assetRef
	 * @return
	 */
	public void setFindAssetFunction(BiFunction<IProject, JSONObject, Object> findAssetFunction_project_assetRef) {
		_findAssetFunction = findAssetFunction_project_assetRef;
	}

	Object findAssetElement(JSONObject assetRef) {
		return _findAssetFunction.apply(getProject(), assetRef);
	}
}
