import java.io.*;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Writes VM output
 */
public class VMWriter {
    private final BufferedWriter bufferedWriter;
    private final String className;
    private int ifCount = 0;
    private int whileCount = 0;

    public VMWriter(File outputFile) {
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(outputFile));
            className = outputFile.getName().split("\\.")[0];
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * VM code for return statement
     */
    void writeReturnStatement(Node returnNode) {
        // void function
        if (returnNode.getChildren().get(1).getValue().equals(";")) {
            writePush("constant", 0);
        } else {
            Node expression = returnNode.getChildren().get(1);
            writeExpression(expression);
        }
        writeToFile("return");
    }

    /**
     * VM code for statements
     */
    void writeStatements(Node statements) {
        for (Node statement : statements.getChildren()) {
            switch (statement.getName()) {
                case "doStatement":
                    writeDoStatement(statement);
                    break;
                case "returnStatement":
                    writeReturnStatement(statement);
                    break;
                case "letStatement":
                    writeLetStatement(statement);
                    break;
                case "ifStatement":
                    writeIfStatement(statement);
                    break;
                case "whileStatement":
                    writeWhileStatement(statement);
                    break;
                default:
                    throw new InvalidTokenException(statement.getValue());
            }
        }
    }

    /**
     * VM code for return subroutine call i.e. subroutineName '(' expressionList ')'
     */
    void writeSubroutineCall(String subroutineName, Node expressionList) {
        int nParams = (expressionList.getChildren().size() + 1) / 2 + 1;
        writePush("pointer", 0);
        writeExpressions(expressionList);
        writeCall(className + "." + subroutineName, nParams);
    }

    /**
     * VM code for return subroutine call i.e. ( className | varName) '.' subroutineName '(' expressionList ')'
     */
    void writeSubroutineCall(String className, String subroutineName, Node expressionList) {
        int nParams = (expressionList.getChildren().size() + 1) / 2;
        if (CompilationEngine.SYMBOL_TABLE.contains(className)) { // varName case
            writePush(CompilationEngine.SYMBOL_TABLE.getVmKind(className), CompilationEngine.SYMBOL_TABLE.getIndex(className));
            className = CompilationEngine.SYMBOL_TABLE.getType(className);
            nParams++;
        }
        writeExpressions(expressionList);
        writeCall(className + "." + subroutineName, nParams);
    }

    /**
     * 'do' subroutineCall ';'
     */
    void writeDoStatement(Node doStatement) {
        if (doStatement.getChildren().get(2).getValue().equals(".")) { // (varName|className).subroutine(expressions)
            String className = doStatement.getChildren().get(1).getValue();
            String functionName = doStatement.getChildren().get(3).getValue();
            Node expressionList = doStatement.getChildren().get(5);
            writeSubroutineCall(className, functionName, expressionList);
        } else {
            String functionName = doStatement.getChildren().get(1).getValue();
            Node expressionList = doStatement.getChildren().get(3);
            writeSubroutineCall(functionName, expressionList);
        }
        writePop("temp", 0);
    }

    /**
     * 'if' '(' expression ')' '{' statements '}' ( 'else' '{' statements '}' )?
     */
    void writeIfStatement(Node ifStatement) {
        Node expression = ifStatement.getChild("expression");
        boolean hasElse = ifStatement.getChildren().size() > 7;
        writeExpression(expression);
        int ifGotoCount = this.ifCount++;
        writeIfGoto("IF_TRUE" + ifGotoCount);
        writeGoto("IF_FALSE" + ifGotoCount);
        writeLabel("IF_TRUE" + ifGotoCount);
        writeStatements(ifStatement.getChildren().get(5));
        if (hasElse) {
            writeGoto("IF_END" + ifGotoCount);
            writeLabel("IF_FALSE" + ifGotoCount);
            writeStatements(ifStatement.getChildren().get(9));
            writeLabel("IF_END" + ifGotoCount);
        } else {
            writeLabel("IF_FALSE" + ifGotoCount);
        }
    }

    /**
     * while' '(' expression ')' '{' statements '}'
     */
    void writeWhileStatement(Node whileStatement) {
        int whileCount = this.whileCount++;
        writeLabel("WHILE_EXP" + whileCount);
        Node expression = whileStatement.getChildren().get(2);
        writeExpression(expression);
        writeArithmetic("not");
        writeIfGoto("WHILE_END" + whileCount);
        Node statements = whileStatement.getChildren().get(5);
        writeStatements(statements);
        writeGoto("WHILE_EXP" + whileCount);
        writeLabel("WHILE_END" + whileCount);
    }

    // 'let' varName ('[' expression ']')? '=' expression ';'
    void writeLetStatement(Node letStatement) {
        String varName = letStatement.getChildren().get(1).getValue();
        if (letStatement.getChildren().get(2).getValue().equals("[")) {
            Node expression1 = letStatement.getChildren().get(3);
            Node expression2 = letStatement.getChildren().get(6);
            writeExpression(expression1);
            writePush(CompilationEngine.SYMBOL_TABLE.getVmKind(varName), CompilationEngine.SYMBOL_TABLE.getIndex(varName));
            writeArithmetic("add");
            writeExpression(expression2);
            writePop("temp", 0);
            writePop("pointer", 1);
            writePush("temp", 0);
            writePop("that", 0);
        } else {
            Node expression = letStatement.getChild("expression");
            writeExpression(expression);
            writePop(CompilationEngine.SYMBOL_TABLE.getVmKind(varName), CompilationEngine.SYMBOL_TABLE.getIndex(varName));
        }
    }

