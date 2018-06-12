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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.inspect.core.InspectCore;

public class PhaserJSDoc {
	private static PhaserJSDoc _instance;

	public synchronized static PhaserJSDoc getInstance() {
		if (_instance == null) {
			long t = currentTimeMillis();
			Path docsJsonFile = InspectCore
					.getBundleFile(InspectCore.RESOURCES_METADATA_PLUGIN, "phaser-custom/phaser3-docs/json/phaser.json")
					.toAbsolutePath().normalize();
			Path srcFolder = InspectCore.getBundleFile(InspectCore.RESOURCES_PHASER_CODE_PLUGIN, "phaser-master/src");

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

	private Map<String, IMemberContainer> _containersMap;
	private Map<String, IPhaserMember> _membersMap;

	private Path _srcFolder;

	public PhaserJSDoc(Path srcFolder, Path docsJsonFile) throws IOException {
		_srcFolder = srcFolder;
		_membersMap = new HashMap<>();

		_containersMap = buildPhaserJSDoc(docsJsonFile);

		for (IMemberContainer container : _containersMap.values()) {
			String name = container.getName();

			_membersMap.put(name, container);

			PhaserType type = container.castType();

			if (type == null) {
				continue;
			}

			_membersMap.put(name + ".constructor", type);

			for (PhaserMember m : type.getMethods()) {
				_membersMap.put(name + "." + m.getName(), m);
			}

			for (PhaserMember m : type.getProperties()) {
				_membersMap.put(name + "." + m.getName(), m);
			}

			for (PhaserMember m : type.getConstants()) {
				_membersMap.put(name + "." + m.getName(), m);
			}
		}

	}

	public Path getMemberPath(IPhaserMember member) {
		return _srcFolder.resolve(member.getFile());
	}

	public Map<String, IMemberContainer> getContainerMap() {
		return _containersMap;
	}

	public PhaserType getType(String name) {
		IMemberContainer container = _containersMap.get(name);
		return container == null ? null : (PhaserType) container;
	}

	public Collection<IMemberContainer> getContainers() {
		return _containersMap.values();
	}

	public Map<String, IPhaserMember> getMembersMap() {
		return _membersMap;
	}

	private static Map<String, IMemberContainer> buildPhaserJSDoc(Path docsJsonFile) throws IOException {
		if (!Files.exists(docsJsonFile)) {
			return new HashMap<>();
		}

		try (InputStream input = Files.newInputStream(docsJsonFile)) {
			JSONObject jsonDoc = new JSONObject(new JSONTokener(input));
			JSONArray jsdocElements = jsonDoc.getJSONArray("docs");

			// printElementKinds(jsdocElements);

			Map<String, IMemberContainer> containersMap = new HashMap<>();

			// pass to get all the namespaces in the map

			for (int i = 0; i < jsdocElements.length(); i++) {
				JSONObject obj = jsdocElements.getJSONObject(i);

				buildNamespace(obj, containersMap);
			}

			// pass to get all the classes in the map

			for (int i = 0; i < jsdocElements.length(); i++) {
				JSONObject obj = jsdocElements.getJSONObject(i);

				if (obj.getString("longname").contains("~")) {
					continue;
				}

				buildClass(obj, containersMap);
			}

			for (int i = 0; i < jsdocElements.length(); i++) {
				JSONObject jsdocElement = jsdocElements.getJSONObject(i);

				String longname = jsdocElement.getString("longname");

				if (longname.contains("~")) {
					continue;
				}

				String access = jsdocElement.optString("access", "");
				if (access.equals("private")) {
					continue;
				}

				buildEnum(jsdocElement, containersMap);

				buildConstant(jsdocElement, containersMap);

				buildMethod(jsdocElement, containersMap);

				buildProperty(jsdocElement, containersMap);
			}

			Collection<IMemberContainer> containers = containersMap.values();

			{
				// build inherited members

				Set<PhaserType> visited = new HashSet<>();

				for (IMemberContainer container : containers) {
					if (container instanceof PhaserType) {
						buildInheritance(containersMap, visited, (PhaserType) container);
					}
				}
			}

			{
				// build specific member lists

				for (IMemberContainer container : containers) {
					container.build();
				}
			}

			return containersMap;
		}
	}

	@SuppressWarnings("unused")
	private static void printElementKinds(JSONArray jsdocElements) {
		Set<String> kinds = new HashSet<>();
		Set<String> scopes = new HashSet<>();
		for (int i = 0; i < jsdocElements.length(); i++) {
			JSONObject obj = jsdocElements.getJSONObject(i);
			kinds.add(obj.getString("kind"));
		}
		out.println("kinds: " + Arrays.toString(kinds.toArray()));
		out.println();
	}

	private static void buildInheritance(Map<String, IMemberContainer> typeMap, Set<PhaserType> visited,
			PhaserType type) {
		if (visited.contains(type)) {
			return;
		}

		visited.add(type);

		for (String superTypeName : type.getExtends()) {
			Map<String, IPhaserMember> subTypeMap = type.getMemberMap();

			IMemberContainer container = typeMap.get(superTypeName);
			PhaserType superType = container == null ? null : container.castType();

			if (superType == null) {
				// out.println("Ignore " + superTypeName);
				continue;
			}

			buildInheritance(typeMap, visited, superType);

			Map<String, IPhaserMember> superTypeMap = superType.getMemberMap();

			for (IPhaserMember member : superTypeMap.values()) {
				String memberName = member.getName();
				if (!subTypeMap.containsKey(memberName)) {
					// out.println("Add " + superTypeName + "." + memberName + "
					// to " + type.getName() + "." + memberName);
					subTypeMap.put(memberName, member);
				}
			}
		}
	}

	private static boolean buildConstant(JSONObject obj, Map<String, IMemberContainer> containersMap) {
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
		IMemberContainer container = containersMap.get(memberof);
		PhaserType type = container == null ? null : container.castType();

		if (type == null) {
			return false;
		}

		Map<String, IPhaserMember> map = type.getMemberMap();
		if (!map.containsKey(name)) {
			map.put(name, cons);
			cons.setDeclType(type);
			buildMeta(cons, obj);
		}
		return true;
	}

	private static void buildProperty(JSONObject obj, Map<String, IMemberContainer> typeMap) {
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

				IMemberContainer container = typeMap.get(memberof);
				PhaserType type = container == null ? null : container.castType();

				if (type == null) {
					return;
				}
				

				Map<String, IPhaserMember> map = type.getMemberMap();

				if (!map.containsKey(name)) {
					map.put(name, property);
					property.setDeclType(type);
					buildMeta(property, obj);
				}
			}
			// else {
			// out.println("JSDoc: memberof not found: " + memberof + " from member " +
			// obj.getString("longname"));
			// }
		}
	}

