/**
 * 
 */
package buildbrowserapi;

import static java.lang.System.out;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * @author arian
 *
 */
public class BuildBrowserAPI {

	static class Member {
		public String name;
		public Container parent;
		public final String doc;

		public Member(String name, String doc, Container parent) {
			super();
			this.name = normalizeName(name);
			this.doc = doc == null ? null : (doc.trim().length() == 0 ? null : doc);
			this.parent = parent;
		}

		public static String camelCase(String name) {
			return name.substring(0, 1).toUpperCase() + name.substring(1);
		}

		protected static String normalizeName(String name) {
			if (name == null) {
				return name;
			}

			switch (name) {
			case "delete":
				return "_delete";
			case "this":
				return "_this";
			default:
				break;
			}

			String normalname = "";
			for (char c : name.toCharArray()) {
				if (Character.isJavaIdentifierPart(c)) {
					normalname += c;
				} else {
					normalname += "_";
				}
			}
			return normalname;
		}

		protected static String normalizeType(String type) {
			switch (type) {
			case "?":
				return "new Object()";
			case "<top>":
				return "new Object()";
			case "string":
				return "\"\"";
			case "bool":
				return "false";
			case "number":
				return "0";
			case "integer":
				return "0";
			default:
				break;
			}

			if (type.startsWith("+")) {
				return "new " + normalizeType(type.substring(1)) + "()";
			}

			if (type.startsWith("[")) {
				return "new Array()";
			}

			return type;
		}

		public void buildDocIndex(JSONObject index) {
			if (doc != null) {
				String prefix = name;
				if (parent != null && parent.name != null) {
					prefix = parent.name + "." + name;
				}
				index.put(prefix, doc);
			}
		}
	}

	static class Container extends Member {

		public Container(String name, String doc, Container parent) {
			super(name, doc, parent);
		}

		private List<Member> members = new ArrayList<>();

		public void add(Member member) {
			members.add(member);
		}

		public void addAll(Collection<Member> list) {
			for (Member member : list) {
				add(member);
			}
		}

		public List<Member> members() {
			return members;
		}

		public Container getRoot() {
			if (parent == null) {
				return this;
			}
			return parent;
		}

		@Override
		public void buildDocIndex(JSONObject index) {
			super.buildDocIndex(index);
			for (Member member : members) {
				member.buildDocIndex(index);
			}
		}
	}

	/**
	 * 
	 * @author arian
	 *
	 */
	static class Unit extends Container {

		public Unit() {
			super(null, null, null);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (Member member : members()) {
				if (member instanceof Func) {
					sb.append(member.toString() + "\n\n");
				} else if (member instanceof InlineObject) {
					sb.append(member.toString());
					sb.append(member.name + " = new _" + camelCase(member.name) + "();\n\n");
				} else if (member instanceof Func || member instanceof Field) {
					sb.append(member.name + " = " + member.toString() + ";\n\n");
				} else {
					sb.append(member + "\n");
				}
			}
			return sb.toString();
		}

		/**
		 * @return
		 */
		public JSONObject buildDocIndex() {
			JSONObject index = new JSONObject();

			for (Member member : members()) {
				member.buildDocIndex(index);
			}

			return index;
		}
	}

	static class Proto extends Container {
		public Proto(String name, String doc, Container parent) {
			super(name, doc, parent);
		}

		public boolean nowAddStatics = false;

		public List<Member> staticMembers = new ArrayList<>();
		public String extendsProto;
		public boolean forceNoStatics = false;

		@Override
		public void add(Member member) {
			if (!forceNoStatics && nowAddStatics) {
				staticMembers.add(member);
			} else {
				super.add(member);
			}
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			// TODO: missing to use the constructor signature
			sb.append("function " + name + "() {\n");
			for (Member member : members()) {
				if (!(member instanceof Func)) {
					sb.append(" this." + member.name + " = " + member.toString() + ";\n");
				}
			}
			sb.append("}\n");

			if (extendsProto == null) {
				sb.append(name + ".prototype = new Object();\n");
			} else {
				sb.append(name + ".prototype = new " + extendsProto.replace(".prototype", "") + "();\n");
			}

			for (Member member : members()) {
				if (member instanceof Func) {
					sb.append(name + ".prototype." + member.name + " = " + member.toString() + ";\n");
				}
			}

			for (Member member : staticMembers) {
				sb.append(name + "." + member.name + " = " + member.toString() + ";\n");
			}

			return sb.toString();
		}
	}

	static class InlineObject extends Container {

		public InlineObject(String name, String doc, Container parent) {
			super(name, doc, parent);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (parent instanceof Unit) {
				// render as prototype
				String genname = "_" + camelCase(name);
				Proto proto = new Proto(genname, doc, parent);
				proto.addAll(members());
				sb.append(proto.toString());
			} else {
				sb.append("{\n");
				for (Member member : members()) {
					sb.append("  " + member.name + " : " + member.toString() + ",\n");
				}
				sb.append("}");
			}
			return sb.toString();
		}
	}

	static class Func extends Member {

		public Func(String name, String doc, Container parent) {
			super(name, doc, parent);
		}

		public String type;

		@Override
		public String toString() {
			return toString2(parent instanceof Unit ? false : true);
		}

