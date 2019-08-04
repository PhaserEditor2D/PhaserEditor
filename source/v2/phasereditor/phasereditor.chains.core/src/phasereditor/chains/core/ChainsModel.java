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
package phasereditor.chains.core;

import static java.lang.System.out;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.examples.PhaserExampleCategoryModel;
import phasereditor.inspect.core.examples.PhaserExampleModel;
import phasereditor.inspect.core.examples.PhaserExamplesRepoModel;
import phasereditor.inspect.core.jsdoc.IMemberContainer;
import phasereditor.inspect.core.jsdoc.ITypeMember;
import phasereditor.inspect.core.jsdoc.PhaserJsdocModel;
import phasereditor.inspect.core.jsdoc.PhaserMethod;
import phasereditor.inspect.core.jsdoc.PhaserMethodArg;
import phasereditor.inspect.core.jsdoc.PhaserNamespace;
import phasereditor.inspect.core.jsdoc.PhaserType;
import phasereditor.inspect.core.jsdoc.PhaserVariable;

public class ChainsModel {
	private ArrayList<ChainItem> _chains;
	private List<PhaserExampleModel> _exampleItems;
	private List<Line> _examplesLines;
	private PhaserJsdocModel _jsdoc;
	private PhaserExamplesRepoModel _examplesModel;

	public ChainsModel() {
		this(InspectCore.getPhaserHelp(), InspectCore.getPhaserExamplesRepoModel());
	}
	
	public ChainsModel(PhaserJsdocModel jsdocModel, PhaserExamplesRepoModel examplesModel) {
		_jsdoc = jsdocModel;
		_examplesModel = examplesModel;
		build();
	}

