--main options
%options ACTION, AN=JavaAction.java, GP=java, 
%options FILE-PREFIX=java, ESCAPE=$, PREFIX=TokenName, OUTPUT-SIZE=125 ,
%options NOGOTO-DEFAULT, SINGLE-PRODUCTIONS, LALR=1 , TABLE, 

--error recovering options.....
%options ERROR_MAPS 

--grammar understanding options
%options first follow
%options TRACE=FULL ,
%options VERBOSE

%options DEFERRED
%options NAMES=MAX
%options SCOPES

--Usefull macros helping reading/writing semantic actions
$Define 
$putCase 
/.    case $rule_number : if (DEBUG) { System.out.println("$rule_text"); }  //$NON-NLS-1$
		   ./

$break
/. 
			break;
./


$readableName 
/.1#$rule_number#./
$compliance
/.2#$rule_number#./
$recovery
/.2#$rule_number# recovery./
$recovery_template
/.3#$rule_number#./
$no_statements_recovery
/.4#$rule_number# 1./
$empty_statement
/.5#$rule_number# 1./
-- here it starts really ------------------------------------------
$Terminals

	Identifier

	abstract boolean break byte case catch char class 
	continue const default delete debugger do double else enum export extends false final finally float
	for function  goto if in infinity implements import instanceof int
	interface long native new null package private
	protected public return short static strictfp super switch
	synchronized this throw throws transient true try typeof undefined var void
	volatile with while

	IntegerLiteral
	LongLiteral
	FloatingPointLiteral
	DoubleLiteral
	CharacterLiteral
	StringLiteral
	RegExLiteral

	PLUS_PLUS
	MINUS_MINUS
	EQUAL_EQUAL
	EQUAL_EQUAL_EQUAL
	NOT_EQUAL_EQUAL
	LESS_EQUAL
	GREATER_EQUAL
	NOT_EQUAL
	LEFT_SHIFT
	RIGHT_SHIFT
	UNSIGNED_RIGHT_SHIFT
	PLUS_EQUAL
	MINUS_EQUAL
	MULTIPLY_EQUAL
	DIVIDE_EQUAL
	AND_EQUAL
	OR_EQUAL
	XOR_EQUAL
	REMAINDER_EQUAL
	LEFT_SHIFT_EQUAL
	RIGHT_SHIFT_EQUAL
	UNSIGNED_RIGHT_SHIFT_EQUAL
	OR_OR
	AND_AND
	PLUS
	MINUS
	NOT
	REMAINDER
	XOR
	AND
	MULTIPLY
	OR
	TWIDDLE
	DIVIDE
	GREATER
	LESS
	LPAREN
	RPAREN
	LBRACE
	RBRACE
	LBRACKET
	RBRACKET
	SEMICOLON
	QUESTION
	COLON
	COMMA
	DOT
	EQUAL
	AT

--    BodyMarker

$Alias

	'++'   ::= PLUS_PLUS
	'--'   ::= MINUS_MINUS
	'=='   ::= EQUAL_EQUAL
	'==='  ::= EQUAL_EQUAL_EQUAL
	'<='   ::= LESS_EQUAL
	'>='   ::= GREATER_EQUAL
	'!='   ::= NOT_EQUAL
	'!=='   ::= NOT_EQUAL_EQUAL
	'<<'   ::= LEFT_SHIFT
	'>>'   ::= RIGHT_SHIFT
	'>>>'  ::= UNSIGNED_RIGHT_SHIFT
	'+='   ::= PLUS_EQUAL
	'-='   ::= MINUS_EQUAL
	'*='   ::= MULTIPLY_EQUAL
	'/='   ::= DIVIDE_EQUAL
	'&='   ::= AND_EQUAL
	'|='   ::= OR_EQUAL
	'^='   ::= XOR_EQUAL
	'%='   ::= REMAINDER_EQUAL
	'<<='  ::= LEFT_SHIFT_EQUAL
	'>>='  ::= RIGHT_SHIFT_EQUAL
	'>>>=' ::= UNSIGNED_RIGHT_SHIFT_EQUAL
	'||'   ::= OR_OR
	'&&'   ::= AND_AND
	'+'    ::= PLUS
	'-'    ::= MINUS
	'!'    ::= NOT
	'%'    ::= REMAINDER
	'^'    ::= XOR
	'&'    ::= AND
	'*'    ::= MULTIPLY
	'|'    ::= OR
	'~'    ::= TWIDDLE
	'/'    ::= DIVIDE
	'>'    ::= GREATER
	'<'    ::= LESS
	'('    ::= LPAREN
	')'    ::= RPAREN
	'{'    ::= LBRACE
	'}'    ::= RBRACE
	'['    ::= LBRACKET
	']'    ::= RBRACKET
	';'    ::= SEMICOLON
	'?'    ::= QUESTION
	':'    ::= COLON
	','    ::= COMMA
	'.'    ::= DOT
	'='    ::= EQUAL
	'@'	   ::= AT
	
$Start
	Goal

$Rules

