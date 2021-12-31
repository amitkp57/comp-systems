import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VirtualMachine {
    public static void main(String[] args) throws IOException {
        String inputFileName = args[0];
        File inputFIle = new File(inputFileName);
        File outputFile = new File(inputFileName.substring(0, inputFileName.length() - 2) + "asm");
        CodeWriter codeWriter = new CodeWriter(outputFile);
        Parser parser = new Parser(inputFIle);
        while (parser.hasMoreCommands()) {
            String command = parser.nextCommand();
            CommandType commandType = parser.commandType(command.trim());
            if (commandType == CommandType.C_ARITHMETIC) {
                codeWriter.writeArithmeticCommand(command);
            } else {
                codeWriter.writePushPop(commandType, parser.getArg1(command), parser.getArg2(command));
            }
        }
        System.out.println("Successfully wrote to: " + outputFile);
    }
}

class CodeWriter {
    private int jumpCount = 0;
    private final BufferedWriter bufferedWriter;
    private final String fileName;

    CodeWriter(File outputFile) throws IOException {
        bufferedWriter = new BufferedWriter(new FileWriter(outputFile));
        fileName = Paths.get(outputFile.getPath()).getFileName().toString().split("\\.")[0];
    }

    /**
     * Translates arithmetic command to assembly language syntax
     *
     * @param command
     */
    void writeArithmeticCommand(String command) throws IOException {
        if (command.equals("add")) {
            bufferedWriter.write("@SP\n");
            bufferedWriter.write("AM=M-1\n");
            bufferedWriter.write("D=M\n");
            bufferedWriter.write("A=A-1\n");
            bufferedWriter.write("M=D+M\n");
        }

        if (command.equals("sub")) {
            bufferedWriter.write("@SP\n");
            bufferedWriter.write("AM=M-1\n");
            bufferedWriter.write("D=M\n");
            bufferedWriter.write("A=A-1\n");
            bufferedWriter.write("M=M-D\n");
        }

        if (command.equals("and")) {
            bufferedWriter.write("@SP\n");
            bufferedWriter.write("AM=M-1\n");
            bufferedWriter.write("D=M\n");
            bufferedWriter.write("A=A-1\n");
            bufferedWriter.write("M=D&M\n");
        }


        if (command.equals("or")) {
            bufferedWriter.write("@SP\n");
            bufferedWriter.write("AM=M-1\n");
            bufferedWriter.write("D=M\n");
            bufferedWriter.write("A=A-1\n");
            bufferedWriter.write("M=D|M\n");
        }

        if (command.equals("neg")) {
            bufferedWriter.write("@SP\n");
            bufferedWriter.write("A=M-1\n");
            bufferedWriter.write("M=-M\n");
        }

        if (command.equals("not")) {
            bufferedWriter.write("@SP\n");
            bufferedWriter.write("A=M-1\n");
            bufferedWriter.write("M=!M\n");
        }

        if (command.equals("gt")) {
            bufferedWriter.write("@SP\n");
            bufferedWriter.write("AM=M-1\n");
            bufferedWriter.write("D=M\n");
            bufferedWriter.write("A=A-1\n");
            bufferedWriter.write("D=M-D\n");
            bufferedWriter.write("@TRUE" + jumpCount + "\n");
            bufferedWriter.write("D;JGT\n");
            bufferedWriter.write("@SP\n");
            bufferedWriter.write("A=M-1\n");
            bufferedWriter.write("M=0\n");
            bufferedWriter.write("@CONTINUE" + jumpCount + "\n");
            bufferedWriter.write("0;JMP\n");
            bufferedWriter.write("(TRUE" + jumpCount + ")\n");
            bufferedWriter.write("@SP\n");
            bufferedWriter.write("A=M-1\n");
            bufferedWriter.write("M=-1\n");
            bufferedWriter.write("(CONTINUE" + jumpCount + ")\n");
            jumpCount++;
        }

        if (command.equals("lt")) {
            bufferedWriter.write("@SP\n");
            bufferedWriter.write("AM=M-1\n");
            bufferedWriter.write("D=M\n");
            bufferedWriter.write("A=A-1\n");
            bufferedWriter.write("D=M-D\n");
            bufferedWriter.write("@TRUE" + jumpCount + "\n");
            bufferedWriter.write("D;JLT\n");
            bufferedWriter.write("@SP\n");
            bufferedWriter.write("A=M-1\n");
            bufferedWriter.write("M=0\n");
            bufferedWriter.write("@CONTINUE" + jumpCount + "\n");
            bufferedWriter.write("0;JMP\n");
            bufferedWriter.write("(TRUE" + jumpCount + ")\n");
            bufferedWriter.write("@SP\n");
            bufferedWriter.write("A=M-1\n");
            bufferedWriter.write("M=-1\n");
            bufferedWriter.write("(CONTINUE" + jumpCount + ")\n");
            jumpCount++;
        }
        if (command.equals("eq")) {
            bufferedWriter.write("@SP\n");
            bufferedWriter.write("AM=M-1\n");
            bufferedWriter.write("D=M\n");
            bufferedWriter.write("A=A-1\n");
            bufferedWriter.write("D=M-D\n");
            bufferedWriter.write("@TRUE" + jumpCount + "\n");
            bufferedWriter.write("D;JEQ\n");
            bufferedWriter.write("@SP\n");
            bufferedWriter.write("A=M-1\n");
            bufferedWriter.write("M=0\n");
            bufferedWriter.write("@CONTINUE" + jumpCount + "\n");
            bufferedWriter.write("0;JMP\n");
            bufferedWriter.write("(TRUE" + jumpCount + ")\n");
            bufferedWriter.write("@SP\n");
            bufferedWriter.write("A=M-1\n");
            bufferedWriter.write("M=-1\n");
            bufferedWriter.write("(CONTINUE" + jumpCount + ")\n");
            jumpCount++;
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
                    bufferedWriter.write("@" + fileName + arg2 + "\n");
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
                bufferedWriter.write("@" + fileName + Integer.valueOf(arg2) + "\n");
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
}

enum CommandType {C_ARITHMETIC, C_PUSH, C_POP}

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
        if (parts.length == 1) {
            return CommandType.C_ARITHMETIC;
        } else if (parts[0].equals("push")) {
            return CommandType.C_PUSH;
        } else {
            return CommandType.C_POP;
        }
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
