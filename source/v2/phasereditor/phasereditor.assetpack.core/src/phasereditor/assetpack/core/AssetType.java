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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;

import phasereditor.inspect.core.jsdoc.IJsdocProvider;
import phasereditor.inspect.core.jsdoc.IPhaserMember;
import phasereditor.inspect.core.jsdoc.PhaserJsdocModel;
import phasereditor.inspect.core.jsdoc.PhaserMemberJsdocProvider;
import phasereditor.inspect.core.jsdoc.PhaserMethod;
import phasereditor.inspect.core.jsdoc.PhaserType;

public enum AssetType implements IAssetPackEelement, IAdaptable {
	image("png"), spritesheet("png"), atlas("json"), atlasXML("xml"), multiatlas("json"), audio("mp3"), audioSprite(
			"json"), video("mp4"), tilemap("json"), bitmapFont("xml"), physics(
					"json"), text("txt"), json("json"), xml("xml"), script("js"), shader("glsl"), binary("");

	private AssetType(String fileExt) {
		_fileExt = fileExt;
	}

	public String capitalName() {
		return name().substring(0, 1).toUpperCase() + name().substring(1);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == IJsdocProvider.class) {
			return new PhaserMemberJsdocProvider(getPhaserMethod());
		}

		return null;
	}

	private static Set<String> _allowedNames;

	static {
		_allowedNames = new HashSet<>();
		for (var value : values()) {
			_allowedNames.add(value.name());
		}
	}

	public static boolean isTypeSupported(String name) {
		return _allowedNames.contains(name);
	}

	private static Map<AssetType, PhaserMethod> _methodMap;

	public static Map<AssetType, PhaserMethod> getMethodMap() {
		if (_methodMap == null) {
			_methodMap = new HashMap<>();
			PhaserJsdocModel jsdoc = PhaserJsdocModel.getInstance();
			PhaserType phaserType = jsdoc.getContainerMap().get("Phaser.Loader.LoaderPlugin").castType();

			// the phaserType can be null if the phaser version is wrong.
			if (phaserType != null) {

				Map<String, IPhaserMember> map = phaserType.getMemberMap();

				for (AssetType assetType : AssetType.values()) {
					PhaserMethod method = (PhaserMethod) map.get(assetType.name());
					_methodMap.put(assetType, method);
				}
			}

		}
		return _methodMap;
	}

	public PhaserMethod getPhaserMethod() {
		return getMethodMap().get(this);
	}

	private String _fileExt;

	public String getFileExtension() {
		return _fileExt;
	}

}