/.// This method is part of an automatic generation : do NOT edit-modify  
protected void consumeRule(int act) {
  switch ( act ) {
./



Goal ::= '++' CompilationUnit
Goal ::= '--' MethodBody
-- error recovery
-- Modifiersopt is used to properly consume a header and exit the rule reduction at the end of the parse() method
Goal ::= '>>>' Header1 Modifiersopt
Goal ::= '*' BlockStatements
Goal ::= '*' CatchHeader
-- JDOM
Goal ::= '&&' LocalVariableDeclaration
-- code snippet
Goal ::= '%' Expression
-- completion parser
Goal ::= '~' BlockStatementsopt
Goal ::= '+' ProgramElement
/:$readableName Goal:/

Literal -> IntegerLiteral
Literal -> LongLiteral
Literal -> FloatingPointLiteral
Literal -> DoubleLiteral
Literal -> CharacterLiteral
Literal -> StringLiteral
Literal -> RegExLiteral
Literal -> null
Literal -> undefined
Literal -> BooleanLiteral
/:$readableName Literal:/
BooleanLiteral -> true
BooleanLiteral -> false
/:$readableName BooleanLiteral:/

--------------------------------------------------------------
SimpleName -> 'Identifier'
/:$readableName SimpleName:/

CompilationUnit ::= EnterCompilationUnit InternalCompilationUnit
/.$putCase consumeCompilationUnit(); $break ./
/:$readableName CompilationUnit:/

InternalCompilationUnit ::= ProgramElements
/.$putCase consumeInternalCompilationUnitWithTypes(); $break ./
InternalCompilationUnit ::= $empty
/.$putCase consumeEmptyInternalCompilationUnit(); $break ./
/:$readableName CompilationUnit:/

EnterCompilationUnit ::= $empty
/.$putCase consumeEnterCompilationUnit(); $break ./
/:$readableName EnterCompilationUnit:/

Header -> RecoveryMethodHeader
Header -> LocalVariableDeclaration
/:$readableName Header:/

Header1 -> Header
/:$readableName Header1:/

CatchHeader ::= 'catch' '(' FormalParameter ')' '{'
/.$putCase consumeCatchHeader(); $break ./
/:$readableName CatchHeader:/

VariableDeclarators -> VariableDeclarator 
VariableDeclarators ::= VariableDeclarators ',' VariableDeclarator
/.$putCase consumeVariableDeclarators(); $break ./
/:$readableName VariableDeclarators:/

VariableDeclaratorsNoIn -> VariableDeclaratorNoIn
VariableDeclaratorsNoIn ::= VariableDeclaratorsNoIn ',' VariableDeclaratorNoIn
/.$putCase consumeVariableDeclarators(); $break ./
/:$readableName VariableDeclarators:/

VariableDeclarator ::= VariableDeclaratorId EnterVariable ExitVariableWithoutInitialization
VariableDeclarator ::= VariableDeclaratorId EnterVariable '=' ForceNoDiet VariableInitializer RestoreDiet ExitVariableWithInitialization
/:$readableName VariableDeclarator:/
/:$recovery_template Identifier:/

VariableDeclaratorNoIn ::= VariableDeclaratorId EnterVariable ExitVariableWithoutInitialization
VariableDeclaratorNoIn ::= VariableDeclaratorId EnterVariable '=' ForceNoDiet VariableInitializerNoIn RestoreDiet ExitVariableWithInitialization
/:$readableName VariableDeclarator:/
/:$recovery_template Identifier:/

EnterVariable ::= $empty
/.$putCase consumeEnterVariable(); $break ./
/:$readableName EnterVariable:/

ExitVariableWithInitialization ::= $empty
/.$putCase consumeExitVariableWithInitialization(); $break ./
/:$readableName ExitVariableWithInitialization:/

ExitVariableWithoutInitialization ::= $empty
/.$putCase consumeExitVariableWithoutInitialization(); $break ./
/:$readableName ExitVariableWithoutInitialization:/

ForceNoDiet ::= $empty
/.$putCase consumeForceNoDiet(); $break ./
/:$readableName ForceNoDiet:/
RestoreDiet ::= $empty
/.$putCase consumeRestoreDiet(); $break ./
/:$readableName RestoreDiet:/

VariableDeclaratorId ::= 'Identifier' 
/:$readableName VariableDeclaratorId:/
/:$recovery_template Identifier:/

VariableInitializer -> AssignmentExpression
/:$readableName VariableInitializer:/
/:$recovery_template Identifier:/

VariableInitializerNoIn -> AssignmentExpressionNoIn
/:$readableName VariableInitializer:/
/:$recovery_template Identifier:/

FunctionExpression ::= FunctionExpressionHeader MethodBody 
/.$putCase // set to true to consume a method with a body
  consumeFunctionExpression();  $break ./
/:$readableName FunctionExpression:/

FunctionExpressionHeader ::= FunctionExpressionHeaderName FormalParameterListopt MethodHeaderRightParen  
/.$putCase consumeMethodHeader(); $break ./
/:$readableName FunctionExpressionHeader :/

FunctionExpressionHeaderName ::= Modifiersopt 'function' 'Identifier' '('
/.$putCase consumeMethodHeaderName(false); $break ./
FunctionExpressionHeaderName ::= Modifiersopt 'function'   '('
/.$putCase consumeMethodHeaderName(true); $break ./
/:$readableName FunctionExpressionHeaderName :/

MethodDeclaration -> AbstractMethodDeclaration
MethodDeclaration ::= MethodHeader MethodBody 
/.$putCase // set to true to consume a method with a body
  consumeMethodDeclaration(true);  $break ./
/:$readableName MethodDeclaration:/

AbstractMethodDeclaration ::= MethodHeader ';'
/.$putCase // set to false to consume a method without body
  consumeMethodDeclaration(false); $break ./
/:$readableName MethodDeclaration:/

MethodHeader ::= MethodHeaderName FormalParameterListopt MethodHeaderRightParen  
/.$putCase consumeMethodHeader(); $break ./
/:$readableName MethodDeclaration:/

MethodHeaderName ::= Modifiersopt 'function' 'Identifier' '('
/.$putCase consumeMethodHeaderName(false); $break ./
/:$readableName MethodHeaderName:/

MethodHeaderRightParen ::= ')'
/.$putCase consumeMethodHeaderRightParen(); $break ./
/:$readableName ):/
/:$recovery_template ):/

FormalParameterList -> FormalParameter
FormalParameterList ::= FormalParameterList ',' FormalParameter
/.$putCase consumeFormalParameterList(); $break ./
/:$readableName FormalParameterList:/

--1.1 feature
FormalParameter ::= VariableDeclaratorId
/.$putCase consumeFormalParameter(false); $break ./

MethodBody ::= NestedMethod '{' PostDoc BlockStatementsopt '}' 
/.$putCase consumeMethodBody(); $break ./
/:$readableName MethodBody:/
-- /:$no_statements_recovery:/

NestedMethod ::= $empty
/.$putCase consumeNestedMethod(); $break ./
/:$readableName NestedMethod:/

PostDoc ::= $empty
/.$putCase consumePostDoc(); $break ./
/:$readableName PostDoc:/

PushLeftBraceObjLit ::= $empty
/.$putCase consumePushLeftBrace(); $break ./
/:$readableName PushLeftBrace:/

Block ::= OpenBlock '{' BlockStatementsopt '}'
/.$putCase consumeBlock(); $break ./
/:$readableName Block:/

OpenBlock ::= $empty
/.$putCase consumeOpenBlock() ; $break ./
/:$readableName OpenBlock:/

ProgramElements -> ProgramElement
ProgramElements ::= ProgramElements ProgramElement
/.$putCase consumeProgramElements() ; $break ./
/:$readableName ProgramElements:/

ProgramElement -> BlockStatement
/:$readableName ProgramElement:/

BlockStatements -> BlockStatement
BlockStatements ::= BlockStatements BlockStatement
/.$putCase consumeBlockStatements() ; $break ./
/:$readableName BlockStatements:/

BlockStatement -> LocalVariableDeclarationStatement
BlockStatement -> MethodDeclaration
BlockStatement -> Statement

LocalVariableDeclarationStatement ::= LocalVariableDeclaration ';'
/.$putCase consumeLocalVariableDeclarationStatement(); $break ./
/:$readableName LocalVariableDeclarationStatement:/

LocalVariableDeclaration ::= 'var' PushModifiers VariableDeclarators
/.$putCase consumeLocalVariableDeclaration(); $break ./
/:$readableName LocalVariableDeclaration:/

LocalVariableDeclarationNoIn ::= 'var' PushModifiers VariableDeclaratorsNoIn
/.$putCase consumeLocalVariableDeclaration(); $break ./
/:$readableName LocalVariableDeclaration:/

PushModifiers ::= $empty
/.$putCase consumePushModifiers(); $break ./
/:$readableName PushModifiers:/

Statement -> StatementWithoutTrailingSubstatement
Statement -> LabeledStatement
Statement -> IfThenStatement
Statement -> IfThenElseStatement
Statement -> WhileStatement
Statement -> WithStatement
Statement -> ForStatement
/:$readableName Statement:/
/:$recovery_template ;:/

StatementNoShortIf -> StatementWithoutTrailingSubstatement
StatementNoShortIf -> LabeledStatementNoShortIf
StatementNoShortIf -> IfThenElseStatementNoShortIf
StatementNoShortIf -> WhileStatementNoShortIf
StatementNoShortIf -> WithStatementNoShortIf
StatementNoShortIf -> ForStatementNoShortIf
/:$readableName Statement:/

