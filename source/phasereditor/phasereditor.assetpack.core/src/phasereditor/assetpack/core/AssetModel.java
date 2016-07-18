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
package phasereditor.assetpack.core;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.json.JSONException;
import org.json.JSONObject;

import phasereditor.inspect.core.jsdoc.PhaserJSDoc;
import phasereditor.inspect.core.jsdoc.PhaserMember;
import phasereditor.inspect.core.jsdoc.PhaserMethod;
import phasereditor.inspect.core.jsdoc.PhaserType;
import phasereditor.inspect.core.jsdoc.PhaserVariable;

public abstract class AssetModel implements IAssetKey, IAdaptable {
	private static Map<AssetType, PhaserMethod> _methodMap = new HashMap<>();

	static {
		PhaserJSDoc jsdoc = PhaserJSDoc.getInstance();
		PhaserType phaserType = jsdoc.getTypesMap().get("Phaser.Loader");

		// the phaserType can be null if the phaser version is wrong.
		if (phaserType != null) {

			Map<String, PhaserMember> map = phaserType.getMemberMap();

			for (AssetType assetType : AssetType.values()) {
				PhaserMethod method = (PhaserMethod) map.get(assetType.name());
				_methodMap.put(assetType, method);
			}
		}
	}

	@Override
	public AssetModel findFreshVersion() {
		if (isFreshVersion()) {
			return this;
		}
		
		AssetPackModel pack = getPack();
		if (!pack.isFreshVersion()) {
			return null;
		}
		
		AssetSectionModel section = pack.findSection(_section.getKey());
		if (section == null) {
			return null;
		}
		
		AssetModel asset = section.findAsset(_key);
		
		return asset;
	}
	
	@Override
	public AssetModel getAsset() {
		return this;
	}

	public static String getHelp(AssetType type) throws JSONException {
		PhaserMethod method = _methodMap.get(type);

		if (method == null) {
			return "<Help not found>";
		}

		String help = method.getHelp();
		return help.replace("\\n", "\n");
	}

	public static String getHelp(AssetType type, String parameter) throws JSONException {
		PhaserMethod method = _methodMap.get(type);

		if (method == null) {
			return "<Help not found>";
		}

		PhaserVariable arg = method.getArgsMap().get(parameter);

		if (arg == null) {
			return "<Help not found>";
		}

		return arg.getHelp().replace("\\n", "\n");
	}

	public String getHelp(String parameter) throws JSONException {
		return getHelp(getType(), parameter);
	}

	private String _key;
	private AssetType _type;
	private String _help;
	private AssetSectionModel _section;
	private IFile[] _lastUsedFiles;

	public AssetModel(String key, AssetType type, AssetSectionModel section) throws JSONException {
		_key = key;
		_type = type;
		_section = section;
		_help = getHelp(_type);
		_lastUsedFiles = new IFile[0];
	}

	public AssetModel(JSONObject jsonDef, AssetSectionModel section) throws JSONException {
		this(jsonDef.getString("key"), readAssetType(jsonDef), section);
	}

	public final JSONObject toJSON() {
		JSONObject obj = new JSONObject();
		writeParameters(obj);
		return obj;
	}

	protected void writeParameters(JSONObject obj) {
		obj.put("type", _type.name());
		obj.put("key", _key);
	}

	public static AssetType readAssetType(JSONObject definition) throws JSONException {
		return AssetType.valueOf(definition.getString("type"));
	}

	@Override
	public String getKey() {
		return _key;
	}

	public void setKey(String key) {
		_key = key;
		firePropertyChange("key");
		AssetPackModel model = getPack();
		if (model != null) {
			model.firePropertyChange(AssetPackModel.PROP_ASSET_KEY);
		}
	}

	public AssetType getType() {
		return _type;
	}

	public String getHelp() {
		return _help;
	}

	public AssetSectionModel getSection() {
		return _section;
	}

	public void setSection(AssetSectionModel section) {
		Assert.isNotNull(section);
		_section = section;
		firePropertyChange("section");
	}

	public AssetGroupModel getGroup() {
		return getSection().getGroup(_type);
	}

	public AssetPackModel getPack() {
		return _section.getPack();
	}

	@Override
	public boolean isFreshVersion() {
		AssetPackModel pack = getPack();
		return pack.isFreshVersion() && pack.getSections().contains(_section) && _section.getAssets().contains(this);
	}

	protected IContainer getUrlStartFolder() {
		return getPack().getAssetsFolder();
	}

