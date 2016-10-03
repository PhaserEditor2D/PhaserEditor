/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     IBM Corporation - added the following constants
 *								   NonStaticAccessToStaticField
 *								   NonStaticAccessToStaticMethod
 *								   Task
 *								   ExpressionShouldBeAVariable
 *								   AssignmentHasNoEffect
 *     IBM Corporation - added the following constants
 *								   TooManyArrayDimensions
 *								   TooManyBytesForStringConstant
 *								   TooManyMethods
 *								   TooManyFields
 *								   NonBlankFinalLocalAssignment
 *								   ObjectCannotHaveSuperTypes
 *								   MissingSemiColon
 *								   InvalidParenthesizedExpression
 *								   EnclosingInstanceInConstructorCall
 *								   BytecodeExceeds64KLimitForConstructor
 *								   IncompatibleReturnTypeForNonInheritedInterfaceMethod
 *								   UnusedPrivateMethod
 *								   UnusedPrivateConstructor
 *								   UnusedPrivateType
 *								   UnusedPrivateField
 *								   IncompatibleExceptionInThrowsClauseForNonInheritedInterfaceMethod
 *								   InvalidExplicitConstructorCall
 *     IBM Corporation - added the following constants
 *								   PossibleAccidentalBooleanAssignment
 *								   SuperfluousSemicolon
 *								   IndirectAccessToStaticField
 *								   IndirectAccessToStaticMethod
 *								   IndirectAccessToStaticType
 *								   BooleanMethodThrowingException
 *								   UnnecessaryCast
 *								   UnnecessaryArgumentCast
 *								   UnnecessaryInstanceof
 *								   FinallyMustCompleteNormally
 *								   UnusedMethodDeclaredThrownException
 *								   UnusedConstructorDeclaredThrownException
 *								   InvalidCatchBlockSequence
 *								   UnqualifiedFieldAccess
 *     IBM Corporation - added the following constants
 *								   Javadoc
 *								   JavadocUnexpectedTag
 *								   JavadocMissingParamTag
 *								   JavadocMissingParamName
 *								   JavadocDuplicateParamName
 *								   JavadocInvalidParamName
 *								   JavadocMissingReturnTag
 *								   JavadocDuplicateReturnTag
 *								   JavadocMissingThrowsTag
 *								   JavadocMissingThrowsClassName
 *								   JavadocInvalidThrowsClass
 *								   JavadocDuplicateThrowsClassName
 *								   JavadocInvalidThrowsClassName
 *								   JavadocMissingSeeReference
 *								   JavadocInvalidSeeReference
 *								   JavadocInvalidSeeHref
 *								   JavadocInvalidSeeArgs
 *								   JavadocMissing
 *								   JavadocInvalidTag
 *								   JavadocMessagePrefix
 *								   EmptyControlFlowStatement
 *     IBM Corporation - added the following constants
 *								   IllegalUsageOfQualifiedTypeReference
 *								   InvalidDigit
 *     IBM Corporation - added the following constants
 *								   ParameterAssignment
 *								   FallthroughCase
 *     IBM Corporation - added the following constants
 *                                 UnusedLabel
 *                                 UnnecessaryNLSTag
 *                                 LocalVariableMayBeNull
 *                                 EnumConstantsCannotBeSurroundedByParenthesis
 *                                 JavadocMissingIdentifier
 *                                 JavadocNonStaticTypeFromStaticInvocation
 *                                 RawTypeReference
 *                                 NoAdditionalBoundAfterTypeVariable
 *                                 UnsafeGenericArrayForVarargs
 *                                 IllegalAccessFromTypeVariable
 *                                 InvalidEncoding
 *                                 CannotReadSource
 *                                 ExternalProblemNotFixable
 *                                 ExternalProblemFixable
 *     IBM Corporation - added the following constants
 *                                 OverridingMethodWithoutSuperInvocation
 *                                 MethodMustOverrideOrImplement
 *                                 TypeHidingTypeParameterFromType
 *                                 TypeHidingTypeParameterFromMethod
 *                                 TypeHidingType
 *     IBM Corporation - added the following constants
 *								   NullLocalVariableReference
 *								   PotentialNullLocalVariableReference
 *								   RedundantNullCheckOnNullLocalVariable
 * 								   NullLocalVariableComparisonYieldsFalse
 * 								   RedundantLocalVariableNullAssignment
 * 								   NullLocalVariableInstanceofYieldsFalse
 * 								   RedundantNullCheckOnNonNullLocalVariable
 * 								   NonNullLocalVariableComparisonYieldsFalse
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.compiler;