StatementWithoutTrailingSubstatement -> Block
StatementWithoutTrailingSubstatement -> EmptyStatement
StatementWithoutTrailingSubstatement -> ExpressionStatement
StatementWithoutTrailingSubstatement -> SwitchStatement
StatementWithoutTrailingSubstatement -> DoStatement
StatementWithoutTrailingSubstatement -> BreakStatement
StatementWithoutTrailingSubstatement -> ContinueStatement
StatementWithoutTrailingSubstatement -> ReturnStatement
StatementWithoutTrailingSubstatement -> ThrowStatement
StatementWithoutTrailingSubstatement -> TryStatement
/:$readableName Statement:/

EmptyStatement ::= ';'
/.$putCase consumeEmptyStatement(); $break ./
/:$readableName EmptyStatement:/
/:$empty_statement:/

LabeledStatement ::= Label ':' Statement
/.$putCase consumeStatementLabel() ; $break ./
/:$readableName LabeledStatement:/

LabeledStatementNoShortIf ::= Label ':' StatementNoShortIf
/.$putCase consumeStatementLabel() ; $break ./
/:$readableName LabeledStatement:/

Label ::= 'Identifier'
/.$putCase consumeLabel() ; $break ./
/:$readableName Label:/

ExpressionStatement ::= StatementExpression ';'
/. $putCase consumeExpressionStatement(); $break ./
-- /:$readableName Statement:/

StatementExpression ::= ListExpressionStmt
/:$readableName Expression:/

IfThenStatement ::=  'if' '(' Expression ')' BlockStatement
/.$putCase consumeStatementIfNoElse(); $break ./
/:$readableName IfStatement:/

IfThenElseStatement ::=  'if' '(' Expression ')' StatementNoShortIf 'else' BlockStatement
/.$putCase consumeStatementIfWithElse(); $break ./
/:$readableName IfStatement:/

IfThenElseStatementNoShortIf ::=  'if' '(' Expression ')' StatementNoShortIf 'else' StatementNoShortIf
/.$putCase consumeStatementIfWithElse(); $break ./
/:$readableName IfStatement:/

IfThenElseStatement ::=  'if' '(' Expression ')' LocalVariableDeclarationStatement 'else' BlockStatement
/.$putCase consumeStatementIfWithElse(); $break ./
/:$readableName IfStatement:/

IfThenElseStatementNoShortIf ::=  'if' '(' Expression ')' LocalVariableDeclarationStatement 'else' LocalVariableDeclarationStatement
/.$putCase consumeStatementIfWithElse(); $break ./
/:$readableName IfStatement:/

SwitchStatement ::= 'switch' '(' Expression ')' OpenBlock SwitchBlock
/.$putCase consumeStatementSwitch() ; $break ./
/:$readableName SwitchStatement:/

SwitchBlock ::= '{' '}'
/.$putCase consumeEmptySwitchBlock() ; $break ./

SwitchBlock ::= '{' SwitchBlockStatements '}'
SwitchBlock ::= '{' SwitchLabels '}'
SwitchBlock ::= '{' SwitchBlockStatements SwitchLabels '}'
/.$putCase consumeSwitchBlock() ; $break ./
/:$readableName SwitchBlock:/

SwitchBlockStatements -> SwitchBlockStatement
SwitchBlockStatements ::= SwitchBlockStatements SwitchBlockStatement
/.$putCase consumeSwitchBlockStatements() ; $break ./
/:$readableName SwitchBlockStatements:/

SwitchBlockStatement ::= SwitchLabels BlockStatements
/.$putCase consumeSwitchBlockStatement() ; $break ./
/:$readableName SwitchBlockStatement:/

SwitchLabels -> SwitchLabel
SwitchLabels ::= SwitchLabels SwitchLabel
/.$putCase consumeSwitchLabels() ; $break ./
/:$readableName SwitchLabels:/

SwitchLabel ::= 'case' ConstantExpression ':'
/. $putCase consumeCaseLabel(); $break ./

SwitchLabel ::= 'default' ':'
/. $putCase consumeDefaultLabel(); $break ./
/:$readableName SwitchLabel:/

WhileStatement ::= 'while' '(' Expression ')' Statement
/.$putCase consumeStatementWhile() ; $break ./
/:$readableName WhileStatement:/

WhileStatementNoShortIf ::= 'while' '(' Expression ')' StatementNoShortIf
/.$putCase consumeStatementWhile() ; $break ./
/:$readableName WhileStatement:/

WithStatement ::= 'with' '(' Expression ')' Statement
/.$putCase consumeStatementWith() ; $break ./
/:$readableName WithStatement:/

WithStatementNoShortIf ::= 'with' '(' Expression ')' StatementNoShortIf
/.$putCase consumeStatementWith() ; $break ./
/:$readableName WithStatementNoShortIf:/

DoStatement ::= 'do' Statement 'while' '(' Expression ')' ';'
/.$putCase consumeStatementDo() ; $break ./
/:$readableName DoStatement:/

ForStatement ::= 'for' '(' ForInitopt ';' Expressionopt ';' ForUpdateopt ')' Statement
/.$putCase consumeStatementFor() ; $break ./
/:$readableName ForStatement:/

ForStatement ::= 'for' '(' ForInInit 'in' Expression  ')' Statement
/.$putCase consumeStatementForIn() ; $break ./
/:$readableName ForStatement:/

ForStatementNoShortIf ::= 'for' '(' ForInitopt ';' Expressionopt ';' ForUpdateopt ')' StatementNoShortIf
/.$putCase consumeStatementFor() ; $break ./
/:$readableName ForStatement:/

ForStatementNoShortIf ::= 'for' '(' ForInInit 'in' Expression  ')' StatementNoShortIf
/.$putCase consumeStatementForIn() ; $break ./
/:$readableName ForStatement:/


-- SHOULD BE 'FullPostfixExpression', but that causes shift/reduce conflict
ForInInit ::= SimpleName
/.$putCase consumeForInInit() ; $break ./
ForInInit -> LocalVariableDeclarationNoIn
/:$readableName ForInit:/

--the minus one allows to avoid a stack-to-stack transfer
ForInit ::= StatementExpressionListNoIn
/.$putCase consumeForInit() ; $break ./
ForInit -> LocalVariableDeclarationNoIn
/:$readableName ForInit:/

ForUpdate -> StatementExpressionList
/:$readableName ForUpdate:/

StatementExpressionList -> AssignmentExpression
StatementExpressionList ::= StatementExpressionList ',' AssignmentExpression
/.$putCase consumeStatementExpressionList() ; $break ./
/:$readableName StatementExpressionList:/

StatementExpressionListNoIn -> AssignmentExpressionNoIn
StatementExpressionListNoIn ::= StatementExpressionListNoIn ',' AssignmentExpressionNoIn
/.$putCase consumeStatementExpressionList() ; $break ./
/:$readableName StatementExpressionList:/

BreakStatement ::= 'break' ';'
/.$putCase consumeStatementBreak() ; $break ./

BreakStatement ::= 'break' Identifier ';'
/.$putCase consumeStatementBreakWithLabel() ; $break ./
/:$readableName BreakStatement:/

ContinueStatement ::= 'continue' ';'
/.$putCase consumeStatementContinue() ; $break ./

