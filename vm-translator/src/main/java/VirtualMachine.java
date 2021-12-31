import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VirtualMachine {
    static int jumpCount = 0;

    public static void main(String[] args) throws IOException {
        String inputPath = args[0]; //"/home/amit/Software/nand2tetris/projects/08/FunctionCalls/NestedCall";
        File inputFIle = new File(inputPath);
        List<File> vmFiles = new ArrayList<>();

        // If only one input file /some/path/file.vm was passed, write the output to /some/path/file.asm
        // If only one input directory /some/path/folder/ was passed, write the output to /some/path/folder/folder.asm
        File outputFile;
        if (!inputFIle.isDirectory()) {
            vmFiles.add(inputFIle);
            outputFile = new File(inputPath.substring(0, inputPath.length() - 2) + "asm");
        } else {
            vmFiles = Arrays.asList(inputFIle.listFiles()).stream().filter(file -> file.toString().endsWith(".vm")).
                    collect(Collectors.toList());
            outputFile = new File(inputPath + File.separator + inputFIle.getName() + ".asm");
        }

        boolean bootStrap = vmFiles.stream().anyMatch(file -> file.getName().equals("Sys.vm"));

        CodeWriter codeWriter = new CodeWriter(outputFile, "");
        if (bootStrap) {
            codeWriter.writeBootStrap();
        }
        for (File file : vmFiles) {
            String vmFileName = file.getName().split("\\.")[0];
            codeWriter.setVmFileName(vmFileName);
            Parser parser = new Parser(file);
            while (parser.hasMoreCommands()) {
                String command = parser.nextCommand();
                codeWriter.writeComment(command);
                CommandType commandType = parser.commandType(command.trim());
                if (commandType == CommandType.C_ARITHMETIC) {
                    codeWriter.writeArithmeticCommand(command.trim());
                } else if (commandType == CommandType.C_POP) {
                    codeWriter.writePushPop(commandType, parser.getArg1(command), parser.getArg2(command));
                } else if (commandType == CommandType.C_PUSH) {
                    codeWriter.writePushPop(commandType, parser.getArg1(command), parser.getArg2(command));
                } else if (commandType == CommandType.C_LABEL) {
                    codeWriter.writeProgramFlow(commandType, String.format("%s.%s", vmFileName, parser.getArg1(command)));
                } else if (commandType == CommandType.C_GOTO) {
                    codeWriter.writeProgramFlow(commandType, String.format("%s.%s", vmFileName, parser.getArg1(command)));
                } else if (commandType == CommandType.C_IF_GOTO) {
                    codeWriter.writeProgramFlow(commandType, String.format("%s.%s", vmFileName, parser.getArg1(command)));
                } else if (commandType == CommandType.C_FUNCTION_RETURN) {
                    codeWriter.writeFunctionReturn();
                } else if (commandType == CommandType.C_FUNCTION_CALL) {
                    codeWriter.writeCallFunction(parser.getArg1(command), parser.getArg2(command));
                } else if (commandType == CommandType.C_FUNCTION_DEFINE) {
                    codeWriter.writeDefineFunction(parser.getArg1(command), parser.getArg2(command));
                } else {
                    throw new RuntimeException("Unknown command");
                }
            }
        }

        System.out.println("Successfully wrote to: " + outputFile);
    }
}

class CodeWriter {
    private final BufferedWriter bufferedWriter;
    private String vmFileName;

    CodeWriter(File outputFile, String vmFileName) throws IOException {
        bufferedWriter = new BufferedWriter(new FileWriter(outputFile));
        this.vmFileName = vmFileName;
    }

    void writeComment(String comment) throws IOException {
        bufferedWriter.write("// " + comment + "\n");
        bufferedWriter.flush();
    }

    public void setVmFileName(String vmFileName) {
        this.vmFileName = vmFileName;
    }

    /**
     * Bootstrap code which will be in the beginning of the asm file
     */
    void writeBootStrap() throws IOException {
        bufferedWriter.write("@256\n");
        bufferedWriter.write("D=A\n");
        bufferedWriter.write("@SP\n");
        bufferedWriter.write("M=D\n");

        writeCallFunction("Sys.init", "0");
        bufferedWriter.flush();
    }