import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReasons;

/**
 * Description of a JavaScript problem, as detected by the validator
 * A problem provides access to:
 * <ul>
 * <li> its location (originating source file name, source position, line number), </li>
 * <li> its message description and a predicate to check its severity (warning or error). </li>
 * <li> its ID : a number identifying the very nature of this problem. All possible IDs are listed
 * as constants on this interface. </li>
 * </ul>
 *
 * Note: the validator produces IProblems internally, which are turned into markers by the JavaScriptBuilder
 * so as to persist problem descriptions. This explains why there is no API allowing to reach IProblem detected
 * when compiling. However, the JavaScript problem markers carry equivalent information to IProblem, in particular
 * their ID (attribute "id") is set to one of the IDs defined on this interface.
 *  
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 */
public interface IProblem {

/**
 * Answer back the original arguments recorded into the problem.
 * @return the original arguments recorded into the problem
 */
String[] getArguments();

/**
 * Returns the problem id
 *
 * @return the problem id
 */
int getID();

/**
 * Answer a localized, human-readable message string which describes the problem.
 *
 * @return a localized, human-readable message string which describes the problem
 */
String getMessage();

/**
 * Answer the file name in which the problem was found.
 *
 * @return the file name in which the problem was found
 */
char[] getOriginatingFileName();

/**
 * Answer the end position of the problem (inclusive), or -1 if unknown.
 *
 * @return the end position of the problem (inclusive), or -1 if unknown
 */
int getSourceEnd();

/**
 * Answer the line number in source where the problem begins.
 *
 * @return the line number in source where the problem begins
 */
int getSourceLineNumber();

/**
 * Answer the start position of the problem (inclusive), or -1 if unknown.
 *
 * @return the start position of the problem (inclusive), or -1 if unknown
 */
int getSourceStart();

/**
 * Checks the severity to see if the Error bit is set.
 *
 * @return true if the Error bit is set for the severity, false otherwise
 */
boolean isError();

/**
 * Checks the severity to see if the Error bit is not set.
 *
 * @return true if the Error bit is not set for the severity, false otherwise
 */
boolean isWarning();

/**
 * Set the end position of the problem (inclusive), or -1 if unknown.
 * Used for shifting problem positions.
 *
 * @param sourceEnd the given end position
 */
void setSourceEnd(int sourceEnd);

/**
 * Set the line number in source where the problem begins.
 *
 * @param lineNumber the given line number
 */
void setSourceLineNumber(int lineNumber);

/**
 * Set the start position of the problem (inclusive), or -1 if unknown.
 * Used for shifting problem positions.
 *
 * @param sourceStart the given start position
 */
void setSourceStart(int sourceStart);


	/**
	 * Problem Categories
	 * The high bits of a problem ID contains information about the category of a problem.
	 * For example, (problemID & TypeRelated) != 0, indicates that this problem is type related.
	 *
	 * A problem category can help to implement custom problem filters. Indeed, when numerous problems
	 * are listed, focusing on import related problems first might be relevant.
	 *
	 * When a problem is tagged as Internal, it means that no change other than a local source code change
	 * can  fix the corresponding problem. A type related problem could be addressed by changing the type
	 * involved in it.
	 */
	int TypeRelated = 0x01000000;
	int FieldRelated = 0x02000000;
	int MethodRelated = 0x04000000;
	int ConstructorRelated = 0x08000000;
	int ImportRelated = 0x10000000;
	int Internal = 0x20000000;
	int Syntax = 0x40000000;
	int Javadoc = 0x80000000;

	/**
	 * Mask to use in order to filter out the category portion of the problem ID.
	 */
	int IgnoreCategoriesMask = 0xFFFFFF;