ContinueStatement ::= 'continue' Identifier ';'
/.$putCase consumeStatementContinueWithLabel() ; $break ./
/:$readableName ContinueStatement:/

ReturnStatement ::= 'return' Expressionopt ';'
/.$putCase consumeStatementReturn() ; $break ./
/:$readableName ReturnStatement:/

ThrowStatement ::= 'throw' Expression ';'
/.$putCase consumeStatementThrow(); $break ./
/:$readableName ThrowStatement:/

TryStatement ::= 'try' TryBlock Catches
/.$putCase consumeStatementTry(false); $break ./
TryStatement ::= 'try' TryBlock Catchesopt Finally
/.$putCase consumeStatementTry(true); $break ./
/:$readableName TryStatement:/

TryBlock ::= Block ExitTryBlock
/:$readableName Block:/

ExitTryBlock ::= $empty
/.$putCase consumeExitTryBlock(); $break ./
/:$readableName ExitTryBlock:/

Catches -> CatchClause
Catches ::= Catches CatchClause
/.$putCase consumeCatches(); $break ./
/:$readableName Catches:/

CatchClause ::= 'catch' '(' FormalParameter ')'    Block
/.$putCase consumeStatementCatch() ; $break ./
/:$readableName CatchClause:/

Finally ::= 'finally'    Block
/:$readableName Finally:/

--18.12 Productions from 14: Expressions

--for source positionning purpose
PushLPAREN ::= '('
/.$putCase consumeLeftParen(); $break ./
/:$readableName (:/
/:$recovery_template (:/
PushRPAREN ::= ')'
/.$putCase consumeRightParen(); $break ./
/:$readableName ):/
/:$recovery_template ):/

Primary -> PrimaryNoNewArray
Primary -> ArrayLiteral
Primary -> ObjectLiteral
Primary	-> 	FunctionExpression
/:$readableName Primary:/

PrimaryNoNewArray -> Literal
PrimaryNoNewArray ::= 'this'
/.$putCase consumePrimaryNoNewArrayThis(); $break ./

PrimaryNoNewArray ::=  PushLPAREN AssignmentExpression PushRPAREN 
/.$putCase consumePrimaryNoNewArray(); $break ./

ObjectLiteral ::=	'{'PushLeftBraceObjLit FieldList '}'
/.$putCase consumeObjectLiteral(); $break ./
/:$readableName ObjectLiteral:/
	
FieldList	::= $empty
/.$putCase consumeEmptyFieldList(); $break ./
FieldList	-> NonemptyFieldList
/:$readableName FieldList:/
	
NonemptyFieldList	-> LiteralField
NonemptyFieldList	::= NonemptyFieldList	',' LiteralField 
/.$putCase consumeFieldList(); $break ./
/:$readableName NonemptyFieldList:/
	
LiteralField ::=	FieldName ':' AssignmentExpression
/.$putCase consumeLiteralField(); $break ./
/:$readableName LiteralField:/
	
FieldName	::= SimpleName
/.$putCase consumeFieldNameSimple(); $break ./
FieldName	-> StringLiteral
FieldName	-> CharacterLiteral
FieldName	-> IntegerLiteral
/:$readableName FieldName:/
	
ArrayLiteral ::= 	ArrayLiteralHeader  ElisionOpt ']'
/.$putCase comsumeArrayLiteral(false); $break ./
	
ArrayLiteral ::= 	ArrayLiteralHeader  ArrayLiteralElementList ']'
/.$putCase comsumeArrayLiteral(false); $break ./
/:$readableName ArrayLiteral:/
	
ArrayLiteral ::= 	ArrayLiteralHeader  ArrayLiteralElementList ',' ElisionOpt  ']'
/.$putCase comsumeArrayLiteral(true); $break ./
/:$readableName ArrayLiteral:/

ArrayLiteralHeader ::= 	'[' 
/.$putCase comsumeArrayLiteralHeader(); $break ./

ElisionOpt ::= $empty 
/.$putCase comsumeElisionEmpty(); $break ./
ElisionOpt -> Elision

Elision ::= ','
/.$putCase comsumeElisionOne(); $break ./
Elision ::= Elision ','
/.$putCase comsumeElisionList(); $break ./
 
ArrayLiteralElementList ::= ElisionOpt ArrayLiteralElement
/.$putCase comsumeArrayLiteralListOne(); $break ./
ArrayLiteralElementList ::=	ArrayLiteralElementList ',' ElisionOpt ArrayLiteralElement
/.$putCase comsumeArrayLiteralList(); $break ./
	
ArrayLiteralElement ::=	AssignmentExpression
/.$putCase comsumeArrayLiteralElement(); $break ./
/:$readableName ArrayLiteralElement:/

FullNewExpression	::= 'new' FullNewSubexpression '(' ArgumentListopt ')'
/.$putCase consumeFullNewExpression(); $break ./
/:$readableName FullNewExpression:/
	
FullNewSubexpression ->	Primary
FullNewSubexpression ::=	SimpleName
/.$putCase consumeFullNewSubexpressionSimpleName (); $break ./
FullNewSubexpression ->	FullNewExpression
FullNewSubexpression ::=	FullNewSubexpression PropertyOperator
/.$putCase consumeFullNewSubexpressionPropertyOperator(); $break ./
/:$readableName FullNewSubexpression:/
	
ShortNewExpression ::= 'new' ShortNewSubexpression
/.$putCase comsumeShortNewSubexpression(); $break ./
/:$readableName ShortNewExpression:/
	
	
ShortNewSubexpression -> FullNewSubexpression
ShortNewSubexpression -> ShortNewExpression
/:$readableName ShortNewSubexpression:/
	
PropertyOperator ::= '.' SimpleName
/.$putCase consumePropertyOperator() ; $break ./
PropertyOperator -> Brackets 
/.$putCase consumePropertyOperatorBrackets() ; $break ./
/:$readableName PropertyOperator:/

Brackets ::= '[' Expression ']'
/.$putCase consumeListExpressionBrackets() ; $break ./
/:$readableName Brackets:/


ListExpression -> AssignmentExpression
ListExpression ::= ListExpression ',' AssignmentExpression
/.$putCase consumeListExpression(); $break ./
/:$readableName ListExpression:/

ListExpressionStmt -> AssignmentExpressionStmt
ListExpressionStmt ::= ListExpressionStmt ',' AssignmentExpressionStmt
/.$putCase consumeListExpression(); $break ./
/:$readableName ListExpression:/


ArgumentList ::= AssignmentExpression
ArgumentList ::= ArgumentList ',' AssignmentExpression
/.$putCase consumeArgumentList(); $break ./
/:$readableName ArgumentList:/

MethodInvocation ::= FullPostfixExpression '(' ArgumentListopt ')'
/.$putCase consumeMethodInvocationPrimary(); $break ./
/:$readableName MethodInvocation:/

PostfixExpression	->	FullPostfixExpression
PostfixExpression	->	ShortNewExpression