	public IFile getFileFromUrl(String url) {
		if (url == null || url.length() == 0) {
			return null;
		}

		IContainer startFolder = getUrlStartFolder();
		IContainer parent = startFolder instanceof IProject ? startFolder : startFolder.getParent();

		IPath path = parent.getProjectRelativePath().append(url);

		IFile file = parent.getProject().getFile(path);

		if (!file.exists()) {
			return null;
		}

		return file;
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
		getPack().setDirty(true);
	}

	@SuppressWarnings("static-method")
	public IFile[] getUsedFiles() {
		return new IFile[0];
	}

	public IFile[] getLastUsedFiles() {
		return _lastUsedFiles;
	}

	public List<IFile> getFilesFromUrls(String... urls) {
		return getFilesFromUrls(Arrays.asList(urls));
	}

	public List<IFile> getFilesFromUrls(List<String> urls) {
		List<IFile> files = new ArrayList<>();
		for (String url : urls) {
			IFile file = getFileFromUrl(url);
			if (file != null) {
				files.add(file);
			}
		}
		return files;
	}

	public String getUrlFromFile(IFile file) {
		return getPack().getAssetUrl(file);
	}

	public List<String> getUrlsFromFiles(List<IFile> files) {
		List<String> urls = new ArrayList<>();
		for (IFile file : files) {
			String url = getUrlFromFile(file);
			urls.add(url);
		}
		return urls;
	}

	@SuppressWarnings("static-method")
	public List<? extends IAssetElementModel> getSubElements() {
		return Collections.emptyList();
	}

	/**
	 * Return <code>null</code> if the given string is <code>null</code> or the
	 * empty string.
	 * 
	 * @param str
	 *            String to normalize.
	 * @return The string or null.
	 */
	protected static String normalizeString(String str) {
		return str == null || str.trim().length() == 0 ? null : str;
	}

	/**
	 * Build the internal state and validate it.
	 * 
	 * @param problems
	 *            Validation problems.
	 */
	public final void build(List<IStatus> problems) {
		_lastUsedFiles = getUsedFiles();
		internalBuild(problems);
	}

	protected abstract void internalBuild(List<IStatus> problems);

	protected void validateUrlAndData(List<IStatus> problems, String urlParam, String url, String dataParam,
			String data) {
		boolean fileOk = false;
		boolean dataOk = false;

		if (normalizeString(data) != null) {
			dataOk = true;
		}

		// do not validate HTTP
		if (url != null && url.startsWith("http")) {
			fileOk = true;
		} else {
			if (!dataOk) {
				if (url == null || url.length() == 0) {
					fileOk = false;
				} else {
					fileOk = true;
					IFile file = getFileFromUrl(url);
					if (file == null) {
						String msg = "The file of '" + urlParam + "' ('" + url
								+ "'), is not found in asset pack entry: '" + _section.getKey() + "/" + _type.name()
								+ "/" + _key + "'";
						problems.add(errorStatus(msg));
					}
				}
			}
		}
		if (!dataOk && !fileOk) {
			String msg = "Missing '" + urlParam + "' or '" + dataParam + "' in asset pack entry: '" + _section.getKey()
					+ "/" + _type.name() + "/" + _key + "'";
			problems.add(errorStatus(msg));
		}
	}

	protected void validateUrl(List<IStatus> problems, String param, String url) {
		// do not validate HTTP
		if (url == null || url.startsWith("http")) {
			return;
		}

		IFile file = getFileFromUrl(url);

		if (file == null) {
			String msg = "The file of '" + param + "' ('" + url + "') is not found in asset pack entry: '"
					+ _section.getKey() + "/" + _type.name() + "/" + _key + "'";
			problems.add(errorStatus(msg));
		}
	}

	protected void validateUrlList(List<IStatus> problems, String param, List<String> urls) {
		if (urls == null || urls.isEmpty()) {
			problems.add(errorStatus("Missing the values of '" + param + "' in asset pack entry '" + _section.getKey()
					+ "/" + _type.name() + "/" + _key + "'."));
		} else {
			for (String url : urls) {
				validateUrl(problems, param, url);
			}
		}
	}

	public class AssetStatus extends Status {

		public AssetStatus(int severity, String message) {
			super(severity, Activator.PLUGIN_ID, message);
		}

		public AssetModel getAsset() {
			return AssetModel.this;
		}

	}

	protected Status errorStatus(String msg) {
		return new AssetStatus(IStatus.ERROR, msg);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}
}