	/**
	 * Below are listed all available problem IDs. Note that this list could be augmented in the future,
	 * as new features are added to the JavaScript core implementation.
	 */

	/**
	 * ID reserved for referencing an internal error inside the JavaScriptCore implementation which
	 * may be surfaced as a problem associated with the javaScript unit which caused it to occur.
	 */
	int Unclassified = 0;

	/**
	 * General type related problems
	 */
	int UndefinedType = TypeRelated + 2;
	int NotVisibleType = TypeRelated + 3;
	int AmbiguousType = TypeRelated + 4;
	int UsingDeprecatedType = TypeRelated + 5;
	int InternalTypeNameProvided = TypeRelated + 6;
	int UnusedPrivateType = Internal + TypeRelated + 7;

	int IncompatibleTypesInEqualityOperator = TypeRelated + 15;
	int IncompatibleTypesInConditionalOperator = TypeRelated + 16;
	int TypeMismatch = TypeRelated + 17;
	int IndirectAccessToStaticType = Internal + TypeRelated + 18;

	/**
	 * Inner types related problems
	 */
	int MissingEnclosingInstanceForConstructorCall = TypeRelated + 20;
	int MissingEnclosingInstance = TypeRelated + 21;
	int IncorrectEnclosingInstanceReference = TypeRelated + 22;
	int IllegalEnclosingInstanceSpecification = TypeRelated + 23;
	int CannotDefineStaticInitializerInLocalType = Internal + 24;
	int OuterLocalMustBeFinal = Internal + 25;
	int IllegalPrimitiveOrArrayTypeForEnclosingInstance = TypeRelated + 27;
	int EnclosingInstanceInConstructorCall = Internal + 28;
	int TypeHidingType = TypeRelated + 33;


	// variables
	int UndefinedName = Internal + FieldRelated + 50;
	int UninitializedLocalVariable = Internal + 51;
	int VariableTypeCannotBeVoid = Internal + 52;
	int CannotAllocateVoidArray = Internal + 54;
	// local variables
	int RedefinedLocal = Internal + 55;
	int RedefinedArgument = Internal + 56;
	// final local variables
	int DuplicateFinalLocalInitialization = Internal + 57;
	int NonBlankFinalLocalAssignment = Internal + 58;
	int ParameterAssignment = Internal + 59;
	int FinalOuterLocalAssignment = Internal + 60;
	int LocalVariableIsNeverUsed = Internal + 61;
	int ArgumentIsNeverUsed = Internal + 62;
	int BytecodeExceeds64KLimit = Internal + 63;
	int BytecodeExceeds64KLimitForClinit = Internal + 64;
	int TooManyArgumentSlots = Internal + 65;
	int TooManyLocalVariableSlots = Internal + 66;
	int TooManyArrayDimensions = Internal + 68;
	int BytecodeExceeds64KLimitForConstructor = Internal + 69;

	// fields
	int UndefinedField = FieldRelated + 70;
	int NotVisibleField = FieldRelated + 71;
	int AmbiguousField = FieldRelated + 72;
	int UsingDeprecatedField = FieldRelated + 73;
	int NonStaticFieldFromStaticInvocation = FieldRelated + 74;
	int ReferenceToForwardField = FieldRelated + Internal + 75;
	int NonStaticAccessToStaticField = Internal + FieldRelated + 76;
	int UnusedPrivateField = Internal + FieldRelated + 77;
	int IndirectAccessToStaticField = Internal + FieldRelated + 78;
	int UnqualifiedFieldAccess = Internal + FieldRelated + 79;

	// blank final fields
	int FinalFieldAssignment = FieldRelated + 80;
	int UninitializedBlankFinalField = FieldRelated + 81;
	int DuplicateBlankFinalFieldInitialization = FieldRelated + 82;

