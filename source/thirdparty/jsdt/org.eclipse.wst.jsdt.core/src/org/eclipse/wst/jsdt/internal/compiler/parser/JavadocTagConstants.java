/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.jsdt.internal.compiler.parser;

/**
 * Javadoc tag constants.
 *
 * @since 3.2
 */
public interface JavadocTagConstants {

	// recognized JSdoc tags
	public static final char[] TAG_AUGMENTS = "augments".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_AUTHOR = "author".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_ARGUMENT = "argument".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_BORROWS = "borrows".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_CLASS = "class".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_CONSTANT = "constant".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_CONSTRUCTOR = "constructor".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_CONSTRUCTS = "constructs".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_DEFAULT = "default".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_DEPRECATED = "deprecated".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_DESCRIPTION = "description".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_EVENT = "event".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_EXAMPLE = "example".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_EXTENDS = "extends".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_FIELD = "field".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_FILEOVERVIEW = "fileOverview".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_FUNCTION = "function".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_IGNORE = "ignore".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_INNER = "inner".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_LENDS = "lends".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_LINK = "link".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_MEMBEROF = "memberOf".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_NAME = "name".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_NAMESPACE = "namespace".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_PARAM = "param".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_PRIVATE = "private".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_PROPERTY = "property".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_PUBLIC = "public".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_REQUIRES = "requires".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_RETURN = "return".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_RETURNS = "returns".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_SEE = "see".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_SINCE = "since".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_STATIC = "static".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_THROWS = "throws".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_TYPE = "type".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_VERSION = "version".toCharArray(); //$NON-NLS-1$
	
	// legacy JSdoc tags
	public static final char[] TAG_EXCEPTION = "exception".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_FINAL = "final".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_MEMBER = "member".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_BASE = "base".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_ADDON = "addon".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_EXEC = "exec".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_PROTECTED = "protected".toCharArray(); //$NON-NLS-1$
	
	// from scriptdoc.org
	public static final char[] TAG_ALIAS = "alias".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_CLASSDECRIPTION = "classDescription".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_ID = "id".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_PROJECT_DESCRIPTION = "projectDescription".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_SDOC = "sdoc".toCharArray(); //$NON-NLS-1$
	public static final char[] TAG_METHOD = "method".toCharArray(); //$NON-NLS-1$

	// tags lengths
	public static final int TAG_AUGMENTS_LENGTH = TAG_AUGMENTS.length;
	public static final int TAG_AUTHOR_LENGTH = TAG_AUTHOR.length;
	public static final int TAG_ARGUMENT_LENGTH = TAG_ARGUMENT.length;
	public static final int TAG_BORROWS_LENGTH = TAG_BORROWS.length;
	public static final int TAG_CLASS_LENGTH = TAG_CLASS.length;
	public static final int TAG_CONSTANT_LENGTH = TAG_CONSTANT.length;
	public static final int TAG_CONSTRUCTOR_LENGTH = TAG_CONSTRUCTOR.length;
	public static final int TAG_CONSTRUCTS_LENGTH = TAG_CONSTRUCTS.length;
	public static final int TAG_DEFAULT_LENGTH = TAG_DEFAULT.length;
	public static final int TAG_DEPRECATED_LENGTH = TAG_DEPRECATED.length;
	public static final int TAG_DESCRIPTION_LENGTH = TAG_DESCRIPTION.length;
	public static final int TAG_EVENT_LENGTH = TAG_EVENT.length;
	public static final int TAG_EXAMPLE_LENGTH = TAG_EXAMPLE.length;
	public static final int TAG_EXTENDS_LENGTH = TAG_EXTENDS.length;
	public static final int TAG_FIELD_LENGTH = TAG_FIELD.length;
	public static final int TAG_FILEOVERVIEW_LENGTH = TAG_FILEOVERVIEW.length;
	public static final int TAG_FUNCTION_LENGTH = TAG_FUNCTION.length;
	public static final int TAG_IGNORE_LENGTH = TAG_IGNORE.length;
	public static final int TAG_INNER_LENGTH = TAG_INNER.length;
	public static final int TAG_LENDS_LENGTH = TAG_LENDS.length;
	public static final int TAG_LINK_LENGTH = TAG_LINK.length;
	public static final int TAG_MEMBEROF_LENGTH = TAG_MEMBEROF.length;
	public static final int TAG_NAME_LENGTH = TAG_NAME.length;
	public static final int TAG_NAMESPACE_LENGTH = TAG_NAMESPACE.length;
	public static final int TAG_PARAM_LENGTH = TAG_PARAM.length;
	public static final int TAG_PRIVATE_LENGTH = TAG_PRIVATE.length;
	public static final int TAG_PROPERTY_LENGTH = TAG_PROPERTY.length;
	public static final int TAG_PUBLIC_LENGTH = TAG_PUBLIC.length;
	public static final int TAG_REQUIRES_LENGTH = TAG_REQUIRES.length;
	public static final int TAG_RETURN_LENGTH = TAG_RETURN.length;
	public static final int TAG_RETURNS_LENGTH = TAG_RETURNS.length;
	public static final int TAG_SEE_LENGTH = TAG_SEE.length;
	public static final int TAG_SINCE_LENGTH = TAG_SINCE.length;
	public static final int TAG_STATIC_LENGTH = TAG_STATIC.length;
	public static final int TAG_THROWS_LENGTH = TAG_THROWS.length;
	public static final int TAG_TYPE_LENGTH = TAG_TYPE.length;
	public static final int TAG_VERSION_LENGTH = TAG_VERSION.length;
	