    /**
     * Writes assembly language for returning function
     */
    void writeFunctionReturn() throws IOException {
        // FRAME = LCL
        //writeComment("FRAME = LCL");
        bufferedWriter.write("@LCL\n");
        bufferedWriter.write("D=M\n");
        bufferedWriter.write("@FRAME\n");
        bufferedWriter.write("M=D\n");

        // RET = *(FRAME - 5)
        //writeComment("RET = *(FRAME - 5)");
        writeFromTemp("RET", 5);

        //writeComment("*ARG = pop()");
        writePushPop(CommandType.C_POP, "argument", "0");

        // SP = ARG + 1
        //writeComment("SP = ARG + 1");
        bufferedWriter.write("@ARG\n");
        bufferedWriter.write("D=M+1\n");
        bufferedWriter.write("@SP\n");
        bufferedWriter.write("M=D\n");

        //writeComment("THAT = *(FRAME - 1)");
        writeFromTemp("THAT", 1);
        //writeComment("THIS = *(FRAME - 2)");
        writeFromTemp("THIS", 2);
        //writeComment("ARG = *(FRAME - 3)");
        writeFromTemp("ARG", 3);
        //writeComment("LCL = *(FRAME - 4)");
        writeFromTemp("LCL", 4);


        //writeComment("goto RET");
        bufferedWriter.write("@RET\n");
        bufferedWriter.write("A=M\n");
        bufferedWriter.write("0;JMP\n");
        bufferedWriter.flush();
    }

    // For example: RET = *(FRAME - 5), address = RET, offset = 5
    void writeFromTemp(String address, int offset) throws IOException {
        bufferedWriter.write("@FRAME\n");
        bufferedWriter.write("D=M\n");
        bufferedWriter.write("@" + offset + "\n");
        bufferedWriter.write("A=D-A\n");
        bufferedWriter.write("D=M\n");
        bufferedWriter.write("@" + address + "\n");
        bufferedWriter.write("M=D\n");
        bufferedWriter.flush();
    }

    /**
     * Writes assembly language for defining  a function e.g. function f k
     *
     * @param arg1: function name
     * @param arg2: number of local variables needed
     */
    void writeDefineFunction(String arg1, String arg2) throws IOException {
        //writeComment(String.format("function %s %s", arg1, arg2));
        bufferedWriter.write(String.format("(%s)\n", arg1));
        for (int i = 0; i < Integer.valueOf(arg2); i++) {
            writePushPop(CommandType.C_PUSH, "constant", "0");
        }
        bufferedWriter.flush();
    }

    /**
     * Writes assembly language for function call e.g. call f n
     *
     * @param arg1: function name
     * @param arg2: number of arguments
     * @throws IOException
     */
    void writeCallFunction(String arg1, String arg2) throws IOException {
        //writeComment(String.format("call %s %s", arg1, arg2));
        String returnAddress = String.format("%s_return_address_%s", vmFileName, VirtualMachine.jumpCount++);
        pushToStack(returnAddress, true);
        pushToStack("LCL", false);
        pushToStack("ARG", false);
        pushToStack("THIS", false);
        pushToStack("THAT", false);
        // ARG = SP - n - 5
        bufferedWriter.write(String.format("@SP\n"));
        bufferedWriter.write(String.format("D=M\n"));
        bufferedWriter.write(String.format("@%s\n", arg2));
        bufferedWriter.write(String.format("D=D-A\n"));
        bufferedWriter.write(String.format("@5\n", arg1));
        bufferedWriter.write(String.format("D=D-A\n"));
        bufferedWriter.write(String.format("@ARG\n"));
        bufferedWriter.write(String.format("M=D\n"));
        //LCL = SP
        bufferedWriter.write("@SP\n");
        bufferedWriter.write("D=M\n");
        bufferedWriter.write("@LCL\n");
        bufferedWriter.write("M=D\n");
        writeProgramFlow(CommandType.C_GOTO, arg1);
        bufferedWriter.write(String.format("(%s)\n", returnAddress));
        bufferedWriter.flush();
    }