	// variable hiding
	int LocalVariableHidingLocalVariable = Internal + 90;
	int LocalVariableHidingField = Internal + FieldRelated + 91;
	int FieldHidingLocalVariable = Internal + FieldRelated + 92;
	int FieldHidingField = Internal + FieldRelated + 93;
	int ArgumentHidingLocalVariable = Internal + 94;
	int ArgumentHidingField = Internal + 95;
	/* START -------------------------------- Bug 197884 Loosly defined var (for statement) and optional semi-colon --------------------- */
	int LooseVarDecl = Internal + 97;
	/* END   -------------------------------- Bug 197884 Loosly defined var (for statement) and optional semi-colon --------------------- */
	int UninitializedGlobalVariable = Internal + 98;

	// methods
	int UndefinedMethod = MethodRelated + 100;
	int NotVisibleMethod = MethodRelated + 101;
	int AmbiguousMethod = MethodRelated + 102;
	int UsingDeprecatedMethod = MethodRelated + 103;
	int DirectInvocationOfAbstractMethod = MethodRelated + 104;
	int VoidMethodReturnsValue = MethodRelated + 105;
	int MethodReturnsVoid = MethodRelated + 106;
	int MethodRequiresBody = Internal + MethodRelated + 107;
	int ShouldReturnValue = Internal + MethodRelated + 108;
	int UndefinedFunction = MethodRelated + 109;
	int MethodButWithConstructorName = MethodRelated + 110;
	int MissingReturnType = TypeRelated + 111;
	int BodyForNativeMethod = Internal + MethodRelated + 112;
	int BodyForAbstractMethod = Internal + MethodRelated + 113;
	int NoMessageSendOnBaseType = MethodRelated + 114;
	int ParameterMismatch = MethodRelated + 115;
	int NoMessageSendOnArrayType = MethodRelated + 116;
    int NonStaticAccessToStaticMethod = Internal + MethodRelated + 117;
	int UnusedPrivateMethod = Internal + MethodRelated + 118;
	int IndirectAccessToStaticMethod = Internal + MethodRelated + 119;
	int WrongNumberOfArguments = Internal + MethodRelated + 120;
	int NotAFunction = Internal + MethodRelated + 121;

	// constructors
	int UndefinedConstructor = ConstructorRelated + 130;
	int NotVisibleConstructor = ConstructorRelated + 131;
	int AmbiguousConstructor = ConstructorRelated + 132;
	int UsingDeprecatedConstructor = ConstructorRelated + 133;
	int UnusedPrivateConstructor = Internal + MethodRelated + 134;
	// explicit constructor calls
	int InstanceFieldDuringConstructorInvocation = ConstructorRelated + 135;
	int InstanceMethodDuringConstructorInvocation = ConstructorRelated + 136;
	int RecursiveConstructorInvocation = ConstructorRelated + 137;
	int ThisSuperDuringConstructorInvocation = ConstructorRelated + 138;
	int InvalidExplicitConstructorCall = ConstructorRelated + Syntax + 139;
	// implicit constructor calls
	int UndefinedConstructorInDefaultConstructor = ConstructorRelated + 140;
	int NotVisibleConstructorInDefaultConstructor = ConstructorRelated + 141;
	int AmbiguousConstructorInDefaultConstructor = ConstructorRelated + 142;
	int UndefinedConstructorInImplicitConstructorCall = ConstructorRelated + 143;
	int NotVisibleConstructorInImplicitConstructorCall = ConstructorRelated + 144;
	int AmbiguousConstructorInImplicitConstructorCall = ConstructorRelated + 145;
	int UnhandledExceptionInDefaultConstructor = TypeRelated + 146;
	int UnhandledExceptionInImplicitConstructorCall = TypeRelated + 147;

	// expressions
	int ArrayReferenceRequired = Internal + 150;
	// constant expressions
	int StringConstantIsExceedingUtf8Limit = Internal + 152;
	int NumericValueOutOfRange = Internal + 154;
	// allocations
	int InvalidClassInstantiation = TypeRelated + 157;
	int CannotDefineDimensionExpressionsWithInit = Internal + 158;
	int MustDefineEitherDimensionExpressionsOrInitializer = Internal + 159;
	// operators
	int InvalidOperator = Internal + 160;
	// statements
	int CodeCannotBeReached = Internal + 161;
	int CannotReturnOutsideFunction = Internal + 162;
	int InitializerMustCompleteNormally = Internal + 163;
	// assert
	int InvalidVoidExpression = Internal + 164;
	// try
	int MaskedCatch = TypeRelated + 165;
	int DuplicateDefaultCase = Internal + 166;
	int UnreachableCatch = TypeRelated + MethodRelated + 167;
	int UnhandledException = TypeRelated + 168;
	// switch
	int IncorrectSwitchType = TypeRelated + 169;
	int DuplicateCase = FieldRelated + 170;

