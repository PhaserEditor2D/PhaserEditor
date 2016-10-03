/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.Compiler;
import org.eclipse.wst.jsdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.wst.jsdt.internal.compiler.ICompilerRequestor;
import org.eclipse.wst.jsdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.wst.jsdt.internal.compiler.IProblemFactory;
import org.eclipse.wst.jsdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction;
import org.eclipse.wst.jsdt.internal.compiler.env.INameEnvironment;
import org.eclipse.wst.jsdt.internal.compiler.env.ISourceType;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.wst.jsdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.wst.jsdt.internal.compiler.parser.Parser;
import org.eclipse.wst.jsdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.wst.jsdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObjectToInt;
import org.eclipse.wst.jsdt.internal.compiler.util.Messages;
import org.eclipse.wst.jsdt.internal.core.BinaryMember;
import org.eclipse.wst.jsdt.internal.core.CancelableNameEnvironment;
import org.eclipse.wst.jsdt.internal.core.CancelableProblemFactory;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.core.NameLookup;
import org.eclipse.wst.jsdt.internal.core.SourceRefElement;
import org.eclipse.wst.jsdt.internal.core.SourceTypeElementInfo;
import org.eclipse.wst.jsdt.internal.core.util.BindingKeyResolver;
import org.eclipse.wst.jsdt.internal.core.util.CommentRecorderParser;
import org.eclipse.wst.jsdt.internal.core.util.DOMFinder;

