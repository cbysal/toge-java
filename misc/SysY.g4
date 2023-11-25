grammar SysY;

root : compUnit*;

compUnit:
    varDecl
    | funcDef;

type: INT | FLOAT | VOID;

dimensions: (LB binaryExp RB)+;

varDecl: CONST? type varDef (COMMA varDef)* SEMI;

varDef: Ident dimensions? (ASSIGN initVal)?;

initVal:
	binaryExp
	| LC (initVal (COMMA initVal)*)? RC;

funcDef: type Ident LP (funcFParam (COMMA funcFParam)*)? RP blockStmt;

funcFParam: type Ident (LB RB (LB binaryExp RB)*)?;

blockStmt: LC stmt* RC;

stmt:
    assignStmt
    | varDecl
    | expStmt
    | ifStmt
    | whileStmt
	| blockStmt
	| blankStmt
	| breakStmt
	| continueStmt
	| retStmt;

assignStmt: lVal ASSIGN binaryExp SEMI;

blankStmt: SEMI;

expStmt: binaryExp SEMI;

ifStmt: IF LP binaryExp RP stmt (ELSE stmt)?;

whileStmt: WHILE LP binaryExp RP stmt;

breakStmt: BREAK SEMI;

continueStmt: CONTINUE SEMI;

retStmt: RETURN binaryExp? SEMI;

lVal: Ident (LB binaryExp RB)*;

unaryExp:
    (ADD | SUB | LNOT) unaryExp
    | LP binaryExp RP
    | varExp
    | funcCallExp
    | IntConst
    | FloatConst;

varExp: Ident (LB binaryExp RB)*;

funcCallExp: Ident LP (binaryExp (COMMA binaryExp)*)? RP;

binaryExp:
    binaryExp (MUL | DIV | MOD) binaryExp
    | binaryExp (ADD | SUB) binaryExp
    | binaryExp (LT | GT | LE | GE) binaryExp
    | binaryExp (EQ | NE) binaryExp
    | binaryExp LAND binaryExp
    | binaryExp LOR binaryExp
    | unaryExp;

BREAK: 'break';
CONST: 'const';
CONTINUE: 'continue';
ELSE: 'else';
FLOAT: 'float';
IF: 'if';
INT: 'int';
RETURN: 'return';
VOID: 'void';
WHILE: 'while';

ASSIGN: '=';

ADD: '+';
SUB: '-';
MUL: '*';
DIV: '/';
MOD: '%';

EQ: '==';
NE: '!=';
LT: '<';
LE: '<=';
GT: '>';
GE: '>=';

LNOT: '!';
LAND: '&&';
LOR: '||';

LP: '(';
RP: ')';
LB: '[';
RB: ']';
LC: '{';
RC: '}';

COMMA: ',';
SEMI: ';';

Ident: [A-Za-z_][0-9A-Za-z_]*;

IntConst:
	[1-9][0-9]*
	| '0'[0-7]*
	| '0'[Xx][0-9A-Fa-f]+;

FloatConst:
    ([0-9]+'.'[0-9]*|'.'[0-9]+)([Ee][+-]?[0-9]+)?
    | [0-9]+[Ee][+-]?[0-9]+
    | '0'[Xx]([0-9A-Fa-f]+'.'[0-9A-Fa-f]*|'.'[0-9A-Fa-f]+)[Pp][+-]?[0-9]+;

Whitespace: [ \r\t]+ -> skip;

Newline: '\n' -> skip;

BlockComment: '/*' .*? '*/' -> skip;

LineComment: '//' (~'\n'* '\\\n')* ~'\n'* -> skip;