    /**
     * (expression (',' expression)* )?
     */
    void writeExpressions(Node expressions) {
        expressions.getChildren().stream().forEach(this::writeExpression);
    }

    /**
     * term (op term)*
     */
    void writeExpression(Node expression) {
        Stack<Node> stack = new Stack<>();
        for (Node node : expression.getChildren()) {
            if (node.getName().equals("symbol")) {
                stack.push(node);
            } else {
                writeTerm(node);
            }
        }
        while (!stack.empty()) {
            writeOp(stack.pop());
        }
    }

    // '~" or '-' term
    void writeUnaryOp(Node op) {
        switch (op.getValue()) {
            case "-":
                writeArithmetic("neg");
                break;
            case "~":
                writeArithmetic("not");
                break;
        }
    }

    // operation symbols
    void writeOp(Node op) {
        switch (op.getValue()) {
            case "+":
                writeArithmetic("add");
                break;
            case "-":
                writeArithmetic("sub");
                break;
            case "*":
                writeCall("Math.multiply", 2);
                break;
            case "/":
                writeCall("Math.divide", 2);
                break;
            case "&":
                writeArithmetic("and");
                break;
            case "|":
                writeArithmetic("or");
                break;
            case ">":
                writeArithmetic("gt");
                break;
            case "<":
                writeArithmetic("lt");
                break;
            case "=":
                writeArithmetic("eq");
                break;
        }
    }


    /**
     * integerConstant | stringConstant | keywordConstant | varName | varName '[' expression ']' | subroutineCall |
     * '(' expression ')' | unaryOp term
     */
    void writeTerm(Node term) {
        switch (term.getChildren().get(0).getName()) {

            case "integerConstant":
                writePush("constant", Integer.valueOf(term.getChildren().get(0).getValue()));
                break;
            case "stringConstant":
                String str = term.getChildren().get(0).getValue();
                writePush("constant", str.length());
                writeCall("String.new", 1);
                for (char c : str.toCharArray()) {
                    writePush("constant", c);
                    writeCall("String.appendChar", 2);
                }
                break;
            case "keyword":
                String kw = term.getChildren().get(0).getValue();
                switch (kw) {
                    case "true":
                        writePush("constant", 0);
                        writeArithmetic("not");
                        break;
                    case "false":
                    case "null":
                        writePush("constant", 0);
                        break;
                    case "this":
                        writePush("pointer", 0);
                        break;
                }
                break;
            case "symbol":
                String symbol = term.getChildren().get(0).getValue();
                if (symbol.equals("~")) {
                    writeTerm(term.getChildren().get(1));
                    writeArithmetic("not");
                } else if (symbol.equals("-")) {
                    writeTerm(term.getChildren().get(1));
                    writeArithmetic("neg");
                } else {
                    writeExpression(term.getChildren().get(1));
                }
                break;
            case "identifier":
                String varName = term.getChildren().get(0).getValue();
                String subroutine = "";
                Node expressions;
                if (term.getChildren().size() == 1) { // varName
                    writePush(CompilationEngine.SYMBOL_TABLE.getVmKind(varName), CompilationEngine.SYMBOL_TABLE.getIndex(varName));
                } else if (term.getChildren().get(1).getValue().equals("(")) { // subroutine(expressions)
                    subroutine = term.getChildren().get(0).getValue();
                    expressions = term.getChildren().get(2);
                    writeSubroutineCall(subroutine, expressions);
                } else if (term.getChildren().get(1).getValue().equals(".")) { // (class|varName).subroutine(expressions)
                    subroutine = term.getChildren().get(2).getValue();
                    expressions = term.getChildren().get(4);
                    writeSubroutineCall(varName, subroutine, expressions);
                } else if (term.getChildren().get(1).getValue().equals("[")) { //varName[expression]
                    Node expression = term.getChildren().get(2);
                    writeExpression(expression);
                    writePush(CompilationEngine.SYMBOL_TABLE.getVmKind(varName), CompilationEngine.SYMBOL_TABLE.getIndex(varName));
                    writeArithmetic("add");
                    writePop("pointer", 1);
                    writePush("that", 0);
                }

        }
    }

    // push <kind> <index>
    void writePush(String segment, int index) {
        writeToFile(String.format("push %s %d", segment, index));
    }

    // pop <kind> <index>
    void writePop(String segment, int index) {
        if (segment.equals("constant")) {
            writeToFile("pop constant");
        } else {
            writeToFile(String.format("pop %s %d", segment, index));
        }
    }

    // e.g. add
    void writeArithmetic(String command) {
        writeToFile(command);
    }

    // label <name>
    void writeLabel(String label) {
        writeToFile(String.format("label %s", label));
    }

    // goto <name>
    void writeGoto(String label) {
        writeToFile(String.format("goto %s", label));
    }

    // if-goto <name>
    void writeIfGoto(String label) {
        writeToFile(String.format("if-goto %s", label));
    }

    // call <class>.<function> <number of args>
    void writeCall(String function, int nArguments) {
        writeToFile(String.format("call %s %d", function, nArguments));
    }

    // function <class>.<function> <number of local variable>
    void writeFunction(String function, int nLocals) {
        writeToFile(String.format("function %s %d", function, nLocals));
        ifCount = 0;
        whileCount = 0;
    }

    // flushes buffer memory to the output file
    void close() {
        try {
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    // write one line to output vm file
    private void writeToFile(String line) {
        try {
            bufferedWriter.write(line);
            bufferedWriter.write("\n");
            bufferedWriter.flush();
//            System.out.println(line);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
