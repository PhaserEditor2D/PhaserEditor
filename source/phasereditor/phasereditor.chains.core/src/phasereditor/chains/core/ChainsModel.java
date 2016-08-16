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
import phasereditor.inspect.core.jsdoc.PhaserConstant;
import phasereditor.inspect.core.jsdoc.PhaserJSDoc;
import phasereditor.inspect.core.jsdoc.PhaserMethod;
import phasereditor.inspect.core.jsdoc.PhaserMethodArg;
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

		buildChain("Phaser.Game", _chains, 0, 3, null);

		for (PhaserType unit : _jsdoc.getTypes()) {
			String name = unit.getName();

			if (!name.equals("Phaser.Game")) {
				buildChain(name, _chains, 0, 2, null);
			}
		}

		// global constants
		for (PhaserConstant cons : _jsdoc.getGlobalConstants()) {
			String name = cons.getName();
			String type = cons.getTypes()[0];
			String chain = "const Phaser." + name;
			_chains.add(new ChainItem(cons, chain, type, 0));
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
			query = "state." + query.substring(5);
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
		return _jsdoc.getTypesMap().containsKey(typeName);
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

	Set<String> _usedTypes = new HashSet<>();

	private void buildChain(String className, List<ChainItem> chains, int currentDepth, int depth, String aPrefix) {
		if (currentDepth == depth) {
			return;
		}

		PhaserType unit = _jsdoc.getType(className);

		if (unit == null) {
			return;
		}

		// constructor
		if (!_usedTypes.contains(className)) {
			// class
			{
				String chain = "class " + className + (unit.getExtends().isEmpty() ? "" : " extends");
				int i = 0;
				for (String e : unit.getExtends()) {
					chain += (i == 0 ? " " : "|") + e;
					i++;
				}
				_chains.add(new ChainItem(unit, chain, className, 0));
			}

			// constructor
			{
				String chain = "new " + className + "(";
				int i = 0;
				for (PhaserMethodArg arg : unit.getConstructorArgs()) {
					chain += (i > 0 ? "," : "") + arg.getName();
					i++;
				}
				chain += ")";

				_chains.add(new ChainItem(unit, chain, className, 0));
			}
			_usedTypes.add(className);
		}

		String prefix = aPrefix == null ? className : aPrefix;

		// properties

		for (PhaserVariable prop : unit.getProperties()) {
			for (String type : prop.getTypes()) {
				String name = prop.getName();
				String chain = prefix + "." + name;
				chains.add(new ChainItem(prop, chain, type, currentDepth));
				buildChain(type, chains, currentDepth + 1, depth, chain);
			}
		}

		// constants

		for (PhaserVariable cons : unit.getConstants()) {
			String name = cons.getName();
			String type = cons.getTypes()[0];
			String chain = prefix + "." + name;
			chains.add(new ChainItem(cons, chain, type, currentDepth));
		}

		// methods

		for (PhaserMethod method : unit.getMethods()) {
			String[] methodTypes = method.getReturnTypes();

			if (methodTypes.length == 0) {
				methodTypes = new String[] { "void" };
			}

			for (String type : methodTypes) {
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

				chains.add(new ChainItem(method, chain, type, currentDepth));
			}
		}
	}

	public ArrayList<ChainItem> getChains() {
		return _chains;
	}
}