FullPostfixExpression ->	Primary
-- used identifier instead FullPostfixExpression ->	ExpressionQualifiedIdentifier
FullPostfixExpression ::=	SimpleName
/.$putCase consumePostfixExpression(); $break ./
FullPostfixExpression ->	FullNewExpression
FullPostfixExpression ::=	FullPostfixExpression PropertyOperator
/.$putCase consumeFullPropertyOperator(); $break ./
FullPostfixExpression ::=	MethodInvocation 
/.$putCase consumeMethodInvocation(); $break ./
FullPostfixExpression -> PostIncrementExpression
FullPostfixExpression -> PostDecrementExpression
-- /:$readableName FullPostfixExpression:/

PostIncrementExpression ::= PostfixExpression '++'
/.$putCase consumeUnaryExpression(OperatorIds.PLUS,true); $break ./
/:$readableName PostIncrementExpression:/

PostDecrementExpression ::= PostfixExpression '--'
/.$putCase consumeUnaryExpression(OperatorIds.MINUS,true); $break ./
/:$readableName PostDecrementExpression:/

--for source managment purpose
PushPosition ::= $empty
 /.$putCase consumePushPosition(); $break ./
/:$readableName PushPosition:/

UnaryExpression -> PreIncrementExpression
UnaryExpression -> PreDecrementExpression
UnaryExpression ::= '+' PushPosition UnaryExpression
/.$putCase consumeUnaryExpression(OperatorIds.PLUS); $break ./
UnaryExpression ::= '-' PushPosition UnaryExpression
/.$putCase consumeUnaryExpression(OperatorIds.MINUS); $break ./
UnaryExpression -> UnaryExpressionNotPlusMinus
/:$readableName Expression:/

PreIncrementExpression ::= '++' PushPosition UnaryExpression
/.$putCase consumeUnaryExpression(OperatorIds.PLUS,false); $break ./
/:$readableName PreIncrementExpression:/

PreDecrementExpression ::= '--' PushPosition UnaryExpression
/.$putCase consumeUnaryExpression(OperatorIds.MINUS,false); $break ./
/:$readableName PreDecrementExpression:/

UnaryExpressionNotPlusMinus -> PostfixExpression
UnaryExpressionNotPlusMinus ::= '~' PushPosition UnaryExpression
/.$putCase consumeUnaryExpression(OperatorIds.TWIDDLE); $break ./
UnaryExpressionNotPlusMinus ::= '!' PushPosition UnaryExpression
/.$putCase consumeUnaryExpression(OperatorIds.NOT); $break ./
UnaryExpressionNotPlusMinus ::= 'delete' PushPosition UnaryExpression
/.$putCase consumeUnaryExpression(OperatorIds.DELETE); $break ./
UnaryExpressionNotPlusMinus ::= 'void' PushPosition UnaryExpression
/.$putCase consumeUnaryExpression(OperatorIds.VOID); $break ./
UnaryExpressionNotPlusMinus ::= 'typeof' PushPosition UnaryExpression
/.$putCase consumeUnaryExpression(OperatorIds.TYPEOF); $break ./
/:$readableName Expression:/

MultiplicativeExpression -> UnaryExpression
MultiplicativeExpression ::= MultiplicativeExpression '*' UnaryExpression
/.$putCase consumeBinaryExpression(OperatorIds.MULTIPLY); $break ./
MultiplicativeExpression ::= MultiplicativeExpression '/' UnaryExpression
/.$putCase consumeBinaryExpression(OperatorIds.DIVIDE); $break ./
MultiplicativeExpression ::= MultiplicativeExpression '%' UnaryExpression
/.$putCase consumeBinaryExpression(OperatorIds.REMAINDER); $break ./
/:$readableName Expression:/

AdditiveExpression -> MultiplicativeExpression
AdditiveExpression ::= AdditiveExpression '+' MultiplicativeExpression
/.$putCase consumeBinaryExpression(OperatorIds.PLUS); $break ./
AdditiveExpression ::= AdditiveExpression '-' MultiplicativeExpression
/.$putCase consumeBinaryExpression(OperatorIds.MINUS); $break ./
/:$readableName Expression:/

ShiftExpression -> AdditiveExpression
ShiftExpression ::= ShiftExpression '<<'  AdditiveExpression
/.$putCase consumeBinaryExpression(OperatorIds.LEFT_SHIFT); $break ./
ShiftExpression ::= ShiftExpression '>>'  AdditiveExpression
/.$putCase consumeBinaryExpression(OperatorIds.RIGHT_SHIFT); $break ./
ShiftExpression ::= ShiftExpression '>>>' AdditiveExpression
/.$putCase consumeBinaryExpression(OperatorIds.UNSIGNED_RIGHT_SHIFT); $break ./
/:$readableName Expression:/

--
--RelationalExpression 
--
RelationalExpression -> ShiftExpression
RelationalExpression ::= RelationalExpression '<'  ShiftExpression
/.$putCase consumeBinaryExpression(OperatorIds.LESS); $break ./
RelationalExpression ::= RelationalExpression '>'  ShiftExpression
/.$putCase consumeBinaryExpression(OperatorIds.GREATER); $break ./
RelationalExpression ::= RelationalExpression '<=' ShiftExpression
/.$putCase consumeBinaryExpression(OperatorIds.LESS_EQUAL); $break ./
RelationalExpression ::= RelationalExpression '>=' ShiftExpression
/.$putCase consumeBinaryExpression(OperatorIds.GREATER_EQUAL); $break ./
RelationalExpression ::= RelationalExpression 'instanceof' ShiftExpression
/.$putCase consumeBinaryExpression(OperatorIds.INSTANCEOF); $break ./
RelationalExpression ::= RelationalExpression 'in' ShiftExpression
/.$putCase consumeBinaryExpression(OperatorIds.IN); $break ./
/:$readableName Expression:/

RelationalExpressionNoIn -> ShiftExpression
RelationalExpressionNoIn ::= RelationalExpressionNoIn '<'  ShiftExpression
/.$putCase consumeBinaryExpression(OperatorIds.LESS); $break ./
RelationalExpressionNoIn ::= RelationalExpressionNoIn '>'  ShiftExpression
/.$putCase consumeBinaryExpression(OperatorIds.GREATER); $break ./
RelationalExpressionNoIn ::= RelationalExpressionNoIn '<=' ShiftExpression
/.$putCase consumeBinaryExpression(OperatorIds.LESS_EQUAL); $break ./
RelationalExpressionNoIn ::= RelationalExpressionNoIn '>=' ShiftExpression
/.$putCase consumeBinaryExpression(OperatorIds.GREATER_EQUAL); $break ./
RelationalExpressionNoIn ::= RelationalExpressionNoIn 'instanceof' ShiftExpression
/.$putCase consumeBinaryExpression(OperatorIds.INSTANCEOF); $break ./
/:$readableName Expression:/

--
--EqualityExpression
--
EqualityExpression -> RelationalExpression 
EqualityExpression ::= EqualityExpression '==' RelationalExpression
/.$putCase consumeEqualityExpression(OperatorIds.EQUAL_EQUAL); $break ./
EqualityExpression ::= EqualityExpression '!=' RelationalExpression
/.$putCase consumeEqualityExpression(OperatorIds.NOT_EQUAL); $break ./
EqualityExpression ::= EqualityExpression '===' RelationalExpression
/.$putCase consumeEqualityExpression(OperatorIds.EQUAL_EQUAL_EQUAL); $break ./
EqualityExpression ::= EqualityExpression '!==' RelationalExpression
/.$putCase consumeEqualityExpression(OperatorIds.NOT_EQUAL_EQUAL); $break ./
/:$readableName Expression:/

