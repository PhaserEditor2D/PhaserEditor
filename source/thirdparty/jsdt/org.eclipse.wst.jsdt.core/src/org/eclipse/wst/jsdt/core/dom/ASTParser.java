/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.dom;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.ITypeRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.core.compiler.CategorizedProblem;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.parser.RecoveryScanner;
import org.eclipse.wst.jsdt.internal.compiler.parser.RecoveryScannerData;
import org.eclipse.wst.jsdt.internal.compiler.parser.Scanner;
import org.eclipse.wst.jsdt.internal.core.BasicCompilationUnit;
import org.eclipse.wst.jsdt.internal.core.DefaultWorkingCopyOwner;
import org.eclipse.wst.jsdt.internal.core.PackageFragment;
import org.eclipse.wst.jsdt.internal.core.util.CodeSnippetParsingUtil;
import org.eclipse.wst.jsdt.internal.core.util.RecordedParsingInformation;
import org.eclipse.wst.jsdt.internal.core.util.Util;

/**
 * A JavaScript language parser for creating abstract syntax trees (ASTs).
 * <p>
 * Example: Create basic AST from source string
 * <pre>
 * char[] source = ...;
 * ASTParser parser = ASTParser.newParser(AST.JLS3);  
 * parser.setSource(source);
 * JavaScriptUnit result = (JavaScriptUnit) parser.createAST(null);
 * </pre>
 * Once a configured parser instance has been used to create an AST,
 * the settings are automatically reset to their defaults,
 * ready for the parser instance to be reused.
 * </p>
 * <p>
 * There are a number of configurable features:
 * <ul>
 * <li>Source string from {@link #setSource(char[]) char[]},
 * {@link #setSource(IJavaScriptUnit) IJavaScriptUnit},
 * or {@link #setSource(IClassFile) IClassFile}, and limited
 * to a specified {@linkplain #setSourceRange(int,int) subrange}.</li>
 * <li>Whether {@linkplain #setResolveBindings(boolean) bindings} will be created.</li>
 * <li>Which {@linkplain #setWorkingCopyOwner(WorkingCopyOwner)
 * working set owner} to use when resolving bindings).</li>
 * <li>A hypothetical {@linkplain #setUnitName(String) javaScript unit file name}
 * and {@linkplain #setProject(IJavaScriptProject) JavaScript project}
 * for locating a raw source string in the JavaScript model (when
 * resolving bindings)</li>
 * <li>Which {@linkplain #setCompilerOptions(Map) validator options}
 * to use.</li>
 * <li>Whether to parse just {@linkplain #setKind(int) an expression, statements,
 * or body declarations} rather than an entire javaScript unit.</li>
 * <li>Whether to return a {@linkplain #setFocalPosition(int) abridged AST}
 * focused on the declaration containing a given source position.</li>
 * </ul>
 * </p>
 *
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public class ASTParser {

	/**
	 * Kind constant used to request that the source be parsed
     * as a single expression.
	 */
	public static final int K_EXPRESSION = 0x01;

	/**
	 * Kind constant used to request that the source be parsed
     * as a sequence of statements.
	 */
	public static final int K_STATEMENTS = 0x02;

	/**
	 * Kind constant used to request that the source be parsed
	 * as a sequence of class body declarations.
	 */
	public static final int K_CLASS_BODY_DECLARATIONS = 0x04;

	/**
	 * Kind constant used to request that the source be parsed
	 * as a javaScript unit.
	 */
	public static final int K_COMPILATION_UNIT = 0x08;

	/**
	 * Creates a new object for creating a JavaScript abstract syntax tree
     * (AST) following the specified set of API rules.
     *
 	 * @param level the API level; one of the LEVEL constants
     * declared on <code>AST</code>
	 * @return new ASTParser instance
	 */
	public static ASTParser newParser(int level) {
		return new ASTParser(level);
	}

	/**
	 * Level of AST API desired.
	 */
	private final int apiLevel;

	/**
	 * Kind of parse requested. Defaults to an entire javaScript unit.
	 */
	private int astKind;

	/**
	 * Compiler options. Defaults to JavaScriptCore.getOptions().
	 */
	private Map compilerOptions;

	/**
	 * Request for bindings. Defaults to <code>false</code>.
     */
	private boolean resolveBindings;

	/**
	 * Request for a partial AST. Defaults to <code>false</code>.
     */
	private boolean partial = false;

	/**
	 * Request for a statements recovery. Defaults to <code>false</code>.
     */
	private boolean statementsRecovery;

	/**
     * Request for a bindings recovery. Defaults to <code>false</code>.
     */
    private boolean bindingsRecovery;


	/**
	 * The focal point for a partial AST request.
     * Only used when <code>partial</code> is <code>true</code>.
     */
	private int focalPointPosition;

    /**
     * Source string.
     */
    private char[] rawSource = null;

    /**
     * JavaScript  unit supplying the source.
     */
    private ITypeRoot typeRoot = null;

    /**
     * Character-based offset into the source string where parsing is to
     * begin. Defaults to 0.
     */
	private int sourceOffset = 0;

    /**
     * Character-based length limit, or -1 if unlimited.
     * All characters in the source string between <code>offset</code>
     * and <code>offset+length-1</code> inclusive are parsed. Defaults to -1,
     * which means the rest of the source string.
     */
	private int sourceLength = -1;

    /**
     * Working copy owner. Defaults to primary owner.
     */
	private WorkingCopyOwner workingCopyOwner = DefaultWorkingCopyOwner.PRIMARY;

    /**
	 * JavaScript project used to resolve names, or <code>null</code> if none.
     * Defaults to none.
     */
	private IJavaScriptProject project = null;

    /**
	 * Name of the javaScript unit for resolving bindings, or
	 * <code>null</code> if none. Defaults to none.
     */
	private String unitName = null;

 	/**
	 * Creates a new AST parser for the given API level.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param level the API level; one of the LEVEL constants
     * declared on <code>AST</code>
	 */
	ASTParser(int level) {
		if ((level != AST.JLS2_INTERNAL)
			&& (level != AST.JLS3)) {
			throw new IllegalArgumentException();
		}
		this.apiLevel = level;
	   	initializeDefaults();
	}

	/**
	 * Sets all the setting to their default values.
	 */
	private void initializeDefaults() {
		this.astKind = K_COMPILATION_UNIT;
		this.rawSource = null;
		this.typeRoot = null;
		this.resolveBindings = false;
		this.sourceLength = -1;
		this.sourceOffset = 0;
		this.workingCopyOwner = DefaultWorkingCopyOwner.PRIMARY;
		this.unitName = null;
		this.project = null;
		this.partial = false;
		Map options = JavaScriptCore.getOptions();
		options.remove(JavaScriptCore.COMPILER_TASK_TAGS); // no need to parse task tags
		this.compilerOptions = options;
	}

    /**
     * Requests that the validator should perform bindings recovery.
     * When bindings recovery is enabled the validator returns incomplete bindings.
     * <p>
     * Default to <code>false</code>.
     * </p>
     * <p>This should be set to true only if bindings are resolved. It has no effect if there is no binding
     * resolution.</p>
     *
     * @param enabled <code>true</code> if incomplete bindings are expected,
     *   and <code>false</code> if only complete bindings are expected.
     *
     * @see IBinding#isRecovered()
     */
    public void setBindingsRecovery(boolean enabled) {
        this.bindingsRecovery = enabled;
    }

	/**
	 * Sets the validator options to be used when parsing.
	 * <p>
	 * Note that {@link #setSource(IClassFile)},
	 * {@link #setSource(IJavaScriptUnit)},
	 * and {@link #setProject(IJavaScriptProject)} reset the validator options
	 * based on the JavaScript project. In other cases, validator options default
	 * to {@link JavaScriptCore#getOptions()}. In either case, and especially
	 * in the latter, the caller should carefully weight the consequences of
	 * allowing validator options to be defaulted as opposed to being
	 * explicitly specified for the <code>ASTParser</code> instance.
	 * For instance, there is a validator option called "Source Compatibility Mode"
	 * which determines which JDK level the source code is expected to meet.
	 * If you specify "1.4", then "assert" is treated as a keyword and disallowed
	 * as an identifier; if you specify "1.3", then "assert" is allowed as an
	 * identifier. So this particular setting has a major bearing on what is
	 * considered syntactically legal. By explicitly specifying the setting,
	 * the client control exactly how the parser works. On the other hand,
	 * allowing default settings means the parsing behaves like other JDT tools.
	 * </p>
	 *
	 * @param options the table of options (key type: <code>String</code>;
	 * value type: <code>String</code>), or <code>null</code>
	 * to set it back to the default
	 */
	public void setCompilerOptions(Map options) {
		if (options == null) {
			options = JavaScriptCore.getOptions();
		} else {
			// copy client's options so as to not do any side effect on them
			options = new HashMap(options);
		}
		options.remove(JavaScriptCore.COMPILER_TASK_TAGS); // no need to parse task tags
		this.compilerOptions = options;
	}

	/**
	 * Requests that the validator should provide binding information for
     * the AST nodes it creates.
     * <p>
     * Default to <code>false</code> (no bindings).
     * </p>
	 * <p>
	 * If <code>setResolveBindings(true)</code>, the various names
	 * and types appearing in the AST can be resolved to "bindings"
	 * by calling the <code>resolveBinding</code> methods. These bindings
	 * draw connections between the different parts of a program, and
	 * generally afford a more powerful vantage point for clients who wish to
	 * analyze a program's structure more deeply. These bindings come at a
	 * considerable cost in both time and space, however, and should not be
	 * requested frivolously. The additional space is not reclaimed until the
	 * AST, all its nodes, and all its bindings become garbage. So it is very
	 * important to not retain any of these objects longer than absolutely
	 * necessary. Bindings are resolved at the time the AST is created. Subsequent
	 * modifications to the AST do not affect the bindings returned by
	 * <code>resolveBinding</code> methods in any way; these methods return the
	 * same binding as before the AST was modified (including modifications
	 * that rearrange subtrees by reparenting nodes).
	 * If <code>setResolveBindings(false)</code> (the default), the analysis
	 * does not go beyond parsing and building the tree, and all
	 * <code>resolveBinding</code> methods return <code>null</code> from the
	 * outset.
	 * </p>
	 * <p>
	 * When bindings are requested, instead of considering javaScript units on disk only
	 * one can supply a <code>WorkingCopyOwner</code>. Working copies owned
	 * by this owner take precedence over the underlying javaScript units when looking
	 * up names and drawing the connections.
	 * </p>
	 * <p>
     * Binding information is obtained from the JavaScript model.
     * This means that the javaScript unit must be located relative to the
     * JavaScript model. This happens automatically when the source code comes from
     * either {@link #setSource(IJavaScriptUnit) setSource(IJavaScriptUnit)}
     * or {@link #setSource(IClassFile) setSource(IClassFile)}.
     * When source is supplied by {@link #setSource(char[]) setSource(char[])},
     * the location must be extablished explicitly by calling
     * {@link #setProject(IJavaScriptProject)} and  {@link #setUnitName(String)}.
	 * Note that the validator options that affect doc comment checking may also
	 * affect whether any bindings are resolved for nodes within doc comments.
	 * </p>
	 *
	 * @param bindings <code>true</code> if bindings are wanted,
	 *   and <code>false</code> if bindings are not of interest
	 */
	public void setResolveBindings(boolean bindings) {
	  this.resolveBindings = bindings;
	}

	/**
     * Requests an abridged abstract syntax tree.
     * By default, complete ASTs are returned.
     * <p>
     * When <code>true</code> the resulting AST does not have nodes for
     * the entire javaScript unit. Rather, the AST is only fleshed out
     * for the node that include the given source position. This kind of limited
     * AST is sufficient for certain purposes but totally unsuitable for others.
     * In places where it can be used, the limited AST offers the advantage of
     * being smaller and faster to construct.
	 * </p>
	 * <p>
	 * The AST will include nodes for all of the javaScript unit's functions, top-level vars,
	 * package, import, and top-level type declarations. It will also always contain
	 * nodes for all the body declarations for those top-level types, as well
	 * as body declarations for any member types. However, some of the body
	 * declarations may be abridged. In particular, the statements ordinarily
	 * found in the body of a method declaration node will not be included
	 * (the block will be empty) unless the source position falls somewhere
	 * within the source range of that method declaration node. The same is true
	 * for initializer declarations; the statements ordinarily found in the body
	 * of initializer node will not be included unless the source position falls
	 * somewhere within the source range of that initializer declaration node.
	 * Field declarations are never abridged. Note that the AST for the body of
	 * that one unabridged method (or initializer) is 100% complete; it has all
	 * its statements, including any local or anonymous type declarations
	 * embedded within them. When the the given position is not located within
	 * the source range of any body declaration of a top-level type, the AST
	 * returned will be a skeleton that includes nodes for all and only the major
	 * declarations; this kind of AST is still quite useful because it contains
	 * all the constructs that introduce names visible to the world outside the
	 * javaScript unit.
	 * </p>
	 *
	 * @param position a position into the corresponding body declaration
	 */
	public void setFocalPosition(int position) {
		this.partial = true;
		this.focalPointPosition = position;
	}

	/**
	 * Sets the kind of constructs to be parsed from the source.
     * Defaults to an entire javaScript unit.
	 * <p>
	 * When the parse is successful the result returned includes the ASTs for the
	 * requested source:
	 * <ul>
	 * <li>{@link #K_JAVASCRIPT_UNIT K_JAVASCRIPT_UNIT}: The result node
	 * is a {@link JavaScriptUnit}.</li>
	 * <li>{@link #K_CLASS_BODY_DECLARATIONS K_CLASS_BODY_DECLARATIONS}: The result node
	 * is a {@link TypeDeclaration} whose
	 * {@link TypeDeclaration#bodyDeclarations() bodyDeclarations}
	 * are the new trees. Other aspects of the type declaration are unspecified.</li>
	 * <li>{@link #K_STATEMENTS K_STATEMENTS}: The result node is a
	 * {@link Block Block} whose {@link Block#statements() statements}
	 * are the new trees. Other aspects of the block are unspecified.</li>
	 * <li>{@link #K_EXPRESSION K_EXPRESSION}: The result node is a subclass of
	 * {@link Expression Expression}. Other aspects of the expression are unspecified.</li>
	 * </ul>
	 * The resulting AST node is rooted under (possibly contrived)
	 * {@link JavaScriptUnit JavaScriptUnit} node, to allow the
	 * client to retrieve the following pieces of information
	 * available there:
	 * <ul>
	 * <li>{@linkplain JavaScriptUnit#getLineNumber(int) Line number map}. Line
	 * numbers start at 1 and only cover the subrange scanned
	 * (<code>source[offset]</code> through <code>source[offset+length-1]</code>).</li>
	 * <li>{@linkplain JavaScriptUnit#getMessages() Compiler messages}
	 * and {@linkplain JavaScriptUnit#getProblems() detailed problem reports}.
	 * Character positions are relative to the start of
	 * <code>source</code>; line positions are for the subrange scanned.</li>
	 * <li>{@linkplain JavaScriptUnit#getCommentList() Comment list}
	 * for the subrange scanned.</li>
	 * </ul>
	 * The contrived nodes do not have source positions. Other aspects of the
	 * {@link JavaScriptUnit JavaScriptUnit} node are unspecified, including
	 * the exact arrangment of intervening nodes.
	 * </p>
	 * <p>
	 * Lexical or syntax errors detected while parsing can result in
	 * a result node being marked as {@link ASTNode#MALFORMED MALFORMED}.
	 * In more severe failure cases where the parser is unable to
	 * recognize the input, this method returns
	 * a {@link JavaScriptUnit JavaScriptUnit} node with at least the
	 * validator messages.
	 * </p>
	 * <p>Each node in the subtree (other than the contrived nodes)
	 * carries source range(s) information relating back
	 * to positions in the given source (the given source itself
	 * is not remembered with the AST).
	 * The source range usually begins at the first character of the first token
	 * corresponding to the node; leading whitespace and comments are <b>not</b>
	 * included. The source range usually extends through the last character of
	 * the last token corresponding to the node; trailing whitespace and
	 * comments are <b>not</b> included. There are a handful of exceptions
	 * (including the various body declarations); the
	 * specification for these node type spells out the details.
	 * Source ranges nest properly: the source range for a child is always
	 * within the source range of its parent, and the source ranges of sibling
	 * nodes never overlap.
	 * </p>
	 * <p>
	 * Binding information is only computed when <code>kind</code> is
     * <code>K_JAVASCRIPT_UNIT</code>.
	 * </p>
	 *
	 * @param kind the kind of construct to parse: one of
	 * {@link #K_JAVASCRIPT_UNIT},
	 * {@link #K_CLASS_BODY_DECLARATIONS},
	 * {@link #K_EXPRESSION},
	 * {@link #K_STATEMENTS}
	 */
	public void setKind(int kind) {
	    if ((kind != K_COMPILATION_UNIT)
		    && (kind != K_CLASS_BODY_DECLARATIONS)
		    && (kind != K_EXPRESSION)
		    && (kind != K_STATEMENTS)) {
	    	throw new IllegalArgumentException();
	    }
		this.astKind = kind;
	}

	/**
     * Sets the source code to be parsed.
     *
	 * @param source the source string to be parsed,
     * or <code>null</code> if none
     */
	public void setSource(char[] source) {
		this.rawSource = source;
		// clear the type root
		this.typeRoot = null;
	}

	/**
     * Sets the source code to be parsed.
     * This method automatically sets the project (and compiler
     * options) based on the given javaScript unit, in a manner
     * equivalent to <code>setProject(source.getJavaProject())</code>
     *
	 * @param source the JavaScript model javaScript unit whose source code
     * is to be parsed, or <code>null</code> if none
      */
	public void setSource(IJavaScriptUnit source) {
		setSource((ITypeRoot)source);
	}

	/**
     * Sets the source code to be parsed.
     * <p>This method automatically sets the project (and compiler
     * options) based on the given javaScript unit, in a manner
     * equivalent to <code>setProject(source.getJavaProject())</code>.</p>
     *
	 * @param source the JavaScript file whose corresponding source code
     * is to be parsed, or <code>null</code> if none
     */
	public void setSource(IClassFile source) {
		setSource((ITypeRoot)source);
	}

	/**
	 * Sets the source code to be parsed.
	 * <p>This method automatically sets the project (and compiler
	 * options) based on the given javaScript unit, in a manner
	 * equivalent to <code>setProject(source.getJavaProject())</code>.</p>
	 *
	 * @param source the JavaScript model javaScript unit whose corresponding source code
	 * is to be parsed, or <code>null</code> if none
	 */
	public void setSource(ITypeRoot source) {
		this.typeRoot = source;
		// clear the raw source
		this.rawSource = null;
		if (source != null) {
			this.project = source.getJavaScriptProject();
			Map options = this.project.getOptions(true);
			options.remove(JavaScriptCore.COMPILER_TASK_TAGS); // no need to parse task tags
			this.compilerOptions = options;
		}
	}

	/**
     * Sets the subrange of the source code to be parsed.
     * By default, the entire source string will be parsed
     * (<code>offset</code> 0 and <code>length</code> -1).
     *
     * @param offset the index of the first character to parse
     * @param length the number of characters to parse, or -1 if
     * the remainder of the source string is
     */
	public void setSourceRange(int offset, int length) {
		if (offset < 0 || length < -1) {
			throw new IllegalArgumentException();
		}
		this.sourceOffset = offset;
		this.sourceLength = length;
	}

	/**
	 * Requests that the validator should perform statements recovery.
	 * When statements recovery is enabled the validator tries to create statement nodes
	 * from code containing syntax errors
     * <p>
     * Default to <code>false</code>.
     * </p>
	 *
	 * @param enabled <code>true</code> if statements containing syntax errors are wanted,
	 *   and <code>false</code> if these statements aren't wanted.
	 *
	 */
	public void setStatementsRecovery(boolean enabled) {
		this.statementsRecovery = enabled;
	}

    /**
     * Sets the working copy owner using when resolving bindings, where
     * <code>null</code> means the primary owner. Defaults to the primary owner.
     *
	 * @param owner the owner of working copies that take precedence over underlying
	 *   javaScript units, or <code>null</code> if the primary owner should be used
     */
	public void setWorkingCopyOwner(WorkingCopyOwner owner) {
	    if (owner == null) {
			this.workingCopyOwner = DefaultWorkingCopyOwner.PRIMARY;
		} else {
			this.workingCopyOwner = owner;
	 	}
	}

	/**
     * Sets the name of the javaScript unit that would hypothetically contains
     * the source string. This is used in conjunction with {@link #setSource(char[])}
     * and {@link #setProject(IJavaScriptProject) } to locate the javaScript unit relative to a JavaScript project.
     * Defaults to none (<code>null</code>).
	 * <p>
	 * The name of the javaScript unit must be supplied for resolving bindings.
	 * This name should be suffixed by a dot ('.') followed by one of the
	 * {@link JavaScriptCore#getJavaScriptLikeExtensions() JavaScript-like extensions}.
	 *
	 * <p>This name must represent the full path of the unit inside the given project. For example, if the source
	 * declares a public class named "Foo" in a project "P", the name of the javaScript unit must be
	 * "/P/Foo.js". If the source declares a public class name "Bar" in a package "p1.p2" in a project "P",
	 * the name of the javaScript unit must be "/P/p1/p2/Bar.js".</p>
     *
	 * @param unitName the name of the javaScript unit that would contain the source
	 *    string, or <code>null</code> if none
     */
	public void setUnitName(String unitName) {
		this.unitName = unitName;
	}

	/**
	 * Sets the JavaScript project used when resolving bindings.
	 * This method automatically sets the compiler
	 * options based on the given project:
	 * <pre>
	 * setCompilerOptions(project.getOptions(true));
	 * </pre>
	 * See {@link #setCompilerOptions(Map)} for a discussion of
	 * the pros and cons of using these options vs specifying
	 * validator options explicitly.
	 * This setting is used in conjunction with <code>setSource(char[])</code>.
	 * For the purposes of resolving bindings, types declared in the
	 * source string will hide types by the same name available
	 * through the includepath of the given project.
	 * Defaults to none (<code>null</code>).
	 *
	 * @param project the JavaScript project used to resolve names, or
	 *    <code>null</code> if none
	 */
	public void setProject(IJavaScriptProject project) {
		this.project = project;
		if (project != null) {
			Map options = project.getOptions(true);
			options.remove(JavaScriptCore.COMPILER_TASK_TAGS); // no need to parse task tags
			this.compilerOptions = options;
		}
	}

	/**
     * Creates an abstract syntax tree.
     * <p>
     * A successful call to this method returns all settings to their
     * default values so the object is ready to be reused.
     * </p>
     *
	 * @param monitor the progress monitor used to report progress and request cancelation,
	 *   or <code>null</code> if none
	 * @return an AST node whose type depends on the kind of parse
	 *  requested, with a fallback to a <code>JavaScriptUnit</code>
	 *  in the case of severe parsing errors
	 * @exception IllegalStateException if the settings provided
	 * are insufficient, contradictory, or otherwise unsupported
     */
	public ASTNode createAST(IProgressMonitor monitor) {
	   ASTNode result = null;
	   if (monitor != null) monitor.beginTask("", 1); //$NON-NLS-1$
		try {
			if (this.rawSource == null && this.typeRoot == null) {
		   	  throw new IllegalStateException("source not specified"); //$NON-NLS-1$
		   }
	   		result = internalCreateAST(monitor);
		} finally {
	   	   // re-init defaults to allow reuse (and avoid leaking)
	   	   initializeDefaults();
	   	   if (monitor != null) monitor.done();
		}
   	   return result;
	}

	/**
     * Creates ASTs for a batch of javaScript units.
     * When bindings are being resolved, processing a
     * batch of javaScript units is more efficient because much
     * of the work involved in resolving bindings can be shared.
     * <p>
     * When bindings are being resolved, all javaScript units must
     * come from the same JavaScript project, which must be set beforehand
     * with <code>setProject</code>.
     * The javaScript units are processed one at a time in no
     * specified order. For each of the javaScript units in turn,
	 * <ul>
	 * <li><code>ASTParser.createAST</code> is called to parse it
	 * and create a corresponding AST. The calls to
	 * <code>ASTParser.createAST</code> all employ the same settings.</li>
	 * <li><code>ASTRequestor.acceptAST</code> is called passing
	 * the javaScript unit and the corresponding AST to
	 * <code>requestor</code>.
	 * </li>
	 * </ul>
     * Note only ASTs from the given javaScript units are reported
     * to the requestor. If additional javaScript units are required to
     * resolve the original ones, the corresponding ASTs are <b>not</b>
     * reported to the requestor.
     * </p>
	 * <p>
	 * Note also the following parser parameters are used, regardless of what
	 * may have been specified:
	 * <ul>
	 * <li>The {@linkplain #setKind(int) parser kind} is <code>K_JAVASCRIPT_UNIT</code></li>
	 * <li>The {@linkplain #setSourceRange(int,int) source range} is <code>(0, -1)</code></li>
	 * <li>The {@linkplain #setFocalPosition(int) focal position} is not set</li>
	 * </ul>
	 * </p>
     * <p>
     * The <code>bindingKeys</code> parameter specifies bindings keys
     * ({@link IBinding#getKey()}) that are to be looked up. These keys may
     * be for elements either inside or outside the set of compilation
     * units being processed. When bindings are being resolved,
     * the keys and corresponding bindings (or <code>null</code> if none) are
     * passed to <code>ASTRequestor.acceptBinding</code>. Note that binding keys
     * for elements outside the set of javaScript units being processed are looked up
     * after all <code>ASTRequestor.acceptAST</code> callbacks have been made.
     * Binding keys for elements inside the set of javaScript units being processed
     * are looked up and reported right after the corresponding
     * <code>ASTRequestor.acceptAST</code> callback has been made.
     * No <code>ASTRequestor.acceptBinding</code> callbacks are made unless
     * bindings are being resolved.
     * </p>
     * <p>
     * A successful call to this method returns all settings to their
     * default values so the object is ready to be reused.
     * </p>
     *
     * @param compilationUnits the javaScript units to create ASTs for
     * @param bindingKeys the binding keys to create bindings for
     * @param requestor the AST requestor that collects abtract syntax trees and bindings
	 * @param monitor the progress monitor used to report progress and request cancelation,
	 *   or <code>null</code> if none
	 * @exception IllegalStateException if the settings provided
	 * are insufficient, contradictory, or otherwise unsupported
     */
	public void createASTs(IJavaScriptUnit[] compilationUnits, String[] bindingKeys, ASTRequestor requestor, IProgressMonitor monitor) {
		try {
			int flags = 0;
			if (this.statementsRecovery) flags |= IJavaScriptUnit.ENABLE_STATEMENTS_RECOVERY;
			if (this.resolveBindings) {
				if (this.project == null)
					throw new IllegalStateException("project not specified"); //$NON-NLS-1$
				if (this.bindingsRecovery) flags |= IJavaScriptUnit.ENABLE_BINDINGS_RECOVERY;
				JavaScriptUnitResolver.resolve(compilationUnits, bindingKeys, requestor, this.apiLevel, this.compilerOptions, this.project, this.workingCopyOwner, flags, monitor);
			} else {
				JavaScriptUnitResolver.parse(compilationUnits, requestor, this.apiLevel, this.compilerOptions, flags, monitor);
			}
		} finally {
	   	   // re-init defaults to allow reuse (and avoid leaking)
	   	   initializeDefaults();
		}
	}

	/**
     * Creates bindings for a batch of JavaScript elements. These elements are either
     * enclosed in {@link IJavaScriptUnit}s or in {@link IClassFile}s.
     * <p>
     * All enclosing javaScript units must
     * come from the same JavaScript project, which must be set beforehand
     * with <code>setProject</code>.
     * </p>
     * <p>
     * All elements must exist. If one doesn't exist, an <code>IllegalStateException</code>
     * is thrown.
     * </p>
     * <p>
     * The returned array has the same size as the given elements array. At a given position
     * it contains the binding of the corresponding JavaScript element, or <code>null</code>
     * if no binding could be created.
     * </p>
	 * <p>
	 * Note also the following parser parameters are used, regardless of what
	 * may have been specified:
	 * <ul>
	 * <li>The {@linkplain #setResolveBindings(boolean) binding resolution flag} is <code>true</code></li>
	 * <li>The {@linkplain #setKind(int) parser kind} is <code>K_JAVASCRIPT_UNIT</code></li>
	 * <li>The {@linkplain #setSourceRange(int,int) source range} is <code>(0, -1)</code></li>
	 * <li>The {@linkplain #setFocalPosition(int) focal position} is not set</li>
	 * </ul>
	 * </p>
     * <p>
     * A successful call to this method returns all settings to their
     * default values so the object is ready to be reused.
     * </p>
     *
     * @param elements the JavaScript elements to create bindings for
     * @return the bindings for the given JavaScript elements, possibly containing <code>null</code>s
     *              if some bindings could not be created
	 * @exception IllegalStateException if the settings provided
	 * are insufficient, contradictory, or otherwise unsupported
     */
	public IBinding[] createBindings(IJavaScriptElement[] elements, IProgressMonitor monitor) {
		try {
			if (this.project == null)
				throw new IllegalStateException("project not specified"); //$NON-NLS-1$
			int flags = 0;
			if (this.statementsRecovery) flags |= IJavaScriptUnit.ENABLE_STATEMENTS_RECOVERY;
			if (this.bindingsRecovery)  flags |= IJavaScriptUnit.ENABLE_BINDINGS_RECOVERY;
			return JavaScriptUnitResolver.resolve(elements, this.apiLevel, this.compilerOptions, this.project, this.workingCopyOwner,flags, monitor);
		} finally {
	   	   // re-init defaults to allow reuse (and avoid leaking)
	   	   initializeDefaults();
		}
	}

	private ASTNode internalCreateAST(IProgressMonitor monitor) {
		boolean needToResolveBindings = this.resolveBindings;
		switch(this.astKind) {
			case K_CLASS_BODY_DECLARATIONS :
			case K_EXPRESSION :
			case K_STATEMENTS :
				if (this.rawSource != null) {
					if (this.sourceOffset + this.sourceLength > this.rawSource.length) {
					    throw new IllegalStateException();
					}
					return internalCreateASTForKind();
				}
				break;
			case K_COMPILATION_UNIT :
				CompilationUnitDeclaration compilationUnitDeclaration = null;
				try {
					NodeSearcher searcher = null;
					org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit sourceUnit = null;
					WorkingCopyOwner wcOwner = this.workingCopyOwner;
					if (this.typeRoot instanceof IJavaScriptUnit) {
							/*
							 * this.compilationUnitSource is an instance of org.eclipse.wst.jsdt.internal.core.CompilationUnit that implements
							 * both org.eclipse.wst.jsdt.core.IJavaScriptUnit and org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit
							 */
							sourceUnit = (org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit) this.typeRoot;
							/*
							 * use a BasicCompilation that caches the source instead of using the compilationUnitSource directly
							 * (if it is a working copy, the source can change between the parse and the AST convertion)
							 * (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=75632)
							 */
							sourceUnit = new BasicCompilationUnit(sourceUnit.getContents(), sourceUnit.getPackageName(), new String(sourceUnit.getFileName()), this.project);
							wcOwner = ((IJavaScriptUnit) this.typeRoot).getOwner();
					} else if (this.typeRoot instanceof IClassFile) {
						try {
							String sourceString = this.typeRoot.getSource();
							if (sourceString == null) {
								throw new IllegalStateException();
							}
							PackageFragment packageFragment = (PackageFragment) this.typeRoot.getParent();
//							BinaryType type = (BinaryType) this.typeRoot.findPrimaryType();
							char[] fileName =this.typeRoot.getElementName().toCharArray();
//							IBinaryType binaryType = (IBinaryType) type.getElementInfo();
//							// file name is used to recreate the JavaScript element, so it has to be the toplevel .class file name
//							char[] fileName = type.getElementName().toCharArray();
//							int firstDollar = CharOperation.indexOf('$', fileName);
//							if (firstDollar != -1) {
//								char[] suffix = SuffixConstants.SUFFIX_class;
//								int suffixLength = suffix.length;
//								char[] newFileName = new char[firstDollar + suffixLength];
//								System.arraycopy(fileName, 0, newFileName, 0, firstDollar);
//								System.arraycopy(suffix, 0, newFileName, firstDollar, suffixLength);
//								fileName = newFileName;
//							}
							sourceUnit = new BasicCompilationUnit(sourceString.toCharArray(), Util.toCharArrays(packageFragment.names), new String(fileName), this.project);
						} catch(JavaScriptModelException e) {
							// an error occured accessing the javaScript element
							throw new IllegalStateException();
						}
					} else if (this.rawSource != null) {
						needToResolveBindings = this.resolveBindings && this.unitName != null && this.project != null && this.compilerOptions != null;
						sourceUnit = new BasicCompilationUnit(this.rawSource, null, this.unitName == null ? "" : this.unitName, this.project); //$NON-NLS-1$
					} else {
						throw new IllegalStateException();
					}
					if (this.partial) {
						searcher = new NodeSearcher(this.focalPointPosition);
					}
					int flags = 0;
					if (this.statementsRecovery) flags |= IJavaScriptUnit.ENABLE_STATEMENTS_RECOVERY;
					if (needToResolveBindings) {
						if (this.bindingsRecovery) flags |= IJavaScriptUnit.ENABLE_BINDINGS_RECOVERY;
						try {
							// parse and resolve
							compilationUnitDeclaration =
								JavaScriptUnitResolver.resolve(
									sourceUnit,
									this.project,
									searcher,
									this.compilerOptions,
									this.workingCopyOwner,
									flags,
									monitor);
						} catch (JavaScriptModelException e) {
							flags &= ~IJavaScriptUnit.ENABLE_BINDINGS_RECOVERY;
							compilationUnitDeclaration = JavaScriptUnitResolver.parse(
									sourceUnit,
									searcher,
									this.compilerOptions,
									flags);
							needToResolveBindings = false;
						}
					} else {
						compilationUnitDeclaration = JavaScriptUnitResolver.parse(
								sourceUnit,
								searcher,
								this.compilerOptions,
								flags);
						needToResolveBindings = false;
					}
					JavaScriptUnit result = JavaScriptUnitResolver.convert(
						compilationUnitDeclaration,
						sourceUnit.getContents(),
						this.apiLevel,
						this.compilerOptions,
						needToResolveBindings,
						wcOwner,
						needToResolveBindings ? new DefaultBindingResolver.BindingTables() : null,
						flags,
						monitor);
					result.setTypeRoot(this.typeRoot);
					return result;
				} finally {
					if (compilationUnitDeclaration != null && this.resolveBindings) {
						compilationUnitDeclaration.cleanUp();
						if (compilationUnitDeclaration.scope!=null)
							compilationUnitDeclaration.scope.cleanup();
					}
				}
		}
		throw new IllegalStateException();
	}

	/**
	 * Parses the given source between the bounds specified by the given offset (inclusive)
	 * and the given length and creates and returns a corresponding abstract syntax tree.
	 * <p>
	 * When the parse is successful the result returned includes the ASTs for the
	 * requested source:
	 * <ul>
	 * <li>{@link #K_CLASS_BODY_DECLARATIONS K_CLASS_BODY_DECLARATIONS}: The result node
	 * is a {@link TypeDeclaration TypeDeclaration} whose
	 * {@link TypeDeclaration#bodyDeclarations() bodyDeclarations}
	 * are the new trees. Other aspects of the type declaration are unspecified.</li>
	 * <li>{@link #K_STATEMENTS K_STATEMENTS}: The result node is a
	 * {@link Block Block} whose {@link Block#statements() statements}
	 * are the new trees. Other aspects of the block are unspecified.</li>
	 * <li>{@link #K_EXPRESSION K_EXPRESSION}: The result node is a subclass of
	 * {@link Expression Expression}. Other aspects of the expression are unspecified.</li>
	 * </ul>
	 * The resulting AST node is rooted under an contrived
	 * {@link JavaScriptUnit JavaScriptUnit} node, to allow the
	 * client to retrieve the following pieces of information
	 * available there:
	 * <ul>
	 * <li>{@linkplain JavaScriptUnit#getLineNumber(int) Line number map}. Line
	 * numbers start at 1 and only cover the subrange scanned
	 * (<code>source[offset]</code> through <code>source[offset+length-1]</code>).</li>
	 * <li>{@linkplain JavaScriptUnit#getMessages() Compiler messages}
	 * and {@linkplain JavaScriptUnit#getProblems() detailed problem reports}.
	 * Character positions are relative to the start of
	 * <code>source</code>; line positions are for the subrange scanned.</li>
	 * <li>{@linkplain JavaScriptUnit#getCommentList() Comment list}
	 * for the subrange scanned.</li>
	 * </ul>
	 * The contrived nodes do not have source positions. Other aspects of the
	 * {@link JavaScriptUnit JavaScriptUnit} node are unspecified, including
	 * the exact arrangment of intervening nodes.
	 * </p>
	 * <p>
	 * Lexical or syntax errors detected while parsing can result in
	 * a result node being marked as {@link ASTNode#MALFORMED MALFORMED}.
	 * In more severe failure cases where the parser is unable to
	 * recognize the input, this method returns
	 * a {@link JavaScriptUnit JavaScriptUnit} node with at least the
	 * validator messages.
	 * </p>
	 * <p>Each node in the subtree (other than the contrived nodes)
	 * carries source range(s) information relating back
	 * to positions in the given source (the given source itself
	 * is not remembered with the AST).
	 * The source range usually begins at the first character of the first token
	 * corresponding to the node; leading whitespace and comments are <b>not</b>
	 * included. The source range usually extends through the last character of
	 * the last token corresponding to the node; trailing whitespace and
	 * comments are <b>not</b> included. There are a handful of exceptions
	 * (including the various body declarations); the
	 * specification for these node type spells out the details.
	 * Source ranges nest properly: the source range for a child is always
	 * within the source range of its parent, and the source ranges of sibling
	 * nodes never overlap.
	 * </p>
	 * <p>
	 * This method does not compute binding information; all <code>resolveBinding</code>
	 * methods applied to nodes of the resulting AST return <code>null</code>.
	 * </p>
	 *
	 * @return an AST node whose type depends on the kind of parse
	 *  requested, with a fallback to a <code>JavaScriptUnit</code>
	 *  in the case of severe parsing errors
	 * @see ASTNode#getStartPosition()
	 * @see ASTNode#getLength()
	 */
	private ASTNode internalCreateASTForKind() {
		final ASTConverter converter = new ASTConverter(this.compilerOptions, false, null);
		converter.compilationUnitSource = this.rawSource;
		converter.compilationUnitSourceLength = this.rawSource.length;
		converter.scanner.setSource(this.rawSource);

		AST ast = AST.newAST(this.apiLevel);
		ast.setDefaultNodeFlag(ASTNode.ORIGINAL);
		ast.setBindingResolver(new BindingResolver());
		if (this.statementsRecovery) {
			ast.setFlag(IJavaScriptUnit.ENABLE_STATEMENTS_RECOVERY);
		}
		converter.setAST(ast);
		CodeSnippetParsingUtil codeSnippetParsingUtil = new CodeSnippetParsingUtil();
		JavaScriptUnit compilationUnit = ast.newJavaScriptUnit();
		if (this.sourceLength == -1) {
			this.sourceLength = this.rawSource.length;
		}
		switch(this.astKind) {
			case K_STATEMENTS :
				ConstructorDeclaration constructorDeclaration = codeSnippetParsingUtil.parseStatements(this.rawSource, this.sourceOffset, this.sourceLength, this.compilerOptions, true, this.statementsRecovery);
				RecoveryScannerData data = constructorDeclaration.compilationResult.recoveryScannerData;
				if(data != null) {
					Scanner scanner = converter.scanner;
					converter.scanner = new RecoveryScanner(scanner, data.removeUnused());
					converter.docParser.scanner = converter.scanner;
					converter.scanner.setSource(scanner.source);
				}
				RecordedParsingInformation recordedParsingInformation = codeSnippetParsingUtil.recordedParsingInformation;
				int[][] comments = recordedParsingInformation.commentPositions;
				if (comments != null) {
					converter.buildCommentsTable(compilationUnit, comments);
				}
				compilationUnit.setLineEndTable(recordedParsingInformation.lineEnds);
					Block block = ast.newBlock();
					block.setSourceRange(this.sourceOffset, this.sourceOffset + this.sourceLength);
					org.eclipse.wst.jsdt.internal.compiler.ast.Statement[] statements = constructorDeclaration.statements;
					if (statements != null) {
						int statementsLength = statements.length;
						for (int i = 0; i < statementsLength; i++) {
							if (statements[i] instanceof org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration) {
								converter.checkAndAddMultipleLocalDeclaration(statements, i, block.statements());
							} else {
								Statement statement = converter.convert(statements[i]);
								if (statement != null) {
									block.statements().add(statement);
								}
							}
						}
					}
					rootNodeToCompilationUnit(ast, compilationUnit, block, recordedParsingInformation, data);
					ast.setDefaultNodeFlag(0);
					ast.setOriginalModificationCount(ast.modificationCount());
					return block;
			case K_EXPRESSION :
				org.eclipse.wst.jsdt.internal.compiler.ast.Expression expression = codeSnippetParsingUtil.parseExpression(this.rawSource, this.sourceOffset, this.sourceLength, this.compilerOptions, true);
				recordedParsingInformation = codeSnippetParsingUtil.recordedParsingInformation;
				comments = recordedParsingInformation.commentPositions;
				if (comments != null) {
					converter.buildCommentsTable(compilationUnit, comments);
				}
				compilationUnit.setLineEndTable(recordedParsingInformation.lineEnds);
				if (expression != null) {
					Expression expression2 = converter.convert(expression);
					rootNodeToCompilationUnit(expression2.getAST(), compilationUnit, expression2, codeSnippetParsingUtil.recordedParsingInformation, null);
					ast.setDefaultNodeFlag(0);
					ast.setOriginalModificationCount(ast.modificationCount());
					return expression2;
				} else {
					CategorizedProblem[] problems = recordedParsingInformation.problems;
					if (problems != null) {
						compilationUnit.setProblems(problems);
					}
					ast.setDefaultNodeFlag(0);
					ast.setOriginalModificationCount(ast.modificationCount());
					return compilationUnit;
				}
			case K_CLASS_BODY_DECLARATIONS :
				final org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode[] nodes = codeSnippetParsingUtil.parseClassBodyDeclarations(this.rawSource, this.sourceOffset, this.sourceLength, this.compilerOptions, true);
				recordedParsingInformation = codeSnippetParsingUtil.recordedParsingInformation;
				comments = recordedParsingInformation.commentPositions;
				if (comments != null) {
					converter.buildCommentsTable(compilationUnit, comments);
				}
				compilationUnit.setLineEndTable(recordedParsingInformation.lineEnds);
				if (nodes != null) {
//					TypeDeclaration typeDeclaration = converter.convert(nodes);
//					typeDeclaration.setSourceRange(this.sourceOffset, this.sourceOffset + this.sourceLength);
//					rootNodeToCompilationUnit(typeDeclaration.getAST(), compilationUnit, typeDeclaration, codeSnippetParsingUtil.recordedParsingInformation, null);
					JavaScriptUnit compUnit=converter.convert(nodes, compilationUnit);
					rootNodeToCompilationUnit(compUnit.getAST(), compilationUnit, compUnit, codeSnippetParsingUtil.recordedParsingInformation, null);
					ast.setDefaultNodeFlag(0);
					ast.setOriginalModificationCount(ast.modificationCount());
					return compilationUnit;
				} else {
					CategorizedProblem[] problems = recordedParsingInformation.problems;
					if (problems != null) {
						compilationUnit.setProblems(problems);
					}
					ast.setDefaultNodeFlag(0);
					ast.setOriginalModificationCount(ast.modificationCount());
					return compilationUnit;
				}
		}
		throw new IllegalStateException();
	}

	private void propagateErrors(ASTNode astNode, CategorizedProblem[] problems, RecoveryScannerData data) {
		astNode.accept(new ASTSyntaxErrorPropagator(problems));
		if (data != null) {
			astNode.accept(new ASTRecoveryPropagator(problems, data));
		}
	}

	private void rootNodeToCompilationUnit(AST ast, JavaScriptUnit compilationUnit, ASTNode node, RecordedParsingInformation recordedParsingInformation, RecoveryScannerData data) {
		final int problemsCount = recordedParsingInformation.problemsCount;
		switch(node.getNodeType()) {
			case ASTNode.BLOCK :
				{
					Block block = (Block) node;
					if (problemsCount != 0) {
						// propagate and record problems
						final CategorizedProblem[] problems = recordedParsingInformation.problems;
						propagateErrors(block, problems, data);
						compilationUnit.setProblems(problems);
					}
					TypeDeclaration typeDeclaration = ast.newTypeDeclaration();
					Initializer initializer = ast.newInitializer();
					initializer.setBody(block);
					typeDeclaration.bodyDeclarations().add(initializer);
					compilationUnit.types().add(typeDeclaration);
				}
				break;
			case ASTNode.JAVASCRIPT_UNIT :
			{
				JavaScriptUnit compUnit = (JavaScriptUnit) node;
				if (problemsCount != 0) {
					// propagate and record problems
					final CategorizedProblem[] problems = recordedParsingInformation.problems;
					for (int i = 0, max = compUnit.statements().size(); i < max; i++) {
						propagateErrors((ASTNode) compUnit.statements().get(i), problems, data);
					}
					compilationUnit.setProblems(problems);
				}
				if (compilationUnit!=node)
					for (int i = 0, max = compUnit.statements().size(); i < max; i++)
						compilationUnit.statements().add(compUnit.statements().get(i));
			}
			break;
			case ASTNode.TYPE_DECLARATION :
				{
					TypeDeclaration typeDeclaration = (TypeDeclaration) node;
					if (problemsCount != 0) {
						// propagate and record problems
						final CategorizedProblem[] problems = recordedParsingInformation.problems;
						propagateErrors(typeDeclaration, problems, data);
						compilationUnit.setProblems(problems);
					}
					compilationUnit.types().add(typeDeclaration);
				}
				break;
			default :
				if (node instanceof Expression) {
					Expression expression = (Expression) node;
					if (problemsCount != 0) {
						// propagate and record problems
						final CategorizedProblem[] problems = recordedParsingInformation.problems;
						propagateErrors(expression, problems, data);
						compilationUnit.setProblems(problems);
					}
					ExpressionStatement expressionStatement = ast.newExpressionStatement(expression);
					Block block = ast.newBlock();
					block.statements().add(expressionStatement);
					Initializer initializer = ast.newInitializer();
					initializer.setBody(block);
					TypeDeclaration typeDeclaration = ast.newTypeDeclaration();
					typeDeclaration.bodyDeclarations().add(initializer);
					compilationUnit.types().add(typeDeclaration);
				}
		}
	}
}