	private void build() {
		_chains = new ArrayList<>();

		buildChain("Phaser.Scene", _chains, 0, 2, null);

		for (IMemberContainer container : _jsdoc.getContainers()) {
			String name = container.getName();

			if (!name.equals("Phaser.Scene")) {
				buildChain(name, _chains, 0, 2, null);
			}
		}

		// sort data
		_chains.sort(new Comparator<ChainItem>() {

			@Override
			public int compare(ChainItem a, ChainItem b) {
				return Integer.compare(value(a), value(b));
			}

			private int value(ChainItem item) {

				var phaser = item.getChain().contains("Phaser") ? 0 : 10;
				var m = item.getPhaserMember();
				var inherited = m instanceof ITypeMember && ((ITypeMember) m).isInherited() ? 10 : 0;
				var depth = item.getDepth() + 1;

				return depth * 100_000 + phaser * 1_000 + inherited;
			}
		});

		out.println("Built chains: " + _chains.size());

		// examples

		_exampleItems = new ArrayList<>();
		_examplesLines = new ArrayList<>();

		try {
			for (PhaserExampleCategoryModel category : _examplesModel.getExamplesCategories()) {
				processCategory(category);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void processCategory(PhaserExampleCategoryModel category) throws IOException {
		for (PhaserExampleCategoryModel category2 : category.getSubCategories()) {
			processCategory(category2);
		}

		for (PhaserExampleModel example : category.getTemplates()) {
			_exampleItems.add(example);
			List<String> contentLines = Files.readAllLines(example.getMainFilePath());
			int linenum = 1;
			for (String text : contentLines) {
				text = text.trim();
				if (text.length() > 0 && !text.startsWith("//")) {
					Line line = new Line();
					line.linenum = linenum;
					line.text = text;
					line.example = example;
					_examplesLines.add(line);
				}
				linenum++;
			}
		}
	}

	public List<Match> searchChains(String aQuery, int limit) {
		String query = aQuery.toLowerCase();
		boolean showall = query.trim().length() == 0;

		if (query.startsWith("this.")) {
			query = "scene." + query.substring(5);
		}

		List<Match> matches = new ArrayList<>();
		query = quote(query);
		Pattern pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);

		for (ChainItem item : _chains) {
			Matcher matcher = pattern.matcher(item.getDisplay());
			if (showall || matcher.matches()) {
				Match match = new Match();
				match.item = item;
				if (showall) {
					match.start = 0;
					match.length = 0;
				} else {
					match.start = matcher.start(1);
					match.length = matcher.end(1) - match.start;
				}
				matches.add(match);
				if (matches.size() >= limit) {
					break;
				}
			}
		}
		return matches;
	}

	
	private static String querySpaceToAster(String query) {
		return  Arrays.stream(query.split(" ")).filter( s -> s.trim().length() > 0).collect(joining("*"));
	}
	
	private static String quote(String query) {
		var query2 = querySpaceToAster(query);
		String patternStart = query2.startsWith("*") ? "" : ".*";
		String patternEnd = query2.endsWith("*") ? "" : ".*";

		// String pattern = query.replace(".", "\\.").replace("*", ".*").replace("(",
		// "\\(").replace(")", "\\)")
		// .replace(":", "\\:");

		var sb = new StringBuilder();

		boolean open = false;

		for (char c : query2.toCharArray()) {

			if (c == '*') {
				if (open) {
					sb.append("\\E");
					sb.append(".*");
					open = false;
				} else {
					sb.append(".*");
					sb.append("\\Q");
					open = true;
				}
			} else {
				if (!open) {
					sb.append("\\Q");
					open = true;
				}
				sb.append(c);
			}
		}

		if (open) {
			sb.append("\\E");
		}

		var pattern = sb.toString();

		return patternStart + "(" + pattern + ")" + patternEnd;
	}

	public List<Match> searchExamples(String aQuery, int limit) {
		String query = aQuery.toLowerCase();
		boolean showall = query.trim().length() == 0;
		List<Match> matches = new ArrayList<>();
		if (query.length() > 1 || showall) {
			query = quote(query);
			Pattern pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);

			// search of file names

			for (PhaserExampleModel example : _exampleItems) {
				Matcher matcher = pattern.matcher(example.getFullName());
				if (showall || matcher.matches()) {
					Match match = new Match();
					match.item = example;
					if (showall) {
						match.start = 0;
						match.length = 0;
					} else {
						match.start = matcher.start(1);
						match.length = matcher.end(1) - match.start;
					}
					matches.add(match);
					if (matches.size() >= limit) {
						break;
					}
				}
			}

			// search on lines

			for (Line line : _examplesLines) {
				if (matches.size() >= limit) {
					break;
				}
				Matcher matcher = pattern.matcher(line.text);
				if (showall || matcher.matches()) {
					Match match = new Match();
					match.item = line;
					if (showall) {
						match.start = 0;
						match.length = 0;
					} else {
						match.start = matcher.start(1);
						match.length = matcher.end(1) - match.start;
					}
					matches.add(match);
				}
			}
		}
		return matches;
	}

	public boolean isPhaserType(String typeName) {
		return _jsdoc.getContainerMap().containsKey(typeName);
	}

	static int countDots(String s) {
		int n = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '.') {
				n++;
			}
		}
		return n;
	}

	Set<String> _usednames = new HashSet<>();
	Set<String> _usedChains = new HashSet<>();

	private void buildChain(String containerName, List<ChainItem> chains, int currentDepth, int depthLimit,
			String aPrefix) {
		if (currentDepth == depthLimit) {
			return;
		}

		IMemberContainer container = _jsdoc.getContainerMap().get(containerName);

		if (container == null) {
			return;
		}

		String prefix = aPrefix == null ? containerName : aPrefix;

		if (container instanceof PhaserType) {
			PhaserType type = (PhaserType) container;

			// constructor
			if (!_usednames.contains(containerName)) {

				if (type.isEnum()) {
					String chain = "enum " + containerName;
					_chains.add(new ChainItem(type, chain, containerName, 0));
					_usedChains.add(chain);
				} else {
					// constructor
					{
						String chain = "class " + containerName + "(";
						int i = 0;
						for (PhaserMethodArg arg : type.getConstructorArgs()) {
							chain += (i > 0 ? "," : "") + arg.getName();
							i++;
						}
						chain += ")";

						chain += (type.getExtends().isEmpty() ? "" : " extends");
						i = 0;
						for (String e : type.getExtends()) {
							chain += (i == 0 ? " " : "|") + e;
							i++;
						}

						_chains.add(new ChainItem(type, chain, containerName, 0));
					}
				}

				_usednames.add(containerName);
			}
		} else {
			PhaserNamespace namespace = (PhaserNamespace) container;
			String chain = "namespace " + containerName;
			if (!_usedChains.contains(chain)) {
				_chains.add(new ChainItem(namespace, chain, containerName, 0));
				_usedChains.add(chain);
			}

			for (PhaserNamespace namespace2 : namespace.getNamespaces()) {
				String longname = containerName + "." + namespace2.getName();
				buildChain(longname, chains, currentDepth, depthLimit, containerName);
			}
		}

		// types

		for (PhaserType type : container.getTypes()) {
			String longname = containerName + "." + type.getName();
			buildChain(longname, chains, currentDepth, depthLimit, containerName);
		}

		// properties

		boolean is_Phaser_Scenes_Systems = containerName.equals("Phaser.Scenes.Systems");

		for (PhaserVariable prop : container.getAllProperties()) {
			for (String typename : prop.getTypes()) {
				String name = prop.getName();
				String chain = prefix + "." + name;
				if (_usedChains.contains(chain)) {
					continue;
				}
				chains.add(new ChainItem(prop, chain, typename, currentDepth));
				_usedChains.add(chain);

				if (is_Phaser_Scenes_Systems) {
					// do not enters into Phaser.Scenes.Systems.(property) because it repeats the
					// same of the Phaser.Scene.(property).
					continue;
				}

				if (typename.equals("Phaser.Scene")) {
					// do not enters into Phaser.Scene chains
					continue;
				}

				if (!typename.startsWith("Phaser") && !typename.startsWith("Matter")) {
					// avoid to enters into non-Phaser types
					continue;
				}

				buildChain(typename, chains, currentDepth + 1, depthLimit, chain);
			}

		}

		// constants

		for (PhaserVariable cons : container.getAllConstants()) {
			String name = cons.getName();
			String typename = cons.getTypes()[0];
			String chain = prefix + "." + name;
			if (_usedChains.contains(chain)) {
				continue;
			}
			chains.add(new ChainItem(cons, chain, typename, currentDepth));
			_usedChains.add(chain);

			buildChain(typename, chains, currentDepth + 1, depthLimit, chain);
		}

		// methods

		for (PhaserMethod method : container.getAllMethods()) {
			String[] methodTypes = method.getReturnTypes();

			if (methodTypes.length == 0) {
				methodTypes = new String[] { "void" };
			}

			for (String typename : methodTypes) {
				String name = method.getName();
				String chain = prefix + "." + name + "(";
				int i = 0;
				for (PhaserVariable param : method.getArgs()) {
					if (i > 0) {
						chain += ", ";
					}
					chain += param.getName();
					i++;
				}
				chain += ")";

				if (_usedChains.contains(chain)) {
					continue;
				}
				chains.add(new ChainItem(method, chain, typename, currentDepth));
				_usedChains.add(chain);
			}
		}
	}

	public ArrayList<ChainItem> getChains() {
		return _chains;
	}
}