    /**
     * Translates control flow commands to assembly language
     *
     * @param commandType
     * @param arg1
     * @throws IOException
     */
    void writeProgramFlow(CommandType commandType, String arg1) throws IOException {
        if (commandType == CommandType.C_LABEL) {
            bufferedWriter.write(String.format("(%s)\n", arg1));
        } else if (commandType == CommandType.C_GOTO) {
            bufferedWriter.write(String.format("@%s\n", arg1));
            bufferedWriter.write(String.format("0;JMP\n", arg1));
        } else {
            bufferedWriter.write(String.format("@SP\n", arg1));
            bufferedWriter.write(String.format("AM=M-1\n", arg1));
            bufferedWriter.write(String.format("D=M\n", arg1));
            bufferedWriter.write(String.format("@%s\n", arg1));
            bufferedWriter.write(String.format("D;JNE\n", arg1));
        }
        bufferedWriter.flush();
    }

    void writeArithmeticCommonCode() throws IOException {
        bufferedWriter.write("@SP\n");
        bufferedWriter.write("AM=M-1\n");
        bufferedWriter.write("D=M\n");
        bufferedWriter.write("A=A-1\n");
        bufferedWriter.flush();
    }

    void writeComparisonCommonCode(String jmp) throws IOException {
        bufferedWriter.write("@SP\n");
        bufferedWriter.write("AM=M-1\n");
        bufferedWriter.write("D=M\n");
        bufferedWriter.write("A=A-1\n");
        bufferedWriter.write("D=M-D\n");
        bufferedWriter.write("@TRUE" + VirtualMachine.jumpCount + "\n");
        bufferedWriter.write("D;" + jmp + "\n");
        bufferedWriter.write("@SP\n");
        bufferedWriter.write("A=M-1\n");
        bufferedWriter.write("M=0\n");
        bufferedWriter.write("@CONTINUE" + VirtualMachine.jumpCount + "\n");
        bufferedWriter.write("0;JMP\n");
        bufferedWriter.write("(TRUE" + VirtualMachine.jumpCount + ")\n");
        bufferedWriter.write("@SP\n");
        bufferedWriter.write("A=M-1\n");
        bufferedWriter.write("M=-1\n");
        bufferedWriter.write("(CONTINUE" + VirtualMachine.jumpCount + ")\n");
        VirtualMachine.jumpCount++;
    }

    /**
     * Translates arithmetic command to assembly language syntax
     *
     * @param command
     */
    void writeArithmeticCommand(String command) throws IOException {
        if (command.equals("add")) {
            writeArithmeticCommonCode();
            bufferedWriter.write("M=D+M\n");
        } else if (command.equals("sub")) {
            writeArithmeticCommonCode();
            bufferedWriter.write("M=M-D\n");
        } else if (command.equals("and")) {
            writeArithmeticCommonCode();
            bufferedWriter.write("M=D&M\n");
        } else if (command.equals("or")) {
            writeArithmeticCommonCode();
            bufferedWriter.write("M=D|M\n");
        } else if (command.equals("neg")) {
            bufferedWriter.write("@SP\n");
            bufferedWriter.write("A=M-1\n");
            bufferedWriter.write("M=-M\n");
        } else if (command.equals("not")) {
            bufferedWriter.write("@SP\n");
            bufferedWriter.write("A=M-1\n");
            bufferedWriter.write("M=!M\n");
        } else if (command.equals("gt")) {
            writeComparisonCommonCode("JGT");
        } else if (command.equals("lt")) {
            writeComparisonCommonCode("JLT");
        } else if (command.equals("eq")) {
            writeComparisonCommonCode("JEQ");
        }

        bufferedWriter.flush();
    }

