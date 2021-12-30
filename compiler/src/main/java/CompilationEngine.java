import java.io.*;
import java.util.*;

/**
 * Class for token parsing and output
 */
class CompilationEngine {
    public static final SymbolTable SYMBOL_TABLE = new SymbolTable();
    private final Tokenizer tokenizer;
    private final VMWriter vmWriter;
    private String className;

    CompilationEngine(Tokenizer tokenizer, VMWriter vmWriter) {
        this.tokenizer = tokenizer;
        this.vmWriter = vmWriter;
    }

    /**
     * Throws exception if type of token is not same as given tokenType
     *
     * @param token
     * @param tokenType
     */
    public static void assertToken(Token token, Token.TokenType tokenType) {
        if (!token.getTokenType().equals(tokenType)) {
            throw new InvalidTokenException(token);
        }
    }

    /**
     * Throws exception if type of token is not same as given tokenType token value is not same as given value
     *
     * @param token
     * @param tokenType
     * @param value
     */
    public static void assertToken(Token token, Token.TokenType tokenType, String value) {
        if (!(token.getTokenType().equals(tokenType) && token.getValue().equals(value))) {
            throw new InvalidTokenException(token);
        }
    }

    /**
     * Writes output to the output file
     *
     * @param outputFile
     */
    public void writeXml(File outputFile) {
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(outputFile));
            bufferedWriter.write(getClassNode().toString());
            if (tokenizer.hasMoreTokens()) {
                throw new InvalidTokenException("Only one class can be defined in one jack file!");
            }
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            vmWriter.close();
        }
    }

    /**
     * (expression (',' expression)* )?
     */
    public Node getExpressionList() {
        List<Node> children = new ArrayList<>();
        if (!(this.tokenizer.peekToken().getTokenType().equals(Token.TokenType.SYMBOL)
                && this.tokenizer.peekToken().getValue().equals(")"))) { // found ')', so no expression exists
            children.add(getExpression());
            while (true) {
                if (tokenizer.peekToken().getTokenType().equals(Token.TokenType.SYMBOL)
                        && this.tokenizer.peekToken().getValue().equals(",")) {
                    children.add(new TerminalNode(this.tokenizer.pollToken()));
                    children.add(getExpression());
                } else {
                    break;
                }
            }
        }

        return new NonTerminalNode("expressionList", children);
    }

    /**
     * term (op term)*
     */
    public Node getExpression() {
        List<Node> children = new ArrayList<>();
        children.add(getTerm());
        while (true) {
            if (new HashSet<>(Arrays.asList("+", "-", "*", "/", "&", "|", "<", ">", "="))
                    .contains(tokenizer.peekToken().getValue())) {
                children.add(new TerminalNode(tokenizer.pollToken()));
                children.add(getTerm());
            } else {
                break;
            }
        }
        return new NonTerminalNode("expression", children);
    }

    /**
     * integerConstant | stringConstant | keywordConstant | varName | varName '[' expression ']' | subroutineCall |
     * '(' expression ')' | unaryOp term
     */
    public Node getTerm() {
        Token next = tokenizer.peekToken();
        List<Node> nodes;
        switch (next.getTokenType()) {
            case INT_CONST:
            case STRING_CONST:
                nodes = Collections.singletonList(new TerminalNode(tokenizer.pollToken()));
                break;
            case KEYWORD:
                if (next.getValue().equals("true") || next.getValue().equals("false") || next.getValue().equals("this")
                        || next.getValue().equals("null")) {
                    nodes = Collections.singletonList(new TerminalNode(tokenizer.pollToken()));
                } else {
                    throw new InvalidTokenException(next);
                }
                break;
            case IDENTIFIER:
                nodes = new ArrayList<>();
                Token varName = tokenizer.pollToken();

                if (tokenizer.peekToken().getTokenType().equals(Token.TokenType.SYMBOL)
                        && tokenizer.peekToken().getValue().equals("[")) {
                    // varName[expression]
                    nodes.add(new IdentifierTerminalNode(varName.getValue(), SYMBOL_TABLE.getKind(varName.getValue()),
                            SYMBOL_TABLE.getType(varName.getValue()), SYMBOL_TABLE.getIndex(varName.getValue()), true));
                    assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "[");
                    nodes.add(new TerminalNode(tokenizer.pollToken()));
                    nodes.add(getExpression());
                    assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "]");
                    nodes.add(new TerminalNode(tokenizer.pollToken()));
                } else if (tokenizer.peekToken().getTokenType().equals(Token.TokenType.SYMBOL)
                        && tokenizer.peekToken().getValue().equals(".")) {
                    nodes.addAll(getSubroutineCall(varName));
                } else {
                    nodes.add(new IdentifierTerminalNode(varName.getValue(), SYMBOL_TABLE.getKind(varName.getValue()),
                            SYMBOL_TABLE.getType(varName.getValue()), SYMBOL_TABLE.getIndex(varName.getValue()), true));
                }
                break;
            case SYMBOL:
                nodes = new ArrayList<>();
                if (next.getValue().equals("(")) { // (expression)
                    assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "(");
                    nodes.add(new TerminalNode(tokenizer.pollToken()));
                    nodes.add(getExpression());
                    assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, ")");
                    nodes.add(new TerminalNode(tokenizer.pollToken()));
                } else if (next.getValue().equals("-") || next.getValue().equals("~")) {
                    nodes.add(new TerminalNode(tokenizer.pollToken()));
                    nodes.add(getTerm());
                }
                break;
            default:
                throw new InvalidTokenException(next);
        }
        return new NonTerminalNode("term", nodes);
    }

    /**
     * subroutineName '(' expressionList ')' | ( className | varName) '.' subroutineName '(' expressionList ')'
     */
    public List<Node> getSubroutineCall(Token subroutineName) {
        List<Node> nodes = new ArrayList<>();
        if (tokenizer.peekToken().getValue().equals("(")) { // subroutineName(expressionList)
            nodes.add(new IdentifierTerminalNode(subroutineName.getValue(), "subroutine", null, 0, true));
            assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "(");
            nodes.add(new TerminalNode(tokenizer.pollToken()));
            nodes.add(getExpressionList());
            assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, ")");
            nodes.add(new TerminalNode(tokenizer.pollToken()));
        } else if (tokenizer.peekToken().getValue().equals(".")) { // ( className | varName).subroutineName(expressionList)
            String name = subroutineName.getValue();
            if (SYMBOL_TABLE.contains(name)) {
                nodes.add(new IdentifierTerminalNode(name, SYMBOL_TABLE.getKind(name), SYMBOL_TABLE.getType(name),
                        SYMBOL_TABLE.getIndex(name), true));
            } else {
                nodes.add(new IdentifierTerminalNode(name, "class", null, 0, true));
            }
            assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, ".");
            nodes.add(new TerminalNode(tokenizer.pollToken()));
            assertToken(tokenizer.peekToken(), Token.TokenType.IDENTIFIER);
            nodes.add(new IdentifierTerminalNode(tokenizer.pollToken().getValue(), "subroutine", null, 0, true)); // subroutineName
            assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "(");
            nodes.add(new TerminalNode(tokenizer.pollToken()));
            nodes.add(getExpressionList());
            assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, ")");
            nodes.add(new TerminalNode(tokenizer.pollToken()));
        } else {
            throw new InvalidTokenException(tokenizer.peekToken());
        }
        return nodes;
    }

    /**
     * 'return' expression? ';'
     */
    public Node getReturnStatement() {
        List<Node> children = new ArrayList<>();
        assertToken(tokenizer.peekToken(), Token.TokenType.KEYWORD, "return");
        children.add(new TerminalNode(tokenizer.pollToken()));
        if (!tokenizer.peekToken().getValue().equals(";")) {
            children.add(getExpression());
        }
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, ";");
        children.add(new TerminalNode(tokenizer.pollToken()));
        return new NonTerminalNode("returnStatement", children);
    }

    /**
     * 'do' subroutineCall ';'
     */
    public Node getDoStatement() {
        List<Node> children = new ArrayList<>();
        assertToken(tokenizer.peekToken(), Token.TokenType.KEYWORD, "do");
        children.add(new TerminalNode(tokenizer.pollToken()));
        Token subroutineName = tokenizer.pollToken();
        children.addAll(getSubroutineCall(subroutineName));
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, ";");
        children.add(new TerminalNode(tokenizer.pollToken()));
        return new NonTerminalNode("doStatement", children);
    }

    /**
     * while' '(' expression ')' '{' statements '}'
     */
    public Node getWhileStatement() {
        List<Node> children = new ArrayList<>();
        assertToken(tokenizer.peekToken(), Token.TokenType.KEYWORD, "while");
        children.add(new TerminalNode(tokenizer.pollToken()));
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "(");
        children.add(new TerminalNode(tokenizer.pollToken()));
        children.add(getExpression());
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, ")");
        children.add(new TerminalNode(tokenizer.pollToken()));
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "{");
        children.add(new TerminalNode(tokenizer.pollToken()));
        children.add(getStatements());
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "}");
        children.add(new TerminalNode(tokenizer.pollToken()));
        return new NonTerminalNode("whileStatement", children);
    }

    /**
     * 'if' '(' expression ')' '{' statements '}' ( 'else' '{' statements '}' )?
     */
    public Node getIfStatement() {
        List<Node> children = new ArrayList<>();
        assertToken(tokenizer.peekToken(), Token.TokenType.KEYWORD, "if");
        children.add(new TerminalNode(tokenizer.pollToken()));
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "(");
        children.add(new TerminalNode(tokenizer.pollToken()));
        children.add(getExpression());
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, ")");
        children.add(new TerminalNode(tokenizer.pollToken()));
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "{");
        children.add(new TerminalNode(tokenizer.pollToken()));
        children.add(getStatements());
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "}");
        children.add(new TerminalNode(tokenizer.pollToken()));
        if (tokenizer.peekToken().getValue().equals("else")) {
            assertToken(tokenizer.peekToken(), Token.TokenType.KEYWORD, "else");
            children.add(new TerminalNode(tokenizer.pollToken()));
            assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "{");
            children.add(new TerminalNode(tokenizer.pollToken()));
            children.add(getStatements());
            assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "}");
            children.add(new TerminalNode(tokenizer.pollToken()));
        }
        return new NonTerminalNode("ifStatement", children);
    }

    /**
     * 'let' varName ('[' expression ']')? '=' expression ';'
     */
    public Node getLetStatement() {
        List<Node> children = new ArrayList<>();
        assertToken(tokenizer.peekToken(), Token.TokenType.KEYWORD, "let");
        children.add(new TerminalNode(tokenizer.pollToken()));
        assertToken(tokenizer.peekToken(), Token.TokenType.IDENTIFIER);
        String name = tokenizer.pollToken().getValue();
        children.add(new IdentifierTerminalNode(name, SYMBOL_TABLE.getKind(name), SYMBOL_TABLE.getType(name),
                SYMBOL_TABLE.getIndex(name), true));
        if (tokenizer.peekToken().getValue().equals("[")) {
            assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "[");
            children.add(new TerminalNode(tokenizer.pollToken()));
            children.add(getExpression());
            assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "]");
            children.add(new TerminalNode(tokenizer.pollToken()));
        }
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "=");
        children.add(new TerminalNode(tokenizer.pollToken()));
        children.add(getExpression());
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, ";");
        children.add(new TerminalNode(tokenizer.pollToken()));
