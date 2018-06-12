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

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.examples.ExampleCategoryModel;
import phasereditor.inspect.core.examples.ExampleModel;
import phasereditor.inspect.core.examples.ExamplesModel;
import phasereditor.inspect.core.jsdoc.IMemberContainer;
import phasereditor.inspect.core.jsdoc.PhaserJSDoc;
import phasereditor.inspect.core.jsdoc.PhaserMethod;
import phasereditor.inspect.core.jsdoc.PhaserMethodArg;
import phasereditor.inspect.core.jsdoc.PhaserNamespace;
import phasereditor.inspect.core.jsdoc.PhaserType;
import phasereditor.inspect.core.jsdoc.PhaserVariable;

public class ChainsModel {
	private ArrayList<ChainItem> _chains;
	private List<String> _examplesFiles;
	private List<Line> _examplesLines;
	private PhaserJSDoc _jsdoc;

	public ChainsModel() {
		_jsdoc = PhaserJSDoc.getInstance();
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
				if (a.getDepth() != b.getDepth()) {
					return a.getDepth() - b.getDepth();
				}

				boolean a_phaser = a.getChain().contains("Phaser");
				boolean b_phaser = b.getChain().contains("Phaser");

				if (a_phaser != b_phaser) {
					return a_phaser ? -1 : 1;
				}

				a_phaser = a.getReturnTypeName().contains("Phaser");
				b_phaser = b.getReturnTypeName().contains("Phaser");

				if (a_phaser != b_phaser) {
					return a_phaser ? -1 : 1;
				}

				int a_type_weight = a.isType() ? 0 : countDots(a.getReturnTypeName());
				int b_type_weight = b.isType() ? 0 : countDots(b.getReturnTypeName());

				if (a_type_weight != b_type_weight) {
					return (a_type_weight - b_type_weight);
				}

				return 0;
			}
		});

		out.println("Built chains: " + _chains.size());

		// examples

		_examplesFiles = new ArrayList<>();
		_examplesLines = new ArrayList<>();

		try {
			ExamplesModel examples = InspectCore.getExamplesModel();
			for (ExampleCategoryModel category : examples.getExamplesCategories()) {
				for (ExampleModel example : category.getTemplates()) {
					String filename = example.getCategory().getName().toLowerCase() + "/"
							+ example.getInfo().getMainFile();
					_examplesFiles.add(filename);
					List<String> contentLines = Files.readAllLines(example.getMainFilePath());
					int linenum = 1;
					for (String text : contentLines) {
						text = text.trim();
						if (text.length() > 0 && !text.startsWith("//")) {
							Line line = new Line();
							line.filename = filename;
							line.linenum = linenum;
							line.text = text;
							_examplesLines.add(line);
						}
						linenum++;
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
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

	private static String quote(String query) {
		String patternStart = query.startsWith("*") ? "" : ".*";
		String patternEnd = query.endsWith("*") ? "" : ".*";

		String pattern = query.replace(".", "\\.").replace("*", ".*").replace("(", "\\(").replace(")", "\\)")
				.replace(":", "\\:");

		return patternStart + "(" + pattern + ")" + patternEnd;
	}

	public List<Match> searchExamples(String aQuery, int limit) {
		String query = aQuery.toLowerCase();
		boolean showall = query.trim().length() == 0;
		List<Match> matches = new ArrayList<>();
		if (query.length() > 2 || showall) {
			query = quote(query);
			Pattern pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);

			// search of file names

			for (String filename : _examplesFiles) {
				Matcher matcher = pattern.matcher(filename);
				if (showall || matcher.matches()) {
					Match match = new Match();
					match.item = filename;
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

		for (PhaserVariable prop : container.getProperties()) {
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

		for (PhaserVariable cons : container.getConstants()) {
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

		for (PhaserMethod method : container.getMethods()) {
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