	public static final int TAG_EXCEPTION_LENGTH = TAG_EXCEPTION.length;
	public static final int TAG_FINAL_LENGTH = TAG_FINAL.length;
	public static final int TAG_MEMBER_LENGTH = TAG_MEMBER.length;
	public static final int TAG_BASE_LENGTH = TAG_BASE.length;
	public static final int TAG_ADDON_LENGTH = TAG_ADDON.length;
	public static final int TAG_EXEC_LENGTH = TAG_EXEC.length;
	public static final int TAG_PROTECTED_LENGTH = TAG_PROTECTED.length;

	public static final int TAG_ALIAS_LENGTH = TAG_ALIAS.length;
	public static final int TAG_CLASSDECRIPTION_LENGTH = TAG_CLASSDECRIPTION.length;
	public static final int TAG_ID_LENGTH = TAG_ID.length;
	public static final int TAG_PROJECT_DESCRIPTION_LENGTH = TAG_PROJECT_DESCRIPTION.length;
	public static final int TAG_SDOC_LENGTH = TAG_SDOC.length;
	public static final int TAG_METHOD_LENGTH = TAG_METHOD.length;
	
	// tags value
	public static final int NO_TAG_VALUE = 0;
	public static final int TAG_AUGMENTS_VALUE = 1;
	public static final int TAG_AUTHOR_VALUE = 2;
	public static final int TAG_ARGUMENT_VALUE = 3;
	public static final int TAG_BORROWS_VALUE = 4;
	public static final int TAG_CLASS_VALUE = 5;
	public static final int TAG_CONSTANT_VALUE = 6;
	public static final int TAG_CONSTRUCTOR_VALUE = 7;
	public static final int TAG_CONSTRUCTS_VALUE = 8;
	public static final int TAG_DEFAULT_VALUE = 9;
	public static final int TAG_DEPRECATED_VALUE = 10;
	public static final int TAG_DESCRIPTION_VALUE = 11;
	public static final int TAG_EVENT_VALUE = 12;
	public static final int TAG_EXAMPLE_VALUE = 13;
	public static final int TAG_EXTENDS_VALUE = 14;
	public static final int TAG_FIELD_VALUE = 15;
	public static final int TAG_FILEOVERVIEW_VALUE = 16;
	public static final int TAG_FUNCTION_VALUE = 17;
	public static final int TAG_IGNORE_VALUE = 18;
	public static final int TAG_INNER_VALUE = 19;
	public static final int TAG_LENDS_VALUE = 20;
	public static final int TAG_LINK_VALUE = 21;
	public static final int TAG_MEMBEROF_VALUE = 22;
	public static final int TAG_NAME_VALUE = 23;
	public static final int TAG_NAMESPACE_VALUE = 24;
	public static final int TAG_PARAM_VALUE = 25;
	public static final int TAG_PRIVATE_VALUE = 26;
	public static final int TAG_PROPERTY_VALUE = 27;
	public static final int TAG_PUBLIC_VALUE = 28;
	public static final int TAG_REQUIRES_VALUE = 29;
	public static final int TAG_RETURN_VALUE = 30;
	public static final int TAG_RETURNS_VALUE = 31;
	public static final int TAG_SEE_VALUE = 32;
	public static final int TAG_SINCE_VALUE = 33;
	public static final int TAG_STATIC_VALUE = 34;
	public static final int TAG_THROWS_VALUE = 35;
	public static final int TAG_TYPE_VALUE = 36;
	public static final int TAG_VERSION_VALUE = 37;
	
	public static final int TAG_EXCEPTION_VALUE = 38;
	public static final int TAG_FINAL_VALUE = 39;
	public static final int TAG_MEMBER_VALUE = 40;
	public static final int TAG_BASE_VALUE = 41;
	public static final int TAG_ADDON_VALUE = 42;
	public static final int TAG_EXEC_VALUE = 43;
	public static final int TAG_PROTECTED_VALUE = 44;