	private static void buildMethod(JSONObject obj, Map<String, IMemberContainer> typeMap) {
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

			IMemberContainer container = typeMap.get(memberof);

			if (container == null) {
				return;
			}

			if (container instanceof PhaserType) {
				PhaserType type = (PhaserType) container;
				if (type.isStatic()) {
					method.setStatic(true);
				}
				method.setDeclType(type);
			} else {
				method.setStatic(true);
			}

			if (!container.getMemberMap().containsKey(name)) {
				container.getMemberMap().put(name, method);
				buildMeta(method, obj);
			}
		}
	}

	private static void buildNamespace(JSONObject obj, Map<String, IMemberContainer> containersMap) {
		String kind = obj.getString("kind");
		if (kind.equals("namespace")) {

			String name = obj.getString("longname");

			// out.println("Parsing namespace: " + name);

			String desc = obj.optString("description", "");

			PhaserNamespace namespace = new PhaserNamespace();
			containersMap.put(name, namespace);

			namespace.setName(name);
			namespace.setHelp(desc);

			String memberof = obj.optString("memberof");
			if (memberof == null) {
				return;
			}

			IMemberContainer container = containersMap.get(memberof);

			if (container == null) {
				return;
			}

			if (!container.getMemberMap().containsKey(name)) {
				container.getMemberMap().put(name, namespace);
				buildMeta(namespace, obj);
			}

		}
	}

	private static void buildEnum(JSONObject obj, Map<String, IMemberContainer> containersMap) {
		boolean isEnum = obj.optBoolean("isEnum");
		if (isEnum) {
			String name = obj.getString("longname");

			String desc = obj.optString("description", "");

			PhaserType type = new PhaserType();
			type.setEnum(true);
			containersMap.put(name, type);

			type.setName(name);
			type.setHelp(desc);

			String memberof = obj.optString("memberof");
			if (memberof == null) {
				return;
			}

			IMemberContainer container = containersMap.get(memberof);

			if (container == null) {
				return;
			}

			if (!container.getMemberMap().containsKey(name)) {
				container.getMemberMap().put(name, type);
				buildMeta(type, obj);
			}
		}
	}

	private static void buildClass(JSONObject obj, Map<String, IMemberContainer> containersMap) {
		String kind = obj.getString("kind");
		if (kind.equals("class")) {

			String name = obj.getString("longname");

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
			containersMap.put(name, type);

			type.setName(name);
			type.setHelp(desc);
			type.setExtends(extend);
			type.getConstructorArgs().addAll(args);

			String memberof = obj.optString("memberof");
			if (memberof == null) {
				return;
			}

			IMemberContainer container = containersMap.get(memberof);

			if (container == null) {
				return;
			}

			if (!container.getMemberMap().containsKey(name)) {
				container.getMemberMap().put(name, type);
				buildMeta(type, obj);
			}
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
