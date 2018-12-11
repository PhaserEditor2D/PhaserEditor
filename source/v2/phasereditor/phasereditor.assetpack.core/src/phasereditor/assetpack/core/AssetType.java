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
import java.util.List;
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
	image("png", "Image"),

	svg("svg", "SVG Image"),
	
	spritesheet("png", "Sprite-Sheet"),

	atlas("json", "Atlas"),

	atlasXML("xml", "Atlas (XML)"),

	unityAtlas("meta", "Atlas (Unity)"),
	
	multiatlas("json", "Atlas (Multi)"),

	animation("json", "Animations"),

	audio("mp3", "Audio"),

	audioSprite("json", "Audio Sprite"),

	video("mp4", "Video"),

	tilemapCSV("csv", "Tilemap (CSV)"),

	tilemapTiledJSON("json", "Tilemap (Tiled)"),

	tilemapImpact("json", "Tilemap (Impact)"),
	
	bitmapFont("xml", "Bitmap Font"), 
	
	physics("json", "Physics"), 
	
	text("txt", "Text File"), 
	
	json("json", "JSON File"), 
	
	xml("xml", "XML File"), 
	
	script("js", "JavaScript File"),
	
	plugin("js", "Phaser Plugin File"),
	
	scenePlugin("js", "Phaser Scene Plugin"),
	
	html("html", "HTML File"),
	
	glsl("glsl", "Shader File"), 
	
	binary("dat", "Binary File");

	
	private AssetType(String fileExt, String capitalName) {
		_fileExt = fileExt;
		_capitalName = capitalName;
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
		_allowedNames.removeAll(List.of(video.name(), physics.name()));
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

	private final String _fileExt;
	private final String _capitalName;


	public String getFileExtension() {
		return _fileExt;
	}
	
	public String getCapitalName() {
		return _capitalName;
	}

}