EqualityExpressionNoIn -> RelationalExpressionNoIn 
EqualityExpressionNoIn ::= EqualityExpressionNoIn '==' RelationalExpressionNoIn
/.$putCase consumeEqualityExpression(OperatorIds.EQUAL_EQUAL); $break ./
EqualityExpressionNoIn ::= EqualityExpressionNoIn '!=' RelationalExpressionNoIn
/.$putCase consumeEqualityExpression(OperatorIds.NOT_EQUAL); $break ./
EqualityExpressionNoIn ::= EqualityExpressionNoIn '===' RelationalExpressionNoIn
/.$putCase consumeEqualityExpression(OperatorIds.EQUAL_EQUAL_EQUAL); $break ./
EqualityExpressionNoIn ::= EqualityExpressionNoIn '!==' RelationalExpressionNoIn
/.$putCase consumeEqualityExpression(OperatorIds.NOT_EQUAL_EQUAL); $break ./
/:$readableName Expression:/

--
--AndExpression 
--
AndExpression -> EqualityExpression
AndExpression ::= AndExpression '&' EqualityExpression
/.$putCase consumeBinaryExpression(OperatorIds.AND); $break ./
/:$readableName Expression:/

AndExpressionNoIn -> EqualityExpressionNoIn
AndExpressionNoIn ::= AndExpressionNoIn '&' EqualityExpressionNoIn
/.$putCase consumeBinaryExpression(OperatorIds.AND); $break ./
/:$readableName Expression:/

--
--ExclusiveOrExpression 
--
ExclusiveOrExpression -> AndExpression
ExclusiveOrExpression ::= ExclusiveOrExpression '^' AndExpression
/.$putCase consumeBinaryExpression(OperatorIds.XOR); $break ./
/:$readableName Expression:/

ExclusiveOrExpressionNoIn -> AndExpressionNoIn
ExclusiveOrExpressionNoIn ::= ExclusiveOrExpressionNoIn '^' AndExpressionNoIn
/.$putCase consumeBinaryExpression(OperatorIds.XOR); $break ./
/:$readableName Expression:/

--
--InclusiveOrExpression 
--
InclusiveOrExpression -> ExclusiveOrExpression
InclusiveOrExpression ::= InclusiveOrExpression '|' ExclusiveOrExpression
/.$putCase consumeBinaryExpression(OperatorIds.OR); $break ./
/:$readableName Expression:/

InclusiveOrExpressionNoIn -> ExclusiveOrExpressionNoIn
InclusiveOrExpressionNoIn ::= InclusiveOrExpressionNoIn '|' ExclusiveOrExpressionNoIn
/.$putCase consumeBinaryExpression(OperatorIds.OR); $break ./
/:$readableName Expression:/

--
--ConditionalAndExpression 
--
ConditionalAndExpression -> InclusiveOrExpression
ConditionalAndExpression ::= ConditionalAndExpression '&&' InclusiveOrExpression
/.$putCase consumeBinaryExpression(OperatorIds.AND_AND); $break ./
/:$readableName Expression:/

ConditionalAndExpressionNoIn -> InclusiveOrExpressionNoIn
ConditionalAndExpressionNoIn ::= ConditionalAndExpressionNoIn '&&' InclusiveOrExpressionNoIn
/.$putCase consumeBinaryExpression(OperatorIds.AND_AND); $break ./
/:$readableName Expression:/

--
--ConditionalOrExpression 
--
ConditionalOrExpression -> ConditionalAndExpression
ConditionalOrExpression ::= ConditionalOrExpression '||' ConditionalAndExpression
/.$putCase consumeBinaryExpression(OperatorIds.OR_OR); $break ./
/:$readableName Expression:/

ConditionalOrExpressionNoIn -> ConditionalAndExpressionNoIn
ConditionalOrExpressionNoIn ::= ConditionalOrExpressionNoIn '||' ConditionalAndExpressionNoIn
/.$putCase consumeBinaryExpression(OperatorIds.OR_OR); $break ./
/:$readableName Expression:/

--
--ConditionalExpression 
--
ConditionalExpression -> ConditionalOrExpression
ConditionalExpression ::= ConditionalOrExpression '?' AssignmentExpression ':' AssignmentExpression 
/.$putCase consumeConditionalExpression(OperatorIds.QUESTIONCOLON) ; $break ./
/:$readableName Expression:/

ConditionalExpressionNoIn -> ConditionalOrExpressionNoIn
ConditionalExpressionNoIn ::= ConditionalOrExpressionNoIn '?' AssignmentExpressionNoIn ':' AssignmentExpressionNoIn 
/.$putCase consumeConditionalExpression(OperatorIds.QUESTIONCOLON) ; $break ./
/:$readableName Expression:/

--
--AssignmentExpression 
--
AssignmentExpression -> ConditionalExpression
AssignmentExpression -> Assignment
/:$readableName Expression:/
/:$recovery_template Identifier:/

AssignmentExpressionNoIn -> ConditionalExpressionNoIn
AssignmentExpressionNoIn -> AssignmentNoIn
/:$readableName Expression:/
/:$recovery_template Identifier:/

--
--Assignment
--
Assignment ::= PostfixExpression AssignmentOperator AssignmentExpression
/.$putCase consumeAssignment(); $break ./
/:$readableName Assignment:/

AssignmentNoIn ::= PostfixExpression AssignmentOperator AssignmentExpressionNoIn
/.$putCase consumeAssignment(); $break ./
/:$readableName Assignment:/

AssignmentOperator ::= '='
/.$putCase consumeAssignmentOperator(EQUAL); $break ./
AssignmentOperator ::= '*='
/.$putCase consumeAssignmentOperator(MULTIPLY); $break ./
AssignmentOperator ::= '/='
/.$putCase consumeAssignmentOperator(DIVIDE); $break ./
AssignmentOperator ::= '%='
/.$putCase consumeAssignmentOperator(REMAINDER); $break ./
AssignmentOperator ::= '+='
/.$putCase consumeAssignmentOperator(PLUS); $break ./
AssignmentOperator ::= '-='
/.$putCase consumeAssignmentOperator(MINUS); $break ./
AssignmentOperator ::= '<<='
/.$putCase consumeAssignmentOperator(LEFT_SHIFT); $break ./
AssignmentOperator ::= '>>='
/.$putCase consumeAssignmentOperator(RIGHT_SHIFT); $break ./
AssignmentOperator ::= '>>>='
/.$putCase consumeAssignmentOperator(UNSIGNED_RIGHT_SHIFT); $break ./
AssignmentOperator ::= '&='
/.$putCase consumeAssignmentOperator(AND); $break ./
AssignmentOperator ::= '^='
/.$putCase consumeAssignmentOperator(XOR); $break ./
AssignmentOperator ::= '|='
/.$putCase consumeAssignmentOperator(OR); $break ./
/:$readableName AssignmentOperator:/
/:$recovery_template =:/

Expression -> ListExpression
/:$readableName Expression:/
/:$recovery_template Identifier:/

-- The following rules are for optional nonterminals.
Expressionopt ::= $empty
/.$putCase consumeEmptyExpression(); $break ./
Expressionopt -> Expression
/:$readableName Expression:/