	// labelled
	int DuplicateLabel = Internal + 171;
	int InvalidBreak = Internal + 172;
	int InvalidContinue = Internal + 173;
	int UndefinedLabel = Internal + 174;
	//synchronized
	int InvalidTypeToSynchronized = Internal + 175;
	int InvalidNullToSynchronized = Internal + 176;
	// throw
	int CannotThrowNull = Internal + 177;
	// assignment
	int AssignmentHasNoEffect = Internal + 178;
	int PossibleAccidentalBooleanAssignment = Internal + 179;
	int SuperfluousSemicolon = Internal + 180;
	int UnnecessaryInstanceof = Internal + TypeRelated + 183;
	int FinallyMustCompleteNormally = Internal + 184;
	int UnusedMethodDeclaredThrownException = Internal + 185;
	int UnusedConstructorDeclaredThrownException = Internal + 186;
	int EmptyControlFlowStatement = Internal + TypeRelated + 188;
	int UnnecessaryElse = Internal + 189;

	// inner emulation
	int NeedToEmulateFieldReadAccess = FieldRelated + 190;
	int NeedToEmulateFieldWriteAccess = FieldRelated + 191;
	int NeedToEmulateMethodAccess = MethodRelated + 192;
	int NeedToEmulateConstructorAccess = MethodRelated + 193;

	int FallthroughCase = Internal + 194;

	//inherited name hides enclosing name (sort of ambiguous)
	int InheritedMethodHidesEnclosingName = MethodRelated + 195;
	int InheritedFieldHidesEnclosingName = FieldRelated + 196;
	int InheritedTypeHidesEnclosingName = TypeRelated + 197;

	int IllegalUsageOfQualifiedTypeReference = Internal + Syntax + 198;

	// miscellaneous
	int UnusedLabel = Internal + 199;
	int ThisInStaticContext = Internal + 200;
	int StaticMethodRequested = Internal + MethodRelated + 201;
	int IllegalDimension = Internal + 202;
	int ParsingError = Syntax + Internal + 204;
	int ParsingErrorNoSuggestion = Syntax + Internal + 205;
	int InvalidUnaryExpression = Syntax + Internal + 206;

	// syntax errors
	int ArrayConstantsOnlyInArrayInitializers = Syntax + Internal + 208;
	int ParsingErrorOnKeyword = Syntax + Internal + 209;
	int ParsingErrorOnKeywordNoSuggestion = Syntax + Internal + 210;

	int UnmatchedBracket = Syntax + Internal + 220;
	int NoFieldOnBaseType = FieldRelated + 221;
	int InvalidExpressionAsStatement = Syntax + Internal + 222;
	int ExpressionShouldBeAVariable = Syntax + Internal + 223;
	int MissingSemiColon = Syntax + Internal + 224;
	int InvalidParenthesizedExpression = Syntax + Internal + 225;

	int ParsingErrorInsertTokenBefore = Syntax + Internal + 230;
	int ParsingErrorInsertTokenAfter = Syntax + Internal + 231;
    int ParsingErrorDeleteToken = Syntax + Internal + 232;
    int ParsingErrorDeleteTokens = Syntax + Internal + 233;
    int ParsingErrorMergeTokens = Syntax + Internal + 234;
    int ParsingErrorInvalidToken = Syntax + Internal + 235;
    int ParsingErrorMisplacedConstruct = Syntax + Internal + 236;
    int ParsingErrorReplaceTokens = Syntax + Internal + 237;
    int ParsingErrorNoSuggestionForTokens = Syntax + Internal + 238;
    int ParsingErrorUnexpectedEOF = Syntax + Internal + 239;
    int ParsingErrorInsertToComplete = Syntax + Internal + 240;
    int ParsingErrorInsertToCompleteScope = Syntax + Internal + 241;
    int ParsingErrorInsertToCompletePhrase = Syntax + Internal + 242;
    /* START -------------------------------- Bug 197884 Loosly defined var (for statement) and optional semi-colon --------------------- */
    int OptionalSemiColon = Syntax + Internal + 243;
    /* END   -------------------------------- Bug 197884 Loosly defined var (for statement) and optional semi-colon --------------------- */