    /**
     * Translates push or pop command to assembly language syntax
     *
     * @param commandType
     * @param arg1        push or pop
     * @param arg2        rest of the command for ex: pointer 0
     */
    void writePushPop(CommandType commandType, String arg1, String arg2) throws IOException {
        if (commandType == CommandType.C_PUSH) {
            if (arg1.equals("constant")) {
                bufferedWriter.write("@" + Integer.valueOf(arg2) + "\n");
                bufferedWriter.write("D=A\n");
                bufferedWriter.write("@SP\n");
                bufferedWriter.write("A=M\n");
                bufferedWriter.write("M=D\n");
                bufferedWriter.write("@SP\n");
                bufferedWriter.write("M=M+1\n");
            } else {
                Map<String, String> map = new HashMap<String, String>() {{
                    put("local", "LCL");
                    put("argument", "ARG");
                    put("this", "THIS");
                    put("that", "THAT");
                }};

                if (arg1.equals("static")) {
                    bufferedWriter.write("@" + vmFileName + arg2 + "\n");
                    bufferedWriter.write("D=M\n");
                } else if (arg1.equals("temp")) {
                    bufferedWriter.write("@" + (5 + Integer.valueOf(arg2)) + "\n");
                    bufferedWriter.write("D=M\n");
                } else if (arg1.equals("pointer")) {
                    if (arg2.equals("0")) {
                        bufferedWriter.write("@THIS\n");
                    } else {
                        bufferedWriter.write("@THAT\n");
                    }
                    bufferedWriter.write("D=M\n");
                } else {
                    bufferedWriter.write("@" + map.get(arg1) + "\n");
                    bufferedWriter.write("D=M\n");
                    bufferedWriter.write("@" + Integer.valueOf(arg2) + "\n");
                    bufferedWriter.write("A=D+A\n");
                    bufferedWriter.write("D=M\n");
                }

                // common part
                bufferedWriter.write("@SP\n");
                bufferedWriter.write("A=M\n");
                bufferedWriter.write("M=D\n");
                bufferedWriter.write("@SP\n");
                bufferedWriter.write("M=M+1\n");
            }
        }

        if (commandType == CommandType.C_POP) {
            Map<String, String> map = new HashMap<String, String>() {{
                put("local", "LCL");
                put("argument", "ARG");
                put("this", "THIS");
                put("that", "THAT");
            }};

            if (arg1.equals("static")) {
                bufferedWriter.write("@" + vmFileName + Integer.valueOf(arg2) + "\n");
                bufferedWriter.write("D=A\n");
            } else if (arg1.equals("temp")) {
                bufferedWriter.write("@" + (5 + Integer.valueOf(arg2)) + "\n");
                bufferedWriter.write("D=A\n");
            } else if (arg1.equals("pointer")) {
                if (arg2.equals("0")) {
                    bufferedWriter.write("@THIS\n");
                } else {
                    bufferedWriter.write("@THAT\n");
                }
                bufferedWriter.write("D=A\n");
            } else {
                bufferedWriter.write("@" + map.get(arg1) + "\n");
                bufferedWriter.write("D=M\n");
                bufferedWriter.write("@" + Integer.valueOf(arg2) + "\n");
                bufferedWriter.write("D=D+A\n");
            }

            // common part
            bufferedWriter.write("@R13\n");
            bufferedWriter.write("M=D\n");
            bufferedWriter.write("@SP\n");
            bufferedWriter.write("AM=M-1\n");
            bufferedWriter.write("D=M\n");
            bufferedWriter.write("@R13\n");
            bufferedWriter.write("A=M\n");
            bufferedWriter.write("M=D\n");
        }

        bufferedWriter.flush();
    }

    void pushToStack(String value, boolean address) throws IOException {
        bufferedWriter.write(String.format("@%s\n", value));
        if (address) {
            bufferedWriter.write(String.format("D=A\n", value));
        } else {
            bufferedWriter.write(String.format("D=M\n", value));
        }
        bufferedWriter.write(String.format("@SP\n", value));
        bufferedWriter.write(String.format("A=M\n", value));
        bufferedWriter.write(String.format("M=D\n", value));
        bufferedWriter.write(String.format("@SP\n", value));
        bufferedWriter.write(String.format("M=M+1\n", value));
        bufferedWriter.flush();
    }
}

enum CommandType {C_ARITHMETIC, C_PUSH, C_POP, C_LABEL, C_GOTO, C_IF_GOTO, C_FUNCTION_CALL, C_FUNCTION_RETURN, C_FUNCTION_DEFINE}

class Parser {