//        System.out.println(new NonTerminalNode("letStatement", children));
        return new NonTerminalNode("letStatement", children);
    }

    /**
     * statement*
     */
    public Node getStatements() {
        List<Node> children = new ArrayList<>();
        while (true) {
            if (tokenizer.peekToken().getValue().equals("}")) {
                break;
            }
            children.add(getStatement());
        }
        return new NonTerminalNode("statements", children);
    }

    /**
     * letStatement | ifStatement | whileStatement | doStatement | returnStatement
     */
    public Node getStatement() {
        Token next = tokenizer.peekToken();
        switch (next.getValue()) {
            case "let":
                return getLetStatement();
            case "if":
                return getIfStatement();
            case "while":
                return getWhileStatement();
            case "do":
                return getDoStatement();
            case "return":
                return getReturnStatement();
            default:
                throw new InvalidTokenException(next);
        }
    }

    /**
     * 'var' type varName (',' varName)* ';'
     */
    public Node getVarDec() {
        List<Node> children = new ArrayList<>();

        //var
        assertToken(tokenizer.peekToken(), Token.TokenType.KEYWORD, "var");
        children.add(new TerminalNode(tokenizer.pollToken()));

        //type
        Node type = getType();
        children.add(type);

        //varName
        assertToken(tokenizer.peekToken(), Token.TokenType.IDENTIFIER);
        Token name = tokenizer.pollToken();
        SYMBOL_TABLE.add(name.getValue(), SymbolTable.Kind.VAR, type.getValue());
        children.add(new IdentifierTerminalNode(name.getValue(), "var", type.getValue(),
                SYMBOL_TABLE.getIndex(name.getValue()), false));
//        children.add(new TerminalNode(tokenizer.pollToken()));

        while (true) {
            if (tokenizer.peekToken().getValue().equals(";")) {
                break;
            }
            assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, ",");
            children.add(new TerminalNode(tokenizer.pollToken()));

            assertToken(tokenizer.peekToken(), Token.TokenType.IDENTIFIER);
            name = tokenizer.pollToken();
            SYMBOL_TABLE.add(name.getValue(), SymbolTable.Kind.VAR, type.getValue());
            children.add(new IdentifierTerminalNode(name.getValue(), SymbolTable.Kind.VAR.name(), type.getValue(),
                    SYMBOL_TABLE.getIndex(name.getValue()), false));
//            children.add(new TerminalNode(tokenizer.pollToken()));
        }
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, ";");
        children.add(new TerminalNode(tokenizer.pollToken()));
        return new NonTerminalNode("varDec", children);
    }

    /**
     * '{' varDec* statements '}'
     */
    public Node getSubroutineBody(String subroutineName, String subroutineType) {
        List<Node> children = new ArrayList<>();
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "{");
        children.add(new TerminalNode(tokenizer.pollToken()));
        while (tokenizer.peekToken().getValue().equals("var")) {
            children.add(getVarDec());
        }
        int nLocal = CompilationEngine.SYMBOL_TABLE.indexMapping.get(SymbolTable.Kind.VAR);
        Node statements = getStatements();
        children.add(statements);
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "}");
        children.add(new TerminalNode(tokenizer.pollToken()));
        vmWriter.writeFunction(className + "." + subroutineName, nLocal);

        // constructor always returns 'this'
        if (subroutineType.equals("constructor")) {
            int nClassVars = CompilationEngine.SYMBOL_TABLE.indexMapping.get(SymbolTable.Kind.FIELD);
            vmWriter.writePush("constant", nClassVars);
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop("pointer", 0);
        } else if (subroutineType.equals("method")) {
            vmWriter.writePush("argument", 0);
            vmWriter.writePop("pointer", 0);
        }
        vmWriter.writeStatements(statements);
        return new NonTerminalNode("subroutineBody", children);
    }

    /**
     * ( (type varName) (',' type varName)*)?
     */
    public Node getParameterList() {
        List<Node> children = new ArrayList<>();
        while (!tokenizer.peekToken().getValue().equals(")")) {
            if (!children.isEmpty()) {
                assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, ",");
                children.add(new TerminalNode(tokenizer.pollToken())); // comma
            }
            Node type = getType();
            children.add(type); // type

            assertToken(tokenizer.peekToken(), Token.TokenType.IDENTIFIER);
            Token name = tokenizer.pollToken();
            SYMBOL_TABLE.add(name.getValue(), SymbolTable.Kind.ARG, type.getValue());
            children.add(new IdentifierTerminalNode(name.getValue(), SymbolTable.Kind.ARG.name(), type.getValue(),
                    SYMBOL_TABLE.getIndex(name.getValue()), false)); // varName
        }
        return new NonTerminalNode("parameterList", children);
    }

    /**
     * ('constructor' | 'function' | 'method') ('void' | type) subroutineName '(' parameterList ')' subroutineBody
     */
    public Node getSubroutineDec() {
        SYMBOL_TABLE.resetFunctionSymbols();
        List<Node> children = new ArrayList<>();
        if (!Arrays.asList("constructor", "function", "method").contains(tokenizer.peekToken().getValue())) {
            throw new InvalidTokenException(tokenizer.peekToken());
        }
        assertToken(tokenizer.peekToken(), Token.TokenType.KEYWORD);
        String subroutineType = tokenizer.peekToken().getValue();
        children.add(new TerminalNode(tokenizer.pollToken()));

        String type = "";
        if (!tokenizer.peekToken().getValue().equals("void")) {
            Node node = getType();
            children.add(node); // type
            type = node.getName();
        } else {
            assertToken(tokenizer.peekToken(), Token.TokenType.KEYWORD);
            type = tokenizer.peekToken().getValue();
            children.add(new TerminalNode(tokenizer.pollToken())); //void
        }
        assertToken(tokenizer.peekToken(), Token.TokenType.IDENTIFIER);
        String subroutineName = tokenizer.peekToken().getValue();
        children.add(new IdentifierTerminalNode(tokenizer.pollToken().getValue(), subroutineType, type,
                0, false)); // subroutineName
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "(");
        children.add(new TerminalNode(tokenizer.pollToken()));
        if (subroutineType.equals("method")) {
            CompilationEngine.SYMBOL_TABLE.add("this", SymbolTable.Kind.ARG, className);
        }
        Node params = getParameterList();
        children.add(params);
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, ")");
        children.add(new TerminalNode(tokenizer.pollToken()));
        children.add(getSubroutineBody(subroutineName, subroutineType)); // subroutineBody
        return new NonTerminalNode("subroutineDec", children);
    }

    /**
     * ('static' | 'field' ) type varName (',' varName)* ';'
     */
    public Node getClassVarDec() {
        List<Node> children = new ArrayList<>();
        if (!Arrays.asList("static", "field").contains(tokenizer.peekToken().getValue())) {
            throw new InvalidTokenException(tokenizer.peekToken());
        }
        assertToken(tokenizer.peekToken(), Token.TokenType.KEYWORD);
        Token kind = tokenizer.pollToken();
        children.add(new TerminalNode(kind));
        Node type = getType();
        children.add(type); // type
        assertToken(tokenizer.peekToken(), Token.TokenType.IDENTIFIER);
        Token name = tokenizer.pollToken();
        SYMBOL_TABLE.add(name.getValue(), SymbolTable.Kind.getEnum(kind.getValue()), type.getValue());
        children.add(new IdentifierTerminalNode(name.getValue(), kind.getValue(), type.getValue(),
                SYMBOL_TABLE.getIndex(name.getValue()), false)); // varName
        while (!tokenizer.peekToken().getValue().equals(";")) {
            assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, ",");
            children.add(new TerminalNode(tokenizer.pollToken()));
            assertToken(tokenizer.peekToken(), Token.TokenType.IDENTIFIER);
            name = tokenizer.pollToken();
            SYMBOL_TABLE.add(name.getValue(), SymbolTable.Kind.getEnum(kind.getValue()), type.getValue());
            children.add(new IdentifierTerminalNode(name.getValue(), kind.getValue(), type.getValue(),
                    SYMBOL_TABLE.getIndex(name.getValue()), false)); // varName
        }
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, ";");
        children.add(new TerminalNode(tokenizer.pollToken()));
        return new NonTerminalNode("classVarDec", children);
    }

    /**
     * 'class' className '{' classVarDec* subroutineDec* '}'
     */
    public Node getClassNode() {
        SYMBOL_TABLE.resetClassSymbols();
        SYMBOL_TABLE.resetFunctionSymbols();

        List<Node> children = new ArrayList<>();
        assertToken(tokenizer.peekToken(), Token.TokenType.KEYWORD, "class");
        children.add(new TerminalNode(tokenizer.pollToken()));
        assertToken(tokenizer.peekToken(), Token.TokenType.IDENTIFIER);
        className = tokenizer.peekToken().getValue();
        children.add(new IdentifierTerminalNode(tokenizer.pollToken().getValue(), "class", null, 0, false)); // className
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "{");
        children.add(new TerminalNode(tokenizer.pollToken())); // '{'
        while (true) {
            if (!Arrays.asList("static", "field").contains(tokenizer.peekToken().getValue())) {
                break;
            }
            children.add(getClassVarDec());
        }
        while (true) {
            if (tokenizer.peekToken().getValue().equals("}")) {
                break;
            }
            children.add(getSubroutineDec());
        }
        assertToken(tokenizer.peekToken(), Token.TokenType.SYMBOL, "}");
        children.add(new TerminalNode(tokenizer.pollToken())); // '}'
        return new NonTerminalNode("class", children);
    }

    /**
     * 'int' | 'char' | 'boolean' | className
     */
    public Node getType() {
        Token next = tokenizer.peekToken();
        if (next.getTokenType().equals(Token.TokenType.KEYWORD)
                && new HashSet<>(Arrays.asList("int", "char", "boolean")).contains(next.getValue())) {
            return new TerminalNode(tokenizer.pollToken());
        } else if (next.getTokenType().equals(Token.TokenType.IDENTIFIER)) {
            return new IdentifierTerminalNode(tokenizer.pollToken().getValue(), "class", null, 0, false);
        } else {
            throw new InvalidTokenException(next);
        }
    }
}