	// scanner errors
	int EndOfSource = Syntax + Internal + 250;
	int InvalidHexa = Syntax + Internal + 251;
	int InvalidOctal = Syntax + Internal + 252;
	int InvalidCharacterConstant = Syntax + Internal + 253;
	int InvalidEscape = Syntax + Internal + 254;
	int InvalidInput = Syntax + Internal + 255;
	int InvalidUnicodeEscape = Syntax + Internal + 256;
	int InvalidFloat = Syntax + Internal + 257;
	int NullSourceString = Syntax + Internal + 258;
	int UnterminatedString = Syntax + Internal + 259;
	int UnterminatedComment = Syntax + Internal + 260;
	int NonExternalizedStringLiteral = Internal + 261;
	int InvalidDigit = Syntax + Internal + 262;
	int InvalidLowSurrogate = Syntax + Internal + 263;
	int InvalidHighSurrogate = Syntax + Internal + 264;
	int UnnecessaryNLSTag = Internal + 265;

	// type related problems
	int DiscouragedReference = TypeRelated + 280;

	int DuplicateModifierForType = TypeRelated + 301;
	int IllegalModifierForClass = TypeRelated + 302;
	int IllegalModifierForMemberClass = TypeRelated + 304;
	int IllegalModifierForLocalClass = TypeRelated + 306;
	int ForbiddenReference = TypeRelated + 307;
	int IllegalModifierCombinationFinalAbstractForClass = TypeRelated + 308;
	int IllegalVisibilityModifierCombinationForMemberType = TypeRelated + 310;
	int IllegalStaticModifierForMemberType = TypeRelated + 311;
	int SuperclassMustBeAClass = TypeRelated + 312;
	int ClassExtendFinalClass = TypeRelated + 313;
	int HierarchyCircularitySelfReference = TypeRelated + 316;
	int HierarchyCircularity = TypeRelated + 317;
	int HidingEnclosingType = TypeRelated + 318;
	int DuplicateNestedType = TypeRelated + 319;
	int CannotThrowType = TypeRelated + 320;
	int PackageCollidesWithType = TypeRelated + 321;
	int TypeCollidesWithPackage = TypeRelated + 322;
	int DuplicateTypes = TypeRelated + 323;
	int IsClassPathCorrect = TypeRelated + 324;
	int MustSpecifyPackage = Internal + 326;
	int HierarchyHasProblems = TypeRelated + 327;
	int PackageIsNotExpectedPackage = Internal + 328;
	int ObjectCannotHaveSuperTypes = Internal + 329;
	int ObjectMustBeClass = Internal + 330;

	// field related problems
	int DuplicateField = FieldRelated + 340;
	int DuplicateModifierForField = FieldRelated + 341;
	int IllegalModifierForField = FieldRelated + 342;
	int IllegalVisibilityModifierCombinationForField = FieldRelated + 344;
	int IllegalModifierCombinationFinalVolatileForField = FieldRelated + 345;
	int UnexpectedStaticModifierForField = FieldRelated + 346;

	// method related problems
	int DuplicateMethod = MethodRelated + 355;
	int DuplicateModifierForMethod = MethodRelated + 357;
	int IllegalModifierForMethod = MethodRelated + 358;
	int IllegalVisibilityModifierCombinationForMethod = MethodRelated + 360;
	int UnexpectedStaticModifierForMethod = MethodRelated + 361;
	int IllegalAbstractModifierCombinationForMethod = MethodRelated + 362;
	int AbstractMethodInAbstractClass = MethodRelated + 363;
	int ArgumentTypeCannotBeVoid = MethodRelated + 364;
	int NativeMethodsCannotBeStrictfp = MethodRelated + 367;
	int DuplicateModifierForArgument = MethodRelated + 368;

