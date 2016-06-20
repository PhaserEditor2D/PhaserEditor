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
package phasereditor.inspect.core.jsdoc;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.wst.jsdt.core.IMember;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.inspect.core.InspectCore;

public class PhaserJSDoc {
	private static PhaserJSDoc _instance;

	public synchronized static PhaserJSDoc getInstance() {
		if (_instance == null) {
			long t = currentTimeMillis();
			Path phaserVersionFolder = InspectCore.getPhaserVersionFolder();
			Path docsJsonFile = phaserVersionFolder.resolve("phaser-custom/jsdoc/docs.json").toAbsolutePath()
					.normalize();
			Path srcFolder = phaserVersionFolder.resolve("phaser-master/src");

			try {
				_instance = new PhaserJSDoc(srcFolder, docsJsonFile);
				out.println("Build Phaser JSDoc " + (currentTimeMillis() - t));
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		return _instance;
	}

	private Map<String, PhaserType> _typesMap;
	private Map<String, IPhaserMember> _membersMap;
	private List<PhaserConstant> _globalConstants;

	private Path _srcFolder;

	public PhaserJSDoc(Path srcFolder, Path docsJsonFile) throws IOException {
		_srcFolder = srcFolder;
		_globalConstants = new ArrayList<>();
		_membersMap = new HashMap<>();

		_typesMap = buildPhaserJSDoc(docsJsonFile);

		for (PhaserType type : _typesMap.values()) {
			String typeName1 = type.getName();
			String typeName2 = typeName1.replace(".", "_");

			for (String typeName : new String[] { typeName1, typeName2 }) {
				_membersMap.put(typeName, type);
				_membersMap.put(typeName + ".constructor", type);

				for (PhaserMember m : type.getMethods()) {
					_membersMap.put(typeName + "." + m.getName(), m);
				}

				for (PhaserMember m : type.getProperties()) {
					_membersMap.put(typeName + "." + m.getName(), m);
				}

				for (PhaserMember m : type.getConstants()) {
					_membersMap.put(typeName + "." + m.getName(), m);
				}
			}
		}

		for (PhaserConstant cons : _globalConstants) {
			_membersMap.put("Phaser." + cons.getName(), cons);
		}
	}

	public Path getTypePath(PhaserType type) {
		if (type == null) {
			// a global constant
			return _srcFolder.resolve("Phaser.js");
		}
		return _srcFolder.resolve(type.getFile());
	}

	public Map<String, PhaserType> getTypesMap() {
		return _typesMap;
	}

	public PhaserType getType(String name) {
		return _typesMap.get(name);
	}

	public Collection<PhaserType> getTypes() {
		return _typesMap.values();
	}

	public Map<String, IPhaserMember> getMembersMap() {
		return _membersMap;
	}

	public List<PhaserConstant> getGlobalConstants() {
		return _globalConstants;
	}

	public String getJSDoc(IMember member) {
		String name = InspectCore.getFullName(member);
		Object phaserMember = _membersMap.get(name);

		if (phaserMember == null) {
			out.println("<b>Cannot find JSDoc of " + name + "</b>");
			return "<b>Cannot find JSDoc of " + name + "</b>";
		}

		return JSDocRenderer.getInstance().render(phaserMember);
	}

	public boolean isPhaserMember(IMember member) {
		String name = InspectCore.getFullName(member);
		boolean b = _membersMap.containsKey(name);
		return b;
	}

	private Map<String, PhaserType> buildPhaserJSDoc(Path docsJsonFile) throws IOException {
		if (!Files.exists(docsJsonFile)) {
			return new HashMap<>();
		}

		try (InputStream input = Files.newInputStream(docsJsonFile)) {
			JSONArray jsdocElements = new JSONArray(new JSONTokener(input));

			// Set<String> kinds = new HashSet<>();
			// Set<String> scopes = new HashSet<>();
			// for (int i = 0; i < jsdocElements.length(); i++) {
			// JSONObject obj = jsdocElements.getJSONObject(i);
			// kinds.add(obj.getString("kind"));
			// if (obj.getString("kind").equals("namespace")) {
			// out.println(obj.toString(2));
			// }
			// scopes.add(obj.getString("scope"));
			// }
			// out.println("kinds: " + Arrays.toString(kinds.toArray()));
			// out.println("scopes: " + Arrays.toString(scopes.toArray()));
			// out.println();
			//
			// System.exit(0);

			Map<String, PhaserType> typeMap = new HashMap<>();

			// first pass to get all the classes in the map

			for (int i = 0; i < jsdocElements.length(); i++) {
				JSONObject obj = jsdocElements.getJSONObject(i);

				if (obj.getString("longname").contains("~")) {
					continue;
				}

				buildClass(obj, typeMap);
			}

			for (int i = 0; i < jsdocElements.length(); i++) {
				JSONObject jsdocElement = jsdocElements.getJSONObject(i);

				if (jsdocElement.getString("longname").contains("~")) {
					continue;
				}

				String access = jsdocElement.optString("access", "");
				if (access.equals("private")) {
					continue;
				}

				{
					boolean added = buildConstant(jsdocElement, typeMap);
					if (added) {
						// just to avoid the same obj is parsed as other kind of
						// member.
						continue;
					}
				}

				buildMethod(jsdocElement, typeMap);

				buildProperty(jsdocElement, typeMap);
			}

			Collection<PhaserType> types = typeMap.values();

			{
				// fix incomplete names

				for (PhaserType type : types) {

					List<String> superTypes = type.getExtends();
					for (int i = 0; i < superTypes.size(); i++) {
						String name = superTypes.get(i);
						if (!typeMap.containsKey(name)) {
							if (typeMap.containsKey("PIXI." + name)) {
								superTypes.set(i, "PIXI." + name);
							} else if (typeMap.containsKey("Phaser." + name)) {
								superTypes.set(i, "Phaser." + name);
							}

						}
					}

					Collection<PhaserMember> list = type.getMemberMap().values();
					for (PhaserMember member : list) {
						if (member instanceof PhaserMethod) {
							PhaserMethod method = (PhaserMethod) member;
							String[] retTypes = method.getReturnTypes();
							for (int i = 0; i < retTypes.length; i++) {
								String name = retTypes[i];
								if (!typeMap.containsKey(name)) {
									if (typeMap.containsKey("PIXI." + name)) {
										retTypes[i] = "PIXI." + name;
									} else if (typeMap.containsKey("Phaser." + name)) {
										retTypes[i] = "Phaser." + name;
									}
								}
							}
						} else if (member instanceof PhaserVariable) {
							PhaserVariable var = (PhaserVariable) member;
							String[] retTypes = var.getTypes();
							for (int i = 0; i < retTypes.length; i++) {
								String name = retTypes[i];
								if (!typeMap.containsKey(name)) {
									if (typeMap.containsKey("PIXI." + name)) {
										// out.println("Fix property " +
										// var.getName() + " to PIXI." + name);
										retTypes[i] = "PIXI." + name;
									} else if (typeMap.containsKey("Phaser." + name)) {
										// out.println("Fix property " +
										// var.getName() + " to Phaser." +
										// name);
										retTypes[i] = "Phaser." + name;
									}
								}
							}
						}
					}
				}
			}

			{
				// build inherited members

				Set<PhaserType> visited = new HashSet<>();

				for (PhaserType type : types) {
					buildInheritance(typeMap, visited, type);
				}
			}

			{
				// build specific member lists

				for (PhaserType type : types) {
					for (PhaserMember member : type.getMemberMap().values()) {
						if (member instanceof PhaserMethod) {
							type.getMethods().add((PhaserMethod) member);
						} else if (member instanceof PhaserConstant) {
							type.getConstants().add((PhaserConstant) member);
						} else if (member instanceof PhaserProperty) {
							type.getProperties().add((PhaserProperty) member);
						}
					}
				}
			}

			return typeMap;
		}
	}

	private static void buildInheritance(Map<String, PhaserType> typeMap, Set<PhaserType> visited, PhaserType type) {
		if (visited.contains(type)) {
			return;
		}

		visited.add(type);

		for (String superTypeName : type.getExtends()) {
			Map<String, PhaserMember> subTypeMap = type.getMemberMap();

			PhaserType superType = typeMap.get(superTypeName);
			if (superType == null) {
				// out.println("Ignore " + superTypeName);
				continue;
			}

			buildInheritance(typeMap, visited, superType);

			Map<String, PhaserMember> superTypeMap = superType.getMemberMap();

			for (PhaserMember member : superTypeMap.values()) {
				String memberName = member.getName();
				if (!subTypeMap.containsKey(memberName)) {
					// out.println("Add " + superTypeName + "." + memberName + "
					// to " + type.getName() + "." + memberName);
					subTypeMap.put(memberName, member);
				}
			}
		}
	}

	private boolean buildConstant(JSONObject obj, Map<String, PhaserType> typeMap) {
		boolean isCons = obj.getString("kind").equals("constant");

		if (!isCons) {
			if (obj.optString("scope", "").equals("static")) {
				String name = obj.getString("name");
				if (obj.getString("kind").equals("member") && name.toUpperCase().equals(name)) {
					isCons = true;
				}
			}
		}

		if (!isCons) {
			return false;
		}

		String name = obj.getString("name");
		String desc = obj.optString("description", "");
		Object defaultValue = obj.opt("defaultvalue");

		String[] types;
		if (obj.has("type")) {
			JSONArray jsonTypes = obj.getJSONObject("type").getJSONArray("names");
			types = getStringArray(jsonTypes);
		} else {
			// FIXME: this is the case of blendModes and scaleModes
			types = new String[] { "Object" };
		}
		PhaserConstant cons = new PhaserConstant();
		{
			// static flag
			String scope = obj.optString("scope", "");
			if (scope.equals("static")) {
				cons.setStatic(true);
			}
		}
		cons.setName(name);
		cons.setHelp(desc);
		cons.setTypes(types);
		cons.setDefaultValue(defaultValue);
		String memberof = obj.optString("memberof", null);
		if (memberof == null) {
			// global constant
			buildMeta(cons, obj);

			// FIXME: only add those Phaser.js constants
			if (cons.getFile().getFileName().toString().equals("Phaser.js")) {
				_globalConstants.add(cons);
			} else {
				out.println(obj.toString(2));
				throw new AssertionFailedException("All global constants should come from Phaser.js and not from "
						+ cons.getFile().getFileName() + "#" + cons.getName());
			}

		} else {
			PhaserType type = typeMap.get(memberof);
			Map<String, PhaserMember> map = type.getMemberMap();
			if (!map.containsKey(name)) {
				map.put(name, cons);
				cons.setDeclType(type);
				buildMeta(cons, obj);
			}
		}
		return true;
	}

	private static void buildProperty(JSONObject obj, Map<String, PhaserType> typeMap) {
		if (!obj.has("memberof")) {
			return;
		}

		String kind = obj.getString("kind");
		if (kind.equals("member") && !obj.has("params")) {
			String name = obj.optString("name", "");
			String desc = obj.optString("description", "");
			Object defaultValue = obj.opt("defaultvalue");
			JSONArray jsonTypes = null;
			if (obj.has("type")) {
				jsonTypes = obj.optJSONObject("type").getJSONArray("names");
			}
			String[] types = null;
			if (obj.has("properties")) {
				JSONArray props = obj.getJSONArray("properties");
				if (props.length() > 0) {
					if (props.length() > 1) {
						types = new String[] { "Object" };
					} else {
						Object propObj = props.get(0);
						if (propObj instanceof JSONObject) {
							JSONObject prop = (JSONObject) propObj;
							name = prop.optString("name", name);
							desc = prop.optString("description", desc);
							if (prop.has("type")) {
								JSONObject namesJson = prop.getJSONObject("type");
								jsonTypes = namesJson.getJSONArray("names");
							}
						}
					}
				}
			}
			if (types == null) {
				if (jsonTypes == null) {
					types = new String[] { "Object" };
				} else {
					types = getStringArray(jsonTypes);
				}
			}

			PhaserProperty property = new PhaserProperty();
			{
				// static flag
				String scope = obj.optString("scope", "");
				if (scope.equals("static")) {
					property.setStatic(true);
				}
			}
			property.setName(name);
			property.setHelp(desc);
			property.setTypes(types);
			property.setDefaultValue(defaultValue);
			property.setReadOnly(obj.optBoolean("readonly", false));

			String memberof = obj.getString("memberof");

			if (typeMap.containsKey(memberof)) {
				PhaserType type = typeMap.get(memberof);
				Map<String, PhaserMember> map = type.getMemberMap();

				if (!map.containsKey(name)) {
					map.put(name, property);
					property.setDeclType(type);
					buildMeta(property, obj);
				}
			}
		}
	}

	private static void buildMethod(JSONObject obj, Map<String, PhaserType> typeMap) {
		String kind = obj.getString("kind");

		if (kind.equals("function") || obj.has("params")) {
			PhaserMethod method = new PhaserMethod();

			{
				// static flag
				String scope = obj.optString("scope", "");
				if (scope.equals("static")) {
					method.setStatic(true);
				}
			}

			String name = obj.getString("name");
			method.setName(name);
			method.setHelp(obj.optString("description", ""));

			JSONArray jsonReturn = obj.optJSONArray("returns");
			if (jsonReturn != null) {
				JSONObject jsonReturnObj = jsonReturn.getJSONObject(0);
				JSONObject type = jsonReturnObj.optJSONObject("type");
				String[] types;
				if (type == null) {// Phaser.StateManager#getCurrentState
					types = new String[] { jsonReturnObj.getString("description") };
				} else {
					JSONArray names = type.getJSONArray("names");
					types = getStringArray(names);
				}
				method.setReturnTypes(types);
				method.setReturnHelp(jsonReturnObj.optString("description", ""));
			}

			List<PhaserMethodArg> args = buildArgs(obj);
			method.getArgs().addAll(args);
			for (PhaserMethodArg arg : args) {
				method.getArgsMap().put(arg.getName(), arg);
			}

			String memberof = obj.optString("memberof");
			if (memberof == null) {
				return;
			}
			PhaserType type = typeMap.get(memberof);
			if (type == null) {
				return;
			}

			if (!type.getMemberMap().containsKey(name)) {
				type.getMemberMap().put(name, method);
				method.setDeclType(type);
				buildMeta(method, obj);
			}
		}
	}

	private static void buildClass(JSONObject obj, Map<String, PhaserType> typeMap) {
		String kind = obj.getString("kind");
		if (kind.equals("class")) {

			String name = obj.getString("longname");

			if (name.equals("module:PIXI.PIXI")) {
				return;
			}

			// out.println("Parsing class: " + name);

			List<String> extend = new ArrayList<>();
			{
				JSONArray a = obj.optJSONArray("augments");
				if (a != null) {
					for (int j = 0; j < a.length(); j++) {
						String typename = a.getString(j);
						extend.add(typename);
					}
				}
			}

			List<PhaserMethodArg> args = buildArgs(obj);

			String desc = obj.optString("description", "");

			PhaserType type = new PhaserType();
			typeMap.put(name, type);

			type.setName(name);
			type.setHelp(desc);
			type.setExtends(extend);
			type.getConstructorArgs().addAll(args);

			buildMeta(type, obj);
		}
	}

	private static void buildMeta(IPhaserMember member, JSONObject obj) {
		JSONObject meta = obj.getJSONObject("meta");
		member.setLine(meta.getInt("lineno"));
		JSONArray jsonRange = meta.optJSONArray("range");
		if (jsonRange == null) {
			member.setOffset(-1);
		} else {
			member.setOffset(jsonRange.getInt(0));
		}

		String path = meta.getString("path");
		int beginIndex = path.indexOf("src") + 4;
		int endIndex = path.length();
		if (beginIndex > endIndex) {
			// the case of src/Phaser.js
			member.setFile(Paths.get(meta.getString("filename")));
		} else {
			String dir = path.substring(beginIndex, endIndex);
			path = dir + "/" + meta.getString("filename");
			member.setFile(Paths.get(path));
		}
	}

	private static List<PhaserMethodArg> buildArgs(JSONObject obj) {
		List<PhaserMethodArg> args = new ArrayList<>();
		JSONArray params = obj.optJSONArray("params");
		if (params != null) {
			for (int j = 0; j < params.length(); j++) {
				JSONObject param = params.getJSONObject(j);
				PhaserMethodArg arg = new PhaserMethodArg();
				arg.setName(param.optString("name", "_any"));
				arg.setHelp(param.optString("description"));
				arg.setDefaultValue(param.opt("defaultvalue"));
				arg.setOptional(param.optBoolean("optional", false));
				{
					if (param.has("type")) {
						JSONArray jsonTypes = param.getJSONObject("type").getJSONArray("names");
						String[] argTypes = getStringArray(jsonTypes);
						arg.setTypes(argTypes);
					} else {
						arg.setTypes(new String[] { "Object" });
					}
				}
				args.add(arg);
			}
		}
		return args;
	}

	private static String[] getStringArray(JSONArray jsonTypes) {
		String[] argTypes = new String[jsonTypes.length()];
		for (int k = 0; k < jsonTypes.length(); k++) {
			argTypes[k] = jsonTypes.getString(k);
		}
		return argTypes;
	}

	public String getMemberHelp(String memberFullName) {
		IPhaserMember member = _membersMap.get(memberFullName);
		if (member == null) {
			return "<No help available>";
		}
		return member.getHelp();
	}

	public String getMethodArgHelp(String methodName, String argName) {
		IPhaserMember member = _membersMap.get(methodName);
		List<PhaserMethodArg> args = Collections.emptyList();
		
		if (member instanceof PhaserMethod) {
			args = ((PhaserMethod) member).getArgs();
		} else if (member instanceof PhaserType) {
			args = ((PhaserType) member).getConstructorArgs();
		}
		
		for (PhaserMethodArg arg : args) {
			if (arg.getName().equals(argName)) {
				return arg.getHelp();
			}
		}
		return "<No help available>";
	}
}