ConstantExpression -> Expression
/:$readableName ConstantExpression:/

--
-- Disambigate OpenBlock and ObjectLiteral
--
PrimaryStmt -> PrimaryNoNewArrayStmt
PrimaryStmt -> ArrayLiteral
/:$readableName PrimaryStmt:/

PrimaryNoNewArrayStmt -> Literal
PrimaryNoNewArrayStmt ::= 'this'
/.$putCase consumePrimaryNoNewArrayThis(); $break ./

PrimaryNoNewArrayStmt ::=  PushLPAREN AssignmentExpression PushRPAREN 
/.$putCase consumePrimaryNoNewArray(); $break ./

 


FullNewExpressionStmt	::= 'new' FullNewSubexpression '(' ArgumentListopt ')'
/.$putCase consumeFullNewExpression(); $break ./
/:$readableName FullNewExpression:/
	
ShortNewExpressionStmt ::= 'new' ShortNewSubexpression
/.$putCase comsumeShortNewSubexpression(); $break ./
/:$readableName ShortNewExpression:/
	
MethodInvocationStmt ::= FullPostfixExpressionStmt '(' ArgumentListopt ')'
/.$putCase consumeMethodInvocationPrimary(); $break ./
/:$readableName MethodInvocation:/

PostfixExpressionStmt	->	FullPostfixExpressionStmt
PostfixExpressionStmt	->	ShortNewExpressionStmt

 

FullPostfixExpressionStmt ->	PrimaryStmt
-- used identifier instead FullPostfixExpression ->	ExpressionQualifiedIdentifier
FullPostfixExpressionStmt ::=	SimpleName
/.$putCase consumePostfixExpression(); $break ./
FullPostfixExpressionStmt ->	FullNewExpressionStmt
FullPostfixExpressionStmt ::=	FullPostfixExpressionStmt PropertyOperator
/.$putCase consumeFullPropertyOperator(); $break ./
FullPostfixExpressionStmt ::=	MethodInvocationStmt 
/.$putCase consumeMethodInvocation(); $break ./
FullPostfixExpressionStmt -> PostIncrementExpressionStmt
FullPostfixExpressionStmt -> PostDecrementExpressionStmt
-- /:$readableName FullPostfixExpression:/



PostIncrementExpressionStmt ::= PostfixExpressionStmt '++'
/.$putCase consumeUnaryExpression(OperatorIds.PLUS,true); $break ./
/:$readableName PostIncrementExpression:/

PostDecrementExpressionStmt ::= PostfixExpressionStmt '--'
/.$putCase consumeUnaryExpression(OperatorIds.MINUS,true); $break ./
/:$readableName PostDecrementExpression:/

UnaryExpressionStmt -> PreIncrementExpression
UnaryExpressionStmt -> PreDecrementExpression
UnaryExpressionStmt ::= '+' PushPosition UnaryExpression
/.$putCase consumeUnaryExpression(OperatorIds.PLUS); $break ./
UnaryExpressionStmt ::= '-' PushPosition UnaryExpression
/.$putCase consumeUnaryExpression(OperatorIds.MINUS); $break ./
UnaryExpressionStmt -> UnaryExpressionNotPlusMinusStmt
/:$readableName Expression:/

UnaryExpressionNotPlusMinusStmt -> PostfixExpressionStmt
UnaryExpressionNotPlusMinusStmt ::= '~' PushPosition UnaryExpression
/.$putCase consumeUnaryExpression(OperatorIds.TWIDDLE); $break ./
UnaryExpressionNotPlusMinusStmt ::= '!' PushPosition UnaryExpression
/.$putCase consumeUnaryExpression(OperatorIds.NOT); $break ./
UnaryExpressionNotPlusMinusStmt ::= 'delete' PushPosition UnaryExpression
/.$putCase consumeUnaryExpression(OperatorIds.DELETE); $break ./
UnaryExpressionNotPlusMinusStmt ::= 'void' PushPosition UnaryExpression
/.$putCase consumeUnaryExpression(OperatorIds.VOID); $break ./
UnaryExpressionNotPlusMinusStmt ::= 'typeof' PushPosition UnaryExpression
/.$putCase consumeUnaryExpression(OperatorIds.TYPEOF); $break ./
-- UnaryExpressionNotPlusMinus -> CastExpression
/:$readableName Expression:/


MultiplicativeExpressionStmt -> UnaryExpressionStmt
MultiplicativeExpressionStmt ::= MultiplicativeExpressionStmt '*' UnaryExpression
/.$putCase consumeBinaryExpression(OperatorIds.MULTIPLY); $break ./
MultiplicativeExpressionStmt ::= MultiplicativeExpressionStmt '/' UnaryExpression
/.$putCase consumeBinaryExpression(OperatorIds.DIVIDE); $break ./
MultiplicativeExpressionStmt ::= MultiplicativeExpressionStmt '%' UnaryExpression
/.$putCase consumeBinaryExpression(OperatorIds.REMAINDER); $break ./
/:$readableName Expression:/

AdditiveExpressionStmt -> MultiplicativeExpressionStmt
AdditiveExpressionStmt ::= AdditiveExpressionStmt '+' MultiplicativeExpression
/.$putCase consumeBinaryExpression(OperatorIds.PLUS); $break ./
AdditiveExpressionStmt ::= AdditiveExpressionStmt '-' MultiplicativeExpression
/.$putCase consumeBinaryExpression(OperatorIds.MINUS); $break ./
/:$readableName Expression:/

ShiftExpressionStmt -> AdditiveExpressionStmt
ShiftExpressionStmt ::= ShiftExpressionStmt '<<'  AdditiveExpression
/.$putCase consumeBinaryExpression(OperatorIds.LEFT_SHIFT); $break ./
ShiftExpressionStmt ::= ShiftExpressionStmt '>>'  AdditiveExpression
/.$putCase consumeBinaryExpression(OperatorIds.RIGHT_SHIFT); $break ./
ShiftExpressionStmt ::= ShiftExpressionStmt '>>>' AdditiveExpression
/.$putCase consumeBinaryExpression(OperatorIds.UNSIGNED_RIGHT_SHIFT); $break ./
/:$readableName Expression:/

RelationalExpressionStmt -> ShiftExpressionStmt
RelationalExpressionStmt ::= RelationalExpressionStmt '<'  ShiftExpression
/.$putCase consumeBinaryExpression(OperatorIds.LESS); $break ./
RelationalExpressionStmt ::= RelationalExpressionStmt '>'  ShiftExpression
/.$putCase consumeBinaryExpression(OperatorIds.GREATER); $break ./
RelationalExpressionStmt ::= RelationalExpressionStmt '<=' ShiftExpression
/.$putCase consumeBinaryExpression(OperatorIds.LESS_EQUAL); $break ./
RelationalExpressionStmt ::= RelationalExpressionStmt '>=' ShiftExpression
/.$putCase consumeBinaryExpression(OperatorIds.GREATER_EQUAL); $break ./
RelationalExpressionStmt ::= RelationalExpressionStmt 'instanceof' ShiftExpression
/.$putCase consumeBinaryExpression(OperatorIds.INSTANCEOF); $break ./
RelationalExpressionStmt ::= RelationalExpressionStmt 'in' ShiftExpression
/.$putCase consumeBinaryExpression(OperatorIds.IN); $break ./
/:$readableName Expression:/