	// import related problems
	int ConflictingImport = ImportRelated + 385;
	int DuplicateImport = ImportRelated + 386;
	int CannotImportPackage = ImportRelated + 387;

	int ImportNotFound =  ImportRelated + 389 + ProblemReasons.NotFound; // ImportRelated + 390

	// local variable related problems
	int DuplicateModifierForVariable = MethodRelated + 395;

	// method verifier problems
	int AbstractMethodMustBeImplemented = MethodRelated + 400;
	int IncompatibleExceptionInThrowsClause = MethodRelated + 402;
	int IncompatibleExceptionInInheritedMethodThrowsClause = MethodRelated + 403;
	int IncompatibleReturnType = MethodRelated + 404;
	int InheritedMethodReducesVisibility = MethodRelated + 405;
	int CannotOverrideAStaticMethodWithAnInstanceMethod = MethodRelated + 406;
	int CannotHideAnInstanceMethodWithAStaticMethod = MethodRelated + 407;
	int StaticInheritedMethodConflicts = MethodRelated + 408;
	int MethodReducesVisibility = MethodRelated + 409;
	int OverridingNonVisibleMethod = MethodRelated + 410;
	int AbstractMethodCannotBeOverridden = MethodRelated + 411;
	int OverridingDeprecatedMethod = MethodRelated + 412;
	int IllegalVararg = MethodRelated + 415;
	int OverridingMethodWithoutSuperInvocation = MethodRelated + 416;

	// code snippet support
	int CodeSnippetMissingClass = Internal + 420;
	int CodeSnippetMissingMethod = Internal + 421;

	//constant pool
	int TooManyConstantsInConstantPool = Internal + 430;
	int TooManyBytesForStringConstant = Internal + 431;

	// static constraints
	int TooManyFields = Internal + 432;
	int TooManyMethods = Internal + 433;

	// 1.4 features
	// assertion warning
	int UseAssertAsAnIdentifier = Internal + 440;

	// 1.5 features
	int UseEnumAsAnIdentifier = Internal + 441;

	// detected task
	int Task = Internal + 450;

	// local variables related problems, cont'd
	int NullLocalVariableReference = Internal + 451;
	int PotentialNullLocalVariableReference = Internal + 452;
	int RedundantNullCheckOnNullLocalVariable = Internal + 453;
	int NullLocalVariableComparisonYieldsFalse = Internal + 454;
	int RedundantLocalVariableNullAssignment = Internal + 455;
	int NullLocalVariableInstanceofYieldsFalse = Internal + 456;
	int RedundantNullCheckOnNonNullLocalVariable = Internal + 457;
	int NonNullLocalVariableComparisonYieldsFalse = Internal + 458;


	// block
	int UndocumentedEmptyBlock = Internal + 460;