    private final static String WHITE_SPACE = "[\\s]*"; // white space characters
    private final static String EMPTY_LINE = "^[\\s]*$";
    private final static String SINGLE_LINE_COMMENT = "(//.*$)|(/\\*.*\\*/)";
    private final static String MULTI_LINE_COMMENT_START = "/\\*.*$";
    private final static String MULTI_LINE_COMMENT_END = "^.*\\*/";
    private final Queue<String> nextCommand = new LinkedList();
    private BufferedReader bufferedReader;
    private boolean multiLineComment = false;

    Parser(File inputFIle) {
        parse(inputFIle);
    }


    /**
     * Returns command type of a given command
     *
     * @param command
     * @return
     */
    CommandType commandType(String command) {
        String[] parts = command.split(" ");
        if (parts[0].equals("return")) {
            return CommandType.C_FUNCTION_RETURN;
        } else if (parts.length == 1) {
            return CommandType.C_ARITHMETIC;
        } else if (parts[0].equals("push")) {
            return CommandType.C_PUSH;
        } else if (parts[0].equals("pop")) {
            return CommandType.C_POP;
        } else if (parts[0].equals("label")) {
            return CommandType.C_LABEL;
        } else if (parts[0].equals("goto")) {
            return CommandType.C_GOTO;
        } else if (parts[0].equals("if-goto")) {
            return CommandType.C_IF_GOTO;
        } else if (parts[0].equals("function")) {
            return CommandType.C_FUNCTION_DEFINE;
        } else if (parts[0].equals("call")) {
            return CommandType.C_FUNCTION_CALL;
        }
        throw new RuntimeException("Unknown command");
    }


    /**
     * Returns segment part of the command
     *
     * @param command
     * @return
     */
    String getArg1(String command) {
        return command.split(" ")[1];
    }

    /**
     * Returns value part of a command
     *
     * @param command
     * @return
     */
    String getArg2(String command) {
        return command.split(" ")[2];
    }

    /**
     * Initializes bufferedReader with the input file
     *
     * @param file: input file where commands are stored
     */
    private void parse(File file) {
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns true if there are any more commands to process else false
     *
     * @return
     */
    boolean hasMoreCommands() {
        if (nextCommand.isEmpty())
            loadNextCommand();
        return !nextCommand.isEmpty();
    }


    /**
     * Removes spaces, tabs, empty lines, single and multi line comments
     *
     * @param content a line from a text
     * @return
     */
    private static String trimContent(String content) {
        String[] patterns = {EMPTY_LINE, SINGLE_LINE_COMMENT};
        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern);
            Matcher matcher = p.matcher(content);
            content = matcher.replaceAll("");
        }
        return content;
    }


    /**
     * Returns the next command from the input file
     *
     * @return
     */
    public String nextCommand() {
        return nextCommand.poll();
    }


    /**
     * Reads the next command from the input file and loads it to the nextCommand queue
     *
     * @return
     */
    private void loadNextCommand() {
        while (true) {
            try {
                String line = bufferedReader.readLine();
                if (line == null) break;
                String trimmedLine = trimContent(line);
                Pattern multiLineStart = Pattern.compile(MULTI_LINE_COMMENT_START);
                Pattern multiLineEnd = Pattern.compile(MULTI_LINE_COMMENT_END);
                if (!trimmedLine.isEmpty()) {
                    Matcher multiLIneStartMatcher = multiLineStart.matcher(trimmedLine);
                    Matcher multiLIneEndMatcher = multiLineEnd.matcher(trimmedLine);
                    String command = "";
                    if (multiLIneStartMatcher.find()) {
                        multiLineComment = true;
                        command = multiLIneStartMatcher.replaceFirst(""); //return anything before comment starts
                    } else if (multiLIneEndMatcher.find()) {
                        multiLineComment = false;
                        command = multiLIneEndMatcher.replaceFirst("");//return anything after comment ends
                    } else if (!multiLineComment) {
                        command = trimmedLine;
                    }
                    if (!command.isEmpty()) {
                        command = command.replaceAll("[\\s]+", " ");
                        nextCommand.add(command);
                        break;
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