/**
 * 
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
class JavaScriptUnitResolver extends Compiler {

	/* A list of int */
	static class IntArrayList {
		public int[] list = new int[5];
		public int length = 0;
		public void add(int i) {
			if (this.list.length == this.length) {
				System.arraycopy(this.list, 0, this.list = new int[this.length*2], 0, this.length);
			}
				this.list[this.length++] = i;
			}
		}

	/*
	 * The sources that were requested.
	 * Map from file name (char[]) to IJavaScriptUnit.
	 */
	HashtableOfObject requestedSources;

	/*
	 * The binding keys that were requested.
	 * Map from file name (char[]) to BindingKey (or ArrayList if multiple keys in the same file).
	 */
	HashtableOfObject requestedKeys;

	DefaultBindingResolver.BindingTables bindingTables;

	boolean hasCompilationAborted;

	private IProgressMonitor monitor;

	/**
	 * Answer a new CompilationUnitVisitor using the given name environment and validator options.
	 * The environment and options will be in effect for the lifetime of the compiler.
	 * When the validator is run, compilation results are sent to the given requestor.
	 *
	 *  @param environment org.eclipse.wst.jsdt.internal.compiler.api.env.INameEnvironment
	 *      Environment used by the validator in order to resolve type and package
	 *      names. The name environment implements the actual connection of the compiler
	 *      to the outside world (for example, in batch mode the name environment is performing
	 *      pure file accesses, reuse previous build state or connection to repositories).
	 *      Note: the name environment is responsible for implementing the actual includepath
	 *            rules.
	 *
	 *  @param policy org.eclipse.wst.jsdt.internal.compiler.api.problem.IErrorHandlingPolicy
	 *      Configurable part for problem handling, allowing the validator client to
	 *      specify the rules for handling problems (stop on first error or accumulate
	 *      them all) and at the same time perform some actions such as opening a dialog
	 *      in UI when validating interactively.
	 *      @see org.eclipse.wst.jsdt.internal.compiler.DefaultErrorHandlingPolicies
	 *
	 *	@param compilerOptions The validator options to use for the resolution.
	 *
	 *  @param requestor org.eclipse.wst.jsdt.internal.compiler.api.ICompilerRequestor
	 *      Component which will receive and persist all compilation results and is intended
	 *      to consume them as they are produced. Typically, in a batch compiler, it is
	 *      responsible for writing out the actual .class files to the file system.
	 *      @see org.eclipse.wst.jsdt.internal.compiler.CompilationResult
	 *
	 *  @param problemFactory org.eclipse.wst.jsdt.internal.compiler.api.problem.IProblemFactory
	 *      Factory used inside the validator to create problem descriptors. It allows the
	 *      validator client to supply its own representation of compilation problems in
	 *      order to avoid object conversions. Note that the factory is not supposed
	 *      to accumulate the created problems, the validator will gather them all and hand
	 *      them back as part of the javaScript unit result.
	 */
	public JavaScriptUnitResolver(
		INameEnvironment environment,
		IErrorHandlingPolicy policy,
		CompilerOptions compilerOptions,
		ICompilerRequestor requestor,
		IProblemFactory problemFactory,
		IProgressMonitor monitor) {

		super(environment, policy, compilerOptions, requestor, problemFactory);
		this.hasCompilationAborted = false;
		this.monitor =monitor;
	}

	/*
	 * Add additional source types
	 */
	public void accept(ISourceType[] sourceTypes, PackageBinding packageBinding, AccessRestriction accessRestriction) {
		// Need to reparse the entire source of the javaScript unit so as to get source positions
		// (case of processing a source that was not known by beginToCompile (e.g. when asking to createBinding))
		SourceTypeElementInfo sourceType = (SourceTypeElementInfo) sourceTypes[0];
		accept((org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit) sourceType.getHandle().getJavaScriptUnit(), accessRestriction);
	}

	/**
	 * Add the initial set of javaScript units into the loop
	 *  ->  build javaScript unit declarations, their bindings and record their results.
	 */
	protected void beginToCompile(org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit[] sourceUnits, String[] bindingKeys) {
		int sourceLength = sourceUnits.length;
		int keyLength = bindingKeys.length;
		int maxUnits = sourceLength + keyLength;
		this.totalUnits = 0;
		this.unitsToProcess = new CompilationUnitDeclaration[maxUnits];
		int index = 0;

		// walks the source units
		this.requestedSources = new HashtableOfObject();
		for (int i = 0; i < sourceLength; i++) {
			org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit sourceUnit = sourceUnits[i];
			CompilationUnitDeclaration parsedUnit;
			CompilationResult unitResult =
				new CompilationResult(sourceUnit, index++, maxUnits, this.options.maxProblemsPerUnit);
			try {
				if (options.verbose) {
					this.out.println(
						Messages.bind(Messages.compilation_request,
						new String[] {
							String.valueOf(index++ + 1),
							String.valueOf(maxUnits),
							new String(sourceUnit.getFileName())
						}));
				}
				// diet parsing for large collection of units
				if (this.totalUnits < this.parseThreshold) {
					parsedUnit = this.parser.parse(sourceUnit, unitResult);
				} else {
					parsedUnit = this.parser.dietParse(sourceUnit, unitResult);
				}
				// initial type binding creation
				this.lookupEnvironment.buildTypeBindings(parsedUnit, null /*no access restriction*/);
				addCompilationUnit(sourceUnit, parsedUnit);
				this.requestedSources.put(unitResult.getFileName(), sourceUnit);
				worked(1);
			} finally {
				sourceUnits[i] = null; // no longer hold onto the unit
			}
		}

		// walk the binding keys
		this.requestedKeys = new HashtableOfObject();
		for (int i = 0; i < keyLength; i++) {
			BindingKeyResolver resolver = new BindingKeyResolver(bindingKeys[i], this, this.lookupEnvironment);
			resolver.parse(true/*pause after fully qualified name*/);
			// If it doesn't have a type name, then it is either an array type, package or base type, which will definitely not have a javaScript unit.
			// Skipping it will speed up performance because the call will open jars. (theodora)
			CompilationUnitDeclaration parsedUnit = resolver.hasTypeName() ? resolver.getCompilationUnitDeclaration() : null;
			if (parsedUnit != null) {
				char[] fileName = parsedUnit.compilationResult.getFileName();
				Object existing = this.requestedKeys.get(fileName);
				if (existing == null)
					this.requestedKeys.put(fileName, resolver);
				else if (existing instanceof ArrayList)
					((ArrayList) existing).add(resolver);
				else {
					ArrayList list = new ArrayList();
					list.add(existing);
					list.add(resolver);
					this.requestedKeys.put(fileName, list);
				}

			} else {
				char[] key = resolver.hasTypeName()
					? resolver.getKey().toCharArray() // binary binding
					: CharOperation.concatWith(resolver.compoundName(), '.'); // package binding or base type binding
				this.requestedKeys.put(key, resolver);
			}
			worked(1);
		}

		// binding resolution
		lookupEnvironment.completeTypeBindings();
	}

	IBinding createBinding(String key) {
		if (this.bindingTables == null)
			throw new RuntimeException("Cannot be called outside ASTParser#createASTs(...)"); //$NON-NLS-1$
		BindingKeyResolver keyResolver = new BindingKeyResolver(key, this, this.lookupEnvironment);
		Binding compilerBinding = keyResolver.getCompilerBinding();
		if (compilerBinding == null) return null;
		DefaultBindingResolver resolver = new DefaultBindingResolver(this.lookupEnvironment, null/*no owner*/, this.bindingTables, false);
		return resolver.getBinding(compilerBinding);
	}

	public static JavaScriptUnit convert(CompilationUnitDeclaration compilationUnitDeclaration, char[] source, int apiLevel, Map options, boolean needToResolveBindings, WorkingCopyOwner owner, DefaultBindingResolver.BindingTables bindingTables, int flags, IProgressMonitor monitor) {
		BindingResolver resolver = null;
		AST ast = AST.newAST(apiLevel);
		ast.setDefaultNodeFlag(ASTNode.ORIGINAL);
		JavaScriptUnit compilationUnit = null;
		ASTConverter converter = new ASTConverter(options, needToResolveBindings, monitor);
		if (needToResolveBindings) {
			resolver = new DefaultBindingResolver(compilationUnitDeclaration.scope, owner, bindingTables, (flags & IJavaScriptUnit.ENABLE_BINDINGS_RECOVERY) != 0);
			ast.setFlag(flags | AST.RESOLVED_BINDINGS);
		} else {
			resolver = new BindingResolver();
			ast.setFlag(flags);
		}
		ast.setBindingResolver(resolver);
		converter.setAST(ast);
		compilationUnit = converter.convert(compilationUnitDeclaration, source);
		compilationUnit.setLineEndTable(compilationUnitDeclaration.compilationResult.getLineSeparatorPositions());
		ast.setDefaultNodeFlag(0);
		ast.setOriginalModificationCount(ast.modificationCount());
		return compilationUnit;
	}

	protected static CompilerOptions getCompilerOptions(Map options, boolean statementsRecovery) {
		CompilerOptions compilerOptions = new CompilerOptions(options);
		compilerOptions.performMethodsFullRecovery = statementsRecovery;
		compilerOptions.performStatementsRecovery = statementsRecovery;
		compilerOptions.parseLiteralExpressionsAsConstants = false;
		compilerOptions.storeAnnotations = true /*store annotations in the bindings*/;
		return compilerOptions;
	}
	/*
	 *  Low-level API performing the actual compilation
	 */
	protected static IErrorHandlingPolicy getHandlingPolicy() {

		// passes the initial set of files to the batch oracle (to avoid finding more than once the same units when case insensitive match)
		return new IErrorHandlingPolicy() {
			public boolean stopOnFirstError() {
				return false;
			}
			public boolean proceedOnErrors() {
				return false; // stop if there are some errors
			}
		};
	}

	/*
	 * Answer the component to which will be handed back compilation results from the compiler
	 */
	protected static ICompilerRequestor getRequestor() {
		return new ICompilerRequestor() {
			public void acceptResult(CompilationResult compilationResult) {
				// do nothing
			}
		};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.internal.compiler.Compiler#initializeParser()
	 */
	public void initializeParser() {
		this.parser = new CommentRecorderParser(this.problemReporter, false);
	}
	public void process(CompilationUnitDeclaration unit, int i) {
		// don't resolve a second time the same unit (this would create the same binding twice)
		char[] fileName = unit.compilationResult.getFileName();
		if (!this.requestedKeys.containsKey(fileName) && !this.requestedSources.containsKey(fileName))
			super.process(unit, i);
	}
	/*
	 * Compiler crash recovery in case of unexpected runtime exceptions
	 */
	protected void handleInternalException(
			Throwable internalException,
			CompilationUnitDeclaration unit,
			CompilationResult result) {
		super.handleInternalException(internalException, unit, result);
		if (unit != null) {
			removeUnresolvedBindings(unit);
		}
	}

	/*
	 * Compiler recovery in case of internal AbortCompilation event
	 */
	protected void handleInternalException(
			AbortCompilation abortException,
			CompilationUnitDeclaration unit) {
		super.handleInternalException(abortException, unit);
		if (unit != null) {
			removeUnresolvedBindings(unit);
		}
		this.hasCompilationAborted = true;
	}

	public static void parse(IJavaScriptUnit[] compilationUnits, ASTRequestor astRequestor, int apiLevel, Map options, int flags, IProgressMonitor monitor) {
		try {
			CompilerOptions compilerOptions = new CompilerOptions(options);
			Parser parser = new CommentRecorderParser(
				new ProblemReporter(
						DefaultErrorHandlingPolicies.proceedWithAllProblems(),
						compilerOptions,
						new DefaultProblemFactory()),
				false);
			int length = compilationUnits.length;
			if (monitor != null) monitor.beginTask("", length); //$NON-NLS-1$
			for (int i = 0; i < length; i++) {
				org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit sourceUnit = (org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit) compilationUnits[i];
				CompilationResult compilationResult = new CompilationResult(sourceUnit, 0, 0, compilerOptions.maxProblemsPerUnit);
				CompilationUnitDeclaration compilationUnitDeclaration = parser.dietParse(sourceUnit, compilationResult);
				parser.inferTypes(compilationUnitDeclaration, compilerOptions);

				if (compilationUnitDeclaration.ignoreMethodBodies) {
					compilationUnitDeclaration.ignoreFurtherInvestigation = true;
					// if initial diet parse did not work, no need to dig into method bodies.
					continue;
				}

				//fill the methods bodies in order for the code to be generated
				//real parse of the method....
				org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration[] types = compilationUnitDeclaration.types;
				if (types != null) {
					for (int j = types.length; --j >= 0;)
						types[j].parseMethod(parser, compilationUnitDeclaration);
				}

				// convert AST
				JavaScriptUnit node = convert(compilationUnitDeclaration, parser.scanner.getSource(), apiLevel, options, false/*don't resolve binding*/, null/*no owner needed*/, null/*no binding table needed*/, flags /* flags */, monitor);
				node.setTypeRoot(compilationUnits[i]);

				// accept AST
				astRequestor.acceptAST(compilationUnits[i], node);

				if (monitor != null) monitor.worked(1);
			}
		} finally {
			if (monitor != null) monitor.done();
		}
	}

	public static CompilationUnitDeclaration parse(
			org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit sourceUnit,
			NodeSearcher nodeSearcher,
			Map settings,
			int flags) {
		if (sourceUnit == null) {
			throw new IllegalStateException();
		}
		CompilerOptions compilerOptions = new CompilerOptions(settings);
		boolean statementsRecovery = (flags & IJavaScriptUnit.ENABLE_STATEMENTS_RECOVERY) != 0;
		compilerOptions.performMethodsFullRecovery = statementsRecovery;
		compilerOptions.performStatementsRecovery = statementsRecovery;
		Parser parser = new CommentRecorderParser(
			new ProblemReporter(
					DefaultErrorHandlingPolicies.proceedWithAllProblems(),
					compilerOptions,
					new DefaultProblemFactory()),
			false);
		CompilationResult compilationResult = new CompilationResult(sourceUnit, 0, 0, compilerOptions.maxProblemsPerUnit);
		CompilationUnitDeclaration compilationUnitDeclaration = parser.dietParse(sourceUnit, compilationResult);

		parser.inferTypes(compilationUnitDeclaration, compilerOptions);
		if (compilationUnitDeclaration.ignoreMethodBodies) {
			compilationUnitDeclaration.ignoreFurtherInvestigation = true;
			// if initial diet parse did not work, no need to dig into method bodies.
			return null;
		}

		if (nodeSearcher != null) {
			char[] source = parser.scanner.getSource();
			int searchPosition = nodeSearcher.position;
			if (searchPosition < 0 || searchPosition > source.length) {
				// the position is out of range. There is no need to search for a node.
	 			return compilationUnitDeclaration;
			}

			compilationUnitDeclaration.traverse(nodeSearcher, compilationUnitDeclaration.scope);

			org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode node = nodeSearcher.found;
	 		if (node == null) {
	 			return compilationUnitDeclaration;
	 		}

	 		org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration enclosingTypeDeclaration = nodeSearcher.enclosingType;

			if (node instanceof AbstractMethodDeclaration) {
				((AbstractMethodDeclaration)node).parseStatements(parser, compilationUnitDeclaration);
			} else if (enclosingTypeDeclaration != null) {
				if (node instanceof org.eclipse.wst.jsdt.internal.compiler.ast.Initializer) {
					((org.eclipse.wst.jsdt.internal.compiler.ast.Initializer) node).parseStatements(parser, enclosingTypeDeclaration, compilationUnitDeclaration);
				} else {
					((org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration)node).parseMethod(parser, compilationUnitDeclaration);
				}
			}
		} else {
			//fill the methods bodies in order for the code to be generated
			//real parse of the method....
			org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration[] types = compilationUnitDeclaration.types;
			if (types != null) {
				for (int i = types.length; --i >= 0;)
					types[i].parseMethod(parser, compilationUnitDeclaration);
			}
		}
		return compilationUnitDeclaration;
	}

	public static void resolve(
		IJavaScriptUnit[] compilationUnits,
		String[] bindingKeys,
		ASTRequestor requestor,
		int apiLevel,
		Map options,
		IJavaScriptProject javaProject,
		WorkingCopyOwner owner,
		int flags,
		IProgressMonitor monitor) {

		CancelableNameEnvironment environment = null;
		CancelableProblemFactory problemFactory = null;
		try {
			if (monitor != null) {
				int amountOfWork = (compilationUnits.length + bindingKeys.length) * 2; // 1 for beginToCompile, 1 for resolve
				monitor.beginTask("", amountOfWork); //$NON-NLS-1$
			}
			environment = new CancelableNameEnvironment(((JavaProject) javaProject), owner, monitor);
			problemFactory = new CancelableProblemFactory(monitor);
			JavaScriptUnitResolver resolver =
				new JavaScriptUnitResolver(
					environment,
					getHandlingPolicy(),
					getCompilerOptions(options, (flags & IJavaScriptUnit.ENABLE_STATEMENTS_RECOVERY) != 0),
					getRequestor(),
					problemFactory,
					monitor);

			resolver.resolve(compilationUnits, bindingKeys, requestor, apiLevel, options, owner, flags);
			if (NameLookup.VERBOSE) {
				System.out.println(Thread.currentThread() + " TIME SPENT in NameLoopkup#seekTypesInSourcePackage: " + environment.nameLookup.timeSpentInSeekTypesInSourcePackage + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
				System.out.println(Thread.currentThread() + " TIME SPENT in NameLoopkup#seekTypesInBinaryPackage: " + environment.nameLookup.timeSpentInSeekTypesInBinaryPackage + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
			}
		} catch (JavaScriptModelException e) {
			// project doesn't exist -> simple parse without resolving
			parse(compilationUnits, requestor, apiLevel, options, flags, monitor);
		} finally {
			if (monitor != null) monitor.done();
			if (environment != null) {
				environment.monitor = null; // don't hold a reference to this external object
			}
			if (problemFactory != null) {
				problemFactory.monitor = null; // don't hold a reference to this external object
			}
		}
	}
	public static CompilationUnitDeclaration resolve(
			org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit sourceUnit,
			IJavaScriptProject javaProject,
			NodeSearcher nodeSearcher,
			Map options,
			WorkingCopyOwner owner,
			int flags,
			IProgressMonitor monitor) throws JavaScriptModelException {

		CompilationUnitDeclaration unit = null;
		CancelableNameEnvironment environment = null;
		CancelableProblemFactory problemFactory = null;
		JavaScriptUnitResolver resolver = null;
		try {
			environment = new CancelableNameEnvironment(((JavaProject)javaProject), owner, monitor);
			environment.setCompilationUnit(sourceUnit);
			problemFactory = new CancelableProblemFactory(monitor);
			resolver =
				new JavaScriptUnitResolver(
					environment,
					getHandlingPolicy(),
					getCompilerOptions(options, (flags & IJavaScriptUnit.ENABLE_STATEMENTS_RECOVERY) != 0),
					getRequestor(),
					problemFactory,
					monitor);

			unit =
				resolver.resolve(
					null, // no existing javaScript unit declaration
					sourceUnit,
					nodeSearcher,
					true, // method verification
					true, // analyze code
					true); // generate code
			if (resolver.hasCompilationAborted) {
				// the bindings could not be resolved due to missing types in name environment
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=86541
				CompilationUnitDeclaration unitDeclaration = parse(sourceUnit, nodeSearcher, options, flags);
				if (unit != null && unit.compilationResult != null) {
					final int problemCount = unit.compilationResult.problemCount;
					if (problemCount != 0) {
						unitDeclaration.compilationResult.problems = new CategorizedProblem[problemCount];
						System.arraycopy(unit.compilationResult.problems, 0, unitDeclaration.compilationResult.problems, 0, problemCount);
						unitDeclaration.compilationResult.problemCount = problemCount;
					}
				}
				return unitDeclaration;
			}
			if (NameLookup.VERBOSE) {
				System.out.println(Thread.currentThread() + " TIME SPENT in NameLoopkup#seekTypesInSourcePackage: " + environment.nameLookup.timeSpentInSeekTypesInSourcePackage + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
				System.out.println(Thread.currentThread() + " TIME SPENT in NameLoopkup#seekTypesInBinaryPackage: " + environment.nameLookup.timeSpentInSeekTypesInBinaryPackage + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
			}
			return unit;
		} finally {
			if (environment != null) {
				environment.monitor = null; // don't hold a reference to this external object
			}
			if (problemFactory != null) {
				problemFactory.monitor = null; // don't hold a reference to this external object
			}
			// first unit cleanup is done by caller, but cleanup all enqueued requested units (not processed)
//			if (resolver != null) {
//				for (int i = 1; i <  resolver.totalUnits; i++) { // could be more requested units
//					CompilationUnitDeclaration parsedUnit = resolver.unitsToProcess[i];
//					if (parsedUnit.scope != null)
//						parsedUnit.scope.faultInTypes(); // force resolution of signatures, so clients can query DOM AST
//					parsedUnit.cleanUp();
//				}
//			}
		}
	}
	public static IBinding[] resolve(
		final IJavaScriptElement[] elements,
		int apiLevel,
		Map compilerOptions,
		IJavaScriptProject javaProject,
		WorkingCopyOwner owner,
		int flags,
		IProgressMonitor monitor) {

		final int length = elements.length;
		final HashMap sourceElementPositions = new HashMap(); // a map from IJavaScriptUnit to int[] (positions in elements)
		int cuNumber = 0;
		final HashtableOfObjectToInt binaryElementPositions = new HashtableOfObjectToInt(); // a map from String (binding key) to int (position in elements)
		for (int i = 0; i < length; i++) {
			IJavaScriptElement element = elements[i];
			if (!(element instanceof SourceRefElement))
				throw new IllegalStateException(element + " is not part of a javaScript unit or class file"); //$NON-NLS-1$
			Object cu = element.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
			if (cu != null) {
				// source member
				IntArrayList intList = (IntArrayList) sourceElementPositions.get(cu);
				if (intList == null) {
					sourceElementPositions.put(cu, intList = new IntArrayList());
					cuNumber++;
				}
				intList.add(i);
			} else {
				// binary member
				try {
					String key = ((BinaryMember) element).getKey(true/*open to get resolved info*/);
					binaryElementPositions.put(key, i);
				} catch (JavaScriptModelException e) {
					throw new IllegalArgumentException(element + " does not exist"); //$NON-NLS-1$
				}
			}
		}
		IJavaScriptUnit[] cus = new IJavaScriptUnit[cuNumber];
		sourceElementPositions.keySet().toArray(cus);

		int bindingKeyNumber = binaryElementPositions.size();
		String[] bindingKeys = new String[bindingKeyNumber];
		binaryElementPositions.keysToArray(bindingKeys);

		class Requestor extends ASTRequestor {
			IBinding[] bindings = new IBinding[length];
			public void acceptAST(IJavaScriptUnit source, JavaScriptUnit ast) {
				// TODO (jerome) optimize to visit the AST only once
				IntArrayList intList = (IntArrayList) sourceElementPositions.get(source);
				for (int i = 0; i < intList.length; i++) {
					final int index = intList.list[i];
					SourceRefElement element = (SourceRefElement) elements[index];
					DOMFinder finder = new DOMFinder(ast, element, true/*resolve binding*/);
					try {
						finder.search();
					} catch (JavaScriptModelException e) {
						throw new IllegalArgumentException(element + " does not exist"); //$NON-NLS-1$
					}
					this.bindings[index] = finder.foundBinding;
				}
			}
			public void acceptBinding(String bindingKey, IBinding binding) {
				int index = binaryElementPositions.get(bindingKey);
				this.bindings[index] = binding;
			}
		}
		Requestor requestor = new Requestor();
		resolve(cus, bindingKeys, requestor, apiLevel, compilerOptions, javaProject, owner, flags, monitor);
		return requestor.bindings;
	}
	/*
	 * When unit result is about to be accepted, removed back pointers
	 * to unresolved bindings
	 */
	public void removeUnresolvedBindings(CompilationUnitDeclaration compilationUnitDeclaration) {
		final org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration[] types = compilationUnitDeclaration.types;
		if (types != null) {
			for (int i = 0, max = types.length; i < max; i++) {
				removeUnresolvedBindings(types[i]);
			}
		}
	}
	private void removeUnresolvedBindings(org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration type) {
		final org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration[] memberTypes = type.memberTypes;
		if (memberTypes != null) {
			for (int i = 0, max = memberTypes.length; i < max; i++){
				removeUnresolvedBindings(memberTypes[i]);
			}
		}
		if (type.binding != null && (type.binding.modifiers & ExtraCompilerModifiers.AccUnresolved) != 0) {
			type.binding = null;
		}

		final org.eclipse.wst.jsdt.internal.compiler.ast.FieldDeclaration[] fields = type.fields;
		if (fields != null) {
			for (int i = 0, max = fields.length; i < max; i++){
				if (fields[i].binding != null && (fields[i].binding.modifiers & ExtraCompilerModifiers.AccUnresolved) != 0) {
					fields[i].binding = null;
				}
			}
		}

		final AbstractMethodDeclaration[] methods = type.methods;
		if (methods != null) {
			for (int i = 0, max = methods.length; i < max; i++){
				if (methods[i].hasBinding() && (methods[i].getBinding().modifiers & ExtraCompilerModifiers.AccUnresolved) != 0) {
					methods[i].setBinding(null);
				}
			}
		}
	}

	private void resolve(IJavaScriptUnit[] compilationUnits, String[] bindingKeys, ASTRequestor astRequestor, int apiLevel, Map compilerOptions, WorkingCopyOwner owner, int flags) {

		// temporararily connect ourselves to the ASTResolver - must disconnect when done
		astRequestor.compilationUnitResolver = this;
		this.bindingTables = new DefaultBindingResolver.BindingTables();
		CompilationUnitDeclaration unit = null;
		int i = 0;
		try {
			int length = compilationUnits.length;
			org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit[] sourceUnits = new org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit[length];
			System.arraycopy(compilationUnits, 0, sourceUnits, 0, length);
			beginToCompile(sourceUnits, bindingKeys);
			// process all units (some more could be injected in the loop by the lookup environment)
			for (; i < this.totalUnits; i++) {
				if (this.requestedSources.size() == 0 && this.requestedKeys.size() == 0) {
					// no need to keep resolving if no more ASTs and no more binding keys are needed
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=114935
					// cleanup remaining units
					for (; i < this.totalUnits; i++) {
						this.unitsToProcess[i].cleanUp();
						this.unitsToProcess[i] = null;
					}
					break;
				}
				unit = this.unitsToProcess[i];
				try {
					super.process(unit, i); // this.process(...) is optimized to not process already known units

					// requested AST
					char[] fileName = unit.compilationResult.getFileName();
					IJavaScriptUnit source = (IJavaScriptUnit) this.requestedSources.get(fileName);
					if (source != null) {
						// convert AST
						CompilationResult compilationResult = unit.compilationResult;
						org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit sourceUnit = compilationResult.compilationUnit;
						char[] contents = sourceUnit.getContents();
						AST ast = AST.newAST(apiLevel);
						ast.setFlag(flags | AST.RESOLVED_BINDINGS);
						ast.setDefaultNodeFlag(ASTNode.ORIGINAL);
						ASTConverter converter = new ASTConverter(compilerOptions, true/*need to resolve bindings*/, this.monitor);
						BindingResolver resolver = new DefaultBindingResolver(unit.scope, owner, this.bindingTables, (flags & IJavaScriptUnit.ENABLE_BINDINGS_RECOVERY) != 0);
						ast.setBindingResolver(resolver);
						converter.setAST(ast);
						JavaScriptUnit compilationUnit = converter.convert(unit, contents);
						compilationUnit.setTypeRoot(source);
						compilationUnit.setLineEndTable(compilationResult.getLineSeparatorPositions());
						ast.setDefaultNodeFlag(0);
						ast.setOriginalModificationCount(ast.modificationCount());

						// pass it to requestor
						astRequestor.acceptAST(source, compilationUnit);

						worked(1);
					}

					// requested binding
					Object key = this.requestedKeys.get(fileName);
					if (key instanceof BindingKeyResolver) {
						reportBinding(key, astRequestor, owner, unit);
						worked(1);
					} else if (key instanceof ArrayList) {
						Iterator iterator = ((ArrayList) key).iterator();
						while (iterator.hasNext()) {
							reportBinding(iterator.next(), astRequestor, owner, unit);
							worked(1);
						}
					}

					// remove at the end so that we don't resolve twice if a source and a key for the same file name have been requested
					this.requestedSources.removeKey(fileName);
					this.requestedKeys.removeKey(fileName);
				} finally {
					// cleanup javaScript unit result
					unit.cleanUp();
				}
				this.unitsToProcess[i] = null; // release reference to processed unit declaration
				this.requestor.acceptResult(unit.compilationResult.tagAsAccepted());
			}

			// remaining binding keys
			DefaultBindingResolver resolver = new DefaultBindingResolver(this.lookupEnvironment, owner, this.bindingTables, (flags & IJavaScriptUnit.ENABLE_BINDINGS_RECOVERY) != 0);
			Object[] keys = this.requestedKeys.valueTable;
			for (int j = 0, keysLength = keys.length; j < keysLength; j++) {
				BindingKeyResolver keyResolver = (BindingKeyResolver) keys[j];
				if (keyResolver == null) continue;
				Binding compilerBinding = keyResolver.getCompilerBinding();
				IBinding binding = compilerBinding == null ? null : resolver.getBinding(compilerBinding);
				// pass it to requestor
				astRequestor.acceptBinding(((BindingKeyResolver) this.requestedKeys.valueTable[j]).getKey(), binding);
				worked(1);
			}
		} catch (OperationCanceledException e) {
			throw e;
		} catch (AbortCompilation e) {
			this.handleInternalException(e, unit);
		} catch (Error e) {
			this.handleInternalException(e, unit, null);
			throw e; // rethrow
		} catch (RuntimeException e) {
			this.handleInternalException(e, unit, null);
			throw e; // rethrow
		} finally {
			// disconnect ourselves from ast requestor
			astRequestor.compilationUnitResolver = null;
		}
	}

	private void reportBinding(Object key, ASTRequestor astRequestor, WorkingCopyOwner owner, CompilationUnitDeclaration unit) {
		BindingKeyResolver keyResolver = (BindingKeyResolver) key;
		Binding compilerBinding = keyResolver.getCompilerBinding();
		if (compilerBinding != null) {
			DefaultBindingResolver resolver = new DefaultBindingResolver(unit.scope, owner, this.bindingTables, false);
			IBinding binding = resolver.getBinding(compilerBinding);
			if (binding != null)
				astRequestor.acceptBinding(keyResolver.getKey(), binding);
		}
	}

	private CompilationUnitDeclaration resolve(
			CompilationUnitDeclaration unit,
			org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit sourceUnit,
			NodeSearcher nodeSearcher,
			boolean verifyMethods,
			boolean analyzeCode,
			boolean generateCode) {

		try {

			if (unit == null) {
				// build and record parsed units
				this.parseThreshold = 0; // will request a full parse
				beginToCompile(new org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit[] { sourceUnit });
				// process all units (some more could be injected in the loop by the lookup environment)
				unit = this.unitsToProcess[0];
			} else {
				// initial type binding creation
				this.lookupEnvironment.buildTypeBindings(unit, null /*no access restriction*/);

				// binding resolution
				this.lookupEnvironment.completeTypeBindings();
			}

			if (nodeSearcher == null) {
				this.parser.getMethodBodies(unit); // no-op if method bodies have already been parsed
			} else {
				int searchPosition = nodeSearcher.position;
				char[] source = sourceUnit.getContents();
				int length = source.length;
				if (searchPosition >= 0 && searchPosition <= length) {
					unit.traverse(nodeSearcher, unit.scope);

					org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode node = nodeSearcher.found;

					this.parser.scanner.setSource(source, unit.compilationResult);

		 			if (node != null) {
						org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration enclosingTypeDeclaration = nodeSearcher.enclosingType;
		  				if (node instanceof AbstractMethodDeclaration) {
							((AbstractMethodDeclaration)node).parseStatements(this.parser, unit);
		 				} else if (enclosingTypeDeclaration != null) {
							if (node instanceof org.eclipse.wst.jsdt.internal.compiler.ast.Initializer) {
			 					((org.eclipse.wst.jsdt.internal.compiler.ast.Initializer) node).parseStatements(this.parser, enclosingTypeDeclaration, unit);
		 					} else if (node instanceof org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration) {
								((org.eclipse.wst.jsdt.internal.compiler.ast.TypeDeclaration)node).parseMethod(this.parser, unit);
							}
		 				}
		 			}
				}
			}

			if (unit.scope != null) {
				// fault in fields & methods
				unit.scope.faultInTypes();

				// type checking
				unit.resolve();

				// flow analysis
				if (analyzeCode && this.options.enableSemanticValidation) unit.analyseCode();

			}
			if (this.unitsToProcess != null) this.unitsToProcess[0] = null; // release reference to processed unit declaration
			this.requestor.acceptResult(unit.compilationResult.tagAsAccepted());
			return unit;
		} catch (AbortCompilation e) {
			this.handleInternalException(e, unit);
			return unit == null ? this.unitsToProcess[0] : unit;
		} catch (Error e) {
			this.handleInternalException(e, unit, null);
			throw e; // rethrow
		} catch (RuntimeException e) {
			this.handleInternalException(e, unit, null);
			throw e; // rethrow
		} finally {
			// No reset is performed there anymore since,
			// within the CodeAssist (or related tools),
			// the validator may be called *after* a call
			// to this resolve(...) method. And such a call
			// needs to have a validator with a non-empty
			// environment.
			// this.reset();
		}
	}
	/*
	 * Internal API used to resolve a given javaScript unit. Can run a subset of the compilation process
	 */
	public CompilationUnitDeclaration resolve(
			org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit sourceUnit,
			boolean verifyMethods,
			boolean analyzeCode,
			boolean generateCode) {

		return resolve(
			null, /* no existing javaScript unit declaration*/
			sourceUnit,
			null/*no node searcher*/,
			verifyMethods,
			analyzeCode,
			generateCode);
	}

	/*
	 * Internal API used to resolve a given javaScript unit. Can run a subset of the compilation process
	 */
	public CompilationUnitDeclaration resolve(
			CompilationUnitDeclaration unit,
			org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit sourceUnit,
			boolean verifyMethods,
			boolean analyzeCode,
			boolean generateCode) {

		return resolve(
			unit,
			sourceUnit,
			null/*no node searcher*/,
			verifyMethods,
			analyzeCode,
			generateCode);
	}

	private void worked(int work) {
		if (this.monitor != null) {
			if (this.monitor.isCanceled())
				throw new OperationCanceledException();
			this.monitor.worked(work);
		}
	}
}
