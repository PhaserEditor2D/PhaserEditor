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
import static java.lang.System.err;
import static java.lang.System.out;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
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

public class PhaserJsdocModel implements Serializable {

	private static final long serialVersionUID = 1L;

	private transient static PhaserJsdocModel _instance;

	public synchronized static PhaserJsdocModel getInstance() {
		if (_instance == null) {
			try {
				var srcFolder = InspectCore.getBundleFile(InspectCore.RESOURCES_PHASER_CODE_PLUGIN,
						"phaser-master/src");

				var t = currentTimeMillis();

				Path docsJsonFile = InspectCore.getBundleFile(InspectCore.RESOURCES_METADATA_PLUGIN,
						"phaser-custom/phaser3-docs/json/phaser.json").toAbsolutePath().normalize();

				var size = Files.getLastModifiedTime(docsJsonFile).toMillis();

				var cacheFile = InspectCore.getUserCacheFolder().resolve("phaser.json." + size + ".binary");

				if (Files.exists(cacheFile)) {
					try (var input = new ObjectInputStream(Files.newInputStream(cacheFile))) {
						_instance = (PhaserJsdocModel) input.readObject();
						_instance._srcFolder = srcFolder;

						out.println("Read cached docs..." + (currentTimeMillis() - t) + "ms");

						return _instance;
					} catch (IOException | ClassNotFoundException e) {
						e.printStackTrace();
					}
				}

				_instance = new PhaserJsdocModel(srcFolder, docsJsonFile);

				var t2 = currentTimeMillis();

				try (var serilizer = new ObjectOutputStream(Files.newOutputStream(cacheFile))) {
					serilizer.writeObject(_instance);
				}

				out.println("Write phaser.jsdoc cache in " + (currentTimeMillis() - t2) + "ms");

				out.println("Build Phaser JSDoc " + (currentTimeMillis() - t) + "ms");
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		return _instance;
	}

	private Map<String, IMemberContainer> _containersMap;
	private Map<String, IPhaserMember> _membersMap;
	private List<IPhaserMember> _rootNamespaces;
	private Set<String> _elementsWithMembers;

	private transient Path _srcFolder;
	private PhaserGlobalScope _globalScope;

	public PhaserJsdocModel(Path srcFolder, Path docsJsonFile) throws IOException {
		_srcFolder = srcFolder;

		buildPhaserJSDoc(docsJsonFile);
	}

	public Path getMemberPath(IPhaserMember member) {
		return _srcFolder.resolve(member.getFile());
	}

	public Path getSrcFolder() {
		return _srcFolder;
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

	private void buildPhaserJSDoc(Path docsJsonFile) throws IOException {
		_containersMap = new HashMap<>();
		_membersMap = new HashMap<>();
		_rootNamespaces = new ArrayList<>();
		_elementsWithMembers = new HashSet<>();

		if (!Files.exists(docsJsonFile)) {
			return;
		}

		try (InputStream input = Files.newInputStream(docsJsonFile)) {
			var t = currentTimeMillis();
			JSONObject jsonDoc = new JSONObject(new JSONTokener(input));
			out.println("Read phaser.json file in " + (currentTimeMillis() - t) + "ms");
			JSONArray jsdocElements = jsonDoc.getJSONArray("docs");

			// printElementKinds(jsdocElements);

			// not all classes are declared as classes, for example
			// Phaser.GameObjects.Component.Alpha, but we can detect it if there is any
			// memberof declared.

			for (int i = 0; i < jsdocElements.length(); i++) {
				JSONObject obj = jsdocElements.getJSONObject(i);

				buildElementsWidthMembers(obj);
			}

			// pass to get all the namespaces in the map

			for (int i = 0; i < jsdocElements.length(); i++) {
				JSONObject obj = jsdocElements.getJSONObject(i);

				buildNamespace(obj);
			}

			// pass to get all the classes in the map

			for (int i = 0; i < jsdocElements.length(); i++) {
				JSONObject obj = jsdocElements.getJSONObject(i);

				if (obj.getString("longname").contains("~")) {
					continue;
				}

				buildClass(obj);
				buildTypeDef(obj);
			}

			// link to containers
			{
				for (IMemberContainer elem : _containersMap.values()) {
					JSONObject obj = elem.getJSON();
					String memberof = obj.optString("memberof");
					if (memberof == null || memberof.equals("")) {
						continue;
					}

					IMemberContainer container = _containersMap.get(memberof);

					if (container == null) {
						err.println("!ERROR: Member-of not found. The element '" + elem.getName() + "' is member of '"
								+ memberof + "'");
						continue;
					}

					if (!container.getMemberMap().containsKey(elem.getName())) {
						container.getMemberMap().put(elem.getName(), elem);
					}
				}

			}

			// build elements of containers

			for (int i = 0; i < jsdocElements.length(); i++) {
				JSONObject jsdocElement = jsdocElements.getJSONObject(i);

				// do not add inherited members
				if (jsdocElement.optBoolean("inherited", false)) {
					continue;
				}

				String longname = jsdocElement.getString("longname");

				if (longname.contains("~")) {
					continue;
				}

				String access = jsdocElement.optString("access", "");
				if (access.equals("private")) {
					continue;
				}

				buildEnumType(jsdocElement);

				buildEventConstant(jsdocElement);

				if (!buildConstant(jsdocElement)) {
					buildProperty(jsdocElement);
				}

				buildMethod(jsdocElement);
			}

			Collection<IMemberContainer> containers = _containersMap.values();

			{
				// build inherited members

				Set<PhaserType> visited = new HashSet<>();

				for (IMemberContainer container : containers) {
					if (container instanceof PhaserType) {
						buildInheritance(visited, (PhaserType) container);
					}
				}
			}

			{
				// build specific member lists

				for (IMemberContainer container : containers) {
					container.build();
				}
			}

			{
				List<IPhaserMember> globals = new ArrayList<>();
				// build root members
				for (IPhaserMember member : _membersMap.values()) {
					if (member.getContainer() == null) {
						if (member.getClass() == PhaserNamespace.class) {
							_rootNamespaces.add(member);
						} else {
							globals.add(member);
						}
					}
				}
				globals.sort((a, b) -> a.getName().compareTo(b.getName()));
				_globalScope = new PhaserGlobalScope(globals);
			}
		}
	}

	private void buildElementsWidthMembers(JSONObject obj) {
		var memberof = obj.optString("memberof", null);
		if (memberof != null) {
			_elementsWithMembers.add(memberof);
		}
	}

	public PhaserGlobalScope getGlobalScope() {
		return _globalScope;
	}

	public List<IPhaserMember> getRootNamespaces() {
		return _rootNamespaces;
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

	private void buildInheritance(Set<PhaserType> visited, PhaserType type) {
		if (visited.contains(type)) {
			return;
		}

		visited.add(type);

		var subTypeMap = type.getMemberMap();
		var inheritedMap = type.getInheritedMembers();

		for (String superTypeName : type.getExtends()) {

			IMemberContainer container = _containersMap.get(superTypeName);
			PhaserType superType = container == null ? null : container.castType();

			if (superType == null) {
				// out.println("Ignore " + superTypeName);
				continue;
			}

			superType.getExtenders().add(type);
			type.getExtending().add(superType);

			buildInheritance(visited, superType);

			var superTypeMap = superType.getMemberMap();

			for (IPhaserMember member : superTypeMap.values()) {
				String memberName = member.getName();
				if (!subTypeMap.containsKey(memberName)) {
					// out.println("Add " + superTypeName + "." + memberName + "
					// to " + type.getName() + "." + memberName);
					subTypeMap.put(memberName, member);
					inheritedMap.add(member);
				}
			}
		}
	}

	private void buildEventConstant(JSONObject obj) {
		if (!obj.getString("kind").equals("event")) {
			return;
		}

		String name = obj.getString("name");
		String desc = obj.optString("description", "");
		Object defaultValue = obj.opt("defaultvalue");

		var types = new String[] { "String" };

		var event = new PhaserEventConstant(obj);
		// all event constants are decalred in namespaces
		event.setStatic(true);

		event.setName(name);
		event.setHelp(desc);
		event.setTypes(types);
		event.setDefaultValue(defaultValue);

		var memberof = obj.optString("memberof", null);
		var container = (PhaserNamespace) _containersMap.get(memberof);

		var map = container.getMemberMap();

		if (map.containsKey(name)) {

			assert (false);

		} else {

			map.put(name, event);

			var longname = container.getName() + "." + name;

			_membersMap.put(longname, event);

			buildMeta(event, obj);
		}

	}

	private boolean buildConstant(JSONObject obj) {
		String kind = obj.getString("kind");
		boolean isCons = kind.equals("constant");
		if (!isCons) {
			if (obj.optString("scope", "").equals("static")) {
				String name = obj.getString("name");
				if (kind.equals("member") && name.toUpperCase().equals(name)) {
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

		PhaserConstant cons = new PhaserConstant(obj);
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
		IMemberContainer container = _containersMap.get(memberof);

		if (container == null) {
			return false;
		}

		Map<String, IPhaserMember> map = container.getMemberMap();
		if (!map.containsKey(name)) {

			map.put(name, cons);

			String longname = container.getName() + "." + name;
			_membersMap.put(longname, cons);

			if (container instanceof PhaserType) {
				PhaserType type = (PhaserType) container;
				cons.setDeclType(type);
				if (isCons && !obj.has("type")) {
					cons.setTypes(type.getEnumElementsType());
				}
			}

			buildMeta(cons, obj);
		}

		return true;
	}

	private void buildProperty(JSONObject obj) {
		if (!obj.has("memberof")) {
			return;
		}

		String kind = obj.getString("kind");

		if (kind.equals("member") && !obj.has("params")) {

			{
				// check if it is not a class
				var longname = obj.optString("longname");
				if (_elementsWithMembers.contains(longname)) {
					// this was parsed as a class
					return;
				}
			}

			String name = obj.optString("name", "");
			String desc = obj.optString("description", "");
			Object defaultValue = obj.opt("defaultvalue");

			String[] types = parseElementTypes(obj);

			PhaserProperty property = new PhaserProperty(obj);
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

			if (_containersMap.containsKey(memberof)) {

				IMemberContainer container = _containersMap.get(memberof);

				if (container == null) {
					return;
				}

				Map<String, IPhaserMember> map = container.getMemberMap();

				if (!map.containsKey(name)) {
					map.put(name, property);

					var longname = container.getName() + "." + name;
					_membersMap.put(longname, property);

					if (container instanceof PhaserType) {
						PhaserType type = (PhaserType) container;
						property.setDeclType(type);
					}

					buildMeta(property, obj);
				}
			}
		}
	}

	private static String[] parseElementTypes(JSONObject obj) {
		JSONArray jsonTypes = null;

		if (obj.has("type")) {
			jsonTypes = obj.optJSONObject("type").getJSONArray("names");
		}

		String[] types;

		if (jsonTypes == null) {
			types = new String[] { "Object" };
		} else {
			types = getStringArray(jsonTypes);
		}
		return types;
	}

	private void buildMethod(JSONObject obj) {
		String kind = obj.getString("kind");

		if (kind.equals("function")) {
			PhaserMethod method = new PhaserMethod(obj);

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

			IMemberContainer container = _containersMap.get(memberof);

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
				String longname = container.getName() + "." + name;
				_membersMap.put(longname, method);
				buildMeta(method, obj);
			}
		}
	}

	private void buildNamespace(JSONObject obj) {
		String kind = obj.getString("kind");
		if (kind.equals("namespace")) {

			String longname = obj.getString("longname");

			// out.println("Parsing namespace: " + name);

			String desc = obj.optString("description", "");

			PhaserNamespace namespace = new PhaserNamespace(obj);
			_containersMap.put(longname, namespace);
			_membersMap.put(longname, namespace);

			namespace.setName(longname);
			namespace.setHelp(desc);

			buildMeta(namespace, obj);
		}
	}

	private void buildEnumType(JSONObject obj) {
		boolean isEnum = obj.optBoolean("isEnum");
		if (isEnum) {
			String longname = obj.getString("longname");

			String desc = obj.optString("description", "");

			PhaserType type = new PhaserType(obj);
			type.setEnum(true);
			type.setEnumElementsType(parseElementTypes(obj));

			_containersMap.put(longname, type);
			_membersMap.put(longname, type);

			type.setName(longname);
			type.setHelp(desc);

			String memberof = obj.optString("memberof");
			if (memberof == null) {
				return;
			}

			IMemberContainer container = _containersMap.get(memberof);

			if (container == null) {
				return;
			}

			if (!container.getMemberMap().containsKey(longname)) {
				container.getMemberMap().put(longname, type);
				buildMeta(type, obj);
			}
		}
	}

	private void buildTypeDef(JSONObject obj) {
		String kind = obj.getString("kind");
		if (kind.equals("typedef")) {

			String longname = obj.getString("longname");

			String desc = obj.optString("description", null);
			if (desc == null) {
				desc = obj.optString("classdesc", "");
			}

			PhaserType type = new PhaserType(obj);
			type.setTypeDef(true);
			_containersMap.put(longname, type);
			_membersMap.put(longname, type);

			type.setName(longname);
			type.setHelp(desc);

			JSONArray propertiesJson = obj.optJSONArray("properties");
			if (propertiesJson != null) {
				for (int i = 0; i < propertiesJson.length(); i++) {
					JSONObject propertyJson = propertiesJson.getJSONObject(i);
					String propName = propertyJson.getString("name");
					String propLongname = longname + "." + propName;
					propertyJson.put("longname", propLongname);
					PhaserProperty property = new PhaserProperty(propertyJson);
					property.setName(propName);
					property.setDeclType(type);
					property.setContainer(type);
					property.setTypes(parseElementTypes(propertyJson));
					property.setHelp(propertyJson.optString("description", ""));
					_membersMap.put(propLongname, property);
					type.getMemberMap().put(propName, property);
					buildMeta(property, obj);
				}
			}

			buildMeta(type, obj);
		}
	}

	private void buildClass(JSONObject obj) {
		String kind = obj.getString("kind");

		String longname = obj.getString("longname");

		var hasMembers = _elementsWithMembers.contains(longname);
		var isEnum = obj.optBoolean("isEnum");

		if (kind.equals("class") || kind.equals("member") && hasMembers && !isEnum) {

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

			String desc = obj.optString("description", null);
			if (desc == null) {
				desc = obj.optString("classdesc", "");
			}

			PhaserType type = new PhaserType(obj);
			_containersMap.put(longname, type);
			_membersMap.put(longname, type);

			type.setName(longname);
			type.setHelp(desc);
			type.setExtends(extend);
			type.getConstructorArgs().addAll(args);

			String memberof = obj.optString("memberof");
			if (memberof == null) {
				return;
			}

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
		path = path.replace("\\", "/");
		int beginIndex = path.indexOf("src") + 4;
		int endIndex = path.length();
		if (beginIndex > endIndex) {
			// the case of src/Phaser.js
			member.setFile(meta.getString("filename"));
		} else {
			String dir = path.substring(beginIndex, endIndex);
			path = dir + "/" + meta.getString("filename");
			member.setFile(path);
		}
	}

	private static List<PhaserMethodArg> buildArgs(JSONObject obj) {
		List<PhaserMethodArg> args = new ArrayList<>();
		JSONArray params = obj.optJSONArray("params");
		if (params != null) {
			for (int j = 0; j < params.length(); j++) {
				JSONObject param = params.getJSONObject(j);
				PhaserMethodArg arg = new PhaserMethodArg(param);
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
