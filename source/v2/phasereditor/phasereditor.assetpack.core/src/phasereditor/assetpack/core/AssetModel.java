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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.json.JSONException;
import org.json.JSONObject;

import phasereditor.inspect.core.jsdoc.IJsdocProvider;
import phasereditor.inspect.core.jsdoc.PhaserMemberJsdocProvider;
import phasereditor.inspect.core.jsdoc.PhaserMethod;
import phasereditor.inspect.core.jsdoc.PhaserVariable;
import phasereditor.project.core.ProjectCore;

public abstract class AssetModel implements IAssetKey, IEditableKey, IAdaptable {

	@Override
	public boolean isSharedVersion() {
		AssetPackModel pack = getPack();
		return pack.isSharedVersion() && pack.getSections().contains(_section) && _section.getAssets().contains(this);
	}
	
	public abstract void readInfo(JSONObject data);

	@Override
	public final AssetModel getSharedVersion() {
		if (isSharedVersion()) {
			return this;
		}

		try {
			AssetPackModel pack = getPack().getSharedVersion();

			if (pack == null) {
				return null;
			}

			AssetSectionModel section = pack.findSection(_section.getKey());
			if (section == null) {
				return null;
			}

			AssetModel asset = section.findAsset(_key);

			return asset;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public AssetModel getAsset() {
		return this;
	}

	public static String getHelp(AssetType type) throws JSONException {
		PhaserMethod method = AssetType.getMethodMap().get(type);

		if (method == null) {
			return "<Help not found>";
		}

		String help = method.getHelp();
		return help.replace("\\n", "\n");
	}

	public static String getHelp(AssetType type, String parameter) throws JSONException {
		PhaserMethod method = type.getPhaserMethod();

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

	public static AssetType readAssetType(JSONObject jsonData) throws JSONException {
		String name = jsonData.getString("type");
		return AssetType.valueOf(name);
	}

	@Override
	public String getKey() {
		return _key;
	}

	public String getId() {
		return getPack().getName() + "." + getSection().getKey() + "." + _key;
	}

	@Override
	public void setKey(String key) {
		setKey(key, true);
	}

	public void setKey(String key, boolean notify) {
		_key = key;
		if (notify) {
			AssetPackModel model = getPack();
			if (model != null) {
				model.firePropertyChange(AssetPackModel.PROP_ASSET_KEY);
			}
		}
	}

	public AssetType getType() {
		return _type;
	}

	public String getHelp() {
		if (_help == null) {
			_help = getHelp(_type);
		}
		return _help;
	}

	public AssetSectionModel getSection() {
		return _section;
	}

	public void setSection(AssetSectionModel section) {
		Assert.isNotNull(section);
		_section = section;
	}

	public AssetGroupModel getGroup() {
		return getPack().getGroup(_type);
	}

	public AssetPackModel getPack() {
		return _section.getPack();
	}

	public void updateUrlsListWithFileChange(IFile file, IFile newFile, List<String> _urls) {
		String url = getUrlFromFile(file);
		String newUrl = getUrlFromFile(newFile);
		for (int i = 0; i < _urls.size(); i++) {
			String url2 = _urls.get(i);
			if (url.equals(url2)) {
				_urls.set(i, newUrl);
			}
		}
	}
	
	public IFile getFileFromUrl(String url) {
		return getPack().getFileFromUrl(url);
	}

	public IFolder getFolderFromUrl(String url) {
		if (url == null || url.length() == 0) {
			return null;
		}

		IContainer webContentFolder = getPack().getWebContentFolder();
		var folder = webContentFolder.getFolder(new Path(url));

		if (!folder.exists()) {
			return null;
		}

		return folder;
	}

	@SuppressWarnings("static-method")
	public IFile[] computeUsedFiles() {
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

	@SuppressWarnings("static-method")
	public String getUrlFromFile(IFile file) {
		return ProjectCore.getAssetUrl(file);
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
		_lastUsedFiles = computeUsedFiles();

		internalBuild(problems == null ? new ArrayList<>() : problems);

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
		if (adapter == IJsdocProvider.class) {
			return new PhaserMemberJsdocProvider(getType().getPhaserMethod());
		}

		return null;
	}

	public boolean touched(IResourceDelta delta) {
		Set<IFile> list = new HashSet<>();

		AssetModel asset = getAsset();
		list.add(asset.getPack().getFile());
		list.addAll(Arrays.asList(asset.computeUsedFiles()));
		list.addAll(Arrays.asList(asset.getLastUsedFiles()));

		return ProjectCore.areFilesAffectedByDelta(delta, list);
	}

	public abstract void fileChanged(IFile file, IFile newFile);

	public AssetModel copy(AssetSectionModel section) {
		try {
			JSONObject jsonAsset = toJSON();
			AssetType type = AssetModel.readAssetType(jsonAsset);
			AssetFactory factory = AssetFactory.getFactory(type);
			AssetModel asset;
			asset = factory.createAsset(jsonAsset, section);
			return asset;
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}
}