		private String toString2(boolean asValue) {
			String signature = "";
			String rettype = null;

			{
				int i = type.lastIndexOf("->");
				if (i > 0) {
					rettype = type.substring(i + 2).trim();
					rettype = normalizeName(rettype);
					rettype = normalizeType(rettype);
				}
			}

			if (type.lastIndexOf("fn(") > 0) {
				// TODO: missing to parse function based arguments.
				signature = "()";
			} else {

				StringBuilder args = new StringBuilder();
				{
					// remove the return expr
					String leftpart = type.split("->")[0].trim();
					// remove the fn keyword
					leftpart = leftpart.substring(2);
					// remove parents
					String argspart = leftpart.substring(1, leftpart.length() - 1);
					String[] vardecls = argspart.split(",");

					Set<String> used = new HashSet<>();

					for (int i = 0; i < vardecls.length; i++) {
						if (i > 0) {
							args.append(",");
						}
						String vardecl = vardecls[0];
						String argname0 = vardecl.split(":")[0];
						String argname = normalizeName(argname0);

						if (used.contains(argname)) {
							argname += used.size();
						}

						args.append(argname);
						used.add(argname);
					}
				}
				// build signature
				signature = "(" + args + ")";
			}

			String header = "function " + (asValue ? "" : name);

			if (rettype == null) {
				return header + signature + " { }";
			}

			return header + signature + " { return " + rettype + "; }";
		}
	}

	static class Field extends Member {

		public Field(String name, String doc, Container parent) {
			super(name, doc, parent);
		}

		public String type;

		@Override
		public String toString() {
			return normalizeType(type);
		}
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		build("browser", true);
		build("ecma5", false);
	}

	private static void build(String libname, boolean genwindow) throws IOException {
		Path defsFile = Paths.get(libname + ".json");
		JSONObject doc = new JSONObject(new JSONTokener(Files.newInputStream(defsFile)));

		Unit unit = new Unit();

		list(unit, doc);

		if (genwindow) {
			addWindowProto(unit);
		}

		String content = unit.toString();
		out.println(libname + "\n**********************************");
		out.println(content);

		Files.write(Paths.get("output/" + libname + ".js"), content.getBytes(), StandardOpenOption.CREATE);

		JSONObject docIndex = unit.buildDocIndex();
		Files.write(Paths.get("output/" + libname + ".doc.json"), docIndex.toString(2).getBytes(),
				StandardOpenOption.CREATE);
	}

	private static void addWindowProto(Unit unit) {
		Proto winproto = new Proto("_Window", null, unit);
		for (Member member : unit.members()) {
			if (member instanceof Func || member instanceof Field) {
				winproto.add(member);
			}
		}
		unit.add(winproto);
		Field field = new Field("window", "The window object represents a window containing a DOM document.", unit);
		field.type = "+_Window";
		unit.add(field);
	}

	private static void list(Container parent, JSONObject data) {
		for (String key : data.keySet()) {

			// this is very ugly, but we are going to generate "window" manually
			if (parent instanceof Unit && key.equals("window")) {
				continue;
			}

			if (key.startsWith("!")) {
				continue;
			}

			member(parent, key, data.get(key));
		}
	}

	private static void member(Container parent, String key, Object value) {
		if (value instanceof JSONObject) {
			JSONObject jsonValue = (JSONObject) value;
			String jsdoc = jsonValue.optString("!doc");
			if (jsonValue.has("prototype")) {
				Object protoValue = jsonValue.get("prototype");
				Proto proto = new Proto(key, jsdoc, parent);
				parent.add(proto);

				if (protoValue instanceof JSONObject) {
					JSONObject jsonProtoValue = (JSONObject) protoValue;
					if (jsonProtoValue.has("!proto")) {
						proto.extendsProto = jsonProtoValue.getString("!proto");
					}
					list(proto, jsonProtoValue);
				} else {
					// prototype alias
					proto.extendsProto = (String) protoValue;
				}

				proto.nowAddStatics = true;
				for (String key2 : jsonValue.keySet()) {
					if (key2.startsWith("!") || key2.equals("prototype")) {
						continue;
					}
					Object value2 = jsonValue.get(key2);
					member(proto, key2, value2);
				}
			} else if (jsonValue.has("!type")) {
				String type = jsonValue.getString("!type");
				Member member;
				if (type.startsWith("fn(")) {
					// function
					Func func = new Func(key, jsdoc, parent);
					func.type = jsonValue.getString("!type");
					member = func;
				} else {
					// field
					if (type.equals("Element")) {
						// XXX: a hack!!!!
						Proto proto = new Proto(key, jsdoc, parent);
						proto.extendsProto = "Element";
						member = proto;
					} else {
						Field field = new Field(key, jsdoc, parent);
						field.type = type;
						member = field;
					}
				}
				parent.add(member);
			} else {
				// not proto, no type? is just an inline object, so simulate it
				// with a prototype
				Container root = parent.getRoot();
				Proto proto = new Proto("_" + Member.camelCase(key), jsdoc, root);
				proto.forceNoStatics = true;
				root.add(proto);
				list(proto, jsonValue);
				Field field = new Field(key, jsdoc, parent);
				field.type = "+" + proto.name;
				parent.add(field);
			}
		} else {
			// not json, then it is a field or a function
			String strValue = (String) value;
			if (strValue.startsWith("fn(")) {
				// is a function
				Func func = new Func(key, null, parent);
				func.type = strValue;
				parent.add(func);
			} else {
				// is a field
				Field field = new Field(key, null, parent);
				field.type = strValue;
				if (parent instanceof Proto) {
					Proto proto2 = (Proto) parent;
					proto2.add(field);
				} else {
					parent.add(field);
				}
			}
		}
	}
}