EqualityExpressionStmt -> RelationalExpressionStmt 
EqualityExpressionStmt ::= EqualityExpressionStmt '==' RelationalExpression
/.$putCase consumeEqualityExpression(OperatorIds.EQUAL_EQUAL); $break ./
EqualityExpressionStmt ::= EqualityExpressionStmt '!=' RelationalExpression
/.$putCase consumeEqualityExpression(OperatorIds.NOT_EQUAL); $break ./
EqualityExpressionStmt ::= EqualityExpressionStmt '===' RelationalExpression
/.$putCase consumeEqualityExpression(OperatorIds.EQUAL_EQUAL_EQUAL); $break ./
EqualityExpressionStmt ::= EqualityExpressionStmt '!==' RelationalExpression
/.$putCase consumeEqualityExpression(OperatorIds.NOT_EQUAL_EQUAL); $break ./
/:$readableName Expression:/

AndExpressionStmt -> EqualityExpressionStmt
AndExpressionStmt ::= AndExpressionStmt '&' EqualityExpression
/.$putCase consumeBinaryExpression(OperatorIds.AND); $break ./
/:$readableName Expression:/

ExclusiveOrExpressionStmt -> AndExpressionStmt
ExclusiveOrExpressionStmt ::= ExclusiveOrExpressionStmt '^' AndExpression
/.$putCase consumeBinaryExpression(OperatorIds.XOR); $break ./
/:$readableName Expression:/

InclusiveOrExpressionStmt -> ExclusiveOrExpressionStmt
InclusiveOrExpressionStmt ::= InclusiveOrExpressionStmt '|' ExclusiveOrExpression
/.$putCase consumeBinaryExpression(OperatorIds.OR); $break ./
/:$readableName Expression:/

ConditionalAndExpressionStmt -> InclusiveOrExpressionStmt
ConditionalAndExpressionStmt ::= ConditionalAndExpressionStmt '&&' InclusiveOrExpression
/.$putCase consumeBinaryExpression(OperatorIds.AND_AND); $break ./
/:$readableName Expression:/

ConditionalOrExpressionStmt -> ConditionalAndExpressionStmt
ConditionalOrExpressionStmt ::= ConditionalOrExpressionStmt '||' ConditionalAndExpression
/.$putCase consumeBinaryExpression(OperatorIds.OR_OR); $break ./
/:$readableName Expression:/

ConditionalExpressionStmt -> ConditionalOrExpressionStmt
ConditionalExpressionStmt ::= ConditionalOrExpressionStmt '?' AssignmentExpressionStmt ':' AssignmentExpressionStmt
/.$putCase consumeConditionalExpression(OperatorIds.QUESTIONCOLON) ; $break ./
/:$readableName Expression:/

AssignmentExpressionStmt -> ConditionalExpressionStmt
AssignmentExpressionStmt -> AssignmentStmt
/:$readableName Expression:/
/:$recovery_template Identifier:/

AssignmentStmt ::= PostfixExpressionStmt AssignmentOperator AssignmentExpression
/.$putCase consumeAssignment(); $break ./
/:$readableName AssignmentStmt:/

Modifiersopt ::= $empty 
/. $putCase consumeDefaultModifiers(); $break ./
/:$readableName Modifiers:/

BlockStatementsopt ::= $empty
/.$putCase consumeEmptyBlockStatementsopt(); $break ./
BlockStatementsopt -> BlockStatements
/:$readableName BlockStatements:/

ArgumentListopt ::= $empty
/. $putCase consumeEmptyArgumentListopt(); $break ./
ArgumentListopt -> ArgumentList
/:$readableName ArgumentList:/

FormalParameterListopt ::= $empty
/.$putcase consumeFormalParameterListopt(); $break ./
FormalParameterListopt -> FormalParameterList
/:$readableName FormalParameterList:/

ForInitopt ::= $empty
/. $putCase consumeEmptyForInitopt(); $break ./
ForInitopt -> ForInit
/:$readableName ForInit:/

ForUpdateopt ::= $empty
/. $putCase consumeEmptyForUpdateopt(); $break ./
ForUpdateopt -> ForUpdate
/:$readableName ForUpdate:/

Catchesopt ::= $empty
/. $putCase consumeEmptyCatchesopt(); $break ./
Catchesopt -> Catches
/:$readableName Catches:/

-----------------------------------
-- 1.5 features : recovery rules --
-----------------------------------
RecoveryMethodHeaderName ::= Modifiersopt 'function' 'Identifier' '('
/.$putCase consumeRecoveryMethodHeaderName(); $break ./
/:$readableName MethodHeaderName:/

RecoveryMethodHeader ::= RecoveryMethodHeaderName FormalParameterListopt MethodHeaderRightParen 
/.$putCase consumeMethodHeader(); $break ./
-- RecoveryMethodHeader ::= RecoveryMethodHeaderName FormalParameterListopt MethodHeaderRightParen MethodHeaderExtendedDims MethodHeaderThrowsClause
-- /.$putCase consumeMethodHeader(); $break ./
/:$readableName MethodHeader:/
-----------------------------------
-- 1.5 features : recovery rules --
-----------------------------------

/.	}
}./

$names

PLUS_PLUS ::=    '++'   
MINUS_MINUS ::=    '--'   
EQUAL_EQUAL ::=    '=='   
LESS_EQUAL ::=    '<='   
GREATER_EQUAL ::=    '>='   
NOT_EQUAL ::=    '!='   
LEFT_SHIFT ::=    '<<'   
RIGHT_SHIFT ::=    '>>'   
UNSIGNED_RIGHT_SHIFT ::=    '>>>'  
PLUS_EQUAL ::=    '+='   
MINUS_EQUAL ::=    '-='   
MULTIPLY_EQUAL ::=    '*='   
DIVIDE_EQUAL ::=    '/='   
AND_EQUAL ::=    '&='   
OR_EQUAL ::=    '|='   
XOR_EQUAL ::=    '^='   
REMAINDER_EQUAL ::=    '%='   
LEFT_SHIFT_EQUAL ::=    '<<='  
RIGHT_SHIFT_EQUAL ::=    '>>='  
UNSIGNED_RIGHT_SHIFT_EQUAL ::=    '>>>=' 
OR_OR ::=    '||'   
AND_AND ::=    '&&'
PLUS ::=    '+'    
MINUS ::=    '-'    
NOT ::=    '!'    
REMAINDER ::=    '%'    
XOR ::=    '^'    
AND ::=    '&'    
MULTIPLY ::=    '*'    
OR ::=    '|'    
TWIDDLE ::=    '~'    
DIVIDE ::=    '/'    
GREATER ::=    '>'    
LESS ::=    '<'    
LPAREN ::=    '('    
RPAREN ::=    ')'    
LBRACE ::=    '{'    
RBRACE ::=    '}'    
LBRACKET ::=    '['    
RBRACKET ::=    ']'    
SEMICOLON ::=    ';'    
QUESTION ::=    '?'    
COLON ::=    ':'    
COMMA ::=    ','    
DOT ::=    '.'    
EQUAL ::=    '='    
AT ::=    '@'    

$end
-- need a carriage return after the $end