	public static final int TAG_ALIAS_VALUE = 45;
	public static final int TAG_CLASSDECRIPTION_VALUE = 46;
	public static final int TAG_ID_VALUE = 47;
	public static final int TAG_PROJECT_DESCRIPTION_VALUE = 48;
	public static final int TAG_SDOC_VALUE = 49;
	public static final int TAG_METHOD_VALUE = 50;

	public static final int TAG_OTHERS_VALUE = 100;

	// tags expected positions
	public final static int PARAM_TAG_EXPECTED_ORDER = 0;
	public final static int THROWS_TAG_EXPECTED_ORDER = 1;
	public final static int SEE_TAG_EXPECTED_ORDER = 2;
	public final static int ORDERED_TAGS_NUMBER = 3;

	/*
	 * Tag kinds indexes
	 */
	public final static int BLOCK_IDX = 0;
	public final static int INLINE_IDX = 1;

	/*
	 * Tags versions
	 */
	public static final char[][][] BLOCK_TAGS = {
		// since 1.0
		{ TAG_AUGMENTS, TAG_AUTHOR, TAG_ARGUMENT, TAG_BORROWS, TAG_CLASS, TAG_CONSTANT, TAG_CONSTRUCTOR, TAG_CONSTRUCTS, 
			TAG_DEFAULT, TAG_DEPRECATED, TAG_DESCRIPTION, TAG_EVENT, TAG_EXAMPLE, TAG_EXTENDS, TAG_FIELD, TAG_FILEOVERVIEW,
			TAG_FUNCTION, TAG_IGNORE, TAG_INNER, TAG_LENDS, TAG_MEMBEROF, TAG_NAME, TAG_NAMESPACE, TAG_PARAM, TAG_PRIVATE,
			TAG_PROPERTY, TAG_PUBLIC, TAG_REQUIRES, TAG_RETURN, TAG_RETURNS, TAG_SEE, TAG_SINCE, TAG_STATIC, TAG_THROWS, 
			TAG_TYPE, TAG_VERSION, TAG_EXCEPTION, TAG_FINAL, TAG_MEMBER, TAG_BASE, TAG_ADDON, TAG_EXEC, TAG_PROTECTED,
			TAG_ALIAS, TAG_CLASSDECRIPTION, TAG_ID, TAG_PROJECT_DESCRIPTION, TAG_SDOC, TAG_METHOD},
		// since 1.1
		{},
		// since 1.2
		{},
		// since 1.3
		{},
		// since 1.4
		{},
		// since 1.5
		{},
		// since 1.6
		{}
	};
	public static final char[][][] INLINE_TAGS = {
		// since 1.0
		{TAG_LINK},
		// since 1.1
		{},
		// since 1.2
		{},
		// since 1.3
		{},
		// since 1.4
		{},
		// since 1.5
		{},
		// since 1.6
		{}
	};
	
	public final static int INLINE_TAGS_LENGTH = INLINE_TAGS.length;
	public final static int BLOCK_TAGS_LENGTH = BLOCK_TAGS.length;
	public final static int ALL_TAGS_LENGTH = BLOCK_TAGS_LENGTH+INLINE_TAGS_LENGTH;

	/*
	 * Tags usage
	 */
	public static final char[][] PACKAGE_TAGS = {
		TAG_SEE,
		TAG_SINCE,
		TAG_AUTHOR,
		TAG_VERSION,
		TAG_LINK,
		TAG_CLASSDECRIPTION,TAG_FILEOVERVIEW,TAG_PROJECT_DESCRIPTION
	};
	public static final char[][] CLASS_TAGS = {
		TAG_SEE,
		TAG_SINCE,
		TAG_DEPRECATED,
		TAG_AUTHOR,
		TAG_VERSION,
		TAG_PARAM,
		TAG_LINK,
		TAG_CLASSDECRIPTION,TAG_NAMESPACE, TAG_REQUIRES, TAG_PRIVATE,TAG_CLASS
	};
	public static final char[][] FIELD_TAGS = {
		TAG_SEE,
		TAG_SINCE,
		TAG_DEPRECATED,
		TAG_LINK,
		TAG_MEMBEROF,TAG_FINAL,TAG_PRIVATE
	};
	public static final char[][] METHOD_TAGS = {
		TAG_SEE,
		TAG_SINCE,
		TAG_DEPRECATED,
		TAG_PARAM,
		TAG_RETURNS,
		TAG_THROWS,
		TAG_EXCEPTION,
		TAG_LINK,
		TAG_BASE, TAG_ADDON,TAG_CONSTRUCTOR,TAG_TYPE
	};
}