	/*
	 * Javadoc comments
	 */
	/**
	 * Problem signaled on an hidden reference due to a too low visibility level.
	 */
	int JavadocHiddenReference = Javadoc + Internal + 465;
	/**
	 * Problem signaled on an invalid qualification for member type reference.
	 */
	int JavadocInvalidMemberTypeQualification = Javadoc + Internal + 466;
	int JavadocMissingIdentifier = Javadoc + Internal + 467;
	int JavadocNonStaticTypeFromStaticInvocation = Javadoc + Internal + 468;
	int JavadocUnexpectedTag = Javadoc + Internal + 470;
	int JavadocMissingParamTag = Javadoc + Internal + 471;
	int JavadocMissingParamName = Javadoc + Internal + 472;
	int JavadocDuplicateParamName = Javadoc + Internal + 473;
	int JavadocInvalidParamName = Javadoc + Internal + 474;
	int JavadocMissingReturnTag = Javadoc + Internal + 475;
	int JavadocDuplicateReturnTag = Javadoc + Internal + 476;
	int JavadocMissingThrowsTag = Javadoc + Internal + 477;
	int JavadocMissingThrowsClassName = Javadoc + Internal + 478;
	int JavadocInvalidThrowsClass = Javadoc + Internal + 479;
	int JavadocDuplicateThrowsClassName = Javadoc + Internal + 480;
	int JavadocInvalidThrowsClassName = Javadoc + Internal + 481;
	int JavadocMissingSeeReference = Javadoc + Internal + 482;
	int JavadocInvalidSeeReference = Javadoc + Internal + 483;
	int JavadocInvalidSeeHref = Javadoc + Internal + 484;
	int JavadocInvalidSeeArgs = Javadoc + Internal + 485;
	int JavadocMissing = Javadoc + Internal + 486;
	int JavadocInvalidTag = Javadoc + Internal + 487;
	/*
	 * ID for field errors in Javadoc
	 */
	int JavadocUndefinedField = Javadoc + Internal + 488;
	int JavadocNotVisibleField = Javadoc + Internal + 489;
	int JavadocAmbiguousField = Javadoc + Internal + 490;
	int JavadocUsingDeprecatedField = Javadoc + Internal + 491;
	/*
	 * IDs for constructor errors in Javadoc
	 */
	int JavadocUndefinedConstructor = Javadoc + Internal + 492;
	int JavadocNotVisibleConstructor = Javadoc + Internal + 493;
	int JavadocAmbiguousConstructor = Javadoc + Internal + 494;
	int JavadocUsingDeprecatedConstructor = Javadoc + Internal + 495;
	/*
	 * IDs for method errors in Javadoc
	 */
	int JavadocUndefinedMethod = Javadoc + Internal + 496;
	int JavadocNotVisibleMethod = Javadoc + Internal + 497;
	int JavadocAmbiguousMethod = Javadoc + Internal + 498;
	int JavadocUsingDeprecatedMethod = Javadoc + Internal + 499;
	int JavadocNoMessageSendOnBaseType = Javadoc + Internal + 500;
	int JavadocParameterMismatch = Javadoc + Internal + 501;
	int JavadocNoMessageSendOnArrayType = Javadoc + Internal + 502;
	/*
	 * IDs for type errors in Javadoc
	 */
	int JavadocUndefinedType = Javadoc + Internal + 503;
	int JavadocNotVisibleType = Javadoc + Internal + 504;
	int JavadocAmbiguousType = Javadoc + Internal + 505;
	int JavadocUsingDeprecatedType = Javadoc + Internal + 506;
	int JavadocInternalTypeNameProvided = Javadoc + Internal + 507;
	int JavadocInheritedMethodHidesEnclosingName = Javadoc + Internal + 508;
	int JavadocInheritedFieldHidesEnclosingName = Javadoc + Internal + 509;
	int JavadocInheritedNameHidesEnclosingTypeName = Javadoc + Internal + 510;
	int JavadocUnterminatedInlineTag = Javadoc + Internal + 512;
	int JavadocMalformedSeeReference = Javadoc + Internal + 513;
	int JavadocMessagePrefix = Internal + 514;
	int JavadocMissingHashCharacter = Javadoc + Internal + 515;
	int JavadocEmptyReturnTag = Javadoc + Internal + 516;
	int JavadocUnexpectedText = Javadoc + Internal + 518;
	int JavadocInvalidParamTagName = Javadoc + Internal + 519;

	/**
	 * Foreach
	 */
	int IncompatibleTypesInForeach = TypeRelated + 580;
	int InvalidTypeForCollection = Internal + 581;

	/**
	 * 1.5 Syntax errors (when source level < 1.5)
	 */
    int InvalidUsageOfForeachStatements = Syntax + Internal + 592;

	/**
	 * Corrupted binaries
	 */
	int CorruptedSignature = Internal + 700;
	/**
	 * Corrupted source
	 */
	int InvalidEncoding = Internal + 701;
	int CannotReadSource = Internal + 702;

	/**
	 * External problems -- These are problems defined by other plugins
	 */

	int ExternalProblemNotFixable = 900;

	// indicates an externally defined problem that has a quick-assist processor
	// associated with it
	int ExternalProblemFixable = 901;
	
	int InvalidValueForSetter = 902;
	int InvalidValueForGetter = 903;
}
